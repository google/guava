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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.graph.testing.TestGraphBuilder;
import com.google.common.testing.AbstractPackageSanityTests;

import junit.framework.AssertionFailedError;

/**
 * Covers basic sanity checks for the entire package.
 *
 * @author Kurt Alfred Kluever
 */

public class PackageSanityTests extends AbstractPackageSanityTests {

  private static final GraphBuilder<?, ?> BUILDER_A =
      GraphBuilder.directed().allowsParallelEdges(true).expectedNodeCount(10);
  private static final GraphBuilder<?, ?> BUILDER_B =
      GraphBuilder.directed().allowsSelfLoops(false).expectedNodeCount(16);

  private static final ImmutableGraph<String, String> IMMUTABLE_GRAPH_A =
      TestGraphBuilder.<String, String>init(GraphBuilder.directed())
          .addNode("A")
          .toImmutableGraph();
  private static final ImmutableGraph<String, String> IMMUTABLE_GRAPH_B =
      TestGraphBuilder.<String, String>init(GraphBuilder.directed())
          .addNode("B")
          .toImmutableGraph();

  public PackageSanityTests() {
    setDistinctValues(GraphBuilder.class, BUILDER_A, BUILDER_B);
    setDistinctValues(Graph.class, IMMUTABLE_GRAPH_A, IMMUTABLE_GRAPH_B);

    // We override AbstractPackageSanityTests's equality testing of mutable graphs by defining
    // testEquals() methods in ConfigurableUndirectedGraphTest and ConfigurableDirectedGraphTest.
    // If we don't define testEquals(), the tool tries to automatically create non-equal, mutable
    // graphs by passing different instances of GraphBuilder into their constructors. However,
    // the GraphBuilder instances are *not* used to determine equality for mutable graphs.
    // Therefore, the tool ends up creating 2 equal mutable instances and it causes failures.
    // However, the tool is still checking the nullability contracts of the mutable graphs.
  }

  @Override
  public void testNulls() throws Exception {
    try {
      super.testNulls();
    } catch (AssertionFailedError e) {
      assertThat(e.getCause().getMessage()).contains(AbstractGraphTest.ERROR_ELEMENT_NOT_IN_GRAPH);
    }
  }
}
