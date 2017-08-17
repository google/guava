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
import com.google.common.base.Optional;

/**
 * A builder for constructing instances of {@link MutableNetwork} with user-defined properties.
 *
 * <p>A network built by this class will have the following properties by default:
 *
 * <ul>
 *   <li>does not allow parallel edges
 *   <li>does not allow self-loops
 *   <li>orders {@link Network#nodes()} and {@link Network#edges()} in the order in which the
 *       elements were added
 * </ul>
 *
 * <p>Example of use:
 *
 * <pre>{@code
 * MutableNetwork<String, Integer> flightNetwork =
 *     NetworkBuilder.directed().allowsParallelEdges(true).build();
 * flightNetwork.addEdge("LAX", "ATL", 3025);
 * flightNetwork.addEdge("LAX", "ATL", 1598);
 * flightNetwork.addEdge("ATL", "LAX", 2450);
 * }</pre>
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @since 20.0
 */
@Beta
public final class NetworkBuilder<N, E> extends AbstractGraphBuilder<N> {
  boolean allowsParallelEdges = false;
  ElementOrder<? super E> edgeOrder = ElementOrder.insertion();
  Optional<Integer> expectedEdgeCount = Optional.absent();

  /** Creates a new instance with the specified edge directionality. */
  private NetworkBuilder(boolean directed) {
    super(directed);
  }

  /** Returns a {@link NetworkBuilder} for building directed networks. */
  public static NetworkBuilder<Object, Object> directed() {
    return new NetworkBuilder<>(true);
  }

  /** Returns a {@link NetworkBuilder} for building undirected networks. */
  public static NetworkBuilder<Object, Object> undirected() {
    return new NetworkBuilder<>(false);
  }

  /**
   * Returns a {@link NetworkBuilder} initialized with all properties queryable from {@code
   * network}.
   *
   * <p>The "queryable" properties are those that are exposed through the {@link Network} interface,
   * such as {@link Network#isDirected()}. Other properties, such as {@link
   * #expectedNodeCount(int)}, are not set in the new builder.
   */
  public static <N, E> NetworkBuilder<N, E> from(Network<N, E> network) {
    return new NetworkBuilder<N, E>(network.isDirected())
        .allowsParallelEdges(network.allowsParallelEdges())
        .allowsSelfLoops(network.allowsSelfLoops())
        .nodeOrder(network.nodeOrder())
        .edgeOrder(network.edgeOrder());
  }

  /**
   * Specifies whether the network will allow parallel edges. Attempting to add a parallel edge to a
   * network that does not allow them will throw an {@link UnsupportedOperationException}.
   */
  public NetworkBuilder<N, E> allowsParallelEdges(boolean allowsParallelEdges) {
    this.allowsParallelEdges = allowsParallelEdges;
    return this;
  }

  /**
   * Specifies whether the network will allow self-loops (edges that connect a node to itself).
   * Attempting to add a self-loop to a network that does not allow them will throw an {@link
   * UnsupportedOperationException}.
   */
  public NetworkBuilder<N, E> allowsSelfLoops(boolean allowsSelfLoops) {
    this.allowsSelfLoops = allowsSelfLoops;
    return this;
  }

  /**
   * Specifies the expected number of nodes in the network.
   *
   * @throws IllegalArgumentException if {@code expectedNodeCount} is negative
   */
  public NetworkBuilder<N, E> expectedNodeCount(int expectedNodeCount) {
    this.expectedNodeCount = Optional.of(checkNonNegative(expectedNodeCount));
    return this;
  }

  /**
   * Specifies the expected number of edges in the network.
   *
   * @throws IllegalArgumentException if {@code expectedEdgeCount} is negative
   */
  public NetworkBuilder<N, E> expectedEdgeCount(int expectedEdgeCount) {
    this.expectedEdgeCount = Optional.of(checkNonNegative(expectedEdgeCount));
    return this;
  }

  /** Specifies the order of iteration for the elements of {@link Network#nodes()}. */
  public <N1 extends N> NetworkBuilder<N1, E> nodeOrder(ElementOrder<N1> nodeOrder) {
    NetworkBuilder<N1, E> newBuilder = cast();
    newBuilder.nodeOrder = checkNotNull(nodeOrder);
    return newBuilder;
  }

  /** Specifies the order of iteration for the elements of {@link Network#edges()}. */
  public <E1 extends E> NetworkBuilder<N, E1> edgeOrder(ElementOrder<E1> edgeOrder) {
    NetworkBuilder<N, E1> newBuilder = cast();
    newBuilder.edgeOrder = checkNotNull(edgeOrder);
    return newBuilder;
  }

  /** Returns an empty {@link MutableNetwork} with the properties of this {@link NetworkBuilder}. */
  public <N1 extends N, E1 extends E> MutableNetwork<N1, E1> build() {
    return new ConfigurableMutableNetwork<>(this);
  }

  @SuppressWarnings("unchecked")
  private <N1 extends N, E1 extends E> NetworkBuilder<N1, E1> cast() {
    return (NetworkBuilder<N1, E1>) this;
  }
}
