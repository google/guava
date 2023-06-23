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
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;

public final class ListeningExecutorServiceTest extends TestCase {

  private Collection<? extends Callable<?>> recordedTasks;
  private long recordedTimeout;
  private TimeUnit recordedTimeUnit;

  private final ListeningExecutorService executorService = new FakeExecutorService();

  public void testInvokeAny() throws Exception {
    Set<Callable<String>> tasks = Collections.singleton(() -> "invokeAny");

    String result = executorService.invokeAny(tasks, Duration.ofSeconds(7));

    assertThat(result).isEqualTo("invokeAny");
    assertThat(recordedTasks).isSameInstanceAs(tasks);
    assertThat(recordedTimeUnit).isEqualTo(NANOSECONDS);
    assertThat(Duration.ofNanos(recordedTimeout)).isEqualTo(Duration.ofSeconds(7));
  }

  public void testInvokeAll() throws Exception {
    Set<Callable<String>> tasks = Collections.singleton(() -> "invokeAll");

    List<Future<String>> result = executorService.invokeAll(tasks, Duration.ofDays(365));

    assertThat(result).hasSize(1);
    assertThat(Futures.getDone(result.get(0))).isEqualTo("invokeAll");
    assertThat(recordedTasks).isSameInstanceAs(tasks);
    assertThat(recordedTimeUnit).isEqualTo(NANOSECONDS);
    assertThat(Duration.ofNanos(recordedTimeout)).isEqualTo(Duration.ofDays(365));
  }

  public void testAwaitTermination() throws Exception {
    boolean result = executorService.awaitTermination(Duration.ofMinutes(144));

    assertThat(result).isTrue();
    assertThat(recordedTimeUnit).isEqualTo(NANOSECONDS);
    assertThat(Duration.ofNanos(recordedTimeout)).isEqualTo(Duration.ofMinutes(144));
  }

  private class FakeExecutorService extends AbstractListeningExecutorService {
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      recordedTasks = tasks;
      recordedTimeout = timeout;
      recordedTimeUnit = unit;
      try {
        return tasks.iterator().next().call();
      } catch (Exception e) {
        throw new ExecutionException(e);
      }
    }

    @Override
    public <T> List<Future<T>> invokeAll(
        Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException {
      recordedTasks = tasks;
      recordedTimeout = timeout;
      recordedTimeUnit = unit;
      try {
        return Collections.singletonList(immediateFuture(tasks.iterator().next().call()));
      } catch (Exception e) {
        return Collections.singletonList(immediateFailedFuture(e));
      }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      recordedTimeout = timeout;
      recordedTimeUnit = unit;
      return true;
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
  }
}
