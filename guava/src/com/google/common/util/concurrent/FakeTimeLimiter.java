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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Platform.restoreInterruptIfIsInterruptedException;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;

/**
 * A TimeLimiter implementation which actually does not attempt to limit time at all. This may be
 * desirable to use in some unit tests. More importantly, attempting to debug a call which is
 * time-limited would be extremely annoying, so this gives you a time-limiter you can easily swap in
 * for your real time-limiter while you're debugging.
 *
 * @author Kevin Bourrillion
 * @author Jens Nyman
 * @since 1.0
 */
@J2ktIncompatible
@GwtIncompatible
public final class FakeTimeLimiter implements TimeLimiter {
  /** Creates a new {@link FakeTimeLimiter}. */
  public FakeTimeLimiter() {}

  @CanIgnoreReturnValue // TODO(kak): consider removing this
  @Override
  public <T> T newProxy(
      T target, Class<T> interfaceType, long timeoutDuration, TimeUnit timeoutUnit) {
    checkNotNull(target);
    checkNotNull(interfaceType);
    checkNotNull(timeoutUnit);
    return target; // ha ha
  }

  @CanIgnoreReturnValue // TODO(kak): consider removing this
  @Override
  @ParametricNullness
  public <T extends @Nullable Object> T callWithTimeout(
      Callable<T> callable, long timeoutDuration, TimeUnit timeoutUnit) throws ExecutionException {
    checkNotNull(callable);
    checkNotNull(timeoutUnit);
    try {
      return callable.call();
    } catch (RuntimeException e) {
      throw new UncheckedExecutionException(e);
    } catch (Exception e) {
      restoreInterruptIfIsInterruptedException(e);
      throw new ExecutionException(e);
    } catch (Error e) {
      throw new ExecutionError(e);
    }
  }

  @CanIgnoreReturnValue // TODO(kak): consider removing this
  @Override
  @ParametricNullness
  public <T extends @Nullable Object> T callUninterruptiblyWithTimeout(
      Callable<T> callable, long timeoutDuration, TimeUnit timeoutUnit) throws ExecutionException {
    return callWithTimeout(callable, timeoutDuration, timeoutUnit);
  }

  @Override
  @SuppressWarnings("CatchingUnchecked") // sneaky checked exception
  public void runWithTimeout(Runnable runnable, long timeoutDuration, TimeUnit timeoutUnit) {
    checkNotNull(runnable);
    checkNotNull(timeoutUnit);
    try {
      runnable.run();
    } catch (Exception e) { // sneaky checked exception
      throw new UncheckedExecutionException(e);
    } catch (Error e) {
      throw new ExecutionError(e);
    }
  }

  @Override
  public void runUninterruptiblyWithTimeout(
      Runnable runnable, long timeoutDuration, TimeUnit timeoutUnit) {
    runWithTimeout(runnable, timeoutDuration, timeoutUnit);
  }
}
