/*
 * Copyright (C) 2006 Google Inc.
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

import com.google.common.base.Function;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Static utility methods pertaining to the {@link Future} interface.
 *
 * @author Kevin Bourrillion
 * @author Nishant Thakkar
 * @author Sven Mawson
 * @since 2009.09.15 <b>tentative</b>
 */
public class Futures {
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
    if (future instanceof UninterruptibleFuture) {
      return (UninterruptibleFuture<V>) future;
    }
    return new UninterruptibleFuture<V>() {
      public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
      }
      public boolean isCancelled() {
        return future.isCancelled();
      }
      public boolean isDone() {
        return future.isDone();
      }

      public V get(long timeoutDuration, TimeUnit timeoutUnit)
          throws TimeoutException, ExecutionException {
        boolean interrupted = false;
        try {
          long timeoutNanos = timeoutUnit.toNanos(timeoutDuration);
          long end = System.nanoTime() + timeoutNanos;
          for (long remaining = timeoutNanos; remaining > 0;
              remaining = end - System.nanoTime()) {
            try {
              return future.get(remaining, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ignored) {
              interrupted = true;
            }
          }
          throw new TimeoutException();
        } finally {
          if (interrupted) {
            Thread.currentThread().interrupt();
          }
        }
      }

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
   * Creates a {@link ListenableFuture} out of a normal {@link Future}. The
   * returned future will create a thread to wait for the source future to
   * complete before executing the listeners.
   *
   * <p>Callers who have a future that subclasses
   * {@link java.util.concurrent.FutureTask} may want to instead subclass
   * {@link ListenableFutureTask}, which adds the {@link ListenableFuture}
   * functionality to the standard {@code FutureTask} implementation.
   */
  public static <T> ListenableFuture<T> makeListenable(Future<T> future) {
    if (future instanceof ListenableFuture) {
      return (ListenableFuture<T>) future;
    }
    return new ListenableFutureAdapter<T>(future);
  }

  /**
   * Creates a {@link CheckedFuture} out of a normal {@link Future} and a
   * {@link Function} that maps from {@link Exception} instances into the
   * appropriate checked type.
   *
   * <p>The given mapping function will be applied to an
   * {@link InterruptedException}, a {@link CancellationException}, or an
   * {@link ExecutionException} with the actual cause of the exception.
   * See {@link Future#get()} for details on the exceptions thrown.
   */
  public static <T, E extends Exception> CheckedFuture<T, E> makeChecked(
      Future<T> future, Function<Exception, E> mapper) {
    return new MappingCheckedFuture<T, E>(makeListenable(future), mapper);
  }

  /**
   * Creates a {@code ListenableFuture} which has its value set immediately upon
   * construction. The getters just return the value. This {@code Future} can't
   * be canceled or timed out and its {@code isDone()} method always returns
   * {@code true}. It's useful for returning something that implements the
   * {@code ListenableFuture} interface but already has the result.
   */
  public static <T> ListenableFuture<T> immediateFuture(@Nullable T value) {
    ValueFuture<T> future = ValueFuture.create();
    future.set(value);
    return future;
  }

  /**
   * Creates a {@code CheckedFuture} which has its value set immediately upon
   * construction. The getters just return the value. This {@code Future} can't
   * be canceled or timed out and its {@code isDone()} method always returns
   * {@code true}. It's useful for returning something that implements the
   * {@code CheckedFuture} interface but already has the result.
   */
  public static <T, E extends Exception> CheckedFuture<T, E>
      immediateCheckedFuture(@Nullable T value) {
    ValueFuture<T> future = ValueFuture.create();
    future.set(value);
    return Futures.makeChecked(future, new Function<Exception, E>() {
      public E apply(Exception e) {
        throw new AssertionError("impossible");
      }
    });
  }

  /**
   * Creates a {@code ListenableFuture} which has an exception set immediately
   * upon construction. The getters just return the value. This {@code Future}
   * can't be canceled or timed out and its {@code isDone()} method always
   * returns {@code true}. It's useful for returning something that implements
   * the {@code ListenableFuture} interface but already has a failed
   * result. Calling {@code get()} will throw the provided {@code Throwable}
   * (wrapped in an {@code ExecutionException}).
   *
   * @throws Error if the throwable was an {@link Error}.
   */
  public static <T> ListenableFuture<T> immediateFailedFuture(
      Throwable throwable) {
    checkNotNull(throwable);
    ValueFuture<T> future = ValueFuture.create();
    future.setException(throwable);
    return future;
  }

  /**
   * Creates a {@code CheckedFuture} which has an exception set immediately
   * upon construction. The getters just return the value. This {@code Future}
   * can't be canceled or timed out and its {@code isDone()} method always
   * returns {@code true}. It's useful for returning something that implements
   * the {@code CheckedFuture} interface but already has a failed result.
   * Calling {@code get()} will throw the provided {@code Throwable} (wrapped in
   * an {@code ExecutionException}) and calling {@code checkedGet()} will throw
   * the provided exception itself.
   *
   * @throws Error if the throwable was an {@link Error}.
   */
  public static <T, E extends Exception> CheckedFuture<T, E>
      immediateFailedCheckedFuture(final E exception) {
    checkNotNull(exception);
    return makeChecked(Futures.<T>immediateFailedFuture(exception),
        new Function<Exception, E>() {
          public E apply(Exception e) {
            return exception;
          }
        });
  }

  /**
   * Creates a new {@code ListenableFuture} that wraps another
   * {@code ListenableFuture}.  The result of the new future is the result of
   * the provided function called on the result of the provided future.
   * The resulting future doesn't interrupt when aborted.
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
    return chain(input, function, Executors.sameThreadExecutor());
  }

  /**
   * Creates a new {@code ListenableFuture} that wraps another
   * {@code ListenableFuture}.  The result of the new future is the result of
   * the provided function called on the result of the provided future.
   * The resulting future doesn't interrupt when aborted.
   *
   * <p>This version allows an arbitrary executor to be passed in for running
   * the chained Function. When using {@link Executors#sameThreadExecutor}, the
   * thread chained Function executes in will be whichever thread set the
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
   * Creates a new {@code ListenableFuture} that wraps another
   * {@code ListenableFuture}.  The result of the new future is the result of
   * the provided function called on the result of the provided future.
   * The resulting future doesn't interrupt when aborted.
   *
   * <p>An example use of this method is to convert a serializable object
   * returned from an RPC into a POJO.
   *
   * @param future The future to compose
   * @param function A Function to compose the results of the provided future
   *     to the results of the returned future.  This will be run in the thread
   *     that notifies input it is complete.
   * @return A future that holds result of the composition.
   */
  public static <I, O> ListenableFuture<O> compose(ListenableFuture<I> future,
      final Function<? super I, ? extends O> function) {
    return compose(future, function, Executors.sameThreadExecutor());
  }

  /**
   * Creates a new {@code ListenableFuture} that wraps another
   * {@code ListenableFuture}.  The result of the new future is the result of
   * the provided function called on the result of the provided future.
   * The resulting future doesn't interrupt when aborted.
   *
   * <p>An example use of this method is to convert a serializable object
   * returned from an RPC into a POJO.
   *
   * <p>This version allows an arbitrary executor to be passed in for running
   * the chained Function. When using {@link Executors#sameThreadExecutor}, the
   * thread chained Function executes in will be whichever thread set the result
   * of the input Future, which may be the network thread in the case of
   * RPC-based Futures.
   *
   * @param future The future to compose
   * @param function A Function to compose the results of the provided future
   *     to the results of the returned future.
   * @param exec Executor to run the function in.
   * @return A future that holds result of the composition.
   * @since 2010.01.04 <b>tentative</b>
   */
  public static <I, O> ListenableFuture<O> compose(ListenableFuture<I> future,
      final Function<? super I, ? extends O> function, Executor exec) {
    Function<I, ListenableFuture<O>> wrapperFunction
        = new Function<I, ListenableFuture<O>>() {
            /*@Override*/ public ListenableFuture<O> apply(I input) {
              O output = function.apply(input);
              return immediateFuture(output);
            }
        };
    return chain(future, wrapperFunction, exec);
  }

  /**
   * Creates a new {@code Future} that wraps another {@code Future}.
   * The result of the new future is the result of the provided function called
   * on the result of the provided future.
   *
   * <p>An example use of this method is to convert a Future that produces a
   * handle to an object to a future that produces the object itself.
   *
   * <p>Each call to {@code Future<O>.get(*)} results in a call to
   * {@code Future<I>.get(*)}, but {@code function} is only applied once, so it
   * is assumed that {@code Future<I>.get(*)} is idempotent.
   *
   * <p>When calling {@link Future#get(long, TimeUnit)} on the returned
   * future, the timeout only applies to the future passed in to this method.
   * Any additional time taken by applying {@code function} is not considered.
   *
   * @param future The future to compose
   * @param function A Function to compose the results of the provided future
   *     to the results of the returned future.  This will be run in the thread
   *     that calls one of the varieties of {@code get()}.
   * @return A future that computes result of the composition.
   */
  public static <I, O> Future<O> compose(final Future<I> future,
      final Function<? super I, ? extends O> function) {

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

      /*@Override*/
      public O get() throws InterruptedException, ExecutionException {
        return apply(future.get());
      }

      /*@Override*/
      public O get(long timeout, TimeUnit unit) throws InterruptedException,
          ExecutionException, TimeoutException {
        return apply(future.get(timeout, unit));
      }

      private O apply(I raw) {
        synchronized(lock) {
          if (!set) {
            value = function.apply(raw);
            set = true;
          }
          return value;
        }
      }

      /*@Override*/
      public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
      }

      /*@Override*/
      public boolean isCancelled() {
        return future.isCancelled();
      }

      /*@Override*/
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
   * The resulting future doesn't interrupt when aborted.
   *
   * <p>If the function throws any checked exceptions, they should be wrapped
   * in a {@code UndeclaredThrowableException} so that this class can get
   * access to the cause.
   */
  private static class ChainingListenableFuture<I, O>
      extends AbstractListenableFuture<O> implements Runnable {

    private final Function<? super I, ? extends ListenableFuture<? extends O>>
        function;
    private final UninterruptibleFuture<? extends I> inputFuture;

    private ChainingListenableFuture(
        Function<? super I, ? extends ListenableFuture<? extends O>> function,
        ListenableFuture<? extends I> inputFuture) {
      this.function = function;
      this.inputFuture = makeUninterruptible(inputFuture);
    }

    public void run() {
      try {
        I sourceResult;
        try {
          sourceResult = inputFuture.get();
        } catch (CancellationException e) {
          // Cancel this future and return.
          cancel();
          return;
        } catch (ExecutionException e) {
          // Set the cause of the exception as this future's exception
          setException(e.getCause());
          return;
        }

        final ListenableFuture<? extends O> outputFuture =
            function.apply(sourceResult);
        outputFuture.addListener(new Runnable() {
            public void run() {
              try {
                // Here it would have been nice to have had an
                // UninterruptibleListenableFuture, but we don't want to start a
                // combinatorial explosion of interfaces, so we have to make do.
                set(makeUninterruptible(outputFuture).get());
              } catch (ExecutionException e) {
                // Set the cause of the exception as this future's exception
                setException(e.getCause());
              }
            }
          }, Executors.sameThreadExecutor());
      } catch (UndeclaredThrowableException e) {
        // Set the cause of the exception as this future's exception
        setException(e.getCause());
      } catch (RuntimeException e) {
        // This exception is irrelevant in this thread, but useful for the
        // client
        setException(e);
      } catch (Error e) {
        // This seems evil, but the client needs to know an error occured and
        // the error needs to be propagated ASAP.
        setException(e);
        throw e;
      }
    }
  }

  /**
   * A checked future that uses a function to map from exceptions to the
   * appropriate checked type.
   */
  private static class MappingCheckedFuture<T, E extends Exception> extends
      AbstractCheckedFuture<T, E> {

    final Function<Exception, E> mapper;

    MappingCheckedFuture(ListenableFuture<T> delegate,
        Function<Exception, E> mapper) {
      super(delegate);

      this.mapper = mapper;
    }

    @Override
    protected E mapException(Exception e) {
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
  private static class ListenableFutureAdapter<T> extends ForwardingFuture<T>
      implements ListenableFuture<T> {

    private static final Executor adapterExecutor =
        java.util.concurrent.Executors.newCachedThreadPool();

    // The execution list to hold our listeners.
    private final ExecutionList executionList = new ExecutionList();

    // This allows us to only start up a thread waiting on the delegate future
    // when the first listener is added.
    private final AtomicBoolean hasListeners = new AtomicBoolean(false);

    // The delegate future.
    private final Future<T> delegate;

    ListenableFutureAdapter(final Future<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    protected Future<T> delegate() {
      return delegate;
    }

    /*@Override*/
    public void addListener(Runnable listener, Executor exec) {

      // When a listener is first added, we run a task that will wait for
      // the delegate to finish, and when it is done will run the listeners.
      if (!hasListeners.get() && hasListeners.compareAndSet(false, true)) {
        adapterExecutor.execute(new Runnable() {
          /*@Override*/
          public void run() {
            try {
              delegate.get();
            } catch (CancellationException e) {
              // The task was cancelled, so it is done, run the listeners.
            } catch (InterruptedException e) {
              // This thread was interrupted.  This should never happen, so we
              // throw an IllegalStateException.
              throw new IllegalStateException("Adapter thread interrupted!", e);
            } catch (ExecutionException e) {
              // The task caused an exception, so it is done, run the listeners.
            }
            executionList.run();
          }
        });
      }
      executionList.add(listener, exec);
    }
  }
}
