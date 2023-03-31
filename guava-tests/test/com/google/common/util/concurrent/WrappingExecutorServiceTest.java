/*
 * Copyright (C) 2011 The Guava Authors
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
import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static com.google.common.util.concurrent.Runnables.doNothing;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;

/**
 * Test for {@link WrappingExecutorService}
 *
 * @author Chris Nokleberg
 */
public class WrappingExecutorServiceTest extends TestCase {
  private static final String RESULT_VALUE = "ran";
  // Uninteresting delegations
  public void testDelegations() throws InterruptedException {
    MockExecutor mock = new MockExecutor();
    TestExecutor testExecutor = new TestExecutor(mock);
    assertFalse(testExecutor.awaitTermination(10, TimeUnit.MILLISECONDS));
    mock.assertLastMethodCalled("awaitTermination");
    assertFalse(testExecutor.isTerminated());
    mock.assertLastMethodCalled("isTerminated");
    assertFalse(testExecutor.isShutdown());
    mock.assertLastMethodCalled("isShutdown");
    testExecutor.shutdown();
    mock.assertLastMethodCalled("shutdown");
    List<Runnable> list = testExecutor.shutdownNow();
    mock.assertLastMethodCalled("shutdownNow");
    assertEquals(ImmutableList.of(), list);
  }

  public void testExecute() {
    MockExecutor mock = new MockExecutor();
    TestExecutor testExecutor = new TestExecutor(mock);
    testExecutor.execute(doNothing());
    mock.assertLastMethodCalled("execute");
  }

  public void testSubmit() throws InterruptedException, ExecutionException {
    {
      MockExecutor mock = new MockExecutor();
      TestExecutor testExecutor = new TestExecutor(mock);
      Future<?> f = testExecutor.submit(doNothing());
      mock.assertLastMethodCalled("submit");
      f.get();
    }
    {
      MockExecutor mock = new MockExecutor();
      TestExecutor testExecutor = new TestExecutor(mock);
      Future<String> f = testExecutor.submit(doNothing(), RESULT_VALUE);
      mock.assertLastMethodCalled("submit");
      assertEquals(RESULT_VALUE, f.get());
    }
    {
      MockExecutor mock = new MockExecutor();
      TestExecutor testExecutor = new TestExecutor(mock);
      Callable<String> task = Callables.returning(RESULT_VALUE);
      Future<String> f = testExecutor.submit(task);
      mock.assertLastMethodCalled("submit");
      assertEquals(RESULT_VALUE, f.get());
    }
  }

  public void testInvokeAll() throws InterruptedException, ExecutionException {
    List<Callable<String>> tasks = createTasks(3);
    {
      MockExecutor mock = new MockExecutor();
      TestExecutor testExecutor = new TestExecutor(mock);
      List<Future<String>> futures = testExecutor.invokeAll(tasks);
      mock.assertLastMethodCalled("invokeAll");
      checkResults(futures);
    }
    {
      MockExecutor mock = new MockExecutor();
      TimeUnit unit = TimeUnit.SECONDS;
      long timeout = 5;
      TestExecutor testExecutor = new TestExecutor(mock);
      List<Future<String>> futures = testExecutor.invokeAll(tasks, timeout, unit);
      mock.assertMethodWithTimeout("invokeAll", timeout, unit);
      checkResults(futures);
    }
  }

  public void testInvokeAny() throws InterruptedException, ExecutionException, TimeoutException {
    List<Callable<String>> tasks = createTasks(3);
    {
      MockExecutor mock = new MockExecutor();
      TestExecutor testExecutor = new TestExecutor(mock);
      String s = testExecutor.invokeAny(tasks);
      assertEquals("ran0", s);
      mock.assertLastMethodCalled("invokeAny");
    }
    {
      MockExecutor mock = new MockExecutor();
      TimeUnit unit = TimeUnit.SECONDS;
      long timeout = 5;
      TestExecutor testExecutor = new TestExecutor(mock);
      String s = testExecutor.invokeAny(tasks, timeout, unit);
      assertEquals(RESULT_VALUE + "0", s);
      mock.assertMethodWithTimeout("invokeAny", timeout, unit);
    }
  }

  private static void checkResults(List<Future<String>> futures)
      throws InterruptedException, ExecutionException {
    for (int i = 0; i < futures.size(); i++) {
      assertEquals(RESULT_VALUE + i, futures.get(i).get());
    }
  }

  private static List<Callable<String>> createTasks(int n) {
    List<Callable<String>> callables = Lists.newArrayList();
    for (int i = 0; i < n; i++) {
      callables.add(Callables.returning(RESULT_VALUE + i));
    }
    return callables;
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

  private static final class TestExecutor extends WrappingExecutorService {
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

  // TODO: If this test can ever depend on Mockito or the like, use it instead.
  private static final class MockExecutor implements ExecutorService {
    private String lastMethodCalled = "";
    private long lastTimeoutInMillis = -1;
    private ExecutorService inline = newDirectExecutorService();

    public void assertLastMethodCalled(String method) {
      assertEquals(method, lastMethodCalled);
    }

    public void assertMethodWithTimeout(String method, long timeout, TimeUnit unit) {
      assertLastMethodCalled(method + "Timeout");
      assertEquals(unit.toMillis(timeout), lastTimeoutInMillis);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      lastMethodCalled = "awaitTermination";
      return false;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException {
      lastMethodCalled = "invokeAll";
      assertTaskWrapped(tasks);
      return inline.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(
        Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException {
      assertTaskWrapped(tasks);
      lastMethodCalled = "invokeAllTimeout";
      lastTimeoutInMillis = unit.toMillis(timeout);
      return inline.invokeAll(tasks, timeout, unit);
    }

    // Define the invokeAny methods to invoke the first task
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws ExecutionException, InterruptedException {
      assertTaskWrapped(tasks);
      lastMethodCalled = "invokeAny";
      return inline.submit(Iterables.get(tasks, 0)).get();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws ExecutionException, InterruptedException, TimeoutException {
      assertTaskWrapped(tasks);
      lastMethodCalled = "invokeAnyTimeout";
      lastTimeoutInMillis = unit.toMillis(timeout);
      return inline.submit(Iterables.get(tasks, 0)).get(timeout, unit);
    }

    @Override
    public boolean isShutdown() {
      lastMethodCalled = "isShutdown";
      return false;
    }

    @Override
    public boolean isTerminated() {
      lastMethodCalled = "isTerminated";
      return false;
    }

    @Override
    public void shutdown() {
      lastMethodCalled = "shutdown";
    }

    @Override
    public List<Runnable> shutdownNow() {
      lastMethodCalled = "shutdownNow";
      return ImmutableList.of();
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
      lastMethodCalled = "submit";
      assertThat(task).isInstanceOf(WrappedCallable.class);
      return inline.submit(task);
    }

    @Override
    public Future<?> submit(Runnable task) {
      lastMethodCalled = "submit";
      assertThat(task).isInstanceOf(WrappedRunnable.class);
      return inline.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
      lastMethodCalled = "submit";
      assertThat(task).isInstanceOf(WrappedRunnable.class);
      return inline.submit(task, result);
    }

    @Override
    public void execute(Runnable command) {
      lastMethodCalled = "execute";
      assertThat(command).isInstanceOf(WrappedRunnable.class);
      inline.execute(command);
    }

    private static <T> void assertTaskWrapped(Collection<? extends Callable<T>> tasks) {
      Predicate<Object> p = Predicates.instanceOf(WrappedCallable.class);
      assertTrue(Iterables.all(tasks, p));
    }
  }
}
