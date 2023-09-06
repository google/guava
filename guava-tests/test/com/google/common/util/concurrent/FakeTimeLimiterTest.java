/*
 * Copyright (C) 2006 The Guava Authors
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
import static org.junit.Assert.assertThrows;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;

/**
 * Unit test for {@link FakeTimeLimiter}.
 *
 * @author Jens Nyman
 */
public class FakeTimeLimiterTest extends TestCase {

  private static final int DELAY_MS = 50;
  private static final String RETURN_VALUE = "abc";

  private TimeLimiter timeLimiter;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    timeLimiter = new FakeTimeLimiter();
  }

  public void testCallWithTimeout_propagatesReturnValue() throws Exception {
    String result =
        timeLimiter.callWithTimeout(
            Callables.returning(RETURN_VALUE), DELAY_MS, TimeUnit.MILLISECONDS);

    assertThat(result).isEqualTo(RETURN_VALUE);
  }

  public void testCallWithTimeout_wrapsCheckedException() throws Exception {
    Exception exception = new SampleCheckedException();
    ExecutionException e =
        assertThrows(
            ExecutionException.class,
            () ->
                timeLimiter.callWithTimeout(
                    callableThrowing(exception), DELAY_MS, TimeUnit.MILLISECONDS));
    assertThat(e.getCause()).isEqualTo(exception);
  }

  public void testCallWithTimeout_wrapsUncheckedException() throws Exception {
    Exception exception = new RuntimeException("test");
    UncheckedExecutionException e =
        assertThrows(
            UncheckedExecutionException.class,
            () ->
                timeLimiter.callWithTimeout(
                    callableThrowing(exception), DELAY_MS, TimeUnit.MILLISECONDS));
    assertThat(e.getCause()).isEqualTo(exception);
  }

  public void testCallUninterruptiblyWithTimeout_propagatesReturnValue() throws Exception {
    String result =
        timeLimiter.callUninterruptiblyWithTimeout(
            Callables.returning(RETURN_VALUE), DELAY_MS, TimeUnit.MILLISECONDS);

    assertThat(result).isEqualTo(RETURN_VALUE);
  }

  public void testRunWithTimeout_returnsWithoutException() throws Exception {
    timeLimiter.runWithTimeout(Runnables.doNothing(), DELAY_MS, TimeUnit.MILLISECONDS);
  }

  public void testRunWithTimeout_wrapsUncheckedException() throws Exception {
    RuntimeException exception = new RuntimeException("test");
    UncheckedExecutionException e =
        assertThrows(
            UncheckedExecutionException.class,
            () ->
                timeLimiter.runWithTimeout(
                    runnableThrowing(exception), DELAY_MS, TimeUnit.MILLISECONDS));
    assertThat(e.getCause()).isEqualTo(exception);
  }

  public void testRunUninterruptiblyWithTimeout_wrapsUncheckedException() throws Exception {
    RuntimeException exception = new RuntimeException("test");
    UncheckedExecutionException e =
        assertThrows(
            UncheckedExecutionException.class,
            () ->
                timeLimiter.runUninterruptiblyWithTimeout(
                    runnableThrowing(exception), DELAY_MS, TimeUnit.MILLISECONDS));
    assertThat(e.getCause()).isEqualTo(exception);
  }

  public static <T> Callable<T> callableThrowing(final Exception exception) {
    return new Callable<T>() {
      @Override
      public T call() throws Exception {
        throw exception;
      }
    };
  }

  private static Runnable runnableThrowing(final RuntimeException e) {
    return new Runnable() {
      @Override
      public void run() {
        throw e;
      }
    };
  }

  @SuppressWarnings("serial")
  private static class SampleCheckedException extends Exception {}
}
