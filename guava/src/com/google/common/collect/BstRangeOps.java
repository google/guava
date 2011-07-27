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
   * Returns the number of nodes in the specified tree in the specified range. Assumes that the
   * tree satisfies the binary search ordering property relative to {@code range.comparator()}.
   */
  public static <K, N extends BstNode<K, N>> int countInRange(
      GeneralRange<K> range, @Nullable N root) {
    if (root == null || range.isEmpty()) {
      return 0;
    }
    int totalCount = root.count();
    if (range.hasLowerBound()) {
      totalCount -= countTooLow(range, root);
    }
    if (range.hasUpperBound()) {
      totalCount -= countTooHigh(range, root);
    }
    return totalCount;
  }

  // Returns the number of nodes strictly below the specified range.
  private static <K, N extends BstNode<K, N>> int countTooLow(
      GeneralRange<K> range, @Nullable N root) {
    if (root == null) {
      return 0;
    } else if (range.tooLow(root.getKey())) {
      return 1 + countOrZero(root.childOrNull(LEFT)) + countTooLow(range, root.childOrNull(RIGHT));
    } else {
      return countTooLow(range, root.childOrNull(LEFT));
    }
  }

  // Returns the number of nodes strictly above the specified range.
  @Nullable
  private static <K, N extends BstNode<K, N>> int countTooHigh(
      GeneralRange<K> range, @Nullable N root) {
    if (root == null) {
      return 0;
    } else if (range.tooHigh(root.getKey())) {
      return 1 + countOrZero(root.childOrNull(RIGHT))
          + countTooHigh(range, root.childOrNull(LEFT));
    } else {
      return countTooHigh(range, root.childOrNull(RIGHT));
    }
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
   * Returns the leftmost path in the specified tree that is within the specified range.
   */
  @Nullable
  public static <K, N extends BstNode<K, N>, P extends BstPath<N, P>> P firstPath(
      GeneralRange<K> range, BstPathFactory<N, P> pathFactory, @Nullable N root) {
    checkNotNull(range);
    checkNotNull(pathFactory);
    return (root == null) ? null : firstPath(pathFactory.initialPath(root), range, pathFactory);
  }

  /**
   * Returns the rightmost path in the specified tree that is within the specified range.
   */
  @Nullable
  public static <K, N extends BstNode<K, N>, P extends BstPath<N, P>> P lastPath(
      GeneralRange<K> range, BstPathFactory<N, P> pathFactory, @Nullable N root) {
    checkNotNull(range);
    checkNotNull(pathFactory);
    return (root == null) ? null : lastPath(pathFactory.initialPath(root), range, pathFactory);
  }

  @Nullable
  private static <K, N extends BstNode<K, N>, P extends BstPath<N, P>> P firstPath(
      P currentPath, GeneralRange<K> range, BstPathFactory<N, P> pathFactory) {
    K tipKey = currentPath.getTip().getKey();
    if (range.tooLow(tipKey)) {
      if (currentPath.getTip().hasChild(RIGHT)) {
        return firstPath(pathFactory.extension(currentPath, RIGHT), range, pathFactory);
      } else {
        return null;
      }
    } else {
      P leftPath = currentPath.getTip().hasChild(LEFT)
          ? firstPath(pathFactory.extension(currentPath, LEFT), range, pathFactory)
          : null;
      if (leftPath != null) {
        return leftPath;
      } else {
        return range.tooHigh(tipKey) ? null : currentPath;
      }
    }
  }

  @Nullable
  private static <K, N extends BstNode<K, N>, P extends BstPath<N, P>> P lastPath(
      P currentPath, GeneralRange<K> range, BstPathFactory<N, P> pathFactory) {
    K tipKey = currentPath.getTip().getKey();
    if (range.tooHigh(tipKey)) {
      if (currentPath.getTip().hasChild(LEFT)) {
        return lastPath(pathFactory.extension(currentPath, LEFT), range, pathFactory);
      } else {
        return null;
      }
    } else {
      P rightPath = currentPath.getTip().hasChild(RIGHT)
          ? lastPath(pathFactory.extension(currentPath, RIGHT), range, pathFactory)
          : null;
      if (rightPath != null) {
        return rightPath;
      } else {
        return range.tooLow(tipKey) ? null : currentPath;
      }
    }
  }

  private BstRangeOps() {}
}
