/*
 * Copyright (C) 2006 The Guava Authors
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

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of {@code Futures#withTimeout}.
 *
 * <p>Future that delegates to another but will finish early (via a {@link TimeoutException} wrapped
 * in an {@link ExecutionException}) if the specified duration expires. The delegate future is
 * interrupted and cancelled if it times out.
 */
@GwtIncompatible
final class TimeoutFuture<V> extends FluentFuture.TrustedFuture<V> {
  static <V> ListenableFuture<V> create(
      ListenableFuture<V> delegate,
      long time,
      TimeUnit unit,
      ScheduledExecutorService scheduledExecutor) {
    TimeoutFuture<V> result = new TimeoutFuture<>(delegate);
    Fire<V> fire = new Fire<>(result);
    result.timer = scheduledExecutor.schedule(fire, time, unit);
    delegate.addListener(fire, directExecutor());
    return result;
  }

  /*
   * Memory visibility of these fields. There are two cases to consider.
   *
   * 1. visibility of the writes to these fields to Fire.run:
   *
   * The initial write to delegateRef is made definitely visible via the semantics of
   * addListener/SES.schedule. The later racy write in cancel() is not guaranteed to be observed,
   * however that is fine since the correctness is based on the atomic state in our base class. The
   * initial write to timer is never definitely visible to Fire.run since it is assigned after
   * SES.schedule is called. Therefore Fire.run has to check for null. However, it should be visible
   * if Fire.run is called by delegate.addListener since addListener is called after the assignment
   * to timer, and importantly this is the main situation in which we need to be able to see the
   * write.
   *
   * 2. visibility of the writes to an afterDone() call triggered by cancel():
   *
   * Since these fields are non-final that means that TimeoutFuture is not being 'safely published',
   * thus a motivated caller may be able to expose the reference to another thread that would then
   * call cancel() and be unable to cancel the delegate.
   * There are a number of ways to solve this, none of which are very pretty, and it is currently
   * believed to be a purely theoretical problem (since the other actions should supply sufficient
   * write-barriers).
   */

  private @Nullable ListenableFuture<V> delegateRef;
  private @Nullable ScheduledFuture<?> timer;

  private TimeoutFuture(ListenableFuture<V> delegate) {
    this.delegateRef = Preconditions.checkNotNull(delegate);
  }

  /** A runnable that is called when the delegate or the timer completes. */
  private static final class Fire<V> implements Runnable {
    @Nullable TimeoutFuture<V> timeoutFutureRef;

    Fire(TimeoutFuture<V> timeoutFuture) {
      this.timeoutFutureRef = timeoutFuture;
    }

    @Override
    public void run() {
      // If either of these reads return null then we must be after a successful cancel or another
      // call to this method.
      TimeoutFuture<V> timeoutFuture = timeoutFutureRef;
      if (timeoutFuture == null) {
        return;
      }
      ListenableFuture<V> delegate = timeoutFuture.delegateRef;
      if (delegate == null) {
        return;
      }

      /*
       * If we're about to complete the TimeoutFuture, we want to release our reference to it.
       * Otherwise, we'll pin it (and its result) in memory until the timeout task is GCed. (The
       * need to clear our reference to the TimeoutFuture is the reason we use a *static* nested
       * class with a manual reference back to the "containing" class.)
       *
       * This has the nice-ish side effect of limiting reentrancy: run() calls
       * timeoutFuture.setException() calls run(). That reentrancy would already be harmless, since
       * timeoutFuture can be set (and delegate cancelled) only once. (And "set only once" is
       * important for other reasons: run() can still be invoked concurrently in different threads,
       * even with the above null checks.)
       */
      timeoutFutureRef = null;
      if (delegate.isDone()) {
        timeoutFuture.setFuture(delegate);
      } else {
        try {
          ScheduledFuture<?> timer = timeoutFuture.timer;
          timeoutFuture.timer = null; // Don't include already elapsed delay in delegate.toString()
          String message = "Timed out";
          // This try-finally block ensures that we complete the timeout future, even if attempting
          // to produce the message throws (probably StackOverflowError from delegate.toString())
          try {
            if (timer != null) {
              long overDelayMs = Math.abs(timer.getDelay(TimeUnit.MILLISECONDS));
              if (overDelayMs > 10) { // Not all timing drift is worth reporting
                message += " (timeout delayed by " + overDelayMs + " ms after scheduled time)";
              }
            }
            message += ": " + delegate;
          } finally {
            timeoutFuture.setException(new TimeoutFutureException(message));
          }
        } finally {
          delegate.cancel(true);
        }
      }
    }
  }

  private static final class TimeoutFutureException extends TimeoutException {
    private TimeoutFutureException(String message) {
      super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
      setStackTrace(new StackTraceElement[0]);
      return this; // no stack trace, wouldn't be useful anyway
    }
  }

  @Override
  protected String pendingToString() {
    ListenableFuture<? extends V> localInputFuture = delegateRef;
    ScheduledFuture<?> localTimer = timer;
    if (localInputFuture != null) {
      String message = "inputFuture=[" + localInputFuture + "]";
      if (localTimer != null) {
        final long delay = localTimer.getDelay(TimeUnit.MILLISECONDS);
        // Negative delays look confusing in an error message
        if (delay > 0) {
          message += ", remaining delay=[" + delay + " ms]";
        }
      }
      return message;
    }
    return null;
  }

  @Override
  protected void afterDone() {
    maybePropagateCancellationTo(delegateRef);

    Future<?> localTimer = timer;
    // Try to cancel the timer as an optimization.
    // timer may be null if this call to run was by the timer task since there is no happens-before
    // edge between the assignment to timer and an execution of the timer task.
    if (localTimer != null) {
      localTimer.cancel(false);
    }

    delegateRef = null;
    timer = null;
  }
}
