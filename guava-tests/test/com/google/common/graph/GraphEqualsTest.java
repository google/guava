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

import static com.google.common.graph.Graphs.MULTIGRAPH;

import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

@AndroidIncompatible
// TODO(cpovirk): Figure out Android JUnit 4 support. Does it work with Gingerbread? @RunWith?
@RunWith(Parameterized.class)
public final class GraphEqualsTest {
  private static final Integer N1 = 1;
  private static final Integer N2 = 2;
  private static final Integer N3 = 3;

  private static final String E12 = "1-2";
  private static final String E12_A = "1-2a";
  private static final String E13 = "1-3";

  enum GraphType {
    UNDIRECTED,
    DIRECTED,
    HYPER // not yet used because we don't yet have a Hypergraph implementation
  }

  private final GraphType graphType;
  private final Graph<Integer, String> graph;

  // add parameters: directed/undirected
  @Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{GraphType.UNDIRECTED}, {GraphType.DIRECTED}});
  }

  public GraphEqualsTest(GraphType graphType) {
    this.graphType = graphType;
    this.graph = createGraph();
  }

  private Graph<Integer, String> createGraph() {
    return createGraph(Graphs.config());
  }

  private Graph<Integer, String> createGraph(GraphConfig config) {
    switch (graphType) {
      case UNDIRECTED:
        return Graphs.createUndirected(config);
      case DIRECTED:
        return Graphs.createDirected(config);
      default:
        throw new IllegalStateException("Unexpected graph type: " + graphType);
    }
  }

  @Test
  public void equals_nodeSetsDiffer() {
    graph.addNode(N1);

    Graph<Integer, String> g2 = createGraph();
    g2.addNode(N2);

    new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
  }

  // Node sets are the same, but edge sets differ.
  @Test
  public void equals_edgeSetsDiffer() {
    graph.addEdge(E12, N1, N2);

    Graph<Integer, String> g2 = createGraph();
    g2.addEdge(E13, N1, N2);

    new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
  }

  // Node/edge sets are the same, but types differ.
  @Test
  public void equals_typesDiffer() {
    graph.addEdge(E12, N1, N2);

    // Whatever graphType specifies, pick another type.
    Graph<Integer, String> g2;
    switch (graphType) {
      case UNDIRECTED:
        g2 = Graphs.createDirected();
        break;
      case DIRECTED:
        g2 = Graphs.createUndirected();
        break;
      default:
        throw new IllegalStateException("Unexpected graph type: " + graphType);
    }

    g2.addEdge(E12, N1, N2);

    new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
  }

  // Node/edge sets and graph type are the same, but node/edge connections differ.
  @Test
  public void equals_connectionsDiffer() {
    graph.addEdge(E12, N1, N2);
    graph.addEdge(E13, N1, N3);

    Graph<Integer, String> g2 = createGraph();
    // connect E13 to N1 and N2, and E12 to N1 and N3 => not equal
    g2.addEdge(E13, N1, N2);
    g2.addEdge(E12, N1, N3);

    new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
  }

  // Node/edge sets, graph type, and node/edge connections are the same, but GraphConfigs differ.
  // (In this case the graphs are considered equal; the config differences are irrelevant.)
  @Test
  public void equals_configsDiffer() {
    graph.addEdge(E12, N1, N2);

    Graph<Integer, String> g2 = createGraph(MULTIGRAPH.noSelfLoops());
    g2.addEdge(E12, N1, N2);

    new EqualsTester().addEqualityGroup(graph, g2).testEquals();
  }

  // Node/edge sets, graph type, and node/edge connections are the same, but edge order differs.
  // (In this case the graphs are considered equal; the edge add orderings are irrelevant.)
  @Test
  public void equals_edgeAddOrdersDiffer() {
    Graph<Integer, String> g1 = createGraph(MULTIGRAPH);
    Graph<Integer, String> g2 = createGraph(MULTIGRAPH);

    // for ug1, add e12 first, then e12_a
    g1.addEdge(E12, N1, N2);
    g1.addEdge(E12_A, N1, N2);

    // for ug2, add e12_a first, then e12
    g2.addEdge(E12_A, N1, N2);
    g2.addEdge(E12, N1, N2);

    new EqualsTester().addEqualityGroup(g1, g2).testEquals();
  }

  @Test
  public void equals_edgeDirectionsDiffer() {
    graph.addEdge(E12, N1, N2);

    Graph<Integer, String> g2 = createGraph();
    g2.addEdge(E12, N2, N1);

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
