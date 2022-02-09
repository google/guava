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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.newSequentialExecutor;
import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;

/**
 * Tests {@link SequentialExecutor}.
 *
 * @author JJ Furman
 */
public class SequentialExecutorTest extends TestCase {

  private static class FakeExecutor implements Executor {
    Queue<Runnable> tasks = Queues.newArrayDeque();

    @Override
    public void execute(Runnable command) {
      tasks.add(command);
    }

    boolean hasNext() {
      return !tasks.isEmpty();
    }

    void runNext() {
      assertTrue("expected at least one task to run", hasNext());
      tasks.remove().run();
    }

    void runAll() {
      while (hasNext()) {
        runNext();
      }
    }
  }

  private FakeExecutor fakePool;
  private SequentialExecutor e;

  @Override
  public void setUp() {
    fakePool = new FakeExecutor();
    e = new SequentialExecutor(fakePool);
  }

  public void testConstructingWithNullExecutor_fails() {
    try {
      new SequentialExecutor(null);
      fail("Should have failed with NullPointerException.");
    } catch (NullPointerException expected) {
    }
  }

  public void testBasics() {
    final AtomicInteger totalCalls = new AtomicInteger();
    Runnable intCounter =
        new Runnable() {
          @Override
          public void run() {
            totalCalls.incrementAndGet();
            // Make sure that no other tasks are scheduled to run while this is running.
            assertFalse(fakePool.hasNext());
          }
        };

    assertFalse(fakePool.hasNext());
    e.execute(intCounter);
    // A task should have been scheduled
    assertTrue(fakePool.hasNext());
    e.execute(intCounter);
    // Our executor hasn't run any tasks yet.
    assertEquals(0, totalCalls.get());
    fakePool.runAll();
    assertEquals(2, totalCalls.get());
    // Queue is empty so no runner should be scheduled.
    assertFalse(fakePool.hasNext());

    // Check that execute can be safely repeated
    e.execute(intCounter);
    e.execute(intCounter);
    e.execute(intCounter);
    // No change yet.
    assertEquals(2, totalCalls.get());
    fakePool.runAll();
    assertEquals(5, totalCalls.get());
    assertFalse(fakePool.hasNext());
  }

  public void testOrdering() {
    final List<Integer> callOrder = Lists.newArrayList();

    class FakeOp implements Runnable {
      final int op;

      FakeOp(int op) {
        this.op = op;
      }

      @Override
      public void run() {
        callOrder.add(op);
      }
    }

    e.execute(new FakeOp(0));
    e.execute(new FakeOp(1));
    e.execute(new FakeOp(2));
    fakePool.runAll();

    assertEquals(ImmutableList.of(0, 1, 2), callOrder);
  }

  public void testRuntimeException_doesNotStopExecution() {

    final AtomicInteger numCalls = new AtomicInteger();

    Runnable runMe =
        new Runnable() {
          @Override
          public void run() {
            numCalls.incrementAndGet();
            throw new RuntimeException("FAKE EXCEPTION!");
          }
        };

    e.execute(runMe);
    e.execute(runMe);
    fakePool.runAll();

    assertEquals(2, numCalls.get());
  }

  public void testInterrupt_beforeRunRestoresInterruption() throws Exception {
    // Run a task on the composed Executor that interrupts its thread (i.e. this thread).
    fakePool.execute(
        new Runnable() {
          @Override
          public void run() {
            Thread.currentThread().interrupt();
          }
        });
    // Run a task that expects that it is not interrupted while it is running.
    e.execute(
        new Runnable() {
          @Override
          public void run() {
            assertThat(Thread.currentThread().isInterrupted()).isFalse();
          }
        });

    // Run these together.
    fakePool.runAll();

    // Check that this thread has been marked as interrupted again now that the thread has been
    // returned by SequentialExecutor. Clear the bit while checking so that the test doesn't hose
    // JUnit or some other test case.
    assertThat(Thread.interrupted()).isTrue();
  }

  public void testInterrupt_doesNotInterruptSubsequentTask() throws Exception {
    // Run a task that interrupts its thread (i.e. this thread).
    e.execute(
        new Runnable() {
          @Override
          public void run() {
            Thread.currentThread().interrupt();
          }
        });
    // Run a task that expects that it is not interrupted while it is running.
    e.execute(
        new Runnable() {
          @Override
          public void run() {
            assertThat(Thread.currentThread().isInterrupted()).isFalse();
          }
        });

    // Run those tasks together.
    fakePool.runAll();

    // Check that the interruption of a SequentialExecutor's task is restored to the thread once
    // it is yielded. Clear the bit while checking so that the test doesn't hose JUnit or some other
    // test case.
    assertThat(Thread.interrupted()).isTrue();
  }

  public void testInterrupt_doesNotStopExecution() {

    final AtomicInteger numCalls = new AtomicInteger();

    Runnable runMe =
        new Runnable() {
          @Override
          public void run() {
            numCalls.incrementAndGet();
          }
        };

    Thread.currentThread().interrupt();

    e.execute(runMe);
    e.execute(runMe);
    fakePool.runAll();

    assertEquals(2, numCalls.get());

    assertTrue(Thread.interrupted());
  }

  public void testDelegateRejection() {
    final AtomicInteger numCalls = new AtomicInteger();
    final AtomicBoolean reject = new AtomicBoolean(true);
    final SequentialExecutor executor =
        new SequentialExecutor(
            new Executor() {
              @Override
              public void execute(Runnable r) {
                if (reject.get()) {
                  throw new RejectedExecutionException();
                }
                r.run();
              }
            });
    Runnable task =
        new Runnable() {
          @Override
          public void run() {
            numCalls.incrementAndGet();
          }
        };
    try {
      executor.execute(task);
      fail();
    } catch (RejectedExecutionException expected) {
    }
    assertEquals(0, numCalls.get());
    reject.set(false);
    executor.execute(task);
    assertEquals(1, numCalls.get());
  }

  /*
   * Under Android, MyError propagates up and fails the test?
   *
   * TODO(b/218700094): Does this matter to prod users, or is it just a feature of our testing
   * environment? If the latter, maybe write a custom Executor that avoids failing the test when it
   * sees an Error?
   */
  @AndroidIncompatible
  public void testTaskThrowsError() throws Exception {
    class MyError extends Error {}
    final CyclicBarrier barrier = new CyclicBarrier(2);
    // we need to make sure the error gets thrown on a different thread.
    ExecutorService service = Executors.newSingleThreadExecutor();
    try {
      final SequentialExecutor executor = new SequentialExecutor(service);
      Runnable errorTask =
          new Runnable() {
            @Override
            public void run() {
              throw new MyError();
            }
          };
      Runnable barrierTask =
          new Runnable() {
            @Override
            public void run() {
              try {
                barrier.await();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            }
          };
      executor.execute(errorTask);
      service.execute(barrierTask); // submit directly to the service
      // the barrier task runs after the error task so we know that the error has been observed by
      // SequentialExecutor by the time the barrier is satified
      barrier.await(1, TimeUnit.SECONDS);
      executor.execute(barrierTask);
      // timeout means the second task wasn't even tried
      barrier.await(1, TimeUnit.SECONDS);
    } finally {
      service.shutdown();
    }
  }

  public void testRejectedExecutionThrownWithMultipleCalls() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final SettableFuture<?> future = SettableFuture.create();
    final Executor delegate =
        new Executor() {
          @Override
          public void execute(Runnable task) {
            if (future.set(null)) {
              awaitUninterruptibly(latch);
            }
            throw new RejectedExecutionException();
          }
        };
    final SequentialExecutor executor = new SequentialExecutor(delegate);
    final ExecutorService blocked = Executors.newCachedThreadPool();
    Future<?> first =
        blocked.submit(
            new Runnable() {
              @Override
              public void run() {
                executor.execute(Runnables.doNothing());
              }
            });
    future.get(10, TimeUnit.SECONDS);
    try {
      executor.execute(Runnables.doNothing());
      fail();
    } catch (RejectedExecutionException expected) {
    }
    latch.countDown();
    try {
      first.get(10, TimeUnit.SECONDS);
      fail();
    } catch (ExecutionException expected) {
      assertThat(expected).hasCauseThat().isInstanceOf(RejectedExecutionException.class);
    }
  }

  public void testToString() {
    final Runnable[] currentTask = new Runnable[1];
    final Executor delegate =
        new Executor() {
          @Override
          public void execute(Runnable task) {
            currentTask[0] = task;
            task.run();
            currentTask[0] = null;
          }

          @Override
          public String toString() {
            return "theDelegate";
          }
        };
    Executor sequential1 = newSequentialExecutor(delegate);
    Executor sequential2 = newSequentialExecutor(delegate);
    assertThat(sequential1.toString()).contains("theDelegate");
    assertThat(sequential1.toString()).isNotEqualTo(sequential2.toString());
    final String[] whileRunningToString = new String[1];
    sequential1.execute(
        new Runnable() {
          @Override
          public void run() {
            whileRunningToString[0] = "" + currentTask[0];
          }

          @Override
          public String toString() {
            return "my runnable's toString";
          }
        });
    assertThat(whileRunningToString[0]).contains("my runnable's toString");
  }
}
