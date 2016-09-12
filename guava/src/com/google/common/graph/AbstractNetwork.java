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
 * this class rather than implement {@link Network} directly.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
@Beta
public abstract class AbstractNetwork<N, E> implements Network<N, E> {

  @Override
  public Graph<N> asGraph() {
    return new AbstractGraph<N>() {
      @Override
      public Set<N> nodes() {
        return AbstractNetwork.this.nodes();
      }

      @Override
      public Set<EndpointPair<N>> edges() {
        if (allowsParallelEdges()) {
          return super.edges(); // Defer to AbstractGraph implementation.
        }

        // Optimized implementation assumes no parallel edges (1:1 edge to EndpointPair mapping).
        return new AbstractSet<EndpointPair<N>>() {
          @Override
          public Iterator<EndpointPair<N>> iterator() {
            return Iterators.transform(
                AbstractNetwork.this.edges().iterator(),
                new Function<E, EndpointPair<N>>() {
                  @Override
                  public EndpointPair<N> apply(E edge) {
                    return incidentNodes(edge);
                  }
                });
          }

          @Override
          public int size() {
            return AbstractNetwork.this.edges().size();
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

      // DO NOT override the AbstractGraph *degree() implementations.
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
    EndpointPair<?> endpointPair = incidentNodes(edge); // Verifies that edge is in this network.
    Set<E> endpointPairIncidentEdges =
        Sets.union(incidentEdges(endpointPair.nodeU()), incidentEdges(endpointPair.nodeV()));
    return Sets.difference(endpointPairIncidentEdges, ImmutableSet.of(edge));
  }

  /** Returns a string representation of this network. */
  @Override
  public String toString() {
    String propertiesString =
        String.format(
            "isDirected: %s, allowsParallelEdges: %s, allowsSelfLoops: %s",
            isDirected(), allowsParallelEdges(), allowsSelfLoops());
    return String.format(GRAPH_STRING_FORMAT, propertiesString, nodes(), edgeIncidentNodesMap());
  }

  private Map<E, EndpointPair<N>> edgeIncidentNodesMap() {
    Function<E, EndpointPair<N>> edgeToIncidentNodesFn =
        new Function<E, EndpointPair<N>>() {
          @Override
          public EndpointPair<N> apply(E edge) {
            return incidentNodes(edge);
          }
        };
    return Maps.asMap(edges(), edgeToIncidentNodesFn);
  }
}
