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

import com.google.common.annotations.GwtCompatible;

/**
 * A rule for a local mutation to a binary search tree, that changes at most one entry. In addition
 * to specifying how it modifies a particular entry via a {@code BSTModifier}, it specifies a
 * {@link BSTBalancePolicy} for rebalancing the tree after the modification is performed and a
 * {@link BSTNodeFactory} for constructing newly rebalanced nodes.
 *
 * @author Louis Wasserman
 * @param <K> The key type of the nodes in binary search trees that this rule can modify.
 * @param <N> The type of the nodes in binary search trees that this rule can modify.
 */
@GwtCompatible
final class BSTMutationRule<K, N extends BSTNode<K, N>> {
  /**
   * Constructs a {@code BSTMutationRule} with the specified modifier, balance policy, and node
   * factory.
   */
  public static <K, N extends BSTNode<K, N>> BSTMutationRule<K, N> createRule(
      BSTModifier<K, N> modifier, BSTBalancePolicy<K, N> balancePolicy,
      BSTNodeFactory<K, N> nodeFactory) {
    return new BSTMutationRule<K, N>(modifier, balancePolicy, nodeFactory);
  }

  private final BSTModifier<K, N> modifier;
  private final BSTBalancePolicy<K, N> balancePolicy;
  private final BSTNodeFactory<K, N> nodeFactory;

  private BSTMutationRule(BSTModifier<K, N> modifier, BSTBalancePolicy<K, N> balancePolicy,
      BSTNodeFactory<K, N> nodeFactory) {
    this.balancePolicy = checkNotNull(balancePolicy);
    this.nodeFactory = checkNotNull(nodeFactory);
    this.modifier = checkNotNull(modifier);
  }

  /**
   * Returns the {@link BSTModifier} that specifies the change to a targeted entry in a binary
   * search tree.
   */
  public BSTModifier<K, N> getModifier() {
    return modifier;
  }

  /**
   * Returns the policy used to rebalance nodes in the tree after this modification has been
   * performed.
   */
  public BSTBalancePolicy<K, N> getBalancePolicy() {
    return balancePolicy;
  }

  /**
   * Returns the node factory used to create new nodes in the tree after this modification has been
   * performed.
   */
  public BSTNodeFactory<K, N> getNodeFactory() {
    return nodeFactory;
  }
}
