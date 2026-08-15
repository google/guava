/*
 * Copyright (C) 2018 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.util.concurrent;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.getDone;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.immediateVoidFuture;
import static com.google.common.util.concurrent.Futures.transform;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.GcFinalization;
import com.google.common.testing.TestLogHandler;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/** Tests for {@link ExecutionSequencer} */
@NullUnmarked
@GwtIncompatible
@J2ktIncompatible
public class ExecutionSequencerTest extends TestCase {
  private final ExecutionSequencer serializer = ExecutionSequencer.create();
  private final ExecutorService backgroundThread = newSingleThreadExecutor();

  @Override
  protected void tearDown() {
    backgroundThread.shutdown();
  }

  public void testCallableStartsAfterFirstFutureCompletes() throws Exception {
    SettableFuture<@Nullable Void> returnedByFirstCallable = SettableFuture.create();
    TestAsyncCallable firstCallable = new TestAsyncCallable(returnedByFirstCallable);
    Future<?> firstSubmission = serializer.submitAsync(firstCallable, directExecutor());

    TestAsyncCallable secondCallable = new TestAsyncCallable(immediateVoidFuture());
    Future<?> secondSubmission = serializer.submitAsync(secondCallable, directExecutor());

    assertThat(firstCallable.called).isTrue();
    assertThat(secondCallable.called).isFalse();
    returnedByFirstCallable.set(null);
    assertThat(secondCallable.called).isTrue();
    firstSubmission.get();
    secondSubmission.get();
  }

  public void testCancellationDoesNotViolateSerialization() throws Exception {
    SettableFuture<@Nullable Void> returnedByFirstCallable = SettableFuture.create();
    AsyncCallable<@Nullable Void> firstCallable = () -> returnedByFirstCallable;
    Future<?> firstSubmission = serializer.submitAsync(firstCallable, directExecutor());

    TestAsyncCallable secondCallable = new TestAsyncCallable(immediateVoidFuture());
    ListenableFuture<@Nullable Void> secondSubmission =
        serializer.submitAsync(secondCallable, directExecutor());

    TestAsyncCallable thirdCallable = new TestAsyncCallable(immediateVoidFuture());
    Future<?> thirdSubmission = serializer.submitAsync(thirdCallable, directExecutor());

    secondSubmission.cancel(true);
    assertThat(secondCallable.called).isFalse();
    assertThat(thirdCallable.called).isFalse();
    returnedByFirstCallable.set(null);
    assertThat(secondCallable.called).isFalse();
    assertThat(thirdCallable.called).isTrue();
    firstSubmission.get();
    thirdSubmission.get();
  }

  public void testCancellationMultipleThreads() throws Exception {
    BlockingCallable firstCallable = new BlockingCallable();
    ListenableFuture<@Nullable Void> unused = serializer.submit(firstCallable, backgroundThread);
    ListenableFuture<Boolean> secondSubmission =
        serializer.submit(firstCallable::isRunning, directExecutor());

    // Wait for the first task to be started in the background. It will block until we explicitly
    // stop it.
    firstCallable.waitForStart();

    /*
     * If the second task were going to (incorrectly) start while the first task is running, it
     * would presumably have done so by now, since it was submitted earlier with directExecutor().
     */
    assertThat(secondSubmission.isDone()).isFalse();

    // Stop the first task. The second task should then run.
    firstCallable.stop();
    backgroundThread.shutdown();
    assertThat(backgroundThread.awaitTermination(10, SECONDS)).isTrue();
    assertThat(getDone(secondSubmission)).isFalse();
  }

  public void testSecondTaskWaitsForFirstEvenIfCancelled() throws Exception {
    BlockingCallable firstCallable = new BlockingCallable();
    ListenableFuture<@Nullable Void> firstSubmission =
        serializer.submit(firstCallable, backgroundThread);
    ListenableFuture<Boolean> secondSubmission =
        serializer.submit(firstCallable::isRunning, directExecutor());

    // Wait for the first task to be started in the background. It will block until we explicitly
    // stop it.
    firstCallable.waitForStart();

    // This time, cancel the future for the first task. The task remains running, only the future
    // is cancelled.
    firstSubmission.cancel(false);

    /*
     * If the second task were going to (incorrectly) start while the first task is running, it
     * would presumably have done so by now, since it was submitted earlier with directExecutor().
     */
    assertThat(secondSubmission.isDone()).isFalse();

    // Stop the first task. The second task should then run.
    firstCallable.stop();
    backgroundThread.shutdown();
    assertThat(backgroundThread.awaitTermination(10, SECONDS)).isTrue();
    assertThat(getDone(secondSubmission)).isFalse();
  }

  public void testCancellationWithReferencedObject() {
    Object toBeGCed = new Object();
    WeakReference<Object> ref = new WeakReference<>(toBeGCed);
    SettableFuture<@Nullable Void> settableFuture = SettableFuture.create();
    ListenableFuture<?> ignored = serializer.submitAsync(() -> settableFuture, directExecutor());
    serializer.submit(toStringCallable(toBeGCed), directExecutor()).cancel(true);
    toBeGCed = null;
    GcFinalization.awaitClear(ref);
  }

  private static Callable<String> toStringCallable(Object object) {
    return object::toString;
  }

  public void testCancellationDuringReentrancy() throws Exception {
    TestLogHandler logHandler = new TestLogHandler();
    Logger.getLogger(AbstractFuture.class.getName()).addHandler(logHandler);

    List<Future<?>> results = new ArrayList<>();
    Runnable[] manualExecutorTask = new Runnable[1];
    Executor manualExecutor = task -> manualExecutorTask[0] = task;

    results.add(serializer.submit(Callables.returning(null), manualExecutor));
    Future<?>[] thingToCancel = new Future<?>[1];
    results.add(
        serializer.submit(
            () -> {
              thingToCancel[0].cancel(false);
              return null;
            },
            directExecutor()));
    thingToCancel[0] = serializer.submit(Callables.returning(null), directExecutor());
    results.add(thingToCancel[0]);
    // Enqueue more than enough tasks to force reentrancy.
    for (int i = 0; i < 5; i++) {
      results.add(serializer.submit(Callables.returning(null), directExecutor()));
    }

    manualExecutorTask[0].run();

    for (Future<?> result : results) {
      if (!result.isCancelled()) {
        result.get(10, SECONDS);
      }
      // TODO(cpovirk): Verify that the cancelled futures are exactly ones that we expect.
    }

    assertThat(logHandler.getStoredLogRecords()).isEmpty();
  }

  public void testAvoidsStackOverflow_manySubmitted() throws Exception {
    SettableFuture<@Nullable Void> settableFuture = SettableFuture.create();
    ArrayList<ListenableFuture<@Nullable Void>> results = new ArrayList<>(50_001);
    results.add(serializer.submitAsync(() -> settableFuture, directExecutor()));
    for (int i = 0; i < 50_000; i++) {
      results.add(serializer.submit(Callables.returning(null), directExecutor()));
    }
    settableFuture.set(null);
    getDone(allAsList(results));
  }

  public void testAvoidsStackOverflow_manyCancelled() throws Exception {
    SettableFuture<@Nullable Void> settableFuture = SettableFuture.create();
    ListenableFuture<@Nullable Void> unused =
        serializer.submitAsync(() -> settableFuture, directExecutor());
    for (int i = 0; i < 50_000; i++) {
      serializer.submit(Callables.<Void>returning(null), directExecutor()).cancel(true);
    }
    ListenableFuture<Integer> stackDepthCheck =
        serializer.submit(() -> currentThread().getStackTrace().length, directExecutor());
    settableFuture.set(null);
    assertThat(getDone(stackDepthCheck)).isLessThan(currentThread().getStackTrace().length + 100);
  }

  public void testAvoidsStackOverflow_alternatingCancelledAndSubmitted() throws Exception {
    SettableFuture<@Nullable Void> settableFuture = SettableFuture.create();
    ListenableFuture<@Nullable Void> unused =
        serializer.submitAsync(() -> settableFuture, directExecutor());
    for (int i = 0; i < 25_000; i++) {
      serializer.submit(Callables.<Void>returning(null), directExecutor()).cancel(true);
      unused = serializer.submit(Callables.returning(null), directExecutor());
    }
    ListenableFuture<Integer> stackDepthCheck =
        serializer.submit(() -> currentThread().getStackTrace().length, directExecutor());
    settableFuture.set(null);
    assertThat(getDone(stackDepthCheck)).isLessThan(currentThread().getStackTrace().length + 100);
  }

  private static AsyncCallable<Integer> asyncAdd(
      ListenableFuture<Integer> future, int delta, Executor executor) {
    return () -> transform(future, input -> input + delta, executor);
  }

  public void testSubmittedChainOfFutures() throws Exception {
    SettableFuture<Integer> inputToFirst = SettableFuture.create();
    Queue<Runnable> submittedToFirstExecutor = new ArrayDeque<>();
    Executor firstExecutor = submittedToFirstExecutor::add;
    AsyncCallable<Integer> firstCallable = asyncAdd(inputToFirst, 5, firstExecutor);
    AsyncCallable<Integer> secondCallable = () -> immediateFuture(222);

    // Submit to the sequencer
    ListenableFuture<Integer> firstSubmission =
        serializer.submitAsync(firstCallable, directExecutor());
    ListenableFuture<Integer> secondSubmission =
        serializer.submitAsync(secondCallable, directExecutor());

    inputToFirst.set(10);
    // Because transformation on inputToFirst is pending...
    assertThat(submittedToFirstExecutor).isNotEmpty();
    // ...the work of secondCallable can't start...
    assertThat(secondSubmission.isDone()).isFalse();

    // Both callables should now get executed.
    submittedToFirstExecutor.forEach(Runnable::run);
    assertThat(getDone(firstSubmission)).isEqualTo(10 + 5);
    assertThat(getDone(secondSubmission)).isEqualTo(222);
  }

  public void testSubmittedChainOfFutures_outputCancelled() throws Exception {
    SettableFuture<Integer> inputToFirst = SettableFuture.create();
    Queue<Runnable> submittedToFirstExecutor = new ArrayDeque<>();
    Executor firstExecutor = submittedToFirstExecutor::add;
    AsyncCallable<Integer> firstCallable = asyncAdd(inputToFirst, 1, firstExecutor);
    AsyncCallable<Integer> secondCallable = () -> immediateFuture(222);

    // Submit to the sequencer
    ListenableFuture<Integer> firstSubmission =
        serializer.submitAsync(firstCallable, directExecutor());
    ListenableFuture<Integer> secondSubmission =
        serializer.submitAsync(secondCallable, directExecutor());

    firstSubmission.cancel(true);
    /*
     * Because we don't propagate cancellation to inputToFirst, it hasn't completed, the listener
     * added by firstCallable's Futures.transform hasn't been submitted...
     */
    assertThat(submittedToFirstExecutor).isEmpty();
    // ...and the serializer won't submit secondCallable until all firstCallable work is done...
    assertThat(secondSubmission.isDone()).isFalse();

    // secondCallable should be executed only when the underlying future completes.
    inputToFirst.set(10);
    // as well as the transformation in firstCallable, despite the output having been cancelled
    assertThat(secondSubmission.isDone()).isFalse();

    // With the output from firstCallable now complete, secondSubmission will run & complete
    submittedToFirstExecutor.forEach(Runnable::run);
    assertThat(getDone(secondSubmission)).isEqualTo(222);
  }

  public void testSubmittedChainOfFutures_internallyCancelled() throws Exception {
    SettableFuture<Integer> returnedByFirstCallable = SettableFuture.create();
    AsyncCallable<Integer> firstCallable = () -> returnedByFirstCallable;
    AsyncCallable<Integer> secondCallable = () -> immediateFuture(222);

    ListenableFuture<Integer> firstSubmission =
        serializer.submitAsync(firstCallable, directExecutor());
    ListenableFuture<Integer> secondSubmission =
        serializer.submitAsync(secondCallable, directExecutor());

    returnedByFirstCallable.cancel(false);

    /*
     * Because the future was internally cancelled, we immediately run the next enqueued task; i.e.
     * unlike testSubmittedChainOfFutures_outputCancelled which stops cancellation propagation
     */
    assertThat(secondSubmission.isDone()).isTrue();
    assertThat(getDone(secondSubmission)).isEqualTo(222);
    assertThat(firstSubmission.isDone()).isTrue();
  }

  private static final class LongHolder {
    long count;
  }

  private static final int ITERATION_COUNT = 50_000;
  private static final int DIRECT_EXECUTIONS_PER_THREAD = 100;

  public void testAvoidsStackOverflow_multipleThreads() throws Exception {
    LongHolder holder = new LongHolder();
    ArrayList<ListenableFuture<Integer>> lengthChecks = new ArrayList<>();
    List<Integer> completeLengthChecks;
    int baseStackDepth;
    ExecutorService threadPool = newFixedThreadPool(5);
    try {
      // Avoid counting frames from the executor itself, or the ExecutionSequencer
      baseStackDepth =
          serializer.submit(() -> currentThread().getStackTrace().length, threadPool).get();
      SettableFuture<@Nullable Void> settableFuture = SettableFuture.create();
      ListenableFuture<?> unused = serializer.submitAsync(() -> settableFuture, directExecutor());
      for (int i = 0; i < 50_000; i++) {
        if (i % DIRECT_EXECUTIONS_PER_THREAD == 0) {
          // after some number of iterations, switch threads
          unused =
              serializer.submit(
                  () -> {
                    holder.count++;
                    return null;
                  },
                  threadPool);
        } else if (i % DIRECT_EXECUTIONS_PER_THREAD == DIRECT_EXECUTIONS_PER_THREAD - 1) {
          // When at max depth, record stack trace depth
          lengthChecks.add(
              serializer.submit(
                  () -> {
                    holder.count++;
                    return currentThread().getStackTrace().length;
                  },
                  directExecutor()));
        } else {
          // Otherwise, schedule a task on directExecutor
          unused =
              serializer.submit(
                  () -> {
                    holder.count++;
                    return null;
                  },
                  directExecutor());
        }
      }
      settableFuture.set(null);
      completeLengthChecks = allAsList(lengthChecks).get();
    } finally {
      threadPool.shutdown();
    }
    assertThat(holder.count).isEqualTo(ITERATION_COUNT);
    for (int length : completeLengthChecks) {
      // Verify that at max depth, less than one stack frame per submitted task was consumed
      assertThat(length - baseStackDepth).isLessThan(DIRECT_EXECUTIONS_PER_THREAD / 2);
    }
  }

  /*
   * Verifies that when a non-reentrant delegate executor rejects a task (e.g., throwing a
   * RejectedExecutionException because a bounded queue is full), ExecutionSequencer catches the
   * failure and unblocks the queue, preventing subsequent tasks from stalling or leaking.
   */
  public void testRecoversFromNonReentrantRejectedExecution() throws Exception {
    SettableFuture<@Nullable Void> returnedByFirstCallable = SettableFuture.create();
    AsyncCallable<@Nullable Void> firstCallable = () -> returnedByFirstCallable;

    // 1. Submit Task 1 (firstCallable) using directExecutor. It blocks because firstFuture is not
    // yet set.
    Future<?> unused = serializer.submitAsync(firstCallable, directExecutor());

    Executor rejectingExecutor =
        task -> {
          throw new RejectedExecutionException();
        };
    TestAsyncCallable secondCallable = new TestAsyncCallable(immediateVoidFuture());

    // 2. Submit Task 2 using the rejectingExecutor. When Task 1 completes, ExecutionSequencer will
    // attempt to dispatch Task 2 via rejectingExecutor.execute(...), which will throw
    // RejectedExecutionException.
    Future<?> failingFuture = serializer.submitAsync(secondCallable, rejectingExecutor);

    // 3. Submit Task 3 using directExecutor. Task 3 is queued after Task 2 in ExecutionSequencer.
    TestAsyncCallable thirdCallable = new TestAsyncCallable(immediateVoidFuture());
    ListenableFuture<@Nullable Void> thirdFuture =
        serializer.submitAsync(thirdCallable, directExecutor());

    // Complete Task 1's future. This triggers listener notification for Task 2.
    // Task 2's dispatch fails with RejectedExecutionException inside rejectingExecutor.execute().
    returnedByFirstCallable.set(null);

    // Verify queue recovery: Task 2's failure is handled, and Task 3 runs successfully.
    assertThat(secondCallable.called).isFalse();
    assertThrows(ExecutionException.class, () -> getDone(failingFuture));
    assertThat(thirdCallable.called).isTrue();
    assertThat(thirdFuture.isDone()).isTrue();
  }

  /*
   * Verifies that when a reentrant delegate executor rejects a task, TaskNonReentrantExecutor.run()
   * handles the failure without stalling the queue or leaking subsequent submitted tasks.
   */
  public void testRecoversFromReentrantRejectedExecution() throws Exception {
    Executor rejectingExecutor =
        task -> {
          throw new RejectedExecutionException();
        };

    TestAsyncCallable secondCallable = new TestAsyncCallable(immediateVoidFuture());
    TestAsyncCallable thirdCallable = new TestAsyncCallable(immediateVoidFuture());
    AtomicReference<ListenableFuture<@Nullable Void>> secondSubmission = new AtomicReference<>();
    AtomicReference<ListenableFuture<@Nullable Void>> thirdSubmission = new AtomicReference<>();

    // Submit an outer task using directExecutor.
    // Inside its execution, we submit Task 2 (rejectingExecutor) and Task 3 (directExecutor)
    // reentrantly to test reentrant queue draining error handling.
    Future<?> unused =
        serializer.submitAsync(
            () -> {
              // Reentrant submission of Task 2 to rejectingExecutor.
              secondSubmission.set(serializer.submitAsync(secondCallable, rejectingExecutor));
              // Reentrant submission of Task 3 to directExecutor.
              thirdSubmission.set(serializer.submitAsync(thirdCallable, directExecutor()));
              return immediateVoidFuture();
            },
            directExecutor());

    // Verify reentrant queue recovery: Task 3 runs successfully despite Task 2's executor
    // rejection.
    assertThat(secondCallable.called).isFalse();
    assertThrows(ExecutionException.class, () -> getDone(secondSubmission.get()));
    assertThat(thirdCallable.called).isTrue();
    assertThat(thirdSubmission.get().isDone()).isTrue();
  }

  @SuppressWarnings("ObjectToString") // Intended behavior
  public void testToString() {
    SettableFuture<@Nullable Void> returnedByFirstCallable = SettableFuture.create();
    AsyncCallable<@Nullable Void> firstCallable = () -> returnedByFirstCallable;
    Future<?> unused = serializer.submitAsync(firstCallable, directExecutor());

    TestAsyncCallable secondCallable = new TestAsyncCallable(SettableFuture.create());
    Future<?> secondSubmission = serializer.submitAsync(secondCallable, directExecutor());

    assertThat(secondCallable.called).isFalse();
    assertThat(secondSubmission.toString()).contains(secondCallable.toString());
    returnedByFirstCallable.set(null);
    assertThat(secondSubmission.toString()).contains(secondCallable.resultFuture.toString());
  }

  private static final class BlockingCallable implements Callable<@Nullable Void> {
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch stopLatch = new CountDownLatch(1);

    volatile boolean running = false;

    @Override
    public @Nullable Void call() throws InterruptedException {
      running = true;
      startLatch.countDown();
      stopLatch.await();
      running = false;
      return null;
    }

    void waitForStart() throws InterruptedException {
      startLatch.await();
    }

    void stop() {
      stopLatch.countDown();
    }

    boolean isRunning() {
      return running;
    }
  }

  private static final class TestAsyncCallable implements AsyncCallable<@Nullable Void> {
    final ListenableFuture<@Nullable Void> resultFuture;
    boolean called;

    TestAsyncCallable(ListenableFuture<@Nullable Void> resultFuture) {
      this.resultFuture = resultFuture;
    }

    @Override
    public ListenableFuture<@Nullable Void> call() {
      called = true;
      return resultFuture;
    }
  }
}
