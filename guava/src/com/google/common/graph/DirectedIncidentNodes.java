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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nullable;

/**
 * An immutable pair representing the incident nodes (source and target) on a directed edge.
 *
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 */
final class DirectedIncidentNodes<N> {

  private final N source;
  private final N target;

  private DirectedIncidentNodes(N source, N target) {
    this.source = checkNotNull(source, "source");
    this.target = checkNotNull(target, "target");
  }

  static <N> DirectedIncidentNodes<N> of(N source, N target) {
    return new DirectedIncidentNodes<N>(source, target);
  }

  N source() {
    return source;
  }

  N target() {
    return target;
  }

  boolean isSelfLoop() {
    return source.equals(target);
  }

  ImmutableSet<N> asImmutableSet() {
    return ImmutableSet.of(source, target);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(source, target);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof DirectedIncidentNodes<?>) {
      DirectedIncidentNodes<?> that = (DirectedIncidentNodes<?>) object;
      return this.source.equals(that.source)
          && this.target.equals(that.target);
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("<%s -> %s>", source, target);
  }
}
