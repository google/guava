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

import java.util.Arrays;
import java.util.Collection;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for an undirected {@link ConfigurableMutableGraph}. */
@AndroidIncompatible
@RunWith(Parameterized.class)
public final class StandardImmutableUndirectedGraphTest
    extends AbstractStandardUndirectedGraphTest {

  @Parameters(name = "allowsSelfLoops={0}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{false}, {true}});
  }

  private final boolean allowsSelfLoops;
  private ImmutableGraph.Builder<Integer> graphBuilder;

  public StandardImmutableUndirectedGraphTest(boolean allowsSelfLoops) {
    this.allowsSelfLoops = allowsSelfLoops;
  }

  @Override
  boolean allowsSelfLoops() {
    return allowsSelfLoops;
  }

  @Override
  ElementOrder<Integer> incidentEdgeOrder() {
    return ElementOrder.stable();
  }

  @Override
  public Graph<Integer> createGraph() {
    graphBuilder = GraphBuilder.undirected().allowsSelfLoops(allowsSelfLoops()).immutable();
    return graphBuilder.build();
  }

  @Override
  final void addNode(Integer n) {
    graphBuilder.addNode(n);
    graph = graphBuilder.build();
  }

  @Override
  final void putEdge(Integer n1, Integer n2) {
    graphBuilder.putEdge(n1, n2);
    graph = graphBuilder.build();
  }
}
