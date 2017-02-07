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
import java.util.Spliterator;
import java.util.Spliterators;
import javax.annotation.Nullable;

/**
 * Implementation of {@link ImmutableSet} with two or more elements.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
final class RegularImmutableSet<E> extends ImmutableSet<E> {
  static final RegularImmutableSet<Object> EMPTY =
      new RegularImmutableSet<Object>(ObjectArrays.EMPTY_ARRAY, 0, null, 0);

  private final transient Object[] elements;
  // the same elements in hashed positions (plus nulls); null if size <= 1
  @VisibleForTesting final transient Object[] table;
  // 'and' with an int to get a valid table index.
  private final transient int mask;

  /*
   * The hash code of the entire set, equal to the sum of the hash codes of the elements, unless
   * this is a singleton set which did not precompute the hash code, in which case this is 0.
   */
  private final transient int hashCode;

  RegularImmutableSet(Object[] elements, int hashCode, Object[] table, int mask) {
    this.elements = elements;
    this.table = table;
    this.mask = mask;
    this.hashCode = hashCode;
  }

  /**
   * Creates a singleton RegularImmutableSet. If hashCode is 0, then the hash code of the Set as a
   * whole will be computed on the fly; we won't precompute it just in case hashCode() is called.
   */
  RegularImmutableSet(Object element, int hashCode) {
    this(new Object[] {element}, hashCode, null, 0);
  }

  @Override
  public boolean contains(@Nullable Object target) {
    Object[] table = this.table;
    if (target == null) {
      return false;
    } else if (elements.length == 1) {
      return elements[0].equals(target);
    } else if (table == null) {
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
    return elements.length;
  }

  @Override
  public UnmodifiableIterator<E> iterator() {
    return (UnmodifiableIterator<E>) Iterators.forArray(elements);
  }

  @Override
  public Spliterator<E> spliterator() {
    return Spliterators.spliterator(elements, SPLITERATOR_CHARACTERISTICS);
  }

  @Override
  int copyIntoArray(Object[] dst, int offset) {
    System.arraycopy(elements, 0, dst, offset, elements.length);
    return offset + elements.length;
  }

  @Override
  ImmutableList<E> createAsList() {
    switch (elements.length) {
      case 0:
        return ImmutableList.of();
      case 1:
        return ImmutableList.of((E) elements[0]);
      default:
        return new RegularImmutableAsList<E>(this, elements);
    }
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  public int hashCode() {
    if (hashCode == 0 && elements.length == 1) {
      return elements[0].hashCode();
    }
    return hashCode;
  }

  @Override
  boolean isHashCodeFast() {
    return true;
  }
}
