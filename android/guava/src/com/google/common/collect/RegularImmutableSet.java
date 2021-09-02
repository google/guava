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
import com.google.common.annotations.VisibleForTesting;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of {@link ImmutableSet} with two or more elements.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
@ElementTypesAreNonnullByDefault
final class RegularImmutableSet<E> extends ImmutableSet<E> {
  private static final Object[] EMPTY_ARRAY = new Object[0];
  static final RegularImmutableSet<Object> EMPTY =
      new RegularImmutableSet<>(EMPTY_ARRAY, 0, EMPTY_ARRAY, 0, 0);

  // The first `size` elements are non-null.
  @VisibleForTesting final transient @Nullable Object[] elements;
  private final transient int hashCode;
  // the same values as `elements` in hashed positions (plus nulls)
  @VisibleForTesting final transient @Nullable Object[] table;
  // 'and' with an int to get a valid table index.
  private final transient int mask;
  private final transient int size;

  RegularImmutableSet(
      @Nullable Object[] elements, int hashCode, @Nullable Object[] table, int mask, int size) {
    this.elements = elements;
    this.hashCode = hashCode;
    this.table = table;
    this.mask = mask;
    this.size = size;
  }

  @Override
  public boolean contains(@CheckForNull Object target) {
    @Nullable Object[] table = this.table;
    if (target == null || table.length == 0) {
      return false;
    }
    for (int i = Hashing.smearedHash(target); ; i++) {
      i &= mask;
      Object candidate = table[i];
      if (candidate == null) {
        return false;
      } else if (candidate.equals(target)) {
        return true;
      }
    }
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public UnmodifiableIterator<E> iterator() {
    return asList().iterator();
  }

  @Override
  @Nullable
  Object[] internalArray() {
    return elements;
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
  int copyIntoArray(@Nullable Object[] dst, int offset) {
    System.arraycopy(elements, 0, dst, offset, size);
    return offset + size;
  }

  @Override
  ImmutableList<E> createAsList() {
    return ImmutableList.asImmutableList(elements, size);
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  boolean isHashCodeFast() {
    return true;
  }
}
