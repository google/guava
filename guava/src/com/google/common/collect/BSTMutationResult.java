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
import static com.google.common.collect.BSTSide.LEFT;
import static com.google.common.collect.BSTSide.RIGHT;

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * The result of a mutation operation performed at a single location in a binary search tree.
 *
 * @author Louis Wasserman
 * @param <K> The key type of the nodes in the modified binary search tree.
 * @param <N> The type of the nodes in the modified binary search tree.
 */
@GwtCompatible
final class BSTMutationResult<K, N extends BSTNode<K, N>> {
  /**
   * Creates a {@code BSTMutationResult}.
   *
   * @param targetKey The key targeted for modification. If {@code originalTarget} or {@code
   *        changedTarget} are non-null, their keys must compare as equal to {@code targetKey}.
   * @param originalRoot The root of the subtree that was modified.
   * @param changedRoot The root of the subtree, after the modification and any rebalancing.
   * @param originalTarget The node in the original subtree with key {@code targetKey}, if any.
   * @param changedTarget The node with key {@code targetKey} after the modification.
   */
  public static <K, N extends BSTNode<K, N>> BSTMutationResult<K, N> mutationResult(K targetKey,
      @Nullable N originalRoot, @Nullable N changedRoot, @Nullable N originalTarget,
      @Nullable N changedTarget) {
    return new BSTMutationResult<K, N>(
        targetKey, originalRoot, changedRoot, originalTarget, changedTarget);
  }

  /**
   * Returns the identity mutation.
   *
   * @param targetKey The key targeted for modification.
   * @param root The subtree that was to be modified.
   * @param target The node in the subtree with key {@code targetKey}, if any.
   */
  public static <K, N extends BSTNode<K, N>> BSTMutationResult<K, N> identity(
      K targetKey, @Nullable N root, @Nullable N target) {
    return mutationResult(targetKey, root, root, target, target);
  }

  private final K targetKey;

  @Nullable
  private final N originalRoot;

  @Nullable
  private final N changedRoot;

  @Nullable
  private final N originalTarget;

  @Nullable
  private final N changedTarget;

  private BSTMutationResult(K targetKey, @Nullable N originalRoot, @Nullable N changedRoot,
      @Nullable N originalTarget, @Nullable N changedTarget) {
    assert (originalTarget == null | originalRoot != null);
    assert (changedTarget == null | changedRoot != null);
    assert ((originalRoot == changedRoot) == (originalTarget == changedTarget));
    this.targetKey = checkNotNull(targetKey);
    this.originalRoot = originalRoot;
    this.changedRoot = changedRoot;
    this.originalTarget = originalTarget;
    this.changedTarget = changedTarget;
  }

  /**
   * Returns the key which was the target of this modification.
   */
  public K getTargetKey() {
    return targetKey;
  }

  /**
   * Returns the root of the subtree that was modified.
   */
  @Nullable
  public N getOriginalRoot() {
    return originalRoot;
  }

  /**
   * Returns the root of the subtree, after the modification and any rebalancing was performed.
   */
  @Nullable
  public N getChangedRoot() {
    return changedRoot;
  }

  /**
   * Returns the entry in the original subtree with key {@code targetKey}, if any. This should not
   * be treated as a subtree, but only as an entry, and no guarantees are made about its children
   * when viewed as a subtree.
   */
  @Nullable
  public N getOriginalTarget() {
    return originalTarget;
  }

  /**
   * Returns the result of the modification to {@link #getOriginalTarget()}. This should not be
   * treated as a subtree, but only as an entry, and no guarantees are made about its children when
   * viewed as a subtree.
   */
  @Nullable
  public N getChangedTarget() {
    return changedTarget;
  }

  /**
   * Returns {@code true} if this mutation represents an identity operation, which is to say, no
   * changes were made at all.
   */
  public boolean isIdentity() {
    return originalTarget == changedTarget;
  }

  /**
   * If this mutation was to an immediate child subtree of the specified root on the specified
   * side, returns the {@code BSTMutationResult} of applying the mutation to the appropriate child
   * of the specified root and rebalancing using the specified mutation rule.
   */
  public BSTMutationResult<K, N> lift(N liftOriginalRoot, BSTSide side,
      BSTNodeFactory<N> nodeFactory, BSTBalancePolicy<N> balancePolicy) {
    checkNotNull(liftOriginalRoot);
    checkNotNull(side);
    checkNotNull(nodeFactory);
    checkNotNull(balancePolicy);
    if (isIdentity()) {
      return identity(targetKey, liftOriginalRoot, originalTarget);
    }

    N resultLeft = liftOriginalRoot.childOrNull(LEFT);
    N resultRight = liftOriginalRoot.childOrNull(RIGHT);

    switch (side) {
      case LEFT:
        assert originalRoot == resultLeft;
        resultLeft = changedRoot;
        break;
      case RIGHT:
        assert originalRoot == resultRight;
        resultRight = changedRoot;
        break;
      default:
        throw new AssertionError();
    }

    N liftChangedRoot =
        balancePolicy.balance(nodeFactory, liftOriginalRoot, resultLeft, resultRight);
    return mutationResult(
        targetKey, liftOriginalRoot, liftChangedRoot, originalTarget, changedTarget);
  }
}
