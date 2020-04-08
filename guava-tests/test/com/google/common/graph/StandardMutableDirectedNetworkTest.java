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

/** Tests for a directed {@link StandardMutableNetwork} allowing self-loops. */
@AndroidIncompatible
@RunWith(Parameterized.class)
public class StandardMutableDirectedNetworkTest extends AbstractStandardDirectedNetworkTest {

  @Parameters(name = "allowsSelfLoops={0}, nodeOrder={1}, edgeOrder={2}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(
        new Object[][] {
          {false, ElementOrder.insertion(), ElementOrder.insertion()},
          {true, ElementOrder.insertion(), ElementOrder.insertion()},
          {false, ElementOrder.sorted(Ordering.natural()), ElementOrder.sorted(Ordering.natural())},
        });
  }

  private final boolean allowsSelfLoops;
  private final ElementOrder<Integer> nodeOrder;
  private final ElementOrder<String> edgeOrder;

  public StandardMutableDirectedNetworkTest(
      boolean allowsSelfLoops, ElementOrder<Integer> nodeOrder, ElementOrder<String> edgeOrder) {
    this.allowsSelfLoops = allowsSelfLoops;
    this.nodeOrder = nodeOrder;
    this.edgeOrder = edgeOrder;
  }

  @Override
  MutableNetwork<Integer, String> createGraph() {
    return NetworkBuilder.directed()
        .allowsSelfLoops(allowsSelfLoops)
        .nodeOrder(nodeOrder)
        .edgeOrder(edgeOrder)
        .build();
  }

  @Override
  void addNode(Integer n) {
    networkAsMutableNetwork.addNode(n);
  }

  @Override
  void addEdge(Integer n1, Integer n2, String e) {
    networkAsMutableNetwork.addEdge(n1, n2, e);
  }
}
