/*
 * Copyright (C) 2015 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Thread.currentThread;

import com.google.common.annotations.GwtCompatible;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.Nullable;

/** Methods factored out so that they can be emulated differently in GWT. */
@GwtCompatible(emulated = true)
final class Platform {
  static boolean isInstanceOfThrowableClass(
      @Nullable Throwable t, Class<? extends Throwable> expectedClass) {
    return expectedClass.isInstance(t);
  }

  static void restoreInterruptIfIsInterruptedException(Throwable t) {
    checkNotNull(t); // to satisfy NullPointerTester
    if (t instanceof InterruptedException) {
      currentThread().interrupt();
    }
  }

  static void interruptCurrentThread() {
    Thread.currentThread().interrupt();
  }

  static void rethrowIfErrorOtherThanStackOverflow(Throwable t) {
    checkNotNull(t);
    if (t instanceof Error && !(t instanceof StackOverflowError)) {
      throw (Error) t;
    }
  }

  static <V extends @Nullable Object> V get(AbstractFuture<V> future)
      throws InterruptedException, ExecutionException {
    return future.blockingGet();
  }

  static <V extends @Nullable Object> V get(AbstractFuture<V> future, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return future.blockingGet(timeout, unit);
  }

  private Platform() {}
}
