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

import com.google.common.annotations.Beta;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * A subinterface of {@link Network} which specifies that all edges are hyperedges, that is,
 * they connect arbitrary sets of nodes rather than pairs of nodes.
 *
 * <p>A few notes about how hyperedges and connectivity:
 * <ul>
 * <li>Hyperedges, like undirected edges, are both incoming and outgoing edges.
 * <li>Hyperedges incident to a single node {@code node} connect {@code node} to itself; such edges
 *     are analogous to self-loops in graphs.  Hyperedges incident to > 1 nodes do not connect any
 *     of their incident nodes to themselves.
 * </ul>
 *
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
@Beta
public interface Hypergraph<N, E> extends Network<N, E> {
  /**
   * Source is not applicable to hypergraphs.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation. Call {@link #incidentNodes(Object)} instead.
   */
  @Deprecated
  @Override
  N source(Object edge);

  /**
   * Target is not applicable to hypergraphs.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation. Call {@link #incidentNodes(Object)} instead.
   */
  @Deprecated
  @Override
  N target(Object edge);

  /**
   * Adds {@code edge} to this graph, connecting {@code nodes}.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the add operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean addEdge(E edge, N... nodes);

  /**
   * Adds {@code edge} to this graph, connecting {@code nodes}.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the add operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean addEdge(E edge, Iterable<N> nodes);
}
