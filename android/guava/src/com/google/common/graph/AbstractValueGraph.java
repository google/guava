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
import static com.google.common.graph.GraphConstants.EDGE_CONNECTING_NOT_IN_GRAPH;
import static com.google.common.graph.GraphConstants.GRAPH_STRING_FORMAT;
import static com.google.common.graph.GraphConstants.NODE_NOT_IN_GRAPH;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
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
public abstract class AbstractValueGraph<N, V> extends AbstractBaseGraph<N>
    implements ValueGraph<N, V> {

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
      public Set<N> adjacentNodes(N node) {
        return AbstractValueGraph.this.adjacentNodes(node);
      }

      @Override
      public Set<N> predecessors(N node) {
        return AbstractValueGraph.this.predecessors(node);
      }

      @Override
      public Set<N> successors(N node) {
        return AbstractValueGraph.this.successors(node);
      }

      @Override
      public int degree(N node) {
        return AbstractValueGraph.this.degree(node);
      }

      @Override
      public int inDegree(N node) {
        return AbstractValueGraph.this.inDegree(node);
      }

      @Override
      public int outDegree(N node) {
        return AbstractValueGraph.this.outDegree(node);
      }
    };
  }

  @Override
  public V edgeValue(N nodeU, N nodeV) {
    V value = edgeValueOrDefault(nodeU, nodeV, null);
    if (value == null) {
      checkArgument(nodes().contains(nodeU), NODE_NOT_IN_GRAPH, nodeU);
      checkArgument(nodes().contains(nodeV), NODE_NOT_IN_GRAPH, nodeV);
      throw new IllegalArgumentException(String.format(EDGE_CONNECTING_NOT_IN_GRAPH, nodeU, nodeV));
    }
    return value;
  }

  @Override
  public final boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ValueGraph)) {
      return false;
    }
    ValueGraph<?, ?> other = (ValueGraph<?, ?>) obj;

    return isDirected() == other.isDirected()
        && nodes().equals(other.nodes())
        && edgeValueMap(this).equals(edgeValueMap(other));
  }

  @Override
  public final int hashCode() {
    return edgeValueMap(this).hashCode();
  }

  /** Returns a string representation of this graph. */
  @Override
  public String toString() {
    String propertiesString =
        String.format("isDirected: %s, allowsSelfLoops: %s", isDirected(), allowsSelfLoops());
    return String.format(GRAPH_STRING_FORMAT, propertiesString, nodes(), edgeValueMap(this));
  }

  private static <N, V> Map<EndpointPair<N>, V> edgeValueMap(final ValueGraph<N, V> graph) {
    Function<EndpointPair<N>, V> edgeToValueFn =
        new Function<EndpointPair<N>, V>() {
          @Override
          public V apply(EndpointPair<N> edge) {
            return graph.edgeValue(edge.nodeU(), edge.nodeV());
          }
        };
    return Maps.asMap(graph.edges(), edgeToValueFn);
  }
}
