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

package com.google.common.base;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.FakeTicker;
import junit.framework.TestCase;

/**
 * Unit test for {@link Stopwatch}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
public class StopwatchTest extends TestCase {

  private final FakeTicker ticker = new FakeTicker();
  private final Stopwatch stopwatch = new Stopwatch(ticker);

  public void testCreateStarted() {
    Stopwatch startedStopwatch = Stopwatch.createStarted();
    assertTrue(startedStopwatch.isRunning());
  }

  public void testCreateUnstarted() {
    Stopwatch unstartedStopwatch = Stopwatch.createUnstarted();
    assertFalse(unstartedStopwatch.isRunning());
    assertEquals(0, unstartedStopwatch.elapsed(NANOSECONDS));
  }

  public void testInitialState() {
    assertFalse(stopwatch.isRunning());
    assertEquals(0, stopwatch.elapsed(NANOSECONDS));
  }

  public void testStart() {
    assertSame(stopwatch, stopwatch.start());
    assertTrue(stopwatch.isRunning());
  }

  public void testStart_whileRunning() {
    stopwatch.start();
    try {
      stopwatch.start();
      fail();
    } catch (IllegalStateException expected) {
    }
    assertTrue(stopwatch.isRunning());
  }

  public void testStop() {
    stopwatch.start();
    assertSame(stopwatch, stopwatch.stop());
    assertFalse(stopwatch.isRunning());
  }

  public void testStop_new() {
    try {
      stopwatch.stop();
      fail();
    } catch (IllegalStateException expected) {
    }
    assertFalse(stopwatch.isRunning());
  }

  public void testStop_alreadyStopped() {
    stopwatch.start();
    stopwatch.stop();
    try {
      stopwatch.stop();
      fail();
    } catch (IllegalStateException expected) {
    }
    assertFalse(stopwatch.isRunning());
  }

  public void testReset_new() {
    ticker.advance(1);
    stopwatch.reset();
    assertFalse(stopwatch.isRunning());
    ticker.advance(2);
    assertEquals(0, stopwatch.elapsed(NANOSECONDS));
    stopwatch.start();
    ticker.advance(3);
    assertEquals(3, stopwatch.elapsed(NANOSECONDS));
  }

  public void testReset_whileRunning() {
    ticker.advance(1);
    stopwatch.start();
    assertEquals(0, stopwatch.elapsed(NANOSECONDS));
    ticker.advance(2);
    assertEquals(2, stopwatch.elapsed(NANOSECONDS));
    stopwatch.reset();
    assertFalse(stopwatch.isRunning());
    ticker.advance(3);
    assertEquals(0, stopwatch.elapsed(NANOSECONDS));
  }

  public void testElapsed_whileRunning() {
    ticker.advance(78);
    stopwatch.start();
    assertEquals(0, stopwatch.elapsed(NANOSECONDS));

    ticker.advance(345);
    assertEquals(345, stopwatch.elapsed(NANOSECONDS));
  }

  public void testElapsed_notRunning() {
    ticker.advance(1);
    stopwatch.start();
    ticker.advance(4);
    stopwatch.stop();
    ticker.advance(9);
    assertEquals(4, stopwatch.elapsed(NANOSECONDS));
  }

  public void testElapsed_multipleSegments() {
    stopwatch.start();
    ticker.advance(9);
    stopwatch.stop();

    ticker.advance(16);

    stopwatch.start();
    assertEquals(9, stopwatch.elapsed(NANOSECONDS));
    ticker.advance(25);
    assertEquals(34, stopwatch.elapsed(NANOSECONDS));

    stopwatch.stop();
    ticker.advance(36);
    assertEquals(34, stopwatch.elapsed(NANOSECONDS));
  }

  public void testElapsed_micros() {
    stopwatch.start();
    ticker.advance(999);
    assertEquals(0, stopwatch.elapsed(MICROSECONDS));
    ticker.advance(1);
    assertEquals(1, stopwatch.elapsed(MICROSECONDS));
  }

  public void testElapsed_millis() {
    stopwatch.start();
    ticker.advance(999999);
    assertEquals(0, stopwatch.elapsed(MILLISECONDS));
    ticker.advance(1);
    assertEquals(1, stopwatch.elapsed(MILLISECONDS));
  }

  @J2ktIncompatible // TODO(b/259213718): Enable
  public void testToString() {
    stopwatch.start();
    assertEquals("0.000 ns", stopwatch.toString());
    ticker.advance(1);
    assertEquals("1.000 ns", stopwatch.toString());
    ticker.advance(998);
    assertEquals("999.0 ns", stopwatch.toString());
    ticker.advance(1);
    assertEquals("1.000 \u03bcs", stopwatch.toString());
    ticker.advance(1);
    assertEquals("1.001 \u03bcs", stopwatch.toString());
    ticker.advance(8998);
    assertEquals("9.999 \u03bcs", stopwatch.toString());
    stopwatch.reset();
    stopwatch.start();
    ticker.advance(1234567);
    assertEquals("1.235 ms", stopwatch.toString());
    stopwatch.reset();
    stopwatch.start();
    ticker.advance(5000000000L);
    assertEquals("5.000 s", stopwatch.toString());
    stopwatch.reset();
    stopwatch.start();
    ticker.advance((long) (1.5 * 60 * 1000000000L));
    assertEquals("1.500 min", stopwatch.toString());
    stopwatch.reset();
    stopwatch.start();
    ticker.advance((long) (2.5 * 60 * 60 * 1000000000L));
    assertEquals("2.500 h", stopwatch.toString());
    stopwatch.reset();
    stopwatch.start();
    ticker.advance((long) (7.25 * 24 * 60 * 60 * 1000000000L));
    assertEquals("7.250 d", stopwatch.toString());
  }
}
