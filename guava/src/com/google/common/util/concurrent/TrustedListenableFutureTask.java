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
import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import javax.annotation.Nullable;

/**
 * A {@link RunnableFuture} that also implements the {@link ListenableFuture}
 * interface.
 * 
 * <p>This should be used in preference to {@link ListenableFutureTask} when possible for 
 * performance reasons.
 */
class TrustedListenableFutureTask<V> extends AbstractFuture.TrustedFuture<V>
    implements RunnableFuture<V> {

  private static final AtomicReferenceFieldUpdater<TrustedListenableFutureTask, Thread> RUNNER =
      newUpdater(TrustedListenableFutureTask.class, Thread.class, "runner");

  /**
   * Creates a {@code ListenableFutureTask} that will upon running, execute the
   * given {@code Callable}.
   *
   * @param callable the callable task
   */
  static <V> TrustedListenableFutureTask<V> create(Callable<V> callable) {
    return new TrustedListenableFutureTask<V>(callable);
  }

  /**
   * Creates a {@code ListenableFutureTask} that will upon running, execute the
   * given {@code Runnable}, and arrange that {@code get} will return the
   * given result on successful completion.
   *
   * @param runnable the runnable task
   * @param result the result to return on successful completion. If you don't
   *     need a particular result, consider using constructions of the form:
   *     {@code ListenableFuture<?> f = ListenableFutureTask.create(runnable,
   *     null)}
   */
  static <V> TrustedListenableFutureTask<V> create(
      Runnable runnable, @Nullable V result) {
    return new TrustedListenableFutureTask<V>(Executors.callable(runnable, result));
  }

  private Callable<V> task;

  // These two fields are used to interrupt running tasks.  The thread executing the task publishes
  // itself to the 'runner' field and the thread interrupting sets 'doneInterrupting' when it has
  // finished interrupting.
  private volatile Thread runner;
  private volatile boolean doneInterrupting;

  TrustedListenableFutureTask(Callable<V> callable) {
    this.task = checkNotNull(callable);
  }

  @Override public void run() {
    if (!RUNNER.compareAndSet(this, null, Thread.currentThread())) {
      return;  // someone else has run or is running.
    }
    try {
      // Read before checking isDone to ensure that a cancel race doesn't cause us to read null.
      Callable<V> localTask = task;
      // Ensure we haven't been cancelled or already run.
      if (!isDone()) {
        doRun(localTask);
      }
    } catch (Throwable t) {
      setException(t);
    } finally {
      task = null;
      runner = null;
      if (wasInterrupted()) {
        // We were interrupted, it is possible that the interrupted bit hasn't been set yet.  Wait
        // for the interrupting thread to set 'doneInterrupting' to true. See interruptTask().
        // We want to wait so that we don't interrupt the _next_ thing run on the thread.
        // Note. We don't reset the interrupted bit, just wait for it to be set.
        // If this is a thread pool thread, the thread pool will reset it for us.  Otherwise, the
        // interrupted bit may have been intended for something else, so don't clear it.
        while (!doneInterrupting) {
          Thread.yield();
        }
      }
    }
  }

  @Override public boolean cancel(boolean mayInterruptIfRunning) {
    if (super.cancel(mayInterruptIfRunning)) {
      task = null;
      return true;
    }
    return false;
  }

  @Override protected final void interruptTask() {
    // interruptTask is guaranteed to be called at most once and if runner is non-null when that
    // happens then it must have been the first thread that entered run().  So there is no risk that
    // we are interrupting the wrong thread.
    Thread currentRunner = runner;
    if (currentRunner != null) {
      currentRunner.interrupt();
    }
    doneInterrupting = true;
  }

  /**
   * Template method for calculating and setting the value. Guaranteed to be called at most once.
   *
   * <p>Extracted as an extension point for subclasses that wish to modify behavior.
   * See Futures.combine (which has specialized exception handling).
   */
  void doRun(Callable<V> localTask) throws Exception {
    set(localTask.call());
  }
}
