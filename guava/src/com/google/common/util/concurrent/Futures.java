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
import static com.google.common.util.concurrent.Uninterruptibles.putUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.takeUninterruptibly;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to the {@link Future} interface.
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
   * {@link ExecutionException} with the actual cause of the exception.
   * See {@link Future#get()} for details on the exceptions thrown.
   *
   * @since 9.0 (source-compatible since 1.0)
   */
  public static <V, X extends Exception> CheckedFuture<V, X> makeChecked(
      ListenableFuture<V> future, Function<Exception, X> mapper) {
    return new MappingCheckedFuture<V, X>(checkNotNull(future), mapper);
  }

  /**
   * Creates a {@code ListenableFuture} which has its value set immediately upon
   * construction. The getters just return the value. This {@code Future} can't
   * be canceled or timed out and its {@code isDone()} method always returns
   * {@code true}.
   */
  public static <V> ListenableFuture<V> immediateFuture(@Nullable V value) {
    SettableFuture<V> future = SettableFuture.create();
    future.set(value);
    return future;
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
    SettableFuture<V> future = SettableFuture.create();
    future.set(value);
    return Futures.makeChecked(future, new Function<Exception, X>() {
      @Override
      public X apply(Exception e) {
        throw new AssertionError("impossible");
      }
    });
  }

  /**
   * Returns a {@code ListenableFuture} which has an exception set immediately
   * upon construction.
   *
   * <p>The returned {@code Future} can't be cancelled, and its {@code isDone()}
   * method always returns {@code true}. Calling {@code get()} will immediately
   * throw the provided {@code Throwable} wrapped in an {@code
   * ExecutionException}.
   *
   * @throws Error if the throwable is an {@link Error}.
   */
  public static <V> ListenableFuture<V> immediateFailedFuture(
      Throwable throwable) {
    checkNotNull(throwable);
    SettableFuture<V> future = SettableFuture.create();
    future.setException(throwable);
    return future;
  }

  /**
   * Returns a {@code CheckedFuture} which has an exception set immediately upon
   * construction.
   *
   * <p>The returned {@code Future} can't be cancelled, and its {@code isDone()}
   * method always returns {@code true}. Calling {@code get()} will immediately
   * throw the provided {@code Throwable} wrapped in an {@code
   * ExecutionException}, and calling {@code checkedGet()} will throw the
   * provided exception itself.
   *
   * @throws Error if the throwable is an {@link Error}.
   */
  public static <V, X extends Exception> CheckedFuture<V, X>
      immediateFailedCheckedFuture(final X exception) {
    checkNotNull(exception);
    return makeChecked(Futures.<V>immediateFailedFuture(exception),
        new Function<Exception, X>() {
          @Override
          public X apply(Exception e) {
            return exception;
          }
        });
  }

  /**
   * <p>Returns a new {@code ListenableFuture} whose result is asynchronously
   * derived from the result of the given {@code Future}. More precisely, the
   * returned {@code Future} takes its result from a {@code Future} produced by
   * applying the given {@code Function} to the result of the original {@code
   * Future}. Example:
   *
   * <pre>   {@code
   *   ListenableFuture<RowKey> rowKeyFuture = indexService.lookUp(query);
   *   Function<RowKey, ListenableFuture<QueryResult>> queryFunction =
   *       new Function<RowKey, ListenableFuture<QueryResult>>() {
   *         public ListenableFuture<QueryResult> apply(RowKey rowKey) {
   *           return dataService.read(rowKey);
   *         }
   *       };
   *   ListenableFuture<QueryResult> queryFuture =
   *       chain(rowKeyFuture, queryFunction);
   * }</pre>
   *
   * <p>Note: This overload of {@code chain} is designed for cases in which the
   * work of creating the derived future is fast and lightweight, as the method
   * does not accept an {@code Executor} in which to perform the the work. For
   * heavier derivations, this overload carries some caveats: First, the thread
   * that the derivation runs in depends on whether the input {@code Future} is
   * done at the time {@code chain} is called. In particular, if called late,
   * {@code chain} will run the derivation in the thread that called {@code
   * chain}.  Second, derivations may run in an internal thread of the system
   * responsible for the input {@code Future}, such as an RPC network thread.
   * Finally, during the execution of a {@code sameThreadExecutor} {@code
   * chain} function, all other registered but unexecuted listeners are
   * prevented from running, even if those listeners are to run in other
   * executors.
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in
   * sync with that of the input future and that of the future returned by the
   * chain function. That is, if the returned {@code Future} is cancelled, it
   * will attempt to cancel the other two, and if either of the other two is
   * cancelled, the returned {@code Future} will receive a callback in which it
   * will attempt to cancel itself.
   *
   * @param input The future to chain
   * @param function A function to chain the results of the provided future
   *     to the results of the returned future.  This will be run in the thread
   *     that notifies input it is complete.
   * @return A future that holds result of the chain.
   * @deprecated Convert your {@code Function} to a {@code AsyncFunction}, and
   *     use {@link #transform(ListenableFuture, AsyncFunction)}. This method is
   *     scheduled to be removed from Guava in Guava release 12.0.
   */
  @Deprecated
  public static <I, O> ListenableFuture<O> chain(
      ListenableFuture<I> input,
      Function<? super I, ? extends ListenableFuture<? extends O>> function) {
    return chain(input, function, MoreExecutors.sameThreadExecutor());
  }

  /**
   * <p>Returns a new {@code ListenableFuture} whose result is asynchronously
   * derived from the result of the given {@code Future}. More precisely, the
   * returned {@code Future} takes its result from a {@code Future} produced by
   * applying the given {@code Function} to the result of the original {@code
   * Future}. Example:
   *
   * <pre>   {@code
   *   ListenableFuture<RowKey> rowKeyFuture = indexService.lookUp(query);
   *   Function<RowKey, ListenableFuture<QueryResult>> queryFunction =
   *       new Function<RowKey, ListenableFuture<QueryResult>>() {
   *         public ListenableFuture<QueryResult> apply(RowKey rowKey) {
   *           return dataService.read(rowKey);
   *         }
   *       };
   *   ListenableFuture<QueryResult> queryFuture =
   *       chain(rowKeyFuture, queryFunction, executor);
   * }</pre>
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in
   * sync with that of the input future and that of the future returned by the
   * chain function. That is, if the returned {@code Future} is cancelled, it
   * will attempt to cancel the other two, and if either of the other two is
   * cancelled, the returned {@code Future} will receive a callback in which it
   * will attempt to cancel itself.
   *
   * <p>Note: For cases in which the work of creating the derived future is
   * fast and lightweight, consider {@linkplain Futures#chain(ListenableFuture,
   * Function) the other overload} or explicit use of {@code
   * sameThreadExecutor}. For heavier derivations, this choice carries some
   * caveats: First, the thread that the derivation runs in depends on whether
   * the input {@code Future} is done at the time {@code chain} is called. In
   * particular, if called late, {@code chain} will run the derivation in the
   * thread that called {@code chain}. Second, derivations may run in an
   * internal thread of the system responsible for the input {@code Future},
   * such as an RPC network thread. Finally, during the execution of a {@code
   * sameThreadExecutor} {@code chain} function, all other registered but
   * unexecuted listeners are prevented from running, even if those listeners
   * are to run in other executors.
   *
   * @param input The future to chain
   * @param function A function to chain the results of the provided future
   *     to the results of the returned future.
   * @param executor Executor to run the function in.
   * @return A future that holds result of the chain.
   * @deprecated Convert your {@code Function} to a {@code AsyncFunction}, and
   *     use {@link #transform(ListenableFuture, AsyncFunction, Executor)}. This
   *     method is scheduled to be removed from Guava in Guava release 12.0.
   */
  @Deprecated
  public static <I, O> ListenableFuture<O> chain(ListenableFuture<I> input,
      final Function<? super I, ? extends ListenableFuture<? extends O>>
          function,
      Executor executor) {
    checkNotNull(function);
    ChainingListenableFuture<I, O> chain =
        new ChainingListenableFuture<I, O>(new AsyncFunction<I, O>() {
          @Override
          /*
           * All methods of ListenableFuture are covariant, and we don't expose
           * the object anywhere that would allow it to be downcast.
           */
          @SuppressWarnings("unchecked")
          public ListenableFuture<O> apply(I input) {
            return (ListenableFuture) function.apply(input);
          }
        }, input);
    input.addListener(chain, executor);
    return chain;
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
   *       transform(rowKeyFuture, queryFunction);
   * }</pre>
   *
   * <p>Note: This overload of {@code transform} is designed for cases in which
   * the work of creating the derived {@code Future} is fast and lightweight,
   * as the method does not accept an {@code Executor} in which to perform the
   * the work. (The created {@code Future} itself need not complete quickly.)
   * For heavier operations, this overload carries some caveats: First, the
   * thread that {@code function.apply} runs in depends on whether the input
   * {@code Future} is done at the time {@code transform} is called. In
   * particular, if called late, {@code transform} will run the operation in
   * the thread that called {@code transform}.  Second, {@code function.apply}
   * may run in an internal thread of the system responsible for the input
   * {@code Future}, such as an RPC network thread.  Finally, during the
   * execution of a {@code sameThreadExecutor} {@code function.apply}, all
   * other registered but unexecuted listeners are prevented from running, even
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
   *       transform(rowKeyFuture, queryFunction, executor);
   * }</pre>
   *
   * <p>The returned {@code Future} attempts to keep its cancellation state in
   * sync with that of the input future and that of the future returned by the
   * chain function. That is, if the returned {@code Future} is cancelled, it
   * will attempt to cancel the other two, and if either of the other two is
   * cancelled, the returned {@code Future} will receive a callback in which it
   * will attempt to cancel itself.
   *
   * <p>Note: For cases in which the work of creating the derived future is
   * fast and lightweight, consider {@linkplain
   * Futures#transform(ListenableFuture, Function) the other overload} or
   * explicit use of {@code sameThreadExecutor}. For heavier derivations, this
   * choice carries some caveats: First, the thread that {@code function.apply}
   * runs in depends on whether the input {@code Future} is done at the time
   * {@code transform} is called. In particular, if called late, {@code
   * transform} will run the operation in the thread that called {@code
   * transform}.  Second, {@code function.apply} may run in an internal thread
   * of the system responsible for the input {@code Future}, such as an RPC
   * network thread.  Finally, during the execution of a {@code
   * sameThreadExecutor} {@code function.apply}, all other registered but
   * unexecuted listeners are prevented from running, even if those listeners
   * are to run in other executors.
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
   *       transform(queryFuture, rowsFunction);
   * }</pre>
   *
   * <p>Note: This overload of {@code transform} is designed for cases in which
   * the transformation is fast and lightweight, as the method does not accept
   * an {@code Executor} in which to perform the the work. For heavier
   * transformations, this overload carries some caveats: First, the thread
   * that the transformation runs in depends on whether the input {@code
   * Future} is done at the time {@code transform} is called. In particular, if
   * called late, {@code transform} will perform the transformation in the
   * thread that called {@code transform}. Second, transformations may run in
   * an internal thread of the system responsible for the input {@code Future},
   * such as an RPC network thread. Finally, during the execution of a {@code
   * sameThreadExecutor} transformation, all other registered but unexecuted
   * listeners are prevented from running, even if those listeners are to run
   * in other executors.
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
   * @param future The future to transform
   * @param function A Function to transform the results of the provided future
   *     to the results of the returned future.  This will be run in the thread
   *     that notifies input it is complete.
   * @return A future that holds result of the transformation.
   * @since 9.0 (in 1.0 as {@code compose})
   */
  public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> future,
      final Function<? super I, ? extends O> function) {
    return transform(future, function, MoreExecutors.sameThreadExecutor());
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
   *       transform(queryFuture, rowsFunction, executor);
   * }</pre>
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
   * <p>Note: For cases in which the transformation is fast and lightweight,
   * consider {@linkplain Futures#transform(ListenableFuture, Function) the
   * other overload} or explicit use of {@link
   * MoreExecutors#sameThreadExecutor}. For heavier transformations, this
   * choice carries some caveats: First, the thread that the transformation
   * runs in depends on whether the input {@code Future} is done at the time
   * {@code transform} is called. In particular, if called late, {@code
   * transform} will perform the transformation in the thread that called
   * {@code transform}.  Second, transformations may run in an internal thread
   * of the system responsible for the input {@code Future}, such as an RPC
   * network thread.  Finally, during the execution of a {@code
   * sameThreadExecutor} transformation, all other registered but unexecuted
   * listeners are prevented from running, even if those listeners are to run
   * in other executors.
   *
   * @param future The future to transform
   * @param function A Function to transform the results of the provided future
   *     to the results of the returned future.
   * @param executor Executor to run the function in.
   * @return A future that holds result of the transformation.
   * @since 9.0 (in 2.0 as {@code compose})
   */
  public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> future,
      final Function<? super I, ? extends O> function, Executor executor) {
    checkNotNull(function);
    Function<I, ListenableFuture<O>> wrapperFunction
        = new Function<I, ListenableFuture<O>>() {
            @Override public ListenableFuture<O> apply(I input) {
              O output = function.apply(input);
              return immediateFuture(output);
            }
        };
    return chain(future, wrapperFunction, executor);
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
   * @param future The future to transform
   * @param function A Function to transform the results of the provided future
   *     to the results of the returned future.
   * @return A future that returns the result of the transformation.
   * @since 10.0
   */
  @Beta
  public static <I, O> Future<O> lazyTransform(final Future<I> future,
      final Function<? super I, ? extends O> function) {
    checkNotNull(future);
    checkNotNull(function);
    return new Future<O>() {

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
      }

      @Override
      public boolean isCancelled() {
        return future.isCancelled();
      }

      @Override
      public boolean isDone() {
        return future.isDone();
      }

      @Override
      public O get() throws InterruptedException, ExecutionException {
        return applyTransformation(future.get());
      }

      @Override
      public O get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
        return applyTransformation(future.get(timeout, unit));
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
   * <p>If the function throws any checked exceptions, they should be wrapped
   * in a {@code UndeclaredThrowableException} so that this class can get
   * access to the cause.
   */
  private static class ChainingListenableFuture<I, O>
      extends AbstractFuture<O> implements Runnable {

    private AsyncFunction<? super I, ? extends O> function;
    private ListenableFuture<? extends I> inputFuture;
    private volatile ListenableFuture<? extends O> outputFuture;
    private final BlockingQueue<Boolean> mayInterruptIfRunningChannel =
        new LinkedBlockingQueue<Boolean>(1);
    private final CountDownLatch outputCreated = new CountDownLatch(1);

    private ChainingListenableFuture(
        AsyncFunction<? super I, ? extends O> function,
        ListenableFuture<? extends I> inputFuture) {
      this.function = checkNotNull(function);
      this.inputFuture = checkNotNull(inputFuture);
    }

    /**
     * Delegate the get() to the input and output futures, in case
     * their implementations defer starting computation until their
     * own get() is invoked.
     */
    @Override
    public O get() throws InterruptedException, ExecutionException {
      if (!isDone()) {
        // Invoking get on the inputFuture will ensure our own run()
        // method below is invoked as a listener when inputFuture sets
        // its value.  Therefore when get() returns we should then see
        // the outputFuture be created.
        ListenableFuture<? extends I> inputFuture = this.inputFuture;
        if (inputFuture != null) {
          inputFuture.get();
        }

        // If our listener was scheduled to run on an executor we may
        // need to wait for our listener to finish running before the
        // outputFuture has been constructed by the function.
        outputCreated.await();

        // Like above with the inputFuture, we have a listener on
        // the outputFuture that will set our own value when its
        // value is set.  Invoking get will ensure the output can
        // complete and invoke our listener, so that we can later
        // get the result.
        ListenableFuture<? extends O> outputFuture = this.outputFuture;
        if (outputFuture != null) {
          outputFuture.get();
        }
      }
      return super.get();
    }

    /**
     * Delegate the get() to the input and output futures, in case
     * their implementations defer starting computation until their
     * own get() is invoked.
     */
    @Override
    public O get(long timeout, TimeUnit unit) throws TimeoutException,
        ExecutionException, InterruptedException {
      if (!isDone()) {
        // Use a single time unit so we can decrease remaining timeout
        // as we wait for various phases to complete.
        if (unit != NANOSECONDS) {
          timeout = NANOSECONDS.convert(timeout, unit);
          unit = NANOSECONDS;
        }

        // Invoking get on the inputFuture will ensure our own run()
        // method below is invoked as a listener when inputFuture sets
        // its value.  Therefore when get() returns we should then see
        // the outputFuture be created.
        ListenableFuture<? extends I> inputFuture = this.inputFuture;
        if (inputFuture != null) {
          long start = System.nanoTime();
          inputFuture.get(timeout, unit);
          timeout -= Math.max(0, System.nanoTime() - start);
        }

        // If our listener was scheduled to run on an executor we may
        // need to wait for our listener to finish running before the
        // outputFuture has been constructed by the function.
        long start = System.nanoTime();
        if (!outputCreated.await(timeout, unit)) {
          throw new TimeoutException();
        }
        timeout -= Math.max(0, System.nanoTime() - start);

        // Like above with the inputFuture, we have a listener on
        // the outputFuture that will set our own value when its
        // value is set.  Invoking get will ensure the output can
        // complete and invoke our listener, so that we can later
        // get the result.
        ListenableFuture<? extends O> outputFuture = this.outputFuture;
        if (outputFuture != null) {
          outputFuture.get(timeout, unit);
        }
      }
      return super.get(timeout, unit);
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
        putUninterruptibly(mayInterruptIfRunningChannel, mayInterruptIfRunning);
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
            function.apply(sourceResult);
        if (isCancelled()) {
          // Handles the case where cancel was called while the function was
          // being applied.
          // There is a gap in cancel(boolean) between calling sync.cancel()
          // and storing the value of mayInterruptIfRunning, so this thread
          // needs to block, waiting for that value.
          outputFuture.cancel(
              takeUninterruptibly(mayInterruptIfRunningChannel));
          this.outputFuture = null;
          return;
        }
        outputFuture.addListener(new Runnable() {
            @Override
            public void run() {
              try {
                // Here it would have been nice to have had an
                // UninterruptibleListenableFuture, but we don't want to start a
                // combinatorial explosion of interfaces, so we have to make do.
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
      } catch (Exception e) {
        // This exception is irrelevant in this thread, but useful for the
        // client
        setException(e);
      } catch (Error e) {
        // Propagate errors up ASAP - our superclass will rethrow the error
        setException(e);
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
   * Creates a new {@code ListenableFuture} whose value is a list containing the
   * values of all its input futures, if all succeed. If any input fails, the
   * returned future fails.
   *
   * <p>The list of results is in the same order as the input list.
   *
   * <p>Canceling this future does not cancel any of the component futures;
   * however, if any of the provided futures fails or is canceled, this one is,
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
    return new ListFuture<V>(ImmutableList.copyOf(futures), true,
        MoreExecutors.sameThreadExecutor());
  }

  /**
   * Creates a new {@code ListenableFuture} whose value is a list containing the
   * values of all its input futures, if all succeed. If any input fails, the
   * returned future fails.
   *
   * <p>The list of results is in the same order as the input list.
   *
   * <p>Canceling this future does not cancel any of the component futures;
   * however, if any of the provided futures fails or is canceled, this one is,
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
    return new ListFuture<V>(ImmutableList.copyOf(futures), true,
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
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component
   *         futures
   * @since 10.0
   */
  @Beta
  public static <V> ListenableFuture<List<V>> successfulAsList(
      ListenableFuture<? extends V>... futures) {
    return new ListFuture<V>(ImmutableList.copyOf(futures), false,
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
   * @param futures futures to combine
   * @return a future that provides a list of the results of the component
   *         futures
   * @since 10.0
   */
  @Beta
  public static <V> ListenableFuture<List<V>> successfulAsList(
      Iterable<? extends ListenableFuture<? extends V>> futures) {
    return new ListFuture<V>(ImmutableList.copyOf(futures), false,
        MoreExecutors.sameThreadExecutor());
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
   * <p>Note: This overload of {@code addCallback} is designed for cases in
   * which the callack is fast and lightweight, as the method does not accept
   * an {@code Executor} in which to perform the the work. For heavier
   * callbacks, this overload carries some caveats: First, the thread that the
   * callback runs in depends on whether the input {@code Future} is done at the
   * time {@code addCallback} is called and on whether the input {@code Future}
   * is ever cancelled. In particular, {@code addCallback} may execute the
   * callback in the thread that calls {@code addCallback} or {@code
   * Future.cancel}. Second, callbacks may run in an internal thread of the
   * system responsible for the input {@code Future}, such as an RPC network
   * thread. Finally, during the execution of a {@code sameThreadExecutor}
   * callback, all other registered but unexecuted listeners are prevented from
   * running, even if those listeners are to run in other executors.
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
   * addCallback(future, e,
   *     new FutureCallback<QueryResult> {
   *       public void onSuccess(QueryResult result) {
   *         storeInCache(result);
   *       }
   *       public void onFailure(Throwable t) {
   *         reportError(t);
   *       }
   *     });}</pre>
   *
   * When the callback is fast and lightweight consider {@linkplain
   * Futures#addCallback(ListenableFuture, FutureCallback) the other overload}
   * or explicit use of {@link MoreExecutors#sameThreadExecutor
   * sameThreadExecutor}. For heavier callbacks, this choice carries some
   * caveats: First, the thread that the callback runs in depends on whether
   * the input {@code Future} is done at the time {@code addCallback} is called
   * and on whether the input {@code Future} is ever cancelled. In particular,
   * {@code addCallback} may execute the callback in the thread that calls
   * {@code addCallback} or {@code Future.cancel}. Second, callbacks may run in
   * an internal thread of the system responsible for the input {@code Future},
   * such as an RPC network thread. Finally, during the execution of a {@code
   * sameThreadExecutor} callback, all other registered but unexecuted
   * listeners are prevented from running, even if those listeners are to run
   * in other executors.
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
        try {
          // TODO(user): (Before Guava release), validate that this
          // is the thing for IE.
          V value = getUninterruptibly(future);
          callback.onSuccess(value);
        } catch (ExecutionException e) {
          callback.onFailure(e.getCause());
        } catch (RuntimeException e) {
          callback.onFailure(e);
        } catch (Error e) {
          callback.onFailure(e);
        }
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
   * The overall principle is to continue to treat every checked exception as a
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
  @Beta
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
   * The overall principle is to continue to treat every checked exception as a
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
  @Beta
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
   * The overall principle is to eliminate all checked exceptions: to loop to
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
  @Beta
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

  /**
   * Class that implements {@link #allAsList} and {@link #successfulAsList}.
   * The idea is to create a (null-filled) List and register a listener with
   * each component future to fill out the value in the List when that future
   * completes.
   */
  private static class ListFuture<V> extends AbstractFuture<List<V>> {
    ImmutableList<? extends ListenableFuture<? extends V>> futures;
    final boolean allMustSucceed;
    final AtomicInteger remaining;
    List<V> values;

    /**
     * Constructor.
     *
     * @param futures all the futures to build the list from
     * @param allMustSucceed whether a single failure or cancellation should
     *        propagate to this future
     * @param listenerExecutor used to run listeners on all the passed in
     *        futures.
     */
    ListFuture(
        final ImmutableList<? extends ListenableFuture<? extends V>> futures,
        final boolean allMustSucceed, final Executor listenerExecutor) {
      this.futures = futures;
      this.values = Lists.newArrayListWithCapacity(futures.size());
      this.allMustSucceed = allMustSucceed;
      this.remaining = new AtomicInteger(futures.size());

      init(listenerExecutor);
    }

    private void init(final Executor listenerExecutor) {
      // First, schedule cleanup to execute when the Future is done.
      addListener(new Runnable() {
        @Override
        public void run() {
          // By now the values array has either been set as the Future's value,
          // or (in case of failure) is no longer useful.
          ListFuture.this.values = null;

          // Let go of the memory held by other futures
          ListFuture.this.futures = null;
        }
      }, MoreExecutors.sameThreadExecutor());

      // Now begin the "real" initialization.

      // Corner case: List is empty.
      if (futures.isEmpty()) {
        set(Lists.newArrayList(values));
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
      // We store a reference to futures to avoid the NPE.
      ImmutableList<? extends ListenableFuture<? extends V>> localFutures = futures;
      for (int i = 0; i < localFutures.size(); i++) {
        final ListenableFuture<? extends V> listenable = localFutures.get(i);
        final int index = i;
        listenable.addListener(new Runnable() {
          @Override
          public void run() {
            setOneValue(index, listenable);
          }
        }, listenerExecutor);
      }
    }

    /**
     * Sets the value at the given index to that of the given future.
     */
    private void setOneValue(int index, Future<? extends V> future) {
      List<V> localValues = values;
      if (isDone() || localValues == null) {
        // Some other future failed or has been cancelled, causing this one to
        // also be cancelled or have an exception set. This should only happen
        // if allMustSucceed is true.
        checkState(allMustSucceed,
            "Future was done before all dependencies completed");
        return;
      }

      try {
        checkState(future.isDone(),
            "Tried to set value from future which is not done");
        localValues.set(index, getUninterruptibly(future));
      } catch (CancellationException e) {
        if (allMustSucceed) {
          // Set ourselves as cancelled. Let the input futures keep running
          // as some of them may be used elsewhere.
          // (Currently we don't override interruptTask, so
          // mayInterruptIfRunning==false isn't technically necessary.)
          cancel(false);
        }
      } catch (ExecutionException e) {
        if (allMustSucceed) {
          // As soon as the first one fails, throw the exception up.
          // The result of all other inputs is then ignored.
          setException(e.getCause());
        }
      } catch (RuntimeException e) {
        if (allMustSucceed) {
          setException(e);
        }
      } catch (Error e) {
        // Propagate errors up ASAP - our superclass will rethrow the error
        setException(e);
      } finally {
        int newRemaining = remaining.decrementAndGet();
        checkState(newRemaining >= 0, "Less than 0 remaining futures");
        if (newRemaining == 0) {
          localValues = values;
          if (localValues != null) {
            set(Lists.newArrayList(localValues));
          } else {
            checkState(isDone());
          }
        }
      }
    }

    @Override
    public List<V> get() throws InterruptedException, ExecutionException {
      callAllGets();

      // This may still block in spite of the calls above, as the listeners may
      // be scheduled for execution in other threads.
      return super.get();
    }

    /**
     * Calls the get method of all dependency futures to work around a bug in
     * some ListenableFutures where the listeners aren't called until get() is
     * called.
     */
    private void callAllGets() throws InterruptedException {
      List<? extends ListenableFuture<? extends V>> oldFutures = futures;
      if (oldFutures != null && !isDone()) {
        for (ListenableFuture<? extends V> future : oldFutures) {
          // We wait for a little while for the future, but if it's not done,
          // we check that no other futures caused a cancellation or failure.
          // This can introduce a delay of up to 10ms in reporting an exception.
          while (!future.isDone()) {
            try {
              future.get();
            } catch (Error e) {
              throw e;
            } catch (InterruptedException e) {
              throw e;
            } catch (Throwable e) {
              // ExecutionException / CancellationException / RuntimeException
              if (allMustSucceed) {
                return;
              } else {
                continue;
              }
            }
          }
        }
      }
    }
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
