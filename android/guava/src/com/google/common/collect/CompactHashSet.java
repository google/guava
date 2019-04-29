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

import static com.google.common.collect.CollectPreconditions.checkRemove;
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
import org.checkerframework.checker.nullness.compatqual.MonotonicNonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

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

  private static final float LOAD_FACTOR = 1.0f;

  /** Bitmask that selects the low 32 bits. */
  private static final long NEXT_MASK = (1L << 32) - 1;

  /** Bitmask that selects the high 32 bits. */
  private static final long HASH_MASK = ~NEXT_MASK;

  // TODO(user): decide default size
  @VisibleForTesting static final int DEFAULT_SIZE = 3;

  // used to indicate blank table entries
  static final int UNSET = -1;

  /**
   * The hashtable. Its values are indexes to the elements and entries arrays.
   *
   * <p>Currently, the UNSET value means "null pointer", and any non negative value x is the actual
   * index.
   *
   * <p>Its size must be a power of two.
   */
  @MonotonicNonNullDecl private transient int[] table;

  /**
   * Contains the logical entries, in the range of [0, size()). The high 32 bits of each long is the
   * smeared hash of the element, whereas the low 32 bits is the "next" pointer (pointing to the
   * next entry in the bucket chain). The pointers in [size(), entries.length) are all "null"
   * (UNSET).
   */
  @MonotonicNonNullDecl private transient long[] entries;

  /**
   * The elements contained in the set, in the range of [0, size()). The elements in [size(),
   * elements.length) are all {@code null}.
   */
  @MonotonicNonNullDecl transient Object[] elements;

  /**
   * Keeps track of modifications of this set, to make it possible to throw
   * ConcurrentModificationException in the iterator. Note that we choose not to make this volatile,
   * so we do less of a "best effort" to track such errors, for better performance.
   */
  transient int modCount;

  /** The number of elements contained in the set. */
  private transient int size;

  /** Constructs a new empty instance of {@code CompactHashSet}. */
  CompactHashSet() {
    init(DEFAULT_SIZE);
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
    Preconditions.checkArgument(expectedSize >= 0, "Initial capacity must be non-negative");
    this.modCount = Math.max(1, expectedSize); // Save expectedSize for use in allocArrays()
  }

  /** Returns whether arrays need to be allocated. */
  boolean needsAllocArrays() {
    return table == null;
  }

  /** Handle lazy allocation of arrays. */
  void allocArrays() {
    Preconditions.checkState(needsAllocArrays(), "Arrays already allocated");

    int expectedSize = modCount;
    int buckets = Hashing.closedTableSize(expectedSize, LOAD_FACTOR);
    this.table = newTable(buckets);

    this.entries = newEntries(expectedSize);
    this.elements = new Object[expectedSize];
  }

  private static int[] newTable(int size) {
    int[] array = new int[size];
    Arrays.fill(array, UNSET);
    return array;
  }

  private static long[] newEntries(int size) {
    long[] array = new long[size];
    Arrays.fill(array, UNSET);
    return array;
  }

  private int hashTableMask() {
    return table.length - 1;
  }

  private static int getHash(long entry) {
    return (int) (entry >>> 32);
  }

  /** Returns the index, or UNSET if the pointer is "null" */
  private static int getNext(long entry) {
    return (int) entry;
  }

  /** Returns a new entry value by changing the "next" index of an existing entry */
  private static long swapNext(long entry, int newNext) {
    return (HASH_MASK & entry) | (NEXT_MASK & newNext);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean add(@NullableDecl E object) {
    if (needsAllocArrays()) {
      allocArrays();
    }
    long[] entries = this.entries;
    Object[] elements = this.elements;

    int hash = smearedHash(object);
    int tableIndex = hash & hashTableMask();
    int newEntryIndex = this.size; // current size, and pointer to the entry to be appended
    int next = table[tableIndex];
    if (next == UNSET) { // uninitialized bucket
      table[tableIndex] = newEntryIndex;
    } else {
      int last;
      long entry;
      do {
        last = next;
        entry = entries[next];
        if (getHash(entry) == hash && Objects.equal(object, elements[next])) {
          return false;
        }
        next = getNext(entry);
      } while (next != UNSET);
      entries[last] = swapNext(entry, newEntryIndex);
    }
    if (newEntryIndex == Integer.MAX_VALUE) {
      throw new IllegalStateException("Cannot contain more than Integer.MAX_VALUE elements!");
    }
    int newSize = newEntryIndex + 1;
    resizeMeMaybe(newSize);
    insertEntry(newEntryIndex, object, hash);
    this.size = newSize;
    int oldCapacity = table.length;
    if (Hashing.needsResizing(newEntryIndex, oldCapacity, LOAD_FACTOR)) {
      resizeTable(2 * oldCapacity);
    }
    modCount++;
    return true;
  }

  /**
   * Creates a fresh entry with the specified object at the specified position in the entry arrays.
   */
  void insertEntry(int entryIndex, E object, int hash) {
    this.entries[entryIndex] = ((long) hash << 32) | (NEXT_MASK & UNSET);
    this.elements[entryIndex] = object;
  }

  /** Resizes the entries storage if necessary. */
  private void resizeMeMaybe(int newSize) {
    int entriesSize = entries.length;
    if (newSize > entriesSize) {
      int newCapacity = entriesSize + Math.max(1, entriesSize >>> 1);
      if (newCapacity < 0) {
        newCapacity = Integer.MAX_VALUE;
      }
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
    this.elements = Arrays.copyOf(elements, newCapacity);
    long[] entries = this.entries;
    int oldCapacity = entries.length;
    entries = Arrays.copyOf(entries, newCapacity);
    if (newCapacity > oldCapacity) {
      Arrays.fill(entries, oldCapacity, newCapacity, UNSET);
    }
    this.entries = entries;
  }

  private void resizeTable(int newCapacity) { // newCapacity always a power of two
    int[] newTable = newTable(newCapacity);
    long[] entries = this.entries;

    int mask = newTable.length - 1;
    for (int i = 0; i < size; i++) {
      long oldEntry = entries[i];
      int hash = getHash(oldEntry);
      int tableIndex = hash & mask;
      int next = newTable[tableIndex];
      newTable[tableIndex] = i;
      entries[i] = ((long) hash << 32) | (NEXT_MASK & next);
    }

    this.table = newTable;
  }

  @Override
  public boolean contains(@NullableDecl Object object) {
    if (needsAllocArrays()) {
      return false;
    }
    int hash = smearedHash(object);
    int next = table[hash & hashTableMask()];
    while (next != UNSET) {
      long entry = entries[next];
      if (getHash(entry) == hash && Objects.equal(object, elements[next])) {
        return true;
      }
      next = getNext(entry);
    }
    return false;
  }

  @CanIgnoreReturnValue
  @Override
  public boolean remove(@NullableDecl Object object) {
    if (needsAllocArrays()) {
      return false;
    }
    return remove(object, smearedHash(object));
  }

  @CanIgnoreReturnValue
  private boolean remove(Object object, int hash) {
    int tableIndex = hash & hashTableMask();
    int next = table[tableIndex];
    if (next == UNSET) {
      return false;
    }
    int last = UNSET;
    do {
      if (getHash(entries[next]) == hash && Objects.equal(object, elements[next])) {
        if (last == UNSET) {
          // we need to update the root link from table[]
          table[tableIndex] = getNext(entries[next]);
        } else {
          // we need to update the link from the chain
          entries[last] = swapNext(entries[last], getNext(entries[next]));
        }

        moveLastEntry(next);
        size--;
        modCount++;
        return true;
      }
      last = next;
      next = getNext(entries[next]);
    } while (next != UNSET);
    return false;
  }

  /**
   * Moves the last entry in the entry array into {@code dstIndex}, and nulls out its old position.
   */
  void moveLastEntry(int dstIndex) {
    int srcIndex = size() - 1;
    if (dstIndex < srcIndex) {
      // move last entry to deleted spot
      elements[dstIndex] = elements[srcIndex];
      elements[srcIndex] = null;

      // move the last entry to the removed spot, just like we moved the element
      long lastEntry = entries[srcIndex];
      entries[dstIndex] = lastEntry;
      entries[srcIndex] = UNSET;

      // also need to update whoever's "next" pointer was pointing to the last entry place
      // reusing "tableIndex" and "next"; these variables were no longer needed
      int tableIndex = getHash(lastEntry) & hashTableMask();
      int lastNext = table[tableIndex];
      if (lastNext == srcIndex) {
        // we need to update the root pointer
        table[tableIndex] = dstIndex;
      } else {
        // we need to update a pointer in an entry
        int previous;
        long entry;
        do {
          previous = lastNext;
          lastNext = getNext(entry = entries[lastNext]);
        } while (lastNext != srcIndex);
        // here, entries[previous] points to the old entry location; update it
        entries[previous] = swapNext(entry, dstIndex);
      }
    } else {
      elements[dstIndex] = null;
      entries[dstIndex] = UNSET;
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
      int expectedModCount = modCount;
      int currentIndex = firstEntryIndex();
      int indexToRemove = -1;

      @Override
      public boolean hasNext() {
        return currentIndex >= 0;
      }

      @Override
      @SuppressWarnings("unchecked")
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
        expectedModCount++;
        CompactHashSet.this.remove(elements[indexToRemove], getHash(entries[indexToRemove]));
        currentIndex = adjustAfterRemove(currentIndex, indexToRemove);
        indexToRemove = -1;
      }

      private void checkForConcurrentModification() {
        if (modCount != expectedModCount) {
          throw new ConcurrentModificationException();
        }
      }
    };
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
    int minimumTableSize = Hashing.closedTableSize(size, LOAD_FACTOR);
    if (minimumTableSize < table.length) {
      resizeTable(minimumTableSize);
    }
  }

  @Override
  public void clear() {
    if (needsAllocArrays()) {
      return;
    }
    modCount++;
    Arrays.fill(elements, 0, size, null);
    Arrays.fill(table, UNSET);
    Arrays.fill(entries, 0, size, UNSET);
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
