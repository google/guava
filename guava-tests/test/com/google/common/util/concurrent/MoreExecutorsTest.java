/*
 * Copyright (C) 2008 The Guava Authors
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

/*
 * Portions of this file are modified versions of
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/test/tck/AbstractExecutorServiceTest.java?revision=1.30
 * which contained the following notice:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

package com.google.common.util.concurrent;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.util.concurrent.MoreExecutors.invokeAnyImpl;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static com.google.common.util.concurrent.MoreExecutors.renamingDecorator;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.truth0.Truth.ASSERT;

import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.testing.ClassSanityTester;
import com.google.common.util.concurrent.MoreExecutors.Application;

import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for MoreExecutors.
 *
 * @author Kyle Littlefield (klittle)
 */
public class MoreExecutorsTest extends JSR166TestCase {

  private static final Runnable EMPTY_RUNNABLE = new Runnable() {
    @Override public void run() {}
  };

  public void testSameThreadExecutorServiceInThreadExecution()
      throws Exception {
    final ListeningExecutorService executor =
        MoreExecutors.sameThreadExecutor();
    final ThreadLocal<Integer> threadLocalCount = new ThreadLocal<Integer>() {
      @Override
      protected Integer initialValue() {
        return 0;
      }
    };
    final AtomicReference<Throwable> throwableFromOtherThread =
        new AtomicReference<Throwable>(null);
    final Runnable incrementTask =
        new Runnable() {
          @Override
          public void run() {
            threadLocalCount.set(threadLocalCount.get() + 1);
          }
        };

    Thread otherThread = new Thread(
        new Runnable() {
          @Override
          public void run() {
            try {
              Future<?> future = executor.submit(incrementTask);
              assertTrue(future.isDone());
              assertEquals(1, threadLocalCount.get().intValue());
            } catch (Throwable t) {
              throwableFromOtherThread.set(t);
            }
          }
        });

    otherThread.start();

    ListenableFuture<?> future = executor.submit(incrementTask);
    assertTrue(future.isDone());
    assertListenerRunImmediately(future);
    assertEquals(1, threadLocalCount.get().intValue());
    otherThread.join(1000);
    assertEquals(Thread.State.TERMINATED, otherThread.getState());
    Throwable throwable = throwableFromOtherThread.get();
    assertNull("Throwable from other thread: "
        + (throwable == null ? null : Throwables.getStackTraceAsString(throwable)),
        throwableFromOtherThread.get());
  }

  public void testSameThreadExecutorInvokeAll() throws Exception {
    final ExecutorService executor = MoreExecutors.sameThreadExecutor();
    final ThreadLocal<Integer> threadLocalCount = new ThreadLocal<Integer>() {
      @Override
      protected Integer initialValue() {
        return 0;
      }
    };

    final Callable<Integer> incrementTask = new Callable<Integer>() {
      @Override
      public Integer call() {
        int i = threadLocalCount.get();
        threadLocalCount.set(i + 1);
        return i;
      }
    };

    List<Future<Integer>> futures =
        executor.invokeAll(Collections.nCopies(10, incrementTask));

    for (int i = 0; i < 10; i++) {
      Future<Integer> future = futures.get(i);
      assertTrue("Task should have been run before being returned", future.isDone());
      assertEquals(i, future.get().intValue());
    }

    assertEquals(10, threadLocalCount.get().intValue());
  }

  public void testSameThreadExecutorServiceTermination()
      throws Exception {
    final ExecutorService executor = MoreExecutors.sameThreadExecutor();
    final CyclicBarrier barrier = new CyclicBarrier(2);
    final AtomicReference<Throwable> throwableFromOtherThread =
        new AtomicReference<Throwable>(null);
    final Runnable doNothingRunnable = new Runnable() {
        @Override public void run() {
        }};

    Thread otherThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Future<?> future = executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
              // WAIT #1
              barrier.await(1, TimeUnit.SECONDS);

              // WAIT #2
              barrier.await(1, TimeUnit.SECONDS);
              assertTrue(executor.isShutdown());
              assertFalse(executor.isTerminated());

              // WAIT #3
              barrier.await(1, TimeUnit.SECONDS);
              return null;
            }
          });
          assertTrue(future.isDone());
          assertTrue(executor.isShutdown());
          assertTrue(executor.isTerminated());
        } catch (Throwable t) {
          throwableFromOtherThread.set(t);
        }
      }});

    otherThread.start();

    // WAIT #1
    barrier.await(1, TimeUnit.SECONDS);
    assertFalse(executor.isShutdown());
    assertFalse(executor.isTerminated());

    executor.shutdown();
    assertTrue(executor.isShutdown());
    try {
      executor.submit(doNothingRunnable);
      fail("Should have encountered RejectedExecutionException");
    } catch (RejectedExecutionException ex) {
      // good to go
    }
    assertFalse(executor.isTerminated());

    // WAIT #2
    barrier.await(1, TimeUnit.SECONDS);
    assertFalse(executor.awaitTermination(20, TimeUnit.MILLISECONDS));

    // WAIT #3
    barrier.await(1, TimeUnit.SECONDS);
    assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    assertTrue(executor.awaitTermination(0, TimeUnit.SECONDS));
    assertTrue(executor.isShutdown());
    try {
      executor.submit(doNothingRunnable);
      fail("Should have encountered RejectedExecutionException");
    } catch (RejectedExecutionException ex) {
      // good to go
    }
    assertTrue(executor.isTerminated());

    otherThread.join(1000);
    assertEquals(Thread.State.TERMINATED, otherThread.getState());
    Throwable throwable = throwableFromOtherThread.get();
    assertNull("Throwable from other thread: "
        + (throwable == null ? null : Throwables.getStackTraceAsString(throwable)),
        throwableFromOtherThread.get());
  }

  public void testSameThreadExecutor_shutdownNow() {
    ExecutorService executor = MoreExecutors.sameThreadExecutor();
    assertEquals(ImmutableList.of(), executor.shutdownNow());
    assertTrue(executor.isShutdown());
  }

  public void testExecuteAfterShutdown() {
    ExecutorService executor = MoreExecutors.sameThreadExecutor();
    executor.shutdown();
    try {
      executor.execute(EMPTY_RUNNABLE);
      fail();
    } catch (RejectedExecutionException expected) {}
  }

  public <T> void testListeningExecutorServiceInvokeAllJavadocCodeCompiles()
      throws Exception {
    ListeningExecutorService executor = MoreExecutors.sameThreadExecutor();
    List<Callable<T>> tasks = ImmutableList.of();
    @SuppressWarnings("unchecked") // guaranteed by invokeAll contract
    List<ListenableFuture<T>> futures = (List) executor.invokeAll(tasks);
  }

  public void testListeningDecorator() throws Exception {
    ListeningExecutorService service =
        listeningDecorator(MoreExecutors.sameThreadExecutor());
    assertSame(service, listeningDecorator(service));
    List<Callable<String>> callables =
        ImmutableList.of(Callables.returning("x"));
    List<Future<String>> results;

    results = service.invokeAll(callables);
    ASSERT.that(getOnlyElement(results)).isA(ListenableFutureTask.class);

    results = service.invokeAll(callables, 1, SECONDS);
    ASSERT.that(getOnlyElement(results)).isA(ListenableFutureTask.class);

    /*
     * TODO(cpovirk): move ForwardingTestCase somewhere common, and use it to
     * test the forwarded methods
     */
  }

  public void testListeningDecorator_noWrapExecuteTask() {
    ExecutorService delegate = mock(ExecutorService.class);
    ListeningExecutorService service = listeningDecorator(delegate);
    Runnable task = new Runnable() {
      @Override
      public void run() {}
    };
    service.execute(task);
    verify(delegate).execute(task);
  }

  public void testListeningDecorator_scheduleSuccess() throws Exception {
    final CountDownLatch completed = new CountDownLatch(1);
    ScheduledThreadPoolExecutor delegate = new ScheduledThreadPoolExecutor(1) {
      @Override
      protected void afterExecute(Runnable r, Throwable t) {
        completed.countDown();
      }
    };
    ListeningScheduledExecutorService service = listeningDecorator(delegate);
    ListenableFuture<?> future =
        service.schedule(Callables.returning(null), 1, TimeUnit.MILLISECONDS);

    /*
     * Wait not just until the Future's value is set (as in future.get()) but
     * also until ListeningScheduledExecutorService's wrapper task is done
     * executing listeners, as detected by yielding control to afterExecute.
     */
    completed.await();
    assertTrue(future.isDone());
    assertListenerRunImmediately(future);
    assertEquals(0, delegate.getQueue().size());
  }

  public void testListeningDecorator_scheduleFailure() throws Exception {
    ScheduledThreadPoolExecutor delegate = new ScheduledThreadPoolExecutor(1);
    ListeningScheduledExecutorService service = listeningDecorator(delegate);
    RuntimeException ex = new RuntimeException();
    ListenableFuture<?> future =
        service.schedule(new ThrowingRunnable(0, ex), 1, TimeUnit.MILLISECONDS);
    assertExecutionException(future, ex);
    assertEquals(0, delegate.getQueue().size());
  }

  public void testListeningDecorator_schedulePeriodic() throws Exception {
    ScheduledThreadPoolExecutor delegate = new ScheduledThreadPoolExecutor(1);
    ListeningScheduledExecutorService service = listeningDecorator(delegate);
    RuntimeException ex = new RuntimeException();

    ListenableFuture<?> future;

    ThrowingRunnable runnable = new ThrowingRunnable(5, ex);
    future = service.scheduleAtFixedRate(runnable, 1, 1, TimeUnit.MILLISECONDS);
    assertExecutionException(future, ex);
    assertEquals(5, runnable.count);
    assertEquals(0, delegate.getQueue().size());

    runnable = new ThrowingRunnable(5, ex);
    future = service.scheduleWithFixedDelay(runnable, 1, 1, TimeUnit.MILLISECONDS);
    assertExecutionException(future, ex);
    assertEquals(5, runnable.count);
    assertEquals(0, delegate.getQueue().size());
  }

  public void testListeningDecorator_cancelled() throws Exception {
    ScheduledThreadPoolExecutor delegate = new ScheduledThreadPoolExecutor(1);
    BlockingQueue<?> delegateQueue = delegate.getQueue();
    ListeningScheduledExecutorService service = listeningDecorator(delegate);
    ListenableFuture<?> future;
    ScheduledFuture<?> delegateFuture;

    Runnable runnable = new Runnable() {
      @Override public void run() {}
    };

    future = service.schedule(runnable, 5 * 60, TimeUnit.SECONDS);
    future.cancel(true);
    assertTrue(future.isCancelled());
    delegateFuture = (ScheduledFuture<?>) delegateQueue.element();
    assertTrue(delegateFuture.isCancelled());

    delegateQueue.clear();

    future = service.scheduleAtFixedRate(runnable, 5 * 60, 5 * 60, TimeUnit.SECONDS);
    future.cancel(true);
    assertTrue(future.isCancelled());
    delegateFuture = (ScheduledFuture<?>) delegateQueue.element();
    assertTrue(delegateFuture.isCancelled());

    delegateQueue.clear();

    future = service.scheduleWithFixedDelay(runnable, 5 * 60, 5 * 60, TimeUnit.SECONDS);
    future.cancel(true);
    assertTrue(future.isCancelled());
    delegateFuture = (ScheduledFuture<?>) delegateQueue.element();
    assertTrue(delegateFuture.isCancelled());
  }

  private static final class ThrowingRunnable implements Runnable {
    final int throwAfterCount;
    final RuntimeException thrown;
    int count;

    ThrowingRunnable(int throwAfterCount, RuntimeException thrown) {
      this.throwAfterCount = throwAfterCount;
      this.thrown = thrown;
    }

    @Override
    public void run() {
      if (++count >= throwAfterCount) {
        throw thrown;
      }
    }
  }

  private static void assertExecutionException(Future<?> future, Exception expectedCause)
      throws Exception {
    try {
      future.get();
      fail("Expected ExecutionException");
    } catch (ExecutionException e) {
      assertSame(expectedCause, e.getCause());
    }
  }

  /**
   * invokeAny(null) throws NPE
   */
  public void testInvokeAnyImpl_nullTasks() throws Exception {
    ListeningExecutorService e = sameThreadExecutor();
    try {
      invokeAnyImpl(e, null, false, 0);
      shouldThrow();
    } catch (NullPointerException success) {
    } finally {
      joinPool(e);
    }
  }

  /**
   * invokeAny(empty collection) throws IAE
   */
  public void testInvokeAnyImpl_emptyTasks() throws Exception {
    ListeningExecutorService e = sameThreadExecutor();
    try {
      invokeAnyImpl(e, new ArrayList<Callable<String>>(), false, 0);
      shouldThrow();
    } catch (IllegalArgumentException success) {
    } finally {
      joinPool(e);
    }
  }

  /**
   * invokeAny(c) throws NPE if c has null elements
   */
  public void testInvokeAnyImpl_nullElement() throws Exception {
    ListeningExecutorService e = sameThreadExecutor();
    List<Callable<Integer>> l = new ArrayList<Callable<Integer>>();
    l.add(new Callable<Integer>() {
      @Override public Integer call() {
          throw new ArithmeticException("/ by zero");
      }
    });
    l.add(null);
    try {
      invokeAnyImpl(e, l, false, 0);
      shouldThrow();
    } catch (NullPointerException success) {
    } finally {
      joinPool(e);
    }
  }

  /**
   * invokeAny(c) throws ExecutionException if no task in c completes
   */
  public void testInvokeAnyImpl_noTaskCompletes() throws Exception {
    ListeningExecutorService e = sameThreadExecutor();
    List<Callable<String>> l = new ArrayList<Callable<String>>();
    l.add(new NPETask());
    try {
      invokeAnyImpl(e, l, false, 0);
      shouldThrow();
    } catch (ExecutionException success) {
      assertTrue(success.getCause() instanceof NullPointerException);
    } finally {
      joinPool(e);
    }
  }

  /**
   * invokeAny(c) returns result of some task in c if at least one completes
   */
  public void testInvokeAnyImpl() throws Exception {
    ListeningExecutorService e = sameThreadExecutor();
    try {
      List<Callable<String>> l = new ArrayList<Callable<String>>();
      l.add(new StringTask());
      l.add(new StringTask());
      String result = invokeAnyImpl(e, l, false, 0);
      assertSame(TEST_STRING, result);
    } finally {
      joinPool(e);
    }
  }

  private static void assertListenerRunImmediately(ListenableFuture<?> future) {
    CountingRunnable listener = new CountingRunnable();
    future.addListener(listener, sameThreadExecutor());
    assertEquals(1, listener.count);
  }

  private static final class CountingRunnable implements Runnable {
    int count;

    @Override
    public void run() {
      count++;
    }
  }

  public void testAddDelayedShutdownHook_success() throws InterruptedException {
    TestApplication application = new TestApplication();
    ExecutorService service = mock(ExecutorService.class);
    application.addDelayedShutdownHook(service, 2, TimeUnit.SECONDS);
    verify(service, Mockito.never()).shutdown();
    application.shutdown();
    InOrder shutdownFirst = Mockito.inOrder(service);
    shutdownFirst.verify(service).shutdown();
    shutdownFirst.verify(service).awaitTermination(2, TimeUnit.SECONDS);
  }

  public void testAddDelayedShutdownHook_interrupted() throws InterruptedException {
    TestApplication application = new TestApplication();
    ExecutorService service = mock(ExecutorService.class);
    application.addDelayedShutdownHook(service, 2, TimeUnit.SECONDS);
    when(service.awaitTermination(2, TimeUnit.SECONDS)).thenThrow(new InterruptedException());
    application.shutdown();
    verify(service).shutdown();
  }

  public void testGetExitingExcutorService_executorSetToUseDaemonThreads() {
    TestApplication application = new TestApplication();
    ThreadPoolExecutor executor = new ThreadPoolExecutor(
        1, 2, 3, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1));
    assertNotNull(application.getExitingExecutorService(executor));
    assertTrue(executor.getThreadFactory().newThread(EMPTY_RUNNABLE).isDaemon());
  }

  public void testGetExitingExcutorService_executorDelegatesToOriginal() {
    TestApplication application = new TestApplication();
    ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
    ThreadFactory threadFactory = mock(ThreadFactory.class);
    when(executor.getThreadFactory()).thenReturn(threadFactory);
    application.getExitingExecutorService(executor).execute(EMPTY_RUNNABLE);
    verify(executor).execute(EMPTY_RUNNABLE);
  }

  public void testGetExitingExcutorService_shutdownHookRegistered() throws InterruptedException {
    TestApplication application = new TestApplication();
    ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
    ThreadFactory threadFactory = mock(ThreadFactory.class);
    when(executor.getThreadFactory()).thenReturn(threadFactory);
    application.getExitingExecutorService(executor);
    application.shutdown();
    verify(executor).shutdown();
  }

  public void testGetExitingScheduledExcutorService_executorSetToUseDaemonThreads() {
    TestApplication application = new TestApplication();
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    assertNotNull(application.getExitingScheduledExecutorService(executor));
    assertTrue(executor.getThreadFactory().newThread(EMPTY_RUNNABLE).isDaemon());
  }

  public void testGetExitingScheduledExcutorService_executorDelegatesToOriginal() {
    TestApplication application = new TestApplication();
    ScheduledThreadPoolExecutor executor = mock(ScheduledThreadPoolExecutor.class);
    ThreadFactory threadFactory = mock(ThreadFactory.class);
    when(executor.getThreadFactory()).thenReturn(threadFactory);
    application.getExitingScheduledExecutorService(executor).execute(EMPTY_RUNNABLE);
    verify(executor).execute(EMPTY_RUNNABLE);
  }

  public void testGetScheduledExitingExcutorService_shutdownHookRegistered()
      throws InterruptedException {
    TestApplication application = new TestApplication();
    ScheduledThreadPoolExecutor executor = mock(ScheduledThreadPoolExecutor.class);
    ThreadFactory threadFactory = mock(ThreadFactory.class);
    when(executor.getThreadFactory()).thenReturn(threadFactory);
    application.getExitingScheduledExecutorService(executor);
    application.shutdown();
    verify(executor).shutdown();
  }

  public void testPlatformThreadFactory_default() {
    ThreadFactory factory = MoreExecutors.platformThreadFactory();
    assertNotNull(factory);
    // Executors#defaultThreadFactory() may return a new instance each time.
    assertEquals(factory.getClass(), Executors.defaultThreadFactory().getClass());
  }

  public void testThreadRenaming() {
    Executor renamingExecutor = renamingDecorator(sameThreadExecutor(),
        Suppliers.ofInstance("FooBar"));
    String oldName = Thread.currentThread().getName();
    renamingExecutor.execute(new Runnable() {
      @Override public void run() {
        assertEquals("FooBar", Thread.currentThread().getName());
      }});
    assertEquals(oldName, Thread.currentThread().getName());
  }

  public void testExecutors_nullCheck() throws Exception {
    new ClassSanityTester()
        .setDefault(RateLimiter.class, RateLimiter.create(1.0))
        .forAllPublicStaticMethods(MoreExecutors.class)
        .thatReturn(Executor.class)
        .testNulls();
  }

  private static class TestApplication extends Application {
    private final List<Thread> hooks = Lists.newArrayList();

    @Override synchronized void addShutdownHook(Thread hook) {
      hooks.add(hook);
    }

    synchronized void shutdown() throws InterruptedException {
      for (Thread hook : hooks) {
        hook.start();
      }
      for (Thread hook : hooks) {
        hook.join();
      }
    }
  }

  /* Half of a 1-second timeout in nanoseconds */
  private static final long HALF_SECOND_NANOS = NANOSECONDS.convert(1L, SECONDS) / 2;

  public void testShutdownAndAwaitTermination_immediateShutdown() throws Exception {
    ExecutorService service = Executors.newSingleThreadExecutor();
    assertTrue(shutdownAndAwaitTermination(service, 1L, SECONDS));
    assertTrue(service.isTerminated());
  }

  public void testShutdownAndAwaitTermination_immediateShutdownInternal() throws Exception {
    ExecutorService service = mock(ExecutorService.class);
    when(service.awaitTermination(HALF_SECOND_NANOS, NANOSECONDS)).thenReturn(true);
    when(service.isTerminated()).thenReturn(true);
    assertTrue(shutdownAndAwaitTermination(service, 1L, SECONDS));
    verify(service).shutdown();
    verify(service).awaitTermination(HALF_SECOND_NANOS, NANOSECONDS);
  }

  public void testShutdownAndAwaitTermination_forcedShutDownInternal() throws Exception {
    ExecutorService service = mock(ExecutorService.class);
    when(service.awaitTermination(HALF_SECOND_NANOS, NANOSECONDS))
        .thenReturn(false).thenReturn(true);
    when(service.isTerminated()).thenReturn(true);
    assertTrue(shutdownAndAwaitTermination(service, 1L, SECONDS));
    verify(service).shutdown();
    verify(service, times(2)).awaitTermination(HALF_SECOND_NANOS, NANOSECONDS);
    verify(service).shutdownNow();
  }

  public void testShutdownAndAwaitTermination_nonTerminationInternal() throws Exception {
    ExecutorService service = mock(ExecutorService.class);
    when(service.awaitTermination(HALF_SECOND_NANOS, NANOSECONDS))
        .thenReturn(false).thenReturn(false);
    assertFalse(shutdownAndAwaitTermination(service, 1L, SECONDS));
    verify(service).shutdown();
    verify(service, times(2)).awaitTermination(HALF_SECOND_NANOS, NANOSECONDS);
    verify(service).shutdownNow();
  }

  public void testShutdownAndAwaitTermination_interruptedInternal() throws Exception {
    final ExecutorService service = mock(ExecutorService.class);
    when(service.awaitTermination(HALF_SECOND_NANOS, NANOSECONDS))
        .thenThrow(new InterruptedException());

    final AtomicBoolean terminated = new AtomicBoolean();
    // we need to keep this in a flag because t.isInterrupted() returns false after t.join()
    final AtomicBoolean interrupted = new AtomicBoolean();
    // we need to use another thread because it will be interrupted and thus using
    // the current one, owned by JUnit, would make the test fail
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        terminated.set(shutdownAndAwaitTermination(service, 1L, SECONDS));
        interrupted.set(Thread.currentThread().isInterrupted());
      }
    });
    thread.start();
    thread.join();
    verify(service).shutdown();
    verify(service).awaitTermination(HALF_SECOND_NANOS, NANOSECONDS);
    verify(service).shutdownNow();
    assertTrue(interrupted.get());
    assertFalse(terminated.get());
  }
}
