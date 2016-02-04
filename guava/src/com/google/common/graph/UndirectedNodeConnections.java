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
 * A class representing an origin node's adjacent nodes and incident edges in an undirected graph.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
final class UndirectedNodeConnections<N, E> implements NodeConnections<N, E> {
  private final Set<N> adjacentNodes;
  private final Set<E> incidentEdges;

  private UndirectedNodeConnections(Set<N> adjacentNodes, Set<E> incidentEdges) {
    this.adjacentNodes = checkNotNull(adjacentNodes, "adjacentNodes");
    this.incidentEdges = checkNotNull(incidentEdges, "incidentEdges");
  }

  static <N, E> UndirectedNodeConnections<N, E> of() {
    // TODO(user): Enable users to specify the expected number of neighbors of a new node.
    return new UndirectedNodeConnections<N, E>(Sets.<N>newHashSet(), Sets.<E>newHashSet());
  }

  static <N, E> UndirectedNodeConnections<N, E> ofImmutable(
      Set<N> adjacentNodes, Set<E> incidentEdges) {
    return new UndirectedNodeConnections<N, E>(
        ImmutableSet.copyOf(adjacentNodes), ImmutableSet.copyOf(incidentEdges));
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
  public Set<E> incidentEdges() {
    return Collections.unmodifiableSet(incidentEdges);
  }

  @Override
  public Set<E> inEdges() {
    return incidentEdges();
  }

  @Override
  public Set<E> outEdges() {
    return incidentEdges();
  }

  @Override
  public void removeInEdge(Object edge) {
    removeOutEdge(edge);
  }

  @Override
  public void removeOutEdge(Object edge) {
    checkNotNull(edge, "edge");
    incidentEdges.remove(edge);
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
  public void addPredecessor(N node, E edge) {
    addSuccessor(node, edge);
  }

  @Override
  public void addSuccessor(N node, E edge) {
    checkNotNull(node, "node");
    checkNotNull(edge, "edge");
    adjacentNodes.add(node);
    incidentEdges.add(edge);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(adjacentNodes, incidentEdges);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof UndirectedNodeConnections) {
      UndirectedNodeConnections<?, ?> that = (UndirectedNodeConnections<?, ?>) object;
      return this.adjacentNodes.equals(that.adjacentNodes)
          && this.incidentEdges.equals(that.incidentEdges);
    }
    return false;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("adjacentNodes", adjacentNodes)
        .add("incidentEdges", incidentEdges)
        .toString();
  }
}
