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

import static com.google.common.graph.GraphConstants.ENDPOINTS_MISMATCH;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Test;

/**
 * Abstract base class for testing undirected {@link Network} implementations defined in this
 * package.
 */
public abstract class AbstractStandardUndirectedNetworkTest extends AbstractNetworkTest {
  private static final EndpointPair<Integer> ENDPOINTS_N1N2 = EndpointPair.ordered(N1, N2);
  private static final EndpointPair<Integer> ENDPOINTS_N2N1 = EndpointPair.ordered(N2, N1);

  @After
  public void validateUndirectedEdges() {
    for (Integer node : network.nodes()) {
      new EqualsTester()
          .addEqualityGroup(
              network.inEdges(node), network.outEdges(node), network.incidentEdges(node))
          .testEquals();
      new EqualsTester()
          .addEqualityGroup(
              network.predecessors(node), network.successors(node), network.adjacentNodes(node))
          .testEquals();

      for (Integer adjacentNode : network.adjacentNodes(node)) {
        assertThat(network.edgesConnecting(node, adjacentNode))
            .containsExactlyElementsIn(network.edgesConnecting(adjacentNode, node));
      }
    }
  }

  @Override
  @Test
  public void nodes_checkReturnedSetMutability() {
    Set<Integer> nodes = network.nodes();
    UnsupportedOperationException e =
        assertThrows(UnsupportedOperationException.class, () -> nodes.add(N2));
    addNode(N1);
    assertThat(network.nodes()).containsExactlyElementsIn(nodes);
  }

  @Override
  @Test
  public void edges_checkReturnedSetMutability() {
    Set<String> edges = network.edges();
    UnsupportedOperationException e =
        assertThrows(UnsupportedOperationException.class, () -> edges.add(E12));
    addEdge(N1, N2, E12);
    assertThat(network.edges()).containsExactlyElementsIn(edges);
  }

  @Override
  @Test
  public void incidentEdges_checkReturnedSetMutability() {
    addNode(N1);
    Set<String> incidentEdges = network.incidentEdges(N1);
    UnsupportedOperationException e =
        assertThrows(UnsupportedOperationException.class, () -> incidentEdges.add(E12));
    addEdge(N1, N2, E12);
    assertThat(network.incidentEdges(N1)).containsExactlyElementsIn(incidentEdges);
  }

  @Override
  @Test
  public void adjacentNodes_checkReturnedSetMutability() {
    addNode(N1);
    Set<Integer> adjacentNodes = network.adjacentNodes(N1);
    UnsupportedOperationException e =
        assertThrows(UnsupportedOperationException.class, () -> adjacentNodes.add(N2));
    addEdge(N1, N2, E12);
    assertThat(network.adjacentNodes(N1)).containsExactlyElementsIn(adjacentNodes);
  }

  @Override
  public void adjacentEdges_checkReturnedSetMutability() {
    addEdge(N1, N2, E12);
    Set<String> adjacentEdges = network.adjacentEdges(E12);
    try {
      adjacentEdges.add(E23);
      fail(ERROR_MODIFIABLE_COLLECTION);
    } catch (UnsupportedOperationException e) {
      addEdge(N2, N3, E23);
      assertThat(network.adjacentEdges(E12)).containsExactlyElementsIn(adjacentEdges);
    }
  }

  @Override
  @Test
  public void edgesConnecting_checkReturnedSetMutability() {
    addNode(N1);
    addNode(N2);
    Set<String> edgesConnecting = network.edgesConnecting(N1, N2);
    UnsupportedOperationException e =
        assertThrows(UnsupportedOperationException.class, () -> edgesConnecting.add(E23));
    addEdge(N1, N2, E12);
    assertThat(network.edgesConnecting(N1, N2)).containsExactlyElementsIn(edgesConnecting);
  }

  @Override
  @Test
  public void inEdges_checkReturnedSetMutability() {
    addNode(N2);
    Set<String> inEdges = network.inEdges(N2);
    UnsupportedOperationException e =
        assertThrows(UnsupportedOperationException.class, () -> inEdges.add(E12));
    addEdge(N1, N2, E12);
    assertThat(network.inEdges(N2)).containsExactlyElementsIn(inEdges);
  }

  @Override
  @Test
  public void outEdges_checkReturnedSetMutability() {
    addNode(N1);
    Set<String> outEdges = network.outEdges(N1);
    UnsupportedOperationException e =
        assertThrows(UnsupportedOperationException.class, () -> outEdges.add(E12));
    addEdge(N1, N2, E12);
    assertThat(network.outEdges(N1)).containsExactlyElementsIn(outEdges);
  }

  @Override
  @Test
  public void predecessors_checkReturnedSetMutability() {
    addNode(N2);
    Set<Integer> predecessors = network.predecessors(N2);
    UnsupportedOperationException e =
        assertThrows(UnsupportedOperationException.class, () -> predecessors.add(N1));
    addEdge(N1, N2, E12);
    assertThat(network.predecessors(N2)).containsExactlyElementsIn(predecessors);
  }

  @Override
  @Test
  public void successors_checkReturnedSetMutability() {
    addNode(N1);
    Set<Integer> successors = network.successors(N1);
    UnsupportedOperationException e =
        assertThrows(UnsupportedOperationException.class, () -> successors.add(N2));
    addEdge(N1, N2, E12);
    assertThat(network.successors(N1)).containsExactlyElementsIn(successors);
  }

  @Test
  public void edges_containsOrderMismatch() {
    addEdge(N1, N2, E12);
    assertThat(network.asGraph().edges()).doesNotContain(ENDPOINTS_N2N1);
    assertThat(network.asGraph().edges()).doesNotContain(ENDPOINTS_N1N2);
  }

  @Test
  public void edgesConnecting_orderMismatch() {
    addEdge(N1, N2, E12);
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              Set<String> unused = network.edgesConnecting(ENDPOINTS_N1N2);
            });
    assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
  }

  @Test
  public void edgeConnecting_orderMismatch() {
    addEdge(N1, N2, E12);
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              Optional<String> unused = network.edgeConnecting(ENDPOINTS_N1N2);
            });
    assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
  }

  @Test
  public void edgeConnectingOrNull_orderMismatch() {
    addEdge(N1, N2, E12);
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              String unused = network.edgeConnectingOrNull(ENDPOINTS_N1N2);
            });
    assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
  }

  @Test
  public void edgesConnecting_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(network.edgesConnecting(N2, N1)).containsExactly(E12);
  }

  @Test
  public void inEdges_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.inEdges(N2)).containsExactly(E12);
    assertThat(network.inEdges(N1)).containsExactly(E12);
  }

  @Test
  public void outEdges_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.outEdges(N2)).containsExactly(E12);
    assertThat(network.outEdges(N1)).containsExactly(E12);
  }

  @Test
  public void predecessors_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.predecessors(N2)).containsExactly(N1);
    assertThat(network.predecessors(N1)).containsExactly(N2);
  }

  @Test
  public void successors_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.successors(N1)).containsExactly(N2);
    assertThat(network.successors(N2)).containsExactly(N1);
  }

  @Test
  public void inDegree_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.inDegree(N2)).isEqualTo(1);
    assertThat(network.inDegree(N1)).isEqualTo(1);
  }

  @Test
  public void outDegree_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.outDegree(N1)).isEqualTo(1);
    assertThat(network.outDegree(N2)).isEqualTo(1);
  }

  @Test
  public void edges_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    assertThat(network.edges()).containsExactly(E11);
  }

  @Test
  public void incidentEdges_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    assertThat(network.incidentEdges(N1)).containsExactly(E11);
  }

  @Test
  public void incidentNodes_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    assertThat(network.incidentNodes(E11).nodeU()).isEqualTo(N1);
    assertThat(network.incidentNodes(E11).nodeV()).isEqualTo(N1);
  }

  @Test
  public void adjacentNodes_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    addEdge(N1, N2, E12);
    assertThat(network.adjacentNodes(N1)).containsExactly(N1, N2);
  }

  @Test
  public void adjacentEdges_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    addEdge(N1, N2, E12);
    assertThat(network.adjacentEdges(E11)).containsExactly(E12);
  }

  @Test
  public void edgesConnecting_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
    addEdge(N1, N2, E12);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(network.edgesConnecting(N2, N1)).containsExactly(E12);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void inEdges_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    assertThat(network.inEdges(N1)).containsExactly(E11);
    addEdge(N1, N2, E12);
    assertThat(network.inEdges(N1)).containsExactly(E11, E12);
  }

  @Test
  public void outEdges_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    assertThat(network.outEdges(N1)).containsExactly(E11);
    addEdge(N2, N1, E12);
    assertThat(network.outEdges(N1)).containsExactly(E11, E12);
  }

  @Test
  public void predecessors_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    assertThat(network.predecessors(N1)).containsExactly(N1);
    addEdge(N1, N2, E12);
    assertThat(network.predecessors(N1)).containsExactly(N1, N2);
  }

  @Test
  public void successors_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    assertThat(network.successors(N1)).containsExactly(N1);
    addEdge(N2, N1, E12);
    assertThat(network.successors(N1)).containsExactly(N1, N2);
  }

  @Test
  public void degree_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    assertThat(network.degree(N1)).isEqualTo(2);
    addEdge(N1, N2, E12);
    assertThat(network.degree(N1)).isEqualTo(3);
  }

  @Test
  public void inDegree_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    assertThat(network.inDegree(N1)).isEqualTo(2);
    addEdge(N1, N2, E12);
    assertThat(network.inDegree(N1)).isEqualTo(3);
  }

  @Test
  public void outDegree_selfLoop() {
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    assertThat(network.outDegree(N1)).isEqualTo(2);
    addEdge(N2, N1, E12);
    assertThat(network.outDegree(N1)).isEqualTo(3);
  }

  // Element Mutation

  @Test
  public void addEdge_existingNodes() {
    assume().that(graphIsMutable()).isTrue();

    // Adding nodes initially for safety (insulating from possible future
    // modifications to proxy methods)
    addNode(N1);
    addNode(N2);
    assertThat(networkAsMutableNetwork.addEdge(N1, N2, E12)).isTrue();
    assertThat(network.edges()).contains(E12);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(network.edgesConnecting(N2, N1)).containsExactly(E12);
  }

  @Test
  public void addEdge_existingEdgeBetweenSameNodes() {
    assume().that(graphIsMutable()).isTrue();

    assertThat(networkAsMutableNetwork.addEdge(N1, N2, E12)).isTrue();
    ImmutableSet<String> edges = ImmutableSet.copyOf(network.edges());
    assertThat(networkAsMutableNetwork.addEdge(N1, N2, E12)).isFalse();
    assertThat(network.edges()).containsExactlyElementsIn(edges);
    assertThat(networkAsMutableNetwork.addEdge(N2, N1, E12)).isFalse();
    assertThat(network.edges()).containsExactlyElementsIn(edges);
  }

  @Test
  public void addEdge_existingEdgeBetweenDifferentNodes() {
    assume().that(graphIsMutable()).isTrue();

    addEdge(N1, N2, E12);
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> networkAsMutableNetwork.addEdge(N4, N5, E12));
    assertThat(e).hasMessageThat().contains(ERROR_REUSE_EDGE);
  }

  @Test
  public void addEdge_parallelEdge_notAllowed() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(network.allowsParallelEdges()).isFalse();

    addEdge(N1, N2, E12);
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> networkAsMutableNetwork.addEdge(N1, N2, EDGE_NOT_IN_GRAPH));
    assertThat(e).hasMessageThat().contains(ERROR_PARALLEL_EDGE);
    e =
        assertThrows(
            IllegalArgumentException.class,
            () -> networkAsMutableNetwork.addEdge(N2, N1, EDGE_NOT_IN_GRAPH));
    assertThat(e).hasMessageThat().contains(ERROR_PARALLEL_EDGE);
  }

  @Test
  public void addEdge_parallelEdge_allowsParallelEdges() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(network.allowsParallelEdges()).isTrue();

    assertTrue(networkAsMutableNetwork.addEdge(N1, N2, E12));
    assertTrue(networkAsMutableNetwork.addEdge(N2, N1, E21));
    assertTrue(networkAsMutableNetwork.addEdge(N1, N2, E12_A));
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12, E12_A, E21);
  }

  @Test
  public void addEdge_orderMismatch() {
    assume().that(graphIsMutable()).isTrue();

    EndpointPair<Integer> endpoints = EndpointPair.ordered(N1, N2);
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> networkAsMutableNetwork.addEdge(endpoints, E12));
    assertThat(e).hasMessageThat().contains(ENDPOINTS_MISMATCH);
  }

  @Test
  public void addEdge_selfLoop_notAllowed() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(network.allowsSelfLoops()).isFalse();

    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> networkAsMutableNetwork.addEdge(N1, N1, E11));
    assertThat(e).hasMessageThat().contains(ERROR_SELF_LOOP);
  }

  /**
   * This test checks an implementation dependent feature. It tests that the method {@code addEdge}
   * will silently add the missing nodes to the graph, then add the edge connecting them. We are not
   * using the proxy methods here as we want to test {@code addEdge} when the end-points are not
   * elements of the graph.
   */
  @Test
  public void addEdge_nodesNotInGraph() {
    assume().that(graphIsMutable()).isTrue();

    networkAsMutableNetwork.addNode(N1);
    assertTrue(networkAsMutableNetwork.addEdge(N1, N5, E15));
    assertTrue(networkAsMutableNetwork.addEdge(N4, N1, E41));
    assertTrue(networkAsMutableNetwork.addEdge(N2, N3, E23));
    assertThat(network.nodes()).containsExactly(N1, N5, N4, N2, N3);
    assertThat(network.edges()).containsExactly(E15, E41, E23);
    assertThat(network.edgesConnecting(N1, N5)).containsExactly(E15);
    assertThat(network.edgesConnecting(N4, N1)).containsExactly(E41);
    assertThat(network.edgesConnecting(N2, N3)).containsExactly(E23);
    assertThat(network.edgesConnecting(N3, N2)).containsExactly(E23);
  }

  @Test
  public void addEdge_selfLoop() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(network.allowsSelfLoops()).isTrue();

    assertThat(networkAsMutableNetwork.addEdge(N1, N1, E11)).isTrue();
    assertThat(network.edges()).contains(E11);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void addEdge_existingSelfLoopEdgeBetweenSameNodes() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    ImmutableSet<String> edges = ImmutableSet.copyOf(network.edges());
    assertThat(networkAsMutableNetwork.addEdge(N1, N1, E11)).isFalse();
    assertThat(network.edges()).containsExactlyElementsIn(edges);
  }

  @Test
  public void addEdge_existingEdgeBetweenDifferentNodes_selfLoops() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> networkAsMutableNetwork.addEdge(N1, N2, E11));
    assertThat(e).hasMessageThat().contains(ERROR_REUSE_EDGE);
    e =
        assertThrows(
            IllegalArgumentException.class, () -> networkAsMutableNetwork.addEdge(N2, N2, E11));
    assertThat(e).hasMessageThat().contains(ERROR_REUSE_EDGE);
    addEdge(N1, N2, E12);
    e =
        assertThrows(
            IllegalArgumentException.class, () -> networkAsMutableNetwork.addEdge(N1, N1, E12));
    assertThat(e).hasMessageThat().contains(ERROR_REUSE_EDGE);
  }

  @Test
  public void addEdge_parallelSelfLoopEdge_notAllowed() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(network.allowsSelfLoops()).isTrue();
    assume().that(network.allowsParallelEdges()).isFalse();

    addEdge(N1, N1, E11);
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> networkAsMutableNetwork.addEdge(N1, N1, EDGE_NOT_IN_GRAPH));
    assertThat(e).hasMessageThat().contains(ERROR_PARALLEL_EDGE);
  }

  @Test
  public void addEdge_parallelSelfLoopEdge_allowsParallelEdges() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(network.allowsSelfLoops()).isTrue();
    assume().that(network.allowsParallelEdges()).isTrue();

    assertTrue(networkAsMutableNetwork.addEdge(N1, N1, E11));
    assertTrue(networkAsMutableNetwork.addEdge(N1, N1, E11_A));
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11, E11_A);
  }

  @Test
  public void removeNode_existingNodeWithSelfLoopEdge() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(network.allowsSelfLoops()).isTrue();

    addNode(N1);
    addEdge(N1, N1, E11);
    assertThat(networkAsMutableNetwork.removeNode(N1)).isTrue();
    assertThat(network.nodes()).isEmpty();
    assertThat(network.edges()).doesNotContain(E11);
  }

  @Test
  public void removeEdge_existingSelfLoopEdge() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    assertThat(networkAsMutableNetwork.removeEdge(E11)).isTrue();
    assertThat(network.edges()).doesNotContain(E11);
    assertThat(network.edgesConnecting(N1, N1)).isEmpty();
  }
}
