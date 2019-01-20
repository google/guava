/*
 * Copyright (C) 2017 The Guava Authors
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

import static com.google.common.graph.AbstractNetworkTest.ERROR_MODIFIABLE_COLLECTION;
import static com.google.common.graph.TestUtil.ERROR_NODE_NOT_IN_GRAPH;
import static com.google.common.graph.TestUtil.EdgeType.DIRECTED;
import static com.google.common.graph.TestUtil.EdgeType.UNDIRECTED;
import static com.google.common.graph.TestUtil.assertNodeNotInGraphErrorMessage;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.graph.TestUtil.EdgeType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test for {@link Network} methods which have default implementations. Currently those
 * implementations are in {@link AbstractNetwork}; in future they might be in {@link Network}
 * itself, once we are willing to use Java 8 default methods.
 */
@AndroidIncompatible
// TODO(cpovirk): Figure out Android JUnit 4 support. Does it work with Gingerbread? @RunWith?
@RunWith(Parameterized.class)
public final class DefaultNetworkImplementationsTest {
  private MutableNetwork<Integer, String> network;
  private NetworkForTest<Integer, String> networkForTest;
  private static final Integer N1 = 1;
  private static final Integer N2 = 2;
  private static final Integer NODE_NOT_IN_GRAPH = 1000;
  private static final String E11 = "1-1";
  private static final String E11_A = "1-1a";
  private static final String E12 = "1-2";
  private static final String E12_A = "1-2a";
  private static final String E21 = "2-1";
  private static final String E23 = "2-3";

  @Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(
        new Object[][] {
          {UNDIRECTED}, {DIRECTED},
        });
  }

  private final EdgeType edgeType;

  public DefaultNetworkImplementationsTest(EdgeType edgeType) {
    this.edgeType = edgeType;
  }

  @Before
  public void setUp() throws Exception {
    NetworkBuilder<Object, Object> builder =
        (edgeType == EdgeType.DIRECTED) ? NetworkBuilder.directed() : NetworkBuilder.undirected();

    network = builder.allowsSelfLoops(true).allowsParallelEdges(true).build();
    networkForTest = NetworkForTest.from(network);
  }

  @Test
  public void edgesConnecting_disconnectedNodes() {
    network.addNode(N1);
    network.addNode(N2);
    assertThat(networkForTest.edgesConnecting(N1, N2)).isEmpty();
  }

  @Test
  public void edgesConnecting_nodesNotInGraph() {
    network.addNode(N1);
    network.addNode(N2);
    try {
      networkForTest.edgesConnecting(N1, NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
    try {
      networkForTest.edgesConnecting(NODE_NOT_IN_GRAPH, N2);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
    try {
      networkForTest.edgesConnecting(NODE_NOT_IN_GRAPH, NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void edgesConnecting_checkReturnedSetMutability() {
    network.addNode(N1);
    network.addNode(N2);
    Set<String> edgesConnecting = network.edgesConnecting(N1, N2);
    try {
      edgesConnecting.add(E23);
      fail(ERROR_MODIFIABLE_COLLECTION);
    } catch (UnsupportedOperationException e) {
      network.addEdge(N1, N2, E12);
      assertThat(networkForTest.edgesConnecting(N1, N2)).containsExactlyElementsIn(edgesConnecting);
    }
  }

  @Test
  public void edgesConnecting_oneEdge() {
    network.addEdge(N1, N2, E12);
    assertThat(networkForTest.edgesConnecting(N1, N2)).containsExactly(E12);
    if (edgeType == EdgeType.DIRECTED) {
      assertThat(networkForTest.edgesConnecting(N2, N1)).isEmpty();
    } else {
      assertThat(networkForTest.edgesConnecting(N2, N1)).containsExactly(E12);
    }
  }

  @Test
  public void edgesConnecting_selfLoop() {
    network.addEdge(N1, N1, E11);
    assertThat(networkForTest.edgesConnecting(N1, N1)).containsExactly(E11);
    network.addEdge(N1, N2, E12);
    assertThat(networkForTest.edgesConnecting(N1, N2)).containsExactly(E12);
    assertThat(networkForTest.edgesConnecting(N1, N1)).containsExactly(E11);
  }

  @Test
  public void edgesConnecting_parallelEdges() {
    network.addEdge(N1, N2, E12);
    network.addEdge(N1, N2, E12_A);
    network.addEdge(N2, N1, E21);
    if (edgeType == EdgeType.DIRECTED) {
      assertThat(networkForTest.edgesConnecting(N1, N2)).containsExactly(E12, E12_A);
      assertThat(networkForTest.edgesConnecting(N2, N1)).containsExactly(E21);
    } else {
      assertThat(networkForTest.edgesConnecting(N1, N2)).containsExactly(E12, E12_A, E21);
      assertThat(networkForTest.edgesConnecting(N2, N1)).containsExactly(E12, E12_A, E21);
    }
  }

  @Test
  public void edgesConnecting_parallelSelfLoopEdges() {
    network.addEdge(N1, N1, E11);
    network.addEdge(N1, N1, E11_A);
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11, E11_A);
  }

  private static class NetworkForTest<N, E> extends AbstractNetwork<N, E> {
    private final Network<N, E> network;

    NetworkForTest(Network<N, E> network) {
      this.network = network;
    }

    static <N, E> NetworkForTest<N, E> from(Network<N, E> network) {
      return new NetworkForTest<>(network);
    }

    @Override
    public Set<N> nodes() {
      return network.nodes();
    }

    @Override
    public Set<E> edges() {
      return network.edges();
    }

    @Override
    public boolean isDirected() {
      return network.isDirected();
    }

    @Override
    public boolean allowsParallelEdges() {
      return network.allowsParallelEdges();
    }

    @Override
    public boolean allowsSelfLoops() {
      return network.allowsSelfLoops();
    }

    @Override
    public ElementOrder<N> nodeOrder() {
      return network.nodeOrder();
    }

    @Override
    public ElementOrder<E> edgeOrder() {
      return network.edgeOrder();
    }

    @Override
    public Set<N> adjacentNodes(N node) {
      return network.adjacentNodes(node);
    }

    @Override
    public Set<N> predecessors(N node) {
      return network.predecessors(node);
    }

    @Override
    public Set<N> successors(N node) {
      return network.successors(node);
    }

    @Override
    public Set<E> incidentEdges(N node) {
      return network.incidentEdges(node);
    }

    @Override
    public Set<E> inEdges(N node) {
      return network.inEdges(node);
    }

    @Override
    public Set<E> outEdges(N node) {
      return network.outEdges(node);
    }

    @Override
    public EndpointPair<N> incidentNodes(E edge) {
      return network.incidentNodes(edge);
    }

    @Override
    public Set<E> adjacentEdges(E edge) {
      return network.adjacentEdges(edge);
    }

    // _don't_ override edge*Connecting*; we want the behavior from AbstractNetwork
  }
}
