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

package com.google.common.cache;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;
import java.util.concurrent.Executor;

/**
 * A collection of common removal listeners.
 *
 * @author Charles Fry
 * @since 10.0
 */
@GwtIncompatible
public final class RemovalListeners {

  private RemovalListeners() {}

  /**
   * Returns a {@code RemovalListener} which processes all eviction notifications using {@code
   * executor}.
   *
   * @param listener the backing listener
   * @param executor the executor with which removal notifications are asynchronously executed
   */
  public static <K, V> RemovalListener<K, V> asynchronous(
      RemovalListener<K, V> listener, Executor executor) {
    checkNotNull(listener);
    checkNotNull(executor);
    return (RemovalNotification<K, V> notification) ->
        executor.execute(() -> listener.onRemoval(notification));
  }
}
