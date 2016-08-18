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
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * This class provides a skeletal implementation of {@link Graph}. It is recommended to extend
 * this class rather than implement {@link Graph} directly, to ensure consistent {@link
 * #equals(Object)} and {@link #hashCode()} results across different graph implementations.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
 */
@Beta
public abstract class AbstractGraph<N, V> implements Graph<N, V> {

  /**
   * Returns the number of edges in this graph; used to calculate the size of {@link #edges()}.
   * The default implementation is O(|N|). You can manually keep track of the number of edges and
   * override this method for better performance.
   */
  protected long edgeCount() {
    long degreeSum = 0L;
    for (N node : nodes()) {
      degreeSum += degree(node);
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
  public int degree(Object node) {
    if (isDirected()) {
      return IntMath.saturatedAdd(predecessors(node).size(), successors(node).size());
    } else {
      Set<N> neighbors = adjacentNodes(node);
      int selfLoop = (allowsSelfLoops() && neighbors.contains(node)) ? 1 : 0;
      return IntMath.saturatedAdd(neighbors.size(), selfLoop);
    }
  }

  @Override
  public int inDegree(Object node) {
    return isDirected() ? predecessors(node).size() : degree(node);
  }

  @Override
  public int outDegree(Object node) {
    return isDirected() ? successors(node).size() : degree(node);
  }

  @Override
  public final boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Graph)) {
      return false;
    }
    Graph<?, ?> other = (Graph<?, ?>) obj;

    if (isDirected() != other.isDirected()
        || !nodes().equals(other.nodes())
        || !edges().equals(other.edges())) {
      return false;
    }

    for (Endpoints<N> edge : edges()) {
      if (!edgeValue(edge.nodeA(), edge.nodeB()).equals(
          other.edgeValue(edge.nodeA(), edge.nodeB()))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public final int hashCode() {
    return edgeValueMap().hashCode();
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
        edgeValueMap());
  }

  private Map<Endpoints<N>, V> edgeValueMap() {
    Function<Endpoints<N>, V> edgeToValueFn = new Function<Endpoints<N>, V>() {
      @Override
      public V apply(Endpoints<N> edge) {
        return edgeValue(edge.nodeA(), edge.nodeB());
      }
    };
    return Maps.asMap(edges(), edgeToValueFn);
  }
}
