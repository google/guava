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

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@AndroidIncompatible
// TODO(cpovirk): Figure out Android JUnit 4 support. Does it work with Gingerbread? @RunWith?
@RunWith(Parameterized.class)
public final class GraphEquivalenceTest {
  private static final Integer N1 = 1;
  private static final Integer N2 = 2;
  private static final Integer N3 = 3;

  enum GraphType {
    UNDIRECTED,
    DIRECTED
  }

  private final GraphType graphType;
  private final MutableGraph<Integer> graph;

  // add parameters: directed/undirected
  @Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{GraphType.UNDIRECTED}, {GraphType.DIRECTED}});
  }

  public GraphEquivalenceTest(GraphType graphType) {
    this.graphType = graphType;
    this.graph = createGraph(graphType);
  }

  private static MutableGraph<Integer> createGraph(GraphType graphType) {
    switch (graphType) {
      case UNDIRECTED:
        return GraphBuilder.undirected().allowsSelfLoops(true).build();
      case DIRECTED:
        return GraphBuilder.directed().allowsSelfLoops(true).build();
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
  public void equivalent_nodeSetsDiffer() {
    graph.addNode(N1);

    MutableGraph<Integer> g2 = createGraph(graphType);
    g2.addNode(N2);

    assertThat(Graphs.equivalent(graph, g2)).isFalse();
  }

  // Node/edge sets are the same, but node/edge connections differ due to graph type.
  @Test
  public void equivalent_directedVsUndirected() {
    graph.putEdge(N1, N2);

    MutableGraph<Integer> g2 = createGraph(oppositeType(graphType));
    g2.putEdge(N1, N2);

    assertThat(Graphs.equivalent(graph, g2)).isFalse();
  }

  // Node/edge sets and node/edge connections are the same, but directedness differs.
  @Test
  public void equivalent_selfLoop_directedVsUndirected() {
    graph.putEdge(N1, N1);

    MutableGraph<Integer> g2 = createGraph(oppositeType(graphType));
    g2.putEdge(N1, N1);

    assertThat(Graphs.equivalent(graph, g2)).isFalse();
  }

  // Node/edge sets and node/edge connections are the same, but graph properties differ.
  // In this case the graphs are considered equivalent; the property differences are irrelevant.
  @Test
  public void equivalent_propertiesDiffer() {
    graph.putEdge(N1, N2);

    MutableGraph<Integer> g2 = GraphBuilder.from(graph)
        .allowsSelfLoops(!graph.allowsSelfLoops())
        .build();
    g2.putEdge(N1, N2);

    assertThat(Graphs.equivalent(graph, g2)).isTrue();
  }

  // Node/edge sets and node/edge connections are the same, but edge order differs.
  // In this case the graphs are considered equivalent; the edge add orderings are irrelevant.
  @Test
  public void equivalent_edgeAddOrdersDiffer() {
    GraphBuilder<Integer> builder = GraphBuilder.from(graph);
    MutableGraph<Integer> g1 = builder.build();
    MutableGraph<Integer> g2 = builder.build();

    // for g1, add 1->2 first, then 3->1
    g1.putEdge(N1, N2);
    g1.putEdge(N3, N1);

    // for g2, add 3->1 first, then 1->2
    g2.putEdge(N3, N1);
    g2.putEdge(N1, N2);

    assertThat(Graphs.equivalent(g1, g2)).isTrue();
  }

  @Test
  public void equivalent_edgeDirectionsDiffer() {
    graph.putEdge(N1, N2);

    MutableGraph<Integer> g2 = createGraph(graphType);
    g2.putEdge(N2, N1);

    switch (graphType) {
      case UNDIRECTED:
        assertThat(Graphs.equivalent(graph, g2)).isTrue();
        break;
      case DIRECTED:
        assertThat(Graphs.equivalent(graph, g2)).isFalse();
        break;
      default:
        throw new IllegalStateException("Unexpected graph type: " + graphType);
    }
  }
}
