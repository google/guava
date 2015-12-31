/*
 * Copyright (C) 2014 The Guava Authors
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

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * A graph consisting of a set of nodes of type N and a set of edges of type E.
 *
 * <p>This interface permits, but does not enforce, any of the following variations of graphs:
 * <ul>
 * <li>directed and undirected edges
 * <li>hyperedges (edges which are incident to arbitrary sets of nodes)
 * <li>nodes and edges with attributes (for example, weighted edges)
 * <li>nodes and edges of different types (for example, bipartite or multimodal graphs)
 * <li>parallel edges (multiple edges which connect a single set of vertices)
 * <li>internal representations as matrices, adjacency lists, adjacency maps, etc.
 * </ul>
 *
 * <p>Extensions or implementations of this interface may enforce or disallow any or all
 * of these variations.
 *
 * <p>Definitions:
 * <ul>
 * <li>{@code edge} and {@code node} are <b>incident</b> to each other if the set of
 *     {@code edge}'s endpoints includes {@code node}.
 * <li>{@code node1} and {@code node2} are mutually <b>adjacent</b> if both are incident
 *     to a common {@code edge}.
 *     <br>Similarly, {@code edge1} and {@code edge2} are mutually adjacent if both are
 *     incident to a common {@code node}.
 * <li>Elements are <b>connected</b> if they are either incident or adjacent.
 * <li>{@code edge} is an <b>incoming edge</b> of a {@code node} if it can be traversed (in
 *     the direction, if any, of {@code edge}) from a node adjacent to {@code node}.
 * <li>{@code edge} is an <b>outgoing edge</b> of {@code node} if it can be traversed (in
 *     the direction, if any, of {@code edge}) from {@code node} to reach a node adjacent to
 *     {@code node}.
 *   <ul>
 *   <li>Note: <b>undirected</b> edges are both incoming and outgoing edges of a {@code node},
 *       while <b>directed</b> edges are either incoming or outgoing edges of {@code node}
 *       (and not both, unless the edge is a self-loop).
 *       <br>Thus, in the following example {@code edge1} is an incoming edge of {@code node2} and
 *       an outgoing edge of {@code node1}, while {@code edge2} is both an incoming and an outgoing
 *       edge of both {@code node3} and {@code node4}:
 *       <br><pre><code>
 *         directedGraph.addEdge(edge1, node1, node2);
 *         undirectedGraph.addEdge(edge2, node3, node4);
 *       </pre></code>
 *   </ul>
 * <li>A node {@code pred} is a <b>predecessor</b> of {@code node} if it is incident to an incoming
 *     {@code edge} of {@code node} (and is not itself {@code node} unless {@code edge} is
 *     a self-loop).
 * <li>A node {@code succ} is a <b>successor</b> of {@code node} if it is incident to an outgoing
 *     {@code edge} of {@code node} (and is not itself {@code node} unless {@code edge} is
 *     a self-loop).
 * <li>Directed edges only:
 *   <ul>
 *   <li>{@code node} is a <b>source</b> of {@code edge} if {@code edge} is an outgoing edge
 *       of {@code node}.
 *   <li>{@code node} is a <b>target</b> of {@code edge} if {@code edge} is an incoming edge
 *       of {@code node}.
 *   </ul>
 * </ul>
 *
 * <p>General notes:
 * <ul>
 * <li><b>Nodes/Edges must be unique</b>, just as {@code Map} keys must be. So, nodes {@code node1}
 *     and {@code node2} are considered different if and only if
 *     {@code node1.equals(node2) == false}, and the same for edges.
 * <li>There are no Node or Edge classes built in.  So you can have a {@code Graph<Integer, String>}
 *     or a {@code Graph<Author,Publication>} or a {@code Graph<Webpage,Link>}.
 * <li>This framework supports multiple mechanisms for storing the topology of a graph, including:
 *   <ul>
 *   <li>the Graph implementation stores the topology (for example, by storing a {@code Map<N, E>}
 *       that maps nodes onto their incident edges); this implies that the nodes and edges
 *       are just keys, and can be shared among graphs
 *   <li>the nodes store the topology (for example, by storing a {@code List<E>} of incident edges);
 *       this (usually) implies that nodes are graph-specific or map
 *   <li>a separate data repository (for example, a database) stores the topology
 *   </ul>
 * <li>Users that are not interested in edges as first-class objects can create an implementation of
 *     (or subinterface of, or class delegating to) Graph that only exposes node-related methods.
 * <li>{@code Multimap}s can't be used as internal data structures for Graph implementations
 * that support isolated nodes (nodes that have no incident edges), due to their restriction
 * that a key either maps to at least one value, or is not present in the {@code Multimap}.
 * </ul>
 *
 * <p>Notes on accessors:
 * <ul>
 * <li>Accessors which return collections return unmodifiable views.
 * <li>Accessors which return collections will return empty collections if their inputs are valid
 * but no elements satisfy the request (for example: {@code adjacentNodes(node)} will return an
 * empty collection if {@code node} has no adjacent nodes).
 * <li>Accessors will throw {@code IllegalArgumentException} if passed a node/edge
 *     that is not in the graph.
 * <li>Accessors take Object parameters rather than N/E generic type specifiers to match the pattern
 *     set by the Java Collections Framework.
 * </ul>
 *
 * <p>For accessors that return a {@code Set}, there are several options for the set behavior,
 *    including:
 * <ol>
 * <li>Set is an immutable copy (e.g. {@code ImmutableSet}): attempts to modify the set in any way
 *     will throw an exception, and modifications to the graph will <b>not</b> be reflected in
 *     the set.
 * <li>Set is an unmodifiable view (e.g. {@code Collections.unmodifiableSet()}): attempts to modify
 *     the set in any way will throw an exception, and modifications to the graph will be
 *     reflected in the set.
 * <li>Set is a mutable copy: it may be modified, but modifications to the graph will <b>not</b> be
 *     reflected in the set, and vice versa.
 * <li>Set is a modifiable view: it may be modified, and modifications to the graph will be
 *     reflected in the set (but modifications to the set will <b>not</b> be
 *     reflected in the graph).
 * <li>Set exposes the internal data directly: it may be modified, and modifications to the
 *     graph will be reflected in the set, and vice versa.
 * </ol>
 * Note that (1) and (2) are generally preferred. (5) is generally a hazardous design choice
 * and should be avoided, because keeping the internal data structures consistent can be tricky.
 *
 * <p>Examples of common operations:
 * <ul>
 * <li>Is {@code node} in the graph?
 *   <ul>
 *   <li>{@code graph.nodes().contains(n)}
 *   </ul>
 * <li>Traversing an undirected graph node-wise:
 * <p><pre><code>
 *   // visit nodes reachable from a given node n
 *   void depthFirstTraverse(N n) {
 *     for (N neighbor : graph.adjacentNodes(n)) {
 *       if (!isVisited(neighbor)) {
 *         visit(neighbor);
 *         depthFirstTraverse(neighbor);
 *       }
 *     }
 *   }
 * </code></pre>
 * <li>Traversing a directed graph edge-wise:
 * <p><pre><code>
 *   // visit nodes reachable from a given node n via each of n's outedges
 *   // TODO(user): create a utility method for getting the 'other' node associated with an edge
 *   // that will work with non-directed graphs
 *   void depthFirstTraverse(N n) {
 *     for (E outEdge : graph.outEdges(n)) {
 *       N successor = graph.target(outEdge);
 *       if (!isVisited(successor)) {
 *         visit(successor);
 *         depthFirstTraverse(successor);
 *       }
 *     }
 *   }
 * </ul>
 *
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
@Beta
public interface Graph<N, E> {
  /** Returns all nodes in this graph. */
  @CheckReturnValue
  Set<N> nodes();

  /** Returns all edges in this graph. */
  @CheckReturnValue
  Set<E> edges();

  /** Returns the {@link GraphConfig} that defines this instance's configuration. */
  @CheckReturnValue
  GraphConfig config();

  //
  // Element-level accessors
  //

  /**
   * Returns the edges whose endpoints in this graph include {@code node}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  @CheckReturnValue
  Set<E> incidentEdges(Object node);

  /**
   * Returns the nodes which are the endpoints of {@code edge} in this graph.
   *
   * @throws IllegalArgumentException if {@code edge} is not an element of this graph
   */
  @CheckReturnValue
  Set<N> incidentNodes(Object edge);

  /**
   * Returns the nodes which have an {@linkplain #incidentEdges(Object) incident edge}
   * in common with {@code node} in this graph.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  @CheckReturnValue
  Set<N> adjacentNodes(Object node);

  /**
   * Returns the edges which have an {@linkplain #incidentNodes(Object) incident node}
   * in common with {@code edge} in this graph.
   *
   * <p>Whether an edge is considered adjacent to itself is not defined by this interface, but
   * generally for non-hypergraphs, edges are not considered to be self-adjacent.
   *
   * @throws IllegalArgumentException if {@code edge} is not an element of this graph
   */
  @CheckReturnValue
  Set<E> adjacentEdges(Object edge);

  /**
   * Returns the edges that are {@linkplain #incidentEdges(Object) incident} in this graph
   * to both nodes {@code node1} and {@code node2}.
   *
   * <p>If the graph is directed, the {@linkplain #source(Object) source} and
   * {@linkplain DirectedGraph#target(Object) target} of the edges returned must be {@code node1}
   * and {@code node2}, respectively.
   *
   * @throws IllegalArgumentException if {@code node1} or {@code node2} is not an element
   *     of this graph
   */
  @CheckReturnValue
  Set<E> edgesConnecting(Object node1, Object node2);

  /**
   * Returns all edges in this graph which can be traversed in the direction (if any) of the edge
   * to end at {@code node}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  @CheckReturnValue
  Set<E> inEdges(Object node);

  /**
   * Returns all edges in this graph which can be traversed in the direction (if any) of the edge
   * starting from {@code node}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  @CheckReturnValue
  Set<E> outEdges(Object node);

  /**
   * Returns all nodes in this graph adjacent to {@code node} which can be reached by traversing
   * {@code node}'s {@linkplain #inEdges(Object) incoming edges} <i>against</i> the direction
   * (if any) of the edge.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  @CheckReturnValue
  Set<N> predecessors(Object node);

  /**
   * Returns all nodes in this graph adjacent to {@code node} which can be reached by traversing
   * {@code node}'s {@linkplain #outEdges(Object) outgoing edges} in the direction (if any) of the
   * edge.
   *
   * <p>This is <i>not</i> the same as "all nodes reachable from {@code node} by following outgoing
   * edges" (also known as {@code node}'s transitive closure).
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  @CheckReturnValue
  Set<N> successors(Object node);

  //
  // Element-level queries
  //

  /**
   * Returns the number of edges {@linkplain #incidentEdges(Object) incident} in this graph
   * to {@code node}.
   *
   * <p>Equivalent to {@code incidentEdges(node).size()}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  @CheckReturnValue
  long degree(Object node);

  /**
   * Returns the number of {@linkplain #inEdges(Object) incoming edges} in this graph
   * of {@code node}.
   *
   * <p>Equivalent to {@code inEdges(node).size()}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  @CheckReturnValue
  long inDegree(Object node);

  /**
   * Returns the number of {@linkplain #outEdges(Object) outgoing edges} in this graph
   * of {@code node}.
   *
   * <p>Equivalent to {@code outEdges(node).size()}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  @CheckReturnValue
  long outDegree(Object node);

  //
  // Element mutations
  //

  /**
   * Adds {@code node} to this graph (optional operation).
   *
   * <p><b>Nodes must be unique</b>, just as {@code Map} keys must be; they must also be non-null.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the add operation is not supported by this graph
   */
  boolean addNode(N node);

  /**
   * Adds {@code edge} to this graph, connecting {@code node1} and {@code node2}
   * (optional operation).
   *
   * <p><b>Edges must be unique</b>, just as {@code Map} keys must be; they must also be non-null.
   *
   * <p>If the graph is directed, {@code node1} is {@code edge}’s source,
   * {@code node2} is {@code edge}’s target, and {@code edge} is an outgoing edge of
   * {@code node1} and an incoming edge of {@code node2}.
   *
   * <p>If {@code edge} already connects {@code node1} to {@code node2} in this graph
   * (in the specified order if order is significant, as for directed graphs, else in any order),
   * then this method will have no effect and will return {@code false}.
   *
   * <p>Behavior if {@code node1} and {@code node2} are not already elements of the graph is
   * unspecified. Suggested behaviors include (a) silently adding {@code node1} and {@code node2}
   * to the graph or (b) throwing {@code IllegalArgumentException}.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws IllegalArgumentException if {@code edge} already exists and connects nodes other than
   *     {@code node1} and {@code node2}, or if the graph is not a multigraph and {@code node1} is
   *     already connected to {@code node2}
   * @throws UnsupportedOperationException if the add operation is not supported by this graph
   */
  boolean addEdge(E edge, N node1, N node2);

  /**
   * Removes {@code node} from this graph, if it is present (optional operation).
   * In general, all edges incident to {@code node} in this graph will also be removed.
   * (This is not true for hyperedges.)
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the remove operation is not supported by this graph
   */
  boolean removeNode(Object node);

  /**
   * Removes {@code edge} from this graph, if it is present (optional operation).
   * In general, nodes incident to {@code edge} are unaffected (although implementations may choose
   * to disallow certain configurations, e.g., isolated nodes).
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * @throws UnsupportedOperationException if the remove operation is not supported by this graph
   */
  boolean removeEdge(Object edge);

  /**
   * Returns {@code true} iff {@code object} is the same type of graph (directed, undirected,
   * hypergraph) as this graph, and the same node/edge relationships exist in both graphs.
   *
   * <p>Thus, two graphs A and B are equal if <b>all</b> of the following are true:
   * <ul>
   * <li>A and B are of the same type ({@code DirectedGraph, UndirectedGraph, Hypergraph})
   * <li>A and B have the same node set
   * <li>A and B have the same edge set
   * <li>A and B have the same incidence relationships, e.g., for each node/edge in A and in B
   *     its incident edge/node set in A is the same as its incident edge/node set in B.
   *     <br>Thus, even if a {@code node} has the same sets of <i>adjacent</i> nodes
   *         (neighbors) in both A and B, if the sets of edges by which {@code node} is connected to
   *         its adjacent nodes are not the same in both A and B, then A and B are not equal.
   * </ul>
   *
   * <p>Properties that are <b>not</b> respected by this method:
   * <ul>
   * <li>{@code GraphConfig} configurations.  If two graphs are equal by the above criteria but have
   * different configurations, they are still equal.  (For example: two graphs may be considered
   * equal even if one allows parallel edges and the other doesn't.)
   * <li>Edge/node ordering.  The order in which edges or nodes are added to the graph, and the
   * order in which they are iterated over, are irrelevant.
   * </ul>
   */
  @Override
  @CheckReturnValue
  boolean equals(@Nullable Object object);
}
