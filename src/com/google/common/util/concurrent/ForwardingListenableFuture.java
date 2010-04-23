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

import java.util.concurrent.Executor;

/**
 * A {@link ForwardingFuture} that also implements {@link ListenableFuture}.
 * Subclasses will have to provide a delegate {@link ListenableFuture} through
 * the {@link #delegate()} method.
 *
 * @param <V> The result type returned by this Future's <tt>get</tt> method
 *
 * @author Shardul Deo
 * @since 4
 */
@Beta
public abstract class ForwardingListenableFuture<V> extends ForwardingFuture<V>
    implements ListenableFuture<V> {

  /** Constructor for use by subclasses. */
  protected ForwardingListenableFuture() {}

  @Override
  protected abstract ListenableFuture<V> delegate();

  @Override
  public void addListener(Runnable listener, Executor exec) {
    delegate().addListener(listener, exec);
  }
}
