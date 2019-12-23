/*
 * Copyright (C) 2012 The Guava Authors
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
import static com.google.common.collect.CollectPreconditions.checkRemove;
import static com.google.common.collect.CompactHashing.UNSET;
import static com.google.common.collect.Hashing.smearedHash;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2objc.annotations.WeakOuter;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * CompactHashMap is an implementation of a Map. All optional operations (put and remove) are
 * supported. Null keys and values are supported.
 *
 * <p>{@code containsKey(k)}, {@code put(k, v)} and {@code remove(k)} are all (expected and
 * amortized) constant time operations. Expected in the hashtable sense (depends on the hash
 * function doing a good job of distributing the elements to the buckets to a distribution not far
 * from uniform), and amortized since some operations can trigger a hash table resize.
 *
 * <p>Unlike {@code java.util.HashMap}, iteration is only proportional to the actual {@code size()},
 * which is optimal, and <i>not</i> the size of the internal hashtable, which could be much larger
 * than {@code size()}. Furthermore, this structure places significantly reduced load on the garbage
 * collector by only using a constant number of internal objects.
 *
 * <p>If there are no removals, then iteration order for the {@link #entrySet}, {@link #keySet}, and
 * {@link #values} views is the same as insertion order. Any removal invalidates any ordering
 * guarantees.
 *
 * <p>This class should not be assumed to be universally superior to {@code java.util.HashMap}.
 * Generally speaking, this class reduces object allocation and memory consumption at the price of
 * moderately increased constant factors of CPU. Only use this class when there is a specific reason
 * to prioritize memory over CPU.
 *
 * @author Louis Wasserman
 * @author Jon Noack
 */
@GwtIncompatible // not worth using in GWT for now
class CompactHashMap<K, V> extends AbstractMap<K, V> implements Serializable {
  /*
   * TODO: Make this a drop-in replacement for j.u. versions, actually drop them in, and test the
   * world. Figure out what sort of space-time tradeoff we're actually going to get here with the
   * *Map variants. This class is particularly hard to benchmark, because the benefit is not only in
   * less allocation, but also having the GC do less work to scan the heap because of fewer
   * references, which is particularly hard to quantify.
   */

  /** Creates an empty {@code CompactHashMap} instance. */
  public static <K, V> CompactHashMap<K, V> create() {
    return new CompactHashMap<>();
  }

  /**
   * Creates a {@code CompactHashMap} instance, with a high enough "initial capacity" that it
   * <i>should</i> hold {@code expectedSize} elements without growth.
   *
   * @param expectedSize the number of elements you expect to add to the returned set
   * @return a new, empty {@code CompactHashMap} with enough capacity to hold {@code expectedSize}
   *     elements without resizing
   * @throws IllegalArgumentException if {@code expectedSize} is negative
   */
  public static <K, V> CompactHashMap<K, V> createWithExpectedSize(int expectedSize) {
    return new CompactHashMap<>(expectedSize);
  }

  private static final Object NOT_FOUND = new Object();

  /**
   * Maximum allowed false positive probability of detecting a hash flooding attack given random
   * input.
   */
  @VisibleForTesting(
      )
  static final double HASH_FLOODING_FPP = 0.001;

  /**
   * Maximum allowed length of a hash table bucket before falling back to a j.u.LinkedHashMap-based
   * implementation. Experimentally determined.
   */
  private static final int MAX_HASH_BUCKET_LENGTH = 9;

  /**
   * The hashtable object. This can be either:
   *
   * <ul>
   *   <li>a byte[], short[], or int[], with size a power of two, created by
   *       CompactHashing.createTable, whose values are either
   *       <ul>
   *         <li>UNSET, meaning "null pointer"
   *         <li>one plus an index into the keys, values, and entries arrays
   *       </ul>
   *   <li>another java.util.Map delegate implementation. In most modern JDKs, normal java.util hash
   *       collections intelligently fall back to a binary search tree if hash table collisions are
   *       detected. Rather than going to all the trouble of reimplementing this ourselves, we
   *       simply switch over to use the JDK implementation wholesale if probable hash flooding is
   *       detected, sacrificing the compactness guarantee in very rare cases in exchange for much
   *       more reliable worst-case behavior.
   *   <li>null, if no entries have yet been added to the map
   * </ul>
   */
  @Nullable private transient Object table;

  /**
   * Contains the logical entries, in the range of [0, size()). The high bits of each int are the
   * part of the smeared hash of the key not covered by the hashtable mask, whereas the low bits are
   * the "next" pointer (pointing to the next entry in the bucket chain), which will always be less
   * than or equal to the hashtable mask.
   *
   * <pre>
   * hash  = aaaaaaaa
   * mask  = 0000ffff
   * next  = 0000bbbb
   * entry = aaaabbbb
   * </pre>
   *
   * <p>The pointers in [size(), entries.length) are all "null" (UNSET).
   */
  @VisibleForTesting transient int @Nullable [] entries;

  /**
   * The keys of the entries in the map, in the range of [0, size()). The keys in [size(),
   * keys.length) are all {@code null}.
   */
  @VisibleForTesting transient Object @Nullable [] keys;

  /**
   * The values of the entries in the map, in the range of [0, size()). The values in [size(),
   * values.length) are all {@code null}.
   */
  @VisibleForTesting transient Object @Nullable [] values;

  /**
   * Keeps track of metadata like the number of hash table bits and modifications of this data
   * structure (to make it possible to throw ConcurrentModificationException in the iterator). Note
   * that we choose not to make this volatile, so we do less of a "best effort" to track such
   * errors, for better performance.
   */
  private transient int metadata;

  /** The number of elements contained in the set. */
  private transient int size;

  /** Constructs a new empty instance of {@code CompactHashMap}. */
  CompactHashMap() {
    init(CompactHashing.DEFAULT_SIZE);
  }

  /**
   * Constructs a new instance of {@code CompactHashMap} with the specified capacity.
   *
   * @param expectedSize the initial capacity of this {@code CompactHashMap}.
   */
  CompactHashMap(int expectedSize) {
    init(expectedSize);
  }

  /** Pseudoconstructor for serialization support. */
  void init(int expectedSize) {
    Preconditions.checkArgument(expectedSize >= 0, "Expected size must be >= 0");

    // Save expectedSize for use in allocArrays()
    this.metadata = Ints.constrainToRange(expectedSize, 1, CompactHashing.MAX_SIZE);
  }

  /** Returns whether arrays need to be allocated. */
  @VisibleForTesting
  boolean needsAllocArrays() {
    return table == null;
  }

  /** Handle lazy allocation of arrays. */
  @CanIgnoreReturnValue
  int allocArrays() {
    Preconditions.checkState(needsAllocArrays(), "Arrays already allocated");

    int expectedSize = metadata;
    int buckets = CompactHashing.tableSize(expectedSize);
    this.table = CompactHashing.createTable(buckets);
    setHashTableMask(buckets - 1);

    this.entries = new int[expectedSize];
    this.keys = new Object[expectedSize];
    this.values = new Object[expectedSize];

    return expectedSize;
  }

  @SuppressWarnings("unchecked")
  @VisibleForTesting
  @Nullable
  Map<K, V> delegateOrNull() {
    if (table instanceof Map) {
      return (Map<K, V>) table;
    }
    return null;
  }

  Map<K, V> createHashFloodingResistantDelegate(int tableSize) {
    return new LinkedHashMap<>(tableSize, 1.0f);
  }

  @SuppressWarnings("unchecked")
  @VisibleForTesting
  @CanIgnoreReturnValue
  Map<K, V> convertToHashFloodingResistantImplementation() {
    Map<K, V> newDelegate = createHashFloodingResistantDelegate(hashTableMask() + 1);
    for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
      newDelegate.put((K) keys[i], (V) values[i]);
    }
    this.table = newDelegate;
    this.entries = null;
    this.keys = null;
    this.values = null;
    incrementModCount();
    return newDelegate;
  }

  /** Stores the hash table mask as the number of bits needed to represent an index. */
  private void setHashTableMask(int mask) {
    int hashTableBits = Integer.SIZE - Integer.numberOfLeadingZeros(mask);
    metadata =
        CompactHashing.maskCombine(metadata, hashTableBits, CompactHashing.HASH_TABLE_BITS_MASK);
  }

  /** Gets the hash table mask using the stored number of hash table bits. */
  private int hashTableMask() {
    return (1 << (metadata & CompactHashing.HASH_TABLE_BITS_MASK)) - 1;
  }

  void incrementModCount() {
    metadata += CompactHashing.MODIFICATION_COUNT_INCREMENT;
  }

  /**
   * Mark an access of the specified entry. Used only in {@code CompactLinkedHashMap} for LRU
   * ordering.
   */
  void accessEntry(int index) {
    // no-op by default
  }

  @CanIgnoreReturnValue
  @Override
  public @Nullable V put(@Nullable K key, @Nullable V value) {
    if (needsAllocArrays()) {
      allocArrays();
    }
    @Nullable Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.put(key, value);
    }
    int[] entries = this.entries;
    Object[] keys = this.keys;
    Object[] values = this.values;

    int newEntryIndex = this.size; // current size, and pointer to the entry to be appended
    int newSize = newEntryIndex + 1;
    int hash = smearedHash(key);
    int mask = hashTableMask();
    int tableIndex = hash & mask;
    int next = CompactHashing.tableGet(table, tableIndex);
    if (next == UNSET) { // uninitialized bucket
      if (newSize > mask) {
        // Resize and add new entry
        mask = resizeTable(mask, CompactHashing.newCapacity(mask), hash, newEntryIndex);
      } else {
        CompactHashing.tableSet(table, tableIndex, newEntryIndex + 1);
      }
    } else {
      int entryIndex;
      int entry;
      int hashPrefix = CompactHashing.getHashPrefix(hash, mask);
      int bucketLength = 0;
      do {
        entryIndex = next - 1;
        entry = entries[entryIndex];
        if (CompactHashing.getHashPrefix(entry, mask) == hashPrefix
            && Objects.equal(key, keys[entryIndex])) {
          @SuppressWarnings("unchecked") // known to be a V
          @Nullable
          V oldValue = (V) values[entryIndex];

          values[entryIndex] = value;
          accessEntry(entryIndex);
          return oldValue;
        }
        next = CompactHashing.getNext(entry, mask);
        bucketLength++;
      } while (next != UNSET);

      if (bucketLength >= MAX_HASH_BUCKET_LENGTH) {
        return convertToHashFloodingResistantImplementation().put(key, value);
      }

      if (newSize > mask) {
        // Resize and add new entry
        mask = resizeTable(mask, CompactHashing.newCapacity(mask), hash, newEntryIndex);
      } else {
        entries[entryIndex] = CompactHashing.maskCombine(entry, newEntryIndex + 1, mask);
      }
    }
    resizeMeMaybe(newSize);
    insertEntry(newEntryIndex, key, value, hash, mask);
    this.size = newSize;
    incrementModCount();
    return null;
  }

  /**
   * Creates a fresh entry with the specified object at the specified position in the entry arrays.
   */
  void insertEntry(int entryIndex, @Nullable K key, @Nullable V value, int hash, int mask) {
    this.entries[entryIndex] = CompactHashing.maskCombine(hash, UNSET, mask);
    this.keys[entryIndex] = key;
    this.values[entryIndex] = value;
  }

  /** Resizes the entries storage if necessary. */
  private void resizeMeMaybe(int newSize) {
    int entriesSize = entries.length;
    if (newSize > entriesSize) {
      // 1.5x but round up to nearest odd (this is optimal for memory consumption on Android)
      int newCapacity =
          Math.min(CompactHashing.MAX_SIZE, (entriesSize + Math.max(1, entriesSize >>> 1)) | 1);
      if (newCapacity != entriesSize) {
        resizeEntries(newCapacity);
      }
    }
  }

  /**
   * Resizes the internal entries array to the specified capacity, which may be greater or less than
   * the current capacity.
   */
  void resizeEntries(int newCapacity) {
    this.entries = Arrays.copyOf(entries, newCapacity);
    this.keys = Arrays.copyOf(keys, newCapacity);
    this.values = Arrays.copyOf(values, newCapacity);
  }

  @CanIgnoreReturnValue
  private int resizeTable(int mask, int newCapacity, int targetHash, int targetEntryIndex) {
    Object newTable = CompactHashing.createTable(newCapacity);
    int newMask = newCapacity - 1;

    if (targetEntryIndex != UNSET) {
      // Add target first; it must be last in the chain because its entry hasn't yet been created
      CompactHashing.tableSet(newTable, targetHash & newMask, targetEntryIndex + 1);
    }

    Object table = this.table;
    int[] entries = this.entries;

    // Loop over current hashtable
    for (int tableIndex = 0; tableIndex <= mask; tableIndex++) {
      int next = CompactHashing.tableGet(table, tableIndex);
      while (next != UNSET) {
        int entryIndex = next - 1;
        int entry = entries[entryIndex];

        // Rebuild hash using entry hashPrefix and tableIndex ("hashSuffix")
        int hash = CompactHashing.getHashPrefix(entry, mask) | tableIndex;

        int newTableIndex = hash & newMask;
        int newNext = CompactHashing.tableGet(newTable, newTableIndex);
        CompactHashing.tableSet(newTable, newTableIndex, next);
        entries[entryIndex] = CompactHashing.maskCombine(hash, newNext, newMask);

        next = CompactHashing.getNext(entry, mask);
      }
    }

    this.table = newTable;
    setHashTableMask(newMask);
    return newMask;
  }

  private int indexOf(@Nullable Object key) {
    if (needsAllocArrays()) {
      return -1;
    }
    int hash = smearedHash(key);
    int mask = hashTableMask();
    int next = CompactHashing.tableGet(table, hash & mask);
    if (next == UNSET) {
      return -1;
    }
    int hashPrefix = CompactHashing.getHashPrefix(hash, mask);
    do {
      int entryIndex = next - 1;
      int entry = entries[entryIndex];
      if (CompactHashing.getHashPrefix(entry, mask) == hashPrefix
          && Objects.equal(key, keys[entryIndex])) {
        return entryIndex;
      }
      next = CompactHashing.getNext(entry, mask);
    } while (next != UNSET);
    return -1;
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    @Nullable Map<K, V> delegate = delegateOrNull();
    return (delegate != null) ? delegate.containsKey(key) : indexOf(key) != -1;
  }

  @SuppressWarnings("unchecked") // known to be a V
  @Override
  public V get(@Nullable Object key) {
    @Nullable Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.get(key);
    }
    int index = indexOf(key);
    if (index == -1) {
      return null;
    }
    accessEntry(index);
    return (V) values[index];
  }

  @CanIgnoreReturnValue
  @SuppressWarnings("unchecked") // known to be a V
  @Override
  public @Nullable V remove(@Nullable Object key) {
    @Nullable Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.remove(key);
    }
    Object oldValue = removeHelper(key);
    return (oldValue == NOT_FOUND) ? null : (V) oldValue;
  }

  private @Nullable Object removeHelper(@Nullable Object key) {
    if (needsAllocArrays()) {
      return NOT_FOUND;
    }
    int mask = hashTableMask();
    int index =
        CompactHashing.remove(
            key, /* value= */ null, mask, table, entries, keys, /* values= */ null);
    if (index == -1) {
      return NOT_FOUND;
    }

    @Nullable Object oldValue = values[index];

    moveLastEntry(index, mask);
    size--;
    incrementModCount();

    return oldValue;
  }

  /**
   * Moves the last entry in the entry array into {@code dstIndex}, and nulls out its old position.
   */
  void moveLastEntry(int dstIndex, int mask) {
    int srcIndex = size() - 1;
    if (dstIndex < srcIndex) {
      // move last entry to deleted spot
      @Nullable Object key = keys[srcIndex];
      keys[dstIndex] = key;
      values[dstIndex] = values[srcIndex];
      keys[srcIndex] = null;
      values[srcIndex] = null;

      // move the last entry to the removed spot, just like we moved the element
      entries[dstIndex] = entries[srcIndex];
      entries[srcIndex] = 0;

      // also need to update whoever's "next" pointer was pointing to the last entry place
      int tableIndex = smearedHash(key) & mask;
      int next = CompactHashing.tableGet(table, tableIndex);
      int srcNext = srcIndex + 1;
      if (next == srcNext) {
        // we need to update the root pointer
        CompactHashing.tableSet(table, tableIndex, dstIndex + 1);
      } else {
        // we need to update a pointer in an entry
        int entryIndex;
        int entry;
        do {
          entryIndex = next - 1;
          entry = entries[entryIndex];
          next = CompactHashing.getNext(entry, mask);
        } while (next != srcNext);
        // here, entries[entryIndex] points to the old entry location; update it
        entries[entryIndex] = CompactHashing.maskCombine(entry, dstIndex + 1, mask);
      }
    } else {
      keys[dstIndex] = null;
      values[dstIndex] = null;
      entries[dstIndex] = 0;
    }
  }

  int firstEntryIndex() {
    return isEmpty() ? -1 : 0;
  }

  int getSuccessor(int entryIndex) {
    return (entryIndex + 1 < size) ? entryIndex + 1 : -1;
  }

  /**
   * Updates the index an iterator is pointing to after a call to remove: returns the index of the
   * entry that should be looked at after a removal on indexRemoved, with indexBeforeRemove as the
   * index that *was* the next entry that would be looked at.
   */
  int adjustAfterRemove(int indexBeforeRemove, @SuppressWarnings("unused") int indexRemoved) {
    return indexBeforeRemove - 1;
  }

  private abstract class Itr<T> implements Iterator<T> {
    int expectedMetadata = metadata;
    int currentIndex = firstEntryIndex();
    int indexToRemove = -1;

    @Override
    public boolean hasNext() {
      return currentIndex >= 0;
    }

    abstract T getOutput(int entry);

    @Override
    public T next() {
      checkForConcurrentModification();
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      indexToRemove = currentIndex;
      T result = getOutput(currentIndex);
      currentIndex = getSuccessor(currentIndex);
      return result;
    }

    @Override
    public void remove() {
      checkForConcurrentModification();
      checkRemove(indexToRemove >= 0);
      incrementExpectedModCount();
      CompactHashMap.this.remove(keys[indexToRemove]);
      currentIndex = adjustAfterRemove(currentIndex, indexToRemove);
      indexToRemove = -1;
    }

    void incrementExpectedModCount() {
      expectedMetadata += CompactHashing.MODIFICATION_COUNT_INCREMENT;
    }

    private void checkForConcurrentModification() {
      if (metadata != expectedMetadata) {
        throw new ConcurrentModificationException();
      }
    }
  }

  @SuppressWarnings("unchecked") // known to be Ks and Vs
  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    checkNotNull(function);
    @Nullable Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      delegate.replaceAll(function);
    } else {
      for (int i = 0; i < size; i++) {
        values[i] = function.apply((K) keys[i], (V) values[i]);
      }
    }
  }

  private transient @MonotonicNonNull Set<K> keySetView;

  @Override
  public Set<K> keySet() {
    return (keySetView == null) ? keySetView = createKeySet() : keySetView;
  }

  Set<K> createKeySet() {
    return new KeySetView();
  }

  @WeakOuter
  class KeySetView extends Maps.KeySet<K, V> {
    KeySetView() {
      super(CompactHashMap.this);
    }

    @Override
    public Object[] toArray() {
      if (needsAllocArrays()) {
        return new Object[0];
      }
      @Nullable Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.keySet().toArray()
          : ObjectArrays.copyAsObjectArray(keys, 0, size);
    }

    @Override
    public <T> T[] toArray(T[] a) {
      if (needsAllocArrays()) {
        if (a.length > 0) {
          a[0] = null;
        }
        return a;
      }
      @Nullable Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.keySet().toArray(a)
          : ObjectArrays.toArrayImpl(keys, 0, size, a);
    }

    @Override
    public boolean remove(@Nullable Object o) {
      @Nullable Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.keySet().remove(o)
          : CompactHashMap.this.removeHelper(o) != NOT_FOUND;
    }

    @Override
    public Iterator<K> iterator() {
      return keySetIterator();
    }

    @Override
    public Spliterator<K> spliterator() {
      if (needsAllocArrays()) {
        return Spliterators.spliterator(new Object[0], Spliterator.DISTINCT | Spliterator.ORDERED);
      }
      @Nullable Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.keySet().spliterator()
          : Spliterators.spliterator(keys, 0, size, Spliterator.DISTINCT | Spliterator.ORDERED);
    }

    @SuppressWarnings("unchecked") // known to be Ks
    @Override
    public void forEach(Consumer<? super K> action) {
      checkNotNull(action);
      @Nullable Map<K, V> delegate = delegateOrNull();
      if (delegate != null) {
        delegate.keySet().forEach(action);
      } else {
        for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
          action.accept((K) keys[i]);
        }
      }
    }
  }

  Iterator<K> keySetIterator() {
    @Nullable Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.keySet().iterator();
    }
    return new Itr<K>() {
      @SuppressWarnings("unchecked") // known to be a K
      @Override
      K getOutput(int entry) {
        return (K) keys[entry];
      }
    };
  }

  @SuppressWarnings("unchecked") // known to be Ks and Vs
  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    checkNotNull(action);
    @Nullable Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      delegate.forEach(action);
    } else {
      for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
        action.accept((K) keys[i], (V) values[i]);
      }
    }
  }

  private transient @MonotonicNonNull Set<Entry<K, V>> entrySetView;

  @Override
  public Set<Entry<K, V>> entrySet() {
    return (entrySetView == null) ? entrySetView = createEntrySet() : entrySetView;
  }

  Set<Entry<K, V>> createEntrySet() {
    return new EntrySetView();
  }

  @WeakOuter
  class EntrySetView extends Maps.EntrySet<K, V> {
    @Override
    Map<K, V> map() {
      return CompactHashMap.this;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return entrySetIterator();
    }

    @Override
    public Spliterator<Entry<K, V>> spliterator() {
      @Nullable Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.entrySet().spliterator()
          : CollectSpliterators.indexed(
              size, Spliterator.DISTINCT | Spliterator.ORDERED, MapEntry::new);
    }

    @Override
    public boolean contains(@Nullable Object o) {
      @Nullable Map<K, V> delegate = delegateOrNull();
      if (delegate != null) {
        return delegate.entrySet().contains(o);
      } else if (o instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) o;
        int index = indexOf(entry.getKey());
        return index != -1 && Objects.equal(values[index], entry.getValue());
      }
      return false;
    }

    @Override
    public boolean remove(@Nullable Object o) {
      @Nullable Map<K, V> delegate = delegateOrNull();
      if (delegate != null) {
        return delegate.entrySet().remove(o);
      } else if (o instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) o;
        if (needsAllocArrays()) {
          return false;
        }
        int mask = hashTableMask();
        int index =
            CompactHashing.remove(
                entry.getKey(), entry.getValue(), mask, table, entries, keys, values);
        if (index == -1) {
          return false;
        }

        moveLastEntry(index, mask);
        size--;
        incrementModCount();

        return true;
      }
      return false;
    }
  }

  Iterator<Entry<K, V>> entrySetIterator() {
    @Nullable Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.entrySet().iterator();
    }
    return new Itr<Entry<K, V>>() {
      @Override
      Entry<K, V> getOutput(int entry) {
        return new MapEntry(entry);
      }
    };
  }

  final class MapEntry extends AbstractMapEntry<K, V> {
    private final @Nullable K key;

    private int lastKnownIndex;

    @SuppressWarnings("unchecked") // known to be a K
    MapEntry(int index) {
      this.key = (K) keys[index];
      this.lastKnownIndex = index;
    }

    @Nullable
    @Override
    public K getKey() {
      return key;
    }

    private void updateLastKnownIndex() {
      if (lastKnownIndex == -1
          || lastKnownIndex >= size()
          || !Objects.equal(key, keys[lastKnownIndex])) {
        lastKnownIndex = indexOf(key);
      }
    }

    @SuppressWarnings("unchecked") // known to be a V
    @Nullable
    @Override
    public V getValue() {
      @Nullable Map<K, V> delegate = delegateOrNull();
      if (delegate != null) {
        return delegate.get(key);
      }
      updateLastKnownIndex();
      return (lastKnownIndex == -1) ? null : (V) values[lastKnownIndex];
    }

    @SuppressWarnings("unchecked") // known to be a V
    @Override
    public V setValue(V value) {
      @Nullable Map<K, V> delegate = delegateOrNull();
      if (delegate != null) {
        return delegate.put(key, value);
      }
      updateLastKnownIndex();
      if (lastKnownIndex == -1) {
        put(key, value);
        return null;
      } else {
        V old = (V) values[lastKnownIndex];
        values[lastKnownIndex] = value;
        return old;
      }
    }
  }

  @Override
  public int size() {
    @Nullable Map<K, V> delegate = delegateOrNull();
    return (delegate != null) ? delegate.size() : size;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    @Nullable Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.containsValue(value);
    }
    for (int i = 0; i < size; i++) {
      if (Objects.equal(value, values[i])) {
        return true;
      }
    }
    return false;
  }

  private transient @MonotonicNonNull Collection<V> valuesView;

  @Override
  public Collection<V> values() {
    return (valuesView == null) ? valuesView = createValues() : valuesView;
  }

  Collection<V> createValues() {
    return new ValuesView();
  }

  @WeakOuter
  class ValuesView extends Maps.Values<K, V> {
    ValuesView() {
      super(CompactHashMap.this);
    }

    @Override
    public Iterator<V> iterator() {
      return valuesIterator();
    }

    @SuppressWarnings("unchecked") // known to be Vs
    @Override
    public void forEach(Consumer<? super V> action) {
      checkNotNull(action);
      @Nullable Map<K, V> delegate = delegateOrNull();
      if (delegate != null) {
        delegate.values().forEach(action);
      } else {
        for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
          action.accept((V) values[i]);
        }
      }
    }

    @Override
    public Spliterator<V> spliterator() {
      if (needsAllocArrays()) {
        return Spliterators.spliterator(new Object[0], Spliterator.ORDERED);
      }
      @Nullable Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.values().spliterator()
          : Spliterators.spliterator(values, 0, size, Spliterator.ORDERED);
    }

    @Override
    public Object[] toArray() {
      if (needsAllocArrays()) {
        return new Object[0];
      }
      @Nullable Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.values().toArray()
          : ObjectArrays.copyAsObjectArray(values, 0, size);
    }

    @Override
    public <T> T[] toArray(T[] a) {
      if (needsAllocArrays()) {
        if (a.length > 0) {
          a[0] = null;
        }
        return a;
      }
      @Nullable Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.values().toArray(a)
          : ObjectArrays.toArrayImpl(values, 0, size, a);
    }
  }

  Iterator<V> valuesIterator() {
    @Nullable Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.values().iterator();
    }
    return new Itr<V>() {
      @SuppressWarnings("unchecked") // known to be a V
      @Override
      V getOutput(int entry) {
        return (V) values[entry];
      }
    };
  }

  /**
   * Ensures that this {@code CompactHashMap} has the smallest representation in memory, given its
   * current size.
   */
  public void trimToSize() {
    if (needsAllocArrays()) {
      return;
    }
    @Nullable Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      Map<K, V> newDelegate = createHashFloodingResistantDelegate(size());
      newDelegate.putAll(delegate);
      this.table = newDelegate;
      return;
    }
    int size = this.size;
    if (size < entries.length) {
      resizeEntries(size);
    }
    int minimumTableSize = CompactHashing.tableSize(size);
    int mask = hashTableMask();
    if (minimumTableSize < mask) { // smaller table size will always be less than current mask
      resizeTable(mask, minimumTableSize, UNSET, UNSET);
    }
  }

  @Override
  public void clear() {
    if (needsAllocArrays()) {
      return;
    }
    incrementModCount();
    @Nullable Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      metadata =
          Ints.constrainToRange(size(), CompactHashing.DEFAULT_SIZE, CompactHashing.MAX_SIZE);
      table = null;
      size = 0;
    } else {
      Arrays.fill(keys, 0, size, null);
      Arrays.fill(values, 0, size, null);
      CompactHashing.tableClear(table);
      Arrays.fill(entries, 0, size, 0);
      this.size = 0;
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeInt(size());
    Iterator<Entry<K, V>> entryIterator = entrySetIterator();
    while (entryIterator.hasNext()) {
      Entry<K, V> e = entryIterator.next();
      stream.writeObject(e.getKey());
      stream.writeObject(e.getValue());
    }
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    int elementCount = stream.readInt();
    if (elementCount < 0) {
      throw new InvalidObjectException("Invalid size: " + elementCount);
    }
    init(elementCount);
    for (int i = 0; i < elementCount; i++) {
      K key = (K) stream.readObject();
      V value = (V) stream.readObject();
      put(key, value);
    }
  }
}
