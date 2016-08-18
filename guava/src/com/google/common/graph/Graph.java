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

import com.google.common.annotations.Beta;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * An interface to represent a graph data structure. Graphs can be either directed or undirected
 * (but cannot have both directed edges and undirected edges). Every edge is associated with an
 * arbitrary user-provided value. Parallel edges are not supported (although the Value type may be,
 * for example, a collection).
 *
 * <p>Nodes in a graph are analogous to keys in a Map - they must be unique within a graph.
 * Values in a graph are analogous to values in a Map - they may be any arbitrary object.
 *
 * <p>If you don't need to associate value objects with edges (e.g. you're modeling a binary
 * relation where an edge either exists or doesn't), see the {@link BasicGraph} interface.
 *
 * TODO(b/30133524): Rewrite the top-level javadoc from scratch.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
 */
@Beta
public interface Graph<N, V> {
  //
  // Graph-level accessors
  //

  /**
   * Returns all nodes in this graph, in the order specified by {@link #nodeOrder()}.
   */
  Set<N> nodes();

  /**
   * Returns all edges in this graph.
   */
  Set<Endpoints<N>> edges();

  //
  // Graph properties
  //

  /**
   * Returns true if the edges in this graph have a direction associated with them.
   */
  boolean isDirected();

  /**
   * Returns true if this graph allows self-loops (edges that connect a node to itself).
   * Attempting to add a self-loop to a graph that does not allow them will throw an
   * {@link UnsupportedOperationException}.
   */
  boolean allowsSelfLoops();

  /**
   * Returns the order of iteration for the elements of {@link #nodes()}.
   */
  ElementOrder<N> nodeOrder();

  //
  // Element-level accessors
  //

  /**
   * Returns the nodes which have an incident edge in common with {@code node} in this graph.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  Set<N> adjacentNodes(Object node);

  /**
   * Returns all nodes in this graph adjacent to {@code node} which can be reached by traversing
   * {@code node}'s incoming edges <i>against</i> the direction (if any) of the edge.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  Set<N> predecessors(Object node);

  /**
   * Returns all nodes in this graph adjacent to {@code node} which can be reached by traversing
   * {@code node}'s outgoing edges in the direction (if any) of the edge.
   *
   * <p>This is <i>not</i> the same as "all nodes reachable from {@code node} by following outgoing
   * edges". For that functionality, see {@link Graphs#reachableNodes(Graph, Object)} and {@link
   * Graphs#transitiveClosure(Graph)}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  Set<N> successors(Object node);

  /**
   * Returns the count of {@code node}'s incident edges, counting self-loops twice (equivalently,
   * the number of times an edge touches {@code node}).
   *
   * <p>For directed graphs, this is equivalent to {@code inDegree(node) + outDegree(node)}.
   *
   * <p>For undirected graphs, this is equivalent to {@code adjacentNodes(node).size()} + (1 if
   * {@code node} has an incident self-loop, 0 otherwise).
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  int degree(Object node);

  /**
   * Returns the count of {@code node}'s incoming edges (equal to {@code predecessors(node).size()})
   * in a directed graph. In an undirected graph, returns the {@link #degree(Object)}.
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  int inDegree(Object node);

  /**
   * Returns the count of {@code node}'s outgoing edges (equal to {@code successors(node).size()})
   * in a directed graph. In an undirected graph, returns the {@link #degree(Object)}.
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  int outDegree(Object node);

  /**
   * If there is an edge connecting {@code nodeA} to {@code nodeB}, returns the non-null value
   * associated with that edge.
   *
   * @throws IllegalArgumentException if there is no edge connecting {@code nodeA} to {@code nodeB}
   */
  V edgeValue(Object nodeA, Object nodeB);

  /**
   * If there is an edge connecting {@code nodeA} to {@code nodeB}, returns the non-null value
   * associated with that edge; otherwise, returns {@code defaultValue}.
   *
   * @throws IllegalArgumentException if {@code nodeA} or {@code nodeB} is not an element of
   *     this graph
   */
  V edgeValueOrDefault(Object nodeA, Object nodeB, @Nullable V defaultValue);

  //
  // Graph identity
  //

  /**
   * Returns {@code true} iff {@code object} is a {@link Graph} that has the same elements and the
   * same structural relationships as those in this graph.
   *
   * <p>Thus, two graphs A and B are equal if <b>all</b> of the following are true:
   * <ul>
   * <li>A and B have equal {@link #isDirected() directedness}.
   * <li>A and B have equal {@link #nodes() node sets}.
   * <li>A and B have equal {@link #edges() edge sets}.
   * <li>Every edge in A and B are associated with equal {@link #edgeValue(Object, Object) values}.
   * </ul>
   *
   * <p>Graph properties besides {@link #isDirected() directedness} do <b>not</b> affect equality.
   * For example, two graphs may be considered equal even if one allows self-loops and the other
   * doesn't. Additionally, the order in which nodes or edges are added to the graph, and the order
   * in which they are iterated over, are irrelevant.
   *
   * <p>A reference implementation of this is provided by {@link AbstractGraph#equals(Object)}.
   */
  @Override
  boolean equals(@Nullable Object object);

  /**
   * Returns the hash code for this graph. The hash code of a graph is defined as the hash code
   * of a map from each of its {@link #edges() edges} to the associated {@link #edgeValue(Object,
   * Object) edge value}.
   *
   * <p>A reference implementation of this is provided by {@link AbstractGraph#hashCode()}.
   */
  @Override
  int hashCode();
}
