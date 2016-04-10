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
import static com.google.common.graph.GraphProperties.roots;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link GraphProperties}.
 */
@RunWith(JUnit4.class)
public class GraphPropertiesTest {
  private static final Integer N1 = 1;
  private static final Integer N2 = 2;
  private static final Integer N3 = 3;
  private static final String E11 = "1-1";
  private static final String E12 = "1-2";
  private static final String E12_A = "1-2a";
  private static final String E13 = "1-3";
  private static final String E21 = "2-1";
  private static final String E23 = "2-3";
  private static final String E31 = "3-1";

  @Test
  public void isCyclic_emptyGraph() {
    Network<Integer, String> directedGraph = NetworkBuilder.directed().build();
    assertThat(isCyclic(directedGraph)).isFalse();
  }

  @Test
  public void isCyclic_isolatedNodes() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addNode(N1);
    assertThat(isCyclic(directedGraph)).isFalse();
    directedGraph.addNode(N2);
    assertThat(isCyclic(directedGraph)).isFalse();
  }

  @Test
  public void isCyclic_oneEdge() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addEdge(E12, N1, N2);
    assertThat(isCyclic(directedGraph)).isFalse();
  }

  @Test
  public void isCyclic_selfLoopEdge() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addEdge(E11, N1, N1);
    assertThat(isCyclic(directedGraph)).isTrue();
  }

  @Test
  public void isCyclic_twoParallelEdges() {
    MutableNetwork<Integer, String> directedGraph =
        NetworkBuilder.directed().allowsParallelEdges(true).build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E12_A, N1, N2);
    assertThat(isCyclic(directedGraph)).isFalse();
  }

  @Test
  public void isCyclic_twoAcyclicEdges() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E13, N1, N3);
    assertThat(isCyclic(directedGraph)).isFalse();
  }

  @Test
  public void isCyclic_twoCyclicEdges() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E21, N2, N1);
    assertThat(isCyclic(directedGraph)).isTrue();
  }

  @Test
  public void isCyclic_threeAcyclicEdges() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E23, N2, N3);
    directedGraph.addEdge(E13, N1, N3);
    assertThat(isCyclic(directedGraph)).isFalse();
  }

  @Test
  public void isCyclic_threeCyclicEdges() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E23, N2, N3);
    directedGraph.addEdge(E31, N3, N1);
    assertThat(isCyclic(directedGraph)).isTrue();
  }

  @Test
  public void isCyclic_disconnectedCyclicGraph() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E21, N2, N1);
    directedGraph.addNode(N3);
    assertThat(isCyclic(directedGraph)).isTrue();
  }

  @Test
  public void isCyclic_cyclicMultigraph() {
    MutableNetwork<Integer, String> directedGraph =
        NetworkBuilder.directed().allowsParallelEdges(true).build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E12_A, N1, N2);
    directedGraph.addEdge(E23, N2, N3);
    directedGraph.addEdge(E31, N3, N1);
    assertThat(isCyclic(directedGraph)).isTrue();
  }

  @Test
  public void isCyclic_multipleCycles() {
    MutableNetwork<Integer, String> directedGraph =
        NetworkBuilder.directed().allowsParallelEdges(true).build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E21, N2, N1);
    directedGraph.addEdge(E23, N2, N3);
    directedGraph.addEdge(E31, N3, N1);
    assertThat(isCyclic(directedGraph)).isTrue();
  }

  @Test
  public void roots_emptyGraph() {
    Network<Integer, String> directedGraph = NetworkBuilder.directed().build();
    assertThat(roots(directedGraph)).isEmpty();
  }

  @Test
  public void roots_trivialGraph() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addNode(N1);
    assertThat(roots(directedGraph)).isEqualTo(ImmutableSet.of(N1));
  }

  @Test
  public void roots_nodeWithSelfLoop() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addNode(N1);
    directedGraph.addEdge(E11, N1, N1);
    assertThat(roots(directedGraph)).isEmpty();
  }

  @Test
  public void roots_nodeWithChildren() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E13, N1, N3);
    assertThat(roots(directedGraph)).isEqualTo(ImmutableSet.of(N1));
  }

  @Test
  public void roots_cycle() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E21, N2, N1);
    assertThat(roots(directedGraph)).isEmpty();
  }

  @Test
  public void roots_multipleRoots() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addNode(N1);
    directedGraph.addNode(N2);
    assertThat(roots(directedGraph)).isEqualTo(ImmutableSet.of(N1, N2));
  }
}
