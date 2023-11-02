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
import com.google.errorprone.annotations.Immutable;
import javax.annotation.CheckForNull;

/**
 * An immutable pair representing the two endpoints of an edge in a graph. The {@link EndpointPair}
 * of a directed edge is an ordered pair of nodes ({@link #source()} and {@link #target()}). The
 * {@link EndpointPair} of an undirected edge is an unordered pair of nodes ({@link #nodeU()} and
 * {@link #nodeV()}).
 *
 * <p>The edge is a self-loop if, and only if, the two endpoints are equal.
 *
 * @author James Sexton
 * @since 20.0
 */
@Beta
@Immutable(containerOf = {"N"})
@ElementTypesAreNonnullByDefault
public abstract class EndpointPair<N> implements Iterable<N> {
  private final N nodeU;
  private final N nodeV;

  private EndpointPair(N nodeU, N nodeV) {
    this.nodeU = checkNotNull(nodeU);
    this.nodeV = checkNotNull(nodeV);
  }

  /** Returns an {@link EndpointPair} representing the endpoints of a directed edge. */
  public static <N> EndpointPair<N> ordered(N source, N target) {
    return new Ordered<>(source, target);
  }

  /** Returns an {@link EndpointPair} representing the endpoints of an undirected edge. */
  public static <N> EndpointPair<N> unordered(N nodeU, N nodeV) {
    // Swap nodes on purpose to prevent callers from relying on the "ordering" of an unordered pair.
    return new Unordered<>(nodeV, nodeU);
  }

  /** Returns an {@link EndpointPair} representing the endpoints of an edge in {@code graph}. */
  static <N> EndpointPair<N> of(Graph<?> graph, N nodeU, N nodeV) {
    return graph.isDirected() ? ordered(nodeU, nodeV) : unordered(nodeU, nodeV);
  }

  /** Returns an {@link EndpointPair} representing the endpoints of an edge in {@code network}. */
  static <N> EndpointPair<N> of(Network<?, ?> network, N nodeU, N nodeV) {
    return network.isDirected() ? ordered(nodeU, nodeV) : unordered(nodeU, nodeV);
  }

  /**
   * If this {@link EndpointPair} {@link #isOrdered()}, returns the node which is the source.
   *
   * @throws UnsupportedOperationException if this {@link EndpointPair} is not ordered
   */
  public abstract N source();

  /**
   * If this {@link EndpointPair} {@link #isOrdered()}, returns the node which is the target.
   *
   * @throws UnsupportedOperationException if this {@link EndpointPair} is not ordered
   */
  public abstract N target();

  /**
   * If this {@link EndpointPair} {@link #isOrdered()} returns the {@link #source()}; otherwise,
   * returns an arbitrary (but consistent) endpoint of the origin edge.
   */
  public final N nodeU() {
    return nodeU;
  }

  /**
   * Returns the node {@link #adjacentNode(Object) adjacent} to {@link #nodeU()} along the origin
   * edge. If this {@link EndpointPair} {@link #isOrdered()}, this is equal to {@link #target()}.
   */
  public final N nodeV() {
    return nodeV;
  }

  /**
   * Returns the node that is adjacent to {@code node} along the origin edge.
   *
   * @throws IllegalArgumentException if this {@link EndpointPair} does not contain {@code node}
   * @since 20.0 (but the argument type was changed from {@code Object} to {@code N} in 31.0)
   */
  public final N adjacentNode(N node) {
    if (node.equals(nodeU)) {
      return nodeV;
    } else if (node.equals(nodeV)) {
      return nodeU;
    } else {
      throw new IllegalArgumentException("EndpointPair " + this + " does not contain node " + node);
    }
  }

  /**
   * Returns {@code true} if this {@link EndpointPair} is an ordered pair (i.e. represents the
   * endpoints of a directed edge).
   */
  public abstract boolean isOrdered();

  /** Iterates in the order {@link #nodeU()}, {@link #nodeV()}. */
  @Override
  public final UnmodifiableIterator<N> iterator() {
    return Iterators.forArray(nodeU, nodeV);
  }

  /**
   * Two ordered {@link EndpointPair}s are equal if their {@link #source()} and {@link #target()}
   * are equal. Two unordered {@link EndpointPair}s are equal if they contain the same nodes. An
   * ordered {@link EndpointPair} is never equal to an unordered {@link EndpointPair}.
   */
  @Override
  public abstract boolean equals(@CheckForNull Object obj);

  /**
   * The hashcode of an ordered {@link EndpointPair} is equal to {@code Objects.hashCode(source(),
   * target())}. The hashcode of an unordered {@link EndpointPair} is equal to {@code
   * nodeU().hashCode() + nodeV().hashCode()}.
   */
  @Override
  public abstract int hashCode();

  private static final class Ordered<N> extends EndpointPair<N> {
    private Ordered(N source, N target) {
      super(source, target);
    }

    @Override
    public N source() {
      return nodeU();
    }

    @Override
    public N target() {
      return nodeV();
    }

    @Override
    public boolean isOrdered() {
      return true;
    }

    @Override
    public boolean equals(@CheckForNull Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof EndpointPair)) {
        return false;
      }

      EndpointPair<?> other = (EndpointPair<?>) obj;
      if (isOrdered() != other.isOrdered()) {
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
      return "<" + source() + " -> " + target() + ">";
    }
  }

  private static final class Unordered<N> extends EndpointPair<N> {
    private Unordered(N nodeU, N nodeV) {
      super(nodeU, nodeV);
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
    public boolean isOrdered() {
      return false;
    }

    @Override
    public boolean equals(@CheckForNull Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof EndpointPair)) {
        return false;
      }

      EndpointPair<?> other = (EndpointPair<?>) obj;
      if (isOrdered() != other.isOrdered()) {
        return false;
      }

      // Equivalent to the following simple implementation:
      // boolean condition1 = nodeU().equals(other.nodeU()) && nodeV().equals(other.nodeV());
      // boolean condition2 = nodeU().equals(other.nodeV()) && nodeV().equals(other.nodeU());
      // return condition1 || condition2;
      if (nodeU().equals(other.nodeU())) { // check condition1
        // Here's the tricky bit. We don't have to explicitly check for condition2 in this case.
        // Why? The second half of condition2 requires that nodeV equals other.nodeU.
        // We already know that nodeU equals other.nodeU. Combined with the earlier statement,
        // and the transitive property of equality, this implies that nodeU equals nodeV.
        // If nodeU equals nodeV, condition1 == condition2, so checking condition1 is sufficient.
        return nodeV().equals(other.nodeV());
      }
      return nodeU().equals(other.nodeV()) && nodeV().equals(other.nodeU()); // check condition2
    }

    @Override
    public int hashCode() {
      return nodeU().hashCode() + nodeV().hashCode();
    }

    @Override
    public String toString() {
      return "[" + nodeU() + ", " + nodeV() + "]";
    }
  }
}
