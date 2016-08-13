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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.After;
import org.junit.Test;

/**
 * Abstract base class for testing undirected implementations of the {@link Network} interface.
 *
 * <p>This class is responsible for testing that an undirected implementation of {@link Network}
 * is correctly handling undirected edges.  Implementation-dependent test cases are left to
 * subclasses. Test cases that do not require the graph to be undirected are found in superclasses.
 */
public abstract class AbstractUndirectedNetworkTest extends AbstractNetworkTest {

  @After
  public void validateUndirectedEdges() {
    for (Integer node : network.nodes()) {
      new EqualsTester()
          .addEqualityGroup(
              network.inEdges(node),
              network.outEdges(node),
              network.incidentEdges(node))
          .testEquals();
      new EqualsTester()
          .addEqualityGroup(
              network.predecessors(node),
              network.successors(node),
              network.adjacentNodes(node))
          .testEquals();

      for (Integer adjacentNode : network.adjacentNodes(node)) {
        assertThat(network.edgesConnecting(node, adjacentNode))
            .containsExactlyElementsIn(network.edgesConnecting(adjacentNode, node));
      }
    }
  }

  @Test
  public void edgesConnecting_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(network.edgesConnecting(N2, N1)).containsExactly(E12);
  }

  @Test
  public void inEdges_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.inEdges(N2)).containsExactly(E12);
    assertThat(network.inEdges(N1)).containsExactly(E12);
  }

  @Test
  public void outEdges_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.outEdges(N2)).containsExactly(E12);
    assertThat(network.outEdges(N1)).containsExactly(E12);
  }

  @Test
  public void predecessors_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.predecessors(N2)).containsExactly(N1);
    assertThat(network.predecessors(N1)).containsExactly(N2);
  }

  @Test
  public void successors_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.successors(N1)).containsExactly(N2);
    assertThat(network.successors(N2)).containsExactly(N1);
  }

  // Element Mutation

  @Test
  public void addEdge_existingNodes() {
    // Adding nodes initially for safety (insulating from possible future
    // modifications to proxy methods)
    addNode(N1);
    addNode(N2);
    assertTrue(addEdge(E12, N1, N2));
    assertThat(network.edges()).contains(E12);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(network.edgesConnecting(N2, N1)).containsExactly(E12);
  }

  @Test
  public void addEdge_existingEdgeBetweenSameNodes() {
    addEdge(E12, N1, N2);
    ImmutableSet<String> edges = ImmutableSet.copyOf(network.edges());
    assertFalse(addEdge(E12, N1, N2));
    assertThat(network.edges()).containsExactlyElementsIn(edges);
    assertFalse(addEdge(E12, N2, N1));
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
}
