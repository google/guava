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
import static com.google.common.graph.GraphConstants.EXPECTED_DEGREE;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Set;

/**
 * A class representing an origin node's adjacent nodes in an undirected graph.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 */
final class UndirectedNodeAdjacencies<N> implements NodeAdjacencies<N> {
  private final Set<N> adjacentNodes;

  private UndirectedNodeAdjacencies(Set<N> adjacentNodes) {
    this.adjacentNodes = checkNotNull(adjacentNodes, "adjacentNodes");
  }

  static <N> UndirectedNodeAdjacencies<N> of() {
    return new UndirectedNodeAdjacencies<N>(Sets.<N>newHashSetWithExpectedSize(EXPECTED_DEGREE));
  }

  static <N> UndirectedNodeAdjacencies<N> ofImmutable(Set<N> adjacentNodes) {
    return new UndirectedNodeAdjacencies<N>(ImmutableSet.copyOf(adjacentNodes));
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
