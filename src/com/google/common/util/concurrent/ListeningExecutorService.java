// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * An {@link ExecutorService} that returns {@link ListenableFuture} instances.
 * To create an instance from an existing {@link ExecutorService}, call
 * {@link MoreExecutors#listeningDecorator(ExecutorService)}.
 *
 * @author Chris Povirk
 * @since 8
 */
@Beta
public interface ListeningExecutorService extends ExecutorService {
  /**
   * @return a {@code ListenableFuture} representing pending completion of the
   *         task
   */
  // @Override
  <T> ListenableFuture<T> submit(Callable<T> task);

  /**
   * @return a {@code ListenableFuture} representing pending completion of the
   *         task
   */
  // @Override
  ListenableFuture<?> submit(Runnable task);

  /**
   * @return a {@code ListenableFuture} representing pending completion of the
   *         task
   */
  // @Override
  <T> ListenableFuture<T> submit(Runnable task, T result);

  /**
   * {@inheritDoc}
   *
   * <p>
   * Though the return type does not express this, all elements in the returned
   * list must be {@link ListenableFuture} instances.
   *
   * @return A list of {@code ListenableFuture} instances representing the
   *         tasks, in the same sequential order as produced by the iterator for
   *         the given task list, each of which has completed.
   */
  // @Override
  <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException;

  /**
   * {@inheritDoc}
   *
   * <p>
   * Though the return type does not express this, all elements in the returned
   * list must be {@link ListenableFuture} instances.
   *
   * @return a list of {@code ListenableFuture} instances representing the
   *         tasks, in the same sequential order as produced by the iterator for
   *         the given task list. If the operation did not time out, each task
   *         will have completed. If it did time out, some of these tasks will
   *         not have completed.
   */
  // @Override
  <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException;
}
