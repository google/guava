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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.testing.SerializableTester.reserialize;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Ordering.ArbitraryOrdering;
import com.google.common.collect.Ordering.IncomparableValueException;
import com.google.common.collect.testing.Helpers;
import com.google.common.primitives.Ints;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.RandomAccess;
import javax.annotation.CheckForNull;
import junit.framework.TestCase;

/**
 * Unit tests for {@code Ordering}.
 *
 * @author Jesse Wilson
 */
@GwtCompatible(emulated = true)
public class OrderingTest extends TestCase {
  // TODO(cpovirk): some of these are inexplicably slow (20-30s) under GWT

  private final Ordering<Number> numberOrdering = new NumberOrdering();

  public void testAllEqual() {
    Ordering<Object> comparator = Ordering.allEqual();
    assertSame(comparator, comparator.reverse());

    assertEquals(0, comparator.compare(null, null));
    assertEquals(0, comparator.compare(new Object(), new Object()));
    assertEquals(0, comparator.compare("apples", "oranges"));
    assertSame(comparator, reserialize(comparator));
    assertEquals("Ordering.allEqual()", comparator.toString());

    List<String> strings = ImmutableList.of("b", "a", "d", "c");
    assertEquals(strings, comparator.sortedCopy(strings));
    assertEquals(strings, comparator.immutableSortedCopy(strings));
  }

  // From https://github.com/google/guava/issues/1342
  public void testComplicatedOrderingExample() {
    Integer nullInt = (Integer) null;
    Ordering<Iterable<Integer>> example =
        Ordering.<Integer>natural().nullsFirst().reverse().lexicographical().reverse().nullsLast();
    List<Integer> list1 = Lists.newArrayList();
    List<Integer> list2 = Lists.newArrayList(1);
    List<Integer> list3 = Lists.newArrayList(1, 1);
    List<Integer> list4 = Lists.newArrayList(1, 2);
    List<Integer> list5 = Lists.newArrayList(1, null, 2);
    List<Integer> list6 = Lists.newArrayList(2);
    List<Integer> list7 = Lists.newArrayList(nullInt);
    List<Integer> list8 = Lists.newArrayList(nullInt, nullInt);
    List<List<Integer>> list =
        Lists.newArrayList(list1, list2, list3, list4, list5, list6, list7, list8, null);
    List<List<Integer>> sorted = example.sortedCopy(list);

    // [[null, null], [null], [1, null, 2], [1, 1], [1, 2], [1], [2], [], null]
    assertThat(sorted)
        .containsExactly(
            Lists.newArrayList(nullInt, nullInt),
            Lists.newArrayList(nullInt),
            Lists.newArrayList(1, null, 2),
            Lists.newArrayList(1, 1),
            Lists.newArrayList(1, 2),
            Lists.newArrayList(1),
            Lists.newArrayList(2),
            Lists.newArrayList(),
            null)
        .inOrder();
  }

  public void testNatural() {
    Ordering<Integer> comparator = Ordering.natural();
    Helpers.testComparator(comparator, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE);
    try {
      comparator.compare(1, null);
      fail();
    } catch (NullPointerException expected) {
    }
    try {
      comparator.compare(null, 2);
      fail();
    } catch (NullPointerException expected) {
    }
    try {
      comparator.compare(null, null);
      fail();
    } catch (NullPointerException expected) {
    }
    assertSame(comparator, reserialize(comparator));
    assertEquals("Ordering.natural()", comparator.toString());
  }

  public void testFrom() {
    Ordering<String> caseInsensitiveOrdering = Ordering.from(String.CASE_INSENSITIVE_ORDER);
    assertEquals(0, caseInsensitiveOrdering.compare("A", "a"));
    assertTrue(caseInsensitiveOrdering.compare("a", "B") < 0);
    assertTrue(caseInsensitiveOrdering.compare("B", "a") > 0);

    @SuppressWarnings("deprecation") // test of deprecated method
    Ordering<String> orderingFromOrdering = Ordering.from(Ordering.<String>natural());
    new EqualsTester()
        .addEqualityGroup(caseInsensitiveOrdering, Ordering.from(String.CASE_INSENSITIVE_ORDER))
        .addEqualityGroup(orderingFromOrdering, Ordering.natural())
        .testEquals();
  }

  public void testExplicit_none() {
    Comparator<Integer> c = Ordering.explicit(Collections.<Integer>emptyList());
    try {
      c.compare(0, 0);
      fail();
    } catch (IncomparableValueException expected) {
      assertEquals(0, expected.value);
    }
    reserializeAndAssert(c);
  }

  public void testExplicit_one() {
    Comparator<Integer> c = Ordering.explicit(0);
    assertEquals(0, c.compare(0, 0));
    try {
      c.compare(0, 1);
      fail();
    } catch (IncomparableValueException expected) {
      assertEquals(1, expected.value);
    }
    reserializeAndAssert(c);
    assertEquals("Ordering.explicit([0])", c.toString());
  }

  public void testExplicit_two() {
    Comparator<Integer> c = Ordering.explicit(42, 5);
    assertEquals(0, c.compare(5, 5));
    assertTrue(c.compare(5, 42) > 0);
    assertTrue(c.compare(42, 5) < 0);
    try {
      c.compare(5, 666);
      fail();
    } catch (IncomparableValueException expected) {
      assertEquals(666, expected.value);
    }
    new EqualsTester()
        .addEqualityGroup(c, Ordering.explicit(42, 5))
        .addEqualityGroup(Ordering.explicit(5, 42))
        .addEqualityGroup(Ordering.explicit(42))
        .testEquals();
    reserializeAndAssert(c);
  }

  public void testExplicit_sortingExample() {
    Comparator<Integer> c = Ordering.explicit(2, 8, 6, 1, 7, 5, 3, 4, 0, 9);
    List<Integer> list = Arrays.asList(0, 3, 5, 6, 7, 8, 9);
    Collections.sort(list, c);
    assertThat(list).containsExactly(8, 6, 7, 5, 3, 0, 9).inOrder();
    reserializeAndAssert(c);
  }

  public void testExplicit_withDuplicates() {
    try {
      Ordering.explicit(1, 2, 3, 4, 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  // A more limited test than the one that follows, but this one uses the
  // actual public API.
  public void testArbitrary_withoutCollisions() {
    List<Object> list = Lists.newArrayList();
    for (int i = 0; i < 50; i++) {
      list.add(new Object());
    }

    Ordering<Object> arbitrary = Ordering.arbitrary();
    Collections.sort(list, arbitrary);

    // Now we don't care what order it's put the list in, only that
    // comparing any pair of elements gives the answer we expect.
    Helpers.testComparator(arbitrary, list);

    assertEquals("Ordering.arbitrary()", arbitrary.toString());
  }

  public void testArbitrary_withCollisions() {
    List<Integer> list = Lists.newArrayList();
    for (int i = 0; i < 50; i++) {
      list.add(i);
    }

    Ordering<Object> arbitrary =
        new ArbitraryOrdering() {
          @Override
          int identityHashCode(Object object) {
            return ((Integer) object) % 5; // fake tons of collisions!
          }
        };

    // Don't let the elements be in such a predictable order
    list = shuffledCopy(list, new Random(1));

    Collections.sort(list, arbitrary);

    // Now we don't care what order it's put the list in, only that
    // comparing any pair of elements gives the answer we expect.
    Helpers.testComparator(arbitrary, list);
  }

  public void testUsingToString() {
    Ordering<Object> ordering = Ordering.usingToString();
    Helpers.testComparator(ordering, 1, 12, 124, 2);
    assertEquals("Ordering.usingToString()", ordering.toString());
    assertSame(ordering, reserialize(ordering));
  }

  // use an enum to get easy serializability
  private enum CharAtFunction implements Function<String, Character> {
    AT0(0),
    AT1(1),
    AT2(2),
    AT3(3),
    AT4(4),
    AT5(5),
    ;

    final int index;

    CharAtFunction(int index) {
      this.index = index;
    }

    @Override
    public Character apply(String string) {
      return string.charAt(index);
    }
  }

  private static Ordering<String> byCharAt(int index) {
    return Ordering.natural().onResultOf(CharAtFunction.values()[index]);
  }

  public void testCompound_static() {
    Comparator<String> comparator =
        Ordering.compound(
            ImmutableList.of(
                byCharAt(0), byCharAt(1), byCharAt(2), byCharAt(3), byCharAt(4), byCharAt(5)));
    Helpers.testComparator(
        comparator,
        ImmutableList.of(
            "applesauce",
            "apricot",
            "artichoke",
            "banality",
            "banana",
            "banquet",
            "tangelo",
            "tangerine"));
    reserializeAndAssert(comparator);
  }

  public void testCompound_instance() {
    Comparator<String> comparator = byCharAt(1).compound(byCharAt(0));
    Helpers.testComparator(
        comparator,
        ImmutableList.of("red", "yellow", "violet", "blue", "indigo", "green", "orange"));
  }

  public void testCompound_instance_generics() {
    Ordering<Object> objects = Ordering.explicit((Object) 1);
    Ordering<Number> numbers = Ordering.explicit((Number) 1);
    Ordering<Integer> integers = Ordering.explicit(1);

    // Like by like equals like
    Ordering<Number> a = numbers.compound(numbers);

    // The compound takes the more specific type of the two, regardless of order

    Ordering<Number> b = numbers.compound(objects);
    Ordering<Number> c = objects.compound(numbers);

    Ordering<Integer> d = numbers.compound(integers);
    Ordering<Integer> e = integers.compound(numbers);

    // This works with three levels too (IDEA falsely reports errors as noted
    // below. Both javac and eclipse handle these cases correctly.)

    Ordering<Number> f = numbers.compound(objects).compound(objects); // bad IDEA
    Ordering<Number> g = objects.compound(numbers).compound(objects);
    Ordering<Number> h = objects.compound(objects).compound(numbers);

    Ordering<Number> i = numbers.compound(objects.compound(objects));
    Ordering<Number> j = objects.compound(numbers.compound(objects)); // bad IDEA
    Ordering<Number> k = objects.compound(objects.compound(numbers));

    // You can also arbitrarily assign a more restricted type - not an intended
    // feature, exactly, but unavoidable (I think) and harmless
    Ordering<Integer> l = objects.compound(numbers);

    // This correctly doesn't work:
    // Ordering<Object> m = numbers.compound(objects);

    // Sadly, the following works in javac 1.6, but at least it fails for
    // eclipse, and is *correctly* highlighted red in IDEA.
    // Ordering<Object> n = objects.compound(numbers);
  }

  public void testReverse() {
    Ordering<Number> reverseOrder = numberOrdering.reverse();
    Helpers.testComparator(reverseOrder, Integer.MAX_VALUE, 1, 0, -1, Integer.MIN_VALUE);

    new EqualsTester()
        .addEqualityGroup(reverseOrder, numberOrdering.reverse())
        .addEqualityGroup(Ordering.natural().reverse())
        .addEqualityGroup(Collections.reverseOrder())
        .testEquals();
  }

  public void testReverseOfReverseSameAsForward() {
    // Not guaranteed by spec, but it works, and saves us from testing
    // exhaustively
    assertSame(numberOrdering, numberOrdering.reverse().reverse());
  }

  private enum StringLengthFunction implements Function<String, Integer> {
    StringLength;

    @Override
    public Integer apply(String string) {
      return string.length();
    }
  }

  private static final Ordering<Integer> DECREASING_INTEGER = Ordering.natural().reverse();

  public void testOnResultOf_natural() {
    Comparator<String> comparator =
        Ordering.natural().onResultOf(StringLengthFunction.StringLength);
    assertTrue(comparator.compare("to", "be") == 0);
    assertTrue(comparator.compare("or", "not") < 0);
    assertTrue(comparator.compare("that", "to") > 0);

    new EqualsTester()
        .addEqualityGroup(
            comparator, Ordering.natural().onResultOf(StringLengthFunction.StringLength))
        .addEqualityGroup(DECREASING_INTEGER)
        .testEquals();
    reserializeAndAssert(comparator);
    assertEquals("Ordering.natural().onResultOf(StringLength)", comparator.toString());
  }

  public void testOnResultOf_chained() {
    Comparator<String> comparator =
        DECREASING_INTEGER.onResultOf(StringLengthFunction.StringLength);
    assertTrue(comparator.compare("to", "be") == 0);
    assertTrue(comparator.compare("not", "or") < 0);
    assertTrue(comparator.compare("to", "that") > 0);

    new EqualsTester()
        .addEqualityGroup(
            comparator, DECREASING_INTEGER.onResultOf(StringLengthFunction.StringLength))
        .addEqualityGroup(DECREASING_INTEGER.onResultOf(Functions.constant(1)))
        .addEqualityGroup(Ordering.natural())
        .testEquals();
    reserializeAndAssert(comparator);
    assertEquals("Ordering.natural().reverse().onResultOf(StringLength)", comparator.toString());
  }

  @SuppressWarnings("unchecked") // dang varargs
  public void testLexicographical() {
    Ordering<String> ordering = Ordering.natural();
    Ordering<Iterable<String>> lexy = ordering.lexicographical();

    ImmutableList<String> empty = ImmutableList.of();
    ImmutableList<String> a = ImmutableList.of("a");
    ImmutableList<String> aa = ImmutableList.of("a", "a");
    ImmutableList<String> ab = ImmutableList.of("a", "b");
    ImmutableList<String> b = ImmutableList.of("b");

    Helpers.testComparator(lexy, empty, a, aa, ab, b);

    new EqualsTester()
        .addEqualityGroup(lexy, ordering.lexicographical())
        .addEqualityGroup(numberOrdering.lexicographical())
        .addEqualityGroup(Ordering.natural())
        .testEquals();
  }

  public void testNullsFirst() {
    Ordering<Integer> ordering = Ordering.natural().nullsFirst();
    Helpers.testComparator(ordering, null, Integer.MIN_VALUE, 0, 1);

    new EqualsTester()
        .addEqualityGroup(ordering, Ordering.natural().nullsFirst())
        .addEqualityGroup(numberOrdering.nullsFirst())
        .addEqualityGroup(Ordering.natural())
        .testEquals();
  }

  public void testNullsLast() {
    Ordering<Integer> ordering = Ordering.natural().nullsLast();
    Helpers.testComparator(ordering, 0, 1, Integer.MAX_VALUE, null);

    new EqualsTester()
        .addEqualityGroup(ordering, Ordering.natural().nullsLast())
        .addEqualityGroup(numberOrdering.nullsLast())
        .addEqualityGroup(Ordering.natural())
        .testEquals();
  }

  public void testBinarySearch() {
    List<Integer> ints = Lists.newArrayList(0, 2, 3, 5, 7, 9);
    assertEquals(4, numberOrdering.binarySearch(ints, 7));
  }

  public void testSortedCopy() {
    List<Integer> unsortedInts = Collections.unmodifiableList(Arrays.asList(5, 0, 3, null, 0, 9));
    List<Integer> sortedInts = numberOrdering.nullsLast().sortedCopy(unsortedInts);
    assertEquals(Arrays.asList(0, 0, 3, 5, 9, null), sortedInts);

    assertEquals(
        Collections.emptyList(), numberOrdering.sortedCopy(Collections.<Integer>emptyList()));
  }

  public void testImmutableSortedCopy() {
    ImmutableList<Integer> unsortedInts = ImmutableList.of(5, 3, 0, 9, 3);
    ImmutableList<Integer> sortedInts = numberOrdering.immutableSortedCopy(unsortedInts);
    assertEquals(Arrays.asList(0, 3, 3, 5, 9), sortedInts);

    assertEquals(
        Collections.<Integer>emptyList(),
        numberOrdering.immutableSortedCopy(Collections.<Integer>emptyList()));

    List<Integer> listWithNull = Arrays.asList(5, 3, null, 9);
    try {
      Ordering.natural().nullsFirst().immutableSortedCopy(listWithNull);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testIsOrdered() {
    assertFalse(numberOrdering.isOrdered(asList(5, 3, 0, 9)));
    assertFalse(numberOrdering.isOrdered(asList(0, 5, 3, 9)));
    assertTrue(numberOrdering.isOrdered(asList(0, 3, 5, 9)));
    assertTrue(numberOrdering.isOrdered(asList(0, 0, 3, 3)));
    assertTrue(numberOrdering.isOrdered(asList(0, 3)));
    assertTrue(numberOrdering.isOrdered(Collections.singleton(1)));
    assertTrue(numberOrdering.isOrdered(Collections.<Integer>emptyList()));
  }

  public void testIsStrictlyOrdered() {
    assertFalse(numberOrdering.isStrictlyOrdered(asList(5, 3, 0, 9)));
    assertFalse(numberOrdering.isStrictlyOrdered(asList(0, 5, 3, 9)));
    assertTrue(numberOrdering.isStrictlyOrdered(asList(0, 3, 5, 9)));
    assertFalse(numberOrdering.isStrictlyOrdered(asList(0, 0, 3, 3)));
    assertTrue(numberOrdering.isStrictlyOrdered(asList(0, 3)));
    assertTrue(numberOrdering.isStrictlyOrdered(Collections.singleton(1)));
    assertTrue(numberOrdering.isStrictlyOrdered(Collections.<Integer>emptyList()));
  }

  public void testLeastOfIterable_empty_0() {
    List<Integer> result = numberOrdering.leastOf(Arrays.<Integer>asList(), 0);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.<Integer>of(), result);
  }

  public void testLeastOfIterator_empty_0() {
    List<Integer> result = numberOrdering.leastOf(Iterators.<Integer>emptyIterator(), 0);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.<Integer>of(), result);
  }

  public void testLeastOfIterable_empty_1() {
    List<Integer> result = numberOrdering.leastOf(Arrays.<Integer>asList(), 1);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.<Integer>of(), result);
  }

  public void testLeastOfIterator_empty_1() {
    List<Integer> result = numberOrdering.leastOf(Iterators.<Integer>emptyIterator(), 1);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.<Integer>of(), result);
  }

  public void testLeastOfIterable_simple_negativeOne() {
    try {
      numberOrdering.leastOf(Arrays.asList(3, 4, 5, -1), -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testLeastOfIterator_simple_negativeOne() {
    try {
      numberOrdering.leastOf(Iterators.forArray(3, 4, 5, -1), -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testLeastOfIterable_singleton_0() {
    List<Integer> result = numberOrdering.leastOf(Arrays.asList(3), 0);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.<Integer>of(), result);
  }

  public void testLeastOfIterator_singleton_0() {
    List<Integer> result = numberOrdering.leastOf(Iterators.singletonIterator(3), 0);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.<Integer>of(), result);
  }

  public void testLeastOfIterable_simple_0() {
    List<Integer> result = numberOrdering.leastOf(Arrays.asList(3, 4, 5, -1), 0);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.<Integer>of(), result);
  }

  public void testLeastOfIterator_simple_0() {
    List<Integer> result = numberOrdering.leastOf(Iterators.forArray(3, 4, 5, -1), 0);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.<Integer>of(), result);
  }

  public void testLeastOfIterable_simple_1() {
    List<Integer> result = numberOrdering.leastOf(Arrays.asList(3, 4, 5, -1), 1);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.of(-1), result);
  }

  public void testLeastOfIterator_simple_1() {
    List<Integer> result = numberOrdering.leastOf(Iterators.forArray(3, 4, 5, -1), 1);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.of(-1), result);
  }

  public void testLeastOfIterable_simple_nMinusOne_withNullElement() {
    List<Integer> list = Arrays.asList(3, null, 5, -1);
    List<Integer> result = Ordering.natural().nullsLast().leastOf(list, list.size() - 1);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.of(-1, 3, 5), result);
  }

  public void testLeastOfIterator_simple_nMinusOne_withNullElement() {
    Iterator<Integer> itr = Iterators.forArray(3, null, 5, -1);
    List<Integer> result = Ordering.natural().nullsLast().leastOf(itr, 3);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.of(-1, 3, 5), result);
  }

  public void testLeastOfIterable_simple_nMinusOne() {
    List<Integer> list = Arrays.asList(3, 4, 5, -1);
    List<Integer> result = numberOrdering.leastOf(list, list.size() - 1);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.of(-1, 3, 4), result);
  }

  public void testLeastOfIterator_simple_nMinusOne() {
    List<Integer> list = Arrays.asList(3, 4, 5, -1);
    List<Integer> result = numberOrdering.leastOf(list.iterator(), list.size() - 1);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.of(-1, 3, 4), result);
  }

  public void testLeastOfIterable_simple_n() {
    List<Integer> list = Arrays.asList(3, 4, 5, -1);
    List<Integer> result = numberOrdering.leastOf(list, list.size());
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.of(-1, 3, 4, 5), result);
  }

  public void testLeastOfIterator_simple_n() {
    List<Integer> list = Arrays.asList(3, 4, 5, -1);
    List<Integer> result = numberOrdering.leastOf(list.iterator(), list.size());
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.of(-1, 3, 4, 5), result);
  }

  public void testLeastOfIterable_simple_n_withNullElement() {
    List<Integer> list = Arrays.asList(3, 4, 5, null, -1);
    List<Integer> result = Ordering.natural().nullsLast().leastOf(list, list.size());
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(Arrays.asList(-1, 3, 4, 5, null), result);
  }

  public void testLeastOfIterator_simple_n_withNullElement() {
    List<Integer> list = Arrays.asList(3, 4, 5, null, -1);
    List<Integer> result = Ordering.natural().nullsLast().leastOf(list.iterator(), list.size());
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(Arrays.asList(-1, 3, 4, 5, null), result);
  }

  public void testLeastOfIterable_simple_nPlusOne() {
    List<Integer> list = Arrays.asList(3, 4, 5, -1);
    List<Integer> result = numberOrdering.leastOf(list, list.size() + 1);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.of(-1, 3, 4, 5), result);
  }

  public void testLeastOfIterator_simple_nPlusOne() {
    List<Integer> list = Arrays.asList(3, 4, 5, -1);
    List<Integer> result = numberOrdering.leastOf(list.iterator(), list.size() + 1);
    assertTrue(result instanceof RandomAccess);
    assertListImmutable(result);
    assertEquals(ImmutableList.of(-1, 3, 4, 5), result);
  }

  public void testLeastOfIterable_ties() {
    Integer foo = new Integer(Integer.MAX_VALUE - 10);
    Integer bar = new Integer(Integer.MAX_VALUE - 10);

    assertNotSame(foo, bar);
    assertEquals(foo, bar);

    List<Integer> list = Arrays.asList(3, foo, bar, -1);
    List<Integer> result = numberOrdering.leastOf(list, list.size());
    assertEquals(ImmutableList.of(-1, 3, foo, bar), result);
  }

  public void testLeastOfIterator_ties() {
    Integer foo = new Integer(Integer.MAX_VALUE - 10);
    Integer bar = new Integer(Integer.MAX_VALUE - 10);

    assertNotSame(foo, bar);
    assertEquals(foo, bar);

    List<Integer> list = Arrays.asList(3, foo, bar, -1);
    List<Integer> result = numberOrdering.leastOf(list.iterator(), list.size());
    assertEquals(ImmutableList.of(-1, 3, foo, bar), result);
  }

  @GwtIncompatible // slow
  public void testLeastOf_reconcileAgainstSortAndSublist() {
    runLeastOfComparison(1000, 300, 20);
  }

  public void testLeastOf_reconcileAgainstSortAndSublistSmall() {
    runLeastOfComparison(10, 30, 2);
  }

  private static void runLeastOfComparison(int iterations, int elements, int seeds) {
    Random random = new Random(42);
    Ordering<Integer> ordering = Ordering.natural();

    for (int i = 0; i < iterations; i++) {
      List<Integer> list = Lists.newArrayList();
      for (int j = 0; j < elements; j++) {
        list.add(random.nextInt(10 * i + j + 1));
      }

      for (int seed = 1; seed < seeds; seed++) {
        int k = random.nextInt(10 * seed);
        assertEquals(ordering.sortedCopy(list).subList(0, k), ordering.leastOf(list, k));
      }
    }
  }

  public void testLeastOfIterableLargeK() {
    List<Integer> list = Arrays.asList(4, 2, 3, 5, 1);
    assertEquals(Arrays.asList(1, 2, 3, 4, 5), Ordering.natural().leastOf(list, Integer.MAX_VALUE));
  }

  public void testLeastOfIteratorLargeK() {
    List<Integer> list = Arrays.asList(4, 2, 3, 5, 1);
    assertEquals(
        Arrays.asList(1, 2, 3, 4, 5),
        Ordering.natural().leastOf(list.iterator(), Integer.MAX_VALUE));
  }

  public void testGreatestOfIterable_simple() {
    /*
     * If greatestOf() promised to be implemented as reverse().leastOf(), this
     * test would be enough. It doesn't... but we'll cheat and act like it does
     * anyway. There's a comment there to remind us to fix this if we change it.
     */
    List<Integer> list = Arrays.asList(3, 1, 3, 2, 4, 2, 4, 3);
    assertEquals(Arrays.asList(4, 4, 3, 3), numberOrdering.greatestOf(list, 4));
  }

  public void testGreatestOfIterator_simple() {
    /*
     * If greatestOf() promised to be implemented as reverse().leastOf(), this
     * test would be enough. It doesn't... but we'll cheat and act like it does
     * anyway. There's a comment there to remind us to fix this if we change it.
     */
    List<Integer> list = Arrays.asList(3, 1, 3, 2, 4, 2, 4, 3);
    assertEquals(Arrays.asList(4, 4, 3, 3), numberOrdering.greatestOf(list.iterator(), 4));
  }

  private static void assertListImmutable(List<Integer> result) {
    try {
      result.set(0, 1);
      fail();
    } catch (UnsupportedOperationException expected) {
      // pass
    }
  }

  public void testIteratorMinAndMax() {
    List<Integer> ints = Lists.newArrayList(5, 3, 0, 9);
    assertEquals(9, (int) numberOrdering.max(ints.iterator()));
    assertEquals(0, (int) numberOrdering.min(ints.iterator()));

    // when the values are the same, the first argument should be returned
    Integer a = new Integer(4);
    Integer b = new Integer(4);
    ints = Lists.newArrayList(a, b, b);
    assertSame(a, numberOrdering.max(ints.iterator()));
    assertSame(a, numberOrdering.min(ints.iterator()));
  }

  public void testIteratorMinExhaustsIterator() {
    List<Integer> ints = Lists.newArrayList(9, 0, 3, 5);
    Iterator<Integer> iterator = ints.iterator();
    assertEquals(0, (int) numberOrdering.min(iterator));
    assertFalse(iterator.hasNext());
  }

  public void testIteratorMaxExhaustsIterator() {
    List<Integer> ints = Lists.newArrayList(9, 0, 3, 5);
    Iterator<Integer> iterator = ints.iterator();
    assertEquals(9, (int) numberOrdering.max(iterator));
    assertFalse(iterator.hasNext());
  }

  public void testIterableMinAndMax() {
    List<Integer> ints = Lists.newArrayList(5, 3, 0, 9);
    assertEquals(9, (int) numberOrdering.max(ints));
    assertEquals(0, (int) numberOrdering.min(ints));

    // when the values are the same, the first argument should be returned
    Integer a = new Integer(4);
    Integer b = new Integer(4);
    ints = Lists.newArrayList(a, b, b);
    assertSame(a, numberOrdering.max(ints));
    assertSame(a, numberOrdering.min(ints));
  }

  public void testVarargsMinAndMax() {
    // try the min and max values in all positions, since some values are proper
    // parameters and others are from the varargs array
    assertEquals(9, (int) numberOrdering.max(9, 3, 0, 5, 8));
    assertEquals(9, (int) numberOrdering.max(5, 9, 0, 3, 8));
    assertEquals(9, (int) numberOrdering.max(5, 3, 9, 0, 8));
    assertEquals(9, (int) numberOrdering.max(5, 3, 0, 9, 8));
    assertEquals(9, (int) numberOrdering.max(5, 3, 0, 8, 9));
    assertEquals(0, (int) numberOrdering.min(0, 3, 5, 9, 8));
    assertEquals(0, (int) numberOrdering.min(5, 0, 3, 9, 8));
    assertEquals(0, (int) numberOrdering.min(5, 3, 0, 9, 8));
    assertEquals(0, (int) numberOrdering.min(5, 3, 9, 0, 8));
    assertEquals(0, (int) numberOrdering.min(5, 3, 0, 9, 0));

    // when the values are the same, the first argument should be returned
    Integer a = new Integer(4);
    Integer b = new Integer(4);
    assertSame(a, numberOrdering.max(a, b, b));
    assertSame(a, numberOrdering.min(a, b, b));
  }

  public void testParameterMinAndMax() {
    assertEquals(5, (int) numberOrdering.max(3, 5));
    assertEquals(5, (int) numberOrdering.max(5, 3));
    assertEquals(3, (int) numberOrdering.min(3, 5));
    assertEquals(3, (int) numberOrdering.min(5, 3));

    // when the values are the same, the first argument should be returned
    Integer a = new Integer(4);
    Integer b = new Integer(4);
    assertSame(a, numberOrdering.max(a, b));
    assertSame(a, numberOrdering.min(a, b));
  }

  private static class NumberOrdering extends Ordering<Number> {
    @Override
    public int compare(Number a, Number b) {
      return ((Double) a.doubleValue()).compareTo(b.doubleValue());
    }

    @Override
    public int hashCode() {
      return NumberOrdering.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof NumberOrdering;
    }

    private static final long serialVersionUID = 0;
  }

  /*
   * Now we have monster tests that create hundreds of Orderings using different
   * combinations of methods, then checks compare(), binarySearch() and so
   * forth on each one.
   */

  // should periodically try increasing this, but it makes the test run long
  private static final int RECURSE_DEPTH = 2;

  public void testCombinationsExhaustively_startingFromNatural() {
    testExhaustively(Ordering.<String>natural(), "a", "b", "d");
  }

  @GwtIncompatible // too slow
  public void testCombinationsExhaustively_startingFromExplicit() {
    testExhaustively(Ordering.explicit("a", "b", "c", "d"), "a", "b", "d");
  }

  @GwtIncompatible // too slow
  public void testCombinationsExhaustively_startingFromUsingToString() {
    testExhaustively(Ordering.usingToString(), 1, 12, 2);
  }

  @GwtIncompatible // too slow
  public void testCombinationsExhaustively_startingFromFromComparator() {
    testExhaustively(Ordering.from(String.CASE_INSENSITIVE_ORDER), "A", "b", "C", "d");
  }

  @GwtIncompatible // too slow
  public void testCombinationsExhaustively_startingFromArbitrary() {
    Ordering<Object> arbitrary = Ordering.arbitrary();
    Object[] array = {1, "foo", new Object()};

    // There's no way to tell what the order should be except empirically
    Arrays.sort(array, arbitrary);
    testExhaustively(arbitrary, array);
  }

  /**
   * Requires at least 3 elements in {@code strictlyOrderedElements} in order to test the varargs
   * version of min/max.
   */
  private static <T> void testExhaustively(
      Ordering<? super T> ordering, T... strictlyOrderedElements) {
    checkArgument(
        strictlyOrderedElements.length >= 3,
        "strictlyOrderedElements " + "requires at least 3 elements");
    List<T> list = Arrays.asList(strictlyOrderedElements);

    // for use calling Collection.toArray later
    T[] emptyArray = Platform.newArray(strictlyOrderedElements, 0);

    // shoot me, but I didn't want to deal with wildcards through the whole test
    @SuppressWarnings("unchecked")
    Scenario<T> starter = new Scenario<>((Ordering) ordering, list, emptyArray);
    verifyScenario(starter, 0);
  }

  private static <T> void verifyScenario(Scenario<T> scenario, int level) {
    scenario.testCompareTo();
    scenario.testIsOrdered();
    scenario.testMinAndMax();
    scenario.testBinarySearch();
    scenario.testSortedCopy();

    if (level < RECURSE_DEPTH) {
      for (OrderingMutation alteration : OrderingMutation.values()) {
        verifyScenario(alteration.mutate(scenario), level + 1);
      }
    }
  }

  /**
   * An aggregation of an ordering with a list (of size > 1) that should prove to be in strictly
   * increasing order according to that ordering.
   */
  private static class Scenario<T> {
    final Ordering<T> ordering;
    final List<T> strictlyOrderedList;
    final T[] emptyArray;

    Scenario(Ordering<T> ordering, List<T> strictlyOrderedList, T[] emptyArray) {
      this.ordering = ordering;
      this.strictlyOrderedList = strictlyOrderedList;
      this.emptyArray = emptyArray;
    }

    void testCompareTo() {
      Helpers.testComparator(ordering, strictlyOrderedList);
    }

    void testIsOrdered() {
      assertTrue(ordering.isOrdered(strictlyOrderedList));
      assertTrue(ordering.isStrictlyOrdered(strictlyOrderedList));
    }

    @SuppressWarnings("unchecked") // generic arrays and unchecked cast
    void testMinAndMax() {
      List<T> shuffledList = Lists.newArrayList(strictlyOrderedList);
      shuffledList = shuffledCopy(shuffledList, new Random(5));

      T min = strictlyOrderedList.get(0);
      T max = strictlyOrderedList.get(strictlyOrderedList.size() - 1);

      T first = shuffledList.get(0);
      T second = shuffledList.get(1);
      T third = shuffledList.get(2);
      T[] rest = shuffledList.subList(3, shuffledList.size()).toArray(emptyArray);

      assertEquals(min, ordering.min(shuffledList));
      assertEquals(min, ordering.min(shuffledList.iterator()));
      assertEquals(min, ordering.min(first, second, third, rest));
      assertEquals(min, ordering.min(min, max));
      assertEquals(min, ordering.min(max, min));

      assertEquals(max, ordering.max(shuffledList));
      assertEquals(max, ordering.max(shuffledList.iterator()));
      assertEquals(max, ordering.max(first, second, third, rest));
      assertEquals(max, ordering.max(min, max));
      assertEquals(max, ordering.max(max, min));
    }

    void testBinarySearch() {
      for (int i = 0; i < strictlyOrderedList.size(); i++) {
        assertEquals(i, ordering.binarySearch(strictlyOrderedList, strictlyOrderedList.get(i)));
      }
      List<T> newList = Lists.newArrayList(strictlyOrderedList);
      T valueNotInList = newList.remove(1);
      assertEquals(-2, ordering.binarySearch(newList, valueNotInList));
    }

    void testSortedCopy() {
      List<T> shuffledList = Lists.newArrayList(strictlyOrderedList);
      shuffledList = shuffledCopy(shuffledList, new Random(5));

      assertEquals(strictlyOrderedList, ordering.sortedCopy(shuffledList));

      if (!strictlyOrderedList.contains(null)) {
        assertEquals(strictlyOrderedList, ordering.immutableSortedCopy(shuffledList));
      }
    }
  }

  /**
   * A means for changing an Ordering into another Ordering. Each instance is responsible for
   * creating the alternate Ordering, and providing a List that is known to be ordered, based on an
   * input List known to be ordered according to the input Ordering.
   */
  private enum OrderingMutation {
    REVERSE {
      @Override
      <T> Scenario<?> mutate(Scenario<T> scenario) {
        List<T> newList = Lists.newArrayList(scenario.strictlyOrderedList);
        Collections.reverse(newList);
        return new Scenario<T>(scenario.ordering.reverse(), newList, scenario.emptyArray);
      }
    },
    NULLS_FIRST {
      @Override
      <T> Scenario<?> mutate(Scenario<T> scenario) {
        @SuppressWarnings("unchecked")
        List<T> newList = Lists.newArrayList((T) null);
        for (T t : scenario.strictlyOrderedList) {
          if (t != null) {
            newList.add(t);
          }
        }
        return new Scenario<T>(scenario.ordering.nullsFirst(), newList, scenario.emptyArray);
      }
    },
    NULLS_LAST {
      @Override
      <T> Scenario<?> mutate(Scenario<T> scenario) {
        List<T> newList = Lists.newArrayList();
        for (T t : scenario.strictlyOrderedList) {
          if (t != null) {
            newList.add(t);
          }
        }
        newList.add(null);
        return new Scenario<T>(scenario.ordering.nullsLast(), newList, scenario.emptyArray);
      }
    },
    ON_RESULT_OF {
      @Override
      <T> Scenario<?> mutate(final Scenario<T> scenario) {
        Ordering<Integer> ordering =
            scenario.ordering.onResultOf(
                new Function<Integer, T>() {
                  @Override
                  public T apply(@CheckForNull Integer from) {
                    return scenario.strictlyOrderedList.get(from);
                  }
                });
        List<Integer> list = Lists.newArrayList();
        for (int i = 0; i < scenario.strictlyOrderedList.size(); i++) {
          list.add(i);
        }
        return new Scenario<>(ordering, list, new Integer[0]);
      }
    },
    COMPOUND_THIS_WITH_NATURAL {
      @SuppressWarnings("unchecked") // raw array
      @Override
      <T> Scenario<?> mutate(Scenario<T> scenario) {
        List<Composite<T>> composites = Lists.newArrayList();
        for (T t : scenario.strictlyOrderedList) {
          composites.add(new Composite<T>(t, 1));
          composites.add(new Composite<T>(t, 2));
        }
        Ordering<Composite<T>> ordering =
            scenario
                .ordering
                .onResultOf(Composite.<T>getValueFunction())
                .compound(Ordering.natural());
        return new Scenario<Composite<T>>(ordering, composites, new Composite[0]);
      }
    },
    COMPOUND_NATURAL_WITH_THIS {
      @SuppressWarnings("unchecked") // raw array
      @Override
      <T> Scenario<?> mutate(Scenario<T> scenario) {
        List<Composite<T>> composites = Lists.newArrayList();
        for (T t : scenario.strictlyOrderedList) {
          composites.add(new Composite<T>(t, 1));
        }
        for (T t : scenario.strictlyOrderedList) {
          composites.add(new Composite<T>(t, 2));
        }
        Ordering<Composite<T>> ordering =
            Ordering.natural()
                .compound(scenario.ordering.onResultOf(Composite.<T>getValueFunction()));
        return new Scenario<Composite<T>>(ordering, composites, new Composite[0]);
      }
    },
    LEXICOGRAPHICAL {
      @SuppressWarnings("unchecked") // dang varargs
      @Override
      <T> Scenario<?> mutate(Scenario<T> scenario) {
        List<Iterable<T>> words = Lists.newArrayList();
        words.add(Collections.<T>emptyList());
        for (T t : scenario.strictlyOrderedList) {
          words.add(Arrays.asList(t));
          for (T s : scenario.strictlyOrderedList) {
            words.add(Arrays.asList(t, s));
          }
        }
        return new Scenario<Iterable<T>>(
            scenario.ordering.lexicographical(), words, new Iterable[0]);
      }
    },
    ;

    abstract <T> Scenario<?> mutate(Scenario<T> scenario);
  }

  /**
   * A dummy object we create so that we can have something meaningful to have a compound ordering
   * over.
   */
  private static class Composite<T> implements Comparable<Composite<T>> {
    final T value;
    final int rank;

    Composite(T value, int rank) {
      this.value = value;
      this.rank = rank;
    }

    // natural order is by rank only; the test will compound() this with the
    // order of 't'.
    @Override
    public int compareTo(Composite<T> that) {
      return Ints.compare(rank, that.rank);
    }

    static <T> Function<Composite<T>, T> getValueFunction() {
      return new Function<Composite<T>, T>() {
        @Override
        public T apply(Composite<T> from) {
          return from.value;
        }
      };
    }
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Ordering.class);

    // any Ordering<Object> instance that accepts nulls should be good enough
    tester.testAllPublicInstanceMethods(Ordering.usingToString().nullsFirst());
  }

  private static <T> List<T> shuffledCopy(List<T> in, Random random) {
    List<T> mutable = newArrayList(in);
    List<T> out = newArrayList();
    while (!mutable.isEmpty()) {
      out.add(mutable.remove(random.nextInt(mutable.size())));
    }
    return out;
  }
}
