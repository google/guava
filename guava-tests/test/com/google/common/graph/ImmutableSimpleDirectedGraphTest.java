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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ImmutableDirectedGraph}, creating a simple directed graph (parallel and
 * self-loop edges are not allowed).
 */
@RunWith(JUnit4.class)
public class ImmutableSimpleDirectedGraphTest extends AbstractImmutableGraphTest {
  protected ImmutableDirectedGraph<Integer, String> immutableGraph;
  protected ImmutableDirectedGraph.Builder<Integer, String> builder;

  @Override
  final boolean addNode(Integer n) {
    graph = immutableGraph = builder.addNode(n).build();
    return true;
  }

  @Override
  final boolean addEdge(String e, Integer n1, Integer n2) {
    builder.addNode(n1);
    builder.addNode(n2);
    graph = immutableGraph = builder.addEdge(e, n1, n2).build();
    return true;
  }

  @Override
  public ImmutableDirectedGraph<Integer, String> createGraph() {
    builder = ImmutableDirectedGraph.builder(Graphs.config().noSelfLoops());
    return builder.build();
  }

  @Override
  public void init() {
    graph = immutableGraph = createGraph();
  }

  @Test
  public void edgesConnecting_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(immutableGraph.edgesConnecting(N1, N2)).containsExactly(E12);
    // Passed nodes should be in the correct edge direction, first is the
    // source node and the second is the target node
    assertThat(immutableGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void inEdges_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(immutableGraph.inEdges(N2)).containsExactly(E12);
    // Edge direction handled correctly
    assertThat(immutableGraph.inEdges(N1)).isEmpty();
  }

  @Test
  public void outEdges_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(immutableGraph.outEdges(N1)).containsExactly(E12);
    // Edge direction handled correctly
    assertThat(immutableGraph.outEdges(N2)).isEmpty();
  }

  @Test
  public void predecessors_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(immutableGraph.predecessors(N2)).containsExactly(N1);
    // Edge direction handled correctly
    assertThat(immutableGraph.predecessors(N1)).isEmpty();
  }

  @Test
  public void successors_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(immutableGraph.successors(N1)).containsExactly(N2);
    // Edge direction handled correctly
    assertThat(immutableGraph.successors(N2)).isEmpty();
  }

  @Test
  public void inDegree_oneEdge() {
    addEdge(E12, N1, N2);
    assertEquals(1, immutableGraph.inDegree(N2));
    // Edge direction handled correctly
    assertEquals(0, immutableGraph.inDegree(N1));
  }

  @Test
  public void outDegree_oneEdge() {
    addEdge(E12, N1, N2);
    assertEquals(1, immutableGraph.outDegree(N1));
    // Edge direction handled correctly
    assertEquals(0, immutableGraph.outDegree(N2));
  }

  @Test
  public void source_oneEdge() {
    addEdge(E12, N1, N2);
    assertEquals(N1, immutableGraph.source(E12));
  }

  @Test
  public void source_edgeNotInGraph() {
    try {
      immutableGraph.source(EDGE_NOT_IN_GRAPH);
      fail(ERROR_EDGE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertEdgeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void target_oneEdge() {
    addEdge(E12, N1, N2);
    assertEquals(N2, immutableGraph.target(E12));
  }

  @Test
  public void target_edgeNotInGraph() {
    try {
      immutableGraph.target(EDGE_NOT_IN_GRAPH);
      fail(ERROR_EDGE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertEdgeNotInGraphErrorMessage(e);
    }
  }

  // Builder mutation methods

  @Test
  public void addEdge_builder_selfLoop() {
    try {
      addEdge(E11, N1, N1);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_SELF_LOOP);
    }
  }

  @Test
  public void addEdge_builder_existingNodes() {
    // Adding nodes initially for safety (insulating from possible future
    // modifications to proxy methods)
    addNode(N1);
    addNode(N2);
    assertTrue(addEdge(E12, N1, N2));
    assertThat(immutableGraph.edges()).contains(E12);
    assertThat(immutableGraph.edgesConnecting(N1, N2)).containsExactly(E12);
    // Direction of the added edge is correctly handled
    assertThat(immutableGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void addEdge_builder_existingEdgeBetweenDifferentNodes() {
    addEdge(E12, N1, N2);
    try {
      // Edge between totally different nodes
      addEdge(E12, N4, N5);
      fail(ERROR_ADDED_EXISTING_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
    try {
      // Edge between same nodes but in reverse direction
      addEdge(E12, N2, N1);
      fail(ERROR_ADDED_EXISTING_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
  }

  @Test
  public void addEdge_builder_parallelEdge() {
    addEdge(E12, N1, N2);
    try {
      addEdge(EDGE_NOT_IN_GRAPH, N1, N2);
      fail(ERROR_ADDED_PARALLEL_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
  }

  /**
   * This test checks an implementation dependent feature. It tests that
   * the method {@code addEdge} will silently add the missing nodes to the builder,
   * then add the edge connecting them. We are not using the proxy methods here
   * as we want to test {@code addEdge} when the end-points are not elements
   * of the graph.
   */
  @Test
  public void addEdge_builder_nodesNotInGraph() {
    addNode(N1);
    assertTrue(addEdge(E15, N1, N5));
    assertTrue(addEdge(E41, N4, N1));
    assertTrue(addEdge(E23, N2, N3));
    assertThat(immutableGraph.nodes()).containsExactly(N1, N5, N4, N2, N3).inOrder();
    assertThat(immutableGraph.edges()).containsExactly(E15, E41, E23).inOrder();
    assertThat(immutableGraph.edgesConnecting(N1, N5)).containsExactly(E15);
    assertThat(immutableGraph.edgesConnecting(N4, N1)).containsExactly(E41);
    assertThat(immutableGraph.edgesConnecting(N2, N3)).containsExactly(E23);
    // Direction of the added edge is correctly handled
    assertThat(immutableGraph.edgesConnecting(N3, N2)).isEmpty();
  }

  @Test
  public void copyOf() {
    DirectedGraph<Integer, String> graph = Graphs.createDirected(immutableGraph.config());
    populateInputGraph(graph);
    assertThat(ImmutableDirectedGraph.copyOf(graph)).isEqualTo(graph);
  }

  @Test
  public void addGraph_incompatibleMultigraphConfig() {
    try {
      DirectedGraph multigraph = Graphs.createDirected(Graphs.MULTIGRAPH);
      ImmutableDirectedGraph.Builder immutableGraphBuilder = ImmutableDirectedGraph.builder();
      immutableGraphBuilder.addGraph(multigraph);
      fail("Should have rejected a graph with an incompatible multigraph configuration");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void addGraph_incompatibleSelfLoopConfig() {
    try {
      DirectedGraph graph = Graphs.createDirected(Graphs.config().noSelfLoops());
      ImmutableDirectedGraph.Builder immutableGraphBuilder = ImmutableDirectedGraph.builder();
      immutableGraphBuilder.addGraph(graph);
      fail("Should have rejected a graph with an incompatible self-loop configuration");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void addGraph() {
    DirectedGraph<Integer, String> graph = Graphs.createDirected(immutableGraph.config());
    populateInputGraph(graph);
    assertThat(builder.addGraph(graph).build()).isEqualTo(graph);
  }

  @Test
  public void addGraph_overlap() {
    DirectedGraph<Integer, String> graph = Graphs.createDirected(immutableGraph.config());
    populateInputGraph(graph);
    // Add an edge that is in 'graph' (overlap)
    builder.addEdge(E12, N1, N2);
    builder.addGraph(graph);
    assertThat(builder.build()).isEqualTo(graph);
  }

  @Test
  public void addGraph_inconsistentEdges() {
    DirectedGraph<Integer, String> graph = Graphs.createDirected(immutableGraph.config());
    populateInputGraph(graph);
    builder.addEdge(E21, N3, N1);
    try {
      builder.addGraph(graph);
      fail("Should have rejected a graph whose edge definitions were inconsistent with existing"
          + "builder state");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void toString_emptyGraph() {
    assertThat(graph.toString()).isEqualTo(String.format("config: %s, nodes: %s, edges: {}",
        graph.config(), graph.nodes()));
  }

  @Test
  public void toString_noEdges() {
    addNode(N1);
    assertThat(graph.toString()).isEqualTo(String.format("config: %s, nodes: %s, edges: {}",
        graph.config(), graph.nodes()));
  }

  @Test
  public void toString_singleEdge() {
    addEdge(E12, N1, N2);
    assertThat(graph.toString()).isEqualTo(String.format(
        "config: %s, nodes: %s, edges: {%s=<%s -> %s>}",
        graph.config(), graph.nodes(), E12, N1, N2));
  }

  @Test
  public void toString_multipleNodesAndEdges() {
    addEdge(E12, N1, N2);
    addEdge(E13, N1, N3);
    assertThat(graph.toString()).isEqualTo(String.format(
        "config: %s, nodes: %s, edges: {%s=<%s -> %s>, %s=<%s -> %s>}",
        graph.config(),
        graph.nodes(),
        E12, N1, N2,
        E13, N1, N3));
  }

  protected void populateInputGraph(DirectedGraph<Integer, String> graph) {
    graph.addEdge(E12, N1, N2);
    graph.addEdge(E21, N2, N1);
    graph.addEdge(E23, N2, N3);
    graph.addNode(N5);
  }
}
