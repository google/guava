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
import static com.google.common.collect.BstOperations.extractMax;
import static com.google.common.collect.BstOperations.extractMin;
import static com.google.common.collect.BstOperations.insertMax;
import static com.google.common.collect.BstOperations.insertMin;
import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstSide.RIGHT;

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * A tree-size-based set of balancing policies, based on <a
 * href="http://www.swiss.ai.mit.edu/~adams/BB/"> Stephen Adams, "Efficient sets: a balancing
 * act."</a>.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class BstCountBasedBalancePolicies {
  private BstCountBasedBalancePolicies() {}

  private static final int SINGLE_ROTATE_RATIO = 4;
  private static final int SECOND_ROTATE_RATIO = 2;

  /**
   * Returns a balance policy that does no balancing or the bare minimum (for {@code combine}).
   */
  public static <N extends BstNode<?, N>> BstBalancePolicy<N> noRebalancePolicy(
      final BstAggregate<N> countAggregate) {
    checkNotNull(countAggregate);
    return new BstBalancePolicy<N>() {
      @Override
      public N balance(
          BstNodeFactory<N> nodeFactory, N source, @Nullable N left, @Nullable N right) {
        return checkNotNull(nodeFactory).createNode(source, left, right);
      }

      @Nullable
      @Override
      public N combine(BstNodeFactory<N> nodeFactory, @Nullable N left, @Nullable N right) {
        if (left == null) {
          return right;
        } else if (right == null) {
          return left;
        } else if (countAggregate.treeValue(left) > countAggregate.treeValue(right)) {
          return nodeFactory.createNode(
              left, left.childOrNull(LEFT), combine(nodeFactory, left.childOrNull(RIGHT), right));
        } else {
          return nodeFactory.createNode(right, combine(nodeFactory, left, right.childOrNull(LEFT)),
              right.childOrNull(RIGHT));
        }
      }
    };
  }

  /**
   * Returns a balance policy that expects the sizes of each side to be at most one node (added or
   * removed) away from being balanced. {@code balance} takes {@code O(1)} time, and {@code
   * combine} takes {@code O(log n)} time.
   */
  public static <K, N extends BstNode<K, N>> BstBalancePolicy<N> singleRebalancePolicy(
      final BstAggregate<N> countAggregate) {
    checkNotNull(countAggregate);
    return new BstBalancePolicy<N>() {
      @Override
      public N balance(
          BstNodeFactory<N> nodeFactory, N source, @Nullable N left, @Nullable N right) {
        long countL = countAggregate.treeValue(left);
        long countR = countAggregate.treeValue(right);
        if (countL + countR > 1) {
          if (countR >= SINGLE_ROTATE_RATIO * countL) {
            return rotateL(nodeFactory, source, left, right);
          } else if (countL >= SINGLE_ROTATE_RATIO * countR) {
            return rotateR(nodeFactory, source, left, right);
          }
        }
        return nodeFactory.createNode(source, left, right);
      }

      private N rotateL(BstNodeFactory<N> nodeFactory, N source, @Nullable N left, N right) {
        checkNotNull(right);
        N rl = right.childOrNull(LEFT);
        N rr = right.childOrNull(RIGHT);
        if (countAggregate.treeValue(rl) >= SECOND_ROTATE_RATIO * countAggregate.treeValue(rr)) {
          right = singleR(nodeFactory, right, rl, rr);
        }
        return singleL(nodeFactory, source, left, right);
      }

      private N rotateR(BstNodeFactory<N> nodeFactory, N source, N left, @Nullable N right) {
        checkNotNull(left);
        N lr = left.childOrNull(RIGHT);
        N ll = left.childOrNull(LEFT);
        if (countAggregate.treeValue(lr) >= SECOND_ROTATE_RATIO * countAggregate.treeValue(ll)) {
          left = singleL(nodeFactory, left, ll, lr);
        }
        return singleR(nodeFactory, source, left, right);
      }

      private N singleL(BstNodeFactory<N> nodeFactory, N source, @Nullable N left, N right) {
        checkNotNull(right);
        return nodeFactory.createNode(right,
            nodeFactory.createNode(source, left, right.childOrNull(LEFT)),
            right.childOrNull(RIGHT));
      }

      private N singleR(BstNodeFactory<N> nodeFactory, N source, N left, @Nullable N right) {
        checkNotNull(left);
        return nodeFactory.createNode(left, left.childOrNull(LEFT),
            nodeFactory.createNode(source, left.childOrNull(RIGHT), right));
      }

      @Nullable
      @Override
      public N combine(BstNodeFactory<N> nodeFactory, @Nullable N left, @Nullable N right) {
        if (left == null) {
          return right;
        } else if (right == null) {
          return left;
        }
        N newRootSource;
        if (countAggregate.treeValue(left) > countAggregate.treeValue(right)) {
          BstMutationResult<K, N> extractLeftMax = extractMax(left, nodeFactory, this);
          newRootSource = extractLeftMax.getOriginalTarget();
          left = extractLeftMax.getChangedRoot();
        } else {
          BstMutationResult<K, N> extractRightMin = extractMin(right, nodeFactory, this);
          newRootSource = extractRightMin.getOriginalTarget();
          right = extractRightMin.getChangedRoot();
        }
        return nodeFactory.createNode(newRootSource, left, right);
      }
    };
  }

  /**
   * Returns a balance policy that makes no assumptions on the relative balance of the two sides
   * and performs a full rebalancing as necessary. Both {@code balance} and {@code combine} take
   * {@code O(log n)} time.
   */
  public static <K, N extends BstNode<K, N>> BstBalancePolicy<N> fullRebalancePolicy(
      final BstAggregate<N> countAggregate) {
    checkNotNull(countAggregate);
    final BstBalancePolicy<N> singleBalancePolicy =
        BstCountBasedBalancePolicies.<K, N>singleRebalancePolicy(countAggregate);
    return new BstBalancePolicy<N>() {
      @Override
      public N balance(
          BstNodeFactory<N> nodeFactory, N source, @Nullable N left, @Nullable N right) {
        if (left == null) {
          return insertMin(right, source, nodeFactory, singleBalancePolicy);
        } else if (right == null) {
          return insertMax(left, source, nodeFactory, singleBalancePolicy);
        }
        long countL = countAggregate.treeValue(left);
        long countR = countAggregate.treeValue(right);
        if (SINGLE_ROTATE_RATIO * countL <= countR) {
          N resultLeft = balance(nodeFactory, source, left, right.childOrNull(LEFT));
          return singleBalancePolicy.balance(
              nodeFactory, right, resultLeft, right.childOrNull(RIGHT));
        } else if (SINGLE_ROTATE_RATIO * countR <= countL) {
          N resultRight = balance(nodeFactory, source, left.childOrNull(RIGHT), right);
          return singleBalancePolicy.balance(
              nodeFactory, left, left.childOrNull(LEFT), resultRight);
        } else {
          return nodeFactory.createNode(source, left, right);
        }
      }

      @Nullable
      @Override
      public N combine(BstNodeFactory<N> nodeFactory, @Nullable N left, @Nullable N right) {
        if (left == null) {
          return right;
        } else if (right == null) {
          return left;
        }
        long countL = countAggregate.treeValue(left);
        long countR = countAggregate.treeValue(right);
        if (SINGLE_ROTATE_RATIO * countL <= countR) {
          N resultLeft = combine(nodeFactory, left, right.childOrNull(LEFT));
          return singleBalancePolicy.balance(
              nodeFactory, right, resultLeft, right.childOrNull(RIGHT));
        } else if (SINGLE_ROTATE_RATIO * countR <= countL) {
          N resultRight = combine(nodeFactory, left.childOrNull(RIGHT), right);
          return singleBalancePolicy.balance(
              nodeFactory, left, left.childOrNull(LEFT), resultRight);
        } else {
          return singleBalancePolicy.combine(nodeFactory, left, right);
        }
      }
    };
  }
}
