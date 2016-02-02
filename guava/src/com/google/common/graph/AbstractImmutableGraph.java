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

/**
 * Abstract base class for implementation of immutable graphs/hypergraphs.
 *
 * <p>All mutation methods are not supported as the graph can't be modified.
 *
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
abstract class AbstractImmutableGraph<N, E> implements Graph<N, E> {

  @Override
  public boolean addNode(N n) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addEdge(E e, N n1, N n2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeNode(Object n) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeEdge(Object e) {
    throw new UnsupportedOperationException();
  }

  /**
   * An interface for builders of immutable graph instances.
   *
   * @param <N> Node parameter type
   * @param <E> Edge parameter type
   * TODO(user): Consider throwing exception when the graph is not affected
   * by method calls (methods returning false).
   */
  interface Builder<N, E> {

    /**
     * Adds {@code n} to the graph being built.
     *
     * @return this {@code Builder} instance
     * @throws NullPointerException if {@code n} is null
     */
    Builder<N, E> addNode(N n);

    /**
     * Adds {@code e} to the graph being built, connecting {@code n1} and {@code n2};
     * adds {@code n1} and {@code n2} if not already present.
     *
     * @return this {@code Builder} instance
     * @throws IllegalArgumentException when {@code Graph.addEdge(e, n1, n2)} throws
     *     on the graph being built
     * @throws NullPointerException if {@code e}, {@code n1}, or {@code n2} is null
     * @see Graph#addEdge(E, N, N)
     */
    Builder<N, E> addEdge(E e, N n1, N n2);

    /**
     * Creates and returns a new instance of {@code AbstractImmutableGraph}
     * based on the contents of the {@code Builder}.
     */
    AbstractImmutableGraph<N, E> build();
  }
}
