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
import static org.junit.Assert.assertThrows;

import org.jspecify.annotations.NullUnmarked;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ImmutableGraph}. */
@RunWith(JUnit4.class)
@NullUnmarked
public class ImmutableGraphTest {

  @Test
  public void immutableGraph() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.addNode("A");
    ImmutableGraph<String> immutableGraph = ImmutableGraph.copyOf(mutableGraph);

    assertThat(immutableGraph).isNotInstanceOf(MutableGraph.class);
    assertThat(immutableGraph).isEqualTo(mutableGraph);

    mutableGraph.addNode("B");
    assertThat(immutableGraph).isNotEqualTo(mutableGraph);
  }

  @Test
  public void copyOfImmutableGraph_optimized() {
    Graph<String> graph1 = ImmutableGraph.copyOf(GraphBuilder.directed().<String>build());
    Graph<String> graph2 = ImmutableGraph.copyOf(graph1);

    assertThat(graph2).isSameInstanceAs(graph1);
  }

  @Test
  public void incidentEdgeOrder_stable() {
    ImmutableGraph<String> immutableGraph =
        ImmutableGraph.copyOf(GraphBuilder.directed().<String>build());

    assertThat(immutableGraph.incidentEdgeOrder()).isEqualTo(ElementOrder.stable());
  }

  @Test
  public void emptyGraph_nodes_unmodifiable() {
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(GraphBuilder.directed().<String>build());

    assertThrows(UnsupportedOperationException.class, () -> graph.nodes().add("A"));
  }

  @Test
  public void emptyGraph_nodes_clear() {
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(GraphBuilder.directed().<String>build());

    assertThrows(UnsupportedOperationException.class, () -> graph.nodes().clear());
  }

  @Test
  public void emptyGraph_edges_unmodifiable() {
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(GraphBuilder.directed().<String>build());

    assertThrows(
        UnsupportedOperationException.class,
        () -> graph.edges().add(EndpointPair.ordered("A", "B")));
  }

  @Test
  public void emptyGraph_edges_clear() {
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(GraphBuilder.directed().<String>build());

    assertThrows(UnsupportedOperationException.class, () -> graph.edges().clear());
  }

  @Test
  public void nonEmptyGraph_nodes_unmodifiable() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.addNode("A");
    mutableGraph.addNode("B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(UnsupportedOperationException.class, () -> graph.nodes().add("C"));
    assertThrows(UnsupportedOperationException.class, () -> graph.nodes().remove("A"));
  }

  @Test
  public void nonEmptyGraph_nodes_clear() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.addNode("A");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(UnsupportedOperationException.class, () -> graph.nodes().clear());
  }

  @Test
  public void nonEmptyGraph_nodes_removeAll() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.addNode("A");
    mutableGraph.addNode("B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(
        UnsupportedOperationException.class,
        () -> graph.nodes().removeAll(java.util.Arrays.asList("A")));
  }

  @Test
  public void nonEmptyGraph_edges_unmodifiable() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.putEdge("A", "B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(
        UnsupportedOperationException.class,
        () -> graph.edges().add(EndpointPair.ordered("B", "C")));
    assertThrows(
        UnsupportedOperationException.class,
        () -> graph.edges().remove(EndpointPair.ordered("A", "B")));
  }

  @Test
  public void nonEmptyGraph_edges_clear() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.putEdge("A", "B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(UnsupportedOperationException.class, () -> graph.edges().clear());
  }

  @Test
  public void nonEmptyGraph_adjacentNodes_unmodifiable() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.putEdge("A", "B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(UnsupportedOperationException.class, () -> graph.adjacentNodes("A").add("C"));
    assertThrows(UnsupportedOperationException.class, () -> graph.adjacentNodes("A").remove("B"));
  }

  @Test
  public void nonEmptyGraph_adjacentNodes_clear() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.putEdge("A", "B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(UnsupportedOperationException.class, () -> graph.adjacentNodes("A").clear());
  }

  @Test
  public void nonEmptyGraph_predecessors_unmodifiable() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.putEdge("A", "B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(UnsupportedOperationException.class, () -> graph.predecessors("B").add("C"));
    assertThrows(UnsupportedOperationException.class, () -> graph.predecessors("B").remove("A"));
  }

  @Test
  public void nonEmptyGraph_predecessors_clear() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.putEdge("A", "B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(UnsupportedOperationException.class, () -> graph.predecessors("B").clear());
  }

  @Test
  public void nonEmptyGraph_successors_unmodifiable() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.putEdge("A", "B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(UnsupportedOperationException.class, () -> graph.successors("A").add("C"));
    assertThrows(UnsupportedOperationException.class, () -> graph.successors("A").remove("B"));
  }

  @Test
  public void nonEmptyGraph_successors_clear() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.putEdge("A", "B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(UnsupportedOperationException.class, () -> graph.successors("A").clear());
  }

  @Test
  public void nonEmptyGraph_incidentEdges_unmodifiable() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.putEdge("A", "B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(
        UnsupportedOperationException.class,
        () -> graph.incidentEdges("A").add(EndpointPair.ordered("A", "C")));
    assertThrows(
        UnsupportedOperationException.class,
        () -> graph.incidentEdges("A").remove(EndpointPair.ordered("A", "B")));
  }

  @Test
  public void nonEmptyGraph_incidentEdges_clear() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.putEdge("A", "B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    assertThrows(UnsupportedOperationException.class, () -> graph.incidentEdges("A").clear());
  }

  @Test
  public void nonEmptyGraph_nodes_iteratorRemove() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.addNode("A");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    java.util.Iterator<String> iterator = graph.nodes().iterator();
    iterator.next();
    assertThrows(UnsupportedOperationException.class, () -> iterator.remove());
  }

  @Test
  public void nonEmptyGraph_edges_iteratorRemove() {
    MutableGraph<String> mutableGraph = GraphBuilder.directed().build();
    mutableGraph.putEdge("A", "B");
    ImmutableGraph<String> graph = ImmutableGraph.copyOf(mutableGraph);

    java.util.Iterator<EndpointPair<String>> iterator = graph.edges().iterator();
    iterator.next();
    assertThrows(UnsupportedOperationException.class, () -> iterator.remove());
  }
}
