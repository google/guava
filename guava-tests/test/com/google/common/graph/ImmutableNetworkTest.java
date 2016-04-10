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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ImmutableNetwork}.
 */
@RunWith(JUnit4.class)
public class ImmutableNetworkTest {

  @Test
  public void copyOfImmutableNetwork_optimized() {
    Network<String, String> graph1 = ImmutableNetwork.copyOf(
        NetworkBuilder.directed().<String, String>build());
    Network<String, String> graph2 = ImmutableNetwork.copyOf(graph1);

    assertThat(graph2).isSameAs(graph1);
  }

  @Test
  public void edgesConnecting_directed() {
    MutableNetwork<String, String> mutableGraph = NetworkBuilder.directed().build();
    mutableGraph.addEdge("AA", "A", "A");
    mutableGraph.addEdge("AB", "A", "B");
    Network<String, String> graph = ImmutableNetwork.copyOf(mutableGraph);

    assertThat(graph.edgesConnecting("A", "A")).containsExactly("AA");
    assertThat(graph.edgesConnecting("A", "B")).containsExactly("AB");
    assertThat(graph.edgesConnecting("B", "A")).isEmpty();
  }

  @Test
  public void edgesConnecting_undirected() {
    MutableNetwork<String, String> mutableGraph = NetworkBuilder.undirected().build();
    mutableGraph.addEdge("AA", "A", "A");
    mutableGraph.addEdge("AB", "A", "B");
    Network<String, String> graph = ImmutableNetwork.copyOf(mutableGraph);

    assertThat(graph.edgesConnecting("A", "A")).containsExactly("AA");
    assertThat(graph.edgesConnecting("A", "B")).containsExactly("AB");
    assertThat(graph.edgesConnecting("B", "A")).containsExactly("AB");
  }
}
