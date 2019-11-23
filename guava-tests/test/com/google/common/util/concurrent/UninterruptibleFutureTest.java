/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.util.concurrent.InterruptionUtil.repeatedlyInterruptTestThread;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.testing.TearDown;
import com.google.common.testing.TearDownStack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;

// TODO(azana/cpovirk): Should this be merged into UninterruptiblesTest?
/**
 * Unit test for {@link Uninterruptibles#getUninterruptibly}
 *
 * @author Kevin Bourrillion
 * @author Chris Povirk
 */
public class UninterruptibleFutureTest extends TestCase {
  private SleepingRunnable sleeper;
  private Future<Boolean> delayedFuture;

  private final TearDownStack tearDownStack = new TearDownStack();

  @Override
  protected void setUp() {
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    tearDownStack.addTearDown(
        new TearDown() {
          @Override
          public void tearDown() {
            executor.shutdownNow();
          }
        });
    sleeper = new SleepingRunnable(1000);
    delayedFuture = executor.submit(sleeper, true);

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

  /**
   * This first test doesn't test anything in Uninterruptibles, just demonstrates some normal
   * behavior of futures so that you can contrast the next test with it.
   */

  public void testRegularFutureInterrupted() throws ExecutionException {

    /*
     * Here's the order of events that we want.
     *
     * 1. The client thread begins to block on a get() call to a future.
     * 2. The client thread is interrupted sometime before the result would be
     *   available.
     * 3. We expect the client's get() to throw an InterruptedException.
     * 4. We expect the client thread's interrupt state to be false.
     * 5. The client thread again makes a blocking call to get().
     * 6. Now the result becomes available.
     * 7. We expect get() to return this result.
     * 8. We expect the test thread's interrupt state to be false.
     */
    InterruptionUtil.requestInterruptIn(200, TimeUnit.MILLISECONDS);

    assertFalse(Thread.interrupted());
    try {
      delayedFuture.get(20000, TimeUnit.MILLISECONDS);
      fail("expected to be interrupted");
    } catch (InterruptedException expected) {
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }

    // we were interrupted, but it's been cleared now
    assertFalse(Thread.interrupted());

    assertFalse(sleeper.completed);
    try {
      assertTrue(delayedFuture.get());
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    assertTrue(sleeper.completed);
  }

  public void testMakeUninterruptible_timeoutPreservedThroughInterruption()
      throws ExecutionException {

    repeatedlyInterruptTestThread(100, tearDownStack);

    try {
      getUninterruptibly(delayedFuture, 500, TimeUnit.MILLISECONDS);
      fail("expected to time out");
    } catch (TimeoutException expected) {
    }
    assertTrue(Thread.interrupted()); // clears the interrupt state, too

    assertFalse(sleeper.completed);
    assertTrue(getUninterruptibly(delayedFuture));

    assertTrue(Thread.interrupted()); // clears the interrupt state, too
    assertTrue(sleeper.completed);
  }

  private static class SleepingRunnable implements Runnable {
    final int millis;
    volatile boolean completed;

    public SleepingRunnable(int millis) {
      this.millis = millis;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException wontHappen) {
        throw new AssertionError();
      }
      completed = true;
    }
  }

  public void testMakeUninterruptible_untimed_uninterrupted() throws Exception {
    runUntimedInterruptsTest(0);
  }

  public void testMakeUninterruptible_untimed_interrupted() throws Exception {
    runUntimedInterruptsTest(1);
  }

  public void testMakeUninterruptible_untimed_multiplyInterrupted() throws Exception {
    runUntimedInterruptsTest(38);
  }

  public void testMakeUninterruptible_timed_uninterrupted() throws Exception {
    runTimedInterruptsTest(0);
  }

  public void testMakeUninterruptible_timed_interrupted() throws Exception {
    runTimedInterruptsTest(1);
  }

  public void testMakeUninterruptible_timed_multiplyInterrupted() throws Exception {
    runTimedInterruptsTest(38);
  }

  private static void runUntimedInterruptsTest(int times)
      throws InterruptedException, ExecutionException, TimeoutException {
    SettableFuture<String> future = SettableFuture.create();
    FutureTask<Boolean> interruptReporter = untimedInterruptReporter(future, false);

    runNInterruptsTest(times, future, interruptReporter);
  }

  private static void runTimedInterruptsTest(int times)
      throws InterruptedException, ExecutionException, TimeoutException {
    SettableFuture<String> future = SettableFuture.create();
    FutureTask<Boolean> interruptReporter = timedInterruptReporter(future);

    runNInterruptsTest(times, future, interruptReporter);
  }

  private static void runNInterruptsTest(
      int times, SettableFuture<String> future, FutureTask<Boolean> interruptReporter)
      throws InterruptedException, ExecutionException, TimeoutException {
    Thread waitingThread = new Thread(interruptReporter);
    waitingThread.start();
    for (int i = 0; i < times; i++) {
      waitingThread.interrupt();
    }

    future.set(RESULT);

    assertEquals(times > 0, (boolean) interruptReporter.get(20, SECONDS));
  }

  /**
   * Confirms that the test code triggers {@link InterruptedException} in a standard {@link Future}.
   */

  public void testMakeUninterruptible_plainFutureSanityCheck() throws Exception {
    SettableFuture<String> future = SettableFuture.create();
    FutureTask<Boolean> wasInterrupted = untimedInterruptReporter(future, true);

    Thread waitingThread = new Thread(wasInterrupted);
    waitingThread.start();
    waitingThread.interrupt();
    try {
      wasInterrupted.get();
      fail();
    } catch (ExecutionException expected) {
      assertTrue(
          expected.getCause().toString(), expected.getCause() instanceof InterruptedException);
    }
  }

  public void testMakeUninterruptible_timedGetZeroTimeoutAttempted()
      throws TimeoutException, ExecutionException {
    SettableFuture<String> future = SettableFuture.create();
    future.set(RESULT);
    /*
     * getUninterruptibly should call the timed get method once with a
     * wait of 0 seconds (and it should succeed, since the result is already
     * available).
     */
    assertEquals(RESULT, getUninterruptibly(future, 0, SECONDS));
  }

  public void testMakeUninterruptible_timedGetNegativeTimeoutAttempted()
      throws TimeoutException, ExecutionException {
    SettableFuture<String> future = SettableFuture.create();
    future.set(RESULT);
    /*
     * The getUninterruptibly should call the timed get method once with a
     * wait of -1 seconds (and it should succeed, since the result is already
     * available).
     */
    assertEquals(RESULT, getUninterruptibly(future, -1, SECONDS));
  }

  private static FutureTask<Boolean> untimedInterruptReporter(
      final Future<?> future, final boolean allowInterruption) {
    return new FutureTask<>(
        new Callable<Boolean>() {
          @Override
          public Boolean call() throws Exception {
            Object actual;
            if (allowInterruption) {
              actual = future.get();
            } else {
              actual = getUninterruptibly(future);
            }
            assertEquals(RESULT, actual);
            return Thread.interrupted();
          }
        });
  }

  private static FutureTask<Boolean> timedInterruptReporter(final Future<?> future) {
    return new FutureTask<>(
        new Callable<Boolean>() {
          @Override
          public Boolean call() throws Exception {
            assertEquals(RESULT, getUninterruptibly(future, 10, MINUTES));
            return Thread.interrupted();
          }
        });
  }

  private static final String RESULT = "result";
}
