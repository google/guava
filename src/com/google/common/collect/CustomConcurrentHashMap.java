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

import com.google.common.base.Equivalence;
import com.google.common.base.Equivalences;
import com.google.common.base.FinalizableReferenceQueue;
import com.google.common.base.FinalizableSoftReference;
import com.google.common.base.FinalizableWeakReference;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
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
   * TODO: Select a permanent name for this class. The name matters because
   * we expose it in the serialized state and will be stuck w/ it forever.
   */

  /*
   * The basic strategy is to subdivide the table among Segments,
   * each of which itself is a concurrently readable hash table.
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
   * Number of unsynchronized retries in size and containsValue methods before
   * resorting to locking. This is used to avoid unbounded retries if tables
   * undergo continuous modification which would make it impossible to obtain
   * an accurate result.
   *
   * TODO: Talk to Doug about the possiblity of defining size() and
   * containsValue() in terms of weakly consistent iteration.
   */
  static final int RETRIES_BEFORE_LOCK = 2;

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

  /** Strategy for comparing keys. */
  final Equivalence<Object> keyEquivalence;

  /** Strategy for comparing values. */
  final Equivalence<Object> valueEquivalence;

  /** Strategy for referencing keys. */
  final Strength keyStrength;

  /** Strategy for referencing values. */
  final Strength valueStrength;

  /** How long the map retains values in ns. */
  final long expirationNanos;

  /** True if expiration is enabled. */
  final boolean expires;

  /**
   * The maximum size of this map. MapMaker.UNSET_MAXIMUM_SIZE if there is no
   * maximum.
   */
  final int maximumSize;

  /** True if size-based eviction is enabled. */
  final boolean evicts;

  /** The concurrency level. */
  final int concurrencyLevel;

  /** Factory used to create new entries. */
  final transient EntryFactory entryFactory;

  /**
   * Creates a new, empty map with the specified strategy, initial capacity
   * and concurrency level.
   */
  CustomConcurrentHashMap(MapMaker builder) {
    keyStrength = builder.getKeyStrength();
    valueStrength = builder.getValueStrength();

    keyEquivalence = builder.getKeyEquivalence();
    valueEquivalence = builder.getValueEquivalence();

    expirationNanos = builder.getExpirationNanos();
    maximumSize = builder.maximumSize;

    evicts = maximumSize != MapMaker.UNSET_MAXIMUM_SIZE;
    expires = expirationNanos > 0;

    entryFactory = EntryFactory.getFactory(keyStrength, expires, evicts);

    concurrencyLevel = filterConcurrencyLevel(builder.getConcurrencyLevel());

    // TODO: Handle initialCapacity > maximumSize.
    int initialCapacity = builder.getInitialCapacity();
    if (initialCapacity > MAXIMUM_CAPACITY) {
      initialCapacity = MAXIMUM_CAPACITY;
    }

    // Find power-of-two sizes best matching arguments
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

    if (evicts) {
      // Ensure sum of segment max sizes = overall max size
      int maximumSegmentSize = maximumSize / segmentCount + 1;
      int remainder = maximumSize % segmentCount;
      for (int i = 0; i < this.segments.length; ++i) {
        if (i == remainder) {
          maximumSegmentSize--;
        }
        this.segments[i] = new Segment(segmentSize, maximumSegmentSize);
      }
    } else {
      for (int i = 0; i < this.segments.length; ++i) {
        this.segments[i] = new Segment(segmentSize,
            MapMaker.UNSET_MAXIMUM_SIZE);
      }
    }
  }

  /**
   * Returns the given concurrency level or MAX_SEGMENTS if the given level
   * is > MAX_SEGMENTS.
   */
  static int filterConcurrencyLevel(int concurrenyLevel) {
    return Math.min(concurrenyLevel, MAX_SEGMENTS);
  }

  enum Strength {
    /*
     * TODO: If we strongly reference the value and aren't computing, we
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

    // TODO: Generate all of these combos at build time.

    STRONG {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          ReferenceEntry<K, V> next) {
        return new StrongEntry<K, V>(map, key, hash, next);
      }
    },
    STRONG_EXPIRABLE {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          ReferenceEntry<K, V> next) {
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
          ReferenceEntry<K, V> next) {
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
          ReferenceEntry<K, V> next) {
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
          ReferenceEntry<K, V> next) {
        return new SoftEntry<K, V>(map, key, hash, next);
      }
    },
    SOFT_EXPIRABLE {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          ReferenceEntry<K, V> next) {
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
          ReferenceEntry<K, V> next) {
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
          ReferenceEntry<K, V> next) {
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
          ReferenceEntry<K, V> next) {
        return new WeakEntry<K, V>(map, key, hash, next);
      }
    },
    WEAK_EXPIRABLE {
      @Override <K, V> ReferenceEntry<K, V> newEntry(
          CustomConcurrentHashMap<K, V> map, K key, int hash,
          ReferenceEntry<K, V> next) {
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
          ReferenceEntry<K, V> next) {
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
          ReferenceEntry<K, V> next) {
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
    static final EntryFactory[][] FACTORIES = {
      { STRONG, STRONG_EXPIRABLE, STRONG_EVICTABLE, STRONG_EXPIRABLE_EVICTABLE },
      { SOFT,   SOFT_EXPIRABLE,   SOFT_EVICTABLE,   SOFT_EXPIRABLE_EVICTABLE   },
      { WEAK,   WEAK_EXPIRABLE,   WEAK_EVICTABLE,   WEAK_EXPIRABLE_EVICTABLE   }
    };

    static EntryFactory getFactory(Strength keyStrength,
        boolean expires, boolean evicts) {
      int flags = (expires ? EXPIRABLE_MASK : 0)
          | (evicts ? EVICTABLE_MASK : 0);
      return FACTORIES[keyStrength.ordinal()][flags];
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
        ReferenceEntry<K, V> next);

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
      Expirable originalExpirable = (Expirable) original;
      Expirable newExpirable = (Expirable) newEntry;
      newExpirable.setWriteTime(originalExpirable.getWriteTime());

      connectExpirable(originalExpirable.getPreviousExpirable(), newExpirable);
      connectExpirable(newExpirable, originalExpirable.getNextExpirable());

      nullifyExpirable(originalExpirable);
    }

    <K, V> void copyEvictableEntry(
        ReferenceEntry<K, V> original, ReferenceEntry<K, V> newEntry) {
      Evictable originalEvictable = (Evictable) original;
      Evictable newEvictable = (Evictable) newEntry;
      newEvictable.setLastUsage(originalEvictable.getLastUsage());
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
  }

  /**
   * Placeholder. Indicates that the value hasn't been set yet.
   */
  static final ValueReference<Object, Object> UNSET
      = new ValueReference<Object, Object>() {
    public Object get() {
      return null;
    }
    public ValueReference<Object, Object> copyFor(
        ReferenceEntry<Object, Object> entry) {
      throw new AssertionError();
    }
    public Object waitForValue() {
      throw new AssertionError();
    }
  };

  /**
   * Singleton placeholder that indicates a value is being computed.
   */
  @SuppressWarnings("unchecked")
  // Safe because impl never uses a parameter or returns any non-null value
  private static <K, V> ValueReference<K, V> unset() {
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
    void valueReclaimed();

    /** Gets the next entry in the chain. */
    ReferenceEntry<K, V> getNext();

    /** Gets the entry's hash. */
    int getHash();

    /** Gets the key for this entry. */
    K getKey();
  }

  /**
   * Implemented by entries that are expirable. Expirable entries are
   * maintained in a doubly-linked list. New entries are added at the tail
   * of the list at write time; stale entries are expired from the head
   * of the list.
   */
  interface Expirable {
    /** Gets the entry write time in ns. */
    long getWriteTime();

    /** Sets the entry write time in ns. */
    void setWriteTime(long writeTime);

    /** Gets the next entry in the recency list. */
    Expirable getNextExpirable();

    /** Sets the next entry in the recency list. */
    void setNextExpirable(Expirable next);

    /** Gets the previous entry in the recency list. */
    Expirable getPreviousExpirable();

    /** Sets the previous entry in the recency list. */
    void setPreviousExpirable(Expirable previous);
  }

  private enum NullExpirable implements Expirable {
    INSTANCE;

    @Override
    public long getWriteTime() {
      return 0;
    }
    @Override
    public void setWriteTime(long writeTime) {}

    @Override
    public Expirable getNextExpirable() {
      return this;
    }
    @Override
    public void setNextExpirable(Expirable next) {}

    @Override
    public Expirable getPreviousExpirable() {
      return this;
    }
    @Override
    public void setPreviousExpirable(Expirable previous) {}
  }

  /** Implemented by entries that support eviction. */
  interface Evictable {
    /** Sets the last usage timestamp. */
    void setLastUsage(int timestamp);

    /** Gets the last usage timestamp. */
    int getLastUsage();
  }

  /*
   * Note: All of this duplicate code sucks, but it saves a lot of memory.
   * If only Java had mixins! To maintain this code, make a change for
   * the strong reference type. Then, cut and paste, and replace "Strong"
   * with "Soft" or "Weak" within the pasted text. The primary difference
   * is that strong entries store the key reference directly while soft
   * and weak entries delegate to their respective superclasses.
   *
   * TODO: Generate this code.
   */

  /**
   * Used for strongly-referenced keys.
   */
  private static class StrongEntry<K, V> implements ReferenceEntry<K, V> {
    final K key;

    StrongEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        ReferenceEntry<K, V> next) {
      this.map = map;
      this.key = key;
      this.hash = hash;
      this.next = next;
    }

    public K getKey() {
      return this.key;
    }

    // The code below is exactly the same for each entry type.

    final CustomConcurrentHashMap<K, V> map;
    final int hash;
    final ReferenceEntry<K, V> next;
    volatile ValueReference<K, V> valueReference = unset();

    public ValueReference<K, V> getValueReference() {
      return valueReference;
    }
    public void setValueReference(
        ValueReference<K, V> valueReference) {
      this.valueReference = valueReference;
    }
    public void valueReclaimed() {
      map.reclaimValue(this);
    }
    public int getHash() {
      return hash;
    }
    public ReferenceEntry<K, V> getNext() {
      return next;
    }
  }

  private static class StrongExpirableEntry<K, V> extends StrongEntry<K, V>
      implements Expirable {
    StrongExpirableEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each expirable entry type.

    volatile long writeTime = Long.MAX_VALUE;
    public long getWriteTime() {
      return writeTime;
    }
    public void setWriteTime(long writeTime) {
      this.writeTime = writeTime;
    }

    @GuardedBy("Segment.this")
    Expirable next = NullExpirable.INSTANCE;
    public Expirable getNextExpirable() {
      return next;
    }
    public void setNextExpirable(Expirable next) {
      this.next = next;
    }

    @GuardedBy("Segment.this")
    Expirable previous = NullExpirable.INSTANCE;
    public Expirable getPreviousExpirable() {
      return previous;
    }
    public void setPreviousExpirable(Expirable previous) {
      this.previous = previous;
    }
  }

  private static class StrongEvictableEntry<K, V> extends StrongEntry<K, V>
      implements Evictable {
    StrongEvictableEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each evictable entry type.

    volatile int lastUsage;
    public int getLastUsage() {
      return lastUsage;
    }
    public void setLastUsage(int lastUsage) {
      this.lastUsage = lastUsage;
    }
  }

  private static class StrongExpirableEvictableEntry<K, V>
      extends StrongEntry<K, V> implements Expirable, Evictable {
    StrongExpirableEvictableEntry(CustomConcurrentHashMap<K, V> map, K key,
        int hash, ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each expirable entry type.

    volatile long writeTime = Long.MAX_VALUE;
    public long getWriteTime() {
      return writeTime;
    }
    public void setWriteTime(long writeTime) {
      this.writeTime = writeTime;
    }

    @GuardedBy("Segment.this")
    Expirable next = NullExpirable.INSTANCE;
    public Expirable getNextExpirable() {
      return next;
    }
    public void setNextExpirable(Expirable next) {
      this.next = next;
    }

    @GuardedBy("Segment.this")
    Expirable previous = NullExpirable.INSTANCE;
    public Expirable getPreviousExpirable() {
      return previous;
    }
    public void setPreviousExpirable(Expirable previous) {
      this.previous = previous;
    }

    // The code below is exactly the same for each evictable entry type.

    volatile int lastUsage;
    public int getLastUsage() {
      return lastUsage;
    }
    public void setLastUsage(int lastUsage) {
      this.lastUsage = lastUsage;
    }
  }

  /**
   * Used for softly-referenced keys.
   */
  private static class SoftEntry<K, V> extends FinalizableSoftReference<K>
      implements ReferenceEntry<K, V> {
    SoftEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        ReferenceEntry<K, V> next) {
      super(key, QueueHolder.queue);
      this.map = map;
      this.hash = hash;
      this.next = next;
    }

    public K getKey() {
      return get();
    }

    public void finalizeReferent() {
      map.removeEntry(this);
    }

    // The code below is exactly the same for each entry type.

    final CustomConcurrentHashMap<K, V> map;
    final int hash;
    final ReferenceEntry<K, V> next;
    volatile ValueReference<K, V> valueReference = unset();

    public ValueReference<K, V> getValueReference() {
      return valueReference;
    }
    public void setValueReference(
        ValueReference<K, V> valueReference) {
      this.valueReference = valueReference;
    }
    public void valueReclaimed() {
      map.reclaimValue(this);
    }
    public int getHash() {
      return hash;
    }
    public ReferenceEntry<K, V> getNext() {
      return next;
    }
  }

  private static class SoftExpirableEntry<K, V> extends SoftEntry<K, V>
      implements Expirable {
    SoftExpirableEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each expirable entry type.

    volatile long writeTime = Long.MAX_VALUE;
    public long getWriteTime() {
      return writeTime;
    }
    public void setWriteTime(long writeTime) {
      this.writeTime = writeTime;
    }

    @GuardedBy("Segment.this")
    Expirable next = NullExpirable.INSTANCE;
    public Expirable getNextExpirable() {
      return next;
    }
    public void setNextExpirable(Expirable next) {
      this.next = next;
    }

    @GuardedBy("Segment.this")
    Expirable previous = NullExpirable.INSTANCE;
    public Expirable getPreviousExpirable() {
      return previous;
    }
    public void setPreviousExpirable(Expirable previous) {
      this.previous = previous;
    }
  }

  private static class SoftEvictableEntry<K, V> extends SoftEntry<K, V>
      implements Evictable {
    SoftEvictableEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each evictable entry type.

    volatile int lastUsage;
    public int getLastUsage() {
      return lastUsage;
    }
    public void setLastUsage(int lastUsage) {
      this.lastUsage = lastUsage;
    }
  }

  private static class SoftExpirableEvictableEntry<K, V>
      extends SoftEntry<K, V> implements Expirable, Evictable {
    SoftExpirableEvictableEntry(CustomConcurrentHashMap<K, V> map, K key,
        int hash, ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each expirable entry type.

    volatile long writeTime = Long.MAX_VALUE;
    public long getWriteTime() {
      return writeTime;
    }
    public void setWriteTime(long writeTime) {
      this.writeTime = writeTime;
    }

    @GuardedBy("Segment.this")
    Expirable next = NullExpirable.INSTANCE;
    public Expirable getNextExpirable() {
      return next;
    }
    public void setNextExpirable(Expirable next) {
      this.next = next;
    }

    @GuardedBy("Segment.this")
    Expirable previous = NullExpirable.INSTANCE;
    public Expirable getPreviousExpirable() {
      return previous;
    }
    public void setPreviousExpirable(Expirable previous) {
      this.previous = previous;
    }

    // The code below is exactly the same for each evictable entry type.

    volatile int lastUsage;
    public int getLastUsage() {
      return lastUsage;
    }
    public void setLastUsage(int lastUsage) {
      this.lastUsage = lastUsage;
    }
  }

  /**
   * Used for weakly-referenced keys.
   */
  private static class WeakEntry<K, V> extends FinalizableWeakReference<K>
      implements ReferenceEntry<K, V> {
    WeakEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        ReferenceEntry<K, V> next) {
      super(key, QueueHolder.queue);
      this.map = map;
      this.hash = hash;
      this.next = next;
    }

    public K getKey() {
      return get();
    }

    public void finalizeReferent() {
      map.removeEntry(this);
    }

    // The code below is exactly the same for each entry type.

    final CustomConcurrentHashMap<K, V> map;
    final int hash;
    final ReferenceEntry<K, V> next;
    volatile ValueReference<K, V> valueReference = unset();

    public ValueReference<K, V> getValueReference() {
      return valueReference;
    }
    public void setValueReference(
        ValueReference<K, V> valueReference) {
      this.valueReference = valueReference;
    }
    public void valueReclaimed() {
      map.reclaimValue(this);
    }
    public int getHash() {
      return hash;
    }
    public ReferenceEntry<K, V> getNext() {
      return next;
    }
  }

  private static class WeakExpirableEntry<K, V> extends WeakEntry<K, V>
      implements Expirable {
    WeakExpirableEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each expirable entry type.

    volatile long writeTime = Long.MAX_VALUE;
    public long getWriteTime() {
      return writeTime;
    }
    public void setWriteTime(long writeTime) {
      this.writeTime = writeTime;
    }

    @GuardedBy("Segment.this")
    Expirable next = NullExpirable.INSTANCE;
    public Expirable getNextExpirable() {
      return next;
    }
    public void setNextExpirable(Expirable next) {
      this.next = next;
    }

    @GuardedBy("Segment.this")
    Expirable previous = NullExpirable.INSTANCE;
    public Expirable getPreviousExpirable() {
      return previous;
    }
    public void setPreviousExpirable(Expirable previous) {
      this.previous = previous;
    }
  }

  private static class WeakEvictableEntry<K, V> extends WeakEntry<K, V>
      implements Evictable {
    WeakEvictableEntry(CustomConcurrentHashMap<K, V> map, K key, int hash,
        ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each evictable entry type.

    volatile int lastUsage;
    public int getLastUsage() {
      return lastUsage;
    }
    public void setLastUsage(int lastUsage) {
      this.lastUsage = lastUsage;
    }
  }

  private static class WeakExpirableEvictableEntry<K, V>
      extends WeakEntry<K, V> implements Expirable, Evictable {
    WeakExpirableEvictableEntry(CustomConcurrentHashMap<K, V> map, K key,
        int hash, ReferenceEntry<K, V> next) {
      super(map, key, hash, next);
    }

    // The code below is exactly the same for each expirable entry type.

    volatile long writeTime = Long.MAX_VALUE;
    public long getWriteTime() {
      return writeTime;
    }
    public void setWriteTime(long writeTime) {
      this.writeTime = writeTime;
    }

    @GuardedBy("Segment.this")
    Expirable next = NullExpirable.INSTANCE;
    public Expirable getNextExpirable() {
      return next;
    }
    public void setNextExpirable(Expirable next) {
      this.next = next;
    }

    @GuardedBy("Segment.this")
    Expirable previous = NullExpirable.INSTANCE;
    public Expirable getPreviousExpirable() {
      return previous;
    }
    public void setPreviousExpirable(Expirable previous) {
      this.previous = previous;
    }

    // The code below is exactly the same for each evictable entry type.

    volatile int lastUsage;
    public int getLastUsage() {
      return lastUsage;
    }
    public void setLastUsage(int lastUsage) {
      this.lastUsage = lastUsage;
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

    public void finalizeReferent() {
      entry.valueReclaimed();
    }

    public ValueReference<K, V> copyFor(
        ReferenceEntry<K, V> entry) {
      return new WeakValueReference<K, V>(get(), entry);
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

    public void finalizeReferent() {
      entry.valueReclaimed();
    }

    public ValueReference<K, V> copyFor(
        ReferenceEntry<K, V> entry) {
      return new SoftValueReference<K, V>(get(), entry);
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

    public ValueReference<K, V> copyFor(
        ReferenceEntry<K, V> entry) {
      return this;
    }

    public V waitForValue() {
      return get();
    }
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
    // TODO: use Hashing/move this to Hashing?
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }

  /**
   * Sets the value reference on an entry and notifies waiting threads.
   */
  void setValueReference(ReferenceEntry<K, V> entry,
      ValueReference<K, V> valueReference) {
    entry.setValueReference(valueReference);
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

  int hash(Object key) {
    // TODO: can we just trust keyEquivalence to throw NPE as it promises?
    // (That is, if some user's Equivalence doesn't, let them get a broken map?)
    int h = keyEquivalence.hash(checkNotNull(key));
    return rehash(h);
  }

  boolean reclaimValue(ReferenceEntry<K, V> entry) {
    int hash = entry.getHash();
    return segmentFor(hash).reclaimValue(entry, hash);
  }

  boolean removeEntry(ReferenceEntry<K, V> entry) {
    int hash = entry.getHash();
    return segmentFor(hash).removeEntry(entry, hash);
  }

  @GuardedBy("Segment.this")
  static void connectExpirable(Expirable previous, Expirable next) {
    previous.setNextExpirable(next);
    next.setPreviousExpirable(previous);
  }

  @GuardedBy("Segment.this")
  static void nullifyExpirable(Expirable nulled) {
    nulled.setNextExpirable(NullExpirable.INSTANCE);
    nulled.setPreviousExpirable(NullExpirable.INSTANCE);
  }

  /**
   * Returns true if the given entry has expired.
   */
  boolean isExpired(ReferenceEntry<K, V> entry) {
    return isExpired((Expirable) entry, System.nanoTime());
  }

  /**
   * Returns true if the given entry has expired.
   */
  boolean isExpired(Expirable expirable, long now) {
    // Avoid overflow.
    return now - expirable.getWriteTime() > expirationNanos;
  }

  /**
   * Gets the value from an entry. Returns null if the value is null (i.e.
   * reclaimed or not computed yet) or if the entry is expired. If
   * you already called expireEntries() you can just check the value for
   * null and skip the expiration check.
   */
  V getUnexpiredValue(ReferenceEntry<K, V> e) {
    V value = e.getValueReference().get();
    return (expires && isExpired(e)) ? null : value;
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
    // TODO: Lazily create segments.
    return segments[(hash >>> segmentShift) & segmentMask];
  }

  /* ---------------- Inner Classes -------------- */

  /**
   * Segments are specialized versions of hash tables.  This subclass inherits
   * from ReentrantLock opportunistically, just to simplify some locking and
   * avoid separate construction.
   */
  @SuppressWarnings("serial") // This class is never serialized.
  final class Segment extends ReentrantLock {

    /*
     * TODO: Consider copying variables (like evicts) from outer class into
     * this class. It will require more memory but will reduce indirection.
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
     * The number of elements in this segment's region.
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
     * The maximum size of this map. MapMaker.UNSET_MAXIMUM_SIZE if there is no
     * maximum.
     */
    final int maxSegmentSize;

    /** The head of the expiration queue. */
    final Expirable expirationHead = new Expirable() {
      public long getWriteTime() {
        return Long.MAX_VALUE;
      }
      public void setWriteTime(long writeTime) {}

      @GuardedBy("Segment.this")
      Expirable next = this;
      public Expirable getNextExpirable() {
        return next;
      }
      public void setNextExpirable(Expirable next) {
        this.next = next;
      }

      @GuardedBy("Segment.this")
      Expirable previous = this;
      public Expirable getPreviousExpirable() {
        return previous;
      }
      public void setPreviousExpirable(Expirable previous) {
        this.previous = previous;
      }
    };

    Segment(int initialCapacity, int maxSegmentSize) {
      setTable(newEntryArray(initialCapacity));
      this.maxSegmentSize = maxSegmentSize;
    }

    /**
     * Sets a new value of an entry. Adds newly created entries at the end
     * of the expiration queue.
     */
    @GuardedBy("Segment.this") // if expires
    void setValue(ReferenceEntry<K, V> entry, V value, boolean inserted) {
      // TODO: explore other expiration strategies (e.g. on insertion)
      if (expires) {
        Expirable expirable = (Expirable) entry;
        addExpirable(expirable);
      }
      setValueReference(entry, valueStrength.referenceValue(entry, value));
    }

    @GuardedBy("Segment.this")
    void addExpirable(Expirable added) {
      connectExpirable(added.getPreviousExpirable(),
          added.getNextExpirable());

      added.setWriteTime(System.nanoTime());

      connectExpirable(expirationHead.getPreviousExpirable(), added);
      connectExpirable(added, expirationHead);
    }

    @GuardedBy("Segment.this")
    void removeExpirable(Expirable removed) {
      connectExpirable(removed.getPreviousExpirable(),
          removed.getNextExpirable());
      nullifyExpirable(removed);
    }

    /**
     * Removes expired entries.
     */
    @GuardedBy("Segment.this")
    void expireEntries() {
      Expirable expirable = expirationHead.getNextExpirable();
      if (expirable == expirationHead) {
        // There's no point in calling nanoTime() if we have no entries to
        // expire.
        return;
      }
      long now = System.nanoTime();
      while (expirable != expirationHead && isExpired(expirable, now)) {
        @SuppressWarnings("unchecked")
        ReferenceEntry<K, V> entry = (ReferenceEntry<K,V>) expirable;
        removeEntry(entry, entry.getHash());
        // removeEntry should have called removeExpirable, but let's be sure
        removeExpirable(expirable);
        expirable = expirationHead.getNextExpirable();
      }
    }

    AtomicReferenceArray<ReferenceEntry<K, V>> newEntryArray(int size) {
      return new AtomicReferenceArray<ReferenceEntry<K, V>>(size);
    }

    @GuardedBy("Segment.this")
    void clearExpirationQueue() {
      Expirable expirable = expirationHead.getNextExpirable();
      while (expirable != expirationHead) {
        Expirable next = expirable.getNextExpirable();
        nullifyExpirable(expirable);
        expirable = next;
      }

      expirationHead.setNextExpirable(expirationHead);
      expirationHead.setPreviousExpirable(expirationHead);
    }

    /**
     * Sets table to new HashEntry array.
     */
    @GuardedBy("Segment.this")
    void setTable(AtomicReferenceArray<ReferenceEntry<K, V>> newTable) {
      this.threshold = newTable.length() * 3 / 4; // 0.75
      this.table = newTable;
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

    public ReferenceEntry<K, V> getEntry(Object key, int hash) {
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

          if (keyEquivalence.equivalent(entryKey, key)) {
            if (expires && isExpired(e)) {
              continue;
            }
            return e;
          }
        }
      }

      return null;
    }

    V get(Object key, int hash) {
      ReferenceEntry<K, V> entry = getEntry(key, hash);
      if (entry == null) {
        return null;
      }

      return entry.getValueReference().get();
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

          if (keyEquivalence.equivalent(entryKey, key)) {
            return getUnexpiredValue(e) != null;
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
            V entryValue = getUnexpiredValue(e);
            if (entryValue == null) {
              continue;
            }
            if (valueEquivalence.equivalent(entryValue, value)) {
              return true;
            }
          }
        }
      }

      return false;
    }

    boolean replace(K key, int hash, V oldValue, V newValue) {
      checkNotNull(newValue);
      lock();
      try {
        if (expires) {
          expireEntries();
        }

        for (ReferenceEntry<K, V> e = getFirst(hash); e != null;
            e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash && entryKey != null
              && keyEquivalence.equivalent(key, entryKey)) {
            // If the value disappeared, this entry is partially collected,
            // and we should pretend like it doesn't exist.
            V entryValue = e.getValueReference().get();
            if (entryValue == null) {
              return false;
            }

            if (valueEquivalence.equivalent(entryValue, oldValue)) {
              setValue(e, newValue, false);
              return true;
            }
          }
        }

        return false;
      } finally {
        unlock();
      }
    }

    V replace(K key, int hash, V newValue) {
      checkNotNull(newValue);
      lock();
      try {
        if (expires) {
          expireEntries();
        }

        for (ReferenceEntry<K, V> e = getFirst(hash); e != null;
            e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash && entryKey != null
              && keyEquivalence.equivalent(key, entryKey)) {
            // If the value disappeared, this entry is partially collected,
            // and we should pretend like it doesn't exist.
            V entryValue = e.getValueReference().get();
            if (entryValue == null) {
              return null;
            }

            setValue(e, newValue, false);
            return entryValue;
          }
        }

        return null;
      } finally {
        unlock();
      }
    }

    V put(K key, int hash, V value, boolean onlyIfAbsent) {
      checkNotNull(value);
      lock();
      try {
        if (expires) {
          expireEntries();
        }

        int newCount = this.count + 1;
        if (newCount > this.threshold) { // ensure capacity
          expand();
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

            // If the value disappeared, this entry is partially collected,
            // and we should pretend like it doesn't exist.
            V entryValue = e.getValueReference().get();
            boolean absent = entryValue == null;
            if (onlyIfAbsent && !absent) {
              return entryValue;
            }

            setValue(e, value, absent);
            return entryValue;
          }
        }

        // Create a new entry.
        ++modCount;
        ReferenceEntry<K, V> newEntry = entryFactory.newEntry(
            CustomConcurrentHashMap.this, key, hash, first);
        setValue(newEntry, value, true);
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
              K key = e.getKey();
              if (key != null) {
                int newIndex = e.getHash() & newMask;
                ReferenceEntry<K, V> newNext = newTable.get(newIndex);
                newTable.set(newIndex, copyEntry(e, newNext));
              } // Else key was reclaimed. Skip entry.
            }
          }
        }
      }
      table = newTable;
    }

    V remove(Object key, int hash, boolean expire) {
      lock();
      try {
        if (expire) {
          expireEntries();
        }

        int newCount = this.count - 1;
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash && entryKey != null
              && keyEquivalence.equivalent(entryKey, key)) {
            V entryValue = e.getValueReference().get();
            ++modCount;
            ReferenceEntry<K, V> newFirst = removeFromTable(first, e);
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
        if (expires) {
          expireEntries();
        }

        int newCount = this.count - 1;
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash && entryKey != null
              && keyEquivalence.equivalent(entryKey, key)) {
            V entryValue = e.getValueReference().get();
            if (value == entryValue || (value != null && entryValue != null
                && valueEquivalence.equivalent(entryValue, value))) {
              ++modCount;
              ReferenceEntry<K, V> newFirst = removeFromTable(first, e);
              table.set(index, newFirst);
              this.count = newCount; // write-volatile
              return true;
            } else {
              return false;
            }
          }
        }

        return false;
      } finally {
        unlock();
      }
    }

    /**
     * Reclaims a value that has been collected. This is identical to
     * removeEntry with the addition of a null value check, which avoids
     * removing an entry that has already been reused for a new value.
     */
    boolean reclaimValue(ReferenceEntry<K, V> entry, int hash) {
      /*
       * Used for reference cleanup. We probably don't want to expire entries
       * here as it can be called over and over.
       */
      lock();
      try {
        int newCount = this.count - 1;
        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
          if (e == entry) {
            V entryValue = e.getValueReference().get();
            if (entryValue == null) {
              ++modCount;
              ReferenceEntry<K, V> newFirst = removeFromTable(first, e);
              table.set(index, newFirst);
              this.count = newCount; // write-volatile
              return true;
            } else {
              return false;
            }
          }
        }

        return false;
      } finally {
        unlock();
      }
    }

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
            ReferenceEntry<K, V> newFirst = removeFromTable(first, e);
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
        ReferenceEntry<K, V> removed) {
      if (expires) {
        removeExpirable((Expirable) removed);
      }

      ReferenceEntry<K, V> newFirst = removed.getNext();
      for (ReferenceEntry<K, V> p = first; p != removed; p = p.getNext()) {
        K pKey = p.getKey();
        if (pKey != null) {
          newFirst = copyEntry(p, newFirst);
        } // Else key was reclaimed. Skip entry.
      }
      return newFirst;
    }

    void clear() {
      if (count != 0) {
        lock();
        try {
          AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
          for (int i = 0; i < table.length(); ++i) {
            table.set(i, null);
          }
          clearExpirationQueue();

          ++modCount;
          count = 0; // write-volatile
        } finally {
          unlock();
        }
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
    long check = 0;
    int[] mc = new int[segments.length];
    // Try a few times to get accurate count. On failure due to
    // continuous async changes in table, resort to locking.
    for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
      check = 0;
      sum = 0;
      int mcsum = 0;
      for (int i = 0; i < segments.length; ++i) {
        sum += segments[i].count;
        mcsum += mc[i] = segments[i].modCount;
      }
      if (mcsum != 0) {
        for (int i = 0; i < segments.length; ++i) {
          check += segments[i].count;
          if (mc[i] != segments[i].modCount) {
            check = -1; // force retry
            break;
          }
        }
      }
      if (check == sum) {
        break;
      }
    }
    if (check != sum) { // Resort to locking all segments
      sum = 0;
      for (Segment segment : segments) {
        segment.lock();
      }
      for (Segment segment : segments) {
        sum += segment.count;
      }
      for (Segment segment : segments) {
        segment.unlock();
      }
    }
    return Ints.saturatedCast(sum);
  }

  @Override public V get(Object key) {
    int hash = hash(key);
    return segmentFor(hash).get(key, hash);
  }

  @Override public boolean containsKey(Object key) {
    int hash = hash(key);
    return segmentFor(hash).containsKey(key, hash);
  }

  @Override public boolean containsValue(Object value) {
    // TODO: document why we choose to throw over returning false?
    checkNotNull(value, "value");

    // See explanation of modCount use above

    Segment[] segments = this.segments;
    int[] mc = new int[segments.length];

    // Try a few times without locking
    for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
      int mcsum = 0;
      for (int i = 0; i < segments.length; ++i) {
        // TODO: verify the importance of this crazy trick with Doug
        @SuppressWarnings("UnusedDeclaration")
        int c = segments[i].count;
        mcsum += (mc[i] = segments[i].modCount);
        if (segments[i].containsValue(value)) {
          return true;
        }
      }
      boolean cleanSweep = true;
      if (mcsum != 0) {
        for (int i = 0; i < segments.length; ++i) {
          // TODO: verify the importance of this crazy trick with Doug
          @SuppressWarnings("UnusedDeclaration")
          int c = segments[i].count;
          if (mc[i] != segments[i].modCount) {
            cleanSweep = false;
            break;
          }
        }
      }
      if (cleanSweep) {
        return false;
      }
    }

    // Resort to locking all segments
    for (Segment segment : segments) {
      segment.lock();
    }
    try {
      for (Segment segment : segments) {
        if (segment.containsValue(value)) {
          return true;
        }
      }
    } finally {
      for (Segment segment : segments) {
        segment.unlock();
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
    return segmentFor(hash).remove(key, hash, expires);
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
      V value = getUnexpiredValue(entry);
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

      return v != null && valueEquivalence.equivalent(v, e.getValue());
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

  private static final long serialVersionUID = 2;

  Object writeReplace() {
    return new SerializationProxy<K, V>(keyStrength, valueStrength,
        keyEquivalence, valueEquivalence, expirationNanos, maximumSize,
        concurrencyLevel, this);
  }

  /**
   * The actual object that gets serialized. Unfortunately, readResolve()
   * doesn't get called when a circular dependency is present, so the proxy
   * must be able to behave as the map itself.
   */
  abstract static class AbstractSerializationProxy<K, V>
      extends ForwardingConcurrentMap<K, V> implements Serializable {
    private static final long serialVersionUID = 0;

    final Strength keyStrength;
    final Strength valueStrength;
    final Equivalence<Object> keyEquivalence;
    final Equivalence<Object> valueEquivalence;
    final long expirationNanos;
    final int maximumSize;
    final int concurrencyLevel;

    transient ConcurrentMap<K, V> delegate;

    AbstractSerializationProxy(Strength keyStrength,
        Strength valueStrength,
        Equivalence<Object> keyEquivalence,
        Equivalence<Object> valueEquivalence,
        long expirationNanos, int maximumSize, int concurrencyLevel,
        ConcurrentMap<K, V> delegate) {
      this.keyStrength = keyStrength;
      this.valueStrength = valueStrength;
      this.keyEquivalence = keyEquivalence;
      this.valueEquivalence = valueEquivalence;
      this.expirationNanos = expirationNanos;
      this.maximumSize = maximumSize;
      this.concurrencyLevel = concurrencyLevel;
      this.delegate = delegate;
    }

    protected ConcurrentMap<K, V> delegate() {
      return delegate;
    }

    void writeMapTo(java.io.ObjectOutputStream out) throws IOException {
      out.writeInt(delegate.size());
      // TODO: Serialize expiration times. (Wait, what??)
      for (Entry<K, V> entry : delegate.entrySet()) {
        out.writeObject(entry.getKey());
        out.writeObject(entry.getValue());
      }
      out.writeObject(null); // terminate entries
    }

    MapMaker readMapMaker(ObjectInputStream in) throws IOException,
        ClassNotFoundException {
      int size = in.readInt();
      MapMaker mapMaker = new MapMaker()
          .initialCapacity(size)
          .setKeyStrength(keyStrength)
          .setValueStrength(valueStrength)
          .privateKeyEquivalence(keyEquivalence)
          .privateValueEquivalence(valueEquivalence)
          .concurrencyLevel(concurrencyLevel);
      if (expirationNanos != 0) {
        // expiration() throws an exception if you pass 0.
        mapMaker.expiration(expirationNanos, TimeUnit.NANOSECONDS);
      }
      if (maximumSize != MapMaker.UNSET_MAXIMUM_SIZE) {
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
    private static final long serialVersionUID = 0;

    SerializationProxy(Strength keyStrength,
        Strength valueStrength,
        Equivalence<Object> keyEquivalence,
        Equivalence<Object> valueEquivalence,
        long expirationNanos,
        int maximumSize,
        int concurrencyLevel,
        ConcurrentMap<K, V> delegate) {
      super(keyStrength, valueStrength, keyEquivalence, valueEquivalence,
          expirationNanos, maximumSize, concurrencyLevel, delegate);
    }

    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
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
