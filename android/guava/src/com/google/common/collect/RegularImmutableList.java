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

import static com.google.common.base.Preconditions.checkElementIndex;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of {@link ImmutableList} backed by a simple array.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
@ElementTypesAreNonnullByDefault
class RegularImmutableList<E> extends ImmutableList<E> {
  static final ImmutableList<Object> EMPTY = new RegularImmutableList<>(new Object[0], 0);

  // The first `size` elements are non-null.
  @VisibleForTesting final transient @Nullable Object[] array;
  private final transient int size;

  RegularImmutableList(@Nullable Object[] array, int size) {
    this.array = array;
    this.size = size;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  @Nullable
  Object[] internalArray() {
    return array;
  }

  @Override
  int internalArrayStart() {
    return 0;
  }

  @Override
  int internalArrayEnd() {
    return size;
  }

  @Override
  int copyIntoArray(@Nullable Object[] dst, int dstOff) {
    System.arraycopy(array, 0, dst, dstOff, size);
    return dstOff + size;
  }

  // The fake cast to E is safe because the creation methods only allow E's
  @Override
  @SuppressWarnings("unchecked")
  public E get(int index) {
    checkElementIndex(index, size);
    // requireNonNull is safe because we guarantee that the first `size` elements are non-null.
    return (E) requireNonNull(array[index]);
  }

  // TODO(lowasser): benchmark optimizations for equals() and see if they're worthwhile
}
