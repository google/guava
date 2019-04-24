/*
 * Copyright (C) 2016 The Guava Authors
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

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.testing.CollectorTester;
import com.google.common.testing.EqualsTester;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import junit.framework.TestCase;

/**
 * Tests for {@code Comparators}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class ComparatorsTest extends TestCase {
  @SuppressWarnings("unchecked") // dang varargs
  public void testLexicographical() {
    Comparator<String> comparator = Ordering.natural();
    Comparator<Iterable<String>> lexy = Comparators.lexicographical(comparator);

    ImmutableList<String> empty = ImmutableList.of();
    ImmutableList<String> a = ImmutableList.of("a");
    ImmutableList<String> aa = ImmutableList.of("a", "a");
    ImmutableList<String> ab = ImmutableList.of("a", "b");
    ImmutableList<String> b = ImmutableList.of("b");

    Helpers.testComparator(lexy, empty, a, aa, ab, b);

    new EqualsTester()
        .addEqualityGroup(lexy, Comparators.lexicographical(comparator))
        .addEqualityGroup(Comparators.lexicographical(String.CASE_INSENSITIVE_ORDER))
        .addEqualityGroup(Ordering.natural())
        .testEquals();
  }

  public void testIsInOrder() {
    assertFalse(Comparators.isInOrder(asList(5, 3, 0, 9), Ordering.natural()));
    assertFalse(Comparators.isInOrder(asList(0, 5, 3, 9), Ordering.natural()));
    assertTrue(Comparators.isInOrder(asList(0, 3, 5, 9), Ordering.natural()));
    assertTrue(Comparators.isInOrder(asList(0, 0, 3, 3), Ordering.natural()));
    assertTrue(Comparators.isInOrder(asList(0, 3), Ordering.natural()));
    assertTrue(Comparators.isInOrder(Collections.singleton(1), Ordering.natural()));
    assertTrue(Comparators.isInOrder(Collections.<Integer>emptyList(), Ordering.natural()));
  }

  public void testIsInStrictOrder() {
    assertFalse(Comparators.isInStrictOrder(asList(5, 3, 0, 9), Ordering.natural()));
    assertFalse(Comparators.isInStrictOrder(asList(0, 5, 3, 9), Ordering.natural()));
    assertTrue(Comparators.isInStrictOrder(asList(0, 3, 5, 9), Ordering.natural()));
    assertFalse(Comparators.isInStrictOrder(asList(0, 0, 3, 3), Ordering.natural()));
    assertTrue(Comparators.isInStrictOrder(asList(0, 3), Ordering.natural()));
    assertTrue(Comparators.isInStrictOrder(Collections.singleton(1), Ordering.natural()));
    assertTrue(Comparators.isInStrictOrder(Collections.<Integer>emptyList(), Ordering.natural()));
  }

  public void testLeastCollector() {
    CollectorTester.of(Comparators.<Integer>least(2, Comparator.naturalOrder()))
        .expectCollects(Arrays.asList(1, 2), 1, 2, 3, 4, 5, 6)
        .expectCollects(Arrays.asList(1), 1)
        .expectCollects(Collections.emptyList());
  }

  public void testGreatestCollector() {
    CollectorTester.of(Comparators.<Integer>greatest(2, Comparator.naturalOrder()))
        .expectCollects(Arrays.asList(6, 5), 1, 2, 3, 4, 5, 6)
        .expectCollects(Arrays.asList(1), 1)
        .expectCollects(Collections.emptyList());
  }

  public void testEmptiesFirst() {
    Optional<String> empty = Optional.empty();
    Optional<String> abc = Optional.of("abc");
    Optional<String> z = Optional.of("z");

    Comparator<Optional<String>> comparator = Comparators.emptiesFirst(comparing(String::length));
    Helpers.testComparator(comparator, empty, z, abc);

    // Just demonstrate that no explicit type parameter is required
    comparator = Comparators.emptiesFirst(naturalOrder());
  }

  public void testEmptiesLast() {
    Optional<String> empty = Optional.empty();
    Optional<String> abc = Optional.of("abc");
    Optional<String> z = Optional.of("z");

    Comparator<Optional<String>> comparator = Comparators.emptiesLast(comparing(String::length));
    Helpers.testComparator(comparator, z, abc, empty);

    // Just demonstrate that no explicit type parameter is required
    comparator = Comparators.emptiesLast(naturalOrder());
  }
}
