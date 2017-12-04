/*
 * Copyright (C) 2012 The Guava Authors
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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.AbstractListeningExecutorService;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Factory methods for {@link ExecutorService} for testing.
 *
 * @author Chris Nokleberg
 * @since 14.0
 */
@Beta
@GwtIncompatible
public final class TestingExecutors {
  private TestingExecutors() {}

  /**
   * Returns a {@link ScheduledExecutorService} that never executes anything.
   *
   * <p>The {@code shutdownNow} method of the returned executor always returns an empty list despite
   * the fact that everything is still technically awaiting execution. The {@code getDelay} method
   * of any {@link ScheduledFuture} returned by the executor will always return the max long value
   * instead of the time until the user-specified delay.
   */
  public static ListeningScheduledExecutorService noOpScheduledExecutor() {
    return new NoOpScheduledExecutorService();
  }

  /**
   * Creates a scheduled executor service that runs each task in the thread that invokes {@code
   * execute/submit/schedule}, as in {@link CallerRunsPolicy}. This applies both to individually
   * submitted tasks and to collections of tasks submitted via {@code invokeAll}, {@code invokeAny},
   * {@code schedule}, {@code scheduleAtFixedRate}, and {@code scheduleWithFixedDelay}. In the case
   * of tasks submitted by {@code invokeAll} or {@code invokeAny}, tasks will run serially on the
   * calling thread. Tasks are run to completion before a {@code Future} is returned to the caller
   * (unless the executor has been shutdown).
   *
   * <p>The returned executor is backed by the executor returned by {@link
   * MoreExecutors#newDirectExecutorService} and subject to the same constraints.
   *
   * <p>Although all tasks are immediately executed in the thread that submitted the task, this
   * {@code ExecutorService} imposes a small locking overhead on each task submission in order to
   * implement shutdown and termination behavior.
   *
   * <p>Because of the nature of single-thread execution, the methods {@code scheduleAtFixedRate}
   * and {@code scheduleWithFixedDelay} are not supported by this class and will throw an
   * UnsupportedOperationException.
   *
   * <p>The implementation deviates from the {@code ExecutorService} specification with regards to
   * the {@code shutdownNow} method. First, "best-effort" with regards to canceling running tasks is
   * implemented as "no-effort". No interrupts or other attempts are made to stop threads executing
   * tasks. Second, the returned list will always be empty, as any submitted task is considered to
   * have started execution. This applies also to tasks given to {@code invokeAll} or {@code
   * invokeAny} which are pending serial execution, even the subset of the tasks that have not yet
   * started execution. It is unclear from the {@code ExecutorService} specification if these should
   * be included, and it's much easier to implement the interpretation that they not be. Finally, a
   * call to {@code shutdown} or {@code shutdownNow} may result in concurrent calls to {@code
   * invokeAll/invokeAny} throwing RejectedExecutionException, although a subset of the tasks may
   * already have been executed.
   *
   * @since 15.0
   */
  public static SameThreadScheduledExecutorService sameThreadScheduledExecutor() {
    return new SameThreadScheduledExecutorService();
  }

  private static final class NoOpScheduledExecutorService extends AbstractListeningExecutorService
      implements ListeningScheduledExecutorService {

    private volatile boolean shutdown;

    @Override
    public void shutdown() {
      shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
      shutdown();
      return ImmutableList.of();
    }

    @Override
    public boolean isShutdown() {
      return shutdown;
    }

    @Override
    public boolean isTerminated() {
      return shutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return true;
    }

    @Override
    public void execute(Runnable runnable) {}

    @Override
    public <V> ListenableScheduledFuture<V> schedule(
        Callable<V> callable, long delay, TimeUnit unit) {
      return NeverScheduledFuture.create();
    }

    @Override
    public ListenableScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      return NeverScheduledFuture.create();
    }

    @Override
    public ListenableScheduledFuture<?> scheduleAtFixedRate(
        Runnable command, long initialDelay, long period, TimeUnit unit) {
      return NeverScheduledFuture.create();
    }

    @Override
    public ListenableScheduledFuture<?> scheduleWithFixedDelay(
        Runnable command, long initialDelay, long delay, TimeUnit unit) {
      return NeverScheduledFuture.create();
    }

    private static class NeverScheduledFuture<V> extends AbstractFuture<V>
        implements ListenableScheduledFuture<V> {

      static <V> NeverScheduledFuture<V> create() {
        return new NeverScheduledFuture<V>();
      }

      @Override
      public long getDelay(TimeUnit unit) {
        return Long.MAX_VALUE;
      }

      @Override
      public int compareTo(Delayed other) {
        return Longs.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
      }
    }
  }
}
