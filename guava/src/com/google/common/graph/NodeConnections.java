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
   * Remove {@code edge} from the sets of in edges. If this is known to be the last remaining edge
   * between two (formally) connected nodes, {@link #removePredecessor} must also be called.
   */
  void removeInEdge(Object edge);

  /**
   * Remove {@code edge} from the sets of out edges. If this is known to be the last remaining edge
   * between two (formally) connected nodes, {@link #removeSuccessor} must also be called.
   */
  void removeOutEdge(Object edge);

  /**
   * Remove {@code node} from the set of predecessors.
   */
  void removePredecessor(Object node);

  /**
   * Remove {@code node} from the set of successors.
   */
  void removeSuccessor(Object node);

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
