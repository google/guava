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

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Set;

/**
 * An interface for representing an origin node's adjacent nodes and incident edges in a network.
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
   * Returns the node that is opposite the origin node along {@code edge}.
   * In the directed case, {@code edge} is assumed to be an outgoing edge.
   */
  N oppositeNode(Object edge);

  /**
   * Returns the set of edges connecting the origin node to {@code node}.
   * For networks without parallel edges, this set cannot be of size greater than one.
   */
  Set<E> edgesConnecting(Object node);

  /**
   * Remove {@code edge} from the set of incoming edges. Returns the former predecessor node.
   */
  @CanIgnoreReturnValue
  N removeInEdge(Object edge);

  /**
   * Remove {@code edge} from the set of outgoing edges. Returns the former successor node.
   */
  @CanIgnoreReturnValue
  N removeOutEdge(Object edge);

  /**
   * Add {@code edge} to the set of incoming edges. Implicitly adds {@code node} as a predecessor.
   * Returns true if the edge did not already exist.
   */
  @CanIgnoreReturnValue
  boolean addInEdge(E edge, N node);

  /**
   * Add {@code edge} to the set of outgoing edges. Implicitly adds {@code node} as a successor.
   * Returns true if the edge did not already exist.
   */
  @CanIgnoreReturnValue
  boolean addOutEdge(E edge, N node);
}
