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
import com.google.common.graph.BasicGraph.Presence;

/**
 * This class provides a skeletal implementation of {@link BasicGraph}. It is recommended to extend
 * this class rather than implement {@link BasicGraph} directly, to ensure consistent {@link
 * #equals(Object)} and {@link #hashCode()} results across different graph implementations.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @since 20.0
 */
@Beta
public abstract class AbstractBasicGraph<N>
    extends AbstractGraph<N, Presence> implements BasicGraph<N> {

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
}
