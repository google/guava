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

import static com.google.common.graph.GraphConstants.ENDPOINTS_MISMATCH;
import static com.google.common.graph.TestUtil.assertStronglyEquivalent;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ConfigurableMutableValueGraph} and related functionality. */
// TODO(user): Expand coverage and move to proper test suite.
@RunWith(JUnit4.class)
public final class ValueGraphTest {
  private static final String DEFAULT = "default";

  MutableValueGraph<Integer, String> graph;

  @After
  public void validateGraphState() {
    assertStronglyEquivalent(graph, Graphs.copyOf(graph));
    assertStronglyEquivalent(graph, ImmutableValueGraph.copyOf(graph));

    Graph<Integer> asGraph = graph.asGraph();
    AbstractGraphTest.validateGraph(asGraph);
    assertThat(graph.nodes()).isEqualTo(asGraph.nodes());
    assertThat(graph.edges()).isEqualTo(asGraph.edges());
    assertThat(graph.nodeOrder()).isEqualTo(asGraph.nodeOrder());
    assertThat(graph.isDirected()).isEqualTo(asGraph.isDirected());
    assertThat(graph.allowsSelfLoops()).isEqualTo(asGraph.allowsSelfLoops());

    for (Integer node : graph.nodes()) {
      assertThat(graph.adjacentNodes(node)).isEqualTo(asGraph.adjacentNodes(node));
      assertThat(graph.predecessors(node)).isEqualTo(asGraph.predecessors(node));
      assertThat(graph.successors(node)).isEqualTo(asGraph.successors(node));
      assertThat(graph.degree(node)).isEqualTo(asGraph.degree(node));
      assertThat(graph.inDegree(node)).isEqualTo(asGraph.inDegree(node));
      assertThat(graph.outDegree(node)).isEqualTo(asGraph.outDegree(node));

      for (Integer otherNode : graph.nodes()) {
        boolean hasEdge = graph.hasEdgeConnecting(node, otherNode);
        assertThat(hasEdge).isEqualTo(asGraph.hasEdgeConnecting(node, otherNode));
        assertThat(graph.edgeValueOrDefault(node, otherNode, null) != null).isEqualTo(hasEdge);
        assertThat(!graph.edgeValueOrDefault(node, otherNode, DEFAULT).equals(DEFAULT))
            .isEqualTo(hasEdge);
      }
    }
  }

  @Test
  public void directedGraph() {
    graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB");
    graph.putEdgeValue(2, 3, "valueC");
    graph.putEdgeValue(4, 4, "valueD");

    assertThat(graph.edgeValueOrDefault(1, 2, null)).isEqualTo("valueA");
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(2, 3, null)).isEqualTo("valueC");
    assertThat(graph.edgeValueOrDefault(4, 4, null)).isEqualTo("valueD");
    assertThat(graph.edgeValueOrDefault(1, 2, DEFAULT)).isEqualTo("valueA");
    assertThat(graph.edgeValueOrDefault(2, 1, DEFAULT)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(2, 3, DEFAULT)).isEqualTo("valueC");
    assertThat(graph.edgeValueOrDefault(4, 4, DEFAULT)).isEqualTo("valueD");

    String toString = graph.toString();
    assertThat(toString).contains("valueA");
    assertThat(toString).contains("valueB");
    assertThat(toString).contains("valueC");
    assertThat(toString).contains("valueD");
  }

  @Test
  public void undirectedGraph() {
    graph = ValueGraphBuilder.undirected().allowsSelfLoops(true).build();
    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB"); // overwrites valueA in undirected case
    graph.putEdgeValue(2, 3, "valueC");
    graph.putEdgeValue(4, 4, "valueD");

    assertThat(graph.edgeValueOrDefault(1, 2, null)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(2, 3, null)).isEqualTo("valueC");
    assertThat(graph.edgeValueOrDefault(4, 4, null)).isEqualTo("valueD");
    assertThat(graph.edgeValueOrDefault(1, 2, DEFAULT)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(2, 1, DEFAULT)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(2, 3, DEFAULT)).isEqualTo("valueC");
    assertThat(graph.edgeValueOrDefault(4, 4, DEFAULT)).isEqualTo("valueD");

    String toString = graph.toString();
    assertThat(toString).doesNotContain("valueA");
    assertThat(toString).contains("valueB");
    assertThat(toString).contains("valueC");
    assertThat(toString).contains("valueD");
  }

  @Test
  public void hasEdgeConnecting_directed_correct() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(1, 2))).isTrue();
  }

  @Test
  public void hasEdgeConnecting_directed_backwards() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(2, 1))).isFalse();
  }

  @Test
  public void hasEdgeConnecting_directed_mismatch() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(1, 2))).isFalse();
    assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(2, 1))).isFalse();
  }

  @Test
  public void hasEdgeConnecting_undirected_correct() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(1, 2))).isTrue();
  }

  @Test
  public void hasEdgeConnecting_undirected_backwards() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(2, 1))).isTrue();
  }

  @Test
  public void hasEdgeConnecting_undirected_mismatch() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(1, 2))).isTrue();
    assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(2, 1))).isTrue();
  }

  @Test
  public void edgeValueOrDefault_directed_correct() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValueOrDefault(EndpointPair.ordered(1, 2), "default")).isEqualTo("A");
  }

  @Test
  public void edgeValueOrDefault_directed_backwards() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValueOrDefault(EndpointPair.ordered(2, 1), "default"))
        .isEqualTo("default");
  }

  @Test
  public void edgeValueOrDefault_directed_mismatch() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    try {
      String unused = graph.edgeValueOrDefault(EndpointPair.unordered(1, 2), "default");
      unused = graph.edgeValueOrDefault(EndpointPair.unordered(2, 1), "default");
      fail("Expected IllegalArgumentException: " + ENDPOINTS_MISMATCH);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
    }
  }

  @Test
  public void edgeValueOrDefault_undirected_correct() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValueOrDefault(EndpointPair.unordered(1, 2), "default")).isEqualTo("A");
  }

  @Test
  public void edgeValueOrDefault_undirected_backwards() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValueOrDefault(EndpointPair.unordered(2, 1), "default")).isEqualTo("A");
  }

  @Test
  public void edgeValueOrDefault_undirected_mismatch() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValueOrDefault(EndpointPair.ordered(2, 1), "default")).isEqualTo("A");
    assertThat(graph.edgeValueOrDefault(EndpointPair.ordered(2, 1), "default")).isEqualTo("A");
  }

  @Test
  public void putEdgeValue_directed() {
    graph = ValueGraphBuilder.directed().build();

    assertThat(graph.putEdgeValue(1, 2, "valueA")).isNull();
    assertThat(graph.putEdgeValue(2, 1, "valueB")).isNull();
    assertThat(graph.putEdgeValue(1, 2, "valueC")).isEqualTo("valueA");
    assertThat(graph.putEdgeValue(2, 1, "valueD")).isEqualTo("valueB");
  }

  @Test
  public void putEdgeValue_directed_orderMismatch() {
    graph = ValueGraphBuilder.directed().build();
    try {
      graph.putEdgeValue(EndpointPair.unordered(1, 2), "irrelevant");
      fail("Expected IllegalArgumentException: " + ENDPOINTS_MISMATCH);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
    }
  }

  @Test
  public void putEdgeValue_undirected_orderMismatch() {
    graph = ValueGraphBuilder.undirected().build();
    assertThat(graph.putEdgeValue(EndpointPair.ordered(1, 2), "irrelevant")).isNull();
  }

  @Test
  public void putEdgeValue_undirected() {
    graph = ValueGraphBuilder.undirected().build();

    assertThat(graph.putEdgeValue(1, 2, "valueA")).isNull();
    assertThat(graph.putEdgeValue(2, 1, "valueB")).isEqualTo("valueA");
    assertThat(graph.putEdgeValue(1, 2, "valueC")).isEqualTo("valueB");
    assertThat(graph.putEdgeValue(2, 1, "valueD")).isEqualTo("valueC");
  }

  @Test
  public void removeEdge_directed() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB");
    graph.putEdgeValue(2, 3, "valueC");

    assertThat(graph.removeEdge(1, 2)).isEqualTo("valueA");
    assertThat(graph.removeEdge(1, 2)).isNull();
    assertThat(graph.removeEdge(2, 1)).isEqualTo("valueB");
    assertThat(graph.removeEdge(2, 1)).isNull();
    assertThat(graph.removeEdge(2, 3)).isEqualTo("valueC");
    assertThat(graph.removeEdge(2, 3)).isNull();
  }

  @Test
  public void removeEdge_undirected() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB");
    graph.putEdgeValue(2, 3, "valueC");

    assertThat(graph.removeEdge(1, 2)).isEqualTo("valueB");
    assertThat(graph.removeEdge(1, 2)).isNull();
    assertThat(graph.removeEdge(2, 1)).isNull();
    assertThat(graph.removeEdge(2, 3)).isEqualTo("valueC");
    assertThat(graph.removeEdge(2, 3)).isNull();
  }

  @Test
  public void removeEdge_directed_orderMismatch() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "1->2");
    graph.putEdgeValue(2, 1, "2->1");
    try {
      graph.removeEdge(EndpointPair.unordered(1, 2));
      graph.removeEdge(EndpointPair.unordered(2, 1));
      fail("Expected IllegalArgumentException: " + ENDPOINTS_MISMATCH);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
    }
  }

  @Test
  public void removeEdge_undirected_orderMismatch() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "1-2");
    assertThat(graph.removeEdge(EndpointPair.ordered(1, 2))).isEqualTo("1-2");
  }

  @Test
  public void edgeValue_missing() {
    graph = ValueGraphBuilder.directed().build();

    assertThat(graph.edgeValueOrDefault(1, 2, DEFAULT)).isEqualTo(DEFAULT);
    assertThat(graph.edgeValueOrDefault(2, 1, DEFAULT)).isEqualTo(DEFAULT);
    assertThat(graph.edgeValueOrDefault(1, 2, null)).isNull();
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isNull();

    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB");
    assertThat(graph.edgeValueOrDefault(1, 2, DEFAULT)).isEqualTo("valueA");
    assertThat(graph.edgeValueOrDefault(2, 1, DEFAULT)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(1, 2, null)).isEqualTo("valueA");
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isEqualTo("valueB");

    graph.removeEdge(1, 2);
    graph.putEdgeValue(2, 1, "valueC");
    assertThat(graph.edgeValueOrDefault(1, 2, DEFAULT)).isEqualTo(DEFAULT);
    assertThat(graph.edgeValueOrDefault(2, 1, DEFAULT)).isEqualTo("valueC");
    assertThat(graph.edgeValueOrDefault(1, 2, null)).isNull();
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isEqualTo("valueC");
  }

  @Test
  public void equivalence_considersEdgeValue() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "valueA");

    MutableValueGraph<Integer, String> otherGraph = ValueGraphBuilder.undirected().build();
    otherGraph.putEdgeValue(1, 2, "valueA");
    assertThat(graph).isEqualTo(otherGraph);

    otherGraph.putEdgeValue(1, 2, "valueB");
    assertThat(graph).isNotEqualTo(otherGraph); // values differ
  }
}
