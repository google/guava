/*
 * Copyright (C) 2010 The Guava Authors
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
   * Creates a new, empty map with the specified strategy, initial capacity, load factor and
   * concurrency level.
   */
  ComputingConcurrentHashMap(MapMaker builder, Function<? super K, ? extends V> computingFunction) {
    super(builder);
    this.computingFunction = checkNotNull(computingFunction);
  }

  @Override
  public ConcurrentMap<K, V> asMap() {
    return this;
  }

  @Override
  Segment<K, V> createSegment(int initialCapacity, int maxSegmentSize) {
    return new ComputingSegment<K, V>(this, initialCapacity, maxSegmentSize);
  }

  @Override
  ComputingSegment<K, V> segmentFor(int hash) {
    return (ComputingSegment<K, V>) super.segmentFor(hash);
  }

  @Override
  public V apply(K key) {
    int hash = hash(key);
    return segmentFor(hash).compute(key, hash);
  }

  @SuppressWarnings("serial") // This class is never serialized.
  static class ComputingSegment<K, V> extends Segment<K, V> {
    ComputingSegment(CustomConcurrentHashMap<K, V> map, int initialCapacity, int maxSegmentSize) {
      super(map, initialCapacity, maxSegmentSize);
    }

    V compute(K key, int hash) {
      try {
        outer: while (true) {
          ReferenceEntry<K, V> e = getEntry(key, hash);
          if (e != null) {
            V value = getLiveValue(e);
            if (value != null) {
              recordRead(e);
              return value;
            }
          }

          // at this point e is either null, computing, or expired;
          // avoid locking if it's already computing
          if (e == null || !e.getValueReference().isComputingReference()) {
            ComputingValueReference<K, V> computingValueReference = null;
            lock();
            try {
              preWriteCleanup();

              // getFirst, but remember the index
              AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
              int index = hash & (table.length() - 1);
              ReferenceEntry<K, V> first = table.get(index);

              for (e = first; e != null; e = e.getNext()) {
                K entryKey = e.getKey();
                if (e.getHash() == hash && entryKey != null
                    && map.keyEquivalence.equivalent(key, entryKey)) {
                  if (!e.getValueReference().isComputingReference()) {
                    // never return expired entries
                    V value = getLiveValue(e);
                    if (value != null) {
                      recordLockedRead(e);
                      return value;
                    }
                    // clobber invalid entries
                    unsetLiveEntry(e, hash);
                  }
                  break;
                }
              }

              if (e == null || isUnset(e)) {
                // Create a new entry.
                ComputingConcurrentHashMap<K, V> computingMap =
                    (ComputingConcurrentHashMap<K, V>) map;
                computingValueReference = new ComputingValueReference<K, V>(computingMap);

                if (e == null) {
                  e = computingMap.newEntry(key, hash, first);
                  table.set(index, e);
                }
                e.setValueReference(computingValueReference);
              }
            } finally {
              unlock();
              postWriteCleanup();
            }

            if (computingValueReference != null) {
              // This thread solely created the entry.
              V value = null;
              try {
                // Synchronizes on the entry to allow failing fast when a
                // recursive computation is detected. This is not fool-proof
                // since the entry may be copied when the segment is written to.
                synchronized (e) {
                  value = computingValueReference.compute(key, hash);
                }
                checkNotNull(value, "compute() returned null unexpectedly");
                return value;
              } finally {
                if (value == null) {
                  clearValue(key, hash, computingValueReference);
                }
              }
            }
          }

          // The entry already exists. Wait for the computation.
          boolean interrupted = false;
          try {
            while (true) {
              try {
                checkState(!Thread.holdsLock(e), "Recursive computation");
                V value = e.getValueReference().waitForValue();
                // don't consider expiration as we're concurrent with computation
                if (value != null) {
                  recordRead(e);
                  return value;
                }
                // else computing thread will clearValue
                continue outer;
              } catch (InterruptedException ie) {
                interrupted = true;
              }
            }
          } finally {
            if (interrupted) {
              Thread.currentThread().interrupt();
            }
          }
        }
      } finally {
        postReadCleanup();
      }
    }
  }

  /** Used to provide null pointer exceptions to other threads. */
  private static class NullPointerExceptionReference<K, V> implements ValueReference<K, V> {
    final String message;

    NullPointerExceptionReference(String message) {
      this.message = message;
    }

    @Override
    public V get() {
      return null;
    }

    @Override
    public ValueReference<K, V> copyFor(ReferenceEntry<K, V> entry) {
      return this;
    }

    @Override
    public boolean isComputingReference() {
      return false;
    }

    @Override
    public V waitForValue() {
      throw new NullPointerException(message);
    }

    @Override
    public void notifyValueReclaimed() {}

    @Override
    public void clear() {}
  }

  /** Used to provide computation exceptions to other threads. */
  private static class ComputationExceptionReference<K, V> implements ValueReference<K, V> {
    final Throwable t;

    ComputationExceptionReference(Throwable t) {
      this.t = t;
    }

    @Override
    public V get() {
      return null;
    }

    @Override
    public ValueReference<K, V> copyFor(ReferenceEntry<K, V> entry) {
      return this;
    }

    @Override
    public boolean isComputingReference() {
      return false;
    }

    @Override
    public V waitForValue() {
      throw new AsynchronousComputationException(t);
    }

    @Override
    public void notifyValueReclaimed() {}

    @Override
    public void clear() {}
  }

  /** Used to provide computation result to other threads. */
  private static class ComputedReference<K, V> implements ValueReference<K, V> {
    final V value;

    ComputedReference(@Nullable V value) {
      this.value = value;
    }

    @Override
    public V get() {
      return value;
    }

    @Override
    public ValueReference<K, V> copyFor(ReferenceEntry<K, V> entry) {
      return this;
    }

    @Override
    public boolean isComputingReference() {
      return false;
    }

    @Override
    public V waitForValue() {
      return get();
    }

    @Override
    public void notifyValueReclaimed() {}

    @Override
    public void clear() {}
  }

  private static class ComputingValueReference<K, V> implements ValueReference<K, V> {
    final ComputingConcurrentHashMap<K, V> map;

    @GuardedBy("ComputingValueReference.this") // writes
    ValueReference<K, V> computedReference = unset();

    public ComputingValueReference(ComputingConcurrentHashMap<K, V> map) {
      this.map = map;
    }

    @Override
    public V get() {
      // All computation lookups go through waitForValue. This method thus is
      // only used by put, to whom we always want to appear absent.
      return null;
    }

    @Override
    public ValueReference<K, V> copyFor(ReferenceEntry<K, V> entry) {
      return this;
    }

    @Override
    public boolean isComputingReference() {
      return true;
    }

    /**
     * Waits for a computation to complete. Returns the result of the computation.
     */
    @Override
    public V waitForValue() throws InterruptedException {
      if (computedReference == UNSET) {
        synchronized (this) {
          if (computedReference == UNSET) {
            wait();
          }
        }
      }
      return computedReference.waitForValue();
    }

    @Override
    public void clear() {
      // The pending computation was clobbered by a manual write. Unblock all
      // pending gets, and have them return the new value.
      setValueReference(new ComputedReference<K, V>(null));

      // TODO(user): could also cancel computation if we had a thread handle
    }

    @Override
    public void notifyValueReclaimed() {}

    V compute(K key, int hash) {
      V value;
      try {
        value = map.computingFunction.apply(key);
      } catch (ComputationException e) {
        // if computingFunction has thrown a computation exception,
        // propagate rather than wrap
        setValueReference(new ComputationExceptionReference<K, V>(e.getCause()));
        throw e;
      } catch (Throwable t) {
        setValueReference(new ComputationExceptionReference<K, V>(t));
        throw new ComputationException(t);
      }

      if (value == null) {
        String message = map.computingFunction + " returned null for key " + key + ".";
        setValueReference(new NullPointerExceptionReference<K, V>(message));
        throw new NullPointerException(message);
      }

      // Call setValueReference first to avoid put clearing us.
      setValueReference(new ComputedReference<K, V>(value));
      // putIfAbsent
      map.segmentFor(hash).put(key, hash, value, true);
      return value;
    }

    void setValueReference(ValueReference<K, V> valueReference) {
      synchronized (this) {
        if (computedReference == UNSET) {
          computedReference = valueReference;
          notifyAll();
        }
      }
    }
  }

  /* ---------------- Serialization Support -------------- */

  private static final long serialVersionUID = 2;

  @Override
  Object writeReplace() {
    return new ComputingSerializationProxy<K, V>(keyStrength, valueStrength, keyEquivalence,
        valueEquivalence, expireAfterWriteNanos, expireAfterAccessNanos, maximumSize,
        concurrencyLevel, evictionListener, this, computingFunction);
  }

  static class ComputingSerializationProxy<K, V> extends AbstractSerializationProxy<K, V> {

    final Function<? super K, ? extends V> computingFunction;
    transient Cache<K, V> cache;

    ComputingSerializationProxy(Strength keyStrength, Strength valueStrength,
        Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence,
        long expireAfterWriteNanos, long expireAfterAccessNanos, int maximumSize,
        int concurrencyLevel, MapEvictionListener<? super K, ? super V> evictionListener,
        ConcurrentMap<K, V> delegate, Function<? super K, ? extends V> computingFunction) {
      super(keyStrength, valueStrength, keyEquivalence, valueEquivalence, expireAfterWriteNanos,
          expireAfterAccessNanos, maximumSize, concurrencyLevel, evictionListener, delegate);
      this.computingFunction = computingFunction;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      writeMapTo(out);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
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
