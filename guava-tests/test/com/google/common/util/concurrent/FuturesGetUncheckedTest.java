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
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.FAILED_FUTURE_CHECKED_EXCEPTION;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.FAILED_FUTURE_ERROR;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.FAILED_FUTURE_OTHER_THROWABLE;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.FAILED_FUTURE_UNCHECKED_EXCEPTION;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.FAILED_FUTURE_TIMEOUT_EXCEPTION;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.OTHER_THROWABLE;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.RUNTIME_EXCEPTION;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.RUNTIME_EXCEPTION_FUTURE;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.UNCHECKED_EXCEPTION;
import static com.google.common.util.concurrent.FuturesGetCheckedInputs.TIMEOUT_EXCEPTION;
import static java.util.concurrent.TimeUnit.SECONDS;

import junit.framework.TestCase;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link Futures#getUnchecked(Future)} and
 *   {@link Futures#getUnchecked(Future, long, TimeUnit)}.
 */
public class FuturesGetUncheckedTest extends TestCase {
  public void testGetUncheckedUntimed_success() {
    assertEquals("foo", getUnchecked(immediateFuture("foo")));
  }

  public void testGetUncheckedUntimed_interrupted() {
    Thread.currentThread().interrupt();
    try {
      assertEquals("foo", getUnchecked(immediateFuture("foo")));
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  public void testGetUncheckedUntimed_cancelled() {
    SettableFuture<String> future = SettableFuture.create();
    future.cancel(true);
    try {
      getUnchecked(future);
      fail();
    } catch (CancellationException expected) {
    }
  }

  public void testGetUncheckedUntimed_ExecutionExceptionChecked() {
    try {
      getUnchecked(FAILED_FUTURE_CHECKED_EXCEPTION);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertEquals(CHECKED_EXCEPTION, expected.getCause());
    }
  }

  public void testGetUncheckedUntimed_ExecutionExceptionUnchecked() {
    try {
      getUnchecked(FAILED_FUTURE_UNCHECKED_EXCEPTION);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertEquals(UNCHECKED_EXCEPTION, expected.getCause());
    }
  }

  public void testGetUncheckedUntimed_ExecutionExceptionError() {
    try {
      getUnchecked(FAILED_FUTURE_ERROR);
      fail();
    } catch (ExecutionError expected) {
      assertEquals(ERROR, expected.getCause());
    }
  }

  public void testGetUncheckedUntimed_ExecutionExceptionOtherThrowable() {
    try {
      getUnchecked(FAILED_FUTURE_OTHER_THROWABLE);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertEquals(OTHER_THROWABLE, expected.getCause());
    }
  }

  public void testGetUncheckedUntimed_RuntimeException() {
    try {
      getUnchecked(RUNTIME_EXCEPTION_FUTURE);
      fail();
    } catch (RuntimeException expected) {
      assertEquals(RUNTIME_EXCEPTION, expected);
    }
  }

  public void testGetUncheckedTimed_success() {
    assertEquals("foo", getUnchecked(immediateFuture("foo"), 0, SECONDS));
  }

  public void testGetUncheckedTimed_interrupted() {
    Thread.currentThread().interrupt();
    try {
      assertEquals("foo", getUnchecked(immediateFuture("foo"), 0, SECONDS));
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  public void testGetUncheckedTimed_cancelled() {
    SettableFuture<String> future = SettableFuture.create();
    future.cancel(true);
    try {
      getUnchecked(future, 0, SECONDS);
      fail();
    } catch (CancellationException expected) {
    }
  }

  public void testGetUncheckedTimed_ExecutionExceptionChecked() {
    try {
      getUnchecked(FAILED_FUTURE_CHECKED_EXCEPTION, 0, SECONDS);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertEquals(CHECKED_EXCEPTION, expected.getCause());
    }
  }

  public void testGetUncheckedTimed_ExecutionExceptionUnchecked() {
    try {
      getUnchecked(FAILED_FUTURE_UNCHECKED_EXCEPTION, 0, SECONDS);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertEquals(UNCHECKED_EXCEPTION, expected.getCause());
    }
  }

  public void testGetUncheckedTimed_ExecutionExceptionError() {
    try {
      getUnchecked(FAILED_FUTURE_ERROR, 0, SECONDS);
      fail();
    } catch (ExecutionError expected) {
      assertEquals(ERROR, expected.getCause());
    }
  }

  public void testGetUncheckedTimed_ExecutionExceptionOtherThrowable() {
    try {
      getUnchecked(FAILED_FUTURE_OTHER_THROWABLE, 0, SECONDS);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertEquals(OTHER_THROWABLE, expected.getCause());
    }
  }

  public void testGetUncheckedTimed_RuntimeException() {
    try {
      getUnchecked(RUNTIME_EXCEPTION_FUTURE, 0, SECONDS);
      fail();
    } catch (RuntimeException expected) {
      assertEquals(RUNTIME_EXCEPTION, expected);
    }
  }

  public void testGetUncheckedTimed_TimeoutException() {
    try {
      getUnchecked(FAILED_FUTURE_TIMEOUT_EXCEPTION, 0, SECONDS);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertEquals(TIMEOUT_EXCEPTION, expected.getCause());
    }
  }

}
