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
import static com.google.common.graph.GraphErrorMessageUtils.EDGE_NOT_IN_GRAPH;
import static com.google.common.graph.GraphErrorMessageUtils.NODE_NOT_IN_GRAPH;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Abstract configurable implementation of {@link Network} that supports the options supplied
 * by {@link NetworkBuilder}.
 *
 * <p>This class maintains a map of nodes to {@link NodeConnections}. This class also maintains
 * a map of edges to reference nodes. The reference node is defined to be the edge's source node
 * on directed graphs, and an arbitrary endpoint of the edge on undirected graphs.
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
 * @param <E> Edge parameter type
 */
abstract class AbstractConfigurableNetwork<N, E> extends AbstractNetwork<N, E> {
  // The default of 11 is rather arbitrary, but roughly matches the sizing of just new HashMap()
  private static final int DEFAULT_MAP_SIZE = 11;

  private final boolean isDirected;
  private final boolean allowsParallelEdges;
  private final boolean allowsSelfLoops;
  private final ElementOrder<? super N> nodeOrder;
  private final ElementOrder<? super E> edgeOrder;

  protected final Map<N, NodeConnections<N, E>> nodeConnections;

  // We could make this a Map<E, Endpoints<N>>. Although it would make incidentNodes(edge)
  // slightly faster, it would also make Networks consume approximately 20% more memory.
  protected final Map<E, N> edgeToReferenceNode; // reference node == source on directed networks

  /**
   * Constructs a graph with the properties specified in {@code builder}.
   */
  AbstractConfigurableNetwork(NetworkBuilder<? super N, ? super E> builder) {
    this(
        builder,
        AbstractConfigurableNetwork.<N, E>getNodeMapForBuilder(builder),
        AbstractConfigurableNetwork.<N, E>getEdgeMapForBuilder(builder));
  }

  private static <S, T> Map<S, NodeConnections<S, T>> getNodeMapForBuilder(
      NetworkBuilder<? super S, ? super T> builder) {
    int expectedNodeSize = builder.expectedNodeCount.or(DEFAULT_MAP_SIZE);
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

  private static <S, T> Map<T, S> getEdgeMapForBuilder(
      NetworkBuilder<? super S, ? super T> builder) {
    int expectedEdgeSize = builder.expectedEdgeCount.or(DEFAULT_MAP_SIZE);
    switch (builder.edgeOrder.type()) {
        case UNORDERED:
          return Maps.newHashMapWithExpectedSize(expectedEdgeSize);
        case INSERTION:
          return Maps.newLinkedHashMapWithExpectedSize(expectedEdgeSize);
        case SORTED:
          return Maps.newTreeMap(builder.edgeOrder.comparator());
        default:
          throw new IllegalArgumentException("Unrecognized edge ElementOrder type");
    }
  }

  /**
   * Constructs a graph with the properties specified in {@code builder}, initialized with
   * the given node and edge maps.
   */
  AbstractConfigurableNetwork(NetworkBuilder<? super N, ? super E> builder,
      Map<N, NodeConnections<N, E>> nodeConnections,
      Map<E, N> edgeToReferenceNode) {
    this.isDirected = builder.directed;
    this.allowsParallelEdges = builder.allowsParallelEdges;
    this.allowsSelfLoops = builder.allowsSelfLoops;
    this.nodeOrder = builder.nodeOrder;
    this.edgeOrder = builder.edgeOrder;
    this.nodeConnections = checkNotNull(nodeConnections);
    this.edgeToReferenceNode = checkNotNull(edgeToReferenceNode);
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
    return Collections.unmodifiableSet(nodeConnections.keySet());
  }

  /**
   * {@inheritDoc}
   *
   * <p>The order of iteration for this set is determined by the {@code ElementOrder<E>} provided
   * to the {@code GraphBuilder} that was used to create this instance.
   * By default, that order is the order in which the edges were added to the graph.
   */
  @Override
  public Set<E> edges() {
    return Collections.unmodifiableSet(edgeToReferenceNode.keySet());
  }

  @Override
  public boolean isDirected() {
    return isDirected;
  }

  @Override
  public boolean allowsParallelEdges() {
    return allowsParallelEdges;
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
  public ElementOrder<? super E> edgeOrder() {
    return edgeOrder;
  }

  @Override
  public Set<E> incidentEdges(Object node) {
    return checkedConnections(node).incidentEdges();
  }

  @Override
  public Endpoints<N> incidentNodes(Object edge) {
    N nodeA = checkedReferenceNode(edge);
    N nodeB = nodeConnections.get(nodeA).oppositeNode(edge);
    return isDirected ? Endpoints.ofDirected(nodeA, nodeB) : Endpoints.ofUndirected(nodeA, nodeB);
  }

  @Override
  public Set<N> adjacentNodes(Object node) {
    return checkedConnections(node).adjacentNodes();
  }

  @Override
  public Set<E> adjacentEdges(Object edge) {
    Endpoints<N> endpoints = incidentNodes(edge);
    Set<E> endpointsIncidentEdges =
        Sets.union(incidentEdges(endpoints.nodeA()), incidentEdges(endpoints.nodeB()));
    return Sets.difference(endpointsIncidentEdges, ImmutableSet.of(edge));
  }

  @Override
  public Set<E> edgesConnecting(Object nodeA, Object nodeB) {
    NodeConnections<N, E> connectionsN1 = checkedConnections(nodeA);
    if (!allowsSelfLoops && nodeA.equals(nodeB)) {
      return ImmutableSet.of();
    }
    checkArgument(containsNode(nodeB), NODE_NOT_IN_GRAPH, nodeB);
    return connectionsN1.edgesConnecting(nodeB);
  }

  @Override
  public Set<E> inEdges(Object node) {
    return checkedConnections(node).inEdges();
  }

  @Override
  public Set<E> outEdges(Object node) {
    return checkedConnections(node).outEdges();
  }

  @Override
  public Set<N> predecessors(Object node) {
    return checkedConnections(node).predecessors();
  }

  @Override
  public Set<N> successors(Object node) {
    return checkedConnections(node).successors();
  }

  protected NodeConnections<N, E> checkedConnections(Object node) {
    checkNotNull(node, "node");
    NodeConnections<N, E> connections = nodeConnections.get(node);
    checkArgument(connections != null, NODE_NOT_IN_GRAPH, node);
    return connections;
  }

  protected N checkedReferenceNode(Object edge) {
    checkNotNull(edge, "edge");
    N referenceNode = edgeToReferenceNode.get(edge);
    checkArgument(referenceNode != null, EDGE_NOT_IN_GRAPH, edge);
    return referenceNode;
  }

  protected boolean containsNode(@Nullable Object node) {
    return nodeConnections.containsKey(node);
  }

  protected boolean containsEdge(@Nullable Object edge) {
    return edgeToReferenceNode.containsKey(edge);
  }
}
