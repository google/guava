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

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * A local balancing policy for modified nodes in binary search trees.
 *
 * @author Louis Wasserman
 * @param <N> The type of the nodes in the trees that this {@code BstRebalancePolicy} can
 *        rebalance.
 */
@GwtCompatible
interface BstBalancePolicy<N extends BstNode<?, N>> {
  /**
   * Constructs a locally balanced tree around the key and value data in {@code source}, and the
   * subtrees {@code left} and {@code right}. It is guaranteed that the resulting tree will have
   * the same inorder traversal order as the subtree {@code left}, then the entry {@code source},
   * then the subtree {@code right}.
   */
  N balance(BstNodeFactory<N> nodeFactory, N source, @Nullable N left, @Nullable N right);

  /**
   * Constructs a locally balanced tree around the subtrees {@code left} and {@code right}. It is
   * guaranteed that the resulting tree will have the same inorder traversal order as the subtree
   * {@code left}, then the subtree {@code right}.
   */
  @Nullable
  N combine(BstNodeFactory<N> nodeFactory, @Nullable N left, @Nullable N right);
}
