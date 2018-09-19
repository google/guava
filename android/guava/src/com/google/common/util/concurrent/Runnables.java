/*
 * Copyright (C) 2013 The Guava Authors
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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/**
 * Static utility methods pertaining to the {@link Runnable} interface.
 *
 * @since 16.0
 */
@Beta
@GwtCompatible
public final class Runnables {

  private static final Runnable EMPTY_RUNNABLE =
      new Runnable() {
        @Override
        public void run() {}
      };

  /** Returns a {@link Runnable} instance that does nothing when run. */
  public static Runnable doNothing() {
    return EMPTY_RUNNABLE;
  }

  /** Returns a thread-safe {@link Runnable} that invokes the delegate only once */
  public static Runnable runOnce(Runnable delegate) {
    if (delegate instanceof OnceRunnable) {
      return delegate;
    }
    return new OnceRunnable(checkNotNull(delegate));
  }

  static class OnceRunnable implements Runnable {
    Runnable delegate;
    Object initLock = new Object();

    OnceRunnable(Runnable delegate) {
      this.delegate = delegate;
    }

    @Override
    public void run() {
      // Double-checked locking to improve performance and avoid redundant locking after the single invocation
      Object lockState = this.initLock;
      if (lockState != null) {
        synchronized (lockState) {
          if (this.initLock != null) {
            delegate.run();
            // release, not needed anymore
            this.initLock = null;
            this.delegate = null;
          }
        }
      }
    }
  }

  private Runnables() {}
}
