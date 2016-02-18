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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Set;

/**
 * Abstract base class for implementation of immutable graphs.
 *
 * <p>All mutation methods throw {@link UnsupportedOperationException} as the graph
 * can't be modified.
 *
 * <p>The time complexity of {@code edgesConnecting(node1, node2)} is O(min(outD_node1, inD_node2)).
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
//TODO(user): Add support for sorted nodes/edges and/or hypergraphs.
abstract class AbstractImmutableGraph<N, E> extends AbstractConfigurableGraph<N, E> {

  AbstractImmutableGraph(Builder<N, E> builder) {
    super(builder.graph.config(), builder.getNodeConnections(), builder.getEdgeToIncidentNodes());
  }

  @Override
  final NodeConnections<N, E> newNodeConnections() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean addNode(N node) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean addEdge(E edge, N node1, N node2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean removeNode(Object node) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean removeEdge(Object edge) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<E> edgesConnecting(Object node1, Object node2) {
    // This set is calculated as the intersection of two sets, and is likely to be small.
    // As an optimization, copy it to an ImmutableSet so re-iterating is fast.
    return ImmutableSet.copyOf(super.edgesConnecting(node1, node2));
  }

  /**
   * An abstract class for builders of immutable graph instances.
   *
   * @param <N> Node parameter type
   * @param <E> Edge parameter type
   */
  abstract static class Builder<N, E> {

    final Graph<N, E> graph;

    Builder(Graph<N, E> graph) {
      this.graph = checkNotNull(graph, "graph");
    }

    abstract NodeConnections<N, E> nodeConnectionsOf(N node);

    /**
     * Creates and returns a new instance of {@code AbstractImmutableGraph}
     * based on the contents of the {@code Builder}.
     */
    public abstract AbstractImmutableGraph<N, E> build();

    /**
     * Adds {@code node} to the graph being built.
     *
     * @return this {@code Builder} instance
     * @throws NullPointerException if {@code node} is null
     */
    @CanIgnoreReturnValue
    public Builder<N, E> addNode(N node) {
      graph.addNode(node);
      return this;
    }

    /**
     * Adds {@code edge} to the graph being built, connecting {@code node1} and {@code node2};
     * adds {@code node1} and {@code node2} if not already present.
     *
     * @return this {@code Builder} instance
     * @throws IllegalArgumentException when {@code Graph.addEdge(edge, node1, node2)} throws
     *     on the graph being built
     * @throws NullPointerException if {@code edge}, {@code node1}, or {@code node2} is null
     * @see Graph#addEdge
     */
    @CanIgnoreReturnValue
    public Builder<N, E> addEdge(E edge, N node1, N node2) {
      graph.addEdge(edge, node1, node2);
      return this;
    }

    /**
     * Adds all elements of {@code graph} to the graph being built.
     *
     * @throws IllegalArgumentException under either of two conditions:
     *     (1) the {@code GraphConfig} objects held by the graph being built and by {@code graph}
     *     are not compatible
     *     (2) calling {@link Graphs#addEdge} on the graph being built throws IAE
     * @see Graph#addEdge
     */
    @CanIgnoreReturnValue
    public Builder<N, E> addGraph(Graph<N, E> graphToAdd) {
      checkArgument(
          graph.config().compatibleWith(graphToAdd.config()),
          "GraphConfigs for input and for graph being built are not compatible: input: %s, "
              + "this graph: %s",
          graphToAdd.config(),
          graph.config());

      for (N node : graphToAdd.nodes()) {
        graph.addNode(node);
      }
      for (E edge : graphToAdd.edges()) {
        Graphs.addEdge(graph, edge, graphToAdd.incidentNodes(edge));
      }
      return this;
    }

    private ImmutableMap<N, NodeConnections<N, E>> getNodeConnections() {
      ImmutableMap.Builder<N, NodeConnections<N, E>> nodeConnections = ImmutableMap.builder();
      for (N node : graph.nodes()) {
        nodeConnections.put(node, nodeConnectionsOf(node));
      }
      return nodeConnections.build();
    }

    private ImmutableMap<E, IncidentNodes<N>> getEdgeToIncidentNodes() {
      ImmutableMap.Builder<E, IncidentNodes<N>> edgeToIncidentNodes = ImmutableMap.builder();
      for (E edge : graph.edges()) {
        edgeToIncidentNodes.put(edge, IncidentNodes.of(graph.incidentNodes(edge)));
      }
      return edgeToIncidentNodes.build();
    }
  }
}
