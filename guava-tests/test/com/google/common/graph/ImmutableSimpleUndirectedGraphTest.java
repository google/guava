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
 * Tests for {@link ImmutableUndirectedGraph}, creating a simple undirected graph (parallel and
 * self-loop edges are not allowed)
 */
@RunWith(JUnit4.class)
public class ImmutableSimpleUndirectedGraphTest extends AbstractImmutableUndirectedGraphTest {
  protected ImmutableUndirectedGraph.Builder<Integer, String> builder;

  @Override
  @CanIgnoreReturnValue
  final boolean addNode(Integer n) {
    UndirectedGraph<Integer, String> oldGraph = Graphs.copyOf(undirectedGraph);
    graph = undirectedGraph = builder.addNode(n).build();
    return !graph.equals(oldGraph);
  }

  @Override
  @CanIgnoreReturnValue
  final boolean addEdge(String e, Integer n1, Integer n2) {
    UndirectedGraph<Integer, String> oldGraph = Graphs.copyOf(undirectedGraph);
    graph = undirectedGraph = builder.addEdge(e, n1, n2).build();
    return !graph.equals(oldGraph);
  }

  @Override
  public ImmutableUndirectedGraph<Integer, String> createGraph() {
    builder = ImmutableUndirectedGraph.builder(Graphs.config().noSelfLoops());
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
   * then add the edge connecting them. We are not using the proxy methods here
   * as we want to test {@code addEdge} when the end-points are not elements
   * of the graph.
   */
  @Test
  public void addEdge_nodesNotInGraph() {
    addNode(N1);
    assertTrue(addEdge(E15, N1, N5));
    assertTrue(addEdge(E41, N4, N1));
    assertTrue(addEdge(E23, N2, N3));
    assertThat(undirectedGraph.nodes()).containsExactly(N1, N5, N4, N2, N3).inOrder();
    assertThat(undirectedGraph.edges()).containsExactly(E15, E41, E23).inOrder();
    assertThat(undirectedGraph.edgesConnecting(N1, N5)).containsExactly(E15);
    assertThat(undirectedGraph.edgesConnecting(N4, N1)).containsExactly(E41);
    assertThat(undirectedGraph.edgesConnecting(N2, N3)).containsExactly(E23);
    assertThat(undirectedGraph.edgesConnecting(N3, N2)).containsExactly(E23);
  }

  @Test
  public void copyOf_nullArgument() {
    try {
      ImmutableUndirectedGraph.copyOf(null);
      fail("Should have rejected a null graph");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void copyOf() {
    UndirectedGraph<Integer, String> graph = Graphs.createUndirected(undirectedGraph.config());
    populateInputGraph(graph);
    assertThat(ImmutableUndirectedGraph.copyOf(graph)).isEqualTo(graph);
  }

  @Test
  public void addGraph() {
    UndirectedGraph<Integer, String> graph = Graphs.createUndirected(undirectedGraph.config());
    populateInputGraph(graph);
    assertThat(builder.addGraph(graph).build()).isEqualTo(graph);
  }

  @Test
  public void addGraph_overlap() {
    UndirectedGraph<Integer, String> graph = Graphs.createUndirected(undirectedGraph.config());
    populateInputGraph(graph);
    // Add an edge that is in 'graph' (overlap)
    builder.addEdge(E12, N1, N2);
    builder.addGraph(graph);
    assertThat(builder.build()).isEqualTo(graph);
  }

  @Test
  public void addGraph_inconsistentEdges() {
    UndirectedGraph<Integer, String> graph = Graphs.createUndirected(undirectedGraph.config());
    populateInputGraph(graph);
    builder.addEdge(E12, N5, N1);
    try {
      builder.addGraph(graph);
      fail("Should have rejected a graph whose edge definitions were inconsistent with existing"
          + "builder state");
    } catch (IllegalArgumentException expected) {
    }
  }

  protected void populateInputGraph(UndirectedGraph<Integer, String> graph) {
    graph.addEdge(E12, N1, N2);
    graph.addEdge(E23, N2, N3);
    graph.addNode(N5);
  }
}
