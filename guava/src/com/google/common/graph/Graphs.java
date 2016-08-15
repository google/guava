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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
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
   * Returns the transitive closure of {@code graph}. The transitive closure of a graph is another
   * graph with an edge connecting node A to node B iff node B is {@link #reachableNodes(Graph,
   * Object) reachable} from node A.
   *
   * <p>This is a "snapshot" based on the current topology of {@code graph}, rather than a live
   * view of the transitive closure of {@code graph}. In other words, the returned {@link Graph}
   * will not be updated after modifications to {@code graph}.
   */
  public static <N> Graph<N> transitiveClosure(Graph<N> graph) {
    MutableGraph<N> transitiveClosure = GraphBuilder.from(graph).allowsSelfLoops(true).build();
    // Every node is, at a minimum, reachable from itself. Since the resulting transitive closure
    // will have no isolated nodes, we can skip adding nodes explicitly and let putEdge() do it.

    if (graph.isDirected()) {
      // Note: works for both directed and undirected graphs, but we only use in the directed case.
      for (N node : graph.nodes()) {
        for (N reachableNode : reachableNodes(graph, node)) {
          transitiveClosure.putEdge(node, reachableNode);
        }
      }
    } else {
      // An optimization for the undirected case: for every node B reachable from node A,
      // node A and node B have the same reachability set.
      Set<N> visitedNodes = new HashSet<N>();
      for (N node : graph.nodes()) {
        if (!visitedNodes.contains(node)) {
          ImmutableList<N> reachableNodes = ImmutableList.copyOf(reachableNodes(graph, node));
          visitedNodes.addAll(reachableNodes);
          for (int a = 0; a < reachableNodes.size(); ++a) {
            N nodeA = reachableNodes.get(a);
            for (int b = a; b < reachableNodes.size(); ++b) {
              N nodeB = reachableNodes.get(b);
              transitiveClosure.putEdge(nodeA, nodeB);
            }
          }
        }
      }
    }

    return transitiveClosure;
  }

  /**
   * Returns the set of nodes that are reachable from {@code node}. Node B is defined as reachable
   * from node A if there exists a path (a sequence of adjacent outgoing edges) starting at node A
   * and ending at node B. Note that a node is always reachable from itself via a zero-length path.
   *
   * <p>This is a "snapshot" based on the current topology of {@code graph}, rather than a live
   * view of the set of nodes reachable from {@code node}. In other words, the returned {@link Set}
   * will not be updated after modifications to {@code graph}.
   *
   * @throws IllegalArgumentException if {@code node} is not present in {@code graph}
   */
  @SuppressWarnings("unchecked") // Throws an exception if node is not an element of graph.
  public static <N> Set<N> reachableNodes(Graph<N> graph, Object node) {
    checkArgument(graph.nodes().contains(node));
    Set<N> visitedNodes = new HashSet<N>();
    Queue<N> queuedNodes = new ArrayDeque<N>();
    visitedNodes.add((N) node);
    queuedNodes.add((N) node);
    // Perform a breadth-first traversal rooted at the input node.
    while (!queuedNodes.isEmpty()) {
      N currentNode = queuedNodes.remove();
      for (N successor : graph.successors(currentNode)) {
        if (visitedNodes.add(successor)) {
          queuedNodes.add(successor);
        }
      }
    }
    return Collections.unmodifiableSet(visitedNodes);
  }

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
          subgraph.addEdge(node, adjacentNode, edge);
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
      checkState(copy.addEdge(endpoints.nodeA(), endpoints.nodeB(), edge));
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
