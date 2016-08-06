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

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;

/**
 * This class is useful for fluently building an immutable graph in tests. Example usage:
 * <pre><code>
 * // Constructs the following graph: (A)    (B)--->(C)
 * private static final ImmutableGraph<String> GRAPH =
 *     TestGraphBuilder.<String>init(GraphBuilder.directed())
 *         .addNode("A")
 *         .addNode("B")
 *         .addNode("C")
 *         .addEdge("B", "C")
 *         .toImmutableGraph();
 * </code></pre>
 */
public final class TestGraphBuilder<N> {
  private final MutableGraph<N> graph;

  private TestGraphBuilder(MutableGraph<N> graph) {
    this.graph = graph;
  }

  public static <N> TestGraphBuilder<N> init(GraphBuilder<? super N> builder) {
    return new TestGraphBuilder<N>(builder.<N>build());
  }

  public TestGraphBuilder<N> addNode(N node) {
    graph.addNode(node);
    return this;
  }

  public TestGraphBuilder<N> addEdge(N nodeA, N nodeB) {
    graph.putEdge(nodeA, nodeB);
    return this;
  }

  public ImmutableGraph<N> toImmutableGraph() {
    return ImmutableGraph.copyOf(graph);
  }
}
