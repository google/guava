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
import static com.google.common.graph.Graphs.inducedSubgraph;
import static com.google.common.graph.Graphs.roots;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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

  /**
   * Returns a {@link Predicate} that returns {@code true} if the input edge is a self-loop in
   * {@code graph}. A self-loop is defined as an edge whose pair of incident nodes are equal.
   * The predicate's {@code apply} method will throw an {@link IllegalArgumentException} if
   * {@code graph} does not contain {@code edge}.
   */
  private static <E> Predicate<E> selfLoopPredicate(final Network<?, E> graph) {
    checkNotNull(graph, "graph");
    return new Predicate<E>() {
      @Override
      public boolean apply(E edge) {
        Endpoints<?> endpoints = graph.incidentNodes(edge);
        return endpoints.nodeA().equals(endpoints.nodeB());
      }
    };
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
  public void addEdge_mismatchedDirectedness() {
    try {
      addEdge(NetworkBuilder.undirected().<Integer, String>build(), E12,
          Endpoints.ofDirected(N1, N2));
      fail("Should have rejected adding an edge with directed endpoints to a undirected graph.");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void addEdge_selfLoop() {
    MutableNetwork<Integer, String> undirectedGraph = NetworkBuilder.undirected().build();
    assertThat(addEdge(undirectedGraph, E11, Endpoints.ofUndirected(N1, N1))).isTrue();
    assertThat(undirectedGraph.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void addEdge_basic() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    assertThat(addEdge(directedGraph, E12, Endpoints.ofDirected(N1, N2))).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void inducedSubgraph_partialEdgeIncidence() {
    Set<Integer> nodeSubset = ImmutableSet.of(N1, N2, N4);

    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addEdge(E12, N1, N2);
    directedGraph.addEdge(E21, N2, N1);
    directedGraph.addEdge(E13, N1, N3); // only incident to one node in nodeSubset
    directedGraph.addEdge(E44, N4, N4);
    directedGraph.addEdge("5-6", 5, 6); // not incident to any node in nodeSubset

    MutableNetwork<Integer, String> expectedSubgraph = NetworkBuilder.directed().build();
    expectedSubgraph.addEdge(E12, N1, N2);
    expectedSubgraph.addEdge(E21, N2, N1);
    expectedSubgraph.addEdge(E44, N4, N4);

    assertThat(inducedSubgraph(directedGraph, nodeSubset)).isEqualTo(expectedSubgraph);
  }

  @Test
  public void inducedSubgraph_nodeNotInGraph() {
    MutableNetwork<Integer, String> undirectedGraph = NetworkBuilder.undirected().build();

    try {
      inducedSubgraph(undirectedGraph, ImmutableSet.of(N1));
      fail("Should have rejected getting induced subgraph with node not in original graph");
    } catch (IllegalArgumentException expected) {
    }
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

  // Graph creation tests

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
