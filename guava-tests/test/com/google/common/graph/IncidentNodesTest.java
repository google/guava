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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link IncidentNodes}.
 *
 * TODO(b/24415223): Consider SetTestSuiteBuilder once this supports > 2 nodes (i.e. hypergraphs).
 */
@RunWith(JUnit4.class)
public final class IncidentNodesTest {

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            IncidentNodes.of("foo", "bar"),
            IncidentNodes.of("bar", "foo"),
            IncidentNodes.of(ImmutableSet.of("foo", "bar")),
            IncidentNodes.of(ImmutableSet.of("bar", "foo")))
        .addEqualityGroup(
            IncidentNodes.of("test", "test"),
            IncidentNodes.of(ImmutableSet.of("test")))
        .testEquals();
  }

  @Test
  public void testNodes_basic() {
    IncidentNodes<String> incidentNodes = IncidentNodes.of("foo", "bar");
    assertThat(incidentNodes.node1()).isEqualTo("foo");
    assertThat(incidentNodes.node2()).isEqualTo("bar");
  }

  @Test
  public void testNodes_orderedSet() {
    IncidentNodes<String> incidentNodes = IncidentNodes.of(ImmutableSet.of("foo", "bar"));
    // An ImmutableSet preserves order, so we should see that order in node1 and node2.
    assertThat(incidentNodes.node1()).isEqualTo("foo");
    assertThat(incidentNodes.node2()).isEqualTo("bar");
  }

  @Test
  public void testNodes_selfLoop() {
    IncidentNodes<String> incidentNodes = IncidentNodes.of(ImmutableSet.of("test"));
    assertThat(incidentNodes.node1()).isEqualTo("test");
    assertThat(incidentNodes.node2()).isEqualTo("test");
  }

  @Test
  public void testSet_basic() {
    IncidentNodes<String> incidentNodes = IncidentNodes.of("source", "target");
    assertThat(incidentNodes).containsExactly("source", "target").inOrder();
    new EqualsTester().addEqualityGroup(incidentNodes, ImmutableSet.of("source", "target"))
        .testEquals();
  }

  @Test
  public void testSet_selfLoop() {
    // Allocate new strings to ensure that equals() equality is used instead of reference equality.
    IncidentNodes<String> incidentNodes = IncidentNodes.of(new String("node"), new String("node"));
    assertThat(incidentNodes).containsExactly("node").inOrder();
    new EqualsTester().addEqualityGroup(incidentNodes, ImmutableSet.of("node")).testEquals();
  }
}
