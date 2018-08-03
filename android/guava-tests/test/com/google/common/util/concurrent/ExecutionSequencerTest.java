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
import static com.google.common.util.concurrent.Futures.getDone;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ExecutionSequencer} */
@RunWith(JUnit4.class)
public class ExecutionSequencerTest {

  ExecutorService executor;

  private ExecutionSequencer serializer;
  private SettableFuture<Void> firstFuture;
  private TestCallable firstCallable;

  @Before
  public void setUp() throws Exception {
    executor = Executors.newCachedThreadPool();
    serializer = ExecutionSequencer.create();
    firstFuture = SettableFuture.create();
    firstCallable = new TestCallable(firstFuture);
  }

  @After
  public void tearDown() throws Exception {
    executor.shutdown();
  }

  @Test
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

  @Test
  public void testCancellationNotPropagatedIfAlreadyStarted() {
    serializer.submitAsync(firstCallable, directExecutor()).cancel(true);
    assertThat(firstFuture.isCancelled()).isFalse();
  }

  @Test
  public void testCancellationDoesNotViolateSerialization() {
    @SuppressWarnings({"unused", "nullness"})
    Future<?> possiblyIgnoredError = serializer.submitAsync(firstCallable, directExecutor());
    TestCallable secondCallable = new TestCallable(Futures.<Void>immediateFuture(null));
    ListenableFuture<Void> secondFuture = serializer.submitAsync(secondCallable, directExecutor());
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

  @Test
  public void testCancellationMultipleThreads() throws Exception {
    final BlockingCallable blockingCallable = new BlockingCallable();
    ListenableFuture<Void> unused = serializer.submit(blockingCallable, executor);
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

  @Test
  public void secondTaskWaitsForFirstEvenIfCancelled() throws Exception {
    final BlockingCallable blockingCallable = new BlockingCallable();
    ListenableFuture<Void> future1 = serializer.submit(blockingCallable, executor);
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

  private static class BlockingCallable implements Callable<Void> {
    private final CountDownLatch startLatch = new CountDownLatch(1);
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    private volatile boolean running = false;

    @Override
    public Void call() throws InterruptedException {
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

  private static final class TestCallable implements AsyncCallable<Void> {

    private final ListenableFuture<Void> future;
    private boolean called = false;

    private TestCallable(ListenableFuture<Void> future) {
      this.future = future;
    }

    @Override
    public ListenableFuture<Void> call() throws Exception {
      called = true;
      return future;
    }
  }
}
