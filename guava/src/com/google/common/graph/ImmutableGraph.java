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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.graph.GraphConstants.Presence;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;

/**
 * A {@link Graph} whose elements and structural relationships will never change. Instances of this
 * class may be obtained with {@link #copyOf(Graph)}.
 *
 * <p>See the Guava User's Guide's <a
 * href="https://github.com/google/guava/wiki/GraphsExplained#immutable-implementations">discussion
 * of the {@code Immutable*} types</a> for more information on the properties and guarantees
 * provided by this class.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @author Jens Nyman
 * @param <N> Node parameter type
 * @since 20.0
 */
@Beta
@Immutable(containerOf = {"N"})
public class ImmutableGraph<N> extends ForwardingGraph<N> {
  @SuppressWarnings("Immutable") // The backing graph must be immutable.
  private final BaseGraph<N> backingGraph;

  ImmutableGraph(BaseGraph<N> backingGraph) {
    this.backingGraph = backingGraph;
  }

  /** Returns an immutable copy of {@code graph}. */
  public static <N> ImmutableGraph<N> copyOf(Graph<N> graph) {
    return (graph instanceof ImmutableGraph)
        ? (ImmutableGraph<N>) graph
        : new ImmutableGraph<N>(
            new ConfigurableValueGraph<N, Presence>(
                GraphBuilder.from(graph), getNodeConnections(graph), graph.edges().size()));
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   */
  @Deprecated
  public static <N> ImmutableGraph<N> copyOf(ImmutableGraph<N> graph) {
    return checkNotNull(graph);
  }

  @Override
  public ElementOrder<N> incidentEdgeOrder() {
    return ElementOrder.stable();
  }

  private static <N> ImmutableMap<N, GraphConnections<N, Presence>> getNodeConnections(
      Graph<N> graph) {
    // ImmutableMap.Builder maintains the order of the elements as inserted, so the map will have
    // whatever ordering the graph's nodes do, so ImmutableSortedMap is unnecessary even if the
    // input nodes are sorted.
    ImmutableMap.Builder<N, GraphConnections<N, Presence>> nodeConnections = ImmutableMap.builder();
    for (N node : graph.nodes()) {
      nodeConnections.put(node, connectionsOf(graph, node));
    }
    return nodeConnections.build();
  }

  @SuppressWarnings("unchecked")
  private static <N> GraphConnections<N, Presence> connectionsOf(Graph<N> graph, N node) {
    Function<N, Presence> edgeValueFn =
        (Function<N, Presence>) Functions.constant(Presence.EDGE_EXISTS);
    return graph.isDirected()
        ? DirectedGraphConnections.ofImmutable(node, graph.incidentEdges(node), edgeValueFn)
        : UndirectedGraphConnections.ofImmutable(
            Maps.asMap(graph.adjacentNodes(node), edgeValueFn));
  }

  @Override
  protected BaseGraph<N> delegate() {
    return backingGraph;
  }

  /**
   * A builder for creating {@link ImmutableGraph} instances, especially {@code static final}
   * graphs. Example:
   *
   * <pre>{@code
   * static final ImmutableGraph<Country> COUNTRY_ADJACENCY_GRAPH =
   *     GraphBuilder.undirected()
   *         .<Country>immutable()
   *         .putEdge(FRANCE, GERMANY)
   *         .putEdge(FRANCE, BELGIUM)
   *         .putEdge(GERMANY, BELGIUM)
   *         .addNode(ICELAND)
   *         .build();
   * }</pre>
   *
   * <p>Builder instances can be reused; it is safe to call {@link #build} multiple times to build
   * multiple graphs in series. Each new graph contains all the elements of the ones created before
   * it.
   *
   * @since 28.0
   */
  public static class Builder<N> {

    private final MutableGraph<N> mutableGraph;

    Builder(GraphBuilder<N> graphBuilder) {
      // The incidentEdgeOrder for immutable graphs is always stable. However, we don't want to
      // modify this builder, so we make a copy instead.
      this.mutableGraph = graphBuilder.copy().incidentEdgeOrder(ElementOrder.<N>stable()).build();
    }

    /**
     * Adds {@code node} if it is not already present.
     *
     * <p><b>Nodes must be unique</b>, just as {@code Map} keys must be. They must also be non-null.
     *
     * @return this {@code Builder} object
     */
    @CanIgnoreReturnValue
    public Builder<N> addNode(N node) {
      mutableGraph.addNode(node);
      return this;
    }

    /**
     * Adds an edge connecting {@code nodeU} to {@code nodeV} if one is not already present.
     *
     * <p>If the graph is directed, the resultant edge will be directed; otherwise, it will be
     * undirected.
     *
     * <p>If {@code nodeU} and {@code nodeV} are not already present in this graph, this method will
     * silently {@link #addNode(Object) add} {@code nodeU} and {@code nodeV} to the graph.
     *
     * @return this {@code Builder} object
     * @throws IllegalArgumentException if the introduction of the edge would violate {@link
     *     #allowsSelfLoops()}
     */
    @CanIgnoreReturnValue
    public Builder<N> putEdge(N nodeU, N nodeV) {
      mutableGraph.putEdge(nodeU, nodeV);
      return this;
    }

    /**
     * Adds an edge connecting {@code endpoints} (in the order, if any, specified by {@code
     * endpoints}) if one is not already present.
     *
     * <p>If this graph is directed, {@code endpoints} must be ordered and the added edge will be
     * directed; if it is undirected, the added edge will be undirected.
     *
     * <p>If this graph is directed, {@code endpoints} must be ordered.
     *
     * <p>If either or both endpoints are not already present in this graph, this method will
     * silently {@link #addNode(Object) add} each missing endpoint to the graph.
     *
     * @return this {@code Builder} object
     * @throws IllegalArgumentException if the introduction of the edge would violate {@link
     *     #allowsSelfLoops()}
     * @throws IllegalArgumentException if the endpoints are unordered and the graph is directed
     */
    @CanIgnoreReturnValue
    public Builder<N> putEdge(EndpointPair<N> endpoints) {
      mutableGraph.putEdge(endpoints);
      return this;
    }

    /**
     * Returns a newly-created {@code ImmutableGraph} based on the contents of this {@code Builder}.
     */
    public ImmutableGraph<N> build() {
      return ImmutableGraph.copyOf(mutableGraph);
    }
  }
}
