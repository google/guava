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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.JdkFutureAdapters.listenInPoolThread;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.testing.ClassSanityTester;
import com.google.common.util.concurrent.FuturesTest.ExecutorSpy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Unit tests for {@link JdkFutureAdapters}.
 *
 * @author Sven Mawson
 * @author Kurt Alfred Kluever
 */
public class JdkFutureAdaptersTest extends TestCase {
  private static final String DATA1 = "data";

  public void testListenInPoolThreadReturnsSameFuture() throws Exception {
    ListenableFuture<String> listenableFuture = immediateFuture(DATA1);
    assertSame(listenableFuture, listenInPoolThread(listenableFuture));
  }

  private static class SingleCallListener implements Runnable {

    private boolean expectCall = false;
    private final CountDownLatch calledCountDown = new CountDownLatch(1);

    @Override
    public void run() {
      assertTrue("Listener called before it was expected", expectCall);
      assertFalse("Listener called more than once", wasCalled());
      calledCountDown.countDown();
    }

    public void expectCall() {
      assertFalse("expectCall is already true", expectCall);
      expectCall = true;
    }

    public boolean wasCalled() {
      return calledCountDown.getCount() == 0;
    }

    public void waitForCall() throws InterruptedException {
      assertTrue("expectCall is false", expectCall);
      calledCountDown.await();
    }
  }

  public void testListenInPoolThreadIgnoresExecutorWhenDelegateIsDone() throws Exception {
    NonListenableSettableFuture<String> abstractFuture = NonListenableSettableFuture.create();
    abstractFuture.set(DATA1);
    ExecutorSpy spy = new ExecutorSpy(directExecutor());
    ListenableFuture<String> listenableFuture = listenInPoolThread(abstractFuture, spy);

    SingleCallListener singleCallListener = new SingleCallListener();
    singleCallListener.expectCall();

    assertFalse(spy.wasExecuted);
    assertFalse(singleCallListener.wasCalled());
    assertTrue(listenableFuture.isDone()); // We call AbstractFuture#set above.

    // #addListener() will run the listener immediately because the Future is
    // already finished (we explicitly set the result of it above).
    listenableFuture.addListener(singleCallListener, directExecutor());
    assertEquals(DATA1, listenableFuture.get());

    // 'spy' should have been ignored since 'abstractFuture' was done before
    // a listener was added.
    assertFalse(spy.wasExecuted);
    assertTrue(singleCallListener.wasCalled());
    assertTrue(listenableFuture.isDone());
  }

  public void testListenInPoolThreadUsesGivenExecutor() throws Exception {
    ExecutorService executorService =
        newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).build());
    NonListenableSettableFuture<String> abstractFuture = NonListenableSettableFuture.create();
    ExecutorSpy spy = new ExecutorSpy(executorService);
    ListenableFuture<String> listenableFuture = listenInPoolThread(abstractFuture, spy);

    SingleCallListener singleCallListener = new SingleCallListener();
    singleCallListener.expectCall();

    assertFalse(spy.wasExecuted);
    assertFalse(singleCallListener.wasCalled());
    assertFalse(listenableFuture.isDone());

    listenableFuture.addListener(singleCallListener, executorService);
    abstractFuture.set(DATA1);
    assertEquals(DATA1, listenableFuture.get());
    singleCallListener.waitForCall();

    assertTrue(spy.wasExecuted);
    assertTrue(singleCallListener.wasCalled());
    assertTrue(listenableFuture.isDone());
  }

  public void testListenInPoolThreadCustomExecutorInterrupted() throws Exception {
    final CountDownLatch submitSuccessful = new CountDownLatch(1);
    ExecutorService executorService =
        new ThreadPoolExecutor(
            0,
            Integer.MAX_VALUE,
            60L,
            TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            new ThreadFactoryBuilder().setDaemon(true).build()) {
          @Override
          protected void beforeExecute(Thread t, Runnable r) {
            submitSuccessful.countDown();
          }
        };
    NonListenableSettableFuture<String> abstractFuture = NonListenableSettableFuture.create();
    ListenableFuture<String> listenableFuture = listenInPoolThread(abstractFuture, executorService);

    SingleCallListener singleCallListener = new SingleCallListener();
    singleCallListener.expectCall();

    assertFalse(singleCallListener.wasCalled());
    assertFalse(listenableFuture.isDone());

    listenableFuture.addListener(singleCallListener, directExecutor());
    /*
     * Don't shut down until the listenInPoolThread task has been accepted to
     * run. We want to see what happens when it's interrupted, not when it's
     * rejected.
     */
    submitSuccessful.await();
    executorService.shutdownNow();
    abstractFuture.set(DATA1);
    assertEquals(DATA1, listenableFuture.get());
    singleCallListener.waitForCall();

    assertTrue(singleCallListener.wasCalled());
    assertTrue(listenableFuture.isDone());
  }

  /** A Future that doesn't implement ListenableFuture, useful for testing listenInPoolThread. */
  private static final class NonListenableSettableFuture<V> extends ForwardingFuture<V> {
    static <V> NonListenableSettableFuture<V> create() {
      return new NonListenableSettableFuture<V>();
    }

    final SettableFuture<V> delegate = SettableFuture.create();

    @Override
    protected Future<V> delegate() {
      return delegate;
    }

    void set(V value) {
      delegate.set(value);
    }
  }

  private static final class RuntimeExceptionThrowingFuture<V> implements Future<V> {
    final CountDownLatch allowGetToComplete = new CountDownLatch(1);

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      throw new AssertionFailedError();
    }

    @Override
    public V get() throws InterruptedException {
      /*
       * Wait a little to give us time to call addListener before the future's
       * value is set in addition to the call we'll make after then.
       */
      allowGetToComplete.await(1, SECONDS);
      throw new RuntimeException("expected, should be caught");
    }

    @Override
    public V get(long timeout, TimeUnit unit) {
      throw new AssertionFailedError();
    }

    @Override
    public boolean isCancelled() {
      throw new AssertionFailedError();
    }

    @Override
    public boolean isDone() {
      /*
       * If isDone is true during the call to listenInPoolThread,
       * listenInPoolThread doesn't start a thread. Make sure it's false the
       * first time through (and forever after, since no one else cares about
       * it).
       */
      return false;
    }
  }

  private static final class RecordingRunnable implements Runnable {
    final CountDownLatch wasRun = new CountDownLatch(1);

    // synchronized so that checkState works as expected.
    @Override
    public synchronized void run() {
      checkState(wasRun.getCount() > 0);
      wasRun.countDown();
    }
  }

  @SuppressWarnings("IsInstanceIncompatibleType") // intentional.
  public void testListenInPoolThreadRunsListenerAfterRuntimeException() throws Exception {
    RuntimeExceptionThrowingFuture<String> input = new RuntimeExceptionThrowingFuture<>();
    /*
     * The compiler recognizes that "input instanceof ListenableFuture" is
     * impossible. We want the test, though, in case that changes in the future,
     * so we use isInstance instead.
     */
    assertFalse(
        "Can't test the main listenInPoolThread path "
            + "if the input is already a ListenableFuture",
        ListenableFuture.class.isInstance(input));
    ListenableFuture<String> listenable = listenInPoolThread(input);
    /*
     * This will occur before the waiting get() in the
     * listenInPoolThread-spawned thread completes:
     */
    RecordingRunnable earlyListener = new RecordingRunnable();
    listenable.addListener(earlyListener, directExecutor());

    input.allowGetToComplete.countDown();
    // Now give the get() thread time to finish:
    assertTrue(earlyListener.wasRun.await(1, SECONDS));

    // Now test an additional addListener call, which will be run in-thread:
    RecordingRunnable lateListener = new RecordingRunnable();
    listenable.addListener(lateListener, directExecutor());
    assertTrue(lateListener.wasRun.await(1, SECONDS));
  }

  public void testAdapters_nullChecks() throws Exception {
    new ClassSanityTester()
        .forAllPublicStaticMethods(JdkFutureAdapters.class)
        .thatReturn(Future.class)
        .testNulls();
  }
}
