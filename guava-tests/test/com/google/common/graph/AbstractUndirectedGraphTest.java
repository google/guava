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

import com.google.common.testing.EqualsTester;

import org.junit.After;
import org.junit.Test;

/**
 * Abstract base class for testing undirected implementations of the {@link Graph} interface.
 *
 * <p>This class is responsible for testing that an undirected implementation of {@link Graph}
 * is correctly handling undirected edges.  Implementation-dependent test cases are left to
 * subclasses. Test cases that do not require the graph to be undirected are found in superclasses.
 */
public abstract class AbstractUndirectedGraphTest extends AbstractGraphTest {

  @After
  public void validateUndirectedEdges() {
    for (Integer node : graph.nodes()) {
      new EqualsTester()
          .addEqualityGroup(graph.predecessors(node), graph.successors(node),
              graph.adjacentNodes(node))
          .testEquals();
    }
  }

  @Test
  public void predecessors_oneEdge() {
    addEdge(N1, N2);
    assertThat(graph.predecessors(N2)).containsExactly(N1);
    assertThat(graph.predecessors(N1)).containsExactly(N2);
  }

  @Test
  public void successors_oneEdge() {
    addEdge(N1, N2);
    assertThat(graph.successors(N1)).containsExactly(N2);
    assertThat(graph.successors(N2)).containsExactly(N1);
  }

  @Test
  public void inDegree_oneEdge() {
    addEdge(N1, N2);
    assertEquals(1, graph.inDegree(N2));
    assertEquals(1, graph.inDegree(N1));
  }

  @Test
  public void outDegree_oneEdge() {
    addEdge(N1, N2);
    assertEquals(1, graph.outDegree(N1));
    assertEquals(1, graph.outDegree(N2));
  }

  // Element Mutation

  @Test
  public void addEdge_existingNodes() {
    // Adding nodes initially for safety (insulating from possible future
    // modifications to proxy methods)
    addNode(N1);
    addNode(N2);
    assertTrue(addEdge(N1, N2));
  }

  @Test
  public void addEdge_existingEdgeBetweenSameNodes() {
    addEdge(N1, N2);
    assertFalse(addEdge(N2, N1));
  }

  @Test
  public void removeEdge_existingEdge() {
    addEdge(N1, N2);
    assertThat(graph.successors(N1)).containsExactly(N2);
    assertThat(graph.predecessors(N2)).containsExactly(N1);
    assertTrue(graph.removeEdge(N1, N2));
    assertThat(graph.successors(N1)).isEmpty();
    assertThat(graph.predecessors(N2)).isEmpty();
  }
}
