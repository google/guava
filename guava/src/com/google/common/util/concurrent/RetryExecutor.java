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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Predicate;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.CheckForNull;

/**
 * An executor that retries failed operations with configurable retry policies, exponential backoff
 * with jitter, and circuit breaker support. Retried operations return {@link ListenableFuture} for
 * async composition.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RetryExecutor executor = RetryExecutor.builder()
 *     .setMaxRetries(3)
 *     .setInitialDelay(Duration.ofMillis(100))
 *     .setMaxDelay(Duration.ofSeconds(10))
 *     .setBackoffMultiplier(2.0)
 *     .setJitterFactor(0.1)
 *     .setRetryPredicate(e -> e instanceof IOException)
 *     .setScheduledExecutor(scheduledExecutor)
 *     .build();
 *
 * ListenableFuture<String> result = executor.executeWithRetry(() -> fetchData());
 * }</pre>
 *
 * @since 33.0
 */
@Beta
@GwtIncompatible
public final class RetryExecutor {

  /** The state of the circuit breaker. */
  enum CircuitState {
    CLOSED,
    OPEN,
    HALF_OPEN
  }

  private final int maxRetries;
  private final long initialDelayNanos;
  private final long maxDelayNanos;
  private final double backoffMultiplier;
  private final double jitterFactor;
  private final Predicate<Throwable> retryPredicate;
  private final ScheduledExecutorService scheduledExecutor;

  // Circuit breaker state
  private final int circuitBreakerThreshold;
  private final long circuitBreakerResetNanos;
  private final AtomicReference<CircuitState> circuitState;
  private final AtomicInteger consecutiveFailures;
  private final AtomicLong circuitOpenedAtNanos;

  // Metrics
  private final AtomicLong totalAttempts;
  private final AtomicLong totalSuccesses;
  private final AtomicLong totalFailures;
  private final AtomicLong totalRetriesExhausted;

  private RetryExecutor(Builder builder) {
    this.maxRetries = builder.maxRetries;
    this.initialDelayNanos = builder.initialDelayNanos;
    this.maxDelayNanos = builder.maxDelayNanos;
    this.backoffMultiplier = builder.backoffMultiplier;
    this.jitterFactor = builder.jitterFactor;
    this.retryPredicate = builder.retryPredicate;
    this.scheduledExecutor = checkNotNull(builder.scheduledExecutor);
    this.circuitBreakerThreshold = builder.circuitBreakerThreshold;
    this.circuitBreakerResetNanos = builder.circuitBreakerResetNanos;
    this.circuitState = new AtomicReference<>(CircuitState.CLOSED);
    this.consecutiveFailures = new AtomicInteger(0);
    this.circuitOpenedAtNanos = new AtomicLong(0);
    this.totalAttempts = new AtomicLong(0);
    this.totalSuccesses = new AtomicLong(0);
    this.totalFailures = new AtomicLong(0);
    this.totalRetriesExhausted = new AtomicLong(0);
  }

  /** Returns a new {@link Builder} for configuring a {@code RetryExecutor}. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Executes the given callable with retry logic. Returns a {@link ListenableFuture} that completes
   * when the callable succeeds or all retries are exhausted.
   */
  @CanIgnoreReturnValue
  public <T> ListenableFuture<T> executeWithRetry(Callable<T> callable) {
    checkNotNull(callable, "callable");
    SettableFuture<T> resultFuture = SettableFuture.create();
    attemptExecution(callable, 0, resultFuture);
    return resultFuture;
  }

  /**
   * Executes the given runnable with retry logic. Returns a {@link ListenableFuture} that completes
   * when the runnable succeeds or all retries are exhausted.
   */
  @CanIgnoreReturnValue
  public ListenableFuture<Void> executeWithRetry(Runnable runnable) {
    checkNotNull(runnable, "runnable");
    return executeWithRetry(
        () -> {
          runnable.run();
          return null;
        });
  }

  private <T> void attemptExecution(
      Callable<T> callable, int attempt, SettableFuture<T> resultFuture) {
    if (resultFuture.isCancelled()) {
      return;
    }

    // Check circuit breaker
    if (!isCircuitAllowingRequests()) {
      resultFuture.setException(
          new CircuitBreakerOpenException(
              "Circuit breaker is open after " + consecutiveFailures.get() + " failures"));
      return;
    }

    totalAttempts.incrementAndGet();

    try {
      T result = callable.call();
      onSuccess();
      resultFuture.set(result);
    } catch (Exception e) {
      onFailure();

      if (attempt >= maxRetries || !shouldRetry(e)) {
        totalRetriesExhausted.incrementAndGet();
        resultFuture.setException(e);
        return;
      }

      long delayNanos = computeDelay(attempt);
      scheduledExecutor.schedule(
          () -> attemptExecution(callable, attempt + 1, resultFuture),
          delayNanos,
          TimeUnit.NANOSECONDS);
    }
  }

  private boolean shouldRetry(Throwable t) {
    if (retryPredicate == null) {
      return true;
    }
    return retryPredicate.apply(t);
  }

  long computeDelay(int attempt) {
    double delay = initialDelayNanos * Math.pow(backoffMultiplier, attempt);
    delay = Math.min(delay, maxDelayNanos);

    if (jitterFactor > 0) {
      double jitter = delay * jitterFactor;
      double randomJitter = ThreadLocalRandom.current().nextDouble(-jitter, jitter);
      delay = delay + randomJitter;
    }

    return Math.max(0, (long) delay);
  }

  private boolean isCircuitAllowingRequests() {
    CircuitState state = circuitState.get();
    if (state == CircuitState.CLOSED) {
      return true;
    }
    if (state == CircuitState.OPEN) {
      long elapsed = System.nanoTime() - circuitOpenedAtNanos.get();
      if (elapsed >= circuitBreakerResetNanos) {
        circuitState.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN);
        return true;
      }
      return false;
    }
    // HALF_OPEN: allow one request through to test
    return true;
  }

  private void onSuccess() {
    totalSuccesses.incrementAndGet();
    consecutiveFailures.set(0);
    circuitState.set(CircuitState.CLOSED);
  }

  private void onFailure() {
    totalFailures.incrementAndGet();
    int failures = consecutiveFailures.incrementAndGet();
    if (circuitBreakerThreshold > 0 && failures >= circuitBreakerThreshold) {
      if (circuitState.compareAndSet(CircuitState.CLOSED, CircuitState.OPEN)
          || circuitState.compareAndSet(CircuitState.HALF_OPEN, CircuitState.OPEN)) {
        circuitOpenedAtNanos.set(System.nanoTime());
      }
    }
  }

  /** Returns the total number of execution attempts. */
  public long getTotalAttempts() {
    return totalAttempts.get();
  }

  /** Returns the total number of successful executions. */
  public long getTotalSuccesses() {
    return totalSuccesses.get();
  }

  /** Returns the total number of failed attempts. */
  public long getTotalFailures() {
    return totalFailures.get();
  }

  /** Returns the number of times retries were exhausted without success. */
  public long getTotalRetriesExhausted() {
    return totalRetriesExhausted.get();
  }

  /** Returns the current circuit breaker state. */
  public CircuitState getCircuitState() {
    return circuitState.get();
  }

  /** Exception thrown when the circuit breaker is open. */
  public static final class CircuitBreakerOpenException extends RuntimeException {
    CircuitBreakerOpenException(String message) {
      super(message);
    }
  }

  /** Builder for {@link RetryExecutor}. */
  public static final class Builder {
    private int maxRetries = 3;
    private long initialDelayNanos = TimeUnit.MILLISECONDS.toNanos(100);
    private long maxDelayNanos = TimeUnit.SECONDS.toNanos(30);
    private double backoffMultiplier = 2.0;
    private double jitterFactor = 0.1;
    @CheckForNull private Predicate<Throwable> retryPredicate;
    @CheckForNull private ScheduledExecutorService scheduledExecutor;
    private int circuitBreakerThreshold = 0; // 0 = disabled
    private long circuitBreakerResetNanos = TimeUnit.SECONDS.toNanos(60);

    private Builder() {}

    /** Sets the maximum number of retries. Must be non-negative. */
    @CanIgnoreReturnValue
    public Builder setMaxRetries(int maxRetries) {
      checkArgument(maxRetries >= 0, "maxRetries must be non-negative, was %s", maxRetries);
      this.maxRetries = maxRetries;
      return this;
    }

    /** Sets the initial delay between retries. */
    @CanIgnoreReturnValue
    public Builder setInitialDelay(Duration delay) {
      checkNotNull(delay);
      checkArgument(!delay.isNegative(), "delay must be non-negative");
      this.initialDelayNanos = delay.toNanos();
      return this;
    }

    /** Sets the maximum delay between retries (caps exponential backoff). */
    @CanIgnoreReturnValue
    public Builder setMaxDelay(Duration maxDelay) {
      checkNotNull(maxDelay);
      checkArgument(!maxDelay.isNegative(), "maxDelay must be non-negative");
      this.maxDelayNanos = maxDelay.toNanos();
      return this;
    }

    /** Sets the backoff multiplier for exponential backoff. Must be >= 1.0. */
    @CanIgnoreReturnValue
    public Builder setBackoffMultiplier(double multiplier) {
      checkArgument(multiplier >= 1.0, "multiplier must be >= 1.0, was %s", multiplier);
      this.backoffMultiplier = multiplier;
      return this;
    }

    /** Sets the jitter factor (0.0 to 1.0). Adds randomness to delay. */
    @CanIgnoreReturnValue
    public Builder setJitterFactor(double jitterFactor) {
      checkArgument(
          jitterFactor >= 0.0 && jitterFactor <= 1.0,
          "jitterFactor must be between 0.0 and 1.0, was %s",
          jitterFactor);
      this.jitterFactor = jitterFactor;
      return this;
    }

    /** Sets a predicate to determine if a given exception should be retried. */
    @CanIgnoreReturnValue
    public Builder setRetryPredicate(Predicate<Throwable> retryPredicate) {
      this.retryPredicate = checkNotNull(retryPredicate);
      return this;
    }

    /** Sets the scheduled executor used for delayed retries. Required. */
    @CanIgnoreReturnValue
    public Builder setScheduledExecutor(ScheduledExecutorService executor) {
      this.scheduledExecutor = checkNotNull(executor);
      return this;
    }

    /**
     * Enables circuit breaker with the given failure threshold. After {@code threshold}
     * consecutive failures, the circuit opens and rejects requests until the reset duration elapses.
     */
    @CanIgnoreReturnValue
    public Builder setCircuitBreakerThreshold(int threshold) {
      checkArgument(threshold >= 0, "threshold must be non-negative, was %s", threshold);
      this.circuitBreakerThreshold = threshold;
      return this;
    }

    /** Sets how long the circuit breaker stays open before transitioning to half-open. */
    @CanIgnoreReturnValue
    public Builder setCircuitBreakerResetDuration(Duration duration) {
      checkNotNull(duration);
      checkArgument(!duration.isNegative(), "duration must be non-negative");
      this.circuitBreakerResetNanos = duration.toNanos();
      return this;
    }

    /** Builds the {@link RetryExecutor}. A scheduled executor must be set. */
    public RetryExecutor build() {
      checkState(scheduledExecutor != null, "scheduledExecutor must be set");
      return new RetryExecutor(this);
    }
  }
}
