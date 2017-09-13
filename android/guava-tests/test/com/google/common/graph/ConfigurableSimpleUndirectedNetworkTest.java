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

import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for an undirected {@link ConfigurableMutableNetwork}, creating a simple undirected graph
 * (parallel and self-loop edges are not allowed).
 */
@RunWith(JUnit4.class)
public class ConfigurableSimpleUndirectedNetworkTest extends AbstractUndirectedNetworkTest {

  @Override
  public MutableNetwork<Integer, String> createGraph() {
    return NetworkBuilder.undirected().allowsParallelEdges(false).allowsSelfLoops(false).build();
  }

  @Override
  @Test
  public void nodes_checkReturnedSetMutability() {
    Set<Integer> nodes = network.nodes();
    try {
      nodes.add(N2);
      fail(ERROR_MODIFIABLE_COLLECTION);
    } catch (UnsupportedOperationException e) {
      addNode(N1);
      assertThat(network.nodes()).containsExactlyElementsIn(nodes);
    }
  }

  @Override
  @Test
  public void edges_checkReturnedSetMutability() {
    Set<String> edges = network.edges();
    try {
      edges.add(E12);
      fail(ERROR_MODIFIABLE_COLLECTION);
    } catch (UnsupportedOperationException e) {
      addEdge(N1, N2, E12);
      assertThat(network.edges()).containsExactlyElementsIn(edges);
    }
  }

  @Override
  @Test
  public void incidentEdges_checkReturnedSetMutability() {
    addNode(N1);
    Set<String> incidentEdges = network.incidentEdges(N1);
    try {
      incidentEdges.add(E12);
      fail(ERROR_MODIFIABLE_COLLECTION);
    } catch (UnsupportedOperationException e) {
      addEdge(N1, N2, E12);
      assertThat(network.incidentEdges(N1)).containsExactlyElementsIn(incidentEdges);
    }
  }

  @Override
  @Test
  public void adjacentNodes_checkReturnedSetMutability() {
    addNode(N1);
    Set<Integer> adjacentNodes = network.adjacentNodes(N1);
    try {
      adjacentNodes.add(N2);
      fail(ERROR_MODIFIABLE_COLLECTION);
    } catch (UnsupportedOperationException e) {
      addEdge(N1, N2, E12);
      assertThat(network.adjacentNodes(N1)).containsExactlyElementsIn(adjacentNodes);
    }
  }

  @Override
  public void adjacentEdges_checkReturnedSetMutability() {
    addEdge(N1, N2, E12);
    Set<String> adjacentEdges = network.adjacentEdges(E12);
    try {
      adjacentEdges.add(E23);
      fail(ERROR_MODIFIABLE_COLLECTION);
    } catch (UnsupportedOperationException e) {
      addEdge(N2, N3, E23);
      assertThat(network.adjacentEdges(E12)).containsExactlyElementsIn(adjacentEdges);
    }
  }

  @Override
  @Test
  public void edgesConnecting_checkReturnedSetMutability() {
    addNode(N1);
    addNode(N2);
    Set<String> edgesConnecting = network.edgesConnecting(N1, N2);
    try {
      edgesConnecting.add(E23);
      fail(ERROR_MODIFIABLE_COLLECTION);
    } catch (UnsupportedOperationException e) {
      addEdge(N1, N2, E12);
      assertThat(network.edgesConnecting(N1, N2)).containsExactlyElementsIn(edgesConnecting);
    }
  }

  @Override
  @Test
  public void inEdges_checkReturnedSetMutability() {
    addNode(N2);
    Set<String> inEdges = network.inEdges(N2);
    try {
      inEdges.add(E12);
      fail(ERROR_MODIFIABLE_COLLECTION);
    } catch (UnsupportedOperationException e) {
      addEdge(N1, N2, E12);
      assertThat(network.inEdges(N2)).containsExactlyElementsIn(inEdges);
    }
  }

  @Override
  @Test
  public void outEdges_checkReturnedSetMutability() {
    addNode(N1);
    Set<String> outEdges = network.outEdges(N1);
    try {
      outEdges.add(E12);
      fail(ERROR_MODIFIABLE_COLLECTION);
    } catch (UnsupportedOperationException e) {
      addEdge(N1, N2, E12);
      assertThat(network.outEdges(N1)).containsExactlyElementsIn(outEdges);
    }
  }

  @Override
  @Test
  public void predecessors_checkReturnedSetMutability() {
    addNode(N2);
    Set<Integer> predecessors = network.predecessors(N2);
    try {
      predecessors.add(N1);
      fail(ERROR_MODIFIABLE_COLLECTION);
    } catch (UnsupportedOperationException e) {
      addEdge(N1, N2, E12);
      assertThat(network.predecessors(N2)).containsExactlyElementsIn(predecessors);
    }
  }

  @Override
  @Test
  public void successors_checkReturnedSetMutability() {
    addNode(N1);
    Set<Integer> successors = network.successors(N1);
    try {
      successors.add(N2);
      fail(ERROR_MODIFIABLE_COLLECTION);
    } catch (UnsupportedOperationException e) {
      addEdge(N1, N2, E12);
      assertThat(network.successors(N1)).containsExactlyElementsIn(successors);
    }
  }

  // Element Mutation

  @Test
  public void addEdge_selfLoop() {
    try {
      addEdge(N1, N1, E11);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_SELF_LOOP);
    }
  }

  /**
   * This test checks an implementation dependent feature. It tests that the method {@code addEdge}
   * will silently add the missing nodes to the graph, then add the edge connecting them. We are not
   * using the proxy methods here as we want to test {@code addEdge} when the end-points are not
   * elements of the graph.
   */
  @Test
  public void addEdge_nodesNotInGraph() {
    network.addNode(N1);
    assertTrue(network.addEdge(N1, N5, E15));
    assertTrue(network.addEdge(N4, N1, E41));
    assertTrue(network.addEdge(N2, N3, E23));
    assertThat(network.nodes()).containsExactly(N1, N5, N4, N2, N3).inOrder();
    assertThat(network.edges()).containsExactly(E15, E41, E23).inOrder();
    assertThat(network.edgesConnecting(N1, N5)).containsExactly(E15);
    assertThat(network.edgesConnecting(N4, N1)).containsExactly(E41);
    assertThat(network.edgesConnecting(N2, N3)).containsExactly(E23);
    assertThat(network.edgesConnecting(N3, N2)).containsExactly(E23);
  }
}
