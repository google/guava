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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingObject;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link Future} which forwards all its method calls to another future. Subclasses should
 * override one or more methods to modify the behavior of the backing future as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p>Most subclasses can just use {@link SimpleForwardingFuture}.
 *
 * @author Sven Mawson
 * @since 1.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public abstract class ForwardingFuture<V extends @Nullable Object> extends ForwardingObject
    implements Future<V> {
  /** Constructor for use by subclasses. */
  protected ForwardingFuture() {}

  @Override
  protected abstract Future<? extends V> delegate();

  @Override
  @CanIgnoreReturnValue
  public boolean cancel(boolean mayInterruptIfRunning) {
    return delegate().cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return delegate().isCancelled();
  }

  @Override
  public boolean isDone() {
    return delegate().isDone();
  }

  @Override
  @CanIgnoreReturnValue
  @ParametricNullness
  public V get() throws InterruptedException, ExecutionException {
    return delegate().get();
  }

  @Override
  @CanIgnoreReturnValue
  @ParametricNullness
  public V get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return delegate().get(timeout, unit);
  }

  // TODO(cpovirk): Use standard Javadoc form for SimpleForwarding* class and constructor
  /**
   * A simplified version of {@link ForwardingFuture} where subclasses can pass in an already
   * constructed {@link Future} as the delegate.
   *
   * @since 9.0
   */
  public abstract static class SimpleForwardingFuture<V extends @Nullable Object>
      extends ForwardingFuture<V> {
    private final Future<V> delegate;

    protected SimpleForwardingFuture(Future<V> delegate) {
      this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    protected final Future<V> delegate() {
      return delegate;
    }
  }
}
