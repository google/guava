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
   * Adds an (implicit) edge to this graph connecting {@code nodeA} to {@code nodeB}
   * (optional operation).
   *
   * <p>Behavior if {@code nodeA} and {@code nodeB} are not already elements of the graph is
   * unspecified. Suggested behaviors include (a) silently adding {@code nodeA} and {@code nodeB}
   * to the graph or (b) throwing {@code IllegalArgumentException}.
   *
   * <p>Currently, this type does not support parallel edges.  {@code addEdge(nodeA, nodeB)} will
   * simply return false on any future calls with the same arguments (analogous to the behavior of
   * {@code Network.addEdge(e, nodeA, nodeB)}).  A hypothetical instance that supported parallel
   * edges would add a new edge between {@code nodeA} and {@code nodeB} for every call to
   * {@code addEdge(nodeA, nodeB)}, and return {@code true} every time.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the add operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean addEdge(N nodeA, N nodeB);

  /**
   * Removes {@code node} from this graph, if it is present (optional operation).
   * All edges incident to {@code node} in this graph will also be removed.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the remove operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean removeNode(Object node);

  /**
   * Removes an edge connecting {@code nodeA} to {@code nodeB} from this graph, if one is present
   * (optional operation).
   *
   * <p>In general, the input nodes are unaffected (although implementations may choose
   * to disallow certain configurations, e.g., isolated nodes).
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the remove operation is not supported by this graph
   */
  @CanIgnoreReturnValue
  boolean removeEdge(Object nodeA, Object nodeB);
}
