/*
 * Copyright (C) 2017 The Guava Authors
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.util.concurrent.ClosingFuture.ValueAndCloser;
import com.google.common.util.concurrent.ClosingFuture.ValueAndCloserConsumer;
import java.io.Closeable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for {@link ClosingFuture} that exercise {@link
 * ClosingFuture#finishToValueAndCloser(ValueAndCloserConsumer, Executor)}.
 */
@NullUnmarked
@GwtIncompatible
@J2ktIncompatible
public class ClosingFutureFinishToValueAndCloserTest extends AbstractClosingFutureTest {
  private final ExecutorService finishToValueAndCloserExecutor = newSingleThreadExecutor();
  private volatile ValueAndCloser<?> valueAndCloser;

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    assertWithMessage("finishToValueAndCloserExecutor was shut down")
        .that(shutdownAndAwaitTermination(finishToValueAndCloserExecutor, 10, SECONDS))
        .isTrue();
  }

  public void testFinishToValueAndCloser_throwsIfCalledTwice() {
    ClosingFuture<Closeable> closingFuture =
        ClosingFuture.submit(closer -> closer.eventuallyClose(mockCloseable, executor), executor);
    closingFuture.finishToValueAndCloser(
        new NoOpValueAndCloserConsumer<>(), finishToValueAndCloserExecutor);
    assertThrows(
        IllegalStateException.class,
        () ->
            closingFuture.finishToValueAndCloser(
                new NoOpValueAndCloserConsumer<>(), finishToValueAndCloserExecutor));
  }

  public void testFinishToValueAndCloser_throwsAfterCallingFinishToFuture() {
    ClosingFuture<Closeable> closingFuture =
        ClosingFuture.submit(closer -> closer.eventuallyClose(mockCloseable, executor), executor);
    FluentFuture<Closeable> unused = closingFuture.finishToFuture();
    assertThrows(
        IllegalStateException.class,
        () ->
            closingFuture.finishToValueAndCloser(
                new NoOpValueAndCloserConsumer<>(), finishToValueAndCloserExecutor));
  }

  @Override
  <T> T getFinalValue(ClosingFuture<T> closingFuture) throws ExecutionException {
    return finishToValueAndCloser(closingFuture).get();
  }

  @Override
  void assertFinallyFailsWithException(ClosingFuture<?> closingFuture) {
    assertThatFutureFailsWithException(closingFuture.statusFuture());
    ValueAndCloser<?> valueAndCloser = finishToValueAndCloser(closingFuture);
    ExecutionException expected = assertThrows(ExecutionException.class, valueAndCloser::get);
    assertThat(expected).hasCauseThat().isEqualTo(exception);
    valueAndCloser.closeAsync();
  }

  @Override
  void assertBecomesCanceled(ClosingFuture<?> closingFuture) {
    assertThatFutureBecomesCancelled(closingFuture.statusFuture());
  }

  @Override
  void waitUntilClosed(ClosingFuture<?> closingFuture) {
    if (valueAndCloser != null) {
      valueAndCloser.closeAsync();
    }
    super.waitUntilClosed(closingFuture);
  }

  @Override
  void cancelFinalStepAndWait(ClosingFuture<TestCloseable> closingFuture) {
    assertThat(closingFuture.cancel(false)).isTrue();
    ValueAndCloser<?> unused = finishToValueAndCloser(closingFuture);
    waitUntilClosed(closingFuture);
    futureCancelled.countDown();
  }

  private <V> ValueAndCloser<V> finishToValueAndCloser(ClosingFuture<V> closingFuture) {
    CountDownLatch valueAndCloserSet = new CountDownLatch(1);
    closingFuture.finishToValueAndCloser(
        valueAndCloser -> {
          ClosingFutureFinishToValueAndCloserTest.this.valueAndCloser = valueAndCloser;
          valueAndCloserSet.countDown();
        },
        finishToValueAndCloserExecutor);
    assertWithMessage("valueAndCloser was set")
        .that(awaitUninterruptibly(valueAndCloserSet, 10, SECONDS))
        .isTrue();
    @SuppressWarnings("unchecked")
    ValueAndCloser<V> valueAndCloserWithType = (ValueAndCloser<V>) valueAndCloser;
    return valueAndCloserWithType;
  }
}
