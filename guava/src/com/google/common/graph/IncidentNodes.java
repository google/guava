/*
 * Copyright (C) 2014 The Guava Authors
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

import com.google.common.collect.Iterators;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * An immutable set representing the nodes incident to an edge in a graph.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 */
abstract class IncidentNodes<N> extends AbstractSet<N> {

  static <N> IncidentNodes<N> of(N node1, N node2) {
    if (node1.equals(node2)) {
      return new OneNode<N>(node1);
    } else {
      return new TwoNodes<N>(node1, node2);
    }
  }

  static <N> IncidentNodes<N> of(Set<N> nodes) {
    Iterator<N> nodesIterator = nodes.iterator();
    switch (nodes.size()) {
      case 1:
        return new OneNode<N>(nodesIterator.next());
      case 2:
        return new TwoNodes<N>(nodesIterator.next(), nodesIterator.next());
      default:
        throw new IllegalArgumentException("Hypergraphs are not currently supported. An edge in a"
            + " non-hypergraph cannot be incident to " + nodes.size() + " nodes: " + nodes);
    }
  }

  boolean isSelfLoop() {
    return size() == 1;
  }

  /**
   * In the case of a directed graph, returns the source node of the incident edge. In the case of
   * an undirected graph, returns an arbitrary (but consistent) endpoint of the incident edge.
   */
  abstract N node1();

  /**
   * Returns the node opposite to {@link #node1} along the incident edge. In the case of a directed
   * graph, this will always be the target node of the incident edge.
   */
  abstract N node2();

  private static final class OneNode<N> extends IncidentNodes<N> {
    private final N node;

    private OneNode(N node) {
      this.node = checkNotNull(node, "node");
    }

    @Override
    public Iterator<N> iterator() {
      return Iterators.singletonIterator(node);
    }

    @Override
    public int size() {
      return 1;
    }

    @Override
    public boolean contains(@Nullable Object object) {
      return node.equals(object);
    }

    @Override
    N node1() {
      return node;
    }

    @Override
    N node2() {
      return node;
    }
  }

  private static final class TwoNodes<N> extends IncidentNodes<N> {
    private final N node1;
    private final N node2;

    /**
     * An immutable set with two non-equal nodes. Iterates as {@code node1}, {@code node2}.
     */
    private TwoNodes(N node1, N node2) {
      this.node1 = checkNotNull(node1, "node1");
      this.node2 = checkNotNull(node2, "node2");
    }

    @Override
    public Iterator<N> iterator() {
      return Iterators.forArray(node1, node2);
    }

    @Override
    public int size() {
      return 2;
    }

    @Override
    public boolean contains(@Nullable Object object) {
      return node1.equals(object) || node2.equals(object);
    }

    @Override
    N node1() {
      return node1;
    }

    @Override
    N node2() {
      return node2;
    }
  }
}
