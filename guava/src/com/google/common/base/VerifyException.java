/*
 * Copyright (C) 2013 The Guava Authors
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
import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * Exception thrown upon the failure of a
 * <a href="http://code.google.com/p/guava-libraries/wiki/ConditionalFailuresExplained">verification
 * check</a>, including those performed by the convenience methods of the {@link Verify} class.
 *
 * @since 17.0
 */
@Beta
@GwtCompatible
public class VerifyException extends RuntimeException {
  /** Constructs a {@code VerifyException} with no message. */
  public VerifyException() {}

  /** Constructs a {@code VerifyException} with the message {@code message}. */
  public VerifyException(@Nullable String message) {
    super(message);
  }

  /**
   * Constructs a {@code VerifyException} with the cause {@code cause} and a message that is
   * {@code null} if {@code cause} is null, and {@code cause.toString()} otherwise.
   *
   * @since 19.0
   */
  public VerifyException(@Nullable Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a {@code VerifyException} with the message {@code message} and the cause
   * {@code cause}.
   *
   * @since 19.0
   */
  public VerifyException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
