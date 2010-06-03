/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.base;

import com.google.common.annotations.Beta;

/**
 * A strategy for determining whether two instances are considered equivalent. Examples of
 * equivalences are the {@link Equivalences#identity() identity equivalence} and {@link
 * Equivalences#equals equals equivalence}.
 *
 * @author Bob Lee
 * @since 4
 */
@Beta
public interface Equivalence<T> {
  /**
   * Returns {@code true} if the given objects are considered equivalent.
   *
   * <p>The <code>equivalent</code> method implements an equivalence relation on non-null object
   * references:
   *
   * <ul>
   * <li>It is <i>reflexive</i>: for any non-null reference value {@code x}, {@code x.equals(x)}
   *     should return {@code true}.
   * <li>It is <i>symmetric</i>: for any non-null reference values {@code x} and {@code y}, {@code
   *     x.equals(y)} should return {@code true} if and only if {@code y.equals(x)} returns {@code
   *     true}.
   * <li>It is <i>transitive</i>: for any non-null reference values {@code x}, {@code y}, and {@code
   *     z}, if {@code x.equals(y)} returns {@code true} and {@code y.equals(z)} returns {@code
   *     true}, then {@code x.equals(z)} should return {@code true}.
   * <li>It is <i>consistent</i>: for any non-null reference values {@code x} and {@code y},
   *     multiple invocations of {@code x.equals(y)} consistently return {@code true} or
   *     consistently return {@code false}, provided no information used in {@code equals}
   *     comparisons on the objects is modified.
   * <li>For any non-null reference value {@code x}, {@code x.equals(null)} should return {@code
   *     false}.
   * </ul>
   */
  boolean equivalent(T a, T b);

  /**
   * Returns a hash code for {@code object}. This function <b>must</b> return the same value for
   * any two instances which are {@link #equivalent}, and should as often as possible return a
   * distinct value for instances which are not equivalent.
   *
   * @see Object#hashCode the same contractual obligations apply here
   * @throws NullPointerException if t is null
   */
  int hash(T t);
}
