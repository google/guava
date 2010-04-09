/*
 * Copyright (C) 2009 Google Inc.
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
import com.google.common.base.Service;
import com.google.common.base.Service.State; // javadoc needs this
import com.google.common.base.Throwables;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for implementing services that can handle {@link #doStart} and
 * {@link #doStop} requests, responding to them with {@link #notifyStarted()}
 * and {@link #notifyStopped()} callbacks. Its subclasses must manage threads
 * manually; consider {@link AbstractExecutionThreadService} if you need only a
 * single execution thread.
 *
 * @author Jesse Wilson
 * @since 2009.09.15 <b>tentative</b>
 */
public abstract class AbstractService implements Service {

  private final ReentrantLock lock = new ReentrantLock();

  private final Transition startup = new Transition();
  private final Transition shutdown = new Transition();

  /**
   * The internal state, which equals external state unless
   * shutdownWhenStartupFinishes is true. Guarded by {@code lock}.
   */
  private State state = State.NEW;

  /**
   * If true, the user requested a shutdown while the service was still starting
   * up. Guarded by {@code lock}.
   */
  private boolean shutdownWhenStartupFinishes = false;

  /**
   * This method is called by {@link #start} to initiate service startup. The
   * invocation of this method should cause a call to {@link #notifyStarted()},
   * either during this method's run, or after it has returned. If startup
   * fails, the invocation should cause a call to {@link
   * #notifyFailed(Throwable)} instead.
   *
   * <p>This method should return promptly; prefer to do work on a different
   * thread where it is convenient. It is invoked exactly once on service
   * startup, even when {@link #start} is called multiple times.
   */
  protected abstract void doStart();

  /**
   * This method should be used to initiate service shutdown. The invocation
   * of this method should cause a call to {@link #notifyStopped()}, either
   * during this method's run, or after it has returned. If shutdown fails, the
   * invocation should cause a call to {@link #notifyFailed(Throwable)} instead.
   *
   * <p>This method should return promptly; prefer to do work on a different
   * thread where it is convenient. It is invoked exactly once on service
   * shutdown, even when {@link #stop} is called multiple times.
   */
  protected abstract void doStop();

  public final Future<State> start() {
    lock.lock();
    try {
      if (state == State.NEW) {
        state = State.STARTING;
        doStart();
      }
    } catch (Throwable startupFailure) {
      // put the exception in the future, the user can get it via Future.get()
      notifyFailed(startupFailure);
    } finally {
      lock.unlock();
    }

    return startup;
  }

  public final Future<State> stop() {
    lock.lock();
    try {
      if (state == State.NEW) {
        state = State.TERMINATED;
        startup.transitionSucceeded(State.TERMINATED);
        shutdown.transitionSucceeded(State.TERMINATED);
      } else if (state == State.STARTING) {
        shutdownWhenStartupFinishes = true;
        startup.transitionSucceeded(State.STOPPING);
      } else if (state == State.RUNNING) {
        state = State.STOPPING;
        doStop();
      }
    } catch (Throwable shutdownFailure) {
      // put the exception in the future, the user can get it via Future.get()
      notifyFailed(shutdownFailure);
    } finally {
      lock.unlock();
    }

    return shutdown;
  }

  public State startAndWait() {
    try {
      return start().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw Throwables.propagate(e.getCause());
    }
  }

  public State stopAndWait() {
    try {
      return stop().get();
    } catch (ExecutionException e) {
      throw Throwables.propagate(e.getCause());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  /**
   * Implementing classes should invoke this method once their service has
   * started. It will cause the service to transition from {@link
   * State#STARTING} to {@link State#RUNNING}.
   *
   * @throws IllegalStateException if the service is not
   *     {@link State#STARTING}.
   */
  protected final void notifyStarted() {
    lock.lock();
    try {
      if (state != State.STARTING) {
        IllegalStateException failure = new IllegalStateException(
            "Cannot notifyStarted() when the service is " + state);
        notifyFailed(failure);
        throw failure;
      }

      state = State.RUNNING;
      if (shutdownWhenStartupFinishes) {
        stop();
      } else {
        startup.transitionSucceeded(State.RUNNING);
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Implementing classes should invoke this method once their service has
   * stopped. It will cause the service to transition from {@link
   * State#STOPPING} to {@link State#TERMINATED}.
   *
   * @throws IllegalStateException if the service is neither {@link
   *     State#STOPPING} nor {@link State#RUNNING}.
   */
  protected final void notifyStopped() {
    lock.lock();
    try {
      if (state != State.STOPPING && state != State.RUNNING) {
        IllegalStateException failure = new IllegalStateException(
            "Cannot notifyStopped() when the service is " + state);
        notifyFailed(failure);
        throw failure;
      }

      state = State.TERMINATED;
      shutdown.transitionSucceeded(State.TERMINATED);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Invoke this method to transition the service to the
   * {@link State#FAILED}. The service will <b>not be stopped</b> if it
   * is running. Invoke this method when a service has failed critically or
   * otherwise cannot be started nor stopped.
   */
  protected final void notifyFailed(Throwable cause) {
    checkNotNull(cause);

    lock.lock();
    try {
      if (state == State.STARTING) {
        startup.transitionFailed(cause);
        shutdown.transitionFailed(new Exception(
            "Service failed to start.", cause));
      } else if (state == State.STOPPING) {
        shutdown.transitionFailed(cause);
      }

      state = State.FAILED;
    } finally {
      lock.unlock();
    }
  }

  public final boolean isRunning() {
    return state() == State.RUNNING;
  }

  public final State state() {
    lock.lock();
    try {
      if (shutdownWhenStartupFinishes && state == State.STARTING) {
        return State.STOPPING;
      } else {
        return state;
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * A change from one service state to another, plus the result of the change.
   *
   * TODO: could this be renamed to DefaultFuture, with methods
   *     like setResult(T) and setFailure(T) ?
   */
  private static class Transition implements Future<State> {
    private final CountDownLatch done = new CountDownLatch(1);
    private State result;
    private Throwable failureCause;

    void transitionSucceeded(State result) {
      // guarded by AbstractService.lock
      checkState(this.result == null);
      this.result = result;
      done.countDown();
    }

    void transitionFailed(Throwable cause) {
      // guarded by AbstractService.lock
      checkState(result == null);
      this.result = State.FAILED;
      this.failureCause = cause;
      done.countDown();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    public boolean isCancelled() {
      return false;
    }

    public boolean isDone() {
      return done.getCount() == 0;
    }

    public State get() throws InterruptedException, ExecutionException {
      done.await();
      return getImmediately();
    }

    public State get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      if (done.await(timeout, unit)) {
        return getImmediately();
      }
      throw new TimeoutException();
    }

    private State getImmediately() throws ExecutionException {
      if (result == State.FAILED) {
        throw new ExecutionException(failureCause);
      } else {
        return result;
      }
    }
  }
}
