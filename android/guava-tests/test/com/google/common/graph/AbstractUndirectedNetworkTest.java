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
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.After;
import org.junit.Test;

/**
 * Abstract base class for testing undirected implementations of the {@link Network} interface.
 *
 * <p>This class is responsible for testing that an undirected implementation of {@link Network} is
 * correctly handling undirected edges. Implementation-dependent test cases are left to subclasses.
 * Test cases that do not require the graph to be undirected are found in superclasses.
 */
public abstract class AbstractUndirectedNetworkTest extends AbstractNetworkTest {
  private static final EndpointPair<Integer> ENDPOINTS_N1N2 = EndpointPair.ordered(N1, N2);
  private static final EndpointPair<Integer> ENDPOINTS_N2N1 = EndpointPair.ordered(N2, N1);

  @After
  public void validateUndirectedEdges() {
    for (Integer node : network.nodes()) {
      new EqualsTester()
          .addEqualityGroup(
              network.inEdges(node), network.outEdges(node), network.incidentEdges(node))
          .testEquals();
      new EqualsTester()
          .addEqualityGroup(
              network.predecessors(node), network.successors(node), network.adjacentNodes(node))
          .testEquals();

      for (Integer adjacentNode : network.adjacentNodes(node)) {
        assertThat(network.edgesConnecting(node, adjacentNode))
            .containsExactlyElementsIn(network.edgesConnecting(adjacentNode, node));
      }
    }
  }

  @Test
  public void edges_containsOrderMismatch() {
    addEdge(N1, N2, E12);
    assertThat(network.asGraph().edges()).contains(ENDPOINTS_N2N1);
    assertThat(network.asGraph().edges()).contains(ENDPOINTS_N1N2);
  }

  @Test
  public void edgesConnecting_orderMismatch() {
    addEdge(N1, N2, E12);
    assertThat(network.edgesConnecting(ENDPOINTS_N2N1)).containsExactly(E12);
    assertThat(network.edgesConnecting(ENDPOINTS_N1N2)).containsExactly(E12);
  }

  @Test
  public void edgeConnectingOrNull_orderMismatch() {
    addEdge(N1, N2, E12);
    assertThat(network.edgeConnectingOrNull(ENDPOINTS_N2N1)).isEqualTo(E12);
    assertThat(network.edgeConnectingOrNull(ENDPOINTS_N1N2)).isEqualTo(E12);
  }

  @Test
  public void edgesConnecting_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(network.edgesConnecting(N2, N1)).containsExactly(E12);
  }

  @Test
  public void inEdges_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.inEdges(N2)).containsExactly(E12);
    assertThat(network.inEdges(N1)).containsExactly(E12);
  }

  @Test
  public void outEdges_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.outEdges(N2)).containsExactly(E12);
    assertThat(network.outEdges(N1)).containsExactly(E12);
  }

  @Test
  public void predecessors_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.predecessors(N2)).containsExactly(N1);
    assertThat(network.predecessors(N1)).containsExactly(N2);
  }

  @Test
  public void successors_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.successors(N1)).containsExactly(N2);
    assertThat(network.successors(N2)).containsExactly(N1);
  }

  @Test
  public void inDegree_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.inDegree(N2)).isEqualTo(1);
    assertThat(network.inDegree(N1)).isEqualTo(1);
  }

  @Test
  public void outDegree_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.outDegree(N1)).isEqualTo(1);
    assertThat(network.outDegree(N2)).isEqualTo(1);
  }

  // Element Mutation

  @Test
  public void addEdge_existingNodes() {
    // Adding nodes initially for safety (insulating from possible future
    // modifications to proxy methods)
    addNode(N1);
    addNode(N2);
    assertThat(addEdge(N1, N2, E12)).isTrue();
    assertThat(network.edges()).contains(E12);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(network.edgesConnecting(N2, N1)).containsExactly(E12);
  }

  @Test
  public void addEdge_existingEdgeBetweenSameNodes() {
    assertThat(addEdge(N1, N2, E12)).isTrue();
    ImmutableSet<String> edges = ImmutableSet.copyOf(network.edges());
    assertThat(addEdge(N1, N2, E12)).isFalse();
    assertThat(network.edges()).containsExactlyElementsIn(edges);
    assertThat(addEdge(N2, N1, E12)).isFalse();
    assertThat(network.edges()).containsExactlyElementsIn(edges);
  }

  @Test
  public void addEdge_existingEdgeBetweenDifferentNodes() {
    addEdge(N1, N2, E12);
    try {
      // Edge between totally different nodes
      addEdge(N4, N5, E12);
      fail(ERROR_ADDED_EXISTING_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
  }

  @Test
  public void addEdge_parallelEdge() {
    addEdge(N1, N2, E12);
    try {
      addEdge(N1, N2, EDGE_NOT_IN_GRAPH);
      fail(ERROR_ADDED_PARALLEL_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
    try {
      addEdge(N2, N1, EDGE_NOT_IN_GRAPH);
      fail(ERROR_ADDED_PARALLEL_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
  }

  @Test
  public void addEdge_orderMismatch() {
    EndpointPair<Integer> endpoints = EndpointPair.ordered(N1, N2);
    assertThat(addEdge(endpoints, E12)).isTrue();
  }
}
