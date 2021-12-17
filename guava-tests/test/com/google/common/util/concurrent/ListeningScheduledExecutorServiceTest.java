/*
 * Copyright (C) 2020 The Guava Authors
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;

/** Tests for default methods of the interface. */
public class ListeningScheduledExecutorServiceTest extends TestCase {

  private Runnable recordedCommand;
  private long recordedDelay;
  private long recordedInterval;
  private TimeUnit recordedTimeUnit;

  private final ListeningScheduledExecutorService executorService = new FakeExecutorService();

  public void testScheduleRunnable() throws Exception {
    Runnable command = () -> {};

    ListenableScheduledFuture<?> future = executorService.schedule(command, Duration.ofSeconds(12));

    assertThat(future.get()).isEqualTo("schedule");
    assertThat(recordedCommand).isSameInstanceAs(command);
    assertThat(recordedTimeUnit).isEqualTo(TimeUnit.NANOSECONDS);
    assertThat(Duration.ofNanos(recordedDelay)).isEqualTo(Duration.ofSeconds(12));
  }

  public void testScheduleCallable() throws Exception {
    Callable<String> callable = () -> "hello";

    ListenableScheduledFuture<String> future =
        executorService.schedule(callable, Duration.ofMinutes(12));

    assertThat(future.get()).isEqualTo("hello");
    assertThat(recordedTimeUnit).isEqualTo(TimeUnit.NANOSECONDS);
    assertThat(Duration.ofNanos(recordedDelay)).isEqualTo(Duration.ofMinutes(12));
  }

  public void testScheduleAtFixedRate() throws Exception {
    Runnable command = () -> {};

    ListenableScheduledFuture<?> future =
        executorService.scheduleAtFixedRate(command, Duration.ofDays(2), Duration.ofHours(4));

    assertThat(future.get()).isEqualTo("scheduleAtFixedRate");
    assertThat(recordedCommand).isSameInstanceAs(command);
    assertThat(recordedTimeUnit).isEqualTo(TimeUnit.NANOSECONDS);
    assertThat(Duration.ofNanos(recordedDelay)).isEqualTo(Duration.ofDays(2));
    assertThat(Duration.ofNanos(recordedInterval)).isEqualTo(Duration.ofHours(4));
  }

  public void testScheduleWithFixedDelay() throws Exception {
    Runnable command = () -> {};

    ListenableScheduledFuture<?> future =
        executorService.scheduleWithFixedDelay(command, Duration.ofDays(8), Duration.ofHours(16));

    assertThat(future.get()).isEqualTo("scheduleWithFixedDelay");
    assertThat(recordedCommand).isSameInstanceAs(command);
    assertThat(recordedTimeUnit).isEqualTo(TimeUnit.NANOSECONDS);
    assertThat(Duration.ofNanos(recordedDelay)).isEqualTo(Duration.ofDays(8));
    assertThat(Duration.ofNanos(recordedInterval)).isEqualTo(Duration.ofHours(16));
  }

  private class FakeExecutorService extends AbstractListeningExecutorService
      implements ListeningScheduledExecutorService {
    @Override
    public ListenableScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      recordedCommand = command;
      recordedDelay = delay;
      recordedTimeUnit = unit;
      return ImmediateScheduledFuture.of("schedule");
    }

    @Override
    public <V> ListenableScheduledFuture<V> schedule(
        Callable<V> callable, long delay, TimeUnit unit) {
      recordedDelay = delay;
      recordedTimeUnit = unit;
      try {
        return ImmediateScheduledFuture.of(callable.call());
      } catch (Exception e) {
        return ImmediateScheduledFuture.failed(e);
      }
    }

    @Override
    public ListenableScheduledFuture<?> scheduleAtFixedRate(
        Runnable command, long initialDelay, long period, TimeUnit unit) {
      recordedCommand = command;
      recordedDelay = initialDelay;
      recordedInterval = period;
      recordedTimeUnit = unit;
      return ImmediateScheduledFuture.of("scheduleAtFixedRate");
    }

    @Override
    public ListenableScheduledFuture<?> scheduleWithFixedDelay(
        Runnable command, long initialDelay, long delay, TimeUnit unit) {
      recordedCommand = command;
      recordedDelay = initialDelay;
      recordedInterval = delay;
      recordedTimeUnit = unit;
      return ImmediateScheduledFuture.of("scheduleWithFixedDelay");
    }

    @Override
    public void execute(Runnable runnable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Runnable> shutdownNow() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTerminated() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      throw new UnsupportedOperationException();
    }
  }

  private static class ImmediateScheduledFuture<V> extends SimpleForwardingListenableFuture<V>
      implements ListenableScheduledFuture<V> {
    static <V> ListenableScheduledFuture<V> of(V value) {
      return new ImmediateScheduledFuture<>(Futures.immediateFuture(value));
    }

    static <V> ListenableScheduledFuture<V> failed(Throwable t) {
      return new ImmediateScheduledFuture<>(Futures.immediateFailedFuture(t));
    }

    ImmediateScheduledFuture(ListenableFuture<V> delegate) {
      super(delegate);
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return 0;
    }

    @Override
    public int compareTo(Delayed other) {
      return 0;
    }
  }
}
