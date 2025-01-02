/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;
import static com.google.common.base.StandardSystemProperty.OS_NAME;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.util.concurrent.Futures.immediateCancelledFuture;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.SneakyThrows.sneakyThrow;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.internal.InternalFutureFailureAccess;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link AbstractFuture}.
 *
 * @author Brian Stoler
 */
@NullUnmarked
public class AbstractFutureTest extends TestCase {
  public void testSuccess() throws ExecutionException, InterruptedException {
    final Object value = new Object();
    assertSame(
        value,
        new AbstractFuture<Object>() {
          {
            set(value);
          }
        }.get());
  }

  public void testException() throws InterruptedException {
    final Throwable failure = new Throwable();
    AbstractFuture<String> future =
        new AbstractFuture<String>() {
          {
            setException(failure);
          }
        };

    ExecutionException ee1 = getExpectingExecutionException(future);
    ExecutionException ee2 = getExpectingExecutionException(future);

    // Ensure we get a unique execution exception on each get
    assertNotSame(ee1, ee2);

    assertThat(ee1).hasCauseThat().isSameInstanceAs(failure);
    assertThat(ee2).hasCauseThat().isSameInstanceAs(failure);

    checkStackTrace(ee1);
    checkStackTrace(ee2);
  }

  public void testCancel_notDoneNoInterrupt() throws Exception {
    InterruptibleFuture future = new InterruptibleFuture();
    assertTrue(future.cancel(false));
    assertTrue(future.isCancelled());
    assertTrue(future.isDone());
    assertFalse(future.wasInterrupted());
    assertFalse(future.interruptTaskWasCalled);
    CancellationException e = assertThrows(CancellationException.class, () -> future.get());
    assertThat(e).hasCauseThat().isNull();
  }

  public void testCancel_notDoneInterrupt() throws Exception {
    InterruptibleFuture future = new InterruptibleFuture();
    assertTrue(future.cancel(true));
    assertTrue(future.isCancelled());
    assertTrue(future.isDone());
    assertTrue(future.wasInterrupted());
    assertTrue(future.interruptTaskWasCalled);
    CancellationException e = assertThrows(CancellationException.class, () -> future.get());
    assertThat(e).hasCauseThat().isNull();
  }

  public void testCancel_done() throws Exception {
    AbstractFuture<String> future =
        new AbstractFuture<String>() {
          {
            set("foo");
          }
        };
    assertFalse(future.cancel(true));
    assertFalse(future.isCancelled());
    assertTrue(future.isDone());
  }

  public void testGetWithTimeoutDoneFuture() throws Exception {
    AbstractFuture<String> future =
        new AbstractFuture<String>() {
          {
            set("foo");
          }
        };
    assertEquals("foo", future.get(0, SECONDS));
  }

  public void testEvilFuture_setFuture() throws Exception {
    final RuntimeException exception = new RuntimeException("you didn't say the magic word!");
    AbstractFuture<String> evilFuture =
        new AbstractFuture<String>() {
          @Override
          public void addListener(Runnable r, Executor e) {
            throw exception;
          }
        };
    AbstractFuture<String> normalFuture = new AbstractFuture<String>() {};
    normalFuture.setFuture(evilFuture);
    assertTrue(normalFuture.isDone());
    ExecutionException e = assertThrows(ExecutionException.class, () -> normalFuture.get());
    assertThat(e).hasCauseThat().isSameInstanceAs(exception);
  }

  public void testRemoveWaiter_interruption() throws Exception {
    final AbstractFuture<String> future = new AbstractFuture<String>() {};
    WaiterThread waiter1 = new WaiterThread(future);
    waiter1.start();
    waiter1.awaitWaiting();

    WaiterThread waiter2 = new WaiterThread(future);
    waiter2.start();
    waiter2.awaitWaiting();
    // The waiter queue should be waiter2->waiter1

    // This should wake up waiter1 and cause the waiter1 node to be removed.
    waiter1.interrupt();

    waiter1.join();
    waiter2.awaitWaiting(); // should still be blocked

    LockSupport.unpark(waiter2); // spurious wakeup
    waiter2.awaitWaiting(); // should eventually re-park

    future.set(null);
    waiter2.join();
  }

  public void testRemoveWaiter_polling() throws Exception {
    final AbstractFuture<String> future = new AbstractFuture<String>() {};
    WaiterThread waiter = new WaiterThread(future);
    waiter.start();
    waiter.awaitWaiting();
    PollingThread poller = new PollingThread(future);
    poller.start();
    PollingThread poller2 = new PollingThread(future);
    poller2.start();
    PollingThread poller3 = new PollingThread(future);
    poller3.start();
    poller.awaitInLoop();
    poller2.awaitInLoop();
    poller3.awaitInLoop();

    // The waiter queue should be {poller x 3}->waiter1
    waiter.interrupt();

    // This should wake up waiter1 and cause the waiter1 node to be removed.
    waiter.join();
    future.set(null);
    poller.join();
  }

  public void testToString_allUnique() throws Exception {
    // Two futures should not have the same toString, to avoid people asserting on it
    assertThat(SettableFuture.create().toString()).isNotEqualTo(SettableFuture.create().toString());
  }

  public void testToString_oom() throws Exception {
    SettableFuture<Object> future = SettableFuture.create();
    future.set(
        new Object() {
          @Override
          public String toString() {
            throw new OutOfMemoryError();
          }

          @Override
          public int hashCode() {
            throw new OutOfMemoryError();
          }
        });

    String unused = future.toString();

    SettableFuture<Object> future2 = SettableFuture.create();

    // A more organic OOM from a toString implementation
    Object object =
        new Object() {
          @Override
          public String toString() {
            return new String(new char[50_000]);
          }
        };
    List<Object> list = Collections.singletonList(object);
    for (int i = 0; i < 10; i++) {
      Object[] array = new Object[500];
      Arrays.fill(array, list);
      list = Arrays.asList(array);
    }
    future2.set(list);

    unused = future.toString();
  }

  public void testToString_notDone() throws Exception {
    AbstractFuture<Object> testFuture =
        new AbstractFuture<Object>() {
          @Override
          public String pendingToString() {
            return "cause=[Because this test isn't done]";
          }
        };
    assertThat(testFuture.toString())
        .matches(
            "[^\\[]+\\[status=PENDING, info=\\[cause=\\[Because this test isn't done\\]\\]\\]");
    TimeoutException e = assertThrows(TimeoutException.class, () -> testFuture.get(1, NANOSECONDS));
    assertThat(e).hasMessageThat().contains("1 nanoseconds");
    assertThat(e).hasMessageThat().contains("Because this test isn't done");
  }

  public void testToString_completesDuringToString() throws Exception {
    AbstractFuture<Object> testFuture =
        new AbstractFuture<Object>() {
          @Override
          public String pendingToString() {
            // Complete ourselves during the toString calculation
            this.set(true);
            return "cause=[Because this test isn't done]";
          }
        };
    assertThat(testFuture.toString())
        .matches("[^\\[]+\\[status=SUCCESS, result=\\[java.lang.Boolean@\\w+\\]\\]");
  }

  /**
   * This test attempts to cause a future to wait for longer than it was requested to from a timed
   * get() call. As measurements of time are prone to flakiness, it tries to assert based on ranges
   * derived from observing how much time actually passed for various operations.
   */
  @SuppressWarnings("ThreadPriorityCheck")
  @AndroidIncompatible // Thread.suspend
  public void testToString_delayedTimeout() throws Exception {
    Integer javaVersion = Ints.tryParse(JAVA_SPECIFICATION_VERSION.value());
    // Parsing to an integer might fail because Java 8 returns "1.8" instead of "8."
    // We can continue if it's 1.8, and we can continue if it's an integer in [9, 20).
    if (javaVersion != null && javaVersion >= 20) {
      // TODO(b/261217224, b/361604053): Make this test work under newer JDKs.
      return;
    }
    TimedWaiterThread thread = new TimedWaiterThread(new AbstractFuture<Object>() {}, 2, SECONDS);
    thread.start();
    thread.awaitWaiting();
    Thread.class.getMethod("suspend").invoke(thread);
    // Sleep for enough time to add 1500 milliseconds of overwait to the get() call.
    long toWaitMillis = 3500 - NANOSECONDS.toMillis(System.nanoTime() - thread.startTime);
    Thread.sleep(toWaitMillis);
    thread.setPriority(Thread.MAX_PRIORITY);
    Thread.class.getMethod("resume").invoke(thread);
    thread.join();
    // It's possible to race and suspend the thread just before the park call actually takes effect,
    // causing the thread to be suspended for 3.5 seconds, and then park itself for 2 seconds after
    // being resumed. To avoid a flake in this scenario, calculate how long that thread actually
    // waited and assert based on that time. Empirically, the race where the thread ends up waiting
    // for 5.5 seconds happens about 2% of the time.
    boolean longWait = NANOSECONDS.toSeconds(thread.timeSpentBlocked) >= 5;
    // Count how long it actually took to return; we'll accept any number between the expected delay
    // and the approximate actual delay, to be robust to variance in thread scheduling.
    char overWaitNanosFirstDigit =
        Long.toString(thread.timeSpentBlocked - MILLISECONDS.toNanos(longWait ? 5000 : 3000))
            .charAt(0);
    if (overWaitNanosFirstDigit < '4') {
      overWaitNanosFirstDigit = '9';
    }
    String nanosRegex = "[4-" + overWaitNanosFirstDigit + "][0-9]+";
    assertWithMessage(
            "Spent " + thread.timeSpentBlocked + " ns blocked; slept for " + toWaitMillis + " ms")
        .that(thread.exception)
        .hasMessageThat()
        .matches(
            "Waited 2 seconds \\(plus "
                + (longWait ? "3" : "1")
                + " seconds, "
                + nanosRegex
                + " nanoseconds delay\\).*");
  }

  public void testToString_completed() throws Exception {
    AbstractFuture<Object> testFuture2 =
        new AbstractFuture<Object>() {
          @Override
          public String pendingToString() {
            return "cause=[Someday...]";
          }
        };
    AbstractFuture<Object> testFuture3 = new AbstractFuture<Object>() {};
    testFuture3.setFuture(testFuture2);
    assertThat(testFuture3.toString())
        .matches(
            "[^\\[]+\\[status=PENDING, setFuture=\\[[^\\[]+\\[status=PENDING,"
                + " info=\\[cause=\\[Someday...]]]]]");
    testFuture2.set("result string");
    assertThat(testFuture3.toString())
        .matches("[^\\[]+\\[status=SUCCESS, result=\\[java.lang.String@\\w+\\]\\]");
  }

  public void testToString_cancelled() throws Exception {
    assertThat(immediateCancelledFuture().toString()).matches("[^\\[]+\\[status=CANCELLED\\]");
  }

  public void testToString_failed() {
    assertThat(immediateFailedFuture(new RuntimeException("foo")).toString())
        .matches("[^\\[]+\\[status=FAILURE, cause=\\[java.lang.RuntimeException: foo\\]\\]");
  }

  public void testToString_misbehaving() throws Exception {
    assertThat(
            new AbstractFuture<Object>() {
              @Override
              public String pendingToString() {
                throw new RuntimeException("I'm a misbehaving implementation");
              }
            }.toString())
        .matches(
            "[^\\[]+\\[status=PENDING, info=\\[Exception thrown from implementation: "
                + "class java.lang.RuntimeException\\]\\]");
  }

  public void testCompletionFinishesWithDone() {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    for (int i = 0; i < 50000; i++) {
      final AbstractFuture<String> future = new AbstractFuture<String>() {};
      final AtomicReference<String> errorMessage = Atomics.newReference();
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              future.set("success");
              if (!future.isDone()) {
                errorMessage.set("Set call exited before future was complete.");
              }
            }
          });
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              future.setException(new IllegalArgumentException("failure"));
              if (!future.isDone()) {
                errorMessage.set("SetException call exited before future was complete.");
              }
            }
          });
      executor.execute(
          new Runnable() {
            @Override
            public void run() {
              future.cancel(true);
              if (!future.isDone()) {
                errorMessage.set("Cancel call exited before future was complete.");
              }
            }
          });
      try {
        future.get();
      } catch (Throwable t) {
        // Ignore, we just wanted to block.
      }
      String error = errorMessage.get();
      assertNull(error, error);
    }
    executor.shutdown();
  }

  /**
   * He did the bash, he did the future bash The future bash, it was a concurrency smash He did the
   * bash, it caught on in a flash He did the bash, he did the future bash
   */

  public void testFutureBash() {
    if (isWindows()) {
      return; // TODO: b/136041958 - Running very slowly on Windows CI.
    }
    final CyclicBarrier barrier =
        new CyclicBarrier(
            6 // for the setter threads
                + 50 // for the listeners
                + 50 // for the blocking get threads,
                + 1); // for the main thread
    final ExecutorService executor = Executors.newFixedThreadPool(barrier.getParties());
    final AtomicReference<AbstractFuture<String>> currentFuture = Atomics.newReference();
    final AtomicInteger numSuccessfulSetCalls = new AtomicInteger();
    Callable<@Nullable Void> completeSuccessfullyRunnable =
        new Callable<@Nullable Void>() {
          @Override
          public @Nullable Void call() {
            if (currentFuture.get().set("set")) {
              numSuccessfulSetCalls.incrementAndGet();
            }
            awaitUnchecked(barrier);
            return null;
          }
        };
    Callable<@Nullable Void> completeExceptionallyRunnable =
        new Callable<@Nullable Void>() {
          Exception failureCause = new Exception("setException");

          @Override
          public @Nullable Void call() {
            if (currentFuture.get().setException(failureCause)) {
              numSuccessfulSetCalls.incrementAndGet();
            }
            awaitUnchecked(barrier);
            return null;
          }
        };
    Callable<@Nullable Void> cancelRunnable =
        new Callable<@Nullable Void>() {
          @Override
          public @Nullable Void call() {
            if (currentFuture.get().cancel(true)) {
              numSuccessfulSetCalls.incrementAndGet();
            }
            awaitUnchecked(barrier);
            return null;
          }
        };
    Callable<@Nullable Void> setFutureCompleteSuccessfullyRunnable =
        new Callable<@Nullable Void>() {
          ListenableFuture<String> future = immediateFuture("setFuture");

          @Override
          public @Nullable Void call() {
            if (currentFuture.get().setFuture(future)) {
              numSuccessfulSetCalls.incrementAndGet();
            }
            awaitUnchecked(barrier);
            return null;
          }
        };
    Callable<@Nullable Void> setFutureCompleteExceptionallyRunnable =
        new Callable<@Nullable Void>() {
          ListenableFuture<String> future = immediateFailedFuture(new Exception("setFuture"));

          @Override
          public @Nullable Void call() {
            if (currentFuture.get().setFuture(future)) {
              numSuccessfulSetCalls.incrementAndGet();
            }
            awaitUnchecked(barrier);
            return null;
          }
        };
    Callable<@Nullable Void> setFutureCancelRunnable =
        new Callable<@Nullable Void>() {
          ListenableFuture<String> future = immediateCancelledFuture();

          @Override
          public @Nullable Void call() {
            if (currentFuture.get().setFuture(future)) {
              numSuccessfulSetCalls.incrementAndGet();
            }
            awaitUnchecked(barrier);
            return null;
          }
        };
    final Set<Object> finalResults = Collections.synchronizedSet(Sets.newIdentityHashSet());
    Runnable collectResultsRunnable =
        new Runnable() {
          @Override
          public void run() {
            try {
              String result = Uninterruptibles.getUninterruptibly(currentFuture.get());
              finalResults.add(result);
            } catch (ExecutionException e) {
              finalResults.add(e.getCause());
            } catch (CancellationException e) {
              finalResults.add(CancellationException.class);
            } finally {
              awaitUnchecked(barrier);
            }
          }
        };
    Runnable collectResultsTimedGetRunnable =
        new Runnable() {
          @Override
          public void run() {
            Future<String> future = currentFuture.get();
            while (true) {
              try {
                String result = Uninterruptibles.getUninterruptibly(future, 0, SECONDS);
                finalResults.add(result);
                break;
              } catch (ExecutionException e) {
                finalResults.add(e.getCause());
                break;
              } catch (CancellationException e) {
                finalResults.add(CancellationException.class);
                break;
              } catch (TimeoutException e) {
                // loop
              }
            }
            awaitUnchecked(barrier);
          }
        };
    List<Callable<?>> allTasks = new ArrayList<>();
    allTasks.add(completeSuccessfullyRunnable);
    allTasks.add(completeExceptionallyRunnable);
    allTasks.add(cancelRunnable);
    allTasks.add(setFutureCompleteSuccessfullyRunnable);
    allTasks.add(setFutureCompleteExceptionallyRunnable);
    allTasks.add(setFutureCancelRunnable);
    for (int k = 0; k < 50; k++) {
      // For each listener we add a task that submits it to the executor directly for the blocking
      // get use case and another task that adds it as a listener to the future to exercise both
      // racing addListener calls and addListener calls completing after the future completes.
      final Runnable listener =
          k % 2 == 0 ? collectResultsRunnable : collectResultsTimedGetRunnable;
      allTasks.add(Executors.callable(listener));
      allTasks.add(
          new Callable<@Nullable Void>() {
            @Override
            public @Nullable Void call() throws Exception {
              currentFuture.get().addListener(listener, executor);
              return null;
            }
          });
    }
    assertEquals(allTasks.size() + 1, barrier.getParties());
    for (int i = 0; i < 1000; i++) {
      Collections.shuffle(allTasks);
      final AbstractFuture<String> future = new AbstractFuture<String>() {};
      currentFuture.set(future);
      for (Callable<?> task : allTasks) {
        @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
        Future<?> possiblyIgnoredError = executor.submit(task);
      }
      awaitUnchecked(barrier);
      assertThat(future.isDone()).isTrue();
      // inspect state and ensure it is correct!
      // asserts that all get calling threads received the same value
      Object result = Iterables.getOnlyElement(finalResults);
      if (result == CancellationException.class) {
        assertTrue(future.isCancelled());
        if (future.wasInterrupted()) {
          // We were cancelled, it is possible that setFuture could have succeeded too.
          assertThat(numSuccessfulSetCalls.get()).isIn(Range.closed(1, 2));
        } else {
          assertThat(numSuccessfulSetCalls.get()).isEqualTo(1);
        }
      } else {
        assertThat(numSuccessfulSetCalls.get()).isEqualTo(1);
      }
      // reset for next iteration
      numSuccessfulSetCalls.set(0);
      finalResults.clear();
    }
    executor.shutdown();
  }

  // setFuture and cancel() interact in more complicated ways than the other setters.
  public void testSetFutureCancelBash() {
    if (isWindows()) {
      return; // TODO: b/136041958 - Running very slowly on Windows CI.
    }
    final int size = 50;
    final CyclicBarrier barrier =
        new CyclicBarrier(
            2 // for the setter threads
                + size // for the listeners
                + size // for the get threads,
                + 1); // for the main thread
    final ExecutorService executor = Executors.newFixedThreadPool(barrier.getParties());
    final AtomicReference<AbstractFuture<String>> currentFuture = Atomics.newReference();
    final AtomicReference<AbstractFuture<String>> setFutureFuture = Atomics.newReference();
    final AtomicBoolean setFutureSetSuccess = new AtomicBoolean();
    final AtomicBoolean setFutureCompletionSuccess = new AtomicBoolean();
    final AtomicBoolean cancellationSuccess = new AtomicBoolean();
    Runnable cancelRunnable =
        new Runnable() {
          @Override
          public void run() {
            cancellationSuccess.set(currentFuture.get().cancel(true));
            awaitUnchecked(barrier);
          }
        };
    Runnable setFutureCompleteSuccessfullyRunnable =
        new Runnable() {
          @Override
          public void run() {
            AbstractFuture<String> future = setFutureFuture.get();
            setFutureSetSuccess.set(currentFuture.get().setFuture(future));
            setFutureCompletionSuccess.set(future.set("hello-async-world"));
            awaitUnchecked(barrier);
          }
        };
    final Set<Object> finalResults = Collections.synchronizedSet(Sets.newIdentityHashSet());
    Runnable collectResultsRunnable =
        new Runnable() {
          @Override
          public void run() {
            try {
              String result = Uninterruptibles.getUninterruptibly(currentFuture.get());
              finalResults.add(result);
            } catch (ExecutionException e) {
              finalResults.add(e.getCause());
            } catch (CancellationException e) {
              finalResults.add(CancellationException.class);
            } finally {
              awaitUnchecked(barrier);
            }
          }
        };
    Runnable collectResultsTimedGetRunnable =
        new Runnable() {
          @Override
          public void run() {
            Future<String> future = currentFuture.get();
            while (true) {
              try {
                String result = Uninterruptibles.getUninterruptibly(future, 0, SECONDS);
                finalResults.add(result);
                break;
              } catch (ExecutionException e) {
                finalResults.add(e.getCause());
                break;
              } catch (CancellationException e) {
                finalResults.add(CancellationException.class);
                break;
              } catch (TimeoutException e) {
                // loop
              }
            }
            awaitUnchecked(barrier);
          }
        };
    List<Runnable> allTasks = new ArrayList<>();
    allTasks.add(cancelRunnable);
    allTasks.add(setFutureCompleteSuccessfullyRunnable);
    for (int k = 0; k < size; k++) {
      // For each listener we add a task that submits it to the executor directly for the blocking
      // get use case and another task that adds it as a listener to the future to exercise both
      // racing addListener calls and addListener calls completing after the future completes.
      final Runnable listener =
          k % 2 == 0 ? collectResultsRunnable : collectResultsTimedGetRunnable;
      allTasks.add(listener);
      allTasks.add(
          new Runnable() {
            @Override
            public void run() {
              currentFuture.get().addListener(listener, executor);
            }
          });
    }
    assertEquals(allTasks.size() + 1, barrier.getParties()); // sanity check
    for (int i = 0; i < 1000; i++) {
      Collections.shuffle(allTasks);
      final AbstractFuture<String> future = new AbstractFuture<String>() {};
      final AbstractFuture<String> setFuture = new AbstractFuture<String>() {};
      currentFuture.set(future);
      setFutureFuture.set(setFuture);
      for (Runnable task : allTasks) {
        executor.execute(task);
      }
      awaitUnchecked(barrier);
      assertThat(future.isDone()).isTrue();
      // inspect state and ensure it is correct!
      // asserts that all get calling threads received the same value
      Object result = Iterables.getOnlyElement(finalResults);
      if (result == CancellationException.class) {
        assertTrue(future.isCancelled());
        assertTrue(cancellationSuccess.get());
        // cancellation can interleave in 3 ways
        // 1. prior to setFuture
        // 2. after setFuture before set() on the future assigned
        // 3. after setFuture and set() are called but before the listener completes.
        if (!setFutureSetSuccess.get() || !setFutureCompletionSuccess.get()) {
          // If setFuture fails or set on the future fails then it must be because that future was
          // cancelled
          assertTrue(setFuture.isCancelled());
          assertTrue(setFuture.wasInterrupted()); // we only call cancel(true)
        }
      } else {
        // set on the future completed
        assertFalse(cancellationSuccess.get());
        assertTrue(setFutureSetSuccess.get());
        assertTrue(setFutureCompletionSuccess.get());
      }
      // reset for next iteration
      setFutureSetSuccess.set(false);
      setFutureCompletionSuccess.set(false);
      cancellationSuccess.set(false);
      finalResults.clear();
    }
    executor.shutdown();
  }

  // Test to ensure that when calling setFuture with a done future only setFuture or cancel can
  // return true.
  public void testSetFutureCancelBash_withDoneFuture() {
    final CyclicBarrier barrier =
        new CyclicBarrier(
            2 // for the setter threads
                + 1 // for the blocking get thread,
                + 1); // for the main thread
    final ExecutorService executor = Executors.newFixedThreadPool(barrier.getParties());
    final AtomicReference<AbstractFuture<String>> currentFuture = Atomics.newReference();
    final AtomicBoolean setFutureSuccess = new AtomicBoolean();
    final AtomicBoolean cancellationSuccess = new AtomicBoolean();
    Callable<@Nullable Void> cancelRunnable =
        new Callable<@Nullable Void>() {
          @Override
          public @Nullable Void call() {
            cancellationSuccess.set(currentFuture.get().cancel(true));
            awaitUnchecked(barrier);
            return null;
          }
        };
    Callable<@Nullable Void> setFutureCompleteSuccessfullyRunnable =
        new Callable<@Nullable Void>() {
          final ListenableFuture<String> future = immediateFuture("hello");

          @Override
          public @Nullable Void call() {
            setFutureSuccess.set(currentFuture.get().setFuture(future));
            awaitUnchecked(barrier);
            return null;
          }
        };
    final Set<Object> finalResults = Collections.synchronizedSet(Sets.newIdentityHashSet());
    final Runnable collectResultsRunnable =
        new Runnable() {
          @Override
          public void run() {
            try {
              String result = Uninterruptibles.getUninterruptibly(currentFuture.get());
              finalResults.add(result);
            } catch (ExecutionException e) {
              finalResults.add(e.getCause());
            } catch (CancellationException e) {
              finalResults.add(CancellationException.class);
            } finally {
              awaitUnchecked(barrier);
            }
          }
        };
    List<Callable<?>> allTasks = new ArrayList<>();
    allTasks.add(cancelRunnable);
    allTasks.add(setFutureCompleteSuccessfullyRunnable);
    allTasks.add(Executors.callable(collectResultsRunnable));
    assertEquals(allTasks.size() + 1, barrier.getParties()); // sanity check
    for (int i = 0; i < 1000; i++) {
      Collections.shuffle(allTasks);
      final AbstractFuture<String> future = new AbstractFuture<String>() {};
      currentFuture.set(future);
      for (Callable<?> task : allTasks) {
        @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
        Future<?> possiblyIgnoredError = executor.submit(task);
      }
      awaitUnchecked(barrier);
      assertThat(future.isDone()).isTrue();
      // inspect state and ensure it is correct!
      // asserts that all get calling threads received the same value
      Object result = Iterables.getOnlyElement(finalResults);
      if (result == CancellationException.class) {
        assertTrue(future.isCancelled());
        assertTrue(cancellationSuccess.get());
        assertFalse(setFutureSuccess.get());
      } else {
        assertTrue(setFutureSuccess.get());
        assertFalse(cancellationSuccess.get());
      }
      // reset for next iteration
      setFutureSuccess.set(false);
      cancellationSuccess.set(false);
      finalResults.clear();
    }
    executor.shutdown();
  }

  // In a previous implementation this would cause a stack overflow after ~2000 futures chained
  // together.  Now it should only be limited by available memory (and time)
  public void testSetFuture_stackOverflow() {
    SettableFuture<String> orig = SettableFuture.create();
    SettableFuture<String> prev = orig;
    for (int i = 0; i < 100000; i++) {
      SettableFuture<String> curr = SettableFuture.create();
      prev.setFuture(curr);
      prev = curr;
    }
    // prev represents the 'innermost' future
    prev.set("done");
    assertTrue(orig.isDone());
  }

  // Verify that StackOverflowError in a long chain of SetFuture doesn't cause the entire toString
  // call to fail
  @J2ktIncompatible
  @GwtIncompatible
  @AndroidIncompatible
  public void testSetFutureToString_stackOverflow() {
    SettableFuture<String> orig = SettableFuture.create();
    SettableFuture<String> prev = orig;
    for (int i = 0; i < 100000; i++) {
      SettableFuture<String> curr = SettableFuture.create();
      prev.setFuture(curr);
      prev = curr;
    }
    // orig represents the 'outermost' future
    assertThat(orig.toString())
        .contains("Exception thrown from implementation: class java.lang.StackOverflowError");
  }

  public void testSetFuture_misbehavingFutureThrows() throws Exception {
    SettableFuture<String> future = SettableFuture.create();
    ListenableFuture<String> badFuture =
        new ListenableFuture<String>() {
          @Override
          public boolean cancel(boolean interrupt) {
            return false;
          }

          @Override
          public boolean isDone() {
            return true;
          }

          @Override
          public boolean isCancelled() {
            return false; // BAD!!
          }

          @Override
          public String get() {
            throw new CancellationException(); // BAD!!
          }

          @Override
          public String get(long time, TimeUnit unit) {
            throw new CancellationException(); // BAD!!
          }

          @Override
          public void addListener(Runnable runnable, Executor executor) {
            executor.execute(runnable);
          }
        };
    future.setFuture(badFuture);
    ExecutionException expected = getExpectingExecutionException(future);
    assertThat(expected).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
    assertThat(expected).hasCauseThat().hasMessageThat().contains(badFuture.toString());
  }

  public void testSetFuture_misbehavingFutureDoesNotThrow() throws Exception {
    SettableFuture<String> future = SettableFuture.create();
    ListenableFuture<String> badFuture =
        new ListenableFuture<String>() {
          @Override
          public boolean cancel(boolean interrupt) {
            return false;
          }

          @Override
          public boolean isDone() {
            return true;
          }

          @Override
          public boolean isCancelled() {
            return true; // BAD!!
          }

          @Override
          public String get() {
            return "foo"; // BAD!!
          }

          @Override
          public String get(long time, TimeUnit unit) {
            return "foo"; // BAD!!
          }

          @Override
          public void addListener(Runnable runnable, Executor executor) {
            executor.execute(runnable);
          }
        };
    future.setFuture(badFuture);
    assertThat(future.isCancelled()).isTrue();
  }

  public void testCancel_stackOverflow() {
    SettableFuture<String> orig = SettableFuture.create();
    SettableFuture<String> prev = orig;
    for (int i = 0; i < 100000; i++) {
      SettableFuture<String> curr = SettableFuture.create();
      prev.setFuture(curr);
      prev = curr;
    }
    // orig is the 'outermost future', this should propagate fully down the stack of futures.
    orig.cancel(true);
    assertTrue(orig.isCancelled());
    assertTrue(prev.isCancelled());
    assertTrue(prev.wasInterrupted());
  }

  public void testSetFutureSelf_cancel() {
    SettableFuture<String> orig = SettableFuture.create();
    orig.setFuture(orig);
    orig.cancel(true);
    assertTrue(orig.isCancelled());
  }

  public void testSetFutureSelf_toString() {
    SettableFuture<String> orig = SettableFuture.create();
    orig.setFuture(orig);
    assertThat(orig.toString()).contains("[status=PENDING, setFuture=[this future]]");
  }

  public void testSetSelf_toString() {
    SettableFuture<Object> orig = SettableFuture.create();
    orig.set(orig);
    assertThat(orig.toString()).contains("[status=SUCCESS, result=[this future]]");
  }

  public void testSetFutureSelf_toStringException() {
    SettableFuture<String> orig = SettableFuture.create();
    orig.setFuture(
        new AbstractFuture<String>() {
          @Override
          public String toString() {
            throw new NullPointerException();
          }
        });
    assertThat(orig.toString())
        .contains(
            "[status=PENDING, setFuture=[Exception thrown from implementation: class"
                + " java.lang.NullPointerException]]");
  }

  public void testSetIndirectSelf_toString() {
    final SettableFuture<Object> orig = SettableFuture.create();
    // unlike the above this indirection defeats the trivial cycle detection and causes a SOE
    orig.setFuture(
        new ForwardingListenableFuture<Object>() {
          @Override
          protected ListenableFuture<Object> delegate() {
            return orig;
          }
        });
    assertThat(orig.toString())
        .contains("Exception thrown from implementation: class java.lang.StackOverflowError");
  }

  // Regression test for a case where we would fail to execute listeners immediately on done futures
  // this would be observable from an afterDone callback
  public void testListenersExecuteImmediately_fromAfterDone() {
    AbstractFuture<String> f =
        new AbstractFuture<String>() {
          @Override
          protected void afterDone() {
            final AtomicBoolean ranImmediately = new AtomicBoolean();
            addListener(
                new Runnable() {
                  @Override
                  public void run() {
                    ranImmediately.set(true);
                  }
                },
                directExecutor());
            assertThat(ranImmediately.get()).isTrue();
          }
        };
    f.set("foo");
  }

  // Regression test for a case where we would fail to execute listeners immediately on done futures
  // this would be observable from a waiter that was just unblocked.
  public void testListenersExecuteImmediately_afterWaiterWakesUp() throws Exception {
    final AbstractFuture<String> f =
        new AbstractFuture<String>() {
          @Override
          protected void afterDone() {
            // this simply delays executing listeners
            try {
              Thread.sleep(SECONDS.toMillis(10));
            } catch (InterruptedException ignored) {
              Thread.currentThread().interrupt(); // preserve status
            }
          }
        };
    Thread t =
        new Thread() {
          @Override
          public void run() {
            f.set("foo");
          }
        };
    t.start();
    f.get();
    final AtomicBoolean ranImmediately = new AtomicBoolean();
    f.addListener(
        new Runnable() {
          @Override
          public void run() {
            ranImmediately.set(true);
          }
        },
        directExecutor());
    assertThat(ranImmediately.get()).isTrue();
    t.interrupt();
    t.join();
  }

  public void testCatchesUndeclaredThrowableFromListener() {
    AbstractFuture<String> f = new AbstractFuture<String>() {};
    f.set("foo");
    f.addListener(() -> sneakyThrow(new SomeCheckedException()), directExecutor());
  }

  private static final class SomeCheckedException extends Exception {}

  public void testTrustedGetFailure_completed() {
    SettableFuture<String> future = SettableFuture.create();
    future.set("261");
    assertThat(future.tryInternalFastPathGetFailure()).isNull();
  }

  public void testTrustedGetFailure_failed() {
    SettableFuture<String> future = SettableFuture.create();
    Throwable failure = new Throwable();
    future.setException(failure);
    assertThat(future.tryInternalFastPathGetFailure()).isEqualTo(failure);
  }

  public void testTrustedGetFailure_notCompleted() {
    SettableFuture<String> future = SettableFuture.create();
    assertThat(future.isDone()).isFalse();
    assertThat(future.tryInternalFastPathGetFailure()).isNull();
  }

  public void testTrustedGetFailure_canceledNoCause() {
    SettableFuture<String> future = SettableFuture.create();
    future.cancel(false);
    assertThat(future.tryInternalFastPathGetFailure()).isNull();
  }

  public void testGetFailure_completed() {
    AbstractFuture<String> future = new AbstractFuture<String>() {};
    future.set("261");
    assertThat(future.tryInternalFastPathGetFailure()).isNull();
  }

  public void testGetFailure_failed() {
    AbstractFuture<String> future = new AbstractFuture<String>() {};
    final Throwable failure = new Throwable();
    future.setException(failure);
    assertThat(future.tryInternalFastPathGetFailure()).isNull();
  }

  public void testGetFailure_notCompleted() {
    AbstractFuture<String> future = new AbstractFuture<String>() {};
    assertThat(future.isDone()).isFalse();
    assertThat(future.tryInternalFastPathGetFailure()).isNull();
  }

  public void testGetFailure_canceledNoCause() {
    AbstractFuture<String> future = new AbstractFuture<String>() {};
    future.cancel(false);
    assertThat(future.tryInternalFastPathGetFailure()).isNull();
  }

  public void testForwardExceptionFastPath() throws Exception {
    class FailFuture extends InternalFutureFailureAccess implements ListenableFuture<String> {
      Throwable failure;

      FailFuture(Throwable throwable) {
        failure = throwable;
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        throw new AssertionFailedError("cancel shouldn't be called on this object");
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return true;
      }

      @Override
      public String get() throws InterruptedException, ExecutionException {
        throw new AssertionFailedError("get() shouldn't be called on this object");
      }

      @Override
      public String get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
        return get();
      }

      @Override
      protected Throwable tryInternalFastPathGetFailure() {
        return failure;
      }

      @Override
      public void addListener(Runnable listener, Executor executor) {
        throw new AssertionFailedError("addListener() shouldn't be called on this object");
      }
    }

    final RuntimeException exception = new RuntimeException("you still didn't say the magic word!");
    SettableFuture<String> normalFuture = SettableFuture.create();
    normalFuture.setFuture(new FailFuture(exception));
    assertTrue(normalFuture.isDone());
    ExecutionException e = assertThrows(ExecutionException.class, () -> normalFuture.get());
    assertSame(exception, e.getCause());
  }

  private static void awaitUnchecked(final CyclicBarrier barrier) {
    try {
      barrier.await();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void checkStackTrace(ExecutionException e) {
    // Our call site for get() should be in the trace.
    int index = findStackFrame(e, getClass().getName(), "getExpectingExecutionException");

    assertThat(index).isNotEqualTo(0);

    // Above our method should be the call to get(). Don't assert on the class
    // because it could be some superclass.
    assertThat(e.getStackTrace()[index - 1].getMethodName()).isEqualTo("get");
  }

  private static int findStackFrame(ExecutionException e, String clazz, String method) {
    StackTraceElement[] elements = e.getStackTrace();
    for (int i = 0; i < elements.length; i++) {
      StackTraceElement element = elements[i];
      if (element.getClassName().equals(clazz) && element.getMethodName().equals(method)) {
        return i;
      }
    }
    throw new AssertionError(
        "Expected element " + clazz + "." + method + " not found in stack trace", e);
  }

  private ExecutionException getExpectingExecutionException(AbstractFuture<String> future)
      throws InterruptedException {
    try {
      String got = future.get();
      throw new AssertionError("Expected exception but got " + got);
    } catch (ExecutionException e) {
      return e;
    }
  }

  private static final class WaiterThread extends Thread {
    private final AbstractFuture<?> future;

    private WaiterThread(AbstractFuture<?> future) {
      this.future = future;
    }

    @Override
    public void run() {
      try {
        future.get();
      } catch (Exception e) {
        // nothing
      }
    }

    @SuppressWarnings("ThreadPriorityCheck") // TODO: b/175898629 - Consider onSpinWait.
    void awaitWaiting() {
      while (!isBlocked()) {
        if (getState() == State.TERMINATED) {
          throw new RuntimeException("Thread exited");
        }
        Thread.yield();
      }
    }

    private boolean isBlocked() {
      return getState() == Thread.State.WAITING && LockSupport.getBlocker(this) == future;
    }
  }

  static final class TimedWaiterThread extends Thread {
    private final AbstractFuture<?> future;
    private final long timeout;
    private final TimeUnit unit;
    private Exception exception;
    private volatile long startTime;
    private long timeSpentBlocked;

    TimedWaiterThread(AbstractFuture<?> future, long timeout, TimeUnit unit) {
      this.future = future;
      this.timeout = timeout;
      this.unit = unit;
    }

    @Override
    public void run() {
      startTime = System.nanoTime();
      try {
        future.get(timeout, unit);
      } catch (Exception e) {
        // nothing
        exception = e;
      } finally {
        timeSpentBlocked = System.nanoTime() - startTime;
      }
    }

    @SuppressWarnings("ThreadPriorityCheck") // TODO: b/175898629 - Consider onSpinWait.
    void awaitWaiting() {
      while (!isBlocked()) {
        if (getState() == State.TERMINATED) {
          throw new RuntimeException("Thread exited");
        }
        Thread.yield();
      }
    }

    private boolean isBlocked() {
      return getState() == Thread.State.TIMED_WAITING && LockSupport.getBlocker(this) == future;
    }
  }

  private static final class PollingThread extends Thread {
    private final AbstractFuture<?> future;
    private final CountDownLatch completedIteration = new CountDownLatch(10);

    private PollingThread(AbstractFuture<?> future) {
      this.future = future;
    }

    @Override
    public void run() {
      while (true) {
        try {
          future.get(0, SECONDS);
          return;
        } catch (InterruptedException | ExecutionException e) {
          return;
        } catch (TimeoutException e) {
          // do nothing
        } finally {
          completedIteration.countDown();
        }
      }
    }

    void awaitInLoop() {
      Uninterruptibles.awaitUninterruptibly(completedIteration);
    }
  }

  private static final class InterruptibleFuture extends AbstractFuture<String> {
    boolean interruptTaskWasCalled;

    @Override
    protected void interruptTask() {
      assertFalse(interruptTaskWasCalled);
      interruptTaskWasCalled = true;
    }
  }

  private static boolean isWindows() {
    return OS_NAME.value().startsWith("Windows");
  }
}
