/*
 * Copyright (C) 2015 The Guava Authors
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
import static com.google.common.util.concurrent.AggregateFuture.ReleaseResourcesReason.OUTPUT_FUTURE_DONE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableCollection;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedLocalRef;
import com.google.j2objc.annotations.WeakOuter;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Aggregate future that computes its value by calling a callable. */
@GwtCompatible
@ElementTypesAreNonnullByDefault
final class CombinedFuture<V extends @Nullable Object>
    extends AggregateFuture<@Nullable Object, V> {
  @CheckForNull @LazyInit private CombinedFutureInterruptibleTask<?> task;

  CombinedFuture(
      ImmutableCollection<? extends ListenableFuture<?>> futures,
      boolean allMustSucceed,
      Executor listenerExecutor,
      AsyncCallable<V> callable) {
    super(futures, allMustSucceed, false);
    this.task = new AsyncCallableInterruptibleTask(callable, listenerExecutor);
    init();
  }

  CombinedFuture(
      ImmutableCollection<? extends ListenableFuture<?>> futures,
      boolean allMustSucceed,
      Executor listenerExecutor,
      Callable<V> callable) {
    super(futures, allMustSucceed, false);
    this.task = new CallableInterruptibleTask(callable, listenerExecutor);
    init();
  }

  @Override
  void collectOneValue(int index, @CheckForNull Object returnValue) {}

  @Override
  void handleAllCompleted() {
    @RetainedLocalRef CombinedFutureInterruptibleTask<?> localTask = task;
    if (localTask != null) {
      localTask.execute();
    }
  }

  @Override
  void releaseResources(ReleaseResourcesReason reason) {
    super.releaseResources(reason);
    /*
     * If the output future is done, then it won't need to interrupt the task later, so it can clear
     * its reference to it.
     *
     * If the output future is *not* done, then the task field will be cleared after the task runs
     * or after the output future is done, whichever comes first.
     */
    if (reason == OUTPUT_FUTURE_DONE) {
      this.task = null;
    }
  }

  @Override
  protected void interruptTask() {
    @RetainedLocalRef CombinedFutureInterruptibleTask<?> localTask = task;
    if (localTask != null) {
      localTask.interruptTask();
    }
  }

  @WeakOuter
  private abstract class CombinedFutureInterruptibleTask<T extends @Nullable Object>
      extends InterruptibleTask<T> {
    private final Executor listenerExecutor;

    CombinedFutureInterruptibleTask(Executor listenerExecutor) {
      this.listenerExecutor = checkNotNull(listenerExecutor);
    }

    @Override
    final boolean isDone() {
      return CombinedFuture.this.isDone();
    }

    final void execute() {
      try {
        listenerExecutor.execute(this);
      } catch (RejectedExecutionException e) {
        CombinedFuture.this.setException(e);
      }
    }

    @Override
    final void afterRanInterruptiblySuccess(@ParametricNullness T result) {
      /*
       * The future no longer needs to interrupt this task, so it no longer needs a reference to it.
       *
       * TODO(cpovirk): It might be nice for our InterruptibleTask subclasses to null out their
       *  `callable` fields automatically. That would make it less important for us to null out the
       * reference to `task` here (though it's still nice to do so in case our reference to the
       * executor keeps it alive). Ideally, nulling out `callable` would be the responsibility of
       * InterruptibleTask itself so that its other subclasses also benefit. (Handling `callable` in
       * InterruptibleTask itself might also eliminate some of the existing boilerplate for, e.g.,
       * pendingToString().)
       */
      CombinedFuture.this.task = null;

      setValue(result);
    }

    @Override
    final void afterRanInterruptiblyFailure(Throwable error) {
      // See afterRanInterruptiblySuccess.
      CombinedFuture.this.task = null;

      if (error instanceof ExecutionException) {
        /*
         * Cast to ExecutionException to satisfy our nullness checker, which (unsoundly but
         * *usually* safely) assumes that getCause() returns non-null on an ExecutionException.
         */
        CombinedFuture.this.setException(((ExecutionException) error).getCause());
      } else if (error instanceof CancellationException) {
        cancel(false);
      } else {
        CombinedFuture.this.setException(error);
      }
    }

    abstract void setValue(@ParametricNullness T value);
  }

  @WeakOuter
  private final class AsyncCallableInterruptibleTask
      extends CombinedFutureInterruptibleTask<ListenableFuture<V>> {
    private final AsyncCallable<V> callable;

    AsyncCallableInterruptibleTask(AsyncCallable<V> callable, Executor listenerExecutor) {
      super(listenerExecutor);
      this.callable = checkNotNull(callable);
    }

    @Override
    ListenableFuture<V> runInterruptibly() throws Exception {
      ListenableFuture<V> result = callable.call();
      return checkNotNull(
          result,
          "AsyncCallable.call returned null instead of a Future. "
              + "Did you mean to return immediateFuture(null)? %s",
          callable);
    }

    @Override
    void setValue(ListenableFuture<V> value) {
      CombinedFuture.this.setFuture(value);
    }

    @Override
    String toPendingString() {
      return callable.toString();
    }
  }

  @WeakOuter
  private final class CallableInterruptibleTask extends CombinedFutureInterruptibleTask<V> {
    private final Callable<V> callable;

    CallableInterruptibleTask(Callable<V> callable, Executor listenerExecutor) {
      super(listenerExecutor);
      this.callable = checkNotNull(callable);
    }

    @Override
    @ParametricNullness
    V runInterruptibly() throws Exception {
      return callable.call();
    }

    @Override
    void setValue(@ParametricNullness V value) {
      CombinedFuture.this.set(value);
    }

    @Override
    String toPendingString() {
      return callable.toString();
    }
  }
}
