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
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * A subtype of {@link ValueGraph} which permits mutations.
 * Users should generally use the {@link ValueGraph} interface where possible.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
 */
@Beta
public interface MutableValueGraph<N, V> extends ValueGraph<N, V> {

  /**
   * Adds {@code node} to this graph.
   *
   * <p><b>Nodes must be unique</b>, just as {@code Map} keys must be; they must also be non-null.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   */
  @CanIgnoreReturnValue
  boolean addNode(N node);

  /**
   * Adds an edge connecting {@code nodeA} to {@code nodeB} if one is not already present.
   * Associates {@code value} with that edge (as returned by {@link #edgeValue(Object, Object)}).
   *
   * <p>Values in a graph do not have to be unique. However, values must be non-null.
   *
   * @return {@code true} the value previously associated with the edge connecting {@code nodeA} to
   *     {@code nodeB}, or null if there was no edge.
   */
  @CanIgnoreReturnValue
  V putEdgeValue(N nodeA, N nodeB, V value);

  /**
   * Removes {@code node} from this graph, if it is present.
   * All edges incident to {@code node} in this graph will also be removed.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   */
  @CanIgnoreReturnValue
  boolean removeNode(Object node);

  /**
   * Removes the edge connecting {@code nodeA} to {@code nodeB}, if it is present.
   *
   * @return {@code true} the value previously associated with the edge connecting {@code nodeA} to
   *     {@code nodeB}, or null if there was no edge.
   */
  @CanIgnoreReturnValue
  V removeEdge(Object nodeA, Object nodeB);
}
