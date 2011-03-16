/*
 * Copyright (C) 2009 Google Inc.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Equivalence;
import com.google.common.base.Equivalences;
import com.google.common.base.FinalizableReferenceQueue;
import com.google.common.base.FinalizableSoftReference;
import com.google.common.base.FinalizableWeakReference;
import com.google.common.base.Ticker;
import com.google.common.collect.MapMaker.NullListener;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * The concurrent hash map implementation built by {@link MapMaker}.
 *
 * This implementation is heavily derived from revision 1.96 of
 * <a href="http://tinyurl.com/ConcurrentHashMap">ConcurrentHashMap.java</a>.
 *
 * @author Bob Lee
 * @author Doug Lea ({@code ConcurrentHashMap})
 */
class CustomConcurrentHashMap<K, V> extends AbstractMap<K, V>
    implements ConcurrentMap<K, V>, Serializable {
  /*
   * The basic strategy is to subdivide the table among Segments, each of which
   * itself is a concurrently readable hash table. The map supports
   * non-blocking reads and concurrent writes across different segments.
   *
   * If a maximum size is specified, a best-effort bounding is performed per
   * segment, using a page-replacement algorithm to determine which entries to
   * evict when the capacity has been exceeded.
   *
   * The page replacement algorithm's data structures are kept casually
   * consistent with the map. The ordering of writes to a segment is
   * sequentially consistent. An update to the map and recording of reads may
   * not be immediately reflected on the algorithm's data structures. These
   * structures are guarded by a lock and operations are applied in batches to
   * avoid lock contention. The penalty of applying the batches is spread across
   * threads so that the amortized cost is slightly higher than performing just
   * the operation without enforcing the capacity constraint.
   *
   * This implementation uses a per-segment queue to record a memento of the
   * additions, removals, and accesses that were performed on the map. The queue
   * is drained on writes and when it exceeds its capacity threshold.
   *
   * The Least Recently Used page replacement algorithm was chosen due to its
   * simplicity, high hit rate, and ability to be implemented with O(1) time
   * complexity. The initial LRU implementation operates per-segment rather
   * than globally for increased implementation simplicity. We expect the cache
   * hit rate to be similar to that of a global LRU algorithm.
   */

  /* ---------------- Constants -------------- */

  /**
   * The maximum capacity, used if a higher value is implicitly specified by
   * either of the constructors with arguments.  MUST be a power of two <=
   * 1<<30 to ensure that entries are indexable using ints.
   */
  static final int MAXIMUM_CAPACITY = 1 << 30;

  /**
   * The maximum number of segments to allow; used to bound constructor
   * arguments.
   */
  static final int MAX_SEGMENTS = 1 << 16; // slightly conservative

  /**
   * Number of cache access operations that can be buffered per segment before
   * the cache's recency ordering information is updated. This is used to avoid
   * lock contention by recording a memento of reads and delaying a lock
   * acquisition until the threshold is crossed or a mutation occurs.
   *
   * <p>This must be a (2^n)-1 as it is used as a mask.
   */
  static final int DRAIN_THRESHOLD = 0x3F;

  /**
   * Maximum number of entries to be cleaned up in a single cleanup run.
   * TODO(user): empirically optimize this
   */
  static final int CLEANUP_MAX = 16;

  /* ---------------- Fields -------------- */

  /**
   * Mask value for indexing into segments. The upper bits of a key's hash
   * code are used to choose the segment.
   */
  final transient int segmentMask;

  /**
   * Shift value for indexing within segments. Helps prevent entries that
   * end up in the same segment from also ending up in the same bucket.
   */
  final transient int segmentShift;

  /** The segments, each of which is a specialized hash table. */
  final transient Segment[] segments;

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

  /**
   * The maximum size of this map. MapMaker.UNSET_INT if there is no
   * maximum.
   */
  final int maximumSize;

  /**
   * How long after the last access to an entry the map will retain that
   * entry.
   */
  final long expireAfterAccessNanos;

  /**
   * How long after the last write to an entry the map will retain that
   * entry.
   */
  final long expireAfterWriteNanos;

  /** Entries waiting to be consumed by the eviction listener. */
  final Queue<ReferenceEntry<K, V>> evictionNotificationQueue;

  /**
   * A listener that is invoked when an entry is removed due to expiration or
   * garbage collection of soft/weak entries.
   */
  final MapEvictionListener<? super K, ? super V> evictionListener;

  /** Factory used to create new entries. */
  final transient EntryFactory entryFactory;

  /** Performs map housekeeping operations. */
  final Executor cleanupExecutor;

  /** Measures time in a testable way. */
  final Ticker ticker;

  /**
   * Creates a new, empty map with the specified strategy, initial capacity
   * and concurrency level.
   */
  CustomConcurrentHashMap(MapMaker builder) {
    concurrencyLevel = Math.min(builder.getConcurrencyLevel(), MAX_SEGMENTS);

    keyStrength = builder.getKeyStrength();
    valueStrength = builder.getValueStrength();

    keyEquivalence = builder.getKeyEquivalence();
    valueEquivalence = builder.getValueEquivalence();

    maximumSize = builder.maximumSize;
    expireAfterAccessNanos = builder.getExpireAfterAccessNanos();
    expireAfterWriteNanos = builder.getExpireAfterWriteNanos();

    entryFactory =
        EntryFactory.getFactory(keyStrength, expires(), evictsBySize());
    cleanupExecutor = builder.getCleanupExecutor();
    ticker = builder.getTicker();

    evictionListener = builder.getEvictionListener();
    evictionNotificationQueue = (evictionListener == NullListener.INSTANCE)
        ? CustomConcurrentHashMap.<ReferenceEntry<K, V>>discardingQueue()
        : new ConcurrentLinkedQueue<ReferenceEntry<K, V>>();

    int initialCapacity =
        Math.min(builder.getInitialCapacity(), MAXIMUM_CAPACITY);
    if (evictsBySize()) {
        initialCapacity = Math.min(initialCapacity, maximumSize);
    }

    // Find power-of-two sizes best matching arguments. Constraints:
    // (segmentCount <= maximumSize)
    // && (concurrencyLevel > maximumSize || segmentCount > concurrencyLevel)
    int segmentShift = 0;
    int segmentCount = 1;
    while (segmentCount < concurrencyLevel
        && (!evictsBySize() || segmentCount * 2 <= maximumSize)) {
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

    if (evictsBySize()) {
      // Ensure sum of segment max sizes = overall max size
      int maximumSegmentSize = maximumSize / segmentCount + 1;
      int remainder = maximumSize % segmentCount;
      for (int i = 0; i < this.segments.length; ++i) {
        if (i == remainder) {
          maximumSegmentSize--;
        }
        this.segments[i] = createSegment(segmentSize, maximumSegmentSize);
      }
    } else {
      for (int i = 0; i < this.segments.length; ++i) {
        this.segments[i] = createSegment(segmentSize,
            MapMaker.UNSET_INT);
      }
    }
  }

  boolean evictsBySize() {
    return maximumSize != MapMaker.UNSET_INT;
  }

  boolean expires() {
    return expiresAfterWrite() || expiresAfterAccess();
  }

  boolean expiresAfterWrite() {
    return expireAfterWriteNanos > 0;
  }

  boolean expiresAfterAccess() {
    return expireAfterAccessNanos > 0;
  }

  boolean isInlineCleanup() {
    return cleanupExecutor == MapMaker.DEFAULT_CLEANUP_EXECUTOR;
  }

  enum Strength {
    /*
     * TODO(kevinb): If we strongly reference the value and aren't computing, we
     * needn't wrap the value. This could save ~8 bytes per entry.
     */

    STRONG {
      @Override <K, V> ValueReference<K, V> referenceValue(
          ReferenceEntry<K, V> entry, V value) {
        return new StrongValueReference<K, V>(value);
      }
      @Override Equivalence<Object> defaultEquivalence() {
        return Equivalences.equals();
      }
    },

    SOFT {
      @Override <K, V> ValueReference<K, V> referenceValue(
          ReferenceEntry<K, V> entry, V value) {
        return new SoftValueReference<K, V>(value, entry);
      }
      @Override Equivalence<Object> defaultEquivalence() {
        return Equivalences.identity();
      }
    },

    WEAK {
      @Override <K, V> ValueReference<K, V> referenceValue(
          ReferenceEntry<K, V> entry, V value) {
        return new WeakValueReference<K, V>(value, entry);
      }
      @Override Equivalence<Object> defaultEquivalence() {
        return Equivalences.identity();
      }
    };

    /**
     * Creates a reference for the given value according to this value
     * strength.
     */
    abstract <K, V> ValueReference<K, V> referenceValue(
        ReferenceEntry<K, V> entry, V value);

    /**
     * Returns the default equivalence strategy used to compare and hash
     * keys or values referenced at this strength. This strategy will be used
     * unless the user explicitly specifies an alternate strategy.
     */
    abstract Equivalence<Object> defaultEquivalence();
  }

  /**
   * Creates new entries.
   */
  enum EntryFactory {
    STRONG {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          @Nullable ReferenceEntry<K, V> next) {
        return new StrongEntry<K, V>(map, key, hash, next);
      }
    },
    STRONG_EXPIRABLE {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          @Nullable ReferenceEntry<K, V> next) {
        return new StrongExpirableEntry<K, V>(map, key, hash, next);
      }
      @Override <K, V> ReferenceEntry<K, V> copyEntry(
          CustomConcurrentHashMap<K, V> map,
          ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
        ReferenceEntry<K, V> newEntry = super.copyEntry(map, original, newNext);
        copyExpirableEntry(original, newEntry);
        return newEntry;
      }
    },
    STRONG_EVICTABLE {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          @Nullable ReferenceEntry<K, V> next) {
        return new StrongEvictableEntry<K, V>(map, key, hash, next);
      }
      @Override <K, V> ReferenceEntry<K, V> copyEntry(
          CustomConcurrentHashMap<K, V> map,
          ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
        ReferenceEntry<K, V> newEntry = super.copyEntry(map, original, newNext);
        copyEvictableEntry(original, newEntry);
        return newEntry;
      }
    },
    STRONG_EXPIRABLE_EVICTABLE {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          @Nullable ReferenceEntry<K, V> next) {
        return new StrongExpirableEvictableEntry<K, V>(map, key, hash, next);
      }
      @Override <K, V> ReferenceEntry<K, V> copyEntry(
          CustomConcurrentHashMap<K, V> map,
          ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
        ReferenceEntry<K, V> newEntry = super.copyEntry(map, original, newNext);
        copyExpirableEntry(original, newEntry);
        copyEvictableEntry(original, newEntry);
        return newEntry;
      }
    },

    SOFT {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          @Nullable ReferenceEntry<K, V> next) {
        return new SoftEntry<K, V>(map, key, hash, next);
      }
    },
    SOFT_EXPIRABLE {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          @Nullable ReferenceEntry<K, V> next) {
        return new SoftExpirableEntry<K, V>(map, key, hash, next);
      }
      @Override <K, V> ReferenceEntry<K, V> copyEntry(
          CustomConcurrentHashMap<K, V> map,
          ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
        ReferenceEntry<K, V> newEntry = super.copyEntry(map, original, newNext);
        copyExpirableEntry(original, newEntry);
        return newEntry;
      }
    },
    SOFT_EVICTABLE {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          @Nullable ReferenceEntry<K, V> next) {
        return new SoftEvictableEntry<K, V>(map, key, hash, next);
      }
      @Override <K, V> ReferenceEntry<K, V> copyEntry(
          CustomConcurrentHashMap<K, V> map,
          ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
        ReferenceEntry<K, V> newEntry = super.copyEntry(map, original, newNext);
        copyEvictableEntry(original, newEntry);
        return newEntry;
      }
    },
    SOFT_EXPIRABLE_EVICTABLE {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          @Nullable ReferenceEntry<K, V> next) {
        return new SoftExpirableEvictableEntry<K, V>(map, key, hash, next);
      }
      @Override <K, V> ReferenceEntry<K, V> copyEntry(
          CustomConcurrentHashMap<K, V> map,
          ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
        ReferenceEntry<K, V> newEntry = super.copyEntry(map, original, newNext);
        copyExpirableEntry(original, newEntry);
        copyEvictableEntry(original, newEntry);
        return newEntry;
      }
    },

    WEAK {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          @Nullable ReferenceEntry<K, V> next) {
        return new WeakEntry<K, V>(map, key, hash, next);
      }
    },
    WEAK_EXPIRABLE {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          @Nullable ReferenceEntry<K, V> next) {
        return new WeakExpirableEntry<K, V>(map, key, hash, next);
      }
      @Override <K, V> ReferenceEntry<K, V> copyEntry(
          CustomConcurrentHashMap<K, V> map,
          ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
        ReferenceEntry<K, V> newEntry = super.copyEntry(map, original, newNext);
        copyExpirableEntry(original, newEntry);
        return newEntry;
      }
    },
    WEAK_EVICTABLE {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          @Nullable ReferenceEntry<K, V> next) {
        return new WeakEvictableEntry<K, V>(map, key, hash, next);
      }
      @Override <K, V> ReferenceEntry<K, V> copyEntry(
          CustomConcurrentHashMap<K, V> map,
          ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
        ReferenceEntry<K, V> newEntry = super.copyEntry(map, original, newNext);
        copyEvictableEntry(original, newEntry);
        return newEntry;
      }
    },
    WEAK_EXPIRABLE_EVICTABLE {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          @Nullable ReferenceEntry<K, V> next) {
        return new WeakExpirableEvictableEntry<K, V>(map, key, hash, next);
      }
      @Override <K, V> ReferenceEntry<K, V> copyEntry(
          CustomConcurrentHashMap<K, V> map,
          ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
        ReferenceEntry<K, V> newEntry = super.copyEntry(map, original, newNext);
        copyExpirableEntry(original, newEntry);
        copyEvictableEntry(original, newEntry);
        return newEntry;
      }
    };

    /**
     * Masks used to compute indices in the following table.
     */
    static final int EXPIRABLE_MASK = 1;
    static final int EVICTABLE_MASK = 2;

    /**
     * Look-up table for factories. First dimension is the reference type.
     * The second dimension is the result of OR-ing the feature masks.
     */
    static final EntryFactory[][] factories = {
      { STRONG, STRONG_EXPIRABLE, STRONG_EVICTABLE, STRONG_EXPIRABLE_EVICTABLE },
      { SOFT,   SOFT_EXPIRABLE,   SOFT_EVICTABLE,   SOFT_EXPIRABLE_EVICTABLE   },
      { WEAK,   WEAK_EXPIRABLE,   WEAK_EVICTABLE,   WEAK_EXPIRABLE_EVICTABLE   }
    };

    static EntryFactory getFactory(Strength keyStrength,
        boolean expireAfterWrite, boolean evictsBySize) {
      int flags = (expireAfterWrite ? EXPIRABLE_MASK : 0)
          | (evictsBySize ? EVICTABLE_MASK : 0);
      return factories[keyStrength.ordinal()][flags];
    }

    /**
     * Creates a new entry.
     *
     * @param map to create the entry for
     * @param key of the entry
     * @param hash of the key
     * @param next entry in the same bucket
     */
    abstract <K, V> ReferenceEntry<K, V> newEntry(
        CustomConcurrentHashMap<K, V> map, K key, int hash,
        @Nullable ReferenceEntry<K, V> next);

    /**
     * Copies an entry, assigning it a new {@code next} entry.
     *
     * @param original the entry to copy
     * @param newNext entry in the same bucket
     */
    @GuardedBy("Segment.this")
    <K, V> ReferenceEntry<K, V> copyEntry(
        CustomConcurrentHashMap<K, V> map,
        ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
      return newEntry(map, original.getKey(), original.getHash(), newNext);
    }

    @GuardedBy("Segment.this")
    <K, V> void copyExpirableEntry(
        ReferenceEntry<K, V> original, ReferenceEntry<K, V> newEntry) {
      // TODO(user): when we link values instead of entries this method can go
      // away, as can connectExpirables, nullifyExpirable.
      newEntry.setExpirationTime(original.getExpirationTime());

      connectExpirables(original.getPreviousExpirable(), newEntry);
      connectExpirables(newEntry, original.getNextExpirable());

      nullifyExpirable(original);
    }

    @GuardedBy("Segment.this")
    <K, V> void copyEvictableEntry(
        ReferenceEntry<K, V> original, ReferenceEntry<K, V> newEntry) {
      // TODO(user): when we link values instead of entries this method can go
      // away, as can connectEvictables, nullifyEvictable.
      connectEvictables(original.getPreviousEvictable(), newEntry);
      connectEvictables(newEntry, original.getNextEvictable());

      nullifyEvictable(original);
    }
  }

  /** A reference to a value. */
  interface ValueReference<K, V> {
    /**
     * Gets the value. Does not block or throw exceptions.
     */
    V get();

    /** Creates a copy of this reference for the given entry. */
    ValueReference<K, V> copyFor(ReferenceEntry<K, V> entry);

    /**
     * Waits for a value that may still be computing. Unlike get(),
     * this method can block (in the case of FutureValueReference) or
     * throw an exception.
     */
    V waitForValue() throws InterruptedException;

    /**
     * Clears this reference object. This intentionally mimics {@link
     * java.lang.ref.Reference#clear()}, and indeed is implemented by
     * {@code Reference} subclasses for weak and soft values.
     */
    void clear();

    /**
     * Returns true if the value type is a computing reference (regardless of
     * whether or not computation has completed). This is necessary to
     * distiguish between partially-collected entries and computing entries,
     * which need to be cleaned up differently.
     */
    boolean isComputingReference();

    /**
     * Invoked after the value has been garbage collected.
     */
    void notifyValueReclaimed();
  }

  /**
   * Placeholder. Indicates that the value hasn't been set yet.
   */
  static final ValueReference<Object, Object> UNSET
      = new ValueReference<Object, Object>() {
    @Override
    public Object get() {
      return null;
    }
    @Override
    public ValueReference<Object, Object> copyFor(
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
    public void notifyValueReclaimed() {}
    @Override
    public void clear() {}
  };

  /**
   * Singleton placeholder that indicates a value is being computed.
   */
  @SuppressWarnings("unchecked")
  // Safe because impl never uses a parameter or returns any non-null value
  static <K, V> ValueReference<K, V> unset() {
    return (ValueReference<K, V>) UNSET;
  }

  /** Wrapper class ensures that queue isn't created until it's used. */
  private static class QueueHolder {
    static final FinalizableReferenceQueue queue
        = new FinalizableReferenceQueue();
  }

  /**
   * An entry in a reference map.
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
     * Removes this entry from the map if its value reference hasn't
     * changed.  Used to clean up after values. The value reference can
     * just call this method on the entry so it doesn't have to keep
     * its own reference to the map.
     */
    void valueReclaimed(ValueReference<K, V> valueReference);

    /** Gets the next entry in the chain. */
    ReferenceEntry<K, V> getNext();

    /** Gets the entry's hash. */
    int getHash();

    /** Gets the key for this entry. */
    K getKey();

    /*
     * Used by entries that are expirable. Expirable entries are
     * maintained in a doubly-linked list. New entries are added at the tail
     * of the list at write time; stale entries are expired from the head
     * of the list.
     */

    /** Gets the entry expiration time in ns. */
    long getExpirationTime();

    /** Sets the entry expiration time in ns. */
    void setExpirationTime(long time);

    /** Gets the next entry in the recency list. */
    ReferenceEntry<K, V> getNextExpirable();

    /** Sets the next entry in the recency list. */
    void setNextExpirable(ReferenceEntry<K, V> next);

    /** Gets the previous entry in the recency list. */
    ReferenceEntry<K, V> getPreviousExpirable();

    /** Sets the previous entry in the recency list. */
    void setPreviousExpirable(ReferenceEntry<K, V> previous);

    /*
     * Implemented by entries that are evictable. Evictable entries are
     * maintained in a doubly-linked list. New entries are added at the tail of
     * the list at write time and stale entries are expired from the head of the
     * list.
     */

    /** Gets the next entry in the recency list. */
    ReferenceEntry<K, V> getNextEvictable();

    /** Sets the next entry in the recency list. */
    void setNextEvictable(ReferenceEntry<K, V> next);

    /** Gets the previous entry in the recency list. */
    ReferenceEntry<K, V> getPreviousEvictable();

    /** Sets the previous entry in the recency list. */
    void setPreviousEvictable(ReferenceEntry<K, V> previous);
  }

  private enum NullEntry implements ReferenceEntry<Object, Object> {
    INSTANCE;

    @Override
    public ValueReference<Object, Object> getValueReference() {
      return null;
    }
    @Override
    public void setValueReference(
        ValueReference<Object, Object> valueReference) {}
    @Override
    public void valueReclaimed(ValueReference<Object, Object> v) {}
    @Override
    public ReferenceEntry<Object, Object> getNext() {
      return null;
    }
    @Override
    public int getHash() {
      return 0;
    }
    @Override
    public Object getKey() {
      return null;
    }

    @Override
    public long getExpirationTime() {
      return 0;
    }
    @Override
    public void setExpirationTime(long time) {}
    @Override
    public ReferenceEntry<Object, Object> getNextExpirable() {
      return this;
    }
    @Override
    public void setNextExpirable(ReferenceEntry<Object, Object> next) {}

    @Override
    public ReferenceEntry<Object, Object> getPreviousExpirable() {
      return this;
    }
    @Override
    public void setPreviousExpirable(ReferenceEntry<Object, Object> previous) {}

    @Override
    public ReferenceEntry<Object, Object> getNextEvictable() {
      return this;
    }
    @Override
    public void setNextEvictable(ReferenceEntry<Object, Object> next) {}

    @Override
    public ReferenceEntry<Object, Object> getPreviousEvictable() {
      return this;
    }
    @Override
    public void setPreviousEvictable(ReferenceEntry<Object, Object> previous) {}
  }

  @SuppressWarnings("unchecked")
  // Safe because impl never uses a parameter or returns any non-null value
  static <K, V> ReferenceEntry<K, V> nullEntry() {
    return (ReferenceEntry<K, V>) NullEntry.INSTANCE;
  }

  static final Queue<Object> DISCARDING_QUEUE = new AbstractQueue<Object>() {
    @Override
    public boolean offer(Object o) {
      return true;
    }

    @Override
    public Object peek() {
      return null;
    }

    @Override
    public Object poll() {
      return null;
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public Iterator<Object> iterator() {
      return Iterators.emptyIterator();
    }
  };

  /**
   * Queue that discards all elements.
   */
  @SuppressWarnings("unchecked")
  // Safe because impl never uses a parameter or returns any non-null value
  static <E> Queue<E> discardingQueue() {
    return (Queue<E>) DISCARDING_QUEUE;
  }

  /*
   * Note: All of this duplicate code sucks, but it saves a lot of memory.
   * If only Java had mixins! To maintain this code, make a change for
   * the strong reference type. Then, cut and paste, and replace "Strong"
   * with "Soft" or "Weak" within the pasted text. The primary difference
   * is that strong entries store the key reference directly while soft
   * and weak entries delegate to their respective superclasses.
   */

  /**
   * Used for strongly-referenced keys.
   */
  private static class StrongEntry<K, V> implements ReferenceEntry<K, V> {
    final K key;

    StrongEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        @Nullable ReferenceEntry<K, V> next) {
      this.map = map;
      this.key = key;
      this.hash = hash;
      this.next = next;
    }

    @Override
    public K getKey() {
      return this.key;
    }

    // null expiration
    @Override
    public long getExpirationTime() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setExpirationTime(long time) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ReferenceEntry<K, V> getNextExpirable() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setNextExpirable(ReferenceEntry<K, V> next) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ReferenceEntry<K, V> getPreviousExpirable() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
      throw new UnsupportedOperationException();
    }

    // null eviction
    @Override
    public ReferenceEntry<K, V> getNextEvictable() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setNextEvictable(ReferenceEntry<K, V> next) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ReferenceEntry<K, V> getPreviousEvictable() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
      throw new UnsupportedOperationException();
    }

    // The code below is exactly the same for each entry type.

    final CustomConcurrentHashMap<K, V> map;
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
      previous.clear();
    }
    @Override
    public void valueReclaimed(ValueReference<K, V> v) {
      map.reclaimValue(this, v);
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

  private static class StrongExpirableEntry<K, V> extends StrongEntry<K, V>
      implements ReferenceEntry<K, V> {
    StrongExpirableEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        @Nullable ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each expirable entry type.

    volatile long time = Long.MAX_VALUE;
    @Override
    public long getExpirationTime() {
      return time;
    }
    @Override
    public void setExpirationTime(long time) {
      this.time = time;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> nextExpirable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getNextExpirable() {
      return nextExpirable;
    }
    @Override
    public void setNextExpirable(ReferenceEntry<K, V> next) {
      this.nextExpirable = next;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> previousExpirable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getPreviousExpirable() {
      return previousExpirable;
    }
    @Override
    public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
      this.previousExpirable = previous;
    }
  }

  private static class StrongEvictableEntry<K, V> extends StrongEntry<K, V>
      implements ReferenceEntry<K, V> {
    StrongEvictableEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        @Nullable ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each evictable entry type.

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> nextEvictable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getNextEvictable() {
      return nextEvictable;
    }
    @Override
    public void setNextEvictable(ReferenceEntry<K, V> next) {
      this.nextEvictable = next;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> previousEvictable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getPreviousEvictable() {
      return previousEvictable;
    }
    @Override
    public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
      this.previousEvictable = previous;
    }
  }

  private static class StrongExpirableEvictableEntry<K, V>
      extends StrongEntry<K, V> implements ReferenceEntry<K, V> {
    StrongExpirableEvictableEntry(CustomConcurrentHashMap<K, V> map, K key,
        int hash, @Nullable ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each expirable entry type.

    volatile long time = Long.MAX_VALUE;
    @Override
    public long getExpirationTime() {
      return time;
    }
    @Override
    public void setExpirationTime(long time) {
      this.time = time;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> nextExpirable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getNextExpirable() {
      return nextExpirable;
    }
    @Override
    public void setNextExpirable(ReferenceEntry<K, V> next) {
      this.nextExpirable = next;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> previousExpirable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getPreviousExpirable() {
      return previousExpirable;
    }
    @Override
    public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
      this.previousExpirable = previous;
    }

    // The code below is exactly the same for each evictable entry type.

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> nextEvictable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getNextEvictable() {
      return nextEvictable;
    }
    @Override
    public void setNextEvictable(ReferenceEntry<K, V> next) {
      this.nextEvictable = next;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> previousEvictable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getPreviousEvictable() {
      return previousEvictable;
    }
    @Override
    public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
      this.previousEvictable = previous;
    }
  }

  /**
   * Used for softly-referenced keys.
   */
  private static class SoftEntry<K, V> extends FinalizableSoftReference<K>
      implements ReferenceEntry<K, V> {
    SoftEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        @Nullable ReferenceEntry<K, V> next) {
      super(key, QueueHolder.queue);
      this.map = map;
      this.hash = hash;
      this.next = next;
    }

    @Override
    public K getKey() {
      return get();
    }

    @Override
    public void finalizeReferent() {
      if (map.removeEntry(this)) {
        // send removal notification if the entry is in the map
        map.evictionNotificationQueue.offer(this);
      }
    }

    // null expiration
    @Override
    public long getExpirationTime() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setExpirationTime(long time) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ReferenceEntry<K, V> getNextExpirable() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setNextExpirable(ReferenceEntry<K, V> next) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ReferenceEntry<K, V> getPreviousExpirable() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
      throw new UnsupportedOperationException();
    }

    // null eviction
    @Override
    public ReferenceEntry<K, V> getNextEvictable() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setNextEvictable(ReferenceEntry<K, V> next) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ReferenceEntry<K, V> getPreviousEvictable() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
      throw new UnsupportedOperationException();
    }

    // The code below is exactly the same for each entry type.

    final CustomConcurrentHashMap<K, V> map;
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
      previous.clear();
    }
    @Override
    public void valueReclaimed(ValueReference<K, V> v) {
      map.reclaimValue(this, v);
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

  private static class SoftExpirableEntry<K, V> extends SoftEntry<K, V>
      implements ReferenceEntry<K, V> {
    SoftExpirableEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        @Nullable ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each expirable entry type.

    volatile long time = Long.MAX_VALUE;
    @Override
    public long getExpirationTime() {
      return time;
    }
    @Override
    public void setExpirationTime(long time) {
      this.time = time;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> nextExpirable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getNextExpirable() {
      return nextExpirable;
    }
    @Override
    public void setNextExpirable(ReferenceEntry<K, V> next) {
      this.nextExpirable = next;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> previousExpirable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getPreviousExpirable() {
      return previousExpirable;
    }
    @Override
    public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
      this.previousExpirable = previous;
    }
  }

  private static class SoftEvictableEntry<K, V> extends SoftEntry<K, V>
      implements ReferenceEntry<K, V> {
    SoftEvictableEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        @Nullable ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each evictable entry type.

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> nextEvictable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getNextEvictable() {
      return nextEvictable;
    }
    @Override
    public void setNextEvictable(ReferenceEntry<K, V> next) {
      this.nextEvictable = next;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> previousEvictable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getPreviousEvictable() {
      return previousEvictable;
    }
    @Override
    public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
      this.previousEvictable = previous;
    }
  }

  private static class SoftExpirableEvictableEntry<K, V>
      extends SoftEntry<K, V> implements ReferenceEntry<K, V> {
    SoftExpirableEvictableEntry(CustomConcurrentHashMap<K, V> map, K key,
        int hash, @Nullable ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each expirable entry type.

    volatile long time = Long.MAX_VALUE;
    @Override
    public long getExpirationTime() {
      return time;
    }
    @Override
    public void setExpirationTime(long time) {
      this.time = time;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> nextExpirable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getNextExpirable() {
      return nextExpirable;
    }
    @Override
    public void setNextExpirable(ReferenceEntry<K, V> next) {
      this.nextExpirable = next;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> previousExpirable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getPreviousExpirable() {
      return previousExpirable;
    }
    @Override
    public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
      this.previousExpirable = previous;
    }

    // The code below is exactly the same for each evictable entry type.

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> nextEvictable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getNextEvictable() {
      return nextEvictable;
    }
    @Override
    public void setNextEvictable(ReferenceEntry<K, V> next) {
      this.nextEvictable = next;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> previousEvictable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getPreviousEvictable() {
      return previousEvictable;
    }
    @Override
    public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
      this.previousEvictable = previous;
    }
  }

  /**
   * Used for weakly-referenced keys.
   */
  private static class WeakEntry<K, V> extends FinalizableWeakReference<K>
      implements ReferenceEntry<K, V> {
    WeakEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        @Nullable ReferenceEntry<K, V> next) {
      super(key, QueueHolder.queue);
      this.map = map;
      this.hash = hash;
      this.next = next;
    }

    @Override
    public K getKey() {
      return get();
    }

    @Override
    public void finalizeReferent() {
      if (map.removeEntry(this)) {
        // send removal notification if the entry is in the map
        map.evictionNotificationQueue.offer(this);
      }
    }

    // null expiration
    @Override
    public long getExpirationTime() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setExpirationTime(long time) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ReferenceEntry<K, V> getNextExpirable() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setNextExpirable(ReferenceEntry<K, V> next) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ReferenceEntry<K, V> getPreviousExpirable() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
      throw new UnsupportedOperationException();
    }

    // null eviction
    @Override
    public ReferenceEntry<K, V> getNextEvictable() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setNextEvictable(ReferenceEntry<K, V> next) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ReferenceEntry<K, V> getPreviousEvictable() {
      throw new UnsupportedOperationException();
    }
    @Override
    public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
      throw new UnsupportedOperationException();
    }

    // The code below is exactly the same for each entry type.

    final CustomConcurrentHashMap<K, V> map;
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
      previous.clear();
    }
    @Override
    public void valueReclaimed(ValueReference<K, V> v) {
      map.reclaimValue(this, v);
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

  private static class WeakExpirableEntry<K, V> extends WeakEntry<K, V>
      implements ReferenceEntry<K, V> {
    WeakExpirableEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        @Nullable ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each expirable entry type.

    volatile long time = Long.MAX_VALUE;
    @Override
    public long getExpirationTime() {
      return time;
    }
    @Override
    public void setExpirationTime(long time) {
      this.time = time;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> nextExpirable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getNextExpirable() {
      return nextExpirable;
    }
    @Override
    public void setNextExpirable(ReferenceEntry<K, V> next) {
      this.nextExpirable = next;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> previousExpirable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getPreviousExpirable() {
      return previousExpirable;
    }
    @Override
    public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
      this.previousExpirable = previous;
    }
  }

  private static class WeakEvictableEntry<K, V> extends WeakEntry<K, V>
      implements ReferenceEntry<K, V> {
    WeakEvictableEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        @Nullable ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each evictable entry type.

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> nextEvictable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getNextEvictable() {
      return nextEvictable;
    }
    @Override
    public void setNextEvictable(ReferenceEntry<K, V> next) {
      this.nextEvictable = next;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> previousEvictable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getPreviousEvictable() {
      return previousEvictable;
    }
    @Override
    public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
      this.previousEvictable = previous;
    }
  }

  private static class WeakExpirableEvictableEntry<K, V>
      extends WeakEntry<K, V> implements ReferenceEntry<K, V> {
    WeakExpirableEvictableEntry(CustomConcurrentHashMap<K, V> map, K key,
        int hash, @Nullable ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each expirable entry type.

    volatile long time = Long.MAX_VALUE;
    @Override
    public long getExpirationTime() {
      return time;
    }
    @Override
    public void setExpirationTime(long time) {
      this.time = time;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> nextExpirable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getNextExpirable() {
      return nextExpirable;
    }
    @Override
    public void setNextExpirable(ReferenceEntry<K, V> next) {
      this.nextExpirable = next;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> previousExpirable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getPreviousExpirable() {
      return previousExpirable;
    }
    @Override
    public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
      this.previousExpirable = previous;
    }

    // The code below is exactly the same for each evictable entry type.

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> nextEvictable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getNextEvictable() {
      return nextEvictable;
    }
    @Override
    public void setNextEvictable(ReferenceEntry<K, V> next) {
      this.nextEvictable = next;
    }

    @GuardedBy("Segment.this")
    ReferenceEntry<K, V> previousEvictable = nullEntry();
    @Override
    public ReferenceEntry<K, V> getPreviousEvictable() {
      return previousEvictable;
    }
    @Override
    public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
      this.previousEvictable = previous;
    }
  }

  /** References a weak value. */
  private static class WeakValueReference<K, V>
      extends FinalizableWeakReference<V>
      implements ValueReference<K, V> {
    final ReferenceEntry<K, V> entry;

    WeakValueReference(V referent, ReferenceEntry<K, V> entry) {
      super(referent, QueueHolder.queue);
      this.entry = entry;
    }

    @Override
    public void notifyValueReclaimed() {
      finalizeReferent();
    }

    @Override
    public void finalizeReferent() {
      entry.valueReclaimed(this);
    }

    @Override
    public ValueReference<K, V> copyFor(ReferenceEntry<K, V> entry) {
      return new WeakValueReference<K, V>(get(), entry);
    }

    public boolean isComputingReference() {
      return false;
    }

    public V waitForValue() {
      return get();
    }
  }

  /** References a soft value. */
  private static class SoftValueReference<K, V>
      extends FinalizableSoftReference<V>
      implements ValueReference<K, V> {
    final ReferenceEntry<K, V> entry;

    SoftValueReference(V referent, ReferenceEntry<K, V> entry) {
      super(referent, QueueHolder.queue);
      this.entry = entry;
    }

    public void notifyValueReclaimed() {
      finalizeReferent();
    }

    public void finalizeReferent() {
      entry.valueReclaimed(this);
    }

    public ValueReference<K, V> copyFor(ReferenceEntry<K, V> entry) {
      return new SoftValueReference<K, V>(get(), entry);
    }

    public boolean isComputingReference() {
      return false;
    }

    public V waitForValue() {
      return get();
    }
  }

  /** References a strong value. */
  private static class StrongValueReference<K, V>
      implements ValueReference<K, V> {
    final V referent;

    StrongValueReference(V referent) {
      this.referent = referent;
    }

    public V get() {
      return referent;
    }

    public ValueReference<K, V> copyFor(ReferenceEntry<K, V> entry) {
      return this;
    }

    public boolean isComputingReference() {
      return false;
    }

    public V waitForValue() {
      return get();
    }

    public void notifyValueReclaimed() {}

    public void clear() {}
  }

  /**
   * Applies a supplemental hash function to a given hash code, which defends
   * against poor quality hash functions. This is critical when the
   * concurrent hash map uses power-of-two length hash tables, that otherwise
   * encounter collisions for hash codes that do not differ in lower or upper
   * bits.
   *
   * @param h hash code
   */
  private static int rehash(int h) {
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

  @GuardedBy("Segment.this")
  ReferenceEntry<K, V> newEntry(K key, int hash, ReferenceEntry<K, V> next) {
    return entryFactory.newEntry(this, key, hash, next);
  }

  @GuardedBy("Segment.this")
  ReferenceEntry<K, V> copyEntry(
      ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
    ValueReference<K, V> valueReference = original.getValueReference();
    ReferenceEntry<K, V> newEntry
        = entryFactory.copyEntry(this, original, newNext);
    newEntry.setValueReference(valueReference.copyFor(newEntry));
    return newEntry;
  }

  @GuardedBy("Segment.this")
  ValueReference<K, V> newValueReference(ReferenceEntry<K, V> entry, V value) {
    return valueStrength.referenceValue(entry, value);
  }

  int hash(Object key) {
    int h = keyEquivalence.hash(checkNotNull(key));
    return rehash(h);
  }

  void reclaimValue(ReferenceEntry<K, V> entry,
      ValueReference<K, V> valueReference) {
    int hash = entry.getHash();
    Segment segment = segmentFor(hash);
    segment.unsetValue(entry.getKey(), hash, valueReference);
    if (!segment.isHeldByCurrentThread()) { // don't cleanup inside of put
      segment.postWriteCleanup();
    }
  }

  boolean removeEntry(ReferenceEntry<K, V> entry) {
    int hash = entry.getHash();
    return segmentFor(hash).removeEntry(entry, hash);
  }

  // Entries in the map can be in the following states:
  // Valid:
  // - Live: valid key/value are set
  // - Computing: computation is pending
  // Invalid:
  // - Expired: time expired (key/value may still be set)
  // - Collected: key/value was partially collected, but not yet cleaned up
  // - Unset: marked as unset, awaiting cleanup or reuse

  @VisibleForTesting boolean isLive(ReferenceEntry<K, V> entry) {
    return entry.getKey() != null && getLiveValue(entry) != null;
  }

  /**
   * Returns true if the entry has expired.
   */
  boolean isExpired(ReferenceEntry<K, V> entry) {
    return isExpired(entry, ticker.read());
  }

  /**
   * Returns true if the entry has expired.
   */
  boolean isExpired(ReferenceEntry<K, V> entry, long now) {
    // if the expiration time had overflowed, this "undoes" the overflow
    return now - entry.getExpirationTime() > 0;
  }

  /**
   * Returns true if the entry has been partially collected, meaning that either
   * the key is null, or the value is null and it is not computing.
   */
  boolean isCollected(ReferenceEntry<K, V> entry) {
    if (entry.getKey() == null) {
      return true;
    }
    ValueReference<K, V> valueReference = entry.getValueReference();
    if (valueReference.isComputingReference()) {
      return false;
    }
    return valueReference.get() == null;
  }

  boolean isUnset(ReferenceEntry<K, V> entry) {
    return isUnset(entry.getValueReference());
  }

  boolean isUnset(ValueReference<K, V> valueReference) {
    return valueReference == UNSET;
  }

  /**
   * Gets the value from an entry. Returns null if the entry is invalid,
   * partially-collected, computing, or expired. This method is unnecessary in
   * blocks that call preWriteCleanup() directly, which can simply assume that
   * remaining entries are not expired, and only need compare the value to
   * null.
   */
  V getLiveValue(ReferenceEntry<K, V> entry) {
    V value = entry.getValueReference().get();
    return (expires() && isExpired(entry)) ? null : value;
  }

  // expiration

  @GuardedBy("Segment.this")
  static <K, V> void connectExpirables(ReferenceEntry<K, V> previous,
      ReferenceEntry<K, V> next) {
    previous.setNextExpirable(next);
    next.setPreviousExpirable(previous);
  }

  @GuardedBy("Segment.this")
  static <K, V> void nullifyExpirable(ReferenceEntry<K, V> nulled) {
    ReferenceEntry<K, V> nullEntry = nullEntry();
    nulled.setNextExpirable(nullEntry);
    nulled.setPreviousExpirable(nullEntry);
  }

  // eviction

  void enqueueNotification(K key, int hash,
      ValueReference<K, V> valueReference) {
    ReferenceEntry<K, V> notifyEntry = newEntry(key, hash, null);
    notifyEntry.setValueReference(valueReference.copyFor(notifyEntry));
    evictionNotificationQueue.offer(notifyEntry);
  }

  void enqueueNotification(K key, int hash) {
    ReferenceEntry<K, V> notifyEntry = newEntry(key, hash, null);
    evictionNotificationQueue.offer(notifyEntry);
  }

  /**
   * Notifies listeners that an entry has been automatically removed due to
   * expiration, eviction, or eligibility for garbage collection. This should
   * be called every time expireEntries or evictEntry is called (once the lock
   * is released). It must only be called from user threads (e.g. not from
   * garbage collection callbacks).
   */
  void processPendingNotifications() {
    ReferenceEntry<K, V> entry;
    while ((entry = evictionNotificationQueue.poll()) != null) {
      evictionListener.onEviction(entry.getKey(),
          entry.getValueReference().get());
    }
  }

  /** Links the evitables together. */
  @GuardedBy("Segment.this")
  static <K, V> void connectEvictables(ReferenceEntry<K, V> previous,
      ReferenceEntry<K, V> next) {
    previous.setNextEvictable(next);
    next.setPreviousEvictable(previous);
  }

  @GuardedBy("Segment.this")
  static <K, V> void nullifyEvictable(ReferenceEntry<K, V> nulled) {
    ReferenceEntry<K, V> nullEntry = nullEntry();
    nulled.setNextEvictable(nullEntry);
    nulled.setPreviousEvictable(nullEntry);
  }

  @SuppressWarnings("unchecked")
  final Segment[] newSegmentArray(int ssize) {
    // Note: This is the only way I could figure out how to create
    // a segment array (the compiler has a tough time with arrays of
    // inner classes of generic types apparently). Safe because we're
    // restricting what can go in the array and no one has an
    // unrestricted reference.
    return (Segment[]) Array.newInstance(Segment.class, ssize);
  }

  /* ---------------- Small Utilities -------------- */

  /**
   * Returns the segment that should be used for a key with the given hash.
   *
   * @param hash the hash code for the key
   * @return the segment
   */
  Segment segmentFor(int hash) {
    // TODO(user): Lazily create segments?
    return segments[(hash >>> segmentShift) & segmentMask];
  }

  Segment createSegment(int initialCapacity, int maxSegmentSize) {
    return new Segment(initialCapacity, maxSegmentSize);
  }

  /* ---------------- Inner Classes -------------- */

  /**
   * Segments are specialized versions of hash tables.  This subclass inherits
   * from ReentrantLock opportunistically, just to simplify some locking and
   * avoid separate construction.
   */
  @SuppressWarnings("serial") // This class is never serialized.
  class Segment extends ReentrantLock {

    /*
     * TODO(user): Consider copying variables (like evictsBySize) from outer
     * class into this class. It will require more memory but will reduce
     * indirection.
     */

    /*
     * Segments maintain a table of entry lists that are ALWAYS
     * kept in a consistent state, so can be read without locking.
     * Next fields of nodes are immutable (final).  All list
     * additions are performed at the front of each bin. This
     * makes it easy to check changes, and also fast to traverse.
     * When nodes would otherwise be changed, new nodes are
     * created to replace them. This works well for hash tables
     * since the bin lists tend to be short. (The average length
     * is less than two.)
     *
     * Read operations can thus proceed without locking, but rely
     * on selected uses of volatiles to ensure that completed
     * write operations performed by other threads are
     * noticed. For most purposes, the "count" field, tracking the
     * number of elements, serves as that volatile variable
     * ensuring visibility.  This is convenient because this field
     * needs to be read in many read operations anyway:
     *
     *   - All (unsynchronized) read operations must first read the
     *     "count" field, and should not look at table entries if
     *     it is 0.
     *
     *   - All (synchronized) write operations should write to
     *     the "count" field after structurally changing any bin.
     *     The operations must not take any action that could even
     *     momentarily cause a concurrent read operation to see
     *     inconsistent data. This is made easier by the nature of
     *     the read operations in Map. For example, no operation
     *     can reveal that the table has grown but the threshold
     *     has not yet been updated, so there are no atomicity
     *     requirements for this with respect to reads.
     *
     * As a guide, all critical volatile reads and writes to the
     * count field are marked in code comments.
     */

    /**
     * The number of live elements in this segment's region. This does not
     * include unset elements which are awaiting cleanup.
     */
    volatile int count;

    /**
     * Number of updates that alter the size of the table. This is used
     * during bulk-read methods to make sure they see a consistent snapshot:
     * If modCounts change during a traversal of segments computing size or
     * checking containsValue, then we might have an inconsistent view of
     * state so (usually) must retry.
     */
    int modCount;

    /**
     * The table is expanded when its size exceeds this threshold. (The
     * value of this field is always {@code (int)(capacity * 0.75)}.)
     */
    int threshold;

    /**
     * The per-segment table.
     */
    volatile AtomicReferenceArray<ReferenceEntry<K, V>> table;

    /**
     * The maximum size of this map. MapMaker.UNSET_INT if there is no
     * maximum.
     */
    final int maxSegmentSize;

    /**
     * The cleanup queue is used to record entries which have been unset
     * and need to be removed from the map. It is drained by the cleanup
     * executor.
     */
    final Queue<ReferenceEntry<K, V>> cleanupQueue =
        new ConcurrentLinkedQueue<ReferenceEntry<K, V>>();

    /**
     * The recency queue is used to record which entries were accessed
     * for updating the eviction list's ordering. It is drained
     * as a batch operation when either the DRAIN_THRESHOLD is crossed or
     * a write occurs on the segment.
     */
    final Queue<ReferenceEntry<K, V>> recencyQueue;

    /**
     * A counter of the number of reads since the last write, used to drain
     * queues on a small fraction of read operations.
     */
    final AtomicInteger readCount = new AtomicInteger();

    /**
     * A queue of elements currently in the map, ordered by access time.
     * Elements are added to the tail of the queue on access/write.
     */
    @GuardedBy("Segment.this")
    final Queue<ReferenceEntry<K, V>> evictionQueue;

    /**
     * A queue of elements currently in the map, ordered by expiration time
     * (either access or write time). Elements are added to the tail of the
     * queue on access/write.
     */
    @GuardedBy("Segment.this")
    final Queue<ReferenceEntry<K, V>> expirationQueue;

    Segment(int initialCapacity, int maxSegmentSize) {
      this.maxSegmentSize = maxSegmentSize;
      initTable(newEntryArray(initialCapacity));

      recencyQueue = (evictsBySize() || expiresAfterAccess())
          ? new ConcurrentLinkedQueue<ReferenceEntry<K, V>>()
          : CustomConcurrentHashMap.<ReferenceEntry<K, V>>discardingQueue();

      evictionQueue = evictsBySize()
          ? new EvictionQueue()
          : CustomConcurrentHashMap.<ReferenceEntry<K, V>>discardingQueue();

      expirationQueue = expires()
          ? new ExpirationQueue()
          : CustomConcurrentHashMap.<ReferenceEntry<K, V>>discardingQueue();
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

    /**
     * Sets a new value of an entry. Adds newly created entries at the end
     * of the expiration queue.
     */
    @GuardedBy("Segment.this")
    void setValue(ReferenceEntry<K, V> entry, V value) {
      recordWrite(entry);
      ValueReference<K, V> valueReference = newValueReference(entry, value);
      entry.setValueReference(valueReference);
    }

    // recency queue, shared by expiration and eviction

    /**
     * Records the relative order in which this read was performed by adding
     * {@code entry} to the recency queue. At write-time, or when the queue is
     * full past the threshold, the queue will be drained and the entries
     * therein processed.
     */
    void recordRead(ReferenceEntry<K, V> entry) {
      if (expiresAfterAccess()) {
        recordExpirationTime(entry, expireAfterAccessNanos);
      }
      recencyQueue.add(entry);
    }

    /**
     * Updates eviction metadata that {@code entry} was just written. This
     * currently amounts to adding {@code entry} to relevant eviction lists.
     */
    @GuardedBy("Segment.this")
    void recordWrite(ReferenceEntry<K, V> entry) {
      // we are already under lock, so drain the recency queue immediately
      drainRecencyQueue();
      evictionQueue.add(entry);
      if (expires()) {
        // currently MapMaker ensures that expireAfterWrite and
        // expireAfterAccess are mutually exclusive
        long expiration = expiresAfterAccess()
            ? expireAfterAccessNanos : expireAfterWriteNanos;
        recordExpirationTime(entry, expiration);
        expirationQueue.add(entry);
      }
    }

    /**
     * Drains the recency queue, updating eviction metadata that the entries
     * therein were read in the specified relative order. This currently amounts
     * to adding them to relevant eviction lists (accounting for the fact that
     * they could have been removed from the map since being added to the
     * recency queue).
     */
    @GuardedBy("Segment.this")
    void drainRecencyQueue() {
      ReferenceEntry<K, V> e;
      while ((e = recencyQueue.poll()) != null) {
        // An entry may be in the recency queue despite it being removed from
        // the map . This can occur when the entry was concurrently read while a
        // writer is removing it from the segment or after a clear has removed
        // all of the segment's entries.
        if (evictionQueue.contains(e)) {
          evictionQueue.add(e);
        }
        if (expiresAfterAccess() && expirationQueue.contains(e)) {
          expirationQueue.add(e);
        }
      }
    }

    // expiration

    void recordExpirationTime(ReferenceEntry<K, V> entry,
        long expirationNanos) {
      // might overflow, but that's okay (see isExpired())
      entry.setExpirationTime(ticker.read() + expirationNanos);
    }

    @GuardedBy("Segment.this")
    void expireEntries() {
      drainRecencyQueue();

      if (expirationQueue.isEmpty()) {
        // There's no point in calling nanoTime() if we have no entries to
        // expire.
        return;
      }
      long now = ticker.read();
      ReferenceEntry<K, V> e;
      while ((e = expirationQueue.peek()) != null && isExpired(e, now)) {
        if (!unsetEntry(e, e.getHash())) {
          throw new AssertionError();
        }
      }
    }

    // eviction

    /**
     * Performs eviction if the segment is full. This should only be called
     * prior to adding a new entry and increasing {@code count}.
     *
     * @return true if eviction occurred
     */
    @GuardedBy("Segment.this")
    boolean evictEntries() {
      if (evictsBySize() && count >= maxSegmentSize) {
        drainRecencyQueue();

        ReferenceEntry<K, V> e = evictionQueue.remove();
        if (!unsetEntry(e, e.getHash())) {
          throw new AssertionError();
        }
        return true;
      }
      return false;
    }

    /**
     * Returns first entry of bin for given hash.
     */
    ReferenceEntry<K, V> getFirst(int hash) {
      // read this volatile field only once
      AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
      return table.get(hash & (table.length() - 1));
    }

    /* Specialized implementations of map methods */

    /**
     * Returns the entry for a given key. Note that the entry may not be live.
     * This is only used for testing.
     */
    @VisibleForTesting ReferenceEntry<K, V> getEntry(Object key, int hash) {
      for (ReferenceEntry<K, V> e = getFirst(hash); e != null;
          e = e.getNext()) {
        if (e.getHash() != hash) {
          continue;
        }

        K entryKey = e.getKey();
        if (entryKey == null) {
          continue;
        }

        if (keyEquivalence.equivalent(key, entryKey)) {
          return e;
        }
      }

      return null;
    }

    V get(Object key, int hash) {
      try {
        if (count != 0) { // read-volatile
          for (ReferenceEntry<K, V> e = getFirst(hash); e != null;
               e = e.getNext()) {
            if (e.getHash() != hash) {
              continue;
            }

            K entryKey = e.getKey();
            if (entryKey == null) {
              continue;
            }

            if (keyEquivalence.equivalent(key, entryKey)) {
              V value = getLiveValue(e);
              if (value != null) {
                recordRead(e);
              }
              return value;
            }
          }
        }

        return null;
      } finally {
        postReadCleanup();
      }
    }

    boolean containsKey(Object key, int hash) {
      if (count != 0) { // read-volatile
        for (ReferenceEntry<K, V> e = getFirst(hash); e != null;
            e = e.getNext()) {
          if (e.getHash() != hash) {
            continue;
          }

          K entryKey = e.getKey();
          if (entryKey == null) {
            continue;
          }

          if (keyEquivalence.equivalent(key, entryKey)) {
            return getLiveValue(e) != null;
          }
        }
      }

      return false;
    }

    boolean containsValue(Object value) {
      if (count != 0) { // read-volatile
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int length = table.length();
        for (int i = 0; i < length; ++i) {
          for (ReferenceEntry<K, V> e = table.get(i); e != null;
              e = e.getNext()) {
            V entryValue = getLiveValue(e);
            if (entryValue == null) {
              continue;
            }
            if (valueEquivalence.equivalent(value, entryValue)) {
              return true;
            }
          }
        }
      }

      return false;
    }

    boolean replace(K key, int hash, V oldValue, V newValue) {
      checkNotNull(oldValue);
      checkNotNull(newValue);
      lock();
      try {
        preWriteCleanup();

        for (ReferenceEntry<K, V> e = getFirst(hash); e != null;
            e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash && entryKey != null
              && keyEquivalence.equivalent(key, entryKey)) {
            // If the value disappeared, this entry is partially collected,
            // and we should pretend like it doesn't exist.
            V entryValue = e.getValueReference().get();
            if (entryValue == null) {
              unsetLiveEntry(e, hash);
              return false;
            }

            if (valueEquivalence.equivalent(oldValue, entryValue)) {
              setValue(e, newValue);
              return true;
            } else {
              // Mimic
              // "if (map.containsKey(key) && map.get(key).equals(oldValue))..."
              recordRead(e);
              return false;
            }
          }
        }

        return false;
      } finally {
        unlock();
        postWriteCleanup();
      }
    }

    V replace(K key, int hash, V newValue) {
      checkNotNull(newValue);
      lock();
      try {
        preWriteCleanup();

        for (ReferenceEntry<K, V> e = getFirst(hash); e != null;
            e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash && entryKey != null
              && keyEquivalence.equivalent(key, entryKey)) {
            // If the value disappeared, this entry is partially collected,
            // and we should pretend like it doesn't exist.
            V entryValue = e.getValueReference().get();
            if (entryValue == null) {
              unsetLiveEntry(e, hash);
              return null;
            }

            setValue(e, newValue);
            return entryValue;
          }
        }

        return null;
      } finally {
        unlock();
        postWriteCleanup();
      }
    }

    V put(K key, int hash, V value, boolean onlyIfAbsent) {
      checkNotNull(value);
      lock();
      try {
        preWriteCleanup();

        int newCount = this.count + 1;
        if (newCount > this.threshold) { // ensure capacity
          expand();
          newCount = this.count + 1;
        }

        // getFirst, but remember the index
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        // Look for an existing entry.
        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash && entryKey != null
              && keyEquivalence.equivalent(key, entryKey)) {
            // We found an existing entry.

            ValueReference<K, V> valueReference = e.getValueReference();
            V entryValue = valueReference.get();

            if (entryValue == null) {
              ++modCount;
              // Value could be partially-collected, unset, or computing.
              // In the first case, the value must be reclaimed. In the latter
              // two cases en entry must be evicted. This entry could be both
              // partially-collected and next on the eviction list, which is why
              // notifyValueReclaimed must be called prior to evictEntries.
              valueReference.notifyValueReclaimed();
              evictEntries();
              newCount = this.count + 1;
              this.count = newCount; // write-volatile
            } else if (onlyIfAbsent) {
              // Mimic
              // "if (!map.containsKey(key)) ...
              //  else return map.get(key);
              recordRead(e);
              return entryValue;
            }
            // else clobber, don't adjust count

            setValue(e, value);
            return entryValue;
          }
        }

        if (evictEntries()) {
          newCount = this.count + 1;
          first = table.get(index);
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
        postWriteCleanup();
      }
    }

    /**
     * Expands the table if possible.
     */
    @GuardedBy("Segment.this")
    void expand() {
      AtomicReferenceArray<ReferenceEntry<K, V>> oldTable = table;
      int oldCapacity = oldTable.length();
      if (oldCapacity >= MAXIMUM_CAPACITY) {
        return;
      }

      /*
       * Reclassify nodes in each list to new Map.  Because we are
       * using power-of-two expansion, the elements from each bin
       * must either stay at same index, or move with a power of two
       * offset. We eliminate unnecessary node creation by catching
       * cases where old nodes can be reused because their next
       * fields won't change. Statistically, at the default
       * threshold, only about one-sixth of them need cloning when
       * a table doubles. The nodes they replace will be garbage
       * collectable as soon as they are no longer referenced by any
       * reader thread that may be in the midst of traversing table
       * right now.
       */

      AtomicReferenceArray<ReferenceEntry<K, V>> newTable
          = newEntryArray(oldCapacity << 1);
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
              if (isCollected(e)) {
                unsetLiveEntry(e, e.getHash()); // decrements count
              } else {
                int newIndex = e.getHash() & newMask;
                ReferenceEntry<K, V> newNext = newTable.get(newIndex);
                newTable.set(newIndex, copyEntry(e, newNext));
              }
            }
          }
        }
      }
      table = newTable;
    }

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
          if (e.getHash() == hash && entryKey != null
              && keyEquivalence.equivalent(key, entryKey)) {
            V entryValue = e.getValueReference().get();
            if (entryValue == null) {
              unsetLiveEntry(e, hash);
            } else {
              ++modCount;
              ReferenceEntry<K, V> newFirst =
                  removeFromTable(first, e); // could decrement count
              newCount = this.count - 1;
              table.set(index, newFirst);
              this.count = newCount; // write-volatile
            }
            return entryValue;
          }
        }

        return null;
      } finally {
        unlock();
        postWriteCleanup();
      }
    }

    boolean remove(Object key, int hash, Object value) {
      checkNotNull(value);
      lock();
      try {
        preWriteCleanup();

        int newCount = this.count - 1;
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash && entryKey != null
              && keyEquivalence.equivalent(key, entryKey)) {
            V entryValue = e.getValueReference().get();
            if (entryValue == null) {
              unsetLiveEntry(e, hash);
            } else if (valueEquivalence.equivalent(value, entryValue)) {
              ++modCount;
              ReferenceEntry<K, V> newFirst =
                  removeFromTable(first, e); // could decrement count
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
        postWriteCleanup();
      }
    }

    // Note, this method should only be called when it is impossible to reuse
    // an entry. Currently this only occurs when its key is garbage collected.
    boolean removeEntry(ReferenceEntry<K, V> entry, int hash) {
      /*
       * This is used during expiration, computation and reclamation, so
       * we don't want to recursively expire entries.
       */
      lock();
      try {
        int newCount = count - 1;
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          if (e == entry) {
            ++modCount;
            ReferenceEntry<K, V> newFirst =
                removeFromTable(first, e); // could decrement count
            newCount = this.count - 1;
            table.set(index, newFirst);
            count = newCount; // write-volatile
            return true;
          }
        }

        return false;
      } finally {
        unlock();
      }
    }

    /**
     * Removes an entry from within a table. All entries following the removed
     * node can stay, but all preceding ones need to be cloned.
     *
     * @param first the first entry of the table
     * @param removed the entry being removed from the table
     * @return the new first entry for the table
     */
    @GuardedBy("Segment.this")
    private ReferenceEntry<K, V> removeFromTable(ReferenceEntry<K, V> first,
        ReferenceEntry<K, V> entry) {
      evictionQueue.remove(entry);
      expirationQueue.remove(entry);

      ReferenceEntry<K, V> newFirst = entry.getNext();
      for (ReferenceEntry<K, V> e = first; e != entry; e = e.getNext()) {
        if (isCollected(e)) {
          unsetLiveEntry(e, e.getHash()); // decrements count
        } else {
          newFirst = copyEntry(e, newFirst);
        }
      }
      return newFirst;
    }

    @GuardedBy("Segment.this")
    boolean unsetEntry(ReferenceEntry<K, V> entry, int hash) {
      for (ReferenceEntry<K, V> e = getFirst(hash); e != null;
          e = e.getNext()) {
        if (e == entry) {
          return unsetLiveEntry(entry, hash);
        }
      }

      return false;
    }

    @GuardedBy("Segment.this")
    boolean unsetLiveEntry(ReferenceEntry<K, V> entry, int hash) {
      if (isUnset(entry)) {
        // keep count consistent
        return false;
      }

      int newCount = this.count - 1;
      ++modCount;
      ValueReference<K, V> valueReference = entry.getValueReference();
      if (valueReference.isComputingReference()) {
        return false;
      }

      K key = entry.getKey();
      enqueueNotification(key, hash, valueReference);
      enqueueCleanup(entry);
      this.count = newCount; // write-volatile
      return true;
    }

    boolean unsetValue(K key, int hash,
        ValueReference<K, V> valueReference) {
      lock();
      try {
        int newCount = this.count - 1;
        for (ReferenceEntry<K, V> e = getFirst(hash); e != null;
            e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash && entryKey != null
              && keyEquivalence.equivalent(key, entryKey)) {
            ValueReference<K, V> v = e.getValueReference();
            if (v == valueReference) {
              ++modCount;
              enqueueNotification(key, hash, valueReference);
              enqueueCleanup(e);
              this.count = newCount;
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

    boolean clearValue(K key, int hash, ValueReference<K, V> valueReference) {
      lock();
      try {
        for (ReferenceEntry<K, V> e = getFirst(hash); e != null;
            e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash && entryKey != null
              && keyEquivalence.equivalent(key, entryKey)) {
            ValueReference<K, V> v = e.getValueReference();
            if (v == valueReference) {
              enqueueCleanup(e);
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

    @GuardedBy("Segment.this")
    void enqueueCleanup(ReferenceEntry<K, V> entry) {
      ValueReference<K, V> unset = unset();
      entry.setValueReference(unset);
      cleanupQueue.offer(entry);
      evictionQueue.remove(entry);
      expirationQueue.remove(entry);
    }

    @GuardedBy("Segment.this")
    void processPendingCleanup() {
      AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
      ReferenceEntry<K, V> entry;
      int cleanedUp = 0;
      while (cleanedUp < CLEANUP_MAX && (entry = cleanupQueue.poll()) != null) {
        int index = entry.getHash() & (table.length() - 1);

        ReferenceEntry<K, V> first = table.get(index);
        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          if (e == entry) {
            if (isUnset(e)) {
              ReferenceEntry<K, V> newFirst = removeFromTable(first, e);
              table.set(index, newFirst);
              cleanedUp++;
            }
            break;
          }
        }
      }
    }

    void postReadCleanup() {
      // we are not under lock, so only drain a small fraction of the time
      if ((readCount.incrementAndGet() & DRAIN_THRESHOLD) == 0) {
        if (isInlineCleanup()) {
          // inline cleanup normally avoids taking the lock, but since no
          // writes are happening we need to force some locked cleanup
          runCleanup();
        } else if (!isHeldByCurrentThread()) {
          cleanupExecutor.execute(cleanupRunnable);
        }
      }
    }

    /**
     * Performs routine cleanup prior to executing a write. This should be
     * called every time a write thread acquires the segment lock, immediately
     * after acquiring the lock.
     */
    @GuardedBy("Segment.this")
    void preWriteCleanup() {
      if (isInlineCleanup()) {
        runLockedCleanup();
      } else {
        expireEntries();
      }
    }

    void postWriteCleanup() {
      if (isInlineCleanup()) {
        // this cleanup pattern is optimized for writes, where cleanup requiring
        // the lock is performed when the lock is acquired, and cleanup not
        // requiring the lock is performed when the lock is released
        if (isHeldByCurrentThread()) {
          runLockedCleanup();
        } else {
          runUnlockedCleanup();
        }
      } else if (!isHeldByCurrentThread()) {
        // non-default cleanup executors can ignore cleanup optimizations when
        // the lock is held, as cleanup will always be called when the lock is
        // released
        cleanupExecutor.execute(cleanupRunnable);
      }
    }

    final Runnable cleanupRunnable =
        new Runnable() {
          @Override
          public void run() {
            runCleanup();
          }
        };

    void runCleanup() {
      runLockedCleanup();
      // locked cleanup may generate notifications we can send unlocked
      runUnlockedCleanup();
    }

    /**
     * Performs housekeeping tasks on this segment that don't require the
     * segment lock.
     */
    void runUnlockedCleanup() {
      processPendingNotifications();
    }

    /**
     * Performs housekeeping tasks on this segment that require the segment
     * lock.
     */
    void runLockedCleanup() {
      lock();
      try {
        expireEntries(); // calls drainRecencyQueue
        processPendingCleanup();
        readCount.set(0);
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
          evictionQueue.clear();
          expirationQueue.clear();
          readCount.set(0);

          ++modCount;
          count = 0; // write-volatile
        } finally {
          unlock();
        }
      }
    }

    // Queues

    /**
     * A custom queue for managing eviction order. Note that this is tightly
     * integrated with {@code ReferenceEntry}, upon which it relies to perform
     * its linking.
     *
     * <p>Note that this entire implementation makes the assumption that all
     * elements which are in the map are also in this queue, and that all
     * elements not in the queue are not in the map.
     *
     * <p>The benefits of creating our own queue are that (1) we can
     * replace elements in the middle of the queue as part of
     * copyEvictableEntry, and (2) the contains method is highly optimized for
     * the current model.
     */
    @VisibleForTesting class EvictionQueue
        extends AbstractQueue<ReferenceEntry<K, V>> {
      // TODO(user): create UnsupportedOperationException throwing base class
      @VisibleForTesting final ReferenceEntry<K, V> head =
          new ReferenceEntry<K, V>() {

        ReferenceEntry<K, V> nextEvictable = this;
        @Override
        public ReferenceEntry<K, V> getNextEvictable() {
          return nextEvictable;
        }
        @Override
        public void setNextEvictable(ReferenceEntry<K, V> next) {
          this.nextEvictable = next;
        }

        ReferenceEntry<K, V> previousEvictable = this;
        @Override
        public ReferenceEntry<K, V> getPreviousEvictable() {
          return previousEvictable;
        }
        @Override
        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
          this.previousEvictable = previous;
        }

        @Override
        public ValueReference<K, V> getValueReference() {
          throw new UnsupportedOperationException();
        }
        @Override
        public void setValueReference(ValueReference<K, V> valueReference) {
          throw new UnsupportedOperationException();
        }
        @Override
        public void valueReclaimed(ValueReference<K, V> valueReference) {
          throw new UnsupportedOperationException();
        }
        @Override
        public ReferenceEntry<K, V> getNext() {
          throw new UnsupportedOperationException();
        }
        @Override
        public int getHash() {
          throw new UnsupportedOperationException();
        }
        @Override
        public K getKey() {
          throw new UnsupportedOperationException();
        }

        // null expiration
        @Override
        public long getExpirationTime() {
          throw new UnsupportedOperationException();
        }
        @Override
        public void setExpirationTime(long time) {
          throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getNextExpirable() {
          throw new UnsupportedOperationException();
        }
        @Override
        public void setNextExpirable(ReferenceEntry<K, V> next) {
          throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getPreviousExpirable() {
          throw new UnsupportedOperationException();
        }
        @Override
        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
          throw new UnsupportedOperationException();
        }
      };

      // implements Queue

      @Override
      public boolean offer(ReferenceEntry<K, V> entry) {
        // unlink
        connectEvictables(entry.getPreviousEvictable(),
            entry.getNextEvictable());

        // add to tail
        connectEvictables(head.getPreviousEvictable(), entry);
        connectEvictables(entry, head);

        return true;
      }

      @Override
      public ReferenceEntry<K, V> peek() {
        ReferenceEntry<K, V> next = head.getNextEvictable();
        return (next == head) ? null : next;
      }

      @Override
      public ReferenceEntry<K, V> poll() {
        ReferenceEntry<K, V> next = head.getNextEvictable();
        if (next == head) {
          return null;
        }

        remove(next);
        return next;
      }

      @Override
      @SuppressWarnings("unchecked")
      public boolean remove(Object o) {
        ReferenceEntry<K, V> e = (ReferenceEntry) o;
        ReferenceEntry<K, V> previous = e.getPreviousEvictable();
        ReferenceEntry<K, V> next = e.getNextEvictable();
        connectEvictables(previous, next);
        nullifyEvictable(e);

        return next != NullEntry.INSTANCE;
      }

      @Override
      @SuppressWarnings("unchecked")
      public boolean contains(Object o) {
        ReferenceEntry<K, V> e = (ReferenceEntry) o;
        return e.getNextEvictable() != NullEntry.INSTANCE;
      }

      @Override
      public boolean isEmpty() {
        return head.getNextEvictable() == head;
      }

      @Override
      public int size() {
        int size = 0;
        for (ReferenceEntry<K, V> e = head.getNextEvictable(); e != head;
             e = e.getNextEvictable()) {
          size++;
        }
        return size;
      }

      @Override
      public void clear() {
        ReferenceEntry<K, V> e = head.getNextEvictable();
        while (e != head) {
          ReferenceEntry<K, V> next = e.getNextEvictable();
          nullifyEvictable(e);
          e = next;
        }

        head.setNextEvictable(head);
        head.setPreviousEvictable(head);
      }

      @Override
      public Iterator<ReferenceEntry<K, V>> iterator() {
        return new AbstractLinkedIterator<ReferenceEntry<K, V>>(peek()) {
          @Override
          protected ReferenceEntry<K, V> computeNext(
              ReferenceEntry<K, V> previous) {
            ReferenceEntry<K, V> next = previous.getNextEvictable();
            return (next == head) ? null : next;
          }
        };
      }
    }

    /**
     * A custom queue for managing expiration order. Note that this is tightly
     * integrated with {@code ReferenceEntry}, upon which it reliese to perform
     * its linking.
     *
     * <p>Note that this entire implementation makes the assumption that all
     * elements which are in the map are also in this queue, and that all
     * elements not in the queue are not in the map.
     *
     * <p>The benefits of creating our own queue are that (1) we can
     * replace elements in the middle of the queue as part of
     * copyEvictableEntry, and (2) the contains method is highly optimized for
     * the current model.
     */
    @VisibleForTesting class ExpirationQueue
        extends AbstractQueue<ReferenceEntry<K, V>> {
      // TODO(user): create UnsupportedOperationException throwing base class
      @VisibleForTesting final ReferenceEntry<K, V> head =
          new ReferenceEntry<K, V>() {

        @Override
        public long getExpirationTime() {
          return Long.MAX_VALUE;
        }
        @Override
        public void setExpirationTime(long time) {}

        ReferenceEntry<K, V> nextExpirable = this;
        @Override
        public ReferenceEntry<K, V> getNextExpirable() {
          return nextExpirable;
        }
        @Override
        public void setNextExpirable(ReferenceEntry<K, V> next) {
          this.nextExpirable = next;
        }

        ReferenceEntry<K, V> previousExpirable = this;
        @Override
        public ReferenceEntry<K, V> getPreviousExpirable() {
          return previousExpirable;
        }
        @Override
        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
          this.previousExpirable = previous;
        }

        @Override
        public ValueReference<K, V> getValueReference() {
          throw new UnsupportedOperationException();
        }
        @Override
        public void setValueReference(ValueReference<K, V> valueReference) {
          throw new UnsupportedOperationException();
        }
        @Override
        public void valueReclaimed(ValueReference<K, V> valueReference) {
          throw new UnsupportedOperationException();
        }
        @Override
        public ReferenceEntry<K, V> getNext() {
          throw new UnsupportedOperationException();
        }
        @Override
        public int getHash() {
          throw new UnsupportedOperationException();
        }
        @Override
        public K getKey() {
          throw new UnsupportedOperationException();
        }

        // null eviction
        @Override
        public ReferenceEntry<K, V> getNextEvictable() {
          throw new UnsupportedOperationException();
        }
        @Override
        public void setNextEvictable(ReferenceEntry<K, V> next) {
          throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceEntry<K, V> getPreviousEvictable() {
          throw new UnsupportedOperationException();
        }
        @Override
        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
          throw new UnsupportedOperationException();
        }
      };

      // implements Queue

      @Override
      public boolean offer(ReferenceEntry<K, V> entry) {
        // unlink
        connectExpirables(entry.getPreviousExpirable(),
            entry.getNextExpirable());

        // add to tail
        connectExpirables(head.getPreviousExpirable(), entry);
        connectExpirables(entry, head);

        return true;
      }

      @Override
      public ReferenceEntry<K, V> peek() {
        ReferenceEntry<K, V> next = head.getNextExpirable();
        return (next == head) ? null : next;
      }

      @Override
      public ReferenceEntry<K, V> poll() {
        ReferenceEntry<K, V> next = head.getNextExpirable();
        if (next == head) {
          return null;
        }

        remove(next);
        return next;
      }

      @Override
      @SuppressWarnings("unchecked")
      public boolean remove(Object o) {
        ReferenceEntry<K, V> e = (ReferenceEntry) o;
        ReferenceEntry<K, V> previous = e.getPreviousExpirable();
        ReferenceEntry<K, V> next = e.getNextExpirable();
        connectExpirables(previous, next);
        nullifyExpirable(e);

        return next != NullEntry.INSTANCE;
      }

      @Override
      @SuppressWarnings("unchecked")
      public boolean contains(Object o) {
        ReferenceEntry<K, V> e = (ReferenceEntry) o;
        return e.getNextExpirable() != NullEntry.INSTANCE;
      }

      @Override
      public boolean isEmpty() {
        return head.getNextExpirable() == head;
      }

      @Override
      public int size() {
        int size = 0;
        for (ReferenceEntry<K, V> e = head.getNextExpirable(); e != head;
             e = e.getNextExpirable()) {
          size++;
        }
        return size;
      }

      @Override
      public void clear() {
        ReferenceEntry<K, V> e = head.getNextExpirable();
        while (e != head) {
          ReferenceEntry<K, V> next = e.getNextExpirable();
          nullifyExpirable(e);
          e = next;
        }

        head.setNextExpirable(head);
        head.setPreviousExpirable(head);
      }

      @Override
      public Iterator<ReferenceEntry<K, V>> iterator() {
        return new AbstractLinkedIterator<ReferenceEntry<K, V>>(peek()) {
          @Override
          protected ReferenceEntry<K, V> computeNext(
              ReferenceEntry<K, V> previous) {
            ReferenceEntry<K, V> next = previous.getNextExpirable();
            return (next == head) ? null : next;
          }
        };
      }
    }
  }

  /* ---------------- Public operations -------------- */

  @Override public boolean isEmpty() {
    Segment[] segments = this.segments;
    /*
     * We keep track of per-segment modCounts to avoid ABA
     * problems in which an element in one segment was added and
     * in another removed during traversal, in which case the
     * table was never actually empty at any point. Note the
     * similar use of modCounts in the size() and containsValue()
     * methods, which are the only other methods also susceptible
     * to ABA problems.
     */
    int[] mc = new int[segments.length];
    int mcsum = 0;
    for (int i = 0; i < segments.length; ++i) {
      if (segments[i].count != 0) {
        return false;
      }
      mcsum += mc[i] = segments[i].modCount;
    }

    // If mcsum happens to be zero, then we know we got a snapshot
    // before any modifications at all were made.  This is
    // probably common enough to bother tracking.
    if (mcsum != 0) {
      for (int i = 0; i < segments.length; ++i) {
        if (segments[i].count != 0 ||
            mc[i] != segments[i].modCount) {
          return false;
        }
      }
    }
    return true;
  }

  @Override public int size() {
    Segment[] segments = this.segments;
    long sum = 0;
    for (int i = 0; i < segments.length; ++i) {
      sum += segments[i].count;
    }
    return Ints.saturatedCast(sum);
  }

  @Override public V get(Object key) {
    int hash = hash(key);
    return segmentFor(hash).get(key, hash);
  }

  /**
   * Returns the entry for a given key. Note that the entry may not be live.
   * This is only used for testing.
   */
  @VisibleForTesting ReferenceEntry<K, V> getEntry(Object key) {
    int hash = hash(key);
    return segmentFor(hash).getEntry(key, hash);
  }

  @Override public boolean containsKey(Object key) {
    int hash = hash(key);
    return segmentFor(hash).containsKey(key, hash);
  }

  @Override public boolean containsValue(Object value) {
    // TODO(kevinb): document why we choose to throw over returning false?
    checkNotNull(value);

    Segment[] segments = this.segments;
    for (int i = 0; i < segments.length; ++i) {
      // ensure visibility of most recent completed write
      @SuppressWarnings({"UnusedDeclaration", "unused"})
      int c = segments[i].count; // read-volatile
      if (segments[i].containsValue(value)) {
        return true;
      }
    }
    return false;
  }

  @Override public V put(K key, V value) {
    int hash = hash(key);
    return segmentFor(hash).put(key, hash, value, false);
  }

  public V putIfAbsent(K key, V value) {
    int hash = hash(key);
    return segmentFor(hash).put(key, hash, value, true);
  }

  @Override public void putAll(Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  @Override public V remove(Object key) {
    int hash = hash(key);
    return segmentFor(hash).remove(key, hash);
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException if the specified key is null
   */
  public boolean remove(Object key, Object value) {
    int hash = hash(key);
    return segmentFor(hash).remove(key, hash, value);
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException if any of the arguments are null
   */
  public boolean replace(K key, V oldValue, V newValue) {
    int hash = hash(key);
    return segmentFor(hash).replace(key, hash, oldValue, newValue);
  }

  /**
   * {@inheritDoc}
   *
   * @return the previous value associated with the specified key, or
   *         {@code null} if there was no mapping for the key
   * @throws NullPointerException if the specified key or value is null
   */
  public V replace(K key, V value) {
    int hash = hash(key);
    return segmentFor(hash).replace(key, hash, value);
  }

  @Override public void clear() {
    for (Segment segment : segments) {
      segment.clear();
    }
  }

  Set<K> keySet;

  @Override public Set<K> keySet() {
    Set<K> ks = keySet;
    return (ks != null) ? ks : (keySet = new KeySet());
  }

  Collection<V> values;

  @Override public Collection<V> values() {
    Collection<V> vs = values;
    return (vs != null) ? vs : (values = new Values());
  }

  Set<Entry<K, V>> entrySet;

  @Override public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> es = entrySet;
    return (es != null) ? es : (entrySet = new EntrySet());
  }

  /* ---------------- Iterator Support -------------- */

  abstract class HashIterator {

    int nextSegmentIndex;
    int nextTableIndex;
    AtomicReferenceArray<ReferenceEntry<K, V>> currentTable;
    ReferenceEntry<K, V> nextEntry;
    WriteThroughEntry nextExternal;
    WriteThroughEntry lastReturned;

    HashIterator() {
      nextSegmentIndex = segments.length - 1;
      nextTableIndex = -1;
      advance();
    }

    final void advance() {
      nextExternal = null;

      if (nextInChain()) {
        return;
      }

      if (nextInTable()) {
        return;
      }

      while (nextSegmentIndex >= 0) {
        Segment seg = segments[nextSegmentIndex--];
        if (seg.count != 0) {
          currentTable = seg.table;
          nextTableIndex = currentTable.length() - 1;
          if (nextInTable()) {
            return;
          }
        }
      }
    }

    /**
     * Finds the next entry in the current chain. Returns true if an entry
     * was found.
     */
    boolean nextInChain() {
      if (nextEntry != null) {
        for (nextEntry = nextEntry.getNext(); nextEntry != null;
            nextEntry = nextEntry.getNext()) {
          if (advanceTo(nextEntry)) {
            return true;
          }
        }
      }
      return false;
    }

    /**
     * Finds the next entry in the current table. Returns true if an entry
     * was found.
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
     * Advances to the given entry. Returns true if the entry was valid,
     * false if it should be skipped.
     */
    boolean advanceTo(ReferenceEntry<K, V> entry) {
      K key = entry.getKey();
      V value = getLiveValue(entry);
      if (key != null && value != null) {
        nextExternal = new WriteThroughEntry(key, value);
        return true;
      } else {
        // Skip partially reclaimed entry.
        return false;
      }
    }

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

    public void remove() {
      checkState(lastReturned != null);
      CustomConcurrentHashMap.this.remove(lastReturned.getKey());
      lastReturned = null;
    }
  }

  final class KeyIterator extends HashIterator implements Iterator<K> {

    public K next() {
      return nextEntry().getKey();
    }
  }

  final class ValueIterator extends HashIterator implements Iterator<V> {

    public V next() {
      return nextEntry().getValue();
    }
  }

  /**
   * Custom Entry class used by EntryIterator.next(), that relays setValue
   * changes to the underlying map.
   */
  final class WriteThroughEntry extends AbstractMapEntry<K, V> {
    final K key; // non-null
    V value; // non-null

    WriteThroughEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override public K getKey() {
      return key;
    }

    @Override public V getValue() {
      return value;
    }

    @Override public boolean equals(@Nullable Object object) {
      // Cannot use key and value equivalence
      if (object instanceof Entry) {
        Entry<?, ?> that = (Entry<?, ?>) object;
        return key.equals(that.getKey())
            && value.equals(that.getValue());
      }
      return false;
    }

    @Override public int hashCode() {
      // Cannot use key and value equivalence
      return key.hashCode() ^ value.hashCode();
    }

    @Override public V setValue(V newValue) {
      V oldValue = put(key, newValue);
      value = newValue; // only if put succeeds
      return oldValue;
    }
  }

  final class EntryIterator extends HashIterator
      implements Iterator<Entry<K, V>> {

    public Entry<K, V> next() {
      return nextEntry();
    }
  }

  final class KeySet extends AbstractSet<K> {

    @Override public Iterator<K> iterator() {
      return new KeyIterator();
    }

    @Override public int size() {
      return CustomConcurrentHashMap.this.size();
    }

    @Override public boolean isEmpty() {
      return CustomConcurrentHashMap.this.isEmpty();
    }

    @Override public boolean contains(Object o) {
      return CustomConcurrentHashMap.this.containsKey(o);
    }

    @Override public boolean remove(Object o) {
      return CustomConcurrentHashMap.this.remove(o) != null;
    }

    @Override public void clear() {
      CustomConcurrentHashMap.this.clear();
    }
  }

  final class Values extends AbstractCollection<V> {

    @Override public Iterator<V> iterator() {
      return new ValueIterator();
    }

    @Override public int size() {
      return CustomConcurrentHashMap.this.size();
    }

    @Override public boolean isEmpty() {
      return CustomConcurrentHashMap.this.isEmpty();
    }

    @Override public boolean contains(Object o) {
      return CustomConcurrentHashMap.this.containsValue(o);
    }

    @Override public void clear() {
      CustomConcurrentHashMap.this.clear();
    }
  }

  final class EntrySet extends AbstractSet<Entry<K, V>> {

    @Override public Iterator<Entry<K, V>> iterator() {
      return new EntryIterator();
    }

    @Override public boolean contains(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;
      Object key = e.getKey();
      if (key == null) {
        return false;
      }
      V v = CustomConcurrentHashMap.this.get(key);

      return v != null && valueEquivalence.equivalent(e.getValue(), v);
    }

    @Override public boolean remove(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Entry<?, ?> e = (Entry<?, ?>) o;
      Object key = e.getKey();
      return key != null
          && CustomConcurrentHashMap.this.remove(key, e.getValue());
    }

    @Override public int size() {
      return CustomConcurrentHashMap.this.size();
    }

    @Override public boolean isEmpty() {
      return CustomConcurrentHashMap.this.isEmpty();
    }

    @Override public void clear() {
      CustomConcurrentHashMap.this.clear();
    }
  }

  /* ---------------- Serialization Support -------------- */

  private static final long serialVersionUID = 4;

  Object writeReplace() {
    return new SerializationProxy<K, V>(keyStrength, valueStrength,
        keyEquivalence, valueEquivalence, expireAfterWriteNanos,
        expireAfterAccessNanos, maximumSize, concurrencyLevel, evictionListener,
        this);
  }

  /**
   * The actual object that gets serialized. Unfortunately, readResolve()
   * doesn't get called when a circular dependency is present, so the proxy
   * must be able to behave as the map itself.
   */
  abstract static class AbstractSerializationProxy<K, V>
      extends ForwardingConcurrentMap<K, V> implements Serializable {
    private static final long serialVersionUID = 2;

    final Strength keyStrength;
    final Strength valueStrength;
    final Equivalence<Object> keyEquivalence;
    final Equivalence<Object> valueEquivalence;
    final long expireAfterWriteNanos;
    final long expireAfterAccessNanos;
    final int maximumSize;
    final int concurrencyLevel;
    final MapEvictionListener<? super K, ? super V> evictionListener;

    transient ConcurrentMap<K, V> delegate;

    AbstractSerializationProxy(Strength keyStrength,
        Strength valueStrength,
        Equivalence<Object> keyEquivalence,
        Equivalence<Object> valueEquivalence,
        long expireAfterWriteNanos, long expireAfterAccessNanos,
        int maximumSize, int concurrencyLevel,
        MapEvictionListener<? super K, ? super V> evictionListener,
        ConcurrentMap<K, V> delegate) {
      this.keyStrength = keyStrength;
      this.valueStrength = valueStrength;
      this.keyEquivalence = keyEquivalence;
      this.valueEquivalence = valueEquivalence;
      this.expireAfterWriteNanos = expireAfterWriteNanos;
      this.expireAfterAccessNanos = expireAfterAccessNanos;
      this.maximumSize = maximumSize;
      this.concurrencyLevel = concurrencyLevel;
      this.evictionListener = evictionListener;
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

    MapMaker readMapMaker(ObjectInputStream in) throws IOException {
      int size = in.readInt();
      MapMaker mapMaker = new MapMaker()
          .initialCapacity(size)
          .setKeyStrength(keyStrength)
          .setValueStrength(valueStrength)
          .privateKeyEquivalence(keyEquivalence)
          .privateValueEquivalence(valueEquivalence)
          .concurrencyLevel(concurrencyLevel);
      mapMaker.evictionListener(evictionListener);
      if (expireAfterWriteNanos > 0) {
        mapMaker.expireAfterWrite(expireAfterWriteNanos, TimeUnit.NANOSECONDS);
      }
      if (expireAfterAccessNanos > 0) {
        mapMaker.expireAfterAccess(
            expireAfterAccessNanos, TimeUnit.NANOSECONDS);
      }
      if (maximumSize != MapMaker.UNSET_INT) {
        mapMaker.maximumSize(maximumSize);
      }
      return mapMaker;
    }

    @SuppressWarnings("unchecked")
    void readEntries(ObjectInputStream in) throws IOException,
        ClassNotFoundException {
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
   * The actual object that gets serialized. Unfortunately, readResolve()
   * doesn't get called when a circular dependency is present, so the proxy
   * must be able to behave as the map itself.
   */
  private static class SerializationProxy<K, V>
      extends AbstractSerializationProxy<K, V> {
    private static final long serialVersionUID = 2;

    SerializationProxy(Strength keyStrength,
        Strength valueStrength,
        Equivalence<Object> keyEquivalence,
        Equivalence<Object> valueEquivalence,
        long expireAfterWriteNanos,
        long expireAfterAccessNanos,
        int maximumSize,
        int concurrencyLevel,
        MapEvictionListener<? super K, ? super V> evictionListener,
        ConcurrentMap<K, V> delegate) {
      super(keyStrength, valueStrength, keyEquivalence, valueEquivalence,
          expireAfterWriteNanos, expireAfterAccessNanos, maximumSize,
          concurrencyLevel, evictionListener, delegate);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      writeMapTo(out);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
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
