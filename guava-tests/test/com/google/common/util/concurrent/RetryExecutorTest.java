package com.google.common.util.concurrent;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/** Tests for {@link RetryExecutor}. */
public class RetryExecutorTest {

  @Test
  public void execute_successOnFirstAttempt() throws Exception {
    RetryExecutor executor = RetryExecutor.builder()
        .maxRetries(3)
        .backoffMillis(10)
        .build();

    String result = executor.execute(() -> "hello");
    assertEquals("hello", result);
  }

  @Test
  public void execute_retriesThenSucceeds() throws Exception {
    AtomicInteger attempts = new AtomicInteger(0);
    RetryExecutor executor = RetryExecutor.builder()
        .maxRetries(3)
        .backoffMillis(10)
        .build();

    String result = executor.execute(() -> {
      if (attempts.incrementAndGet() < 3) {
        throw new RuntimeException("not yet");
      }
      return "recovered";
    });

    assertEquals("recovered", result);
    assertEquals(3, attempts.get());
  }

  @Test(expected = RuntimeException.class)
  public void execute_throwsAfterMaxRetries() throws Exception {
    RetryExecutor executor = RetryExecutor.builder()
        .maxRetries(2)
        .backoffMillis(10)
        .build();

    executor.execute(() -> {
      throw new RuntimeException("always fails");
    });
  }

  @Test
  public void execute_retryOnlyOnSpecifiedException() throws Exception {
    AtomicInteger attempts = new AtomicInteger(0);
    RetryExecutor executor = RetryExecutor.builder()
        .maxRetries(3)
        .backoffMillis(10)
        .retryOn(IllegalStateException.class)
        .build();

    String result = executor.execute(() -> {
      if (attempts.incrementAndGet() == 1) {
        throw new IllegalStateException("transient");
      }
      return "ok";
    });

    assertEquals("ok", result);
  }
}
