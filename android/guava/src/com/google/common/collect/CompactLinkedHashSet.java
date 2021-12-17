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

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * CompactLinkedHashSet is an implementation of a Set, which a predictable iteration order that
 * matches the insertion order. All optional operations (adding and removing) are supported. All
 * elements, including {@code null}, are permitted.
 *
 * <p>{@code contains(x)}, {@code add(x)} and {@code remove(x)}, are all (expected and amortized)
 * constant time operations. Expected in the hashtable sense (depends on the hash function doing a
 * good job of distributing the elements to the buckets to a distribution not far from uniform), and
 * amortized since some operations can trigger a hash table resize.
 *
 * <p>This implementation consumes significantly less memory than {@code java.util.LinkedHashSet} or
 * even {@code java.util.HashSet}, and places considerably less load on the garbage collector. Like
 * {@code java.util.LinkedHashSet}, it offers insertion-order iteration, with identical behavior.
 *
 * <p>This class should not be assumed to be universally superior to {@code
 * java.util.LinkedHashSet}. Generally speaking, this class reduces object allocation and memory
 * consumption at the price of moderately increased constant factors of CPU. Only use this class
 * when there is a specific reason to prioritize memory over CPU.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible // not worth using in GWT for now
@ElementTypesAreNonnullByDefault
class CompactLinkedHashSet<E extends @Nullable Object> extends CompactHashSet<E> {

  /** Creates an empty {@code CompactLinkedHashSet} instance. */
  public static <E extends @Nullable Object> CompactLinkedHashSet<E> create() {
    return new CompactLinkedHashSet<>();
  }

  /**
   * Creates a <i>mutable</i> {@code CompactLinkedHashSet} instance containing the elements of the
   * given collection in the order returned by the collection's iterator.
   *
   * @param collection the elements that the set should contain
   * @return a new {@code CompactLinkedHashSet} containing those elements (minus duplicates)
   */
  public static <E extends @Nullable Object> CompactLinkedHashSet<E> create(
      Collection<? extends E> collection) {
    CompactLinkedHashSet<E> set = createWithExpectedSize(collection.size());
    set.addAll(collection);
    return set;
  }

  /**
   * Creates a {@code CompactLinkedHashSet} instance containing the given elements in unspecified
   * order.
   *
   * @param elements the elements that the set should contain
   * @return a new {@code CompactLinkedHashSet} containing those elements (minus duplicates)
   */
  @SafeVarargs
  public static <E extends @Nullable Object> CompactLinkedHashSet<E> create(E... elements) {
    CompactLinkedHashSet<E> set = createWithExpectedSize(elements.length);
    Collections.addAll(set, elements);
    return set;
  }

  /**
   * Creates a {@code CompactLinkedHashSet} instance, with a high enough "initial capacity" that it
   * <i>should</i> hold {@code expectedSize} elements without rebuilding internal data structures.
   *
   * @param expectedSize the number of elements you expect to add to the returned set
   * @return a new, empty {@code CompactLinkedHashSet} with enough capacity to hold {@code
   *     expectedSize} elements without resizing
   * @throws IllegalArgumentException if {@code expectedSize} is negative
   */
  public static <E extends @Nullable Object> CompactLinkedHashSet<E> createWithExpectedSize(
      int expectedSize) {
    return new CompactLinkedHashSet<>(expectedSize);
  }

  private static final int ENDPOINT = -2;

  // TODO(user): predecessors and successors should be collocated (reducing cache misses).
  // Might also explore collocating all of [hash, next, predecessor, successor] fields of an
  // entry in a *single* long[], though that reduces the maximum size of the set by a factor of 2

  /**
   * Pointer to the predecessor of an entry in insertion order. ENDPOINT indicates a node is the
   * first node in insertion order; all values at indices ≥ {@link #size()} are UNSET.
   */
  @CheckForNull private transient int[] predecessor;

  /**
   * Pointer to the successor of an entry in insertion order. ENDPOINT indicates a node is the last
   * node in insertion order; all values at indices ≥ {@link #size()} are UNSET.
   */
  @CheckForNull private transient int[] successor;

  /** Pointer to the first node in the linked list, or {@code ENDPOINT} if there are no entries. */
  private transient int firstEntry;

  /** Pointer to the last node in the linked list, or {@code ENDPOINT} if there are no entries. */
  private transient int lastEntry;

  CompactLinkedHashSet() {
    super();
  }

  CompactLinkedHashSet(int expectedSize) {
    super(expectedSize);
  }

  @Override
  void init(int expectedSize) {
    super.init(expectedSize);
    this.firstEntry = ENDPOINT;
    this.lastEntry = ENDPOINT;
  }

  @Override
  int allocArrays() {
    int expectedSize = super.allocArrays();
    this.predecessor = new int[expectedSize];
    this.successor = new int[expectedSize];
    return expectedSize;
  }

  @Override
  @CanIgnoreReturnValue
  Set<E> convertToHashFloodingResistantImplementation() {
    Set<E> result = super.convertToHashFloodingResistantImplementation();
    this.predecessor = null;
    this.successor = null;
    return result;
  }

  /*
   * For discussion of the safety of the following methods for operating on predecessors and
   * successors, see the comments near the end of CompactHashMap, noting that the methods here call
   * requirePredecessors() and requireSuccessors(), which are defined at the end of this file.
   */

  private int getPredecessor(int entry) {
    return requirePredecessors()[entry] - 1;
  }

  @Override
  int getSuccessor(int entry) {
    return requireSuccessors()[entry] - 1;
  }

  private void setSuccessor(int entry, int succ) {
    requireSuccessors()[entry] = succ + 1;
  }

  private void setPredecessor(int entry, int pred) {
    requirePredecessors()[entry] = pred + 1;
  }

  private void setSucceeds(int pred, int succ) {
    if (pred == ENDPOINT) {
      firstEntry = succ;
    } else {
      setSuccessor(pred, succ);
    }

    if (succ == ENDPOINT) {
      lastEntry = pred;
    } else {
      setPredecessor(succ, pred);
    }
  }

  @Override
  void insertEntry(int entryIndex, @ParametricNullness E object, int hash, int mask) {
    super.insertEntry(entryIndex, object, hash, mask);
    setSucceeds(lastEntry, entryIndex);
    setSucceeds(entryIndex, ENDPOINT);
  }

  @Override
  void moveLastEntry(int dstIndex, int mask) {
    int srcIndex = size() - 1;
    super.moveLastEntry(dstIndex, mask);

    setSucceeds(getPredecessor(dstIndex), getSuccessor(dstIndex));
    if (dstIndex < srcIndex) {
      setSucceeds(getPredecessor(srcIndex), dstIndex);
      setSucceeds(dstIndex, getSuccessor(srcIndex));
    }
    requirePredecessors()[srcIndex] = 0;
    requireSuccessors()[srcIndex] = 0;
  }

  @Override
  void resizeEntries(int newCapacity) {
    super.resizeEntries(newCapacity);
    predecessor = Arrays.copyOf(requirePredecessors(), newCapacity);
    successor = Arrays.copyOf(requireSuccessors(), newCapacity);
  }

  @Override
  int firstEntryIndex() {
    return firstEntry;
  }

  @Override
  int adjustAfterRemove(int indexBeforeRemove, int indexRemoved) {
    return (indexBeforeRemove >= size()) ? indexRemoved : indexBeforeRemove;
  }

  @Override
  public @Nullable Object[] toArray() {
    return ObjectArrays.toArrayImpl(this);
  }

  @Override
  @SuppressWarnings("nullness") // b/192354773 in our checker affects toArray declarations
  public <T extends @Nullable Object> T[] toArray(T[] a) {
    return ObjectArrays.toArrayImpl(this, a);
  }

  @Override
  public void clear() {
    if (needsAllocArrays()) {
      return;
    }
    this.firstEntry = ENDPOINT;
    this.lastEntry = ENDPOINT;
    // Either both arrays are null or neither is, but we check both to satisfy the nullness checker.
    if (predecessor != null && successor != null) {
      Arrays.fill(predecessor, 0, size(), 0);
      Arrays.fill(successor, 0, size(), 0);
    }
    super.clear();
  }

  /*
   * For discussion of the safety of the following methods, see the comments near the end of
   * CompactHashMap.
   */

  private int[] requirePredecessors() {
    return requireNonNull(predecessor);
  }

  private int[] requireSuccessors() {
    return requireNonNull(successor);
  }

  /*
   * We don't define getPredecessor+getSuccessor and setPredecessor+setSuccessor here because
   * they're defined above -- including logic to add and subtract 1 to map between the values stored
   * in the predecessor/successor arrays and the indexes in the elements array that they identify.
   */
}
