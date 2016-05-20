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
import static com.google.common.graph.GraphConstants.EXPECTED_DEGREE;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A class representing an origin node's adjacent nodes in a directed graph.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 */
final class DirectedNodeAdjacencies<N> implements NodeAdjacencies<N> {
  enum Adjacency {
    PRED,
    SUCC,
    BOTH;
  }

  private final Map<N, Adjacency> adjacentNodes;

  private int predecessorCount;
  private int successorCount;

  private DirectedNodeAdjacencies(
      Map<N, Adjacency> adjacentNodes, int predecessorCount, int successorCount) {
    this.adjacentNodes = checkNotNull(adjacentNodes, "adjacentNodes");
    this.predecessorCount = predecessorCount;
    this.successorCount = successorCount;
  }

  static <N> DirectedNodeAdjacencies<N> of() {
    return new DirectedNodeAdjacencies<N>(
        Maps.<N, Adjacency>newHashMapWithExpectedSize(EXPECTED_DEGREE), 0, 0);
  }

  static <N> DirectedNodeAdjacencies<N> ofImmutable(
      Map<N, Adjacency> adjacentNodes, int predecessorCount, int successorCount) {
    return new DirectedNodeAdjacencies<N>(
        ImmutableMap.copyOf(adjacentNodes), predecessorCount, successorCount);
  }

  @Override
  public Set<N> adjacentNodes() {
    return Collections.unmodifiableSet(adjacentNodes.keySet());
  }

  @Override
  public Set<N> predecessors() {
    return new AbstractSet<N>() {
      @Override
      public Iterator<N> iterator() {
        final Iterator<Entry<N, Adjacency>> entries = adjacentNodes.entrySet().iterator();
        return new AbstractIterator<N>() {
          @Override
          protected N computeNext() {
            while (entries.hasNext()) {
              Entry<N, Adjacency> entry = entries.next();
              if (isPredecessor(entry.getValue())) {
                return entry.getKey();
              }
            }
            return endOfData();
          }
        };
      }

      @Override
      public int size() {
        return predecessorCount;
      }

      @Override
      public boolean contains(Object obj) {
        return isPredecessor(adjacentNodes.get(obj));
      }
    };
  }

  @Override
  public Set<N> successors() {
    return new AbstractSet<N>() {
      @Override
      public Iterator<N> iterator() {
        final Iterator<Entry<N, Adjacency>> entries = adjacentNodes.entrySet().iterator();
        return new AbstractIterator<N>() {
          @Override
          protected N computeNext() {
            while (entries.hasNext()) {
              Entry<N, Adjacency> entry = entries.next();
              if (isSuccessor(entry.getValue())) {
                return entry.getKey();
              }
            }
            return endOfData();
          }
        };
      }

      @Override
      public int size() {
        return successorCount;
      }

      @Override
      public boolean contains(Object obj) {
        return isSuccessor(adjacentNodes.get(obj));
      }
    };
  }

  @SuppressWarnings("unchecked") // Safe because we only cast if node is a key of Map<N, Adjacency>
  @Override
  public void removePredecessor(Object node) {
    checkNotNull(node, "node");
    Adjacency adjacency = adjacentNodes.get(node);
    if (adjacency == Adjacency.BOTH) {
      adjacentNodes.put((N) node, Adjacency.SUCC);
      predecessorCount--;
    } else if (adjacency == Adjacency.PRED) {
      adjacentNodes.remove(node);
      predecessorCount--;
    }
    checkState(predecessorCount >= 0);
  }

  @SuppressWarnings("unchecked") // Safe because we only cast if node is a key of Map<N, Adjacency>
  @Override
  public void removeSuccessor(Object node) {
    checkNotNull(node, "node");
    Adjacency adjacency = adjacentNodes.get(node);
    if (adjacency == Adjacency.BOTH) {
      adjacentNodes.put((N) node, Adjacency.PRED);
      successorCount--;
    } else if (adjacency == Adjacency.SUCC) {
      adjacentNodes.remove(node);
      successorCount--;
    }
    checkState(successorCount >= 0);
  }

  @Override
  public void addPredecessor(N node) {
    checkNotNull(node, "node");
    Adjacency adjacency = adjacentNodes.get(node);
    if (adjacency == null) {
      adjacentNodes.put(node, Adjacency.PRED);
      predecessorCount++;
    } else if (adjacency == Adjacency.SUCC) {
      adjacentNodes.put(node, Adjacency.BOTH);
      predecessorCount++;
    }
    checkState(predecessorCount >= 1);
  }

  @Override
  public void addSuccessor(N node) {
    checkNotNull(node, "node");
    Adjacency adjacency = adjacentNodes.get(node);
    if (adjacency == null) {
      adjacentNodes.put(node, Adjacency.SUCC);
      successorCount++;
    } else if (adjacency == Adjacency.PRED) {
      adjacentNodes.put(node, Adjacency.BOTH);
      successorCount++;
    }
    checkState(successorCount >= 1);
  }

  private static boolean isPredecessor(@Nullable Adjacency adjacency) {
    return (adjacency == Adjacency.PRED || adjacency == Adjacency.BOTH);
  }

  private static boolean isSuccessor(@Nullable Adjacency adjacency) {
    return (adjacency == Adjacency.SUCC || adjacency == Adjacency.BOTH);
  }
}
