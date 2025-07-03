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

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/** See newDirectExecutorService javadoc for behavioral notes. */
@J2ktIncompatible // Emulated
@GwtIncompatible
final class DirectExecutorService extends AbstractListeningExecutorService {

  /** Lock used whenever accessing the state variables (runningTasks, shutdown) of the executor */
  private final Object lock = new Object();

  /*
   * Conceptually, these two variables describe the executor being in
   * one of three states:
   *   - Active: shutdown == false
   *   - Shutdown: runningTasks > 0 and shutdown == true
   *   - Terminated: runningTasks == 0 and shutdown == true
   */
  @GuardedBy("lock")
  private int runningTasks = 0;

  @GuardedBy("lock")
  private boolean shutdown = false;

  @Override
  public void execute(Runnable command) {
    startTask();
    try {
      command.run();
    } finally {
      endTask();
    }
  }

  @Override
  public boolean isShutdown() {
    synchronized (lock) {
      return shutdown;
    }
  }

  @Override
  public void shutdown() {
    synchronized (lock) {
      shutdown = true;
      if (runningTasks == 0) {
        lock.notifyAll();
      }
    }
  }

  // See newDirectExecutorService javadoc for unusual behavior of this method.
  @Override
  public List<Runnable> shutdownNow() {
    shutdown();
    return ImmutableList.of();
  }

  @Override
  public boolean isTerminated() {
    synchronized (lock) {
      return shutdown && runningTasks == 0;
    }
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    synchronized (lock) {
      while (true) {
        if (shutdown && runningTasks == 0) {
          return true;
        } else if (nanos <= 0) {
          return false;
        } else {
          long now = System.nanoTime();
          NANOSECONDS.timedWait(lock, nanos);
          nanos -= System.nanoTime() - now; // subtract the actual time we waited
        }
      }
    }
  }

  /**
   * Checks if the executor has been shut down and increments the running task count.
   *
   * @throws RejectedExecutionException if the executor has been previously shutdown
   */
  private void startTask() {
    synchronized (lock) {
      if (shutdown) {
        throw new RejectedExecutionException("Executor already shutdown");
      }
      runningTasks++;
    }
  }

  /** Decrements the running task count. */
  private void endTask() {
    synchronized (lock) {
      int numRunning = --runningTasks;
      if (numRunning == 0) {
        lock.notifyAll();
      }
    }
  }
}
