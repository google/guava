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

/**
 * A builder for constructing instances of {@link MutableValueGraph} or {@link ImmutableValueGraph}
 * with user-defined properties.
 *
 * <p>A {@code ValueGraph} built by this class has the following default properties:
 *
 * <ul>
 *   <li>does not allow self-loops
 *   <li>orders {@link ValueGraph#nodes()} in the order in which the elements were added (insertion
 *       order)
 * </ul>
 *
 * <p>{@code ValueGraph}s built by this class also guarantee that each collection-returning accessor
 * returns a <b>(live) unmodifiable view</b>; see <a
 * href="https://github.com/google/guava/wiki/GraphsExplained#accessor-behavior">the external
 * documentation</a> for details.
 *
 * <p>Examples of use:
 *
 * <pre>{@code
 * // Building a mutable value graph
 * MutableValueGraph<String, Double> graph =
 *     ValueGraphBuilder.undirected().allowsSelfLoops(true).build();
 * graph.putEdgeValue("San Francisco", "San Francisco", 0.0);
 * graph.putEdgeValue("San Jose", "San Jose", 0.0);
 * graph.putEdgeValue("San Francisco", "San Jose", 48.4);
 *
 * // Building an immutable value graph
 * ImmutableValueGraph<String, Double> immutableGraph =
 *     ValueGraphBuilder.undirected()
 *         .allowsSelfLoops(true)
 *         .<String, Double>immutable()
 *         .putEdgeValue("San Francisco", "San Francisco", 0.0)
 *         .putEdgeValue("San Jose", "San Jose", 0.0)
 *         .putEdgeValue("San Francisco", "San Jose", 48.4)
 *         .build();
 * }</pre>
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @param <N> The most general node type this builder will support. This is normally {@code Object}
 *     unless it is constrained by using a method like {@link #nodeOrder}, or the builder is
 *     constructed based on an existing {@code ValueGraph} using {@link #from(ValueGraph)}.
 * @param <V> The most general value type this builder will support. This is normally {@code Object}
 *     unless the builder is constructed based on an existing {@code Graph} using {@link
 *     #from(ValueGraph)}.
 * @since 20.0
 */
@Beta
public final class ValueGraphBuilder<N, V> extends AbstractGraphBuilder<N> {

  /** Creates a new instance with the specified edge directionality. */
  private ValueGraphBuilder(boolean directed) {
    super(directed);
  }

  /** Returns a {@link ValueGraphBuilder} for building directed graphs. */
  public static ValueGraphBuilder<Object, Object> directed() {
    return new ValueGraphBuilder<>(true);
  }

  /** Returns a {@link ValueGraphBuilder} for building undirected graphs. */
  public static ValueGraphBuilder<Object, Object> undirected() {
    return new ValueGraphBuilder<>(false);
  }

  /**
   * Returns a {@link ValueGraphBuilder} initialized with all properties queryable from {@code
   * graph}.
   *
   * <p>The "queryable" properties are those that are exposed through the {@link ValueGraph}
   * interface, such as {@link ValueGraph#isDirected()}. Other properties, such as {@link
   * #expectedNodeCount(int)}, are not set in the new builder.
   */
  public static <N, V> ValueGraphBuilder<N, V> from(ValueGraph<N, V> graph) {
    return new ValueGraphBuilder<N, V>(graph.isDirected())
        .allowsSelfLoops(graph.allowsSelfLoops())
        .nodeOrder(graph.nodeOrder())
        .incidentEdgeOrder(graph.incidentEdgeOrder());
  }

  /**
   * Returns an {@link ImmutableValueGraph.Builder} with the properties of this {@link
   * ValueGraphBuilder}.
   *
   * <p>The returned builder can be used for populating an {@link ImmutableValueGraph}.
   *
   * <p>Note that the returned builder will always have {@link #incidentEdgeOrder} set to {@link
   * ElementOrder#stable()}, regardless of the value that was set in this builder.
   *
   * @since 28.0
   */
  public <N1 extends N, V1 extends V> ImmutableValueGraph.Builder<N1, V1> immutable() {
    ValueGraphBuilder<N1, V1> castBuilder = cast();
    return new ImmutableValueGraph.Builder<>(castBuilder);
  }

  /**
   * Specifies whether the graph will allow self-loops (edges that connect a node to itself).
   * Attempting to add a self-loop to a graph that does not allow them will throw an {@link
   * UnsupportedOperationException}.
   *
   * <p>The default value is {@code false}.
   */
  @CanIgnoreReturnValue
  public ValueGraphBuilder<N, V> allowsSelfLoops(boolean allowsSelfLoops) {
    this.allowsSelfLoops = allowsSelfLoops;
    return this;
  }

  /**
   * Specifies the expected number of nodes in the graph.
   *
   * @throws IllegalArgumentException if {@code expectedNodeCount} is negative
   */
  @CanIgnoreReturnValue
  public ValueGraphBuilder<N, V> expectedNodeCount(int expectedNodeCount) {
    this.expectedNodeCount = Optional.of(checkNonNegative(expectedNodeCount));
    return this;
  }

  /**
   * Specifies the order of iteration for the elements of {@link Graph#nodes()}.
   *
   * <p>The default value is {@link ElementOrder#insertion() insertion order}.
   */
  public <N1 extends N> ValueGraphBuilder<N1, V> nodeOrder(ElementOrder<N1> nodeOrder) {
    ValueGraphBuilder<N1, V> newBuilder = cast();
    newBuilder.nodeOrder = checkNotNull(nodeOrder);
    return newBuilder;
  }

  /**
   * Specifies the order of iteration for the elements of {@link ValueGraph#edges()}, {@link
   * ValueGraph#adjacentNodes(Object)}, {@link ValueGraph#predecessors(Object)}, {@link
   * ValueGraph#successors(Object)} and {@link ValueGraph#incidentEdges(Object)}.
   *
   * <p>The default value is {@link ElementOrder#unordered() unordered} for mutable graphs. For
   * immutable graphs, this value is ignored; they always have a {@link ElementOrder#stable()
   * stable} order.
   *
   * @throws IllegalArgumentException if {@code incidentEdgeOrder} is not either {@code
   *     ElementOrder.unordered()} or {@code ElementOrder.stable()}.
   * @since 29.0
   */
  public <N1 extends N> ValueGraphBuilder<N1, V> incidentEdgeOrder(
      ElementOrder<N1> incidentEdgeOrder) {
    checkArgument(
        incidentEdgeOrder.type() == ElementOrder.Type.UNORDERED
            || incidentEdgeOrder.type() == ElementOrder.Type.STABLE,
        "The given elementOrder (%s) is unsupported. incidentEdgeOrder() only supports"
            + " ElementOrder.unordered() and ElementOrder.stable().",
        incidentEdgeOrder);
    ValueGraphBuilder<N1, V> newBuilder = cast();
    newBuilder.incidentEdgeOrder = checkNotNull(incidentEdgeOrder);
    return newBuilder;
  }
  /**
   * Returns an empty {@link MutableValueGraph} with the properties of this {@link
   * ValueGraphBuilder}.
   */
  public <N1 extends N, V1 extends V> MutableValueGraph<N1, V1> build() {
    return new StandardMutableValueGraph<>(this);
  }

  ValueGraphBuilder<N, V> copy() {
    ValueGraphBuilder<N, V> newBuilder = new ValueGraphBuilder<>(directed);
    newBuilder.allowsSelfLoops = allowsSelfLoops;
    newBuilder.nodeOrder = nodeOrder;
    newBuilder.expectedNodeCount = expectedNodeCount;
    newBuilder.incidentEdgeOrder = incidentEdgeOrder;
    return newBuilder;
  }

  @SuppressWarnings("unchecked")
  private <N1 extends N, V1 extends V> ValueGraphBuilder<N1, V1> cast() {
    return (ValueGraphBuilder<N1, V1>) this;
  }
}
