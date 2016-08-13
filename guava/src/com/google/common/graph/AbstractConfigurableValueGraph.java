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
import static com.google.common.graph.GraphConstants.NODE_NOT_IN_GRAPH;

import java.util.Map;
import javax.annotation.Nullable;

/**
 * Subclass of {@link AbstractConfigurableGraph} that adds support for {@link ValueGraph}.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 */
abstract class AbstractConfigurableValueGraph<N, V>
    extends AbstractConfigurableGraph<N, V> implements ValueGraph<N, V> {

  /**
   * Constructs a graph with the properties specified in {@code builder}.
   */
  AbstractConfigurableValueGraph(AbstractGraphBuilder<? super N> builder) {
    super(builder);
  }

  /**
   * Constructs a graph with the properties specified in {@code builder}, initialized with
   * the given node map.
   */
  AbstractConfigurableValueGraph(AbstractGraphBuilder<? super N> builder,
      Map<N, GraphConnections<N, V>> nodeConnections, long edgeCount) {
    super(builder, nodeConnections, edgeCount);
  }

  @Override
  public V edgeValue(Object nodeA, Object nodeB) {
    V value = edgeValueOrDefault(nodeA, nodeB, null);
    checkArgument(value != null, EDGE_CONNECTING_NOT_IN_GRAPH, nodeA, nodeB);
    return value;
  }

  @Override
  public V edgeValueOrDefault(Object nodeA, Object nodeB, @Nullable V defaultValue) {
    V value = checkedConnections(nodeA).value(nodeB);
    if (value == null) {
      checkArgument(containsNode(nodeB), NODE_NOT_IN_GRAPH, nodeB);
      return defaultValue;
    }
    return value;
  }
}
