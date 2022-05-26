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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An unbounded {@linkplain BlockingQueue blocking queue} that uses the same ordering rules as class
 * {@link PriorityQueue} and supplies blocking retrieval operations. While this queue is logically
 * unbounded, attempted additions may fail due to resource exhaustion (causing {@code
 * OutOfMemoryError}). This class does not permit {@code null} elements. A priority queue relying on
 * {@linkplain Comparable natural ordering} also does not permit insertion of non-comparable objects
 * (doing so results in {@code ClassCastException}).
 *
 * <p>This class and its iterator implement all of the <em>optional</em> methods of the {@link
 * Collection} and {@link Iterator} interfaces. The Iterator provided in method {@link #iterator()}
 * is <em>not</em> guaranteed to traverse the elements of the MonitorBasedPriorityBlockingQueue in
 * any particular order. If you need ordered traversal, consider using {@code
 * Arrays.sort(pq.toArray())}. Also, method {@code drainTo} can be used to <em>remove</em> some or
 * all elements in priority order and place them in another collection.
 *
 * <p>Operations on this class make no guarantees about the ordering of elements with equal
 * priority. If you need to enforce an ordering, you can define custom classes or comparators that
 * use a secondary key to break ties in primary priority values. For example, here is a class that
 * applies first-in-first-out tie-breaking to comparable elements. To use it, you would insert a
 * {@code new FIFOEntry(anEntry)} instead of a plain entry object.
 *
 * <pre>
 * class FIFOEntry&lt;E extends Comparable&lt;? super E&gt;&gt;
 *     implements Comparable&lt;FIFOEntry&lt;E&gt;&gt; {
 *   final static AtomicLong seq = new AtomicLong();
 *   final long seqNum;
 *   final E entry;
 *   public FIFOEntry(E entry) {
 *     seqNum = seq.getAndIncrement();
 *     this.entry = entry;
 *   }
 *   public E getEntry() { return entry; }
 *   public int compareTo(FIFOEntry&lt;E&gt; other) {
 *     int res = entry.compareTo(other.entry);
 *     if (res == 0 &amp;&amp; other.entry != this.entry)
 *       res = (seqNum &lt; other.seqNum ? -1 : 1);
 *     return res;
 *   }
 * }</pre>
 *
 * @author Doug Lea
 * @author Justin T. Sampson
 * @param <E> the type of elements held in this collection
 */
@CanIgnoreReturnValue // TODO(cpovirk): Consider being more strict.
public class MonitorBasedPriorityBlockingQueue<E> extends AbstractQueue<E>
    implements BlockingQueue<E> {

  // Based on revision 1.55 of PriorityBlockingQueue by Doug Lea, from
  // http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/

  private static final long serialVersionUID = 5595510919245408276L;

  final PriorityQueue<E> q;
  final Monitor monitor = new Monitor(true);
  private final Monitor.Guard notEmpty =
      new Monitor.Guard(monitor) {
        @Override
        public boolean isSatisfied() {
          return !q.isEmpty();
        }
      };

  /**
   * Creates a {@code MonitorBasedPriorityBlockingQueue} with the default initial capacity (11) that
   * orders its elements according to their {@linkplain Comparable natural ordering}.
   */
  public MonitorBasedPriorityBlockingQueue() {
    q = new PriorityQueue<E>();
  }

  /**
   * Creates a {@code MonitorBasedPriorityBlockingQueue} with the specified initial capacity that
   * orders its elements according to their {@linkplain Comparable natural ordering}.
   *
   * @param initialCapacity the initial capacity for this priority queue
   * @throws IllegalArgumentException if {@code initialCapacity} is less than 1
   */
  public MonitorBasedPriorityBlockingQueue(int initialCapacity) {
    q = new PriorityQueue<E>(initialCapacity, null);
  }

  /**
   * Creates a {@code MonitorBasedPriorityBlockingQueue} with the specified initial capacity that
   * orders its elements according to the specified comparator.
   *
   * @param initialCapacity the initial capacity for this priority queue
   * @param comparator the comparator that will be used to order this priority queue. If {@code
   *     null}, the {@linkplain Comparable natural ordering} of the elements will be used.
   * @throws IllegalArgumentException if {@code initialCapacity} is less than 1
   */
  public MonitorBasedPriorityBlockingQueue(
      int initialCapacity, @Nullable Comparator<? super E> comparator) {
    q = new PriorityQueue<E>(initialCapacity, comparator);
  }

  /**
   * Creates a {@code MonitorBasedPriorityBlockingQueue} containing the elements in the specified
   * collection. If the specified collection is a {@link SortedSet} or a {@link PriorityQueue}, this
   * priority queue will be ordered according to the same ordering. Otherwise, this priority queue
   * will be ordered according to the {@linkplain Comparable natural ordering} of its elements.
   *
   * @param c the collection whose elements are to be placed into this priority queue
   * @throws ClassCastException if elements of the specified collection cannot be compared to one
   *     another according to the priority queue's ordering
   * @throws NullPointerException if the specified collection or any of its elements are null
   */
  public MonitorBasedPriorityBlockingQueue(Collection<? extends E> c) {
    q = new PriorityQueue<E>(c);
  }

  /**
   * Inserts the specified element into this priority queue.
   *
   * @param e the element to add
   * @return {@code true} (as specified by {@link Collection#add})
   * @throws ClassCastException if the specified element cannot be compared with elements currently
   *     in the priority queue according to the priority queue's ordering
   * @throws NullPointerException if the specified element is null
   */
  @Override
  public boolean add(E e) {
    return offer(e);
  }

  /**
   * Inserts the specified element into this priority queue.
   *
   * @param e the element to add
   * @return {@code true} (as specified by {@link Queue#offer})
   * @throws ClassCastException if the specified element cannot be compared with elements currently
   *     in the priority queue according to the priority queue's ordering
   * @throws NullPointerException if the specified element is null
   */
  @Override
  public boolean offer(E e) {
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      boolean ok = q.offer(e);
      if (!ok) {
        throw new AssertionError();
      }
      return true;
    } finally {
      monitor.leave();
    }
  }

  /**
   * Inserts the specified element into this priority queue. As the queue is unbounded this method
   * will never block.
   *
   * @param e the element to add
   * @param timeout This parameter is ignored as the method never blocks
   * @param unit This parameter is ignored as the method never blocks
   * @return {@code true}
   * @throws ClassCastException if the specified element cannot be compared with elements currently
   *     in the priority queue according to the priority queue's ordering
   * @throws NullPointerException if the specified element is null
   */
  @Override
  public boolean offer(E e, long timeout, TimeUnit unit) {
    checkNotNull(unit);
    return offer(e); // never need to block
  }

  /**
   * Inserts the specified element into this priority queue. As the queue is unbounded this method
   * will never block.
   *
   * @param e the element to add
   * @throws ClassCastException if the specified element cannot be compared with elements currently
   *     in the priority queue according to the priority queue's ordering
   * @throws NullPointerException if the specified element is null
   */
  @Override
  public void put(E e) {
    offer(e); // never need to block
  }

  @Override
  public E poll() {
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      return q.poll();
    } finally {
      monitor.leave();
    }
  }

  @Override
  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    final Monitor monitor = this.monitor;
    if (monitor.enterWhen(notEmpty, timeout, unit)) {
      try {
        return q.poll();
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
      return q.poll();
    } finally {
      monitor.leave();
    }
  }

  @Override
  public E peek() {
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      return q.peek();
    } finally {
      monitor.leave();
    }
  }

  /**
   * Returns the comparator used to order the elements in this queue, or {@code null} if this queue
   * uses the {@linkplain Comparable natural ordering} of its elements.
   *
   * @return the comparator used to order the elements in this queue, or {@code null} if this queue
   *     uses the natural ordering of its elements
   */
  public Comparator<? super E> comparator() {
    return q.comparator();
  }

  @Override
  public int size() {
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      return q.size();
    } finally {
      monitor.leave();
    }
  }

  /**
   * Always returns {@code Integer.MAX_VALUE} because a {@code MonitorBasedPriorityBlockingQueue} is
   * not capacity constrained.
   *
   * @return {@code Integer.MAX_VALUE}
   */
  @Override
  public int remainingCapacity() {
    return Integer.MAX_VALUE;
  }

  /**
   * Removes a single instance of the specified element from this queue, if it is present. More
   * formally, removes an element {@code e} such that {@code o.equals(e)}, if this queue contains
   * one or more such elements. Returns {@code true} if and only if this queue contained the
   * specified element (or equivalently, if this queue changed as a result of the call).
   *
   * @param o element to be removed from this queue, if present
   * @return {@code true} if this queue changed as a result of the call
   */
  @Override
  public boolean remove(@Nullable Object o) {
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      return q.remove(o);
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
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      return q.contains(o);
    } finally {
      monitor.leave();
    }
  }

  /**
   * Returns an array containing all of the elements in this queue. The returned array elements are
   * in no particular order.
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
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      return q.toArray();
    } finally {
      monitor.leave();
    }
  }

  /**
   * Returns an array containing all of the elements in this queue; the runtime type of the returned
   * array is that of the specified array. The returned array elements are in no particular order.
   * If the queue fits in the specified array, it is returned therein. Otherwise, a new array is
   * allocated with the runtime type of the specified array and the size of this queue.
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
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      return q.toArray(a);
    } finally {
      monitor.leave();
    }
  }

  @Override
  public String toString() {
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      return q.toString();
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
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      int n = 0;
      E e;
      while ((e = q.poll()) != null) {
        c.add(e);
        ++n;
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
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      int n = 0;
      E e;
      while (n < maxElements && (e = q.poll()) != null) {
        c.add(e);
        ++n;
      }
      return n;
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
    final Monitor monitor = this.monitor;
    monitor.enter();
    try {
      q.clear();
    } finally {
      monitor.leave();
    }
  }

  /**
   * Returns an iterator over the elements in this queue. The iterator does not return the elements
   * in any particular order. The returned {@code Iterator} is a "weakly consistent" iterator that
   * will never throw {@link ConcurrentModificationException}, and guarantees to traverse elements
   * as they existed upon construction of the iterator, and may (but is not guaranteed to) reflect
   * any modifications subsequent to construction.
   *
   * @return an iterator over the elements in this queue
   */
  @Override
  public Iterator<E> iterator() {
    return new Itr(toArray());
  }

  /** Snapshot iterator that works off copy of underlying q array. */
  private class Itr implements Iterator<E> {
    final Object[] array; // Array of all elements
    int cursor; // index of next element to return;
    int lastRet; // index of last element, or -1 if no such

    Itr(Object[] array) {
      lastRet = -1;
      this.array = array;
    }

    @Override
    public boolean hasNext() {
      return cursor < array.length;
    }

    @Override
    public E next() {
      if (cursor >= array.length) throw new NoSuchElementException();
      lastRet = cursor;

      // array comes from q.toArray() and so should have only E's in it
      @SuppressWarnings("unchecked")
      E e = (E) array[cursor++];
      return e;
    }

    @Override
    public void remove() {
      if (lastRet < 0) throw new IllegalStateException();
      Object x = array[lastRet];
      lastRet = -1;
      // Traverse underlying queue to find == element,
      // not just a .equals element.
      monitor.enter();
      try {
        for (Iterator<E> it = q.iterator(); it.hasNext(); ) {
          if (it.next() == x) {
            it.remove();
            return;
          }
        }
      } finally {
        monitor.leave();
      }
    }
  }

  /**
   * Saves the state to a stream (that is, serializes it). This merely wraps default serialization
   * within the monitor. The serialization strategy for items is left to underlying Queue. Note that
   * locking is not needed on deserialization, so readObject is not defined, just relying on
   * default.
   */
  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    monitor.enter();
    try {
      s.defaultWriteObject();
    } finally {
      monitor.leave();
    }
  }
}
