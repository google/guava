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

import static com.google.common.graph.Graphs.hasCycle;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Graphs#hasCycle(Graph)} and {@link Graphs#hasCycle(Network)}. */
// TODO(user): Consider moving this to GraphsTest.
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
    MutableGraph<Integer> mutableDirectedGraph =
        GraphBuilder.directed().allowsSelfLoops(true).build();
    MutableGraph<Integer> mutableUndirectedGraph =
        GraphBuilder.undirected().allowsSelfLoops(true).build();
    graphsToTest = ImmutableList.of(mutableDirectedGraph, mutableUndirectedGraph);
    directedGraph = mutableDirectedGraph;
    undirectedGraph = mutableUndirectedGraph;

    MutableNetwork<Integer, String> mutableDirectedNetwork =
        NetworkBuilder.directed().allowsParallelEdges(true).allowsSelfLoops(true).build();
    MutableNetwork<Integer, String> mutableUndirectedNetwork =
        NetworkBuilder.undirected().allowsParallelEdges(true).allowsSelfLoops(true).build();
    networksToTest = ImmutableList.of(mutableDirectedNetwork, mutableUndirectedNetwork);
    directedNetwork = mutableDirectedNetwork;
    undirectedNetwork = mutableUndirectedNetwork;
  }

  @Test
  public void hasCycle_emptyGraph() {
    assertThat(hasCycle(directedGraph)).isFalse();
    assertThat(hasCycle(undirectedGraph)).isFalse();
  }

  @Test
  public void hasCycle_isolatedNodes() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.addNode(1);
      graph.addNode(2);
    }
    assertThat(hasCycle(directedGraph)).isFalse();
    assertThat(hasCycle(undirectedGraph)).isFalse();
  }

  @Test
  public void hasCycle_oneEdge() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
    }
    assertThat(hasCycle(directedGraph)).isFalse();
    assertThat(hasCycle(undirectedGraph)).isFalse();
  }

  @Test
  public void hasCycle_selfLoopEdge() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 1);
    }
    assertThat(hasCycle(directedGraph)).isTrue();
    assertThat(hasCycle(undirectedGraph)).isTrue();
  }

  @Test
  public void hasCycle_twoAcyclicEdges() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
      graph.putEdge(1, 3);
    }
    assertThat(hasCycle(directedGraph)).isFalse();
    assertThat(hasCycle(undirectedGraph)).isFalse();
  }

  @Test
  public void hasCycle_twoCyclicEdges() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
      graph.putEdge(2, 1); // no-op in undirected case
    }
    assertThat(hasCycle(directedGraph)).isTrue();
    assertThat(hasCycle(undirectedGraph)).isFalse();
  }

  @Test
  public void hasCycle_threeAcyclicEdges() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
      graph.putEdge(2, 3);
      graph.putEdge(1, 3);
    }
    assertThat(hasCycle(directedGraph)).isFalse();
    assertThat(hasCycle(undirectedGraph)).isTrue(); // cyclic in undirected case
  }

  @Test
  public void hasCycle_threeCyclicEdges() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
      graph.putEdge(2, 3);
      graph.putEdge(3, 1);
    }
    assertThat(hasCycle(directedGraph)).isTrue();
    assertThat(hasCycle(undirectedGraph)).isTrue();
  }

  @Test
  public void hasCycle_disconnectedCyclicGraph() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
      graph.putEdge(2, 1); // no-op in undirected case
      graph.addNode(3);
    }
    assertThat(hasCycle(directedGraph)).isTrue();
    assertThat(hasCycle(undirectedGraph)).isFalse();
  }

  @Test
  public void hasCycle_multipleCycles() {
    for (MutableGraph<Integer> graph : graphsToTest) {
      graph.putEdge(1, 2);
      graph.putEdge(2, 1);
      graph.putEdge(2, 3);
      graph.putEdge(3, 1);
    }
    assertThat(hasCycle(directedGraph)).isTrue();
    assertThat(hasCycle(undirectedGraph)).isTrue();
  }

  @Test
  public void hasCycle_twoParallelEdges() {
    for (MutableNetwork<Integer, String> network : networksToTest) {
      network.addEdge(1, 2, "1-2a");
      network.addEdge(1, 2, "1-2b");
    }
    assertThat(hasCycle(directedNetwork)).isFalse();
    assertThat(hasCycle(undirectedNetwork)).isTrue(); // cyclic in undirected case
  }

  @Test
  public void hasCycle_cyclicMultigraph() {
    for (MutableNetwork<Integer, String> network : networksToTest) {
      network.addEdge(1, 2, "1-2a");
      network.addEdge(1, 2, "1-2b");
      network.addEdge(2, 3, "2-3");
      network.addEdge(3, 1, "3-1");
    }
    assertThat(hasCycle(directedNetwork)).isTrue();
    assertThat(hasCycle(undirectedNetwork)).isTrue();
  }
}
