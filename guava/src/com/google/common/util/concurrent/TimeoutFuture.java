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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

/**
 * Implementation of {@code Futures#withTimeout}.
 *
 * <p>Future that delegates to another but will finish early (via a {@link TimeoutException} wrapped
 * in an {@link ExecutionException}) if the specified duration expires. The delegate future is
 * interrupted and cancelled if it times out.
 */
@GwtIncompatible
final class TimeoutFuture<V> extends AbstractFuture.TrustedFuture<V> {
  static <V> ListenableFuture<V> create(
      ListenableFuture<V> delegate,
      long time,
      TimeUnit unit,
      ScheduledExecutorService scheduledExecutor) {
    TimeoutFuture<V> result = new TimeoutFuture<V>(delegate);
    TimeoutFuture.Fire<V> fire = new TimeoutFuture.Fire<V>(result);
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
   * 2. visibility of the writes to cancel:
   *
   * Since these fields are non-final that means that TimeoutFuture is not being 'safely published',
   * thus a motivated caller may be able to expose the reference to another thread that would then
   * call cancel() and be unable to cancel the delegate.
   * There are a number of ways to solve this, none of which are very pretty, and it is currently
   * believed to be a purely theoretical problem (since the other actions should supply sufficient
   * write-barriers).
   */

  @Nullable private ListenableFuture<V> delegateRef;
  @Nullable private Future<?> timer;

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
          // TODO(lukes): this stack trace is particularly useless (all it does is point at the
          // scheduledexecutorservice thread), consider eliminating it altogether?
          timeoutFuture.setException(new TimeoutException("Future timed out: " + delegate));
        } finally {
          delegate.cancel(true);
        }
      }
    }
  }

  @Override
  protected void afterDone() {
    maybePropagateCancellation(delegateRef);

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
