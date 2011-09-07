/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.annotations.Beta;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import javax.annotation.Nullable;

/**
 * A {@link FutureTask} that also implements the {@link ListenableFuture}
 * interface.  Subclasses must make sure to call {@code super.done()} if they
 * also override the {@link #done()} method, otherwise the listeners will not
 * be called.
 *
 * @author Sven Mawson
 * @since 1.0
 */
public final class ListenableFutureTask<V> extends FutureTask<V>
    implements ListenableFuture<V> {

  // The execution list to hold our listeners.
  private final ExecutionList executionList = new ExecutionList();

  /**
   * Creates a {@code ListenableFutureTask} that will upon running, execute the
   * given {@code Callable}.
   *
   * @param callable the callable task
   * @since 10.0
   */
  public static <V> ListenableFutureTask<V> create(Callable<V> callable) {
    return new ListenableFutureTask<V>(callable);
  }

  /**
   * Creates a {@code ListenableFutureTask} that will upon running, execute the
   * given {@code Runnable}, and arrange that {@code get} will return the
   * given result on successful completion.
   *
   * @param runnable the runnable task
   * @param result the result to return on successful completion. If you don't
   *     need a particular result, consider using constructions of the form:
   *     {@code ListenableFuture<?> f = ListenableFutureTask.create(runnable,
   *     null)}
   * @since 10.0
   */
  public static <V> ListenableFutureTask<V> create(
      Runnable runnable, @Nullable V result) {
    return new ListenableFutureTask<V>(runnable, result);
  }

  /**
   * <b>Deprecated.</b> Use {@link #create(Callable)} instead. This method will be
   * removed from Guava in Guava release 11.0.
   *
   * Creates a {@code ListenableFutureTask} that will upon running, execute the
   * given {@code Callable}.
   *
   * @param callable the callable task
   */
  @Beta @Deprecated public
  ListenableFutureTask(Callable<V> callable) {
    super(callable);
  }

  /**
   * <b>Deprecated. Use {@link #create(Runnable, Object)} instead. This method
   * will be removed from Guava in Guava release 11.0.</b>
   *
   * Creates a {@code ListenableFutureTask} that will upon running, execute the
   * given {@code Runnable}, and arrange that {@code get} will return the
   * given result on successful completion.
   *
   * @param runnable the runnable task
   * @param result the result to return on successful completion. If
   * you don't need a particular result, consider using
   * constructions of the form:
   * {@code ListenableFuture<?> f =
   *     ListenableFutureTask.create(runnable, null)}
   */
  @Beta @Deprecated public
  ListenableFutureTask(Runnable runnable, @Nullable V result) {
    super(runnable, result);
  }

  @Override
  public void addListener(Runnable listener, Executor exec) {
    executionList.add(listener, exec);
  }

  @Override
  protected void done() {
    executionList.execute();
  }
}
