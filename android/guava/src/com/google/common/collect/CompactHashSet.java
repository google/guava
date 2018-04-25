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
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
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
 * moderately increased constant factors of CPU.  Only use this class when there is a specific
 * reason to prioritize memory over CPU.
 *
 * @author Dimitris Andreou
 */
@GwtIncompatible // not worth using in GWT for now
class CompactHashSet<E> extends AbstractSet<E> implements Serializable {
  // TODO(user): cache all field accesses in local vars

  /** Creates an empty {@code CompactHashSet} instance. */
  public static <E> CompactHashSet<E> create() {
    return new CompactHashSet<E>();
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
    return new CompactHashSet<E>(expectedSize);
  }

  private static final int MAXIMUM_CAPACITY = 1 << 30;

  // TODO(user): decide, and inline, load factor. 0.75?
  private static final float DEFAULT_LOAD_FACTOR = 1.0f;

  /** Bitmask that selects the low 32 bits. */
  private static final long NEXT_MASK = (1L << 32) - 1;

  /** Bitmask that selects the high 32 bits. */
  private static final long HASH_MASK = ~NEXT_MASK;

  // TODO(user): decide default size
  private static final int DEFAULT_SIZE = 3;

  static final int UNSET = -1;

  /**
   * The hashtable. Its values are indexes to both the elements and entries arrays.
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

  /** The elements contained in the set, in the range of [0, size()). */
  @MonotonicNonNullDecl transient Object[] elements;

  /** The load factor. */
  transient float loadFactor;

  /**
   * Keeps track of modifications of this set, to make it possible to throw
   * ConcurrentModificationException in the iterator. Note that we choose not to make this volatile,
   * so we do less of a "best effort" to track such errors, for better performance.
   */
  transient int modCount;

  /** When we have this many elements, resize the hashtable. */
  private transient int threshold;

  /** The number of elements contained in the set. */
  private transient int size;

  /** Constructs a new empty instance of {@code CompactHashSet}. */
  CompactHashSet() {
    init(DEFAULT_SIZE, DEFAULT_LOAD_FACTOR);
  }

  /**
   * Constructs a new instance of {@code CompactHashSet} with the specified capacity.
   *
   * @param expectedSize the initial capacity of this {@code CompactHashSet}.
   */
  CompactHashSet(int expectedSize) {
    init(expectedSize, DEFAULT_LOAD_FACTOR);
  }

  /** Pseudoconstructor for serialization support. */
  void init(int expectedSize, float loadFactor) {
    Preconditions.checkArgument(expectedSize >= 0, "Initial capacity must be non-negative");
    Preconditions.checkArgument(loadFactor > 0, "Illegal load factor");
    int buckets = Hashing.closedTableSize(expectedSize, loadFactor);
    this.table = newTable(buckets);
    this.loadFactor = loadFactor;
    this.elements = new Object[expectedSize];
    this.entries = newEntries(expectedSize);
    this.threshold = Math.max(1, (int) (buckets * loadFactor));
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

  private int hashTableMask() {
    return table.length - 1;
  }

  @CanIgnoreReturnValue
  @Override
  public boolean add(@NullableDecl E object) {
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
    if (newEntryIndex >= threshold) {
      resizeTable(2 * table.length);
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

  /** Returns currentSize + 1, after resizing the entries storage if necessary. */
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
    int oldSize = entries.length;
    entries = Arrays.copyOf(entries, newCapacity);
    if (newCapacity > oldSize) {
      Arrays.fill(entries, oldSize, newCapacity, UNSET);
    }
    this.entries = entries;
  }

  private void resizeTable(int newCapacity) { // newCapacity always a power of two
    int[] oldTable = table;
    int oldCapacity = oldTable.length;
    if (oldCapacity >= MAXIMUM_CAPACITY) {
      threshold = Integer.MAX_VALUE;
      return;
    }
    int newThreshold = 1 + (int) (newCapacity * loadFactor);
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

    this.threshold = newThreshold;
    this.table = newTable;
  }

  @Override
  public boolean contains(@NullableDecl Object object) {
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

        moveEntry(next);
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
  void moveEntry(int dstIndex) {
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
      int index = firstEntryIndex();
      int indexToRemove = -1;

      @Override
      public boolean hasNext() {
        return index >= 0;
      }

      @Override
      @SuppressWarnings("unchecked")
      public E next() {
        checkForConcurrentModification();
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        indexToRemove = index;
        E result = (E) elements[index];
        index = getSuccessor(index);
        return result;
      }

      @Override
      public void remove() {
        checkForConcurrentModification();
        checkRemove(indexToRemove >= 0);
        expectedModCount++;
        CompactHashSet.this.remove(elements[indexToRemove], getHash(entries[indexToRemove]));
        index = adjustAfterRemove(index, indexToRemove);
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
    return Arrays.copyOf(elements, size);
  }

  @CanIgnoreReturnValue
  @Override
  public <T> T[] toArray(T[] a) {
    return ObjectArrays.toArrayImpl(elements, 0, size, a);
  }

  /**
   * Ensures that this {@code CompactHashSet} has the smallest representation in memory, given its
   * current size.
   */
  public void trimToSize() {
    int size = this.size;
    if (size < entries.length) {
      resizeEntries(size);
    }
    // size / loadFactor gives the table size of the appropriate load factor,
    // but that may not be a power of two. We floor it to a power of two by
    // keeping its highest bit. But the smaller table may have a load factor
    // larger than what we want; then we want to go to the next power of 2 if we can
    int minimumTableSize = Math.max(1, Integer.highestOneBit((int) (size / loadFactor)));
    if (minimumTableSize < MAXIMUM_CAPACITY) {
      double load = (double) size / minimumTableSize;
      if (load > loadFactor) {
        minimumTableSize <<= 1; // increase to next power if possible
      }
    }

    if (minimumTableSize < table.length) {
      resizeTable(minimumTableSize);
    }
  }

  @Override
  public void clear() {
    modCount++;
    Arrays.fill(elements, 0, size, null);
    Arrays.fill(table, UNSET);
    Arrays.fill(entries, UNSET);
    this.size = 0;
  }

  /**
   * The serial form currently mimics Android's java.util.HashSet version, e.g. see
   * http://omapzoom.org/?p=platform/libcore.git;a=blob;f=luni/src/main/java/java/util/HashSet.java
   */
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeInt(size);
    for (E e : this) {
      stream.writeObject(e);
    }
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    init(DEFAULT_SIZE, DEFAULT_LOAD_FACTOR);
    int elementCount = stream.readInt();
    for (int i = elementCount; --i >= 0; ) {
      E element = (E) stream.readObject();
      add(element);
    }
  }
}
