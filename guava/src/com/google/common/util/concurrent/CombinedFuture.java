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

import javax.annotation.Nullable;

/**
 * Aggregate future that computes its value by calling a callable.
 */
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
        ImmutableCollection<? extends ListenableFuture<? extends Object>> futures,
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
  private abstract class CombinedFutureInterruptibleTask extends InterruptibleTask {
    private final Executor listenerExecutor;
    volatile boolean thrownByExecute = true;

    public CombinedFutureInterruptibleTask(Executor listenerExecutor) {
      this.listenerExecutor = checkNotNull(listenerExecutor);
    }

    @Override
    final void runInterruptibly() {
      thrownByExecute = false;
      // Ensure we haven't been cancelled or already run.
      if (!isDone()) {
        try {
          setValue();
        } catch (ExecutionException e) {
          setException(e.getCause());
        } catch (CancellationException e) {
          cancel(false);
        } catch (Throwable e) {
          setException(e);
        }
      }
    }

    @Override
    final boolean wasInterrupted() {
      return CombinedFuture.this.wasInterrupted();
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

    abstract void setValue() throws Exception;
  }

  @WeakOuter
  private final class AsyncCallableInterruptibleTask extends CombinedFutureInterruptibleTask {
    private final AsyncCallable<V> callable;

    public AsyncCallableInterruptibleTask(AsyncCallable<V> callable, Executor listenerExecutor) {
      super(listenerExecutor);
      this.callable = checkNotNull(callable);
    }

    @Override
    void setValue() throws Exception {
      setFuture(callable.call());
    }
  }

  @WeakOuter
  private final class CallableInterruptibleTask extends CombinedFutureInterruptibleTask {
    private final Callable<V> callable;

    public CallableInterruptibleTask(Callable<V> callable, Executor listenerExecutor) {
      super(listenerExecutor);
      this.callable = checkNotNull(callable);
    }

    @Override
    void setValue() throws Exception {
      set(callable.call());
    }
  }
}
