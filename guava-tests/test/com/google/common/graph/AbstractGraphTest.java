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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
  MutableGraph<Integer> graph;
  static final Integer N1 = 1;
  static final Integer N2 = 2;
  static final Integer N3 = 3;
  static final Integer N4 = 4;
  static final Integer N5 = 5;
  static final Integer NODE_NOT_IN_GRAPH = 1000;

  // TODO(user): Consider separating Strings that we've defined here to capture
  // identifiable substrings of expected error messages, from Strings that we've defined
  // here to provide error messages.
  // TODO(user): Some Strings used in the subclasses can be added as static Strings
  // here too.
  static final String ERROR_ELEMENT_NOT_IN_GRAPH = "not an element of this graph";
  static final String NODE_STRING = "Node";
  static final String EDGE_STRING = "Edge";
  static final String ERROR_MODIFIABLE_SET = "Set returned is unexpectedly modifiable";
  static final String ERROR_SELF_LOOP = "self-loops are not allowed";
  static final String ERROR_NODE_NOT_IN_GRAPH =
      "Should not be allowed to pass a node that is not an element of the graph.";
  static final String ERROR_ADDED_SELF_LOOP = "Should not be allowed to add a self-loop edge.";
  static final String ERROR_ADDED_PARALLEL_EDGE = "Should not be allowed to add a parallel edge.";

  /**
   * Creates and returns an instance of the graph to be tested.
   */
  public abstract MutableGraph<Integer> createGraph();

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
    return graph.addNode(n);
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
  boolean addEdge(Integer n1, Integer n2) {
    graph.addNode(n1);
    graph.addNode(n2);
    return graph.addEdge(n1, n2);
  }

  @Before
  public void init() {
    graph = createGraph();
  }

  @After
  public void validateGraphState() {
    new EqualsTester().addEqualityGroup(
        graph,
        Graphs.copyOf(graph),
        ImmutableGraph.copyOf(graph)).testEquals();

    String graphString = graph.toString();
    assertThat(graphString).contains("isDirected: " + graph.isDirected());
    // TODO(b/28087289): add test for allowsParallelEdges when supported
    assertThat(graphString).contains("allowsSelfLoops: " + graph.allowsSelfLoops());

    int nodeStart = graphString.indexOf("nodes:");
    int edgeStart = graphString.indexOf("edges:");
    String nodeString = graphString.substring(nodeStart, edgeStart);

    for (Integer node : graph.nodes()) {
      assertThat(nodeString).contains(node.toString());

      for (Integer adjacentNode : graph.adjacentNodes(node)) {
        assertTrue(graph.predecessors(node).contains(adjacentNode)
            || graph.successors(node).contains(adjacentNode));
      }

      for (Integer predecessor : graph.predecessors(node)) {
        assertThat(graph.successors(predecessor)).contains(node);
      }

      for (Integer successor : graph.successors(node)) {
        assertThat(graph.predecessors(successor)).contains(node);
      }
    }
  }

  /**
   * Verifies that the {@code Set} returned by {@code nodes} has the expected mutability property
   * (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void nodes_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code adjacentNodes} has the expected
   * mutability property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void adjacentNodes_checkReturnedSetMutability();

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
  public void adjacentNodes_oneEdge() {
    addEdge(N1, N2);
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
      graph.adjacentNodes(NODE_NOT_IN_GRAPH);
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
      graph.predecessors(NODE_NOT_IN_GRAPH);
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
      graph.successors(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void degree_oneEdge() {
    addEdge(N1, N2);
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
      graph.degree(NODE_NOT_IN_GRAPH);
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
      graph.inDegree(NODE_NOT_IN_GRAPH);
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
      graph.outDegree(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void addNode_newNode() {
    assertTrue(addNode(N1));
    assertThat(graph.nodes()).contains(N1);
  }

  @Test
  public void addNode_existingNode() {
    addNode(N1);
    ImmutableSet<Integer> nodes = ImmutableSet.copyOf(graph.nodes());
    assertFalse(addNode(N1));
    assertThat(graph.nodes()).containsExactlyElementsIn(nodes);
  }

  @Test
  public void removeNode_existingNode() {
    addEdge(N1, N2);
    addEdge(N4, N1);
    assertTrue(graph.removeNode(N1));
    assertThat(graph.nodes()).containsExactly(N2, N4);
    assertThat(graph.adjacentNodes(N2)).isEmpty();
    assertThat(graph.adjacentNodes(N4)).isEmpty();
  }

  @Test
  public void removeNode_invalidArgument() {
    ImmutableSet<Integer> nodes = ImmutableSet.copyOf(graph.nodes());
    assertFalse(graph.removeNode(NODE_NOT_IN_GRAPH));
    assertThat(graph.nodes()).containsExactlyElementsIn(nodes);
  }

  @Test
  public void removeEdge_oneOfMany() {
    addEdge(N1, N2);
    addEdge(N1, N3);
    addEdge(N1, N4);
    assertTrue(graph.removeEdge(N1, N3));
    assertThat(graph.adjacentNodes(N1)).containsExactly(N2, N4);
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
