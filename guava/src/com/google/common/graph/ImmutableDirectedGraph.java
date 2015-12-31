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

import java.util.Collections;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

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
 * <p>Most of the {@code Set}-returning accessors return an immutable set which is an internal
 * data structure, hence they have a time complexity of O(1). The rest of these accessors build
 * and return an immutable set that is <b>derived</b> from the internal data structures, hence
 * they have the following time complexities:
 * <ul>
 * <li>{@code incidentEdges(node)}: O(d_node).
 * <li>Methods that ask for adjacent nodes:
 *     <ul>
 *     <li>{@code adjacentNodes(node)}: O(d_node).
 *     <li>{@code predecessors(node)}: O(inD_node).
 *     <li>{@code successors(node)}: O(outD_node).
 *     </ul>
 * <li>{@code adjacentEdges(edge)}: O(d_node1 + d_node2), where node1 and node2 are
 *     {@code edge}'s incident nodes.
 * <li>{@code edgesConnecting(node1, node2)}: O(min(outD_node1, inD_node2)).
 * </ul>
 * where d_node is the degree of node, inD_node is the in-degree of node, and outD_node is the
 * out-degree of node. The set returned by these methods is <b>not</b> cached, so every time the
 * user calls the method, the same set will be reconstructed again.
 *
 * <p>All other accessors have a time complexity of O(1), except for
 * {@code degree(node)}, whose time complexity is linear in the minimum of
 * the out-degree and in-degree of {@code node}. This is due to a call to {@code edgesConnecting}.
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
  private final ImmutableMap<N, IncidentEdges<E>> nodeToIncidentEdges;
  // All edges in the graph exist in this map
  private final ImmutableMap<E, IncidentNodes<N>> edgeToIncidentNodes;
  private final GraphConfig config;

  private ImmutableDirectedGraph(Builder<N, E> builder) {
    DirectedGraph<N, E> directedGraph = builder.directedGraph;
    ImmutableMap.Builder<N, IncidentEdges<E>> nodeToIncidentEdgesBuilder = ImmutableMap.builder();
    for (N node : directedGraph.nodes()) {
      IncidentEdges<E> incidentEdges =
          IncidentEdges.ofImmutable(directedGraph.inEdges(node), directedGraph.outEdges(node));
      nodeToIncidentEdgesBuilder.put(node, incidentEdges);
    }
    nodeToIncidentEdges = nodeToIncidentEdgesBuilder.build();
    ImmutableMap.Builder<E, IncidentNodes<N>> edgeToIncidentNodesBuilder = ImmutableMap.builder();
    for (E edge : directedGraph.edges()) {
      IncidentNodes<N> incidentNodes =
          IncidentNodes.of(directedGraph.source(edge), directedGraph.target(edge));
      edgeToIncidentNodesBuilder.put(edge, incidentNodes);
    }
    edgeToIncidentNodes = edgeToIncidentNodesBuilder.build();
    this.config = directedGraph.config();
  }

  @Override
  public Set<N> nodes() {
    return nodeToIncidentEdges.keySet();
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
    return Sets.union(inEdges(node), outEdges(node)).immutableCopy();
  }

  @Override
  public Set<N> incidentNodes(Object edge) {
    checkNotNull(edge, "edge");
    IncidentNodes<N> endpoints = edgeToIncidentNodes.get(edge);
    checkArgument(endpoints != null, EDGE_NOT_IN_GRAPH, edge);
    return endpoints.asImmutableSet();
  }

  @Override
  public Set<N> adjacentNodes(Object node) {
    return Sets.union(predecessors(node), successors(node)).immutableCopy();
  }

  @Override
  public Set<E> adjacentEdges(Object edge) {
    checkNotNull(edge, "edge");
    IncidentNodes<N> endpoints = edgeToIncidentNodes.get(edge);
    checkArgument(endpoints != null, EDGE_NOT_IN_GRAPH, edge);
    Set<E> adjacentEdges = Sets.newLinkedHashSet();
    adjacentEdges.addAll(incidentEdges(endpoints.target()));
    adjacentEdges.addAll(incidentEdges(endpoints.source()));
    // Edges are not adjacent to themselves by definition.
    adjacentEdges.remove(edge);
    return Collections.unmodifiableSet(adjacentEdges);
  }

  /**
   * Returns the intersection of these two sets, using {@code Sets.intersection}:
   * <ol>
   * <li>Outgoing edges of {@code node1}.
   * <li>Incoming edges of {@code node2}.
   * </ol>
   * The first argument passed to {@code Sets.intersection} is the smaller of the two sets.
   *
   * @see Sets#intersection
   */
  @Override
  public Set<E> edgesConnecting(Object node1, Object node2) {
    checkNotNull(node1, "node1");
    checkNotNull(node2, "node2");
    Set<E> sourceOutEdges = outEdges(node1);
    Set<E> targetInEdges = inEdges(node2);
    return sourceOutEdges.size() <= targetInEdges.size()
        ? Sets.intersection(sourceOutEdges, targetInEdges).immutableCopy()
        : Sets.intersection(targetInEdges, sourceOutEdges).immutableCopy();
  }

  @Override
  public Set<E> inEdges(Object node) {
    checkNotNull(node, "node");
    IncidentEdges<E> incidentEdges = nodeToIncidentEdges.get(node);
    checkArgument(incidentEdges != null, NODE_NOT_IN_GRAPH, node);
    return incidentEdges.inEdges();
  }

  @Override
  public Set<E> outEdges(Object node) {
    checkNotNull(node, "node");
    IncidentEdges<E> incidentEdges = nodeToIncidentEdges.get(node);
    checkArgument(incidentEdges != null, NODE_NOT_IN_GRAPH, node);
    return incidentEdges.outEdges();
  }

  @Override
  public Set<N> predecessors(Object node) {
    ImmutableSet.Builder<N> predecessorsBuilder = ImmutableSet.builder();
    for (E edge : inEdges(node)) {
      predecessorsBuilder.add(source(edge));
    }
    return predecessorsBuilder.build();
  }

  @Override
  public Set<N> successors(Object node) {
    ImmutableSet.Builder<N> successorsBuilder = ImmutableSet.builder();
    for (E edge : outEdges(node)) {
      successorsBuilder.add(target(edge));
    }
    return successorsBuilder.build();
  }

  @Override
  public long degree(Object node) {
    return inDegree(node) + outDegree(node) - edgesConnecting(node, node).size();
  }

  @Override
  public long inDegree(Object node) {
    return inEdges(node).size();
  }

  @Override
  public long outDegree(Object node) {
    return outEdges(node).size();
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

  @Override
  public boolean equals(@Nullable Object object) {
    return (object instanceof DirectedGraph) && Graphs.equal(this, (DirectedGraph) object);
  }

  @Override
  public int hashCode() {
    // This map encapsulates all of the structural relationships of this graph, so its hash code
    // is consistent with the above definition of equals().
    return nodeToIncidentEdges.hashCode();
  }

  /**
   * Returns a new builder. The generated builder is equivalent to the builder
   * created by the {@code Builder} constructor.
   */
  @CheckReturnValue
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
  @CheckReturnValue
  public static <N, E> Builder<N, E> builder(GraphConfig config) {
    return new Builder<N, E>(config);
  }

  /**
   * Returns an immutable copy of the input graph.
   */
  @CheckReturnValue
  public static <N, E> ImmutableDirectedGraph<N, E> copyOf(DirectedGraph<N, E> graph) {
    return new Builder<N, E>(graph).build();
  }

  @Override
  public String toString() {
    return String.format("config: %s, nodes: %s, edges: %s",
        config,
        nodeToIncidentEdges.keySet(),
        edgeToIncidentNodes);
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
    public Builder<N, E> addNode(N node) {
      directedGraph.addNode(node);
      return this;
    }

    @Override
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
     * @see Graph#addEdge(e, n1, n2)
     */
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
    @CheckReturnValue
    public ImmutableDirectedGraph<N, E> build() {
      return new ImmutableDirectedGraph<N, E>(this);
    }
  }
}
