/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Uninterruptibles.joinUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static junit.framework.Assert.fail;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.TearDownAccepter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.jspecify.annotations.NullUnmarked;

/**
 * Utilities for performing thread interruption in tests
 *
 * @author Kevin Bourrillion
 * @author Chris Povirk
 */
@NullUnmarked
@GwtIncompatible
@J2ktIncompatible
final class InterruptionUtil {
  private static final Logger logger = Logger.getLogger(InterruptionUtil.class.getName());

  /** Runnable which will interrupt the target thread repeatedly when run. */
  private static final class Interruptenator implements Runnable {
    private final Thread interruptee;
    private volatile boolean shouldStop = false;

    Interruptenator(Thread interruptee) {
      this.interruptee = interruptee;
    }

    @Override
    public void run() {
      while (!shouldStop) {
        interruptee.interrupt();
      }
    }

    void stopInterrupting() {
      shouldStop = true;
    }
  }

  /** Interrupts the current thread after sleeping for the specified delay. */
  static void requestInterruptIn(long time, TimeUnit unit) {
    checkNotNull(unit);
    Thread interruptee = Thread.currentThread();
    new Thread(
            () -> {
              try {
                unit.sleep(time);
              } catch (InterruptedException wontHappen) {
                throw new AssertionError(wontHappen);
              }
              interruptee.interrupt();
            })
        .start();
  }

  static void repeatedlyInterruptTestThread(TearDownAccepter tearDownAccepter) {
    Interruptenator interruptingTask = new Interruptenator(Thread.currentThread());
    Thread interruptingThread = new Thread(interruptingTask);
    interruptingThread.start();
    tearDownAccepter.addTearDown(() -> {
      interruptingTask.stopInterrupting();
      interruptingThread.interrupt();
      joinUninterruptibly(interruptingThread, 2500, MILLISECONDS);
      Thread.interrupted();
      if (interruptingThread.isAlive()) {
        // This will be hidden by test-output redirection:
        logger.severe("InterruptenatorTask did not exit; future tests may be affected");
        /*
         * This fail() helps as long as TearDownAccepter is a TearDownStack that was *not*
         * configured with suppressThrows=true.
         */
        fail();
      }
    });
  }

  private InterruptionUtil() {}
}
