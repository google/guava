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

/**
 * TODO(b/30133524): Rewrite the top-level javadoc from scratch.
 *
 * A graph consisting of a set of nodes of type N and a set of (implicit) edges.
 * Users that want edges to be first-class objects or support for parallel edges should use the
 * {@link Network} interface instead.
 *
 * <p>For convenience, we may use the term 'graph' refer to {@link BasicGraph}s and/or
 * {@link Network}s.
 *
 * <p>Users that wish to modify a {@code Graph} must work with its subinterface,
 * {@link MutableBasicGraph}.
 *
 * <p>This interface permits, but does not enforce, any of the following variations of graphs:
 * <ul>
 * <li>directed and undirected edges
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
 * <li>{@code nodeA} and {@code nodeB} are mutually <b>adjacent</b> (or <b>connected</b>) in
 *     {@code graph} if an edge has been added between them:
 *     <br><pre><code>
 *       graph.addEdge(nodeA, nodeB);  // after this returns, nodeA and nodeB are adjacent
 *     </pre></code>
 *   In this example, if {@code graph} is <b>directed</b>, then:
 *   <ul>
 *   <li>{@code nodeA} is a <b>predecessor</b> of {code nodeB} in {@code graph}
 *   <li>{@code nodeB} is a <b>successor</b> of {@code nodeA} in {@code graph}
 *   <li>{@code nodeA} has an (implicit) outgoing edge to {@code nodeB} in {@code graph}
 *   <li>{@code nodeB} has an (implicit) incoming edge from {@code nodeA} in {@code graph}
 *   </ul>
 *   If {@code graph} is <b>undirected</b>, then:
 *   <ul>
 *   <li>{@code nodeA} and {@code nodeB} are mutually predecessors and successors
 *       in {@code graph}
 *   <li>{@code nodeA} has an (implicit) edge in {@code graph} that is both outgoing to
 *       {@code nodeB} and incoming from {@code nodeB}, and vice versa.
 *   </ul>
 * <li>A self-loop is an edge that connects a node to itself.
 * </ul>
 *
 * <p>General notes:
 * <ul>
 * <li><b>Nodes must be useable as {@code Map} keys</b>:
 *   <ul>
 *   <li>They must be unique in a graph: nodes {@code nodeA} and {@code nodeB} are considered
 *       different if and only if {@code nodeA.equals(nodeB) == false}.
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
 * <li>There are no Node classes built in.  So you can have a {@code BasicGraph<Integer>}
 *     or a {@code BasicGraph<Author>} or a {@code BasicGraph<Webpage>}.
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
 * <li>Prefer extending {@link AbstractBasicGraph} over implementing {@link BasicGraph} directly.
 *     This will ensure consistent {@link #equals(Object)} and {@link #hashCode()} across
 *     implementations.
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
 *     if (!isVisited(node)) {
 *       visit(node);
 *       for (N successor : graph.successors(node)) {
 *         depthFirstTraverse(successor);
 *       }
 *     }
 *   }
 * </code></pre>
 * </ul>
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @since 20.0
 */
@Beta
public interface BasicGraph<N> extends Graph<N, BasicGraph.Presence> {

  /**
   * A placeholder for the (generally ignored) Value type of a {@link BasicGraph}. Users shouldn't
   * have to reference this enum unless they are implementing the {@link BasicGraph} interface.
   */
  public enum Presence {
    EDGE_EXISTS
  }
}
