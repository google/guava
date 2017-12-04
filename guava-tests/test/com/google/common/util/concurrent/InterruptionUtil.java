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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static junit.framework.Assert.fail;

import com.google.common.testing.TearDown;
import com.google.common.testing.TearDownAccepter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Utilities for performing thread interruption in tests
 *
 * @author Kevin Bourrillion
 * @author Chris Povirk
 */
final class InterruptionUtil {
  private static final Logger logger = Logger.getLogger(InterruptionUtil.class.getName());

  /** Runnable which will interrupt the target thread repeatedly when run. */
  private static final class Interruptenator implements Runnable {
    private final long everyMillis;
    private final Thread interruptee;
    private volatile boolean shouldStop = false;

    Interruptenator(Thread interruptee, long everyMillis) {
      this.everyMillis = everyMillis;
      this.interruptee = interruptee;
    }

    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(everyMillis);
        } catch (InterruptedException e) {
          // ok. just stop sleeping.
        }
        if (shouldStop) {
          break;
        }
        interruptee.interrupt();
      }
    }

    void stopInterrupting() {
      shouldStop = true;
    }
  }

  /** Interrupts the current thread after sleeping for the specified delay. */
  static void requestInterruptIn(final long time, final TimeUnit unit) {
    checkNotNull(unit);
    final Thread interruptee = Thread.currentThread();
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  unit.sleep(time);
                } catch (InterruptedException wontHappen) {
                  throw new AssertionError(wontHappen);
                }
                interruptee.interrupt();
              }
            })
        .start();
  }

  static void repeatedlyInterruptTestThread(
      long interruptPeriodMillis, TearDownAccepter tearDownAccepter) {
    final Interruptenator interruptingTask =
        new Interruptenator(Thread.currentThread(), interruptPeriodMillis);
    final Thread interruptingThread = new Thread(interruptingTask);
    interruptingThread.start();
    tearDownAccepter.addTearDown(
        new TearDown() {
          @Override
          public void tearDown() throws Exception {
            interruptingTask.stopInterrupting();
            interruptingThread.interrupt();
            joinUninterruptibly(interruptingThread, 2500, MILLISECONDS);
            Thread.interrupted();
            if (interruptingThread.isAlive()) {
              // This will be hidden by test-output redirection:
              logger.severe("InterruptenatorTask did not exit; future tests may be affected");
              /*
               * This won't do any good under JUnit 3, but I'll leave it around in
               * case we ever switch to JUnit 4:
               */
              fail();
            }
          }
        });
  }

  // TODO(cpovirk): promote to Uninterruptibles, and add untimed version
  private static void joinUninterruptibly(Thread thread, long timeout, TimeUnit unit) {
    boolean interrupted = false;
    try {
      long remainingNanos = unit.toNanos(timeout);
      long end = System.nanoTime() + remainingNanos;

      while (true) {
        try {
          // TimeUnit.timedJoin() treats negative timeouts just like zero.
          NANOSECONDS.timedJoin(thread, remainingNanos);
          return;
        } catch (InterruptedException e) {
          interrupted = true;
          remainingNanos = end - System.nanoTime();
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
