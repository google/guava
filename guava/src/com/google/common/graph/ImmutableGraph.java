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
import static com.google.common.graph.Graphs.checkNonNegative;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * A {@link Graph} whose elements and structural relationships will never change. Instances of this
 * class may be obtained with {@link #copyOf(Graph)}.
 *
 * <p>This class generally provides all of the same guarantees as {@link ImmutableCollection}
 * (despite not extending {@link ImmutableCollection} itself), including guaranteed thread-safety.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
 */
@Beta
public class ImmutableGraph<N, V> extends ConfigurableGraph<N, V> {
  private final long edgeCount;

  /** To ensure the immutability contract is maintained, there must be no public constructors. */
  ImmutableGraph(Graph<N, V> graph) {
    super(GraphBuilder.from(graph), getNodeConnections(graph));
    this.edgeCount = checkNonNegative(graph.edges().size());
  }

  /** Returns an immutable copy of {@code graph}. */
  public static <N, V> ImmutableGraph<N, V> copyOf(Graph<N, V> graph) {
    return (graph instanceof ImmutableGraph)
        ? (ImmutableGraph<N, V>) graph
        : new ImmutableGraph<N, V>(graph);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   */
  @Deprecated
  public static <N, V> ImmutableGraph<N, V> copyOf(ImmutableGraph<N, V> graph) {
    return checkNotNull(graph);
  }

  private static <N, V> ImmutableMap<N, GraphConnections<N, V>> getNodeConnections(
      Graph<N, V> graph) {
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
      final Graph<N, V> graph, final N node) {
    Function<N, V> successorNodeToValueFn =
        new Function<N, V>() {
          @Override
          public V apply(N successorNode) {
            return graph.edgeValue(node, successorNode);
          }
        };
    return graph.isDirected()
        ? DirectedGraphConnections.ofImmutable(
            graph.predecessors(node), Maps.asMap(graph.successors(node), successorNodeToValueFn))
        : UndirectedGraphConnections.ofImmutable(
            Maps.asMap(graph.adjacentNodes(node), successorNodeToValueFn));
  }

  @Override
  protected long edgeCount() {
    return edgeCount;
  }
}
