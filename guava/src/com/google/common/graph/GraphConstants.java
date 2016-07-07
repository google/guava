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

import com.google.common.math.DoubleMath;

import java.math.RoundingMode;

/**
 * A utility class to hold various constants used by the Guava Graph library.
 */
final class GraphConstants {

  private GraphConstants() {}

  static final int EXPECTED_DEGREE = 2;

  static final int DEFAULT_NODE_COUNT = 10;
  static final int DEFAULT_EDGE_COUNT = DEFAULT_NODE_COUNT * EXPECTED_DEGREE;

  // Load factor and capacity for "inner" (i.e. per node/edge element) hash sets or maps
  static final float INNER_LOAD_FACTOR = 1.0f;
  static final int INNER_CAPACITY = DoubleMath.roundToInt(
      (double) EXPECTED_DEGREE / INNER_LOAD_FACTOR, RoundingMode.CEILING);

  // Error messages
  static final String NODE_NOT_IN_GRAPH = "Node %s is not an element of this graph.";
  static final String EDGE_NOT_IN_GRAPH = "Edge %s is not an element of this graph.";
  static final String REUSING_EDGE =
      "Edge %s already exists between the following nodes: %s, "
          + "so it cannot be reused to connect the following nodes: %s.";
  static final String PARALLEL_EDGES_NOT_ALLOWED =
      "Nodes %s and %s are already connected by a different edge. To construct a graph "
          + "that allows parallel edges, call allowsParallelEdges(true) on the Builder.";
  static final String SELF_LOOPS_NOT_ALLOWED =
      "Cannot add self-loop edge on node %s, as self-loops are not allowed. To construct a graph "
          + "that allows self-loops, call allowsSelfLoops(true) on the Builder.";
  static final String NOT_AVAILABLE_ON_UNDIRECTED =
      "Cannot call source()/target() on undirected endpoints. Consider calling otherNode() to get "
          + "a single node or using the endpoints' iterator to get both nodes.";
  static final String ENDPOINTS_GRAPH_DIRECTEDNESS =
      "The endpoints' directedness (isDirected = %s) does not match the graph's directedness "
          + "(isDirected = %s). A graph cannot have both directed and undirected edges.";
  static final String EDGE_ALREADY_EXISTS = "Edge %s already exists in the graph.";
  static final String NETWORK_WITH_PARALLEL_EDGE =
      "Cannot make a Graph copy of a Network that allows parallel edges.";
}
