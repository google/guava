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
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.SerializableTester;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;

/**
 * Tests for {@link TreeRangeSet}.
 *
 * @author Louis Wasserman
 * @author Chris Povirk
 */
@GwtIncompatible // TreeRangeSet
public class TreeRangeSetTest extends AbstractRangeSetTest {
  // TODO(cpovirk): test all of these with the ranges added in the reverse order

  private static final ImmutableList<Range<Integer>> QUERY_RANGES;

  private static final int MIN_BOUND = -1;
  private static final int MAX_BOUND = 1;

  static {
    ImmutableList.Builder<Range<Integer>> queryBuilder = ImmutableList.builder();

    queryBuilder.add(Range.<Integer>all());

    for (int i = MIN_BOUND; i <= MAX_BOUND; i++) {
      for (BoundType boundType : BoundType.values()) {
        queryBuilder.add(Range.upTo(i, boundType));
        queryBuilder.add(Range.downTo(i, boundType));
      }
      queryBuilder.add(Range.singleton(i));
      queryBuilder.add(Range.openClosed(i, i));
      queryBuilder.add(Range.closedOpen(i, i));

      for (BoundType lowerBoundType : BoundType.values()) {
        for (int j = i + 1; j <= MAX_BOUND; j++) {
          for (BoundType upperBoundType : BoundType.values()) {
            queryBuilder.add(Range.range(i, lowerBoundType, j, upperBoundType));
          }
        }
      }
    }
    QUERY_RANGES = queryBuilder.build();
  }

  void testViewAgainstExpected(RangeSet<Integer> expected, RangeSet<Integer> view) {
    assertEquals(expected, view);
    assertEquals(expected.asRanges(), view.asRanges());
    assertEquals(expected.isEmpty(), view.isEmpty());

    if (!expected.isEmpty()) {
      assertEquals(expected.span(), view.span());
    }

    for (int i = MIN_BOUND - 1; i <= MAX_BOUND + 1; i++) {
      assertEquals(expected.contains(i), view.contains(i));
      assertEquals(expected.rangeContaining(i), view.rangeContaining(i));
    }
    testEnclosing(view);
    if (view instanceof TreeRangeSet) {
      testRangesByLowerBounds((TreeRangeSet<Integer>) view, expected.asRanges());
    }
  }

  private static final ImmutableList<Cut<Integer>> CUTS_TO_TEST;

  static {
    List<Cut<Integer>> cutsToTest = Lists.newArrayList();
    for (int i = MIN_BOUND - 1; i <= MAX_BOUND + 1; i++) {
      cutsToTest.add(Cut.belowValue(i));
      cutsToTest.add(Cut.aboveValue(i));
    }
    cutsToTest.add(Cut.<Integer>aboveAll());
    cutsToTest.add(Cut.<Integer>belowAll());
    CUTS_TO_TEST = ImmutableList.copyOf(cutsToTest);
  }

  private void testRangesByLowerBounds(
      TreeRangeSet<Integer> rangeSet, Iterable<Range<Integer>> expectedRanges) {
    NavigableMap<Cut<Integer>, Range<Integer>> expectedRangesByLowerBound = Maps.newTreeMap();
    for (Range<Integer> range : expectedRanges) {
      expectedRangesByLowerBound.put(range.lowerBound, range);
    }

    NavigableMap<Cut<Integer>, Range<Integer>> rangesByLowerBound = rangeSet.rangesByLowerBound;
    testNavigationAgainstExpected(expectedRangesByLowerBound, rangesByLowerBound, CUTS_TO_TEST);
  }

  <K, V> void testNavigationAgainstExpected(
      NavigableMap<K, V> expected, NavigableMap<K, V> navigableMap, Iterable<K> keysToTest) {
    for (K key : keysToTest) {
      assertEquals(expected.lowerEntry(key), navigableMap.lowerEntry(key));
      assertEquals(expected.floorEntry(key), navigableMap.floorEntry(key));
      assertEquals(expected.ceilingEntry(key), navigableMap.ceilingEntry(key));
      assertEquals(expected.higherEntry(key), navigableMap.higherEntry(key));
      for (boolean inclusive : new boolean[] {false, true}) {
        assertThat(navigableMap.headMap(key, inclusive).entrySet())
            .containsExactlyElementsIn(expected.headMap(key, inclusive).entrySet())
            .inOrder();
        assertThat(navigableMap.tailMap(key, inclusive).entrySet())
            .containsExactlyElementsIn(expected.tailMap(key, inclusive).entrySet())
            .inOrder();
        assertThat(navigableMap.headMap(key, inclusive).descendingMap().entrySet())
            .containsExactlyElementsIn(expected.headMap(key, inclusive).descendingMap().entrySet())
            .inOrder();
        assertThat(navigableMap.tailMap(key, inclusive).descendingMap().entrySet())
            .containsExactlyElementsIn(expected.tailMap(key, inclusive).descendingMap().entrySet())
            .inOrder();
      }
    }
  }

  public void testIntersects(RangeSet<Integer> rangeSet) {
    for (Range<Integer> query : QUERY_RANGES) {
      boolean expectIntersect = false;
      for (Range<Integer> expectedRange : rangeSet.asRanges()) {
        if (expectedRange.isConnected(query) && !expectedRange.intersection(query).isEmpty()) {
          expectIntersect = true;
          break;
        }
      }
      assertEquals(
          rangeSet + " was incorrect on intersects(" + query + ")",
          expectIntersect,
          rangeSet.intersects(query));
    }
  }

  public void testEnclosing(RangeSet<Integer> rangeSet) {
    assertTrue(rangeSet.enclosesAll(ImmutableList.<Range<Integer>>of()));
    for (Range<Integer> query : QUERY_RANGES) {
      boolean expectEnclose = false;
      for (Range<Integer> expectedRange : rangeSet.asRanges()) {
        if (expectedRange.encloses(query)) {
          expectEnclose = true;
          break;
        }
      }

      assertEquals(
          rangeSet + " was incorrect on encloses(" + query + ")",
          expectEnclose,
          rangeSet.encloses(query));
      assertEquals(
          rangeSet + " was incorrect on enclosesAll([" + query + "])",
          expectEnclose,
          rangeSet.enclosesAll(ImmutableList.of(query)));
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
      assertThat(rangeSet.complement().asRanges())
          .containsExactlyElementsIn(complement.asRanges())
          .inOrder();
    }
  }

  public void testInvariantsEmpty() {
    testInvariants(TreeRangeSet.create());
  }

  public void testEmptyIntersecting() {
    testIntersects(TreeRangeSet.<Integer>create());
    testIntersects(TreeRangeSet.<Integer>create().complement());
  }

  public void testAllSingleRangesIntersecting() {
    for (Range<Integer> range : QUERY_RANGES) {
      TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
      rangeSet.add(range);
      testIntersects(rangeSet);
      testIntersects(rangeSet.complement());
    }
  }

  public void testAllTwoRangesIntersecting() {
    for (Range<Integer> range1 : QUERY_RANGES) {
      for (Range<Integer> range2 : QUERY_RANGES) {
        TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
        rangeSet.add(range1);
        rangeSet.add(range2);
        testIntersects(rangeSet);
        testIntersects(rangeSet.complement());
      }
    }
  }

  public void testEmptyEnclosing() {
    testEnclosing(TreeRangeSet.<Integer>create());
    testEnclosing(TreeRangeSet.<Integer>create().complement());
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

  private RangeSet<Integer> expectedSubRangeSet(
      RangeSet<Integer> rangeSet, Range<Integer> subRange) {
    RangeSet<Integer> expected = TreeRangeSet.create();
    for (Range<Integer> range : rangeSet.asRanges()) {
      if (range.isConnected(subRange)) {
        expected.add(range.intersection(subRange));
      }
    }
    return expected;
  }

  private RangeSet<Integer> expectedComplement(RangeSet<Integer> rangeSet) {
    RangeSet<Integer> expected = TreeRangeSet.create();
    expected.add(Range.<Integer>all());
    expected.removeAll(rangeSet);
    return expected;
  }


  public void testSubRangeSet() {
    for (Range<Integer> range1 : QUERY_RANGES) {
      for (Range<Integer> range2 : QUERY_RANGES) {
        TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
        rangeSet.add(range1);
        rangeSet.add(range2);
        for (Range<Integer> subRange : QUERY_RANGES) {
          testViewAgainstExpected(
              expectedSubRangeSet(rangeSet, subRange), rangeSet.subRangeSet(subRange));
        }
      }
    }
  }

  public void testSubRangeSetAdd() {
    TreeRangeSet<Integer> set = TreeRangeSet.create();
    Range<Integer> range = Range.closedOpen(0, 5);
    set.subRangeSet(range).add(range);
  }

  public void testSubRangeSetReplaceAdd() {
    TreeRangeSet<Integer> set = TreeRangeSet.create();
    Range<Integer> range = Range.closedOpen(0, 5);
    set.add(range);
    set.subRangeSet(range).add(range);
  }

  public void testComplement() {
    for (Range<Integer> range1 : QUERY_RANGES) {
      for (Range<Integer> range2 : QUERY_RANGES) {
        TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
        rangeSet.add(range1);
        rangeSet.add(range2);
        testViewAgainstExpected(expectedComplement(rangeSet), rangeSet.complement());
      }
    }
  }


  public void testSubRangeSetOfComplement() {
    for (Range<Integer> range1 : QUERY_RANGES) {
      for (Range<Integer> range2 : QUERY_RANGES) {
        TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
        rangeSet.add(range1);
        rangeSet.add(range2);
        for (Range<Integer> subRange : QUERY_RANGES) {
          testViewAgainstExpected(
              expectedSubRangeSet(expectedComplement(rangeSet), subRange),
              rangeSet.complement().subRangeSet(subRange));
        }
      }
    }
  }


  public void testComplementOfSubRangeSet() {
    for (Range<Integer> range1 : QUERY_RANGES) {
      for (Range<Integer> range2 : QUERY_RANGES) {
        TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
        rangeSet.add(range1);
        rangeSet.add(range2);
        for (Range<Integer> subRange : QUERY_RANGES) {
          testViewAgainstExpected(
              expectedComplement(expectedSubRangeSet(rangeSet, subRange)),
              rangeSet.subRangeSet(subRange).complement());
        }
      }
    }
  }

  public void testRangesByUpperBound() {
    for (Range<Integer> range1 : QUERY_RANGES) {
      for (Range<Integer> range2 : QUERY_RANGES) {
        TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
        rangeSet.add(range1);
        rangeSet.add(range2);

        NavigableMap<Cut<Integer>, Range<Integer>> expectedRangesByUpperBound = Maps.newTreeMap();
        for (Range<Integer> range : rangeSet.asRanges()) {
          expectedRangesByUpperBound.put(range.upperBound, range);
        }
        testNavigationAgainstExpected(
            expectedRangesByUpperBound,
            new TreeRangeSet.RangesByUpperBound<Integer>(rangeSet.rangesByLowerBound),
            CUTS_TO_TEST);
      }
    }
  }

  public void testMergesConnectedWithOverlap() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 4));
    rangeSet.add(Range.open(2, 6));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.closedOpen(1, 6));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(1), Range.atLeast(6))
        .inOrder();
  }

  public void testMergesConnectedDisjoint() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 4));
    rangeSet.add(Range.open(4, 6));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.closedOpen(1, 6));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(1), Range.atLeast(6))
        .inOrder();
  }

  public void testIgnoresSmallerSharingNoBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 6));
    rangeSet.add(Range.open(2, 4));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.closed(1, 6));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(1), Range.greaterThan(6))
        .inOrder();
  }

  public void testIgnoresSmallerSharingLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 6));
    rangeSet.add(Range.closed(1, 4));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.closed(1, 6));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(1), Range.greaterThan(6))
        .inOrder();
  }

  public void testIgnoresSmallerSharingUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 6));
    rangeSet.add(Range.closed(3, 6));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.closed(1, 6));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(1), Range.greaterThan(6))
        .inOrder();
  }

  public void testIgnoresEqual() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 6));
    rangeSet.add(Range.closed(1, 6));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.closed(1, 6));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(1), Range.greaterThan(6))
        .inOrder();
  }

  public void testExtendSameLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 4));
    rangeSet.add(Range.closed(1, 6));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.closed(1, 6));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(1), Range.greaterThan(6))
        .inOrder();
  }

  public void testExtendSameUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.add(Range.closed(1, 6));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.closed(1, 6));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(1), Range.greaterThan(6))
        .inOrder();
  }

  public void testExtendBothDirections() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 4));
    rangeSet.add(Range.closed(1, 6));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.closed(1, 6));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(1), Range.greaterThan(6))
        .inOrder();
  }

  public void testAddEmpty() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closedOpen(3, 3));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).isEmpty();
    assertThat(rangeSet.complement().asRanges()).containsExactly(Range.<Integer>all());
  }

  public void testFillHoleExactly() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closedOpen(1, 3));
    rangeSet.add(Range.closedOpen(4, 6));
    rangeSet.add(Range.closedOpen(3, 4));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.closedOpen(1, 6));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(1), Range.atLeast(6))
        .inOrder();
  }

  public void testFillHoleWithOverlap() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closedOpen(1, 3));
    rangeSet.add(Range.closedOpen(4, 6));
    rangeSet.add(Range.closedOpen(2, 5));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.closedOpen(1, 6));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(1), Range.atLeast(6))
        .inOrder();
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
                    doPairTest(
                        range(aLow, aLowType, aHigh, aHighType),
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
      assertThat(rangeSet.asRanges()).isEmpty();
    } else if (a.isEmpty()) {
      assertThat(rangeSet.asRanges()).contains(b);
    } else if (b.isEmpty()) {
      assertThat(rangeSet.asRanges()).contains(a);
    } else if (a.isConnected(b)) {
      assertThat(rangeSet.asRanges()).containsExactly(a.span(b));
    } else {
      if (a.lowerEndpoint() < b.lowerEndpoint()) {
        assertThat(rangeSet.asRanges()).containsExactly(a, b).inOrder();
      } else {
        assertThat(rangeSet.asRanges()).containsExactly(b, a).inOrder();
      }
    }
  }

  public void testRemoveEmpty() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(1, 6));
    rangeSet.remove(Range.closedOpen(3, 3));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.closed(1, 6));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(1), Range.greaterThan(6))
        .inOrder();
  }

  public void testRemovePartSharingLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 5));
    rangeSet.remove(Range.closedOpen(3, 5));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.singleton(5));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(5), Range.greaterThan(5))
        .inOrder();
  }

  public void testRemovePartSharingUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 5));
    rangeSet.remove(Range.openClosed(3, 5));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).contains(Range.singleton(3));
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.lessThan(3), Range.greaterThan(3))
        .inOrder();
  }

  public void testRemoveMiddle() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.atMost(6));
    rangeSet.remove(Range.closedOpen(3, 4));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges())
        .containsExactly(Range.lessThan(3), Range.closed(4, 6))
        .inOrder();
    assertThat(rangeSet.complement().asRanges())
        .containsExactly(Range.closedOpen(3, 4), Range.greaterThan(6))
        .inOrder();
  }

  public void testRemoveNoOverlap() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closedOpen(1, 3));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).containsExactly(Range.closed(3, 6));
  }

  public void testRemovePartFromBelowLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closed(1, 3));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).containsExactly(Range.openClosed(3, 6));
  }

  public void testRemovePartFromAboveUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closed(6, 9));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).containsExactly(Range.closedOpen(3, 6));
  }

  public void testRemoveExact() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closed(3, 6));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).isEmpty();
  }

  public void testRemoveAllFromBelowLowerBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closed(2, 6));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).isEmpty();
  }

  public void testRemoveAllFromAboveUpperBound() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closed(3, 7));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).isEmpty();
  }

  public void testRemoveAllExtendingBothDirections() {
    TreeRangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 6));
    rangeSet.remove(Range.closed(2, 7));
    testInvariants(rangeSet);
    assertThat(rangeSet.asRanges()).isEmpty();
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

  public void testAddAll() {
    RangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 10));
    rangeSet.addAll(Arrays.asList(Range.open(1, 3), Range.closed(5, 8), Range.closed(9, 11)));
    assertThat(rangeSet.asRanges()).containsExactly(Range.openClosed(1, 11)).inOrder();
  }

  public void testRemoveAll() {
    RangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 10));
    rangeSet.removeAll(Arrays.asList(Range.open(1, 3), Range.closed(5, 8), Range.closed(9, 11)));
    assertThat(rangeSet.asRanges())
        .containsExactly(Range.closedOpen(3, 5), Range.open(8, 9))
        .inOrder();
  }

  @GwtIncompatible // SerializableTester
  public void testSerialization() {
    RangeSet<Integer> rangeSet = TreeRangeSet.create();
    rangeSet.add(Range.closed(3, 10));
    rangeSet.remove(Range.open(5, 7));
    SerializableTester.reserializeAndAssert(rangeSet);
  }
}
