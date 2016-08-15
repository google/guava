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
 * A subtype of {@link Network} which permits mutations.
 * Users should generally use the {@link Network} interface where possible.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
@Beta
public interface MutableNetwork<N, E> extends Network<N, E> {

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
   * Adds {@code edge} connecting {@code nodeA} to {@code nodeB}.
   *
   * <p><b>Edges must be unique</b>, just as {@code Map} keys must be. They must also be non-null.
   *
   * <p>Behavior if {@code nodeA} and {@code nodeB} are not already present in this graph is
   * implementation-dependent. Suggested behaviors include (a) silently {@link #addNode(Object)
   * adding} {@code nodeA} and {@code nodeB} to the graph (this is the behavior of the default
   * implementations) or (b) throwing {@code IllegalArgumentException}.
   *
   * <p>If {@code edge} already connects {@code nodeA} to {@code nodeB} (in the specified order if
   * this graph {@link #isDirected()}, else in any order), then this method will have no effect.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws IllegalArgumentException if {@code edge} already exists and does not connect
   *     {@code nodeA} to {@code nodeB}, or if the introduction of the edge would violate
   *     {@link #allowsParallelEdges()} or {@link #allowsSelfLoops()}
   */
  @CanIgnoreReturnValue
  boolean addEdge(N nodeA, N nodeB, E edge);

  /**
   * Removes {@code node} if it is present; all edges incident to {@code node} will also be removed.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   */
  @CanIgnoreReturnValue
  boolean removeNode(Object node);

  /**
   * Removes {@code edge} from this graph, if it is present.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   */
  @CanIgnoreReturnValue
  boolean removeEdge(Object edge);
}
