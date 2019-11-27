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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for a directed {@link ConfigurableMutableGraph} with stable incident edge order. */
@RunWith(JUnit4.class)
public class ConfigurableDirectedGraphWithStableOrderTest
    extends ConfigurableSimpleDirectedGraphTest {

  @Override
  public MutableGraph<Integer> createGraph() {
    return GraphBuilder.directed().incidentEdgeOrder(ElementOrder.stable()).build();
  }

  // Note: Stable order means that the ordering doesn't change between iterations and versions.
  // Ideally, the ordering in test should never be updated.
  @Test
  public void edges_returnsInStableOrder() {
    populateStarShapedGraph(graph);

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
  public void adjacentNodes_returnsInConnectingEdgeInsertionOrder() {
    populateStarShapedGraph(graph);

    assertThat(graph.adjacentNodes(1)).containsExactly(2, 4, 3, 5).inOrder();
  }

  @Test
  public void predecessors_returnsInConnectingEdgeInsertionOrder() {
    populateStarShapedGraph(graph);

    assertThat(graph.predecessors(1)).containsExactly(2, 5, 3).inOrder();
  }

  @Test
  public void successors_returnsInConnectingEdgeInsertionOrder() {
    populateStarShapedGraph(graph);

    assertThat(graph.successors(1)).containsExactly(4, 3, 2).inOrder();
  }

  // Note: Stable order means that the ordering doesn't change between iterations and versions.
  // Ideally, the ordering in test should never be updated.
  @Test
  public void incidentEdges_returnsInEdgeInsertionOrder() {
    populateStarShapedGraph(graph);

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

  /**
   * Populates the given graph with nodes and edges in a star shape with node `1` in the middle.
   *
   * <p>Note that the edges are added in a shuffled order to properly test the effect of the
   * insertion order.
   */
  private static void populateStarShapedGraph(MutableGraph<Integer> graph) {
    graph.putEdge(2, 1);
    graph.putEdge(1, 4);
    graph.putEdge(1, 3);
    graph.putEdge(5, 1);
    graph.putEdge(1, 2);
    graph.putEdge(3, 1);
  }
}
