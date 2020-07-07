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

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Range;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;

/**
 * Unit test for {@link SimpleTimeLimiter}.
 *
 * @author kevinb
 * @author Jens Nyman
 */

public class SimpleTimeLimiterTest extends TestCase {

  private static final long DELAY_MS = 50;
  private static final long ENOUGH_MS = 10000;
  private static final long NOT_ENOUGH_MS = 5;

  private static final String GOOD_CALLABLE_RESULT = "good callable result";
  private static final Callable<String> GOOD_CALLABLE =
      new Callable<String>() {
        @Override
        public String call() throws InterruptedException {
          MILLISECONDS.sleep(DELAY_MS);
          return GOOD_CALLABLE_RESULT;
        }
      };
  private static final Callable<String> BAD_CALLABLE =
      new Callable<String>() {
        @Override
        public String call() throws InterruptedException, SampleException {
          MILLISECONDS.sleep(DELAY_MS);
          throw new SampleException();
        }
      };
  private static final Runnable GOOD_RUNNABLE =
      new Runnable() {
        @Override
        public void run() {
          try {
            MILLISECONDS.sleep(DELAY_MS);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      };
  private static final Runnable BAD_RUNNABLE =
      new Runnable() {
        @Override
        public void run() {
          try {
            MILLISECONDS.sleep(DELAY_MS);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          throw new SampleRuntimeException();
        }
      };

  private TimeLimiter service;

  private static final ExecutorService executor = Executors.newFixedThreadPool(1);

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    service = SimpleTimeLimiter.create(executor);
  }

  public void testNewProxy_goodMethodWithEnoughTime() throws Exception {
    SampleImpl target = new SampleImpl(DELAY_MS);
    Sample proxy = service.newProxy(target, Sample.class, ENOUGH_MS, MILLISECONDS);
    Stopwatch stopwatch = Stopwatch.createStarted();

    String result = proxy.sleepThenReturnInput("x");

    assertThat(result).isEqualTo("x");
    assertThat(stopwatch.elapsed(MILLISECONDS)).isIn(Range.closed(DELAY_MS, ENOUGH_MS));
    assertThat(target.finished).isTrue();
  }

  public void testNewProxy_goodMethodWithNotEnoughTime() throws Exception {
    SampleImpl target = new SampleImpl(9999);
    Sample proxy = service.newProxy(target, Sample.class, NOT_ENOUGH_MS, MILLISECONDS);
    Stopwatch stopwatch = Stopwatch.createStarted();

    try {
      proxy.sleepThenReturnInput("x");
      fail("no exception thrown");
    } catch (UncheckedTimeoutException expected) {
    }

    assertThat(stopwatch.elapsed(MILLISECONDS)).isIn(Range.closed(NOT_ENOUGH_MS, DELAY_MS * 2));
    // Is it still computing away anyway?
    assertThat(target.finished).isFalse();
    MILLISECONDS.sleep(ENOUGH_MS);
    assertThat(target.finished).isFalse();
  }

  public void testNewProxy_badMethodWithEnoughTime() throws Exception {
    SampleImpl target = new SampleImpl(DELAY_MS);
    Sample proxy = service.newProxy(target, Sample.class, ENOUGH_MS, MILLISECONDS);
    Stopwatch stopwatch = Stopwatch.createStarted();

    try {
      proxy.sleepThenThrowException();
      fail("no exception thrown");
    } catch (SampleException expected) {
    }

    assertThat(stopwatch.elapsed(MILLISECONDS)).isIn(Range.closed(DELAY_MS, ENOUGH_MS));
  }

  public void testNewProxy_badMethodWithNotEnoughTime() throws Exception {
    SampleImpl target = new SampleImpl(9999);
    Sample proxy = service.newProxy(target, Sample.class, NOT_ENOUGH_MS, MILLISECONDS);
    Stopwatch stopwatch = Stopwatch.createStarted();

    try {
      proxy.sleepThenThrowException();
      fail("no exception thrown");
    } catch (UncheckedTimeoutException expected) {
    }

    assertThat(stopwatch.elapsed(MILLISECONDS)).isIn(Range.closed(NOT_ENOUGH_MS, DELAY_MS * 2));
  }

  public void testCallWithTimeout_goodCallableWithEnoughTime() throws Exception {
    Stopwatch stopwatch = Stopwatch.createStarted();

    String result = service.callWithTimeout(GOOD_CALLABLE, ENOUGH_MS, MILLISECONDS);

    assertThat(result).isEqualTo(GOOD_CALLABLE_RESULT);
    assertThat(stopwatch.elapsed(MILLISECONDS)).isIn(Range.closed(DELAY_MS, ENOUGH_MS));
  }

  public void testCallWithTimeout_goodCallableWithNotEnoughTime() throws Exception {
    try {
      service.callWithTimeout(GOOD_CALLABLE, NOT_ENOUGH_MS, MILLISECONDS);
      fail("no exception thrown");
    } catch (TimeoutException expected) {
    }
  }

  public void testCallWithTimeout_badCallableWithEnoughTime() throws Exception {
    try {
      service.callWithTimeout(BAD_CALLABLE, ENOUGH_MS, MILLISECONDS);
      fail("no exception thrown");
    } catch (ExecutionException expected) {
      assertThat(expected.getCause()).isInstanceOf(SampleException.class);
    }
  }

  public void testCallUninterruptiblyWithTimeout_goodCallableWithEnoughTime() throws Exception {
    Stopwatch stopwatch = Stopwatch.createStarted();

    String result = service.callUninterruptiblyWithTimeout(GOOD_CALLABLE, ENOUGH_MS, MILLISECONDS);

    assertThat(result).isEqualTo(GOOD_CALLABLE_RESULT);
    assertThat(stopwatch.elapsed(MILLISECONDS)).isIn(Range.closed(DELAY_MS, ENOUGH_MS));
  }

  public void testCallUninterruptiblyWithTimeout_goodCallableWithNotEnoughTime() throws Exception {
    try {
      service.callUninterruptiblyWithTimeout(GOOD_CALLABLE, NOT_ENOUGH_MS, MILLISECONDS);
      fail("no exception thrown");
    } catch (TimeoutException expected) {
    }
  }

  public void testCallUninterruptiblyWithTimeout_badCallableWithEnoughTime() throws Exception {
    try {
      service.callUninterruptiblyWithTimeout(BAD_CALLABLE, ENOUGH_MS, MILLISECONDS);
      fail("no exception thrown");
    } catch (ExecutionException expected) {
      assertThat(expected.getCause()).isInstanceOf(SampleException.class);
    }
  }

  public void testRunWithTimeout_goodRunnableWithEnoughTime() throws Exception {
    Stopwatch stopwatch = Stopwatch.createStarted();

    service.runWithTimeout(GOOD_RUNNABLE, ENOUGH_MS, MILLISECONDS);

    assertThat(stopwatch.elapsed(MILLISECONDS)).isIn(Range.closed(DELAY_MS, ENOUGH_MS));
  }

  public void testRunWithTimeout_goodRunnableWithNotEnoughTime() throws Exception {
    try {
      service.runWithTimeout(GOOD_RUNNABLE, NOT_ENOUGH_MS, MILLISECONDS);
      fail("no exception thrown");
    } catch (TimeoutException expected) {
    }
  }

  public void testRunWithTimeout_badRunnableWithEnoughTime() throws Exception {
    try {
      service.runWithTimeout(BAD_RUNNABLE, ENOUGH_MS, MILLISECONDS);
      fail("no exception thrown");
    } catch (UncheckedExecutionException expected) {
      assertThat(expected.getCause()).isInstanceOf(SampleRuntimeException.class);
    }
  }

  public void testRunUninterruptiblyWithTimeout_goodRunnableWithEnoughTime() throws Exception {
    Stopwatch stopwatch = Stopwatch.createStarted();

    service.runUninterruptiblyWithTimeout(GOOD_RUNNABLE, ENOUGH_MS, MILLISECONDS);

    assertThat(stopwatch.elapsed(MILLISECONDS)).isIn(Range.closed(DELAY_MS, ENOUGH_MS));
  }

  public void testRunUninterruptiblyWithTimeout_goodRunnableWithNotEnoughTime() throws Exception {
    try {
      service.runUninterruptiblyWithTimeout(GOOD_RUNNABLE, NOT_ENOUGH_MS, MILLISECONDS);
      fail("no exception thrown");
    } catch (TimeoutException expected) {
    }
  }

  public void testRunUninterruptiblyWithTimeout_badRunnableWithEnoughTime() throws Exception {
    try {
      service.runUninterruptiblyWithTimeout(BAD_RUNNABLE, ENOUGH_MS, MILLISECONDS);
      fail("no exception thrown");
    } catch (UncheckedExecutionException expected) {
      assertThat(expected.getCause()).isInstanceOf(SampleRuntimeException.class);
    }
  }

  private interface Sample {
    String sleepThenReturnInput(String input);

    void sleepThenThrowException() throws SampleException;
  }

  @SuppressWarnings("serial")
  private static class SampleException extends Exception {}

  @SuppressWarnings("serial")
  private static class SampleRuntimeException extends RuntimeException {}

  private static class SampleImpl implements Sample {
    final long delayMillis;
    boolean finished;

    SampleImpl(long delayMillis) {
      this.delayMillis = delayMillis;
    }

    @Override
    public String sleepThenReturnInput(String input) {
      try {
        MILLISECONDS.sleep(delayMillis);
        finished = true;
        return input;
      } catch (InterruptedException e) {
        return null;
      }
    }

    @Override
    public void sleepThenThrowException() throws SampleException {
      try {
        MILLISECONDS.sleep(delayMillis);
      } catch (InterruptedException e) {
      }
      throw new SampleException();
    }
  }
}
