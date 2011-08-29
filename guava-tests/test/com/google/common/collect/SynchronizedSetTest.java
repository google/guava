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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.testing.SerializableTester;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Tests for {@code Synchronized#set}.
 *
 * @author Mike Bostock
 */
public class SynchronizedSetTest extends AbstractCollectionTest {
  public final Object mutex = new Integer(1); // something Serializable

  @Override protected <E> Set<E> create() {
    TestSet<E> inner = new TestSet<E>(new HashSet<E>(), mutex);
    Set<E> outer = Synchronized.set(inner, inner.mutex);
    return outer;
  }

  @Override public void testNullPointerExceptions() throws Exception {
    /* Skip this test, as SynchronizedSet is not a public class. */
  }

  public void testSerialization() {
    SerializableTester.reserializeAndAssert(create());
  }

  static class TestSet<E> extends ForwardingSet<E> implements Serializable {
    final Set<E> delegate;
    public final Object mutex;

    public TestSet(Set<E> delegate, Object mutex) {
      checkNotNull(mutex);
      this.delegate = delegate;
      this.mutex = mutex;
    }

    @Override protected Set<E> delegate() {
      return delegate;
    }

    @Override public String toString() {
      assertTrue(Thread.holdsLock(mutex));
      return super.toString();
    }

    @Override public boolean equals(@Nullable Object o) {
      assertTrue(Thread.holdsLock(mutex));
      return super.equals(o);
    }

    @Override public int hashCode() {
      assertTrue(Thread.holdsLock(mutex));
      return super.hashCode();
    }

    @Override public boolean add(@Nullable E o) {
      assertTrue(Thread.holdsLock(mutex));
      return super.add(o);
    }

    @Override public boolean addAll(Collection<? extends E> c) {
      assertTrue(Thread.holdsLock(mutex));
      return super.addAll(c);
    }

    @Override public void clear() {
      assertTrue(Thread.holdsLock(mutex));
      super.clear();
    }

    @Override public boolean contains(@Nullable Object o) {
      assertTrue(Thread.holdsLock(mutex));
      return super.contains(o);
    }

    @Override public boolean containsAll(Collection<?> c) {
      assertTrue(Thread.holdsLock(mutex));
      return super.containsAll(c);
    }

    @Override public boolean isEmpty() {
      assertTrue(Thread.holdsLock(mutex));
      return super.isEmpty();
    }

    /* Don't test iterator(); it may or may not hold the mutex. */

    @Override public boolean remove(@Nullable Object o) {
      assertTrue(Thread.holdsLock(mutex));
      return super.remove(o);
    }

    @Override public boolean removeAll(Collection<?> c) {
      assertTrue(Thread.holdsLock(mutex));
      return super.removeAll(c);
    }

    @Override public boolean retainAll(Collection<?> c) {
      assertTrue(Thread.holdsLock(mutex));
      return super.retainAll(c);
    }

    @Override public int size() {
      assertTrue(Thread.holdsLock(mutex));
      return super.size();
    }

    @Override public Object[] toArray() {
      assertTrue(Thread.holdsLock(mutex));
      return super.toArray();
    }

    @Override public <T> T[] toArray(T[] a) {
      assertTrue(Thread.holdsLock(mutex));
      return super.toArray(a);
    }

    private static final long serialVersionUID = 0;
  }
}
