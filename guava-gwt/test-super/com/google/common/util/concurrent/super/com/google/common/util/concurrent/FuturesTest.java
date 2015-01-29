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
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Functions;

import junit.framework.AssertionFailedError;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link Futures}.
 *
 * @author Nishant Thakkar
 */
@GwtCompatible(emulated = true)
public class FuturesTest extends EmptySetUpAndTearDown {

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

  private static class MyException extends Exception {}

  // Class hierarchy for generics sanity checks
  private static class Foo {}
  private static class FooChild extends Foo {}
  private static class Bar {}
  private static class BarChild extends Bar {}

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
      @Override public BarChild apply(Foo unused) {
        return barChild;
      }
    };
    Bar bar = Futures.transform(future, function).get();
    assertSame(barChild, bar);
  }

  public void testTransform_cancelPropagatesToInput() throws Exception {
    SettableFuture<Foo> input = SettableFuture.create();
    AsyncFunction<Foo, Bar> function = new AsyncFunction<Foo, Bar>() {
      @Override public ListenableFuture<Bar> apply(Foo unused) {
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
      @Override public ListenableFuture<Bar> apply(Foo unused) {
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
      @Override public ListenableFuture<Bar> apply(Foo unused) {
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
      @Override public ListenableFuture<Bar> apply(Foo unused) {
        return secondary;
      }
    };
    assertTrue(Futures.transform(immediate, function).cancel(true));
    assertTrue(secondary.isCancelled());
    assertTrue(secondary.wasInterrupted());
  }

  /**
   * Tests that the function is invoked only once, even if it throws an
   * exception.
   */
  public void testTransformValueRemainsMemoized() throws Exception {
    class Holder {
      int value = 2;
    }
    final Holder holder = new Holder();

    // This function adds the holder's value to the input value.
    Function<Integer, Integer> adder = new Function<Integer, Integer>() {
      @Override public Integer apply(Integer from) {
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

  static class MyError extends Error {}
  static class MyRuntimeException extends RuntimeException {}

  // TODO(cpovirk): top-level class?
  static class ExecutorSpy implements Executor {
    Executor delegate;
    boolean wasExecuted;

    public ExecutorSpy(Executor delegate) {
      this.delegate = delegate;
    }

    @Override public void execute(Runnable command) {
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

  public void testTransform_genericsWildcard_AsyncFunction() throws Exception {
    ListenableFuture<?> nullFuture = immediateFuture(null);
    ListenableFuture<?> chainedFuture =
        Futures.transform(nullFuture, constantAsyncFunction(nullFuture));
    assertNull(chainedFuture.get());
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

  public void testTransform_genericsHierarchy_AsyncFunction() throws Exception {
    ListenableFuture<FooChild> future = Futures.immediateFuture(null);
    final BarChild barChild = new BarChild();
    AsyncFunction<Foo, BarChild> function =
        new AsyncFunction<Foo, BarChild>() {
          @Override public AbstractFuture<BarChild> apply(Foo unused) {
            AbstractFuture<BarChild> future = new AbstractFuture<BarChild>() {};
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
      @Override public ListenableFuture<Integer> apply(String input) {
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

  private static String createCombinedResult(Integer i, Boolean b) {
    return "-" + i + "-" + b;
  }

  /*
   * TODO(cpovirk): maybe pass around TestFuture instances instead of
   * ListenableFuture instances
   */

  private static final class OtherThrowable extends Throwable {}

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

  /** A future that throws a runtime exception from get. */
  static class BuggyFuture extends AbstractFuture<String> {
    @Override public String get() {
      throw new RuntimeException();
    }
    @Override public boolean set(String v) {
      return super.set(v);
    }
  }

  // This test covers a bug where an Error thrown from a callback could cause the TimeoutFuture to
  // never complete when timing out.  Notably, nothing would get logged since the Error would get
  // stuck in the ScheduledFuture inside of TimeoutFuture and nothing ever calls get on it.

  // Simulate a timeout that fires before the call the SES.schedule returns but the future is
  // already completed.
}
