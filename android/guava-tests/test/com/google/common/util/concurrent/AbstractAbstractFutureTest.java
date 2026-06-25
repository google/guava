/*
 * Copyright (C) 2015 The Guava Authors
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
import static com.google.common.util.concurrent.Futures.getDone;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.Runnables.doNothing;
import static com.google.common.util.concurrent.TestPlatform.getDoneFromTimeoutOverload;
import static com.google.common.util.concurrent.TestPlatform.verifyGetOnPendingFuture;
import static com.google.common.util.concurrent.TestPlatform.verifyTimedGetOnPendingFuture;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.util.concurrent.AbstractFutureTest.TimedWaiterThread;
import com.google.common.util.concurrent.SettableAbstractFuture.UntrustedAbstractFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Base class for tests for emulated {@link AbstractFuture} that allow subclasses to swap in a
 * different "source Future" for {@link AbstractFuture#setFuture} calls.
 */
@GwtCompatible
@NullUnmarked
abstract class AbstractAbstractFutureTest<
        FutureT extends
            AbstractFuture<@Nullable Integer> & SettableAbstractFuture<@Nullable Integer>>
    extends TestCase {
  @SuppressWarnings("initialization.field.uninitialized")
  private UntrustedAbstractFuture<@Nullable Integer> future;

  @SuppressWarnings("initialization.field.uninitialized")
  private FutureT delegate;

  abstract FutureT newDelegate();

  @Override
  protected void setUp() {
    future = new UntrustedAbstractFuture<>();
    delegate = newDelegate();
  }

  public void testPending() {
    assertPending(future);
  }

  public void testSuccessful() throws Exception {
    assertThat(future.doSet(1)).isTrue();
    assertSuccessful(future, 1);
  }

  public void testFailed() {
    Exception cause = new Exception();
    assertThat(future.doSetException(cause)).isTrue();
    assertFailed(future, cause);
  }

  public void testCanceled() {
    assertThat(future.cancel(/* mayInterruptIfRunning= */ false)).isTrue();
    assertCancelled(future, false);
  }

  public void testInterrupted() {
    assertThat(future.cancel(/* mayInterruptIfRunning= */ true)).isTrue();
    assertCancelled(future, true);
  }

  public void testSetFuturePending() {
    assertThat(future.doSetFuture(delegate)).isTrue();
    assertSetAsynchronously(future);
  }

  public void testSetFutureThenCancel() {
    assertThat(future.doSetFuture(delegate)).isTrue();
    assertThat(future.cancel(/* mayInterruptIfRunning= */ false)).isTrue();
    assertCancelled(future, false);
    assertCancelled(delegate, false);
  }

  public void testSetFutureThenInterrupt() {
    assertThat(future.doSetFuture(delegate)).isTrue();
    assertThat(future.cancel(/* mayInterruptIfRunning= */ true)).isTrue();
    assertCancelled(future, true);
    assertCancelled(delegate, true);
  }

  public void testSetFutureDelegateAlreadySuccessful() throws Exception {
    delegate.doSet(5);
    assertThat(future.doSetFuture(delegate)).isTrue();
    assertSuccessful(future, 5);
  }

  public void testSetFutureDelegateLaterSuccessful() throws Exception {
    assertThat(future.doSetFuture(delegate)).isTrue();
    delegate.doSet(6);
    assertSuccessful(future, 6);
  }

  public void testSetFutureDelegateAlreadyCancelled() {
    delegate.cancel(/* mayInterruptIfRunning= */ false);
    assertThat(future.doSetFuture(delegate)).isTrue();
    assertCancelled(future, false);
  }

  public void testSetFutureDelegateLaterCancelled() {
    assertThat(future.doSetFuture(delegate)).isTrue();
    delegate.cancel(/* mayInterruptIfRunning= */ false);
    assertCancelled(future, false);
  }

  public void testSetFutureDelegateAlreadyInterrupted() {
    delegate.cancel(/* mayInterruptIfRunning= */ true);
    assertThat(future.doSetFuture(delegate)).isTrue();
    assertCancelled(future, /* expectWasInterrupted= */ false);
  }

  public void testSetFutureDelegateLaterInterrupted() {
    assertThat(future.doSetFuture(delegate)).isTrue();
    delegate.cancel(/* mayInterruptIfRunning= */ true);
    assertCancelled(future, /* expectWasInterrupted= */ false);
  }

  public void testListenLaterSuccessful() {
    CountingRunnable listener = new CountingRunnable();

    future.addListener(listener, directExecutor());
    listener.assertNotRun();

    future.doSet(1);
    listener.assertRun();
  }

  public void testListenLaterFailed() {
    CountingRunnable listener = new CountingRunnable();

    future.addListener(listener, directExecutor());
    listener.assertNotRun();

    future.doSetException(new Exception());
    listener.assertRun();
  }

  public void testListenLaterCancelled() {
    CountingRunnable listener = new CountingRunnable();

    future.addListener(listener, directExecutor());
    listener.assertNotRun();

    future.cancel(false);
    listener.assertRun();
  }

  public void testListenLaterInterrupted() {
    CountingRunnable listener = new CountingRunnable();

    future.addListener(listener, directExecutor());
    listener.assertNotRun();

    future.cancel(true);
    listener.assertRun();
  }

  public void testListenLaterSetAsynchronously() {
    CountingRunnable listener = new CountingRunnable();

    future.addListener(listener, directExecutor());
    listener.assertNotRun();

    future.doSetFuture(delegate);
    listener.assertNotRun();
  }

  public void testListenLaterSetAsynchronouslyLaterDelegateSuccessful() {
    CountingRunnable before = new CountingRunnable();
    CountingRunnable inBetween = new CountingRunnable();
    CountingRunnable after = new CountingRunnable();

    future.addListener(before, directExecutor());
    future.doSetFuture(delegate);
    future.addListener(inBetween, directExecutor());
    delegate.doSet(1);
    future.addListener(after, directExecutor());

    before.assertRun();
    inBetween.assertRun();
    after.assertRun();
  }

  public void testListenLaterSetAsynchronouslyLaterDelegateFailed() {
    CountingRunnable before = new CountingRunnable();
    CountingRunnable inBetween = new CountingRunnable();
    CountingRunnable after = new CountingRunnable();

    future.addListener(before, directExecutor());
    future.doSetFuture(delegate);
    future.addListener(inBetween, directExecutor());
    delegate.doSetException(new Exception());
    future.addListener(after, directExecutor());

    before.assertRun();
    inBetween.assertRun();
    after.assertRun();
  }

  public void testListenLaterSetAsynchronouslyLaterDelegateCancelled() {
    CountingRunnable before = new CountingRunnable();
    CountingRunnable inBetween = new CountingRunnable();
    CountingRunnable after = new CountingRunnable();

    future.addListener(before, directExecutor());
    future.doSetFuture(delegate);
    future.addListener(inBetween, directExecutor());
    delegate.cancel(false);
    future.addListener(after, directExecutor());

    before.assertRun();
    inBetween.assertRun();
    after.assertRun();
  }

  public void testListenLaterSetAsynchronouslyLaterDelegateInterrupted() {
    CountingRunnable before = new CountingRunnable();
    CountingRunnable inBetween = new CountingRunnable();
    CountingRunnable after = new CountingRunnable();

    future.addListener(before, directExecutor());
    future.doSetFuture(delegate);
    future.addListener(inBetween, directExecutor());
    delegate.cancel(true);
    future.addListener(after, directExecutor());

    before.assertRun();
    inBetween.assertRun();
    after.assertRun();
  }

  public void testListenLaterSetAsynchronouslyLaterSelfCancelled() {
    CountingRunnable before = new CountingRunnable();
    CountingRunnable inBetween = new CountingRunnable();
    CountingRunnable after = new CountingRunnable();

    future.addListener(before, directExecutor());
    future.doSetFuture(delegate);
    future.addListener(inBetween, directExecutor());
    future.cancel(false);
    future.addListener(after, directExecutor());

    before.assertRun();
    inBetween.assertRun();
    after.assertRun();
  }

  public void testListenLaterSetAsynchronouslyLaterSelfInterrupted() {
    CountingRunnable before = new CountingRunnable();
    CountingRunnable inBetween = new CountingRunnable();
    CountingRunnable after = new CountingRunnable();

    future.addListener(before, directExecutor());
    future.doSetFuture(delegate);
    future.addListener(inBetween, directExecutor());
    future.cancel(true);
    future.addListener(after, directExecutor());

    before.assertRun();
    inBetween.assertRun();
    after.assertRun();
  }

  private static final class BadRunnableException extends RuntimeException {}

  public void testMisbehavingListenerAlreadyDone() {
    Runnable bad =
        () -> {
          throw new BadRunnableException();
        };

    future.doSet(1);
    future.addListener(bad, directExecutor()); // BadRunnableException must not propagate.
  }

  public void testMisbehavingListenerLaterDone() {
    CountingRunnable before = new CountingRunnable();
    Runnable bad =
        () -> {
          throw new BadRunnableException();
        };
    CountingRunnable after = new CountingRunnable();

    future.addListener(before, directExecutor());
    future.addListener(bad, directExecutor());
    future.addListener(after, directExecutor());

    future.doSet(1); // BadRunnableException must not propagate.

    before.assertRun();
    after.assertRun();
  }

  @SuppressWarnings("nullness") // test of a bogus call
  public void testNullListener() {
    assertThrows(NullPointerException.class, () -> future.addListener(null, directExecutor()));
  }

  @SuppressWarnings("nullness") // test of a bogus call
  public void testNullExecutor() {
    assertThrows(NullPointerException.class, () -> future.addListener(doNothing(), null));
  }

  @SuppressWarnings("nullness") // test of a bogus call
  public void testNullTimeUnit() {
    future.doSet(1);
    assertThrows(NullPointerException.class, () -> future.get(0, null));
  }

  public void testNegativeTimeout() throws Exception {
    future.doSet(1);
    assertEquals(1, future.get(-1, SECONDS).intValue());
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testOverflowTimeout() throws Exception {
    // First, sanity check that naive multiplication would really overflow to a negative number:
    long nanosPerSecond = NANOSECONDS.convert(1, SECONDS);
    assertThat(nanosPerSecond * Long.MAX_VALUE).isLessThan(0L);

    // Check that we wait long enough anyway (presumably as long as MAX_VALUE nanos):
    TimedWaiterThread waiter = new TimedWaiterThread(future, Long.MAX_VALUE, SECONDS);
    waiter.start();
    waiter.awaitWaiting();

    future.doSet(1);
    waiter.join();
  }

  @J2ktIncompatible // TODO(b/324550390): Enable
  public void testSetNull() throws Exception {
    future.doSet(null);
    assertSuccessful(future, null);
  }

  @SuppressWarnings("nullness") // test of a bogus call
  public void testSetExceptionNull() throws Exception {
    assertThrows(NullPointerException.class, () -> future.doSetException(null));

    assertThat(future.isDone()).isFalse();
    assertThat(future.doSet(1)).isTrue();
    assertSuccessful(future, 1);
  }

  @SuppressWarnings("nullness") // test of a bogus call
  public void testSetFutureNull() throws Exception {
    assertThrows(NullPointerException.class, () -> future.doSetFuture(null));

    assertThat(future.isDone()).isFalse();
    assertThat(future.doSet(1)).isTrue();
    assertSuccessful(future, 1);
  }

  private static final class CountingRunnable implements Runnable {
    int count;

    @Override
    public void run() {
      count++;
    }

    void assertNotRun() {
      assertEquals(0, count);
    }

    void assertRun() {
      assertEquals(1, count);
    }
  }

  private static <
          FutureT extends
              AbstractFuture<@Nullable Integer> & SettableAbstractFuture<@Nullable Integer>>
      void assertSetAsynchronously(FutureT future) {
    assertCannotSet(future);
    assertPending(future);
  }

  private static void assertPending(AbstractFuture<?> future) {
    assertThat(future.isDone()).isFalse();
    assertThat(future.isCancelled()).isFalse();

    CountingRunnable listener = new CountingRunnable();
    future.addListener(listener, directExecutor());
    listener.assertNotRun();

    verifyGetOnPendingFuture(future);
    verifyTimedGetOnPendingFuture(future);
  }

  private static <
          FutureT extends
              AbstractFuture<@Nullable Integer> & SettableAbstractFuture<@Nullable Integer>>
      void assertSuccessful(FutureT future, @Nullable Integer expectedResult)
          throws ExecutionException {
    assertDone(future);
    assertThat(future.isCancelled()).isFalse();

    assertThat(getDone(future)).isEqualTo(expectedResult);
    assertThat(getDoneFromTimeoutOverload(future)).isEqualTo(expectedResult);
  }

  private static <
          FutureT extends
              AbstractFuture<@Nullable Integer> & SettableAbstractFuture<@Nullable Integer>>
      void assertFailed(FutureT future, Throwable expectedException) {
    assertDone(future);
    assertThat(future.isCancelled()).isFalse();

    ExecutionException e1 = assertThrows(ExecutionException.class, () -> getDone(future));
    assertThat(e1).hasCauseThat().isEqualTo(expectedException);

    ExecutionException e2 =
        assertThrows(ExecutionException.class, () -> getDoneFromTimeoutOverload(future));
    assertThat(e2).hasCauseThat().isEqualTo(expectedException);
  }

  private static <
          FutureT extends
              AbstractFuture<@Nullable Integer> & SettableAbstractFuture<@Nullable Integer>>
      void assertCancelled(FutureT future, boolean expectWasInterrupted) {
    assertDone(future);
    assertThat(future.isCancelled()).isTrue();
    assertThat(future.doWasInterrupted()).isEqualTo(expectWasInterrupted);

    assertThrows(CancellationException.class, () -> getDone(future));

    assertThrows(CancellationException.class, () -> getDoneFromTimeoutOverload(future));
  }

  private static <
          FutureT extends
              AbstractFuture<@Nullable Integer> & SettableAbstractFuture<@Nullable Integer>>
      void assertDone(FutureT future) {
    CountingRunnable listener = new CountingRunnable();
    future.addListener(listener, directExecutor());
    listener.assertRun();

    assertThat(future.isDone()).isTrue();
    assertCannotSet(future);
    assertCannotCancel(future);
  }

  private static <
          FutureT extends
              AbstractFuture<@Nullable Integer> & SettableAbstractFuture<@Nullable Integer>>
      void assertCannotSet(FutureT future) {
    assertThat(future.doSet(99)).isFalse();
    assertThat(future.doSetException(new IndexOutOfBoundsException())).isFalse();
    assertThat(future.doSetFuture(new AbstractFuture<Integer>() {})).isFalse();
    assertThat(future.doSetFuture(immediateFuture(99))).isFalse();
  }

  private static void assertCannotCancel(AbstractFuture<?> future) {
    assertThat(future.cancel(true)).isFalse();
    assertThat(future.cancel(false)).isFalse();
  }
}
