/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.collect.BoundType.OPEN;
import static com.google.common.collect.Range.range;
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

  private static final ImmutableList<Range<Integer>> QUERY_RANGES;

  static {
    ImmutableList.Builder<Range<Integer>> queryBuilder = ImmutableList.builder();

    queryBuilder.add(Range.<Integer>all());

    for (int i = 0; i <= 4; i++) {
      for (BoundType boundType : BoundType.values()) {
        queryBuilder.add(Range.upTo(i, boundType));
        queryBuilder.add(Range.downTo(i, boundType));
      }
      queryBuilder.add(Range.singleton(i));
      queryBuilder.add(Range.openClosed(i, i));
      queryBuilder.add(Range.closedOpen(i, i));

      for (BoundType lowerBoundType : BoundType.values()) {
        for (int j = i + 1; j <= 4; j++) {
          for (BoundType upperBoundType : BoundType.values()) {
            queryBuilder.add(Range.range(i, lowerBoundType, j, upperBoundType));
          }
        }
      }
    }
    QUERY_RANGES = queryBuilder.build();
  }

  public void testEnclosing(RangeSet<Integer> rangeSet) {
    for (Range<Integer> query : QUERY_RANGES) {
      boolean expectEnclose = false;
      for (Range<Integer> expectedRange : rangeSet.asRanges()) {
        if (expectedRange.encloses(query)) {
          expectEnclose = true;
          break;
        }
      }

      assertEquals(rangeSet + " was incorrect on encloses(" + query + ")", expectEnclose,
          rangeSet.encloses(query));
    }
  }

  public void testAllSingleRangesComplementAgainstRemove() {
    for (Range<Integer> range : QUERY_RANGES) {
      TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
      rangeSet.add(range);

      TreeRangeSet<Integer> complement = TreeRangeSet.create();
      complement.add(Range.<Integer>all());
      complement.remove(range);

      assertEquals(complement, rangeSet.complement());
      ASSERT.that(rangeSet.complement().asRanges())
          .hasContentsInOrder(complement.asRanges().toArray());
    }
  }

  public void testInvariantsEmpty() {
    testInvariants(TreeRangeSet.create());
  }

  public void testAllSingleRangesEnclosing() {
    for (Range<Integer> range : QUERY_RANGES) {
      TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
      rangeSet.add(range);
      testEnclosing(rangeSet);
      testEnclosing(rangeSet.complement());
    }
  }

  public void testAllTwoRangesEnclosing() {
    for (Range<Integer> range1 : QUERY_RANGES) {
      for (Range<Integer> range2 : QUERY_RANGES) {
        TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
        rangeSet.add(range1);
        rangeSet.add(range2);
        testEnclosing(rangeSet);
        testEnclosing(rangeSet.complement());
      }
    }
  }

  public void testCreateCopy() {
    for (Range<Integer> range1 : QUERY_RANGES) {
      for (Range<Integer> range2 : QUERY_RANGES) {
        TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
        rangeSet.add(range1);
        rangeSet.add(range2);
        
        assertEquals(rangeSet, TreeRangeSet.create(rangeSet));
      }
    }
  }

  public void testMergesConnectedWithOverlap() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 4));
    rangeSet.add(Range.open(2, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closedOpen(1, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(1), Range.atLeast(6));
  }

  public void testMergesConnectedDisjoint() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 4));
    rangeSet.add(Range.open(4, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closedOpen(1, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(1), Range.atLeast(6));
  }

  public void testIgnoresSmallerSharingNoBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 6));
    rangeSet.add(Range.open(2, 4));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(1), Range.greaterThan(6));
  }

  public void testIgnoresSmallerSharingLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 6));
    rangeSet.add(Range.closed(1, 4));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(1), Range.greaterThan(6));
  }

  public void testIgnoresSmallerSharingUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 6));
    rangeSet.add(Range.closed(3, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(1), Range.greaterThan(6));
  }

  public void testIgnoresEqual() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 6));
    rangeSet.add(Range.closed(1, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(1), Range.greaterThan(6));
  }

  public void testExtendSameLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 4));
    rangeSet.add(Range.closed(1, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(1), Range.greaterThan(6));
  }

  public void testExtendSameUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.add(Range.closed(1, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(1), Range.greaterThan(6));
  }

  public void testExtendBothDirections() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 4));
    rangeSet.add(Range.closed(1, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(1), Range.greaterThan(6));
  }

  public void testAddEmpty() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closedOpen(3, 3));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).isEmpty();
    ASSERT.that(rangeSet.complement().asRanges()).hasContentsInOrder(Range.<Integer>all());
  }

  public void testFillHoleExactly() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closedOpen(1, 3));
    rangeSet.add(Range.closedOpen(4, 6));
    rangeSet.add(Range.closedOpen(3, 4));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closedOpen(1, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(1), Range.atLeast(6));
  }

  public void testFillHoleWithOverlap() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closedOpen(1, 3));
    rangeSet.add(Range.closedOpen(4, 6));
    rangeSet.add(Range.closedOpen(2, 5));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closedOpen(1, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(1), Range.atLeast(6));
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
    rangeSet.add(Range.closed(1, 6));
    rangeSet.remove(Range.closedOpen(3, 3));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closed(1, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(1), Range.greaterThan(6));
  }

  public void testRemovePartSharingLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 5));
    rangeSet.remove(Range.closedOpen(3, 5));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.singleton(5));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(5), Range.greaterThan(5));
  }

  public void testRemovePartSharingUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 5));
    rangeSet.remove(Range.openClosed(3, 5));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.singleton(3));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.lessThan(3), Range.greaterThan(3));
  }

  public void testRemoveMiddle() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.atMost(6));
    rangeSet.remove(Range.closedOpen(3, 4));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.lessThan(3), Range.closed(4, 6));
    ASSERT.that(rangeSet.complement().asRanges())
        .hasContentsInOrder(Range.closedOpen(3, 4), Range.greaterThan(6));
  }

  public void testRemoveNoOverlap() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closedOpen(1, 3));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closed(3, 6));
  }

  public void testRemovePartFromBelowLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closed(1, 3));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.openClosed(3, 6));
  }

  public void testRemovePartFromAboveUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closed(6, 9));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closedOpen(3, 6));
  }

  public void testRemoveExact() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closed(3, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).isEmpty();
  }

  public void testRemoveAllFromBelowLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closed(2, 6));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).isEmpty();
  }

  public void testRemoveAllFromAboveUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closed(3, 7));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).isEmpty();
  }

  public void testRemoveAllExtendingBothDirections() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closed(2, 7));
    testInvariants(rangeSet);
    ASSERT.that(rangeSet.asRanges()).isEmpty();
  }

  public void testRangeContaining1() {
    RangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 10));
    assertEquals(Range.closed(3, 10), rangeSet.rangeContaining(5));
    assertTrue(rangeSet.contains(5));
    assertNull(rangeSet.rangeContaining(1));
    assertFalse(rangeSet.contains(1));
  }

  public void testRangeContaining2() {
    RangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 10));
    rangeSet.remove(Range.open(5, 7));
    assertEquals(Range.closed(3, 5), rangeSet.rangeContaining(5));
    assertTrue(rangeSet.contains(5));
    assertEquals(Range.closed(7, 10), rangeSet.rangeContaining(8));
    assertTrue(rangeSet.contains(8));
    assertNull(rangeSet.rangeContaining(6));
    assertFalse(rangeSet.contains(6));
  }
}
