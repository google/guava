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

package com.google.common.graph.testing;

import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;

/**
 * This class is useful for fluently building an immutable graph in tests. Example usage:
 * <pre><code>
 * // Constructs the following graph: (A)    (B)--->(C)
 * private static final ImmutableNetwork<String, String> GRAPH =
 *     TestNetworkBuilder.<String, String>init(NetworkBuilder.directed())
 *         .addNode("A")
 *         .addNode("B")
 *         .addNode("C")
 *         .addEdge("B->C", "B", "C")
 *         .toImmutableNetwork();
 * </code></pre>
 */
public final class TestNetworkBuilder<N, E> {
  private final MutableNetwork<N, E> graph;

  private TestNetworkBuilder(MutableNetwork<N, E> graph) {
    this.graph = graph;
  }

  public static <N, E> TestNetworkBuilder<N, E> init(NetworkBuilder<? super N, ? super E> builder) {
    return new TestNetworkBuilder<N, E>(builder.<N, E>build());
  }

  public TestNetworkBuilder<N, E> addNode(N node) {
    graph.addNode(node);
    return this;
  }

  public TestNetworkBuilder<N, E> addEdge(E edge, N node1, N node2) {
    graph.addEdge(edge, node1, node2);
    return this;
  }

  public ImmutableNetwork<N, E> toImmutableNetwork() {
    return ImmutableNetwork.copyOf(graph);
  }
}
