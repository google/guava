/*
 * Copyright (C) 2016 The Guava Authors
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

import static com.google.common.collect.Iterators.size;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Set;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/** Utility methods used in various common.graph tests. */
@NullUnmarked
final class TestUtil {
  static final String ERROR_ELEMENT_NOT_IN_GRAPH = "not an element of this graph";
  static final String ERROR_NODE_NOT_IN_GRAPH =
      "Should not be allowed to pass a node that is not an element of the graph.";
  static final String ERROR_ELEMENT_REMOVED = "used to generate this set";
  private static final String NODE_STRING = "Node";
  private static final String EDGE_STRING = "Edge";

  enum EdgeType {
    UNDIRECTED,
    DIRECTED;
  }

  private TestUtil() {}

  static void assertNodeNotInGraphErrorMessage(@Nullable Throwable throwable) {
    assertThat(throwable).hasMessageThat().startsWith(NODE_STRING);
    assertThat(throwable).hasMessageThat().contains(ERROR_ELEMENT_NOT_IN_GRAPH);
  }

  static void assertEdgeNotInGraphErrorMessage(@Nullable Throwable throwable) {
    assertThat(throwable).hasMessageThat().startsWith(EDGE_STRING);
    assertThat(throwable).hasMessageThat().contains(ERROR_ELEMENT_NOT_IN_GRAPH);
  }

  static void assertNodeRemovedFromGraphErrorMessage(@Nullable Throwable throwable) {
    assertThat(throwable).hasMessageThat().startsWith(NODE_STRING);
    assertThat(throwable).hasMessageThat().contains(ERROR_ELEMENT_REMOVED);
  }

  static void assertEdgeRemovedFromGraphErrorMessage(@Nullable Throwable throwable) {
    assertThat(throwable).hasMessageThat().startsWith(EDGE_STRING);
    assertThat(throwable).hasMessageThat().contains(ERROR_ELEMENT_REMOVED);
  }

  static void assertStronglyEquivalent(Graph<?> graphA, Graph<?> graphB) {
    // Properties not covered by equals()
    assertThat(graphA.allowsSelfLoops()).isEqualTo(graphB.allowsSelfLoops());
    assertThat(graphA.nodeOrder()).isEqualTo(graphB.nodeOrder());

    assertThat(graphA).isEqualTo(graphB);
  }

  static void assertStronglyEquivalent(ValueGraph<?, ?> graphA, ValueGraph<?, ?> graphB) {
    // Properties not covered by equals()
    assertThat(graphA.allowsSelfLoops()).isEqualTo(graphB.allowsSelfLoops());
    assertThat(graphA.nodeOrder()).isEqualTo(graphB.nodeOrder());

    assertThat(graphA).isEqualTo(graphB);
  }

  static void assertStronglyEquivalent(Network<?, ?> networkA, Network<?, ?> networkB) {
    // Properties not covered by equals()
    assertThat(networkA.allowsParallelEdges()).isEqualTo(networkB.allowsParallelEdges());
    assertThat(networkA.allowsSelfLoops()).isEqualTo(networkB.allowsSelfLoops());
    assertThat(networkA.nodeOrder()).isEqualTo(networkB.nodeOrder());
    assertThat(networkA.edgeOrder()).isEqualTo(networkB.edgeOrder());

    assertThat(networkA).isEqualTo(networkB);
  }

  /**
   * In some cases our graph implementations return custom sets that define their own size() and
   * contains(). Verify that these sets are consistent with the elements of their iterator.
   */
  // We want to test our implementations of Collection methods, so we call them directly.
  @SuppressWarnings({
    "CollectionSizeTruth",
    "CollectionContainsTruth",
    "CollectionDoesNotContainTruth"
  })
  @CanIgnoreReturnValue
  static <T> Set<T> sanityCheckSet(Set<T> set) {
    assertThat(set.size()).isEqualTo(size(set.iterator()));
    for (Object element : set) {
      assertThat(set.contains(element)).isTrue();
    }
    try {
      assertThat(set.contains(new Object())).isFalse();
    } catch (ClassCastException tolerated) {
      // It's OK for our backing TreeMap to throw. TODO(cpovirk): Would we prefer that it not?
    }
    new EqualsTester().addEqualityGroup(set, ImmutableSet.copyOf(set)).testEquals();
    return set;
  }
}
