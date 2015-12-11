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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Set;

/**
 * Tests for {@link IncidenceSetUndirectedGraph}, creating a simple undirected graph (parallel and
 * self-loop edges are not allowed).
 */
@RunWith(JUnit4.class)
public class IncidenceSetSimpleUndirectedGraphTest extends AbstractUndirectedGraphTest {

  @Override
  public UndirectedGraph<Integer, String> createGraph() {
    return Graphs.createUndirected(Graphs.config().noSelfLoops());
  }

  @Override
  public void nodes_checkReturnedSetMutability() {
    Set<Integer> nodes = undirectedGraph.nodes();
    try {
      nodes.add(N2);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addNode(N1);
      assertThat(undirectedGraph.nodes()).containsExactlyElementsIn(nodes);
    }
  }

  @Override
  public void edges_checkReturnedSetMutability() {
    Set<String> edges = undirectedGraph.edges();
    try {
      edges.add(E12);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(undirectedGraph.edges()).containsExactlyElementsIn(edges);
    }
  }

  @Override
  public void incidentEdges_checkReturnedSetMutability() {
    addNode(N1);
    Set<String> incidentEdges = undirectedGraph.incidentEdges(N1);
    try {
      incidentEdges.add(E12);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(undirectedGraph.incidentEdges(N1)).containsExactlyElementsIn(incidentEdges);
    }
  }

  @Override
  public void incidentNodes_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    Set<Integer> incidentNodes = undirectedGraph.incidentNodes(E12);
    try {
      incidentNodes.add(N3);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Override
  public void adjacentNodes_checkReturnedSetMutability() {
    addNode(N1);
    Set<Integer> adjacentNodes = undirectedGraph.adjacentNodes(N1);
    try {
      adjacentNodes.add(N2);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(undirectedGraph.adjacentNodes(N1)).containsExactlyElementsIn(adjacentNodes);
    }
  }

  @Override
  public void adjacentEdges_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    Set<String> adjacentEdges = undirectedGraph.adjacentEdges(E12);
    try {
      adjacentEdges.add(E23);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E23, N2, N3);
      assertThat(undirectedGraph.adjacentEdges(E12)).containsExactlyElementsIn(adjacentEdges);
    }
  }

  @Override
  public void edgesConnecting_checkReturnedSetMutability() {
    addNode(N1);
    addNode(N2);
    Set<String> edgesConnecting = undirectedGraph.edgesConnecting(N1, N2);
    try {
      edgesConnecting.add(E23);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(undirectedGraph.edgesConnecting(N1, N2))
          .containsExactlyElementsIn(edgesConnecting);
    }
  }

  @Override
  public void inEdges_checkReturnedSetMutability() {
    addNode(N2);
    Set<String> inEdges = undirectedGraph.inEdges(N2);
    try {
      inEdges.add(E12);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(undirectedGraph.inEdges(N2)).containsExactlyElementsIn(inEdges);
    }
  }

  @Override
  public void outEdges_checkReturnedSetMutability() {
    addNode(N1);
    Set<String> outEdges = undirectedGraph.outEdges(N1);
    try {
      outEdges.add(E12);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(undirectedGraph.outEdges(N1)).containsExactlyElementsIn(outEdges);
    }
  }

  @Override
  public void predecessors_checkReturnedSetMutability() {
    addNode(N2);
    Set<Integer> predecessors = undirectedGraph.predecessors(N2);
    try {
      predecessors.add(N1);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(undirectedGraph.predecessors(N2)).containsExactlyElementsIn(predecessors);
    }
  }

  @Override
  public void successors_checkReturnedSetMutability() {
    addNode(N1);
    Set<Integer> successors = undirectedGraph.successors(N1);
    try {
      successors.add(N2);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(undirectedGraph.successors(N1)).containsExactlyElementsIn(successors);
    }
  }

  // Element Mutation

  @Test
  public void addEdge_selfLoop() {
    try {
      addEdge(E11, N1, N1);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_SELF_LOOP);
    }
  }

  /**
   * This test checks an implementation dependent feature. It tests that
   * the method {@code addEdge} will silently add the missing nodes to the graph,
   * then add the edge connecting them. We are not using the proxy methods here
   * as we want to test {@code addEdge} when the end-points are not elements
   * of the graph.
   */
  @Test
  public void addEdge_nodesNotInGraph() {
    undirectedGraph.addNode(N1);
    assertTrue(undirectedGraph.addEdge(E15, N1, N5));
    assertTrue(undirectedGraph.addEdge(E41, N4, N1));
    assertTrue(undirectedGraph.addEdge(E23, N2, N3));
    assertThat(undirectedGraph.nodes()).containsExactly(N1, N5, N4, N2, N3).inOrder();
    assertThat(undirectedGraph.edges()).containsExactly(E15, E41, E23).inOrder();
    assertThat(undirectedGraph.edgesConnecting(N1, N5)).containsExactly(E15);
    assertThat(undirectedGraph.edgesConnecting(N4, N1)).containsExactly(E41);
    assertThat(undirectedGraph.edgesConnecting(N2, N3)).containsExactly(E23);
    assertThat(undirectedGraph.edgesConnecting(N3, N2)).containsExactly(E23);
  }
}
