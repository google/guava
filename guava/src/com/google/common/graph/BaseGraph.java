/*
 * Copyright (C) 2017 The Guava Authors
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

import java.util.Set;

/**
 * A non-public interface for the methods shared between {@link Graph} and {@link ValueGraph}.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 */
interface BaseGraph<N> extends ArchetypeGraph<N> {
  //
  // Graph-level accessors
  //

  /** Returns all edges in this graph. */
  Set<EndpointPair<N>> edges();

  /**
   * Returns an {@link ElementOrder} that specifies the order of iteration for the elements of
   * {@link #edges()}, {@link #adjacentNodes(Object)}, {@link #predecessors(Object)}, {@link
   * #successors(Object)} and {@link #incidentEdges(Object)}.
   *
   * @since 29.0
   */
  ElementOrder<N> incidentEdgeOrder();

  /**
   * Returns a live view of this graph as a {@link Network} whose edges {@code E} are {@code
   * EndpointPair<N>} objects (that is, a {@code Network<N, EndpointPair<N>>}). The resulting {@code
   * Network}'s edge-oriented methods (such as {@code inEdges()}) will return views transformed from
   * the corresponding node-oriented methods (such as {@code predecessors()}).
   *
   * <p>This capability facilitates writing implementations of <a
   * href="https://github.com/google/guava/wiki/GraphsExplained#graph-types-for-algorithms">edge-oriented
   * code</a>.
   *
   * @since NEXT
   */
  Network<N, EndpointPair<N>> asNetwork();

  //
  // Element-level accessors
  //

  /**
   * Returns a live view of the edges in this graph whose endpoints include {@code node}.
   *
   * <p>This is equal to the union of incoming and outgoing edges.
   *
   * <p>If {@code node} is removed from the graph after this method is called, the {@code Set}
   * {@code view} returned by this method will be invalidated, and will throw {@code
   * IllegalStateException} if it is accessed in any way, with the following exceptions:
   *
   * <ul>
   *   <li>{@code view.equals(view)} evaluates to {@code true} (but any other `equals()` expression
   *       involving {@code view} will throw)
   *   <li>{@code hashCode()} does not throw
   *   <li>if {@code node} is re-added to the graph after having been removed, {@code view}'s
   *       behavior is undefined
   * </ul>
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   * @since 24.0
   */
  Set<EndpointPair<N>> incidentEdges(N node);
}
