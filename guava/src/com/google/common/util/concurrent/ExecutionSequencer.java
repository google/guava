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

  enum RunningState {
    NOT_RUN,
    CANCELLED,
    STARTED,
  }

  /** This reference acts as a pointer tracking the head of a linked list of ListenableFutures. */
  private final AtomicReference<ListenableFuture<Object>> ref =
      new AtomicReference<>(immediateFuture(null));

  /**
   * Enqueues a task to run when the previous task (if any) completes.
   *
   * <p>Cancellation does not propagate from the output future to a callable that has begun to
   * execute, but if the output future is cancelled before {@link Callable#call()} is invoked,
   * {@link Callable#call()} will not be invoked.
   */
  public <T> ListenableFuture<T> submit(final Callable<T> callable, Executor executor) {
    checkNotNull(callable);
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
    final AtomicReference<RunningState> runningState = new AtomicReference<>(NOT_RUN);
    final AsyncCallable<T> task =
        new AsyncCallable<T>() {
          @Override
          public ListenableFuture<T> call() throws Exception {
            if (!runningState.compareAndSet(NOT_RUN, STARTED)) {
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
    final ListenableFuture<T> taskFuture =
        Futures.submitAsync(
            task,
            new Executor() {
              @Override
              public void execute(Runnable runnable) {
                oldFuture.addListener(runnable, executor);
              }
            });

    final ListenableFuture<T> outputFuture = Futures.nonCancellationPropagating(taskFuture);

    // newFuture's lifetime is determined by taskFuture, which can't complete before oldFuture
    // unless taskFuture is cancelled, in which case it falls back to oldFuture. This ensures that
    // if the future we return is cancelled, we don't begin execution of the next task until after
    // oldFuture completes.
    Runnable listener =
        new Runnable() {
          @Override
          public void run() {
            if (taskFuture.isDone()
                // If this CAS succeeds, we know that the provided callable will never be invoked,
                // so when oldFuture completes it is safe to allow the next submitted task to
                // proceed.
                || (outputFuture.isCancelled() && runningState.compareAndSet(NOT_RUN, CANCELLED))) {
              // Since the value of oldFuture can only ever be immediateFuture(null) or setFuture of
              // a future that eventually came from immediateFuture(null), this doesn't leak
              // throwables or completion values.
              newFuture.setFuture(oldFuture);
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
}
