/*
 * Copyright (C) 2009 Google Inc.
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
import com.google.common.base.Throwables;
import com.google.common.collect.ForwardingObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A {@link Service} that forwards all method calls to another service.
 *
 * @author Chris Nokleberg
 * @since 1
 */
@Beta
public abstract class ForwardingService extends ForwardingObject
    implements Service {

  /** Constructor for use by subclasses. */
  protected ForwardingService() {}

  @Override protected abstract Service delegate();

  @Override public Future<State> start() {
    return delegate().start();
  }

  @Override public State state() {
    return delegate().state();
  }

  @Override public Future<State> stop() {
    return delegate().stop();
  }

  @Override public State startAndWait() {
    return delegate().startAndWait();
  }

  @Override public State stopAndWait() {
    return delegate().stopAndWait();
  }

  @Override public boolean isRunning() {
    return delegate().isRunning();
  }

  /**
   * A sensible default implementation of {@link #startAndWait()}, in terms of
   * {@link #start}. If you override {@link #start}, you may wish to override
   * {@link #startAndWait()} to forward to this implementation.
   */
  protected State standardStartAndWait() {
    try {
      return Futures.makeUninterruptible(start()).get();
    } catch (ExecutionException e) {
      throw Throwables.propagate(e.getCause());
    }
  }

  /**
   * A sensible default implementation of {@link #stopAndWait()}, in terms of
   * {@link #stop}. If you override {@link #stop}, you may wish to override
   * {@link #stopAndWait()} to forward to this implementation.
   */
  protected State standardStopAndWait() {
    try {
      return Futures.makeUninterruptible(stop()).get();
    } catch (ExecutionException e) {
      throw Throwables.propagate(e.getCause());
    }
  }
}
