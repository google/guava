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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract {@link ListeningExecutorService} implementation that creates {@link ListenableFuture}
 * instances for each {@link Runnable} and {@link Callable} submitted to it. These tasks are run
 * with the abstract {@link #execute execute(Runnable)} method.
 *
 * <p>In addition to {@link #execute}, subclasses must implement all methods related to shutdown and
 * termination.
 *
 * @author Chris Povirk
 * @since 14.0
 */
@CheckReturnValue
@GwtIncompatible
@J2ktIncompatible
@ElementTypesAreNonnullByDefault
public abstract class AbstractListeningExecutorService extends AbstractExecutorService
    implements ListeningExecutorService {

  /**
   * @since 19.0 (present with return type {@code ListenableFutureTask} since 14.0)
   */
  @CanIgnoreReturnValue // TODO(kak): consider removing this
  @Override
  protected final <T extends @Nullable Object> RunnableFuture<T> newTaskFor(
      Runnable runnable, @ParametricNullness T value) {
    return TrustedListenableFutureTask.create(runnable, value);
  }

  /**
   * @since 19.0 (present with return type {@code ListenableFutureTask} since 14.0)
   */
  @CanIgnoreReturnValue // TODO(kak): consider removing this
  @Override
  protected final <T extends @Nullable Object> RunnableFuture<T> newTaskFor(Callable<T> callable) {
    return TrustedListenableFutureTask.create(callable);
  }

  @CanIgnoreReturnValue // TODO(kak): consider removing this
  @Override
  public ListenableFuture<?> submit(Runnable task) {
    return (ListenableFuture<?>) super.submit(task);
  }

  @CanIgnoreReturnValue // TODO(kak): consider removing this
  @Override
  public <T extends @Nullable Object> ListenableFuture<T> submit(
      Runnable task, @ParametricNullness T result) {
    return (ListenableFuture<T>) super.submit(task, result);
  }

  @CanIgnoreReturnValue // TODO(kak): consider removing this
  @Override
  public <T extends @Nullable Object> ListenableFuture<T> submit(Callable<T> task) {
    return (ListenableFuture<T>) super.submit(task);
  }
}
