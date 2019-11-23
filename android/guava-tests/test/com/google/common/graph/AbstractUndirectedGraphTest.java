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

import com.google.common.testing.EqualsTester;
import org.junit.After;
import org.junit.Test;

/**
 * Abstract base class for testing undirected implementations of the {@link Graph} interface.
 *
 * <p>This class is responsible for testing that an undirected implementation of {@link Graph} is
 * correctly handling undirected edges. Implementation-dependent test cases are left to subclasses.
 * Test cases that do not require the graph to be undirected are found in superclasses.
 */
public abstract class AbstractUndirectedGraphTest extends AbstractGraphTest {

  @After
  public void validateUndirectedEdges() {
    for (Integer node : graph.nodes()) {
      new EqualsTester()
          .addEqualityGroup(
              graph.predecessors(node), graph.successors(node), graph.adjacentNodes(node))
          .testEquals();
    }
  }

  @Test
  public void predecessors_oneEdge() {
    putEdge(N1, N2);
    assertThat(graph.predecessors(N2)).containsExactly(N1);
    assertThat(graph.predecessors(N1)).containsExactly(N2);
  }

  @Test
  public void successors_oneEdge() {
    putEdge(N1, N2);
    assertThat(graph.successors(N1)).containsExactly(N2);
    assertThat(graph.successors(N2)).containsExactly(N1);
  }

  @Test
  public void incidentEdges_oneEdge() {
    putEdge(N1, N2);
    EndpointPair<Integer> expectedEndpoints = EndpointPair.unordered(N1, N2);
    assertThat(graph.incidentEdges(N1)).containsExactly(expectedEndpoints);
    assertThat(graph.incidentEdges(N2)).containsExactly(expectedEndpoints);
  }

  @Test
  public void inDegree_oneEdge() {
    putEdge(N1, N2);
    assertThat(graph.inDegree(N2)).isEqualTo(1);
    assertThat(graph.inDegree(N1)).isEqualTo(1);
  }

  @Test
  public void outDegree_oneEdge() {
    putEdge(N1, N2);
    assertThat(graph.outDegree(N1)).isEqualTo(1);
    assertThat(graph.outDegree(N2)).isEqualTo(1);
  }

  @Test
  public void hasEdgeConnecting_correct() {
    putEdge(N1, N2);
    assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(N1, N2))).isTrue();
    assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(N2, N1))).isTrue();
  }

  @Test
  public void hasEdgeConnecting_mismatch() {
    putEdge(N1, N2);
    assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(N1, N2))).isTrue();
    assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(N2, N1))).isTrue();
  }

  // Element Mutation

  @Test
  public void addEdge_existingNodes() {
    // Adding nodes initially for safety (insulating from possible future
    // modifications to proxy methods)
    addNode(N1);
    addNode(N2);
    assertThat(putEdge(N1, N2)).isTrue();
  }

  @Test
  public void addEdge_existingEdgeBetweenSameNodes() {
    putEdge(N1, N2);
    assertThat(putEdge(N2, N1)).isFalse();
  }

  @Test
  public void removeEdge_antiparallelEdges() {
    putEdge(N1, N2);
    putEdge(N2, N1); // no-op

    assertThat(graph.removeEdge(N1, N2)).isTrue();
    assertThat(graph.adjacentNodes(N1)).isEmpty();
    assertThat(graph.edges()).isEmpty();
    assertThat(graph.removeEdge(N2, N1)).isFalse();
  }
}
