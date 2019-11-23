/*
 * Copyright (C) 2007 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A support class for {@code ListenableFuture} implementations to manage their listeners. An
 * instance contains a list of listeners, each with an associated {@code Executor}, and guarantees
 * that every {@code Runnable} that is {@linkplain #add added} will be executed after {@link
 * #execute()} is called. Any {@code Runnable} added after the call to {@code execute} is still
 * guaranteed to execute. There is no guarantee, however, that listeners will be executed in the
 * order that they are added.
 *
 * <p>Exceptions thrown by a listener will be propagated up to the executor. Any exception thrown
 * during {@code Executor.execute} (e.g., a {@code RejectedExecutionException} or an exception
 * thrown by {@linkplain MoreExecutors#directExecutor direct execution}) will be caught and logged.
 *
 * @author Nishant Thakkar
 * @author Sven Mawson
 * @since 1.0
 */
@GwtIncompatible
public final class ExecutionList {
  /** Logger to log exceptions caught when running runnables. */
  private static final Logger log = Logger.getLogger(ExecutionList.class.getName());

  /**
   * The runnable, executor pairs to execute. This acts as a stack threaded through the {@link
   * RunnableExecutorPair#next} field.
   */
  @GuardedBy("this")
  private @Nullable RunnableExecutorPair runnables;

  @GuardedBy("this")
  private boolean executed;

  /** Creates a new, empty {@link ExecutionList}. */
  public ExecutionList() {}

  /**
   * Adds the {@code Runnable} and accompanying {@code Executor} to the list of listeners to
   * execute. If execution has already begun, the listener is executed immediately.
   *
   * <p>When selecting an executor, note that {@code directExecutor} is dangerous in some cases. See
   * the discussion in the {@link ListenableFuture#addListener ListenableFuture.addListener}
   * documentation.
   */
  public void add(Runnable runnable, Executor executor) {
    // Fail fast on a null. We throw NPE here because the contract of Executor states that it throws
    // NPE on null listener, so we propagate that contract up into the add method as well.
    checkNotNull(runnable, "Runnable was null.");
    checkNotNull(executor, "Executor was null.");

    // Lock while we check state. We must maintain the lock while adding the new pair so that
    // another thread can't run the list out from under us. We only add to the list if we have not
    // yet started execution.
    synchronized (this) {
      if (!executed) {
        runnables = new RunnableExecutorPair(runnable, executor, runnables);
        return;
      }
    }
    // Execute the runnable immediately. Because of scheduling this may end up getting called before
    // some of the previously added runnables, but we're OK with that. If we want to change the
    // contract to guarantee ordering among runnables we'd have to modify the logic here to allow
    // it.
    executeListener(runnable, executor);
  }

  /**
   * Runs this execution list, executing all existing pairs in the order they were added. However,
   * note that listeners added after this point may be executed before those previously added, and
   * note that the execution order of all listeners is ultimately chosen by the implementations of
   * the supplied executors.
   *
   * <p>This method is idempotent. Calling it several times in parallel is semantically equivalent
   * to calling it exactly once.
   *
   * @since 10.0 (present in 1.0 as {@code run})
   */
  public void execute() {
    // Lock while we update our state so the add method above will finish adding any listeners
    // before we start to run them.
    RunnableExecutorPair list;
    synchronized (this) {
      if (executed) {
        return;
      }
      executed = true;
      list = runnables;
      runnables = null; // allow GC to free listeners even if this stays around for a while.
    }
    // If we succeeded then list holds all the runnables we to execute. The pairs in the stack are
    // in the opposite order from how they were added so we need to reverse the list to fulfill our
    // contract.
    // This is somewhat annoying, but turns out to be very fast in practice. Alternatively, we could
    // drop the contract on the method that enforces this queue like behavior since depending on it
    // is likely to be a bug anyway.

    // N.B. All writes to the list and the next pointers must have happened before the above
    // synchronized block, so we can iterate the list without the lock held here.
    RunnableExecutorPair reversedList = null;
    while (list != null) {
      RunnableExecutorPair tmp = list;
      list = list.next;
      tmp.next = reversedList;
      reversedList = tmp;
    }
    while (reversedList != null) {
      executeListener(reversedList.runnable, reversedList.executor);
      reversedList = reversedList.next;
    }
  }

  /**
   * Submits the given runnable to the given {@link Executor} catching and logging all {@linkplain
   * RuntimeException runtime exceptions} thrown by the executor.
   */
  private static void executeListener(Runnable runnable, Executor executor) {
    try {
      executor.execute(runnable);
    } catch (RuntimeException e) {
      // Log it and keep going -- bad runnable and/or executor. Don't punish the other runnables if
      // we're given a bad one. We only catch RuntimeException because we want Errors to propagate
      // up.
      log.log(
          Level.SEVERE,
          "RuntimeException while executing runnable " + runnable + " with executor " + executor,
          e);
    }
  }

  private static final class RunnableExecutorPair {
    final Runnable runnable;
    final Executor executor;
    @Nullable RunnableExecutorPair next;

    RunnableExecutorPair(Runnable runnable, Executor executor, RunnableExecutorPair next) {
      this.runnable = runnable;
      this.executor = executor;
      this.next = next;
    }
  }
}
