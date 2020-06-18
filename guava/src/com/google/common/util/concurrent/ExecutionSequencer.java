/*
 * Copyright (C) 2018 The Guava Authors
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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.ExecutionSequencer.RunningState.CANCELLED;
import static com.google.common.util.concurrent.ExecutionSequencer.RunningState.NOT_RUN;
import static com.google.common.util.concurrent.ExecutionSequencer.RunningState.STARTED;
import static com.google.common.util.concurrent.Futures.immediateCancelledFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.annotations.Beta;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Serializes execution of a set of operations. This class guarantees that a submitted callable will
 * not be called before previously submitted callables (and any {@code Future}s returned from them)
 * have completed.
 *
 * <p>This class implements a superset of the behavior of {@link
 * MoreExecutors#newSequentialExecutor}. If your tasks all run on the same underlying executor and
 * don't need to wait for {@code Future}s returned from {@code AsyncCallable}s, use it instead.
 *
 * @since 26.0
 */
@Beta
public final class ExecutionSequencer {

  private ExecutionSequencer() {}

  /** Creates a new instance. */
  public static ExecutionSequencer create() {
    return new ExecutionSequencer();
  }

  /** This reference acts as a pointer tracking the head of a linked list of ListenableFutures. */
  private final AtomicReference<ListenableFuture<Object>> ref =
      new AtomicReference<>(immediateFuture(null));

  private ThreadConfinedTaskQueue latestTaskQueue = new ThreadConfinedTaskQueue();

  /**
   * This object is unsafely published, but avoids problematic races by relying exclusively on the
   * identity equality of its Thread field so that the task field is only accessed by a single
   * thread.
   */
  private static final class ThreadConfinedTaskQueue {
    /**
     * This field is only used for identity comparisons with the current thread. Field assignments
     * are atomic, but do not provide happens-before ordering; however:
     *
     * <ul>
     *   <li>If this field's value == currentThread, we know that it's up to date, because write
     *       operations in a thread always happen-before subsequent read operations in the same
     *       thread
     *   <li>If this field's value == null because of unsafe publication, we know that it isn't the
     *       object associated with our thread, because if it was the publication wouldn't have been
     *       unsafe and we'd have seen our thread as the value. This state is also why a new
     *       ThreadConfinedTaskQueue object must be created for each inline execution, because
     *       observing a null thread does not mean the object is safe to reuse.
     *   <li>If this field's value is some other thread object, we know that it's not our thread.
     *   <li>If this field's value == null because it originally belonged to another thread and that
     *       thread cleared it, we still know that it's not associated with our thread
     *   <li>If this field's value == null because it was associated with our thread and was
     *       cleared, we know that we're not executing inline any more
     * </ul>
     *
     * All the states where thread != currentThread are identical for our purposes, and so even
     * though it's racy, we don't care which of those values we get, so no need to synchronize.
     */
    Thread thread;
    /** Only used by the thread associated with this object */
    Runnable nextTask;
    /** Only used by the thread associated with this object */
    Executor nextExecutor;
  }

  /**
   * Enqueues a task to run when the previous task (if any) completes.
   *
   * <p>Cancellation does not propagate from the output future to a callable that has begun to
   * execute, but if the output future is cancelled before {@link Callable#call()} is invoked,
   * {@link Callable#call()} will not be invoked.
   */
  public <T> ListenableFuture<T> submit(final Callable<T> callable, Executor executor) {
    checkNotNull(callable);
    checkNotNull(executor);
    return submitAsync(
        new AsyncCallable<T>() {
          @Override
          public ListenableFuture<T> call() throws Exception {
            return immediateFuture(callable.call());
          }

          @Override
          public String toString() {
            return callable.toString();
          }
        },
        executor);
  }

  /**
   * Enqueues a task to run when the previous task (if any) completes.
   *
   * <p>Cancellation does not propagate from the output future to the future returned from {@code
   * callable} or a callable that has begun to execute, but if the output future is cancelled before
   * {@link AsyncCallable#call()} is invoked, {@link AsyncCallable#call()} will not be invoked.
   */
  public <T> ListenableFuture<T> submitAsync(
      final AsyncCallable<T> callable, final Executor executor) {
    checkNotNull(callable);
    checkNotNull(executor);
    final TaskNonReentrantExecutor taskExecutor = new TaskNonReentrantExecutor(executor, this);
    final AsyncCallable<T> task =
        new AsyncCallable<T>() {
          @Override
          public ListenableFuture<T> call() throws Exception {
            if (!taskExecutor.trySetStarted()) {
              return immediateCancelledFuture();
            }
            return callable.call();
          }

          @Override
          public String toString() {
            return callable.toString();
          }
        };
    /*
     * Four futures are at play here:
     * taskFuture is the future tracking the result of the callable.
     * newFuture is a future that completes after this and all prior tasks are done.
     * oldFuture is the previous task's newFuture.
     * outputFuture is the future we return to the caller, a nonCancellationPropagating taskFuture.
     *
     * newFuture is guaranteed to only complete once all tasks previously submitted to this instance
     * have completed - namely after oldFuture is done, and taskFuture has either completed or been
     * cancelled before the callable started execution.
     */
    final SettableFuture<Object> newFuture = SettableFuture.create();

    final ListenableFuture<?> oldFuture = ref.getAndSet(newFuture);

    // Invoke our task once the previous future completes.
    final TrustedListenableFutureTask<T> taskFuture = TrustedListenableFutureTask.create(task);
    oldFuture.addListener(taskFuture, taskExecutor);

    final ListenableFuture<T> outputFuture = Futures.nonCancellationPropagating(taskFuture);

    // newFuture's lifetime is determined by taskFuture, which can't complete before oldFuture
    // unless taskFuture is cancelled, in which case it falls back to oldFuture. This ensures that
    // if the future we return is cancelled, we don't begin execution of the next task until after
    // oldFuture completes.
    Runnable listener =
        new Runnable() {
          @Override
          public void run() {
            if (taskFuture.isDone()) {
              // Since the value of oldFuture can only ever be immediateFuture(null) or setFuture of
              // a future that eventually came from immediateFuture(null), this doesn't leak
              // throwables or completion values.
              newFuture.setFuture(oldFuture);
            } else if (outputFuture.isCancelled() && taskExecutor.trySetCancelled()) {
              // If this CAS succeeds, we know that the provided callable will never be invoked,
              // so when oldFuture completes it is safe to allow the next submitted task to
              // proceed. Doing this immediately here lets the next task run without waiting for
              // the cancelled task's executor to run the noop AsyncCallable.
              taskFuture.cancel(false);
            }
          }
        };
    // Adding the listener to both futures guarantees that newFuture will aways be set. Adding to
    // taskFuture guarantees completion if the callable is invoked, and adding to outputFuture
    // propagates cancellation if the callable has not yet been invoked.
    outputFuture.addListener(listener, directExecutor());
    taskFuture.addListener(listener, directExecutor());

    return outputFuture;
  }

  enum RunningState {
    NOT_RUN,
    CANCELLED,
    STARTED,
  }

  /**
   * This class helps avoid a StackOverflowError when large numbers of tasks are submitted with
   * {@link MoreExecutors#directExecutor}. Normally, when the first future completes, all the other
   * tasks would be called recursively. Here, we detect that the delegate executor is executing
   * inline, and maintain a queue to dispatch tasks iteratively. There is one instance of this class
   * per call to submit() or submitAsync(), and each instance supports only one call to execute().
   *
   * <p>This class would certainly be simpler and easier to reason about if it were built with
   * ThreadLocal; however, ThreadLocal is not well optimized for the case where the ThreadLocal is
   * non-static, and is initialized/removed frequently - this causes churn in the Thread specific
   * hashmaps. Using a static ThreadLocal to avoid that overhead would mean that different
   * ExecutionSequencer objects interfere with each other, which would be undesirable, in addition
   * to increasing the memory footprint of every thread that interacted with it. In order to release
   * entries in thread-specific maps when the ThreadLocal object itself is no longer referenced,
   * ThreadLocal is usually implemented with a WeakReference, which can have negative performance
   * properties; for example, calling WeakReference.get() on Android will block during an
   * otherwise-concurrent GC cycle.
   */
  @SuppressWarnings("ShouldNotSubclass") // Saving an allocation here is worth it
  private static final class TaskNonReentrantExecutor extends AtomicReference<RunningState>
      implements Executor, Runnable {

    /**
     * Used to update and read the latestTaskQueue field. Set to null once the runnable has been run
     * or queued.
     */
    ExecutionSequencer sequencer;

    /**
     * Executor the task was set to run on. Set to null when the task has been queued, run, or
     * cancelled.
     */
    Executor delegate;

    /**
     * Set before calling delegate.execute(); set to null once run, so that it can be GCed; this
     * object may live on after, if submitAsync returns an incomplete future.
     */
    Runnable task;

    /** Thread that called execute(). Set in execute, cleared when delegate.execute() returns. */
    Thread submitting;

    private TaskNonReentrantExecutor(Executor delegate, ExecutionSequencer sequencer) {
      super(NOT_RUN);
      this.delegate = delegate;
      this.sequencer = sequencer;
    }

    @Override
    public void execute(Runnable task) {
      // If this operation was successfully cancelled already, calling the runnable will be a noop.
      // This also avoids a race where if outputFuture is cancelled, it will call taskFuture.cancel,
      // which will call newFuture.setFuture(oldFuture), to allow the next task in the queue to run
      // without waiting for the user's executor to run our submitted Runnable. However, this can
      // interact poorly with the reentrancy-avoiding behavior of this executor - when the operation
      // before the cancelled future completes, it will synchronously complete both the newFuture
      // from the cancelled operation and its own. This can cause one runnable to queue two tasks,
      // breaking the invariant this method relies on to iteratively run the next task after the
      // previous one completes.
      if (get() == RunningState.CANCELLED) {
        delegate = null;
        sequencer = null;
        return;
      }
      submitting = Thread.currentThread();
      try {
        ThreadConfinedTaskQueue submittingTaskQueue = sequencer.latestTaskQueue;
        if (submittingTaskQueue.thread == submitting) {
          sequencer = null;
          // Submit from inside a reentrant submit. We don't know if this one will be reentrant (and
          // can't know without submitting something to the executor) so queue to run iteratively.
          // Task must be null, since each execution on this executor can only produce one more
          // execution.
          checkState(submittingTaskQueue.nextTask == null);
          submittingTaskQueue.nextTask = task;
          submittingTaskQueue.nextExecutor = delegate;
          delegate = null;
        } else {
          Executor localDelegate = delegate;
          delegate = null;
          this.task = task;
          localDelegate.execute(this);
        }
      } finally {
        // Important to null this out here - if we did *not* execute inline, we might still
        // run() on the same thread that called execute() - such as in a thread pool, and think
        // that it was happening inline. As a side benefit, avoids holding on to the Thread object
        // longer than necessary.
        submitting = null;
      }
    }

    @SuppressWarnings("ShortCircuitBoolean")
    @Override
    public void run() {
      Thread currentThread = Thread.currentThread();
      if (currentThread != submitting) {
        Runnable localTask = task;
        task = null;
        localTask.run();
        return;
      }
      // Executor called reentrantly! Make sure that further calls don't overflow stack. Further
      // reentrant calls will see that their current thread is the same as the one set in
      // latestTaskQueue, and queue rather than calling execute() directly.
      ThreadConfinedTaskQueue executingTaskQueue = new ThreadConfinedTaskQueue();
      executingTaskQueue.thread = currentThread;
      // Unconditionally set; there is no risk of throwing away a queued task from another thread,
      // because in order for the current task to run on this executor the previous task must have
      // already started execution. Because each task on a TaskNonReentrantExecutor can only produce
      // one execute() call to another instance from the same ExecutionSequencer, we know by
      // induction that the task that launched this one must not have added any other runnables to
      // that thread's queue, and thus we cannot be replacing a TaskAndThread object that would
      // otherwise have another task queued on to it. Note the exception to this, cancellation, is
      // specially handled in execute() - execute() calls triggered by cancellation are no-ops, and
      // thus don't count.
      sequencer.latestTaskQueue = executingTaskQueue;
      sequencer = null;
      try {
        Runnable localTask = task;
        task = null;
        localTask.run();
        // Now check if our task attempted to reentrantly execute the next task.
        Runnable queuedTask;
        Executor queuedExecutor;
        // Intentionally using non-short-circuit operator
        while ((queuedTask = executingTaskQueue.nextTask) != null
            & (queuedExecutor = executingTaskQueue.nextExecutor) != null) {
          executingTaskQueue.nextTask = null;
          executingTaskQueue.nextExecutor = null;
          queuedExecutor.execute(queuedTask);
        }
      } finally {
        // Null out the thread field, so that we don't leak a reference to Thread, and so that
        // future `thread == currentThread()` calls from this thread don't incorrectly queue instead
        // of executing. Don't null out the latestTaskQueue field, because the work done here
        // may have scheduled more operations on another thread, and if those operations then
        // trigger reentrant calls that thread will have updated the latestTaskQueue field, and
        // we'd be interfering with their operation.
        executingTaskQueue.thread = null;
      }
    }

    private boolean trySetStarted() {
      return compareAndSet(NOT_RUN, STARTED);
    }

    private boolean trySetCancelled() {
      return compareAndSet(NOT_RUN, CANCELLED);
    }
  }
}
