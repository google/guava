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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.Set;

/**
 * Static utility methods for {@link Graph} instances.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @since 20.0
 */
@Beta
public final class Graphs {

  private Graphs() {}

  // Graph query methods

  /**
   * Returns an unmodifiable view of edges that are parallel to {@code edge}, i.e. the set of edges
   * that connect the same nodes in the same direction (if any). An edge is not parallel to itself.
   *
   * @throws IllegalArgumentException if {@code edge} is not present in {@code graph}
   */
  public static <E> Set<E> parallelEdges(Network<?, E> graph, Object edge) {
    Endpoints<?> endpoints = graph.incidentNodes(edge); // Verifies that edge is in graph
    if (!graph.allowsParallelEdges()) {
      return ImmutableSet.of();
    }
    return Sets.difference(graph.edgesConnecting(endpoints.nodeA(), endpoints.nodeB()),
        ImmutableSet.of(edge)); // An edge is not parallel to itself.
  }

  /**
   * Returns an unmodifiable view of the edges which have an {@link Network#incidentNodes(Object)
   * incident node} in common with {@code edge}. An edge is not considered adjacent to itself.
   *
   * @throws IllegalArgumentException if {@code edge} is not present in {@code graph}
   */
  public static <E> Set<E> adjacentEdges(Network<?, E> graph, Object edge) {
    Endpoints<?> endpoints = graph.incidentNodes(edge); // Verifies that edge is in graph
    Set<E> endpointsIncidentEdges =
        Sets.union(graph.incidentEdges(endpoints.nodeA()), graph.incidentEdges(endpoints.nodeB()));
    return Sets.difference(endpointsIncidentEdges, ImmutableSet.of(edge));
  }

  // Graph mutation methods

  // Graph transformation methods

  /**
   * Returns an induced subgraph of {@code graph}. This subgraph is a new graph that contains
   * all of the nodes in {@code nodes}, and all of the edges from {@code graph} for which the
   * edge's incident nodes are both contained by {@code nodes}.
   *
   * @throws IllegalArgumentException if any element in {@code nodes} is not a node in the graph
   */
  public static <N, E> MutableNetwork<N, E> inducedSubgraph(Network<N, E> graph,
      Iterable<? extends N> nodes) {
    NetworkBuilder<N, E> builder = NetworkBuilder.from(graph);
    if (nodes instanceof Collection) {
      builder = builder.expectedNodeCount(((Collection<?>) nodes).size());
    }
    MutableNetwork<N, E> subgraph = builder.build();
    for (N node : nodes) {
      subgraph.addNode(node);
    }
    for (N node : subgraph.nodes()) {
      for (E edge : graph.outEdges(node)) {
        N adjacentNode = graph.incidentNodes(edge).adjacentNode(node);
        if (subgraph.nodes().contains(adjacentNode)) {
          subgraph.addEdgeV2(node, adjacentNode, edge);
        }
      }
    }
    return subgraph;
  }

  /**
   * Creates a mutable copy of {@code graph}, using the same nodes and edges.
   */
  public static <N> MutableGraph<N> copyOf(Graph<N> graph) {
    checkNotNull(graph, "graph");
    MutableGraph<N> copy = GraphBuilder.from(graph)
        .expectedNodeCount(graph.nodes().size())
        .build();

    for (N node : graph.nodes()) {
      checkState(copy.addNode(node));
    }
    for (Endpoints<N> endpoints : graph.edges()) {
      checkState(copy.putEdge(endpoints.nodeA(), endpoints.nodeB()));
    }

    return copy;
  }

  /**
   * Creates a mutable copy of {@code graph}, using the same node and edge elements.
   */
  public static <N, E> MutableNetwork<N, E> copyOf(Network<N, E> graph) {
    checkNotNull(graph, "graph");
    MutableNetwork<N, E> copy = NetworkBuilder.from(graph)
        .expectedNodeCount(graph.nodes().size())
        .expectedEdgeCount(graph.edges().size())
        .build();

    for (N node : graph.nodes()) {
      checkState(copy.addNode(node));
    }
    for (E edge : graph.edges()) {
      Endpoints<N> endpoints = graph.incidentNodes(edge);
      checkState(copy.addEdgeV2(endpoints.nodeA(), endpoints.nodeB(), edge));
    }

    return copy;
  }

  @CanIgnoreReturnValue
  static int checkNonNegative(int value) {
    checkState(value >= 0, "Not true that %s is non-negative.", value);
    return value;
  }

  @CanIgnoreReturnValue
  static int checkPositive(int value) {
    checkState(value > 0, "Not true that %s is positive.", value);
    return value;
  }

  @CanIgnoreReturnValue
  static long checkNonNegative(long value) {
    checkState(value >= 0, "Not true that %s is non-negative.", value);
    return value;
  }

  @CanIgnoreReturnValue
  static long checkPositive(long value) {
    checkState(value > 0, "Not true that %s is positive.", value);
    return value;
  }
}
