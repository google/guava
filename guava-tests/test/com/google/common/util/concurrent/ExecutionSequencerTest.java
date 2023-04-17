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
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.testing.GcFinalization;
import com.google.common.testing.TestLogHandler;
import com.google.j2objc.annotations.J2ObjCIncompatible;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Tests for {@link ExecutionSequencer} */
public class ExecutionSequencerTest extends TestCase {

  ExecutorService executor;

  private ExecutionSequencer serializer;
  private SettableFuture<@Nullable Void> firstFuture;
  private TestCallable firstCallable;

  @Override
  public void setUp() throws Exception {
    executor = Executors.newCachedThreadPool();
    serializer = ExecutionSequencer.create();
    firstFuture = SettableFuture.create();
    firstCallable = new TestCallable(firstFuture);
  }

  @Override
  public void tearDown() throws Exception {
    executor.shutdown();
  }

  public void testCallableStartsAfterFirstFutureCompletes() {
    @SuppressWarnings({"unused", "nullness"})
    Future<?> possiblyIgnoredError = serializer.submitAsync(firstCallable, directExecutor());
    TestCallable secondCallable = new TestCallable(Futures.<Void>immediateFuture(null));
    @SuppressWarnings({"unused", "nullness"})
    Future<?> possiblyIgnoredError1 = serializer.submitAsync(secondCallable, directExecutor());
    assertThat(firstCallable.called).isTrue();
    assertThat(secondCallable.called).isFalse();
    firstFuture.set(null);
    assertThat(secondCallable.called).isTrue();
  }

  public void testCancellationDoesNotViolateSerialization() {
    @SuppressWarnings({"unused", "nullness"})
    Future<?> possiblyIgnoredError = serializer.submitAsync(firstCallable, directExecutor());
    TestCallable secondCallable = new TestCallable(Futures.<Void>immediateFuture(null));
    ListenableFuture<@Nullable Void> secondFuture =
        serializer.submitAsync(secondCallable, directExecutor());
    TestCallable thirdCallable = new TestCallable(Futures.<Void>immediateFuture(null));
    @SuppressWarnings({"unused", "nullness"})
    Future<?> possiblyIgnoredError1 = serializer.submitAsync(thirdCallable, directExecutor());
    secondFuture.cancel(true);
    assertThat(secondCallable.called).isFalse();
    assertThat(thirdCallable.called).isFalse();
    firstFuture.set(null);
    assertThat(secondCallable.called).isFalse();
    assertThat(thirdCallable.called).isTrue();
  }

  public void testCancellationMultipleThreads() throws Exception {
    final BlockingCallable blockingCallable = new BlockingCallable();
    ListenableFuture<@Nullable Void> unused = serializer.submit(blockingCallable, executor);
    ListenableFuture<Boolean> future2 =
        serializer.submit(
            new Callable<Boolean>() {
              @Override
              public Boolean call() {
                return blockingCallable.isRunning();
              }
            },
            directExecutor());

    // Wait for the first task to be started in the background. It will block until we explicitly
    // stop it.
    blockingCallable.waitForStart();

    // Give the second task a chance to (incorrectly) start up while the first task is running.
    assertThat(future2.isDone()).isFalse();

    // Stop the first task. The second task should then run.
    blockingCallable.stop();
    executor.shutdown();
    assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    assertThat(getDone(future2)).isFalse();
  }

  public void testSecondTaskWaitsForFirstEvenIfCancelled() throws Exception {
    final BlockingCallable blockingCallable = new BlockingCallable();
    ListenableFuture<@Nullable Void> future1 = serializer.submit(blockingCallable, executor);
    ListenableFuture<Boolean> future2 =
        serializer.submit(
            new Callable<Boolean>() {
              @Override
              public Boolean call() {
                return blockingCallable.isRunning();
              }
            },
            directExecutor());

    // Wait for the first task to be started in the background. It will block until we explicitly
    // stop it.
    blockingCallable.waitForStart();

    // This time, cancel the future for the first task. The task remains running, only the future
    // is cancelled.
    future1.cancel(false);

    // Give the second task a chance to (incorrectly) start up while the first task is running.
    // (This is the assertion that fails.)
    assertThat(future2.isDone()).isFalse();

    // Stop the first task. The second task should then run.
    blockingCallable.stop();
    executor.shutdown();
    assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    assertThat(getDone(future2)).isFalse();
  }

  @GwtIncompatible
  @J2ObjCIncompatible // gc
  @AndroidIncompatible
  public void testCancellationWithReferencedObject() throws Exception {
    Object toBeGCed = new Object();
    WeakReference<Object> ref = new WeakReference<>(toBeGCed);
    final SettableFuture<@Nullable Void> settableFuture = SettableFuture.create();
    ListenableFuture<?> ignored =
        serializer.submitAsync(
            new AsyncCallable<@Nullable Void>() {
              @Override
              public ListenableFuture<@Nullable Void> call() {
                return settableFuture;
              }
            },
            directExecutor());
    serializer.submit(toStringCallable(toBeGCed), directExecutor()).cancel(true);
    toBeGCed = null;
    GcFinalization.awaitClear(ref);
  }

  private static Callable<String> toStringCallable(final Object object) {
    return new Callable<String>() {
      @Override
      public String call() {
        return object.toString();
      }
    };
  }

  public void testCancellationDuringReentrancy() throws Exception {
    TestLogHandler logHandler = new TestLogHandler();
    Logger.getLogger(AbstractFuture.class.getName()).addHandler(logHandler);

    List<Future<?>> results = new ArrayList<>();
    final Runnable[] manualExecutorTask = new Runnable[1];
    Executor manualExecutor =
        new Executor() {
          @Override
          public void execute(Runnable task) {
            manualExecutorTask[0] = task;
          }
        };

    results.add(serializer.submit(Callables.returning(null), manualExecutor));
    final Future<?>[] thingToCancel = new Future<?>[1];
    results.add(
        serializer.submit(
            new Callable<@Nullable Void>() {
              @Override
              public @Nullable Void call() {
                thingToCancel[0].cancel(false);
                return null;
              }
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
    final SettableFuture<@Nullable Void> settableFuture = SettableFuture.create();
    ArrayList<ListenableFuture<@Nullable Void>> results = new ArrayList<>(50_001);
    results.add(
        serializer.submitAsync(
            new AsyncCallable<@Nullable Void>() {
              @Override
              public ListenableFuture<@Nullable Void> call() {
                return settableFuture;
              }
            },
            directExecutor()));
    for (int i = 0; i < 50_000; i++) {
      results.add(serializer.submit(Callables.<Void>returning(null), directExecutor()));
    }
    settableFuture.set(null);
    getDone(allAsList(results));
  }

  public void testAvoidsStackOverflow_manyCancelled() throws Exception {
    final SettableFuture<@Nullable Void> settableFuture = SettableFuture.create();
    ListenableFuture<@Nullable Void> unused =
        serializer.submitAsync(
            new AsyncCallable<@Nullable Void>() {
              @Override
              public ListenableFuture<@Nullable Void> call() {
                return settableFuture;
              }
            },
            directExecutor());
    for (int i = 0; i < 50_000; i++) {
      serializer.submit(Callables.<Void>returning(null), directExecutor()).cancel(true);
    }
    ListenableFuture<Integer> stackDepthCheck =
        serializer.submit(
            new Callable<Integer>() {
              @Override
              public Integer call() {
                return Thread.currentThread().getStackTrace().length;
              }
            },
            directExecutor());
    settableFuture.set(null);
    assertThat(getDone(stackDepthCheck))
        .isLessThan(Thread.currentThread().getStackTrace().length + 100);
  }

  public void testAvoidsStackOverflow_alternatingCancelledAndSubmitted() throws Exception {
    final SettableFuture<@Nullable Void> settableFuture = SettableFuture.create();
    ListenableFuture<@Nullable Void> unused =
        serializer.submitAsync(
            new AsyncCallable<@Nullable Void>() {
              @Override
              public ListenableFuture<@Nullable Void> call() {
                return settableFuture;
              }
            },
            directExecutor());
    for (int i = 0; i < 25_000; i++) {
      serializer.submit(Callables.<Void>returning(null), directExecutor()).cancel(true);
      unused = serializer.submit(Callables.<Void>returning(null), directExecutor());
    }
    ListenableFuture<Integer> stackDepthCheck =
        serializer.submit(
            new Callable<Integer>() {
              @Override
              public Integer call() {
                return Thread.currentThread().getStackTrace().length;
              }
            },
            directExecutor());
    settableFuture.set(null);
    assertThat(getDone(stackDepthCheck))
        .isLessThan(Thread.currentThread().getStackTrace().length + 100);
  }

  private static Function<Integer, Integer> add(final int delta) {
    return new Function<Integer, Integer>() {
      @Override
      public Integer apply(Integer input) {
        return input + delta;
      }
    };
  }

  private static AsyncCallable<Integer> asyncAdd(
      final ListenableFuture<Integer> future, final int delta, final Executor executor) {
    return new AsyncCallable<Integer>() {
      @Override
      public ListenableFuture<Integer> call() throws Exception {
        return Futures.transform(future, add(delta), executor);
      }
    };
  }

  private static final class LongHolder {
    long count;
  }

  private static final int ITERATION_COUNT = 50_000;
  private static final int DIRECT_EXECUTIONS_PER_THREAD = 100;

  @GwtIncompatible // threads
  public void testAvoidsStackOverflow_multipleThreads() throws Exception {
    final LongHolder holder = new LongHolder();
    final ArrayList<ListenableFuture<Integer>> lengthChecks = new ArrayList<>();
    final List<Integer> completeLengthChecks;
    final int baseStackDepth;
    ExecutorService service = Executors.newFixedThreadPool(5);
    try {
      // Avoid counting frames from the executor itself, or the ExecutionSequencer
      baseStackDepth =
          serializer
              .submit(
                  new Callable<Integer>() {
                    @Override
                    public Integer call() {
                      return Thread.currentThread().getStackTrace().length;
                    }
                  },
                  service)
              .get();
      final SettableFuture<@Nullable Void> settableFuture = SettableFuture.create();
      ListenableFuture<?> unused =
          serializer.submitAsync(
              new AsyncCallable<@Nullable Void>() {
                @Override
                public ListenableFuture<@Nullable Void> call() {
                  return settableFuture;
                }
              },
              directExecutor());
      for (int i = 0; i < 50_000; i++) {
        if (i % DIRECT_EXECUTIONS_PER_THREAD == 0) {
          // after some number of iterations, switch threads
          unused =
              serializer.submit(
                  new Callable<@Nullable Void>() {
                    @Override
                    public @Nullable Void call() {
                      holder.count++;
                      return null;
                    }
                  },
                  service);
        } else if (i % DIRECT_EXECUTIONS_PER_THREAD == DIRECT_EXECUTIONS_PER_THREAD - 1) {
          // When at max depth, record stack trace depth
          lengthChecks.add(
              serializer.submit(
                  new Callable<Integer>() {
                    @Override
                    public Integer call() {
                      holder.count++;
                      return Thread.currentThread().getStackTrace().length;
                    }
                  },
                  directExecutor()));
        } else {
          // Otherwise, schedule a task on directExecutor
          unused =
              serializer.submit(
                  new Callable<@Nullable Void>() {
                    @Override
                    public @Nullable Void call() {
                      holder.count++;
                      return null;
                    }
                  },
                  directExecutor());
        }
      }
      settableFuture.set(null);
      completeLengthChecks = allAsList(lengthChecks).get();
    } finally {
      service.shutdown();
    }
    assertThat(holder.count).isEqualTo(ITERATION_COUNT);
    for (int length : completeLengthChecks) {
      // Verify that at max depth, less than one stack frame per submitted task was consumed
      assertThat(length - baseStackDepth).isLessThan(DIRECT_EXECUTIONS_PER_THREAD / 2);
    }
  }

  @SuppressWarnings("ObjectToString") // Intended behavior
  public void testToString() {
    Future<?> unused = serializer.submitAsync(firstCallable, directExecutor());
    TestCallable secondCallable = new TestCallable(SettableFuture.<Void>create());
    Future<?> second = serializer.submitAsync(secondCallable, directExecutor());
    assertThat(secondCallable.called).isFalse();
    assertThat(second.toString()).contains(secondCallable.toString());
    firstFuture.set(null);
    assertThat(second.toString()).contains(secondCallable.future.toString());
  }

  private static class BlockingCallable implements Callable<@Nullable Void> {
    private final CountDownLatch startLatch = new CountDownLatch(1);
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    private volatile boolean running = false;

    @Override
    public @Nullable Void call() throws InterruptedException {
      running = true;
      startLatch.countDown();
      stopLatch.await();
      running = false;
      return null;
    }

    public void waitForStart() throws InterruptedException {
      startLatch.await();
    }

    public void stop() {
      stopLatch.countDown();
    }

    public boolean isRunning() {
      return running;
    }
  }

  private static final class TestCallable implements AsyncCallable<@Nullable Void> {

    private final ListenableFuture<@Nullable Void> future;
    private boolean called = false;

    private TestCallable(ListenableFuture<@Nullable Void> future) {
      this.future = future;
    }

    @Override
    public ListenableFuture<@Nullable Void> call() throws Exception {
      called = true;
      return future;
    }
  }
}
