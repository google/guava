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
import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstSide.RIGHT;
import static com.google.common.collect.BstTesting.assertInOrderTraversalIs;
import static com.google.common.collect.BstTesting.balancePolicy;
import static com.google.common.collect.BstTesting.countAggregate;
import static com.google.common.collect.BstTesting.defaultNullPointerTester;
import static com.google.common.collect.BstTesting.nodeFactory;
import static com.google.common.collect.BstTesting.pathFactory;
import static com.google.common.collect.BstTesting.pathToList;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.BstTesting.SimpleNode;

import junit.framework.TestCase;

import java.util.SortedSet;

/**
 * Tests for {@code BSTRangeOps}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class BstRangeOpsTest extends TestCase {
  private static final SortedSet<Character> MODEL =
      ImmutableSortedSet.of('a', 'b', 'c', 'd', 'e', 'f', 'g');
  private static final SimpleNode ROOT;

  static {
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', a, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);
    ROOT = d;
  }

  public void testCountInRangeLowerBound() {
    for (char c : "abcdefg".toCharArray()) {
      for (BoundType type : BoundType.values()) {
        long count = BstRangeOps.totalInRange(
            countAggregate, GeneralRange.downTo(Ordering.natural(), c, type), ROOT);
        char d = c;
        if (type == BoundType.OPEN) {
          d++;
        }
        assertEquals(MODEL.tailSet(d).size(), count);
      }
    }
  }

  public void testCountInRangeUpperBound() {
    for (char c : "abcdefg".toCharArray()) {
      for (BoundType type : BoundType.values()) {
        long count = BstRangeOps.totalInRange(
            countAggregate, GeneralRange.upTo(Ordering.natural(), c, type), ROOT);
        char d = c;
        if (type == BoundType.CLOSED) {
          d++;
        }
        assertEquals(MODEL.headSet(d).size(), count);
      }
    }
  }

  public void testCountInRangeBothBounds() {
    String chars = "abcdefg";
    for (int i = 0; i < chars.length(); i++) {
      for (BoundType lb : BoundType.values()) {
        for (int j = i; j < chars.length(); j++) {
          for (BoundType ub : BoundType.values()) {
            if (i == j && lb == BoundType.OPEN && ub == BoundType.OPEN) {
              continue;
            }
            long count = BstRangeOps.totalInRange(countAggregate, GeneralRange.range(
                Ordering.natural(), chars.charAt(i), lb, chars.charAt(j), ub), ROOT);
            char lo = chars.charAt(i);
            if (lb == BoundType.OPEN) {
              lo++;
            }
            char hi = chars.charAt(j);
            if (ub == BoundType.CLOSED) {
              hi++;
            }
            if (lo > hi) {
              lo = hi;
            }
            assertEquals(MODEL.subSet(lo, hi).size(), count);
          }
        }
      }
    }
  }

  public void testCountInRangeAll() {
    assertEquals(MODEL.size(), BstRangeOps.totalInRange(
        countAggregate, GeneralRange.<Character>all(Ordering.natural()), ROOT));
  }

  public void testCountInRangeEmpty() {
    SimpleNode empty = null;
    GeneralRange<Character> range = GeneralRange.all(Ordering.natural());
    assertEquals(0, BstRangeOps.totalInRange(countAggregate, range, empty));
  }

  public void testClearRangeLowerBound() {
    //     d
    //    / \
    //   b   f
    //  /   / \
    //  a   e g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode b = new SimpleNode('b', a, null);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);

    assertInOrderTraversalIs(d, "abdefg");
    GeneralRange<Character> range1 = GeneralRange.downTo(Ordering.natural(), 'f', CLOSED);
    testTraversalAfterClearingRangeIs(d, range1, "abde");

    GeneralRange<Character> range2 = GeneralRange.downTo(Ordering.natural(), 'f', OPEN);
    testTraversalAfterClearingRangeIs(d, range2, "abdef");

    GeneralRange<Character> range3 = GeneralRange.downTo(Ordering.natural(), 'a', CLOSED);
    testTraversalAfterClearingRangeIs(d, range3, "");

    GeneralRange<Character> range4 = GeneralRange.downTo(Ordering.natural(), 'a', OPEN);
    testTraversalAfterClearingRangeIs(d, range4, "a");

    GeneralRange<Character> range5 = GeneralRange.downTo(Ordering.natural(), 'c', OPEN);
    testTraversalAfterClearingRangeIs(d, range5, "ab");

    GeneralRange<Character> range6 = GeneralRange.downTo(Ordering.natural(), 'c', CLOSED);
    testTraversalAfterClearingRangeIs(d, range6, "ab");
  }

  public void testClearRangeUpperBound() {
    //     d
    //    / \
    //   b   f
    //  /   / \
    //  a   e g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode b = new SimpleNode('b', a, null);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);

    assertInOrderTraversalIs(d, "abdefg");
    GeneralRange<Character> range1 = GeneralRange.upTo(Ordering.natural(), 'f', CLOSED);
    testTraversalAfterClearingRangeIs(d, range1, "g");

    GeneralRange<Character> range2 = GeneralRange.upTo(Ordering.natural(), 'f', OPEN);
    testTraversalAfterClearingRangeIs(d, range2, "fg");

    GeneralRange<Character> range3 = GeneralRange.upTo(Ordering.natural(), 'a', CLOSED);
    testTraversalAfterClearingRangeIs(d, range3, "bdefg");

    GeneralRange<Character> range4 = GeneralRange.upTo(Ordering.natural(), 'a', OPEN);
    testTraversalAfterClearingRangeIs(d, range4, "abdefg");

    GeneralRange<Character> range5 = GeneralRange.upTo(Ordering.natural(), 'c', OPEN);
    testTraversalAfterClearingRangeIs(d, range5, "defg");

    GeneralRange<Character> range6 = GeneralRange.upTo(Ordering.natural(), 'c', CLOSED);
    testTraversalAfterClearingRangeIs(d, range6, "defg");
  }

  public void testClearRangeDoublyBounded() {
    //     d
    //    / \
    //   b   f
    //  / \ / \
    //  a c e g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', a, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);

    GeneralRange<Character> range1 =
        GeneralRange.range(Ordering.natural(), 'c', OPEN, 'f', CLOSED);
    testTraversalAfterClearingRangeIs(d, range1, "abcg");

    GeneralRange<Character> range2 =
        GeneralRange.range(Ordering.natural(), 'a', CLOSED, 'h', OPEN);
    testTraversalAfterClearingRangeIs(d, range2, "");

  }

  public void testClearRangeAll() {
    //     d
    //    / \
    //   b   f
    //  / \ / \
    //  a c e g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', a, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);

    testTraversalAfterClearingRangeIs(d, GeneralRange.<Character>all(Ordering.natural()), "");
  }

  private void testTraversalAfterClearingRangeIs(
      SimpleNode d, GeneralRange<Character> range, String expected) {
    assertInOrderTraversalIs(
        BstRangeOps.minusRange(range, balancePolicy, nodeFactory, d), expected);
  }

  public void testLeftmostPathAll() {
    //     d
    //    / \
    //   b   f
    //    \ / \
    //    c e g
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);

    GeneralRange<Character> range1 = GeneralRange.all(Ordering.natural());
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range1, LEFT, pathFactory, d)))
        .hasContentsInOrder(b, d);

    assertNull(BstRangeOps.furthestPath(range1, LEFT, pathFactory, null));
  }

  public void testLeftmostPathDownTo() {
    //     d
    //    / \
    //   b   f
    //    \ / \
    //    c e g
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);

    GeneralRange<Character> range1 = GeneralRange.downTo(Ordering.natural(), 'd', OPEN);
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range1, LEFT, pathFactory, d)))
        .hasContentsInOrder(e, f, d);

    GeneralRange<Character> range2 = GeneralRange.downTo(Ordering.natural(), 'd', CLOSED);
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range2, LEFT, pathFactory, d)))
        .hasContentsInOrder(d);

    GeneralRange<Character> range3 = GeneralRange.downTo(Ordering.natural(), 'a', CLOSED);
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range3, LEFT, pathFactory, d)))
        .hasContentsInOrder(b, d);

    GeneralRange<Character> range4 = GeneralRange.downTo(Ordering.natural(), 'h', CLOSED);
    assertNull(BstRangeOps.furthestPath(range4, LEFT, pathFactory, d));
  }

  public void testLeftmostPathUpTo() {
    //     d
    //    / \
    //   b   f
    //    \ / \
    //    c e g
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);

    GeneralRange<Character> range1 = GeneralRange.upTo(Ordering.natural(), 'd', OPEN);
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range1, LEFT, pathFactory, d)))
        .hasContentsInOrder(b, d);

    GeneralRange<Character> range2 = GeneralRange.upTo(Ordering.natural(), 'd', CLOSED);
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range2, LEFT, pathFactory, d)))
        .hasContentsInOrder(b, d);

    GeneralRange<Character> range3 = GeneralRange.upTo(Ordering.natural(), 'a', CLOSED);
    assertNull(BstRangeOps.furthestPath(range3, LEFT, pathFactory, d));
  }

  public void testRightmostPathAll() {
    //     d
    //    / \
    //   b   f
    //    \ / \
    //    c e g
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);

    GeneralRange<Character> range1 = GeneralRange.all(Ordering.natural());
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range1, RIGHT, pathFactory, d)))
        .hasContentsInOrder(g, f, d);

    assertNull(BstRangeOps.furthestPath(range1, RIGHT, pathFactory, null));
  }

  public void testRightmostPathDownTo() {
    //     d
    //    / \
    //   b   f
    //    \ / \
    //    c e g
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);

    GeneralRange<Character> range1 = GeneralRange.downTo(Ordering.natural(), 'd', OPEN);
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range1, RIGHT, pathFactory, d)))
        .hasContentsInOrder(g, f, d);

    GeneralRange<Character> range2 = GeneralRange.downTo(Ordering.natural(), 'd', CLOSED);
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range2, RIGHT, pathFactory, d)))
        .hasContentsInOrder(g, f, d);

    GeneralRange<Character> range3 = GeneralRange.downTo(Ordering.natural(), 'a', CLOSED);
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range3, RIGHT, pathFactory, d)))
        .hasContentsInOrder(g, f, d);

    GeneralRange<Character> range4 = GeneralRange.downTo(Ordering.natural(), 'h', CLOSED);
    assertNull(BstRangeOps.furthestPath(range4, RIGHT, pathFactory, d));
  }

  public void testRightmostPathUpTo() {
    //     d
    //    / \
    //   b   f
    //    \ / \
    //    c e g
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);

    GeneralRange<Character> range1 = GeneralRange.upTo(Ordering.natural(), 'd', OPEN);
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range1, RIGHT, pathFactory, d)))
        .hasContentsInOrder(c, b, d);

    GeneralRange<Character> range2 = GeneralRange.upTo(Ordering.natural(), 'd', CLOSED);
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range2, RIGHT, pathFactory, d)))
        .hasContentsInOrder(d);

    GeneralRange<Character> range3 = GeneralRange.upTo(Ordering.natural(), 'a', CLOSED);
    assertNull(BstRangeOps.furthestPath(range3, RIGHT, pathFactory, d));

    GeneralRange<Character> range4 = GeneralRange.upTo(Ordering.natural(), 'h', CLOSED);
    ASSERT.that(pathToList(BstRangeOps.furthestPath(range4, RIGHT, pathFactory, d)))
        .hasContentsInOrder(g, f, d);
  }

  @GwtIncompatible("NullPointerTester")
  public void testNullPointers() throws Exception {
    defaultNullPointerTester().testAllPublicStaticMethods(BstRangeOps.class);
  }
}
