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

/** Tests for {@link ImmutableGraph} and {@link ImmutableValueGraph} . */
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
  public void copyOfImmutableGraph_optimized() {
    Graph<String> graph1 = ImmutableGraph.copyOf(GraphBuilder.directed().<String>build());
    Graph<String> graph2 = ImmutableGraph.copyOf(graph1);

    assertThat(graph2).isSameAs(graph1);
  }

  @Test
  public void copyOfImmutableValueGraph_optimized() {
    ValueGraph<String, Integer> graph1 =
        ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().<String, Integer>build());
    ValueGraph<String, Integer> graph2 = ImmutableValueGraph.copyOf(graph1);

    assertThat(graph2).isSameAs(graph1);
  }
}
