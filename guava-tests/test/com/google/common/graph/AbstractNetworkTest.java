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

import static com.google.common.graph.TestUtil.assertEdgeNotInGraphErrorMessage;
import static com.google.common.graph.TestUtil.assertEdgeRemovedFromGraphErrorMessage;
import static com.google.common.graph.TestUtil.assertNodeNotInGraphErrorMessage;
import static com.google.common.graph.TestUtil.assertNodeRemovedFromGraphErrorMessage;
import static com.google.common.graph.TestUtil.assertStronglyEquivalent;
import static com.google.common.graph.TestUtil.sanityCheckSet;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Abstract base class for testing implementations of {@link Network} interface. Network instances
 * created for testing should have Integer node and String edge objects.
 *
 * <p>Test cases that should be handled similarly in any graph implementation are included in this
 * class. For example, testing that {@code nodes()} method returns the set of the nodes in the
 * graph. The following test cases are left for the subclasses to handle:
 *
 * <ul>
 *   <li>Test cases related to whether the graph is directed, undirected, mutable, or immutable.
 *   <li>Test cases related to the specific implementation of the {@link Network} interface.
 * </ul>
 *
 * TODO(user): Make this class generic (using <N, E>) for all node and edge types.
 * TODO(user): Differentiate between directed and undirected edge strings.
 */
public abstract class AbstractNetworkTest {

  Network<Integer, String> network;

  /**
   * The same reference as {@link #network}, except as a mutable network. This field is null in case
   * {@link #createGraph()} didn't return a mutable network.
   */
  MutableNetwork<Integer, String> networkAsMutableNetwork;

  static final Integer N1 = 1;
  static final Integer N2 = 2;
  static final Integer N3 = 3;
  static final Integer N4 = 4;
  static final Integer N5 = 5;
  static final Integer NODE_NOT_IN_GRAPH = 1000;

  static final String E11 = "1-1";
  static final String E11_A = "1-1a";
  static final String E12 = "1-2";
  static final String E12_A = "1-2a";
  static final String E12_B = "1-2b";
  static final String E21 = "2-1";
  static final String E13 = "1-3";
  static final String E14 = "1-4";
  static final String E23 = "2-3";
  static final String E31 = "3-1";
  static final String E34 = "3-4";
  static final String E41 = "4-1";
  static final String E15 = "1-5";
  static final String EDGE_NOT_IN_GRAPH = "edgeNotInGraph";

  // TODO(user): Consider separating Strings that we've defined here to capture
  // identifiable substrings of expected error messages, from Strings that we've defined
  // here to provide error messages.
  // TODO(user): Some Strings used in the subclasses can be added as static Strings
  // here too.
  static final String ERROR_PARALLEL_EDGE = "connected by a different edge";
  static final String ERROR_REUSE_EDGE = "it cannot be reused to connect";
  static final String ERROR_MODIFIABLE_COLLECTION =
      "Collection returned is unexpectedly modifiable";
  static final String ERROR_SELF_LOOP = "self-loops are not allowed";
  static final String ERROR_EDGE_NOT_IN_GRAPH =
      "Should not be allowed to pass an edge that is not an element of the graph.";
  static final String ERROR_ADDED_SELF_LOOP = "Should not be allowed to add a self-loop edge.";
  static final String ERROR_ADDED_PARALLEL_EDGE = "Should not be allowed to add a parallel edge.";
  static final String ERROR_ADDED_EXISTING_EDGE =
      "Reusing an existing edge to connect different nodes succeeded";

  /** Creates and returns an instance of the graph to be tested. */
  abstract Network<Integer, String> createGraph();

  /**
   * A proxy method that adds the node {@code n} to the graph being tested. In case of Immutable
   * graph implementations, this method should replace {@link #network} with a new graph that
   * includes this node.
   */
  abstract void addNode(Integer n);

  /**
   * A proxy method that adds the edge {@code e} to the graph being tested. In case of Immutable
   * graph implementations, this method should replace {@link #network} with a new graph that
   * includes this edge.
   */
  abstract void addEdge(Integer n1, Integer n2, String e);

  final boolean graphIsMutable() {
    return networkAsMutableNetwork != null;
  }

  @Before
  public void init() {
    network = createGraph();
    if (network instanceof MutableNetwork) {
      networkAsMutableNetwork = (MutableNetwork<Integer, String>) network;
    }
  }

  @After
  public void validateNetworkState() {
    validateNetwork(network);
  }

  static <N, E> void validateNetwork(Network<N, E> network) {
    assertStronglyEquivalent(network, Graphs.copyOf(network));
    assertStronglyEquivalent(network, ImmutableNetwork.copyOf(network));

    String networkString = network.toString();
    assertThat(networkString).contains("isDirected: " + network.isDirected());
    assertThat(networkString).contains("allowsParallelEdges: " + network.allowsParallelEdges());
    assertThat(networkString).contains("allowsSelfLoops: " + network.allowsSelfLoops());

    int nodeStart = networkString.indexOf("nodes:");
    int edgeStart = networkString.indexOf("edges:");
    String nodeString = networkString.substring(nodeStart, edgeStart);
    String edgeString = networkString.substring(edgeStart);

    Graph<N> asGraph = network.asGraph();
    AbstractGraphTest.validateGraph(asGraph);
    assertThat(network.nodes()).isEqualTo(asGraph.nodes());
    assertThat(network.edges().size()).isAtLeast(asGraph.edges().size());
    assertThat(network.nodeOrder()).isEqualTo(asGraph.nodeOrder());
    assertThat(network.isDirected()).isEqualTo(asGraph.isDirected());
    assertThat(network.allowsSelfLoops()).isEqualTo(asGraph.allowsSelfLoops());

    for (E edge : sanityCheckSet(network.edges())) {
      // TODO(b/27817069): Consider verifying the edge's incident nodes in the string.
      assertThat(edgeString).contains(edge.toString());

      EndpointPair<N> endpointPair = network.incidentNodes(edge);
      N nodeU = endpointPair.nodeU();
      N nodeV = endpointPair.nodeV();
      assertThat(asGraph.edges()).contains(EndpointPair.of(network, nodeU, nodeV));
      assertThat(network.edgesConnecting(nodeU, nodeV)).contains(edge);
      assertThat(network.successors(nodeU)).contains(nodeV);
      assertThat(network.adjacentNodes(nodeU)).contains(nodeV);
      assertThat(network.outEdges(nodeU)).contains(edge);
      assertThat(network.incidentEdges(nodeU)).contains(edge);
      assertThat(network.predecessors(nodeV)).contains(nodeU);
      assertThat(network.adjacentNodes(nodeV)).contains(nodeU);
      assertThat(network.inEdges(nodeV)).contains(edge);
      assertThat(network.incidentEdges(nodeV)).contains(edge);

      for (N incidentNode : network.incidentNodes(edge)) {
        assertThat(network.nodes()).contains(incidentNode);
        for (E adjacentEdge : network.incidentEdges(incidentNode)) {
          assertTrue(
              edge.equals(adjacentEdge) || network.adjacentEdges(edge).contains(adjacentEdge));
        }
      }
    }

    for (N node : sanityCheckSet(network.nodes())) {
      assertThat(nodeString).contains(node.toString());

      assertThat(network.adjacentNodes(node)).isEqualTo(asGraph.adjacentNodes(node));
      assertThat(network.predecessors(node)).isEqualTo(asGraph.predecessors(node));
      assertThat(network.successors(node)).isEqualTo(asGraph.successors(node));

      int selfLoopCount = network.edgesConnecting(node, node).size();
      assertThat(network.incidentEdges(node).size() + selfLoopCount)
          .isEqualTo(network.degree(node));

      if (network.isDirected()) {
        assertThat(network.incidentEdges(node).size() + selfLoopCount)
            .isEqualTo(network.inDegree(node) + network.outDegree(node));
        assertThat(network.inEdges(node)).hasSize(network.inDegree(node));
        assertThat(network.outEdges(node)).hasSize(network.outDegree(node));
      } else {
        assertThat(network.predecessors(node)).isEqualTo(network.adjacentNodes(node));
        assertThat(network.successors(node)).isEqualTo(network.adjacentNodes(node));
        assertThat(network.inEdges(node)).isEqualTo(network.incidentEdges(node));
        assertThat(network.outEdges(node)).isEqualTo(network.incidentEdges(node));
        assertThat(network.inDegree(node)).isEqualTo(network.degree(node));
        assertThat(network.outDegree(node)).isEqualTo(network.degree(node));
      }

      for (N otherNode : network.nodes()) {
        Set<E> edgesConnecting = sanityCheckSet(network.edgesConnecting(node, otherNode));
        switch (edgesConnecting.size()) {
          case 0:
            assertThat(network.edgeConnectingOrNull(node, otherNode)).isNull();
            assertThat(network.edgeConnecting(node, otherNode).isPresent()).isFalse();
            assertThat(network.hasEdgeConnecting(node, otherNode)).isFalse();
            break;
          case 1:
            E edge = edgesConnecting.iterator().next();
            assertThat(network.edgeConnectingOrNull(node, otherNode)).isEqualTo(edge);
            assertThat(network.edgeConnecting(node, otherNode).get()).isEqualTo(edge);
            assertThat(network.hasEdgeConnecting(node, otherNode)).isTrue();
            break;
          default:
            assertThat(network.hasEdgeConnecting(node, otherNode)).isTrue();
            try {
              network.edgeConnectingOrNull(node, otherNode);
              fail();
            } catch (IllegalArgumentException expected) {
            }
            try {
              network.edgeConnecting(node, otherNode);
              fail();
            } catch (IllegalArgumentException expected) {
            }
        }

        boolean isSelfLoop = node.equals(otherNode);
        boolean connected = !edgesConnecting.isEmpty();
        if (network.isDirected() || !isSelfLoop) {
          assertThat(edgesConnecting)
              .isEqualTo(Sets.intersection(network.outEdges(node), network.inEdges(otherNode)));
        }
        if (!network.allowsParallelEdges()) {
          assertThat(edgesConnecting.size()).isAtMost(1);
        }
        if (!network.allowsSelfLoops() && isSelfLoop) {
          assertThat(connected).isFalse();
        }

        assertThat(network.successors(node).contains(otherNode)).isEqualTo(connected);
        assertThat(network.predecessors(otherNode).contains(node)).isEqualTo(connected);
        for (E edge : edgesConnecting) {
          assertThat(network.incidentNodes(edge))
              .isEqualTo(EndpointPair.of(network, node, otherNode));
          assertThat(network.outEdges(node)).contains(edge);
          assertThat(network.inEdges(otherNode)).contains(edge);
        }
      }

      for (N adjacentNode : sanityCheckSet(network.adjacentNodes(node))) {
        assertTrue(
            network.predecessors(node).contains(adjacentNode)
                || network.successors(node).contains(adjacentNode));
        assertTrue(
            !network.edgesConnecting(node, adjacentNode).isEmpty()
                || !network.edgesConnecting(adjacentNode, node).isEmpty());
      }

      for (N predecessor : sanityCheckSet(network.predecessors(node))) {
        assertThat(network.successors(predecessor)).contains(node);
        assertThat(network.edgesConnecting(predecessor, node)).isNotEmpty();
      }

      for (N successor : sanityCheckSet(network.successors(node))) {
        assertThat(network.predecessors(successor)).contains(node);
        assertThat(network.edgesConnecting(node, successor)).isNotEmpty();
      }

      for (E incidentEdge : sanityCheckSet(network.incidentEdges(node))) {
        assertTrue(
            network.inEdges(node).contains(incidentEdge)
                || network.outEdges(node).contains(incidentEdge));
        assertThat(network.edges()).contains(incidentEdge);
        assertThat(network.incidentNodes(incidentEdge)).contains(node);
      }

      for (E inEdge : sanityCheckSet(network.inEdges(node))) {
        assertThat(network.incidentEdges(node)).contains(inEdge);
        assertThat(network.outEdges(network.incidentNodes(inEdge).adjacentNode(node)))
            .contains(inEdge);
        if (network.isDirected()) {
          assertThat(network.incidentNodes(inEdge).target()).isEqualTo(node);
        }
      }

      for (E outEdge : sanityCheckSet(network.outEdges(node))) {
        assertThat(network.incidentEdges(node)).contains(outEdge);
        assertThat(network.inEdges(network.incidentNodes(outEdge).adjacentNode(node)))
            .contains(outEdge);
        if (network.isDirected()) {
          assertThat(network.incidentNodes(outEdge).source()).isEqualTo(node);
        }
      }
    }
  }

  /**
   * Verifies that the {@code Set} returned by {@code nodes} has the expected mutability property
   * (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void nodes_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code edges} has the expected mutability property
   * (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void edges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code incidentEdges} has the expected mutability
   * property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void incidentEdges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code adjacentNodes} has the expected mutability
   * property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void adjacentNodes_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code adjacentEdges} has the expected mutability
   * property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void adjacentEdges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code edgesConnecting} has the expected mutability
   * property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void edgesConnecting_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code inEdges} has the expected mutability property
   * (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void inEdges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code outEdges} has the expected mutability property
   * (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void outEdges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code predecessors} has the expected mutability
   * property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void predecessors_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code successors} has the expected mutability
   * property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void successors_checkReturnedSetMutability();

  @Test
  public void nodes_oneNode() {
    addNode(N1);
    assertThat(network.nodes()).containsExactly(N1);
  }

  @Test
  public void nodes_noNodes() {
    assertThat(network.nodes()).isEmpty();
  }

  @Test
  public void edges_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.edges()).containsExactly(E12);
  }

  @Test
  public void edges_noEdges() {
    assertThat(network.edges()).isEmpty();
    // Network with no edges, given disconnected nodes
    addNode(N1);
    addNode(N2);
    assertThat(network.edges()).isEmpty();
  }

  @Test
  public void incidentEdges_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.incidentEdges(N2)).containsExactly(E12);
    assertThat(network.incidentEdges(N1)).containsExactly(E12);
  }

  @Test
  public void incidentEdges_isolatedNode() {
    addNode(N1);
    assertThat(network.incidentEdges(N1)).isEmpty();
  }

  @Test
  public void incidentEdges_nodeNotInGraph() {
    assertNodeNotInGraphErrorMessage(
        assertThrows(
            IllegalArgumentException.class, () -> network.incidentEdges(NODE_NOT_IN_GRAPH)));
  }

  @Test
  public void incidentNodes_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.incidentNodes(E12)).containsExactly(N1, N2);
  }

  @Test
  public void incidentNodes_edgeNotInGraph() {
    assertEdgeNotInGraphErrorMessage(
        assertThrows(
            IllegalArgumentException.class, () -> network.incidentNodes(EDGE_NOT_IN_GRAPH)));
  }

  @Test
  public void adjacentNodes_oneEdge() {
    addEdge(N1, N2, E12);
    assertThat(network.adjacentNodes(N1)).containsExactly(N2);
    assertThat(network.adjacentNodes(N2)).containsExactly(N1);
  }

  @Test
  public void adjacentNodes_noAdjacentNodes() {
    addNode(N1);
    assertThat(network.adjacentNodes(N1)).isEmpty();
  }

  @Test
  public void adjacentNodes_nodeNotInGraph() {
    assertNodeNotInGraphErrorMessage(
        assertThrows(
            IllegalArgumentException.class, () -> network.adjacentNodes(NODE_NOT_IN_GRAPH)));
  }

  @Test
  public void adjacentEdges_bothEndpoints() {
    addEdge(N1, N2, E12);
    addEdge(N2, N3, E23);
    addEdge(N3, N1, E31);
    addEdge(N3, N4, E34);
    assertThat(network.adjacentEdges(E12)).containsExactly(E31, E23);
  }

  @Test
  public void adjacentEdges_noAdjacentEdges() {
    addEdge(N1, N2, E12);
    addEdge(N3, N4, E34);
    assertThat(network.adjacentEdges(E12)).isEmpty();
  }

  @Test
  public void adjacentEdges_edgeNotInGraph() {
    assertEdgeNotInGraphErrorMessage(
        assertThrows(
            IllegalArgumentException.class, () -> network.adjacentEdges(EDGE_NOT_IN_GRAPH)));
  }

  @Test
  public void adjacentEdges_parallelEdges() {
    assume().that(network.allowsParallelEdges()).isTrue();

    addEdge(N1, N2, E12);
    addEdge(N1, N2, E12_A);
    addEdge(N1, N2, E12_B);
    addEdge(N3, N4, E34);

    assertThat(network.adjacentEdges(E12)).containsExactly(E12_A, E12_B);
  }

  @Test
  public void edgesConnecting_disconnectedNodes() {
    addNode(N1);
    addNode(N2);
    assertThat(network.edgesConnecting(N1, N2)).isEmpty();
  }

  @Test
  public void edgesConnecting_nodesNotInGraph() {
    addNode(N1);
    addNode(N2);
    assertNodeNotInGraphErrorMessage(
        assertThrows(
            IllegalArgumentException.class, () -> network.edgesConnecting(N1, NODE_NOT_IN_GRAPH)));
    assertNodeNotInGraphErrorMessage(
        assertThrows(
            IllegalArgumentException.class, () -> network.edgesConnecting(NODE_NOT_IN_GRAPH, N2)));
    assertNodeNotInGraphErrorMessage(
        assertThrows(
            IllegalArgumentException.class,
            () -> network.edgesConnecting(NODE_NOT_IN_GRAPH, NODE_NOT_IN_GRAPH)));
  }

  @Test
  public void edgesConnecting_parallelEdges_directed() {
    assume().that(network.allowsParallelEdges()).isTrue();
    assume().that(network.isDirected()).isTrue();

    addEdge(N1, N2, E12);
    addEdge(N1, N2, E12_A);

    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12, E12_A);
    // Passed nodes should be in the correct edge direction, first is the
    // source node and the second is the target node
    assertThat(network.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void edgesConnecting_parallelEdges_undirected() {
    assume().that(network.allowsParallelEdges()).isTrue();
    assume().that(network.isDirected()).isFalse();

    addEdge(N1, N2, E12);
    addEdge(N1, N2, E12_A);
    addEdge(N2, N1, E21);

    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12, E12_A, E21);
    assertThat(network.edgesConnecting(N2, N1)).containsExactly(E12, E12_A, E21);
  }

  @Test
  public void edgesConnecting_parallelSelfLoopEdges() {
    assume().that(network.allowsParallelEdges()).isTrue();
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    addEdge(N1, N1, E11_A);

    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11, E11_A);
  }

  @Test
  public void hasEdgeConnecting_disconnectedNodes() {
    addNode(N1);
    addNode(N2);
    assertThat(network.hasEdgeConnecting(N1, N2)).isFalse();
  }

  @Test
  public void hasEdgesConnecting_nodesNotInGraph() {
    addNode(N1);
    addNode(N2);
    assertThat(network.hasEdgeConnecting(N1, NODE_NOT_IN_GRAPH)).isFalse();
    assertThat(network.hasEdgeConnecting(NODE_NOT_IN_GRAPH, N2)).isFalse();
    assertThat(network.hasEdgeConnecting(NODE_NOT_IN_GRAPH, NODE_NOT_IN_GRAPH)).isFalse();
  }

  @Test
  public void inEdges_noInEdges() {
    addNode(N1);
    assertThat(network.inEdges(N1)).isEmpty();
  }

  @Test
  public void inEdges_nodeNotInGraph() {
    assertNodeNotInGraphErrorMessage(
        assertThrows(IllegalArgumentException.class, () -> network.inEdges(NODE_NOT_IN_GRAPH)));
  }

  @Test
  public void outEdges_noOutEdges() {
    addNode(N1);
    assertThat(network.outEdges(N1)).isEmpty();
  }

  @Test
  public void outEdges_nodeNotInGraph() {
    assertNodeNotInGraphErrorMessage(
        assertThrows(IllegalArgumentException.class, () -> network.outEdges(NODE_NOT_IN_GRAPH)));
  }

  @Test
  public void predecessors_noPredecessors() {
    addNode(N1);
    assertThat(network.predecessors(N1)).isEmpty();
  }

  @Test
  public void predecessors_nodeNotInGraph() {
    assertNodeNotInGraphErrorMessage(
        assertThrows(
            IllegalArgumentException.class, () -> network.predecessors(NODE_NOT_IN_GRAPH)));
  }

  @Test
  public void successors_noSuccessors() {
    addNode(N1);
    assertThat(network.successors(N1)).isEmpty();
  }

  @Test
  public void successors_nodeNotInGraph() {
    assertNodeNotInGraphErrorMessage(
        assertThrows(IllegalArgumentException.class, () -> network.successors(NODE_NOT_IN_GRAPH)));
  }

  @Test
  public void addNode_newNode() {
    assume().that(graphIsMutable()).isTrue();

    assertTrue(networkAsMutableNetwork.addNode(N1));
    assertThat(networkAsMutableNetwork.nodes()).contains(N1);
  }

  @Test
  public void addNode_existingNode() {
    assume().that(graphIsMutable()).isTrue();

    addNode(N1);
    ImmutableSet<Integer> nodes = ImmutableSet.copyOf(networkAsMutableNetwork.nodes());
    assertFalse(networkAsMutableNetwork.addNode(N1));
    assertThat(networkAsMutableNetwork.nodes()).containsExactlyElementsIn(nodes);
  }

  @Test
  public void removeNode_existingNode() {
    assume().that(graphIsMutable()).isTrue();

    addEdge(N1, N2, E12);
    addEdge(N4, N1, E41);
    assertTrue(networkAsMutableNetwork.removeNode(N1));
    assertFalse(networkAsMutableNetwork.removeNode(N1));
    assertThat(networkAsMutableNetwork.nodes()).containsExactly(N2, N4);
    assertThat(networkAsMutableNetwork.edges()).doesNotContain(E12);
    assertThat(networkAsMutableNetwork.edges()).doesNotContain(E41);

    assertThat(network.adjacentNodes(N2)).isEmpty();
    assertThat(network.predecessors(N2)).isEmpty();
    assertThat(network.successors(N2)).isEmpty();
    assertThat(network.incidentEdges(N2)).isEmpty();
    assertThat(network.inEdges(N2)).isEmpty();
    assertThat(network.outEdges(N2)).isEmpty();
    assertThat(network.adjacentNodes(N4)).isEmpty();
    assertThat(network.predecessors(N4)).isEmpty();
    assertThat(network.successors(N4)).isEmpty();
    assertThat(network.incidentEdges(N4)).isEmpty();
    assertThat(network.inEdges(N4)).isEmpty();
    assertThat(network.outEdges(N4)).isEmpty();

    assertNodeNotInGraphErrorMessage(
        assertThrows(IllegalArgumentException.class, () -> network.adjacentNodes(N1)));
    assertNodeNotInGraphErrorMessage(
        assertThrows(IllegalArgumentException.class, () -> network.predecessors(N1)));
    assertNodeNotInGraphErrorMessage(
        assertThrows(IllegalArgumentException.class, () -> network.successors(N1)));
    assertNodeNotInGraphErrorMessage(
        assertThrows(IllegalArgumentException.class, () -> network.incidentEdges(N1)));
  }

  @Test
  public void removeNode_nodeNotPresent() {
    assume().that(graphIsMutable()).isTrue();

    addNode(N1);
    ImmutableSet<Integer> nodes = ImmutableSet.copyOf(networkAsMutableNetwork.nodes());
    assertFalse(networkAsMutableNetwork.removeNode(NODE_NOT_IN_GRAPH));
    assertThat(networkAsMutableNetwork.nodes()).containsExactlyElementsIn(nodes);
  }

  @Test
  public void queryAccessorSetAfterElementRemoval() {
    assume().that(graphIsMutable()).isTrue();

    addEdge(N1, N2, E12);
    Set<Integer> n1AdjacentNodes = network.adjacentNodes(N1);
    Set<Integer> n2AdjacentNodes = network.adjacentNodes(N2);
    Set<Integer> n1Predecessors = network.predecessors(N1);
    Set<Integer> n2Predecessors = network.predecessors(N2);
    Set<Integer> n1Successors = network.successors(N1);
    Set<Integer> n2Successors = network.successors(N2);
    Set<String> n1IncidentEdges = network.incidentEdges(N1);
    Set<String> n2IncidentEdges = network.incidentEdges(N2);
    Set<String> n1InEdges = network.inEdges(N1);
    Set<String> n2InEdges = network.inEdges(N2);
    Set<String> n1OutEdges = network.outEdges(N1);
    Set<String> n2OutEdges = network.outEdges(N2);
    Set<String> e12AdjacentEdges = network.adjacentEdges(E12);
    Set<String> n12EdgesConnecting = network.edgesConnecting(N1, N2);
    assertThat(networkAsMutableNetwork.removeNode(N1)).isTrue();

    // The choice of the size() method to call here is arbitrary.  We assume that if any of the Set
    // methods executes the validation check, they all will, and thus we only need to test one of
    // them to ensure that the validation check happens and has the expected behavior.
    assertNodeRemovedFromGraphErrorMessage(
        assertThrows(IllegalStateException.class, n1AdjacentNodes::size));
    assertNodeRemovedFromGraphErrorMessage(
        assertThrows(IllegalStateException.class, n1Predecessors::size));
    assertNodeRemovedFromGraphErrorMessage(
        assertThrows(IllegalStateException.class, n1Successors::size));
    assertNodeRemovedFromGraphErrorMessage(
        assertThrows(IllegalStateException.class, n1IncidentEdges::size));
    assertNodeRemovedFromGraphErrorMessage(
        assertThrows(IllegalStateException.class, n1InEdges::size));
    assertNodeRemovedFromGraphErrorMessage(
        assertThrows(IllegalStateException.class, n1OutEdges::size));
    assertEdgeRemovedFromGraphErrorMessage(
        assertThrows(IllegalStateException.class, e12AdjacentEdges::size));
    assertNodeRemovedFromGraphErrorMessage(
        assertThrows(IllegalStateException.class, n12EdgesConnecting::size));

    assertThat(n2AdjacentNodes).isEmpty();
    assertThat(n2Predecessors).isEmpty();
    assertThat(n2Successors).isEmpty();
    assertThat(n2IncidentEdges).isEmpty();
    assertThat(n2InEdges).isEmpty();
    assertThat(n2OutEdges).isEmpty();
  }

  @Test
  public void removeEdge_existingEdge() {
    assume().that(graphIsMutable()).isTrue();

    addEdge(N1, N2, E12);
    assertTrue(networkAsMutableNetwork.removeEdge(E12));
    assertFalse(networkAsMutableNetwork.removeEdge(E12));
    assertThat(networkAsMutableNetwork.edges()).doesNotContain(E12);
    assertThat(networkAsMutableNetwork.edgesConnecting(N1, N2)).isEmpty();
  }

  @Test
  public void removeEdge_oneOfMany() {
    assume().that(graphIsMutable()).isTrue();

    addEdge(N1, N2, E12);
    addEdge(N1, N3, E13);
    addEdge(N1, N4, E14);
    assertThat(networkAsMutableNetwork.edges()).containsExactly(E12, E13, E14);
    assertTrue(networkAsMutableNetwork.removeEdge(E13));
    assertThat(networkAsMutableNetwork.edges()).containsExactly(E12, E14);
  }

  @Test
  public void removeEdge_edgeNotPresent() {
    assume().that(graphIsMutable()).isTrue();

    addEdge(N1, N2, E12);
    ImmutableSet<String> edges = ImmutableSet.copyOf(networkAsMutableNetwork.edges());
    assertFalse(networkAsMutableNetwork.removeEdge(EDGE_NOT_IN_GRAPH));
    assertThat(networkAsMutableNetwork.edges()).containsExactlyElementsIn(edges);
  }

  @Test
  public void removeEdge_queryAfterRemoval() {
    assume().that(graphIsMutable()).isTrue();

    addEdge(N1, N2, E12);
    @SuppressWarnings("unused")
    EndpointPair<Integer> unused =
        networkAsMutableNetwork.incidentNodes(E12); // ensure cache (if any) is populated
    assertTrue(networkAsMutableNetwork.removeEdge(E12));
    assertEdgeNotInGraphErrorMessage(
        assertThrows(
            IllegalArgumentException.class, () -> networkAsMutableNetwork.incidentNodes(E12)));
  }

  @Test
  public void removeEdge_parallelEdge() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(network.allowsParallelEdges()).isTrue();

    addEdge(N1, N2, E12);
    addEdge(N1, N2, E12_A);
    assertTrue(networkAsMutableNetwork.removeEdge(E12_A));
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
  }

  @Test
  public void removeEdge_parallelSelfLoopEdge() {
    assume().that(graphIsMutable()).isTrue();
    assume().that(network.allowsParallelEdges()).isTrue();
    assume().that(network.allowsSelfLoops()).isTrue();

    addEdge(N1, N1, E11);
    addEdge(N1, N1, E11_A);
    addEdge(N1, N2, E12);
    assertTrue(networkAsMutableNetwork.removeEdge(E11_A));
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    assertTrue(networkAsMutableNetwork.removeEdge(E11));
    assertThat(network.edgesConnecting(N1, N1)).isEmpty();
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
  }

  @Test
  public void concurrentIteration() throws Exception {
    addEdge(1, 2, "foo");
    addEdge(3, 4, "bar");
    addEdge(5, 6, "baz");

    int threadCount = 20;
    ExecutorService executor = newFixedThreadPool(threadCount);
    final CyclicBarrier barrier = new CyclicBarrier(threadCount);
    ImmutableList.Builder<Future<?>> futures = ImmutableList.builder();
    for (int i = 0; i < threadCount; i++) {
      futures.add(
          executor.submit(
              new Callable<@Nullable Void>() {
                @Override
                public @Nullable Void call() throws Exception {
                  barrier.await();
                  Integer first = network.nodes().iterator().next();
                  for (Integer node : network.nodes()) {
                    Set<Integer> unused = network.successors(node);
                  }
                  /*
                   * Also look up an earlier node so that, if the graph is using MapRetrievalCache,
                   * we read one of the fields declared in that class.
                   */
                  Set<Integer> unused = network.successors(first);
                  return null;
                }
              }));
    }

    /*
     * It's unlikely that any operations would fail by throwing an exception, but let's check them
     * just to be safe.
     *
     * The real purpose of this test is to produce a TSAN failure if MapIteratorCache is unsafe for
     * reads from multiple threads -- unsafe, in fact, even in the absence of a concurrent write.
     * The specific problem we had was unsafe reads of lastEntryReturnedBySomeIterator. (To fix the
     * problem, we've since marked that field as volatile.)
     *
     * When MapIteratorCache is used from Immutable* classes, the TSAN failure doesn't indicate a
     * real problem: The Entry objects are ImmutableMap entries, whose fields are all final and thus
     * safe to read even when the Entry object is unsafely published. But with a mutable graph, the
     * Entry object is likely to have a non-final value field, which is not safe to read when
     * unsafely published. (The Entry object might even be newly created by each iterator.next()
     * call, so we can't assume that writes to the Entry have been safely published by some other
     * synchronization actions.)
     *
     * All that said: I haven't actually managed to make this particular test produce a TSAN error
     * for the field accesses in MapIteratorCache. This test *has* found other TSAN errors,
     * including in MapRetrievalCache, so I'm not sure why this one is different. I did at least
     * confirm that my change to MapIteratorCache fixes the TSAN error in the (larger) test it was
     * originally reported in.
     */
    for (Future<?> future : futures.build()) {
      future.get();
    }
    executor.shutdown();
  }
}
