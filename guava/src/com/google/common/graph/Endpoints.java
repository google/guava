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
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

/**
 * An immutable pair representing the two (possibly equal, in the case of a self-loop) endpoints
 * of an edge in a graph. The {@link Endpoints} of a directed edge are an ordered pair of nodes
 * (source and target). The {@link Endpoints} of an undirected edge are an unordered pair of nodes.
 *
 * @author James Sexton
 * @since 20.0
 */
@Beta
public abstract class Endpoints<N> implements Iterable<N> {
  private final N nodeA;
  private final N nodeB;

  private Endpoints(N nodeA, N nodeB) {
    this.nodeA = checkNotNull(nodeA);
    this.nodeB = checkNotNull(nodeB);
  }

  /**
   * Returns {@link Endpoints} representing the endpoints of an edge in {@code graph}.
   */
  public static <N> Endpoints<N> of(Graph<?, ?> graph, N nodeA, N nodeB) {
    return graph.isDirected() ? ofDirected(nodeA, nodeB) : ofUndirected(nodeA, nodeB);
  }

  /**
   * Returns {@link Endpoints} representing the endpoints of an edge in {@code network}.
   */
  public static <N> Endpoints<N> of(Network<?, ?> network, N nodeA, N nodeB) {
    return network.isDirected() ? ofDirected(nodeA, nodeB) : ofUndirected(nodeA, nodeB);
  }

  /**
   * Returns {@link Endpoints} representing the endpoints of a directed edge.
   */
  static <N> Endpoints.Directed<N> ofDirected(N source, N target) {
    return new Directed<N>(source, target);
  }

  /**
   * Returns {@link Endpoints} representing the endpoints of an undirected edge.
   */
  static <N> Endpoints.Undirected<N> ofUndirected(N nodeA, N nodeB) {
    return new Undirected<N>(nodeA, nodeB);
  }

  /**
   * If these are the {@link Endpoints} of a directed edge, returns the node which is the source of
   * that edge.
   *
   * @throws UnsupportedOperationException if these are the {@link Endpoints} of a undirected edge
   */
  public abstract N source();

  /**
   * If these are the {@link Endpoints} of a directed edge, returns the node which is the target of
   * that edge.
   *
   * @throws UnsupportedOperationException if these are the {@link Endpoints} of a undirected edge
   */
  public abstract N target();

  /**
   * If these are the {@link Endpoints} of a directed edge, returns the {@link #source()};
   * otherwise, returns an arbitrary (but consistent) endpoint of the origin edge.
   */
  public final N nodeA() {
    return nodeA;
  }

  /**
   * Returns the node {@link #adjacentNode(Object) adjacent} to {@link #nodeA()} along the origin
   * edge. If these are the {@link Endpoints} of a directed edge, it is equal to {@link #target()}.
   */
  public final N nodeB() {
    return nodeB;
  }

  /**
   * Returns the node that is adjacent to {@code node} along the origin edge.
   *
   * @throws IllegalArgumentException if this instance does not contain {@code node}, that is, the
   *     origin edge is not incident to {@code}
   */
  public final N adjacentNode(Object node) {
    checkNotNull(node, "node");
    if (node.equals(nodeA)) {
      return nodeB;
    } else if (node.equals(nodeB)) {
      return nodeA;
    } else {
      throw new IllegalArgumentException(
          String.format("Endpoints %s does not contain node %s", this, node));
    }
  }

  abstract boolean isDirected();

  /**
   * Iterates in the order {@link #nodeA()}, {@link #nodeB()}.
   */
  @Override
  public final UnmodifiableIterator<N> iterator() {
    return Iterators.forArray(nodeA, nodeB);
  }

  /**
   * The {@link Endpoints} of two directed edges are equal if their {@link #source()} and
   * {@link #target()} are equal. The {@link Endpoints} of two undirected edges are equal if they
   * contain the same nodes. The {@link Endpoints} of a directed edge are never equal to the
   * {@link Endpoints} of an undirected edge.
   */
  @Override
  public abstract boolean equals(Object obj);

  /**
   * The hashcode of the {@link Endpoints} of a directed edge is equal to
   * {@code Objects.hashCode(source(), target())}. The hashcode of the {@link Endpoints}
   * of an undirected edge is equal to {@code nodeA().hashCode() ^ nodeB().hashCode()}.
   */
  @Override
  public abstract int hashCode();

  /**
   * The {@link Endpoints} of a directed edge.
   */
  private static final class Directed<N> extends Endpoints<N> {
    private Directed(N source, N target) {
      super(source, target);
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
    boolean isDirected() {
      return true;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Endpoints)) {
        return false;
      }

      Endpoints<?> other = (Endpoints<?>) obj;
      if (isDirected() != other.isDirected()) {
        return false;
      }

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

  /**
   * The {@link Endpoints} of an undirected edge.
   */
  private static final class Undirected<N> extends Endpoints<N> {
    private Undirected(N nodeA, N nodeB) {
      super(nodeA, nodeB);
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
    boolean isDirected() {
      return false;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Endpoints)) {
        return false;
      }

      Endpoints<?> other = (Endpoints<?>) obj;
      if (isDirected() != other.isDirected()) {
        return false;
      }

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

    @Override
    public String toString() {
      return String.format("[%s, %s]", nodeA(), nodeB());
    }
  }
}
