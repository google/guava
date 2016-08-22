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
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Set;

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
 * complexity they are: {@link BasicGraph}, {@link Graph}, and {@link Network}. You should generally
 * prefer the simplest interface that satisfies your use case.
 *
 * <p>To choose the right interface, answer these questions:
 *
 * <ol>
 * <li>Do you have data (objects) that you wish to associate with edges?
 *     <p>Yes: Go to question 2. No: Use {@link BasicGraph}.
 * <li>Are the objects you wish to associate with edges unique within the scope of a graph? That is,
 *     no two objects would be {@link Object#equals(Object) equal} to each other. A common example
 *     where this would <i>not</i> be the case is with weighted graphs.
 *     <p>Yes: Go to question 3. No: Use {@link Graph}.
 * <li>Do you need to be able to query the graph for an edge associated with a particular object?
 *     For example, do you need to query what nodes an edge associated with a particular object
 *     connects, or whether an edge associated with that object exists in the graph?
 *     <p>Yes: Use {@link Network}. No: Go to question 4.
 * <li>Do you need explicit support for parallel edges? For example, do you need to remove one edge
 *     connecting a pair of nodes while leaving other edges connecting those same nodes intact?
 *     <p>Yes: Use {@link Network}. No: Use {@link Graph}.
 * </ol>
 *
 * <p>In all three interfaces, nodes have all the same requirements as keys in a {@link Map}.
 *
 * <p>All mutation methods live on the subinterface {@link MutableBasicGraph}. If you do not need to
 * mutate a graph (e.g. if you write a method than runs a read-only algorithm on the graph), you
 * should prefer the non-mutating {@link BasicGraph} interface.
 *
 * <p>The {@link BasicGraph} interface extends {@link Graph}. When storing references, it is
 * preferable to store them as {@link BasicGraph}s so you do not have to worry about the value type.
 * However, when writing methods that operate on graphs but do not care about edge values, it is
 * preferable to accept {@code Graph<N, ?>} to allow the widest variety of valid input.
 *
 * <p>We provide an efficient implementation of this interface via {@link BasicGraphBuilder}. When
 * using the implementation provided, all {@link Set}-returning methods provide live, unmodifiable
 * views of the graph. In other words, you cannot add an element to the {@link Set}, but if an
 * element is added to the {@link BasicGraph} that would affect the result of that set, it will be
 * updated automatically. This also means that you cannot modify a {@link BasicGraph} in a way that
 * would affect a {#link Set} while iterating over that set. For example, you cannot remove the
 * nodes from a {@link BasicGraph} while iterating over {@link #nodes} (unless you first make a copy
 * of the nodes), just as you could not remove the keys from a {@link Map} while iterating over its
 * {@link Map#keySet()}. This will either throw a {@link ConcurrentModificationException} or risk
 * undefined behavior.
 *
 * <p>Example of use:
 *
 * <pre><code>
 * MutableBasicGraph<String> managementGraph = BasicGraphBuilder.directed().build();
 * managementGraph.putEdge("Big Boss", "Middle Manager Jack");
 * managementGraph.putEdge("Big Boss", "Middle Manager Jill");
 * managementGraph.putEdge("Middle Manager Jack", "Joe");
 * managementGraph.putEdge("Middle Manager Jack", "Schmoe");
 * managementGraph.putEdge("Middle Manager Jill", "Jane");
 * managementGraph.putEdge("Middle Manager Jill", "Doe");
 * for (String employee : managementGraph.nodes()) {
 *   Set<String> reports = managementGraph.successors(employee);
 *   if (!reports.isEmpty()) {
 *     System.out.format("%s has the following direct reports: %s%n", employee, reports);
 *   }
 * }
 * </code></pre>
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @since 20.0
 */
@Beta
public interface BasicGraph<N> extends Graph<N, BasicGraph.Presence> {

  /**
   * A placeholder for the (generally ignored) value type of a {@link BasicGraph}. Users shouldn't
   * have to reference this enum unless they are implementing the {@link BasicGraph} interface.
   */
  public enum Presence {
    EDGE_EXISTS
  }
}
