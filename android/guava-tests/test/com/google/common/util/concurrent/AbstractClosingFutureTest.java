/*
 * Copyright (C) 2017 The Guava Authors
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.asList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.util.concurrent.ClosingFuture.withoutCloser;
import static com.google.common.util.concurrent.Futures.immediateCancelledFuture;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.Reflection;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.StandardSubjectBuilder;
import com.google.common.util.concurrent.ClosingFuture.AsyncClosingCallable;
import com.google.common.util.concurrent.ClosingFuture.AsyncClosingFunction;
import com.google.common.util.concurrent.ClosingFuture.ClosingCallable;
import com.google.common.util.concurrent.ClosingFuture.ClosingFunction;
import com.google.common.util.concurrent.ClosingFuture.Combiner;
import com.google.common.util.concurrent.ClosingFuture.Combiner.AsyncCombiningCallable;
import com.google.common.util.concurrent.ClosingFuture.Combiner.CombiningCallable;
import com.google.common.util.concurrent.ClosingFuture.Combiner2.AsyncClosingFunction2;
import com.google.common.util.concurrent.ClosingFuture.Combiner2.ClosingFunction2;
import com.google.common.util.concurrent.ClosingFuture.Combiner3.ClosingFunction3;
import com.google.common.util.concurrent.ClosingFuture.Combiner4.ClosingFunction4;
import com.google.common.util.concurrent.ClosingFuture.Combiner5.ClosingFunction5;
import com.google.common.util.concurrent.ClosingFuture.DeferredCloser;
import com.google.common.util.concurrent.ClosingFuture.ValueAndCloser;
import com.google.common.util.concurrent.ClosingFuture.ValueAndCloserConsumer;
import java.io.Closeable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for {@link ClosingFuture}. Subclasses exercise either the {@link
 * ClosingFuture#finishToFuture()} or {@link
 * ClosingFuture#finishToValueAndCloser(ValueAndCloserConsumer, Executor)} paths to complete a
 * {@link ClosingFuture} pipeline.
 */
@NullUnmarked
@GwtIncompatible
@J2ktIncompatible
public abstract class AbstractClosingFutureTest extends TestCase {
  // TODO(dpb): Use Expect once that supports JUnit 3, or we can use JUnit 4.
  final List<AssertionError> failures = new ArrayList<>();
  final StandardSubjectBuilder expect =
      StandardSubjectBuilder.forCustomFailureStrategy(
          new FailureStrategy() {
            @Override
            public void fail(AssertionError failure) {
              failures.add(failure);
            }
          });

  final ListeningExecutorService executor = listeningDecorator(newSingleThreadExecutor());
  final ExecutorService closingExecutor = newSingleThreadExecutor();

  final TestCloseable closeable1 = new TestCloseable("closeable1");
  final TestCloseable closeable2 = new TestCloseable("closeable2");
  final TestCloseable closeable3 = new TestCloseable("closeable3");
  final TestCloseable closeable4 = new TestCloseable("closeable4");

  final Waiter waiter = new Waiter();
  final CountDownLatch futureCancelled = new CountDownLatch(1);
  final Exception exception = new Exception();
  final Closeable mockCloseable = mock(Closeable.class);

  @Override
  protected void tearDown() throws Exception {
    assertNoExpectedFailures();
    super.tearDown();
  }

  public void testFrom() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.from(executor.submit(Callables.returning(closeable1)))
            .transform(
                (DeferredCloser closer, TestCloseable v) -> {
                  assertThat(v).isSameInstanceAs(closeable1);
                  return "value";
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
  }

  public void testFrom_failedInput() {
    assertFinallyFailsWithException(failedClosingFuture());
  }

  public void testFrom_cancelledInput() {
    assertBecomesCanceled(ClosingFuture.from(immediateCancelledFuture()));
  }

  public void testEventuallyClosing() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor)
            .transform(
                (DeferredCloser closer, TestCloseable v) -> {
                  assertThat(v).isSameInstanceAs(closeable1);
                  assertStillOpen(closeable1);
                  return "value";
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testEventuallyClosing_failedInput() {
    assertFinallyFailsWithException(
        ClosingFuture.eventuallyClosing(
            Futures.<Closeable>immediateFailedFuture(exception), closingExecutor));
  }

  public void testEventuallyClosing_cancelledInput() {
    assertBecomesCanceled(
        ClosingFuture.eventuallyClosing(
            Futures.<Closeable>immediateCancelledFuture(), closingExecutor));
  }

  public void testEventuallyClosing_cancelledPipeline() {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.eventuallyClosing(
            executor.submit(
                waiter.waitFor(
                    (Callable<TestCloseable>)
                        () -> {
                          awaitUninterruptibly(futureCancelled);
                          return closeable1;
                        })),
            closingExecutor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    // not closed until the callable returns
    assertStillOpen(closeable1);
    waiter.awaitReturned();
    assertClosed(closeable1);
  }

  public void testEventuallyClosing_throws() {
    assertFinallyFailsWithException(
        ClosingFuture.eventuallyClosing(
            executor.submit(
                () -> {
                  throw exception;
                }),
            closingExecutor));
  }

  public void testSubmit() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.submit(
                closer -> {
                  closer.eventuallyClose(closeable1, closingExecutor);
                  closer.eventuallyClose(closeable2, closingExecutor);
                  return closeable3;
                },
                executor)
            .transform(
                (DeferredCloser closer, TestCloseable v) -> {
                  assertThat(v).isSameInstanceAs(closeable3);
                  assertStillOpen(closeable1, closeable2, closeable3);
                  return "value";
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
    assertStillOpen(closeable3);
  }

  public void testSubmit_cancelledPipeline() {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.submit(
            waiter.waitFor(
                (ClosingCallable<TestCloseable>)
                    closer -> {
                      awaitUninterruptibly(futureCancelled);
                      closer.eventuallyClose(closeable1, closingExecutor);
                      closer.eventuallyClose(closeable2, closingExecutor);
                      return closeable3;
                    }),
            executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2);
    assertStillOpen(closeable3);
  }

  public void testSubmit_throws() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.submit(
            closer -> {
              closer.eventuallyClose(closeable1, closingExecutor);
              closer.eventuallyClose(closeable2, closingExecutor);
              throw exception;
            },
            executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testSubmitAsync() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.submitAsync(
            closer -> {
              closer.eventuallyClose(closeable1, closingExecutor);
              return ClosingFuture.submit(
                  new ClosingCallable<TestCloseable>() {
                    @Override
                    public TestCloseable call(DeferredCloser deferredCloser) {
                      return closeable2;
                    }
                  },
                  directExecutor());
            },
            executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
    assertStillOpen(closeable2);
  }

  public void testSubmitAsync_cancelledPipeline() {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.submitAsync(
            waiter.waitFor(
                (AsyncClosingCallable<TestCloseable>)
                    closer -> {
                      awaitUninterruptibly(futureCancelled);
                      closer.eventuallyClose(closeable1, closingExecutor);
                      closer.eventuallyClose(closeable2, closingExecutor);
                      return ClosingFuture.submit(
                          new ClosingCallable<TestCloseable>() {
                            @Override
                            public TestCloseable call(DeferredCloser deferredCloser) {
                              deferredCloser.eventuallyClose(closeable3, closingExecutor);
                              return closeable3;
                            }
                          },
                          directExecutor());
                    }),
            executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2, closeable3);
  }

  public void testSubmitAsync_throws() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.submitAsync(
            closer -> {
              closer.eventuallyClose(closeable1, closingExecutor);
              closer.eventuallyClose(closeable2, closingExecutor);
              throw exception;
            },
            executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testAutoCloseable() throws Exception {
    AutoCloseable autoCloseable = closeable1::close;
    ClosingFuture<String> closingFuture =
        ClosingFuture.submit(
            closer -> {
              closer.eventuallyClose(autoCloseable, closingExecutor);
              return "foo";
            },
            executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("foo");
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testStatusFuture() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.submit(waiter.waitFor((ClosingCallable<String>) closer -> "value"), executor);
    ListenableFuture<?> statusFuture = closingFuture.statusFuture();
    waiter.awaitStarted();
    assertThat(statusFuture.isDone()).isFalse();
    waiter.awaitReturned();
    assertThat(getUninterruptibly(statusFuture)).isNull();
  }

  public void testStatusFuture_failure() {
    ClosingFuture<String> closingFuture =
        ClosingFuture.submit(
            waiter.waitFor(
                (ClosingCallable<String>)
                    closer -> {
                      throw exception;
                    }),
            executor);
    ListenableFuture<?> statusFuture = closingFuture.statusFuture();
    waiter.awaitStarted();
    assertThat(statusFuture.isDone()).isFalse();
    waiter.awaitReturned();
    assertThatFutureFailsWithException(statusFuture);
  }

  public void testStatusFuture_cancelDoesNothing() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.submit(waiter.waitFor((ClosingCallable<String>) closer -> "value"), executor);
    ListenableFuture<?> statusFuture = closingFuture.statusFuture();
    waiter.awaitStarted();
    assertThat(statusFuture.isDone()).isFalse();
    statusFuture.cancel(true);
    assertThat(statusFuture.isCancelled()).isTrue();
    waiter.awaitReturned();
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
  }

  public void testCancel_caught() throws Exception {
    ClosingFuture<String> step0 = ClosingFuture.from(immediateFuture("value 0"));
    ClosingFuture<String> step1 =
        step0.transform(
            (DeferredCloser closer, String v) -> {
              closer.eventuallyClose(closeable1, closingExecutor);
              return "value 1";
            },
            executor);
    Waiter step2Waiter = new Waiter();
    ClosingFuture<String> step2 =
        step1.transform(
            step2Waiter.waitFor(
                (ClosingFunction<String, String>)
                    (DeferredCloser closer, String v) -> {
                      closer.eventuallyClose(closeable2, closingExecutor);
                      return "value 2";
                    }),
            executor);
    ClosingFuture<String> step3 =
        step2.transform(
            (DeferredCloser closer, String input) -> {
              closer.eventuallyClose(closeable3, closingExecutor);
              return "value 3";
            },
            executor);
    Waiter step4Waiter = new Waiter();
    ClosingFuture<String> step4 =
        step3.catching(
            CancellationException.class,
            step4Waiter.waitFor(
                (ClosingFunction<CancellationException, String>)
                    (DeferredCloser closer, CancellationException input) -> {
                      closer.eventuallyClose(closeable4, closingExecutor);
                      return "value 4";
                    }),
            executor);

    // Pause in step 2.
    step2Waiter.awaitStarted();

    // Everything should still be open.
    assertStillOpen(closeable1, closeable2, closeable3, closeable4);

    // Cancel step 3, resume step 2, and pause in step 4.
    assertWithMessage("step3.cancel()").that(step3.cancel(false)).isTrue();
    step2Waiter.awaitReturned();
    step4Waiter.awaitStarted();

    // Step 1 is not cancelled because it was done.
    assertWithMessage("step1.statusFuture().isCancelled()")
        .that(step1.statusFuture().isCancelled())
        .isFalse();
    // But its closeable is closed.
    assertClosed(closeable1);

    // Step 2 is cancelled because it wasn't complete.
    assertWithMessage("step2.statusFuture().isCancelled()")
        .that(step2.statusFuture().isCancelled())
        .isTrue();
    // Its closeable is closed.
    assertClosed(closeable2);

    // Step 3 was cancelled before it began
    assertWithMessage("step3.statusFuture().isCancelled()")
        .that(step3.statusFuture().isCancelled())
        .isTrue();
    // Its closeable is still open.
    assertStillOpen(closeable3);

    // Step 4 is not cancelled, because it caught the cancellation.
    assertWithMessage("step4.statusFuture().isCancelled()")
        .that(step4.statusFuture().isCancelled())
        .isFalse();
    // Its closeable isn't closed yet.
    assertStillOpen(closeable4);

    // Resume step 4 and complete.
    step4Waiter.awaitReturned();
    assertThat(getFinalValue(step4)).isEqualTo("value 4");

    // Step 4's closeable is now closed.
    assertClosed(closeable4);
    // Step 3 still never ran, so its closeable should still be open.
    assertStillOpen(closeable3);
  }

  public void testTransform() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.from(immediateFuture("value"))
            .transform(
                (DeferredCloser closer, String v) -> {
                  closer.eventuallyClose(closeable1, closingExecutor);
                  closer.eventuallyClose(closeable2, closingExecutor);
                  return closeable3;
                },
                executor)
            .transform(
                (DeferredCloser closer, TestCloseable v) -> {
                  assertThat(v).isSameInstanceAs(closeable3);
                  assertStillOpen(closeable1, closeable2, closeable3);
                  return "value";
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
    assertStillOpen(closeable3);
  }

  public void testTransform_cancelledPipeline() {
    String value = "value";
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.from(immediateFuture(value))
            .transform(
                (DeferredCloser closer, String v) ->
                    closer.eventuallyClose(closeable1, closingExecutor),
                executor)
            .transform(
                waiter.waitFor(
                    (ClosingFunction<TestCloseable, TestCloseable>)
                        (DeferredCloser closer, TestCloseable v) -> {
                          awaitUninterruptibly(futureCancelled);
                          closer.eventuallyClose(closeable2, closingExecutor);
                          closer.eventuallyClose(closeable3, closingExecutor);
                          return closeable4;
                        }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2, closeable3);
    assertStillOpen(closeable4);
  }

  public void testTransform_throws() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.from(immediateFuture("value"))
            .transform(
                (DeferredCloser closer, String v) -> {
                  closer.eventuallyClose(closeable1, closingExecutor);
                  closer.eventuallyClose(closeable2, closingExecutor);
                  throw exception;
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testTransformAsync() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.from(immediateFuture("value"))
            .transformAsync(
                (DeferredCloser closer, String v) -> {
                  closer.eventuallyClose(closeable1, closingExecutor);
                  closer.eventuallyClose(closeable2, closingExecutor);
                  return ClosingFuture.eventuallyClosing(
                      immediateFuture(closeable3), closingExecutor);
                },
                executor)
            .transform(
                (DeferredCloser closer, TestCloseable v) -> {
                  assertThat(v).isSameInstanceAs(closeable3);
                  assertStillOpen(closeable1, closeable2, closeable3);
                  return "value";
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2, closeable3);
  }

  public void testTransformAsync_cancelledPipeline() {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.from(immediateFuture("value"))
            .transformAsync(
                waiter.waitFor(
                    (AsyncClosingFunction<String, TestCloseable>)
                        (DeferredCloser closer, String v) -> {
                          awaitUninterruptibly(futureCancelled);
                          closer.eventuallyClose(closeable1, closingExecutor);
                          closer.eventuallyClose(closeable2, closingExecutor);
                          return ClosingFuture.eventuallyClosing(
                              immediateFuture(closeable3), closingExecutor);
                        }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    // not closed until the function returns
    assertStillOpen(closeable1, closeable2, closeable3);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2, closeable3);
  }

  public void testTransformAsync_throws() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.from(immediateFuture("value"))
            .transformAsync(
                (DeferredCloser closer, String v) -> {
                  closer.eventuallyClose(closeable1, closingExecutor);
                  closer.eventuallyClose(closeable2, closingExecutor);
                  throw exception;
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testTransformAsync_failed() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.from(immediateFuture("value"))
            .transformAsync(
                (DeferredCloser closer, String v) -> {
                  closer.eventuallyClose(closeable1, closingExecutor);
                  closer.eventuallyClose(closeable2, closingExecutor);
                  return failedClosingFuture();
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testTransformAsync_withoutCloser() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.submit(
                closer -> closer.eventuallyClose(closeable1, closingExecutor), executor)
            .transformAsync(
                withoutCloser(
                    (TestCloseable v) -> {
                      assertThat(v).isSameInstanceAs(closeable1);
                      assertStillOpen(closeable1);
                      return immediateFuture("value");
                    }),
                executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testWhenAllComplete_call() throws Exception {
    ClosingFuture<String> input1 = ClosingFuture.from(immediateFuture("value1"));
    ClosingFuture<Object> input2Failed = failedClosingFuture();
    ClosingFuture<String> nonInput = ClosingFuture.from(immediateFuture("value3"));
    AtomicReference<ClosingFuture.Peeker> capturedPeeker = new AtomicReference<>();
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllComplete(ImmutableList.of(input1, input2Failed))
            .call(
                (closer, peeker) -> {
                  closer.eventuallyClose(closeable1, closingExecutor);
                  assertThat(peeker.getDone(input1)).isSameInstanceAs("value1");
                  assertThrows(ExecutionException.class, () -> peeker.getDone(input2Failed));
                  assertThrows(IllegalArgumentException.class, () -> peeker.getDone(nonInput));
                  capturedPeeker.set(peeker);
                  return closeable2;
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable2);
    assertClosed(closeable1);
    assertThrows(IllegalStateException.class, () -> capturedPeeker.get().getDone(input1));
  }

  public void testWhenAllComplete_call_cancelledPipeline() {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllComplete(
                ImmutableList.of(
                    ClosingFuture.from(immediateFuture(closeable1)),
                    ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor)))
            .call(
                waiter.waitFor(
                    (CombiningCallable<TestCloseable>)
                        (closer, peeker) -> {
                          awaitUninterruptibly(futureCancelled);
                          closer.eventuallyClose(closeable1, closingExecutor);
                          return closeable3;
                        }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2);
    assertStillOpen(closeable3);
  }

  public void testWhenAllComplete_call_throws() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllComplete(
                ImmutableList.of(
                    ClosingFuture.from(immediateFuture(closeable1)),
                    ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor)))
            .call(
                (closer, peeker) -> {
                  closer.eventuallyClose(closeable3, closingExecutor);
                  throw exception;
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  public void testWhenAllComplete_callAsync() throws Exception {
    ClosingFuture<String> input1 = ClosingFuture.from(immediateFuture("value1"));
    ClosingFuture<Object> input2Failed = failedClosingFuture();
    ClosingFuture<String> nonInput = ClosingFuture.from(immediateFuture("value3"));
    AtomicReference<ClosingFuture.Peeker> capturedPeeker = new AtomicReference<>();
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllComplete(ImmutableList.of(input1, input2Failed))
            .callAsync(
                (closer, peeker) -> {
                  closer.eventuallyClose(closeable1, closingExecutor);
                  assertThat(peeker.getDone(input1)).isSameInstanceAs("value1");
                  assertThrows(ExecutionException.class, () -> peeker.getDone(input2Failed));
                  assertThrows(IllegalArgumentException.class, () -> peeker.getDone(nonInput));
                  capturedPeeker.set(peeker);
                  return ClosingFuture.eventuallyClosing(
                      immediateFuture(closeable2), closingExecutor);
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
    assertThrows(IllegalStateException.class, () -> capturedPeeker.get().getDone(input1));
  }

  public void testWhenAllComplete_callAsync_cancelledPipeline() {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllComplete(
                ImmutableList.of(
                    ClosingFuture.from(immediateFuture(closeable1)),
                    ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor)))
            .callAsync(
                waiter.waitFor(
                    (AsyncCombiningCallable<TestCloseable>)
                        (closer, peeker) -> {
                          awaitUninterruptibly(futureCancelled);
                          closer.eventuallyClose(closeable1, closingExecutor);
                          return ClosingFuture.eventuallyClosing(
                              immediateFuture(closeable3), closingExecutor);
                        }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2, closeable3);
  }

  public void testWhenAllComplete_callAsync_throws() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllComplete(
                ImmutableList.of(
                    ClosingFuture.from(immediateFuture(closeable1)),
                    ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor)))
            .callAsync(
                (closer, peeker) -> {
                  closer.eventuallyClose(closeable3, closingExecutor);
                  throw exception;
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  // We don't need to test the happy case for SuccessfulCombiner.call(Async) because it's the same
  // as Combiner.

  public void testWhenAllSucceed_call_failedInput() {
    assertFinallyFailsWithException(
        ClosingFuture.whenAllSucceed(
                ImmutableList.of(
                    ClosingFuture.from(immediateFuture("value")), failedClosingFuture()))
            .call(
                (closer, peeker) -> {
                  expect.fail();
                  throw new AssertionError();
                },
                executor));
  }

  public void testWhenAllSucceed_callAsync_failedInput() {
    assertFinallyFailsWithException(
        ClosingFuture.whenAllSucceed(
                ImmutableList.of(
                    ClosingFuture.from(immediateFuture("value")), failedClosingFuture()))
            .callAsync(
                (closer, peeker) -> {
                  expect.fail();
                  throw new AssertionError();
                },
                executor));
  }

  public void testWhenAllSucceed2_call() throws ExecutionException {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                ClosingFuture.from(immediateFuture("value1")))
            .call(
                (DeferredCloser closer, TestCloseable v1, String v2) -> {
                  assertThat(v1).isEqualTo(closeable1);
                  assertThat(v2).isEqualTo("value1");
                  assertStillOpen(closeable1);
                  closer.eventuallyClose(closeable2, closingExecutor);
                  return closeable2;
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testWhenAllSucceed2_call_failedInput() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                failedClosingFuture())
            .call(
                (DeferredCloser closer, TestCloseable v1, Object v2) -> {
                  expect.fail();
                  throw new AssertionError();
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testWhenAllSucceed2_call_cancelledPipeline() {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.from(immediateFuture(closeable2)))
            .call(
                waiter.waitFor(
                    (DeferredCloser closer, TestCloseable v1, TestCloseable v2) -> {
                      awaitUninterruptibly(futureCancelled);
                      closer.eventuallyClose(closeable1, closingExecutor);
                      closer.eventuallyClose(closeable2, closingExecutor);
                      return closeable3;
                    }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    // not closed until the function returns
    assertStillOpen(closeable1, closeable2);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2);
    assertStillOpen(closeable3);
  }

  public void testWhenAllSucceed2_call_throws() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor))
            .call(
                (DeferredCloser closer, TestCloseable v1, TestCloseable v2) -> {
                  closer.eventuallyClose(closeable3, closingExecutor);
                  throw exception;
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  public void testWhenAllSucceed2_callAsync() throws ExecutionException {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                ClosingFuture.from(immediateFuture("value1")))
            .callAsync(
                (DeferredCloser closer, TestCloseable v1, String v2) -> {
                  assertThat(v1).isEqualTo(closeable1);
                  assertThat(v2).isEqualTo("value1");
                  assertStillOpen(closeable1);
                  closer.eventuallyClose(closeable2, closingExecutor);
                  return ClosingFuture.eventuallyClosing(
                      immediateFuture(closeable3), closingExecutor);
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable3);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2, closeable3);
  }

  public void testWhenAllSucceed2_callAsync_failedInput() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                failedClosingFuture())
            .callAsync(
                (DeferredCloser closer, TestCloseable v1, Object v2) -> {
                  expect.fail();
                  throw new AssertionError();
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testWhenAllSucceed2_callAsync_cancelledPipeline() {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.from(immediateFuture(closeable2)))
            .callAsync(
                waiter.waitFor(
                    (DeferredCloser closer, TestCloseable v1, TestCloseable v2) -> {
                      awaitUninterruptibly(futureCancelled);
                      closer.eventuallyClose(closeable1, closingExecutor);
                      closer.eventuallyClose(closeable2, closingExecutor);
                      return ClosingFuture.eventuallyClosing(
                          immediateFuture(closeable3), closingExecutor);
                    }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    // not closed until the function returns
    assertStillOpen(closeable1, closeable2, closeable3);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2, closeable3);
  }

  public void testWhenAllSucceed2_callAsync_throws() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor))
            .callAsync(
                (DeferredCloser closer, TestCloseable v1, TestCloseable v2) -> {
                  closer.eventuallyClose(closeable3, closingExecutor);
                  throw exception;
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  public void testWhenAllSucceed3_call() throws ExecutionException {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                ClosingFuture.from(immediateFuture("value2")),
                ClosingFuture.from(immediateFuture("value3")))
            .call(
                (DeferredCloser closer, TestCloseable v1, String v2, String v3) -> {
                  assertThat(v1).isEqualTo(closeable1);
                  assertThat(v2).isEqualTo("value2");
                  assertThat(v3).isEqualTo("value3");
                  assertStillOpen(closeable1);
                  closer.eventuallyClose(closeable2, closingExecutor);
                  return closeable2;
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testWhenAllSucceed3_call_failedInput() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                failedClosingFuture(),
                ClosingFuture.from(immediateFuture("value3")))
            .call(
                (DeferredCloser closer, TestCloseable v1, Object v2, String v3) -> {
                  expect.fail();
                  throw new AssertionError();
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testWhenAllSucceed3_call_cancelledPipeline() {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.from(immediateFuture(closeable2)),
                ClosingFuture.from(immediateFuture("value3")))
            .call(
                waiter.waitFor(
                    (DeferredCloser closer, TestCloseable v1, TestCloseable v2, String v3) -> {
                      awaitUninterruptibly(futureCancelled);
                      closer.eventuallyClose(closeable1, closingExecutor);
                      closer.eventuallyClose(closeable2, closingExecutor);
                      return closeable3;
                    }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    // not closed until the function returns
    assertStillOpen(closeable1, closeable2);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2);
    assertStillOpen(closeable3);
  }

  public void testWhenAllSucceed3_call_throws() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor),
                ClosingFuture.from(immediateFuture("value3")))
            .call(
                (DeferredCloser closer, TestCloseable v1, TestCloseable v2, String v3) -> {
                  closer.eventuallyClose(closeable3, closingExecutor);
                  throw exception;
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  public void testWhenAllSucceed4_call() throws ExecutionException {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                ClosingFuture.from(immediateFuture("value2")),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")))
            .call(
                (DeferredCloser closer, TestCloseable v1, String v2, String v3, String v4) -> {
                  assertThat(v1).isEqualTo(closeable1);
                  assertThat(v2).isEqualTo("value2");
                  assertThat(v3).isEqualTo("value3");
                  assertThat(v4).isEqualTo("value4");
                  assertStillOpen(closeable1);
                  closer.eventuallyClose(closeable2, closingExecutor);
                  return closeable2;
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testWhenAllSucceed4_call_failedInput() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                failedClosingFuture(),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")))
            .call(
                (DeferredCloser closer, TestCloseable v1, Object v2, String v3, String v4) -> {
                  expect.fail();
                  throw new AssertionError();
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testWhenAllSucceed4_call_cancelledPipeline() {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.from(immediateFuture(closeable2)),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")))
            .call(
                waiter.waitFor(
                    (DeferredCloser closer,
                        TestCloseable v1,
                        TestCloseable v2,
                        String v3,
                        String v4) -> {
                      awaitUninterruptibly(futureCancelled);
                      closer.eventuallyClose(closeable1, closingExecutor);
                      closer.eventuallyClose(closeable2, closingExecutor);
                      return closeable3;
                    }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    // not closed until the function returns
    assertStillOpen(closeable1, closeable2);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2);
    assertStillOpen(closeable3);
  }

  public void testWhenAllSucceed4_call_throws() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")))
            .call(
                (DeferredCloser closer,
                    TestCloseable v1,
                    TestCloseable v2,
                    String v3,
                    String v4) -> {
                  closer.eventuallyClose(closeable3, closingExecutor);
                  throw exception;
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  public void testWhenAllSucceed5_call() throws ExecutionException {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                ClosingFuture.from(immediateFuture("value2")),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")),
                ClosingFuture.from(immediateFuture("value5")))
            .call(
                (DeferredCloser closer,
                    TestCloseable v1,
                    String v2,
                    String v3,
                    String v4,
                    String v5) -> {
                  assertThat(v1).isEqualTo(closeable1);
                  assertThat(v2).isEqualTo("value2");
                  assertThat(v3).isEqualTo("value3");
                  assertThat(v4).isEqualTo("value4");
                  assertThat(v5).isEqualTo("value5");
                  assertStillOpen(closeable1);
                  closer.eventuallyClose(closeable2, closingExecutor);
                  return closeable2;
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testWhenAllSucceed5_call_failedInput() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                failedClosingFuture(),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")),
                ClosingFuture.from(immediateFuture("value5")))
            .call(
                (DeferredCloser closer,
                    TestCloseable v1,
                    Object v2,
                    String v3,
                    String v4,
                    String v5) -> {
                  expect.fail();
                  throw new AssertionError();
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testWhenAllSucceed5_call_cancelledPipeline() {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.from(immediateFuture(closeable2)),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")),
                ClosingFuture.from(immediateFuture("value5")))
            .call(
                waiter.waitFor(
                    (DeferredCloser closer,
                        TestCloseable v1,
                        TestCloseable v2,
                        String v3,
                        String v4,
                        String v5) -> {
                      awaitUninterruptibly(futureCancelled);
                      closer.eventuallyClose(closeable1, closingExecutor);
                      closer.eventuallyClose(closeable2, closingExecutor);
                      return closeable3;
                    }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    // not closed until the function returns
    assertStillOpen(closeable1, closeable2);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2);
    assertStillOpen(closeable3);
  }

  public void testWhenAllSucceed5_call_throws() {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")),
                ClosingFuture.from(immediateFuture("value5")))
            .call(
                (DeferredCloser closer,
                    TestCloseable v1,
                    TestCloseable v2,
                    String v3,
                    String v4,
                    String v5) -> {
                  closer.eventuallyClose(closeable3, closingExecutor);
                  throw exception;
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  public void testTransform_preventsFurtherOperations() {
    ClosingFuture<String> closingFuture = ClosingFuture.from(immediateFuture("value1"));
    ClosingFuture<String> unused = closingFuture.transform((closer, v) -> "value2", executor);
    assertDerivingThrowsIllegalStateException(closingFuture);
    assertFinalStepThrowsIllegalStateException(closingFuture);
  }

  public void testTransformAsync_preventsFurtherOperations() {
    ClosingFuture<String> closingFuture = ClosingFuture.from(immediateFuture("value1"));
    ClosingFuture<String> unused =
        closingFuture.transformAsync(
            (closer, v) -> ClosingFuture.from(immediateFuture("value2")), executor);
    assertDerivingThrowsIllegalStateException(closingFuture);
    assertFinalStepThrowsIllegalStateException(closingFuture);
  }

  public void testCatching_preventsFurtherOperations() {
    ClosingFuture<String> closingFuture = ClosingFuture.from(immediateFuture("value1"));
    ClosingFuture<String> unused =
        closingFuture.catching(Exception.class, (closer, x) -> "value2", executor);
    assertDerivingThrowsIllegalStateException(closingFuture);
    assertFinalStepThrowsIllegalStateException(closingFuture);
  }

  public void testCatchingAsync_preventsFurtherOperations() {
    ClosingFuture<String> closingFuture = ClosingFuture.from(immediateFuture("value1"));
    ClosingFuture<String> unused =
        closingFuture.catchingAsync(
            Exception.class, withoutCloser(x -> immediateFuture("value2")), executor);
    assertDerivingThrowsIllegalStateException(closingFuture);
    assertFinalStepThrowsIllegalStateException(closingFuture);
  }

  public void testWhenAllComplete_preventsFurtherOperations() {
    ClosingFuture<String> closingFuture = ClosingFuture.from(immediateFuture("value1"));
    Combiner unused = ClosingFuture.whenAllComplete(asList(closingFuture));
    assertDerivingThrowsIllegalStateException(closingFuture);
    assertFinalStepThrowsIllegalStateException(closingFuture);
  }

  public void testWhenAllSucceed_preventsFurtherOperations() {
    ClosingFuture<String> closingFuture = ClosingFuture.from(immediateFuture("value1"));
    Combiner unused = ClosingFuture.whenAllSucceed(asList(closingFuture));
    assertDerivingThrowsIllegalStateException(closingFuture);
    assertFinalStepThrowsIllegalStateException(closingFuture);
  }

  protected final void assertDerivingThrowsIllegalStateException(
      ClosingFuture<String> closingFuture) {
    assertThrows(
        IllegalStateException.class,
        () -> closingFuture.transform((closer3, v1) -> "value3", executor));
    assertThrows(
        IllegalStateException.class,
        () ->
            closingFuture.transformAsync(
                (closer2, v) -> ClosingFuture.from(immediateFuture("value3")), executor));
    assertThrows(
        IllegalStateException.class,
        () -> closingFuture.catching(Exception.class, (closer1, x1) -> "value3", executor));
    assertThrows(
        IllegalStateException.class,
        () ->
            closingFuture.catchingAsync(
                Exception.class,
                (closer, x) -> ClosingFuture.from(immediateFuture("value3")),
                executor));
    assertThrows(
        IllegalStateException.class, () -> ClosingFuture.whenAllComplete(asList(closingFuture)));
    assertThrows(
        IllegalStateException.class, () -> ClosingFuture.whenAllSucceed(asList(closingFuture)));
  }

  /** Asserts that marking this step a final step throws {@link IllegalStateException}. */
  protected void assertFinalStepThrowsIllegalStateException(ClosingFuture<?> closingFuture) {
    assertThrows(IllegalStateException.class, () -> closingFuture.finishToFuture());

    NoOpValueAndCloserConsumer<Object> consumer = new NoOpValueAndCloserConsumer<>();
    assertThrows(
        IllegalStateException.class,
        () -> closingFuture.finishToValueAndCloser(consumer, executor));
  }

  // Avoid infinite recursion if a closeable's close() method throws RejectedExecutionException and
  // is closed using the direct executor.
  public void testCloseThrowsRejectedExecutionException() throws Exception {
    doThrow(new RejectedExecutionException()).when(mockCloseable).close();
    ClosingFuture<Closeable> closingFuture =
        ClosingFuture.submit(
            closer -> closer.eventuallyClose(mockCloseable, directExecutor()), executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo(mockCloseable);
    waitUntilClosed(closingFuture);
    verify(mockCloseable, timeout(1000)).close();
  }

  /**
   * Marks the given step final, waits for it to be finished, and returns the value.
   *
   * @throws ExecutionException if the step failed
   * @throws CancellationException if the step was cancelled
   */
  abstract <T> T getFinalValue(ClosingFuture<T> closingFuture) throws ExecutionException;

  /** Marks the given step final, cancels it, and waits for the cancellation to happen. */
  abstract void cancelFinalStepAndWait(ClosingFuture<TestCloseable> closingFuture);

  /**
   * Marks the given step final and waits for it to fail. Expects the failure exception to match
   * {@link AbstractClosingFutureTest#exception}.
   */
  abstract void assertFinallyFailsWithException(ClosingFuture<?> closingFuture);

  /** Waits for the given step to be canceled. */
  abstract void assertBecomesCanceled(ClosingFuture<?> closingFuture);

  /** Waits for the given step's closeables to be closed. */
  void waitUntilClosed(ClosingFuture<?> closingFuture) {
    assertTrue(awaitUninterruptibly(closingFuture.whenClosedCountDown(), 1, SECONDS));
  }

  void assertThatFutureFailsWithException(Future<?> future) {
    ExecutionException e = assertThrows(ExecutionException.class, () -> getUninterruptibly(future));
    assertThat(e).hasCauseThat().isEqualTo(exception);
  }

  static void assertThatFutureBecomesCancelled(Future<?> future) {
    assertThrows(CancellationException.class, () -> getUninterruptibly(future));
  }

  private static void assertStillOpen(TestCloseable closeable1, TestCloseable... moreCloseables) {
    for (TestCloseable closeable : asList(closeable1, moreCloseables)) {
      assertWithMessage("%s.stillOpen()", closeable).that(closeable.stillOpen()).isTrue();
    }
  }

  static void assertClosed(TestCloseable closeable1, TestCloseable... moreCloseables) {
    for (TestCloseable closeable : asList(closeable1, moreCloseables)) {
      assertWithMessage("%s.isClosed()", closeable).that(closeable.awaitClosed()).isTrue();
    }
  }

  private ClosingFuture<Object> failedClosingFuture() {
    return ClosingFuture.from(immediateFailedFuture(exception));
  }

  private void assertNoExpectedFailures() {
    assertWithMessage("executor was shut down")
        .that(shutdownAndAwaitTermination(executor, 10, SECONDS))
        .isTrue();
    assertWithMessage("closingExecutor was shut down")
        .that(shutdownAndAwaitTermination(closingExecutor, 10, SECONDS))
        .isTrue();
    if (!failures.isEmpty()) {
      StringWriter message = new StringWriter();
      PrintWriter writer = new PrintWriter(message);
      writer.println("Expected no failures, but found:");
      for (AssertionError failure : failures) {
        failure.printStackTrace(writer);
      }
      failures.clear();
      assertWithMessage(message.toString()).fail();
    }
  }

  static final class TestCloseable implements Closeable {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final String name;

    TestCloseable(String name) {
      this.name = name;
    }

    @Override
    public void close() {
      latch.countDown();
    }

    boolean awaitClosed() {
      return awaitUninterruptibly(latch, 10, SECONDS);
    }

    boolean stillOpen() {
      return !awaitUninterruptibly(latch, 1, SECONDS);
    }

    @Override
    public String toString() {
      return name;
    }
  }

  static final class Waiter {
    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch canReturn = new CountDownLatch(1);
    private final CountDownLatch returned = new CountDownLatch(1);
    private Object proxy;

    @SuppressWarnings("unchecked") // proxy for a generic class
    <V> Callable<V> waitFor(Callable<V> callable) {
      return waitFor(callable, Callable.class);
    }

    @SuppressWarnings("unchecked") // proxy for a generic class
    <V> ClosingCallable<V> waitFor(ClosingCallable<V> closingCallable) {
      return waitFor(closingCallable, ClosingCallable.class);
    }

    @SuppressWarnings("unchecked") // proxy for a generic class
    <V> AsyncClosingCallable<V> waitFor(AsyncClosingCallable<V> asyncClosingCallable) {
      return waitFor(asyncClosingCallable, AsyncClosingCallable.class);
    }

    @SuppressWarnings("unchecked") // proxy for a generic class
    <T, U> ClosingFunction<T, U> waitFor(ClosingFunction<T, U> closingFunction) {
      return waitFor(closingFunction, ClosingFunction.class);
    }

    @SuppressWarnings("unchecked") // proxy for a generic class
    <T, U> AsyncClosingFunction<T, U> waitFor(AsyncClosingFunction<T, U> asyncClosingFunction) {
      return waitFor(asyncClosingFunction, AsyncClosingFunction.class);
    }

    @SuppressWarnings("unchecked") // proxy for a generic class
    <V> CombiningCallable<V> waitFor(CombiningCallable<V> combiningCallable) {
      return waitFor(combiningCallable, CombiningCallable.class);
    }

    @SuppressWarnings("unchecked") // proxy for a generic class
    <V> AsyncCombiningCallable<V> waitFor(AsyncCombiningCallable<V> asyncCombiningCallable) {
      return waitFor(asyncCombiningCallable, AsyncCombiningCallable.class);
    }

    @SuppressWarnings("unchecked") // proxy for a generic class
    <V1, V2, U> ClosingFunction2<V1, V2, U> waitFor(ClosingFunction2<V1, V2, U> closingFunction2) {
      return waitFor(closingFunction2, ClosingFunction2.class);
    }

    @SuppressWarnings("unchecked") // proxy for a generic class
    <V1, V2, U> AsyncClosingFunction2<V1, V2, U> waitFor(
        AsyncClosingFunction2<V1, V2, U> asyncClosingFunction2) {
      return waitFor(asyncClosingFunction2, AsyncClosingFunction2.class);
    }

    @SuppressWarnings("unchecked") // proxy for a generic class
    <V1, V2, V3, U> ClosingFunction3<V1, V2, V3, U> waitFor(
        ClosingFunction3<V1, V2, V3, U> closingFunction3) {
      return waitFor(closingFunction3, ClosingFunction3.class);
    }

    @SuppressWarnings("unchecked") // proxy for a generic class
    <V1, V2, V3, V4, U> ClosingFunction4<V1, V2, V3, V4, U> waitFor(
        ClosingFunction4<V1, V2, V3, V4, U> closingFunction4) {
      return waitFor(closingFunction4, ClosingFunction4.class);
    }

    @SuppressWarnings("unchecked") // proxy for a generic class
    <V1, V2, V3, V4, V5, U> ClosingFunction5<V1, V2, V3, V4, V5, U> waitFor(
        ClosingFunction5<V1, V2, V3, V4, V5, U> closingFunction5) {
      return waitFor(closingFunction5, ClosingFunction5.class);
    }

    <T> T waitFor(T delegate, Class<T> type) {
      checkState(proxy == null);
      T proxyObject =
          Reflection.newProxy(
              type,
              new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                  if (!method.getDeclaringClass().equals(type)) {
                    return method.invoke(delegate, args);
                  }
                  checkState(started.getCount() == 1);
                  started.countDown();
                  try {
                    return method.invoke(delegate, args);
                  } catch (InvocationTargetException e) {
                    throw e.getCause();
                  } finally {
                    awaitUninterruptibly(canReturn);
                    returned.countDown();
                  }
                }
              });
      this.proxy = proxyObject;
      return proxyObject;
    }

    void awaitStarted() {
      assertTrue(awaitUninterruptibly(started, 10, SECONDS));
    }

    void awaitReturned() {
      canReturn.countDown();
      assertTrue(awaitUninterruptibly(returned, 10, SECONDS));
    }
  }

  static final class NoOpValueAndCloserConsumer<V> implements ValueAndCloserConsumer<V> {
    @Override
    public void accept(ValueAndCloser<V> valueAndCloser) {}
  }
}
