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
import static com.google.common.graph.GraphErrorMessageUtils.EDGE_NOT_IN_GRAPH;
import static com.google.common.graph.GraphErrorMessageUtils.NODE_NOT_IN_GRAPH;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Set;

/**
 * Implementation of an immutable directed graph consisting of nodes
 * of type N and edges of type E.
 *
 * <p>This class maintains the following data structures:
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
 *     edges directed from node1 to node2) are not allowed.  If you want a multigraph,
 *     build the graph with the 'multigraph' option:
 *     <pre>ImmutableDirectedGraph.builder(Graphs.config().multigraph()).build();</pre>
 * <li>Anti-parallel edges (same incident nodes but in opposite direction,
 *     e.g. (node1, node2) and (node2, node1)) are always allowed.
 * <li>Edges are not adjacent to themselves by definition. In the case of a
 *     self-loop, a node can be adjacent to itself, but an edge will never be.
 * </ul>
 *
 * <p>The time complexity of all {@code Set}-returning accessors is O(1), since we
 * are returning views. An exception to this is {@code edgesConnecting(node1, node2)},
 * which is O(min(outD_node1, inD_node2)).
 *
 * <p>All other accessors have a time complexity of O(1), except for {@code degree(node)},
 * whose time complexity is O(outD_node).
 *
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @see Graphs
 * @since 20.0
 */
// TODO(user): Add support for sorted nodes/edges and/or hypergraphs.
@Beta
public final class ImmutableDirectedGraph<N, E> extends AbstractImmutableGraph<N, E>
    implements DirectedGraph<N, E> {

  // All nodes in the graph exist in this map
  private final ImmutableMap<N, NodeConnections<N, E>> nodeConnections;
  // All edges in the graph exist in this map
  private final ImmutableMap<E, IncidentNodes<N>> edgeToIncidentNodes;

  private ImmutableDirectedGraph(Builder<N, E> builder) {
    super(builder.directedGraph.config());
    DirectedGraph<N, E> directedGraph = builder.directedGraph;
    ImmutableMap.Builder<N, NodeConnections<N, E>> nodeConnectionsBuilder =
        ImmutableMap.builder();
    for (N node : directedGraph.nodes()) {
      NodeConnections<N, E> connections = DirectedNodeConnections.ofImmutable(
          directedGraph.predecessors(node), directedGraph.successors(node),
          directedGraph.inEdges(node), directedGraph.outEdges(node));
      nodeConnectionsBuilder.put(node, connections);
    }
    this.nodeConnections = nodeConnectionsBuilder.build();
    ImmutableMap.Builder<E, IncidentNodes<N>> edgeToIncidentNodesBuilder = ImmutableMap.builder();
    for (E edge : directedGraph.edges()) {
      IncidentNodes<N> incidentNodes = IncidentNodes.of(
          directedGraph.source(edge), directedGraph.target(edge));
      edgeToIncidentNodesBuilder.put(edge, incidentNodes);
    }
    this.edgeToIncidentNodes = edgeToIncidentNodesBuilder.build();
  }

  @Override
  public Set<N> nodes() {
    return nodeConnections.keySet();
  }

  @Override
  public Set<E> edges() {
    return edgeToIncidentNodes.keySet();
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
    ImmutableSet.Builder<E> adjacentEdges = ImmutableSet.builder();
    for (N endpoint : incidentNodes(edge)) {
      for (E adjacentEdge : incidentEdges(endpoint)) {
        if (!edge.equals(adjacentEdge)) { // Edges are not adjacent to themselves by definition.
          adjacentEdges.add(adjacentEdge);
        }
      }
    }
    return adjacentEdges.build();
  }

  /**
   * Returns the intersection of these two sets, using {@link Sets#intersection}:
   * <ol>
   * <li>Outgoing edges of {@code node1}.
   * <li>Incoming edges of {@code node2}.
   * </ol>
   */
  @Override
  public Set<E> edgesConnecting(Object node1, Object node2) {
    Set<E> sourceOutEdges = outEdges(node1); // Verifies that node1 is in graph
    if (!config.isSelfLoopsAllowed() && node1.equals(node2)) {
      return ImmutableSet.of();
    }
    Set<E> targetInEdges = inEdges(node2);
    return (sourceOutEdges.size() <= targetInEdges.size())
        ? Sets.intersection(sourceOutEdges, targetInEdges).immutableCopy()
        : Sets.intersection(targetInEdges, sourceOutEdges).immutableCopy();
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

  @Override
  public N source(Object edge) {
    return checkedIncidentNodes(edge).node1();
  }

  @Override
  public N target(Object edge) {
    return checkedIncidentNodes(edge).node2();
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

  /**
   * Returns a new builder. The generated builder is equivalent to the builder
   * created by the {@code Builder} constructor.
   */
  public static <N, E> Builder<N, E> builder() {
    return new Builder<N, E>();
  }

  /**
   * Returns a new builder. The generated builder is equivalent to the builder
   * created by the {@code Builder} constructor.
   *
   * @param config an instance of {@code GraphConfig} with the intended
   *        graph configuration.
   */
  public static <N, E> Builder<N, E> builder(GraphConfig config) {
    return new Builder<N, E>(config);
  }

  /**
   * Returns an immutable copy of the input graph.
   */
  public static <N, E> ImmutableDirectedGraph<N, E> copyOf(DirectedGraph<N, E> graph) {
    return new Builder<N, E>(graph).build();
  }

  /**
   * A builder for creating immutable directed graph instances.
   *
   * @param <N> Node parameter type
   * @param <E> edge parameter type
   * @see GraphConfig
   */
  public static final class Builder<N, E> implements AbstractImmutableGraph.Builder<N, E> {

    private final DirectedGraph<N, E> directedGraph;

    /**
     * Creates a new builder with the default graph configuration.
     */
    public Builder() {
      this(Graphs.<N, E>createDirected());
    }

    /**
     * Creates a new builder with the specified graph configuration.
     */
    public Builder(GraphConfig config) {
      this(Graphs.<N, E>createDirected(config));
    }

    /**
     * Creates a new builder whose internal state is that of {@code graph}.
     *
     * <p>NOTE: This constructor should only be used in the case where it will be immediately
     * followed by a call to {@code build}, to ensure that the input graph will not be modified.
     * Currently the only such context is {@code Immutable*Graph.copyOf()}, which use these
     * constructors to avoid making an extra copy of the graph state.
     * @see ImmutableDirectedGraph#copyOf(DirectedGraph)
     */
    private Builder(DirectedGraph<N, E> graph) {
      this.directedGraph = checkNotNull(graph, "graph");
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<N, E> addNode(N node) {
      directedGraph.addNode(node);
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<N, E> addEdge(E edge, N node1, N node2) {
      directedGraph.addEdge(edge, node1, node2);
      return this;
    }

    /**
     * Adds all elements of {@code graph} to the graph being built.
     *
     * @throws IllegalArgumentException under either of two conditions:
     *     (1) the {@code GraphConfig} objects held by the graph being built and by {@code graph}
     *     are not compatible
     *     (2) calling {@code Graph.addEdge(e, n1, n2)} on the graph being built throws IAE
     * @see Graph#addEdge
     */
    @CanIgnoreReturnValue
    public Builder<N, E> addGraph(DirectedGraph<N, E> graph) {
      checkArgument(
          directedGraph.config().compatibleWith(graph.config()),
          "GraphConfigs for input and for graph being built are not compatible: input: %s, "
              + "this graph: %s",
          graph.config(),
          directedGraph.config());

      for (N node : graph.nodes()) {
        directedGraph.addNode(node);
      }
      for (E edge : graph.edges()) {
        directedGraph.addEdge(edge, graph.source(edge), graph.target(edge));
      }

      return this;
    }

    @Override
    public ImmutableDirectedGraph<N, E> build() {
      return new ImmutableDirectedGraph<N, E>(this);
    }
  }
}
