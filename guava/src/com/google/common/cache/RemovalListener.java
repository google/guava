/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.cache;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/**
 * An object that can receive a notification when an entry is removed from a cache. The removal
 * resulting in notification could have occured to an entry being manually removed or replaced, or
 * due to eviction resulting from timed expiration, exceeding a maximum size, or garbage
 * collection.
 *
 * <p>An instance may be called concurrently by multiple threads to process different entries.
 * Implementations of this interface should avoid performing blocking calls or synchronizing on
 * shared resources.
 *
 * @param <K> the most general type of keys this listener can listen for; for
 *     example {@code Object} if any key is acceptable
 * @param <V> the most general type of values this listener can listen for; for
 *     example {@code Object} if any key is acceptable
 * @author Charles Fry
 * @since 10.0
 */
@Beta
@GwtCompatible
public interface RemovalListener<K, V> {
  /**
   * Notifies the listener that a removal occurred at some point in the past.
   *
   * <p>This does not always signify that the key is now absent from the cache,
   * as it may have already been re-added.
   */
  // Technically should accept RemovalNotification<? extends K, ? extends V>, but because
  // RemovalNotification is guaranteed covariant, let's make users' lives simpler.
  void onRemoval(RemovalNotification<K, V> notification);
}
