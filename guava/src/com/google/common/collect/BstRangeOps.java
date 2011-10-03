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
 * A utility class with operations on binary search trees that operate on some interval.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class BstRangeOps {
  /**
   * Returns the total value of the specified aggregation function on the specified tree restricted
   * to the specified range. Assumes that the tree satisfies the binary search ordering property
   * relative to {@code range.comparator()}.
   */
  public static <K, N extends BstNode<K, N>> long totalInRange(
      BstAggregate<? super N> aggregate, GeneralRange<K> range, @Nullable N root) {
    checkNotNull(aggregate);
    checkNotNull(range);
    if (root == null || range.isEmpty()) {
      return 0;
    }
    long total = aggregate.treeValue(root);
    if (range.hasLowerBound()) {
      total -= totalBeyondRangeToSide(aggregate, range, LEFT, root);
    }
    if (range.hasUpperBound()) {
      total -= totalBeyondRangeToSide(aggregate, range, RIGHT, root);
    }
    return total;
  }

  // Returns total value strictly to the specified side of the specified range.
  private static <K, N extends BstNode<K, N>> long totalBeyondRangeToSide(
      BstAggregate<? super N> aggregate, GeneralRange<K> range, BstSide side, @Nullable N root) {
    long accum = 0;
    while (root != null) {
      if (beyond(range, root.getKey(), side)) {
        accum += aggregate.entryValue(root);
        accum += aggregate.treeValue(root.childOrNull(side));
        root = root.childOrNull(side.other());
      } else {
        root = root.childOrNull(side);
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
    N higher = range.hasUpperBound()
        ? subTreeBeyondRangeToSide(range, balancePolicy, nodeFactory, RIGHT, root)
        : null;
    N lower = range.hasLowerBound()
        ? subTreeBeyondRangeToSide(range, balancePolicy, nodeFactory, LEFT, root)
        : null;
    return balancePolicy.combine(nodeFactory, lower, higher);
  }

  /*
   * Returns a balanced tree containing all nodes in the specified tree that are strictly to the
   * specified side of the specified range.
   */
  @Nullable
  private static <K, N extends BstNode<K, N>> N subTreeBeyondRangeToSide(GeneralRange<K> range,
      BstBalancePolicy<N> balancePolicy, BstNodeFactory<N> nodeFactory, BstSide side,
      @Nullable N root) {
    if (root == null) {
      return null;
    }
    if (beyond(range, root.getKey(), side)) {
      N left = root.childOrNull(LEFT);
      N right = root.childOrNull(RIGHT);
      switch (side) {
        case LEFT:
          right = subTreeBeyondRangeToSide(range, balancePolicy, nodeFactory, LEFT, right);
          break;
        case RIGHT:
          left = subTreeBeyondRangeToSide(range, balancePolicy, nodeFactory, RIGHT, left);
          break;
        default:
          throw new AssertionError();
      }
      return balancePolicy.balance(nodeFactory, root, left, right);
    } else {
      return subTreeBeyondRangeToSide(
          range, balancePolicy, nodeFactory, side, root.childOrNull(side));
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
  public static <K> boolean beyond(GeneralRange<K> range, @Nullable K key, BstSide side) {
    checkNotNull(range);
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
