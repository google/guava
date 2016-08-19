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

import static com.google.common.graph.TestUtil.sanityCheckCollection;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.testing.EqualsTester;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Abstract base class for testing implementations of {@link Network} interface. Network
 * instances created for testing should have Integer node and String edge objects.
 *
 * <p>Test cases that should be handled similarly in any graph implementation are
 * included in this class. For example, testing that {@code nodes()} method returns
 * the set of the nodes in the graph. The following test cases are left for the subclasses
 * to handle:
 * <ul>
 * <li>Test cases related to whether the graph is directed, undirected, mutable,
 *     or immutable.
 * <li>Test cases related to the specific implementation of the {@link Network} interface.
 * </ul>
 *
 * TODO(user): Make this class generic (using <N, E>) for all node and edge types.
 * TODO(user): Differentiate between directed and undirected edge strings.
 */
public abstract class AbstractNetworkTest {
  MutableNetwork<Integer, String> network;
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
  static final String ERROR_ELEMENT_NOT_IN_GRAPH = "not an element of this graph";
  static final String NODE_STRING = "Node";
  static final String EDGE_STRING = "Edge";
  static final String ERROR_PARALLEL_EDGE = "connected by a different edge";
  static final String ERROR_REUSE_EDGE = "it cannot be reused to connect";
  static final String ERROR_MODIFIABLE_COLLECTION =
      "Collection returned is unexpectedly modifiable";
  static final String ERROR_SELF_LOOP = "self-loops are not allowed";
  static final String ERROR_NODE_NOT_IN_GRAPH =
      "Should not be allowed to pass a node that is not an element of the graph.";
  static final String ERROR_EDGE_NOT_IN_GRAPH =
      "Should not be allowed to pass an edge that is not an element of the graph.";
  static final String ERROR_ADDED_SELF_LOOP = "Should not be allowed to add a self-loop edge.";
  static final String ERROR_ADDED_PARALLEL_EDGE = "Should not be allowed to add a parallel edge.";
  static final String ERROR_ADDED_EXISTING_EDGE =
      "Reusing an existing edge to connect different nodes succeeded";

  /**
   * Creates and returns an instance of the graph to be tested.
   */
  public abstract MutableNetwork<Integer, String> createGraph();

  /**
   * A proxy method that adds the node {@code n} to the graph being tested.
   * In case of Immutable graph implementations, this method should add {@code n} to the graph
   * builder and build a new graph with the current builder state.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * TODO(user): Consider changing access modifier to be protected.
   */
  @CanIgnoreReturnValue
  boolean addNode(Integer n) {
    return network.addNode(n);
  }

  /**
   * A proxy method that adds the edge {@code e} to the graph
   * being tested. In case of Immutable graph implementations, this method
   * should add {@code e} to the graph builder and build a new graph with the current
   * builder state.
   *
   * <p>This method should be used in tests of specific implementations if you want to
   * ensure uniform behavior (including side effects) with how edges are added elsewhere
   * in the tests.  For example, the existing implementations of this method explicitly
   * add the supplied nodes to the graph, and then call {@code graph.addEdge()} to connect
   * the edge to the nodes; this is not part of the contract of {@code graph.addEdge()}
   * and is done for convenience.  In cases where you want to avoid such side effects
   * (e.g., if you're testing what happens in your implementation if you add an edge
   * whose end-points don't already exist in the graph), you should <b>not</b> use this
   * method.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * TODO(user): Consider changing access modifier to be protected.
   */
  @CanIgnoreReturnValue
  boolean addEdge(String e, Integer n1, Integer n2) {
    network.addNode(n1);
    network.addNode(n2);
    return network.addEdge(n1, n2, e);
  }

  @Before
  public void init() {
    network = createGraph();
  }

  @After
  public void validateNetworkState() {
    validateNetwork(network);
  }

  static <N, E> void validateNetwork(Network<N, E> network) {
    new EqualsTester().addEqualityGroup(
        network,
        Graphs.copyOf(network),
        ImmutableNetwork.copyOf(network)).testEquals();

    String networkString = network.toString();
    assertThat(networkString).contains("isDirected: " + network.isDirected());
    assertThat(networkString).contains("allowsParallelEdges: " + network.allowsParallelEdges());
    assertThat(networkString).contains("allowsSelfLoops: " + network.allowsSelfLoops());

    int nodeStart = networkString.indexOf("nodes:");
    int edgeStart = networkString.indexOf("edges:");
    String nodeString = networkString.substring(nodeStart, edgeStart);
    String edgeString = networkString.substring(edgeStart);

    Graph<N, Set<E>> asGraph = network.asGraph();
    AbstractGraphTest.validateGraph(asGraph);
    assertThat(network.nodes()).isEqualTo(asGraph.nodes());
    assertThat(network.edges().size()).isAtLeast(asGraph.edges().size());
    assertThat(network.nodeOrder()).isEqualTo(asGraph.nodeOrder());
    assertThat(network.isDirected()).isEqualTo(asGraph.isDirected());
    assertThat(network.allowsSelfLoops()).isEqualTo(asGraph.allowsSelfLoops());

    sanityCheckCollection(network.nodes());
    sanityCheckCollection(network.edges());
    sanityCheckCollection(asGraph.edges());

    for (E edge : network.edges()) {
      // TODO(b/27817069): Consider verifying the edge's incident nodes in the string.
      assertThat(edgeString).contains(edge.toString());

      Endpoints<N> endpoints = network.incidentNodes(edge);
      N nodeA = endpoints.nodeA();
      N nodeB = endpoints.nodeB();
      assertThat(asGraph.edges()).contains(Endpoints.of(network, nodeA, nodeB));
      assertThat(network.edgesConnecting(nodeA, nodeB)).contains(edge);
      assertThat(network.successors(nodeA)).contains(nodeB);
      assertThat(network.adjacentNodes(nodeA)).contains(nodeB);
      assertThat(network.outEdges(nodeA)).contains(edge);
      assertThat(network.incidentEdges(nodeA)).contains(edge);
      assertThat(network.predecessors(nodeB)).contains(nodeA);
      assertThat(network.adjacentNodes(nodeB)).contains(nodeA);
      assertThat(network.inEdges(nodeB)).contains(edge);
      assertThat(network.incidentEdges(nodeB)).contains(edge);

      for (N incidentNode : ImmutableSet.of(
          network.incidentNodes(edge).nodeA(), network.incidentNodes(edge).nodeB())) {
        assertThat(network.nodes()).contains(incidentNode);
        for (E adjacentEdge : network.incidentEdges(incidentNode)) {
          assertTrue(edge.equals(adjacentEdge)
              || network.adjacentEdges(edge).contains(adjacentEdge));
        }
      }
    }

    for (N node : network.nodes()) {
      assertThat(nodeString).contains(node.toString());

      assertThat(network.adjacentNodes(node)).isEqualTo(asGraph.adjacentNodes(node));
      assertThat(network.predecessors(node)).isEqualTo(asGraph.predecessors(node));
      assertThat(network.successors(node)).isEqualTo(asGraph.successors(node));

      sanityCheckCollection(network.adjacentNodes(node));
      sanityCheckCollection(network.predecessors(node));
      sanityCheckCollection(network.successors(node));
      sanityCheckCollection(network.incidentEdges(node));
      sanityCheckCollection(network.inEdges(node));
      sanityCheckCollection(network.outEdges(node));

      if (network.isDirected()) {
        assertThat(network.degree(node)).isEqualTo(
            network.inEdges(node).size() + network.outEdges(node).size());
        assertThat(network.inDegree(node)).isEqualTo(network.inEdges(node).size());
        assertThat(network.outDegree(node)).isEqualTo(network.outEdges(node).size());
      } else {
        assertThat(network.degree(node)).isEqualTo(
            network.incidentEdges(node).size() + network.edgesConnecting(node, node).size());
        assertThat(network.inDegree(node)).isEqualTo(network.degree(node));
        assertThat(network.outDegree(node)).isEqualTo(network.degree(node));
      }

      for (N otherNode : network.nodes()) {
        Set<E> edgesConnecting = network.edgesConnecting(node, otherNode);
        boolean isSelfLoop = node.equals(otherNode);
        if (network.isDirected() || !isSelfLoop) {
          assertThat(edgesConnecting).isEqualTo(
              Sets.intersection(network.outEdges(node), network.inEdges(otherNode)));
        }
        if (!network.allowsParallelEdges()) {
          assertThat(edgesConnecting.size()).isAtMost(1);
        }
        if (!network.allowsSelfLoops() && isSelfLoop) {
          assertThat(edgesConnecting).isEmpty();
        }
        for (E edge : edgesConnecting) {
          assertThat(network.incidentNodes(edge)).isEqualTo(Endpoints.of(network, node, otherNode));
        }
      }

      for (E incidentEdge : network.incidentEdges(node)) {
        assertTrue(network.inEdges(node).contains(incidentEdge)
            || network.outEdges(node).contains(incidentEdge));
        assertThat(network.edges()).contains(incidentEdge);
        assertTrue(network.incidentNodes(incidentEdge).nodeA().equals(node)
            || network.incidentNodes(incidentEdge).nodeB().equals(node));
      }

      for (E inEdge : network.inEdges(node)) {
        assertThat(network.incidentEdges(node)).contains(inEdge);
        assertThat(network.outEdges(network.incidentNodes(inEdge).adjacentNode(node)))
            .contains(inEdge);
      }

      for (E outEdge : network.outEdges(node)) {
        assertThat(network.incidentEdges(node)).contains(outEdge);
        assertThat(network.inEdges(network.incidentNodes(outEdge).adjacentNode(node)))
            .contains(outEdge);
      }

      for (N adjacentNode : network.adjacentNodes(node)) {
        assertTrue(network.predecessors(node).contains(adjacentNode)
            || network.successors(node).contains(adjacentNode));
        assertTrue(!network.edgesConnecting(node, adjacentNode).isEmpty()
            || !network.edgesConnecting(adjacentNode, node).isEmpty());
      }

      for (N predecessor : network.predecessors(node)) {
        assertThat(network.successors(predecessor)).contains(node);
        assertThat(network.edgesConnecting(predecessor, node)).isNotEmpty();
      }

      for (N successor : network.successors(node)) {
        Set<E> edgesConnecting = network.edgesConnecting(node, successor);
        assertThat(network.predecessors(successor)).contains(node);
        assertThat(edgesConnecting).isNotEmpty();
        assertThat(edgesConnecting).isEqualTo(asGraph.edgeValue(node, successor));
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
   * Verifies that the {@code Set} returned by {@code incidentEdges} has the expected
   * mutability property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void incidentEdges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code adjacentNodes} has the expected
   * mutability property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void adjacentNodes_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code adjacentEdges} has the expected
   * mutability property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void adjacentEdges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code edgesConnecting} has the expected
   * mutability property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void edgesConnecting_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code inEdges} has the expected
   * mutability property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void inEdges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code outEdges} has the expected
   * mutability property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void outEdges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code predecessors} has the expected
   * mutability property (see the {@code Network} documentation for more information).
   */
  @Test
  public abstract void predecessors_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code successors} has the expected
   * mutability property (see the {@code Network} documentation for more information).
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
    addEdge(E12, N1, N2);
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
    addEdge(E12, N1, N2);
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
    try {
      network.incidentEdges(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void incidentNodes_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(network.incidentNodes(E12)).containsExactly(N1, N2);
  }

  @Test
  public void incidentNodes_edgeNotInGraph() {
    try {
      network.incidentNodes(EDGE_NOT_IN_GRAPH);
      fail(ERROR_EDGE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertEdgeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void adjacentNodes_oneEdge() {
    addEdge(E12, N1, N2);
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
    try {
      network.adjacentNodes(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void adjacentEdges_bothEndpoints() {
    addEdge(E12, N1, N2);
    addEdge(E23, N2, N3);
    addEdge(E31, N3, N1);
    addEdge(E34, N3, N4);
    assertThat(network.adjacentEdges(E12)).containsExactly(E31, E23);
  }

  @Test
  public void adjacentEdges_noAdjacentEdges() {
    addEdge(E12, N1, N2);
    addEdge(E34, N3, N4);
    assertThat(network.adjacentEdges(E12)).isEmpty();
  }

  @Test
  public void adjacentEdges_edgeNotInGraph() {
    try {
      network.adjacentEdges(EDGE_NOT_IN_GRAPH);
      fail(ERROR_EDGE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertEdgeNotInGraphErrorMessage(e);
    }
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
    try {
      network.edgesConnecting(N1, NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
    try {
      network.edgesConnecting(NODE_NOT_IN_GRAPH, N2);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
    try {
      network.edgesConnecting(NODE_NOT_IN_GRAPH, NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void inEdges_noInEdges() {
    addNode(N1);
    assertThat(network.inEdges(N1)).isEmpty();
  }

  @Test
  public void inEdges_nodeNotInGraph() {
    try {
      network.inEdges(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void outEdges_noOutEdges() {
    addNode(N1);
    assertThat(network.outEdges(N1)).isEmpty();
  }

  @Test
  public void outEdges_nodeNotInGraph() {
    try {
      network.outEdges(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void predecessors_noPredecessors() {
    addNode(N1);
    assertThat(network.predecessors(N1)).isEmpty();
  }

  @Test
  public void predecessors_nodeNotInGraph() {
    try {
      network.predecessors(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void successors_noSuccessors() {
    addNode(N1);
    assertThat(network.successors(N1)).isEmpty();
  }

  @Test
  public void successors_nodeNotInGraph() {
    try {
      network.successors(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void addNode_newNode() {
    assertTrue(addNode(N1));
    assertThat(network.nodes()).contains(N1);
  }

  @Test
  public void addNode_existingNode() {
    addNode(N1);
    ImmutableSet<Integer> nodes = ImmutableSet.copyOf(network.nodes());
    assertFalse(addNode(N1));
    assertThat(network.nodes()).containsExactlyElementsIn(nodes);
  }

  @Test
  public void removeNode_existingNode() {
    addEdge(E12, N1, N2);
    addEdge(E41, N4, N1);
    assertTrue(network.removeNode(N1));
    assertFalse(network.removeNode(N1));
    assertThat(network.nodes()).containsExactly(N2, N4);
    assertThat(network.edges()).doesNotContain(E12);
    assertThat(network.edges()).doesNotContain(E41);
  }

  @Test
  public void removeNode_nodeNotPresent() {
    addNode(N1);
    ImmutableSet<Integer> nodes = ImmutableSet.copyOf(network.nodes());
    assertFalse(network.removeNode(NODE_NOT_IN_GRAPH));
    assertThat(network.nodes()).containsExactlyElementsIn(nodes);
  }

  @Test
  public void removeNode_queryAfterRemoval() {
    addNode(N1);
    Set<Integer> unused = network.adjacentNodes(N1); // ensure cache (if any) is populated
    assertTrue(network.removeNode(N1));
    try {
      network.adjacentNodes(N1);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void removeEdge_existingEdge() {
    addEdge(E12, N1, N2);
    assertTrue(network.removeEdge(E12));
    assertFalse(network.removeEdge(E12));
    assertThat(network.edges()).doesNotContain(E12);
    assertThat(network.edgesConnecting(N1, N2)).isEmpty();
  }

  @Test
  public void removeEdge_oneOfMany() {
    addEdge(E12, N1, N2);
    addEdge(E13, N1, N3);
    addEdge(E14, N1, N4);
    assertThat(network.edges()).containsExactly(E12, E13, E14);
    assertTrue(network.removeEdge(E13));
    assertThat(network.edges()).containsExactly(E12, E14);
  }

  @Test
  public void removeEdge_edgeNotPresent() {
    addEdge(E12, N1, N2);
    ImmutableSet<String> edges = ImmutableSet.copyOf(network.edges());
    assertFalse(network.removeEdge(EDGE_NOT_IN_GRAPH));
    assertThat(network.edges()).containsExactlyElementsIn(edges);
  }

  @Test
  public void removeEdge_queryAfterRemoval() {
    addEdge(E12, N1, N2);
    Endpoints<Integer> unused = network.incidentNodes(E12); // ensure cache (if any) is populated
    assertTrue(network.removeEdge(E12));
    try {
      network.incidentNodes(E12);
      fail(ERROR_EDGE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertEdgeNotInGraphErrorMessage(e);
    }
  }

  static void assertNodeNotInGraphErrorMessage(Throwable throwable) {
    assertThat(throwable.getMessage()).startsWith(NODE_STRING);
    assertThat(throwable.getMessage()).contains(ERROR_ELEMENT_NOT_IN_GRAPH);
  }

  static void assertEdgeNotInGraphErrorMessage(Throwable throwable) {
    assertThat(throwable.getMessage()).startsWith(EDGE_STRING);
    assertThat(throwable.getMessage()).contains(ERROR_ELEMENT_NOT_IN_GRAPH);
  }
}
