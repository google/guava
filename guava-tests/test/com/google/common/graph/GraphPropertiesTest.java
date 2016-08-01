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

import static com.google.common.graph.GraphProperties.isCyclic;
import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link GraphProperties}.
 */
@RunWith(JUnit4.class)
public class GraphPropertiesTest {

  @Test
  public void isCyclic_emptyGraph() {
    Graph<Integer> directedGraph = GraphBuilder.directed().build();
    assertThat(isCyclic(directedGraph)).isFalse();
  }

  @Test
  public void isCyclic_isolatedNodes() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    directedGraph.addNode(1);
    assertThat(isCyclic(directedGraph)).isFalse();
    directedGraph.addNode(2);
    assertThat(isCyclic(directedGraph)).isFalse();
  }

  @Test
  public void isCyclic_oneEdge() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    directedGraph.addEdge(1, 2);
    assertThat(isCyclic(directedGraph)).isFalse();
  }

  @Test
  public void isCyclic_selfLoopEdge() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    directedGraph.addEdge(1, 1);
    assertThat(isCyclic(directedGraph)).isTrue();
  }

  @Test
  public void isCyclic_twoAcyclicEdges() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    directedGraph.addEdge(1, 2);
    directedGraph.addEdge(1, 3);
    assertThat(isCyclic(directedGraph)).isFalse();
  }

  @Test
  public void isCyclic_twoCyclicEdges() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    directedGraph.addEdge(1, 2);
    directedGraph.addEdge(2, 1);
    assertThat(isCyclic(directedGraph)).isTrue();
  }

  @Test
  public void isCyclic_threeAcyclicEdges() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    directedGraph.addEdge(1, 2);
    directedGraph.addEdge(2, 3);
    directedGraph.addEdge(1, 3);
    assertThat(isCyclic(directedGraph)).isFalse();
  }

  @Test
  public void isCyclic_threeCyclicEdges() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    directedGraph.addEdge(1, 2);
    directedGraph.addEdge(2, 3);
    directedGraph.addEdge(3, 1);
    assertThat(isCyclic(directedGraph)).isTrue();
  }

  @Test
  public void isCyclic_disconnectedCyclicGraph() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    directedGraph.addEdge(1, 2);
    directedGraph.addEdge(2, 1);
    directedGraph.addNode(3);
    assertThat(isCyclic(directedGraph)).isTrue();
  }

  @Test
  public void isCyclic_multipleCycles() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    directedGraph.addEdge(1, 2);
    directedGraph.addEdge(2, 1);
    directedGraph.addEdge(2, 3);
    directedGraph.addEdge(3, 1);
    assertThat(isCyclic(directedGraph)).isTrue();
  }
}
