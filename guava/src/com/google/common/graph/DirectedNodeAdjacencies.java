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
 * A class representing an origin node's adjacent nodes in a directed graph.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 */
final class DirectedNodeAdjacencies<N> implements NodeAdjacencies<N> {
  private final Set<N> predecessors;
  private final Set<N> successors;

  private DirectedNodeAdjacencies(Set<N> predecessors, Set<N> successors) {
    this.predecessors = checkNotNull(predecessors, "predecessors");
    this.successors = checkNotNull(successors, "successors");
  }

  static <N> DirectedNodeAdjacencies<N> of() {
    // TODO(user): Enable users to specify the expected number of neighbors of a new node.
    return new DirectedNodeAdjacencies<N>(Sets.<N>newHashSet(), Sets.<N>newHashSet());
  }

  static <N> DirectedNodeAdjacencies<N> ofImmutable(Set<N> predecessors, Set<N> successors) {
    return new DirectedNodeAdjacencies<N>(
        ImmutableSet.copyOf(predecessors), ImmutableSet.copyOf(successors));
  }

  @Override
  public Set<N> adjacentNodes() {
    return Sets.union(predecessors(), successors());
  }

  @Override
  public Set<N> predecessors() {
    return Collections.unmodifiableSet(predecessors);
  }

  @Override
  public Set<N> successors() {
    return Collections.unmodifiableSet(successors);
  }

  @Override
  public boolean removePredecessor(Object node) {
    checkNotNull(node, "node");
    return predecessors.remove(node);
  }

  @Override
  public boolean removeSuccessor(Object node) {
    checkNotNull(node, "node");
    return successors.remove(node);
  }

  @Override
  public boolean addPredecessor(N node) {
    checkNotNull(node, "node");
    return predecessors.add(node);
  }

  @Override
  public boolean addSuccessor(N node) {
    checkNotNull(node, "node");
    return successors.add(node);
  }

  // For now, hashCode() and equals() are unused by any graph implementation.
  @Override
  public int hashCode() {
    return Objects.hashCode(predecessors, successors);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof DirectedNodeAdjacencies) {
      DirectedNodeAdjacencies<?> that = (DirectedNodeAdjacencies<?>) object;
      return this.predecessors.equals(that.predecessors)
          && this.successors.equals(that.successors);
    }
    return false;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("predecessors", predecessors)
        .add("successors", successors)
        .toString();
  }
}
