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
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.Immutable;
import java.util.Map;

/**
 * A {@link Network} whose elements and structural relationships will never change. Instances of
 * this class may be obtained with {@link #copyOf(Network)}.
 *
 * <p>See the Guava User's Guide's <a
 * href="https://github.com/google/guava/wiki/GraphsExplained#immutable-implementations">discussion
 * of the {@code Immutable*} types</a> for more information on the properties and guarantees
 * provided by this class.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
@Beta
@Immutable(containerOf = {"N", "E"})
@SuppressWarnings("Immutable") // Extends ConfigurableNetwork but uses ImmutableMaps.
public final class ImmutableNetwork<N, E> extends ConfigurableNetwork<N, E> {

  private ImmutableNetwork(Network<N, E> network) {
    super(
        NetworkBuilder.from(network), getNodeConnections(network), getEdgeToReferenceNode(network));
  }

  /** Returns an immutable copy of {@code network}. */
  public static <N, E> ImmutableNetwork<N, E> copyOf(Network<N, E> network) {
    return (network instanceof ImmutableNetwork)
        ? (ImmutableNetwork<N, E>) network
        : new ImmutableNetwork<N, E>(network);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   */
  @Deprecated
  public static <N, E> ImmutableNetwork<N, E> copyOf(ImmutableNetwork<N, E> network) {
    return checkNotNull(network);
  }

  @Override
  public ImmutableGraph<N> asGraph() {
    return new ImmutableGraph<N>(super.asGraph()); // safe because the view is effectively immutable
  }

  private static <N, E> Map<N, NetworkConnections<N, E>> getNodeConnections(Network<N, E> network) {
    // ImmutableMap.Builder maintains the order of the elements as inserted, so the map will have
    // whatever ordering the network's nodes do, so ImmutableSortedMap is unnecessary even if the
    // input nodes are sorted.
    ImmutableMap.Builder<N, NetworkConnections<N, E>> nodeConnections = ImmutableMap.builder();
    for (N node : network.nodes()) {
      nodeConnections.put(node, connectionsOf(network, node));
    }
    return nodeConnections.build();
  }

  private static <N, E> Map<E, N> getEdgeToReferenceNode(Network<N, E> network) {
    // ImmutableMap.Builder maintains the order of the elements as inserted, so the map will have
    // whatever ordering the network's edges do, so ImmutableSortedMap is unnecessary even if the
    // input edges are sorted.
    ImmutableMap.Builder<E, N> edgeToReferenceNode = ImmutableMap.builder();
    for (E edge : network.edges()) {
      edgeToReferenceNode.put(edge, network.incidentNodes(edge).nodeU());
    }
    return edgeToReferenceNode.build();
  }

  private static <N, E> NetworkConnections<N, E> connectionsOf(Network<N, E> network, N node) {
    if (network.isDirected()) {
      Map<E, N> inEdgeMap = Maps.asMap(network.inEdges(node), sourceNodeFn(network));
      Map<E, N> outEdgeMap = Maps.asMap(network.outEdges(node), targetNodeFn(network));
      int selfLoopCount = network.edgesConnecting(node, node).size();
      return network.allowsParallelEdges()
          ? DirectedMultiNetworkConnections.ofImmutable(inEdgeMap, outEdgeMap, selfLoopCount)
          : DirectedNetworkConnections.ofImmutable(inEdgeMap, outEdgeMap, selfLoopCount);
    } else {
      Map<E, N> incidentEdgeMap =
          Maps.asMap(network.incidentEdges(node), adjacentNodeFn(network, node));
      return network.allowsParallelEdges()
          ? UndirectedMultiNetworkConnections.ofImmutable(incidentEdgeMap)
          : UndirectedNetworkConnections.ofImmutable(incidentEdgeMap);
    }
  }

  private static <N, E> Function<E, N> sourceNodeFn(final Network<N, E> network) {
    return new Function<E, N>() {
      @Override
      public N apply(E edge) {
        return network.incidentNodes(edge).source();
      }
    };
  }

  private static <N, E> Function<E, N> targetNodeFn(final Network<N, E> network) {
    return new Function<E, N>() {
      @Override
      public N apply(E edge) {
        return network.incidentNodes(edge).target();
      }
    };
  }

  private static <N, E> Function<E, N> adjacentNodeFn(final Network<N, E> network, final N node) {
    return new Function<E, N>() {
      @Override
      public N apply(E edge) {
        return network.incidentNodes(edge).adjacentNode(node);
      }
    };
  }
}
