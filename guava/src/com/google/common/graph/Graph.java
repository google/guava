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

import javax.annotation.Nullable;

/**
 * A graph consisting of a set of nodes of type N and a set of (implicit) edges.
 * Users that want edges to be first-class objects should use the subtype {@link Network} instead.
 *
 * <p>For convenience, we may use the term 'graph' to refer to any subtype of {@link Graph},
 * including {@link Network} and {@link Hypergraph}.
 *
 * <p>Users that wish to modify a {@code Graph} must work with its subinterface,
 * {@link MutableNetwork}.
 *
 * <p>This interface permits, but does not enforce, any of the following variations of graphs:
 * <ul>
 * <li>directed and undirected edges
 * <li>hyperedges (edges which are incident to arbitrary sets of nodes)
 * <li>nodes and edges with attributes (for example, weighted edges)
 * <li>nodes and edges of different types (for example, bipartite or multimodal graphs)
 * <li>internal representations as matrices, adjacency lists, adjacency maps, etc.
 * </ul>
 *
 * <p>Extensions or implementations of this interface may enforce or disallow any or all
 * of these variations.
 *
 * <p>Definitions:
 * <ul>
 * <li>{@code node1} and {@code node2} are mutually <b>adjacent</b> (or <b>connected</b> in
 *     {@code graph} if an edge has been added between them:
 *     <br><pre><code>
 *       graph.addEdge(node1, node2);  // after this returns, node1 and node2 are adjacent
 *     </pre></code>
 *   In this example, if {@code graph} is <b>directed</b>, then:
 *   <ul>
 *   <li>{@code node1} is a <b>predecessor</b> of {code node2} in {@code graph}
 *   <li>{@code node2} is a <b>successor</b> of {@code node1} in {@code graph}
 *   <li>{@code node1} has an (implicit) outgoing edge to {@code node2} in {@code graph}
 *   <li>{@code node2} has an (implicit) incoming edge from {@code node1} in {@code graph}
 *   </ul>
 *   If {@code graph} is <b>undirected</b>, then:
 *   <ul>
 *   <li>{@code node1} and {@code node2} are mutually predecessors and successors
 *       in {@code graph}
 *   <li>{@code node1} has an (implicit) edge in {@code graph} that is both outgoing to
 *       to {@code node2} and incoming from {@code node2}, and vice versa.
 *   </ul>
 * <li>A self-loop is an edge that connects a node to itself.
 * </ul>
 *
 * <p>General notes:
 * <ul>
 * <li><b>Nodes must be useable as {@code Map} keys</b>:
 *   <ul>
 *   <li>They must be unique in a graph: nodes {@code node1} and {@code node2} are considered
 *       different if and only if {@code node1.equals(node2) == false}.
 *   <li>If graph elements have mutable state:
 *     <ul>
 *     <li>the mutable state must not be reflected in the {@code equals/hashCode} methods
 *         (this is discussed in the {@code Map} documentation in detail)
 *     <li>don't construct multiple elements that are equal to each other and expect them to be
 *         interchangeable.  In particular, when adding such elements to a graph, you should
 *         create them once and store the reference if you will need to refer to those elements
 *         more than once during creation (rather than passing {@code new MyMutableNode(id)}
 *         to each {@code add*()} call).
 *     </ul>
 *   </ul>
 *   <br>Generally speaking, your design may be more robust if you use immutable nodes and
 * store mutable per-element state in a separate data structure (e.g. an element-to-state map).
 * <li>There are no Node classes built in.  So you can have a {@code Graph<Integer>}
 *     or a {@code Graph<Author>} or a {@code Graph<Webpage>}.
 * <li>This framework supports multiple mechanisms for storing the topology of a graph,
 *      including:
 *   <ul>
 *   <li>the Graph implementation stores the topology (for example, by storing a
 *       {@code Map<N, N>} that maps nodes onto their adjacent nodes); this implies that the nodes
 *       are just keys, and can be shared among graphs
 *   <li>the nodes store the topology (for example, by storing a {@code List<E>} of adjacent nodes);
 *       this (usually) implies that nodes are graph-specific
 *   <li>a separate data repository (for example, a database) stores the topology
 *   </ul>
 * </ul>
 *
 * <p>Notes on accessors:
 * <ul>
 * <li>Accessors which return collections may return views of the Graph. Modifications to the graph
 *     which affect a view (e.g. calling {@code addNode(n)} or {@code removeNode(n)} while iterating
 *     through {@code nodes()}) are not supported and may result in ConcurrentModificationException.
 * <li>Accessors which return collections will return empty collections if their inputs are valid
 *     but no elements satisfy the request (for example: {@code adjacentNodes(node)} will return an
 *     empty collection if {@code node} has no adjacent nodes).
 * <li>Accessors will throw {@code IllegalArgumentException} if passed an element
 *     that is not in the graph.
 * <li>Accessors take Object parameters rather than generic type specifiers to match the pattern
 *     set by the Java Collections Framework.
 * </ul>
 *
 * <p>Notes for implementors:
 * <ul>
 * <li>For accessors that return a {@code Set}, there are several options for the set behavior,
 *     including:
 *     <ol>
 *     <li>Set is an immutable copy (e.g. {@code ImmutableSet}): attempts to modify the set in any
 *         way will throw an exception, and modifications to the graph will <b>not</b> be reflected
 *         in the set.
 *     <li>Set is an unmodifiable view (e.g. {@code Collections.unmodifiableSet()}): attempts to
 *         modify the set in any way will throw an exception, and modifications to the graph will be
 *         reflected in the set.
 *     <li>Set is a mutable copy: it may be modified, but modifications to the graph will <b>not</b>
 *         be reflected in the set, and vice versa.
 *     <li>Set is a modifiable view: it may be modified, and modifications to the graph will be
 *         reflected in the set (but modifications to the set will <b>not</b> be reflected in the
 *         graph).
 *     <li>Set exposes the internal data directly: it may be modified, and modifications to the
 *         graph will be reflected in the set, and vice versa.
 *     </ol>
 *     Note that (1) and (2) are generally preferred. (5) is generally a hazardous design choice
 *     and should be avoided, because keeping the internal data structures consistent can be tricky.
 * <li>Prefer extending {@link AbstractGraph} over implementing {@link Graph} directly. This will
 *     ensure consistent {@link #equals(Object)} and {@link #hashCode()} across implementations.
 * <li>{@code Multimap}s are not sufficient internal data structures for Graph implementations
 *     that support isolated nodes (nodes that have no incident edges), due to their restriction
 *     that a key either maps to at least one value, or is not present in the {@code Multimap}.
 * </ul>
 *
 * <p>Examples of use:
 * <ul>
 * <li>Is {@code node} in the graph?
 * <pre><code>
 *   graph.nodes().contains(node)
 * </code></pre>
 * <li>Traversing an undirected graph node-wise:
 * <pre><code>
 *   // Visit nodes reachable from {@code node}.
 *   void depthFirstTraverse(N node) {
 *     for (N neighbor : graph.adjacentNodes(node)) {
 *       if (!isVisited(neighbor)) {
 *         visit(neighbor);
 *         depthFirstTraverse(neighbor);
 *       }
 *     }
 *   }
 * </code></pre>
 * </ul>
 *
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @since 20.0
 */
@Beta
public interface Graph<N> {
  /** Returns all nodes in this graph. */
  Set<N> nodes();

  //
  // Graph properties
  //

  /**
   * Returns true if the edges in this graph have a direction associated with them.
   *
   * <p>A directed edge is an {@linkplain #outEdges(Object) outgoing edge} of its
   * {@linkplain #source(Object) source}, and an {@linkplain #inEdges(Object) incoming edge} of its
   * {@linkplain #target(Object) target}. An undirected edge connects its
   * {@linkplain #incidentNodes(Object) incident nodes} to each other, and is both an
   * {@linkplain #outEdges(Object) outgoing edge} and {@linkplain #inEdges(Object) incoming edge}
   * of each incident node.
   */
  boolean isDirected();

  /**
   * Returns true if this graph allows self-loops (edges that connect a node to itself).
   * Attempting to add a self-loop to a graph that does not allow them will throw an
   * {@link UnsupportedOperationException}.
   */
  boolean allowsSelfLoops();

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
   * edges" (also known as {@code node}'s transitive closure).
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  Set<N> successors(Object node);

  //
  // Element-level queries
  //

  /**
   * Returns the number of edges incident in this graph to {@code node}.  If this node has more than
   * {@code Integer.MAX_VALUE} incident edges in this graph, returns {@code Integer.MAX_VALUE}.
   *
   * <p>Note that self-loops only count once towards a node's degree.
   * This is consistent with the definition of {@link #incidentEdges(Object)}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  int degree(Object node);

  /**
   * Returns the number of incoming edges in this graph of {@code node}.  If this node has more than
   * {@code Integer.MAX_VALUE} incoming edges in this graph, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  int inDegree(Object node);

  /**
   * Returns the number of outgoing edges in this graph of {@code node}.  If this node has more than
   * {@code Integer.MAX_VALUE} outgoing edges in this graph, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  int outDegree(Object node);

  /**
   * Returns {@code true} iff {@code object} is a graph that has the same node relationships
   * as those in this graph.
   *
   * <p>Thus, two graphs A and B are equal if <b>all</b> of the following are true:
   * <ul>
   * <li>A and B have the same node set
   * <li>A and B have the same adjacency relationships, e.g., for each {@code node} the sets of
   *     successor and predecessor nodes are the same in both graphs.
   *     <br>This implies that every edge in A and B connects the same nodes in the same
   *     direction (if any).
   * </ul>
   *
   * <p>Graph properties are <b>not</b> respected by this method. For example, two relations may
   * be considered equal even if one allows self-loops and the other doesn't. Additionally, the
   * order in which edges or nodes are added to the graph, and the order in which they are
   * iterated over, are irrelevant.
   *
   * <p>A reference implementation of this is provided by {@link Graphs#equal(Graph, Graph)}.
   */
  @Override
  boolean equals(@Nullable Object object);

  /**
   * Returns the hash code for this graph. The hash code of a graph is defined as the hash code
   * of a map from each of the graph's nodes to their adjacent nodes.
   *
   * <p>A reference implementation of this is provided by {@link Graphs#hashCode(Graph)}.
   *
   * <p>Note that by this definition, two graphs that are equal in every aspect except edge
   * direction will have the same hash code (but can still be differentiated by {@link #equals}.
   */
  @Override
  int hashCode();
}
