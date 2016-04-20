/*
 * Copyright (C) 2015 The Guava Authors
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

import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

import com.google.common.annotations.GwtCompatible;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;

@GwtCompatible(emulated = true)
abstract class InterruptibleTask implements Runnable {
  // These two fields are used to interrupt running tasks. The thread executing the task publishes
  // itself to the 'runner' field and the thread interrupting sets 'doneInterrupting' when it has
  // finished interrupting.
  private volatile Thread runner;
  private volatile boolean doneInterrupting;

  private static final AtomicHelper ATOMIC_HELPER;

  private static final Logger log = Logger.getLogger(InterruptibleTask.class.getName());

  static {
    AtomicHelper helper;
    try {
      helper =
          new SafeAtomicHelper(newUpdater(InterruptibleTask.class, (Class) Thread.class, "runner"));
    } catch (Throwable reflectionFailure) {
      // Some Android 5.0.x Samsung devices have bugs in JDK reflection APIs that cause
      // getDeclaredField to throw a NoSuchFieldException when the field is definitely there.
      // For these users fallback to a suboptimal implementation, based on synchronized. This will
      // be a definite performance hit to those users.
      log.log(Level.SEVERE, "SafeAtomicHelper is broken!", reflectionFailure);
      helper = new SynchronizedAtomicHelper();
    }
    ATOMIC_HELPER = helper;
  }

  @Override
  public final void run() {
    if (!ATOMIC_HELPER.compareAndSetRunner(this, null, Thread.currentThread())) {
      return; // someone else has run or is running.
    }
    try {
      runInterruptibly();
    } finally {
      if (wasInterrupted()) {
        // We were interrupted, it is possible that the interrupted bit hasn't been set yet. Wait
        // for the interrupting thread to set 'doneInterrupting' to true. See interruptTask().
        // We want to wait so that we don't interrupt the _next_ thing run on the thread.
        // Note: We don't reset the interrupted bit, just wait for it to be set.
        // If this is a thread pool thread, the thread pool will reset it for us. Otherwise, the
        // interrupted bit may have been intended for something else, so don't clear it.
        while (!doneInterrupting) {
          Thread.yield();
        }
      }
    }
  }

  abstract void runInterruptibly();

  abstract boolean wasInterrupted();

  final void interruptTask() {
    // interruptTask is guaranteed to be called at most once, and if runner is non-null when that
    // happens, then it must have been the first thread that entered run(). So there is no risk that
    // we are interrupting the wrong thread.
    Thread currentRunner = runner;
    if (currentRunner != null) {
      currentRunner.interrupt();
    }
    doneInterrupting = true;
  }

  private abstract static class AtomicHelper {
    /**
     * Atomic compare-and-set of the {@link InterruptibleTask#runner} field.
     * @return true if successful
     */
    abstract boolean compareAndSetRunner(InterruptibleTask task, Thread expect, Thread update);
  }

  private static final class SafeAtomicHelper extends AtomicHelper {
    final AtomicReferenceFieldUpdater<InterruptibleTask, Thread> runnerUpdater;

    SafeAtomicHelper(AtomicReferenceFieldUpdater runnerUpdater) {
      this.runnerUpdater = runnerUpdater;
    }

    @Override
    boolean compareAndSetRunner(InterruptibleTask task, Thread expect, Thread update) {
      return runnerUpdater.compareAndSet(task, expect, update);
    }
  }

  private static final class SynchronizedAtomicHelper extends AtomicHelper {
    @Override
    boolean compareAndSetRunner(InterruptibleTask task, Thread expect, Thread update) {
      synchronized (task) {
        if (task.runner == expect) {
          task.runner = update;
        }
      }
      return true;
    }
  }
}
