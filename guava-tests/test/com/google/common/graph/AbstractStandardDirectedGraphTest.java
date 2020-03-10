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

import static com.google.common.graph.GraphConstants.ENDPOINTS_MISMATCH;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;
import org.junit.Test;

/**
 * Abstract base class for testing directed {@link Graph} implementations defined in this package.
 */
public abstract class AbstractStandardDirectedGraphTest extends AbstractGraphTest {

  @Override
  @Test
  public void nodes_checkReturnedSetMutability() {
    assume().that(graphIsMutable()).isTrue();

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
    assume().that(graphIsMutable()).isTrue();

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
    assume().that(graphIsMutable()).isTrue();

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
    assume().that(graphIsMutable()).isTrue();

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
    assume().that(graphIsMutable()).isTrue();

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

  @Test
  public void predecessors_oneEdge() {
    putEdge(N1, N2);
    assertThat(graph.predecessors(N2)).containsExactly(N1);
    // Edge direction handled correctly
    assertThat(graph.predecessors(N1)).isEmpty();
  }

  @Test
  public void successors_oneEdge() {
    putEdge(N1, N2);
    assertThat(graph.successors(N1)).containsExactly(N2);
    // Edge direction handled correctly
    assertThat(graph.successors(N2)).isEmpty();
  }

  @Test
  public void incidentEdges_oneEdge() {
    putEdge(N1, N2);
    EndpointPair<Integer> expectedEndpoints = EndpointPair.ordered(N1, N2);
    assertThat(graph.incidentEdges(N1)).containsExactly(expectedEndpoints);
    assertThat(graph.incidentEdges(N2)).containsExactly(expectedEndpoints);
  }

  @Test
  public void inDegree_oneEdge() {
    putEdge(N1, N2);
    assertThat(graph.inDegree(N2)).isEqualTo(1);
    // Edge direction handled correctly
    assertThat(graph.inDegree(N1)).isEqualTo(0);
  }

  @Test
  public void outDegree_oneEdge() {
    putEdge(N1, N2);
    assertThat(graph.outDegree(N1)).isEqualTo(1);
    // Edge direction handled correctly
    assertThat(graph.outDegree(N2)).isEqualTo(0);
  }

  @Test
  public void hasEdgeConnecting_correct() {
    putEdge(N1, N2);
    assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(N1, N2))).isTrue();
  }

  @Test
  public void hasEdgeConnecting_backwards() {
    putEdge(N1, N2);
    assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(N2, N1))).isFalse();
  }

  @Test
  public void hasEdgeConnecting_mismatch() {
    putEdge(N1, N2);
    assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(N1, N2))).isFalse();
    assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(N2, N1))).isFalse();
  }

  @Test
  public void adjacentNodes_selfLoop() {
    assume().that(graph.allowsSelfLoops()).isTrue();

    putEdge(N1, N1);
    putEdge(N1, N2);
    assertThat(graph.adjacentNodes(N1)).containsExactly(N1, N2);
  }

  @Test
  public void predecessors_selfLoop() {
    assume().that(graph.allowsSelfLoops()).isTrue();

    putEdge(N1, N1);
    assertThat(graph.predecessors(N1)).containsExactly(N1);
    putEdge(N4, N1);
    assertThat(graph.predecessors(N1)).containsExactly(N1, N4);
  }

  @Test
  public void successors_selfLoop() {
    assume().that(graph.allowsSelfLoops()).isTrue();

    putEdge(N1, N1);
    assertThat(graph.successors(N1)).containsExactly(N1);
    putEdge(N1, N2);
    assertThat(graph.successors(N1)).containsExactly(N1, N2);
  }

  @Test
  public void incidentEdges_selfLoop() {
    assume().that(graph.allowsSelfLoops()).isTrue();

    putEdge(N1, N1);
    assertThat(graph.incidentEdges(N1)).containsExactly(EndpointPair.ordered(N1, N1));
    putEdge(N1, N2);
    assertThat(graph.incidentEdges(N1))
        .containsExactly(EndpointPair.ordered(N1, N1), EndpointPair.ordered(N1, N2));
  }

  @Test
  public void degree_selfLoop() {
    assume().that(graph.allowsSelfLoops()).isTrue();

    putEdge(N1, N1);
    assertThat(graph.degree(N1)).isEqualTo(2);
    putEdge(N1, N2);
    assertThat(graph.degree(N1)).isEqualTo(3);
  }

  @Test
  public void inDegree_selfLoop() {
    assume().that(graph.allowsSelfLoops()).isTrue();

    putEdge(N1, N1);
    assertThat(graph.inDegree(N1)).isEqualTo(1);
    putEdge(N4, N1);
    assertThat(graph.inDegree(N1)).isEqualTo(2);
  }

  @Test
  public void outDegree_selfLoop() {
    assume().that(graph.allowsSelfLoops()).isTrue();

    putEdge(N1, N1);
    assertThat(graph.outDegree(N1)).isEqualTo(1);
    putEdge(N1, N2);
    assertThat(graph.outDegree(N1)).isEqualTo(2);
  }

  // Stable order tests

  // Note: Stable order means that the ordering doesn't change between iterations and versions.
  // Ideally, the ordering in test should never be updated.
  @Test
  public void stableIncidentEdgeOrder_edges_returnsInStableOrder() {
    assume().that(graph.incidentEdgeOrder().type()).isEqualTo(ElementOrder.Type.STABLE);

    populateStarShapedGraph();

    assertThat(graph.edges())
        .containsExactly(
            EndpointPair.ordered(2, 1),
            EndpointPair.ordered(1, 4),
            EndpointPair.ordered(1, 3),
            EndpointPair.ordered(1, 2),
            EndpointPair.ordered(3, 1),
            EndpointPair.ordered(5, 1))
        .inOrder();
  }

  @Test
  public void stableIncidentEdgeOrder_adjacentNodes_returnsInConnectingEdgeInsertionOrder() {
    assume().that(graph.incidentEdgeOrder().type()).isEqualTo(ElementOrder.Type.STABLE);

    populateStarShapedGraph();

    assertThat(graph.adjacentNodes(1)).containsExactly(2, 4, 3, 5).inOrder();
  }

  @Test
  public void stableIncidentEdgeOrder_predecessors_returnsInConnectingEdgeInsertionOrder() {
    assume().that(graph.incidentEdgeOrder().type()).isEqualTo(ElementOrder.Type.STABLE);

    populateStarShapedGraph();

    assertThat(graph.predecessors(1)).containsExactly(2, 5, 3).inOrder();
  }

  @Test
  public void stableIncidentEdgeOrder_successors_returnsInConnectingEdgeInsertionOrder() {
    assume().that(graph.incidentEdgeOrder().type()).isEqualTo(ElementOrder.Type.STABLE);

    populateStarShapedGraph();

    assertThat(graph.successors(1)).containsExactly(4, 3, 2).inOrder();
  }

  @Test
  public void stableIncidentEdgeOrder_incidentEdges_returnsInEdgeInsertionOrder() {
    assume().that(graph.incidentEdgeOrder().type()).isEqualTo(ElementOrder.Type.STABLE);

    populateStarShapedGraph();

    assertThat(graph.incidentEdges(1))
        .containsExactly(
            EndpointPair.ordered(2, 1),
            EndpointPair.ordered(1, 4),
            EndpointPair.ordered(1, 3),
            EndpointPair.ordered(5, 1),
            EndpointPair.ordered(1, 2),
            EndpointPair.ordered(3, 1))
        .inOrder();
  }

  @Test
  public void stableIncidentEdgeOrder_incidentEdges_withSelfLoop_returnsInEdgeInsertionOrder() {
    assume().that(graph.incidentEdgeOrder().type()).isEqualTo(ElementOrder.Type.STABLE);
    assume().that(graph.allowsSelfLoops()).isTrue();

    putEdge(2, 1);
    putEdge(1, 1);
    putEdge(1, 3);
    putEdge(1, 2);

    assertThat(graph.incidentEdges(1))
        .containsExactly(
            EndpointPair.ordered(2, 1),
            EndpointPair.ordered(1, 1),
            EndpointPair.ordered(1, 3),
            EndpointPair.ordered(1, 2))
        .inOrder();
  }

  /**
   * Populates the graph with nodes and edges in a star shape with node `1` in the middle.
   *
   * <p>Note that the edges are added in a shuffled order to properly test the effect of the
   * insertion order.
   */
  private void populateStarShapedGraph() {
    putEdge(2, 1);
    putEdge(1, 4);
    putEdge(1, 3);
    putEdge(5, 1);
    putEdge(1, 2);
    putEdge(3, 1);
  }

  // Element Mutation

  @Test
  public void putEdge_existingNodes() {
    assume().that(graphIsMutable()).isTrue();

    // Adding nodes initially for safety (insulating from possible future
    // modifications to proxy methods)
    addNode(N1);
    addNode(N2);

    assertThat(graphAsMutableGraph.putEdge(N1, N2)).isTrue();
  }

  @Test
  public void putEdge_existingEdgeBetweenSameNodes() {
    assume().that(graphIsMutable()).isTrue();

    assertThat(graphAsMutableGraph.putEdge(N1, N2)).isTrue();
    assertThat(graphAsMutableGraph.putEdge(N1, N2)).isFalse();
  }

  @Test
  public void putEdge_orderMismatch() {
    assume().that(graphIsMutable()).isTrue();

    EndpointPair<Integer> endpoints = EndpointPair.unordered(N1, N2);
    try {
      graphAsMutableGraph.putEdge(endpoints);
      fail("Expected IllegalArgumentException: " + ENDPOINTS_MISMATCH);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
    }
  }

  /**
   * Tests that the method {@code putEdge} will silently add the missing nodes to the graph, then
   * add the edge connecting them. We are not using the proxy methods here as we want to test {@code
   * putEdge} when the end-points are not elements of the graph.
   */
  @Test
  public void putEdge_nodesNotInGraph() {
    assume().that(graphIsMutable()).isTrue();

    graphAsMutableGraph.addNode(N1);
    assertTrue(graphAsMutableGraph.putEdge(N1, N5));
    assertTrue(graphAsMutableGraph.putEdge(N4, N1));
    assertTrue(graphAsMutableGraph.putEdge(N2, N3));
    assertThat(graph.nodes()).containsExactly(N1, N5, N4, N2, N3).inOrder();
    assertThat(graph.successors(N1)).containsExactly(N5);
    assertThat(graph.successors(N2)).containsExactly(N3);
    assertThat(graph.successors(N3)).isEmpty();
    assertThat(graph.successors(N4)).containsExactly(N1);
    assertThat(graph.successors(N5)).isEmpty();
  }

  @Test
  public void putEdge_doesntAllowSelfLoops() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(graph.allowsSelfLoops()).isFalse();

    try {
      graphAsMutableGraph.putEdge(N1, N1);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().contains(ERROR_SELF_LOOP);
    }
  }

  @Test
  public void putEdge_allowsSelfLoops() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(graph.allowsSelfLoops()).isTrue();

    assertThat(graphAsMutableGraph.putEdge(N1, N1)).isTrue();
    assertThat(graph.successors(N1)).containsExactly(N1);
    assertThat(graph.predecessors(N1)).containsExactly(N1);
  }

  @Test
  public void putEdge_existingSelfLoopEdgeBetweenSameNodes() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(graph.allowsSelfLoops()).isTrue();

    graphAsMutableGraph.putEdge(N1, N1);
    assertThat(graphAsMutableGraph.putEdge(N1, N1)).isFalse();
  }

  @Test
  public void removeEdge_antiparallelEdges() {
    assume().that(graphIsMutable()).isTrue();

    putEdge(N1, N2);
    putEdge(N2, N1);

    assertThat(graphAsMutableGraph.removeEdge(N1, N2)).isTrue();
    assertThat(graph.successors(N1)).isEmpty();
    assertThat(graph.predecessors(N1)).containsExactly(N2);
    assertThat(graph.edges()).hasSize(1);

    assertThat(graphAsMutableGraph.removeEdge(N2, N1)).isTrue();
    assertThat(graph.successors(N1)).isEmpty();
    assertThat(graph.predecessors(N1)).isEmpty();
    assertThat(graph.edges()).isEmpty();
  }

  @Test
  public void removeEdge_orderMismatch() {
    assume().that(graphIsMutable()).isTrue();

    putEdge(N1, N2);
    EndpointPair<Integer> endpoints = EndpointPair.unordered(N1, N2);
    try {
      graphAsMutableGraph.removeEdge(endpoints);
      fail("Expected IllegalArgumentException: " + ENDPOINTS_MISMATCH);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
    }
  }

  @Test
  public void removeNode_existingNodeWithSelfLoopEdge() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(graph.allowsSelfLoops()).isTrue();

    addNode(N1);
    putEdge(N1, N1);
    assertThat(graphAsMutableGraph.removeNode(N1)).isTrue();
    assertThat(graph.nodes()).isEmpty();
  }

  @Test
  public void removeEdge_existingSelfLoopEdge() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(graph.allowsSelfLoops()).isTrue();

    putEdge(N1, N1);
    assertThat(graphAsMutableGraph.removeEdge(N1, N1)).isTrue();
    assertThat(graph.nodes()).containsExactly(N1);
    assertThat(graph.successors(N1)).isEmpty();
  }
}
