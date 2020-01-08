/*
 * Copyright (C) 2016 The Guava Authors
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

import static com.google.common.graph.GraphConstants.ENDPOINTS_MISMATCH;
import static com.google.common.graph.TestUtil.assertStronglyEquivalent;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ConfigurableMutableValueGraph} and related functionality. */
// TODO(user): Expand coverage and move to proper test suite.
@RunWith(JUnit4.class)
public final class ValueGraphTest {
  private static final String DEFAULT = "default";

  MutableValueGraph<Integer, String> graph;

  @After
  public void validateGraphState() {
    assertStronglyEquivalent(graph, Graphs.copyOf(graph));
    assertStronglyEquivalent(graph, ImmutableValueGraph.copyOf(graph));

    Graph<Integer> asGraph = graph.asGraph();
    AbstractGraphTest.validateGraph(asGraph);
    assertThat(graph.nodes()).isEqualTo(asGraph.nodes());
    assertThat(graph.edges()).isEqualTo(asGraph.edges());
    assertThat(graph.nodeOrder()).isEqualTo(asGraph.nodeOrder());
    assertThat(graph.incidentEdgeOrder()).isEqualTo(asGraph.incidentEdgeOrder());
    assertThat(graph.isDirected()).isEqualTo(asGraph.isDirected());
    assertThat(graph.allowsSelfLoops()).isEqualTo(asGraph.allowsSelfLoops());

    for (Integer node : graph.nodes()) {
      assertThat(graph.adjacentNodes(node)).isEqualTo(asGraph.adjacentNodes(node));
      assertThat(graph.predecessors(node)).isEqualTo(asGraph.predecessors(node));
      assertThat(graph.successors(node)).isEqualTo(asGraph.successors(node));
      assertThat(graph.degree(node)).isEqualTo(asGraph.degree(node));
      assertThat(graph.inDegree(node)).isEqualTo(asGraph.inDegree(node));
      assertThat(graph.outDegree(node)).isEqualTo(asGraph.outDegree(node));

      for (Integer otherNode : graph.nodes()) {
        boolean hasEdge = graph.hasEdgeConnecting(node, otherNode);
        assertThat(hasEdge).isEqualTo(asGraph.hasEdgeConnecting(node, otherNode));
        assertThat(graph.edgeValueOrDefault(node, otherNode, null) != null).isEqualTo(hasEdge);
        assertThat(!graph.edgeValueOrDefault(node, otherNode, DEFAULT).equals(DEFAULT))
            .isEqualTo(hasEdge);
      }
    }
  }

  @Test
  public void directedGraph() {
    graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB");
    graph.putEdgeValue(2, 3, "valueC");
    graph.putEdgeValue(4, 4, "valueD");

    assertThat(graph.edgeValueOrDefault(1, 2, null)).isEqualTo("valueA");
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(2, 3, null)).isEqualTo("valueC");
    assertThat(graph.edgeValueOrDefault(4, 4, null)).isEqualTo("valueD");
    assertThat(graph.edgeValueOrDefault(1, 2, DEFAULT)).isEqualTo("valueA");
    assertThat(graph.edgeValueOrDefault(2, 1, DEFAULT)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(2, 3, DEFAULT)).isEqualTo("valueC");
    assertThat(graph.edgeValueOrDefault(4, 4, DEFAULT)).isEqualTo("valueD");

    String toString = graph.toString();
    assertThat(toString).contains("valueA");
    assertThat(toString).contains("valueB");
    assertThat(toString).contains("valueC");
    assertThat(toString).contains("valueD");
  }

  @Test
  public void undirectedGraph() {
    graph = ValueGraphBuilder.undirected().allowsSelfLoops(true).build();
    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB"); // overwrites valueA in undirected case
    graph.putEdgeValue(2, 3, "valueC");
    graph.putEdgeValue(4, 4, "valueD");

    assertThat(graph.edgeValueOrDefault(1, 2, null)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(2, 3, null)).isEqualTo("valueC");
    assertThat(graph.edgeValueOrDefault(4, 4, null)).isEqualTo("valueD");
    assertThat(graph.edgeValueOrDefault(1, 2, DEFAULT)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(2, 1, DEFAULT)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(2, 3, DEFAULT)).isEqualTo("valueC");
    assertThat(graph.edgeValueOrDefault(4, 4, DEFAULT)).isEqualTo("valueD");

    String toString = graph.toString();
    assertThat(toString).doesNotContain("valueA");
    assertThat(toString).contains("valueB");
    assertThat(toString).contains("valueC");
    assertThat(toString).contains("valueD");
  }

  @Test
  public void incidentEdgeOrder_unordered() {
    graph = ValueGraphBuilder.directed().incidentEdgeOrder(ElementOrder.unordered()).build();
    assertThat(graph.incidentEdgeOrder()).isEqualTo(ElementOrder.unordered());
  }

  @Test
  public void incidentEdgeOrder_stable() {
    graph = ValueGraphBuilder.directed().incidentEdgeOrder(ElementOrder.stable()).build();
    assertThat(graph.incidentEdgeOrder()).isEqualTo(ElementOrder.stable());
  }

  @Test
  public void hasEdgeConnecting_directed_correct() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(1, 2))).isTrue();
  }

  @Test
  public void hasEdgeConnecting_directed_backwards() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(2, 1))).isFalse();
  }

  @Test
  public void hasEdgeConnecting_directed_mismatch() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(1, 2))).isFalse();
    assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(2, 1))).isFalse();
  }

  @Test
  public void hasEdgeConnecting_undirected_correct() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(1, 2))).isTrue();
  }

  @Test
  public void hasEdgeConnecting_undirected_backwards() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(2, 1))).isTrue();
  }

  @Test
  public void hasEdgeConnecting_undirected_mismatch() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(1, 2))).isTrue();
    assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(2, 1))).isTrue();
  }

  @Test
  public void edgeValue_directed_correct() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValue(EndpointPair.ordered(1, 2))).hasValue("A");
  }

  @Test
  public void edgeValue_directed_backwards() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValue(EndpointPair.ordered(2, 1))).isEmpty();
  }

  @Test
  public void edgeValue_directed_mismatch() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    try {
      Optional<String> unused = graph.edgeValue(EndpointPair.unordered(1, 2));
      unused = graph.edgeValue(EndpointPair.unordered(2, 1));
      fail("Expected IllegalArgumentException: " + ENDPOINTS_MISMATCH);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
    }
  }

  @Test
  public void edgeValue_undirected_correct() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValue(EndpointPair.unordered(1, 2))).hasValue("A");
  }

  @Test
  public void edgeValue_undirected_backwards() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValue(EndpointPair.unordered(2, 1))).hasValue("A");
  }

  @Test
  public void edgeValue_undirected_mismatch() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValue(EndpointPair.ordered(1, 2))).hasValue("A");
    assertThat(graph.edgeValue(EndpointPair.ordered(2, 1))).hasValue("A");
  }

  @Test
  public void edgeValueOrDefault_directed_correct() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValueOrDefault(EndpointPair.ordered(1, 2), "default")).isEqualTo("A");
  }

  @Test
  public void edgeValueOrDefault_directed_backwards() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValueOrDefault(EndpointPair.ordered(2, 1), "default"))
        .isEqualTo("default");
  }

  @Test
  public void edgeValueOrDefault_directed_mismatch() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    try {
      String unused = graph.edgeValueOrDefault(EndpointPair.unordered(1, 2), "default");
      unused = graph.edgeValueOrDefault(EndpointPair.unordered(2, 1), "default");
      fail("Expected IllegalArgumentException: " + ENDPOINTS_MISMATCH);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
    }
  }

  @Test
  public void edgeValueOrDefault_undirected_correct() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValueOrDefault(EndpointPair.unordered(1, 2), "default")).isEqualTo("A");
  }

  @Test
  public void edgeValueOrDefault_undirected_backwards() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValueOrDefault(EndpointPair.unordered(2, 1), "default")).isEqualTo("A");
  }

  @Test
  public void edgeValueOrDefault_undirected_mismatch() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "A");
    assertThat(graph.edgeValueOrDefault(EndpointPair.ordered(2, 1), "default")).isEqualTo("A");
    assertThat(graph.edgeValueOrDefault(EndpointPair.ordered(2, 1), "default")).isEqualTo("A");
  }

  @Test
  public void putEdgeValue_directed() {
    graph = ValueGraphBuilder.directed().build();

    assertThat(graph.putEdgeValue(1, 2, "valueA")).isNull();
    assertThat(graph.putEdgeValue(2, 1, "valueB")).isNull();
    assertThat(graph.putEdgeValue(1, 2, "valueC")).isEqualTo("valueA");
    assertThat(graph.putEdgeValue(2, 1, "valueD")).isEqualTo("valueB");
  }

  @Test
  public void putEdgeValue_directed_orderMismatch() {
    graph = ValueGraphBuilder.directed().build();
    try {
      graph.putEdgeValue(EndpointPair.unordered(1, 2), "irrelevant");
      fail("Expected IllegalArgumentException: " + ENDPOINTS_MISMATCH);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
    }
  }

  @Test
  public void putEdgeValue_undirected_orderMismatch() {
    graph = ValueGraphBuilder.undirected().build();
    assertThat(graph.putEdgeValue(EndpointPair.ordered(1, 2), "irrelevant")).isNull();
  }

  @Test
  public void putEdgeValue_undirected() {
    graph = ValueGraphBuilder.undirected().build();

    assertThat(graph.putEdgeValue(1, 2, "valueA")).isNull();
    assertThat(graph.putEdgeValue(2, 1, "valueB")).isEqualTo("valueA");
    assertThat(graph.putEdgeValue(1, 2, "valueC")).isEqualTo("valueB");
    assertThat(graph.putEdgeValue(2, 1, "valueD")).isEqualTo("valueC");
  }

  @Test
  public void removeEdge_directed() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB");
    graph.putEdgeValue(2, 3, "valueC");

    assertThat(graph.removeEdge(1, 2)).isEqualTo("valueA");
    assertThat(graph.removeEdge(1, 2)).isNull();
    assertThat(graph.removeEdge(2, 1)).isEqualTo("valueB");
    assertThat(graph.removeEdge(2, 1)).isNull();
    assertThat(graph.removeEdge(2, 3)).isEqualTo("valueC");
    assertThat(graph.removeEdge(2, 3)).isNull();
  }

  @Test
  public void removeEdge_undirected() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB");
    graph.putEdgeValue(2, 3, "valueC");

    assertThat(graph.removeEdge(1, 2)).isEqualTo("valueB");
    assertThat(graph.removeEdge(1, 2)).isNull();
    assertThat(graph.removeEdge(2, 1)).isNull();
    assertThat(graph.removeEdge(2, 3)).isEqualTo("valueC");
    assertThat(graph.removeEdge(2, 3)).isNull();
  }

  @Test
  public void removeEdge_directed_orderMismatch() {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "1->2");
    graph.putEdgeValue(2, 1, "2->1");
    try {
      graph.removeEdge(EndpointPair.unordered(1, 2));
      graph.removeEdge(EndpointPair.unordered(2, 1));
      fail("Expected IllegalArgumentException: " + ENDPOINTS_MISMATCH);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
    }
  }

  @Test
  public void removeEdge_undirected_orderMismatch() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "1-2");
    assertThat(graph.removeEdge(EndpointPair.ordered(1, 2))).isEqualTo("1-2");
  }

  @Test
  public void edgeValue_missing() {
    graph = ValueGraphBuilder.directed().build();

    assertThat(graph.edgeValueOrDefault(1, 2, DEFAULT)).isEqualTo(DEFAULT);
    assertThat(graph.edgeValueOrDefault(2, 1, DEFAULT)).isEqualTo(DEFAULT);
    assertThat(graph.edgeValue(1, 2).orElse(DEFAULT)).isEqualTo(DEFAULT);
    assertThat(graph.edgeValue(2, 1).orElse(DEFAULT)).isEqualTo(DEFAULT);
    assertThat(graph.edgeValueOrDefault(1, 2, null)).isNull();
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isNull();
    assertThat(graph.edgeValue(1, 2).orElse(null)).isNull();
    assertThat(graph.edgeValue(2, 1).orElse(null)).isNull();

    graph.putEdgeValue(1, 2, "valueA");
    graph.putEdgeValue(2, 1, "valueB");
    assertThat(graph.edgeValueOrDefault(1, 2, DEFAULT)).isEqualTo("valueA");
    assertThat(graph.edgeValueOrDefault(2, 1, DEFAULT)).isEqualTo("valueB");
    assertThat(graph.edgeValueOrDefault(1, 2, null)).isEqualTo("valueA");
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isEqualTo("valueB");
    assertThat(graph.edgeValue(1, 2).get()).isEqualTo("valueA");
    assertThat(graph.edgeValue(2, 1).get()).isEqualTo("valueB");

    graph.removeEdge(1, 2);
    graph.putEdgeValue(2, 1, "valueC");
    assertThat(graph.edgeValueOrDefault(1, 2, DEFAULT)).isEqualTo(DEFAULT);
    assertThat(graph.edgeValueOrDefault(2, 1, DEFAULT)).isEqualTo("valueC");
    assertThat(graph.edgeValue(1, 2).orElse(DEFAULT)).isEqualTo(DEFAULT);
    assertThat(graph.edgeValueOrDefault(1, 2, null)).isNull();
    assertThat(graph.edgeValueOrDefault(2, 1, null)).isEqualTo("valueC");
    assertThat(graph.edgeValue(1, 2).orElse(null)).isNull();
    assertThat(graph.edgeValue(2, 1).get()).isEqualTo("valueC");
  }

  @Test
  public void equivalence_considersEdgeValue() {
    graph = ValueGraphBuilder.undirected().build();
    graph.putEdgeValue(1, 2, "valueA");

    MutableValueGraph<Integer, String> otherGraph = ValueGraphBuilder.undirected().build();
    otherGraph.putEdgeValue(1, 2, "valueA");
    assertThat(graph).isEqualTo(otherGraph);

    otherGraph.putEdgeValue(1, 2, "valueB");
    assertThat(graph).isNotEqualTo(otherGraph); // values differ
  }

  @Test
  public void incidentEdges_stableIncidentEdgeOrder_preservesIncidentEdgesOrder_directed() {
    graph = ValueGraphBuilder.directed().incidentEdgeOrder(ElementOrder.stable()).build();
    graph.putEdgeValue(2, 1, "2-1");
    graph.putEdgeValue(2, 3, "2-3");
    graph.putEdgeValue(1, 2, "1-2");

    assertThat(graph.incidentEdges(2))
        .containsExactly(
            EndpointPair.ordered(2, 1), EndpointPair.ordered(2, 3), EndpointPair.ordered(1, 2))
        .inOrder();
  }

  @Test
  public void incidentEdges_stableIncidentEdgeOrder_preservesIncidentEdgesOrder_undirected() {
    graph = ValueGraphBuilder.undirected().incidentEdgeOrder(ElementOrder.stable()).build();
    graph.putEdgeValue(2, 3, "2-3");
    graph.putEdgeValue(2, 1, "2-1");
    graph.putEdgeValue(2, 4, "2-4");
    graph.putEdgeValue(1, 2, "1-2"); // Duplicate nodes, different value

    assertThat(graph.incidentEdges(2))
        .containsExactly(
            EndpointPair.unordered(2, 3),
            EndpointPair.unordered(1, 2),
            EndpointPair.unordered(2, 4))
        .inOrder();
  }

  @Test
  public void concurrentIteration() throws Exception {
    graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "A");
    graph.putEdgeValue(3, 4, "B");
    graph.putEdgeValue(5, 6, "C");

    int threadCount = 20;
    ExecutorService executor = newFixedThreadPool(threadCount);
    final CyclicBarrier barrier = new CyclicBarrier(threadCount);
    ImmutableList.Builder<Future<?>> futures = ImmutableList.builder();
    for (int i = 0; i < threadCount; i++) {
      futures.add(
          executor.submit(
              new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                  barrier.await();
                  Integer first = graph.nodes().iterator().next();
                  for (Integer node : graph.nodes()) {
                    Set<Integer> unused = graph.successors(node);
                  }
                  /*
                   * Also look up an earlier node so that, if the graph is using MapRetrievalCache,
                   * we read one of the fields declared in that class.
                   */
                  Set<Integer> unused = graph.successors(first);
                  return null;
                }
              }));
    }

    // For more about this test, see the equivalent in AbstractNetworkTest.
    for (Future<?> future : futures.build()) {
      future.get();
    }
    executor.shutdown();
  }
}
