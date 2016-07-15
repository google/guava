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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.graph.GraphConstants.INNER_CAPACITY;
import static com.google.common.graph.GraphConstants.INNER_LOAD_FACTOR;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A class representing an origin node's adjacent nodes in an undirected graph.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 */
final class UndirectedGraphConnections<N> implements GraphConnections<N> {
  private final Set<N> adjacentNodes;

  private UndirectedGraphConnections(Set<N> adjacentNodes) {
    this.adjacentNodes = checkNotNull(adjacentNodes, "adjacentNodes");
  }

  static <N> UndirectedGraphConnections<N> of() {
    return new UndirectedGraphConnections<N>(new HashSet<N>(INNER_CAPACITY, INNER_LOAD_FACTOR));
  }

  static <N> UndirectedGraphConnections<N> ofImmutable(Set<N> adjacentNodes) {
    return new UndirectedGraphConnections<N>(ImmutableSet.copyOf(adjacentNodes));
  }

  @Override
  public Set<N> adjacentNodes() {
    return Collections.unmodifiableSet(adjacentNodes);
  }

  @Override
  public Set<N> predecessors() {
    return adjacentNodes();
  }

  @Override
  public Set<N> successors() {
    return adjacentNodes();
  }

  @Override
  public void removePredecessor(Object node) {
    removeSuccessor(node);
  }

  @Override
  public void removeSuccessor(Object node) {
    checkNotNull(node, "node");
    adjacentNodes.remove(node);
  }

  @Override
  public void addPredecessor(N node) {
    addSuccessor(node);
  }

  @Override
  public void addSuccessor(N node) {
    checkNotNull(node, "node");
    adjacentNodes.add(node);
  }
}
