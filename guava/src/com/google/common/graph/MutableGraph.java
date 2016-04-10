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
 * A subtype of {@link Graph} which permits mutations.
 * Users should generally use the {@link Graph} interface where possible.
 *
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @since 20.0
 */
@Beta
public interface MutableGraph<N> extends Graph<N> {

  /**
   * Adds {@code node} to this graph (optional operation).
   *
   * <p><b>Nodes must be unique</b>, just as {@code Map} keys must be; they must also be non-null.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the add operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean addNode(N node);

  /**
   * Adds an (implicit) edge to this graph connecting {@code node1} to {@code node2}
   * (optional operation).
   *
   * <p>Behavior if {@code node1} and {@code node2} are not already elements of the graph is
   * unspecified. Suggested behaviors include (a) silently adding {@code node1} and {@code node2}
   * to the graph or (b) throwing {@code IllegalArgumentException}.
   *
   * <p>Currently, this type does not support parallel edges.  {@code addEdge(node1, node2)} will
   * simply return false on any future calls with the same arguments (analogous to the behavior of
   * {@code Network.addEdge(e, node1, node2)}).  A hypothetical instance that supported parallel
   * edges would add a new edge between {@code node1} and {@code node2} for every call to
   * {@code addEdge(node1, node2)}, and return {@code true} every time.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the add operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean addEdge(N node1, N node2);

  /**
   * Removes {@code node} from this graph, if it is present (optional operation).
   * In general, all edges incident to {@code node} in this graph will also be removed.
   * (This is not true for hyperedges.)
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the remove operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean removeNode(Object node);

  /**
   * Removes an edge connecting {@code node1} to {@code node2} from this graph, if one is present
   * (optional operation).
   *
   * <p>In general, the input nodes are unaffected (although implementations may choose
   * to disallow certain configurations, e.g., isolated nodes).
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the remove operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean removeEdge(Object node1, Object node2);
}
