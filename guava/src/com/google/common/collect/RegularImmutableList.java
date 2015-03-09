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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;

/**
 * Implementation of {@link ImmutableList} used for 0 or 2+ elements (not 1).
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
class RegularImmutableList<E> extends ImmutableList<E> {
  static final ImmutableList<Object> EMPTY =
      new RegularImmutableList<Object>(ObjectArrays.EMPTY_ARRAY);

  private final transient int offset;
  private final transient int size;
  private final transient Object[] array;

  RegularImmutableList(Object[] array, int offset, int size) {
    this.offset = offset;
    this.size = size;
    this.array = array;
  }

  RegularImmutableList(Object[] array) {
    this(array, 0, array.length);
  }

  @Override
  public int size() {
    return size;
  }

  @Override boolean isPartialView() {
    return size != array.length;
  }

  @Override
  int copyIntoArray(Object[] dst, int dstOff) {
    System.arraycopy(array, offset, dst, dstOff, size);
    return dstOff + size;
  }

  // The fake cast to E is safe because the creation methods only allow E's
  @Override
  @SuppressWarnings("unchecked")
  public E get(int index) {
    Preconditions.checkElementIndex(index, size);
    return (E) array[index + offset];
  }

  @Override
  public int indexOf(@Nullable Object object) {
    if (object == null) {
      return -1;
    }
    for (int i = 0; i < size; i++) {
      if (array[offset + i].equals(object)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int lastIndexOf(@Nullable Object object) {
    if (object == null) {
      return -1;
    }
    for (int i = size - 1; i >= 0; i--) {
      if (array[offset + i].equals(object)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  ImmutableList<E> subListUnchecked(int fromIndex, int toIndex) {
    return new RegularImmutableList<E>(
        array, offset + fromIndex, toIndex - fromIndex);
  }

  @SuppressWarnings("unchecked")
  @Override
  public UnmodifiableListIterator<E> listIterator(int index) {
    // for performance
    // The fake cast to E is safe because the creation methods only allow E's
    return (UnmodifiableListIterator<E>)
        Iterators.forArray(array, offset, size, index);
  }

  // TODO(user): benchmark optimizations for equals() and see if they're worthwhile
}
