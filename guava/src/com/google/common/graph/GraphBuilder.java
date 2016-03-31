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

import com.google.common.base.Optional;

import java.util.Comparator;

/**
 * A builder for constructing instances of {@link Graph} with user-defined properties.
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
public final class GraphBuilder<N, E> {
  Boolean directed = null; // No default value to enforce that this is set before building
  boolean allowsParallelEdges = false;
  boolean allowsSelfLoops = true;
  Comparator<N> nodeComparator = null;
  Comparator<E> edgeComparator = null;
  Optional<Integer> expectedNodeCount = Optional.absent();
  Optional<Integer> expectedEdgeCount = Optional.absent();

  private GraphBuilder() {}

  /**
   * Returns a {@link GraphBuilder} for building directed graphs.
   */
  public static GraphBuilder<Object, Object> directed() {
    return new GraphBuilder<Object, Object>().directed(true);
  }

  /**
   * Returns a {@link GraphBuilder} for building undirected graphs.
   */
  public static GraphBuilder<Object, Object> undirected() {
    return new GraphBuilder<Object, Object>().directed(false);
  }

  /**
   * Returns a {@link GraphBuilder} initialized with all properties queryable from {@code graph}.
   *
   * <p>The "queryable" properties are those that are exposed through the {@link Graph} interface,
   * such as {@link Graph#isDirected()}. Other properties, such as {@link #expectedNodeCount(int)},
   * are not set in the new builder.
   */
  public static <N, E> GraphBuilder<N, E> from(Graph<N, E> graph) {
    return new GraphBuilder<N, E>()
        .directed(graph.isDirected())
        .allowsParallelEdges(graph.allowsParallelEdges())
        .allowsSelfLoops(graph.allowsSelfLoops());
  }

  /**
   * This value should be set by {@link #directed()}, {@link #undirected()},
   * or {@link #from(Graph)}.
   */
  private GraphBuilder<N, E> directed(boolean directed) {
    this.directed = directed;
    return this;
  }

  /**
   * Specifies whether the graph will allow parallel edges. Attempting to add a parallel edge to
   * a graph that does not allow them will throw an {@link UnsupportedOperationException}.
   */
  public GraphBuilder<N, E> allowsParallelEdges(boolean allowsParallelEdges) {
    this.allowsParallelEdges = allowsParallelEdges;
    return this;
  }

  /**
   * Specifies whether the graph will allow self-loops (edges that connect a node to itself).
   * Attempting to add a self-loop to a graph that does not allow them will throw an
   * {@link UnsupportedOperationException}.
   */
  public GraphBuilder<N, E> allowsSelfLoops(boolean allowsSelfLoops) {
    this.allowsSelfLoops = allowsSelfLoops;
    return this;
  }

  /**
   * Specifies the expected number of nodes in the graph.
   *
   * @throws IllegalArgumentException if {@code expectedNodeCount} is negative
   */
  public GraphBuilder<N, E> expectedNodeCount(int expectedNodeCount) {
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
  public GraphBuilder<N, E> expectedEdgeCount(int expectedEdgeCount) {
    checkArgument(expectedEdgeCount >= 0, "The expected number of edges can't be negative: %s",
        expectedEdgeCount);
    this.expectedEdgeCount = Optional.of(expectedEdgeCount);
    return this;
  }

  /**
   * Returns an empty mutable {@link Graph} with the properties of this {@link GraphBuilder}.
   */
  public <N1 extends N, E1 extends E> Graph<N1, E1> build() {
    return new ConfigurableGraph<N1, E1>(this);
  }
}
