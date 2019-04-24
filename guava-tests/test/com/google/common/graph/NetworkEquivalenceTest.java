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

import static com.google.common.graph.TestUtil.EdgeType.DIRECTED;
import static com.google.common.graph.TestUtil.EdgeType.UNDIRECTED;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.graph.TestUtil.EdgeType;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@AndroidIncompatible
// TODO(cpovirk): Figure out Android JUnit 4 support. Does it work with Gingerbread? @RunWith?
@RunWith(Parameterized.class)
public final class NetworkEquivalenceTest {
  private static final Integer N1 = 1;
  private static final Integer N2 = 2;
  private static final Integer N3 = 3;

  private static final String E11 = "1-1";
  private static final String E12 = "1-2";
  private static final String E12_A = "1-2a";
  private static final String E13 = "1-3";

  private final EdgeType edgeType;
  private final MutableNetwork<Integer, String> network;

  // add parameters: directed/undirected
  @Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{EdgeType.UNDIRECTED}, {EdgeType.DIRECTED}});
  }

  public NetworkEquivalenceTest(EdgeType edgeType) {
    this.edgeType = edgeType;
    this.network = createNetwork(edgeType);
  }

  private static MutableNetwork<Integer, String> createNetwork(EdgeType edgeType) {
    switch (edgeType) {
      case UNDIRECTED:
        return NetworkBuilder.undirected().allowsSelfLoops(true).build();
      case DIRECTED:
        return NetworkBuilder.directed().allowsSelfLoops(true).build();
      default:
        throw new IllegalStateException("Unexpected edge type: " + edgeType);
    }
  }

  private static EdgeType oppositeType(EdgeType edgeType) {
    switch (edgeType) {
      case UNDIRECTED:
        return EdgeType.DIRECTED;
      case DIRECTED:
        return EdgeType.UNDIRECTED;
      default:
        throw new IllegalStateException("Unexpected edge type: " + edgeType);
    }
  }

  @Test
  public void equivalent_nodeSetsDiffer() {
    network.addNode(N1);

    MutableNetwork<Integer, String> g2 = createNetwork(edgeType);
    g2.addNode(N2);

    assertThat(network).isNotEqualTo(g2);
  }

  // Node sets are the same, but edge sets differ.
  @Test
  public void equivalent_edgeSetsDiffer() {
    network.addEdge(N1, N2, E12);

    MutableNetwork<Integer, String> g2 = createNetwork(edgeType);
    g2.addEdge(N1, N2, E13);

    assertThat(network).isNotEqualTo(g2);
  }

  // Node/edge sets are the same, but node/edge connections differ due to edge type.
  @Test
  public void equivalent_directedVsUndirected() {
    network.addEdge(N1, N2, E12);

    MutableNetwork<Integer, String> g2 = createNetwork(oppositeType(edgeType));
    g2.addEdge(N1, N2, E12);

    assertThat(network).isNotEqualTo(g2);
  }

  // Node/edge sets and node/edge connections are the same, but directedness differs.
  @Test
  public void equivalent_selfLoop_directedVsUndirected() {
    network.addEdge(N1, N1, E11);

    MutableNetwork<Integer, String> g2 = createNetwork(oppositeType(edgeType));
    g2.addEdge(N1, N1, E11);

    assertThat(network).isNotEqualTo(g2);
  }

  // Node/edge sets are the same, but node/edge connections differ.
  @Test
  public void equivalent_connectionsDiffer() {
    network.addEdge(N1, N2, E12);
    network.addEdge(N1, N3, E13);

    MutableNetwork<Integer, String> g2 = createNetwork(edgeType);
    // connect E13 to N1 and N2, and E12 to N1 and N3 => not equivalent
    g2.addEdge(N1, N2, E13);
    g2.addEdge(N1, N3, E12);

    assertThat(network).isNotEqualTo(g2);
  }

  // Node/edge sets and node/edge connections are the same, but network properties differ.
  // (In this case the networks are considered equivalent; the property differences are irrelevant.)
  @Test
  public void equivalent_propertiesDiffer() {
    network.addEdge(N1, N2, E12);

    MutableNetwork<Integer, String> g2 =
        NetworkBuilder.from(network)
            .allowsParallelEdges(!network.allowsParallelEdges())
            .allowsSelfLoops(!network.allowsSelfLoops())
            .build();
    g2.addEdge(N1, N2, E12);

    assertThat(network).isEqualTo(g2);
  }

  // Node/edge sets and node/edge connections are the same, but edge order differs.
  // (In this case the networks are considered equivalent; the edge add orderings are irrelevant.)
  @Test
  public void equivalent_edgeAddOrdersDiffer() {
    NetworkBuilder<Integer, String> builder =
        NetworkBuilder.from(network).allowsParallelEdges(true);
    MutableNetwork<Integer, String> g1 = builder.build();
    MutableNetwork<Integer, String> g2 = builder.build();

    // for ug1, add e12 first, then e12_a
    g1.addEdge(N1, N2, E12);
    g1.addEdge(N1, N2, E12_A);

    // for ug2, add e12_a first, then e12
    g2.addEdge(N1, N2, E12_A);
    g2.addEdge(N1, N2, E12);

    assertThat(g1).isEqualTo(g2);
  }

  @Test
  public void equivalent_edgeDirectionsDiffer() {
    network.addEdge(N1, N2, E12);

    MutableNetwork<Integer, String> g2 = createNetwork(edgeType);
    g2.addEdge(N2, N1, E12);

    switch (edgeType) {
      case UNDIRECTED:
        assertThat(network).isEqualTo(g2);
        break;
      case DIRECTED:
        assertThat(network).isNotEqualTo(g2);
        break;
      default:
        throw new IllegalStateException("Unexpected edge type: " + edgeType);
    }
  }
}
