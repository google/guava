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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.CollectionFuture.ListFuture;
import com.google.common.util.concurrent.ImmediateFuture.ImmediateCancelledFuture;
import com.google.common.util.concurrent.ImmediateFuture.ImmediateFailedCheckedFuture;
import com.google.common.util.concurrent.ImmediateFuture.ImmediateFailedFuture;
import com.google.common.util.concurrent.ImmediateFuture.ImmediateSuccessfulCheckedFuture;
import com.google.common.util.concurrent.ImmediateFuture.ImmediateSuccessfulFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to the {@link Future} interface.
 *
 * <p>Many of these methods use the {@link ListenableFuture} API; consult the Guava User Guide
 * article on <a href="https://github.com/google/guava/wiki/ListenableFutureExplained">
 * {@code ListenableFuture}</a>.
 *
 * @author Kevin Bourrillion
 * @author Nishant Thakkar
 * @author Sven Mawson
 * @since 1.0
 */
@Beta
@GwtCompatible(emulated = true)
public final class Futures extends GwtFuturesCatchingSpecialization {

  // A note on memory visibility.
  // Many of the utilities in this class (transform, withFallback, withTimeout, asList, combine)
  // have two requirements that significantly complicate their design.
  // 1. Cancellation should propagate from the returned future to the input future(s).
  // 2. The returned futures shouldn't unnecessarily 'pin' their inputs after completion.
  //
  // A consequence of these requirements is that the delegate futures cannot be stored in
  // final fields.
  //
  // For simplicity the rest of this description will discuss Futures.catching since it is the
  // simplest instance, though very similar descriptions apply to many other classes in this file.
  //
  // In the constructor of AbstractCatchingFuture, the delegate future is assigned to a field
  // 'inputFuture'. That field is non-final and non-volatile. There are 2 places where the
  // 'inputFuture' field is read and where we will have to consider visibility of the write
  // operation in the constructor.
  //
  // 1. In the listener that performs the callback. In this case it is fine since inputFuture is
  //    assigned prior to calling addListener, and addListener happens-before any invocation of the
  //    listener. Notably, this means that 'volatile' is unnecessary to make 'inputFuture' visible
  //    to the listener.
  //
  // 2. In done() where we may propagate cancellation to the input. In this case it is _not_ fine.
  //    There is currently nothing that enforces that the write to inputFuture in the constructor is
  //    visible to done(). This is because there is no happens before edge between the write and a
  //    (hypothetical) unsafe read by our caller. Note: adding 'volatile' does not fix this issue,
  //    it would just add an edge such that if done() observed non-null, then it would also
  //    definitely observe all earlier writes, but we still have no guarantee that done() would see
  //    the inital write (just stronger guarantees if it does).
  //
  // See: http://cs.oswego.edu/pipermail/concurrency-interest/2015-January/013800.html
  // For a (long) discussion about this specific issue and the general futility of life.
  //
  // For the time being we are OK with the problem discussed above since it requires a caller to
  // introduce a very specific kind of data-race. And given the other operations performed by these
  // methods that involve volatile read/write operations, in practice there is no issue. Also, the
  // way in such a visibility issue would surface is most likely as a failure of cancel() to
  // propagate to the input. Cancellation propagation is fundamentally racy so this is fine.
  //
  // Future versions of the JMM may revise safe construction semantics in such a way that we can
  // safely publish these objects and we won't need this whole discussion.
  // TODO(user,lukes): consider adding volatile to all these fields since in current known JVMs
  // that should resolve the issue. This comes at the cost of adding more write barriers to the
  // implementations.

  private Futures() {}

  /**
   * Creates a {@link CheckedFuture} out of a normal {@link ListenableFuture} and a {@link Function}
   * that maps from {@link Exception} instances into the appropriate checked type.
   *
   * <p><b>Warning:</b> We recommend against using {@code CheckedFuture} in new projects. {@code
   * CheckedFuture} is difficult to build libraries atop. {@code CheckedFuture} ports of methods
   * like {@link Futures#transformAsync} have historically had bugs, and some of these bugs are
   * necessary, unavoidable consequences of the {@code CheckedFuture} API. Additionally, {@code
   * CheckedFuture} encourages users to take exceptions from one thread and rethrow them in another,
   * producing confusing stack traces.
   *
   * <p>The given mapping function will be applied to an {@link InterruptedException}, a {@link
   * CancellationException}, or an {@link ExecutionException}. See {@link Future#get()} for details
   * on the exceptions thrown.
   *
   * @since 9.0 (source-compatible since 1.0)
   */
  @GwtIncompatible // TODO
  public static <V, X extends Exception> CheckedFuture<V, X> makeChecked(
      ListenableFuture<V> future, Function<? super Exception, X> mapper) {
    return new MappingCheckedFuture<V, X>(checkNotNull(future), mapper);
  }

  /**
   * Creates a {@code ListenableFuture} which has its value set immediately upon construction. The
   * getters just return the value. This {@code Future} can't be canceled or timed out and its
   * {@code isDone()} method always returns {@code true}.
   */
  public static <V> ListenableFuture<V> immediateFuture(@Nullable V value) {
    if (value == null) {
      // This cast is safe because null is assignable to V for all V (i.e. it is covariant)
      @SuppressWarnings({"unchecked", "rawtypes"})
      ListenableFuture<V> typedNull = (ListenableFuture) ImmediateSuccessfulFuture.NULL;
      return typedNull;
    }
    return new ImmediateSuccessfulFuture<V>(value);
  }

  /**
   * Returns a {@code CheckedFuture} which has its value set immediately upon construction.
   *
   * <p>The returned {@code Future} can't be cancelled, and its {@code isDone()} method always
   * returns {@code true}. Calling {@code get()} or {@code checkedGet()} will immediately return the
   * provided value.
   */
  @GwtIncompatible // TODO
  public static <V, X extends Exception> CheckedFuture<V, X> immediateCheckedFuture(
      @Nullable V value) {
    return new ImmediateSuccessfulCheckedFuture<V, X>(value);
  }

  /**
   * Returns a {@code ListenableFuture} which has an exception set immediately upon construction.
   *
   * <p>The returned {@code Future} can't be cancelled, and its {@code isDone()} method always
   * returns {@code true}. Calling {@code get()} will immediately throw the provided {@code
   * Throwable} wrapped in an {@code ExecutionException}.
   */
  public static <V> ListenableFuture<V> immediateFailedFuture(Throwable throwable) {
    checkNotNull(throwable);
    return new ImmediateFailedFuture<V>(throwable);
  }

  /**
   * Creates a {@code ListenableFuture} which is cancelled immediately upon construction, so that
   * {@code isCancelled()} always returns {@code true}.
   *
   * @since 14.0
   */
  public static <V> ListenableFuture<V> immediateCancelledFuture() {
    return new ImmediateCancelledFuture<V>();
  }

  /**
   * Returns a {@code CheckedFuture} which has an exception set immediately upon construction.
   *
   * <p>The returned {@code Future} can't be cancelled, and its {@code isDone()} method always
   * returns {@code true}. Calling {@code get()} will immediately throw the provided {@code
   * Exception} wrapped in an {@code ExecutionException}, and calling {@code checkedGet()} will
   * throw the provided exception itself.
   */
  @GwtIncompatible // TODO
  public static <V, X extends Exception> CheckedFuture<V, X> immediateFailedCheckedFuture(
      X exception) {
    checkNotNull(exception);
    return new ImmediateFailedCheckedFuture<V, X>(exception);
  }

  /**
   * Returns a {@code Future} whose result is taken from the given primary {@code input} or, if the
   * primary input fails with the given {@code exceptionType}, from the result provided by the
   * {@code fallback}. {@link Function#apply} is not invoked until the primary input has failed, so
   * if the primary input succeeds, it is never invoked. If, during the invocation of {@code
   * fallback}, an exception is thrown, this exception is used as the result of the output {@code
   * Future}.
   *
   * <p>Usage example:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter in case an exception happens when
   *   // processing the RPC to fetch counters.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.catching(
   *       fetchCounterFuture, FetchException.class,
   *       new Function<FetchException, Integer>() {
   *         public Integer apply(FetchException e) {
   *           return 0;
   *         }
   *       });}</pre>
   *
   * <p>This overload, which does not accept an executor, uses {@code directExecutor}, a dangerous
   * choice in some cases. See the discussion in the {@link ListenableFuture#addListener
   * ListenableFuture.addListener} documentation. The documentation's warnings about "lightweight
   * listeners" refer here to the work done during {@code Function.apply}.
   *
   * @param input the primary input {@code Future}
   * @param exceptionType the exception type that triggers use of {@code fallback}. The exception
   *     type is matched against the input's exception. "The input's exception" means the cause of
   *     the {@link ExecutionException} thrown by {@code input.get()} or, if {@code get()} throws a
   *     different kind of exception, that exception itself. To avoid hiding bugs and other
   *     unrecoverable errors, callers should prefer more specific types, avoiding {@code
   *     Throwable.class} in particular.
   * @param fallback the {@link Function} to be called if {@code input} fails with the expected
   *     exception type. The function's argument is the input's exception. "The input's exception"
   *     means the cause of the {@link ExecutionException} thrown by {@code input.get()} or, if
   *     {@code get()} throws a different kind of exception, that exception itself.
   * @since 19.0
   */
  @Partially.GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
  public static <V, X extends Throwable> ListenableFuture<V> catching(
      ListenableFuture<? extends V> input,
      Class<X> exceptionType,
      Function<? super X, ? extends V> fallback) {
    return AbstractCatchingFuture.create(input, exceptionType, fallback);
  }

  /**
   * Returns a {@code Future} whose result is taken from the given primary {@code input} or, if the
   * primary input fails with the given {@code exceptionType}, from the result provided by the
   * {@code fallback}. {@link Function#apply} is not invoked until the primary input has failed, so
   * if the primary input succeeds, it is never invoked. If, during the invocation of {@code
   * fallback}, an exception is thrown, this exception is used as the result of the output {@code
   * Future}.
   *
   * <p>Usage example:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter in case an exception happens when
   *   // processing the RPC to fetch counters.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.catching(
   *       fetchCounterFuture, FetchException.class,
   *       new Function<FetchException, Integer>() {
   *         public Integer apply(FetchException e) {
   *           return 0;
   *         }
   *       }, directExecutor());}</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. The documentation's warnings about "lightweight listeners" refer here to the
   * work done during {@code Function.apply}.
   *
   * @param input the primary input {@code Future}
   * @param exceptionType the exception type that triggers use of {@code fallback}. The exception
   *     type is matched against the input's exception. "The input's exception" means the cause of
   *     the {@link ExecutionException} thrown by {@code input.get()} or, if {@code get()} throws a
   *     different kind of exception, that exception itself. To avoid hiding bugs and other
   *     unrecoverable errors, callers should prefer more specific types, avoiding {@code
   *     Throwable.class} in particular.
   * @param fallback the {@link Function} to be called if {@code input} fails with the expected
   *     exception type. The function's argument is the input's exception. "The input's exception"
   *     means the cause of the {@link ExecutionException} thrown by {@code input.get()} or, if
   *     {@code get()} throws a different kind of exception, that exception itself.
   * @param executor the executor that runs {@code fallback} if {@code input} fails
   * @since 19.0
   */
  @Partially.GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
  public static <V, X extends Throwable> ListenableFuture<V> catching(
      ListenableFuture<? extends V> input,
      Class<X> exceptionType,
      Function<? super X, ? extends V> fallback,
      Executor executor) {
    return AbstractCatchingFuture.create(input, exceptionType, fallback, executor);
  }

  /**
   * Returns a {@code Future} whose result is taken from the given primary {@code input} or, if the
   * primary input fails with the given {@code exceptionType}, from the result provided by the
   * {@code fallback}. {@link AsyncFunction#apply} is not invoked until the primary input has
   * failed, so if the primary input succeeds, it is never invoked. If, during the invocation of
   * {@code fallback}, an exception is thrown, this exception is used as the result of the output
   * {@code Future}.
   *
   * <p>Usage examples:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter in case an exception happens when
   *   // processing the RPC to fetch counters.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.catchingAsync(
   *       fetchCounterFuture, FetchException.class,
   *       new AsyncFunction<FetchException, Integer>() {
   *         public ListenableFuture<Integer> apply(FetchException e) {
   *           return immediateFuture(0);
   *         }
   *       });}</pre>
   *
   * <p>The fallback can also choose to propagate the original exception when desired:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter only in case the exception was a
   *   // TimeoutException.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.catchingAsync(
   *       fetchCounterFuture, FetchException.class,
   *       new AsyncFunction<FetchException, Integer>() {
   *         public ListenableFuture<Integer> apply(FetchException e)
   *             throws FetchException {
   *           if (omitDataOnFetchFailure) {
   *             return immediateFuture(0);
   *           }
   *           throw e;
   *         }
   *       });}</pre>
   *
   * <p>This overload, which does not accept an executor, uses {@code directExecutor}, a dangerous
   * choice in some cases. See the discussion in the {@link ListenableFuture#addListener
   * ListenableFuture.addListener} documentation. The documentation's warnings about "lightweight
   * listeners" refer here to the work done during {@code AsyncFunction.apply}, not to any work done
   * to complete the returned {@code Future}.
   *
   * @param input the primary input {@code Future}
   * @param exceptionType the exception type that triggers use of {@code fallback}. The exception
   *     type is matched against the input's exception. "The input's exception" means the cause of
   *     the {@link ExecutionException} thrown by {@code input.get()} or, if {@code get()} throws a
   *     different kind of exception, that exception itself. To avoid hiding bugs and other
   *     unrecoverable errors, callers should prefer more specific types, avoiding {@code
   *     Throwable.class} in particular.
   * @param fallback the {@link AsyncFunction} to be called if {@code input} fails with the expected
   *     exception type. The function's argument is the input's exception. "The input's exception"
   *     means the cause of the {@link ExecutionException} thrown by {@code input.get()} or, if
   *     {@code get()} throws a different kind of exception, that exception itself.
   * @since 19.0 (similar functionality in 14.0 as {@code withFallback})
   */
  @CanIgnoreReturnValue // TODO(kak): @CheckReturnValue
  @Partially.GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
  public static <V, X extends Throwable> ListenableFuture<V> catchingAsync(
      ListenableFuture<? extends V> input,
      Class<X> exceptionType,
      AsyncFunction<? super X, ? extends V> fallback) {
    return AbstractCatchingFuture.create(input, exceptionType, fallback);
  }

  /**
   * Returns a {@code Future} whose result is taken from the given primary {@code input} or, if the
   * primary input fails with the given {@code exceptionType}, from the result provided by the
   * {@code fallback}. {@link AsyncFunction#apply} is not invoked until the primary input has
   * failed, so if the primary input succeeds, it is never invoked. If, during the invocation of
   * {@code fallback}, an exception is thrown, this exception is used as the result of the output
   * {@code Future}.
   *
   * <p>Usage examples:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter in case an exception happens when
   *   // processing the RPC to fetch counters.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.catchingAsync(
   *       fetchCounterFuture, FetchException.class,
   *       new AsyncFunction<FetchException, Integer>() {
   *         public ListenableFuture<Integer> apply(FetchException e) {
   *           return immediateFuture(0);
   *         }
   *       }, directExecutor());}</pre>
   *
   * <p>The fallback can also choose to propagate the original exception when desired:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter only in case the exception was a
   *   // TimeoutException.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.catchingAsync(
   *       fetchCounterFuture, FetchException.class,
   *       new AsyncFunction<FetchException, Integer>() {
   *         public ListenableFuture<Integer> apply(FetchException e)
   *             throws FetchException {
   *           if (omitDataOnFetchFailure) {
   *             return immediateFuture(0);
   *           }
   *           throw e;
   *         }
   *       }, directExecutor());}</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. The documentation's warnings about "lightweight listeners" refer here to the
   * work done during {@code AsyncFunction.apply}, not to any work done to complete the returned
   * {@code Future}.
   *
   * @param input the primary input {@code Future}
   * @param exceptionType the exception type that triggers use of {@code fallback}. The exception
   *     type is matched against the input's exception. "The input's exception" means the cause of
   *     the {@link ExecutionException} thrown by {@code input.get()} or, if {@code get()} throws a
   *     different kind of exception, that exception itself. To avoid hiding bugs and other
   *     unrecoverable errors, callers should prefer more specific types, avoiding {@code
   *     Throwable.class} in particular.
   * @param fallback the {@link AsyncFunction} to be called if {@code input} fails with the expected
   *     exception type. The function's argument is the input's exception. "The input's exception"
   *     means the cause of the {@link ExecutionException} thrown by {@code input.get()} or, if
   *     {@code get()} throws a different kind of exception, that exception itself.
   * @param executor the executor that runs {@code fallback} if {@code input} fails
   * @since 19.0 (similar functionality in 14.0 as {@code withFallback})
   */
  @CanIgnoreReturnValue // TODO(kak): @CheckReturnValue
  @Partially.GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
  public static <V, X extends Throwable> ListenableFuture<V> catchingAsync(
      ListenableFuture<? extends V> input,
      Class<X> exceptionType,
      AsyncFunction<? super X, ? extends V> fallback,
      Executor executor) {
    return AbstractCatchingFuture.create(input, exceptionType, fallback, executor);
  }

  /**
   * Returns a future that delegates to another but will finish early (via a {@link
   * TimeoutException} wrapped in an {@link ExecutionException}) if the specified duration expires.
   *
   * <p>The delegate future is interrupted and cancelled if it times out.
   *
   * @param delegate The future to delegate to.
   * @param time when to timeout the future
   * @param unit the time unit of the time parameter
   * @param scheduledExecutor The executor service to enforce the timeout.
   *
   * @since 19.0
   */
  @GwtIncompatible // java.util.concurrent.ScheduledExecutorService
  public static <V> ListenableFuture<V> withTimeout(
      ListenableFuture<V> delegate,
      long time,
      TimeUnit unit,
      ScheduledExecutorService scheduledExecutor) {
    return TimeoutFuture.create(delegate, time, unit, scheduledExecutor);
  }

  /**
   * Returns a new {@code Future} whose result is asynchronously derived from the result of the
   * given {@code Future}. If the given {@code Future} fails, the returned {@code Future} fails with
   * the same exception (and the function is not invoked).
   *
   * <p>More precisely, the returned {@code Future} takes its result from a {@code Future} produced
   * by applying the given {@code AsyncFunction} to the result of the original {@code Future}.
   * Example usage:
   *
   * <pre>   {@code
   *   ListenableFuture<RowKey> rowKeyFuture = indexService.lookUp(query);
   *   AsyncFunction<RowKey, QueryResult> queryFunction =
   *       new AsyncFunction<RowKey, QueryResult>() {
   *         public ListenableFuture<QueryResult> apply(RowKey rowKey) {
   *           return dataService.read(rowKey);
   *         }
   *       };
   *   ListenableFuture<QueryResult> queryFuture =
   *       transformAsync(rowKeyFuture, queryFunction);}</pre>
   *
   * <p>This overload, which does not accept an executor, uses {@code directExecutor}, a dangerous
   * choice in some cases. See the discussion in the {@link ListenableFuture#addListener
   * ListenableFuture.addListener} documentation. The documentation's warnings about "lightweight
   * listeners" refer here to the work done during {@code AsyncFunction.apply}, not to any work done
   * to complete the returned {@code Future}.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in sync with that of the
   * input future and that of the future returned by the function. That is, if the returned {@code
   * Future} is cancelled, it will attempt to cancel the other two, and if either of the other two
   * is cancelled, the returned {@code Future} will receive a callback in which it will attempt to
   * cancel itself.
   *
   * @param input The future to transform
   * @param function A function to transform the result of the input future to the result of the
   *     output future
   * @return A future that holds result of the function (if the input succeeded) or the original
   *     input's failure (if not)
   * @since 19.0 (in 11.0 as {@code transform})
   */
  public static <I, O> ListenableFuture<O> transformAsync(
      ListenableFuture<I> input, AsyncFunction<? super I, ? extends O> function) {
    return AbstractTransformFuture.create(input, function);
  }

  /**
   * Returns a new {@code Future} whose result is asynchronously derived from the result of the
   * given {@code Future}. If the given {@code Future} fails, the returned {@code Future} fails with
   * the same exception (and the function is not invoked).
   *
   * <p>More precisely, the returned {@code Future} takes its result from a {@code Future} produced
   * by applying the given {@code AsyncFunction} to the result of the original {@code Future}.
   * Example usage:
   *
   * <pre>   {@code
   *   ListenableFuture<RowKey> rowKeyFuture = indexService.lookUp(query);
   *   AsyncFunction<RowKey, QueryResult> queryFunction =
   *       new AsyncFunction<RowKey, QueryResult>() {
   *         public ListenableFuture<QueryResult> apply(RowKey rowKey) {
   *           return dataService.read(rowKey);
   *         }
   *       };
   *   ListenableFuture<QueryResult> queryFuture =
   *       transformAsync(rowKeyFuture, queryFunction, executor);}</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. The documentation's warnings about "lightweight listeners" refer here to the
   * work done during {@code AsyncFunction.apply}, not to any work done to complete the returned
   * {@code Future}.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in sync with that of the
   * input future and that of the future returned by the chain function. That is, if the returned
   * {@code Future} is cancelled, it will attempt to cancel the other two, and if either of the
   * other two is cancelled, the returned {@code Future} will receive a callback in which it will
   * attempt to cancel itself.
   *
   * @param input The future to transform
   * @param function A function to transform the result of the input future to the result of the
   *     output future
   * @param executor Executor to run the function in.
   * @return A future that holds result of the function (if the input succeeded) or the original
   *     input's failure (if not)
   * @since 19.0 (in 11.0 as {@code transform})
   */
  public static <I, O> ListenableFuture<O> transformAsync(
      ListenableFuture<I> input,
      AsyncFunction<? super I, ? extends O> function,
      Executor executor) {
    return AbstractTransformFuture.create(input, function, executor);
  }

  /**
   * Returns a new {@code Future} whose result is derived from the result of the given {@code
   * Future}. If {@code input} fails, the returned {@code Future} fails with the same exception (and
   * the function is not invoked). Example usage:
   *
   * <pre>   {@code
   *   ListenableFuture<QueryResult> queryFuture = ...;
   *   Function<QueryResult, List<Row>> rowsFunction =
   *       new Function<QueryResult, List<Row>>() {
   *         public List<Row> apply(QueryResult queryResult) {
   *           return queryResult.getRows();
   *         }
   *       };
   *   ListenableFuture<List<Row>> rowsFuture =
   *       transform(queryFuture, rowsFunction);}</pre>
   *
   * <p>This overload, which does not accept an executor, uses {@code directExecutor}, a dangerous
   * choice in some cases. See the discussion in the {@link ListenableFuture#addListener
   * ListenableFuture.addListener} documentation. The documentation's warnings about "lightweight
   * listeners" refer here to the work done during {@code Function.apply}.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in sync with that of the
   * input future. That is, if the returned {@code Future} is cancelled, it will attempt to cancel
   * the input, and if the input is cancelled, the returned {@code Future} will receive a callback
   * in which it will attempt to cancel itself.
   *
   * <p>An example use of this method is to convert a serializable object returned from an RPC into
   * a POJO.
   *
   * @param input The future to transform
   * @param function A Function to transform the results of the provided future to the results of
   *     the returned future.  This will be run in the thread that notifies input it is complete.
   * @return A future that holds result of the transformation.
   * @since 9.0 (in 1.0 as {@code compose})
   */
  public static <I, O> ListenableFuture<O> transform(
      ListenableFuture<I> input, Function<? super I, ? extends O> function) {
    return AbstractTransformFuture.create(input, function);
  }

  /**
   * Returns a new {@code Future} whose result is derived from the result of the given {@code
   * Future}. If {@code input} fails, the returned {@code Future} fails with the same exception (and
   * the function is not invoked). Example usage:
   *
   * <pre>   {@code
   *   ListenableFuture<QueryResult> queryFuture = ...;
   *   Function<QueryResult, List<Row>> rowsFunction =
   *       new Function<QueryResult, List<Row>>() {
   *         public List<Row> apply(QueryResult queryResult) {
   *           return queryResult.getRows();
   *         }
   *       };
   *   ListenableFuture<List<Row>> rowsFuture =
   *       transform(queryFuture, rowsFunction, executor);}</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. The documentation's warnings about "lightweight listeners" refer here to the
   * work done during {@code Function.apply}.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in sync with that of the
   * input future. That is, if the returned {@code Future} is cancelled, it will attempt to cancel
   * the input, and if the input is cancelled, the returned {@code Future} will receive a callback
   * in which it will attempt to cancel itself.
   *
   * <p>An example use of this method is to convert a serializable object returned from an RPC into
   * a POJO.
   *
   * @param input The future to transform
   * @param function A Function to transform the results of the provided future to the results of
   *     the returned future.
   * @param executor Executor to run the function in.
   * @return A future that holds result of the transformation.
   * @since 9.0 (in 2.0 as {@code compose})
   */
  public static <I, O> ListenableFuture<O> transform(
      ListenableFuture<I> input, Function<? super I, ? extends O> function, Executor executor) {
    return AbstractTransformFuture.create(input, function, executor);
  }

  /**
   * Like {@link #transform(ListenableFuture, Function)} except that the transformation {@code
   * function} is invoked on each call to {@link Future#get() get()} on the returned future.
   *
   * <p>The returned {@code Future} reflects the input's cancellation state directly, and any
   * attempt to cancel the returned Future is likewise passed through to the input Future.
   *
   * <p>Note that calls to {@linkplain Future#get(long, TimeUnit) timed get} only apply the timeout
   * to the execution of the underlying {@code Future}, <em>not</em> to the execution of the
   * transformation function.
   *
   * <p>The primary audience of this method is callers of {@code transform} who don't have a {@code
   * ListenableFuture} available and do not mind repeated, lazy function evaluation.
   *
   * @param input The future to transform
   * @param function A Function to transform the results of the provided future to the results of
   *     the returned future.
   * @return A future that returns the result of the transformation.
   * @since 10.0
   */
  @GwtIncompatible // TODO
  public static <I, O> Future<O> lazyTransform(
      final Future<I> input, final Function<? super I, ? extends O> function) {
    checkNotNull(input);
    checkNotNull(function);
    return new Future<O>() {

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return input.cancel(mayInterruptIfRunning);
      }

      @Override
      public boolean isCancelled() {
        return input.isCancelled();
      }

      @Override
      public boolean isDone() {
        return input.isDone();
      }

      @Override
      public O get() throws InterruptedException, ExecutionException {
        return applyTransformation(input.get());
      }

      @Override
      public O get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
        return applyTransformation(input.get(timeout, unit));
      }

      private O applyTransformation(I input) throws ExecutionException {
        try {
          return function.apply(input);
        } catch (Throwable t) {
          throw new ExecutionException(t);
        }
      }
    };
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is the product of calling {@code get()} on
   * the {@code Future} nested within the given {@code Future}, effectively chaining the futures one
   * after the other.  Example:
   *
   * <pre>   {@code
   *   SettableFuture<ListenableFuture<String>> nested = SettableFuture.create();
   *   ListenableFuture<String> dereferenced = dereference(nested);}</pre>
   *
   * <p>Most users will not need this method. To create a {@code Future} that completes with the
   * result of another {@code Future}, create a {@link SettableFuture}, and call {@link
   * SettableFuture#setFuture setFuture(otherFuture)} on it.
   *
   * <p>{@code dereference} has the same cancellation and execution semantics as {@link
   * #transformAsync(ListenableFuture, AsyncFunction)}, in that the returned {@code Future}
   * attempts to keep its cancellation state in sync with both the input {@code Future} and the
   * nested {@code Future}.  The transformation is very lightweight and therefore takes place in
   * the same thread (either the thread that called {@code dereference}, or the thread in which
   * the dereferenced future completes).
   *
   * @param nested The nested future to transform.
   * @return A future that holds result of the inner future.
   * @since 13.0
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static <V> ListenableFuture<V> dereference(
      ListenableFuture<? extends ListenableFuture<? extends V>> nested) {
    return transformAsync((ListenableFuture) nested, (AsyncFunction) DEREFERENCER);
  }

  /**
   * Helper {@code Function} for {@link #dereference}.
   */
  private static final AsyncFunction<ListenableFuture<Object>, Object> DEREFERENCER =
      new AsyncFunction<ListenableFuture<Object>, Object>() {
        @Override
        public ListenableFuture<Object> apply(ListenableFuture<Object> input) {
          return input;
        }
      };

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the values of all its
   * input futures, if all succeed. If any input fails, the returned future fails immediately.
   *
   * <p>The list of results is in the same order as the input list.
   *
   * <p>Canceling this future will attempt to cancel all the component futures, and if any of the
   * provided futures fails or is canceled, this one is, too.
   *
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component futures
   * @since 10.0
   */
  @Beta
  @SafeVarargs
  public static <V> ListenableFuture<List<V>> allAsList(ListenableFuture<? extends V>... futures) {
    return new ListFuture<V>(ImmutableList.copyOf(futures), true);
  }

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the values of all its
   * input futures, if all succeed. If any input fails, the returned future fails immediately.
   *
   * <p>The list of results is in the same order as the input list.
   *
   * <p>Canceling this future will attempt to cancel all the component futures, and if any of the
   * provided futures fails or is canceled, this one is, too.
   *
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component futures
   * @since 10.0
   */
  @Beta
  public static <V> ListenableFuture<List<V>> allAsList(
      Iterable<? extends ListenableFuture<? extends V>> futures) {
    return new ListFuture<V>(ImmutableList.copyOf(futures), true);
  }

  /**
   * Creates a {@link FutureCombiner} that processes the completed futures whether or not they're
   * successful.
   *
   * @since 20.0
   */
  @SafeVarargs
  public static <V> FutureCombiner<V> whenAllComplete(ListenableFuture<? extends V>... futures) {
    return new FutureCombiner<V>(false, ImmutableList.copyOf(futures));
  }

  /**
   * Creates a {@link FutureCombiner} that processes the completed futures whether or not they're
   * successful.
   *
   * @since 20.0
   */
  public static <V> FutureCombiner<V> whenAllComplete(
      Iterable<? extends ListenableFuture<? extends V>> futures) {
    return new FutureCombiner<V>(false, ImmutableList.copyOf(futures));
  }

  /**
   * Creates a {@link FutureCombiner} requiring that all passed in futures are successful.
   *
   * <p>If any input fails, the returned future fails immediately.
   *
   * @since 20.0
   */
  @SafeVarargs
  public static <V> FutureCombiner<V> whenAllSucceed(ListenableFuture<? extends V>... futures) {
    return new FutureCombiner<V>(true, ImmutableList.copyOf(futures));
  }

  /**
   * Creates a {@link FutureCombiner} requiring that all passed in futures are successful.
   *
   * <p>If any input fails, the returned future fails immediately.
   *
   * @since 20.0
   */
  public static <V> FutureCombiner<V> whenAllSucceed(
      Iterable<? extends ListenableFuture<? extends V>> futures) {
    return new FutureCombiner<V>(true, ImmutableList.copyOf(futures));
  }

  /**
   * A helper to create a new {@code ListenableFuture} whose result is generated from a combination
   * of input futures.
   *
   * <p>See {@link #whenAllComplete} and {@link #whenAllSucceed} for how to instantiate this class.
   *
   * <p>Example:
   *
   * <pre>   {@code
   *   final ListenableFuture<Instant> loginDateFuture =
   *       loginService.findLastLoginDate(username);
   *   final ListenableFuture<List<String>> recentCommandsFuture =
   *       recentCommandsService.findRecentCommands(username);
   *   Callable<UsageHistory> usageComputation =
   *       new Callable<UsageHistory>() {
   *         public UsageHistory call() throws Exception {
   *           return new UsageHistory(
   *               username, loginDateFuture.get(), recentCommandsFuture.get());
   *         }
   *       };
   *   ListenableFuture<UsageHistory> usageFuture =
   *       Futures.whenAllSucceed(loginDateFuture, recentCommandsFuture)
   *           .call(usageComputation, executor);}</pre>
   *
   * @since 20.0
   */
  @Beta
  @CanIgnoreReturnValue // TODO(cpovirk): Consider removing, especially if we provide run(Runnable)
  @GwtCompatible
  public static final class FutureCombiner<V> {
    private final boolean allMustSucceed;
    private final ImmutableList<ListenableFuture<? extends V>> futures;

    private FutureCombiner(
        boolean allMustSucceed, ImmutableList<ListenableFuture<? extends V>> futures) {
      this.allMustSucceed = allMustSucceed;
      this.futures = futures;
    }

    /**
     * Creates the {@link ListenableFuture} which will return the result of calling {@link
     * AsyncCallable#call} in {@code combiner} when all futures complete, using the specified {@code
     * executor}.
     *
     * <p>If the combiner throws a {@code CancellationException}, the returned future will be
     * cancelled.
     *
     * <p>If the combiner throws an {@code ExecutionException}, the cause of the thrown {@code
     * ExecutionException} will be extracted and returned as the cause of the new {@code
     * ExecutionException} that gets thrown by the returned combined future.
     *
     * <p>Canceling this future will attempt to cancel all the component futures.
     */
    public <C> ListenableFuture<C> callAsync(AsyncCallable<C> combiner, Executor executor) {
      return new CombinedFuture<C>(futures, allMustSucceed, executor, combiner);
    }

    /**
     * Like {@link #callAsync(AsyncCallable, Executor)} but using {@linkplain
     * MoreExecutors#directExecutor direct executor}.
     */
    public <C> ListenableFuture<C> callAsync(AsyncCallable<C> combiner) {
      return callAsync(combiner, directExecutor());
    }

    /**
     * Creates the {@link ListenableFuture} which will return the result of calling {@link
     * Callable#call} in {@code combiner} when all futures complete, using the specified {@code
     * executor}.
     *
     * <p>If the combiner throws a {@code CancellationException}, the returned future will be
     * cancelled.
     *
     * <p>If the combiner throws an {@code ExecutionException}, the cause of the thrown {@code
     * ExecutionException} will be extracted and returned as the cause of the new {@code
     * ExecutionException} that gets thrown by the returned combined future.
     *
     * <p>Canceling this future will attempt to cancel all the component futures.
     */
    @CanIgnoreReturnValue
    public <C> ListenableFuture<C> call(Callable<C> combiner, Executor executor) {
      return new CombinedFuture<C>(futures, allMustSucceed, executor, combiner);
    }

    /**
     * Like {@link #call(Callable, Executor)} but using {@linkplain MoreExecutors#directExecutor
     * direct executor}.
     */
    @CanIgnoreReturnValue
    public <C> ListenableFuture<C> call(Callable<C> combiner) {
      return call(combiner, directExecutor());
    }

    /*
     * TODO(cpovirk): Evaluate demand for a run(Runnable) version. Would it allow us to remove
     * @CanIgnoreReturnValue from the call() methods above?
     * https://github.com/google/guava/issues/2371
     */
  }

  /**
   * Creates a new {@code ListenableFuture} whose result is set from the supplied future when it
   * completes. Cancelling the supplied future will also cancel the returned future, but cancelling
   * the returned future will have no effect on the supplied future.
   *
   * @since 15.0
   */
  @GwtIncompatible // TODO
  public static <V> ListenableFuture<V> nonCancellationPropagating(ListenableFuture<V> future) {
    return new NonCancellationPropagatingFuture<V>(future);
  }

  /**
   * A wrapped future that does not propagate cancellation to its delegate.
   */
  @GwtIncompatible // TODO
  private static final class NonCancellationPropagatingFuture<V>
      extends AbstractFuture.TrustedFuture<V> {
    NonCancellationPropagatingFuture(final ListenableFuture<V> delegate) {
      delegate.addListener(
          new Runnable() {
            @Override
            public void run() {
              // This prevents cancellation from propagating because we don't assign delegate until
              // delegate is already done, so calling cancel() on it is a no-op.
              setFuture(delegate);
            }
          },
          directExecutor());
    }
  }

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the values of all its
   * successful input futures. The list of results is in the same order as the input list, and if
   * any of the provided futures fails or is canceled, its corresponding position will contain
   * {@code null} (which is indistinguishable from the future having a successful value of {@code
   * null}).
   *
   * <p>Canceling this future will attempt to cancel all the component futures.
   *
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component futures
   * @since 10.0
   */
  @Beta
  @SafeVarargs
  public static <V> ListenableFuture<List<V>> successfulAsList(
      ListenableFuture<? extends V>... futures) {
    return new ListFuture<V>(ImmutableList.copyOf(futures), false);
  }

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the values of all its
   * successful input futures. The list of results is in the same order as the input list, and if
   * any of the provided futures fails or is canceled, its corresponding position will contain
   * {@code null} (which is indistinguishable from the future having a successful value of {@code
   * null}).
   *
   * <p>Canceling this future will attempt to cancel all the component futures.
   *
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component futures
   * @since 10.0
   */
  @Beta
  public static <V> ListenableFuture<List<V>> successfulAsList(
      Iterable<? extends ListenableFuture<? extends V>> futures) {
    return new ListFuture<V>(ImmutableList.copyOf(futures), false);
  }

  /**
   * Returns a list of delegate futures that correspond to the futures received in the order that
   * they complete. Delegate futures return the same value or throw the same exception as the
   * corresponding input future returns/throws.
   *
   * <p>Cancelling a delegate future has no effect on any input future, since the delegate future
   * does not correspond to a specific input future until the appropriate number of input futures
   * have completed. At that point, it is too late to cancel the input future. The input future's
   * result, which cannot be stored into the cancelled delegate future, is ignored.
   *
   * @since 17.0
   */
  @Beta
  @GwtIncompatible // TODO
  public static <T> ImmutableList<ListenableFuture<T>> inCompletionOrder(
      Iterable<? extends ListenableFuture<? extends T>> futures) {
    // A CLQ may be overkill here. We could save some pointers/memory by synchronizing on an
    // ArrayDeque
    final ConcurrentLinkedQueue<SettableFuture<T>> delegates = Queues.newConcurrentLinkedQueue();
    ImmutableList.Builder<ListenableFuture<T>> listBuilder = ImmutableList.builder();
    // Using SerializingExecutor here will ensure that each CompletionOrderListener executes
    // atomically and therefore that each returned future is guaranteed to be in completion order.
    // N.B. there are some cases where the use of this executor could have possibly surprising
    // effects when input futures finish at approximately the same time _and_ the output futures
    // have directExecutor listeners. In this situation, the listeners may end up running on a
    // different thread than if they were attached to the corresponding input future. We believe
    // this to be a negligible cost since:
    // 1. Using the directExecutor implies that your callback is safe to run on any thread.
    // 2. This would likely only be noticeable if you were doing something expensive or blocking on
    //    a directExecutor listener on one of the output futures which is an antipattern anyway.
    SerializingExecutor executor = new SerializingExecutor(directExecutor());
    for (final ListenableFuture<? extends T> future : futures) {
      SettableFuture<T> delegate = SettableFuture.create();
      // Must make sure to add the delegate to the queue first in case the future is already done
      delegates.add(delegate);
      future.addListener(
          new Runnable() {
            @Override
            public void run() {
              delegates.remove().setFuture(future);
            }
          },
          executor);
      listBuilder.add(delegate);
    }
    return listBuilder.build();
  }

  /**
   * Registers separate success and failure callbacks to be run when the {@code Future}'s
   * computation is {@linkplain java.util.concurrent.Future#isDone() complete} or, if the
   * computation is already complete, immediately.
   *
   * <p>There is no guaranteed ordering of execution of callbacks, but any callback added through
   * this method is guaranteed to be called once the computation is complete.
   *
   * Example: <pre> {@code
   * ListenableFuture<QueryResult> future = ...;
   * addCallback(future,
   *     new FutureCallback<QueryResult>() {
   *       public void onSuccess(QueryResult result) {
   *         storeInCache(result);
   *       }
   *       public void onFailure(Throwable t) {
   *         reportError(t);
   *       }
   *     });}</pre>
   *
   * <p>This overload, which does not accept an executor, uses {@code directExecutor}, a dangerous
   * choice in some cases. See the discussion in the {@link ListenableFuture#addListener
   * ListenableFuture.addListener} documentation.
   *
   * <p>For a more general interface to attach a completion listener to a {@code Future}, see {@link
   * ListenableFuture#addListener addListener}.
   *
   * @param future The future attach the callback to.
   * @param callback The callback to invoke when {@code future} is completed.
   * @since 10.0
   */
  public static <V> void addCallback(
      ListenableFuture<V> future, FutureCallback<? super V> callback) {
    addCallback(future, callback, directExecutor());
  }

  /**
   * Registers separate success and failure callbacks to be run when the {@code Future}'s
   * computation is {@linkplain java.util.concurrent.Future#isDone() complete} or, if the
   * computation is already complete, immediately.
   *
   * <p>The callback is run in {@code executor}. There is no guaranteed ordering of execution of
   * callbacks, but any callback added through this method is guaranteed to be called once the
   * computation is complete.
   *
   * Example: <pre> {@code
   * ListenableFuture<QueryResult> future = ...;
   * Executor e = ...
   * addCallback(future,
   *     new FutureCallback<QueryResult>() {
   *       public void onSuccess(QueryResult result) {
   *         storeInCache(result);
   *       }
   *       public void onFailure(Throwable t) {
   *         reportError(t);
   *       }
   *     }, e);}</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation.
   *
   * <p>For a more general interface to attach a completion listener to a {@code Future}, see {@link
   * ListenableFuture#addListener addListener}.
   *
   * @param future The future attach the callback to.
   * @param callback The callback to invoke when {@code future} is completed.
   * @param executor The executor to run {@code callback} when the future completes.
   * @since 10.0
   */
  public static <V> void addCallback(
      final ListenableFuture<V> future,
      final FutureCallback<? super V> callback,
      Executor executor) {
    Preconditions.checkNotNull(callback);
    Runnable callbackListener =
        new Runnable() {
          @Override
          public void run() {
            final V value;
            try {
              value = getDone(future);
            } catch (ExecutionException e) {
              callback.onFailure(e.getCause());
              return;
            } catch (RuntimeException e) {
              callback.onFailure(e);
              return;
            } catch (Error e) {
              callback.onFailure(e);
              return;
            }
            callback.onSuccess(value);
          }
        };
    future.addListener(callbackListener, executor);
  }

  /**
   * Returns the result of the input {@code Future}, which must have already completed.
   *
   * <p>The benefits of this method are twofold. First, the name "getDone" suggests to readers that
   * the {@code Future} is already done. Second, if buggy code calls {@code getDone} on a {@code
   * Future} that is still pending, the program will throw instead of block. This can be important
   * for APIs like {@link whenAllComplete whenAllComplete(...)}{@code .}{@link
   * FutureCombiner#call(Callable) call(...)}, where it is easy to use a new input from the {@code
   * call} implementation but forget to add it to the arguments of {@code whenAllComplete}.
   *
   * <p>If you are looking for a method to determine whether a given {@code Future} is done, use the
   * instance method {@link Future#isDone()}.
   *
   * @throws ExecutionException if the {@code Future} failed with an exception
   * @throws CancellationException if the {@code Future} was cancelled
   * @throws IllegalStateException if the {@code Future} is not done
   * @since 20.0
   */
  @CanIgnoreReturnValue
  // TODO(cpovirk): Consider calling getDone() in our own code.
  public static <V> V getDone(Future<V> future) throws ExecutionException {
    /*
     * We throw IllegalStateException, since the call could succeed later. Perhaps we "should" throw
     * IllegalArgumentException, since the call could succeed with a different argument. Those
     * exceptions' docs suggest that either is acceptable. Google's Java Practices page recommends
     * IllegalArgumentException here, in part to keep its recommendation simple: Static methods
     * should throw IllegalStateException only when they use static state.
     *
     *
     * Why do we deviate here? The answer: We want for fluentFuture.getDone() to throw the same
     * exception as Futures.getDone(fluentFuture).
     */
    checkState(future.isDone(), "Future was expected to be done: %s", future);
    return getUninterruptibly(future);
  }

  /**
   * Returns the result of {@link Future#get()}, converting most exceptions to a new instance of the
   * given checked exception type. This reduces boilerplate for a common use of {@code Future} in
   * which it is unnecessary to programmatically distinguish between exception types or to extract
   * other information from the exception instance.
   *
   * <p>Exceptions from {@code Future.get} are treated as follows:
   * <ul>
   * <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an {@code X} if the cause is
   *     a checked exception, an {@link UncheckedExecutionException} if the cause is a {@code
   *     RuntimeException}, or an {@link ExecutionError} if the cause is an {@code Error}.
   * <li>Any {@link InterruptedException} is wrapped in an {@code X} (after restoring the
   *     interrupt).
   * <li>Any {@link CancellationException} is propagated untouched, as is any other {@link
   *     RuntimeException} (though {@code get} implementations are discouraged from throwing such
   *     exceptions).
   * </ul>
   *
   * <p>The overall principle is to continue to treat every checked exception as a checked
   * exception, every unchecked exception as an unchecked exception, and every error as an error. In
   * addition, the cause of any {@code ExecutionException} is wrapped in order to ensure that the
   * new stack trace matches that of the current thread.
   *
   * <p>Instances of {@code exceptionClass} are created by choosing an arbitrary public constructor
   * that accepts zero or more arguments, all of type {@code String} or {@code Throwable}
   * (preferring constructors with at least one {@code String}) and calling the constructor via
   * reflection. If the exception did not already have a cause, one is set by calling {@link
   * Throwable#initCause(Throwable)} on it. If no such constructor exists, an {@code
   * IllegalArgumentException} is thrown.
   *
   * @throws X if {@code get} throws any checked exception except for an {@code ExecutionException}
   *     whose cause is not itself a checked exception
   * @throws UncheckedExecutionException if {@code get} throws an {@code ExecutionException} with a
   *     {@code RuntimeException} as its cause
   * @throws ExecutionError if {@code get} throws an {@code ExecutionException} with an {@code
   *     Error} as its cause
   * @throws CancellationException if {@code get} throws a {@code CancellationException}
   * @throws IllegalArgumentException if {@code exceptionClass} extends {@code RuntimeException} or
   *     does not have a suitable constructor
   * @since 19.0 (in 10.0 as {@code get})
   */
  @CanIgnoreReturnValue
  @GwtIncompatible // reflection
  public static <V, X extends Exception> V getChecked(Future<V> future, Class<X> exceptionClass)
      throws X {
    return FuturesGetChecked.getChecked(future, exceptionClass);
  }

  /**
   * Returns the result of {@link Future#get(long, TimeUnit)}, converting most exceptions to a new
   * instance of the given checked exception type. This reduces boilerplate for a common use of
   * {@code Future} in which it is unnecessary to programmatically distinguish between exception
   * types or to extract other information from the exception instance.
   *
   * <p>Exceptions from {@code Future.get} are treated as follows:
   * <ul>
   * <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an {@code X} if the cause is
   *     a checked exception, an {@link UncheckedExecutionException} if the cause is a {@code
   *     RuntimeException}, or an {@link ExecutionError} if the cause is an {@code Error}.
   * <li>Any {@link InterruptedException} is wrapped in an {@code X} (after restoring the
   *     interrupt).
   * <li>Any {@link TimeoutException} is wrapped in an {@code X}.
   * <li>Any {@link CancellationException} is propagated untouched, as is any other {@link
   *     RuntimeException} (though {@code get} implementations are discouraged from throwing such
   *     exceptions).
   * </ul>
   *
   * <p>The overall principle is to continue to treat every checked exception as a checked
   * exception, every unchecked exception as an unchecked exception, and every error as an error. In
   * addition, the cause of any {@code ExecutionException} is wrapped in order to ensure that the
   * new stack trace matches that of the current thread.
   *
   * <p>Instances of {@code exceptionClass} are created by choosing an arbitrary public constructor
   * that accepts zero or more arguments, all of type {@code String} or {@code Throwable}
   * (preferring constructors with at least one {@code String}) and calling the constructor via
   * reflection. If the exception did not already have a cause, one is set by calling {@link
   * Throwable#initCause(Throwable)} on it. If no such constructor exists, an {@code
   * IllegalArgumentException} is thrown.
   *
   * @throws X if {@code get} throws any checked exception except for an {@code ExecutionException}
   *     whose cause is not itself a checked exception
   * @throws UncheckedExecutionException if {@code get} throws an {@code ExecutionException} with a
   *     {@code RuntimeException} as its cause
   * @throws ExecutionError if {@code get} throws an {@code ExecutionException} with an {@code
   *     Error} as its cause
   * @throws CancellationException if {@code get} throws a {@code CancellationException}
   * @throws IllegalArgumentException if {@code exceptionClass} extends {@code RuntimeException} or
   *     does not have a suitable constructor
   * @since 19.0 (in 10.0 as {@code get} and with different parameter order)
   */
  @CanIgnoreReturnValue
  @GwtIncompatible // reflection
  public static <V, X extends Exception> V getChecked(
      Future<V> future, Class<X> exceptionClass, long timeout, TimeUnit unit) throws X {
    return FuturesGetChecked.getChecked(future, exceptionClass, timeout, unit);
  }

  /**
   * Returns the result of calling {@link Future#get()} uninterruptibly on a task known not to throw
   * a checked exception. This makes {@code Future} more suitable for lightweight, fast-running
   * tasks that, barring bugs in the code, will not fail. This gives it exception-handling behavior
   * similar to that of {@code ForkJoinTask.join}.
   *
   * <p>Exceptions from {@code Future.get} are treated as follows:
   * <ul>
   * <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an {@link
   *     UncheckedExecutionException} (if the cause is an {@code Exception}) or {@link
   *     ExecutionError} (if the cause is an {@code Error}).
   * <li>Any {@link InterruptedException} causes a retry of the {@code get} call. The interrupt is
   *     restored before {@code getUnchecked} returns.
   * <li>Any {@link CancellationException} is propagated untouched. So is any other {@link
   *     RuntimeException} ({@code get} implementations are discouraged from throwing such
   *     exceptions).
   * </ul>
   *
   * <p>The overall principle is to eliminate all checked exceptions: to loop to avoid {@code
   * InterruptedException}, to pass through {@code CancellationException}, and to wrap any exception
   * from the underlying computation in an {@code UncheckedExecutionException} or {@code
   * ExecutionError}.
   *
   * <p>For an uninterruptible {@code get} that preserves other exceptions, see {@link
   * Uninterruptibles#getUninterruptibly(Future)}.
   *
   * @throws UncheckedExecutionException if {@code get} throws an {@code ExecutionException} with an
   *     {@code Exception} as its cause
   * @throws ExecutionError if {@code get} throws an {@code ExecutionException} with an {@code
   *     Error} as its cause
   * @throws CancellationException if {@code get} throws a {@code CancellationException}
   * @since 10.0
   */
  @CanIgnoreReturnValue
  @GwtIncompatible // TODO
  public static <V> V getUnchecked(Future<V> future) {
    checkNotNull(future);
    try {
      return getUninterruptibly(future);
    } catch (ExecutionException e) {
      wrapAndThrowUnchecked(e.getCause());
      throw new AssertionError();
    }
  }

  @GwtIncompatible // TODO
  private static void wrapAndThrowUnchecked(Throwable cause) {
    if (cause instanceof Error) {
      throw new ExecutionError((Error) cause);
    }
    /*
     * It's a non-Error, non-Exception Throwable. From my survey of such classes, I believe that
     * most users intended to extend Exception, so we'll treat it like an Exception.
     */
    throw new UncheckedExecutionException(cause);
  }

  /*
   * Arguably we don't need a timed getUnchecked because any operation slow enough to require a
   * timeout is heavyweight enough to throw a checked exception and therefore be inappropriate to
   * use with getUnchecked. Further, it's not clear that converting the checked TimeoutException to
   * a RuntimeException -- especially to an UncheckedExecutionException, since it wasn't thrown by
   * the computation -- makes sense, and if we don't convert it, the user still has to write a
   * try-catch block.
   *
   * If you think you would use this method, let us know. You might also also look into the
   * Fork-Join framework: http://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html
   */

  /**
   * A checked future that uses a function to map from exceptions to the appropriate checked type.
   */
  @GwtIncompatible // TODO
  private static class MappingCheckedFuture<V, X extends Exception>
      extends AbstractCheckedFuture<V, X> {

    final Function<? super Exception, X> mapper;

    MappingCheckedFuture(ListenableFuture<V> delegate, Function<? super Exception, X> mapper) {
      super(delegate);

      this.mapper = checkNotNull(mapper);
    }

    @Override
    protected X mapException(Exception e) {
      return mapper.apply(e);
    }
  }
}
