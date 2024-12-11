/*
 * Copyright (C) 2007 The Guava Authors
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
import java.util.Iterator;
import java.util.Queue;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@link Synchronized#queue} and {@link Queues#synchronizedQueue}.
 *
 * @author Kurt Alfred Kluever
 */
public class SynchronizedQueueTest extends TestCase {

  protected Queue<String> create() {
    TestQueue<String> inner = new TestQueue<>();
    Queue<String> outer = Synchronized.queue(inner, inner.mutex);
    outer.add("foo"); // necessary because we try to remove elements later on
    return outer;
  }

  private static final class TestQueue<E> implements Queue<E> {
    private final Queue<E> delegate = Lists.newLinkedList();
    public final Object mutex = new Object[0]; // something Serializable

    @Override
    public boolean offer(E o) {
      assertTrue(Thread.holdsLock(mutex));
      return delegate.offer(o);
    }

    @Override
    public @Nullable E poll() {
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
    public @Nullable E peek() {
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

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings("CheckReturnValue")
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
  }
}
