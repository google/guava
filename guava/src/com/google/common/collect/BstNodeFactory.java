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
 * A factory for copying nodes in binary search trees with different children.
 *
 * <p>Typically, nodes will carry more information than the fields in the {@link BstNode} class,
 * often some kind of value or some aggregate data for the subtree. This factory is responsible for
 * copying this additional data between nodes.
 *
 * @author Louis Wasserman
 * @param <N> The type of the tree nodes constructed with this {@code BstNodeFactory}.
 */
@GwtCompatible
abstract class BstNodeFactory<N extends BstNode<?, N>> {
  /**
   * Returns a new {@code N} with the key and value data from {@code source}, with left child
   * {@code left}, and right child {@code right}. If {@code left} or {@code right} is null, the
   * returned node will not have a child on the corresponding side.
   */
  public abstract N createNode(N source, @Nullable N left, @Nullable N right);

  /**
   * Returns a new {@code N} with the key and value data from {@code source} that is a leaf.
   */
  public final N createLeaf(N source) {
    return createNode(source, null, null);
  }
}
