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

import static com.google.common.collect.BstTesting.assertInOrderTraversalIs;
import static com.google.common.collect.BstTesting.nodeFactory;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.BstTesting.SimpleNode;

import junit.framework.TestCase;

import javax.annotation.Nullable;

/**
 * Tests for an arbitrary {@code BSTRebalancePolicy}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public abstract class AbstractBstBalancePolicyTest extends TestCase {
  protected abstract BstBalancePolicy<SimpleNode> getBalancePolicy();

  public void testBalanceLeaf() {
    SimpleNode a = new SimpleNode('a', null, null);
    assertInOrderTraversalIs(getBalancePolicy().balance(nodeFactory, a, null, null), "a");
  }

  private SimpleNode balanceNew(char c, @Nullable SimpleNode left, @Nullable SimpleNode right) {
    return getBalancePolicy().balance(nodeFactory, new SimpleNode(c, null, null), left, right);
  }

  public void testBalanceTree1() {
    //   b
    //    \
    //    c
    SimpleNode c = balanceNew('c', null, null);
    SimpleNode b = balanceNew('b', null, c);
    assertInOrderTraversalIs(b, "bc");
  }

  public void testBalanceTree2() {
    //   b
    //  /
    //  a
    SimpleNode a = balanceNew('a', null, null);
    SimpleNode b = balanceNew('b', a, null);
    assertInOrderTraversalIs(b, "ab");
  }

  public void testBalanceTree3() {
    //   b
    //  / \
    //  a c
    SimpleNode a = balanceNew('a', null, null);
    SimpleNode c = balanceNew('c', null, null);
    SimpleNode b = balanceNew('b', a, c);
    assertInOrderTraversalIs(b, "abc");
  }

  public void testBalanceTree4() {
    // a
    //  \
    //  b
    //   \
    //   c
    //    \
    //    d
    //     \
    //     e
    //      \
    //       f

    SimpleNode f = balanceNew('f', null, null);
    SimpleNode e = balanceNew('e', null, f);
    SimpleNode d = balanceNew('d', null, e);
    SimpleNode c = balanceNew('c', null, d);
    SimpleNode b = balanceNew('b', null, c);
    SimpleNode a = balanceNew('a', null, b);
    assertInOrderTraversalIs(a, "abcdef");
  }

  public void testBalanceTree5() {
    //       f
    //      /
    //      e
    //     /
    //     d
    //    /
    //    c
    //   /
    //   b
    //  /
    //  a
    SimpleNode a = balanceNew('a', null, null);
    SimpleNode b = balanceNew('b', a, null);
    SimpleNode c = balanceNew('c', b, null);
    SimpleNode d = balanceNew('d', c, null);
    SimpleNode e = balanceNew('e', d, null);
    SimpleNode f = balanceNew('f', e, null);
    assertInOrderTraversalIs(f, "abcdef");
  }
}
