/*
 * Copyright (C) 2009 The Guava Authors
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

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.Service.State; // javadoc needs this

import java.util.concurrent.ExecutionException;
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
 * @since 1.0
 */
@Beta
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

  @Override
  public final ListenableFuture<State> start() {
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

  @Override
  public final ListenableFuture<State> stop() {
    lock.lock();
    try {
      if (state == State.NEW) {
        state = State.TERMINATED;
        startup.set(State.TERMINATED);
        shutdown.set(State.TERMINATED);
      } else if (state == State.STARTING) {
        shutdownWhenStartupFinishes = true;
        startup.set(State.STOPPING);
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

  @Override
  public State startAndWait() {
    return Futures.getUnchecked(start());
  }

  @Override
  public State stopAndWait() {
    return Futures.getUnchecked(stop());
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
        startup.set(State.RUNNING);
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
      shutdown.set(State.TERMINATED);
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
        startup.setException(cause);
        shutdown.setException(new Exception(
            "Service failed to start.", cause));
      } else if (state == State.STOPPING) {
        shutdown.setException(cause);
      } else if (state == State.RUNNING) {
        shutdown.setException(new Exception("Service failed while running", cause));
      } else if (state == State.NEW || state == State.TERMINATED) {
        throw new IllegalStateException("Failed while in state:" + state, cause);
      }
      state = State.FAILED;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public final boolean isRunning() {
    return state() == State.RUNNING;
  }

  @Override
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

  @Override public String toString() {
    return getClass().getSimpleName() + " [" + state() + "]";
  }

  /**
   * A change from one service state to another, plus the result of the change.
   */
  private class Transition extends AbstractFuture<State> {
    @Override
    public State get(long timeout, TimeUnit unit)
        throws InterruptedException, TimeoutException, ExecutionException {
      try {
        return super.get(timeout, unit);
      } catch (TimeoutException e) {
        throw new TimeoutException(AbstractService.this.toString());
      }
    }
  }
}
