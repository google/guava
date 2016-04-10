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

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.Map;

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
   * Returns true iff {@code graph} has at least one cycle.
   */
  public static boolean isCyclic(Graph<?> graph) {
    // TODO(user): Implement an algorithm that also works on undirected graphs.
    // For instance, we should keep track of the edge used to reach a node to avoid
    // reusing it (making a cycle by getting back to that node). Also, parallel edges
    // will need to be carefully handled for undirected graphs.
    checkArgument(graph.isDirected(), "isCyclic() currently only works on directed graphs");

    Map<Object, NodeVisitState> nodeToVisitState = Maps.newHashMap();
    for (Object node : graph.nodes()) {
      if (nodeToVisitState.get(node) == null) {
        if (isSubgraphCyclic(graph, nodeToVisitState, node)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true iff there is a cycle in the subgraph of {@code graph} reachable from
   * {@code node}.
   */
  private static boolean isSubgraphCyclic(
      Graph<?> graph, Map<Object, NodeVisitState> nodeToVisitState, Object node) {
    nodeToVisitState.put(node, NodeVisitState.PENDING);
    for (Object successor : graph.successors(node)) {
      NodeVisitState nodeVisitState = nodeToVisitState.get(successor);
      if (nodeVisitState == NodeVisitState.PENDING) {
        return true;
      } else if (nodeVisitState == null) {
        if (isSubgraphCyclic(graph, nodeToVisitState, successor)) {
          return true;
        }
      } // otherwise the state is COMPLETE, nothing to do
    }
    nodeToVisitState.put(node, NodeVisitState.COMPLETE);
    return false;
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

  /**
   * Returns the set of all nodes in {@code graph} that have no predecessors.
   *
   * <p>Note that in an undirected graph, this is equivalent to all isolated nodes.
   */
  public static <N> ImmutableSet<N> roots(Graph<N> graph) {
    ImmutableSet.Builder<N> builder = ImmutableSet.builder();
    for (N node : graph.nodes()) {
      if (graph.predecessors(node).isEmpty()) {
        builder.add(node);
      }
    }
    return builder.build();
  }
}
