/*
 * Copyright (C) 2008 The Guava Authors
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

import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link FutureTask} that also implements the {@link ListenableFuture} interface. Unlike {@code
 * FutureTask}, {@code ListenableFutureTask} does not provide an overrideable {@link
 * FutureTask#done() done()} method. For similar functionality, call {@link #addListener}.
 *
 * <p>Few users should use this class. It is intended primarily for those who are implementing an
 * {@code ExecutorService}. Most users should call {@link ListeningExecutorService#submit(Callable)
 * ListeningExecutorService.submit} on a service obtained from {@link
 * MoreExecutors#listeningDecorator}.
 *
 * @author Sven Mawson
 * @since 1.0
 */
@J2ktIncompatible
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public class ListenableFutureTask<V extends @Nullable Object> extends FutureTask<V>
    implements ListenableFuture<V> {
  // TODO(cpovirk): explore ways of making ListenableFutureTask final. There are some valid reasons
  // such as BoundedQueueExecutorService to allow extends but it would be nice to make it final to
  // avoid unintended usage.

  // The execution list to hold our listeners.
  private final ExecutionList executionList = new ExecutionList();

  /**
   * Creates a {@code ListenableFutureTask} that will upon running, execute the given {@code
   * Callable}.
   *
   * @param callable the callable task
   * @since 10.0
   */
  public static <V extends @Nullable Object> ListenableFutureTask<V> create(Callable<V> callable) {
    return new ListenableFutureTask<>(callable);
  }

  /**
   * Creates a {@code ListenableFutureTask} that will upon running, execute the given {@code
   * Runnable}, and arrange that {@code get} will return the given result on successful completion.
   *
   * @param runnable the runnable task
   * @param result the result to return on successful completion. If you don't need a particular
   *     result, consider using constructions of the form: {@code ListenableFuture<?> f =
   *     ListenableFutureTask.create(runnable, null)}
   * @since 10.0
   */
  public static <V extends @Nullable Object> ListenableFutureTask<V> create(
      Runnable runnable, @ParametricNullness V result) {
    return new ListenableFutureTask<>(runnable, result);
  }

  ListenableFutureTask(Callable<V> callable) {
    super(callable);
  }

  ListenableFutureTask(Runnable runnable, @ParametricNullness V result) {
    super(runnable, result);
  }

  @Override
  public void addListener(Runnable listener, Executor exec) {
    executionList.add(listener, exec);
  }

  @CanIgnoreReturnValue
  @Override
  @ParametricNullness
  public V get(long timeout, TimeUnit unit)
      throws TimeoutException, InterruptedException, ExecutionException {

    long timeoutNanos = unit.toNanos(timeout);
    if (timeoutNanos <= OverflowAvoidingLockSupport.MAX_NANOSECONDS_THRESHOLD) {
      return super.get(timeout, unit);
    }
    // Waiting 68 years should be enough for any program.
    return super.get(
        min(timeoutNanos, OverflowAvoidingLockSupport.MAX_NANOSECONDS_THRESHOLD), NANOSECONDS);
  }

  /** Internal implementation detail used to invoke the listeners. */
  @Override
  protected void done() {
    executionList.execute();
  }
}
