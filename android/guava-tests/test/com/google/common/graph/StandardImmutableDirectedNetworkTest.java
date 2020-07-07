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

import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Collection;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for a directed {@link ImmutableNetwork}. */
@AndroidIncompatible
@RunWith(Parameterized.class)
public class StandardImmutableDirectedNetworkTest extends AbstractStandardDirectedNetworkTest {

  @Parameters(name = "allowsSelfLoops={0}, allowsParallelEdges={1}, nodeOrder={2}, edgeOrder={3}")
  public static Collection<Object[]> parameters() {
    ElementOrder<?> naturalElementOrder = ElementOrder.sorted(Ordering.natural());

    return Arrays.asList(
        new Object[][] {
          {false, false, ElementOrder.insertion(), ElementOrder.insertion()},
          {true, false, ElementOrder.insertion(), ElementOrder.insertion()},
          {false, false, naturalElementOrder, naturalElementOrder},
          {true, true, ElementOrder.insertion(), ElementOrder.insertion()},
        });
  }

  private final boolean allowsSelfLoops;
  private final boolean allowsParallelEdges;
  private final ElementOrder<Integer> nodeOrder;
  private final ElementOrder<String> edgeOrder;

  private ImmutableNetwork.Builder<Integer, String> networkBuilder;

  public StandardImmutableDirectedNetworkTest(
      boolean allowsSelfLoops,
      boolean allowsParallelEdges,
      ElementOrder<Integer> nodeOrder,
      ElementOrder<String> edgeOrder) {
    this.allowsSelfLoops = allowsSelfLoops;
    this.allowsParallelEdges = allowsParallelEdges;
    this.nodeOrder = nodeOrder;
    this.edgeOrder = edgeOrder;
  }

  @Override
  Network<Integer, String> createGraph() {
    networkBuilder =
        NetworkBuilder.directed()
            .allowsSelfLoops(allowsSelfLoops)
            .allowsParallelEdges(allowsParallelEdges)
            .nodeOrder(nodeOrder)
            .edgeOrder(edgeOrder)
            .immutable();

    return networkBuilder.build();
  }

  @Override
  void addNode(Integer n) {
    networkBuilder.addNode(n);
    network = networkBuilder.build();
  }

  @Override
  void addEdge(Integer n1, Integer n2, String e) {
    networkBuilder.addEdge(n1, n2, e);
    network = networkBuilder.build();
  }
}
