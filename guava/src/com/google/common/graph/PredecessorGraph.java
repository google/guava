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

/**
 * A functional interface for <a
 * href="https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)">graph</a>-structured data.
 *
 * <p>A graph is composed of a set of nodes and a set of edges connecting pairs of nodes.
 *
 * <p>There are three main interfaces provided to represent graphs. In order of increasing
 * complexity they are: {@link Graph}, {@link ValueGraph}, and {@link Network}. You should generally
 * prefer the simplest interface that satisfies your use case. See the <a
 * href="https://github.com/google/guava/wiki/GraphsExplained#choosing-the-right-graph-type">
 * "Choosing the right graph type"</a> section of the Guava User Guide for more details.
 *
 * <h3>Usage</h3>
 *
 * Some graph algorithms only care about the nodes that are adjacent to a node but not about the
 * type of edges (e.g. topological sort). These algorithms should prefer working with {@code
 * PredecessorGraph} rather than a more specialized type.
 *
 * <p>When calling a method that requires a {@code PredecessorGraph}, you can use {@link Graph},
 * {@link ValueGraph}, {@link Network} or implement this interface for an already existing data
 * structure.
 *
 * <h3>Additional documentation</h3>
 *
 * <p>See the Guava User Guide for the {@code common.graph} package (<a
 * href="https://github.com/google/guava/wiki/GraphsExplained">"Graphs Explained"</a>) for
 * additional documentation, including <a
 * href="https://github.com/google/guava/wiki/GraphsExplained#notes-for-implementors">notes for
 * implementors</a>
 *
 * @author Joshua O'Madadhain
 * @author Jens Nyman
 * @param <N> Node parameter type
 * @since 22.0
 */
// TODO(b/35456940): Update the documentation to reflect the new interfaces
@Beta
public interface PredecessorGraph<N> {

  /**
   * Returns all nodes in this graph adjacent to {@code node} which can be reached by traversing
   * {@code node}'s incoming edges <i>against</i> the direction (if any) of the edge.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  Set<N> predecessors(@CompatibleWith("N") Object node);
}
