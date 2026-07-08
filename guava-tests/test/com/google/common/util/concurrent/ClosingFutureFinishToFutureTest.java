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
import static java.util.logging.Level.FINER;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.TestLogHandler;
import java.io.Closeable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.NullUnmarked;

/** Tests for {@link ClosingFuture} that exercise {@link ClosingFuture#finishToFuture()}. */
@NullUnmarked
@GwtIncompatible
@J2ktIncompatible
public class ClosingFutureFinishToFutureTest extends AbstractClosingFutureTest {
  public void testFinishToFuture_throwsIfCalledTwice() {
    ClosingFuture<Closeable> closingFuture =
        ClosingFuture.submit(closer -> closer.eventuallyClose(mockCloseable, executor), executor);
    FluentFuture<Closeable> unused = closingFuture.finishToFuture();
    assertThrows(
        IllegalStateException.class,
        () -> {
          FluentFuture<Closeable> unused2 = closingFuture.finishToFuture();
        });
  }

  public void testFinishToFuture_throwsAfterCallingFinishToValueAndCloser() {
    ClosingFuture<Closeable> closingFuture =
        ClosingFuture.submit(closer -> closer.eventuallyClose(mockCloseable, executor), executor);
    closingFuture.finishToValueAndCloser(new NoOpValueAndCloserConsumer<>(), directExecutor());
    assertThrows(
        IllegalStateException.class,
        () -> {
          FluentFuture<Closeable> unused = closingFuture.finishToFuture();
        });
  }

  public void testFinishToFuture_preventsFurtherDerivation() {
    ClosingFuture<String> closingFuture = ClosingFuture.from(immediateFuture("value1"));
    FluentFuture<String> unused = closingFuture.finishToFuture();
    assertDerivingThrowsIllegalStateException(closingFuture);
  }

  public void testFinishToFuture_doesNotCallToStringOnFutureWhenLoggingDisabled() throws Exception {
    ListenableFuture<String> future =
        new AbstractFuture<String>() {
          {
            set("foo");
          }

          @Override
          public String toString() {
            throw new AssertionError("toString called");
          }
        };
    FluentFuture<String> finished = ClosingFuture.from(future).finishToFuture();
    assertThat(getUninterruptibly(finished)).isEqualTo("foo");
  }

  public void testCancel_doesNotCallToStringOnFutureWhenLoggingDisabled() {
    ListenableFuture<String> future =
        new AbstractFuture<String>() {
          @Override
          public String toString() {
            throw new AssertionError("toString called");
          }
        };
    ClosingFuture.from(future).cancel(true);
  }

  public void testFinishToFuture_logsWhenLoggingEnabled() {
    TestLogHandler logHandler = new TestLogHandler();
    Logger logger = Logger.getLogger(ClosingFuture.class.getName());
    logger.addHandler(logHandler);
    Level oldLevel = logger.getLevel();
    logger.setLevel(FINER);
    try {
      ListenableFuture<String> future =
          new AbstractFuture<String>() {
            @Override
            public String toString() {
              return "mocked future toString";
            }
          };
      FluentFuture<String> unused = ClosingFuture.from(future).finishToFuture();
      assertThat(logHandler.getStoredLogRecords().get(0).getParameters()[0].toString())
          .contains("mocked future toString");
    } finally {
      logger.removeHandler(logHandler);
      logger.setLevel(oldLevel);
    }
  }

  public void testCancel_logsWhenLoggingEnabled() {
    TestLogHandler logHandler = new TestLogHandler();
    Logger logger = Logger.getLogger(ClosingFuture.class.getName());
    logger.addHandler(logHandler);
    Level oldLevel = logger.getLevel();
    logger.setLevel(FINER);
    try {
      ListenableFuture<String> future =
          new AbstractFuture<String>() {
            @Override
            public String toString() {
              return "mocked future toString";
            }
          };
      ClosingFuture.from(future).cancel(true);
      assertThat(logHandler.getStoredLogRecords().get(0).getParameters()[0].toString())
          .contains("mocked future toString");
    } finally {
      logger.removeHandler(logHandler);
      logger.setLevel(oldLevel);
    }
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
  void assertBecomesCanceled(ClosingFuture<?> closingFuture) {
    assertThatFutureBecomesCancelled(closingFuture.finishToFuture());
  }

  @Override
  void cancelFinalStepAndWait(ClosingFuture<TestCloseable> closingFuture) {
    assertThat(closingFuture.finishToFuture().cancel(false)).isTrue();
    waitUntilClosed(closingFuture);
    futureCancelled.countDown();
  }
}
