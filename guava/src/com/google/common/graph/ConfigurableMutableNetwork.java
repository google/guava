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
   * Return {@code false} if {@code edge} already exists between {@code nodeA} and {@code nodeB},
   * and in the same direction.
   *
   * @throws IllegalArgumentException if an edge (other than {@code edge}) already
   *         exists from {@code nodeA} to {@code nodeB}, and this is not a multigraph.
   *         Also, if self-loops are not allowed, and {@code nodeA} is equal to {@code nodeB}.
   */
  @Override
  @CanIgnoreReturnValue
  public boolean addEdge(E edge, N nodeA, N nodeB) {
    checkNotNull(edge, "edge");
    checkNotNull(nodeA, "nodeA");
    checkNotNull(nodeB, "nodeB");
    checkArgument(allowsSelfLoops() || !nodeA.equals(nodeB), SELF_LOOPS_NOT_ALLOWED, nodeA);
    boolean containsN1 = containsNode(nodeA);
    boolean containsN2 = containsNode(nodeB);
    if (containsEdge(edge)) {
      checkArgument(containsN1 && containsN2 && edgesConnecting(nodeA, nodeB).contains(edge),
          REUSING_EDGE, edge, incidentNodes(edge), nodeA, nodeB);
      return false;
    } else if (!allowsParallelEdges()) {
      checkArgument(!(containsN1 && containsN2 && successors(nodeA).contains(nodeB)),
          ADDING_PARALLEL_EDGE, nodeA, nodeB);
    }
    if (!containsN1) {
      addNode(nodeA);
    }
    NodeConnections<N, E> connectionsN1 = nodeConnections.get(nodeA);
    connectionsN1.addOutEdge(edge, nodeB);
    if (!containsN2) {
      addNode(nodeB);
    }
    NodeConnections<N, E> connectionsN2 = nodeConnections.get(nodeB);
    connectionsN2.addInEdge(edge, nodeA);
    edgeToReferenceNode.put(edge, nodeA);
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
    N nodeA = edgeToReferenceNode.get(edge);
    if (nodeA == null) {
      return false;
    }
    N nodeB = nodeConnections.get(nodeA).oppositeNode(edge);
    nodeConnections.get(nodeA).removeOutEdge(edge);
    nodeConnections.get(nodeB).removeInEdge(edge);
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
