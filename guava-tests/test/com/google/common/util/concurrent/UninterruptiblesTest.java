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

import static com.google.common.util.concurrent.InterruptionUtil.repeatedlyInterruptTestThread;
import static com.google.common.util.concurrent.Uninterruptibles.awaitTerminationUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.joinUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.putUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.takeUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.tryAcquireUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.tryLockUninterruptibly;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.TearDown;
import com.google.common.testing.TearDownStack;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import junit.framework.TestCase;

/**
 * Tests for {@link Uninterruptibles}.
 *
 * @author Anthony Zana
 */
public class UninterruptiblesTest extends TestCase {
  private static final String EXPECTED_TAKE = "expectedTake";

  /** Timeout to use when we don't expect the timeout to expire. */
  private static final long LONG_DELAY_MS = 2500;

  private static final long SLEEP_SLACK = 2;

  private final TearDownStack tearDownStack = new TearDownStack();

  // NOTE: All durations in these tests are expressed in milliseconds
  @Override
  protected void setUp() {
    // Clear any previous interrupt before running the test.
    if (Thread.currentThread().isInterrupted()) {
      throw new AssertionError(
          "Thread interrupted on test entry. "
              + "Some test probably didn't clear the interrupt state");
    }

    tearDownStack.addTearDown(
        new TearDown() {
          @Override
          public void tearDown() {
            Thread.interrupted();
          }
        });
  }

  @Override
  protected void tearDown() {
    tearDownStack.runTearDown();
  }

  public void testNull() throws Exception {
    new NullPointerTester()
        .setDefault(CountDownLatch.class, new CountDownLatch(0))
        .setDefault(Semaphore.class, new Semaphore(999))
        .testAllPublicStaticMethods(Uninterruptibles.class);
  }

  // IncrementableCountDownLatch.await() tests

  // CountDownLatch.await() tests

  // Condition.await() tests
  public void testConditionAwaitTimeoutExceeded() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Condition condition = TestCondition.create();

    boolean signaledBeforeTimeout = awaitUninterruptibly(condition, 500, MILLISECONDS);

    assertFalse(signaledBeforeTimeout);
    assertAtLeastTimePassed(stopwatch, 500);
    assertNotInterrupted();
  }

  public void testConditionAwaitTimeoutNotExceeded() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Condition condition = TestCondition.createAndSignalAfter(500, MILLISECONDS);

    boolean signaledBeforeTimeout = awaitUninterruptibly(condition, 1500, MILLISECONDS);

    assertTrue(signaledBeforeTimeout);
    assertTimeNotPassed(stopwatch, LONG_DELAY_MS);
    assertNotInterrupted();
  }

  public void testConditionAwaitInterruptedTimeoutExceeded() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Condition condition = TestCondition.create();
    requestInterruptIn(500);

    boolean signaledBeforeTimeout = awaitUninterruptibly(condition, 1000, MILLISECONDS);

    assertFalse(signaledBeforeTimeout);
    assertAtLeastTimePassed(stopwatch, 1000);
    assertInterrupted();
  }

  public void testConditionAwaitInterruptedTimeoutNotExceeded() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Condition condition = TestCondition.createAndSignalAfter(1000, MILLISECONDS);
    requestInterruptIn(500);

    boolean signaledBeforeTimeout = awaitUninterruptibly(condition, 1500, MILLISECONDS);

    assertTrue(signaledBeforeTimeout);
    assertTimeNotPassed(stopwatch, LONG_DELAY_MS);
    assertInterrupted();
  }

  // Lock.tryLock() tests
  public void testTryLockTimeoutExceeded() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Lock lock = new ReentrantLock();
    Thread lockThread = acquireFor(lock, 5, SECONDS);

    boolean lockAcquired = tryLockUninterruptibly(lock, 500, MILLISECONDS);

    assertFalse(lockAcquired);
    assertAtLeastTimePassed(stopwatch, 500);
    assertNotInterrupted();

    // finish locking thread
    lockThread.interrupt();
  }

  public void testTryLockTimeoutNotExceeded() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Lock lock = new ReentrantLock();
    acquireFor(lock, 500, MILLISECONDS);

    boolean signaledBeforeTimeout = tryLockUninterruptibly(lock, 1500, MILLISECONDS);

    assertTrue(signaledBeforeTimeout);
    assertTimeNotPassed(stopwatch, LONG_DELAY_MS);
    assertNotInterrupted();
  }

  public void testTryLockInterruptedTimeoutExceeded() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Lock lock = new ReentrantLock();
    Thread lockThread = acquireFor(lock, 5, SECONDS);
    requestInterruptIn(500);

    boolean signaledBeforeTimeout = tryLockUninterruptibly(lock, 1000, MILLISECONDS);

    assertFalse(signaledBeforeTimeout);
    assertAtLeastTimePassed(stopwatch, 1000);
    assertInterrupted();

    // finish locking thread
    lockThread.interrupt();
  }

  public void testTryLockInterruptedTimeoutNotExceeded() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Lock lock = new ReentrantLock();
    acquireFor(lock, 1000, MILLISECONDS);
    requestInterruptIn(500);

    boolean signaledBeforeTimeout = tryLockUninterruptibly(lock, 1500, MILLISECONDS);

    assertTrue(signaledBeforeTimeout);
    assertTimeNotPassed(stopwatch, LONG_DELAY_MS);
    assertInterrupted();
  }

  // BlockingQueue.put() tests
  public void testPutWithNoWait() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    BlockingQueue<String> queue = new ArrayBlockingQueue<>(999);
    putUninterruptibly(queue, "");
    assertTimeNotPassed(stopwatch, LONG_DELAY_MS);
    assertEquals("", queue.peek());
  }

  public void testPutNoInterrupt() {
    TimedPutQueue queue = TimedPutQueue.createWithDelay(20);
    queue.putSuccessfully();
    assertNotInterrupted();
  }

  public void testPutSingleInterrupt() {
    TimedPutQueue queue = TimedPutQueue.createWithDelay(50);
    requestInterruptIn(10);
    queue.putSuccessfully();
    assertInterrupted();
  }

  public void testPutMultiInterrupt() {
    TimedPutQueue queue = TimedPutQueue.createWithDelay(100);
    repeatedlyInterruptTestThread(20, tearDownStack);
    queue.putSuccessfully();
    assertInterrupted();
  }

  // BlockingQueue.take() tests
  public void testTakeWithNoWait() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    BlockingQueue<String> queue = new ArrayBlockingQueue<>(1);
    assertTrue(queue.offer(""));
    assertEquals("", takeUninterruptibly(queue));
    assertTimeNotPassed(stopwatch, LONG_DELAY_MS);
  }

  public void testTakeNoInterrupt() {
    TimedTakeQueue queue = TimedTakeQueue.createWithDelay(20);
    queue.takeSuccessfully();
    assertNotInterrupted();
  }

  public void testTakeSingleInterrupt() {
    TimedTakeQueue queue = TimedTakeQueue.createWithDelay(50);
    requestInterruptIn(10);
    queue.takeSuccessfully();
    assertInterrupted();
  }

  public void testTakeMultiInterrupt() {
    TimedTakeQueue queue = TimedTakeQueue.createWithDelay(100);
    repeatedlyInterruptTestThread(20, tearDownStack);
    queue.takeSuccessfully();
    assertInterrupted();
  }

  // join() tests
  public void testJoinWithNoWait() throws InterruptedException {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Thread thread = new Thread(new JoinTarget(15));
    thread.start();
    thread.join();
    assertFalse(thread.isAlive());

    joinUninterruptibly(thread);
    joinUninterruptibly(thread, 0, MILLISECONDS);
    joinUninterruptibly(thread, -42, MILLISECONDS);
    joinUninterruptibly(thread, LONG_DELAY_MS, MILLISECONDS);
    assertTimeNotPassed(stopwatch, LONG_DELAY_MS);
  }

  public void testJoinNoInterrupt() {
    TimedThread thread = TimedThread.createWithDelay(20);
    thread.joinSuccessfully();
    assertNotInterrupted();
  }

  public void testJoinTimeoutNoInterruptNotExpired() {
    TimedThread thread = TimedThread.createWithDelay(20);
    thread.joinSuccessfully(LONG_DELAY_MS);
    assertNotInterrupted();
  }

  public void testJoinTimeoutNoInterruptExpired() {
    TimedThread thread = TimedThread.createWithDelay(LONG_DELAY_MS);
    thread.joinUnsuccessfully(30);
    assertNotInterrupted();
  }

  public void testJoinSingleInterrupt() {
    TimedThread thread = TimedThread.createWithDelay(50);
    requestInterruptIn(10);
    thread.joinSuccessfully();
    assertInterrupted();
  }

  public void testJoinTimeoutSingleInterruptNoExpire() {
    TimedThread thread = TimedThread.createWithDelay(50);
    requestInterruptIn(10);
    thread.joinSuccessfully(LONG_DELAY_MS);
    assertInterrupted();
  }

  public void testJoinTimeoutSingleInterruptExpired() {
    TimedThread thread = TimedThread.createWithDelay(LONG_DELAY_MS);
    requestInterruptIn(10);
    thread.joinUnsuccessfully(50);
    assertInterrupted();
  }

  public void testJoinMultiInterrupt() {
    TimedThread thread = TimedThread.createWithDelay(100);
    repeatedlyInterruptTestThread(20, tearDownStack);
    thread.joinSuccessfully();
    assertInterrupted();
  }

  public void testJoinTimeoutMultiInterruptNoExpire() {
    TimedThread thread = TimedThread.createWithDelay(100);
    repeatedlyInterruptTestThread(20, tearDownStack);
    thread.joinSuccessfully(LONG_DELAY_MS);
    assertInterrupted();
  }

  public void testJoinTimeoutMultiInterruptExpired() {
    /*
     * We don't "need" to schedule a thread completion at all here, but by doing
     * so, we come the closest we can to testing that the wait time is
     * appropriately decreased on each progressive join() call.
     */
    TimedThread thread = TimedThread.createWithDelay(LONG_DELAY_MS);
    repeatedlyInterruptTestThread(20, tearDownStack);
    thread.joinUnsuccessfully(70);
    assertInterrupted();
  }

  // sleep() Tests
  public void testSleepNoInterrupt() {
    sleepSuccessfully(10);
  }

  public void testSleepSingleInterrupt() {
    requestInterruptIn(10);
    sleepSuccessfully(50);
    assertInterrupted();
  }

  public void testSleepMultiInterrupt() {
    repeatedlyInterruptTestThread(10, tearDownStack);
    sleepSuccessfully(100);
    assertInterrupted();
  }

  // Semaphore.tryAcquire() tests
  public void testTryAcquireWithNoWait() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Semaphore semaphore = new Semaphore(99);
    assertTrue(tryAcquireUninterruptibly(semaphore, 0, MILLISECONDS));
    assertTrue(tryAcquireUninterruptibly(semaphore, -42, MILLISECONDS));
    assertTrue(tryAcquireUninterruptibly(semaphore, LONG_DELAY_MS, MILLISECONDS));
    assertTimeNotPassed(stopwatch, LONG_DELAY_MS);
  }

  public void testTryAcquireTimeoutNoInterruptNotExpired() {
    TimedSemaphore semaphore = TimedSemaphore.createWithDelay(20);
    semaphore.tryAcquireSuccessfully(LONG_DELAY_MS);
    assertNotInterrupted();
  }

  public void testTryAcquireTimeoutNoInterruptExpired() {
    TimedSemaphore semaphore = TimedSemaphore.createWithDelay(LONG_DELAY_MS);
    semaphore.tryAcquireUnsuccessfully(30);
    assertNotInterrupted();
  }

  public void testTryAcquireTimeoutSingleInterruptNoExpire() {
    TimedSemaphore semaphore = TimedSemaphore.createWithDelay(50);
    requestInterruptIn(10);
    semaphore.tryAcquireSuccessfully(LONG_DELAY_MS);
    assertInterrupted();
  }

  public void testTryAcquireTimeoutSingleInterruptExpired() {
    TimedSemaphore semaphore = TimedSemaphore.createWithDelay(LONG_DELAY_MS);
    requestInterruptIn(10);
    semaphore.tryAcquireUnsuccessfully(50);
    assertInterrupted();
  }

  public void testTryAcquireTimeoutMultiInterruptNoExpire() {
    TimedSemaphore semaphore = TimedSemaphore.createWithDelay(100);
    repeatedlyInterruptTestThread(20, tearDownStack);
    semaphore.tryAcquireSuccessfully(LONG_DELAY_MS);
    assertInterrupted();
  }

  public void testTryAcquireTimeoutMultiInterruptExpired() {
    /*
     * We don't "need" to schedule a release() call at all here, but by doing
     * so, we come the closest we can to testing that the wait time is
     * appropriately decreased on each progressive tryAcquire() call.
     */
    TimedSemaphore semaphore = TimedSemaphore.createWithDelay(LONG_DELAY_MS);
    repeatedlyInterruptTestThread(20, tearDownStack);
    semaphore.tryAcquireUnsuccessfully(70);
    assertInterrupted();
  }

  public void testTryAcquireWithNoWaitMultiPermit() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    Semaphore semaphore = new Semaphore(99);
    assertTrue(tryAcquireUninterruptibly(semaphore, 10, 0, MILLISECONDS));
    assertTrue(tryAcquireUninterruptibly(semaphore, 10, -42, MILLISECONDS));
    assertTrue(tryAcquireUninterruptibly(semaphore, 10, LONG_DELAY_MS, MILLISECONDS));
    assertTimeNotPassed(stopwatch, LONG_DELAY_MS);
  }

  public void testTryAcquireTimeoutNoInterruptNotExpiredMultiPermit() {
    TimedSemaphore semaphore = TimedSemaphore.createWithDelay(20);
    semaphore.tryAcquireSuccessfully(10, LONG_DELAY_MS);
    assertNotInterrupted();
  }

  public void testTryAcquireTimeoutNoInterruptExpiredMultiPermit() {
    TimedSemaphore semaphore = TimedSemaphore.createWithDelay(LONG_DELAY_MS);
    semaphore.tryAcquireUnsuccessfully(10, 30);
    assertNotInterrupted();
  }

  public void testTryAcquireTimeoutSingleInterruptNoExpireMultiPermit() {
    TimedSemaphore semaphore = TimedSemaphore.createWithDelay(50);
    requestInterruptIn(10);
    semaphore.tryAcquireSuccessfully(10, LONG_DELAY_MS);
    assertInterrupted();
  }

  public void testTryAcquireTimeoutSingleInterruptExpiredMultiPermit() {
    TimedSemaphore semaphore = TimedSemaphore.createWithDelay(LONG_DELAY_MS);
    requestInterruptIn(10);
    semaphore.tryAcquireUnsuccessfully(10, 50);
    assertInterrupted();
  }

  public void testTryAcquireTimeoutMultiInterruptNoExpireMultiPermit() {
    TimedSemaphore semaphore = TimedSemaphore.createWithDelay(100);
    repeatedlyInterruptTestThread(20, tearDownStack);
    semaphore.tryAcquireSuccessfully(10, LONG_DELAY_MS);
    assertInterrupted();
  }

  public void testTryAcquireTimeoutMultiInterruptExpiredMultiPermit() {
    /*
     * We don't "need" to schedule a release() call at all here, but by doing
     * so, we come the closest we can to testing that the wait time is
     * appropriately decreased on each progressive tryAcquire() call.
     */
    TimedSemaphore semaphore = TimedSemaphore.createWithDelay(LONG_DELAY_MS);
    repeatedlyInterruptTestThread(20, tearDownStack);
    semaphore.tryAcquireUnsuccessfully(10, 70);
    assertInterrupted();
  }

  // executor.awaitTermination Testcases
  public void testTryAwaitTerminationUninterruptiblyDuration_success() {
    ExecutorService executor = newFixedThreadPool(1);
    requestInterruptIn(500);
    executor.execute(new SleepTask(1000));
    executor.shutdown();
    assertTrue(awaitTerminationUninterruptibly(executor, Duration.ofMillis(LONG_DELAY_MS)));
    assertTrue(executor.isTerminated());
    assertInterrupted();
  }

  public void testTryAwaitTerminationUninterruptiblyDuration_failure() {
    ExecutorService executor = newFixedThreadPool(1);
    requestInterruptIn(500);
    executor.execute(new SleepTask(10000));
    executor.shutdown();
    assertFalse(awaitTerminationUninterruptibly(executor, Duration.ofMillis(1000)));
    assertFalse(executor.isTerminated());
    assertInterrupted();
  }

  public void testTryAwaitTerminationUninterruptiblyLongTimeUnit_success() {
    ExecutorService executor = newFixedThreadPool(1);
    requestInterruptIn(500);
    executor.execute(new SleepTask(1000));
    executor.shutdown();
    assertTrue(awaitTerminationUninterruptibly(executor, LONG_DELAY_MS, MILLISECONDS));
    assertTrue(executor.isTerminated());
    assertInterrupted();
  }

  public void testTryAwaitTerminationUninterruptiblyLongTimeUnit_failure() {
    ExecutorService executor = newFixedThreadPool(1);
    requestInterruptIn(500);
    executor.execute(new SleepTask(10000));
    executor.shutdown();
    assertFalse(awaitTerminationUninterruptibly(executor, 1000, MILLISECONDS));
    assertFalse(executor.isTerminated());
    assertInterrupted();
  }

  public void testTryAwaitTerminationInfiniteTimeout() {
    ExecutorService executor = newFixedThreadPool(1);
    requestInterruptIn(500);
    executor.execute(new SleepTask(1000));
    executor.shutdown();
    awaitTerminationUninterruptibly(executor);
    assertTrue(executor.isTerminated());
    assertInterrupted();
  }

  /**
   * Wrapper around {@link Stopwatch} which also contains an "expected completion time." Creating a
   * {@code Completion} starts the underlying stopwatch.
   */
  private static final class Completion {
    final Stopwatch stopwatch;
    final long expectedCompletionWaitMillis;

    Completion(long expectedCompletionWaitMillis) {
      this.expectedCompletionWaitMillis = expectedCompletionWaitMillis;
      stopwatch = Stopwatch.createStarted();
    }

    /**
     * Asserts that the expected completion time has passed (and not "too much" time beyond that).
     */
    void assertCompletionExpected() {
      assertAtLeastTimePassed(stopwatch, expectedCompletionWaitMillis);
      assertTimeNotPassed(stopwatch, expectedCompletionWaitMillis + LONG_DELAY_MS);
    }

    /**
     * Asserts that at least {@code timeout} has passed but the expected completion time has not.
     */
    void assertCompletionNotExpected(long timeout) {
      Preconditions.checkArgument(timeout < expectedCompletionWaitMillis);
      assertAtLeastTimePassed(stopwatch, timeout);
      assertTimeNotPassed(stopwatch, expectedCompletionWaitMillis);
    }
  }

  private static void assertAtLeastTimePassed(Stopwatch stopwatch, long expectedMillis) {
    long elapsedMillis = stopwatch.elapsed(MILLISECONDS);
    /*
     * The "+ 5" below is to permit, say, sleep(10) to sleep only 9 milliseconds. We see such
     * behavior sometimes when running these tests publicly as part of Guava. "+ 5" is probably more
     * generous than it needs to be.
     */
    assertTrue(
        "Expected elapsed millis to be >= " + expectedMillis + " but was " + elapsedMillis,
        elapsedMillis + 5 >= expectedMillis);
  }

  // TODO(cpovirk): Split this into separate CountDownLatch and IncrementableCountDownLatch classes.

  /** Manages a {@link BlockingQueue} and associated timings for a {@code put} call. */
  private static final class TimedPutQueue {
    final BlockingQueue<String> queue;
    final Completion completed;

    /**
     * Creates a {@link EnableWrites} which open up a spot for a {@code put} to succeed in {@code
     * countdownInMillis}.
     */
    static TimedPutQueue createWithDelay(long countdownInMillis) {
      return new TimedPutQueue(countdownInMillis);
    }

    private TimedPutQueue(long countdownInMillis) {
      this.queue = new ArrayBlockingQueue<>(1);
      assertTrue(queue.offer("blocksPutCallsUntilRemoved"));
      this.completed = new Completion(countdownInMillis);
      scheduleEnableWrites(this.queue, countdownInMillis);
    }

    /** Perform a {@code put} and assert that operation completed in the expected timeframe. */
    void putSuccessfully() {
      putUninterruptibly(queue, "");
      completed.assertCompletionExpected();
      assertEquals("", queue.peek());
    }

    private static void scheduleEnableWrites(BlockingQueue<String> queue, long countdownInMillis) {
      Runnable toRun = new EnableWrites(queue, countdownInMillis);
      // TODO(cpovirk): automatically fail the test if this thread throws
      Thread enablerThread = new Thread(toRun);
      enablerThread.start();
    }
  }

  /** Manages a {@link BlockingQueue} and associated timings for a {@code take} call. */
  private static final class TimedTakeQueue {
    final BlockingQueue<String> queue;
    final Completion completed;

    /**
     * Creates a {@link EnableReads} which insert an element for a {@code take} to receive in {@code
     * countdownInMillis}.
     */
    static TimedTakeQueue createWithDelay(long countdownInMillis) {
      return new TimedTakeQueue(countdownInMillis);
    }

    private TimedTakeQueue(long countdownInMillis) {
      this.queue = new ArrayBlockingQueue<>(1);
      this.completed = new Completion(countdownInMillis);
      scheduleEnableReads(this.queue, countdownInMillis);
    }

    /** Perform a {@code take} and assert that operation completed in the expected timeframe. */
    void takeSuccessfully() {
      assertEquals(EXPECTED_TAKE, takeUninterruptibly(queue));
      completed.assertCompletionExpected();
      assertTrue(queue.isEmpty());
    }

    private static void scheduleEnableReads(BlockingQueue<String> queue, long countdownInMillis) {
      Runnable toRun = new EnableReads(queue, countdownInMillis);
      // TODO(cpovirk): automatically fail the test if this thread throws
      Thread enablerThread = new Thread(toRun);
      enablerThread.start();
    }
  }

  /** Manages a {@link Semaphore} and associated timings. */
  private static final class TimedSemaphore {
    final Semaphore semaphore;
    final Completion completed;

    /**
     * Create a {@link Release} which will release a semaphore permit in {@code countdownInMillis}.
     */
    static TimedSemaphore createWithDelay(long countdownInMillis) {
      return new TimedSemaphore(countdownInMillis);
    }

    private TimedSemaphore(long countdownInMillis) {
      this.semaphore = new Semaphore(0);
      this.completed = new Completion(countdownInMillis);
      scheduleRelease(countdownInMillis);
    }

    /**
     * Requests a permit from the semaphore with a timeout and asserts that operation completed in
     * the expected timeframe.
     */
    void tryAcquireSuccessfully(long timeoutMillis) {
      assertTrue(tryAcquireUninterruptibly(semaphore, timeoutMillis, MILLISECONDS));
      completed.assertCompletionExpected();
    }

    void tryAcquireSuccessfully(int permits, long timeoutMillis) {
      assertTrue(tryAcquireUninterruptibly(semaphore, permits, timeoutMillis, MILLISECONDS));
      completed.assertCompletionExpected();
    }

    /**
     * Requests a permit from the semaphore with a timeout and asserts that the wait returned within
     * the expected timeout.
     */
    private void tryAcquireUnsuccessfully(long timeoutMillis) {
      assertFalse(tryAcquireUninterruptibly(semaphore, timeoutMillis, MILLISECONDS));
      completed.assertCompletionNotExpected(timeoutMillis);
    }

    private void tryAcquireUnsuccessfully(int permits, long timeoutMillis) {
      assertFalse(tryAcquireUninterruptibly(semaphore, permits, timeoutMillis, MILLISECONDS));
      completed.assertCompletionNotExpected(timeoutMillis);
    }

    private void scheduleRelease(long countdownInMillis) {
      DelayedActionRunnable toRun = new Release(semaphore, countdownInMillis);
      // TODO(cpovirk): automatically fail the test if this thread throws
      Thread releaserThread = new Thread(toRun);
      releaserThread.start();
    }
  }

  private abstract static class DelayedActionRunnable implements Runnable {
    private final long tMinus;

    protected DelayedActionRunnable(long tMinus) {
      this.tMinus = tMinus;
    }

    @Override
    public final void run() {
      try {
        Thread.sleep(tMinus);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      doAction();
    }

    protected abstract void doAction();
  }

  private static class CountDown extends DelayedActionRunnable {
    private final CountDownLatch latch;

    public CountDown(CountDownLatch latch, long tMinus) {
      super(tMinus);
      this.latch = latch;
    }

    @Override
    protected void doAction() {
      latch.countDown();
    }
  }

  private static class EnableWrites extends DelayedActionRunnable {
    private final BlockingQueue<String> queue;

    public EnableWrites(BlockingQueue<String> queue, long tMinus) {
      super(tMinus);
      assertFalse(queue.isEmpty());
      assertFalse(queue.offer("shouldBeRejected"));
      this.queue = queue;
    }

    @Override
    protected void doAction() {
      assertNotNull(queue.remove());
    }
  }

  private static class EnableReads extends DelayedActionRunnable {
    private final BlockingQueue<String> queue;

    public EnableReads(BlockingQueue<String> queue, long tMinus) {
      super(tMinus);
      assertTrue(queue.isEmpty());
      this.queue = queue;
    }

    @Override
    protected void doAction() {
      assertTrue(queue.offer(EXPECTED_TAKE));
    }
  }

  private static final class TimedThread {
    private final Thread thread;
    private final Completion completed;

    static TimedThread createWithDelay(long countdownInMillis) {
      return new TimedThread(countdownInMillis);
    }

    private TimedThread(long expectedCompletionWaitMillis) {
      completed = new Completion(expectedCompletionWaitMillis);
      thread = new Thread(new JoinTarget(expectedCompletionWaitMillis));
      thread.start();
    }

    void joinSuccessfully() {
      Uninterruptibles.joinUninterruptibly(thread);
      completed.assertCompletionExpected();
      assertEquals(Thread.State.TERMINATED, thread.getState());
    }

    void joinSuccessfully(long timeoutMillis) {
      Uninterruptibles.joinUninterruptibly(thread, timeoutMillis, MILLISECONDS);
      completed.assertCompletionExpected();
      assertEquals(Thread.State.TERMINATED, thread.getState());
    }

    void joinUnsuccessfully(long timeoutMillis) {
      Uninterruptibles.joinUninterruptibly(thread, timeoutMillis, MILLISECONDS);
      completed.assertCompletionNotExpected(timeoutMillis);
      assertFalse(Thread.State.TERMINATED.equals(thread.getState()));
    }
  }

  private static class JoinTarget extends DelayedActionRunnable {
    public JoinTarget(long tMinus) {
      super(tMinus);
    }

    @Override
    protected void doAction() {}
  }

  private static class Release extends DelayedActionRunnable {
    private final Semaphore semaphore;

    public Release(Semaphore semaphore, long tMinus) {
      super(tMinus);
      this.semaphore = semaphore;
    }

    @Override
    protected void doAction() {
      semaphore.release(10);
    }
  }

  private static final class SleepTask extends DelayedActionRunnable {
    SleepTask(long tMinus) {
      super(tMinus);
    }

    @Override
    protected void doAction() {}
  }

  private static void sleepSuccessfully(long sleepMillis) {
    Completion completed = new Completion(sleepMillis - SLEEP_SLACK);
    Uninterruptibles.sleepUninterruptibly(sleepMillis, MILLISECONDS);
    completed.assertCompletionExpected();
  }

  private static void assertTimeNotPassed(Stopwatch stopwatch, long timelimitMillis) {
    long elapsedMillis = stopwatch.elapsed(MILLISECONDS);
    assertTrue(elapsedMillis < timelimitMillis);
  }

  /**
   * Await an interrupt, then clear the interrupt status. Similar to {@code
   * assertTrue(Thread.interrupted())} except that this version tolerates late interrupts.
   */
  private static void assertInterrupted() {
    try {
      /*
       * The sleep() will end immediately if we've already been interrupted or
       * wait patiently for the interrupt if not.
       */
      Thread.sleep(LONG_DELAY_MS);
      fail("Dude, where's my interrupt?");
    } catch (InterruptedException expected) {
    }
  }

  private static void assertNotInterrupted() {
    assertFalse(Thread.interrupted());
  }

  private static void requestInterruptIn(long millis) {
    InterruptionUtil.requestInterruptIn(millis, MILLISECONDS);
  }

  @CanIgnoreReturnValue
  private static Thread acquireFor(final Lock lock, final long duration, final TimeUnit unit) {
    final CountDownLatch latch = new CountDownLatch(1);
    Thread thread =
        new Thread() {
          @Override
          public void run() {
            lock.lock();
            latch.countDown();
            try {
              Thread.sleep(unit.toMillis(duration));
            } catch (InterruptedException e) {
              // simply finish execution
            } finally {
              lock.unlock();
            }
          }
        };
    thread.setDaemon(true);
    thread.start();
    awaitUninterruptibly(latch);
    return thread;
  }

  private static class TestCondition implements Condition {
    private final Lock lock;
    private final Condition condition;

    private TestCondition(Lock lock, Condition condition) {
      this.lock = lock;
      this.condition = condition;
    }

    static TestCondition createAndSignalAfter(long delay, TimeUnit unit) {
      final TestCondition testCondition = create();

      ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
      // If signal() fails somehow, we should see a failed test, even without looking at the Future.
      Future<?> unused =
          scheduledPool.schedule(
              new Runnable() {
                @Override
                public void run() {
                  testCondition.signal();
                }
              },
              delay,
              unit);

      return testCondition;
    }

    static TestCondition create() {
      Lock lock = new ReentrantLock();
      Condition condition = lock.newCondition();
      return new TestCondition(lock, condition);
    }

    @Override
    public void await() throws InterruptedException {
      lock.lock();
      try {
        condition.await();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
      lock.lock();
      try {
        return condition.await(time, unit);
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void awaitUninterruptibly() {
      lock.lock();
      try {
        condition.awaitUninterruptibly();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public long awaitNanos(long nanosTimeout) throws InterruptedException {
      lock.lock();
      try {
        return condition.awaitNanos(nanosTimeout);
      } finally {
        lock.unlock();
      }
    }

    @Override
    public boolean awaitUntil(Date deadline) throws InterruptedException {
      lock.lock();
      try {
        return condition.awaitUntil(deadline);
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void signal() {
      lock.lock();
      try {
        condition.signal();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void signalAll() {
      lock.lock();
      try {
        condition.signalAll();
      } finally {
        lock.unlock();
      }
    }
  }
}
