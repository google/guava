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
import static com.google.common.collect.BstModificationResult.ModificationType.IDENTITY;
import static com.google.common.collect.BstModificationResult.ModificationType.REBUILDING_CHANGE;
import static com.google.common.collect.BstModificationResult.ModificationType.REBALANCING_CHANGE;
import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstSide.RIGHT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.BstModificationResult.ModificationType;

import javax.annotation.Nullable;

/**
 * The result of a mutation operation performed at a single location in a binary search tree.
 *
 * @author Louis Wasserman
 * @param <K> The key type of the nodes in the modified binary search tree.
 * @param <N> The type of the nodes in the modified binary search tree.
 */
@GwtCompatible
final class BstMutationResult<K, N extends BstNode<K, N>> {
  /**
   * Creates a {@code BstMutationResult}.
   *
   * @param targetKey The key targeted for modification. If {@code originalTarget} or {@code
   *        changedTarget} are non-null, their keys must compare as equal to {@code targetKey}.
   * @param originalRoot The root of the subtree that was modified.
   * @param changedRoot The root of the subtree, after the modification and any rebalancing.
   * @param modificationResult The result of the local modification to an entry.
   */
  public static <K, N extends BstNode<K, N>> BstMutationResult<K, N> mutationResult(
      @Nullable K targetKey, @Nullable N originalRoot, @Nullable N changedRoot,
      BstModificationResult<N> modificationResult) {
    return new BstMutationResult<K, N>(targetKey, originalRoot, changedRoot, modificationResult);
  }

  private final K targetKey;

  @Nullable
  private N originalRoot;

  @Nullable
  private N changedRoot;
  
  private final BstModificationResult<N> modificationResult;

  private BstMutationResult(@Nullable K targetKey, @Nullable N originalRoot,
      @Nullable N changedRoot, BstModificationResult<N> modificationResult) {
    this.targetKey = targetKey;
    this.originalRoot = originalRoot;
    this.changedRoot = changedRoot;
    this.modificationResult = checkNotNull(modificationResult);
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
    return modificationResult.getOriginalTarget();
  }

  /**
   * Returns the result of the modification to {@link #getOriginalTarget()}. This should not be
   * treated as a subtree, but only as an entry, and no guarantees are made about its children when
   * viewed as a subtree.
   */
  @Nullable
  public N getChangedTarget() {
    return modificationResult.getChangedTarget();
  }

  ModificationType modificationType() {
    return modificationResult.getType();
  }

  /**
   * If this mutation was to an immediate child subtree of the specified root on the specified
   * side, returns the {@code BstMutationResult} of applying the mutation to the appropriate child
   * of the specified root and rebalancing using the specified mutation rule.
   */
  public BstMutationResult<K, N> lift(N liftOriginalRoot, BstSide side,
      BstNodeFactory<N> nodeFactory, BstBalancePolicy<N> balancePolicy) {
    assert liftOriginalRoot != null & side != null & nodeFactory != null & balancePolicy != null;
    switch (modificationType()) {
      case IDENTITY:
        this.originalRoot = this.changedRoot = liftOriginalRoot;
        return this;
      case REBUILDING_CHANGE:
      case REBALANCING_CHANGE:
        this.originalRoot = liftOriginalRoot;
        N resultLeft = liftOriginalRoot.childOrNull(LEFT);
        N resultRight = liftOriginalRoot.childOrNull(RIGHT);
        switch (side) {
          case LEFT:
            resultLeft = changedRoot;
            break;
          case RIGHT:
            resultRight = changedRoot;
            break;
          default:
            throw new AssertionError();
        }
        if (modificationType() == REBUILDING_CHANGE) {
          this.changedRoot = nodeFactory.createNode(liftOriginalRoot, resultLeft, resultRight);
        } else {
          this.changedRoot =
              balancePolicy.balance(nodeFactory, liftOriginalRoot, resultLeft, resultRight);
        }
        return this;
      default:
        throw new AssertionError();
    }
  }
}
