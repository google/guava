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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.nCopies;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Predicate;
import com.google.common.collect.testing.CollectionTestSuiteBuilder;
import com.google.common.collect.testing.TestStringCollectionGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.NullPointerTester;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@link Collections2}.
 *
 * @author Chris Povirk
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class Collections2Test extends TestCase {
  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite(Collections2Test.class.getSimpleName());
    suite.addTest(testsForFilter());
    suite.addTest(testsForFilterAll());
    suite.addTest(testsForFilterLinkedList());
    suite.addTest(testsForFilterNoNulls());
    suite.addTest(testsForFilterFiltered());
    suite.addTest(testsForTransform());
    suite.addTestSuite(Collections2Test.class);
    return suite;
  }

  static final Predicate<@Nullable String> NOT_YYY_ZZZ =
      input -> !"yyy".equals(input) && !"zzz".equals(input);

  static final Predicate<String> LENGTH_1 = input -> input.length() == 1;

  @GwtIncompatible // suite
  private static Test testsForFilter() {
    return CollectionTestSuiteBuilder.using(
            new TestStringCollectionGenerator() {
              @Override
              public Collection<String> create(String[] elements) {
                List<String> unfiltered = newArrayList();
                unfiltered.add("yyy");
                Collections.addAll(unfiltered, elements);
                unfiltered.add("zzz");
                return Collections2.filter(unfiltered, NOT_YYY_ZZZ);
              }
            })
        .named("Collections2.filter")
        .withFeatures(
            CollectionFeature.SUPPORTS_ADD,
            CollectionFeature.SUPPORTS_REMOVE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .createTestSuite();
  }

  @GwtIncompatible // suite
  private static Test testsForFilterAll() {
    return CollectionTestSuiteBuilder.using(
            new TestStringCollectionGenerator() {
              @Override
              public Collection<String> create(String[] elements) {
                List<String> unfiltered = newArrayList();
                Collections.addAll(unfiltered, elements);
                return Collections2.filter(unfiltered, NOT_YYY_ZZZ);
              }
            })
        .named("Collections2.filter")
        .withFeatures(
            CollectionFeature.SUPPORTS_ADD,
            CollectionFeature.SUPPORTS_REMOVE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .createTestSuite();
  }

  @GwtIncompatible // suite
  private static Test testsForFilterLinkedList() {
    return CollectionTestSuiteBuilder.using(
            new TestStringCollectionGenerator() {
              @Override
              public Collection<String> create(String[] elements) {
                List<String> unfiltered = newLinkedList();
                unfiltered.add("yyy");
                Collections.addAll(unfiltered, elements);
                unfiltered.add("zzz");
                return Collections2.filter(unfiltered, NOT_YYY_ZZZ);
              }
            })
        .named("Collections2.filter")
        .withFeatures(
            CollectionFeature.SUPPORTS_ADD,
            CollectionFeature.SUPPORTS_REMOVE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .createTestSuite();
  }

  @GwtIncompatible // suite
  private static Test testsForFilterNoNulls() {
    return CollectionTestSuiteBuilder.using(
            new TestStringCollectionGenerator() {
              @Override
              public Collection<String> create(String[] elements) {
                List<String> unfiltered = newArrayList();
                unfiltered.add("yyy");
                unfiltered.addAll(ImmutableList.copyOf(elements));
                unfiltered.add("zzz");
                return Collections2.filter(unfiltered, LENGTH_1);
              }
            })
        .named("Collections2.filter, no nulls")
        .withFeatures(
            CollectionFeature.SUPPORTS_ADD,
            CollectionFeature.SUPPORTS_REMOVE,
            CollectionFeature.ALLOWS_NULL_QUERIES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .createTestSuite();
  }

  @GwtIncompatible // suite
  private static Test testsForFilterFiltered() {
    return CollectionTestSuiteBuilder.using(
            new TestStringCollectionGenerator() {
              @Override
              public Collection<String> create(String[] elements) {
                List<String> unfiltered = newArrayList();
                unfiltered.add("yyy");
                unfiltered.addAll(ImmutableList.copyOf(elements));
                unfiltered.add("zzz");
                unfiltered.add("abc");
                return Collections2.filter(Collections2.filter(unfiltered, LENGTH_1), NOT_YYY_ZZZ);
              }
            })
        .named("Collections2.filter, filtered input")
        .withFeatures(
            CollectionFeature.SUPPORTS_ADD,
            CollectionFeature.SUPPORTS_REMOVE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES,
            CollectionSize.ANY)
        .createTestSuite();
  }

  @GwtIncompatible // suite
  private static Test testsForTransform() {
    return CollectionTestSuiteBuilder.using(
            new TestStringCollectionGenerator() {
              @Override
              public Collection<@Nullable String> create(@Nullable String[] elements) {
                List<@Nullable String> list = newArrayList();
                for (String element : elements) {
                  list.add((element == null) ? null : "q" + element);
                }
                return Collections2.transform(
                    list, from -> isNullOrEmpty(from) ? null : from.substring(1));
              }
            })
        .named("Collections2.transform")
        .withFeatures(
            CollectionFeature.REMOVE_OPERATIONS,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .createTestSuite();
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Collections2.class);
  }

  public void testOrderedPermutationSetEmpty() {
    List<Integer> list = newArrayList();
    Collection<List<Integer>> permutationSet = Collections2.orderedPermutations(list);

    assertEquals(1, permutationSet.size());
    assertThat(permutationSet).contains(list);

    Iterator<List<Integer>> permutations = permutationSet.iterator();

    assertNextPermutation(Lists.<Integer>newArrayList(), permutations);
    assertNoMorePermutations(permutations);
  }

  public void testOrderedPermutationSetOneElement() {
    List<Integer> list = newArrayList(1);
    Iterator<List<Integer>> permutations = Collections2.orderedPermutations(list).iterator();

    assertNextPermutation(newArrayList(1), permutations);
    assertNoMorePermutations(permutations);
  }

  public void testOrderedPermutationSetThreeElements() {
    List<String> list = newArrayList("b", "a", "c");
    Iterator<List<String>> permutations = Collections2.orderedPermutations(list).iterator();

    assertNextPermutation(newArrayList("a", "b", "c"), permutations);
    assertNextPermutation(newArrayList("a", "c", "b"), permutations);
    assertNextPermutation(newArrayList("b", "a", "c"), permutations);
    assertNextPermutation(newArrayList("b", "c", "a"), permutations);
    assertNextPermutation(newArrayList("c", "a", "b"), permutations);
    assertNextPermutation(newArrayList("c", "b", "a"), permutations);
    assertNoMorePermutations(permutations);
  }

  public void testOrderedPermutationSetRepeatedElements() {
    List<Integer> list = newArrayList(1, 1, 2, 2);
    Iterator<List<Integer>> permutations =
        Collections2.orderedPermutations(list, Ordering.natural()).iterator();

    assertNextPermutation(newArrayList(1, 1, 2, 2), permutations);
    assertNextPermutation(newArrayList(1, 2, 1, 2), permutations);
    assertNextPermutation(newArrayList(1, 2, 2, 1), permutations);
    assertNextPermutation(newArrayList(2, 1, 1, 2), permutations);
    assertNextPermutation(newArrayList(2, 1, 2, 1), permutations);
    assertNextPermutation(newArrayList(2, 2, 1, 1), permutations);
    assertNoMorePermutations(permutations);
  }

  public void testOrderedPermutationSetRepeatedElementsSize() {
    List<Integer> list = newArrayList(1, 1, 1, 1, 2, 2, 3);
    Collection<List<Integer>> permutations =
        Collections2.orderedPermutations(list, Ordering.natural());

    assertPermutationsCount(105, permutations);
  }

  public void testOrderedPermutationSetSizeOverflow() {
    // 12 elements won't overflow
    assertEquals(
        479001600 /*12!*/,
        Collections2.orderedPermutations(newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
            .size());
    // 13 elements overflow an int
    assertEquals(
        Integer.MAX_VALUE,
        Collections2.orderedPermutations(newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13))
            .size());
    // 21 elements overflow a long
    assertEquals(
        Integer.MAX_VALUE,
        Collections2.orderedPermutations(
                newArrayList(
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21))
            .size());

    // Almost force an overflow in the binomial coefficient calculation
    assertEquals(
        1391975640 /*C(34,14)*/,
        Collections2.orderedPermutations(concat(nCopies(20, 1), nCopies(14, 2))).size());
    // Do force an overflow in the binomial coefficient calculation
    assertEquals(
        Integer.MAX_VALUE,
        Collections2.orderedPermutations(concat(nCopies(21, 1), nCopies(14, 2))).size());
  }

  public void testOrderedPermutationSetContains() {
    List<Integer> list = newArrayList(3, 2, 1);
    Collection<List<Integer>> permutationSet = Collections2.orderedPermutations(list);

    assertTrue(permutationSet.contains(newArrayList(1, 2, 3)));
    assertTrue(permutationSet.contains(newArrayList(2, 3, 1)));
    assertFalse(permutationSet.contains(newArrayList(1, 2)));
    assertFalse(permutationSet.contains(newArrayList(1, 1, 2, 3)));
    assertFalse(permutationSet.contains(newArrayList(1, 2, 3, 4)));
    assertFalse(permutationSet.contains(null));
  }

  public void testPermutationSetEmpty() {
    Collection<List<Integer>> permutationSet =
        Collections2.permutations(Collections.<Integer>emptyList());

    assertEquals(1, permutationSet.size());
    assertTrue(permutationSet.contains(Collections.<Integer>emptyList()));

    Iterator<List<Integer>> permutations = permutationSet.iterator();
    assertNextPermutation(Collections.<Integer>emptyList(), permutations);
    assertNoMorePermutations(permutations);
  }

  public void testPermutationSetOneElement() {
    Iterator<List<Integer>> permutations =
        Collections2.permutations(Collections.<Integer>singletonList(1)).iterator();
    assertNextPermutation(newArrayList(1), permutations);
    assertNoMorePermutations(permutations);
  }

  public void testPermutationSetTwoElements() {
    Iterator<List<Integer>> permutations = Collections2.permutations(newArrayList(1, 2)).iterator();
    assertNextPermutation(newArrayList(1, 2), permutations);
    assertNextPermutation(newArrayList(2, 1), permutations);
    assertNoMorePermutations(permutations);
  }

  public void testPermutationSetThreeElements() {
    Iterator<List<Integer>> permutations =
        Collections2.permutations(newArrayList(1, 2, 3)).iterator();
    assertNextPermutation(newArrayList(1, 2, 3), permutations);
    assertNextPermutation(newArrayList(1, 3, 2), permutations);
    assertNextPermutation(newArrayList(3, 1, 2), permutations);

    assertNextPermutation(newArrayList(3, 2, 1), permutations);
    assertNextPermutation(newArrayList(2, 3, 1), permutations);
    assertNextPermutation(newArrayList(2, 1, 3), permutations);
    assertNoMorePermutations(permutations);
  }

  public void testPermutationSetThreeElementsOutOfOrder() {
    Iterator<List<Integer>> permutations =
        Collections2.permutations(newArrayList(3, 2, 1)).iterator();
    assertNextPermutation(newArrayList(3, 2, 1), permutations);
    assertNextPermutation(newArrayList(3, 1, 2), permutations);
    assertNextPermutation(newArrayList(1, 3, 2), permutations);

    assertNextPermutation(newArrayList(1, 2, 3), permutations);
    assertNextPermutation(newArrayList(2, 1, 3), permutations);
    assertNextPermutation(newArrayList(2, 3, 1), permutations);
    assertNoMorePermutations(permutations);
  }

  public void testPermutationSetThreeRepeatedElements() {
    Iterator<List<Integer>> permutations =
        Collections2.permutations(newArrayList(1, 1, 2)).iterator();
    assertNextPermutation(newArrayList(1, 1, 2), permutations);
    assertNextPermutation(newArrayList(1, 2, 1), permutations);
    assertNextPermutation(newArrayList(2, 1, 1), permutations);
    assertNextPermutation(newArrayList(2, 1, 1), permutations);
    assertNextPermutation(newArrayList(1, 2, 1), permutations);
    assertNextPermutation(newArrayList(1, 1, 2), permutations);
    assertNoMorePermutations(permutations);
  }

  public void testPermutationSetFourElements() {
    Iterator<List<Integer>> permutations =
        Collections2.permutations(newArrayList(1, 2, 3, 4)).iterator();
    assertNextPermutation(newArrayList(1, 2, 3, 4), permutations);
    assertNextPermutation(newArrayList(1, 2, 4, 3), permutations);
    assertNextPermutation(newArrayList(1, 4, 2, 3), permutations);
    assertNextPermutation(newArrayList(4, 1, 2, 3), permutations);

    assertNextPermutation(newArrayList(4, 1, 3, 2), permutations);
    assertNextPermutation(newArrayList(1, 4, 3, 2), permutations);
    assertNextPermutation(newArrayList(1, 3, 4, 2), permutations);
    assertNextPermutation(newArrayList(1, 3, 2, 4), permutations);

    assertNextPermutation(newArrayList(3, 1, 2, 4), permutations);
    assertNextPermutation(newArrayList(3, 1, 4, 2), permutations);
    assertNextPermutation(newArrayList(3, 4, 1, 2), permutations);
    assertNextPermutation(newArrayList(4, 3, 1, 2), permutations);

    assertNextPermutation(newArrayList(4, 3, 2, 1), permutations);
    assertNextPermutation(newArrayList(3, 4, 2, 1), permutations);
    assertNextPermutation(newArrayList(3, 2, 4, 1), permutations);
    assertNextPermutation(newArrayList(3, 2, 1, 4), permutations);

    assertNextPermutation(newArrayList(2, 3, 1, 4), permutations);
    assertNextPermutation(newArrayList(2, 3, 4, 1), permutations);
    assertNextPermutation(newArrayList(2, 4, 3, 1), permutations);
    assertNextPermutation(newArrayList(4, 2, 3, 1), permutations);

    assertNextPermutation(newArrayList(4, 2, 1, 3), permutations);
    assertNextPermutation(newArrayList(2, 4, 1, 3), permutations);
    assertNextPermutation(newArrayList(2, 1, 4, 3), permutations);
    assertNextPermutation(newArrayList(2, 1, 3, 4), permutations);
    assertNoMorePermutations(permutations);
  }

  public void testPermutationSetSize() {
    assertPermutationsCount(1, Collections2.permutations(Collections.<Integer>emptyList()));
    assertPermutationsCount(1, Collections2.permutations(newArrayList(1)));
    assertPermutationsCount(2, Collections2.permutations(newArrayList(1, 2)));
    assertPermutationsCount(6, Collections2.permutations(newArrayList(1, 2, 3)));
    assertPermutationsCount(5040, Collections2.permutations(newArrayList(1, 2, 3, 4, 5, 6, 7)));
    assertPermutationsCount(40320, Collections2.permutations(newArrayList(1, 2, 3, 4, 5, 6, 7, 8)));
  }

  public void testPermutationSetSizeOverflow() {
    // 13 elements overflow an int
    assertEquals(
        Integer.MAX_VALUE,
        Collections2.permutations(newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)).size());
    // 21 elements overflow a long
    assertEquals(
        Integer.MAX_VALUE,
        Collections2.orderedPermutations(
                newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20))
            .size());
    assertEquals(
        Integer.MAX_VALUE,
        Collections2.orderedPermutations(
                newArrayList(
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21))
            .size());
  }

  public void testPermutationSetContains() {
    List<Integer> list = newArrayList(3, 2, 1);
    Collection<List<Integer>> permutationSet = Collections2.permutations(list);

    assertTrue(permutationSet.contains(newArrayList(1, 2, 3)));
    assertTrue(permutationSet.contains(newArrayList(2, 3, 1)));
    assertFalse(permutationSet.contains(newArrayList(1, 2)));
    assertFalse(permutationSet.contains(newArrayList(1, 1, 2, 3)));
    assertFalse(permutationSet.contains(newArrayList(1, 2, 3, 4)));
    assertFalse(permutationSet.contains(null));
  }

  private <T> void assertNextPermutation(
      List<T> expectedPermutation, Iterator<List<T>> permutations) {
    assertTrue("Expected another permutation, but there was none.", permutations.hasNext());
    assertEquals(expectedPermutation, permutations.next());
  }

  private <T> void assertNoMorePermutations(Iterator<List<T>> permutations) {
    assertFalse("Expected no more permutations, but there was one.", permutations.hasNext());
    try {
      permutations.next();
      fail("Expected NoSuchElementException.");
    } catch (NoSuchElementException expected) {
    }
  }

  private <T> void assertPermutationsCount(int expected, Collection<List<T>> permutationSet) {
    assertEquals(expected, permutationSet.size());
    Iterator<List<T>> permutations = permutationSet.iterator();
    for (int i = 0; i < expected; i++) {
      assertTrue(permutations.hasNext());
      permutations.next();
    }
    assertNoMorePermutations(permutations);
  }

  public void testToStringImplWithNullEntries() throws Exception {
    List<String> list = Lists.newArrayList();
    list.add("foo");
    list.add(null);

    assertEquals(list.toString(), Collections2.toStringImpl(list));
  }
}
