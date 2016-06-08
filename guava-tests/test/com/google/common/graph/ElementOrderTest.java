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

package com.google.common.graph;

import static com.google.common.graph.ElementOrder.insertion;
import static com.google.common.graph.ElementOrder.unordered;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Ordering;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for ordering the elements of graphs.
 */
@RunWith(JUnit4.class)
public final class ElementOrderTest {
  // Node order tests

  @Test
  public void orderNodes_none() throws Exception {
    MutableGraph<Integer> graph = GraphBuilder
        .directed()
        .orderNodes(unordered())
        .build();

    assertThat(graph.nodeOrder()).isEqualTo(unordered());
  }

  @Test
  public void orderNodes_insertion() throws Exception {
    MutableGraph<Integer> graph = GraphBuilder
        .directed()
        .orderNodes(insertion())
        .build();

    addNodes(graph);

    assertThat(graph.nodeOrder()).isEqualTo(insertion());
    assertThat(graph.nodes()).containsExactly(3, 1, 4).inOrder();
  }

  // The default ordering is INSERTION unless otherwise specified.
  @Test
  public void orderNodes_default() throws Exception {
    MutableGraph<Integer> graph = GraphBuilder
        .directed()
        .build();

    addNodes(graph);

    assertThat(graph.nodeOrder()).isEqualTo(insertion());
    assertThat(graph.nodes()).containsExactly(3, 1, 4).inOrder();
  }

  @Test
  public void orderNodes_natural() throws Exception {
    MutableGraph<Integer> graph = GraphBuilder
        .directed()
        .orderNodes(ElementOrder.<Integer>natural())
        .build();

    addNodes(graph);

    assertThat(graph.nodeOrder()).isEqualTo(ElementOrder.sorted(Ordering.<Integer>natural()));
    assertThat(graph.nodes()).containsExactly(1, 3, 4).inOrder();
  }

  @Test
  public void orderNodes_sorted() throws Exception {
    MutableGraph<Integer> graph = GraphBuilder
        .directed()
        .orderNodes(ElementOrder.sorted(Ordering.<Integer>natural().reverse()))
        .build();

    addNodes(graph);

    assertThat(graph.nodeOrder()).isEqualTo(
        ElementOrder.sorted(Ordering.<Integer>natural().reverse()));
    assertThat(graph.nodes()).containsExactly(4, 3, 1).inOrder();
  }

  // Edge order tests

  @Test
  public void orderEdges_none() throws Exception {
    MutableNetwork<Integer, String> graph = NetworkBuilder
        .directed()
        .orderEdges(unordered())
        .build();

    assertThat(graph.edgeOrder()).isEqualTo(unordered());
    assertThat(graph.nodeOrder()).isEqualTo(insertion()); // default
  }

  @Test
  public void orderEdges_insertion() throws Exception {
    MutableNetwork<Integer, String> graph = NetworkBuilder
        .directed()
        .orderEdges(insertion())
        .build();

    addEdges(graph);

    assertThat(graph.edgeOrder()).isEqualTo(ElementOrder.insertion());
    assertThat(graph.edges()).containsExactly("i", "e", "p").inOrder();
    assertThat(graph.nodeOrder()).isEqualTo(ElementOrder.insertion()); // default
  }

  // The default ordering is INSERTION unless otherwise specified.
  @Test
  public void orderEdges_default() throws Exception {
    MutableNetwork<Integer, String> graph = NetworkBuilder
        .directed()
        .build();

    addEdges(graph);

    assertThat(graph.edgeOrder()).isEqualTo(ElementOrder.insertion());
    assertThat(graph.edges()).containsExactly("i", "e", "p").inOrder();
    assertThat(graph.nodeOrder()).isEqualTo(ElementOrder.insertion()); // default
  }

  @Test
  public void orderEdges_natural() throws Exception {
    MutableNetwork<Integer, String> graph = NetworkBuilder
        .directed()
        .orderEdges(ElementOrder.<String>natural())
        .build();

    addEdges(graph);

    assertThat(graph.edgeOrder()).isEqualTo(ElementOrder.sorted(Ordering.<String>natural()));
    assertThat(graph.edges()).containsExactly("e", "i", "p").inOrder();
    assertThat(graph.nodeOrder()).isEqualTo(insertion()); // default
  }

  @Test
  public void orderEdges_sorted() throws Exception {
    MutableNetwork<Integer, String> graph = NetworkBuilder
        .directed()
        .orderEdges(ElementOrder.sorted(Ordering.<String>natural().reverse()))
        .build();

    addEdges(graph);

    assertThat(graph.edgeOrder()).isEqualTo(
        ElementOrder.sorted(Ordering.<String>natural().reverse()));
    assertThat(graph.edges()).containsExactly("p", "i", "e").inOrder();
    assertThat(graph.nodeOrder()).isEqualTo(ElementOrder.insertion()); // default
  }

  // Combined node and edge order tests
  @Test
  public void orderNodesUnorderedandEdgesSorted() throws Exception {
    MutableNetwork<Integer, String> graph = NetworkBuilder
        .directed()
        .orderNodes(unordered())
        .orderEdges(ElementOrder.sorted(Ordering.<String>natural().reverse()))
        .build();

    addEdges(graph);

    assertThat(graph.edgeOrder()).isEqualTo(
        ElementOrder.sorted(Ordering.<String>natural().reverse()));
    assertThat(graph.edges()).containsExactly("p", "i", "e").inOrder();
    assertThat(graph.nodeOrder()).isEqualTo(unordered());
    assertThat(graph.nodes()).containsExactly(4, 1, 3);
  }

  private static void addNodes(MutableGraph<Integer> graph) {
    graph.addNode(3);
    graph.addNode(1);
    graph.addNode(4);
  }

  private static void addEdges(MutableNetwork<Integer, String> graph) {
    graph.addEdge("i", 3, 1);
    graph.addEdge("e", 1, 4);
    graph.addEdge("p", 4, 3);
  }
}
