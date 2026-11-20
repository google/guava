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

import static com.google.common.collect.Hashing.smear;
import static java.lang.System.arraycopy;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of {@link ImmutableSet} with two or more elements.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
final class RegularImmutableSet<E> extends ImmutableSet<E> {
  private static final Object[] EMPTY_ARRAY = new Object[0];
  static final RegularImmutableSet<Object> EMPTY =
      new RegularImmutableSet<>(EMPTY_ARRAY, 0, null, 0);

  /** Value stored in {@link #table} for unoccupied slots. */
  static final int ABSENT = 0;

  /** Bit set in {@link #table} entries for occupied slots. */
  static final int OCCUPIED_BIT = Integer.MIN_VALUE;

  private static final int SMALL_INDEX_MASK = 0xFFFF;
  private static final int MEDIUM_INDEX_MASK = 0xFF_FFFF;
  private static final int LARGE_INDEX_MASK = 0x7FFF_FFFF;

  static int indexMaskForTableLength(int tableLength) {
    if (tableLength <= (1 << 16)) {
      return SMALL_INDEX_MASK;
    }
    if (tableLength <= (1 << 24)) {
      return MEDIUM_INDEX_MASK;
    }
    return LARGE_INDEX_MASK;
  }

  // The first `size` elements are non-null.
  @VisibleForTesting final transient @Nullable Object[] elements;
  private final transient int hashCode;
  /*
   * Open-addressed linear-probing hash table of power-of-two length (or null for EMPTY). Empty
   * slots hold ABSENT (0); occupied slots hold OCCUPIED_BIT | (smearedHash & ~indexMask) |
   * elementIndex.
   */
  @VisibleForTesting final transient int @Nullable [] table;
  private final transient int size;

  RegularImmutableSet(@Nullable Object[] elements, int hashCode, int @Nullable [] table, int size) {
    this.elements = elements;
    this.hashCode = hashCode;
    this.table = table;
    this.size = size;
  }

  @Override
  public boolean contains(@Nullable Object target) {
    int[] table = this.table;
    if (target == null || table == null) {
      return false;
    }
    int mask = table.length - 1;
    int indexMask = indexMaskForTableLength(table.length);
    int hash = smear(target.hashCode());
    int tag = OCCUPIED_BIT | (hash & ~indexMask);
    @Nullable Object[] elements = this.elements;
    for (int i = hash; ; i++) {
      int entry = table[i & mask];
      if (entry == ABSENT) {
        return false;
      }
      int index = entry ^ tag;
      if (index <= indexMask) {
        Object candidate = requireNonNull(elements[index]);
        if (candidate == target || candidate.equals(target)) {
          return true;
        }
      }
    }
  }

  @Override
  public int size() {
    return size;
  }

  // We're careful to put only E instances into the array in the mainline.
  // (In the backport, we don't need this suppression, but we keep it to minimize diffs.)
  @SuppressWarnings("unchecked")
  @Override
  public UnmodifiableIterator<E> iterator() {
    return asList().iterator();
  }

  @Override
  @Nullable Object[] internalArray() {
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
    arraycopy(elements, 0, dst, offset, size);
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

  // redeclare to help optimizers with b/310253115
  @SuppressWarnings("RedundantOverride")
  @Override
  @J2ktIncompatible
  @GwtIncompatible
    Object writeReplace() {
    return super.writeReplace();
  }
}
