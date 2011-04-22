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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to the {@link Future} interface.
 *
 * @author Kevin Bourrillion
 * @author Nishant Thakkar
 * @author Sven Mawson
 * @since Guava release 01
 */
@Beta
public final class Futures {
  private Futures() {}

  /**
   * Returns an uninterruptible view of a {@code Future}. If a thread is
   * interrupted during an attempt to {@code get()} from the returned future, it
   * continues to wait on the result until it is available or the timeout
   * elapses, and only then re-interrupts the thread.
   */
  public static <V> UninterruptibleFuture<V> makeUninterruptible(
      final Future<V> future) {
    checkNotNull(future);
    if (future instanceof UninterruptibleFuture<?>) {
      return (UninterruptibleFuture<V>) future;
    }
    return new UninterruptibleFuture<V>() {
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
      public V get(long timeout, TimeUnit unit)
          throws TimeoutException, ExecutionException {
        boolean interrupted = false;
        try {
          long remainingNanos = unit.toNanos(timeout);
          long end = System.nanoTime() + remainingNanos;

          while (true) {
            try {
              // Future treats negative timeouts just like zero.
              return future.get(remainingNanos, NANOSECONDS);
            } catch (InterruptedException e) {
              interrupted = true;
              remainingNanos = end - System.nanoTime();
            }
          }
        } finally {
          if (interrupted) {
            Thread.currentThread().interrupt();
          }
        }
      }

      @Override
      public V get() throws ExecutionException {
        boolean interrupted = false;
        try {
          while (true) {
            try {
              return future.get();
            } catch (InterruptedException ignored) {
              interrupted = true;
            }
          }
        } finally {
          if (interrupted) {
            Thread.currentThread().interrupt();
          }
        }
      }
    };
  }

  /**
   *
   * <p>Creates a {@link ListenableFuture} out of a normal {@link Future}. The
   * returned future will create a thread to wait for the source future to
   * complete before executing the listeners.
   *
   * <p><b>Warning:</b> If the input future does not already implement {@link
   * ListenableFuture}, the returned future will emulate {@link
   * ListenableFuture#addListener} by taking a thread from an internal,
   * unbounded pool at the first call to {@code addListener} and holding it
   * until the future is {@linkplain Future#isDone() done}.
   *
   * <p>Callers who have a future that subclasses
   * {@link java.util.concurrent.FutureTask} may want to instead subclass
   * {@link ListenableFutureTask}, which adds the {@link ListenableFuture}
   * functionality to the standard {@code FutureTask} implementation.
   */
  public static <V> ListenableFuture<V> makeListenable(
      Future<V> future) {
    if (future instanceof ListenableFuture<?>) {
      return (ListenableFuture<V>) future;
    }
    return new ListenableFutureAdapter<V>(future);
  }

  static <V> ListenableFuture<V> makeListenable(
      Future<V> future, Executor executor) {
    checkNotNull(executor);
    if (future instanceof ListenableFuture<?>) {
      return (ListenableFuture<V>) future;
    }
    return new ListenableFutureAdapter<V>(future, executor);
  }

  /**
   * Creates a {@link CheckedFuture} out of a normal {@link Future} and a
   * {@link Function} that maps from {@link Exception} instances into the
   * appropriate checked type.
   *
   * <p><b>Warning:</b> If the input future does not implement {@link
   * ListenableFuture}, the returned future will emulate {@link
   * ListenableFuture#addListener} by taking a thread from an internal,
   * unbounded pool at the first call to {@code addListener} and holding it
   * until the future is {@linkplain Future#isDone() done}.
   *
   * <p>The given mapping function will be applied to an
   * {@link InterruptedException}, a {@link CancellationException}, or an
   * {@link ExecutionException} with the actual cause of the exception.
   * See {@link Future#get()} for details on the exceptions thrown.
   */
  public static <V, X extends Exception> CheckedFuture<V, X> makeChecked(
      Future<V> future, Function<Exception, X> mapper) {
    return new MappingCheckedFuture<V, X>(makeListenable(future), mapper);
  }

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
   * @since Guava release 09 (source-compatible since release 01)
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
   * Returns a new {@code ListenableFuture} whose result is asynchronously
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
   *       chain(queryFuture, queryFunction);
   * }</pre>
   *
   * <p>Successful cancellation of either the input future or the result of
   * function application will cause the returned future to be cancelled.
   * Cancelling the returned future will succeed if it is currently running.
   * In this case, attempts will be made to cancel the input future and the
   * result of the function, however there is no guarantee of success.
   *
   * <p>TODO: Add a version that accepts a normal {@code Future}
   *
   * <p>The typical use for this method would be when a RPC call is dependent on
   * the results of another RPC.  One would call the first RPC (input), create a
   * function that calls another RPC based on input's result, and then call
   * chain on input and that function to get a {@code ListenableFuture} of
   * the result.
   *
   * @param input The future to chain
   * @param function A function to chain the results of the provided future
   *     to the results of the returned future.  This will be run in the thread
   *     that notifies input it is complete.
   * @return A future that holds result of the chain.
   */
  public static <I, O> ListenableFuture<O> chain(ListenableFuture<I> input,
      Function<? super I, ? extends ListenableFuture<? extends O>> function) {
    return chain(input, function, MoreExecutors.sameThreadExecutor());
  }

  /**
   * Returns a new {@code ListenableFuture} whose result is asynchronously
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
   *       chain(queryFuture, queryFunction, executor);
   * }</pre>
   *
   * <p>Successful cancellation of either the input future or the result of
   * function application will cause the returned future to be cancelled.
   * Cancelling the returned future will succeed if it is currently running.
   * In this case, attempts will be made to cancel the input future and the
   * result of the function, however there is no guarantee of success.
   *
   * <p>This version allows an arbitrary executor to be passed in for running
   * the chained Function. When using {@link MoreExecutors#sameThreadExecutor},
   * the thread chained Function executes in will be whichever thread set the
   * result of the input Future, which may be the network thread in the case of
   * RPC-based Futures.
   *
   * @param input The future to chain
   * @param function A function to chain the results of the provided future
   *     to the results of the returned future.
   * @param exec Executor to run the function in.
   * @return A future that holds result of the chain.
   */
  public static <I, O> ListenableFuture<O> chain(ListenableFuture<I> input,
      Function<? super I, ? extends ListenableFuture<? extends O>> function,
      Executor exec) {
    ChainingListenableFuture<I, O> chain =
        new ChainingListenableFuture<I, O>(function, input);
    input.addListener(chain, exec);
    return chain;
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
   * <p>Successful cancellation of the input future will cause the returned
   * future to be cancelled.  Cancelling the returned future will succeed if it
   * is currently running.  In this case, an attempt will be made to cancel the
   * input future, however there is no guarantee of success.
   *
   * <p>An example use of this method is to convert a serializable object
   * returned from an RPC into a POJO.
   *
   * @param future The future to compose
   * @param function A Function to compose the results of the provided future
   *     to the results of the returned future.  This will be run in the thread
   *     that notifies input it is complete.
   * @return A future that holds result of the composition.
   * @since Guava release 09 (in release 01 as {@code compose})
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
   * <p>Successful cancellation of the input future will cause the returned
   * future to be cancelled.  Cancelling the returned future will succeed if it
   * is currently running.  In this case, an attempt will be made to cancel the
   * input future, however there is no guarantee of success.
   *
   * <p>An example use of this method is to convert a serializable object
   * returned from an RPC into a POJO.
   *
   * <p>This version allows an arbitrary executor to be passed in for running
   * the chained Function. When using {@link MoreExecutors#sameThreadExecutor},
   * the thread chained Function executes in will be whichever thread set the
   * result of the input Future, which may be the network thread in the case of
   * RPC-based Futures.
   *
   * @param future The future to compose
   * @param function A Function to compose the results of the provided future
   *     to the results of the returned future.
   * @param exec Executor to run the function in.
   * @return A future that holds result of the composition.
   * @since Guava release 09 (in release 02 as {@code compose})
   */
  public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> future,
      final Function<? super I, ? extends O> function, Executor exec) {
    checkNotNull(function);
    Function<I, ListenableFuture<O>> wrapperFunction
        = new Function<I, ListenableFuture<O>>() {
            @Override public ListenableFuture<O> apply(I input) {
              O output = function.apply(input);
              return immediateFuture(output);
            }
        };
    return chain(future, wrapperFunction, exec);
  }

  /**
   * Returns a new {@code Future} whose result is the product of applying the
   * given {@code Function} to the result of the given {@code Future}. Example:
   *
   * <pre>   {@code
   *   Future<QueryResult> queryFuture = ...;
   *   Function<QueryResult, List<Row>> rowsFunction =
   *       new Function<QueryResult, List<Row>>() {
   *         public List<Row> apply(QueryResult queryResult) {
   *           return queryResult.getRows();
   *         }
   *       };
   *   Future<List<Row>> rowsFuture = transform(queryFuture, rowsFunction);
   * }</pre>
   *
   * <p>Each call to {@code Future<O>.get(*)} results in a call to
   * {@code Future<I>.get(*)}, but {@code function} is only applied once, so it
   * is assumed that {@code Future<I>.get(*)} is idempotent.
   *
   * <p>When calling {@link Future#get(long, TimeUnit)} on the returned
   * future, the timeout only applies to the future passed in to this method.
   * Any additional time taken by applying {@code function} is not considered.
   * (Exception: If the input future is a {@link ListenableFuture}, timeouts
   * will be strictly enforced.)
   *
   * @param future The future to compose
   * @param function A Function to compose the results of the provided future
   *     to the results of the returned future.  This will be run in the thread
   *     that calls one of the varieties of {@code get()}.
   * @return A future that computes result of the composition.
   * @since Guava release 09 (in release 01 as {@code compose})
   */
  public static <I, O> Future<O> transform(final Future<I> future,
      final Function<? super I, ? extends O> function) {
    if (future instanceof ListenableFuture) {
      return transform((ListenableFuture<I>) future, function);
    }
    checkNotNull(future);
    checkNotNull(function);
    return new Future<O>() {

      /*
       * Concurrency detail:
       *
       * <p>To preserve the idempotency of calls to this.get(*) calls to the
       * function are only applied once. A lock is required to prevent multiple
       * applications of the function. The calls to future.get(*) are performed
       * outside the lock, as is required to prevent calls to
       * get(long, TimeUnit) to persist beyond their timeout.
       *
       * <p>Calls to future.get(*) on every call to this.get(*) also provide
       * the cancellation behavior for this.
       *
       * <p>(Consider: in thread A, call get(), in thread B call get(long,
       * TimeUnit). Thread B may have to wait for Thread A to finish, which
       * would be unacceptable.)
       *
       * <p>Note that each call to Future<O>.get(*) results in a call to
       * Future<I>.get(*), but the function is only applied once, so
       * Future<I>.get(*) is assumed to be idempotent.
       */

      private final Object lock = new Object();
      private boolean set = false;
      private O value = null;
      private ExecutionException exception = null;

      @Override
      public O get() throws InterruptedException, ExecutionException {
        return apply(future.get());
      }

      @Override
      public O get(long timeout, TimeUnit unit) throws InterruptedException,
          ExecutionException, TimeoutException {
        return apply(future.get(timeout, unit));
      }

      private O apply(I raw) throws ExecutionException {
        synchronized (lock) {
          if (!set) {
            try {
              value = function.apply(raw);
            } catch (RuntimeException e) {
              exception = new ExecutionException(e);
            } catch (Error e) {
              exception = new ExecutionException(e);
            }
            set = true;
          }

          if (exception != null) {
            throw exception;
          }
          return value;
        }
      }

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
      extends AbstractListenableFuture<O> implements Runnable {

    private Function<? super I, ? extends ListenableFuture<? extends O>>
        function;
    private ListenableFuture<? extends I> inputFuture;
    private volatile ListenableFuture<? extends O> outputFuture;
    private final BlockingQueue<Boolean> mayInterruptIfRunningChannel =
        new LinkedBlockingQueue<Boolean>(1);
    private final CountDownLatch outputCreated = new CountDownLatch(1);

    private ChainingListenableFuture(
        Function<? super I, ? extends ListenableFuture<? extends O>> function,
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
      if (cancel()) {
        try {
          // This should never block since only one thread is allowed to cancel
          // this Future.
          mayInterruptIfRunningChannel.put(mayInterruptIfRunning);
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
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
          sourceResult = makeUninterruptible(inputFuture).get();
        } catch (CancellationException e) {
          // Cancel this future and return.
          cancel();
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
          try {
            // There is a gap in cancel(boolean) between calling cancel() and
            // storing the value of mayInterruptIfRunning, so this thread needs
            // to block, waiting for that value.
            outputFuture.cancel(mayInterruptIfRunningChannel.take());
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
          }
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
                set(makeUninterruptible(outputFuture).get());
              } catch (CancellationException e) {
                // Cancel this future and return.
                cancel();
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
      } catch (RuntimeException e) {
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
   * @since Guava release 10
   */
  @Beta
  public static <T> ListenableFuture<List<T>> allAsList(
      ListenableFuture<? extends T>... futures) {
    return new ListFuture<T>(ImmutableList.copyOf(futures), true,
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
   * @since Guava release 10
   */
  @Beta
  public static <T> ListenableFuture<List<T>> allAsList(
      Iterable<? extends ListenableFuture<? extends T>> futures) {
    return new ListFuture<T>(ImmutableList.copyOf(futures), true,
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
   * @since Guava release 10
   */
  @Beta
  public static <T> ListenableFuture<List<T>> successfulAsList(
      ListenableFuture<? extends T>... futures) {
    return new ListFuture<T>(ImmutableList.copyOf(futures), false,
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
   * @since Guava release 10
   */
  @Beta
  public static <T> ListenableFuture<List<T>> successfulAsList(
      Iterable<? extends ListenableFuture<? extends T>> futures) {
    return new ListFuture<T>(ImmutableList.copyOf(futures), false,
        MoreExecutors.sameThreadExecutor());
  }

  /**
   * Class that implements {@link #allAsList} and {@link #successfulAsList}.
   * The idea is to create a (null-filled) List and register a listener with
   * each component future to fill out the value in the List when that future
   * completes.
   */
  private static class ListFuture<T> extends AbstractListenableFuture<List<T>> {
    ImmutableList<? extends ListenableFuture<? extends T>> futures;
    final boolean allMustSucceed;
    final AtomicInteger remaining;
    List<T> values;

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
        final ImmutableList<? extends ListenableFuture<? extends T>> futures,
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
      ImmutableList<? extends ListenableFuture<? extends T>> localFutures = futures;
      for (int i = 0; i < localFutures.size(); i++) {
        final ListenableFuture<? extends T> listenable = localFutures.get(i);
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
    private void setOneValue(int index, Future<? extends T> future) {
      List<T> localValues = values;
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
        localValues.set(index, makeUninterruptible(future).get());
      } catch (CancellationException e) {
        if (allMustSucceed) {
          // Set ourselves as cancelled. Let the input futures keep running
          // as some of them may be used elsewhere.
          cancel();
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
    public List<T> get() throws InterruptedException, ExecutionException {
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
      List<? extends ListenableFuture<? extends T>> oldFutures = futures;
      if (oldFutures != null && !isDone()) {
        for (ListenableFuture<? extends T> future : oldFutures) {
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

  /**
   * An adapter to turn a {@link Future} into a {@link ListenableFuture}.  This
   * will wait on the future to finish, and when it completes, run the
   * listeners.  This implementation will wait on the source future
   * indefinitely, so if the source future never completes, the adapter will
   * never complete either.
   *
   * <p>If the delegate future is interrupted or throws an unexpected unchecked
   * exception, the listeners will not be invoked.
   */
  private static class ListenableFutureAdapter<V> extends ForwardingFuture<V>
      implements ListenableFuture<V> {

    private static final ThreadFactory threadFactory =
        new ThreadFactoryBuilder()
            .setNameFormat("ListenableFutureAdapter-thread-%d")
            .build();
    private static final Executor defaultAdapterExecutor =
        Executors.newCachedThreadPool(threadFactory);

    private final Executor adapterExecutor;

    // The execution list to hold our listeners.
    private final ExecutionList executionList = new ExecutionList();

    // This allows us to only start up a thread waiting on the delegate future
    // when the first listener is added.
    private final AtomicBoolean hasListeners = new AtomicBoolean(false);

    // The delegate future.
    private final Future<V> delegate;

    ListenableFutureAdapter(Future<V> delegate) {
      this(delegate, defaultAdapterExecutor);
    }

    ListenableFutureAdapter(Future<V> delegate, Executor adapterExecutor) {
      this.delegate = checkNotNull(delegate);
      this.adapterExecutor = checkNotNull(adapterExecutor);
    }

    @Override
    protected Future<V> delegate() {
      return delegate;
    }

    @Override
    public void addListener(Runnable listener, Executor exec) {
      executionList.add(listener, exec);

      // When a listener is first added, we run a task that will wait for
      // the delegate to finish, and when it is done will run the listeners.
      if (hasListeners.compareAndSet(false, true)) {
        if (delegate.isDone()) {
          // If the delegate is already done, run the execution list
          // immediately on the current thread.
          executionList.run();
          return;
        }

        adapterExecutor.execute(new Runnable() {
          @Override
          public void run() {
            try {
              delegate.get();
            } catch (Error e) {
              throw e;
            } catch (InterruptedException e) {
              // This thread was interrupted.  This should never happen, so we
              // throw an IllegalStateException.
              Thread.currentThread().interrupt();
              throw new IllegalStateException("Adapter thread interrupted!", e);
            } catch (Throwable e) {
              // ExecutionException / CancellationException / RuntimeException
              // The task is done, run the listeners.
            }
            executionList.run();
          }
        });
      }
    }
  }
}
