/*
 * Copyright (C) 2010 The Guava Authors
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

import static java.util.logging.Level.SEVERE;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Factories for {@link UncaughtExceptionHandler} instances.
 *
 * @author Gregory Kick
 * @since 8.0
 */
@GwtIncompatible
public final class UncaughtExceptionHandlers {
  private UncaughtExceptionHandlers() {}

  /**
   * Returns an exception handler that exits the system. This is particularly useful for the main
   * thread, which may start up other, non-daemon threads, but fail to fully initialize the
   * application successfully.
   *
   * <p>Example usage:
   *
   * <pre>
   * public static void main(String[] args) {
   *   Thread.currentThread().setUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());
   *   ...
   * </pre>
   *
   * <p>The returned handler logs any exception at severity {@code SEVERE} and then shuts down the
   * process with an exit status of 1, indicating abnormal termination.
   */
  public static UncaughtExceptionHandler systemExit() {
    return new Exiter(Runtime.getRuntime());
  }

  @VisibleForTesting
  static final class Exiter implements UncaughtExceptionHandler {
    private static final Logger logger = Logger.getLogger(Exiter.class.getName());

    private final Runtime runtime;

    Exiter(Runtime runtime) {
      this.runtime = runtime;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
      try {
        // cannot use FormattingLogger due to a dependency loop
        logger.log(
            SEVERE, String.format(Locale.ROOT, "Caught an exception in %s.  Shutting down.", t), e);
      } catch (Throwable errorInLogging) {
        // If logging fails, e.g. due to missing memory, at least try to log the
        // message and the cause for the failed logging.
        System.err.println(e.getMessage());
        System.err.println(errorInLogging.getMessage());
      } finally {
        runtime.exit(1);
      }
    }
  }
}
