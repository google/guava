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

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Static utility methods for calculating properties of {@link Graph} instances.
 *
 * @author Joshua O'Madadhain
 * @since 20.0
 */
// TODO(b/27628622): Move these methods to {@link Graphs}? Or at least rename this class to
// something besides "GraphProperties", and consider putting in graph/algorithms/.
@Beta
public final class GraphProperties {

  private GraphProperties() {}

  /**
   * Returns true iff {@code graph} has at least one cycle. A cycle is defined as a non-empty
   * subset of edges in a graph arranged to form a path (a sequence of adjacent outgoing edges)
   * starting and ending with the same node.
   *
   * <p>This method will detect any non-empty cycle, including self-loops (a cycle of length 1).
   */
  public static boolean isCyclic(Graph<?> graph) {
    int numEdges = graph.edges().size();
    if (numEdges == 0) {
      return false; // An edge-free graph is acyclic by definition.
    }
    if (!graph.isDirected() && numEdges >= graph.nodes().size()) {
      return true; // Optimization for the undirected case: at least one cycle must exist.
    }

    Map<Object, NodeState> visitedNodes = Maps.newHashMapWithExpectedSize(graph.nodes().size());
    for (Object node : graph.nodes()) {
      if (isSubgraphCyclic(graph, visitedNodes, node, null)) {
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
  public static boolean isCyclic(Network<?, ?> network) {
    // In a directed graph, parallel edges cannot introduce a cycle in an acyclic graph.
    // However, in an undirected graph, any parallel edge induces a cycle in the graph.
    if (!network.isDirected() && network.allowsParallelEdges()
        && network.edges().size() > network.asGraph().edges().size()) {
      return true;
    }
    return isCyclic(network.asGraph());
  }

  /**
   * Performs a traversal of the nodes reachable from {@code node}. If we ever reach a node we've
   * already visited (following only outgoing edges and without reusing edges), we know there's a
   * cycle in the graph.
   */
  private static boolean isSubgraphCyclic(
      Graph<?> graph,
      Map<Object, NodeState> visitedNodes,
      Object node,
      @Nullable Object previousNode) {
    NodeState state = visitedNodes.get(node);
    if (state == NodeState.COMPLETE) {
      return false;
    }
    if (state == NodeState.PENDING) {
      return true;
    }

    visitedNodes.put(node, NodeState.PENDING);
    for (Object nextNode : graph.successors(node)) {
      if (canTraverseWithoutReusingEdge(graph, nextNode, previousNode)
          && isSubgraphCyclic(graph, visitedNodes, nextNode, node)) {
        return true;
      }
    }
    visitedNodes.put(node, NodeState.COMPLETE);
    return false;
  }

  /**
   * Determines whether an edge has already been used during traversal. In the directed case a cycle
   * is always detected before reusing an edge, so no special logic is required. In the undirected
   * case, we must take care not to "backtrack" over an edge (i.e. going from A to B and then going
   * from B to A).
   */
  private static boolean canTraverseWithoutReusingEdge(
      Graph<?> graph, Object nextNode, @Nullable Object previousNode) {
    if (graph.isDirected() || !Objects.equal(previousNode, nextNode)) {
      return true;
    }
    // This falls into the undirected A->B->A case. The Graph interface does not support parallel
    // edges, so this traversal would require reusing the undirected AB edge.
    return false;
  }

  /**
   * An enum representing the state of a node during DFS. {@code PENDING} means that
   * the node is on the stack of the DFS, while {@code COMPLETE} means that
   * the node and all its successors have been already explored. Any node that
   * has not been explored will not have a state at all.
   */
  private enum NodeState {
    PENDING,
    COMPLETE
  }
}
