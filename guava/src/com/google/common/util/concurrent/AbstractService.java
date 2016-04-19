/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.Service.State.FAILED;
import static com.google.common.util.concurrent.Service.State.NEW;
import static com.google.common.util.concurrent.Service.State.RUNNING;
import static com.google.common.util.concurrent.Service.State.STARTING;
import static com.google.common.util.concurrent.Service.State.STOPPING;
import static com.google.common.util.concurrent.Service.State.TERMINATED;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.util.concurrent.ListenerCallQueue.Callback;
import com.google.common.util.concurrent.Monitor.Guard;
import com.google.common.util.concurrent.Service.State; // javadoc needs this
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2objc.annotations.WeakOuter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;

/**
 * Base class for implementing services that can handle {@link #doStart} and {@link #doStop}
 * requests, responding to them with {@link #notifyStarted()} and {@link #notifyStopped()}
 * callbacks. Its subclasses must manage threads manually; consider {@link
 * AbstractExecutionThreadService} if you need only a single execution thread.
 *
 * @author Jesse Wilson
 * @author Luke Sandberg
 * @since 1.0
 */
@Beta
@GwtIncompatible
public abstract class AbstractService implements Service {
  private static final Callback<Listener> STARTING_CALLBACK =
      new Callback<Listener>("starting()") {
        @Override
        void call(Listener listener) {
          listener.starting();
        }
      };
  private static final Callback<Listener> RUNNING_CALLBACK =
      new Callback<Listener>("running()") {
        @Override
        void call(Listener listener) {
          listener.running();
        }
      };
  private static final Callback<Listener> STOPPING_FROM_STARTING_CALLBACK =
      stoppingCallback(STARTING);
  private static final Callback<Listener> STOPPING_FROM_RUNNING_CALLBACK =
      stoppingCallback(RUNNING);

  private static final Callback<Listener> TERMINATED_FROM_NEW_CALLBACK = terminatedCallback(NEW);
  private static final Callback<Listener> TERMINATED_FROM_RUNNING_CALLBACK =
      terminatedCallback(RUNNING);
  private static final Callback<Listener> TERMINATED_FROM_STOPPING_CALLBACK =
      terminatedCallback(STOPPING);

  private static Callback<Listener> terminatedCallback(final State from) {
    return new Callback<Listener>("terminated({from = " + from + "})") {
      @Override
      void call(Listener listener) {
        listener.terminated(from);
      }
    };
  }

  private static Callback<Listener> stoppingCallback(final State from) {
    return new Callback<Listener>("stopping({from = " + from + "})") {
      @Override
      void call(Listener listener) {
        listener.stopping(from);
      }
    };
  }

  private final Monitor monitor = new Monitor();

  private final Guard isStartable = new IsStartableGuard();

  @WeakOuter
  private final class IsStartableGuard extends Guard {
    IsStartableGuard() {
      super(AbstractService.this.monitor);
    }

    @Override
    public boolean isSatisfied() {
      return state() == NEW;
    }
  }

  private final Guard isStoppable = new IsStoppableGuard();

  @WeakOuter
  private final class IsStoppableGuard extends Guard {
    IsStoppableGuard() {
      super(AbstractService.this.monitor);
    }

    @Override
    public boolean isSatisfied() {
      return state().compareTo(RUNNING) <= 0;
    }
  }

  private final Guard hasReachedRunning = new HasReachedRunningGuard();

  @WeakOuter
  private final class HasReachedRunningGuard extends Guard {
    HasReachedRunningGuard() {
      super(AbstractService.this.monitor);
    }

    @Override
    public boolean isSatisfied() {
      return state().compareTo(RUNNING) >= 0;
    }
  }

  private final Guard isStopped = new IsStoppedGuard();

  @WeakOuter
  private final class IsStoppedGuard extends Guard {
    IsStoppedGuard() {
      super(AbstractService.this.monitor);
    }

    @Override
    public boolean isSatisfied() {
      return state().isTerminal();
    }
  }

  /**
   * The listeners to notify during a state transition.
   */
  @GuardedBy("monitor")
  private final List<ListenerCallQueue<Listener>> listeners =
      Collections.synchronizedList(new ArrayList<ListenerCallQueue<Listener>>());

  /**
   * The current state of the service. This should be written with the lock held but can be read
   * without it because it is an immutable object in a volatile field. This is desirable so that
   * methods like {@link #state}, {@link #failureCause} and notably {@link #toString} can be run
   * without grabbing the lock.
   *
   * <p>To update this field correctly the lock must be held to guarantee that the state is
   * consistent.
   */
  @GuardedBy("monitor")
  private volatile StateSnapshot snapshot = new StateSnapshot(NEW);

  /** Constructor for use by subclasses. */
  protected AbstractService() {}

  /**
   * This method is called by {@link #startAsync} to initiate service startup. The invocation of
   * this method should cause a call to {@link #notifyStarted()}, either during this method's run,
   * or after it has returned. If startup fails, the invocation should cause a call to {@link
   * #notifyFailed(Throwable)} instead.
   *
   * <p>This method should return promptly; prefer to do work on a different thread where it is
   * convenient. It is invoked exactly once on service startup, even when {@link #startAsync} is
   * called multiple times.
   */
  protected abstract void doStart();

  /**
   * This method should be used to initiate service shutdown. The invocation of this method should
   * cause a call to {@link #notifyStopped()}, either during this method's run, or after it has
   * returned. If shutdown fails, the invocation should cause a call to {@link
   * #notifyFailed(Throwable)} instead.
   *
   * <p>This method should return promptly; prefer to do work on a different thread where it is
   * convenient. It is invoked exactly once on service shutdown, even when {@link #stopAsync} is
   * called multiple times.
   */
  protected abstract void doStop();

  @CanIgnoreReturnValue
  @Override
  public final Service startAsync() {
    if (monitor.enterIf(isStartable)) {
      try {
        snapshot = new StateSnapshot(STARTING);
        starting();
        doStart();
      } catch (Throwable startupFailure) {
        notifyFailed(startupFailure);
      } finally {
        monitor.leave();
        executeListeners();
      }
    } else {
      throw new IllegalStateException("Service " + this + " has already been started");
    }
    return this;
  }

  @CanIgnoreReturnValue
  @Override
  public final Service stopAsync() {
    if (monitor.enterIf(isStoppable)) {
      try {
        State previous = state();
        switch (previous) {
          case NEW:
            snapshot = new StateSnapshot(TERMINATED);
            terminated(NEW);
            break;
          case STARTING:
            snapshot = new StateSnapshot(STARTING, true, null);
            stopping(STARTING);
            break;
          case RUNNING:
            snapshot = new StateSnapshot(STOPPING);
            stopping(RUNNING);
            doStop();
            break;
          case STOPPING:
          case TERMINATED:
          case FAILED:
            // These cases are impossible due to the if statement above.
            throw new AssertionError("isStoppable is incorrectly implemented, saw: " + previous);
          default:
            throw new AssertionError("Unexpected state: " + previous);
        }
      } catch (Throwable shutdownFailure) {
        notifyFailed(shutdownFailure);
      } finally {
        monitor.leave();
        executeListeners();
      }
    }
    return this;
  }

  @Override
  public final void awaitRunning() {
    monitor.enterWhenUninterruptibly(hasReachedRunning);
    try {
      checkCurrentState(RUNNING);
    } finally {
      monitor.leave();
    }
  }

  @Override
  public final void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException {
    if (monitor.enterWhenUninterruptibly(hasReachedRunning, timeout, unit)) {
      try {
        checkCurrentState(RUNNING);
      } finally {
        monitor.leave();
      }
    } else {
      // It is possible due to races the we are currently in the expected state even though we
      // timed out. e.g. if we weren't event able to grab the lock within the timeout we would never
      // even check the guard. I don't think we care too much about this use case but it could lead
      // to a confusing error message.
      throw new TimeoutException("Timed out waiting for " + this + " to reach the RUNNING state.");
    }
  }

  @Override
  public final void awaitTerminated() {
    monitor.enterWhenUninterruptibly(isStopped);
    try {
      checkCurrentState(TERMINATED);
    } finally {
      monitor.leave();
    }
  }

  @Override
  public final void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException {
    if (monitor.enterWhenUninterruptibly(isStopped, timeout, unit)) {
      try {
        checkCurrentState(TERMINATED);
      } finally {
        monitor.leave();
      }
    } else {
      // It is possible due to races the we are currently in the expected state even though we
      // timed out. e.g. if we weren't event able to grab the lock within the timeout we would never
      // even check the guard. I don't think we care too much about this use case but it could lead
      // to a confusing error message.
      throw new TimeoutException(
          "Timed out waiting for "
              + this
              + " to reach a terminal state. "
              + "Current state: "
              + state());
    }
  }

  /** Checks that the current state is equal to the expected state. */
  @GuardedBy("monitor")
  private void checkCurrentState(State expected) {
    State actual = state();
    if (actual != expected) {
      if (actual == FAILED) {
        // Handle this specially so that we can include the failureCause, if there is one.
        throw new IllegalStateException(
            "Expected the service " + this + " to be " + expected + ", but the service has FAILED",
            failureCause());
      }
      throw new IllegalStateException(
          "Expected the service " + this + " to be " + expected + ", but was " + actual);
    }
  }

  /**
   * Implementing classes should invoke this method once their service has started. It will cause
   * the service to transition from {@link State#STARTING} to {@link State#RUNNING}.
   *
   * @throws IllegalStateException if the service is not {@link State#STARTING}.
   */
  protected final void notifyStarted() {
    monitor.enter();
    try {
      // We have to examine the internal state of the snapshot here to properly handle the stop
      // while starting case.
      if (snapshot.state != STARTING) {
        IllegalStateException failure =
            new IllegalStateException(
                "Cannot notifyStarted() when the service is " + snapshot.state);
        notifyFailed(failure);
        throw failure;
      }

      if (snapshot.shutdownWhenStartupFinishes) {
        snapshot = new StateSnapshot(STOPPING);
        // We don't call listeners here because we already did that when we set the
        // shutdownWhenStartupFinishes flag.
        doStop();
      } else {
        snapshot = new StateSnapshot(RUNNING);
        running();
      }
    } finally {
      monitor.leave();
      executeListeners();
    }
  }

  /**
   * Implementing classes should invoke this method once their service has stopped. It will cause
   * the service to transition from {@link State#STOPPING} to {@link State#TERMINATED}.
   *
   * @throws IllegalStateException if the service is neither {@link State#STOPPING} nor {@link
   *     State#RUNNING}.
   */
  protected final void notifyStopped() {
    monitor.enter();
    try {
      // We check the internal state of the snapshot instead of state() directly so we don't allow
      // notifyStopped() to be called while STARTING, even if stop() has already been called.
      State previous = snapshot.state;
      if (previous != STOPPING && previous != RUNNING) {
        IllegalStateException failure =
            new IllegalStateException("Cannot notifyStopped() when the service is " + previous);
        notifyFailed(failure);
        throw failure;
      }
      snapshot = new StateSnapshot(TERMINATED);
      terminated(previous);
    } finally {
      monitor.leave();
      executeListeners();
    }
  }

  /**
   * Invoke this method to transition the service to the {@link State#FAILED}. The service will
   * <b>not be stopped</b> if it is running. Invoke this method when a service has failed critically
   * or otherwise cannot be started nor stopped.
   */
  protected final void notifyFailed(Throwable cause) {
    checkNotNull(cause);

    monitor.enter();
    try {
      State previous = state();
      switch (previous) {
        case NEW:
        case TERMINATED:
          throw new IllegalStateException("Failed while in state:" + previous, cause);
        case RUNNING:
        case STARTING:
        case STOPPING:
          snapshot = new StateSnapshot(FAILED, false, cause);
          failed(previous, cause);
          break;
        case FAILED:
          // Do nothing
          break;
        default:
          throw new AssertionError("Unexpected state: " + previous);
      }
    } finally {
      monitor.leave();
      executeListeners();
    }
  }

  @Override
  public final boolean isRunning() {
    return state() == RUNNING;
  }

  @Override
  public final State state() {
    return snapshot.externalState();
  }

  /**
   * @since 14.0
   */
  @Override
  public final Throwable failureCause() {
    return snapshot.failureCause();
  }

  /**
   * @since 13.0
   */
  @Override
  public final void addListener(Listener listener, Executor executor) {
    checkNotNull(listener, "listener");
    checkNotNull(executor, "executor");
    monitor.enter();
    try {
      if (!state().isTerminal()) {
        listeners.add(new ListenerCallQueue<Listener>(listener, executor));
      }
    } finally {
      monitor.leave();
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [" + state() + "]";
  }

  /**
   * Attempts to execute all the listeners in {@link #listeners} while not holding the
   * {@link #monitor}.
   */
  private void executeListeners() {
    if (!monitor.isOccupiedByCurrentThread()) {
      // iterate by index to avoid concurrent modification exceptions
      for (int i = 0; i < listeners.size(); i++) {
        listeners.get(i).execute();
      }
    }
  }

  @GuardedBy("monitor")
  private void starting() {
    STARTING_CALLBACK.enqueueOn(listeners);
  }

  @GuardedBy("monitor")
  private void running() {
    RUNNING_CALLBACK.enqueueOn(listeners);
  }

  @GuardedBy("monitor")
  private void stopping(final State from) {
    if (from == State.STARTING) {
      STOPPING_FROM_STARTING_CALLBACK.enqueueOn(listeners);
    } else if (from == State.RUNNING) {
      STOPPING_FROM_RUNNING_CALLBACK.enqueueOn(listeners);
    } else {
      throw new AssertionError();
    }
  }

  @GuardedBy("monitor")
  private void terminated(final State from) {
    switch (from) {
      case NEW:
        TERMINATED_FROM_NEW_CALLBACK.enqueueOn(listeners);
        break;
      case RUNNING:
        TERMINATED_FROM_RUNNING_CALLBACK.enqueueOn(listeners);
        break;
      case STOPPING:
        TERMINATED_FROM_STOPPING_CALLBACK.enqueueOn(listeners);
        break;
      case STARTING:
      case TERMINATED:
      case FAILED:
      default:
        throw new AssertionError();
    }
  }

  @GuardedBy("monitor")
  private void failed(final State from, final Throwable cause) {
    // can't memoize this one due to the exception
    new Callback<Listener>("failed({from = " + from + ", cause = " + cause + "})") {
      @Override
      void call(Listener listener) {
        listener.failed(from, cause);
      }
    }.enqueueOn(listeners);
  }

  /**
   * An immutable snapshot of the current state of the service. This class represents a consistent
   * snapshot of the state and therefore it can be used to answer simple queries without needing to
   * grab a lock.
   */
  @Immutable
  private static final class StateSnapshot {
    /**
     * The internal state, which equals external state unless shutdownWhenStartupFinishes is true.
     */
    final State state;

    /**
     * If true, the user requested a shutdown while the service was still starting up.
     */
    final boolean shutdownWhenStartupFinishes;

    /**
     * The exception that caused this service to fail. This will be {@code null} unless the service
     * has failed.
     */
    @Nullable final Throwable failure;

    StateSnapshot(State internalState) {
      this(internalState, false, null);
    }

    StateSnapshot(
        State internalState, boolean shutdownWhenStartupFinishes, @Nullable Throwable failure) {
      checkArgument(
          !shutdownWhenStartupFinishes || internalState == STARTING,
          "shudownWhenStartupFinishes can only be set if state is STARTING. Got %s instead.",
          internalState);
      checkArgument(
          !(failure != null ^ internalState == FAILED),
          "A failure cause should be set if and only if the state is failed.  Got %s and %s "
              + "instead.",
          internalState,
          failure);
      this.state = internalState;
      this.shutdownWhenStartupFinishes = shutdownWhenStartupFinishes;
      this.failure = failure;
    }

    /** @see Service#state() */
    State externalState() {
      if (shutdownWhenStartupFinishes && state == STARTING) {
        return STOPPING;
      } else {
        return state;
      }
    }

    /** @see Service#failureCause() */
    Throwable failureCause() {
      checkState(
          state == FAILED,
          "failureCause() is only valid if the service has failed, service is %s",
          state);
      return failure;
    }
  }
}
