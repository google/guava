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

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>A list of ({@code Runnable}, {@code Executor}) pairs that guarantees that
 * every {@code Runnable} that is added using the {@link #add} method will be
 * executed in its associated {@code Executor} after {@link #run()} is called.
 * Any {@code Runnable} added after the call to {@code run} is still guaranteed
 * to execute. There is no guarantee that listeners will be executed in the
 * order that they are added.
 *
 * @author Nishant Thakkar
 * @author Sven Mawson
 * @since Guava release 01
 */
@Beta
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
   * Add the runnable/executor pair to the list of pairs to execute.  Executes
   * the pair immediately if we've already started execution.
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

    // Execute the runnable immediately.  Because of scheduling this may end up
    // getting called before some of the previously added runnables, but we're
    // ok with that.  If we want to change the contract to guarantee ordering
    // among runnables we'd have to modify the logic here to allow it.
    if (executeImmediate) {
      executor.execute(runnable);
    }
  }

  /**
   * Runs this execution list, executing all pairs in the order they were
   * added.  Pairs added after this method has started executing the list will
   * be executed immediately.
   *
   * <p>This method is idempotent. Calling it several times in parallel is
   * semantically equivalent to calling it exactly once.
   */
  public void run() {

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
