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
 * A class representing an origin node's adjacent nodes and incident edges in a directed graph.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
final class DirectedNodeConnections<N, E> implements NodeConnections<N, E> {
  private final Set<N> predecessors;
  private final Set<N> successors;
  private final Set<E> inEdges;
  private final Set<E> outEdges;

  private DirectedNodeConnections(Set<N> predecessors, Set<N> successors,
      Set<E> inEdges, Set<E> outEdges) {
    this.predecessors = checkNotNull(predecessors, "predecessors");
    this.successors = checkNotNull(successors, "successors");
    this.inEdges = checkNotNull(inEdges, "inEdges");
    this.outEdges = checkNotNull(outEdges, "outEdges");
  }

  static <N, E> DirectedNodeConnections<N, E> of() {
    // TODO(user): Enable users to specify the expected number of neighbors of a new node.
    return new DirectedNodeConnections<N, E>(
        Sets.<N>newHashSet(), Sets.<N>newHashSet(), Sets.<E>newHashSet(), Sets.<E>newHashSet());
  }

  static <N, E> DirectedNodeConnections<N, E> ofImmutable(Set<N> predecessors, Set<N> successors,
      Set<E> inEdges, Set<E> outEdges) {
    return new DirectedNodeConnections<N, E>(
        ImmutableSet.copyOf(predecessors), ImmutableSet.copyOf(successors),
        ImmutableSet.copyOf(inEdges), ImmutableSet.copyOf(outEdges));
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
  public Set<E> incidentEdges() {
    return Sets.union(inEdges(), outEdges());
  }

  @Override
  public Set<E> inEdges() {
    return Collections.unmodifiableSet(inEdges);
  }

  @Override
  public Set<E> outEdges() {
    return Collections.unmodifiableSet(outEdges);
  }

  @Override
  public void removeInEdge(Object edge) {
    checkNotNull(edge, "edge");
    inEdges.remove(edge);
  }

  @Override
  public void removeOutEdge(Object edge) {
    checkNotNull(edge, "edge");
    outEdges.remove(edge);
  }

  @Override
  public void removePredecessor(Object node) {
    checkNotNull(node, "node");
    predecessors.remove(node);
  }

  @Override
  public void removeSuccessor(Object node) {
    checkNotNull(node, "node");
    successors.remove(node);
  }

  @Override
  public void addPredecessor(N node, E edge) {
    checkNotNull(node, "node");
    checkNotNull(edge, "edge");
    predecessors.add(node);
    inEdges.add(edge);
  }

  @Override
  public void addSuccessor(N node, E edge) {
    checkNotNull(node, "node");
    checkNotNull(edge, "edge");
    successors.add(node);
    outEdges.add(edge);
  }

  // For now, hashCode() and equals() are unused by any graph implementation.
  // If needed, there may be room for optimization (e.g. only considering the edges).
  @Override
  public int hashCode() {
    return Objects.hashCode(predecessors, successors, inEdges, outEdges);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof DirectedNodeConnections) {
      DirectedNodeConnections<?, ?> that = (DirectedNodeConnections<?, ?>) object;
      return this.predecessors.equals(that.predecessors)
          && this.successors.equals(that.successors)
          && this.inEdges.equals(that.inEdges)
          && this.outEdges.equals(that.outEdges);
    }
    return false;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("predecessors", predecessors)
        .add("successors", successors)
        .add("inEdges", inEdges)
        .add("outEdges", outEdges)
        .toString();
  }
}
