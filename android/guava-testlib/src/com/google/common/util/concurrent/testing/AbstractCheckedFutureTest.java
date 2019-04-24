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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test case to make sure the {@link CheckedFuture#checkedGet()} and {@link
 * CheckedFuture#checkedGet(long, TimeUnit)} methods work correctly.
 *
 * @author Sven Mawson
 * @since 10.0
 */
@Beta
@GwtIncompatible
public abstract class AbstractCheckedFutureTest extends AbstractListenableFutureTest {

  /** More specific type for the create method. */
  protected abstract <V> CheckedFuture<V, ?> createCheckedFuture(
      V value, Exception except, CountDownLatch waitOn);

  /** Checks that the exception is the correct type of cancellation exception. */
  protected abstract void checkCancelledException(Exception e);

  /** Checks that the exception is the correct type of execution exception. */
  protected abstract void checkExecutionException(Exception e);

  /** Checks that the exception is the correct type of interruption exception. */
  protected abstract void checkInterruptedException(Exception e);

  @Override
  protected <V> ListenableFuture<V> createListenableFuture(
      V value, Exception except, CountDownLatch waitOn) {
    return createCheckedFuture(value, except, waitOn);
  }

  /**
   * Tests that the {@link CheckedFuture#checkedGet()} method throws the correct type of
   * cancellation exception when it is cancelled.
   */
  public void testCheckedGetThrowsApplicationExceptionOnCancellation() {

    final CheckedFuture<Boolean, ?> future = createCheckedFuture(Boolean.TRUE, null, latch);

    assertFalse(future.isDone());
    assertFalse(future.isCancelled());

    new Thread(
            new Runnable() {
              @Override
              public void run() {
                future.cancel(true);
              }
            })
        .start();

    try {
      future.checkedGet();
      fail("RPC Should have been cancelled.");
    } catch (Exception e) {
      checkCancelledException(e);
    }

    assertTrue(future.isDone());
    assertTrue(future.isCancelled());
  }

  public void testCheckedGetThrowsApplicationExceptionOnInterruption() throws InterruptedException {

    final CheckedFuture<Boolean, ?> future = createCheckedFuture(Boolean.TRUE, null, latch);

    final CountDownLatch startingGate = new CountDownLatch(1);
    final CountDownLatch successLatch = new CountDownLatch(1);

    assertFalse(future.isDone());
    assertFalse(future.isCancelled());

    Thread getThread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                startingGate.countDown();

                try {
                  future.checkedGet();
                } catch (Exception e) {
                  checkInterruptedException(e);

                  // This only gets hit if the original call throws an exception and
                  // the check call above passes.
                  successLatch.countDown();
                }
              }
            });
    getThread.start();

    assertTrue(startingGate.await(500, TimeUnit.MILLISECONDS));
    getThread.interrupt();

    assertTrue(successLatch.await(500, TimeUnit.MILLISECONDS));

    assertFalse(future.isDone());
    assertFalse(future.isCancelled());
  }

  public void testCheckedGetThrowsApplicationExceptionOnError() {
    final CheckedFuture<Boolean, ?> future =
        createCheckedFuture(Boolean.TRUE, new Exception("Error"), latch);

    assertFalse(future.isDone());
    assertFalse(future.isCancelled());

    new Thread(
            new Runnable() {
              @Override
              public void run() {
                latch.countDown();
              }
            })
        .start();

    try {
      future.checkedGet();
      fail();
    } catch (Exception e) {
      checkExecutionException(e);
    }

    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
  }
}
