/*
 * Copyright (C) 2009 The Guava Authors
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

import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test cases for {@link SettableFuture}.
 *
 * @author Sven Mawson
 */
public class SettableFutureTest extends TestCase {

  private SettableFuture<String> future;
  private ListenableFutureTester tester;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    future = SettableFuture.create();
    tester = new ListenableFutureTester(future);
    tester.setUp();
  }

  public void testDefaultState() throws Exception {
    try {
      future.get(5, TimeUnit.MILLISECONDS);
      fail();
    } catch (TimeoutException expected) {}
  }

  public void testSetValue() throws Exception {
    assertTrue(future.set("value"));
    tester.testCompletedFuture("value");
  }

  public void testSetFailure() throws Exception {
    assertTrue(future.setException(new Exception("failure")));
    tester.testFailedFuture("failure");
  }

  public void testSetFailureNull() throws Exception {
    try {
      future.setException(null);
      fail();
    } catch (NullPointerException expected) {
    }
    assertFalse(future.isDone());
    assertTrue(future.setException(new Exception("failure")));
    tester.testFailedFuture("failure");
  }

  public void testCancel() throws Exception {
    assertTrue(future.cancel(true));
    tester.testCancelledFuture();
  }
}
