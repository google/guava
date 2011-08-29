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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.common.util.concurrent.ForwardingCheckedFuture.SimpleForwardingCheckedFuture;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test for {@link ForwardingCheckedFuture}
 * 
 * @author Anthony Zana
 */
public class ForwardingCheckedFutureTest extends TestCase {
  private static final String VALUE = "delegated";
  private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

  @SuppressWarnings("unchecked")
  private CheckedFuture<String, IOException> delegate =
      createMock(CheckedFuture.class);

  private TestDelegateFuture forwarded = new TestDelegateFuture();
  private TestSimpleFuture simple = new TestSimpleFuture();

  public void testCheckedGet() throws IOException {
    expect(delegate.checkedGet()).andReturn(VALUE).times(2);
    replay(delegate);
    assertEquals(VALUE, forwarded.checkedGet());
    assertEquals(VALUE, simple.checkedGet());
    verify(delegate);
  }

  public void testTimedCheckedGet() throws TimeoutException, IOException {
    expect(delegate.checkedGet(100, TIME_UNIT)).andReturn(VALUE).times(2);
    replay(delegate);
    assertEquals(VALUE, forwarded.checkedGet(100, TIME_UNIT));
    assertEquals(VALUE, simple.checkedGet(100, TIME_UNIT));
    verify(delegate);
  }

  public void testTimedCheckedGet_timeout()
      throws IOException, TimeoutException {
    expect(delegate.checkedGet(100, TIME_UNIT))
        .andThrow(new TimeoutException()).times(2);
    replay(delegate);
    try {
      forwarded.checkedGet(100, TIME_UNIT);
      fail();
    } catch (TimeoutException expected) {}
    try {
      simple.checkedGet(100, TIME_UNIT);
      fail();
    } catch (TimeoutException expected) {}
    verify(delegate);
  }

  public void testCheckedGetException() throws IOException {
    IOException expected = new IOException("expected");
    expect(delegate.checkedGet()).andThrow(expected).times(2);
    replay(delegate);
    try {
      delegate.checkedGet();
      fail();
    } catch (IOException e) {
      assertEquals(expected.getMessage(), e.getMessage());
    }
    try {
      simple.checkedGet();
      fail();
    } catch (IOException e) {
      assertEquals(expected.getMessage(), e.getMessage());
    }
    verify(delegate);
  }

  private class TestDelegateFuture 
      extends ForwardingCheckedFuture<String, IOException> {
    @Override
    protected CheckedFuture<String, IOException> delegate() {
      return delegate;
    }
  }

  private class TestSimpleFuture 
      extends SimpleForwardingCheckedFuture<String, IOException> {
    public TestSimpleFuture() {
      super(delegate);
    }
  }
  
}
