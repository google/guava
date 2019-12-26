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
import static com.google.common.util.concurrent.Internal.toNanosSaturated;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CollectionFuture.ListFuture;
import com.google.common.util.concurrent.ImmediateFuture.ImmediateCancelledFuture;
import com.google.common.util.concurrent.ImmediateFuture.ImmediateFailedFuture;
import com.google.common.util.concurrent.internal.InternalFutureFailureAccess;
import com.google.common.util.concurrent.internal.InternalFutures;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Static utility methods pertaining to the {@link Future} interface.
 *
 * <p>Many of these methods use the {@link ListenableFuture} API; consult the Guava User Guide
 * article on <a href="https://github.com/google/guava/wiki/ListenableFutureExplained">{@code
 * ListenableFuture}</a>.
 *
 * <p>The main purpose of {@code ListenableFuture} is to help you chain together a graph of
 * asynchronous operations. You can chain them together manually with calls to methods like {@link
 * Futures#transform(ListenableFuture, Function, Executor) Futures.transform}, but you will often
 * find it easier to use a framework. Frameworks automate the process, often adding features like
 * monitoring, debugging, and cancellation. Examples of frameworks include:
 *
 * <ul>
 *   <li><a href="http://dagger.dev/producers.html">Dagger Producers</a>
 * </ul>
 *
 * <p>If you do chain your operations manually, you may want to use {@link FluentFuture}.
 *
 * @author Kevin Bourrillion
 * @author Nishant Thakkar
 * @author Sven Mawson
 * @since 1.0
 */
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
   * Creates a {@code ListenableFuture} which has its value set immediately upon construction. The
   * getters just return the value. This {@code Future} can't be canceled or timed out and its
   * {@code isDone()} method always returns {@code true}.
   */
  public static <V> ListenableFuture<V> immediateFuture(@Nullable V value) {
    if (value == null) {
      // This cast is safe because null is assignable to V for all V (i.e. it is bivariant)
      @SuppressWarnings("unchecked")
      ListenableFuture<V> typedNull = (ListenableFuture<V>) ImmediateFuture.NULL;
      return typedNull;
    }
    return new ImmediateFuture<>(value);
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
   * Executes {@code callable} on the specified {@code executor}, returning a {@code Future}.
   *
   * @throws RejectedExecutionException if the task cannot be scheduled for execution
   * @since 28.2
   */
  @Beta
  public static <O> ListenableFuture<O> submit(Callable<O> callable, Executor executor) {
    TrustedListenableFutureTask<O> task = TrustedListenableFutureTask.create(callable);
    executor.execute(task);
    return task;
  }

  /**
   * Executes {@code runnable} on the specified {@code executor}, returning a {@code Future} that
   * will complete after execution.
   *
   * @throws RejectedExecutionException if the task cannot be scheduled for execution
   * @since 28.2
   */
  @Beta
  public static ListenableFuture<Void> submit(Runnable runnable, Executor executor) {
    TrustedListenableFutureTask<Void> task = TrustedListenableFutureTask.create(runnable, null);
    executor.execute(task);
    return task;
  }

  /**
   * Executes {@code callable} on the specified {@code executor}, returning a {@code Future}.
   *
   * @throws RejectedExecutionException if the task cannot be scheduled for execution
   * @since 23.0
   */
  @Beta
  public static <O> ListenableFuture<O> submitAsync(AsyncCallable<O> callable, Executor executor) {
    TrustedListenableFutureTask<O> task = TrustedListenableFutureTask.create(callable);
    executor.execute(task);
    return task;
  }

  /**
   * Schedules {@code callable} on the specified {@code executor}, returning a {@code Future}.
   *
   * @throws RejectedExecutionException if the task cannot be scheduled for execution
   * @since 28.0
   */
  @Beta
  @GwtIncompatible // java.util.concurrent.ScheduledExecutorService
  public static <O> ListenableFuture<O> scheduleAsync(
      AsyncCallable<O> callable, Duration delay, ScheduledExecutorService executorService) {
    return scheduleAsync(callable, toNanosSaturated(delay), TimeUnit.NANOSECONDS, executorService);
  }

  /**
   * Schedules {@code callable} on the specified {@code executor}, returning a {@code Future}.
   *
   * @throws RejectedExecutionException if the task cannot be scheduled for execution
   * @since 23.0
   */
  @Beta
  @GwtIncompatible // java.util.concurrent.ScheduledExecutorService
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static <O> ListenableFuture<O> scheduleAsync(
      AsyncCallable<O> callable,
      long delay,
      TimeUnit timeUnit,
      ScheduledExecutorService executorService) {
    TrustedListenableFutureTask<O> task = TrustedListenableFutureTask.create(callable);
    final Future<?> scheduled = executorService.schedule(task, delay, timeUnit);
    task.addListener(
        new Runnable() {
          @Override
          public void run() {
            // Don't want to interrupt twice
            scheduled.cancel(false);
          }
        },
        directExecutor());
    return task;
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
   * <pre>{@code
   * ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   * // Falling back to a zero counter in case an exception happens when
   * // processing the RPC to fetch counters.
   * ListenableFuture<Integer> faultTolerantFuture = Futures.catching(
   *     fetchCounterFuture, FetchException.class, x -> 0, directExecutor());
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. All its warnings about heavyweight listeners are also applicable to heavyweight
   * functions passed to this method.
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
  @Beta
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
   * <pre>{@code
   * ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   * // Falling back to a zero counter in case an exception happens when
   * // processing the RPC to fetch counters.
   * ListenableFuture<Integer> faultTolerantFuture = Futures.catchingAsync(
   *     fetchCounterFuture, FetchException.class, x -> immediateFuture(0), directExecutor());
   * }</pre>
   *
   * <p>The fallback can also choose to propagate the original exception when desired:
   *
   * <pre>{@code
   * ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   * // Falling back to a zero counter only in case the exception was a
   * // TimeoutException.
   * ListenableFuture<Integer> faultTolerantFuture = Futures.catchingAsync(
   *     fetchCounterFuture,
   *     FetchException.class,
   *     e -> {
   *       if (omitDataOnFetchFailure) {
   *         return immediateFuture(0);
   *       }
   *       throw e;
   *     },
   *     directExecutor());
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. All its warnings about heavyweight listeners are also applicable to heavyweight
   * functions passed to this method. (Specifically, {@code directExecutor} functions should avoid
   * heavyweight operations inside {@code AsyncFunction.apply}. Any heavyweight operations should
   * occur in other threads responsible for completing the returned {@code Future}.)
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
  @Beta
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
   * @param scheduledExecutor The executor service to enforce the timeout.
   * @since 28.0
   */
  @Beta
  @GwtIncompatible // java.util.concurrent.ScheduledExecutorService
  public static <V> ListenableFuture<V> withTimeout(
      ListenableFuture<V> delegate, Duration time, ScheduledExecutorService scheduledExecutor) {
    return withTimeout(delegate, toNanosSaturated(time), TimeUnit.NANOSECONDS, scheduledExecutor);
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
   * @since 19.0
   */
  @Beta
  @GwtIncompatible // java.util.concurrent.ScheduledExecutorService
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  public static <V> ListenableFuture<V> withTimeout(
      ListenableFuture<V> delegate,
      long time,
      TimeUnit unit,
      ScheduledExecutorService scheduledExecutor) {
    if (delegate.isDone()) {
      return delegate;
    }
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
   * <pre>{@code
   * ListenableFuture<RowKey> rowKeyFuture = indexService.lookUp(query);
   * ListenableFuture<QueryResult> queryFuture =
   *     transformAsync(rowKeyFuture, dataService::readFuture, executor);
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. All its warnings about heavyweight listeners are also applicable to heavyweight
   * functions passed to this method. (Specifically, {@code directExecutor} functions should avoid
   * heavyweight operations inside {@code AsyncFunction.apply}. Any heavyweight operations should
   * occur in other threads responsible for completing the returned {@code Future}.)
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
  @Beta
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
   * <pre>{@code
   * ListenableFuture<QueryResult> queryFuture = ...;
   * ListenableFuture<List<Row>> rowsFuture =
   *     transform(queryFuture, QueryResult::getRows, executor);
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. All its warnings about heavyweight listeners are also applicable to heavyweight
   * functions passed to this method.
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
  @Beta
  public static <I, O> ListenableFuture<O> transform(
      ListenableFuture<I> input, Function<? super I, ? extends O> function, Executor executor) {
    return AbstractTransformFuture.create(input, function, executor);
  }

  /**
   * Like {@link #transform(ListenableFuture, Function, Executor)} except that the transformation
   * {@code function} is invoked on each call to {@link Future#get() get()} on the returned future.
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
  @Beta
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
   * Creates a new {@code ListenableFuture} whose value is a list containing the values of all its
   * input futures, if all succeed.
   *
   * <p>The list of results is in the same order as the input list.
   *
   * <p>This differs from {@link #successfulAsList(ListenableFuture[])} in that it will return a
   * failed future if any of the items fails.
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
   * input futures, if all succeed.
   *
   * <p>The list of results is in the same order as the input list.
   *
   * <p>This differs from {@link #successfulAsList(Iterable)} in that it will return a failed future
   * if any of the items fails.
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
   * <p>Any failures from the input futures will not be propagated to the returned future.
   *
   * @since 20.0
   */
  @Beta
  @SafeVarargs
  public static <V> FutureCombiner<V> whenAllComplete(ListenableFuture<? extends V>... futures) {
    return new FutureCombiner<V>(false, ImmutableList.copyOf(futures));
  }

  /**
   * Creates a {@link FutureCombiner} that processes the completed futures whether or not they're
   * successful.
   *
   * <p>Any failures from the input futures will not be propagated to the returned future.
   *
   * @since 20.0
   */
  @Beta
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
  @Beta
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
  @Beta
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
   * <pre>{@code
   * final ListenableFuture<Instant> loginDateFuture =
   *     loginService.findLastLoginDate(username);
   * final ListenableFuture<List<String>> recentCommandsFuture =
   *     recentCommandsService.findRecentCommands(username);
   * ListenableFuture<UsageHistory> usageFuture =
   *     Futures.whenAllSucceed(loginDateFuture, recentCommandsFuture)
   *         .call(
   *             () ->
   *                 new UsageHistory(
   *                     username,
   *                     Futures.getDone(loginDateFuture),
   *                     Futures.getDone(recentCommandsFuture)),
   *             executor);
   * }</pre>
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
    @CanIgnoreReturnValue // TODO(cpovirk): Remove this
    public <C> ListenableFuture<C> call(Callable<C> combiner, Executor executor) {
      return new CombinedFuture<C>(futures, allMustSucceed, executor, combiner);
    }

    /**
     * Creates the {@link ListenableFuture} which will return the result of running {@code combiner}
     * when all Futures complete. {@code combiner} will run using {@code executor}.
     *
     * <p>If the combiner throws a {@code CancellationException}, the returned future will be
     * cancelled.
     *
     * <p>Canceling this Future will attempt to cancel all the component futures.
     *
     * @since 23.6
     */
    public ListenableFuture<?> run(final Runnable combiner, Executor executor) {
      return call(
          new Callable<Void>() {
            @Override
            public Void call() throws Exception {
              combiner.run();
              return null;
            }
          },
          executor);
    }
  }

  /**
   * Returns a {@code ListenableFuture} whose result is set from the supplied future when it
   * completes. Cancelling the supplied future will also cancel the returned future, but cancelling
   * the returned future will have no effect on the supplied future.
   *
   * @since 15.0
   */
  @Beta
  public static <V> ListenableFuture<V> nonCancellationPropagating(ListenableFuture<V> future) {
    if (future.isDone()) {
      return future;
    }
    NonCancellationPropagatingFuture<V> output = new NonCancellationPropagatingFuture<>(future);
    future.addListener(output, directExecutor());
    return output;
  }

  /** A wrapped future that does not propagate cancellation to its delegate. */
  private static final class NonCancellationPropagatingFuture<V>
      extends AbstractFuture.TrustedFuture<V> implements Runnable {
    private ListenableFuture<V> delegate;

    NonCancellationPropagatingFuture(final ListenableFuture<V> delegate) {
      this.delegate = delegate;
    }

    @Override
    public void run() {
      // This prevents cancellation from propagating because we don't call setFuture(delegate) until
      // delegate is already done, so calling cancel() on this future won't affect it.
      ListenableFuture<V> localDelegate = delegate;
      if (localDelegate != null) {
        setFuture(localDelegate);
      }
    }

    @Override
    protected String pendingToString() {
      ListenableFuture<V> localDelegate = delegate;
      if (localDelegate != null) {
        return "delegate=[" + localDelegate + "]";
      }
      return null;
    }

    @Override
    protected void afterDone() {
      delegate = null;
    }
  }

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the values of all its
   * successful input futures. The list of results is in the same order as the input list, and if
   * any of the provided futures fails or is canceled, its corresponding position will contain
   * {@code null} (which is indistinguishable from the future having a successful value of {@code
   * null}).
   *
   * <p>The list of results is in the same order as the input list.
   *
   * <p>This differs from {@link #allAsList(ListenableFuture[])} in that it's tolerant of failed
   * futures for any of the items, representing them as {@code null} in the result list.
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
   * <p>The list of results is in the same order as the input list.
   *
   * <p>This differs from {@link #allAsList(Iterable)} in that it's tolerant of failed futures for
   * any of the items, representing them as {@code null} in the result list.
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
   * <p>"In the order that they complete" means, for practical purposes, about what you would
   * expect, but there are some subtleties. First, we do guarantee that, if the output future at
   * index n is done, the output future at index n-1 is also done. (But as usual with futures, some
   * listeners for future n may complete before some for future n-1.) However, it is possible, if
   * one input completes with result X and another later with result Y, for Y to come before X in
   * the output future list. (Such races are impossible to solve without global synchronization of
   * all future completions. And they should have little practical impact.)
   *
   * <p>Cancelling a delegate future propagates to input futures once all the delegates complete,
   * either from cancellation or because an input future has completed. If N futures are passed in,
   * and M delegates are cancelled, the remaining M input futures will be cancelled once N - M of
   * the input futures complete. If all the delegates are cancelled, all the input futures will be
   * too.
   *
   * @since 17.0
   */
  @Beta
  public static <T> ImmutableList<ListenableFuture<T>> inCompletionOrder(
      Iterable<? extends ListenableFuture<? extends T>> futures) {
    // Can't use Iterables.toArray because it's not gwt compatible
    final Collection<ListenableFuture<? extends T>> collection;
    if (futures instanceof Collection) {
      collection = (Collection<ListenableFuture<? extends T>>) futures;
    } else {
      collection = ImmutableList.copyOf(futures);
    }
    @SuppressWarnings("unchecked")
    ListenableFuture<? extends T>[] copy =
        (ListenableFuture<? extends T>[])
            collection.toArray(new ListenableFuture[collection.size()]);
    final InCompletionOrderState<T> state = new InCompletionOrderState<>(copy);
    ImmutableList.Builder<AbstractFuture<T>> delegatesBuilder = ImmutableList.builder();
    for (int i = 0; i < copy.length; i++) {
      delegatesBuilder.add(new InCompletionOrderFuture<T>(state));
    }

    final ImmutableList<AbstractFuture<T>> delegates = delegatesBuilder.build();
    for (int i = 0; i < copy.length; i++) {
      final int localI = i;
      copy[i].addListener(
          new Runnable() {
            @Override
            public void run() {
              state.recordInputCompletion(delegates, localI);
            }
          },
          directExecutor());
    }

    @SuppressWarnings("unchecked")
    ImmutableList<ListenableFuture<T>> delegatesCast = (ImmutableList) delegates;
    return delegatesCast;
  }

  // This can't be a TrustedFuture, because TrustedFuture has clever optimizations that
  // mean cancel won't be called if this Future is passed into setFuture, and then
  // cancelled.
  private static final class InCompletionOrderFuture<T> extends AbstractFuture<T> {
    private InCompletionOrderState<T> state;

    private InCompletionOrderFuture(InCompletionOrderState<T> state) {
      this.state = state;
    }

    @Override
    public boolean cancel(boolean interruptIfRunning) {
      InCompletionOrderState<T> localState = state;
      if (super.cancel(interruptIfRunning)) {
        localState.recordOutputCancellation(interruptIfRunning);
        return true;
      }
      return false;
    }

    @Override
    protected void afterDone() {
      state = null;
    }

    @Override
    protected String pendingToString() {
      InCompletionOrderState<T> localState = state;
      if (localState != null) {
        // Don't print the actual array! We don't want inCompletionOrder(list).toString() to have
        // quadratic output.
        return "inputCount=["
            + localState.inputFutures.length
            + "], remaining=["
            + localState.incompleteOutputCount.get()
            + "]";
      }
      return null;
    }
  }

  private static final class InCompletionOrderState<T> {
    // A happens-before edge between the writes of these fields and their reads exists, because
    // in order to read these fields, the corresponding write to incompleteOutputCount must have
    // been read.
    private boolean wasCancelled = false;
    private boolean shouldInterrupt = true;
    private final AtomicInteger incompleteOutputCount;
    private final ListenableFuture<? extends T>[] inputFutures;
    private volatile int delegateIndex = 0;

    private InCompletionOrderState(ListenableFuture<? extends T>[] inputFutures) {
      this.inputFutures = inputFutures;
      incompleteOutputCount = new AtomicInteger(inputFutures.length);
    }

    private void recordOutputCancellation(boolean interruptIfRunning) {
      wasCancelled = true;
      // If all the futures were cancelled with interruption, cancel the input futures
      // with interruption; otherwise cancel without
      if (!interruptIfRunning) {
        shouldInterrupt = false;
      }
      recordCompletion();
    }

    private void recordInputCompletion(
        ImmutableList<AbstractFuture<T>> delegates, int inputFutureIndex) {
      ListenableFuture<? extends T> inputFuture = inputFutures[inputFutureIndex];
      // Null out our reference to this future, so it can be GCed
      inputFutures[inputFutureIndex] = null;
      for (int i = delegateIndex; i < delegates.size(); i++) {
        if (delegates.get(i).setFuture(inputFuture)) {
          recordCompletion();
          // this is technically unnecessary, but should speed up later accesses
          delegateIndex = i + 1;
          return;
        }
      }
      // If all the delegates were complete, no reason for the next listener to have to
      // go through the whole list. Avoids O(n^2) behavior when the entire output list is
      // cancelled.
      delegateIndex = delegates.size();
    }

    private void recordCompletion() {
      if (incompleteOutputCount.decrementAndGet() == 0 && wasCancelled) {
        for (ListenableFuture<?> toCancel : inputFutures) {
          if (toCancel != null) {
            toCancel.cancel(shouldInterrupt);
          }
        }
      }
    }
  }

  /**
   * Registers separate success and failure callbacks to be run when the {@code Future}'s
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
   *     }, e);
   * }</pre>
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation. All its warnings about heavyweight listeners are also applicable to heavyweight
   * callbacks passed to this method.
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
    future.addListener(new CallbackListener<V>(future, callback), executor);
  }

  /** See {@link #addCallback(ListenableFuture, FutureCallback, Executor)} for behavioral notes. */
  private static final class CallbackListener<V> implements Runnable {
    final Future<V> future;
    final FutureCallback<? super V> callback;

    CallbackListener(Future<V> future, FutureCallback<? super V> callback) {
      this.future = future;
      this.callback = callback;
    }

    @Override
    public void run() {
      if (future instanceof InternalFutureFailureAccess) {
        Throwable failure =
            InternalFutures.tryInternalFastPathGetFailure((InternalFutureFailureAccess) future);
        if (failure != null) {
          callback.onFailure(failure);
          return;
        }
      }
      final V value;
      try {
        value = getDone(future);
      } catch (ExecutionException e) {
        callback.onFailure(e.getCause());
        return;
      } catch (RuntimeException | Error e) {
        callback.onFailure(e);
        return;
      }
      callback.onSuccess(value);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).addValue(callback).toString();
    }
  }

  /**
   * Returns the result of the input {@code Future}, which must have already completed.
   *
   * <p>The benefits of this method are twofold. First, the name "getDone" suggests to readers that
   * the {@code Future} is already done. Second, if buggy code calls {@code getDone} on a {@code
   * Future} that is still pending, the program will throw instead of block. This can be important
   * for APIs like {@link #whenAllComplete whenAllComplete(...)}{@code .}{@link
   * FutureCombiner#call(Callable, Executor) call(...)}, where it is easy to use a new input from
   * the {@code call} implementation but forget to add it to the arguments of {@code
   * whenAllComplete}.
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
   *
   * <ul>
   *   <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an {@code X} if the cause
   *       is a checked exception, an {@link UncheckedExecutionException} if the cause is a {@code
   *       RuntimeException}, or an {@link ExecutionError} if the cause is an {@code Error}.
   *   <li>Any {@link InterruptedException} is wrapped in an {@code X} (after restoring the
   *       interrupt).
   *   <li>Any {@link CancellationException} is propagated untouched, as is any other {@link
   *       RuntimeException} (though {@code get} implementations are discouraged from throwing such
   *       exceptions).
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
  @Beta
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
   *
   * <ul>
   *   <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an {@code X} if the cause
   *       is a checked exception, an {@link UncheckedExecutionException} if the cause is a {@code
   *       RuntimeException}, or an {@link ExecutionError} if the cause is an {@code Error}.
   *   <li>Any {@link InterruptedException} is wrapped in an {@code X} (after restoring the
   *       interrupt).
   *   <li>Any {@link TimeoutException} is wrapped in an {@code X}.
   *   <li>Any {@link CancellationException} is propagated untouched, as is any other {@link
   *       RuntimeException} (though {@code get} implementations are discouraged from throwing such
   *       exceptions).
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
   * @since 28.0
   */
  @Beta
  @CanIgnoreReturnValue
  @GwtIncompatible // reflection
  public static <V, X extends Exception> V getChecked(
      Future<V> future, Class<X> exceptionClass, Duration timeout) throws X {
    return getChecked(future, exceptionClass, toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
  }

  /**
   * Returns the result of {@link Future#get(long, TimeUnit)}, converting most exceptions to a new
   * instance of the given checked exception type. This reduces boilerplate for a common use of
   * {@code Future} in which it is unnecessary to programmatically distinguish between exception
   * types or to extract other information from the exception instance.
   *
   * <p>Exceptions from {@code Future.get} are treated as follows:
   *
   * <ul>
   *   <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an {@code X} if the cause
   *       is a checked exception, an {@link UncheckedExecutionException} if the cause is a {@code
   *       RuntimeException}, or an {@link ExecutionError} if the cause is an {@code Error}.
   *   <li>Any {@link InterruptedException} is wrapped in an {@code X} (after restoring the
   *       interrupt).
   *   <li>Any {@link TimeoutException} is wrapped in an {@code X}.
   *   <li>Any {@link CancellationException} is propagated untouched, as is any other {@link
   *       RuntimeException} (though {@code get} implementations are discouraged from throwing such
   *       exceptions).
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
  @Beta
  @CanIgnoreReturnValue
  @GwtIncompatible // reflection
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
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
   *
   * <ul>
   *   <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an {@link
   *       UncheckedExecutionException} (if the cause is an {@code Exception}) or {@link
   *       ExecutionError} (if the cause is an {@code Error}).
   *   <li>Any {@link InterruptedException} causes a retry of the {@code get} call. The interrupt is
   *       restored before {@code getUnchecked} returns.
   *   <li>Any {@link CancellationException} is propagated untouched. So is any other {@link
   *       RuntimeException} ({@code get} implementations are discouraged from throwing such
   *       exceptions).
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
  public static <V> V getUnchecked(Future<V> future) {
    checkNotNull(future);
    try {
      return getUninterruptibly(future);
    } catch (ExecutionException e) {
      wrapAndThrowUnchecked(e.getCause());
      throw new AssertionError();
    }
  }

  private static void wrapAndThrowUnchecked(Throwable cause) {
    if (cause instanceof Error) {
      throw new ExecutionError((Error) cause);
    }
    /*
     * It's an Exception. (Or it's a non-Error, non-Exception Throwable. From my survey of such
     * classes, I believe that most users intended to extend Exception, so we'll treat it like an
     * Exception.)
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
}
