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
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractCache.StatsCounter;
import com.google.common.collect.MapMaker.RemovalCause;
import com.google.common.collect.MapMaker.RemovalListener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * Adds computing functionality to {@link CustomConcurrentHashMap}.
 *
 * @author Bob Lee
 * @author Charles Fry
 */
class ComputingConcurrentHashMap<K, V> extends CustomConcurrentHashMap<K, V> {
  final CacheLoader<? super K, ? extends V> loader;

  /**
   * Creates a new, empty map with the specified strategy, initial capacity, load factor and
   * concurrency level.
   */
  ComputingConcurrentHashMap(MapMaker builder,
      Supplier<? extends StatsCounter> statsCounterSupplier,
      CacheLoader<? super K, ? extends V> loader) {
    super(builder, statsCounterSupplier);
    this.loader = checkNotNull(loader);
  }

  @Override
  Segment<K, V> createSegment(int initialCapacity, int maxSegmentSize,
      StatsCounter statsCounter) {
    return new ComputingSegment<K, V>(this, initialCapacity, maxSegmentSize, statsCounter);
  }

  @Override
  ComputingSegment<K, V> segmentFor(int hash) {
    return (ComputingSegment<K, V>) super.segmentFor(hash);
  }

  V getOrCompute(K key) throws ExecutionException {
    int hash = hash(checkNotNull(key));
    return segmentFor(hash).getOrCompute(key, hash, loader);
  }

  @SuppressWarnings("serial") // This class is never serialized.
  static final class ComputingSegment<K, V> extends Segment<K, V> {
    ComputingSegment(CustomConcurrentHashMap<K, V> map, int initialCapacity, int maxSegmentSize,
        StatsCounter statsCounter) {
      super(map, initialCapacity, maxSegmentSize, statsCounter);
    }

    V getOrCompute(K key, int hash, CacheLoader<? super K, ? extends V> loader)
        throws ExecutionException {
      try {
        outer: while (true) {
          // don't call getLiveEntry, which would ignore computing values
          ReferenceEntry<K, V> e = getEntry(key, hash);
          if (e != null) {
            V value = getLiveValue(e);
            if (value != null) {
              recordRead(e);
              statsCounter.recordHit();
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

              int newCount = this.count - 1;
              AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
              int index = hash & (table.length() - 1);
              ReferenceEntry<K, V> first = table.get(index);

              boolean createNewEntry = true;
              for (e = first; e != null; e = e.getNext()) {
                K entryKey = e.getKey();
                if (e.getHash() == hash && entryKey != null
                    && map.keyEquivalence.equivalent(key, entryKey)) {
                  ValueReference<K, V> valueReference = e.getValueReference();
                  if (valueReference.isComputingReference()) {
                    createNewEntry = false;
                  } else {
                    // never return expired entries
                    V value = getLiveValue(e);
                    if (value != null) {
                      recordLockedRead(e);
                      statsCounter.recordHit();
                      return value;
                    }
                    // immediately reuse partially collected entries
                    enqueueNotification(entryKey, hash, value, RemovalCause.COLLECTED);
                    evictionQueue.remove(e);
                    expirationQueue.remove(e);
                    this.count = newCount; // write-volatile
                  }
                  break;
                }
              }

              if (createNewEntry) {
                computingValueReference = new ComputingValueReference<K, V>(loader);

                if (e == null) {
                  e = newEntry(key, hash, first);
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
              return compute(key, hash, e, computingValueReference);
            }
          }

          // The entry already exists. Wait for the computation.
          checkState(!Thread.holdsLock(e), "Recursive computation");
          // don't consider expiration as we're concurrent with computation
          V value = e.getValueReference().waitForValue();
          if (value != null) {
            recordRead(e);
            statsCounter.recordConcurrentMiss();
            return value;
          }
          // else computing thread will clearValue
          continue outer;
        }
      } finally {
        postReadCleanup();
      }
    }

    V compute(K key, int hash, ReferenceEntry<K, V> e,
        ComputingValueReference<K, V> computingValueReference)
        throws ExecutionException {
      V value = null;
      long start = System.nanoTime();
      long end = 0;
      try {
        // Synchronizes on the entry to allow failing fast when a recursive computation is
        // detected. This is not fool-proof since the entry may be copied when the segment
        // is written to.
        synchronized (e) {
          value = computingValueReference.compute(key, hash);
          end = System.nanoTime();
        }
        if (value != null) {
          // a null return value is an Error, so don't count it in the stats
          statsCounter.recordCreateSuccess(end - start);

          // putIfAbsent
          V oldValue = put(key, hash, value, true);
          if (oldValue != null) {
            // the computed value was already clobbered
            enqueueNotification(key, hash, value, RemovalCause.REPLACED);
          }
        }
        return value;
      } finally {
        if (end == 0) {
          end = System.nanoTime();
          statsCounter.recordCreateException(end - start);
        }
        if (value == null) {
          clearValue(key, hash, computingValueReference);
        }
      }
    }
  }

  /**
   * Used to provide computation exceptions to other threads.
   */
  private static final class ComputationExceptionReference<K, V> implements ValueReference<K, V> {
    final Throwable t;

    ComputationExceptionReference(Throwable t) {
      this.t = t;
    }

    @Override
    public V get() {
      return null;
    }

    @Override
    public ReferenceEntry<K, V> getEntry() {
      return null;
    }

    @Override
    public ValueReference<K, V> copyFor(ReferenceQueue<V> queue, ReferenceEntry<K, V> entry) {
      return this;
    }

    @Override
    public boolean isComputingReference() {
      return false;
    }

    @Override
    public V waitForValue() throws ExecutionException {
      throw new ExecutionException(t);
    }

    @Override
    public void clear(ValueReference<K, V> newValue) {}
  }

  /**
   * Used to provide computation result to other threads.
   */
  private static final class ComputedReference<K, V> implements ValueReference<K, V> {
    final V value;

    ComputedReference(@Nullable V value) {
      this.value = value;
    }

    @Override
    public V get() {
      return value;
    }

    @Override
    public ReferenceEntry<K, V> getEntry() {
      return null;
    }

    @Override
    public ValueReference<K, V> copyFor(ReferenceQueue<V> queue, ReferenceEntry<K, V> entry) {
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
    public void clear(ValueReference<K, V> newValue) {}
  }

  private static final class ComputingValueReference<K, V> implements ValueReference<K, V> {
    final CacheLoader<? super K, ? extends V> loader;

    @GuardedBy("ComputingValueReference.this") // writes
    volatile ValueReference<K, V> computedReference = unset();

    public ComputingValueReference(CacheLoader<? super K, ? extends V> loader) {
      this.loader = loader;
    }

    @Override
    public V get() {
      // All computation lookups go through waitForValue. This method thus is
      // only used by put, to whom we always want to appear absent.
      return null;
    }

    @Override
    public ReferenceEntry<K, V> getEntry() {
      return null;
    }

    @Override
    public ValueReference<K, V> copyFor(ReferenceQueue<V> queue, ReferenceEntry<K, V> entry) {
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
    public V waitForValue() throws ExecutionException {
      if (computedReference == UNSET) {
        boolean interrupted = false;
        try {
          synchronized (this) {
            while (computedReference == UNSET) {
              try {
                wait();
              } catch (InterruptedException ie) {
                interrupted = true;
              }
            }
          }
        } finally {
          if (interrupted) {
            Thread.currentThread().interrupt();
          }
        }
      }
      return computedReference.waitForValue();
    }

    @Override
    public void clear(ValueReference<K, V> newValue) {
      // The pending computation was clobbered by a manual write. Unblock all
      // pending gets, and have them return the new value.
      setValueReference(newValue);

      // TODO(user): could also cancel computation if we had a thread handle
    }

    V compute(K key, int hash) throws ExecutionException {
      V value;
      try {
        value = loader.load(key);
      } catch (Throwable t) {
        setValueReference(new ComputationExceptionReference<K, V>(t));
        throw new ExecutionException(t);
      }

      setValueReference(new ComputedReference<K, V>(value));
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

  /**
   * Overrides get() to compute on demand. Also throws an exception when null is returned from a
   * computation.
   */
  static final class ComputingMapAdapter<K, V>
      extends ComputingConcurrentHashMap<K, V> implements Serializable {
    private static final long serialVersionUID = 0;

    ComputingMapAdapter(MapMaker mapMaker,
        Supplier<? extends StatsCounter> statsCounterSupplier,
        CacheLoader<? super K, ? extends V> loader) {
      super(mapMaker, statsCounterSupplier, loader);
    }

    @SuppressWarnings("unchecked") // unsafe, which is one advantage of Cache over Map
    @Override
    public V get(Object key) {
      V value;
      try {
        value = getOrCompute((K) key);
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        Throwables.propagateIfInstanceOf(cause, ComputationException.class);
        throw new ComputationException(cause);
      }

      if (value == null) {
        throw new NullPointerException(loader + " returned null for key " + key + ".");
      }
      return value;
    }
  }

  // Serialization Support

  private static final long serialVersionUID = 4;

  @Override
  Object writeReplace() {
    return new ComputingSerializationProxy<K, V>(keyStrength, valueStrength, keyEquivalence,
        valueEquivalence, expireAfterWriteNanos, expireAfterAccessNanos, maximumSize,
        concurrencyLevel, removalListener, this, loader);
  }

  static final class ComputingSerializationProxy<K, V> extends AbstractSerializationProxy<K, V> {

    final CacheLoader<? super K, ? extends V> loader;

    ComputingSerializationProxy(Strength keyStrength, Strength valueStrength,
        Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence,
        long expireAfterWriteNanos, long expireAfterAccessNanos, int maximumSize,
        int concurrencyLevel, RemovalListener<? super K, ? super V> removalListener,
        ConcurrentMap<K, V> delegate, CacheLoader<? super K, ? extends V> loader) {
      super(keyStrength, valueStrength, keyEquivalence, valueEquivalence, expireAfterWriteNanos,
          expireAfterAccessNanos, maximumSize, concurrencyLevel, removalListener, delegate);
      this.loader = loader;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      writeMapTo(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      MapMaker mapMaker = readMapMaker(in);
      delegate = mapMaker.makeComputingMap(loader);
      readEntries(in);
    }

    Object readResolve() {
      return delegate;
    }

    private static final long serialVersionUID = 4;
  }
}
