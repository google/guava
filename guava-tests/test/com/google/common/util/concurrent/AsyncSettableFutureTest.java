/*
 * Copyright (C) 2012 The Guava Authors
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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tests that {@code AsyncSettableFuture} is a valid {@link ListenableFuture}
 * that behaves itself as expected.
 */

public class AsyncSettableFutureTest extends TestCase {

  private static class Foo {}
  private static class FooChild extends Foo {}

  /** Tests the initial state of the future. */
  public void testCreate() throws Exception {
    AsyncSettableFuture<Integer> future = AsyncSettableFuture.create();
    assertFalse(future.isSet());
    assertFalse(future.isDone());
    assertFalse(future.isCancelled());
  }

  public void testSetValue() throws Exception {
    AsyncSettableFuture<Integer> future = AsyncSettableFuture.create();
    assertTrue(future.setValue(42));
    assertTrue(future.isSet());
    // Later attempts to set the future should return false.
    assertFalse(future.setValue(23));
    assertFalse(future.setException(new Exception("bar")));
    assertFalse(future.setFuture(SettableFuture.<Integer>create()));
    // Check that the future has been set properly.
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
    assertEquals(42, (int) future.get());
  }

  public void testSetException() throws Exception {
    AsyncSettableFuture<Object> future = AsyncSettableFuture.create();
    Exception e = new Exception("foobarbaz");
    assertTrue(future.setException(e));
    assertTrue(future.isSet());
    // Later attempts to set the future should return false.
    assertFalse(future.setValue(23));
    assertFalse(future.setException(new Exception("quux")));
    assertFalse(future.setFuture(SettableFuture.create()));
    // Check that the future has been set properly.
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
    try {
      future.get();
      fail("Expected ExecutionException");
    } catch (ExecutionException ee) {
      assertSame(e, ee.getCause());
    }
  }

  public void testSetFuture() throws Exception {
    AsyncSettableFuture<String> future = AsyncSettableFuture.create();
    SettableFuture<String> nested = SettableFuture.create();
    assertTrue(future.setFuture(nested));
    assertTrue(future.isSet());
    // Later attempts to set the future should return false.
    assertFalse(future.setValue("x"));
    assertFalse(future.setException(new Exception("bar")));
    assertFalse(future.setFuture(SettableFuture.<String>create()));
    // Check that the future has been set properly.
    assertFalse(future.isDone());
    assertFalse(future.isCancelled());
    try {
      future.get(0, TimeUnit.MILLISECONDS);
      fail("Expected TimeoutException");
    } catch (TimeoutException expected) { /* expected */ }
    nested.set("foo");
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
    assertEquals("foo", future.get());
  }

  public void testSetFuture_genericsHierarchy() throws Exception {
    AsyncSettableFuture<Foo> future = AsyncSettableFuture.create();
    SettableFuture<FooChild> nested = SettableFuture.create();
    assertTrue(future.setFuture(nested));
    assertTrue(future.isSet());
    // Later attempts to set the future should return false.
    assertFalse(future.setValue(new Foo()));
    assertFalse(future.setException(new Exception("bar")));
    assertFalse(future.setFuture(SettableFuture.<Foo>create()));
    // Check that the future has been set properly.
    assertFalse(future.isDone());
    assertFalse(future.isCancelled());
    try {
      future.get(0, TimeUnit.MILLISECONDS);
      fail("Expected TimeoutException");
    } catch (TimeoutException expected) { /* expected */ }
    FooChild value = new FooChild();
    nested.set(value);
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
    assertSame(value, future.get());
  }

  public void testCancel_innerCancelsAsync() throws Exception {
    AsyncSettableFuture<Object> async = AsyncSettableFuture.create();
    SettableFuture<Object> inner = SettableFuture.create();
    async.setFuture(inner);
    inner.cancel(true);
    assertTrue(async.isCancelled());
    try {
      async.get();
      fail("Expected CancellationException");
    } catch (CancellationException expected) { /* expected */ }
  }

  public void testCancel_resultCancelsInner_interrupted() throws Exception {
    AsyncSettableFuture<Object> async = AsyncSettableFuture.create();
    MyFuture<Object> inner = new MyFuture<Object>();
    async.setFuture(inner);
    async.cancel(true);
    assertTrue(inner.isCancelled());
    assertTrue(inner.myWasInterrupted());
    try {
      inner.get();
      fail("Expected CancellationException");
    } catch (CancellationException expected) { /* expected */ }
  }

  public void testCancel_resultCancelsInner() throws Exception {
    AsyncSettableFuture<Object> async = AsyncSettableFuture.create();
    MyFuture<Object> inner = new MyFuture<Object>();
    async.setFuture(inner);
    async.cancel(false);
    assertTrue(inner.isCancelled());
    assertFalse(inner.myWasInterrupted());
    try {
      inner.get();
      fail("Expected CancellationException");
    } catch (CancellationException expected) { /* expected */ }
  }

  public void testCancel_beforeSet() throws Exception {
    AsyncSettableFuture<Object> async = AsyncSettableFuture.create();
    async.cancel(true);
    assertFalse(async.setValue(42));
  }

  public void testCancel_multipleBeforeSetFuture_noInterruptFirst() throws Exception {
    AsyncSettableFuture<Object> async = AsyncSettableFuture.create();
    async.cancel(false);
    async.cancel(true);
    MyFuture<Object> inner = new MyFuture<Object>();
    assertFalse(async.setFuture(inner));
    assertTrue(inner.isCancelled());
    assertFalse(inner.myWasInterrupted());
  }

  public void testCancel_multipleBeforeSetFuture_interruptFirst() throws Exception {
    AsyncSettableFuture<Object> async = AsyncSettableFuture.create();
    async.cancel(true);
    async.cancel(false);
    MyFuture<Object> inner = new MyFuture<Object>();
    assertFalse(async.setFuture(inner));
    assertTrue(inner.isCancelled());
    assertTrue(inner.myWasInterrupted());
  }

  private static class MyFuture<V> extends AbstractFuture<V> {
    boolean myWasInterrupted() {
      // we need a new method since wasInterrupted is final, so we can't increase its visibility.
      return wasInterrupted();
    }
  }
}
