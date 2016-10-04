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
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ConfigurableMutableValueGraph} and related functionality. */
// TODO(user): Expand coverage and move to proper test suite.
@RunWith(JUnit4.class)
public final class ValueGraphTest {
  MutableValueGraph<Integer, String> graph;

  @After
  public void validateGraphState() {
    assertStronglyEquivalent(graph, Graphs.copyOf(graph));
    assertStronglyEquivalent(graph, ImmutableValueGraph.copyOf(graph));

    for (Integer node : graph.nodes()) {
      for (Integer otherNode : graph.nodes()) {
        boolean connected = graph.successors(node).contains(otherNode);
        assertThat(graph.edgeValueOrDefault(node, otherNode, null) != null).isEqualTo(connected);
      }
    }

    AbstractGraphTest.validateGraph(graph);
  }

  @Test
  public void directedGraph() {
    graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB");
    graph.putEdgeValue(2, 3, "valueC");
    graph.putEdgeValue(4, 4, "valueD");

    assertThat(graph.edgeValue(1, 2)).isEqualTo("valueA");
    assertThat(graph.edgeValue(2, 1)).isEqualTo("valueB");
    assertThat(graph.edgeValue(2, 3)).isEqualTo("valueC");
    assertThat(graph.edgeValue(4, 4)).isEqualTo("valueD");

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

    assertThat(graph.edgeValue(1, 2)).isEqualTo("valueB");
    assertThat(graph.edgeValue(2, 1)).isEqualTo("valueB");
    assertThat(graph.edgeValue(2, 3)).isEqualTo("valueC");
    assertThat(graph.edgeValue(4, 4)).isEqualTo("valueD");

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
  public void edgeValue_edgeNotPresent() {
    graph = ValueGraphBuilder.directed().build();
    graph.addNode(1);
    graph.addNode(2);

    try {
      graph.edgeValue(2, 1);
      fail("Should have rejected edgeValue() if edge not present in graph.");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Edge connecting 2 to 1 is not present in this graph.");
    }
  }

  @Test
  public void edgeValue_nodeNotPresent() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "value");

    try {
      graph.edgeValue(2, 3);
      fail("Should have rejected edgeValue() if node not present in graph.");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Node 3 is not an element of this graph.");
    }
  }

  @Test
  public void edgeValueOrDefault() {
    graph = ValueGraphBuilder.directed().build();

    assertThat(graph.edgeValueOrDefault(1, 2, "default")).isEqualTo("default");
    assertThat(graph.edgeValueOrDefault(2, 1, "default")).isEqualTo("default");

    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB");
    assertThat(graph.edgeValueOrDefault(1, 2, "default")).isEqualTo("valueA");
    assertThat(graph.edgeValueOrDefault(2, 1, "default")).isEqualTo("valueB");

    graph.removeEdge(1, 2);
    graph.putEdgeValue(2, 1, "valueC");
    assertThat(graph.edgeValueOrDefault(1, 2, "default")).isEqualTo("default");
    assertThat(graph.edgeValueOrDefault(2, 1, "default")).isEqualTo("valueC");
  }

  @Test
  public void equivalence_considersEdgeValue() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "valueA");

    MutableValueGraph<Integer, String> otherGraph = ValueGraphBuilder.undirected().build();
    otherGraph.putEdgeValue(1, 2, "valueA");

    assertThat(Graphs.equivalent(graph, otherGraph)).isTrue();
    assertThat(Graphs.equivalent((Graph<Integer>) graph, otherGraph)).isTrue();

    otherGraph.putEdgeValue(1, 2, "valueB");

    assertThat(Graphs.equivalent(graph, otherGraph)).isFalse(); // values differ
    assertThat(Graphs.equivalent((Graph<Integer>) graph, otherGraph)).isTrue();
  }
}
