/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;

import java.util.LinkedList;
import java.util.Iterator;

/**
 * A variant of {@link TreeTraverser} for binary trees, providing additional traversals specific to
 * binary trees.
 *
 * @author Louis Wasserman
 */
public abstract class BinaryTreeTraverser<T> extends TreeTraverser<T> {
  // TODO(user): make this GWT-compatible when we've checked in ArrayDeque and BitSet emulation

  /**
   * Returns the left child of the specified node, or {@link Optional#absent()} if the specified
   * node has no left child.
   */
  public abstract Optional<T> leftChild(T root);

  /**
   * Returns the right child of the specified node, or {@link Optional#absent()} if the specified
   * node has no right child.
   */
  public abstract Optional<T> rightChild(T root);

  /**
   * Returns the children of this node, in left-to-right order.
   */
  @Override
  public final Iterable<T> children(final T root) {
    checkNotNull(root);
    return new FluentIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new AbstractIterator<T>() {
          boolean doneLeft;
          boolean doneRight;

          @Override
          protected T computeNext() {
            if (!doneLeft) {
              doneLeft = true;
              Optional<T> left = leftChild(root);
              if (left.isPresent()) {
                return left.get();
              }
            }
            if (!doneRight) {
              doneRight = true;
              Optional<T> right = rightChild(root);
              if (right.isPresent()) {
                return right.get();
              }
            }
            return endOfData();
          }
        };
      }
    };
  }

  // TODO(user): see if any significant optimizations are possible for breadthFirstIterator

  public final FluentIterable<T> inOrderTraversal(final T root) {
    checkNotNull(root);
    return new FluentIterable<T>() {
      @Override
      public UnmodifiableIterator<T> iterator() {
        return new InOrderIterator(root);
      }
    };
  }

  private static final class InOrderNode<T> {
    final T node;
    boolean hasExpandedLeft;

    InOrderNode(T node) {
      this.node = checkNotNull(node);
      this.hasExpandedLeft = false;
    }
  }

  private final class InOrderIterator extends AbstractIterator<T> {
    private final LinkedList<InOrderNode<T>> stack;

    InOrderIterator(T root) {
      this.stack = Lists.newLinkedList();
      stack.addLast(new InOrderNode<T>(root));
    }

    @Override
    protected T computeNext() {
      while (!stack.isEmpty()) {
        InOrderNode<T> inOrderNode = stack.getLast();
        if (inOrderNode.hasExpandedLeft) {
          stack.removeLast();
          pushIfPresent(rightChild(inOrderNode.node));
          return inOrderNode.node;
        } else {
          inOrderNode.hasExpandedLeft = true;
          pushIfPresent(leftChild(inOrderNode.node));
        }
      }
      return endOfData();
    }

    private void pushIfPresent(Optional<T> node) {
      if (node.isPresent()) {
        stack.addLast(new InOrderNode<T>(node.get()));
      }
    }
  }
}
