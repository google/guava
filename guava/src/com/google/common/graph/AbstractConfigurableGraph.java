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
import static com.google.common.graph.GraphConstants.DEFAULT_NODE_COUNT;
import static com.google.common.graph.GraphConstants.NODE_NOT_IN_GRAPH;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Abstract configurable implementation of {@link Graph} that supports the options supplied
 * by {@link GraphBuilder}.
 *
 * <p>This class maintains a map of nodes to {@link NodeAdjacencies}.
 *
 * <p>{@code Set}-returning accessors return unmodifiable views: the view returned will reflect
 * changes to the graph (if the graph is mutable) but may not be modified by the user.
 * The behavior of the returned view is undefined in the following cases:
 * <ul>
 * <li>Removing the element on which the accessor is called (e.g.:
 *     <pre>{@code
 *     Set<N> adjacentNodes = adjacentNodes(node);
 *     graph.removeNode(node);}</pre>
 *     At this point, the contents of {@code adjacentNodes} are undefined.
 * </ul>
 *
 * <p>The time complexity of all {@code Set}-returning accessors is O(1), since views are returned.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 */
abstract class AbstractConfigurableGraph<N> extends AbstractGraph<N> {
  private final boolean isDirected;
  private final boolean allowsSelfLoops;
  private final ElementOrder<? super N> nodeOrder;

  protected final MapIteratorCache<N, NodeAdjacencies<N>> nodeConnections;

  /**
   * Constructs a graph with the properties specified in {@code builder}.
   */
  AbstractConfigurableGraph(GraphBuilder<? super N> builder) {
    this(builder, AbstractConfigurableGraph.<N>getNodeMapforBuilder(builder));
  }

  private static <N> Map<N, NodeAdjacencies<N>> getNodeMapforBuilder(
      GraphBuilder<? super N> builder) {
    int expectedNodeSize = builder.expectedNodeCount.or(DEFAULT_NODE_COUNT);
    switch (builder.nodeOrder.type()) {
        case UNORDERED:
          return Maps.newHashMapWithExpectedSize(expectedNodeSize);
        case INSERTION:
          return Maps.newLinkedHashMapWithExpectedSize(expectedNodeSize);
        case SORTED:
          return Maps.newTreeMap(builder.nodeOrder.comparator());
        default:
          throw new IllegalArgumentException("Unrecognized node ElementOrder type");
    }
  }

  /**
   * Constructs a graph with the properties specified in {@code builder}, initialized with
   * the given node map.
   */
  AbstractConfigurableGraph(GraphBuilder<? super N> builder,
      Map<N, NodeAdjacencies<N>> nodeConnections) {
    this.isDirected = builder.directed;
    this.allowsSelfLoops = builder.allowsSelfLoops;
    this.nodeOrder = builder.nodeOrder;
    this.nodeConnections = new MapRetrievalCache<N, NodeAdjacencies<N>>(nodeConnections);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The order of iteration for this set is determined by the {@code ElementOrder<N>} provided
   * to the {@code GraphBuilder} that was used to create this instance.
   * By default, that order is the order in which the nodes were added to the graph.
   */
  @Override
  public Set<N> nodes() {
    return nodeConnections.unmodifiableKeySet();
  }

  @Override
  public boolean isDirected() {
    return isDirected;
  }

  @Override
  public boolean allowsSelfLoops() {
    return allowsSelfLoops;
  }

  @Override
  public ElementOrder<? super N> nodeOrder() {
    return nodeOrder;
  }

  @Override
  public Set<N> adjacentNodes(Object node) {
    return checkedConnections(node).adjacentNodes();
  }

  @Override
  public Set<N> predecessors(Object node) {
    return checkedConnections(node).predecessors();
  }

  @Override
  public Set<N> successors(Object node) {
    return checkedConnections(node).successors();
  }

  protected final NodeAdjacencies<N> checkedConnections(Object node) {
    checkNotNull(node, "node");
    NodeAdjacencies<N> connections = nodeConnections.get(node);
    checkArgument(connections != null, NODE_NOT_IN_GRAPH, node);
    return connections;
  }

  protected final boolean containsNode(@Nullable Object node) {
    return nodeConnections.containsKey(node);
  }
}
