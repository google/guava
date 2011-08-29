// Copyright 2008 Google Inc. All Rights Reserved.

package com.google.common.util.concurrent.testing;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import junit.framework.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * A simple mock implementation of {@code Runnable} that can be used for
 * testing ListenableFutures.
 *
 * @author nish@google.com (Nishant Thakkar)
 * @since Guava release 10
 */
@Beta
public class MockFutureListener implements Runnable {
  private final CountDownLatch countDownLatch;
  private final ListenableFuture<?> future;

  public MockFutureListener(ListenableFuture<?> future) {
    this.countDownLatch = new CountDownLatch(1);
    this.future = future;

    future.addListener(this, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public void run() {
    countDownLatch.countDown();
  }

  /**
   * Verify that the listener completes in a reasonable amount of time, and
   * Asserts that the future returns the expected data.
   * @throws Throwable if the listener isn't called or if it resulted in a
   *     throwable or if the result doesn't match the expected value.
   */
  public void assertSuccess(Object expectedData) throws Throwable {
    // Verify that the listener executed in a reasonable amount of time.
    Assert.assertTrue(countDownLatch.await(1L, TimeUnit.SECONDS));

    try {
      Assert.assertEquals(expectedData, future.get());
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  /**
   * Verify that the listener completes in a reasonable amount of time, and
   * Asserts that the future throws an {@code ExecutableException} and that the
   * cause of the {@code ExecutableException} is {@code expectedCause}.
   */
  public void assertException(Throwable expectedCause) throws Exception {
    // Verify that the listener executed in a reasonable amount of time.
    Assert.assertTrue(countDownLatch.await(1L, TimeUnit.SECONDS));

    try {
      future.get();
      Assert.fail("This call was supposed to throw an ExecutionException");
    } catch (ExecutionException expected) {
      Assert.assertSame(expectedCause, expected.getCause());
    }
  }

  public void assertTimeout() throws Exception {
    // Verify that the listener does not get called in a reasonable amount of
    // time.
    Assert.assertFalse(countDownLatch.await(1L, TimeUnit.SECONDS));
  }
}
