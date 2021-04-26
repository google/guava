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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;

/**
 * A {@link ValueGraph} whose elements and structural relationships will never change. Instances of
 * this class may be obtained with {@link #copyOf(ValueGraph)}.
 *
 * <p>See the Guava User's Guide's <a
 * href="https://github.com/google/guava/wiki/GraphsExplained#immutable-implementations">discussion
 * of the {@code Immutable*} types</a> for more information on the properties and guarantees
 * provided by this class.
 *
 * @author James Sexton
 * @author Jens Nyman
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
 */
@Beta
@Immutable(containerOf = {"N", "V"})
@SuppressWarnings("Immutable") // Extends StandardValueGraph but uses ImmutableMaps.
@ElementTypesAreNonnullByDefault
public final class ImmutableValueGraph<N, V> extends StandardValueGraph<N, V> {

  private ImmutableValueGraph(ValueGraph<N, V> graph) {
    super(ValueGraphBuilder.from(graph), getNodeConnections(graph), graph.edges().size());
  }

  /** Returns an immutable copy of {@code graph}. */
  public static <N, V> ImmutableValueGraph<N, V> copyOf(ValueGraph<N, V> graph) {
    return (graph instanceof ImmutableValueGraph)
        ? (ImmutableValueGraph<N, V>) graph
        : new ImmutableValueGraph<N, V>(graph);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   */
  @Deprecated
  public static <N, V> ImmutableValueGraph<N, V> copyOf(ImmutableValueGraph<N, V> graph) {
    return checkNotNull(graph);
  }

  @Override
  public ElementOrder<N> incidentEdgeOrder() {
    return ElementOrder.stable();
  }

  @Override
  public ImmutableGraph<N> asGraph() {
    return new ImmutableGraph<N>(this); // safe because the view is effectively immutable
  }

  private static <N, V> ImmutableMap<N, GraphConnections<N, V>> getNodeConnections(
      ValueGraph<N, V> graph) {
    // ImmutableMap.Builder maintains the order of the elements as inserted, so the map will have
    // whatever ordering the graph's nodes do, so ImmutableSortedMap is unnecessary even if the
    // input nodes are sorted.
    ImmutableMap.Builder<N, GraphConnections<N, V>> nodeConnections = ImmutableMap.builder();
    for (N node : graph.nodes()) {
      nodeConnections.put(node, connectionsOf(graph, node));
    }
    return nodeConnections.build();
  }

  private static <N, V> GraphConnections<N, V> connectionsOf(
      final ValueGraph<N, V> graph, final N node) {
    Function<N, V> successorNodeToValueFn =
        new Function<N, V>() {
          @Override
          public V apply(N successorNode) {
            // requireNonNull is safe because the endpoint pair comes from the graph.
            return requireNonNull(graph.edgeValueOrDefault(node, successorNode, null));
          }
        };
    return graph.isDirected()
        ? DirectedGraphConnections.ofImmutable(
            node, graph.incidentEdges(node), successorNodeToValueFn)
        : UndirectedGraphConnections.ofImmutable(
            Maps.asMap(graph.adjacentNodes(node), successorNodeToValueFn));
  }

  /**
   * A builder for creating {@link ImmutableValueGraph} instances, especially {@code static final}
   * graphs. Example:
   *
   * <pre>{@code
   * static final ImmutableValueGraph<City, Distance> CITY_ROAD_DISTANCE_GRAPH =
   *     ValueGraphBuilder.undirected()
   *         .<City, Distance>immutable()
   *         .putEdgeValue(PARIS, BERLIN, kilometers(1060))
   *         .putEdgeValue(PARIS, BRUSSELS, kilometers(317))
   *         .putEdgeValue(BERLIN, BRUSSELS, kilometers(764))
   *         .addNode(REYKJAVIK)
   *         .build();
   * }</pre>
   *
   * <p>Builder instances can be reused; it is safe to call {@link #build} multiple times to build
   * multiple graphs in series. Each new graph contains all the elements of the ones created before
   * it.
   *
   * @since 28.0
   */
  public static class Builder<N, V> {

    private final MutableValueGraph<N, V> mutableValueGraph;

    Builder(ValueGraphBuilder<N, V> graphBuilder) {
      // The incidentEdgeOrder for immutable graphs is always stable. However, we don't want to
      // modify this builder, so we make a copy instead.
      this.mutableValueGraph =
          graphBuilder.copy().incidentEdgeOrder(ElementOrder.<N>stable()).build();
    }

    /**
     * Adds {@code node} if it is not already present.
     *
     * <p><b>Nodes must be unique</b>, just as {@code Map} keys must be. They must also be non-null.
     *
     * @return this {@code Builder} object
     */
    @CanIgnoreReturnValue
    public ImmutableValueGraph.Builder<N, V> addNode(N node) {
      mutableValueGraph.addNode(node);
      return this;
    }

    /**
     * Adds an edge connecting {@code nodeU} to {@code nodeV} if one is not already present, and
     * sets a value for that edge to {@code value} (overwriting the existing value, if any).
     *
     * <p>If the graph is directed, the resultant edge will be directed; otherwise, it will be
     * undirected.
     *
     * <p>Values do not have to be unique. However, values must be non-null.
     *
     * <p>If {@code nodeU} and {@code nodeV} are not already present in this graph, this method will
     * silently {@link #addNode(Object) add} {@code nodeU} and {@code nodeV} to the graph.
     *
     * @return this {@code Builder} object
     * @throws IllegalArgumentException if the introduction of the edge would violate {@link
     *     #allowsSelfLoops()}
     */
    @CanIgnoreReturnValue
    public ImmutableValueGraph.Builder<N, V> putEdgeValue(N nodeU, N nodeV, V value) {
      mutableValueGraph.putEdgeValue(nodeU, nodeV, value);
      return this;
    }

    /**
     * Adds an edge connecting {@code endpoints} if one is not already present, and sets a value for
     * that edge to {@code value} (overwriting the existing value, if any).
     *
     * <p>If the graph is directed, the resultant edge will be directed; otherwise, it will be
     * undirected.
     *
     * <p>If this graph is directed, {@code endpoints} must be ordered.
     *
     * <p>Values do not have to be unique. However, values must be non-null.
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
    public ImmutableValueGraph.Builder<N, V> putEdgeValue(EndpointPair<N> endpoints, V value) {
      mutableValueGraph.putEdgeValue(endpoints, value);
      return this;
    }

    /**
     * Returns a newly-created {@code ImmutableValueGraph} based on the contents of this {@code
     * Builder}.
     */
    public ImmutableValueGraph<N, V> build() {
      return ImmutableValueGraph.copyOf(mutableValueGraph);
    }
  }
}
