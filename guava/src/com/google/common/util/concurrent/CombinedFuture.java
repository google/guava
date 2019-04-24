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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableCollection;
import com.google.j2objc.annotations.WeakOuter;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Aggregate future that computes its value by calling a callable. */
@GwtCompatible
final class CombinedFuture<V> extends AggregateFuture<Object, V> {
  CombinedFuture(
      ImmutableCollection<? extends ListenableFuture<?>> futures,
      boolean allMustSucceed,
      Executor listenerExecutor,
      AsyncCallable<V> callable) {
    init(
        new CombinedFutureRunningState(
            futures,
            allMustSucceed,
            new AsyncCallableInterruptibleTask(callable, listenerExecutor)));
  }

  CombinedFuture(
      ImmutableCollection<? extends ListenableFuture<?>> futures,
      boolean allMustSucceed,
      Executor listenerExecutor,
      Callable<V> callable) {
    init(
        new CombinedFutureRunningState(
            futures, allMustSucceed, new CallableInterruptibleTask(callable, listenerExecutor)));
  }

  private final class CombinedFutureRunningState extends RunningState {
    private CombinedFutureInterruptibleTask task;

    CombinedFutureRunningState(
        ImmutableCollection<? extends ListenableFuture<?>> futures,
        boolean allMustSucceed,
        CombinedFutureInterruptibleTask task) {
      super(futures, allMustSucceed, false);
      this.task = task;
    }

    @Override
    void collectOneValue(boolean allMustSucceed, int index, @Nullable Object returnValue) {}

    @Override
    void handleAllCompleted() {
      CombinedFutureInterruptibleTask localTask = task;
      if (localTask != null) {
        localTask.execute();
      } else {
        checkState(isDone());
      }
    }

    @Override
    void releaseResourcesAfterFailure() {
      super.releaseResourcesAfterFailure();
      this.task = null;
    }

    @Override
    void interruptTask() {
      CombinedFutureInterruptibleTask localTask = task;
      if (localTask != null) {
        localTask.interruptTask();
      }
    }
  }

  @WeakOuter
  private abstract class CombinedFutureInterruptibleTask<T> extends InterruptibleTask<T> {
    private final Executor listenerExecutor;
    boolean thrownByExecute = true;

    public CombinedFutureInterruptibleTask(Executor listenerExecutor) {
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
        if (thrownByExecute) {
          setException(e);
        }
      }
    }

    @Override
    final void afterRanInterruptibly(T result, Throwable error) {
      if (error != null) {
        if (error instanceof ExecutionException) {
          setException(error.getCause());
        } else if (error instanceof CancellationException) {
          cancel(false);
        } else {
          setException(error);
        }
      } else {
        setValue(result);
      }
    }

    abstract void setValue(T value);
  }

  @WeakOuter
  private final class AsyncCallableInterruptibleTask
      extends CombinedFutureInterruptibleTask<ListenableFuture<V>> {
    private final AsyncCallable<V> callable;

    public AsyncCallableInterruptibleTask(AsyncCallable<V> callable, Executor listenerExecutor) {
      super(listenerExecutor);
      this.callable = checkNotNull(callable);
    }

    @Override
    ListenableFuture<V> runInterruptibly() throws Exception {
      thrownByExecute = false;
      ListenableFuture<V> result = callable.call();
      return checkNotNull(
          result,
          "AsyncCallable.call returned null instead of a Future. "
              + "Did you mean to return immediateFuture(null)? %s",
          callable);
    }

    @Override
    void setValue(ListenableFuture<V> value) {
      setFuture(value);
    }

    @Override
    String toPendingString() {
      return callable.toString();
    }
  }

  @WeakOuter
  private final class CallableInterruptibleTask extends CombinedFutureInterruptibleTask<V> {
    private final Callable<V> callable;

    public CallableInterruptibleTask(Callable<V> callable, Executor listenerExecutor) {
      super(listenerExecutor);
      this.callable = checkNotNull(callable);
    }

    @Override
    V runInterruptibly() throws Exception {
      thrownByExecute = false;
      return callable.call();
    }

    @Override
    void setValue(V value) {
      CombinedFuture.this.set(value);
    }

    @Override
    String toPendingString() {
      return callable.toString();
    }
  }
}
