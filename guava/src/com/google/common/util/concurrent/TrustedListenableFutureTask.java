/*
 * Copyright (C) 2014 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.j2objc.annotations.WeakOuter;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import javax.annotation.Nullable;

/**
 * A {@link RunnableFuture} that also implements the {@link ListenableFuture} interface.
 *
 * <p>This should be used in preference to {@link ListenableFutureTask} when possible for
 * performance reasons.
 */
@GwtCompatible
class TrustedListenableFutureTask<V> extends AbstractFuture.TrustedFuture<V>
    implements RunnableFuture<V> {

  static <V> TrustedListenableFutureTask<V> create(AsyncCallable<V> callable) {
    return new TrustedListenableFutureTask<V>(callable);
  }

  static <V> TrustedListenableFutureTask<V> create(Callable<V> callable) {
    return new TrustedListenableFutureTask<V>(callable);
  }

  /**
   * Creates a {@code ListenableFutureTask} that will upon running, execute the given
   * {@code Runnable}, and arrange that {@code get} will return the given result on successful
   * completion.
   *
   * @param runnable the runnable task
   * @param result the result to return on successful completion. If you don't need a particular
   *     result, consider using constructions of the form:
   *     {@code ListenableFuture<?> f = ListenableFutureTask.create(runnable,
   *     null)}
   */
  static <V> TrustedListenableFutureTask<V> create(Runnable runnable, @Nullable V result) {
    return new TrustedListenableFutureTask<V>(Executors.callable(runnable, result));
  }

  /*
   * In certain circumstances, this field might theoretically not be visible to an afterDone() call
   * triggered by cancel(). For details, see the comments on the fields of TimeoutFuture.
   */
  private InterruptibleTask task;

  TrustedListenableFutureTask(Callable<V> callable) {
    this.task = new TrustedFutureInterruptibleTask(callable);
  }

  TrustedListenableFutureTask(AsyncCallable<V> callable) {
    this.task = new TrustedFutureInterruptibleAsyncTask(callable);
  }

  @Override
  public void run() {
    InterruptibleTask localTask = task;
    if (localTask != null) {
      localTask.run();
    }
  }

  @Override
  protected void afterDone() {
    super.afterDone();

    if (wasInterrupted()) {
      InterruptibleTask localTask = task;
      if (localTask != null) {
        localTask.interruptTask();
      }
    }

    this.task = null;
  }

  @Override
  protected String pendingToString() {
    InterruptibleTask localTask = task;
    if (localTask != null) {
      return "task=[" + localTask + "]";
    }
    return null;
  }

  @WeakOuter
  private final class TrustedFutureInterruptibleTask extends InterruptibleTask {
    private final Callable<V> callable;

    TrustedFutureInterruptibleTask(Callable<V> callable) {
      this.callable = checkNotNull(callable);
    }

    @Override
    void runInterruptibly() {
      // Ensure we haven't been cancelled or already run.
      if (!isDone()) {
        try {
          TrustedListenableFutureTask.this.set(callable.call());
        } catch (Throwable t) {
          setException(t);
        }
      }
    }

    @Override
    boolean wasInterrupted() {
      return TrustedListenableFutureTask.this.wasInterrupted();
    }

    @Override
    public String toString() {
      return callable.toString();
    }
  }

  @WeakOuter
  private final class TrustedFutureInterruptibleAsyncTask extends InterruptibleTask {
    private final AsyncCallable<V> callable;

    TrustedFutureInterruptibleAsyncTask(AsyncCallable<V> callable) {
      this.callable = checkNotNull(callable);
    }

    @Override
    void runInterruptibly() {
      // Ensure we haven't been cancelled or already run.
      if (!isDone()) {
        try {
          ListenableFuture<V> result = callable.call();
          checkNotNull(
              result,
              "AsyncCallable.call returned null instead of a Future. "
                  + "Did you mean to return immediateFuture(null)?");
          setFuture(result);
        } catch (Throwable t) {
          setException(t);
        }
      }
    }

    @Override
    boolean wasInterrupted() {
      return TrustedListenableFutureTask.this.wasInterrupted();
    }

    @Override
    public String toString() {
      return callable.toString();
    }
  }
}
