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
import java.util.Collections;
import java.util.Set;
import org.junit.After;
import org.junit.Test;

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
    for (Integer node : network.nodes()) {
      for (String inEdge : network.inEdges(node)) {
        Endpoints<Integer> endpoints = network.incidentNodes(inEdge);
        assertThat(endpoints.source()).isEqualTo(endpoints.adjacentNode(node));
        assertThat(endpoints.target()).isEqualTo(node);
      }

      for (String outEdge : network.outEdges(node)) {
        Endpoints<Integer> endpoints = network.incidentNodes(outEdge);
        assertThat(endpoints.source()).isEqualTo(node);
        assertThat(endpoints.target()).isEqualTo(endpoints.adjacentNode(node));
      }

      for (Integer adjacentNode : network.adjacentNodes(node)) {
        Set<String> edges = network.edgesConnecting(node, adjacentNode);
        Set<String> antiParallelEdges = network.edgesConnecting(adjacentNode, node);
        assertThat(
            node.equals(adjacentNode) || Collections.disjoint(edges, antiParallelEdges)).isTrue();
      }
    }
  }

  @Override
  @Test
  public void incidentNodes_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.incidentNodes(E12).source()).isEqualTo(N1);
    assertThat(network.incidentNodes(E12).target()).isEqualTo(N2);
  }

  @Test
  public void edgesConnecting_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    // Passed nodes should be in the correct edge direction, first is the
    // source node and the second is the target node
    assertThat(network.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void inEdges_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.inEdges(N2)).containsExactly(E12);
    // Edge direction handled correctly
    assertThat(network.inEdges(N1)).isEmpty();
  }

  @Test
  public void outEdges_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.outEdges(N1)).containsExactly(E12);
    // Edge direction handled correctly
    assertThat(network.outEdges(N2)).isEmpty();
  }

  @Test
  public void predecessors_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.predecessors(N2)).containsExactly(N1);
    // Edge direction handled correctly
    assertThat(network.predecessors(N1)).isEmpty();
  }

  @Test
  public void successors_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.successors(N1)).containsExactly(N2);
    // Edge direction handled correctly
    assertThat(network.successors(N2)).isEmpty();
  }

  @Test
  public void source_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.incidentNodes(E12).source()).isEqualTo(N1);
  }

  @Test
  public void source_edgeNotInGraph() {
    try {
      network.incidentNodes(EDGE_NOT_IN_GRAPH).source();
      fail(ERROR_EDGE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertEdgeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void target_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.incidentNodes(E12).target()).isEqualTo(N2);
  }

  @Test
  public void target_edgeNotInGraph() {
    try {
      network.incidentNodes(EDGE_NOT_IN_GRAPH).target();
      fail(ERROR_EDGE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertEdgeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void inDegree_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.inDegree(N2)).isEqualTo(1);
    // Edge direction handled correctly
    assertThat(network.inDegree(N1)).isEqualTo(0);
  }

  @Test
  public void outDegree_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.outDegree(N1)).isEqualTo(1);
    // Edge direction handled correctly
    assertThat(network.outDegree(N2)).isEqualTo(0);
  }

  // Element Mutation

  @Test
  public void addEdge_existingNodes() {
    // Adding nodes initially for safety (insulating from possible future
    // modifications to proxy methods)
    addNode(N1);
    addNode(N2);
    assertThat(addEdge(E12, N1, N2)).isTrue();
    assertThat(network.edges()).contains(E12);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    // Direction of the added edge is correctly handled
    assertThat(network.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void addEdge_existingEdgeBetweenSameNodes() {
    addEdge(E12, N1, N2);
    ImmutableSet<String> edges = ImmutableSet.copyOf(network.edges());
    assertThat(addEdge(E12, N1, N2)).isFalse();
    assertThat(network.edges()).containsExactlyElementsIn(edges);
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
}
