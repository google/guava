/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkRemove;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Equivalence;
import com.google.common.collect.MapMaker.Dummy;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.google.j2objc.annotations.Weak;
import com.google.j2objc.annotations.WeakOuter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The concurrent hash map implementation built by {@link MapMaker}.
 *
 * <p>This implementation is heavily derived from revision 1.96 of <a
 * href="http://tinyurl.com/ConcurrentHashMap">ConcurrentHashMap.java</a>.
 *
 * @param <K> the type of the keys in the map
 * @param <V> the type of the values in the map
 * @param <E> the type of the {@link InternalEntry} entry implementation used internally
 * @param <S> the type of the {@link Segment} entry implementation used internally
 * @author Bob Lee
 * @author Charles Fry
 * @author Doug Lea ({@code ConcurrentHashMap})
 */
// TODO(kak): Consider removing @CanIgnoreReturnValue from this class.
@GwtIncompatible
@SuppressWarnings({
  "GuardedBy", // TODO(b/35466881): Fix or suppress.
  "nullness", // too much trouble for the payoff
})
// TODO(cpovirk): Annotate for nullness.
class MapMakerInternalMap<
        K,
        V,
        E extends MapMakerInternalMap.InternalEntry<K, V, E>,
        S extends MapMakerInternalMap.Segment<K, V, E, S>>
    extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Serializable {

  /*
   * The basic strategy is to subdivide the table among Segments, each of which itself is a
   * concurrently readable hash table. The map supports non-blocking reads and concurrent writes
   * across different segments.
   *
   * The page replacement algorithm's data structures are kept casually consistent with the map. The
   * ordering of writes to a segment is sequentially consistent. An update to the map and recording
   * of reads may not be immediately reflected on the algorithm's data structures. These structures
   * are guarded by a lock and operations are applied in batches to avoid lock contention. The
   * penalty of applying the batches is spread across threads so that the amortized cost is slightly
   * higher than performing just the operation without enforcing the capacity constraint.
   *
   * This implementation uses a per-segment queue to record a memento of the additions, removals,
   * and accesses that were performed on the map. The queue is drained on writes and when it exceeds
   * its capacity threshold.
   *
   * The Least Recently Used page replacement algorithm was chosen due to its simplicity, high hit
   * rate, and ability to be implemented with O(1) time complexity. The initial LRU implementation
   * operates per-segment rather than globally for increased implementation simplicity. We expect
   * the cache hit rate to be similar to that of a global LRU algorithm.
   */

  // Constants

  /**
   * The maximum capacity, used if a higher value is implicitly specified by either of the
   * constructors with arguments. MUST be a power of two no greater than {@code 1<<30} to ensure
   * that entries are indexable using ints.
   */
  static final int MAXIMUM_CAPACITY = Ints.MAX_POWER_OF_TWO;

  /** The maximum number of segments to allow; used to bound constructor arguments. */
  static final int MAX_SEGMENTS = 1 << 16; // slightly conservative

  /** Number of (unsynchronized) retries in the containsValue method. */
  static final int CONTAINS_VALUE_RETRIES = 3;

  /**
   * Number of cache access operations that can be buffered per segment before the cache's recency
   * ordering information is updated. This is used to avoid lock contention by recording a memento
   * of reads and delaying a lock acquisition until the threshold is crossed or a mutation occurs.
   *
   * <p>This must be a (2^n)-1 as it is used as a mask.
   */
  static final int DRAIN_THRESHOLD = 0x3F;

  /**
   * Maximum number of entries to be drained in a single cleanup run. This applies independently to
   * the cleanup queue and both reference queues.
   */
  // TODO(fry): empirically optimize this
  static final int DRAIN_MAX = 16;

  static final long CLEANUP_EXECUTOR_DELAY_SECS = 60;

  // Fields

  /**
   * Mask value for indexing into segments. The upper bits of a key's hash code are used to choose
   * the segment.
   */
  final transient int segmentMask;

  /**
   * Shift value for indexing within segments. Helps prevent entries that end up in the same segment
   * from also ending up in the same bucket.
   */
  final transient int segmentShift;

  /** The segments, each of which is a specialized hash table. */
  final transient Segment<K, V, E, S>[] segments;

  /** The concurrency level. */
  final int concurrencyLevel;

  /** Strategy for comparing keys. */
  final Equivalence<Object> keyEquivalence;

  /** Strategy for handling entries and segments in a type-safe and efficient manner. */
  final transient InternalEntryHelper<K, V, E, S> entryHelper;

  /**
   * Creates a new, empty map with the specified strategy, initial capacity and concurrency level.
   */
  private MapMakerInternalMap(MapMaker builder, InternalEntryHelper<K, V, E, S> entryHelper) {
    concurrencyLevel = Math.min(builder.getConcurrencyLevel(), MAX_SEGMENTS);

    keyEquivalence = builder.getKeyEquivalence();
    this.entryHelper = entryHelper;

    int initialCapacity = Math.min(builder.getInitialCapacity(), MAXIMUM_CAPACITY);

    // Find power-of-two sizes best matching arguments. Constraints:
    // (segmentCount > concurrencyLevel)
    int segmentShift = 0;
    int segmentCount = 1;
    while (segmentCount < concurrencyLevel) {
      ++segmentShift;
      segmentCount <<= 1;
    }
    this.segmentShift = 32 - segmentShift;
    segmentMask = segmentCount - 1;

    this.segments = newSegmentArray(segmentCount);

    int segmentCapacity = initialCapacity / segmentCount;
    if (segmentCapacity * segmentCount < initialCapacity) {
      ++segmentCapacity;
    }

    int segmentSize = 1;
    while (segmentSize < segmentCapacity) {
      segmentSize <<= 1;
    }

    for (int i = 0; i < this.segments.length; ++i) {
      this.segments[i] = createSegment(segmentSize, MapMaker.UNSET_INT);
    }
  }

  /** Returns a fresh {@link MapMakerInternalMap} as specified by the given {@code builder}. */
  static <K, V> MapMakerInternalMap<K, V, ? extends InternalEntry<K, V, ?>, ?> create(
      MapMaker builder) {
    if (builder.getKeyStrength() == Strength.STRONG
        && builder.getValueStrength() == Strength.STRONG) {
      return new MapMakerInternalMap<>(builder, StrongKeyStrongValueEntry.Helper.<K, V>instance());
    }
    if (builder.getKeyStrength() == Strength.STRONG
        && builder.getValueStrength() == Strength.WEAK) {
      return new MapMakerInternalMap<>(builder, StrongKeyWeakValueEntry.Helper.<K, V>instance());
    }
    if (builder.getKeyStrength() == Strength.WEAK
        && builder.getValueStrength() == Strength.STRONG) {
      return new MapMakerInternalMap<>(builder, WeakKeyStrongValueEntry.Helper.<K, V>instance());
    }
    if (builder.getKeyStrength() == Strength.WEAK && builder.getValueStrength() == Strength.WEAK) {
      return new MapMakerInternalMap<>(builder, WeakKeyWeakValueEntry.Helper.<K, V>instance());
    }
    throw new AssertionError();
  }

  /**
   * Returns a fresh {@link MapMakerInternalMap} with {@link MapMaker.Dummy} values but otherwise as
   * specified by the given {@code builder}. The returned {@link MapMakerInternalMap} will be
   * optimized to saved memory. Since {@link MapMaker.Dummy} is a singleton, we don't need to store
   * any values at all. Because of this optimization, {@code build.getValueStrength()} must be
   * {@link Strength#STRONG}.
   *
   * <p>This method is intended to only be used by the internal implementation of {@link Interners},
   * since a map of dummy values is the exact use case there.
   */
  static <K>
      MapMakerInternalMap<K, Dummy, ? extends InternalEntry<K, Dummy, ?>, ?> createWithDummyValues(
          MapMaker builder) {
    if (builder.getKeyStrength() == Strength.STRONG
        && builder.getValueStrength() == Strength.STRONG) {
      return new MapMakerInternalMap<>(builder, StrongKeyDummyValueEntry.Helper.<K>instance());
    }
    if (builder.getKeyStrength() == Strength.WEAK
        && builder.getValueStrength() == Strength.STRONG) {
      return new MapMakerInternalMap<>(builder, WeakKeyDummyValueEntry.Helper.<K>instance());
    }
    if (builder.getValueStrength() == Strength.WEAK) {
      throw new IllegalArgumentException("Map cannot have both weak and dummy values");
    }
    throw new AssertionError();
  }

  enum Strength {
    STRONG {
      @Override
      Equivalence<Object> defaultEquivalence() {
        return Equivalence.equals();
      }
    },

    WEAK {
      @Override
      Equivalence<Object> defaultEquivalence() {
        return Equivalence.identity();
      }
    };

    /**
     * Returns the default equivalence strategy used to compare and hash keys or values referenced
     * at this strength. This strategy will be used unless the user explicitly specifies an
     * alternate strategy.
     */
    abstract Equivalence<Object> defaultEquivalence();
  }

  /**
   * A helper object for operating on {@link InternalEntry} instances in a type-safe and efficient
   * manner.
   *
   * <p>For each of the four combinations of strong/weak key and strong/weak value, there are
   * corresponding {@link InternalEntry}, {@link Segment}, and {@link InternalEntryHelper}
   * implementations.
   *
   * @param <K> the type of the key in each entry
   * @param <V> the type of the value in each entry
   * @param <E> the type of the {@link InternalEntry} entry implementation
   * @param <S> the type of the {@link Segment} entry implementation
   */
  interface InternalEntryHelper<
      K, V, E extends InternalEntry<K, V, E>, S extends Segment<K, V, E, S>> {
    /** The strength of the key type in each entry. */
    Strength keyStrength();

    /** The strength of the value type in each entry. */
    Strength valueStrength();

    /** Returns a freshly created segment, typed at the {@code S} type. */
    S newSegment(MapMakerInternalMap<K, V, E, S> map, int initialCapacity, int maxSegmentSize);

    /**
     * Returns a freshly created entry, typed at the {@code E} type, for the given {@code segment}.
     */
    E newEntry(S segment, K key, int hash, @Nullable E next);

    /**
     * Returns a freshly created entry, typed at the {@code E} type, for the given {@code segment},
     * that is a copy of the given {@code entry}.
     */
    E copy(S segment, E entry, @Nullable E newNext);

    /**
     * Sets the value of the given {@code entry} in the given {@code segment} to be the given {@code
     * value}
     */
    void setValue(S segment, E entry, V value);
  }

  /**
   * An entry in a hash table of a {@link Segment}.
   *
   * <p>Entries in the map can be in the following states:
   *
   * <p>Valid: - Live: valid key/value are set
   *
   * <p>Invalid: - Collected: key/value was partially collected, but not yet cleaned up
   */
  interface InternalEntry<K, V, E extends InternalEntry<K, V, E>> {
    /** Gets the next entry in the chain. */
    E getNext();

    /** Gets the entry's hash. */
    int getHash();

    /** Gets the key for this entry. */
    K getKey();

    /** Gets the value for the entry. */
    V getValue();
  }

  /*
   * Note: the following classes have a lot of duplicate code. It sucks, but it saves a lot of
   * memory. If only Java had mixins!
   */

  /** Base class for {@link InternalEntry} implementations for strong keys. */
  abstract static class AbstractStrongKeyEntry<K, V, E extends InternalEntry<K, V, E>>
      implements InternalEntry<K, V, E> {
    final K key;
    final int hash;
    final @Nullable E next;

    AbstractStrongKeyEntry(K key, int hash, @Nullable E next) {
      this.key = key;
      this.hash = hash;
      this.next = next;
    }

    @Override
    public K getKey() {
      return this.key;
    }

    @Override
    public int getHash() {
      return hash;
    }

    @Override
    public E getNext() {
      return next;
    }
  }

  /** Marker interface for {@link InternalEntry} implementations for strong values. */
  interface StrongValueEntry<K, V, E extends InternalEntry<K, V, E>>
      extends InternalEntry<K, V, E> {}

  /** Marker interface for {@link InternalEntry} implementations for weak values. */
  interface WeakValueEntry<K, V, E extends InternalEntry<K, V, E>> extends InternalEntry<K, V, E> {
    /** Gets the weak value reference held by entry. */
    WeakValueReference<K, V, E> getValueReference();

    /**
     * Clears the weak value reference held by the entry. Should be used when the entry's value is
     * overwritten.
     */
    void clearValue();
  }

  @SuppressWarnings("unchecked") // impl never uses a parameter or returns any non-null value
  static <K, V, E extends InternalEntry<K, V, E>>
      WeakValueReference<K, V, E> unsetWeakValueReference() {
    return (WeakValueReference<K, V, E>) UNSET_WEAK_VALUE_REFERENCE;
  }

  /** Concrete implementation of {@link InternalEntry} for strong keys and strong values. */
  static final class StrongKeyStrongValueEntry<K, V>
      extends AbstractStrongKeyEntry<K, V, StrongKeyStrongValueEntry<K, V>>
      implements StrongValueEntry<K, V, StrongKeyStrongValueEntry<K, V>> {
    private volatile @Nullable V value = null;

    StrongKeyStrongValueEntry(K key, int hash, @Nullable StrongKeyStrongValueEntry<K, V> next) {
      super(key, hash, next);
    }

    @Override
    public @Nullable V getValue() {
      return value;
    }

    void setValue(V value) {
      this.value = value;
    }

    StrongKeyStrongValueEntry<K, V> copy(StrongKeyStrongValueEntry<K, V> newNext) {
      StrongKeyStrongValueEntry<K, V> newEntry =
          new StrongKeyStrongValueEntry<>(this.key, this.hash, newNext);
      newEntry.value = this.value;
      return newEntry;
    }

    /** Concrete implementation of {@link InternalEntryHelper} for strong keys and strong values. */
    static final class Helper<K, V>
        implements InternalEntryHelper<
            K, V, StrongKeyStrongValueEntry<K, V>, StrongKeyStrongValueSegment<K, V>> {
      private static final Helper<?, ?> INSTANCE = new Helper<>();

      @SuppressWarnings("unchecked")
      static <K, V> Helper<K, V> instance() {
        return (Helper<K, V>) INSTANCE;
      }

      @Override
      public Strength keyStrength() {
        return Strength.STRONG;
      }

      @Override
      public Strength valueStrength() {
        return Strength.STRONG;
      }

      @Override
      public StrongKeyStrongValueSegment<K, V> newSegment(
          MapMakerInternalMap<
                  K, V, StrongKeyStrongValueEntry<K, V>, StrongKeyStrongValueSegment<K, V>>
              map,
          int initialCapacity,
          int maxSegmentSize) {
        return new StrongKeyStrongValueSegment<>(map, initialCapacity, maxSegmentSize);
      }

      @Override
      public StrongKeyStrongValueEntry<K, V> copy(
          StrongKeyStrongValueSegment<K, V> segment,
          StrongKeyStrongValueEntry<K, V> entry,
          @Nullable StrongKeyStrongValueEntry<K, V> newNext) {
        return entry.copy(newNext);
      }

      @Override
      public void setValue(
          StrongKeyStrongValueSegment<K, V> segment,
          StrongKeyStrongValueEntry<K, V> entry,
          V value) {
        entry.setValue(value);
      }

      @Override
      public StrongKeyStrongValueEntry<K, V> newEntry(
          StrongKeyStrongValueSegment<K, V> segment,
          K key,
          int hash,
          @Nullable StrongKeyStrongValueEntry<K, V> next) {
        return new StrongKeyStrongValueEntry<>(key, hash, next);
      }
    }
  }

  /** Concrete implementation of {@link InternalEntry} for strong keys and weak values. */
  static final class StrongKeyWeakValueEntry<K, V>
      extends AbstractStrongKeyEntry<K, V, StrongKeyWeakValueEntry<K, V>>
      implements WeakValueEntry<K, V, StrongKeyWeakValueEntry<K, V>> {
    private volatile WeakValueReference<K, V, StrongKeyWeakValueEntry<K, V>> valueReference =
        unsetWeakValueReference();

    StrongKeyWeakValueEntry(K key, int hash, @Nullable StrongKeyWeakValueEntry<K, V> next) {
      super(key, hash, next);
    }

    @Override
    public V getValue() {
      return valueReference.get();
    }

    @Override
    public void clearValue() {
      valueReference.clear();
    }

    void setValue(V value, ReferenceQueue<V> queueForValues) {
      WeakValueReference<K, V, StrongKeyWeakValueEntry<K, V>> previous = this.valueReference;
      this.valueReference = new WeakValueReferenceImpl<>(queueForValues, value, this);
      previous.clear();
    }

    StrongKeyWeakValueEntry<K, V> copy(
        ReferenceQueue<V> queueForValues, StrongKeyWeakValueEntry<K, V> newNext) {
      StrongKeyWeakValueEntry<K, V> newEntry = new StrongKeyWeakValueEntry<>(key, hash, newNext);
      newEntry.valueReference = valueReference.copyFor(queueForValues, newEntry);
      return newEntry;
    }

    @Override
    public WeakValueReference<K, V, StrongKeyWeakValueEntry<K, V>> getValueReference() {
      return valueReference;
    }

    /** Concrete implementation of {@link InternalEntryHelper} for strong keys and weak values. */
    static final class Helper<K, V>
        implements InternalEntryHelper<
            K, V, StrongKeyWeakValueEntry<K, V>, StrongKeyWeakValueSegment<K, V>> {
      private static final Helper<?, ?> INSTANCE = new Helper<>();

      @SuppressWarnings("unchecked")
      static <K, V> Helper<K, V> instance() {
        return (Helper<K, V>) INSTANCE;
      }

      @Override
      public Strength keyStrength() {
        return Strength.STRONG;
      }

      @Override
      public Strength valueStrength() {
        return Strength.WEAK;
      }

      @Override
      public StrongKeyWeakValueSegment<K, V> newSegment(
          MapMakerInternalMap<K, V, StrongKeyWeakValueEntry<K, V>, StrongKeyWeakValueSegment<K, V>>
              map,
          int initialCapacity,
          int maxSegmentSize) {
        return new StrongKeyWeakValueSegment<>(map, initialCapacity, maxSegmentSize);
      }

      @Override
      public StrongKeyWeakValueEntry<K, V> copy(
          StrongKeyWeakValueSegment<K, V> segment,
          StrongKeyWeakValueEntry<K, V> entry,
          @Nullable StrongKeyWeakValueEntry<K, V> newNext) {
        if (Segment.isCollected(entry)) {
          return null;
        }
        return entry.copy(segment.queueForValues, newNext);
      }

      @Override
      public void setValue(
          StrongKeyWeakValueSegment<K, V> segment, StrongKeyWeakValueEntry<K, V> entry, V value) {
        entry.setValue(value, segment.queueForValues);
      }

      @Override
      public StrongKeyWeakValueEntry<K, V> newEntry(
          StrongKeyWeakValueSegment<K, V> segment,
          K key,
          int hash,
          @Nullable StrongKeyWeakValueEntry<K, V> next) {
        return new StrongKeyWeakValueEntry<>(key, hash, next);
      }
    }
  }

  /** Concrete implementation of {@link InternalEntry} for strong keys and {@link Dummy} values. */
  static final class StrongKeyDummyValueEntry<K>
      extends AbstractStrongKeyEntry<K, Dummy, StrongKeyDummyValueEntry<K>>
      implements StrongValueEntry<K, Dummy, StrongKeyDummyValueEntry<K>> {
    StrongKeyDummyValueEntry(K key, int hash, @Nullable StrongKeyDummyValueEntry<K> next) {
      super(key, hash, next);
    }

    @Override
    public Dummy getValue() {
      return Dummy.VALUE;
    }

    void setValue(Dummy value) {}

    StrongKeyDummyValueEntry<K> copy(StrongKeyDummyValueEntry<K> newNext) {
      return new StrongKeyDummyValueEntry<K>(this.key, this.hash, newNext);
    }

    /**
     * Concrete implementation of {@link InternalEntryHelper} for strong keys and {@link Dummy}
     * values.
     */
    static final class Helper<K>
        implements InternalEntryHelper<
            K, Dummy, StrongKeyDummyValueEntry<K>, StrongKeyDummyValueSegment<K>> {
      private static final Helper<?> INSTANCE = new Helper<>();

      @SuppressWarnings("unchecked")
      static <K> Helper<K> instance() {
        return (Helper<K>) INSTANCE;
      }

      @Override
      public Strength keyStrength() {
        return Strength.STRONG;
      }

      @Override
      public Strength valueStrength() {
        return Strength.STRONG;
      }

      @Override
      public StrongKeyDummyValueSegment<K> newSegment(
          MapMakerInternalMap<K, Dummy, StrongKeyDummyValueEntry<K>, StrongKeyDummyValueSegment<K>>
              map,
          int initialCapacity,
          int maxSegmentSize) {
        return new StrongKeyDummyValueSegment<K>(map, initialCapacity, maxSegmentSize);
      }

      @Override
      public StrongKeyDummyValueEntry<K> copy(
          StrongKeyDummyValueSegment<K> segment,
          StrongKeyDummyValueEntry<K> entry,
          @Nullable StrongKeyDummyValueEntry<K> newNext) {
        return entry.copy(newNext);
      }

      @Override
      public void setValue(
          StrongKeyDummyValueSegment<K> segment, StrongKeyDummyValueEntry<K> entry, Dummy value) {}

      @Override
      public StrongKeyDummyValueEntry<K> newEntry(
          StrongKeyDummyValueSegment<K> segment,
          K key,
          int hash,
          @Nullable StrongKeyDummyValueEntry<K> next) {
        return new StrongKeyDummyValueEntry<K>(key, hash, next);
      }
    }
  }

  /** Base class for {@link InternalEntry} implementations for weak keys. */
  abstract static class AbstractWeakKeyEntry<K, V, E extends InternalEntry<K, V, E>>
      extends WeakReference<K> implements InternalEntry<K, V, E> {
    final int hash;
    final @Nullable E next;

    AbstractWeakKeyEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable E next) {
      super(key, queue);
      this.hash = hash;
      this.next = next;
    }

    @Override
    public K getKey() {
      return get();
    }

    @Override
    public int getHash() {
      return hash;
    }

    @Override
    public E getNext() {
      return next;
    }
  }

  /** Concrete implementation of {@link InternalEntry} for weak keys and {@link Dummy} values. */
  static final class WeakKeyDummyValueEntry<K>
      extends AbstractWeakKeyEntry<K, Dummy, WeakKeyDummyValueEntry<K>>
      implements StrongValueEntry<K, Dummy, WeakKeyDummyValueEntry<K>> {
    WeakKeyDummyValueEntry(
        ReferenceQueue<K> queue, K key, int hash, @Nullable WeakKeyDummyValueEntry<K> next) {
      super(queue, key, hash, next);
    }

    @Override
    public Dummy getValue() {
      return Dummy.VALUE;
    }

    void setValue(Dummy value) {}

    WeakKeyDummyValueEntry<K> copy(
        ReferenceQueue<K> queueForKeys, WeakKeyDummyValueEntry<K> newNext) {
      return new WeakKeyDummyValueEntry<K>(queueForKeys, getKey(), this.hash, newNext);
    }

    /**
     * Concrete implementation of {@link InternalEntryHelper} for weak keys and {@link Dummy}
     * values.
     */
    static final class Helper<K>
        implements InternalEntryHelper<
            K, Dummy, WeakKeyDummyValueEntry<K>, WeakKeyDummyValueSegment<K>> {
      private static final Helper<?> INSTANCE = new Helper<>();

      @SuppressWarnings("unchecked")
      static <K> Helper<K> instance() {
        return (Helper<K>) INSTANCE;
      }

      @Override
      public Strength keyStrength() {
        return Strength.WEAK;
      }

      @Override
      public Strength valueStrength() {
        return Strength.STRONG;
      }

      @Override
      public WeakKeyDummyValueSegment<K> newSegment(
          MapMakerInternalMap<K, Dummy, WeakKeyDummyValueEntry<K>, WeakKeyDummyValueSegment<K>> map,
          int initialCapacity,
          int maxSegmentSize) {
        return new WeakKeyDummyValueSegment<K>(map, initialCapacity, maxSegmentSize);
      }

      @Override
      public WeakKeyDummyValueEntry<K> copy(
          WeakKeyDummyValueSegment<K> segment,
          WeakKeyDummyValueEntry<K> entry,
          @Nullable WeakKeyDummyValueEntry<K> newNext) {
        if (entry.getKey() == null) {
          // key collected
          return null;
        }
        return entry.copy(segment.queueForKeys, newNext);
      }

      @Override
      public void setValue(
          WeakKeyDummyValueSegment<K> segment, WeakKeyDummyValueEntry<K> entry, Dummy value) {}

      @Override
      public WeakKeyDummyValueEntry<K> newEntry(
          WeakKeyDummyValueSegment<K> segment,
          K key,
          int hash,
          @Nullable WeakKeyDummyValueEntry<K> next) {
        return new WeakKeyDummyValueEntry<K>(segment.queueForKeys, key, hash, next);
      }
    }
  }

  /** Concrete implementation of {@link InternalEntry} for weak keys and strong values. */
  static final class WeakKeyStrongValueEntry<K, V>
      extends AbstractWeakKeyEntry<K, V, WeakKeyStrongValueEntry<K, V>>
      implements StrongValueEntry<K, V, WeakKeyStrongValueEntry<K, V>> {
    private volatile @Nullable V value = null;

    WeakKeyStrongValueEntry(
        ReferenceQueue<K> queue, K key, int hash, @Nullable WeakKeyStrongValueEntry<K, V> next) {
      super(queue, key, hash, next);
    }

    @Override
    public @Nullable V getValue() {
      return value;
    }

    void setValue(V value) {
      this.value = value;
    }

    WeakKeyStrongValueEntry<K, V> copy(
        ReferenceQueue<K> queueForKeys, WeakKeyStrongValueEntry<K, V> newNext) {
      WeakKeyStrongValueEntry<K, V> newEntry =
          new WeakKeyStrongValueEntry<>(queueForKeys, getKey(), this.hash, newNext);
      newEntry.setValue(value);
      return newEntry;
    }

    /** Concrete implementation of {@link InternalEntryHelper} for weak keys and strong values. */
    static final class Helper<K, V>
        implements InternalEntryHelper<
            K, V, WeakKeyStrongValueEntry<K, V>, WeakKeyStrongValueSegment<K, V>> {
      private static final Helper<?, ?> INSTANCE = new Helper<>();

      @SuppressWarnings("unchecked")
      static <K, V> Helper<K, V> instance() {
        return (Helper<K, V>) INSTANCE;
      }

      @Override
      public Strength keyStrength() {
        return Strength.WEAK;
      }

      @Override
      public Strength valueStrength() {
        return Strength.STRONG;
      }

      @Override
      public WeakKeyStrongValueSegment<K, V> newSegment(
          MapMakerInternalMap<K, V, WeakKeyStrongValueEntry<K, V>, WeakKeyStrongValueSegment<K, V>>
              map,
          int initialCapacity,
          int maxSegmentSize) {
        return new WeakKeyStrongValueSegment<>(map, initialCapacity, maxSegmentSize);
      }

      @Override
      public WeakKeyStrongValueEntry<K, V> copy(
          WeakKeyStrongValueSegment<K, V> segment,
          WeakKeyStrongValueEntry<K, V> entry,
          @Nullable WeakKeyStrongValueEntry<K, V> newNext) {
        if (entry.getKey() == null) {
          // key collected
          return null;
        }
        return entry.copy(segment.queueForKeys, newNext);
      }

      @Override
      public void setValue(
          WeakKeyStrongValueSegment<K, V> segment, WeakKeyStrongValueEntry<K, V> entry, V value) {
        entry.setValue(value);
      }

      @Override
      public WeakKeyStrongValueEntry<K, V> newEntry(
          WeakKeyStrongValueSegment<K, V> segment,
          K key,
          int hash,
          @Nullable WeakKeyStrongValueEntry<K, V> next) {
        return new WeakKeyStrongValueEntry<>(segment.queueForKeys, key, hash, next);
      }
    }
  }

  /** Concrete implementation of {@link InternalEntry} for weak keys and weak values. */
  static final class WeakKeyWeakValueEntry<K, V>
      extends AbstractWeakKeyEntry<K, V, WeakKeyWeakValueEntry<K, V>>
      implements WeakValueEntry<K, V, WeakKeyWeakValueEntry<K, V>> {
    private volatile WeakValueReference<K, V, WeakKeyWeakValueEntry<K, V>> valueReference =
        unsetWeakValueReference();

    WeakKeyWeakValueEntry(
        ReferenceQueue<K> queue, K key, int hash, @Nullable WeakKeyWeakValueEntry<K, V> next) {
      super(queue, key, hash, next);
    }

    @Override
    public V getValue() {
      return valueReference.get();
    }

    WeakKeyWeakValueEntry<K, V> copy(
        ReferenceQueue<K> queueForKeys,
        ReferenceQueue<V> queueForValues,
        WeakKeyWeakValueEntry<K, V> newNext) {
      WeakKeyWeakValueEntry<K, V> newEntry =
          new WeakKeyWeakValueEntry<>(queueForKeys, getKey(), this.hash, newNext);
      newEntry.valueReference = valueReference.copyFor(queueForValues, newEntry);
      return newEntry;
    }

    @Override
    public void clearValue() {
      valueReference.clear();
    }

    void setValue(V value, ReferenceQueue<V> queueForValues) {
      WeakValueReference<K, V, WeakKeyWeakValueEntry<K, V>> previous = this.valueReference;
      this.valueReference = new WeakValueReferenceImpl<>(queueForValues, value, this);
      previous.clear();
    }

    @Override
    public WeakValueReference<K, V, WeakKeyWeakValueEntry<K, V>> getValueReference() {
      return valueReference;
    }

    /** Concrete implementation of {@link InternalEntryHelper} for weak keys and weak values. */
    static final class Helper<K, V>
        implements InternalEntryHelper<
            K, V, WeakKeyWeakValueEntry<K, V>, WeakKeyWeakValueSegment<K, V>> {
      private static final Helper<?, ?> INSTANCE = new Helper<>();

      @SuppressWarnings("unchecked")
      static <K, V> Helper<K, V> instance() {
        return (Helper<K, V>) INSTANCE;
      }

      @Override
      public Strength keyStrength() {
        return Strength.WEAK;
      }

      @Override
      public Strength valueStrength() {
        return Strength.WEAK;
      }

      @Override
      public WeakKeyWeakValueSegment<K, V> newSegment(
          MapMakerInternalMap<K, V, WeakKeyWeakValueEntry<K, V>, WeakKeyWeakValueSegment<K, V>> map,
          int initialCapacity,
          int maxSegmentSize) {
        return new WeakKeyWeakValueSegment<>(map, initialCapacity, maxSegmentSize);
      }

      @Override
      public WeakKeyWeakValueEntry<K, V> copy(
          WeakKeyWeakValueSegment<K, V> segment,
          WeakKeyWeakValueEntry<K, V> entry,
          @Nullable WeakKeyWeakValueEntry<K, V> newNext) {
        if (entry.getKey() == null) {
          // key collected
          return null;
        }
        if (Segment.isCollected(entry)) {
          return null;
        }
        return entry.copy(segment.queueForKeys, segment.queueForValues, newNext);
      }

      @Override
      public void setValue(
          WeakKeyWeakValueSegment<K, V> segment, WeakKeyWeakValueEntry<K, V> entry, V value) {
        entry.setValue(value, segment.queueForValues);
      }

      @Override
      public WeakKeyWeakValueEntry<K, V> newEntry(
          WeakKeyWeakValueSegment<K, V> segment,
          K key,
          int hash,
          @Nullable WeakKeyWeakValueEntry<K, V> next) {
        return new WeakKeyWeakValueEntry<>(segment.queueForKeys, key, hash, next);
      }
    }
  }

  /** A weakly referenced value that also has a reference to its containing entry. */
  interface WeakValueReference<K, V, E extends InternalEntry<K, V, E>> {
    /**
     * Returns the current value being referenced, or {@code null} if there is none (e.g. because
     * either it got collected, or {@link #clear} was called, or it wasn't set in the first place).
     */
    @Nullable
    V get();

    /** Returns the entry which contains this {@link WeakValueReference}. */
    E getEntry();

    /** Unsets the referenced value. Subsequent calls to {@link #get} will return {@code null}. */
    void clear();

    /**
     * Returns a freshly created {@link WeakValueReference} for the given {@code entry} (and on the
     * given {@code queue} with the same value as this {@link WeakValueReference}.
     */
    WeakValueReference<K, V, E> copyFor(ReferenceQueue<V> queue, E entry);
  }

  /**
   * A dummy implementation of {@link InternalEntry}, solely for use in the type signature of {@link
   * #UNSET_WEAK_VALUE_REFERENCE} below.
   */
  static final class DummyInternalEntry
      implements InternalEntry<Object, Object, DummyInternalEntry> {
    private DummyInternalEntry() {
      throw new AssertionError();
    }

    @Override
    public DummyInternalEntry getNext() {
      throw new AssertionError();
    }

    @Override
    public int getHash() {
      throw new AssertionError();
    }

    @Override
    public Object getKey() {
      throw new AssertionError();
    }

    @Override
    public Object getValue() {
      throw new AssertionError();
    }
  }

  /**
   * A singleton {@link WeakValueReference} used to denote an unset value in a entry with weak
   * values.
   */
  static final WeakValueReference<Object, Object, DummyInternalEntry> UNSET_WEAK_VALUE_REFERENCE =
      new WeakValueReference<Object, Object, DummyInternalEntry>() {
        @Override
        public DummyInternalEntry getEntry() {
          return null;
        }

        @Override
        public void clear() {}

        @Override
        public Object get() {
          return null;
        }

        @Override
        public WeakValueReference<Object, Object, DummyInternalEntry> copyFor(
            ReferenceQueue<Object> queue, DummyInternalEntry entry) {
          return this;
        }
      };

  /** Concrete implementation of {@link WeakValueReference}. */
  static final class WeakValueReferenceImpl<K, V, E extends InternalEntry<K, V, E>>
      extends WeakReference<V> implements WeakValueReference<K, V, E> {
    @Weak final E entry;

    WeakValueReferenceImpl(ReferenceQueue<V> queue, V referent, E entry) {
      super(referent, queue);
      this.entry = entry;
    }

    @Override
    public E getEntry() {
      return entry;
    }

    @Override
    public WeakValueReference<K, V, E> copyFor(ReferenceQueue<V> queue, E entry) {
      return new WeakValueReferenceImpl<>(queue, get(), entry);
    }
  }

  /**
   * Applies a supplemental hash function to a given hash code, which defends against poor quality
   * hash functions. This is critical when the concurrent hash map uses power-of-two length hash
   * tables, that otherwise encounter collisions for hash codes that do not differ in lower or upper
   * bits.
   *
   * @param h hash code
   */
  static int rehash(int h) {
    // Spread bits to regularize both segment and index locations,
    // using variant of single-word Wang/Jenkins hash.
    // TODO(kevinb): use Hashing/move this to Hashing?
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }

  /**
   * This method is a convenience for testing. Code should call {@link Segment#copyEntry} directly.
   */
  // Guarded By Segment.this
  @VisibleForTesting
  E copyEntry(E original, E newNext) {
    int hash = original.getHash();
    return segmentFor(hash).copyEntry(original, newNext);
  }

  int hash(Object key) {
    int h = keyEquivalence.hash(key);
    return rehash(h);
  }

  void reclaimValue(WeakValueReference<K, V, E> valueReference) {
    E entry = valueReference.getEntry();
    int hash = entry.getHash();
    segmentFor(hash).reclaimValue(entry.getKey(), hash, valueReference);
  }

  void reclaimKey(E entry) {
    int hash = entry.getHash();
    segmentFor(hash).reclaimKey(entry, hash);
  }

  /**
   * This method is a convenience for testing. Code should call {@link Segment#getLiveValue}
   * instead.
   */
  @VisibleForTesting
  boolean isLiveForTesting(InternalEntry<K, V, ?> entry) {
    return segmentFor(entry.getHash()).getLiveValueForTesting(entry) != null;
  }

  /**
   * Returns the segment that should be used for a key with the given hash.
   *
   * @param hash the hash code for the key
   * @return the segment
   */
  Segment<K, V, E, S> segmentFor(int hash) {
    // TODO(fry): Lazily create segments?
    return segments[(hash >>> segmentShift) & segmentMask];
  }

  Segment<K, V, E, S> createSegment(int initialCapacity, int maxSegmentSize) {
    return entryHelper.newSegment(this, initialCapacity, maxSegmentSize);
  }

  /**
   * Gets the value from an entry. Returns {@code null} if the entry is invalid, partially-collected
   * or computing.
   */
  V getLiveValue(E entry) {
    if (entry.getKey() == null) {
      return null;
    }
    return entry.getValue();
  }

  @SuppressWarnings("unchecked")
  final Segment<K, V, E, S>[] newSegmentArray(int ssize) {
    return new Segment[ssize];
  }

  // Inner Classes

  /**
   * Segments are specialized versions of hash tables. This subclass inherits from ReentrantLock
   * opportunistically, just to simplify some locking and avoid separate construction.
   */
  @SuppressWarnings("serial") // This class is never serialized.
  abstract static class Segment<
          K, V, E extends InternalEntry<K, V, E>, S extends Segment<K, V, E, S>>
      extends ReentrantLock {

    /*
     * Segments maintain a table of entry lists that are ALWAYS kept in a consistent state, so can
     * be read without locking. Next fields of nodes are immutable (final). All list additions are
     * performed at the front of each bin. This makes it easy to check changes, and also fast to
     * traverse. When nodes would otherwise be changed, new nodes are created to replace them. This
     * works well for hash tables since the bin lists tend to be short. (The average length is less
     * than two.)
     *
     * Read operations can thus proceed without locking, but rely on selected uses of volatiles to
     * ensure that completed write operations performed by other threads are noticed. For most
     * purposes, the "count" field, tracking the number of elements, serves as that volatile
     * variable ensuring visibility. This is convenient because this field needs to be read in many
     * read operations anyway:
     *
     * - All (unsynchronized) read operations must first read the "count" field, and should not
     * look at table entries if it is 0.
     *
     * - All (synchronized) write operations should write to the "count" field after structurally
     * changing any bin. The operations must not take any action that could even momentarily
     * cause a concurrent read operation to see inconsistent data. This is made easier by the
     * nature of the read operations in Map. For example, no operation can reveal that the table
     * has grown but the threshold has not yet been updated, so there are no atomicity requirements
     * for this with respect to reads.
     *
     * As a guide, all critical volatile reads and writes to the count field are marked in code
     * comments.
     */

    @Weak final MapMakerInternalMap<K, V, E, S> map;

    /**
     * The number of live elements in this segment's region. This does not include unset elements
     * which are awaiting cleanup.
     */
    volatile int count;

    /**
     * Number of updates that alter the size of the table. This is used during bulk-read methods to
     * make sure they see a consistent snapshot: If modCounts change during a traversal of segments
     * computing size or checking containsValue, then we might have an inconsistent view of state so
     * (usually) must retry.
     */
    int modCount;

    /**
     * The table is expanded when its size exceeds this threshold. (The value of this field is
     * always {@code (int) (capacity * 0.75)}.)
     */
    int threshold;

    /** The per-segment table. */
    volatile @Nullable AtomicReferenceArray<E> table;

    /** The maximum size of this map. MapMaker.UNSET_INT if there is no maximum. */
    final int maxSegmentSize;

    /**
     * A counter of the number of reads since the last write, used to drain queues on a small
     * fraction of read operations.
     */
    final AtomicInteger readCount = new AtomicInteger();

    Segment(MapMakerInternalMap<K, V, E, S> map, int initialCapacity, int maxSegmentSize) {
      this.map = map;
      this.maxSegmentSize = maxSegmentSize;
      initTable(newEntryArray(initialCapacity));
    }

    /**
     * Returns {@code this} up-casted to the specific {@link Segment} implementation type {@code S}.
     *
     * <p>This method exists so that the {@link Segment} code can be generic in terms of {@code S},
     * the type of the concrete implementation.
     */
    abstract S self();

    /** Drains the reference queues used by this segment, if any. */
    @GuardedBy("this")
    void maybeDrainReferenceQueues() {}

    /** Clears the reference queues used by this segment, if any. */
    void maybeClearReferenceQueues() {}

    /** Sets the value of the given {@code entry}. */
    void setValue(E entry, V value) {
      this.map.entryHelper.setValue(self(), entry, value);
    }

    /** Returns a copy of the given {@code entry}. */
    E copyEntry(E original, E newNext) {
      return this.map.entryHelper.copy(self(), original, newNext);
    }

    AtomicReferenceArray<E> newEntryArray(int size) {
      return new AtomicReferenceArray<E>(size);
    }

    void initTable(AtomicReferenceArray<E> newTable) {
      this.threshold = newTable.length() * 3 / 4; // 0.75
      if (this.threshold == maxSegmentSize) {
        // prevent spurious expansion before eviction
        this.threshold++;
      }
      this.table = newTable;
    }

    // Convenience methods for testing

    /**
     * Unsafe cast of the given entry to {@code E}, the type of the specific {@link InternalEntry}
     * implementation type.
     *
     * <p>This method is provided as a convenience for tests. Otherwise they'd need to be
     * knowledgable about all the implementation details of our type system trickery.
     */
    abstract E castForTesting(InternalEntry<K, V, ?> entry);

    /** Unsafely extracts the key reference queue used by this segment. */
    ReferenceQueue<K> getKeyReferenceQueueForTesting() {
      throw new AssertionError();
    }

    /** Unsafely extracts the value reference queue used by this segment. */
    ReferenceQueue<V> getValueReferenceQueueForTesting() {
      throw new AssertionError();
    }

    /** Unsafely extracts the weak value reference inside of the given {@code entry}. */
    WeakValueReference<K, V, E> getWeakValueReferenceForTesting(InternalEntry<K, V, ?> entry) {
      throw new AssertionError();
    }

    /**
     * Unsafely creates of a fresh {@link WeakValueReference}, referencing the given {@code value},
     * for the given {@code entry}
     */
    WeakValueReference<K, V, E> newWeakValueReferenceForTesting(
        InternalEntry<K, V, ?> entry, V value) {
      throw new AssertionError();
    }

    /**
     * Unsafely sets the weak value reference inside the given {@code entry} to be the given {@code
     * valueReference}
     */
    void setWeakValueReferenceForTesting(
        InternalEntry<K, V, ?> entry,
        WeakValueReference<K, V, ? extends InternalEntry<K, V, ?>> valueReference) {
      throw new AssertionError();
    }

    /**
     * Unsafely sets the given index of this segment's internal hash table to be the given entry.
     */
    void setTableEntryForTesting(int i, InternalEntry<K, V, ?> entry) {
      table.set(i, castForTesting(entry));
    }

    /** Unsafely returns a copy of the given entry. */
    E copyForTesting(InternalEntry<K, V, ?> entry, @Nullable InternalEntry<K, V, ?> newNext) {
      return this.map.entryHelper.copy(self(), castForTesting(entry), castForTesting(newNext));
    }

    /** Unsafely sets the value of the given entry. */
    void setValueForTesting(InternalEntry<K, V, ?> entry, V value) {
      this.map.entryHelper.setValue(self(), castForTesting(entry), value);
    }

    /** Unsafely returns a fresh entry. */
    E newEntryForTesting(K key, int hash, @Nullable InternalEntry<K, V, ?> next) {
      return this.map.entryHelper.newEntry(self(), key, hash, castForTesting(next));
    }

    /** Unsafely removes the given entry from this segment's hash table. */
    @CanIgnoreReturnValue
    boolean removeTableEntryForTesting(InternalEntry<K, V, ?> entry) {
      return removeEntryForTesting(castForTesting(entry));
    }

    /** Unsafely removes the given entry from the given chain in this segment's hash table. */
    E removeFromChainForTesting(InternalEntry<K, V, ?> first, InternalEntry<K, V, ?> entry) {
      return removeFromChain(castForTesting(first), castForTesting(entry));
    }

    /**
     * Unsafely returns the value of the given entry if it's still live, or {@code null} otherwise.
     */
    @Nullable
    V getLiveValueForTesting(InternalEntry<K, V, ?> entry) {
      return getLiveValue(castForTesting(entry));
    }

    // reference queues, for garbage collection cleanup

    /** Cleanup collected entries when the lock is available. */
    void tryDrainReferenceQueues() {
      if (tryLock()) {
        try {
          maybeDrainReferenceQueues();
        } finally {
          unlock();
        }
      }
    }

    @GuardedBy("this")
    void drainKeyReferenceQueue(ReferenceQueue<K> keyReferenceQueue) {
      Reference<? extends K> ref;
      int i = 0;
      while ((ref = keyReferenceQueue.poll()) != null) {
        @SuppressWarnings("unchecked")
        E entry = (E) ref;
        map.reclaimKey(entry);
        if (++i == DRAIN_MAX) {
          break;
        }
      }
    }

    @GuardedBy("this")
    void drainValueReferenceQueue(ReferenceQueue<V> valueReferenceQueue) {
      Reference<? extends V> ref;
      int i = 0;
      while ((ref = valueReferenceQueue.poll()) != null) {
        @SuppressWarnings("unchecked")
        WeakValueReference<K, V, E> valueReference = (WeakValueReference<K, V, E>) ref;
        map.reclaimValue(valueReference);
        if (++i == DRAIN_MAX) {
          break;
        }
      }
    }

    <T> void clearReferenceQueue(ReferenceQueue<T> referenceQueue) {
      while (referenceQueue.poll() != null) {}
    }

    /** Returns first entry of bin for given hash. */
    E getFirst(int hash) {
      // read this volatile field only once
      AtomicReferenceArray<E> table = this.table;
      return table.get(hash & (table.length() - 1));
    }

    // Specialized implementations of map methods

    E getEntry(Object key, int hash) {
      if (count != 0) { // read-volatile
        for (E e = getFirst(hash); e != null; e = e.getNext()) {
          if (e.getHash() != hash) {
            continue;
          }

          K entryKey = e.getKey();
          if (entryKey == null) {
            tryDrainReferenceQueues();
            continue;
          }

          if (map.keyEquivalence.equivalent(key, entryKey)) {
            return e;
          }
        }
      }

      return null;
    }

    E getLiveEntry(Object key, int hash) {
      return getEntry(key, hash);
    }

    V get(Object key, int hash) {
      try {
        E e = getLiveEntry(key, hash);
        if (e == null) {
          return null;
        }

        V value = e.getValue();
        if (value == null) {
          tryDrainReferenceQueues();
        }
        return value;
      } finally {
        postReadCleanup();
      }
    }

    boolean containsKey(Object key, int hash) {
      try {
        if (count != 0) { // read-volatile
          E e = getLiveEntry(key, hash);
          return e != null && e.getValue() != null;
        }

        return false;
      } finally {
        postReadCleanup();
      }
    }

    /**
     * This method is a convenience for testing. Code should call {@link
     * MapMakerInternalMap#containsValue} directly.
     */
    @VisibleForTesting
    boolean containsValue(Object value) {
      try {
        if (count != 0) { // read-volatile
          AtomicReferenceArray<E> table = this.table;
          int length = table.length();
          for (int i = 0; i < length; ++i) {
            for (E e = table.get(i); e != null; e = e.getNext()) {
              V entryValue = getLiveValue(e);
              if (entryValue == null) {
                continue;
              }
              if (map.valueEquivalence().equivalent(value, entryValue)) {
                return true;
              }
            }
          }
        }

        return false;
      } finally {
        postReadCleanup();
      }
    }

    V put(K key, int hash, V value, boolean onlyIfAbsent) {
      lock();
      try {
        preWriteCleanup();

        int newCount = this.count + 1;
        if (newCount > this.threshold) { // ensure capacity
          expand();
          newCount = this.count + 1;
        }

        AtomicReferenceArray<E> table = this.table;
        int index = hash & (table.length() - 1);
        E first = table.get(index);

        // Look for an existing entry.
        for (E e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            // We found an existing entry.

            V entryValue = e.getValue();

            if (entryValue == null) {
              ++modCount;
              setValue(e, value);
              newCount = this.count; // count remains unchanged
              this.count = newCount; // write-volatile
              return null;
            } else if (onlyIfAbsent) {
              // Mimic
              // "if (!map.containsKey(key)) ...
              // else return map.get(key);
              return entryValue;
            } else {
              // clobber existing entry, count remains unchanged
              ++modCount;
              setValue(e, value);
              return entryValue;
            }
          }
        }

        // Create a new entry.
        ++modCount;
        E newEntry = map.entryHelper.newEntry(self(), key, hash, first);
        setValue(newEntry, value);
        table.set(index, newEntry);
        this.count = newCount; // write-volatile
        return null;
      } finally {
        unlock();
      }
    }

    /** Expands the table if possible. */
    @GuardedBy("this")
    void expand() {
      AtomicReferenceArray<E> oldTable = table;
      int oldCapacity = oldTable.length();
      if (oldCapacity >= MAXIMUM_CAPACITY) {
        return;
      }

      /*
       * Reclassify nodes in each list to new Map. Because we are using power-of-two expansion, the
       * elements from each bin must either stay at same index, or move with a power of two offset.
       * We eliminate unnecessary node creation by catching cases where old nodes can be reused
       * because their next fields won't change. Statistically, at the default threshold, only
       * about one-sixth of them need cloning when a table doubles. The nodes they replace will be
       * garbage collectable as soon as they are no longer referenced by any reader thread that may
       * be in the midst of traversing table right now.
       */

      int newCount = count;
      AtomicReferenceArray<E> newTable = newEntryArray(oldCapacity << 1);
      threshold = newTable.length() * 3 / 4;
      int newMask = newTable.length() - 1;
      for (int oldIndex = 0; oldIndex < oldCapacity; ++oldIndex) {
        // We need to guarantee that any existing reads of old Map can
        // proceed. So we cannot yet null out each bin.
        E head = oldTable.get(oldIndex);

        if (head != null) {
          E next = head.getNext();
          int headIndex = head.getHash() & newMask;

          // Single node on list
          if (next == null) {
            newTable.set(headIndex, head);
          } else {
            // Reuse the consecutive sequence of nodes with the same target
            // index from the end of the list. tail points to the first
            // entry in the reusable list.
            E tail = head;
            int tailIndex = headIndex;
            for (E e = next; e != null; e = e.getNext()) {
              int newIndex = e.getHash() & newMask;
              if (newIndex != tailIndex) {
                // The index changed. We'll need to copy the previous entry.
                tailIndex = newIndex;
                tail = e;
              }
            }
            newTable.set(tailIndex, tail);

            // Clone nodes leading up to the tail.
            for (E e = head; e != tail; e = e.getNext()) {
              int newIndex = e.getHash() & newMask;
              E newNext = newTable.get(newIndex);
              E newFirst = copyEntry(e, newNext);
              if (newFirst != null) {
                newTable.set(newIndex, newFirst);
              } else {
                newCount--;
              }
            }
          }
        }
      }
      table = newTable;
      this.count = newCount;
    }

    boolean replace(K key, int hash, V oldValue, V newValue) {
      lock();
      try {
        preWriteCleanup();

        AtomicReferenceArray<E> table = this.table;
        int index = hash & (table.length() - 1);
        E first = table.get(index);

        for (E e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            // If the value disappeared, this entry is partially collected,
            // and we should pretend like it doesn't exist.
            V entryValue = e.getValue();
            if (entryValue == null) {
              if (isCollected(e)) {
                int newCount = this.count - 1;
                ++modCount;
                E newFirst = removeFromChain(first, e);
                newCount = this.count - 1;
                table.set(index, newFirst);
                this.count = newCount; // write-volatile
              }
              return false;
            }

            if (map.valueEquivalence().equivalent(oldValue, entryValue)) {
              ++modCount;
              setValue(e, newValue);
              return true;
            } else {
              // Mimic
              // "if (map.containsKey(key) && map.get(key).equals(oldValue))..."
              return false;
            }
          }
        }

        return false;
      } finally {
        unlock();
      }
    }

    V replace(K key, int hash, V newValue) {
      lock();
      try {
        preWriteCleanup();

        AtomicReferenceArray<E> table = this.table;
        int index = hash & (table.length() - 1);
        E first = table.get(index);

        for (E e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            // If the value disappeared, this entry is partially collected,
            // and we should pretend like it doesn't exist.
            V entryValue = e.getValue();
            if (entryValue == null) {
              if (isCollected(e)) {
                int newCount = this.count - 1;
                ++modCount;
                E newFirst = removeFromChain(first, e);
                newCount = this.count - 1;
                table.set(index, newFirst);
                this.count = newCount; // write-volatile
              }
              return null;
            }

            ++modCount;
            setValue(e, newValue);
            return entryValue;
          }
        }

        return null;
      } finally {
        unlock();
      }
    }

    @CanIgnoreReturnValue
    V remove(Object key, int hash) {
      lock();
      try {
        preWriteCleanup();

        int newCount = this.count - 1;
        AtomicReferenceArray<E> table = this.table;
        int index = hash & (table.length() - 1);
        E first = table.get(index);

        for (E e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            V entryValue = e.getValue();

            if (entryValue != null) {
              // TODO(kak): Remove this branch
            } else if (isCollected(e)) {
              // TODO(kak): Remove this branch
            } else {
              return null;
            }

            ++modCount;
            E newFirst = removeFromChain(first, e);
            newCount = this.count - 1;
            table.set(index, newFirst);
            this.count = newCount; // write-volatile
            return entryValue;
          }
        }

        return null;
      } finally {
        unlock();
      }
    }

    boolean remove(Object key, int hash, Object value) {
      lock();
      try {
        preWriteCleanup();

        int newCount = this.count - 1;
        AtomicReferenceArray<E> table = this.table;
        int index = hash & (table.length() - 1);
        E first = table.get(index);

        for (E e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            V entryValue = e.getValue();

            boolean explicitRemoval = false;
            if (map.valueEquivalence().equivalent(value, entryValue)) {
              explicitRemoval = true;
            } else if (isCollected(e)) {
              // TODO(kak): Remove this branch
            } else {
              return false;
            }

            ++modCount;
            E newFirst = removeFromChain(first, e);
            newCount = this.count - 1;
            table.set(index, newFirst);
            this.count = newCount; // write-volatile
            return explicitRemoval;
          }
        }

        return false;
      } finally {
        unlock();
      }
    }

    void clear() {
      if (count != 0) {
        lock();
        try {
          AtomicReferenceArray<E> table = this.table;
          for (int i = 0; i < table.length(); ++i) {
            table.set(i, null);
          }
          maybeClearReferenceQueues();
          readCount.set(0);

          ++modCount;
          count = 0; // write-volatile
        } finally {
          unlock();
        }
      }
    }

    /**
     * Removes an entry from within a table. All entries following the removed node can stay, but
     * all preceding ones need to be cloned.
     *
     * <p>This method does not decrement count for the removed entry, but does decrement count for
     * all partially collected entries which are skipped. As such callers which are modifying count
     * must re-read it after calling removeFromChain.
     *
     * @param first the first entry of the table
     * @param entry the entry being removed from the table
     * @return the new first entry for the table
     */
    @GuardedBy("this")
    E removeFromChain(E first, E entry) {
      int newCount = count;
      E newFirst = entry.getNext();
      for (E e = first; e != entry; e = e.getNext()) {
        E next = copyEntry(e, newFirst);
        if (next != null) {
          newFirst = next;
        } else {
          newCount--;
        }
      }
      this.count = newCount;
      return newFirst;
    }

    /** Removes an entry whose key has been garbage collected. */
    @CanIgnoreReturnValue
    boolean reclaimKey(E entry, int hash) {
      lock();
      try {
        int newCount = count - 1;
        AtomicReferenceArray<E> table = this.table;
        int index = hash & (table.length() - 1);
        E first = table.get(index);

        for (E e = first; e != null; e = e.getNext()) {
          if (e == entry) {
            ++modCount;
            E newFirst = removeFromChain(first, e);
            newCount = this.count - 1;
            table.set(index, newFirst);
            this.count = newCount; // write-volatile
            return true;
          }
        }

        return false;
      } finally {
        unlock();
      }
    }

    /** Removes an entry whose value has been garbage collected. */
    @CanIgnoreReturnValue
    boolean reclaimValue(K key, int hash, WeakValueReference<K, V, E> valueReference) {
      lock();
      try {
        int newCount = this.count - 1;
        AtomicReferenceArray<E> table = this.table;
        int index = hash & (table.length() - 1);
        E first = table.get(index);

        for (E e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            WeakValueReference<K, V, E> v = ((WeakValueEntry<K, V, E>) e).getValueReference();
            if (v == valueReference) {
              ++modCount;
              E newFirst = removeFromChain(first, e);
              newCount = this.count - 1;
              table.set(index, newFirst);
              this.count = newCount; // write-volatile
              return true;
            }
            return false;
          }
        }

        return false;
      } finally {
        unlock();
      }
    }

    /** Clears a value that has not yet been set, and thus does not require count to be modified. */
    @CanIgnoreReturnValue
    boolean clearValueForTesting(
        K key,
        int hash,
        WeakValueReference<K, V, ? extends InternalEntry<K, V, ?>> valueReference) {
      lock();
      try {
        AtomicReferenceArray<E> table = this.table;
        int index = hash & (table.length() - 1);
        E first = table.get(index);

        for (E e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            WeakValueReference<K, V, E> v = ((WeakValueEntry<K, V, E>) e).getValueReference();
            if (v == valueReference) {
              E newFirst = removeFromChain(first, e);
              table.set(index, newFirst);
              return true;
            }
            return false;
          }
        }

        return false;
      } finally {
        unlock();
      }
    }

    @GuardedBy("this")
    boolean removeEntryForTesting(E entry) {
      int hash = entry.getHash();
      int newCount = this.count - 1;
      AtomicReferenceArray<E> table = this.table;
      int index = hash & (table.length() - 1);
      E first = table.get(index);

      for (E e = first; e != null; e = e.getNext()) {
        if (e == entry) {
          ++modCount;
          E newFirst = removeFromChain(first, e);
          newCount = this.count - 1;
          table.set(index, newFirst);
          this.count = newCount; // write-volatile
          return true;
        }
      }

      return false;
    }

    /**
     * Returns {@code true} if the value has been partially collected, meaning that the value is
     * null.
     */
    static <K, V, E extends InternalEntry<K, V, E>> boolean isCollected(E entry) {
      return entry.getValue() == null;
    }

    /**
     * Gets the value from an entry. Returns {@code null} if the entry is invalid or
     * partially-collected.
     */
    @Nullable
    V getLiveValue(E entry) {
      if (entry.getKey() == null) {
        tryDrainReferenceQueues();
        return null;
      }
      V value = entry.getValue();
      if (value == null) {
        tryDrainReferenceQueues();
        return null;
      }

      return value;
    }

    /**
     * Performs routine cleanup following a read. Normally cleanup happens during writes, or from
     * the cleanupExecutor. If cleanup is not observed after a sufficient number of reads, try
     * cleaning up from the read thread.
     */
    void postReadCleanup() {
      if ((readCount.incrementAndGet() & DRAIN_THRESHOLD) == 0) {
        runCleanup();
      }
    }

    /**
     * Performs routine cleanup prior to executing a write. This should be called every time a write
     * thread acquires the segment lock, immediately after acquiring the lock.
     */
    @GuardedBy("this")
    void preWriteCleanup() {
      runLockedCleanup();
    }

    void runCleanup() {
      runLockedCleanup();
    }

    void runLockedCleanup() {
      if (tryLock()) {
        try {
          maybeDrainReferenceQueues();
          readCount.set(0);
        } finally {
          unlock();
        }
      }
    }
  }

  /** Concrete implementation of {@link Segment} for strong keys and strong values. */
  static final class StrongKeyStrongValueSegment<K, V>
      extends Segment<K, V, StrongKeyStrongValueEntry<K, V>, StrongKeyStrongValueSegment<K, V>> {
    StrongKeyStrongValueSegment(
        MapMakerInternalMap<
                K, V, StrongKeyStrongValueEntry<K, V>, StrongKeyStrongValueSegment<K, V>>
            map,
        int initialCapacity,
        int maxSegmentSize) {
      super(map, initialCapacity, maxSegmentSize);
    }

    @Override
    StrongKeyStrongValueSegment<K, V> self() {
      return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public StrongKeyStrongValueEntry<K, V> castForTesting(InternalEntry<K, V, ?> entry) {
      return (StrongKeyStrongValueEntry<K, V>) entry;
    }
  }

  /** Concrete implementation of {@link Segment} for strong keys and weak values. */
  static final class StrongKeyWeakValueSegment<K, V>
      extends Segment<K, V, StrongKeyWeakValueEntry<K, V>, StrongKeyWeakValueSegment<K, V>> {
    private final ReferenceQueue<V> queueForValues = new ReferenceQueue<V>();

    StrongKeyWeakValueSegment(
        MapMakerInternalMap<K, V, StrongKeyWeakValueEntry<K, V>, StrongKeyWeakValueSegment<K, V>>
            map,
        int initialCapacity,
        int maxSegmentSize) {
      super(map, initialCapacity, maxSegmentSize);
    }

    @Override
    StrongKeyWeakValueSegment<K, V> self() {
      return this;
    }

    @Override
    ReferenceQueue<V> getValueReferenceQueueForTesting() {
      return queueForValues;
    }

    @SuppressWarnings("unchecked")
    @Override
    public StrongKeyWeakValueEntry<K, V> castForTesting(InternalEntry<K, V, ?> entry) {
      return (StrongKeyWeakValueEntry<K, V>) entry;
    }

    @Override
    public WeakValueReference<K, V, StrongKeyWeakValueEntry<K, V>> getWeakValueReferenceForTesting(
        InternalEntry<K, V, ?> e) {
      return castForTesting(e).getValueReference();
    }

    @Override
    public WeakValueReference<K, V, StrongKeyWeakValueEntry<K, V>> newWeakValueReferenceForTesting(
        InternalEntry<K, V, ?> e, V value) {
      return new WeakValueReferenceImpl<>(queueForValues, value, castForTesting(e));
    }

    @Override
    public void setWeakValueReferenceForTesting(
        InternalEntry<K, V, ?> e,
        WeakValueReference<K, V, ? extends InternalEntry<K, V, ?>> valueReference) {
      StrongKeyWeakValueEntry<K, V> entry = castForTesting(e);
      @SuppressWarnings("unchecked")
      WeakValueReference<K, V, StrongKeyWeakValueEntry<K, V>> newValueReference =
          (WeakValueReference<K, V, StrongKeyWeakValueEntry<K, V>>) valueReference;
      WeakValueReference<K, V, StrongKeyWeakValueEntry<K, V>> previous = entry.valueReference;
      entry.valueReference = newValueReference;
      previous.clear();
    }

    @Override
    void maybeDrainReferenceQueues() {
      drainValueReferenceQueue(queueForValues);
    }

    @Override
    void maybeClearReferenceQueues() {
      clearReferenceQueue(queueForValues);
    }
  }

  /** Concrete implementation of {@link Segment} for strong keys and {@link Dummy} values. */
  static final class StrongKeyDummyValueSegment<K>
      extends Segment<K, Dummy, StrongKeyDummyValueEntry<K>, StrongKeyDummyValueSegment<K>> {
    StrongKeyDummyValueSegment(
        MapMakerInternalMap<K, Dummy, StrongKeyDummyValueEntry<K>, StrongKeyDummyValueSegment<K>>
            map,
        int initialCapacity,
        int maxSegmentSize) {
      super(map, initialCapacity, maxSegmentSize);
    }

    @Override
    StrongKeyDummyValueSegment<K> self() {
      return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public StrongKeyDummyValueEntry<K> castForTesting(InternalEntry<K, Dummy, ?> entry) {
      return (StrongKeyDummyValueEntry<K>) entry;
    }
  }

  /** Concrete implementation of {@link Segment} for weak keys and strong values. */
  static final class WeakKeyStrongValueSegment<K, V>
      extends Segment<K, V, WeakKeyStrongValueEntry<K, V>, WeakKeyStrongValueSegment<K, V>> {
    private final ReferenceQueue<K> queueForKeys = new ReferenceQueue<K>();

    WeakKeyStrongValueSegment(
        MapMakerInternalMap<K, V, WeakKeyStrongValueEntry<K, V>, WeakKeyStrongValueSegment<K, V>>
            map,
        int initialCapacity,
        int maxSegmentSize) {
      super(map, initialCapacity, maxSegmentSize);
    }

    @Override
    WeakKeyStrongValueSegment<K, V> self() {
      return this;
    }

    @Override
    ReferenceQueue<K> getKeyReferenceQueueForTesting() {
      return queueForKeys;
    }

    @SuppressWarnings("unchecked")
    @Override
    public WeakKeyStrongValueEntry<K, V> castForTesting(InternalEntry<K, V, ?> entry) {
      return (WeakKeyStrongValueEntry<K, V>) entry;
    }

    @Override
    void maybeDrainReferenceQueues() {
      drainKeyReferenceQueue(queueForKeys);
    }

    @Override
    void maybeClearReferenceQueues() {
      clearReferenceQueue(queueForKeys);
    }
  }

  /** Concrete implementation of {@link Segment} for weak keys and weak values. */
  static final class WeakKeyWeakValueSegment<K, V>
      extends Segment<K, V, WeakKeyWeakValueEntry<K, V>, WeakKeyWeakValueSegment<K, V>> {
    private final ReferenceQueue<K> queueForKeys = new ReferenceQueue<K>();
    private final ReferenceQueue<V> queueForValues = new ReferenceQueue<V>();

    WeakKeyWeakValueSegment(
        MapMakerInternalMap<K, V, WeakKeyWeakValueEntry<K, V>, WeakKeyWeakValueSegment<K, V>> map,
        int initialCapacity,
        int maxSegmentSize) {
      super(map, initialCapacity, maxSegmentSize);
    }

    @Override
    WeakKeyWeakValueSegment<K, V> self() {
      return this;
    }

    @Override
    ReferenceQueue<K> getKeyReferenceQueueForTesting() {
      return queueForKeys;
    }

    @Override
    ReferenceQueue<V> getValueReferenceQueueForTesting() {
      return queueForValues;
    }

    @SuppressWarnings("unchecked")
    @Override
    public WeakKeyWeakValueEntry<K, V> castForTesting(InternalEntry<K, V, ?> entry) {
      return (WeakKeyWeakValueEntry<K, V>) entry;
    }

    @Override
    public WeakValueReference<K, V, WeakKeyWeakValueEntry<K, V>> getWeakValueReferenceForTesting(
        InternalEntry<K, V, ?> e) {
      return castForTesting(e).getValueReference();
    }

    @Override
    public WeakValueReference<K, V, WeakKeyWeakValueEntry<K, V>> newWeakValueReferenceForTesting(
        InternalEntry<K, V, ?> e, V value) {
      return new WeakValueReferenceImpl<>(queueForValues, value, castForTesting(e));
    }

    @Override
    public void setWeakValueReferenceForTesting(
        InternalEntry<K, V, ?> e,
        WeakValueReference<K, V, ? extends InternalEntry<K, V, ?>> valueReference) {
      WeakKeyWeakValueEntry<K, V> entry = castForTesting(e);
      @SuppressWarnings("unchecked")
      WeakValueReference<K, V, WeakKeyWeakValueEntry<K, V>> newValueReference =
          (WeakValueReference<K, V, WeakKeyWeakValueEntry<K, V>>) valueReference;
      WeakValueReference<K, V, WeakKeyWeakValueEntry<K, V>> previous = entry.valueReference;
      entry.valueReference = newValueReference;
      previous.clear();
    }

    @Override
    void maybeDrainReferenceQueues() {
      drainKeyReferenceQueue(queueForKeys);
      drainValueReferenceQueue(queueForValues);
    }

    @Override
    void maybeClearReferenceQueues() {
      clearReferenceQueue(queueForKeys);
    }
  }

  /** Concrete implementation of {@link Segment} for weak keys and {@link Dummy} values. */
  static final class WeakKeyDummyValueSegment<K>
      extends Segment<K, Dummy, WeakKeyDummyValueEntry<K>, WeakKeyDummyValueSegment<K>> {
    private final ReferenceQueue<K> queueForKeys = new ReferenceQueue<K>();

    WeakKeyDummyValueSegment(
        MapMakerInternalMap<K, Dummy, WeakKeyDummyValueEntry<K>, WeakKeyDummyValueSegment<K>> map,
        int initialCapacity,
        int maxSegmentSize) {
      super(map, initialCapacity, maxSegmentSize);
    }

    @Override
    WeakKeyDummyValueSegment<K> self() {
      return this;
    }

    @Override
    ReferenceQueue<K> getKeyReferenceQueueForTesting() {
      return queueForKeys;
    }

    @SuppressWarnings("unchecked")
    @Override
    public WeakKeyDummyValueEntry<K> castForTesting(InternalEntry<K, Dummy, ?> entry) {
      return (WeakKeyDummyValueEntry<K>) entry;
    }

    @Override
    void maybeDrainReferenceQueues() {
      drainKeyReferenceQueue(queueForKeys);
    }

    @Override
    void maybeClearReferenceQueues() {
      clearReferenceQueue(queueForKeys);
    }
  }

  static final class CleanupMapTask implements Runnable {
    final WeakReference<MapMakerInternalMap<?, ?, ?, ?>> mapReference;

    public CleanupMapTask(MapMakerInternalMap<?, ?, ?, ?> map) {
      this.mapReference = new WeakReference<MapMakerInternalMap<?, ?, ?, ?>>(map);
    }

    @Override
    public void run() {
      MapMakerInternalMap<?, ?, ?, ?> map = mapReference.get();
      if (map == null) {
        throw new CancellationException();
      }

      for (Segment<?, ?, ?, ?> segment : map.segments) {
        segment.runCleanup();
      }
    }
  }

  @VisibleForTesting
  Strength keyStrength() {
    return entryHelper.keyStrength();
  }

  @VisibleForTesting
  Strength valueStrength() {
    return entryHelper.valueStrength();
  }

  @VisibleForTesting
  Equivalence<Object> valueEquivalence() {
    return entryHelper.valueStrength().defaultEquivalence();
  }

  // ConcurrentMap methods

  @Override
  public boolean isEmpty() {
    /*
     * Sum per-segment modCounts to avoid mis-reporting when elements are concurrently added and
     * removed in one segment while checking another, in which case the table was never actually
     * empty at any point. (The sum ensures accuracy up through at least 1<<31 per-segment
     * modifications before recheck.)  Method containsValue() uses similar constructions for
     * stability checks.
     */
    long sum = 0L;
    Segment<K, V, E, S>[] segments = this.segments;
    for (int i = 0; i < segments.length; ++i) {
      if (segments[i].count != 0) {
        return false;
      }
      sum += segments[i].modCount;
    }

    if (sum != 0L) { // recheck unless no modifications
      for (int i = 0; i < segments.length; ++i) {
        if (segments[i].count != 0) {
          return false;
        }
        sum -= segments[i].modCount;
      }
      return sum == 0L;
    }
    return true;
  }

  @Override
  public int size() {
    Segment<K, V, E, S>[] segments = this.segments;
    long sum = 0;
    for (int i = 0; i < segments.length; ++i) {
      sum += segments[i].count;
    }
    return Ints.saturatedCast(sum);
  }

  @Override
  public V get(@Nullable Object key) {
    if (key == null) {
      return null;
    }
    int hash = hash(key);
    return segmentFor(hash).get(key, hash);
  }

  /**
   * Returns the internal entry for the specified key. The entry may be computing or partially
   * collected. Does not impact recency ordering.
   */
  E getEntry(@Nullable Object key) {
    if (key == null) {
      return null;
    }
    int hash = hash(key);
    return segmentFor(hash).getEntry(key, hash);
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    if (key == null) {
      return false;
    }
    int hash = hash(key);
    return segmentFor(hash).containsKey(key, hash);
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    if (value == null) {
      return false;
    }

    // This implementation is patterned after ConcurrentHashMap, but without the locking. The only
    // way for it to return a false negative would be for the target value to jump around in the map
    // such that none of the subsequent iterations observed it, despite the fact that at every point
    // in time it was present somewhere int the map. This becomes increasingly unlikely as
    // CONTAINS_VALUE_RETRIES increases, though without locking it is theoretically possible.
    final Segment<K, V, E, S>[] segments = this.segments;
    long last = -1L;
    for (int i = 0; i < CONTAINS_VALUE_RETRIES; i++) {
      long sum = 0L;
      for (Segment<K, V, E, S> segment : segments) {
        // ensure visibility of most recent completed write
        int unused = segment.count; // read-volatile

        AtomicReferenceArray<E> table = segment.table;
        for (int j = 0; j < table.length(); j++) {
          for (E e = table.get(j); e != null; e = e.getNext()) {
            V v = segment.getLiveValue(e);
            if (v != null && valueEquivalence().equivalent(value, v)) {
              return true;
            }
          }
        }
        sum += segment.modCount;
      }
      if (sum == last) {
        break;
      }
      last = sum;
    }
    return false;
  }

  @CanIgnoreReturnValue
  @Override
  public V put(K key, V value) {
    checkNotNull(key);
    checkNotNull(value);
    int hash = hash(key);
    return segmentFor(hash).put(key, hash, value, false);
  }

  @CanIgnoreReturnValue
  @Override
  public V putIfAbsent(K key, V value) {
    checkNotNull(key);
    checkNotNull(value);
    int hash = hash(key);
    return segmentFor(hash).put(key, hash, value, true);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  @CanIgnoreReturnValue
  @Override
  public V remove(@Nullable Object key) {
    if (key == null) {
      return null;
    }
    int hash = hash(key);
    return segmentFor(hash).remove(key, hash);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean remove(@Nullable Object key, @Nullable Object value) {
    if (key == null || value == null) {
      return false;
    }
    int hash = hash(key);
    return segmentFor(hash).remove(key, hash, value);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean replace(K key, @Nullable V oldValue, V newValue) {
    checkNotNull(key);
    checkNotNull(newValue);
    if (oldValue == null) {
      return false;
    }
    int hash = hash(key);
    return segmentFor(hash).replace(key, hash, oldValue, newValue);
  }

  @CanIgnoreReturnValue
  @Override
  public V replace(K key, V value) {
    checkNotNull(key);
    checkNotNull(value);
    int hash = hash(key);
    return segmentFor(hash).replace(key, hash, value);
  }

  @Override
  public void clear() {
    for (Segment<K, V, E, S> segment : segments) {
      segment.clear();
    }
  }

  transient @Nullable Set<K> keySet;

  @Override
  public Set<K> keySet() {
    Set<K> ks = keySet;
    return (ks != null) ? ks : (keySet = new KeySet());
  }

  transient @Nullable Collection<V> values;

  @Override
  public Collection<V> values() {
    Collection<V> vs = values;
    return (vs != null) ? vs : (values = new Values());
  }

  transient @Nullable Set<Entry<K, V>> entrySet;

  @Override
  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> es = entrySet;
    return (es != null) ? es : (entrySet = new EntrySet());
  }

  // Iterator Support

  abstract class HashIterator<T> implements Iterator<T> {

    int nextSegmentIndex;
    int nextTableIndex;
    @Nullable Segment<K, V, E, S> currentSegment;
    @Nullable AtomicReferenceArray<E> currentTable;
    @Nullable E nextEntry;
    @Nullable WriteThroughEntry nextExternal;
    @Nullable WriteThroughEntry lastReturned;

    HashIterator() {
      nextSegmentIndex = segments.length - 1;
      nextTableIndex = -1;
      advance();
    }

    @Override
    public abstract T next();

    final void advance() {
      nextExternal = null;

      if (nextInChain()) {
        return;
      }

      if (nextInTable()) {
        return;
      }

      while (nextSegmentIndex >= 0) {
        currentSegment = segments[nextSegmentIndex--];
        if (currentSegment.count != 0) {
          currentTable = currentSegment.table;
          nextTableIndex = currentTable.length() - 1;
          if (nextInTable()) {
            return;
          }
        }
      }
    }

    /** Finds the next entry in the current chain. Returns {@code true} if an entry was found. */
    boolean nextInChain() {
      if (nextEntry != null) {
        for (nextEntry = nextEntry.getNext(); nextEntry != null; nextEntry = nextEntry.getNext()) {
          if (advanceTo(nextEntry)) {
            return true;
          }
        }
      }
      return false;
    }

    /** Finds the next entry in the current table. Returns {@code true} if an entry was found. */
    boolean nextInTable() {
      while (nextTableIndex >= 0) {
        if ((nextEntry = currentTable.get(nextTableIndex--)) != null) {
          if (advanceTo(nextEntry) || nextInChain()) {
            return true;
          }
        }
      }
      return false;
    }

    /**
     * Advances to the given entry. Returns {@code true} if the entry was valid, {@code false} if it
     * should be skipped.
     */
    boolean advanceTo(E entry) {
      try {
        K key = entry.getKey();
        V value = getLiveValue(entry);
        if (value != null) {
          nextExternal = new WriteThroughEntry(key, value);
          return true;
        } else {
          // Skip stale entry.
          return false;
        }
      } finally {
        currentSegment.postReadCleanup();
      }
    }

    @Override
    public boolean hasNext() {
      return nextExternal != null;
    }

    WriteThroughEntry nextEntry() {
      if (nextExternal == null) {
        throw new NoSuchElementException();
      }
      lastReturned = nextExternal;
      advance();
      return lastReturned;
    }

    @Override
    public void remove() {
      checkRemove(lastReturned != null);
      MapMakerInternalMap.this.remove(lastReturned.getKey());
      lastReturned = null;
    }
  }

  final class KeyIterator extends HashIterator<K> {

    @Override
    public K next() {
      return nextEntry().getKey();
    }
  }

  final class ValueIterator extends HashIterator<V> {

    @Override
    public V next() {
      return nextEntry().getValue();
    }
  }

  /**
   * Custom Entry class used by EntryIterator.next(), that relays setValue changes to the underlying
   * map.
   */
  final class WriteThroughEntry extends AbstractMapEntry<K, V> {
    final K key; // non-null
    V value; // non-null

    WriteThroughEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public boolean equals(@Nullable Object object) {
      // Cannot use key and value equivalence
      if (object instanceof Entry) {
        Entry<?, ?> that = (Entry<?, ?>) object;
        return key.equals(that.getKey()) && value.equals(that.getValue());
      }
      return false;
    }

    @Override
    public int hashCode() {
      // Cannot use key and value equivalence
      return key.hashCode() ^ value.hashCode();
    }

    @Override
    public V setValue(V newValue) {
      V oldValue = put(key, newValue);
      value = newValue; // only if put succeeds
      return oldValue;
    }
  }

  final class EntryIterator extends HashIterator<Entry<K, V>> {

    @Override
    public Entry<K, V> next() {
      return nextEntry();
    }
  }

  @WeakOuter
  final class KeySet extends SafeToArraySet<K> {

    @Override
    public Iterator<K> iterator() {
      return new KeyIterator();
    }

    @Override
    public int size() {
      return MapMakerInternalMap.this.size();
    }

    @Override
    public boolean isEmpty() {
      return MapMakerInternalMap.this.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return MapMakerInternalMap.this.containsKey(o);
    }

    @Override
    public boolean remove(Object o) {
      return MapMakerInternalMap.this.remove(o) != null;
    }

    @Override
    public void clear() {
      MapMakerInternalMap.this.clear();
    }
  }

  @WeakOuter
  final class Values extends AbstractCollection<V> {

    @Override
    public Iterator<V> iterator() {
      return new ValueIterator();
    }

    @Override
    public int size() {
      return MapMakerInternalMap.this.size();
    }

    @Override
    public boolean isEmpty() {
      return MapMakerInternalMap.this.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return MapMakerInternalMap.this.containsValue(o);
    }

    @Override
    public void clear() {
      MapMakerInternalMap.this.clear();
    }

    // super.toArray() may misbehave if size() is inaccurate, at least on old versions of Android.
    // https://code.google.com/p/android/issues/detail?id=36519 / http://r.android.com/47508

    @Override
    public Object[] toArray() {
      return toArrayList(this).toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return toArrayList(this).toArray(a);
    }
  }

  @WeakOuter
  final class EntrySet extends SafeToArraySet<Entry<K, V>> {

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new EntryIterator();
    }

    @Override
    public boolean contains(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;
      Object key = e.getKey();
      if (key == null) {
        return false;
      }
      V v = MapMakerInternalMap.this.get(key);

      return v != null && valueEquivalence().equivalent(e.getValue(), v);
    }

    @Override
    public boolean remove(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;
      Object key = e.getKey();
      return key != null && MapMakerInternalMap.this.remove(key, e.getValue());
    }

    @Override
    public int size() {
      return MapMakerInternalMap.this.size();
    }

    @Override
    public boolean isEmpty() {
      return MapMakerInternalMap.this.isEmpty();
    }

    @Override
    public void clear() {
      MapMakerInternalMap.this.clear();
    }
  }

  private abstract static class SafeToArraySet<E> extends AbstractSet<E> {
    // super.toArray() may misbehave if size() is inaccurate, at least on old versions of Android.
    // https://code.google.com/p/android/issues/detail?id=36519 / http://r.android.com/47508

    @Override
    public Object[] toArray() {
      return toArrayList(this).toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return toArrayList(this).toArray(a);
    }
  }

  private static <E> ArrayList<E> toArrayList(Collection<E> c) {
    // Avoid calling ArrayList(Collection), which may call back into toArray.
    ArrayList<E> result = new ArrayList<>(c.size());
    Iterators.addAll(result, c.iterator());
    return result;
  }

  // Serialization Support

  private static final long serialVersionUID = 5;

  Object writeReplace() {
    return new SerializationProxy<>(
        entryHelper.keyStrength(),
        entryHelper.valueStrength(),
        keyEquivalence,
        entryHelper.valueStrength().defaultEquivalence(),
        concurrencyLevel,
        this);
  }

  /**
   * The actual object that gets serialized. Unfortunately, readResolve() doesn't get called when a
   * circular dependency is present, so the proxy must be able to behave as the map itself.
   */
  abstract static class AbstractSerializationProxy<K, V> extends ForwardingConcurrentMap<K, V>
      implements Serializable {
    private static final long serialVersionUID = 3;

    final Strength keyStrength;
    final Strength valueStrength;
    final Equivalence<Object> keyEquivalence;
    final Equivalence<Object> valueEquivalence;
    final int concurrencyLevel;

    transient ConcurrentMap<K, V> delegate;

    AbstractSerializationProxy(
        Strength keyStrength,
        Strength valueStrength,
        Equivalence<Object> keyEquivalence,
        Equivalence<Object> valueEquivalence,
        int concurrencyLevel,
        ConcurrentMap<K, V> delegate) {
      this.keyStrength = keyStrength;
      this.valueStrength = valueStrength;
      this.keyEquivalence = keyEquivalence;
      this.valueEquivalence = valueEquivalence;
      this.concurrencyLevel = concurrencyLevel;
      this.delegate = delegate;
    }

    @Override
    protected ConcurrentMap<K, V> delegate() {
      return delegate;
    }

    void writeMapTo(ObjectOutputStream out) throws IOException {
      out.writeInt(delegate.size());
      for (Entry<K, V> entry : delegate.entrySet()) {
        out.writeObject(entry.getKey());
        out.writeObject(entry.getValue());
      }
      out.writeObject(null); // terminate entries
    }

    @SuppressWarnings("deprecation") // serialization of deprecated feature
    MapMaker readMapMaker(ObjectInputStream in) throws IOException {
      int size = in.readInt();
      return new MapMaker()
          .initialCapacity(size)
          .setKeyStrength(keyStrength)
          .setValueStrength(valueStrength)
          .keyEquivalence(keyEquivalence)
          .concurrencyLevel(concurrencyLevel);
    }

    @SuppressWarnings("unchecked")
    void readEntries(ObjectInputStream in) throws IOException, ClassNotFoundException {
      while (true) {
        K key = (K) in.readObject();
        if (key == null) {
          break; // terminator
        }
        V value = (V) in.readObject();
        delegate.put(key, value);
      }
    }
  }

  /**
   * The actual object that gets serialized. Unfortunately, readResolve() doesn't get called when a
   * circular dependency is present, so the proxy must be able to behave as the map itself.
   */
  private static final class SerializationProxy<K, V> extends AbstractSerializationProxy<K, V> {
    private static final long serialVersionUID = 3;

    SerializationProxy(
        Strength keyStrength,
        Strength valueStrength,
        Equivalence<Object> keyEquivalence,
        Equivalence<Object> valueEquivalence,
        int concurrencyLevel,
        ConcurrentMap<K, V> delegate) {
      super(
          keyStrength, valueStrength, keyEquivalence, valueEquivalence, concurrencyLevel, delegate);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      writeMapTo(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      MapMaker mapMaker = readMapMaker(in);
      delegate = mapMaker.makeMap();
      readEntries(in);
    }

    private Object readResolve() {
      return delegate;
    }
  }
}
