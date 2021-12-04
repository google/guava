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
import static com.google.common.util.concurrent.Futures.getDone;
import static com.google.common.util.concurrent.MoreExecutors.rejectionPropagatingExecutor;
import static com.google.common.util.concurrent.NullnessCasts.uncheckedCastNullableTToT;
import static com.google.common.util.concurrent.Platform.isInstanceOfThrowableClass;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.util.concurrent.internal.InternalFutureFailureAccess;
import com.google.common.util.concurrent.internal.InternalFutures;
import com.google.errorprone.annotations.ForOverride;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Implementations of {@code Futures.catching*}. */
@GwtCompatible
@ElementTypesAreNonnullByDefault
@SuppressWarnings("nullness") // TODO(b/147136275): Remove once our checker understands & and |.
abstract class AbstractCatchingFuture<
        V extends @Nullable Object, X extends Throwable, F, T extends @Nullable Object>
    extends FluentFuture.TrustedFuture<V> implements Runnable {
  static <V extends @Nullable Object, X extends Throwable> ListenableFuture<V> create(
      ListenableFuture<? extends V> input,
      Class<X> exceptionType,
      Function<? super X, ? extends V> fallback,
      Executor executor) {
    CatchingFuture<V, X> future = new CatchingFuture<>(input, exceptionType, fallback);
    input.addListener(future, rejectionPropagatingExecutor(executor, future));
    return future;
  }

  static <X extends Throwable, V extends @Nullable Object> ListenableFuture<V> create(
      ListenableFuture<? extends V> input,
      Class<X> exceptionType,
      AsyncFunction<? super X, ? extends V> fallback,
      Executor executor) {
    AsyncCatchingFuture<V, X> future = new AsyncCatchingFuture<>(input, exceptionType, fallback);
    input.addListener(future, rejectionPropagatingExecutor(executor, future));
    return future;
  }

  /*
   * In certain circumstances, this field might theoretically not be visible to an afterDone() call
   * triggered by cancel(). For details, see the comments on the fields of TimeoutFuture.
   */
  @CheckForNull ListenableFuture<? extends V> inputFuture;
  @CheckForNull Class<X> exceptionType;
  @CheckForNull F fallback;

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
    if (localInputFuture == null | localExceptionType == null | localFallback == null
        // This check, unlike all the others, is a volatile read
        || isCancelled()) {
      return;
    }
    inputFuture = null;

    // For an explanation of the cases here, see the comments on AbstractTransformFuture.run.
    V sourceResult = null;
    Throwable throwable = null;
    try {
      if (localInputFuture instanceof InternalFutureFailureAccess) {
        throwable =
            InternalFutures.tryInternalFastPathGetFailure(
                (InternalFutureFailureAccess) localInputFuture);
      }
      if (throwable == null) {
        sourceResult = getDone(localInputFuture);
      }
    } catch (ExecutionException e) {
      throwable = e.getCause();
      if (throwable == null) {
        throwable =
            new NullPointerException(
                "Future type "
                    + localInputFuture.getClass()
                    + " threw "
                    + e.getClass()
                    + " without a cause");
      }
    } catch (Throwable e) { // this includes cancellation exception
      throwable = e;
    }

    if (throwable == null) {
      /*
       * The cast is safe: There was no exception, so the assignment from getDone must have
       * succeeded.
       */
      set(uncheckedCastNullableTToT(sourceResult));
      return;
    }

    if (!isInstanceOfThrowableClass(throwable, localExceptionType)) {
      setFuture(localInputFuture);
      // TODO(cpovirk): Test that fallback is not run in this case.
      return;
    }

    @SuppressWarnings("unchecked") // verified safe by isInstanceOfThrowableClass
    X castThrowable = (X) throwable;
    T fallbackResult;
    try {
      fallbackResult = doFallback(localFallback, castThrowable);
    } catch (Throwable t) {
      setException(t);
      return;
    } finally {
      exceptionType = null;
      fallback = null;
    }

    setResult(fallbackResult);
  }

  @Override
  @CheckForNull
  protected String pendingToString() {
    ListenableFuture<? extends V> localInputFuture = inputFuture;
    Class<X> localExceptionType = exceptionType;
    F localFallback = fallback;
    String superString = super.pendingToString();
    String resultString = "";
    if (localInputFuture != null) {
      resultString = "inputFuture=[" + localInputFuture + "], ";
    }
    if (localExceptionType != null && localFallback != null) {
      return resultString
          + "exceptionType=["
          + localExceptionType
          + "], fallback=["
          + localFallback
          + "]";
    } else if (superString != null) {
      return resultString + superString;
    }
    return null;
  }

  /** Template method for subtypes to actually run the fallback. */
  @ForOverride
  @ParametricNullness
  abstract T doFallback(F fallback, X throwable) throws Exception;

  /** Template method for subtypes to actually set the result. */
  @ForOverride
  abstract void setResult(@ParametricNullness T result);

  @Override
  protected final void afterDone() {
    maybePropagateCancellationTo(inputFuture);
    this.inputFuture = null;
    this.exceptionType = null;
    this.fallback = null;
  }

  /**
   * An {@link AbstractCatchingFuture} that delegates to an {@link AsyncFunction} and {@link
   * #setFuture(ListenableFuture)}.
   */
  private static final class AsyncCatchingFuture<V extends @Nullable Object, X extends Throwable>
      extends AbstractCatchingFuture<
          V, X, AsyncFunction<? super X, ? extends V>, ListenableFuture<? extends V>> {
    AsyncCatchingFuture(
        ListenableFuture<? extends V> input,
        Class<X> exceptionType,
        AsyncFunction<? super X, ? extends V> fallback) {
      super(input, exceptionType, fallback);
    }

    @Override
    ListenableFuture<? extends V> doFallback(
        AsyncFunction<? super X, ? extends V> fallback, X cause) throws Exception {
      ListenableFuture<? extends V> replacement = fallback.apply(cause);
      checkNotNull(
          replacement,
          "AsyncFunction.apply returned null instead of a Future. "
              + "Did you mean to return immediateFuture(null)? %s",
          fallback);
      return replacement;
    }

    @Override
    void setResult(ListenableFuture<? extends V> result) {
      setFuture(result);
    }
  }

  /**
   * An {@link AbstractCatchingFuture} that delegates to a {@link Function} and {@link
   * #set(Object)}.
   */
  private static final class CatchingFuture<V extends @Nullable Object, X extends Throwable>
      extends AbstractCatchingFuture<V, X, Function<? super X, ? extends V>, V> {
    CatchingFuture(
        ListenableFuture<? extends V> input,
        Class<X> exceptionType,
        Function<? super X, ? extends V> fallback) {
      super(input, exceptionType, fallback);
    }

    @Override
    @ParametricNullness
    V doFallback(Function<? super X, ? extends V> fallback, X cause) throws Exception {
      return fallback.apply(cause);
    }

    @Override
    void setResult(@ParametricNullness V result) {
      set(result);
    }
  }
}
