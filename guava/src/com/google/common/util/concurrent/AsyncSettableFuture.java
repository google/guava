/*
 * Copyright (C) 2012 The Guava Authors
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

import javax.annotation.Nullable;

/**
 * A settable future that can be set asynchronously via {@link #setFuture}.
 * A similar effect could be accomplished by adding a listener to the delegate
 * future that sets a normal settable future after the delegate is complete.
 * This approach gains us the ability to keep track of whether a delegate has
 * been set (i.e. so that we can prevent collisions from setting it twice and
 * can know before the computation is done whether it has been set), as well
 * as improved cancellation semantics (i.e. if either future is cancelled,
 * then the other one is too).  This class is thread-safe.
 *
 * @param <V> The result type returned by the Future's {@code get} method.
 *
 * @author Stephen Hicks
 */
final class AsyncSettableFuture<V> extends ForwardingListenableFuture<V> {

  /** Creates a new asynchronously-settable future. */
  public static <V> AsyncSettableFuture<V> create() {
    return new AsyncSettableFuture<V>();
  }

  private final NestedFuture<V> nested = new NestedFuture<V>();
  private final ListenableFuture<V> dereferenced = Futures.dereference(nested);

  private AsyncSettableFuture() {}

  @Override protected ListenableFuture<V> delegate() {
    return dereferenced;
  }

  /**
   * Sets this future to forward to the given future.  Returns {@code true}
   * if the future was able to be set (i.e. it hasn't been set already).
   */
  public boolean setFuture(ListenableFuture<? extends V> future) {
    return nested.setFuture(checkNotNull(future));
  }

  /**
   * Convenience method that calls {@link #setFuture} on a {@link
   * Futures#immediateFuture}.  Returns {@code true} if the future
   * was able to be set (i.e. it hasn't been set already).
   */
  public boolean setValue(@Nullable V value) {
    return setFuture(Futures.immediateFuture(value));
  }

  /**
   * Convenience method that calls {@link #setFuture} on a {@link
   * Futures#immediateFailedFuture}.  Returns {@code true} if the
   * future was able to be set (i.e. it hasn't been set already).
   */
  public boolean setException(Throwable exception) {
    return setFuture(Futures.<V>immediateFailedFuture(exception));
  }

  /**
   * Returns {@code true} if this future has been (possibly asynchronously) set.
   * Note that a {@code false} result in no way gaurantees that a later call
   * to, e.g., {@link #setFuture} will succeed, since another thread could
   * make the call in between.  This is somewhat analogous to {@link #isDone},
   * but since setting and completing are not the same event, it is useful to
   * have this method broken out.
   */
  public boolean isSet() {
    return nested.isDone();
  }

  private static final class NestedFuture<V> extends AbstractFuture<ListenableFuture<? extends V>> {
    boolean setFuture(ListenableFuture<? extends V> value) {
      boolean result = set(value);
      if (isCancelled()) {
        value.cancel(wasInterrupted());
      }
      return result;
    }
  }
}
