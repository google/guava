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

import static com.google.common.graph.TestUtil.ERROR_NODE_NOT_IN_GRAPH;
import static com.google.common.graph.TestUtil.assertNodeNotInGraphErrorMessage;
import static com.google.common.graph.TestUtil.assertStronglyEquivalent;
import static com.google.common.graph.TestUtil.sanityCheckSet;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Abstract base class for testing implementations of {@link Graph} interface. Graph instances
 * created for testing should have Integer node and String edge objects.
 *
 * <p>Test cases that should be handled similarly in any graph implementation are included in this
 * class. For example, testing that {@code nodes()} method returns the set of the nodes in the
 * graph. The following test cases are left for the subclasses to handle:
 *
 * <ul>
 *   <li>Test cases related to whether the graph is directed or undirected.
 *   <li>Test cases related to the specific implementation of the {@link Graph} interface.
 * </ul>
 *
 * TODO(user): Make this class generic (using <N, E>) for all node and edge types.
 * TODO(user): Differentiate between directed and undirected edge strings.
 */
public abstract class AbstractGraphTest {

  Graph<Integer> graph;

  /**
   * The same reference as {@link #graph}, except as a mutable graph. This field is null in case
   * {@link #createGraph()} didn't return a mutable graph.
   */
  MutableGraph<Integer> graphAsMutableGraph;

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
  static final String ERROR_MODIFIABLE_SET = "Set returned is unexpectedly modifiable";
  static final String ERROR_SELF_LOOP = "self-loops are not allowed";
  static final String ERROR_ADDED_SELF_LOOP = "Should not be allowed to add a self-loop edge.";

  /** Creates and returns an instance of the graph to be tested. */
  abstract Graph<Integer> createGraph();

  /**
   * A proxy method that adds the node {@code n} to the graph being tested. In case of Immutable
   * graph implementations, this method should replace {@link #graph} with a new graph that includes
   * this node.
   */
  abstract void addNode(Integer n);

  /**
   * A proxy method that adds the edge {@code e} to the graph being tested. In case of Immutable
   * graph implementations, this method should replace {@link #graph} with a new graph that includes
   * this edge.
   */
  abstract void putEdge(Integer n1, Integer n2);

  final boolean graphIsMutable() {
    return graphAsMutableGraph != null;
  }

  @Before
  public final void init() {
    graph = createGraph();
    if (graph instanceof MutableGraph) {
      graphAsMutableGraph = (MutableGraph<Integer>) graph;
    }
  }

  @After
  public final void validateGraphState() {
    validateGraph(graph);
  }

  static <N> void validateGraph(Graph<N> graph) {
    assertStronglyEquivalent(graph, Graphs.copyOf(graph));
    assertStronglyEquivalent(graph, ImmutableGraph.copyOf(graph));

    String graphString = graph.toString();
    assertThat(graphString).contains("isDirected: " + graph.isDirected());
    assertThat(graphString).contains("allowsSelfLoops: " + graph.allowsSelfLoops());

    int nodeStart = graphString.indexOf("nodes:");
    int edgeStart = graphString.indexOf("edges:");
    String nodeString = graphString.substring(nodeStart, edgeStart);

    Set<EndpointPair<N>> allEndpointPairs = new HashSet<>();

    for (N node : sanityCheckSet(graph.nodes())) {
      assertThat(nodeString).contains(node.toString());

      if (graph.isDirected()) {
        assertThat(graph.degree(node)).isEqualTo(graph.inDegree(node) + graph.outDegree(node));
        assertThat(graph.predecessors(node)).hasSize(graph.inDegree(node));
        assertThat(graph.successors(node)).hasSize(graph.outDegree(node));
      } else {
        int selfLoopCount = graph.adjacentNodes(node).contains(node) ? 1 : 0;
        assertThat(graph.degree(node)).isEqualTo(graph.adjacentNodes(node).size() + selfLoopCount);
        assertThat(graph.predecessors(node)).isEqualTo(graph.adjacentNodes(node));
        assertThat(graph.successors(node)).isEqualTo(graph.adjacentNodes(node));
        assertThat(graph.inDegree(node)).isEqualTo(graph.degree(node));
        assertThat(graph.outDegree(node)).isEqualTo(graph.degree(node));
      }

      for (N adjacentNode : sanityCheckSet(graph.adjacentNodes(node))) {
        if (!graph.allowsSelfLoops()) {
          assertThat(node).isNotEqualTo(adjacentNode);
        }
        assertThat(
                graph.predecessors(node).contains(adjacentNode)
                    || graph.successors(node).contains(adjacentNode))
            .isTrue();
      }

      for (N predecessor : sanityCheckSet(graph.predecessors(node))) {
        assertThat(graph.successors(predecessor)).contains(node);
        assertThat(graph.hasEdgeConnecting(predecessor, node)).isTrue();
        assertThat(graph.incidentEdges(node)).contains(EndpointPair.of(graph, predecessor, node));
      }

      for (N successor : sanityCheckSet(graph.successors(node))) {
        allEndpointPairs.add(EndpointPair.of(graph, node, successor));
        assertThat(graph.predecessors(successor)).contains(node);
        assertThat(graph.hasEdgeConnecting(node, successor)).isTrue();
        assertThat(graph.incidentEdges(node)).contains(EndpointPair.of(graph, node, successor));
      }

      for (EndpointPair<N> endpoints : sanityCheckSet(graph.incidentEdges(node))) {
        if (graph.isDirected()) {
          assertThat(graph.hasEdgeConnecting(endpoints.source(), endpoints.target())).isTrue();
        } else {
          assertThat(graph.hasEdgeConnecting(endpoints.nodeU(), endpoints.nodeV())).isTrue();
        }
      }
    }

    sanityCheckSet(graph.edges());
    assertThat(graph.edges()).doesNotContain(EndpointPair.of(graph, new Object(), new Object()));
    assertThat(graph.edges()).isEqualTo(allEndpointPairs);
  }

  /**
   * Verifies that the {@code Set} returned by {@code nodes} has the expected mutability property
   * (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void nodes_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code adjacentNodes} has the expected mutability
   * property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void adjacentNodes_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code predecessors} has the expected mutability
   * property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void predecessors_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code successors} has the expected mutability
   * property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void successors_checkReturnedSetMutability();

  /**
   * Verifies that the {@code Set} returned by {@code incidentEdges} has the expected mutability
   * property (see the {@code Graph} documentation for more information).
   */
  @Test
  public abstract void incidentEdges_checkReturnedSetMutability();

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
    putEdge(N1, N2);
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
  public void incidentEdges_noIncidentEdges() {
    addNode(N1);
    assertThat(graph.incidentEdges(N1)).isEmpty();
  }

  @Test
  public void incidentEdges_nodeNotInGraph() {
    try {
      graph.incidentEdges(NODE_NOT_IN_GRAPH);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void degree_oneEdge() {
    putEdge(N1, N2);
    assertThat(graph.degree(N1)).isEqualTo(1);
    assertThat(graph.degree(N2)).isEqualTo(1);
  }

  @Test
  public void degree_isolatedNode() {
    addNode(N1);
    assertThat(graph.degree(N1)).isEqualTo(0);
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
    assertThat(graph.inDegree(N1)).isEqualTo(0);
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
    assertThat(graph.outDegree(N1)).isEqualTo(0);
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
    assume().that(graphIsMutable()).isTrue();

    assertThat(graphAsMutableGraph.addNode(N1)).isTrue();
    assertThat(graph.nodes()).contains(N1);
  }

  @Test
  public void addNode_existingNode() {
    assume().that(graphIsMutable()).isTrue();

    addNode(N1);
    ImmutableSet<Integer> nodes = ImmutableSet.copyOf(graph.nodes());
    assertThat(graphAsMutableGraph.addNode(N1)).isFalse();
    assertThat(graph.nodes()).containsExactlyElementsIn(nodes);
  }

  @Test
  public void removeNode_existingNode() {
    assume().that(graphIsMutable()).isTrue();

    putEdge(N1, N2);
    putEdge(N4, N1);
    assertThat(graphAsMutableGraph.removeNode(N1)).isTrue();
    assertThat(graphAsMutableGraph.removeNode(N1)).isFalse();
    assertThat(graph.nodes()).containsExactly(N2, N4);
    assertThat(graph.adjacentNodes(N2)).isEmpty();
    assertThat(graph.adjacentNodes(N4)).isEmpty();
  }

  @Test
  public void removeNode_antiparallelEdges() {
    assume().that(graphIsMutable()).isTrue();

    putEdge(N1, N2);
    putEdge(N2, N1);

    assertThat(graphAsMutableGraph.removeNode(N1)).isTrue();
    assertThat(graph.nodes()).containsExactly(N2);
    assertThat(graph.edges()).isEmpty();

    assertThat(graphAsMutableGraph.removeNode(N2)).isTrue();
    assertThat(graph.nodes()).isEmpty();
    assertThat(graph.edges()).isEmpty();
  }

  @Test
  public void removeNode_nodeNotPresent() {
    assume().that(graphIsMutable()).isTrue();

    addNode(N1);
    ImmutableSet<Integer> nodes = ImmutableSet.copyOf(graph.nodes());
    assertThat(graphAsMutableGraph.removeNode(NODE_NOT_IN_GRAPH)).isFalse();
    assertThat(graph.nodes()).containsExactlyElementsIn(nodes);
  }

  @Test
  public void removeNode_queryAfterRemoval() {
    assume().that(graphIsMutable()).isTrue();

    addNode(N1);
    @SuppressWarnings("unused")
    Set<Integer> unused = graph.adjacentNodes(N1); // ensure cache (if any) is populated
    assertThat(graphAsMutableGraph.removeNode(N1)).isTrue();
    try {
      graph.adjacentNodes(N1);
      fail(ERROR_NODE_NOT_IN_GRAPH);
    } catch (IllegalArgumentException e) {
      assertNodeNotInGraphErrorMessage(e);
    }
  }

  @Test
  public void removeEdge_existingEdge() {
    assume().that(graphIsMutable()).isTrue();

    putEdge(N1, N2);
    assertThat(graph.successors(N1)).containsExactly(N2);
    assertThat(graph.predecessors(N2)).containsExactly(N1);
    assertThat(graphAsMutableGraph.removeEdge(N1, N2)).isTrue();
    assertThat(graphAsMutableGraph.removeEdge(N1, N2)).isFalse();
    assertThat(graph.successors(N1)).isEmpty();
    assertThat(graph.predecessors(N2)).isEmpty();
  }

  @Test
  public void removeEdge_oneOfMany() {
    assume().that(graphIsMutable()).isTrue();

    putEdge(N1, N2);
    putEdge(N1, N3);
    putEdge(N1, N4);
    assertThat(graphAsMutableGraph.removeEdge(N1, N3)).isTrue();
    assertThat(graph.adjacentNodes(N1)).containsExactly(N2, N4);
  }

  @Test
  public void removeEdge_nodeNotPresent() {
    assume().that(graphIsMutable()).isTrue();

    putEdge(N1, N2);
    assertThat(graphAsMutableGraph.removeEdge(N1, NODE_NOT_IN_GRAPH)).isFalse();
    assertThat(graph.successors(N1)).contains(N2);
  }

  @Test
  public void removeEdge_edgeNotPresent() {
    assume().that(graphIsMutable()).isTrue();

    putEdge(N1, N2);
    addNode(N3);

    assertThat(graphAsMutableGraph.removeEdge(N1, N3)).isFalse();
    assertThat(graph.successors(N1)).contains(N2);
  }
}
