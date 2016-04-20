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
 * An implementation of {@link NodeConnections} for directed networks with parallel edges.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
final class DirectedMultiNodeConnections<N, E> extends AbstractDirectedNodeConnections<N, E> {

  private DirectedMultiNodeConnections(Map<E, N> inEdges, Map<E, N> outEdges) {
    super(inEdges, outEdges);
  }

  static <N, E> DirectedMultiNodeConnections<N, E> of() {
    return new DirectedMultiNodeConnections<N, E>(
        Maps.<E, N>newHashMapWithExpectedSize(EXPECTED_DEGREE),
        Maps.<E, N>newHashMapWithExpectedSize(EXPECTED_DEGREE));
  }

  static <N, E> DirectedMultiNodeConnections<N, E> ofImmutable(
      Map<E, N> inEdges, Map<E, N> outEdges) {
    return new DirectedMultiNodeConnections<N, E>(
        ImmutableMap.copyOf(inEdges), ImmutableMap.copyOf(outEdges));
  }

  private transient Reference<Multiset<N>> predecessorsReference;

  @Override
  public Set<N> predecessors() {
    Multiset<N> predecessors = getReference(predecessorsReference);
    if (predecessors == null) {
      predecessors = HashMultiset.create(inEdgeMap.values());
      predecessorsReference = new SoftReference<Multiset<N>>(predecessors);
    }
    return Collections.unmodifiableSet(predecessors.elementSet());
  }

  private transient Reference<Multiset<N>> successorsReference;

  @Override
  public Set<N> successors() {
    Multiset<N> successors = getReference(successorsReference);
    if (successors == null) {
      successors = HashMultiset.create(outEdgeMap.values());
      successorsReference = new SoftReference<Multiset<N>>(successors);
    }
    return Collections.unmodifiableSet(successors.elementSet());
  }

  @Override
  public Set<E> edgesConnecting(final Object node) {
    return Collections.unmodifiableSet(
        Maps.filterEntries(outEdgeMap, new Predicate<Entry<E, N>>() {
          @Override
          public boolean apply(Entry<E, N> entry) {
            return entry.getValue().equals(node);
          }
        }).keySet());
  }

  @Override
  public N removeInEdge(Object edge) {
    N node = super.removeInEdge(edge);
    if (node != null) {
      Multiset<N> predecessors = getReference(predecessorsReference);
      if (predecessors != null) {
        checkState(predecessors.remove(node));
      }
    }
    return node;
  }

  @Override
  public N removeOutEdge(Object edge) {
    N node = super.removeOutEdge(edge);
    if (node != null) {
      Multiset<N> successors = getReference(successorsReference);
      if (successors != null) {
        checkState(successors.remove(node));
      }
    }
    return node;
  }

  @Override
  public boolean addInEdge(E edge, N node) {
    if (super.addInEdge(edge, node)) {
      Multiset<N> predecessors = getReference(predecessorsReference);
      if (predecessors != null) {
        checkState(predecessors.add(node));
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean addOutEdge(E edge, N node) {
    if (super.addOutEdge(edge, node)) {
      Multiset<N> successors = getReference(successorsReference);
      if (successors != null) {
        checkState(successors.add(node));
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
