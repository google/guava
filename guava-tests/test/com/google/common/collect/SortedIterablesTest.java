/*
 * Copyright (C) 2011 The Guava Authors
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

import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

/**
 * Unit tests for {@code SortedIterables}.
 * 
 * @author Louis Wasserman
 */
@GwtCompatible
public class SortedIterablesTest extends TestCase {
  @SuppressWarnings("unchecked")
  public void testSortedCounts() {
    List<Integer> list = Arrays.asList(3, 9, 8, 4, 5, 2, 2, 8);
    ASSERT.that(SortedIterables.sortedCounts(Ordering.natural(), list))
        .hasContentsInOrder(
            Multisets.immutableEntry(2, 2), Multisets.immutableEntry(3, 1),
            Multisets.immutableEntry(4, 1), Multisets.immutableEntry(5, 1),
            Multisets.immutableEntry(8, 2), Multisets.immutableEntry(9, 1));
  }

  @SuppressWarnings("unchecked")
  public void testSortedCountsIterator() {
    List<Integer> list = Arrays.asList(3, 9, 8, 4, 5, 2, 2, 8);
    ASSERT.that(SortedIterables.sortedCounts(Ordering.natural(), list.iterator()))
        .hasContentsInOrder(
            Multisets.immutableEntry(2, 2), Multisets.immutableEntry(3, 1),
            Multisets.immutableEntry(4, 1), Multisets.immutableEntry(5, 1),
            Multisets.immutableEntry(8, 2), Multisets.immutableEntry(9, 1));
  }

  public void testSameComparator() {
    assertTrue(SortedIterables.hasSameComparator(Ordering.natural(), Sets.newTreeSet()));
    // Before JDK6 (including under GWT), the TreeMap keySet is a plain Set.
    if (Maps.newTreeMap().keySet() instanceof SortedSet) {
      assertTrue(
          SortedIterables.hasSameComparator(Ordering.natural(), Maps.newTreeMap().keySet()));
    }
    assertTrue(SortedIterables.hasSameComparator(Ordering.natural().reverse(),
        Sets.newTreeSet(Ordering.natural().reverse())));
  }
}
