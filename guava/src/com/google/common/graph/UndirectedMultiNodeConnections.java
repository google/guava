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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.graph.GraphConstants.EXPECTED_DEGREE;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * An implementation of {@link NodeConnections} for undirected networks with parallel edges.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
final class UndirectedMultiNodeConnections<N, E> extends AbstractUndirectedNodeConnections<N, E> {

  private UndirectedMultiNodeConnections(Map<E, N> incidentEdges) {
    super(incidentEdges);
  }

  static <N, E> UndirectedMultiNodeConnections<N, E> of() {
    return new UndirectedMultiNodeConnections<N, E>(
        Maps.<E, N>newHashMapWithExpectedSize(EXPECTED_DEGREE));
  }

  static <N, E> UndirectedMultiNodeConnections<N, E> ofImmutable(Map<E, N> incidentEdges) {
    return new UndirectedMultiNodeConnections<N, E>(ImmutableMap.copyOf(incidentEdges));
  }

  @Override
  public Set<E> edgesConnecting(final Object node) {
    return Collections.unmodifiableSet(
        Maps.filterEntries(incidentEdgeMap, new Predicate<Entry<E, N>>() {
          @Override
          public boolean apply(Entry<E, N> entry) {
            return entry.getValue().equals(node);
          }
        }).keySet());
  }

  private transient Reference<Multiset<N>> adjacentNodesReference;

  @Override
  public Set<N> adjacentNodes() {
    Multiset<N> adjacentNodes = getReference(adjacentNodesReference);
    if (adjacentNodes == null) {
      adjacentNodes = HashMultiset.create(incidentEdgeMap.values());
      adjacentNodesReference = new SoftReference<Multiset<N>>(adjacentNodes);
    }
    return Collections.unmodifiableSet(adjacentNodes.elementSet());
  }

  @Override
  public N removeInEdge(Object edge) {
    return removeOutEdge(edge);
  }

  @Override
  public N removeOutEdge(Object edge) {
    N node = super.removeOutEdge(edge);
    if (node != null) {
      Multiset<N> adjacentNodes = getReference(adjacentNodesReference);
      if (adjacentNodes != null) {
        checkState(adjacentNodes.remove(node));
      }
    }
    return node;
  }

  @Override
  public boolean addInEdge(E edge, N node) {
    return addOutEdge(edge, node);
  }

  @Override
  public boolean addOutEdge(E edge, N node) {
    if (super.addOutEdge(edge, node)) {
      Multiset<N> adjacentNodes = getReference(adjacentNodesReference);
      if (adjacentNodes != null) {
        checkState(adjacentNodes.add(node));
      }
      return true;
    }
    return false;
  }

  // TODO(user): Move to NodeConnections interface once on Java 8
  @Nullable private static <T> T getReference(@Nullable Reference<T> reference) {
    if (reference == null) {
      return null;
    }
    return reference.get(); // can be null
  }
}
