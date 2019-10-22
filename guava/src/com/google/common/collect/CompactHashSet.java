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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * CompactHashSet is an implementation of a Set. All optional operations (adding and removing) are
 * supported. The elements can be any objects.
 *
 * <p>{@code contains(x)}, {@code add(x)} and {@code remove(x)}, are all (expected and amortized)
 * constant time operations. Expected in the hashtable sense (depends on the hash function doing a
 * good job of distributing the elements to the buckets to a distribution not far from uniform), and
 * amortized since some operations can trigger a hash table resize.
 *
 * <p>Unlike {@code java.util.HashSet}, iteration is only proportional to the actual {@code size()},
 * which is optimal, and <i>not</i> the size of the internal hashtable, which could be much larger
 * than {@code size()}. Furthermore, this structure only depends on a fixed number of arrays; {@code
 * add(x)} operations <i>do not</i> create objects for the garbage collector to deal with, and for
 * every element added, the garbage collector will have to traverse {@code 1.5} references on
 * average, in the marking phase, not {@code 5.0} as in {@code java.util.HashSet}.
 *
 * <p>If there are no removals, then {@link #iterator iteration} order is the same as insertion
 * order. Any removal invalidates any ordering guarantees.
 *
 * <p>This class should not be assumed to be universally superior to {@code java.util.HashSet}.
 * Generally speaking, this class reduces object allocation and memory consumption at the price of
 * moderately increased constant factors of CPU. Only use this class when there is a specific reason
 * to prioritize memory over CPU.
 *
 * @author Dimitris Andreou
 * @author Jon Noack
 */
@GwtIncompatible // not worth using in GWT for now
class CompactHashSet<E> extends AbstractSet<E> implements Serializable {
  // TODO(user): cache all field accesses in local vars

  /** Creates an empty {@code CompactHashSet} instance. */
  public static <E> CompactHashSet<E> create() {
    return new CompactHashSet<>();
  }

  /**
   * Creates a <i>mutable</i> {@code CompactHashSet} instance containing the elements of the given
   * collection in unspecified order.
   *
   * @param collection the elements that the set should contain
   * @return a new {@code CompactHashSet} containing those elements (minus duplicates)
   */
  public static <E> CompactHashSet<E> create(Collection<? extends E> collection) {
    CompactHashSet<E> set = createWithExpectedSize(collection.size());
    set.addAll(collection);
    return set;
  }

  /**
   * Creates a <i>mutable</i> {@code CompactHashSet} instance containing the given elements in
   * unspecified order.
   *
   * @param elements the elements that the set should contain
   * @return a new {@code CompactHashSet} containing those elements (minus duplicates)
   */
  @SafeVarargs
  public static <E> CompactHashSet<E> create(E... elements) {
    CompactHashSet<E> set = createWithExpectedSize(elements.length);
    Collections.addAll(set, elements);
    return set;
  }

  /**
   * Creates a {@code CompactHashSet} instance, with a high enough "initial capacity" that it
   * <i>should</i> hold {@code expectedSize} elements without growth.
   *
   * @param expectedSize the number of elements you expect to add to the returned set
   * @return a new, empty {@code CompactHashSet} with enough capacity to hold {@code expectedSize}
   *     elements without resizing
   * @throws IllegalArgumentException if {@code expectedSize} is negative
   */
  public static <E> CompactHashSet<E> createWithExpectedSize(int expectedSize) {
    return new CompactHashSet<>(expectedSize);
  }

  /**
   * The hashtable. Its values are indexes to the elements and entries arrays.
   *
   * <p>Currently, the UNSET value means "null pointer", and any positive value x is the actual
   * index + 1.
   *
   * <p>Its size must be a power of two.
   */
  @MonotonicNonNull private transient Object table;

  /**
   * Contains the logical entries, in the range of [0, size()). The high bits of each int are the
   * part of the smeared hash of the element not covered by the hashtable mask, whereas the low bits
   * are the "next" pointer (pointing to the next entry in the bucket chain), which will always be
   * less than or equal to the hashtable mask.
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
  private transient int @MonotonicNonNull [] entries;

  /**
   * The elements contained in the set, in the range of [0, size()). The elements in [size(),
   * elements.length) are all {@code null}.
   */
  @VisibleForTesting transient Object @MonotonicNonNull [] elements;

  /**
   * Keeps track of metadata like the number of hash table bits and modifications of this data
   * structure (to make it possible to throw ConcurrentModificationException in the iterator). Note
   * that we choose not to make this volatile, so we do less of a "best effort" to track such
   * errors, for better performance.
   */
  private transient int metadata;

  /** The number of elements contained in the set. */
  private transient int size;

  /** Constructs a new empty instance of {@code CompactHashSet}. */
  CompactHashSet() {
    init(CompactHashing.DEFAULT_SIZE);
  }

  /**
   * Constructs a new instance of {@code CompactHashSet} with the specified capacity.
   *
   * @param expectedSize the initial capacity of this {@code CompactHashSet}.
   */
  CompactHashSet(int expectedSize) {
    init(expectedSize);
  }

  /** Pseudoconstructor for serialization support. */
  void init(int expectedSize) {
    Preconditions.checkArgument(expectedSize >= 0, "Expected size must be >= 0");

    // Save expectedSize for use in allocArrays()
    this.metadata = Math.max(1, Math.min(CompactHashing.MAX_SIZE, expectedSize));
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
    this.elements = new Object[expectedSize];

    return expectedSize;
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

  @CanIgnoreReturnValue
  @Override
  public boolean add(@Nullable E object) {
    if (needsAllocArrays()) {
      allocArrays();
    }
    int[] entries = this.entries;
    Object[] elements = this.elements;

    int newEntryIndex = this.size; // current size, and pointer to the entry to be appended
    int newSize = newEntryIndex + 1;
    int hash = smearedHash(object);
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
      do {
        entryIndex = next - 1;
        entry = entries[entryIndex];
        if (CompactHashing.getHashPrefix(entry, mask) == hashPrefix
            && Objects.equal(object, elements[entryIndex])) {
          return false;
        }
        next = CompactHashing.getNext(entry, mask);
      } while (next != UNSET);
      if (newSize > mask) {
        // Resize and add new entry
        mask = resizeTable(mask, CompactHashing.newCapacity(mask), hash, newEntryIndex);
      } else {
        entries[entryIndex] = CompactHashing.maskCombine(entry, newEntryIndex + 1, mask);
      }
    }
    resizeMeMaybe(newSize);
    insertEntry(newEntryIndex, object, hash, mask);
    this.size = newSize;
    incrementModCount();
    return true;
  }

  /**
   * Creates a fresh entry with the specified object at the specified position in the entry arrays.
   */
  void insertEntry(int entryIndex, @Nullable E object, int hash, int mask) {
    this.entries[entryIndex] = CompactHashing.maskCombine(hash, UNSET, mask);
    this.elements[entryIndex] = object;
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
    this.elements = Arrays.copyOf(elements, newCapacity);
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

  @Override
  public boolean contains(@Nullable Object object) {
    if (needsAllocArrays()) {
      return false;
    }
    int hash = smearedHash(object);
    int mask = hashTableMask();
    int next = CompactHashing.tableGet(table, hash & mask);
    if (next == UNSET) {
      return false;
    }
    int hashPrefix = CompactHashing.getHashPrefix(hash, mask);
    do {
      int entryIndex = next - 1;
      int entry = entries[entryIndex];
      if (CompactHashing.getHashPrefix(entry, mask) == hashPrefix
          && Objects.equal(object, elements[entryIndex])) {
        return true;
      }
      next = CompactHashing.getNext(entry, mask);
    } while (next != UNSET);
    return false;
  }

  @CanIgnoreReturnValue
  @Override
  public boolean remove(@Nullable Object object) {
    if (needsAllocArrays()) {
      return false;
    }
    int mask = hashTableMask();
    int index =
        CompactHashing.remove(
            object, /* value= */ null, mask, table, entries, elements, /* values= */ null);
    if (index == -1) {
      return false;
    }

    moveLastEntry(index, mask);
    size--;
    incrementModCount();

    return true;
  }

  /**
   * Moves the last entry in the entry array into {@code dstIndex}, and nulls out its old position.
   */
  void moveLastEntry(int dstIndex, int mask) {
    int srcIndex = size() - 1;
    if (dstIndex < srcIndex) {
      // move last entry to deleted spot
      @Nullable Object object = elements[srcIndex];
      elements[dstIndex] = object;
      elements[srcIndex] = null;

      // move the last entry to the removed spot, just like we moved the element
      entries[dstIndex] = entries[srcIndex];
      entries[srcIndex] = 0;

      // also need to update whoever's "next" pointer was pointing to the last entry place
      int tableIndex = smearedHash(object) & mask;
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
      elements[dstIndex] = null;
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

  @Override
  public Iterator<E> iterator() {
    return new Iterator<E>() {
      int expectedMetadata = metadata;
      int currentIndex = firstEntryIndex();
      int indexToRemove = -1;

      @Override
      public boolean hasNext() {
        return currentIndex >= 0;
      }

      @SuppressWarnings("unchecked") // known to be Es
      @Override
      public E next() {
        checkForConcurrentModification();
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        indexToRemove = currentIndex;
        E result = (E) elements[currentIndex];
        currentIndex = getSuccessor(currentIndex);
        return result;
      }

      @Override
      public void remove() {
        checkForConcurrentModification();
        checkRemove(indexToRemove >= 0);
        incrementExpectedModCount();
        CompactHashSet.this.remove(elements[indexToRemove]);
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
    };
  }

  @Override
  public Spliterator<E> spliterator() {
    if (needsAllocArrays()) {
      return Spliterators.spliterator(new Object[0], Spliterator.DISTINCT | Spliterator.ORDERED);
    }
    return Spliterators.spliterator(elements, 0, size, Spliterator.DISTINCT | Spliterator.ORDERED);
  }

  @SuppressWarnings("unchecked") // known to be Es
  @Override
  public void forEach(Consumer<? super E> action) {
    checkNotNull(action);
    for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
      action.accept((E) elements[i]);
    }
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public Object[] toArray() {
    if (needsAllocArrays()) {
      return new Object[0];
    }
    return Arrays.copyOf(elements, size);
  }

  @CanIgnoreReturnValue
  @Override
  public <T> T[] toArray(T[] a) {
    if (needsAllocArrays()) {
      if (a.length > 0) {
        a[0] = null;
      }
      return a;
    }
    return ObjectArrays.toArrayImpl(elements, 0, size, a);
  }

  /**
   * Ensures that this {@code CompactHashSet} has the smallest representation in memory, given its
   * current size.
   */
  public void trimToSize() {
    if (needsAllocArrays()) {
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
    Arrays.fill(elements, 0, size, null);
    CompactHashing.tableClear(table);
    Arrays.fill(entries, 0, size, 0);
    this.size = 0;
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeInt(size);
    for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
      stream.writeObject(elements[i]);
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
      E element = (E) stream.readObject();
      add(element);
    }
  }
}
