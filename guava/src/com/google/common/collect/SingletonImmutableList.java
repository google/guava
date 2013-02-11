/*
 * Copyright (C) 2009 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Implementation of {@link ImmutableList} with exactly one element.
 *
 * @author Hayward Chan
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
final class SingletonImmutableList<E> extends ImmutableList<E> {

  final transient E element;

  SingletonImmutableList(E element) {
    this.element = checkNotNull(element);
  }

  @Override
  public E get(int index) {
    Preconditions.checkElementIndex(index, 1);
    return element;
  }

  @Override public int indexOf(@Nullable Object object) {
    return element.equals(object) ? 0 : -1;
  }

  @Override public UnmodifiableIterator<E> iterator() {
    return Iterators.singletonIterator(element);
  }

  @Override public int lastIndexOf(@Nullable Object object) {
    return indexOf(object);
  }

  @Override
  public int size() {
    return 1;
  }

  @Override public ImmutableList<E> subList(int fromIndex, int toIndex) {
    Preconditions.checkPositionIndexes(fromIndex, toIndex, 1);
    return (fromIndex == toIndex) ? ImmutableList.<E>of() : this;
  }

  @Override public ImmutableList<E> reverse() {
    return this;
  }

  @Override public boolean contains(@Nullable Object object) {
    return element.equals(object);
  }

  @Override public boolean equals(@Nullable Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof List) {
      List<?> that = (List<?>) object;
      return that.size() == 1 && element.equals(that.get(0));
    }
    return false;
  }

  @Override public int hashCode() {
    // not caching hash code since it could change if the element is mutable
    // in a way that modifies its hash code.
    return 31 + element.hashCode();
  }

  @Override public String toString() {
    String elementToString = element.toString();
    return new StringBuilder(elementToString.length() + 2)
        .append('[')
        .append(elementToString)
        .append(']')
        .toString();
  }

  @Override public boolean isEmpty() {
    return false;
  }

  @Override boolean isPartialView() {
    return false;
  }

  @Override
  int copyIntoArray(Object[] dst, int offset) {
    dst[offset] = element;
    return offset + 1;
  }
}
