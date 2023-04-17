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

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.testing.EqualsTester;
import java.util.Collections;
import java.util.Comparator;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

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

  public void testMinMaxNatural() {
    assertThat(Comparators.min(1, 2)).isEqualTo(1);
    assertThat(Comparators.min(2, 1)).isEqualTo(1);
    assertThat(Comparators.max(1, 2)).isEqualTo(2);
    assertThat(Comparators.max(2, 1)).isEqualTo(2);
  }

  public void testMinMaxNatural_equalInstances() {
    Foo a = new Foo(1);
    Foo b = new Foo(1);
    assertThat(Comparators.min(a, b)).isSameInstanceAs(a);
    assertThat(Comparators.max(a, b)).isSameInstanceAs(a);
  }

  public void testMinMaxComparator() {
    Comparator<Integer> natural = Ordering.natural();
    Comparator<Integer> reverse = Collections.reverseOrder(natural);
    assertThat(Comparators.min(1, 2, reverse)).isEqualTo(2);
    assertThat(Comparators.min(2, 1, reverse)).isEqualTo(2);
    assertThat(Comparators.max(1, 2, reverse)).isEqualTo(1);
    assertThat(Comparators.max(2, 1, reverse)).isEqualTo(1);
  }

  public void testMinMaxComparator_equalInstances() {
    Comparator<Foo> natural = Ordering.natural();
    Comparator<Foo> reverse = Collections.reverseOrder(natural);
    Foo a = new Foo(1);
    Foo b = new Foo(1);
    assertThat(Comparators.min(a, b, reverse)).isSameInstanceAs(a);
    assertThat(Comparators.max(a, b, reverse)).isSameInstanceAs(a);
  }

  private static class Foo implements Comparable<Foo> {
    final Integer value;

    Foo(int value) {
      this.value = value;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      return (o instanceof Foo) && ((Foo) o).value.equals(value);
    }

    @Override
    public int compareTo(Foo other) {
      return value.compareTo(other.value);
    }
  }
}
