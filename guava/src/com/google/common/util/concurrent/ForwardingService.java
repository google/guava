/*
 * Copyright (C) 2009 The Guava Authors
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
import com.google.common.collect.ForwardingObject;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link Service} that forwards all method calls to another service.
 *
 * @deprecated Instead of using a {@link ForwardingService}, consider using the
 * {@link Service.Listener} functionality to hook into the {@link Service}
 * lifecycle, or if you really do need to provide access to some Service
 * methods, consider just providing the few that you actually need (e.g. just
 * {@link #startAndWait()}) and not implementing Service.
 *
 * @author Chris Nokleberg
 * @since 1.0
 */
@Beta
@Deprecated
public abstract class ForwardingService extends ForwardingObject
    implements Service {

  /** Constructor for use by subclasses. */
  protected ForwardingService() {}

  @Override protected abstract Service delegate();

  @Deprecated
  @Override
  public ListenableFuture<State> start() {
    return delegate().start();
  }

  @Override public State state() {
    return delegate().state();
  }

  @Deprecated
  @Override
  public ListenableFuture<State> stop() {
    return delegate().stop();
  }

  @Deprecated
  @Override
  public State startAndWait() {
    return delegate().startAndWait();
  }

  @Deprecated
  @Override
  public State stopAndWait() {
    return delegate().stopAndWait();
  }

  @Override public boolean isRunning() {
    return delegate().isRunning();
  }

  /**
   * @since 13.0
   */
  @Override public void addListener(Listener listener, Executor executor) {
    delegate().addListener(listener, executor);
  }

  /**
   * @since 14.0
   */
  @Override public Throwable failureCause() {
    return delegate().failureCause();
  }

  /**
   * @since 15.0
   */
  @Override public Service startAsync() {
    delegate().startAsync();
    return this;
  }

  /**
   * @since 15.0
   */
  @Override public Service stopAsync() {
    delegate().stopAsync();
    return this;
  }
  /**
   * @since 15.0
   */
  @Override public void awaitRunning() {
    delegate().awaitRunning();
  }

  /**
   * @since 15.0
   */
  @Override public void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException {
    delegate().awaitRunning(timeout, unit);
  }

  /**
   * @since 15.0
   */
  @Override public void awaitTerminated() {
    delegate().awaitTerminated();
  }

  /**
   * @since 15.0
   */
  @Override public void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException {
    delegate().awaitTerminated(timeout, unit);
  }

  /**
   * A sensible default implementation of {@link #startAndWait()}, in terms of
   * {@link #start}. If you override {@link #start}, you may wish to override
   * {@link #startAndWait()} to forward to this implementation.
   * @since 9.0
   */
  protected State standardStartAndWait() {
    return Futures.getUnchecked(start());
  }

  /**
   * A sensible default implementation of {@link #stopAndWait()}, in terms of
   * {@link #stop}. If you override {@link #stop}, you may wish to override
   * {@link #stopAndWait()} to forward to this implementation.
   * @since 9.0
   */
  protected State standardStopAndWait() {
    return Futures.getUnchecked(stop());
  }
}
