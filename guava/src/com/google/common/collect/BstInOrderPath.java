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

  private final BstSide sideExtension;
  private transient Optional<BstInOrderPath<N>> prevInOrder;
  private transient Optional<BstInOrderPath<N>> nextInOrder;

  private BstInOrderPath(
      N tip, @Nullable BstSide sideExtension, @Nullable BstInOrderPath<N> tail) {
    super(tip, tail);
    this.sideExtension = sideExtension;
    assert (sideExtension == null) == (tail == null);
  }

  private Optional<BstInOrderPath<N>> computeNextInOrder(BstSide side) {
    if (getTip().hasChild(side)) {
      BstInOrderPath<N> path = extension(this, side);
      BstSide otherSide = side.other();
      while (path.getTip().hasChild(otherSide)) {
        path = extension(path, otherSide);
      }
      return Optional.of(path);
    } else {
      BstInOrderPath<N> current = this;
      while (current.sideExtension == side) {
        current = current.getPrefix();
      }
      current = current.prefixOrNull();
      return Optional.fromNullable(current);
    }
  }

  private Optional<BstInOrderPath<N>> nextInOrder(BstSide side) {
    Optional<BstInOrderPath<N>> result;
    switch (side) {
      case LEFT:
        result = prevInOrder;
        return (result == null) ? prevInOrder = computeNextInOrder(side) : result;
      case RIGHT:
        result = nextInOrder;
        return (result == null) ? nextInOrder = computeNextInOrder(side) : result;
      default:
        throw new AssertionError();
    }
  }

  /**
   * Returns {@code true} if there is a next path in an in-order traversal in the given direction.
   */
  public boolean hasNext(BstSide side) {
    return nextInOrder(side).isPresent();
  }

  /**
   * Returns the next path in an in-order traversal in the given direction.
   *
   * @throws NoSuchElementException if this would be the last path in an in-order traversal
   */
  public BstInOrderPath<N> next(BstSide side) {
    if (!hasNext(side)) {
      throw new NoSuchElementException();
    }
    return nextInOrder(side).get();
  }

  /**
   * Returns the direction this path went in relative to its tail path, or {@code null} if this
   * path has no tail.
   */
  public BstSide getSideOfExtension() {
    return sideExtension;
  }
}
