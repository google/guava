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
  public static <K, N extends BstNode<K, N>> N seek(
      Comparator<? super K> comparator, @Nullable N tree, @Nullable K key) {
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
   *
   * <ul>
   * <li>If the returned {@link BstModificationResult} has type {@code IDENTITY}, the exact
   * original tree is returned.
   * <li>If the returned {@code BstModificationResult} has type {@code REBUILDING_CHANGE},
   * the tree will be rebuilt with the node factory of the mutation rule, but not rebalanced.
   * <li>If the returned {@code BstModificationResult} has type {@code REBALANCING_CHANGE},
   * the tree will be rebalanced using the balance policy of the mutation rule.
   * </ul>
   */
  public static <K, N extends BstNode<K, N>> BstMutationResult<K, N> mutate(
      Comparator<? super K> comparator, BstMutationRule<K, N> mutationRule, @Nullable N tree,
      @Nullable K key) {
    checkNotNull(comparator);
    checkNotNull(mutationRule);

    if (tree != null) {
      int cmp = comparator.compare(key, tree.getKey());
      if (cmp != 0) {
        BstSide side = (cmp < 0) ? LEFT : RIGHT;
        BstMutationResult<K, N> mutation =
            mutate(comparator, mutationRule, tree.childOrNull(side), key);
        return mutation.lift(
            tree, side, mutationRule.getNodeFactory(), mutationRule.getBalancePolicy());
      }
    }
    return modify(tree, key, mutationRule);
  }

  /**
   * Perform the local mutation at the tip of the specified path.
   */
  public static <K, N extends BstNode<K, N>> BstMutationResult<K, N> mutate(
      BstInOrderPath<N> path, BstMutationRule<K, N> mutationRule) {
    checkNotNull(path);
    checkNotNull(mutationRule);
    BstBalancePolicy<N> balancePolicy = mutationRule.getBalancePolicy();
    BstNodeFactory<N> nodeFactory = mutationRule.getNodeFactory();
    BstModifier<K, N> modifier = mutationRule.getModifier();

    N target = path.getTip();
    K key = target.getKey();
    BstMutationResult<K, N> result = modify(target, key, mutationRule);
    while (path.hasPrefix()) {
      BstInOrderPath<N> prefix = path.getPrefix();
      result = result.lift(prefix.getTip(), path.getSideOfExtension(), nodeFactory, balancePolicy);
      path = prefix;
    }
    return result;
  }

  /**
   * Perform the local mutation right here, at the specified node.
   */
  private static <K, N extends BstNode<K, N>> BstMutationResult<K, N> modify(
      @Nullable N tree, K key, BstMutationRule<K, N> mutationRule) {
    BstBalancePolicy<N> rebalancePolicy = mutationRule.getBalancePolicy();
    BstNodeFactory<N> nodeFactory = mutationRule.getNodeFactory();
    BstModifier<K, N> modifier = mutationRule.getModifier();

    N originalRoot = tree;
    N changedRoot;
    N originalTarget = (tree == null) ? null : nodeFactory.createLeaf(tree);
    BstModificationResult<N> modResult = modifier.modify(key, originalTarget);
    N originalLeft = null;
    N originalRight = null;
    if (tree != null) {
      originalLeft = tree.childOrNull(LEFT);
      originalRight = tree.childOrNull(RIGHT);
    }
    switch (modResult.getType()) {
      case IDENTITY:
        changedRoot = tree;
        break;
      case REBUILDING_CHANGE:
        if (modResult.getChangedTarget() != null) {
          changedRoot =
              nodeFactory.createNode(modResult.getChangedTarget(), originalLeft, originalRight);
        } else if (tree == null) {
          changedRoot = null;
        } else {
          throw new AssertionError(
              "Modification result is a REBUILDING_CHANGE, but rebalancing required");
        }
        break;
      case REBALANCING_CHANGE:
        if (modResult.getChangedTarget() != null) {
          changedRoot = rebalancePolicy.balance(
              nodeFactory, modResult.getChangedTarget(), originalLeft, originalRight);
        } else if (tree != null) {
          changedRoot = rebalancePolicy.combine(nodeFactory, originalLeft, originalRight);
        } else {
          changedRoot = null;
        }
        break;
      default:
        throw new AssertionError();
    }
    return BstMutationResult.mutationResult(key, originalRoot, changedRoot, modResult);
  }

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
        root.getKey(), root, root.childOrNull(RIGHT), 
        BstModificationResult.rebalancingChange(root, null));
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
    return BstMutationResult.mutationResult(root.getKey(), root, root.childOrNull(LEFT),
        BstModificationResult.rebalancingChange(root, null));
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
