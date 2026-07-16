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
import org.jspecify.annotations.NullUnmarked;

/**
 * Covers basic sanity checks for the entire package.
 *
 * @author Kurt Alfred Kluever
 */

@NullUnmarked
public class PackageSanityTest extends AbstractPackageSanityTests {

  private static final AbstractGraphBuilder<?> graphBuilderA =
      GraphBuilder.directed().expectedNodeCount(10);
  private static final AbstractGraphBuilder<?> graphBuilderB =
      ValueGraphBuilder.directed().allowsSelfLoops(true).expectedNodeCount(16);

  private static final ImmutableGraph<String> IMMUTABLE_GRAPH_A =
      GraphBuilder.directed().<String>immutable().addNode("A").build();
  private static final ImmutableGraph<String> IMMUTABLE_GRAPH_B =
      GraphBuilder.directed().<String>immutable().addNode("B").build();

  private static final NetworkBuilder<?, ?> networkBuilderA =
      NetworkBuilder.directed().allowsParallelEdges(true).expectedNodeCount(10);
  private static final NetworkBuilder<?, ?> networkBuilderB =
      NetworkBuilder.directed().allowsSelfLoops(true).expectedNodeCount(16);

  private static final ImmutableNetwork<String, String> IMMUTABLE_NETWORK_A =
      NetworkBuilder.directed().<String, String>immutable().addNode("A").build();
  private static final ImmutableNetwork<String, String> IMMUTABLE_NETWORK_B =
      NetworkBuilder.directed().<String, String>immutable().addNode("B").build();

  private static final ImmutableValueGraph<String, String> IMMUTABLE_VALUE_GRAPH_A =
      ValueGraphBuilder.directed().<String, String>immutable().addNode("A").build();
  private static final ImmutableValueGraph<String, String> IMMUTABLE_VALUE_GRAPH_B =
      ValueGraphBuilder.directed().<String, String>immutable().addNode("B").build();

  public PackageSanityTest() {
    MutableNetwork<String, String> mutableNetworkA = NetworkBuilder.directed().build();
    mutableNetworkA.addNode("a");
    MutableNetwork<String, String> mutableNetworkB = NetworkBuilder.directed().build();
    mutableNetworkB.addNode("b");

    MutableValueGraph<String, String> mutableValueGraphA = ValueGraphBuilder.directed().build();
    mutableValueGraphA.addNode("a");
    MutableValueGraph<String, String> mutableValueGraphB = ValueGraphBuilder.directed().build();
    mutableValueGraphB.addNode("b");

    setDistinctValues(AbstractGraphBuilder.class, graphBuilderA, graphBuilderB);
    setDistinctValues(Graph.class, IMMUTABLE_GRAPH_A, IMMUTABLE_GRAPH_B);
    setDistinctValues(ValueGraph.class, IMMUTABLE_VALUE_GRAPH_A, IMMUTABLE_VALUE_GRAPH_B);
    setDistinctValues(MutableNetwork.class, mutableNetworkA, mutableNetworkB);
    setDistinctValues(MutableValueGraph.class, mutableValueGraphA, mutableValueGraphB);
    setDistinctValues(NetworkBuilder.class, networkBuilderA, networkBuilderB);
    setDistinctValues(Network.class, IMMUTABLE_NETWORK_A, IMMUTABLE_NETWORK_B);
    setDistinctValues(Object.class, "A", "B");
    setDefault(EndpointPair.class, EndpointPair.ordered("A", "B"));
  }
}
