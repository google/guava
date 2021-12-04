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
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;
import static com.google.common.collect.NullnessCasts.unsafeNull;
import static java.util.Objects.requireNonNull;

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
import javax.annotation.CheckForNull;
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
@ElementTypesAreNonnullByDefault
class CompactHashMap<K extends @Nullable Object, V extends @Nullable Object>
    extends AbstractMap<K, V> implements Serializable {
  /*
   * TODO: Make this a drop-in replacement for j.u. versions, actually drop them in, and test the
   * world. Figure out what sort of space-time tradeoff we're actually going to get here with the
   * *Map variants. This class is particularly hard to benchmark, because the benefit is not only in
   * less allocation, but also having the GC do less work to scan the heap because of fewer
   * references, which is particularly hard to quantify.
   */

  /** Creates an empty {@code CompactHashMap} instance. */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      CompactHashMap<K, V> create() {
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
  public static <K extends @Nullable Object, V extends @Nullable Object>
      CompactHashMap<K, V> createWithExpectedSize(int expectedSize) {
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

  // The way the `table`, `entries`, `keys`, and `values` arrays work together is as follows.
  //
  // The `table` array always has a size that is a power of 2. The hashcode of a key in the map
  // is masked in order to correspond to the current table size. For example, if the table size
  // is 128 then the mask is 127 == 0x7f, keeping the bottom 7 bits of the hash value.
  // If a key hashes to 0x89abcdef the mask reduces it to 0x89abcdef & 0x7f == 0x6f. We'll call this
  // the "short hash".
  //
  // The `keys`, `values`, and `entries` arrays always have the same size as each other. They can be
  // seen as fields of an imaginary `Entry` object like this:
  //
  // class Entry {
  //    int hash;
  //    Entry next;
  //    K key;
  //    V value;
  // }
  //
  // The imaginary `hash` and `next` values are combined into a single `int` value in the `entries`
  // array. The top bits of this value are the remaining bits of the hash value that were not used
  // in the short hash. We saw that a mask of 0x7f would keep the 7-bit value 0x6f from a full
  // hashcode of 0x89abcdef. The imaginary `hash` value would then be the remaining top 25 bits,
  // 0x89abcd80. To this is added (or'd) the `next` value, which is an index within `entries`
  // (and therefore within `keys` and `values`) of another entry that has the same short hash
  // value. In our example, it would be another entry for a key whose short hash is also 0x6f.
  //
  // Essentially, then, `table[h]` gives us the start of a linked list in `entries`, where every
  // element of the list has the short hash value h.
  //
  // A wrinkle here is that the value 0 (called UNSET in the code) is used as the equivalent of a
  // null pointer. If `table[h] == 0` that means there are no keys in the map whose short hash is h.
  // If the `next` bits in `entries[i]` are 0 that means there are no further entries for the given
  // short hash. But 0 is also a valid index in `entries`, so we add 1 to these indices before
  // putting them in `table` or in `next` bits, and subtract 1 again when we need an index value.
  //
  // The elements of `keys`, `values`, and `entries` are added sequentially, so that elements 0 to
  // `size() - 1` are used and remaining elements are not. This makes iteration straightforward.
  // Removing an entry generally involves moving the last element of each array to where the removed
  // entry was, and adjusting index links accordingly.

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
  @CheckForNull private transient Object table;

  /**
   * Contains the logical entries, in the range of [0, size()). The high bits of each int are the
   * part of the smeared hash of the key not covered by the hashtable mask, whereas the low bits are
   * the "next" pointer (pointing to the next entry in the bucket chain), which will always be less
   * than or equal to the hashtable mask.
   *
   * <pre>
   * hash  = aaaaaaaa
   * mask  = 00000fff
   * next  = 00000bbb
   * entry = aaaaabbb
   * </pre>
   *
   * <p>The pointers in [size(), entries.length) are all "null" (UNSET).
   */
  @VisibleForTesting @CheckForNull transient int[] entries;

  /**
   * The keys of the entries in the map, in the range of [0, size()). The keys in [size(),
   * keys.length) are all {@code null}.
   */
  @VisibleForTesting @CheckForNull transient @Nullable Object[] keys;

  /**
   * The values of the entries in the map, in the range of [0, size()). The values in [size(),
   * values.length) are all {@code null}.
   */
  @VisibleForTesting @CheckForNull transient @Nullable Object[] values;

  /**
   * Keeps track of metadata like the number of hash table bits and modifications of this data
   * structure (to make it possible to throw ConcurrentModificationException in the iterator). Note
   * that we choose not to make this volatile, so we do less of a "best effort" to track such
   * errors, for better performance.
   *
   * <p>For a new instance, where the arrays above have not yet been allocated, the value of {@code
   * metadata} is the size that the arrays should be allocated with. Once the arrays have been
   * allocated, the value of {@code metadata} combines the number of bits in the "short hash", in
   * its bottom {@value CompactHashing#HASH_TABLE_BITS_MAX_BITS} bits, with a modification count in
   * the remaining bits that is used to detect concurrent modification during iteration.
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
  @CheckForNull
  Map<K, V> delegateOrNull() {
    if (table instanceof Map) {
      return (Map<K, V>) table;
    }
    return null;
  }

  Map<K, V> createHashFloodingResistantDelegate(int tableSize) {
    return new LinkedHashMap<>(tableSize, 1.0f);
  }

  @VisibleForTesting
  @CanIgnoreReturnValue
  Map<K, V> convertToHashFloodingResistantImplementation() {
    Map<K, V> newDelegate = createHashFloodingResistantDelegate(hashTableMask() + 1);
    for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
      newDelegate.put(key(i), value(i));
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
  @CheckForNull
  public V put(@ParametricNullness K key, @ParametricNullness V value) {
    if (needsAllocArrays()) {
      allocArrays();
    }
    Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.put(key, value);
    }
    int[] entries = requireEntries();
    @Nullable Object[] keys = requireKeys();
    @Nullable Object[] values = requireValues();

    int newEntryIndex = this.size; // current size, and pointer to the entry to be appended
    int newSize = newEntryIndex + 1;
    int hash = smearedHash(key);
    int mask = hashTableMask();
    int tableIndex = hash & mask;
    int next = CompactHashing.tableGet(requireTable(), tableIndex);
    if (next == UNSET) { // uninitialized bucket
      if (newSize > mask) {
        // Resize and add new entry
        mask = resizeTable(mask, CompactHashing.newCapacity(mask), hash, newEntryIndex);
      } else {
        CompactHashing.tableSet(requireTable(), tableIndex, newEntryIndex + 1);
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
  void insertEntry(
      int entryIndex, @ParametricNullness K key, @ParametricNullness V value, int hash, int mask) {
    this.setEntry(entryIndex, CompactHashing.maskCombine(hash, UNSET, mask));
    this.setKey(entryIndex, key);
    this.setValue(entryIndex, value);
  }

  /** Resizes the entries storage if necessary. */
  private void resizeMeMaybe(int newSize) {
    int entriesSize = requireEntries().length;
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
    this.entries = Arrays.copyOf(requireEntries(), newCapacity);
    this.keys = Arrays.copyOf(requireKeys(), newCapacity);
    this.values = Arrays.copyOf(requireValues(), newCapacity);
  }

  @CanIgnoreReturnValue
  private int resizeTable(int oldMask, int newCapacity, int targetHash, int targetEntryIndex) {
    Object newTable = CompactHashing.createTable(newCapacity);
    int newMask = newCapacity - 1;

    if (targetEntryIndex != UNSET) {
      // Add target first; it must be last in the chain because its entry hasn't yet been created
      CompactHashing.tableSet(newTable, targetHash & newMask, targetEntryIndex + 1);
    }

    Object oldTable = requireTable();
    int[] entries = requireEntries();

    // Loop over `oldTable` to construct its replacement, ``newTable`. The entries do not move, so
    // the `keys` and `values` arrays do not need to change. But because the "short hash" now has a
    // different number of bits, we must rewrite each element of `entries` so that its contribution
    // to the full hashcode reflects the change, and so that its `next` link corresponds to the new
    // linked list of entries with the new short hash.
    for (int oldTableIndex = 0; oldTableIndex <= oldMask; oldTableIndex++) {
      int oldNext = CompactHashing.tableGet(oldTable, oldTableIndex);
      // Each element of `oldTable` is the head of a (possibly empty) linked list of elements in
      // `entries`. The `oldNext` loop is going to traverse that linked list.
      // We need to rewrite the `next` link of each of the elements so that it is in the appropriate
      // linked list starting from `newTable`. In general, each element from the old linked list
      // belongs to a different linked list from `newTable`. We insert each element in turn at the
      // head of its appropriate `newTable` linked list.
      while (oldNext != UNSET) {
        int entryIndex = oldNext - 1;
        int oldEntry = entries[entryIndex];

        // Rebuild the full 32-bit hash using entry hashPrefix and oldTableIndex ("hashSuffix").
        int hash = CompactHashing.getHashPrefix(oldEntry, oldMask) | oldTableIndex;

        int newTableIndex = hash & newMask;
        int newNext = CompactHashing.tableGet(newTable, newTableIndex);
        CompactHashing.tableSet(newTable, newTableIndex, oldNext);
        entries[entryIndex] = CompactHashing.maskCombine(hash, newNext, newMask);

        oldNext = CompactHashing.getNext(oldEntry, oldMask);
      }
    }

    this.table = newTable;
    setHashTableMask(newMask);
    return newMask;
  }

  private int indexOf(@CheckForNull Object key) {
    if (needsAllocArrays()) {
      return -1;
    }
    int hash = smearedHash(key);
    int mask = hashTableMask();
    int next = CompactHashing.tableGet(requireTable(), hash & mask);
    if (next == UNSET) {
      return -1;
    }
    int hashPrefix = CompactHashing.getHashPrefix(hash, mask);
    do {
      int entryIndex = next - 1;
      int entry = entry(entryIndex);
      if (CompactHashing.getHashPrefix(entry, mask) == hashPrefix
          && Objects.equal(key, key(entryIndex))) {
        return entryIndex;
      }
      next = CompactHashing.getNext(entry, mask);
    } while (next != UNSET);
    return -1;
  }

  @Override
  public boolean containsKey(@CheckForNull Object key) {
    Map<K, V> delegate = delegateOrNull();
    return (delegate != null) ? delegate.containsKey(key) : indexOf(key) != -1;
  }

  @Override
  @CheckForNull
  public V get(@CheckForNull Object key) {
    Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.get(key);
    }
    int index = indexOf(key);
    if (index == -1) {
      return null;
    }
    accessEntry(index);
    return value(index);
  }

  @CanIgnoreReturnValue
  @SuppressWarnings("unchecked") // known to be a V
  @Override
  @CheckForNull
  public V remove(@CheckForNull Object key) {
    Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.remove(key);
    }
    Object oldValue = removeHelper(key);
    return (oldValue == NOT_FOUND) ? null : (V) oldValue;
  }

  private @Nullable Object removeHelper(@CheckForNull Object key) {
    if (needsAllocArrays()) {
      return NOT_FOUND;
    }
    int mask = hashTableMask();
    int index =
        CompactHashing.remove(
            key,
            /* value= */ null,
            mask,
            requireTable(),
            requireEntries(),
            requireKeys(),
            /* values= */ null);
    if (index == -1) {
      return NOT_FOUND;
    }

    Object oldValue = value(index);

    moveLastEntry(index, mask);
    size--;
    incrementModCount();

    return oldValue;
  }

  /**
   * Moves the last entry in the entry array into {@code dstIndex}, and nulls out its old position.
   */
  void moveLastEntry(int dstIndex, int mask) {
    Object table = requireTable();
    int[] entries = requireEntries();
    @Nullable Object[] keys = requireKeys();
    @Nullable Object[] values = requireValues();
    int srcIndex = size() - 1;
    if (dstIndex < srcIndex) {
      // move last entry to deleted spot
      Object key = keys[srcIndex];
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

  private abstract class Itr<T extends @Nullable Object> implements Iterator<T> {
    int expectedMetadata = metadata;
    int currentIndex = firstEntryIndex();
    int indexToRemove = -1;

    @Override
    public boolean hasNext() {
      return currentIndex >= 0;
    }

    @ParametricNullness
    abstract T getOutput(int entry);

    @Override
    @ParametricNullness
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
      CompactHashMap.this.remove(key(indexToRemove));
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

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    checkNotNull(function);
    Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      delegate.replaceAll(function);
    } else {
      for (int i = 0; i < size; i++) {
        setValue(i, function.apply(key(i), value(i)));
      }
    }
  }

  @CheckForNull private transient Set<K> keySetView;

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
    public @Nullable Object[] toArray() {
      if (needsAllocArrays()) {
        return new Object[0];
      }
      Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.keySet().toArray()
          : ObjectArrays.copyAsObjectArray(requireKeys(), 0, size);
    }

    @Override
    @SuppressWarnings("nullness") // b/192354773 in our checker affects toArray declarations
    public <T extends @Nullable Object> T[] toArray(T[] a) {
      if (needsAllocArrays()) {
        if (a.length > 0) {
          @Nullable Object[] unsoundlyCovariantArray = a;
          unsoundlyCovariantArray[0] = null;
        }
        return a;
      }
      Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.keySet().toArray(a)
          : ObjectArrays.toArrayImpl(requireKeys(), 0, size, a);
    }

    @Override
    public boolean remove(@CheckForNull Object o) {
      Map<K, V> delegate = delegateOrNull();
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
      Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.keySet().spliterator()
          : Spliterators.spliterator(
              requireKeys(), 0, size, Spliterator.DISTINCT | Spliterator.ORDERED);
    }

    @Override
    public void forEach(Consumer<? super K> action) {
      checkNotNull(action);
      Map<K, V> delegate = delegateOrNull();
      if (delegate != null) {
        delegate.keySet().forEach(action);
      } else {
        for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
          action.accept(key(i));
        }
      }
    }
  }

  Iterator<K> keySetIterator() {
    Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.keySet().iterator();
    }
    return new Itr<K>() {
      @Override
      @ParametricNullness
      K getOutput(int entry) {
        return key(entry);
      }
    };
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    checkNotNull(action);
    Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      delegate.forEach(action);
    } else {
      for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
        action.accept(key(i), value(i));
      }
    }
  }

  @CheckForNull private transient Set<Entry<K, V>> entrySetView;

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
      Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.entrySet().spliterator()
          : CollectSpliterators.indexed(
              size, Spliterator.DISTINCT | Spliterator.ORDERED, MapEntry::new);
    }

    @Override
    public boolean contains(@CheckForNull Object o) {
      Map<K, V> delegate = delegateOrNull();
      if (delegate != null) {
        return delegate.entrySet().contains(o);
      } else if (o instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) o;
        int index = indexOf(entry.getKey());
        return index != -1 && Objects.equal(value(index), entry.getValue());
      }
      return false;
    }

    @Override
    public boolean remove(@CheckForNull Object o) {
      Map<K, V> delegate = delegateOrNull();
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
                entry.getKey(),
                entry.getValue(),
                mask,
                requireTable(),
                requireEntries(),
                requireKeys(),
                requireValues());
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
    Map<K, V> delegate = delegateOrNull();
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
    @ParametricNullness private final K key;

    private int lastKnownIndex;

    MapEntry(int index) {
      this.key = key(index);
      this.lastKnownIndex = index;
    }

    @Override
    @ParametricNullness
    public K getKey() {
      return key;
    }

    private void updateLastKnownIndex() {
      if (lastKnownIndex == -1
          || lastKnownIndex >= size()
          || !Objects.equal(key, key(lastKnownIndex))) {
        lastKnownIndex = indexOf(key);
      }
    }

    @Override
    @ParametricNullness
    public V getValue() {
      Map<K, V> delegate = delegateOrNull();
      if (delegate != null) {
        /*
         * The cast is safe because the entry is present in the map. Or, if it has been removed by a
         * concurrent modification, behavior is undefined.
         */
        return uncheckedCastNullableTToT(delegate.get(key));
      }
      updateLastKnownIndex();
      /*
       * If the entry has been removed from the map, we return null, even though that might not be a
       * valid value. That's the best we can do, short of holding a reference to the most recently
       * seen value. And while we *could* do that, we aren't required to: Map.Entry explicitly says
       * that behavior is undefined when the backing map is modified through another API. (It even
       * permits us to throw IllegalStateException. Maybe we should have done that, but we probably
       * shouldn't change now for fear of breaking people.)
       */
      return (lastKnownIndex == -1) ? unsafeNull() : value(lastKnownIndex);
    }

    @Override
    @ParametricNullness
    public V setValue(@ParametricNullness V value) {
      Map<K, V> delegate = delegateOrNull();
      if (delegate != null) {
        return uncheckedCastNullableTToT(delegate.put(key, value)); // See discussion in getValue().
      }
      updateLastKnownIndex();
      if (lastKnownIndex == -1) {
        put(key, value);
        return unsafeNull(); // See discussion in getValue().
      } else {
        V old = value(lastKnownIndex);
        CompactHashMap.this.setValue(lastKnownIndex, value);
        return old;
      }
    }
  }

  @Override
  public int size() {
    Map<K, V> delegate = delegateOrNull();
    return (delegate != null) ? delegate.size() : size;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsValue(@CheckForNull Object value) {
    Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.containsValue(value);
    }
    for (int i = 0; i < size; i++) {
      if (Objects.equal(value, value(i))) {
        return true;
      }
    }
    return false;
  }

  @CheckForNull private transient Collection<V> valuesView;

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

    @Override
    public void forEach(Consumer<? super V> action) {
      checkNotNull(action);
      Map<K, V> delegate = delegateOrNull();
      if (delegate != null) {
        delegate.values().forEach(action);
      } else {
        for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
          action.accept(value(i));
        }
      }
    }

    @Override
    public Spliterator<V> spliterator() {
      if (needsAllocArrays()) {
        return Spliterators.spliterator(new Object[0], Spliterator.ORDERED);
      }
      Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.values().spliterator()
          : Spliterators.spliterator(requireValues(), 0, size, Spliterator.ORDERED);
    }

    @Override
    public @Nullable Object[] toArray() {
      if (needsAllocArrays()) {
        return new Object[0];
      }
      Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.values().toArray()
          : ObjectArrays.copyAsObjectArray(requireValues(), 0, size);
    }

    @Override
    @SuppressWarnings("nullness") // b/192354773 in our checker affects toArray declarations
    public <T extends @Nullable Object> T[] toArray(T[] a) {
      if (needsAllocArrays()) {
        if (a.length > 0) {
          @Nullable Object[] unsoundlyCovariantArray = a;
          unsoundlyCovariantArray[0] = null;
        }
        return a;
      }
      Map<K, V> delegate = delegateOrNull();
      return (delegate != null)
          ? delegate.values().toArray(a)
          : ObjectArrays.toArrayImpl(requireValues(), 0, size, a);
    }
  }

  Iterator<V> valuesIterator() {
    Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.values().iterator();
    }
    return new Itr<V>() {
      @Override
      @ParametricNullness
      V getOutput(int entry) {
        return value(entry);
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
    Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      Map<K, V> newDelegate = createHashFloodingResistantDelegate(size());
      newDelegate.putAll(delegate);
      this.table = newDelegate;
      return;
    }
    int size = this.size;
    if (size < requireEntries().length) {
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
    Map<K, V> delegate = delegateOrNull();
    if (delegate != null) {
      metadata =
          Ints.constrainToRange(size(), CompactHashing.DEFAULT_SIZE, CompactHashing.MAX_SIZE);
      delegate.clear(); // invalidate any iterators left over!
      table = null;
      size = 0;
    } else {
      Arrays.fill(requireKeys(), 0, size, null);
      Arrays.fill(requireValues(), 0, size, null);
      CompactHashing.tableClear(requireTable());
      Arrays.fill(requireEntries(), 0, size, 0);
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

  /*
   * The following methods are safe to call as long as both of the following hold:
   *
   * - allocArrays() has been called. Callers can confirm this by checking needsAllocArrays().
   *
   * - The map has not switched to delegating to a java.util implementation to mitigate hash
   *   flooding. Callers can confirm this by null-checking delegateOrNull().
   *
   * In an ideal world, we would document why we know those things are true every time we call these
   * methods. But that is a bit too painful....
   */

  private Object requireTable() {
    return requireNonNull(table);
  }

  private int[] requireEntries() {
    return requireNonNull(entries);
  }

  private @Nullable Object[] requireKeys() {
    return requireNonNull(keys);
  }

  private @Nullable Object[] requireValues() {
    return requireNonNull(values);
  }

  /*
   * The following methods are safe to call as long as the conditions in the *previous* comment are
   * met *and* the index is less than size().
   *
   * (The above explains when these methods are safe from a `nullness` perspective. From an
   * `unchecked` perspective, they're safe because we put only K/V elements into each array.)
   */

  @SuppressWarnings("unchecked")
  private K key(int i) {
    return (K) requireKeys()[i];
  }

  @SuppressWarnings("unchecked")
  private V value(int i) {
    return (V) requireValues()[i];
  }

  private int entry(int i) {
    return requireEntries()[i];
  }

  private void setKey(int i, K key) {
    requireKeys()[i] = key;
  }

  private void setValue(int i, V value) {
    requireValues()[i] = value;
  }

  private void setEntry(int i, int value) {
    requireEntries()[i] = value;
  }
}
