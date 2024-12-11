/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.collect.BoundType.CLOSED;
import static com.google.common.collect.BoundType.OPEN;
import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Objects;
import com.google.common.testing.NullPointerTester;
import java.util.List;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@code GeneralRange}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public class GeneralRangeTest extends TestCase {
  private static final Ordering<@Nullable Integer> ORDERING =
      Ordering.<Integer>natural().<Integer>nullsFirst();

  private static final List<@Nullable Integer> IN_ORDER_VALUES = asList(null, 1, 2, 3, 4, 5);

  public void testCreateEmptyRangeFails() {
    for (BoundType lboundType : BoundType.values()) {
      for (BoundType uboundType : BoundType.values()) {
        assertThrows(
            IllegalArgumentException.class,
            () -> GeneralRange.range(ORDERING, 4, lboundType, 2, uboundType));
      }
    }
  }

  public void testCreateEmptyRangeOpenOpenFails() {
    for (Integer i : IN_ORDER_VALUES) {
      assertThrows(
          IllegalArgumentException.class,
          () -> GeneralRange.<@Nullable Integer>range(ORDERING, i, OPEN, i, OPEN));
    }
  }

  public void testCreateEmptyRangeClosedOpenSucceeds() {
    for (Integer i : IN_ORDER_VALUES) {
      GeneralRange<@Nullable Integer> range = GeneralRange.range(ORDERING, i, CLOSED, i, OPEN);
      for (Integer j : IN_ORDER_VALUES) {
        assertFalse(range.contains(j));
      }
    }
  }

  public void testCreateEmptyRangeOpenClosedSucceeds() {
    for (Integer i : IN_ORDER_VALUES) {
      GeneralRange<@Nullable Integer> range = GeneralRange.range(ORDERING, i, OPEN, i, CLOSED);
      for (Integer j : IN_ORDER_VALUES) {
        assertFalse(range.contains(j));
      }
    }
  }

  public void testCreateSingletonRangeSucceeds() {
    for (Integer i : IN_ORDER_VALUES) {
      GeneralRange<@Nullable Integer> range = GeneralRange.range(ORDERING, i, CLOSED, i, CLOSED);
      for (Integer j : IN_ORDER_VALUES) {
        assertEquals(Objects.equal(i, j), range.contains(j));
      }
    }
  }

  public void testSingletonRange() {
    GeneralRange<@Nullable Integer> range = GeneralRange.range(ORDERING, 3, CLOSED, 3, CLOSED);
    for (Integer i : IN_ORDER_VALUES) {
      assertEquals(ORDERING.compare(i, 3) == 0, range.contains(i));
    }
  }

  public void testLowerRange() {
    for (BoundType lBoundType : BoundType.values()) {
      GeneralRange<@Nullable Integer> range = GeneralRange.downTo(ORDERING, 3, lBoundType);
      for (Integer i : IN_ORDER_VALUES) {
        assertEquals(
            ORDERING.compare(i, 3) > 0 || (ORDERING.compare(i, 3) == 0 && lBoundType == CLOSED),
            range.contains(i));
        assertEquals(
            ORDERING.compare(i, 3) < 0 || (ORDERING.compare(i, 3) == 0 && lBoundType == OPEN),
            range.tooLow(i));
        assertFalse(range.tooHigh(i));
      }
    }
  }

  public void testUpperRange() {
    for (BoundType lBoundType : BoundType.values()) {
      GeneralRange<@Nullable Integer> range = GeneralRange.upTo(ORDERING, 3, lBoundType);
      for (Integer i : IN_ORDER_VALUES) {
        assertEquals(
            ORDERING.compare(i, 3) < 0 || (ORDERING.compare(i, 3) == 0 && lBoundType == CLOSED),
            range.contains(i));
        assertEquals(
            ORDERING.compare(i, 3) > 0 || (ORDERING.compare(i, 3) == 0 && lBoundType == OPEN),
            range.tooHigh(i));
        assertFalse(range.tooLow(i));
      }
    }
  }

  public void testDoublyBoundedAgainstRange() {
    for (BoundType lboundType : BoundType.values()) {
      for (BoundType uboundType : BoundType.values()) {
        Range<Integer> range = Range.range(2, lboundType, 4, uboundType);
        GeneralRange<@Nullable Integer> gRange =
            GeneralRange.range(ORDERING, 2, lboundType, 4, uboundType);
        for (Integer i : IN_ORDER_VALUES) {
          assertEquals(i != null && range.contains(i), gRange.contains(i));
        }
      }
    }
  }

  public void testIntersectAgainstMatchingEndpointsRange() {
    GeneralRange<Integer> range = GeneralRange.range(ORDERING, 2, CLOSED, 4, OPEN);
    assertEquals(
        GeneralRange.range(ORDERING, 2, OPEN, 4, OPEN),
        range.intersect(GeneralRange.range(ORDERING, 2, OPEN, 4, CLOSED)));
  }

  public void testIntersectAgainstBiggerRange() {
    GeneralRange<Integer> range = GeneralRange.range(ORDERING, 2, CLOSED, 4, OPEN);

    assertEquals(
        GeneralRange.range(ORDERING, 2, CLOSED, 4, OPEN),
        range.intersect(GeneralRange.<@Nullable Integer>range(ORDERING, null, OPEN, 5, CLOSED)));

    assertEquals(
        GeneralRange.range(ORDERING, 2, OPEN, 4, OPEN),
        range.intersect(GeneralRange.range(ORDERING, 2, OPEN, 5, CLOSED)));

    assertEquals(
        GeneralRange.range(ORDERING, 2, CLOSED, 4, OPEN),
        range.intersect(GeneralRange.range(ORDERING, 1, OPEN, 4, OPEN)));
  }

  public void testIntersectAgainstSmallerRange() {
    GeneralRange<Integer> range = GeneralRange.range(ORDERING, 2, OPEN, 4, OPEN);
    assertEquals(
        GeneralRange.range(ORDERING, 3, CLOSED, 4, OPEN),
        range.intersect(GeneralRange.range(ORDERING, 3, CLOSED, 4, CLOSED)));
  }

  public void testIntersectOverlappingRange() {
    GeneralRange<Integer> range = GeneralRange.range(ORDERING, 2, OPEN, 4, CLOSED);
    assertEquals(
        GeneralRange.range(ORDERING, 3, CLOSED, 4, CLOSED),
        range.intersect(GeneralRange.range(ORDERING, 3, CLOSED, 5, CLOSED)));
    assertEquals(
        GeneralRange.range(ORDERING, 2, OPEN, 3, OPEN),
        range.intersect(GeneralRange.range(ORDERING, 1, OPEN, 3, OPEN)));
  }

  public void testIntersectNonOverlappingRange() {
    GeneralRange<Integer> range = GeneralRange.range(ORDERING, 2, OPEN, 4, CLOSED);
    assertTrue(range.intersect(GeneralRange.range(ORDERING, 5, CLOSED, 6, CLOSED)).isEmpty());
    assertTrue(range.intersect(GeneralRange.range(ORDERING, 1, OPEN, 2, OPEN)).isEmpty());
  }

  public void testFromRangeAll() {
    assertEquals(GeneralRange.all(Ordering.natural()), GeneralRange.from(Range.all()));
  }

  public void testFromRangeOneEnd() {
    for (BoundType endpointType : BoundType.values()) {
      assertEquals(
          GeneralRange.upTo(Ordering.natural(), 3, endpointType),
          GeneralRange.from(Range.upTo(3, endpointType)));

      assertEquals(
          GeneralRange.downTo(Ordering.natural(), 3, endpointType),
          GeneralRange.from(Range.downTo(3, endpointType)));
    }
  }

  public void testFromRangeTwoEnds() {
    for (BoundType lowerType : BoundType.values()) {
      for (BoundType upperType : BoundType.values()) {
        assertEquals(
            GeneralRange.range(Ordering.natural(), 3, lowerType, 4, upperType),
            GeneralRange.from(Range.range(3, lowerType, 4, upperType)));
      }
    }
  }

  public void testReverse() {
    assertEquals(GeneralRange.all(ORDERING.reverse()), GeneralRange.all(ORDERING).reverse());
    assertEquals(
        GeneralRange.downTo(ORDERING.reverse(), 3, CLOSED),
        GeneralRange.upTo(ORDERING, 3, CLOSED).reverse());
    assertEquals(
        GeneralRange.upTo(ORDERING.reverse(), 3, OPEN),
        GeneralRange.downTo(ORDERING, 3, OPEN).reverse());
    assertEquals(
        GeneralRange.range(ORDERING.reverse(), 5, OPEN, 3, CLOSED),
        GeneralRange.range(ORDERING, 3, CLOSED, 5, OPEN).reverse());
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    new NullPointerTester().testAllPublicStaticMethods(GeneralRange.class);
  }
}
