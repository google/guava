/*
 * Copyright (C) 2026 The Guava Authors
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
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.jspecify.annotations.NullUnmarked;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for incident-edge order and view behavior of value-graph adapters. */
@RunWith(JUnit4.class)
@NullUnmarked
public final class ValueGraphIncidentEdgeOrderTest {
  @Test
  public void asGraph_preservesStableIncidentEdgeOrder() {
    MutableValueGraph<Integer, String> graph = newGraph();
    graph.putEdgeValue(1, 2, "outgoing");
    graph.putEdgeValue(3, 1, "incoming");

    assertThat(graph.asGraph().incidentEdgeOrder()).isEqualTo(ElementOrder.stable());
    assertThat(graph.asGraph().incidentEdges(1))
        .containsExactly(EndpointPair.ordered(1, 2), EndpointPair.ordered(3, 1))
        .inOrder();
  }

  @Test
  public void transpose_preservesStableIncidentEdgeOrder() {
    MutableValueGraph<Integer, String> graph = newGraph();
    graph.putEdgeValue(2, 1, "incoming");
    graph.putEdgeValue(1, 3, "outgoing");

    ValueGraph<Integer, String> transposed = Graphs.transpose(graph);
    assertThat(transposed.incidentEdgeOrder()).isEqualTo(ElementOrder.stable());
    assertThat(transposed.incidentEdges(1))
        .containsExactly(EndpointPair.ordered(1, 2), EndpointPair.ordered(3, 1))
        .inOrder();
  }

  @Test
  public void asGraph_incidentEdgesReflectChanges() {
    MutableValueGraph<Integer, String> graph = newGraph();
    graph.putEdgeValue(1, 2, "outgoing");
    Set<EndpointPair<Integer>> incident = graph.asGraph().incidentEdges(1);

    graph.putEdgeValue(3, 1, "incoming");
    graph.putEdgeValue(1, 1, "loop");
    graph.putEdgeValue(1, 2, "new value");
    assertThat(incident)
        .containsExactly(
            EndpointPair.ordered(1, 2), EndpointPair.ordered(3, 1), EndpointPair.ordered(1, 1))
        .inOrder();

    graph.removeEdge(1, 2);
    graph.putEdgeValue(1, 2, "reinserted");
    assertThat(incident)
        .containsExactly(
            EndpointPair.ordered(3, 1), EndpointPair.ordered(1, 1), EndpointPair.ordered(1, 2))
        .inOrder();
    assertThat(incident).doesNotContain(EndpointPair.ordered(2, 1));
    assertThat(incident).doesNotContain(EndpointPair.unordered(1, 2));
    assertThrows(
        UnsupportedOperationException.class, () -> incident.remove(EndpointPair.ordered(1, 2)));
  }

  @Test
  public void transpose_incidentEdgesReflectChanges() {
    MutableValueGraph<Integer, String> graph = newGraph();
    graph.putEdgeValue(2, 1, "incoming");
    Set<EndpointPair<Integer>> incident = Graphs.transpose(graph).incidentEdges(1);

    graph.putEdgeValue(1, 3, "outgoing");
    graph.putEdgeValue(1, 1, "loop");
    graph.putEdgeValue(2, 1, "new value");
    assertThat(incident)
        .containsExactly(
            EndpointPair.ordered(1, 2), EndpointPair.ordered(3, 1), EndpointPair.ordered(1, 1))
        .inOrder();

    graph.removeEdge(2, 1);
    graph.putEdgeValue(2, 1, "reinserted");
    assertThat(incident)
        .containsExactly(
            EndpointPair.ordered(3, 1), EndpointPair.ordered(1, 1), EndpointPair.ordered(1, 2))
        .inOrder();
    assertThat(incident).doesNotContain(EndpointPair.ordered(2, 1));
    assertThat(incident).doesNotContain(EndpointPair.unordered(1, 2));
    assertThat(incident.hashCode()).isEqualTo(ImmutableSet.copyOf(incident).hashCode());
    assertThat(incident).isEqualTo(ImmutableSet.copyOf(incident));
    assertThrows(
        UnsupportedOperationException.class, () -> incident.remove(EndpointPair.ordered(1, 2)));
  }

  @Test
  public void asGraph_incidentEdgesValidateNode() {
    MutableValueGraph<Integer, String> graph = newGraph();
    Graph<Integer> asGraph = graph.asGraph();
    assertThrows(IllegalArgumentException.class, () -> asGraph.incidentEdges(1));

    graph.putEdgeValue(1, 2, "edge");
    Set<EndpointPair<Integer>> incident = asGraph.incidentEdges(1);
    graph.removeNode(1);
    assertInvalidated(incident);
  }

  @Test
  public void transpose_incidentEdgesValidateNode() {
    MutableValueGraph<Integer, String> graph = newGraph();
    ValueGraph<Integer, String> transposed = Graphs.transpose(graph);
    assertThrows(IllegalArgumentException.class, () -> transposed.incidentEdges(1));

    graph.putEdgeValue(1, 2, "edge");
    Set<EndpointPair<Integer>> incident = transposed.incidentEdges(1);
    graph.removeNode(1);
    assertInvalidated(incident);
  }

  @Test
  public void transposeGraph_preservesStableIncidentEdgeOrder() {
    MutableGraph<Integer> graph =
        GraphBuilder.directed().incidentEdgeOrder(ElementOrder.stable()).build();
    graph.putEdge(2, 1);
    graph.putEdge(1, 3);
    assertThat(Graphs.transpose(graph).incidentEdges(1))
        .containsExactly(EndpointPair.ordered(1, 2), EndpointPair.ordered(3, 1))
        .inOrder();
  }

  @Test
  public void immutableAsGraph_preservesStableIncidentEdgeOrder() {
    MutableValueGraph<Integer, String> graph = newGraph();
    graph.putEdgeValue(1, 2, "outgoing");
    graph.putEdgeValue(3, 1, "incoming");
    assertThat(ImmutableValueGraph.copyOf(graph).asGraph().incidentEdges(1))
        .containsExactly(EndpointPair.ordered(1, 2), EndpointPair.ordered(3, 1))
        .inOrder();
  }

  private static MutableValueGraph<Integer, String> newGraph() {
    return ValueGraphBuilder.directed()
        .allowsSelfLoops(true)
        .incidentEdgeOrder(ElementOrder.stable())
        .build();
  }

  private static void assertInvalidated(Set<EndpointPair<Integer>> incident) {
    assertThrows(IllegalStateException.class, incident::size);
    assertThrows(IllegalStateException.class, incident::iterator);
    assertThrows(IllegalStateException.class, () -> incident.contains(EndpointPair.ordered(1, 2)));
    assertThrows(IllegalStateException.class, () -> incident.equals(ImmutableSet.of()));
    assertThat(incident.equals(incident)).isTrue();
    int unused = incident.hashCode();
  }
}
