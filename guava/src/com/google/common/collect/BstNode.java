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
import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstSide.RIGHT;

import com.google.common.annotations.GwtCompatible;

import java.util.Comparator;

import javax.annotation.Nullable;

/**
 * A reusable abstraction for a node in a binary search tree. Null keys are allowed.
 *
 * <p>The node is considered to be immutable. Any subclass with mutable fields must create a new
 * {@code BstNode} object upon any mutation, as the {@code Bst} classes assume that two nodes
 * {@code a} and {@code b} represent exactly the same tree if and only if {@code a == b}.
 *
 * <p>A {@code BstNode} can be considered to be an <i>entry</i>, containing a key and possibly some
 * value data, or it can be considered to be a <i>subtree</i>, representative of it and all its
 * descendants.
 *
 * @author Louis Wasserman
 * @param <K> The key type associated with this tree.
 * @param <N> The type of the nodes in this tree.
 */
@GwtCompatible
class BstNode<K, N extends BstNode<K, N>> {
  /**
   * The key on which this binary search tree is ordered. All descendants of the left subtree of
   * this node must have keys strictly less than {@code this.key}.
   */
  private final K key;

  /**
   * The left child of this node. A null value indicates that this node has no left child.
   */
  @Nullable
  private final N left;

  /**
   * The right child of this node. A null value indicates that this node has no right child.
   */
  @Nullable
  private final N right;

  BstNode(@Nullable K key, @Nullable N left, @Nullable N right) {
    this.key = key;
    this.left = left;
    this.right = right;
  }

  /**
   * Returns the ordered key associated with this node.
   */
  @Nullable
  public final K getKey() {
    return key;
  }

  /**
   * Returns the child on the specified side, or {@code null} if there is no such child.
   */
  @Nullable
  public final N childOrNull(BstSide side) {
    switch (side) {
      case LEFT:
        return left;
      case RIGHT:
        return right;
      default:
        throw new AssertionError();
    }
  }

  /**
   * Returns {@code true} if this node has a child on the specified side.
   */
  public final boolean hasChild(BstSide side) {
    return childOrNull(side) != null;
  }

  /**
   * Returns this node's child on the specified side.
   *
   * @throws IllegalStateException if this node has no such child
   */
  public final N getChild(BstSide side) {
    N child = childOrNull(side);
    checkState(child != null);
    return child;
  }

  /**
   * Returns {@code true} if the traditional binary search tree ordering invariant holds with
   * respect to the specified {@code comparator}.
   */
  protected final boolean orderingInvariantHolds(Comparator<? super K> comparator) {
    checkNotNull(comparator);
    boolean result = true;
    if (hasChild(LEFT)) {
      result &= comparator.compare(getChild(LEFT).getKey(), key) < 0;
    }
    if (hasChild(RIGHT)) {
      result &= comparator.compare(getChild(RIGHT).getKey(), key) > 0;
    }
    return result;
  }
}
