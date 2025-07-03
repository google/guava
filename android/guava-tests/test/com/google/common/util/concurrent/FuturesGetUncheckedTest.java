/*
 * Copyright (C) 2008 The Guava Authors
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
import static com.google.common.util.concurrent.Futures.getUnchecked;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.CHECKED_EXCEPTION;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.ERROR;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.ERROR_FUTURE;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.FAILED_FUTURE_CHECKED_EXCEPTION;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.FAILED_FUTURE_ERROR;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.FAILED_FUTURE_OTHER_THROWABLE;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.FAILED_FUTURE_UNCHECKED_EXCEPTION;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.OTHER_THROWABLE;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.RUNTIME_EXCEPTION;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.RUNTIME_EXCEPTION_FUTURE;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.UNCHECKED_EXCEPTION;
import static com.google.common.util.concurrent.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/** Unit tests for {@link Futures#getUnchecked(Future)}. */
@GwtCompatible(emulated = true)
@NullUnmarked
public class FuturesGetUncheckedTest extends TestCase {
  public void testGetUnchecked_success() {
    assertEquals("foo", getUnchecked(immediateFuture("foo")));
  }

  @J2ktIncompatible
  @GwtIncompatible // Thread.interrupt
  public void testGetUnchecked_interrupted() {
    Thread.currentThread().interrupt();
    try {
      assertEquals("foo", getUnchecked(immediateFuture("foo")));
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  public void testGetUnchecked_cancelled() {
    SettableFuture<String> future = SettableFuture.create();
    future.cancel(true);
    assertThrows(CancellationException.class, () -> getUnchecked(future));
  }

  public void testGetUnchecked_executionExceptionChecked() {
    UncheckedExecutionException expected =
        assertThrows(
            UncheckedExecutionException.class, () -> getUnchecked(FAILED_FUTURE_CHECKED_EXCEPTION));
    assertThat(expected).hasCauseThat().isEqualTo(CHECKED_EXCEPTION);
  }

  public void testGetUnchecked_executionExceptionUnchecked() {
    UncheckedExecutionException expected =
        assertThrows(
            UncheckedExecutionException.class,
            () -> getUnchecked(FAILED_FUTURE_UNCHECKED_EXCEPTION));
    assertThat(expected).hasCauseThat().isEqualTo(UNCHECKED_EXCEPTION);
  }

  public void testGetUnchecked_executionExceptionError() {
    ExecutionError expected =
        assertThrows(ExecutionError.class, () -> getUnchecked(FAILED_FUTURE_ERROR));
    assertThat(expected).hasCauseThat().isEqualTo(ERROR);
  }

  public void testGetUnchecked_executionExceptionOtherThrowable() {
    UncheckedExecutionException expected =
        assertThrows(
            UncheckedExecutionException.class, () -> getUnchecked(FAILED_FUTURE_OTHER_THROWABLE));
    assertThat(expected).hasCauseThat().isEqualTo(OTHER_THROWABLE);
  }

  public void testGetUnchecked_runtimeException() {
    RuntimeException expected =
        assertThrows(RuntimeException.class, () -> getUnchecked(RUNTIME_EXCEPTION_FUTURE));
    assertEquals(RUNTIME_EXCEPTION, expected);
  }

  public void testGetUnchecked_error() {
    try {
      getUnchecked(ERROR_FUTURE);
    } catch (Error expected) {
      assertEquals(ERROR, expected);
      return;
    }
    fail();
  }
}
