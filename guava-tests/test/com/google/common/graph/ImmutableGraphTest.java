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
import static org.junit.Assert.fail;

import com.google.common.graph.testing.TestGraphBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ImmutableGraph}.
 */
@RunWith(JUnit4.class)
public class ImmutableGraphTest {

  @Test
  public void addNode_immutable() {
    Graph<String, String> graph = TestGraphBuilder.<String, String>init(GraphBuilder.directed())
        .toImmutableGraph();
    try {
      graph.addNode("node");
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    assertThat(graph.nodes()).isEmpty();
  }

  @Test
  public void addEdge_immutable() {
    Graph<String, String> graph = TestGraphBuilder.<String, String>init(GraphBuilder.directed())
        .toImmutableGraph();
    try {
      graph.addEdge("edge", "node1", "node2");
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    assertThat(graph.edges()).isEmpty();
  }

  @Test
  public void removeNode_immutable() {
    Graph<String, String> graph = TestGraphBuilder.<String, String>init(GraphBuilder.directed())
        .addNode("node")
        .toImmutableGraph();
    try {
      graph.removeNode("node");
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    assertThat(graph.nodes()).containsExactly("node");
  }

  @Test
  public void removeEdge_immutable() {
    Graph<String, String> graph = TestGraphBuilder.<String, String>init(GraphBuilder.directed())
        .addEdge("edge", "node1", "node2")
        .toImmutableGraph();
    try {
      graph.removeEdge("edge");
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    assertThat(graph.edges()).containsExactly("edge");
  }

  @Test
  public void copyOfImmutableGraph_optimized() {
    Graph<String, String> graph1 = ImmutableGraph.copyOf(
        GraphBuilder.directed().<String, String>build());
    Graph<String, String> graph2 = ImmutableGraph.copyOf(graph1);

    assertThat(graph2).isSameAs(graph1);
  }

  @Test
  public void edgesConnecting_directed() {
    Graph<String, String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.addEdge("AA", "A", "A");
    mutableGraph.addEdge("AB", "A", "B");
    Graph<String, String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThat(graph.edgesConnecting("A", "A")).containsExactly("AA");
    assertThat(graph.edgesConnecting("A", "B")).containsExactly("AB");
    assertThat(graph.edgesConnecting("B", "A")).isEmpty();
  }

  @Test
  public void edgesConnecting_undirected() {
    Graph<String, String> mutableGraph = GraphBuilder.undirected().build();
    mutableGraph.addEdge("AA", "A", "A");
    mutableGraph.addEdge("AB", "A", "B");
    Graph<String, String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThat(graph.edgesConnecting("A", "A")).containsExactly("AA");
    assertThat(graph.edgesConnecting("A", "B")).containsExactly("AB");
    assertThat(graph.edgesConnecting("B", "A")).containsExactly("AB");
  }
}
