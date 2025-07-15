/*
 * Copyright (C) 2011 The Guava Authors
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
import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.util.concurrent.TestExceptions.SomeError;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Test for {@link FutureCallback}.
 *
 * @author Anthony Zana
 */
@GwtCompatible
@NullUnmarked
public class FutureCallbackTest extends TestCase {
  public void testSameThreadSuccess() {
    SettableFuture<String> f = SettableFuture.create();
    MockCallback callback = new MockCallback("foo");
    addCallback(f, callback, directExecutor());
    f.set("foo");
  }

  public void testExecutorSuccess() {
    CountingSameThreadExecutor ex = new CountingSameThreadExecutor();
    SettableFuture<String> f = SettableFuture.create();
    MockCallback callback = new MockCallback("foo");
    Futures.addCallback(f, callback, ex);
    f.set("foo");
    assertEquals(1, ex.runCount);
  }

  // Error cases
  public void testSameThreadExecutionException() {
    SettableFuture<String> f = SettableFuture.create();
    Exception e = new IllegalArgumentException("foo not found");
    MockCallback callback = new MockCallback(e);
    addCallback(f, callback, directExecutor());
    f.setException(e);
  }

  public void testCancel() {
    SettableFuture<String> f = SettableFuture.create();
    FutureCallback<String> callback =
        new FutureCallback<String>() {
          private final Object monitor = new Object();
          private boolean called = false;

          @Override
          public void onSuccess(String result) {
            fail("Was not expecting onSuccess() to be called.");
          }

          @Override
          public void onFailure(Throwable t) {
            synchronized (monitor) {
              assertFalse(called);
              assertThat(t).isInstanceOf(CancellationException.class);
              called = true;
            }
          }
        };
    addCallback(f, callback, directExecutor());
    f.cancel(true);
  }

  public void testThrowErrorFromGet() {
    Error error = new AssertionError("ASSERT!");
    ListenableFuture<String> f = UncheckedThrowingFuture.throwingError(error);
    MockCallback callback = new MockCallback(error);
    addCallback(f, callback, directExecutor());
  }

  public void testRuntimeExceptionFromGet() {
    RuntimeException e = new IllegalArgumentException("foo not found");
    ListenableFuture<String> f = UncheckedThrowingFuture.throwingRuntimeException(e);
    MockCallback callback = new MockCallback(e);
    addCallback(f, callback, directExecutor());
  }

  public void testOnSuccessThrowsRuntimeException() throws Exception {
    RuntimeException exception = new RuntimeException();
    String result = "result";
    SettableFuture<String> future = SettableFuture.create();
    int[] successCalls = new int[1];
    int[] failureCalls = new int[1];
    FutureCallback<String> callback =
        new FutureCallback<String>() {
          @Override
          public void onSuccess(String result) {
            successCalls[0]++;
            throw exception;
          }

          @Override
          public void onFailure(Throwable t) {
            failureCalls[0]++;
          }
        };
    addCallback(future, callback, directExecutor());
    future.set(result);
    assertEquals(result, future.get());
    assertThat(successCalls[0]).isEqualTo(1);
    assertThat(failureCalls[0]).isEqualTo(0);
  }

  public void testOnSuccessThrowsError() throws Exception {
    SomeError error = new SomeError();
    String result = "result";
    SettableFuture<String> future = SettableFuture.create();
    int[] successCalls = new int[1];
    int[] failureCalls = new int[1];
    FutureCallback<String> callback =
        new FutureCallback<String>() {
          @Override
          public void onSuccess(String result) {
            successCalls[0]++;
            throw error;
          }

          @Override
          public void onFailure(Throwable t) {
            failureCalls[0]++;
          }
        };
    addCallback(future, callback, directExecutor());
    SomeError e = assertThrows(SomeError.class, () -> future.set(result));
    assertSame(error, e);
    assertEquals(result, future.get());
    assertThat(successCalls[0]).isEqualTo(1);
    assertThat(failureCalls[0]).isEqualTo(0);
  }

  public void testWildcardFuture() {
    SettableFuture<String> settable = SettableFuture.create();
    ListenableFuture<?> f = settable;
    FutureCallback<@Nullable Object> callback =
        new FutureCallback<@Nullable Object>() {
          @Override
          public void onSuccess(@Nullable Object result) {}

          @Override
          public void onFailure(Throwable t) {}
        };
    addCallback(f, callback, directExecutor());
  }

  private class CountingSameThreadExecutor implements Executor {
    int runCount = 0;

    @Override
    public void execute(Runnable command) {
      command.run();
      runCount++;
    }
  }

  private final class MockCallback implements FutureCallback<String> {
    @Nullable private String value = null;
    @Nullable private Throwable failure = null;
    private boolean wasCalled = false;
    private final Object monitor = new Object();

    MockCallback(String expectedValue) {
      this.value = expectedValue;
    }

    MockCallback(Throwable expectedFailure) {
      this.failure = expectedFailure;
    }

    @Override
    public void onSuccess(String result) {
      synchronized (monitor) {
        assertFalse(wasCalled);
        wasCalled = true;
        assertEquals(value, result);
      }
    }

    @Override
    public synchronized void onFailure(Throwable t) {
      synchronized (monitor) {
        assertFalse(wasCalled);
        wasCalled = true;
        assertEquals(failure, t);
      }
    }
  }
}
