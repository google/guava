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
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * The concurrent hash map implementation built by {@link MapMaker}.
 *
 * <p>This implementation is heavily derived from revision 1.96 of <a
 * href="http://tinyurl.com/ConcurrentHashMap">ConcurrentHashMap.java</a>.
 *
 * @author Bob Lee
 * @author Charles Fry
 * @author Doug Lea ({@code ConcurrentHashMap})
 */
// TODO(kak/cpovirk): Consider removing @CanIgnoreReturnValue from this class.
@GwtIncompatible
class MapMakerInternalMap<K, V> extends AbstractMap<K, V>
    implements ConcurrentMap<K, V>, Serializable {

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
   * constructors with arguments. MUST be a power of two <= 1<<30 to ensure that entries are
   * indexable using ints.
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
  final transient Segment<K, V>[] segments;

  /** The concurrency level. */
  final int concurrencyLevel;

  /** Strategy for comparing keys. */
  final Equivalence<Object> keyEquivalence;

  /** Strategy for comparing values. */
  final Equivalence<Object> valueEquivalence;

  /** Strategy for referencing keys. */
  final Strength keyStrength;

  /** Strategy for referencing values. */
  final Strength valueStrength;

  /** Factory used to create new entries. */
  final transient EntryFactory entryFactory;

  /**
   * Creates a new, empty map with the specified strategy, initial capacity and concurrency level.
   */
  MapMakerInternalMap(MapMaker builder) {
    concurrencyLevel = Math.min(builder.getConcurrencyLevel(), MAX_SEGMENTS);

    keyStrength = builder.getKeyStrength();
    valueStrength = builder.getValueStrength();

    keyEquivalence = builder.getKeyEquivalence();
    valueEquivalence = valueStrength.defaultEquivalence();

    entryFactory = EntryFactory.getFactory(keyStrength);

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

  boolean usesKeyReferences() {
    return keyStrength != Strength.STRONG;
  }

  boolean usesValueReferences() {
    return valueStrength != Strength.STRONG;
  }

  enum Strength {
    /*
     * TODO(kevinb): If we strongly reference the value and aren't computing, we needn't wrap the
     * value. This could save ~8 bytes per entry.
     */

    STRONG {
      @Override
      <K, V> ValueReference<K, V> referenceValue(
          Segment<K, V> segment, ReferenceEntry<K, V> entry, V value) {
        return new StrongValueReference<K, V>(value);
      }

      @Override
      Equivalence<Object> defaultEquivalence() {
        return Equivalence.equals();
      }
    },

    WEAK {
      @Override
      <K, V> ValueReference<K, V> referenceValue(
          Segment<K, V> segment, ReferenceEntry<K, V> entry, V value) {
        return new WeakValueReference<K, V>(segment.valueReferenceQueue, value, entry);
      }

      @Override
      Equivalence<Object> defaultEquivalence() {
        return Equivalence.identity();
      }
    };

    /**
     * Creates a reference for the given value according to this value strength.
     */
    abstract <K, V> ValueReference<K, V> referenceValue(
        Segment<K, V> segment, ReferenceEntry<K, V> entry, V value);

    /**
     * Returns the default equivalence strategy used to compare and hash keys or values referenced
     * at this strength. This strategy will be used unless the user explicitly specifies an
     * alternate strategy.
     */
    abstract Equivalence<Object> defaultEquivalence();
  }

  /**
   * Creates new entries.
   */
  enum EntryFactory {
    STRONG {
      @Override
      <K, V> ReferenceEntry<K, V> newEntry(
          Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
        return new StrongEntry<K, V>(key, hash, next);
      }
    },

    WEAK {
      @Override
      <K, V> ReferenceEntry<K, V> newEntry(
          Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
        return new WeakEntry<K, V>(segment.keyReferenceQueue, key, hash, next);
      }
    };

    static EntryFactory getFactory(Strength keyStrength) {
      switch (keyStrength) {
        case STRONG:
          return EntryFactory.STRONG;
        case WEAK:
          return EntryFactory.WEAK;
      }
      throw new AssertionError();
    }

    /**
     * Creates a new entry.
     *
     * @param segment to create the entry for
     * @param key of the entry
     * @param hash of the key
     * @param next entry in the same bucket
     */
    abstract <K, V> ReferenceEntry<K, V> newEntry(
        Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next);

    /**
     * Copies an entry, assigning it a new {@code next} entry.
     *
     * @param original the entry to copy
     * @param newNext entry in the same bucket
     */
    // Guarded By Segment.this
    <K, V> ReferenceEntry<K, V> copyEntry(
        Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
      return newEntry(segment, original.getKey(), original.getHash(), newNext);
    }
  }

  /**
   * A reference to a value.
   */
  interface ValueReference<K, V> {
    /**
     * Gets the value. Does not block or throw exceptions.
     */
    V get();

    /**
     * Waits for a value that may still be computing. Unlike get(), this method can block (in the
     * case of FutureValueReference).
     *
     * @throws ExecutionException if the computing thread throws an exception
     */
    V waitForValue() throws ExecutionException;

    /**
     * Returns the entry associated with this value reference, or {@code null} if this value
     * reference is independent of any entry.
     */
    ReferenceEntry<K, V> getEntry();

    /**
     * Creates a copy of this reference for the given entry.
     *
     * <p>{@code value} may be null only for a loading reference.
     */
    ValueReference<K, V> copyFor(
        ReferenceQueue<V> queue, @Nullable V value, ReferenceEntry<K, V> entry);

    /**
     * Clears this reference object.
     *
     * @param newValue the new value reference which will replace this one; this is only used during
     *     computation to immediately notify blocked threads of the new value
     */
    void clear(@Nullable ValueReference<K, V> newValue);

    /**
     * Returns {@code true} if the value type is a computing reference (regardless of whether or not
     * computation has completed). This is necessary to distiguish between partially-collected
     * entries and computing entries, which need to be cleaned up differently.
     */
    boolean isComputingReference();
  }

  /**
   * Placeholder. Indicates that the value hasn't been set yet.
   */
  static final ValueReference<Object, Object> UNSET =
      new ValueReference<Object, Object>() {
        @Override
        public Object get() {
          return null;
        }

        @Override
        public ReferenceEntry<Object, Object> getEntry() {
          return null;
        }

        @Override
        public ValueReference<Object, Object> copyFor(
            ReferenceQueue<Object> queue,
            @Nullable Object value,
            ReferenceEntry<Object, Object> entry) {
          return this;
        }

        @Override
        public boolean isComputingReference() {
          return false;
        }

        @Override
        public Object waitForValue() {
          return null;
        }

        @Override
        public void clear(ValueReference<Object, Object> newValue) {}
      };

  /**
   * Singleton placeholder that indicates a value is being computed.
   */
  @SuppressWarnings("unchecked") // impl never uses a parameter or returns any non-null value
  static <K, V> ValueReference<K, V> unset() {
    return (ValueReference<K, V>) UNSET;
  }

  /**
   * An entry in a reference map.
   *
   * Entries in the map can be in the following states:
   *
   * Valid:
   * - Live: valid key/value are set
   * - Computing: computation is pending
   *
   * Invalid:
   * - Collected: key/value was partially collected, but not yet cleaned up
   */
  interface ReferenceEntry<K, V> {
    /**
     * Gets the value reference from this entry.
     */
    ValueReference<K, V> getValueReference();

    /**
     * Sets the value reference for this entry.
     */
    void setValueReference(ValueReference<K, V> valueReference);

    /**
     * Gets the next entry in the chain.
     */
    ReferenceEntry<K, V> getNext();

    /**
     * Gets the entry's hash.
     */
    int getHash();

    /**
     * Gets the key for this entry.
     */
    K getKey();
  }

  /*
   * Note: All of this duplicate code sucks, but it saves a lot of memory. If only Java had mixins!
   */

  /**
   * Used for strongly-referenced keys.
   */
  static class StrongEntry<K, V> implements ReferenceEntry<K, V> {
    final K key;

    StrongEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
      this.key = key;
      this.hash = hash;
      this.next = next;
    }

    @Override
    public K getKey() {
      return this.key;
    }

    // The code below is exactly the same for each entry type.

    final int hash;
    final ReferenceEntry<K, V> next;
    volatile ValueReference<K, V> valueReference = unset();

    @Override
    public ValueReference<K, V> getValueReference() {
      return valueReference;
    }

    @Override
    public void setValueReference(ValueReference<K, V> valueReference) {
      ValueReference<K, V> previous = this.valueReference;
      this.valueReference = valueReference;
      previous.clear(valueReference);
    }

    @Override
    public int getHash() {
      return hash;
    }

    @Override
    public ReferenceEntry<K, V> getNext() {
      return next;
    }
  }

  /**
   * Used for weakly-referenced keys.
   */
  static class WeakEntry<K, V> extends WeakReference<K> implements ReferenceEntry<K, V> {
    WeakEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
      super(key, queue);
      this.hash = hash;
      this.next = next;
    }

    @Override
    public K getKey() {
      return get();
    }

    // The code below is exactly the same for each entry type.

    final int hash;
    final ReferenceEntry<K, V> next;
    volatile ValueReference<K, V> valueReference = unset();

    @Override
    public ValueReference<K, V> getValueReference() {
      return valueReference;
    }

    @Override
    public void setValueReference(ValueReference<K, V> valueReference) {
      ValueReference<K, V> previous = this.valueReference;
      this.valueReference = valueReference;
      previous.clear(valueReference);
    }

    @Override
    public int getHash() {
      return hash;
    }

    @Override
    public ReferenceEntry<K, V> getNext() {
      return next;
    }
  }

  /**
   * References a weak value.
   */
  static final class WeakValueReference<K, V> extends WeakReference<V>
      implements ValueReference<K, V> {
    final ReferenceEntry<K, V> entry;

    WeakValueReference(ReferenceQueue<V> queue, V referent, ReferenceEntry<K, V> entry) {
      super(referent, queue);
      this.entry = entry;
    }

    @Override
    public ReferenceEntry<K, V> getEntry() {
      return entry;
    }

    @Override
    public void clear(ValueReference<K, V> newValue) {
      clear();
    }

    @Override
    public ValueReference<K, V> copyFor(
        ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
      return new WeakValueReference<K, V>(queue, value, entry);
    }

    @Override
    public boolean isComputingReference() {
      return false;
    }

    @Override
    public V waitForValue() {
      return get();
    }
  }

  /**
   * References a strong value.
   */
  static final class StrongValueReference<K, V> implements ValueReference<K, V> {
    final V referent;

    StrongValueReference(V referent) {
      this.referent = referent;
    }

    @Override
    public V get() {
      return referent;
    }

    @Override
    public ReferenceEntry<K, V> getEntry() {
      return null;
    }

    @Override
    public ValueReference<K, V> copyFor(
        ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
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

  /**
   * Applies a supplemental hash function to a given hash code, which defends against poor quality
   * hash functions. This is critical when the concurrent hash map uses power-of-two length hash
   * tables, that otherwise encounter collisions for hash codes that do not differ in lower or
   * upper bits.
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
   * This method is a convenience for testing. Code should call {@link Segment#newEntry} directly.
   */
  // Guarded By Segment.this
  @VisibleForTesting
  ReferenceEntry<K, V> newEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
    return segmentFor(hash).newEntry(key, hash, next);
  }

  /**
   * This method is a convenience for testing. Code should call {@link Segment#copyEntry} directly.
   */
  // Guarded By Segment.this
  @VisibleForTesting
  ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
    int hash = original.getHash();
    return segmentFor(hash).copyEntry(original, newNext);
  }

  /**
   * This method is a convenience for testing. Code should call {@link Segment#setValue} instead.
   */
  // Guarded By Segment.this
  @VisibleForTesting
  ValueReference<K, V> newValueReference(ReferenceEntry<K, V> entry, V value) {
    int hash = entry.getHash();
    return valueStrength.referenceValue(segmentFor(hash), entry, value);
  }

  int hash(Object key) {
    int h = keyEquivalence.hash(key);
    return rehash(h);
  }

  void reclaimValue(ValueReference<K, V> valueReference) {
    ReferenceEntry<K, V> entry = valueReference.getEntry();
    int hash = entry.getHash();
    segmentFor(hash).reclaimValue(entry.getKey(), hash, valueReference);
  }

  void reclaimKey(ReferenceEntry<K, V> entry) {
    int hash = entry.getHash();
    segmentFor(hash).reclaimKey(entry, hash);
  }

  /**
   * This method is a convenience for testing. Code should call {@link Segment#getLiveValue}
   * instead.
   */
  @VisibleForTesting
  boolean isLive(ReferenceEntry<K, V> entry) {
    return segmentFor(entry.getHash()).getLiveValue(entry) != null;
  }

  /**
   * Returns the segment that should be used for a key with the given hash.
   *
   * @param hash the hash code for the key
   * @return the segment
   */
  Segment<K, V> segmentFor(int hash) {
    // TODO(fry): Lazily create segments?
    return segments[(hash >>> segmentShift) & segmentMask];
  }

  Segment<K, V> createSegment(int initialCapacity, int maxSegmentSize) {
    return new Segment<K, V>(this, initialCapacity, maxSegmentSize);
  }

  /**
   * Gets the value from an entry. Returns {@code null} if the entry is invalid,
   * partially-collected or computing. Unlike {@link Segment#getLiveValue} this method
   * does not attempt to clean up stale entries.
   */
  V getLiveValue(ReferenceEntry<K, V> entry) {
    if (entry.getKey() == null) {
      return null;
    }
    V value = entry.getValueReference().get();
    if (value == null) {
      return null;
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  final Segment<K, V>[] newSegmentArray(int ssize) {
    return new Segment[ssize];
  }

  // Inner Classes

  /**
   * Segments are specialized versions of hash tables. This subclass inherits from ReentrantLock
   * opportunistically, just to simplify some locking and avoid separate construction.
   */
  @SuppressWarnings("serial") // This class is never serialized.
  static class Segment<K, V> extends ReentrantLock {

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

    @Weak final MapMakerInternalMap<K, V> map;

    /**
     * The number of live elements in this segment's region. This does not include unset elements
     * which are awaiting cleanup.
     */
    volatile int count;

    /**
     * Number of updates that alter the size of the table. This is used during bulk-read methods to
     * make sure they see a consistent snapshot: If modCounts change during a traversal of segments
     * computing size or checking containsValue, then we might have an inconsistent view of state
     * so (usually) must retry.
     */
    int modCount;

    /**
     * The table is expanded when its size exceeds this threshold. (The value of this field is
     * always {@code (int) (capacity * 0.75)}.)
     */
    int threshold;

    /**
     * The per-segment table.
     */
    volatile AtomicReferenceArray<ReferenceEntry<K, V>> table;

    /**
     * The maximum size of this map. MapMaker.UNSET_INT if there is no maximum.
     */
    final int maxSegmentSize;

    /**
     * The key reference queue contains entries whose keys have been garbage collected, and which
     * need to be cleaned up internally.
     */
    final ReferenceQueue<K> keyReferenceQueue;

    /**
     * The value reference queue contains value references whose values have been garbage collected,
     * and which need to be cleaned up internally.
     */
    final ReferenceQueue<V> valueReferenceQueue;

    /**
     * A counter of the number of reads since the last write, used to drain queues on a small
     * fraction of read operations.
     */
    final AtomicInteger readCount = new AtomicInteger();

    Segment(MapMakerInternalMap<K, V> map, int initialCapacity, int maxSegmentSize) {
      this.map = map;
      this.maxSegmentSize = maxSegmentSize;
      initTable(newEntryArray(initialCapacity));

      keyReferenceQueue = map.usesKeyReferences() ? new ReferenceQueue<K>() : null;

      valueReferenceQueue = map.usesValueReferences() ? new ReferenceQueue<V>() : null;
    }

    AtomicReferenceArray<ReferenceEntry<K, V>> newEntryArray(int size) {
      return new AtomicReferenceArray<ReferenceEntry<K, V>>(size);
    }

    void initTable(AtomicReferenceArray<ReferenceEntry<K, V>> newTable) {
      this.threshold = newTable.length() * 3 / 4; // 0.75
      if (this.threshold == maxSegmentSize) {
        // prevent spurious expansion before eviction
        this.threshold++;
      }
      this.table = newTable;
    }

    @GuardedBy("this")
    ReferenceEntry<K, V> newEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
      return map.entryFactory.newEntry(this, key, hash, next);
    }

    /**
     * Copies {@code original} into a new entry chained to {@code newNext}. Returns the new entry,
     * or {@code null} if {@code original} was already garbage collected.
     */
    @GuardedBy("this")
    ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
      if (original.getKey() == null) {
        // key collected
        return null;
      }

      ValueReference<K, V> valueReference = original.getValueReference();
      V value = valueReference.get();
      if ((value == null) && !valueReference.isComputingReference()) {
        // value collected
        return null;
      }

      ReferenceEntry<K, V> newEntry = map.entryFactory.copyEntry(this, original, newNext);
      newEntry.setValueReference(valueReference.copyFor(this.valueReferenceQueue, value, newEntry));
      return newEntry;
    }

    /**
     * Sets a new value of an entry.
     */
    @GuardedBy("this")
    void setValue(ReferenceEntry<K, V> entry, V value) {
      ValueReference<K, V> valueReference = map.valueStrength.referenceValue(this, entry, value);
      entry.setValueReference(valueReference);
    }

    // reference queues, for garbage collection cleanup

    /**
     * Cleanup collected entries when the lock is available.
     */
    void tryDrainReferenceQueues() {
      if (tryLock()) {
        try {
          drainReferenceQueues();
        } finally {
          unlock();
        }
      }
    }

    /**
     * Drain the key and value reference queues, cleaning up internal entries containing garbage
     * collected keys or values.
     */
    @GuardedBy("this")
    void drainReferenceQueues() {
      if (map.usesKeyReferences()) {
        drainKeyReferenceQueue();
      }
      if (map.usesValueReferences()) {
        drainValueReferenceQueue();
      }
    }

    @GuardedBy("this")
    void drainKeyReferenceQueue() {
      Reference<? extends K> ref;
      int i = 0;
      while ((ref = keyReferenceQueue.poll()) != null) {
        @SuppressWarnings("unchecked")
        ReferenceEntry<K, V> entry = (ReferenceEntry<K, V>) ref;
        map.reclaimKey(entry);
        if (++i == DRAIN_MAX) {
          break;
        }
      }
    }

    @GuardedBy("this")
    void drainValueReferenceQueue() {
      Reference<? extends V> ref;
      int i = 0;
      while ((ref = valueReferenceQueue.poll()) != null) {
        @SuppressWarnings("unchecked")
        ValueReference<K, V> valueReference = (ValueReference<K, V>) ref;
        map.reclaimValue(valueReference);
        if (++i == DRAIN_MAX) {
          break;
        }
      }
    }

    /**
     * Clears all entries from the key and value reference queues.
     */
    void clearReferenceQueues() {
      if (map.usesKeyReferences()) {
        clearKeyReferenceQueue();
      }
      if (map.usesValueReferences()) {
        clearValueReferenceQueue();
      }
    }

    void clearKeyReferenceQueue() {
      while (keyReferenceQueue.poll() != null) {}
    }

    void clearValueReferenceQueue() {
      while (valueReferenceQueue.poll() != null) {}
    }

    /**
     * Returns first entry of bin for given hash.
     */
    ReferenceEntry<K, V> getFirst(int hash) {
      // read this volatile field only once
      AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
      return table.get(hash & (table.length() - 1));
    }

    // Specialized implementations of map methods

    ReferenceEntry<K, V> getEntry(Object key, int hash) {
      if (count != 0) { // read-volatile
        for (ReferenceEntry<K, V> e = getFirst(hash); e != null; e = e.getNext()) {
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

    ReferenceEntry<K, V> getLiveEntry(Object key, int hash) {
      ReferenceEntry<K, V> e = getEntry(key, hash);
      if (e == null) {
        return null;
      }
      return e;
    }

    V get(Object key, int hash) {
      try {
        ReferenceEntry<K, V> e = getLiveEntry(key, hash);
        if (e == null) {
          return null;
        }

        V value = e.getValueReference().get();
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
          ReferenceEntry<K, V> e = getLiveEntry(key, hash);
          if (e == null) {
            return false;
          }
          return e.getValueReference().get() != null;
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
          AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
          int length = table.length();
          for (int i = 0; i < length; ++i) {
            for (ReferenceEntry<K, V> e = table.get(i); e != null; e = e.getNext()) {
              V entryValue = getLiveValue(e);
              if (entryValue == null) {
                continue;
              }
              if (map.valueEquivalence.equivalent(value, entryValue)) {
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

        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        // Look for an existing entry.
        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            // We found an existing entry.

            ValueReference<K, V> valueReference = e.getValueReference();
            V entryValue = valueReference.get();

            if (entryValue == null) {
              ++modCount;
              setValue(e, value);
              if (!valueReference.isComputingReference()) {
                newCount = this.count; // count remains unchanged
              }
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
        ReferenceEntry<K, V> newEntry = newEntry(key, hash, first);
        setValue(newEntry, value);
        table.set(index, newEntry);
        this.count = newCount; // write-volatile
        return null;
      } finally {
        unlock();
      }
    }

    /**
     * Expands the table if possible.
     */
    @GuardedBy("this")
    void expand() {
      AtomicReferenceArray<ReferenceEntry<K, V>> oldTable = table;
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
      AtomicReferenceArray<ReferenceEntry<K, V>> newTable = newEntryArray(oldCapacity << 1);
      threshold = newTable.length() * 3 / 4;
      int newMask = newTable.length() - 1;
      for (int oldIndex = 0; oldIndex < oldCapacity; ++oldIndex) {
        // We need to guarantee that any existing reads of old Map can
        // proceed. So we cannot yet null out each bin.
        ReferenceEntry<K, V> head = oldTable.get(oldIndex);

        if (head != null) {
          ReferenceEntry<K, V> next = head.getNext();
          int headIndex = head.getHash() & newMask;

          // Single node on list
          if (next == null) {
            newTable.set(headIndex, head);
          } else {
            // Reuse the consecutive sequence of nodes with the same target
            // index from the end of the list. tail points to the first
            // entry in the reusable list.
            ReferenceEntry<K, V> tail = head;
            int tailIndex = headIndex;
            for (ReferenceEntry<K, V> e = next; e != null; e = e.getNext()) {
              int newIndex = e.getHash() & newMask;
              if (newIndex != tailIndex) {
                // The index changed. We'll need to copy the previous entry.
                tailIndex = newIndex;
                tail = e;
              }
            }
            newTable.set(tailIndex, tail);

            // Clone nodes leading up to the tail.
            for (ReferenceEntry<K, V> e = head; e != tail; e = e.getNext()) {
              int newIndex = e.getHash() & newMask;
              ReferenceEntry<K, V> newNext = newTable.get(newIndex);
              ReferenceEntry<K, V> newFirst = copyEntry(e, newNext);
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

        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            // If the value disappeared, this entry is partially collected,
            // and we should pretend like it doesn't exist.
            ValueReference<K, V> valueReference = e.getValueReference();
            V entryValue = valueReference.get();
            if (entryValue == null) {
              if (isCollected(valueReference)) {
                int newCount = this.count - 1;
                ++modCount;
                ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
                newCount = this.count - 1;
                table.set(index, newFirst);
                this.count = newCount; // write-volatile
              }
              return false;
            }

            if (map.valueEquivalence.equivalent(oldValue, entryValue)) {
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

        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            // If the value disappeared, this entry is partially collected,
            // and we should pretend like it doesn't exist.
            ValueReference<K, V> valueReference = e.getValueReference();
            V entryValue = valueReference.get();
            if (entryValue == null) {
              if (isCollected(valueReference)) {
                int newCount = this.count - 1;
                ++modCount;
                ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
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
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            ValueReference<K, V> valueReference = e.getValueReference();
            V entryValue = valueReference.get();

            if (entryValue != null) {
              // TODO(kak): Remove this branch
            } else if (isCollected(valueReference)) {
              // TODO(kak): Remove this branch
            } else {
              return null;
            }

            ++modCount;
            ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
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
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            ValueReference<K, V> valueReference = e.getValueReference();
            V entryValue = valueReference.get();

            boolean explicitRemoval = false;
            if (map.valueEquivalence.equivalent(value, entryValue)) {
              explicitRemoval = true;
            } else if (isCollected(valueReference)) {
              // TODO(kak): Remove this branch
            } else {
              return false;
            }

            ++modCount;
            ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
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
          AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
          for (int i = 0; i < table.length(); ++i) {
            table.set(i, null);
          }
          clearReferenceQueues();
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
    ReferenceEntry<K, V> removeFromChain(ReferenceEntry<K, V> first, ReferenceEntry<K, V> entry) {
      int newCount = count;
      ReferenceEntry<K, V> newFirst = entry.getNext();
      for (ReferenceEntry<K, V> e = first; e != entry; e = e.getNext()) {
        ReferenceEntry<K, V> next = copyEntry(e, newFirst);
        if (next != null) {
          newFirst = next;
        } else {
          newCount--;
        }
      }
      this.count = newCount;
      return newFirst;
    }

    /**
     * Removes an entry whose key has been garbage collected.
     */
    @CanIgnoreReturnValue
    boolean reclaimKey(ReferenceEntry<K, V> entry, int hash) {
      lock();
      try {
        int newCount = count - 1;
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          if (e == entry) {
            ++modCount;
            ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
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

    /**
     * Removes an entry whose value has been garbage collected.
     */
    @CanIgnoreReturnValue
    boolean reclaimValue(K key, int hash, ValueReference<K, V> valueReference) {
      lock();
      try {
        int newCount = this.count - 1;
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            ValueReference<K, V> v = e.getValueReference();
            if (v == valueReference) {
              ++modCount;
              ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
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

    /**
     * Clears a value that has not yet been set, and thus does not require count to be modified.
     */
    @CanIgnoreReturnValue
    boolean clearValue(K key, int hash, ValueReference<K, V> valueReference) {
      lock();
      try {
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash
              && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            ValueReference<K, V> v = e.getValueReference();
            if (v == valueReference) {
              ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
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

    @CanIgnoreReturnValue
    @GuardedBy("this")
    boolean removeEntry(ReferenceEntry<K, V> entry, int hash) {
      int newCount = this.count - 1;
      AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
      int index = hash & (table.length() - 1);
      ReferenceEntry<K, V> first = table.get(index);

      for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
        if (e == entry) {
          ++modCount;
          ReferenceEntry<K, V> newFirst = removeFromChain(first, e);
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
     * null and it is not computing.
     */
    boolean isCollected(ValueReference<K, V> valueReference) {
      if (valueReference.isComputingReference()) {
        return false;
      }
      return (valueReference.get() == null);
    }

    /**
     * Gets the value from an entry. Returns {@code null} if the entry is invalid,
     * partially-collected or computing.
     */
    V getLiveValue(ReferenceEntry<K, V> entry) {
      if (entry.getKey() == null) {
        tryDrainReferenceQueues();
        return null;
      }
      V value = entry.getValueReference().get();
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
     * Performs routine cleanup prior to executing a write. This should be called every time a
     * write thread acquires the segment lock, immediately after acquiring the lock.
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
          drainReferenceQueues();
          readCount.set(0);
        } finally {
          unlock();
        }
      }
    }
  }

  static final class CleanupMapTask implements Runnable {
    final WeakReference<MapMakerInternalMap<?, ?>> mapReference;

    public CleanupMapTask(MapMakerInternalMap<?, ?> map) {
      this.mapReference = new WeakReference<MapMakerInternalMap<?, ?>>(map);
    }

    @Override
    public void run() {
      MapMakerInternalMap<?, ?> map = mapReference.get();
      if (map == null) {
        throw new CancellationException();
      }

      for (Segment<?, ?> segment : map.segments) {
        segment.runCleanup();
      }
    }
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
    Segment<K, V>[] segments = this.segments;
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
      if (sum != 0L) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int size() {
    Segment<K, V>[] segments = this.segments;
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
   * Returns the internal entry for the specified key. The entry may be computing or
   * partially collected. Does not impact recency ordering.
   */
  ReferenceEntry<K, V> getEntry(@Nullable Object key) {
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
    final Segment<K, V>[] segments = this.segments;
    long last = -1L;
    for (int i = 0; i < CONTAINS_VALUE_RETRIES; i++) {
      long sum = 0L;
      for (Segment<K, V> segment : segments) {
        // ensure visibility of most recent completed write
        int unused = segment.count; // read-volatile

        AtomicReferenceArray<ReferenceEntry<K, V>> table = segment.table;
        for (int j = 0; j < table.length(); j++) {
          for (ReferenceEntry<K, V> e = table.get(j); e != null; e = e.getNext()) {
            V v = segment.getLiveValue(e);
            if (v != null && valueEquivalence.equivalent(value, v)) {
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
    for (Segment<K, V> segment : segments) {
      segment.clear();
    }
  }

  transient Set<K> keySet;

  @Override
  public Set<K> keySet() {
    Set<K> ks = keySet;
    return (ks != null) ? ks : (keySet = new KeySet());
  }

  transient Collection<V> values;

  @Override
  public Collection<V> values() {
    Collection<V> vs = values;
    return (vs != null) ? vs : (values = new Values());
  }

  transient Set<Entry<K, V>> entrySet;

  @Override
  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> es = entrySet;
    return (es != null) ? es : (entrySet = new EntrySet());
  }

  // Iterator Support

  abstract class HashIterator<E> implements Iterator<E> {

    int nextSegmentIndex;
    int nextTableIndex;
    Segment<K, V> currentSegment;
    AtomicReferenceArray<ReferenceEntry<K, V>> currentTable;
    ReferenceEntry<K, V> nextEntry;
    WriteThroughEntry nextExternal;
    WriteThroughEntry lastReturned;

    HashIterator() {
      nextSegmentIndex = segments.length - 1;
      nextTableIndex = -1;
      advance();
    }

    @Override
    public abstract E next();

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

    /**
     * Finds the next entry in the current chain. Returns {@code true} if an entry was found.
     */
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

    /**
     * Finds the next entry in the current table. Returns {@code true} if an entry was found.
     */
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
    boolean advanceTo(ReferenceEntry<K, V> entry) {
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
   * Custom Entry class used by EntryIterator.next(), that relays setValue changes to the
   * underlying map.
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
    public <E> E[] toArray(E[] a) {
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

      return v != null && valueEquivalence.equivalent(e.getValue(), v);
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
    public <E> E[] toArray(E[] a) {
      return toArrayList(this).toArray(a);
    }
  }

  private static <E> ArrayList<E> toArrayList(Collection<E> c) {
    // Avoid calling ArrayList(Collection), which may call back into toArray.
    ArrayList<E> result = new ArrayList<E>(c.size());
    Iterators.addAll(result, c.iterator());
    return result;
  }

  // Serialization Support

  private static final long serialVersionUID = 5;

  Object writeReplace() {
    return new SerializationProxy<K, V>(
        keyStrength,
        valueStrength,
        keyEquivalence,
        valueEquivalence,
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
          keyStrength,
          valueStrength,
          keyEquivalence,
          valueEquivalence,
          concurrencyLevel,
          delegate);
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
