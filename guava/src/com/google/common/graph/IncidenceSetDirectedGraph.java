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
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Adjacency-set-based implementation of a directed graph consisting of nodes
 * of type N and edges of type E.
 *
 * <p>{@link Graphs#createDirected()} should be used to get an instance of this class.
 *
 * <p>This class maintains the following for representing the directed graph data
 *    structure:
 * <ul>
 * <li>For each node: sets of incoming and outgoing edges.
 * <li>For each edge: references to the source and target nodes.
 * </ul>
 *
 * <p>Some invariants/assumptions are maintained in this implementation:
 * <ul>
 * <li>An edge has exactly two end-points (source node and target node), which
 *     may or may not be distinct.
 * <li>By default, this is not a multigraph, that is, parallel edges (multiple
 *     edges directed from n1 to n2) are not allowed.  If you want a multigraph,
 *     create the graph with the 'multigraph' option:
 *     <pre>Graphs.createDirected(Graphs.config().multigraph());</pre>
 * <li>Anti-parallel edges (same incident nodes but in opposite direction,
 *     e.g. (n1, n2) and (n2, n1)) are always allowed.
 * <li>By default, self-loop edges are allowed. If you want to disallow them,
 *     create the graph without the option of self-loops:
 *     <pre>Graphs.createDirected(Graphs.config().noSelfLoops());</pre>
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
 *     Set<N> preds = predecessors(n);
 *     graph.removeNode(n);}</pre>
 *     At this point, the contents of {@code preds} are undefined.
 * </ul>
 *
 * <p>The time complexity of all {@code Set}-returning accessors is O(1), since we
 * are returning views. It should be noted that for the following methods:
 * <ul>
 * <li>{@code incidentEdges}.
 * <li>Methods that ask for adjacent nodes (e.g. {@code predecessors}).
 * <li>{@code adjacentEdges}.
 * <li>{@code edgesConnecting}.
 * </ul>
 * the view is calculated lazily and the backing set is <b>not</b> cached, so every time the user
 * accesses the returned view, the backing set will be reconstructed again. If the user wants
 * to avoid this, they should either use {@code ImmutableDirectedGraph}
 * (if their input is not changing) or make a copy of the return value.
 *
 * <p>All other accessors have a time complexity of O(1), except for
 * {@code degree(n)}, whose time complexity is linear in the minimum of
 * the out-degree and in-degree of {@code n}, in case of allowing self-loop edges.
 * This is due to a call to {@code edgesConnecting}.
 *
 * <p>Time complexities for mutation methods:
 * <ul>
 * <li>{@code addNode}: O(1).
 * <li>{@code removeEdge}: O(1).
 * <li>{@code addEdge(E e, N n1, N n2)}: O(1), unless this graph is not a multigraph
 * (does not support parallel edges). In such case, this method may call
 * {@code edgesConnecting(n1, n2)}.
 * <li>{@code removeNode(n)}: O(d), where d is the degree of the node {@code n}.
 * </ul>
 *
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @see IncidenceSetUndirectedGraph
 * @see Graphs
 */
final class IncidenceSetDirectedGraph<N, E> implements DirectedGraph<N, E> {
 // TODO(b/24620028): Enable this class to support sorted nodes/edges.

  private final Map<N, IncidentEdges<E>> nodeToIncidentEdges;
  private final Map<E, IncidentNodes<N>> edgeToIncidentNodes;
  private final GraphConfig config;

  IncidenceSetDirectedGraph(GraphConfig config) {
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
    IncidentEdges<E> incidentEdges = nodeToIncidentEdges.get(node);
    checkArgument(incidentEdges != null, NODE_NOT_IN_GRAPH, node);
    return Sets.union(incidentEdges.inEdges(), incidentEdges.outEdges());
  }

  @Override
  public Set<N> incidentNodes(final Object edge) {
    checkNotNull(edge, "edge");
    // Returning an immutable set here as the edge's endpoints will not change anyway.
    IncidentNodes<N> endpoints = edgeToIncidentNodes.get(edge);
    checkArgument(endpoints != null, EDGE_NOT_IN_GRAPH, edge);
    return endpoints.asImmutableSet();
  }

  @Override
  public Set<N> adjacentNodes(Object node) {
    return Sets.union(predecessors(node), successors(node));
  }

  @Override
  public Set<E> adjacentEdges(Object edge) {
    checkNotNull(edge, "edge");
    IncidentNodes<N> endpoints = edgeToIncidentNodes.get(edge);
    checkArgument(endpoints != null, EDGE_NOT_IN_GRAPH, edge);
    return Sets.difference(
        Sets.union(incidentEdges(endpoints.target()), incidentEdges(endpoints.source())),
        ImmutableSet.of(edge));
  }

  /**
   * Returns the intersection of these two sets, using {@code Sets.intersection}:
   * <ol>
   * <li>Outgoing edges of {@code node1}.
   * <li>Incoming edges of {@code node2}.
   * </ol>
   * The first argument passed to {@code Sets.intersection} is the smaller of
   * the two sets.
   *
   * @see Sets#intersection
   */
  @Override
  public Set<E> edgesConnecting(Object node1, Object node2) {
    checkNotNull(node1, "node1");
    checkNotNull(node2, "node2");
    IncidentEdges<E> incidentEdgesN1 = nodeToIncidentEdges.get(node1);
    checkArgument(incidentEdgesN1 != null, NODE_NOT_IN_GRAPH, node1);
    IncidentEdges<E> incidentEdgesN2 = nodeToIncidentEdges.get(node2);
    checkArgument(incidentEdgesN2 != null, NODE_NOT_IN_GRAPH, node2);
    Set<E> outEdges = incidentEdgesN1.outEdges();
    Set<E> inEdges = incidentEdgesN2.inEdges();
    return outEdges.size() <= inEdges.size()
        ? Sets.intersection(outEdges, inEdges)
        : Sets.intersection(inEdges, outEdges);
  }

  @Override
  public Set<E> inEdges(Object node) {
    checkNotNull(node, "node");
    IncidentEdges<E> incidentEdges = nodeToIncidentEdges.get(node);
    checkArgument(incidentEdges != null, NODE_NOT_IN_GRAPH, node);
    return Collections.unmodifiableSet(incidentEdges.inEdges());
  }

  @Override
  public Set<E> outEdges(Object node) {
    checkNotNull(node, "node");
    IncidentEdges<E> incidentEdges = nodeToIncidentEdges.get(node);
    checkArgument(incidentEdges != null, NODE_NOT_IN_GRAPH, node);
    return Collections.unmodifiableSet(incidentEdges.outEdges());
  }

  @Override
  public Set<N> predecessors(Object node) {
    checkNotNull(node, "node");
    IncidentEdges<E> incidentEdges = nodeToIncidentEdges.get(node);
    checkArgument(incidentEdges != null, NODE_NOT_IN_GRAPH, node);
    final Set<E> inEdges = incidentEdges.inEdges();
    return new SetView<N>() {
      @Override
      public boolean isEmpty() {
        return inEdges.isEmpty();
      }

      @Override
      Set<N> elements() {
        Set<N> nodes = Sets.newLinkedHashSet();
        for (E edge : inEdges) {
          nodes.add(source(edge));
        }
        return nodes;
      }
    };
  }

  @Override
  public Set<N> successors(Object node) {
    checkNotNull(node, "node");
    IncidentEdges<E> incidentEdges = nodeToIncidentEdges.get(node);
    checkArgument(incidentEdges != null, NODE_NOT_IN_GRAPH, node);
    final Set<E> outEdges = incidentEdges.outEdges();
    return new SetView<N>() {
      @Override
      public boolean isEmpty() {
        return outEdges.isEmpty();
      }

      @Override
      Set<N> elements() {
        Set<N> nodes = Sets.newLinkedHashSet();
        for (E edge : outEdges) {
          nodes.add(target(edge));
        }
        return nodes;
      }
    };
  }

  @Override
  public long degree(Object node) {
    checkNotNull(node, "node");
    return config.isSelfLoopsAllowed()
        ? inDegree(node) + outDegree(node) - edgesConnecting(node, node).size()
        : inDegree(node) + outDegree(node);
  }

  @Override
  public long inDegree(Object node) {
    checkNotNull(node, "node");
    IncidentEdges<E> incidentEdges = nodeToIncidentEdges.get(node);
    checkArgument(incidentEdges != null, NODE_NOT_IN_GRAPH, node);
    return incidentEdges.inEdges().size();
  }

  @Override
  public long outDegree(Object node) {
    checkNotNull(node, "node");
    IncidentEdges<E> incidentEdges = nodeToIncidentEdges.get(node);
    checkArgument(incidentEdges != null, NODE_NOT_IN_GRAPH, node);
    return incidentEdges.outEdges().size();
  }

  @Override
  public N source(Object edge) {
    checkNotNull(edge, "edge");
    IncidentNodes<N> endpoints = edgeToIncidentNodes.get(edge);
    checkArgument(endpoints != null, EDGE_NOT_IN_GRAPH, edge);
    return endpoints.source();
  }

  @Override
  public N target(Object edge) {
    checkNotNull(edge, "edge");
    IncidentNodes<N> endpoints = edgeToIncidentNodes.get(edge);
    checkArgument(endpoints != null, EDGE_NOT_IN_GRAPH, edge);
    return endpoints.target();
  }

  // Element Mutation

  @Override
  public boolean addNode(N node) {
    checkNotNull(node, "node");
    if (containsNode(node)) {
      return false;
    }
    // TODO(user): Enable users to specify the expected number of neighbors
    // of a new node.
    nodeToIncidentEdges.put(node, IncidentEdges.<E>of());
    return true;
  }

  /**
   * Add nodes that are not elements of the graph, then add {@code edge} between them.
   * Return {@code false} if {@code edge} already exists between {@code node1} and {@code node2},
   * and in the same direction.
   *
   * <p>If this graph is not a multigraph (does not support parallel edges), this method may call
   * {@code edgesConnecting(node1, node2)} to discover whether node1 and node2 are already
   * connected.
   *
   * @throws IllegalArgumentException if an edge (other than {@code edge}) already
   *         exists from {@code node1} to {@code node2}, and this is not a multigraph.
   *         Also, if self-loops are not allowed, and {@code node1} is equal to {@code node2}.
   */
  @Override
  public boolean addEdge(E edge, N node1, N node2) {
    checkNotNull(edge, "edge");
    checkNotNull(node1, "node1");
    checkNotNull(node2, "node2");
    checkArgument(config.isSelfLoopsAllowed() || !node1.equals(node2),
        SELF_LOOPS_NOT_ALLOWED, node1);
    IncidentNodes<N> endpoints = edgeToIncidentNodes.get(edge);
    if (endpoints != null) {
      N source = endpoints.source();
      N target = endpoints.target();
      checkArgument(
          source.equals(node1) && target.equals(node2),
          REUSING_EDGE,
          edge,
          ImmutableList.of(source, target),
          ImmutableList.of(node1, node2));
      return false;
    } else if (!config.isMultigraph() && containsNode(node1) && containsNode(node2)) {
      E edgeConnecting = Iterables.getOnlyElement(edgesConnecting(node1, node2), null);
      checkArgument(edgeConnecting == null, ADDING_PARALLEL_EDGE, node1, node2, edgeConnecting);
    }
    addNode(node1);
    addNode(node2);
    edgeToIncidentNodes.put(edge, IncidentNodes.of(node1, node2));
    nodeToIncidentEdges.get(node1).outEdges().add(edge);
    nodeToIncidentEdges.get(node2).inEdges().add(edge);
    return true;
  }

  @Override
  public boolean removeNode(Object node) {
    checkNotNull(node, "node");
    // Return false if the node doesn't exist in the graph
    if (!containsNode(node)) {
      return false;
    }
    // Since views are returned, we need to copy the set of incident edges
    // to an equivalent collection to avoid removing the edges we are looping on.
    for (Object edge : incidentEdges(node).toArray()) {
      removeEdge(edge);
    }
    nodeToIncidentEdges.remove(node);
    return true;
  }

  @Override
  public boolean removeEdge(Object edge) {
    checkNotNull(edge, "edge");
    // Return false if the edge doesn't exist in the graph
    IncidentNodes<N> endpoints = edgeToIncidentNodes.get(edge);
    if (endpoints == null) {
      return false;
    }
    nodeToIncidentEdges.get(endpoints.source()).outEdges().remove(edge);
    nodeToIncidentEdges.get(endpoints.target()).inEdges().remove(edge);
    edgeToIncidentNodes.remove(edge);
    return true;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (other instanceof DirectedGraph) && Graphs.equal(this, (DirectedGraph) other);
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
