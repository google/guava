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

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import org.jspecify.annotations.NullUnmarked;
import org.junit.runner.RunWith;

/** Tests for an undirected {@link StandardMutableGraph}. */
@AndroidIncompatible
@RunWith(TestParameterInjector.class)
@NullUnmarked
public class StandardMutableUndirectedGraphTest extends AbstractStandardUndirectedGraphTest {

  private enum IncidentEdgeOrder {
    UNORDERED(ElementOrder.unordered()),
    STABLE(ElementOrder.stable());

    final ElementOrder<Integer> elementOrder;

    IncidentEdgeOrder(ElementOrder<Integer> elementOrder) {
      this.elementOrder = elementOrder;
    }
  }

  private final boolean allowsSelfLoops;
  private final ElementOrder<Integer> incidentEdgeOrder;

  public StandardMutableUndirectedGraphTest(
      @TestParameter boolean allowsSelfLoops, @TestParameter IncidentEdgeOrder incidentEdgeOrder) {
    this.allowsSelfLoops = allowsSelfLoops;
    this.incidentEdgeOrder = incidentEdgeOrder.elementOrder;
  }

  @Override
  MutableGraph<Integer> createGraph() {
    return GraphBuilder.undirected()
        .allowsSelfLoops(allowsSelfLoops)
        .incidentEdgeOrder(incidentEdgeOrder)
        .build();
  }

  @Override
  final void addNode(Integer n) {
    graphAsMutableGraph.addNode(n);
  }

  @Override
  final void putEdge(Integer n1, Integer n2) {
    graphAsMutableGraph.putEdge(n1, n2);
  }
}
