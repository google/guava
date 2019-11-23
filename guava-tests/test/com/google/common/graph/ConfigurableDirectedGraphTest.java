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

/** Tests for a directed {@link ConfigurableMutableGraph} allowing self-loops. */
@RunWith(JUnit4.class)
public class ConfigurableDirectedGraphTest extends ConfigurableSimpleDirectedGraphTest {

  @Override
  public MutableGraph<Integer> createGraph() {
    return GraphBuilder.directed().allowsSelfLoops(true).build();
  }

  @Test
  public void adjacentNodes_selfLoop() {
    putEdge(N1, N1);
    putEdge(N1, N2);
    assertThat(graph.adjacentNodes(N1)).containsExactly(N1, N2);
  }

  @Test
  public void predecessors_selfLoop() {
    putEdge(N1, N1);
    assertThat(graph.predecessors(N1)).containsExactly(N1);
    putEdge(N4, N1);
    assertThat(graph.predecessors(N1)).containsExactly(N1, N4);
  }

  @Test
  public void successors_selfLoop() {
    putEdge(N1, N1);
    assertThat(graph.successors(N1)).containsExactly(N1);
    putEdge(N1, N2);
    assertThat(graph.successors(N1)).containsExactly(N1, N2);
  }

  @Test
  public void incidentEdges_selfLoop() {
    putEdge(N1, N1);
    assertThat(graph.incidentEdges(N1)).containsExactly(EndpointPair.ordered(N1, N1));
    putEdge(N1, N2);
    assertThat(graph.incidentEdges(N1))
        .containsExactly(EndpointPair.ordered(N1, N1), EndpointPair.ordered(N1, N2));
  }

  @Test
  public void degree_selfLoop() {
    putEdge(N1, N1);
    assertThat(graph.degree(N1)).isEqualTo(2);
    putEdge(N1, N2);
    assertThat(graph.degree(N1)).isEqualTo(3);
  }

  @Test
  public void inDegree_selfLoop() {
    putEdge(N1, N1);
    assertThat(graph.inDegree(N1)).isEqualTo(1);
    putEdge(N4, N1);
    assertThat(graph.inDegree(N1)).isEqualTo(2);
  }

  @Test
  public void outDegree_selfLoop() {
    putEdge(N1, N1);
    assertThat(graph.outDegree(N1)).isEqualTo(1);
    putEdge(N1, N2);
    assertThat(graph.outDegree(N1)).isEqualTo(2);
  }

  @Override
  @Test
  public void addEdge_selfLoop() {
    assertThat(putEdge(N1, N1)).isTrue();
    assertThat(graph.successors(N1)).containsExactly(N1);
    assertThat(graph.predecessors(N1)).containsExactly(N1);
  }

  @Test
  public void addEdge_existingSelfLoopEdgeBetweenSameNodes() {
    putEdge(N1, N1);
    assertThat(putEdge(N1, N1)).isFalse();
  }

  @Test
  public void removeNode_existingNodeWithSelfLoopEdge() {
    addNode(N1);
    putEdge(N1, N1);
    assertThat(graph.removeNode(N1)).isTrue();
    assertThat(graph.nodes()).isEmpty();
  }

  @Test
  public void removeEdge_existingSelfLoopEdge() {
    putEdge(N1, N1);
    assertThat(graph.removeEdge(N1, N1)).isTrue();
    assertThat(graph.nodes()).containsExactly(N1);
    assertThat(graph.successors(N1)).isEmpty();
  }
}
