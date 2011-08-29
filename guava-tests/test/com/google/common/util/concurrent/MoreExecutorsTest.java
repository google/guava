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

package com.google.common.util.concurrent;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for MoreExecutors.
 *
 * @author Kyle Littlefield (klittle)
 */
public class MoreExecutorsTest extends TestCase {

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
            } catch (Throwable Throwable) {
              throwableFromOtherThread.set(Throwable);
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
        } catch (Throwable Throwable) {
          throwableFromOtherThread.set(Throwable);
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
}
