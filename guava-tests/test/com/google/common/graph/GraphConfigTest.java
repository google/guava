/*
 * Copyright (C) 2015 The Guava Authors
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

import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class GraphConfigTest {

  @Test
  public void testEquals() throws Exception {
    GraphConfig configA1 = Graphs.config().expectedNodeCount(5);
    GraphConfig configA2 = Graphs.config().expectedNodeCount(5);

    GraphConfig configB = Graphs.config().expectedNodeCount(10);

    new EqualsTester()
        .addEqualityGroup(configA1, configA2)
        .addEqualityGroup(configB)
        .testEquals();
  }

  @Test
  public void toString_selfLoops() {
    assertThat(Graphs.config().toString())
        .isEqualTo("self-loops allowed");
  }

  @Test
  public void toString_noSelfLoops() {
    assertThat(Graphs.config().noSelfLoops().toString())
        .isEqualTo("self-loops disallowed");
  }

  @Test
  public void toString_selfLoops_multigraph() {
    assertThat(Graphs.config().multigraph().toString())
        .isEqualTo("multigraph,self-loops allowed");
  }

  @Test
  public void toString_noSelfLoops_multigraph() {
    assertThat(Graphs.config().noSelfLoops().multigraph().toString())
        .isEqualTo("multigraph,self-loops disallowed");
  }
}
