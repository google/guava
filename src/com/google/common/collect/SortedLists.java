/*
 * Copyright (C) 2010 The Guava Authors
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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Static methods pertaining to sorted {@link List} instances.
 * 
 * In this documentation, the terms <em>greatest</em>, <em>greater</em>,
 * <em>least</em>, and <em>lest</em> are considered to refer to the comparator 
 * on the elements, and the terms <em>first</em> and <em>last</em> are 
 * considered to refer to the elements' ordering in a list. 
 *
 * @author Louis Wasserman
 * @see Lists
 * @see Collections
 */
final class SortedLists {

  private SortedLists() {}

  /**
   * A comparison relationship between a value and an element in a collection.
   */
  enum Relation {

    /**
     * The relation that specifies the greatest element strictly less than the
     * value.  In the case of duplicates, the last such element is 
     * chosen.
     *
     * <p>In the {@link SortedLists#binarySearch binarySearch} methods, {@code
     * -1} is considered to be the index of negative infinity. That is, if there
     * is no element in the list lower than a given value, {@code -1} is
     * returned as the index of the lower element.  For example: <pre> {@code
     * 
     * Integer low = 1;
     * Integer mid = 2;
     * Integer high = 3;
     * List<Integer> list = Arrays.asList(2, 2, 2);
     * binarySearch(list, low, Ordering.natural(), LOWER); // -1
     * binarySearch(list, mid, Ordering.natural(), LOWER); // -1
     * binarySearch(list, high, Ordering.natural(), LOWER); // 2
     * }</pre>
     */
    LOWER {

      @Override Relation reverse() {
        return HIGHER;
      }

      @Override <E> int exactMatchFound(List<? extends E> list, E e,
          int lower, int index, int upper, Comparator<? super E> comparator,
          boolean worryAboutDuplicates) {
        return FLOOR.exactMatchFound(
            list, e, lower, index, upper, comparator, worryAboutDuplicates)
            - 1;
      }

      @Override <E> int exactMatchNotFound(List<? extends E> list, E e,
          int higherIndex, Comparator<? super E> comparator) {
        return higherIndex - 1;
      }
    },

    /**
     * The relation that specifies the greatest element less than or equal to
     * the value. In the case of duplicates, the first element equal to the
     * value is chosen, or if no such element exists, among the greatest
     * elements less than or equal to the value, the last is chosen.
     *
     * <p>
     * In the {@link SortedLists#binarySearch binarySearch} methods, {@code -1}
     * is considered to be the index of negative infinity. That is, if there is
     * no element in the list less than or equal to a given value, {@code -1} is
     * returned as the index of the floor element.  For example: <pre> {@code
     * 
     * Integer low = 1;
     * Integer mid = 2;
     * Integer high = 3;
     * List<Integer> list = Arrays.asList(2, 2, 2);
     * binarySearch(list, low, Ordering.natural(), FLOOR); // -1
     * binarySearch(list, mid, Ordering.natural(), FLOOR); // 0
     * binarySearch(list, high, Ordering.natural(), FLOOR); // 2
     * }</pre>
     */
    FLOOR {

      @Override Relation reverse() {
        return CEILING;
      }

      @Override <E> int exactMatchFound(List<? extends E> list, E e,
          int lower, int index, int upper, Comparator<? super E> comparator,
          boolean worryAboutDuplicates) {
        if (!worryAboutDuplicates) {
          return index;
        }
        // Of course, we have to use binary search to find the precise
        // breakpoint...
        upper = index;
        // Everything between lower and upper inclusive compares at <= 0.
        while (lower < upper) {
          int middle = lower + (upper - lower) / 2;
          int c = comparator.compare(list.get(middle), e);
          if (c < 0) {
            lower = middle + 1;
          } else { // c == 0
            upper = middle;
          }
        }
        return lower;
      }

      @Override <E> int exactMatchNotFound(List<? extends E> list, E e,
          int higherIndex, Comparator<? super E> comparator) {
        return higherIndex - 1;
      }
    },

    /**
     * The relation that specifies an element equal to the value.  In the case
     * of duplicates, no guarantee is made on which element is chosen.
     *
     * <p>
     * In the {@link SortedLists#binarySearch binarySearch} methods, if there is
     * no element that compares as equal to a given value, {@code -1} is
     * returned to indicate failure. For example: <pre> {@code
     * 
     * Integer low = 1;
     * Integer mid = 2;
     * Integer high = 3;
     * List<Integer> list = Arrays.asList(2, 2, 2);
     * binarySearch(list, low, Ordering.natural(), EQUAL); // -1
     * binarySearch(list, mid, Ordering.natural(), EQUAL); // could be 0, 1, 2
     * binarySearch(list, high, Ordering.natural(), EQUAL); // -1
     * }</pre>
     */
    EQUAL {

      @Override Relation reverse() {
        return this;
      }

      @Override <E> int exactMatchFound(List<? extends E> list, E e,
          int lower, int index, int upper, Comparator<? super E> comparator,
          boolean worryAboutDuplicates) {
        return index;
      }

      @Override <E> int exactMatchNotFound(List<? extends E> list, E e,
          int higherIndex, Comparator<? super E> comparator) {
        return -1;
      }
    },

    /**
     * The relation that specifies the least element greater than or equal to
     * the value. In the case of duplicates, the last element equal to the
     * value is chosen, or if no such element exists, among the least
     * elements greater than or equal to the value, the first is chosen.
     
     *
     * <p>
     * In the {@link SortedLists#binarySearch binarySearch} methods, {@code
     * list.size()} is considered to be the index of positive infinity. That is,
     * if there is no element in the list greater than or equal to a given
     * value, {@code -1} is returned as the index of the ceiling element.
     * For example: <pre> {@code
     * 
     * Integer low = 1;
     * Integer mid = 2;
     * Integer high = 3;
     * List<Integer> list = Arrays.asList(2, 2, 2);
     * binarySearch(list, low, Ordering.natural(), CEILING); // 0
     * binarySearch(list, mid, Ordering.natural(), CEILING); // 2
     * binarySearch(list, high, Ordering.natural(), CEILING); // 3
     * }</pre>
     */
    CEILING {

      @Override Relation reverse() {
        return FLOOR;
      }

      @Override <E> int exactMatchFound(List<? extends E> list, E e,
          int lower, int index, int upper, Comparator<? super E> comparator,
          boolean worryAboutDuplicates) {
        if (!worryAboutDuplicates) {
          return index;
        }
        // Of course, we have to use binary search to find the precise
        // breakpoint...
        lower = index;
        // Everything between lower and upper inclusive compares at >= 0.
        while (lower < upper) {
          int middle = lower + (upper - lower + 1) / 2;
          int c = comparator.compare(list.get(middle), e);
          if (c > 0) {
            upper = middle - 1;
          } else { // c == 0
            lower = middle;
          }
        }
        return lower;
      }

      @Override <E> int exactMatchNotFound(List<? extends E> list, E e,
          int higherIndex, Comparator<? super E> comparator) {
        return higherIndex;
      }
    },

    /**
     * The relation that specifies the least element strictly greater than the
     * value.  In the case of duplicates, the first such element is chosen.
     *
     * <p>
     * In the {@link SortedLists#binarySearch binarySearch} methods, {@code
     * list.size()} is considered to be the index of positive infinity. That is,
     * if there is no element in the list greater than a given value, {@code -1}
     * is returned as the index of the higher element. For example: <pre> {@code
     * 
     * Integer low = 1;
     * Integer mid = 2;
     * Integer high = 3;
     * List<Integer> list = Arrays.asList(2, 2, 2);
     * binarySearch(list, low, Ordering.natural(), HIGHER); // 0
     * binarySearch(list, mid, Ordering.natural(), HIGHER); // 3
     * binarySearch(list, high, Ordering.natural(), HIGHER); // 3
     * }</pre>
     */
    HIGHER {

      @Override Relation reverse() {
        return LOWER;
      }

      @Override <E> int exactMatchFound(List<? extends E> list, E e,
          int lower, int index, int upper, Comparator<? super E> comparator,
          boolean worryAboutDuplicates) {
        return CEILING.exactMatchFound(
            list, e, lower, index, upper, comparator, worryAboutDuplicates)
            + 1;
      }

      @Override <E> int exactMatchNotFound(List<? extends E> list, E e,
          int higherIndex, Comparator<? super E> comparator) {
        return higherIndex;
      }
    };

    /**
     * The reverse order counterpart of the relation. Useful for descending
     * views.
     */
    abstract Relation reverse();

    /**
     * Given that {@code list.get(lower - 1) < list.get(index) = e <
     * list.get(upper + 1)} according to this comparator, find the index of the
     * element with this relation.
     *
     * <p>
     * {@code 0 <= lower <= index <= upper < list.size()}.
     */
    abstract <E> int exactMatchFound(List<? extends E> list, @Nullable E e,
        int lower, int index, int upper, Comparator<? super E> comparator,
        boolean worryAboutDuplicates);

    /**
     * Given that {@code list.get(higherIndex - 1) < e <
     * list.get(higherIndex)} according to this comparator, find the index of
     * the element with this relation.
     *
     * <p>
     * {@code 0 <= higherIndex <= list.size()}.
     */
    abstract <E> int exactMatchNotFound(List<? extends E> list, @Nullable E e,
        int higherIndex, Comparator<? super E> comparator);
  }

  /**
   * Searches the specified list for the specified object using the binary
   * search algorithm. The list must be sorted into ascending order according to
   * the specified comparator (as by the {@link Collections#sort(List,
   * Comparator) Collections.sort(List, Comparator)} method), prior to making
   * this call. If it is not sorted, the results are undefined.
   *
   * <p>Returns the index of the element in the list which has the specified
   * {@code Relation} to the specified object. So as to provide meaningful
   * results in all cases, {@code -1} is considered to be the index of negative
   * infinity, and {@code list.size()} is considered to be the index of positive
   * infinity. The exception is {@link Relation#EQUAL EQUAL}. If {@code EQUAL}
   * is specified and no equal element is found, {@code -1} is returned, but it
   * should not be interpreted as "negative infinity."
   *
   * <p>If there are duplicate elements, see the documentation on the relation
   * for more details.
   *
   * <p>This method runs in log(n) time for a random access list (which
   * provides near-constant-time positional access).
   *
   * @param list the list to be searched.
   * @param e the value to be searched for.
   * @param comparator the comparator by which the list is ordered.
   * @return the index of element with the specified relation to the search key,
   *         if it is contained in the list. Otherwise, if negative infinity has
   *         the specified relation (or if {@code relation} is {@code EQUAL} and
   *         the search key was not in the list), returns {@code -1}, or if
   *         positive infinity has the specified relation, returns {@code
   *         list.size()}.
   * @throws NullPointerException if {@code key} is null and the specified 
   *         comparator does not accept null values.
   * @throws ClassCastException if the list contains elements that are not
   *         <i>mutually comparable</i> using the specified comparator, or the
   *         search key is not mutually comparable with the elements of the list
   *         using this comparator.
   */
  static <E> int binarySearch(List<? extends E> list, @Nullable E e,
      Comparator<? super E> comparator, Relation relation) {
    return binarySearch(list, e, comparator, relation, true);
  }
  
  static <E> int binarySearch(List<? extends E> list, @Nullable E e,
      Comparator<? super E> comparator, Relation relation,
      boolean worryAboutDuplicates) {

    checkNotNull(comparator);
    checkNotNull(relation);

    int lower = 0;
    int upper = list.size() - 1;

    while (lower <= upper) {
      int middle = lower + (upper - lower) / 2;
      int c = comparator.compare(e, list.get(middle));
      if (c < 0) {
        upper = middle - 1;
      } else if (c > 0) {
        lower = middle + 1;
      } else {
        return relation.exactMatchFound(
            list, e, lower, middle, upper, comparator, worryAboutDuplicates);
      }
    }

    return relation.exactMatchNotFound(list, e, lower, comparator);
  }
}
