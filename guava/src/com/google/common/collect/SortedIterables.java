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
import com.google.common.base.Function;
import com.google.common.collect.Multiset.Entry;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
   * Returns {@code true} if {@code elements} is a sorted collection using an ordering equivalent
   * to {@code comparator}.
   */
  public static boolean hasSameComparator(Comparator<?> comparator, Iterable<?> elements) {
    checkNotNull(comparator);
    checkNotNull(elements);
    Comparator<?> comparator2;
    if (elements instanceof SortedSet) {
      SortedSet<?> sortedSet = (SortedSet<?>) elements;
      comparator2 = sortedSet.comparator();
      if (comparator2 == null) {
        comparator2 = (Comparator) Ordering.natural();
      }
    } else if (elements instanceof SortedIterable) {
      comparator2 = ((SortedIterable<?>) elements).comparator();
    } else {
      comparator2 = null;
    }
    return comparator.equals(comparator2);
  }

  /**
   * Returns a sorted collection of the unique elements according to the specified comparator.  Does
   * not check for null.
   */
  @SuppressWarnings("unchecked")
  public static <E> Collection<E> sortedUnique(
      Comparator<? super E> comparator, Iterator<E> elements) {
    SortedSet<E> sortedSet = Sets.newTreeSet(comparator);
    Iterators.addAll(sortedSet, elements);
    return sortedSet;
  }

  /**
   * Returns a sorted collection of the unique elements according to the specified comparator. Does
   * not check for null.
   */
  @SuppressWarnings("unchecked")
  public static <E> Collection<E> sortedUnique(
      Comparator<? super E> comparator, Iterable<E> elements) {
    if (elements instanceof Multiset) {
      elements = ((Multiset<E>) elements).elementSet();
    }
    if (elements instanceof Set) {
      if (hasSameComparator(comparator, elements)) {
        return (Set<E>) elements;
      }
      List<E> list = Lists.newArrayList(elements);
      Collections.sort(list, comparator);
      return list;
    }
    E[] array = (E[]) Iterables.toArray(elements);
    if (!hasSameComparator(comparator, elements)) {
      Arrays.sort(array, comparator);
    }
    return uniquifySortedArray(comparator, array);
  }

  private static <E> Collection<E> uniquifySortedArray(
      Comparator<? super E> comparator, E[] array) {
    if (array.length == 0) {
      return Collections.emptySet();
    }
    int length = 1;
    for (int i = 1; i < array.length; i++) {
      int cmp = comparator.compare(array[i], array[length - 1]);
      if (cmp != 0) {
        array[length++] = array[i];
      }
    }
    if (length < array.length) {
      array = ObjectArrays.arraysCopyOf(array, length);
    }
    return Arrays.asList(array);
  }

  /**
   * Returns a collection of multiset entries representing the counts of the distinct elements, in
   * sorted order. Does not check for null.
   */
  public static <E> Collection<Multiset.Entry<E>> sortedCounts(
      Comparator<? super E> comparator, Iterator<E> elements) {
    TreeMultiset<E> multiset = TreeMultiset.create(comparator);
    Iterators.addAll(multiset, elements);
    return multiset.entrySet();
  }
  
  /**
   * Returns a collection of multiset entries representing the counts of the distinct elements, in
   * sorted order. Does not check for null.
   */
  public static <E> Collection<Multiset.Entry<E>> sortedCounts(
      Comparator<? super E> comparator, Iterable<E> elements) {
    if (elements instanceof Multiset) {
      Multiset<E> multiset = (Multiset<E>) elements;
      if (hasSameComparator(comparator, elements)) {
        return multiset.entrySet();
      }
      List<Multiset.Entry<E>> entries = Lists.newArrayList(multiset.entrySet());
      Collections.sort(
          entries, Ordering.from(comparator).onResultOf(new Function<Multiset.Entry<E>, E>() {
            @Override
            public E apply(Entry<E> entry) {
              return entry.getElement();
            }
          }));
      return entries;
    } else if (elements instanceof Set) { // known distinct
      Collection<E> sortedElements;
      if (hasSameComparator(comparator, elements)) {
        sortedElements = (Collection<E>) elements;
      } else {
        List<E> list = Lists.newArrayList(elements);
        Collections.sort(list, comparator);
        sortedElements = list;
      }
      return singletonEntries(sortedElements);
    } else if (hasSameComparator(comparator, elements)) {
      E current = null;
      int currentCount = 0;
      List<Multiset.Entry<E>> sortedEntries = Lists.newArrayList();
      for (E e : elements) {
        if (currentCount > 0) {
          if (comparator.compare(current, e) == 0) {
            currentCount++;
          } else {
            sortedEntries.add(Multisets.immutableEntry(current, currentCount));
            current = e;
            currentCount = 1;
          }
        } else {
          current = e;
          currentCount = 1;
        }
      }
      if (currentCount > 0) {
        sortedEntries.add(Multisets.immutableEntry(current, currentCount));
      }
      return sortedEntries;
    }
    TreeMultiset<E> multiset = TreeMultiset.create(comparator);
    Iterables.addAll(multiset, elements);
    return multiset.entrySet();
  }

  static <E> Collection<Multiset.Entry<E>> singletonEntries(Collection<E> set) {
    return Collections2.transform(set, new Function<E, Multiset.Entry<E>>() {
      @Override
      public Entry<E> apply(E elem) {
        return Multisets.immutableEntry(elem, 1);
      }
    });
  }
}
