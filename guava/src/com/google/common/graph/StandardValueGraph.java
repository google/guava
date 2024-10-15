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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.graph.GraphConstants.DEFAULT_NODE_COUNT;
import static com.google.common.graph.Graphs.checkNonNegative;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.CheckForNull;

/**
 * Standard implementation of {@link ValueGraph} that supports the options supplied by {@link
 * AbstractGraphBuilder}.
 *
 * <p>This class maintains a map of nodes to {@link GraphConnections}.
 *
 * <p>Collection-returning accessors return unmodifiable views: the view returned will reflect
 * changes to the graph (if the graph is mutable) but may not be modified by the user.
 *
 * <p>The time complexity of all collection-returning accessors is O(1), since views are returned.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 */
@ElementTypesAreNonnullByDefault
class StandardValueGraph<N, V> extends AbstractValueGraph<N, V> {
  private final boolean isDirected;
  private final boolean allowsSelfLoops;
  private final ElementOrder<N> nodeOrder;

  final MapIteratorCache<N, GraphConnections<N, V>> nodeConnections;

  long edgeCount; // must be updated when edges are added or removed

  /** Constructs a graph with the properties specified in {@code builder}. */
  StandardValueGraph(AbstractGraphBuilder<? super N> builder) {
    this(
        builder,
        builder.nodeOrder.<N, GraphConnections<N, V>>createMap(
            builder.expectedNodeCount.or(DEFAULT_NODE_COUNT)),
        0L);
  }

  /**
   * Constructs a graph with the properties specified in {@code builder}, initialized with the given
   * node map.
   */
  StandardValueGraph(
      AbstractGraphBuilder<? super N> builder,
      Map<N, GraphConnections<N, V>> nodeConnections,
      long edgeCount) {
    this.isDirected = builder.directed;
    this.allowsSelfLoops = builder.allowsSelfLoops;
    this.nodeOrder = builder.nodeOrder.cast();
    // Prefer the heavier "MapRetrievalCache" for nodes if lookup is expensive.
    this.nodeConnections =
        (nodeConnections instanceof TreeMap)
            ? new MapRetrievalCache<N, GraphConnections<N, V>>(nodeConnections)
            : new MapIteratorCache<N, GraphConnections<N, V>>(nodeConnections);
    this.edgeCount = checkNonNegative(edgeCount);
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
  public Set<N> adjacentNodes(N node) {
    return nodeInvalidatableSet(checkedConnections(node).adjacentNodes(), node);
  }

  @Override
  public Set<N> predecessors(N node) {
    return nodeInvalidatableSet(checkedConnections(node).predecessors(), node);
  }

  @Override
  public Set<N> successors(N node) {
    return nodeInvalidatableSet(checkedConnections(node).successors(), node);
  }

  @Override
  public Set<EndpointPair<N>> incidentEdges(N node) {
    GraphConnections<N, V> connections = checkedConnections(node);
    IncidentEdgeSet<N> incident =
        new IncidentEdgeSet<N>(this, node) {
          @Override
          public Iterator<EndpointPair<N>> iterator() {
            return connections.incidentEdgeIterator(node);
          }
        };
    return nodeInvalidatableSet(incident, node);
  }

  @Override
  public boolean hasEdgeConnecting(N nodeU, N nodeV) {
    return hasEdgeConnectingInternal(checkNotNull(nodeU), checkNotNull(nodeV));
  }

  @Override
  public boolean hasEdgeConnecting(EndpointPair<N> endpoints) {
    checkNotNull(endpoints);
    return isOrderingCompatible(endpoints)
        && hasEdgeConnectingInternal(endpoints.nodeU(), endpoints.nodeV());
  }

  @Override
  @CheckForNull
  public V edgeValueOrDefault(N nodeU, N nodeV, @CheckForNull V defaultValue) {
    return edgeValueOrDefaultInternal(checkNotNull(nodeU), checkNotNull(nodeV), defaultValue);
  }

  @Override
  @CheckForNull
  public V edgeValueOrDefault(EndpointPair<N> endpoints, @CheckForNull V defaultValue) {
    validateEndpoints(endpoints);
    return edgeValueOrDefaultInternal(endpoints.nodeU(), endpoints.nodeV(), defaultValue);
  }

  @Override
  protected long edgeCount() {
    return edgeCount;
  }

  private final GraphConnections<N, V> checkedConnections(N node) {
    GraphConnections<N, V> connections = nodeConnections.get(node);
    if (connections == null) {
      checkNotNull(node);
      throw new IllegalArgumentException("Node " + node + " is not an element of this graph.");
    }
    return connections;
  }

  final boolean containsNode(@CheckForNull N node) {
    return nodeConnections.containsKey(node);
  }

  private final boolean hasEdgeConnectingInternal(N nodeU, N nodeV) {
    GraphConnections<N, V> connectionsU = nodeConnections.get(nodeU);
    return (connectionsU != null) && connectionsU.successors().contains(nodeV);
  }

  @CheckForNull
  private final V edgeValueOrDefaultInternal(N nodeU, N nodeV, @CheckForNull V defaultValue) {
    GraphConnections<N, V> connectionsU = nodeConnections.get(nodeU);
    V value = (connectionsU == null) ? null : connectionsU.value(nodeV);
    // TODO(b/192579700): Use a ternary once it no longer confuses our nullness checker.
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }
}
