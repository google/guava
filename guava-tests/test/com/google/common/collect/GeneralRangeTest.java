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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import java.util.Comparator;

/**
 * Tests for {@code GeneralRange}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class GeneralRangeTest extends TestCase {
  public void testCreateEmptyRangeFails() {
    for (BoundType lboundType : BoundType.values()) {
      for (BoundType uboundType : BoundType.values()) {
        try {
          GeneralRange<Integer> range =
              GeneralRange.range(Ordering.natural(), 4, lboundType, 2, uboundType);
          fail("Expected IAE");
        } catch (IllegalArgumentException expected) {}
      }
    }
  }

  public void testCreateEmptyRangeOpenOpenFails() {
    for (int i = 1; i <= 5; i++) {
      try {
        GeneralRange<Integer> range = GeneralRange.range(Ordering.natural(), i, OPEN, i, OPEN);
        fail("Expected IAE");
      } catch (IllegalArgumentException expected) {}
    }
  }

  public void testCreateEmptyRangeClosedOpenSucceeds() {
    for (int i = 1; i <= 5; i++) {
      GeneralRange<Integer> range = GeneralRange.range(Ordering.natural(), i, CLOSED, i, OPEN);
      for (int j = 1; j <= 5; j++) {
        assertFalse(range.contains(j));
      }
    }
  }

  public void testCreateEmptyRangeOpenClosedSucceeds() {
    for (int i = 1; i <= 5; i++) {
      GeneralRange<Integer> range = GeneralRange.range(Ordering.natural(), i, OPEN, i, CLOSED);
      for (int j = 1; j <= 5; j++) {
        assertFalse(range.contains(j));
      }
    }
  }

  public void testCreateSingletonRangeSucceeds() {
    for (int i = 1; i <= 5; i++) {
      GeneralRange<Integer> range = GeneralRange.range(Ordering.natural(), i, CLOSED, i, CLOSED);
      for (int j = 1; j <= 5; j++) {
        assertEquals(i == j, range.contains(j));
      }
    }
  }

  public void testSingletonRange() {
    GeneralRange<Integer> range = GeneralRange.range(Ordering.natural(), 3, CLOSED, 3, CLOSED);
    for (int i = 1; i <= 5; i++) {
      assertEquals(i == 3, range.contains(i));
    }
  }

  public void testLowerRange() {
    for (BoundType lBoundType : BoundType.values()) {
      GeneralRange<Integer> range = GeneralRange.downTo(Ordering.natural(), 3, lBoundType);
      for (int i = 1; i <= 5; i++) {
        assertEquals(i > 3 || (i == 3 && lBoundType == CLOSED), range.contains(i));
        assertEquals(i < 3 || (i == 3 && lBoundType == OPEN), range.tooLow(i));
        assertFalse(range.tooHigh(i));
      }
    }
  }

  public void testUpperRange() {
    for (BoundType lBoundType : BoundType.values()) {
      GeneralRange<Integer> range = GeneralRange.upTo(Ordering.natural(), 3, lBoundType);
      for (int i = 1; i <= 5; i++) {
        assertEquals(i < 3 || (i == 3 && lBoundType == CLOSED), range.contains(i));
        assertEquals(i > 3 || (i == 3 && lBoundType == OPEN), range.tooHigh(i));
        assertFalse(range.tooLow(i));
      }
    }
  }

  public void testDoublyBoundedAgainstRange() {
    for (BoundType lboundType : BoundType.values()) {
      for (BoundType uboundType : BoundType.values()) {
        Range<Integer> range = Ranges.range(2, lboundType, 4, uboundType);
        GeneralRange<Integer> gRange =
            GeneralRange.range(Ordering.natural(), 2, lboundType, 4, uboundType);
        for (int i = 1; i <= 5; i++) {
          assertEquals(range.contains(i), gRange.contains(i));
        }
      }
    }
  }

  public void testIntersectAgainstMatchingEndpointsRange() {
    GeneralRange<Integer> range = GeneralRange.range(Ordering.natural(), 2, CLOSED, 4, OPEN);
    assertEquals(GeneralRange.range(Ordering.natural(), 2, OPEN, 4, OPEN),
        range.intersect(GeneralRange.range(Ordering.natural(), 2, OPEN, 4, CLOSED)));
  }

  public void testIntersectAgainstBiggerRange() {
    GeneralRange<Integer> range = GeneralRange.range(Ordering.natural(), 2, CLOSED, 4, OPEN);

    assertEquals(GeneralRange.range(Ordering.natural(), 2, CLOSED, 4, OPEN),
        range.intersect(GeneralRange.range(Ordering.natural(), 1, OPEN, 5, CLOSED)));

    assertEquals(GeneralRange.range(Ordering.natural(), 2, OPEN, 4, OPEN),
        range.intersect(GeneralRange.range(Ordering.natural(), 2, OPEN, 5, CLOSED)));

    assertEquals(GeneralRange.range(Ordering.natural(), 2, CLOSED, 4, OPEN),
        range.intersect(GeneralRange.range(Ordering.natural(), 1, OPEN, 4, OPEN)));
  }

  public void testIntersectAgainstSmallerRange() {
    GeneralRange<Integer> range = GeneralRange.range(Ordering.natural(), 2, OPEN, 4, OPEN);
    assertEquals(GeneralRange.range(Ordering.natural(), 3, CLOSED, 4, OPEN),
        range.intersect(GeneralRange.range(Ordering.natural(), 3, CLOSED, 4, CLOSED)));
  }

  public void testIntersectOverlappingRange() {
    GeneralRange<Integer> range = GeneralRange.range(Ordering.natural(), 2, OPEN, 4, CLOSED);
    assertEquals(GeneralRange.range(Ordering.natural(), 3, CLOSED, 4, CLOSED),
        range.intersect(GeneralRange.range(Ordering.natural(), 3, CLOSED, 5, CLOSED)));
    assertEquals(GeneralRange.range(Ordering.natural(), 2, OPEN, 3, OPEN),
        range.intersect(GeneralRange.range(Ordering.natural(), 1, OPEN, 3, OPEN)));
  }

  public void testIntersectNonOverlappingRange() {
    GeneralRange<Integer> range = GeneralRange.range(Ordering.natural(), 2, OPEN, 4, CLOSED);
    assertTrue(
        range.intersect(GeneralRange.range(Ordering.natural(), 5, CLOSED, 6, CLOSED)).isEmpty());
    assertTrue(
        range.intersect(GeneralRange.range(Ordering.natural(), 1, OPEN, 2, OPEN)).isEmpty());
  }

  public void testFromRangeAll() {
    assertEquals(GeneralRange.all(Ordering.natural()), GeneralRange.from(Ranges.all()));
  }

  public void testFromRangeOneEnd() {
    for (BoundType endpointType : BoundType.values()) {
      assertEquals(GeneralRange.upTo(Ordering.natural(), 3, endpointType),
          GeneralRange.from(Ranges.upTo(3, endpointType)));

      assertEquals(GeneralRange.downTo(Ordering.natural(), 3, endpointType),
          GeneralRange.from(Ranges.downTo(3, endpointType)));
    }
  }

  public void testFromRangeTwoEnds() {
    for (BoundType lowerType : BoundType.values()) {
      for (BoundType upperType : BoundType.values()) {
        assertEquals(GeneralRange.range(Ordering.natural(), 3, lowerType, 4, upperType),
            GeneralRange.from(Ranges.range(3, lowerType, 4, upperType)));
      }
    }
  }

  public void testReverse() {
    assertEquals(GeneralRange.all(Ordering.natural().reverse()),
        GeneralRange.all(Ordering.natural()).reverse());
    assertEquals(GeneralRange.downTo(Ordering.natural().reverse(), 3, CLOSED),
        GeneralRange.upTo(Ordering.natural(), 3, CLOSED).reverse());
    assertEquals(GeneralRange.upTo(Ordering.natural().reverse(), 3, OPEN),
        GeneralRange.downTo(Ordering.natural(), 3, OPEN).reverse());
    assertEquals(GeneralRange.range(Ordering.natural().reverse(), 5, OPEN, 3, CLOSED),
        GeneralRange.range(Ordering.natural(), 3, CLOSED, 5, OPEN).reverse());
  }

  @GwtIncompatible("NullPointerTester")
  public void testNullPointers() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(Comparator.class, Ordering.arbitrary());
    tester.setDefault(BoundType.class, BoundType.CLOSED);
    tester.testAllPublicStaticMethods(GeneralRange.class);
  }
}
