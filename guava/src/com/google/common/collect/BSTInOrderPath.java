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
import com.google.common.base.Optional;

import java.util.NoSuchElementException;

import javax.annotation.Nullable;

/**
 * A {@code BSTPath} supporting inorder traversal operations.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class BSTInOrderPath<K, N extends BSTNode<K, N>> extends BSTPath<
    K, N, BSTInOrderPath<K, N>> {
  /**
   * The factory to use to construct {@code BSTInOrderPath} values.
   */
  public static <K, N extends BSTNode<K, N>> BSTPathFactory<
      K, N, BSTInOrderPath<K, N>> inOrderFactory() {
    return new BSTPathFactory<K, N, BSTInOrderPath<K, N>>() {
      @Override
      public BSTInOrderPath<K, N> extension(BSTInOrderPath<K, N> path, BSTSide side) {
        return BSTInOrderPath.extension(path, side);
      }

      @Override
      public BSTInOrderPath<K, N> initialPath(N root) {
        return new BSTInOrderPath<K, N>(root, null, null);
      }
    };
  }

  private static <K, N extends BSTNode<K, N>> BSTInOrderPath<K, N> extension(
      BSTInOrderPath<K, N> path, BSTSide side) {
    checkNotNull(path);
    N tip = path.getTip();
    return new BSTInOrderPath<K, N>(tip.getChild(side), side, path);
  }

  private final BSTSide side;
  private transient Optional<BSTInOrderPath<K, N>> prevInOrder;
  private transient Optional<BSTInOrderPath<K, N>> nextInOrder;

  private BSTInOrderPath(N tip, @Nullable BSTSide side, @Nullable BSTInOrderPath<K, N> tail) {
    super(tip, tail);
    this.side = side;
    assert (side == null) == (tail == null);
  }

  private Optional<BSTInOrderPath<K, N>> computeNextInOrder() {
    if (getTip().hasChild(RIGHT)) {
      BSTInOrderPath<K, N> path = extension(this, RIGHT);
      while (path.getTip().hasChild(LEFT)) {
        path = extension(path, LEFT);
      }
      return Optional.of(path);
    } else {
      BSTInOrderPath<K, N> current = this;
      while (current.side == RIGHT) {
        current = current.getPrefix();
      }
      current = current.prefixOrNull();
      if (current != null) {
        current.prevInOrder = Optional.of(this);
      }
      return Optional.fromNullable(current);
    }
  }

  private Optional<BSTInOrderPath<K, N>> computePrevInOrder() {
    if (getTip().hasChild(LEFT)) {
      BSTInOrderPath<K, N> path = extension(this, LEFT);
      while (path.getTip().hasChild(RIGHT)) {
        path = extension(path, RIGHT);
      }
      return Optional.of(path);
    } else {
      BSTInOrderPath<K, N> current = this;
      while (current.side == LEFT) {
        current = current.getPrefix();
      }
      current = current.prefixOrNull();
      if (current != null) {
        current.nextInOrder = Optional.of(this);
      }
      return Optional.fromNullable(current);
    }
  }

  private Optional<BSTInOrderPath<K, N>> nextInOrder() {
    Optional<BSTInOrderPath<K, N>> result = nextInOrder;
    return (result == null) ? nextInOrder = computeNextInOrder() : result;
  }

  private Optional<BSTInOrderPath<K, N>> prevInOrder() {
    Optional<BSTInOrderPath<K, N>> result = prevInOrder;
    return (result == null) ? prevInOrder = computePrevInOrder() : result;
  }

  /**
   * Returns {@code true} if there is a next path in an in-order traversal.
   */
  public boolean hasNext() {
    return nextInOrder().isPresent();
  }

  /**
   * Returns {@code true} if there is a previous path in an in-order traversal.
   */
  public boolean hasPrev() {
    return prevInOrder().isPresent();
  }

  /**
   * Returns the next path in an in-order traversal.
   *
   * @throws NoSuchElementException if this would be the last path in an in-order traversal
   */
  public BSTInOrderPath<K, N> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return nextInOrder().get();
  }

  /**
   * Returns the previous path in an in-order traversal.
   *
   * @throws NoSuchElementException if this would be the first path in an in-order traversal
   */
  public BSTInOrderPath<K, N> prev() {
    if (!hasPrev()) {
      throw new NoSuchElementException();
    }
    return prevInOrder().get();
  }

  /**
   * Returns the direction this path went in relative to its tail path, or {@code null} if this
   * path has no tail.
   */
  public BSTSide getSide() {
    return side;
  }
}
