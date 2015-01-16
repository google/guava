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
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.TestPlatform.verifyGetOnPendingFuture;
import static com.google.common.util.concurrent.TestPlatform.verifyTimedGetOnPendingFuture;

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Base class for tests for emulated {@link AbstractFuture} that allow subclasses to swap in a
 * different "source Future" for {@link AbstractFuture#setFuture} calls.
 */
@GwtCompatible
abstract class AbstractAbstractFutureTest extends TestCase {
  private TestedFuture<Integer> future;
  private AbstractFuture<Integer> delegate;

  abstract AbstractFuture<Integer> newDelegate();

  @Override
  protected void setUp() {
    future = TestedFuture.create();
    delegate = newDelegate();
  }

  public void testPending() {
    assertThat(future.isDone()).isFalse();
    assertThat(future.isCancelled()).isFalse();

    verifyGetOnPendingFuture(future);
    verifyTimedGetOnPendingFuture(future);
  }

  public void testResolved() throws Exception {
    assertThat(future.set(1)).isTrue();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isFalse();

    assertThat(future.get()).is(1);
    assertThat(future.get(0, TimeUnit.SECONDS)).is(1);
  }

  public void testResolved_afterResolved() throws Exception {
    future.set(2);

    assertThat(future.set(3)).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isFalse();

    assertThat(future.get()).is(2);
  }

  public void testResolved_afterException() throws Exception {
    Exception cause = new Exception();
    future.setException(cause);

    assertThat(future.set(3)).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isFalse();

    try {
      future.get();
      fail();
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isSameAs(cause);
    }
  }

  public void testResolved_afterCanceled() throws Exception {
    future.cancel(false /* mayInterruptIfRunning */);

    assertThat(future.set(3)).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();

    try {
      future.get();
      fail();
    } catch (CancellationException expected) {
    }
  }

  public void testResolved_afterInterrupted() throws Exception {
    future.cancel(true /* mayInterruptIfRunning */);

    assertThat(future.set(3)).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();

    try {
      future.get();
      fail();
    } catch (CancellationException expected) {
    }
  }

  public void testResolved_afterDelegated() throws Exception {
    future.setFuture(delegate);

    assertThat(future.set(3)).isFalse();

    assertThat(future.isDone()).isFalse();
    assertThat(future.isCancelled()).isFalse();

    verifyGetOnPendingFuture(future);
  }

  public void testExceptional() throws Exception {
    Exception cause = new Exception();
    assertThat(future.setException(cause)).isTrue();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isFalse();

    try {
      future.get();
      fail();
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isSameAs(cause);
    }

    try {
      future.get(0, TimeUnit.SECONDS);
      fail();
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isSameAs(cause);
    }
  }

  public void testExceptional_afterResolved() throws Exception {
    future.set(3);

    assertThat(future.setException(new Exception())).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isFalse();

    assertThat(future.get()).is(3);
  }

  public void testExceptional_afterException() throws Exception {
    Exception cause = new Exception();
    future.setException(cause);

    assertThat(future.setException(new Exception())).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isFalse();

    try {
      future.get();
      fail();
    } catch (ExecutionException e) {
      assertThat(e.getCause()).isSameAs(cause);
    }
  }

  public void testExceptional_afterCancelled() throws Exception {
    future.cancel(false /* mayInterruptIfRunning */);

    assertThat(future.setException(new Exception())).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();

    try {
      future.get();
    } catch (CancellationException expected) {
    }
  }

  public void testExceptional_afterInterrupted() throws Exception {
    future.cancel(true /* mayInterruptIfRunning */);

    assertThat(future.setException(new Exception())).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();

    try {
      future.get();
    } catch (CancellationException expected) {
    }
  }

  public void testExceptional_afterDelegated() throws Exception {
    future.setFuture(delegate);

    assertThat(future.setException(new Exception())).isFalse();

    assertThat(future.isDone()).isFalse();
    assertThat(future.isCancelled()).isFalse();

    verifyGetOnPendingFuture(future);
  }

  public void testCancelled() throws Exception {
    assertThat(future.cancel(false /* mayInterruptIfRunning */)).isTrue();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();

    try {
      future.get();
      fail();
    } catch (CancellationException expected) {
    }

    try {
      future.get(0, TimeUnit.SECONDS);
      fail();
    } catch (CancellationException expected) {
    }
  }

  public void testCancelled_afterResolved() throws Exception {
    future.set(1);

    assertThat(future.cancel(false /* mayInterruptIfRunning */)).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isFalse();
  }

  public void testCancelled_afterException() throws Exception {
    future.setException(new Exception());

    assertThat(future.cancel(false /* mayInterruptIfRunning */)).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isFalse();
  }

  public void testCancelled_afterCancelled() throws Exception {
    future.cancel(false /* mayInterruptIfRunning */);

    assertThat(future.cancel(false /* mayInterruptIfRunning */)).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
  }

  public void testCancelled_afterInterrupted() throws Exception {
    future.cancel(true /* mayInterruptIfRunning */);

    assertThat(future.cancel(false /* mayInterruptIfRunning */)).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
  }

  public void testCancelled_afterDelegated() throws Exception {
    future.setFuture(delegate);

    assertThat(future.cancel(false /* mayInterruptIfRunning */)).isTrue();

    assertThat(delegate.isDone()).isTrue();
    assertThat(delegate.isCancelled()).isTrue();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
  }

  public void testInterrupted() throws Exception {
    future.cancel(true /* mayInterruptIfRunning */);

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();

    try {
      future.get();
      fail();
    } catch (CancellationException expected) {
    }

    try {
      future.get(0, TimeUnit.SECONDS);
      fail();
    } catch (CancellationException expected) {
    }
  }

  public void testInterrupted_afterResolved() throws Exception {
    future.set(1);

    assertThat(future.cancel(true /* mayInterruptIfRunning */)).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isFalse();
  }

  public void testInterrupted_afterException() throws Exception {
    future.setException(new Exception());

    assertThat(future.cancel(true /* mayInterruptIfRunning */)).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isFalse();
  }

  public void testInterrupted_afterCancelled() throws Exception {
    future.cancel(false /* mayInterruptIfRunning */);

    assertThat(future.cancel(true /* mayInterruptIfRunning */)).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
  }

  public void testInterrupted_afterInterrupted() throws Exception {
    future.cancel(true /* mayInterruptIfRunning */);

    assertThat(future.cancel(true /* mayInterruptIfRunning */)).isFalse();

    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
  }

  public void testInterrupted_afterDelegated() throws Exception {
    future.setFuture(delegate);

    assertThat(future.cancel(false /* mayInterruptIfRunning */)).isTrue();

    assertThat(delegate.isDone()).isTrue();
    assertThat(delegate.isCancelled()).isTrue();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
  }

  public void testDelegated_delegateIsStillPending() throws Exception {
    assertThat(future.setFuture(delegate)).isTrue();

    assertThat(delegate.isDone()).isFalse();
    assertThat(delegate.isCancelled()).isFalse();
    assertThat(future.isDone()).isFalse();
    assertThat(future.isCancelled()).isFalse();

    verifyGetOnPendingFuture(future);
    verifyTimedGetOnPendingFuture(future);
  }

  public void testDelegated_delegateWasResolved() throws Exception {
    delegate.set(5);

    assertThat(future.setFuture(delegate)).isTrue();

    assertThat(delegate.isDone()).isTrue();
    assertThat(delegate.get()).isEqualTo(5);
    assertThat(delegate.get(0, TimeUnit.SECONDS)).isEqualTo(5);
    assertThat(future.isDone()).isTrue();
    assertThat(future.get()).isEqualTo(5);
    assertThat(future.get(0, TimeUnit.SECONDS)).isEqualTo(5);
  }

  public void testDelegated_delegateIsResolved() throws Exception {
    assertThat(future.setFuture(delegate)).isTrue();

    delegate.set(6);

    assertThat(delegate.isDone()).isTrue();
    assertThat(delegate.get()).isEqualTo(6);
    assertThat(delegate.get(0, TimeUnit.SECONDS)).isEqualTo(6);
    assertThat(future.isDone()).isTrue();
    assertThat(future.get()).isEqualTo(6);
    assertThat(future.get(0, TimeUnit.SECONDS)).isEqualTo(6);
  }

  public void testDelegated_delegateWasCancelled() throws Exception {
    delegate.cancel(false /** mayInterruptIfRunning */);

    assertThat(future.setFuture(delegate)).isTrue();

    assertThat(delegate.isDone()).isTrue();
    assertThat(delegate.isCancelled()).isTrue();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
  }

  public void testDelegated_delegateIsCancelled() throws Exception {
    assertThat(future.setFuture(delegate)).isTrue();

    delegate.cancel(false /** mayInterruptIfRunning */);

    assertThat(delegate.isDone()).isTrue();
    assertThat(delegate.isCancelled()).isTrue();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
  }

  public void testDelegated_delegateWasInterrupted() throws Exception {
    delegate.cancel(true /** mayInterruptIfRunning */);

    assertThat(future.setFuture(delegate)).isTrue();

    assertThat(delegate.isDone()).isTrue();
    assertThat(delegate.isCancelled()).isTrue();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
  }

  public void testDelegated_delegateIsInterrupted() throws Exception {
    assertThat(future.setFuture(delegate)).isTrue();

    delegate.cancel(true /** mayInterruptIfRunning */);

    assertThat(delegate.isDone()).isTrue();
    assertThat(delegate.isCancelled()).isTrue();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
  }

  public void testDelegated_afterResolved() throws Exception {
    future.set(7);
    delegate.set(5);

    assertThat(future.setFuture(delegate)).isFalse();

    assertThat(future.get()).isEqualTo(7);
  }

  public void testDelegated_afterDelegated() throws Exception {
    TestedFuture<Integer> delegated2 = TestedFuture.create();

    assertThat(future.setFuture(delegate)).isTrue();
    assertThat(future.setFuture(delegated2)).isFalse();

    delegate.set(1);
    delegated2.set(2);

    assertThat(future.get()).isEqualTo(1);
  }

  public void testDelegated_afterCancelled() throws Exception {
    future.cancel(false /** mayInterruptIfRunning */);

    assertThat(future.setFuture(delegate)).isFalse();

    assertThat(future.isCancelled()).isTrue();
    assertThat(delegate.isCancelled()).isTrue();
  }

  public void testListenToPending() {
    RunnableVerifier before = new RunnableVerifier();
    future.addListener(before.expectedToNotRun(), directExecutor());
    before.verify();
  }

  public void testListenToResolved() {
    RunnableVerifier before = new RunnableVerifier();
    RunnableVerifier after = new RunnableVerifier();

    future.addListener(before.expectedToRun(), directExecutor());
    future.set(1);
    future.addListener(after.expectedToRun(), directExecutor());

    before.verify();
    after.verify();
  }

  public void testListenToResolved_misbehavingListener() {
    class BadRunnableException extends RuntimeException {
    }

    Runnable bad = new Runnable() {
      @Override
      public void run() {
        throw new BadRunnableException();
      }
    };

    future.addListener(bad, directExecutor());
    future.set(1); // BadRunnableException must not propagate.
  }

  public void testListenToExceptional() {
    RunnableVerifier before = new RunnableVerifier();
    RunnableVerifier after = new RunnableVerifier();

    future.addListener(before.expectedToRun(), directExecutor());
    future.setException(new Exception());
    future.addListener(after.expectedToRun(), directExecutor());

    before.verify();
    after.verify();
  }

  public void testListenToCancelled() {
    RunnableVerifier before = new RunnableVerifier();
    RunnableVerifier after = new RunnableVerifier();

    future.addListener(before.expectedToRun(), directExecutor());
    future.cancel(false /* mayInterruptIfRunning */);
    future.addListener(after.expectedToRun(), directExecutor());

    before.verify();
    after.verify();
  }

  public void testListenToInterrupted() {
    RunnableVerifier before = new RunnableVerifier();
    RunnableVerifier after = new RunnableVerifier();

    future.addListener(before.expectedToRun(), directExecutor());
    future.cancel(true /* mayInterruptIfRunning */);
    future.addListener(after.expectedToRun(), directExecutor());

    before.verify();
    after.verify();
  }

  public void testListenToDelegatePending() {
    RunnableVerifier before = new RunnableVerifier();
    RunnableVerifier after = new RunnableVerifier();

    future.addListener(before.expectedToNotRun(), directExecutor());
    future.setFuture(delegate);
    future.addListener(after.expectedToNotRun(), directExecutor());

    before.verify();
    after.verify();
  }

  public void testListenToDelegateResolved() {
    RunnableVerifier before = new RunnableVerifier();
    RunnableVerifier inBetween = new RunnableVerifier();
    RunnableVerifier after = new RunnableVerifier();

    future.addListener(before.expectedToRun(), directExecutor());
    future.setFuture(delegate);
    future.addListener(inBetween.expectedToRun(), directExecutor());
    delegate.set(1);
    future.addListener(after.expectedToRun(), directExecutor());

    before.verify();
    inBetween.verify();
    after.verify();
  }

  public void testListenToDelegateExceptional() {
    RunnableVerifier before = new RunnableVerifier();
    RunnableVerifier after = new RunnableVerifier();
    RunnableVerifier inBetween = new RunnableVerifier();

    future.addListener(before.expectedToRun(), directExecutor());
    future.setFuture(delegate);
    future.addListener(inBetween.expectedToRun(), directExecutor());
    delegate.setException(new Exception());
    future.addListener(after.expectedToRun(), directExecutor());

    before.verify();
    inBetween.verify();
    after.verify();
  }

  public void testListenToDelegateCancelled() {
    RunnableVerifier before = new RunnableVerifier();
    RunnableVerifier after = new RunnableVerifier();
    RunnableVerifier inBetween = new RunnableVerifier();

    future.addListener(before.expectedToRun(), directExecutor());
    future.setFuture(delegate);
    future.addListener(inBetween.expectedToRun(), directExecutor());
    delegate.cancel(false /* mayInterruptIfRunning */);
    future.addListener(after.expectedToRun(), directExecutor());

    before.verify();
    after.verify();
  }

  public void testListenToDelegateCancelled_byDelegator() {
    RunnableVerifier before = new RunnableVerifier();
    RunnableVerifier after = new RunnableVerifier();
    RunnableVerifier inBetween = new RunnableVerifier();

    future.addListener(before.expectedToRun(), directExecutor());
    future.setFuture(delegate);
    future.addListener(inBetween.expectedToRun(), directExecutor());
    future.cancel(false /* mayInterruptIfRunning */);
    future.addListener(after.expectedToRun(), directExecutor());

    before.verify();
    after.verify();
  }

  public void testListenToDelegateInterrupted_byDelegator() {
    RunnableVerifier before = new RunnableVerifier();
    RunnableVerifier after = new RunnableVerifier();
    RunnableVerifier inBetween = new RunnableVerifier();

    future.addListener(before.expectedToRun(), directExecutor());
    future.setFuture(delegate);
    future.addListener(inBetween.expectedToRun(), directExecutor());
    future.cancel(false /* mayInterruptIfRunning */);
    future.addListener(after.expectedToRun(), directExecutor());

    before.verify();
    after.verify();
  }

  public void testSetFutureNull() throws Exception {
    try {
      future.setFuture(null);
      fail();
    } catch (NullPointerException expected) {
    }

    assertThat(future.isDone()).isFalse();
    assertThat(future.set(1)).isTrue();
    assertThat(future.get()).isEqualTo(1);
  }

  /**
   * Concrete subclass for testing.
   */
  private static class TestedFuture<V> extends AbstractFuture<V> {
    private static <V> TestedFuture<V> create() {
      return new TestedFuture<V>();
    }
  }

  private static final class RunnableVerifier implements Runnable {

    Expectation expectation = Expectation.UNDEFINED;
    boolean actuallyRan;

    RunnableVerifier expectedToRun() {
      expectation = Expectation.EXPECTED_TO_RUN;
      return this;
    }

    RunnableVerifier expectedToNotRun() {
      expectation = Expectation.EXPECTED_TO_NO_RUN;
      return this;
    }

    @Override
    public void run() {
      this.actuallyRan = true;
    }

    void verify() {
      expectation.verify(actuallyRan);
    }

    enum Expectation {
      UNDEFINED {
        @Override
        void verify(boolean didRun) {
          fail("Did you forget to define the expectation?");
        }
      },
      EXPECTED_TO_RUN {
        @Override
        void verify(boolean actuallyRan) {
          if (!actuallyRan) {
            fail("Runnable was expected to run but it did not.");
          }
        }
      },
      EXPECTED_TO_NO_RUN {
        @Override
        void verify(boolean actuallyRan) {
          if (actuallyRan) {
            fail("Runnable was NOT expected to run but it did.");
          }
        }
      };

      abstract void verify(boolean didRun);
    }
  }
}
