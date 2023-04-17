/*
 * Copyright (C) 2014 The Guava Authors
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
import static com.google.common.util.concurrent.Callables.returning;
import static com.google.common.util.concurrent.Futures.getDone;
import static com.google.common.util.concurrent.TestPlatform.verifyThreadWasNotInterrupted;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Test case for {@link TrustedListenableFutureTask}. */
@GwtCompatible(emulated = true)
public class TrustedListenableFutureTaskTest extends TestCase {

  public void testSuccessful() throws Exception {
    TrustedListenableFutureTask<Integer> task = TrustedListenableFutureTask.create(returning(2));
    assertFalse(task.isDone());
    task.run();
    assertTrue(task.isDone());
    assertFalse(task.isCancelled());
    assertEquals(2, getDone(task).intValue());
  }

  public void testCancelled() throws Exception {
    TrustedListenableFutureTask<Integer> task = TrustedListenableFutureTask.create(returning(2));
    assertFalse(task.isDone());
    task.cancel(false);
    assertTrue(task.isDone());
    assertTrue(task.isCancelled());
    assertFalse(task.wasInterrupted());
    try {
      getDone(task);
      fail();
    } catch (CancellationException expected) {
    }
    verifyThreadWasNotInterrupted();
  }

  public void testFailed() throws Exception {
    final Exception e = new Exception();
    TrustedListenableFutureTask<Integer> task =
        TrustedListenableFutureTask.create(
            new Callable<Integer>() {
              @Override
              public Integer call() throws Exception {
                throw e;
              }
            });
    task.run();
    assertTrue(task.isDone());
    assertFalse(task.isCancelled());
    try {
      getDone(task);
      fail();
    } catch (ExecutionException executionException) {
      assertThat(executionException).hasCauseThat().isEqualTo(e);
    }
  }

  @GwtIncompatible // blocking wait
  public void testCancel_interrupted() throws Exception {
    final AtomicBoolean interruptedExceptionThrown = new AtomicBoolean();
    final CountDownLatch enterLatch = new CountDownLatch(1);
    final CountDownLatch exitLatch = new CountDownLatch(1);
    final TrustedListenableFutureTask<Integer> task =
        TrustedListenableFutureTask.create(
            new Callable<Integer>() {
              @Override
              public Integer call() throws Exception {
                enterLatch.countDown();
                try {
                  new CountDownLatch(1).await(); // wait forever
                  throw new AssertionError();
                } catch (InterruptedException e) {
                  interruptedExceptionThrown.set(true);
                  throw e;
                } finally {
                }
              }
            });
    assertFalse(task.isDone());
    Thread thread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  task.run();
                } finally {
                  exitLatch.countDown();
                }
              }
            });
    thread.start();
    enterLatch.await();
    assertFalse(task.isDone());
    task.cancel(true);
    assertTrue(task.isDone());
    assertTrue(task.isCancelled());
    assertTrue(task.wasInterrupted());
    try {
      task.get();
      fail();
    } catch (CancellationException expected) {
    }
    exitLatch.await();
    assertTrue(interruptedExceptionThrown.get());
  }

  @GwtIncompatible // blocking wait
  public void testRunIdempotency() throws Exception {
    final int numThreads = 10;
    final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    for (int i = 0; i < 1000; i++) {
      final AtomicInteger counter = new AtomicInteger();
      final TrustedListenableFutureTask<Integer> task =
          TrustedListenableFutureTask.create(
              new Callable<Integer>() {
                @Override
                public Integer call() {
                  return counter.incrementAndGet();
                }
              });
      final CyclicBarrier barrier = new CyclicBarrier(numThreads + 1);
      Runnable wrapper =
          new Runnable() {
            @Override
            public void run() {
              awaitUnchecked(barrier);
              task.run();
              awaitUnchecked(barrier);
            }
          };
      for (int j = 0; j < 10; j++) {
        executor.execute(wrapper);
      }
      barrier.await(); // release the threads!
      barrier.await(); // wait for them all to complete
      assertEquals(1, task.get().intValue());
      assertEquals(1, counter.get());
    }
    executor.shutdown();
  }

  @GwtIncompatible // blocking wait
  public void testToString() throws Exception {
    final CountDownLatch enterLatch = new CountDownLatch(1);
    final CountDownLatch exitLatch = new CountDownLatch(1);
    final TrustedListenableFutureTask<@Nullable Void> task =
        TrustedListenableFutureTask.create(
            new Callable<@Nullable Void>() {
              @Override
              public @Nullable Void call() throws Exception {
                enterLatch.countDown();
                new CountDownLatch(1).await(); // wait forever
                return null;
              }
            });
    assertFalse(task.isDone());
    Thread thread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  task.run();
                } finally {
                  exitLatch.countDown();
                }
              }
            },
            "Custom thread name");
    thread.start();
    enterLatch.await();
    assertFalse(task.isDone());
    String result = task.toString();
    assertThat(result).contains("Custom thread name");
    task.cancel(true);
    exitLatch.await();
  }

  @GwtIncompatible // used only in GwtIncompatible tests
  private void awaitUnchecked(CyclicBarrier barrier) {
    try {
      barrier.await();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
