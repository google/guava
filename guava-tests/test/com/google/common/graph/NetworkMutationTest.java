/*
 * Copyright (C) 2016 The Guava Authors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.RandomAccess;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for repeated node and edge addition and removal in a {@link Network}. */
@RunWith(JUnit4.class)

public final class NetworkMutationTest {
  private static final int NUM_TRIALS = 25;
  private static final int NUM_NODES = 100;
  private static final int NUM_EDGES = 1000;
  private static final int NODE_POOL_SIZE = 1000; // must be >> NUM_NODES

  @Test
  public void directedNetwork() {
    testNetworkMutation(NetworkBuilder.directed());
  }

  @Test
  public void undirectedNetwork() {
    testNetworkMutation(NetworkBuilder.undirected());
  }

  private static void testNetworkMutation(NetworkBuilder<? super Integer, Object> networkBuilder) {
    Random gen = new Random(42); // Fixed seed so test results are deterministic.

    for (int trial = 0; trial < NUM_TRIALS; ++trial) {
      MutableNetwork<Integer, Object> network =
          networkBuilder.allowsParallelEdges(true).allowsSelfLoops(true).build();

      assertThat(network.nodes()).isEmpty();
      assertThat(network.edges()).isEmpty();
      AbstractNetworkTest.validateNetwork(network);

      while (network.nodes().size() < NUM_NODES) {
        network.addNode(gen.nextInt(NODE_POOL_SIZE));
      }
      ArrayList<Integer> nodeList = new ArrayList<>(network.nodes());
      for (int i = 0; i < NUM_EDGES; ++i) {
        // Parallel edges are allowed, so this should always succeed.
        assertThat(
                network.addEdge(
                    getRandomElement(nodeList, gen), getRandomElement(nodeList, gen), new Object()))
            .isTrue();
      }
      ArrayList<Object> edgeList = new ArrayList<>(network.edges());

      assertThat(network.nodes()).hasSize(NUM_NODES);
      assertThat(network.edges()).hasSize(NUM_EDGES);
      AbstractNetworkTest.validateNetwork(network);

      Collections.shuffle(edgeList, gen);
      int numEdgesToRemove = gen.nextInt(NUM_EDGES);
      for (int i = 0; i < numEdgesToRemove; ++i) {
        Object edge = edgeList.get(i);
        assertThat(network.removeEdge(edge)).isTrue();
      }

      assertThat(network.nodes()).hasSize(NUM_NODES);
      assertThat(network.edges()).hasSize(NUM_EDGES - numEdgesToRemove);
      AbstractNetworkTest.validateNetwork(network);

      Collections.shuffle(nodeList, gen);
      int numNodesToRemove = gen.nextInt(NUM_NODES);
      for (int i = 0; i < numNodesToRemove; ++i) {
        assertThat(network.removeNode(nodeList.get(i))).isTrue();
      }

      assertThat(network.nodes()).hasSize(NUM_NODES - numNodesToRemove);
      // Number of edges remaining is unknown (node's incident edges have been removed).
      AbstractNetworkTest.validateNetwork(network);

      for (int i = numNodesToRemove; i < NUM_NODES; ++i) {
        assertThat(network.removeNode(nodeList.get(i))).isTrue();
      }

      assertThat(network.nodes()).isEmpty();
      assertThat(network.edges()).isEmpty(); // no edges can remain if there's no nodes
      AbstractNetworkTest.validateNetwork(network);

      Collections.shuffle(nodeList, gen);
      for (Integer node : nodeList) {
        assertThat(network.addNode(node)).isTrue();
      }
      Collections.shuffle(edgeList, gen);
      for (Object edge : edgeList) {
        assertThat(
                network.addEdge(
                    getRandomElement(nodeList, gen), getRandomElement(nodeList, gen), edge))
            .isTrue();
      }

      assertThat(network.nodes()).hasSize(NUM_NODES);
      assertThat(network.edges()).hasSize(NUM_EDGES);
      AbstractNetworkTest.validateNetwork(network);
    }
  }

  private static <L extends List<T> & RandomAccess, T> T getRandomElement(L list, Random gen) {
    return list.get(gen.nextInt(list.size()));
  }
}
