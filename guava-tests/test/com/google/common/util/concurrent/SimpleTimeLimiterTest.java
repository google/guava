/*
 * Copyright (C) 2006 The Guava Authors
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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Unit test for {@link SimpleTimeLimiter}.
 *
 * @author kevinb
 */
public class SimpleTimeLimiterTest extends TestCase {

  private static final int DELAY_MS = 50;
  private static final int ENOUGH_MS = 500;
  private static final int NOT_ENOUGH_MS = 5;

  private TimeLimiter service;

  private static final ExecutorService executor
      = Executors.newFixedThreadPool(1);

  private static String someGoodStaticMethod() throws InterruptedException {
    TimeUnit.MILLISECONDS.sleep(DELAY_MS);
    return "yes";
  }

  private static String someBadStaticMethod() throws InterruptedException,
      SampleException {
    TimeUnit.MILLISECONDS.sleep(DELAY_MS);
    throw new SampleException();
  }

  @Override protected void setUp() throws Exception {
    super.setUp();
    service = new SimpleTimeLimiter(executor);
  }

  public void testGoodCallableWithEnoughTime() throws Exception {
    long start = System.nanoTime();
    String result = service.callWithTimeout(
        new Callable<String>() {
          @Override
          public String call() throws InterruptedException {
            return someGoodStaticMethod();
          }
        }, ENOUGH_MS, TimeUnit.MILLISECONDS, true);
    assertEquals("yes", result);
    assertTheCallTookBetween(start, DELAY_MS, ENOUGH_MS);
  }

  public void testGoodCallableWithNotEnoughTime() throws Exception {
    long start = System.nanoTime();
    try {
      service.callWithTimeout(
          new Callable<String>() {
            @Override
            public String call() throws InterruptedException {
              return someGoodStaticMethod();
            }
          }, NOT_ENOUGH_MS, TimeUnit.MILLISECONDS, true);
      fail("no exception thrown");
    } catch (UncheckedTimeoutException expected) {
    }
    assertTheCallTookBetween(start, NOT_ENOUGH_MS, DELAY_MS);
  }

  public void testBadCallableWithEnoughTime() throws Exception {
    long start = System.nanoTime();
    try {
      service.callWithTimeout(
          new Callable<String>() {
            @Override
            public String call() throws SampleException, InterruptedException {
              return someBadStaticMethod();
            }
          }, ENOUGH_MS, TimeUnit.MILLISECONDS, true);
      fail("no exception thrown");
    } catch (SampleException expected) {
    }
    assertTheCallTookBetween(start, DELAY_MS, ENOUGH_MS);
  }

  public void testBadCallableWithNotEnoughTime() throws Exception {
    long start = System.nanoTime();
    try {
      service.callWithTimeout(
          new Callable<String>() {
            @Override
            public String call() throws SampleException, InterruptedException {
              return someBadStaticMethod();
            }
          }, NOT_ENOUGH_MS, TimeUnit.MILLISECONDS, true);
      fail("no exception thrown");
    } catch (UncheckedTimeoutException expected) {
    }
    assertTheCallTookBetween(start, NOT_ENOUGH_MS, DELAY_MS);
  }

  public void testGoodMethodWithEnoughTime() throws Exception {
    SampleImpl target = new SampleImpl();
    Sample proxy = service.newProxy(
        target, Sample.class, ENOUGH_MS, TimeUnit.MILLISECONDS);
    long start = System.nanoTime();
    assertEquals("x", proxy.sleepThenReturnInput("x"));
    assertTheCallTookBetween(start, DELAY_MS, ENOUGH_MS);
    assertTrue(target.finished);
  }

  public void testGoodMethodWithNotEnoughTime() throws Exception {
    SampleImpl target = new SampleImpl();
    Sample proxy = service.newProxy(
        target, Sample.class, NOT_ENOUGH_MS, TimeUnit.MILLISECONDS);
    long start = System.nanoTime();
    try {
      proxy.sleepThenReturnInput("x");
      fail("no exception thrown");
    } catch (UncheckedTimeoutException expected) {
    }
    assertTheCallTookBetween(start, NOT_ENOUGH_MS, DELAY_MS);

    // Is it still computing away anyway?
    assertFalse(target.finished);
    TimeUnit.MILLISECONDS.sleep(ENOUGH_MS);
    assertFalse(target.finished);
  }

  public void testBadMethodWithEnoughTime() throws Exception {
    SampleImpl target = new SampleImpl();
    Sample proxy = service.newProxy(
        target, Sample.class, ENOUGH_MS, TimeUnit.MILLISECONDS);
    long start = System.nanoTime();
    try {
      proxy.sleepThenThrowException();
      fail("no exception thrown");
    } catch (SampleException expected) {
    }
    assertTheCallTookBetween(start, DELAY_MS, ENOUGH_MS);
  }

  public void testBadMethodWithNotEnoughTime() throws Exception {
    SampleImpl target = new SampleImpl();
    Sample proxy = service.newProxy(
        target, Sample.class, NOT_ENOUGH_MS, TimeUnit.MILLISECONDS);
    long start = System.nanoTime();
    try {
      proxy.sleepThenThrowException();
      fail("no exception thrown");
    } catch (UncheckedTimeoutException expected) {
    }
    assertTheCallTookBetween(start, NOT_ENOUGH_MS, DELAY_MS);
  }

  private static void assertTheCallTookBetween(
      long startNanos, int atLeastMillis, int atMostMillis) {
    long nanos = System.nanoTime() - startNanos;
    assertTrue(nanos >= atLeastMillis * 1000000);
    assertTrue(nanos <= atMostMillis * 1000000);
  }

  public interface Sample {
    String sleepThenReturnInput(String input);
    void sleepThenThrowException() throws SampleException;
  }

  @SuppressWarnings("serial")
  public static class SampleException extends Exception {}

  public static class SampleImpl implements Sample {
    boolean finished;

    @Override
    public String sleepThenReturnInput(String input) {
      try {
        TimeUnit.MILLISECONDS.sleep(DELAY_MS);
        finished = true;
        return input;
      } catch (InterruptedException e) {
        return null;
      }
    }
    @Override
    public void sleepThenThrowException() throws SampleException {
      try {
        TimeUnit.MILLISECONDS.sleep(DELAY_MS);
      } catch (InterruptedException e) {
      }
      throw new SampleException();
    }
  }
}
