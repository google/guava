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

import static com.google.common.collect.Comparators.emptiesFirst;
import static com.google.common.collect.Comparators.emptiesLast;
import static com.google.common.collect.Comparators.isInOrder;
import static com.google.common.collect.Comparators.isInStrictOrder;
import static com.google.common.collect.Comparators.max;
import static com.google.common.collect.Comparators.min;
import static com.google.common.collect.testing.Helpers.testComparator;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;

import com.google.common.annotations.GwtCompatible;
import com.google.common.testing.EqualsTester;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@code Comparators}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@NullMarked
public class ComparatorsTest extends TestCase {
  public void testLexicographical() {
    Comparator<String> comparator = Ordering.natural();
    Comparator<Iterable<String>> lexy = Comparators.lexicographical(comparator);

    ImmutableList<String> empty = ImmutableList.of();
    ImmutableList<String> a = ImmutableList.of("a");
    ImmutableList<String> aa = ImmutableList.of("a", "a");
    ImmutableList<String> ab = ImmutableList.of("a", "b");
    ImmutableList<String> b = ImmutableList.of("b");

    testComparator(lexy, empty, a, aa, ab, b);

    new EqualsTester()
        .addEqualityGroup(lexy, Comparators.lexicographical(comparator))
        .addEqualityGroup(Comparators.lexicographical(String.CASE_INSENSITIVE_ORDER))
        .addEqualityGroup(Ordering.natural())
        .testEquals();
  }

  public void testIsInOrder() {
    assertFalse(isInOrder(asList(5, 3, 0, 9), Ordering.natural()));
    assertFalse(isInOrder(asList(0, 5, 3, 9), Ordering.natural()));
    assertTrue(isInOrder(asList(0, 3, 5, 9), Ordering.natural()));
    assertTrue(isInOrder(asList(0, 0, 3, 3), Ordering.natural()));
    assertTrue(isInOrder(asList(0, 3), Ordering.natural()));
    assertTrue(isInOrder(singleton(1), Ordering.natural()));
    assertTrue(isInOrder(ImmutableList.of(), Ordering.natural()));
  }

  public void testIsInStrictOrder() {
    assertFalse(isInStrictOrder(asList(5, 3, 0, 9), Ordering.natural()));
    assertFalse(isInStrictOrder(asList(0, 5, 3, 9), Ordering.natural()));
    assertTrue(isInStrictOrder(asList(0, 3, 5, 9), Ordering.natural()));
    assertFalse(isInStrictOrder(asList(0, 0, 3, 3), Ordering.natural()));
    assertTrue(isInStrictOrder(asList(0, 3), Ordering.natural()));
    assertTrue(isInStrictOrder(singleton(1), Ordering.natural()));
    assertTrue(isInStrictOrder(ImmutableList.of(), Ordering.natural()));
  }

  public void testEmptiesFirst() {
    Optional<String> empty = Optional.empty();
    Optional<String> abc = Optional.of("abc");
    Optional<String> z = Optional.of("z");

    Comparator<Optional<String>> comparator = emptiesFirst(comparing(String::length));
    testComparator(comparator, empty, z, abc);

    // Just demonstrate that no explicit type parameter is required
    Comparator<Optional<String>> unused = emptiesFirst(naturalOrder());
  }

  public void testEmptiesLast() {
    Optional<String> empty = Optional.empty();
    Optional<String> abc = Optional.of("abc");
    Optional<String> z = Optional.of("z");

    Comparator<Optional<String>> comparator = emptiesLast(comparing(String::length));
    testComparator(comparator, z, abc, empty);

    // Just demonstrate that no explicit type parameter is required
    Comparator<Optional<String>> unused = emptiesLast(naturalOrder());
  }

  public void testMinMaxNatural() {
    assertThat(min(1, 2)).isEqualTo(1);
    assertThat(min(2, 1)).isEqualTo(1);
    assertThat(max(1, 2)).isEqualTo(2);
    assertThat(max(2, 1)).isEqualTo(2);
  }

  public void testMinMaxNatural_equalInstances() {
    Foo a = new Foo(1);
    Foo b = new Foo(1);
    assertThat(min(a, b)).isSameInstanceAs(a);
    assertThat(max(a, b)).isSameInstanceAs(a);
  }

  public void testMinMaxComparator() {
    Comparator<Integer> reverse = reverseOrder();
    assertThat(min(1, 2, reverse)).isEqualTo(2);
    assertThat(min(2, 1, reverse)).isEqualTo(2);
    assertThat(max(1, 2, reverse)).isEqualTo(1);
    assertThat(max(2, 1, reverse)).isEqualTo(1);
  }

  /**
   * Fails compilation if the signature of min and max is changed to take {@code Comparator<T>}
   * instead of {@code Comparator<? super T>}.
   */
  public void testMinMaxWithSupertypeComparator() {
    Comparator<Number> numberComparator = comparing(Number::intValue);
    Integer comparand1 = 1;
    Integer comparand2 = 2;

    Integer min = min(comparand1, comparand2, numberComparator);
    Integer max = max(comparand1, comparand2, numberComparator);

    assertThat(min).isEqualTo(1);
    assertThat(max).isEqualTo(2);
  }

  public void testMinMaxComparator_equalInstances() {
    Comparator<Foo> natural = Ordering.natural();
    Comparator<Foo> reverse = Collections.reverseOrder(natural);
    Foo a = new Foo(1);
    Foo b = new Foo(1);
    assertThat(min(a, b, reverse)).isSameInstanceAs(a);
    assertThat(max(a, b, reverse)).isSameInstanceAs(a);
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
