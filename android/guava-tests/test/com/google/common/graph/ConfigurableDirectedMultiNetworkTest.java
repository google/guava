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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for a directed {@link ConfigurableMutableNetwork} allowing parallel edges and self-loops.
 */
@RunWith(JUnit4.class)
public class ConfigurableDirectedMultiNetworkTest extends ConfigurableDirectedNetworkTest {
  @Override
  public MutableNetwork<Integer, String> createGraph() {
    return NetworkBuilder.directed().allowsParallelEdges(true).allowsSelfLoops(true).build();
  }

  @Test
  public void adjacentEdges_parallelEdges() {
    addEdge(N1, N2, E12);
    addEdge(N1, N2, E12_A);
    addEdge(N1, N2, E12_B);
    addEdge(N3, N4, E34);
    assertThat(network.adjacentEdges(E12)).containsExactly(E12_A, E12_B);
  }

  @Test
  public void edgesConnecting_parallelEdges() {
    assertTrue(addEdge(N1, N2, E12));
    assertTrue(addEdge(N1, N2, E12_A));
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12, E12_A);
    // Passed nodes should be in the correct edge direction, first is the
    // source node and the second is the target node
    assertThat(network.edgesConnecting(N2, N1)).isEmpty();
  }

  @Test
  public void edgesConnecting_parallelSelfLoopEdges() {
    assertTrue(addEdge(N1, N1, E11));
    assertTrue(addEdge(N1, N1, E11_A));
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11, E11_A);
  }

  @Override
  @Test
  public void addEdge_parallelEdge() {
    assertTrue(addEdge(N1, N2, E12));
    assertTrue(addEdge(N1, N2, E12_A));
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12, E12_A);
  }

  @Override
  @Test
  public void addEdge_parallelSelfLoopEdge() {
    assertTrue(addEdge(N1, N1, E11));
    assertTrue(addEdge(N1, N1, E11_A));
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11, E11_A);
  }

  @Test
  public void removeEdge_parallelEdge() {
    addEdge(N1, N2, E12);
    addEdge(N1, N2, E12_A);
    assertTrue(network.removeEdge(E12_A));
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
  }

  @Test
  public void removeEdge_parallelSelfLoopEdge() {
    addEdge(N1, N1, E11);
    addEdge(N1, N1, E11_A);
    addEdge(N1, N2, E12);
    assertTrue(network.removeEdge(E11_A));
    assertThat(network.edgesConnecting(N1, N1)).containsExactly(E11);
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
    assertTrue(network.removeEdge(E11));
    assertThat(network.edgesConnecting(N1, N1)).isEmpty();
    assertThat(network.edgesConnecting(N1, N2)).containsExactly(E12);
  }
}
