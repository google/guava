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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Abstract base class for testing immutable implementations of the {@link UndirectedGraph}
 * interface.
 *
 * <p>This class is testing that all mutation methods called directly
 * on the immutable graph will throw {@code UnsupportedOperationException}. Also,
 * it tests the builder mutation methods {@code addNode} and {@code addEdge}.
 * Any other test cases should be either included in the superclasses or subclasses.
 *
 */
public abstract class AbstractImmutableUndirectedGraphTest extends AbstractUndirectedGraphTest {
  @Rule public final ExpectedException expectedException = ExpectedException.none();

  @Override
  @Test
  public final void nodes_checkReturnedSetMutability() {
    expectedException.expect(UnsupportedOperationException.class);
    graph.nodes().add(N2);
  }

  @Override
  @Test
  public final void edges_checkReturnedSetMutability() {
    expectedException.expect(UnsupportedOperationException.class);
    graph.edges().add(E12);
  }

  @Override
  @Test
  public final void incidentEdges_checkReturnedSetMutability() {
    addNode(N1);
    expectedException.expect(UnsupportedOperationException.class);
    graph.incidentEdges(N1).add(E12);
  }

  @Override
  @Test
  public final void incidentNodes_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.incidentNodes(E12).add(N2);
  }

  @Override
  @Test
  public final void adjacentNodes_checkReturnedSetMutability() {
    addNode(N1);
    expectedException.expect(UnsupportedOperationException.class);
    graph.adjacentNodes(N1).add(N2);
  }

  @Override
  @Test
  public final void adjacentEdges_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.adjacentEdges(E12).add(E23);
  }

  @Override
  @Test
  public final void edgesConnecting_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.edgesConnecting(N1, N2).add(E23);
  }

  @Override
  @Test
  public final void inEdges_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.inEdges(N2).add(E23);
  }

  @Override
  @Test
  public final void outEdges_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.outEdges(N1).add(E23);
  }

  @Override
  @Test
  public final void predecessors_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.predecessors(N2).add(N1);
  }

  @Override
  @Test
  public final void successors_checkReturnedSetMutability() {
    addEdge(E12, N1, N2);
    expectedException.expect(UnsupportedOperationException.class);
    graph.successors(N1).add(N2);
  }

  // Builder mutation methods only support addition, not removal, so these tests would fail.
  @Override
  @Test
  public void removeNode_existingNode() {
    expectedException.expect(UnsupportedOperationException.class);
    super.removeNode_existingNode();
  }

  @Override
  @Test
  public void removeNode_invalidArgument() {
    expectedException.expect(UnsupportedOperationException.class);
    super.removeNode_invalidArgument();
  }

  @Override
  @Test
  public void removeEdge_existingEdge() {
    expectedException.expect(UnsupportedOperationException.class);
    super.removeEdge_existingEdge();
  }

  @Override
  @Test
  public void removeEdge_oneOfMany() {
    expectedException.expect(UnsupportedOperationException.class);
    super.removeEdge_oneOfMany();
  }

  @Override
  @Test
  public void removeEdge_invalidArgument() {
    expectedException.expect(UnsupportedOperationException.class);
    super.removeEdge_invalidArgument();
  }

  // Test that adding to the graph directly (as opposed to via the proxy methods) is not supported.
  @Test
  public void addNode_immutable() {
    expectedException.expect(UnsupportedOperationException.class);
    graph.addNode(N3);
  }

  @Test
  public void addEdge_immutable() {
    expectedException.expect(UnsupportedOperationException.class);
    graph.addEdge(E13, N1, N3);
  }
}
