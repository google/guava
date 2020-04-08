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

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for a directed {@link StandardMutableNetwork} allowing parallel edges and self-loops. */
@RunWith(JUnit4.class)
public class StandardDirectedMultiNetworkTest extends AbstractStandardDirectedNetworkTest {

  @Override
  MutableNetwork<Integer, String> createGraph() {
    return NetworkBuilder.directed().allowsParallelEdges(true).allowsSelfLoops(true).build();
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
