/*
 * Copyright (C) 2015 The Guava Authors
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

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;

import javax.annotation.Nullable;

/**
 * Emulation for TrustedListenableFutureTask in GWT.
 */
public class TrustedListenableFutureTask<V> extends AbstractFuture.TrustedFuture<V>
    implements RunnableFuture<V> {

  static <V> TrustedListenableFutureTask<V> create(Callable<V> callable) {
    return new TrustedListenableFutureTask<V>(callable);
  }

  static <V> TrustedListenableFutureTask<V> create(
      Runnable runnable, @Nullable V result) {
    return new TrustedListenableFutureTask<V>(Executors.callable(runnable, result));
  }

  private Callable<V> task;

  TrustedListenableFutureTask(Callable<V> callable) {
    this.task = checkNotNull(callable);
  }

  @Override public void run() {
    try {
      // Ensure we haven't been cancelled or already run.
      if (!isDone()) {
        doRun(task);
      }
    } catch (Throwable t) {
      setException(t);
    } finally {
      task = null;
    }
  }

  @Override public boolean cancel(boolean mayInterruptIfRunning) {
    if (super.cancel(mayInterruptIfRunning)) {
      task = null;
      return true;
    }
    return false;
  }

  void doRun(Callable<V> localTask) throws Exception {
    set(localTask.call());
  }
}
