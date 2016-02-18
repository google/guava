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
import static com.google.common.graph.GraphErrorMessageUtils.EDGE_NOT_IN_GRAPH;
import static com.google.common.graph.GraphErrorMessageUtils.NODE_NOT_IN_GRAPH;
import static com.google.common.graph.GraphErrorMessageUtils.REUSING_EDGE;
import static com.google.common.graph.GraphErrorMessageUtils.SELF_LOOPS_NOT_ALLOWED;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Configurable implementation of {@link Graph} that supports both directed and undirected graphs.
 *
 * <p>This class maintains a map of {@link NodeConnections} for every node
 * and {@link IncidentNodes} for every edge.
 *
 * <p>{@code Set}-returning accessors return unmodifiable views: the view returned will reflect
 * changes to the graph (if the graph is mutable) but may not be modified by the user.
 * The behavior of the returned view is undefined in the following cases:
 * <ul>
 * <li>Removing the element on which the accessor is called (e.g.:
 *     <pre>{@code
 *     Set<N> adjacentNodes = adjacentNodes(node);
 *     graph.removeNode(node);}</pre>
 *     At this point, the contents of {@code adjacentNodes} are undefined.
 * </ul>
 *
 * <p>The time complexity of all {@code Set}-returning accessors is O(1), since we
 * are returning views.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @see Graph
 */
// TODO(b/24620028): Enable this class to support sorted nodes/edges.
abstract class AbstractConfigurableGraph<N, E> extends AbstractGraph<N, E> {
  // The default of 11 is rather arbitrary, but roughly matches the sizing of just new HashMap()
  private static final int DEFAULT_MAP_SIZE = 11;

  private final Map<N, NodeConnections<N, E>> nodeConnections;
  private final Map<E, IncidentNodes<N>> edgeToIncidentNodes;

  /**
   * Constructs a mutable graph with the specified configuration.
   */
  AbstractConfigurableGraph(GraphConfig config) {
    super(config);
    this.nodeConnections =
        Maps.newLinkedHashMapWithExpectedSize(config.getExpectedNodeCount().or(DEFAULT_MAP_SIZE));
    this.edgeToIncidentNodes =
        Maps.newLinkedHashMapWithExpectedSize(config.getExpectedEdgeCount().or(DEFAULT_MAP_SIZE));
  }

  /**
   * Constructs a graph with the specified configuration and node/edge relationships.
   * May be used for immutable graphs.
   */
  AbstractConfigurableGraph(GraphConfig config, Map<N, NodeConnections<N, E>> nodeConnections,
      Map<E, IncidentNodes<N>> edgeToIncidentNodes) {
    super(config);
    this.nodeConnections = nodeConnections;
    this.edgeToIncidentNodes = edgeToIncidentNodes;
  }

  abstract NodeConnections<N, E> newNodeConnections();

  @Override
  public Set<N> nodes() {
    return Collections.unmodifiableSet(nodeConnections.keySet());
  }

  @Override
  public Set<E> edges() {
    return Collections.unmodifiableSet(edgeToIncidentNodes.keySet());
  }

  @Override
  public Set<E> incidentEdges(Object node) {
    return checkedConnections(node).incidentEdges();
  }

  @Override
  public Set<N> incidentNodes(Object edge) {
    return checkedIncidentNodes(edge);
  }

  @Override
  public Set<N> adjacentNodes(Object node) {
    return checkedConnections(node).adjacentNodes();
  }

  @Override
  public Set<E> adjacentEdges(Object edge) {
    Iterator<N> incidentNodesIterator = incidentNodes(edge).iterator();
    Set<E> endpointsIncidentEdges = incidentEdges(incidentNodesIterator.next());
    while (incidentNodesIterator.hasNext()) {
      endpointsIncidentEdges =
          Sets.union(incidentEdges(incidentNodesIterator.next()), endpointsIncidentEdges);
    }
    return Sets.difference(endpointsIncidentEdges, ImmutableSet.of(edge));
  }

  /**
   * If {@code node1} is equal to {@code node2}, the set of self-loop edges is returned.
   * Otherwise, returns the intersection of these two sets, using {@link Sets#intersection}:
   * <ol>
   * <li>Outgoing edges of {@code node1}.
   * <li>Incoming edges of {@code node2}.
   * </ol>
   */
  @Override
  public Set<E> edgesConnecting(Object node1, Object node2) {
    Set<E> outEdgesN1 = outEdges(node1); // Verifies that node1 is in graph
    if (node1.equals(node2)) {
      if (!config.isSelfLoopsAllowed()) {
        return ImmutableSet.of();
      }
      Set<E> selfLoopEdges = Sets.filter(outEdgesN1, Graphs.selfLoopPredicate(this));
      return Collections.unmodifiableSet(selfLoopEdges);
    }
    Set<E> inEdgesN2 = inEdges(node2);
    return (outEdgesN1.size() <= inEdgesN2.size())
        ? Sets.intersection(outEdgesN1, inEdgesN2)
        : Sets.intersection(inEdgesN2, outEdgesN1);
  }

  @Override
  public Set<E> inEdges(Object node) {
    return checkedConnections(node).inEdges();
  }

  @Override
  public Set<E> outEdges(Object node) {
    return checkedConnections(node).outEdges();
  }

  @Override
  public Set<N> predecessors(Object node) {
    return checkedConnections(node).predecessors();
  }

  @Override
  public Set<N> successors(Object node) {
    return checkedConnections(node).successors();
  }

  // Element Mutation

  @Override
  @CanIgnoreReturnValue
  public boolean addNode(N node) {
    checkNotNull(node, "node");
    if (nodes().contains(node)) {
      return false;
    }
    nodeConnections.put(node, newNodeConnections());
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
    checkArgument(config.isSelfLoopsAllowed() || !incidentNodes.isSelfLoop(),
        SELF_LOOPS_NOT_ALLOWED, node1);
    boolean containsN1 = nodes().contains(node1);
    boolean containsN2 = nodes().contains(node2);
    if (edges().contains(edge)) {
      checkArgument(containsN1 && containsN2 && edgesConnecting(node1, node2).contains(edge),
          REUSING_EDGE, edge, incidentNodes(edge), incidentNodes);
      return false;
    } else if (!config.isMultigraph()) {
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

  NodeConnections<N, E> checkedConnections(Object node) {
    checkNotNull(node, "node");
    NodeConnections<N, E> connections = nodeConnections.get(node);
    checkArgument(connections != null, NODE_NOT_IN_GRAPH, node);
    return connections;
  }

  IncidentNodes<N> checkedIncidentNodes(Object edge) {
    checkNotNull(edge, "edge");
    IncidentNodes<N> incidentNodes = edgeToIncidentNodes.get(edge);
    checkArgument(incidentNodes != null, EDGE_NOT_IN_GRAPH, edge);
    return incidentNodes;
  }
}
