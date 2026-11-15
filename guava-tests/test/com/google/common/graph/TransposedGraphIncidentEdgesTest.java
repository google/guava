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

/** Tests for the incident-edge view of a transposed graph. */
@RunWith(JUnit4.class)
@NullUnmarked
public final class TransposedGraphIncidentEdgesTest {
  @Test
  public void incidentEdges_missingNodeThrowsImmediately() {
    MutableGraph<Integer> graph = newGraph();
    graph.putEdge(1, 2);

    assertThrows(IllegalArgumentException.class, () -> Graphs.transpose(graph).incidentEdges(99));
  }

  @Test
  public void incidentEdges_nullNodeThrowsImmediately() {
    assertThrows(
        NullPointerException.class, () -> Graphs.transpose(newGraph()).incidentEdges(null));
  }

  @Test
  public void incidentEdges_sizeThrowsAfterNodeRemoval() {
    assertThrows(IllegalStateException.class, invalidatedView()::size);
  }

  @Test
  public void incidentEdges_iteratorThrowsAfterNodeRemoval() {
    assertThrows(IllegalStateException.class, invalidatedView()::iterator);
  }

  @Test
  public void incidentEdges_containsThrowsAfterNodeRemoval() {
    Set<EndpointPair<Integer>> incident = invalidatedView();
    assertThrows(IllegalStateException.class, () -> incident.contains(EndpointPair.ordered(2, 1)));
  }

  @Test
  public void incidentEdges_equalsAllowsOnlySelfAfterNodeRemoval() {
    Set<EndpointPair<Integer>> incident = invalidatedView();
    assertThat(incident.equals(incident)).isTrue();
    assertThrows(IllegalStateException.class, () -> incident.equals(ImmutableSet.of()));
  }

  @Test
  public void incidentEdges_hashCodeDoesNotThrowAfterNodeRemoval() {
    int unused = invalidatedView().hashCode();
  }

  @Test
  public void incidentEdges_liveViewPreservesOrderAndMembership() {
    MutableGraph<Integer> graph = newGraph();
    graph.putEdge(2, 1);
    Set<EndpointPair<Integer>> incident = Graphs.transpose(graph).incidentEdges(1);

    graph.putEdge(1, 3);
    graph.putEdge(1, 1);
    assertThat(incident)
        .containsExactly(
            EndpointPair.ordered(1, 2), EndpointPair.ordered(3, 1), EndpointPair.ordered(1, 1))
        .inOrder();
    assertThat(incident).hasSize(3);
    assertThat(incident).contains(EndpointPair.ordered(1, 2));
    assertThat(incident).doesNotContain(EndpointPair.ordered(2, 1));
    assertThat(incident).doesNotContain(EndpointPair.unordered(1, 2));
    assertThat(incident).doesNotContain(EndpointPair.ordered(8, 9));
    assertThat(incident).doesNotContain("not an endpoint pair");
    assertThat(incident).isEqualTo(ImmutableSet.copyOf(incident));
    assertThat(incident.hashCode()).isEqualTo(ImmutableSet.copyOf(incident).hashCode());
    assertThrows(
        UnsupportedOperationException.class, () -> incident.remove(EndpointPair.ordered(1, 2)));

    graph.removeEdge(2, 1);
    graph.putEdge(2, 1);
    assertThat(incident)
        .containsExactly(
            EndpointPair.ordered(3, 1), EndpointPair.ordered(1, 1), EndpointPair.ordered(1, 2))
        .inOrder();
    assertThat(Graphs.transpose(graph).incidentEdgeOrder()).isEqualTo(ElementOrder.stable());
    assertThat(Graphs.transpose(Graphs.transpose(graph))).isSameInstanceAs(graph);
  }

  @Test
  public void incidentEdges_isolatedNodeViewReflectsEdgeChanges() {
    MutableGraph<Integer> graph = newGraph();
    graph.addNode(1);
    Graph<Integer> transposed = Graphs.transpose(graph);
    Set<EndpointPair<Integer>> incident = transposed.incidentEdges(1);
    assertThat(incident).isEmpty();

    graph.putEdge(2, 1);
    assertThat(incident).containsExactly(EndpointPair.ordered(1, 2));
    graph.removeEdge(2, 1);
    assertThat(incident).isEmpty();

    graph.removeNode(1);
    assertThrows(IllegalStateException.class, incident::size);
    graph.putEdge(3, 1);
    assertThat(transposed.incidentEdges(1)).containsExactly(EndpointPair.ordered(1, 3));
  }

  @Test
  public void transpose_undirectedGraphRetainsViewBehavior() {
    MutableGraph<Integer> graph = GraphBuilder.undirected().allowsSelfLoops(true).build();
    Graph<Integer> transposed = Graphs.transpose(graph);
    assertThat(transposed).isSameInstanceAs(graph);
    assertThrows(IllegalArgumentException.class, () -> transposed.incidentEdges(1));

    graph.putEdge(1, 2);
    Set<EndpointPair<Integer>> incident = transposed.incidentEdges(1);
    graph.putEdge(1, 1);
    assertThat(incident)
        .containsExactly(EndpointPair.unordered(1, 2), EndpointPair.unordered(1, 1));
    assertThat(incident).doesNotContain(EndpointPair.ordered(1, 2));
    graph.removeNode(1);
    assertThrows(IllegalStateException.class, incident::size);
  }

  private static Set<EndpointPair<Integer>> invalidatedView() {
    MutableGraph<Integer> graph = newGraph();
    graph.putEdge(1, 2);
    Set<EndpointPair<Integer>> incident = Graphs.transpose(graph).incidentEdges(1);
    graph.removeNode(1);
    return incident;
  }

  private static MutableGraph<Integer> newGraph() {
    return GraphBuilder.directed()
        .allowsSelfLoops(true)
        .incidentEdgeOrder(ElementOrder.stable())
        .build();
  }
}
