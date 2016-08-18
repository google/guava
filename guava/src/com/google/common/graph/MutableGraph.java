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
 * A subinterface of {@link Graph} which adds mutation methods. When mutation is not required, users
 * should prefer the {@link Graph} interface.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
 */
@Beta
public interface MutableGraph<N, V> extends Graph<N, V> {

  /**
   * Adds {@code node} if it is not already present.
   *
   * <p><b>Nodes must be unique</b>, just as {@code Map} keys must be. They must also be non-null.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   */
  @CanIgnoreReturnValue
  boolean addNode(N node);

  /**
   * Adds an edge connecting {@code nodeA} to {@code nodeB} if one is not already present; associate
   * that edge with {@code value}. In an undirected graph, the edge will also connect {@code nodeB}
   * to {@code nodeA}.
   *
   * <p>Values do not have to be unique. However, values must be non-null.
   *
   * <p>Behavior if {@code nodeA} and {@code nodeB} are not already present in this graph is
   * implementation-dependent. Suggested behaviors include (a) silently {@link #addNode(Object)
   * adding} {@code nodeA} and {@code nodeB} to the graph (this is the behavior of the default
   * implementations) or (b) throwing {@code IllegalArgumentException}.
   *
   * @return the value previously associated with the edge connecting {@code nodeA} to {@code
   *     nodeB}, or null if there was no such edge.
   * @throws IllegalArgumentException if the introduction of the edge would violate {@link
   *     #allowsSelfLoops()}
   */
  @CanIgnoreReturnValue
  V putEdgeValue(N nodeA, N nodeB, V value);

  /**
   * Removes {@code node} if it is present; all edges incident to {@code node} will also be removed.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   */
  @CanIgnoreReturnValue
  boolean removeNode(Object node);

  /**
   * Removes the edge connecting {@code nodeA} to {@code nodeB}, if it is present.
   *
   * @return the value previously associated with the edge connecting {@code nodeA} to {@code
   *     nodeB}, or null if there was no such edge.
   */
  @CanIgnoreReturnValue
  V removeEdge(Object nodeA, Object nodeB);
}
