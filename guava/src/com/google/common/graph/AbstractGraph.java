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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.graph.GraphConstants.GRAPH_STRING_FORMAT;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * This class provides a skeletal implementation of {@link Graph}. It is recommended to extend this
 * class rather than implement {@link Graph} directly, to ensure consistent {@link #equals(Object)}
 * and {@link #hashCode()} results across different graph implementations.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @since 20.0
 */
@Beta
public abstract class AbstractGraph<N> implements Graph<N> {

  /**
   * Returns the number of edges in this graph; used to calculate the size of {@link #edges()}.
   * The default implementation is O(|N|). You can manually keep track of the number of edges and
   * override this method for better performance.
   */
  protected long edgeCount() {
    long degreeSum = 0L;
    for (N node : nodes()) {
      degreeSum += degree(this, node);
    }
    // According to the degree sum formula, this is equal to twice the number of edges.
    checkState((degreeSum & 1) == 0);
    return degreeSum >>> 1;
  }

  /**
   * A reasonable default implementation of {@link Graph#edges()} defined in terms of
   * {@link #nodes()} and {@link #successors(Object)}.
   */
  @Override
  public Set<Endpoints<N>> edges() {
    return new AbstractSet<Endpoints<N>>() {
      @Override
      public Iterator<Endpoints<N>> iterator() {
        return EndpointsIterator.of(AbstractGraph.this);
      }

      @Override
      public int size() {
        return Ints.saturatedCast(edgeCount());
      }

      @Override
      public boolean contains(Object obj) {
        if (!(obj instanceof Endpoints)) {
          return false;
        }
        Endpoints<?> endpoints = (Endpoints<?>) obj;
        return isDirected() == endpoints.isDirected()
            && nodes().contains(endpoints.nodeA())
            && successors(endpoints.nodeA()).contains(endpoints.nodeB());
      }
    };
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Graph)) {
      return false;
    }
    Graph<?> other = (Graph<?>) obj;

    if (isDirected() != other.isDirected()) {
      return false;
    }

    if (!nodes().equals(other.nodes())) {
      return false;
    }

    for (N node : nodes()) {
      if (!successors(node).equals(other.successors(node))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int hashCode() {
    Function<N, Set<N>> nodeToSuccessors = new Function<N, Set<N>>() {
      @Override
      public Set<N> apply(N node) {
        return successors(node);
      }
    };
    return Maps.asMap(nodes(), nodeToSuccessors).hashCode();
  }

  /**
   * Returns a string representation of this graph.
   */
  @Override
  public String toString() {
    String propertiesString = String.format(
        "isDirected: %s, allowsSelfLoops: %s", isDirected(), allowsSelfLoops());
    return String.format(GRAPH_STRING_FORMAT,
        propertiesString,
        nodes(),
        edges());
  }

  /**
   * Returns the number of times an edge touches {@code node} in {@code graph}. This is equivalent
   * to the number of edges incident to {@code node} in the graph, with self-loops counting twice.
   *
   * <p>If this number is greater than {@code Integer.MAX_VALUE}, returns {@code Integer.MAX_VALUE}.
   *
   * @throws IllegalArgumentException if {@code node} is not an element of this graph
   */
  // TODO(b/30649235): What to do with this? Move to Graphs or interfaces? Provide in/outDegree?
  private static int degree(Graph<?> graph, Object node) {
    if (graph.isDirected()) {
      return IntMath.saturatedAdd(graph.predecessors(node).size(), graph.successors(node).size());
    } else {
      int selfLoops = (graph.allowsSelfLoops() && graph.adjacentNodes(node).contains(node)) ? 1 : 0;
      return IntMath.saturatedAdd(graph.adjacentNodes(node).size(), selfLoops);
    }
  }
}
