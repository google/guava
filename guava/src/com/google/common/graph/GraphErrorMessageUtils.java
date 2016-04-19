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

/**
 * A utility class for graph error messages.
 */
final class GraphErrorMessageUtils {

  private GraphErrorMessageUtils() {}

  static final String NODE_NOT_IN_GRAPH = "Node %s is not an element of this graph";
  static final String EDGE_NOT_IN_GRAPH = "Edge %s is not an element of this graph";
  static final String REUSING_EDGE =
      "Edge %s already exists between the "
          + "following nodes: %s, so it can't be reused to connect node %s to %s";
  static final String ADDING_PARALLEL_EDGE =
      "Nodes %s and %s are already connected by a different edge.";
  static final String SELF_LOOPS_NOT_ALLOWED =
      "Can't add self-loop edge on node %s, as self-loops are not allowed.";
  static final String NOT_AVAILABLE_ON_UNDIRECTED =
      "Cannot call source()/target() on an undirected graph. "
          + "Consider using incidentNodes() (if you don't know either incident node) "
          + "or Graphs.oppositeNode() (if you know one of the incident nodes).";
}
