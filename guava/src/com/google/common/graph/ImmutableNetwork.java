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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * A {@link Network} whose relationships are constant. Instances of this class may be obtained
 * with {@link #copyOf(Network)}.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
@Beta
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
  public Set<E> edgesConnecting(Object nodeA, Object nodeB) {
    // This set is calculated as the intersection of two sets, and is likely to be small.
    // As an optimization, copy it to an ImmutableSet so re-iterating is fast.
    return ImmutableSet.copyOf(super.edgesConnecting(nodeA, nodeB));
  }

  private static <N, E> Map<N, NodeConnections<N, E>> getNodeConnections(Network<N, E> graph) {
    // ImmutableMap.Builder maintains the order of the elements as inserted, so the map will
    // have whatever ordering the graph's nodes do, so ImmutableSortedMap is unnecessary even if the
    // input nodes are sorted.
    ImmutableMap.Builder<N, NodeConnections<N, E>> nodeConnections = ImmutableMap.builder();
    for (N node : graph.nodes()) {
      nodeConnections.put(node, nodeConnectionsOf(graph, node));
    }
    return nodeConnections.build();
  }

  private static <N, E> Map<E, N> getEdgeToReferenceNode(Network<N, E> graph) {
    // ImmutableMap.Builder maintains the order of the elements as inserted, so the map will
    // have whatever ordering the graph's edges do, so ImmutableSortedMap is unnecessary even if the
    // input edges are sorted.
    ImmutableMap.Builder<E, N> edgeToReferenceNode = ImmutableMap.builder();
    for (E edge : graph.edges()) {
      edgeToReferenceNode.put(edge, graph.incidentNodes(edge).nodeA());
    }
    return edgeToReferenceNode.build();
  }

  private static <N, E> NodeConnections<N, E> nodeConnectionsOf(Network<N, E> graph, N node) {
    if (graph.isDirected()) {
      Map<E, N> inEdgeMap = Maps.asMap(graph.inEdges(node), sourceNodeFn(graph));
      Map<E, N> outEdgeMap = Maps.asMap(graph.outEdges(node), targetNodeFn(graph));
      int selfLoopCount = graph.allowsSelfLoops()
          ? Sets.intersection(inEdgeMap.keySet(), outEdgeMap.keySet()).size()
          : 0;
      return graph.allowsParallelEdges()
           ? DirectedMultiNodeConnections.ofImmutable(inEdgeMap, outEdgeMap, selfLoopCount)
           : DirectedNodeConnections.ofImmutable(inEdgeMap, outEdgeMap, selfLoopCount);
    } else {
      Map<E, N> incidentEdgeMap = Maps.asMap(graph.incidentEdges(node), otherNodeFn(graph, node));
      return graph.allowsParallelEdges()
          ? UndirectedMultiNodeConnections.ofImmutable(incidentEdgeMap)
          : UndirectedNodeConnections.ofImmutable(incidentEdgeMap);
    }
  }

  private static <N, E> Function<E, N> sourceNodeFn(final Network<N, E> graph) {
    return new Function<E, N>() {
      @Override
      public N apply(E edge) {
        return graph.incidentNodes(edge).source();
      }
    };
  }

  private static <N, E> Function<E, N> targetNodeFn(final Network<N, E> graph) {
    return new Function<E, N>() {
      @Override
      public N apply(E edge) {
        return graph.incidentNodes(edge).target();
      }
    };
  }

  private static <N, E> Function<E, N> otherNodeFn(final Network<N, E> graph, final N node) {
    return new Function<E, N>() {
      @Override
      public N apply(E edge) {
        return graph.incidentNodes(edge).otherNode(node);
      }
    };
  }
}
