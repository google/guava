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
public final class GraphEqualsTest {
  private static final Integer N1 = 1;
  private static final Integer N2 = 2;
  private static final Integer N3 = 3;

  enum GraphType {
    UNDIRECTED,
    DIRECTED
  }

  private final GraphType graphType;
  private final MutableBasicGraph<Integer> graph;

  // add parameters: directed/undirected
  @Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{GraphType.UNDIRECTED}, {GraphType.DIRECTED}});
  }

  public GraphEqualsTest(GraphType graphType) {
    this.graphType = graphType;
    this.graph = createGraph(graphType);
  }

  private static MutableBasicGraph<Integer> createGraph(GraphType graphType) {
    switch (graphType) {
      case UNDIRECTED:
        return BasicGraphBuilder.undirected().allowsSelfLoops(true).build();
      case DIRECTED:
        return BasicGraphBuilder.directed().allowsSelfLoops(true).build();
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

    MutableBasicGraph<Integer> g2 = createGraph(graphType);
    g2.addNode(N2);

    new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
  }

  // Node/edge sets are the same, but node/edge connections differ due to graph type.
  @Test
  public void equals_directedVsUndirected() {
    graph.putEdge(N1, N2);

    MutableBasicGraph<Integer> g2 = createGraph(oppositeType(graphType));
    g2.putEdge(N1, N2);

    new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
  }

  // Node/edge sets and node/edge connections are the same, but directedness differs.
  @Test
  public void equals_selfLoop_directedVsUndirected() {
    graph.putEdge(N1, N1);

    MutableBasicGraph<Integer> g2 = createGraph(oppositeType(graphType));
    g2.putEdge(N1, N1);

    new EqualsTester().addEqualityGroup(graph).addEqualityGroup(g2).testEquals();
  }

  // Node/edge sets and node/edge connections are the same, but graph properties differ.
  // (In this case the graphs are considered equal; the property differences are irrelevant.)
  @Test
  public void equals_propertiesDiffer() {
    graph.putEdge(N1, N2);

    MutableBasicGraph<Integer> g2 = BasicGraphBuilder.from(graph)
        .allowsSelfLoops(!graph.allowsSelfLoops())
        .build();
    g2.putEdge(N1, N2);

    new EqualsTester().addEqualityGroup(graph, g2).testEquals();
  }

  // Node/edge sets and node/edge connections are the same, but edge order differs.
  // (In this case the graphs are considered equal; the edge add orderings are irrelevant.)
  @Test
  public void equals_edgeAddOrdersDiffer() {
    BasicGraphBuilder<Integer> builder = BasicGraphBuilder.from(graph);
    MutableBasicGraph<Integer> g1 = builder.build();
    MutableBasicGraph<Integer> g2 = builder.build();

    // for g1, add 1->2 first, then 3->1
    g1.putEdge(N1, N2);
    g1.putEdge(N3, N1);

    // for g2, add 3->1 first, then 1->2
    g2.putEdge(N3, N1);
    g2.putEdge(N1, N2);

    new EqualsTester().addEqualityGroup(g1, g2).testEquals();
  }

  @Test
  public void equals_edgeDirectionsDiffer() {
    graph.putEdge(N1, N2);

    MutableBasicGraph<Integer> g2 = createGraph(graphType);
    g2.putEdge(N2, N1);

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
