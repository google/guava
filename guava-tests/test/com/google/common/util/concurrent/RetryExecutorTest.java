/*
 * Copyright (C) 2024 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.util.concurrent;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Predicate;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;

/** Tests for {@link RetryExecutor}. */
public class RetryExecutorTest extends TestCase {

  private ScheduledExecutorService scheduler;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    scheduler = Executors.newScheduledThreadPool(2);
  }

  @Override
  protected void tearDown() throws Exception {
    scheduler.shutdownNow();
    scheduler.awaitTermination(5, SECONDS);
    super.tearDown();
  }

  public void testSuccessfulExecution() throws Exception {
    RetryExecutor executor =
        RetryExecutor.builder()
            .setMaxRetries(3)
            .setInitialDelay(Duration.ofMillis(10))
            .setScheduledExecutor(scheduler)
            .build();

    ListenableFuture<String> future = executor.executeWithRetry(() -> "success");
    assertThat(future.get(5, SECONDS)).isEqualTo("success");
    assertThat(executor.getTotalAttempts()).isEqualTo(1);
    assertThat(executor.getTotalSuccesses()).isEqualTo(1);
    assertThat(executor.getTotalFailures()).isEqualTo(0);
  }

  public void testRetryOnFailureThenSucceed() throws Exception {
    AtomicInteger attempts = new AtomicInteger(0);

    RetryExecutor executor =
        RetryExecutor.builder()
            .setMaxRetries(3)
            .setInitialDelay(Duration.ofMillis(10))
            .setScheduledExecutor(scheduler)
            .build();

    ListenableFuture<String> future =
        executor.executeWithRetry(
            () -> {
              if (attempts.incrementAndGet() < 3) {
                throw new IOException("transient failure");
              }
              return "recovered";
            });

    assertThat(future.get(5, SECONDS)).isEqualTo("recovered");
    assertThat(attempts.get()).isEqualTo(3);
  }

  public void testExhaustedRetries() throws Exception {
    RetryExecutor executor =
        RetryExecutor.builder()
            .setMaxRetries(2)
            .setInitialDelay(Duration.ofMillis(10))
            .setScheduledExecutor(scheduler)
            .build();

    ListenableFuture<String> future =
        executor.executeWithRetry(
            (Callable<String>)
                () -> {
                  throw new IOException("persistent failure");
                });

    try {
      future.get(5, SECONDS);
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e.getCause()).isInstanceOf(IOException.class);
    }
    assertThat(executor.getTotalRetriesExhausted()).isEqualTo(1);
  }

  public void testRetryPredicateFiltersExceptions() throws Exception {
    AtomicInteger attempts = new AtomicInteger(0);

    RetryExecutor executor =
        RetryExecutor.builder()
            .setMaxRetries(5)
            .setInitialDelay(Duration.ofMillis(10))
            .setRetryPredicate((Predicate<Throwable>) t -> t instanceof IOException)
            .setScheduledExecutor(scheduler)
            .build();

    // IllegalArgumentException should NOT be retried
    ListenableFuture<String> future =
        executor.executeWithRetry(
            (Callable<String>)
                () -> {
                  attempts.incrementAndGet();
                  throw new IllegalArgumentException("non-retryable");
                });

    try {
      future.get(5, SECONDS);
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(attempts.get()).isEqualTo(1); // No retries for non-matching exception
  }

  public void testExponentialBackoffDelay() {
    RetryExecutor executor =
        RetryExecutor.builder()
            .setMaxRetries(5)
            .setInitialDelay(Duration.ofMillis(100))
            .setMaxDelay(Duration.ofSeconds(10))
            .setBackoffMultiplier(2.0)
            .setJitterFactor(0.0) // No jitter for predictable testing
            .setScheduledExecutor(scheduler)
            .build();

    long delay0 = executor.computeDelay(0);
    long delay1 = executor.computeDelay(1);
    long delay2 = executor.computeDelay(2);

    assertThat(delay0).isEqualTo(MILLISECONDS.toNanos(100));
    assertThat(delay1).isEqualTo(MILLISECONDS.toNanos(200));
    assertThat(delay2).isEqualTo(MILLISECONDS.toNanos(400));
  }

  public void testMaxDelayCapsBacking() {
    RetryExecutor executor =
        RetryExecutor.builder()
            .setMaxRetries(10)
            .setInitialDelay(Duration.ofSeconds(1))
            .setMaxDelay(Duration.ofSeconds(5))
            .setBackoffMultiplier(10.0)
            .setJitterFactor(0.0)
            .setScheduledExecutor(scheduler)
            .build();

    long delay = executor.computeDelay(5);
    assertThat(delay).isAtMost(SECONDS.toNanos(5));
  }

  public void testCircuitBreakerOpens() throws Exception {
    RetryExecutor executor =
        RetryExecutor.builder()
            .setMaxRetries(0)
            .setInitialDelay(Duration.ofMillis(10))
            .setCircuitBreakerThreshold(3)
            .setCircuitBreakerResetDuration(Duration.ofSeconds(60))
            .setScheduledExecutor(scheduler)
            .build();

    // Cause 3 failures to trip the circuit breaker
    for (int i = 0; i < 3; i++) {
      try {
        executor
            .executeWithRetry(
                (Callable<String>)
                    () -> {
                      throw new IOException("fail");
                    })
            .get(1, SECONDS);
      } catch (Exception ignored) {
      }
    }

    assertThat(executor.getCircuitState()).isEqualTo(RetryExecutor.CircuitState.OPEN);

    // Next request should fail with CircuitBreakerOpenException
    ListenableFuture<String> future = executor.executeWithRetry(() -> "should not run");
    try {
      future.get(1, SECONDS);
      fail("Expected circuit breaker exception");
    } catch (Exception e) {
      assertThat(e.getCause()).isInstanceOf(RetryExecutor.CircuitBreakerOpenException.class);
    }
  }

  public void testCircuitBreakerResetsOnSuccess() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);

    RetryExecutor executor =
        RetryExecutor.builder()
            .setMaxRetries(0)
            .setInitialDelay(Duration.ofMillis(10))
            .setCircuitBreakerThreshold(5)
            .setScheduledExecutor(scheduler)
            .build();

    // Two failures
    for (int i = 0; i < 2; i++) {
      try {
        executor
            .executeWithRetry(
                (Callable<String>)
                    () -> {
                      throw new IOException("fail");
                    })
            .get(1, SECONDS);
      } catch (Exception ignored) {
      }
    }

    // Then a success - should reset consecutive failure count
    executor.executeWithRetry(() -> "ok").get(1, SECONDS);
    assertThat(executor.getCircuitState()).isEqualTo(RetryExecutor.CircuitState.CLOSED);
  }

  public void testRunnableRetry() throws Exception {
    AtomicInteger counter = new AtomicInteger(0);

    RetryExecutor executor =
        RetryExecutor.builder()
            .setMaxRetries(3)
            .setInitialDelay(Duration.ofMillis(10))
            .setScheduledExecutor(scheduler)
            .build();

    ListenableFuture<Void> future =
        executor.executeWithRetry(
            (Runnable)
                () -> {
                  if (counter.incrementAndGet() < 2) {
                    throw new RuntimeException("transient");
                  }
                });

    future.get(5, SECONDS);
    assertThat(counter.get()).isEqualTo(2);
  }

  public void testBuilderValidation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> RetryExecutor.builder().setMaxRetries(-1));

    assertThrows(
        IllegalArgumentException.class,
        () -> RetryExecutor.builder().setBackoffMultiplier(0.5));

    assertThrows(
        IllegalArgumentException.class,
        () -> RetryExecutor.builder().setJitterFactor(1.5));

    assertThrows(
        IllegalStateException.class,
        () -> RetryExecutor.builder().build()); // No scheduled executor
  }

  public void testMetricsTracking() throws Exception {
    AtomicInteger attempts = new AtomicInteger(0);

    RetryExecutor executor =
        RetryExecutor.builder()
            .setMaxRetries(2)
            .setInitialDelay(Duration.ofMillis(10))
            .setScheduledExecutor(scheduler)
            .build();

    // Successful call
    executor.executeWithRetry(() -> "ok").get(5, SECONDS);

    // Failing call (exhausts retries)
    try {
      executor
          .executeWithRetry(
              (Callable<String>)
                  () -> {
                    throw new RuntimeException("fail");
                  })
          .get(5, SECONDS);
    } catch (Exception ignored) {
    }

    assertThat(executor.getTotalSuccesses()).isEqualTo(1);
    assertThat(executor.getTotalFailures()).isGreaterThan(0L);
    assertThat(executor.getTotalAttempts()).isGreaterThan(1L);
  }
}
