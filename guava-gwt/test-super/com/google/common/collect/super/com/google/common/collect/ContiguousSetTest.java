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
import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.testing.EqualsTester;

import junit.framework.TestCase;

import java.util.Set;

/**
 * @author Gregory Kick
 */
@GwtCompatible(emulated = true)
public class ContiguousSetTest extends TestCase {
  private static DiscreteDomain<Integer> NOT_EQUAL_TO_INTEGERS = new DiscreteDomain<Integer>() {
    @Override public Integer next(Integer value) {
      return integers().next(value);
    }

    @Override public Integer previous(Integer value) {
      return integers().previous(value);
    }

    @Override public long distance(Integer start, Integer end) {
      return integers().distance(start, end);
    }

    @Override public Integer minValue() {
      return integers().minValue();
    }

    @Override public Integer maxValue() {
      return integers().maxValue();
    }
  };

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            ContiguousSet.create(Range.closed(1, 3), integers()),
            ContiguousSet.create(Range.closedOpen(1, 4), integers()),
            ContiguousSet.create(Range.openClosed(0, 3), integers()),
            ContiguousSet.create(Range.open(0, 4), integers()),
            ContiguousSet.create(Range.closed(1, 3), NOT_EQUAL_TO_INTEGERS),
            ContiguousSet.create(Range.closedOpen(1, 4), NOT_EQUAL_TO_INTEGERS),
            ContiguousSet.create(Range.openClosed(0, 3), NOT_EQUAL_TO_INTEGERS),
            ContiguousSet.create(Range.open(0, 4), NOT_EQUAL_TO_INTEGERS),
            ImmutableSortedSet.of(1, 2, 3))
        .testEquals();
    // not testing hashCode for these because it takes forever to compute
    assertEquals(
        ContiguousSet.create(Range.closed(Integer.MIN_VALUE, Integer.MAX_VALUE), integers()),
        ContiguousSet.create(Range.<Integer>all(), integers()));
    assertEquals(
        ContiguousSet.create(Range.closed(Integer.MIN_VALUE, Integer.MAX_VALUE), integers()),
        ContiguousSet.create(Range.atLeast(Integer.MIN_VALUE), integers()));
    assertEquals(
        ContiguousSet.create(Range.closed(Integer.MIN_VALUE, Integer.MAX_VALUE), integers()),
        ContiguousSet.create(Range.atMost(Integer.MAX_VALUE), integers()));
  }

  public void testCreate_noMin() {
    Range<Integer> range = Range.lessThan(0);
    try {
      ContiguousSet.create(range, RangeTest.UNBOUNDED_DOMAIN);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testCreate_noMax() {
    Range<Integer> range = Range.greaterThan(0);
    try {
      ContiguousSet.create(range, RangeTest.UNBOUNDED_DOMAIN);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testCreate_empty() {
    assertEquals(ImmutableSet.of(), ContiguousSet.create(Range.closedOpen(1, 1), integers()));
    assertEquals(ImmutableSet.of(), ContiguousSet.create(Range.openClosed(5, 5), integers()));
    assertEquals(ImmutableSet.of(),
        ContiguousSet.create(Range.lessThan(Integer.MIN_VALUE), integers()));
    assertEquals(ImmutableSet.of(),
        ContiguousSet.create(Range.greaterThan(Integer.MAX_VALUE), integers()));
  }

  public void testHeadSet() {
    ImmutableSortedSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    ASSERT.that(set.headSet(1)).isEmpty();
    ASSERT.that(set.headSet(2)).has().item(1);
    ASSERT.that(set.headSet(3)).has().exactly(1, 2).inOrder();
    ASSERT.that(set.headSet(4)).has().exactly(1, 2, 3).inOrder();
    ASSERT.that(set.headSet(Integer.MAX_VALUE)).has().exactly(1, 2, 3).inOrder();
    ASSERT.that(set.headSet(1, true)).has().item(1);
    ASSERT.that(set.headSet(2, true)).has().exactly(1, 2).inOrder();
    ASSERT.that(set.headSet(3, true)).has().exactly(1, 2, 3).inOrder();
    ASSERT.that(set.headSet(4, true)).has().exactly(1, 2, 3).inOrder();
    ASSERT.that(set.headSet(Integer.MAX_VALUE, true)).has().exactly(1, 2, 3).inOrder();
  }

  public void testHeadSet_tooSmall() {
    ASSERT.that(ContiguousSet.create(Range.closed(1, 3), integers()).headSet(0)).isEmpty();
  }

  public void testTailSet() {
    ImmutableSortedSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    ASSERT.that(set.tailSet(Integer.MIN_VALUE)).has().exactly(1, 2, 3).inOrder();
    ASSERT.that(set.tailSet(1)).has().exactly(1, 2, 3).inOrder();
    ASSERT.that(set.tailSet(2)).has().exactly(2, 3).inOrder();
    ASSERT.that(set.tailSet(3)).has().item(3);
    ASSERT.that(set.tailSet(Integer.MIN_VALUE, false)).has().exactly(1, 2, 3).inOrder();
    ASSERT.that(set.tailSet(1, false)).has().exactly(2, 3).inOrder();
    ASSERT.that(set.tailSet(2, false)).has().item(3);
    ASSERT.that(set.tailSet(3, false)).isEmpty();
  }

  public void testTailSet_tooLarge() {
    ASSERT.that(ContiguousSet.create(Range.closed(1, 3), integers()).tailSet(4)).isEmpty();
  }

  public void testSubSet() {
    ImmutableSortedSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    ASSERT.that(set.subSet(1, 4)).has().exactly(1, 2, 3).inOrder();
    ASSERT.that(set.subSet(2, 4)).has().exactly(2, 3).inOrder();
    ASSERT.that(set.subSet(3, 4)).has().item(3);
    ASSERT.that(set.subSet(3, 3)).isEmpty();
    ASSERT.that(set.subSet(2, 3)).has().item(2);
    ASSERT.that(set.subSet(1, 3)).has().exactly(1, 2).inOrder();
    ASSERT.that(set.subSet(1, 2)).has().item(1);
    ASSERT.that(set.subSet(2, 2)).isEmpty();
    ASSERT.that(set.subSet(Integer.MIN_VALUE, Integer.MAX_VALUE)).has().exactly(1, 2, 3).inOrder();
    ASSERT.that(set.subSet(1, true, 3, true)).has().exactly(1, 2, 3).inOrder();
    ASSERT.that(set.subSet(1, false, 3, true)).has().exactly(2, 3).inOrder();
    ASSERT.that(set.subSet(1, true, 3, false)).has().exactly(1, 2).inOrder();
    ASSERT.that(set.subSet(1, false, 3, false)).has().item(2);
  }

  public void testSubSet_outOfOrder() {
    ImmutableSortedSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    try {
      set.subSet(3, 2);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testSubSet_tooLarge() {
    ASSERT.that(ContiguousSet.create(Range.closed(1, 3), integers()).subSet(4, 6)).isEmpty();
  }

  public void testSubSet_tooSmall() {
    ASSERT.that(ContiguousSet.create(Range.closed(1, 3), integers()).subSet(-1, 0)).isEmpty();
  }

  public void testFirst() {
    assertEquals(1, ContiguousSet.create(Range.closed(1, 3), integers()).first().intValue());
    assertEquals(1, ContiguousSet.create(Range.open(0, 4), integers()).first().intValue());
    assertEquals(Integer.MIN_VALUE,
        ContiguousSet.create(Range.<Integer>all(), integers()).first().intValue());
  }

  public void testLast() {
    assertEquals(3, ContiguousSet.create(Range.closed(1, 3), integers()).last().intValue());
    assertEquals(3, ContiguousSet.create(Range.open(0, 4), integers()).last().intValue());
    assertEquals(Integer.MAX_VALUE,
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
    assertFalse(set.contains("blah"));
  }

  public void testContainsAll() {
    ImmutableSortedSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    for (Set<Integer> subset : Sets.powerSet(ImmutableSet.of(1, 2, 3))) {
      assertTrue(set.containsAll(subset));
    }
    for (Set<Integer> subset : Sets.powerSet(ImmutableSet.of(1, 2, 3))) {
      assertFalse(set.containsAll(Sets.union(subset, ImmutableSet.of(9))));
    }
    assertFalse(set.containsAll(ImmutableSet.of("blah")));
  }

  public void testRange() {
    assertEquals(Range.closed(1, 3),
        ContiguousSet.create(Range.closed(1, 3), integers()).range());
    assertEquals(Range.closed(1, 3),
        ContiguousSet.create(Range.closedOpen(1, 4), integers()).range());
    assertEquals(Range.closed(1, 3), ContiguousSet.create(Range.open(0, 4), integers()).range());
    assertEquals(Range.closed(1, 3),
        ContiguousSet.create(Range.openClosed(0, 3), integers()).range());

    assertEquals(Range.openClosed(0, 3),
        ContiguousSet.create(Range.closed(1, 3), integers()).range(OPEN, CLOSED));
    assertEquals(Range.openClosed(0, 3),
        ContiguousSet.create(Range.closedOpen(1, 4), integers()).range(OPEN, CLOSED));
    assertEquals(Range.openClosed(0, 3),
        ContiguousSet.create(Range.open(0, 4), integers()).range(OPEN, CLOSED));
    assertEquals(Range.openClosed(0, 3),
        ContiguousSet.create(Range.openClosed(0, 3), integers()).range(OPEN, CLOSED));

    assertEquals(Range.open(0, 4),
        ContiguousSet.create(Range.closed(1, 3), integers()).range(OPEN, OPEN));
    assertEquals(Range.open(0, 4),
        ContiguousSet.create(Range.closedOpen(1, 4), integers()).range(OPEN, OPEN));
    assertEquals(Range.open(0, 4),
        ContiguousSet.create(Range.open(0, 4), integers()).range(OPEN, OPEN));
    assertEquals(Range.open(0, 4),
        ContiguousSet.create(Range.openClosed(0, 3), integers()).range(OPEN, OPEN));

    assertEquals(Range.closedOpen(1, 4),
        ContiguousSet.create(Range.closed(1, 3), integers()).range(CLOSED, OPEN));
    assertEquals(Range.closedOpen(1, 4),
        ContiguousSet.create(Range.closedOpen(1, 4), integers()).range(CLOSED, OPEN));
    assertEquals(Range.closedOpen(1, 4),
        ContiguousSet.create(Range.open(0, 4), integers()).range(CLOSED, OPEN));
    assertEquals(Range.closedOpen(1, 4),
        ContiguousSet.create(Range.openClosed(0, 3), integers()).range(CLOSED, OPEN));
  }

  public void testRange_unboundedRange() {
    assertEquals(Range.closed(Integer.MIN_VALUE, Integer.MAX_VALUE),
        ContiguousSet.create(Range.<Integer>all(), integers()).range());
    assertEquals(Range.atLeast(Integer.MIN_VALUE),
        ContiguousSet.create(Range.<Integer>all(), integers()).range(CLOSED, OPEN));
    assertEquals(Range.all(),
        ContiguousSet.create(Range.<Integer>all(), integers()).range(OPEN, OPEN));
    assertEquals(Range.atMost(Integer.MAX_VALUE),
        ContiguousSet.create(Range.<Integer>all(), integers()).range(OPEN, CLOSED));
  }

  public void testIntersection_empty() {
    ContiguousSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    ContiguousSet<Integer> emptySet = ContiguousSet.create(Range.closedOpen(2, 2), integers());
    assertEquals(ImmutableSet.of(), set.intersection(emptySet));
    assertEquals(ImmutableSet.of(), emptySet.intersection(set));
    assertEquals(ImmutableSet.of(),
        ContiguousSet.create(Range.closed(-5, -1), integers()).intersection(
            ContiguousSet.create(Range.open(3, 64), integers())));
  }

  public void testIntersection() {
    ContiguousSet<Integer> set = ContiguousSet.create(Range.closed(1, 3), integers());
    assertEquals(ImmutableSet.of(1, 2, 3),
        ContiguousSet.create(Range.open(-1, 4), integers()).intersection(set));
    assertEquals(ImmutableSet.of(1, 2, 3),
        set.intersection(ContiguousSet.create(Range.open(-1, 4), integers())));
  }
}

