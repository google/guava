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

import javax.annotation.Nullable;

/**
 * This class provides a skeletal implementation of {@link Graph}. It is recommended to extend this
 * class rather than implement {@link Graph} directly, to ensure consistent {@link #equals(Object)}
 * and {@link #hashCode()} results across different graph implementations.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
public abstract class AbstractNetwork<N, E> implements Network<N, E> {

  @Override
  public int degree(Object node) {
    return incidentEdges(node).size();
  }

  @Override
  public int inDegree(Object node) {
    return inEdges(node).size();
  }

  @Override
  public int outDegree(Object node) {
    return outEdges(node).size();
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (!(object instanceof Network)) {
      return false;
    }
    return Graphs.equal(this, (Network<?, ?>) object);
  }

  @Override
  public int hashCode() {
    return Graphs.hashCode(this);
  }

  @Override
  public String toString() {
    return Graphs.toString(this);
  }
}
