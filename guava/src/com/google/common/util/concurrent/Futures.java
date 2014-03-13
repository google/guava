/*
 * Copyright (C) 2006 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to the {@link Future} interface.
 *
 * <p>Many of these methods use the {@link ListenableFuture} API; consult the
 * Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/ListenableFutureExplained">
 * {@code ListenableFuture}</a>.
 *
 * @author Kevin Bourrillion
 * @author Nishant Thakkar
 * @author Sven Mawson
 * @since 1.0
 */
@Beta
public final class Futures {
  private Futures() {}

  /**
   * Creates a {@link CheckedFuture} out of a normal {@link ListenableFuture}
   * and a {@link Function} that maps from {@link Exception} instances into the
   * appropriate checked type.
   *
   * <p>The given mapping function will be applied to an
   * {@link InterruptedException}, a {@link CancellationException}, or an
   * {@link ExecutionException}.
   * See {@link Future#get()} for details on the exceptions thrown.
   *
   * @since 9.0 (source-compatible since 1.0)
   */
  public static <V, X extends Exception> CheckedFuture<V, X> makeChecked(
      ListenableFuture<V> future, Function<Exception, X> mapper) {
    return new MappingCheckedFuture<V, X>(checkNotNull(future), mapper);
  }

  private abstract static class ImmediateFuture<V>
      implements ListenableFuture<V> {

    private static final Logger log =
        Logger.getLogger(ImmediateFuture.class.getName());

    @Override
    public void addListener(Runnable listener, Executor executor) {
      checkNotNull(listener, "Runnable was null.");
      checkNotNull(executor, "Executor was null.");
      try {
        executor.execute(listener);
      } catch (RuntimeException e) {
        // ListenableFuture's contract is that it will not throw unchecked
        // exceptions, so log the bad runnable and/or executor and swallow it.
        log.log(Level.SEVERE, "RuntimeException while executing runnable "
            + listener + " with executor " + executor, e);
      }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public abstract V get() throws ExecutionException;

    @Override
    public V get(long timeout, TimeUnit unit) throws ExecutionException {
      checkNotNull(unit);
      return get();
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return true;
    }
  }

  private static class ImmediateSuccessfulFuture<V> extends ImmediateFuture<V> {

    @Nullable private final V value;

    ImmediateSuccessfulFuture(@Nullable V value) {
      this.value = value;
    }

    @Override
    public V get() {
      return value;
    }
  }

  private static class ImmediateSuccessfulCheckedFuture<V, X extends Exception>
      extends ImmediateFuture<V> implements CheckedFuture<V, X> {

    @Nullable private final V value;

    ImmediateSuccessfulCheckedFuture(@Nullable V value) {
      this.value = value;
    }

    @Override
    public V get() {
      return value;
    }

    @Override
    public V checkedGet() {
      return value;
    }

    @Override
    public V checkedGet(long timeout, TimeUnit unit) {
      checkNotNull(unit);
      return value;
    }
  }

  private static class ImmediateFailedFuture<V> extends ImmediateFuture<V> {

    private final Throwable thrown;

    ImmediateFailedFuture(Throwable thrown) {
      this.thrown = thrown;
    }

    @Override
    public V get() throws ExecutionException {
      throw new ExecutionException(thrown);
    }
  }

  private static class ImmediateCancelledFuture<V> extends ImmediateFuture<V> {

    private final CancellationException thrown;

    ImmediateCancelledFuture() {
      this.thrown = new CancellationException("Immediate cancelled future.");
    }

    @Override
    public boolean isCancelled() {
      return true;
    }

    @Override
    public V get() {
      throw AbstractFuture.cancellationExceptionWithCause(
          "Task was cancelled.", thrown);
    }
  }

  private static class ImmediateFailedCheckedFuture<V, X extends Exception>
      extends ImmediateFuture<V> implements CheckedFuture<V, X> {

    private final X thrown;

    ImmediateFailedCheckedFuture(X thrown) {
      this.thrown = thrown;
    }

    @Override
    public V get() throws ExecutionException {
      throw new ExecutionException(thrown);
    }

    @Override
    public V checkedGet() throws X {
      throw thrown;
    }

    @Override
    public V checkedGet(long timeout, TimeUnit unit) throws X {
      checkNotNull(unit);
      throw thrown;
    }
  }

  /**
   * Creates a {@code ListenableFuture} which has its value set immediately upon
   * construction. The getters just return the value. This {@code Future} can't
   * be canceled or timed out and its {@code isDone()} method always returns
   * {@code true}.
   */
  public static <V> ListenableFuture<V> immediateFuture(@Nullable V value) {
    return new ImmediateSuccessfulFuture<V>(value);
  }

  /**
   * Returns a {@code CheckedFuture} which has its value set immediately upon
   * construction.
   *
   * <p>The returned {@code Future} can't be cancelled, and its {@code isDone()}
   * method always returns {@code true}. Calling {@code get()} or {@code
   * checkedGet()} will immediately return the provided value.
   */
  public static <V, X extends Exception> CheckedFuture<V, X>
      immediateCheckedFuture(@Nullable V value) {
    return new ImmediateSuccessfulCheckedFuture<V, X>(value);
  }

  /**
   * Returns a {@code ListenableFuture} which has an exception set immediately
   * upon construction.
   *
   * <p>The returned {@code Future} can't be cancelled, and its {@code isDone()}
   * method always returns {@code true}. Calling {@code get()} will immediately
   * throw the provided {@code Throwable} wrapped in an {@code
   * ExecutionException}.
   */
  public static <V> ListenableFuture<V> immediateFailedFuture(
      Throwable throwable) {
    checkNotNull(throwable);
    return new ImmediateFailedFuture<V>(throwable);
  }

  /**
   * Creates a {@code ListenableFuture} which is cancelled immediately upon
   * construction, so that {@code isCancelled()} always returns {@code true}.
   *
   * @since 14.0
   */
  public static <V> ListenableFuture<V> immediateCancelledFuture() {
    return new ImmediateCancelledFuture<V>();
  }

  /**
   * Returns a {@code CheckedFuture} which has an exception set immediately upon
   * construction.
   *
   * <p>The returned {@code Future} can't be cancelled, and its {@code isDone()}
   * method always returns {@code true}. Calling {@code get()} will immediately
   * throw the provided {@code Exception} wrapped in an {@code
   * ExecutionException}, and calling {@code checkedGet()} will throw the
   * provided exception itself.
   */
  public static <V, X extends Exception> CheckedFuture<V, X>
      immediateFailedCheckedFuture(X exception) {
    checkNotNull(exception);
    return new ImmediateFailedCheckedFuture<V, X>(exception);
  }

  /**
   * Returns a {@code Future} whose result is taken from the given primary
   * {@code input} or, if the primary input fails, from the {@code Future}
   * provided by the {@code fallback}. {@link FutureFallback#create} is not
   * invoked until the primary input has failed, so if the primary input
   * succeeds, it is never invoked. If, during the invocation of {@code
   * fallback}, an exception is thrown, this exception is used as the result of
   * the output {@code Future}.
   *
   * <p>Below is an example of a fallback that returns a default value if an
   * exception occurs:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter in case an exception happens when
   *   // processing the RPC to fetch counters.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.withFallback(
   *       fetchCounterFuture, new FutureFallback<Integer>() {
   *         public ListenableFuture<Integer> create(Throwable t) {
   *           // Returning "0" as the default for the counter when the
   *           // exception happens.
   *           return immediateFuture(0);
   *         }
   *       });}</pre>
   *
   * <p>The fallback can also choose to propagate the original exception when
   * desired:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter only in case the exception was a
   *   // TimeoutException.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.withFallback(
   *       fetchCounterFuture, new FutureFallback<Integer>() {
   *         public ListenableFuture<Integer> create(Throwable t) {
   *           if (t instanceof TimeoutException) {
   *             return immediateFuture(0);
   *           }
   *           return immediateFailedFuture(t);
   *         }
   *       });}</pre>
   *
   * <p>Note: If the derived {@code Future} is slow or heavyweight to create
   * (whether the {@code Future} itself is slow or heavyweight to complete is
   * irrelevant), consider {@linkplain #withFallback(ListenableFuture,
   * FutureFallback, Executor) supplying an executor}. If you do not supply an
   * executor, {@code withFallback} will use {@link
   * MoreExecutors#sameThreadExecutor sameThreadExecutor}, which carries some
   * caveats for heavier operations. For example, the call to {@code
   * fallback.create} may run on an unpredictable or undesirable thread:
   *
   * <ul>
   * <li>If the input {@code Future} is done at the time {@code withFallback}
   * is called, {@code withFallback} will call {@code fallback.create} inline.
   * <li>If the input {@code Future} is not yet done, {@code withFallback} will
   * schedule {@code fallback.create} to be run by the thread that completes
   * the input {@code Future}, which may be an internal system thread such as
   * an RPC network thread.
   * </ul>
   *
   * <p>Also note that, regardless of which thread executes the {@code
   * sameThreadExecutor} {@code fallback.create}, all other registered but
   * unexecuted listeners are prevented from running during its execution, even
   * if those listeners are to run in other executors.
   *
   * @param input the primary input {@code Future}
   * @param fallback the {@link FutureFallback} implementation to be called if
   *     {@code input} fails
   * @since 14.0
   */
  public static <V> ListenableFuture<V> withFallback(
      ListenableFuture<? extends V> input,
      FutureFallback<? extends V> fallback) {
    return withFallback(input, fallback, sameThreadExecutor());
  }

  /**
   * Returns a {@code Future} whose result is taken from the given primary
   * {@code input} or, if the primary input fails, from the {@code Future}
   * provided by the {@code fallback}. {@link FutureFallback#create} is not
   * invoked until the primary input has failed, so if the primary input
   * succeeds, it is never invoked. If, during the invocation of {@code
   * fallback}, an exception is thrown, this exception is used as the result of
   * the output {@code Future}.
   *
   * <p>Below is an example of a fallback that returns a default value if an
   * exception occurs:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter in case an exception happens when
   *   // processing the RPC to fetch counters.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.withFallback(
   *       fetchCounterFuture, new FutureFallback<Integer>() {
   *         public ListenableFuture<Integer> create(Throwable t) {
   *           // Returning "0" as the default for the counter when the
   *           // exception happens.
   *           return immediateFuture(0);
   *         }
   *       }, sameThreadExecutor());}</pre>
   *
   * <p>The fallback can also choose to propagate the original exception when
   * desired:
   *
   * <pre>   {@code
   *   ListenableFuture<Integer> fetchCounterFuture = ...;
   *
   *   // Falling back to a zero counter only in case the exception was a
   *   // TimeoutException.
   *   ListenableFuture<Integer> faultTolerantFuture = Futures.withFallback(
   *       fetchCounterFuture, new FutureFallback<Integer>() {
   *         public ListenableFuture<Integer> create(Throwable t) {
   *           if (t instanceof TimeoutException) {
   *             return immediateFuture(0);
   *           }
   *           return immediateFailedFuture(t);
   *         }
   *       }, sameThreadExecutor());}</pre>
   *
   * <p>When the execution of {@code fallback.create} is fast and lightweight
   * (though the {@code Future} it returns need not meet these criteria),
   * consider {@linkplain #withFallback(ListenableFuture, FutureFallback)
   * omitting the executor} or explicitly specifying {@code
   * sameThreadExecutor}. However, be aware of the caveats documented in the
   * link above.
   *
   * @param input the primary input {@code Future}
   * @param fallback the {@link FutureFallback} implementation to be called if
   *     {@code input} fails
   * @param executor the executor that runs {@code fallback} if {@code input}
   *     fails
   * @since 14.0
   */
  public static <V> ListenableFuture<V> withFallback(
      ListenableFuture<? extends V> input,
      FutureFallback<? extends V> fallback, Executor executor) {
    checkNotNull(fallback);
    return new FallbackFuture<V>(input, fallback, executor);
  }

  /**
   * A future that falls back on a second, generated future, in case its
   * original future fails.
   */
  private static class FallbackFuture<V> extends AbstractFuture<V> {

    private volatile ListenableFuture<? extends V> running;

    FallbackFuture(ListenableFuture<? extends V> input,
        final FutureFallback<? extends V> fallback,
        final Executor executor) {
      running = input;
      addCallback(running, new FutureCallback<V>() {
        @Override
        public void onSuccess(V value) {
          set(value);
        }

        @Override
        public void onFailure(Throwable t) {
          if (isCancelled()) {
            return;
          }
          try {
            running = fallback.create(t);
            if (isCancelled()) { // in case cancel called in the meantime
              running.cancel(wasInterrupted());
              return;
            }
            addCallback(running, new FutureCallback<V>() {
              @Override
              public void onSuccess(V value) {
                set(value);
              }

              @Override
              public void onFailure(Throwable t) {
                if (running.isCancelled()) {
                  cancel(false);
                } else {
                  setException(t);
                }
              }
            }, sameThreadExecutor());
          } catch (Throwable e) {
            setException(e);
          }
        }
      }, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      if (super.cancel(mayInterruptIfRunning)) {
        running.cancel(mayInterruptIfRunning);
        return true;
      }
      return false;
    }
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is asynchronously
   * derived from the result of the given {@code Future}. More precisely, the
   * returned {@code Future} takes its result from a {@code Future} produced by
   * applying the given {@code AsyncFunction} to the result of the original
   * {@code Future}. Example:
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
   *       transform(rowKeyFuture, queryFunction);}</pre>
   *
   * <p>Note: If the derived {@code Future} is slow or heavyweight to create
   * (whether the {@code Future} itself is slow or heavyweight to complete is
   * irrelevant), consider {@linkplain #transform(ListenableFuture,
   * AsyncFunction, Executor) supplying an executor}. If you do not supply an
   * executor, {@code transform} will use {@link
   * MoreExecutors#sameThreadExecutor sameThreadExecutor}, which carries some
   * caveats for heavier operations. For example, the call to {@code
   * function.apply} may run on an unpredictable or undesirable thread:
   *
   * <ul>
   * <li>If the input {@code Future} is done at the time {@code transform} is
   * called, {@code transform} will call {@code function.apply} inline.
   * <li>If the input {@code Future} is not yet done, {@code transform} will
   * schedule {@code function.apply} to be run by the thread that completes the
   * input {@code Future}, which may be an internal system thread such as an
   * RPC network thread.
   * </ul>
   *
   * <p>Also note that, regardless of which thread executes the {@code
   * sameThreadExecutor} {@code function.apply}, all other registered but
   * unexecuted listeners are prevented from running during its execution, even
   * if those listeners are to run in other executors.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in
   * sync with that of the input future and that of the future returned by the
   * function. That is, if the returned {@code Future} is cancelled, it will
   * attempt to cancel the other two, and if either of the other two is
   * cancelled, the returned {@code Future} will receive a callback in which it
   * will attempt to cancel itself.
   *
   * @param input The future to transform
   * @param function A function to transform the result of the input future
   *     to the result of the output future
   * @return A future that holds result of the function (if the input succeeded)
   *     or the original input's failure (if not)
   * @since 11.0
   */
  public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input,
      AsyncFunction<? super I, ? extends O> function) {
    return transform(input, function, MoreExecutors.sameThreadExecutor());
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is asynchronously
   * derived from the result of the given {@code Future}. More precisely, the
   * returned {@code Future} takes its result from a {@code Future} produced by
   * applying the given {@code AsyncFunction} to the result of the original
   * {@code Future}. Example:
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
   *       transform(rowKeyFuture, queryFunction, executor);}</pre>
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in
   * sync with that of the input future and that of the future returned by the
   * chain function. That is, if the returned {@code Future} is cancelled, it
   * will attempt to cancel the other two, and if either of the other two is
   * cancelled, the returned {@code Future} will receive a callback in which it
   * will attempt to cancel itself.
   *
   * <p>When the execution of {@code function.apply} is fast and lightweight
   * (though the {@code Future} it returns need not meet these criteria),
   * consider {@linkplain #transform(ListenableFuture, AsyncFunction) omitting
   * the executor} or explicitly specifying {@code sameThreadExecutor}.
   * However, be aware of the caveats documented in the link above.
   *
   * @param input The future to transform
   * @param function A function to transform the result of the input future
   *     to the result of the output future
   * @param executor Executor to run the function in.
   * @return A future that holds result of the function (if the input succeeded)
   *     or the original input's failure (if not)
   * @since 11.0
   */
  public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input,
      AsyncFunction<? super I, ? extends O> function,
      Executor executor) {
    ChainingListenableFuture<I, O> output =
        new ChainingListenableFuture<I, O>(function, input);
    input.addListener(output, executor);
    return output;
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is the product of
   * applying the given {@code Function} to the result of the given {@code
   * Future}. Example:
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
   * <p>Note: If the transformation is slow or heavyweight, consider {@linkplain
   * #transform(ListenableFuture, Function, Executor) supplying an executor}.
   * If you do not supply an executor, {@code transform} will use {@link
   * MoreExecutors#sameThreadExecutor sameThreadExecutor}, which carries some
   * caveats for heavier operations.  For example, the call to {@code
   * function.apply} may run on an unpredictable or undesirable thread:
   *
   * <ul>
   * <li>If the input {@code Future} is done at the time {@code transform} is
   * called, {@code transform} will call {@code function.apply} inline.
   * <li>If the input {@code Future} is not yet done, {@code transform} will
   * schedule {@code function.apply} to be run by the thread that completes the
   * input {@code Future}, which may be an internal system thread such as an
   * RPC network thread.
   * </ul>
   *
   * <p>Also note that, regardless of which thread executes the {@code
   * sameThreadExecutor} {@code function.apply}, all other registered but
   * unexecuted listeners are prevented from running during its execution, even
   * if those listeners are to run in other executors.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in
   * sync with that of the input future. That is, if the returned {@code Future}
   * is cancelled, it will attempt to cancel the input, and if the input is
   * cancelled, the returned {@code Future} will receive a callback in which it
   * will attempt to cancel itself.
   *
   * <p>An example use of this method is to convert a serializable object
   * returned from an RPC into a POJO.
   *
   * @param input The future to transform
   * @param function A Function to transform the results of the provided future
   *     to the results of the returned future.  This will be run in the thread
   *     that notifies input it is complete.
   * @return A future that holds result of the transformation.
   * @since 9.0 (in 1.0 as {@code compose})
   */
  public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input,
      final Function<? super I, ? extends O> function) {
    return transform(input, function, MoreExecutors.sameThreadExecutor());
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is the product of
   * applying the given {@code Function} to the result of the given {@code
   * Future}. Example:
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
   * <p>The returned {@code Future} attempts to keep its cancellation state in
   * sync with that of the input future. That is, if the returned {@code Future}
   * is cancelled, it will attempt to cancel the input, and if the input is
   * cancelled, the returned {@code Future} will receive a callback in which it
   * will attempt to cancel itself.
   *
   * <p>An example use of this method is to convert a serializable object
   * returned from an RPC into a POJO.
   *
   * <p>When the transformation is fast and lightweight, consider {@linkplain
   * #transform(ListenableFuture, Function) omitting the executor} or
   * explicitly specifying {@code sameThreadExecutor}. However, be aware of the
   * caveats documented in the link above.
   *
   * @param input The future to transform
   * @param function A Function to transform the results of the provided future
   *     to the results of the returned future.
   * @param executor Executor to run the function in.
   * @return A future that holds result of the transformation.
   * @since 9.0 (in 2.0 as {@code compose})
   */
  public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input,
      final Function<? super I, ? extends O> function, Executor executor) {
    checkNotNull(function);
    AsyncFunction<I, O> wrapperFunction
        = new AsyncFunction<I, O>() {
            @Override public ListenableFuture<O> apply(I input) {
              O output = function.apply(input);
              return immediateFuture(output);
            }
        };
    return transform(input, wrapperFunction, executor);
  }

  /**
   * Like {@link #transform(ListenableFuture, Function)} except that the
   * transformation {@code function} is invoked on each call to
   * {@link Future#get() get()} on the returned future.
   *
   * <p>The returned {@code Future} reflects the input's cancellation
   * state directly, and any attempt to cancel the returned Future is likewise
   * passed through to the input Future.
   *
   * <p>Note that calls to {@linkplain Future#get(long, TimeUnit) timed get}
   * only apply the timeout to the execution of the underlying {@code Future},
   * <em>not</em> to the execution of the transformation function.
   *
   * <p>The primary audience of this method is callers of {@code transform}
   * who don't have a {@code ListenableFuture} available and
   * do not mind repeated, lazy function evaluation.
   *
   * @param input The future to transform
   * @param function A Function to transform the results of the provided future
   *     to the results of the returned future.
   * @return A future that returns the result of the transformation.
   * @since 10.0
   */
  public static <I, O> Future<O> lazyTransform(final Future<I> input,
      final Function<? super I, ? extends O> function) {
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
   * An implementation of {@code ListenableFuture} that also implements
   * {@code Runnable} so that it can be used to nest ListenableFutures.
   * Once the passed-in {@code ListenableFuture} is complete, it calls the
   * passed-in {@code Function} to generate the result.
   *
   * <p>For historical reasons, this class has a special case in its exception
   * handling: If the given {@code AsyncFunction} throws an {@code
   * UndeclaredThrowableException}, {@code ChainingListenableFuture} unwraps it
   * and uses its <i>cause</i> as the output future's exception, rather than
   * using the {@code UndeclaredThrowableException} itself as it would for other
   * exception types. The reason for this is that {@code Futures.transform} used
   * to require a {@code Function}, whose {@code apply} method is not allowed to
   * throw checked exceptions. Nowadays, {@code Futures.transform} has an
   * overload that accepts an {@code AsyncFunction}, whose {@code apply} method
   * <i>is</i> allowed to throw checked exception. Users who wish to throw
   * checked exceptions should use that overload instead, and <a
   * href="http://code.google.com/p/guava-libraries/issues/detail?id=1548">we
   * should remove the {@code UndeclaredThrowableException} special case</a>.
   */
  private static class ChainingListenableFuture<I, O>
      extends AbstractFuture<O> implements Runnable {

    private AsyncFunction<? super I, ? extends O> function;
    private ListenableFuture<? extends I> inputFuture;
    private volatile ListenableFuture<? extends O> outputFuture;
    private final CountDownLatch outputCreated = new CountDownLatch(1);

    private ChainingListenableFuture(
        AsyncFunction<? super I, ? extends O> function,
        ListenableFuture<? extends I> inputFuture) {
      this.function = checkNotNull(function);
      this.inputFuture = checkNotNull(inputFuture);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      /*
       * Our additional cancellation work needs to occur even if
       * !mayInterruptIfRunning, so we can't move it into interruptTask().
       */
      if (super.cancel(mayInterruptIfRunning)) {
        // This should never block since only one thread is allowed to cancel
        // this Future.
        cancel(inputFuture, mayInterruptIfRunning);
        cancel(outputFuture, mayInterruptIfRunning);
        return true;
      }
      return false;
    }

    private void cancel(@Nullable Future<?> future,
        boolean mayInterruptIfRunning) {
      if (future != null) {
        future.cancel(mayInterruptIfRunning);
      }
    }

    @Override
    public void run() {
      try {
        I sourceResult;
        try {
          sourceResult = getUninterruptibly(inputFuture);
        } catch (CancellationException e) {
          // Cancel this future and return.
          // At this point, inputFuture is cancelled and outputFuture doesn't
          // exist, so the value of mayInterruptIfRunning is irrelevant.
          cancel(false);
          return;
        } catch (ExecutionException e) {
          // Set the cause of the exception as this future's exception
          setException(e.getCause());
          return;
        }

        final ListenableFuture<? extends O> outputFuture = this.outputFuture =
            Preconditions.checkNotNull(function.apply(sourceResult),
                "AsyncFunction may not return null.");
        if (isCancelled()) {
          outputFuture.cancel(wasInterrupted());
          this.outputFuture = null;
          return;
        }
        outputFuture.addListener(new Runnable() {
            @Override
            public void run() {
              try {
                set(getUninterruptibly(outputFuture));
              } catch (CancellationException e) {
                // Cancel this future and return.
                // At this point, inputFuture and outputFuture are done, so the
                // value of mayInterruptIfRunning is irrelevant.
                cancel(false);
                return;
              } catch (ExecutionException e) {
                // Set the cause of the exception as this future's exception
                setException(e.getCause());
              } finally {
                // Don't pin inputs beyond completion
                ChainingListenableFuture.this.outputFuture = null;
              }
            }
          }, MoreExecutors.sameThreadExecutor());
      } catch (UndeclaredThrowableException e) {
        // Set the cause of the exception as this future's exception
        setException(e.getCause());
      } catch (Throwable t) {
        // This exception is irrelevant in this thread, but useful for the
        // client
        setException(t);
      } finally {
        // Don't pin inputs beyond completion
        function = null;
        inputFuture = null;
        // Allow our get routines to examine outputFuture now.
        outputCreated.countDown();
      }
    }
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is the product of
   * calling {@code get()} on the {@code Future} nested within the given {@code
   * Future}, effectively chaining the futures one after the other.  Example:
   *
   * <pre>   {@code
   *   SettableFuture<ListenableFuture<String>> nested = SettableFuture.create();
   *   ListenableFuture<String> dereferenced = dereference(nested);}</pre>
   *
   * <p>This call has the same cancellation and execution semantics as {@link
   * #transform(ListenableFuture, AsyncFunction)}, in that the returned {@code
   * Future} attempts to keep its cancellation state in sync with both the
   * input {@code Future} and the nested {@code Future}.  The transformation
   * is very lightweight and therefore takes place in the same thread (either
   * the thread that called {@code dereference}, or the thread in which the
   * dereferenced future completes).
   *
   * @param nested The nested future to transform.
   * @return A future that holds result of the inner future.
   * @since 13.0
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static <V> ListenableFuture<V> dereference(
      ListenableFuture<? extends ListenableFuture<? extends V>> nested) {
    return Futures.transform((ListenableFuture) nested, (AsyncFunction) DEREFERENCER);
  }

  /**
   * Helper {@code Function} for {@link #dereference}.
   */
  private static final AsyncFunction<ListenableFuture<Object>, Object> DEREFERENCER =
      new AsyncFunction<ListenableFuture<Object>, Object>() {
        @Override public ListenableFuture<Object> apply(ListenableFuture<Object> input) {
          return input;
        }
      };

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the
   * values of all its input futures, if all succeed. If any input fails, the
   * returned future fails.
   *
   * <p>The list of results is in the same order as the input list.
   *
   * <p>Canceling this future will attempt to cancel all the component futures,
   * and if any of the provided futures fails or is canceled, this one is,
   * too.
   *
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component
   *         futures
   * @since 10.0
   */
  @Beta
  public static <V> ListenableFuture<List<V>> allAsList(
      ListenableFuture<? extends V>... futures) {
    return listFuture(ImmutableList.copyOf(futures), true,
        MoreExecutors.sameThreadExecutor());
  }

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the
   * values of all its input futures, if all succeed. If any input fails, the
   * returned future fails.
   *
   * <p>The list of results is in the same order as the input list.
   *
   * <p>Canceling this future will attempt to cancel all the component futures,
   * and if any of the provided futures fails or is canceled, this one is,
   * too.
   *
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component
   *         futures
   * @since 10.0
   */
  @Beta
  public static <V> ListenableFuture<List<V>> allAsList(
      Iterable<? extends ListenableFuture<? extends V>> futures) {
    return listFuture(ImmutableList.copyOf(futures), true,
        MoreExecutors.sameThreadExecutor());
  }

  /**
   * Creates a new {@code ListenableFuture} whose result is set from the
   * supplied future when it completes.  Cancelling the supplied future
   * will also cancel the returned future, but cancelling the returned
   * future will have no effect on the supplied future.
   *
   * @since 15.0
   */
  public static <V> ListenableFuture<V> nonCancellationPropagating(
      ListenableFuture<V> future) {
    return new NonCancellationPropagatingFuture<V>(future);
  }

  /**
   * A wrapped future that does not propagate cancellation to its delegate.
   */
  private static class NonCancellationPropagatingFuture<V>
      extends AbstractFuture<V> {
    NonCancellationPropagatingFuture(final ListenableFuture<V> delegate) {
      checkNotNull(delegate);
      addCallback(delegate, new FutureCallback<V>() {
        @Override
        public void onSuccess(V result) {
          set(result);
        }

        @Override
        public void onFailure(Throwable t) {
          if (delegate.isCancelled()) {
            cancel(false);
          } else {
            setException(t);
          }
        }
      }, sameThreadExecutor());
    }
  }

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the
   * values of all its successful input futures. The list of results is in the
   * same order as the input list, and if any of the provided futures fails or
   * is canceled, its corresponding position will contain {@code null} (which is
   * indistinguishable from the future having a successful value of
   * {@code null}).
   *
   * <p>Canceling this future will attempt to cancel all the component futures.
   *
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component
   *         futures
   * @since 10.0
   */
  @Beta
  public static <V> ListenableFuture<List<V>> successfulAsList(
      ListenableFuture<? extends V>... futures) {
    return listFuture(ImmutableList.copyOf(futures), false,
        MoreExecutors.sameThreadExecutor());
  }

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the
   * values of all its successful input futures. The list of results is in the
   * same order as the input list, and if any of the provided futures fails or
   * is canceled, its corresponding position will contain {@code null} (which is
   * indistinguishable from the future having a successful value of
   * {@code null}).
   *
   * <p>Canceling this future will attempt to cancel all the component futures.
   *
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component
   *         futures
   * @since 10.0
   */
  @Beta
  public static <V> ListenableFuture<List<V>> successfulAsList(
      Iterable<? extends ListenableFuture<? extends V>> futures) {
    return listFuture(ImmutableList.copyOf(futures), false,
        MoreExecutors.sameThreadExecutor());
  }

  /**
   * Returns a list of delegate futures that correspond to the futures received in the order
   * that they complete. Delegate futures return the same value or throw the same exception
   * as the corresponding input future returns/throws.
   *
   * <p>Cancelling a delegate future has no effect on any input future, since the delegate future
   * does not correspond to a specific input future until the appropriate number of input
   * futures have completed. At that point, it is too late to cancel the input future.
   * The input future's result, which cannot be stored into the cancelled delegate future,
   * is ignored.
   *
   * @since 17.0
   */
  @Beta
  public static <T> ImmutableList<ListenableFuture<T>> inCompletionOrder(
      Iterable<? extends ListenableFuture<? extends T>> futures) {
    // A CLQ may be overkill here.  We could save some pointers/memory by synchronizing on an
    // ArrayDeque
    final ConcurrentLinkedQueue<AsyncSettableFuture<T>> delegates =
        Queues.newConcurrentLinkedQueue();
    ImmutableList.Builder<ListenableFuture<T>> listBuilder = ImmutableList.builder();
    // Using SerializingExecutor here will ensure that each CompletionOrderListener executes
    // atomically and therefore that each returned future is guaranteed to be in completion order.
    // N.B. there are some cases where the use of this executor could have possibly surprising
    // effects when input futures finish at approximately the same time _and_ the output futures
    // have sameThreadExecutor listeners. In this situation, the listeners may end up running on a
    // different thread than if they were attached to the corresponding input future.  We believe
    // this to be a negligible cost since:
    // 1. Using the sameThreadExecutor implies that your callback is safe to run on any thread.
    // 2. This would likely only be noticeable if you were doing something expensive or blocking on
    //    a sameThreadExecutor listener on one of the output futures which is an antipattern anyway.
    SerializingExecutor executor = new SerializingExecutor(MoreExecutors.sameThreadExecutor());
    for (final ListenableFuture<? extends T> future : futures) {
      AsyncSettableFuture<T> delegate = AsyncSettableFuture.create();
      // Must make sure to add the delegate to the queue first in case the future is already done
      delegates.add(delegate);
      future.addListener(new Runnable() {
        @Override public void run() {
          delegates.remove().setFuture(future);
        }
      }, executor);
      listBuilder.add(delegate);
    }
    return listBuilder.build();
  }

  /**
   * Registers separate success and failure callbacks to be run when the {@code
   * Future}'s computation is {@linkplain java.util.concurrent.Future#isDone()
   * complete} or, if the computation is already complete, immediately.
   *
   * <p>There is no guaranteed ordering of execution of callbacks, but any
   * callback added through this method is guaranteed to be called once the
   * computation is complete.
   *
   * Example: <pre> {@code
   * ListenableFuture<QueryResult> future = ...;
   * addCallback(future,
   *     new FutureCallback<QueryResult> {
   *       public void onSuccess(QueryResult result) {
   *         storeInCache(result);
   *       }
   *       public void onFailure(Throwable t) {
   *         reportError(t);
   *       }
   *     });}</pre>
   *
   * <p>Note: If the callback is slow or heavyweight, consider {@linkplain
   * #addCallback(ListenableFuture, FutureCallback, Executor) supplying an
   * executor}. If you do not supply an executor, {@code addCallback} will use
   * {@link MoreExecutors#sameThreadExecutor sameThreadExecutor}, which carries
   * some caveats for heavier operations. For example, the callback may run on
   * an unpredictable or undesirable thread:
   *
   * <ul>
   * <li>If the input {@code Future} is done at the time {@code addCallback} is
   * called, {@code addCallback} will execute the callback inline.
   * <li>If the input {@code Future} is not yet done, {@code addCallback} will
   * schedule the callback to be run by the thread that completes the input
   * {@code Future}, which may be an internal system thread such as an RPC
   * network thread.
   * </ul>
   *
   * <p>Also note that, regardless of which thread executes the {@code
   * sameThreadExecutor} callback, all other registered but unexecuted listeners
   * are prevented from running during its execution, even if those listeners
   * are to run in other executors.
   *
   * <p>For a more general interface to attach a completion listener to a
   * {@code Future}, see {@link ListenableFuture#addListener addListener}.
   *
   * @param future The future attach the callback to.
   * @param callback The callback to invoke when {@code future} is completed.
   * @since 10.0
   */
  public static <V> void addCallback(ListenableFuture<V> future,
      FutureCallback<? super V> callback) {
    addCallback(future, callback, MoreExecutors.sameThreadExecutor());
  }

  /**
   * Registers separate success and failure callbacks to be run when the {@code
   * Future}'s computation is {@linkplain java.util.concurrent.Future#isDone()
   * complete} or, if the computation is already complete, immediately.
   *
   * <p>The callback is run in {@code executor}.
   * There is no guaranteed ordering of execution of callbacks, but any
   * callback added through this method is guaranteed to be called once the
   * computation is complete.
   *
   * Example: <pre> {@code
   * ListenableFuture<QueryResult> future = ...;
   * Executor e = ...
   * addCallback(future,
   *     new FutureCallback<QueryResult> {
   *       public void onSuccess(QueryResult result) {
   *         storeInCache(result);
   *       }
   *       public void onFailure(Throwable t) {
   *         reportError(t);
   *       }
   *     }, e);}</pre>
   *
   * <p>When the callback is fast and lightweight, consider {@linkplain
   * #addCallback(ListenableFuture, FutureCallback) omitting the executor} or
   * explicitly specifying {@code sameThreadExecutor}. However, be aware of the
   * caveats documented in the link above.
   *
   * <p>For a more general interface to attach a completion listener to a
   * {@code Future}, see {@link ListenableFuture#addListener addListener}.
   *
   * @param future The future attach the callback to.
   * @param callback The callback to invoke when {@code future} is completed.
   * @param executor The executor to run {@code callback} when the future
   *    completes.
   * @since 10.0
   */
  public static <V> void addCallback(final ListenableFuture<V> future,
      final FutureCallback<? super V> callback, Executor executor) {
    Preconditions.checkNotNull(callback);
    Runnable callbackListener = new Runnable() {
      @Override
      public void run() {
        final V value;
        try {
          // TODO(user): (Before Guava release), validate that this
          // is the thing for IE.
          value = getUninterruptibly(future);
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
   * Returns the result of {@link Future#get()}, converting most exceptions to a
   * new instance of the given checked exception type. This reduces boilerplate
   * for a common use of {@code Future} in which it is unnecessary to
   * programmatically distinguish between exception types or to extract other
   * information from the exception instance.
   *
   * <p>Exceptions from {@code Future.get} are treated as follows:
   * <ul>
   * <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an
   *     {@code X} if the cause is a checked exception, an {@link
   *     UncheckedExecutionException} if the cause is a {@code
   *     RuntimeException}, or an {@link ExecutionError} if the cause is an
   *     {@code Error}.
   * <li>Any {@link InterruptedException} is wrapped in an {@code X} (after
   *     restoring the interrupt).
   * <li>Any {@link CancellationException} is propagated untouched, as is any
   *     other {@link RuntimeException} (though {@code get} implementations are
   *     discouraged from throwing such exceptions).
   * </ul>
   *
   * <p>The overall principle is to continue to treat every checked exception as a
   * checked exception, every unchecked exception as an unchecked exception, and
   * every error as an error. In addition, the cause of any {@code
   * ExecutionException} is wrapped in order to ensure that the new stack trace
   * matches that of the current thread.
   *
   * <p>Instances of {@code exceptionClass} are created by choosing an arbitrary
   * public constructor that accepts zero or more arguments, all of type {@code
   * String} or {@code Throwable} (preferring constructors with at least one
   * {@code String}) and calling the constructor via reflection. If the
   * exception did not already have a cause, one is set by calling {@link
   * Throwable#initCause(Throwable)} on it. If no such constructor exists, an
   * {@code IllegalArgumentException} is thrown.
   *
   * @throws X if {@code get} throws any checked exception except for an {@code
   *         ExecutionException} whose cause is not itself a checked exception
   * @throws UncheckedExecutionException if {@code get} throws an {@code
   *         ExecutionException} with a {@code RuntimeException} as its cause
   * @throws ExecutionError if {@code get} throws an {@code ExecutionException}
   *         with an {@code Error} as its cause
   * @throws CancellationException if {@code get} throws a {@code
   *         CancellationException}
   * @throws IllegalArgumentException if {@code exceptionClass} extends {@code
   *         RuntimeException} or does not have a suitable constructor
   * @since 10.0
   */
  public static <V, X extends Exception> V get(
      Future<V> future, Class<X> exceptionClass) throws X {
    checkNotNull(future);
    checkArgument(!RuntimeException.class.isAssignableFrom(exceptionClass),
        "Futures.get exception type (%s) must not be a RuntimeException",
        exceptionClass);
    try {
      return future.get();
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw newWithCause(exceptionClass, e);
    } catch (ExecutionException e) {
      wrapAndThrowExceptionOrError(e.getCause(), exceptionClass);
      throw new AssertionError();
    }
  }

  /**
   * Returns the result of {@link Future#get(long, TimeUnit)}, converting most
   * exceptions to a new instance of the given checked exception type. This
   * reduces boilerplate for a common use of {@code Future} in which it is
   * unnecessary to programmatically distinguish between exception types or to
   * extract other information from the exception instance.
   *
   * <p>Exceptions from {@code Future.get} are treated as follows:
   * <ul>
   * <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an
   *     {@code X} if the cause is a checked exception, an {@link
   *     UncheckedExecutionException} if the cause is a {@code
   *     RuntimeException}, or an {@link ExecutionError} if the cause is an
   *     {@code Error}.
   * <li>Any {@link InterruptedException} is wrapped in an {@code X} (after
   *     restoring the interrupt).
   * <li>Any {@link TimeoutException} is wrapped in an {@code X}.
   * <li>Any {@link CancellationException} is propagated untouched, as is any
   *     other {@link RuntimeException} (though {@code get} implementations are
   *     discouraged from throwing such exceptions).
   * </ul>
   *
   * <p>The overall principle is to continue to treat every checked exception as a
   * checked exception, every unchecked exception as an unchecked exception, and
   * every error as an error. In addition, the cause of any {@code
   * ExecutionException} is wrapped in order to ensure that the new stack trace
   * matches that of the current thread.
   *
   * <p>Instances of {@code exceptionClass} are created by choosing an arbitrary
   * public constructor that accepts zero or more arguments, all of type {@code
   * String} or {@code Throwable} (preferring constructors with at least one
   * {@code String}) and calling the constructor via reflection. If the
   * exception did not already have a cause, one is set by calling {@link
   * Throwable#initCause(Throwable)} on it. If no such constructor exists, an
   * {@code IllegalArgumentException} is thrown.
   *
   * @throws X if {@code get} throws any checked exception except for an {@code
   *         ExecutionException} whose cause is not itself a checked exception
   * @throws UncheckedExecutionException if {@code get} throws an {@code
   *         ExecutionException} with a {@code RuntimeException} as its cause
   * @throws ExecutionError if {@code get} throws an {@code ExecutionException}
   *         with an {@code Error} as its cause
   * @throws CancellationException if {@code get} throws a {@code
   *         CancellationException}
   * @throws IllegalArgumentException if {@code exceptionClass} extends {@code
   *         RuntimeException} or does not have a suitable constructor
   * @since 10.0
   */
  public static <V, X extends Exception> V get(
      Future<V> future, long timeout, TimeUnit unit, Class<X> exceptionClass)
      throws X {
    checkNotNull(future);
    checkNotNull(unit);
    checkArgument(!RuntimeException.class.isAssignableFrom(exceptionClass),
        "Futures.get exception type (%s) must not be a RuntimeException",
        exceptionClass);
    try {
      return future.get(timeout, unit);
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw newWithCause(exceptionClass, e);
    } catch (TimeoutException e) {
      throw newWithCause(exceptionClass, e);
    } catch (ExecutionException e) {
      wrapAndThrowExceptionOrError(e.getCause(), exceptionClass);
      throw new AssertionError();
    }
  }

  private static <X extends Exception> void wrapAndThrowExceptionOrError(
      Throwable cause, Class<X> exceptionClass) throws X {
    if (cause instanceof Error) {
      throw new ExecutionError((Error) cause);
    }
    if (cause instanceof RuntimeException) {
      throw new UncheckedExecutionException(cause);
    }
    throw newWithCause(exceptionClass, cause);
  }

  /**
   * Returns the result of calling {@link Future#get()} uninterruptibly on a
   * task known not to throw a checked exception. This makes {@code Future} more
   * suitable for lightweight, fast-running tasks that, barring bugs in the
   * code, will not fail. This gives it exception-handling behavior similar to
   * that of {@code ForkJoinTask.join}.
   *
   * <p>Exceptions from {@code Future.get} are treated as follows:
   * <ul>
   * <li>Any {@link ExecutionException} has its <i>cause</i> wrapped in an
   *     {@link UncheckedExecutionException} (if the cause is an {@code
   *     Exception}) or {@link ExecutionError} (if the cause is an {@code
   *     Error}).
   * <li>Any {@link InterruptedException} causes a retry of the {@code get}
   *     call. The interrupt is restored before {@code getUnchecked} returns.
   * <li>Any {@link CancellationException} is propagated untouched. So is any
   *     other {@link RuntimeException} ({@code get} implementations are
   *     discouraged from throwing such exceptions).
   * </ul>
   *
   * <p>The overall principle is to eliminate all checked exceptions: to loop to
   * avoid {@code InterruptedException}, to pass through {@code
   * CancellationException}, and to wrap any exception from the underlying
   * computation in an {@code UncheckedExecutionException} or {@code
   * ExecutionError}.
   *
   * <p>For an uninterruptible {@code get} that preserves other exceptions, see
   * {@link Uninterruptibles#getUninterruptibly(Future)}.
   *
   * @throws UncheckedExecutionException if {@code get} throws an {@code
   *         ExecutionException} with an {@code Exception} as its cause
   * @throws ExecutionError if {@code get} throws an {@code ExecutionException}
   *         with an {@code Error} as its cause
   * @throws CancellationException if {@code get} throws a {@code
   *         CancellationException}
   * @since 10.0
   */
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
     * It's a non-Error, non-Exception Throwable. From my survey of such
     * classes, I believe that most users intended to extend Exception, so we'll
     * treat it like an Exception.
     */
    throw new UncheckedExecutionException(cause);
  }

  /*
   * TODO(user): FutureChecker interface for these to be static methods on? If
   * so, refer to it in the (static-method) Futures.get documentation
   */

  /*
   * Arguably we don't need a timed getUnchecked because any operation slow
   * enough to require a timeout is heavyweight enough to throw a checked
   * exception and therefore be inappropriate to use with getUnchecked. Further,
   * it's not clear that converting the checked TimeoutException to a
   * RuntimeException -- especially to an UncheckedExecutionException, since it
   * wasn't thrown by the computation -- makes sense, and if we don't convert
   * it, the user still has to write a try-catch block.
   *
   * If you think you would use this method, let us know.
   */

  private static <X extends Exception> X newWithCause(
      Class<X> exceptionClass, Throwable cause) {
    // getConstructors() guarantees this as long as we don't modify the array.
    @SuppressWarnings("unchecked")
    List<Constructor<X>> constructors =
        (List) Arrays.asList(exceptionClass.getConstructors());
    for (Constructor<X> constructor : preferringStrings(constructors)) {
      @Nullable X instance = newFromConstructor(constructor, cause);
      if (instance != null) {
        if (instance.getCause() == null) {
          instance.initCause(cause);
        }
        return instance;
      }
    }
    throw new IllegalArgumentException(
        "No appropriate constructor for exception of type " + exceptionClass
            + " in response to chained exception", cause);
  }

  private static <X extends Exception> List<Constructor<X>>
      preferringStrings(List<Constructor<X>> constructors) {
    return WITH_STRING_PARAM_FIRST.sortedCopy(constructors);
  }

  private static final Ordering<Constructor<?>> WITH_STRING_PARAM_FIRST =
      Ordering.natural().onResultOf(new Function<Constructor<?>, Boolean>() {
        @Override public Boolean apply(Constructor<?> input) {
          return asList(input.getParameterTypes()).contains(String.class);
        }
      }).reverse();

  @Nullable private static <X> X newFromConstructor(
      Constructor<X> constructor, Throwable cause) {
    Class<?>[] paramTypes = constructor.getParameterTypes();
    Object[] params = new Object[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      Class<?> paramType = paramTypes[i];
      if (paramType.equals(String.class)) {
        params[i] = cause.toString();
      } else if (paramType.equals(Throwable.class)) {
        params[i] = cause;
      } else {
        return null;
      }
    }
    try {
      return constructor.newInstance(params);
    } catch (IllegalArgumentException e) {
      return null;
    } catch (InstantiationException e) {
      return null;
    } catch (IllegalAccessException e) {
      return null;
    } catch (InvocationTargetException e) {
      return null;
    }
  }

  private interface FutureCombiner<V, C> {
    C combine(List<Optional<V>> values);
  }

  private static class CombinedFuture<V, C> extends AbstractFuture<C> {
    private static final Logger logger =
        Logger.getLogger(CombinedFuture.class.getName());

    ImmutableCollection<? extends ListenableFuture<? extends V>> futures;
    final boolean allMustSucceed;
    final AtomicInteger remaining;
    FutureCombiner<V, C> combiner;
    List<Optional<V>> values;
    final Object seenExceptionsLock = new Object();
    Set<Throwable> seenExceptions;

    CombinedFuture(
        ImmutableCollection<? extends ListenableFuture<? extends V>> futures,
        boolean allMustSucceed, Executor listenerExecutor,
        FutureCombiner<V, C> combiner) {
      this.futures = futures;
      this.allMustSucceed = allMustSucceed;
      this.remaining = new AtomicInteger(futures.size());
      this.combiner = combiner;
      this.values = Lists.newArrayListWithCapacity(futures.size());
      init(listenerExecutor);
    }

    /**
     * Must be called at the end of the constructor.
     */
    protected void init(final Executor listenerExecutor) {
      // First, schedule cleanup to execute when the Future is done.
      addListener(new Runnable() {
        @Override
        public void run() {
          // Cancel all the component futures.
          if (CombinedFuture.this.isCancelled()) {
            for (ListenableFuture<?> future : CombinedFuture.this.futures) {
              future.cancel(CombinedFuture.this.wasInterrupted());
            }
          }

          // Let go of the memory held by other futures
          CombinedFuture.this.futures = null;

          // By now the values array has either been set as the Future's value,
          // or (in case of failure) is no longer useful.
          CombinedFuture.this.values = null;

          // The combiner may also hold state, so free that as well
          CombinedFuture.this.combiner = null;
        }
      }, MoreExecutors.sameThreadExecutor());

      // Now begin the "real" initialization.

      // Corner case: List is empty.
      if (futures.isEmpty()) {
        set(combiner.combine(ImmutableList.<Optional<V>>of()));
        return;
      }

      // Populate the results list with null initially.
      for (int i = 0; i < futures.size(); ++i) {
        values.add(null);
      }

      // Register a listener on each Future in the list to update
      // the state of this future.
      // Note that if all the futures on the list are done prior to completing
      // this loop, the last call to addListener() will callback to
      // setOneValue(), transitively call our cleanup listener, and set
      // this.futures to null.
      // This is not actually a problem, since the foreach only needs
      // this.futures to be non-null at the beginning of the loop.
      int i = 0;
      for (final ListenableFuture<? extends V> listenable : futures) {
        final int index = i++;
        listenable.addListener(new Runnable() {
          @Override
          public void run() {
            setOneValue(index, listenable);
          }
        }, listenerExecutor);
      }
    }

    /**
     * Fails this future with the given Throwable if {@link #allMustSucceed} is
     * true. Also, logs the throwable if it is an {@link Error} or if
     * {@link #allMustSucceed} is {@code true}, the throwable did not cause
     * this future to fail, and it is the first time we've seen that particular Throwable.
     */
    private void setExceptionAndMaybeLog(Throwable throwable) {
      boolean visibleFromOutputFuture = false;
      boolean firstTimeSeeingThisException = true;
      if (allMustSucceed) {
        // As soon as the first one fails, throw the exception up.
        // The result of all other inputs is then ignored.
        visibleFromOutputFuture = super.setException(throwable);

        synchronized (seenExceptionsLock) {
          if (seenExceptions == null) {
            seenExceptions = Sets.newHashSet();
          }
          firstTimeSeeingThisException = seenExceptions.add(throwable);
        }
      }

      if (throwable instanceof Error
          || (allMustSucceed && !visibleFromOutputFuture && firstTimeSeeingThisException)) {
        logger.log(Level.SEVERE, "input future failed.", throwable);
      }
    }

    /**
     * Sets the value at the given index to that of the given future.
     */
    private void setOneValue(int index, Future<? extends V> future) {
      List<Optional<V>> localValues = values;
      // TODO(user): This check appears to be redundant since values is
      // assigned null only after the future completes.  However, values
      // is not volatile so it may be possible for us to observe the changes
      // to these two values in a different order... which I think is why
      // we need to check both.  Clear up this craziness either by making
      // values volatile or proving that it doesn't need to be for some other
      // reason.
      if (isDone() || localValues == null) {
        // Some other future failed or has been cancelled, causing this one to
        // also be cancelled or have an exception set. This should only happen
        // if allMustSucceed is true or if the output itself has been
        // cancelled.
        checkState(allMustSucceed || isCancelled(),
            "Future was done before all dependencies completed");
      }

      try {
        checkState(future.isDone(),
            "Tried to set value from future which is not done");
        V returnValue = getUninterruptibly(future);
        if (localValues != null) {
          localValues.set(index, Optional.fromNullable(returnValue));
        }
      } catch (CancellationException e) {
        if (allMustSucceed) {
          // Set ourselves as cancelled. Let the input futures keep running
          // as some of them may be used elsewhere.
          cancel(false);
        }
      } catch (ExecutionException e) {
        setExceptionAndMaybeLog(e.getCause());
      } catch (Throwable t) {
        setExceptionAndMaybeLog(t);
      } finally {
        int newRemaining = remaining.decrementAndGet();
        checkState(newRemaining >= 0, "Less than 0 remaining futures");
        if (newRemaining == 0) {
          FutureCombiner<V, C> localCombiner = combiner;
          if (localCombiner != null && localValues != null) {
            set(localCombiner.combine(localValues));
          } else {
            checkState(isDone());
          }
        }
      }
    }

  }

  /** Used for {@link #allAsList} and {@link #successfulAsList}. */
  private static <V> ListenableFuture<List<V>> listFuture(
      ImmutableList<ListenableFuture<? extends V>> futures,
      boolean allMustSucceed, Executor listenerExecutor) {
    return new CombinedFuture<V, List<V>>(
        futures, allMustSucceed, listenerExecutor,
        new FutureCombiner<V, List<V>>() {
          @Override
          public List<V> combine(List<Optional<V>> values) {
            List<V> result = Lists.newArrayList();
            for (Optional<V> element : values) {
              result.add(element != null ? element.orNull() : null);
            }
            return Collections.unmodifiableList(result);
          }
        });
  }

  /**
   * A checked future that uses a function to map from exceptions to the
   * appropriate checked type.
   */
  private static class MappingCheckedFuture<V, X extends Exception> extends
      AbstractCheckedFuture<V, X> {

    final Function<Exception, X> mapper;

    MappingCheckedFuture(ListenableFuture<V> delegate,
        Function<Exception, X> mapper) {
      super(delegate);

      this.mapper = checkNotNull(mapper);
    }

    @Override
    protected X mapException(Exception e) {
      return mapper.apply(e);
    }
  }
}
