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
// to narrow the generic type when Comparators are provided.
@Beta
public final class GraphBuilder<N> {
  final boolean directed;
  boolean allowsSelfLoops = true;
  Comparator<N> nodeComparator = null;
  Optional<Integer> expectedNodeCount = Optional.absent();

  /**
   * Creates a new instance with the specified edge directionality.
   *
   * @param directed if true, creates an instance for graphs whose edges are each directed;
   *      if false, creates an instance for graphs whose edges are each undirected.
   */
  private GraphBuilder(boolean directed) {
    this.directed = directed;
  }

  /**
   * Returns a {@link GraphBuilder} for building directed graphs.
   */
  public static GraphBuilder<Object> directed() {
    return new GraphBuilder<Object>(true);
  }

  /**
   * Returns a {@link GraphBuilder} for building undirected graphs.
   */
  public static GraphBuilder<Object> undirected() {
    return new GraphBuilder<Object>(false);
  }

  /**
   * Returns a {@link GraphBuilder} initialized with all properties queryable from {@code graph}.
   *
   * <p>The "queryable" properties are those that are exposed through the {@link Graph} interface,
   * such as {@link Graph#isDirected()}. Other properties, such as {@link #expectedNodeCount(int)},
   * are not set in the new builder.
   */
  public static <N> GraphBuilder<N> from(Graph<N> graph) {
    // TODO(b/28087289): add allowsParallelEdges() once we support them
    return new GraphBuilder<N>(graph.isDirected())
        .allowsSelfLoops(graph.allowsSelfLoops());
  }

  /**
   * Specifies whether the graph will allow self-loops (edges that connect a node to itself).
   * Attempting to add a self-loop to a graph that does not allow them will throw an
   * {@link UnsupportedOperationException}.
   */
  public GraphBuilder<N> allowsSelfLoops(boolean allowsSelfLoops) {
    this.allowsSelfLoops = allowsSelfLoops;
    return this;
  }

  /**
   * Specifies the expected number of nodes in the graph.
   *
   * @throws IllegalArgumentException if {@code expectedNodeCount} is negative
   */
  public GraphBuilder<N> expectedNodeCount(int expectedNodeCount) {
    checkArgument(expectedNodeCount >= 0, "The expected number of nodes can't be negative: %s",
        expectedNodeCount);
    this.expectedNodeCount = Optional.of(expectedNodeCount);
    return this;
  }

  /**
   * Returns an empty {@link MutableGraph} with the properties of this {@link GraphBuilder}.
   */
  public <N1 extends N> MutableGraph<N1> build() {
    return new ConfigurableMutableGraph<N1>(this);
  }
}
