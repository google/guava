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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link IncidenceSetUndirectedGraph} with default graph configuration.
 *
 * @see GraphConfig
 */
@RunWith(JUnit4.class)
public class IncidenceSetUndirectedGraphTest extends IncidenceSetSimpleUndirectedGraphTest {
  @Override
  public UndirectedGraph<Integer, String> createGraph() {
    return Graphs.createUndirected();
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
    assertThat(undirectedGraph.edgesConnecting(N1, N1)).containsExactly(E11);
    addEdge(E12, N1, N2);
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).containsExactly(E12);
    assertThat(undirectedGraph.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void inEdges_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(undirectedGraph.inEdges(N1)).containsExactly(E11);
    addEdge(E12, N1, N2);
    assertThat(undirectedGraph.inEdges(N1)).containsExactly(E11, E12).inOrder();
  }

  @Test
  public void outEdges_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(undirectedGraph.outEdges(N1)).containsExactly(E11);
    addEdge(E12, N2, N1);
    assertThat(undirectedGraph.outEdges(N1)).containsExactly(E11, E12).inOrder();
  }

  @Test
  public void predecessors_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(undirectedGraph.predecessors(N1)).containsExactly(N1);
    addEdge(E12, N1, N2);
    assertThat(undirectedGraph.predecessors(N1)).containsExactly(N1, N2).inOrder();
  }

  @Test
  public void successors_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(undirectedGraph.successors(N1)).containsExactly(N1);
    addEdge(E12, N2, N1);
    assertThat(undirectedGraph.successors(N1)).containsExactly(N1, N2).inOrder();
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
    assertEquals(1, undirectedGraph.inDegree(N1));
    addEdge(E12, N1, N2);
    assertEquals(2, undirectedGraph.inDegree(N1));
  }

  @Test
  public void outDegree_selfLoop() {
    addEdge(E11, N1, N1);
    assertEquals(1, undirectedGraph.outDegree(N1));
    addEdge(E12, N2, N1);
    assertEquals(2, undirectedGraph.outDegree(N1));
  }

  @Override
  public void addEdge_selfLoop() {
    assertTrue(addEdge(E11, N1, N1));
    assertThat(graph.edges()).contains(E11);
    assertThat(graph.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void addEdge_existingSelfLoopEdgeBetweenSameNodes() {
    addEdge(E11, N1, N1);
    ImmutableSet<String> edges = ImmutableSet.copyOf(graph.edges());
    assertFalse(addEdge(E11, N1, N1));
    assertThat(graph.edges()).containsExactlyElementsIn(edges);
  }

  @Test
  public void addEdge_existingEdgeBetweenDifferentNodes_selfLoops() {
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
  public void addEdge_parallelSelfLoopEdge() {
    addEdge(E11, N1, N1);
    try {
      addEdge(EDGE_NOT_IN_GRAPH, N1, N1);
      fail("Adding a parallel self-loop edge succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
  }

  @Test
  public void removeNode_existingNodeWithSelfLoopEdge() {
    addNode(N1);
    addEdge(E11, N1, N1);
    assertTrue(graph.removeNode(N1));
    assertThat(graph.nodes()).isEmpty();
    assertThat(graph.edges()).doesNotContain(E11);
  }

  @Test
  public void removeEdge_existingSelfLoopEdge() {
    addEdge(E11, N1, N1);
    assertTrue(graph.removeEdge(E11));
    assertThat(graph.edges()).doesNotContain(E11);
    assertThat(graph.edgesConnecting(N1, N1)).isEmpty();
  }

  // TODO(kak): Can't we ditch this and just use PackageSanityTests?
  @Test
  public void testEquals() {
    UndirectedGraph<Integer, String> graphA = createGraph();
    graphA.addNode(N1);
    UndirectedGraph<Integer, String> graphB = createGraph();
    graphA.addNode(N2);

    new EqualsTester()
        .addEqualityGroup(graphA)
        .addEqualityGroup(graphB)
        .testEquals();
  }

  @Test
  public void toString_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(graph.toString()).isEqualTo(String.format(
        "config: %s, nodes: %s, edges: {%s=[%s]}",
        graph.config(), graph.nodes(), E11, N1));
  }
}
