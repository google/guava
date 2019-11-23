/*
 * Copyright (C) 2017 The Guava Authors
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

import java.util.Set;

/**
 * A non-public interface for the methods shared between {@link Graph} and {@link ValueGraph}.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 */
interface BaseGraph<N> extends SuccessorsFunction<N>, PredecessorsFunction<N> {
  //
  // Graph-level accessors
  //

  /** Returns all nodes in this graph, in the order specified by {@link #nodeOrder()}. */
  Set<N> nodes();

  /** Returns all edges in this graph. */
  Set<EndpointPair<N>> edges();

  //
  // Graph properties
  //

  /**
   * Returns true if the edges in this graph are directed. Directed edges connect a {@link
   * EndpointPair#source() source node} to a {@link EndpointPair#target() target node}, while
   * undirected edges connect a pair of nodes to each other.
   */
  boolean isDirected();

  /**
   * Returns true if this graph allows self-loops (edges that connect a node to itself). Attempting
   * to add a self-loop to a graph that does not allow them will throw an {@link
   * IllegalArgumentException}.
   */
  boolean allowsSelfLoops();

  /** Returns the order of iteration for the elements of {@link #nodes()}. */
  ElementOrder<N> nodeOrder();

  //
  // Element-level accessors
  //

  /**
   * Returns the nodes which have an incident edge in common with {@code node} in this graph.
   *
   * <p>This is equal to the union of {@link #predecessors(Object)} and {@link #successors(Object)}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  Set<N> adjacentNodes(N node);

  /**
   * Returns all nodes in this graph adjacent to {@code node} which can be reached by traversing
   * {@code node}'s incoming edges <i>against</i> the direction (if any) of the edge.
   *
   * <p>In an undirected graph, this is equivalent to {@link #adjacentNodes(Object)}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  @Override
  Set<N> predecessors(N node);

  /**
   * Returns all nodes in this graph adjacent to {@code node} which can be reached by traversing
   * {@code node}'s outgoing edges in the direction (if any) of the edge.
   *
   * <p>In an undirected graph, this is equivalent to {@link #adjacentNodes(Object)}.
   *
   * <p>This is <i>not</i> the same as "all nodes reachable from {@code node} by following outgoing
   * edges". For that functionality, see {@link Graphs#reachableNodes(Graph, Object)}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  @Override
  Set<N> successors(N node);

  /**
   * Returns the edges in this graph whose endpoints include {@code node}.
   *
   * <p>This is equal to the union of incoming and outgoing edges.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   * @since 24.0
   */
  Set<EndpointPair<N>> incidentEdges(N node);

  /**
   * Returns the count of {@code node}'s incident edges, counting self-loops twice (equivalently,
   * the number of times an edge touches {@code node}).
   *
   * <p>For directed graphs, this is equal to {@code inDegree(node) + outDegree(node)}.
   *
   * <p>For undirected graphs, this is equal to {@code incidentEdges(node).size()} + (number of
   * self-loops incident to {@code node}).
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  int degree(N node);

  /**
   * Returns the count of {@code node}'s incoming edges (equal to {@code predecessors(node).size()})
   * in a directed graph. In an undirected graph, returns the {@link #degree(Object)}.
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  int inDegree(N node);

  /**
   * Returns the count of {@code node}'s outgoing edges (equal to {@code successors(node).size()})
   * in a directed graph. In an undirected graph, returns the {@link #degree(Object)}.
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  int outDegree(N node);

  /**
   * Returns true if there is an edge that directly connects {@code nodeU} to {@code nodeV}. This is
   * equivalent to {@code nodes().contains(nodeU) && successors(nodeU).contains(nodeV)}.
   *
   * <p>In an undirected graph, this is equal to {@code hasEdgeConnecting(nodeV, nodeU)}.
   *
   * @since 23.0
   */
  boolean hasEdgeConnecting(N nodeU, N nodeV);

  /**
   * Returns true if there is an edge that directly connects {@code endpoints} (in the order, if
   * any, specified by {@code endpoints}). This is equivalent to {@code
   * edges().contains(endpoints)}.
   *
   * <p>Unlike the other {@code EndpointPair}-accepting methods, this method does not throw if the
   * endpoints are unordered; it simply returns false. This is for consistency with the behavior of
   * {@link Collection#contains(Object)} (which does not generally throw if the object cannot be
   * present in the collection), and the desire to have this method's behavior be compatible with
   * {@code edges().contains(endpoints)}.
   *
   * @since 27.1
   */
  boolean hasEdgeConnecting(EndpointPair<N> endpoints);
}
