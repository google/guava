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
 * A subinterface of {@link Graph} which adds mutation methods. When mutation is not required, users
 * should prefer the {@link Graph} interface.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @since 20.0
 */
@Beta
public interface MutableGraph<N> extends Graph<N> {

  /**
   * Adds {@code node} if it is not already present.
   *
   * <p><b>Nodes must be unique</b>, just as {@code Map} keys must be. They must also be non-null.
   *
   * @return {@code true} if the graph was modified as a result of this call
   */
  @CanIgnoreReturnValue
  boolean addNode(N node);

  /**
   * Adds an edge connecting {@code nodeU} to {@code nodeV} if one is not already present.
   *
   * <p>If the graph is directed, the resultant edge will be directed; otherwise, it will be
   * undirected.
   *
   * <p>If {@code nodeU} and {@code nodeV} are not already present in this graph, this method will
   * silently {@link #addNode(Object) add} {@code nodeU} and {@code nodeV} to the graph.
   *
   * @return {@code true} if the graph was modified as a result of this call
   * @throws IllegalArgumentException if the introduction of the edge would violate {@link
   *     #allowsSelfLoops()}
   */
  @CanIgnoreReturnValue
  boolean putEdge(N nodeU, N nodeV);

  /**
   * Adds an edge connecting {@code endpoints} (in the order, if any, specified by {@code
   * endpoints}) if one is not already present.
   *
   * <p>If this graph is directed, {@code endpoints} must be ordered and the added edge will be
   * directed; if it is undirected, the added edge will be undirected.
   *
   * <p>If this graph is directed, {@code endpoints} must be ordered.
   *
   * <p>If either or both endpoints are not already present in this graph, this method will silently
   * {@link #addNode(Object) add} each missing endpoint to the graph.
   *
   * @return {@code true} if the graph was modified as a result of this call
   * @throws IllegalArgumentException if the introduction of the edge would violate {@link
   *     #allowsSelfLoops()}
   * @throws IllegalArgumentException if the endpoints are unordered and the graph is directed
   * @since 27.1
   */
  @CanIgnoreReturnValue
  boolean putEdge(EndpointPair<N> endpoints);

  /**
   * Removes {@code node} if it is present; all edges incident to {@code node} will also be removed.
   *
   * @return {@code true} if the graph was modified as a result of this call
   */
  @CanIgnoreReturnValue
  boolean removeNode(N node);

  /**
   * Removes the edge connecting {@code nodeU} to {@code nodeV}, if it is present.
   *
   * @return {@code true} if the graph was modified as a result of this call
   */
  @CanIgnoreReturnValue
  boolean removeEdge(N nodeU, N nodeV);

  /**
   * Removes the edge connecting {@code endpoints}, if it is present.
   *
   * <p>If this graph is directed, {@code endpoints} must be ordered.
   *
   * @throws IllegalArgumentException if the endpoints are unordered and the graph is directed
   * @return {@code true} if the graph was modified as a result of this call
   * @since 27.1
   */
  @CanIgnoreReturnValue
  boolean removeEdge(EndpointPair<N> endpoints);
}
