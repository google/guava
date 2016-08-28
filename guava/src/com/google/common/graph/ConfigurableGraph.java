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
import static com.google.common.graph.GraphConstants.EDGE_CONNECTING_NOT_IN_GRAPH;
import static com.google.common.graph.GraphConstants.NODE_NOT_IN_GRAPH;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nullable;

/**
 * Configurable implementation of {@link Graph} that supports the options supplied by
 * {@link AbstractGraphBuilder}.
 *
 * <p>This class maintains a map of nodes to {@link GraphConnections}.
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
 * @param <V> Value parameter type
 */
class ConfigurableGraph<N, V> extends AbstractGraph<N, V> {
  private final boolean isDirected;
  private final boolean allowsSelfLoops;
  private final ElementOrder<N> nodeOrder;

  protected final MapIteratorCache<N, GraphConnections<N, V>> nodeConnections;

  /**
   * Constructs a graph with the properties specified in {@code builder}.
   */
  ConfigurableGraph(AbstractGraphBuilder<? super N> builder) {
    this(
        builder,
        builder.nodeOrder.<N, GraphConnections<N, V>>createMap(
            builder.expectedNodeCount.or(DEFAULT_NODE_COUNT)));
  }

  /**
   * Constructs a graph with the properties specified in {@code builder}, initialized with
   * the given node map.
   */
  ConfigurableGraph(AbstractGraphBuilder<? super N> builder,
      Map<N, GraphConnections<N, V>> nodeConnections) {
    this.isDirected = builder.directed;
    this.allowsSelfLoops = builder.allowsSelfLoops;
    this.nodeOrder = builder.nodeOrder.cast();
    // Prefer the heavier "MapRetrievalCache" for nodes if lookup is expensive.
    this.nodeConnections = (nodeConnections instanceof TreeMap)
        ? new MapRetrievalCache<N, GraphConnections<N, V>>(nodeConnections)
        : new MapIteratorCache<N, GraphConnections<N, V>>(nodeConnections);
  }

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
  public ElementOrder<N> nodeOrder() {
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

  @Override
  public V edgeValue(Object nodeA, Object nodeB) {
    V value = edgeValueOrDefault(nodeA, nodeB, null);
    checkArgument(value != null, EDGE_CONNECTING_NOT_IN_GRAPH, nodeA, nodeB);
    return value;
  }

  @Override
  public V edgeValueOrDefault(Object nodeA, Object nodeB, @Nullable V defaultValue) {
    V value = checkedConnections(nodeA).value(nodeB);
    if (value == null) {
      checkArgument(containsNode(nodeB), NODE_NOT_IN_GRAPH, nodeB);
      return defaultValue;
    }
    return value;
  }

  protected final GraphConnections<N, V> checkedConnections(Object node) {
    checkNotNull(node, "node");
    GraphConnections<N, V> connections = nodeConnections.get(node);
    checkArgument(connections != null, NODE_NOT_IN_GRAPH, node);
    return connections;
  }

  protected final boolean containsNode(@Nullable Object node) {
    return nodeConnections.containsKey(node);
  }
}
