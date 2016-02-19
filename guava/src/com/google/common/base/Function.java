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
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import javax.annotation.Nullable;

/**
 * Determines an output value based on an input value; a pre-Java-8 version of {@code
 * java.util.function.Function}.
 *
 * <p>The {@link Functions} class provides common functions and related utilites.
 *
 * <p>See the Guava User Guide article on
 * <a href="https://github.com/google/guava/wiki/FunctionalExplained">the use of {@code
 * Function}</a>.
 *
 * <h3>For Java 8+ users</h3>
 *
 * <p>This interface is now a legacy type. Use {@code java.util.function.Function} (or the
 * appropriate primitive specialization such as {@code ToIntFunction}) instead whenever possible.
 * Otherwise, at least reduce <i>explicit</i> dependencies on this type by using lambda expressions
 * or method references instead of classes, leaving your code easier to migrate in the future.
 *
 * <p>To use an existing function (say, named {@code function}) in a context where the <i>other
 * type</i> of function is expected, use the method reference {@code function::apply}. A future
 * version of {@code com.google.common.base.Function} will be made to <i>extend</i> {@code
 * java.util.function.Function}, making conversion code necessary only in one direction. At that
 * time, this interface will be officially discouraged.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible
public interface Function<F, T> {
  /**
   * Returns the result of applying this function to {@code input}. This method is <i>generally
   * expected</i>, but not absolutely required, to have the following properties:
   *
   * <ul>
   * <li>Its execution does not cause any observable side effects.
   * <li>The computation is <i>consistent with equals</i>; that is, {@link Objects#equal
   *     Objects.equal}{@code (a, b)} implies that {@code Objects.equal(function.apply(a),
   *     function.apply(b))}.
   * </ul>
   *
   * @throws NullPointerException if {@code input} is null and this function does not accept null
   *     arguments
   */
  @Nullable
  @CanIgnoreReturnValue
  T apply(@Nullable F input);

  /**
   * Indicates whether another object is equal to this function.
   *
   * <p>Most implementations will have no reason to override the behavior of {@link Object#equals}.
   * However, an implementation may also choose to return {@code true} whenever {@code object} is a
   * {@link Function} that it considers <i>interchangeable</i> with this one. "Interchangeable"
   * <i>typically</i> means that {@code Objects.equal(this.apply(f), that.apply(f))} is true for all
   * {@code f} of type {@code F}. Note that a {@code false} result from this method does not imply
   * that the functions are known <i>not</i> to be interchangeable.
   */
  @Override
  boolean equals(@Nullable Object object);
}
