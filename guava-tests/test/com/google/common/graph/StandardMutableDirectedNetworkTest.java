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

import com.google.common.graph.TestUtil.NetworkType;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import org.jspecify.annotations.NullUnmarked;
import org.junit.runner.RunWith;

/** Tests for a directed {@link StandardMutableNetwork} allowing self-loops. */
@AndroidIncompatible
@RunWith(TestParameterInjector.class)
@NullUnmarked
public class StandardMutableDirectedNetworkTest extends AbstractStandardDirectedNetworkTest {

  private final NetworkType networkType;

  public StandardMutableDirectedNetworkTest(@TestParameter NetworkType networkType) {
    this.networkType = networkType;
  }

  @Override
  MutableNetwork<Integer, String> createGraph() {
    return networkType.configure(NetworkBuilder.directed()).build();
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
