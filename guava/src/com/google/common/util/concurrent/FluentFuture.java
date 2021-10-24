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
import static com.google.common.util.concurrent.Internal.toNanosSaturated;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotMock;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link ListenableFuture} that supports fluent chains of operations. For example:
 *
 * <pre>{@code
 * ListenableFuture<Boolean> adminIsLoggedIn =
 *     FluentFuture.from(usersDatabase.getAdminUser())
 *         .transform(User::getId, directExecutor())
 *         .transform(ActivityService::isLoggedIn, threadPool)
 *         .catching(RpcException.class, e -> false, directExecutor());
 * }</pre>
 *
 * <h3>Alternatives</h3>
 *
 * <h4>Frameworks</h4>
 *
 * <p>When chaining together a graph of asynchronous operations, you will often find it easier to
 * use a framework. Frameworks automate the process, often adding features like monitoring,
 * debugging, and cancellation. Examples of frameworks include:
 *
 * <ul>
 *   <li><a href="https://dagger.dev/producers.html">Dagger Producers</a>
 * </ul>
 *
 * <h4>{@link java.util.concurrent.CompletableFuture} / {@link java.util.concurrent.CompletionStage}
 * </h4>
 *
 * <p>Users of {@code CompletableFuture} will likely want to continue using {@code
 * CompletableFuture}. {@code FluentFuture} is targeted at people who use {@code ListenableFuture},
 * who can't use Java 8, or who want an API more focused than {@code CompletableFuture}. (If you
 * need to adapt between {@code CompletableFuture} and {@code ListenableFuture}, consider <a
 * href="https://github.com/lukas-krecan/future-converter">Future Converter</a>.)
 *
 * <h3>Extension</h3>
 *
 * If you want a class like {@code FluentFuture} but with extra methods, we recommend declaring your
 * own subclass of {@link ListenableFuture}, complete with a method like {@link #from} to adapt an
 * existing {@code ListenableFuture}, implemented atop a {@link ForwardingListenableFuture} that
 * forwards to that future and adds the desired methods.
 *
 * @since 23.0
 */
@Beta
@DoNotMock("Use FluentFuture.from(Futures.immediate*Future) or SettableFuture")
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public abstract class FluentFuture<V extends @Nullable Object>
    extends GwtFluentFutureCatchingSpecialization<V> {

  /**
   * A less abstract subclass of AbstractFuture. This can be used to optimize setFuture by ensuring
   * that {@link #get} calls exactly the implementation of {@link AbstractFuture#get}.
   */
  abstract static class TrustedFuture<V extends @Nullable Object> extends FluentFuture<V>
      implements AbstractFuture.Trusted<V> {
    @CanIgnoreReturnValue
    @Override
    @ParametricNullness
    public final V get() throws InterruptedException, ExecutionException {
      return super.get();
    }

    @CanIgnoreReturnValue
    @Override
    @ParametricNullness
    public final V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return super.get(timeout, unit);
    }

    @Override
    public final boolean isDone() {
      return super.isDone();
    }

    @Override
    public final boolean isCancelled() {
      return super.isCancelled();
    }

    @Override
    public final void addListener(Runnable listener, Executor executor) {
      super.addListener(listener, executor);
    }

    @CanIgnoreReturnValue
    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
      return super.cancel(mayInterruptIfRunning);
    }
  }

  FluentFuture() {}

  /**
   * Converts the given {@code ListenableFuture} to an equivalent {@code FluentFuture}.
   *
   * <p>If the given {@code ListenableFuture} is already a {@code FluentFuture}, it is returned
   * directly. If not, it is wrapped in a {@code FluentFuture} that delegates all calls to the
   * original {@code ListenableFuture}.
   */
  public static <V extends @Nullable Object> FluentFuture<V> from(ListenableFuture<V> future) {
    return future instanceof FluentFuture
        ? (FluentFuture<V>) future
        : new ForwardingFluentFuture<V>(future);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 28.0
   */
  @Deprecated
  public static <V extends @Nullable Object> FluentFuture<V> from(FluentFuture<V> future) {
    return checkNotNull(future);
  }

  /**
   * Returns a {@code Future} whose result is taken from this {@code Future} or, if this {@code
   * Future} fails with the given {@code exceptionType}, from the result provided by the {@code
   * fallback}. {@link Function#apply} is not invoked until the primary input has failed, so if the
   * primary input succeeds, it is never invoked. If, during the invocation of {@code fallback}, an
   * exception is thrown, this exception is used as the result of the output {@code Future}.
   *
   * <p>Usage example:
   *
   * <pre>{@code
   * // Falling back to a zero counter in case an exception happens when processing the RPC to fetch
   * // counters.
   * ListenableFuture<Integer> faultTolerantFuture =
   *     fetchCounters().catching(FetchException.class, x -> 0, directExecutor());
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link #addListener} documentation. All its warnings about heavyweight
   * listeners are also applicable to heavyweight functions passed to this method.
   *
   * <p>This method is similar to {@link java.util.concurrent.CompletableFuture#exceptionally}. It
   * can also serve some of the use cases of {@link java.util.concurrent.CompletableFuture#handle}
   * and {@link java.util.concurrent.CompletableFuture#handleAsync} when used along with {@link
   * #transform}.
   *
   * @param exceptionType the exception type that triggers use of {@code fallback}. The exception
   *     type is matched against the input's exception. "The input's exception" means the cause of
   *     the {@link ExecutionException} thrown by {@code input.get()} or, if {@code get()} throws a
   *     different kind of exception, that exception itself. To avoid hiding bugs and other
   *     unrecoverable errors, callers should prefer more specific types, avoiding {@code
   *     Throwable.class} in particular.
   * @param fallback the {@link Function} to be called if the input fails with the expected
   *     exception type. The function's argument is the input's exception. "The input's exception"
   *     means the cause of the {@link ExecutionException} thrown by {@code this.get()} or, if
   *     {@code get()} throws a different kind of exception, that exception itself.
   * @param executor the executor that runs {@code fallback} if the input fails
   */
  @Partially.GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
  public final <X extends Throwable> FluentFuture<V> catching(
      Class<X> exceptionType, Function<? super X, ? extends V> fallback, Executor executor) {
    return (FluentFuture<V>) Futures.catching(this, exceptionType, fallback, executor);
  }

  /**
   * Returns a {@code Future} whose result is taken from this {@code Future} or, if this {@code
   * Future} fails with the given {@code exceptionType}, from the result provided by the {@code
   * fallback}. {@link AsyncFunction#apply} is not invoked until the primary input has failed, so if
   * the primary input succeeds, it is never invoked. If, during the invocation of {@code fallback},
   * an exception is thrown, this exception is used as the result of the output {@code Future}.
   *
   * <p>Usage examples:
   *
   * <pre>{@code
   * // Falling back to a zero counter in case an exception happens when processing the RPC to fetch
   * // counters.
   * ListenableFuture<Integer> faultTolerantFuture =
   *     fetchCounters().catchingAsync(
   *         FetchException.class, x -> immediateFuture(0), directExecutor());
   * }</pre>
   *
   * <p>The fallback can also choose to propagate the original exception when desired:
   *
   * <pre>{@code
   * // Falling back to a zero counter only in case the exception was a
   * // TimeoutException.
   * ListenableFuture<Integer> faultTolerantFuture =
   *     fetchCounters().catchingAsync(
   *         FetchException.class,
   *         e -> {
   *           if (omitDataOnFetchFailure) {
   *             return immediateFuture(0);
   *           }
   *           throw e;
   *         },
   *         directExecutor());
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link #addListener} documentation. All its warnings about heavyweight
   * listeners are also applicable to heavyweight functions passed to this method. (Specifically,
   * {@code directExecutor} functions should avoid heavyweight operations inside {@code
   * AsyncFunction.apply}. Any heavyweight operations should occur in other threads responsible for
   * completing the returned {@code Future}.)
   *
   * <p>This method is similar to {@link java.util.concurrent.CompletableFuture#exceptionally}. It
   * can also serve some of the use cases of {@link java.util.concurrent.CompletableFuture#handle}
   * and {@link java.util.concurrent.CompletableFuture#handleAsync} when used along with {@link
   * #transform}.
   *
   * @param exceptionType the exception type that triggers use of {@code fallback}. The exception
   *     type is matched against the input's exception. "The input's exception" means the cause of
   *     the {@link ExecutionException} thrown by {@code this.get()} or, if {@code get()} throws a
   *     different kind of exception, that exception itself. To avoid hiding bugs and other
   *     unrecoverable errors, callers should prefer more specific types, avoiding {@code
   *     Throwable.class} in particular.
   * @param fallback the {@link AsyncFunction} to be called if the input fails with the expected
   *     exception type. The function's argument is the input's exception. "The input's exception"
   *     means the cause of the {@link ExecutionException} thrown by {@code input.get()} or, if
   *     {@code get()} throws a different kind of exception, that exception itself.
   * @param executor the executor that runs {@code fallback} if the input fails
   */
  @Partially.GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
  public final <X extends Throwable> FluentFuture<V> catchingAsync(
      Class<X> exceptionType, AsyncFunction<? super X, ? extends V> fallback, Executor executor) {
    return (FluentFuture<V>) Futures.catchingAsync(this, exceptionType, fallback, executor);
  }

  /**
   * Returns a future that delegates to this future but will finish early (via a {@link
   * TimeoutException} wrapped in an {@link ExecutionException}) if the specified timeout expires.
   * If the timeout expires, not only will the output future finish, but also the input future
   * ({@code this}) will be cancelled and interrupted.
   *
   * @param timeout when to time out the future
   * @param scheduledExecutor The executor service to enforce the timeout.
   * @since 28.0
   */
  @GwtIncompatible // ScheduledExecutorService
  public final FluentFuture<V> withTimeout(
      Duration timeout, ScheduledExecutorService scheduledExecutor) {
    return withTimeout(toNanosSaturated(timeout), TimeUnit.NANOSECONDS, scheduledExecutor);
  }

  /**
   * Returns a future that delegates to this future but will finish early (via a {@link
   * TimeoutException} wrapped in an {@link ExecutionException}) if the specified timeout expires.
   * If the timeout expires, not only will the output future finish, but also the input future
   * ({@code this}) will be cancelled and interrupted.
   *
   * @param timeout when to time out the future
   * @param unit the time unit of the time parameter
   * @param scheduledExecutor The executor service to enforce the timeout.
   */
  @GwtIncompatible // ScheduledExecutorService
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public final FluentFuture<V> withTimeout(
      long timeout, TimeUnit unit, ScheduledExecutorService scheduledExecutor) {
    return (FluentFuture<V>) Futures.withTimeout(this, timeout, unit, scheduledExecutor);
  }

  /**
   * Returns a new {@code Future} whose result is asynchronously derived from the result of this
   * {@code Future}. If the input {@code Future} fails, the returned {@code Future} fails with the
   * same exception (and the function is not invoked).
   *
   * <p>More precisely, the returned {@code Future} takes its result from a {@code Future} produced
   * by applying the given {@code AsyncFunction} to the result of the original {@code Future}.
   * Example usage:
   *
   * <pre>{@code
   * FluentFuture<RowKey> rowKeyFuture = FluentFuture.from(indexService.lookUp(query));
   * ListenableFuture<QueryResult> queryFuture =
   *     rowKeyFuture.transformAsync(dataService::readFuture, executor);
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link #addListener} documentation. All its warnings about heavyweight
   * listeners are also applicable to heavyweight functions passed to this method. (Specifically,
   * {@code directExecutor} functions should avoid heavyweight operations inside {@code
   * AsyncFunction.apply}. Any heavyweight operations should occur in other threads responsible for
   * completing the returned {@code Future}.)
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in sync with that of the
   * input future and that of the future returned by the chain function. That is, if the returned
   * {@code Future} is cancelled, it will attempt to cancel the other two, and if either of the
   * other two is cancelled, the returned {@code Future} will receive a callback in which it will
   * attempt to cancel itself.
   *
   * <p>This method is similar to {@link java.util.concurrent.CompletableFuture#thenCompose} and
   * {@link java.util.concurrent.CompletableFuture#thenComposeAsync}. It can also serve some of the
   * use cases of {@link java.util.concurrent.CompletableFuture#handle} and {@link
   * java.util.concurrent.CompletableFuture#handleAsync} when used along with {@link #catching}.
   *
   * @param function A function to transform the result of this future to the result of the output
   *     future
   * @param executor Executor to run the function in.
   * @return A future that holds result of the function (if the input succeeded) or the original
   *     input's failure (if not)
   */
  public final <T extends @Nullable Object> FluentFuture<T> transformAsync(
      AsyncFunction<? super V, T> function, Executor executor) {
    return (FluentFuture<T>) Futures.transformAsync(this, function, executor);
  }

  /**
   * Returns a new {@code Future} whose result is derived from the result of this {@code Future}. If
   * this input {@code Future} fails, the returned {@code Future} fails with the same exception (and
   * the function is not invoked). Example usage:
   *
   * <pre>{@code
   * ListenableFuture<List<Row>> rowsFuture =
   *     queryFuture.transform(QueryResult::getRows, executor);
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link #addListener} documentation. All its warnings about heavyweight
   * listeners are also applicable to heavyweight functions passed to this method.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in sync with that of the
   * input future. That is, if the returned {@code Future} is cancelled, it will attempt to cancel
   * the input, and if the input is cancelled, the returned {@code Future} will receive a callback
   * in which it will attempt to cancel itself.
   *
   * <p>An example use of this method is to convert a serializable object returned from an RPC into
   * a POJO.
   *
   * <p>This method is similar to {@link java.util.concurrent.CompletableFuture#thenApply} and
   * {@link java.util.concurrent.CompletableFuture#thenApplyAsync}. It can also serve some of the
   * use cases of {@link java.util.concurrent.CompletableFuture#handle} and {@link
   * java.util.concurrent.CompletableFuture#handleAsync} when used along with {@link #catching}.
   *
   * @param function A Function to transform the results of this future to the results of the
   *     returned future.
   * @param executor Executor to run the function in.
   * @return A future that holds result of the transformation.
   */
  public final <T extends @Nullable Object> FluentFuture<T> transform(
      Function<? super V, T> function, Executor executor) {
    return (FluentFuture<T>) Futures.transform(this, function, executor);
  }

  /**
   * Registers separate success and failure callbacks to be run when this {@code Future}'s
   * computation is {@linkplain java.util.concurrent.Future#isDone() complete} or, if the
   * computation is already complete, immediately.
   *
   * <p>The callback is run on {@code executor}. There is no guaranteed ordering of execution of
   * callbacks, but any callback added through this method is guaranteed to be called once the
   * computation is complete.
   *
   * <p>Example:
   *
   * <pre>{@code
   * future.addCallback(
   *     new FutureCallback<QueryResult>() {
   *       public void onSuccess(QueryResult result) {
   *         storeInCache(result);
   *       }
   *       public void onFailure(Throwable t) {
   *         reportError(t);
   *       }
   *     }, executor);
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link #addListener} documentation. All its warnings about heavyweight
   * listeners are also applicable to heavyweight callbacks passed to this method.
   *
   * <p>For a more general interface to attach a completion listener, see {@link #addListener}.
   *
   * <p>This method is similar to {@link java.util.concurrent.CompletableFuture#whenComplete} and
   * {@link java.util.concurrent.CompletableFuture#whenCompleteAsync}. It also serves the use case
   * of {@link java.util.concurrent.CompletableFuture#thenAccept} and {@link
   * java.util.concurrent.CompletableFuture#thenAcceptAsync}.
   *
   * @param callback The callback to invoke when this {@code Future} is completed.
   * @param executor The executor to run {@code callback} when the future completes.
   */
  public final void addCallback(FutureCallback<? super V> callback, Executor executor) {
    Futures.addCallback(this, callback, executor);
  }
}
