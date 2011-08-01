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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstSide.RIGHT;

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * Internal utilities on binary search trees.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class BstUtilities {
  /**
   * Returns the result of removing the minimum element from the specified subtree.
   */
  public static <K, N extends BstNode<K, N>> BstMutationResult<K, N> extractMin(
      N root, BstNodeFactory<N> nodeFactory, BstBalancePolicy<N> balancePolicy) {
    checkNotNull(root);
    checkNotNull(nodeFactory);
    checkNotNull(balancePolicy);
    if (root.hasChild(LEFT)) {
      BstMutationResult<K, N> subResult =
          extractMin(root.getChild(LEFT), nodeFactory, balancePolicy);
      return subResult.lift(root, LEFT, nodeFactory, balancePolicy);
    }
    return BstMutationResult.mutationResult(
        root.getKey(), root, root.childOrNull(RIGHT), root, null);
  }

  /**
   * Returns the result of removing the maximum element from the specified subtree.
   */
  public static <K, N extends BstNode<K, N>> BstMutationResult<K, N> extractMax(
      N root, BstNodeFactory<N> nodeFactory, BstBalancePolicy<N> balancePolicy) {
    checkNotNull(root);
    checkNotNull(nodeFactory);
    checkNotNull(balancePolicy);
    if (root.hasChild(RIGHT)) {
      BstMutationResult<K, N> subResult =
          extractMax(root.getChild(RIGHT), nodeFactory, balancePolicy);
      return subResult.lift(root, RIGHT, nodeFactory, balancePolicy);
    }
    return BstMutationResult.mutationResult(
        root.getKey(), root, root.childOrNull(LEFT), root, null);
  }

  /**
   * Inserts the specified entry into the tree as the minimum entry. Assumes that {@code
   * entry.getKey()} is less than the key of all nodes in the subtree {@code root}.
   */
  public static <N extends BstNode<?, N>> N insertMin(@Nullable N root, N entry,
      BstNodeFactory<N> nodeFactory, BstBalancePolicy<N> balancePolicy) {
    checkNotNull(entry);
    checkNotNull(nodeFactory);
    checkNotNull(balancePolicy);
    if (root == null) {
      return nodeFactory.createLeaf(entry);
    } else {
      return balancePolicy.balance(nodeFactory, root,
          insertMin(root.childOrNull(LEFT), entry, nodeFactory, balancePolicy),
          root.childOrNull(RIGHT));
    }
  }

  /**
   * Inserts the specified entry into the tree as the maximum entry. Assumes that {@code
   * entry.getKey()} is greater than the key of all nodes in the subtree {@code root}.
   */
  public static <N extends BstNode<?, N>> N insertMax(@Nullable N root, N entry,
      BstNodeFactory<N> nodeFactory, BstBalancePolicy<N> balancePolicy) {
    checkNotNull(entry);
    checkNotNull(nodeFactory);
    checkNotNull(balancePolicy);
    if (root == null) {
      return nodeFactory.createLeaf(entry);
    } else {
      return balancePolicy.balance(nodeFactory, root, root.childOrNull(LEFT),
          insertMax(root.childOrNull(RIGHT), entry, nodeFactory, balancePolicy));
    }
  }
}
