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

import java.util.Set;

/**
 * Configurable implementation of {@link MutableBasicGraph} that supports both directed and
 * undirected graphs. Instances of this class should be constructed with {@link BasicGraphBuilder}.
 *
 * <p>Time complexities for mutation methods are all O(1) except for {@code removeNode(N node)},
 * which is in O(d_node) where d_node is the degree of {@code node}.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 */
final class ConfigurableMutableBasicGraph<N>
    extends AbstractBasicGraph<N> implements MutableBasicGraph<N> {
  private final MutableGraph<N, Presence> backingGraph;

  /**
   * Constructs a {@link MutableBasicGraph} with the properties specified in {@code builder}.
   */
  ConfigurableMutableBasicGraph(AbstractGraphBuilder<? super N> builder) {
    this.backingGraph = new ConfigurableMutableGraph<N, Presence>(builder);
  }

  @Override
  public Set<N> nodes() {
    return backingGraph.nodes();
  }

  @Override
  public Set<Endpoints<N>> edges() {
    return backingGraph.edges();
  }

  @Override
  public boolean isDirected() {
    return backingGraph.isDirected();
  }

  @Override
  public boolean allowsSelfLoops() {
    return backingGraph.allowsSelfLoops();
  }

  @Override
  public ElementOrder<N> nodeOrder() {
    return backingGraph.nodeOrder();
  }

  @Override
  public Set<N> adjacentNodes(Object node) {
    return backingGraph.adjacentNodes(node);
  }

  @Override
  public Set<N> predecessors(Object node) {
    return backingGraph.predecessors(node);
  }

  @Override
  public Set<N> successors(Object node) {
    return backingGraph.successors(node);
  }

  @Override
  public Presence edgeValue(Object nodeA, Object nodeB) {
    return backingGraph.edgeValue(nodeA, nodeB);
  }

  @Override
  public Presence edgeValueOrDefault(Object nodeA, Object nodeB, Presence defaultValue) {
    return backingGraph.edgeValueOrDefault(nodeA, nodeB, defaultValue);
  }

  @Override
  public boolean addNode(N node) {
    return backingGraph.addNode(node);
  }

  @Override
  public boolean putEdge(N nodeA, N nodeB) {
    return backingGraph.putEdgeValue(nodeA, nodeB, Presence.EDGE_EXISTS) == null;
  }

  @Override
  public boolean removeNode(Object node) {
    return backingGraph.removeNode(node);
  }

  @Override
  public boolean removeEdge(Object nodeA, Object nodeB) {
    return backingGraph.removeEdge(nodeA, nodeB) != null;
  }
}
