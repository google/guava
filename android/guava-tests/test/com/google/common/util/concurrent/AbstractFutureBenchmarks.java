/*
 * Copyright (C) 2014 The Guava Authors
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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import javax.annotation.CheckForNull;

/** Utilities for the AbstractFutureBenchmarks */
final class AbstractFutureBenchmarks {
  private AbstractFutureBenchmarks() {}

  interface Facade<T> extends ListenableFuture<T> {
    @CanIgnoreReturnValue
    boolean set(T t);

    @CanIgnoreReturnValue
    boolean setException(Throwable t);
  }

  private static class NewAbstractFutureFacade<T> extends AbstractFuture<T> implements Facade<T> {
    @CanIgnoreReturnValue
    @Override
    public boolean set(T t) {
      return super.set(t);
    }

    @CanIgnoreReturnValue
    @Override
    public boolean setException(Throwable t) {
      return super.setException(t);
    }
  }

  private static class OldAbstractFutureFacade<T> extends OldAbstractFuture<T>
      implements Facade<T> {
    @CanIgnoreReturnValue
    @Override
    public boolean set(T t) {
      return super.set(t);
    }

    @CanIgnoreReturnValue
    @Override
    public boolean setException(Throwable t) {
      return super.setException(t);
    }
  }

  enum Impl {
    NEW {
      @Override
      <T> Facade<T> newFacade() {
        return new NewAbstractFutureFacade<T>();
      }
    },
    OLD {
      @Override
      <T> Facade<T> newFacade() {
        return new OldAbstractFutureFacade<T>();
      }
    };

    abstract <T> Facade<T> newFacade();
  }

  static void awaitWaiting(Thread t) {
    while (true) {
      Thread.State state = t.getState();
      switch (state) {
        case RUNNABLE:
        case BLOCKED:
          Thread.yield();
          break;
        case WAITING:
          return;
        default:
          throw new AssertionError("unexpected state: " + state);
      }
    }
  }

  abstract static class OldAbstractFuture<V> implements ListenableFuture<V> {

    /** Synchronization control for AbstractFutures. */
    private final Sync<V> sync = new Sync<V>();

    // The execution list to hold our executors.
    private final ExecutionList executionList = new ExecutionList();

    /** Constructor for use by subclasses. */
    protected OldAbstractFuture() {}

    /*
     * Improve the documentation of when InterruptedException is thrown. Our
     * behavior matches the JDK's, but the JDK's documentation is misleading.
     */
    /**
     * {@inheritDoc}
     *
     * <p>The default {@link AbstractFuture} implementation throws {@code InterruptedException} if
     * the current thread is interrupted before or during the call, even if the value is already
     * available.
     *
     * @throws InterruptedException if the current thread was interrupted before or during the call
     *     (optional but recommended).
     * @throws CancellationException {@inheritDoc}
     */
    @CanIgnoreReturnValue
    @Override
    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, TimeoutException, ExecutionException {
      return sync.get(unit.toNanos(timeout));
    }

    /*
     * Improve the documentation of when InterruptedException is thrown. Our
     * behavior matches the JDK's, but the JDK's documentation is misleading.
     */
    /**
     * {@inheritDoc}
     *
     * <p>The default {@link AbstractFuture} implementation throws {@code InterruptedException} if
     * the current thread is interrupted before or during the call, even if the value is already
     * available.
     *
     * @throws InterruptedException if the current thread was interrupted before or during the call
     *     (optional but recommended).
     * @throws CancellationException {@inheritDoc}
     */
    @CanIgnoreReturnValue
    @Override
    public V get() throws InterruptedException, ExecutionException {
      return sync.get();
    }

    @Override
    public boolean isDone() {
      return sync.isDone();
    }

    @Override
    public boolean isCancelled() {
      return sync.isCancelled();
    }

    @CanIgnoreReturnValue
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      if (!sync.cancel(mayInterruptIfRunning)) {
        return false;
      }
      executionList.execute();
      if (mayInterruptIfRunning) {
        interruptTask();
      }
      return true;
    }

    /**
     * Subclasses can override this method to implement interruption of the future's computation.
     * The method is invoked automatically by a successful call to {@link #cancel(boolean)
     * cancel(true)}.
     *
     * <p>The default implementation does nothing.
     *
     * @since 10.0
     */
    protected void interruptTask() {}

    /**
     * Returns true if this future was cancelled with {@code mayInterruptIfRunning} set to {@code
     * true}.
     *
     * @since 14.0
     */
    protected final boolean wasInterrupted() {
      return sync.wasInterrupted();
    }

    /**
     * {@inheritDoc}
     *
     * @since 10.0
     */
    @Override
    public void addListener(Runnable listener, Executor exec) {
      executionList.add(listener, exec);
    }

    /**
     * Subclasses should invoke this method to set the result of the computation to {@code value}.
     * This will set the state of the future to {@link OldAbstractFuture.Sync#COMPLETED} and invoke
     * the listeners if the state was successfully changed.
     *
     * @param value the value that was the result of the task.
     * @return true if the state was successfully changed.
     */
    @CanIgnoreReturnValue
    protected boolean set(@CheckForNull V value) {
      boolean result = sync.set(value);
      if (result) {
        executionList.execute();
      }
      return result;
    }

    /**
     * Subclasses should invoke this method to set the result of the computation to an error, {@code
     * throwable}. This will set the state of the future to {@link OldAbstractFuture.Sync#COMPLETED}
     * and invoke the listeners if the state was successfully changed.
     *
     * @param throwable the exception that the task failed with.
     * @return true if the state was successfully changed.
     */
    @CanIgnoreReturnValue
    protected boolean setException(Throwable throwable) {
      boolean result = sync.setException(checkNotNull(throwable));
      if (result) {
        executionList.execute();
      }
      return result;
    }

    /**
     * Following the contract of {@link AbstractQueuedSynchronizer} we create a private subclass to
     * hold the synchronizer. This synchronizer is used to implement the blocking and waiting calls
     * as well as to handle state changes in a thread-safe manner. The current state of the future
     * is held in the Sync state, and the lock is released whenever the state changes to {@link
     * #COMPLETED}, {@link #CANCELLED}, or {@link #INTERRUPTED}
     *
     * <p>To avoid races between threads doing release and acquire, we transition to the final state
     * in two steps. One thread will successfully CAS from RUNNING to COMPLETING, that thread will
     * then set the result of the computation, and only then transition to COMPLETED, CANCELLED, or
     * INTERRUPTED.
     *
     * <p>We don't use the integer argument passed between acquire methods so we pass around a -1
     * everywhere.
     */
    static final class Sync<V> extends AbstractQueuedSynchronizer {

      private static final long serialVersionUID = 0L;

      /* Valid states. */
      static final int RUNNING = 0;
      static final int COMPLETING = 1;
      static final int COMPLETED = 2;
      static final int CANCELLED = 4;
      static final int INTERRUPTED = 8;

      private V value;
      private Throwable exception;

      /*
       * Acquisition succeeds if the future is done, otherwise it fails.
       */
      @Override
      protected int tryAcquireShared(int ignored) {
        if (isDone()) {
          return 1;
        }
        return -1;
      }

      /*
       * We always allow a release to go through, this means the state has been
       * successfully changed and the result is available.
       */
      @Override
      protected boolean tryReleaseShared(int finalState) {
        setState(finalState);
        return true;
      }

      /**
       * Blocks until the task is complete or the timeout expires. Throws a {@link TimeoutException}
       * if the timer expires, otherwise behaves like {@link #get()}.
       */
      V get(long nanos)
          throws TimeoutException, CancellationException, ExecutionException, InterruptedException {

        // Attempt to acquire the shared lock with a timeout.
        if (!tryAcquireSharedNanos(-1, nanos)) {
          throw new TimeoutException("Timeout waiting for task.");
        }

        return getValue();
      }

      /**
       * Blocks until {@link #complete(Object, Throwable, int)} has been successfully called. Throws
       * a {@link CancellationException} if the task was cancelled, or a {@link ExecutionException}
       * if the task completed with an error.
       */
      V get() throws CancellationException, ExecutionException, InterruptedException {

        // Acquire the shared lock allowing interruption.
        acquireSharedInterruptibly(-1);
        return getValue();
      }

      /**
       * Implementation of the actual value retrieval. Will return the value on success, an
       * exception on failure, a cancellation on cancellation, or an illegal state if the
       * synchronizer is in an invalid state.
       */
      private V getValue() throws CancellationException, ExecutionException {
        int state = getState();
        switch (state) {
          case COMPLETED:
            if (exception != null) {
              throw new ExecutionException(exception);
            } else {
              return value;
            }

          case CANCELLED:
          case INTERRUPTED:
            throw cancellationExceptionWithCause("Task was cancelled.", exception);

          default:
            throw new IllegalStateException("Error, synchronizer in invalid state: " + state);
        }
      }

      /** Checks if the state is {@link #COMPLETED}, {@link #CANCELLED}, or {@link #INTERRUPTED}. */
      boolean isDone() {
        return (getState() & (COMPLETED | CANCELLED | INTERRUPTED)) != 0;
      }

      /** Checks if the state is {@link #CANCELLED} or {@link #INTERRUPTED}. */
      boolean isCancelled() {
        return (getState() & (CANCELLED | INTERRUPTED)) != 0;
      }

      /** Checks if the state is {@link #INTERRUPTED}. */
      boolean wasInterrupted() {
        return getState() == INTERRUPTED;
      }

      /** Transition to the COMPLETED state and set the value. */
      boolean set(@CheckForNull V v) {
        return complete(v, null, COMPLETED);
      }

      /** Transition to the COMPLETED state and set the exception. */
      boolean setException(Throwable t) {
        return complete(null, t, COMPLETED);
      }

      /** Transition to the CANCELLED or INTERRUPTED state. */
      boolean cancel(boolean interrupt) {
        return complete(null, null, interrupt ? INTERRUPTED : CANCELLED);
      }

      /**
       * Implementation of completing a task. Either {@code v} or {@code t} will be set but not
       * both. The {@code finalState} is the state to change to from {@link #RUNNING}. If the state
       * is not in the RUNNING state we return {@code false} after waiting for the state to be set
       * to a valid final state ({@link #COMPLETED}, {@link #CANCELLED}, or {@link #INTERRUPTED}).
       *
       * @param v the value to set as the result of the computation.
       * @param t the exception to set as the result of the computation.
       * @param finalState the state to transition to.
       */
      private boolean complete(@CheckForNull V v, @CheckForNull Throwable t, int finalState) {
        boolean doCompletion = compareAndSetState(RUNNING, COMPLETING);
        if (doCompletion) {
          // If this thread successfully transitioned to COMPLETING, set the value
          // and exception and then release to the final state.
          this.value = v;
          // Don't actually construct a CancellationException until necessary.
          this.exception =
              ((finalState & (CANCELLED | INTERRUPTED)) != 0)
                  ? new CancellationException("Future.cancel() was called.")
                  : t;
          releaseShared(finalState);
        } else if (getState() == COMPLETING) {
          // If some other thread is currently completing the future, block until
          // they are done so we can guarantee completion.
          acquireShared(-1);
        }
        return doCompletion;
      }
    }

    static final CancellationException cancellationExceptionWithCause(
        @CheckForNull String message, @CheckForNull Throwable cause) {
      CancellationException exception = new CancellationException(message);
      exception.initCause(cause);
      return exception;
    }
  }
}
