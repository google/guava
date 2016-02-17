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
 * Adjacency-set-based implementation of an undirected graph consisting of nodes
 * of type N and edges of type E.
 *
 * <p>{@link Graphs#createUndirected} should be used to get an instance of this class.
 *
 * <p>This class maintains the following for representing the undirected graph
 *    data structure:
 * <ul>
 * <li>For each node: set of incident edges.
 * <li>For each edge: references to incident nodes.
 * </ul>
 *
 * <p>Some invariants/assumptions are maintained in this implementation:
 * <ul>
 * <li>An edge has exactly two end-points, which may or may not be distinct.
 * <li>By default, this is not a multigraph, that is, parallel edges (multiple
 *     edges between node1 and node2) are not allowed.  If you want a multigraph,
 *     create the graph with the 'multigraph' option:
 *     <pre>Graphs.createUndirected(Graphs.config().multigraph());</pre>
 * <li>By default, self-loop edges are allowed. If you want to disallow them,
 *     create the graph without the option of self-loops:
 *     <pre>Graphs.createUndirected(Graphs.config().noSelfLoops());</pre>
 * <li>Edges are not adjacent to themselves by definition. In the case of a
 *     self-loop, a node can be adjacent to itself, but an edge will never be.
 * </ul>
 *
 * <p>{@code Set}-returning accessors return unmodifiable views: the view returned
 * will reflect changes to the graph, but may not be modified by the user.
 * The behavior of the returned view is undefined in the following cases:
 * <ul>
 * <li>Removing the element on which the accessor is called (e.g.:
 *     <pre>{@code
 *     Set<N> adjNodes = adjacentNodes(node);
 *     graph.removeNode(node);}</pre>
 *     At this point, the contents of {@code adjNodes} are undefined.
 * </ul>
 *
 * <p>The time complexity of all {@code Set}-returning accessors is O(1), since we
 * are returning views.
 *
 * <p>All other accessors have a time complexity of O(1).
 *
 * <p>Time complexities for mutation methods:
 * <ul>
 * <li>{@code addNode}: O(1).
 * <li>{@code addEdge(E edge, N node1, N node2)}: O(1).
 * <li>{@code removeNode(node)}: O(d_node).
 * <li>{@code removeEdge}: O(1), unless this graph is a multigraph (supports parallel edges);
 *     in that case this method is O(min(d_edgeNode1, d_edgeNode2)).
 *
 * </ul>
 * where d_node is the degree of node.
 *
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @see IncidenceSetDirectedGraph
 * @see Graphs
 */
final class IncidenceSetUndirectedGraph<N, E> extends AbstractGraph<N, E>
    implements UndirectedGraph<N, E> {
  // TODO(b/24620028): Enable this class to support sorted nodes/edges.

  // All nodes in the graph exist in this map
  private final Map<N, NodeConnections<N, E>> nodeConnections;
  // All edges in the graph exist in this map
  private final Map<E, IncidentNodes<N>> edgeToIncidentNodes;

  IncidenceSetUndirectedGraph(GraphConfig config) {
    super(config);
    // The default of 11 is rather arbitrary, but roughly matches the sizing of just new HashMap()
    this.nodeConnections =
        Maps.newLinkedHashMapWithExpectedSize(config.getExpectedNodeCount().or(11));
    this.edgeToIncidentNodes =
        Maps.newLinkedHashMapWithExpectedSize(config.getExpectedEdgeCount().or(11));
  }

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
   * <li>Incident edges of {@code node1}.
   * <li>Incident edges of {@code node2}.
   * </ol>
   */
  @Override
  public Set<E> edgesConnecting(Object node1, Object node2) {
    Set<E> incidentEdgesN1 = incidentEdges(node1);
    if (node1.equals(node2)) {
      if (!config.isSelfLoopsAllowed()) {
        return ImmutableSet.of();
      }
      Set<E> selfLoopEdges = Sets.filter(incidentEdgesN1, Graphs.selfLoopPredicate(this));
      return Collections.unmodifiableSet(selfLoopEdges);
    }
    Set<E> incidentEdgesN2 = incidentEdges(node2);
    return (incidentEdgesN1.size() <= incidentEdgesN2.size())
        ? Sets.intersection(incidentEdgesN1, incidentEdgesN2)
        : Sets.intersection(incidentEdgesN2, incidentEdgesN1);
  }

  @Override
  public Set<E> inEdges(Object node) {
    return incidentEdges(node);
  }

  @Override
  public Set<E> outEdges(Object node) {
    return incidentEdges(node);
  }

  @Override
  public Set<N> predecessors(Object node) {
    return adjacentNodes(node);
  }

  @Override
  public Set<N> successors(Object node) {
    return adjacentNodes(node);
  }

  // Element Mutation

  @Override
  @CanIgnoreReturnValue
  public boolean addNode(N node) {
    checkNotNull(node, "node");
    if (nodes().contains(node)) {
      return false;
    }
    nodeConnections.put(node, UndirectedNodeConnections.<N, E>of());
    return true;
  }

  /**
   * Add nodes that are not elements of the graph, then add {@code edge}
   * between them. Return {@code false} if {@code edge} already exists between
   * {@code node1} and {@code node2}.
   *
   * <p>If this graph is not a multigraph (does not support parallel edges), this
   * method may call {@code edgesConnecting(node1, node2)} to discover whether node1 and node2 are
   * already connected.
   *
   * @throws IllegalArgumentException if an edge (other than {@code edge}) already
   *         exists between {@code node1} and {@code node2}, and this is not a multigraph.
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
    IncidentNodes<N> previousIncidentNodes = edgeToIncidentNodes.get(edge);
    if (previousIncidentNodes != null) {
      checkArgument(previousIncidentNodes.equals(incidentNodes),
          REUSING_EDGE, edge, previousIncidentNodes, incidentNodes);
      return false;
    } else if (!config.isMultigraph()) {
      checkArgument(!(nodes().contains(node1) && successors(node1).contains(node2)),
          ADDING_PARALLEL_EDGE, node1, node2);
    }
    addNode(node1);
    NodeConnections<N, E> connectionsN1 = nodeConnections.get(node1);
    connectionsN1.addSuccessor(node2, edge);
    if (!incidentNodes.isSelfLoop()) {
      addNode(node2);
      NodeConnections<N, E> connectionsN2 = nodeConnections.get(node2);
      connectionsN2.addPredecessor(node1, edge);
    }
    edgeToIncidentNodes.put(edge, incidentNodes);
    return true;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean removeNode(Object node) {
    checkNotNull(node, "node");
    // Return false if the node doesn't exist in the graph
    NodeConnections<N, E> connections = nodeConnections.get(node);
    if (connections == null) {
      return false;
    }
    // Since views are returned, we need to copy the edges that will be removed.
    // Thus we avoid modifying the underlying view while iterating over it.
    for (E edge : ImmutableList.copyOf(incidentEdges(node))) {
      removeEdge(edge);
    }
    nodeConnections.remove(node);
    return true;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean removeEdge(Object edge) {
    checkNotNull(edge, "edge");
    // Return false if the edge doesn't exist in the graph
    IncidentNodes<N> incidentNodes = edgeToIncidentNodes.get(edge);
    if (incidentNodes == null) {
      return false;
    }
    N node1 = incidentNodes.node1();
    N node2 = incidentNodes.node2();
    NodeConnections<N, E> node1Connections = nodeConnections.get(node1);
    NodeConnections<N, E> node2Connections = nodeConnections.get(node2);
    if (!config.isMultigraph() || edgesConnecting(node1, node2).size() <= 1) {
      // If this is the last connecting edge between node1 and node2, they are no longer adjacent.
      node1Connections.removeSuccessor(node2);
      node2Connections.removePredecessor(node1);
    }
    node1Connections.removeOutEdge(edge);
    node2Connections.removeInEdge(edge);
    edgeToIncidentNodes.remove(edge);
    return true;
  }

  private NodeConnections<N, E> checkedConnections(Object node) {
    checkNotNull(node, "node");
    NodeConnections<N, E> connections = nodeConnections.get(node);
    checkArgument(connections != null, NODE_NOT_IN_GRAPH, node);
    return connections;
  }

  private IncidentNodes<N> checkedIncidentNodes(Object edge) {
    checkNotNull(edge, "edge");
    IncidentNodes<N> incidentNodes = edgeToIncidentNodes.get(edge);
    checkArgument(incidentNodes != null, EDGE_NOT_IN_GRAPH, edge);
    return incidentNodes;
  }
}
