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

import org.junit.Test;

/**
 * Abstract base class for testing mutable implementations of
 * {@link UndirectedGraph} interface.
 *
 * <p>This class is responsible for testing that an implementation of the interface
 * {@code UndirectedGraph} is correctly handling undirected edges.
 * Implementation-dependent test cases are left to subclasses. Test cases that
 * do not require the graph to be undirected are found in superclasses.
 *
 */
public abstract class AbstractUndirectedGraphTest extends AbstractMutableGraphTest {
  UndirectedGraph<Integer, String> undirectedGraph;

  @Override
  public abstract UndirectedGraph<Integer, String> createGraph();

  @Override
  public void init() {
    graph = undirectedGraph = createGraph();
  }

  @Test
  public void edgesConnecting_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).containsExactly(E12);
  }

  @Test
  public void inEdges_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(undirectedGraph.inEdges(N2)).containsExactly(E12);
    assertThat(undirectedGraph.inEdges(N1)).containsExactly(E12);
  }

  @Test
  public void outEdges_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(undirectedGraph.outEdges(N2)).containsExactly(E12);
    assertThat(undirectedGraph.outEdges(N1)).containsExactly(E12);
  }

  @Test
  public void predecessors_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(undirectedGraph.predecessors(N2)).containsExactly(N1);
    assertThat(undirectedGraph.predecessors(N1)).containsExactly(N2);
  }

  @Test
  public void successors_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(undirectedGraph.successors(N1)).containsExactly(N2);
    assertThat(undirectedGraph.successors(N2)).containsExactly(N1);
  }

  @Test
  public void inDegree_oneEdge() {
    addEdge(E12, N1, N2);
    assertEquals(1, undirectedGraph.inDegree(N2));
    assertEquals(1, undirectedGraph.inDegree(N1));
  }

  @Test
  public void outDegree_oneEdge() {
    addEdge(E12, N1, N2);
    assertEquals(1, undirectedGraph.outDegree(N1));
    assertEquals(1, undirectedGraph.outDegree(N2));
  }

  // Element Mutation

  @Test
  public void addEdge_existingNodes() {
    // Adding nodes initially for safety (insulating from possible future
    // modifications to proxy methods)
    addNode(N1);
    addNode(N2);
    assertTrue(addEdge(E12, N1, N2));
    assertThat(undirectedGraph.edges()).contains(E12);
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).containsExactly(E12);
  }

  @Test
  public void addEdge_existingEdgeBetweenSameNodes() {
    addEdge(E12, N1, N2);
    ImmutableSet<String> edges = ImmutableSet.copyOf(undirectedGraph.edges());
    assertFalse(addEdge(E12, N1, N2));
    assertThat(undirectedGraph.edges()).containsExactlyElementsIn(edges);
    assertFalse(addEdge(E12, N2, N1));
    assertThat(undirectedGraph.edges()).containsExactlyElementsIn(edges);
  }

  @Test
  public void addEdge_existingEdgeBetweenDifferentNodes() {
    addEdge(E12, N1, N2);
    try {
      // Edge between totally different nodes
      addEdge(E12, N4, N5);
      fail(ERROR_ADDED_EXISTING_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
  }

  @Test
  public void addEdge_parallelEdge() {
    addEdge(E12, N1, N2);
    try {
      addEdge(EDGE_NOT_IN_GRAPH, N1, N2);
      fail(ERROR_ADDED_PARALLEL_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
    try {
      addEdge(EDGE_NOT_IN_GRAPH, N2, N1);
      fail(ERROR_ADDED_PARALLEL_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
  }

  @Test
  public void removeEdge_existingEdge() {
    addEdge(E12, N1, N2);
    assertTrue(undirectedGraph.removeEdge(E12));
    assertThat(undirectedGraph.edges()).doesNotContain(E12);
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).isEmpty();
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).isEmpty();
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
        "config: %s, nodes: %s, edges: {%s=[%s, %s]}",
        graph.config(), graph.nodes(), E12, N1, N2));
  }

  @Test
  public void toString_multipleNodesAndEdges() {
    addEdge(E12, N1, N2);
    addEdge(E13, N1, N3);
    assertThat(graph.toString()).isEqualTo(String.format(
        "config: %s, nodes: %s, edges: {%s=[%s, %s], %s=[%s, %s]}",
        graph.config(),
        graph.nodes(),
        E12, N1, N2,
        E13, N1, N3));
  }
}
