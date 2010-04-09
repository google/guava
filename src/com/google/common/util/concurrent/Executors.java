/*
 * Copyright (C) 2007 Google Inc.
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Old location of {@link MoreExecutors}.
 *
 * @deprecated use {@link MoreExecutors}
 */
@Beta // TODO: delete after Guava release 3
@Deprecated
public final class Executors {
  private Executors() {}

  /**
   * Old location of {@link MoreExecutors#getExitingExecutorService(
   * ThreadPoolExecutor, long, TimeUnit)}.
   *
   * @deprecated use {@link MoreExecutors#getExitingExecutorService(
   * ThreadPoolExecutor, long, TimeUnit)}.
   */
  @Deprecated public static ExecutorService getExitingExecutorService(
      ThreadPoolExecutor executor, long terminationTimeout, TimeUnit timeUnit) {
    return MoreExecutors.getExitingExecutorService(
        executor, terminationTimeout, timeUnit);
  }

  /**
   * Old location of {@link MoreExecutors#getExitingScheduledExecutorService(
   * ScheduledThreadPoolExecutor, long, TimeUnit)}.
   *
   * @deprecated use {@link MoreExecutors#getExitingScheduledExecutorService(
   * ScheduledThreadPoolExecutor, long, TimeUnit)}.
   */
  @Deprecated
  public static ScheduledExecutorService getExitingScheduledExecutorService(
      ScheduledThreadPoolExecutor executor, long terminationTimeout,
      TimeUnit timeUnit) {
    return MoreExecutors.getExitingScheduledExecutorService(
        executor, terminationTimeout, timeUnit);
  }

  /**
   * Old location of {@link MoreExecutors#addDelayedShutdownHook(
   * ExecutorService, long, TimeUnit)}.
   *
   * @deprecated use {@link MoreExecutors#addDelayedShutdownHook(
   * ExecutorService, long, TimeUnit)}.
   */
  @Deprecated public static void addDelayedShutdownHook(
      final ExecutorService service, final long terminationTimeout,
      final TimeUnit timeUnit) {
    MoreExecutors.addDelayedShutdownHook(service, terminationTimeout, timeUnit);
  }

  /**
   * Old location of {@link MoreExecutors#getExitingExecutorService(
   * ThreadPoolExecutor)}.
   *
   * @deprecated use {@link MoreExecutors#getExitingExecutorService(
   * ThreadPoolExecutor)}.
   */
  @Deprecated public static ExecutorService getExitingExecutorService(
      ThreadPoolExecutor executor) {
    return MoreExecutors.getExitingExecutorService(executor);
  }

  /**
   * Old location of {@link MoreExecutors#getExitingScheduledExecutorService(
   * ScheduledThreadPoolExecutor)}.
   *
   * @deprecated use {@link MoreExecutors#getExitingScheduledExecutorService(
   * ScheduledThreadPoolExecutor)}.
   */
  @Deprecated
  public static ScheduledExecutorService getExitingScheduledExecutorService(
      ScheduledThreadPoolExecutor executor) {
    return MoreExecutors.getExitingScheduledExecutorService(executor);
  }

  /**
   * Old location of {@link MoreExecutors#daemonThreadFactory()}.
   *
   * @deprecated use {@link MoreExecutors#daemonThreadFactory()}.
   */
  @Deprecated public static ThreadFactory daemonThreadFactory() {
    return MoreExecutors.daemonThreadFactory();
  }

  /**
   * Old location of {@link MoreExecutors#daemonThreadFactory(ThreadFactory)}.
   *
   * @deprecated use {@link MoreExecutors#daemonThreadFactory(ThreadFactory)}.
   */
  @Deprecated
  public static ThreadFactory daemonThreadFactory(ThreadFactory factory) {
    return MoreExecutors.daemonThreadFactory(factory);
  }

  /**
   * Old location of {@link MoreExecutors#sameThreadExecutor()}.
   *
   * @deprecated use {@link MoreExecutors#sameThreadExecutor()}.
   */
  @Deprecated public static ExecutorService sameThreadExecutor() {
    return MoreExecutors.sameThreadExecutor();
  }
}
