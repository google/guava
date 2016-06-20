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
import static com.google.common.graph.GraphErrorMessageUtils.SELF_LOOPS_NOT_ALLOWED;

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
    nodeConnections.put(node, newNodeConnections());
    return true;
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
    checkArgument(allowsSelfLoops() || !nodeA.equals(nodeB), SELF_LOOPS_NOT_ALLOWED, nodeA);
    boolean containsA = containsNode(nodeA);
    boolean containsB = containsNode(nodeB);
    // TODO(user): does not support parallel edges
    if (containsA && containsB && nodeConnections.get(nodeA).successors().contains(nodeB)) {
      return false;
    }
    if (!containsA) {
      addNode(nodeA);
    }
    NodeAdjacencies<N> connectionsA = nodeConnections.get(nodeA);
    connectionsA.addSuccessor(nodeB);
    if (!containsB) {
      addNode(nodeB);
    }
    NodeAdjacencies<N> connectionsB = nodeConnections.get(nodeB);
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
    for (N successor : connections.successors()) {
      if (!node.equals(successor)) {
        // don't remove the successor if it's the input node (=> CME); will be removed below
        nodeConnections.get(successor).removePredecessor(node);
      }
    }
    for (N predecessor : connections.predecessors()) {
      nodeConnections.get(predecessor).removeSuccessor(node);
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
