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

/** Tests for a directed {@link StandardMutableGraph}. */
@AndroidIncompatible
@RunWith(TestParameterInjector.class)
@NullUnmarked
public final class StandardImmutableDirectedGraphTest extends AbstractStandardDirectedGraphTest {

  private final boolean allowsSelfLoops;
  private ImmutableGraph.Builder<Integer> graphBuilder;

  public StandardImmutableDirectedGraphTest(@TestParameter boolean allowsSelfLoops) {
    this.allowsSelfLoops = allowsSelfLoops;
  }

  @Override
  Graph<Integer> createGraph() {
    graphBuilder = GraphBuilder.directed().allowsSelfLoops(allowsSelfLoops).immutable();
    return graphBuilder.build();
  }

  @Override
  void addNode(Integer n) {
    graphBuilder.addNode(n);
    graph = graphBuilder.build();
  }

  @Override
  void putEdge(Integer n1, Integer n2) {
    graphBuilder.putEdge(n1, n2);
    graph = graphBuilder.build();
  }
}
