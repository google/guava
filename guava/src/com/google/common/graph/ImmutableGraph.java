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

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.DirectedGraphConnections.Adjacency;
import java.util.Set;

/**
 * A {@link Graph} whose relationships are constant. Instances of this class may be obtained
 * with {@link #copyOf(Graph)}.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @since 20.0
 */
@Beta
public final class ImmutableGraph<N> extends AbstractConfigurableGraph<N> {

  private ImmutableGraph(Graph<N> graph) {
    super(GraphBuilder.from(graph), getNodeConnections(graph), graph.edges().size());
  }

  /**
   * Returns an immutable copy of {@code graph}.
   */
  @SuppressWarnings("unchecked")
  public static <N> ImmutableGraph<N> copyOf(Graph<N> graph) {
    return (graph instanceof ImmutableGraph)
        ? (ImmutableGraph<N>) graph
        : new ImmutableGraph<N>(graph);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   */
  @Deprecated
  public static <N> ImmutableGraph<N> copyOf(ImmutableGraph<N> graph) {
    return checkNotNull(graph);
  }

  private static <N> ImmutableMap<N, GraphConnections<N>> getNodeConnections(Graph<N> graph) {
    // ImmutableMap.Builder maintains the order of the elements as inserted, so the map will
    // have whatever ordering the graph's nodes do, so ImmutableSortedMap is unnecessary even if the
    // input nodes are sorted.
    ImmutableMap.Builder<N, GraphConnections<N>> nodeConnections = ImmutableMap.builder();
    for (N node : graph.nodes()) {
      nodeConnections.put(node, connectionsOf(graph, node));
    }
    return nodeConnections.build();
  }

  private static <N> GraphConnections<N> connectionsOf(Graph<N> graph, N node) {
    return graph.isDirected()
        ? DirectedGraphConnections.ofImmutable(createAdjacencyMap(graph, node),
            graph.predecessors(node).size(), graph.successors(node).size())
        : UndirectedGraphConnections.ofImmutable(graph.adjacentNodes(node));
  }

  private static <N> ImmutableMap<N, Adjacency> createAdjacencyMap(Graph<N> graph, N node) {
    Set<N> predecessors = graph.predecessors(node);
    Set<N> successors = graph.successors(node);
    ImmutableMap.Builder<N, Adjacency> nodeAdjacencies = ImmutableMap.builder();
    for (N adjacentNode : graph.adjacentNodes(node)) {
      nodeAdjacencies.put(adjacentNode,
          getAdjacency(predecessors.contains(adjacentNode), successors.contains(adjacentNode)));
    }
    return nodeAdjacencies.build();
  }

  private static Adjacency getAdjacency(boolean isPredecessor, boolean isSuccesor) {
    if (isPredecessor && isSuccesor) {
      return Adjacency.BOTH;
    } else if (isPredecessor) {
      return Adjacency.PRED;
    } else if (isSuccesor) {
      return Adjacency.SUCC;
    } else {
      throw new IllegalStateException();
    }
  }
}
