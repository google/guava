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

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link GraphProperties}.
 */
@RunWith(JUnit4.class)
public class GraphPropertiesTest {
  ImmutableList<MutableGraph<Integer>> graphsToTest;
  Graph<Integer> directedGraph;
  Graph<Integer> undirectedGraph;

  ImmutableList<MutableNetwork<Integer, String>> networksToTest;
  Network<Integer, String> directedNetwork;
  Network<Integer, String> undirectedNetwork;

  @Before
  public void init() {
    graphsToTest = ImmutableList.of(
        GraphBuilder.directed().<Integer>build(),
        GraphBuilder.undirected().<Integer>build());
    directedGraph = graphsToTest.get(0);
    undirectedGraph = graphsToTest.get(1);

    networksToTest = ImmutableList.of(
        NetworkBuilder.directed().allowsParallelEdges(true).<Integer, String>build(),
        NetworkBuilder.undirected().allowsParallelEdges(true).<Integer, String>build());
    directedNetwork = networksToTest.get(0);
    undirectedNetwork = networksToTest.get(1);
  }

  @Test
  public void isCyclic_emptyGraph() {
    assertThat(isCyclic(directedGraph)).isFalse();
    assertThat(isCyclic(undirectedGraph)).isFalse();
  }

  @Test
  public void isCyclic_isolatedNodes() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.addNode(1);
      graph.addNode(2);
    }
    assertThat(isCyclic(directedGraph)).isFalse();
    assertThat(isCyclic(undirectedGraph)).isFalse();
  }

  @Test
  public void isCyclic_oneEdge() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
    }
    assertThat(isCyclic(directedGraph)).isFalse();
    assertThat(isCyclic(undirectedGraph)).isFalse();
  }

  @Test
  public void isCyclic_selfLoopEdge() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 1);
    }
    assertThat(isCyclic(directedGraph)).isTrue();
    assertThat(isCyclic(undirectedGraph)).isTrue();
  }

  @Test
  public void isCyclic_twoAcyclicEdges() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
      graph.putEdge(1, 3);
    }
    assertThat(isCyclic(directedGraph)).isFalse();
    assertThat(isCyclic(undirectedGraph)).isFalse();
  }

  @Test
  public void isCyclic_twoCyclicEdges() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
      graph.putEdge(2, 1); // no-op in undirected case
    }
    assertThat(isCyclic(directedGraph)).isTrue();
    assertThat(isCyclic(undirectedGraph)).isFalse();
  }

  @Test
  public void isCyclic_threeAcyclicEdges() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
      graph.putEdge(2, 3);
      graph.putEdge(1, 3);
    }
    assertThat(isCyclic(directedGraph)).isFalse();
    assertThat(isCyclic(undirectedGraph)).isTrue(); // cyclic in undirected case
  }

  @Test
  public void isCyclic_threeCyclicEdges() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
      graph.putEdge(2, 3);
      graph.putEdge(3, 1);
    }
    assertThat(isCyclic(directedGraph)).isTrue();
    assertThat(isCyclic(undirectedGraph)).isTrue();
  }

  @Test
  public void isCyclic_disconnectedCyclicGraph() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
      graph.putEdge(2, 1); // no-op in undirected case
      graph.addNode(3);
    }
    assertThat(isCyclic(directedGraph)).isTrue();
    assertThat(isCyclic(undirectedGraph)).isFalse();
  }

  @Test
  public void isCyclic_multipleCycles() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
      graph.putEdge(2, 1);
      graph.putEdge(2, 3);
      graph.putEdge(3, 1);
    }
    assertThat(isCyclic(directedGraph)).isTrue();
    assertThat(isCyclic(undirectedGraph)).isTrue();
  }

  @Test
  public void isCyclic_twoParallelEdges() {
    for (MutableNetwork<Integer, String> network : networksToTest) {
      network.addEdge(1, 2, "1-2a");
      network.addEdge(1, 2, "1-2b");
    }
    assertThat(isCyclic(directedNetwork)).isFalse();
    assertThat(isCyclic(undirectedNetwork)).isTrue(); // cyclic in undirected case
  }

  @Test
  public void isCyclic_cyclicMultigraph() {
    for (MutableNetwork<Integer, String> network : networksToTest) {
      network.addEdge(1, 2, "1-2a");
      network.addEdge(1, 2, "1-2b");
      network.addEdge(2, 3, "2-3");
      network.addEdge(3, 1, "3-1");
    }
    assertThat(isCyclic(directedNetwork)).isTrue();
    assertThat(isCyclic(undirectedNetwork)).isTrue();
  }
}
