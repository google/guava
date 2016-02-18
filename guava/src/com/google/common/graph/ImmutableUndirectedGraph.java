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

import com.google.common.annotations.Beta;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * Implementation of an immutable undirected graph consisting of nodes of type N
 * and edges of type E.
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
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @see AbstractConfigurableGraph
 * @see AbstractImmutableGraph
 * @since 20.0
 */
@Beta
public final class ImmutableUndirectedGraph<N, E> extends AbstractImmutableGraph<N, E>
    implements UndirectedGraph<N, E> {

  private ImmutableUndirectedGraph(Builder<N, E> builder) {
    super(builder);
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
  public static final class Builder<N, E> extends AbstractImmutableGraph.Builder<N, E> {

    /**
     * Creates a new builder with the default graph configuration.
     */
    public Builder() {
      super(Graphs.<N, E>createUndirected());
    }

    /**
     * Creates a new builder with the specified configuration.
     */
    public Builder(GraphConfig config) {
      super(Graphs.<N, E>createUndirected(config));
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
    private Builder(UndirectedGraph<N, E> graph) {
      super(graph);
    }

    @Override
    NodeConnections<N, E> nodeConnectionsOf(N node) {
      return UndirectedNodeConnections.ofImmutable(
          graph.adjacentNodes(node), graph.incidentEdges(node));
    }

    @Override
    public ImmutableUndirectedGraph<N, E> build() {
      return new ImmutableUndirectedGraph<N, E>(this);
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<N, E> addNode(N node) {
      return (Builder<N, E>) super.addNode(node); // Refine the return type
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<N, E> addEdge(E edge, N node1, N node2) {
      return (Builder<N, E>) super.addEdge(edge, node1, node2); // Refine the return type
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<N, E> addGraph(Graph<N, E> graphToAdd) {
      return (Builder<N, E>) super.addGraph(graphToAdd); // Refine the return type
    }
  }
}
