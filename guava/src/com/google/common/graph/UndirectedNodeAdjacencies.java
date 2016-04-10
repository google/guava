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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nullable;

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
    // TODO(user): Enable users to specify the expected number of neighbors of a new node.
    return new UndirectedNodeAdjacencies<N>(Sets.<N>newHashSet());
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
  public boolean removePredecessor(Object node) {
    return removeSuccessor(node);
  }

  @Override
  public boolean removeSuccessor(Object node) {
    checkNotNull(node, "node");
    return adjacentNodes.remove(node);
  }

  @Override
  public boolean addPredecessor(N node) {
    return addSuccessor(node);
  }

  @Override
  public boolean addSuccessor(N node) {
    checkNotNull(node, "node");
    return adjacentNodes.add(node);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(adjacentNodes);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof UndirectedNodeAdjacencies) {
      UndirectedNodeAdjacencies<?> that = (UndirectedNodeAdjacencies<?>) object;
      return this.adjacentNodes.equals(that.adjacentNodes);
    }
    return false;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("adjacentNodes", adjacentNodes)
        .toString();
  }
}
