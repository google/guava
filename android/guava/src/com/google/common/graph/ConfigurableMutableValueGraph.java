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
 * Configurable implementation of {@link MutableValueGraph} that supports both directed and
 * undirected graphs. Instances of this class should be constructed with {@link ValueGraphBuilder}.
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
final class ConfigurableMutableValueGraph<N, V> extends ConfigurableValueGraph<N, V>
    implements MutableValueGraph<N, V> {

  /** Constructs a mutable graph with the properties specified in {@code builder}. */
  ConfigurableMutableValueGraph(AbstractGraphBuilder<? super N> builder) {
    super(builder);
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
  public V putEdgeValue(N nodeU, N nodeV, V value) {
    checkNotNull(nodeU, "nodeU");
    checkNotNull(nodeV, "nodeV");
    checkNotNull(value, "value");

    if (!allowsSelfLoops()) {
      checkArgument(!nodeU.equals(nodeV), SELF_LOOPS_NOT_ALLOWED, nodeU);
    }

    GraphConnections<N, V> connectionsU = nodeConnections.get(nodeU);
    if (connectionsU == null) {
      connectionsU = addNodeInternal(nodeU);
    }
    V previousValue = connectionsU.addSuccessor(nodeV, value);
    GraphConnections<N, V> connectionsV = nodeConnections.get(nodeV);
    if (connectionsV == null) {
      connectionsV = addNodeInternal(nodeV);
    }
    connectionsV.addPredecessor(nodeU, value);
    if (previousValue == null) {
      checkPositive(++edgeCount);
    }
    return previousValue;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean removeNode(N node) {
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
  public V removeEdge(N nodeU, N nodeV) {
    checkNotNull(nodeU, "nodeU");
    checkNotNull(nodeV, "nodeV");

    GraphConnections<N, V> connectionsU = nodeConnections.get(nodeU);
    GraphConnections<N, V> connectionsV = nodeConnections.get(nodeV);
    if (connectionsU == null || connectionsV == null) {
      return null;
    }

    V previousValue = connectionsU.removeSuccessor(nodeV);
    if (previousValue != null) {
      connectionsV.removePredecessor(nodeU);
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
