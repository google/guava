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

import com.google.common.annotations.GwtIncompatible;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.checkerframework.checker.nullness.compatqual.MonotonicNonNullDecl;

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
class CompactLinkedHashSet<E> extends CompactHashSet<E> {

  /**
   * Creates an empty {@code CompactLinkedHashSet} instance.
   */
  public static <E> CompactLinkedHashSet<E> create() {
    return new CompactLinkedHashSet<E>();
  }

  /**
   * Creates a <i>mutable</i> {@code CompactLinkedHashSet} instance containing the elements
   * of the given collection in the order returned by the collection's iterator.
   *
   * @param collection the elements that the set should contain
   * @return a new {@code CompactLinkedHashSet} containing those elements (minus duplicates)
   */
  public static <E> CompactLinkedHashSet<E> create(Collection<? extends E> collection) {
    CompactLinkedHashSet<E> set = createWithExpectedSize(collection.size());
    set.addAll(collection);
    return set;
  }

  /**
   * Creates a {@code CompactLinkedHashSet} instance containing the given elements in
   * unspecified order.
   *
   * @param elements the elements that the set should contain
   * @return a new {@code CompactLinkedHashSet} containing those elements (minus duplicates)
   */
  public static <E> CompactLinkedHashSet<E> create(E... elements) {
    CompactLinkedHashSet<E> set = createWithExpectedSize(elements.length);
    Collections.addAll(set, elements);
    return set;
  }

  /**
   * Creates a {@code CompactLinkedHashSet} instance, with a high enough "initial capacity"
   * that it <i>should</i> hold {@code expectedSize} elements without rebuilding internal
   * data structures.
   *
   * @param expectedSize the number of elements you expect to add to the returned set
   * @return a new, empty {@code CompactLinkedHashSet} with enough capacity to hold {@code
   *         expectedSize} elements without resizing
   * @throws IllegalArgumentException if {@code expectedSize} is negative
   */
  public static <E> CompactLinkedHashSet<E> createWithExpectedSize(int expectedSize) {
    return new CompactLinkedHashSet<E>(expectedSize);
  }

  private static final int ENDPOINT = -2;

  // TODO(user): predecessors and successors should be collocated (reducing cache misses).
  // Might also explore collocating all of [hash, next, predecessor, succesor] fields of an
  // entry in a *single* long[], though that reduces the maximum size of the set by a factor of 2

  /**
   * Pointer to the predecessor of an entry in insertion order. ENDPOINT indicates a node is the
   * first node in insertion order; all values at indices ≥ {@link #size()} are UNSET.
   */
  @MonotonicNonNullDecl private transient int[] predecessor;

  /**
   * Pointer to the successor of an entry in insertion order. ENDPOINT indicates a node is the last
   * node in insertion order; all values at indices ≥ {@link #size()} are UNSET.
   */
  @MonotonicNonNullDecl private transient int[] successor;

  private transient int firstEntry;
  private transient int lastEntry;

  CompactLinkedHashSet() {
    super();
  }

  CompactLinkedHashSet(int expectedSize) {
    super(expectedSize);
  }

  @Override
  void init(int expectedSize, float loadFactor) {
    super.init(expectedSize, loadFactor);
    this.predecessor = new int[expectedSize];
    this.successor = new int[expectedSize];

    Arrays.fill(predecessor, UNSET);
    Arrays.fill(successor, UNSET);
    firstEntry = ENDPOINT;
    lastEntry = ENDPOINT;
  }

  private void succeeds(int pred, int succ) {
    if (pred == ENDPOINT) {
      firstEntry = succ;
    } else {
      successor[pred] = succ;
    }

    if (succ == ENDPOINT) {
      lastEntry = pred;
    } else {
      predecessor[succ] = pred;
    }
  }

  @Override
  void insertEntry(int entryIndex, E object, int hash) {
    super.insertEntry(entryIndex, object, hash);
    succeeds(lastEntry, entryIndex);
    succeeds(entryIndex, ENDPOINT);
  }

  @Override
  void moveEntry(int dstIndex) {
    int srcIndex = size() - 1;
    super.moveEntry(dstIndex);

    succeeds(predecessor[dstIndex], successor[dstIndex]);
    if (srcIndex != dstIndex) {
      succeeds(predecessor[srcIndex], dstIndex);
      succeeds(dstIndex, successor[srcIndex]);
    }
    predecessor[srcIndex] = UNSET;
    successor[srcIndex] = UNSET;
  }

  @Override
  public void clear() {
    super.clear();
    firstEntry = ENDPOINT;
    lastEntry = ENDPOINT;
    Arrays.fill(predecessor, UNSET);
    Arrays.fill(successor, UNSET);
  }

  @Override
  void resizeEntries(int newCapacity) {
    super.resizeEntries(newCapacity);
    int oldCapacity = predecessor.length;
    predecessor = Arrays.copyOf(predecessor, newCapacity);
    successor = Arrays.copyOf(successor, newCapacity);

    if (oldCapacity < newCapacity) {
      Arrays.fill(predecessor, oldCapacity, newCapacity, UNSET);
      Arrays.fill(successor, oldCapacity, newCapacity, UNSET);
    }
  }

  @Override
  public Object[] toArray() {
    return ObjectArrays.toArrayImpl(this);
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return ObjectArrays.toArrayImpl(this, a);
  }

  @Override
  int firstEntryIndex() {
    return firstEntry;
  }

  @Override
  int adjustAfterRemove(int indexBeforeRemove, int indexRemoved) {
    return (indexBeforeRemove == size()) ? indexRemoved : indexBeforeRemove;
  }

  @Override
  int getSuccessor(int entryIndex) {
    return successor[entryIndex];
  }
}
