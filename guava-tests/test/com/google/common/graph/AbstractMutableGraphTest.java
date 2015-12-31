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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

/**
 * Abstract base class for testing mutable implementations of {@link Graph}
 * interface.
 *
 * <p>This class is responsible for testing mutation methods. Some test cases are left
 * for the subclasses to handle:
 * <ul>
 * <li>Mutation test cases related to whether the graph is directed or undirected.
 * <li>Test cases related to the specific implementation of the mutation methods.
 * </ul>
 *
 */
public abstract class AbstractMutableGraphTest extends AbstractGraphTest {

  @Override
  final boolean addNode(Integer n) {
    return graph.addNode(n);
  }

  /**
   * Explicitly adds the supplied incident nodes to the graph (they need not be
   * elements of the graph), then adds the edge connecting them.
   */
  @Override
  final boolean addEdge(String e, Integer n1, Integer n2) {
    graph.addNode(n1);
    graph.addNode(n2);
    return graph.addEdge(e, n1, n2);
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
    addEdge(E12, N1, N2);
    addEdge(E41, N4, N1);
    assertTrue(graph.removeNode(N1));
    assertThat(graph.nodes()).containsExactly(N2, N4);
    assertThat(graph.edges()).doesNotContain(E12);
    assertThat(graph.edges()).doesNotContain(E41);
  }

  @Test
  public void removeNode_invalidArgument() {
    ImmutableSet<Integer> nodes = ImmutableSet.copyOf(graph.nodes());
    assertFalse(graph.removeNode(NODE_NOT_IN_GRAPH));
    assertThat(graph.nodes()).containsExactlyElementsIn(nodes);
  }

  @Test
  public void removeEdge_invalidArgument() {
    ImmutableSet<String> edges = ImmutableSet.copyOf(graph.edges());
    assertFalse(graph.removeEdge(EDGE_NOT_IN_GRAPH));
    assertThat(graph.edges()).containsExactlyElementsIn(edges);
  }
}
