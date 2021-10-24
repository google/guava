/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.util.concurrent;

import static com.google.common.truth.Truth.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;

/**
 * Test for {@link WrappingScheduledExecutorService}
 *
 * @author Luke Sandberg
 */
public class WrappingScheduledExecutorServiceTest extends TestCase {
  private static final Runnable DO_NOTHING =
      new Runnable() {
        @Override
        public void run() {}
      };

  public void testSchedule() {
    MockExecutor mock = new MockExecutor();
    TestExecutor testExecutor = new TestExecutor(mock);

    @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
    Future<?> possiblyIgnoredError = testExecutor.schedule(DO_NOTHING, 10, TimeUnit.MINUTES);
    mock.assertLastMethodCalled("scheduleRunnable", 10, TimeUnit.MINUTES);

    @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
    Future<?> possiblyIgnoredError1 =
        testExecutor.schedule(Executors.callable(DO_NOTHING), 5, TimeUnit.SECONDS);
    mock.assertLastMethodCalled("scheduleCallable", 5, TimeUnit.SECONDS);
  }

  public void testSchedule_repeating() {
    MockExecutor mock = new MockExecutor();
    TestExecutor testExecutor = new TestExecutor(mock);
    @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
    Future<?> possiblyIgnoredError =
        testExecutor.scheduleWithFixedDelay(DO_NOTHING, 100, 10, TimeUnit.MINUTES);
    mock.assertLastMethodCalled("scheduleWithFixedDelay", 100, 10, TimeUnit.MINUTES);

    @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
    Future<?> possiblyIgnoredError1 =
        testExecutor.scheduleAtFixedRate(DO_NOTHING, 3, 7, TimeUnit.SECONDS);
    mock.assertLastMethodCalled("scheduleAtFixedRate", 3, 7, TimeUnit.SECONDS);
  }

  private static final class WrappedCallable<T> implements Callable<T> {
    private final Callable<T> delegate;

    public WrappedCallable(Callable<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public T call() throws Exception {
      return delegate.call();
    }
  }

  private static final class WrappedRunnable implements Runnable {
    private final Runnable delegate;

    public WrappedRunnable(Runnable delegate) {
      this.delegate = delegate;
    }

    @Override
    public void run() {
      delegate.run();
    }
  }

  private static final class TestExecutor extends WrappingScheduledExecutorService {
    public TestExecutor(MockExecutor mock) {
      super(mock);
    }

    @Override
    protected <T> Callable<T> wrapTask(Callable<T> callable) {
      return new WrappedCallable<T>(callable);
    }

    @Override
    protected Runnable wrapTask(Runnable command) {
      return new WrappedRunnable(command);
    }
  }

  private static final class MockExecutor implements ScheduledExecutorService {
    String lastMethodCalled = "";
    long lastInitialDelay;
    long lastDelay;
    TimeUnit lastUnit;

    void assertLastMethodCalled(String method, long delay, TimeUnit unit) {
      assertEquals(method, lastMethodCalled);
      assertEquals(delay, lastDelay);
      assertEquals(unit, lastUnit);
    }

    void assertLastMethodCalled(String method, long initialDelay, long delay, TimeUnit unit) {
      assertEquals(method, lastMethodCalled);
      assertEquals(initialDelay, lastInitialDelay);
      assertEquals(delay, lastDelay);
      assertEquals(unit, lastUnit);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      assertThat(command).isInstanceOf(WrappedRunnable.class);
      lastMethodCalled = "scheduleRunnable";
      lastDelay = delay;
      lastUnit = unit;
      return null;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
      assertThat(callable).isInstanceOf(WrappedCallable.class);
      lastMethodCalled = "scheduleCallable";
      lastDelay = delay;
      lastUnit = unit;
      return null;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
        Runnable command, long initialDelay, long period, TimeUnit unit) {
      assertThat(command).isInstanceOf(WrappedRunnable.class);
      lastMethodCalled = "scheduleAtFixedRate";
      lastInitialDelay = initialDelay;
      lastDelay = period;
      lastUnit = unit;
      return null;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
        Runnable command, long initialDelay, long delay, TimeUnit unit) {
      assertThat(command).isInstanceOf(WrappedRunnable.class);
      lastMethodCalled = "scheduleWithFixedDelay";
      lastInitialDelay = initialDelay;
      lastDelay = delay;
      lastUnit = unit;
      return null;
    }

    // No need to test these methods as they are handled by WrappingExecutorServiceTest
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(
        Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws ExecutionException, InterruptedException {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws ExecutionException, InterruptedException, TimeoutException {
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
    public void shutdown() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Runnable> shutdownNow() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Future<?> submit(Runnable task) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void execute(Runnable command) {
      throw new UnsupportedOperationException();
    }
  }
}
