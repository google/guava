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
 * Tests for {@link ImmutableDirectedGraph} with default graph configuration.
 *
 * @see GraphConfig
 */
@RunWith(JUnit4.class)
public class ImmutableDirectedGraphTest extends ImmutableSimpleDirectedGraphTest {
  @Override
  public ImmutableDirectedGraph<Integer, String> createGraph() {
    builder = ImmutableDirectedGraph.builder();
    return builder.build();
  }

  @Test
  public void edges_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(graph.edges()).containsExactly(E11);
  }

  @Test
  public void incidentEdges_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(graph.incidentEdges(N1)).containsExactly(E11);
  }

  @Test
  public void incidentNodes_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(graph.incidentNodes(E11)).containsExactly(N1);
  }

  @Test
  public void adjacentNodes_selfLoop() {
    addEdge(E11, N1, N1);
    addEdge(E12, N1, N2);
    assertThat(graph.adjacentNodes(N1)).containsExactly(N1, N2).inOrder();
  }

  @Test
  public void adjacentEdges_selfLoop() {
    // An edge is never adjacent to itself
    addEdge(E11, N1, N1);
    assertThat(graph.adjacentEdges(E11)).isEmpty();
    addEdge(E12, N1, N2);
    assertThat(graph.adjacentEdges(E11)).containsExactly(E12);
  }

  @Test
  public void edgesConnecting_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(immutableGraph.edgesConnecting(N1, N1)).containsExactly(E11);
    addEdge(E12, N1, N2);
    assertThat(immutableGraph.edgesConnecting(N1, N2)).containsExactly(E12);
  }

  @Test
  public void inEdges_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(immutableGraph.inEdges(N1)).containsExactly(E11);
    addEdge(E41, N4, N1);
    assertThat(immutableGraph.inEdges(N1)).containsExactly(E11, E41).inOrder();
  }

  @Test
  public void outEdges_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(immutableGraph.outEdges(N1)).containsExactly(E11);
    addEdge(E12, N1, N2);
    assertThat(immutableGraph.outEdges(N1)).containsExactly(E11, E12).inOrder();
  }

  @Test
  public void predecessors_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(immutableGraph.predecessors(N1)).containsExactly(N1);
    addEdge(E41, N4, N1);
    assertThat(immutableGraph.predecessors(N1)).containsExactly(N1, N4).inOrder();
  }

  @Test
  public void successors_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(immutableGraph.successors(N1)).containsExactly(N1);
    addEdge(E12, N1, N2);
    assertThat(immutableGraph.successors(N1)).containsExactly(N1, N2).inOrder();
  }

  @Test
  public void degree_selfLoop() {
    addEdge(E11, N1, N1);
    assertEquals(1, graph.degree(N1));
    addEdge(E12, N1, N2);
    assertEquals(2, graph.degree(N1));
  }

  @Test
  public void inDegree_selfLoop() {
    addEdge(E11, N1, N1);
    assertEquals(1, immutableGraph.inDegree(N1));
    addEdge(E41, N4, N1);
    assertEquals(2, immutableGraph.inDegree(N1));
  }

  @Test
  public void outDegree_selfLoop() {
    addEdge(E11, N1, N1);
    assertEquals(1, immutableGraph.outDegree(N1));
    addEdge(E12, N1, N2);
    assertEquals(2, immutableGraph.outDegree(N1));
  }

  @Test
  public void source_selfLoop() {
    addEdge(E11, N1, N1);
    assertEquals(N1, immutableGraph.source(E11));
  }

  @Test
  public void target_selfLoop() {
    addEdge(E11, N1, N1);
    assertEquals(N1, immutableGraph.target(E11));
  }

  // Builder mutation methods

  @Override
  public void addEdge_builder_selfLoop() {
    assertTrue(addEdge(E11, N1, N1));
    assertThat(graph.edges()).contains(E11);
    assertThat(graph.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void addEdge_builder_existingEdgeBetweenDifferentNodes_selfLoops() {
    addEdge(E11, N1, N1);
    try {
      addEdge(E11, N1, N2);
      fail("Reusing an existing self-loop edge to connect different nodes succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
    try {
      addEdge(E11, N2, N2);
      fail("Reusing an existing self-loop edge to make a different self-loop edge succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
    addEdge(E12, N1, N2);
    try {
      addEdge(E12, N1, N1);
      fail("Reusing an existing edge to add a self-loop edge between different nodes succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
  }

  @Test
  public void addEdge_builder_parallelSelfLoopEdge() {
    addEdge(E11, N1, N1);
    try {
      addEdge(EDGE_NOT_IN_GRAPH, N1, N1);
      fail("Adding a parallel self-loop edge succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
  }

  @Test
  public void toString_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(graph.toString()).isEqualTo(String.format(
        "config: %s, nodes: %s, edges: {%s=<%s -> %s>}",
        graph.config(), graph.nodes(), E11, N1, N1));
  }

  @Override
  protected void populateInputGraph(DirectedGraph<Integer, String> graph) {
    super.populateInputGraph(graph);
    // add a self-loop
    graph.addEdge(E11, N1, N1);
  }
}
