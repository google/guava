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

import static com.google.common.collect.BoundType.OPEN;
import static com.google.common.collect.Ranges.range;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtIncompatible;

/**
 * Tests for {@link TreeRangeSet}.
 *
 * @author Louis Wasserman
 * @author Chris Povirk
 */
@GwtIncompatible("TreeRangeSet")
public class TreeRangeSetTest extends AbstractRangeSetTest {
  // TODO(cpovirk): test all of these with the ranges added in the reverse order

  public void testMergesConnectedWithOverlap() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(1, 4));
    rangeSet.add(Ranges.open(2, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closedOpen(1, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(1),
        Ranges.atLeast(6));
  }

  public void testMergesConnectedDisjoint() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(1, 4));
    rangeSet.add(Ranges.open(4, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closedOpen(1, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(1),
        Ranges.atLeast(6));
  }

  public void testIgnoresSmallerSharingNoBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(1, 6));
    rangeSet.add(Ranges.open(2, 4));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(1),
        Ranges.greaterThan(6));
  }

  public void testIgnoresSmallerSharingLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(1, 6));
    rangeSet.add(Ranges.closed(1, 4));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(1),
        Ranges.greaterThan(6));
  }

  public void testIgnoresSmallerSharingUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(1, 6));
    rangeSet.add(Ranges.closed(3, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(1),
        Ranges.greaterThan(6));
  }

  public void testIgnoresEqual() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(1, 6));
    rangeSet.add(Ranges.closed(1, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(1),
        Ranges.greaterThan(6));
  }

  public void testExtendSameLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(1, 4));
    rangeSet.add(Ranges.closed(1, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(1),
        Ranges.greaterThan(6));
  }

  public void testExtendSameUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 6));
    rangeSet.add(Ranges.closed(1, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(1),
        Ranges.greaterThan(6));
  }

  public void testExtendBothDirections() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 4));
    rangeSet.add(Ranges.closed(1, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(1),
        Ranges.greaterThan(6));
  }

  public void testAddEmpty() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closedOpen(3, 3));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).isEmpty();
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.<Integer>all());
  }

  public void testFillHoleExactly() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closedOpen(1, 3));
    rangeSet.add(Ranges.closedOpen(4, 6));
    rangeSet.add(Ranges.closedOpen(3, 4));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closedOpen(1, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(1),
        Ranges.atLeast(6));
  }

  public void testFillHoleWithOverlap() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closedOpen(1, 3));
    rangeSet.add(Ranges.closedOpen(4, 6));
    rangeSet.add(Ranges.closedOpen(2, 5));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closedOpen(1, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(1),
        Ranges.atLeast(6));
  }

  public void testAddManyPairs() {
    for (int aLow = 0; aLow < 6; aLow++) {
      for (int aHigh = 0; aHigh < 6; aHigh++) {
        for (BoundType aLowType : BoundType.values()) {
          for (BoundType aHighType : BoundType.values()) {
            if ((aLow == aHigh && aLowType == OPEN && aHighType == OPEN) || aLow > aHigh) {
              continue;
            }
            for (int bLow = 0; bLow < 6; bLow++) {
              for (int bHigh = 0; bHigh < 6; bHigh++) {
                for (BoundType bLowType : BoundType.values()) {
                  for (BoundType bHighType : BoundType.values()) {
                    if ((bLow == bHigh && bLowType == OPEN && bHighType == OPEN) || bLow > bHigh) {
                      continue;
                    }
                    doPairTest(range(aLow, aLowType, aHigh, aHighType),
                        range(bLow, bLowType, bHigh, bHighType));
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private static void doPairTest(Range<Integer> a, Range<Integer> b) {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(a);
    rangeSet.add(b);
    if (a.isEmpty() && b.isEmpty()) {
      ASSERT.that(rangeSet.asRanges()).isEmpty();
    } else if (a.isEmpty()) {
      ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(b);
    } else if (b.isEmpty()) {
      ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(a);
    } else if (a.isConnected(b)) {
      ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(a.span(b));
    } else {
      if (a.lowerEndpoint() < b.lowerEndpoint()) {
        ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(a, b);
      } else {
        ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(b, a);
      }
    }
  }

  public void testRemoveEmpty() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(1, 6));
    rangeSet.remove(Ranges.closedOpen(3, 3));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(1),
        Ranges.greaterThan(6));
  }

  public void testRemovePartSharingLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 5));
    rangeSet.remove(Ranges.closedOpen(3, 5));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.singleton(5));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(5),
        Ranges.greaterThan(5));
  }

  public void testRemovePartSharingUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 5));
    rangeSet.remove(Ranges.openClosed(3, 5));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.singleton(3));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.lessThan(3),
        Ranges.greaterThan(3));
  }

  public void testRemoveMiddle() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.atMost(6));
    rangeSet.remove(Ranges.closedOpen(3, 4));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.lessThan(3), Ranges.closed(4, 6));
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Ranges.closedOpen(3, 4),
        Ranges.greaterThan(6));
  }

  public void testRemoveNoOverlap() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 6));
    rangeSet.remove(Ranges.closedOpen(1, 3));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closed(3, 6));
  }

  public void testRemovePartFromBelowLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 6));
    rangeSet.remove(Ranges.closed(1, 3));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.openClosed(3, 6));
  }

  public void testRemovePartFromAboveUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 6));
    rangeSet.remove(Ranges.closed(6, 9));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Ranges.closedOpen(3, 6));
  }

  public void testRemoveExact() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 6));
    rangeSet.remove(Ranges.closed(3, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).isEmpty();
  }

  public void testRemoveAllFromBelowLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 6));
    rangeSet.remove(Ranges.closed(2, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).isEmpty();
  }

  public void testRemoveAllFromAboveUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 6));
    rangeSet.remove(Ranges.closed(3, 7));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).isEmpty();
  }

  public void testRemoveAllExtendingBothDirections() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 6));
    rangeSet.remove(Ranges.closed(2, 7));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).isEmpty();
  }

  public void testRangeContaining1() {
    RangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 10));
    assertEquals(Ranges.closed(3, 10), rangeSet.rangeContaining(5));
    assertTrue(rangeSet.contains(5));
    assertNull(rangeSet.rangeContaining(1));
    assertFalse(rangeSet.contains(1));
  }

  public void testRangeContaining2() {
    RangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Ranges.closed(3, 10));
    rangeSet.remove(Ranges.open(5, 7));
    assertEquals(Ranges.closed(3, 5), rangeSet.rangeContaining(5));
    assertTrue(rangeSet.contains(5));
    assertEquals(Ranges.closed(7, 10), rangeSet.rangeContaining(8));
    assertTrue(rangeSet.contains(8));
    assertNull(rangeSet.rangeContaining(6));
    assertFalse(rangeSet.contains(6));
  }
}
