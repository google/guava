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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An object with an operational state, plus asynchronous {@link #startAsync()} and {@link
 * #stopAsync()} lifecycle methods to transition between states. Example services include
 * webservers, RPC servers and timers.
 *
 * <p>The normal lifecycle of a service is:
 *
 * <ul>
 *   <li>{@linkplain State#NEW NEW} -&gt;
 *   <li>{@linkplain State#STARTING STARTING} -&gt;
 *   <li>{@linkplain State#RUNNING RUNNING} -&gt;
 *   <li>{@linkplain State#STOPPING STOPPING} -&gt;
 *   <li>{@linkplain State#TERMINATED TERMINATED}
 * </ul>
 *
 * <p>There are deviations from this if there are failures or if {@link Service#stopAsync} is called
 * before the {@link Service} reaches the {@linkplain State#RUNNING RUNNING} state. The set of legal
 * transitions form a <a href="http://en.wikipedia.org/wiki/Directed_acyclic_graph">DAG</a>,
 * therefore every method of the listener will be called at most once. N.B. The {@link State#FAILED}
 * and {@link State#TERMINATED} states are terminal states, once a service enters either of these
 * states it cannot ever leave them.
 *
 * <p>Implementors of this interface are strongly encouraged to extend one of the abstract classes
 * in this package which implement this interface and make the threading and state management
 * easier.
 *
 * @author Jesse Wilson
 * @author Luke Sandberg
 * @since 9.0 (in 1.0 as {@code com.google.common.base.Service})
 */
@Beta
@GwtIncompatible
public interface Service {
  /**
   * If the service state is {@link State#NEW}, this initiates service startup and returns
   * immediately. A stopped service may not be restarted.
   *
   * @return this
   * @throws IllegalStateException if the service is not {@link State#NEW}
   * @since 15.0
   */
  @CanIgnoreReturnValue
  Service startAsync();

  /** Returns {@code true} if this service is {@linkplain State#RUNNING running}. */
  boolean isRunning();

  /** Returns the lifecycle state of the service. */
  State state();

  /**
   * If the service is {@linkplain State#STARTING starting} or {@linkplain State#RUNNING running},
   * this initiates service shutdown and returns immediately. If the service is {@linkplain
   * State#NEW new}, it is {@linkplain State#TERMINATED terminated} without having been started nor
   * stopped. If the service has already been stopped, this method returns immediately without
   * taking action.
   *
   * @return this
   * @since 15.0
   */
  @CanIgnoreReturnValue
  Service stopAsync();

  /**
   * Waits for the {@link Service} to reach the {@linkplain State#RUNNING running state}.
   *
   * @throws IllegalStateException if the service reaches a state from which it is not possible to
   *     enter the {@link State#RUNNING} state. e.g. if the {@code state} is {@code
   *     State#TERMINATED} when this method is called then this will throw an IllegalStateException.
   * @since 15.0
   */
  void awaitRunning();

  /**
   * Waits for the {@link Service} to reach the {@linkplain State#RUNNING running state} for no more
   * than the given time.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @throws TimeoutException if the service has not reached the given state within the deadline
   * @throws IllegalStateException if the service reaches a state from which it is not possible to
   *     enter the {@link State#RUNNING RUNNING} state. e.g. if the {@code state} is {@code
   *     State#TERMINATED} when this method is called then this will throw an IllegalStateException.
   * @since 15.0
   */
  void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException;

  /**
   * Waits for the {@link Service} to reach the {@linkplain State#TERMINATED terminated state}.
   *
   * @throws IllegalStateException if the service {@linkplain State#FAILED fails}.
   * @since 15.0
   */
  void awaitTerminated();

  /**
   * Waits for the {@link Service} to reach a terminal state (either {@link Service.State#TERMINATED
   * terminated} or {@link Service.State#FAILED failed}) for no more than the given time.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @throws TimeoutException if the service has not reached the given state within the deadline
   * @throws IllegalStateException if the service {@linkplain State#FAILED fails}.
   * @since 15.0
   */
  void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException;

  /**
   * Returns the {@link Throwable} that caused this service to fail.
   *
   * @throws IllegalStateException if this service's state isn't {@linkplain State#FAILED FAILED}.
   * @since 14.0
   */
  Throwable failureCause();

  /**
   * Registers a {@link Listener} to be {@linkplain Executor#execute executed} on the given
   * executor. The listener will have the corresponding transition method called whenever the
   * service changes state. The listener will not have previous state changes replayed, so it is
   * suggested that listeners are added before the service starts.
   *
   * <p>{@code addListener} guarantees execution ordering across calls to a given listener but not
   * across calls to multiple listeners. Specifically, a given listener will have its callbacks
   * invoked in the same order as the underlying service enters those states. Additionally, at most
   * one of the listener's callbacks will execute at once. However, multiple listeners' callbacks
   * may execute concurrently, and listeners may execute in an order different from the one in which
   * they were registered.
   *
   * <p>RuntimeExceptions thrown by a listener will be caught and logged. Any exception thrown
   * during {@code Executor.execute} (e.g., a {@code RejectedExecutionException}) will be caught and
   * logged.
   *
   * @param listener the listener to run when the service changes state is complete
   * @param executor the executor in which the listeners callback methods will be run. For fast,
   *     lightweight listeners that would be safe to execute in any thread, consider {@link
   *     MoreExecutors#directExecutor}.
   * @since 13.0
   */
  void addListener(Listener listener, Executor executor);

  /**
   * The lifecycle states of a service.
   *
   * <p>The ordering of the {@link State} enum is defined such that if there is a state transition
   * from {@code A -> B} then {@code A.compareTo(B) < 0}. N.B. The converse is not true, i.e. if
   * {@code A.compareTo(B) < 0} then there is <b>not</b> guaranteed to be a valid state transition
   * {@code A -> B}.
   *
   * @since 9.0 (in 1.0 as {@code com.google.common.base.Service.State})
   */
  @Beta // should come out of Beta when Service does
  enum State {
    /** A service in this state is inactive. It does minimal work and consumes minimal resources. */
    NEW {
      @Override
      boolean isTerminal() {
        return false;
      }
    },

    /** A service in this state is transitioning to {@link #RUNNING}. */
    STARTING {
      @Override
      boolean isTerminal() {
        return false;
      }
    },

    /** A service in this state is operational. */
    RUNNING {
      @Override
      boolean isTerminal() {
        return false;
      }
    },

    /** A service in this state is transitioning to {@link #TERMINATED}. */
    STOPPING {
      @Override
      boolean isTerminal() {
        return false;
      }
    },

    /**
     * A service in this state has completed execution normally. It does minimal work and consumes
     * minimal resources.
     */
    TERMINATED {
      @Override
      boolean isTerminal() {
        return true;
      }
    },

    /**
     * A service in this state has encountered a problem and may not be operational. It cannot be
     * started nor stopped.
     */
    FAILED {
      @Override
      boolean isTerminal() {
        return true;
      }
    };

    /** Returns true if this state is terminal. */
    abstract boolean isTerminal();
  }

  /**
   * A listener for the various state changes that a {@link Service} goes through in its lifecycle.
   *
   * <p>All methods are no-ops by default, implementors should override the ones they care about.
   *
   * @author Luke Sandberg
   * @since 15.0 (present as an interface in 13.0)
   */
  @Beta // should come out of Beta when Service does
  abstract class Listener {
    /**
     * Called when the service transitions from {@linkplain State#NEW NEW} to {@linkplain
     * State#STARTING STARTING}. This occurs when {@link Service#startAsync} is called the first
     * time.
     */
    public void starting() {}

    /**
     * Called when the service transitions from {@linkplain State#STARTING STARTING} to {@linkplain
     * State#RUNNING RUNNING}. This occurs when a service has successfully started.
     */
    public void running() {}

    /**
     * Called when the service transitions to the {@linkplain State#STOPPING STOPPING} state. The
     * only valid values for {@code from} are {@linkplain State#STARTING STARTING} or {@linkplain
     * State#RUNNING RUNNING}. This occurs when {@link Service#stopAsync} is called.
     *
     * @param from The previous state that is being transitioned from.
     */
    public void stopping(State from) {}

    /**
     * Called when the service transitions to the {@linkplain State#TERMINATED TERMINATED} state.
     * The {@linkplain State#TERMINATED TERMINATED} state is a terminal state in the transition
     * diagram. Therefore, if this method is called, no other methods will be called on the {@link
     * Listener}.
     *
     * @param from The previous state that is being transitioned from. The only valid values for
     *     this are {@linkplain State#NEW NEW}, {@linkplain State#RUNNING RUNNING} or {@linkplain
     *     State#STOPPING STOPPING}.
     */
    public void terminated(State from) {}

    /**
     * Called when the service transitions to the {@linkplain State#FAILED FAILED} state. The
     * {@linkplain State#FAILED FAILED} state is a terminal state in the transition diagram.
     * Therefore, if this method is called, no other methods will be called on the {@link Listener}.
     *
     * @param from The previous state that is being transitioned from. Failure can occur in any
     *     state with the exception of {@linkplain State#NEW NEW} or {@linkplain State#TERMINATED
     *     TERMINATED}.
     * @param failure The exception that caused the failure.
     */
    public void failed(State from, Throwable failure) {}
  }
}
