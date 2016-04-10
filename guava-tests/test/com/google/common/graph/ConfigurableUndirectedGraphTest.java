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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for an undirected {@link ConfigurableGraph} with default graph properties.
 */
@RunWith(JUnit4.class)
public class ConfigurableUndirectedGraphTest extends ConfigurableSimpleUndirectedGraphTest {

  @Override
  public MutableGraph<Integer> createGraph() {
    return GraphBuilder.undirected().build();
  }

  @Test
  public void adjacentNodes_selfLoop() {
    addEdge(N1, N1);
    addEdge(N1, N2);
    assertThat(graph.adjacentNodes(N1)).containsExactly(N1, N2);
  }

  @Test
  public void predecessors_selfLoop() {
    addEdge(N1, N1);
    assertThat(graph.predecessors(N1)).containsExactly(N1);
    addEdge(N1, N2);
    assertThat(graph.predecessors(N1)).containsExactly(N1, N2);
  }

  @Test
  public void successors_selfLoop() {
    addEdge(N1, N1);
    assertThat(graph.successors(N1)).containsExactly(N1);
    addEdge(N2, N1);
    assertThat(graph.successors(N1)).containsExactly(N1, N2);
  }

  @Test
  public void degree_selfLoop() {
    addEdge(N1, N1);
    assertEquals(1, graph.degree(N1));
    addEdge(N1, N2);
    assertEquals(2, graph.degree(N1));
  }

  @Test
  public void inDegree_selfLoop() {
    addEdge(N1, N1);
    assertEquals(1, graph.inDegree(N1));
    addEdge(N1, N2);
    assertEquals(2, graph.inDegree(N1));
  }

  @Test
  public void outDegree_selfLoop() {
    addEdge(N1, N1);
    assertEquals(1, graph.outDegree(N1));
    addEdge(N2, N1);
    assertEquals(2, graph.outDegree(N1));
  }

  @Override
  @Test
  public void addEdge_selfLoop() {
    assertTrue(addEdge(N1, N1));
    assertThat(graph.adjacentNodes(N1)).containsExactly(N1);
  }

  @Test
  public void addEdge_existingSelfLoopEdgeBetweenSameNodes() {
    addEdge(N1, N1);
    assertFalse(addEdge(N1, N1));
  }

  @Test
  public void removeNode_existingNodeWithSelfLoopEdge() {
    addNode(N1);
    addEdge(N1, N1);
    assertTrue(graph.removeNode(N1));
    assertThat(graph.nodes()).isEmpty();
  }

  @Test
  public void removeEdge_existingSelfLoopEdge() {
    addEdge(N1, N1);
    assertTrue(graph.removeEdge(N1, N1));
    assertThat(graph.nodes()).containsExactly(N1);
    assertThat(graph.adjacentNodes(N1)).isEmpty();
  }
}
