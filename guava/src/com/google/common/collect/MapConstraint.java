/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * A constraint on the keys and values that may be added to a {@code Map} or
 * {@code Multimap}. For example, {@link MapConstraints#notNull()}, which
 * prevents a map from including any null keys or values, could be implemented
 * like this: <pre>   {@code
 *
 *   public void checkKeyValue(Object key, Object value) {
 *     if (key == null || value == null) {
 *       throw new NullPointerException();
 *     }
 *   }}</pre>
 *
 * <p>In order to be effective, constraints should be deterministic; that is, they
 * should not depend on state that can change (such as external state, random
 * variables, and time) and should only depend on the value of the passed-in key
 * and value. A non-deterministic constraint cannot reliably enforce that all
 * the collection's elements meet the constraint, since the constraint is only
 * enforced when elements are added.
 *
 * @author Mike Bostock
 * @see MapConstraints
 * @see Constraint
 * @since 3.0
 * @deprecated Use {@link Preconditions} for basic checks. In place of
 *     constrained maps, we encourage you to check your preconditions
 *     explicitly instead of leaving that work to the map implementation.
 *     For the specific case of rejecting null, consider {@link ImmutableMap}.
 *     This class is scheduled for removal in Guava 20.0.
 */
@GwtCompatible
@Beta
@Deprecated
public interface MapConstraint<K, V> {
  /**
   * Throws a suitable {@code RuntimeException} if the specified key or value is
   * illegal. Typically this is either a {@link NullPointerException}, an
   * {@link IllegalArgumentException}, or a {@link ClassCastException}, though
   * an application-specific exception class may be used if appropriate.
   */
  void checkKeyValue(@Nullable K key, @Nullable V value);

  /**
   * Returns a brief human readable description of this constraint, such as
   * "Not null".
   */
  @Override
  String toString();
}
