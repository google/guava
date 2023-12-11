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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.graph.Graphs.checkNonNegative;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotMock;

/**
 * A builder for constructing instances of {@link MutableGraph} or {@link ImmutableGraph} with
 * user-defined properties.
 *
 * <p>A {@code Graph} built by this class has the following default properties:
 *
 * <ul>
 *   <li>does not allow self-loops
 *   <li>orders {@link Graph#nodes()} in the order in which the elements were added (insertion
 *       order)
 * </ul>
 *
 * <p>{@code Graph}s built by this class also guarantee that each collection-returning accessor
 * returns a <b>(live) unmodifiable view</b>; see <a
 * href="https://github.com/google/guava/wiki/GraphsExplained#accessor-behavior">the external
 * documentation</a> for details.
 *
 * <p>Examples of use:
 *
 * <pre>{@code
 * // Building a mutable graph
 * MutableGraph<String> graph = GraphBuilder.undirected().allowsSelfLoops(true).build();
 * graph.putEdge("bread", "bread");
 * graph.putEdge("chocolate", "peanut butter");
 * graph.putEdge("peanut butter", "jelly");
 *
 * // Building an immutable graph
 * ImmutableGraph<String> immutableGraph =
 *     GraphBuilder.undirected()
 *         .allowsSelfLoops(true)
 *         .<String>immutable()
 *         .putEdge("bread", "bread")
 *         .putEdge("chocolate", "peanut butter")
 *         .putEdge("peanut butter", "jelly")
 *         .build();
 * }</pre>
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @param <N> The most general node type this builder will support. This is normally {@code Object}
 *     unless it is constrained by using a method like {@link #nodeOrder}, or the builder is
 *     constructed based on an existing {@code Graph} using {@link #from(Graph)}.
 * @since 20.0
 */
@Beta
@DoNotMock
@ElementTypesAreNonnullByDefault
public final class GraphBuilder<N> extends AbstractGraphBuilder<N> {

  /** Creates a new instance with the specified edge directionality. */
  private GraphBuilder(boolean directed) {
    super(directed);
  }

  /** Returns a {@link GraphBuilder} for building directed graphs. */
  public static GraphBuilder<Object> directed() {
    return new GraphBuilder<>(true);
  }

  /** Returns a {@link GraphBuilder} for building undirected graphs. */
  public static GraphBuilder<Object> undirected() {
    return new GraphBuilder<>(false);
  }

  /**
   * Returns a {@link GraphBuilder} initialized with all properties queryable from {@code graph}.
   *
   * <p>The "queryable" properties are those that are exposed through the {@link Graph} interface,
   * such as {@link Graph#isDirected()}. Other properties, such as {@link #expectedNodeCount(int)},
   * are not set in the new builder.
   */
  public static <N> GraphBuilder<N> from(Graph<N> graph) {
    return new GraphBuilder<N>(graph.isDirected())
        .allowsSelfLoops(graph.allowsSelfLoops())
        .nodeOrder(graph.nodeOrder())
        .incidentEdgeOrder(graph.incidentEdgeOrder());
  }

  /**
   * Returns an {@link ImmutableGraph.Builder} with the properties of this {@link GraphBuilder}.
   *
   * <p>The returned builder can be used for populating an {@link ImmutableGraph}.
   *
   * <p>Note that the returned builder will always have {@link #incidentEdgeOrder} set to {@link
   * ElementOrder#stable()}, regardless of the value that was set in this builder.
   *
   * @since 28.0
   */
  public <N1 extends N> ImmutableGraph.Builder<N1> immutable() {
    GraphBuilder<N1> castBuilder = cast();
    return new ImmutableGraph.Builder<>(castBuilder);
  }

  /**
   * Specifies whether the graph will allow self-loops (edges that connect a node to itself).
   * Attempting to add a self-loop to a graph that does not allow them will throw an {@link
   * UnsupportedOperationException}.
   *
   * <p>The default value is {@code false}.
   */
  @CanIgnoreReturnValue
  public GraphBuilder<N> allowsSelfLoops(boolean allowsSelfLoops) {
    this.allowsSelfLoops = allowsSelfLoops;
    return this;
  }

  /**
   * Specifies the expected number of nodes in the graph.
   *
   * @throws IllegalArgumentException if {@code expectedNodeCount} is negative
   */
  @CanIgnoreReturnValue
  public GraphBuilder<N> expectedNodeCount(int expectedNodeCount) {
    this.expectedNodeCount = Optional.of(checkNonNegative(expectedNodeCount));
    return this;
  }

  /**
   * Specifies the order of iteration for the elements of {@link Graph#nodes()}.
   *
   * <p>The default value is {@link ElementOrder#insertion() insertion order}.
   */
  public <N1 extends N> GraphBuilder<N1> nodeOrder(ElementOrder<N1> nodeOrder) {
    GraphBuilder<N1> newBuilder = cast();
    newBuilder.nodeOrder = checkNotNull(nodeOrder);
    return newBuilder;
  }

  /**
   * Specifies the order of iteration for the elements of {@link Graph#edges()}, {@link
   * Graph#adjacentNodes(Object)}, {@link Graph#predecessors(Object)}, {@link
   * Graph#successors(Object)} and {@link Graph#incidentEdges(Object)}.
   *
   * <p>The default value is {@link ElementOrder#unordered() unordered} for mutable graphs. For
   * immutable graphs, this value is ignored; they always have a {@link ElementOrder#stable()
   * stable} order.
   *
   * @throws IllegalArgumentException if {@code incidentEdgeOrder} is not either {@code
   *     ElementOrder.unordered()} or {@code ElementOrder.stable()}.
   * @since 29.0
   */
  public <N1 extends N> GraphBuilder<N1> incidentEdgeOrder(ElementOrder<N1> incidentEdgeOrder) {
    checkArgument(
        incidentEdgeOrder.type() == ElementOrder.Type.UNORDERED
            || incidentEdgeOrder.type() == ElementOrder.Type.STABLE,
        "The given elementOrder (%s) is unsupported. incidentEdgeOrder() only supports"
            + " ElementOrder.unordered() and ElementOrder.stable().",
        incidentEdgeOrder);
    GraphBuilder<N1> newBuilder = cast();
    newBuilder.incidentEdgeOrder = checkNotNull(incidentEdgeOrder);
    return newBuilder;
  }

  /** Returns an empty {@link MutableGraph} with the properties of this {@link GraphBuilder}. */
  public <N1 extends N> MutableGraph<N1> build() {
    return new StandardMutableGraph<>(this);
  }

  GraphBuilder<N> copy() {
    GraphBuilder<N> newBuilder = new GraphBuilder<>(directed);
    newBuilder.allowsSelfLoops = allowsSelfLoops;
    newBuilder.nodeOrder = nodeOrder;
    newBuilder.expectedNodeCount = expectedNodeCount;
    newBuilder.incidentEdgeOrder = incidentEdgeOrder;
    return newBuilder;
  }

  @SuppressWarnings("unchecked")
  private <N1 extends N> GraphBuilder<N1> cast() {
    return (GraphBuilder<N1>) this;
  }
}
