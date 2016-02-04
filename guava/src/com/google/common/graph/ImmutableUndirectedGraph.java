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
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Implementation of an immutable undirected graph consisting of nodes of type N
 * and edges of type E.
 *
 * <p>This class maintains the following data structures:
 * <ul>
 * <li>For each node: set of incident edges.
 * <li>For each edge: references to incident nodes.
 * </ul>
 *
 * <p>Some invariants/assumptions are maintained in this implementation:
 * <ul>
 * <li>An edge has exactly two end-points, which may or may not be distinct.
 * <li>By default, this is not a multigraph, that is, parallel edges (multiple
 *     edges between n1 and n2) are not allowed.  If you want a multigraph,
 *     build the graph with the 'multigraph' option:
 *     <pre>ImmutableUndirectedGraph.builder(Graphs.config().multigraph()).build();</pre>
 * <li>Edges are not adjacent to themselves by definition. In the case of a
 *     self-loop, a node can be adjacent to itself, but an edge will never be adjacent to itself.
 * </ul>
 *
 * <p>The time complexity of all {@code Set}-returning accessors is O(1), since we
 * are returning views. An exception to this is {@code edgesConnecting(node1, node2)},
 * which is O(min(d_node1, d_node2)).
 *
 * <p>All other accessors have a time complexity of O(1).
 *
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @see Graphs
 * @since 20.0
 */
// TODO(user): Add support for sorted nodes/edges and/or hypergraphs.
@Beta
public final class ImmutableUndirectedGraph<N, E> extends AbstractImmutableGraph<N, E>
    implements UndirectedGraph<N, E> {
  // All nodes in the graph exist in this map
  private final ImmutableMap<N, NodeConnections<N, E>> nodeConnections;
  // All edges in the graph exist in this map
  private final ImmutableMap<E, UndirectedIncidentNodes<N>> edgeToIncidentNodes;
  private final GraphConfig config;

  private ImmutableUndirectedGraph(Builder<N, E> builder) {
    UndirectedGraph<N, E> undirectedGraph = builder.undirectedGraph;
    ImmutableMap.Builder<N, NodeConnections<N, E>> nodeConnectionsBuilder =
        ImmutableMap.builder();
    for (N node : undirectedGraph.nodes()) {
      nodeConnectionsBuilder.put(node, UndirectedNodeConnections.ofImmutable(
          undirectedGraph.adjacentNodes(node), undirectedGraph.incidentEdges(node)));
    }
    this.nodeConnections = nodeConnectionsBuilder.build();
    ImmutableMap.Builder<E, UndirectedIncidentNodes<N>> edgeToNodesBuilder = ImmutableMap.builder();
    for (E edge : undirectedGraph.edges()) {
      edgeToNodesBuilder.put(edge, UndirectedIncidentNodes.of(undirectedGraph.incidentNodes(edge)));
    }
    this.edgeToIncidentNodes = edgeToNodesBuilder.build();
    this.config = undirectedGraph.config();
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
  public GraphConfig config() {
    return config;
  }

  @Override
  public Set<E> incidentEdges(Object node) {
    return checkedConnections(node).incidentEdges();
  }

  @Override
  public Set<N> incidentNodes(Object edge) {
    checkNotNull(edge, "edge");
    UndirectedIncidentNodes<N> incidentNodes = edgeToIncidentNodes.get(edge);
    checkArgument(incidentNodes != null, EDGE_NOT_IN_GRAPH, edge);
    return incidentNodes;
  }

  @Override
  public Set<N> adjacentNodes(Object node) {
    return checkedConnections(node).adjacentNodes();
  }

  @Override
  public Set<E> adjacentEdges(Object edge) {
    Set<E> adjacentEdges = Sets.newLinkedHashSet();
    for (N node : incidentNodes(edge)) {
      adjacentEdges.addAll(incidentEdges(node));
    }
    // Edges are not adjacent to themselves by definition.
    adjacentEdges.remove(edge);
    return Collections.unmodifiableSet(adjacentEdges);
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
      return ImmutableSet.copyOf(Iterables.filter(incidentEdgesN1, Graphs.selfLoopPredicate(this)));
    }
    Set<E> incidentEdgesN2 = incidentEdges(node2);
    return (incidentEdgesN1.size() <= incidentEdgesN2.size())
        ? Sets.intersection(incidentEdgesN1, incidentEdgesN2).immutableCopy()
        : Sets.intersection(incidentEdgesN2, incidentEdgesN1).immutableCopy();
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
    return incidentEdges(node).size();
  }

  @Override
  public long inDegree(Object node) {
    return degree(node);
  }

  @Override
  public long outDegree(Object node) {
    return degree(node);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    return (object instanceof UndirectedGraph)
        && Graphs.equal(this, (UndirectedGraph<?, ?>) object);
  }

  @Override
  public int hashCode() {
    // The node set is included in the hash to differentiate between graphs with isolated nodes.
    return Objects.hashCode(nodes(), edgeToIncidentNodes);
  }

  @Override
  public String toString() {
    return String.format("config: %s, nodes: %s, edges: %s",
        config,
        nodes(),
        edgeToIncidentNodes);
  }

  private NodeConnections<N, E> checkedConnections(Object node) {
    checkNotNull(node, "node");
    NodeConnections<N, E> connections = nodeConnections.get(node);
    checkArgument(connections != null, NODE_NOT_IN_GRAPH, node);
    return connections;
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
  public static <N, E> ImmutableUndirectedGraph<N, E> copyOf(UndirectedGraph<N, E> graph) {
    return new Builder<N, E>(graph).build();
  }

  /**
   * A builder for creating immutable undirected graph instances.
   *
   * @param <N> Node parameter type
   * @param <E> edge parameter type
   * @see GraphConfig
   */
  public static final class Builder<N, E> implements AbstractImmutableGraph.Builder<N, E> {

    private final UndirectedGraph<N, E> undirectedGraph;

    /**
     * Creates a new builder with the default graph configuration.
     */
    public Builder() {
      this(Graphs.<N, E>createUndirected());
    }

    /**
     * Creates a new builder with the specified configuration.
     */
    public Builder(GraphConfig config) {
      this(Graphs.<N, E>createUndirected(config));
    }

    /**
     * Creates a new builder whose internal state is that of {@code graph}.
     *
     * <p>NOTE: This constructor should only be used in the case where it will be immediately
     * followed by a call to {@code build}, so that the input graph will not be modified.
     * Currently the only such contexts are {@code Immutable*Graph.copyOf()}, which use these
     * constructors to avoid making an extra copy of the graph state.
     * @see ImmutableUndirectedGraph#copyOf(UndirectedGraph)
     */
    private Builder(UndirectedGraph<N, E> undirectedGraph) {
      this.undirectedGraph = checkNotNull(undirectedGraph, "undirectedGraph");
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<N, E> addNode(N node) {
      undirectedGraph.addNode(node);
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<N, E> addEdge(E edge, N node1, N node2) {
      undirectedGraph.addEdge(edge, node1, node2);
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
    public Builder<N, E> addGraph(UndirectedGraph<N, E> graph) {
      checkArgument(
          undirectedGraph.config().compatibleWith(graph.config()),
          "GraphConfigs for input and for graph being built are not compatible: input: %s, "
              + "this graph: %s",
          graph.config(),
          undirectedGraph.config());

      for (N node : graph.nodes()) {
        undirectedGraph.addNode(node);
      }
      for (E edge : graph.edges()) {
        Graphs.addEdge(undirectedGraph, edge, graph.incidentNodes(edge));
      }

      return this;
    }

    @Override
    public ImmutableUndirectedGraph<N, E> build() {
      return new ImmutableUndirectedGraph<N, E>(this);
    }
  }
}
