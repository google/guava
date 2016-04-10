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

import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

/**
 * Abstract base class for testing implementations of {@link Network} interface.
 *
 * <p>This class is responsible for testing that a directed implementation of {@link Network}
 * is correctly handling directed edges. Implementation-dependent test cases are left to
 * subclasses. Test cases that do not require the graph to be directed are found in superclasses.
 *
 */
public abstract class AbstractDirectedNetworkTest extends AbstractNetworkTest {

  @After
  public void validateSourceAndTarget() {
    for (Integer node : graph.nodes()) {
      for (String inEdge : graph.inEdges(node)) {
        Integer oppositeNode = Graphs.oppositeNode(graph, inEdge, node);
        assertThat(graph.source(inEdge)).isEqualTo(oppositeNode);
        assertThat(graph.target(inEdge)).isEqualTo(node);
      }

      for (String outEdge : graph.outEdges(node)) {
        Integer oppositeNode = Graphs.oppositeNode(graph, outEdge, node);
        assertThat(graph.source(outEdge)).isEqualTo(node);
        assertThat(graph.target(outEdge)).isEqualTo(oppositeNode);
      }

      for (Integer adjacentNode : graph.adjacentNodes(node)) {
        Set<String> edges = graph.edgesConnecting(node, adjacentNode);
        Set<String> antiParallelEdges = graph.edgesConnecting(adjacentNode, node);
        assertTrue(node.equals(adjacentNode) || Collections.disjoint(edges, antiParallelEdges));
      }
    }
  }

  @Override
  @Test
  public void incidentNodes_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(graph.incidentNodes(E12)).containsExactly(N1, N2).inOrder();
  }

  @Test
  public void edgesConnecting_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(graph.edgesConnecting(N1, N2)).containsExactly(E12);
    // Passed nodes should be in the correct edge direction, first is the
    // source node and the second is the target node
    assertThat(graph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void inEdges_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(graph.inEdges(N2)).containsExactly(E12);
    // Edge direction handled correctly
    assertThat(graph.inEdges(N1)).isEmpty();
  }

  @Test
  public void outEdges_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(graph.outEdges(N1)).containsExactly(E12);
    // Edge direction handled correctly
    assertThat(graph.outEdges(N2)).isEmpty();
  }

  @Test
  public void predecessors_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(graph.predecessors(N2)).containsExactly(N1);
    // Edge direction handled correctly
    assertThat(graph.predecessors(N1)).isEmpty();
  }

  @Test
  public void successors_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(graph.successors(N1)).containsExactly(N2);
    // Edge direction handled correctly
    assertThat(graph.successors(N2)).isEmpty();
  }

  @Test
  public void inDegree_oneEdge() {
    addEdge(E12, N1, N2);
    assertEquals(1, graph.inDegree(N2));
    // Edge direction handled correctly
    assertEquals(0, graph.inDegree(N1));
  }

  @Test
  public void outDegree_oneEdge() {
    addEdge(E12, N1, N2);
    assertEquals(1, graph.outDegree(N1));
    // Edge direction handled correctly
    assertEquals(0, graph.outDegree(N2));
  }

  @Test
  public void source_oneEdge() {
    addEdge(E12, N1, N2);
    assertEquals(N1, graph.source(E12));
  }

  @Test
  public void source_edgeNotInGraph() {
    try {
      graph.source(EDGE_NOT_IN_GRAPH);
      fail(ERROR_EDGE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertEdgeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void target_oneEdge() {
    addEdge(E12, N1, N2);
    assertEquals(N2, graph.target(E12));
  }

  @Test
  public void target_edgeNotInGraph() {
    try {
      graph.target(EDGE_NOT_IN_GRAPH);
      fail(ERROR_EDGE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertEdgeNotInGraphErrorMessage(e);
    }
  }

  // Element Mutation

  @Test
  public void addEdge_existingNodes() {
    // Adding nodes initially for safety (insulating from possible future
    // modifications to proxy methods)
    addNode(N1);
    addNode(N2);
    assertTrue(addEdge(E12, N1, N2));
    assertThat(graph.edges()).contains(E12);
    assertThat(graph.edgesConnecting(N1, N2)).containsExactly(E12);
    // Direction of the added edge is correctly handled
    assertThat(graph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void addEdge_existingEdgeBetweenSameNodes() {
    addEdge(E12, N1, N2);
    ImmutableSet<String> edges = ImmutableSet.copyOf(graph.edges());
    assertFalse(addEdge(E12, N1, N2));
    assertThat(graph.edges()).containsExactlyElementsIn(edges);
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
    try {
      // Edge between same nodes but in reverse direction
      addEdge(E12, N2, N1);
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
  }

  @Test
  public void removeEdge_existingEdge() {
    addEdge(E12, N1, N2);
    assertTrue(graph.removeEdge(E12));
    assertThat(graph.edges()).doesNotContain(E12);
    assertThat(graph.edgesConnecting(N1, N2)).isEmpty();
  }
}
