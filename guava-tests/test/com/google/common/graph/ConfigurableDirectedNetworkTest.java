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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for a directed {@link ConfigurableMutableNetwork} with default graph properties.
 */
@RunWith(JUnit4.class)
public class ConfigurableDirectedNetworkTest extends ConfigurableSimpleDirectedNetworkTest {

  @Override
  public MutableNetwork<Integer, String> createGraph() {
    return NetworkBuilder.directed().build();
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
    assertThat(network.incidentNodes(E11).source()).isEqualTo(N1);
    assertThat(network.incidentNodes(E11).target()).isEqualTo(N1);
  }

  @Test
  public void adjacentNodes_selfLoop() {
    addEdge(E11, N1, N1);
    addEdge(E12, N1, N2);
    assertThat(network.adjacentNodes(N1)).containsExactly(N1, N2);
  }

  @Test
  public void edgesConnecting_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
    addEdge(E12, N1, N2);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void inEdges_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.inEdges(N1)).containsExactly(E11);
    addEdge(E41, N4, N1);
    assertThat(network.inEdges(N1)).containsExactly(E11, E41);
  }

  @Test
  public void outEdges_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.outEdges(N1)).containsExactly(E11);
    addEdge(E12, N1, N2);
    assertThat(network.outEdges(N1)).containsExactly(E11, E12);
  }

  @Test
  public void predecessors_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.predecessors(N1)).containsExactly(N1);
    addEdge(E41, N4, N1);
    assertThat(network.predecessors(N1)).containsExactly(N1, N4);
  }

  @Test
  public void successors_selfLoop() {
    addEdge(E11, N1, N1);
    assertThat(network.successors(N1)).containsExactly(N1);
    addEdge(E12, N1, N2);
    assertThat(network.successors(N1)).containsExactly(N1, N2);
  }

  @Test
  public void source_selfLoop() {
    addEdge(E11, N1, N1);
    assertEquals(N1, network.incidentNodes(E11).source());
  }

  @Test
  public void target_selfLoop() {
    addEdge(E11, N1, N1);
    assertEquals(N1, network.incidentNodes(E11).target());
  }

  @Override
  @Test
  public void addEdge_selfLoop() {
    assertTrue(addEdge(E11, N1, N1));
    assertThat(network.edges()).contains(E11);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void addEdge_existingSelfLoopEdgeBetweenSameNodes() {
    addEdge(E11, N1, N1);
    ImmutableSet<String> edges = ImmutableSet.copyOf(network.edges());
    assertFalse(addEdge(E11, N1, N1));
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
    assertTrue(network.removeNode(N1));
    assertThat(network.nodes()).isEmpty();
    assertThat(network.edges()).doesNotContain(E11);
  }

  @Test
  public void removeEdge_existingSelfLoopEdge() {
    addEdge(E11, N1, N1);
    assertTrue(network.removeEdge(E11));
    assertThat(network.edges()).doesNotContain(E11);
    assertThat(network.edgesConnecting(N1, N1)).isEmpty();
  }
}
