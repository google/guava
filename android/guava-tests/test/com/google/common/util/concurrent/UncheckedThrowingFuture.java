/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Future} implementation which always throws directly from calls to {@code get()} (i.e.
 * not wrapped in {@code ExecutionException}. For just a normal failure, use {@link
 * SettableFuture}).
 *
 * <p>Useful for testing the behavior of Future utilities against odd futures.
 *
 * @author Anthony Zana
 */
@GwtCompatible
final class UncheckedThrowingFuture<V> implements ListenableFuture<V> {
  private final Error error;
  private final RuntimeException runtime;

  public static <V> ListenableFuture<V> throwingError(Error error) {
    return new UncheckedThrowingFuture<V>(error);
  }

  public static <V> ListenableFuture<V> throwingRuntimeException(RuntimeException e) {
    return new UncheckedThrowingFuture<V>(e);
  }

  private UncheckedThrowingFuture(Error error) {
    this.error = checkNotNull(error);
    this.runtime = null;
  }

  public UncheckedThrowingFuture(RuntimeException e) {
    this.runtime = checkNotNull(e);
    this.error = null;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public V get() {
    throwOnGet();
    throw new AssertionError("Unreachable");
  }

  @Override
  public V get(long timeout, TimeUnit unit) {
    checkNotNull(unit);
    throwOnGet();
    throw new AssertionError();
  }

  @Override
  public void addListener(Runnable listener, Executor executor) {
    checkNotNull(listener);
    // TODO(cpovirk): Catch RuntimeExceptions
    executor.execute(listener);
  }

  private void throwOnGet() {
    if (error != null) {
      throw error;
    } else {
      throw runtime;
    }
  }
}
