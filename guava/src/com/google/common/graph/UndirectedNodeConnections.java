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

import static com.google.common.graph.GraphConstants.EXPECTED_DEGREE;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of {@link NodeConnections} for undirected networks.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
final class UndirectedNodeConnections<N, E> extends AbstractUndirectedNodeConnections<N, E> {

  protected UndirectedNodeConnections(Map<E, N> incidentEdgeMap) {
    super(incidentEdgeMap);
  }

  static <N, E> UndirectedNodeConnections<N, E> of() {
    return new UndirectedNodeConnections<N, E>(HashBiMap.<E, N>create(EXPECTED_DEGREE));
  }

  static <N, E> UndirectedNodeConnections<N, E> ofImmutable(Map<E, N> incidentEdges) {
    return new UndirectedNodeConnections<N, E>(ImmutableBiMap.copyOf(incidentEdges));
  }

  @Override
  public Set<N> adjacentNodes() {
    return Collections.unmodifiableSet(((BiMap<E, N>) incidentEdgeMap).values());
  }

  @Override
  public Set<E> edgesConnecting(final Object node) {
    return new AbstractSet<E>() {
      @Override
      public Iterator<E> iterator() {
        Map<N, E> adjacentNodes = ((BiMap<E, N>) incidentEdgeMap).inverse();
        E connectingEdge = adjacentNodes.get(node);
        if (connectingEdge == null) {
          return ImmutableSet.<E>of().iterator();
        }
        return Iterators.singletonIterator(connectingEdge);
      }

      @Override
      public int size() {
        return adjacentNodes().contains(node) ? 1 : 0;
      }
    };
  }
}
