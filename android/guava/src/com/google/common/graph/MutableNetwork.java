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
 * A subinterface of {@link Network} which adds mutation methods. When mutation is not required,
 * users should prefer the {@link Network} interface.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
@Beta
@ElementTypesAreNonnullByDefault
public interface MutableNetwork<N, E> extends Network<N, E> {

  /**
   * Adds {@code node} if it is not already present.
   *
   * <p><b>Nodes must be unique</b>, just as {@code Map} keys must be. They must also be non-null.
   *
   * @return {@code true} if the network was modified as a result of this call
   */
  @CanIgnoreReturnValue
  boolean addNode(N node);

  /**
   * Adds {@code edge} connecting {@code nodeU} to {@code nodeV}.
   *
   * <p>If the graph is directed, {@code edge} will be directed in this graph; otherwise, it will be
   * undirected.
   *
   * <p><b>{@code edge} must be unique to this graph</b>, just as a {@code Map} key must be. It must
   * also be non-null.
   *
   * <p>If {@code nodeU} and {@code nodeV} are not already present in this graph, this method will
   * silently {@link #addNode(Object) add} {@code nodeU} and {@code nodeV} to the graph.
   *
   * <p>If {@code edge} already connects {@code nodeU} to {@code nodeV} (in the specified order if
   * this network {@link #isDirected()}, else in any order), then this method will have no effect.
   *
   * @return {@code true} if the network was modified as a result of this call
   * @throws IllegalArgumentException if {@code edge} already exists in the graph and does not
   *     connect {@code nodeU} to {@code nodeV}
   * @throws IllegalArgumentException if the introduction of the edge would violate {@link
   *     #allowsParallelEdges()} or {@link #allowsSelfLoops()}
   */
  @CanIgnoreReturnValue
  boolean addEdge(N nodeU, N nodeV, E edge);

  /**
   * Adds {@code edge} connecting {@code endpoints}. In an undirected network, {@code edge} will
   * also connect {@code nodeV} to {@code nodeU}.
   *
   * <p>If this graph is directed, {@code edge} will be directed in this graph; if it is undirected,
   * {@code edge} will be undirected in this graph.
   *
   * <p>If this graph is directed, {@code endpoints} must be ordered.
   *
   * <p><b>{@code edge} must be unique to this graph</b>, just as a {@code Map} key must be. It must
   * also be non-null.
   *
   * <p>If either or both endpoints are not already present in this graph, this method will silently
   * {@link #addNode(Object) add} each missing endpoint to the graph.
   *
   * <p>If {@code edge} already connects an endpoint pair equal to {@code endpoints}, then this
   * method will have no effect.
   *
   * @return {@code true} if the network was modified as a result of this call
   * @throws IllegalArgumentException if {@code edge} already exists in the graph and connects some
   *     other endpoint pair that is not equal to {@code endpoints}
   * @throws IllegalArgumentException if the introduction of the edge would violate {@link
   *     #allowsParallelEdges()} or {@link #allowsSelfLoops()}
   * @throws IllegalArgumentException if the endpoints are unordered and the graph is directed
   * @since 27.1
   */
  @CanIgnoreReturnValue
  boolean addEdge(EndpointPair<N> endpoints, E edge);

  /**
   * Removes {@code node} if it is present; all edges incident to {@code node} will also be removed.
   *
   * @return {@code true} if the network was modified as a result of this call
   */
  @CanIgnoreReturnValue
  boolean removeNode(N node);

  /**
   * Removes {@code edge} from this network, if it is present.
   *
   * @return {@code true} if the network was modified as a result of this call
   */
  @CanIgnoreReturnValue
  boolean removeEdge(E edge);
}
