/*
 * Copyright (C) 2013 The Guava Authors
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

import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for {@link ExecutionQueue}.
 *
 * @author Luke Sandberg
 */
public class ExecutionQueueTest extends TestCase {
  private final ExecutionQueue queue = new ExecutionQueue();

  public void testAddAndExecuteMultipleRounds() throws InterruptedException {
    Executor executor = MoreExecutors.sameThreadExecutor();
    addAndExecuteMultipleTimes(executor);
  }

  public void testAddAndExecuteMultipleRounds_multipleThreads() throws InterruptedException {
    ExecutorService executor = Executors.newCachedThreadPool();
    try {
      addAndExecuteMultipleTimes(executor);
    } finally {
      executor.shutdown();
    }
  }

  /**
   * Test that tasks are submitted to the executor in the correct order even when there are many
   * concurrent calls to execute and add.
   */

  public void testAddAndConcurrentExecute() throws InterruptedException {
    final int numThreads = 20;
    final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    final CountDownLatch latch = new CountDownLatch(numThreads);
    // make sure all the threads are started
    for (int i = 0; i < numThreads; i++) {
      executor.execute(new Runnable() {
        @Override public void run() {
          latch.countDown();
        }
      });
    }
    latch.await();
    final AtomicInteger integer = new AtomicInteger();
    final int numTasks = 10000;
    final CountDownLatch taskLatch = new CountDownLatch(numTasks);
    final CyclicBarrier startBarrier = new CyclicBarrier(numThreads);
    executor.execute(new Runnable() {
      @Override public void run() {
        // We are only interested in testing the order in which things are added to the executor,
        // but by using a single threaded executor we know that the runnables are executed in the
        // same order that they are added to the queue managed by this executor.
        final ExecutorService callbackExecutor = singleThreadedExecutorService();
        // We want to delay starting until all the threads calling execute have started.
        try {
          startBarrier.await();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        for (int i = 0; i < numTasks; i++) {
          final int expectedCount = i;
          queue.add(new Runnable() {
            @Override public void run() {
              integer.compareAndSet(expectedCount, expectedCount + 1);
              taskLatch.countDown();
              if (taskLatch.getCount() == 0) {
                callbackExecutor.shutdown();
              }
            }
          }, callbackExecutor);
        }
      }

    });
    for (int i = 0; i < numThreads - 1; i++) {
      executor.execute(new Runnable() {
        @Override public void run() {
          // We want to delay starting until all the threads calling execute have started.
          try {
            startBarrier.await();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          while (taskLatch.getCount() > 0) {
            queue.execute();
          }
        }});
    }
    taskLatch.await();  // wait for them all to finish
    assertEquals(numTasks, integer.get());
    executor.shutdown();
  }

  private ExecutorService singleThreadedExecutorService() {
    final ExecutorService callbackExecutor = new ThreadPoolExecutor(1, 1,
        0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>()) {
      @Override public void execute(Runnable command) {
        // Yields to try to force more thread interleavings
        Thread.yield();
        super.execute(command);
        Thread.yield();
      }
    };
    return callbackExecutor;
  }

  private void addAndExecuteMultipleTimes(Executor executor) throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      CountDownLatch countDownLatch = new CountDownLatch(3);
      queue.add(new CountDownRunnable(countDownLatch), executor);
      queue.add(new CountDownRunnable(countDownLatch), executor);
      queue.add(new CountDownRunnable(countDownLatch), executor);
      assertEquals(countDownLatch.getCount(), 3L);
      queue.execute();
      assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));
    }
  }

  private class CountDownRunnable implements Runnable {
    final CountDownLatch countDownLatch;

    CountDownRunnable(CountDownLatch countDownLatch) {
      this.countDownLatch = countDownLatch;
    }

    @Override public void run() {
      countDownLatch.countDown();
    }
  }
}
