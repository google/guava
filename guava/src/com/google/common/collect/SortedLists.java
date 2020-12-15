/*
 * Copyright (C) 2010 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Static methods pertaining to sorted {@link List} instances.
 *
 * <p>In this documentation, the terms <i>greatest</i>, <i>greater</i>, <i>least</i>, and
 * <i>lesser</i> are considered to refer to the comparator on the elements, and the terms
 * <i>first</i> and <i>last</i> are considered to refer to the elements' ordering in a list.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Beta
final class SortedLists {
  private SortedLists() {}

  /**
   * A specification for which index to return if the list contains at least one element that
   * compares as equal to the key.
   */
  enum KeyPresentBehavior {
    /**
     * Return the index of any list element that compares as equal to the key. No guarantees are
     * made as to which index is returned, if more than one element compares as equal to the key.
     */
    ANY_PRESENT {
      @Override
      <E> int resultIndex(
          Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
        return foundIndex;
      }
    },
    /** Return the index of the last list element that compares as equal to the key. */
    LAST_PRESENT {
      @Override
      <E> int resultIndex(
          Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
        // Of course, we have to use binary search to find the precise
        // breakpoint...
        int lower = foundIndex;
        int upper = list.size() - 1;
        // Everything between lower and upper inclusive compares at >= 0.
        while (lower < upper) {
          int middle = (lower + upper + 1) >>> 1;
          int c = comparator.compare(list.get(middle), key);
          if (c > 0) {
            upper = middle - 1;
          } else { // c == 0
            lower = middle;
          }
        }
        return lower;
      }
    },
    /** Return the index of the first list element that compares as equal to the key. */
    FIRST_PRESENT {
      @Override
      <E> int resultIndex(
          Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
        // Of course, we have to use binary search to find the precise
        // breakpoint...
        int lower = 0;
        int upper = foundIndex;
        // Of course, we have to use binary search to find the precise breakpoint...
        // Everything between lower and upper inclusive compares at <= 0.
        while (lower < upper) {
          int middle = (lower + upper) >>> 1;
          int c = comparator.compare(list.get(middle), key);
          if (c < 0) {
            lower = middle + 1;
          } else { // c == 0
            upper = middle;
          }
        }
        return lower;
      }
    },
    /**
     * Return the index of the first list element that compares as greater than the key, or {@code
     * list.size()} if there is no such element.
     */
    FIRST_AFTER {
      @Override
      public <E> int resultIndex(
          Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
        return LAST_PRESENT.resultIndex(comparator, key, list, foundIndex) + 1;
      }
    },
    /**
     * Return the index of the last list element that compares as less than the key, or {@code -1}
     * if there is no such element.
     */
    LAST_BEFORE {
      @Override
      public <E> int resultIndex(
          Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
        return FIRST_PRESENT.resultIndex(comparator, key, list, foundIndex) - 1;
      }
    };

    abstract <E> int resultIndex(
        Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex);
  }

  /**
   * A specification for which index to return if the list contains no elements that compare as
   * equal to the key.
   */
  enum KeyAbsentBehavior {
    /**
     * Return the index of the next lower element in the list, or {@code -1} if there is no such
     * element.
     */
    NEXT_LOWER {
      @Override
      int resultIndex(int higherIndex) {
        return higherIndex - 1;
      }
    },
    /**
     * Return the index of the next higher element in the list, or {@code list.size()} if there is
     * no such element.
     */
    NEXT_HIGHER {
      @Override
      public int resultIndex(int higherIndex) {
        return higherIndex;
      }
    },
    /**
     * Return {@code ~insertionIndex}, where {@code insertionIndex} is defined as the point at which
     * the key would be inserted into the list: the index of the next higher element in the list, or
     * {@code list.size()} if there is no such element.
     *
     * <p>Note that the return value will be {@code >= 0} if and only if there is an element of the
     * list that compares as equal to the key.
     *
     * <p>This is equivalent to the behavior of {@link java.util.Collections#binarySearch(List,
     * Object)} when the key isn't present, since {@code ~insertionIndex} is equal to {@code -1 -
     * insertionIndex}.
     */
    INVERTED_INSERTION_INDEX {
      @Override
      public int resultIndex(int higherIndex) {
        return ~higherIndex;
      }
    };

    abstract int resultIndex(int higherIndex);
  }

  /**
   * Searches the specified naturally ordered list for the specified object using the binary search
   * algorithm.
   *
   * <p>Equivalent to {@link #binarySearch(List, Function, Object, Comparator, KeyPresentBehavior,
   * KeyAbsentBehavior)} using {@link Ordering#natural}.
   */
  public static <E extends Comparable> int binarySearch(
      List<? extends E> list,
      E e,
      KeyPresentBehavior presentBehavior,
      KeyAbsentBehavior absentBehavior) {
    checkNotNull(e);
    return binarySearch(list, e, Ordering.natural(), presentBehavior, absentBehavior);
  }

  /**
   * Binary searches the list for the specified key, using the specified key function.
   *
   * <p>Equivalent to {@link #binarySearch(List, Function, Object, Comparator, KeyPresentBehavior,
   * KeyAbsentBehavior)} using {@link Ordering#natural}.
   */
  public static <E, K extends Comparable> int binarySearch(
      List<E> list,
      Function<? super E, K> keyFunction,
      @Nullable K key,
      KeyPresentBehavior presentBehavior,
      KeyAbsentBehavior absentBehavior) {
    return binarySearch(
        list, keyFunction, key, Ordering.natural(), presentBehavior, absentBehavior);
  }

  /**
   * Binary searches the list for the specified key, using the specified key function.
   *
   * <p>Equivalent to {@link #binarySearch(List, Object, Comparator, KeyPresentBehavior,
   * KeyAbsentBehavior)} using {@link Lists#transform(List, Function) Lists.transform(list,
   * keyFunction)}.
   */
  public static <E, K> int binarySearch(
      List<E> list,
      Function<? super E, K> keyFunction,
      @Nullable K key,
      Comparator<? super K> keyComparator,
      KeyPresentBehavior presentBehavior,
      KeyAbsentBehavior absentBehavior) {
    return binarySearch(
        Lists.transform(list, keyFunction), key, keyComparator, presentBehavior, absentBehavior);
  }

  /**
   * Searches the specified list for the specified object using the binary search algorithm. The
   * list must be sorted into ascending order according to the specified comparator (as by the
   * {@link Collections#sort(List, Comparator) Collections.sort(List, Comparator)} method), prior to
   * making this call. If it is not sorted, the results are undefined.
   *
   * <p>If there are elements in the list which compare as equal to the key, the choice of {@link
   * KeyPresentBehavior} decides which index is returned. If no elements compare as equal to the
   * key, the choice of {@link KeyAbsentBehavior} decides which index is returned.
   *
   * <p>This method runs in log(n) time on random-access lists, which offer near-constant-time
   * access to each list element.
   *
   * @param list the list to be searched.
   * @param key the value to be searched for.
   * @param comparator the comparator by which the list is ordered.
   * @param presentBehavior the specification for what to do if at least one element of the list
   *     compares as equal to the key.
   * @param absentBehavior the specification for what to do if no elements of the list compare as
   *     equal to the key.
   * @return the index determined by the {@code KeyPresentBehavior}, if the key is in the list;
   *     otherwise the index determined by the {@code KeyAbsentBehavior}.
   */
  public static <E> int binarySearch(
      List<? extends E> list,
      @Nullable E key,
      Comparator<? super E> comparator,
      KeyPresentBehavior presentBehavior,
      KeyAbsentBehavior absentBehavior) {
    checkNotNull(comparator);
    checkNotNull(list);
    checkNotNull(presentBehavior);
    checkNotNull(absentBehavior);
    if (!(list instanceof RandomAccess)) {
      list = Lists.newArrayList(list);
    }
    // TODO(lowasser): benchmark when it's best to do a linear search

    int lower = 0;
    int upper = list.size() - 1;

    while (lower <= upper) {
      int middle = (lower + upper) >>> 1;
      int c = comparator.compare(key, list.get(middle));
      if (c < 0) {
        upper = middle - 1;
      } else if (c > 0) {
        lower = middle + 1;
      } else {
        return lower
            + presentBehavior.resultIndex(
                comparator, key, list.subList(lower, upper + 1), middle - lower);
      }
    }
    return absentBehavior.resultIndex(lower);
  }
}
