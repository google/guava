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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Static utility methods for {@link Graph} and {@link Network} instances.
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
   * Returns true iff {@code graph} has at least one cycle. A cycle is defined as a non-empty
   * subset of edges in a graph arranged to form a path (a sequence of adjacent outgoing edges)
   * starting and ending with the same node.
   *
   * <p>This method will detect any non-empty cycle, including self-loops (a cycle of length 1).
   */
  public static boolean hasCycle(Graph<?, ?> graph) {
    int numEdges = graph.edges().size();
    if (numEdges == 0) {
      return false; // An edge-free graph is acyclic by definition.
    }
    if (!graph.isDirected() && numEdges >= graph.nodes().size()) {
      return true; // Optimization for the undirected case: at least one cycle must exist.
    }

    Map<Object, NodeVisitState> visitedNodes =
        Maps.newHashMapWithExpectedSize(graph.nodes().size());
    for (Object node : graph.nodes()) {
      if (subgraphHasCycle(graph, visitedNodes, node, null)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true iff {@code network} has at least one cycle. A cycle is defined as a non-empty
   * subset of edges in a graph arranged to form a path (a sequence of adjacent outgoing edges)
   * starting and ending with the same node.
   *
   * <p>This method will detect any non-empty cycle, including self-loops (a cycle of length 1).
   */
  public static boolean hasCycle(Network<?, ?> network) {
    // In a directed graph, parallel edges cannot introduce a cycle in an acyclic graph.
    // However, in an undirected graph, any parallel edge induces a cycle in the graph.
    if (!network.isDirected() && network.allowsParallelEdges()
        && network.edges().size() > network.asGraph().edges().size()) {
      return true;
    }
    return hasCycle(network.asGraph());
  }

  /**
   * Performs a traversal of the nodes reachable from {@code node}. If we ever reach a node we've
   * already visited (following only outgoing edges and without reusing edges), we know there's a
   * cycle in the graph.
   */
  private static boolean subgraphHasCycle(
      Graph<?, ?> graph,
      Map<Object, NodeVisitState> visitedNodes,
      Object node,
      @Nullable Object previousNode) {
    NodeVisitState state = visitedNodes.get(node);
    if (state == NodeVisitState.COMPLETE) {
      return false;
    }
    if (state == NodeVisitState.PENDING) {
      return true;
    }

    visitedNodes.put(node, NodeVisitState.PENDING);
    for (Object nextNode : graph.successors(node)) {
      if (canTraverseWithoutReusingEdge(graph, nextNode, previousNode)
          && subgraphHasCycle(graph, visitedNodes, nextNode, node)) {
        return true;
      }
    }
    visitedNodes.put(node, NodeVisitState.COMPLETE);
    return false;
  }

  /**
   * Determines whether an edge has already been used during traversal. In the directed case a cycle
   * is always detected before reusing an edge, so no special logic is required. In the undirected
   * case, we must take care not to "backtrack" over an edge (i.e. going from A to B and then going
   * from B to A).
   */
  private static boolean canTraverseWithoutReusingEdge(
      Graph<?, ?> graph, Object nextNode, @Nullable Object previousNode) {
    if (graph.isDirected() || !Objects.equal(previousNode, nextNode)) {
      return true;
    }
    // This falls into the undirected A->B->A case. The Graph interface does not support parallel
    // edges, so this traversal would require reusing the undirected AB edge.
    return false;
  }

  /**
   * Returns the transitive closure of {@code graph}. The transitive closure of a graph is another
   * graph with an edge connecting node A to node B iff node B is {@link #reachableNodes(Graph,
   * Object) reachable} from node A.
   *
   * <p>This is a "snapshot" based on the current topology of {@code graph}, rather than a live view
   * of the transitive closure of {@code graph}. In other words, the returned {@link BasicGraph}
   * will not be updated after modifications to {@code graph}.
   */
  public static <N> BasicGraph<N> transitiveClosure(Graph<N, ?> graph) {
    MutableBasicGraph<N> transitiveClosure =
        BasicGraphBuilder.from(graph).allowsSelfLoops(true).build();
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
   * <p>This is a "snapshot" based on the current topology of {@code graph}, rather than a live view
   * of the set of nodes reachable from {@code node}. In other words, the returned {@link Set} will
   * not be updated after modifications to {@code graph}.
   *
   * @throws IllegalArgumentException if {@code node} is not present in {@code graph}
   */
  @SuppressWarnings("unchecked") // Throws an exception if node is not an element of graph.
  public static <N> Set<N> reachableNodes(Graph<N, ?> graph, Object node) {
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
   * all of the nodes in {@code nodes}, and all of the {@link Graph#edges() edges} from {@code
   * graph} for which the endpoints are both contained by {@code nodes}.
   *
   * @throws IllegalArgumentException if any element in {@code nodes} is not a node in the graph
   */
  public static <N> MutableBasicGraph<N> inducedSubgraph(BasicGraph<N> graph,
      Iterable<? extends N> nodes) {
    MutableBasicGraph<N> subgraph = BasicGraphBuilder.from(graph).build();
    for (N node : nodes) {
      subgraph.addNode(node);
    }
    for (N node : subgraph.nodes()) {
      for (N successorNode : graph.successors(node)) {
        if (subgraph.nodes().contains(successorNode)) {
          subgraph.putEdge(node, successorNode);
        }
      }
    }
    return subgraph;
  }

  /**
   * Returns an induced subgraph of {@code graph}. This subgraph is a new graph that contains
   * all of the nodes in {@code nodes}, and all of the {@link Graph#edges() edges} (and
   * associated edge values) from {@code graph} for which the endpoints are both contained by
   * {@code nodes}.
   *
   * @throws IllegalArgumentException if any element in {@code nodes} is not a node in the graph
   */
  public static <N, V> MutableGraph<N, V> inducedSubgraph(Graph<N, V> graph,
      Iterable<? extends N> nodes) {
    MutableGraph<N, V> subgraph = GraphBuilder.from(graph).build();
    for (N node : nodes) {
      subgraph.addNode(node);
    }
    for (N node : subgraph.nodes()) {
      for (N successorNode : graph.successors(node)) {
        if (subgraph.nodes().contains(successorNode)) {
          subgraph.putEdgeValue(node, successorNode, graph.edgeValue(node, successorNode));
        }
      }
    }
    return subgraph;
  }

  /**
   * Returns an induced subgraph of {@code graph}. This subgraph is a new graph that contains
   * all of the nodes in {@code nodes}, and all of the {@link Network#edges() edges} from {@code
   * graph} for which the endpoints are both contained by {@code nodes}.
   *
   * @throws IllegalArgumentException if any element in {@code nodes} is not a node in the graph
   */
  public static <N, E> MutableNetwork<N, E> inducedSubgraph(Network<N, E> graph,
      Iterable<? extends N> nodes) {
    MutableNetwork<N, E> subgraph = NetworkBuilder.from(graph).build();
    for (N node : nodes) {
      subgraph.addNode(node);
    }
    for (N node : subgraph.nodes()) {
      for (E edge : graph.outEdges(node)) {
        N successorNode = graph.incidentNodes(edge).adjacentNode(node);
        if (subgraph.nodes().contains(successorNode)) {
          subgraph.addEdge(node, successorNode, edge);
        }
      }
    }
    return subgraph;
  }

  /**
   * Creates a mutable copy of {@code graph} with the same nodes and edges.
   */
  public static <N> MutableBasicGraph<N> copyOf(BasicGraph<N> graph) {
    MutableBasicGraph<N> copy = BasicGraphBuilder.from(graph)
        .expectedNodeCount(graph.nodes().size())
        .build();
    for (N node : graph.nodes()) {
      copy.addNode(node);
    }
    for (Endpoints<N> endpoints : graph.edges()) {
      copy.putEdge(endpoints.nodeA(), endpoints.nodeB());
    }
    return copy;
  }

  /**
   * Creates a mutable copy of {@code graph} with the same nodes, edges, and edge values.
   */
  public static <N, V> MutableGraph<N, V> copyOf(Graph<N, V> graph) {
    MutableGraph<N, V> copy = GraphBuilder.from(graph)
        .expectedNodeCount(graph.nodes().size())
        .build();
    for (N node : graph.nodes()) {
      copy.addNode(node);
    }
    for (Endpoints<N> edge : graph.edges()) {
      copy.putEdgeValue(edge.nodeA(), edge.nodeB(), graph.edgeValue(edge.nodeA(), edge.nodeB()));
    }
    return copy;
  }

  /**
   * Creates a mutable copy of {@code graph} with the same nodes and edges.
   */
  public static <N, E> MutableNetwork<N, E> copyOf(Network<N, E> graph) {
    MutableNetwork<N, E> copy = NetworkBuilder.from(graph)
        .expectedNodeCount(graph.nodes().size())
        .expectedEdgeCount(graph.edges().size())
        .build();
    for (N node : graph.nodes()) {
      copy.addNode(node);
    }
    for (E edge : graph.edges()) {
      Endpoints<N> endpoints = graph.incidentNodes(edge);
      copy.addEdge(endpoints.nodeA(), endpoints.nodeB(), edge);
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

  /**
   * An enum representing the state of a node during DFS. {@code PENDING} means that
   * the node is on the stack of the DFS, while {@code COMPLETE} means that
   * the node and all its successors have been already explored. Any node that
   * has not been explored will not have a state at all.
   */
  private enum NodeVisitState {
    PENDING,
    COMPLETE
  }
}
