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
import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstSide.RIGHT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Optional;

import java.util.NoSuchElementException;

import javax.annotation.Nullable;

/**
 * A {@code BstPath} supporting inorder traversal operations.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class BstInOrderPath<N extends BstNode<?, N>> extends BstPath<N, BstInOrderPath<N>> {
  /**
   * The factory to use to construct {@code BstInOrderPath} values.
   */
  public static <N extends BstNode<?, N>> BstPathFactory<N, BstInOrderPath<N>> inOrderFactory() {
    return new BstPathFactory<N, BstInOrderPath<N>>() {
      @Override
      public BstInOrderPath<N> extension(BstInOrderPath<N> path, BstSide side) {
        return BstInOrderPath.extension(path, side);
      }

      @Override
      public BstInOrderPath<N> initialPath(N root) {
        return new BstInOrderPath<N>(root, null, null);
      }
    };
  }

  private static <N extends BstNode<?, N>> BstInOrderPath<N> extension(
      BstInOrderPath<N> path, BstSide side) {
    checkNotNull(path);
    N tip = path.getTip();
    return new BstInOrderPath<N>(tip.getChild(side), side, path);
  }

  private final BstSide side;
  private transient Optional<BstInOrderPath<N>> prevInOrder;
  private transient Optional<BstInOrderPath<N>> nextInOrder;

  private BstInOrderPath(N tip, @Nullable BstSide side, @Nullable BstInOrderPath<N> tail) {
    super(tip, tail);
    this.side = side;
    assert (side == null) == (tail == null);
  }

  private Optional<BstInOrderPath<N>> computeNextInOrder() {
    if (getTip().hasChild(RIGHT)) {
      BstInOrderPath<N> path = extension(this, RIGHT);
      while (path.getTip().hasChild(LEFT)) {
        path = extension(path, LEFT);
      }
      return Optional.of(path);
    } else {
      BstInOrderPath<N> current = this;
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

  private Optional<BstInOrderPath<N>> computePrevInOrder() {
    if (getTip().hasChild(LEFT)) {
      BstInOrderPath<N> path = extension(this, LEFT);
      while (path.getTip().hasChild(RIGHT)) {
        path = extension(path, RIGHT);
      }
      return Optional.of(path);
    } else {
      BstInOrderPath<N> current = this;
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

  private Optional<BstInOrderPath<N>> nextInOrder() {
    Optional<BstInOrderPath<N>> result = nextInOrder;
    return (result == null) ? nextInOrder = computeNextInOrder() : result;
  }

  private Optional<BstInOrderPath<N>> prevInOrder() {
    Optional<BstInOrderPath<N>> result = prevInOrder;
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
  public BstInOrderPath<N> next() {
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
  public BstInOrderPath<N> prev() {
    if (!hasPrev()) {
      throw new NoSuchElementException();
    }
    return prevInOrder().get();
  }

  /**
   * Returns the direction this path went in relative to its tail path, or {@code null} if this
   * path has no tail.
   */
  public BstSide getSide() {
    return side;
  }
}
