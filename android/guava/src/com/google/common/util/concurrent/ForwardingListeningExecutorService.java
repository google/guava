/*
 * Copyright (C) 2011 The Guava Authors
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
import com.google.common.annotations.J2ktIncompatible;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

/**
 * A listening executor service which forwards all its method calls to another listening executor
 * service. Subclasses should override one or more methods to modify the behavior of the backing
 * executor service as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>{@code default} method warning:</b> This class does <i>not</i> forward calls to {@code
 * default} methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the {@code ForwardingListeningExecutorService}.
 *
 * @author Isaac Shum
 * @since 10.0
 */
@J2ktIncompatible
@GwtIncompatible
public abstract class ForwardingListeningExecutorService extends ForwardingExecutorService
    implements ListeningExecutorService {
  /** Constructor for use by subclasses. */
  protected ForwardingListeningExecutorService() {}

  @Override
  protected abstract ListeningExecutorService delegate();

  @Override
  public <T extends @Nullable Object> ListenableFuture<T> submit(Callable<T> task) {
    return delegate().submit(task);
  }

  @Override
  public ListenableFuture<?> submit(Runnable task) {
    return delegate().submit(task);
  }

  @Override
  public <T extends @Nullable Object> ListenableFuture<T> submit(
      Runnable task, @ParametricNullness T result) {
    return delegate().submit(task, result);
  }
}
