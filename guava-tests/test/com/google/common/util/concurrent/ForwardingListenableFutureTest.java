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

/**
 * Tests for {@link ForwardingListenableFuture}.
 *
 * @author Shardul Deo
 */
public class ForwardingListenableFutureTest extends TestCase {

  private SettableFuture<String> delegate;
  private ListenableFuture<String> forwardingFuture;

  private ListenableFutureTester tester;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    delegate = SettableFuture.create();
    forwardingFuture = new ForwardingListenableFuture<String>() {
      @Override
      protected ListenableFuture<String> delegate() {
        return delegate;
      }
    };
    tester = new ListenableFutureTester(forwardingFuture);
    tester.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    tester.tearDown();
    super.tearDown();
  }

  public void testCompletedFuture() throws Exception {
    delegate.set("foo");
    tester.testCompletedFuture("foo");
  }

  public void testCancelledFuture() throws Exception {
    delegate.cancel(true); // parameter is ignored
    tester.testCancelledFuture();
  }

  public void testFailedFuture() throws Exception {
    delegate.setException(new Exception("failed"));
    tester.testFailedFuture("failed");
  }
}
