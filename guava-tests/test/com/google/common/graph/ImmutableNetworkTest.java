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
import static org.junit.Assert.assertThrows;

import org.jspecify.annotations.NullUnmarked;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ImmutableNetwork}. */
@RunWith(JUnit4.class)
@NullUnmarked
public class ImmutableNetworkTest {

  @Test
  public void immutableNetwork() {
    MutableNetwork<String, Integer> mutableNetwork = NetworkBuilder.directed().build();
    mutableNetwork.addNode("A");
    ImmutableNetwork<String, Integer> immutableNetwork = ImmutableNetwork.copyOf(mutableNetwork);

    assertThat(immutableNetwork).isNotInstanceOf(MutableNetwork.class);
    assertThat(immutableNetwork).isEqualTo(mutableNetwork);

    mutableNetwork.addNode("B");
    assertThat(immutableNetwork).isNotEqualTo(mutableNetwork);
  }

  @Test
  public void copyOfImmutableNetwork_optimized() {
    Network<String, String> network1 =
        ImmutableNetwork.copyOf(NetworkBuilder.directed().<String, String>build());
    Network<String, String> network2 = ImmutableNetwork.copyOf(network1);

    assertThat(network2).isSameInstanceAs(network1);
  }

  @Test
  public void edgesConnecting_directed() {
    MutableNetwork<String, String> mutableNetwork =
        NetworkBuilder.directed().allowsSelfLoops(true).build();
    mutableNetwork.addEdge("A", "A", "AA");
    mutableNetwork.addEdge("A", "B", "AB");
    Network<String, String> network = ImmutableNetwork.copyOf(mutableNetwork);

    assertThat(network.edgesConnecting("A", "A")).containsExactly("AA");
    assertThat(network.edgesConnecting("A", "B")).containsExactly("AB");
    assertThat(network.edgesConnecting("B", "A")).isEmpty();
  }

  @Test
  public void edgesConnecting_undirected() {
    MutableNetwork<String, String> mutableNetwork =
        NetworkBuilder.undirected().allowsSelfLoops(true).build();
    mutableNetwork.addEdge("A", "A", "AA");
    mutableNetwork.addEdge("A", "B", "AB");
    Network<String, String> network = ImmutableNetwork.copyOf(mutableNetwork);

    assertThat(network.edgesConnecting("A", "A")).containsExactly("AA");
    assertThat(network.edgesConnecting("A", "B")).containsExactly("AB");
    assertThat(network.edgesConnecting("B", "A")).containsExactly("AB");
  }

  @Test
  public void immutableNetworkBuilder_appliesNetworkBuilderConfig() {
    ImmutableNetwork<String, Integer> emptyNetwork =
        NetworkBuilder.directed()
            .allowsSelfLoops(true)
            .nodeOrder(ElementOrder.<String>natural())
            .<String, Integer>immutable()
            .build();

    assertThat(emptyNetwork.isDirected()).isTrue();
    assertThat(emptyNetwork.allowsSelfLoops()).isTrue();
    assertThat(emptyNetwork.nodeOrder()).isEqualTo(ElementOrder.<String>natural());
  }

  /**
   * Tests that the ImmutableNetwork.Builder doesn't change when the creating NetworkBuilder
   * changes.
   */
  @Test
  @SuppressWarnings("CheckReturnValue")
  public void immutableNetworkBuilder_copiesNetworkBuilder() {
    NetworkBuilder<String, Object> networkBuilder =
        NetworkBuilder.directed()
            .allowsSelfLoops(true)
            .<String>nodeOrder(ElementOrder.<String>natural());
    ImmutableNetwork.Builder<String, Integer> immutableNetworkBuilder =
        networkBuilder.<String, Integer>immutable();

    // Update NetworkBuilder, but this shouldn't impact immutableNetworkBuilder
    networkBuilder.allowsSelfLoops(false).nodeOrder(ElementOrder.<String>unordered());

    ImmutableNetwork<String, Integer> emptyNetwork = immutableNetworkBuilder.build();

    assertThat(emptyNetwork.isDirected()).isTrue();
    assertThat(emptyNetwork.allowsSelfLoops()).isTrue();
    assertThat(emptyNetwork.nodeOrder()).isEqualTo(ElementOrder.<String>natural());
  }

  @Test
  public void immutableNetworkBuilder_addNode() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().addNode("A").build();

    assertThat(network.nodes()).containsExactly("A");
    assertThat(network.edges()).isEmpty();
  }

  @Test
  public void immutableNetworkBuilder_putEdgeFromNodes() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().addEdge("A", "B", 10).build();

    assertThat(network.nodes()).containsExactly("A", "B");
    assertThat(network.edges()).containsExactly(10);
    assertThat(network.incidentNodes(10)).isEqualTo(EndpointPair.ordered("A", "B"));
  }

  @Test
  public void immutableNetworkBuilder_putEdgeFromEndpointPair() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed()
            .<String, Integer>immutable()
            .addEdge(EndpointPair.ordered("A", "B"), 10)
            .build();

    assertThat(network.nodes()).containsExactly("A", "B");
    assertThat(network.edges()).containsExactly(10);
    assertThat(network.incidentNodes(10)).isEqualTo(EndpointPair.ordered("A", "B"));
  }

  @Test
  public void emptyNetwork_nodes_unmodifiable() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().build();

    assertThrows(UnsupportedOperationException.class, () -> network.nodes().add("A"));
  }

  @Test
  public void emptyNetwork_edges_unmodifiable() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().build();

    assertThrows(UnsupportedOperationException.class, () -> network.edges().add(10));
  }

  @Test
  public void nonEmptyNetwork_nodes_unmodifiable() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().addNode("A").addNode("B").build();

    assertThrows(UnsupportedOperationException.class, () -> network.nodes().add("C"));
    assertThrows(UnsupportedOperationException.class, () -> network.nodes().remove("A"));
  }

  @Test
  public void nonEmptyNetwork_edges_unmodifiable() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().addEdge("A", "B", 10).build();

    assertThrows(UnsupportedOperationException.class, () -> network.edges().add(20));
    assertThrows(UnsupportedOperationException.class, () -> network.edges().remove(10));
  }

  @Test
  public void nonEmptyNetwork_adjacentNodes_unmodifiable() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().addEdge("A", "B", 10).build();

    assertThrows(UnsupportedOperationException.class, () -> network.adjacentNodes("A").add("C"));
    assertThrows(UnsupportedOperationException.class, () -> network.adjacentNodes("A").remove("B"));
  }

  @Test
  public void nonEmptyNetwork_predecessors_unmodifiable() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().addEdge("A", "B", 10).build();

    assertThrows(UnsupportedOperationException.class, () -> network.predecessors("B").add("C"));
    assertThrows(UnsupportedOperationException.class, () -> network.predecessors("B").remove("A"));
  }

  @Test
  public void nonEmptyNetwork_successors_unmodifiable() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().addEdge("A", "B", 10).build();

    assertThrows(UnsupportedOperationException.class, () -> network.successors("A").add("C"));
    assertThrows(UnsupportedOperationException.class, () -> network.successors("A").remove("B"));
  }

  @Test
  public void nonEmptyNetwork_incidentEdges_unmodifiable() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().addEdge("A", "B", 10).build();

    assertThrows(UnsupportedOperationException.class, () -> network.incidentEdges("A").add(20));
    assertThrows(UnsupportedOperationException.class, () -> network.incidentEdges("A").remove(10));
  }

  @Test
  public void nonEmptyNetwork_nodes_clear() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().addNode("A").build();

    assertThrows(UnsupportedOperationException.class, () -> network.nodes().clear());
  }

  @Test
  public void nonEmptyNetwork_edges_clear() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().addEdge("A", "B", 10).build();

    assertThrows(UnsupportedOperationException.class, () -> network.edges().clear());
  }

  @Test
  public void nonEmptyNetwork_nodes_iteratorRemove() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().addNode("A").build();

    java.util.Iterator<String> iterator = network.nodes().iterator();
    iterator.next();
    assertThrows(UnsupportedOperationException.class, () -> iterator.remove());
  }

  @Test
  public void nonEmptyNetwork_edges_iteratorRemove() {
    ImmutableNetwork<String, Integer> network =
        NetworkBuilder.directed().<String, Integer>immutable().addEdge("A", "B", 10).build();

    java.util.Iterator<Integer> iterator = network.edges().iterator();
    iterator.next();
    assertThrows(UnsupportedOperationException.class, () -> iterator.remove());
  }
}
