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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.graph.Graphs.addEdge;
import static com.google.common.graph.Graphs.copyOf;
import static com.google.common.graph.Graphs.mergeEdgesFrom;
import static com.google.common.graph.Graphs.mergeNodesFrom;
import static com.google.common.graph.Graphs.oppositeNode;
import static com.google.common.graph.Graphs.selfLoopPredicate;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.Set;

/**
 * Tests for {@link Graphs}. Tests assume that the implementation of the method
 * {@code addEdge} adds the missing nodes to the graph, then adds the edge between them.
 */
@RunWith(JUnit4.class)
public class GraphsTest {
  private static final Integer N1 = 1;
  private static final Integer N2 = 2;
  private static final Integer N3 = 3;
  private static final Integer N4 = 4;
  private static final String E11 = "1-1";
  private static final String E11_A = "1-1a";
  private static final String E12 = "1-2";
  private static final String E12_A = "1-2a";
  private static final String E12_B = "1-2b";
  private static final String E21 = "2-1";
  private static final String E22 = "2-2";
  private static final String E13 = "1-3";
  private static final String E44 = "4-4";
  private static final int NODE_COUNT = 20;
  private static final int EDGE_COUNT = 20;
  // TODO(user): Consider adding both error messages from here and {@link AbstractGraphTest}
  // in one class (may be a utility class for error messages).
  private static final String ERROR_PARALLEL_EDGE = "connected by a different edge";
  private static final String ERROR_NEGATIVE_NODE_COUNT =
      "expected number of nodes can't be negative";
  private static final String ERROR_NEGATIVE_EDGE_COUNT =
      "expected number of edges can't be negative";
  private static final String ERROR_ADDED_PARALLEL_EDGE =
      "Should not be allowed to add a parallel edge.";
  private static final String ERROR_ADDED_SELF_LOOP =
      "Should not be allowed to add a self-loop edge.";
  static final String ERROR_SELF_LOOP = "self-loops are not allowed";

  @Test
  public void oppositeNode_basic() {
    List<Graph<Integer, String>> testGraphs = ImmutableList.of(
        GraphBuilder.directed().<Integer, String>build(),
        GraphBuilder.undirected().<Integer, String>build());
    for (Graph<Integer, String> graph : testGraphs) {
      graph.addEdge(E12, N1, N2);
      assertThat(oppositeNode(graph, E12, N1)).isEqualTo(N2);
      assertThat(oppositeNode(graph, E12, N2)).isEqualTo(N1);
    }
  }

  @Test
  public void oppositeNode_parallelEdge() {
    List<Graph<Integer, String>> testGraphs = ImmutableList.of(
        GraphBuilder.directed().allowsParallelEdges(true).<Integer, String>build(),
        GraphBuilder.undirected().allowsParallelEdges(true).<Integer, String>build());
    for (Graph<Integer, String> graph : testGraphs) {
      graph.addEdge(E12, N1, N2);
      graph.addEdge(E12_A, N1, N2);
      assertThat(oppositeNode(graph, E12, N1)).isEqualTo(N2);
      assertThat(oppositeNode(graph, E12, N2)).isEqualTo(N1);
      assertThat(oppositeNode(graph, E12_A, N1)).isEqualTo(N2);
      assertThat(oppositeNode(graph, E12_A, N2)).isEqualTo(N1);
    }
  }

  @Test
  public void oppositeNode_selfLoop() {
    List<Graph<Integer, String>> testGraphs = ImmutableList.of(
        GraphBuilder.directed().<Integer, String>build(),
        GraphBuilder.undirected().<Integer, String>build());
    for (Graph<Integer, String> graph : testGraphs) {
      graph.addEdge(E11, N1, N1);
      assertThat(oppositeNode(graph, E11, N1)).isEqualTo(N1);
    }
  }

  @Test
  public void oppositeNode_nodeNotIncident() {
    List<Graph<Integer, String>> testGraphs = ImmutableList.of(
        GraphBuilder.directed().<Integer, String>build(),
        GraphBuilder.undirected().<Integer, String>build());
    for (Graph<Integer, String> graph : testGraphs) {
      graph.addEdge(E12, N1, N2);
      graph.addEdge(E13, N1, N3);
      try {
        oppositeNode(graph, E12, N3);
        fail("Should have rejected oppositeNode() called without a node incident to edge");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @Test
  public void parallelEdges_directed() {
    Graph<Integer, String> directedGraph =
        GraphBuilder.directed().allowsParallelEdges(true).build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E12_A, N1, N2);
    directedGraph.addEdge(E21, N2, N1);
    assertThat(Graphs.parallelEdges(directedGraph, E12)).containsExactly(E12_A);
    assertThat(Graphs.parallelEdges(directedGraph, E12_A)).containsExactly(E12);
    assertThat(Graphs.parallelEdges(directedGraph, E21)).isEmpty();
  }

  @Test
  public void parallelEdges_selfLoop_directed() {
    Graph<Integer, String> directedGraph =
        GraphBuilder.directed().allowsParallelEdges(true).build();
    directedGraph.addEdge(E11, N1, N1);
    directedGraph.addEdge(E11_A, N1, N1);
    assertThat(Graphs.parallelEdges(directedGraph, E11)).containsExactly(E11_A);
    assertThat(Graphs.parallelEdges(directedGraph, E11_A)).containsExactly(E11);
  }

  @Test
  public void parallelEdges_undirected() {
    Graph<Integer, String> undirectedGraph =
        GraphBuilder.undirected().allowsParallelEdges(true).build();
    undirectedGraph.addEdge(E12, N1, N2);
    undirectedGraph.addEdge(E12_A, N1, N2);
    undirectedGraph.addEdge(E21, N2, N1);
    assertThat(Graphs.parallelEdges(undirectedGraph, E12)).containsExactly(E12_A, E21);
    assertThat(Graphs.parallelEdges(undirectedGraph, E12_A)).containsExactly(E12, E21);
    assertThat(Graphs.parallelEdges(undirectedGraph, E21)).containsExactly(E12, E12_A);
  }

  @Test
  public void parallelEdges_selfLoop_undirected() {
    Graph<Integer, String> undirectedGraph =
        GraphBuilder.undirected().allowsParallelEdges(true).build();
    undirectedGraph.addEdge(E11, N1, N1);
    undirectedGraph.addEdge(E11_A, N1, N1);
    assertThat(Graphs.parallelEdges(undirectedGraph, E11)).containsExactly(E11_A);
    assertThat(Graphs.parallelEdges(undirectedGraph, E11_A)).containsExactly(E11);
  }

  @Test
  public void parallelEdges_unmodifiableView() {
    Graph<Integer, String> undirectedGraph =
        GraphBuilder.undirected().allowsParallelEdges(true).build();
    undirectedGraph.addEdge(E12, N1, N2);
    undirectedGraph.addEdge(E12_A, N1, N2);
    Set<String> parallelEdges = Graphs.parallelEdges(undirectedGraph, E12);
    assertThat(parallelEdges).containsExactly(E12_A);
    undirectedGraph.addEdge(E12_B, N1, N2);
    assertThat(parallelEdges).containsExactly(E12_A, E12_B);
    try {
      parallelEdges.add(E21);
      fail("Set returned by parallelEdges() should be unmodifiable");
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Test
  public void createDirected() {
    Graph<Integer, String> directedGraph = GraphBuilder.directed().build();
    assertThat(directedGraph.nodes()).isEmpty();
    assertThat(directedGraph.edges()).isEmpty();
    assertThat(directedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();
    // By default, parallel edges are not allowed.
    try {
      directedGraph.addEdge(E12_A, N1, N2);
      fail(ERROR_ADDED_PARALLEL_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
    // By default, self-loop edges are allowed.
    assertThat(directedGraph.addEdge(E11, N1, N1)).isTrue();
  }

  @Test
  public void createUndirected() {
    Graph<Integer, String> undirectedGraph = GraphBuilder.undirected().build();
    assertThat(undirectedGraph.nodes()).isEmpty();
    assertThat(undirectedGraph.edges()).isEmpty();
    assertThat(undirectedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).isEqualTo(ImmutableSet.of(E12));
    // By default, parallel edges are not allowed.
    try {
      undirectedGraph.addEdge(E12_A, N1, N2);
      fail(ERROR_ADDED_PARALLEL_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
    try {
      undirectedGraph.addEdge(E21, N2, N1);
      fail(ERROR_ADDED_PARALLEL_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
    // By default, self-loop edges are allowed.
    assertThat(undirectedGraph.addEdge(E11, N1, N1)).isTrue();
  }

  @Test
  public void createDirected_multigraph() {
    Graph<Integer, String> directedMultigraph =
        GraphBuilder.directed().allowsParallelEdges(true).build();
    assertThat(directedMultigraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(directedMultigraph.addEdge(E12_A, N1, N2)).isTrue();
    assertThat(directedMultigraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12, E12_A));
    assertThat(directedMultigraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_multigraph() {
    Graph<Integer, String> undirectedMultigraph =
        GraphBuilder.undirected().allowsParallelEdges(true).build();
    assertThat(undirectedMultigraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(undirectedMultigraph.addEdge(E12_A, N1, N2)).isTrue();
    assertThat(undirectedMultigraph.addEdge(E21, N2, N1)).isTrue();
    assertThat(undirectedMultigraph.edgesConnecting(N1, N2))
        .isEqualTo(ImmutableSet.of(E12, E12_A, E21));
  }

  @Test
  public void createDirected_expectedNodeCount() {
    Graph<Integer, String> directedGraph = GraphBuilder.directed()
        .expectedNodeCount(NODE_COUNT)
        .build();
    assertThat(directedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_expectedNodeCount() {
    Graph<Integer, String> undirectedGraph = GraphBuilder.undirected()
        .expectedNodeCount(NODE_COUNT)
        .build();
    assertThat(undirectedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).isEqualTo(ImmutableSet.of(E12));
  }

  @Test
  public void builder_expectedNodeCount_negative() {
    try {
      GraphBuilder.directed().expectedNodeCount(-1);
      fail(ERROR_NEGATIVE_NODE_COUNT);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_NEGATIVE_NODE_COUNT);
    }
  }

  @Test
  public void createDirected_expectedEdgeCount() {
    Graph<Integer, String> directedGraph = GraphBuilder.directed()
        .expectedEdgeCount(EDGE_COUNT)
        .build();
    assertThat(directedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_expectedEdgeCount() {
    Graph<Integer, String> undirectedGraph = GraphBuilder.undirected()
        .expectedEdgeCount(EDGE_COUNT)
        .build();
    assertThat(undirectedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).isEqualTo(ImmutableSet.of(E12));
  }

  @Test
  public void builder_expectedEdgeCount_negative() {
    try {
      GraphBuilder.directed().expectedEdgeCount(-1);
      fail(ERROR_NEGATIVE_EDGE_COUNT);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_NEGATIVE_EDGE_COUNT);
    }
  }

  @Test
  public void createDirected_noSelfLoops() {
    Graph<Integer, String> directedGraph = GraphBuilder.directed().allowsSelfLoops(false).build();
    try {
      directedGraph.addEdge(E11, N1, N1);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_SELF_LOOP);
    }
  }

  @Test
  public void createUndirected_noSelfLoops() {
    Graph<Integer, String> undirectedGraph =
        GraphBuilder.undirected().allowsSelfLoops(false).build();
    try {
      undirectedGraph.addEdge(E11, N1, N1);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_SELF_LOOP);
    }
  }

  @Test
  public void addEdge_nullGraph() {
    try {
      addEdge(null, E11, ImmutableSet.of(N1));
      fail("Should have rejected null graph");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void addEdge_nullNodes() {
    try {
      addEdge(GraphBuilder.directed().build(), E11, null);
      fail("Should have rejected null nodes");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void addEdge_tooManyNodes() {
    try {
      addEdge(GraphBuilder.directed().<Integer, String>build(), E11, ImmutableSet.of(N1, N2, N3));
      fail("Should have rejected adding an edge to a Graph with > 2 nodes");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void addEdge_notEnoughNodes() {
    try {
      addEdge(GraphBuilder.directed().build(), E11, ImmutableSet.of());
      fail("Should have rejected adding an edge to a Graph with < 1 nodes");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void addEdge_selfLoop() {
    Graph<Integer, String> directedGraph = GraphBuilder.directed().build();
    assertThat(addEdge(directedGraph, E11, ImmutableSet.of(N1))).isTrue();
    assertThat(directedGraph.edges()).containsExactly(E11);
    assertThat(directedGraph.nodes()).containsExactly(N1);
    assertThat(directedGraph.incidentNodes(E11)).containsExactly(N1);
  }

  @Test
  public void addEdge_basic() {
    Graph<Integer, String> directedGraph = GraphBuilder.directed().build();
    assertThat(addEdge(directedGraph, E12, ImmutableSet.of(N1, N2))).isTrue();
    assertThat(directedGraph.edges()).containsExactly(E12);
    assertThat(directedGraph.nodes()).containsExactly(N1, N2).inOrder();
    assertThat(directedGraph.incidentNodes(E12)).containsExactly(N1, N2).inOrder();
  }

  @Test
  public void copyOf_nullArgument() {
    try {
      copyOf(null);
      fail("Should have rejected a null graph");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void copyOf_directedGraph() {
    Graph<Integer, String> directedGraph = buildDirectedTestGraph();

    Graph<Integer, String> copy = copyOf(directedGraph);
    assertThat(copy).isEqualTo(directedGraph);
  }

  @Test
  public void copyOf_undirectedGraph() {
    Graph<Integer, String> undirectedGraph = buildUndirectedTestGraph();

    Graph<Integer, String> copy = copyOf(undirectedGraph);
    assertThat(copy).isEqualTo(undirectedGraph);
  }

  // TODO: add a test for copyOf_hypergraph() once we have a Hypergraph implementation

  @Test
  public void copyOf_filtered_undirected() {
    Graph<Integer, String> undirectedGraph = buildUndirectedTestGraph();
    undirectedGraph.addNode(N3);
    Predicate<Integer> nodePredicate = connectedNodePredicate(undirectedGraph);
    Predicate<String> edgePredicate = Predicates.not(selfLoopPredicate(undirectedGraph));

    Graph<Integer, String> filteredCopy =
        copyOf(undirectedGraph, nodePredicate, edgePredicate);

    Graph<Integer, String> expectedGraph =
        GraphBuilder.undirected().allowsParallelEdges(true).build();
    expectedGraph.addEdge(E12, N1, N2);
    expectedGraph.addEdge(E12_A, N1, N2);
    expectedGraph.addEdge(E21, N2, N1);

    assertThat(filteredCopy).isEqualTo(expectedGraph);
  }

  @Test
  public void copyOf_filtered_directed() {
    Graph<Integer, String> directedGraph = buildDirectedTestGraph();
    directedGraph.addNode(N3);
    Predicate<Integer> nodePredicate = connectedNodePredicate(directedGraph);
    Predicate<String> edgePredicate = Predicates.not(selfLoopPredicate(directedGraph));

    Graph<Integer, String> filteredCopy =
        copyOf(directedGraph, nodePredicate, edgePredicate);

    Graph<Integer, String> expectedGraph =
        GraphBuilder.directed().allowsParallelEdges(true).build();
    expectedGraph.addEdge(E12, N1, N2);
    expectedGraph.addEdge(E12_A, N1, N2);
    expectedGraph.addEdge(E21, N2, N1);

    assertThat(filteredCopy).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_directed() {
    Graph<Integer, String> directedGraph = buildDirectedTestGraph();
    directedGraph.addNode(N3);

    Graph<Integer, String> actualGraph = GraphBuilder.directed().build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4);
    actualGraph.addNode(N2);

    mergeNodesFrom(directedGraph, actualGraph);

    Graph<Integer, String> expectedGraph = GraphBuilder.directed().build();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    expectedGraph.addNode(N3);
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_filtered_directed() {
    Graph<Integer, String> directedGraph = buildDirectedTestGraph();
    directedGraph.addNode(N3);
    Predicate<Integer> nodePredicate = connectedNodePredicate(directedGraph);

    Graph<Integer, String> actualGraph = GraphBuilder.directed().build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4); // ensure that we don't remove nodes that don't pass the predicate
    actualGraph.addNode(N2); // ensure that a pre-existing node is not affected by the merging

    mergeNodesFrom(directedGraph, actualGraph, nodePredicate);

    Graph<Integer, String> expectedGraph = GraphBuilder.directed().build();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    // N3 is not expected because it's not connected
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeEdgesFrom_directed() {
    Graph<Integer, String> directedGraph = buildDirectedTestGraph();

    Graph<Integer, String> actualGraph = GraphBuilder.directed().allowsParallelEdges(true).build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addEdge(E11, N1, N1);
    actualGraph.addEdge(E22, N2, N2);

    mergeEdgesFrom(directedGraph, actualGraph);

    Graph<Integer, String> expectedGraph = buildDirectedTestGraph();
    expectedGraph.addEdge(E22, N2, N2);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeEdgesFrom_filtered_directed() {
    Graph<Integer, String> directedGraph = buildDirectedTestGraph();
    directedGraph.addNode(N3);
    Predicate<String> edgePredicate = Predicates.not(selfLoopPredicate(directedGraph));

    Graph<Integer, String> actualGraph = GraphBuilder.directed().allowsParallelEdges(true).build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addEdge(E11, N1, N1); // existing (redundant) self-loops should be retained
    actualGraph.addEdge(E22, N2, N2); // existing (novel) self-loops should be retained
    actualGraph.addEdge(E44, N4, N4); // existing self-loops for unrelated nodes should be retained
    actualGraph.addEdge(E12, N1, N2); // existing edges should be unaffected

    mergeEdgesFrom(directedGraph, actualGraph, edgePredicate);

    Graph<Integer, String> expectedGraph =
        GraphBuilder.directed().allowsParallelEdges(true).build();
    // all pre-existing edges should still be there...
    expectedGraph.addEdge(E11, N1, N1);
    expectedGraph.addEdge(E22, N2, N2);
    expectedGraph.addEdge(E44, N4, N4);
    expectedGraph.addEdge(E12, N1, N2);
    // ...as well as the new ones from the source graph
    expectedGraph.addEdge(E12_A, N1, N2);
    expectedGraph.addEdge(E21, N2, N1);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_undirected() {
    Graph<Integer, String> undirectedGraph = buildUndirectedTestGraph();
    undirectedGraph.addNode(N3);

    Graph<Integer, String> actualGraph = GraphBuilder.undirected().build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4);
    actualGraph.addNode(N2);

    mergeNodesFrom(undirectedGraph, actualGraph);

    Graph<Integer, String> expectedGraph = GraphBuilder.undirected().build();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    expectedGraph.addNode(N3);
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_filtered_undirected() {
    Graph<Integer, String> undirectedGraph = buildUndirectedTestGraph();
    undirectedGraph.addNode(N3);
    Predicate<Integer> nodePredicate = connectedNodePredicate(undirectedGraph);

    Graph<Integer, String> actualGraph = GraphBuilder.undirected().build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4); // ensure that we don't remove nodes that don't pass the predicate
    actualGraph.addNode(N2); // ensure that a pre-existing node is not affected by the merging

    mergeNodesFrom(undirectedGraph, actualGraph, nodePredicate);

    Graph<Integer, String> expectedGraph = GraphBuilder.undirected().build();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    // N3 is not expected because it's not connected
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeEdgesFrom_undirected() {
    Graph<Integer, String> undirectedGraph = buildUndirectedTestGraph();

    Graph<Integer, String> actualGraph =
        GraphBuilder.undirected().allowsParallelEdges(true).build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addEdge(E11, N1, N1);
    actualGraph.addEdge(E22, N2, N2);

    mergeEdgesFrom(undirectedGraph, actualGraph);

    Graph<Integer, String> expectedGraph = buildUndirectedTestGraph();
    expectedGraph.addEdge(E22, N2, N2);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeEdgesFrom_filtered_undirected() {
    Graph<Integer, String> undirectedGraph = buildUndirectedTestGraph();
    undirectedGraph.addNode(N3);
    Predicate<String> edgePredicate = Predicates.not(selfLoopPredicate(undirectedGraph));

    Graph<Integer, String> actualGraph =
        GraphBuilder.undirected().allowsParallelEdges(true).build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addEdge(E11, N1, N1); // existing (redundant) self-loops should be retained
    actualGraph.addEdge(E22, N2, N2); // existing (novel) self-loops should be retained
    actualGraph.addEdge(E44, N4, N4); // existing self-loops for unrelated nodes should be retained
    actualGraph.addEdge(E12, N1, N2); // existing edges should be unaffected

    mergeEdgesFrom(undirectedGraph, actualGraph, edgePredicate);

    Graph<Integer, String> expectedGraph =
        GraphBuilder.undirected().allowsParallelEdges(true).build();
    // all pre-existing edges should still be there...
    expectedGraph.addEdge(E11, N1, N1);
    expectedGraph.addEdge(E22, N2, N2);
    expectedGraph.addEdge(E44, N4, N4);
    expectedGraph.addEdge(E12, N1, N2);
    // ...as well as the new ones from the source graph
    expectedGraph.addEdge(E12_A, N1, N2);
    expectedGraph.addEdge(E21, N2, N1);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  private static Graph<Integer, String> buildDirectedTestGraph() {
    Graph<Integer, String> directedGraph =
        GraphBuilder.directed().allowsParallelEdges(true).build();
    directedGraph.addEdge(E11, N1, N1);
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E11_A, N1, N1);
    directedGraph.addEdge(E12_A, N1, N2);
    directedGraph.addEdge(E21, N2, N1);

    return directedGraph;
  }

  private static Graph<Integer, String> buildUndirectedTestGraph() {
    Graph<Integer, String> undirectedGraph =
        GraphBuilder.undirected().allowsParallelEdges(true).build();
    undirectedGraph.addEdge(E11, N1, N1);
    undirectedGraph.addEdge(E12, N1, N2);
    undirectedGraph.addEdge(E11_A, N1, N1);
    undirectedGraph.addEdge(E12_A, N1, N2);
    undirectedGraph.addEdge(E21, N2, N1);

    return undirectedGraph;
  }

  private static <N> Predicate<N> connectedNodePredicate(final Graph<N, ?> graph) {
    checkNotNull(graph, "graph");
    return new Predicate<N>() {
      @Override
      public boolean apply(N node) {
        return graph.degree(node) > 0;
      }
    };
  }
}
