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
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.AbstractListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

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
public final class TestingExecutors {
  private TestingExecutors() {}

  /**
   * Returns a {@link ScheduledExecutorService} that never executes anything.
   *
   * <p>The {@code shutdownNow} method of the returned executor always returns an empty list despite
   * the fact that everything is still technically awaiting execution.
   * The {@code getDelay} method of any {@link ScheduledFuture} returned by the executor will always
   * return the max long value instead of the time until the user-specified delay.
   */
  public static ListeningScheduledExecutorService noOpScheduledExecutor() {
    return new NoOpScheduledExecutorService();
  }

  private static final class NoOpScheduledExecutorService
      extends AbstractListeningExecutorService implements ListeningScheduledExecutorService {

    private volatile boolean shutdown;

    @Override public void shutdown() {
      shutdown = true;
    }

    @Override public List<Runnable> shutdownNow() {
      shutdown();
      return ImmutableList.of();
    }

    @Override public boolean isShutdown() {
      return shutdown;
    }

    @Override public boolean isTerminated() {
      return shutdown;
    }

    @Override public boolean awaitTermination(long timeout, TimeUnit unit) {
      return true;
    }

    @Override public void execute(Runnable runnable) {}

    @Override public <V> ScheduledFuture<V> schedule(
        Callable<V> callable, long delay, TimeUnit unit) {
      return NeverScheduledFuture.create();
    }

    @Override public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      return NeverScheduledFuture.create();
    }

    @Override public ScheduledFuture<?> scheduleAtFixedRate(
        Runnable command, long initialDelay, long period, TimeUnit unit) {
      return NeverScheduledFuture.create();
    }

    @Override public ScheduledFuture<?> scheduleWithFixedDelay(
        Runnable command, long initialDelay, long delay, TimeUnit unit) {
      return NeverScheduledFuture.create();
    }

    private static class NeverScheduledFuture<V>
        extends AbstractFuture<V> implements ScheduledFuture<V> {

      static <V> NeverScheduledFuture<V> create() {
        return new NeverScheduledFuture<V>();
      }

      @Override public long getDelay(TimeUnit unit) {
        return Long.MAX_VALUE;
      }

      @Override public int compareTo(Delayed other) {
        return Longs.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
      }
    }
  }
}
