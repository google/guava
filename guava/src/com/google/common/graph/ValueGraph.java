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

import com.google.common.annotations.Beta;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * An interface for <a
 * href="https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)">graph</a>-structured data,
 * whose edges have associated non-unique values.
 *
 * <p>A graph is composed of a set of nodes and a set of edges connecting pairs of nodes.
 *
 * <p>There are three primary interfaces provided to represent graphs. In order of increasing
 * complexity they are: {@link Graph}, {@link ValueGraph}, and {@link Network}. You should generally
 * prefer the simplest interface that satisfies your use case. See the <a
 * href="https://github.com/google/guava/wiki/GraphsExplained#choosing-the-right-graph-type">
 * "Choosing the right graph type"</a> section of the Guava User Guide for more details.
 *
 * <h3>Capabilities</h3>
 *
 * <p>{@code ValueGraph} supports the following use cases (<a
 * href="https://github.com/google/guava/wiki/GraphsExplained#definitions">definitions of
 * terms</a>):
 *
 * <ul>
 *   <li>directed graphs
 *   <li>undirected graphs
 *   <li>graphs that do/don't allow self-loops
 *   <li>graphs whose nodes/edges are insertion-ordered, sorted, or unordered
 *   <li>graphs whose edges have associated values
 * </ul>
 *
 * <p>{@code ValueGraph}, as a subtype of {@code Graph}, explicitly does not support parallel edges,
 * and forbids implementations or extensions with parallel edges. If you need parallel edges, use
 * {@link Network}. (You can use a positive {@code Integer} edge value as a loose representation of
 * edge multiplicity, but the {@code *degree()} and mutation methods will not reflect your
 * interpretation of the edge value as its multiplicity.)
 *
 * <h3>Building a {@code ValueGraph}</h3>
 *
 * <p>The implementation classes that `common.graph` provides are not public, by design. To create
 * an instance of one of the built-in implementations of {@code ValueGraph}, use the {@link
 * ValueGraphBuilder} class:
 *
 * <pre>{@code
 *   MutableValueGraph<Integer, Double> graph = ValueGraphBuilder.directed().build();
 * }</pre>
 *
 * <p>{@link ValueGraphBuilder#build()} returns an instance of {@link MutableValueGraph}, which is a
 * subtype of {@code ValueGraph} that provides methods for adding and removing nodes and edges. If
 * you do not need to mutate a graph (e.g. if you write a method than runs a read-only algorithm on
 * the graph), you should use the non-mutating {@link ValueGraph} interface, or an {@link
 * ImmutableValueGraph}.
 *
 * <p>You can create an immutable copy of an existing {@code ValueGraph} using {@link
 * ImmutableValueGraph#copyOf(ValueGraph)}:
 *
 * <pre>{@code
 *   ImmutableValueGraph<Integer, Double> immutableGraph = ImmutableValueGraph.copyOf(graph);
 * }</pre>
 *
 * <p>Instances of {@link ImmutableValueGraph} do not implement {@link MutableValueGraph}
 * (obviously!) and are contractually guaranteed to be unmodifiable and thread-safe.
 *
 * <p>The Guava User Guide has <a
 * href="https://github.com/google/guava/wiki/GraphsExplained#building-graph-instances">more
 * information on (and examples of) building graphs</a>.
 *
 * <h3>Additional documentation</h3>
 *
 * <p>See the Guava User Guide for the {@code common.graph} package (<a
 * href="https://github.com/google/guava/wiki/GraphsExplained">"Graphs Explained"</a>) for
 * additional documentation, including:
 *
 * <ul>
 *   <li><a
 *       href="https://github.com/google/guava/wiki/GraphsExplained#equals-hashcode-and-graph-equivalence">
 *       {@code equals()}, {@code hashCode()}, and graph equivalence</a>
 *   <li><a href="https://github.com/google/guava/wiki/GraphsExplained#synchronization">
 *       Synchronization policy</a>
 *   <li><a href="https://github.com/google/guava/wiki/GraphsExplained#notes-for-implementors">Notes
 *       for implementors</a>
 * </ul>
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
 */
// TODO(b/35456940): Update the documentation to reflect the new interfaces
@Beta
public interface ValueGraph<N, V> extends BaseGraph<N> {
  //
  // ValueGraph-level accessors
  //

  /** {@inheritDoc} */
  @Override
  Set<N> nodes();

  /** {@inheritDoc} */
  @Override
  Set<EndpointPair<N>> edges();

  /**
   * Returns a live view of this graph as a {@link Graph}. The resulting {@link Graph} will have an
   * edge connecting node A to node B if this {@link ValueGraph} has an edge connecting A to B.
   */
  Graph<N> asGraph();

  //
  // ValueGraph properties
  //

  /** {@inheritDoc} */
  @Override
  boolean isDirected();

  /** {@inheritDoc} */
  @Override
  boolean allowsSelfLoops();

  /** {@inheritDoc} */
  @Override
  ElementOrder<N> nodeOrder();

  //
  // Element-level accessors
  //

  /** {@inheritDoc} */
  @Override
  Set<N> adjacentNodes(N node);

  /** {@inheritDoc} */
  @Override
  Set<N> predecessors(N node);

  /** {@inheritDoc} */
  @Override
  Set<N> successors(N node);

  /** {@inheritDoc} */
  @Override
  int degree(N node);

  /** {@inheritDoc} */
  @Override
  int inDegree(N node);

  /** {@inheritDoc} */
  @Override
  int outDegree(N node);

  /** {@inheritDoc} */
  @Override
  boolean hasEdgeConnecting(N nodeU, N nodeV);

  /**
   * Returns the value of the edge connecting {@code nodeU} to {@code nodeV}, if one is present;
   * otherwise, returns {@code Optional.empty()}.
   *
   * <p>In an undirected graph, this is equal to {@code edgeValue(nodeV, nodeU)}.
   *
   * @throws IllegalArgumentException if {@code nodeU} or {@code nodeV} is not an element of this
   *     graph
   * @since 23.0 (since 20.0 with return type {@code V})
   */
  Optional<V> edgeValue(N nodeU, N nodeV);

  /**
   * Returns the value of the edge connecting {@code nodeU} to {@code nodeV}, if one is present;
   * otherwise, returns {@code defaultValue}.
   *
   * <p>In an undirected graph, this is equal to {@code edgeValueOrDefault(nodeV, nodeU,
   * defaultValue)}.
   *
   * @throws IllegalArgumentException if {@code nodeU} or {@code nodeV} is not an element of this
   *     graph
   */
  @Nullable
  V edgeValueOrDefault(N nodeU, N nodeV, @Nullable V defaultValue);

  //
  // ValueGraph identity
  //

  /**
   * Returns {@code true} iff {@code object} is a {@link ValueGraph} that has the same elements and
   * the same structural relationships as those in this graph.
   *
   * <p>Thus, two value graphs A and B are equal if <b>all</b> of the following are true:
   *
   * <ul>
   * <li>A and B have equal {@link #isDirected() directedness}.
   * <li>A and B have equal {@link #nodes() node sets}.
   * <li>A and B have equal {@link #edges() edge sets}.
   * <li>The {@link #edgeValue(Object, Object) value} of a given edge is the same in both A and B.
   * </ul>
   *
   * <p>Graph properties besides {@link #isDirected() directedness} do <b>not</b> affect equality.
   * For example, two graphs may be considered equal even if one allows self-loops and the other
   * doesn't. Additionally, the order in which nodes or edges are added to the graph, and the order
   * in which they are iterated over, are irrelevant.
   *
   * <p>A reference implementation of this is provided by {@link AbstractValueGraph#equals(Object)}.
   */
  @Override
  boolean equals(@Nullable Object object);

  /**
   * Returns the hash code for this graph. The hash code of a graph is defined as the hash code of a
   * map from each of its {@link #edges() edges} to the associated {@link #edgeValue(Object, Object)
   * edge value}.
   *
   * <p>A reference implementation of this is provided by {@link AbstractValueGraph#hashCode()}.
   */
  @Override
  int hashCode();
}
