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

import com.google.common.annotations.Beta;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * An object with an operational state, plus asynchronous {@link #start()} and {@link #stop()}
 * lifecycle methods to transition between states. Example services include webservers, RPC servers
 * and timers.
 *
 * <p>The normal lifecycle of a service is:
 * <ul>
 *   <li>{@linkplain State#NEW NEW} -&gt;
 *   <li>{@linkplain State#STARTING STARTING} -&gt;
 *   <li>{@linkplain State#RUNNING RUNNING} -&gt;
 *   <li>{@linkplain State#STOPPING STOPPING} -&gt;
 *   <li>{@linkplain State#TERMINATED TERMINATED}
 * </ul>
 *
 * <p>There are deviations from this if there are failures or if {@link Service#stop} is called
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
public interface Service {
  /**
   * If the service state is {@link State#NEW}, this initiates service startup and returns
   * immediately. If the service has already been started, this method returns immediately without
   * taking action. A stopped service may not be restarted.
   *
   * @return a future for the startup result, regardless of whether this call initiated startup.
   *         Calling {@link ListenableFuture#get} will block until the service has finished
   *         starting, and returns one of {@link State#RUNNING}, {@link State#STOPPING} or
   *         {@link State#TERMINATED}. If the service fails to start, {@link ListenableFuture#get}
   *         will throw an {@link ExecutionException}, and the service's state will be
   *         {@link State#FAILED}. If it has already finished starting, {@link ListenableFuture#get}
   *         returns immediately. Cancelling this future has no effect on the service.
   */
  ListenableFuture<State> start();

  /**
   * Initiates service startup (if necessary), returning once the service has finished starting.
   * Unlike calling {@code start().get()}, this method throws no checked exceptions, and it cannot
   * be {@linkplain Thread#interrupt interrupted}.
   *
   * @throws UncheckedExecutionException if startup failed
   * @return the state of the service when startup finished.
   */
  State startAndWait();

  /**
   * Returns {@code true} if this service is {@linkplain State#RUNNING running}.
   */
  boolean isRunning();

  /**
   * Returns the lifecycle state of the service.
   */
  State state();

  /**
   * If the service is {@linkplain State#STARTING starting} or {@linkplain State#RUNNING running},
   * this initiates service shutdown and returns immediately. If the service is
   * {@linkplain State#NEW new}, it is {@linkplain State#TERMINATED terminated} without having been
   * started nor stopped. If the service has already been stopped, this method returns immediately
   * without taking action.
   *
   * @return a future for the shutdown result, regardless of whether this call initiated shutdown.
   *         Calling {@link ListenableFuture#get} will block until the service has finished shutting
   *         down, and either returns {@link State#TERMINATED} or throws an
   *         {@link ExecutionException}. If it has already finished stopping,
   *         {@link ListenableFuture#get} returns immediately. Cancelling this future has no effect
   *         on the service.
   */
  ListenableFuture<State> stop();

  /**
   * Initiates service shutdown (if necessary), returning once the service has finished stopping. If
   * this is {@link State#STARTING}, startup will be cancelled. If this is {@link State#NEW}, it is
   * {@link State#TERMINATED terminated} without having been started nor stopped. Unlike calling
   * {@code stop().get()}, this method throws no checked exceptions.
   *
   * @throws UncheckedExecutionException if the service has failed or fails during shutdown
   * @return the state of the service when shutdown finished.
   */
  State stopAndWait();

  /**
   * Registers a {@link Listener} to be {@linkplain Executor#execute executed} on the given
   * executor.  The listener will have the corresponding transition method called whenever the
   * service changes state. The listener will not have previous state changes replayed, so it is
   * suggested that listeners are added before the service starts.
   *
   * <p>There is no guaranteed ordering of execution of listeners, but any listener added through
   * this method is guaranteed to be called whenever there is a state change.
   *
   * <p>Exceptions thrown by a listener will be propagated up to the executor. Any exception thrown
   * during {@code Executor.execute} (e.g., a {@code RejectedExecutionException} or an exception
   * thrown by {@linkplain MoreExecutors#sameThreadExecutor inline execution}) will be caught and
   * logged.
   *
   * @param listener the listener to run when the service changes state is complete
   * @param executor the executor in which the the listeners callback methods will be run. For fast,
   *     lightweight listeners that would be safe to execute in any thread, consider
   *     {@link MoreExecutors#sameThreadExecutor}.
   * @since 13.0
   */
  void addListener(Listener listener, Executor executor);

  /**
   * The lifecycle states of a service.
   *
   * @since 9.0 (in 1.0 as {@code com.google.common.base.Service.State})
   */
  @Beta // should come out of Beta when Service does
  enum State {
    /**
     * A service in this state is inactive. It does minimal work and consumes
     * minimal resources.
     */
    NEW,

    /**
     * A service in this state is transitioning to {@link #RUNNING}.
     */
    STARTING,

    /**
     * A service in this state is operational.
     */
    RUNNING,

    /**
     * A service in this state is transitioning to {@link #TERMINATED}.
     */
    STOPPING,

    /**
     * A service in this state has completed execution normally. It does minimal work and consumes
     * minimal resources.
     */
    TERMINATED,

    /**
     * A service in this state has encountered a problem and may not be operational. It cannot be
     * started nor stopped.
     */
    FAILED
  }

  /**
   * A listener for the various state changes that a {@link Service} goes through in its lifecycle.
   *
   * @author Luke Sandberg
   * @since 13.0
   */
  @Beta // should come out of Beta when Service does
  interface Listener {
    /**
     * Called when the service transitions from {@linkplain State#NEW NEW} to
     * {@linkplain State#STARTING STARTING}. This occurs when {@link Service#start} or
     * {@link Service#startAndWait} is called the first time.
     */
    void starting();

    /**
     * Called when the service transitions from {@linkplain State#STARTING STARTING} to
     * {@linkplain State#RUNNING RUNNING}. This occurs when a service has successfully started.
     */
    void running();

    /**
     * Called when the service transitions to the {@linkplain State#STOPPING STOPPING} state. The
     * only valid values for {@code from} are {@linkplain State#STARTING STARTING} or
     * {@linkplain State#RUNNING RUNNING}.  This occurs when {@link Service#stop} is called.
     *
     * @param from The previous state that is being transitioned from.
     */
    void stopping(State from);

    /**
     * Called when the service transitions to the {@linkplain State#TERMINATED TERMINATED} state.
     * The {@linkplain State#TERMINATED TERMINATED} state is a terminal state in the transition
     * diagram.  Therefore, if this method is called, no other methods will be called on the
     * {@link Listener}.
     *
     * @param from The previous state that is being transitioned from.  The only valid values for
     *     this are {@linkplain State#NEW NEW}, {@linkplain State#RUNNING RUNNING} or
     *     {@linkplain State#STOPPING STOPPING}.
     */
    void terminated(State from);

    /**
     * Called when the service transitions to the {@linkplain State#FAILED FAILED} state. The
     * {@linkplain State#FAILED FAILED} state is a terminal state in the transition diagram.
     * Therefore, if this method is called, no other methods will be called on the {@link Listener}.
     *
     * @param from The previous state that is being transitioned from.  Failure can occur in any
     *     state with the exception of {@linkplain State#NEW NEW} or
     *     {@linkplain State#TERMINATED TERMINATED}.
     * @param failure The exception that caused the failure.
     */
    void failed(State from, Throwable failure);
  }
}
