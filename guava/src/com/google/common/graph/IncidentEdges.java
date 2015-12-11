/*
 * Copyright (C) 2014 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * A class representing the edges incident to a node in a directed graph.
 *
 * @author Joshua O'Madadhain
 * @param <E> Edge parameter type
 */
@CheckReturnValue
final class IncidentEdges<E> {

  private final Set<E> inEdges;
  private final Set<E> outEdges;

  private IncidentEdges(Set<E> inEdges, Set<E> outEdges) {
    this.inEdges = checkNotNull(inEdges, "inEdges");
    this.outEdges = checkNotNull(outEdges, "outEdges");
  }

  static <E> IncidentEdges<E> of() {
    return new IncidentEdges<E>(new LinkedHashSet<E>(), new LinkedHashSet<E>());
  }

  static <E> IncidentEdges<E> ofImmutable(Set<E> inEdges, Set<E> outEdges) {
    return new IncidentEdges<E>(ImmutableSet.copyOf(inEdges), ImmutableSet.copyOf(outEdges));
  }

  Set<E> inEdges() {
    return inEdges;
  }

  Set<E> outEdges() {
    return outEdges;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(inEdges, outEdges);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof IncidentEdges) {
      IncidentEdges<?> that = (IncidentEdges<?>) object;
      return this.inEdges.equals(that.inEdges)
          && this.outEdges.equals(that.outEdges);
    }
    return false;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("inEdges", inEdges)
        .add("outEdges", outEdges)
        .toString();
  }
}
