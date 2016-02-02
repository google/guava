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

import java.util.Set;

/**
 * An interface for representing an origin node's adjacent nodes and incident edges in a graph.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
interface NodeConnections<N, E> {

  Set<N> adjacentNodes();

  Set<N> predecessors();

  Set<N> successors();

  Set<E> incidentEdges();

  Set<E> inEdges();

  Set<E> outEdges();

  /**
   * Remove all references to {@code node} in the sets of adjacent nodes.
   */
  void removeNode(Object node);

  /**
   * Remove all references to {@code edge} in the sets of incident edges.
   */
  void removeEdge(Object edge);

  /**
   * Add {@code node} as a predecessor to the origin node, connected with {@code edge}.
   * In the case of an undirected graph, it also becomes a successor.
   */
  void addPredecessor(N node, E edge);

  /**
   * Add {@code node} as a successor to the origin node, connected with {@code edge}.
   * In the case of an undirected graph, it also becomes a predecessor.
   */
  void addSuccessor(N node, E edge);
}
