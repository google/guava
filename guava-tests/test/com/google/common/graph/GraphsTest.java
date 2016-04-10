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
  // TODO(user): Consider adding both error messages from here and {@link AbstractNetworkTest}
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
    List<MutableNetwork<Integer, String>> testNetworks = ImmutableList.of(
        NetworkBuilder.directed().<Integer, String>build(),
        NetworkBuilder.undirected().<Integer, String>build());
    for (MutableNetwork<Integer, String> graph : testNetworks) {
      graph.addEdge(E12, N1, N2);
      assertThat(oppositeNode(graph, E12, N1)).isEqualTo(N2);
      assertThat(oppositeNode(graph, E12, N2)).isEqualTo(N1);
    }
  }

  @Test
  public void oppositeNode_parallelEdge() {
    List<MutableNetwork<Integer, String>> testNetworks = ImmutableList.of(
        NetworkBuilder.directed().allowsParallelEdges(true).<Integer, String>build(),
        NetworkBuilder.undirected().allowsParallelEdges(true).<Integer, String>build());
    for (MutableNetwork<Integer, String> graph : testNetworks) {
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
    List<MutableNetwork<Integer, String>> testNetworks = ImmutableList.of(
        NetworkBuilder.directed().<Integer, String>build(),
        NetworkBuilder.undirected().<Integer, String>build());
    for (MutableNetwork<Integer, String> graph : testNetworks) {
      graph.addEdge(E11, N1, N1);
      assertThat(oppositeNode(graph, E11, N1)).isEqualTo(N1);
    }
  }

  @Test
  public void oppositeNode_nodeNotIncident() {
    List<MutableNetwork<Integer, String>> testNetworks = ImmutableList.of(
        NetworkBuilder.directed().<Integer, String>build(),
        NetworkBuilder.undirected().<Integer, String>build());
    for (MutableNetwork<Integer, String> graph : testNetworks) {
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
    MutableNetwork<Integer, String> directedGraph =
        NetworkBuilder.directed().allowsParallelEdges(true).build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E12_A, N1, N2);
    directedGraph.addEdge(E21, N2, N1);
    assertThat(Graphs.parallelEdges(directedGraph, E12)).containsExactly(E12_A);
    assertThat(Graphs.parallelEdges(directedGraph, E12_A)).containsExactly(E12);
    assertThat(Graphs.parallelEdges(directedGraph, E21)).isEmpty();
  }

  @Test
  public void parallelEdges_selfLoop_directed() {
    MutableNetwork<Integer, String> directedGraph =
        NetworkBuilder.directed().allowsParallelEdges(true).build();
    directedGraph.addEdge(E11, N1, N1);
    directedGraph.addEdge(E11_A, N1, N1);
    assertThat(Graphs.parallelEdges(directedGraph, E11)).containsExactly(E11_A);
    assertThat(Graphs.parallelEdges(directedGraph, E11_A)).containsExactly(E11);
  }

  @Test
  public void parallelEdges_undirected() {
    MutableNetwork<Integer, String> undirectedGraph =
        NetworkBuilder.undirected().allowsParallelEdges(true).build();
    undirectedGraph.addEdge(E12, N1, N2);
    undirectedGraph.addEdge(E12_A, N1, N2);
    undirectedGraph.addEdge(E21, N2, N1);
    assertThat(Graphs.parallelEdges(undirectedGraph, E12)).containsExactly(E12_A, E21);
    assertThat(Graphs.parallelEdges(undirectedGraph, E12_A)).containsExactly(E12, E21);
    assertThat(Graphs.parallelEdges(undirectedGraph, E21)).containsExactly(E12, E12_A);
  }

  @Test
  public void parallelEdges_selfLoop_undirected() {
    MutableNetwork<Integer, String> undirectedGraph =
        NetworkBuilder.undirected().allowsParallelEdges(true).build();
    undirectedGraph.addEdge(E11, N1, N1);
    undirectedGraph.addEdge(E11_A, N1, N1);
    assertThat(Graphs.parallelEdges(undirectedGraph, E11)).containsExactly(E11_A);
    assertThat(Graphs.parallelEdges(undirectedGraph, E11_A)).containsExactly(E11);
  }

  @Test
  public void parallelEdges_unmodifiableView() {
    MutableNetwork<Integer, String> undirectedGraph =
        NetworkBuilder.undirected().allowsParallelEdges(true).build();
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
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
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
    MutableNetwork<Integer, String> undirectedGraph = NetworkBuilder.undirected().build();
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
    MutableNetwork<Integer, String> directedMultigraph =
        NetworkBuilder.directed().allowsParallelEdges(true).build();
    assertThat(directedMultigraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(directedMultigraph.addEdge(E12_A, N1, N2)).isTrue();
    assertThat(directedMultigraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12, E12_A));
    assertThat(directedMultigraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_multigraph() {
    MutableNetwork<Integer, String> undirectedMultigraph =
        NetworkBuilder.undirected().allowsParallelEdges(true).build();
    assertThat(undirectedMultigraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(undirectedMultigraph.addEdge(E12_A, N1, N2)).isTrue();
    assertThat(undirectedMultigraph.addEdge(E21, N2, N1)).isTrue();
    assertThat(undirectedMultigraph.edgesConnecting(N1, N2))
        .isEqualTo(ImmutableSet.of(E12, E12_A, E21));
  }

  @Test
  public void createDirected_expectedNodeCount() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed()
        .expectedNodeCount(NODE_COUNT)
        .build();
    assertThat(directedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_expectedNodeCount() {
    MutableNetwork<Integer, String> undirectedGraph = NetworkBuilder.undirected()
        .expectedNodeCount(NODE_COUNT)
        .build();
    assertThat(undirectedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).isEqualTo(ImmutableSet.of(E12));
  }

  @Test
  public void builder_expectedNodeCount_negative() {
    try {
      NetworkBuilder.directed().expectedNodeCount(-1);
      fail(ERROR_NEGATIVE_NODE_COUNT);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_NEGATIVE_NODE_COUNT);
    }
  }

  @Test
  public void createDirected_expectedEdgeCount() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed()
        .expectedEdgeCount(EDGE_COUNT)
        .build();
    assertThat(directedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_expectedEdgeCount() {
    MutableNetwork<Integer, String> undirectedGraph = NetworkBuilder.undirected()
        .expectedEdgeCount(EDGE_COUNT)
        .build();
    assertThat(undirectedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).isEqualTo(ImmutableSet.of(E12));
  }

  @Test
  public void builder_expectedEdgeCount_negative() {
    try {
      NetworkBuilder.directed().expectedEdgeCount(-1);
      fail(ERROR_NEGATIVE_EDGE_COUNT);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_NEGATIVE_EDGE_COUNT);
    }
  }

  @Test
  public void createDirected_noSelfLoops() {
    MutableNetwork<Integer, String> directedGraph
        = NetworkBuilder.directed().allowsSelfLoops(false).build();
    try {
      directedGraph.addEdge(E11, N1, N1);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_SELF_LOOP);
    }
  }

  @Test
  public void createUndirected_noSelfLoops() {
    MutableNetwork<Integer, String> undirectedGraph =
        NetworkBuilder.undirected().allowsSelfLoops(false).build();
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
      addEdge(NetworkBuilder.directed().build(), E11, null);
      fail("Should have rejected null nodes");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void addEdge_tooManyNodes() {
    try {
      addEdge(NetworkBuilder.directed().<Integer, String>build(), E11, ImmutableSet.of(N1, N2, N3));
      fail("Should have rejected adding an edge to a Graph with > 2 nodes");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void addEdge_notEnoughNodes() {
    try {
      addEdge(NetworkBuilder.directed().build(), E11, ImmutableSet.of());
      fail("Should have rejected adding an edge to a Graph with < 1 nodes");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void addEdge_selfLoop() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    assertThat(addEdge(directedGraph, E11, ImmutableSet.of(N1))).isTrue();
    assertThat(directedGraph.edges()).containsExactly(E11);
    assertThat(directedGraph.nodes()).containsExactly(N1);
    assertThat(directedGraph.incidentNodes(E11)).containsExactly(N1);
  }

  @Test
  public void addEdge_basic() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
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
    Graph<Integer> directedGraph = buildDirectedTestGraph();

    Graph<Integer> copy = copyOf(directedGraph);
    assertThat(copy).isEqualTo(directedGraph);
  }

  @Test
  public void copyOf_undirectedGraph() {
    Graph<Integer> undirectedGraph = buildUndirectedTestGraph();

    Graph<Integer> copy = copyOf(undirectedGraph);
    assertThat(copy).isEqualTo(undirectedGraph);
  }

  @Test
  public void copyOf_filtered_undirectedGraph() {
    MutableGraph<Integer> undirectedGraph = buildUndirectedTestGraph();
    undirectedGraph.addEdge(N3, N1);
    Predicate<Integer> nodePredicate = Predicates.in(ImmutableSet.of(N1, N2));

    Graph<Integer> filteredCopy = copyOf(undirectedGraph, nodePredicate);

    MutableGraph<Integer> expectedGraph = GraphBuilder.undirected().build();
    expectedGraph.addEdge(N1, N1);
    expectedGraph.addEdge(N1, N2);
    expectedGraph.addEdge(N2, N1);

    assertThat(filteredCopy).isEqualTo(expectedGraph);
  }

  @Test
  public void copyOf_filtered_directedGraph() {
    MutableGraph<Integer> directedGraph = buildDirectedTestGraph();
    directedGraph.addEdge(N3, N1);
    Predicate<Integer> nodePredicate = Predicates.in(ImmutableSet.of(N1, N2));

    Graph<Integer> filteredCopy = copyOf(directedGraph, nodePredicate);

    MutableGraph<Integer> expectedGraph = GraphBuilder.directed().build();
    expectedGraph.addEdge(N1, N1);
    expectedGraph.addEdge(N1, N2);
    expectedGraph.addEdge(N2, N1);

    assertThat(filteredCopy).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_directedGraph() {
    MutableGraph<Integer> directedGraph = buildDirectedTestGraph();
    directedGraph.addNode(N3);

    MutableGraph<Integer> actualGraph = GraphBuilder.directed().build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4);
    actualGraph.addNode(N2);

    mergeNodesFrom(directedGraph, actualGraph);

    MutableGraph<Integer> expectedGraph = GraphBuilder.directed().build();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    expectedGraph.addNode(N3);
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_filtered_directedGraph() {
    MutableGraph<Integer> directedGraph = buildDirectedTestGraph();
    directedGraph.addEdge(N3, N1);
    Predicate<Integer> nodePredicate = Predicates.in(ImmutableSet.of(N1, N2));

    MutableGraph<Integer> actualGraph = GraphBuilder.directed().build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4); // ensure that we don't remove nodes that don't pass the predicate
    actualGraph.addNode(N2); // ensure that a pre-existing node is not affected by the merging

    mergeNodesFrom(directedGraph, actualGraph, nodePredicate);

    MutableGraph<Integer> expectedGraph = GraphBuilder.directed().build();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    // N3 is not expected because it's not in {N1, N2}
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_undirectedGraph() {
    MutableGraph<Integer> undirectedGraph = buildUndirectedTestGraph();
    undirectedGraph.addNode(N3);

    MutableGraph<Integer> actualGraph = GraphBuilder.undirected().build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4);
    actualGraph.addNode(N2);

    mergeNodesFrom(undirectedGraph, actualGraph);

    MutableGraph<Integer> expectedGraph = GraphBuilder.undirected().build();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    expectedGraph.addNode(N3);
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_filtered_undirectedGraph() {
    MutableGraph<Integer> undirectedGraph = buildUndirectedTestGraph();
    undirectedGraph.addEdge(N3, N1);
    Predicate<Integer> nodePredicate = Predicates.in(ImmutableSet.of(N1, N2));

    MutableGraph<Integer> actualGraph = GraphBuilder.undirected().build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4); // ensure that we don't remove nodes that don't pass the predicate
    actualGraph.addNode(N2); // ensure that a pre-existing node is not affected by the merging

    mergeNodesFrom(undirectedGraph, actualGraph, nodePredicate);

    MutableGraph<Integer> expectedGraph = GraphBuilder.undirected().build();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    // N3 is not expected because it's not in {N1, N2}
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void copyOf_directedNetwork() {
    Network<Integer, String> directedGraph = buildDirectedTestNetwork();

    Network<Integer, String> copy = copyOf(directedGraph);
    assertThat(copy).isEqualTo(directedGraph);
  }

  @Test
  public void copyOf_undirectedNetwork() {
    Network<Integer, String> undirectedGraph = buildUndirectedTestNetwork();

    Network<Integer, String> copy = copyOf(undirectedGraph);
    assertThat(copy).isEqualTo(undirectedGraph);
  }

  // TODO: add a test for copyOf_hypergraph() once we have a Hypergraph implementation

  @Test
  public void copyOf_filtered_undirectedNetwork() {
    MutableNetwork<Integer, String> undirectedGraph = buildUndirectedTestNetwork();
    undirectedGraph.addEdge(E13, N1, N3);
    Predicate<Integer> nodePredicate = Predicates.in(ImmutableSet.of(N1, N2));
    Predicate<String> edgePredicate = Predicates.not(selfLoopPredicate(undirectedGraph));

    Network<Integer, String> filteredCopy =
        copyOf(undirectedGraph, nodePredicate, edgePredicate);

    MutableNetwork<Integer, String> expectedGraph =
        NetworkBuilder.undirected().allowsParallelEdges(true).build();
    expectedGraph.addEdge(E12, N1, N2);
    expectedGraph.addEdge(E12_A, N1, N2);
    expectedGraph.addEdge(E21, N2, N1);

    assertThat(filteredCopy).isEqualTo(expectedGraph);
  }

  @Test
  public void copyOf_filtered_directedNetwork() {
    MutableNetwork<Integer, String> directedGraph = buildDirectedTestNetwork();
    directedGraph.addEdge(E13, N1, N3);
    Predicate<Integer> nodePredicate = Predicates.in(ImmutableSet.of(N1, N2));
    Predicate<String> edgePredicate = Predicates.not(selfLoopPredicate(directedGraph));

    Network<Integer, String> filteredCopy =
        copyOf(directedGraph, nodePredicate, edgePredicate);

    MutableNetwork<Integer, String> expectedGraph =
        NetworkBuilder.directed().allowsParallelEdges(true).build();
    expectedGraph.addEdge(E12, N1, N2);
    expectedGraph.addEdge(E12_A, N1, N2);
    expectedGraph.addEdge(E21, N2, N1);

    assertThat(filteredCopy).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_directedNetwork() {
    MutableNetwork<Integer, String> directedGraph = buildDirectedTestNetwork();
    directedGraph.addNode(N3);

    MutableNetwork<Integer, String> actualGraph = NetworkBuilder.directed().build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4);
    actualGraph.addNode(N2);

    mergeNodesFrom(directedGraph, actualGraph);

    MutableNetwork<Integer, String> expectedGraph = NetworkBuilder.directed().build();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    expectedGraph.addNode(N3);
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_filtered_directedNetwork() {
    MutableNetwork<Integer, String> directedGraph = buildDirectedTestNetwork();
    directedGraph.addEdge(E13, N1, N3);
    Predicate<Integer> nodePredicate = Predicates.in(ImmutableSet.of(N1, N2));

    MutableNetwork<Integer, String> actualGraph = NetworkBuilder.directed().build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4); // ensure that we don't remove nodes that don't pass the predicate
    actualGraph.addNode(N2); // ensure that a pre-existing node is not affected by the merging

    mergeNodesFrom(directedGraph, actualGraph, nodePredicate);

    MutableNetwork<Integer, String> expectedGraph = NetworkBuilder.directed().build();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    // N3 is not expected because it's not in {N1, N2}
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeEdgesFrom_directedNetwork() {
    Network<Integer, String> directedGraph = buildDirectedTestNetwork();

    MutableNetwork<Integer, String> actualGraph
        = NetworkBuilder.directed().allowsParallelEdges(true).build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addEdge(E11, N1, N1);
    actualGraph.addEdge(E22, N2, N2);

    mergeEdgesFrom(directedGraph, actualGraph);

    MutableNetwork<Integer, String> expectedGraph = buildDirectedTestNetwork();
    expectedGraph.addEdge(E22, N2, N2);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeEdgesFrom_filtered_directedNetwork() {
    MutableNetwork<Integer, String> directedGraph = buildDirectedTestNetwork();
    directedGraph.addNode(N3);
    Predicate<String> edgePredicate = Predicates.not(selfLoopPredicate(directedGraph));

    MutableNetwork<Integer, String> actualGraph
        = NetworkBuilder.directed().allowsParallelEdges(true).build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addEdge(E11, N1, N1); // existing (redundant) self-loops should be retained
    actualGraph.addEdge(E22, N2, N2); // existing (novel) self-loops should be retained
    actualGraph.addEdge(E44, N4, N4); // existing self-loops for unrelated nodes should be retained
    actualGraph.addEdge(E12, N1, N2); // existing edges should be unaffected

    mergeEdgesFrom(directedGraph, actualGraph, edgePredicate);

    MutableNetwork<Integer, String> expectedGraph =
        NetworkBuilder.directed().allowsParallelEdges(true).build();
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
  public void mergeNodesFrom_undirectedNetwork() {
    MutableNetwork<Integer, String> undirectedGraph = buildUndirectedTestNetwork();
    undirectedGraph.addNode(N3);

    MutableNetwork<Integer, String> actualGraph = NetworkBuilder.undirected().build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4);
    actualGraph.addNode(N2);

    mergeNodesFrom(undirectedGraph, actualGraph);

    MutableNetwork<Integer, String> expectedGraph = NetworkBuilder.undirected().build();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    expectedGraph.addNode(N3);
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_filtered_undirectedNetwork() {
    MutableNetwork<Integer, String> undirectedGraph = buildUndirectedTestNetwork();
    undirectedGraph.addEdge(E13, N1, N3);
    Predicate<Integer> nodePredicate = Predicates.in(ImmutableSet.of(N1, N2));

    MutableNetwork<Integer, String> actualGraph = NetworkBuilder.undirected().build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4); // ensure that we don't remove nodes that don't pass the predicate
    actualGraph.addNode(N2); // ensure that a pre-existing node is not affected by the merging

    mergeNodesFrom(undirectedGraph, actualGraph, nodePredicate);

    MutableNetwork<Integer, String> expectedGraph = NetworkBuilder.undirected().build();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    // N3 is not expected because it's not in {N1, N2}
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeEdgesFrom_undirectedNetwork() {
    Network<Integer, String> undirectedGraph = buildUndirectedTestNetwork();

    MutableNetwork<Integer, String> actualGraph =
        NetworkBuilder.undirected().allowsParallelEdges(true).build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addEdge(E11, N1, N1);
    actualGraph.addEdge(E22, N2, N2);

    mergeEdgesFrom(undirectedGraph, actualGraph);

    MutableNetwork<Integer, String> expectedGraph = buildUndirectedTestNetwork();
    expectedGraph.addEdge(E22, N2, N2);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeEdgesFrom_filtered_undirectedNetwork() {
    MutableNetwork<Integer, String> undirectedGraph = buildUndirectedTestNetwork();
    undirectedGraph.addNode(N3);
    Predicate<String> edgePredicate = Predicates.not(selfLoopPredicate(undirectedGraph));

    MutableNetwork<Integer, String> actualGraph =
        NetworkBuilder.undirected().allowsParallelEdges(true).build();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addEdge(E11, N1, N1); // existing (redundant) self-loops should be retained
    actualGraph.addEdge(E22, N2, N2); // existing (novel) self-loops should be retained
    actualGraph.addEdge(E44, N4, N4); // existing self-loops for unrelated nodes should be retained
    actualGraph.addEdge(E12, N1, N2); // existing edges should be unaffected

    mergeEdgesFrom(undirectedGraph, actualGraph, edgePredicate);

    MutableNetwork<Integer, String> expectedGraph =
        NetworkBuilder.undirected().allowsParallelEdges(true).build();
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

  private static MutableGraph<Integer> buildDirectedTestGraph() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    directedGraph.addEdge(N1, N1);
    directedGraph.addEdge(N1, N2);
    // TODO(b/28087289): add parallel edges to test
    directedGraph.addEdge(N2, N1);

    return directedGraph;
  }

  private static MutableGraph<Integer> buildUndirectedTestGraph() {
    MutableGraph<Integer> undirectedGraph = GraphBuilder.undirected().build();
    undirectedGraph.addEdge(N1, N1);
    undirectedGraph.addEdge(N1, N2);
    // TODO(b/28087289): add parallel edges to test
    undirectedGraph.addEdge(N2, N1);

    return undirectedGraph;
  }

  private static MutableNetwork<Integer, String> buildDirectedTestNetwork() {
    MutableNetwork<Integer, String> directedGraph =
        NetworkBuilder.directed().allowsParallelEdges(true).build();
    directedGraph.addEdge(E11, N1, N1);
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E11_A, N1, N1);
    directedGraph.addEdge(E12_A, N1, N2);
    directedGraph.addEdge(E21, N2, N1);

    return directedGraph;
  }

  private static MutableNetwork<Integer, String> buildUndirectedTestNetwork() {
    MutableNetwork<Integer, String> undirectedGraph =
        NetworkBuilder.undirected().allowsParallelEdges(true).build();
    undirectedGraph.addEdge(E11, N1, N1);
    undirectedGraph.addEdge(E12, N1, N2);
    undirectedGraph.addEdge(E11_A, N1, N1);
    undirectedGraph.addEdge(E12_A, N1, N2);
    undirectedGraph.addEdge(E21, N2, N1);

    return undirectedGraph;
  }
}
