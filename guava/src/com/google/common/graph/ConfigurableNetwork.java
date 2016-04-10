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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.graph.GraphErrorMessageUtils.ADDING_PARALLEL_EDGE;
import static com.google.common.graph.GraphErrorMessageUtils.REUSING_EDGE;
import static com.google.common.graph.GraphErrorMessageUtils.SELF_LOOPS_NOT_ALLOWED;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Map;

/**
 * Configurable implementation of {@link Network} that supports both directed and undirected graphs.
 * Instances of this class should be constructed with {@link NetworkBuilder}.
 *
  * <p>Time complexities for mutation methods:
 * <ul>
 * <li>{@code addNode(N node)}: O(1).
 * <li>{@code addEdge(E edge, N node1, N node2)}: O(1).
 * <li>{@code removeNode(N node)}: O(d_node).
 * <li>{@code removeEdge(E edge)}: O(1), unless this graph allows parallel edges;
 *     in that case this method is O(min(outD_edgeSource, inD_edgeTarget)).
 * </ul>
 * where d_node is the degree of node, inD_node is the in-degree of node, and outD_node is the
 * out-degree of node.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
// TODO(b/24620028): Enable this class to support sorted nodes/edges.
class ConfigurableNetwork<N, E>
    extends AbstractConfigurableNetwork<N, E>
    implements MutableNetwork<N, E> {

  /**
   * Constructs a mutable graph with the properties specified in {@code builder}.
   */
  ConfigurableNetwork(NetworkBuilder<? super N, ? super E> builder) {
    super(builder);
  }

  /**
   * Constructs a graph with the properties specified in {@code builder}, initialized with
   * the given node and edge maps. May be used for either mutable or immutable graphs.
   */
  ConfigurableNetwork(NetworkBuilder<? super N, ? super E> builder,
      Map<N, NodeConnections<N, E>> nodeConnections,
      Map<E, IncidentNodes<N>> edgeToIncidentNodes) {
    super(builder, nodeConnections, edgeToIncidentNodes);
  }

  @Override
  @CanIgnoreReturnValue
  public boolean addNode(N node) {
    checkNotNull(node, "node");
    if (nodes().contains(node)) {
      return false;
    }
    nodeConnections.put(node, newNodeConnections(isDirected()));
    return true;
  }

  /**
   * Add nodes that are not elements of the graph, then add {@code edge} between them.
   * Return {@code false} if {@code edge} already exists between {@code node1} and {@code node2},
   * and in the same direction.
   *
   * @throws IllegalArgumentException if an edge (other than {@code edge}) already
   *         exists from {@code node1} to {@code node2}, and this is not a multigraph.
   *         Also, if self-loops are not allowed, and {@code node1} is equal to {@code node2}.
   */
  @Override
  @CanIgnoreReturnValue
  public boolean addEdge(E edge, N node1, N node2) {
    checkNotNull(edge, "edge");
    checkNotNull(node1, "node1");
    checkNotNull(node2, "node2");
    IncidentNodes<N> incidentNodes = IncidentNodes.of(node1, node2);
    checkArgument(allowsSelfLoops() || !incidentNodes.isSelfLoop(), SELF_LOOPS_NOT_ALLOWED, node1);
    boolean containsN1 = nodes().contains(node1);
    boolean containsN2 = nodes().contains(node2);
    if (edges().contains(edge)) {
      checkArgument(containsN1 && containsN2 && edgesConnecting(node1, node2).contains(edge),
          REUSING_EDGE, edge, incidentNodes(edge), incidentNodes);
      return false;
    } else if (!allowsParallelEdges()) {
      checkArgument(!(containsN1 && containsN2 && successors(node1).contains(node2)),
          ADDING_PARALLEL_EDGE, node1, node2);
    }
    if (!containsN1) {
      addNode(node1);
    }
    NodeConnections<N, E> connectionsN1 = nodeConnections.get(node1);
    connectionsN1.addSuccessor(node2, edge);
    if (!containsN2) {
      addNode(node2);
    }
    NodeConnections<N, E> connectionsN2 = nodeConnections.get(node2);
    connectionsN2.addPredecessor(node1, edge);
    edgeToIncidentNodes.put(edge, incidentNodes);
    return true;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean removeNode(Object node) {
    checkNotNull(node, "node");
    if (!nodes().contains(node)) {
      return false;
    }
    // Since views are returned, we need to copy the edges that will be removed.
    // Thus we avoid modifying the underlying view while iterating over it.
    for (E edge : ImmutableList.copyOf(incidentEdges(node))) {
      // Simply calling removeEdge(edge) would result in O(degree^2) behavior. However, we know that
      // after all incident edges are removed, the input node will be disconnected from all others.
      removeEdgeAndUpdateConnections(edge, true);
    }
    nodeConnections.remove(node);
    return true;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean removeEdge(Object edge) {
    checkNotNull(edge, "edge");
    if (!edges().contains(edge)) {
      return false;
    }
    // If there are no parallel edges, the removal of this edge will disconnect the incident nodes.
    removeEdgeAndUpdateConnections(edge, Graphs.parallelEdges(this, edge).isEmpty());
    return true;
  }

  /**
   * If {@code disconnectIncidentNodes} is true, disconnects the nodes formerly connected
   * by {@code edge}. This should be set when all parallel edges are or will be removed.
   *
   * <p>Unlike {@link #removeEdge(Object)}, this method is guaranteed to run in O(1) time.
   *
   * @throws IllegalArgumentException if {@code edge} is not present in the graph.
   */
  private void removeEdgeAndUpdateConnections(Object edge, boolean disconnectIncidentNodes) {
    IncidentNodes<N> incidentNodes = checkedIncidentNodes(edge);
    N node1 = incidentNodes.node1();
    N node2 = incidentNodes.node2();
    NodeConnections<N, E> connectionsN1 = nodeConnections.get(node1);
    NodeConnections<N, E> connectionsN2 = nodeConnections.get(node2);
    if (disconnectIncidentNodes) {
      connectionsN1.removeSuccessor(node2);
      connectionsN2.removePredecessor(node1);
    }
    connectionsN1.removeOutEdge(edge);
    connectionsN2.removeInEdge(edge);
    edgeToIncidentNodes.remove(edge);
  }

  private NodeConnections<N, E> newNodeConnections(boolean isDirected) {
    return isDirected
        ? DirectedNodeConnections.<N, E>of()
        : UndirectedNodeConnections.<N, E>of();
  }
}
