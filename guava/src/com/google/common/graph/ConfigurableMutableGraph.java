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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.graph.GraphConstants.SELF_LOOPS_NOT_ALLOWED;
import static com.google.common.graph.Graphs.checkNonNegative;
import static com.google.common.graph.Graphs.checkPositive;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * Configurable implementation of {@link MutableGraph} that supports both directed and undirected
 * graphs. Instances of this class should be constructed with {@link GraphBuilder}.
 *
 * <p>Time complexities for mutation methods are all O(1) except for {@code removeNode(N node)},
 * which is in O(d_node) where d_node is the degree of {@code node}.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 */
final class ConfigurableMutableGraph<N, V>
    extends ConfigurableGraph<N, V> implements MutableGraph<N, V> {
  private long edgeCount = 0L; // must be updated when edges are added or removed

  /**
   * Constructs a mutable graph with the properties specified in {@code builder}.
   */
  ConfigurableMutableGraph(AbstractGraphBuilder<? super N> builder) {
    super(builder);
  }

  @Override
  protected long edgeCount() {
    return edgeCount;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean addNode(N node) {
    checkNotNull(node, "node");

    if (containsNode(node)) {
      return false;
    }

    addNodeInternal(node);
    return true;
  }

  /**
   * Adds {@code node} to the graph and returns the associated {@link GraphConnections}.
   *
   * @throws IllegalStateException if {@code node} is already present
   */
  @CanIgnoreReturnValue
  private GraphConnections<N, V> addNodeInternal(N node) {
    GraphConnections<N, V> connections = newConnections();
    checkState(nodeConnections.put(node, connections) == null);
    return connections;
  }

  @Override
  @CanIgnoreReturnValue
  public V putEdgeValue(N nodeA, N nodeB, V value) {
    checkNotNull(nodeA, "nodeA");
    checkNotNull(nodeB, "nodeB");
    checkNotNull(value, "value");

    GraphConnections<N, V> connectionsA = nodeConnections.get(nodeA);
    boolean isSelfLoop = nodeA.equals(nodeB);
    if (!allowsSelfLoops()) {
      checkArgument(!isSelfLoop, SELF_LOOPS_NOT_ALLOWED, nodeA);
    }

    if (connectionsA == null) {
      connectionsA = addNodeInternal(nodeA);
    }
    V previousValue = connectionsA.addSuccessor(nodeB, value);
    GraphConnections<N, V> connectionsB = nodeConnections.get(nodeB);
    if (connectionsB == null) {
      connectionsB = addNodeInternal(nodeB);
    }
    connectionsB.addPredecessor(nodeA, value);
    if (previousValue == null) {
      checkPositive(++edgeCount);
    }
    return previousValue;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean removeNode(Object node) {
    checkNotNull(node, "node");

    GraphConnections<N, V> connections = nodeConnections.get(node);
    if (connections == null) {
      return false;
    }

    if (allowsSelfLoops()) {
      // Remove self-loop (if any) first, so we don't get CME while removing incident edges.
      if (connections.removeSuccessor(node) != null) {
        connections.removePredecessor(node);
        --edgeCount;
      }
    }

    for (N successor : connections.successors()) {
      nodeConnections.getWithoutCaching(successor).removePredecessor(node);
      --edgeCount;
    }
    if (isDirected()) { // In undirected graphs, the successor and predecessor sets are equal.
      for (N predecessor : connections.predecessors()) {
        checkState(nodeConnections.getWithoutCaching(predecessor).removeSuccessor(node) != null);
        --edgeCount;
      }
    }
    nodeConnections.remove(node);
    checkNonNegative(edgeCount);
    return true;
  }

  @Override
  @CanIgnoreReturnValue
  public V removeEdge(Object nodeA, Object nodeB) {
    checkNotNull(nodeA, "nodeA");
    checkNotNull(nodeB, "nodeB");

    GraphConnections<N, V> connectionsA = nodeConnections.get(nodeA);
    GraphConnections<N, V> connectionsB = nodeConnections.get(nodeB);
    if (connectionsA == null || connectionsB == null) {
      return null;
    }

    V previousValue = connectionsA.removeSuccessor(nodeB);
    if (previousValue != null) {
      connectionsB.removePredecessor(nodeA);
      checkNonNegative(--edgeCount);
    }
    return previousValue;
  }

  private GraphConnections<N, V> newConnections() {
    return isDirected()
        ? DirectedGraphConnections.<N, V>of()
        : UndirectedGraphConnections.<N, V>of();
  }
}
