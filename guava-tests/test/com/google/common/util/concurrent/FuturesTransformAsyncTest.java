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
import static com.google.common.util.concurrent.Futures.transformAsync;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;

import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/**
 * Unit tests for {@link Futures#transformAsync(ListenableFuture, AsyncFunction, Executor)}.
 *
 * @author Nishant Thakkar
 */
public class FuturesTransformAsyncTest extends AbstractChainedListenableFutureTest<String> {
  protected static final int SLOW_OUTPUT_VALID_INPUT_DATA = 2;
  protected static final int SLOW_FUNC_VALID_INPUT_DATA = 3;
  private static final String RESULT_DATA = "SUCCESS";

  private SettableFuture<String> outputFuture;
  // Signals that the function is waiting to complete
  private CountDownLatch funcIsWaitingLatch;
  // Signals the function so it will complete
  private CountDownLatch funcCompletionLatch;

  @Override
  protected ListenableFuture<String> buildChainingFuture(ListenableFuture<Integer> inputFuture) {
    outputFuture = SettableFuture.create();
    funcIsWaitingLatch = new CountDownLatch(1);
    funcCompletionLatch = new CountDownLatch(1);
    return transformAsync(inputFuture, new ChainingFunction(), directExecutor());
  }

  @Override
  protected String getSuccessfulResult() {
    return RESULT_DATA;
  }

  private class ChainingFunction implements AsyncFunction<Integer, String> {
    @Override
    public ListenableFuture<String> apply(Integer input) throws Exception {
      switch (input) {
        case VALID_INPUT_DATA:
          outputFuture.set(RESULT_DATA);
          break;
        case SLOW_OUTPUT_VALID_INPUT_DATA:
          break; // do nothing to the result
        case SLOW_FUNC_VALID_INPUT_DATA:
          funcIsWaitingLatch.countDown();
          awaitUninterruptibly(funcCompletionLatch);
          break;
        case EXCEPTION_DATA:
          throw EXCEPTION;
      }
      return outputFuture;
    }
  }

  public void testFutureGetThrowsFunctionException() throws Exception {
    inputFuture.set(EXCEPTION_DATA);
    listener.assertException(EXCEPTION);
  }

  public void testFutureGetThrowsCancellationIfInputCancelled() throws Exception {
    inputFuture.cancel(true); // argument is ignored
    try {
      resultFuture.get();
      fail("Result future must throw CancellationException" + " if input future is cancelled.");
    } catch (CancellationException expected) {
    }
  }

  public void testFutureGetThrowsCancellationIfOutputCancelled() throws Exception {
    inputFuture.set(SLOW_OUTPUT_VALID_INPUT_DATA);
    outputFuture.cancel(true); // argument is ignored
    try {
      resultFuture.get();
      fail(
          "Result future must throw CancellationException"
              + " if function output future is cancelled.");
    } catch (CancellationException expected) {
    }
  }

  public void testAsyncToString() throws Exception {
    inputFuture.set(SLOW_OUTPUT_VALID_INPUT_DATA);
    assertThat(resultFuture.toString()).contains(outputFuture.toString());
  }

  public void testFutureCancelBeforeInputCompletion() throws Exception {
    assertTrue(resultFuture.cancel(true));
    assertTrue(resultFuture.isCancelled());
    assertTrue(inputFuture.isCancelled());
    assertFalse(outputFuture.isCancelled());
    try {
      resultFuture.get();
      fail("Result future is cancelled and should have thrown a" + " CancellationException");
    } catch (CancellationException expected) {
    }
  }

  public void testFutureCancellableBeforeOutputCompletion() throws Exception {
    inputFuture.set(SLOW_OUTPUT_VALID_INPUT_DATA);
    assertTrue(resultFuture.cancel(true));
    assertTrue(resultFuture.isCancelled());
    assertFalse(inputFuture.isCancelled());
    assertTrue(outputFuture.isCancelled());
    try {
      resultFuture.get();
      fail("Result future is cancelled and should have thrown a" + " CancellationException");
    } catch (CancellationException expected) {
    }
  }

  public void testFutureCancellableBeforeFunctionCompletion() throws Exception {
    // Set the result in a separate thread since this test runs the function
    // (which will block) in the same thread.
    new Thread() {
      @Override
      public void run() {
        inputFuture.set(SLOW_FUNC_VALID_INPUT_DATA);
      }
    }.start();
    funcIsWaitingLatch.await();

    assertTrue(resultFuture.cancel(true));
    assertTrue(resultFuture.isCancelled());
    assertFalse(inputFuture.isCancelled());
    assertFalse(outputFuture.isCancelled());
    try {
      resultFuture.get();
      fail("Result future is cancelled and should have thrown a" + " CancellationException");
    } catch (CancellationException expected) {
    }

    funcCompletionLatch.countDown(); // allow the function to complete
    try {
      outputFuture.get();
      fail(
          "The function output future is cancelled and should have thrown a"
              + " CancellationException");
    } catch (CancellationException expected) {
    }
  }

  public void testFutureCancelAfterCompletion() throws Exception {
    inputFuture.set(VALID_INPUT_DATA);
    assertFalse(resultFuture.cancel(true));
    assertFalse(resultFuture.isCancelled());
    assertFalse(inputFuture.isCancelled());
    assertFalse(outputFuture.isCancelled());
    assertEquals(RESULT_DATA, resultFuture.get());
  }

  public void testFutureGetThrowsRuntimeException() throws Exception {
    BadFuture badInput = new BadFuture(Futures.immediateFuture(20));
    ListenableFuture<String> chain = buildChainingFuture(badInput);
    try {
      chain.get();
      fail("Future.get must throw an exception when the input future fails.");
    } catch (ExecutionException e) {
      assertSame(RuntimeException.class, e.getCause().getClass());
    }
  }

  /** Proxy to throw a {@link RuntimeException} out of the {@link #get()} method. */
  public static class BadFuture extends SimpleForwardingListenableFuture<Integer> {
    protected BadFuture(ListenableFuture<Integer> delegate) {
      super(delegate);
    }

    @Override
    public Integer get() {
      throw new RuntimeException("Oops");
    }
  }
}
