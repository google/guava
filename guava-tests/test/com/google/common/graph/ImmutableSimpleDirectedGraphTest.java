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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ImmutableDirectedGraph}, creating a simple directed graph (parallel and
 * self-loop edges are not allowed).
 */
@RunWith(JUnit4.class)
public class ImmutableSimpleDirectedGraphTest extends AbstractImmutableDirectedGraphTest {
  protected ImmutableDirectedGraph.Builder<Integer, String> builder;

  @Override
  @CanIgnoreReturnValue
  final boolean addNode(Integer n) {
    DirectedGraph<Integer, String> oldGraph = Graphs.copyOf(directedGraph);
    graph = directedGraph = builder.addNode(n).build();
    return !graph.equals(oldGraph);
  }

  @Override
  @CanIgnoreReturnValue
  final boolean addEdge(String e, Integer n1, Integer n2) {
    DirectedGraph<Integer, String> oldGraph = Graphs.copyOf(directedGraph);
    graph = directedGraph = builder.addEdge(e, n1, n2).build();
    return !graph.equals(oldGraph);
  }

  @Override
  public ImmutableDirectedGraph<Integer, String> createGraph() {
    builder = ImmutableDirectedGraph.builder(Graphs.config().noSelfLoops());
    return builder.build();
  }

  @Test
  public void addEdge_selfLoop() {
    try {
      addEdge(E11, N1, N1);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_SELF_LOOP);
    }
  }

  /**
   * This test checks an implementation dependent feature. It tests that
   * the method {@code addEdge} will silently add the missing nodes to the builder,
   * then add the edge connecting them.
   */
  @Test
  public void addEdge_nodesNotInGraph() {
    addNode(N1);
    assertTrue(addEdge(E15, N1, N5));
    assertTrue(addEdge(E41, N4, N1));
    assertTrue(addEdge(E23, N2, N3));
    assertThat(directedGraph.nodes()).containsExactly(N1, N5, N4, N2, N3).inOrder();
    assertThat(directedGraph.edges()).containsExactly(E15, E41, E23).inOrder();
    assertThat(directedGraph.edgesConnecting(N1, N5)).containsExactly(E15);
    assertThat(directedGraph.edgesConnecting(N4, N1)).containsExactly(E41);
    assertThat(directedGraph.edgesConnecting(N2, N3)).containsExactly(E23);
    // Direction of the added edge is correctly handled
    assertThat(directedGraph.edgesConnecting(N3, N2)).isEmpty();
  }

  @Test
  public void copyOf() {
    DirectedGraph<Integer, String> graph = Graphs.createDirected(directedGraph.config());
    populateInputGraph(graph);
    assertThat(ImmutableDirectedGraph.copyOf(graph)).isEqualTo(graph);
  }

  @Test
  public void addGraph() {
    DirectedGraph<Integer, String> graph = Graphs.createDirected(directedGraph.config());
    populateInputGraph(graph);
    assertThat(builder.addGraph(graph).build()).isEqualTo(graph);
  }

  @Test
  public void addGraph_overlap() {
    DirectedGraph<Integer, String> graph = Graphs.createDirected(directedGraph.config());
    populateInputGraph(graph);
    // Add an edge that is in 'graph' (overlap)
    builder.addEdge(E12, N1, N2);
    builder.addGraph(graph);
    assertThat(builder.build()).isEqualTo(graph);
  }

  @Test
  public void addGraph_inconsistentEdges() {
    DirectedGraph<Integer, String> graph = Graphs.createDirected(directedGraph.config());
    populateInputGraph(graph);
    builder.addEdge(E21, N3, N1);
    try {
      builder.addGraph(graph);
      fail("Should have rejected a graph whose edge definitions were inconsistent with existing"
          + "builder state");
    } catch (IllegalArgumentException expected) {
    }
  }

  protected void populateInputGraph(DirectedGraph<Integer, String> graph) {
    graph.addEdge(E12, N1, N2);
    graph.addEdge(E21, N2, N1);
    graph.addEdge(E23, N2, N3);
    graph.addNode(N5);
  }
}
