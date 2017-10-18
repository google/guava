/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.j2objc.annotations.WeakOuter;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.GuardedBy;

/**
 * Executor ensuring that all Runnables submitted are executed in order, using the provided
 * Executor, and sequentially such that no two will ever be running at the same time.
 *
 * <p>Tasks submitted to {@link #execute(Runnable)} are executed in FIFO order.
 *
 * <p>The execution of tasks is done by one thread as long as there are tasks left in the queue.
 * When a task is {@linkplain Thread#interrupt interrupted}, execution of subsequent tasks
 * continues. {@code RuntimeException}s thrown by tasks are simply logged and the executor keeps
 * trucking. If an {@code Error} is thrown, the error will propagate and execution will stop until
 * it is restarted by a call to {@link #execute}.
 */
@GwtIncompatible
final class SequentialExecutor implements Executor {
  private static final Logger log = Logger.getLogger(SequentialExecutor.class.getName());

  /** Underlying executor that all submitted Runnable objects are run on. */
  private final Executor executor;

  @GuardedBy("queue")
  private final Queue<Runnable> queue = new ArrayDeque<>();

  @GuardedBy("queue")
  private boolean isWorkerRunning = false;

  private final QueueWorker worker = new QueueWorker();

  /** Use {@link MoreExecutors#newSequentialExecutor} */
  SequentialExecutor(Executor executor) {
    this.executor = Preconditions.checkNotNull(executor);
  }

  /**
   * Adds a task to the queue and makes sure a worker thread is running.
   *
   * <p>If this method throws, e.g. a {@code RejectedExecutionException} from the delegate executor,
   * execution of tasks will stop until a call to this method or to {@link #resume()} is made.
   */
  @Override
  public void execute(Runnable task) {
    synchronized (queue) {
      queue.add(task);
      if (isWorkerRunning) {
        return;
      }
      isWorkerRunning = true;
    }
    startQueueWorker();
  }

  /**
   * Starts a worker.  This should only be called if:
   *
   * <ul>
   *   <li>{@code suspensions == 0}
   *   <li>{@code isWorkerRunning == true}
   *   <li>{@code !queue.isEmpty()}
   *   <li>the {@link #worker} lock is not held
   * </ul>
   */
  private void startQueueWorker() {
    boolean executionRejected = true;
    try {
      executor.execute(worker);
      executionRejected = false;
    } finally {
      if (executionRejected) {
        // The best we can do is to stop executing the queue, but reset the state so that
        // execution can be resumed later if the caller so wishes.
        synchronized (queue) {
          isWorkerRunning = false;
        }
      }
    }
  }

  /**
   * Worker that runs tasks from {@link #queue} until it is empty.
   */
  @WeakOuter
  private final class QueueWorker implements Runnable {
    @Override
    public void run() {
      try {
        workOnQueue();
      } catch (Error e) {
        synchronized (queue) {
          isWorkerRunning = false;
        }
        throw e;
        // The execution of a task has ended abnormally.
        // We could have tasks left in the queue, so should perhaps try to restart a worker,
        // but then the Error will get delayed if we are using a direct (same thread) executor.
      }
    }

    private void workOnQueue() {
      while (true) {
        Runnable task = null;
        synchronized (queue) {
          // TODO(user): How should we handle interrupts and shutdowns?
          task = queue.poll();
          if (task == null) {
            isWorkerRunning = false;
            return;
          }
        }
        try {
          task.run();
        } catch (RuntimeException e) {
          log.log(Level.SEVERE, "Exception while executing runnable " + task, e);
        }
      }
    }
  }
}
