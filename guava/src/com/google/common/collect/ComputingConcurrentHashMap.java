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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;
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
  Segment<K, V> createSegment(int initialCapacity, int maxSegmentSize) {
    return new ComputingSegment<K, V>(this, initialCapacity, maxSegmentSize);
  }

  @Override
  ComputingSegment<K, V> segmentFor(int hash) {
    return (ComputingSegment<K, V>) super.segmentFor(hash);
  }

  V compute(K key) {
    int hash = hash(key);
    return segmentFor(hash).compute(key, hash, computingFunction);
  }

  /**
   * Overrides get() to compute on demand. Also throws an exception when null is returned from a
   * computation.
   */
  static class ComputingMapAdapter<K, V>
      extends ComputingConcurrentHashMap<K, V> implements Serializable {
    private static final long serialVersionUID = 0;

    ComputingMapAdapter(MapMaker mapMaker, Function<? super K, ? extends V> computingFunction) {
      super(mapMaker, computingFunction);
    }

    @SuppressWarnings("unchecked") // unsafe, which is why this is deprecated
    @Override
    public V get(Object key) {
      V value = compute((K) key);
      if (value == null) {
        throw new NullPointerException(computingFunction + " returned null for key " + key + ".");
      }
      return value;
    }
  }

  @SuppressWarnings("serial") // This class is never serialized.
  static class ComputingSegment<K, V> extends Segment<K, V> {
    ComputingSegment(CustomConcurrentHashMap<K, V> map, int initialCapacity, int maxSegmentSize) {
      super(map, initialCapacity, maxSegmentSize);
    }

    V compute(K key, int hash, Function<? super K, ? extends V> computingFunction) {
      try {
        outer: while (true) {
          ReferenceEntry<K, V> e = getEntry(key, hash);
          if (e != null) {
            V value = getLiveValue(e);
            if (value != null) {
              // TODO(user): recordHit
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
                      // TODO(user): recordHit
                      return value;
                    }
                    // immediately reuse invalid entries
                    clearLiveEntry(e, hash, valueReference);
                  }
                  break;
                }
              }

              if (createNewEntry) {
                computingValueReference = new ComputingValueReference<K, V>(computingFunction);

                if (e == null) {
                  e = map.newEntry(key, hash, first);
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
                // Synchronizes on the entry to allow failing fast when a recursive computation is
                // detected. This is not fool-proof since the entry may be copied when the segment
                // is written to.
                synchronized (e) {
                  value = computingValueReference.compute(key, hash);
                }
                if (value != null) {
                  // putIfAbsent
                  put(key, hash, value, true);
                }
                return value;
              } finally {
                if (value == null) {
                  clearValue(key, hash, computingValueReference);
                }
              }
            }
          }

          // The entry already exists. Wait for the computation.
          checkState(!Thread.holdsLock(e), "Recursive computation");
          V value = e.getValueReference().waitForValue();
          // don't consider expiration as we're concurrent with computation
          if (value != null) {
            recordRead(e);
            // TODO(user): recordMiss
            return value;
          }
          // else computing thread will clearValue
          continue outer;
        }
      } finally {
        postReadCleanup();
      }
    }
  }

  /**
   * Used to provide computation exceptions to other threads.
   */
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
    public void clear(ValueReference<K, V> newValue) {}
  }

  /**
   * Used to provide computation result to other threads.
   */
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
    public void clear(ValueReference<K, V> newValue) {}
  }

  private static class ComputingValueReference<K, V> implements ValueReference<K, V> {
    final Function<? super K, ? extends V> computingFunction;

    @GuardedBy("ComputingValueReference.this") // writes
    volatile ValueReference<K, V> computedReference = unset();

    public ComputingValueReference(Function<? super K, ? extends V> computingFunction) {
      this.computingFunction = computingFunction;
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
    public V waitForValue() {
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

    @Override
    public void notifyValueReclaimed() {}

    V compute(K key, int hash) {
      V value;
      try {
        value = computingFunction.apply(key);
      } catch (ComputationException e) {
        // if computingFunction has thrown a computation exception,
        // propagate rather than wrap
        setValueReference(new ComputationExceptionReference<K, V>(e.getCause()));
        throw e;
      } catch (Throwable t) {
        setValueReference(new ComputationExceptionReference<K, V>(t));
        throw new ComputationException(t);
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

  // Serialization Support

  private static final long serialVersionUID = 2;

  @Override
  Object writeReplace() {
    return new ComputingSerializationProxy<K, V>(keyStrength, valueStrength, keyEquivalence,
        valueEquivalence, expireAfterWriteNanos, expireAfterAccessNanos, maximumSize,
        concurrencyLevel, evictionListener, this, computingFunction);
  }

  static class ComputingSerializationProxy<K, V> extends AbstractSerializationProxy<K, V> {

    final Function<? super K, ? extends V> computingFunction;

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
      delegate = mapMaker.makeComputingMap(computingFunction);
      readEntries(in);
    }

    Object readResolve() {
      return delegate;
    }

    private static final long serialVersionUID = 2;
  }
}
