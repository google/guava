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
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
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
  private static final String E12_B = "1-2b";
  private static final String E21 = "2-1";
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
  public void transitiveClosure_directedGraph() {
    MutableBasicGraph<Integer> directedGraph =
        BasicGraphBuilder.directed().allowsSelfLoops(false).build();
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N1, N3);
    directedGraph.putEdge(N2, N3);
    directedGraph.addNode(N4);

    MutableBasicGraph<Integer> expectedClosure =
        BasicGraphBuilder.directed().allowsSelfLoops(true).build();
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
    MutableBasicGraph<Integer> undirectedGraph =
        BasicGraphBuilder.undirected().allowsSelfLoops(false).build();
    undirectedGraph.putEdge(N1, N2);
    undirectedGraph.putEdge(N1, N3);
    undirectedGraph.putEdge(N2, N3);
    undirectedGraph.addNode(N4);

    MutableBasicGraph<Integer> expectedClosure =
        BasicGraphBuilder.undirected().allowsSelfLoops(true).build();
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
    MutableBasicGraph<Integer> directedGraph =
        BasicGraphBuilder.directed().allowsSelfLoops(false).build();
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N2, N3);
    directedGraph.putEdge(N3, N4);

    MutableBasicGraph<Integer> expectedClosure =
        BasicGraphBuilder.directed().allowsSelfLoops(true).build();
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
    MutableBasicGraph<Integer> undirectedGraph =
        BasicGraphBuilder.undirected().allowsSelfLoops(false).build();
    undirectedGraph.putEdge(N1, N2);
    undirectedGraph.putEdge(N2, N3);
    undirectedGraph.putEdge(N3, N4);

    MutableBasicGraph<Integer> expectedClosure =
        BasicGraphBuilder.undirected().allowsSelfLoops(true).build();
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
    MutableBasicGraph<Integer> directedGraph =
        BasicGraphBuilder.directed().allowsSelfLoops(false).build();
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N2, N3);
    directedGraph.putEdge(N3, N4);
    directedGraph.putEdge(N4, N1);

    MutableBasicGraph<Integer> expectedClosure =
        BasicGraphBuilder.directed().allowsSelfLoops(true).build();
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
    MutableBasicGraph<Integer> undirectedGraph =
        BasicGraphBuilder.undirected().allowsSelfLoops(false).build();
    undirectedGraph.putEdge(N1, N2);
    undirectedGraph.putEdge(N2, N3);
    undirectedGraph.putEdge(N3, N4);
    undirectedGraph.putEdge(N4, N1);

    MutableBasicGraph<Integer> expectedClosure =
        BasicGraphBuilder.undirected().allowsSelfLoops(true).build();
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
  public void inducedSubgraph_BasicGraph() {
    Set<Integer> nodeSubset = ImmutableSet.of(N1, N2, N4);

    MutableBasicGraph<Integer> directedGraph = BasicGraphBuilder.directed().build();
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N2, N1);
    directedGraph.putEdge(N1, N3); // only incident to one node in nodeSubset
    directedGraph.putEdge(N4, N4);
    directedGraph.putEdge(5, 6); // not incident to any node in nodeSubset

    MutableBasicGraph<Integer> expectedSubgraph = BasicGraphBuilder.directed().build();
    expectedSubgraph.putEdge(N1, N2);
    expectedSubgraph.putEdge(N2, N1);
    expectedSubgraph.putEdge(N4, N4);

    assertThat(inducedSubgraph(directedGraph, nodeSubset)).isEqualTo(expectedSubgraph);
  }

  @Test
  public void inducedSubgraph_Graph() {
    Set<Integer> nodeSubset = ImmutableSet.of(N1, N2, N4);

    MutableGraph<Integer, String> directedGraph = GraphBuilder.directed().build();
    directedGraph.putEdgeValue(N1, N2, E12);
    directedGraph.putEdgeValue(N2, N1, E21);
    directedGraph.putEdgeValue(N1, N3, E13); // only incident to one node in nodeSubset
    directedGraph.putEdgeValue(N4, N4, E44);
    directedGraph.putEdgeValue(5, 6, "5-6"); // not incident to any node in nodeSubset

    MutableGraph<Integer, String> expectedSubgraph = GraphBuilder.directed().build();
    expectedSubgraph.putEdgeValue(N1, N2, E12);
    expectedSubgraph.putEdgeValue(N2, N1, E21);
    expectedSubgraph.putEdgeValue(N4, N4, E44);

    assertThat(inducedSubgraph(directedGraph, nodeSubset)).isEqualTo(expectedSubgraph);
  }

  @Test
  public void inducedSubgraph_Network() {
    Set<Integer> nodeSubset = ImmutableSet.of(N1, N2, N4);

    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed().build();
    directedGraph.addEdge(N1, N2, E12);
    directedGraph.addEdge(N2, N1, E21);
    directedGraph.addEdge(N1, N3, E13); // only incident to one node in nodeSubset
    directedGraph.addEdge(N4, N4, E44);
    directedGraph.addEdge(5, 6, "5-6"); // not incident to any node in nodeSubset

    MutableNetwork<Integer, String> expectedSubgraph = NetworkBuilder.directed().build();
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
      fail("Should have rejected getting induced subgraph with node not in original graph");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void copyOf_nullArgument() {
    try {
      copyOf((BasicGraph<?>) null);
      fail("Should have rejected a null graph");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void copyOf_directedBasicGraph() {
    BasicGraph<Integer> directedGraph = buildDirectedBasicGraph();

    BasicGraph<Integer> copy = copyOf(directedGraph);
    assertThat(copy).isEqualTo(directedGraph);
  }

  @Test
  public void copyOf_undirectedBasicGraph() {
    BasicGraph<Integer> undirectedGraph = buildUndirectedBasicGraph();

    BasicGraph<Integer> copy = copyOf(undirectedGraph);
    assertThat(copy).isEqualTo(undirectedGraph);
  }

  @Test
  public void copyOf_directedGraph() {
    Graph<Integer, String> directedGraph = buildDirectedGraph();

    Graph<Integer, String> copy = copyOf(directedGraph);
    assertThat(copy).isEqualTo(directedGraph);
  }

  @Test
  public void copyOf_undirectedGraph() {
    Graph<Integer, String> undirectedGraph = buildUndirectedGraph();

    Graph<Integer, String> copy = copyOf(undirectedGraph);
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
    // By default, self-loop edges are allowed.
    assertThat(directedGraph.addEdge(N1, N1, E11)).isTrue();
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
    // By default, self-loop edges are allowed.
    assertThat(undirectedGraph.addEdge(N1, N1, E11)).isTrue();
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
    MutableNetwork<Integer, String> directedGraph = NetworkBuilder.directed()
        .expectedNodeCount(NODE_COUNT)
        .build();
    assertThat(directedGraph.addEdge(N1, N2, E12)).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_expectedNodeCount() {
    MutableNetwork<Integer, String> undirectedGraph = NetworkBuilder.undirected()
        .expectedNodeCount(NODE_COUNT)
        .build();
    assertThat(undirectedGraph.addEdge(N1, N2, E12)).isTrue();
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
    assertThat(directedGraph.addEdge(N1, N2, E12)).isTrue();
    assertThat(directedGraph.edgesConnecting(N1, N2)).isEqualTo(ImmutableSet.of(E12));
    assertThat(directedGraph.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void createUndirected_expectedEdgeCount() {
    MutableNetwork<Integer, String> undirectedGraph = NetworkBuilder.undirected()
        .expectedEdgeCount(EDGE_COUNT)
        .build();
    assertThat(undirectedGraph.addEdge(N1, N2, E12)).isTrue();
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
      directedGraph.addEdge(N1, N1, E11);
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
      undirectedGraph.addEdge(N1, N1, E11);
      fail(ERROR_ADDED_SELF_LOOP);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(ERROR_SELF_LOOP);
    }
  }

  private static <N> void checkTransitiveClosure(
      BasicGraph<N> originalGraph, BasicGraph<N> expectedClosure) {
    for (N node : originalGraph.nodes()) {
      assertThat(reachableNodes(originalGraph, node)).isEqualTo(expectedClosure.successors(node));
    }
    assertThat(transitiveClosure(originalGraph)).isEqualTo(expectedClosure);
  }

  private static MutableBasicGraph<Integer> buildDirectedBasicGraph() {
    MutableBasicGraph<Integer> directedGraph =
        BasicGraphBuilder.directed().allowsSelfLoops(true).build();
    directedGraph.putEdge(N1, N1);
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N2, N1);

    return directedGraph;
  }

  private static MutableBasicGraph<Integer> buildUndirectedBasicGraph() {
    MutableBasicGraph<Integer> undirectedGraph =
        BasicGraphBuilder.undirected().allowsSelfLoops(true).build();
    undirectedGraph.putEdge(N1, N1);
    undirectedGraph.putEdge(N1, N2);
    undirectedGraph.putEdge(N2, N1);

    return undirectedGraph;
  }

  private static MutableGraph<Integer, String> buildDirectedGraph() {
    MutableGraph<Integer, String> directedGraph =
        GraphBuilder.directed().allowsSelfLoops(true).build();
    directedGraph.putEdgeValue(N1, N1, E11);
    directedGraph.putEdgeValue(N1, N2, E12);
    directedGraph.putEdgeValue(N2, N1, E21);

    return directedGraph;
  }

  private static MutableGraph<Integer, String> buildUndirectedGraph() {
    MutableGraph<Integer, String> undirectedGraph =
        GraphBuilder.undirected().allowsSelfLoops(true).build();
    undirectedGraph.putEdgeValue(N1, N1, E11);
    undirectedGraph.putEdgeValue(N1, N2, E12);
    undirectedGraph.putEdgeValue(N2, N1, E21); // overwrites E12

    return undirectedGraph;
  }

  private static MutableNetwork<Integer, String> buildDirectedNetwork() {
    MutableNetwork<Integer, String> directedGraph =
        NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();
    directedGraph.addEdge(N1, N1, E11);
    directedGraph.addEdge(N1, N2, E12);
    directedGraph.addEdge(N1, N1, E11_A);
    directedGraph.addEdge(N1, N2, E12_A);
    directedGraph.addEdge(N2, N1, E21);

    return directedGraph;
  }

  private static MutableNetwork<Integer, String> buildUndirectedNetwork() {
    MutableNetwork<Integer, String> undirectedGraph =
        NetworkBuilder.undirected().allowsSelfLoops(true).allowsParallelEdges(true).build();
    undirectedGraph.addEdge(N1, N1, E11);
    undirectedGraph.addEdge(N1, N2, E12);
    undirectedGraph.addEdge(N1, N1, E11_A);
    undirectedGraph.addEdge(N1, N2, E12_A);
    undirectedGraph.addEdge(N2, N1, E21);

    return undirectedGraph;
  }
}
