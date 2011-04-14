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

import javax.annotation.Nullable;

/**
 * A strategy for determining whether two instances are considered equivalent. Examples of
 * equivalences are the {@link Equivalences#identity() identity equivalence} and {@link
 * Equivalences#equals equals equivalence}.
 *
 * @author Bob Lee
 * @since Guava release 04
 */
@Beta
@GwtCompatible
public interface Equivalence<T> {
  /**
   * Returns {@code true} if the given objects are considered equivalent.
   *
   * <p>The {@code equivalent} method implements an equivalence relation on object references:
   *
   * <ul>
   * <li>It is <i>reflexive</i>: for any reference {@code x}, including null, {@code
   *     equivalent(x, x)} should return {@code true}.
   * <li>It is <i>symmetric</i>: for any references {@code x} and {@code y}, {@code
   *     equivalent(x, y) == equivalent(y, x)}.
   * <li>It is <i>transitive</i>: for any references {@code x}, {@code y}, and {@code z}, if
   *     {@code equivalent(x, y)} returns {@code true} and {@code equivalent(y, z)} returns {@code
   *     true}, then {@code equivalent(x, z)} should return {@code true}.
   * <li>It is <i>consistent</i>: for any references {@code x} and {@code y}, multiple invocations
   *     of {@code equivalent(x, y)} consistently return {@code true} or consistently return {@code
   *     false} (provided that neither {@code x} nor {@code y} is modified).
   * </ul>
   */
  boolean equivalent(@Nullable T a, @Nullable T b);

  /**
   * Returns a hash code for {@code object}. This function <b>must</b> return the same value for
   * any two references which are {@link #equivalent}, and should as often as possible return a
   * distinct value for references which are not equivalent. It should support null references.
   *
   * @see Object#hashCode the same contractual obligations apply here
   */
  int hash(@Nullable T t);
}
