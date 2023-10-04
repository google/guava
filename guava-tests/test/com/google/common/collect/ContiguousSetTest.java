/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.collect.BoundType.CLOSED;
import static com.google.common.collect.BoundType.OPEN;
import static com.google.common.collect.DiscreteDomain.integers;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_QUERIES;
import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionFeature.NON_STANDARD_TOSTRING;
import static com.google.common.collect.testing.features.CollectionFeature.RESTRICTS_ELEMENTS;
import static com.google.common.collect.testing.testers.NavigableSetNavigationTester.getHoleMethods;
import static com.google.common.testing.SerializableTester.reserialize;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.NavigableSetTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.SetGenerators.ContiguousSetDescendingGenerator;
import com.google.common.collect.testing.google.SetGenerators.ContiguousSetGenerator;
import com.google.common.collect.testing.google.SetGenerators.ContiguousSetHeadsetGenerator;
import com.google.common.collect.testing.google.SetGenerators.ContiguousSetSubsetGenerator;
import com.google.common.collect.testing.google.SetGenerators.ContiguousSetTailsetGenerator;
import com.google.common.testing.EqualsTester;
import java.util.Collection;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Gregory Kick
 */
@GwtCompatible(emulated = true)
public class ContiguousSetTest extends TestCase {
  private static final DiscreteDomain<Integer> NOT_EQUAL_TO_INTEGERS =
      new DiscreteDomain<Integer>() {
        @Override
        public Integer next(Integer value) {
          return integers().next(value);
        }

        @Override
        public Integer previous(Integer value) {
          return integers().previous(value);
        }

        @Override
        public long distance(Integer start, Integer end) {
          return integers().distance(start, end);
        }

        @Override
        public Integer minValue() {
          return integers().minValue();
        }

        @Override
        public Integer maxValue() {
          return integers().maxValue();
        }
      };

  public void testInvalidIntRange() {
    try {
      ContiguousSet.closed(2, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      ContiguousSet.closedOpen(2, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testInvalidLongRange() {
    try {
      ContiguousSet.closed(2L, 1L);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      ContiguousSet.closedOpen(2L, 1L);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            ContiguousSet.create(Range.closed(1, 3), integers()),
            ContiguousSet.closed(1, 3),
            ContiguousSet.create(Range.closedOpen(1, 4), integers()),
            ContiguousSet.closedOpen(1, 4),
            ContiguousSet.create(Range.openClosed(0, 3), integers()),
            ContiguousSet.create(Range.open(0, 4), integers()),
            ContiguousSet.create(Range.closed(1, 3), NOT_EQUAL_TO_INTEGERS),
            ContiguousSet.create(Range.closedOpen(1, 4), NOT_EQUAL_TO_INTEGERS),
            ContiguousSet.create(Range.openClosed(0, 3), NOT_EQUAL_TO_INTEGERS),
            ContiguousSet.create(Range.open(0, 4), NOT_EQUAL_TO_INTEGERS),
            ImmutableSortedSet.of(1, 2, 3))
        .addEqualityGroup(
            ContiguousSet.create(Range.closedOpen(1, 1), integers()),
            ContiguousSet.closedOpen(1, 1),
            ContiguousSet.closedOpen(Integer.MIN_VALUE, Integer.MIN_VALUE),
            ImmutableSortedSet.of(),
            ImmutableSet.of())
        .testEquals();
    // not testing hashCode for these because it takes forever to compute
    assertEquals(
        ContiguousSet.closed(Integer.MIN_VALUE, Integer.MAX_VALUE),
        ContiguousSet.create(Range.<Integer>all(), integers()));
    assertEquals(
        ContiguousSet.closed(Integer.MIN_VALUE, Integer.MAX_VALUE),
        ContiguousSet.create(Range.atLeast(Integer.MIN_VALUE), integers()));
    assertEquals(
        ContiguousSet.closed(Integer.MIN_VALUE, Integer.MAX_VALUE),
        ContiguousSet.create(Range.atMost(Integer.MAX_VALUE), integers()));
  }

  @GwtIncompatible // SerializableTester
  public void testSerialization() {
    ContiguousSet<Integer> empty = ContiguousSet.create(Range.closedOpen(1, 1), integers());
    assertTrue(empty instanceof EmptyContiguousSet);
    reserializeAndAssert(empty);

    ContiguousSet<Integer> regular = ContiguousSet.create(Range.closed(1, 3), integers());
    assertTrue(regular instanceof RegularContiguousSet);
    reserializeAndAssert(regular);

    /*
     * Make sure that we're using RegularContiguousSet.SerializedForm and not
     * ImmutableSet.SerializedForm, which would be enormous.
     */
    ContiguousSet<Integer> enormous = ContiguousSet.create(Range.<Integer>all(), integers());
    assertTrue(enormous instanceof RegularContiguousSet);
    // We can't use reserializeAndAssert because it calls hashCode, which is enormously slow.
    ContiguousSet<Integer> enormousReserialized = reserialize(enormous);
    assertEquals(enormous, enormousReserialized);
  }

  private static final DiscreteDomain<Integer> UNBOUNDED_THROWING_DOMAIN =
      new DiscreteDomain<Integer>() {
        @Override
        public Integer next(Integer value) {
          throw new AssertionError();
        }

        @Override
        public Integer previous(Integer value) {
          throw new AssertionError();
        }

        @Override
        public long distance(Integer start, Integer end) {
          throw new AssertionError();
        }
      };

  public void testCreate_noMin() {
    Range<Integer> range = Range.lessThan(0);
    try {
      ContiguousSet.create(range, UNBOUNDED_THROWING_DOMAIN);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreate_noMax() {
    Range<Integer> range = Range.greaterThan(0);
    try {
      ContiguousSet.create(range, UNBOUNDED_THROWING_DOMAIN);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreate_empty() {
    assertEquals(ImmutableSet.of(), ContiguousSet.create(Range.closedOpen(1, 1), integers()));
    assertEquals(ImmutableSet.of(), ContiguousSet.closedOpen(1, 1));
    assertEquals(ImmutableSet.of(), ContiguousSet.create(Range.openClosed(5, 5), integers()));
    assertEquals(
        ImmutableSet.of(), ContiguousSet.create(Range.lessThan(Integer.MIN_VALUE), integers()));
    assertEquals(
        ImmutableSet.of(), ContiguousSet.create(Range.greaterThan(Integer.MAX_VALUE), integers()));
  }

  public void testHeadSet() {
    ImmutableSortedSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    assertThat(set.headSet(1)).isEmpty();
    assertThat(set.headSet(2)).containsExactly(1).inOrder();
    assertThat(set.headSet(3)).containsExactly(1, 2).inOrder();
    assertThat(set.headSet(4)).containsExactly(1, 2, 3).inOrder();
    assertThat(set.headSet(Integer.MAX_VALUE)).containsExactly(1, 2, 3).inOrder();
    assertThat(set.headSet(1, true)).containsExactly(1).inOrder();
    assertThat(set.headSet(2, true)).containsExactly(1, 2).inOrder();
    assertThat(set.headSet(3, true)).containsExactly(1, 2, 3).inOrder();
    assertThat(set.headSet(4, true)).containsExactly(1, 2, 3).inOrder();
    assertThat(set.headSet(Integer.MAX_VALUE, true)).containsExactly(1, 2, 3).inOrder();
  }

  public void testHeadSet_tooSmall() {
    assertThat(ContiguousSet.create(Range.closed(1, 3), integers()).headSet(0)).isEmpty();
  }

  public void testTailSet() {
    ImmutableSortedSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    assertThat(set.tailSet(Integer.MIN_VALUE)).containsExactly(1, 2, 3).inOrder();
    assertThat(set.tailSet(1)).containsExactly(1, 2, 3).inOrder();
    assertThat(set.tailSet(2)).containsExactly(2, 3).inOrder();
    assertThat(set.tailSet(3)).containsExactly(3).inOrder();
    assertThat(set.tailSet(Integer.MIN_VALUE, false)).containsExactly(1, 2, 3).inOrder();
    assertThat(set.tailSet(1, false)).containsExactly(2, 3).inOrder();
    assertThat(set.tailSet(2, false)).containsExactly(3).inOrder();
    assertThat(set.tailSet(3, false)).isEmpty();
  }

  public void testTailSet_tooLarge() {
    assertThat(ContiguousSet.create(Range.closed(1, 3), integers()).tailSet(4)).isEmpty();
  }

  public void testSubSet() {
    ImmutableSortedSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    assertThat(set.subSet(1, 4)).containsExactly(1, 2, 3).inOrder();
    assertThat(set.subSet(2, 4)).containsExactly(2, 3).inOrder();
    assertThat(set.subSet(3, 4)).containsExactly(3).inOrder();
    assertThat(set.subSet(3, 3)).isEmpty();
    assertThat(set.subSet(2, 3)).containsExactly(2).inOrder();
    assertThat(set.subSet(1, 3)).containsExactly(1, 2).inOrder();
    assertThat(set.subSet(1, 2)).containsExactly(1).inOrder();
    assertThat(set.subSet(2, 2)).isEmpty();
    assertThat(set.subSet(Integer.MIN_VALUE, Integer.MAX_VALUE)).containsExactly(1, 2, 3).inOrder();
    assertThat(set.subSet(1, true, 3, true)).containsExactly(1, 2, 3).inOrder();
    assertThat(set.subSet(1, false, 3, true)).containsExactly(2, 3).inOrder();
    assertThat(set.subSet(1, true, 3, false)).containsExactly(1, 2).inOrder();
    assertThat(set.subSet(1, false, 3, false)).containsExactly(2).inOrder();
  }

  public void testSubSet_outOfOrder() {
    ImmutableSortedSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    try {
      set.subSet(3, 2);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testSubSet_tooLarge() {
    assertThat(ContiguousSet.create(Range.closed(1, 3), integers()).subSet(4, 6)).isEmpty();
  }

  public void testSubSet_tooSmall() {
    assertThat(ContiguousSet.create(Range.closed(1, 3), integers()).subSet(-1, 0)).isEmpty();
  }

  public void testFirst() {
    assertEquals(1, ContiguousSet.create(Range.closed(1, 3), integers()).first().intValue());
    assertEquals(1, ContiguousSet.create(Range.open(0, 4), integers()).first().intValue());
    assertEquals(
        Integer.MIN_VALUE,
        ContiguousSet.create(Range.<Integer>all(), integers()).first().intValue());
  }

  public void testLast() {
    assertEquals(3, ContiguousSet.create(Range.closed(1, 3), integers()).last().intValue());
    assertEquals(3, ContiguousSet.create(Range.open(0, 4), integers()).last().intValue());
    assertEquals(
        Integer.MAX_VALUE,
        ContiguousSet.create(Range.<Integer>all(), integers()).last().intValue());
  }

  public void testContains() {
    ImmutableSortedSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    assertFalse(set.contains(0));
    assertTrue(set.contains(1));
    assertTrue(set.contains(2));
    assertTrue(set.contains(3));
    assertFalse(set.contains(4));
    set = ContiguousSet.create(Range.open(0, 4), integers());
    assertFalse(set.contains(0));
    assertTrue(set.contains(1));
    assertTrue(set.contains(2));
    assertTrue(set.contains(3));
    assertFalse(set.contains(4));
    assertFalse(set.contains((Object) "blah"));
  }

  public void testContainsAll() {
    ImmutableSortedSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    for (Set<Integer> subset : Sets.powerSet(ImmutableSet.of(1, 2, 3))) {
      assertTrue(set.containsAll(subset));
    }
    for (Set<Integer> subset : Sets.powerSet(ImmutableSet.of(1, 2, 3))) {
      assertFalse(set.containsAll(Sets.union(subset, ImmutableSet.of(9))));
    }
    assertFalse(set.containsAll((Collection<?>) ImmutableSet.of("blah")));
  }

  public void testRange() {
    assertEquals(Range.closed(1, 3), ContiguousSet.create(Range.closed(1, 3), integers()).range());
    assertEquals(Range.closed(1, 3), ContiguousSet.closed(1, 3).range());
    assertEquals(
        Range.closed(1, 3), ContiguousSet.create(Range.closedOpen(1, 4), integers()).range());
    assertEquals(Range.closed(1, 3), ContiguousSet.closedOpen(1, 4).range());
    assertEquals(Range.closed(1, 3), ContiguousSet.create(Range.open(0, 4), integers()).range());
    assertEquals(
        Range.closed(1, 3), ContiguousSet.create(Range.openClosed(0, 3), integers()).range());

    assertEquals(
        Range.openClosed(0, 3),
        ContiguousSet.create(Range.closed(1, 3), integers()).range(OPEN, CLOSED));
    assertEquals(
        Range.openClosed(0, 3),
        ContiguousSet.create(Range.closedOpen(1, 4), integers()).range(OPEN, CLOSED));
    assertEquals(
        Range.openClosed(0, 3),
        ContiguousSet.create(Range.open(0, 4), integers()).range(OPEN, CLOSED));
    assertEquals(
        Range.openClosed(0, 3),
        ContiguousSet.create(Range.openClosed(0, 3), integers()).range(OPEN, CLOSED));

    assertEquals(
        Range.open(0, 4), ContiguousSet.create(Range.closed(1, 3), integers()).range(OPEN, OPEN));
    assertEquals(
        Range.open(0, 4),
        ContiguousSet.create(Range.closedOpen(1, 4), integers()).range(OPEN, OPEN));
    assertEquals(
        Range.open(0, 4), ContiguousSet.create(Range.open(0, 4), integers()).range(OPEN, OPEN));
    assertEquals(
        Range.open(0, 4),
        ContiguousSet.create(Range.openClosed(0, 3), integers()).range(OPEN, OPEN));

    assertEquals(
        Range.closedOpen(1, 4),
        ContiguousSet.create(Range.closed(1, 3), integers()).range(CLOSED, OPEN));
    assertEquals(
        Range.closedOpen(1, 4),
        ContiguousSet.create(Range.closedOpen(1, 4), integers()).range(CLOSED, OPEN));
    assertEquals(
        Range.closedOpen(1, 4),
        ContiguousSet.create(Range.open(0, 4), integers()).range(CLOSED, OPEN));
    assertEquals(
        Range.closedOpen(1, 4),
        ContiguousSet.create(Range.openClosed(0, 3), integers()).range(CLOSED, OPEN));
  }

  public void testRange_unboundedRange() {
    assertEquals(
        Range.closed(Integer.MIN_VALUE, Integer.MAX_VALUE),
        ContiguousSet.create(Range.<Integer>all(), integers()).range());
    assertEquals(
        Range.atLeast(Integer.MIN_VALUE),
        ContiguousSet.create(Range.<Integer>all(), integers()).range(CLOSED, OPEN));
    assertEquals(
        Range.all(), ContiguousSet.create(Range.<Integer>all(), integers()).range(OPEN, OPEN));
    assertEquals(
        Range.atMost(Integer.MAX_VALUE),
        ContiguousSet.create(Range.<Integer>all(), integers()).range(OPEN, CLOSED));
  }

  public void testIntersection_empty() {
    ContiguousSet<Integer> set = ContiguousSet.closed(1, 3);
    ContiguousSet<Integer> emptySet = ContiguousSet.closedOpen(2, 2);
    assertEquals(ImmutableSet.of(), set.intersection(emptySet));
    assertEquals(ImmutableSet.of(), emptySet.intersection(set));
    assertEquals(
        ImmutableSet.of(),
        ContiguousSet.create(Range.closed(-5, -1), integers())
            .intersection(ContiguousSet.create(Range.open(3, 64), integers())));
  }

  public void testIntersection() {
    ContiguousSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    assertEquals(
        ImmutableSet.of(1, 2, 3),
        ContiguousSet.create(Range.open(-1, 4), integers()).intersection(set));
    assertEquals(
        ImmutableSet.of(1, 2, 3),
        set.intersection(ContiguousSet.create(Range.open(-1, 4), integers())));
    assertEquals(
        ImmutableSet.of(3), set.intersection(ContiguousSet.create(Range.closed(3, 5), integers())));
  }

  public void testAsList() {
    ImmutableList<Integer> list = ContiguousSet.create(Range.closed(1, 3), integers()).asList();
    for (int i = 0; i < 3; i++) {
      assertEquals(i + 1, list.get(i).intValue());
    }
    assertEquals(ImmutableList.of(1, 2, 3), ImmutableList.copyOf(list.iterator()));
    assertEquals(ImmutableList.of(1, 2, 3), ImmutableList.copyOf(list.toArray(new Integer[0])));
  }

  @GwtIncompatible // suite
  public static class BuiltTests extends TestCase {
    public static Test suite() {
      TestSuite suite = new TestSuite();

      suite.addTest(
          NavigableSetTestSuiteBuilder.using(new ContiguousSetGenerator())
              .named("Range.asSet")
              .withFeatures(
                  CollectionSize.ANY,
                  KNOWN_ORDER,
                  ALLOWS_NULL_QUERIES,
                  NON_STANDARD_TOSTRING,
                  RESTRICTS_ELEMENTS)
              .suppressing(getHoleMethods())
              .createTestSuite());

      suite.addTest(
          NavigableSetTestSuiteBuilder.using(new ContiguousSetHeadsetGenerator())
              .named("Range.asSet, headset")
              .withFeatures(
                  CollectionSize.ANY,
                  KNOWN_ORDER,
                  ALLOWS_NULL_QUERIES,
                  NON_STANDARD_TOSTRING,
                  RESTRICTS_ELEMENTS)
              .suppressing(getHoleMethods())
              .createTestSuite());

      suite.addTest(
          NavigableSetTestSuiteBuilder.using(new ContiguousSetTailsetGenerator())
              .named("Range.asSet, tailset")
              .withFeatures(
                  CollectionSize.ANY,
                  KNOWN_ORDER,
                  ALLOWS_NULL_QUERIES,
                  NON_STANDARD_TOSTRING,
                  RESTRICTS_ELEMENTS)
              .suppressing(getHoleMethods())
              .createTestSuite());

      suite.addTest(
          NavigableSetTestSuiteBuilder.using(new ContiguousSetSubsetGenerator())
              .named("Range.asSet, subset")
              .withFeatures(
                  CollectionSize.ANY,
                  KNOWN_ORDER,
                  ALLOWS_NULL_QUERIES,
                  NON_STANDARD_TOSTRING,
                  RESTRICTS_ELEMENTS)
              .suppressing(getHoleMethods())
              .createTestSuite());

      suite.addTest(
          NavigableSetTestSuiteBuilder.using(new ContiguousSetDescendingGenerator())
              .named("Range.asSet.descendingSet")
              .withFeatures(
                  CollectionSize.ANY,
                  KNOWN_ORDER,
                  ALLOWS_NULL_QUERIES,
                  NON_STANDARD_TOSTRING,
                  RESTRICTS_ELEMENTS)
              .suppressing(getHoleMethods())
              .createTestSuite());

      return suite;
    }
  }
}
