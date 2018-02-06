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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.graph.Graphs.checkNonNegative;
import static com.google.common.graph.Graphs.checkPositive;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.math.IntMath;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * A base implementation of {@link NetworkConnections} for directed networks.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
abstract class AbstractDirectedNetworkConnections<N, E> implements NetworkConnections<N, E> {
  /** Keys are edges incoming to the origin node, values are the source node. */
  protected final Map<E, N> inEdgeMap;

  /** Keys are edges outgoing from the origin node, values are the target node. */
  protected final Map<E, N> outEdgeMap;

  private int selfLoopCount;

  protected AbstractDirectedNetworkConnections(
      Map<E, N> inEdgeMap, Map<E, N> outEdgeMap, int selfLoopCount) {
    this.inEdgeMap = checkNotNull(inEdgeMap);
    this.outEdgeMap = checkNotNull(outEdgeMap);
    this.selfLoopCount = checkNonNegative(selfLoopCount);
    checkState(selfLoopCount <= inEdgeMap.size() && selfLoopCount <= outEdgeMap.size());
  }

  @Override
  public Set<N> adjacentNodes() {
    return Sets.union(predecessors(), successors());
  }

  @Override
  public Set<E> incidentEdges() {
    return new AbstractSet<E>() {
      @Override
      public UnmodifiableIterator<E> iterator() {
        Iterable<E> incidentEdges =
            (selfLoopCount == 0)
                ? Iterables.concat(inEdgeMap.keySet(), outEdgeMap.keySet())
                : Sets.union(inEdgeMap.keySet(), outEdgeMap.keySet());
        return Iterators.unmodifiableIterator(incidentEdges.iterator());
      }

      @Override
      public int size() {
        return IntMath.saturatedAdd(inEdgeMap.size(), outEdgeMap.size() - selfLoopCount);
      }

      @Override
      public boolean contains(@NullableDecl Object obj) {
        return inEdgeMap.containsKey(obj) || outEdgeMap.containsKey(obj);
      }
    };
  }

  @Override
  public Set<E> inEdges() {
    return Collections.unmodifiableSet(inEdgeMap.keySet());
  }

  @Override
  public Set<E> outEdges() {
    return Collections.unmodifiableSet(outEdgeMap.keySet());
  }

  @Override
  public N adjacentNode(E edge) {
    // Since the reference node is defined to be 'source' for directed graphs,
    // we can assume this edge lives in the set of outgoing edges.
    return checkNotNull(outEdgeMap.get(edge));
  }

  @Override
  public N removeInEdge(E edge, boolean isSelfLoop) {
    if (isSelfLoop) {
      checkNonNegative(--selfLoopCount);
    }
    N previousNode = inEdgeMap.remove(edge);
    return checkNotNull(previousNode);
  }

  @Override
  public N removeOutEdge(E edge) {
    N previousNode = outEdgeMap.remove(edge);
    return checkNotNull(previousNode);
  }

  @Override
  public void addInEdge(E edge, N node, boolean isSelfLoop) {
    if (isSelfLoop) {
      checkPositive(++selfLoopCount);
    }
    N previousNode = inEdgeMap.put(edge, node);
    checkState(previousNode == null);
  }

  @Override
  public void addOutEdge(E edge, N node) {
    N previousNode = outEdgeMap.put(edge, node);
    checkState(previousNode == null);
  }
}
