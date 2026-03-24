package com.google.common.util.concurrent;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes a {@link Callable} with configurable retry behavior on failure.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RetryExecutor executor = RetryExecutor.builder()
 *     .maxRetries(3)
 *     .backoffMillis(1000)
 *     .exponentialBackoff(true)
 *     .retryOn(IOException.class)
 *     .build();
 *
 * String result = executor.execute(() -> fetchFromRemoteService());
 * }</pre>
 *
 * @since 33.0
 */
public final class RetryExecutor {

  private static final Logger logger = Logger.getLogger(RetryExecutor.class.getName());

  private final int maxRetries;
  private final long backoffMillis;
  private final boolean exponentialBackoff;
  private final Set<Class<? extends Exception>> retryableExceptions;

  private RetryExecutor(Builder builder) {
    this.maxRetries = builder.maxRetries;
    this.backoffMillis = builder.backoffMillis;
    this.exponentialBackoff = builder.exponentialBackoff;
    this.retryableExceptions = new HashSet<>(builder.retryableExceptions);
  }

  /**
   * Executes the given callable, retrying on failure according to this executor's
   * configuration.
   *
   * @param task the callable to execute
   * @return the result of the callable
   * @throws Exception the last exception thrown if all retries are exhausted
   * @throws NullPointerException if task is null
   */
  public <T> T execute(Callable<T> task) throws Exception {
    checkNotNull(task, "task must not be null");

    Exception lastException = null;

    for (int attempt = 0; attempt < maxRetries; attempt++) {
      try {
        return task.call();
      } catch (Exception e) {
        if (!isRetryable(e)) {
          throw e;
        }
        lastException = e;
        logger.log(Level.WARNING,
            String.format("Attempt %d/%d failed, retrying in %dms...",
                attempt + 1, maxRetries, calculateDelay(attempt)),
            e);

        if (attempt < maxRetries - 1) {
          long delay = calculateDelay(attempt);
          try {
            Thread.sleep(delay);
          } catch (InterruptedException ie) {
            throw lastException;
          }
        }
      }
    }

    throw lastException;
  }

  private boolean isRetryable(Exception e) {
    if (retryableExceptions.isEmpty()) {
      return true;
    }
    return retryableExceptions.contains(e.getClass());
  }

  private long calculateDelay(int attempt) {
    if (exponentialBackoff) {
      return (long) (Math.pow(2, attempt) * backoffMillis);
    }
    return backoffMillis;
  }

  /** Returns a new {@link Builder} for configuring a RetryExecutor. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link RetryExecutor}.
   *
   * @since 33.0
   */
  public static final class Builder {
    private int maxRetries = 3;
    private long backoffMillis = 1000;
    private boolean exponentialBackoff = false;
    private final Set<Class<? extends Exception>> retryableExceptions = new HashSet<>();

    private Builder() {}

    /** Sets the maximum number of retries. Default is 3. */
    public Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    /** Sets the base backoff delay in milliseconds. Default is 1000ms. */
    public Builder backoffMillis(long backoffMillis) {
      this.backoffMillis = backoffMillis;
      return this;
    }

    /** Enables or disables exponential backoff. Default is false (fixed delay). */
    public Builder exponentialBackoff(boolean exponentialBackoff) {
      this.exponentialBackoff = exponentialBackoff;
      return this;
    }

    /**
     * Adds an exception type to retry on. If no exception types are specified,
     * all exceptions trigger a retry.
     */
    public Builder retryOn(Class<? extends Exception> exceptionClass) {
      this.retryableExceptions.add(checkNotNull(exceptionClass));
      return this;
    }

    /** Builds the RetryExecutor. */
    public RetryExecutor build() {
      checkArgument(maxRetries > 0, "maxRetries must be positive");
      checkArgument(backoffMillis >= 0, "backoffMillis must be non-negative");
      return new RetryExecutor(this);
    }
  }
}
