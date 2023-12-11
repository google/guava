/*
 * Copyright (C) 2015 The Guava Authors
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

package com.google.common.util.concurrent;

import static com.google.common.util.concurrent.NullnessCasts.uncheckedCastNullableTToT;

import org.checkerframework.checker.nullness.qual.Nullable;

/** Emulation for InterruptibleTask in GWT. */
@ElementTypesAreNonnullByDefault
abstract class InterruptibleTask<T extends @Nullable Object> implements Runnable {

  @Override
  public void run() {
    T result = null;
    Throwable error = null;
    if (isDone()) {
      return;
    }
    try {
      result = runInterruptibly();
    } catch (Throwable t) {
      error = t;
    }
    if (error == null) {
      // The cast is safe because of the `run` and `error` checks.
      afterRanInterruptiblySuccess(uncheckedCastNullableTToT(result));
    } else {
      afterRanInterruptiblyFailure(error);
    }
  }

  abstract boolean isDone();

  abstract T runInterruptibly() throws Exception;

  abstract void afterRanInterruptiblySuccess(T result);

  abstract void afterRanInterruptiblyFailure(Throwable error);

  final void interruptTask() {}

  abstract String toPendingString();
}
