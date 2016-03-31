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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.concurrent.GuardedBy;

/**
 * Executor ensuring that all Runnables submitted are executed in order, using the provided
 * Executor, and serially such that no two will ever be running at the same time.
 *
 * <p>Tasks submitted to {@link #execute(Runnable)} are executed in FIFO order.
 *
 * <p>Tasks can also be prepended to the queue to be executed in LIFO order before any other
 * submitted tasks. Primarily intended for the currently executing task to be able to schedule a
 * continuation task.
 *
 * <p>Execution on the queue can be {@linkplain #suspend suspended}, e.g. while waiting for an RPC,
 * and execution can be {@linkplain #resume resumed} later.
 *
 * <p>The execution of tasks is done by one thread as long as there are tasks left in the queue and
 * execution has not been suspended. (Even if one task is {@linkplain Thread#interrupt interrupted},
 * execution of subsequent tasks continues.) {@code RuntimeException}s thrown by tasks are simply
 * logged and the executor keeps trucking. If an {@code Error} is thrown, the error will propagate
 * and execution will stop until it is restarted by external calls.
 */
@GwtIncompatible
final class SerializingExecutor implements Executor {
  private static final Logger log = Logger.getLogger(SerializingExecutor.class.getName());

  /** Underlying executor that all submitted Runnable objects are run on. */
  private final Executor executor;

  @GuardedBy("internalLock")
  private final Deque<Runnable> queue = new ArrayDeque<Runnable>();

  @GuardedBy("internalLock")
  private boolean isWorkerRunning = false;

  @GuardedBy("internalLock")
  private int suspensions = 0;

  private final Object internalLock = new Object();

  public SerializingExecutor(Executor executor) {
    this.executor = Preconditions.checkNotNull(executor);
  }

  /**
   * Adds a task to the queue and makes sure a worker thread is running, unless the queue has been
   * suspended.
   *
   * <p>If this method throws, e.g. a {@code RejectedExecutionException} from the delegate executor,
   * execution of tasks will stop until a call to this method or to {@link #resume()} is made.
   */
  public void execute(Runnable task) {
    synchronized (internalLock) {
      queue.add(task);
    }
    startQueueWorker();
  }

  /**
   * Prepends a task to the front of the queue and makes sure a worker thread is running, unless the
   * queue has been suspended.
   */
  public void executeFirst(Runnable task) {
    synchronized (internalLock) {
      queue.addFirst(task);
    }
    startQueueWorker();
  }

  /**
   * Suspends the running of tasks until {@link #resume()} is called. This can be called multiple
   * times to increase the suspensions count and execution will not continue until {@link #resume}
   * has been called the same number of times as {@code suspend} has been.
   *
   * <p>Any task that has already been pulled off the queue for execution will be completed before
   * execution is suspended.
   */
  public void suspend() {
    synchronized (internalLock) {
      suspensions++;
    }
  }

  /**
   * Continue execution of tasks after a call to {@link #suspend()}. More accurately, decreases the
   * suspension counter, as has been incremented by calls to {@link #suspend}, and resumes execution
   * if the suspension counter is zero.
   *
   * <p>If this method throws, e.g. a {@code RejectedExecutionException} from the delegate executor,
   * execution of tasks will stop until a call to this method or to {@link #execute(Runnable)} or
   * {@link #executeFirst(Runnable)} is made.
   *
   * @throws java.lang.IllegalStateException if this executor is not suspended.
   */
  public void resume() {
    synchronized (internalLock) {
      Preconditions.checkState(suspensions > 0);
      suspensions--;
    }
    startQueueWorker();
  }

  private void startQueueWorker() {
    synchronized (internalLock) {
      // We sometimes try to start a queue worker without knowing if there is any work to do.
      if (queue.peek() == null) {
        return;
      }
      if (suspensions > 0) {
        return;
      }
      if (isWorkerRunning) {
        return;
      }
      isWorkerRunning = true;
    }
    boolean executionRejected = true;
    try {
      executor.execute(new QueueWorker());
      executionRejected = false;
    } finally {
      if (executionRejected) {
        // The best we can do is to stop executing the queue, but reset the state so that
        // execution can be resumed later if the caller so wishes.
        synchronized (internalLock) {
          isWorkerRunning = false;
        }
      }
    }
  }

  /**
   * Worker that runs tasks off the queue until it is empty or the queue is suspended.
   */
  private final class QueueWorker implements Runnable {
    @Override
    public void run() {
      try {
        workOnQueue();
      } catch (Error e) {
        synchronized (internalLock) {
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
        synchronized (internalLock) {
          // TODO(user): How should we handle interrupts and shutdowns?
          if (suspensions == 0) {
            task = queue.poll();
          }
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
