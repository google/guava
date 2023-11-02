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
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.util.Collection;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link EndpointPair} and {@link Graph#edges()}. */
@RunWith(JUnit4.class)
public final class EndpointPairTest {
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

  // Test for EndpointPair class

  @Test
  public void testOrderedEndpointPair() {
    EndpointPair<String> ordered = EndpointPair.ordered("source", "target");
    assertThat(ordered.isOrdered()).isTrue();
    assertThat(ordered).containsExactly("source", "target").inOrder();
    assertThat(ordered.source()).isEqualTo("source");
    assertThat(ordered.target()).isEqualTo("target");
    assertThat(ordered.nodeU()).isEqualTo("source");
    assertThat(ordered.nodeV()).isEqualTo("target");
    assertThat(ordered.adjacentNode("source")).isEqualTo("target");
    assertThat(ordered.adjacentNode("target")).isEqualTo("source");
    assertThat(ordered.toString()).isEqualTo("<source -> target>");
  }

  @Test
  public void testUnorderedEndpointPair() {
    EndpointPair<String> unordered = EndpointPair.unordered("chicken", "egg");
    assertThat(unordered.isOrdered()).isFalse();
    assertThat(unordered).containsExactly("chicken", "egg");
    assertThat(ImmutableSet.of(unordered.nodeU(), unordered.nodeV()))
        .containsExactly("chicken", "egg");
    assertThat(unordered.adjacentNode(unordered.nodeU())).isEqualTo(unordered.nodeV());
    assertThat(unordered.adjacentNode(unordered.nodeV())).isEqualTo(unordered.nodeU());
    assertThat(unordered.toString()).contains("chicken");
    assertThat(unordered.toString()).contains("egg");
  }

  @Test
  public void testSelfLoop() {
    EndpointPair<String> unordered = EndpointPair.unordered("node", "node");
    assertThat(unordered.isOrdered()).isFalse();
    assertThat(unordered).containsExactly("node", "node");
    assertThat(unordered.nodeU()).isEqualTo("node");
    assertThat(unordered.nodeV()).isEqualTo("node");
    assertThat(unordered.adjacentNode("node")).isEqualTo("node");
    assertThat(unordered.toString()).isEqualTo("[node, node]");
  }

  @Test
  public void testAdjacentNode_nodeNotIncident() {
    ImmutableList<MutableNetwork<Integer, String>> testNetworks =
        ImmutableList.of(
            NetworkBuilder.directed().<Integer, String>build(),
            NetworkBuilder.undirected().<Integer, String>build());
    for (MutableNetwork<Integer, String> network : testNetworks) {
      network.addEdge(1, 2, "1-2");
      EndpointPair<Integer> endpointPair = network.incidentNodes("1-2");
      assertThrows(IllegalArgumentException.class, () -> endpointPair.adjacentNode(3));
    }
  }

  @Test
  public void testEquals() {
    EndpointPair<String> ordered = EndpointPair.ordered("a", "b");
    EndpointPair<String> orderedMirror = EndpointPair.ordered("b", "a");
    EndpointPair<String> unordered = EndpointPair.unordered("a", "b");
    EndpointPair<String> unorderedMirror = EndpointPair.unordered("b", "a");

    new EqualsTester()
        .addEqualityGroup(ordered)
        .addEqualityGroup(orderedMirror)
        .addEqualityGroup(unordered, unorderedMirror)
        .testEquals();
  }

  // Tests for Graph.edges() and Network.asGraph().edges() methods
  // TODO(user): Move these to a more appropriate location in the test suite.

  @Test
  public void endpointPair_directedGraph() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().allowsSelfLoops(true).build();
    directedGraph.addNode(N0);
    directedGraph.putEdge(N1, N2);
    directedGraph.putEdge(N2, N1);
    directedGraph.putEdge(N1, N3);
    directedGraph.putEdge(N4, N4);
    containsExactlySanityCheck(
        directedGraph.edges(),
        EndpointPair.ordered(N1, N2),
        EndpointPair.ordered(N2, N1),
        EndpointPair.ordered(N1, N3),
        EndpointPair.ordered(N4, N4));
  }

  @Test
  public void endpointPair_undirectedGraph() {
    MutableGraph<Integer> undirectedGraph = GraphBuilder.undirected().allowsSelfLoops(true).build();
    undirectedGraph.addNode(N0);
    undirectedGraph.putEdge(N1, N2);
    undirectedGraph.putEdge(N2, N1); // does nothing
    undirectedGraph.putEdge(N1, N3);
    undirectedGraph.putEdge(N4, N4);
    containsExactlySanityCheck(
        undirectedGraph.edges(),
        EndpointPair.unordered(N1, N2),
        EndpointPair.unordered(N1, N3),
        EndpointPair.unordered(N4, N4));
  }

  @Test
  public void endpointPair_directedNetwork() {
    MutableNetwork<Integer, String> directedNetwork =
        NetworkBuilder.directed().allowsSelfLoops(true).build();
    directedNetwork.addNode(N0);
    directedNetwork.addEdge(N1, N2, E12);
    directedNetwork.addEdge(N2, N1, E21);
    directedNetwork.addEdge(N1, N3, E13);
    directedNetwork.addEdge(N4, N4, E44);
    containsExactlySanityCheck(
        directedNetwork.asGraph().edges(),
        EndpointPair.ordered(N1, N2),
        EndpointPair.ordered(N2, N1),
        EndpointPair.ordered(N1, N3),
        EndpointPair.ordered(N4, N4));
  }

  @Test
  public void endpointPair_undirectedNetwork() {
    MutableNetwork<Integer, String> undirectedNetwork =
        NetworkBuilder.undirected().allowsParallelEdges(true).allowsSelfLoops(true).build();
    undirectedNetwork.addNode(N0);
    undirectedNetwork.addEdge(N1, N2, E12);
    undirectedNetwork.addEdge(N2, N1, E12_A); // adds parallel edge, won't be in Graph edges
    undirectedNetwork.addEdge(N1, N3, E13);
    undirectedNetwork.addEdge(N4, N4, E44);
    containsExactlySanityCheck(
        undirectedNetwork.asGraph().edges(),
        EndpointPair.unordered(N1, N2),
        EndpointPair.unordered(N1, N3),
        EndpointPair.unordered(N4, N4));
  }

  @Test
  public void endpointPair_unmodifiableView() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().build();
    Set<EndpointPair<Integer>> edges = directedGraph.edges();

    directedGraph.putEdge(N1, N2);
    containsExactlySanityCheck(edges, EndpointPair.ordered(N1, N2));

    directedGraph.putEdge(N2, N1);
    containsExactlySanityCheck(edges, EndpointPair.ordered(N1, N2), EndpointPair.ordered(N2, N1));

    directedGraph.removeEdge(N1, N2);
    directedGraph.removeEdge(N2, N1);
    containsExactlySanityCheck(edges);

    assertThrows(
        UnsupportedOperationException.class, () -> edges.add(EndpointPair.ordered(N1, N2)));
  }

  @Test
  public void endpointPair_undirected_contains() {
    MutableGraph<Integer> undirectedGraph = GraphBuilder.undirected().allowsSelfLoops(true).build();
    undirectedGraph.putEdge(N1, N1);
    undirectedGraph.putEdge(N1, N2);
    Set<EndpointPair<Integer>> edges = undirectedGraph.edges();

    assertThat(edges).hasSize(2);
    assertThat(edges).contains(EndpointPair.unordered(N1, N1));
    assertThat(edges).contains(EndpointPair.unordered(N1, N2));
    assertThat(edges).contains(EndpointPair.unordered(N2, N1)); // equal to unordered(N1, N2)

    // ordered endpoints not compatible with undirected graph
    assertThat(edges).doesNotContain(EndpointPair.ordered(N1, N2));

    assertThat(edges).doesNotContain(EndpointPair.unordered(N2, N2)); // edge not present
    assertThat(edges).doesNotContain(EndpointPair.unordered(N3, N4)); // nodes not in graph
  }

  @Test
  public void endpointPair_directed_contains() {
    MutableGraph<Integer> directedGraph = GraphBuilder.directed().allowsSelfLoops(true).build();
    directedGraph.putEdge(N1, N1);
    directedGraph.putEdge(N1, N2);
    Set<EndpointPair<Integer>> edges = directedGraph.edges();

    assertThat(edges).hasSize(2);
    assertThat(edges).contains(EndpointPair.ordered(N1, N1));
    assertThat(edges).contains(EndpointPair.ordered(N1, N2));

    // unordered endpoints not OK for directed graph (undefined behavior)
    assertThat(edges).doesNotContain(EndpointPair.unordered(N1, N2));

    assertThat(edges).doesNotContain(EndpointPair.ordered(N2, N1)); // wrong order
    assertThat(edges).doesNotContain(EndpointPair.ordered(N2, N2)); // edge not present
    assertThat(edges).doesNotContain(EndpointPair.ordered(N3, N4)); // nodes not in graph
  }

  private static void containsExactlySanityCheck(Collection<?> collection, Object... varargs) {
    assertThat(collection).hasSize(varargs.length);
    for (Object obj : varargs) {
      assertThat(collection).contains(obj);
    }
    assertThat(collection).containsExactly(varargs);
  }
}
