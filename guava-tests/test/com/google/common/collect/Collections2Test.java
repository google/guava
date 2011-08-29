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
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.testing.testers.CollectionIteratorTester.getIteratorKnownOrderRemoveSupportedMethod;
import static java.util.Arrays.asList;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.testing.CollectionTestSuiteBuilder;
import com.google.common.collect.testing.TestStringCollectionGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.NullPointerTester;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link Collections2}.
 *
 * @author Chris Povirk
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class Collections2Test extends TestCase {
  @GwtIncompatible("suite")
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

  static final Predicate<String> NOT_YYY_ZZZ = new Predicate<String>() {
      @Override
      public boolean apply(String input) {
        return !"yyy".equals(input) && !"zzz".equals(input);
      }
  };

  static final Predicate<String> LENGTH_1 = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      return input.length() == 1;
    }
  };

  static final Predicate<String> STARTS_WITH_VOWEL = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      return asList('a', 'e', 'i', 'o', 'u').contains(input.charAt(0));
    }
  };

  @GwtIncompatible("suite")
  private static Test testsForFilter() {
    return CollectionTestSuiteBuilder.using(
        new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            List<String> unfiltered = newArrayList();
            unfiltered.add("yyy");
            unfiltered.addAll(asList(elements));
            unfiltered.add("zzz");
            return Collections2.filter(unfiltered, NOT_YYY_ZZZ);
          }
        })
        .named("Collections2.filter")
        .withFeatures(
            CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .suppressing(getIteratorKnownOrderRemoveSupportedMethod())
        .createTestSuite();
  }

  @GwtIncompatible("suite")
  private static Test testsForFilterAll() {
    return CollectionTestSuiteBuilder.using(
        new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            List<String> unfiltered = newArrayList();
            unfiltered.addAll(asList(elements));
            return Collections2.filter(unfiltered, NOT_YYY_ZZZ);
          }
        })
        .named("Collections2.filter")
        .withFeatures(
            CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .suppressing(getIteratorKnownOrderRemoveSupportedMethod())
        .createTestSuite();
  }

  @GwtIncompatible("suite")
  private static Test testsForFilterLinkedList() {
    return CollectionTestSuiteBuilder.using(
        new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            List<String> unfiltered = newLinkedList();
            unfiltered.add("yyy");
            unfiltered.addAll(asList(elements));
            unfiltered.add("zzz");
            return Collections2.filter(unfiltered, NOT_YYY_ZZZ);
          }
        })
        .named("Collections2.filter")
        .withFeatures(
            CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .suppressing(getIteratorKnownOrderRemoveSupportedMethod())
        .createTestSuite();
  }

  @GwtIncompatible("suite")
  private static Test testsForFilterNoNulls() {
    return CollectionTestSuiteBuilder.using(
        new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            List<String> unfiltered = newArrayList();
            unfiltered.add("yyy");
            unfiltered.addAll(ImmutableList.copyOf(elements));
            unfiltered.add("zzz");
            return Collections2.filter(unfiltered, LENGTH_1);
          }
        })
        .named("Collections2.filter, no nulls")
        .withFeatures(
            CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_QUERIES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .suppressing(getIteratorKnownOrderRemoveSupportedMethod())
        .createTestSuite();
  }

  @GwtIncompatible("suite")
  private static Test testsForFilterFiltered() {
    return CollectionTestSuiteBuilder.using(
        new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            List<String> unfiltered = newArrayList();
            unfiltered.add("yyy");
            unfiltered.addAll(ImmutableList.copyOf(elements));
            unfiltered.add("zzz");
            unfiltered.add("abc");
            return Collections2.filter(
                Collections2.filter(unfiltered, LENGTH_1), NOT_YYY_ZZZ);
          }
        })
        .named("Collections2.filter, filtered input")
        .withFeatures(
            CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES,
            CollectionSize.ANY)
        .suppressing(getIteratorKnownOrderRemoveSupportedMethod())
        .createTestSuite();
  }

  public abstract static class FilterChangeTest extends TestCase {
    protected abstract <E> List<E> newList();

    public void testFilterIllegalAdd() {
      List<String> unfiltered = newList();
      Collection<String> filtered
          = Collections2.filter(unfiltered, NOT_YYY_ZZZ);
      filtered.add("a");
      filtered.add("b");
      ASSERT.that(filtered).hasContentsInOrder("a", "b");

      try {
        filtered.add("yyy");
        fail();
      } catch (IllegalArgumentException expected) {}

      try {
        filtered.addAll(asList("c", "zzz", "d"));
        fail();
      } catch (IllegalArgumentException expected) {}

      ASSERT.that(filtered).hasContentsInOrder("a", "b");
    }

    public void testFilterChangeUnfiltered() {
      List<String> unfiltered = newList();
      Collection<String> filtered
          = Collections2.filter(unfiltered, NOT_YYY_ZZZ);

      unfiltered.add("a");
      unfiltered.add("yyy");
      unfiltered.add("b");
      ASSERT.that(unfiltered).hasContentsInOrder("a", "yyy", "b");
      ASSERT.that(filtered).hasContentsInOrder("a", "b");

      unfiltered.remove("a");
      ASSERT.that(unfiltered).hasContentsInOrder("yyy", "b");
      ASSERT.that(filtered).hasContentsInOrder("b");

      unfiltered.clear();
      ASSERT.that(unfiltered).isEmpty();
      ASSERT.that(filtered).isEmpty();

      unfiltered.add("yyy");
      ASSERT.that(unfiltered).hasContentsInOrder("yyy");
      ASSERT.that(filtered).isEmpty();
      filtered.clear();
      ASSERT.that(unfiltered).hasContentsInOrder("yyy");
      ASSERT.that(filtered).isEmpty();

      unfiltered.clear();
      filtered.clear();
      ASSERT.that(unfiltered).isEmpty();
      ASSERT.that(filtered).isEmpty();

      unfiltered.add("a");
      ASSERT.that(unfiltered).hasContentsInOrder("a");
      ASSERT.that(filtered).hasContentsInOrder("a");
      filtered.clear();
      ASSERT.that(unfiltered).isEmpty();
      ASSERT.that(filtered).isEmpty();

      unfiltered.clear();
      Collections.addAll(unfiltered,
          "a", "b", "yyy", "zzz", "c", "d", "yyy", "zzz");
      ASSERT.that(unfiltered).hasContentsInOrder(
          "a", "b", "yyy", "zzz", "c", "d", "yyy", "zzz");
      ASSERT.that(filtered).hasContentsInOrder("a", "b", "c", "d");
      filtered.clear();
      ASSERT.that(unfiltered).hasContentsInOrder("yyy", "zzz", "yyy", "zzz");
      ASSERT.that(filtered).isEmpty();
    }

    public void testFilterChangeFiltered() {
      List<String> unfiltered = newList();
      Collection<String> filtered
          = Collections2.filter(unfiltered, NOT_YYY_ZZZ);

      unfiltered.add("a");
      unfiltered.add("yyy");
      filtered.add("b");
      ASSERT.that(unfiltered).hasContentsInOrder("a", "yyy", "b");
      ASSERT.that(filtered).hasContentsInOrder("a", "b");

      filtered.remove("a");
      ASSERT.that(unfiltered).hasContentsInOrder("yyy", "b");
      ASSERT.that(filtered).hasContentsInOrder("b");

      filtered.clear();
      ASSERT.that(unfiltered).hasContentsInOrder("yyy");
      ASSERT.that(filtered);
    }

    public void testFilterFiltered() {
      List<String> unfiltered = newList();
      Collection<String> filtered = Collections2.filter(
          Collections2.filter(unfiltered, LENGTH_1), STARTS_WITH_VOWEL);
      unfiltered.add("a");
      unfiltered.add("b");
      unfiltered.add("apple");
      unfiltered.add("banana");
      unfiltered.add("e");
      ASSERT.that(filtered).hasContentsInOrder("a", "e");
      ASSERT.that(unfiltered).hasContentsInOrder("a", "b", "apple", "banana", "e");

      try {
        filtered.add("d");
        fail();
      } catch (IllegalArgumentException expected) {}
      try {
        filtered.add("egg");
        fail();
      } catch (IllegalArgumentException expected) {}
      ASSERT.that(filtered).hasContentsInOrder("a", "e");
      ASSERT.that(unfiltered).hasContentsInOrder("a", "b", "apple", "banana", "e");

      filtered.clear();
      ASSERT.that(filtered).isEmpty();
      ASSERT.that(unfiltered).hasContentsInOrder("b", "apple", "banana");
    }
  }

  public static class ArrayListFilterChangeTest extends FilterChangeTest {
    @Override protected <E> List<E> newList() {
      return Lists.newArrayList();
    }
  }

  public static class LinkedListFilterChangeTest extends FilterChangeTest {
    @Override protected <E> List<E> newList() {
      return Lists.newLinkedList();
    }
  }

  private static final Function<String, String> REMOVE_FIRST_CHAR
      = new Function<String, String>() {
        @Override
        public String apply(String from) {
          return ((from == null) || "".equals(from))
              ? null : from.substring(1);
        }
      };

  @GwtIncompatible("suite")
  private static Test testsForTransform() {
    return CollectionTestSuiteBuilder.using(
        new TestStringCollectionGenerator() {
          @Override public Collection<String> create(String[] elements) {
            List<String> list = newArrayList();
            for (String element : elements) {
              list.add((element == null) ? null : "q" + element);
            }
            return Collections2.transform(list, REMOVE_FIRST_CHAR);
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

  @GwtIncompatible("NullPointerTester")
  public void testNullPointerExceptions() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Collections2.class);
  }
}
