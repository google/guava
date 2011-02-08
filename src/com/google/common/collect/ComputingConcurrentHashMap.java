/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker.Cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * Adds computing functionality to {@link CustomConcurrentHashMap}.
 *
 * @author Bob Lee
 */
class ComputingConcurrentHashMap<K, V> extends CustomConcurrentHashMap<K, V>
    implements Cache<K, V> {
  final Function<? super K, ? extends V> computingFunction;

  /**
   * Creates a new, empty map with the specified strategy, initial capacity,
   * load factor and concurrency level.
   */
  ComputingConcurrentHashMap(MapMaker builder,
      Function<? super K, ? extends V> computingFunction) {
    super(builder);
    this.computingFunction = checkNotNull(computingFunction);
  }

  public ConcurrentMap<K, V> asMap() {
    return this;
  }

  @Override Segment createSegment(int initialCapacity, int maxSegmentSize) {
    return new ComputingSegment(initialCapacity, maxSegmentSize);
  }

  @SuppressWarnings("unchecked") // explain
  @Override ComputingSegment segmentFor(int hash) {
    return (ComputingSegment) super.segmentFor(hash);
  }

  public V apply(K key) {
    int hash = hash(key);
    return segmentFor(hash).compute(key, hash);
  }

  @SuppressWarnings("serial") // This class is never serialized.
  class ComputingSegment extends Segment {
    ComputingSegment(int initialCapacity, int maxSegmentSize) {
      super(initialCapacity, maxSegmentSize);
    }

    V compute(K key, int hash) {
      outer: while (true) {
        ReferenceEntry<K, V> entry = getEntry(key, hash);
        if (entry == null || entry.getValueReference() == UNSET) {
          boolean created = false;
          ComputingValueReference computingValueReference = null;
          lock();
          try {
            expireEntries();
            // cleanup failed computes
            // TODO(user): move cleanup to an executor, but then we need to
            // account for invalidated entries below
            cleanup();

            // Try again--an entry could have materialized in the interim.
            entry = getEntry(key, hash);
            if (entry == null) {
              // Create a new entry.
              created = true;
              int newCount = this.count + 1; // read-volatile
              if (evictsBySize() && newCount > maxSegmentSize) {
                evictEntry();
                newCount = this.count + 1; // read-volatile
              } else if (newCount > threshold) { // ensure capacity
                expand();
              }
              AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
              int index = hash & (table.length() - 1);
              ReferenceEntry<K, V> first = table.get(index);
              ++modCount;
              entry = entryFactory.newEntry(
                  ComputingConcurrentHashMap.this, key, hash, first);
              computingValueReference = new ComputingValueReference();
              entry.setValueReference(computingValueReference);
              table.set(index, entry);
              this.count = newCount; // write-volatile
            }
          } finally {
            unlock();
            processPendingNotifications();
          }

          if (created) {
            // This thread solely created the entry.
            boolean success = false;
            try {
              V value = null;
              // Synchronizes on the entry to allow failing fast when a
              // recursive computation is detected. This is not full-proof
              // since the entry may be copied when the segment is written to.
              synchronized (entry) {
                value = computingValueReference.compute(key, hash);
              }
              checkNotNull(value, "compute() returned null unexpectedly");
              success = true;
              return value;
            } finally {
              if (!success) {
                invalidateValue(entry, hash, computingValueReference);
              }
            }
          }
        }

        // The entry already exists. Wait for the computation.
        boolean interrupted = false;
        try {
          while (true) {
            try {
              checkState(!Thread.holdsLock(entry), "Recursive computation");
              ValueReference<K, V> valueReference = entry.getValueReference();
              V value = valueReference.waitForValue();
              if (value == null) {
                // Purge entry and try again.
                invalidateValue(entry, hash, valueReference);
                continue outer;
              }
              return value;
            } catch (InterruptedException e) {
              interrupted = true;
            }
          }
        } finally {
          if (interrupted) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }
  }

  /** Used to provide null pointer exceptions to other threads. */
  private static class NullPointerExceptionReference<K, V>
      implements ValueReference<K, V> {
    final String message;
    NullPointerExceptionReference(String message) {
      this.message = message;
    }
    public V get() {
      return null;
    }
    public ValueReference<K, V> copyFor(ReferenceEntry<K, V> entry) {
      return this;
    }
    public V waitForValue() {
      throw new NullPointerException(message);
    }
    public void clear() {}
  }

  /** Used to provide computation exceptions to other threads. */
  private static class ComputationExceptionReference<K, V>
      implements ValueReference<K, V> {
    final Throwable t;
    ComputationExceptionReference(Throwable t) {
      this.t = t;
    }
    public V get() {
      return null;
    }
    public ValueReference<K, V> copyFor(ReferenceEntry<K, V> entry) {
      return this;
    }
    public V waitForValue() {
      throw new AsynchronousComputationException(t);
    }
    public void clear() {}
  }

  private class ComputingValueReference implements ValueReference<K, V> {
    @GuardedBy("ComputingValueReference.this") // writes
    ValueReference<K, V> computedReference = unset();

    public V get() {
      return computedReference.get();
    }

    public ValueReference<K, V> copyFor(ReferenceEntry<K, V> entry) {
      return this;
    }

    /**
     * Waits for a computation to complete. Returns the result of the
     * computation.
     */
    public V waitForValue() throws InterruptedException {
      if (computedReference == UNSET) {
        synchronized (this) {
          if (computedReference == UNSET) {
            wait();
          }
        }
      }
      return get();
    }

    public void clear() {
      // The pending computation was clobbered by a manual write. Unblock all
      // pending gets, and have them return the new value.
      // TODO(user): could also cancel computation if we had a thread handle
      synchronized (this) {
        notifyAll();
      }
    }

    V compute(K key, int hash) {
      V value;
      try {
        value = computingFunction.apply(key);
      } catch (ComputationException e) {
        // if computingFunction has thrown a computation exception,
        // propagate rather than wrap
        setValueReference(
            new ComputationExceptionReference<K, V>(e.getCause()));
        throw e;
      } catch (Throwable t) {
        setValueReference(new ComputationExceptionReference<K, V>(t));
        throw new ComputationException(t);
      }

      if (value == null) {
        String message =
            computingFunction + " returned null for key " + key + ".";
        setValueReference(new NullPointerExceptionReference<K, V>(message));
        throw new NullPointerException(message);
      }
      setComputedValue(key, hash, value);
      return value;
    }

    /**
     * Sets the value of a newly computed entry. Adds newly created entries at
     * the end of the expiration queue.
     */
    void setComputedValue(K key, int hash, V value) {
      Segment segment = segmentFor(hash);
      segment.lock();
      try {
        for (ReferenceEntry<K, V> e = segment.getFirst(hash); e != null;
            e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash && entryKey != null
              && keyEquivalence.equivalent(key, entryKey)) {
            ValueReference<K, V> liveValueReference = e.getValueReference();
            if (liveValueReference == this) {
              if (evictsBySize() || expires()) {
                // "entry" currently points to the original entry created when
                // computation began, but by now that entry may have been
                // replaced. Find the current entry, and pass it to
                // recordWrite to ensure that the eviction lists are
                // consistent with the current map entries.
                segment.recordWrite(e);
              }
              setValueReference(valueStrength.referenceValue(e, value));
            } else {
              // avoid creating a new value reference pointing back to a
              // disconnected entry
              setValueReference(liveValueReference);
            }
            return;
          }
        }
      } finally {
        segment.unlock();
        processPendingNotifications();
      }
    }

    void setValueReference(ValueReference<K, V> valueReference) {
      synchronized (this) {
        computedReference = valueReference;
        notifyAll();
      }
    }
  }

  /* ---------------- Serialization Support -------------- */

  private static final long serialVersionUID = 2;

  @Override Object writeReplace() {
    return new ComputingSerializationProxy<K, V>(keyStrength, valueStrength,
        keyEquivalence, valueEquivalence, expireAfterWriteNanos,
        expireAfterAccessNanos, maximumSize, concurrencyLevel, evictionListener,
        this, computingFunction);
  }

  static class ComputingSerializationProxy<K, V>
      extends AbstractSerializationProxy<K, V> {

    final Function<? super K, ? extends V> computingFunction;
    transient Cache<K, V> cache;

    ComputingSerializationProxy(Strength keyStrength,
        Strength valueStrength,
        Equivalence<Object> keyEquivalence,
        Equivalence<Object> valueEquivalence,
        long expireAfterWriteNanos,
        long expireAfterAccessNanos,
        int maximumSize,
        int concurrencyLevel,
        MapEvictionListener<? super K, ? super V> evictionListener,
        ConcurrentMap<K, V> delegate,
        Function<? super K, ? extends V> computingFunction) {
      super(keyStrength, valueStrength, keyEquivalence, valueEquivalence,
          expireAfterWriteNanos, expireAfterAccessNanos, maximumSize,
          concurrencyLevel, evictionListener, delegate);
      this.computingFunction = computingFunction;
    }

    private void writeObject(ObjectOutputStream out)
        throws IOException {
      out.defaultWriteObject();
      writeMapTo(out);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      MapMaker mapMaker = readMapMaker(in);
      cache = mapMaker.makeCache(computingFunction);
      delegate = cache.asMap();
      readEntries(in);
    }

    Object readResolve() {
      return cache;
    }

    public ConcurrentMap<K, V> asMap() {
      return delegate;
    }

    public V apply(@Nullable K from) {
      return cache.apply(from);
    }

    private static final long serialVersionUID = 2;
  }
}
