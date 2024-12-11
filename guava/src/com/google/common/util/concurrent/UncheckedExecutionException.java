/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.util.concurrent;

import com.google.common.annotations.GwtCompatible;
import javax.annotation.CheckForNull;

/**
 * Unchecked variant of {@link java.util.concurrent.ExecutionException}. As with {@code
 * ExecutionException}, the exception's {@linkplain #getCause() cause} comes from a failed task,
 * possibly run in another thread.
 *
 * <p>{@code UncheckedExecutionException} is intended as an alternative to {@code
 * ExecutionException} when the exception thrown by a task is an unchecked exception. However, it
 * may also wrap a checked exception in some cases.
 *
 * <p>When wrapping an {@code Error} from another thread, prefer {@link ExecutionError}. When
 * wrapping a checked exception, prefer {@code ExecutionException}.
 *
 * @author Charles Fry
 * @since 10.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public class UncheckedExecutionException extends RuntimeException {
  /*
   * Ideally, this class would have exposed only constructors that require a non-null cause. See
   * https://github.com/jspecify/jspecify-reference-checker/blob/61aafa4ae52594830cfc2d61c8b113009dbdb045/src/main/java/com/google/jspecify/nullness/NullSpecTransfer.java#L789
   * and https://github.com/jspecify/jspecify/issues/490.
   *
   * (Perhaps it should also have required that its cause was a RuntimeException. However, that
   * would have required that we throw a different kind of exception for wrapping *checked*
   * exceptions in methods like Futures.getUnchecked and LoadingCache.get.)
   */

  /**
   * Creates a new instance with {@code null} as its detail message and no cause.
   *
   * @deprecated Prefer {@linkplain UncheckedExecutionException(Throwable)} a constructor that
   *     accepts a cause: Users of this class typically expect for instances to have a non-null
   *     cause. At the moment, you can <i>usually</i> still preserve behavior by passing an explicit
   *     {@code null} cause. Note, however, that passing an explicit {@code null} cause prevents
   *     anyone from calling {@link #initCause} later, so it is not quite equivalent to using a
   *     constructor that omits the cause.
   */
  @Deprecated
  protected UncheckedExecutionException() {}

  /**
   * Creates a new instance with the given detail message and no cause.
   *
   * @deprecated Prefer {@linkplain UncheckedExecutionException(String, Throwable)} a constructor
   *     that accepts a cause: Users of this class typically expect for instances to have a non-null
   *     cause. At the moment, you can <i>usually</i> still preserve behavior by passing an explicit
   *     {@code null} cause. Note, however, that passing an explicit {@code null} cause prevents
   *     anyone from calling {@link #initCause} later, so it is not quite equivalent to using a
   *     constructor that omits the cause.
   */
  @Deprecated
  protected UncheckedExecutionException(@CheckForNull String message) {
    super(message);
  }

  /**
   * Creates a new instance with the given detail message and cause. Prefer to provide a
   * non-nullable {@code cause}, as many users expect to find one.
   */
  public UncheckedExecutionException(@CheckForNull String message, @CheckForNull Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new instance with {@code null} as its detail message and the given cause. Prefer to
   * provide a non-nullable {@code cause}, as many users expect to find one.
   */
  public UncheckedExecutionException(@CheckForNull Throwable cause) {
    super(cause);
  }

  private static final long serialVersionUID = 0;
}
