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

import javax.annotation.Nullable;

/**
 * An object that can receive a notification when an entry is evicted from a
 * map.
 *
 * <p>An instance may be called concurrently by multiple threads to process
 * different entries. Implementations of this interface should avoid performing
 * blocking calls or synchronizing on shared resources.
 *
 * @param <K> the type of keys being evicted
 * @param <V> the type of values being evicted
 * @author Ben Manes
 * @since 7.0
 * @deprecated use {@link MapMaker.RemovalListener} <b>This class is scheduled
 *     for deletion from Guava in Guava release 11.0.</b>
 */
@Beta
@Deprecated
public
interface MapEvictionListener<K, V> {

  /**
   * Notifies the listener that an eviction has occurred. Eviction may be for
   * reasons such as timed expiration, exceeding a maximum size, or due to
   * garbage collection. Eviction notification does <i>not</i> occur due to
   * manual removal.
   *
   * @param key the key of the entry that has already been evicted, or {@code
   *     null} if its reference was collected
   * @param value the value of the entry that has already been evicted, or
   *     {@code null} if its reference was collected
   */
  void onEviction(@Nullable K key, @Nullable V value);
}
