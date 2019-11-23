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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ImmutableGraph}. */
@RunWith(JUnit4.class)
public class ImmutableGraphTest {

  @Test
  public void immutableGraph() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.addNode("A");
    ImmutableGraph<String> immutableGraph = ImmutableGraph.copyOf(mutableGraph);

    assertThat(immutableGraph).isNotInstanceOf(MutableValueGraph.class);
    assertThat(immutableGraph).isEqualTo(mutableGraph);

    mutableGraph.addNode("B");
    assertThat(immutableGraph).isNotEqualTo(mutableGraph);
  }

  @Test
  public void copyOfImmutableGraph_optimized() {
    Graph<String> graph1 = ImmutableGraph.copyOf(GraphBuilder.directed().<String>build());
    Graph<String> graph2 = ImmutableGraph.copyOf(graph1);

    assertThat(graph2).isSameInstanceAs(graph1);
  }

  @Test
  public void immutableGraphBuilder_appliesGraphBuilderConfig() {
    ImmutableGraph<String> emptyGraph =
        GraphBuilder.directed()
            .allowsSelfLoops(true)
            .nodeOrder(ElementOrder.<String>natural())
            .immutable()
            .build();

    assertThat(emptyGraph.isDirected()).isTrue();
    assertThat(emptyGraph.allowsSelfLoops()).isTrue();
    assertThat(emptyGraph.nodeOrder()).isEqualTo(ElementOrder.<String>natural());
  }

  /**
   * Tests that the ImmutableGraph.Builder doesn't change when the creating GraphBuilder changes.
   */
  @Test
  @SuppressWarnings("CheckReturnValue")
  public void immutableGraphBuilder_copiesGraphBuilder() {
    GraphBuilder<String> graphBuilder =
        GraphBuilder.directed()
            .allowsSelfLoops(true)
            .<String>nodeOrder(ElementOrder.<String>natural());
    ImmutableGraph.Builder<String> immutableGraphBuilder = graphBuilder.immutable();

    // Update GraphBuilder, but this shouldn't impact immutableGraphBuilder
    graphBuilder.allowsSelfLoops(false).nodeOrder(ElementOrder.<String>unordered());

    ImmutableGraph<String> emptyGraph = immutableGraphBuilder.build();

    assertThat(emptyGraph.isDirected()).isTrue();
    assertThat(emptyGraph.allowsSelfLoops()).isTrue();
    assertThat(emptyGraph.nodeOrder()).isEqualTo(ElementOrder.<String>natural());
  }

  @Test
  public void immutableGraphBuilder_addNode() {
    ImmutableGraph<String> graph = GraphBuilder.directed().<String>immutable().addNode("A").build();

    assertThat(graph.nodes()).containsExactly("A");
    assertThat(graph.edges()).isEmpty();
  }

  @Test
  public void immutableGraphBuilder_putEdgeFromNodes() {
    ImmutableGraph<String> graph =
        GraphBuilder.directed().<String>immutable().putEdge("A", "B").build();

    assertThat(graph.nodes()).containsExactly("A", "B");
    assertThat(graph.edges()).containsExactly(EndpointPair.ordered("A", "B"));
  }

  @Test
  public void immutableGraphBuilder_putEdgeFromEndpointPair() {
    ImmutableGraph<String> graph =
        GraphBuilder.directed().<String>immutable().putEdge(EndpointPair.ordered("A", "B")).build();

    assertThat(graph.nodes()).containsExactly("A", "B");
    assertThat(graph.edges()).containsExactly(EndpointPair.ordered("A", "B"));
  }
}
