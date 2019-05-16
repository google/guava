/*
 * Copyright (C) 2019 The Guava Authors
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

/** Tests for {@link ImmutableValueGraph} . */
@RunWith(JUnit4.class)
public class ImmutableValueGraphTest {

  @Test
  public void immutableValueGraph() {
    MutableValueGraph<String, Integer> mutableValueGraph = ValueGraphBuilder.directed().build();
    mutableValueGraph.addNode("A");
    ImmutableValueGraph<String, Integer> immutableValueGraph =
        ImmutableValueGraph.copyOf(mutableValueGraph);

    assertThat(immutableValueGraph.asGraph()).isInstanceOf(ImmutableGraph.class);
    assertThat(immutableValueGraph).isNotInstanceOf(MutableValueGraph.class);
    assertThat(immutableValueGraph).isEqualTo(mutableValueGraph);

    mutableValueGraph.addNode("B");
    assertThat(immutableValueGraph).isNotEqualTo(mutableValueGraph);
  }

  @Test
  public void copyOfImmutableValueGraph_optimized() {
    ValueGraph<String, Integer> graph1 =
        ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().<String, Integer>build());
    ValueGraph<String, Integer> graph2 = ImmutableValueGraph.copyOf(graph1);

    assertThat(graph2).isSameInstanceAs(graph1);
  }

  @Test
  public void immutableValueGraphBuilder_appliesGraphBuilderConfig() {
    ImmutableValueGraph<String, Integer> emptyGraph =
        ValueGraphBuilder.directed()
            .allowsSelfLoops(true)
            .nodeOrder(ElementOrder.<String>natural())
            .<String, Integer>immutable()
            .build();

    assertThat(emptyGraph.isDirected()).isTrue();
    assertThat(emptyGraph.allowsSelfLoops()).isTrue();
    assertThat(emptyGraph.nodeOrder()).isEqualTo(ElementOrder.<String>natural());
  }

  /**
   * Tests that the ImmutableValueGraph.Builder doesn't change when the creating ValueGraphBuilder
   * changes.
   */
  @Test
  @SuppressWarnings("CheckReturnValue")
  public void immutableValueGraphBuilder_copiesGraphBuilder() {
    ValueGraphBuilder<String, Object> graphBuilder =
        ValueGraphBuilder.directed()
            .allowsSelfLoops(true)
            .<String>nodeOrder(ElementOrder.<String>natural());
    ImmutableValueGraph.Builder<String, Integer> immutableValueGraphBuilder =
        graphBuilder.<String, Integer>immutable();

    // Update ValueGraphBuilder, but this shouldn't impact immutableValueGraphBuilder
    graphBuilder.allowsSelfLoops(false).nodeOrder(ElementOrder.<String>unordered());

    ImmutableValueGraph<String, Integer> emptyGraph = immutableValueGraphBuilder.build();

    assertThat(emptyGraph.isDirected()).isTrue();
    assertThat(emptyGraph.allowsSelfLoops()).isTrue();
    assertThat(emptyGraph.nodeOrder()).isEqualTo(ElementOrder.<String>natural());
  }

  @Test
  public void immutableValueGraphBuilder_addNode() {
    ImmutableValueGraph<String, Integer> graph =
        ValueGraphBuilder.directed().<String, Integer>immutable().addNode("A").build();

    assertThat(graph.nodes()).containsExactly("A");
    assertThat(graph.edges()).isEmpty();
  }

  @Test
  public void immutableValueGraphBuilder_putEdgeFromNodes() {
    ImmutableValueGraph<String, Integer> graph =
        ValueGraphBuilder.directed()
            .<String, Integer>immutable()
            .putEdgeValue("A", "B", 10)
            .build();

    assertThat(graph.nodes()).containsExactly("A", "B");
    assertThat(graph.edges()).containsExactly(EndpointPair.ordered("A", "B"));
    assertThat(graph.edgeValueOrDefault("A", "B", null)).isEqualTo(10);
  }

  @Test
  public void immutableValueGraphBuilder_putEdgeFromEndpointPair() {
    ImmutableValueGraph<String, Integer> graph =
        ValueGraphBuilder.directed()
            .<String, Integer>immutable()
            .putEdgeValue(EndpointPair.ordered("A", "B"), 10)
            .build();

    assertThat(graph.nodes()).containsExactly("A", "B");
    assertThat(graph.edges()).containsExactly(EndpointPair.ordered("A", "B"));
    assertThat(graph.edgeValueOrDefault("A", "B", null)).isEqualTo(10);
  }
}
