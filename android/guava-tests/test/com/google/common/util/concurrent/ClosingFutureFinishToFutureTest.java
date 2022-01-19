package com.google.common.util.concurrent;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;

import com.google.common.util.concurrent.ClosingFuture.ClosingCallable;
import com.google.common.util.concurrent.ClosingFuture.DeferredCloser;
import java.io.Closeable;
import java.util.concurrent.ExecutionException;

/** Tests for {@link ClosingFuture} that exercise {@link ClosingFuture#finishToFuture()}. */

public class ClosingFutureFinishToFutureTest extends ClosingFutureTest {

  public void testFinishToFuture_throwsIfCalledTwice() throws Exception {
    ClosingFuture<Closeable> closingFuture =
        ClosingFuture.submit(
            new ClosingCallable<Closeable>() {
              @Override
              public Closeable call(DeferredCloser closer) throws Exception {
                return closer.eventuallyClose(mockCloseable, executor);
              }
            },
            executor);
    FluentFuture<Closeable> unused = closingFuture.finishToFuture();
    try {
      FluentFuture<Closeable> unused2 = closingFuture.finishToFuture();
      fail("should have thrown");
    } catch (IllegalStateException expected) {
    }
  }

  public void testFinishToFuture_throwsAfterCallingFinishToValueAndCloser() throws Exception {
    ClosingFuture<Closeable> closingFuture =
        ClosingFuture.submit(
            new ClosingCallable<Closeable>() {
              @Override
              public Closeable call(DeferredCloser closer) throws Exception {
                return closer.eventuallyClose(mockCloseable, executor);
              }
            },
            executor);
    closingFuture.finishToValueAndCloser(new NoOpValueAndCloserConsumer<>(), directExecutor());
    try {
      FluentFuture<Closeable> unused = closingFuture.finishToFuture();
      fail("should have thrown");
    } catch (IllegalStateException expected) {
    }
  }

  public void testFinishToFuture_preventsFurtherDerivation() {
    ClosingFuture<String> closingFuture = ClosingFuture.from(immediateFuture("value1"));
    FluentFuture<String> unused = closingFuture.finishToFuture();
    assertDerivingThrowsIllegalStateException(closingFuture);
  }

  @Override
  <T> T getFinalValue(ClosingFuture<T> closingFuture) throws ExecutionException {
    return getUninterruptibly(closingFuture.finishToFuture());
  }

  @Override
  void assertFinallyFailsWithException(ClosingFuture<?> closingFuture) {
    assertThatFutureFailsWithException(closingFuture.finishToFuture());
  }

  @Override
  void assertBecomesCanceled(ClosingFuture<?> closingFuture) throws ExecutionException {
    assertThatFutureBecomesCancelled(closingFuture.finishToFuture());
  }

  @Override
  void cancelFinalStepAndWait(ClosingFuture<TestCloseable> closingFuture) {
    assertThat(closingFuture.finishToFuture().cancel(false)).isTrue();
    waitUntilClosed(closingFuture);
    futureCancelled.countDown();
  }
}
