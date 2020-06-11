/*
 * Copyright (C) 2019 The Guava Authors
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

import java.util.AbstractSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract base class for an incident edges set that allows different implementations of {@link
 * AbstractSet#iterator()}.
 */
abstract class IncidentEdgeSet<N> extends AbstractSet<EndpointPair<N>> {
  protected final N node;
  protected final BaseGraph<N> graph;

  IncidentEdgeSet(BaseGraph<N> graph, N node) {
    this.graph = graph;
    this.node = node;
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    if (graph.isDirected()) {
      return graph.inDegree(node)
          + graph.outDegree(node)
          - (graph.successors(node).contains(node) ? 1 : 0);
    } else {
      return graph.adjacentNodes(node).size();
    }
  }

  @Override
  public boolean contains(@Nullable Object obj) {
    if (!(obj instanceof EndpointPair)) {
      return false;
    }
    EndpointPair<?> endpointPair = (EndpointPair<?>) obj;

    if (graph.isDirected()) {
      if (!endpointPair.isOrdered()) {
        return false;
      }

      Object source = endpointPair.source();
      Object target = endpointPair.target();
      return (node.equals(source) && graph.successors(node).contains(target))
          || (node.equals(target) && graph.predecessors(node).contains(source));
    } else {
      if (endpointPair.isOrdered()) {
        return false;
      }
      Set<N> adjacent = graph.adjacentNodes(node);
      Object nodeU = endpointPair.nodeU();
      Object nodeV = endpointPair.nodeV();

      return (node.equals(nodeV) && adjacent.contains(nodeU))
          || (node.equals(nodeU) && adjacent.contains(nodeV));
    }
  }
}
