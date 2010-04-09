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

import com.google.common.collect.ForwardingObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link Future} which forwards all its method calls to another future.
 * Subclasses should override one or more methods to modify the behavior of
 * the backing collection as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * @see ForwardingObject
 * @author Sven Mawson
 * @since 2009.09.15 <b>tentative</b>
 */
public abstract class ForwardingFuture<V> extends ForwardingObject
    implements Future<V> {

  @Override protected abstract Future<V> delegate();

  /*@Override*/
  public boolean cancel(boolean mayInterruptIfRunning) {
    return delegate().cancel(mayInterruptIfRunning);
  }

  /*@Override*/
  public boolean isCancelled() {
    return delegate().isCancelled();
  }

  /*@Override*/
  public boolean isDone() {
    return delegate().isDone();
  }

  /*@Override*/
  public V get() throws InterruptedException, ExecutionException {
    return delegate().get();
  }

  /*@Override*/
  public V get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return delegate().get(timeout, unit);
  }
}
