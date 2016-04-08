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

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;

/**
 * This class is useful for fluently building an immutable graph in tests. Example usage:
 * <pre><code>
 * // Constructs the following graph: (A)    (B)--->(C)
 * private static final ImmutableGraph<String, String> GRAPH =
 *     TestGraphBuilder.<String, String>init(GraphBuilder.directed())
 *         .addNode("A")
 *         .addNode("B")
 *         .addNode("C")
 *         .addEdge("B->C", "B", "C")
 *         .toImmutableGraph();
 * </code></pre>
 */
public final class TestGraphBuilder<N, E> {
  private final Graph<N, E> graph;

  private TestGraphBuilder(Graph<N, E> graph) {
    this.graph = graph;
  }

  public static <N, E> TestGraphBuilder<N, E> init(GraphBuilder<? super N, ? super E> builder) {
    return new TestGraphBuilder<N, E>(builder.<N, E>build());
  }

  public TestGraphBuilder<N, E> addNode(N node) {
    graph.addNode(node);
    return this;
  }

  public TestGraphBuilder<N, E> addEdge(E edge, N node1, N node2) {
    graph.addEdge(edge, node1, node2);
    return this;
  }

  public ImmutableGraph<N, E> toImmutableGraph() {
    return ImmutableGraph.copyOf(graph);
  }
}
