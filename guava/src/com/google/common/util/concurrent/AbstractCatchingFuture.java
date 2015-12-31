/*
 * Copyright (C) 2006 The Guava Authors
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
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.MoreExecutors.rejectionPropagatingExecutor;
import static com.google.common.util.concurrent.Platform.isInstanceOfThrowableClass;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.errorprone.annotations.ForOverride;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

/**
 * Implementations of {@code Futures.catching*}.
 */
@GwtCompatible
abstract class AbstractCatchingFuture<V, X extends Throwable, F>
    extends AbstractFuture.TrustedFuture<V> implements Runnable {
  static <X extends Throwable, V> ListenableFuture<V> create(
      ListenableFuture<? extends V> input,
      Class<X> exceptionType,
      Function<? super X, ? extends V> fallback) {
    CatchingFuture<V, X> future = new CatchingFuture<V, X>(input, exceptionType, fallback);
    input.addListener(future, directExecutor());
    return future;
  }

  static <V, X extends Throwable> ListenableFuture<V> create(
      ListenableFuture<? extends V> input,
      Class<X> exceptionType,
      Function<? super X, ? extends V> fallback,
      Executor executor) {
    CatchingFuture<V, X> future = new CatchingFuture<V, X>(input, exceptionType, fallback);
    input.addListener(future, rejectionPropagatingExecutor(executor, future));
    return future;
  }

  static <X extends Throwable, V> ListenableFuture<V> create(
      ListenableFuture<? extends V> input,
      Class<X> exceptionType,
      AsyncFunction<? super X, ? extends V> fallback) {
    AsyncCatchingFuture<V, X> future =
        new AsyncCatchingFuture<V, X>(input, exceptionType, fallback);
    input.addListener(future, directExecutor());
    return future;
  }

  static <X extends Throwable, V> ListenableFuture<V> create(
      ListenableFuture<? extends V> input,
      Class<X> exceptionType,
      AsyncFunction<? super X, ? extends V> fallback,
      Executor executor) {
    AsyncCatchingFuture<V, X> future =
        new AsyncCatchingFuture<V, X>(input, exceptionType, fallback);
    input.addListener(future, rejectionPropagatingExecutor(executor, future));
    return future;
  }

  @Nullable ListenableFuture<? extends V> inputFuture;
  @Nullable Class<X> exceptionType;
  @Nullable F fallback;

  AbstractCatchingFuture(
      ListenableFuture<? extends V> inputFuture, Class<X> exceptionType, F fallback) {
    this.inputFuture = checkNotNull(inputFuture);
    this.exceptionType = checkNotNull(exceptionType);
    this.fallback = checkNotNull(fallback);
  }

  @Override
  public final void run() {
    ListenableFuture<? extends V> localInputFuture = inputFuture;
    Class<X> localExceptionType = exceptionType;
    F localFallback = fallback;
    if (localInputFuture == null
        | localExceptionType == null
        | localFallback == null
        | isCancelled()) {
      return;
    }
    inputFuture = null;
    exceptionType = null;
    fallback = null;

    Throwable throwable;
    try {
      set(getUninterruptibly(localInputFuture));
      return;
    } catch (ExecutionException e) {
      throwable = e.getCause();
    } catch (Throwable e) { // this includes cancellation exception
      throwable = e;
    }
    try {
      if (isInstanceOfThrowableClass(throwable, localExceptionType)) {
        @SuppressWarnings("unchecked") // verified safe by isInstance
        X castThrowable = (X) throwable;
        doFallback(localFallback, castThrowable);
      } else {
        setException(throwable);
      }
    } catch (Throwable e) {
      setException(e);
    }
  }

  /** Template method for subtypes to actually run the fallback. */
  @ForOverride
  abstract void doFallback(F fallback, X throwable) throws Exception;

  @Override
  protected final void afterDone() {
    maybePropagateCancellation(inputFuture);
    this.inputFuture = null;
    this.exceptionType = null;
    this.fallback = null;
  }

  /**
   * An {@link AbstractCatchingFuture} that delegates to an {@link AsyncFunction} and
   * {@link #setFuture(ListenableFuture)} to implement {@link #doFallback}
   */
  private static final class AsyncCatchingFuture<V, X extends Throwable>
      extends AbstractCatchingFuture<V, X, AsyncFunction<? super X, ? extends V>> {
    AsyncCatchingFuture(
        ListenableFuture<? extends V> input,
        Class<X> exceptionType,
        AsyncFunction<? super X, ? extends V> fallback) {
      super(input, exceptionType, fallback);
    }

    @Override
    void doFallback(AsyncFunction<? super X, ? extends V> fallback, X cause) throws Exception {
      ListenableFuture<? extends V> replacement = fallback.apply(cause);
      checkNotNull(
          replacement,
          "AsyncFunction.apply returned null instead of a Future. "
              + "Did you mean to return immediateFuture(null)?");
      setFuture(replacement);
    }
  }

  /**
   * An {@link AbstractCatchingFuture} that delegates to a {@link Function} and {@link #set(Object)}
   * to implement {@link #doFallback}
   */
  private static final class CatchingFuture<V, X extends Throwable>
      extends AbstractCatchingFuture<V, X, Function<? super X, ? extends V>> {
    CatchingFuture(
        ListenableFuture<? extends V> input,
        Class<X> exceptionType,
        Function<? super X, ? extends V> fallback) {
      super(input, exceptionType, fallback);
    }

    @Override
    void doFallback(Function<? super X, ? extends V> fallback, X cause) throws Exception {
      V replacement = fallback.apply(cause);
      set(replacement);
    }
  }
}
