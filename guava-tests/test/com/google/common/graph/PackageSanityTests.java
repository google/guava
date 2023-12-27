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

import static com.google.common.graph.TestUtil.ERROR_ELEMENT_NOT_IN_GRAPH;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.testing.AbstractPackageSanityTests;
import junit.framework.AssertionFailedError;

/**
 * Covers basic sanity checks for the entire package.
 *
 * @author Kurt Alfred Kluever
 */

public class PackageSanityTests extends AbstractPackageSanityTests {

  private static final AbstractGraphBuilder<?> GRAPH_BUILDER_A =
      GraphBuilder.directed().expectedNodeCount(10);
  private static final AbstractGraphBuilder<?> GRAPH_BUILDER_B =
      ValueGraphBuilder.directed().allowsSelfLoops(true).expectedNodeCount(16);

  private static final ImmutableGraph<String> IMMUTABLE_GRAPH_A =
      GraphBuilder.directed().<String>immutable().addNode("A").build();
  private static final ImmutableGraph<String> IMMUTABLE_GRAPH_B =
      GraphBuilder.directed().<String>immutable().addNode("B").build();

  private static final NetworkBuilder<?, ?> NETWORK_BUILDER_A =
      NetworkBuilder.directed().allowsParallelEdges(true).expectedNodeCount(10);
  private static final NetworkBuilder<?, ?> NETWORK_BUILDER_B =
      NetworkBuilder.directed().allowsSelfLoops(true).expectedNodeCount(16);

  private static final ImmutableNetwork<String, String> IMMUTABLE_NETWORK_A =
      NetworkBuilder.directed().<String, String>immutable().addNode("A").build();
  private static final ImmutableNetwork<String, String> IMMUTABLE_NETWORK_B =
      NetworkBuilder.directed().<String, String>immutable().addNode("B").build();

  public PackageSanityTests() {
    MutableNetwork<String, String> mutableNetworkA = NetworkBuilder.directed().build();
    mutableNetworkA.addNode("a");
    MutableNetwork<String, String> mutableNetworkB = NetworkBuilder.directed().build();
    mutableNetworkB.addNode("b");

    setDistinctValues(AbstractGraphBuilder.class, GRAPH_BUILDER_A, GRAPH_BUILDER_B);
    setDistinctValues(Graph.class, IMMUTABLE_GRAPH_A, IMMUTABLE_GRAPH_B);
    setDistinctValues(MutableNetwork.class, mutableNetworkA, mutableNetworkB);
    setDistinctValues(NetworkBuilder.class, NETWORK_BUILDER_A, NETWORK_BUILDER_B);
    setDistinctValues(Network.class, IMMUTABLE_NETWORK_A, IMMUTABLE_NETWORK_B);
    setDefault(EndpointPair.class, EndpointPair.ordered("A", "B"));
  }

  @Override
  public void testNulls() throws Exception {
    try {
      super.testNulls();
    } catch (AssertionFailedError e) {
      assertWithMessage("Method did not throw null pointer OR element not in graph exception.")
          .that(e)
          .hasCauseThat()
          .hasMessageThat()
          .contains(ERROR_ELEMENT_NOT_IN_GRAPH);
    }
  }
}
