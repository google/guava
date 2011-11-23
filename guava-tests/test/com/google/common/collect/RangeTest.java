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

import static com.google.common.collect.BoundType.CLOSED;
import static com.google.common.collect.BoundType.OPEN;
import static com.google.common.collect.DiscreteDomains.integers;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Predicate;
import com.google.common.collect.testing.Helpers;
import com.google.common.testing.EqualsTester;

import junit.framework.TestCase;

import java.util.Collections;

/**
 * Unit test for {@link Range}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
public class RangeTest extends TestCase {
  public void testOpen() {
    Range<Integer> range = Ranges.open(4, 8);
    checkContains(range);
    assertTrue(range.hasLowerBound());
    assertEquals(4, (int) range.lowerEndpoint());
    assertEquals(OPEN, range.lowerBoundType());
    assertTrue(range.hasUpperBound());
    assertEquals(8, (int) range.upperEndpoint());
    assertEquals(OPEN, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("(4\u20258)", range.toString());
    reserializeAndAssert(range);
  }

  public void testOpen_invalid() {
    try {
      Ranges.open(4, 3);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      Ranges.open(3, 3);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testClosed() {
    Range<Integer> range = Ranges.closed(5, 7);
    checkContains(range);
    assertTrue(range.hasLowerBound());
    assertEquals(5, (int) range.lowerEndpoint());
    assertEquals(CLOSED, range.lowerBoundType());
    assertTrue(range.hasUpperBound());
    assertEquals(7, (int) range.upperEndpoint());
    assertEquals(CLOSED, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("[5\u20257]", range.toString());
    reserializeAndAssert(range);
  }

  public void testClosed_invalid() {
    try {
      Ranges.closed(4, 3);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testOpenClosed() {
    Range<Integer> range = Ranges.openClosed(4, 7);
    checkContains(range);
    assertTrue(range.hasLowerBound());
    assertEquals(4, (int) range.lowerEndpoint());
    assertEquals(OPEN, range.lowerBoundType());
    assertTrue(range.hasUpperBound());
    assertEquals(7, (int) range.upperEndpoint());
    assertEquals(CLOSED, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("(4\u20257]", range.toString());
    reserializeAndAssert(range);
  }

  public void testClosedOpen() {
    Range<Integer> range = Ranges.closedOpen(5, 8);
    checkContains(range);
    assertTrue(range.hasLowerBound());
    assertEquals(5, (int) range.lowerEndpoint());
    assertEquals(CLOSED, range.lowerBoundType());
    assertTrue(range.hasUpperBound());
    assertEquals(8, (int) range.upperEndpoint());
    assertEquals(OPEN, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("[5\u20258)", range.toString());
    reserializeAndAssert(range);
  }

  public void testIsConnected() {
    assertTrue(Ranges.closed(3, 5).isConnected(Ranges.open(5, 6)));
    assertTrue(Ranges.closed(3, 5).isConnected(Ranges.openClosed(5, 5)));
    assertTrue(Ranges.open(3, 5).isConnected(Ranges.closed(5, 6)));
    assertTrue(Ranges.closed(3, 7).isConnected(Ranges.open(6, 8)));
    assertTrue(Ranges.open(3, 7).isConnected(Ranges.closed(5, 6)));
    assertFalse(Ranges.closed(3, 5).isConnected(Ranges.closed(7, 8)));
    assertFalse(Ranges.closed(3, 5).isConnected(Ranges.closedOpen(7, 7)));
  }

  private static void checkContains(Range<Integer> range) {
    assertFalse(range.contains(4));
    assertTrue(range.contains(5));
    assertTrue(range.contains(7));
    assertFalse(range.contains(8));
  }

  public void testSingleton() {
    Range<Integer> range = Ranges.closed(4, 4);
    assertFalse(range.contains(3));
    assertTrue(range.contains(4));
    assertFalse(range.contains(5));
    assertTrue(range.hasLowerBound());
    assertEquals(4, (int) range.lowerEndpoint());
    assertEquals(CLOSED, range.lowerBoundType());
    assertTrue(range.hasUpperBound());
    assertEquals(4, (int) range.upperEndpoint());
    assertEquals(CLOSED, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("[4\u20254]", range.toString());
    reserializeAndAssert(range);
  }

  public void testEmpty1() {
    Range<Integer> range = Ranges.closedOpen(4, 4);
    assertFalse(range.contains(3));
    assertFalse(range.contains(4));
    assertFalse(range.contains(5));
    assertTrue(range.hasLowerBound());
    assertEquals(4, (int) range.lowerEndpoint());
    assertEquals(CLOSED, range.lowerBoundType());
    assertTrue(range.hasUpperBound());
    assertEquals(4, (int) range.upperEndpoint());
    assertEquals(OPEN, range.upperBoundType());
    assertTrue(range.isEmpty());
    assertEquals("[4\u20254)", range.toString());
    reserializeAndAssert(range);
  }

  public void testEmpty2() {
    Range<Integer> range = Ranges.openClosed(4, 4);
    assertFalse(range.contains(3));
    assertFalse(range.contains(4));
    assertFalse(range.contains(5));
    assertTrue(range.hasLowerBound());
    assertEquals(4, (int) range.lowerEndpoint());
    assertEquals(OPEN, range.lowerBoundType());
    assertTrue(range.hasUpperBound());
    assertEquals(4, (int) range.upperEndpoint());
    assertEquals(CLOSED, range.upperBoundType());
    assertTrue(range.isEmpty());
    assertEquals("(4\u20254]", range.toString());
    reserializeAndAssert(range);
  }

  public void testLessThan() {
    Range<Integer> range = Ranges.lessThan(5);
    assertTrue(range.contains(Integer.MIN_VALUE));
    assertTrue(range.contains(4));
    assertFalse(range.contains(5));
    assertUnboundedBelow(range);
    assertTrue(range.hasUpperBound());
    assertEquals(5, (int) range.upperEndpoint());
    assertEquals(OPEN, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("(-\u221e\u20255)", range.toString());
    reserializeAndAssert(range);
  }

  public void testGreaterThan() {
    Range<Integer> range = Ranges.greaterThan(5);
    assertFalse(range.contains(5));
    assertTrue(range.contains(6));
    assertTrue(range.contains(Integer.MAX_VALUE));
    assertTrue(range.hasLowerBound());
    assertEquals(5, (int) range.lowerEndpoint());
    assertEquals(OPEN, range.lowerBoundType());
    assertUnboundedAbove(range);
    assertFalse(range.isEmpty());
    assertEquals("(5\u2025+\u221e)", range.toString());
    reserializeAndAssert(range);
  }

  public void testAtLeast() {
    Range<Integer> range = Ranges.atLeast(6);
    assertFalse(range.contains(5));
    assertTrue(range.contains(6));
    assertTrue(range.contains(Integer.MAX_VALUE));
    assertTrue(range.hasLowerBound());
    assertEquals(6, (int) range.lowerEndpoint());
    assertEquals(CLOSED, range.lowerBoundType());
    assertUnboundedAbove(range);
    assertFalse(range.isEmpty());
    assertEquals("[6\u2025+\u221e)", range.toString());
    reserializeAndAssert(range);
  }

  public void testAtMost() {
    Range<Integer> range = Ranges.atMost(4);
    assertTrue(range.contains(Integer.MIN_VALUE));
    assertTrue(range.contains(4));
    assertFalse(range.contains(5));
    assertUnboundedBelow(range);
    assertTrue(range.hasUpperBound());
    assertEquals(4, (int) range.upperEndpoint());
    assertEquals(CLOSED, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("(-\u221e\u20254]", range.toString());
    reserializeAndAssert(range);
  }

  public void testAll() {
    Range<Integer> range = Ranges.all();
    assertTrue(range.contains(Integer.MIN_VALUE));
    assertTrue(range.contains(Integer.MAX_VALUE));
    assertUnboundedBelow(range);
    assertUnboundedAbove(range);
    assertFalse(range.isEmpty());
    assertEquals("(-\u221e\u2025+\u221e)", range.toString());
    reserializeAndAssert(range);
  }

  private static void assertUnboundedBelow(Range<Integer> range) {
    assertFalse(range.hasLowerBound());
    try {
      range.lowerEndpoint();
      fail();
    } catch (IllegalStateException expected) {
    }
    try {
      range.lowerBoundType();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  private static void assertUnboundedAbove(Range<Integer> range) {
    assertFalse(range.hasUpperBound());
    try {
      range.upperEndpoint();
      fail();
    } catch (IllegalStateException expected) {
    }
    try {
      range.upperBoundType();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testOrderingCuts() {
    Cut<Integer> a = Ranges.lessThan(0).lowerBound;
    Cut<Integer> b = Ranges.atLeast(0).lowerBound;
    Cut<Integer> c = Ranges.greaterThan(0).lowerBound;
    Cut<Integer> d = Ranges.atLeast(1).lowerBound;
    Cut<Integer> e = Ranges.greaterThan(1).lowerBound;
    Cut<Integer> f = Ranges.greaterThan(1).upperBound;

    Helpers.testCompareToAndEquals(ImmutableList.of(a, b, c, d, e, f));
  }

  public void testContainsAll() {
    Range<Integer> range = Ranges.closed(3, 5);
    assertTrue(range.containsAll(asList(3, 3, 4, 5)));
    assertFalse(range.containsAll(asList(3, 3, 4, 5, 6)));

    // We happen to know that natural-order sorted sets use a different code
    // path, so we test that separately
    assertTrue(range.containsAll(ImmutableSortedSet.of(3, 3, 4, 5)));
    assertTrue(range.containsAll(ImmutableSortedSet.of(3)));
    assertTrue(range.containsAll(ImmutableSortedSet.<Integer>of()));
    assertFalse(range.containsAll(ImmutableSortedSet.of(3, 3, 4, 5, 6)));

    assertTrue(Ranges.openClosed(3, 3).containsAll(
        Collections.<Integer>emptySet()));
  }

  public void testEncloses_open() {
    Range<Integer> range = Ranges.open(2, 5);
    assertTrue(range.encloses(range));
    assertTrue(range.encloses(Ranges.open(2, 4)));
    assertTrue(range.encloses(Ranges.open(3, 5)));
    assertTrue(range.encloses(Ranges.closed(3, 4)));

    assertFalse(range.encloses(Ranges.openClosed(2, 5)));
    assertFalse(range.encloses(Ranges.closedOpen(2, 5)));
    assertFalse(range.encloses(Ranges.closed(1, 4)));
    assertFalse(range.encloses(Ranges.closed(3, 6)));
    assertFalse(range.encloses(Ranges.greaterThan(3)));
    assertFalse(range.encloses(Ranges.lessThan(3)));
    assertFalse(range.encloses(Ranges.atLeast(3)));
    assertFalse(range.encloses(Ranges.atMost(3)));
    assertFalse(range.encloses(Ranges.<Integer>all()));
  }

  public void testEncloses_closed() {
    Range<Integer> range = Ranges.closed(2, 5);
    assertTrue(range.encloses(range));
    assertTrue(range.encloses(Ranges.open(2, 5)));
    assertTrue(range.encloses(Ranges.openClosed(2, 5)));
    assertTrue(range.encloses(Ranges.closedOpen(2, 5)));
    assertTrue(range.encloses(Ranges.closed(3, 5)));
    assertTrue(range.encloses(Ranges.closed(2, 4)));

    assertFalse(range.encloses(Ranges.open(1, 6)));
    assertFalse(range.encloses(Ranges.greaterThan(3)));
    assertFalse(range.encloses(Ranges.lessThan(3)));
    assertFalse(range.encloses(Ranges.atLeast(3)));
    assertFalse(range.encloses(Ranges.atMost(3)));
    assertFalse(range.encloses(Ranges.<Integer>all()));
  }

  public void testIntersection_empty() {
    Range<Integer> range = Ranges.closedOpen(3, 3);
    assertEquals(range, range.intersection(range));

    try {
      range.intersection(Ranges.open(3, 5));
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      range.intersection(Ranges.closed(0, 2));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testIntersection_deFactoEmpty() {
    Range<Integer> range = Ranges.open(3, 4);
    assertEquals(range, range.intersection(range));

    assertEquals(Ranges.openClosed(3, 3),
        range.intersection(Ranges.atMost(3)));
    assertEquals(Ranges.closedOpen(4, 4),
        range.intersection(Ranges.atLeast(4)));
    
    try {
      range.intersection(Ranges.lessThan(3));
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      range.intersection(Ranges.greaterThan(4));
      fail();
    } catch (IllegalArgumentException expected) {
    }

    range = Ranges.closed(3, 4);
    assertEquals(Ranges.openClosed(4, 4),
        range.intersection(Ranges.greaterThan(4)));
  }

  public void testIntersection_singleton() {
    Range<Integer> range = Ranges.closed(3, 3);
    assertEquals(range, range.intersection(range));

    assertEquals(range, range.intersection(Ranges.atMost(4)));
    assertEquals(range, range.intersection(Ranges.atMost(3)));
    assertEquals(range, range.intersection(Ranges.atLeast(3)));
    assertEquals(range, range.intersection(Ranges.atLeast(2)));

    assertEquals(Ranges.closedOpen(3, 3),
        range.intersection(Ranges.lessThan(3)));
    assertEquals(Ranges.openClosed(3, 3),
        range.intersection(Ranges.greaterThan(3)));

    try {
      range.intersection(Ranges.atLeast(4));
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      range.intersection(Ranges.atMost(2));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testIntersection_general() {
    Range<Integer> range = Ranges.closed(4, 8);

    // separate below
    try {
      range.intersection(Ranges.closed(0, 2));
      fail();
    } catch (IllegalArgumentException expected) {
    }

    // adjacent below
    assertEquals(Ranges.closedOpen(4, 4),
        range.intersection(Ranges.closedOpen(2, 4)));

    // overlap below
    assertEquals(Ranges.closed(4, 6), range.intersection(Ranges.closed(2, 6)));

    // enclosed with same start
    assertEquals(Ranges.closed(4, 6), range.intersection(Ranges.closed(4, 6)));

    // enclosed, interior
    assertEquals(Ranges.closed(5, 7), range.intersection(Ranges.closed(5, 7)));

    // enclosed with same end
    assertEquals(Ranges.closed(6, 8), range.intersection(Ranges.closed(6, 8)));

    // equal
    assertEquals(range, range.intersection(range));

    // enclosing with same start
    assertEquals(range, range.intersection(Ranges.closed(4, 10)));

    // enclosing with same end
    assertEquals(range, range.intersection(Ranges.closed(2, 8)));

    // enclosing, exterior
    assertEquals(range, range.intersection(Ranges.closed(2, 10)));

    // overlap above
    assertEquals(Ranges.closed(6, 8), range.intersection(Ranges.closed(6, 10)));

    // adjacent above
    assertEquals(Ranges.openClosed(8, 8),
        range.intersection(Ranges.openClosed(8, 10)));

    // separate above
    try {
      range.intersection(Ranges.closed(10, 12));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testSpan_general() {
    Range<Integer> range = Ranges.closed(4, 8);

    // separate below
    assertEquals(Ranges.closed(0, 8), range.span(Ranges.closed(0, 2)));
    assertEquals(Ranges.atMost(8), range.span(Ranges.atMost(2)));

    // adjacent below
    assertEquals(Ranges.closed(2, 8), range.span(Ranges.closedOpen(2, 4)));
    assertEquals(Ranges.atMost(8), range.span(Ranges.lessThan(4)));

    // overlap below
    assertEquals(Ranges.closed(2, 8), range.span(Ranges.closed(2, 6)));
    assertEquals(Ranges.atMost(8), range.span(Ranges.atMost(6)));

    // enclosed with same start
    assertEquals(range, range.span(Ranges.closed(4, 6)));

    // enclosed, interior
    assertEquals(range, range.span(Ranges.closed(5, 7)));

    // enclosed with same end
    assertEquals(range, range.span(Ranges.closed(6, 8)));

    // equal
    assertEquals(range, range.span(range));

    // enclosing with same start
    assertEquals(Ranges.closed(4, 10), range.span(Ranges.closed(4, 10)));
    assertEquals(Ranges.atLeast(4), range.span(Ranges.atLeast(4)));

    // enclosing with same end
    assertEquals(Ranges.closed(2, 8), range.span(Ranges.closed(2, 8)));
    assertEquals(Ranges.atMost(8), range.span(Ranges.atMost(8)));

    // enclosing, exterior
    assertEquals(Ranges.closed(2, 10), range.span(Ranges.closed(2, 10)));
    assertEquals(Ranges.<Integer>all(), range.span(Ranges.<Integer>all()));

    // overlap above
    assertEquals(Ranges.closed(4, 10), range.span(Ranges.closed(6, 10)));
    assertEquals(Ranges.atLeast(4), range.span(Ranges.atLeast(6)));

    // adjacent above
    assertEquals(Ranges.closed(4, 10), range.span(Ranges.openClosed(8, 10)));
    assertEquals(Ranges.atLeast(4), range.span(Ranges.greaterThan(8)));

    // separate above
    assertEquals(Ranges.closed(4, 12), range.span(Ranges.closed(10, 12)));
    assertEquals(Ranges.atLeast(4), range.span(Ranges.atLeast(10)));
  }

  public void testApply() {
    Predicate<Integer> predicate = Ranges.closed(2, 3);
    assertFalse(predicate.apply(1));
    assertTrue(predicate.apply(2));
    assertTrue(predicate.apply(3));
    assertFalse(predicate.apply(4));
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(Ranges.open(1, 5),
            Ranges.range(1, OPEN, 5, OPEN))
        .addEqualityGroup(Ranges.greaterThan(2), Ranges.greaterThan(2))
        .addEqualityGroup(Ranges.all(), Ranges.all())
        .addEqualityGroup("Phil")
        .testEquals();
  }

  public void testLegacyComparable() {
    Range<LegacyComparable> range
        = Ranges.closed(LegacyComparable.X, LegacyComparable.Y);
  }

  private static final DiscreteDomain<Integer> UNBOUNDED_DOMAIN =
      new DiscreteDomain<Integer>() {
        @Override public Integer next(Integer value) {
          return DiscreteDomains.integers().next(value);
        }

        @Override public Integer previous(Integer value) {
          return DiscreteDomains.integers().previous(value);
        }

        @Override public long distance(Integer start, Integer end) {
          return DiscreteDomains.integers().distance(start, end);
        }
      };

  public void testAsSet_noMin() {
    Range<Integer> range = Ranges.lessThan(0);
    try {
      range.asSet(UNBOUNDED_DOMAIN);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testAsSet_noMax() {
    Range<Integer> range = Ranges.greaterThan(0);
    try {
      range.asSet(UNBOUNDED_DOMAIN);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testAsSet_empty() {
    assertEquals(ImmutableSet.of(), Ranges.closedOpen(1, 1).asSet(integers()));
    assertEquals(ImmutableSet.of(), Ranges.openClosed(5, 5).asSet(integers()));
    assertEquals(ImmutableSet.of(), Ranges.lessThan(Integer.MIN_VALUE).asSet(integers()));
    assertEquals(ImmutableSet.of(), Ranges.greaterThan(Integer.MAX_VALUE).asSet(integers()));
  }

  public void testCanonical() {
    assertEquals(Ranges.closedOpen(1, 5),
        Ranges.closed(1, 4).canonical(integers()));
    assertEquals(Ranges.closedOpen(1, 5),
        Ranges.open(0, 5).canonical(integers()));
    assertEquals(Ranges.closedOpen(1, 5),
        Ranges.closedOpen(1, 5).canonical(integers()));
    assertEquals(Ranges.closedOpen(1, 5),
        Ranges.openClosed(0, 4).canonical(integers()));

    assertEquals(Ranges.closedOpen(Integer.MIN_VALUE, 0),
        Ranges.closedOpen(Integer.MIN_VALUE, 0).canonical(integers()));

    assertEquals(Ranges.closedOpen(Integer.MIN_VALUE, 0),
        Ranges.lessThan(0).canonical(integers()));
    assertEquals(Ranges.closedOpen(Integer.MIN_VALUE, 1),
        Ranges.atMost(0).canonical(integers()));
    assertEquals(Ranges.atLeast(0), Ranges.atLeast(0).canonical(integers()));
    assertEquals(Ranges.atLeast(1), Ranges.greaterThan(0).canonical(integers()));

    assertEquals(Ranges.atLeast(Integer.MIN_VALUE), Ranges.<Integer>all().canonical(integers()));
  }

  public void testCanonical_unboundedDomain() {
    assertEquals(Ranges.lessThan(0), Ranges.lessThan(0).canonical(UNBOUNDED_DOMAIN));
    assertEquals(Ranges.lessThan(1), Ranges.atMost(0).canonical(UNBOUNDED_DOMAIN));
    assertEquals(Ranges.atLeast(0), Ranges.atLeast(0).canonical(UNBOUNDED_DOMAIN));
    assertEquals(Ranges.atLeast(1), Ranges.greaterThan(0).canonical(UNBOUNDED_DOMAIN));

    assertEquals(Ranges.all(), Ranges.<Integer>all().canonical(UNBOUNDED_DOMAIN));
  }
}
