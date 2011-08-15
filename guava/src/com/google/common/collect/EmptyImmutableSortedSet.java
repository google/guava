/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * An empty immutable sorted set.
 *
 * @author Jared Levy
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
class EmptyImmutableSortedSet<E> extends ImmutableSortedSet<E> {
  EmptyImmutableSortedSet(Comparator<? super E> comparator) {
    super(comparator);
  }

  @Override
  public int size() {
    return 0;
  }

  @Override public boolean isEmpty() {
    return true;
  }

  @Override public boolean contains(Object target) {
    return false;
  }

  @Override public UnmodifiableIterator<E> iterator() {
    return Iterators.emptyIterator();
  }

  @Override boolean isPartialView() {
    return false;
  }

  private static final Object[] EMPTY_ARRAY = new Object[0];

  @Override public Object[] toArray() {
    return EMPTY_ARRAY;
  }

  @Override public <T> T[] toArray(T[] a) {
    if (a.length > 0) {
      a[0] = null;
    }
    return a;
  }

  @Override public boolean containsAll(Collection<?> targets) {
    return targets.isEmpty();
  }

  @Override public boolean equals(@Nullable Object object) {
    if (object instanceof Set) {
      Set<?> that = (Set<?>) object;
      return that.isEmpty();
    }
    return false;
  }

  @Override public int hashCode() {
    return 0;
  }

  @Override public String toString() {
    return "[]";
  }

  @Override
  public E first() {
    throw new NoSuchElementException();
  }

  @Override
  public E last() {
    throw new NoSuchElementException();
  }

  @Override
  ImmutableSortedSet<E> headSetImpl(E toElement, boolean inclusive) {
    return this;
  }

  @Override
  ImmutableSortedSet<E> subSetImpl(
      E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
    return this;
  }

  @Override
  ImmutableSortedSet<E> tailSetImpl(E fromElement, boolean inclusive) {
    return this;
  }

  @Override int indexOf(@Nullable Object target) {
    return -1;
  }
}
