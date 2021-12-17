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

import static com.google.common.graph.Graphs.copyOf;
import static com.google.common.graph.Graphs.inducedSubgraph;
import static com.google.common.graph.Graphs.reachableNodes;
import static com.google.common.graph.Graphs.transitiveClosure;
import static com.google.common.graph.Graphs.transpose;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link Graphs}. Tests assume that the implementation of the method {@code addEdge} adds
 * the missing nodes to the graph, then adds the edge between them.
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
  private static final String E13 = "1-3";
  private static final String E31 = "3-1";
  private static final String E34 = "3-4";
  private static final String E44 = "4-4";
  private static final int NODE_COUNT = 20;
  private static final int EDGE_COUNT = 20;
  // TODO(user): Consider adding both error messages from here and {@link AbstractNetworkTest}
  // in one class (may be a utility class for error messages).
  private static final String ERROR_PARALLEL_EDGE = "connected by a different edge";
  private static final String ERROR_NEGATIVE_COUNT = "is non-negative";
  private static final String ERROR_ADDED_PARALLEL_EDGE =
      "Should not be allowed to add a parallel edge.";
  private static final String ERROR_ADDED_SELF_LOOP =
      "Should not be allowed to add a self-loop edge.";
  static final String ERROR_SELF_LOOP = "self-loops are not allowed";

  @Test
  public void transitiveClosure_directedGraph() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().allowsSelfLoops(false).build();
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N1, N3);
    directedGraph.putEdge(N2, N3);
    directedGraph.addNode(N4);

    MutableGraph<Integer> expectedClosure = GraphBuilder.directed().allowsSelfLoops(true).build();
    expectedClosure.putEdge(N1, N1);
    expectedClosure.putEdge(N1, N2);
    expectedClosure.putEdge(N1, N3);
    expectedClosure.putEdge(N2, N2);
    expectedClosure.putEdge(N2, N3);
    expectedClosure.putEdge(N3, N3);
    expectedClosure.putEdge(N4, N4);

    checkTransitiveClosure(directedGraph, expectedClosure);
  }

  @Test
  public void transitiveClosure_undirectedGraph() {
    MutableGraph<Integer> undirectedGraph =
        GraphBuilder.undirected().allowsSelfLoops(false).build();
    undirectedGraph.putEdge(N1, N2);
    undirectedGraph.putEdge(N1, N3);
    undirectedGraph.putEdge(N2, N3);
    undirectedGraph.addNode(N4);

    MutableGraph<Integer> expectedClosure = GraphBuilder.undirected().allowsSelfLoops(true).build();
    expectedClosure.putEdge(N1, N1);
    expectedClosure.putEdge(N1, N2);
    expectedClosure.putEdge(N1, N3);
    expectedClosure.putEdge(N2, N2);
    expectedClosure.putEdge(N2, N3);
    expectedClosure.putEdge(N3, N3);
    expectedClosure.putEdge(N4, N4);

    checkTransitiveClosure(undirectedGraph, expectedClosure);
  }

  @Test
  public void transitiveClosure_directedPathGraph() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().allowsSelfLoops(false).build();
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N2, N3);
    directedGraph.putEdge(N3, N4);

    MutableGraph<Integer> expectedClosure = GraphBuilder.directed().allowsSelfLoops(true).build();
    expectedClosure.putEdge(N1, N1);
    expectedClosure.putEdge(N1, N2);
    expectedClosure.putEdge(N1, N3);
    expectedClosure.putEdge(N1, N4);
    expectedClosure.putEdge(N2, N2);
    expectedClosure.putEdge(N2, N3);
    expectedClosure.putEdge(N2, N4);
    expectedClosure.putEdge(N3, N3);
    expectedClosure.putEdge(N3, N4);
    expectedClosure.putEdge(N4, N4);

    checkTransitiveClosure(directedGraph, expectedClosure);
  }

  @Test
  public void transitiveClosure_undirectedPathGraph() {
    MutableGraph<Integer> undirectedGraph =
        GraphBuilder.undirected().allowsSelfLoops(false).build();
    undirectedGraph.putEdge(N1, N2);
    undirectedGraph.putEdge(N2, N3);
    undirectedGraph.putEdge(N3, N4);

    MutableGraph<Integer> expectedClosure = GraphBuilder.undirected().allowsSelfLoops(true).build();
    expectedClosure.putEdge(N1, N1);
    expectedClosure.putEdge(N1, N2);
    expectedClosure.putEdge(N1, N3);
    expectedClosure.putEdge(N1, N4);
    expectedClosure.putEdge(N2, N2);
    expectedClosure.putEdge(N2, N3);
    expectedClosure.putEdge(N2, N4);
    expectedClosure.putEdge(N3, N3);
    expectedClosure.putEdge(N3, N4);
    expectedClosure.putEdge(N4, N4);

    checkTransitiveClosure(undirectedGraph, expectedClosure);
  }

  @Test
  public void transitiveClosure_directedCycleGraph() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().allowsSelfLoops(false).build();
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N2, N3);
    directedGraph.putEdge(N3, N4);
    directedGraph.putEdge(N4, N1);

    MutableGraph<Integer> expectedClosure = GraphBuilder.directed().allowsSelfLoops(true).build();
    expectedClosure.putEdge(N1, N1);
    expectedClosure.putEdge(N1, N2);
    expectedClosure.putEdge(N1, N3);
    expectedClosure.putEdge(N1, N4);
    expectedClosure.putEdge(N2, N1);
    expectedClosure.putEdge(N2, N2);
    expectedClosure.putEdge(N2, N3);
    expectedClosure.putEdge(N2, N4);
    expectedClosure.putEdge(N3, N1);
    expectedClosure.putEdge(N3, N2);
    expectedClosure.putEdge(N3, N3);
    expectedClosure.putEdge(N3, N4);
    expectedClosure.putEdge(N4, N1);
    expectedClosure.putEdge(N4, N2);
    expectedClosure.putEdge(N4, N3);
    expectedClosure.putEdge(N4, N4);

    checkTransitiveClosure(directedGraph, expectedClosure);
  }

  @Test
  public void transitiveClosure_undirectedCycleGraph() {
    MutableGraph<Integer> undirectedGraph =
        GraphBuilder.undirected().allowsSelfLoops(false).build();
    undirectedGraph.putEdge(N1, N2);
    undirectedGraph.putEdge(N2, N3);
    undirectedGraph.putEdge(N3, N4);
    undirectedGraph.putEdge(N4, N1);

    MutableGraph<Integer> expectedClosure = GraphBuilder.undirected().allowsSelfLoops(true).build();
    expectedClosure.putEdge(N1, N1);
    expectedClosure.putEdge(N1, N2);
    expectedClosure.putEdge(N1, N3);
    expectedClosure.putEdge(N1, N4);
    expectedClosure.putEdge(N2, N2);
    expectedClosure.putEdge(N2, N3);
    expectedClosure.putEdge(N2, N4);
    expectedClosure.putEdge(N3, N3);
    expectedClosure.putEdge(N3, N4);
    expectedClosure.putEdge(N4, N4);

    checkTransitiveClosure(undirectedGraph, expectedClosure);
  }

  @Test
  public void transpose_undirectedGraph() {
    MutableGraph<Integer> undirectedGraph = GraphBuilder.undirected().build();
    undirectedGraph.putEdge(N1, N2);

    assertThat(transpose(undirectedGraph)).isSameInstanceAs(undirectedGraph);
  }

  @Test
  public void transpose_directedGraph() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().allowsSelfLoops(true).build();
    directedGraph.putEdge(N1, N3);
    directedGraph.putEdge(N3, N1);
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N1, N1);
    directedGraph.putEdge(N3, N4);

    MutableGraph<Integer> expectedTranspose = GraphBuilder.directed().allowsSelfLoops(true).build();
    expectedTranspose.putEdge(N3, N1);
    expectedTranspose.putEdge(N1, N3);
    expectedTranspose.putEdge(N2, N1);
    expectedTranspose.putEdge(N1, N1);
    expectedTranspose.putEdge(N4, N3);

    Graph<Integer> transpose = transpose(directedGraph);
    assertThat(transpose).isEqualTo(expectedTranspose);
    assertThat(transpose(transpose)).isSameInstanceAs(directedGraph);
    AbstractGraphTest.validateGraph(transpose);

    for (Integer node : directedGraph.nodes()) {
      assertThat(directedGraph.inDegree(node)).isSameInstanceAs(transpose.outDegree(node));
      assertThat(directedGraph.outDegree(node)).isSameInstanceAs(transpose.inDegree(node));
    }

    assertThat(transpose.successors(N1)).doesNotContain(N2);
    directedGraph.putEdge(N2, N1);
    // View should be updated.
    assertThat(transpose.successors(N1)).contains(N2);
    AbstractGraphTest.validateGraph(transpose);
  }

  @Test
  public void transpose_undirectedValueGraph() {
    MutableValueGraph<Integer, String> undirectedGraph = ValueGraphBuilder.undirected().build();
    undirectedGraph.putEdgeValue(N1, N2, E12);

    assertThat(transpose(undirectedGraph)).isSameInstanceAs(undirectedGraph);
  }

  @Test
  public void transpose_directedValueGraph() {
    MutableValueGraph<Integer, String> directedGraph =
        ValueGraphBuilder.directed().allowsSelfLoops(true).build();
    directedGraph.putEdgeValue(N1, N3, E13);
    directedGraph.putEdgeValue(N3, N1, E31);
    directedGraph.putEdgeValue(N1, N2, E12);
    directedGraph.putEdgeValue(N1, N1, E11);
    directedGraph.putEdgeValue(N3, N4, E34);

    MutableValueGraph<Integer, String> expectedTranspose =
        ValueGraphBuilder.directed().allowsSelfLoops(true).build();
    expectedTranspose.putEdgeValue(N3, N1, E13);
    expectedTranspose.putEdgeValue(N1, N3, E31);
    expectedTranspose.putEdgeValue(N2, N1, E12);
    expectedTranspose.putEdgeValue(N1, N1, E11);
    expectedTranspose.putEdgeValue(N4, N3, E34);

    ValueGraph<Integer, String> transpose = transpose(directedGraph);
    assertThat(transpose).isEqualTo(expectedTranspose);
    assertThat(transpose(transpose)).isSameInstanceAs(directedGraph);
    AbstractGraphTest.validateGraph(transpose.asGraph());

    assertThat(transpose.edgeValueOrDefault(N1, N2, null)).isNull();
    for (Integer node : directedGraph.nodes()) {
      assertThat(directedGraph.inDegree(node)).isSameInstanceAs(transpose.outDegree(node));
      assertThat(directedGraph.outDegree(node)).isSameInstanceAs(transpose.inDegree(node));
    }

    directedGraph.putEdgeValue(N2, N1, E21);
    // View should be updated.
    assertThat(transpose.edgeValueOrDefault(N1, N2, null)).isEqualTo(E21);
    AbstractGraphTest.validateGraph(transpose.asGraph());
  }

  @Test
  public void transpose_undirectedNetwork() {
    MutableNetwork<Integer, String> undirectedGraph = NetworkBuilder.undirected().build();
    undirectedGraph.addEdge(N1, N2, E12);

    assertThat(transpose(undirectedGraph)).isSameInstanceAs(undirectedGraph);
  }

  @Test
  public void transpose_directedNetwork() {
    MutableNetwork<Integer, String> directedGraph =
        NetworkBuilder.directed().allowsParallelEdges(true).allowsSelfLoops(true).build();
    directedGraph.addEdge(N1, N3, E13);
    directedGraph.addEdge(N3, N1, E31);
    directedGraph.addEdge(N1, N2, E12);
    directedGraph.addEdge(N1, N2, E12_A);
    directedGraph.addEdge(N1, N1, E11);
    directedGraph.addEdge(N3, N4, E34);

    MutableNetwork<Integer, String> expectedTranspose =
        NetworkBuilder.directed().allowsParallelEdges(true).allowsSelfLoops(true).build();
    expectedTranspose.addEdge(N3, N1, E13);
    expectedTranspose.addEdge(N1, N3, E31);
    expectedTranspose.addEdge(N2, N1, E12);
    expectedTranspose.addEdge(N2, N1, E12_A);
    expectedTranspose.addEdge(N1, N1, E11);
    expectedTranspose.addEdge(N4, N3, E34);

    Network<Integer, String> transpose = transpose(directedGraph);
    assertThat(transpose).isEqualTo(expectedTranspose);
    assertThat(transpose(transpose)).isSameInstanceAs(directedGraph);
    AbstractNetworkTest.validateNetwork(transpose);

    assertThat(transpose.edgesConnecting(N1, N2)).isEmpty();
    assertThat(transpose.edgeConnectingOrNull(N1, N2)).isNull();

    for (Integer node : directedGraph.nodes()) {
      assertThat(directedGraph.inDegree(node)).isSameInstanceAs(transpose.outDegree(node));
      assertThat(directedGraph.outDegree(node)).isSameInstanceAs(transpose.inDegree(node));
    }

    directedGraph.addEdge(N2, N1, E21);
    // View should be updated.
    assertThat(transpose.edgesConnecting(N1, N2)).containsExactly(E21);
    assertThat(transpose.edgeConnectingOrNull(N1, N2)).isEqualTo(E21);

    AbstractNetworkTest.validateNetwork(transpose);
  }

  @Test
  public void inducedSubgraph_graph() {
    Set<Integer> nodeSubset = ImmutableSet.of(N1, N2, N4);

    MutableGraph<Integer> directedGraph = GraphBuilder.directed().allowsSelfLoops(true).build();
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N2, N1);
    directedGraph.putEdge(N1, N3); // only incident to one node in nodeSubset
    directedGraph.putEdge(N4, N4);
    directedGraph.putEdge(5, 6); // not incident to any node in nodeSubset

    MutableGraph<Integer> expectedSubgraph = GraphBuilder.directed().allowsSelfLoops(true).build();
    expectedSubgraph.putEdge(N1, N2);
    expectedSubgraph.putEdge(N2, N1);
    expectedSubgraph.putEdge(N4, N4);

    assertThat(inducedSubgraph(directedGraph, nodeSubset)).isEqualTo(expectedSubgraph);
  }

  @Test
  public void inducedSubgraph_valueGraph() {
    Set<Integer> nodeSubset = ImmutableSet.of(N1, N2, N4);

    MutableValueGraph<Integer, String> directedGraph =
        ValueGraphBuilder.directed().allowsSelfLoops(true).build();
    directedGraph.putEdgeValue(N1, N2, E12);
    directedGraph.putEdgeValue(N2, N1, E21);
    directedGraph.putEdgeValue(N1, N3, E13); // only incident to one node in nodeSubset
    directedGraph.putEdgeValue(N4, N4, E44);
    directedGraph.putEdgeValue(5, 6, "5-6"); // not incident to any node in nodeSubset

    MutableValueGraph<Integer, String> expectedSubgraph =
        ValueGraphBuilder.directed().allowsSelfLoops(true).build();
    expectedSubgraph.putEdgeValue(N1, N2, E12);
    expectedSubgraph.putEdgeValue(N2, N1, E21);
    expectedSubgraph.putEdgeValue(N4, N4, E44);

    assertThat(inducedSubgraph(directedGraph, nodeSubset)).isEqualTo(expectedSubgraph);
  }

  @Test
  public void inducedSubgraph_network() {
    Set<Integer> nodeSubset = ImmutableSet.of(N1, N2, N4);

    MutableNetwork<Integer, String> directedGraph =
        NetworkBuilder.directed().allowsSelfLoops(true).build();
    directedGraph.addEdge(N1, N2, E12);
    directedGraph.addEdge(N2, N1, E21);
    directedGraph.addEdge(N1, N3, E13); // only incident to one node in nodeSubset
    directedGraph.addEdge(N4, N4, E44);
    directedGraph.addEdge(5, 6, "5-6"); // not incident to any node in nodeSubset

    MutableNetwork<Integer, String> expectedSubgraph =
        NetworkBuilder.directed().allowsSelfLoops(true).build();
    expectedSubgraph.addEdge(N1, N2, E12);
    expectedSubgraph.addEdge(N2, N1, E21);
    expectedSubgraph.addEdge(N4, N4, E44);

    assertThat(inducedSubgraph(directedGraph, nodeSubset)).isEqualTo(expectedSubgraph);
  }

  @Test
  public void inducedSubgraph_nodeNotInGraph() {
    MutableNetwork<Integer, String> undirectedGraph = NetworkBuilder.undirected().build();

    try {
      inducedSubgraph(undirectedGraph, ImmutableSet.of(N1));
      fail("Should have rejected getting induced subgraph with node not in original graph.");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void copyOf_nullArgument() {
    try {
      copyOf((Graph<?>) null);
      fail("Should have rejected a null graph.");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void copyOf_directedGraph() {
    Graph<Integer> directedGraph = buildDirectedGraph();

    Graph<Integer> copy = copyOf(directedGraph);
    assertThat(copy).isEqualTo(directedGraph);
  }

  @Test
  public void copyOf_undirectedGraph() {
    Graph<Integer> undirectedGraph = buildUndirectedGraph();

    Graph<Integer> copy = copyOf(undirectedGraph);
    assertThat(copy).isEqualTo(undirectedGraph);
  }

  @Test
  public void copyOf_directedValueGraph() {
    ValueGraph<Integer, String> directedGraph = buildDirectedValueGraph();

    ValueGraph<Integer, String> copy = copyOf(directedGraph);
    assertThat(copy).isEqualTo(directedGraph);
  }

  @Test
  public void copyOf_undirectedValueGraph() {
    ValueGraph<Integer, String> undirectedGraph = buildUndirectedValueGraph();

    ValueGraph<Integer, String> copy = copyOf(undirectedGraph);
    assertThat(copy).isEqualTo(undirectedGraph);
  }

  @Test
  public void copyOf_directedNetwork() {
    Network<Integer, String> directedGraph = buildDirectedNetwork();

    Network<Integer, String> copy = copyOf(directedGraph);
    assertThat(copy).isEqualTo(directedGraph);
  }

  @Test
  public void copyOf_undirectedNetwork() {
    Network<Integer, String> undirectedGraph = buildUndirectedNetwork();

    Network<Integer, String> copy = copyOf(undirectedGraph);
    assertThat(copy).isEqualTo(undirectedGraph);
  }

  // Graph creation tests

  @Test
  public void createDirected() {
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    assertThat(directedGraph.nodes()).isEmpty();
    assertThat(directedGraph.edges()).isEmpty();
    assertThat(directedGraph.addEdge(N1, N2, E12)).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();

    // By default, parallel edges are not allowed.
    try {
      directedGraph.addEdge(N1, N2, E12_A);
      fail(ERROR_ADDED_PARALLEL_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }

    // By default, self-loop edges are not allowed.
    try {
      directedGraph.addEdge(N1, N1, E11);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().contains(ERROR_SELF_LOOP);
    }
  }

  @Test
  public void createUndirected() {
    MutableNetwork<Integer, String> undirectedGraph = NetworkBuilder.undirected().build();
    assertThat(undirectedGraph.nodes()).isEmpty();
    assertThat(undirectedGraph.edges()).isEmpty();
    assertThat(undirectedGraph.addEdge(N1, N2, E12)).isTrue();
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).isEqualTo(ImmutableSet.of(E12));

    // By default, parallel edges are not allowed.
    try {
      undirectedGraph.addEdge(N1, N2, E12_A);
      fail(ERROR_ADDED_PARALLEL_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }
    try {
      undirectedGraph.addEdge(N2, N1, E21);
      fail(ERROR_ADDED_PARALLEL_EDGE);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_PARALLEL_EDGE);
    }

    // By default, self-loop edges are not allowed.
    try {
      undirectedGraph.addEdge(N1, N1, E11);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().contains(ERROR_SELF_LOOP);
    }
  }

  @Test
  public void createDirected_multigraph() {
    MutableNetwork<Integer, String> directedMultigraph =
        NetworkBuilder.directed().allowsParallelEdges(true).build();
    assertThat(directedMultigraph.addEdge(N1, N2, E12)).isTrue();
    assertThat(directedMultigraph.addEdge(N1, N2, E12_A)).isTrue();
    assertThat(directedMultigraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12, E12_A));
    assertThat(directedMultigraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_multigraph() {
    MutableNetwork<Integer, String> undirectedMultigraph =
        NetworkBuilder.undirected().allowsParallelEdges(true).build();
    assertThat(undirectedMultigraph.addEdge(N1, N2, E12)).isTrue();
    assertThat(undirectedMultigraph.addEdge(N1, N2, E12_A)).isTrue();
    assertThat(undirectedMultigraph.addEdge(N2, N1, E21)).isTrue();
    assertThat(undirectedMultigraph.edgesConnecting(N1, N2))
        .isEqualTo(ImmutableSet.of(E12, E12_A, E21));
  }

  @Test
  public void createDirected_expectedNodeCount() {
    MutableNetwork<Integer, String> directedGraph =
        NetworkBuilder.directed().expectedNodeCount(NODE_COUNT).build();
    assertThat(directedGraph.addEdge(N1, N2, E12)).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_expectedNodeCount() {
    MutableNetwork<Integer, String> undirectedGraph =
        NetworkBuilder.undirected().expectedNodeCount(NODE_COUNT).build();
    assertThat(undirectedGraph.addEdge(N1, N2, E12)).isTrue();
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).isEqualTo(ImmutableSet.of(E12));
  }

  @Test
  public void builder_expectedNodeCount_negative() {
    try {
      NetworkBuilder.directed().expectedNodeCount(-1);
      fail("Should have rejected negative expected node count.");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_NEGATIVE_COUNT);
    }
  }

  @Test
  public void createDirected_expectedEdgeCount() {
    MutableNetwork<Integer, String> directedGraph =
        NetworkBuilder.directed().expectedEdgeCount(EDGE_COUNT).build();
    assertThat(directedGraph.addEdge(N1, N2, E12)).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_expectedEdgeCount() {
    MutableNetwork<Integer, String> undirectedGraph =
        NetworkBuilder.undirected().expectedEdgeCount(EDGE_COUNT).build();
    assertThat(undirectedGraph.addEdge(N1, N2, E12)).isTrue();
    assertThat(undirectedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(undirectedGraph.edgesConnecting(N2, N1)).isEqualTo(ImmutableSet.of(E12));
  }

  @Test
  public void builder_expectedEdgeCount_negative() {
    try {
      NetworkBuilder.directed().expectedEdgeCount(-1);
      fail("Should have rejected negative expected edge count.");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_NEGATIVE_COUNT);
    }
  }

  private static <N> void checkTransitiveClosure(Graph<N> originalGraph, Graph<N> expectedClosure) {
    for (N node : originalGraph.nodes()) {
      assertThat(reachableNodes(originalGraph, node)).isEqualTo(expectedClosure.successors(node));
    }
    assertThat(transitiveClosure(originalGraph)).isEqualTo(expectedClosure);
  }

  private static MutableGraph<Integer> buildDirectedGraph() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().allowsSelfLoops(true).build();
    directedGraph.putEdge(N1, N1);
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N2, N1);

    return directedGraph;
  }

  private static MutableGraph<Integer> buildUndirectedGraph() {
    MutableGraph<Integer> undirectedGraph = GraphBuilder.undirected().allowsSelfLoops(true).build();
    undirectedGraph.putEdge(N1, N1);
    undirectedGraph.putEdge(N1, N2);
    undirectedGraph.putEdge(N2, N1);

    return undirectedGraph;
  }

  private static MutableValueGraph<Integer, String> buildDirectedValueGraph() {
    MutableValueGraph<Integer, String> directedGraph =
        ValueGraphBuilder.directed().allowsSelfLoops(true).build();
    directedGraph.putEdgeValue(N1, N1, E11);
    directedGraph.putEdgeValue(N1, N2, E12);
    directedGraph.putEdgeValue(N2, N1, E21);

    return directedGraph;
  }

  private static MutableValueGraph<Integer, String> buildUndirectedValueGraph() {
    MutableValueGraph<Integer, String> undirectedGraph =
        ValueGraphBuilder.undirected().allowsSelfLoops(true).build();
    undirectedGraph.putEdgeValue(N1, N1, E11);
    undirectedGraph.putEdgeValue(N1, N2, E12);
    undirectedGraph.putEdgeValue(N2, N1, E21); // overwrites E12

    return undirectedGraph;
  }

  private static MutableNetwork<Integer, String> buildDirectedNetwork() {
    MutableNetwork<Integer, String> directedGraph =
        NetworkBuilder.directed().allowsParallelEdges(true).allowsSelfLoops(true).build();
    directedGraph.addEdge(N1, N1, E11);
    directedGraph.addEdge(N1, N2, E12);
    directedGraph.addEdge(N1, N1, E11_A);
    directedGraph.addEdge(N1, N2, E12_A);
    directedGraph.addEdge(N2, N1, E21);

    return directedGraph;
  }

  private static MutableNetwork<Integer, String> buildUndirectedNetwork() {
    MutableNetwork<Integer, String> undirectedGraph =
        NetworkBuilder.undirected().allowsParallelEdges(true).allowsSelfLoops(true).build();
    undirectedGraph.addEdge(N1, N1, E11);
    undirectedGraph.addEdge(N1, N2, E12);
    undirectedGraph.addEdge(N1, N1, E11_A);
    undirectedGraph.addEdge(N1, N2, E12_A);
    undirectedGraph.addEdge(N2, N1, E21);

    return undirectedGraph;
  }
}
