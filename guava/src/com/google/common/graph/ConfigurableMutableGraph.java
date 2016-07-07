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
 */
final class ConfigurableMutableGraph<N>
    extends AbstractConfigurableGraph<N> implements MutableGraph<N> {

  /**
   * Constructs a mutable graph with the properties specified in {@code builder}.
   */
  ConfigurableMutableGraph(GraphBuilder<? super N> builder) {
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
   * Adds {@code node} to the graph and returns the associated {@link NodeAdjacencies}.
   *
   * @throws IllegalStateException if {@code node} is already present
   */
  @CanIgnoreReturnValue
  private NodeAdjacencies<N> addNodeInternal(N node) {
    NodeAdjacencies<N> connections = newNodeConnections();
    checkState(nodeConnections.put(node, connections) == null);
    return connections;
  }

  /**
   * Add an edge between {@code nodeA} and {@code nodeB}; if these nodes are not already
   * present in this graph, then add them.
   * Return {@code false} if an edge already exists between {@code nodeA} and {@code nodeB},
   * and in the same direction.
   *
   * @throws IllegalArgumentException if self-loops are not allowed, and {@code nodeA} is equal to
   *     {@code nodeB}.
   */
  @Override
  @CanIgnoreReturnValue
  public boolean addEdge(N nodeA, N nodeB) {
    checkNotNull(nodeA, "nodeA");
    checkNotNull(nodeB, "nodeB");

    NodeAdjacencies<N> connectionsA = nodeConnections.get(nodeA);
    // TODO(b/28087289): does not support parallel edges
    if (connectionsA != null && connectionsA.successors().contains(nodeB)) {
      return false;
    }
    boolean isSelfLoop = nodeA.equals(nodeB);
    if (!allowsSelfLoops()) {
      checkArgument(!isSelfLoop, SELF_LOOPS_NOT_ALLOWED, nodeA);
    }

    if (connectionsA == null) {
      connectionsA = addNodeInternal(nodeA);
    }
    connectionsA.addSuccessor(nodeB);
    NodeAdjacencies<N> connectionsB = nodeConnections.get(nodeB);
    if (connectionsB == null) {
      connectionsB = addNodeInternal(nodeB);
    }
    connectionsB.addPredecessor(nodeA);
    return true;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean removeNode(Object node) {
    checkNotNull(node, "node");

    NodeAdjacencies<N> connections = nodeConnections.get(node);
    if (connections == null) {
      return false;
    }

    if (allowsSelfLoops()) {
      // Remove any potential self-loop first so we won't get CME while removing incident edges.
      connections.removeSuccessor(node);
      connections.removePredecessor(node);
    }
    for (N successor : connections.successors()) {
      nodeConnections.getWithoutCaching(successor).removePredecessor(node);
    }
    for (N predecessor : connections.predecessors()) {
      nodeConnections.getWithoutCaching(predecessor).removeSuccessor(node);
    }
    nodeConnections.remove(node);
    return true;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean removeEdge(Object nodeA, Object nodeB) {
    checkNotNull(nodeA, "nodeA");
    checkNotNull(nodeB, "nodeB");

    NodeAdjacencies<N> connectionsA = nodeConnections.get(nodeA);
    if (connectionsA == null || !connectionsA.successors().contains(nodeB)) {
      return false;
    }

    NodeAdjacencies<N> connectionsB = nodeConnections.get(nodeB);
    connectionsA.removeSuccessor(nodeB);
    connectionsB.removePredecessor(nodeA);
    return true;
  }

  private NodeAdjacencies<N> newNodeConnections() {
    return isDirected()
        ? DirectedNodeAdjacencies.<N>of()
        : UndirectedNodeAdjacencies.<N>of();
  }
}
