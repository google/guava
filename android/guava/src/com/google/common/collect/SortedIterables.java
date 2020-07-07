/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import java.util.Comparator;
import java.util.SortedSet;

/**
 * Utilities for dealing with sorted collections of all types.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class SortedIterables {
  private SortedIterables() {}

  /**
   * Returns {@code true} if {@code elements} is a sorted collection using an ordering equivalent to
   * {@code comparator}.
   */
  public static boolean hasSameComparator(Comparator<?> comparator, Iterable<?> elements) {
    checkNotNull(comparator);
    checkNotNull(elements);
    Comparator<?> comparator2;
    if (elements instanceof SortedSet) {
      comparator2 = comparator((SortedSet<?>) elements);
    } else if (elements instanceof SortedIterable) {
      comparator2 = ((SortedIterable<?>) elements).comparator();
    } else {
      return false;
    }
    return comparator.equals(comparator2);
  }

  @SuppressWarnings("unchecked")
  // if sortedSet.comparator() is null, the set must be naturally ordered
  public static <E> Comparator<? super E> comparator(SortedSet<E> sortedSet) {
    Comparator<? super E> result = sortedSet.comparator();
    if (result == null) {
      result = (Comparator<? super E>) Ordering.natural();
    }
    return result;
  }
}
