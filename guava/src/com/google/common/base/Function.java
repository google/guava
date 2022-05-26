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
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Legacy version of {@link java.util.function.Function java.util.function.Function}.
 *
 * <p>The {@link Functions} class provides common functions and related utilities.
 *
 * <p>As this interface extends {@code java.util.function.Function}, an instance of this type can be
 * used as a {@code java.util.function.Function} directly. To use a {@code
 * java.util.function.Function} in a context where a {@code com.google.common.base.Function} is
 * needed, use {@code function::apply}.
 *
 * <p>This interface is now a legacy type. Use {@code java.util.function.Function} (or the
 * appropriate primitive specialization such as {@code ToIntFunction}) instead whenever possible.
 * Otherwise, at least reduce <i>explicit</i> dependencies on this type by using lambda expressions
 * or method references instead of classes, leaving your code easier to migrate in the future.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/FunctionalExplained">the use of {@code Function}</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible
@FunctionalInterface
@ElementTypesAreNonnullByDefault
public interface Function<F extends @Nullable Object, T extends @Nullable Object>
    extends java.util.function.Function<F, T> {
  @Override
  @ParametricNullness
  T apply(@ParametricNullness F input);

  /**
   * <i>May</i> return {@code true} if {@code object} is a {@code Function} that behaves identically
   * to this function.
   *
   * <p><b>Warning: do not depend</b> on the behavior of this method.
   *
   * <p>Historically, {@code Function} instances in this library have implemented this method to
   * recognize certain cases where distinct {@code Function} instances would in fact behave
   * identically. However, as code migrates to {@code java.util.function}, that behavior will
   * disappear. It is best not to depend on it.
   */
  @Override
  boolean equals(@CheckForNull Object object);
}
