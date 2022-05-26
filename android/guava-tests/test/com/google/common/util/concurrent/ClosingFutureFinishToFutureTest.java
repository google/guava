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
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;

import com.google.common.util.concurrent.ClosingFuture.ClosingCallable;
import com.google.common.util.concurrent.ClosingFuture.DeferredCloser;
import java.io.Closeable;
import java.util.concurrent.ExecutionException;

/** Tests for {@link ClosingFuture} that exercise {@link ClosingFuture#finishToFuture()}. */
public class ClosingFutureFinishToFutureTest extends AbstractClosingFutureTest {
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
