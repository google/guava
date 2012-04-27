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
import com.google.common.annotations.GwtIncompatible;

import junit.framework.TestCase;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Unit test for {@link FakeTicker}.
 *
 * @author benyu@google.com (Jige Yu)
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

  @GwtIncompatible("concurrency")

  public void testConcurrentAdvance() throws Exception {
    final FakeTicker ticker = new FakeTicker();

    int numberOfThreads = 64;
    ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
    final CountDownLatch startLatch = new CountDownLatch(numberOfThreads);
    final CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
    for (int i = numberOfThreads; i > 0; i--) {
      executorService.submit(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          // adds two nanoseconds to the ticker
          startLatch.countDown();
          startLatch.await();
          ticker.advance(1L);
          Thread.sleep(10);
          ticker.advance(1L);
          doneLatch.countDown();
          return null;
        }
      });
    }
    doneLatch.await();
    assertEquals(numberOfThreads * 2, ticker.read());
  }
}
