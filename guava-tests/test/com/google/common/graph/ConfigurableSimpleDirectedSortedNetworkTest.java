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

import static com.google.common.graph.ElementOrder.sorted;

import com.google.common.collect.Ordering;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for a directed {@link ConfigurableMutableNetwork}, creating a simple directed sorted graph
 * (parallel and self-loop edges are not allowed).
 *
 * <p>The main purpose of this class is to run the inherited {@link #concurrentIteration} test
 * against a sorted graph so as to cover {@link MapRetrievalCache}.
 */
@RunWith(JUnit4.class)
public class ConfigurableSimpleDirectedSortedNetworkTest
    extends ConfigurableSimpleDirectedNetworkTest {

  @Override
  public MutableNetwork<Integer, String> createGraph() {
    return NetworkBuilder.directed()
        .allowsParallelEdges(false)
        .allowsSelfLoops(false)
        .edgeOrder(sorted(Ordering.natural()))
        .nodeOrder(sorted(Ordering.natural()))
        .build();
  }

  @Override
  public void addEdge_nodesNotInGraph() {
    /*
     * Skip this test because the expected ordering is different here than in the superclass because
     * of sorting.
     *
     * TODO(cpovirk): Implement this to check for the proper order.
     */
  }
}
