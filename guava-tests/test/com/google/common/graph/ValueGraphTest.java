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

import static com.google.common.graph.TestUtil.assertStronglyEquivalent;
import static com.google.common.truth.Truth.assertThat;

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

    String toString = graph.toString();
    assertThat(toString).doesNotContain("valueA");
    assertThat(toString).contains("valueB");
    assertThat(toString).contains("valueC");
    assertThat(toString).contains("valueD");
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
  public void edgeValue_missing() {
    graph = ValueGraphBuilder.directed().build();

    assertThat(graph.edgeValueOrDefault(1, 2, null)).isNull();
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isNull();
    assertThat(graph.edgeValue(1, 2).orElse(DEFAULT)).isEqualTo(DEFAULT);
    assertThat(graph.edgeValue(2, 1).orElse(DEFAULT)).isEqualTo(DEFAULT);

    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB");
    assertThat(graph.edgeValueOrDefault(1, 2, null)).isEqualTo("valueA");
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isEqualTo("valueB");
    assertThat(graph.edgeValue(1, 2).get()).isEqualTo("valueA");
    assertThat(graph.edgeValue(2, 1).get()).isEqualTo("valueB");

    graph.removeEdge(1, 2);
    graph.putEdgeValue(2, 1, "valueC");
    assertThat(graph.edgeValueOrDefault(1, 2, null)).isNull();
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isEqualTo("valueC");
    assertThat(graph.edgeValue(1, 2).orElse(DEFAULT)).isEqualTo(DEFAULT);
    assertThat(graph.edgeValue(2, 1).get()).isEqualTo("valueC");
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
