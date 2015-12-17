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
import static com.google.common.graph.Graphs.MULTIGRAPH;
import static com.google.common.graph.Graphs.addEdge;
import static com.google.common.graph.Graphs.config;
import static com.google.common.graph.Graphs.copyOf;
import static com.google.common.graph.Graphs.mergeEdgesFrom;
import static com.google.common.graph.Graphs.mergeNodesFrom;
import static com.google.common.graph.Graphs.noSelfLoopPredicate;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
  private static final String E21 = "2-1";
  private static final String E22 = "2-2";
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
  public void createDirected() {
    DirectedGraph<Integer, String> directedGraph = Graphs.createDirected();
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
    UndirectedGraph<Integer, String> undirectedGraph = Graphs.createUndirected();
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
    DirectedGraph<Integer, String> directedMultigraph =
        Graphs.createDirected(config().multigraph());
    assertThat(directedMultigraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(directedMultigraph.addEdge(E12_A, N1, N2)).isTrue();
    assertThat(directedMultigraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12, E12_A));
    assertThat(directedMultigraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_multigraph() {
    UndirectedGraph<Integer, String> undirectedMultigraph =
        Graphs.createUndirected(Graphs.MULTIGRAPH);
    assertThat(undirectedMultigraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(undirectedMultigraph.addEdge(E12_A, N1, N2)).isTrue();
    assertThat(undirectedMultigraph.addEdge(E21, N2, N1)).isTrue();
    assertThat(undirectedMultigraph.edgesConnecting(N1, N2))
        .isEqualTo(ImmutableSet.of(E12, E12_A, E21));
  }

  @Test
  public void createDirected_expectedNodeCount() {
    DirectedGraph<Integer, String> directedGraph =
        Graphs.createDirected(config().expectedNodeCount(NODE_COUNT));
    assertThat(directedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_expectedNodeCount() {
    UndirectedGraph<Integer, String> undirectedGraph =
        Graphs.createUndirected(config().expectedNodeCount(NODE_COUNT));
    assertThat(undirectedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).isEqualTo(ImmutableSet.of(E12));
  }

  @Test
  public void config_expectedNodeCount_negative() {
    try {
      GraphConfig unused = config().expectedNodeCount(-1);
      fail(ERROR_NEGATIVE_NODE_COUNT);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_NEGATIVE_NODE_COUNT);
    }
  }

  @Test
  public void config_expectedNodeCount_overridden() {
    GraphConfig oldConfig = config().expectedNodeCount(NODE_COUNT);
    assertThat(oldConfig.getExpectedNodeCount().get()).isEqualTo(NODE_COUNT);
    GraphConfig newConfig = oldConfig.expectedNodeCount(NODE_COUNT + 1);
    assertThat(oldConfig.getExpectedNodeCount().get()).isEqualTo(NODE_COUNT);
    assertThat(newConfig.getExpectedNodeCount().get()).isEqualTo(NODE_COUNT + 1);
  }

  @Test
  public void createDirected_expectedEdgeCount() {
    DirectedGraph<Integer, String> directedGraph =
        Graphs.createDirected(config().expectedEdgeCount(EDGE_COUNT));
    assertThat(directedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_expectedEdgeCount() {
    UndirectedGraph<Integer, String> undirectedGraph =
        Graphs.createUndirected(config().expectedEdgeCount(EDGE_COUNT));
    assertThat(undirectedGraph.addEdge(E12, N1, N2)).isTrue();
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).isEqualTo(ImmutableSet.of(E12));
  }

  @Test
  public void config_expectedEdgeCount_negative() {
    try {
      GraphConfig unused = config().expectedEdgeCount(-1);
      fail(ERROR_NEGATIVE_EDGE_COUNT);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_NEGATIVE_EDGE_COUNT);
    }
  }

  @Test
  public void config_expectedEdgeCount_overridden() {
    GraphConfig oldConfig = config().expectedEdgeCount(EDGE_COUNT);
    assertThat(oldConfig.getExpectedEdgeCount().get()).isEqualTo(EDGE_COUNT);
    GraphConfig newConfig = oldConfig.expectedEdgeCount(EDGE_COUNT + 1);
    assertThat(oldConfig.getExpectedEdgeCount().get()).isEqualTo(EDGE_COUNT);
    assertThat(newConfig.getExpectedEdgeCount().get()).isEqualTo(EDGE_COUNT + 1);
  }

  @Test
  public void createDirected_noSelfLoops() {
    DirectedGraph<Integer, String> directedGraph = Graphs.createDirected(config().noSelfLoops());
    try {
      directedGraph.addEdge(E11, N1, N1);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_SELF_LOOP);
    }
  }

  @Test
  public void createUndirected_noSelfLoops() {
    UndirectedGraph<Integer, String> undirectedGraph =
        Graphs.createUndirected(config().noSelfLoops());
    try {
      undirectedGraph.addEdge(E11, N1, N1);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_SELF_LOOP);
    }
  }

  // Note that this test works precisely because config() returns a new object every time.
  @Test
  public void config_immutability() {
    GraphConfig unused = config().multigraph();
    assertThat(config().isMultigraph()).isFalse();
    unused = config().expectedNodeCount(NODE_COUNT);
    assertThat(config().getExpectedNodeCount()).isAbsent();
    unused = config().expectedEdgeCount(EDGE_COUNT);
    assertThat(config().getExpectedEdgeCount()).isAbsent();
    unused = config().noSelfLoops();
    assertThat(config().isSelfLoopsAllowed()).isTrue();
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
      addEdge(Graphs.createDirected(), E11, null);
      fail("Should have rejected null nodes");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void addEdge_tooManyNodes() {
    try {
      addEdge(Graphs.<Integer, String>createDirected(), E11, ImmutableSet.<Integer>of(N1, N2, N3));
      fail("Should have rejected adding an edge to a Graph with > 2 nodes");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void addEdge_notEnoughNodes() {
    try {
      addEdge(Graphs.createDirected(), E11, ImmutableSet.of());
      fail("Should have rejected adding an edge to a Graph with < 1 nodes");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void addEdge_selfLoop() {
    DirectedGraph<Integer, String> directedGraph = Graphs.createDirected();
    assertThat(addEdge(directedGraph, E11, ImmutableSet.of(N1))).isTrue();
    assertThat(directedGraph.edges()).containsExactly(E11);
    assertThat(directedGraph.nodes()).containsExactly(N1);
    assertThat(directedGraph.incidentNodes(E11)).containsExactly(N1);
  }

  @Test
  public void addEdge_basic() {
    DirectedGraph<Integer, String> directedGraph = Graphs.createDirected();
    assertThat(addEdge(directedGraph, E12, ImmutableSet.of(N1, N2))).isTrue();
    assertThat(directedGraph.edges()).containsExactly(E12);
    assertThat(directedGraph.nodes()).containsExactly(N1, N2).inOrder();
    assertThat(directedGraph.incidentNodes(E12)).containsExactly(N1, N2).inOrder();
  }

  @Test
  public void copyOf_nullArgument() {
    try {
      DirectedGraph<Object, Object> unused = copyOf((DirectedGraph) null);
      fail("Should have rejected a null graph");
    } catch (NullPointerException expected) {
    }
    try {
      UndirectedGraph<Object, Object> unused = copyOf((UndirectedGraph) null);
      fail("Should have rejected a null graph");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void copyOf_directedGraph() {
    DirectedGraph<Integer, String> directedGraph = buildDirectedTestGraph();

    DirectedGraph<Integer, String> copy = copyOf(directedGraph);
    assertThat(copy).isEqualTo(directedGraph);
  }

  @Test
  public void copyOf_undirectedGraph() {
    UndirectedGraph<Integer, String> undirectedGraph = buildUndirectedTestGraph();

    UndirectedGraph<Integer, String> copy = copyOf(undirectedGraph);
    assertThat(copy).isEqualTo(undirectedGraph);
  }

  // TODO: add a test for copyOf_hypergraph() once we have a Hypergraph implementation

  @Test
  public void copyOf_filtered_undirected() {
    UndirectedGraph<Integer, String> undirectedGraph = buildUndirectedTestGraph();
    undirectedGraph.addNode(N3);
    Predicate<Integer> nodePredicate = connectedNodePredicate(undirectedGraph);
    Predicate<String> edgePredicate = noSelfLoopPredicate(undirectedGraph);

    UndirectedGraph<Integer, String> filteredCopy =
        copyOf(undirectedGraph, nodePredicate, edgePredicate);

    UndirectedGraph<Integer, String> expectedGraph = Graphs.createUndirected(MULTIGRAPH);
    expectedGraph.addEdge(E12, N1, N2);
    expectedGraph.addEdge(E12_A, N1, N2);
    expectedGraph.addEdge(E21, N2, N1);

    assertThat(filteredCopy).isEqualTo(expectedGraph);
  }

  @Test
  public void copyOf_filtered_directed() {
    DirectedGraph<Integer, String> directedGraph = buildDirectedTestGraph();
    directedGraph.addNode(N3);
    Predicate<Integer> nodePredicate = connectedNodePredicate(directedGraph);
    Predicate<String> edgePredicate = noSelfLoopPredicate(directedGraph);

    DirectedGraph<Integer, String> filteredCopy =
        copyOf(directedGraph, nodePredicate, edgePredicate);

    DirectedGraph<Integer, String> expectedGraph = Graphs.createDirected(MULTIGRAPH);
    expectedGraph.addEdge(E12, N1, N2);
    expectedGraph.addEdge(E12_A, N1, N2);
    expectedGraph.addEdge(E21, N2, N1);

    assertThat(filteredCopy).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_directed() {
    DirectedGraph<Integer, String> directedGraph = buildDirectedTestGraph();
    directedGraph.addNode(N3);

    DirectedGraph<Integer, String> actualGraph = Graphs.createDirected();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4);
    actualGraph.addNode(N2);

    mergeNodesFrom(directedGraph, actualGraph);

    DirectedGraph<Integer, String> expectedGraph = Graphs.createDirected();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    expectedGraph.addNode(N3);
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_filtered_directed() {
    DirectedGraph<Integer, String> directedGraph = buildDirectedTestGraph();
    directedGraph.addNode(N3);
    Predicate<Integer> nodePredicate = connectedNodePredicate(directedGraph);

    DirectedGraph<Integer, String> actualGraph = Graphs.createDirected();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4); // ensure that we don't remove nodes that don't pass the predicate
    actualGraph.addNode(N2); // ensure that a pre-existing node is not affected by the merging

    mergeNodesFrom(directedGraph, actualGraph, nodePredicate);

    DirectedGraph<Integer, String> expectedGraph = Graphs.createDirected();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    // N3 is not expected because it's not connected
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeEdgesFrom_directed() {
    DirectedGraph<Integer, String> directedGraph = buildDirectedTestGraph();

    DirectedGraph<Integer, String> actualGraph = Graphs.createDirected(MULTIGRAPH);
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addEdge(E11, N1, N1);
    actualGraph.addEdge(E22, N2, N2);

    mergeEdgesFrom(directedGraph, actualGraph);

    DirectedGraph<Integer, String> expectedGraph = buildDirectedTestGraph();
    expectedGraph.addEdge(E22, N2, N2);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeEdgesFrom_filtered_directed() {
    DirectedGraph<Integer, String> directedGraph = buildDirectedTestGraph();
    directedGraph.addNode(N3);
    Predicate<String> edgePredicate = noSelfLoopPredicate(directedGraph);

    DirectedGraph<Integer, String> actualGraph = Graphs.createDirected(MULTIGRAPH);
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addEdge(E11, N1, N1); // existing (redundant) self-loops should be retained
    actualGraph.addEdge(E22, N2, N2); // existing (novel) self-loops should be retained
    actualGraph.addEdge(E44, N4, N4); // existing self-loops for unrelated nodes should be retained
    actualGraph.addEdge(E12, N1, N2); // existing edges should be unaffected

    mergeEdgesFrom(directedGraph, actualGraph, edgePredicate);

    DirectedGraph<Integer, String> expectedGraph = Graphs.createDirected(MULTIGRAPH);
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
    UndirectedGraph<Integer, String> undirectedGraph = buildUndirectedTestGraph();
    undirectedGraph.addNode(N3);

    UndirectedGraph<Integer, String> actualGraph = Graphs.createUndirected();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4);
    actualGraph.addNode(N2);

    mergeNodesFrom(undirectedGraph, actualGraph);

    UndirectedGraph<Integer, String> expectedGraph = Graphs.createUndirected();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    expectedGraph.addNode(N3);
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeNodesFrom_filtered_undirected() {
    UndirectedGraph<Integer, String> undirectedGraph = buildUndirectedTestGraph();
    undirectedGraph.addNode(N3);
    Predicate<Integer> nodePredicate = connectedNodePredicate(undirectedGraph);

    UndirectedGraph<Integer, String> actualGraph = Graphs.createUndirected();
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addNode(N4); // ensure that we don't remove nodes that don't pass the predicate
    actualGraph.addNode(N2); // ensure that a pre-existing node is not affected by the merging

    mergeNodesFrom(undirectedGraph, actualGraph, nodePredicate);

    UndirectedGraph<Integer, String> expectedGraph = Graphs.createUndirected();
    expectedGraph.addNode(N1);
    expectedGraph.addNode(N2);
    // N3 is not expected because it's not connected
    expectedGraph.addNode(N4);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeEdgesFrom_undirected() {
    UndirectedGraph<Integer, String> undirectedGraph = buildUndirectedTestGraph();

    UndirectedGraph<Integer, String> actualGraph = Graphs.createUndirected(MULTIGRAPH);
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addEdge(E11, N1, N1);
    actualGraph.addEdge(E22, N2, N2);

    mergeEdgesFrom(undirectedGraph, actualGraph);

    UndirectedGraph<Integer, String> expectedGraph = buildUndirectedTestGraph();
    expectedGraph.addEdge(E22, N2, N2);

    assertThat(actualGraph).isEqualTo(expectedGraph);
  }

  @Test
  public void mergeEdgesFrom_filtered_undirected() {
    UndirectedGraph<Integer, String> undirectedGraph = buildUndirectedTestGraph();
    undirectedGraph.addNode(N3);
    Predicate<String> edgePredicate = noSelfLoopPredicate(undirectedGraph);

    UndirectedGraph<Integer, String> actualGraph = Graphs.createUndirected(MULTIGRAPH);
    // prepopulate actualGraph to make sure that existing elements don't interfere with the merging
    actualGraph.addEdge(E11, N1, N1); // existing (redundant) self-loops should be retained
    actualGraph.addEdge(E22, N2, N2); // existing (novel) self-loops should be retained
    actualGraph.addEdge(E44, N4, N4); // existing self-loops for unrelated nodes should be retained
    actualGraph.addEdge(E12, N1, N2); // existing edges should be unaffected

    mergeEdgesFrom(undirectedGraph, actualGraph, edgePredicate);

    UndirectedGraph<Integer, String> expectedGraph = Graphs.createUndirected(MULTIGRAPH);
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

  private static DirectedGraph<Integer, String> buildDirectedTestGraph() {
    DirectedGraph<Integer, String> directedGraph = Graphs.createDirected(MULTIGRAPH);
    directedGraph.addEdge(E11, N1, N1);
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E11_A, N1, N1);
    directedGraph.addEdge(E12_A, N1, N2);
    directedGraph.addEdge(E21, N2, N1);

    return directedGraph;
  }

  private static UndirectedGraph<Integer, String> buildUndirectedTestGraph() {
    UndirectedGraph<Integer, String> undirectedGraph = Graphs.createUndirected(MULTIGRAPH);
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
