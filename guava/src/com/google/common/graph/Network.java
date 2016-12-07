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
import com.google.errorprone.annotations.CompatibleWith;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * An interface for <a
 * href="https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)">graph</a>-structured data,
 * whose edges are unique objects.
 *
 * <p>A graph is composed of a set of nodes and a set of edges connecting pairs of nodes.
 *
 * <p>There are three main interfaces provided to represent graphs. In order of increasing
 * complexity they are: {@link Graph}, {@link ValueGraph}, and {@link Network}. You should generally
 * prefer the simplest interface that satisfies your use case. See the <a
 * href="https://github.com/google/guava/wiki/GraphsExplained#choosing-the-right-graph-type">
 * "Choosing the right graph type"</a> section of the Guava User Guide for more details.
 *
 * <h3>Capabilities</h3>
 *
 * <p>{@code Network} supports the following use cases (<a
 * href="https://github.com/google/guava/wiki/GraphsExplained#definitions">definitions of
 * terms</a>):
 *
 * <ul>
 *   <li>directed graphs
 *   <li>undirected graphs
 *   <li>graphs that do/don't allow parallel edges
 *   <li>graphs that do/don't allow self-loops
 *   <li>graphs whose nodes/edges are insertion-ordered, sorted, or unordered
 *   <li>graphs whose edges are unique objects
 * </ul>
 *
 * <h3>Building a {@code Network}</h3>
 *
 * <p>The implementation classes that `common.graph` provides are not public, by design. To create
 * an instance of one of the built-in implementations of {@code Network}, use the {@link
 * NetworkBuilder} class:
 *
 * <pre>{@code
 *   MutableNetwork<Integer, MyEdge> graph = NetworkBuilder.directed().build();
 * }</pre>
 *
 * <p>{@link NetworkBuilder#build()} returns an instance of {@link MutableNetwork}, which is a
 * subtype of {@code Network} that provides methods for adding and removing nodes and edges. If you
 * do not need to mutate a graph (e.g. if you write a method than runs a read-only algorithm on the
 * graph), you should use the non-mutating {@link Network} interface, or an {@link
 * ImmutableNetwork}.
 *
 * <p>You can create an immutable copy of an existing {@code Network} using {@link
 * ImmutableNetwork#copyOf(Network)}:
 *
 * <pre>{@code
 *   ImmutableNetwork<Integer, MyEdge> immutableGraph = ImmutableNetwork.copyOf(graph);
 * }</pre>
 *
 * <p>Instances of {@link ImmutableNetwork} do not implement {@link MutableNetwork} (obviously!) and
 * are contractually guaranteed to be unmodifiable and thread-safe.
 *
 * <p>The Guava User Guide has <a
 * href="https://github.com/google/guava/wiki/GraphsExplained#building-graph-instances">more
 * information on (and examples of) building graphs</a>.
 *
 * <h3>Additional documentation</h3>
 *
 * <p>See the Guava User Guide for the {@code common.graph} package (<a
 * href="https://github.com/google/guava/wiki/GraphsExplained">"Graphs Explained"</a>) for
 * additional documentation, including:
 *
 * <ul>
 *   <li><a
 *       href="https://github.com/google/guava/wiki/GraphsExplained#equals-hashcode-and-graph-equivalence">
 *       {@code equals()}, {@code hashCode()}, and graph equivalence</a>
 *   <li><a href="https://github.com/google/guava/wiki/GraphsExplained#synchronization">
 *       Synchronization policy</a>
 *   <li><a href="https://github.com/google/guava/wiki/GraphsExplained#notes-for-implementors">Notes
 *       for implementors</a>
 * </ul>
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
@Beta
public interface Network<N, E> {
  //
  // Network-level accessors
  //

  /** Returns all nodes in this network, in the order specified by {@link #nodeOrder()}. */
  Set<N> nodes();

  /** Returns all edges in this network, in the order specified by {@link #edgeOrder()}. */
  Set<E> edges();

  /**
   * Returns a live view of this network as a {@link Graph}. The resulting {@link Graph} will have
   * an edge connecting node A to node B if this {@link Network} has an edge connecting A to B.
   *
   * <p>If this network {@link #allowsParallelEdges() allows parallel edges}, parallel edges will be
   * treated as if collapsed into a single edge. For example, the {@link #degree(Object)} of a node
   * in the {@link Graph} view may be less than the degree of the same node in this {@link Network}.
   */
  Graph<N> asGraph();

  //
  // Network properties
  //

  /**
   * Returns true if the edges in this network are directed. Directed edges connect a {@link
   * EndpointPair#source() source node} to a {@link EndpointPair#target() target node}, while
   * undirected edges connect a pair of nodes to each other.
   */
  boolean isDirected();

  /**
   * Returns true if this network allows parallel edges. Attempting to add a parallel edge to a
   * network that does not allow them will throw an {@link UnsupportedOperationException}.
   */
  boolean allowsParallelEdges();

  /**
   * Returns true if this network allows self-loops (edges that connect a node to itself).
   * Attempting to add a self-loop to a network that does not allow them will throw an {@link
   * UnsupportedOperationException}.
   */
  boolean allowsSelfLoops();

  /** Returns the order of iteration for the elements of {@link #nodes()}. */
  ElementOrder<N> nodeOrder();

  /** Returns the order of iteration for the elements of {@link #edges()}. */
  ElementOrder<E> edgeOrder();

  //
  // Element-level accessors
  //

  /**
   * Returns the nodes which have an incident edge in common with {@code node} in this network.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  Set<N> adjacentNodes(@CompatibleWith("N") Object node);

  /**
   * Returns all nodes in this network adjacent to {@code node} which can be reached by traversing
   * {@code node}'s incoming edges <i>against</i> the direction (if any) of the edge.
   *
   * <p>In an undirected network, this is equivalent to {@link #adjacentNodes(Object)}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  Set<N> predecessors(@CompatibleWith("N") Object node);

  /**
   * Returns all nodes in this network adjacent to {@code node} which can be reached by traversing
   * {@code node}'s outgoing edges in the direction (if any) of the edge.
   *
   * <p>In an undirected network, this is equivalent to {@link #adjacentNodes(Object)}.
   *
   * <p>This is <i>not</i> the same as "all nodes reachable from {@code node} by following outgoing
   * edges". For that functionality, see {@link Graphs#reachableNodes(Graph, Object)}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  Set<N> successors(@CompatibleWith("N") Object node);

  /**
   * Returns the edges whose {@link #incidentNodes(Object) incident nodes} in this network include
   * {@code node}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  Set<E> incidentEdges(@CompatibleWith("N") Object node);

  /**
   * Returns all edges in this network which can be traversed in the direction (if any) of the edge
   * to end at {@code node}.
   *
   * <p>In a directed network, an incoming edge's {@link EndpointPair#target()} equals {@code node}.
   *
   * <p>In an undirected network, this is equivalent to {@link #incidentEdges(Object)}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  Set<E> inEdges(@CompatibleWith("N") Object node);

  /**
   * Returns all edges in this network which can be traversed in the direction (if any) of the edge
   * starting from {@code node}.
   *
   * <p>In a directed network, an outgoing edge's {@link EndpointPair#source()} equals {@code node}.
   *
   * <p>In an undirected network, this is equivalent to {@link #incidentEdges(Object)}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  Set<E> outEdges(@CompatibleWith("N") Object node);

  /**
   * Returns the count of {@code node}'s {@link #incidentEdges(Object) incident edges}, counting
   * self-loops twice (equivalently, the number of times an edge touches {@code node}).
   *
   * <p>For directed networks, this is equal to {@code inDegree(node) + outDegree(node)}.
   *
   * <p>For undirected networks, this is equal to {@code incidentEdges(node).size()} + (number of
   * self-loops incident to {@code node}).
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  int degree(@CompatibleWith("N") Object node);

  /**
   * Returns the count of {@code node}'s {@link #inEdges(Object) incoming edges} in a directed
   * network. In an undirected network, returns the {@link #degree(Object)}.
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  int inDegree(@CompatibleWith("N") Object node);

  /**
   * Returns the count of {@code node}'s {@link #outEdges(Object) outgoing edges} in a directed
   * network. In an undirected network, returns the {@link #degree(Object)}.
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  int outDegree(@CompatibleWith("N") Object node);

  /**
   * Returns the nodes which are the endpoints of {@code edge} in this network.
   *
   * @throws IllegalArgumentException if {@code edge} is not an element of this network
   */
  EndpointPair<N> incidentNodes(@CompatibleWith("E") Object edge);

  /**
   * Returns the edges which have an {@link #incidentNodes(Object) incident node} in common with
   * {@code edge}. An edge is not considered adjacent to itself.
   *
   * @throws IllegalArgumentException if {@code edge} is not an element of this network
   */
  Set<E> adjacentEdges(@CompatibleWith("E") Object edge);

  /**
   * Returns the set of edges directly connecting {@code nodeU} to {@code nodeV}.
   *
   * <p>In an undirected network, this is equal to {@code edgesConnecting(nodeV, nodeU)}.
   *
   * <p>The resulting set of edges will be parallel (i.e. have equal {@link #incidentNodes(Object)}.
   * If this network does not {@link #allowsParallelEdges() allow parallel edges}, the resulting set
   * will contain at most one edge.
   *
   * @throws IllegalArgumentException if {@code nodeU} or {@code nodeV} is not an element of this
   *     network
   */
  Set<E> edgesConnecting(@CompatibleWith("N") Object nodeU, @CompatibleWith("N") Object nodeV);

  //
  // Network identity
  //

  /**
   * For the default {@link Network} implementations, returns true if {@code this == object}
   * (reference equality). External implementations are free to define this method as they see fit,
   * as long as they satisfy the {@link Object#equals(Object)} contract.
   *
   * <p>To compare two {@link Network}s based on their contents rather than their references, see
   * {@link Graphs#equivalent(Network, Network)}.
   */
  @Override
  boolean equals(@Nullable Object object);

  /**
   * For the default {@link Network} implementations, returns {@code System.identityHashCode(this)}.
   * External implementations are free to define this method as they see fit, as long as they
   * satisfy the {@link Object#hashCode()} contract.
   */
  @Override
  int hashCode();
}
