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

/**
 * A utility class to hold various constants used by the Guava Graph library.
 */
final class GraphConstants {

  private GraphConstants() {}

  // TODO(user): Enable users to specify the expected degree of nodes.
  static final int EXPECTED_DEGREE = 2;

  static final int DEFAULT_NODE_COUNT = 10;
  static final int DEFAULT_EDGE_COUNT = DEFAULT_NODE_COUNT * EXPECTED_DEGREE;

  // Error messages
  static final String NODE_NOT_IN_GRAPH = "Node %s is not an element of this graph.";
  static final String EDGE_NOT_IN_GRAPH = "Edge %s is not an element of this graph.";
  static final String REUSING_EDGE =
      "Edge %s already exists between the following nodes: %s, "
          + "so it can't be reused to connect the following nodes: %s.";
  static final String ADDING_PARALLEL_EDGE =
      "Nodes %s and %s are already connected by a different edge.";
  static final String EDGE_ALREADY_EXISTS = "Edge %s already exists in the graph.";
  static final String SELF_LOOPS_NOT_ALLOWED =
      "Cannot add self-loop edge on node %s, as self-loops are not allowed.";
  static final String NOT_AVAILABLE_ON_UNDIRECTED =
      "Cannot call source()/target() on undirected endpoints.";
  static final String ENDPOINTS_GRAPH_DIRECTEDNESS =
      "The endpoints' directedness (isDirected = %s) does not match the graph's directedness "
          + "(isDirected = %s). A graph cannot have both directed and undirected edges.";
  static final String NETWORK_WITH_PARALLEL_EDGE =
      "Cannot make a Graph copy of a Network that allows parallel edges.";
}
