/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing permissions and limitations
 * under the License.
 */

package com.google.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.testing.NullPointerTester;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import junit.framework.TestCase;

/**
 * Tests for {@code TreeTraverser}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class TreeTraverserTest extends TestCase {
  private static class Node {
    final char value;

    Node(char value) {
      this.value = value;
    }
  }

  private static final class Tree extends Node {
    final List<Tree> children;

    public Tree(char value, Tree... children) {
      super(value);
      this.children = Arrays.asList(children);
    }
  }

  private static final class BinaryTree extends Node {
    @Nullable final BinaryTree left;
    @Nullable
    final BinaryTree right;

    private BinaryTree(char value, BinaryTree left, BinaryTree right) {
      super(value);
      this.left = left;
      this.right = right;
    }
  }

  private static final TreeTraverser<Tree> ADAPTER = new TreeTraverser<Tree>() {
    @Override
    public Iterable<Tree> children(Tree node) {
      return node.children;
    }
  };

  private static final TreeTraverser<Tree> ADAPTER_USING_USING =
      TreeTraverser.using(
          new Function<Tree, Iterable<Tree>>() {
            @Override
            public Iterable<Tree> apply(Tree node) {
              return node.children;
            }
          });

  private static final BinaryTreeTraverser<BinaryTree> BIN_ADAPTER =
      new BinaryTreeTraverser<BinaryTree>() {

    @Override
    public Optional<BinaryTree> leftChild(BinaryTree node) {
      return Optional.fromNullable(node.left);
    }

    @Override
    public Optional<BinaryTree> rightChild(BinaryTree node) {
      return Optional.fromNullable(node.right);
    }
  };

  //        h
  //      / | \
  //     /  e  \
  //    d       g
  //   /|\      |
  //  / | \     f
  // a  b  c
  static final Tree a = new Tree('a');
  static final Tree b = new Tree('b');
  static final Tree c = new Tree('c');
  static final Tree d = new Tree('d', a, b, c);
  static final Tree e = new Tree('e');
  static final Tree f = new Tree('f');
  static final Tree g = new Tree('g', f);
  static final Tree h = new Tree('h', d, e, g);

  //      d
  //     / \
  //    b   e
  //   / \   \
  //  a   c   f
  //         /
  //        g
  static final BinaryTree ba = new BinaryTree('a', null, null);
  static final BinaryTree bc = new BinaryTree('c', null, null);
  static final BinaryTree bb = new BinaryTree('b', ba, bc);
  static final BinaryTree bg = new BinaryTree('g', null, null);
  static final BinaryTree bf = new BinaryTree('f', bg, null);
  static final BinaryTree be = new BinaryTree('e', null, bf);
  static final BinaryTree bd = new BinaryTree('d', bb, be);

  static String iterationOrder(Iterable<? extends Node> iterable) {
    StringBuilder builder = new StringBuilder();
    for (Node t : iterable) {
      builder.append(t.value);
    }
    StringBuilder forEachBuilder = new StringBuilder();
    iterable.forEach(t -> forEachBuilder.append(t.value));
    assertTrue(
        "Iterator content was " + builder + " but forEach content was " + forEachBuilder,
        builder.toString().contentEquals(forEachBuilder));
    return builder.toString();
  }

  public void testPreOrder() {
    assertThat(iterationOrder(ADAPTER.preOrderTraversal(h))).isEqualTo("hdabcegf");
    assertThat(iterationOrder(BIN_ADAPTER.preOrderTraversal(bd))).isEqualTo("dbacefg");
  }

  public void testPostOrder() {
    assertThat(iterationOrder(ADAPTER.postOrderTraversal(h))).isEqualTo("abcdefgh");
    assertThat(iterationOrder(BIN_ADAPTER.postOrderTraversal(bd))).isEqualTo("acbgfed");
  }

  public void testBreadthOrder() {
    assertThat(iterationOrder(ADAPTER.breadthFirstTraversal(h))).isEqualTo("hdegabcf");
    assertThat(iterationOrder(BIN_ADAPTER.breadthFirstTraversal(bd))).isEqualTo("dbeacfg");
  }

  public void testInOrder() {
    assertThat(iterationOrder(BIN_ADAPTER.inOrderTraversal(bd))).isEqualTo("abcdegf");
  }

  public void testUsing() {
    assertThat(iterationOrder(ADAPTER_USING_USING.preOrderTraversal(h))).isEqualTo("hdabcegf");
  }

  @GwtIncompatible // NullPointerTester
  public void testNulls() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(ADAPTER);
    tester.testAllPublicInstanceMethods(BIN_ADAPTER);
  }
}
