/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.util.concurrent.testing;

import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A ScheduledExecutorService that executes all scheduled actions immediately in the calling thread.
 *
 * <p>See {@link TestingExecutors#sameThreadScheduledExecutor()} for a full list of constraints.
 *
 * @author John Sirois
 * @author Zach van Schouwen
 */
@GwtIncompatible
class SameThreadScheduledExecutorService extends AbstractExecutorService
    implements ListeningScheduledExecutorService {

  private final ListeningExecutorService delegate = newDirectExecutorService();

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    Preconditions.checkNotNull(unit, "unit must not be null!");
    return delegate.awaitTermination(timeout, unit);
  }

  @Override
  public <T> ListenableFuture<T> submit(Callable<T> task) {
    Preconditions.checkNotNull(task, "task must not be null!");
    return delegate.submit(task);
  }

  @Override
  public <T> ListenableFuture<T> submit(Runnable task, T result) {
    Preconditions.checkNotNull(task, "task must not be null!");
    Preconditions.checkNotNull(result, "result must not be null!");
    return delegate.submit(task, result);
  }

  @Override
  public ListenableFuture<?> submit(Runnable task) {
    Preconditions.checkNotNull(task, "task must not be null!");
    return delegate.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    Preconditions.checkNotNull(tasks, "tasks must not be null!");
    return delegate.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    Preconditions.checkNotNull(tasks, "tasks must not be null!");
    Preconditions.checkNotNull(unit, "unit must not be null!");
    return delegate.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    Preconditions.checkNotNull(tasks, "tasks must not be null!");
    return delegate.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    Preconditions.checkNotNull(tasks, "tasks must not be null!");
    Preconditions.checkNotNull(unit, "unit must not be null!");
    return delegate.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    Preconditions.checkNotNull(command, "command must not be null!");
    delegate.execute(command);
  }

  @Override
  public ListenableScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    Preconditions.checkNotNull(command, "command must not be null");
    Preconditions.checkNotNull(unit, "unit must not be null!");
    return schedule(java.util.concurrent.Executors.callable(command), delay, unit);
  }

  @Override
  public <V> ListenableScheduledFuture<V> schedule(
      Callable<V> callable, long delay, TimeUnit unit) {
    Preconditions.checkNotNull(callable, "callable must not be null!");
    Preconditions.checkNotNull(unit, "unit must not be null!");
    ListenableFuture<V> delegateFuture = submit(callable);
    return new ImmediateScheduledFuture<>(delegateFuture);
  }

  private static class ImmediateScheduledFuture<V> extends SimpleForwardingListenableFuture<V>
      implements ListenableScheduledFuture<V> {
    private ExecutionException exception;

    protected ImmediateScheduledFuture(ListenableFuture<V> future) {
      super(future);
    }

    @Override
    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      Preconditions.checkNotNull(unit, "unit must not be null!");
      return get();
    }

    @Override
    public long getDelay(TimeUnit unit) {
      Preconditions.checkNotNull(unit, "unit must not be null!");
      return 0;
    }

    @Override
    public int compareTo(Delayed other) {
      Preconditions.checkNotNull(other, "other must not be null!");
      return 0;
    }
  }

  @Override
  public ListenableScheduledFuture<?> scheduleAtFixedRate(
      Runnable command, long initialDelay, long period, TimeUnit unit) {
    throw new UnsupportedOperationException("scheduleAtFixedRate is not supported.");
  }

  @Override
  public ListenableScheduledFuture<?> scheduleWithFixedDelay(
      Runnable command, long initialDelay, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException("scheduleWithFixedDelay is not supported.");
  }
}
