/*
 * Copyright (C) 2007 The Guava Authors
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>A list of listeners, each with an associated {@code Executor}, that
 * guarantees that every {@code Runnable} that is {@linkplain #add added} will
 * be executed after {@link #execute()} is called. Any {@code Runnable} added
 * after the call to {@code execute} is still guaranteed to execute. There is no
 * guarantee, however, that listeners will be executed in the order that they
 * are added.
 *
 * <p>Exceptions thrown by a listener will be propagated up to the executor.
 * Any exception thrown during {@code Executor.execute} (e.g., a {@code
 * RejectedExecutionException} or an exception thrown by {@linkplain
 * MoreExecutors#sameThreadExecutor inline execution}) will be caught and
 * logged.
 *
 * @author Nishant Thakkar
 * @author Sven Mawson
 * @since 1.0
 */
public final class ExecutionList {

  // Logger to log exceptions caught when running runnables.
  private static final Logger log =
      Logger.getLogger(ExecutionList.class.getName());

  // The runnable,executor pairs to execute.
  private final Queue<RunnableExecutorPair> runnables = Lists.newLinkedList();

  // Boolean we use mark when execution has started.  Only accessed from within
  // synchronized blocks.
  private boolean executed = false;

  /** Creates a new, empty {@link ExecutionList}. */
  public ExecutionList() {
  }

  /**
   * Adds the {@code Runnable} and accompanying {@code Executor} to the list of
   * listeners to execute. If execution has already begun, the listener is
   * executed immediately.
   *
   * <p>Note: For fast, lightweight listeners that would be safe to execute in
   * any thread, consider {@link MoreExecutors#sameThreadExecutor}. For heavier
   * listeners, {@code sameThreadExecutor()} carries some caveats: First, the
   * thread that the listener runs in depends on whether the {@code
   * ExecutionList} has been executed at the time it is added. In particular,
   * listeners may run in the thread that calls {@code add}. Second, the thread
   * that calls {@link #execute} may be an internal implementation thread, such
   * as an RPC network thread, and {@code sameThreadExecutor()} listeners may
   * run in this thread. Finally, during the execution of a {@code
   * sameThreadExecutor} listener, all other registered but unexecuted
   * listeners are prevented from running, even if those listeners are to run
   * in other executors.
   */
  public void add(Runnable runnable, Executor executor) {
    // Fail fast on a null.  We throw NPE here because the contract of
    // Executor states that it throws NPE on null listener, so we propagate
    // that contract up into the add method as well.
    Preconditions.checkNotNull(runnable, "Runnable was null.");
    Preconditions.checkNotNull(executor, "Executor was null.");

    boolean executeImmediate = false;

    // Lock while we check state.  We must maintain the lock while adding the
    // new pair so that another thread can't run the list out from under us.
    // We only add to the list if we have not yet started execution.
    synchronized (runnables) {
      if (!executed) {
        runnables.add(new RunnableExecutorPair(runnable, executor));
      } else {
        executeImmediate = true;
      }
    }

    // Execute the runnable immediately. Because of scheduling this may end up
    // getting called before some of the previously added runnables, but we're
    // OK with that.  If we want to change the contract to guarantee ordering
    // among runnables we'd have to modify the logic here to allow it.
    if (executeImmediate) {
      new RunnableExecutorPair(runnable, executor).execute();
    }
  }

  /**
   * Runs this execution list, executing all existing pairs in the order they
   * were added. However, note that listeners added after this point may be
   * executed before those previously added, and note that the execution order
   * of all listeners is ultimately chosen by the implementations of the
   * supplied executors.
   *
   * <p>This method is idempotent. Calling it several times in parallel is
   * semantically equivalent to calling it exactly once.
   *
   * @since 10.0 (present in 1.0 as {@code run})
   */
  public void execute() {
    // Lock while we update our state so the add method above will finish adding
    // any listeners before we start to run them.
    synchronized (runnables) {
      if (executed) {
        return;
      }
      executed = true;
    }

    // At this point the runnables will never be modified by another
    // thread, so we are safe using it outside of the synchronized block.
    while (!runnables.isEmpty()) {
      runnables.poll().execute();
    }
  }

  private static class RunnableExecutorPair {
    final Runnable runnable;
    final Executor executor;

    RunnableExecutorPair(Runnable runnable, Executor executor) {
      this.runnable = runnable;
      this.executor = executor;
    }

    void execute() {
      try {
        executor.execute(runnable);
      } catch (RuntimeException e) {
        // Log it and keep going, bad runnable and/or executor.  Don't
        // punish the other runnables if we're given a bad one.  We only
        // catch RuntimeException because we want Errors to propagate up.
        log.log(Level.SEVERE, "RuntimeException while executing runnable "
            + runnable + " with executor " + executor, e);
      }
    }
  }
}
