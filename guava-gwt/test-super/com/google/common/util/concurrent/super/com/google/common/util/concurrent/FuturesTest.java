/*
 * Copyright (C) 2008 The Guava Authors
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
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Functions;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link Futures}.
 *
 * @author Nishant Thakkar
 */
@SuppressWarnings("CheckReturnValue")
@GwtCompatible(emulated = true)
public class FuturesTest extends TestCase {

  private static final String DATA1 = "data";
  private static final String DATA2 = "more data";
  private static final String DATA3 = "most data";

  public void testImmediateFuture() throws Exception {
    ListenableFuture<String> future = Futures.immediateFuture(DATA1);

    // Verify that the proper object is returned without waiting
    assertSame(DATA1, future.get(0L, TimeUnit.MILLISECONDS));
  }

  public void testMultipleImmediateFutures() throws Exception {
    ListenableFuture<String> future1 = Futures.immediateFuture(DATA1);
    ListenableFuture<String> future2 = Futures.immediateFuture(DATA2);

    // Verify that the proper objects are returned without waiting
    assertSame(DATA1, future1.get(0L, TimeUnit.MILLISECONDS));
    assertSame(DATA2, future2.get(0L, TimeUnit.MILLISECONDS));
  }

  private static class MyException extends Exception {

  }

  // Class hierarchy for generics sanity checks
  private static class Foo {

  }

  private static class FooChild extends Foo {

  }

  private static class Bar {

  }

  private static class BarChild extends Bar {

  }

  public void testTransform_genericsNull() throws Exception {
    ListenableFuture<?> nullFuture = Futures.immediateFuture(null);
    ListenableFuture<?> transformedFuture =
        Futures.transform(nullFuture, Functions.constant(null));
    assertNull(transformedFuture.get());
  }

  public void testTransform_genericsHierarchy() throws Exception {
    ListenableFuture<FooChild> future = Futures.immediateFuture(null);
    final BarChild barChild = new BarChild();
    Function<Foo, BarChild> function = new Function<Foo, BarChild>() {
      @Override
      public BarChild apply(Foo unused) {
        return barChild;
      }
    };
    Bar bar = Futures.transform(future, function).get();
    assertSame(barChild, bar);
  }

  public void testTransform_cancelPropagatesToInput() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    AsyncFunction<Foo, Bar> function = new AsyncFunction<Foo, Bar>() {
      @Override
      public ListenableFuture<Bar> apply(Foo unused) {
        throw new AssertionFailedError("Unexpeted call to apply.");
      }
    };
    assertTrue(Futures.transform(input, function).cancel(false));
    assertTrue(input.isCancelled());
    assertFalse(input.wasInterrupted());
  }

  public void testTransform_interruptPropagatesToInput() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    AsyncFunction<Foo, Bar> function = new AsyncFunction<Foo, Bar>() {
      @Override
      public ListenableFuture<Bar> apply(Foo unused) {
        throw new AssertionFailedError("Unexpeted call to apply.");
      }
    };
    assertTrue(Futures.transform(input, function).cancel(true));
    assertTrue(input.isCancelled());
    assertTrue(input.wasInterrupted());
  }

  public void testTransform_cancelPropagatesToAsyncOutput() throws Exception {
    ListenableFuture<Foo> immediate = Futures.immediateFuture(new Foo());
    final SettableFuture<Bar> secondary = SettableFuture.create();
    AsyncFunction<Foo, Bar> function = new AsyncFunction<Foo, Bar>() {
      @Override
      public ListenableFuture<Bar> apply(Foo unused) {
        return secondary;
      }
    };
    assertTrue(Futures.transform(immediate, function).cancel(false));
    assertTrue(secondary.isCancelled());
    assertFalse(secondary.wasInterrupted());
  }

  public void testTransform_interruptPropagatesToAsyncOutput()
      throws Exception {
    ListenableFuture<Foo> immediate = Futures.immediateFuture(new Foo());
    final SettableFuture<Bar> secondary = SettableFuture.create();
    AsyncFunction<Foo, Bar> function = new AsyncFunction<Foo, Bar>() {
      @Override
      public ListenableFuture<Bar> apply(Foo unused) {
        return secondary;
      }
    };
    assertTrue(Futures.transform(immediate, function).cancel(true));
    assertTrue(secondary.isCancelled());
    assertTrue(secondary.wasInterrupted());
  }

  public void testTransformAsync_cancelPropagatesToInput() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    AsyncFunction<Foo, Bar> function = new AsyncFunction<Foo, Bar>() {
      @Override
      public ListenableFuture<Bar> apply(Foo unused) {
        throw new AssertionFailedError("Unexpeted call to apply.");
      }
    };
    assertTrue(Futures.transformAsync(input, function).cancel(false));
    assertTrue(input.isCancelled());
    assertFalse(input.wasInterrupted());
  }

  public void testTransformAsync_interruptPropagatesToInput() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    AsyncFunction<Foo, Bar> function = new AsyncFunction<Foo, Bar>() {
      @Override
      public ListenableFuture<Bar> apply(Foo unused) {
        throw new AssertionFailedError("Unexpeted call to apply.");
      }
    };
    assertTrue(Futures.transformAsync(input, function).cancel(true));
    assertTrue(input.isCancelled());
    assertTrue(input.wasInterrupted());
  }

  public void testTransformAsync_cancelPropagatesToAsyncOutput() throws Exception {
    ListenableFuture<Foo> immediate = Futures.immediateFuture(new Foo());
    final SettableFuture<Bar> secondary = SettableFuture.create();
    AsyncFunction<Foo, Bar> function = new AsyncFunction<Foo, Bar>() {
      @Override
      public ListenableFuture<Bar> apply(Foo unused) {
        return secondary;
      }
    };
    assertTrue(Futures.transformAsync(immediate, function).cancel(false));
    assertTrue(secondary.isCancelled());
    assertFalse(secondary.wasInterrupted());
  }

  public void testTransformAsync_interruptPropagatesToAsyncOutput()
      throws Exception {
    ListenableFuture<Foo> immediate = Futures.immediateFuture(new Foo());
    final SettableFuture<Bar> secondary = SettableFuture.create();
    AsyncFunction<Foo, Bar> function = new AsyncFunction<Foo, Bar>() {
      @Override
      public ListenableFuture<Bar> apply(Foo unused) {
        return secondary;
      }
    };
    assertTrue(Futures.transformAsync(immediate, function).cancel(true));
    assertTrue(secondary.isCancelled());
    assertTrue(secondary.wasInterrupted());
  }

  /**
   * Tests that the function is invoked only once, even if it throws an exception.
   */
  public void testTransformValueRemainsMemoized() throws Exception {
    class Holder {

      int value = 2;
    }
    final Holder holder = new Holder();

    // This function adds the holder's value to the input value.
    Function<Integer, Integer> adder = new Function<Integer, Integer>() {
      @Override
      public Integer apply(Integer from) {
        return from + holder.value;
      }
    };

    // Since holder.value is 2, applying 4 should yield 6.
    assertEquals(6, adder.apply(4).intValue());

    ListenableFuture<Integer> immediateFuture = Futures.immediateFuture(4);
    Future<Integer> transformedFuture = Futures.transform(immediateFuture, adder);

    // The composed future also yields 6.
    assertEquals(6, transformedFuture.get().intValue());

    // Repeated calls yield the same value even though the function's behavior
    // changes
    holder.value = 3;
    assertEquals(6, transformedFuture.get().intValue());
    assertEquals(7, adder.apply(4).intValue());

    // Once more, with feeling.
    holder.value = 4;
    assertEquals(6, transformedFuture.get().intValue());
    assertEquals(8, adder.apply(4).intValue());

    // Memoized get also retains the value.
    assertEquals(6, transformedFuture.get(1000, TimeUnit.SECONDS).intValue());

    // Unsurprisingly, recomposing the future will return an updated value.
    assertEquals(8, Futures.transform(immediateFuture, adder).get().intValue());

    // Repeating, with the timeout version
    assertEquals(8, Futures.transform(immediateFuture, adder).get(
        1000, TimeUnit.SECONDS).intValue());
  }

  static class MyError extends Error {

  }

  static class MyRuntimeException extends RuntimeException {

  }

  // TODO(cpovirk): top-level class?
  static class ExecutorSpy implements Executor {

    Executor delegate;
    boolean wasExecuted;

    public ExecutorSpy(Executor delegate) {
      this.delegate = delegate;
    }

    @Override
    public void execute(Runnable command) {
      delegate.execute(command);
      wasExecuted = true;
    }
  }

  public void testTransform_Executor() throws Exception {
    Object value = new Object();
    ExecutorSpy spy = new ExecutorSpy(directExecutor());

    assertFalse(spy.wasExecuted);

    ListenableFuture<Object> future = Futures.transform(
        Futures.immediateFuture(value),
        Functions.identity(), spy);

    assertSame(value, future.get());
    assertTrue(spy.wasExecuted);
  }

  private static class FunctionSpy<I, O> implements Function<I, O> {
    private int applyCount;
    private final Function<I, O> delegate;

    public FunctionSpy(Function<I, O> delegate) {
      this.delegate = delegate;
    }

    @Override
    public O apply(I input) {
      applyCount++;
      return delegate.apply(input);
    }

    void verifyCallCount(int expected) {
      assertThat(applyCount).isEqualTo(expected);
    }
  }

  private static <I, O> FunctionSpy<I, O> spy(Function<I, O> delegate) {
    return new FunctionSpy<I, O>(delegate);
  }

  private static <X extends Throwable, V> Function<X, V> unexpectedFunction() {
    return new Function<X, V>() {
      @Override
      public V apply(X t) {
        throw newAssertionError("Unexpected fallback", t);
      }
    };
  }

  private static class FutureFallbackSpy<V> implements FutureFallback<V> {

    private int count;
    private final FutureFallback<V> delegate;

    public FutureFallbackSpy(FutureFallback<V> delegate) {
      this.delegate = delegate;
    }

    @Override
    public final ListenableFuture<V> create(Throwable t) throws Exception {
      count++;
      return delegate.create(t);
    }

    void verifyCallCount(int expected) {
      assertThat(count).isEqualTo(expected);
    }
  }

  private static <V> FutureFallbackSpy<V> spy(FutureFallback<V> delegate) {
    return new FutureFallbackSpy<V>(delegate);
  }

  private static <V> FutureFallback<V> unexpectedFallback() {
    return new FutureFallback<V>() {
      @Override
      public ListenableFuture<V> create(Throwable t) {
        throw newAssertionError("Unexpected fallback", t);
      }
    };
  }

  private static class AsyncFunctionSpy<X extends Throwable, V> implements AsyncFunction<X, V> {
    private int count;
    private final AsyncFunction<X, V> delegate;

    public AsyncFunctionSpy(AsyncFunction<X, V> delegate) {
      this.delegate = delegate;
    }

    @Override
    public final ListenableFuture<V> apply(X t) throws Exception {
      count++;
      return delegate.apply(t);
    }

    void verifyCallCount(int expected) {
      assertThat(count).isEqualTo(expected);
    }
  }

  private static <X extends Throwable, V> AsyncFunctionSpy<X, V> spy(AsyncFunction<X, V> delegate) {
    return new AsyncFunctionSpy<X, V>(delegate);
  }

  private static <X extends Throwable, V> AsyncFunction<X, V> unexpectedAsyncFunction() {
    return new AsyncFunction<X, V>() {
      @Override
      public ListenableFuture<V> apply(X t) {
        throw newAssertionError("Unexpected fallback", t);
      }
    };
  }

  /** Alternative to AssertionError(String, Throwable), which doesn't exist in GWT 2.6.1. */
  private static AssertionError newAssertionError(String message, Throwable cause) {
    AssertionError e = new AssertionError(message);
    e.initCause(cause);
    return e;
  }

  public void testWithFallback_inputDoesNotRaiseException() throws Exception {
    FutureFallback<Integer> fallback = unexpectedFallback();
    ListenableFuture<Integer> originalFuture = Futures.immediateFuture(7);
    ListenableFuture<Integer> faultToleranteFuture = Futures.withFallback(originalFuture, fallback);
    assertEquals(7, faultToleranteFuture.get().intValue());
  }

  public void testWithFallback_inputRaisesException() throws Exception {
    final RuntimeException raisedException = new RuntimeException();
    FutureFallbackSpy<Integer> fallback = spy(new FutureFallback<Integer>() {
      @Override
      public ListenableFuture<Integer> create(Throwable t) throws Exception {
        assertThat(t).isSameAs(raisedException);
        return Futures.immediateFuture(20);
      }
    });
    ListenableFuture<Integer> failingFuture = Futures.immediateFailedFuture(raisedException);
    ListenableFuture<Integer> faultTolerantFuture = Futures.withFallback(failingFuture, fallback);
    assertEquals(20, faultTolerantFuture.get().intValue());
    fallback.verifyCallCount(1);
  }

  public void testWithFallback_fallbackGeneratesRuntimeException() throws Exception {
    RuntimeException expectedException = new RuntimeException();
    runExpectedExceptionFallbackTest(expectedException, false);
  }

  public void testWithFallback_fallbackGeneratesCheckedException() throws Exception {
    Exception expectedException = new Exception() {
    };
    runExpectedExceptionFallbackTest(expectedException, false);
  }

  public void testWithFallback_fallbackGeneratesError() throws Exception {
    final Error error = new Error("deliberate");
    FutureFallback<Integer> fallback = new FutureFallback<Integer>() {
      @Override
      public ListenableFuture<Integer> create(Throwable t) throws Exception {
        throw error;
      }
    };
    ListenableFuture<Integer> failingFuture = Futures.immediateFailedFuture(new RuntimeException());
    try {
      Futures.withFallback(failingFuture, fallback).get();
      fail("An Exception should have been thrown!");
    } catch (ExecutionException expected) {
      assertSame(error, expected.getCause());
    }
  }

  public void testWithFallback_fallbackReturnsRuntimeException() throws Exception {
    RuntimeException expectedException = new RuntimeException();
    runExpectedExceptionFallbackTest(expectedException, true);
  }

  public void testWithFallback_fallbackReturnsCheckedException() throws Exception {
    Exception expectedException = new Exception() {
    };
    runExpectedExceptionFallbackTest(expectedException, true);
  }

  private void runExpectedExceptionFallbackTest(
      final Exception expectedException, final boolean wrapInFuture) throws Exception {
    FutureFallbackSpy<Integer> fallback = spy(new FutureFallback<Integer>() {
      @Override
      public ListenableFuture<Integer> create(Throwable t) throws Exception {
        if (!wrapInFuture) {
          throw expectedException;
        } else {
          return Futures.immediateFailedFuture(expectedException);
        }
      }
    });

    ListenableFuture<Integer> failingFuture = Futures.immediateFailedFuture(new RuntimeException());

    ListenableFuture<Integer> faultToleranteFuture = Futures.withFallback(failingFuture, fallback);
    try {
      faultToleranteFuture.get();
      fail("An Exception should have been thrown!");
    } catch (ExecutionException ee) {
      assertSame(expectedException, ee.getCause());
    }
    fallback.verifyCallCount(1);
  }

  public void testWithFallback_fallbackNotReady() throws Exception {
    ListenableFuture<Integer> primary = immediateFailedFuture(new Exception());
    final SettableFuture<Integer> secondary = SettableFuture.create();
    FutureFallback<Integer> fallback = new FutureFallback<Integer>() {
      @Override
      public ListenableFuture<Integer> create(Throwable t) {
        return secondary;
      }
    };
    ListenableFuture<Integer> derived = Futures.withFallback(primary, fallback);
    secondary.set(1);
    assertEquals(1, (int) derived.get());
  }

  public void testWithFallback_resultInterruptedBeforeFallback() throws Exception {
    SettableFuture<Integer> primary = SettableFuture.create();
    FutureFallback<Integer> fallback = unexpectedFallback();
    ListenableFuture<Integer> derived = Futures.withFallback(primary, fallback);
    derived.cancel(true);
    assertTrue(primary.isCancelled());
    assertTrue(primary.wasInterrupted());
  }

  public void testWithFallback_resultCancelledBeforeFallback() throws Exception {
    SettableFuture<Integer> primary = SettableFuture.create();
    FutureFallback<Integer> fallback = unexpectedFallback();
    ListenableFuture<Integer> derived = Futures.withFallback(primary, fallback);
    derived.cancel(false);
    assertTrue(primary.isCancelled());
    assertFalse(primary.wasInterrupted());
  }

  public void testWithFallback_nullInsteadOfFuture() throws Exception {
    ListenableFuture<Integer> inputFuture = immediateFailedFuture(new Exception());
    ListenableFuture<?> chainedFuture =
        Futures.withFallback(inputFuture, new FutureFallback<Integer>() {
          @Override
          public ListenableFuture<Integer> create(Throwable t) {
            return null;
          }
        });
    try {
      chainedFuture.get();
      fail();
    } catch (ExecutionException expected) {
      NullPointerException cause = (NullPointerException) expected.getCause();
      assertThat(cause).hasMessage("FutureFallback.create returned null instead of a Future. "
          + "Did you mean to return immediateFuture(null)?");
    }
  }

  // catchingAsync tests cloned from the old withFallback tests:

  public void testCatchingAsync_inputDoesNotRaiseException() throws Exception {
    AsyncFunction<Throwable, Integer> fallback = unexpectedAsyncFunction();
    ListenableFuture<Integer> originalFuture = Futures.immediateFuture(7);
    ListenableFuture<Integer> faultToleranteFuture =
        Futures.catchingAsync(originalFuture, Throwable.class, fallback);
    assertEquals(7, faultToleranteFuture.get().intValue());
  }

  public void testCatchingAsync_inputRaisesException() throws Exception {
    final RuntimeException raisedException = new RuntimeException();
    AsyncFunctionSpy<Throwable, Integer> fallback = spy(new AsyncFunction<Throwable, Integer>() {
      @Override
      public ListenableFuture<Integer> apply(Throwable t) throws Exception {
        assertThat(t).isSameAs(raisedException);
        return Futures.immediateFuture(20);
      }
    });
    ListenableFuture<Integer> failingFuture = Futures.immediateFailedFuture(raisedException);
    ListenableFuture<Integer> faultTolerantFuture =
        Futures.catchingAsync(failingFuture, Throwable.class, fallback);
    assertEquals(20, faultTolerantFuture.get().intValue());
    fallback.verifyCallCount(1);
  }

  public void testCatchingAsync_fallbackGeneratesRuntimeException() throws Exception {
    RuntimeException expectedException = new RuntimeException();
    runExpectedExceptionCatchingAsyncTest(expectedException, false);
  }

  public void testCatchingAsync_fallbackGeneratesCheckedException() throws Exception {
    Exception expectedException = new Exception() {
    };
    runExpectedExceptionCatchingAsyncTest(expectedException, false);
  }

  public void testCatchingAsync_fallbackGeneratesError() throws Exception {
    final Error error = new Error("deliberate");
    AsyncFunction<Throwable, Integer> fallback = new AsyncFunction<Throwable, Integer>() {
      @Override
      public ListenableFuture<Integer> apply(Throwable t) throws Exception {
        throw error;
      }
    };
    ListenableFuture<Integer> failingFuture = Futures.immediateFailedFuture(new RuntimeException());
    try {
      Futures.catchingAsync(failingFuture, Throwable.class, fallback).get();
      fail("An Exception should have been thrown!");
    } catch (ExecutionException expected) {
      assertSame(error, expected.getCause());
    }
  }

  public void testCatchingAsync_fallbackReturnsRuntimeException() throws Exception {
    RuntimeException expectedException = new RuntimeException();
    runExpectedExceptionCatchingAsyncTest(expectedException, true);
  }

  public void testCatchingAsync_fallbackReturnsCheckedException() throws Exception {
    Exception expectedException = new Exception() {
    };
    runExpectedExceptionCatchingAsyncTest(expectedException, true);
  }

  private void runExpectedExceptionCatchingAsyncTest(
      final Exception expectedException, final boolean wrapInFuture) throws Exception {
    AsyncFunctionSpy<Throwable, Integer> fallback = spy(new AsyncFunction<Throwable, Integer>() {
      @Override
      public ListenableFuture<Integer> apply(Throwable t) throws Exception {
        if (!wrapInFuture) {
          throw expectedException;
        } else {
          return Futures.immediateFailedFuture(expectedException);
        }
      }
    });

    ListenableFuture<Integer> failingFuture = Futures.immediateFailedFuture(new RuntimeException());

    ListenableFuture<Integer> faultToleranteFuture =
        Futures.catchingAsync(failingFuture, Throwable.class, fallback);
    try {
      faultToleranteFuture.get();
      fail("An Exception should have been thrown!");
    } catch (ExecutionException ee) {
      assertSame(expectedException, ee.getCause());
    }
    fallback.verifyCallCount(1);
  }

  public void testCatchingAsync_fallbackNotReady() throws Exception {
    ListenableFuture<Integer> primary = immediateFailedFuture(new Exception());
    final SettableFuture<Integer> secondary = SettableFuture.create();
    AsyncFunction<Throwable, Integer> fallback = new AsyncFunction<Throwable, Integer>() {
      @Override
      public ListenableFuture<Integer> apply(Throwable t) {
        return secondary;
      }
    };
    ListenableFuture<Integer> derived = Futures.catchingAsync(primary, Throwable.class, fallback);
    secondary.set(1);
    assertEquals(1, (int) derived.get());
  }

  public void testCatchingAsync_resultInterruptedBeforeFallback() throws Exception {
    SettableFuture<Integer> primary = SettableFuture.create();
    AsyncFunction<Throwable, Integer> fallback = unexpectedAsyncFunction();
    ListenableFuture<Integer> derived = Futures.catchingAsync(primary, Throwable.class, fallback);
    derived.cancel(true);
    assertTrue(primary.isCancelled());
    assertTrue(primary.wasInterrupted());
  }

  public void testCatchingAsync_resultCancelledBeforeFallback() throws Exception {
    SettableFuture<Integer> primary = SettableFuture.create();
    AsyncFunction<Throwable, Integer> fallback = unexpectedAsyncFunction();
    ListenableFuture<Integer> derived = Futures.catchingAsync(primary, Throwable.class, fallback);
    derived.cancel(false);
    assertTrue(primary.isCancelled());
    assertFalse(primary.wasInterrupted());
  }

  public void testCatchingAsync_nullInsteadOfFuture() throws Exception {
    ListenableFuture<Integer> inputFuture = immediateFailedFuture(new Exception());
    ListenableFuture<?> chainedFuture = Futures.catchingAsync(inputFuture, Throwable.class,
        new AsyncFunction<Throwable, Integer>() {
          @Override
          @SuppressWarnings("AsyncFunctionReturnsNull")
          public ListenableFuture<Integer> apply(Throwable t) {
            return null;
          }
        });
    try {
      chainedFuture.get();
      fail();
    } catch (ExecutionException expected) {
      NullPointerException cause = (NullPointerException) expected.getCause();
      assertThat(cause).hasMessage("AsyncFunction.apply returned null instead of a Future. "
          + "Did you mean to return immediateFuture(null)?");
    }
  }

  // catching tests cloned from the old withFallback tests:

  public void testCatching_inputDoesNotRaiseException() throws Exception {
    Function<Throwable, Integer> fallback = unexpectedFunction();
    ListenableFuture<Integer> originalFuture = Futures.immediateFuture(7);
    ListenableFuture<Integer> faultToleranteFuture =
        Futures.catching(originalFuture, Throwable.class, fallback);
    assertEquals(7, faultToleranteFuture.get().intValue());
  }

  public void testCatching_inputRaisesException() throws Exception {
    final RuntimeException raisedException = new RuntimeException();
    FunctionSpy<Throwable, Integer> fallback = spy(new Function<Throwable, Integer>() {
      @Override
      public Integer apply(Throwable t) {
        assertThat(t).isSameAs(raisedException);
        return 20;
      }
    });
    ListenableFuture<Integer> failingFuture = Futures.immediateFailedFuture(raisedException);
    ListenableFuture<Integer> faultTolerantFuture =
        Futures.catching(failingFuture, Throwable.class, fallback);
    assertEquals(20, faultTolerantFuture.get().intValue());
    fallback.verifyCallCount(1);
  }

  public void testCatching_fallbackGeneratesRuntimeException() throws Exception {
    RuntimeException expectedException = new RuntimeException();
    runExpectedExceptionCatchingTest(expectedException);
  }

  /*
   * catching() uses a plain Function, so there's no
   * testCatching_fallbackGeneratesCheckedException().
   */

  public void testCatching_fallbackGeneratesError() throws Exception {
    final Error error = new Error("deliberate");
    Function<Throwable, Integer> fallback = new Function<Throwable, Integer>() {
      @Override
      public Integer apply(Throwable t) {
        throw error;
      }
    };
    ListenableFuture<Integer> failingFuture = Futures.immediateFailedFuture(new RuntimeException());
    try {
      Futures.catching(failingFuture, Throwable.class, fallback).get();
      fail("An Exception should have been thrown!");
    } catch (ExecutionException expected) {
      assertSame(error, expected.getCause());
    }
  }

  /*
   * catching() uses a plain Function, so there's no testCatching_fallbackReturnsRuntimeException()
   * or testCatching_fallbackReturnsCheckedException().
   */

  private void runExpectedExceptionCatchingTest(final RuntimeException expectedException)
      throws Exception {
    FunctionSpy<Throwable, Integer> fallback = spy(new Function<Throwable, Integer>() {
      @Override
      public Integer apply(Throwable t) {
        throw expectedException;
      }
    });

    ListenableFuture<Integer> failingFuture = Futures.immediateFailedFuture(new RuntimeException());

    ListenableFuture<Integer> faultToleranteFuture =
        Futures.catching(failingFuture, Throwable.class, fallback);
    try {
      faultToleranteFuture.get();
      fail("An Exception should have been thrown!");
    } catch (ExecutionException ee) {
      assertSame(expectedException, ee.getCause());
    }
    fallback.verifyCallCount(1);
  }

  // catching() uses a plain Function, so there's no testCatching_fallbackNotReady().

  public void testCatching_resultInterruptedBeforeFallback() throws Exception {
    SettableFuture<Integer> primary = SettableFuture.create();
    Function<Throwable, Integer> fallback = unexpectedFunction();
    ListenableFuture<Integer> derived = Futures.catching(primary, Throwable.class, fallback);
    derived.cancel(true);
    assertTrue(primary.isCancelled());
    assertTrue(primary.wasInterrupted());
  }

  public void testCatching_resultCancelledBeforeFallback() throws Exception {
    SettableFuture<Integer> primary = SettableFuture.create();
    Function<Throwable, Integer> fallback = unexpectedFunction();
    ListenableFuture<Integer> derived = Futures.catching(primary, Throwable.class, fallback);
    derived.cancel(false);
    assertTrue(primary.isCancelled());
    assertFalse(primary.wasInterrupted());
  }

  // catching() uses a plain Function, so there's no testCatching_resultCancelledAfterFallback().

  // catching() uses a plain Function, so there's no testCatching_nullInsteadOfFuture().

  // Some tests of the exceptionType parameter:

  public void testCatching_Throwable() throws Exception {
    Function<Throwable, Integer> fallback = functionReturningOne();
    ListenableFuture<Integer> originalFuture = immediateFailedFuture(new IOException());
    ListenableFuture<Integer> faultTolerantFuture =
        Futures.catching(originalFuture, Throwable.class, fallback);
    assertEquals(1, (int) faultTolerantFuture.get());
  }

  public void testCatchingAsync_Throwable() throws Exception {
    AsyncFunction<Throwable, Integer> fallback = asyncFunctionReturningOne();
    ListenableFuture<Integer> originalFuture = immediateFailedFuture(new IOException());
    ListenableFuture<Integer> faultTolerantFuture =
        Futures.catchingAsync(originalFuture, Throwable.class, fallback);
    assertEquals(1, (int) faultTolerantFuture.get());
  }

  private <X extends Throwable> Function<X, Integer> functionReturningOne() {
    return new Function<X, Integer>() {
      @Override
      public Integer apply(X t) {
        return 1;
      }
    };
  }

  private <X extends Throwable> AsyncFunction<X, Integer> asyncFunctionReturningOne() {
    return new AsyncFunction<X, Integer>() {
      @Override
      public ListenableFuture<Integer> apply(X t) {
        return immediateFuture(1);
      }
    };
  }

  private static <I, O> AsyncFunction<I, O> constantAsyncFunction(
      final ListenableFuture<O> output) {
    return new AsyncFunction<I, O>() {
      @Override
      public ListenableFuture<O> apply(I input) {
        return output;
      }
    };
  }

  public void testTransform_genericsWildcard_AsyncFunction() throws Exception {
    ListenableFuture<?> nullFuture = immediateFuture(null);
    ListenableFuture<?> chainedFuture =
        Futures.transform(nullFuture, constantAsyncFunction(nullFuture));
    assertNull(chainedFuture.get());
  }

  public void testTransform_genericsHierarchy_AsyncFunction() throws Exception {
    ListenableFuture<FooChild> future = Futures.immediateFuture(null);
    final BarChild barChild = new BarChild();
    AsyncFunction<Foo, BarChild> function =
        new AsyncFunction<Foo, BarChild>() {
          @Override
          public AbstractFuture<BarChild> apply(Foo unused) {
            AbstractFuture<BarChild> future = new AbstractFuture<BarChild>() {
            };
            future.set(barChild);
            return future;
          }
        };
    Bar bar = Futures.transform(future, function).get();
    assertSame(barChild, bar);
  }

  public void testTransform_asyncFunction_error() throws InterruptedException {
    final Error error = new Error("deliberate");
    AsyncFunction<String, Integer> function = new AsyncFunction<String, Integer>() {
      @Override
      public ListenableFuture<Integer> apply(String input) {
        throw error;
      }
    };
    SettableFuture<String> inputFuture = SettableFuture.create();
    ListenableFuture<Integer> outputFuture = Futures.transform(inputFuture, function);
    inputFuture.set("value");
    try {
      outputFuture.get();
      fail("should have thrown error");
    } catch (ExecutionException e) {
      assertSame(error, e.getCause());
    }
  }

  public void testTransform_asyncFunction_nullInsteadOfFuture() throws Exception {
    ListenableFuture<?> inputFuture = immediateFuture("a");
    ListenableFuture<?> chainedFuture =
        Futures.transform(inputFuture, constantAsyncFunction(null));
    try {
      chainedFuture.get();
      fail();
    } catch (ExecutionException expected) {
      NullPointerException cause = (NullPointerException) expected.getCause();
      assertThat(cause).hasMessage("AsyncFunction.apply returned null instead of a Future. "
          + "Did you mean to return immediateFuture(null)?");
    }
  }

  public void testTransformAsync_genericsWildcard_AsyncFunction() throws Exception {
    ListenableFuture<?> nullFuture = immediateFuture(null);
    ListenableFuture<?> chainedFuture =
        Futures.transformAsync(nullFuture, constantAsyncFunction(nullFuture));
    assertNull(chainedFuture.get());
  }

  public void testTransformAsync_genericsHierarchy_AsyncFunction() throws Exception {
    ListenableFuture<FooChild> future = Futures.immediateFuture(null);
    final BarChild barChild = new BarChild();
    AsyncFunction<Foo, BarChild> function =
        new AsyncFunction<Foo, BarChild>() {
          @Override
          public AbstractFuture<BarChild> apply(Foo unused) {
            AbstractFuture<BarChild> future = new AbstractFuture<BarChild>() {
            };
            future.set(barChild);
            return future;
          }
        };
    Bar bar = Futures.transformAsync(future, function).get();
    assertSame(barChild, bar);
  }

  public void testTransformAsync_asyncFunction_error() throws InterruptedException {
    final Error error = new Error("deliberate");
    AsyncFunction<String, Integer> function = new AsyncFunction<String, Integer>() {
      @Override
      public ListenableFuture<Integer> apply(String input) {
        throw error;
      }
    };
    SettableFuture<String> inputFuture = SettableFuture.create();
    ListenableFuture<Integer> outputFuture = Futures.transformAsync(inputFuture, function);
    inputFuture.set("value");
    try {
      outputFuture.get();
      fail("should have thrown error");
    } catch (ExecutionException e) {
      assertSame(error, e.getCause());
    }
  }

  public void testTransformAsync_asyncFunction_nullInsteadOfFuture() throws Exception {
    ListenableFuture<?> inputFuture = immediateFuture("a");
    ListenableFuture<?> chainedFuture =
        Futures.transformAsync(inputFuture, constantAsyncFunction(null));
    try {
      chainedFuture.get();
      fail();
    } catch (ExecutionException expected) {
      NullPointerException cause = (NullPointerException) expected.getCause();
      assertThat(cause).hasMessage("AsyncFunction.apply returned null instead of a Future. "
          + "Did you mean to return immediateFuture(null)?");
    }
  }

  public void testDereference_genericsWildcard() throws Exception {
    ListenableFuture<?> inner = Futures.immediateFuture(null);
    ListenableFuture<ListenableFuture<?>> outer =
        Futures.<ListenableFuture<?>>immediateFuture(inner);
    ListenableFuture<?> dereferenced = Futures.dereference(outer);
    assertNull(dereferenced.get());
  }

  public void testDereference_genericsHierarchy() throws Exception {
    FooChild fooChild = new FooChild();
    ListenableFuture<FooChild> inner = Futures.immediateFuture(fooChild);
    ListenableFuture<ListenableFuture<FooChild>> outer = Futures.immediateFuture(inner);
    ListenableFuture<Foo> dereferenced = Futures.<Foo>dereference(outer);
    assertSame(fooChild, dereferenced.get());
  }

  public void testDereference_resultCancelsOuter() throws Exception {
    ListenableFuture<ListenableFuture<Foo>> outer = SettableFuture.create();
    ListenableFuture<Foo> dereferenced = Futures.dereference(outer);
    dereferenced.cancel(true);
    assertTrue(outer.isCancelled());
  }

  public void testDereference_resultCancelsInner() throws Exception {
    ListenableFuture<Foo> inner = SettableFuture.create();
    ListenableFuture<ListenableFuture<Foo>> outer = Futures.immediateFuture(inner);
    ListenableFuture<Foo> dereferenced = Futures.dereference(outer);
    dereferenced.cancel(true);
    assertTrue(inner.isCancelled());
  }

  public void testDereference_outerCancelsResult() throws Exception {
    ListenableFuture<ListenableFuture<Foo>> outer = SettableFuture.create();
    ListenableFuture<Foo> dereferenced = Futures.dereference(outer);
    outer.cancel(true);
    assertTrue(dereferenced.isCancelled());
  }

  public void testDereference_innerCancelsResult() throws Exception {
    ListenableFuture<Foo> inner = SettableFuture.create();
    ListenableFuture<ListenableFuture<Foo>> outer = Futures.immediateFuture(inner);
    ListenableFuture<Foo> dereferenced = Futures.dereference(outer);
    inner.cancel(true);
    assertTrue(dereferenced.isCancelled());
  }

  /**
   * Runnable which can be called a single time, and only after {@link #expectCall} is called.
   */
  // TODO(cpovirk): top-level class?
  static class SingleCallListener implements Runnable {

    private boolean expectCall = false;
    private final CountDownLatch calledCountDown =
        new CountDownLatch(1);

    @Override
    public void run() {
      assertTrue("Listener called before it was expected", expectCall);
      assertFalse("Listener called more than once", wasCalled());
      calledCountDown.countDown();
    }

    public void expectCall() {
      assertFalse("expectCall is already true", expectCall);
      expectCall = true;
    }

    public boolean wasCalled() {
      return calledCountDown.getCount() == 0;
    }

    public void waitForCall() throws InterruptedException {
      assertTrue("expectCall is false", expectCall);
      calledCountDown.await();
    }
  }

  public void testAllAsList() throws Exception {
    // Create input and output
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    SettableFuture<String> future3 = SettableFuture.create();
    @SuppressWarnings("unchecked") // array is never modified
        ListenableFuture<List<String>> compound =
        Futures.allAsList(future1, future2, future3);

    // Attach a listener
    SingleCallListener listener = new SingleCallListener();
    compound.addListener(listener, directExecutor());

    // Satisfy each input and check the output
    assertFalse(compound.isDone());
    future1.set(DATA1);
    assertFalse(compound.isDone());
    future2.set(DATA2);
    assertFalse(compound.isDone());
    listener.expectCall();
    future3.set(DATA3);
    assertTrue(compound.isDone());
    assertTrue(listener.wasCalled());

    List<String> results = compound.get();
    assertThat(results).containsExactly(DATA1, DATA2, DATA3).inOrder();
  }

  public void testAllAsList_emptyArray() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    @SuppressWarnings("unchecked") // array is never modified
        ListenableFuture<List<String>> compound = Futures.allAsList();
    compound.addListener(listener, directExecutor());
    assertTrue(compound.isDone());
    assertTrue(compound.get().isEmpty());
    assertTrue(listener.wasCalled());
  }

  public void testAllAsList_failure() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    @SuppressWarnings("unchecked") // array is never modified
        ListenableFuture<List<String>> compound =
        Futures.allAsList(future1, future2);
    compound.addListener(listener, directExecutor());

    listener.expectCall();
    Throwable exception = new Throwable("failed1");
    future1.setException(exception);
    assertTrue(compound.isDone());
    assertTrue(listener.wasCalled());
    future2.set("result2");

    try {
      compound.get();
      fail("Expected exception not thrown");
    } catch (ExecutionException e) {
      assertSame(exception, e.getCause());
    }
  }

  public void testAllAsList_cancelled() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    @SuppressWarnings("unchecked") // array is never modified
        ListenableFuture<List<String>> compound =
        Futures.allAsList(future1, future2);
    compound.addListener(listener, directExecutor());

    listener.expectCall();
    future1.cancel(true);
    assertTrue(compound.isDone());
    assertTrue(listener.wasCalled());
    future2.setException(new Throwable("failed2"));

    try {
      compound.get();
      fail("Expected exception not thrown");
    } catch (CancellationException e) {
      // Expected
    }
  }

  public void testAllAsList_resultCancelled() throws Exception {
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    @SuppressWarnings("unchecked") // array is never modified
        ListenableFuture<List<String>> compound =
        Futures.allAsList(future1, future2);

    future2.set(DATA2);
    assertFalse(compound.isDone());
    assertTrue(compound.cancel(false));
    assertTrue(compound.isCancelled());
    assertTrue(future1.isCancelled());
    assertFalse(future1.wasInterrupted());
  }

  public void testAllAsList_resultCancelledInterrupted_withSecondaryListFuture()
      throws Exception {
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound =
        Futures.allAsList(future1, future2);
    // There was a bug where the event listener for the combined future would
    // result in the sub-futures being cancelled without being interrupted.
    ListenableFuture<List<String>> otherCompound =
        Futures.allAsList(future1, future2);

    assertTrue(compound.cancel(true));
    assertTrue(future1.isCancelled());
    assertTrue(future1.wasInterrupted());
    assertTrue(future2.isCancelled());
    assertTrue(future2.wasInterrupted());
    assertTrue(otherCompound.isCancelled());
  }

  public void testAllAsList_resultCancelled_withSecondaryListFuture()
      throws Exception {
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound =
        Futures.allAsList(future1, future2);
    // This next call is "unused," but it is an important part of the test. Don't remove it!
    Futures.allAsList(future1, future2);

    assertTrue(compound.cancel(false));
    assertTrue(future1.isCancelled());
    assertFalse(future1.wasInterrupted());
    assertTrue(future2.isCancelled());
    assertFalse(future2.wasInterrupted());
  }

  public void testAllAsList_resultInterrupted() throws Exception {
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    @SuppressWarnings("unchecked") // array is never modified
        ListenableFuture<List<String>> compound =
        Futures.allAsList(future1, future2);

    future2.set(DATA2);
    assertFalse(compound.isDone());
    assertTrue(compound.cancel(true));
    assertTrue(compound.isCancelled());
    assertTrue(future1.isCancelled());
    assertTrue(future1.wasInterrupted());
  }

  /**
   * Test the case where the futures are fulfilled prior to constructing the ListFuture.  There was
   * a bug where the loop that connects a Listener to each of the futures would die on the last
   * loop-check as done() on ListFuture nulled out the variable being looped over (the list of
   * futures).
   */
  public void testAllAsList_doneFutures() throws Exception {
    // Create input and output
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    SettableFuture<String> future3 = SettableFuture.create();

    // Satisfy each input prior to creating compound and check the output
    future1.set(DATA1);
    future2.set(DATA2);
    future3.set(DATA3);

    @SuppressWarnings("unchecked") // array is never modified
        ListenableFuture<List<String>> compound =
        Futures.allAsList(future1, future2, future3);

    // Attach a listener
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    compound.addListener(listener, directExecutor());

    assertTrue(compound.isDone());
    assertTrue(listener.wasCalled());

    List<String> results = compound.get();
    assertThat(results).containsExactly(DATA1, DATA2, DATA3).inOrder();
  }

  private static String createCombinedResult(Integer i, Boolean b) {
    return "-" + i + "-" + b;
  }

  /*
   * TODO(cpovirk): maybe pass around TestFuture instances instead of
   * ListenableFuture instances
   */

  private static final class OtherThrowable extends Throwable {

  }

  // Boring untimed-get tests:

  // Boring timed-get tests:

  // Boring getUnchecked tests:

  // Edge case tests of the exception-construction code through untimed get():

  // Mostly an example of how it would look like to use a list of mixed types

  static AssertionFailedError failureWithCause(Throwable cause, String message) {
    AssertionFailedError failure = new AssertionFailedError(message);
    failure.initCause(cause);
    return failure;
  }

  /**
   * A future that throws a runtime exception from get.
   */
  static class BuggyFuture extends AbstractFuture<String> {

    @Override
    public String get() {
      throw new RuntimeException();
    }

    @Override
    public boolean set(String v) {
      return super.set(v);
    }
  }

  // This test covers a bug where an Error thrown from a callback could cause the TimeoutFuture to
  // never complete when timing out.  Notably, nothing would get logged since the Error would get
  // stuck in the ScheduledFuture inside of TimeoutFuture and nothing ever calls get on it.

  // Simulate a timeout that fires before the call the SES.schedule returns but the future is
  // already completed.
}
