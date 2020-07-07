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

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;

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
    } catch (TimeoutException expected) {
    }
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

  /** Tests the initial state of the future. */
  public void testCreate() throws Exception {
    SettableFuture<Integer> future = SettableFuture.create();
    assertFalse(future.isDone());
    assertFalse(future.isCancelled());
  }

  public void testSetValue_simpleThreaded() throws Exception {
    SettableFuture<Integer> future = SettableFuture.create();
    assertTrue(future.set(42));
    // Later attempts to set the future should return false.
    assertFalse(future.set(23));
    assertFalse(future.setException(new Exception("bar")));
    assertFalse(future.setFuture(SettableFuture.<Integer>create()));
    // Check that the future has been set properly.
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
    assertEquals(42, (int) future.get());
  }

  public void testSetException() throws Exception {
    SettableFuture<Object> future = SettableFuture.create();
    Exception e = new Exception("foobarbaz");
    assertTrue(future.setException(e));
    // Later attempts to set the future should return false.
    assertFalse(future.set(23));
    assertFalse(future.setException(new Exception("quux")));
    assertFalse(future.setFuture(SettableFuture.create()));
    // Check that the future has been set properly.
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
    try {
      future.get();
      fail("Expected ExecutionException");
    } catch (ExecutionException ee) {
      assertThat(ee).hasCauseThat().isSameInstanceAs(e);
    }
  }

  public void testSetFuture() throws Exception {
    SettableFuture<String> future = SettableFuture.create();
    SettableFuture<String> nested = SettableFuture.create();
    assertTrue(future.setFuture(nested));
    // Later attempts to set the future should return false.
    assertFalse(future.set("x"));
    assertFalse(future.setException(new Exception("bar")));
    assertFalse(future.setFuture(SettableFuture.<String>create()));
    // Check that the future has been set properly.
    assertFalse(future.isDone());
    assertFalse(future.isCancelled());
    try {
      future.get(0, TimeUnit.MILLISECONDS);
      fail("Expected TimeoutException");
    } catch (TimeoutException expected) {
      /* expected */
    }
    nested.set("foo");
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
    assertEquals("foo", future.get());
  }

  private static class Foo {}

  private static class FooChild extends Foo {}

  public void testSetFuture_genericsHierarchy() throws Exception {
    SettableFuture<Foo> future = SettableFuture.create();
    SettableFuture<FooChild> nested = SettableFuture.create();
    assertTrue(future.setFuture(nested));
    // Later attempts to set the future should return false.
    assertFalse(future.set(new Foo()));
    assertFalse(future.setException(new Exception("bar")));
    assertFalse(future.setFuture(SettableFuture.<Foo>create()));
    // Check that the future has been set properly.
    assertFalse(future.isDone());
    assertFalse(future.isCancelled());
    try {
      future.get(0, TimeUnit.MILLISECONDS);
      fail("Expected TimeoutException");
    } catch (TimeoutException expected) {
      /* expected */
    }
    FooChild value = new FooChild();
    nested.set(value);
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
    assertSame(value, future.get());
  }

  public void testCancel_innerCancelsAsync() throws Exception {
    SettableFuture<Object> async = SettableFuture.create();
    SettableFuture<Object> inner = SettableFuture.create();
    async.setFuture(inner);
    inner.cancel(true);
    assertTrue(async.isCancelled());
    try {
      async.get();
      fail("Expected CancellationException");
    } catch (CancellationException expected) {
      /* expected */
    }
  }

  public void testCancel_resultCancelsInner_interrupted() throws Exception {
    SettableFuture<Object> async = SettableFuture.create();
    SettableFuture<Object> inner = SettableFuture.create();
    async.setFuture(inner);
    async.cancel(true);
    assertTrue(inner.isCancelled());
    assertTrue(inner.wasInterrupted());
    try {
      inner.get();
      fail("Expected CancellationException");
    } catch (CancellationException expected) {
      /* expected */
    }
  }

  public void testCancel_resultCancelsInner() throws Exception {
    SettableFuture<Object> async = SettableFuture.create();
    SettableFuture<Object> inner = SettableFuture.create();
    async.setFuture(inner);
    async.cancel(false);
    assertTrue(inner.isCancelled());
    assertFalse(inner.wasInterrupted());
    try {
      inner.get();
      fail("Expected CancellationException");
    } catch (CancellationException expected) {
      /* expected */
    }
  }

  public void testCancel_beforeSet() throws Exception {
    SettableFuture<Object> async = SettableFuture.create();
    async.cancel(true);
    assertFalse(async.set(42));
  }

  public void testCancel_multipleBeforeSetFuture_noInterruptFirst() throws Exception {
    SettableFuture<Object> async = SettableFuture.create();
    async.cancel(false);
    async.cancel(true);
    SettableFuture<Object> inner = SettableFuture.create();
    assertFalse(async.setFuture(inner));
    assertTrue(inner.isCancelled());
    assertFalse(inner.wasInterrupted());
  }

  public void testCancel_multipleBeforeSetFuture_interruptFirst() throws Exception {
    SettableFuture<Object> async = SettableFuture.create();
    async.cancel(true);
    async.cancel(false);
    SettableFuture<Object> inner = SettableFuture.create();
    assertFalse(async.setFuture(inner));
    assertTrue(inner.isCancelled());
    assertTrue(inner.wasInterrupted());
  }
}
