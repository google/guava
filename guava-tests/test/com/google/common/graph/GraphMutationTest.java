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

/**
 * Tests for repeated node and edge addition and removal in a {@link Graph}.
 */
@RunWith(JUnit4.class)

public final class GraphMutationTest {
  private static final int NUM_TRIALS = 50;
  private static final int NUM_NODES = 100;
  private static final int NUM_EDGES = 1000;
  private static final int NODE_POOL_SIZE = 1000; // must be >> NUM_NODES

  @Test
  public void directedBasicGraph() {
    testBasicGraphMutation(BasicGraphBuilder.directed());
  }

  @Test
  public void undirectedBasicGraph() {
    testBasicGraphMutation(BasicGraphBuilder.undirected());
  }

  private static void testBasicGraphMutation(BasicGraphBuilder<? super Integer> graphBuilder) {
    Random gen = new Random(42); // Fixed seed so test results are deterministic.

    for (int trial = 0; trial < NUM_TRIALS; ++trial) {
      MutableBasicGraph<Integer> graph = graphBuilder.allowsSelfLoops(true).build();

      assertThat(graph.nodes()).isEmpty();
      assertThat(graph.edges()).isEmpty();
      AbstractGraphTest.validateGraph(graph);

      while (graph.nodes().size() < NUM_NODES) {
        graph.addNode(gen.nextInt(NODE_POOL_SIZE));
      }
      ArrayList<Integer> nodeList = new ArrayList<Integer>(graph.nodes());
      while (graph.edges().size() < NUM_EDGES) {
        graph.putEdge(getRandomElement(nodeList, gen), getRandomElement(nodeList, gen));
      }
      ArrayList<Endpoints<Integer>> edgeList = new ArrayList<Endpoints<Integer>>(graph.edges());

      assertThat(graph.nodes()).hasSize(NUM_NODES);
      assertThat(graph.edges()).hasSize(NUM_EDGES);
      AbstractGraphTest.validateGraph(graph);

      Collections.shuffle(edgeList, gen);
      int numEdgesToRemove = gen.nextInt(NUM_EDGES);
      for (int i = 0; i < numEdgesToRemove; ++i) {
        Endpoints<Integer> edge = edgeList.get(i);
        assertThat(graph.removeEdge(edge.nodeA(), edge.nodeB())).isTrue();
      }

      assertThat(graph.nodes()).hasSize(NUM_NODES);
      assertThat(graph.edges()).hasSize(NUM_EDGES - numEdgesToRemove);
      AbstractGraphTest.validateGraph(graph);

      Collections.shuffle(nodeList, gen);
      int numNodesToRemove = gen.nextInt(NUM_NODES);
      for (int i = 0; i < numNodesToRemove; ++i) {
        assertThat(graph.removeNode(nodeList.get(i))).isTrue();
      }

      assertThat(graph.nodes()).hasSize(NUM_NODES - numNodesToRemove);
      // Number of edges remaining is unknown (node's incident edges have been removed).
      AbstractGraphTest.validateGraph(graph);

      for (int i = numNodesToRemove; i < NUM_NODES; ++i) {
        assertThat(graph.removeNode(nodeList.get(i))).isTrue();
      }

      assertThat(graph.nodes()).isEmpty();
      assertThat(graph.edges()).isEmpty(); // no edges can remain if there's no nodes
      AbstractGraphTest.validateGraph(graph);

      Collections.shuffle(nodeList, gen);
      for (Integer node : nodeList) {
        assertThat(graph.addNode(node)).isTrue();
      }
      Collections.shuffle(edgeList, gen);
      for (Endpoints<Integer> edge : edgeList) {
        assertThat(graph.putEdge(edge.nodeA(), edge.nodeB())).isTrue();
      }

      assertThat(graph.nodes()).hasSize(NUM_NODES);
      assertThat(graph.edges()).hasSize(NUM_EDGES);
      AbstractGraphTest.validateGraph(graph);
    }
  }

  private static <L extends List<T> & RandomAccess, T> T getRandomElement(L list, Random gen) {
    return list.get(gen.nextInt(list.size()));
  }
}
