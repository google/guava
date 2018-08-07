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
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Methods factored out so that they can be emulated differently in GWT.
 *
 * @author Hayward Chan
 */
@GwtCompatible(emulated = true)
final class Platform {
  /** Returns the platform preferred implementation of a map based on a hash table. */
  static <K, V> Map<K, V> newHashMapWithExpectedSize(int expectedSize) {
    return Maps.newHashMapWithExpectedSize(expectedSize);
  }

  /**
   * Returns the platform preferred implementation of an insertion ordered map based on a hash
   * table.
   */
  static <K, V> Map<K, V> newLinkedHashMapWithExpectedSize(int expectedSize) {
    return Maps.newLinkedHashMapWithExpectedSize(expectedSize);
  }

  /** Returns the platform preferred implementation of a set based on a hash table. */
  static <E> Set<E> newHashSetWithExpectedSize(int expectedSize) {
    return Sets.newHashSetWithExpectedSize(expectedSize);
  }

  /**
   * Returns the platform preferred implementation of an insertion ordered set based on a hash
   * table.
   */
  static <E> Set<E> newLinkedHashSetWithExpectedSize(int expectedSize) {
    return Sets.newLinkedHashSetWithExpectedSize(expectedSize);
  }

  /**
   * Returns the platform preferred map implementation that preserves insertion order when used only
   * for insertions.
   */
  static <K, V> Map<K, V> preservesInsertionOrderOnPutsMap() {
    return Maps.newLinkedHashMap();
  }

  /**
   * Returns the platform preferred set implementation that preserves insertion order when used only
   * for insertions.
   */
  static <E> Set<E> preservesInsertionOrderOnAddsSet() {
    return Sets.newLinkedHashSet();
  }

  /**
   * Returns a new array of the given length with the same type as a reference array.
   *
   * @param reference any array of the desired type
   * @param length the length of the new array
   */
  static <T> T[] newArray(T[] reference, int length) {
    Class<?> type = reference.getClass().getComponentType();

    // the cast is safe because
    // result.getClass() == reference.getClass().getComponentType()
    @SuppressWarnings("unchecked")
    T[] result = (T[]) Array.newInstance(type, length);
    return result;
  }

  /** Equivalent to Arrays.copyOfRange(source, from, to, arrayOfType.getClass()). */
  static <T> T[] copy(Object[] source, int from, int to, T[] arrayOfType) {
    return Arrays.copyOfRange(source, from, to, (Class<? extends T[]>) arrayOfType.getClass());
  }

  /**
   * Configures the given map maker to use weak keys, if possible; does nothing otherwise (i.e., in
   * GWT). This is sometimes acceptable, when only server-side code could generate enough volume
   * that reclamation becomes important.
   */
  static MapMaker tryWeakKeys(MapMaker mapMaker) {
    return mapMaker.weakKeys();
  }

  static int reduceIterationsIfGwt(int iterations) {
    return iterations;
  }

  static int reduceExponentIfGwt(int exponent) {
    return exponent;
  }

  private Platform() {}
}
