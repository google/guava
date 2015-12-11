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
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Abstract base class for testing immutable implementations of the {@link Graph}
 * interface.
 *
 * <p>This class is testing that all mutation methods called directly
 * on the immutable graph will throw {@code UnsupportedOperationException}. Also,
 * it tests the builder mutation methods {@code addNode} and {@code addEdge}.
 * Any other test cases should be either included in the superclasses or subclasses.
 *
 */
public abstract class AbstractImmutableGraphTest extends AbstractGraphTest {

  @Rule public final ExpectedException expectedException = ExpectedException.none();

  @Override
  public final void nodes_checkReturnedSetMutability() {
    expectedException.expect(UnsupportedOperationException.class);
    graph.nodes().add(N2);
  }

  @Override
  public final void edges_checkReturnedSetMutability() {
    expectedException.expect(UnsupportedOperationException.class);
    graph.edges().add(E12);
  }

  @Override
  public final void incidentEdges_checkReturnedSetMutability() {
    addNode(N1);
    expectedException.expect(UnsupportedOperationException.class);
    graph.incidentEdges(N1).add(E12);
  }

  @Override
  public final void incidentNodes_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.incidentNodes(E12).add(N2);
  }

  @Override
  public final void adjacentNodes_checkReturnedSetMutability() {
    addNode(N1);
    expectedException.expect(UnsupportedOperationException.class);
    graph.adjacentNodes(N1).add(N2);
  }

  @Override
  public final void adjacentEdges_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.adjacentEdges(E12).add(E23);
  }

  @Override
  public final void edgesConnecting_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.edgesConnecting(N1, N2).add(E23);
  }

  @Override
  public final void inEdges_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.inEdges(N2).add(E23);
  }

  @Override
  public final void outEdges_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.outEdges(N1).add(E23);
  }

  @Override
  public final void predecessors_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.predecessors(N2).add(N1);
  }

  @Override
  public final void successors_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.successors(N1).add(N2);
  }

  // We want to test calling the mutation methods directly on the graph,
  // hence the proxy methods are not needed. In case of immutable graphs,
  // proxy methods add nodes/edges to the builder then build a new graph.
  @Test
  public void addNode() {
    expectedException.expect(UnsupportedOperationException.class);
    graph.addNode(N3);
  }

  @Test
  public void addEdge() {
    expectedException.expect(UnsupportedOperationException.class);
    graph.addEdge(E13, N1, N3);
  }

  @Test
  public void removeNode() {
    expectedException.expect(UnsupportedOperationException.class);
    graph.removeNode(N1);
  }

  @Test
  public void removeEdge() {
    expectedException.expect(UnsupportedOperationException.class);
    graph.removeEdge(E12);
  }

  // Builder mutation methods

  @Test
  public void addNode_builder_newNode() {
    assertTrue(addNode(N1));
    assertThat(graph.nodes()).contains(N1);
  }
}
