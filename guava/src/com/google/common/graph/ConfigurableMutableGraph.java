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
// TODO(b/24620028): Enable this class to support sorted nodes/edges.
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
   * Add an edge between {@code node1} and {@code node2}; if these nodes are not already
   * present in this graph, then add them.
   * Return {@code false} if an edge already exists between {@code node1} and {@code node2},
   * and in the same direction.
   *
   * @throws IllegalArgumentException if self-loops are not allowed, and {@code node1} is equal to
   *     {@code node2}.
   */
  @Override
  @CanIgnoreReturnValue
  public boolean addEdge(N node1, N node2) {
    checkNotNull(node1, "node1");
    checkNotNull(node2, "node2");
    checkArgument(allowsSelfLoops() || !node1.equals(node2), SELF_LOOPS_NOT_ALLOWED, node1);
    boolean containsN1 = containsNode(node1);
    boolean containsN2 = containsNode(node2);
    // TODO(user): does not support parallel edges
    if (containsN1 && containsN2 && nodeConnections.get(node1).successors().contains(node2)) {
      return false;
    }
    if (!containsN1) {
      addNode(node1);
    }
    NodeAdjacencies<N> connectionsN1 = nodeConnections.get(node1);
    connectionsN1.addSuccessor(node2);
    if (!containsN2) {
      addNode(node2);
    }
    NodeAdjacencies<N> connectionsN2 = nodeConnections.get(node2);
    connectionsN2.addPredecessor(node1);
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
  public boolean removeEdge(Object node1, Object node2) {
    checkNotNull(node1, "node1");
    checkNotNull(node2, "node2");
    NodeAdjacencies<N> connectionsN1 = nodeConnections.get(node1);
    if (connectionsN1 == null || !connectionsN1.successors().contains(node2)) {
      return false;
    }
    NodeAdjacencies<N> connectionsN2 = nodeConnections.get(node2);
    connectionsN1.removeSuccessor(node2);
    connectionsN2.removePredecessor(node1);
    return true;
  }

  private NodeAdjacencies<N> newNodeConnections() {
    return isDirected()
        ? DirectedNodeAdjacencies.<N>of()
        : UndirectedNodeAdjacencies.<N>of();
  }
}
