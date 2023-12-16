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


/** A utility class to hold various constants used by the Guava Graph library. */
@ElementTypesAreNonnullByDefault
final class GraphConstants {

  private GraphConstants() {}

  static final int EXPECTED_DEGREE = 2;

  static final int DEFAULT_NODE_COUNT = 10;
  static final int DEFAULT_EDGE_COUNT = DEFAULT_NODE_COUNT * EXPECTED_DEGREE;

  // Load factor and capacity for "inner" (i.e. per node/edge element) hash sets or maps
  static final float INNER_LOAD_FACTOR = 1.0f;
  static final int INNER_CAPACITY = 2; // ceiling(EXPECTED_DEGREE / INNER_LOAD_FACTOR)

  // Error messages
  static final String NODE_NOT_IN_GRAPH = "Node %s is not an element of this graph.";
  static final String EDGE_NOT_IN_GRAPH = "Edge %s is not an element of this graph.";
  static final String NODE_REMOVED_FROM_GRAPH =
      "Node %s that was used to generate this set is no longer in the graph.";
  static final String NODE_PAIR_REMOVED_FROM_GRAPH =
      "Node %s or node %s that were used to generate this set are no longer in the graph.";
  static final String EDGE_REMOVED_FROM_GRAPH =
      "Edge %s that was used to generate this set is no longer in the graph.";
  static final String REUSING_EDGE =
      "Edge %s already exists between the following nodes: %s, "
          + "so it cannot be reused to connect the following nodes: %s.";
  static final String MULTIPLE_EDGES_CONNECTING =
      "Cannot call edgeConnecting() when parallel edges exist between %s and %s. Consider calling "
          + "edgesConnecting() instead.";
  static final String PARALLEL_EDGES_NOT_ALLOWED =
      "Nodes %s and %s are already connected by a different edge. To construct a graph "
          + "that allows parallel edges, call allowsParallelEdges(true) on the Builder.";
  static final String SELF_LOOPS_NOT_ALLOWED =
      "Cannot add self-loop edge on node %s, as self-loops are not allowed. To construct a graph "
          + "that allows self-loops, call allowsSelfLoops(true) on the Builder.";
  static final String NOT_AVAILABLE_ON_UNDIRECTED =
      "Cannot call source()/target() on a EndpointPair from an undirected graph. Consider calling "
          + "adjacentNode(node) if you already have a node, or nodeU()/nodeV() if you don't.";
  static final String EDGE_ALREADY_EXISTS = "Edge %s already exists in the graph.";
  static final String ENDPOINTS_MISMATCH =
      "Mismatch: endpoints' ordering is not compatible with directionality of the graph";

  /** Singleton edge value for {@link Graph} implementations backed by {@link ValueGraph}s. */
  enum Presence {
    EDGE_EXISTS
  }
}
