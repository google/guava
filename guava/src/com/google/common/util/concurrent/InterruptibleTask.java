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

import com.google.common.annotations.GwtCompatible;
import com.google.j2objc.annotations.ReflectionSupport;
import java.util.concurrent.atomic.AtomicReference;

@GwtCompatible(emulated = true)
@ReflectionSupport(value = ReflectionSupport.Level.FULL)
// Some Android 5.0.x Samsung devices have bugs in JDK reflection APIs that cause
// getDeclaredField to throw a NoSuchFieldException when the field is definitely there.
// Since this class only needs CAS on one field, we can avoid this bug by extending AtomicReference
// instead of using an AtomicReferenceFieldUpdater.
abstract class InterruptibleTask extends AtomicReference<Thread> implements Runnable {
  // The thread executing the task publishes itself to the superclass' reference and the thread
  // interrupting sets 'doneInterrupting' when it has finished interrupting.
  private volatile boolean doneInterrupting;

  @Override
  public final void run() {
    if (!compareAndSet(null, Thread.currentThread())) {
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
    Thread currentRunner = get();
    if (currentRunner != null) {
      currentRunner.interrupt();
    }
    doneInterrupting = true;
  }

  @Override
  public abstract String toString();
}
