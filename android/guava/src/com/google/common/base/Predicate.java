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
 * Determines a true or false value for a given input; a pre-Java-8 version of {@link
 * java.util.function.Predicate java.util.function.Predicate}.
 *
 * <p>The {@link Predicates} class provides common predicates and related utilities.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/FunctionalExplained">the use of {@code Predicate}</a>.
 *
 * <h3>For Java 8+ users</h3>
 *
 * <p>This interface is now a legacy type. Use {@code java.util.function.Predicate} (or the
 * appropriate primitive specialization such as {@code IntPredicate}) instead whenever possible.
 * Otherwise, at least reduce <i>explicit</i> dependencies on this type by using lambda expressions
 * or method references instead of classes, leaving your code easier to migrate in the future.
 *
 * <p>To use a reference of this type (say, named {@code guavaPredicate}) in a context where {@code
 * java.util.function.Predicate} is expected, use the method reference {@code
 * guavaPredicate::apply}. For the other direction, use {@code javaUtilPredicate::test}. A future
 * version of this interface will be made to <i>extend</i> {@code java.util.function.Predicate}, so
 * that conversion will be necessary in only one direction. At that time, this interface will be
 * officially discouraged.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible
public interface Predicate<T extends @Nullable Object> {
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
}
