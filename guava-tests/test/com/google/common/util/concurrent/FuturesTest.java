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

import static com.google.common.base.Functions.constant;
import static com.google.common.base.Functions.identity;
import static com.google.common.base.Throwables.propagateIfInstanceOf;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.intersection;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.catching;
import static com.google.common.util.concurrent.Futures.catchingAsync;
import static com.google.common.util.concurrent.Futures.getDone;
import static com.google.common.util.concurrent.Futures.immediateCancelledFuture;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.immediateVoidFuture;
import static com.google.common.util.concurrent.Futures.inCompletionOrder;
import static com.google.common.util.concurrent.Futures.lazyTransform;
import static com.google.common.util.concurrent.Futures.nonCancellationPropagating;
import static com.google.common.util.concurrent.Futures.scheduleAsync;
import static com.google.common.util.concurrent.Futures.submit;
import static com.google.common.util.concurrent.Futures.submitAsync;
import static com.google.common.util.concurrent.Futures.successfulAsList;
import static com.google.common.util.concurrent.Futures.transform;
import static com.google.common.util.concurrent.Futures.transformAsync;
import static com.google.common.util.concurrent.Futures.whenAllComplete;
import static com.google.common.util.concurrent.Futures.whenAllSucceed;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.util.concurrent.TestPlatform.clearInterrupt;
import static com.google.common.util.concurrent.TestPlatform.getDoneFromTimeoutOverload;
import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static com.google.common.util.concurrent.testing.TestingExecutors.noOpScheduledExecutor;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.testing.ClassSanityTester;
import com.google.common.testing.GcFinalization;
import com.google.common.testing.TestLogHandler;
import com.google.common.util.concurrent.TestExceptions.SomeError;
import com.google.common.util.concurrent.TestExceptions.SomeUncheckedException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Unit tests for {@link Futures}.
 *
 * @author Nishant Thakkar
 */
@NullMarked
@GwtCompatible(emulated = true)
public class FuturesTest extends TestCase {
  private static final Logger aggregateFutureLogger =
      Logger.getLogger(AggregateFuture.class.getName());
  private final TestLogHandler aggregateFutureLogHandler = new TestLogHandler();

  private static final String DATA1 = "data";
  private static final String DATA2 = "more data";
  private static final String DATA3 = "most data";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    aggregateFutureLogger.addHandler(aggregateFutureLogHandler);
  }

  @Override
  public void tearDown() throws Exception {
    /*
     * Clear interrupt for future tests.
     *
     * (Ideally we would perform interrupts only in threads that we create, but
     * it's hard to imagine that anything will break in practice.)
     */
    clearInterrupt();
    aggregateFutureLogger.removeHandler(aggregateFutureLogHandler);
    super.tearDown();
  }

  /*
   * TODO(cpovirk): Use FutureSubject once it's part of core Truth. But be wary of using it when I'm
   * really testing a Future implementation (e.g., in the case of immediate*Future()). But it's OK
   * to use in the case of the majority of Futures that are AbstractFutures.
   */

  public void testImmediateFuture() throws Exception {
    ListenableFuture<String> future = immediateFuture(DATA1);

    assertSame(DATA1, getDone(future));
    assertSame(DATA1, getDoneFromTimeoutOverload(future));
    assertThat(future.toString()).contains("[status=SUCCESS, result=[" + DATA1 + "]]");
  }

  public void testImmediateVoidFuture() throws Exception {
    ListenableFuture<@Nullable Void> voidFuture = immediateVoidFuture();

    assertThat(getDone(voidFuture)).isNull();
    assertThat(getDoneFromTimeoutOverload(voidFuture)).isNull();
    assertThat(voidFuture.toString()).contains("[status=SUCCESS, result=[null]]");
  }

  public void testImmediateFailedFuture() throws Exception {
    Exception exception = new Exception();
    ListenableFuture<String> future = immediateFailedFuture(exception);
    assertThat(future.toString()).endsWith("[status=FAILURE, cause=[" + exception + "]]");

    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(future));
    assertSame(exception, expected.getCause());

    expected = assertThrows(ExecutionException.class, () -> getDoneFromTimeoutOverload(future));
    assertSame(exception, expected.getCause());
  }

  public void testImmediateFailedFuture_cancellationException() throws Exception {
    CancellationException exception = new CancellationException();
    ListenableFuture<String> future = immediateFailedFuture(exception);
    assertFalse(future.isCancelled());
    assertThat(future.toString()).endsWith("[status=FAILURE, cause=[" + exception + "]]");

    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(future));
    assertSame(exception, expected.getCause());

    expected = assertThrows(ExecutionException.class, () -> getDoneFromTimeoutOverload(future));
    assertSame(exception, expected.getCause());
  }

  public void testImmediateCancelledFutureBasic() throws Exception {
    ListenableFuture<String> future = CallerClass1.makeImmediateCancelledFuture();
    assertTrue(future.isCancelled());
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testImmediateCancelledFutureStack() throws Exception {
    ListenableFuture<String> future = CallerClass1.makeImmediateCancelledFuture();
    assertTrue(future.isCancelled());

    CancellationException expected =
        assertThrows(CancellationException.class, () -> CallerClass2.get(future));
    List<StackTraceElement> stackTrace = ImmutableList.copyOf(expected.getStackTrace());
    assertFalse(Iterables.any(stackTrace, hasClassName(CallerClass1.class)));
    assertTrue(Iterables.any(stackTrace, hasClassName(CallerClass2.class)));

    // See AbstractFutureCancellationCauseTest for how to set causes.
    assertThat(expected).hasCauseThat().isNull();
  }

  @J2ktIncompatible
  @GwtIncompatible // used only in GwtIncompatible tests
  private static Predicate<StackTraceElement> hasClassName(Class<?> clazz) {
    return new Predicate<StackTraceElement>() {
      @Override
      public boolean apply(StackTraceElement element) {
        return element.getClassName().equals(clazz.getName());
      }
    };
  }

  private static final class CallerClass1 {
    static ListenableFuture<String> makeImmediateCancelledFuture() {
      return immediateCancelledFuture();
    }
  }

  private static final class CallerClass2 {
    @CanIgnoreReturnValue
    static <V> V get(ListenableFuture<V> future) throws ExecutionException, InterruptedException {
      return getDone(future);
    }
  }

  private static class MyException extends Exception {}

  // Class hierarchy for generics sanity checks
  private static class Foo {}

  private static class FooChild extends Foo {}

  private static class Bar {}

  private static class BarChild extends Bar {}

  @J2ktIncompatible // TODO(b/324550390): Enable
  public void testTransform_genericsNull() throws Exception {
    ListenableFuture<?> nullFuture = immediateFuture(null);
    ListenableFuture<?> transformedFuture = transform(nullFuture, constant(null), directExecutor());
    assertNull(getDone(transformedFuture));
  }

  @J2ktIncompatible // TODO(b/324550390): Enable
  public void testTransform_genericsHierarchy() throws Exception {
    ListenableFuture<FooChild> future = immediateFuture(null);
    BarChild barChild = new BarChild();
    Function<Foo, BarChild> function =
        new Function<Foo, BarChild>() {
          @Override
          public BarChild apply(Foo unused) {
            return barChild;
          }
        };
    Bar bar = getDone(transform(future, function, directExecutor()));
    assertSame(barChild, bar);
  }

  @J2ktIncompatible
  @GwtIncompatible // StackOverflowError
  public void testTransform_stackOverflow() throws Exception {
    SettableFuture<Object> input = SettableFuture.create();
    ListenableFuture<Object> output = transform(input, identity(), directExecutor());
    output.addListener(
        () -> {
          throw new StackOverflowError();
        },
        directExecutor());
    assertThrows(StackOverflowError.class, () -> input.set("foo"));
  }

  public void testTransform_errorAfterCancellation() throws Exception {
    class Transformer implements Function<Object, Object> {
      @SuppressWarnings("nullness:initialization.field.uninitialized")
      ListenableFuture<Object> output;

      @Override
      public Object apply(Object input) {
        output.cancel(false);
        throw new SomeError();
      }
    }
    Transformer transformer = new Transformer();
    SettableFuture<Object> input = SettableFuture.create();

    ListenableFuture<Object> output = transform(input, transformer, directExecutor());
    transformer.output = output;

    input.set("foo");
    assertTrue(output.isCancelled());
  }

  public void testTransform_exceptionAfterCancellation() throws Exception {
    class Transformer implements Function<Object, Object> {
      @SuppressWarnings("nullness:initialization.field.uninitialized")
      ListenableFuture<Object> output;

      @Override
      public Object apply(Object input) {
        output.cancel(false);
        throw new SomeUncheckedException();
      }
    }
    Transformer transformer = new Transformer();
    SettableFuture<Object> input = SettableFuture.create();

    ListenableFuture<Object> output = transform(input, transformer, directExecutor());
    transformer.output = output;

    input.set("foo");
    assertTrue(output.isCancelled());
  }

  public void testTransform_getThrowsRuntimeException() throws Exception {
    ListenableFuture<Object> input =
        UncheckedThrowingFuture.throwingRuntimeException(new SomeUncheckedException());

    ListenableFuture<Object> output = transform(input, identity(), directExecutor());
    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(output));
    assertThat(expected).hasCauseThat().isInstanceOf(SomeUncheckedException.class);
  }

  public void testTransform_getThrowsError() throws Exception {
    ListenableFuture<Object> input = UncheckedThrowingFuture.throwingError(new SomeError());

    ListenableFuture<Object> output = transform(input, identity(), directExecutor());
    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(output));
    assertThat(expected).hasCauseThat().isInstanceOf(SomeError.class);
  }

  public void testTransform_listenerThrowsError() throws Exception {
    SettableFuture<Object> input = SettableFuture.create();
    ListenableFuture<Object> output = transform(input, identity(), directExecutor());

    output.addListener(
        new Runnable() {
          @Override
          public void run() {
            throw new SomeError();
          }
        },
        directExecutor());
    assertThrows(SomeError.class, () -> input.set("foo"));
  }

  public void testTransformAsync_cancelPropagatesToInput() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    AsyncFunction<Foo, Bar> function =
        new AsyncFunction<Foo, Bar>() {
          @Override
          public ListenableFuture<Bar> apply(Foo unused) {
            throw new AssertionFailedError("Unexpected call to apply.");
          }
        };
    assertTrue(transformAsync(input, function, directExecutor()).cancel(false));
    assertTrue(input.isCancelled());
    assertFalse(input.wasInterrupted());
  }

  public void testTransformAsync_interruptPropagatesToInput() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    AsyncFunction<Foo, Bar> function =
        new AsyncFunction<Foo, Bar>() {
          @Override
          public ListenableFuture<Bar> apply(Foo unused) {
            throw new AssertionFailedError("Unexpected call to apply.");
          }
        };
    assertTrue(transformAsync(input, function, directExecutor()).cancel(true));
    assertTrue(input.isCancelled());
    assertTrue(input.wasInterrupted());
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testTransformAsync_interruptPropagatesToTransformingThread() throws Exception {
    SettableFuture<String> input = SettableFuture.create();
    CountDownLatch inFunction = new CountDownLatch(1);
    CountDownLatch shouldCompleteFunction = new CountDownLatch(1);
    CountDownLatch gotException = new CountDownLatch(1);
    AsyncFunction<String, String> function =
        new AsyncFunction<String, String>() {
          @Override
          public ListenableFuture<String> apply(String s) throws Exception {
            inFunction.countDown();
            try {
              shouldCompleteFunction.await();
            } catch (InterruptedException expected) {
              gotException.countDown();
              throw expected;
            }
            return immediateFuture("a");
          }
        };

    ExecutorService service = newSingleThreadExecutor();
    ListenableFuture<String> futureResult = transformAsync(input, function, service);

    input.set("value");
    inFunction.await();
    futureResult.cancel(true);
    shouldCompleteFunction.countDown();
    assertThrows(CancellationException.class, () -> futureResult.get());
    // TODO(cpovirk): implement interruption, updating this test:
    // https://github.com/google/guava/issues/1989
    assertEquals(1, gotException.getCount());
    // gotException.await();
    service.shutdown();
    service.awaitTermination(30, SECONDS);
  }

  public void testTransformAsync_cancelPropagatesToAsyncOutput() throws Exception {
    ListenableFuture<Foo> immediate = immediateFuture(new Foo());
    SettableFuture<Bar> secondary = SettableFuture.create();
    AsyncFunction<Foo, Bar> function =
        new AsyncFunction<Foo, Bar>() {
          @Override
          public ListenableFuture<Bar> apply(Foo unused) {
            return secondary;
          }
        };
    assertTrue(transformAsync(immediate, function, directExecutor()).cancel(false));
    assertTrue(secondary.isCancelled());
    assertFalse(secondary.wasInterrupted());
  }

  public void testTransformAsync_interruptPropagatesToAsyncOutput() throws Exception {
    ListenableFuture<Foo> immediate = immediateFuture(new Foo());
    SettableFuture<Bar> secondary = SettableFuture.create();
    AsyncFunction<Foo, Bar> function =
        new AsyncFunction<Foo, Bar>() {
          @Override
          public ListenableFuture<Bar> apply(Foo unused) {
            return secondary;
          }
        };
    assertTrue(transformAsync(immediate, function, directExecutor()).cancel(true));
    assertTrue(secondary.isCancelled());
    assertTrue(secondary.wasInterrupted());
  }

  public void testTransformAsync_inputCancelButNotInterruptPropagatesToOutput() throws Exception {
    SettableFuture<Foo> f1 = SettableFuture.create();
    SettableFuture<Bar> secondary = SettableFuture.create();
    AsyncFunction<Foo, Bar> function =
        new AsyncFunction<Foo, Bar>() {
          @Override
          public ListenableFuture<Bar> apply(Foo unused) {
            return secondary;
          }
        };
    ListenableFuture<Bar> f2 = transformAsync(f1, function, directExecutor());
    f1.cancel(true);
    assertTrue(f2.isCancelled());
    /*
     * We might like to propagate interruption, too, but it's not clear that it matters. For now, we
     * test for the behavior that we have today.
     */
    assertFalse(((AbstractFuture<?>) f2).wasInterrupted());
  }

  @J2ktIncompatible
  @GwtIncompatible // StackOverflowError
  public void testTransformAsync_stackOverflow() throws Exception {
    SettableFuture<Object> input = SettableFuture.create();
    ListenableFuture<Object> output = transformAsync(input, asyncIdentity(), directExecutor());
    output.addListener(
        () -> {
          throw new StackOverflowError();
        },
        directExecutor());
    assertThrows(StackOverflowError.class, () -> input.set("foo"));
  }

  public void testTransformAsync_errorAfterCancellation() throws Exception {
    class Transformer implements AsyncFunction<Object, Object> {
      @SuppressWarnings("nullness:initialization.field.uninitialized")
      ListenableFuture<Object> output;

      @Override
      public ListenableFuture<Object> apply(Object input) {
        output.cancel(false);
        throw new SomeError();
      }
    }
    Transformer transformer = new Transformer();
    SettableFuture<Object> input = SettableFuture.create();

    ListenableFuture<Object> output = transformAsync(input, transformer, directExecutor());
    transformer.output = output;

    input.set("foo");
    assertTrue(output.isCancelled());
  }

  public void testTransformAsync_exceptionAfterCancellation() throws Exception {
    class Transformer implements AsyncFunction<Object, Object> {
      @SuppressWarnings("nullness:initialization.field.uninitialized")
      ListenableFuture<Object> output;

      @Override
      public ListenableFuture<Object> apply(Object input) {
        output.cancel(false);
        throw new SomeUncheckedException();
      }
    }
    Transformer transformer = new Transformer();
    SettableFuture<Object> input = SettableFuture.create();

    ListenableFuture<Object> output = transformAsync(input, transformer, directExecutor());
    transformer.output = output;

    input.set("foo");
    assertTrue(output.isCancelled());
  }

  public void testTransformAsync_getThrowsRuntimeException() throws Exception {
    ListenableFuture<Object> input =
        UncheckedThrowingFuture.throwingRuntimeException(new SomeUncheckedException());

    ListenableFuture<Object> output = transformAsync(input, asyncIdentity(), directExecutor());
    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(output));
    assertThat(expected).hasCauseThat().isInstanceOf(SomeUncheckedException.class);
  }

  public void testTransformAsync_getThrowsError() throws Exception {
    ListenableFuture<Object> input = UncheckedThrowingFuture.throwingError(new SomeError());

    ListenableFuture<Object> output = transformAsync(input, asyncIdentity(), directExecutor());
    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(output));
    assertThat(expected).hasCauseThat().isInstanceOf(SomeError.class);
  }

  public void testTransformAsync_listenerThrowsError() throws Exception {
    SettableFuture<Object> input = SettableFuture.create();
    ListenableFuture<Object> output = transformAsync(input, asyncIdentity(), directExecutor());

    output.addListener(
        new Runnable() {
          @Override
          public void run() {
            throw new SomeError();
          }
        },
        directExecutor());
    assertThrows(SomeError.class, () -> input.set("foo"));
  }

  public void testTransform_rejectionPropagatesToOutput() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    Function<Foo, Foo> identity = identity();
    ListenableFuture<Foo> transformed = transform(input, identity, REJECTING_EXECUTOR);
    input.set(new Foo());
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(transformed));
    assertThat(expected).hasCauseThat().isInstanceOf(RejectedExecutionException.class);
  }

  public void testTransformAsync_rejectionPropagatesToOutput() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    AsyncFunction<Foo, Foo> asyncIdentity = asyncIdentity();
    ListenableFuture<Foo> transformed = transformAsync(input, asyncIdentity, REJECTING_EXECUTOR);
    input.set(new Foo());
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(transformed));
    assertThat(expected).hasCauseThat().isInstanceOf(RejectedExecutionException.class);
  }

  /** Tests that the function is invoked only once, even if it throws an exception. */
  public void testTransformValueRemainsMemoized() throws Exception {
    class Holder {

      int value = 2;
    }
    Holder holder = new Holder();

    // This function adds the holder's value to the input value.
    Function<Integer, Integer> adder =
        new Function<Integer, Integer>() {
          @Override
          public Integer apply(Integer from) {
            return from + holder.value;
          }
        };

    // Since holder.value is 2, applying 4 should yield 6.
    assertEquals(6, adder.apply(4).intValue());

    ListenableFuture<Integer> immediateFuture = immediateFuture(4);
    Future<Integer> transformedFuture = transform(immediateFuture, adder, directExecutor());

    // The composed future also yields 6.
    assertEquals(6, getDone(transformedFuture).intValue());

    // Repeated calls yield the same value even though the function's behavior
    // changes
    holder.value = 3;
    assertEquals(6, getDone(transformedFuture).intValue());
    assertEquals(7, adder.apply(4).intValue());

    // Once more, with feeling.
    holder.value = 4;
    assertEquals(6, getDone(transformedFuture).intValue());
    assertEquals(8, adder.apply(4).intValue());

    // Memoized get also retains the value.
    assertEquals(6, getDoneFromTimeoutOverload(transformedFuture).intValue());

    // Unsurprisingly, recomposing the future will return an updated value.
    assertEquals(8, getDone(transform(immediateFuture, adder, directExecutor())).intValue());

    // Repeating, with the timeout version
    assertEquals(
        8,
        getDoneFromTimeoutOverload(transform(immediateFuture, adder, directExecutor())).intValue());
  }

  /**
   * Test that the function is invoked only once, even if it throws an exception. Also, test that
   * that function's result is wrapped in an ExecutionException.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public void testTransformExceptionRemainsMemoized() throws Throwable {
    // We need to test with two input futures since ExecutionList.execute
    // doesn't catch Errors and we cannot depend on the order that our
    // transformations run. (So it is possible that the Error being thrown
    // could prevent our second transformations from running).
    SettableFuture<Integer> exceptionInput = SettableFuture.create();
    ListenableFuture<Integer> exceptionComposedFuture =
        transform(exceptionInput, newOneTimeExceptionThrower(), directExecutor());
    exceptionInput.set(0);
    runGetIdempotencyTest(exceptionComposedFuture, SomeUncheckedException.class);

    SettableFuture<Integer> errorInput = SettableFuture.create();
    ListenableFuture<Integer> errorComposedFuture =
        transform(errorInput, newOneTimeErrorThrower(), directExecutor());
    errorInput.set(0);

    runGetIdempotencyTest(errorComposedFuture, SomeError.class);

    /*
     * Try again when the input's value is already filled in, since the flow is
     * slightly different in that case.
     */
    exceptionComposedFuture =
        transform(exceptionInput, newOneTimeExceptionThrower(), directExecutor());
    runGetIdempotencyTest(exceptionComposedFuture, SomeUncheckedException.class);

    runGetIdempotencyTest(
        transform(errorInput, newOneTimeErrorThrower(), directExecutor()), SomeError.class);
    runGetIdempotencyTest(errorComposedFuture, SomeError.class);
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  private static void runGetIdempotencyTest(
      Future<Integer> transformedFuture, Class<? extends Throwable> expectedExceptionClass)
      throws Throwable {
    for (int i = 0; i < 5; i++) {
      try {
        getDone(transformedFuture);
        fail();
      } catch (ExecutionException expected) {
        if (!expectedExceptionClass.isInstance(expected.getCause())) {
          throw expected.getCause();
        }
      }
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // used only in GwtIncompatible tests
  private static Function<Integer, Integer> newOneTimeExceptionThrower() {
    return new Function<Integer, Integer>() {
      int calls = 0;

      @Override
      public Integer apply(Integer from) {
        if (++calls > 1) {
          fail();
        }
        throw new SomeUncheckedException();
      }
    };
  }

  @J2ktIncompatible
  @GwtIncompatible // used only in GwtIncompatible tests
  private static Function<Integer, Integer> newOneTimeErrorThrower() {
    return new Function<Integer, Integer>() {
      int calls = 0;

      @Override
      public Integer apply(Integer from) {
        if (++calls > 1) {
          fail();
        }
        throw new SomeError();
      }
    };
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

  public void testTransform_executor() throws Exception {
    Object value = new Object();
    ExecutorSpy spy = new ExecutorSpy(directExecutor());

    assertFalse(spy.wasExecuted);

    ListenableFuture<Object> future = transform(immediateFuture(value), identity(), spy);

    assertSame(value, getDone(future));
    assertTrue(spy.wasExecuted);
  }

  @J2ktIncompatible
  @GwtIncompatible // Threads
  public void testTransformAsync_functionToString() throws Exception {
    CountDownLatch functionCalled = new CountDownLatch(1);
    CountDownLatch functionBlocking = new CountDownLatch(1);
    AsyncFunction<Object, Object> function =
        tagged(
            "Called my toString",
            new AsyncFunction<Object, Object>() {
              @Override
              public ListenableFuture<Object> apply(Object input) throws Exception {
                functionCalled.countDown();
                functionBlocking.await();
                return immediateFuture(null);
              }
            });

    ExecutorService executor = newSingleThreadExecutor();
    try {
      ListenableFuture<?> output =
          Futures.transformAsync(immediateFuture(null), function, executor);
      functionCalled.await();
      assertThat(output.toString()).contains(function.toString());
    } finally {
      functionBlocking.countDown();
      executor.shutdown();
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // lazyTransform
  public void testLazyTransform() throws Exception {
    FunctionSpy<Object, String> spy = new FunctionSpy<>(constant("bar"));
    Future<String> input = immediateFuture("foo");
    Future<String> transformed = lazyTransform(input, spy);
    spy.verifyCallCount(0);
    assertEquals("bar", getDone(transformed));
    spy.verifyCallCount(1);
    assertEquals("bar", getDone(transformed));
    spy.verifyCallCount(2);
  }

  @J2ktIncompatible
  @GwtIncompatible // lazyTransform
  public void testLazyTransform_exception() throws Exception {
    RuntimeException exception = new RuntimeException("deliberate");
    Function<Integer, String> function =
        new Function<Integer, String>() {
          @Override
          public String apply(Integer input) {
            throw exception;
          }
        };
    Future<String> transformed = lazyTransform(immediateFuture(1), function);
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(transformed));
    assertSame(exception, expected.getCause());
    expected =
        assertThrows(ExecutionException.class, () -> getDoneFromTimeoutOverload(transformed));
    assertSame(exception, expected.getCause());
  }

  private static class FunctionSpy<I, O> implements Function<I, O> {
    private int applyCount;
    private final Function<I, O> delegate;

    FunctionSpy(Function<I, O> delegate) {
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

  private static <X extends Throwable, V> Function<X, V> unexpectedFunction() {
    return new Function<X, V>() {
      @Override
      public V apply(X t) {
        throw new AssertionError("Unexpected fallback", t);
      }
    };
  }

  private static class AsyncFunctionSpy<X extends Throwable, V> implements AsyncFunction<X, V> {
    private int count;
    private final AsyncFunction<X, V> delegate;

    AsyncFunctionSpy(AsyncFunction<X, V> delegate) {
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

  private static <I, O> FunctionSpy<I, O> spy(Function<I, O> delegate) {
    return new FunctionSpy<>(delegate);
  }

  private static <X extends Throwable, V> AsyncFunctionSpy<X, V> spy(AsyncFunction<X, V> delegate) {
    return new AsyncFunctionSpy<>(delegate);
  }

  private static <X extends Throwable, V> AsyncFunction<X, V> unexpectedAsyncFunction() {
    return new AsyncFunction<X, V>() {
      @Override
      public ListenableFuture<V> apply(X t) {
        throw new AssertionError("Unexpected fallback", t);
      }
    };
  }

  // catchingAsync tests cloned from the old withFallback tests:

  public void testCatchingAsync_inputDoesNotRaiseException() throws Exception {
    AsyncFunction<Throwable, Integer> fallback = unexpectedAsyncFunction();
    ListenableFuture<Integer> originalFuture = immediateFuture(7);
    ListenableFuture<Integer> faultTolerantFuture =
        catchingAsync(originalFuture, Throwable.class, fallback, directExecutor());
    assertEquals(7, getDone(faultTolerantFuture).intValue());
  }

  public void testCatchingAsync_inputRaisesException() throws Exception {
    RuntimeException raisedException = new RuntimeException();
    AsyncFunctionSpy<Throwable, Integer> fallback =
        spy(
            new AsyncFunction<Throwable, Integer>() {
              @Override
              public ListenableFuture<Integer> apply(Throwable t) throws Exception {
                assertThat(t).isSameInstanceAs(raisedException);
                return immediateFuture(20);
              }
            });
    ListenableFuture<Integer> failingFuture = immediateFailedFuture(raisedException);
    ListenableFuture<Integer> faultTolerantFuture =
        catchingAsync(failingFuture, Throwable.class, fallback, directExecutor());
    assertEquals(20, getDone(faultTolerantFuture).intValue());
    fallback.verifyCallCount(1);
  }

  @J2ktIncompatible
  @GwtIncompatible // non-Throwable exceptionType
  public void testCatchingAsync_inputCancelledWithoutFallback() throws Exception {
    AsyncFunction<Throwable, Integer> fallback = unexpectedAsyncFunction();
    ListenableFuture<Integer> originalFuture = immediateCancelledFuture();
    ListenableFuture<Integer> faultTolerantFuture =
        catchingAsync(originalFuture, IOException.class, fallback, directExecutor());
    assertTrue(faultTolerantFuture.isCancelled());
  }

  public void testCatchingAsync_fallbackGeneratesRuntimeException() throws Exception {
    RuntimeException expectedException = new RuntimeException();
    runExpectedExceptionCatchingAsyncTest(expectedException, false);
  }

  public void testCatchingAsync_fallbackGeneratesCheckedException() throws Exception {
    Exception expectedException = new Exception() {};
    runExpectedExceptionCatchingAsyncTest(expectedException, false);
  }

  public void testCatchingAsync_fallbackGeneratesError() throws Exception {
    Error error = new Error("deliberate");
    AsyncFunction<Throwable, Integer> fallback =
        new AsyncFunction<Throwable, Integer>() {
          @Override
          public ListenableFuture<Integer> apply(Throwable t) throws Exception {
            throw error;
          }
        };
    ListenableFuture<Integer> failingFuture = immediateFailedFuture(new RuntimeException());
    ExecutionException expected =
        assertThrows(
            ExecutionException.class,
            () ->
                getDone(catchingAsync(failingFuture, Throwable.class, fallback, directExecutor())));
    assertSame(error, expected.getCause());
  }

  public void testCatchingAsync_fallbackReturnsRuntimeException() throws Exception {
    RuntimeException expectedException = new RuntimeException();
    runExpectedExceptionCatchingAsyncTest(expectedException, true);
  }

  public void testCatchingAsync_fallbackReturnsCheckedException() throws Exception {
    Exception expectedException = new Exception() {};
    runExpectedExceptionCatchingAsyncTest(expectedException, true);
  }

  private void runExpectedExceptionCatchingAsyncTest(
      Exception expectedException, boolean wrapInFuture) throws Exception {
    AsyncFunctionSpy<Throwable, Integer> fallback =
        spy(
            new AsyncFunction<Throwable, Integer>() {
              @Override
              public ListenableFuture<Integer> apply(Throwable t) throws Exception {
                if (!wrapInFuture) {
                  throw expectedException;
                } else {
                  return immediateFailedFuture(expectedException);
                }
              }
            });

    ListenableFuture<Integer> failingFuture = immediateFailedFuture(new RuntimeException());

    ListenableFuture<Integer> faultTolerantFuture =
        catchingAsync(failingFuture, Throwable.class, fallback, directExecutor());
    try {
      getDone(faultTolerantFuture);
      fail();
    } catch (ExecutionException expected) {
      assertSame(expectedException, expected.getCause());
    }
    fallback.verifyCallCount(1);
  }

  public void testCatchingAsync_fallbackNotReady() throws Exception {
    ListenableFuture<Integer> primary = immediateFailedFuture(new Exception());
    SettableFuture<Integer> secondary = SettableFuture.create();
    AsyncFunction<Throwable, Integer> fallback =
        new AsyncFunction<Throwable, Integer>() {
          @Override
          public ListenableFuture<Integer> apply(Throwable t) {
            return secondary;
          }
        };
    ListenableFuture<Integer> derived =
        catchingAsync(primary, Throwable.class, fallback, directExecutor());
    secondary.set(1);
    assertEquals(1, (int) getDone(derived));
  }

  public void testCatchingAsync_resultInterruptedBeforeFallback() throws Exception {
    SettableFuture<Integer> primary = SettableFuture.create();
    AsyncFunction<Throwable, Integer> fallback = unexpectedAsyncFunction();
    ListenableFuture<Integer> derived =
        catchingAsync(primary, Throwable.class, fallback, directExecutor());
    derived.cancel(true);
    assertTrue(primary.isCancelled());
    assertTrue(primary.wasInterrupted());
  }

  public void testCatchingAsync_resultCancelledBeforeFallback() throws Exception {
    SettableFuture<Integer> primary = SettableFuture.create();
    AsyncFunction<Throwable, Integer> fallback = unexpectedAsyncFunction();
    ListenableFuture<Integer> derived =
        catchingAsync(primary, Throwable.class, fallback, directExecutor());
    derived.cancel(false);
    assertTrue(primary.isCancelled());
    assertFalse(primary.wasInterrupted());
  }

  public void testCatchingAsync_resultCancelledAfterFallback() throws Exception {
    SettableFuture<Integer> secondary = SettableFuture.create();
    RuntimeException raisedException = new RuntimeException();
    AsyncFunctionSpy<Throwable, Integer> fallback =
        spy(
            new AsyncFunction<Throwable, Integer>() {
              @Override
              public ListenableFuture<Integer> apply(Throwable t) throws Exception {
                assertThat(t).isSameInstanceAs(raisedException);
                return secondary;
              }
            });

    ListenableFuture<Integer> failingFuture = immediateFailedFuture(raisedException);

    ListenableFuture<Integer> derived =
        catchingAsync(failingFuture, Throwable.class, fallback, directExecutor());
    derived.cancel(false);
    assertTrue(secondary.isCancelled());
    assertFalse(secondary.wasInterrupted());
    fallback.verifyCallCount(1);
  }

  @J2ktIncompatible // Nullability
  public void testCatchingAsync_nullInsteadOfFuture() throws Exception {
    ListenableFuture<Integer> inputFuture = immediateFailedFuture(new Exception());
    ListenableFuture<?> chainedFuture =
        catchingAsync(
            inputFuture,
            Throwable.class,
            new AsyncFunction<Throwable, Integer>() {
              @Override
              @SuppressWarnings("AsyncFunctionReturnsNull")
              public ListenableFuture<Integer> apply(Throwable t) {
                return null;
              }
            },
            directExecutor());
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(chainedFuture));
    NullPointerException cause = (NullPointerException) expected.getCause();
    assertThat(cause)
        .hasMessageThat()
        .contains(
            "AsyncFunction.apply returned null instead of a Future. "
                + "Did you mean to return immediateFuture(null)?");
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testCatchingAsync_interruptPropagatesToTransformingThread() throws Exception {
    SettableFuture<String> input = SettableFuture.create();
    CountDownLatch inFunction = new CountDownLatch(1);
    CountDownLatch shouldCompleteFunction = new CountDownLatch(1);
    CountDownLatch gotException = new CountDownLatch(1);
    AsyncFunction<Throwable, String> function =
        new AsyncFunction<Throwable, String>() {
          @Override
          public ListenableFuture<String> apply(Throwable t) throws Exception {
            inFunction.countDown();
            try {
              shouldCompleteFunction.await();
            } catch (InterruptedException expected) {
              gotException.countDown();
              throw expected;
            }
            return immediateFuture("a");
          }
        };

    ExecutorService executor = newSingleThreadExecutor();
    ListenableFuture<String> futureResult =
        catchingAsync(input, Exception.class, function, executor);

    input.setException(new Exception());
    inFunction.await();
    futureResult.cancel(true);
    shouldCompleteFunction.countDown();
    assertThrows(CancellationException.class, () -> futureResult.get());
    // TODO(cpovirk): implement interruption, updating this test:
    // https://github.com/google/guava/issues/1989
    assertEquals(1, gotException.getCount());
    // gotException.await();
    executor.shutdown();
    executor.awaitTermination(30, SECONDS);
  }

  @J2ktIncompatible
  @GwtIncompatible // Threads
  public void testCatchingAsync_functionToString() throws Exception {
    CountDownLatch functionCalled = new CountDownLatch(1);
    CountDownLatch functionBlocking = new CountDownLatch(1);
    AsyncFunction<Object, Object> function =
        tagged(
            "Called my toString",
            new AsyncFunction<Object, Object>() {
              @Override
              public ListenableFuture<Object> apply(Object input) throws Exception {
                functionCalled.countDown();
                functionBlocking.await();
                return immediateFuture(null);
              }
            });

    ExecutorService executor = newSingleThreadExecutor();
    try {
      ListenableFuture<?> output =
          Futures.catchingAsync(
              immediateFailedFuture(new RuntimeException()), Throwable.class, function, executor);
      functionCalled.await();
      assertThat(output.toString()).contains(function.toString());
    } finally {
      functionBlocking.countDown();
      executor.shutdown();
    }
  }

  public void testCatchingAsync_futureToString() throws Exception {
    SettableFuture<Object> toReturn = SettableFuture.create();
    AsyncFunction<Object, Object> function =
        tagged(
            "Called my toString",
            new AsyncFunction<Object, Object>() {
              @Override
              public ListenableFuture<Object> apply(Object input) throws Exception {
                return toReturn;
              }
            });

    ListenableFuture<?> output =
        Futures.catchingAsync(
            immediateFailedFuture(new RuntimeException()),
            Throwable.class,
            function,
            directExecutor());
    assertThat(output.toString()).contains(toReturn.toString());
  }

  // catching tests cloned from the old withFallback tests:

  public void testCatching_inputDoesNotRaiseException() throws Exception {
    Function<Throwable, Integer> fallback = unexpectedFunction();
    ListenableFuture<Integer> originalFuture = immediateFuture(7);
    ListenableFuture<Integer> faultTolerantFuture =
        catching(originalFuture, Throwable.class, fallback, directExecutor());
    assertEquals(7, getDone(faultTolerantFuture).intValue());
  }

  public void testCatching_inputRaisesException() throws Exception {
    RuntimeException raisedException = new RuntimeException();
    FunctionSpy<Throwable, Integer> fallback =
        spy(
            new Function<Throwable, Integer>() {
              @Override
              public Integer apply(Throwable t) {
                assertThat(t).isSameInstanceAs(raisedException);
                return 20;
              }
            });
    ListenableFuture<Integer> failingFuture = immediateFailedFuture(raisedException);
    ListenableFuture<Integer> faultTolerantFuture =
        catching(failingFuture, Throwable.class, fallback, directExecutor());
    assertEquals(20, getDone(faultTolerantFuture).intValue());
    fallback.verifyCallCount(1);
  }

  @J2ktIncompatible
  @GwtIncompatible // non-Throwable exceptionType
  public void testCatching_inputCancelledWithoutFallback() throws Exception {
    Function<IOException, Integer> fallback = unexpectedFunction();
    ListenableFuture<Integer> originalFuture = immediateCancelledFuture();
    ListenableFuture<Integer> faultTolerantFuture =
        catching(originalFuture, IOException.class, fallback, directExecutor());
    assertTrue(faultTolerantFuture.isCancelled());
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
    Error error = new Error("deliberate");
    Function<Throwable, Integer> fallback =
        new Function<Throwable, Integer>() {
          @Override
          public Integer apply(Throwable t) {
            throw error;
          }
        };
    ListenableFuture<Integer> failingFuture = immediateFailedFuture(new RuntimeException());
    ExecutionException expected =
        assertThrows(
            ExecutionException.class,
            () -> getDone(catching(failingFuture, Throwable.class, fallback, directExecutor())));
    assertSame(error, expected.getCause());
  }

  /*
   * catching() uses a plain Function, so there's no testCatching_fallbackReturnsRuntimeException()
   * or testCatching_fallbackReturnsCheckedException().
   */

  private void runExpectedExceptionCatchingTest(RuntimeException expectedException)
      throws Exception {
    FunctionSpy<Throwable, Integer> fallback =
        spy(
            new Function<Throwable, Integer>() {
              @Override
              public Integer apply(Throwable t) {
                throw expectedException;
              }
            });

    ListenableFuture<Integer> failingFuture = immediateFailedFuture(new RuntimeException());

    ListenableFuture<Integer> faultTolerantFuture =
        catching(failingFuture, Throwable.class, fallback, directExecutor());
    try {
      getDone(faultTolerantFuture);
      fail();
    } catch (ExecutionException expected) {
      assertSame(expectedException, expected.getCause());
    }
    fallback.verifyCallCount(1);
  }

  // catching() uses a plain Function, so there's no testCatching_fallbackNotReady().

  public void testCatching_resultInterruptedBeforeFallback() throws Exception {
    SettableFuture<Integer> primary = SettableFuture.create();
    Function<Throwable, Integer> fallback = unexpectedFunction();
    ListenableFuture<Integer> derived =
        catching(primary, Throwable.class, fallback, directExecutor());
    derived.cancel(true);
    assertTrue(primary.isCancelled());
    assertTrue(primary.wasInterrupted());
  }

  public void testCatching_resultCancelledBeforeFallback() throws Exception {
    SettableFuture<Integer> primary = SettableFuture.create();
    Function<Throwable, Integer> fallback = unexpectedFunction();
    ListenableFuture<Integer> derived =
        catching(primary, Throwable.class, fallback, directExecutor());
    derived.cancel(false);
    assertTrue(primary.isCancelled());
    assertFalse(primary.wasInterrupted());
  }

  // catching() uses a plain Function, so there's no testCatching_resultCancelledAfterFallback().

  // catching() uses a plain Function, so there's no testCatching_nullInsteadOfFuture().

  // Some tests of the exceptionType parameter:

  public void testCatching_throwable() throws Exception {
    Function<Throwable, Integer> fallback = functionReturningOne();
    ListenableFuture<Integer> originalFuture = immediateFailedFuture(new IOException());
    ListenableFuture<Integer> faultTolerantFuture =
        catching(originalFuture, Throwable.class, fallback, directExecutor());
    assertEquals(1, (int) getDone(faultTolerantFuture));
  }

  @J2ktIncompatible
  @GwtIncompatible // non-Throwable exceptionType
  public void testCatching_customTypeMatch() throws Exception {
    Function<IOException, Integer> fallback = functionReturningOne();
    ListenableFuture<Integer> originalFuture = immediateFailedFuture(new FileNotFoundException());
    ListenableFuture<Integer> faultTolerantFuture =
        catching(originalFuture, IOException.class, fallback, directExecutor());
    assertEquals(1, (int) getDone(faultTolerantFuture));
  }

  @J2ktIncompatible
  @GwtIncompatible // non-Throwable exceptionType
  public void testCatching_customTypeNoMatch() throws Exception {
    Function<IOException, Integer> fallback = functionReturningOne();
    ListenableFuture<Integer> originalFuture = immediateFailedFuture(new RuntimeException());
    ListenableFuture<Integer> faultTolerantFuture =
        catching(originalFuture, IOException.class, fallback, directExecutor());
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(faultTolerantFuture));
    assertThat(expected).hasCauseThat().isInstanceOf(RuntimeException.class);
  }

  @J2ktIncompatible
  @GwtIncompatible // StackOverflowError
  public void testCatching_stackOverflow() throws Exception {
    SettableFuture<Object> input = SettableFuture.create();
    ListenableFuture<Object> output =
        catching(input, MyException.class, identity(), directExecutor());
    output.addListener(
        () -> {
          throw new StackOverflowError();
        },
        directExecutor());
    assertThrows(StackOverflowError.class, () -> input.setException(new MyException()));
  }

  public void testCatching_errorAfterCancellation() throws Exception {
    class Fallback implements Function<Throwable, Object> {
      @SuppressWarnings("nullness:initialization.field.uninitialized")
      ListenableFuture<Object> output;

      @Override
      public Object apply(Throwable input) {
        output.cancel(false);
        throw new SomeError();
      }
    }
    Fallback fallback = new Fallback();
    SettableFuture<Object> input = SettableFuture.create();

    ListenableFuture<Object> output = catching(input, Throwable.class, fallback, directExecutor());
    fallback.output = output;

    input.setException(new MyException());
    assertTrue(output.isCancelled());
  }

  public void testCatching_exceptionAfterCancellation() throws Exception {
    class Fallback implements Function<Throwable, Object> {
      @SuppressWarnings("nullness:initialization.field.uninitialized")
      ListenableFuture<Object> output;

      @Override
      public Object apply(Throwable input) {
        output.cancel(false);
        throw new SomeUncheckedException();
      }
    }
    Fallback fallback = new Fallback();
    SettableFuture<Object> input = SettableFuture.create();

    ListenableFuture<Object> output = catching(input, Throwable.class, fallback, directExecutor());
    fallback.output = output;

    input.setException(new MyException());
    assertTrue(output.isCancelled());
  }

  public void testCatching_getThrowsRuntimeException() throws Exception {
    ListenableFuture<Object> input =
        UncheckedThrowingFuture.throwingRuntimeException(new SomeUncheckedException());

    // We'd catch only SomeUncheckedException.class here, but then the test won't compile under GWT.
    ListenableFuture<Object> output =
        catching(input, Throwable.class, identity(), directExecutor());
    assertThat(getDone(output)).isInstanceOf(SomeUncheckedException.class);
  }

  public void testCatching_getThrowsError() throws Exception {
    ListenableFuture<Object> input = UncheckedThrowingFuture.throwingError(new SomeError());

    // We'd catch only SomeError.class here, but then the test won't compile under GWT.
    ListenableFuture<Object> output =
        catching(input, Throwable.class, identity(), directExecutor());
    assertThat(getDone(output)).isInstanceOf(SomeError.class);
  }

  public void testCatching_listenerThrowsError() throws Exception {
    SettableFuture<Object> input = SettableFuture.create();
    ListenableFuture<Object> output =
        catching(input, Throwable.class, identity(), directExecutor());

    output.addListener(
        new Runnable() {
          @Override
          public void run() {
            throw new SomeError();
          }
        },
        directExecutor());
    assertThrows(SomeError.class, () -> input.setException(new MyException()));
  }

  public void testCatchingAsync_throwable() throws Exception {
    AsyncFunction<Throwable, Integer> fallback = asyncFunctionReturningOne();
    ListenableFuture<Integer> originalFuture = immediateFailedFuture(new IOException());
    ListenableFuture<Integer> faultTolerantFuture =
        catchingAsync(originalFuture, Throwable.class, fallback, directExecutor());
    assertEquals(1, (int) getDone(faultTolerantFuture));
  }

  @J2ktIncompatible
  @GwtIncompatible // non-Throwable exceptionType
  public void testCatchingAsync_customTypeMatch() throws Exception {
    AsyncFunction<IOException, Integer> fallback = asyncFunctionReturningOne();
    ListenableFuture<Integer> originalFuture = immediateFailedFuture(new FileNotFoundException());
    ListenableFuture<Integer> faultTolerantFuture =
        catchingAsync(originalFuture, IOException.class, fallback, directExecutor());
    assertEquals(1, (int) getDone(faultTolerantFuture));
  }

  @J2ktIncompatible
  @GwtIncompatible // non-Throwable exceptionType
  public void testCatchingAsync_customTypeNoMatch() throws Exception {
    AsyncFunction<IOException, Integer> fallback = asyncFunctionReturningOne();
    ListenableFuture<Integer> originalFuture = immediateFailedFuture(new RuntimeException());
    ListenableFuture<Integer> faultTolerantFuture =
        catchingAsync(originalFuture, IOException.class, fallback, directExecutor());
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(faultTolerantFuture));
    assertThat(expected).hasCauseThat().isInstanceOf(RuntimeException.class);
  }

  @J2ktIncompatible
  @GwtIncompatible // StackOverflowError
  public void testCatchingAsync_stackOverflow() throws Exception {
    SettableFuture<Object> input = SettableFuture.create();
    ListenableFuture<Object> output =
        catchingAsync(input, MyException.class, asyncIdentity(), directExecutor());
    output.addListener(
        () -> {
          throw new StackOverflowError();
        },
        directExecutor());
    assertThrows(StackOverflowError.class, () -> input.setException(new MyException()));
  }

  public void testCatchingAsync_errorAfterCancellation() throws Exception {
    class Fallback implements AsyncFunction<Throwable, Object> {
      @SuppressWarnings("nullness:initialization.field.uninitialized")
      ListenableFuture<Object> output;

      @Override
      public ListenableFuture<Object> apply(Throwable input) {
        output.cancel(false);
        throw new SomeError();
      }
    }
    Fallback fallback = new Fallback();
    SettableFuture<Object> input = SettableFuture.create();

    ListenableFuture<Object> output =
        catchingAsync(input, Throwable.class, fallback, directExecutor());
    fallback.output = output;

    input.setException(new MyException());
    assertTrue(output.isCancelled());
  }

  public void testCatchingAsync_exceptionAfterCancellation() throws Exception {
    class Fallback implements AsyncFunction<Throwable, Object> {
      @SuppressWarnings("nullness:initialization.field.uninitialized")
      ListenableFuture<Object> output;

      @Override
      public ListenableFuture<Object> apply(Throwable input) {
        output.cancel(false);
        throw new SomeUncheckedException();
      }
    }
    Fallback fallback = new Fallback();
    SettableFuture<Object> input = SettableFuture.create();

    ListenableFuture<Object> output =
        catchingAsync(input, Throwable.class, fallback, directExecutor());
    fallback.output = output;

    input.setException(new MyException());
    assertTrue(output.isCancelled());
  }

  public void testCatchingAsync_getThrowsRuntimeException() throws Exception {
    ListenableFuture<Object> input =
        UncheckedThrowingFuture.throwingRuntimeException(new SomeUncheckedException());

    // We'd catch only SomeUncheckedException.class here, but then the test won't compile under GWT.
    ListenableFuture<Object> output =
        catchingAsync(input, Throwable.class, asyncIdentity(), directExecutor());
    assertThat(getDone(output)).isInstanceOf(SomeUncheckedException.class);
  }

  public void testCatchingAsync_getThrowsError() throws Exception {
    ListenableFuture<Object> input = UncheckedThrowingFuture.throwingError(new SomeError());

    // We'd catch only SomeError.class here, but then the test won't compile under GWT.
    ListenableFuture<Object> output =
        catchingAsync(input, Throwable.class, asyncIdentity(), directExecutor());
    assertThat(getDone(output)).isInstanceOf(SomeError.class);
  }

  public void testCatchingAsync_listenerThrowsError() throws Exception {
    SettableFuture<Object> input = SettableFuture.create();
    ListenableFuture<Object> output =
        catchingAsync(input, Throwable.class, asyncIdentity(), directExecutor());

    output.addListener(
        new Runnable() {
          @Override
          public void run() {
            throw new SomeError();
          }
        },
        directExecutor());
    assertThrows(SomeError.class, () -> input.setException(new MyException()));
  }

  public void testCatching_rejectionPropagatesToOutput() throws Exception {
    SettableFuture<String> input = SettableFuture.create();
    ListenableFuture<String> transformed =
        catching(input, Throwable.class, constant("foo"), REJECTING_EXECUTOR);
    input.setException(new Exception());
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(transformed));
    assertThat(expected).hasCauseThat().isInstanceOf(RejectedExecutionException.class);
  }

  public void testCatchingAsync_rejectionPropagatesToOutput() throws Exception {
    SettableFuture<String> input = SettableFuture.create();
    ListenableFuture<String> transformed =
        catchingAsync(
            input,
            Throwable.class,
            constantAsyncFunction(immediateFuture("foo")),
            REJECTING_EXECUTOR);
    input.setException(new Exception());
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(transformed));
    assertThat(expected).hasCauseThat().isInstanceOf(RejectedExecutionException.class);
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
      @Nullable ListenableFuture<O> output) {
    return new AsyncFunction<I, O>() {
      @Override
      public ListenableFuture<O> apply(I input) {
        return output;
      }
    };
  }

  @J2ktIncompatible // Wildcard generics
  public void testTransformAsync_genericsWildcard_asyncFunction() throws Exception {
    ListenableFuture<?> nullFuture = immediateFuture(null);
    ListenableFuture<?> chainedFuture =
        transformAsync(nullFuture, constantAsyncFunction(nullFuture), directExecutor());
    assertNull(getDone(chainedFuture));
  }

  @J2ktIncompatible // TODO(b/324550390): Enable
  public void testTransformAsync_genericsHierarchy_asyncFunction() throws Exception {
    ListenableFuture<FooChild> future = immediateFuture(null);
    BarChild barChild = new BarChild();
    AsyncFunction<Foo, BarChild> function =
        new AsyncFunction<Foo, BarChild>() {
          @Override
          public AbstractFuture<BarChild> apply(Foo unused) {
            AbstractFuture<BarChild> future = new AbstractFuture<BarChild>() {};
            future.set(barChild);
            return future;
          }
        };
    Bar bar = getDone(transformAsync(future, function, directExecutor()));
    assertSame(barChild, bar);
  }

  @J2ktIncompatible
  @GwtIncompatible // get() timeout
  public void testTransformAsync_asyncFunction_timeout()
      throws InterruptedException, ExecutionException {
    AsyncFunction<String, Integer> function = constantAsyncFunction(immediateFuture(1));
    ListenableFuture<Integer> future =
        transformAsync(SettableFuture.<String>create(), function, directExecutor());
    assertThrows(TimeoutException.class, () -> future.get(1, MILLISECONDS));
  }

  public void testTransformAsync_asyncFunction_error() throws InterruptedException {
    Error error = new Error("deliberate");
    AsyncFunction<String, Integer> function =
        new AsyncFunction<String, Integer>() {
          @Override
          public ListenableFuture<Integer> apply(String input) {
            throw error;
          }
        };
    SettableFuture<String> inputFuture = SettableFuture.create();
    ListenableFuture<Integer> outputFuture =
        transformAsync(inputFuture, function, directExecutor());
    inputFuture.set("value");
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(outputFuture));
    assertSame(error, expected.getCause());
  }

  @J2ktIncompatible // Nullability
  public void testTransformAsync_asyncFunction_nullInsteadOfFuture() throws Exception {
    ListenableFuture<?> inputFuture = immediateFuture("a");
    ListenableFuture<?> chainedFuture =
        transformAsync(inputFuture, constantAsyncFunction(null), directExecutor());
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(chainedFuture));
    NullPointerException cause = (NullPointerException) expected.getCause();
    assertThat(cause)
        .hasMessageThat()
        .contains(
            "AsyncFunction.apply returned null instead of a Future. "
                + "Did you mean to return immediateFuture(null)?");
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testTransformAsync_asyncFunction_cancelledWhileApplyingFunction()
      throws InterruptedException, ExecutionException {
    CountDownLatch inFunction = new CountDownLatch(1);
    CountDownLatch functionDone = new CountDownLatch(1);
    SettableFuture<Integer> resultFuture = SettableFuture.create();
    AsyncFunction<String, Integer> function =
        new AsyncFunction<String, Integer>() {
          @Override
          public ListenableFuture<Integer> apply(String input) throws Exception {
            inFunction.countDown();
            functionDone.await();
            return resultFuture;
          }
        };
    SettableFuture<String> inputFuture = SettableFuture.create();
    ExecutorService service = newSingleThreadExecutor();
    ListenableFuture<Integer> future = transformAsync(inputFuture, function, service);
    inputFuture.set("value");
    inFunction.await();
    future.cancel(false);
    functionDone.countDown();
    assertThrows(CancellationException.class, () -> future.get());
    assertThrows(CancellationException.class, () -> resultFuture.get());
    service.shutdown();
    service.awaitTermination(30, SECONDS);
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testTransformAsync_asyncFunction_cancelledBeforeApplyingFunction()
      throws InterruptedException {
    AtomicBoolean functionCalled = new AtomicBoolean();
    AsyncFunction<String, Integer> function =
        new AsyncFunction<String, Integer>() {
          @Override
          public ListenableFuture<Integer> apply(String input) throws Exception {
            functionCalled.set(true);
            return immediateFuture(1);
          }
        };
    SettableFuture<String> inputFuture = SettableFuture.create();
    ExecutorService executor = newSingleThreadExecutor();
    ListenableFuture<Integer> future = transformAsync(inputFuture, function, executor);

    // Pause the executor.
    CountDownLatch beforeFunction = new CountDownLatch(1);
    executor.execute(
        new Runnable() {
          @Override
          public void run() {
            awaitUninterruptibly(beforeFunction);
          }
        });

    // Cancel the future after making input available.
    inputFuture.set("value");
    future.cancel(false);

    // Unpause the executor.
    beforeFunction.countDown();
    executor.shutdown();
    assertTrue(executor.awaitTermination(5, SECONDS));

    assertFalse(functionCalled.get());
  }

  public void testSubmitAsync_asyncCallable_error() throws InterruptedException {
    Error error = new Error("deliberate");
    AsyncCallable<Integer> callable =
        new AsyncCallable<Integer>() {
          @Override
          public ListenableFuture<Integer> call() {
            throw error;
          }
        };
    SettableFuture<String> inputFuture = SettableFuture.create();
    ListenableFuture<Integer> outputFuture = submitAsync(callable, directExecutor());
    inputFuture.set("value");
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(outputFuture));
    assertSame(error, expected.getCause());
  }

  @J2ktIncompatible // TODO(b/324550390): Enable
  public void testSubmitAsync_asyncCallable_nullInsteadOfFuture() throws Exception {
    ListenableFuture<?> chainedFuture = submitAsync(constantAsyncCallable(null), directExecutor());
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(chainedFuture));
    NullPointerException cause = (NullPointerException) expected.getCause();
    assertThat(cause)
        .hasMessageThat()
        .contains(
            "AsyncCallable.call returned null instead of a Future. "
                + "Did you mean to return immediateFuture(null)?");
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testSubmitAsync_asyncCallable_cancelledWhileApplyingFunction()
      throws InterruptedException, ExecutionException {
    CountDownLatch inFunction = new CountDownLatch(1);
    CountDownLatch callableDone = new CountDownLatch(1);
    SettableFuture<Integer> resultFuture = SettableFuture.create();
    AsyncCallable<Integer> callable =
        new AsyncCallable<Integer>() {
          @Override
          public ListenableFuture<Integer> call() throws InterruptedException {
            inFunction.countDown();
            callableDone.await();
            return resultFuture;
          }
        };
    SettableFuture<String> inputFuture = SettableFuture.create();
    ExecutorService service = newSingleThreadExecutor();
    ListenableFuture<Integer> future = submitAsync(callable, service);
    inputFuture.set("value");
    inFunction.await();
    future.cancel(false);
    callableDone.countDown();
    assertThrows(CancellationException.class, () -> future.get());
    assertThrows(CancellationException.class, () -> resultFuture.get());
    service.shutdown();
    service.awaitTermination(30, SECONDS);
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testSubmitAsync_asyncCallable_cancelledBeforeApplyingFunction()
      throws InterruptedException {
    AtomicBoolean callableCalled = new AtomicBoolean();
    AsyncCallable<Integer> callable =
        new AsyncCallable<Integer>() {
          @Override
          public ListenableFuture<Integer> call() {
            callableCalled.set(true);
            return immediateFuture(1);
          }
        };
    ExecutorService executor = newSingleThreadExecutor();
    // Pause the executor.
    CountDownLatch beforeFunction = new CountDownLatch(1);
    executor.execute(
        new Runnable() {
          @Override
          public void run() {
            awaitUninterruptibly(beforeFunction);
          }
        });
    ListenableFuture<Integer> future = submitAsync(callable, executor);
    future.cancel(false);

    // Unpause the executor.
    beforeFunction.countDown();
    executor.shutdown();
    assertTrue(executor.awaitTermination(5, SECONDS));

    assertFalse(callableCalled.get());
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testSubmitAsync_asyncCallable_returnsInterruptedFuture() throws InterruptedException {
    assertThat(Thread.interrupted()).isFalse();
    SettableFuture<Integer> cancelledFuture = SettableFuture.create();
    cancelledFuture.cancel(true);
    assertThat(Thread.interrupted()).isFalse();
    ListenableFuture<Integer> future =
        submitAsync(constantAsyncCallable(cancelledFuture), directExecutor());
    assertThat(future.isDone()).isTrue();
    assertThat(Thread.interrupted()).isFalse();
  }

  public void testSubmit_callable_returnsValue() throws Exception {
    Callable<Integer> callable =
        new Callable<Integer>() {
          @Override
          public Integer call() {
            return 42;
          }
        };
    ListenableFuture<Integer> future = submit(callable, directExecutor());
    assertThat(future.isDone()).isTrue();
    assertThat(getDone(future)).isEqualTo(42);
  }

  public void testSubmit_callable_throwsException() {
    Exception exception = new Exception("Exception for testing");
    Callable<Integer> callable =
        new Callable<Integer>() {
          @Override
          public Integer call() throws Exception {
            throw exception;
          }
        };
    ListenableFuture<Integer> future = submit(callable, directExecutor());
    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(future));
    assertThat(expected).hasCauseThat().isSameInstanceAs(exception);
  }

  public void testSubmit_runnable_completesAfterRun() throws Exception {
    List<Runnable> pendingRunnables = new ArrayList<>();
    List<Runnable> executedRunnables = new ArrayList<>();
    Runnable runnable =
        new Runnable() {
          @Override
          public void run() {
            executedRunnables.add(this);
          }
        };
    Executor executor =
        new Executor() {
          @Override
          public void execute(Runnable runnable) {
            pendingRunnables.add(runnable);
          }
        };
    ListenableFuture<@Nullable Void> future = submit(runnable, executor);
    assertThat(future.isDone()).isFalse();
    assertThat(executedRunnables).isEmpty();
    assertThat(pendingRunnables).hasSize(1);
    pendingRunnables.remove(0).run();
    assertThat(future.isDone()).isTrue();
    assertThat(executedRunnables).containsExactly(runnable);
    assertThat(pendingRunnables).isEmpty();
  }

  public void testSubmit_runnable_throwsException() throws Exception {
    RuntimeException exception = new RuntimeException("Exception for testing");
    Runnable runnable =
        new Runnable() {
          @Override
          public void run() {
            throw exception;
          }
        };
    ListenableFuture<@Nullable Void> future = submit(runnable, directExecutor());
    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(future));
    assertThat(expected).hasCauseThat().isSameInstanceAs(exception);
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testScheduleAsync_asyncCallable_error() throws InterruptedException {
    Error error = new Error("deliberate");
    AsyncCallable<Integer> callable =
        new AsyncCallable<Integer>() {
          @Override
          public ListenableFuture<Integer> call() {
            throw error;
          }
        };
    SettableFuture<String> inputFuture = SettableFuture.create();
    ListenableFuture<Integer> outputFuture = submitAsync(callable, directExecutor());
    inputFuture.set("value");
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(outputFuture));
    assertSame(error, expected.getCause());
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testScheduleAsync_asyncCallable_nullInsteadOfFuture() throws Exception {
    ExecutorService service = newSingleThreadScheduledExecutor();
    ListenableFuture<?> chainedFuture =
        scheduleAsync(
            constantAsyncCallable(null), 1, NANOSECONDS, newSingleThreadScheduledExecutor());
    ExecutionException expected = assertThrows(ExecutionException.class, () -> chainedFuture.get());
    NullPointerException cause = (NullPointerException) expected.getCause();
    assertThat(cause)
        .hasMessageThat()
        .contains(
            "AsyncCallable.call returned null instead of a Future. "
                + "Did you mean to return immediateFuture(null)?");
    service.shutdown();
    service.awaitTermination(30, SECONDS);
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testScheduleAsync_asyncCallable_cancelledWhileApplyingFunction()
      throws InterruptedException, ExecutionException {
    CountDownLatch inFunction = new CountDownLatch(1);
    CountDownLatch callableDone = new CountDownLatch(1);
    SettableFuture<Integer> resultFuture = SettableFuture.create();
    AsyncCallable<Integer> callable =
        new AsyncCallable<Integer>() {
          @Override
          public ListenableFuture<Integer> call() throws InterruptedException {
            inFunction.countDown();
            callableDone.await();
            return resultFuture;
          }
        };
    ScheduledExecutorService service = newSingleThreadScheduledExecutor();
    ListenableFuture<Integer> future = scheduleAsync(callable, 1, NANOSECONDS, service);
    inFunction.await();
    future.cancel(false);
    callableDone.countDown();
    assertThrows(CancellationException.class, () -> future.get());
    assertThrows(CancellationException.class, () -> resultFuture.get());
    service.shutdown();
    service.awaitTermination(30, SECONDS);
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testScheduleAsync_asyncCallable_cancelledBeforeCallingFunction()
      throws InterruptedException {
    AtomicBoolean callableCalled = new AtomicBoolean();
    AsyncCallable<Integer> callable =
        new AsyncCallable<Integer>() {
          @Override
          public ListenableFuture<Integer> call() {
            callableCalled.set(true);
            return immediateFuture(1);
          }
        };
    ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
    // Pause the executor.
    CountDownLatch beforeFunction = new CountDownLatch(1);
    executor.execute(
        new Runnable() {
          @Override
          public void run() {
            awaitUninterruptibly(beforeFunction);
          }
        });
    ListenableFuture<Integer> future = scheduleAsync(callable, 1, NANOSECONDS, executor);
    future.cancel(false);

    // Unpause the executor.
    beforeFunction.countDown();
    executor.shutdown();
    assertTrue(executor.awaitTermination(5, SECONDS));

    assertFalse(callableCalled.get());
  }

  private static <T> AsyncCallable<T> constantAsyncCallable(
      @Nullable ListenableFuture<T> returnValue) {
    return new AsyncCallable<T>() {
      @Override
      public ListenableFuture<T> call() {
        return returnValue;
      }
    };
  }

  /** Runnable which can be called a single time, and only after {@link #expectCall} is called. */
  // TODO(cpovirk): top-level class?
  private static class SingleCallListener implements Runnable {

    private boolean expectCall = false;
    private final AtomicBoolean called = new AtomicBoolean();

    @Override
    public void run() {
      assertTrue("Listener called before it was expected", expectCall);
      assertFalse("Listener called more than once", wasCalled());
      called.set(true);
    }

    void expectCall() {
      assertFalse("expectCall is already true", expectCall);
      expectCall = true;
    }

    boolean wasCalled() {
      return called.get();
    }
  }

  public void testAllAsList() throws Exception {
    // Create input and output
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    SettableFuture<String> future3 = SettableFuture.create();
    ListenableFuture<List<String>> compound = allAsList(future1, future2, future3);

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
    assertTrue(listener.wasCalled());

    List<String> results = getDone(compound);
    assertThat(results).containsExactly(DATA1, DATA2, DATA3).inOrder();
  }

  public void testAllAsList_emptyList() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    List<ListenableFuture<String>> futures = ImmutableList.of();
    ListenableFuture<List<String>> compound = allAsList(futures);
    compound.addListener(listener, directExecutor());
    assertThat(getDone(compound)).isEmpty();
    assertTrue(listener.wasCalled());
  }

  public void testAllAsList_emptyArray() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    ListenableFuture<List<String>> compound = allAsList();
    compound.addListener(listener, directExecutor());
    assertThat(getDone(compound)).isEmpty();
    assertTrue(listener.wasCalled());
  }

  public void testAllAsList_failure() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound = allAsList(future1, future2);
    compound.addListener(listener, directExecutor());

    listener.expectCall();
    Throwable exception = new Throwable("failed1");
    future1.setException(exception);
    assertTrue(compound.isDone());
    assertTrue(listener.wasCalled());
    assertFalse(future2.isDone());

    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(compound));
    assertSame(exception, expected.getCause());
  }

  public void testAllAsList_singleFailure() throws Exception {
    Throwable exception = new Throwable("failed");
    ListenableFuture<String> future = immediateFailedFuture(exception);
    ListenableFuture<List<String>> compound = allAsList(ImmutableList.of(future));

    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(compound));
    assertSame(exception, expected.getCause());
  }

  public void testAllAsList_immediateFailure() throws Exception {
    Throwable exception = new Throwable("failed");
    ListenableFuture<String> future1 = immediateFailedFuture(exception);
    ListenableFuture<String> future2 = immediateFuture("results");
    ListenableFuture<List<String>> compound = allAsList(ImmutableList.of(future1, future2));

    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(compound));
    assertSame(exception, expected.getCause());
  }

  public void testAllAsList_error() throws Exception {
    Error error = new Error("deliberate");
    SettableFuture<String> future1 = SettableFuture.create();
    ListenableFuture<String> future2 = immediateFuture("results");
    ListenableFuture<List<String>> compound = allAsList(ImmutableList.of(future1, future2));

    future1.setException(error);
    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(compound));
    assertSame(error, expected.getCause());
  }

  public void testAllAsList_cancelled() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound = allAsList(future1, future2);
    compound.addListener(listener, directExecutor());

    listener.expectCall();
    future1.cancel(true);
    assertTrue(compound.isDone());
    assertTrue(listener.wasCalled());
    assertFalse(future2.isDone());

    assertThrows(CancellationException.class, () -> getDone(compound));
  }

  public void testAllAsList_resultCancelled() throws Exception {
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound = allAsList(future1, future2);

    future2.set(DATA2);
    assertFalse(compound.isDone());
    assertTrue(compound.cancel(false));
    assertTrue(compound.isCancelled());
    assertTrue(future1.isCancelled());
    assertFalse(future1.wasInterrupted());
  }

  public void testAllAsList_resultCancelledInterrupted_withSecondaryListFuture() throws Exception {
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound = allAsList(future1, future2);
    // There was a bug where the event listener for the combined future would
    // result in the sub-futures being cancelled without being interrupted.
    ListenableFuture<List<String>> otherCompound = allAsList(future1, future2);

    assertTrue(compound.cancel(true));
    assertTrue(future1.isCancelled());
    assertTrue(future1.wasInterrupted());
    assertTrue(future2.isCancelled());
    assertTrue(future2.wasInterrupted());
    assertTrue(otherCompound.isCancelled());
  }

  public void testAllAsList_resultCancelled_withSecondaryListFuture() throws Exception {
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound = allAsList(future1, future2);
    // This next call is "unused," but it is an important part of the test. Don't remove it!
    ListenableFuture<List<String>> unused = allAsList(future1, future2);

    assertTrue(compound.cancel(false));
    assertTrue(future1.isCancelled());
    assertFalse(future1.wasInterrupted());
    assertTrue(future2.isCancelled());
    assertFalse(future2.wasInterrupted());
  }

  public void testAllAsList_resultInterrupted() throws Exception {
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound = allAsList(future1, future2);

    future2.set(DATA2);
    assertFalse(compound.isDone());
    assertTrue(compound.cancel(true));
    assertTrue(compound.isCancelled());
    assertTrue(future1.isCancelled());
    assertTrue(future1.wasInterrupted());
  }

  /**
   * Test the case where the futures are fulfilled prior to constructing the ListFuture. There was a
   * bug where the loop that connects a Listener to each of the futures would die on the last
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

    ListenableFuture<List<String>> compound = allAsList(future1, future2, future3);

    // Attach a listener
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    compound.addListener(listener, directExecutor());

    assertTrue(listener.wasCalled());

    List<String> results = getDone(compound);
    assertThat(results).containsExactly(DATA1, DATA2, DATA3).inOrder();
  }

  /** A single non-error failure is not logged because it is reported via the output future. */
  public void testAllAsList_logging_exception() throws Exception {
    ExecutionException expected =
        assertThrows(
            ExecutionException.class,
            () -> getDone(allAsList(immediateFailedFuture(new MyException()))));
    assertThat(expected).hasCauseThat().isInstanceOf(MyException.class);
    assertEquals(
        "Nothing should be logged", 0, aggregateFutureLogHandler.getStoredLogRecords().size());
  }

  /** Ensure that errors are always logged. */
  public void testAllAsList_logging_error() throws Exception {
    ExecutionException expected =
        assertThrows(
            ExecutionException.class,
            () -> getDone(allAsList(immediateFailedFuture(new SomeError()))));
    assertThat(expected).hasCauseThat().isInstanceOf(SomeError.class);
    List<LogRecord> logged = aggregateFutureLogHandler.getStoredLogRecords();
    assertThat(logged).hasSize(1); // errors are always logged
    assertThat(logged.get(0).getThrown()).isInstanceOf(SomeError.class);
  }

  /** All as list will log extra exceptions that have already occurred. */
  public void testAllAsList_logging_multipleExceptions_alreadyDone() throws Exception {
    ExecutionException expected =
        assertThrows(
            ExecutionException.class,
            () ->
                getDone(
                    allAsList(
                        immediateFailedFuture(new MyException()),
                        immediateFailedFuture(new MyException()))));
    assertThat(expected).hasCauseThat().isInstanceOf(MyException.class);
    List<LogRecord> logged = aggregateFutureLogHandler.getStoredLogRecords();
    assertThat(logged).hasSize(1); // the second failure is logged
    assertThat(logged.get(0).getThrown()).isInstanceOf(MyException.class);
  }

  /** All as list will log extra exceptions that occur later. */
  public void testAllAsList_logging_multipleExceptions_doneLater() throws Exception {
    SettableFuture<Object> future1 = SettableFuture.create();
    SettableFuture<Object> future2 = SettableFuture.create();
    SettableFuture<Object> future3 = SettableFuture.create();
    ListenableFuture<List<Object>> all = allAsList(future1, future2, future3);

    future1.setException(new MyException());
    future2.setException(new MyException());
    future3.setException(new MyException());

    assertThrows(ExecutionException.class, () -> getDone(all));
    List<LogRecord> logged = aggregateFutureLogHandler.getStoredLogRecords();
    assertThat(logged).hasSize(2); // failures after the first are logged
    assertThat(logged.get(0).getThrown()).isInstanceOf(MyException.class);
    assertThat(logged.get(1).getThrown()).isInstanceOf(MyException.class);
  }

  /** The same exception happening on multiple futures should not be logged. */
  public void testAllAsList_logging_same_exception() throws Exception {
    ExecutionException expected =
        assertThrows(
            ExecutionException.class,
            () -> {
              MyException sameInstance = new MyException();
              getDone(
                  allAsList(
                      immediateFailedFuture(sameInstance), immediateFailedFuture(sameInstance)));
            });
    assertThat(expected).hasCauseThat().isInstanceOf(MyException.class);
    assertEquals(
        "Nothing should be logged", 0, aggregateFutureLogHandler.getStoredLogRecords().size());
  }

  public void testAllAsList_logging_seenExceptionUpdateRace() throws Exception {
    MyException sameInstance = new MyException();
    SettableFuture<Object> firstFuture = SettableFuture.create();
    SettableFuture<Object> secondFuture = SettableFuture.create();
    ListenableFuture<List<Object>> bulkFuture = allAsList(firstFuture, secondFuture);

    bulkFuture.addListener(
        new Runnable() {
          @Override
          public void run() {
            /*
             * firstFuture just completed, but AggregateFuture hasn't yet had time to record the
             * exception in seenExceptions. When we complete secondFuture with the same exception,
             * we want for AggregateFuture to still detect that it's been previously seen.
             */
            secondFuture.setException(sameInstance);
          }
        },
        directExecutor());
    firstFuture.setException(sameInstance);

    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(bulkFuture));
    assertThat(expected).hasCauseThat().isInstanceOf(MyException.class);
    assertThat(aggregateFutureLogHandler.getStoredLogRecords()).isEmpty();
  }

  public void testAllAsList_logging_seenExceptionUpdateCancelRace() throws Exception {
    MyException subsequentFailure = new MyException();
    SettableFuture<Object> firstFuture = SettableFuture.create();
    SettableFuture<Object> secondFuture = SettableFuture.create();
    ListenableFuture<List<Object>> bulkFuture = allAsList(firstFuture, secondFuture);

    bulkFuture.addListener(
        new Runnable() {
          @Override
          public void run() {
            /*
             * This is similar to the above test, but this time we're making sure that we recognize
             * that the output Future is done early not because of an exception but because of a
             * cancellation.
             */
            secondFuture.setException(subsequentFailure);
          }
        },
        directExecutor());
    firstFuture.cancel(false);

    assertThrows(CancellationException.class, () -> getDone(bulkFuture));
    assertThat(getOnlyElement(aggregateFutureLogHandler.getStoredLogRecords()).getThrown())
        .isSameInstanceAs(subsequentFailure);
  }

  /**
   * Different exceptions happening on multiple futures with the same cause should not be logged.
   */
  public void testAllAsList_logging_same_cause() throws Exception {
    ExecutionException expected =
        assertThrows(
            ExecutionException.class,
            () -> {
              MyException exception1 = new MyException();
              MyException exception2 = new MyException();
              MyException exception3 = new MyException();

              MyException sameInstance = new MyException();
              exception1.initCause(sameInstance);
              exception2.initCause(sameInstance);
              exception3.initCause(exception2);
              getDone(
                  allAsList(immediateFailedFuture(exception1), immediateFailedFuture(exception3)));
            });
    assertThat(expected).hasCauseThat().isInstanceOf(MyException.class);
    assertEquals(
        "Nothing should be logged", 0, aggregateFutureLogHandler.getStoredLogRecords().size());
  }

  private static String createCombinedResult(Integer i, Boolean b) {
    return "-" + i + "-" + b;
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testWhenAllComplete_noLeakInterruption() throws Exception {
    SettableFuture<String> stringFuture = SettableFuture.create();
    AsyncCallable<String> combiner =
        new AsyncCallable<String>() {
          @Override
          public ListenableFuture<String> call() throws Exception {
            return stringFuture;
          }
        };

    ListenableFuture<String> futureResult = whenAllComplete().callAsync(combiner, directExecutor());

    assertThat(Thread.interrupted()).isFalse();
    futureResult.cancel(true);
    assertThat(Thread.interrupted()).isFalse();
  }

  @J2ktIncompatible // Wildcard generics
  public void testWhenAllComplete_wildcard() throws Exception {
    ListenableFuture<?> futureA = immediateFuture("a");
    ListenableFuture<?> futureB = immediateFuture("b");
    ListenableFuture<?>[] futures = new ListenableFuture<?>[0];
    Callable<String> combiner =
        new Callable<String>() {
          @Override
          public String call() throws Exception {
            return "hi";
          }
        };

    // We'd like for all the following to compile.
    ListenableFuture<String> unused;

    // Compiles:
    unused = whenAllComplete(futureA, futureB).call(combiner, directExecutor());

    // Does not compile:
    // unused = whenAllComplete(futures).call(combiner);

    // Workaround for the above:
    unused = whenAllComplete(asList(futures)).call(combiner, directExecutor());
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testWhenAllComplete_asyncResult() throws Exception {
    SettableFuture<Integer> futureInteger = SettableFuture.create();
    SettableFuture<Boolean> futureBoolean = SettableFuture.create();

    ExecutorService executor = newSingleThreadExecutor();
    CountDownLatch callableBlocking = new CountDownLatch(1);
    SettableFuture<String> resultOfCombiner = SettableFuture.create();
    AsyncCallable<String> combiner =
        tagged(
            "Called my toString",
            new AsyncCallable<String>() {
              @Override
              public ListenableFuture<String> call() throws Exception {
                // Make this executor terminate after this task so that the test can tell when
                // futureResult has received resultOfCombiner.
                executor.shutdown();
                callableBlocking.await();
                return resultOfCombiner;
              }
            });

    ListenableFuture<String> futureResult =
        whenAllComplete(futureInteger, futureBoolean).callAsync(combiner, executor);

    // Waiting on backing futures
    assertThat(futureResult.toString())
        .matches(
            "CombinedFuture@\\w+\\[status=PENDING,"
                + " info=\\[futures=\\[SettableFuture@\\w+\\[status=PENDING],"
                + " SettableFuture@\\w+\\[status=PENDING]]]]");
    Integer integerPartial = 1;
    futureInteger.set(integerPartial);
    assertThat(futureResult.toString())
        .matches(
            "CombinedFuture@\\w+\\[status=PENDING,"
                + " info=\\[futures=\\[SettableFuture@\\w+\\[status=SUCCESS,"
                + " result=\\[java.lang.Integer@\\w+]], SettableFuture@\\w+\\[status=PENDING]]]]");

    // Backing futures complete
    Boolean booleanPartial = true;
    futureBoolean.set(booleanPartial);
    // Once the backing futures are done there's a (brief) moment where we know nothing
    assertThat(futureResult.toString()).matches("CombinedFuture@\\w+\\[status=PENDING]");
    callableBlocking.countDown();
    // Need to wait for resultFuture to be returned.
    assertTrue(executor.awaitTermination(10, SECONDS));
    // But once the async function has returned a future we can include that in the toString
    assertThat(futureResult.toString())
        .matches(
            "CombinedFuture@\\w+\\[status=PENDING,"
                + " setFuture=\\[SettableFuture@\\w+\\[status=PENDING]]]");

    // Future complete
    resultOfCombiner.set(createCombinedResult(getDone(futureInteger), getDone(futureBoolean)));
    String expectedResult = createCombinedResult(integerPartial, booleanPartial);
    assertEquals(expectedResult, futureResult.get());
    assertThat(futureResult.toString())
        .matches("CombinedFuture@\\w+\\[status=SUCCESS, result=\\[java.lang.String@\\w+]]");
  }

  public void testWhenAllComplete_asyncError() throws Exception {
    Exception thrown = new RuntimeException("test");

    SettableFuture<Integer> futureInteger = SettableFuture.create();
    SettableFuture<Boolean> futureBoolean = SettableFuture.create();
    AsyncCallable<String> combiner =
        new AsyncCallable<String>() {
          @Override
          public ListenableFuture<String> call() throws Exception {
            assertTrue(futureInteger.isDone());
            assertTrue(futureBoolean.isDone());
            return immediateFailedFuture(thrown);
          }
        };

    ListenableFuture<String> futureResult =
        whenAllComplete(futureInteger, futureBoolean).callAsync(combiner, directExecutor());
    Integer integerPartial = 1;
    futureInteger.set(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.set(booleanPartial);

    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(futureResult));
    assertSame(thrown, expected.getCause());
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testWhenAllComplete_cancelledNotInterrupted() throws Exception {
    SettableFuture<String> stringFuture = SettableFuture.create();
    SettableFuture<Boolean> booleanFuture = SettableFuture.create();
    CountDownLatch inFunction = new CountDownLatch(1);
    CountDownLatch shouldCompleteFunction = new CountDownLatch(1);
    SettableFuture<String> resultFuture = SettableFuture.create();
    AsyncCallable<String> combiner =
        new AsyncCallable<String>() {
          @Override
          public ListenableFuture<String> call() throws Exception {
            inFunction.countDown();
            shouldCompleteFunction.await();
            return resultFuture;
          }
        };

    ExecutorService service = newSingleThreadExecutor();
    ListenableFuture<String> futureResult =
        whenAllComplete(stringFuture, booleanFuture).callAsync(combiner, service);

    stringFuture.set("value");
    booleanFuture.set(true);
    inFunction.await();
    futureResult.cancel(false);
    shouldCompleteFunction.countDown();
    assertThrows(CancellationException.class, () -> futureResult.get());

    assertThrows(CancellationException.class, () -> resultFuture.get());
    service.shutdown();
    service.awaitTermination(30, SECONDS);
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testWhenAllComplete_interrupted() throws Exception {
    SettableFuture<String> stringFuture = SettableFuture.create();
    SettableFuture<Boolean> booleanFuture = SettableFuture.create();
    CountDownLatch inFunction = new CountDownLatch(1);
    CountDownLatch gotException = new CountDownLatch(1);
    AsyncCallable<String> combiner =
        new AsyncCallable<String>() {
          @Override
          public ListenableFuture<String> call() throws Exception {
            inFunction.countDown();
            try {
              new CountDownLatch(1).await(); // wait for interrupt
            } catch (InterruptedException expected) {
              gotException.countDown();
              throw expected;
            }
            return immediateFuture("a");
          }
        };

    ExecutorService service = newSingleThreadExecutor();
    ListenableFuture<String> futureResult =
        whenAllComplete(stringFuture, booleanFuture).callAsync(combiner, service);

    stringFuture.set("value");
    booleanFuture.set(true);
    inFunction.await();
    futureResult.cancel(true);
    assertThrows(CancellationException.class, () -> futureResult.get());
    gotException.await();
    service.shutdown();
    service.awaitTermination(30, SECONDS);
  }

  public void testWhenAllComplete_runnableResult() throws Exception {
    SettableFuture<Integer> futureInteger = SettableFuture.create();
    SettableFuture<Boolean> futureBoolean = SettableFuture.create();
    String[] result = new String[1];
    Runnable combiner =
        new Runnable() {
          @Override
          public void run() {
            assertTrue(futureInteger.isDone());
            assertTrue(futureBoolean.isDone());
            result[0] =
                createCombinedResult(
                    Futures.getUnchecked(futureInteger), Futures.getUnchecked(futureBoolean));
          }
        };

    ListenableFuture<?> futureResult =
        whenAllComplete(futureInteger, futureBoolean).run(combiner, directExecutor());
    Integer integerPartial = 1;
    futureInteger.set(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.set(booleanPartial);
    futureResult.get();
    assertEquals(createCombinedResult(integerPartial, booleanPartial), result[0]);
  }

  public void testWhenAllComplete_runnableError() throws Exception {
    RuntimeException thrown = new RuntimeException("test");

    SettableFuture<Integer> futureInteger = SettableFuture.create();
    SettableFuture<Boolean> futureBoolean = SettableFuture.create();
    Runnable combiner =
        new Runnable() {
          @Override
          public void run() {
            assertTrue(futureInteger.isDone());
            assertTrue(futureBoolean.isDone());
            throw thrown;
          }
        };

    ListenableFuture<?> futureResult =
        whenAllComplete(futureInteger, futureBoolean).run(combiner, directExecutor());
    Integer integerPartial = 1;
    futureInteger.set(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.set(booleanPartial);

    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(futureResult));
    assertSame(thrown, expected.getCause());
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testWhenAllCompleteRunnable_resultCanceledWithoutInterrupt_doesNotInterruptRunnable()
      throws Exception {
    SettableFuture<String> stringFuture = SettableFuture.create();
    SettableFuture<Boolean> booleanFuture = SettableFuture.create();
    CountDownLatch inFunction = new CountDownLatch(1);
    CountDownLatch shouldCompleteFunction = new CountDownLatch(1);
    CountDownLatch combinerCompletedWithoutInterrupt = new CountDownLatch(1);
    Runnable combiner =
        new Runnable() {
          @Override
          public void run() {
            inFunction.countDown();
            try {
              shouldCompleteFunction.await();
              combinerCompletedWithoutInterrupt.countDown();
            } catch (InterruptedException e) {
              // Ensure the thread's interrupt status is preserved.
              Thread.currentThread().interrupt();
              throw new RuntimeException(e);
            }
          }
        };

    ExecutorService service = newSingleThreadExecutor();
    ListenableFuture<?> futureResult =
        whenAllComplete(stringFuture, booleanFuture).run(combiner, service);

    stringFuture.set("value");
    booleanFuture.set(true);
    inFunction.await();
    futureResult.cancel(false);
    shouldCompleteFunction.countDown();
    assertThrows(CancellationException.class, () -> futureResult.get());
    combinerCompletedWithoutInterrupt.await();
    service.shutdown();
    service.awaitTermination(30, SECONDS);
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testWhenAllCompleteRunnable_resultCanceledWithInterrupt_interruptsRunnable()
      throws Exception {
    SettableFuture<String> stringFuture = SettableFuture.create();
    SettableFuture<Boolean> booleanFuture = SettableFuture.create();
    CountDownLatch inFunction = new CountDownLatch(1);
    CountDownLatch gotException = new CountDownLatch(1);
    Runnable combiner =
        new Runnable() {
          @Override
          public void run() {
            inFunction.countDown();
            try {
              new CountDownLatch(1).await(); // wait for interrupt
            } catch (InterruptedException expected) {
              // Ensure the thread's interrupt status is preserved.
              Thread.currentThread().interrupt();
              gotException.countDown();
            }
          }
        };

    ExecutorService service = newSingleThreadExecutor();
    ListenableFuture<?> futureResult =
        whenAllComplete(stringFuture, booleanFuture).run(combiner, service);

    stringFuture.set("value");
    booleanFuture.set(true);
    inFunction.await();
    futureResult.cancel(true);
    assertThrows(CancellationException.class, () -> futureResult.get());
    gotException.await();
    service.shutdown();
    service.awaitTermination(30, SECONDS);
  }

  public void testWhenAllSucceed() throws Exception {
    class PartialResultException extends Exception {}

    SettableFuture<Integer> futureInteger = SettableFuture.create();
    SettableFuture<Boolean> futureBoolean = SettableFuture.create();
    AsyncCallable<String> combiner =
        new AsyncCallable<String>() {
          @Override
          public ListenableFuture<String> call() throws Exception {
            throw new AssertionFailedError("AsyncCallable should not have been called.");
          }
        };

    ListenableFuture<String> futureResult =
        whenAllSucceed(futureInteger, futureBoolean).callAsync(combiner, directExecutor());
    PartialResultException partialResultException = new PartialResultException();
    futureInteger.setException(partialResultException);
    Boolean booleanPartial = true;
    futureBoolean.set(booleanPartial);
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(futureResult));
    assertSame(partialResultException, expected.getCause());
  }

  @AndroidIncompatible
  @J2ktIncompatible
  @GwtIncompatible
  public void testWhenAllSucceed_releasesInputFuturesUponSubmission() throws Exception {
    SettableFuture<Long> future1 = SettableFuture.create();
    SettableFuture<Long> future2 = SettableFuture.create();
    WeakReference<SettableFuture<Long>> future1Ref = new WeakReference<>(future1);
    WeakReference<SettableFuture<Long>> future2Ref = new WeakReference<>(future2);

    Callable<Long> combiner =
        new Callable<Long>() {
          @Override
          public Long call() {
            throw new AssertionError();
          }
        };

    ListenableFuture<Long> unused =
        whenAllSucceed(future1, future2).call(combiner, noOpScheduledExecutor());

    future1.set(1L);
    future1 = null;
    future2.set(2L);
    future2 = null;

    /*
     * Futures should be collected even if combiner never runs. This is kind of a silly test, since
     * the combiner is almost certain to hold its own reference to the futures, and a real app would
     * hold a reference to the executor and thus to the combiner. What we really care about is that
     * the futures are released once the combiner is done running. But we happen to provide this
     * earlier cleanup at the moment, so we're testing it.
     */
    GcFinalization.awaitClear(future1Ref);
    GcFinalization.awaitClear(future2Ref);
  }

  @AndroidIncompatible
  @J2ktIncompatible
  @GwtIncompatible
  public void testWhenAllComplete_releasesInputFuturesUponCancellation() throws Exception {
    SettableFuture<Long> future = SettableFuture.create();
    WeakReference<SettableFuture<Long>> futureRef = new WeakReference<>(future);

    Callable<Long> combiner =
        new Callable<Long>() {
          @Override
          public Long call() {
            throw new AssertionError();
          }
        };

    ListenableFuture<Long> unused = whenAllComplete(future).call(combiner, noOpScheduledExecutor());

    unused.cancel(false);
    future = null;

    // Future should be collected because whenAll*Complete* doesn't need to look at its result.
    GcFinalization.awaitClear(futureRef);
  }

  @AndroidIncompatible
  @J2ktIncompatible
  @GwtIncompatible
  public void testWhenAllSucceed_releasesCallable() throws Exception {
    AsyncCallable<Long> combiner =
        new AsyncCallable<Long>() {
          @Override
          public ListenableFuture<Long> call() {
            return SettableFuture.create();
          }
        };
    WeakReference<AsyncCallable<Long>> combinerRef = new WeakReference<>(combiner);

    ListenableFuture<Long> unused =
        whenAllSucceed(immediateFuture(1L)).callAsync(combiner, directExecutor());

    combiner = null;
    // combiner should be collected even if the future it returns never completes.
    GcFinalization.awaitClear(combinerRef);
  }

  /*
   * TODO(cpovirk): maybe pass around TestFuture instances instead of
   * ListenableFuture instances
   */

  /**
   * A future in {@link TestFutureBatch} that also has a name for debugging purposes and a {@code
   * finisher}, a task that will complete the future in some fashion when it is called, allowing for
   * testing both before and after the completion of the future.
   */
  @J2ktIncompatible
  @GwtIncompatible // used only in GwtIncompatible tests
  private static final class TestFuture {

    final ListenableFuture<String> future;
    final String name;
    final Runnable finisher;

    TestFuture(ListenableFuture<String> future, String name, Runnable finisher) {
      this.future = future;
      this.name = name;
      this.finisher = finisher;
    }
  }

  /**
   * A collection of several futures, covering cancellation, success, and failure (both {@link
   * ExecutionException} and {@link RuntimeException}), both immediate and delayed. We use each
   * possible pair of these futures in {@link FuturesTest#runExtensiveMergerTest}.
   *
   * <p>Each test requires a new {@link TestFutureBatch} because we need new delayed futures each
   * time, as the old delayed futures were completed as part of the old test.
   */
  @J2ktIncompatible
  @GwtIncompatible // used only in GwtIncompatible tests
  private static final class TestFutureBatch {

    final ListenableFuture<String> doneSuccess = immediateFuture("a");
    final ListenableFuture<String> doneFailed = immediateFailedFuture(new Exception());
    final SettableFuture<String> doneCancelled = SettableFuture.create();

    {
      doneCancelled.cancel(true);
    }

    final ListenableFuture<String> doneRuntimeException =
        new ForwardingListenableFuture<String>() {
          final ListenableFuture<String> delegate = immediateFuture("Should never be seen");

          @Override
          protected ListenableFuture<String> delegate() {
            return delegate;
          }

          @Override
          public String get() {
            throw new RuntimeException();
          }

          @Override
          public String get(long timeout, TimeUnit unit) {
            throw new RuntimeException();
          }
        };

    final SettableFuture<String> delayedSuccess = SettableFuture.create();
    final SettableFuture<String> delayedFailed = SettableFuture.create();
    final SettableFuture<String> delayedCancelled = SettableFuture.create();

    final SettableFuture<String> delegateForDelayedRuntimeException = SettableFuture.create();
    final ListenableFuture<String> delayedRuntimeException =
        new ForwardingListenableFuture<String>() {
          @Override
          protected ListenableFuture<String> delegate() {
            return delegateForDelayedRuntimeException;
          }

          @Override
          public String get() throws ExecutionException, InterruptedException {
            delegateForDelayedRuntimeException.get();
            throw new RuntimeException();
          }

          @Override
          public String get(long timeout, TimeUnit unit)
              throws ExecutionException, InterruptedException, TimeoutException {
            delegateForDelayedRuntimeException.get(timeout, unit);
            throw new RuntimeException();
          }
        };

    final Runnable doNothing =
        new Runnable() {
          @Override
          public void run() {}
        };
    final Runnable finishSuccess =
        new Runnable() {
          @Override
          public void run() {
            delayedSuccess.set("b");
          }
        };
    final Runnable finishFailure =
        new Runnable() {
          @Override
          public void run() {
            delayedFailed.setException(new Exception());
          }
        };
    final Runnable finishCancelled =
        new Runnable() {
          @Override
          public void run() {
            delayedCancelled.cancel(true);
          }
        };
    final Runnable finishRuntimeException =
        new Runnable() {
          @Override
          public void run() {
            delegateForDelayedRuntimeException.set("Should never be seen");
          }
        };

    /** All the futures, together with human-readable names for use by {@link #smartToString}. */
    final ImmutableList<TestFuture> allFutures =
        ImmutableList.of(
            new TestFuture(doneSuccess, "doneSuccess", doNothing),
            new TestFuture(doneFailed, "doneFailed", doNothing),
            new TestFuture(doneCancelled, "doneCancelled", doNothing),
            new TestFuture(doneRuntimeException, "doneRuntimeException", doNothing),
            new TestFuture(delayedSuccess, "delayedSuccess", finishSuccess),
            new TestFuture(delayedFailed, "delayedFailed", finishFailure),
            new TestFuture(delayedCancelled, "delayedCancelled", finishCancelled),
            new TestFuture(
                delayedRuntimeException, "delayedRuntimeException", finishRuntimeException));

    final Function<ListenableFuture<String>, String> nameGetter =
        new Function<ListenableFuture<String>, String>() {
          @Override
          public String apply(ListenableFuture<String> input) {
            for (TestFuture future : allFutures) {
              if (future.future == input) {
                return future.name;
              }
            }
            throw new IllegalArgumentException(input.toString());
          }
        };

    static boolean intersect(Set<?> a, Set<?> b) {
      return !intersection(a, b).isEmpty();
    }

    /**
     * Like {@code inputs.toString()}, but with the nonsense {@code toString} representations
     * replaced with the name of each future from {@link #allFutures}.
     */
    String smartToString(ImmutableSet<ListenableFuture<String>> inputs) {
      Iterable<String> inputNames = Iterables.transform(inputs, nameGetter);
      return Joiner.on(", ").join(inputNames);
    }

    void smartAssertTrue(
        ImmutableSet<ListenableFuture<String>> inputs, Exception cause, boolean expression) {
      if (!expression) {
        throw new AssertionError(smartToString(inputs), cause);
      }
    }

    boolean hasDelayed(ListenableFuture<String> a, ListenableFuture<String> b) {
      ImmutableSet<ListenableFuture<String>> inputs = ImmutableSet.of(a, b);
      return intersect(
          inputs,
          ImmutableSet.of(
              delayedSuccess, delayedFailed, delayedCancelled, delayedRuntimeException));
    }

    void assertHasDelayed(ListenableFuture<String> a, ListenableFuture<String> b, Exception e) {
      ImmutableSet<ListenableFuture<String>> inputs = ImmutableSet.of(a, b);
      smartAssertTrue(inputs, e, hasDelayed(a, b));
    }

    void assertHasFailure(ListenableFuture<String> a, ListenableFuture<String> b, Exception e) {
      ImmutableSet<ListenableFuture<String>> inputs = ImmutableSet.of(a, b);
      smartAssertTrue(
          inputs,
          e,
          intersect(
              inputs,
              ImmutableSet.of(
                  doneFailed, doneRuntimeException, delayedFailed, delayedRuntimeException)));
    }

    void assertHasCancel(ListenableFuture<String> a, ListenableFuture<String> b, Exception e) {
      ImmutableSet<ListenableFuture<String>> inputs = ImmutableSet.of(a, b);
      smartAssertTrue(
          inputs, e, intersect(inputs, ImmutableSet.of(doneCancelled, delayedCancelled)));
    }

    void assertHasImmediateFailure(
        ListenableFuture<String> a, ListenableFuture<String> b, Exception e) {
      ImmutableSet<ListenableFuture<String>> inputs = ImmutableSet.of(a, b);
      smartAssertTrue(
          inputs, e, intersect(inputs, ImmutableSet.of(doneFailed, doneRuntimeException)));
    }

    void assertHasImmediateCancel(
        ListenableFuture<String> a, ListenableFuture<String> b, Exception e) {
      ImmutableSet<ListenableFuture<String>> inputs = ImmutableSet.of(a, b);
      smartAssertTrue(inputs, e, intersect(inputs, ImmutableSet.of(doneCancelled)));
    }
  }

  /**
   * {@link Futures#allAsList(Iterable)} or {@link Futures#successfulAsList(Iterable)}, hidden
   * behind a common interface for testing.
   */
  @J2ktIncompatible
  @GwtIncompatible // used only in GwtIncompatible tests
  private interface Merger {

    ListenableFuture<List<String>> merged(ListenableFuture<String> a, ListenableFuture<String> b);

    Merger allMerger =
        new Merger() {
          @Override
          public ListenableFuture<List<String>> merged(
              ListenableFuture<String> a, ListenableFuture<String> b) {
            return allAsList(ImmutableSet.of(a, b));
          }
        };
    Merger successMerger =
        new Merger() {
          @Override
          public ListenableFuture<List<String>> merged(
              ListenableFuture<String> a, ListenableFuture<String> b) {
            return successfulAsList(ImmutableSet.of(a, b));
          }
        };
  }

  /**
   * Very rough equivalent of a timed get, produced by calling the no-arg get method in another
   * thread and waiting a short time for it.
   *
   * <p>We need this to test the behavior of no-arg get methods without hanging the main test thread
   * forever in the case of failure.
   */
  @CanIgnoreReturnValue
  @J2ktIncompatible
  @GwtIncompatible // threads
  static <V> V pseudoTimedGetUninterruptibly(Future<V> input, long timeout, TimeUnit unit)
      throws ExecutionException, TimeoutException {
    ExecutorService executor = newSingleThreadExecutor();
    Future<V> waiter =
        executor.submit(
            new Callable<V>() {
              @Override
              public V call() throws Exception {
                return input.get();
              }
            });

    try {
      return getUninterruptibly(waiter, timeout, unit);
    } catch (ExecutionException e) {
      propagateIfInstanceOf(e.getCause(), ExecutionException.class);
      propagateIfInstanceOf(e.getCause(), CancellationException.class);
      throw new AssertionError("Unexpected exception", e);
    } finally {
      executor.shutdownNow();
      // TODO(cpovirk): assertTrue(awaitTerminationUninterruptibly(executor, 10, SECONDS));
    }
  }

  /**
   * For each possible pair of futures from {@link TestFutureBatch}, for each possible completion
   * order of those futures, test that various get calls (timed before future completion, untimed
   * before future completion, and untimed after future completion) return or throw the proper
   * values.
   */
  @J2ktIncompatible
  @GwtIncompatible // used only in GwtIncompatible tests
  private static void runExtensiveMergerTest(Merger merger) throws InterruptedException {
    int inputCount = new TestFutureBatch().allFutures.size();

    for (int i = 0; i < inputCount; i++) {
      for (int j = 0; j < inputCount; j++) {
        for (boolean iBeforeJ : new boolean[] {true, false}) {
          TestFutureBatch inputs = new TestFutureBatch();
          ListenableFuture<String> iFuture = inputs.allFutures.get(i).future;
          ListenableFuture<String> jFuture = inputs.allFutures.get(j).future;
          ListenableFuture<List<String>> future = merger.merged(iFuture, jFuture);

          // Test timed get before we've completed any delayed futures.
          try {
            List<String> result = future.get(0, MILLISECONDS);
            assertTrue("Got " + result, asList("a", null).containsAll(result));
          } catch (CancellationException e) {
            assertTrue(merger == Merger.allMerger);
            inputs.assertHasImmediateCancel(iFuture, jFuture, e);
          } catch (ExecutionException e) {
            assertTrue(merger == Merger.allMerger);
            inputs.assertHasImmediateFailure(iFuture, jFuture, e);
          } catch (TimeoutException e) {
            inputs.assertHasDelayed(iFuture, jFuture, e);
          }

          // Same tests with pseudoTimedGet.
          try {
            List<String> result =
                conditionalPseudoTimedGetUninterruptibly(
                    inputs, iFuture, jFuture, future, 20, MILLISECONDS);
            assertTrue("Got " + result, asList("a", null).containsAll(result));
          } catch (CancellationException e) {
            assertTrue(merger == Merger.allMerger);
            inputs.assertHasImmediateCancel(iFuture, jFuture, e);
          } catch (ExecutionException e) {
            assertTrue(merger == Merger.allMerger);
            inputs.assertHasImmediateFailure(iFuture, jFuture, e);
          } catch (TimeoutException e) {
            inputs.assertHasDelayed(iFuture, jFuture, e);
          }

          // Finish the two futures in the currently specified order:
          inputs.allFutures.get(iBeforeJ ? i : j).finisher.run();
          inputs.allFutures.get(iBeforeJ ? j : i).finisher.run();

          // Test untimed get now that we've completed any delayed futures.
          try {
            List<String> result = getDone(future);
            assertTrue("Got " + result, asList("a", "b", null).containsAll(result));
          } catch (CancellationException e) {
            assertTrue(merger == Merger.allMerger);
            inputs.assertHasCancel(iFuture, jFuture, e);
          } catch (ExecutionException e) {
            assertTrue(merger == Merger.allMerger);
            inputs.assertHasFailure(iFuture, jFuture, e);
          }
        }
      }
    }
  }

  /**
   * Call the non-timed {@link Future#get()} in a way that allows us to abort if it's expected to
   * hang forever. More precisely, if it's expected to return, we simply call it[*], but if it's
   * expected to hang (because one of the input futures that we know makes it up isn't done yet),
   * then we call it in a separate thread (using pseudoTimedGet). The result is that we wait as long
   * as necessary when the method is expected to return (at the cost of hanging forever if there is
   * a bug in the class under test) but that we time out fairly promptly when the method is expected
   * to hang (possibly too quickly, but too-quick failures should be very unlikely, given that we
   * used to bail after 20ms during the expected-successful tests, and there we saw a failure rate
   * of ~1/5000, meaning that the other thread's get() call nearly always completes within 20ms if
   * it's going to complete at all).
   *
   * <p>[*] To avoid hangs, I've disabled the in-thread calls. This makes the test take (very
   * roughly) 2.5s longer. (2.5s is also the maximum length of time we will wait for a timed get
   * that is expected to succeed; the fact that the numbers match is only a coincidence.) See the
   * comment below for how to restore the fast but hang-y version.
   */
  @J2ktIncompatible
  @GwtIncompatible // used only in GwtIncompatible tests
  private static List<String> conditionalPseudoTimedGetUninterruptibly(
      TestFutureBatch inputs,
      ListenableFuture<String> iFuture,
      ListenableFuture<String> jFuture,
      ListenableFuture<List<String>> future,
      int timeout,
      TimeUnit unit)
      throws ExecutionException, TimeoutException {
    /*
     * For faster tests (that may hang indefinitely if the class under test has
     * a bug!), switch the second branch to call untimed future.get() instead of
     * pseudoTimedGet.
     */
    return inputs.hasDelayed(iFuture, jFuture)
        ? pseudoTimedGetUninterruptibly(future, timeout, unit)
        : pseudoTimedGetUninterruptibly(future, 2500, MILLISECONDS);
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testAllAsList_extensive() throws InterruptedException {
    runExtensiveMergerTest(Merger.allMerger);
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testSuccessfulAsList_extensive() throws InterruptedException {
    runExtensiveMergerTest(Merger.successMerger);
  }

  public void testSuccessfulAsList() throws Exception {
    // Create input and output
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    SettableFuture<String> future3 = SettableFuture.create();
    ListenableFuture<List<String>> compound = successfulAsList(future1, future2, future3);

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
    assertTrue(listener.wasCalled());

    List<String> results = getDone(compound);
    assertThat(results).containsExactly(DATA1, DATA2, DATA3).inOrder();
  }

  public void testSuccessfulAsList_emptyList() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    List<ListenableFuture<String>> futures = ImmutableList.of();
    ListenableFuture<List<String>> compound = successfulAsList(futures);
    compound.addListener(listener, directExecutor());
    assertThat(getDone(compound)).isEmpty();
    assertTrue(listener.wasCalled());
  }

  public void testSuccessfulAsList_emptyArray() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    ListenableFuture<List<String>> compound = successfulAsList();
    compound.addListener(listener, directExecutor());
    assertThat(getDone(compound)).isEmpty();
    assertTrue(listener.wasCalled());
  }

  public void testSuccessfulAsList_partialFailure() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound = successfulAsList(future1, future2);
    compound.addListener(listener, directExecutor());

    assertFalse(compound.isDone());
    future1.setException(new Throwable("failed1"));
    assertFalse(compound.isDone());
    listener.expectCall();
    future2.set(DATA2);
    assertTrue(listener.wasCalled());

    List<String> results = getDone(compound);
    assertThat(results).containsExactly(null, DATA2).inOrder();
  }

  public void testSuccessfulAsList_totalFailure() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound = successfulAsList(future1, future2);
    compound.addListener(listener, directExecutor());

    assertFalse(compound.isDone());
    future1.setException(new Throwable("failed1"));
    assertFalse(compound.isDone());
    listener.expectCall();
    future2.setException(new Throwable("failed2"));
    assertTrue(listener.wasCalled());

    List<String> results = getDone(compound);
    assertThat(results).containsExactly(null, null).inOrder();
  }

  public void testSuccessfulAsList_cancelled() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound = successfulAsList(future1, future2);
    compound.addListener(listener, directExecutor());

    assertFalse(compound.isDone());
    future1.cancel(true);
    assertFalse(compound.isDone());
    listener.expectCall();
    future2.set(DATA2);
    assertTrue(listener.wasCalled());

    List<String> results = getDone(compound);
    assertThat(results).containsExactly(null, DATA2).inOrder();
  }

  public void testSuccessfulAsList_resultCancelled() throws Exception {
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound = successfulAsList(future1, future2);

    future2.set(DATA2);
    assertFalse(compound.isDone());
    assertTrue(compound.cancel(false));
    assertTrue(compound.isCancelled());
    assertTrue(future1.isCancelled());
    assertFalse(future1.wasInterrupted());
  }

  public void testSuccessfulAsList_resultCancelledRacingInputDone() throws Exception {
    TestLogHandler listenerLoggerHandler = new TestLogHandler();
    Logger exceptionLogger = Logger.getLogger(AbstractFuture.class.getName());
    exceptionLogger.addHandler(listenerLoggerHandler);
    try {
      doTestSuccessfulAsListResultCancelledRacingInputDone();

      assertWithMessage("Nothing should be logged")
          .that(listenerLoggerHandler.getStoredLogRecords())
          .isEmpty();
    } finally {
      exceptionLogger.removeHandler(listenerLoggerHandler);
    }
  }

  private static void doTestSuccessfulAsListResultCancelledRacingInputDone() throws Exception {
    // Simple (combined.cancel -> input.cancel -> setOneValue):
    successfulAsList(ImmutableList.of(SettableFuture.create())).cancel(true);

    /*
     * Complex (combined.cancel -> input.cancel -> other.set -> setOneValue),
     * to show that this isn't just about problems with the input future we just
     * cancelled:
     */
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound = successfulAsList(future1, future2);

    future1.addListener(
        new Runnable() {
          @Override
          public void run() {
            assertTrue(future1.isCancelled());
            /*
             * This test relies on behavior that's unspecified but currently
             * guaranteed by the implementation: Cancellation of inputs is
             * performed in the order they were provided to the constructor. Verify
             * that as a sanity check:
             */
            assertFalse(future2.isCancelled());
            // Now attempt to trigger the exception:
            future2.set(DATA2);
          }
        },
        directExecutor());
    assertTrue(compound.cancel(false));
    assertTrue(compound.isCancelled());
    assertTrue(future1.isCancelled());
    assertFalse(future2.isCancelled());

    try {
      getDone(compound);
      fail();
    } catch (CancellationException expected) {
    }
  }

  public void testSuccessfulAsList_resultInterrupted() throws Exception {
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<String>> compound = successfulAsList(future1, future2);

    future2.set(DATA2);
    assertFalse(compound.isDone());
    assertTrue(compound.cancel(true));
    assertTrue(compound.isCancelled());
    assertTrue(future1.isCancelled());
    assertTrue(future1.wasInterrupted());
  }

  public void testSuccessfulAsList_mixed() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    SettableFuture<String> future3 = SettableFuture.create();
    ListenableFuture<List<String>> compound = successfulAsList(future1, future2, future3);
    compound.addListener(listener, directExecutor());

    // First is cancelled, second fails, third succeeds
    assertFalse(compound.isDone());
    future1.cancel(true);
    assertFalse(compound.isDone());
    future2.setException(new Throwable("failed2"));
    assertFalse(compound.isDone());
    listener.expectCall();
    future3.set(DATA3);
    assertTrue(listener.wasCalled());

    List<String> results = getDone(compound);
    assertThat(results).containsExactly(null, null, DATA3).inOrder();
  }

  /** Non-Error exceptions are never logged. */
  @J2ktIncompatible // TODO(b/324550390): Enable
  public void testSuccessfulAsList_logging_exception() throws Exception {
    assertEquals(
        newArrayList((Object) null),
        getDone(successfulAsList(immediateFailedFuture(new MyException()))));
    assertWithMessage("Nothing should be logged")
        .that(aggregateFutureLogHandler.getStoredLogRecords())
        .isEmpty();

    // Not even if there are a bunch of failures.
    assertEquals(
        newArrayList(null, null, null),
        getDone(
            successfulAsList(
                immediateFailedFuture(new MyException()),
                immediateFailedFuture(new MyException()),
                immediateFailedFuture(new MyException()))));
    assertWithMessage("Nothing should be logged")
        .that(aggregateFutureLogHandler.getStoredLogRecords())
        .isEmpty();
  }

  /** Ensure that errors are always logged. */
  @J2ktIncompatible // TODO(b/324550390): Enable
  public void testSuccessfulAsList_logging_error() throws Exception {
    assertEquals(
        newArrayList((Object) null),
        getDone(successfulAsList(immediateFailedFuture(new SomeError()))));
    List<LogRecord> logged = aggregateFutureLogHandler.getStoredLogRecords();
    assertThat(logged).hasSize(1); // errors are always logged
    assertThat(logged.get(0).getThrown()).isInstanceOf(SomeError.class);
  }

  public void testSuccessfulAsList_failureLoggedEvenAfterOutputCancelled() throws Exception {
    ListenableFuture<String> input = new CancelPanickingFuture<>();
    ListenableFuture<List<String>> output = successfulAsList(input);
    output.cancel(false);

    List<LogRecord> logged = aggregateFutureLogHandler.getStoredLogRecords();
    assertThat(logged).hasSize(1);
    assertThat(logged.get(0).getThrown()).hasMessageThat().isEqualTo("You can't fire me, I quit.");
  }

  private static final class CancelPanickingFuture<V> extends AbstractFuture<V> {
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      setException(new Error("You can't fire me, I quit."));
      return false;
    }
  }

  public void testNonCancellationPropagating_successful() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    ListenableFuture<Foo> wrapper = nonCancellationPropagating(input);
    Foo foo = new Foo();

    assertFalse(wrapper.isDone());
    input.set(foo);
    assertTrue(wrapper.isDone());
    assertSame(foo, getDone(wrapper));
  }

  public void testNonCancellationPropagating_failure() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    ListenableFuture<Foo> wrapper = nonCancellationPropagating(input);
    Throwable failure = new Throwable("thrown");

    assertFalse(wrapper.isDone());
    input.setException(failure);
    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(wrapper));
    assertSame(failure, expected.getCause());
  }

  public void testNonCancellationPropagating_delegateCancelled() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    ListenableFuture<Foo> wrapper = nonCancellationPropagating(input);

    assertFalse(wrapper.isDone());
    assertTrue(input.cancel(false));
    assertTrue(wrapper.isCancelled());
  }

  public void testNonCancellationPropagating_doesNotPropagate() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    ListenableFuture<Foo> wrapper = nonCancellationPropagating(input);

    assertTrue(wrapper.cancel(true));
    assertTrue(wrapper.isCancelled());
    assertTrue(wrapper.isDone());
    assertFalse(input.isCancelled());
    assertFalse(input.isDone());
  }

  @J2ktIncompatible
  @GwtIncompatible // used only in GwtIncompatible tests
  private static class TestException extends Exception {

    TestException(@Nullable Throwable cause) {
      super(cause);
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // used only in GwtIncompatible tests
  private interface MapperFunction extends Function<Throwable, Exception> {}

  public void testCompletionOrder() throws Exception {
    SettableFuture<Long> future1 = SettableFuture.create();
    SettableFuture<Long> future2 = SettableFuture.create();
    SettableFuture<Long> future3 = SettableFuture.create();
    SettableFuture<Long> future4 = SettableFuture.create();
    SettableFuture<Long> future5 = SettableFuture.create();

    ImmutableList<ListenableFuture<Long>> futures =
        inCompletionOrder(
            ImmutableList.<ListenableFuture<Long>>of(future1, future2, future3, future4, future5));
    future2.set(1L);
    future5.set(2L);
    future1.set(3L);
    future3.set(4L);
    future4.set(5L);

    long expectedResult = 1L;
    for (ListenableFuture<Long> future : futures) {
      assertEquals((Long) expectedResult, getDone(future));
      expectedResult++;
    }
  }

  public void testCompletionOrderExceptionThrown() throws Exception {
    SettableFuture<Long> future1 = SettableFuture.create();
    SettableFuture<Long> future2 = SettableFuture.create();
    SettableFuture<Long> future3 = SettableFuture.create();
    SettableFuture<Long> future4 = SettableFuture.create();
    SettableFuture<Long> future5 = SettableFuture.create();

    ImmutableList<ListenableFuture<Long>> futures =
        inCompletionOrder(
            ImmutableList.<ListenableFuture<Long>>of(future1, future2, future3, future4, future5));
    future2.set(1L);
    future5.setException(new IllegalStateException("2L"));
    future1.set(3L);
    future3.set(4L);
    future4.set(5L);

    long expectedResult = 1L;
    for (ListenableFuture<Long> future : futures) {
      if (expectedResult != 2) {
        assertEquals((Long) expectedResult, getDone(future));
      } else {
        ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(future));
        assertThat(expected).hasCauseThat().hasMessageThat().isEqualTo("2L");
      }
      expectedResult++;
    }
  }

  public void testCompletionOrderFutureCancelled() throws Exception {
    SettableFuture<Long> future1 = SettableFuture.create();
    SettableFuture<Long> future2 = SettableFuture.create();
    SettableFuture<Long> future3 = SettableFuture.create();
    SettableFuture<Long> future4 = SettableFuture.create();
    SettableFuture<Long> future5 = SettableFuture.create();

    ImmutableList<ListenableFuture<Long>> futures =
        inCompletionOrder(
            ImmutableList.<ListenableFuture<Long>>of(future1, future2, future3, future4, future5));
    future2.set(1L);
    future5.set(2L);
    future1.set(3L);
    future3.cancel(true);
    future4.set(5L);

    long expectedResult = 1L;
    for (ListenableFuture<Long> future : futures) {
      if (expectedResult != 4) {
        assertEquals((Long) expectedResult, getDone(future));
      } else {
        assertThrows(CancellationException.class, () -> getDone(future));
      }
      expectedResult++;
    }
  }

  public void testCompletionOrderFutureInterruption() throws Exception {
    SettableFuture<Long> future1 = SettableFuture.create();
    SettableFuture<Long> future2 = SettableFuture.create();
    SettableFuture<Long> future3 = SettableFuture.create();

    ImmutableList<ListenableFuture<Long>> futures =
        inCompletionOrder(ImmutableList.<ListenableFuture<Long>>of(future1, future2, future3));
    future2.set(1L);

    futures.get(1).cancel(true);
    futures.get(2).cancel(false);

    assertTrue(future1.isCancelled());
    assertFalse(future1.wasInterrupted());
    assertTrue(future3.isCancelled());
    assertFalse(future3.wasInterrupted());
  }

  public void testCancellingADelegatePropagates() throws Exception {
    SettableFuture<Long> future1 = SettableFuture.create();
    SettableFuture<Long> future2 = SettableFuture.create();
    SettableFuture<Long> future3 = SettableFuture.create();

    ImmutableList<ListenableFuture<Long>> delegates =
        inCompletionOrder(ImmutableList.<ListenableFuture<Long>>of(future1, future2, future3));

    future1.set(1L);
    // Cannot cancel a complete delegate
    assertFalse(delegates.get(0).cancel(true));
    // Cancel the delegate before the input future is done
    assertTrue(delegates.get(1).cancel(true));
    // Setting the future still works since cancellation didn't propagate
    assertTrue(future2.set(2L));
    // Second check to ensure the input future was not cancelled
    assertEquals((Long) 2L, getDone(future2));

    // All futures are now complete; outstanding inputs are cancelled
    assertTrue(future3.isCancelled());
    assertTrue(future3.wasInterrupted());
  }

  @AndroidIncompatible // runs out of memory under some versions of the emulator
  public void testCancellingAllDelegatesIsNotQuadratic() throws Exception {
    ImmutableList.Builder<SettableFuture<Long>> builder = ImmutableList.builder();
    for (int i = 0; i < 500_000; i++) {
      builder.add(SettableFuture.<Long>create());
    }
    ImmutableList<SettableFuture<Long>> inputs = builder.build();
    ImmutableList<ListenableFuture<Long>> delegates = inCompletionOrder(inputs);

    for (ListenableFuture<?> delegate : delegates) {
      delegate.cancel(true);
    }

    for (ListenableFuture<?> input : inputs) {
      assertTrue(input.isDone());
    }
  }

  @AndroidIncompatible // reference is never cleared under some versions of the emulator
  @J2ktIncompatible
  @GwtIncompatible
  public void testInputGCedIfUnreferenced() throws Exception {
    SettableFuture<Long> future1 = SettableFuture.create();
    SettableFuture<Long> future2 = SettableFuture.create();
    WeakReference<SettableFuture<Long>> future1Ref = new WeakReference<>(future1);
    WeakReference<SettableFuture<Long>> future2Ref = new WeakReference<>(future2);

    ImmutableList<ListenableFuture<Long>> delegates =
        inCompletionOrder(ImmutableList.<ListenableFuture<Long>>of(future1, future2));

    future1.set(1L);

    future1 = null;
    // First future is complete, should be unreferenced
    GcFinalization.awaitClear(future1Ref);
    ListenableFuture<Long> outputFuture1 = delegates.get(0);
    delegates = null;
    future2 = null;
    // No references to list or other output future, second future should be unreferenced
    GcFinalization.awaitClear(future2Ref);
    outputFuture1.get();
  }

  // Mostly an example of how it would look like to use a list of mixed types
  @J2ktIncompatible // Wildcard generics
  public void testCompletionOrderMixedBagOTypes() throws Exception {
    SettableFuture<Long> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    SettableFuture<Integer> future3 = SettableFuture.create();

    ImmutableList<? extends ListenableFuture<?>> inputs =
        ImmutableList.<ListenableFuture<?>>of(future1, future2, future3);
    ImmutableList<ListenableFuture<Object>> futures = inCompletionOrder(inputs);
    future2.set("1L");
    future1.set(2L);
    future3.set(3);

    ImmutableList<?> expected = ImmutableList.of("1L", 2L, 3);
    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i), getDone(futures.get(i)));
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // ClassSanityTester
  public void testFutures_nullChecks() throws Exception {
    new ClassSanityTester()
        .forAllPublicStaticMethods(Futures.class)
        .thatReturn(Future.class)
        .testNulls();
  }

  // This test covers a bug where an Error thrown from a callback could cause the TimeoutFuture to
  // never complete when timing out.  Notably, nothing would get logged since the Error would get
  // stuck in the ScheduledFuture inside of TimeoutFuture and nothing ever calls get on it.

  // Simulate a timeout that fires before the call the SES.schedule returns but the future is
  // already completed.

  // This test covers a bug where an Error thrown from a callback could cause the TimeoutFuture to
  // never complete when timing out.  Notably, nothing would get logged since the Error would get
  // stuck in the ScheduledFuture inside of TimeoutFuture and nothing ever calls get on it.

  private static final Executor REJECTING_EXECUTOR =
      new Executor() {
        @Override
        public void execute(Runnable runnable) {
          throw new RejectedExecutionException();
        }
      };

  private static <V> AsyncFunction<V, V> asyncIdentity() {
    return new AsyncFunction<V, V>() {
      @Override
      public ListenableFuture<V> apply(V input) {
        return immediateFuture(input);
      }
    };
  }

  private static <I, O> AsyncFunction<I, O> tagged(String toString, AsyncFunction<I, O> function) {
    return new AsyncFunction<I, O>() {
      @Override
      public ListenableFuture<O> apply(I input) throws Exception {
        return function.apply(input);
      }

      @Override
      public String toString() {
        return toString;
      }
    };
  }

  private static <V> AsyncCallable<V> tagged(String toString, AsyncCallable<V> callable) {
    return new AsyncCallable<V>() {
      @Override
      public ListenableFuture<V> call() throws Exception {
        return callable.call();
      }

      @Override
      public String toString() {
        return toString;
      }
    };
  }
}
