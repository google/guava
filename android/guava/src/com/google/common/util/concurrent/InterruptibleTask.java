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

import static com.google.common.util.concurrent.NullnessCasts.uncheckedCastNullableTToT;
import static com.google.common.util.concurrent.Platform.restoreInterruptIfIsInterruptedException;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.j2objc.annotations.ReflectionSupport;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.LockSupport;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@GwtCompatible(emulated = true)
@ReflectionSupport(value = ReflectionSupport.Level.FULL)
@ElementTypesAreNonnullByDefault
// Some Android 5.0.x Samsung devices have bugs in JDK reflection APIs that cause
// getDeclaredField to throw a NoSuchFieldException when the field is definitely there.
// Since this class only needs CAS on one field, we can avoid this bug by extending AtomicReference
// instead of using an AtomicReferenceFieldUpdater. This reference stores Thread instances
// and DONE/INTERRUPTED - they have a common ancestor of Runnable.
abstract class InterruptibleTask<T extends @Nullable Object>
    extends AtomicReference<@Nullable Runnable> implements Runnable {
  static {
    // Prevent rare disastrous classloading in first call to LockSupport.park.
    // See: https://bugs.openjdk.java.net/browse/JDK-8074773
    @SuppressWarnings("unused")
    Class<?> ensureLoaded = LockSupport.class;
  }

  private static final class DoNothingRunnable implements Runnable {
    @Override
    public void run() {}
  }
  // The thread executing the task publishes itself to the superclass' reference and the thread
  // interrupting sets DONE when it has finished interrupting.
  private static final Runnable DONE = new DoNothingRunnable();
  private static final Runnable PARKED = new DoNothingRunnable();
  // Why 1000?  WHY NOT!
  private static final int MAX_BUSY_WAIT_SPINS = 1000;

  @SuppressWarnings("ThreadPriorityCheck") // The cow told me to
  @Override
  public final void run() {
    /*
     * Set runner thread before checking isDone(). If we were to check isDone() first, the task
     * might be cancelled before we set the runner thread. That would make it impossible to
     * interrupt, yet it will still run, since interruptTask will leave the runner value null,
     * allowing the CAS below to succeed.
     */
    Thread currentThread = Thread.currentThread();
    if (!compareAndSet(null, currentThread)) {
      return; // someone else has run or is running.
    }

    boolean run = !isDone();
    T result = null;
    Throwable error = null;
    try {
      if (run) {
        result = runInterruptibly();
      }
    } catch (Throwable t) {
      restoreInterruptIfIsInterruptedException(t);
      error = t;
    } finally {
      // Attempt to set the task as done so that further attempts to interrupt will fail.
      if (!compareAndSet(currentThread, DONE)) {
        waitForInterrupt(currentThread);
      }
      if (run) {
        if (error == null) {
          // The cast is safe because of the `run` and `error` checks.
          afterRanInterruptiblySuccess(uncheckedCastNullableTToT(result));
        } else {
          afterRanInterruptiblyFailure(error);
        }
      }
    }
  }

  private void waitForInterrupt(Thread currentThread) {
    /*
     * If someone called cancel(true), it is possible that the interrupted bit hasn't been set yet.
     * Wait for the interrupting thread to set DONE. (See interruptTask().) We want to wait so that
     * the interrupting thread doesn't interrupt the _next_ thing to run on this thread.
     *
     * Note: We don't reset the interrupted bit, just wait for it to be set. If this is a thread
     * pool thread, the thread pool will reset it for us. Otherwise, the interrupted bit may have
     * been intended for something else, so don't clear it.
     */
    boolean restoreInterruptedBit = false;
    int spinCount = 0;
    // Interrupting Cow Says:
    //  ______
    // < Spin >
    //  ------
    //        \   ^__^
    //         \  (oo)\_______
    //            (__)\       )\/\
    //                ||----w |
    //                ||     ||
    Runnable state = get();
    Blocker blocker = null;
    while (state instanceof Blocker || state == PARKED) {
      if (state instanceof Blocker) {
        blocker = (Blocker) state;
      }
      spinCount++;
      if (spinCount > MAX_BUSY_WAIT_SPINS) {
        /*
         * If we have spun a lot, just park ourselves. This will save CPU while we wait for a slow
         * interrupting thread. In theory, interruptTask() should be very fast, but due to
         * InterruptibleChannel and JavaLangAccess.blockedOn(Thread, Interruptible), it isn't
         * predictable what work might be done. (e.g., close a file and flush buffers to disk). To
         * protect ourselves from this, we park ourselves and tell our interrupter that we did so.
         */
        if (state == PARKED || compareAndSet(state, PARKED)) {
          // Interrupting Cow Says:
          //  ______
          // < Park >
          //  ------
          //        \   ^__^
          //         \  (oo)\_______
          //            (__)\       )\/\
          //                ||----w |
          //                ||     ||
          // We need to clear the interrupted bit prior to calling park and maintain it in case we
          // wake up spuriously.
          restoreInterruptedBit = Thread.interrupted() || restoreInterruptedBit;
          LockSupport.park(blocker);
        }
      } else {
        Thread.yield();
      }
      state = get();
    }
    if (restoreInterruptedBit) {
      currentThread.interrupt();
    }
    /*
     * TODO(cpovirk): Clear interrupt status here? We currently don't, which means that an interrupt
     * before, during, or after runInterruptibly() (unless it produced an InterruptedException
     * caught above) can linger and affect listeners.
     */
  }

  /**
   * Called before runInterruptibly - if true, runInterruptibly and afterRanInterruptibly will not
   * be called.
   */
  abstract boolean isDone();

  /**
   * Do interruptible work here - do not complete Futures here, as their listeners could be
   * interrupted.
   */
  @ParametricNullness
  abstract T runInterruptibly() throws Exception;

  /**
   * Any interruption that happens as a result of calling interruptTask will arrive before this
   * method is called. Complete Futures here.
   */
  abstract void afterRanInterruptiblySuccess(@ParametricNullness T result);

  /**
   * Any interruption that happens as a result of calling interruptTask will arrive before this
   * method is called. Complete Futures here.
   */
  abstract void afterRanInterruptiblyFailure(Throwable error);

  /**
   * Interrupts the running task. Because this internally calls {@link Thread#interrupt()} which can
   * in turn invoke arbitrary code it is not safe to call while holding a lock.
   */
  final void interruptTask() {
    // Since the Thread is replaced by DONE before run() invokes listeners or returns, if we succeed
    // in this CAS, there's no risk of interrupting the wrong thread or interrupting a thread that
    // isn't currently executing this task.
    Runnable currentRunner = get();
    if (currentRunner instanceof Thread) {
      Blocker blocker = new Blocker(this);
      blocker.setOwner(Thread.currentThread());
      if (compareAndSet(currentRunner, blocker)) {
        // Thread.interrupt can throw arbitrary exceptions due to the nio InterruptibleChannel API
        // This will make sure that tasks don't get stuck busy waiting.
        // Some of this is fixed in jdk11 (see https://bugs.openjdk.java.net/browse/JDK-8198692) but
        // not all.  See the test cases for examples on how this can happen.
        try {
          ((Thread) currentRunner).interrupt();
        } finally {
          Runnable prev = getAndSet(DONE);
          if (prev == PARKED) {
            LockSupport.unpark((Thread) currentRunner);
          }
        }
      }
    }
  }

  /**
   * Using this as the blocker object allows introspection and debugging tools to see that the
   * currentRunner thread is blocked on the progress of the interruptor thread, which can help
   * identify deadlocks.
   */
  @VisibleForTesting
  static final class Blocker extends AbstractOwnableSynchronizer implements Runnable {
    private final InterruptibleTask<?> task;

    private Blocker(InterruptibleTask<?> task) {
      this.task = task;
    }

    @Override
    public void run() {}

    private void setOwner(Thread thread) {
      super.setExclusiveOwnerThread(thread);
    }

    @VisibleForTesting
    @CheckForNull
    Thread getOwner() {
      return super.getExclusiveOwnerThread();
    }

    @Override
    public String toString() {
      return task.toString();
    }
  }

  @Override
  public final String toString() {
    Runnable state = get();
    String result;
    if (state == DONE) {
      result = "running=[DONE]";
    } else if (state instanceof Blocker) {
      result = "running=[INTERRUPTED]";
    } else if (state instanceof Thread) {
      // getName is final on Thread, no need to worry about exceptions
      result = "running=[RUNNING ON " + ((Thread) state).getName() + "]";
    } else {
      result = "running=[NOT STARTED YET]";
    }
    return result + ", " + toPendingString();
  }

  abstract String toPendingString();
}
