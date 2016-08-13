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

import com.google.common.annotations.Beta;
import javax.annotation.Nullable;

/**
 * A subtype of {@link Graph} that associates a value with each edge.
 *
 * TODO(b/30133524) Flesh out class-level javadoc.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
 */
@Beta
public interface ValueGraph<N, V> extends Graph<N> {
  /**
   * If there is an edge connecting {@code nodeA} to {@code nodeB}, returns the non-null value
   * associated with that edge.
   *
   * @throws IllegalArgumentException if there is no edge connecting {@code nodeA} to {@code nodeB}
   */
  V edgeValue(Object nodeA, Object nodeB);

  /**
   * If there is an edge connecting {@code nodeA} to {@code nodeB}, returns the non-null value
   * associated with that edge. Otherwise, returns {@code defaultValue}.
   */
  V edgeValueOrDefault(Object nodeA, Object nodeB, @Nullable V defaultValue);
}
