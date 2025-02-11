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

package com.google.common.util.concurrent;

import com.google.common.annotations.GwtCompatible;

/**
 * Static utility methods pertaining to the {@link Runnable} interface.
 *
 * @since 16.0
 */
@GwtCompatible
public final class Runnables {
  /*
   * If we inline this, it's not longer a singleton under Android (at least under the Lollipop
   * version that we're testing under) or J2CL.
   *
   * That's not necessarily a real-world problem, but it does break our tests.
   */
  @SuppressWarnings({"InlineLambdaConstant", "UnnecessaryLambda"})
  private static final Runnable EMPTY_RUNNABLE = () -> {};

  /** Returns a {@link Runnable} instance that does nothing when run. */
  public static Runnable doNothing() {
    return EMPTY_RUNNABLE;
  }

  private Runnables() {}
}
