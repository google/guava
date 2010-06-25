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

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker.Cache;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.annotation.Nullable;

/**
 * Adds computing functionality to {@link CustomConcurrentHashMap}.
 *
 * @author Bob Lee
 */
class ComputingConcurrentHashMap<K, V> extends CustomConcurrentHashMap<K, V>
    implements Cache<K, V> {

  /*
   * TODO: Select a permanent name for this class. The name matters because
   * we expose it in the serialized state and will be stuck w/ it forever.
   */

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

  public V apply(K key) {
    checkNotNull(key);

    int hash = hash(key);
    Segment segment = segmentFor(hash);
    outer: while (true) {
      ReferenceEntry<K, V> entry = segment.getEntry(key, hash);
      if (entry == null) {
        boolean created = false;
        segment.lock();
        try {
          if (expires) {
            segment.expireEntries();
          }

          // Try again--an entry could have materialized in the interim.
          entry = segment.getEntry(key, hash);
          if (entry == null) {
            // Create a new entry.
            created = true;
            int count = segment.count;
            if (count++ > segment.threshold) { // ensure capacity
              segment.expand();
            }
            AtomicReferenceArray<ReferenceEntry<K, V>> table = segment.table;
            int index = hash & (table.length() - 1);
            ReferenceEntry<K, V> first = table.get(index);
            ++segment.modCount;
            entry = entryFactory.newEntry(this, key, hash, first);
            table.set(index, entry);
            segment.count = count; // write-volatile
          }
        } finally {
          segment.unlock();
        }

        if (created) {
          // This thread solely created the entry.
          boolean success = false;
          try {
            V value = compute(segment, key, entry);
            checkNotNull(value,
                "compute() returned null unexpectedly");
            success = true;
            return value;
          } finally {
            if (!success) {
              segment.removeEntry(entry, hash);
            }
          }
        }
      }

      // The entry already exists. Wait for the computation.
      boolean interrupted = false;
      try {
        while (true) {
          try {
            V value = waitForValue(entry);
            if (value == null) {
              // Purge entry and try again.
              segment.removeEntry(entry, hash);
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

  @Override void setValueReference(ReferenceEntry<K, V> entry,
      ValueReference<K, V> valueReference) {
    boolean notifyOthers = (entry.getValueReference() == UNSET);
    entry.setValueReference(valueReference);
    if (notifyOthers) {
      synchronized (entry) {
        entry.notifyAll();
      }
    }
  }

  /**
   * Waits for a computation to complete. Returns the result of the
   * computation or null if none was available.
   */
  public V waitForValue(ReferenceEntry<K, V> entry)
      throws InterruptedException {
    ValueReference<K, V> valueReference = entry.getValueReference();
    if (valueReference == UNSET) {
      synchronized (entry) {
        while ((valueReference = entry.getValueReference()) == UNSET) {
          entry.wait();
        }
      }
    }
    return valueReference.waitForValue();
  }

  /** Used to provide null output exceptions to other threads. */
  private static class NullOutputExceptionReference<K, V>
      implements ValueReference<K, V> {
    final String message;
    NullOutputExceptionReference(String message) {
      this.message = message;
    }
    public V get() {
      return null;
    }
    public ValueReference<K, V> copyFor(
        ReferenceEntry<K, V> entry) {
      return this;
    }
    public V waitForValue() {
      throw new NullOutputException(message);
    }
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
    public ValueReference<K, V> copyFor(
        ReferenceEntry<K, V> entry) {
      return this;
    }
    public V waitForValue() {
      throw new AsynchronousComputationException(t);
    }
  }

  public V compute(Segment segment, K key, ReferenceEntry<K, V> entry) {
    V value;
    try {
      value = computingFunction.apply(key);
    } catch (ComputationException e) {
      // if computingFunction has thrown a computation exception,
      // propagate rather than wrap
      // TODO: If we remove the entry before setting the value reference,
      // if the caller retries, they'll get the result of a different
      // rather than the same result.
      setValueReference(entry,
          new ComputationExceptionReference<K, V>(e.getCause()));
      throw e;
    } catch (Throwable t) {
      setValueReference(entry, new ComputationExceptionReference<K, V>(t));
      throw new ComputationException(t);
    }

    if (value == null) {
      String message =
          computingFunction + " returned null for key " + key + ".";
      // TODO: If we remove the entry before setting the value reference,
      // if the caller retries, they'll get the result of a different
      // rather than the same result.
      setValueReference(entry,
          new NullOutputExceptionReference<K, V>(message));
      throw new NullOutputException(message);
    }

    if (expires) {
      segment.lock();
      try {
        segment.setValue(entry, value, true);
      } finally {
        segment.unlock();
      }
    } else {
      segment.setValue(entry, value, true);
    }
    return value;
  }

  @Override ReferenceEntry<K, V> copyEntry(
      ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
    ReferenceEntry<K, V> newEntry
        = entryFactory.copyEntry(this, original, newNext);
    ValueReference<K, V> valueReference = original.getValueReference();
    if (valueReference == UNSET) {
      newEntry.setValueReference(
          new FutureValueReference(original, newEntry));
    } else {
      newEntry.setValueReference(valueReference.copyFor(newEntry));
    }
    return newEntry;
  }

  /**
   * Points to an old entry where a value is being computed. Used to
   * support non-blocking copying of entries during table expansion,
   * removals, etc.
   */
  private class FutureValueReference implements ValueReference<K, V> {
    final ReferenceEntry<K, V> original;
    final ReferenceEntry<K, V> newEntry;

    FutureValueReference(
        ReferenceEntry<K, V> original, ReferenceEntry<K, V> newEntry) {
      this.original = original;
      this.newEntry = newEntry;
    }

    public V get() {
      boolean success = false;
      try {
        V value = original.getValueReference().get();
        success = true;
        return value;
      } finally {
        if (!success) {
          removeEntry();
        }
      }
    }

    public ValueReference<K, V> copyFor(ReferenceEntry<K, V> entry) {
      return new FutureValueReference(original, entry);
    }

    public V waitForValue() throws InterruptedException {
      boolean success = false;
      try {
        // assert that key != null
        V value = ComputingConcurrentHashMap.this.waitForValue(original);
        success = true;
        return value;
      } finally {
        if (!success) {
          removeEntry();
        }
      }
    }

    /**
     * Removes the entry in the event of an exception. Ideally,
     * we'd clean up as soon as the computation completes, but we
     * can't do that without keeping a reference to this entry from
     * the original.
     */
    void removeEntry() {
      ComputingConcurrentHashMap.this.removeEntry(newEntry);
    }
  }

  /* ---------------- Serialization Support -------------- */

  private static final long serialVersionUID = 0;

  @Override Object writeReplace() {
    return new ComputingSerializationProxy<K, V>(keyStrength, valueStrength,
        keyEquivalence, valueEquivalence, expirationNanos, maximumSize,
        concurrencyLevel, this, computingFunction);
  }

  static class ComputingSerializationProxy<K, V>
      extends AbstractSerializationProxy<K, V> {

    final Function<? super K, ? extends V> computingFunction;
    transient Cache<K, V> cache;

    ComputingSerializationProxy(Strength keyStrength,
        Strength valueStrength,
        Equivalence<Object> keyEquivalence,
        Equivalence<Object> valueEquivalence,
        long expirationNanos,
        int maximumSize,
        int concurrencyLevel,
        ConcurrentMap<K, V> delegate,
        Function<? super K, ? extends V> computingFunction) {
      super(keyStrength, valueStrength, keyEquivalence, valueEquivalence,
          expirationNanos, maximumSize, concurrencyLevel, delegate);
      this.computingFunction = computingFunction;
    }

    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
      out.defaultWriteObject();
      writeMapTo(out);
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in)
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

    private static final long serialVersionUID = 0;
  }
}
