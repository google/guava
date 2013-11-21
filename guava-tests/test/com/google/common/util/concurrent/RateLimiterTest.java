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

import com.google.common.collect.Lists;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Tests for RateLimiter.
 *
 * @author Dimitris Andreou
 */
public class RateLimiterTest extends TestCase {
  private static final double EPSILON = 1e-8;

  /**
   * The ticker gathers events and presents them as strings.
   * R0.6 means a delay of 0.6 seconds caused by the (R)ateLimiter
   * U1.0 means the (U)ser caused the ticker to sleep for a second.
   */
  private final FakeTicker ticker = new FakeTicker();

  public void testSimple() {
    RateLimiter limiter = RateLimiter.create(ticker, 5.0);
    limiter.acquire(); // R0.00, since it's the first request
    limiter.acquire(); // R0.20
    limiter.acquire(); // R0.20
    assertEvents("R0.00", "R0.20", "R0.20");
  }

  public void testImmediateTryAcquire() {
    RateLimiter r = RateLimiter.create(1);
    assertTrue("Unable to acquire initial permit", r.tryAcquire());
    assertFalse("Capable of acquiring secondary permit", r.tryAcquire());
  }

  public void testSimpleRateUpdate() {
    RateLimiter limiter = RateLimiter.create(5.0, 5, TimeUnit.SECONDS);
    assertEquals(5.0, limiter.getRate());
    limiter.setRate(10.0);
    assertEquals(10.0, limiter.getRate());

    try {
      limiter.setRate(0.0);
      fail();
    } catch (IllegalArgumentException ok) {}
    try {
      limiter.setRate(-10.0);
      fail();
    } catch (IllegalArgumentException ok) {}
  }

  public void testSimpleWithWait() {
    RateLimiter limiter = RateLimiter.create(ticker, 5.0);
    limiter.acquire();          // R0.00
    ticker.sleepMillis(200);    // U0.20, we are ready for the next request...
    limiter.acquire();          // R0.00, ...which is granted immediately
    limiter.acquire();          // R0.20
    assertEvents("R0.00", "U0.20", "R0.00", "R0.20");
  }

  public void testSimpleAcquireReturnValues() {
    RateLimiter limiter = RateLimiter.create(ticker, 5.0);
    assertEquals(0.0, limiter.acquire(), EPSILON);  // R0.00
    ticker.sleepMillis(200);                        // U0.20, we are ready for the next request...
    assertEquals(0.0, limiter.acquire(), EPSILON);  // R0.00, ...which is granted immediately
    assertEquals(0.2, limiter.acquire(), EPSILON);  // R0.20
    assertEvents("R0.00", "U0.20", "R0.00", "R0.20");
  }

  public void testOneSecondBurst() {
    RateLimiter limiter = RateLimiter.create(ticker, 5.0);
    ticker.sleepMillis(1000); // max capacity reached
    ticker.sleepMillis(1000); // this makes no difference
    limiter.acquire(1); // R0.00, since it's the first request

    limiter.acquire(1); // R0.00, from capacity
    limiter.acquire(3); // R0.00, from capacity
    limiter.acquire(1); // R0.00, concluding a burst of 5 permits

    limiter.acquire(); // R0.20, capacity exhausted
    assertEvents("U1.00", "U1.00",
        "R0.00", "R0.00", "R0.00", "R0.00", // first request and burst
        "R0.20");
  }

  public void testWarmUp() {
    RateLimiter limiter = RateLimiter.create(ticker, 2.0, 4000, TimeUnit.MILLISECONDS);
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); // #1
    }
    ticker.sleepMillis(500); // #2: to repay for the last acquire
    ticker.sleepMillis(4000); // #3: becomes cold again
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); // // #4
    }
    ticker.sleepMillis(500); // #5: to repay for the last acquire
    ticker.sleepMillis(2000); // #6: didn't get cold! It would take another 2 seconds to go cold
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); // #7
    }
    assertEvents(
        "R0.00, R1.38, R1.13, R0.88, R0.63, R0.50, R0.50, R0.50", // #1
        "U0.50", // #2
        "U4.00", // #3
        "R0.00, R1.38, R1.13, R0.88, R0.63, R0.50, R0.50, R0.50", // #4
        "U0.50", // #5
        "U2.00", // #6
        "R0.00, R0.50, R0.50, R0.50, R0.50, R0.50, R0.50, R0.50"); // #7
  }

  public void testWarmUpAndUpdate() {
    RateLimiter limiter = RateLimiter.create(ticker, 2.0, 4000, TimeUnit.MILLISECONDS);
    for (int i = 0; i < 8; i++) {
      limiter.acquire(); // // #1
    }
    ticker.sleepMillis(4500); // #2: back to cold state (warmup period + repay last acquire)
    for (int i = 0; i < 3; i++) { // only three steps, we're somewhere in the warmup period
      limiter.acquire(); // #3
    }

    limiter.setRate(4.0); // double the rate!
    limiter.acquire(); // #4, we repay the debt of the last acquire (imposed by the old rate)
    for (int i = 0; i < 4; i++) {
      limiter.acquire(); // #5
    }
    ticker.sleepMillis(4250); // #6, back to cold state (warmup period + repay last acquire)
    for (int i = 0; i < 11; i++) {
      limiter.acquire(); // #7, showing off the warmup starting from totally cold
    }

    // make sure the areas (times) remain the same, while permits are different
    assertEvents(
        "R0.00, R1.38, R1.13, R0.88, R0.63, R0.50, R0.50, R0.50", // #1
        "U4.50", // #2
        "R0.00, R1.38, R1.13", // #3, after that the rate changes
        "R0.88", // #4, this is what the throttling would be with the old rate
        "R0.34, R0.28, R0.25, R0.25", // #5
        "U4.25", // #6
        "R0.00, R0.72, R0.66, R0.59, R0.53, R0.47, R0.41", // #7
        "R0.34, R0.28, R0.25, R0.25"); // #7 (cont.), note, this matches #5
  }

  public void testBursty() {
    RateLimiter limiter = RateLimiter.createWithCapacity(ticker, 1.0, 10, TimeUnit.SECONDS);
    ticker.sleepMillis(10000); // reach full capacity
    limiter.acquire(11); // all these are served in a burst (10 + 1 by borrowing from the future)
    limiter.acquire(1); // out of capacity, we have to wait
    limiter.acquire(1); // and wait
    ticker.sleepMillis(3000); // fill up 3 permits
    limiter.acquire(5); // we had 3 ready, thus we borrow 2 permits
    limiter.acquire(1); // this acquire() will also repay for the previous acquire()
    assertEvents(
        "U10.00",
        "R0.00", // 10 permits grabbed
        "R1.00", "R1.00", // 1 and 1
        "U3.00", "R0.00", // 5 grabbed
        "R3.00" // 1 grabbed
        );
  }

  public void testBurstyAndUpdate() {
    RateLimiter rateLimiter = RateLimiter.create(ticker, 1.0);
    rateLimiter.acquire(1); // no wait
    rateLimiter.acquire(1); // R1.00, to repay previous

    rateLimiter.setRate(2.0); // update the rate!

    rateLimiter.acquire(1); // R1.00, to repay previous (the previous was under the old rate!)
    rateLimiter.acquire(2); // R0.50, to repay previous (now the rate takes effect)
    rateLimiter.acquire(4); // R1.00, to repay previous
    rateLimiter.acquire(1); // R2.00, to repay previous
    assertEvents("R0.00", "R1.00", "R1.00", "R0.50", "R1.00", "R2.00");
  }

  public void testTimeWrapping() {
    ticker.instant = Long.MAX_VALUE - TimeUnit.SECONDS.toNanos(1); // 1 second before max value
    RateLimiter limiter = RateLimiter.create(ticker, 1.0);
    for (int i = 0; i < 4; i++) {
      limiter.acquire();
    }
    // Without protection from overflow, the last wait value would have been huge,
    // because "now" would have wrapped into a value near MIN_VALUE, and the limiter would think
    // that the next request should be admitted far into the future
    assertEvents("R0.00", "R1.00", "R1.00", "R1.00");
  }

  public void testTryGate() {
    RateLimiter limiter = RateLimiter.create(ticker, 5.0);
    assertTrue(limiter.tryAcquire(0, TimeUnit.SECONDS));
    assertFalse(limiter.tryAcquire(0, TimeUnit.SECONDS));
    assertFalse(limiter.tryAcquire(0, TimeUnit.SECONDS));
    ticker.sleepMillis(100);
    assertFalse(limiter.tryAcquire(0, TimeUnit.SECONDS));
  }

  public void testSimpleWeights() {
    RateLimiter rateLimiter = RateLimiter.create(ticker, 1.0);
    rateLimiter.acquire(1); // no wait
    rateLimiter.acquire(1); // R1.00, to repay previous
    rateLimiter.acquire(2); // R1.00, to repay previous
    rateLimiter.acquire(4); // R2.00, to repay previous
    rateLimiter.acquire(8); // R4.00, to repay previous
    rateLimiter.acquire(1); // R8.00, to repay previous
    assertEvents("R0.00", "R1.00", "R1.00", "R2.00", "R4.00", "R8.00");
  }

  public void testInfinity_Bursty() {
    RateLimiter limiter = RateLimiter.create(ticker, Double.POSITIVE_INFINITY);
    limiter.acquire(Integer.MAX_VALUE / 4);
    limiter.acquire(Integer.MAX_VALUE / 2);
    limiter.acquire(Integer.MAX_VALUE);
    assertEvents("R0.00", "R0.00", "R0.00"); // no wait, infinite rate!

    limiter.setRate(1.0);
    limiter.acquire();
    limiter.acquire();
    limiter.acquire();
    assertEvents("R0.00", "R1.00", "R1.00"); // we repay the last request (but that had no cost)
    // and then we go to 1 second per request mode

    limiter.setRate(Double.POSITIVE_INFINITY);
    limiter.acquire();
    limiter.acquire();
    limiter.acquire();
    assertEvents("R1.00", "R0.00", "R0.00"); // we repay the last request (1sec), then back to +oo
  }

  public void testInfinity_WarmUp() {
    RateLimiter limiter = RateLimiter.create(
        ticker, Double.POSITIVE_INFINITY, 10, TimeUnit.SECONDS);
    limiter.acquire(Integer.MAX_VALUE / 4);
    limiter.acquire(Integer.MAX_VALUE / 2);
    limiter.acquire(Integer.MAX_VALUE);
    assertEvents("R0.00", "R0.00", "R0.00");

    limiter.setRate(1.0);
    limiter.acquire();
    limiter.acquire();
    limiter.acquire();
    assertEvents("R0.00", "R1.00", "R1.00");

    limiter.setRate(Double.POSITIVE_INFINITY);
    limiter.acquire();
    limiter.acquire();
    limiter.acquire();
    assertEvents("R1.00", "R0.00", "R0.00");
  }

  /**
   * Make sure that bursts can never go above 1-second-worth-of-work for the current
   * rate, even when we change the rate.
   */
  public void testWeNeverGetABurstMoreThanOneSec() {
    RateLimiter limiter = RateLimiter.create(ticker, 1.0);
    int[] rates = { 1000, 1, 10, 1000000, 10, 1};
    for (int rate : rates) {
      int oneSecWorthOfWork = rate;
      ticker.sleepMillis(rate * 1000);
      limiter.setRate(rate);
      long burst = measureTotalTimeMillis(limiter, oneSecWorthOfWork, new Random());
      // we allow one second worth of work to go in a burst (i.e. take less than a second)
      assertTrue(burst <= 1000);
      long afterBurst = measureTotalTimeMillis(limiter, oneSecWorthOfWork, new Random());
      // but work beyond that must take at least one second
      assertTrue(afterBurst >= 1000);
    }
  }

  /**
   * This neat test shows that no matter what weights we use in our requests, if we push X
   * amount of permits in a cool state, where X = rate * timeToCoolDown, and we have
   * specified a timeToWarmUp() period, it will cost as the prescribed amount of time. E.g.,
   * calling [acquire(5), acquire(1)] takes exactly the same time as
   * [acquire(2), acquire(3), acquire(1)].
   */
  public void testTimeToWarmUpIsHonouredEvenWithWeights() {
    Random random = new Random();
    int maxPermits = 10;
    double[] qpsToTest = { 4.0, 2.0, 1.0, 0.5, 0.1 };
    for (int trial = 0; trial < 100; trial++) {
      for (double qps : qpsToTest) {
        // Since we know that: maxPermits = 0.5 * warmup / stableInterval;
        // then if maxPermits == 10, we have:
        // warmupSeconds = 20 / qps
        long warmupMillis = (long) ((2 * maxPermits / qps) * 1000.0);
        RateLimiter rateLimiter = RateLimiter.create(
            ticker, qps, warmupMillis, TimeUnit.MILLISECONDS);
        assertEquals(warmupMillis, measureTotalTimeMillis(rateLimiter, maxPermits, random));
      }
    }
  }

  public void testNulls() {
    NullPointerTester tester = new NullPointerTester()
        .setDefault(RateLimiter.SleepingTicker.class, ticker);
    tester.testStaticMethods(RateLimiter.class, Visibility.PACKAGE);
    tester.testInstanceMethods(RateLimiter.create(ticker, 5.0), Visibility.PACKAGE);
  }

  private long measureTotalTimeMillis(RateLimiter rateLimiter, int permits, Random random) {
    long startTime = ticker.instant;
    while (permits > 0) {
      int nextPermitsToAcquire = Math.max(1, random.nextInt(permits));
      permits -= nextPermitsToAcquire;
      rateLimiter.acquire(nextPermitsToAcquire);
    }
    rateLimiter.acquire(1); // to repay for any pending debt
    return TimeUnit.NANOSECONDS.toMillis(ticker.instant - startTime);
  }

  private void assertEvents(String... events) {
    assertEquals(Arrays.toString(events), ticker.readEventsAndClear());
  }

  private static class FakeTicker extends RateLimiter.SleepingTicker {
    long instant = 0L;
    final List<String> events = Lists.newArrayList();

    @Override
    public long read() {
      return instant;
    }

    void sleepMillis(int millis) {
      sleepMicros("U", TimeUnit.MILLISECONDS.toMicros(millis));
    }

    void sleepMicros(String caption, long micros) {
      instant += TimeUnit.MICROSECONDS.toNanos(micros);
      events.add(caption + String.format("%3.2f", (micros / 1000000.0)));
    }

    @Override
    void sleepMicrosUninterruptibly(long micros) {
      sleepMicros("R", micros);
    }

    String readEventsAndClear() {
      try {
        return events.toString();
      } finally {
        events.clear();
      }
    }

    @Override
    public String toString() {
      return events.toString();
    }
  }
}
