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
 * Unit tests for {@code Futures.makeListenable}.
 *
 * @author Sven Mawson
 */
@SuppressWarnings("deprecation") // method is deprecated to be made private
public class FuturesAdapterTest extends TestCase {

  private SettableFuture<String> delegate;

  private ListenableFutureTester tester;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    delegate = SettableFuture.create();
    tester = new ListenableFutureTester(Futures.makeListenable(delegate));
    tester.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    tester.tearDown();
    super.tearDown();
  }

  public void testAdapterExecutesListenersWhenSourceFutureCompletes()
      throws Exception {
    delegate.set("foo");
    tester.testCompletedFuture("foo");
  }

  public void testAdapterHandlesCancellationCorrectly() throws Exception {
    delegate.cancel(true); // parameter is ignored
    tester.testCancelledFuture();
  }

  public void testAdapterHandlesExecutionExceptions() throws Exception {
    delegate.setException(new Exception("failed"));
    tester.testFailedFuture("failed");
  }
}
