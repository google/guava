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
import java.util.ConcurrentModificationException;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * An interface for <a href="https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)">graph</a>
 * data structures. A graph is composed of a set of nodes (sometimes called vertices) and a set of
 * edges connecting pairs of nodes. Graphs are useful for modeling many kinds of relations. If the
 * relation to be modeled is symmetric (such as "distance between cities"), that can be represented
 * with an undirected graph, where an edge that connects node A to node B also connects node B to
 * node A. If the relation to be modeled is asymmetric (such as "employees managed"), that can be
 * represented with a directed graph, where edges are strictly one-way.
 *
 * <p>There are three main interfaces provided to represent graphs. In order of increasing
 * complexity they are: {@link Graph}, {@link ValueGraph}, and {@link Network}. You should generally
 * prefer the simplest interface that satisfies your use case.
 *
 * <p>To choose the right interface, answer these questions:
 *
 * <ol>
 * <li>Do you have data (objects) that you wish to associate with edges?
 *     <p>Yes: Go to question 2. No: Use {@link Graph}.
 * <li>Are the objects you wish to associate with edges unique within the scope of a graph? That is,
 *     no two objects would be {@link Object#equals(Object) equal} to each other. A common example
 *     where this would <i>not</i> be the case is with weighted graphs.
 *     <p>Yes: Go to question 3. No: Use {@link ValueGraph}.
 * <li>Do you need to be able to query the graph for an edge associated with a particular object?
 *     For example, do you need to query what nodes an edge associated with a particular object
 *     connects, or whether an edge associated with that object exists in the graph?
 *     <p>Yes: Use {@link Network}. No: Go to question 4.
 * <li>Do you need explicit support for parallel edges? For example, do you need to remove one edge
 *     connecting a pair of nodes while leaving other edges connecting those same nodes intact?
 *     <p>Yes: Use {@link Network}. No: Use {@link ValueGraph}.
 * </ol>
 *
 * <p>Although {@link MutableValueGraph} and {@link MutableNetwork} both require users to provide
 * objects to associate with edges when adding them, the differentiating factor is that in {@link
 * ValueGraph}s, these objects can be any arbitrary data. Like the values in a {@link Map}, they do
 * not have to be unique, and can be mutated while in the graph. In a {@link Network}, these objects
 * serve as keys into the data structure. Like the keys in a {@link Map}, they must be unique, and
 * cannot be mutated in a way that affects their equals/hashcode or the data structure will become
 * corrupted.
 *
 * <p>In all three interfaces, nodes have all the same requirements as keys in a {@link Map}.
 *
 * <p>The {@link Graph} interface does not support parallel {@link #edges()}, and forbids
 * implementations or extensions with parallel edges. It is possible to encode a notion of edge
 * multiplicity into the values of a {@ValueGraph} (e.g. with an integer or a list of values), but
 * this will not be reflected in methods such as {@link Graph#degree(Object)}. For that
 * functionality, use {@link Network}s.
 *
 * <p>All mutation methods live on the subinterface {@link MutableValueGraph}. If you do not need to
 * mutate a graph (e.g. if you write a method than runs a read-only algorithm on the graph), you
 * should prefer the non-mutating {@link ValueGraph} interface.
 *
 * <p>We provide an efficient implementation of this interface via {@link ValueGraphBuilder}. When
 * using the implementation provided, all collection-returning methods provide live, unmodifiable
 * views of the graph. In other words, you cannot add an element to the collection, but if an
 * element is added to the {@link ValueGraph} that would affect the collection, the collection will
 * be updated automatically. This also means that you cannot mutate a {@link ValueGraph} in a way
 * that would affect a collection while iterating over that collection. For example, you cannot
 * remove either {@code foo} or any successors of {@code foo} from the graph while iterating over
 * {@code successors(foo)} (unless you first make a copy of the successors), just as you could not
 * remove keys from a {@link Map} while iterating over its {@link Map#keySet()}. Behavior in such a
 * case is undefined, and may result in {@link ConcurrentModificationException}.
 *
 * <p>Example of use:
 *
 * <pre><code>
 * MutableGraph<String, Double> synonymGraph = GraphBuilder.undirected().build();
 * synonymGraph.putEdgeValue("large", "big", 0.9);
 * synonymGraph.putEdgeValue("large", "huge", 0.9);
 * synonymGraph.putEdgeValue("large", "grand", 0.6);
 * synonymGraph.putEdgeValue("large", "cold", 0.0);
 * synonymGraph.putEdgeValue("large", "small", -1.0);
 * for (String word : synonymGraph.adjacentNodes("large")) {
 *   if (synonymGraph.edgeValue(word, "large") > 0.5) {
 *     System.out.println(word + " is a synonym for large");
 *   }
 * }
 * </code></pre>
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
 */
@Beta
public interface ValueGraph<N, V> extends Graph<N> {

  /**
   * If there is an edge connecting {@code nodeU} to {@code nodeV}, returns the non-null value
   * associated with that edge.
   *
   * <p>In an undirected graph, this is equal to {@code edgeValue(nodeV, nodeU)}.
   *
   * @throws IllegalArgumentException if there is no edge connecting {@code nodeU} to {@code nodeV},
   *     or if {@code nodeU} or {@code nodeV} is not an element of this graph
   */
  V edgeValue(Object nodeU, Object nodeV);

  /**
   * Returns a {@link Map} of all {@link #edges() edges} mapped to their associated {@link
   * #edgeValue(Object, Object) value}.
   *
   * <p>Note: {@link Map#get(Object)} returns null if you supply an {@link EndpointPair} whose nodes
   * are not connected in this graph. This contrasts with the behavior of {@link #edgeValue(Object,
   * Object)}, which throws {@link IllegalArgumentException} in that case.
   */
  Map<EndpointPair<N>, V> edgeValues();

  //
  // ValueGraph identity
  //

  /**
   * For the default {@link ValueGraph} implementations, returns true iff {@code this == object}
   * (i.e. reference equality). External implementations are free to define this method as they see
   * fit, as long as they satisfy the {@link Object#equals(Object)} contract.
   *
   * <p>To compare two {@link ValueGraph}s based on their contents rather than their references, see
   * {@link Graphs#equivalent(ValueGraph, ValueGraph)}.
   */
  @Override
  boolean equals(@Nullable Object object);

  /**
   * For the default {@link ValueGraph} implementations, returns {@code
   * System.identityHashCode(this)}. External implementations are free to define this method as they
   * see fit, as long as they satisfy the {@link Object#hashCode()} contract.
   */
  @Override
  int hashCode();
}
