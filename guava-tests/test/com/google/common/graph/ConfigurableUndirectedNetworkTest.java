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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for an undirected {@link ConfigurableMutableNetwork} allowing self-loops.
 */
@RunWith(JUnit4.class)
public class ConfigurableUndirectedNetworkTest extends ConfigurableSimpleUndirectedNetworkTest {

  @Override
  public MutableNetwork<Integer, String> createGraph() {
    return NetworkBuilder.undirected().allowsSelfLoops(true).build();
  }

  @Test
  public void edges_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.edges()).containsExactly(E11);
  }

  @Test
  public void incidentEdges_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.incidentEdges(N1)).containsExactly(E11);
  }

  @Test
  public void incidentNodes_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.incidentNodes(E11).nodeA()).isEqualTo(N1);
    assertThat(network.incidentNodes(E11).nodeB()).isEqualTo(N1);
  }

  @Test
  public void adjacentNodes_selfLoop() {
    addEdge(E11, N1, N1);
    addEdge(E12, N1, N2);
    assertThat(network.adjacentNodes(N1)).containsExactly(N1, N2);
  }

  @Test
  public void adjacentEdges_selfLoop() {
    addEdge(E11, N1, N1);
    addEdge(E12, N1, N2);
    assertThat(network.adjacentEdges(E11)).containsExactly(E12);
  }

  @Test
  public void edgesConnecting_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
    addEdge(E12, N1, N2);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(network.edgesConnecting(N2, N1)).containsExactly(E12);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void inEdges_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.inEdges(N1)).containsExactly(E11);
    addEdge(E12, N1, N2);
    assertThat(network.inEdges(N1)).containsExactly(E11, E12);
  }

  @Test
  public void outEdges_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.outEdges(N1)).containsExactly(E11);
    addEdge(E12, N2, N1);
    assertThat(network.outEdges(N1)).containsExactly(E11, E12);
  }

  @Test
  public void predecessors_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.predecessors(N1)).containsExactly(N1);
    addEdge(E12, N1, N2);
    assertThat(network.predecessors(N1)).containsExactly(N1, N2);
  }

  @Test
  public void successors_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.successors(N1)).containsExactly(N1);
    addEdge(E12, N2, N1);
    assertThat(network.successors(N1)).containsExactly(N1, N2);
  }

  @Test
  public void degree_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.degree(N1)).isEqualTo(2);
    addEdge(E12, N1, N2);
    assertThat(network.degree(N1)).isEqualTo(3);
  }

  @Test
  public void inDegree_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.inDegree(N1)).isEqualTo(2);
    addEdge(E12, N1, N2);
    assertThat(network.inDegree(N1)).isEqualTo(3);
  }

  @Test
  public void outDegree_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.outDegree(N1)).isEqualTo(2);
    addEdge(E12, N2, N1);
    assertThat(network.outDegree(N1)).isEqualTo(3);
  }

  @Override
  @Test
  public void addEdge_selfLoop() {
    assertThat(addEdge(E11, N1, N1)).isTrue();
    assertThat(network.edges()).contains(E11);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void addEdge_existingSelfLoopEdgeBetweenSameNodes() {
    addEdge(E11, N1, N1);
    ImmutableSet<String> edges = ImmutableSet.copyOf(network.edges());
    assertThat(addEdge(E11, N1, N1)).isFalse();
    assertThat(network.edges()).containsExactlyElementsIn(edges);
  }

  @Test
  public void addEdge_existingEdgeBetweenDifferentNodes_selfLoops() {
    addEdge(E11, N1, N1);
    try {
      addEdge(E11, N1, N2);
      fail("Reusing an existing self-loop edge to connect different nodes succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
    try {
      addEdge(E11, N2, N2);
      fail("Reusing an existing self-loop edge to make a different self-loop edge succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
    addEdge(E12, N1, N2);
    try {
      addEdge(E12, N1, N1);
      fail("Reusing an existing edge to add a self-loop edge between different nodes succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
  }

  @Test
  public void addEdge_parallelSelfLoopEdge() {
    addEdge(E11, N1, N1);
    try {
      addEdge(EDGE_NOT_IN_GRAPH, N1, N1);
      fail("Adding a parallel self-loop edge succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
  }

  @Test
  public void removeNode_existingNodeWithSelfLoopEdge() {
    addNode(N1);
    addEdge(E11, N1, N1);
    assertThat(network.removeNode(N1)).isTrue();
    assertThat(network.nodes()).isEmpty();
    assertThat(network.edges()).doesNotContain(E11);
  }

  @Test
  public void removeEdge_existingSelfLoopEdge() {
    addEdge(E11, N1, N1);
    assertThat(network.removeEdge(E11)).isTrue();
    assertThat(network.edges()).doesNotContain(E11);
    assertThat(network.edgesConnecting(N1, N1)).isEmpty();
  }
}
