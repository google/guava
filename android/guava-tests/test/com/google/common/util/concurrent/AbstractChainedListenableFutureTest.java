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

import com.google.common.util.concurrent.testing.MockFutureListener;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;

/**
 * Unit tests for any listenable future that chains other listenable futures. Unit tests need only
 * override buildChainingFuture and getSuccessfulResult, but they can add custom tests as needed.
 *
 * @author Nishant Thakkar
 */
public abstract class AbstractChainedListenableFutureTest<T> extends TestCase {
  protected static final int EXCEPTION_DATA = -1;
  protected static final int VALID_INPUT_DATA = 1;
  protected static final Exception EXCEPTION = new Exception("Test exception");

  protected SettableFuture<Integer> inputFuture;
  protected ListenableFuture<T> resultFuture;
  protected MockFutureListener listener;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    inputFuture = SettableFuture.create();
    resultFuture = buildChainingFuture(inputFuture);
    listener = new MockFutureListener(resultFuture);
  }

  public void testFutureGetBeforeCallback() throws Exception {
    // Verify that get throws a timeout exception before the callback is called.
    try {
      resultFuture.get(1L, TimeUnit.MILLISECONDS);
      fail("The data is not yet ready, so a TimeoutException is expected");
    } catch (TimeoutException expected) {
    }
  }

  public void testFutureGetThrowsWrappedException() throws Exception {
    inputFuture.setException(EXCEPTION);
    listener.assertException(EXCEPTION);
  }

  public void testFutureGetThrowsWrappedError() throws Exception {
    Error error = new Error();
    inputFuture.setException(error);
    // Verify that get throws an ExecutionException, caused by an Error, when
    // the callback is called.
    listener.assertException(error);
  }

  public void testAddListenerAfterCallback() throws Throwable {
    inputFuture.set(VALID_INPUT_DATA);

    listener.assertSuccess(getSuccessfulResult());
  }

  public void testFutureBeforeCallback() throws Throwable {
    inputFuture.set(VALID_INPUT_DATA);

    listener.assertSuccess(getSuccessfulResult());
  }

  public void testInputFutureToString() throws Throwable {
    assertThat(resultFuture.toString()).contains(inputFuture.toString());
  }

  /**
   * Override to return a chaining listenableFuture that returns the result of getSuccessfulResult()
   * when inputFuture returns VALID_INPUT_DATA, and sets the exception to EXCEPTION in all other
   * cases.
   */
  protected abstract ListenableFuture<T> buildChainingFuture(ListenableFuture<Integer> inputFuture);

  /**
   * Override to return the result when VALID_INPUT_DATA is passed in to the chaining
   * listenableFuture
   */
  protected abstract T getSuccessfulResult();
}
