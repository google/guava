/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 'b'.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-'b'.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstSide.RIGHT;
import static com.google.common.collect.BstTesting.defaultNullPointerTester;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.BstTesting.SimpleNode;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@code BstNode}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class BstNodeTest extends TestCase {
  private void testLacksChild(SimpleNode node, BstSide side) {
    assertNull(node.childOrNull(side));
    assertFalse(node.hasChild(side));
    try {
      node.getChild(side);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {}
  }

  private void testChildIs(SimpleNode node, BstSide side, SimpleNode expectedChild) {
    assertEquals(expectedChild, node.childOrNull(side));
    assertTrue(node.hasChild(side));
    assertEquals(expectedChild, node.getChild(side));
  }

  public void testHasChildLeaf() {
    SimpleNode leaf = new SimpleNode('a', null, null);
    testLacksChild(leaf, LEFT);
    testLacksChild(leaf, RIGHT);
  }

  public void testHasChildLeftOnly() {
    SimpleNode leaf = new SimpleNode('a', null, null);
    SimpleNode node = new SimpleNode('b', leaf, null);
    testChildIs(node, LEFT, leaf);
    testLacksChild(node, RIGHT);
  }

  public void testHasChildRightOnly() {
    SimpleNode leaf = new SimpleNode('c', null, null);
    SimpleNode node = new SimpleNode('b', null, leaf);
    testLacksChild(node, LEFT);
    testChildIs(node, RIGHT, leaf);
  }

  public void testHasChildBoth() {
    SimpleNode left = new SimpleNode('a', null, null);
    SimpleNode right = new SimpleNode('c', null, null);
    SimpleNode node = new SimpleNode('b', left, right);
    testChildIs(node, LEFT, left);
    testChildIs(node, RIGHT, right);
  }

  private static final char MIDDLE_KEY = 'b';

  private static final List<SimpleNode> GOOD_LEFTS =
      Arrays.asList(null, new SimpleNode('a', null, null));
  private static final List<SimpleNode> BAD_LEFTS =
      Arrays.asList(new SimpleNode('b', null, null), new SimpleNode('c', null, null));
  private static final Iterable<SimpleNode> ALL_LEFTS = Iterables.concat(GOOD_LEFTS, BAD_LEFTS);

  private static final List<SimpleNode> GOOD_RIGHTS =
      Arrays.asList(null, new SimpleNode('c', null, null));
  private static final List<SimpleNode> BAD_RIGHTS =
      Arrays.asList(new SimpleNode('b', null, null), new SimpleNode('a', null, null));
  private static final Iterable<SimpleNode> ALL_RIGHTS = Iterables.concat(GOOD_RIGHTS, BAD_RIGHTS);

  public void testOrderingInvariantHoldsForGood() {
    for (SimpleNode left : GOOD_LEFTS) {
      for (SimpleNode right : GOOD_RIGHTS) {
        assertTrue(
            new SimpleNode(MIDDLE_KEY, left, right).orderingInvariantHolds(Ordering.natural()));
      }
    }
  }

  public void testOrderingInvariantBadLeft() {
    for (SimpleNode left : BAD_LEFTS) {
      for (SimpleNode right : ALL_RIGHTS) {
        assertFalse(
            new SimpleNode(MIDDLE_KEY, left, right).orderingInvariantHolds(Ordering.natural()));
      }
    }
  }

  public void testOrderingInvariantBadRight() {
    for (SimpleNode left : ALL_LEFTS) {
      for (SimpleNode right : BAD_RIGHTS) {
        assertFalse(
            new SimpleNode(MIDDLE_KEY, left, right).orderingInvariantHolds(Ordering.natural()));
      }
    }
  }

  @GwtIncompatible("NullPointerTester")
  public void testNullPointers() throws Exception {
    defaultNullPointerTester().testAllPublicStaticMethods(BstNode.class);
  }
}
