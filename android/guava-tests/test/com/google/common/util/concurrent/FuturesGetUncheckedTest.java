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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import junit.framework.TestCase;

/** Unit tests for {@link Futures#getUnchecked(Future)}. */
@GwtCompatible(emulated = true)
public class FuturesGetUncheckedTest extends TestCase {
  public void testGetUnchecked_success() {
    assertEquals("foo", getUnchecked(immediateFuture("foo")));
  }

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
    try {
      getUnchecked(future);
      fail();
    } catch (CancellationException expected) {
    }
  }

  public void testGetUnchecked_ExecutionExceptionChecked() {
    try {
      getUnchecked(FAILED_FUTURE_CHECKED_EXCEPTION);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertEquals(CHECKED_EXCEPTION, expected.getCause());
    }
  }

  public void testGetUnchecked_ExecutionExceptionUnchecked() {
    try {
      getUnchecked(FAILED_FUTURE_UNCHECKED_EXCEPTION);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertEquals(UNCHECKED_EXCEPTION, expected.getCause());
    }
  }

  public void testGetUnchecked_ExecutionExceptionError() {
    try {
      getUnchecked(FAILED_FUTURE_ERROR);
      fail();
    } catch (ExecutionError expected) {
      assertEquals(ERROR, expected.getCause());
    }
  }

  public void testGetUnchecked_ExecutionExceptionOtherThrowable() {
    try {
      getUnchecked(FAILED_FUTURE_OTHER_THROWABLE);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertEquals(OTHER_THROWABLE, expected.getCause());
    }
  }

  public void testGetUnchecked_RuntimeException() {
    try {
      getUnchecked(RUNTIME_EXCEPTION_FUTURE);
      fail();
    } catch (RuntimeException expected) {
      assertEquals(RUNTIME_EXCEPTION, expected);
    }
  }

  public void testGetUnchecked_Error() {
    try {
      getUnchecked(ERROR_FUTURE);
    } catch (Error expected) {
      assertEquals(ERROR, expected);
      return;
    }
    fail();
  }
}
