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
 * Tests for a directed {@link ConfigurableMutableGraph}, creating a simple directed graph
 * (self-loop edges are not allowed).
 */
@RunWith(JUnit4.class)
public class ConfigurableSimpleDirectedGraphTest extends AbstractDirectedGraphTest {

  @Override
  public MutableGraph<Integer> createGraph() {
    return GraphBuilder.directed().allowsSelfLoops(false).build();
  }

  @Override
  @Test
  public void nodes_checkReturnedSetMutability() {
    Set<Integer> nodes = graph.nodes();
    try {
      nodes.add(N2);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      addNode(N1);
      assertThat(graph.nodes()).containsExactlyElementsIn(nodes);
    }
  }

  @Override
  @Test
  public void adjacentNodes_checkReturnedSetMutability() {
    addNode(N1);
    Set<Integer> adjacentNodes = graph.adjacentNodes(N1);
    try {
      adjacentNodes.add(N2);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      putEdge(N1, N2);
      assertThat(graph.adjacentNodes(N1)).containsExactlyElementsIn(adjacentNodes);
    }
  }

  @Override
  @Test
  public void predecessors_checkReturnedSetMutability() {
    addNode(N2);
    Set<Integer> predecessors = graph.predecessors(N2);
    try {
      predecessors.add(N1);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      putEdge(N1, N2);
      assertThat(graph.predecessors(N2)).containsExactlyElementsIn(predecessors);
    }
  }

  @Override
  @Test
  public void successors_checkReturnedSetMutability() {
    addNode(N1);
    Set<Integer> successors = graph.successors(N1);
    try {
      successors.add(N2);
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      putEdge(N1, N2);
      assertThat(successors).containsExactlyElementsIn(graph.successors(N1));
    }
  }

  @Override
  @Test
  public void incidentEdges_checkReturnedSetMutability() {
    addNode(N1);
    Set<EndpointPair<Integer>> incidentEdges = graph.incidentEdges(N1);
    try {
      incidentEdges.add(EndpointPair.ordered(N1, N2));
      fail(ERROR_MODIFIABLE_SET);
    } catch (UnsupportedOperationException e) {
      putEdge(N1, N2);
      assertThat(incidentEdges).containsExactlyElementsIn(graph.incidentEdges(N1));
    }
  }

  // Element Mutation

  @Test
  public void addEdge_selfLoop() {
    try {
      putEdge(N1, N1);
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
    graph.addNode(N1);
    assertTrue(graph.putEdge(N1, N5));
    assertTrue(graph.putEdge(N4, N1));
    assertTrue(graph.putEdge(N2, N3));
    assertThat(graph.nodes()).containsExactly(N1, N5, N4, N2, N3).inOrder();
    assertThat(graph.successors(N1)).containsExactly(N5);
    assertThat(graph.successors(N2)).containsExactly(N3);
    assertThat(graph.successors(N3)).isEmpty();
    assertThat(graph.successors(N4)).containsExactly(N1);
    assertThat(graph.successors(N5)).isEmpty();
  }
}
