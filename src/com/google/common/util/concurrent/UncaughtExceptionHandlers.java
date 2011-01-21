// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.common.util.concurrent;

import static java.util.logging.Level.SEVERE;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.Logger;

/**
 * Factories for {@link UncaughtExceptionHandler} instances.
 *
 * @author gak@google.com (Gregory Kick)
 * @since 8
 */
@Beta
public final class UncaughtExceptionHandlers {
  private UncaughtExceptionHandlers() {}

  /**
   * Returns an exception handler that exits the system. This is particularly useful for the main
   * thread, which may start up other, non-daemon threads, but fail to fully initialize the
   * application successfully.
   *
   * <p>Example usage:
   * <pre>public static void main(String[] args) {
   *   Thread.currentThread().setUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());
   *   ...
   * </pre>
   */
  public static UncaughtExceptionHandler systemExit() {
    return new Exiter(Runtime.getRuntime());
  }

  @VisibleForTesting static final class Exiter implements UncaughtExceptionHandler {
    private static final Logger logger = Logger.getLogger(Exiter.class.getName());

    private final Runtime runtime;

    Exiter(Runtime runtime) {
      this.runtime = runtime;
    }

    @Override public void uncaughtException(Thread t, Throwable e) {
      // cannot use FormattingLogger due to a dependency loop
      logger.log(SEVERE, String.format("Caught an exception in %s.  Shutting down.", t), e);
      runtime.exit(1);
    }
  }
}
