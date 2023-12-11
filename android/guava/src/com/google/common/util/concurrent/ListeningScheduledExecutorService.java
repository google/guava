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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link ScheduledExecutorService} that returns {@link ListenableFuture} instances from its
 * {@code ExecutorService} methods. To create an instance from an existing {@link
 * ScheduledExecutorService}, call {@link
 * MoreExecutors#listeningDecorator(ScheduledExecutorService)}.
 *
 * @author Chris Povirk
 * @since 10.0
 */
@J2ktIncompatible
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public interface ListeningScheduledExecutorService
    extends ScheduledExecutorService, ListeningExecutorService {

  /** @since 15.0 (previously returned ScheduledFuture) */
  @Override
  ListenableScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

  /** @since 15.0 (previously returned ScheduledFuture) */
  @Override
  <V extends @Nullable Object> ListenableScheduledFuture<V> schedule(
      Callable<V> callable, long delay, TimeUnit unit);

  /** @since 15.0 (previously returned ScheduledFuture) */
  @Override
  ListenableScheduledFuture<?> scheduleAtFixedRate(
      Runnable command, long initialDelay, long period, TimeUnit unit);

  /** @since 15.0 (previously returned ScheduledFuture) */
  @Override
  ListenableScheduledFuture<?> scheduleWithFixedDelay(
      Runnable command, long initialDelay, long delay, TimeUnit unit);
}
