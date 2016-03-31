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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Map;
import java.util.Set;

/**
 * A {@link Graph} whose contents will never change. Instances of this class should be obtained
 * with {@link #copyOf(Graph)}.
 *
 * <p>The time complexity of {@code edgesConnecting(node1, node2)} is O(min(outD_node1, inD_node2)).
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
public final class ImmutableGraph<N, E> extends ConfigurableGraph<N, E> {

  private ImmutableGraph(Graph<N, E> graph) {
    super(GraphBuilder.from(graph), getNodeConnections(graph), getEdgeToIncidentNodes(graph));
  }

  /**
   * Returns an immutable copy of {@code graph}.
   */
  public static <N, E> ImmutableGraph<N, E> copyOf(Graph<N, E> graph) {
    return (graph instanceof ImmutableGraph)
        ? (ImmutableGraph<N, E>) graph
        : new ImmutableGraph<N, E>(graph);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   */
  @Deprecated
  public static <N, E> ImmutableGraph<N, E> copyOf(ImmutableGraph<N, E> graph) {
    return checkNotNull(graph);
  }

  /**
   * Guaranteed to throw an exception and leave the graph unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  public final boolean addNode(N node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the graph unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  public final boolean addEdge(E edge, N node1, N node2) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the graph unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  public final boolean removeNode(Object node) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the graph unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  public final boolean removeEdge(Object edge) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<E> edgesConnecting(Object node1, Object node2) {
    // This set is calculated as the intersection of two sets, and is likely to be small.
    // As an optimization, copy it to an ImmutableSet so re-iterating is fast.
    return ImmutableSet.copyOf(super.edgesConnecting(node1, node2));
  }

  private static <N, E> Map<N, NodeConnections<N, E>> getNodeConnections(Graph<N, E> graph) {
    ImmutableMap.Builder<N, NodeConnections<N, E>> nodeConnections = ImmutableMap.builder();
    for (N node : graph.nodes()) {
      nodeConnections.put(node, nodeConnectionsOf(graph, node));
    }
    return nodeConnections.build();
  }

  private static <N, E> Map<E, IncidentNodes<N>> getEdgeToIncidentNodes(Graph<N, E> graph) {
    ImmutableMap.Builder<E, IncidentNodes<N>> edgeToIncidentNodes = ImmutableMap.builder();
    for (E edge : graph.edges()) {
      edgeToIncidentNodes.put(edge, IncidentNodes.of(graph.incidentNodes(edge)));
    }
    return edgeToIncidentNodes.build();
  }

  private static <N, E> NodeConnections<N, E> nodeConnectionsOf(Graph<N, E> graph, N node) {
    return graph.isDirected()
        ? DirectedNodeConnections.ofImmutable(
            graph.predecessors(node), graph.successors(node),
            graph.inEdges(node), graph.outEdges(node))
        : UndirectedNodeConnections.ofImmutable(
            graph.adjacentNodes(node), graph.incidentEdges(node));
  }
}
