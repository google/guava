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
 * to specifying how it modifies a particular entry via a {@code BstModifier}, it specifies a
 * {@link BstBalancePolicy} for rebalancing the tree after the modification is performed and a
 * {@link BstNodeFactory} for constructing newly rebalanced nodes.
 *
 * @author Louis Wasserman
 * @param <K> The key type of the nodes in binary search trees that this rule can modify.
 * @param <N> The type of the nodes in binary search trees that this rule can modify.
 */
@GwtCompatible
final class BstMutationRule<K, N extends BstNode<K, N>> {
  /**
   * Constructs a {@code BstMutationRule} with the specified modifier, balance policy, and node
   * factory.
   */
  public static <K, N extends BstNode<K, N>> BstMutationRule<K, N> createRule(
      BstModifier<K, N> modifier, BstBalancePolicy<N> balancePolicy,
      BstNodeFactory<N> nodeFactory) {
    return new BstMutationRule<K, N>(modifier, balancePolicy, nodeFactory);
  }

  private final BstModifier<K, N> modifier;
  private final BstBalancePolicy<N> balancePolicy;
  private final BstNodeFactory<N> nodeFactory;

  private BstMutationRule(BstModifier<K, N> modifier, BstBalancePolicy<N> balancePolicy,
      BstNodeFactory<N> nodeFactory) {
    this.balancePolicy = checkNotNull(balancePolicy);
    this.nodeFactory = checkNotNull(nodeFactory);
    this.modifier = checkNotNull(modifier);
  }

  /**
   * Returns the {@link BstModifier} that specifies the change to a targeted entry in a binary
   * search tree.
   */
  public BstModifier<K, N> getModifier() {
    return modifier;
  }

  /**
   * Returns the policy used to rebalance nodes in the tree after this modification has been
   * performed.
   */
  public BstBalancePolicy<N> getBalancePolicy() {
    return balancePolicy;
  }

  /**
   * Returns the node factory used to create new nodes in the tree after this modification has been
   * performed.
   */
  public BstNodeFactory<N> getNodeFactory() {
    return nodeFactory;
  }
}
