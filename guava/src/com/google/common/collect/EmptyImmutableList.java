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

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.base.Preconditions.checkPositionIndexes;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

/**
 * An empty immutable list.
 * 
 * @author Kevin Bourrillion
 */
@GwtCompatible(serializable = true, emulated = true)
final class EmptyImmutableList extends ImmutableList<Object> {
  static final EmptyImmutableList INSTANCE = new EmptyImmutableList();
  static final UnmodifiableListIterator<Object> ITERATOR =
      new UnmodifiableListIterator<Object>() {

        @Override public boolean hasNext() {
          return false;
        }

        @Override public boolean hasPrevious() {
          return false;
        }

        @Override public Object next() {
          throw new NoSuchElementException();
        }

        @Override public int nextIndex() {
          return 0;
        }

        @Override public Object previous() {
          throw new NoSuchElementException();
        }

        @Override public int previousIndex() {
          return -1;
        }
      };

  private EmptyImmutableList() {}

  @Override
  public int size() {
    return 0;
  }

  @Override public boolean isEmpty() {
    return true;
  }

  @Override boolean isPartialView() {
    return false;
  }

  @Override public boolean contains(Object target) {
    return false;
  }

  @Override public UnmodifiableIterator<Object> iterator() {
    return Iterators.emptyIterator();
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

  @Override
  public Object get(int index) {
    // guaranteed to fail, but at least we get a consistent message
    checkElementIndex(index, 0);
    throw new AssertionError("unreachable");
  }

  @Override public int indexOf(@Nullable Object target) {
    return -1;
  }

  @Override public int lastIndexOf(@Nullable Object target) {
    return -1;
  }

  @Override public ImmutableList<Object> subList(int fromIndex, int toIndex) {
    checkPositionIndexes(fromIndex, toIndex, 0);
    return this;
  }

  @Override public ImmutableList<Object> reverse() {
    return this;
  }

  @Override public UnmodifiableListIterator<Object> listIterator(){
    return ITERATOR;
  }

  @Override public UnmodifiableListIterator<Object> listIterator(int start) {
    checkPositionIndex(start, 0);
    return ITERATOR;
  }

  @Override public boolean containsAll(Collection<?> targets) {
    return targets.isEmpty();
  }

  @Override public boolean equals(@Nullable Object object) {
    if (object instanceof List) {
      List<?> that = (List<?>) object;
      return that.isEmpty();
    }
    return false;
  }

  @Override public int hashCode() {
    return 1;
  }

  @Override public String toString() {
    return "[]";
  }

  Object readResolve() {
    return INSTANCE; // preserve singleton property
  }

  private static final long serialVersionUID = 0;
}
