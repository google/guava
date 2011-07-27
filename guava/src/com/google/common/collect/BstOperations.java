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

import java.util.Comparator;

import javax.annotation.Nullable;

/**
 * Tools to perform single-key queries and mutations in binary search trees.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class BstOperations {
  private BstOperations() {}

  /**
   * Returns the node with key {@code key} in {@code tree}, if any.
   */
  @Nullable
  static <K, N extends BstNode<K, N>> N seek(
      Comparator<? super K> comparator, @Nullable N tree, K key) {
    checkNotNull(comparator);
    if (tree == null) {
      return null;
    }
    int cmp = comparator.compare(key, tree.getKey());
    if (cmp == 0) {
      return tree;
    } else {
      BstSide side = (cmp < 0) ? LEFT : RIGHT;
      return seek(comparator, tree.childOrNull(side), key);
    }
  }

  /**
   * Returns the result of performing the mutation specified by {@code mutationRule} in {@code
   * tree} at the location with key {@code key}.
   */
  static <K, N extends BstNode<K, N>> BstMutationResult<K, N> mutate(
      Comparator<? super K> comparator, BstMutationRule<K, N> mutationRule, @Nullable N tree,
      K key) {
    checkNotNull(comparator);
    checkNotNull(mutationRule);
    checkNotNull(key);
    BstBalancePolicy<N> rebalancePolicy = mutationRule.getBalancePolicy();
    BstNodeFactory<N> nodeFactory = mutationRule.getNodeFactory();
    BstModifier<K, N> modifier = mutationRule.getModifier();

    if (tree != null) {
      int cmp = comparator.compare(key, tree.getKey());
      if (cmp != 0) {
        BstSide side = (cmp < 0) ? LEFT : RIGHT;
        BstMutationResult<K, N> mutation =
            mutate(comparator, mutationRule, tree.childOrNull(side), key);
        return mutation.lift(tree, side, nodeFactory, rebalancePolicy);
      }
    }
    // We're modifying this node
    N newTree = modifier.modify(key, tree);
    if (newTree == tree) {
      return BstMutationResult.identity(key, tree, tree);
    } else if (newTree == null) {
      newTree =
          rebalancePolicy.combine(nodeFactory, tree.childOrNull(LEFT), tree.childOrNull(RIGHT));
    } else {
      N left = null;
      N right = null;
      if (tree != null) {
        left = tree.childOrNull(LEFT);
        right = tree.childOrNull(RIGHT);
      }
      newTree = rebalancePolicy.balance(nodeFactory, newTree, left, right);
    }
    return BstMutationResult.mutationResult(key, tree, newTree, tree, newTree);
  }
}
