/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.collect;

import com.google.common.annotations.Beta;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;

/**
 * A collection of common eviction listeners.
 *
 * @author Charles Fry
 * @since 7.0
 * @deprecated Caching functionality in {@code MapMaker} is being moved to
 *     {@link com.google.common.cache.CacheBuilder}. Functionality similar to
 *     {@code EvictionListeners} is provided by {@link
 *     com.google.common.cache.RemovalListeners}. <b>This class is scheduled for
 *     deletion from Guava in Guava release 11.0.</b>
 */
@Beta
@Deprecated
public
final class EvictionListeners {

  private EvictionListeners() {}

  /**
   * Returns an asynchronous {@code MapEvictionListener} which processes all
   * eviction notifications asynchronously, using {@code executor}.
   *
   * @param listener the backing listener
   * @param executor the executor with which eviciton notifications are
   *     asynchronously executed
   * @deprecated Caching functionality in {@code MapMaker} is being moved to
   *     {@link com.google.common.cache.CacheBuilder}. Functionality similar to
   *     {@code EvictionListeners#asynchronous} is provided by
   *     {@link com.google.common.cache.RemovalListeners#asynchronous}.
   *     <b>This method is scheduled for deletion from Guava in Guava release 11.0.</b>
   */
  @Deprecated
  public static <K, V> MapEvictionListener<K, V> asynchronous(
      final MapEvictionListener<K, V> listener, final Executor executor) {
    return new MapEvictionListener<K, V>() {
      @Override
      public void onEviction(@Nullable final K key, @Nullable final V value) {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            listener.onEviction(key, value);
          }
        });
      }
    };
  }

}
