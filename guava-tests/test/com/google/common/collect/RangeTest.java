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
import static com.google.common.collect.DiscreteDomain.integers;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Predicate;
import com.google.common.collect.testing.Helpers;
import com.google.common.testing.EqualsTester;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import junit.framework.TestCase;

/**
 * Unit test for {@link Range}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class RangeTest extends TestCase {
  public void testOpen() {
    Range<Integer> range = Range.open(4, 8);
    checkContains(range);
    assertTrue(range.hasLowerBound());
    assertEquals(4, (int) range.lowerEndpoint());
    assertEquals(OPEN, range.lowerBoundType());
    assertTrue(range.hasUpperBound());
    assertEquals(8, (int) range.upperEndpoint());
    assertEquals(OPEN, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("(4..8)", range.toString());
    reserializeAndAssert(range);
  }

  public void testOpen_invalid() {
    try {
      Range.open(4, 3);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      Range.open(3, 3);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testClosed() {
    Range<Integer> range = Range.closed(5, 7);
    checkContains(range);
    assertTrue(range.hasLowerBound());
    assertEquals(5, (int) range.lowerEndpoint());
    assertEquals(CLOSED, range.lowerBoundType());
    assertTrue(range.hasUpperBound());
    assertEquals(7, (int) range.upperEndpoint());
    assertEquals(CLOSED, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("[5..7]", range.toString());
    reserializeAndAssert(range);
  }

  public void testClosed_invalid() {
    try {
      Range.closed(4, 3);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testOpenClosed() {
    Range<Integer> range = Range.openClosed(4, 7);
    checkContains(range);
    assertTrue(range.hasLowerBound());
    assertEquals(4, (int) range.lowerEndpoint());
    assertEquals(OPEN, range.lowerBoundType());
    assertTrue(range.hasUpperBound());
    assertEquals(7, (int) range.upperEndpoint());
    assertEquals(CLOSED, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("(4..7]", range.toString());
    reserializeAndAssert(range);
  }

  public void testClosedOpen() {
    Range<Integer> range = Range.closedOpen(5, 8);
    checkContains(range);
    assertTrue(range.hasLowerBound());
    assertEquals(5, (int) range.lowerEndpoint());
    assertEquals(CLOSED, range.lowerBoundType());
    assertTrue(range.hasUpperBound());
    assertEquals(8, (int) range.upperEndpoint());
    assertEquals(OPEN, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("[5..8)", range.toString());
    reserializeAndAssert(range);
  }

  public void testIsConnected() {
    assertTrue(Range.closed(3, 5).isConnected(Range.open(5, 6)));
    assertTrue(Range.closed(3, 5).isConnected(Range.closed(5, 6)));
    assertTrue(Range.closed(5, 6).isConnected(Range.closed(3, 5)));
    assertTrue(Range.closed(3, 5).isConnected(Range.openClosed(5, 5)));
    assertTrue(Range.open(3, 5).isConnected(Range.closed(5, 6)));
    assertTrue(Range.closed(3, 7).isConnected(Range.open(6, 8)));
    assertTrue(Range.open(3, 7).isConnected(Range.closed(5, 6)));
    assertFalse(Range.closed(3, 5).isConnected(Range.closed(7, 8)));
    assertFalse(Range.closed(3, 5).isConnected(Range.closedOpen(7, 7)));
  }

  private static void checkContains(Range<Integer> range) {
    assertFalse(range.contains(4));
    assertTrue(range.contains(5));
    assertTrue(range.contains(7));
    assertFalse(range.contains(8));
  }

  public void testSingleton() {
    Range<Integer> range = Range.closed(4, 4);
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
    assertEquals("[4..4]", range.toString());
    reserializeAndAssert(range);
  }

  public void testEmpty1() {
    Range<Integer> range = Range.closedOpen(4, 4);
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
    assertEquals("[4..4)", range.toString());
    reserializeAndAssert(range);
  }

  public void testEmpty2() {
    Range<Integer> range = Range.openClosed(4, 4);
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
    assertEquals("(4..4]", range.toString());
    reserializeAndAssert(range);
  }

  public void testLessThan() {
    Range<Integer> range = Range.lessThan(5);
    assertTrue(range.contains(Integer.MIN_VALUE));
    assertTrue(range.contains(4));
    assertFalse(range.contains(5));
    assertUnboundedBelow(range);
    assertTrue(range.hasUpperBound());
    assertEquals(5, (int) range.upperEndpoint());
    assertEquals(OPEN, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("(-\u221e..5)", range.toString());
    reserializeAndAssert(range);
  }

  public void testGreaterThan() {
    Range<Integer> range = Range.greaterThan(5);
    assertFalse(range.contains(5));
    assertTrue(range.contains(6));
    assertTrue(range.contains(Integer.MAX_VALUE));
    assertTrue(range.hasLowerBound());
    assertEquals(5, (int) range.lowerEndpoint());
    assertEquals(OPEN, range.lowerBoundType());
    assertUnboundedAbove(range);
    assertFalse(range.isEmpty());
    assertEquals("(5..+\u221e)", range.toString());
    reserializeAndAssert(range);
  }

  public void testAtLeast() {
    Range<Integer> range = Range.atLeast(6);
    assertFalse(range.contains(5));
    assertTrue(range.contains(6));
    assertTrue(range.contains(Integer.MAX_VALUE));
    assertTrue(range.hasLowerBound());
    assertEquals(6, (int) range.lowerEndpoint());
    assertEquals(CLOSED, range.lowerBoundType());
    assertUnboundedAbove(range);
    assertFalse(range.isEmpty());
    assertEquals("[6..+\u221e)", range.toString());
    reserializeAndAssert(range);
  }

  public void testAtMost() {
    Range<Integer> range = Range.atMost(4);
    assertTrue(range.contains(Integer.MIN_VALUE));
    assertTrue(range.contains(4));
    assertFalse(range.contains(5));
    assertUnboundedBelow(range);
    assertTrue(range.hasUpperBound());
    assertEquals(4, (int) range.upperEndpoint());
    assertEquals(CLOSED, range.upperBoundType());
    assertFalse(range.isEmpty());
    assertEquals("(-\u221e..4]", range.toString());
    reserializeAndAssert(range);
  }

  public void testAll() {
    Range<Integer> range = Range.all();
    assertTrue(range.contains(Integer.MIN_VALUE));
    assertTrue(range.contains(Integer.MAX_VALUE));
    assertUnboundedBelow(range);
    assertUnboundedAbove(range);
    assertFalse(range.isEmpty());
    assertEquals("(-\u221e..+\u221e)", range.toString());
    assertSame(range, reserializeAndAssert(range));
    assertSame(range, Range.all());
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
    Cut<Integer> a = Range.lessThan(0).lowerBound;
    Cut<Integer> b = Range.atLeast(0).lowerBound;
    Cut<Integer> c = Range.greaterThan(0).lowerBound;
    Cut<Integer> d = Range.atLeast(1).lowerBound;
    Cut<Integer> e = Range.greaterThan(1).lowerBound;
    Cut<Integer> f = Range.greaterThan(1).upperBound;

    Helpers.testCompareToAndEquals(ImmutableList.of(a, b, c, d, e, f));
  }

  public void testContainsAll() {
    Range<Integer> range = Range.closed(3, 5);
    assertTrue(range.containsAll(asList(3, 3, 4, 5)));
    assertFalse(range.containsAll(asList(3, 3, 4, 5, 6)));

    // We happen to know that natural-order sorted sets use a different code
    // path, so we test that separately
    assertTrue(range.containsAll(ImmutableSortedSet.of(3, 3, 4, 5)));
    assertTrue(range.containsAll(ImmutableSortedSet.of(3)));
    assertTrue(range.containsAll(ImmutableSortedSet.<Integer>of()));
    assertFalse(range.containsAll(ImmutableSortedSet.of(3, 3, 4, 5, 6)));

    assertTrue(Range.openClosed(3, 3).containsAll(Collections.<Integer>emptySet()));
  }

  public void testEncloses_open() {
    Range<Integer> range = Range.open(2, 5);
    assertTrue(range.encloses(range));
    assertTrue(range.encloses(Range.open(2, 4)));
    assertTrue(range.encloses(Range.open(3, 5)));
    assertTrue(range.encloses(Range.closed(3, 4)));

    assertFalse(range.encloses(Range.openClosed(2, 5)));
    assertFalse(range.encloses(Range.closedOpen(2, 5)));
    assertFalse(range.encloses(Range.closed(1, 4)));
    assertFalse(range.encloses(Range.closed(3, 6)));
    assertFalse(range.encloses(Range.greaterThan(3)));
    assertFalse(range.encloses(Range.lessThan(3)));
    assertFalse(range.encloses(Range.atLeast(3)));
    assertFalse(range.encloses(Range.atMost(3)));
    assertFalse(range.encloses(Range.<Integer>all()));
  }

  public void testEncloses_closed() {
    Range<Integer> range = Range.closed(2, 5);
    assertTrue(range.encloses(range));
    assertTrue(range.encloses(Range.open(2, 5)));
    assertTrue(range.encloses(Range.openClosed(2, 5)));
    assertTrue(range.encloses(Range.closedOpen(2, 5)));
    assertTrue(range.encloses(Range.closed(3, 5)));
    assertTrue(range.encloses(Range.closed(2, 4)));

    assertFalse(range.encloses(Range.open(1, 6)));
    assertFalse(range.encloses(Range.greaterThan(3)));
    assertFalse(range.encloses(Range.lessThan(3)));
    assertFalse(range.encloses(Range.atLeast(3)));
    assertFalse(range.encloses(Range.atMost(3)));
    assertFalse(range.encloses(Range.<Integer>all()));
  }

  public void testIntersection_empty() {
    Range<Integer> range = Range.closedOpen(3, 3);
    assertEquals(range, range.intersection(range));

    try {
      range.intersection(Range.open(3, 5));
      fail();
    } catch (IllegalArgumentException expected) {
      // TODO(kevinb): convert the rest of this file to Truth someday
      assertThat(expected).hasMessageThat().contains("connected");
    }
    try {
      range.intersection(Range.closed(0, 2));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().contains("connected");
    }
  }

  public void testIntersection_deFactoEmpty() {
    Range<Integer> range = Range.open(3, 4);
    assertEquals(range, range.intersection(range));

    assertEquals(Range.openClosed(3, 3), range.intersection(Range.atMost(3)));
    assertEquals(Range.closedOpen(4, 4), range.intersection(Range.atLeast(4)));

    try {
      range.intersection(Range.lessThan(3));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().contains("connected");
    }
    try {
      range.intersection(Range.greaterThan(4));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().contains("connected");
    }

    range = Range.closed(3, 4);
    assertEquals(Range.openClosed(4, 4), range.intersection(Range.greaterThan(4)));
  }

  public void testIntersection_singleton() {
    Range<Integer> range = Range.closed(3, 3);
    assertEquals(range, range.intersection(range));

    assertEquals(range, range.intersection(Range.atMost(4)));
    assertEquals(range, range.intersection(Range.atMost(3)));
    assertEquals(range, range.intersection(Range.atLeast(3)));
    assertEquals(range, range.intersection(Range.atLeast(2)));

    assertEquals(Range.closedOpen(3, 3), range.intersection(Range.lessThan(3)));
    assertEquals(Range.openClosed(3, 3), range.intersection(Range.greaterThan(3)));

    try {
      range.intersection(Range.atLeast(4));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().contains("connected");
    }
    try {
      range.intersection(Range.atMost(2));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().contains("connected");
    }
  }

  public void testIntersection_general() {
    Range<Integer> range = Range.closed(4, 8);

    // separate below
    try {
      range.intersection(Range.closed(0, 2));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().contains("connected");
    }

    // adjacent below
    assertEquals(Range.closedOpen(4, 4), range.intersection(Range.closedOpen(2, 4)));

    // overlap below
    assertEquals(Range.closed(4, 6), range.intersection(Range.closed(2, 6)));

    // enclosed with same start
    assertEquals(Range.closed(4, 6), range.intersection(Range.closed(4, 6)));

    // enclosed, interior
    assertEquals(Range.closed(5, 7), range.intersection(Range.closed(5, 7)));

    // enclosed with same end
    assertEquals(Range.closed(6, 8), range.intersection(Range.closed(6, 8)));

    // equal
    assertEquals(range, range.intersection(range));

    // enclosing with same start
    assertEquals(range, range.intersection(Range.closed(4, 10)));

    // enclosing with same end
    assertEquals(range, range.intersection(Range.closed(2, 8)));

    // enclosing, exterior
    assertEquals(range, range.intersection(Range.closed(2, 10)));

    // overlap above
    assertEquals(Range.closed(6, 8), range.intersection(Range.closed(6, 10)));

    // adjacent above
    assertEquals(Range.openClosed(8, 8), range.intersection(Range.openClosed(8, 10)));

    // separate above
    try {
      range.intersection(Range.closed(10, 12));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().contains("connected");
    }
  }

  public void testGap_overlapping() {
    Range<Integer> range = Range.closedOpen(3, 5);

    try {
      range.gap(Range.closed(4, 6));
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      range.gap(Range.closed(2, 4));
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      range.gap(Range.closed(2, 3));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGap_invalidRangesWithInfinity() {
    try {
      Range.atLeast(1).gap(Range.atLeast(2));
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      Range.atLeast(2).gap(Range.atLeast(1));
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      Range.atMost(1).gap(Range.atMost(2));
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      Range.atMost(2).gap(Range.atMost(1));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGap_connectedAdjacentYieldsEmpty() {
    Range<Integer> range = Range.open(3, 4);

    assertEquals(Range.closedOpen(4, 4), range.gap(Range.atLeast(4)));
    assertEquals(Range.openClosed(3, 3), range.gap(Range.atMost(3)));
  }

  public void testGap_general() {
    Range<Integer> openRange = Range.open(4, 8);
    Range<Integer> closedRange = Range.closed(4, 8);

    // first range open end, second range open start
    assertEquals(Range.closed(2, 4), Range.lessThan(2).gap(openRange));
    assertEquals(Range.closed(2, 4), openRange.gap(Range.lessThan(2)));

    // first range closed end, second range open start
    assertEquals(Range.openClosed(2, 4), Range.atMost(2).gap(openRange));
    assertEquals(Range.openClosed(2, 4), openRange.gap(Range.atMost(2)));

    // first range open end, second range closed start
    assertEquals(Range.closedOpen(2, 4), Range.lessThan(2).gap(closedRange));
    assertEquals(Range.closedOpen(2, 4), closedRange.gap(Range.lessThan(2)));

    // first range closed end, second range closed start
    assertEquals(Range.open(2, 4), Range.atMost(2).gap(closedRange));
    assertEquals(Range.open(2, 4), closedRange.gap(Range.atMost(2)));
  }

  // TODO(cpovirk): More extensive testing of gap().

  public void testSpan_general() {
    Range<Integer> range = Range.closed(4, 8);

    // separate below
    assertEquals(Range.closed(0, 8), range.span(Range.closed(0, 2)));
    assertEquals(Range.atMost(8), range.span(Range.atMost(2)));

    // adjacent below
    assertEquals(Range.closed(2, 8), range.span(Range.closedOpen(2, 4)));
    assertEquals(Range.atMost(8), range.span(Range.lessThan(4)));

    // overlap below
    assertEquals(Range.closed(2, 8), range.span(Range.closed(2, 6)));
    assertEquals(Range.atMost(8), range.span(Range.atMost(6)));

    // enclosed with same start
    assertEquals(range, range.span(Range.closed(4, 6)));

    // enclosed, interior
    assertEquals(range, range.span(Range.closed(5, 7)));

    // enclosed with same end
    assertEquals(range, range.span(Range.closed(6, 8)));

    // equal
    assertEquals(range, range.span(range));

    // enclosing with same start
    assertEquals(Range.closed(4, 10), range.span(Range.closed(4, 10)));
    assertEquals(Range.atLeast(4), range.span(Range.atLeast(4)));

    // enclosing with same end
    assertEquals(Range.closed(2, 8), range.span(Range.closed(2, 8)));
    assertEquals(Range.atMost(8), range.span(Range.atMost(8)));

    // enclosing, exterior
    assertEquals(Range.closed(2, 10), range.span(Range.closed(2, 10)));
    assertEquals(Range.<Integer>all(), range.span(Range.<Integer>all()));

    // overlap above
    assertEquals(Range.closed(4, 10), range.span(Range.closed(6, 10)));
    assertEquals(Range.atLeast(4), range.span(Range.atLeast(6)));

    // adjacent above
    assertEquals(Range.closed(4, 10), range.span(Range.openClosed(8, 10)));
    assertEquals(Range.atLeast(4), range.span(Range.greaterThan(8)));

    // separate above
    assertEquals(Range.closed(4, 12), range.span(Range.closed(10, 12)));
    assertEquals(Range.atLeast(4), range.span(Range.atLeast(10)));
  }

  public void testApply() {
    Predicate<Integer> predicate = Range.closed(2, 3);
    assertFalse(predicate.apply(1));
    assertTrue(predicate.apply(2));
    assertTrue(predicate.apply(3));
    assertFalse(predicate.apply(4));
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(Range.open(1, 5), Range.range(1, OPEN, 5, OPEN))
        .addEqualityGroup(Range.greaterThan(2), Range.greaterThan(2))
        .addEqualityGroup(Range.all(), Range.all())
        .addEqualityGroup("Phil")
        .testEquals();
  }

  @GwtIncompatible // TODO(b/148207871): Restore once Eclipse compiler no longer flakes for this.
  public void testLegacyComparable() {
    Range<LegacyComparable> range = Range.closed(LegacyComparable.X, LegacyComparable.Y);
  }

  private static final DiscreteDomain<Integer> UNBOUNDED_DOMAIN =
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
      };

  public void testCanonical() {
    assertEquals(Range.closedOpen(1, 5), Range.closed(1, 4).canonical(integers()));
    assertEquals(Range.closedOpen(1, 5), Range.open(0, 5).canonical(integers()));
    assertEquals(Range.closedOpen(1, 5), Range.closedOpen(1, 5).canonical(integers()));
    assertEquals(Range.closedOpen(1, 5), Range.openClosed(0, 4).canonical(integers()));

    assertEquals(
        Range.closedOpen(Integer.MIN_VALUE, 0),
        Range.closedOpen(Integer.MIN_VALUE, 0).canonical(integers()));

    assertEquals(Range.closedOpen(Integer.MIN_VALUE, 0), Range.lessThan(0).canonical(integers()));
    assertEquals(Range.closedOpen(Integer.MIN_VALUE, 1), Range.atMost(0).canonical(integers()));
    assertEquals(Range.atLeast(0), Range.atLeast(0).canonical(integers()));
    assertEquals(Range.atLeast(1), Range.greaterThan(0).canonical(integers()));

    assertEquals(Range.atLeast(Integer.MIN_VALUE), Range.<Integer>all().canonical(integers()));
  }

  public void testCanonical_unboundedDomain() {
    assertEquals(Range.lessThan(0), Range.lessThan(0).canonical(UNBOUNDED_DOMAIN));
    assertEquals(Range.lessThan(1), Range.atMost(0).canonical(UNBOUNDED_DOMAIN));
    assertEquals(Range.atLeast(0), Range.atLeast(0).canonical(UNBOUNDED_DOMAIN));
    assertEquals(Range.atLeast(1), Range.greaterThan(0).canonical(UNBOUNDED_DOMAIN));

    assertEquals(Range.all(), Range.<Integer>all().canonical(UNBOUNDED_DOMAIN));
  }

  public void testEncloseAll() {
    assertEquals(Range.closed(0, 0), Range.encloseAll(Arrays.asList(0)));
    assertEquals(Range.closed(-3, 5), Range.encloseAll(Arrays.asList(5, -3)));
    assertEquals(Range.closed(-3, 5), Range.encloseAll(Arrays.asList(1, 2, 2, 2, 5, -3, 0, -1)));
  }

  public void testEncloseAll_empty() {
    try {
      Range.encloseAll(ImmutableSet.<Integer>of());
      fail();
    } catch (NoSuchElementException expected) {
    }
  }

  public void testEncloseAll_nullValue() {
    List<Integer> nullFirst = Lists.newArrayList(null, 0);
    try {
      Range.encloseAll(nullFirst);
      fail();
    } catch (NullPointerException expected) {
    }
    List<Integer> nullNotFirst = Lists.newArrayList(0, null);
    try {
      Range.encloseAll(nullNotFirst);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testEquivalentFactories() {
    new EqualsTester()
        .addEqualityGroup(Range.all())
        .addEqualityGroup(Range.atLeast(1), Range.downTo(1, CLOSED))
        .addEqualityGroup(Range.greaterThan(1), Range.downTo(1, OPEN))
        .addEqualityGroup(Range.atMost(7), Range.upTo(7, CLOSED))
        .addEqualityGroup(Range.lessThan(7), Range.upTo(7, OPEN))
        .addEqualityGroup(Range.open(1, 7), Range.range(1, OPEN, 7, OPEN))
        .addEqualityGroup(Range.openClosed(1, 7), Range.range(1, OPEN, 7, CLOSED))
        .addEqualityGroup(Range.closed(1, 7), Range.range(1, CLOSED, 7, CLOSED))
        .addEqualityGroup(Range.closedOpen(1, 7), Range.range(1, CLOSED, 7, OPEN))
        .testEquals();
  }
}
