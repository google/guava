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
import static com.google.common.collect.BstNode.countOrZero;
import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstSide.RIGHT;

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * A utility class with operations on binary search trees that operate on some interval.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class BstRangeOps {
  /**
   * An integer-valued function on binary search tree nodes that adds between nodes.
   */
  public interface BstAggregate<N extends BstNode<?, N>> {
    /**
     * The total value on an entire subtree. Must be equal to the sum of the {@link #entryValue
     * entryValue} of this node and all its descendants.
     */
    int treeValue(@Nullable N tree);

    /**
     * The value on a single entry, ignoring its descendants.
     */
    int entryValue(N entry);
  }

  private enum CountAggregate implements BstAggregate {
    INSTANCE {
      @Override
      public int entryValue(BstNode entry) {
        return 1;
      }

      @SuppressWarnings("unchecked")
      @Override
      public int treeValue(@Nullable BstNode tree) {
        return countOrZero(tree);
      }
    };
  }

  /**
   * Returns a {@link BstAggregate} counting the number of nodes.
   */
  @SuppressWarnings("unchecked")
  public static <N extends BstNode<?, N>> BstAggregate<N> countAggregate() {
    return CountAggregate.INSTANCE;
  }

  /**
   * Returns the total value of the specified aggregation function on the specified tree restricted
   * to the specified range. Assumes that the tree satisfies the binary search ordering property
   * relative to {@code range.comparator()}.
   */
  public static <K, N extends BstNode<K, N>> int totalInRange(
      BstAggregate<? super N> aggregate, GeneralRange<K> range, @Nullable N root) {
    checkNotNull(aggregate);
    checkNotNull(range);
    if (root == null || range.isEmpty()) {
      return 0;
    }
    int total = aggregate.treeValue(root);
    if (range.hasLowerBound()) {
      total -= totalTooLow(aggregate, range, root);
    }
    if (range.hasUpperBound()) {
      total -= totalTooHigh(aggregate, range, root);
    }
    return total;
  }

  // Returns total value strictly below the specified range.
  private static <K, N extends BstNode<K, N>> int totalTooLow(
      BstAggregate<? super N> aggregate, GeneralRange<K> range, @Nullable N root) {
    int accum = 0;
    while (root != null) {
      if (range.tooLow(root.getKey())) {
        accum += aggregate.entryValue(root);
        accum += aggregate.treeValue(root.childOrNull(LEFT));
        root = root.childOrNull(RIGHT);
      } else {
        root = root.childOrNull(LEFT);
      }
    }
    return accum;
  }

  // Returns the number of nodes strictly above the specified range.
  @Nullable
  private static <K, N extends BstNode<K, N>> int totalTooHigh(
      BstAggregate<? super N> aggregate, GeneralRange<K> range, @Nullable N root) {
    int accum = 0;
    while (root != null) {
      if (range.tooHigh(root.getKey())) {
        accum += aggregate.entryValue(root);
        accum += aggregate.treeValue(root.childOrNull(RIGHT));
        root = root.childOrNull(LEFT);
      } else {
        root = root.childOrNull(RIGHT);
      }
    }
    return accum;
  }

  /**
   * Returns a balanced tree containing all nodes from the specified tree that were <i>not</i> in
   * the specified range, using the specified balance policy. Assumes that the tree satisfies the
   * binary search ordering property relative to {@code range.comparator()}.
   */
  @Nullable
  public static <K, N extends BstNode<K, N>> N minusRange(GeneralRange<K> range,
      BstBalancePolicy<N> balancePolicy, BstNodeFactory<N> nodeFactory, @Nullable N root) {
    checkNotNull(range);
    checkNotNull(balancePolicy);
    checkNotNull(nodeFactory);
    N higher =
        range.hasUpperBound() ? subTreeTooHigh(range, balancePolicy, nodeFactory, root) : null;
    N lower =
        range.hasLowerBound() ? subTreeTooLow(range, balancePolicy, nodeFactory, root) : null;
    return balancePolicy.combine(nodeFactory, lower, higher);
  }

  /*
   * Returns a balanced tree containing all nodes in the specified tree that are strictly below the
   * specified range.
   */
  @Nullable
  private static <K, N extends BstNode<K, N>> N subTreeTooLow(GeneralRange<K> range,
      BstBalancePolicy<N> balancePolicy, BstNodeFactory<N> nodeFactory, @Nullable N root) {
    if (root == null) {
      return null;
    }
    if (range.tooLow(root.getKey())) {
      N right = subTreeTooLow(range, balancePolicy, nodeFactory, root.childOrNull(RIGHT));
      return balancePolicy.balance(nodeFactory, root, root.childOrNull(LEFT), right);
    } else {
      return subTreeTooLow(range, balancePolicy, nodeFactory, root.childOrNull(LEFT));
    }
  }

  /*
   * Returns a balanced tree containing all nodes in the specified tree that are strictly above the
   * specified range.
   */
  @Nullable
  private static <K, N extends BstNode<K, N>> N subTreeTooHigh(GeneralRange<K> range,
      BstBalancePolicy<N> balancePolicy, BstNodeFactory<N> nodeFactory, @Nullable N root) {
    if (root == null) {
      return null;
    }
    if (range.tooHigh(root.getKey())) {
      N left = subTreeTooHigh(range, balancePolicy, nodeFactory, root.childOrNull(LEFT));
      return balancePolicy.balance(nodeFactory, root, left, root.childOrNull(RIGHT));
    } else {
      return subTreeTooHigh(range, balancePolicy, nodeFactory, root.childOrNull(RIGHT));
    }
  }

  /**
   * Returns the furthest path to the specified side in the specified tree that falls into the
   * specified range.
   */
  @Nullable
  public static <K, N extends BstNode<K, N>, P extends BstPath<N, P>> P furthestPath(
      GeneralRange<K> range, BstSide side, BstPathFactory<N, P> pathFactory, @Nullable N root) {
    checkNotNull(range);
    checkNotNull(pathFactory);
    checkNotNull(side);
    if (root == null) {
      return null;
    }
    P path = pathFactory.initialPath(root);
    return furthestPath(range, side, pathFactory, path);
  }

  private static <K, N extends BstNode<K, N>, P extends BstPath<N, P>> P furthestPath(
      GeneralRange<K> range, BstSide side, BstPathFactory<N, P> pathFactory, P currentPath) {
    N tip = currentPath.getTip();
    K tipKey = tip.getKey();
    if (beyond(range, tipKey, side)) {
      if (tip.hasChild(side.other())) {
        currentPath = pathFactory.extension(currentPath, side.other());
        return furthestPath(range, side, pathFactory, currentPath);
      } else {
        return null;
      }
    } else if (tip.hasChild(side)) {
      P alphaPath = pathFactory.extension(currentPath, side);
      alphaPath = furthestPath(range, side, pathFactory, alphaPath);
      if (alphaPath != null) {
        return alphaPath;
      }
    }
    return beyond(range, tipKey, side.other()) ? null : currentPath;
  }

  /**
   * Returns {@code true} if {@code key} is beyond the specified side of the specified range.
   */
  public static <K> boolean beyond(GeneralRange<K> range, K key, BstSide side) {
    checkNotNull(range);
    checkNotNull(key);
    switch (side) {
      case LEFT:
        return range.tooLow(key);
      case RIGHT:
        return range.tooHigh(key);
      default:
        throw new AssertionError();
    }
  }

  private BstRangeOps() {}
}
