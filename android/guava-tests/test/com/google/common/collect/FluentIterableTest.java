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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.testing.IteratorFeature;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.testing.NullPointerTester;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Unit test for {@link FluentIterable}.
 *
 * @author Marcin Mikosik
 */
@GwtCompatible(emulated = true)
public class FluentIterableTest extends TestCase {

  @GwtIncompatible // NullPointerTester
  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(FluentIterable.class);
  }

  public void testFromArrayAndAppend() {
    FluentIterable<TimeUnit> units =
        FluentIterable.from(TimeUnit.values()).append(TimeUnit.SECONDS);
  }

  public void testFromArrayAndIteratorRemove() {
    FluentIterable<TimeUnit> units = FluentIterable.from(TimeUnit.values());
    try {
      Iterables.removeIf(units, Predicates.equalTo(TimeUnit.SECONDS));
      fail("Expected an UnsupportedOperationException to be thrown but it wasn't.");
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testFrom() {
    assertEquals(
        ImmutableList.of(1, 2, 3, 4),
        Lists.newArrayList(FluentIterable.from(ImmutableList.of(1, 2, 3, 4))));
  }

  @SuppressWarnings("deprecation") // test of deprecated method
  public void testFrom_alreadyFluentIterable() {
    FluentIterable<Integer> iterable = FluentIterable.from(asList(1));
    assertSame(iterable, FluentIterable.from(iterable));
  }

  public void testOf() {
    assertEquals(ImmutableList.of(1, 2, 3, 4), Lists.newArrayList(FluentIterable.of(1, 2, 3, 4)));
  }

  public void testFromArray() {
    assertEquals(
        ImmutableList.of("1", "2", "3", "4"),
        Lists.newArrayList(FluentIterable.from(new Object[] {"1", "2", "3", "4"})));
  }

  public void testOf_empty() {
    assertEquals(ImmutableList.of(), Lists.newArrayList(FluentIterable.of()));
  }

  // Exhaustive tests are in IteratorsTest. These are copied from IterablesTest.
  public void testConcatIterable() {
    List<Integer> list1 = newArrayList(1);
    List<Integer> list2 = newArrayList(4);

    @SuppressWarnings("unchecked")
    List<List<Integer>> input = newArrayList(list1, list2);

    FluentIterable<Integer> result = FluentIterable.concat(input);
    assertEquals(asList(1, 4), newArrayList(result));

    // Now change the inputs and see result dynamically change as well

    list1.add(2);
    List<Integer> list3 = newArrayList(3);
    input.add(1, list3);

    assertEquals(asList(1, 2, 3, 4), newArrayList(result));
    assertEquals("[1, 2, 3, 4]", result.toString());
  }

  public void testConcatVarargs() {
    List<Integer> list1 = newArrayList(1);
    List<Integer> list2 = newArrayList(4);
    List<Integer> list3 = newArrayList(7, 8);
    List<Integer> list4 = newArrayList(9);
    List<Integer> list5 = newArrayList(10);
    @SuppressWarnings("unchecked")
    FluentIterable<Integer> result = FluentIterable.concat(list1, list2, list3, list4, list5);
    assertEquals(asList(1, 4, 7, 8, 9, 10), newArrayList(result));
    assertEquals("[1, 4, 7, 8, 9, 10]", result.toString());
  }

  public void testConcatNullPointerException() {
    List<Integer> list1 = newArrayList(1);
    List<Integer> list2 = newArrayList(4);

    try {
      FluentIterable.concat(list1, null, list2);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testConcatPeformingFiniteCycle() {
    Iterable<Integer> iterable = asList(1, 2, 3);
    int n = 4;
    FluentIterable<Integer> repeated = FluentIterable.concat(Collections.nCopies(n, iterable));
    assertThat(repeated).containsExactly(1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3).inOrder();
  }

  interface X {}

  interface Y {}

  static class A implements X, Y {}

  static class B implements X, Y {}

  /**
   * This test passes if the {@code concat(…).filter(…).filter(…)} statement at the end compiles.
   * That statement compiles only if {@link FluentIterable#concat concat(aIterable, bIterable)}
   * returns a {@link FluentIterable} of elements of an anonymous type whose supertypes are the <a
   * href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.9">intersection</a> of
   * the supertypes of {@code A} and the supertypes of {@code B}.
   */
  public void testConcatIntersectionType() {
    Iterable<A> aIterable = ImmutableList.of();
    Iterable<B> bIterable = ImmutableList.of();

    Predicate<X> xPredicate = Predicates.alwaysTrue();
    Predicate<Y> yPredicate = Predicates.alwaysTrue();

    FluentIterable<?> unused =
        FluentIterable.concat(aIterable, bIterable).filter(xPredicate).filter(yPredicate);

    /* The following fails to compile:
     *
     * The method append(Iterable<? extends FluentIterableTest.A>) in the type
     * FluentIterable<FluentIterableTest.A> is not applicable for the arguments
     * (Iterable<FluentIterableTest.B>)
     */
    // FluentIterable.from(aIterable).append(bIterable);

    /* The following fails to compile:
     *
     * The method filter(Predicate<? super Object>) in the type FluentIterable<Object> is not
     * applicable for the arguments (Predicate<FluentIterableTest.X>)
     */
    // FluentIterable.of().append(aIterable).append(bIterable).filter(xPredicate);
  }

  public void testSize0() {
    assertEquals(0, FluentIterable.<String>of().size());
  }

  public void testSize1Collection() {
    assertEquals(1, FluentIterable.from(asList("a")).size());
  }

  public void testSize2NonCollection() {
    Iterable<Integer> iterable =
        new Iterable<Integer>() {
          @Override
          public Iterator<Integer> iterator() {
            return asList(0, 1).iterator();
          }
        };
    assertEquals(2, FluentIterable.from(iterable).size());
  }

  public void testSize_collectionDoesntIterate() {
    List<Integer> nums = asList(1, 2, 3, 4, 5);
    List<Integer> collection =
        new ArrayList<Integer>(nums) {
          @Override
          public Iterator<Integer> iterator() {
            throw new AssertionFailedError("Don't iterate me!");
          }
        };
    assertEquals(5, FluentIterable.from(collection).size());
  }

  public void testContains_nullSetYes() {
    Iterable<String> set = Sets.newHashSet("a", null, "b");
    assertTrue(FluentIterable.from(set).contains(null));
  }

  public void testContains_nullSetNo() {
    Iterable<String> set = ImmutableSortedSet.of("a", "b");
    assertFalse(FluentIterable.from(set).contains(null));
  }

  public void testContains_nullIterableYes() {
    Iterable<String> iterable = iterable("a", null, "b");
    assertTrue(FluentIterable.from(iterable).contains(null));
  }

  public void testContains_nullIterableNo() {
    Iterable<String> iterable = iterable("a", "b");
    assertFalse(FluentIterable.from(iterable).contains(null));
  }

  public void testContains_nonNullSetYes() {
    Iterable<String> set = Sets.newHashSet("a", null, "b");
    assertTrue(FluentIterable.from(set).contains("b"));
  }

  public void testContains_nonNullSetNo() {
    Iterable<String> set = Sets.newHashSet("a", "b");
    assertFalse(FluentIterable.from(set).contains("c"));
  }

  public void testContains_nonNullIterableYes() {
    Iterable<String> set = iterable("a", null, "b");
    assertTrue(FluentIterable.from(set).contains("b"));
  }

  public void testContains_nonNullIterableNo() {
    Iterable<String> iterable = iterable("a", "b");
    assertFalse(FluentIterable.from(iterable).contains("c"));
  }

  public void testOfToString() {
    assertEquals("[yam, bam, jam, ham]", FluentIterable.of("yam", "bam", "jam", "ham").toString());
  }

  public void testToString() {
    assertEquals("[]", FluentIterable.from(Collections.emptyList()).toString());
    assertEquals("[]", FluentIterable.<String>of().toString());

    assertEquals(
        "[yam, bam, jam, ham]", FluentIterable.from(asList("yam", "bam", "jam", "ham")).toString());
  }

  public void testCycle() {
    FluentIterable<String> cycle = FluentIterable.from(asList("a", "b")).cycle();

    int howManyChecked = 0;
    for (String string : cycle) {
      String expected = (howManyChecked % 2 == 0) ? "a" : "b";
      assertEquals(expected, string);
      if (howManyChecked++ == 5) {
        break;
      }
    }

    // We left the last iterator pointing to "b". But a new iterator should
    // always point to "a".
    assertEquals("a", cycle.iterator().next());
  }

  public void testCycle_emptyIterable() {
    FluentIterable<Integer> cycle = FluentIterable.<Integer>of().cycle();
    assertFalse(cycle.iterator().hasNext());
  }

  public void testCycle_removingAllElementsStopsCycle() {
    FluentIterable<Integer> cycle = fluent(1, 2).cycle();
    Iterator<Integer> iterator = cycle.iterator();
    iterator.next();
    iterator.remove();
    iterator.next();
    iterator.remove();
    assertFalse(iterator.hasNext());
    assertFalse(cycle.iterator().hasNext());
  }

  public void testAppend() {
    FluentIterable<Integer> result =
        FluentIterable.<Integer>from(asList(1, 2, 3)).append(Lists.newArrayList(4, 5, 6));
    assertEquals(asList(1, 2, 3, 4, 5, 6), Lists.newArrayList(result));
    assertEquals("[1, 2, 3, 4, 5, 6]", result.toString());

    result = FluentIterable.<Integer>from(asList(1, 2, 3)).append(4, 5, 6);
    assertEquals(asList(1, 2, 3, 4, 5, 6), Lists.newArrayList(result));
    assertEquals("[1, 2, 3, 4, 5, 6]", result.toString());
  }

  public void testAppend_toEmpty() {
    FluentIterable<Integer> result =
        FluentIterable.<Integer>of().append(Lists.newArrayList(1, 2, 3));
    assertEquals(asList(1, 2, 3), Lists.newArrayList(result));
  }

  public void testAppend_emptyList() {
    FluentIterable<Integer> result =
        FluentIterable.<Integer>from(asList(1, 2, 3)).append(Lists.<Integer>newArrayList());
    assertEquals(asList(1, 2, 3), Lists.newArrayList(result));
  }

  public void testAppend_nullPointerException() {
    try {
      FluentIterable<Integer> unused =
          FluentIterable.<Integer>from(asList(1, 2)).append((List<Integer>) null);
      fail("Appending null iterable should throw NPE.");
    } catch (NullPointerException expected) {
    }
  }

  /*
   * Tests for partition(int size) method.
   */

  /*
   * Tests for partitionWithPadding(int size) method.
   */

  public void testFilter() {
    FluentIterable<String> filtered =
        FluentIterable.from(asList("foo", "bar")).filter(Predicates.equalTo("foo"));

    List<String> expected = Collections.singletonList("foo");
    List<String> actual = Lists.newArrayList(filtered);
    assertEquals(expected, actual);
    assertCanIterateAgain(filtered);
    assertEquals("[foo]", filtered.toString());
  }

  private static class TypeA {}

  private interface TypeB {}

  private static class HasBoth extends TypeA implements TypeB {}

  @GwtIncompatible // Iterables.filter(Iterable, Class)
  public void testFilterByType() throws Exception {
    HasBoth hasBoth = new HasBoth();
    FluentIterable<TypeA> alist =
        FluentIterable.from(asList(new TypeA(), new TypeA(), hasBoth, new TypeA()));
    Iterable<TypeB> blist = alist.filter(TypeB.class);
    assertThat(blist).containsExactly(hasBoth).inOrder();
  }

  public void testAnyMatch() {
    ArrayList<String> list = Lists.newArrayList();
    FluentIterable<String> iterable = FluentIterable.<String>from(list);
    Predicate<String> predicate = Predicates.equalTo("pants");

    assertFalse(iterable.anyMatch(predicate));
    list.add("cool");
    assertFalse(iterable.anyMatch(predicate));
    list.add("pants");
    assertTrue(iterable.anyMatch(predicate));
  }

  public void testAllMatch() {
    List<String> list = Lists.newArrayList();
    FluentIterable<String> iterable = FluentIterable.<String>from(list);
    Predicate<String> predicate = Predicates.equalTo("cool");

    assertTrue(iterable.allMatch(predicate));
    list.add("cool");
    assertTrue(iterable.allMatch(predicate));
    list.add("pants");
    assertFalse(iterable.allMatch(predicate));
  }

  public void testFirstMatch() {
    FluentIterable<String> iterable = FluentIterable.from(Lists.newArrayList("cool", "pants"));
    assertThat(iterable.firstMatch(Predicates.equalTo("cool"))).hasValue("cool");
    assertThat(iterable.firstMatch(Predicates.equalTo("pants"))).hasValue("pants");
    assertThat(iterable.firstMatch(Predicates.alwaysFalse())).isAbsent();
    assertThat(iterable.firstMatch(Predicates.alwaysTrue())).hasValue("cool");
  }

  private static final class IntegerValueOfFunction implements Function<String, Integer> {
    @Override
    public Integer apply(String from) {
      return Integer.valueOf(from);
    }
  }

  public void testTransformWith() {
    List<String> input = asList("1", "2", "3");
    Iterable<Integer> iterable = FluentIterable.from(input).transform(new IntegerValueOfFunction());

    assertEquals(asList(1, 2, 3), Lists.newArrayList(iterable));
    assertCanIterateAgain(iterable);
    assertEquals("[1, 2, 3]", iterable.toString());
  }

  public void testTransformWith_poorlyBehavedTransform() {
    List<String> input = asList("1", null, "3");
    Iterable<Integer> iterable = FluentIterable.from(input).transform(new IntegerValueOfFunction());

    Iterator<Integer> resultIterator = iterable.iterator();
    resultIterator.next();

    try {
      resultIterator.next();
      fail("Transforming null to int should throw NumberFormatException");
    } catch (NumberFormatException expected) {
    }
  }

  private static final class StringValueOfFunction implements Function<Integer, String> {
    @Override
    public String apply(Integer from) {
      return String.valueOf(from);
    }
  }

  public void testTransformWith_nullFriendlyTransform() {
    List<Integer> input = asList(1, 2, null, 3);
    Iterable<String> result = FluentIterable.from(input).transform(new StringValueOfFunction());

    assertEquals(asList("1", "2", "null", "3"), Lists.newArrayList(result));
  }

  private static final class RepeatedStringValueOfFunction
      implements Function<Integer, List<String>> {
    @Override
    public List<String> apply(Integer from) {
      String value = String.valueOf(from);
      return ImmutableList.of(value, value);
    }
  }

  public void testTransformAndConcat() {
    List<Integer> input = asList(1, 2, 3);
    Iterable<String> result =
        FluentIterable.from(input).transformAndConcat(new RepeatedStringValueOfFunction());
    assertEquals(asList("1", "1", "2", "2", "3", "3"), Lists.newArrayList(result));
  }

  private static final class RepeatedStringValueOfWildcardFunction
      implements Function<Integer, List<? extends String>> {
    @Override
    public List<String> apply(Integer from) {
      String value = String.valueOf(from);
      return ImmutableList.of(value, value);
    }
  }

  public void testTransformAndConcat_wildcardFunctionGenerics() {
    List<Integer> input = asList(1, 2, 3);
    FluentIterable<String> unused =
        FluentIterable.from(input).transformAndConcat(new RepeatedStringValueOfWildcardFunction());
  }

  public void testFirst_list() {
    List<String> list = Lists.newArrayList("a", "b", "c");
    assertThat(FluentIterable.from(list).first()).hasValue("a");
  }

  public void testFirst_null() {
    List<String> list = Lists.newArrayList(null, "a", "b");
    try {
      FluentIterable.from(list).first();
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testFirst_emptyList() {
    List<String> list = Collections.emptyList();
    assertThat(FluentIterable.from(list).first()).isAbsent();
  }

  public void testFirst_sortedSet() {
    SortedSet<String> sortedSet = ImmutableSortedSet.of("b", "c", "a");
    assertThat(FluentIterable.from(sortedSet).first()).hasValue("a");
  }

  public void testFirst_emptySortedSet() {
    SortedSet<String> sortedSet = ImmutableSortedSet.of();
    assertThat(FluentIterable.from(sortedSet).first()).isAbsent();
  }

  public void testFirst_iterable() {
    Set<String> set = ImmutableSet.of("a", "b", "c");
    assertThat(FluentIterable.from(set).first()).hasValue("a");
  }

  public void testFirst_emptyIterable() {
    Set<String> set = Sets.newHashSet();
    assertThat(FluentIterable.from(set).first()).isAbsent();
  }

  public void testLast_list() {
    List<String> list = Lists.newArrayList("a", "b", "c");
    assertThat(FluentIterable.from(list).last()).hasValue("c");
  }

  public void testLast_null() {
    List<String> list = Lists.newArrayList("a", "b", null);
    try {
      FluentIterable.from(list).last();
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testLast_emptyList() {
    List<String> list = Collections.emptyList();
    assertThat(FluentIterable.from(list).last()).isAbsent();
  }

  public void testLast_sortedSet() {
    SortedSet<String> sortedSet = ImmutableSortedSet.of("b", "c", "a");
    assertThat(FluentIterable.from(sortedSet).last()).hasValue("c");
  }

  public void testLast_emptySortedSet() {
    SortedSet<String> sortedSet = ImmutableSortedSet.of();
    assertThat(FluentIterable.from(sortedSet).last()).isAbsent();
  }

  public void testLast_iterable() {
    Set<String> set = ImmutableSet.of("a", "b", "c");
    assertThat(FluentIterable.from(set).last()).hasValue("c");
  }

  public void testLast_emptyIterable() {
    Set<String> set = Sets.newHashSet();
    assertThat(FluentIterable.from(set).last()).isAbsent();
  }

  public void testSkip_simple() {
    Collection<String> set = ImmutableSet.of("a", "b", "c", "d", "e");
    assertEquals(
        Lists.newArrayList("c", "d", "e"), Lists.newArrayList(FluentIterable.from(set).skip(2)));
    assertEquals("[c, d, e]", FluentIterable.from(set).skip(2).toString());
  }

  public void testSkip_simpleList() {
    Collection<String> list = Lists.newArrayList("a", "b", "c", "d", "e");
    assertEquals(
        Lists.newArrayList("c", "d", "e"), Lists.newArrayList(FluentIterable.from(list).skip(2)));
    assertEquals("[c, d, e]", FluentIterable.from(list).skip(2).toString());
  }

  public void testSkip_pastEnd() {
    Collection<String> set = ImmutableSet.of("a", "b");
    assertEquals(Collections.emptyList(), Lists.newArrayList(FluentIterable.from(set).skip(20)));
  }

  public void testSkip_pastEndList() {
    Collection<String> list = Lists.newArrayList("a", "b");
    assertEquals(Collections.emptyList(), Lists.newArrayList(FluentIterable.from(list).skip(20)));
  }

  public void testSkip_skipNone() {
    Collection<String> set = ImmutableSet.of("a", "b");
    assertEquals(
        Lists.newArrayList("a", "b"), Lists.newArrayList(FluentIterable.from(set).skip(0)));
  }

  public void testSkip_skipNoneList() {
    Collection<String> list = Lists.newArrayList("a", "b");
    assertEquals(
        Lists.newArrayList("a", "b"), Lists.newArrayList(FluentIterable.from(list).skip(0)));
  }

  public void testSkip_iterator() throws Exception {
    new IteratorTester<Integer>(
        5,
        IteratorFeature.MODIFIABLE,
        Lists.newArrayList(2, 3),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        Collection<Integer> collection = Sets.newLinkedHashSet();
        Collections.addAll(collection, 1, 2, 3);
        return FluentIterable.from(collection).skip(1).iterator();
      }
    }.test();
  }

  public void testSkip_iteratorList() throws Exception {
    new IteratorTester<Integer>(
        5,
        IteratorFeature.MODIFIABLE,
        Lists.newArrayList(2, 3),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return FluentIterable.from(Lists.newArrayList(1, 2, 3)).skip(1).iterator();
      }
    }.test();
  }

  public void testSkip_nonStructurallyModifiedList() throws Exception {
    List<String> list = Lists.newArrayList("a", "b", "c");
    FluentIterable<String> tail = FluentIterable.from(list).skip(1);
    Iterator<String> tailIterator = tail.iterator();
    list.set(2, "c2");
    assertEquals("b", tailIterator.next());
    assertEquals("c2", tailIterator.next());
    assertFalse(tailIterator.hasNext());
  }

  public void testSkip_structurallyModifiedSkipSome() throws Exception {
    Collection<String> set = Sets.newLinkedHashSet();
    Collections.addAll(set, "a", "b", "c");
    FluentIterable<String> tail = FluentIterable.from(set).skip(1);
    set.remove("b");
    set.addAll(Lists.newArrayList("X", "Y", "Z"));
    assertThat(tail).containsExactly("c", "X", "Y", "Z").inOrder();
  }

  public void testSkip_structurallyModifiedSkipSomeList() throws Exception {
    List<String> list = Lists.newArrayList("a", "b", "c");
    FluentIterable<String> tail = FluentIterable.from(list).skip(1);
    list.subList(1, 3).clear();
    list.addAll(0, Lists.newArrayList("X", "Y", "Z"));
    assertThat(tail).containsExactly("Y", "Z", "a").inOrder();
  }

  public void testSkip_structurallyModifiedSkipAll() throws Exception {
    Collection<String> set = Sets.newLinkedHashSet();
    Collections.addAll(set, "a", "b", "c");
    FluentIterable<String> tail = FluentIterable.from(set).skip(2);
    set.remove("a");
    set.remove("b");
    assertFalse(tail.iterator().hasNext());
  }

  public void testSkip_structurallyModifiedSkipAllList() throws Exception {
    List<String> list = Lists.newArrayList("a", "b", "c");
    FluentIterable<String> tail = FluentIterable.from(list).skip(2);
    list.subList(0, 2).clear();
    assertThat(tail).isEmpty();
  }

  public void testSkip_illegalArgument() {
    try {
      FluentIterable.from(asList("a", "b", "c")).skip(-1);
      fail("Skipping negative number of elements should throw IllegalArgumentException.");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testLimit() {
    Iterable<String> iterable = Lists.newArrayList("foo", "bar", "baz");
    FluentIterable<String> limited = FluentIterable.from(iterable).limit(2);

    assertEquals(ImmutableList.of("foo", "bar"), Lists.newArrayList(limited));
    assertCanIterateAgain(limited);
    assertEquals("[foo, bar]", limited.toString());
  }

  public void testLimit_illegalArgument() {
    try {
      FluentIterable<String> unused =
          FluentIterable.from(Lists.newArrayList("a", "b", "c")).limit(-1);
      fail("Passing negative number to limit(...) method should throw IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testIsEmpty() {
    assertTrue(FluentIterable.<String>from(Collections.<String>emptyList()).isEmpty());
    assertFalse(FluentIterable.<String>from(Lists.newArrayList("foo")).isEmpty());
  }

  public void testToList() {
    assertEquals(Lists.newArrayList(1, 2, 3, 4), fluent(1, 2, 3, 4).toList());
  }

  public void testToList_empty() {
    assertTrue(fluent().toList().isEmpty());
  }

  public void testToSortedList_withComparator() {
    assertEquals(
        Lists.newArrayList(4, 3, 2, 1),
        fluent(4, 1, 3, 2).toSortedList(Ordering.<Integer>natural().reverse()));
  }

  public void testToSortedList_withDuplicates() {
    assertEquals(
        Lists.newArrayList(4, 3, 1, 1),
        fluent(1, 4, 1, 3).toSortedList(Ordering.<Integer>natural().reverse()));
  }

  public void testToSet() {
    assertThat(fluent(1, 2, 3, 4).toSet()).containsExactly(1, 2, 3, 4).inOrder();
  }

  public void testToSet_removeDuplicates() {
    assertThat(fluent(1, 2, 1, 2).toSet()).containsExactly(1, 2).inOrder();
  }

  public void testToSet_empty() {
    assertTrue(fluent().toSet().isEmpty());
  }

  public void testToSortedSet() {
    assertThat(fluent(1, 4, 2, 3).toSortedSet(Ordering.<Integer>natural().reverse()))
        .containsExactly(4, 3, 2, 1)
        .inOrder();
  }

  public void testToSortedSet_removeDuplicates() {
    assertThat(fluent(1, 4, 1, 3).toSortedSet(Ordering.<Integer>natural().reverse()))
        .containsExactly(4, 3, 1)
        .inOrder();
  }

  public void testToMultiset() {
    assertThat(fluent(1, 2, 1, 3, 2, 4).toMultiset()).containsExactly(1, 1, 2, 2, 3, 4).inOrder();
  }

  public void testToMultiset_empty() {
    assertThat(fluent().toMultiset()).isEmpty();
  }

  public void testToMap() {
    assertThat(fluent(1, 2, 3).toMap(Functions.toStringFunction()).entrySet())
        .containsExactly(
            Maps.immutableEntry(1, "1"), Maps.immutableEntry(2, "2"), Maps.immutableEntry(3, "3"))
        .inOrder();
  }

  public void testToMap_nullKey() {
    try {
      fluent(1, null, 2).toMap(Functions.constant("foo"));
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testToMap_nullValue() {
    try {
      fluent(1, 2, 3).toMap(Functions.constant(null));
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testIndex() {
    ImmutableListMultimap<Integer, String> expected =
        ImmutableListMultimap.<Integer, String>builder()
            .putAll(3, "one", "two")
            .put(5, "three")
            .put(4, "four")
            .build();
    ImmutableListMultimap<Integer, String> index =
        FluentIterable.from(asList("one", "two", "three", "four"))
            .index(
                new Function<String, Integer>() {
                  @Override
                  public Integer apply(String input) {
                    return input.length();
                  }
                });
    assertEquals(expected, index);
  }

  public void testIndex_nullKey() {
    try {
      ImmutableListMultimap<Object, Integer> unused =
          fluent(1, 2, 3).index(Functions.constant(null));
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testIndex_nullValue() {
    try {
      ImmutableListMultimap<String, Integer> unused =
          fluent(1, null, 2).index(Functions.constant("foo"));
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testUniqueIndex() {
    ImmutableMap<Integer, String> expected = ImmutableMap.of(3, "two", 5, "three", 4, "four");
    ImmutableMap<Integer, String> index =
        FluentIterable.from(asList("two", "three", "four"))
            .uniqueIndex(
                new Function<String, Integer>() {
                  @Override
                  public Integer apply(String input) {
                    return input.length();
                  }
                });
    assertEquals(expected, index);
  }

  public void testUniqueIndex_duplicateKey() {
    try {
      ImmutableMap<Integer, String> unused =
          FluentIterable.from(asList("one", "two", "three", "four"))
              .uniqueIndex(
                  new Function<String, Integer>() {
                    @Override
                    public Integer apply(String input) {
                      return input.length();
                    }
                  });
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testUniqueIndex_nullKey() {
    try {
      fluent(1, 2, 3).uniqueIndex(Functions.constant(null));
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testUniqueIndex_nullValue() {
    try {
      ImmutableMap<Object, Integer> unused =
          fluent(1, null, 2)
              .uniqueIndex(
                  new Function<Integer, Object>() {
                    @Override
                    public Object apply(@NullableDecl Integer input) {
                      return String.valueOf(input);
                    }
                  });
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testCopyInto_List() {
    assertThat(fluent(1, 3, 5).copyInto(Lists.newArrayList(1, 2)))
        .containsExactly(1, 2, 1, 3, 5)
        .inOrder();
  }

  public void testCopyInto_Set() {
    assertThat(fluent(1, 3, 5).copyInto(Sets.newHashSet(1, 2))).containsExactly(1, 2, 3, 5);
  }

  public void testCopyInto_SetAllDuplicates() {
    assertThat(fluent(1, 3, 5).copyInto(Sets.newHashSet(1, 2, 3, 5))).containsExactly(1, 2, 3, 5);
  }

  public void testCopyInto_NonCollection() {
    final ArrayList<Integer> list = Lists.newArrayList(1, 2, 3);

    final ArrayList<Integer> iterList = Lists.newArrayList(9, 8, 7);
    Iterable<Integer> iterable =
        new Iterable<Integer>() {
          @Override
          public Iterator<Integer> iterator() {
            return iterList.iterator();
          }
        };

    assertThat(FluentIterable.from(iterable).copyInto(list))
        .containsExactly(1, 2, 3, 9, 8, 7)
        .inOrder();
  }

  public void testJoin() {
    assertEquals("2,1,3,4", fluent(2, 1, 3, 4).join(Joiner.on(",")));
  }

  public void testJoin_empty() {
    assertEquals("", fluent().join(Joiner.on(",")));
  }

  public void testGet() {
    assertEquals("a", FluentIterable.from(Lists.newArrayList("a", "b", "c")).get(0));
    assertEquals("b", FluentIterable.from(Lists.newArrayList("a", "b", "c")).get(1));
    assertEquals("c", FluentIterable.from(Lists.newArrayList("a", "b", "c")).get(2));
  }

  public void testGet_outOfBounds() {
    try {
      FluentIterable.from(Lists.newArrayList("a", "b", "c")).get(-1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      FluentIterable.from(Lists.newArrayList("a", "b", "c")).get(3);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  private static void assertCanIterateAgain(Iterable<?> iterable) {
    for (Object unused : iterable) {
      // do nothing
    }
  }

  private static FluentIterable<Integer> fluent(Integer... elements) {
    return FluentIterable.from(Lists.newArrayList(elements));
  }

  private static Iterable<String> iterable(String... elements) {
    final List<String> list = asList(elements);
    return new Iterable<String>() {
      @Override
      public Iterator<String> iterator() {
        return list.iterator();
      }
    };
  }
}
