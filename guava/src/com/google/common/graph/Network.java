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
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An interface for <a
 * href="https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)">graph</a>-structured data,
 * whose edges are unique objects.
 *
 * <p>A graph is composed of a set of nodes and a set of edges connecting pairs of nodes.
 *
 * <p>There are three primary interfaces provided to represent graphs. In order of increasing
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
 * <p>The implementation classes that {@code common.graph} provides are not public, by design. To
 * create an instance of one of the built-in implementations of {@code Network}, use the {@link
 * NetworkBuilder} class:
 *
 * <pre>{@code
 * MutableNetwork<Integer, MyEdge> graph = NetworkBuilder.directed().build();
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
 * ImmutableNetwork<Integer, MyEdge> immutableGraph = ImmutableNetwork.copyOf(graph);
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
public interface Network<N, E> extends SuccessorsFunction<N>, PredecessorsFunction<N> {
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
   * network that does not allow them will throw an {@link IllegalArgumentException}.
   */
  boolean allowsParallelEdges();

  /**
   * Returns true if this network allows self-loops (edges that connect a node to itself).
   * Attempting to add a self-loop to a network that does not allow them will throw an {@link
   * IllegalArgumentException}.
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
   * <p>This is equal to the union of {@link #predecessors(Object)} and {@link #successors(Object)}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  Set<N> adjacentNodes(N node);

  /**
   * Returns all nodes in this network adjacent to {@code node} which can be reached by traversing
   * {@code node}'s incoming edges <i>against</i> the direction (if any) of the edge.
   *
   * <p>In an undirected network, this is equivalent to {@link #adjacentNodes(Object)}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  @Override
  Set<N> predecessors(N node);

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
  @Override
  Set<N> successors(N node);

  /**
   * Returns the edges whose {@link #incidentNodes(Object) incident nodes} in this network include
   * {@code node}.
   *
   * <p>This is equal to the union of {@link #inEdges(Object)} and {@link #outEdges(Object)}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  Set<E> incidentEdges(N node);

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
  Set<E> inEdges(N node);

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
  Set<E> outEdges(N node);

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
  int degree(N node);

  /**
   * Returns the count of {@code node}'s {@link #inEdges(Object) incoming edges} in a directed
   * network. In an undirected network, returns the {@link #degree(Object)}.
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  int inDegree(N node);

  /**
   * Returns the count of {@code node}'s {@link #outEdges(Object) outgoing edges} in a directed
   * network. In an undirected network, returns the {@link #degree(Object)}.
   *
   * <p>If the count is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this network
   */
  int outDegree(N node);

  /**
   * Returns the nodes which are the endpoints of {@code edge} in this network.
   *
   * @throws IllegalArgumentException if {@code edge} is not an element of this network
   */
  EndpointPair<N> incidentNodes(E edge);

  /**
   * Returns the edges which have an {@link #incidentNodes(Object) incident node} in common with
   * {@code edge}. An edge is not considered adjacent to itself.
   *
   * @throws IllegalArgumentException if {@code edge} is not an element of this network
   */
  Set<E> adjacentEdges(E edge);

  /**
   * Returns the set of edges that each directly connect {@code nodeU} to {@code nodeV}.
   *
   * <p>In an undirected network, this is equal to {@code edgesConnecting(nodeV, nodeU)}.
   *
   * <p>The resulting set of edges will be parallel (i.e. have equal {@link #incidentNodes(Object)}.
   * If this network does not {@link #allowsParallelEdges() allow parallel edges}, the resulting set
   * will contain at most one edge (equivalent to {@code edgeConnecting(nodeU, nodeV).asSet()}).
   *
   * @throws IllegalArgumentException if {@code nodeU} or {@code nodeV} is not an element of this
   *     network
   */
  Set<E> edgesConnecting(N nodeU, N nodeV);

  /**
   * Returns the set of edges that each directly connect {@code endpoints} (in the order, if any,
   * specified by {@code endpoints}).
   *
   * <p>The resulting set of edges will be parallel (i.e. have equal {@link #incidentNodes(Object)}.
   * If this network does not {@link #allowsParallelEdges() allow parallel edges}, the resulting set
   * will contain at most one edge (equivalent to {@code edgeConnecting(endpoints).asSet()}).
   *
   * <p>If this network is directed, {@code endpoints} must be ordered.
   *
   * @throws IllegalArgumentException if either endpoint is not an element of this network
   * @throws IllegalArgumentException if the endpoints are unordered and the graph is directed
   * @since 27.1
   */
  Set<E> edgesConnecting(EndpointPair<N> endpoints);

  /**
   * Returns the single edge that directly connects {@code nodeU} to {@code nodeV}, if one is
   * present, or {@code Optional.empty()} if no such edge exists.
   *
   * <p>In an undirected network, this is equal to {@code edgeConnecting(nodeV, nodeU)}.
   *
   * @throws IllegalArgumentException if there are multiple parallel edges connecting {@code nodeU}
   *     to {@code nodeV}
   * @throws IllegalArgumentException if {@code nodeU} or {@code nodeV} is not an element of this
   *     network
   * @since 23.0
   */
  Optional<E> edgeConnecting(N nodeU, N nodeV);

  /**
   * Returns the single edge that directly connects {@code endpoints} (in the order, if any,
   * specified by {@code endpoints}), if one is present, or {@code Optional.empty()} if no such edge
   * exists.
   *
   * <p>If this graph is directed, the endpoints must be ordered.
   *
   * @throws IllegalArgumentException if there are multiple parallel edges connecting {@code nodeU}
   *     to {@code nodeV}
   * @throws IllegalArgumentException if either endpoint is not an element of this network
   * @throws IllegalArgumentException if the endpoints are unordered and the graph is directed
   * @since 27.1
   */
  Optional<E> edgeConnecting(EndpointPair<N> endpoints);

  /**
   * Returns the single edge that directly connects {@code nodeU} to {@code nodeV}, if one is
   * present, or {@code null} if no such edge exists.
   *
   * <p>In an undirected network, this is equal to {@code edgeConnectingOrNull(nodeV, nodeU)}.
   *
   * @throws IllegalArgumentException if there are multiple parallel edges connecting {@code nodeU}
   *     to {@code nodeV}
   * @throws IllegalArgumentException if {@code nodeU} or {@code nodeV} is not an element of this
   *     network
   * @since 23.0
   */
  @Nullable
  E edgeConnectingOrNull(N nodeU, N nodeV);

  /**
   * Returns the single edge that directly connects {@code endpoints} (in the order, if any,
   * specified by {@code endpoints}), if one is present, or {@code null} if no such edge exists.
   *
   * <p>If this graph is directed, the endpoints must be ordered.
   *
   * @throws IllegalArgumentException if there are multiple parallel edges connecting {@code nodeU}
   *     to {@code nodeV}
   * @throws IllegalArgumentException if either endpoint is not an element of this network
   * @throws IllegalArgumentException if the endpoints are unordered and the graph is directed
   * @since 27.1
   */
  @Nullable
  E edgeConnectingOrNull(EndpointPair<N> endpoints);

  /**
   * Returns true if there is an edge that directly connects {@code nodeU} to {@code nodeV}. This is
   * equivalent to {@code nodes().contains(nodeU) && successors(nodeU).contains(nodeV)}, and to
   * {@code edgeConnectingOrNull(nodeU, nodeV) != null}.
   *
   * <p>In an undirected graph, this is equal to {@code hasEdgeConnecting(nodeV, nodeU)}.
   *
   * @since 23.0
   */
  boolean hasEdgeConnecting(N nodeU, N nodeV);

  /**
   * Returns true if there is an edge that directly connects {@code endpoints} (in the order, if
   * any, specified by {@code endpoints}).
   *
   * <p>Unlike the other {@code EndpointPair}-accepting methods, this method does not throw if the
   * endpoints are unordered and the graph is directed; it simply returns {@code false}. This is for
   * consistency with {@link Graph#hasEdgeConnecting(EndpointPair)} and {@link
   * ValueGraph#hasEdgeConnecting(EndpointPair)}.
   *
   * @since 27.1
   */
  boolean hasEdgeConnecting(EndpointPair<N> endpoints);

  //
  // Network identity
  //

  /**
   * Returns {@code true} iff {@code object} is a {@link Network} that has the same elements and the
   * same structural relationships as those in this network.
   *
   * <p>Thus, two networks A and B are equal if <b>all</b> of the following are true:
   *
   * <ul>
   *   <li>A and B have equal {@link #isDirected() directedness}.
   *   <li>A and B have equal {@link #nodes() node sets}.
   *   <li>A and B have equal {@link #edges() edge sets}.
   *   <li>Every edge in A and B connects the same nodes in the same direction (if any).
   * </ul>
   *
   * <p>Network properties besides {@link #isDirected() directedness} do <b>not</b> affect equality.
   * For example, two networks may be considered equal even if one allows parallel edges and the
   * other doesn't. Additionally, the order in which nodes or edges are added to the network, and
   * the order in which they are iterated over, are irrelevant.
   *
   * <p>A reference implementation of this is provided by {@link AbstractNetwork#equals(Object)}.
   */
  @Override
  boolean equals(@Nullable Object object);

  /**
   * Returns the hash code for this network. The hash code of a network is defined as the hash code
   * of a map from each of its {@link #edges() edges} to their {@link #incidentNodes(Object)
   * incident nodes}.
   *
   * <p>A reference implementation of this is provided by {@link AbstractNetwork#hashCode()}.
   */
  @Override
  int hashCode();
}
