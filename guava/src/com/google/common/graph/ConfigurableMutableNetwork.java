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
import static com.google.common.graph.GraphErrorMessageUtils.ADDING_PARALLEL_EDGE;
import static com.google.common.graph.GraphErrorMessageUtils.REUSING_EDGE;
import static com.google.common.graph.GraphErrorMessageUtils.SELF_LOOPS_NOT_ALLOWED;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * Configurable implementation of {@link MutableNetwork} that supports both directed and undirected
 * graphs. Instances of this class should be constructed with {@link NetworkBuilder}.
 *
 * <p>Time complexities for mutation methods are all O(1) except for {@code removeNode(N node)},
 * which is in O(d_node) where d_node is the degree of {@code node}.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
// TODO(b/24620028): Enable this class to support sorted nodes/edges.
final class ConfigurableMutableNetwork<N, E>
    extends AbstractConfigurableNetwork<N, E> implements MutableNetwork<N, E> {

  /**
   * Constructs a mutable graph with the properties specified in {@code builder}.
   */
  ConfigurableMutableNetwork(NetworkBuilder<? super N, ? super E> builder) {
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
   * Add nodes that are not elements of the graph, then add {@code edge} between them.
   * Return {@code false} if {@code edge} already exists between {@code node1} and {@code node2},
   * and in the same direction.
   *
   * @throws IllegalArgumentException if an edge (other than {@code edge}) already
   *         exists from {@code node1} to {@code node2}, and this is not a multigraph.
   *         Also, if self-loops are not allowed, and {@code node1} is equal to {@code node2}.
   */
  @Override
  @CanIgnoreReturnValue
  public boolean addEdge(E edge, N node1, N node2) {
    checkNotNull(edge, "edge");
    checkNotNull(node1, "node1");
    checkNotNull(node2, "node2");
    checkArgument(allowsSelfLoops() || !node1.equals(node2), SELF_LOOPS_NOT_ALLOWED, node1);
    boolean containsN1 = containsNode(node1);
    boolean containsN2 = containsNode(node2);
    if (containsEdge(edge)) {
      checkArgument(containsN1 && containsN2 && edgesConnecting(node1, node2).contains(edge),
          REUSING_EDGE, edge, incidentNodes(edge), node1, node2);
      return false;
    } else if (!allowsParallelEdges()) {
      checkArgument(!(containsN1 && containsN2 && successors(node1).contains(node2)),
          ADDING_PARALLEL_EDGE, node1, node2);
    }
    if (!containsN1) {
      addNode(node1);
    }
    NodeConnections<N, E> connectionsN1 = nodeConnections.get(node1);
    connectionsN1.addOutEdge(edge, node2);
    if (!containsN2) {
      addNode(node2);
    }
    NodeConnections<N, E> connectionsN2 = nodeConnections.get(node2);
    connectionsN2.addInEdge(edge, node1);
    edgeToReferenceNode.put(edge, node1);
    return true;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean removeNode(Object node) {
    checkNotNull(node, "node");
    if (!containsNode(node)) {
      return false;
    }
    // Since views are returned, we need to copy the edges that will be removed.
    // Thus we avoid modifying the underlying view while iterating over it.
    for (E edge : ImmutableList.copyOf(incidentEdges(node))) {
      removeEdge(edge);
    }
    nodeConnections.remove(node);
    return true;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean removeEdge(Object edge) {
    checkNotNull(edge, "edge");
    N node1 = edgeToReferenceNode.get(edge);
    if (node1 == null) {
      return false;
    }
    N node2 = nodeConnections.get(node1).oppositeNode(edge);
    nodeConnections.get(node1).removeOutEdge(edge);
    nodeConnections.get(node2).removeInEdge(edge);
    edgeToReferenceNode.remove(edge);
    return true;
  }

  private NodeConnections<N, E> newNodeConnections() {
    return isDirected()
        ? allowsParallelEdges()
            ? DirectedMultiNodeConnections.<N, E>of()
            : DirectedNodeConnections.<N, E>of()
        : allowsParallelEdges()
            ? UndirectedMultiNodeConnections.<N, E>of()
            : UndirectedNodeConnections.<N, E>of();
  }
}
