/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.util.concurrent.testing;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract test case parent for anything implementing {@link ListenableFuture}. Tests the two get
 * methods and the addListener method.
 *
 * @author Sven Mawson
 * @since 10.0
 */
@GwtIncompatible
public abstract class AbstractListenableFutureTest extends TestCase {

  protected CountDownLatch latch;
  protected ListenableFuture<Boolean> future;

  @Override
  protected void setUp() throws Exception {

    // Create a latch and a future that waits on the latch.
    latch = new CountDownLatch(1);
    future = createListenableFuture(Boolean.TRUE, null, latch);
  }

  @Override
  protected void tearDown() throws Exception {

    // Make sure we have no waiting threads.
    latch.countDown();
  }

  /** Constructs a listenable future with a value available after the latch has counted down. */
  protected abstract <V> ListenableFuture<V> createListenableFuture(
      V value, @Nullable Exception except, CountDownLatch waitOn);

  /** Tests that the {@link Future#get()} method blocks until a value is available. */
  public void testGetBlocksUntilValueAvailable() throws Throwable {

    assertFalse(future.isDone());
    assertFalse(future.isCancelled());

    ExecutorService executor = Executors.newSingleThreadExecutor();

    try {
      Future<Boolean> getResult = executor.submit(() -> future.get());

      // Release the future value.
      latch.countDown();

      assertTrue(getResult.get(10, SECONDS));
    } finally {
      executor.shutdownNow();
    }

    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
  }

  /** Tests that the {@link Future#get(long, TimeUnit)} method times out correctly. */
  public void testTimeoutOnGetWorksCorrectly() throws InterruptedException, ExecutionException {

    // The task thread waits for the latch, so we expect a timeout here.
    try {
      future.get(20, MILLISECONDS);
      fail("Should have timed out trying to get the value.");
    } catch (TimeoutException expected) {
    } finally {
      latch.countDown();
    }
  }

  /**
   * Tests that a canceled future throws a cancellation exception.
   *
   * <p>This method checks the cancel, isCancelled, and isDone methods.
   */
  public void testCanceledFutureThrowsCancellation() throws Exception {

    assertFalse(future.isDone());
    assertFalse(future.isCancelled());

    CountDownLatch successLatch = new CountDownLatch(1);

    // Run cancellation in a separate thread as an extra thread-safety test.
    new Thread(
            () -> {
              assertThrows(CancellationException.class, future::get);
              successLatch.countDown();
            })
        .start();

    assertFalse(future.isDone());
    assertFalse(future.isCancelled());

    future.cancel(true);

    assertTrue(future.isDone());
    assertTrue(future.isCancelled());

    assertTrue(successLatch.await(200, MILLISECONDS));

    latch.countDown();
  }

  public void testListenersNotifiedOnError() throws Exception {
    CountDownLatch successLatch = new CountDownLatch(1);
    CountDownLatch listenerLatch = new CountDownLatch(1);

    ExecutorService exec = Executors.newCachedThreadPool();

    future.addListener(listenerLatch::countDown, exec);

    new Thread(
            () -> {
              assertThrows(CancellationException.class, future::get);
              successLatch.countDown();
            })
        .start();

    future.cancel(true);

    assertTrue(future.isCancelled());
    assertTrue(future.isDone());

    assertTrue(successLatch.await(200, MILLISECONDS));
    assertTrue(listenerLatch.await(200, MILLISECONDS));

    latch.countDown();

    exec.shutdown();
    exec.awaitTermination(100, MILLISECONDS);
  }

  /**
   * Tests that all listeners complete, even if they were added before or after the future was
   * finishing. Also acts as a concurrency test to make sure the locking is done correctly when a
   * future is finishing so that no listeners can be lost.
   */
  public void testAllListenersCompleteSuccessfully()
      throws InterruptedException, ExecutionException {

    ExecutorService exec = Executors.newCachedThreadPool();

    int listenerCount = 20;
    CountDownLatch listenerLatch = new CountDownLatch(listenerCount);

    // Test that listeners added both before and after the value is available
    // get called correctly.
    for (int i = 0; i < 20; i++) {

      // Right in the middle start up a thread to close the latch.
      if (i == 10) {
        new Thread(() -> latch.countDown()).start();
      }

      future.addListener(listenerLatch::countDown, exec);
    }

    assertSame(Boolean.TRUE, future.get());
    // Wait for the listener latch to complete.
    listenerLatch.await(500, MILLISECONDS);

    exec.shutdown();
    exec.awaitTermination(500, MILLISECONDS);
  }
}
