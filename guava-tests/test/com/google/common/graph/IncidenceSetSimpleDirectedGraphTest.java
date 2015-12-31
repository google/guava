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
 * Tests for {@link IncidenceSetDirectedGraph}, creating a simple directed graph (parallel and
 * self-loop edges are not allowed).
 */
@RunWith(JUnit4.class)
public class IncidenceSetSimpleDirectedGraphTest extends AbstractDirectedGraphTest {

  @Override
  public DirectedGraph<Integer, String> createGraph() {
    return Graphs.createDirected(Graphs.config().noSelfLoops());
  }

  @Override
  public void nodes_checkReturnedSetMutability() {
    Set<Integer> nodes = directedGraph.nodes();
    try {
      nodes.add(N2);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addNode(N1);
      assertThat(directedGraph.nodes()).containsExactlyElementsIn(nodes);
    }
  }

  @Override
  public void edges_checkReturnedSetMutability() {
    Set<String> edges = directedGraph.edges();
    try {
      edges.add(E12);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(directedGraph.edges()).containsExactlyElementsIn(edges);
    }
  }

  @Override
  public void incidentEdges_checkReturnedSetMutability() {
    addNode(N1);
    Set<String> incidentEdges = directedGraph.incidentEdges(N1);
    try {
      incidentEdges.add(E12);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(directedGraph.incidentEdges(N1)).containsExactlyElementsIn(incidentEdges);
    }
  }

  @Override
  public void incidentNodes_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    Set<Integer> incidentNodes = directedGraph.incidentNodes(E12);
    try {
      incidentNodes.add(N3);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Override
  public void adjacentNodes_checkReturnedSetMutability() {
    addNode(N1);
    Set<Integer> adjacentNodes = directedGraph.adjacentNodes(N1);
    try {
      adjacentNodes.add(N2);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(directedGraph.adjacentNodes(N1)).containsExactlyElementsIn(adjacentNodes);
    }
  }

  @Override
  public void adjacentEdges_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    Set<String> adjacentEdges = directedGraph.adjacentEdges(E12);
    try {
      adjacentEdges.add(E23);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E23, N2, N3);
      assertThat(directedGraph.adjacentEdges(E12)).containsExactlyElementsIn(adjacentEdges);
    }
  }

  @Override
  public void edgesConnecting_checkReturnedSetMutability() {
    addNode(N1);
    addNode(N2);
    Set<String> edgesConnecting = directedGraph.edgesConnecting(N1, N2);
    try {
      edgesConnecting.add(E23);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(directedGraph.edgesConnecting(N1, N2)).containsExactlyElementsIn(edgesConnecting);
    }
  }

  @Override
  public void inEdges_checkReturnedSetMutability() {
    addNode(N2);
    Set<String> inEdges = directedGraph.inEdges(N2);
    try {
      inEdges.add(E12);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(directedGraph.inEdges(N2)).containsExactlyElementsIn(inEdges);
    }
  }

  @Override
  public void outEdges_checkReturnedSetMutability() {
    addNode(N1);
    Set<String> outEdges = directedGraph.outEdges(N1);
    try {
      outEdges.add(E12);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(directedGraph.outEdges(N1)).containsExactlyElementsIn(outEdges);
    }
  }

  @Override
  public void predecessors_checkReturnedSetMutability() {
    addNode(N2);
    Set<Integer> predecessors = directedGraph.predecessors(N2);
    try {
      predecessors.add(N1);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(directedGraph.predecessors(N2)).containsExactlyElementsIn(predecessors);
    }
  }

  @Override
  public void successors_checkReturnedSetMutability() {
    addNode(N1);
    Set<Integer> successors = directedGraph.successors(N1);
    try {
      successors.add(N2);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addEdge(E12, N1, N2);
      assertThat(successors).containsExactlyElementsIn(directedGraph.successors(N1));
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
    directedGraph.addNode(N1);
    assertTrue(directedGraph.addEdge(E15, N1, N5));
    assertTrue(directedGraph.addEdge(E41, N4, N1));
    assertTrue(directedGraph.addEdge(E23, N2, N3));
    assertThat(directedGraph.nodes()).containsExactly(N1, N5, N4, N2, N3).inOrder();
    assertThat(directedGraph.edges()).containsExactly(E15, E41, E23).inOrder();
    assertThat(directedGraph.edgesConnecting(N1, N5)).containsExactly(E15);
    assertThat(directedGraph.edgesConnecting(N4, N1)).containsExactly(E41);
    assertThat(directedGraph.edgesConnecting(N2, N3)).containsExactly(E23);
    // Direction of the added edge is correctly handled
    assertThat(directedGraph.edgesConnecting(N3, N2)).isEmpty();
  }
}
