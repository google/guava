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

/**
 * Tests for {@link ImmutableGraph} and {@link ImmutableBasicGraph}.
 */
@RunWith(JUnit4.class)
public class ImmutableGraphTest {

  @Test
  public void immutableGraph() {
    MutableGraph<String, Integer> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.addNode("A");
    Graph<String, Integer> immutableGraph = ImmutableGraph.copyOf(mutableGraph);

    assertThat(immutableGraph).isNotInstanceOf(MutableGraph.class);
    assertThat(immutableGraph).isEqualTo(mutableGraph);

    mutableGraph.addNode("B");
    assertThat(immutableGraph).isNotEqualTo(mutableGraph);
  }

  @Test
  public void immutableBasicGraph() {
    MutableBasicGraph<String> mutableGraph = BasicGraphBuilder.directed().build();
    mutableGraph.addNode("A");
    BasicGraph<String> immutableGraph = ImmutableBasicGraph.copyOf(mutableGraph);

    assertThat(immutableGraph).isInstanceOf(ImmutableGraph.class);
    assertThat(immutableGraph).isNotInstanceOf(MutableBasicGraph.class);
    assertThat(immutableGraph).isEqualTo(mutableGraph);

    mutableGraph.addNode("B");
    assertThat(immutableGraph).isNotEqualTo(mutableGraph);
  }

  @Test
  public void copyOfImmutableGraph_optimized() {
    Graph<String, Integer> graph1 =
        ImmutableGraph.copyOf(GraphBuilder.directed().<String, Integer>build());
    Graph<String, Integer> graph2 = ImmutableBasicGraph.copyOf(graph1);

    assertThat(graph2).isSameAs(graph1);
  }

  @Test
  public void copyOfImmutableBasicGraph_optimized() {
    BasicGraph<String> graph1 =
        ImmutableBasicGraph.copyOf(BasicGraphBuilder.directed().<String>build());
    BasicGraph<String> graph2 = ImmutableBasicGraph.copyOf(graph1);

    assertThat(graph2).isSameAs(graph1);
  }
}
