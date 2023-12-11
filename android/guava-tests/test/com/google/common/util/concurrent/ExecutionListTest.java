/*
 * Copyright (C) 2007 The Guava Authors
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

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.testing.NullPointerTester;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;

/**
 * Unit tests for {@link ExecutionList}.
 *
 * @author Nishant Thakkar
 * @author Sven Mawson
 */
public class ExecutionListTest extends TestCase {

  private final ExecutionList list = new ExecutionList();

  public void testRunOnPopulatedList() throws Exception {
    Executor exec = Executors.newCachedThreadPool();
    CountDownLatch countDownLatch = new CountDownLatch(3);
    list.add(new MockRunnable(countDownLatch), exec);
    list.add(new MockRunnable(countDownLatch), exec);
    list.add(new MockRunnable(countDownLatch), exec);
    assertEquals(3L, countDownLatch.getCount());

    list.execute();

    // Verify that all of the runnables execute in a reasonable amount of time.
    assertTrue(countDownLatch.await(1L, TimeUnit.SECONDS));
  }

  public void testExecute_idempotent() {
    final AtomicInteger runCalled = new AtomicInteger();
    list.add(
        new Runnable() {
          @Override
          public void run() {
            runCalled.getAndIncrement();
          }
        },
        directExecutor());
    list.execute();
    assertEquals(1, runCalled.get());
    list.execute();
    assertEquals(1, runCalled.get());
  }

  public void testExecute_idempotentConcurrently() throws InterruptedException {
    final CountDownLatch okayToRun = new CountDownLatch(1);
    final AtomicInteger runCalled = new AtomicInteger();
    list.add(
        new Runnable() {
          @Override
          public void run() {
            try {
              okayToRun.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new RuntimeException(e);
            }
            runCalled.getAndIncrement();
          }
        },
        directExecutor());
    Runnable execute =
        new Runnable() {
          @Override
          public void run() {
            list.execute();
          }
        };
    Thread thread1 = new Thread(execute);
    Thread thread2 = new Thread(execute);
    thread1.start();
    thread2.start();
    assertEquals(0, runCalled.get());
    okayToRun.countDown();
    thread1.join();
    thread2.join();
    assertEquals(1, runCalled.get());
  }

  public void testAddAfterRun() throws Exception {
    // Run the previous test
    testRunOnPopulatedList();

    // If it passed, then verify an Add will be executed without calling run
    CountDownLatch countDownLatch = new CountDownLatch(1);
    list.add(new MockRunnable(countDownLatch), Executors.newCachedThreadPool());
    assertTrue(countDownLatch.await(1L, TimeUnit.SECONDS));
  }

  public void testOrdering() throws Exception {
    final AtomicInteger integer = new AtomicInteger();
    for (int i = 0; i < 10; i++) {
      final int expectedCount = i;
      list.add(
          new Runnable() {
            @Override
            public void run() {
              integer.compareAndSet(expectedCount, expectedCount + 1);
            }
          },
          MoreExecutors.directExecutor());
    }
    list.execute();
    assertEquals(10, integer.get());
  }

  private class MockRunnable implements Runnable {
    CountDownLatch countDownLatch;

    MockRunnable(CountDownLatch countDownLatch) {
      this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
      countDownLatch.countDown();
    }
  }

  public void testExceptionsCaught() {
    list.add(THROWING_RUNNABLE, directExecutor());
    list.execute();
    list.add(THROWING_RUNNABLE, directExecutor());
  }

  public void testNulls() {
    new NullPointerTester().testAllPublicInstanceMethods(new ExecutionList());
  }

  private static final Runnable THROWING_RUNNABLE =
      new Runnable() {
        @Override
        public void run() {
          throw new RuntimeException();
        }
      };
}
