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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link Endpoints} and {@link Graph#edges()}.
 */
@RunWith(JUnit4.class)
public final class EndpointsTest {
  private static final Integer N0 = 0;
  private static final Integer N1 = 1;
  private static final Integer N2 = 2;
  private static final Integer N3 = 3;
  private static final Integer N4 = 4;
  private static final String E12 = "1-2";
  private static final String E12_A = "1-2a";
  private static final String E21 = "2-1";
  private static final String E13 = "1-3";
  private static final String E44 = "4-4";

  // Test for Endpoints class

  @Test
  public void testDirectedEndpoints() {
    Endpoints<String> directed = Endpoints.ofDirected("source", "target");
    assertThat(directed.source()).isEqualTo("source");
    assertThat(directed.target()).isEqualTo("target");
    assertThat(directed.nodeA()).isEqualTo("source");
    assertThat(directed.nodeB()).isEqualTo("target");
    assertThat(directed.adjacentNode("source")).isEqualTo("target");
    assertThat(directed.adjacentNode("target")).isEqualTo("source");
    assertThat(directed.toString()).isEqualTo("<source -> target>");
  }

  @Test
  public void testUndirectedEndpoints() {
    Endpoints<String> undirected = Endpoints.ofUndirected("chicken", "egg");
    assertThat(ImmutableSet.of(undirected.nodeA(), undirected.nodeB()))
        .containsExactly("chicken", "egg");
    assertThat(undirected.adjacentNode(undirected.nodeA())).isEqualTo(undirected.nodeB());
    assertThat(undirected.adjacentNode(undirected.nodeB())).isEqualTo(undirected.nodeA());
    assertThat(undirected.toString()).contains("chicken");
    assertThat(undirected.toString()).contains("egg");
  }

  @Test
  public void testSelfLoop() {
    Endpoints<String> undirected = Endpoints.ofUndirected("node", "node");
    assertThat(undirected.nodeA()).isEqualTo("node");
    assertThat(undirected.nodeB()).isEqualTo("node");
    assertThat(undirected.adjacentNode("node")).isEqualTo("node");
    assertThat(undirected.toString()).isEqualTo("[node, node]");
  }

  @Test
  public void testAdjacentNode_nodeNotIncident() {
    List<MutableNetwork<Integer, String>> testGraphs = ImmutableList.of(
        NetworkBuilder.directed().<Integer, String>build(),
        NetworkBuilder.undirected().<Integer, String>build());
    for (MutableNetwork<Integer, String> graph : testGraphs) {
      graph.addEdge(1, 2, "1-2");
      Endpoints<Integer> endpoints = graph.incidentNodes("1-2");
      try {
        endpoints.adjacentNode(3);
        fail("Should have rejected adjacentNode() called with a node not incident to edge.");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @Test
  public void testEquals() {
    Endpoints<String> directed = Endpoints.ofDirected("a", "b");
    Endpoints<String> directedMirror = Endpoints.ofDirected("b", "a");
    Endpoints<String> undirected = Endpoints.ofUndirected("a", "b");
    Endpoints<String> undirectedMirror = Endpoints.ofUndirected("b", "a");

    new EqualsTester()
        .addEqualityGroup(directed)
        .addEqualityGroup(directedMirror)
        .addEqualityGroup(undirected, undirectedMirror)
        .testEquals();
  }

  // Tests for Graph.edges() and Network.asGraph().edges() methods
  // TODO(user): Move these to a more appropiate location in the test suite.

  @Test
  public void endpoints_directedGraph() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    directedGraph.addNode(N0);
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N2, N1);
    directedGraph.putEdge(N1, N3);
    directedGraph.putEdge(N4, N4);
    containsExactlySanityCheck(
        directedGraph.edges(),
        Endpoints.ofDirected(N1, N2),
        Endpoints.ofDirected(N2, N1),
        Endpoints.ofDirected(N1, N3),
        Endpoints.ofDirected(N4, N4));
  }

  @Test
  public void endpoints_undirectedGraph() {
    MutableGraph<Integer> undirectedGraph = GraphBuilder.undirected().build();
    undirectedGraph.addNode(N0);
    undirectedGraph.putEdge(N1, N2);
    undirectedGraph.putEdge(N2, N1); // does nothing
    undirectedGraph.putEdge(N1, N3);
    undirectedGraph.putEdge(N4, N4);
    containsExactlySanityCheck(
        undirectedGraph.edges(),
        Endpoints.ofUndirected(N1, N2),
        Endpoints.ofUndirected(N1, N3),
        Endpoints.ofUndirected(N4, N4));
  }

  @Test
  public void endpoints_directedNetwork() {
    MutableNetwork<Integer, String> directedNetwork = NetworkBuilder.directed().build();
    directedNetwork.addNode(N0);
    directedNetwork.addEdge(N1, N2, E12);
    directedNetwork.addEdge(N2, N1, E21);
    directedNetwork.addEdge(N1, N3, E13);
    directedNetwork.addEdge(N4, N4, E44);
    containsExactlySanityCheck(
        directedNetwork.asGraph().edges(),
        Endpoints.ofDirected(N1, N2),
        Endpoints.ofDirected(N2, N1),
        Endpoints.ofDirected(N1, N3),
        Endpoints.ofDirected(N4, N4));
  }

  @Test
  public void endpoints_undirectedNetwork() {
    MutableNetwork<Integer, String> undirectedNetwork =
        NetworkBuilder.undirected().allowsParallelEdges(true).build();
    undirectedNetwork.addNode(N0);
    undirectedNetwork.addEdge(N1, N2, E12);
    undirectedNetwork.addEdge(N2, N1, E12_A); // adds parallel edge, won't be in Graph edges
    undirectedNetwork.addEdge(N1, N3, E13);
    undirectedNetwork.addEdge(N4, N4, E44);
    containsExactlySanityCheck(
        undirectedNetwork.asGraph().edges(),
        Endpoints.ofUndirected(N1, N2),
        Endpoints.ofUndirected(N1, N3),
        Endpoints.ofUndirected(N4, N4));
  }

  @Test
  public void endpoints_unmodifiableView() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    Collection<Endpoints<Integer>> endpoints = directedGraph.edges();

    directedGraph.putEdge(N1, N2);
    containsExactlySanityCheck(endpoints, Endpoints.ofDirected(N1, N2));

    directedGraph.putEdge(N2, N1);
    containsExactlySanityCheck(
        endpoints,
        Endpoints.ofDirected(N1, N2),
        Endpoints.ofDirected(N2, N1));

    directedGraph.removeEdge(N1, N2);
    directedGraph.removeEdge(N2, N1);
    containsExactlySanityCheck(endpoints);

    try {
      endpoints.add(Endpoints.ofDirected(N1, N2));
      fail("Collection returned by endpoints() should be unmodifiable");
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Test
  public void endpoints_containment() {
    MutableGraph<Integer> undirectedGraph = GraphBuilder.undirected().build();
    undirectedGraph.putEdge(N1, N1);
    undirectedGraph.putEdge(N1, N2);
    Collection<Endpoints<Integer>> endpoints = undirectedGraph.edges();

    assertThat(endpoints).hasSize(2);
    assertThat(endpoints).contains(Endpoints.ofUndirected(N1, N1));
    assertThat(endpoints).contains(Endpoints.ofUndirected(N1, N2));
    assertThat(endpoints).contains(Endpoints.ofUndirected(N2, N1)); // equal to ofUndirected(N1, N2)

    assertThat(endpoints).doesNotContain(Endpoints.ofUndirected(N2, N2));
    assertThat(endpoints).doesNotContain(Endpoints.ofDirected(N1, N2)); // graph not directed
    assertThat(endpoints).doesNotContain(Endpoints.ofUndirected(N3, N4)); // nodes not in graph
  }

  private static void containsExactlySanityCheck(Collection<?> collection, Object... varargs) {
    assertThat(collection).hasSize(varargs.length);
    for (Object obj : varargs) {
      assertThat(collection).contains(obj);
    }
    assertThat(collection).containsExactly(varargs);
  }
}
