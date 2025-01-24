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
import static junit.framework.Assert.assertTrue;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link Set} implementation that asserts that a given lock is held whenever one of its methods is
 * called.
 */
@NullUnmarked
class LockHeldAssertingSet<E> extends ForwardingSet<E> implements Serializable {
  final Set<E> delegate;
  final Object mutex;

  LockHeldAssertingSet(Set<E> delegate, Object mutex) {
    checkNotNull(mutex);
    this.delegate = delegate;
    this.mutex = mutex;
  }

  @Override
  protected Set<E> delegate() {
    return delegate;
  }

  @Override
  public String toString() {
    assertTrue(Thread.holdsLock(mutex));
    return super.toString();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    assertTrue(Thread.holdsLock(mutex));
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    assertTrue(Thread.holdsLock(mutex));
    return super.hashCode();
  }

  @Override
  public boolean add(@Nullable E o) {
    assertTrue(Thread.holdsLock(mutex));
    return super.add(o);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    assertTrue(Thread.holdsLock(mutex));
    return super.addAll(c);
  }

  @Override
  public void clear() {
    assertTrue(Thread.holdsLock(mutex));
    super.clear();
  }

  @Override
  public boolean contains(@Nullable Object o) {
    assertTrue(Thread.holdsLock(mutex));
    return super.contains(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    assertTrue(Thread.holdsLock(mutex));
    return super.containsAll(c);
  }

  @Override
  public boolean isEmpty() {
    assertTrue(Thread.holdsLock(mutex));
    return super.isEmpty();
  }

  /*
   * We don't assert that the lock is held during calls to iterator(), stream(), and spliterator:
   * `Synchronized` doesn't guarantee that it will hold the mutex for those calls because callers
   * are responsible for taking the mutex themselves:
   * https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/util/Collections.html#synchronizedCollection(java.util.Collection)
   *
   * Similarly, we avoid having those methods *implemented* in terms of *other* TestSet methods
   * that will perform holdsLock assertions:
   *
   * - For iterator(), we can accomplish that by not overriding iterator() at all. That way, we
   *   inherit an implementation that forwards to the delegate collection, which performs no
   *   holdsLock assertions.
   *
   * - For stream() and spliterator(), we have to forward to the delegate ourselves because
   *   ForwardingSet does not forward `default` methods, as discussed in its Javadoc.
   */

  @Override
  public Stream<E> stream() {
    return delegate.stream();
  }

  @Override
  public Spliterator<E> spliterator() {
    return delegate.spliterator();
  }

  @Override
  public boolean remove(@Nullable Object o) {
    assertTrue(Thread.holdsLock(mutex));
    return super.remove(o);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    assertTrue(Thread.holdsLock(mutex));
    return super.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    assertTrue(Thread.holdsLock(mutex));
    return super.retainAll(c);
  }

  @Override
  public int size() {
    assertTrue(Thread.holdsLock(mutex));
    return super.size();
  }

  @Override
  public Object[] toArray() {
    assertTrue(Thread.holdsLock(mutex));
    return super.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    assertTrue(Thread.holdsLock(mutex));
    return super.toArray(a);
  }

  private static final long serialVersionUID = 0;
}
