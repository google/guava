/*
 * Copyright (C) 2007 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import org.jspecify.annotations.Nullable;

/**
 * Legacy version of {@link java.util.function.Predicate java.util.function.Predicate}. Determines a
 * true or false value for a given input.
 *
 * <p>As this interface extends {@code java.util.function.Predicate}, an instance of this type may
 * be used as a {@code Predicate} directly. To use a {@code java.util.function.Predicate} where a
 * {@code com.google.common.base.Predicate} is expected, use the method reference {@code
 * predicate::test}.
 *
 * <p>This interface is now a legacy type. Use {@code java.util.function.Predicate} (or the
 * appropriate primitive specialization such as {@code IntPredicate}) instead whenever possible.
 * Otherwise, at least reduce <i>explicit</i> dependencies on this type by using lambda expressions
 * or method references instead of classes, leaving your code easier to migrate in the future.
 *
 * <p>The {@link Predicates} class provides common predicates and related utilities.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/FunctionalExplained">the use of {@code Predicate}</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
@FunctionalInterface
@GwtCompatible
public interface Predicate<T extends @Nullable Object> extends java.util.function.Predicate<T> {
  /**
   * Returns the result of applying this predicate to {@code input} (Java 8+ users, see notes in the
   * class documentation above). This method is <i>generally expected</i>, but not absolutely
   * required, to have the following properties:
   *
   * <ul>
   *   <li>Its execution does not cause any observable side effects.
   *   <li>The computation is <i>consistent with equals</i>; that is, {@link Objects#equal
   *       Objects.equal}{@code (a, b)} implies that {@code predicate.apply(a) ==
   *       predicate.apply(b))}.
   * </ul>
   *
   * @throws NullPointerException if {@code input} is null and this predicate does not accept null
   *     arguments
   */
  boolean apply(@ParametricNullness T input);

  /**
   * Indicates whether another object is equal to this predicate.
   *
   * <p><b>Warning: do not depend</b> on the behavior of this method.
   *
   * <p>Historically, {@code Predicate} instances in this library have implemented this method to
   * recognize certain cases where distinct {@code Predicate} instances would in fact behave
   * identically. However, as code migrates to {@code java.util.function}, that behavior will
   * disappear. It is best not to depend on it.
   */
  @Override
  boolean equals(@Nullable Object obj);

  @Override
  default boolean test(@ParametricNullness T input) {
    return apply(input);
  }
}
