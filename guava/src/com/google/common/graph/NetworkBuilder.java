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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import java.util.Comparator;

/**
 * A builder for constructing instances of {@link Network} with user-defined properties.
 *
 * <p>A graph built by this class will have the following properties by default:
 * <ul>
 * <li>does not allow parallel edges
 * <li>allows self-loops
 * </ul>
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @since 20.0
 */
// TODO(b/24620028): Add support for sorted nodes/edges. Use the same pattern as CacheBuilder
// to narrow the generic <N, E> type when Comparators are provided.
// TODO(user): try creating an abstract superclass that this and GraphBuilder could derive from.
@Beta
public final class NetworkBuilder<N, E> {
  final boolean directed;
  boolean allowsParallelEdges = false;
  boolean allowsSelfLoops = true;
  Comparator<N> nodeComparator = null;
  Comparator<E> edgeComparator = null;
  Optional<Integer> expectedNodeCount = Optional.absent();
  Optional<Integer> expectedEdgeCount = Optional.absent();

  /**
   * Creates a new instance with the specified edge directionality.
   *
   * @param directed if true, creates an instance for graphs whose edges are each directed;
   *      if false, creates an instance for graphs whose edges are each undirected.
   */
  private NetworkBuilder(boolean directed) {
    this.directed = directed;
  }

  /**
   * Returns a {@link NetworkBuilder} for building directed graphs.
   */
  public static NetworkBuilder<Object, Object> directed() {
    return new NetworkBuilder<Object, Object>(true);
  }

  /**
   * Returns a {@link NetworkBuilder} for building undirected graphs.
   */
  public static NetworkBuilder<Object, Object> undirected() {
    return new NetworkBuilder<Object, Object>(false);
  }

  /**
   * Returns a {@link NetworkBuilder} initialized with all properties queryable from {@code graph}.
   *
   * <p>The "queryable" properties are those that are exposed through the {@link Network} interface,
   * such as {@link Network#isDirected()}. Other properties, such as
   * {@link #expectedNodeCount(int)}, are not set in the new builder.
   */
  public static <N, E> NetworkBuilder<N, E> from(Network<N, E> graph) {
    return new NetworkBuilder<N, E>(graph.isDirected())
        .allowsParallelEdges(graph.allowsParallelEdges())
        .allowsSelfLoops(graph.allowsSelfLoops());
  }

  /**
   * Specifies whether the graph will allow parallel edges. Attempting to add a parallel edge to
   * a graph that does not allow them will throw an {@link UnsupportedOperationException}.
   */
  public NetworkBuilder<N, E> allowsParallelEdges(boolean allowsParallelEdges) {
    this.allowsParallelEdges = allowsParallelEdges;
    return this;
  }

  /**
   * Specifies whether the graph will allow self-loops (edges that connect a node to itself).
   * Attempting to add a self-loop to a graph that does not allow them will throw an
   * {@link UnsupportedOperationException}.
   */
  public NetworkBuilder<N, E> allowsSelfLoops(boolean allowsSelfLoops) {
    this.allowsSelfLoops = allowsSelfLoops;
    return this;
  }

  /**
   * Specifies the expected number of nodes in the graph.
   *
   * @throws IllegalArgumentException if {@code expectedNodeCount} is negative
   */
  public NetworkBuilder<N, E> expectedNodeCount(int expectedNodeCount) {
    checkArgument(expectedNodeCount >= 0, "The expected number of nodes can't be negative: %s",
        expectedNodeCount);
    this.expectedNodeCount = Optional.of(expectedNodeCount);
    return this;
  }

  /**
   * Specifies the expected number of edges in the graph.
   *
   * @throws IllegalArgumentException if {@code expectedEdgeCount} is negative
   */
  public NetworkBuilder<N, E> expectedEdgeCount(int expectedEdgeCount) {
    checkArgument(expectedEdgeCount >= 0, "The expected number of edges can't be negative: %s",
        expectedEdgeCount);
    this.expectedEdgeCount = Optional.of(expectedEdgeCount);
    return this;
  }

  /**
   * Returns an empty {@link MutableNetwork} with the properties of this {@link NetworkBuilder}.
   */
  public <N1 extends N, E1 extends E> MutableNetwork<N1, E1> build() {
    return new ConfigurableMutableNetwork<N1, E1>(this);
  }
}
