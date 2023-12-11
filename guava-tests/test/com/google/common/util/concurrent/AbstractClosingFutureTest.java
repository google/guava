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
import static com.google.common.util.concurrent.Futures.immediateCancelledFuture;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

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
import com.google.common.util.concurrent.ClosingFuture.Peeker;
import com.google.common.util.concurrent.ClosingFuture.ValueAndCloser;
import com.google.common.util.concurrent.ClosingFuture.ValueAndCloserConsumer;
import java.io.Closeable;
import java.io.IOException;
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
import org.mockito.Mockito;

/**
 * Tests for {@link ClosingFuture}. Subclasses exercise either the {@link
 * ClosingFuture#finishToFuture()} or {@link
 * ClosingFuture#finishToValueAndCloser(ValueAndCloserConsumer, Executor)} paths to complete a
 * {@link ClosingFuture} pipeline.
 */
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

  final ListeningExecutorService executor =
      MoreExecutors.listeningDecorator(newSingleThreadExecutor());
  final ExecutorService closingExecutor = newSingleThreadExecutor();

  final TestCloseable closeable1 = new TestCloseable("closeable1");
  final TestCloseable closeable2 = new TestCloseable("closeable2");
  final TestCloseable closeable3 = new TestCloseable("closeable3");
  final TestCloseable closeable4 = new TestCloseable("closeable4");

  final Waiter waiter = new Waiter();
  final CountDownLatch futureCancelled = new CountDownLatch(1);
  final Exception exception = new Exception();
  final Closeable mockCloseable = Mockito.mock(Closeable.class);

  @Override
  protected void tearDown() throws Exception {
    assertNoExpectedFailures();
    super.tearDown();
  }

  public void testFrom() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.from(executor.submit(Callables.returning(closeable1)))
            .transform(
                new ClosingFunction<TestCloseable, String>() {
                  @Override
                  public String apply(DeferredCloser closer, TestCloseable v) throws Exception {
                    assertThat(v).isSameInstanceAs(closeable1);
                    return "value";
                  }
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
  }

  public void testFrom_failedInput() throws Exception {
    assertFinallyFailsWithException(failedClosingFuture());
  }

  public void testFrom_cancelledInput() throws Exception {
    assertBecomesCanceled(ClosingFuture.from(immediateCancelledFuture()));
  }

  public void testEventuallyClosing() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor)
            .transform(
                new ClosingFunction<TestCloseable, String>() {
                  @Override
                  public String apply(DeferredCloser closer, TestCloseable v) throws Exception {
                    assertThat(v).isSameInstanceAs(closeable1);
                    assertStillOpen(closeable1);
                    return "value";
                  }
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testEventuallyClosing_failedInput() throws Exception {
    assertFinallyFailsWithException(
        ClosingFuture.eventuallyClosing(
            Futures.<Closeable>immediateFailedFuture(exception), closingExecutor));
  }

  public void testEventuallyClosing_cancelledInput() throws Exception {
    assertBecomesCanceled(
        ClosingFuture.eventuallyClosing(
            Futures.<Closeable>immediateCancelledFuture(), closingExecutor));
  }

  public void testEventuallyClosing_cancelledPipeline() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.eventuallyClosing(
            executor.submit(
                waiter.waitFor(
                    new Callable<TestCloseable>() {
                      @Override
                      public TestCloseable call() throws InterruptedException {
                        awaitUninterruptibly(futureCancelled);
                        return closeable1;
                      }
                    })),
            closingExecutor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    // not closed until the callable returns
    assertStillOpen(closeable1);
    waiter.awaitReturned();
    assertClosed(closeable1);
  }

  public void testEventuallyClosing_throws() throws Exception {
    assertFinallyFailsWithException(
        ClosingFuture.eventuallyClosing(
            executor.submit(
                new Callable<TestCloseable>() {
                  @Override
                  public TestCloseable call() throws Exception {
                    throw exception;
                  }
                }),
            closingExecutor));
  }

  public void testSubmit() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.submit(
                new ClosingCallable<TestCloseable>() {
                  @Override
                  public TestCloseable call(DeferredCloser closer) throws Exception {
                    closer.eventuallyClose(closeable1, closingExecutor);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    return closeable3;
                  }
                },
                executor)
            .transform(
                new ClosingFunction<TestCloseable, String>() {
                  @Override
                  public String apply(DeferredCloser closer, TestCloseable v) throws Exception {
                    assertThat(v).isSameInstanceAs(closeable3);
                    assertStillOpen(closeable1, closeable2, closeable3);
                    return "value";
                  }
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
    assertStillOpen(closeable3);
  }

  public void testSubmit_cancelledPipeline() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.submit(
            waiter.waitFor(
                new ClosingCallable<TestCloseable>() {
                  @Override
                  public TestCloseable call(DeferredCloser closer) throws Exception {
                    awaitUninterruptibly(futureCancelled);
                    closer.eventuallyClose(closeable1, closingExecutor);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    return closeable3;
                  }
                }),
            executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2);
    assertStillOpen(closeable3);
  }

  public void testSubmit_throws() throws Exception {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.submit(
            new ClosingCallable<Object>() {
              @Override
              public Object call(DeferredCloser closer) throws Exception {
                closer.eventuallyClose(closeable1, closingExecutor);
                closer.eventuallyClose(closeable2, closingExecutor);
                throw exception;
              }
            },
            executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testSubmitAsync() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.submitAsync(
            new AsyncClosingCallable<TestCloseable>() {
              @Override
              public ClosingFuture<TestCloseable> call(DeferredCloser closer) {
                closer.eventuallyClose(closeable1, closingExecutor);
                return ClosingFuture.submit(
                    new ClosingCallable<TestCloseable>() {
                      @Override
                      public TestCloseable call(DeferredCloser deferredCloser) throws Exception {
                        return closeable2;
                      }
                    },
                    directExecutor());
              }
            },
            executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
    assertStillOpen(closeable2);
  }

  public void testSubmitAsync_cancelledPipeline() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.submitAsync(
            waiter.waitFor(
                new AsyncClosingCallable<TestCloseable>() {
                  @Override
                  public ClosingFuture<TestCloseable> call(DeferredCloser closer) throws Exception {
                    awaitUninterruptibly(futureCancelled);
                    closer.eventuallyClose(closeable1, closingExecutor);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    return ClosingFuture.submit(
                        new ClosingCallable<TestCloseable>() {
                          @Override
                          public TestCloseable call(DeferredCloser deferredCloser)
                              throws Exception {
                            deferredCloser.eventuallyClose(closeable3, closingExecutor);
                            return closeable3;
                          }
                        },
                        directExecutor());
                  }
                }),
            executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2, closeable3);
  }

  public void testSubmitAsync_throws() throws Exception {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.submitAsync(
            new AsyncClosingCallable<Object>() {
              @Override
              public ClosingFuture<Object> call(DeferredCloser closer) throws Exception {
                closer.eventuallyClose(closeable1, closingExecutor);
                closer.eventuallyClose(closeable2, closingExecutor);
                throw exception;
              }
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
            new ClosingCallable<String>() {
              @Override
              public String call(DeferredCloser closer) throws Exception {
                closer.eventuallyClose(autoCloseable, closingExecutor);
                return "";
              }
            },
            executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("");
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testStatusFuture() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.submit(
            waiter.waitFor(
                new ClosingCallable<String>() {
                  @Override
                  public String call(DeferredCloser closer) throws Exception {
                    return "value";
                  }
                }),
            executor);
    ListenableFuture<?> statusFuture = closingFuture.statusFuture();
    waiter.awaitStarted();
    assertThat(statusFuture.isDone()).isFalse();
    waiter.awaitReturned();
    assertThat(getUninterruptibly(statusFuture)).isNull();
  }

  public void testStatusFuture_failure() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.submit(
            waiter.waitFor(
                new ClosingCallable<String>() {
                  @Override
                  public String call(DeferredCloser closer) throws Exception {
                    throw exception;
                  }
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
        ClosingFuture.submit(
            waiter.waitFor(
                new ClosingCallable<String>() {
                  @Override
                  public String call(DeferredCloser closer) throws Exception {
                    return "value";
                  }
                }),
            executor);
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
            new ClosingFunction<String, String>() {
              @Override
              public String apply(DeferredCloser closer, String v) throws Exception {
                closer.eventuallyClose(closeable1, closingExecutor);
                return "value 1";
              }
            },
            executor);
    Waiter step2Waiter = new Waiter();
    ClosingFuture<String> step2 =
        step1.transform(
            step2Waiter.waitFor(
                new ClosingFunction<String, String>() {
                  @Override
                  public String apply(DeferredCloser closer, String v) throws Exception {
                    closer.eventuallyClose(closeable2, closingExecutor);
                    return "value 2";
                  }
                }),
            executor);
    ClosingFuture<String> step3 =
        step2.transform(
            new ClosingFunction<String, String>() {
              @Override
              public String apply(DeferredCloser closer, String input) throws Exception {
                closer.eventuallyClose(closeable3, closingExecutor);
                return "value 3";
              }
            },
            executor);
    Waiter step4Waiter = new Waiter();
    ClosingFuture<String> step4 =
        step3.catching(
            CancellationException.class,
            step4Waiter.waitFor(
                new ClosingFunction<CancellationException, String>() {
                  @Override
                  public String apply(DeferredCloser closer, CancellationException input)
                      throws Exception {
                    closer.eventuallyClose(closeable4, closingExecutor);
                    return "value 4";
                  }
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
                new ClosingFunction<String, TestCloseable>() {
                  @Override
                  public TestCloseable apply(DeferredCloser closer, String v) throws Exception {
                    closer.eventuallyClose(closeable1, closingExecutor);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    return closeable3;
                  }
                },
                executor)
            .transform(
                new ClosingFunction<TestCloseable, String>() {
                  @Override
                  public String apply(DeferredCloser closer, TestCloseable v) throws Exception {
                    assertThat(v).isSameInstanceAs(closeable3);
                    assertStillOpen(closeable1, closeable2, closeable3);
                    return "value";
                  }
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
    assertStillOpen(closeable3);
  }

  public void testTransform_cancelledPipeline() throws Exception {
    String value = "value";
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.from(immediateFuture(value))
            .transform(
                new ClosingFunction<String, TestCloseable>() {
                  @Override
                  public TestCloseable apply(DeferredCloser closer, String v) throws Exception {
                    return closer.eventuallyClose(closeable1, closingExecutor);
                  }
                },
                executor)
            .transform(
                waiter.waitFor(
                    new ClosingFunction<TestCloseable, TestCloseable>() {
                      @Override
                      public TestCloseable apply(DeferredCloser closer, TestCloseable v)
                          throws Exception {
                        awaitUninterruptibly(futureCancelled);
                        closer.eventuallyClose(closeable2, closingExecutor);
                        closer.eventuallyClose(closeable3, closingExecutor);
                        return closeable4;
                      }
                    }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2, closeable3);
    assertStillOpen(closeable4);
  }

  public void testTransform_throws() throws Exception {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.from(immediateFuture("value"))
            .transform(
                new ClosingFunction<String, Object>() {
                  @Override
                  public Object apply(DeferredCloser closer, String v) throws Exception {
                    closer.eventuallyClose(closeable1, closingExecutor);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    throw exception;
                  }
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
                new AsyncClosingFunction<String, TestCloseable>() {
                  @Override
                  public ClosingFuture<TestCloseable> apply(DeferredCloser closer, String v)
                      throws Exception {
                    closer.eventuallyClose(closeable1, closingExecutor);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    return ClosingFuture.eventuallyClosing(
                        immediateFuture(closeable3), closingExecutor);
                  }
                },
                executor)
            .transform(
                new ClosingFunction<TestCloseable, String>() {
                  @Override
                  public String apply(DeferredCloser closer, TestCloseable v) throws Exception {
                    assertThat(v).isSameInstanceAs(closeable3);
                    assertStillOpen(closeable1, closeable2, closeable3);
                    return "value";
                  }
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2, closeable3);
  }

  public void testTransformAsync_cancelledPipeline() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.from(immediateFuture("value"))
            .transformAsync(
                waiter.waitFor(
                    new AsyncClosingFunction<String, TestCloseable>() {
                      @Override
                      public ClosingFuture<TestCloseable> apply(DeferredCloser closer, String v)
                          throws Exception {
                        awaitUninterruptibly(futureCancelled);
                        closer.eventuallyClose(closeable1, closingExecutor);
                        closer.eventuallyClose(closeable2, closingExecutor);
                        return ClosingFuture.eventuallyClosing(
                            immediateFuture(closeable3), closingExecutor);
                      }
                    }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    // not closed until the function returns
    assertStillOpen(closeable1, closeable2, closeable3);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2, closeable3);
  }

  public void testTransformAsync_throws() throws Exception {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.from(immediateFuture("value"))
            .transformAsync(
                new AsyncClosingFunction<String, Object>() {
                  @Override
                  public ClosingFuture<Object> apply(DeferredCloser closer, String v)
                      throws Exception {
                    closer.eventuallyClose(closeable1, closingExecutor);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    throw exception;
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testTransformAsync_failed() throws Exception {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.from(immediateFuture("value"))
            .transformAsync(
                new AsyncClosingFunction<String, Object>() {
                  @Override
                  public ClosingFuture<Object> apply(DeferredCloser closer, String v)
                      throws Exception {
                    closer.eventuallyClose(closeable1, closingExecutor);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    return failedClosingFuture();
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testTransformAsync_withoutCloser() throws Exception {
    ClosingFuture<String> closingFuture =
        ClosingFuture.submit(
                new ClosingCallable<TestCloseable>() {
                  @Override
                  public TestCloseable call(DeferredCloser closer) throws Exception {
                    return closer.eventuallyClose(closeable1, closingExecutor);
                  }
                },
                executor)
            .transformAsync(
                ClosingFuture.withoutCloser(
                    new AsyncFunction<TestCloseable, String>() {
                      @Override
                      public ListenableFuture<String> apply(TestCloseable v) throws Exception {
                        assertThat(v).isSameInstanceAs(closeable1);
                        assertStillOpen(closeable1);
                        return immediateFuture("value");
                      }
                    }),
                executor);
    assertThat(getFinalValue(closingFuture)).isEqualTo("value");
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testWhenAllComplete_call() throws Exception {
    final ClosingFuture<String> input1 = ClosingFuture.from(immediateFuture("value1"));
    final ClosingFuture<Object> input2Failed = failedClosingFuture();
    final ClosingFuture<String> nonInput = ClosingFuture.from(immediateFuture("value3"));
    final AtomicReference<ClosingFuture.Peeker> capturedPeeker = new AtomicReference<>();
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllComplete(ImmutableList.of(input1, input2Failed))
            .call(
                new CombiningCallable<TestCloseable>() {
                  @Override
                  public TestCloseable call(DeferredCloser closer, Peeker peeker) throws Exception {
                    closer.eventuallyClose(closeable1, closingExecutor);
                    assertThat(peeker.getDone(input1)).isSameInstanceAs("value1");
                    try {
                      peeker.getDone(input2Failed);
                      fail("Peeker.getDone() should fail for failed inputs");
                    } catch (ExecutionException expected) {
                    }
                    try {
                      peeker.getDone(nonInput);
                      fail("Peeker should not be able to peek into non-input ClosingFuture.");
                    } catch (IllegalArgumentException expected) {
                    }
                    capturedPeeker.set(peeker);
                    return closeable2;
                  }
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable2);
    assertClosed(closeable1);
    assertThrows(IllegalStateException.class, () -> capturedPeeker.get().getDone(input1));
  }

  public void testWhenAllComplete_call_cancelledPipeline() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllComplete(
                ImmutableList.of(
                    ClosingFuture.from(immediateFuture(closeable1)),
                    ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor)))
            .call(
                waiter.waitFor(
                    new CombiningCallable<TestCloseable>() {
                      @Override
                      public TestCloseable call(DeferredCloser closer, Peeker peeker)
                          throws Exception {
                        awaitUninterruptibly(futureCancelled);
                        closer.eventuallyClose(closeable1, closingExecutor);
                        return closeable3;
                      }
                    }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2);
    assertStillOpen(closeable3);
  }

  public void testWhenAllComplete_call_throws() throws Exception {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllComplete(
                ImmutableList.of(
                    ClosingFuture.from(immediateFuture(closeable1)),
                    ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor)))
            .call(
                new CombiningCallable<Object>() {
                  @Override
                  public Object call(DeferredCloser closer, Peeker peeker) throws Exception {
                    closer.eventuallyClose(closeable3, closingExecutor);
                    throw exception;
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  public void testWhenAllComplete_callAsync() throws Exception {
    final ClosingFuture<String> input1 = ClosingFuture.from(immediateFuture("value1"));
    final ClosingFuture<Object> input2Failed = failedClosingFuture();
    final ClosingFuture<String> nonInput = ClosingFuture.from(immediateFuture("value3"));
    final AtomicReference<ClosingFuture.Peeker> capturedPeeker = new AtomicReference<>();
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllComplete(ImmutableList.of(input1, input2Failed))
            .callAsync(
                new AsyncCombiningCallable<TestCloseable>() {
                  @Override
                  public ClosingFuture<TestCloseable> call(DeferredCloser closer, Peeker peeker)
                      throws Exception {
                    closer.eventuallyClose(closeable1, closingExecutor);
                    assertThat(peeker.getDone(input1)).isSameInstanceAs("value1");
                    try {
                      peeker.getDone(input2Failed);
                      fail("Peeker should fail for failed inputs");
                    } catch (ExecutionException expected) {
                    }
                    try {
                      peeker.getDone(nonInput);
                      fail("Peeker should not be able to peek into non-input ClosingFuture.");
                    } catch (IllegalArgumentException expected) {
                    }
                    capturedPeeker.set(peeker);
                    return ClosingFuture.eventuallyClosing(
                        immediateFuture(closeable2), closingExecutor);
                  }
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
    assertThrows(IllegalStateException.class, () -> capturedPeeker.get().getDone(input1));
  }

  public void testWhenAllComplete_callAsync_cancelledPipeline() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllComplete(
                ImmutableList.of(
                    ClosingFuture.from(immediateFuture(closeable1)),
                    ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor)))
            .callAsync(
                waiter.waitFor(
                    new AsyncCombiningCallable<TestCloseable>() {
                      @Override
                      public ClosingFuture<TestCloseable> call(DeferredCloser closer, Peeker peeker)
                          throws Exception {
                        awaitUninterruptibly(futureCancelled);
                        closer.eventuallyClose(closeable1, closingExecutor);
                        return ClosingFuture.eventuallyClosing(
                            immediateFuture(closeable3), closingExecutor);
                      }
                    }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2, closeable3);
  }

  public void testWhenAllComplete_callAsync_throws() throws Exception {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllComplete(
                ImmutableList.of(
                    ClosingFuture.from(immediateFuture(closeable1)),
                    ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor)))
            .callAsync(
                new AsyncCombiningCallable<Object>() {
                  @Override
                  public ClosingFuture<Object> call(DeferredCloser closer, Peeker peeker)
                      throws Exception {
                    closer.eventuallyClose(closeable3, closingExecutor);
                    throw exception;
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  // We don't need to test the happy case for SuccessfulCombiner.call(Async) because it's the same
  // as Combiner.

  public void testWhenAllSucceed_call_failedInput() throws Exception {
    assertFinallyFailsWithException(
        ClosingFuture.whenAllSucceed(
                ImmutableList.of(
                    ClosingFuture.from(immediateFuture("value")), failedClosingFuture()))
            .call(
                new CombiningCallable<Object>() {
                  @Override
                  public Object call(DeferredCloser closer, Peeker peeker) throws Exception {
                    expect.fail();
                    throw new AssertionError();
                  }
                },
                executor));
  }

  public void testWhenAllSucceed_callAsync_failedInput() throws Exception {
    assertFinallyFailsWithException(
        ClosingFuture.whenAllSucceed(
                ImmutableList.of(
                    ClosingFuture.from(immediateFuture("value")), failedClosingFuture()))
            .callAsync(
                new AsyncCombiningCallable<Object>() {
                  @Override
                  public ClosingFuture<Object> call(DeferredCloser closer, Peeker peeker)
                      throws Exception {
                    expect.fail();
                    throw new AssertionError();
                  }
                },
                executor));
  }

  public void testWhenAllSucceed2_call() throws ExecutionException, IOException {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                ClosingFuture.from(immediateFuture("value1")))
            .call(
                new ClosingFunction2<TestCloseable, String, TestCloseable>() {
                  @Override
                  public TestCloseable apply(DeferredCloser closer, TestCloseable v1, String v2)
                      throws Exception {
                    assertThat(v1).isEqualTo(closeable1);
                    assertThat(v2).isEqualTo("value1");
                    assertStillOpen(closeable1);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    return closeable2;
                  }
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testWhenAllSucceed2_call_failedInput() throws ExecutionException, IOException {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                failedClosingFuture())
            .call(
                new ClosingFunction2<TestCloseable, Object, Object>() {
                  @Override
                  public Object apply(DeferredCloser closer, TestCloseable v1, Object v2)
                      throws Exception {
                    expect.fail();
                    throw new AssertionError();
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testWhenAllSucceed2_call_cancelledPipeline() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.from(immediateFuture(closeable2)))
            .call(
                waiter.waitFor(
                    new ClosingFunction2<TestCloseable, TestCloseable, TestCloseable>() {
                      @Override
                      public TestCloseable apply(
                          DeferredCloser closer, TestCloseable v1, TestCloseable v2)
                          throws Exception {
                        awaitUninterruptibly(futureCancelled);
                        closer.eventuallyClose(closeable1, closingExecutor);
                        closer.eventuallyClose(closeable2, closingExecutor);
                        return closeable3;
                      }
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

  public void testWhenAllSucceed2_call_throws() throws Exception {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor))
            .call(
                new ClosingFunction2<TestCloseable, TestCloseable, Object>() {
                  @Override
                  public Object apply(DeferredCloser closer, TestCloseable v1, TestCloseable v2)
                      throws Exception {
                    closer.eventuallyClose(closeable3, closingExecutor);
                    throw exception;
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  public void testWhenAllSucceed2_callAsync() throws ExecutionException, IOException {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                ClosingFuture.from(immediateFuture("value1")))
            .callAsync(
                new AsyncClosingFunction2<TestCloseable, String, TestCloseable>() {
                  @Override
                  public ClosingFuture<TestCloseable> apply(
                      DeferredCloser closer, TestCloseable v1, String v2) throws Exception {
                    assertThat(v1).isEqualTo(closeable1);
                    assertThat(v2).isEqualTo("value1");
                    assertStillOpen(closeable1);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    return ClosingFuture.eventuallyClosing(
                        immediateFuture(closeable3), closingExecutor);
                  }
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable3);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2, closeable3);
  }

  public void testWhenAllSucceed2_callAsync_failedInput() throws ExecutionException, IOException {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                failedClosingFuture())
            .callAsync(
                new AsyncClosingFunction2<TestCloseable, Object, Object>() {
                  @Override
                  public ClosingFuture<Object> apply(
                      DeferredCloser closer, TestCloseable v1, Object v2) throws Exception {
                    expect.fail();
                    throw new AssertionError();
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testWhenAllSucceed2_callAsync_cancelledPipeline() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.from(immediateFuture(closeable2)))
            .callAsync(
                waiter.waitFor(
                    new AsyncClosingFunction2<TestCloseable, TestCloseable, TestCloseable>() {
                      @Override
                      public ClosingFuture<TestCloseable> apply(
                          DeferredCloser closer, TestCloseable v1, TestCloseable v2)
                          throws Exception {
                        awaitUninterruptibly(futureCancelled);
                        closer.eventuallyClose(closeable1, closingExecutor);
                        closer.eventuallyClose(closeable2, closingExecutor);
                        return ClosingFuture.eventuallyClosing(
                            immediateFuture(closeable3), closingExecutor);
                      }
                    }),
                executor);
    waiter.awaitStarted();
    cancelFinalStepAndWait(closingFuture);
    // not closed until the function returns
    assertStillOpen(closeable1, closeable2, closeable3);
    waiter.awaitReturned();
    assertClosed(closeable1, closeable2, closeable3);
  }

  public void testWhenAllSucceed2_callAsync_throws() throws Exception {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor))
            .callAsync(
                new AsyncClosingFunction2<TestCloseable, TestCloseable, Object>() {
                  @Override
                  public ClosingFuture<Object> apply(
                      DeferredCloser closer, TestCloseable v1, TestCloseable v2) throws Exception {
                    closer.eventuallyClose(closeable3, closingExecutor);
                    throw exception;
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  public void testWhenAllSucceed3_call() throws ExecutionException, IOException {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                ClosingFuture.from(immediateFuture("value2")),
                ClosingFuture.from(immediateFuture("value3")))
            .call(
                new ClosingFunction3<TestCloseable, String, String, TestCloseable>() {
                  @Override
                  public TestCloseable apply(
                      DeferredCloser closer, TestCloseable v1, String v2, String v3)
                      throws Exception {
                    assertThat(v1).isEqualTo(closeable1);
                    assertThat(v2).isEqualTo("value2");
                    assertThat(v3).isEqualTo("value3");
                    assertStillOpen(closeable1);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    return closeable2;
                  }
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testWhenAllSucceed3_call_failedInput() throws ExecutionException, IOException {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                failedClosingFuture(),
                ClosingFuture.from(immediateFuture("value3")))
            .call(
                new ClosingFunction3<TestCloseable, Object, String, Object>() {
                  @Override
                  public Object apply(DeferredCloser closer, TestCloseable v1, Object v2, String v3)
                      throws Exception {
                    expect.fail();
                    throw new AssertionError();
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testWhenAllSucceed3_call_cancelledPipeline() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.from(immediateFuture(closeable2)),
                ClosingFuture.from(immediateFuture("value3")))
            .call(
                waiter.waitFor(
                    new ClosingFunction3<TestCloseable, TestCloseable, String, TestCloseable>() {
                      @Override
                      public TestCloseable apply(
                          DeferredCloser closer, TestCloseable v1, TestCloseable v2, String v3)
                          throws Exception {
                        awaitUninterruptibly(futureCancelled);
                        closer.eventuallyClose(closeable1, closingExecutor);
                        closer.eventuallyClose(closeable2, closingExecutor);
                        return closeable3;
                      }
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

  public void testWhenAllSucceed3_call_throws() throws Exception {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor),
                ClosingFuture.from(immediateFuture("value3")))
            .call(
                new ClosingFunction3<TestCloseable, TestCloseable, String, Object>() {
                  @Override
                  public Object apply(
                      DeferredCloser closer, TestCloseable v1, TestCloseable v2, String v3)
                      throws Exception {
                    closer.eventuallyClose(closeable3, closingExecutor);
                    throw exception;
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  public void testWhenAllSucceed4_call() throws ExecutionException, IOException {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                ClosingFuture.from(immediateFuture("value2")),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")))
            .call(
                new ClosingFunction4<TestCloseable, String, String, String, TestCloseable>() {
                  @Override
                  public TestCloseable apply(
                      DeferredCloser closer, TestCloseable v1, String v2, String v3, String v4)
                      throws Exception {
                    assertThat(v1).isEqualTo(closeable1);
                    assertThat(v2).isEqualTo("value2");
                    assertThat(v3).isEqualTo("value3");
                    assertThat(v4).isEqualTo("value4");
                    assertStillOpen(closeable1);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    return closeable2;
                  }
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testWhenAllSucceed4_call_failedInput() throws ExecutionException, IOException {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                failedClosingFuture(),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")))
            .call(
                new ClosingFunction4<TestCloseable, Object, String, String, Object>() {
                  @Override
                  public Object apply(
                      DeferredCloser closer, TestCloseable v1, Object v2, String v3, String v4)
                      throws Exception {
                    expect.fail();
                    throw new AssertionError();
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testWhenAllSucceed4_call_cancelledPipeline() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.from(immediateFuture(closeable2)),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")))
            .call(
                waiter.waitFor(
                    new ClosingFunction4<
                        TestCloseable, TestCloseable, String, String, TestCloseable>() {
                      @Override
                      public TestCloseable apply(
                          DeferredCloser closer,
                          TestCloseable v1,
                          TestCloseable v2,
                          String v3,
                          String v4)
                          throws Exception {
                        awaitUninterruptibly(futureCancelled);
                        closer.eventuallyClose(closeable1, closingExecutor);
                        closer.eventuallyClose(closeable2, closingExecutor);
                        return closeable3;
                      }
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

  public void testWhenAllSucceed4_call_throws() throws Exception {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")))
            .call(
                new ClosingFunction4<TestCloseable, TestCloseable, String, String, Object>() {
                  @Override
                  public Object apply(
                      DeferredCloser closer,
                      TestCloseable v1,
                      TestCloseable v2,
                      String v3,
                      String v4)
                      throws Exception {
                    closer.eventuallyClose(closeable3, closingExecutor);
                    throw exception;
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  public void testWhenAllSucceed5_call() throws ExecutionException, IOException {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                ClosingFuture.from(immediateFuture("value2")),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")),
                ClosingFuture.from(immediateFuture("value5")))
            .call(
                new ClosingFunction5<
                    TestCloseable, String, String, String, String, TestCloseable>() {
                  @Override
                  public TestCloseable apply(
                      DeferredCloser closer,
                      TestCloseable v1,
                      String v2,
                      String v3,
                      String v4,
                      String v5)
                      throws Exception {
                    assertThat(v1).isEqualTo(closeable1);
                    assertThat(v2).isEqualTo("value2");
                    assertThat(v3).isEqualTo("value3");
                    assertThat(v4).isEqualTo("value4");
                    assertThat(v5).isEqualTo("value5");
                    assertStillOpen(closeable1);
                    closer.eventuallyClose(closeable2, closingExecutor);
                    return closeable2;
                  }
                },
                executor);
    assertThat(getFinalValue(closingFuture)).isSameInstanceAs(closeable2);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1, closeable2);
  }

  public void testWhenAllSucceed5_call_failedInput() throws ExecutionException, IOException {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.eventuallyClosing(immediateFuture(closeable1), closingExecutor),
                failedClosingFuture(),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")),
                ClosingFuture.from(immediateFuture("value5")))
            .call(
                new ClosingFunction5<TestCloseable, Object, String, String, String, Object>() {
                  @Override
                  public Object apply(
                      DeferredCloser closer,
                      TestCloseable v1,
                      Object v2,
                      String v3,
                      String v4,
                      String v5)
                      throws Exception {
                    expect.fail();
                    throw new AssertionError();
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertClosed(closeable1);
  }

  public void testWhenAllSucceed5_call_cancelledPipeline() throws Exception {
    ClosingFuture<TestCloseable> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.from(immediateFuture(closeable2)),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")),
                ClosingFuture.from(immediateFuture("value5")))
            .call(
                waiter.waitFor(
                    new ClosingFunction5<
                        TestCloseable, TestCloseable, String, String, String, TestCloseable>() {
                      @Override
                      public TestCloseable apply(
                          DeferredCloser closer,
                          TestCloseable v1,
                          TestCloseable v2,
                          String v3,
                          String v4,
                          String v5)
                          throws Exception {
                        awaitUninterruptibly(futureCancelled);
                        closer.eventuallyClose(closeable1, closingExecutor);
                        closer.eventuallyClose(closeable2, closingExecutor);
                        return closeable3;
                      }
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

  public void testWhenAllSucceed5_call_throws() throws Exception {
    ClosingFuture<Object> closingFuture =
        ClosingFuture.whenAllSucceed(
                ClosingFuture.from(immediateFuture(closeable1)),
                ClosingFuture.eventuallyClosing(immediateFuture(closeable2), closingExecutor),
                ClosingFuture.from(immediateFuture("value3")),
                ClosingFuture.from(immediateFuture("value4")),
                ClosingFuture.from(immediateFuture("value5")))
            .call(
                new ClosingFunction5<
                    TestCloseable, TestCloseable, String, String, String, Object>() {
                  @Override
                  public Object apply(
                      DeferredCloser closer,
                      TestCloseable v1,
                      TestCloseable v2,
                      String v3,
                      String v4,
                      String v5)
                      throws Exception {
                    closer.eventuallyClose(closeable3, closingExecutor);
                    throw exception;
                  }
                },
                executor);
    assertFinallyFailsWithException(closingFuture);
    waitUntilClosed(closingFuture);
    assertStillOpen(closeable1);
    assertClosed(closeable2, closeable3);
  }

  public void testTransform_preventsFurtherOperations() {
    ClosingFuture<String> closingFuture = ClosingFuture.from(immediateFuture("value1"));
    ClosingFuture<String> unused =
        closingFuture.transform(
            new ClosingFunction<String, String>() {
              @Override
              public String apply(DeferredCloser closer, String v) throws Exception {
                return "value2";
              }
            },
            executor);
    assertDerivingThrowsIllegalStateException(closingFuture);
    assertFinalStepThrowsIllegalStateException(closingFuture);
  }

  public void testTransformAsync_preventsFurtherOperations() {
    ClosingFuture<String> closingFuture = ClosingFuture.from(immediateFuture("value1"));
    ClosingFuture<String> unused =
        closingFuture.transformAsync(
            new AsyncClosingFunction<String, String>() {
              @Override
              public ClosingFuture<String> apply(DeferredCloser closer, String v) throws Exception {
                return ClosingFuture.from(immediateFuture("value2"));
              }
            },
            executor);
    assertDerivingThrowsIllegalStateException(closingFuture);
    assertFinalStepThrowsIllegalStateException(closingFuture);
  }

  public void testCatching_preventsFurtherOperations() {
    ClosingFuture<String> closingFuture = ClosingFuture.from(immediateFuture("value1"));
    ClosingFuture<String> unused =
        closingFuture.catching(
            Exception.class,
            new ClosingFunction<Exception, String>() {
              @Override
              public String apply(DeferredCloser closer, Exception x) throws Exception {
                return "value2";
              }
            },
            executor);
    assertDerivingThrowsIllegalStateException(closingFuture);
    assertFinalStepThrowsIllegalStateException(closingFuture);
  }

  public void testCatchingAsync_preventsFurtherOperations() {
    ClosingFuture<String> closingFuture = ClosingFuture.from(immediateFuture("value1"));
    ClosingFuture<String> unused =
        closingFuture.catchingAsync(
            Exception.class,
            ClosingFuture.withoutCloser(
                new AsyncFunction<Exception, String>() {
                  @Override
                  public ListenableFuture<String> apply(Exception x) throws Exception {
                    return immediateFuture("value2");
                  }
                }),
            executor);
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
    try {
      closingFuture.transform(
          new ClosingFunction<String, String>() {
            @Override
            public String apply(DeferredCloser closer3, String v1) throws Exception {
              return "value3";
            }
          },
          executor);
      fail();
    } catch (IllegalStateException expected5) {
    }
    try {
      closingFuture.transformAsync(
          new AsyncClosingFunction<String, String>() {
            @Override
            public ClosingFuture<String> apply(DeferredCloser closer2, String v) throws Exception {
              return ClosingFuture.from(immediateFuture("value3"));
            }
          },
          executor);
      fail();
    } catch (IllegalStateException expected4) {
    }
    try {
      closingFuture.catching(
          Exception.class,
          new ClosingFunction<Exception, String>() {
            @Override
            public String apply(DeferredCloser closer1, Exception x1) throws Exception {
              return "value3";
            }
          },
          executor);
      fail();
    } catch (IllegalStateException expected3) {
    }
    try {
      closingFuture.catchingAsync(
          Exception.class,
          new AsyncClosingFunction<Exception, String>() {
            @Override
            public ClosingFuture<String> apply(DeferredCloser closer, Exception x)
                throws Exception {
              return ClosingFuture.from(immediateFuture("value3"));
            }
          },
          executor);
      fail();
    } catch (IllegalStateException expected2) {
    }
    try {
      ClosingFuture.whenAllComplete(asList(closingFuture));
      fail();
    } catch (IllegalStateException expected1) {
    }
    try {
      ClosingFuture.whenAllSucceed(asList(closingFuture));
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  /** Asserts that marking this step a final step throws {@link IllegalStateException}. */
  protected void assertFinalStepThrowsIllegalStateException(ClosingFuture<?> closingFuture) {
    try {
      closingFuture.finishToFuture();
      fail();
    } catch (IllegalStateException expected) {
    }
    try {
      closingFuture.finishToValueAndCloser(new NoOpValueAndCloserConsumer<>(), executor);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  // Avoid infinite recursion if a closeable's close() method throws RejectedExecutionException and
  // is closed using the direct executor.
  public void testCloseThrowsRejectedExecutionException() throws Exception {
    doThrow(new RejectedExecutionException()).when(mockCloseable).close();
    ClosingFuture<Closeable> closingFuture =
        ClosingFuture.submit(
            new ClosingCallable<Closeable>() {
              @Override
              public Closeable call(DeferredCloser closer) throws Exception {
                return closer.eventuallyClose(mockCloseable, directExecutor());
              }
            },
            executor);
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
  abstract void assertBecomesCanceled(ClosingFuture<?> closingFuture) throws ExecutionException;

  /** Waits for the given step's closeables to be closed. */
  void waitUntilClosed(ClosingFuture<?> closingFuture) {
    assertTrue(awaitUninterruptibly(closingFuture.whenClosedCountDown(), 1, SECONDS));
  }

  void assertThatFutureFailsWithException(Future<?> future) {
    try {
      getUninterruptibly(future);
      fail("Expected future to fail: " + future);
    } catch (ExecutionException e) {
      assertThat(e).hasCauseThat().isSameInstanceAs(exception);
    }
  }

  static void assertThatFutureBecomesCancelled(Future<?> future) throws ExecutionException {
    try {
      getUninterruptibly(future);
      fail("Expected future to be canceled: " + future);
    } catch (CancellationException expected) {
    }
  }

  private static void assertStillOpen(TestCloseable closeable1, TestCloseable... moreCloseables)
      throws IOException {
    for (TestCloseable closeable : asList(closeable1, moreCloseables)) {
      assertWithMessage("%s.stillOpen()", closeable).that(closeable.stillOpen()).isTrue();
    }
  }

  static void assertClosed(TestCloseable closeable1, TestCloseable... moreCloseables)
      throws IOException {
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
    public void close() throws IOException {
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

    <V> Callable<V> waitFor(Callable<V> callable) {
      return waitFor(callable, Callable.class);
    }

    <V> ClosingCallable<V> waitFor(ClosingCallable<V> closingCallable) {
      return waitFor(closingCallable, ClosingCallable.class);
    }

    <V> AsyncClosingCallable<V> waitFor(AsyncClosingCallable<V> asyncClosingCallable) {
      return waitFor(asyncClosingCallable, AsyncClosingCallable.class);
    }

    <T, U> ClosingFunction<T, U> waitFor(ClosingFunction<T, U> closingFunction) {
      return waitFor(closingFunction, ClosingFunction.class);
    }

    <T, U> AsyncClosingFunction<T, U> waitFor(AsyncClosingFunction<T, U> asyncClosingFunction) {
      return waitFor(asyncClosingFunction, AsyncClosingFunction.class);
    }

    <V> CombiningCallable<V> waitFor(CombiningCallable<V> combiningCallable) {
      return waitFor(combiningCallable, CombiningCallable.class);
    }

    <V> AsyncCombiningCallable<V> waitFor(AsyncCombiningCallable<V> asyncCombiningCallable) {
      return waitFor(asyncCombiningCallable, AsyncCombiningCallable.class);
    }

    <V1, V2, U> ClosingFunction2<V1, V2, U> waitFor(ClosingFunction2<V1, V2, U> closingFunction2) {
      return waitFor(closingFunction2, ClosingFunction2.class);
    }

    <V1, V2, U> AsyncClosingFunction2<V1, V2, U> waitFor(
        AsyncClosingFunction2<V1, V2, U> asyncClosingFunction2) {
      return waitFor(asyncClosingFunction2, AsyncClosingFunction2.class);
    }

    <V1, V2, V3, U> ClosingFunction3<V1, V2, V3, U> waitFor(
        ClosingFunction3<V1, V2, V3, U> closingFunction3) {
      return waitFor(closingFunction3, ClosingFunction3.class);
    }

    <V1, V2, V3, V4, U> ClosingFunction4<V1, V2, V3, V4, U> waitFor(
        ClosingFunction4<V1, V2, V3, V4, U> closingFunction4) {
      return waitFor(closingFunction4, ClosingFunction4.class);
    }

    <V1, V2, V3, V4, V5, U> ClosingFunction5<V1, V2, V3, V4, V5, U> waitFor(
        ClosingFunction5<V1, V2, V3, V4, V5, U> closingFunction5) {
      return waitFor(closingFunction5, ClosingFunction5.class);
    }

    <T> T waitFor(final T delegate, final Class<T> type) {
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
