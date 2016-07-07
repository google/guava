/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.graph;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.graph.GraphConstants.NOT_AVAILABLE_ON_UNDIRECTED;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.UnmodifiableIterator;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * An immutable {@link Collection} to represent the two (possibly equal, in the case of a self-loop)
 * endpoints of an edge in a graph.
 *
 * <p>If an {@link Endpoints} is directed, it is an ordered pair of nodes (source and target).
 * Otherwise, it is an unordered pair of nodes that can be accessed through the iterator.
 *
 * @author James Sexton
 * @since 20.0
 */
@Beta
public abstract class Endpoints<N> extends AbstractCollection<N> {
  private final N nodeA;
  private final N nodeB;

  private Endpoints(N nodeA, N nodeB) {
    this.nodeA = checkNotNull(nodeA);
    this.nodeB = checkNotNull(nodeB);
  }

  static <N> Endpoints<N> of(N nodeA, N nodeB, boolean isDirected) {
    return isDirected ? ofDirected(nodeA, nodeB) : ofUndirected(nodeA, nodeB);
  }

  /**
   * Returns an {@link Endpoints} representing the endpoints of a directed edge.
   */
  public static <N> Endpoints<N> ofDirected(N source, N target) {
    return new Directed<N>(source, target);
  }

  /**
   * Returns an {@link Endpoints} representing the endpoints of an undirected edge.
   */
  public static <N> Endpoints<N> ofUndirected(N nodeA, N nodeB) {
    return new Undirected<N>(nodeA, nodeB);
  }

  /**
   * Returns whether the nodes of this {@link Endpoints} are ordered. Generally, this is equal to
   * {@link Graph#isDirected()} of the graph that generated this {@link Endpoints}.
   */
  public abstract boolean isDirected();

  /**
   * If this {@link Endpoints} is directed, returns the node which is the source of the origin edge.
   *
   * @throws UnsupportedOperationException if this Endpoints is not directed
   */
  public abstract N source();

  /**
   * If this {@link Endpoints} is directed, returns the node which is the target of the origin edge.
   *
   * @throws UnsupportedOperationException if this Endpoints is not directed
   */
  public abstract N target();

  /**
   * If this {@link Endpoints} is directed, returns the {@link #source()};
   * otherwise, returns an arbitrary (but consistent) endpoint of the origin edge.
   */
  final N nodeA() {
    return nodeA;
  }

  /**
   * Returns the node that is adjacent to {@link #nodeA()} via the origin edge.
   * If this {@link Endpoints} is directed, this is equal to the {@link #target()}.
   */
  final N nodeB() {
    return nodeB;
  }

  /**
   * Returns the node that is adjacent to {@code node} via the origin edge.
   *
   * @throws IllegalArgumentException if the origin edge is not incident to {@code node}
   */
  public final N otherNode(Object node) {
    checkNotNull(node, "node");
    if (node.equals(nodeA())) {
      return nodeB();
    } else if (node.equals(nodeB())) {
      return nodeA();
    } else {
      throw new IllegalArgumentException(
          String.format("Endpoints %s does not contain node %s", this, node));
    }
  }

  @Override
  public final UnmodifiableIterator<N> iterator() {
    return new UnmodifiableIterator<N>() {
      private int pos = 0;

      @Override
      public boolean hasNext() {
        return pos < 2;
      }

      @Override
      public N next() {
        switch (pos++) {
          case 0:
            return nodeA;
          case 1:
            return nodeB;
          default:
            pos = 2;
            throw new NoSuchElementException();
        }
      }
    };
  }

  @Override
  public final int size() {
    return 2;
  }

  @Override
  public final boolean contains(Object obj) {
    return nodeA.equals(obj) || nodeB.equals(obj);
  }

  /**
   * If two {@link Endpoints}s are directed, the source and target must be equal to be considered
   * equal. If two {@link Endpoints}s are undirected, the unordered set of nodes must be equal to be
   * considered equal. Directed {@link Endpoints} are never equal to undirected {@link Endpoints}.
   */
  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();

  private static final class Directed<N> extends Endpoints<N> {
    private Directed(N source, N target) {
      super(source, target);
    }

    @Override
    public boolean isDirected() {
      return true;
    }

    @Override
    public N source() {
      return nodeA();
    }

    @Override
    public N target() {
      return nodeB();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Directed)) {
        return false;
      }

      Directed<?> other = (Directed<?>) obj;
      return source().equals(other.source()) && target().equals(other.target());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(source(), target());
    }

    @Override
    public String toString() {
      return String.format("<%s -> %s>", source(), target());
    }
  }

  private static final class Undirected<N> extends Endpoints<N> {
    private Undirected(N nodeA, N nodeB) {
      super(nodeA, nodeB);
    }

    @Override
    public boolean isDirected() {
      return false;
    }

    @Override
    public N source() {
      throw new UnsupportedOperationException(NOT_AVAILABLE_ON_UNDIRECTED);
    }

    @Override
    public N target() {
      throw new UnsupportedOperationException(NOT_AVAILABLE_ON_UNDIRECTED);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Undirected)) {
        return false;
      }

      Undirected<?> other = (Undirected<?>) obj;
      // Equivalent to the following simple implementation:
      // boolean condition1 = nodeA().equals(other.nodeA()) && nodeB().equals(other.nodeB());
      // boolean condition2 = nodeA().equals(other.nodeB()) && nodeB().equals(other.nodeA());
      // return condition1 || condition2;
      if (nodeA().equals(other.nodeA())) { // check condition1
        // Here's the tricky bit. We don't have to explicitly check for condition2 in this case.
        // Why? The second half of condition2 requires that nodeB equals other.nodeA.
        // We already know that nodeA equals other.nodeA. Combined with the earlier statement,
        // and the transitive property of equality, this implies that nodeA equals nodeB.
        // If nodeA equals nodeB, condition1 == condition2, so checking condition1 is sufficient.
        return nodeB().equals(other.nodeB());
      }
      return nodeA().equals(other.nodeB()) && nodeB().equals(other.nodeA()); // check condition2
    }

    @Override
    public int hashCode() {
      return nodeA().hashCode() ^ nodeB().hashCode();
    }
  }
}
