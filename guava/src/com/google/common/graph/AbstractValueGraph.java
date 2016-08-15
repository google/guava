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
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * This class provides a skeletal implementation of {@link ValueGraph}. It is recommended to extend
 * this class rather than implement {@link ValueGraph} directly, to ensure consistent {@link
 * #equals(Object)} and {@link #hashCode()} results across different value graph implementations.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
 */
@Beta
public abstract class AbstractValueGraph<N, V>
    extends AbstractGraph<N> implements ValueGraph<N, V> {

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ValueGraph)) {
      return false;
    }
    ValueGraph<?, ?> other = (ValueGraph<?, ?>) obj;

    if (isDirected() != other.isDirected()
        || !nodes().equals(other.nodes())
        || !edges().equals(other.edges())) {
      return false;
    }

    for (Endpoints<?> edge : edges()) {
      if (!edgeValue(edge.nodeA(), edge.nodeB()).equals(
          other.edgeValue(edge.nodeA(), edge.nodeB()))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int hashCode() {
    return edgeValueMap().hashCode();
  }

  /**
   * Returns a string representation of this value graph.
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
