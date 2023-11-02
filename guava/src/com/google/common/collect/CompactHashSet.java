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
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
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
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
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
@ElementTypesAreNonnullByDefault
class CompactHashSet<E extends @Nullable Object> extends AbstractSet<E> implements Serializable {
  // TODO(user): cache all field accesses in local vars

  /** Creates an empty {@code CompactHashSet} instance. */
  public static <E extends @Nullable Object> CompactHashSet<E> create() {
    return new CompactHashSet<>();
  }

  /**
   * Creates a <i>mutable</i> {@code CompactHashSet} instance containing the elements of the given
   * collection in unspecified order.
   *
   * @param collection the elements that the set should contain
   * @return a new {@code CompactHashSet} containing those elements (minus duplicates)
   */
  public static <E extends @Nullable Object> CompactHashSet<E> create(
      Collection<? extends E> collection) {
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
  public static <E extends @Nullable Object> CompactHashSet<E> create(E... elements) {
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
  public static <E extends @Nullable Object> CompactHashSet<E> createWithExpectedSize(
      int expectedSize) {
    return new CompactHashSet<>(expectedSize);
  }

  /**
   * Maximum allowed false positive probability of detecting a hash flooding attack given random
   * input.
   */
  @VisibleForTesting(
      )
  static final double HASH_FLOODING_FPP = 0.001;

  /**
   * Maximum allowed length of a hash table bucket before falling back to a j.u.LinkedHashSet based
   * implementation. Experimentally determined.
   */
  private static final int MAX_HASH_BUCKET_LENGTH = 9;

  // See CompactHashMap for a detailed description of how the following fields work. That
  // description talks about `keys`, `values`, and `entries`; here the `keys` and `values` arrays
  // are replaced by a single `elements` array but everything else works similarly.

  /**
   * The hashtable object. This can be either:
   *
   * <ul>
   *   <li>a byte[], short[], or int[], with size a power of two, created by
   *       CompactHashing.createTable, whose values are either
   *       <ul>
   *         <li>UNSET, meaning "null pointer"
   *         <li>one plus an index into the entries and elements array
   *       </ul>
   *   <li>another java.util.Set delegate implementation. In most modern JDKs, normal java.util hash
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
   * part of the smeared hash of the element not covered by the hashtable mask, whereas the low bits
   * are the "next" pointer (pointing to the next entry in the bucket chain), which will always be
   * less than or equal to the hashtable mask.
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
  @CheckForNull private transient int[] entries;

  /**
   * The elements contained in the set, in the range of [0, size()). The elements in [size(),
   * elements.length) are all {@code null}.
   */
  @VisibleForTesting @CheckForNull transient @Nullable Object[] elements;

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
    this.elements = new Object[expectedSize];

    return expectedSize;
  }

  @SuppressWarnings("unchecked")
  @VisibleForTesting
  @CheckForNull
  Set<E> delegateOrNull() {
    if (table instanceof Set) {
      return (Set<E>) table;
    }
    return null;
  }

  private Set<E> createHashFloodingResistantDelegate(int tableSize) {
    return new LinkedHashSet<>(tableSize, 1.0f);
  }

  @VisibleForTesting
  @CanIgnoreReturnValue
  Set<E> convertToHashFloodingResistantImplementation() {
    Set<E> newDelegate = createHashFloodingResistantDelegate(hashTableMask() + 1);
    for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
      newDelegate.add(element(i));
    }
    this.table = newDelegate;
    this.entries = null;
    this.elements = null;
    incrementModCount();
    return newDelegate;
  }

  @VisibleForTesting
  boolean isUsingHashFloodingResistance() {
    return delegateOrNull() != null;
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
  public boolean add(@ParametricNullness E object) {
    if (needsAllocArrays()) {
      allocArrays();
    }
    Set<E> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.add(object);
    }
    int[] entries = requireEntries();
    @Nullable Object[] elements = requireElements();

    int newEntryIndex = this.size; // current size, and pointer to the entry to be appended
    int newSize = newEntryIndex + 1;
    int hash = smearedHash(object);
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
            && Objects.equal(object, elements[entryIndex])) {
          return false;
        }
        next = CompactHashing.getNext(entry, mask);
        bucketLength++;
      } while (next != UNSET);

      if (bucketLength >= MAX_HASH_BUCKET_LENGTH) {
        return convertToHashFloodingResistantImplementation().add(object);
      }

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
  void insertEntry(int entryIndex, @ParametricNullness E object, int hash, int mask) {
    setEntry(entryIndex, CompactHashing.maskCombine(hash, UNSET, mask));
    setElement(entryIndex, object);
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
    this.elements = Arrays.copyOf(requireElements(), newCapacity);
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

    // Loop over current hashtable
    for (int oldTableIndex = 0; oldTableIndex <= oldMask; oldTableIndex++) {
      int oldNext = CompactHashing.tableGet(oldTable, oldTableIndex);
      while (oldNext != UNSET) {
        int entryIndex = oldNext - 1;
        int oldEntry = entries[entryIndex];

        // Rebuild hash using entry hashPrefix and tableIndex ("hashSuffix")
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

  @Override
  public boolean contains(@CheckForNull Object object) {
    if (needsAllocArrays()) {
      return false;
    }
    Set<E> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.contains(object);
    }
    int hash = smearedHash(object);
    int mask = hashTableMask();
    int next = CompactHashing.tableGet(requireTable(), hash & mask);
    if (next == UNSET) {
      return false;
    }
    int hashPrefix = CompactHashing.getHashPrefix(hash, mask);
    do {
      int entryIndex = next - 1;
      int entry = entry(entryIndex);
      if (CompactHashing.getHashPrefix(entry, mask) == hashPrefix
          && Objects.equal(object, element(entryIndex))) {
        return true;
      }
      next = CompactHashing.getNext(entry, mask);
    } while (next != UNSET);
    return false;
  }

  @CanIgnoreReturnValue
  @Override
  public boolean remove(@CheckForNull Object object) {
    if (needsAllocArrays()) {
      return false;
    }
    Set<E> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.remove(object);
    }
    int mask = hashTableMask();
    int index =
        CompactHashing.remove(
            object,
            /* value= */ null,
            mask,
            requireTable(),
            requireEntries(),
            requireElements(),
            /* values= */ null);
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
    Object table = requireTable();
    int[] entries = requireEntries();
    @Nullable Object[] elements = requireElements();
    int srcIndex = size() - 1;
    if (dstIndex < srcIndex) {
      // move last entry to deleted spot
      Object object = elements[srcIndex];
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
    Set<E> delegate = delegateOrNull();
    if (delegate != null) {
      return delegate.iterator();
    }
    return new Iterator<E>() {
      int expectedMetadata = metadata;
      int currentIndex = firstEntryIndex();
      int indexToRemove = -1;

      @Override
      public boolean hasNext() {
        return currentIndex >= 0;
      }

      @Override
      @ParametricNullness
      public E next() {
        checkForConcurrentModification();
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        indexToRemove = currentIndex;
        E result = element(currentIndex);
        currentIndex = getSuccessor(currentIndex);
        return result;
      }

      @Override
      public void remove() {
        checkForConcurrentModification();
        checkRemove(indexToRemove >= 0);
        incrementExpectedModCount();
        CompactHashSet.this.remove(element(indexToRemove));
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
    Set<E> delegate = delegateOrNull();
    return (delegate != null)
        ? delegate.spliterator()
        : Spliterators.spliterator(
            requireElements(), 0, size, Spliterator.DISTINCT | Spliterator.ORDERED);
  }

  @Override
  public void forEach(Consumer<? super E> action) {
    checkNotNull(action);
    Set<E> delegate = delegateOrNull();
    if (delegate != null) {
      delegate.forEach(action);
    } else {
      for (int i = firstEntryIndex(); i >= 0; i = getSuccessor(i)) {
        action.accept(element(i));
      }
    }
  }

  @Override
  public int size() {
    Set<E> delegate = delegateOrNull();
    return (delegate != null) ? delegate.size() : size;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public @Nullable Object[] toArray() {
    if (needsAllocArrays()) {
      return new Object[0];
    }
    Set<E> delegate = delegateOrNull();
    return (delegate != null) ? delegate.toArray() : Arrays.copyOf(requireElements(), size);
  }

  @CanIgnoreReturnValue
  @Override
  @SuppressWarnings("nullness") // b/192354773 in our checker affects toArray declarations
  public <T extends @Nullable Object> T[] toArray(T[] a) {
    if (needsAllocArrays()) {
      if (a.length > 0) {
        a[0] = null;
      }
      return a;
    }
    Set<E> delegate = delegateOrNull();
    return (delegate != null)
        ? delegate.toArray(a)
        : ObjectArrays.toArrayImpl(requireElements(), 0, size, a);
  }

  /**
   * Ensures that this {@code CompactHashSet} has the smallest representation in memory, given its
   * current size.
   */
  public void trimToSize() {
    if (needsAllocArrays()) {
      return;
    }
    Set<E> delegate = delegateOrNull();
    if (delegate != null) {
      Set<E> newDelegate = createHashFloodingResistantDelegate(size());
      newDelegate.addAll(delegate);
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
    Set<E> delegate = delegateOrNull();
    if (delegate != null) {
      metadata =
          Ints.constrainToRange(size(), CompactHashing.DEFAULT_SIZE, CompactHashing.MAX_SIZE);
      delegate.clear(); // invalidate any iterators left over!
      table = null;
      size = 0;
    } else {
      Arrays.fill(requireElements(), 0, size, null);
      CompactHashing.tableClear(requireTable());
      Arrays.fill(requireEntries(), 0, size, 0);
      this.size = 0;
    }
  }

  @J2ktIncompatible
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeInt(size());
    for (E e : this) {
      stream.writeObject(e);
    }
  }

  @SuppressWarnings("unchecked")
  @J2ktIncompatible
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

  /*
   * For discussion of the safety of the following methods, see the comments near the end of
   * CompactHashMap.
   */

  private Object requireTable() {
    return requireNonNull(table);
  }

  private int[] requireEntries() {
    return requireNonNull(entries);
  }

  private @Nullable Object[] requireElements() {
    return requireNonNull(elements);
  }

  @SuppressWarnings("unchecked")
  private E element(int i) {
    return (E) requireElements()[i];
  }

  private int entry(int i) {
    return requireEntries()[i];
  }

  private void setElement(int i, E value) {
    requireElements()[i] = value;
  }

  private void setEntry(int i, int value) {
    requireEntries()[i] = value;
  }
}
