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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

/**
 * Abstract base class for testing implementations of {@link Graph} interface. Graph
 * instances created for testing should have Integer node and String edge objects.
 *
 * <p>Tests assume the following about the graph implementation:
 * <ul>
 * <li>Parallel edges are not allowed.
 * </ul>
 *
 * <p>Test cases that should be handled similarly in any graph implementation are
 * included in this class. For example, testing that {@code nodes()} method returns
 * the set of the nodes in the graph. The following test cases are left for the subclasses
 * to handle:
 * <ul>
 * <li>Test cases related to whether the graph is directed, undirected, mutable,
 *     or immutable.
 * <li>Test cases related to the specific implementation of the {@link Graph} interface.
 * </ul>
 *
 * TODO(user): Make this class generic (using <N, E>) for all node and edge types.
 * TODO(user): Differentiate between directed and undirected edge strings.
 */
public abstract class AbstractGraphTest {
  Graph<Integer, String> graph;
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
  static final String E21 = "2-1";
  static final String E13 = "1-3";
  static final String E14 = "1-4";
  static final String E23 = "2-3";
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
  static final String ERROR_REUSE_EDGE = "it can't be reused to connect";
  static final String ERROR_MODIFIABLE_SET = "Set returned is unexpectedly modifiable";
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
  public abstract Graph<Integer, String> createGraph();

  /**
   * A proxy method that adds the node {@code n} to the graph being tested.
   * In case of Immutable graph implementations, this method should add {@code n} to the graph
   * builder and build a new graph with the current builder state.
   *
   * @return {@code true} iff the graph was modified as a result of this call
   * TODO(user): Consider changing access modifier to be protected.
   */
  abstract boolean addNode(Integer n);

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
  abstract boolean addEdge(String e, Integer n1, Integer n2);

  @Before
  public void init() {
    graph = createGraph();
  }

  /**
   * Verifies that the {@code Set} returned by {@code nodes} has the expected mutability property
   * (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void nodes_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code edges} has the expected mutability property
   * (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void edges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code incidentEdges} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void incidentEdges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code incidentNodes} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void incidentNodes_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code adjacentNodes} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void adjacentNodes_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code adjacentEdges} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void adjacentEdges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code edgesConnecting} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void edgesConnecting_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code inEdges} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void inEdges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code outEdges} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void outEdges_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code predecessors} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void predecessors_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code successors} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void successors_checkReturnedSetMutability();

  @Test
  public void nodes_oneNode() {
    addNode(N1);
    assertThat(graph.nodes()).containsExactly(N1);
  }

  @Test
  public void nodes_noNodes() {
    assertThat(graph.nodes()).isEmpty();
  }

  @Test
  public void edges_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(graph.edges()).containsExactly(E12);
  }

  @Test
  public void edges_noEdges() {
    assertThat(graph.edges()).isEmpty();
    // Graph with no edges, given disconnected nodes
    addNode(N1);
    addNode(N2);
    assertThat(graph.edges()).isEmpty();
  }

  @Test
  public void incidentEdges_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(graph.incidentEdges(N2)).containsExactly(E12);
    assertThat(graph.incidentEdges(N1)).containsExactly(E12);
  }

  @Test
  public void incidentEdges_isolatedNode() {
    addNode(N1);
    assertThat(graph.incidentEdges(N1)).isEmpty();
  }

  @Test
  public void incidentEdges_nodeNotInGraph() {
    try {
      Set<String> unused = graph.incidentEdges(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void incidentNodes_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(graph.incidentNodes(E12)).containsExactly(N1, N2);
  }

  @Test
  public void incidentNodes_edgeNotInGraph() {
    try {
      Set<Integer> unused = graph.incidentNodes(EDGE_NOT_IN_GRAPH);
      fail(ERROR_EDGE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertEdgeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void adjacentNodes_oneEdge() {
    addEdge(E12, N1, N2);
    assertThat(graph.adjacentNodes(N1)).containsExactly(N2);
    assertThat(graph.adjacentNodes(N2)).containsExactly(N1);
  }

  @Test
  public void adjacentNodes_noAdjacentNodes() {
    addNode(N1);
    assertThat(graph.adjacentNodes(N1)).isEmpty();
  }

  @Test
  public void adjacentNodes_nodeNotInGraph() {
    try {
      Set<Integer> unused = graph.adjacentNodes(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void adjacentEdges_addEdges() {
    addEdge(E12, N1, N2);
    addEdge(E13, N1, N3);
    addEdge(E23, N2, N3);
    assertThat(graph.adjacentEdges(E12)).containsExactly(E13, E23);
  }

  @Test
  public void adjacentEdges_noAdjacentEdges() {
    addEdge(E12, N1, N2);
    assertThat(graph.adjacentEdges(E12)).isEmpty();
  }

  @Test
  public void adjacentEdges_nodeNotInGraph() {
    try {
      Set<String> unused = graph.adjacentEdges(EDGE_NOT_IN_GRAPH);
      fail(ERROR_EDGE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertEdgeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void edgesConnecting_disconnectedNodes() {
    addNode(N1);
    addNode(N2);
    assertThat(graph.edgesConnecting(N1, N2)).isEmpty();
  }

  @Test
  public void edgesConnecting_nodesNotInGraph() {
    addNode(N1);
    addNode(N2);
    try {
      Set<String> unused = graph.edgesConnecting(N1, NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
    try {
      Set<String> unused = graph.edgesConnecting(NODE_NOT_IN_GRAPH, N2);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
    try {
      Set<String> unused = graph.edgesConnecting(NODE_NOT_IN_GRAPH, NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void inEdges_noInEdges() {
    addNode(N1);
    assertThat(graph.inEdges(N1)).isEmpty();
  }

  @Test
  public void inEdges_nodeNotInGraph() {
    try {
      Set<String> unused = graph.inEdges(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void outEdges_noOutEdges() {
    addNode(N1);
    assertThat(graph.outEdges(N1)).isEmpty();
  }

  @Test
  public void outEdges_nodeNotInGraph() {
    try {
      Set<String> unused = graph.outEdges(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void predecessors_noPredecessors() {
    addNode(N1);
    assertThat(graph.predecessors(N1)).isEmpty();
  }

  @Test
  public void predecessors_nodeNotInGraph() {
    try {
      Set<Integer> unused = graph.predecessors(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void successors_noSuccessors() {
    addNode(N1);
    assertThat(graph.successors(N1)).isEmpty();
  }

  @Test
  public void successors_nodeNotInGraph() {
    try {
      Set<Integer> unused = graph.successors(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void degree_oneEdge() {
    addEdge(E12, N1, N2);
    assertEquals(1, graph.degree(N1));
    assertEquals(1, graph.degree(N2));
  }

  @Test
  public void degree_isolatedNode() {
    addNode(N1);
    assertEquals(0, graph.degree(N1));
  }

  @Test
  public void degree_nodeNotInGraph() {
    try {
      long unused = graph.degree(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void inDegree_isolatedNode() {
    addNode(N1);
    assertEquals(0, graph.inDegree(N1));
  }

  @Test
  public void inDegree_nodeNotInGraph() {
    try {
      long unused = graph.inDegree(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void outDegree_isolatedNode() {
    addNode(N1);
    assertEquals(0, graph.outDegree(N1));
  }

  @Test
  public void outDegree_nodeNotInGraph() {
    try {
      long unused = graph.outDegree(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
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
