/*
 * Copyright (C) 2009 The Guava Authors
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link FluentFuture} that forwards all calls to a delegate.
 *
 * <h3>Extension</h3>
 *
 * If you want a class like {@code FluentFuture} but with extra methods, we recommend declaring your
 * own subclass of {@link ListenableFuture}, complete with a method like {@link #from} to adapt an
 * existing {@code ListenableFuture}, implemented atop a {@link ForwardingListenableFuture} that
 * forwards to that future and adds the desired methods.
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
final class ForwardingFluentFuture<V extends @Nullable Object> extends FluentFuture<V> {
  private final ListenableFuture<V> delegate;

  ForwardingFluentFuture(ListenableFuture<V> delegate) {
    this.delegate = checkNotNull(delegate);
  }

  @Override
  public void addListener(Runnable listener, Executor executor) {
    delegate.addListener(listener, executor);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return delegate.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return delegate.isCancelled();
  }

  @Override
  public boolean isDone() {
    return delegate.isDone();
  }

  @Override
  @ParametricNullness
  public V get() throws InterruptedException, ExecutionException {
    return delegate.get();
  }

  @Override
  @ParametricNullness
  public V get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.get(timeout, unit);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
