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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.concurrent.Executor;

/**
 * A {@link ListenableFuture} which forwards all its method calls to another future. Subclasses
 * should override one or more methods to modify the behavior of the backing future as desired per
 * the <a href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p>Most subclasses can just use {@link SimpleForwardingListenableFuture}.
 *
 * @author Shardul Deo
 * @since 4.0
 */
@CanIgnoreReturnValue // TODO(cpovirk): Consider being more strict.
@GwtIncompatible
public abstract class ForwardingListenableFuture<V> extends ForwardingFuture<V>
    implements ListenableFuture<V> {

  /** Constructor for use by subclasses. */
  protected ForwardingListenableFuture() {}

  @Override
  protected abstract ListenableFuture<V> delegate();

  @Override
  public void addListener(Runnable listener, Executor exec) {
    delegate().addListener(listener, exec);
  }

  // TODO(cpovirk): Use standard Javadoc form for SimpleForwarding* class and constructor
  /**
   * A simplified version of {@link ForwardingListenableFuture} where subclasses can pass in an
   * already constructed {@link ListenableFuture} as the delegate.
   *
   * @since 9.0
   */
  public abstract static class SimpleForwardingListenableFuture<V>
      extends ForwardingListenableFuture<V> {
    private final ListenableFuture<V> delegate;

    protected SimpleForwardingListenableFuture(ListenableFuture<V> delegate) {
      this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    protected final ListenableFuture<V> delegate() {
      return delegate;
    }
  }
}
