/*
 * Copyright (C) 2006 The Guava Authors
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

import com.google.common.annotations.Beta;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * A TimeLimiter implementation which actually does not attempt to limit time
 * at all.  This may be desirable to use in some unit tests.  More importantly,
 * attempting to debug a call which is time-limited would be extremely annoying,
 * so this gives you a time-limiter you can easily swap in for your real
 * time-limiter while you're debugging.
 *
 * @author Kevin Bourrillion
 * @since 1.0
 */
@Beta
public final class FakeTimeLimiter implements TimeLimiter {
  @Override
  public <T> T newProxy(T target, Class<T> interfaceType, long timeoutDuration,
      TimeUnit timeoutUnit) {
    checkNotNull(target);
    checkNotNull(interfaceType);
    checkNotNull(timeoutUnit);
    return target; // ha ha
  }

  @Override
  public <T> T callWithTimeout(Callable<T> callable, long timeoutDuration,
      TimeUnit timeoutUnit, boolean amInterruptible) throws Exception {
    checkNotNull(timeoutUnit);
    return callable.call(); // fooled you
  }
}
