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

import static com.google.common.base.Functions.identity;
import static com.google.common.base.Throwables.propagateIfInstanceOf;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.transform;
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
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
@GwtCompatible
public class FuturesAggregateTest extends AbstractFuturesTest {
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
    assertThat(expected).hasCauseThat().isEqualTo(exception);
  }

  public void testAllAsList_singleFailure() throws Exception {
    Throwable exception = new Throwable("failed");
    ListenableFuture<String> future = immediateFailedFuture(exception);
    ListenableFuture<List<String>> compound = allAsList(ImmutableList.of(future));

    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(compound));
    assertThat(expected).hasCauseThat().isEqualTo(exception);
  }

  public void testAllAsList_immediateFailure() throws Exception {
    Throwable exception = new Throwable("failed");
    ListenableFuture<String> future1 = immediateFailedFuture(exception);
    ListenableFuture<String> future2 = immediateFuture("results");
    ListenableFuture<List<String>> compound = allAsList(ImmutableList.of(future1, future2));

    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(compound));
    assertThat(expected).hasCauseThat().isEqualTo(exception);
  }

  public void testAllAsList_error() throws Exception {
    Error error = new Error("deliberate");
    SettableFuture<String> future1 = SettableFuture.create();
    ListenableFuture<String> future2 = immediateFuture("results");
    ListenableFuture<List<String>> compound = allAsList(ImmutableList.of(future1, future2));

    future1.setException(error);
    ExecutionException expected = assertThrows(ExecutionException.class, () -> getDone(compound));
    assertThat(expected).hasCauseThat().isEqualTo(error);
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
    MyException sameInstance = new MyException();
    ExecutionException expected =
        assertThrows(
            ExecutionException.class,
            () ->
                getDone(
                    allAsList(
                        immediateFailedFuture(sameInstance), immediateFailedFuture(sameInstance))));
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
        () ->
            /*
             * firstFuture just completed, but AggregateFuture hasn't yet had time to record the
             * exception in seenExceptions. When we complete secondFuture with the same exception,
             * we want for AggregateFuture to still detect that it's been previously seen.
             */
            secondFuture.setException(sameInstance),
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
        () ->
            /*
             * This is similar to the above test, but this time we're making sure that we recognize
             * that the output Future is done early not because of an exception but because of a
             * cancellation.
             */
            secondFuture.setException(subsequentFailure),
        directExecutor());
    firstFuture.cancel(false);

    assertThrows(CancellationException.class, () -> getDone(bulkFuture));
    assertThat(getOnlyElement(aggregateFutureLogHandler.getStoredLogRecords()).getThrown())
        .isEqualTo(subsequentFailure);
  }

  /**
   * Different exceptions happening on multiple futures with the same cause should not be logged.
   */
  public void testAllAsList_logging_same_cause() throws Exception {
    MyException exception1 = new MyException();
    MyException exception2 = new MyException();
    MyException exception3 = new MyException();

    MyException sameInstance = new MyException();
    exception1.initCause(sameInstance);
    exception2.initCause(sameInstance);
    exception3.initCause(exception2);
    ExecutionException expected =
        assertThrows(
            ExecutionException.class,
            () ->
                getDone(
                    allAsList(
                        immediateFailedFuture(exception1), immediateFailedFuture(exception3))));
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
    AsyncCallable<String> combiner = () -> stringFuture;

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
    Callable<String> combiner = () -> "hi";

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
            () -> {
              // Make this executor terminate after this task so that the test can tell when
              // futureResult has received resultOfCombiner.
              executor.shutdown();
              callableBlocking.await();
              return resultOfCombiner;
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
    assertThat(futureResult.get()).isEqualTo(expectedResult);
    assertThat(futureResult.toString())
        .matches("CombinedFuture@\\w+\\[status=SUCCESS, result=\\[java.lang.String@\\w+]]");
  }

  public void testWhenAllComplete_asyncError() throws Exception {
    Exception thrown = new RuntimeException("test");

    SettableFuture<Integer> futureInteger = SettableFuture.create();
    SettableFuture<Boolean> futureBoolean = SettableFuture.create();
    AsyncCallable<String> combiner =
        () -> {
          assertTrue(futureInteger.isDone());
          assertTrue(futureBoolean.isDone());
          return immediateFailedFuture(thrown);
        };

    ListenableFuture<String> futureResult =
        whenAllComplete(futureInteger, futureBoolean).callAsync(combiner, directExecutor());
    Integer integerPartial = 1;
    futureInteger.set(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.set(booleanPartial);

    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(futureResult));
    assertThat(expected).hasCauseThat().isEqualTo(thrown);
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
        () -> {
          inFunction.countDown();
          shouldCompleteFunction.await();
          return resultFuture;
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
        () -> {
          inFunction.countDown();
          try {
            new CountDownLatch(1).await(); // wait for interrupt
          } catch (InterruptedException expected) {
            gotException.countDown();
            throw expected;
          }
          return immediateFuture("a");
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
        () -> {
          assertTrue(futureInteger.isDone());
          assertTrue(futureBoolean.isDone());
          result[0] =
              createCombinedResult(
                  Futures.getUnchecked(futureInteger), Futures.getUnchecked(futureBoolean));
        };

    ListenableFuture<?> futureResult =
        whenAllComplete(futureInteger, futureBoolean).run(combiner, directExecutor());
    Integer integerPartial = 1;
    futureInteger.set(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.set(booleanPartial);
    futureResult.get();
    assertThat(result[0]).isEqualTo(createCombinedResult(integerPartial, booleanPartial));
  }

  public void testWhenAllComplete_runnableError() throws Exception {
    RuntimeException thrown = new RuntimeException("test");

    SettableFuture<Integer> futureInteger = SettableFuture.create();
    SettableFuture<Boolean> futureBoolean = SettableFuture.create();
    Runnable combiner =
        () -> {
          assertTrue(futureInteger.isDone());
          assertTrue(futureBoolean.isDone());
          throw thrown;
        };

    ListenableFuture<?> futureResult =
        whenAllComplete(futureInteger, futureBoolean).run(combiner, directExecutor());
    Integer integerPartial = 1;
    futureInteger.set(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.set(booleanPartial);

    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(futureResult));
    assertThat(expected).hasCauseThat().isEqualTo(thrown);
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
        () -> {
          inFunction.countDown();
          try {
            shouldCompleteFunction.await();
            combinerCompletedWithoutInterrupt.countDown();
          } catch (InterruptedException e) {
            // Ensure the thread's interrupted status is preserved.
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
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
        () -> {
          inFunction.countDown();
          try {
            new CountDownLatch(1).await(); // wait for interrupt
          } catch (InterruptedException expected) {
            // Ensure the thread's interrupted status is preserved.
            Thread.currentThread().interrupt();
            gotException.countDown();
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
        () -> {
          throw new AssertionFailedError("AsyncCallable should not have been called.");
        };

    ListenableFuture<String> futureResult =
        whenAllSucceed(futureInteger, futureBoolean).callAsync(combiner, directExecutor());
    PartialResultException partialResultException = new PartialResultException();
    futureInteger.setException(partialResultException);
    Boolean booleanPartial = true;
    futureBoolean.set(booleanPartial);
    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> getDone(futureResult));
    assertThat(expected).hasCauseThat().isEqualTo(partialResultException);
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testWhenAllSucceed_releasesInputFuturesUponSubmission() throws Exception {
    SettableFuture<Long> future1 = SettableFuture.create();
    SettableFuture<Long> future2 = SettableFuture.create();
    WeakReference<SettableFuture<Long>> future1Ref = new WeakReference<>(future1);
    WeakReference<SettableFuture<Long>> future2Ref = new WeakReference<>(future2);

    Callable<Long> combiner =
        () -> {
          throw new AssertionError();
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

  @J2ktIncompatible
  @GwtIncompatible
  public void testWhenAllComplete_releasesInputFuturesUponCancellation() throws Exception {
    SettableFuture<Long> future = SettableFuture.create();
    WeakReference<SettableFuture<Long>> futureRef = new WeakReference<>(future);

    Callable<Long> combiner =
        () -> {
          throw new AssertionError();
        };

    ListenableFuture<Long> unused = whenAllComplete(future).call(combiner, noOpScheduledExecutor());

    unused.cancel(false);
    future = null;

    // Future should be collected because whenAll*Complete* doesn't need to look at its result.
    GcFinalization.awaitClear(futureRef);
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testWhenAllSucceed_releasesCallable() throws Exception {
    @SuppressWarnings("AnonymousToLambda") // We want an instance that can be GCed.
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

    /** All the futures, together with human-readable names for use by {@link #smartToString}. */
    final ImmutableList<TestFuture> allFutures =
        ImmutableList.of(
            new TestFuture(doneSuccess, "doneSuccess", () -> {}),
            new TestFuture(doneFailed, "doneFailed", () -> {}),
            new TestFuture(doneCancelled, "doneCancelled", () -> {}),
            new TestFuture(doneRuntimeException, "doneRuntimeException", () -> {}),
            new TestFuture(delayedSuccess, "delayedSuccess", () -> delayedSuccess.set("b")),
            new TestFuture(
                delayedFailed, "delayedFailed", () -> delayedFailed.setException(new Exception())),
            new TestFuture(
                delayedCancelled, "delayedCancelled", () -> delayedCancelled.cancel(true)),
            new TestFuture(
                delayedRuntimeException,
                "delayedRuntimeException",
                () -> delegateForDelayedRuntimeException.set("Should never be seen")));

    final Function<ListenableFuture<String>, String> nameGetter =
        input -> {
          for (TestFuture future : allFutures) {
            if (future.future == input) {
              return future.name;
            }
          }
          throw new IllegalArgumentException(input.toString());
        };

    static boolean intersect(Set<?> a, Set<?> b) {
      return !intersection(a, b).isEmpty();
    }

    /**
     * Like {@code inputs.toString()}, but with the nonsense {@code toString} representations
     * replaced with the name of each future from {@link #allFutures}.
     */
    String smartToString(ImmutableSet<ListenableFuture<String>> inputs) {
      Iterable<String> inputNames = transform(inputs, nameGetter);
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
    ListenableFuture<List<@Nullable String>> compound = successfulAsList(future1, future2, future3);

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

    List<@Nullable String> results = getDone(compound);
    assertThat(results).containsExactly(DATA1, DATA2, DATA3).inOrder();
  }

  public void testSuccessfulAsList_emptyList() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    List<ListenableFuture<String>> futures = ImmutableList.of();
    ListenableFuture<List<@Nullable String>> compound = successfulAsList(futures);
    compound.addListener(listener, directExecutor());
    assertThat(getDone(compound)).isEmpty();
    assertTrue(listener.wasCalled());
  }

  public void testSuccessfulAsList_emptyArray() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    listener.expectCall();
    ListenableFuture<List<@Nullable String>> compound = successfulAsList();
    compound.addListener(listener, directExecutor());
    assertThat(getDone(compound)).isEmpty();
    assertTrue(listener.wasCalled());
  }

  public void testSuccessfulAsList_partialFailure() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<@Nullable String>> compound = successfulAsList(future1, future2);
    compound.addListener(listener, directExecutor());

    assertFalse(compound.isDone());
    future1.setException(new Throwable("failed1"));
    assertFalse(compound.isDone());
    listener.expectCall();
    future2.set(DATA2);
    assertTrue(listener.wasCalled());

    List<@Nullable String> results = getDone(compound);
    assertThat(results).containsExactly(null, DATA2).inOrder();
  }

  public void testSuccessfulAsList_totalFailure() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<@Nullable String>> compound = successfulAsList(future1, future2);
    compound.addListener(listener, directExecutor());

    assertFalse(compound.isDone());
    future1.setException(new Throwable("failed1"));
    assertFalse(compound.isDone());
    listener.expectCall();
    future2.setException(new Throwable("failed2"));
    assertTrue(listener.wasCalled());

    List<@Nullable String> results = getDone(compound);
    assertThat(results).containsExactly(null, null).inOrder();
  }

  public void testSuccessfulAsList_cancelled() throws Exception {
    SingleCallListener listener = new SingleCallListener();
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<@Nullable String>> compound = successfulAsList(future1, future2);
    compound.addListener(listener, directExecutor());

    assertFalse(compound.isDone());
    future1.cancel(true);
    assertFalse(compound.isDone());
    listener.expectCall();
    future2.set(DATA2);
    assertTrue(listener.wasCalled());

    List<@Nullable String> results = getDone(compound);
    assertThat(results).containsExactly(null, DATA2).inOrder();
  }

  public void testSuccessfulAsList_resultCancelled() throws Exception {
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<@Nullable String>> compound = successfulAsList(future1, future2);

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
    ListenableFuture<List<@Nullable String>> compound = successfulAsList(future1, future2);

    future1.addListener(
        () -> {
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
        },
        directExecutor());
    assertTrue(compound.cancel(false));
    assertTrue(compound.isCancelled());
    assertTrue(future1.isCancelled());
    assertFalse(future2.isCancelled());

    assertThrows(CancellationException.class, () -> getDone(compound));
  }

  public void testSuccessfulAsList_resultInterrupted() throws Exception {
    SettableFuture<String> future1 = SettableFuture.create();
    SettableFuture<String> future2 = SettableFuture.create();
    ListenableFuture<List<@Nullable String>> compound = successfulAsList(future1, future2);

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
    ListenableFuture<List<@Nullable String>> compound = successfulAsList(future1, future2, future3);
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

    List<@Nullable String> results = getDone(compound);
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
    ListenableFuture<List<@Nullable String>> output = successfulAsList(input);
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
}
