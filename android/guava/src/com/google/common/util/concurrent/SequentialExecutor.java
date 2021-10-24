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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.SequentialExecutor.WorkerRunningState.IDLE;
import static com.google.common.util.concurrent.SequentialExecutor.WorkerRunningState.QUEUED;
import static com.google.common.util.concurrent.SequentialExecutor.WorkerRunningState.QUEUING;
import static com.google.common.util.concurrent.SequentialExecutor.WorkerRunningState.RUNNING;
import static java.lang.System.identityHashCode;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.google.j2objc.annotations.RetainedWith;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;

/**
 * Executor ensuring that all Runnables submitted are executed in order, using the provided
 * Executor, and sequentially such that no two will ever be running at the same time.
 *
 * <p>Tasks submitted to {@link #execute(Runnable)} are executed in FIFO order.
 *
 * <p>The execution of tasks is done by one thread as long as there are tasks left in the queue.
 * When a task is {@linkplain Thread#interrupt interrupted}, execution of subsequent tasks
 * continues. See {@link QueueWorker#workOnQueue} for details.
 *
 * <p>{@code RuntimeException}s thrown by tasks are simply logged and the executor keeps trucking.
 * If an {@code Error} is thrown, the error will propagate and execution will stop until it is
 * restarted by a call to {@link #execute}.
 */
@GwtIncompatible
@ElementTypesAreNonnullByDefault
final class SequentialExecutor implements Executor {
  private static final Logger log = Logger.getLogger(SequentialExecutor.class.getName());

  enum WorkerRunningState {
    /** Runnable is not running and not queued for execution */
    IDLE,
    /** Runnable is not running, but is being queued for execution */
    QUEUING,
    /** runnable has been submitted but has not yet begun execution */
    QUEUED,
    RUNNING,
  }

  /** Underlying executor that all submitted Runnable objects are run on. */
  private final Executor executor;

  @GuardedBy("queue")
  private final Deque<Runnable> queue = new ArrayDeque<>();

  /** see {@link WorkerRunningState} */
  @GuardedBy("queue")
  private WorkerRunningState workerRunningState = IDLE;

  /**
   * This counter prevents an ABA issue where a thread may successfully schedule the worker, the
   * worker runs and exhausts the queue, another thread enqueues a task and fails to schedule the
   * worker, and then the first thread's call to delegate.execute() returns. Without this counter,
   * it would observe the QUEUING state and set it to QUEUED, and the worker would never be
   * scheduled again for future submissions.
   */
  @GuardedBy("queue")
  private long workerRunCount = 0;

  @RetainedWith private final QueueWorker worker = new QueueWorker();

  /** Use {@link MoreExecutors#newSequentialExecutor} */
  SequentialExecutor(Executor executor) {
    this.executor = Preconditions.checkNotNull(executor);
  }

  /**
   * Adds a task to the queue and makes sure a worker thread is running.
   *
   * <p>If this method throws, e.g. a {@code RejectedExecutionException} from the delegate executor,
   * execution of tasks will stop until a call to this method is made.
   */
  @Override
  public void execute(Runnable task) {
    checkNotNull(task);
    Runnable submittedTask;
    long oldRunCount;
    synchronized (queue) {
      // If the worker is already running (or execute() on the delegate returned successfully, and
      // the worker has yet to start) then we don't need to start the worker.
      if (workerRunningState == RUNNING || workerRunningState == QUEUED) {
        queue.add(task);
        return;
      }

      oldRunCount = workerRunCount;

      // If the worker is not yet running, the delegate Executor might reject our attempt to start
      // it. To preserve FIFO order and failure atomicity of rejected execution when the same
      // Runnable is executed more than once, allocate a wrapper that we know is safe to remove by
      // object identity.
      // A data structure that returned a removal handle from add() would allow eliminating this
      // allocation.
      submittedTask =
          new Runnable() {
            @Override
            public void run() {
              task.run();
            }

            @Override
            public String toString() {
              return task.toString();
            }
          };
      queue.add(submittedTask);
      workerRunningState = QUEUING;
    }

    try {
      executor.execute(worker);
    } catch (RuntimeException | Error t) {
      synchronized (queue) {
        boolean removed =
            (workerRunningState == IDLE || workerRunningState == QUEUING)
                && queue.removeLastOccurrence(submittedTask);
        // If the delegate is directExecutor(), the submitted runnable could have thrown a REE. But
        // that's handled by the log check that catches RuntimeExceptions in the queue worker.
        if (!(t instanceof RejectedExecutionException) || removed) {
          throw t;
        }
      }
      return;
    }

    /*
     * This is an unsynchronized read! After the read, the function returns immediately or acquires
     * the lock to check again. Since an IDLE state was observed inside the preceding synchronized
     * block, and reference field assignment is atomic, this may save reacquiring the lock when
     * another thread or the worker task has cleared the count and set the state.
     *
     * <p>When {@link #executor} is a directExecutor(), the value written to
     * {@code workerRunningState} will be available synchronously, and behaviour will be
     * deterministic.
     */
    @SuppressWarnings("GuardedBy")
    boolean alreadyMarkedQueued = workerRunningState != QUEUING;
    if (alreadyMarkedQueued) {
      return;
    }
    synchronized (queue) {
      if (workerRunCount == oldRunCount && workerRunningState == QUEUING) {
        workerRunningState = QUEUED;
      }
    }
  }

  /** Worker that runs tasks from {@link #queue} until it is empty. */
  private final class QueueWorker implements Runnable {
    @CheckForNull Runnable task;

    @Override
    public void run() {
      try {
        workOnQueue();
      } catch (Error e) {
        synchronized (queue) {
          workerRunningState = IDLE;
        }
        throw e;
        // The execution of a task has ended abnormally.
        // We could have tasks left in the queue, so should perhaps try to restart a worker,
        // but then the Error will get delayed if we are using a direct (same thread) executor.
      }
    }

    /**
     * Continues executing tasks from {@link #queue} until it is empty.
     *
     * <p>The thread's interrupt bit is cleared before execution of each task.
     *
     * <p>If the Thread in use is interrupted before or during execution of the tasks in {@link
     * #queue}, the Executor will complete its tasks, and then restore the interruption. This means
     * that once the Thread returns to the Executor that this Executor composes, the interruption
     * will still be present. If the composed Executor is an ExecutorService, it can respond to
     * shutdown() by returning tasks queued on that Thread after {@link #worker} drains the queue.
     */
    private void workOnQueue() {
      boolean interruptedDuringTask = false;
      boolean hasSetRunning = false;
      try {
        while (true) {
          synchronized (queue) {
            // Choose whether this thread will run or not after acquiring the lock on the first
            // iteration
            if (!hasSetRunning) {
              if (workerRunningState == RUNNING) {
                // Don't want to have two workers pulling from the queue.
                return;
              } else {
                // Increment the run counter to avoid the ABA problem of a submitter marking the
                // thread as QUEUED after it already ran and exhausted the queue before returning
                // from execute().
                workerRunCount++;
                workerRunningState = RUNNING;
                hasSetRunning = true;
              }
            }
            task = queue.poll();
            if (task == null) {
              workerRunningState = IDLE;
              return;
            }
          }
          // Remove the interrupt bit before each task. The interrupt is for the "current task" when
          // it is sent, so subsequent tasks in the queue should not be caused to be interrupted
          // by a previous one in the queue being interrupted.
          interruptedDuringTask |= Thread.interrupted();
          try {
            task.run();
          } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Exception while executing runnable " + task, e);
          } finally {
            task = null;
          }
        }
      } finally {
        // Ensure that if the thread was interrupted at all while processing the task queue, it
        // is returned to the delegate Executor interrupted so that it may handle the
        // interruption if it likes.
        if (interruptedDuringTask) {
          Thread.currentThread().interrupt();
        }
      }
    }

    @SuppressWarnings("GuardedBy")
    @Override
    public String toString() {
      Runnable currentlyRunning = task;
      if (currentlyRunning != null) {
        return "SequentialExecutorWorker{running=" + currentlyRunning + "}";
      }
      return "SequentialExecutorWorker{state=" + workerRunningState + "}";
    }
  }

  @Override
  public String toString() {
    return "SequentialExecutor@" + identityHashCode(this) + "{" + executor + "}";
  }
}
