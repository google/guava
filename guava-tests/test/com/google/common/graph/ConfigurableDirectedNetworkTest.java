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

/** Tests for a directed {@link ConfigurableMutableNetwork} allowing self-loops. */
@RunWith(JUnit4.class)
public class ConfigurableDirectedNetworkTest extends ConfigurableSimpleDirectedNetworkTest {

  @Override
  public MutableNetwork<Integer, String> createGraph() {
    return NetworkBuilder.directed().allowsSelfLoops(true).build();
  }

  @Test
  public void edges_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.edges()).containsExactly(E11);
  }

  @Test
  public void incidentEdges_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.incidentEdges(N1)).containsExactly(E11);
  }

  @Test
  public void incidentNodes_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.incidentNodes(E11).source()).isEqualTo(N1);
    assertThat(network.incidentNodes(E11).target()).isEqualTo(N1);
  }

  @Test
  public void adjacentNodes_selfLoop() {
    addEdge(N1, N1, E11);
    addEdge(N1, N2, E12);
    assertThat(network.adjacentNodes(N1)).containsExactly(N1, N2);
  }

  @Test
  public void adjacentEdges_selfLoop() {
    addEdge(N1, N1, E11);
    addEdge(N1, N2, E12);
    assertThat(network.adjacentEdges(E11)).containsExactly(E12);
  }

  @Test
  public void edgesConnecting_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
    addEdge(N1, N2, E12);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void inEdges_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.inEdges(N1)).containsExactly(E11);
    addEdge(N4, N1, E41);
    assertThat(network.inEdges(N1)).containsExactly(E11, E41);
  }

  @Test
  public void outEdges_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.outEdges(N1)).containsExactly(E11);
    addEdge(N1, N2, E12);
    assertThat(network.outEdges(N1)).containsExactly(E11, E12);
  }

  @Test
  public void predecessors_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.predecessors(N1)).containsExactly(N1);
    addEdge(N4, N1, E41);
    assertThat(network.predecessors(N1)).containsExactly(N1, N4);
  }

  @Test
  public void successors_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.successors(N1)).containsExactly(N1);
    addEdge(N1, N2, E12);
    assertThat(network.successors(N1)).containsExactly(N1, N2);
  }

  @Test
  public void source_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.incidentNodes(E11).source()).isEqualTo(N1);
  }

  @Test
  public void target_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.incidentNodes(E11).target()).isEqualTo(N1);
  }

  @Test
  public void degree_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.degree(N1)).isEqualTo(2);
    addEdge(N1, N2, E12);
    assertThat(network.degree(N1)).isEqualTo(3);
  }

  @Test
  public void inDegree_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.inDegree(N1)).isEqualTo(1);
    addEdge(N4, N1, E41);
    assertThat(network.inDegree(N1)).isEqualTo(2);
  }

  @Test
  public void outDegree_selfLoop() {
    addEdge(N1, N1, E11);
    assertThat(network.outDegree(N1)).isEqualTo(1);
    addEdge(N1, N2, E12);
    assertThat(network.outDegree(N1)).isEqualTo(2);
  }

  @Override
  @Test
  public void addEdge_selfLoop() {
    assertThat(addEdge(N1, N1, E11)).isTrue();
    assertThat(network.edges()).contains(E11);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void addEdge_existingSelfLoopEdgeBetweenSameNodes() {
    addEdge(N1, N1, E11);
    ImmutableSet<String> edges = ImmutableSet.copyOf(network.edges());
    assertThat(addEdge(N1, N1, E11)).isFalse();
    assertThat(network.edges()).containsExactlyElementsIn(edges);
  }

  @Test
  public void addEdge_existingEdgeBetweenDifferentNodes_selfLoops() {
    addEdge(N1, N1, E11);
    try {
      addEdge(N1, N2, E11);
      fail("Reusing an existing self-loop edge to connect different nodes succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
    try {
      addEdge(N2, N2, E11);
      fail("Reusing an existing self-loop edge to make a different self-loop edge succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
    addEdge(N1, N2, E12);
    try {
      addEdge(N1, N1, E12);
      fail("Reusing an existing edge to add a self-loop edge between different nodes succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_REUSE_EDGE);
    }
  }

  @Test
  public void addEdge_parallelSelfLoopEdge() {
    addEdge(N1, N1, E11);
    try {
      addEdge(N1, N1, EDGE_NOT_IN_GRAPH);
      fail("Adding a parallel self-loop edge succeeded");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
  }

  @Test
  public void removeNode_existingNodeWithSelfLoopEdge() {
    addNode(N1);
    addEdge(N1, N1, E11);
    assertThat(network.removeNode(N1)).isTrue();
    assertThat(network.nodes()).isEmpty();
    assertThat(network.edges()).doesNotContain(E11);
  }

  @Test
  public void removeEdge_existingSelfLoopEdge() {
    addEdge(N1, N1, E11);
    assertThat(network.removeEdge(E11)).isTrue();
    assertThat(network.edges()).doesNotContain(E11);
    assertThat(network.edgesConnecting(N1, N1)).isEmpty();
  }
}
