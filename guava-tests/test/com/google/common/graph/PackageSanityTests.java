/*
 * Copyright (C) 2015 The Guava Authors
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

import com.google.common.testing.AbstractPackageSanityTests;

/**
 * Covers basic sanity checks for the entire package.
 *
 * @author Kurt Alfred Kluever
 */

public class PackageSanityTests extends AbstractPackageSanityTests {

  private static final GraphConfig CONFIG_A = Graphs.config().multigraph().expectedNodeCount(10);
  private static final GraphConfig CONFIG_B = Graphs.config().noSelfLoops().expectedNodeCount(16);

  private static final ImmutableDirectedGraph<String, String> IMMUTABLE_DIRECTED_A =
      ImmutableDirectedGraph.<String, String>builder().addNode("A").build();
  private static final ImmutableDirectedGraph<String, String> IMMUTABLE_DIRECTED_B =
      ImmutableDirectedGraph.<String, String>builder().addNode("B").build();

  private static final ImmutableUndirectedGraph<String, String> IMMUTABLE_UNDIRECTED_A =
      ImmutableUndirectedGraph.<String, String>builder().addNode("A").build();
  private static final ImmutableUndirectedGraph<String, String> IMMUTABLE_UNDIRECTED_B =
      ImmutableUndirectedGraph.<String, String>builder().addNode("B").build();

  public PackageSanityTests() {
    setDistinctValues(GraphConfig.class, CONFIG_A, CONFIG_B);

    setDistinctValues(DirectedGraph.class, IMMUTABLE_DIRECTED_A, IMMUTABLE_DIRECTED_B);
    setDistinctValues(UndirectedGraph.class, IMMUTABLE_UNDIRECTED_A, IMMUTABLE_UNDIRECTED_B);

    // We override AbstractPackageSanityTests's equality testing of mutable graphs by defining
    // testEquals() methods in IncidenceSetUndirectedGraphTest and IncidenceSetDirectedGraphTest.
    // If we don't define testEquals(), the tool tries to automatically create non-equal, mutable
    // graphs by passing different instances of GraphConfig into their constructors. However,
    // the GraphConfig instances are *not* used to determine equality for mutable graphs. Therefore,
    // the tool ends up creating 2 equal mutable instances and it causes failures.
    // However, the tool is still checking the nullability contracts of the mutable graphs.
  }
}
