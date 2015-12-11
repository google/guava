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
import static com.google.common.graph.Graphs.oppositeNode;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

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
 * are returning views. It should be noted that for the following methods:
 * <ul>
 * <li>Methods that ask for adjacent nodes (e.g. {@code adjacentNodes}).
 * <li>{@code adjacentEdges}.
 * <li>{@code edgesConnecting}.
 * </ul>
 * the view is calculated lazily and the backing set is <b>not</b> cached, so every time the user
 * accesses the returned view, the backing set will be reconstructed again. If the user wants
 * to avoid this, they should either use {@code ImmutableUndirectedGraph}
 * (if their input is not changing) or make a copy of the return value.
 *
 * <p>All other accessors have a time complexity of O(1).
 *
 * <p>Time complexities for mutation methods:
 * <ul>
 * <li>{@code addNode}: O(1).
 * <li>{@code removeEdge}: O(1).
 * <li>{@code addEdge(E edge, N node1, N node2)}: O(1), unless this graph is not a multigraph
 * (does not support parallel edges). In such case, this method may call
 * {@code edgesConnecting(node1, node2)}.
 * <li>{@code removeNode(node)} O(d), where d is the degree of {@code node}.
 * </ul>
 *
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @see IncidenceSetDirectedGraph
 * @see Graphs
 */
final class IncidenceSetUndirectedGraph<N, E> implements UndirectedGraph<N, E> {
  // TODO(b/24620028): Enable this class to support sorted nodes/edges.

  // All nodes in the graph exist in this map
  private final Map<N, Set<E>> nodeToIncidentEdges;
  // All edges in the graph exist in this map
  private final Map<E, ImmutableSet<N>> edgeToIncidentNodes;
  private final GraphConfig config;

  IncidenceSetUndirectedGraph(GraphConfig config) {
    // The default of 11 is rather arbitrary, but roughly matches the sizing of just new HashMap()
    this.nodeToIncidentEdges =
        Maps.newLinkedHashMapWithExpectedSize(config.getExpectedNodeCount().or(11));
    this.edgeToIncidentNodes =
        Maps.newLinkedHashMapWithExpectedSize(config.getExpectedEdgeCount().or(11));
    this.config = config;
  }

  @Override
  public Set<N> nodes() {
    return Collections.unmodifiableSet(nodeToIncidentEdges.keySet());
  }

  @Override
  public Set<E> edges() {
    return Collections.unmodifiableSet(edgeToIncidentNodes.keySet());
  }

  @Override
  public GraphConfig config() {
    return config;
  }

  @Override
  public Set<E> incidentEdges(Object node) {
    checkNotNull(node, "node");
    Set<E> incidentEdges = nodeToIncidentEdges.get(node);
    checkArgument(incidentEdges != null, NODE_NOT_IN_GRAPH, node);
    return Collections.unmodifiableSet(incidentEdges);
  }

  @Override
  public Set<N> incidentNodes(Object edge) {
    checkNotNull(edge, "edge");
    Set<N> incidentNodes = edgeToIncidentNodes.get(edge);
    checkArgument(incidentNodes != null, EDGE_NOT_IN_GRAPH, edge);
    return Collections.unmodifiableSet(incidentNodes);
  }

  @Override
  public Set<N> adjacentNodes(final Object node) {
    checkNotNull(node, "node");
    final Set<E> incidentEdges = nodeToIncidentEdges.get(node);
    checkArgument(incidentEdges != null, NODE_NOT_IN_GRAPH, node);
    return new SetView<N>() {
      @Override
      public boolean isEmpty() {
        return incidentEdges.isEmpty();
      }

      @Override
      Set<N> elements() {
        Set<N> nodes = Sets.newLinkedHashSetWithExpectedSize(incidentEdges.size());
        for (E edge : incidentEdges) {
          nodes.add(oppositeNode(IncidenceSetUndirectedGraph.this, edge, node));
        }
        return nodes;
      }
    };
  }

  @Override
  public Set<E> adjacentEdges(Object edge) {
    checkNotNull(edge, "edge");
    Set<N> incidentNodes = edgeToIncidentNodes.get(edge);
    checkArgument(incidentNodes != null, EDGE_NOT_IN_GRAPH, edge);
    Object[] endpoints = incidentNodes.toArray();
    Set<E> endpointsIncidentEdges =
        endpoints.length == 1
            ? incidentEdges(endpoints[0])
            : Sets.union(incidentEdges(endpoints[0]), incidentEdges(endpoints[1]));
    return Sets.difference(endpointsIncidentEdges, ImmutableSet.of(edge));
  }

  /**
   * If {@code node1} is equal to {@code node2} and self-loops are allowed (if self-loops
   * are not allowed, this would be a trivial case and an empty set is returned),
   * a {@code SetView} instance is returned, calculating the set of self-loop edges.
   * Otherwise, this method returns the intersection of these two sets,
   * using {@code Sets.intersection}:
   * <ol>
   * <li>Incident edges of {@code node1}.
   * <li>Incident edges of {@code node2}.
   * </ol>
   * The first argument passed to {@code Sets.intersection} is the smaller of
   * the two sets.
   *
   * @see Sets#intersection
   */
  @Override
  public Set<E> edgesConnecting(final Object node1, Object node2) {
    checkNotNull(node1, "node1");
    checkNotNull(node2, "node2");
    final Set<E> incidentEdgesN1 = nodeToIncidentEdges.get(node1);
    checkArgument(incidentEdgesN1 != null, NODE_NOT_IN_GRAPH, node1);
    if (node1.equals(node2)) {
      if (!config.isSelfLoopsAllowed()) {
        return ImmutableSet.of();
      }
      return new SetView<E>() {
        @Override
        Set<E> elements() {
          Set<E> selfLoopEdges = Sets.newLinkedHashSet();
          for (E edge : incidentEdgesN1) {
            // An edge is a self-loop iff it has exactly one incident node.
            if (edgeToIncidentNodes.get(edge).size() == 1) {
              selfLoopEdges.add(edge);
            }
          }
          return selfLoopEdges;
        }
      };
    }
    final Set<E> incidentEdgesN2 = nodeToIncidentEdges.get(node2);
    checkArgument(incidentEdgesN2 != null, NODE_NOT_IN_GRAPH, node2);
    return incidentEdgesN1.size() <= incidentEdgesN2.size()
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

  @Override
  public long degree(Object node) {
    checkNotNull(node, "node");
    Set<E> incidentEdges = nodeToIncidentEdges.get(node);
    checkArgument(incidentEdges != null, NODE_NOT_IN_GRAPH, node);
    return incidentEdges.size();
  }

  @Override
  public long inDegree(Object node) {
    return degree(node);
  }

  @Override
  public long outDegree(Object node) {
    return degree(node);
  }

  // Element Mutation

  @Override
  public boolean addNode(N node) {
    checkNotNull(node, "node");
    if (containsNode(node)) {
      return false;
    }
    // TODO(user): Enable users to specify expected number of neighbors for
    // a node.
    nodeToIncidentEdges.put(node, new LinkedHashSet<E>());
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
  public boolean addEdge(E edge, N node1, N node2) {
    checkNotNull(edge, "edge");
    checkNotNull(node1, "node1");
    checkNotNull(node2, "node2");
    checkArgument(config.isSelfLoopsAllowed() || !node1.equals(node2),
        SELF_LOOPS_NOT_ALLOWED, node1);
    ImmutableSet<N> endpoints = ImmutableSet.of(node1, node2);
    Set<N> incidentNodes = edgeToIncidentNodes.get(edge);
    if (incidentNodes != null) {
      checkArgument(incidentNodes.equals(endpoints), REUSING_EDGE, edge, incidentNodes, endpoints);
      return false;
    } else if (!config.isMultigraph() && containsNode(node1) && containsNode(node2)) {
      E edgeConnecting = Iterables.getOnlyElement(edgesConnecting(node1, node2), null);
      checkArgument(edgeConnecting == null, ADDING_PARALLEL_EDGE, node1, node2, edgeConnecting);
    }
    addNode(node1);
    addNode(node2);
    edgeToIncidentNodes.put(edge, endpoints);
    nodeToIncidentEdges.get(node1).add(edge);
    if (!node1.equals(node2)) {
      nodeToIncidentEdges.get(node2).add(edge);
    }
    return true;
  }

  @Override
  public boolean removeNode(Object node) {
    checkNotNull(node, "node");
    // Return false if the node doesn't exist in the graph
    Set<E> incidentEdges = nodeToIncidentEdges.get(node);
    if (incidentEdges == null) {
      return false;
    }
    // Since views are returned, we need to copy the set of incident edges
    // to an equivalent collection to avoid removing the edges we are looping on.
    for (Object edge : incidentEdges.toArray()) {
      removeEdge(edge);
    }
    nodeToIncidentEdges.remove(node);
    return true;
  }

  @Override
  public boolean removeEdge(Object edge) {
    checkNotNull(edge, "edge");
    // Return false if the edge doesn't exist in the graph
    Set<N> incidentNodes = edgeToIncidentNodes.get(edge);
    if (incidentNodes == null) {
      return false;
    }
    for (N node : incidentNodes) {
      nodeToIncidentEdges.get(node).remove(edge);
    }
    edgeToIncidentNodes.remove(edge);
    return true;
  }

  @Override
  public boolean equals(@Nullable Object object) {
    return (object instanceof UndirectedGraph) && Graphs.equal(this, (UndirectedGraph) object);
  }

  @Override
  public int hashCode() {
    // This map encapsulates all of the structural relationships of this graph, so its hash code
    // is consistent with the above definition of equals().
    return nodeToIncidentEdges.hashCode();
  }

  @Override
  public String toString() {
    return String.format("config: %s, nodes: %s, edges: %s",
        config,
        nodeToIncidentEdges.keySet(),
        edgeToIncidentNodes);
  }

  private boolean containsNode(Object node) {
    return nodeToIncidentEdges.containsKey(node);
  }
}
