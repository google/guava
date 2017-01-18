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

/** Tests for {@link ImmutableNetwork}. */
@RunWith(JUnit4.class)
public class ImmutableNetworkTest {

  @Test
  public void immutableNetwork() {
    MutableNetwork<String, Integer> mutableNetwork = NetworkBuilder.directed().build();
    mutableNetwork.addNode("A");
    ImmutableNetwork<String, Integer> immutableNetwork = ImmutableNetwork.copyOf(mutableNetwork);

    assertThat(immutableNetwork.asGraph()).isInstanceOf(ImmutableGraph.class);
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

    assertThat(network2).isSameAs(network1);
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
}
