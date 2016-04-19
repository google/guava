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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.graph.DirectedNodeAdjacencies.Adjacency;

import java.util.Set;

/**
 * A {@link Graph} whose relationships are constant. Instances of this class may be obtained
 * with {@link #copyOf(Graph)}.
 *
 * <p>The time complexity of {@code edgesConnecting(node1, node2)} is O(min(outD_node1, inD_node2)).
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 */
public final class ImmutableGraph<N> extends AbstractConfigurableGraph<N> {

  private ImmutableGraph(Graph<N> graph) {
    super(GraphBuilder.from(graph), getNodeConnections(graph));
  }

  /**
   * Returns an immutable copy of {@code graph}.
   */
  public static <N> ImmutableGraph<N> copyOf(Graph<N> graph) {
    // TODO(b/28087289): we can remove this restriction when Graph supports parallel edges
    checkArgument(!(graph instanceof Network), "Input must not implement common.graph.Network");
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

  private static <N> ImmutableMap<N, NodeAdjacencies<N>> getNodeConnections(Graph<N> graph) {
    ImmutableMap.Builder<N, NodeAdjacencies<N>> nodeConnections = ImmutableMap.builder();
    for (N node : graph.nodes()) {
      nodeConnections.put(node, nodeConnectionsOf(graph, node));
    }
    return nodeConnections.build();
  }

  private static <N> NodeAdjacencies<N> nodeConnectionsOf(Graph<N> graph, N node) {
    return graph.isDirected()
        ? DirectedNodeAdjacencies.ofImmutable(createAdjacencyMap(
            graph, node), graph.predecessors(node).size(), graph.successors(node).size())
        : UndirectedNodeAdjacencies.ofImmutable(graph.adjacentNodes(node));
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
