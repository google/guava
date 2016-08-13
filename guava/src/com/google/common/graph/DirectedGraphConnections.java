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
import static com.google.common.graph.GraphConstants.INNER_CAPACITY;
import static com.google.common.graph.GraphConstants.INNER_LOAD_FACTOR;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of {@link GraphConnections} for directed graphs.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 */
final class DirectedGraphConnections<N, V> implements GraphConnections<N, V> {
  private final Set<N> predecessors;
  private final Map<N, V> successorValues;

  private DirectedGraphConnections(Set<N> predecessors, Map<N, V> successorValues) {
    this.predecessors = checkNotNull(predecessors, "predecessors");
    this.successorValues = checkNotNull(successorValues, "successorValues");
  }

  static <N, V> DirectedGraphConnections<N, V> of() {
    return new DirectedGraphConnections<N, V>(
        new HashSet<N>(INNER_CAPACITY, INNER_LOAD_FACTOR),
        new HashMap<N, V>(INNER_CAPACITY, INNER_LOAD_FACTOR));
  }

  static <N, V> DirectedGraphConnections<N, V> ofImmutable(
      Set<N> predecessors, Map<N, V> successorValues) {
    return new DirectedGraphConnections<N, V>(
        ImmutableSet.copyOf(predecessors), ImmutableMap.copyOf(successorValues));
  }

  @Override
  public Set<N> adjacentNodes() {
    return Sets.union(predecessors, successorValues.keySet());
  }

  @Override
  public Set<N> predecessors() {
    return Collections.unmodifiableSet(predecessors);
  }

  @Override
  public Set<N> successors() {
    return Collections.unmodifiableSet(successorValues.keySet());
  }

  @Override
  public V value(Object node) {
    return successorValues.get(node);
  }

  @Override
  public void removePredecessor(Object node) {
    predecessors.remove(node);
  }

  @Override
  public V removeSuccessor(Object node) {
    return successorValues.remove(node);
  }

  @Override
  public void addPredecessor(N node, V unused) {
    predecessors.add(node);
  }

  @Override
  public V addSuccessor(N node, V value) {
    return successorValues.put(node, value);
  }
}
