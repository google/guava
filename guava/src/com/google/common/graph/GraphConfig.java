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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * A class for configuring different types of graphs.
 *
 * <p>{@code Graphs.config()} should be used to get an instance of this class.
 *
 * <p>Currently, this class supports the following graph configurations (all combinations
 * of these properties are valid unless stated otherwise):
 * <ul>
 * <li>Multigraphs.
 * <li>Expected number of nodes/edges.
 * <li>Self-loop edges.
 * </ul>
 *
 * <p>Default graph configuration:
 * <ul>
 * <li>Self-loop edges are allowed.
 * <li>It is not a multigraph: parallel edges (multiple edges directed from n1
 *     to n2, or between them in case of undirected graphs) are not allowed.
 * <li>In case of directed graphs, anti-parallel edges (same incident nodes but
 *     in opposite direction, e.g. (n1, n2) and (n2, n1)) are allowed.
 * <li>Nodes and edges are not sorted.
 * </ul>
 *
 * <p>{@code GraphConfig} instances are thread-safe immutable, and are therefore safe to
 * store as {@code static final} constants.
 *
 * @author Joshua O'Madadhain
 * @see Graphs
 * @since 20.0
 */
// TODO(user): Add support for sorted nodes/edges. Use Object as
//     the node and edge types in this case: the same scheme used in CacheBuilder.
// TODO(user): Add support for hypergraphs.
// TODO(user): Handle sorted nodes/edges and expected number of nodes/edges together,
//     in case sorted nodes/edges is supported.
@Beta
@CheckReturnValue
public final class GraphConfig {
  private final boolean multigraph;
  private final boolean selfLoopsAllowed;
  private final Optional<Integer> expectedNodeCount;
  private final Optional<Integer> expectedEdgeCount;

  // Creates an instance of this class with the default graph configuration.
  GraphConfig() {
    multigraph = false;
    selfLoopsAllowed = true;
    expectedNodeCount = Optional.absent();
    expectedEdgeCount = Optional.absent();
  }

  private GraphConfig(
      boolean multigraph,
      boolean selfLoopsAllowed,
      Optional<Integer> expectedNodeCount,
      Optional<Integer> expectedEdgeCount) {
    this.multigraph = multigraph;
    this.selfLoopsAllowed = selfLoopsAllowed;
    this.expectedNodeCount = expectedNodeCount;
    this.expectedEdgeCount = expectedEdgeCount;
  }

  public boolean isMultigraph() {
    return multigraph;
  }

  public boolean isSelfLoopsAllowed() {
    return selfLoopsAllowed;
  }

  public Optional<Integer> getExpectedNodeCount() {
    return expectedNodeCount;
  }

  public Optional<Integer> getExpectedEdgeCount() {
    return expectedEdgeCount;
  }

  /**
   * Specifies the expected number of nodes in the graph configuration.
   *
   * @return a new {@code GraphConfig} instance that augments the existing configuration
   *         by specifying the expected number of nodes.
   * @throws IllegalArgumentException if {@code expectedNodeCount} is negative
   */
  public GraphConfig expectedNodeCount(int expectedNodeCount) {
    checkArgument(expectedNodeCount >= 0, "The expected number of nodes can't be negative");
    return new GraphConfig(
        multigraph, selfLoopsAllowed, Optional.of(expectedNodeCount), expectedEdgeCount);
  }

  /**
   * Specifies the expected number of edges in the graph configuration.
   *
   * @return a new {@code GraphConfig} instance that augments the existing configuration
   *         by specifying the expected number of edges.
   * @throws IllegalArgumentException if {@code expectedEdgeCount} is negative
   */
  public GraphConfig expectedEdgeCount(int expectedEdgeCount) {
    checkArgument(expectedEdgeCount >= 0, "The expected number of edges can't be negative");
    return new GraphConfig(
        multigraph, selfLoopsAllowed, expectedNodeCount, Optional.of(expectedEdgeCount));
  }

  /**
   * Specifies that this graph is a multigraph (allows parallel edges).
   *
   * @return a new {@code GraphConfig} instance that augments the existing configuration
   *         by allowing parallel edges (multigraph).
   */
  public GraphConfig multigraph() {
    return new GraphConfig(true, selfLoopsAllowed, expectedNodeCount, expectedEdgeCount);
  }

  /**
   * Specifies that this graph does not allow self-loop edges.
   *
   * @return a new {@code GraphConfig} instance that augments the existing configuration
   *         by disallowing self-loop edges.
   */
  public GraphConfig noSelfLoops() {
    return new GraphConfig(multigraph, false, expectedNodeCount, expectedEdgeCount);
  }

  /**
   * Returns true iff the configuration defined by this object is <i>compatible with</i> the
   * configuration defined by {@code that}.  Intuitively, this checks for structural properties
   * and ignores non-structural properties.  Specifically, this method checks whether the
   * configurations have compatible support for:
   * <ul>
   * <li>parallel edges
   * <li>self-loops
   * </ul>
   *
   * <p>It does not compare expected values for numbers of edges or nodes,
   * and it is not equivalent to {@code Object.equals}.
   */
  public boolean compatibleWith(GraphConfig that) {
    checkNotNull(that, "that");
    return this.multigraph == that.multigraph
        && this.selfLoopsAllowed == that.selfLoopsAllowed;
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof GraphConfig) {
      GraphConfig that = (GraphConfig) object;
      return this.multigraph == that.multigraph
          && this.selfLoopsAllowed == that.selfLoopsAllowed
          && this.expectedNodeCount.equals(that.expectedNodeCount)
          && this.expectedEdgeCount.equals(that.expectedEdgeCount);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(expectedNodeCount, expectedEdgeCount, multigraph, selfLoopsAllowed);
  }

  @Override
  public String toString() {
    List<String> properties = Lists.newArrayList();
    if (multigraph) {
      properties.add("multigraph");
    }

    if (selfLoopsAllowed) {
      properties.add("self-loops allowed");
    } else {
      properties.add("self-loops disallowed");
    }

    return Joiner.on(',').join(properties);
  }
}
