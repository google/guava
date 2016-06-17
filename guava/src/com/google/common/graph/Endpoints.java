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
import static com.google.common.graph.GraphErrorMessageUtils.NOT_AVAILABLE_ON_UNDIRECTED;

import com.google.common.base.Objects;
import com.google.common.collect.UnmodifiableIterator;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * An immutable {@link Collection} to represent the endpoints of an edge in a graph.
 * <p>
 * If an {@link Endpoints} is directed, it is an ordered pair of nodes (source and target).
 * Otherwise, it is an unordered pair of nodes that can be accessed through the iterator.
 *
 * @author James Sexton
 * @since 20.0
 */
public abstract class Endpoints<N> extends AbstractCollection<N> {
  private final N nodeA;
  private final N nodeB;

  private Endpoints(N nodeA, N nodeB) {
    this.nodeA = checkNotNull(nodeA);
    this.nodeB = checkNotNull(nodeB);
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
   * If this {@link Endpoints} is directed, returns the node which is the source.
   *
   * @throws UnsupportedOperationException if this Endpoints is not directed
   */
  public abstract N source();

  /**
   * If this {@link Endpoints} is directed, returns the node which is the target.
   *
   * @throws UnsupportedOperationException if this Endpoints is not directed
   */
  public abstract N target();

  /**
   * If this {@link Endpoints} is directed, returns the node which is the source.
   * Otherwise, returns an arbitrary (but consistent) endpoint of the edge.
   */
  N nodeA() {
    return nodeA;
  }

  /**
   * Returns the node that is opposite {@link #nodeA()}. In the directed case, this is the target.
   */
  N nodeB() {
    return nodeB;
  }

  @Override
  public UnmodifiableIterator<N> iterator() {
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
  public int size() {
    return 2;
  }

  @Override
  public boolean contains(Object obj) {
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
      return (nodeA().equals(other.nodeA()) && nodeB().equals(other.nodeB()))
          || (nodeA().equals(other.nodeB()) && nodeB().equals(other.nodeA()));
    }

    @Override
    public int hashCode() {
      return nodeA().hashCode() ^ nodeB().hashCode();
    }
  }
}
