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

import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstTesting.assertInOrderTraversalIs;
import static com.google.common.collect.BstTesting.balancePolicy;
import static com.google.common.collect.BstTesting.defaultNullPointerTester;
import static com.google.common.collect.BstTesting.extension;
import static com.google.common.collect.BstTesting.nodeFactory;
import static com.google.common.collect.BstTesting.pathFactory;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reportMatcher;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.BstModificationResult.ModificationType;
import com.google.common.collect.BstTesting.SimpleNode;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;

/**
 * Tests for {@code BstOperations}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class BstOperationsTest extends TestCase {
  public void testSeek1() {
    //    d
    //   / \
    //  b   f
    // /     \
    // a     g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode b = new SimpleNode('b', a, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);
    assertEquals(a, BstOperations.seek(Ordering.natural(), d, 'a'));
    assertEquals(b, BstOperations.seek(Ordering.natural(), d, 'b'));
    assertNull(BstOperations.seek(Ordering.natural(), d, 'c'));
    assertEquals(d, BstOperations.seek(Ordering.natural(), d, 'd'));
    assertNull(BstOperations.seek(Ordering.natural(), d, 'e'));
    assertEquals(f, BstOperations.seek(Ordering.natural(), d, 'f'));
    assertEquals(g, BstOperations.seek(Ordering.natural(), d, 'g'));
  }

  public void testSeek2() {
    //    d
    //   / \
    //  b   f
    //   \ /
    //   c e 
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode f = new SimpleNode('f', e, null);
    SimpleNode d = new SimpleNode('d', b, f);
    assertNull(BstOperations.seek(Ordering.natural(), d, 'a'));
    assertEquals(b, BstOperations.seek(Ordering.natural(), d, 'b'));
    assertEquals(c, BstOperations.seek(Ordering.natural(), d, 'c'));
    assertEquals(d, BstOperations.seek(Ordering.natural(), d, 'd'));
    assertEquals(e, BstOperations.seek(Ordering.natural(), d, 'e'));
    assertEquals(f, BstOperations.seek(Ordering.natural(), d, 'f'));
    assertNull(BstOperations.seek(Ordering.natural(), d, 'g'));
  }

  @GwtIncompatible("EasyMock")
  @SuppressWarnings("unchecked")
  public void testModifyInsertAbsentNode() {
    //    d
    //   / \
    //  b   f
    // /     \
    // a     g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode b = new SimpleNode('b', a, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);

    BstNodeFactory<SimpleNode> nodeFactory = EasyMock.createStrictMock(BstNodeFactory.class);
    BstBalancePolicy<SimpleNode> balancePolicy = EasyMock.createStrictMock(BstBalancePolicy.class);
    BstModifier<Character, SimpleNode> modifier = EasyMock.createStrictMock(BstModifier.class);

    SimpleNode c = new SimpleNode('c', null, null);
    expect(modifier.modify(eq('c'), (SimpleNode) isNull())).andReturn(
        BstModificationResult.rebalancingChange(null, c));

    expect(balancePolicy.balance(
        same(nodeFactory), same(c), (SimpleNode) isNull(), (SimpleNode) isNull()))
        .andReturn(c)
        .times(0, 1);

    SimpleNode bWithC = new SimpleNode('b', a, c);
    expectPossibleEntryfication(nodeFactory, b);
    expect(balancePolicy.balance(
        same(nodeFactory), withKey('b'), same(a), withKey('c')))
        .andReturn(bWithC);

    SimpleNode dWithBWithC = new SimpleNode('d', bWithC, f);
    expectPossibleEntryfication(nodeFactory, d);
    expect(
        balancePolicy.balance(same(nodeFactory), withKey('d'), same(bWithC), same(f)))
        .andReturn(dWithBWithC);
    replay(nodeFactory, balancePolicy, modifier);
    BstMutationRule<Character, SimpleNode> mutationRule =
        BstMutationRule.createRule(modifier, balancePolicy, nodeFactory);
    BstMutationResult<Character, SimpleNode> mutationResult =
        BstOperations.mutate(Ordering.natural(), mutationRule, d, 'c');
    assertEquals('c', mutationResult.getTargetKey().charValue());
    assertNull(mutationResult.getOriginalTarget());
    assertEquals('c', mutationResult
        .getChangedTarget()
        .getKey()
        .charValue());
    assertSame(d, mutationResult.getOriginalRoot());
    assertSame(dWithBWithC, mutationResult.getChangedRoot());
    assertEquals(ModificationType.REBALANCING_CHANGE, mutationResult.modificationType());
    verify(nodeFactory, balancePolicy, modifier);
  }

  @GwtIncompatible("EasyMock")
  @SuppressWarnings("unchecked")
  public void testModifyInsertPresentNode() {
    // We wish to test that BstOperations & co. treat IDENTITY modifications as the same.
    //    d
    //   / \
    //  b   f
    // /     \
    // a     g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode b = new SimpleNode('b', a, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);

    BstNodeFactory<SimpleNode> nodeFactory = EasyMock.createStrictMock(BstNodeFactory.class);
    BstBalancePolicy<SimpleNode> balancePolicy = EasyMock.createStrictMock(BstBalancePolicy.class);
    BstModifier<Character, SimpleNode> modifier = EasyMock.createStrictMock(BstModifier.class);

    expectPossibleEntryfication(nodeFactory, a);
    expect(modifier.modify(eq('a'), withKey('a'))).andReturn(
        BstModificationResult.identity(a));
    replay(nodeFactory, balancePolicy, modifier);
    BstMutationRule<Character, SimpleNode> mutationRule =
        BstMutationRule.createRule(modifier, balancePolicy, nodeFactory);
    BstMutationResult<Character, SimpleNode> mutationResult =
        BstOperations.mutate(Ordering.natural(), mutationRule, d, 'a');
    assertEquals('a', mutationResult.getTargetKey().charValue());
    assertSame(a, mutationResult.getOriginalTarget());
    assertSame(a, mutationResult.getChangedTarget());
    assertSame(d, mutationResult.getOriginalRoot());
    assertSame(d, mutationResult.getChangedRoot());
    assertEquals(ModificationType.IDENTITY, mutationResult.modificationType());
    verify(nodeFactory, balancePolicy, modifier);
  }

  @GwtIncompatible("EasyMock")
  @SuppressWarnings("unchecked")
  public void testModifyInsertInequivalentNode() {
    // We wish to test that BstOperations & co. treat non-equivalent() nodes as different.
    //    d
    //   / \
    //  b   f
    // /     \
    // a     g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode b = new SimpleNode('b', a, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);

    BstNodeFactory<SimpleNode> nodeFactory = EasyMock.createStrictMock(BstNodeFactory.class);
    BstBalancePolicy<SimpleNode> balancePolicy = EasyMock.createStrictMock(BstBalancePolicy.class);
    BstModifier<Character, SimpleNode> modifier = EasyMock.createStrictMock(BstModifier.class);

    expectPossibleEntryfication(nodeFactory, a);
    SimpleNode a2 = new SimpleNode('a', null, null);
    expect(modifier.modify(eq('a'), withKey('a'))).andReturn(
        BstModificationResult.rebuildingChange(a, a2));

    expectPossibleEntryfication(nodeFactory, a2);

    SimpleNode bWithA2 = new SimpleNode('b', a2, null);
    expect(nodeFactory.createNode(same(b), withKey('a'), (SimpleNode) isNull())).andReturn(
        bWithA2);

    SimpleNode dWithA2 = new SimpleNode('d', bWithA2, f);
    expect(nodeFactory.createNode(same(d), same(bWithA2), same(f))).andReturn(
        dWithA2);

    replay(nodeFactory, balancePolicy, modifier);
    BstMutationRule<Character, SimpleNode> mutationRule =
        BstMutationRule.createRule(modifier, balancePolicy, nodeFactory);
    BstMutationResult<Character, SimpleNode> mutationResult =
        BstOperations.mutate(Ordering.natural(), mutationRule, d, 'a');
    assertEquals('a', mutationResult.getTargetKey().charValue());
    assertSame(a, mutationResult.getOriginalTarget());
    assertEquals('a', mutationResult.getChangedTarget().getKey().charValue());
    assertSame(d, mutationResult.getOriginalRoot());
    assertSame(dWithA2, mutationResult.getChangedRoot());
    assertEquals(ModificationType.REBUILDING_CHANGE, mutationResult.modificationType());
    verify(nodeFactory, balancePolicy, modifier);
  }

  @GwtIncompatible("EasyMock")
  @SuppressWarnings("unchecked")
  public void testModifyDeletePresentNode() {
    //    d
    //   / \
    //  b   f
    // /     \
    // a     g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode b = new SimpleNode('b', a, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);

    BstNodeFactory<SimpleNode> nodeFactory = EasyMock.createStrictMock(BstNodeFactory.class);
    BstBalancePolicy<SimpleNode> balancePolicy = EasyMock.createStrictMock(BstBalancePolicy.class);
    BstModifier<Character, SimpleNode> modifier = EasyMock.createStrictMock(BstModifier.class);

    expectPossibleEntryfication(nodeFactory, a);
    expect(modifier.modify(eq('a'), withKey('a'))).andReturn(
        BstModificationResult.rebalancingChange(a, null));

    expect(balancePolicy.combine(same(nodeFactory), (SimpleNode) isNull(), (SimpleNode) isNull()))
        .andReturn(null);

    expectPossibleEntryfication(nodeFactory, b);
    SimpleNode leafB = new SimpleNode('b', null, null);
    expect(
        balancePolicy.balance(same(nodeFactory), withKey('b'), (SimpleNode) isNull(),
            (SimpleNode) isNull())).andReturn(leafB);

    SimpleNode dWithLeafB = new SimpleNode('d', leafB, f);
    expectPossibleEntryfication(nodeFactory, d);
    expect(
        balancePolicy.balance(same(nodeFactory), withKey('d'), same(leafB), same(f)))
        .andReturn(dWithLeafB);
    replay(nodeFactory, balancePolicy, modifier);
    BstMutationRule<Character, SimpleNode> mutationRule =
        BstMutationRule.createRule(modifier, balancePolicy, nodeFactory);
    BstMutationResult<Character, SimpleNode> mutationResult =
        BstOperations.mutate(Ordering.natural(), mutationRule, d, 'a');
    assertEquals('a', mutationResult.getTargetKey().charValue());
    assertEquals('a', mutationResult
        .getOriginalTarget()
        .getKey()
        .charValue());
    assertNull(mutationResult.getChangedTarget());
    assertSame(d, mutationResult.getOriginalRoot());
    assertSame(dWithLeafB, mutationResult.getChangedRoot());
    assertEquals(ModificationType.REBALANCING_CHANGE, mutationResult.modificationType());
    verify(nodeFactory, balancePolicy, modifier);
  }

  @GwtIncompatible("EasyMock")
  @SuppressWarnings("unchecked")
  public void testModifyDeleteAbsentNode() {
    //    d
    //   / \
    //  b   f
    // /     \
    // a     g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode b = new SimpleNode('b', a, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);

    BstNodeFactory<SimpleNode> nodeFactory = EasyMock.createStrictMock(BstNodeFactory.class);
    BstBalancePolicy<SimpleNode> balancePolicy = EasyMock.createStrictMock(BstBalancePolicy.class);
    BstModifier<Character, SimpleNode> modifier = EasyMock.createStrictMock(BstModifier.class);

    expectPossibleEntryfication(nodeFactory, a);
    expect(modifier.modify(eq('c'), (SimpleNode) isNull())).andReturn(
        BstModificationResult.<SimpleNode> identity(null));
    replay(nodeFactory, balancePolicy, modifier);
    BstMutationRule<Character, SimpleNode> mutationRule =
        BstMutationRule.createRule(modifier, balancePolicy, nodeFactory);
    BstMutationResult<Character, SimpleNode> mutationResult =
        BstOperations.mutate(Ordering.natural(), mutationRule, d, 'c');
    assertEquals('c', mutationResult.getTargetKey().charValue());
    assertEquals(d, mutationResult.getOriginalRoot());
    assertEquals(d, mutationResult.getChangedRoot());
    assertNull(mutationResult.getOriginalTarget());
    assertNull(mutationResult.getChangedTarget());
    assertEquals(ModificationType.IDENTITY, mutationResult.modificationType());
    verify(nodeFactory, balancePolicy, modifier);
  }

  @GwtIncompatible("EasyMock")
  @SuppressWarnings("unchecked")
  public void testModifyPathInsertPresentNode() {
    // We wish to test that BstOperations & co. treat identity-different nodes as changed,
    // instead of using SimpleNode.equals().
    //    d
    //   / \
    //  b   f
    // /     \
    // a     g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode b = new SimpleNode('b', a, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);

    BstNodeFactory<SimpleNode> nodeFactory = EasyMock.createStrictMock(BstNodeFactory.class);
    BstBalancePolicy<SimpleNode> balancePolicy = EasyMock.createStrictMock(BstBalancePolicy.class);
    BstModifier<Character, SimpleNode> modifier = EasyMock.createStrictMock(BstModifier.class);

    expectPossibleEntryfication(nodeFactory, a);
    expect(modifier.modify(eq('a'), withKey('a'))).andReturn(BstModificationResult.identity(a));
    replay(nodeFactory, balancePolicy, modifier);
    BstInOrderPath<SimpleNode> path = extension(pathFactory, d, LEFT, LEFT);
    BstMutationRule<Character, SimpleNode> mutationRule =
        BstMutationRule.createRule(modifier, balancePolicy, nodeFactory);
    BstMutationResult<Character, SimpleNode> mutationResult =
        BstOperations.mutate(path, mutationRule);
    assertEquals('a', mutationResult.getTargetKey().charValue());
    assertSame(a, mutationResult.getOriginalTarget());
    assertSame(a, mutationResult.getChangedTarget());
    assertSame(d, mutationResult.getOriginalRoot());
    assertSame(d, mutationResult.getChangedRoot());
    assertEquals(ModificationType.IDENTITY, mutationResult.modificationType());
    verify(nodeFactory, balancePolicy, modifier);
  }

  @GwtIncompatible("EasyMock")
  private SimpleNode withKey(final char c) {
    reportMatcher(new IArgumentMatcher() {
      @Override
      public void appendTo(StringBuffer buffer) {
        buffer.append("Expected BstNode with key ").append(c);
      }

      @Override
      public boolean matches(Object argument) {
        return argument instanceof SimpleNode
            && ((SimpleNode) argument).getKey().charValue() == c;
      }
    });
    return null;
  }

  /**
   * The implementation may remove the children of a node it treats as an entry for safety. Expect
   * this and handle it.
   */
  @GwtIncompatible("EasyMock")
  private void expectPossibleEntryfication(BstNodeFactory<SimpleNode> factory, SimpleNode entry) {
    expect(factory.createNode(same(entry), (SimpleNode) isNull(), (SimpleNode) isNull()))
        .andReturn(new SimpleNode(entry.getKey(), null, null))
        .times(0, 1);
  }
  public void testInsertMin1() {
    //    d
    //   / \
    //  b   f
    //   \   \
    //   c   g
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);

    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode newRoot = BstOperations.insertMin(d, a, nodeFactory, balancePolicy);
    assertInOrderTraversalIs(newRoot, "abcdfg");
  }

  public void testInsertMin2() {
    //    d
    //   / \
    //  b   f
    //       \
    //       g
    SimpleNode b = new SimpleNode('b', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);

    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode newRoot = BstOperations.insertMin(d, a, nodeFactory, balancePolicy);
    assertInOrderTraversalIs(newRoot, "abdfg");
  }

  public void testInsertMinEmpty() {
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode newRoot = BstOperations.insertMin(null, a, nodeFactory, balancePolicy);
    assertInOrderTraversalIs(newRoot, "a");
  }

  public void testInsertMax1() {
    //    d
    //   / \
    //  b   f
    //   \   \
    //   c   g
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);

    SimpleNode h = new SimpleNode('h', null, null);
    SimpleNode newRoot = BstOperations.insertMax(d, h, nodeFactory, balancePolicy);
    assertInOrderTraversalIs(newRoot, "bcdfgh");
  }

  public void testInsertMax2() {
    //    d
    //   / \
    //  b   f
    //     / 
    //     e
    SimpleNode b = new SimpleNode('b', null, null);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode f = new SimpleNode('f', e, null);
    SimpleNode d = new SimpleNode('d', b, f);

    SimpleNode h = new SimpleNode('h', null, null);
    SimpleNode newRoot = BstOperations.insertMax(d, h, nodeFactory, balancePolicy);
    assertInOrderTraversalIs(newRoot, "bdefh");
  }

  public void testInsertMaxEmpty() {
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode newRoot = BstOperations.insertMax(null, a, nodeFactory, balancePolicy);
    assertInOrderTraversalIs(newRoot, "a");
  }

  public void testExtractMin1() {
    //    d
    //   / \
    //  b   f
    //   \   \
    //   c   g
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);

    BstMutationResult<Character, SimpleNode> extractMin =
        BstOperations.extractMin(d, nodeFactory, balancePolicy);
    assertEquals('b', extractMin.getTargetKey().charValue());
    assertEquals(d, extractMin.getOriginalRoot());
    assertEquals(b, extractMin.getOriginalTarget());
    assertNull(extractMin.getChangedTarget());
    assertInOrderTraversalIs(extractMin.getChangedRoot(), "cdfg");
  }

  public void testExtractMin2() {
    //    d
    //   / \
    //  b   f
    //       \
    //       g
    SimpleNode b = new SimpleNode('b', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);

    BstMutationResult<Character, SimpleNode> extractMin =
        BstOperations.extractMin(d, nodeFactory, balancePolicy);
    assertEquals('b', extractMin.getTargetKey().charValue());
    assertEquals(d, extractMin.getOriginalRoot());
    assertEquals(b, extractMin.getOriginalTarget());
    assertNull(extractMin.getChangedTarget());
    assertInOrderTraversalIs(extractMin.getChangedRoot(), "dfg");
  }

  public void testExtractMax1() {
    //    d
    //   / \
    //  b   f
    //   \   \
    //   c   g
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);

    BstMutationResult<Character, SimpleNode> extractMax =
        BstOperations.extractMax(d, nodeFactory, balancePolicy);
    assertEquals('g', extractMax.getTargetKey().charValue());
    assertEquals(d, extractMax.getOriginalRoot());
    assertEquals(g, extractMax.getOriginalTarget());
    assertNull(extractMax.getChangedTarget());
    assertInOrderTraversalIs(extractMax.getChangedRoot(), "bcdf");
  }

  public void testExtractMax2() {
    //    d
    //   / \
    //  b   f
    //     /
    //     e
    SimpleNode b = new SimpleNode('b', null, null);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode f = new SimpleNode('f', e, null);
    SimpleNode d = new SimpleNode('d', b, f);

    BstMutationResult<Character, SimpleNode> extractMax =
        BstOperations.extractMax(d, nodeFactory, balancePolicy);
    assertEquals('f', extractMax.getTargetKey().charValue());
    assertEquals(d, extractMax.getOriginalRoot());
    assertEquals(f, extractMax.getOriginalTarget());
    assertNull(extractMax.getChangedTarget());
    assertInOrderTraversalIs(extractMax.getChangedRoot(), "bde");
  }

  @GwtIncompatible("NullPointerTester")
  public void testNullPointers() throws Exception {
    defaultNullPointerTester().testAllPublicStaticMethods(BstOperations.class);
  }
}
