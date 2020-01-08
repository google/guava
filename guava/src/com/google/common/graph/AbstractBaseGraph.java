/*
 * Copyright (C) 2017 The Guava Authors
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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.graph.GraphConstants.ENDPOINTS_MISMATCH;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import java.util.AbstractSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class provides a skeletal implementation of {@link BaseGraph}.
 *
 * <p>The methods implemented in this class should not be overridden unless the subclass admits a
 * more efficient implementation.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 */
abstract class AbstractBaseGraph<N> implements BaseGraph<N> {

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
   * An implementation of {@link BaseGraph#edges()} defined in terms of {@link #nodes()} and {@link
   * #successors(Object)}.
   */
  @Override
  public Set<EndpointPair<N>> edges() {
    return new AbstractSet<EndpointPair<N>>() {
      @Override
      public UnmodifiableIterator<EndpointPair<N>> iterator() {
        return EndpointPairIterator.of(AbstractBaseGraph.this);
      }

      @Override
      public int size() {
        return Ints.saturatedCast(edgeCount());
      }

      @Override
      public boolean remove(Object o) {
        throw new UnsupportedOperationException();
      }

      // Mostly safe: We check contains(u) before calling successors(u), so we perform unsafe
      // operations only in weird cases like checking for an EndpointPair<ArrayList> in a
      // Graph<LinkedList>.
      @SuppressWarnings("unchecked")
      @Override
      public boolean contains(@Nullable Object obj) {
        if (!(obj instanceof EndpointPair)) {
          return false;
        }
        EndpointPair<?> endpointPair = (EndpointPair<?>) obj;
        return isOrderingCompatible(endpointPair)
            && nodes().contains(endpointPair.nodeU())
            && successors((N) endpointPair.nodeU()).contains(endpointPair.nodeV());
      }
    };
  }

  @Override
  public ElementOrder<N> incidentEdgeOrder() {
    return ElementOrder.unordered();
  }

  @Override
  public Set<EndpointPair<N>> incidentEdges(N node) {
    checkNotNull(node);
    checkArgument(nodes().contains(node), "Node %s is not an element of this graph.", node);
    return new IncidentEdgeSet<N>(this, node) {
      @Override
      public UnmodifiableIterator<EndpointPair<N>> iterator() {
        if (graph.isDirected()) {
          return Iterators.unmodifiableIterator(
              Iterators.concat(
                  Iterators.transform(
                      graph.predecessors(node).iterator(),
                      new Function<N, EndpointPair<N>>() {
                        @Override
                        public EndpointPair<N> apply(N predecessor) {
                          return EndpointPair.ordered(predecessor, node);
                        }
                      }),
                  Iterators.transform(
                      // filter out 'node' from successors (already covered by predecessors, above)
                      Sets.difference(graph.successors(node), ImmutableSet.of(node)).iterator(),
                      new Function<N, EndpointPair<N>>() {
                        @Override
                        public EndpointPair<N> apply(N successor) {
                          return EndpointPair.ordered(node, successor);
                        }
                      })));
        } else {
          return Iterators.unmodifiableIterator(
              Iterators.transform(
                  graph.adjacentNodes(node).iterator(),
                  new Function<N, EndpointPair<N>>() {
                    @Override
                    public EndpointPair<N> apply(N adjacentNode) {
                      return EndpointPair.unordered(node, adjacentNode);
                    }
                  }));
        }
      }
    };
  }

  @Override
  public int degree(N node) {
    if (isDirected()) {
      return IntMath.saturatedAdd(predecessors(node).size(), successors(node).size());
    } else {
      Set<N> neighbors = adjacentNodes(node);
      int selfLoopCount = (allowsSelfLoops() && neighbors.contains(node)) ? 1 : 0;
      return IntMath.saturatedAdd(neighbors.size(), selfLoopCount);
    }
  }

  @Override
  public int inDegree(N node) {
    return isDirected() ? predecessors(node).size() : degree(node);
  }

  @Override
  public int outDegree(N node) {
    return isDirected() ? successors(node).size() : degree(node);
  }

  @Override
  public boolean hasEdgeConnecting(N nodeU, N nodeV) {
    checkNotNull(nodeU);
    checkNotNull(nodeV);
    return nodes().contains(nodeU) && successors(nodeU).contains(nodeV);
  }

  @Override
  public boolean hasEdgeConnecting(EndpointPair<N> endpoints) {
    checkNotNull(endpoints);
    if (!isOrderingCompatible(endpoints)) {
      return false;
    }
    N nodeU = endpoints.nodeU();
    N nodeV = endpoints.nodeV();
    return nodes().contains(nodeU) && successors(nodeU).contains(nodeV);
  }

  /**
   * Throws {@code IllegalArgumentException} if the ordering of {@code endpoints} is not compatible
   * with the directionality of this graph.
   */
  protected final void validateEndpoints(EndpointPair<?> endpoints) {
    checkNotNull(endpoints);
    checkArgument(isOrderingCompatible(endpoints), ENDPOINTS_MISMATCH);
  }

  protected final boolean isOrderingCompatible(EndpointPair<?> endpoints) {
    return endpoints.isOrdered() || !this.isDirected();
  }
}
