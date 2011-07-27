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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * A path to a node in a binary search tree, originating at the root.
 *
 * @author Louis Wasserman
 * @param <N> The type of nodes in this binary search tree.
 * @param <P> This path type, and the path type of all suffix paths.
 */
@GwtCompatible
abstract class BstPath<N extends BstNode<?, N>, P extends BstPath<N, P>> {
  private final N tip;
  @Nullable
  private final P prefix;

  BstPath(N tip, @Nullable P prefix) {
    this.tip = checkNotNull(tip);
    this.prefix = prefix;
  }

  /**
   * Return the end of this {@code BstPath}, the deepest node in the path.
   */
  public final N getTip() {
    return tip;
  }

  /**
   * Returns {@code true} if this path has a prefix.
   */
  public final boolean hasPrefix() {
    return prefix != null;
  }

  /**
   * Returns the prefix of this path, which reaches to the parent of the end of this path. Returns
   * {@code null} if this path has no prefix.
   */
  @Nullable
  public final P prefixOrNull() {
    return prefix;
  }

  /**
   * Returns the prefix of this path, which reaches to the parent of the end of this path.
   *
   * @throws IllegalStateException if this path has no prefix.
   */
  public final P getPrefix() {
    checkState(hasPrefix());
    return prefix;
  }
}
