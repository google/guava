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

package com.google.common.testing;

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

/**
 * Unit test for {@link FakeTicker}.
 *
 * @author Jige Yu
 */
@GwtCompatible(emulated = true)
public class FakeTickerTest extends TestCase {

  public void testAdvance() {
    FakeTicker ticker = new FakeTicker();
    assertEquals(0, ticker.read());
    assertSame(ticker, ticker.advance(10));
    assertEquals(10, ticker.read());
    ticker.advance(1, TimeUnit.MILLISECONDS);
    assertEquals(1000010L, ticker.read());
  }

  public void testAutoIncrementStep_returnsSameInstance() {
    FakeTicker ticker = new FakeTicker();
    assertSame(ticker, ticker.setAutoIncrementStep(10, TimeUnit.NANOSECONDS));
  }

  public void testAutoIncrementStep_nanos() {
    FakeTicker ticker = new FakeTicker().setAutoIncrementStep(10, TimeUnit.NANOSECONDS);
    assertEquals(0, ticker.read());
    assertEquals(10, ticker.read());
    assertEquals(20, ticker.read());
  }

  public void testAutoIncrementStep_millis() {
    FakeTicker ticker = new FakeTicker().setAutoIncrementStep(1, TimeUnit.MILLISECONDS);
    assertEquals(0, ticker.read());
    assertEquals(1000000, ticker.read());
    assertEquals(2000000, ticker.read());
  }

  public void testAutoIncrementStep_seconds() {
    FakeTicker ticker = new FakeTicker().setAutoIncrementStep(3, TimeUnit.SECONDS);
    assertEquals(0, ticker.read());
    assertEquals(3000000000L, ticker.read());
    assertEquals(6000000000L, ticker.read());
  }

  public void testAutoIncrementStep_resetToZero() {
    FakeTicker ticker = new FakeTicker().setAutoIncrementStep(10, TimeUnit.NANOSECONDS);
    assertEquals(0, ticker.read());
    assertEquals(10, ticker.read());
    assertEquals(20, ticker.read());

    for (TimeUnit timeUnit : EnumSet.allOf(TimeUnit.class)) {
      ticker.setAutoIncrementStep(0, timeUnit);
      assertEquals(
          "Expected no auto-increment when setting autoIncrementStep to 0 " + timeUnit,
          30, ticker.read());
    }
  }

  public void testAutoIncrement_negative() {
    FakeTicker ticker = new FakeTicker();
    try {
      ticker.setAutoIncrementStep(-1, TimeUnit.NANOSECONDS);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }
}

