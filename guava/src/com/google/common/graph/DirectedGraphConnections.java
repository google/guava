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
import static com.google.common.graph.GraphConstants.INNER_CAPACITY;
import static com.google.common.graph.GraphConstants.INNER_LOAD_FACTOR;
import static com.google.common.graph.Graphs.checkNonNegative;
import static com.google.common.graph.Graphs.checkPositive;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An implementation of {@link GraphConnections} for directed graphs.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 */
final class DirectedGraphConnections<N, V> implements GraphConnections<N, V> {
  /**
   * A wrapper class to indicate a node is both a predecessor and successor while still providing
   * the successor value.
   */
  private static final class PredAndSucc {
    private final Object successorValue;

    PredAndSucc(Object successorValue) {
      this.successorValue = successorValue;
    }
  }

  private static final Object PRED = new Object();

  // Every value in this map must either be an instance of PredAndSucc with a successorValue of
  // type V, PRED (representing predecessor), or an instance of type V (representing successor).
  private final Map<N, Object> adjacentNodeValues;

  private int predecessorCount;
  private int successorCount;

  private DirectedGraphConnections(
      Map<N, Object> adjacentNodeValues, int predecessorCount, int successorCount) {
    this.adjacentNodeValues = checkNotNull(adjacentNodeValues);
    this.predecessorCount = checkNonNegative(predecessorCount);
    this.successorCount = checkNonNegative(successorCount);
    checkState(
        predecessorCount <= adjacentNodeValues.size()
            && successorCount <= adjacentNodeValues.size());
  }

  static <N, V> DirectedGraphConnections<N, V> of() {
    // We store predecessors and successors in the same map, so double the initial capacity.
    int initialCapacity = INNER_CAPACITY * 2;
    return new DirectedGraphConnections<>(
        new HashMap<N, Object>(initialCapacity, INNER_LOAD_FACTOR), 0, 0);
  }

  static <N, V> DirectedGraphConnections<N, V> ofImmutable(
      Set<N> predecessors, Map<N, V> successorValues) {
    Map<N, Object> adjacentNodeValues = new HashMap<>();
    adjacentNodeValues.putAll(successorValues);
    for (N predecessor : predecessors) {
      Object value = adjacentNodeValues.put(predecessor, PRED);
      if (value != null) {
        adjacentNodeValues.put(predecessor, new PredAndSucc(value));
      }
    }
    return new DirectedGraphConnections<>(
        ImmutableMap.copyOf(adjacentNodeValues), predecessors.size(), successorValues.size());
  }

  @Override
  public Set<N> adjacentNodes() {
    return Collections.unmodifiableSet(adjacentNodeValues.keySet());
  }

  @Override
  public Set<N> predecessors() {
    return new AbstractSet<N>() {
      @Override
      public UnmodifiableIterator<N> iterator() {
        final Iterator<Entry<N, Object>> entries = adjacentNodeValues.entrySet().iterator();
        return new AbstractIterator<N>() {
          @Override
          protected N computeNext() {
            while (entries.hasNext()) {
              Entry<N, Object> entry = entries.next();
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
      public boolean contains(@Nullable Object obj) {
        return isPredecessor(adjacentNodeValues.get(obj));
      }
    };
  }

  @Override
  public Set<N> successors() {
    return new AbstractSet<N>() {
      @Override
      public UnmodifiableIterator<N> iterator() {
        final Iterator<Entry<N, Object>> entries = adjacentNodeValues.entrySet().iterator();
        return new AbstractIterator<N>() {
          @Override
          protected N computeNext() {
            while (entries.hasNext()) {
              Entry<N, Object> entry = entries.next();
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
      public boolean contains(@Nullable Object obj) {
        return isSuccessor(adjacentNodeValues.get(obj));
      }
    };
  }

  @SuppressWarnings("unchecked")
  @Override
  public V value(N node) {
    Object value = adjacentNodeValues.get(node);
    if (value == PRED) {
      return null;
    }
    if (value instanceof PredAndSucc) {
      return (V) ((PredAndSucc) value).successorValue;
    }
    return (V) value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void removePredecessor(N node) {
    Object previousValue = adjacentNodeValues.get(node);
    if (previousValue == PRED) {
      adjacentNodeValues.remove(node);
      checkNonNegative(--predecessorCount);
    } else if (previousValue instanceof PredAndSucc) {
      adjacentNodeValues.put((N) node, ((PredAndSucc) previousValue).successorValue);
      checkNonNegative(--predecessorCount);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public V removeSuccessor(Object node) {
    Object previousValue = adjacentNodeValues.get(node);
    if (previousValue == null || previousValue == PRED) {
      return null;
    } else if (previousValue instanceof PredAndSucc) {
      adjacentNodeValues.put((N) node, PRED);
      checkNonNegative(--successorCount);
      return (V) ((PredAndSucc) previousValue).successorValue;
    } else { // successor
      adjacentNodeValues.remove(node);
      checkNonNegative(--successorCount);
      return (V) previousValue;
    }
  }

  @Override
  public void addPredecessor(N node, V unused) {
    Object previousValue = adjacentNodeValues.put(node, PRED);
    if (previousValue == null) {
      checkPositive(++predecessorCount);
    } else if (previousValue instanceof PredAndSucc) {
      // Restore previous PredAndSucc object.
      adjacentNodeValues.put(node, previousValue);
    } else if (previousValue != PRED) { // successor
      // Do NOT use method parameter value 'unused'. In directed graphs, successors store the value.
      adjacentNodeValues.put(node, new PredAndSucc(previousValue));
      checkPositive(++predecessorCount);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public V addSuccessor(N node, V value) {
    Object previousValue = adjacentNodeValues.put(node, value);
    if (previousValue == null) {
      checkPositive(++successorCount);
      return null;
    } else if (previousValue instanceof PredAndSucc) {
      adjacentNodeValues.put(node, new PredAndSucc(value));
      return (V) ((PredAndSucc) previousValue).successorValue;
    } else if (previousValue == PRED) {
      adjacentNodeValues.put(node, new PredAndSucc(value));
      checkPositive(++successorCount);
      return null;
    } else { // successor
      return (V) previousValue;
    }
  }

  private static boolean isPredecessor(@Nullable Object value) {
    return (value == PRED) || (value instanceof PredAndSucc);
  }

  private static boolean isSuccessor(@Nullable Object value) {
    return (value != PRED) && (value != null);
  }
}
