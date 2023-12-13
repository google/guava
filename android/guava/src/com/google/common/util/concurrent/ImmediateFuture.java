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

import com.google.common.annotations.GwtCompatible;
import com.google.common.util.concurrent.AbstractFuture.TrustedFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Implementation of {@link Futures#immediateFuture}. */
@GwtCompatible
@ElementTypesAreNonnullByDefault
// TODO(cpovirk): Make this final (but that may break Mockito spy calls).
class ImmediateFuture<V extends @Nullable Object> implements ListenableFuture<V> {
  static final ListenableFuture<?> NULL = new ImmediateFuture<@Nullable Object>(null);

  private static final LazyLogger log = new LazyLogger(ImmediateFuture.class);

  @ParametricNullness private final V value;

  ImmediateFuture(@ParametricNullness V value) {
    this.value = value;
  }

  @Override
  @SuppressWarnings("CatchingUnchecked") // sneaky checked exception
  public void addListener(Runnable listener, Executor executor) {
    checkNotNull(listener, "Runnable was null.");
    checkNotNull(executor, "Executor was null.");
    try {
      executor.execute(listener);
    } catch (Exception e) { // sneaky checked exception
      // ListenableFuture's contract is that it will not throw unchecked exceptions, so log the bad
      // runnable and/or executor and swallow it.
      log.get()
          .log(
              Level.SEVERE,
              "RuntimeException while executing runnable "
                  + listener
                  + " with executor "
                  + executor,
              e);
    }
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  // TODO(lukes): Consider throwing InterruptedException when appropriate.
  @Override
  @ParametricNullness
  public V get() {
    return value;
  }

  @Override
  @ParametricNullness
  public V get(long timeout, TimeUnit unit) throws ExecutionException {
    checkNotNull(unit);
    return get();
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public String toString() {
    // Behaviour analogous to AbstractFuture#toString().
    return super.toString() + "[status=SUCCESS, result=[" + value + "]]";
  }

  static final class ImmediateFailedFuture<V extends @Nullable Object> extends TrustedFuture<V> {
    ImmediateFailedFuture(Throwable thrown) {
      setException(thrown);
    }
  }

  static final class ImmediateCancelledFuture<V extends @Nullable Object> extends TrustedFuture<V> {
    @CheckForNull
    static final ImmediateCancelledFuture<Object> INSTANCE =
        AbstractFuture.GENERATE_CANCELLATION_CAUSES ? null : new ImmediateCancelledFuture<>();

    ImmediateCancelledFuture() {
      cancel(false);
    }
  }
}
