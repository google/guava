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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstSide.RIGHT;
import static junit.framework.Assert.assertEquals;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.testing.NullPointerTester;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Testing classes and utilities to be used in tests of the binary search tree framework.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class BstTesting {
  static final class SimpleNode extends BstNode<Character, SimpleNode> {
    SimpleNode(Character key, @Nullable SimpleNode left, @Nullable SimpleNode right) {
      super(key, left, right);
    }

    @Override
    public String toString() {
      return getKey().toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof SimpleNode) {
        SimpleNode node = (SimpleNode) obj;
        return getKey().equals(node.getKey())
            && Objects.equal(childOrNull(LEFT), node.childOrNull(LEFT))
            && Objects.equal(childOrNull(RIGHT), node.childOrNull(RIGHT));
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getKey(), childOrNull(LEFT), childOrNull(RIGHT));
    }
  }

  static final BstNodeFactory<SimpleNode> nodeFactory = new BstNodeFactory<SimpleNode>() {
    @Override
    public SimpleNode createNode(
        SimpleNode source, @Nullable SimpleNode left, @Nullable SimpleNode right) {
      return new SimpleNode(source.getKey(), left, right);
    }
  };

  static final BstBalancePolicy<SimpleNode> balancePolicy = new BstBalancePolicy<SimpleNode>() {
    @Override
    public SimpleNode balance(BstNodeFactory<SimpleNode> nodeFactory, SimpleNode source,
        @Nullable SimpleNode left, @Nullable SimpleNode right) {
      return checkNotNull(nodeFactory).createNode(source, left, right);
    }

    @Nullable
    @Override
    public SimpleNode combine(BstNodeFactory<SimpleNode> nodeFactory, @Nullable SimpleNode left,
        @Nullable SimpleNode right) {
      // Shove right into the rightmost position in the left tree.
      if (left == null) {
        return right;
      } else if (right == null) {
        return left;
      } else if (left.hasChild(RIGHT)) {
        return nodeFactory.createNode(
            left, left.childOrNull(LEFT), combine(nodeFactory, left.childOrNull(RIGHT), right));
      } else {
        return nodeFactory.createNode(left, left.childOrNull(LEFT), right);
      }
    }
  };

  static final BstPathFactory<SimpleNode, BstInOrderPath<SimpleNode>> pathFactory =
      BstInOrderPath.inOrderFactory();

  // A direct, if dumb, way to count total nodes in a tree.
  static final BstAggregate<SimpleNode> countAggregate = new BstAggregate<SimpleNode>() {
    @Override
    public int entryValue(SimpleNode entry) {
      return 1;
    }

    @Override
    public long treeValue(@Nullable SimpleNode tree) {
      if (tree == null) {
        return 0;
      } else {
        return 1 + treeValue(tree.childOrNull(LEFT)) + treeValue(tree.childOrNull(RIGHT));
      }
    }
  };

  static <P extends BstPath<SimpleNode, P>> List<SimpleNode> pathToList(P path) {
    List<SimpleNode> list = Lists.newArrayList();
    for (; path != null; path = path.prefixOrNull()) {
      list.add(path.getTip());
    }
    return list;
  }

  static <N extends BstNode<?, N>, P extends BstPath<N, P>> P extension(
      BstPathFactory<N, P> factory, N root, BstSide... sides) {
    P path = factory.initialPath(root);
    for (BstSide side : sides) {
      path = factory.extension(path, side);
    }
    return path;
  }

  static void assertInOrderTraversalIs(@Nullable SimpleNode root, String order) {
    if (root == null) {
      assertEquals("", order);
    } else {
      BstInOrderPath<SimpleNode> path = pathFactory.initialPath(root);
      while (path.getTip().hasChild(LEFT)) {
        path = pathFactory.extension(path, LEFT);
      }
      assertEquals(order.charAt(0), path
          .getTip()
          .getKey()
          .charValue());
      int i;
      for (i = 1; path.hasNext(RIGHT); i++) {
        path = path.next(RIGHT);
        assertEquals(order.charAt(i), path
            .getTip()
            .getKey()
            .charValue());
      }
      assertEquals(i, order.length());
    }
  }

  @GwtIncompatible("NullPointerTester")
  static NullPointerTester defaultNullPointerTester() {
    NullPointerTester tester = new NullPointerTester();
    SimpleNode node = new SimpleNode('a', null, null);
    tester.setDefault(BstNode.class, node);
    tester.setDefault(BstSide.class, LEFT);
    tester.setDefault(BstNodeFactory.class, nodeFactory);
    tester.setDefault(BstBalancePolicy.class, balancePolicy);
    tester.setDefault(BstPathFactory.class, pathFactory);
    tester.setDefault(BstPath.class, pathFactory.initialPath(node));
    tester.setDefault(BstInOrderPath.class, pathFactory.initialPath(node));
    tester.setDefault(Object.class, 'a');
    tester.setDefault(GeneralRange.class, GeneralRange.all(Ordering.natural()));
    tester.setDefault(BstAggregate.class, countAggregate);
    BstModifier<Character, SimpleNode> modifier = new BstModifier<Character, SimpleNode>() {
      @Nullable
      @Override
      public BstModificationResult<SimpleNode> modify(
          Character key, @Nullable SimpleNode originalEntry) {
        return BstModificationResult.identity(originalEntry);
      }
    };
    tester.setDefault(
        BstModificationResult.class, BstModificationResult.<SimpleNode>identity(null));
    tester.setDefault(BstModifier.class, modifier);
    tester.setDefault(
        BstMutationRule.class, BstMutationRule.createRule(modifier, balancePolicy, nodeFactory));
    return tester;
  }
}
