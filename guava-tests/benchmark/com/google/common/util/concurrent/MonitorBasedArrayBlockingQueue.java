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

package com.google.common.util.concurrent;

import com.google.common.collect.ObjectArrays;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A bounded {@linkplain BlockingQueue blocking queue} backed by an array. This queue orders
 * elements FIFO (first-in-first-out). The <em>head</em> of the queue is that element that has been
 * on the queue the longest time. The <em>tail</em> of the queue is that element that has been on
 * the queue the shortest time. New elements are inserted at the tail of the queue, and the queue
 * retrieval operations obtain elements at the head of the queue.
 *
 * <p>This is a classic &quot;bounded buffer&quot;, in which a fixed-sized array holds elements
 * inserted by producers and extracted by consumers. Once created, the capacity cannot be increased.
 * Attempts to {@code put} an element into a full queue will result in the operation blocking;
 * attempts to {@code take} an element from an empty queue will similarly block.
 *
 * <p>This class supports an optional fairness policy for ordering waiting producer and consumer
 * threads. By default, this ordering is not guaranteed. However, a queue constructed with fairness
 * set to {@code true} grants threads access in FIFO order. Fairness generally decreases throughput
 * but reduces variability and avoids starvation.
 *
 * <p>This class and its iterator implement all of the <em>optional</em> methods of the {@link
 * Collection} and {@link Iterator} interfaces.
 *
 * @author Doug Lea
 * @author Justin T. Sampson
 * @param <E> the type of elements held in this collection
 */
@CanIgnoreReturnValue
public class MonitorBasedArrayBlockingQueue<E> extends AbstractQueue<E>
    implements BlockingQueue<E> {

  // Based on revision 1.58 of ArrayBlockingQueue by Doug Lea, from
  // http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/

  /** The queued items */
  final E[] items;
  /** items index for next take, poll or remove */
  int takeIndex;
  /** items index for next put, offer, or add. */
  int putIndex;
  /** Number of items in the queue */
  private int count;

  /*
   * Concurrency control uses the classic two-condition algorithm
   * found in any textbook.
   */

  /** Monitor guarding all access */
  final Monitor monitor;

  /** Guard for waiting takes */
  private final Monitor.Guard notEmpty;

  /** Guard for waiting puts */
  private final Monitor.Guard notFull;

  // Internal helper methods

  /** Circularly increment i. */
  final int inc(int i) {
    return (++i == items.length) ? 0 : i;
  }

  /**
   * Inserts element at current put position, advances, and signals. Call only when occupying
   * monitor.
   */
  private void insert(E x) {
    items[putIndex] = x;
    putIndex = inc(putIndex);
    ++count;
  }

  /**
   * Extracts element at current take position, advances, and signals. Call only when occupying
   * monitor.
   */
  private E extract() {
    final E[] items = this.items;
    E x = items[takeIndex];
    items[takeIndex] = null;
    takeIndex = inc(takeIndex);
    --count;
    return x;
  }

  /**
   * Utility for remove and iterator.remove: Delete item at position i. Call only when occupying
   * monitor.
   */
  void removeAt(int i) {
    final E[] items = this.items;
    // if removing front item, just advance
    if (i == takeIndex) {
      items[takeIndex] = null;
      takeIndex = inc(takeIndex);
    } else {
      // slide over all others up through putIndex.
      for (; ; ) {
        int nexti = inc(i);
        if (nexti != putIndex) {
          items[i] = items[nexti];
          i = nexti;
        } else {
          items[i] = null;
          putIndex = i;
          break;
        }
      }
    }
    --count;
  }

  /**
   * Creates an {@code MonitorBasedArrayBlockingQueue} with the given (fixed) capacity and default
   * access policy.
   *
   * @param capacity the capacity of this queue
   * @throws IllegalArgumentException if {@code capacity} is less than 1
   */
  public MonitorBasedArrayBlockingQueue(int capacity) {
    this(capacity, false);
  }

  /**
   * Creates an {@code MonitorBasedArrayBlockingQueue} with the given (fixed) capacity and the
   * specified access policy.
   *
   * @param capacity the capacity of this queue
   * @param fair if {@code true} then queue accesses for threads blocked on insertion or removal,
   *     are processed in FIFO order; if {@code false} the access order is unspecified.
   * @throws IllegalArgumentException if {@code capacity} is less than 1
   */
  public MonitorBasedArrayBlockingQueue(int capacity, boolean fair) {
    if (capacity <= 0) throw new IllegalArgumentException();
    this.items = newEArray(capacity);
    monitor = new Monitor(fair);
    notEmpty =
        new Monitor.Guard(monitor) {
          @Override
          public boolean isSatisfied() {
            return count > 0;
          }
        };
    notFull =
        new Monitor.Guard(monitor) {
          @Override
          public boolean isSatisfied() {
            return count < items.length;
          }
        };
  }

  /**
   * Creates an {@code MonitorBasedArrayBlockingQueue} with the given (fixed) capacity, the
   * specified access policy and initially containing the elements of the given collection, added in
   * traversal order of the collection's iterator.
   *
   * @param capacity the capacity of this queue
   * @param fair if {@code true} then queue accesses for threads blocked on insertion or removal,
   *     are processed in FIFO order; if {@code false} the access order is unspecified.
   * @param c the collection of elements to initially contain
   * @throws IllegalArgumentException if {@code capacity} is less than {@code c.size()}, or less
   *     than 1.
   * @throws NullPointerException if the specified collection or any of its elements are null
   */
  public MonitorBasedArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> c) {
    this(capacity, fair);
    if (capacity < c.size()) throw new IllegalArgumentException();

    for (E e : c) add(e);
  }

  @SuppressWarnings("unchecked") // please don't try this home, kids
  private static <E> E[] newEArray(int capacity) {
    return (E[]) new Object[capacity];
  }

  /**
   * Inserts the specified element at the tail of this queue if it is possible to do so immediately
   * without exceeding the queue's capacity, returning {@code true} upon success and throwing an
   * {@code IllegalStateException} if this queue is full.
   *
   * @param e the element to add
   * @return {@code true} (as specified by {@link Collection#add})
   * @throws IllegalStateException if this queue is full
   * @throws NullPointerException if the specified element is null
   */
  @Override
  public boolean add(E e) {
    return super.add(e);
  }

  /**
   * Inserts the specified element at the tail of this queue if it is possible to do so immediately
   * without exceeding the queue's capacity, returning {@code true} upon success and {@code false}
   * if this queue is full. This method is generally preferable to method {@link #add}, which can
   * fail to insert an element only by throwing an exception.
   *
   * @throws NullPointerException if the specified element is null
   */
  @Override
  public boolean offer(E e) {
    if (e == null) throw new NullPointerException();
    final Monitor monitor = this.monitor;
    if (monitor.enterIf(notFull)) {
      try {
        insert(e);
        return true;
      } finally {
        monitor.leave();
      }
    } else {
      return false;
    }
  }

  /**
   * Inserts the specified element at the tail of this queue, waiting up to the specified wait time
   * for space to become available if the queue is full.
   *
   * @throws InterruptedException {@inheritDoc}
   * @throws NullPointerException {@inheritDoc}
   */
  @Override
  public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {

    if (e == null) throw new NullPointerException();
    final Monitor monitor = this.monitor;
    if (monitor.enterWhen(notFull, timeout, unit)) {
      try {
        insert(e);
        return true;
      } finally {
        monitor.leave();
      }
    } else {
      return false;
    }
  }

  /**
   * Inserts the specified element at the tail of this queue, waiting for space to become available
   * if the queue is full.
   *
   * @throws InterruptedException {@inheritDoc}
   * @throws NullPointerException {@inheritDoc}
   */
  @Override
  public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    final Monitor monitor = this.monitor;
    monitor.enterWhen(notFull);
    try {
      insert(e);
    } finally {
      monitor.leave();
    }
  }

  @Override
  public E poll() {
    final Monitor monitor = this.monitor;
    if (monitor.enterIf(notEmpty)) {
      try {
        return extract();
      } finally {
        monitor.leave();
      }
    } else {
      return null;
    }
  }

  @Override
  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    final Monitor monitor = this.monitor;
    if (monitor.enterWhen(notEmpty, timeout, unit)) {
      try {
        return extract();
      } finally {
        monitor.leave();
      }
    } else {
      return null;
    }
  }

  @Override
  public E take() throws InterruptedException {
    final Monitor monitor = this.monitor;
    monitor.enterWhen(notEmpty);
    try {
      return extract();
    } finally {
      monitor.leave();
    }
  }

  @Override
  public E peek() {
    final Monitor monitor = this.monitor;
    if (monitor.enterIf(notEmpty)) {
      try {
        return items[takeIndex];
      } finally {
        monitor.leave();
      }
    } else {
      return null;
    }
  }

  // this doc comment is overridden to remove the reference to collections
  // greater in size than Integer.MAX_VALUE
  /**
   * Returns the number of elements in this queue.
   *
   * @return the number of elements in this queue
   */
  @Override
  public int size() {
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      return count;
    } finally {
      monitor.leave();
    }
  }

  // this doc comment is a modified copy of the inherited doc comment,
  // without the reference to unlimited queues.
  /**
   * Returns the number of additional elements that this queue can ideally (in the absence of memory
   * or resource constraints) accept without blocking. This is always equal to the initial capacity
   * of this queue less the current {@code size} of this queue.
   *
   * <p>Note that you <em>cannot</em> always tell if an attempt to insert an element will succeed by
   * inspecting {@code remainingCapacity} because it may be the case that another thread is about to
   * insert or remove an element.
   */
  @Override
  public int remainingCapacity() {
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      return items.length - count;
    } finally {
      monitor.leave();
    }
  }

  /**
   * Removes a single instance of the specified element from this queue, if it is present. More
   * formally, removes an element {@code e} such that {@code o.equals(e)}, if this queue contains
   * one or more such elements. Returns {@code true} if this queue contained the specified element
   * (or equivalently, if this queue changed as a result of the call).
   *
   * @param o element to be removed from this queue, if present
   * @return {@code true} if this queue changed as a result of the call
   */
  @Override
  public boolean remove(@Nullable Object o) {
    if (o == null) return false;
    final E[] items = this.items;
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      int i = takeIndex;
      int k = 0;
      for (; ; ) {
        if (k++ >= count) return false;
        if (o.equals(items[i])) {
          removeAt(i);
          return true;
        }
        i = inc(i);
      }
    } finally {
      monitor.leave();
    }
  }

  /**
   * Returns {@code true} if this queue contains the specified element. More formally, returns
   * {@code true} if and only if this queue contains at least one element {@code e} such that {@code
   * o.equals(e)}.
   *
   * @param o object to be checked for containment in this queue
   * @return {@code true} if this queue contains the specified element
   */
  @Override
  public boolean contains(@Nullable Object o) {
    if (o == null) return false;
    final E[] items = this.items;
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      int i = takeIndex;
      int k = 0;
      while (k++ < count) {
        if (o.equals(items[i])) return true;
        i = inc(i);
      }
      return false;
    } finally {
      monitor.leave();
    }
  }

  /**
   * Returns an array containing all of the elements in this queue, in proper sequence.
   *
   * <p>The returned array will be "safe" in that no references to it are maintained by this queue.
   * (In other words, this method must allocate a new array). The caller is thus free to modify the
   * returned array.
   *
   * <p>This method acts as bridge between array-based and collection-based APIs.
   *
   * @return an array containing all of the elements in this queue
   */
  @Override
  public Object[] toArray() {
    final E[] items = this.items;
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      Object[] a = new Object[count];
      int k = 0;
      int i = takeIndex;
      while (k < count) {
        a[k++] = items[i];
        i = inc(i);
      }
      return a;
    } finally {
      monitor.leave();
    }
  }

  /**
   * Returns an array containing all of the elements in this queue, in proper sequence; the runtime
   * type of the returned array is that of the specified array. If the queue fits in the specified
   * array, it is returned therein. Otherwise, a new array is allocated with the runtime type of the
   * specified array and the size of this queue.
   *
   * <p>If this queue fits in the specified array with room to spare (i.e., the array has more
   * elements than this queue), the element in the array immediately following the end of the queue
   * is set to {@code null}.
   *
   * <p>Like the {@link #toArray()} method, this method acts as bridge between array-based and
   * collection-based APIs. Further, this method allows precise control over the runtime type of the
   * output array, and may, under certain circumstances, be used to save allocation costs.
   *
   * <p>Suppose {@code x} is a queue known to contain only strings. The following code can be used
   * to dump the queue into a newly allocated array of {@code String}:
   *
   * <pre>
   *     String[] y = x.toArray(new String[0]);</pre>
   *
   * <p>Note that {@code toArray(new Object[0])} is identical in function to {@code toArray()}.
   *
   * @param a the array into which the elements of the queue are to be stored, if it is big enough;
   *     otherwise, a new array of the same runtime type is allocated for this purpose
   * @return an array containing all of the elements in this queue
   * @throws ArrayStoreException if the runtime type of the specified array is not a supertype of
   *     the runtime type of every element in this queue
   * @throws NullPointerException if the specified array is null
   */
  @Override
  public <T> T[] toArray(T[] a) {
    final E[] items = this.items;
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      if (a.length < count) a = ObjectArrays.newArray(a, count);

      int k = 0;
      int i = takeIndex;
      while (k < count) {
        // This cast is not itself safe, but the following statement
        // will fail if the runtime type of items[i] is not assignable
        // to the runtime type of a[k++], which is all that the method
        // contract requires (see @throws ArrayStoreException above).
        @SuppressWarnings("unchecked")
        T t = (T) items[i];
        a[k++] = t;
        i = inc(i);
      }
      if (a.length > count) a[count] = null;
      return a;
    } finally {
      monitor.leave();
    }
  }

  @Override
  public String toString() {
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      return super.toString();
    } finally {
      monitor.leave();
    }
  }

  /**
   * Atomically removes all of the elements from this queue. The queue will be empty after this call
   * returns.
   */
  @Override
  public void clear() {
    final E[] items = this.items;
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      int i = takeIndex;
      int k = count;
      while (k-- > 0) {
        items[i] = null;
        i = inc(i);
      }
      count = 0;
      putIndex = 0;
      takeIndex = 0;
    } finally {
      monitor.leave();
    }
  }

  /**
   * @throws UnsupportedOperationException {@inheritDoc}
   * @throws ClassCastException {@inheritDoc}
   * @throws NullPointerException {@inheritDoc}
   * @throws IllegalArgumentException {@inheritDoc}
   */
  @Override
  public int drainTo(Collection<? super E> c) {
    if (c == null) throw new NullPointerException();
    if (c == this) throw new IllegalArgumentException();
    final E[] items = this.items;
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      int i = takeIndex;
      int n = 0;
      int max = count;
      while (n < max) {
        c.add(items[i]);
        items[i] = null;
        i = inc(i);
        ++n;
      }
      if (n > 0) {
        count = 0;
        putIndex = 0;
        takeIndex = 0;
      }
      return n;
    } finally {
      monitor.leave();
    }
  }

  /**
   * @throws UnsupportedOperationException {@inheritDoc}
   * @throws ClassCastException {@inheritDoc}
   * @throws NullPointerException {@inheritDoc}
   * @throws IllegalArgumentException {@inheritDoc}
   */
  @Override
  public int drainTo(Collection<? super E> c, int maxElements) {
    if (c == null) throw new NullPointerException();
    if (c == this) throw new IllegalArgumentException();
    if (maxElements <= 0) return 0;
    final E[] items = this.items;
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      int i = takeIndex;
      int n = 0;
      int max = (maxElements < count) ? maxElements : count;
      while (n < max) {
        c.add(items[i]);
        items[i] = null;
        i = inc(i);
        ++n;
      }
      if (n > 0) {
        count -= n;
        takeIndex = i;
      }
      return n;
    } finally {
      monitor.leave();
    }
  }

  /**
   * Returns an iterator over the elements in this queue in proper sequence. The returned {@code
   * Iterator} is a "weakly consistent" iterator that will never throw {@link
   * ConcurrentModificationException}, and guarantees to traverse elements as they existed upon
   * construction of the iterator, and may (but is not guaranteed to) reflect any modifications
   * subsequent to construction.
   *
   * @return an iterator over the elements in this queue in proper sequence
   */
  @Override
  public Iterator<E> iterator() {
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      return new Itr();
    } finally {
      monitor.leave();
    }
  }

  /** Iterator for MonitorBasedArrayBlockingQueue */
  private class Itr implements Iterator<E> {
    /** Index of element to be returned by next, or a negative number if no such. */
    private int nextIndex;

    /**
     * nextItem holds on to item fields because once we claim that an element exists in hasNext(),
     * we must return it in the following next() call even if it was in the process of being removed
     * when hasNext() was called.
     */
    private E nextItem;

    /**
     * Index of element returned by most recent call to next. Reset to -1 if this element is deleted
     * by a call to remove.
     */
    private int lastRet;

    Itr() {
      lastRet = -1;
      if (count == 0) nextIndex = -1;
      else {
        nextIndex = takeIndex;
        nextItem = items[takeIndex];
      }
    }

    @Override
    public boolean hasNext() {
      /*
       * No sync. We can return true by mistake here
       * only if this iterator passed across threads,
       * which we don't support anyway.
       */
      return nextIndex >= 0;
    }

    /**
     * Checks whether nextIndex is valid; if so setting nextItem. Stops iterator when either hits
     * putIndex or sees null item.
     */
    private void checkNext() {
      if (nextIndex == putIndex) {
        nextIndex = -1;
        nextItem = null;
      } else {
        nextItem = items[nextIndex];
        if (nextItem == null) nextIndex = -1;
      }
    }

    @Override
    public E next() {
      final Monitor monitor = MonitorBasedArrayBlockingQueue.this.monitor;
      monitor.enter();
      try {
        if (nextIndex < 0) throw new NoSuchElementException();
        lastRet = nextIndex;
        E x = nextItem;
        nextIndex = inc(nextIndex);
        checkNext();
        return x;
      } finally {
        monitor.leave();
      }
    }

    @Override
    public void remove() {
      final Monitor monitor = MonitorBasedArrayBlockingQueue.this.monitor;
      monitor.enter();
      try {
        int i = lastRet;
        if (i == -1) throw new IllegalStateException();
        lastRet = -1;

        int ti = takeIndex;
        removeAt(i);
        // back up cursor (reset to front if was first element)
        nextIndex = (i == ti) ? takeIndex : i;
        checkNext();
      } finally {
        monitor.leave();
      }
    }
  }
}
