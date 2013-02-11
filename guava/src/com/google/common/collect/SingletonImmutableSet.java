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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;

import java.util.Set;

import javax.annotation.Nullable;

/**
 * Implementation of {@link ImmutableSet} with exactly one element.
 *
 * @author Kevin Bourrillion
 * @author Nick Kralevich
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
final class SingletonImmutableSet<E> extends ImmutableSet<E> {

  final transient E element;
  // This is transient because it will be recalculated on the first
  // call to hashCode().
  //
  // A race condition is avoided since threads will either see that the value
  // is zero and recalculate it themselves, or two threads will see it at
  // the same time, and both recalculate it.  If the cachedHashCode is 0,
  // it will always be recalculated, unfortunately.
  private transient int cachedHashCode;

  SingletonImmutableSet(E element) {
    this.element = Preconditions.checkNotNull(element);
  }

  SingletonImmutableSet(E element, int hashCode) {
    // Guaranteed to be non-null by the presence of the pre-computed hash code.
    this.element = element;
    cachedHashCode = hashCode;
  }

  @Override
  public int size() {
    return 1;
  }

  @Override public boolean isEmpty() {
    return false;
  }

  @Override public boolean contains(Object target) {
    return element.equals(target);
  }

  @Override public UnmodifiableIterator<E> iterator() {
    return Iterators.singletonIterator(element);
  }

  @Override boolean isPartialView() {
    return false;
  }

  @Override
  int copyIntoArray(Object[] dst, int offset) {
    dst[offset] = element;
    return offset + 1;
  }

  @Override public boolean equals(@Nullable Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof Set) {
      Set<?> that = (Set<?>) object;
      return that.size() == 1 && element.equals(that.iterator().next());
    }
    return false;
  }

  @Override public final int hashCode() {
    // Racy single-check.
    int code = cachedHashCode;
    if (code == 0) {
      cachedHashCode = code = element.hashCode();
    }
    return code;
  }

  @Override boolean isHashCodeFast() {
    return cachedHashCode != 0;
  }

  @Override public String toString() {
    String elementToString = element.toString();
    return new StringBuilder(elementToString.length() + 2)
        .append('[')
        .append(elementToString)
        .append(']')
        .toString();
  }
}
