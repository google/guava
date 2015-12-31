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
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.errorprone.annotations.ForOverride;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

/**
 * Implementations of {@code Futures.transform*}.
 */
@GwtCompatible
abstract class AbstractTransformFuture<I, O, F> extends AbstractFuture.TrustedFuture<O>
    implements Runnable {
  static <I, O> ListenableFuture<O> create(
      ListenableFuture<I> input, AsyncFunction<? super I, ? extends O> function) {
    AsyncTransformFuture<I, O> output = new AsyncTransformFuture<I, O>(input, function);
    input.addListener(output, directExecutor());
    return output;
  }

  static <I, O> ListenableFuture<O> create(
      ListenableFuture<I> input,
      AsyncFunction<? super I, ? extends O> function,
      Executor executor) {
    checkNotNull(executor);
    AsyncTransformFuture<I, O> output = new AsyncTransformFuture<I, O>(input, function);
    input.addListener(output, rejectionPropagatingExecutor(executor, output));
    return output;
  }

  static <I, O> ListenableFuture<O> create(
      ListenableFuture<I> input, Function<? super I, ? extends O> function) {
    checkNotNull(function);
    TransformFuture<I, O> output = new TransformFuture<I, O>(input, function);
    input.addListener(output, directExecutor());
    return output;
  }

  static <I, O> ListenableFuture<O> create(
      ListenableFuture<I> input, Function<? super I, ? extends O> function, Executor executor) {
    checkNotNull(function);
    TransformFuture<I, O> output = new TransformFuture<I, O>(input, function);
    input.addListener(output, rejectionPropagatingExecutor(executor, output));
    return output;
  }

  // In theory, this field might not be visible to a cancel() call in certain circumstances. For
  // details, see the comments on the fields of TimeoutFuture.
  @Nullable ListenableFuture<? extends I> inputFuture;
  @Nullable F function;

  AbstractTransformFuture(ListenableFuture<? extends I> inputFuture, F function) {
    this.inputFuture = checkNotNull(inputFuture);
    this.function = checkNotNull(function);
  }

  @Override
  public final void run() {
    try {
      ListenableFuture<? extends I> localInputFuture = inputFuture;
      F localFunction = function;
      if (isCancelled() | localInputFuture == null | localFunction == null) {
        return;
      }
      inputFuture = null;
      function = null;

      I sourceResult;
      try {
        sourceResult = getUninterruptibly(localInputFuture);
      } catch (CancellationException e) {
        // Cancel this future and return.
        // At this point, inputFuture is cancelled and outputFuture doesn't exist, so the value of
        // mayInterruptIfRunning is irrelevant.
        cancel(false);
        return;
      } catch (ExecutionException e) {
        // Set the cause of the exception as this future's exception.
        setException(e.getCause());
        return;
      }
      doTransform(localFunction, sourceResult);
    } catch (UndeclaredThrowableException e) {
      // Set the cause of the exception as this future's exception.
      setException(e.getCause());
    } catch (Throwable t) {
      // This exception is irrelevant in this thread, but useful for the client.
      setException(t);
    }
  }

  /** Template method for subtypes to actually run the transform. */
  @ForOverride
  abstract void doTransform(F function, I result) throws Exception;

  @Override
  protected final void afterDone() {
    maybePropagateCancellation(inputFuture);
    this.inputFuture = null;
    this.function = null;
  }

  /**
   * An {@link AbstractTransformFuture} that delegates to an {@link AsyncFunction} and
   * {@link #setFuture(ListenableFuture)} to implement {@link #doTransform}.
   */
  private static final class AsyncTransformFuture<I, O>
      extends AbstractTransformFuture<I, O, AsyncFunction<? super I, ? extends O>> {
    AsyncTransformFuture(
        ListenableFuture<? extends I> inputFuture, AsyncFunction<? super I, ? extends O> function) {
      super(inputFuture, function);
    }

    @Override
    void doTransform(AsyncFunction<? super I, ? extends O> function, I input) throws Exception {
      ListenableFuture<? extends O> outputFuture = function.apply(input);
      checkNotNull(
          outputFuture,
          "AsyncFunction.apply returned null instead of a Future. "
              + "Did you mean to return immediateFuture(null)?");
      setFuture(outputFuture);
    }
  }

  /**
   * An {@link AbstractTransformFuture} that delegates to a {@link Function} and
   * {@link #set(Object)} to implement {@link #doTransform}.
   */
  private static final class TransformFuture<I, O>
      extends AbstractTransformFuture<I, O, Function<? super I, ? extends O>> {
    TransformFuture(
        ListenableFuture<? extends I> inputFuture, Function<? super I, ? extends O> function) {
      super(inputFuture, function);
    }

    @Override
    void doTransform(Function<? super I, ? extends O> function, I input) {
      // TODO(lukes): move the UndeclaredThrowable catch block here?
      set(function.apply(input));
    }
  }
}
