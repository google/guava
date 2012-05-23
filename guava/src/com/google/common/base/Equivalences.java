/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/**
 * Contains static factory methods for creating {@code Equivalence} instances.
 *
 * <p>All methods return serializable instances.
 *
 * @author Bob Lee
 * @author Kurt Alfred Kluever
 * @author Gregory Kick
 * @since 4.0
 */
@Beta
@GwtCompatible
public final class Equivalences {
  private Equivalences() {}

  /**
   * Returns an equivalence that delegates to {@link Object#equals} and {@link Object#hashCode}.
   * {@link Equivalence#equivalent} returns {@code true} if both values are null, or if neither
   * value is null and {@link Object#equals} returns {@code true}. {@link Equivalence#hash} returns
   * {@code 0} if passed a null value.
   *
   * @since 8.0 (present null-friendly behavior)
   * @since 4.0 (otherwise)
   * @deprecated This method has been moved to {@link Equivalence#equals}. This method is scheduled
   *     to be removed in Guava release 14.0.
   */
  @Deprecated
  public static Equivalence<Object> equals() {
    return Equivalence.Equals.INSTANCE;
  }

  /**
   * Returns an equivalence that uses {@code ==} to compare values and {@link
   * System#identityHashCode(Object)} to compute the hash code.  {@link Equivalence#equivalent}
   * returns {@code true} if {@code a == b}, including in the case that a and b are both null.
   *
   * @deprecated This method has been moved to {@link Equivalence#identity}. This method is schedule
   *     to be removed in Guava release 14.0.
   */
  @Deprecated
  public static Equivalence<Object> identity() {
    return Equivalence.Identity.INSTANCE;
  }
}
