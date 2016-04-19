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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

/**
 * A {@link Network} whose relationships are constant. Instances of this class may be obtained
 * with {@link #copyOf(Network)}.
 *
 * <p>The time complexity of {@code edgesConnecting(node1, node2)} is O(min(outD_node1, inD_node2)).
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
public final class ImmutableNetwork<N, E> extends AbstractConfigurableNetwork<N, E> {

  private ImmutableNetwork(Network<N, E> graph) {
    super(NetworkBuilder.from(graph), getNodeConnections(graph), getEdgeToReferenceNode(graph));
  }

  /**
   * Returns an immutable copy of {@code graph}.
   */
  public static <N, E> ImmutableNetwork<N, E> copyOf(Network<N, E> graph) {
    return (graph instanceof ImmutableNetwork)
        ? (ImmutableNetwork<N, E>) graph
        : new ImmutableNetwork<N, E>(graph);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   */
  @Deprecated
  public static <N, E> ImmutableNetwork<N, E> copyOf(ImmutableNetwork<N, E> graph) {
    return checkNotNull(graph);
  }

  @Override
  public Set<E> edgesConnecting(Object node1, Object node2) {
    // This set is calculated as the intersection of two sets, and is likely to be small.
    // As an optimization, copy it to an ImmutableSet so re-iterating is fast.
    return ImmutableSet.copyOf(super.edgesConnecting(node1, node2));
  }

  private static <N, E> Map<N, NodeConnections<N, E>> getNodeConnections(Network<N, E> graph) {
    ImmutableMap.Builder<N, NodeConnections<N, E>> nodeConnections = ImmutableMap.builder();
    for (N node : graph.nodes()) {
      nodeConnections.put(node, nodeConnectionsOf(graph, node));
    }
    return nodeConnections.build();
  }

  private static <N, E> Map<E, N> getEdgeToReferenceNode(Network<N, E> graph) {
    ImmutableMap.Builder<E, N> edgeToReferenceNode = ImmutableMap.builder();
    for (E edge : graph.edges()) {
      edgeToReferenceNode.put(edge, graph.incidentNodes(edge).iterator().next());
    }
    return edgeToReferenceNode.build();
  }

  private static <N, E> NodeConnections<N, E> nodeConnectionsOf(Network<N, E> graph, N node) {
    if (graph.isDirected()) {
      Map<E, N> inEdgeMap = Maps.asMap(graph.inEdges(node), sourceNodeFn(graph));
      Map<E, N> outEdgeMap = Maps.asMap(graph.outEdges(node), targetNodeFn(graph));
      return graph.allowsParallelEdges()
           ? DirectedMultiNodeConnections.ofImmutable(inEdgeMap, outEdgeMap)
           : DirectedNodeConnections.ofImmutable(inEdgeMap, outEdgeMap);
    } else {
      Map<E, N> incidentEdgeMap =
          Maps.asMap(graph.incidentEdges(node), oppositeNodeFn(graph, node));
      return graph.allowsParallelEdges()
          ? UndirectedMultiNodeConnections.ofImmutable(incidentEdgeMap)
          : UndirectedNodeConnections.ofImmutable(incidentEdgeMap);
    }
  }

  private static <N, E> Function<E, N> sourceNodeFn(final Network<N, E> graph) {
    return new Function<E, N>() {
      @Override
      public N apply(E edge) {
        return graph.source(edge);
      }
    };
  }

  private static <N, E> Function<E, N> targetNodeFn(final Network<N, E> graph) {
    return new Function<E, N>() {
      @Override
      public N apply(E edge) {
        return graph.target(edge);
      }
    };
  }

  private static <N, E> Function<E, N> oppositeNodeFn(final Network<N, E> graph, final N node) {
    return new Function<E, N>() {
      @Override
      public N apply(E edge) {
        return Graphs.oppositeNode(graph, edge, node);
      }
    };
  }
}
