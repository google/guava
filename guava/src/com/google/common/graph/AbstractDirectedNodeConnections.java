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

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.math.IntMath;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A base implementation of {@link NodeConnections} for directed networks.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
abstract class AbstractDirectedNodeConnections<N, E> implements NodeConnections<N, E> {
  /**
   * Keys are edges incoming to the origin node, values are the source node.
   */
  protected final Map<E, N> inEdgeMap;

  /**
   * Keys are edges outgoing from the origin node, values are the target node.
   */
  protected final Map<E, N> outEdgeMap;

  private int selfLoopCount;

  protected AbstractDirectedNodeConnections(Map<E, N> inEdgeMap, Map<E, N> outEdgeMap,
      int selfLoopCount) {
    this.inEdgeMap = checkNotNull(inEdgeMap, "inEdgeMap");
    this.outEdgeMap = checkNotNull(outEdgeMap, "outEdgeMap");
    this.selfLoopCount = selfLoopCount;
  }

  @Override
  public Set<N> adjacentNodes() {
    return Sets.union(predecessors(), successors());
  }

  @Override
  public Set<E> incidentEdges() {
    return new AbstractSet<E>() {
      @Override
      public Iterator<E> iterator() {
        return selfLoopCount == 0
            ? Iterables.concat(inEdges(), outEdges()).iterator()
            : Sets.union(inEdges(), outEdges()).iterator();
      }

      @Override
      public int size() {
        return IntMath.saturatedAdd(inEdgeMap.size() - selfLoopCount, outEdgeMap.size());
      }

      @Override
      public boolean contains(Object obj) {
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
  public N oppositeNode(Object edge) {
    // Since the reference node is defined to be 'source' for directed graphs,
    // we can assume this edge lives in the set of outgoing edges.
    return checkNotNull(outEdgeMap.get(edge));
  }

  @Override
  public N removeInEdge(Object edge, boolean isSelfLoop) {
    checkNotNull(edge, "edge");
    if (isSelfLoop) {
      selfLoopCount--;
      checkState(selfLoopCount >= 0);
    }
    return inEdgeMap.remove(edge);
  }

  @Override
  public N removeOutEdge(Object edge) {
    checkNotNull(edge, "edge");
    return outEdgeMap.remove(edge);
  }

  @Override
  public boolean addInEdge(E edge, N node, boolean isSelfLoop) {
    checkNotNull(edge, "edge");
    checkNotNull(node, "node");
    if (isSelfLoop) {
      selfLoopCount++;
      checkState(selfLoopCount >= 1);
    }
    N previousNode = inEdgeMap.put(edge, node);
    if (previousNode != null) {
      checkState(node.equals(previousNode));
      return false;
    }
    return true;
  }

  @Override
  public boolean addOutEdge(E edge, N node) {
    checkNotNull(edge, "edge");
    checkNotNull(node, "node");
    N previousNode = outEdgeMap.put(edge, node);
    if (previousNode != null) {
      checkState(node.equals(previousNode));
      return false;
    }
    return true;
  }
}
