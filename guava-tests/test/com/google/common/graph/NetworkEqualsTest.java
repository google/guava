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

import com.google.common.testing.EqualsTester;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@AndroidIncompatible
// TODO(cpovirk): Figure out Android JUnit 4 support. Does it work with Gingerbread? @RunWith?
@RunWith(Parameterized.class)
public final class NetworkEqualsTest {
  private static final Integer N1 = 1;
  private static final Integer N2 = 2;
  private static final Integer N3 = 3;

  private static final String E11 = "1-1";
  private static final String E12 = "1-2";
  private static final String E12_A = "1-2a";
  private static final String E13 = "1-3";

  enum GraphType {
    UNDIRECTED,
    DIRECTED
  }

  private final GraphType graphType;
  private final MutableNetwork<Integer, String> graph;

  // add parameters: directed/undirected
  @Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{GraphType.UNDIRECTED}, {GraphType.DIRECTED}});
  }

  public NetworkEqualsTest(GraphType graphType) {
    this.graphType = graphType;
    this.graph = createGraph(graphType);
  }

  private static MutableNetwork<Integer, String> createGraph(GraphType graphType) {
    switch (graphType) {
      case UNDIRECTED:
        return NetworkBuilder.undirected().allowsSelfLoops(true).build();
      case DIRECTED:
        return NetworkBuilder.directed().allowsSelfLoops(true).build();
      default:
        throw new IllegalStateException("Unexpected graph type: " + graphType);
    }
  }

  private static GraphType oppositeType(GraphType graphType) {
    switch (graphType) {
      case UNDIRECTED:
        return GraphType.DIRECTED;
      case DIRECTED:
        return GraphType.UNDIRECTED;
      default:
        throw new IllegalStateException("Unexpected graph type: " + graphType);
    }
  }

  @Test
  public void equals_nodeSetsDiffer() {
    graph.addNode(N1);

    MutableNetwork<Integer, String> g2 = createGraph(graphType);
    g2.addNode(N2);

    new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
  }

  // Node sets are the same, but edge sets differ.
  @Test
  public void equals_edgeSetsDiffer() {
    graph.addEdge(N1, N2, E12);

    MutableNetwork<Integer, String> g2 = createGraph(graphType);
    g2.addEdge(N1, N2, E13);

    new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
  }

  // Node/edge sets are the same, but node/edge connections differ due to graph type.
  @Test
  public void equals_directedVsUndirected() {
    graph.addEdge(N1, N2, E12);

    MutableNetwork<Integer, String> g2 = createGraph(oppositeType(graphType));
    g2.addEdge(N1, N2, E12);

    new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
  }

  // Node/edge sets and node/edge connections are the same, but directedness differs.
  @Test
  public void equals_selfLoop_directedVsUndirected() {
    graph.addEdge(N1, N1, E11);

    MutableNetwork<Integer, String> g2 = createGraph(oppositeType(graphType));
    g2.addEdge(N1, N1, E11);

    new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
  }

  // Node/edge sets are the same, but node/edge connections differ.
  @Test
  public void equals_connectionsDiffer() {
    graph.addEdge(N1, N2, E12);
    graph.addEdge(N1, N3, E13);

    MutableNetwork<Integer, String> g2 = createGraph(graphType);
    // connect E13 to N1 and N2, and E12 to N1 and N3 => not equal
    g2.addEdge(N1, N2, E13);
    g2.addEdge(N1, N3, E12);

    new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
  }

  // Node/edge sets and node/edge connections are the same, but graph properties differ.
  // (In this case the graphs are considered equal; the property differences are irrelevant.)
  @Test
  public void equals_propertiesDiffer() {
    graph.addEdge(N1, N2, E12);

    MutableNetwork<Integer, String> g2 = NetworkBuilder.from(graph)
        .allowsParallelEdges(!graph.allowsParallelEdges())
        .allowsSelfLoops(!graph.allowsSelfLoops())
        .build();
    g2.addEdge(N1, N2, E12);

    new EqualsTester().addEqualityGroup(graph, g2).testEquals();
  }

  // Node/edge sets and node/edge connections are the same, but edge order differs.
  // (In this case the graphs are considered equal; the edge add orderings are irrelevant.)
  @Test
  public void equals_edgeAddOrdersDiffer() {
    NetworkBuilder<Integer, String> builder = NetworkBuilder.from(graph).allowsParallelEdges(true);
    MutableNetwork<Integer, String> g1 = builder.build();
    MutableNetwork<Integer, String> g2 = builder.build();

    // for ug1, add e12 first, then e12_a
    g1.addEdge(N1, N2, E12);
    g1.addEdge(N1, N2, E12_A);

    // for ug2, add e12_a first, then e12
    g2.addEdge(N1, N2, E12_A);
    g2.addEdge(N1, N2, E12);

    new EqualsTester().addEqualityGroup(g1, g2).testEquals();
  }

  @Test
  public void equals_edgeDirectionsDiffer() {
    graph.addEdge(N1, N2, E12);

    MutableNetwork<Integer, String> g2 = createGraph(graphType);
    g2.addEdge(N2, N1, E12);

    switch (graphType) {
      case UNDIRECTED:
        new EqualsTester().addEqualityGroup(graph, g2).testEquals();
        break;
      case DIRECTED:
        new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
        break;
      default:
        throw new IllegalStateException("Unexpected graph type: " + graphType);
    }
  }
}
