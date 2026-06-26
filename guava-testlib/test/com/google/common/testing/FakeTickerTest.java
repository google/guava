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

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

/**
 * Unit test for {@link FakeTicker}.
 *
 * @author Jige Yu
 */
@GwtCompatible
// We also want to test the TimeUnit overload (especially under GWT, where it's the only option).
@SuppressWarnings("SetAutoIncrementStep_Nanos")
@NullUnmarked
public class FakeTickerTest {

  @Test
  @GwtIncompatible // NullPointerTester
  public void nullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(new FakeTicker());
  }

  @Test
  @GwtIncompatible // java.time.Duration
  public void advance() {
    FakeTicker ticker = new FakeTicker();
    assertEquals(0, ticker.read());
    assertThat(ticker.advance(10)).isSameInstanceAs(ticker);
    assertEquals(10, ticker.read());
    ticker.advance(1, MILLISECONDS);
    assertEquals(1000010L, ticker.read());
    ticker.advance(Duration.ofMillis(1));
    assertEquals(2000010L, ticker.read());
  }

  @Test
  public void autoIncrementStep_returnsSameInstance() {
    FakeTicker ticker = new FakeTicker();
    assertThat(ticker.setAutoIncrementStep(10, NANOSECONDS)).isSameInstanceAs(ticker);
  }

  @Test
  public void autoIncrementStep_nanos() {
    FakeTicker ticker = new FakeTicker().setAutoIncrementStep(10, NANOSECONDS);
    assertEquals(0, ticker.read());
    assertEquals(10, ticker.read());
    assertEquals(20, ticker.read());
  }

  @Test
  public void autoIncrementStep_millis() {
    FakeTicker ticker = new FakeTicker().setAutoIncrementStep(1, MILLISECONDS);
    assertEquals(0, ticker.read());
    assertEquals(1000000, ticker.read());
    assertEquals(2000000, ticker.read());
  }

  @Test
  public void autoIncrementStep_seconds() {
    FakeTicker ticker = new FakeTicker().setAutoIncrementStep(3, SECONDS);
    assertEquals(0, ticker.read());
    assertEquals(3000000000L, ticker.read());
    assertEquals(6000000000L, ticker.read());
  }

  @Test
  @GwtIncompatible // java.time.Duration
  public void autoIncrementStep_duration() {
    FakeTicker ticker = new FakeTicker().setAutoIncrementStep(Duration.ofMillis(1));
    assertEquals(0, ticker.read());
    assertEquals(1000000, ticker.read());
    assertEquals(2000000, ticker.read());
  }

  @Test
  public void autoIncrementStep_resetToZero() {
    FakeTicker ticker = new FakeTicker().setAutoIncrementStep(10, NANOSECONDS);
    assertEquals(0, ticker.read());
    assertEquals(10, ticker.read());
    assertEquals(20, ticker.read());

    for (TimeUnit timeUnit : TimeUnit.values()) {
      ticker.setAutoIncrementStep(0, timeUnit);
      assertEquals(
          "Expected no auto-increment when setting autoIncrementStep to 0 " + timeUnit,
          30,
          ticker.read());
    }
  }

  @Test
  public void autoIncrement_negative() {
    FakeTicker ticker = new FakeTicker();
    assertThrows(
        IllegalArgumentException.class, () -> ticker.setAutoIncrementStep(-1, NANOSECONDS));
  }

  @Test
  @GwtIncompatible // concurrency
  public void concurrentAdvance() throws Exception {
    FakeTicker ticker = new FakeTicker();

    int numberOfThreads = 64;
    runConcurrentTest(
        numberOfThreads,
        () -> {
          // adds two nanoseconds to the ticker
          ticker.advance(1L);
          Thread.sleep(10);
          ticker.advance(1L);
          return null;
        });

    assertEquals(numberOfThreads * 2, ticker.read());
  }

  @Test
  @GwtIncompatible // concurrency
  public void concurrentAutoIncrementStep() throws Exception {
    int incrementByNanos = 3;
    FakeTicker ticker = new FakeTicker().setAutoIncrementStep(incrementByNanos, NANOSECONDS);

    int numberOfThreads = 64;
    runConcurrentTest(
        numberOfThreads,
        () -> {
          long unused = ticker.read();
          return null;
        });

    assertEquals(incrementByNanos * numberOfThreads, ticker.read());
  }

  /** Runs {@code callable} concurrently {@code numberOfThreads} times. */
  @GwtIncompatible // concurrency
  private void runConcurrentTest(int numberOfThreads, Callable<@Nullable Void> callable)
      throws Exception {
    ExecutorService executorService = newFixedThreadPool(numberOfThreads);
    try {
      CountDownLatch startLatch = new CountDownLatch(numberOfThreads);
      CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
      for (int i = numberOfThreads; i > 0; i--) {
        @SuppressWarnings("unused") // https://errorprone.info/bugpattern/FutureReturnValueIgnored
        Future<?> possiblyIgnoredError =
            executorService.submit(
                () -> {
                  startLatch.countDown();
                  startLatch.await();
                  callable.call();
                  doneLatch.countDown();
                  return null;
                });
      }
      doneLatch.await();
    } finally {
      executorService.shutdown();
    }
  }
}
