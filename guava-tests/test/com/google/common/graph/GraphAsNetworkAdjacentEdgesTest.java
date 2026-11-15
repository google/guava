/*
 * Copyright (C) 2026 The Guava Authors
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
import java.util.Set;
import org.jspecify.annotations.NullUnmarked;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests the adjacent-edge views of graph-to-network adapters. */
@RunWith(Parameterized.class)
@NullUnmarked
public final class GraphAsNetworkAdjacentEdgesTest {
  @Parameters(name = "directed={0}, valueGraph={1}")
  public static ImmutableList<Object[]> parameters() {
    return ImmutableList.of(
        new Object[] {true, false},
        new Object[] {false, false},
        new Object[] {true, true},
        new Object[] {false, true});
  }

  private final boolean directed;
  private final boolean valueGraph;
  private MutableGraph<Integer> graph;
  private MutableValueGraph<Integer, String> valuedGraph;
  private Network<Integer, EndpointPair<Integer>> network;

  public GraphAsNetworkAdjacentEdgesTest(boolean directed, boolean valueGraph) {
    this.directed = directed;
    this.valueGraph = valueGraph;
  }

  @Before
  public void setUp() {
    if (valueGraph) {
      valuedGraph =
          (directed ? ValueGraphBuilder.directed() : ValueGraphBuilder.undirected())
              .allowsSelfLoops(true)
              .build();
      network = valuedGraph.asNetwork();
    } else {
      graph =
          (directed ? GraphBuilder.directed() : GraphBuilder.undirected())
              .allowsSelfLoops(true)
              .build();
      network = graph.asNetwork();
    }
  }

  @Test
  public void adjacentEdges_invalidatedByEdgeRemoval() {
    putEdge(1, 2);
    putEdge(2, 3);
    Set<EndpointPair<Integer>> adjacent = network.adjacentEdges(edge(1, 2));
    assertThat(adjacent).containsExactly(edge(2, 3));

    removeEdge(1, 2);
    assertThat(network.nodes()).containsExactly(1, 2, 3);
    assertInvalidated(adjacent, edge(2, 3));

    putEdge(1, 2);
    // The old view has undefined behavior after the edge is re-added; obtain a new view.
    assertThat(network.adjacentEdges(edge(1, 2))).containsExactly(edge(2, 3));
  }

  @Test
  public void adjacentEdges_invalidatedBySelfLoopRemoval() {
    putEdge(1, 1);
    putEdge(1, 2);
    Set<EndpointPair<Integer>> adjacent = network.adjacentEdges(edge(1, 1));
    assertThat(adjacent).containsExactly(edge(1, 2));

    removeEdge(1, 1);
    assertThat(network.nodes()).containsExactly(1, 2);
    assertInvalidated(adjacent, edge(1, 2));
  }

  @Test
  public void adjacentEdges_liveAndUnmodifiable() {
    putEdge(1, 2);
    Set<EndpointPair<Integer>> adjacent = network.adjacentEdges(edge(1, 2));
    assertThat(adjacent).isEmpty();

    putEdge(2, 3);
    putEdge(4, 1);
    putEdge(4, 5);
    assertThat(adjacent).containsExactly(edge(2, 3), edge(4, 1));
    assertThrows(UnsupportedOperationException.class, () -> adjacent.remove(edge(2, 3)));
    assertThrows(UnsupportedOperationException.class, () -> adjacent.add(edge(4, 5)));

    removeEdge(2, 3);
    assertThat(adjacent).containsExactly(edge(4, 1));
    assertThat(adjacent).doesNotContain(edge(1, 2));
  }

  @Test
  public void adjacentEdges_rejectsAbsentEdgeImmediately() {
    putEdge(1, 3);
    putEdge(2, 4);
    assertThrows(IllegalArgumentException.class, () -> network.adjacentEdges(edge(1, 2)));
  }

  private static void assertInvalidated(
      Set<EndpointPair<Integer>> adjacent, EndpointPair<Integer> formerAdjacentEdge) {
    assertThrows(IllegalStateException.class, () -> adjacent.size());
    assertThrows(IllegalStateException.class, () -> adjacent.isEmpty());
    assertThrows(IllegalStateException.class, () -> adjacent.contains(formerAdjacentEdge));
    assertThrows(IllegalStateException.class, () -> adjacent.iterator());
    assertThrows(IllegalStateException.class, () -> adjacent.toArray());
    assertThrows(IllegalStateException.class, () -> adjacent.toString());
    assertThrows(IllegalStateException.class, () -> adjacent.equals(ImmutableSet.of()));
    assertThat(adjacent.equals(adjacent)).isTrue();
  }

  private EndpointPair<Integer> edge(int nodeU, int nodeV) {
    return directed ? EndpointPair.ordered(nodeU, nodeV) : EndpointPair.unordered(nodeU, nodeV);
  }

  private void putEdge(int nodeU, int nodeV) {
    if (valueGraph) {
      valuedGraph.putEdgeValue(nodeU, nodeV, "value");
    } else {
      graph.putEdge(nodeU, nodeV);
    }
  }

  private void removeEdge(int nodeU, int nodeV) {
    if (valueGraph) {
      valuedGraph.removeEdge(nodeU, nodeV);
    } else {
      graph.removeEdge(nodeU, nodeV);
    }
  }
}
