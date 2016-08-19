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

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.math.IntMath;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * This class provides a skeletal implementation of {@link Network}. It is recommended to extend
 * this class rather than implement {@link Network} directly, to ensure consistent {@link
 * #equals(Object)} and {@link #hashCode()} results across different network implementations.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
@Beta
public abstract class AbstractNetwork<N, E> implements Network<N, E> {

  @Override
  public Graph<N, Set<E>> asGraph() {
    return new AbstractGraph<N, Set<E>>() {
      @Override
      public Set<N> nodes() {
        return AbstractNetwork.this.nodes();
      }

      @Override
      public Set<Endpoints<N>> edges() {
        if (allowsParallelEdges()) {
          return super.edges(); // Defer to AbstractGraph implementation.
        }

        // Optimized implementation assumes no parallel edges (1:1 edge to Endpoints mapping).
        return new AbstractSet<Endpoints<N>>() {
          @Override
          public Iterator<Endpoints<N>> iterator() {
            return Iterators.transform(
                AbstractNetwork.this.edges().iterator(),
                new Function<E, Endpoints<N>>() {
                  @Override
                  public Endpoints<N> apply(E edge) {
                    return incidentNodes(edge);
                  }
                });
          }

          @Override
          public int size() {
            return AbstractNetwork.this.edges().size();
          }

          @Override
          public boolean contains(Object obj) {
            if (!(obj instanceof Endpoints)) {
              return false;
            }
            Endpoints<?> endpoints = (Endpoints<?>) obj;
            return isDirected() == endpoints.isDirected()
                && !edgesConnecting(endpoints.nodeA(), endpoints.nodeB()).isEmpty();
          }
        };
      }

      @Override
      public ElementOrder<N> nodeOrder() {
        return AbstractNetwork.this.nodeOrder();
      }

      @Override
      public boolean isDirected() {
        return AbstractNetwork.this.isDirected();
      }

      @Override
      public boolean allowsSelfLoops() {
        return AbstractNetwork.this.allowsSelfLoops();
      }

      @Override
      public Set<N> adjacentNodes(Object node) {
        return AbstractNetwork.this.adjacentNodes(node);
      }

      @Override
      public Set<N> predecessors(Object node) {
        return AbstractNetwork.this.predecessors(node);
      }

      @Override
      public Set<N> successors(Object node) {
        return AbstractNetwork.this.successors(node);
      }

      @Override
      public Set<E> edgeValue(Object nodeA, Object nodeB) {
        Set<E> edges = edgesConnecting(nodeA, nodeB);
        checkArgument(!edges.isEmpty(), EDGE_CONNECTING_NOT_IN_GRAPH, nodeA, nodeB);
        return edges;
      }

      @Override
      public Set<E> edgeValueOrDefault(Object nodeA, Object nodeB, Set<E> defaultValue) {
        Set<E> edges = edgesConnecting(nodeA, nodeB);
        return edges.isEmpty() ? defaultValue : edges;
      }
    };
  }

  @Override
  public int degree(Object node) {
    if (isDirected()) {
      return IntMath.saturatedAdd(inEdges(node).size(), outEdges(node).size());
    } else {
      return IntMath.saturatedAdd(incidentEdges(node).size(), edgesConnecting(node, node).size());
    }
  }

  @Override
  public int inDegree(Object node) {
    return isDirected() ? inEdges(node).size() : degree(node);
  }

  @Override
  public int outDegree(Object node) {
    return isDirected() ? outEdges(node).size() : degree(node);
  }

  @Override
  public Set<E> adjacentEdges(Object edge) {
    Endpoints<?> endpoints = incidentNodes(edge); // Verifies that edge is in this network.
    Set<E> endpointsIncidentEdges =
        Sets.union(incidentEdges(endpoints.nodeA()), incidentEdges(endpoints.nodeB()));
    return Sets.difference(endpointsIncidentEdges, ImmutableSet.of(edge));
  }

  @Override
  public final boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Network)) {
      return false;
    }
    Network<?, ?> other = (Network<?, ?>) obj;

    if (isDirected() != other.isDirected()
        || !nodes().equals(other.nodes())
        || !edges().equals(other.edges())) {
      return false;
    }

    for (E edge : edges()) {
      if (!incidentNodes(edge).equals(other.incidentNodes(edge))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public final int hashCode() {
    return edgeEndpointsMap().hashCode();
  }

  /**
   * Returns a string representation of this network.
   */
  @Override
  public String toString() {
    String propertiesString = String.format(
        "isDirected: %s, allowsParallelEdges: %s, allowsSelfLoops: %s",
        isDirected(), allowsParallelEdges(), allowsSelfLoops());
    return String.format(GRAPH_STRING_FORMAT,
        propertiesString,
        nodes(),
        edgeEndpointsMap());
  }

  private Map<E, Endpoints<N>> edgeEndpointsMap() {
    Function<E, Endpoints<N>> edgeToEndpointsFn = new Function<E, Endpoints<N>>() {
      @Override
      public Endpoints<N> apply(E edge) {
        return incidentNodes(edge);
      }
    };
    return Maps.asMap(edges(), edgeToEndpointsFn);
  }
}
