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
import static com.google.common.graph.GraphErrorMessageUtils.NODE_NOT_IN_GRAPH;

import com.google.common.collect.Maps;

import java.util.Collections;
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
// TODO(b/24620028): Enable this class to support sorted nodes/edges.
abstract class AbstractConfigurableGraph<N> extends AbstractGraph<N> {
  // The default of 11 is rather arbitrary, but roughly matches the sizing of just new HashMap()
  private static final int DEFAULT_MAP_SIZE = 11;

  private final boolean isDirected;
  private final boolean allowsSelfLoops;

  protected final Map<N, NodeAdjacencies<N>> nodeConnections;

  /**
   * Constructs a graph with the properties specified in {@code builder}.
   */
  AbstractConfigurableGraph(GraphBuilder<? super N> builder) {
    this(
        builder,
        Maps.<N, NodeAdjacencies<N>>newLinkedHashMapWithExpectedSize(
            builder.expectedNodeCount.or(DEFAULT_MAP_SIZE)));
  }

  /**
   * Constructs a graph with the properties specified in {@code builder}, initialized with
   * the given node map.
   */
  AbstractConfigurableGraph(GraphBuilder<? super N> builder,
      Map<N, NodeAdjacencies<N>> nodeConnections) {
    this.isDirected = builder.directed;
    this.allowsSelfLoops = builder.allowsSelfLoops;
    this.nodeConnections = checkNotNull(nodeConnections);
  }

  @Override
  public Set<N> nodes() {
    return Collections.unmodifiableSet(nodeConnections.keySet());
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

  protected NodeAdjacencies<N> checkedConnections(Object node) {
    checkNotNull(node, "node");
    NodeAdjacencies<N> connections = nodeConnections.get(node);
    checkArgument(connections != null, NODE_NOT_IN_GRAPH, node);
    return connections;
  }

  protected boolean containsNode(@Nullable Object node) {
    return nodeConnections.containsKey(node);
  }
}
