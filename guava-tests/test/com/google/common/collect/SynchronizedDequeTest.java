/*
 * Copyright (C) 2013 The Guava Authors
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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import junit.framework.TestCase;

/**
 * Tests for {@link Synchronized#deque} and {@link Queues#synchronizedDeque}.
 *
 * @author Kurt Alfred Kluever
 */
public class SynchronizedDequeTest extends TestCase {

  protected Deque<String> create() {
    TestDeque<String> inner = new TestDeque<>();
    Deque<String> outer = Synchronized.deque(inner, inner.mutex);
    outer.add("foo"); // necessary because we try to remove elements later on
    return outer;
  }

  private static final class TestDeque<E> implements Deque<E> {
    private final Deque<E> delegate = Lists.newLinkedList();
    public final Object mutex = new Integer(1); // something Serializable

    @Override
    public boolean offer(E o) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.offer(o);
    }

    @Override
    public E poll() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.poll();
    }

    @Override
    public E remove() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.remove();
    }

    @Override
    public boolean remove(Object object) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.remove(object);
    }

    @Override
    public E peek() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.peek();
    }

    @Override
    public E element() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.element();
    }

    @Override
    public Iterator<E> iterator() {
      // We explicitly don't lock for iterator()
      assertFalse(Thread.holdsLock(mutex));
      return delegate.iterator();
    }

    @Override
    public int size() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.size();
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.removeAll(collection);
    }

    @Override
    public boolean isEmpty() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object object) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.contains(object);
    }

    @Override
    public boolean add(E element) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.add(element);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.addAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.retainAll(collection);
    }

    @Override
    public void clear() {
      assertTrue(Thread.holdsLock(mutex));
      delegate.clear();
    }

    @Override
    public Object[] toArray() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.toArray(array);
    }

    @Override
    public void addFirst(E e) {
      assertTrue(Thread.holdsLock(mutex));
      delegate.addFirst(e);
    }

    @Override
    public void addLast(E e) {
      assertTrue(Thread.holdsLock(mutex));
      delegate.addLast(e);
    }

    @Override
    public boolean offerFirst(E e) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.offerFirst(e);
    }

    @Override
    public boolean offerLast(E e) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.offerLast(e);
    }

    @Override
    public E removeFirst() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.removeFirst();
    }

    @Override
    public E removeLast() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.removeLast();
    }

    @Override
    public E pollFirst() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.pollFirst();
    }

    @Override
    public E pollLast() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.pollLast();
    }

    @Override
    public E getFirst() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.getFirst();
    }

    @Override
    public E getLast() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.getLast();
    }

    @Override
    public E peekFirst() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.peekFirst();
    }

    @Override
    public E peekLast() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.peekLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.removeFirstOccurrence(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.removeLastOccurrence(o);
    }

    @Override
    public void push(E e) {
      assertTrue(Thread.holdsLock(mutex));
      delegate.push(e);
    }

    @Override
    public E pop() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.pop();
    }

    @Override
    public Iterator<E> descendingIterator() {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.descendingIterator();
    }

    private static final long serialVersionUID = 0;
  }

  public void testHoldsLockOnAllOperations() {
    create().element();
    create().offer("foo");
    create().peek();
    create().poll();
    create().remove();
    create().add("foo");
    create().addAll(ImmutableList.of("foo"));
    create().clear();
    create().contains("foo");
    create().containsAll(ImmutableList.of("foo"));
    create().equals(new ArrayDeque<>(ImmutableList.of("foo")));
    create().hashCode();
    create().isEmpty();
    create().iterator();
    create().remove("foo");
    create().removeAll(ImmutableList.of("foo"));
    create().retainAll(ImmutableList.of("foo"));
    create().size();
    create().toArray();
    create().toArray(new String[] {"foo"});
    create().addFirst("e");
    create().addLast("e");
    create().offerFirst("e");
    create().offerLast("e");
    create().removeFirst();
    create().removeLast();
    create().pollFirst();
    create().pollLast();
    create().getFirst();
    create().getLast();
    create().peekFirst();
    create().peekLast();
    create().removeFirstOccurrence("e");
    create().removeLastOccurrence("e");
    create().push("e");
    create().pop();
    create().descendingIterator();
  }
}
