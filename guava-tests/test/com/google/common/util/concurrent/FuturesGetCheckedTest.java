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
import static com.google.common.util.concurrent.ClassPathUtil.parseJavaClassPath;
import static com.google.common.util.concurrent.Futures.getChecked;
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
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.testing.GcFinalization;
import com.google.common.util.concurrent.FuturesGetCheckedInputs.ExceptionWithBadConstructor;
import com.google.common.util.concurrent.FuturesGetCheckedInputs.ExceptionWithGoodAndBadConstructor;
import com.google.common.util.concurrent.FuturesGetCheckedInputs.ExceptionWithManyConstructors;
import com.google.common.util.concurrent.FuturesGetCheckedInputs.ExceptionWithPrivateConstructor;
import com.google.common.util.concurrent.FuturesGetCheckedInputs.ExceptionWithSomePrivateConstructors;
import com.google.common.util.concurrent.FuturesGetCheckedInputs.ExceptionWithWrongTypesConstructor;
import com.google.common.util.concurrent.FuturesGetCheckedInputs.ExceptionWithoutThrowableConstructor;
import com.google.common.util.concurrent.FuturesGetCheckedInputs.TwoArgConstructorException;
import com.google.common.util.concurrent.FuturesGetCheckedInputs.TwoArgConstructorRuntimeException;
import java.lang.ref.WeakReference;
import java.net.URLClassLoader;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;

/** Unit tests for {@link Futures#getChecked(Future, Class)}. */
public class FuturesGetCheckedTest extends TestCase {
  // Boring untimed-get tests:

  public void testGetCheckedUntimed_success() throws TwoArgConstructorException {
    assertEquals("foo", getChecked(immediateFuture("foo"), TwoArgConstructorException.class));
  }

  public void testGetCheckedUntimed_interrupted() {
    SettableFuture<String> future = SettableFuture.create();
    Thread.currentThread().interrupt();
    try {
      getChecked(future, TwoArgConstructorException.class);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertThat(expected).hasCauseThat().isInstanceOf(InterruptedException.class);
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  public void testGetCheckedUntimed_cancelled() throws TwoArgConstructorException {
    SettableFuture<String> future = SettableFuture.create();
    future.cancel(true);
    try {
      getChecked(future, TwoArgConstructorException.class);
      fail();
    } catch (CancellationException expected) {
    }
  }

  public void testGetCheckedUntimed_ExecutionExceptionChecked() {
    try {
      getChecked(FAILED_FUTURE_CHECKED_EXCEPTION, TwoArgConstructorException.class);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertThat(expected).hasCauseThat().isEqualTo(CHECKED_EXCEPTION);
    }
  }

  public void testGetCheckedUntimed_ExecutionExceptionUnchecked()
      throws TwoArgConstructorException {
    try {
      getChecked(FAILED_FUTURE_UNCHECKED_EXCEPTION, TwoArgConstructorException.class);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertThat(expected).hasCauseThat().isEqualTo(UNCHECKED_EXCEPTION);
    }
  }

  public void testGetCheckedUntimed_ExecutionExceptionError() throws TwoArgConstructorException {
    try {
      getChecked(FAILED_FUTURE_ERROR, TwoArgConstructorException.class);
      fail();
    } catch (ExecutionError expected) {
      assertThat(expected).hasCauseThat().isEqualTo(ERROR);
    }
  }

  public void testGetCheckedUntimed_ExecutionExceptionOtherThrowable() {
    try {
      getChecked(FAILED_FUTURE_OTHER_THROWABLE, TwoArgConstructorException.class);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertThat(expected).hasCauseThat().isEqualTo(OTHER_THROWABLE);
    }
  }

  public void testGetCheckedUntimed_RuntimeException() throws TwoArgConstructorException {
    try {
      getChecked(RUNTIME_EXCEPTION_FUTURE, TwoArgConstructorException.class);
      fail();
    } catch (RuntimeException expected) {
      assertEquals(RUNTIME_EXCEPTION, expected);
    }
  }

  public void testGetCheckedUntimed_Error() throws TwoArgConstructorException {
    try {
      getChecked(ERROR_FUTURE, TwoArgConstructorException.class);
    } catch (Error expected) {
      assertEquals(ERROR, expected);
      return;
    }
    fail();
  }

  public void testGetCheckedUntimed_badExceptionConstructor_failsEvenForSuccessfulInput()
      throws Exception {
    try {
      getChecked(immediateFuture("x"), ExceptionWithBadConstructor.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetCheckedUntimed_badExceptionConstructor_wrapsOriginalChecked()
      throws Exception {
    try {
      getChecked(FAILED_FUTURE_CHECKED_EXCEPTION, ExceptionWithBadConstructor.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetCheckedUntimed_withGoodAndBadExceptionConstructor() throws Exception {
    try {
      getChecked(FAILED_FUTURE_CHECKED_EXCEPTION, ExceptionWithGoodAndBadConstructor.class);
      fail();
    } catch (ExceptionWithGoodAndBadConstructor expected) {
      assertThat(expected).hasCauseThat().isSameInstanceAs(CHECKED_EXCEPTION);
    }
  }

  // Boring timed-get tests:

  public void testGetCheckedTimed_success() throws TwoArgConstructorException {
    assertEquals(
        "foo", getChecked(immediateFuture("foo"), TwoArgConstructorException.class, 0, SECONDS));
  }

  public void testGetCheckedTimed_interrupted() {
    SettableFuture<String> future = SettableFuture.create();
    Thread.currentThread().interrupt();
    try {
      getChecked(future, TwoArgConstructorException.class, 0, SECONDS);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertThat(expected).hasCauseThat().isInstanceOf(InterruptedException.class);
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  public void testGetCheckedTimed_cancelled() throws TwoArgConstructorException {
    SettableFuture<String> future = SettableFuture.create();
    future.cancel(true);
    try {
      getChecked(future, TwoArgConstructorException.class, 0, SECONDS);
      fail();
    } catch (CancellationException expected) {
    }
  }

  public void testGetCheckedTimed_ExecutionExceptionChecked() {
    try {
      getChecked(FAILED_FUTURE_CHECKED_EXCEPTION, TwoArgConstructorException.class, 0, SECONDS);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertThat(expected).hasCauseThat().isEqualTo(CHECKED_EXCEPTION);
    }
  }

  public void testGetCheckedTimed_ExecutionExceptionUnchecked() throws TwoArgConstructorException {
    try {
      getChecked(FAILED_FUTURE_UNCHECKED_EXCEPTION, TwoArgConstructorException.class, 0, SECONDS);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertThat(expected).hasCauseThat().isEqualTo(UNCHECKED_EXCEPTION);
    }
  }

  public void testGetCheckedTimed_ExecutionExceptionError() throws TwoArgConstructorException {
    try {
      getChecked(FAILED_FUTURE_ERROR, TwoArgConstructorException.class, 0, SECONDS);
      fail();
    } catch (ExecutionError expected) {
      assertThat(expected).hasCauseThat().isEqualTo(ERROR);
    }
  }

  public void testGetCheckedTimed_ExecutionExceptionOtherThrowable() {
    try {
      getChecked(FAILED_FUTURE_OTHER_THROWABLE, TwoArgConstructorException.class, 0, SECONDS);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertThat(expected).hasCauseThat().isEqualTo(OTHER_THROWABLE);
    }
  }

  public void testGetCheckedTimed_RuntimeException() throws TwoArgConstructorException {
    try {
      getChecked(RUNTIME_EXCEPTION_FUTURE, TwoArgConstructorException.class, 0, SECONDS);
      fail();
    } catch (RuntimeException expected) {
      assertEquals(RUNTIME_EXCEPTION, expected);
    }
  }

  public void testGetCheckedTimed_Error() throws TwoArgConstructorException {
    try {
      getChecked(ERROR_FUTURE, TwoArgConstructorException.class, 0, SECONDS);
    } catch (Error expected) {
      assertEquals(ERROR, expected);
      return;
    }
    fail();
  }

  public void testGetCheckedTimed_TimeoutException() {
    SettableFuture<String> future = SettableFuture.create();
    try {
      getChecked(future, TwoArgConstructorException.class, 0, SECONDS);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertThat(expected).hasCauseThat().isInstanceOf(TimeoutException.class);
    }
  }

  public void testGetCheckedTimed_badExceptionConstructor_failsEvenForSuccessfulInput()
      throws Exception {
    try {
      getChecked(immediateFuture("x"), ExceptionWithBadConstructor.class, 1, TimeUnit.SECONDS);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetCheckedTimed_badExceptionConstructor_wrapsOriginalChecked() throws Exception {
    try {
      getChecked(
          FAILED_FUTURE_CHECKED_EXCEPTION, ExceptionWithBadConstructor.class, 1, TimeUnit.SECONDS);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetCheckedTimed_withGoodAndBadExceptionConstructor() {
    try {
      getChecked(
          FAILED_FUTURE_CHECKED_EXCEPTION,
          ExceptionWithGoodAndBadConstructor.class,
          1,
          TimeUnit.SECONDS);
      fail();
    } catch (ExceptionWithGoodAndBadConstructor expected) {
      assertThat(expected).hasCauseThat().isSameInstanceAs(CHECKED_EXCEPTION);
    }
  }

  // Edge case tests of the exception-construction code through untimed get():

  @SuppressWarnings("FuturesGetCheckedIllegalExceptionType")
  public void testGetCheckedUntimed_exceptionClassIsRuntimeException() {
    try {
      getChecked(FAILED_FUTURE_CHECKED_EXCEPTION, TwoArgConstructorRuntimeException.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetCheckedUntimed_exceptionClassSomePrivateConstructors() {
    try {
      getChecked(FAILED_FUTURE_CHECKED_EXCEPTION, ExceptionWithSomePrivateConstructors.class);
      fail();
    } catch (ExceptionWithSomePrivateConstructors expected) {
    }
  }

  @SuppressWarnings("FuturesGetCheckedIllegalExceptionType")
  public void testGetCheckedUntimed_exceptionClassNoPublicConstructor()
      throws ExceptionWithPrivateConstructor {
    try {
      getChecked(FAILED_FUTURE_CHECKED_EXCEPTION, ExceptionWithPrivateConstructor.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @SuppressWarnings("FuturesGetCheckedIllegalExceptionType")
  public void testGetCheckedUntimed_exceptionClassPublicConstructorWrongType()
      throws ExceptionWithWrongTypesConstructor {
    try {
      getChecked(FAILED_FUTURE_CHECKED_EXCEPTION, ExceptionWithWrongTypesConstructor.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetCheckedUntimed_exceptionClassPrefersStringConstructor() {
    try {
      getChecked(FAILED_FUTURE_CHECKED_EXCEPTION, ExceptionWithManyConstructors.class);
      fail();
    } catch (ExceptionWithManyConstructors expected) {
      assertTrue(expected.usedExpectedConstructor);
    }
  }

  public void testGetCheckedUntimed_exceptionClassUsedInitCause() {
    try {
      getChecked(FAILED_FUTURE_CHECKED_EXCEPTION, ExceptionWithoutThrowableConstructor.class);
      fail();
    } catch (ExceptionWithoutThrowableConstructor expected) {
      assertThat(expected).hasMessageThat().contains("mymessage");
      assertThat(expected).hasCauseThat().isEqualTo(CHECKED_EXCEPTION);
    }
  }

  // Class unloading test:

  public static final class WillBeUnloadedException extends Exception {}

  @AndroidIncompatible // "Parent ClassLoader may not be null"; maybe avoidable if we try?
  public void testGetChecked_classUnloading() throws Exception {
    WeakReference<?> classUsedByGetChecked = doTestClassUnloading();
    GcFinalization.awaitClear(classUsedByGetChecked);
  }

  /**
   * Loads {@link WillBeUnloadedException} in a separate {@code ClassLoader}, calls {@code
   * getChecked(future, WillBeUnloadedException.class)}, and returns the loader. The caller can then
   * test that the {@code ClassLoader} can still be GCed. The test amounts to a test that {@code
   * getChecked} holds no strong references to the class.
   */
  private WeakReference<?> doTestClassUnloading() throws Exception {
    URLClassLoader shadowLoader = new URLClassLoader(parseJavaClassPath(), null);
    @SuppressWarnings("unchecked")
    Class<WillBeUnloadedException> shadowClass =
        (Class<WillBeUnloadedException>)
            Class.forName(WillBeUnloadedException.class.getName(), false, shadowLoader);
    assertNotSame(shadowClass, WillBeUnloadedException.class);
    getChecked(immediateFuture("foo"), shadowClass);
    return new WeakReference<>(shadowLoader);
  }

  /*
   * TODO(cpovirk): It would be great to run all these tests (including class unloading) in an
   * environment that forces Futures.getChecked to its fallback WeakSetValidator. One awful way of
   * doing so would be to derive a separate test library by using remove_from_jar to strip out
   * ClassValueValidator.
   *
   * Fortunately, we get pretty good coverage "by accident": We run all these tests against the
   * *backport*, where ClassValueValidator is not present.
   */
}
