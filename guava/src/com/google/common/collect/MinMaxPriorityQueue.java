/*
 * Copyright (C) 2010 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.CollectPreconditions.checkRemove;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.IntMath;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * A double-ended priority queue, which provides constant-time access to both
 * its least element and its greatest element, as determined by the queue's
 * specified comparator. If no comparator is given at construction time, the
 * natural order of elements is used.
 *
 * <p>As a {@link Queue} it functions exactly as a {@link PriorityQueue}: its
 * head element -- the implicit target of the methods {@link #peek()}, {@link
 * #poll()} and {@link #remove()} -- is defined as the <i>least</i> element in
 * the queue according to the queue's comparator. But unlike a regular priority
 * queue, the methods {@link #peekLast}, {@link #pollLast} and
 * {@link #removeLast} are also provided, to act on the <i>greatest</i> element
 * in the queue instead.
 *
 * <p>A min-max priority queue can be configured with a maximum size. If so,
 * each time the size of the queue exceeds that value, the queue automatically
 * removes its greatest element according to its comparator (which might be the
 * element that was just added). This is different from conventional bounded
 * queues, which either block or reject new elements when full.
 *
 * <p>This implementation is based on the
 * <a href="http://portal.acm.org/citation.cfm?id=6621">min-max heap</a>
 * developed by Atkinson, et al. Unlike many other double-ended priority queues,
 * it stores elements in a single array, as compact as the traditional heap data
 * structure used in {@link PriorityQueue}.
 *
 * <p>This class is not thread-safe, and does not accept null elements.
 *
 * <p><i>Performance notes:</i>
 *
 * <ul>
 * <li>The retrieval operations {@link #peek}, {@link #peekFirst}, {@link
 *     #peekLast}, {@link #element}, and {@link #size} are constant-time
 * <li>The enqueing and dequeing operations ({@link #offer}, {@link #add}, and
 *     all the forms of {@link #poll} and {@link #remove()}) run in {@code
 *     O(log n) time}
 * <li>The {@link #remove(Object)} and {@link #contains} operations require
 *     linear ({@code O(n)}) time
 * <li>If you only access one end of the queue, and don't use a maximum size,
 *     this class is functionally equivalent to {@link PriorityQueue}, but
 *     significantly slower.
 * </ul>
 *
 * @author Sverre Sundsdal
 * @author Torbjorn Gannholm
 * @since 8.0
 */
// TODO(kevinb): GWT compatibility
@Beta
public final class MinMaxPriorityQueue<E> extends AbstractQueue<E> {

  /**
   * Creates a new min-max priority queue with default settings: natural order,
   * no maximum size, no initial contents, and an initial expected size of 11.
   */
  public static <E extends Comparable<E>> MinMaxPriorityQueue<E> create() {
    return new Builder<Comparable>(Ordering.natural()).create();
  }

  /**
   * Creates a new min-max priority queue using natural order, no maximum size,
   * and initially containing the given elements.
   */
  public static <E extends Comparable<E>> MinMaxPriorityQueue<E> create(
      Iterable<? extends E> initialContents) {
    return new Builder<E>(Ordering.<E>natural()).create(initialContents);
  }

  /**
   * Creates and returns a new builder, configured to build {@code
   * MinMaxPriorityQueue} instances that use {@code comparator} to determine the
   * least and greatest elements.
   */
  public static <B> Builder<B> orderedBy(Comparator<B> comparator) {
    return new Builder<B>(comparator);
  }

  /**
   * Creates and returns a new builder, configured to build {@code
   * MinMaxPriorityQueue} instances sized appropriately to hold {@code
   * expectedSize} elements.
   */
  public static Builder<Comparable> expectedSize(int expectedSize) {
    return new Builder<Comparable>(Ordering.natural())
        .expectedSize(expectedSize);
  }

  /**
   * Creates and returns a new builder, configured to build {@code
   * MinMaxPriorityQueue} instances that are limited to {@code maximumSize}
   * elements. Each time a queue grows beyond this bound, it immediately
   * removes its greatest element (according to its comparator), which might be
   * the element that was just added.
   */
  public static Builder<Comparable> maximumSize(int maximumSize) {
    return new Builder<Comparable>(Ordering.natural())
        .maximumSize(maximumSize);
  }

  /**
   * The builder class used in creation of min-max priority queues. Instead of
   * constructing one directly, use {@link
   * MinMaxPriorityQueue#orderedBy(Comparator)}, {@link
   * MinMaxPriorityQueue#expectedSize(int)} or {@link
   * MinMaxPriorityQueue#maximumSize(int)}.
   *
   * @param <B> the upper bound on the eventual type that can be produced by
   *     this builder (for example, a {@code Builder<Number>} can produce a
   *     {@code Queue<Number>} or {@code Queue<Integer>} but not a {@code
   *     Queue<Object>}).
   * @since 8.0
   */
  @Beta
  public static final class Builder<B> {
    /*
     * TODO(kevinb): when the dust settles, see if we still need this or can
     * just default to DEFAULT_CAPACITY.
     */
    private static final int UNSET_EXPECTED_SIZE = -1;

    private final Comparator<B> comparator;
    private int expectedSize = UNSET_EXPECTED_SIZE;
    private int maximumSize = Integer.MAX_VALUE;

    private Builder(Comparator<B> comparator) {
      this.comparator = checkNotNull(comparator);
    }

    /**
     * Configures this builder to build min-max priority queues with an initial
     * expected size of {@code expectedSize}.
     */
    public Builder<B> expectedSize(int expectedSize) {
      checkArgument(expectedSize >= 0);
      this.expectedSize = expectedSize;
      return this;
    }

    /**
     * Configures this builder to build {@code MinMaxPriorityQueue} instances
     * that are limited to {@code maximumSize} elements. Each time a queue grows
     * beyond this bound, it immediately removes its greatest element (according
     * to its comparator), which might be the element that was just added.
     */
    public Builder<B> maximumSize(int maximumSize) {
      checkArgument(maximumSize > 0);
      this.maximumSize = maximumSize;
      return this;
    }

    /**
     * Builds a new min-max priority queue using the previously specified
     * options, and having no initial contents.
     */
    public <T extends B> MinMaxPriorityQueue<T> create() {
      return create(Collections.<T>emptySet());
    }

    /**
     * Builds a new min-max priority queue using the previously specified
     * options, and having the given initial elements.
     */
    public <T extends B> MinMaxPriorityQueue<T> create(
        Iterable<? extends T> initialContents) {
      MinMaxPriorityQueue<T> queue = new MinMaxPriorityQueue<T>(
          this, initialQueueSize(expectedSize, maximumSize, initialContents));
      for (T element : initialContents) {
        queue.offer(element);
      }
      return queue;
    }

    @SuppressWarnings("unchecked") // safe "contravariant cast"
    private <T extends B> Ordering<T> ordering() {
      return Ordering.from((Comparator<T>) comparator);
    }
  }

  private final Heap minHeap;
  private final Heap maxHeap;
  @VisibleForTesting final int maximumSize;
  private Object[] queue;
  private int size;
  private int modCount;

  private MinMaxPriorityQueue(Builder<? super E> builder, int queueSize) {
    Ordering<E> ordering = builder.ordering();
    this.minHeap = new Heap(ordering);
    this.maxHeap = new Heap(ordering.reverse());
    minHeap.otherHeap = maxHeap;
    maxHeap.otherHeap = minHeap;

    this.maximumSize = builder.maximumSize;
    // TODO(kevinb): pad?
    this.queue = new Object[queueSize];
  }

  @Override public int size() {
    return size;
  }

  /**
   * Adds the given element to this queue. If this queue has a maximum size,
   * after adding {@code element} the queue will automatically evict its
   * greatest element (according to its comparator), which may be {@code
   * element} itself.
   *
   * @return {@code true} always
   */
  @Override public boolean add(E element) {
    offer(element);
    return true;
  }

  @Override public boolean addAll(Collection<? extends E> newElements) {
    boolean modified = false;
    for (E element : newElements) {
      offer(element);
      modified = true;
    }
    return modified;
  }

  /**
   * Adds the given element to this queue. If this queue has a maximum size,
   * after adding {@code element} the queue will automatically evict its
   * greatest element (according to its comparator), which may be {@code
   * element} itself.
   */
  @Override public boolean offer(E element) {
    checkNotNull(element);
    modCount++;
    int insertIndex = size++;

    growIfNeeded();

    // Adds the element to the end of the heap and bubbles it up to the correct
    // position.
    heapForIndex(insertIndex).bubbleUp(insertIndex, element);
    return size <= maximumSize || pollLast() != element;
  }

  @Override public E poll() {
    return isEmpty() ? null : removeAndGet(0);
  }

  @SuppressWarnings("unchecked") // we must carefully only allow Es to get in
  E elementData(int index) {
    return (E) queue[index];
  }

  @Override public E peek() {
    return isEmpty() ? null : elementData(0);
  }

  /**
   * Returns the index of the max element.
   */
  private int getMaxElementIndex() {
    switch (size) {
      case 1:
        return 0; // The lone element in the queue is the maximum.
      case 2:
        return 1; // The lone element in the maxHeap is the maximum.
      default:
        // The max element must sit on the first level of the maxHeap. It is
        // actually the *lesser* of the two from the maxHeap's perspective.
        return (maxHeap.compareElements(1, 2) <= 0) ? 1 : 2;
    }
  }

  /**
   * Removes and returns the least element of this queue, or returns {@code
   * null} if the queue is empty.
   */
  public E pollFirst() {
    return poll();
  }

  /**
   * Removes and returns the least element of this queue.
   *
   * @throws NoSuchElementException if the queue is empty
   */
  public E removeFirst() {
    return remove();
  }

  /**
   * Retrieves, but does not remove, the least element of this queue, or returns
   * {@code null} if the queue is empty.
   */
  public E peekFirst() {
    return peek();
  }

  /**
   * Removes and returns the greatest element of this queue, or returns {@code
   * null} if the queue is empty.
   */
  public E pollLast() {
    return isEmpty() ? null : removeAndGet(getMaxElementIndex());
  }

  /**
   * Removes and returns the greatest element of this queue.
   *
   * @throws NoSuchElementException if the queue is empty
   */
  public E removeLast() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return removeAndGet(getMaxElementIndex());
  }

  /**
   * Retrieves, but does not remove, the greatest element of this queue, or
   * returns {@code null} if the queue is empty.
   */
  public E peekLast() {
    return isEmpty() ? null : elementData(getMaxElementIndex());
  }

  /**
   * Removes the element at position {@code index}.
   *
   * <p>Normally this method leaves the elements at up to {@code index - 1},
   * inclusive, untouched.  Under these circumstances, it returns {@code null}.
   *
   * <p>Occasionally, in order to maintain the heap invariant, it must swap a
   * later element of the list with one before {@code index}. Under these
   * circumstances it returns a pair of elements as a {@link MoveDesc}. The
   * first one is the element that was previously at the end of the heap and is
   * now at some position before {@code index}. The second element is the one
   * that was swapped down to replace the element at {@code index}. This fact is
   * used by iterator.remove so as to visit elements during a traversal once and
   * only once.
   */
  @VisibleForTesting MoveDesc<E> removeAt(int index) {
    checkPositionIndex(index, size);
    modCount++;
    size--;
    if (size == index) {
      queue[size] = null;
      return null;
    }
    E actualLastElement = elementData(size);
    int lastElementAt = heapForIndex(size)
        .getCorrectLastElement(actualLastElement);
    E toTrickle = elementData(size);
    queue[size] = null;
    MoveDesc<E> changes = fillHole(index, toTrickle);
    if (lastElementAt < index) {
      // Last element is moved to before index, swapped with trickled element.
      if (changes == null) {
        // The trickled element is still after index.
        return new MoveDesc<E>(actualLastElement, toTrickle);
      } else {
        // The trickled element is back before index, but the replaced element
        // has now been moved after index.
        return new MoveDesc<E>(actualLastElement, changes.replaced);
      }
    }
    // Trickled element was after index to begin with, no adjustment needed.
    return changes;
  }

  private MoveDesc<E> fillHole(int index, E toTrickle) {
    Heap heap = heapForIndex(index);
    // We consider elementData(index) a "hole", and we want to fill it
    // with the last element of the heap, toTrickle.
    // Since the last element of the heap is from the bottom level, we
    // optimistically fill index position with elements from lower levels,
    // moving the hole down. In most cases this reduces the number of
    // comparisons with toTrickle, but in some cases we will need to bubble it
    // all the way up again.
    int vacated = heap.fillHoleAt(index);
    // Try to see if toTrickle can be bubbled up min levels.
    int bubbledTo = heap.bubbleUpAlternatingLevels(vacated, toTrickle);
    if (bubbledTo == vacated) {
      // Could not bubble toTrickle up min levels, try moving
      // it from min level to max level (or max to min level) and bubble up
      // there.
      return heap.tryCrossOverAndBubbleUp(index, vacated, toTrickle);
    } else {
      return (bubbledTo < index)
          ? new MoveDesc<E>(toTrickle, elementData(index))
          : null;
    }
  }

  // Returned from removeAt() to iterator.remove()
  static class MoveDesc<E> {
    final E toTrickle;
    final E replaced;

    MoveDesc(E toTrickle, E replaced) {
      this.toTrickle = toTrickle;
      this.replaced = replaced;
    }
  }

  /**
   * Removes and returns the value at {@code index}.
   */
  private E removeAndGet(int index) {
    E value = elementData(index);
    removeAt(index);
    return value;
  }

  private Heap heapForIndex(int i) {
    return isEvenLevel(i) ? minHeap : maxHeap;
  }

  private static final int EVEN_POWERS_OF_TWO = 0x55555555;
  private static final int ODD_POWERS_OF_TWO = 0xaaaaaaaa;

  @VisibleForTesting static boolean isEvenLevel(int index) {
    int oneBased = index + 1;
    checkState(oneBased > 0, "negative index");
    return (oneBased & EVEN_POWERS_OF_TWO) > (oneBased & ODD_POWERS_OF_TWO);
  }

  /**
   * Returns {@code true} if the MinMax heap structure holds. This is only used
   * in testing.
   *
   * TODO(kevinb): move to the test class?
   */
  @VisibleForTesting boolean isIntact() {
    for (int i = 1; i < size; i++) {
      if (!heapForIndex(i).verifyIndex(i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Each instance of MinMaxPriortyQueue encapsulates two instances of Heap:
   * a min-heap and a max-heap. Conceptually, these might each have their own
   * array for storage, but for efficiency's sake they are stored interleaved on
   * alternate heap levels in the same array (MMPQ.queue).
   */
  private class Heap {
    final Ordering<E> ordering;
    Heap otherHeap;

    Heap(Ordering<E> ordering) {
      this.ordering = ordering;
    }

    int compareElements(int a, int b) {
      return ordering.compare(elementData(a), elementData(b));
    }

    /**
     * Tries to move {@code toTrickle} from a min to a max level and
     * bubble up there. If it moved before {@code removeIndex} this method
     * returns a pair as described in {@link #removeAt}.
     */
    MoveDesc<E> tryCrossOverAndBubbleUp(
        int removeIndex, int vacated, E toTrickle) {
      int crossOver = crossOver(vacated, toTrickle);
      if (crossOver == vacated) {
        return null;
      }
      // Successfully crossed over from min to max.
      // Bubble up max levels.
      E parent;
      // If toTrickle is moved up to a parent of removeIndex, the parent is
      // placed in removeIndex position. We must return that to the iterator so
      // that it knows to skip it.
      if (crossOver < removeIndex) {
        // We crossed over to the parent level in crossOver, so the parent
        // has already been moved.
        parent = elementData(removeIndex);
      } else {
        parent = elementData(getParentIndex(removeIndex));
      }
      // bubble it up the opposite heap
      if (otherHeap.bubbleUpAlternatingLevels(crossOver, toTrickle)
          < removeIndex) {
        return new MoveDesc<E>(toTrickle, parent);
      } else {
        return null;
      }
    }

    /**
     * Bubbles a value from {@code index} up the appropriate heap if required.
     */
    void bubbleUp(int index, E x) {
      int crossOver = crossOverUp(index, x);

      Heap heap;
      if (crossOver == index) {
        heap = this;
      } else {
        index = crossOver;
        heap = otherHeap;
      }
      heap.bubbleUpAlternatingLevels(index, x);
    }

    /**
     * Bubbles a value from {@code index} up the levels of this heap, and
     * returns the index the element ended up at.
     */
    int bubbleUpAlternatingLevels(int index, E x) {
      while (index > 2) {
        int grandParentIndex = getGrandparentIndex(index);
        E e = elementData(grandParentIndex);
        if (ordering.compare(e, x) <= 0) {
          break;
        }
        queue[index] = e;
        index = grandParentIndex;
      }
      queue[index] = x;
      return index;
    }

    /**
     * Returns the index of minimum value between {@code index} and
     * {@code index + len}, or {@code -1} if {@code index} is greater than
     * {@code size}.
     */
    int findMin(int index, int len) {
      if (index >= size) {
        return -1;
      }
      checkState(index > 0);
      int limit = Math.min(index, size - len) + len;
      int minIndex = index;
      for (int i = index + 1; i < limit; i++) {
        if (compareElements(i, minIndex) < 0) {
          minIndex = i;
        }
      }
      return minIndex;
    }

    /**
     * Returns the minimum child or {@code -1} if no child exists.
     */
    int findMinChild(int index) {
      return findMin(getLeftChildIndex(index), 2);
    }

    /**
     * Returns the minimum grand child or -1 if no grand child exists.
     */
    int findMinGrandChild(int index) {
      int leftChildIndex = getLeftChildIndex(index);
      if (leftChildIndex < 0) {
        return -1;
      }
      return findMin(getLeftChildIndex(leftChildIndex), 4);
    }

    /**
     * Moves an element one level up from a min level to a max level
     * (or vice versa).
     * Returns the new position of the element.
     */
    int crossOverUp(int index, E x) {
      if (index == 0) {
        queue[0] = x;
        return 0;
      }
      int parentIndex = getParentIndex(index);
      E parentElement = elementData(parentIndex);
      if (parentIndex != 0) {
        // This is a guard for the case of the childless uncle.
        // Since the end of the array is actually the middle of the heap,
        // a smaller childless uncle can become a child of x when we
        // bubble up alternate levels, violating the invariant.
        int grandparentIndex = getParentIndex(parentIndex);
        int uncleIndex = getRightChildIndex(grandparentIndex);
        if (uncleIndex != parentIndex
            && getLeftChildIndex(uncleIndex) >= size) {
          E uncleElement = elementData(uncleIndex);
          if (ordering.compare(uncleElement, parentElement) < 0) {
            parentIndex = uncleIndex;
            parentElement = uncleElement;
          }
        }
      }
      if (ordering.compare(parentElement, x) < 0) {
        queue[index] = parentElement;
        queue[parentIndex] = x;
        return parentIndex;
      }
      queue[index] = x;
      return index;
    }

    /**
     * Returns the conceptually correct last element of the heap.
     *
     * <p>Since the last element of the array is actually in the
     * middle of the sorted structure, a childless uncle node could be
     * smaller, which would corrupt the invariant if this element
     * becomes the new parent of the uncle. In that case, we first
     * switch the last element with its uncle, before returning.
     */
    int getCorrectLastElement(E actualLastElement) {
      int parentIndex = getParentIndex(size);
      if (parentIndex != 0) {
        int grandparentIndex = getParentIndex(parentIndex);
        int uncleIndex = getRightChildIndex(grandparentIndex);
        if (uncleIndex != parentIndex
            && getLeftChildIndex(uncleIndex) >= size) {
          E uncleElement = elementData(uncleIndex);
          if (ordering.compare(uncleElement, actualLastElement) < 0) {
            queue[uncleIndex] = actualLastElement;
            queue[size] = uncleElement;
            return uncleIndex;
          }
        }
      }
      return size;
    }

    /**
     * Crosses an element over to the opposite heap by moving it one level down
     * (or up if there are no elements below it).
     *
     * Returns the new position of the element.
     */
    int crossOver(int index, E x) {
      int minChildIndex = findMinChild(index);
      // TODO(kevinb): split the && into two if's and move crossOverUp so it's
      // only called when there's no child.
      if ((minChildIndex > 0)
          && (ordering.compare(elementData(minChildIndex), x) < 0)) {
        queue[index] = elementData(minChildIndex);
        queue[minChildIndex] = x;
        return minChildIndex;
      }
      return crossOverUp(index, x);
    }

    /**
     * Fills the hole at {@code index} by moving in the least of its
     * grandchildren to this position, then recursively filling the new hole
     * created.
     *
     * @return the position of the new hole (where the lowest grandchild moved
     *     from, that had no grandchild to replace it)
     */
    int fillHoleAt(int index) {
      int minGrandchildIndex;
      while ((minGrandchildIndex = findMinGrandChild(index)) > 0) {
        queue[index] = elementData(minGrandchildIndex);
        index = minGrandchildIndex;
      }
      return index;
    }

    private boolean verifyIndex(int i) {
      if ((getLeftChildIndex(i) < size)
          && (compareElements(i, getLeftChildIndex(i)) > 0)) {
        return false;
      }
      if ((getRightChildIndex(i) < size)
          && (compareElements(i, getRightChildIndex(i)) > 0)) {
        return false;
      }
      if ((i > 0) && (compareElements(i, getParentIndex(i)) > 0)) {
        return false;
      }
      if ((i > 2) && (compareElements(getGrandparentIndex(i), i) > 0)) {
        return false;
      }
      return true;
    }

    // These would be static if inner classes could have static members.

    private int getLeftChildIndex(int i) {
      return i * 2 + 1;
    }

    private int getRightChildIndex(int i) {
      return i * 2 + 2;
    }

    private int getParentIndex(int i) {
      return (i - 1) / 2;
    }

    private int getGrandparentIndex(int i) {
      return getParentIndex(getParentIndex(i)); // (i - 3) / 4
    }
  }

  /**
   * Iterates the elements of the queue in no particular order.
   *
   * If the underlying queue is modified during iteration an exception will be
   * thrown.
   */
  private class QueueIterator implements Iterator<E> {
    private int cursor = -1;
    private int expectedModCount = modCount;
    private Queue<E> forgetMeNot;
    private List<E> skipMe;
    private E lastFromForgetMeNot;
    private boolean canRemove;

    @Override public boolean hasNext() {
      checkModCount();
      return (nextNotInSkipMe(cursor + 1) < size())
          || ((forgetMeNot != null) && !forgetMeNot.isEmpty());
    }

    @Override public E next() {
      checkModCount();
      int tempCursor = nextNotInSkipMe(cursor + 1);
      if (tempCursor < size()) {
        cursor = tempCursor;
        canRemove = true;
        return elementData(cursor);
      } else if (forgetMeNot != null) {
        cursor = size();
        lastFromForgetMeNot = forgetMeNot.poll();
        if (lastFromForgetMeNot != null) {
          canRemove = true;
          return lastFromForgetMeNot;
        }
      }
      throw new NoSuchElementException(
          "iterator moved past last element in queue.");
    }

    @Override public void remove() {
      checkRemove(canRemove);
      checkModCount();
      canRemove = false;
      expectedModCount++;
      if (cursor < size()) {
        MoveDesc<E> moved = removeAt(cursor);
        if (moved != null) {
          if (forgetMeNot == null) {
            forgetMeNot = new ArrayDeque<E>();
            skipMe = new ArrayList<E>(3);
          }
          forgetMeNot.add(moved.toTrickle);
          skipMe.add(moved.replaced);
        }
        cursor--;
      } else { // we must have set lastFromForgetMeNot in next()
        checkState(removeExact(lastFromForgetMeNot));
        lastFromForgetMeNot = null;
      }
    }

    // Finds only this exact instance, not others that are equals()
    private boolean containsExact(Iterable<E> elements, E target) {
      for (E element : elements) {
        if (element == target) {
          return true;
        }
      }
      return false;
    }

    // Removes only this exact instance, not others that are equals()
    boolean removeExact(Object target) {
      for (int i = 0; i < size; i++) {
        if (queue[i] == target) {
          removeAt(i);
          return true;
        }
      }
      return false;
    }

    void checkModCount() {
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
    }

    /**
     * Returns the index of the first element after {@code c} that is not in
     * {@code skipMe} and returns {@code size()} if there is no such element.
     */
    private int nextNotInSkipMe(int c) {
      if (skipMe != null) {
        while (c < size() && containsExact(skipMe, elementData(c))) {
          c++;
        }
      }
      return c;
    }
  }

  /**
   * Returns an iterator over the elements contained in this collection,
   * <i>in no particular order</i>.
   *
   * <p>The iterator is <i>fail-fast</i>: If the MinMaxPriorityQueue is modified
   * at any time after the iterator is created, in any way except through the
   * iterator's own remove method, the iterator will generally throw a
   * {@link ConcurrentModificationException}. Thus, in the face of concurrent
   * modification, the iterator fails quickly and cleanly, rather than risking
   * arbitrary, non-deterministic behavior at an undetermined time in the
   * future.
   *
   * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
   * as it is, generally speaking, impossible to make any hard guarantees in the
   * presence of unsynchronized concurrent modification.  Fail-fast iterators
   * throw {@code ConcurrentModificationException} on a best-effort basis.
   * Therefore, it would be wrong to write a program that depended on this
   * exception for its correctness: <i>the fail-fast behavior of iterators
   * should be used only to detect bugs.</i>
   *
   * @return an iterator over the elements contained in this collection
   */
  @Override public Iterator<E> iterator() {
    return new QueueIterator();
  }

  @Override public void clear() {
    for (int i = 0; i < size; i++) {
      queue[i] = null;
    }
    size = 0;
  }

  @Override public Object[] toArray() {
    Object[] copyTo = new Object[size];
    System.arraycopy(queue, 0, copyTo, 0, size);
    return copyTo;
  }

  /**
   * Returns the comparator used to order the elements in this queue. Obeys the
   * general contract of {@link PriorityQueue#comparator}, but returns {@link
   * Ordering#natural} instead of {@code null} to indicate natural ordering.
   */
  public Comparator<? super E> comparator() {
    return minHeap.ordering;
  }

  @VisibleForTesting int capacity() {
    return queue.length;
  }

  // Size/capacity-related methods

  private static final int DEFAULT_CAPACITY = 11;

  @VisibleForTesting static int initialQueueSize(int configuredExpectedSize,
      int maximumSize, Iterable<?> initialContents) {
    // Start with what they said, if they said it, otherwise DEFAULT_CAPACITY
    int result = (configuredExpectedSize == Builder.UNSET_EXPECTED_SIZE)
        ? DEFAULT_CAPACITY
        : configuredExpectedSize;

    // Enlarge to contain initial contents
    if (initialContents instanceof Collection) {
      int initialSize = ((Collection<?>) initialContents).size();
      result = Math.max(result, initialSize);
    }

    // Now cap it at maxSize + 1
    return capAtMaximumSize(result, maximumSize);
  }

  private void growIfNeeded() {
    if (size > queue.length) {
      int newCapacity = calculateNewCapacity();
      Object[] newQueue = new Object[newCapacity];
      System.arraycopy(queue, 0, newQueue, 0, queue.length);
      queue = newQueue;
    }
  }

  /** Returns ~2x the old capacity if small; ~1.5x otherwise. */
  private int calculateNewCapacity() {
    int oldCapacity = queue.length;
    int newCapacity = (oldCapacity < 64)
        ? (oldCapacity + 1) * 2
        : IntMath.checkedMultiply(oldCapacity / 2, 3);
    return capAtMaximumSize(newCapacity, maximumSize);
  }

  /** There's no reason for the queueSize to ever be more than maxSize + 1 */
  private static int capAtMaximumSize(int queueSize, int maximumSize) {
    return Math.min(queueSize - 1, maximumSize) + 1; // don't overflow
  }
}
