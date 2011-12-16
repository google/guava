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

import static com.google.common.base.Throwables.propagateIfInstanceOf;
import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.get;
import static com.google.common.util.concurrent.Futures.getUnchecked;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.successfulAsList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.testing.NullPointerTester;
import com.google.common.util.concurrent.ForwardingFuture.SimpleForwardingFuture;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

/**
 * Unit tests for {@link Futures}.
 *
 * TODO: Add tests for other Futures methods
 *
 * @author Nishant Thakkar
 */
public class FuturesTest extends TestCase {
  private static final String DATA1 = "data";
  private static final String DATA2 = "more data";
  private static final String DATA3 = "most data";

  private IMocksControl mocksControl;

  @Override protected void setUp() throws Exception {
    super.setUp();

    mocksControl = EasyMock.createControl();
  }

  @Override protected void tearDown() throws Exception {
    /*
     * Clear interrupt for future tests.
     *
     * (Ideally we would perform interrupts only in threads that we create, but
     * it's hard to imagine that anything will break in practice.)
     */
    Thread.interrupted();

    super.tearDown();
  }

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

  public void testImmediateFailedFuture() throws Exception {
    Exception exception = new Exception();
    ListenableFuture<String> future =
        Futures.immediateFailedFuture(exception);

    try {
      future.get(0L, TimeUnit.MILLISECONDS);
      fail("This call was supposed to throw an ExecutionException");
    } catch (ExecutionException expected) {
      // This is good and expected
      assertSame(exception, expected.getCause());
    }
  }

  private static class MyException extends Exception {}

  public void testImmediateCheckedFuture() throws Exception {
    CheckedFuture<String, MyException> future = Futures.immediateCheckedFuture(
        DATA1);

    // Verify that the proper object is returned without waiting
    assertSame(DATA1, future.get(0L, TimeUnit.MILLISECONDS));
    assertSame(DATA1, future.checkedGet(0L, TimeUnit.MILLISECONDS));
  }

  public void testMultipleImmediateCheckedFutures() throws Exception {
    CheckedFuture<String, MyException> future1 = Futures.immediateCheckedFuture(
        DATA1);
    CheckedFuture<String, MyException> future2 = Futures.immediateCheckedFuture(
        DATA2);

    // Verify that the proper objects are returned without waiting
    assertSame(DATA1, future1.get(0L, TimeUnit.MILLISECONDS));
    assertSame(DATA1, future1.checkedGet(0L, TimeUnit.MILLISECONDS));
    assertSame(DATA2, future2.get(0L, TimeUnit.MILLISECONDS));
    assertSame(DATA2, future2.checkedGet(0L, TimeUnit.MILLISECONDS));
  }

  public void testImmediateFailedCheckedFuture() throws Exception {
    MyException exception = new MyException();
    CheckedFuture<String, MyException> future =
        Futures.immediateFailedCheckedFuture(exception);

    try {
      future.get(0L, TimeUnit.MILLISECONDS);
      fail("This call was supposed to throw an ExecutionException");
    } catch (ExecutionException expected) {
      // This is good and expected
      assertSame(exception, expected.getCause());
    }

    try {
      future.checkedGet(0L, TimeUnit.MILLISECONDS);
      fail("This call was supposed to throw an MyException");
    } catch (MyException expected) {
      // This is good and expected
      assertSame(exception, expected);
    }
  }

  // Class hierarchy for generics sanity checks
  private static class Foo {}
  private static class FooChild extends Foo {}
  private static class Bar {}
  private static class BarChild extends Bar {}

  public void testTransform_ListenableFuture_genericsNull() throws Exception {
    ListenableFuture<?> nullFuture = Futures.immediateFuture(null);
    ListenableFuture<?> transformedFuture =
        Futures.transform(nullFuture, Functions.constant(null));
    assertNull(transformedFuture.get());
  }

  public void testTransform_ListenableFuture_genericsHierarchy()
      throws Exception {
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

  /**
   * {@link ListenableFuture} variant of
   * {@link #testTransformValueRemainsMemoized_Future()}.
   */
  public void testTransformValueRemainsMemoized_ListenableFuture()
      throws Exception {
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

  /**
   * {@link ListenableFuture} variant of
   * {@link #testTransformExceptionRemainsMemoized_Future()}.
   */
  public void testTransformExceptionRemainsMemoized_ListenableFuture()
      throws Throwable {
    SettableFuture<Integer> input = SettableFuture.create();

    ListenableFuture<Integer> exceptionComposedFuture =
        Futures.transform(input, newOneTimeExceptionThrower());
    ListenableFuture<Integer> errorComposedFuture =
        Futures.transform(input, newOneTimeErrorThrower());

    try {
      input.set(0);
      fail();
    } catch (MyError expected) {
      /*
       * The ListenableFuture variant rethrows errors from execute() as well
       * as assigning them to the output of the future.
       */
    }

    runGetIdempotencyTest(exceptionComposedFuture, MyRuntimeException.class);
    runGetIdempotencyTest(errorComposedFuture, MyError.class);

    /*
     * Try again when the input's value is already filled in, since the flow is
     * slightly different in that case.
     */
    exceptionComposedFuture =
        Futures.transform(input, newOneTimeExceptionThrower());
    runGetIdempotencyTest(exceptionComposedFuture, MyRuntimeException.class);

    try {
      Futures.transform(input, newOneTimeErrorThrower());
      fail();
    } catch (MyError expected) {
    }
  }

  private static void runGetIdempotencyTest(Future<Integer> transformedFuture,
      Class<? extends Throwable> expectedExceptionClass) throws Throwable {
    for (int i = 0; i < 5; i++) {
      try {
        transformedFuture.get();
        fail();
      } catch (ExecutionException expected) {
        if (!expectedExceptionClass.isInstance(expected.getCause())) {
          throw expected.getCause();
        }
      }
    }
  }

  private static <I, O> Function<I, O> newOneTimeValueReturner(final O output) {
    return new Function<I, O>() {
      int calls = 0;

      @Override
      public O apply(I arg0) {
        if (++calls > 1) {
          fail();
        }
        return output;
      }
    };
  }

  private static Function<Integer, Integer> newOneTimeExceptionThrower() {
    return new Function<Integer, Integer>() {
      int calls = 0;

      @Override public Integer apply(Integer from) {
        if (++calls > 1) {
          fail();
        }
        throw new MyRuntimeException();
      }
    };
  }

  private static Function<Integer, Integer> newOneTimeErrorThrower() {
    return new Function<Integer, Integer>() {
      int calls = 0;

      @Override public Integer apply(Integer from) {
        if (++calls > 1) {
          fail();
        }
        throw new MyError();
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

    @Override public void execute(Runnable command) {
      delegate.execute(command);
      wasExecuted = true;
    }
  }

  public void testTransform_Executor() throws Exception {
    Object value = new Object();
    ExecutorSpy spy = new ExecutorSpy(MoreExecutors.sameThreadExecutor());

    assertFalse(spy.wasExecuted);

    ListenableFuture<Object> future = Futures.transform(
        Futures.immediateFuture(value),
        Functions.identity(), spy);

    assertSame(value, future.get());
    assertTrue(spy.wasExecuted);
  }

  public void testLazyTransform() throws Exception {
    FunctionSpy<Object, String> spy =
        new FunctionSpy<Object, String>(Functions.constant("bar"));
    Future<String> input = Futures.immediateFuture("foo");
    Future<String> transformed = Futures.lazyTransform(input, spy);
    assertEquals(0, spy.getApplyCount());
    assertEquals("bar", transformed.get());
    assertEquals(1, spy.getApplyCount());
    assertEquals("bar", transformed.get());
    assertEquals(2, spy.getApplyCount());
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

    public int getApplyCount() {
      return applyCount;
    }
  }

  public void testTransform_genericsWildcard_AsyncFunction() throws Exception {
    ListenableFuture<?> nullFuture = Futures.immediateFuture(null);
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

  public void testTransform_delegatesBlockingGet_AsyncFunction() throws Exception {
    performAsyncFunctionTransformedFutureDelgationTest(0, null);
  }

  public void testTransform_delegatesTimedGet_AsyncFunction() throws Exception {
    performAsyncFunctionTransformedFutureDelgationTest(25, TimeUnit.SECONDS);
  }

  private void performAsyncFunctionTransformedFutureDelgationTest(
      long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    final Foo foo = new Foo();
    MockRequiresGetCallFuture<Foo> fooFuture =
        new MockRequiresGetCallFuture<Foo>(foo);

    Bar bar = new Bar();
    final MockRequiresGetCallFuture<Bar> barFuture =
        new MockRequiresGetCallFuture<Bar>(bar);
    AsyncFunction<Foo, Bar> function =
        new AsyncFunction<Foo, Bar>() {
          @Override public ListenableFuture<Bar> apply(Foo from) {
            assertSame(foo, from);
            return barFuture;
          }
        };

    ListenableFuture<Bar> chainFuture = Futures.transform(fooFuture, function);
    Bar theBar;
    if (unit != null) {
      theBar = chainFuture.get(timeout, unit);
    } else {
      theBar = chainFuture.get();
    }
    assertSame(bar, theBar);
    assertTrue(fooFuture.getWasGetCalled());
    assertTrue(barFuture.getWasGetCalled());
  }

  /**
   * A mock listenable future that requires the caller invoke
   * either form of get() before the future will make its value
   * available or invoke listeners.
   */
  private static class MockRequiresGetCallFuture<T> extends AbstractFuture<T> {

    private final T value;
    private boolean getWasCalled;

    MockRequiresGetCallFuture(T value) {
      this.value = value;
    }

    @Override public T get() throws InterruptedException, ExecutionException {
      set(value);
      getWasCalled = true;
      return super.get();
    }

    @Override public T get(long timeout, TimeUnit unit)
        throws TimeoutException, ExecutionException, InterruptedException {
      set(value);
      getWasCalled = true;
      return super.get(timeout, unit);
    }

    boolean getWasGetCalled() {
      return getWasCalled;
    }
  }

  /**
   * Runnable which can be called a single time, and only after
   * {@link #expectCall} is called.
   */
  // TODO(cpovirk): top-level class?
  static class SingleCallListener implements Runnable {
    private boolean expectCall = false;
    private final CountDownLatch calledCountDown =
        new CountDownLatch(1);

    @Override public void run() {
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
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());

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
    ASSERT.that(results).hasContentsInOrder(DATA1, DATA2, DATA3);
  }

  public void testAllAsList_emptyList() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    List<ListenableFuture<String>> futures = ImmutableList.of();
    ListenableFuture<List<String>> compound = Futures.allAsList(futures);
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());
    assertTrue(compound.isDone());
    assertTrue(compound.get().isEmpty());
    assertTrue(listener.wasCalled());
  }

  public void testAllAsList_emptyArray() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    @SuppressWarnings("unchecked") // array is never modified
    ListenableFuture<List<String>> compound = Futures.allAsList();
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());
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
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());

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

  public void testAllAsList_singleFailure() throws Exception {
    Throwable exception = new Throwable("failed");
    ListenableFuture<String> future = Futures.immediateFailedFuture(exception);
    ListenableFuture<List<String>> compound = Futures.allAsList(ImmutableList.of(future));

    try {
      compound.get();
      fail("Expected exception not thrown");
    } catch (ExecutionException e) {
      assertSame(exception, e.getCause());
    }
  }

  public void testAllAsList_immediateFailure() throws Exception {
    Throwable exception = new Throwable("failed");
    ListenableFuture<String> future1 = Futures.immediateFailedFuture(exception);
    ListenableFuture<String> future2 = Futures.immediateFuture("results");
    ListenableFuture<List<String>> compound = Futures.allAsList(ImmutableList.of(future1, future2));

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
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());

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

  public void testAllAsList_buggyInputFutures() throws Exception {
    final Foo foo1 = new Foo();
    MockRequiresGetCallFuture<Foo> foo1Future =
        new MockRequiresGetCallFuture<Foo>(foo1);
    final Foo foo2 = new Foo();
    MockRequiresGetCallFuture<Foo> foo2Future =
        new MockRequiresGetCallFuture<Foo>(foo2);

    @SuppressWarnings("unchecked") // array is never modified
    ListenableFuture<List<Foo>> compound =
        Futures.allAsList(foo1Future, foo2Future);

    assertFalse(compound.isDone());
    ASSERT.that(compound.get()).hasContentsAnyOrder(foo1, foo2);
    assertTrue(foo1Future.getWasGetCalled());
    assertTrue(foo2Future.getWasGetCalled());
  }

  /**
   * Test the case where the futures are fulfilled prior to
   * constructing the ListFuture.  There was a bug where the
   * loop that connects a Listener to each of the futures would die
   * on the last loop-check as done() on ListFuture nulled out the
   * variable being looped over (the list of futures).
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
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());

    assertTrue(compound.isDone());
    assertTrue(listener.wasCalled());

    List<String> results = compound.get();
    ASSERT.that(results).hasContentsInOrder(DATA1, DATA2, DATA3);
  }

  private String createCombinedResult(Integer i, Boolean b) {
    return "-" + i + "-" + b;
  }

  /*
   * TODO(cpovirk): maybe pass around TestFuture instances instead of
   * ListenableFuture instances
   */
  /**
   * A future in {@link TestFutureBatch} that also has a name for debugging
   * purposes and a {@code finisher}, a task that will complete the future in
   * some fashion when it is called, allowing for testing both before and after
   * the completion of the future.
   */
  private static final class TestFuture {
    final ListenableFuture<String> future;
    final String name;
    final Runnable finisher;

    TestFuture(
        ListenableFuture<String> future, String name, Runnable finisher) {
      this.future = future;
      this.name = name;
      this.finisher = finisher;
    }
  }

  /**
   * A collection of several futures, covering cancellation, success, and
   * failure (both {@link ExecutionException} and {@link RuntimeException}),
   * both immediate and delayed. We use each possible pair of these futures in
   * {@link FuturesTest#runExtensiveMergerTest}.
   *
   * <p>Each test requires a new {@link TestFutureBatch} because we need new
   * delayed futures each time, as the old delayed futures were completed as
   * part of the old test.
   */
  private static final class TestFutureBatch {
    final ListenableFuture<String> doneSuccess = immediateFuture("a");
    final ListenableFuture<String> doneFailed =
        immediateFailedFuture(new Exception());
    final SettableFuture<String> doneCancelled = SettableFuture.create();
    {
      doneCancelled.cancel(true);
    }

    final ListenableFuture<String> doneRuntimeException =
        new ForwardingListenableFuture<String>() {
          final ListenableFuture<String> delegate =
              immediateFuture("Should never be seen");

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

    final SettableFuture<String> delegateForDelayedRuntimeException =
        SettableFuture.create();
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
          public String get(long timeout, TimeUnit unit) throws
              ExecutionException, InterruptedException, TimeoutException {
            delegateForDelayedRuntimeException.get(timeout, unit);
            throw new RuntimeException();
          }
    };

    final Runnable doNothing = new Runnable() {
      @Override
      public void run() {
      }
    };
    final Runnable finishSuccess = new Runnable() {
      @Override
      public void run() {
        delayedSuccess.set("b");
      }
    };
    final Runnable finishFailure = new Runnable() {
      @Override
      public void run() {
        delayedFailed.setException(new Exception());
      }
    };
    final Runnable finishCancelled = new Runnable() {
      @Override
      public void run() {
        delayedCancelled.cancel(true);
      }
    };
    final Runnable finishRuntimeException = new Runnable() {
      @Override
      public void run() {
        delegateForDelayedRuntimeException.set("Should never be seen");
      }
    };

    /**
     * All the futures, together with human-readable names for use by
     * {@link #smartToString}.
     */
    final ImmutableList<TestFuture> allFutures =
        ImmutableList.of(new TestFuture(doneSuccess, "doneSuccess", doNothing),
            new TestFuture(doneFailed, "doneFailed", doNothing),
            new TestFuture(doneCancelled, "doneCancelled", doNothing),
            new TestFuture(
                doneRuntimeException, "doneRuntimeException", doNothing),
            new TestFuture(delayedSuccess, "delayedSuccess", finishSuccess),
            new TestFuture(delayedFailed, "delayedFailed", finishFailure),
            new TestFuture(
                delayedCancelled, "delayedCancelled", finishCancelled),
            new TestFuture(delayedRuntimeException, "delayedRuntimeException",
                finishRuntimeException));

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
      return !Sets.intersection(a, b).isEmpty();
    }

    /**
     * Like {@code inputs.toString()}, but with the nonsense {@code toString}
     * representations replaced with the name of each future from
     * {@link #allFutures}.
     */
    String smartToString(ImmutableSet<ListenableFuture<String>> inputs) {
      Iterable<String> inputNames = Iterables.transform(inputs, nameGetter);
      return Joiner.on(", ").join(inputNames);
    }

    void smartAssertTrue(ImmutableSet<ListenableFuture<String>> inputs,
        Exception cause, boolean expression) {
      if (!expression) {
        failWithCause(cause, smartToString(inputs));
      }
    }

    boolean hasDelayed(ListenableFuture<String> a, ListenableFuture<String> b) {
      ImmutableSet<ListenableFuture<String>> inputs = ImmutableSet.of(a, b);
      return intersect(inputs, ImmutableSet.of(
          delayedSuccess, delayedFailed, delayedCancelled,
          delayedRuntimeException));
    }

    void assertHasDelayed(
        ListenableFuture<String> a, ListenableFuture<String> b, Exception e) {
      ImmutableSet<ListenableFuture<String>> inputs = ImmutableSet.of(a, b);
      smartAssertTrue(inputs, e, hasDelayed(a, b));
    }

    void assertHasFailure(
        ListenableFuture<String> a, ListenableFuture<String> b, Exception e) {
      ImmutableSet<ListenableFuture<String>> inputs = ImmutableSet.of(a, b);
      smartAssertTrue(inputs, e, intersect(inputs, ImmutableSet.of(doneFailed,
          doneRuntimeException, delayedFailed, delayedRuntimeException)));
    }

    void assertHasCancel(
        ListenableFuture<String> a, ListenableFuture<String> b, Exception e) {
      ImmutableSet<ListenableFuture<String>> inputs = ImmutableSet.of(a, b);
      smartAssertTrue(inputs, e,
          intersect(inputs, ImmutableSet.of(doneCancelled, delayedCancelled)));
    }

    void assertHasImmediateFailure(
        ListenableFuture<String> a, ListenableFuture<String> b, Exception e) {
      ImmutableSet<ListenableFuture<String>> inputs = ImmutableSet.of(a, b);
      smartAssertTrue(inputs, e, intersect(
          inputs, ImmutableSet.of(doneFailed, doneRuntimeException)));
    }

    void assertHasImmediateCancel(
        ListenableFuture<String> a, ListenableFuture<String> b, Exception e) {
      ImmutableSet<ListenableFuture<String>> inputs = ImmutableSet.of(a, b);
      smartAssertTrue(inputs, e,
          intersect(inputs, ImmutableSet.of(doneCancelled)));
    }
  }

  /**
   * {@link Futures#allAsList(Iterable)} or
   * {@link Futures#successfulAsList(Iterable)}, hidden behind a common
   * interface for testing.
   */
  private interface Merger {
    ListenableFuture<List<String>> merged(
        ListenableFuture<String> a, ListenableFuture<String> b);

    Merger allMerger = new Merger() {
      @Override
      public ListenableFuture<List<String>> merged(
          ListenableFuture<String> a, ListenableFuture<String> b) {
        return allAsList(ImmutableSet.of(a, b));
      }
    };
    Merger successMerger = new Merger() {
      @Override
      public ListenableFuture<List<String>> merged(
          ListenableFuture<String> a, ListenableFuture<String> b) {
        return successfulAsList(ImmutableSet.of(a, b));
      }
    };
  }

  /**
   * Very rough equivalent of a timed get, produced by calling the no-arg get
   * method in another thread and waiting a short time for it.
   *
   * <p>We need this to test the behavior of no-arg get methods without hanging
   * the main test thread forever in the case of failure.
   */
  private static <V> V pseudoTimedGet(
      final Future<V> input, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    ExecutorService executor = newSingleThreadExecutor();
    Future<V> waiter = executor.submit(new Callable<V>() {
      @Override
      public V call() throws Exception {
        return input.get();
      }
    });

    try {
      return waiter.get(timeout, unit);
    } catch (ExecutionException e) {
      propagateIfInstanceOf(e.getCause(), ExecutionException.class);
      propagateIfInstanceOf(e.getCause(), CancellationException.class);
      AssertionFailedError error =
          new AssertionFailedError("Unexpected exception");
      error.initCause(e);
      throw error;
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, SECONDS));
    }
  }

  /**
   * For each possible pair of futures from {@link TestFutureBatch}, for each
   * possible completion order of those futures, test that various get calls
   * (timed before future completion, untimed before future completion, and
   * untimed after future completion) return or throw the proper values.
   */
  private static void runExtensiveMergerTest(Merger merger)
      throws InterruptedException {
    int inputCount = new TestFutureBatch().allFutures.size();

    for (int i = 0; i < inputCount; i++) {
      for (int j = 0; j < inputCount; j++) {
        for (boolean iBeforeJ : new boolean[] { true, false }) {
          TestFutureBatch inputs = new TestFutureBatch();
          ListenableFuture<String> iFuture = inputs.allFutures.get(i).future;
          ListenableFuture<String> jFuture = inputs.allFutures.get(j).future;
          ListenableFuture<List<String>> future =
              merger.merged(iFuture, jFuture);

          // Test timed get before we've completed any delayed futures.
          try {
            List<String> result = future.get(0, MILLISECONDS);
            assertTrue("Got " + result,
                Arrays.asList("a", null).containsAll(result));
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
            List<String> result = conditionalPseudoTimedGet(
                inputs, iFuture, jFuture, future, 20, MILLISECONDS);
            assertTrue("Got " + result,
                Arrays.asList("a", null).containsAll(result));
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
            List<String> result = future.get();
            assertTrue("Got " + result,
                Arrays.asList("a", "b", null).containsAll(result));
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
   * Call the non-timed {@link Future#get()} in a way that allows us to abort if
   * it's expected to hang forever. More precisely, if it's expected to return,
   * we simply call it[*], but if it's expected to hang (because one of the
   * input futures that we know makes it up isn't done yet), then we call it in
   * a separate thread (using pseudoTimedGet). The result is that we wait as
   * long as necessary when the method is expected to return (at the cost of
   * hanging forever if there is a bug in the class under test) but that we time
   * out fairly promptly when the method is expected to hang (possibly too
   * quickly, but too-quick failures should be very unlikely, given that we used
   * to bail after 20ms during the expected-successful tests, and there we saw a
   * failure rate of ~1/5000, meaning that the other thread's get() call nearly
   * always completes within 20ms if it's going to complete at all).
   *
   * [*] To avoid hangs, I've disabled the in-thread calls. This makes the test
   * take (very roughly) 2.5s longer. (2.5s is also the maximum length of time
   * we will wait for a timed get that is expected to succeed; the fact that the
   * numbers match is only a coincidence.) See the comment below for how to
   * restore the fast but hang-y version.
   */
  private static List<String> conditionalPseudoTimedGet(
      TestFutureBatch inputs,
      ListenableFuture<String> iFuture,
      ListenableFuture<String> jFuture,
      ListenableFuture<List<String>> future,
      int timeout,
      TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    /*
     * For faster tests (that may hang indefinitely if the class under test has
     * a bug!), switch the second branch to call untimed future.get() instead of
     * pseudoTimedGet.
     */
    return (inputs.hasDelayed(iFuture, jFuture))
        ? pseudoTimedGet(future, timeout, unit)
        : pseudoTimedGet(future, 2500, MILLISECONDS);
  }

  public void testAllAsList_extensive() throws InterruptedException {
    runExtensiveMergerTest(Merger.allMerger);
  }

  public void testSuccessfulAsList_extensive() throws InterruptedException {
    runExtensiveMergerTest(Merger.successMerger);
  }

  public void testSuccessfulAsList() throws Exception {
    // Create input and output
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    SettableFuture<String> future3 = SettableFuture.create();
    @SuppressWarnings("unchecked") // array is never modified
    ListenableFuture<List<String>> compound =
        Futures.successfulAsList(future1, future2, future3);

    // Attach a listener
    SingleCallListener listener = new SingleCallListener();
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());

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
    ASSERT.that(results).hasContentsInOrder(DATA1, DATA2, DATA3);
  }

  public void testSuccessfulAsList_emptyList() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    List<ListenableFuture<String>> futures = ImmutableList.of();
    ListenableFuture<List<String>> compound = Futures.successfulAsList(futures);
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());
    assertTrue(compound.isDone());
    assertTrue(compound.get().isEmpty());
    assertTrue(listener.wasCalled());
  }

  public void testSuccessfulAsList_emptyArray() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    @SuppressWarnings("unchecked") // array is never modified
    ListenableFuture<List<String>> compound = Futures.successfulAsList();
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());
    assertTrue(compound.isDone());
    assertTrue(compound.get().isEmpty());
    assertTrue(listener.wasCalled());
  }

  public void testSuccessfulAsList_partialFailure() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    @SuppressWarnings("unchecked") // array is never modified
    ListenableFuture<List<String>> compound =
        Futures.successfulAsList(future1, future2);
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());

    assertFalse(compound.isDone());
    future1.setException(new Throwable("failed1"));
    assertFalse(compound.isDone());
    listener.expectCall();
    future2.set(DATA2);
    assertTrue(compound.isDone());
    assertTrue(listener.wasCalled());

    List<String> results = compound.get();
    ASSERT.that(results).hasContentsInOrder(null, DATA2);
  }

  public void testSuccessfulAsList_totalFailure() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    @SuppressWarnings("unchecked") // array is never modified
    ListenableFuture<List<String>> compound =
        Futures.successfulAsList(future1, future2);
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());

    assertFalse(compound.isDone());
    future1.setException(new Throwable("failed1"));
    assertFalse(compound.isDone());
    listener.expectCall();
    future2.setException(new Throwable("failed2"));
    assertTrue(compound.isDone());
    assertTrue(listener.wasCalled());

    List<String> results = compound.get();
    ASSERT.that(results).hasContentsInOrder(null, null);
  }

  public void testSuccessfulAsList_cancelled() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    @SuppressWarnings("unchecked") // array is never modified
    ListenableFuture<List<String>> compound =
        Futures.successfulAsList(future1, future2);
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());

    assertFalse(compound.isDone());
    future1.cancel(true);
    assertFalse(compound.isDone());
    listener.expectCall();
    future2.set(DATA2);
    assertTrue(compound.isDone());
    assertTrue(listener.wasCalled());

    List<String> results = compound.get();
    ASSERT.that(results).hasContentsInOrder(null, DATA2);
  }

  public void testSuccessfulAsList_mixed() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    SettableFuture<String> future3 = SettableFuture.create();
    @SuppressWarnings("unchecked") // array is never modified
    ListenableFuture<List<String>> compound =
        Futures.successfulAsList(future1, future2, future3);
    compound.addListener(listener, MoreExecutors.sameThreadExecutor());

    // First is cancelled, second fails, third succeeds
    assertFalse(compound.isDone());
    future1.cancel(true);
    assertFalse(compound.isDone());
    future2.setException(new Throwable("failed2"));
    assertFalse(compound.isDone());
    listener.expectCall();
    future3.set(DATA3);
    assertTrue(compound.isDone());
    assertTrue(listener.wasCalled());

    List<String> results = compound.get();
    ASSERT.that(results).hasContentsInOrder(null, null, DATA3);
  }

  public void testSuccessfulAsList_buggyInputFutures() throws Exception {
    final Foo foo1 = new Foo();
    MockRequiresGetCallFuture<Foo> foo1Future =
        new MockRequiresGetCallFuture<Foo>(foo1);
    final Foo foo2 = new Foo();
    MockRequiresGetCallFuture<Foo> foo2Future =
        new MockRequiresGetCallFuture<Foo>(foo2);

    @SuppressWarnings("unchecked") // array is never modified
    ListenableFuture<List<Foo>> compound =
        Futures.successfulAsList(foo1Future, foo2Future);

    assertFalse(compound.isDone());
    ASSERT.that(compound.get()).hasContentsAnyOrder(foo1, foo2);
    assertTrue(foo1Future.getWasGetCalled());
    assertTrue(foo2Future.getWasGetCalled());
  }

  private static class TestException extends Exception {
    TestException(@Nullable Throwable cause) {
      super(cause);
    }
  }

  private static final Function<Exception, TestException> mapper =
      new Function<Exception, TestException>() {
    @Override public TestException apply(Exception from) {
      if (from instanceof ExecutionException) {
        return new TestException(from.getCause());
      } else {
        assertTrue("got " + from.getClass(),
            from instanceof InterruptedException
                || from instanceof CancellationException);
        return new TestException(from);
      }
    }
  };

  public void testMakeChecked_mapsExecutionExceptions() throws Exception {
    SettableFuture<String> future = SettableFuture.create();

    CheckedFuture<String, TestException> checked = Futures.makeChecked(
        future, mapper);

    future.setException(new IOException("checked"));

    assertTrue(checked.isDone());
    assertFalse(checked.isCancelled());

    try {
      checked.get();
      fail();
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof IOException);
    }

    try {
      checked.get(5, TimeUnit.SECONDS);
      fail();
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof IOException);
    }

    try {
      checked.checkedGet();
      fail();
    } catch (TestException e) {
      assertTrue(e.getCause() instanceof IOException);
    }

    try {
      checked.checkedGet(5, TimeUnit.SECONDS);
      fail();
    } catch (TestException e) {
      assertTrue(e.getCause() instanceof IOException);
    }
  }

  public void testMakeChecked_mapsInterruption() throws Exception {
    SettableFuture<String> future = SettableFuture.create();

    CheckedFuture<String, TestException> checked = Futures.makeChecked(
        future, mapper);

    Thread.currentThread().interrupt();

    try {
      checked.get();
      fail();
    } catch (InterruptedException e) {
      // Expected.
    }

    Thread.currentThread().interrupt();

    try {
      checked.get(5, TimeUnit.SECONDS);
      fail();
    } catch (InterruptedException e) {
      // Expected.
    }

    Thread.currentThread().interrupt();

    try {
      checked.checkedGet();
      fail();
    } catch (TestException e) {
      assertTrue(e.getCause() instanceof InterruptedException);
    }

    Thread.currentThread().interrupt();

    try {
      checked.checkedGet(5, TimeUnit.SECONDS);
      fail();
    } catch (TestException e) {
      assertTrue(e.getCause() instanceof InterruptedException);
    }
  }

  public void testMakeChecked_mapsCancellation() throws Exception {
    SettableFuture<String> future = SettableFuture.create();

    CheckedFuture<String, TestException> checked = Futures.makeChecked(
        future, mapper);

    assertTrue(future.cancel(true)); // argument is ignored

    try {
      checked.get();
      fail();
    } catch (CancellationException expected) {}

    try {
      checked.get(5, TimeUnit.SECONDS);
      fail();
    } catch (CancellationException expected) {}

    try {
      checked.checkedGet();
      fail();
    } catch (TestException expected) {
      assertTrue(expected.getCause() instanceof CancellationException);
    }

    try {
      checked.checkedGet(5, TimeUnit.SECONDS);
      fail();
    } catch (TestException expected) {
      assertTrue(expected.getCause() instanceof CancellationException);
    }
  }

  public void testMakeChecked_propagatesFailedMappers() throws Exception {
    SettableFuture<String> future = SettableFuture.create();

    CheckedFuture<String, TestException> checked = Futures.makeChecked(
        future, new Function<Exception, TestException>() {
          @Override public TestException apply(Exception from) {
            throw new NullPointerException();
          }
    });

    future.setException(new Exception("failed"));

    try {
      checked.checkedGet();
      fail();
    } catch (NullPointerException expected) {}

    try {
      checked.checkedGet(5, TimeUnit.SECONDS);
      fail();
    } catch (NullPointerException expected) {}
  }

  public void testMakeChecked_listenersRunOnceCompleted() throws Exception {
    SettableFuture<String> future = SettableFuture.create();

    CheckedFuture<String, TestException> checked = Futures.makeChecked(
        future, new Function<Exception, TestException>() {
          @Override public TestException apply(Exception from) {
            throw new NullPointerException();
          }
    });

    ListenableFutureTester tester = new ListenableFutureTester(checked);
    tester.setUp();
    future.set(DATA1);
    tester.testCompletedFuture(DATA1);
    tester.tearDown();
  }

  public void testMakeChecked_listenersRunOnCancel() throws Exception {
    SettableFuture<String> future = SettableFuture.create();

    CheckedFuture<String, TestException> checked = Futures.makeChecked(
        future, new Function<Exception, TestException>() {
          @Override public TestException apply(Exception from) {
            throw new NullPointerException();
          }
    });

    ListenableFutureTester tester = new ListenableFutureTester(checked);
    tester.setUp();
    future.cancel(true); // argument is ignored
    tester.testCancelledFuture();
    tester.tearDown();
  }

  public void testMakeChecked_listenersRunOnFailure() throws Exception {
    SettableFuture<String> future = SettableFuture.create();

    CheckedFuture<String, TestException> checked = Futures.makeChecked(
        future, new Function<Exception, TestException>() {
          @Override public TestException apply(Exception from) {
            throw new NullPointerException();
          }
    });

    ListenableFutureTester tester = new ListenableFutureTester(checked);
    tester.setUp();
    future.setException(new Exception("failed"));
    tester.testFailedFuture("failed");
    tester.tearDown();
  }

  private interface MapperFunction extends Function<Throwable, Exception> {}

  private static final class OtherThrowable extends Throwable {}

  private static final Exception CHECKED_EXCEPTION = new Exception("mymessage");
  private static final Future<String> FAILED_FUTURE_CHECKED_EXCEPTION =
      immediateFailedFuture(CHECKED_EXCEPTION);
  private static final RuntimeException UNCHECKED_EXCEPTION =
      new RuntimeException("mymessage");
  private static final Future<String> FAILED_FUTURE_UNCHECKED_EXCEPTION =
      immediateFailedFuture(UNCHECKED_EXCEPTION);
  private static final RuntimeException RUNTIME_EXCEPTION =
      new RuntimeException();
  private static final OtherThrowable OTHER_THROWABLE = new OtherThrowable();
  private static final Future<String> FAILED_FUTURE_OTHER_THROWABLE =
      immediateFailedFuture(OTHER_THROWABLE);
  private static final Error ERROR = new Error("mymessage");
  private static final Future<String> FAILED_FUTURE_ERROR;
  /*
   * We can't write "= immediateFailedFuture(ERROR)" because setException
   * rethrows the error....
   */
  static {
    SettableFuture<String> f = SettableFuture.create();
    try {
      f.setException(ERROR);
    } catch (Error e) {
      assertEquals(e, ERROR);
    }
    FAILED_FUTURE_ERROR = f;
  }
  private static final Future<String> RUNTIME_EXCEPTION_FUTURE =
      new SimpleForwardingFuture<String>(FAILED_FUTURE_CHECKED_EXCEPTION) {
        @Override public String get() {
          throw RUNTIME_EXCEPTION;
        }

        @Override public String get(long timeout, TimeUnit unit) {
          throw RUNTIME_EXCEPTION;
        }
      };

  // Boring untimed-get tests:

  public void testGetUntimed_success()
      throws TwoArgConstructorException {
    assertEquals("foo",
        get(immediateFuture("foo"), TwoArgConstructorException.class));
  }

  public void testGetUntimed_interrupted() {
    Thread.currentThread().interrupt();
    try {
      get(immediateFuture("foo"), TwoArgConstructorException.class);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertTrue(expected.getCause() instanceof InterruptedException);
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  public void testGetUntimed_cancelled()
      throws TwoArgConstructorException {
    SettableFuture<String> future = SettableFuture.create();
    future.cancel(true);
    try {
      get(future, TwoArgConstructorException.class);
      fail();
    } catch (CancellationException expected) {
    }
  }

  public void testGetUntimed_ExecutionExceptionChecked() {
    try {
      get(FAILED_FUTURE_CHECKED_EXCEPTION, TwoArgConstructorException.class);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertEquals(CHECKED_EXCEPTION, expected.getCause());
    }
  }

  public void testGetUntimed_ExecutionExceptionUnchecked()
      throws TwoArgConstructorException {
    try {
      get(FAILED_FUTURE_UNCHECKED_EXCEPTION, TwoArgConstructorException.class);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertEquals(UNCHECKED_EXCEPTION, expected.getCause());
    }
  }

  public void testGetUntimed_ExecutionExceptionError()
      throws TwoArgConstructorException {
    try {
      get(FAILED_FUTURE_ERROR, TwoArgConstructorException.class);
      fail();
    } catch (ExecutionError expected) {
      assertEquals(ERROR, expected.getCause());
    }
  }

  public void testGetUntimed_ExecutionExceptionOtherThrowable() {
    try {
      get(FAILED_FUTURE_OTHER_THROWABLE, TwoArgConstructorException.class);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertEquals(OTHER_THROWABLE, expected.getCause());
    }
  }

  public void testGetUntimed_RuntimeException()
      throws TwoArgConstructorException {
    try {
      get(RUNTIME_EXCEPTION_FUTURE, TwoArgConstructorException.class);
      fail();
    } catch (RuntimeException expected) {
      assertEquals(RUNTIME_EXCEPTION, expected);
    }
  }

  // Boring timed-get tests:

  public void testGetTimed_success()
      throws TwoArgConstructorException {
    assertEquals("foo", get(
        immediateFuture("foo"), 0, SECONDS, TwoArgConstructorException.class));
  }

  public void testGetTimed_interrupted() {
    Thread.currentThread().interrupt();
    try {
      get(immediateFuture("foo"), 0, SECONDS, TwoArgConstructorException.class);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertTrue(expected.getCause() instanceof InterruptedException);
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  public void testGetTimed_cancelled()
      throws TwoArgConstructorException {
    SettableFuture<String> future = SettableFuture.create();
    future.cancel(true);
    try {
      get(future, 0, SECONDS, TwoArgConstructorException.class);
      fail();
    } catch (CancellationException expected) {
    }
  }

  public void testGetTimed_ExecutionExceptionChecked() {
    try {
      get(FAILED_FUTURE_CHECKED_EXCEPTION, 0, SECONDS,
          TwoArgConstructorException.class);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertEquals(CHECKED_EXCEPTION, expected.getCause());
    }
  }

  public void testGetTimed_ExecutionExceptionUnchecked()
      throws TwoArgConstructorException {
    try {
      get(FAILED_FUTURE_UNCHECKED_EXCEPTION, 0, SECONDS,
          TwoArgConstructorException.class);
      fail();
    } catch (UncheckedExecutionException expected) {
      assertEquals(UNCHECKED_EXCEPTION, expected.getCause());
    }
  }

  public void testGetTimed_ExecutionExceptionError()
      throws TwoArgConstructorException {
    try {
      get(FAILED_FUTURE_ERROR, 0, SECONDS, TwoArgConstructorException.class);
      fail();
    } catch (ExecutionError expected) {
      assertEquals(ERROR, expected.getCause());
    }
  }

  public void testGetTimed_ExecutionExceptionOtherThrowable() {
    try {
      get(FAILED_FUTURE_OTHER_THROWABLE, 0, SECONDS,
          TwoArgConstructorException.class);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertEquals(OTHER_THROWABLE, expected.getCause());
    }
  }

  public void testGetTimed_RuntimeException()
      throws TwoArgConstructorException {
    try {
      get(RUNTIME_EXCEPTION_FUTURE, 0, SECONDS,
          TwoArgConstructorException.class);
      fail();
    } catch (RuntimeException expected) {
      assertEquals(RUNTIME_EXCEPTION, expected);
    }
  }

  public void testGetTimed_TimeoutException() {
    SettableFuture<String> future = SettableFuture.create();
    try {
      get(future, 0, SECONDS, TwoArgConstructorException.class);
      fail();
    } catch (TwoArgConstructorException expected) {
      assertTrue(expected.getCause() instanceof TimeoutException);
    }
  }

  // Boring getUnchecked tests:

  public void testGetUnchecked_success() {
    assertEquals("foo", getUnchecked(immediateFuture("foo")));
  }

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

  // Edge case tests of the exception-construction code through untimed get():

  public void testGetUntimed_exceptionClassIsRuntimeException() {
    try {
      get(FAILED_FUTURE_CHECKED_EXCEPTION,
          TwoArgConstructorRuntimeException.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetUntimed_exceptionClassSomePublicConstructors() {
    try {
      get(FAILED_FUTURE_CHECKED_EXCEPTION,
          ExceptionWithSomePrivateConstructors.class);
      fail();
    } catch (ExceptionWithSomePrivateConstructors expected) {
    }
  }

  public void testGetUntimed_exceptionClassNoPublicConstructor()
      throws ExceptionWithPrivateConstructor {
    try {
      get(FAILED_FUTURE_CHECKED_EXCEPTION,
          ExceptionWithPrivateConstructor.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetUntimed_exceptionClassPublicConstructorWrongType()
      throws ExceptionWithWrongTypesConstructor {
    try {
      get(FAILED_FUTURE_CHECKED_EXCEPTION,
          ExceptionWithWrongTypesConstructor.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetUntimed_exceptionClassPrefersStringConstructor() {
    try {
      get(FAILED_FUTURE_CHECKED_EXCEPTION,
          ExceptionWithManyConstructors.class);
      fail();
    } catch (ExceptionWithManyConstructors expected) {
      assertTrue(expected.usedExpectedConstructor);
    }
  }

  public void testGetUntimed_exceptionClassUsedInitCause() {
    try {
      get(FAILED_FUTURE_CHECKED_EXCEPTION,
          ExceptionWithoutThrowableConstructor.class);
      fail();
    } catch (ExceptionWithoutThrowableConstructor expected) {
      ASSERT.that(expected.getMessage()).contains("mymessage");
      assertEquals(CHECKED_EXCEPTION, expected.getCause());
    }
  }

  public static final class TwoArgConstructorException extends Exception {
    public TwoArgConstructorException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static final class TwoArgConstructorRuntimeException
      extends RuntimeException {
    public TwoArgConstructorRuntimeException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static final class ExceptionWithPrivateConstructor extends Exception {
    private ExceptionWithPrivateConstructor(String message, Throwable cause) {
      super(message, cause);
    }
  }

  @SuppressWarnings("unused") // we're testing that they're not used
  public static final class ExceptionWithSomePrivateConstructors
      extends Exception {
    private ExceptionWithSomePrivateConstructors(String a) {
    }

    private ExceptionWithSomePrivateConstructors(String a, String b) {
    }

    public ExceptionWithSomePrivateConstructors(
        String a, String b, String c) {
    }

    private ExceptionWithSomePrivateConstructors(
        String a, String b, String c, String d) {
    }

    private ExceptionWithSomePrivateConstructors(
        String a, String b, String c, String d, String e) {
    }
  }

  public static final class ExceptionWithManyConstructors extends Exception {
    boolean usedExpectedConstructor;

    public ExceptionWithManyConstructors() {
    }

    public ExceptionWithManyConstructors(Integer i) {
    }

    public ExceptionWithManyConstructors(Throwable a) {
    }

    public ExceptionWithManyConstructors(Throwable a, Throwable b) {
    }

    public ExceptionWithManyConstructors(String s, Throwable b) {
      usedExpectedConstructor = true;
    }

    public ExceptionWithManyConstructors(
        Throwable a, Throwable b, Throwable c) {
    }

    public ExceptionWithManyConstructors(
        Throwable a, Throwable b, Throwable c, Throwable d) {
    }

    public ExceptionWithManyConstructors(
        Throwable a, Throwable b, Throwable c, Throwable d, Throwable e) {
    }

    public ExceptionWithManyConstructors(Throwable a, Throwable b, Throwable c,
        Throwable d, Throwable e, String s, Integer i) {
    }
  }

  public static final class ExceptionWithoutThrowableConstructor
      extends Exception {
    public ExceptionWithoutThrowableConstructor(String s) {
      super(s);
    }
  }

  public static final class ExceptionWithWrongTypesConstructor
      extends Exception {
    public ExceptionWithWrongTypesConstructor(Integer i, String s) {
      super(s);
    }
  }

  public void testNullArguments() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(ListenableFuture.class, Futures.immediateFuture(DATA1));
    tester.setDefault(ListenableFuture[].class,
        new ListenableFuture[] {Futures.immediateFuture(DATA1)});
    tester.setDefault(Future.class, Futures.immediateFuture(DATA1));
    tester.setDefault(Executor.class, MoreExecutors.sameThreadExecutor());
    tester.setDefault(Callable.class, Callables.returning(null));
    tester.setDefault(AsyncFunction.class, new AsyncFunction() {
      @Override
      public ListenableFuture apply(Object input) throws Exception {
        return immediateFuture(DATA1);
      }
    });

    FutureCallback<Object> callback =
        new FutureCallback<Object>() {
          @Override
          public void onSuccess(Object result) {}
          @Override
          public void onFailure(Throwable t) {}
        };
    tester.setDefault(FutureCallback.class, callback);

    tester.testAllPublicStaticMethods(Futures.class);
  }

  private static void failWithCause(Throwable cause, String message) {
    AssertionFailedError failure = new AssertionFailedError(message);
    failure.initCause(cause);
    throw failure;
  }
}
