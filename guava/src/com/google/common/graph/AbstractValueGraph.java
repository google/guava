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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.graph.GraphConstants.EDGE_CONNECTING_NOT_IN_GRAPH;
import static com.google.common.graph.GraphConstants.GRAPH_STRING_FORMAT;
import static com.google.common.graph.GraphConstants.NODE_NOT_IN_GRAPH;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import java.util.AbstractSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * This class provides a skeletal implementation of {@link ValueGraph}. It is recommended to extend
 * this class rather than implement {@link ValueGraph} directly.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
 */
@Beta
public abstract class AbstractValueGraph<N, V> implements ValueGraph<N, V> {

  @Override
  public Graph<N> asGraph() {
    return new AbstractGraph<N>() {
      @Override
      public Set<N> nodes() {
        return AbstractValueGraph.this.nodes();
      }

      @Override
      public Set<EndpointPair<N>> edges() {
        return AbstractValueGraph.this.edges();
      }

      @Override
      public boolean isDirected() {
        return AbstractValueGraph.this.isDirected();
      }

      @Override
      public boolean allowsSelfLoops() {
        return AbstractValueGraph.this.allowsSelfLoops();
      }

      @Override
      public ElementOrder<N> nodeOrder() {
        return AbstractValueGraph.this.nodeOrder();
      }

      @Override
      public Set<N> adjacentNodes(Object node) {
        return AbstractValueGraph.this.adjacentNodes(node);
      }

      @Override
      public Set<N> predecessors(Object node) {
        return AbstractValueGraph.this.predecessors(node);
      }

      @Override
      public Set<N> successors(Object node) {
        return AbstractValueGraph.this.successors(node);
      }

      @Override
      public int degree(Object node) {
        return AbstractValueGraph.this.degree(node);
      }

      @Override
      public int inDegree(Object node) {
        return AbstractValueGraph.this.inDegree(node);
      }

      @Override
      public int outDegree(Object node) {
        return AbstractValueGraph.this.outDegree(node);
      }
    };
  }

  /**
   * Returns the number of edges in this graph; used to calculate the size of {@link #edges()}. This
   * implementation requires O(|N|) time. Classes extending this one may manually keep track of the
   * number of edges as the graph is updated, and override this method for better performance.
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
   * An implementation of {@link ValueGraph#edges()} defined in terms of {@link #nodes()} and {@link
   * #successors(Object)}.
   */
  @Override
  public Set<EndpointPair<N>> edges() {
    return new AbstractSet<EndpointPair<N>>() {
      @Override
      public UnmodifiableIterator<EndpointPair<N>> iterator() {
        return EndpointPairIterator.of(asGraph());
      }

      @Override
      public int size() {
        return Ints.saturatedCast(edgeCount());
      }

      @Override
      public boolean contains(@Nullable Object obj) {
        if (!(obj instanceof EndpointPair)) {
          return false;
        }
        EndpointPair<?> endpointPair = (EndpointPair<?>) obj;
        return isDirected() == endpointPair.isOrdered()
            && nodes().contains(endpointPair.nodeU())
            && successors(endpointPair.nodeU()).contains(endpointPair.nodeV());
      }
    };
  }

  @Override
  public int degree(Object node) {
    if (isDirected()) {
      return IntMath.saturatedAdd(predecessors(node).size(), successors(node).size());
    } else {
      Set<N> neighbors = adjacentNodes(node);
      int selfLoopCount = (allowsSelfLoops() && neighbors.contains(node)) ? 1 : 0;
      return IntMath.saturatedAdd(neighbors.size(), selfLoopCount);
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
  public V edgeValue(Object nodeU, Object nodeV) {
    V value = edgeValueOrDefault(nodeU, nodeV, null);
    if (value == null) {
      checkArgument(nodes().contains(nodeU), NODE_NOT_IN_GRAPH, nodeU);
      checkArgument(nodes().contains(nodeV), NODE_NOT_IN_GRAPH, nodeV);
      throw new IllegalArgumentException(String.format(EDGE_CONNECTING_NOT_IN_GRAPH, nodeU, nodeV));
    }
    return value;
  }

  /** Returns a string representation of this graph. */
  @Override
  public String toString() {
    String propertiesString =
        String.format("isDirected: %s, allowsSelfLoops: %s", isDirected(), allowsSelfLoops());
    return String.format(GRAPH_STRING_FORMAT, propertiesString, nodes(), edgeValueMap());
  }

  private Map<EndpointPair<N>, V> edgeValueMap() {
    Function<EndpointPair<N>, V> edgeToValueFn =
        new Function<EndpointPair<N>, V>() {
          @Override
          public V apply(EndpointPair<N> edge) {
            return edgeValue(edge.nodeU(), edge.nodeV());
          }
        };
    return Maps.asMap(edges(), edgeToValueFn);
  }
}
