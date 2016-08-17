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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.Set;

/**
 * A class to facilitate the set returned by {@link Graph#edges()}.
 *
 * @author James Sexton
 * @since 20.0
 */
abstract class EndpointsIterator<N> extends AbstractIterator<Endpoints<N>> {
  private final Graph<N, ?> graph;
  private final Iterator<N> nodeIterator;

  N node = null; // null is safe as an initial value because graphs do not allow null nodes
  Iterator<N> successorIterator = ImmutableSet.<N>of().iterator();

  static <N> EndpointsIterator<N> of(Graph<N, ?> graph) {
    return graph.isDirected() ? new Directed<N>(graph) : new Undirected<N>(graph);
  }

  EndpointsIterator(Graph<N, ?> graph) {
    this.graph = graph;
    this.nodeIterator = graph.nodes().iterator();
  }

  /**
   * Called after {@link #successorIterator} is exhausted. Advances {@link #node} to the next node
   * and updates {@link #successorIterator} to iterate through the successors of {@link #node}.
   */
  final boolean advance() {
    checkState(!successorIterator.hasNext());
    if (!nodeIterator.hasNext()) {
      return false;
    }
    node = nodeIterator.next();
    successorIterator = graph.successors(node).iterator();
    return true;
  }

  /**
   * If the graph is directed, each ordered [source, target] pair will be visited once if there is
   * one or more edge connecting them.
   */
  private static final class Directed<N> extends EndpointsIterator<N> {
    Directed(Graph<N, ?> graph){
      super(graph);
    }

    @Override
    protected Endpoints<N> computeNext() {
      while (true) {
        if (successorIterator.hasNext()) {
          return Endpoints.ofDirected(node, successorIterator.next());
        }
        if (!advance()) {
          return endOfData();
        }
      }
    }
  }

  /**
   * If the graph is undirected, each unordered [node, otherNode] pair (except self-loops) will be
   * visited twice if there is one or more edge connecting them. To avoid returning duplicate
   * {@link Endpoints}, we keep track of the nodes that we have visited. When processing node pairs,
   * we skip if the "other node" is in the visited set, as shown below:
   *
   * Nodes = {N1, N2, N3, N4}
   *    N2           __
   *   /  \         |  |
   * N1----N3      N4__|
   *
   * Visited Nodes = {}
   * Endpoints [N1, N2] - return
   * Endpoints [N1, N3] - return
   * Visited Nodes = {N1}
   * Endpoints [N2, N1] - skip
   * Endpoints [N2, N3] - return
   * Visited Nodes = {N1, N2}
   * Endpoints [N3, N1] - skip
   * Endpoints [N3, N2] - skip
   * Visited Nodes = {N1, N2, N3}
   * Endpoints [N4, N4] - return
   * Visited Nodes = {N1, N2, N3, N4}
   */
  private static final class Undirected<N> extends EndpointsIterator<N> {
    private Set<N> visitedNodes;

    Undirected(Graph<N, ?> graph) {
      super(graph);
      this.visitedNodes = Sets.newHashSetWithExpectedSize(graph.nodes().size());
    }

    @Override
    protected Endpoints<N> computeNext() {
      while (true) {
        while (successorIterator.hasNext()) {
          N otherNode = successorIterator.next();
          if (!visitedNodes.contains(otherNode)) {
            return Endpoints.ofUndirected(node, otherNode);
          }
        }
        // Add to visited set *after* processing neighbors so we still include self-loops.
        visitedNodes.add(node);
        if (!advance()) {
          visitedNodes = null;
          return endOfData();
        }
      }
    }
  }
}
