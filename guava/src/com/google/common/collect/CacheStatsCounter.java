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

package com.google.common.collect;

import com.google.common.annotations.Beta;

/**
 * Accumulates statistics during the operation of a {@link Cache} for presentation by {@link
 * Cache#stats}. This is solely intended for consumption by {@code Cache} implementors.
 *
 * @author fry@google.com (Charles Fry)
 * @since Guava release 10
 */
@Beta
public interface CacheStatsCounter {
  /**
   * Records a single hit. This should be called when a cache request returns a cached value.
   */
  public void recordHit();

  /**
   * Records a single miss. This should be called when a cache request returns an uncached (newly
   * created) value or null. Multiple concurrent calls to {@link Cache#get} on an absent value
   * should result in multiple calls to this method, despite all being served by the results of a
   * single creation.
   */
  public void recordMiss();

  /**
   * Records the creation of a new value. This should be called when a cache request triggers the
   * creation of a new value. This differs from {@link #recordMiss} in the case of concurrent
   * calls to {@link Cache#get} on an absent value, in which case only a single call to this
   * method should occur. Note that the creating thread should call both {@link #recordCreate} and
   * {@link #recordMiss}.
   *
   * @param createTime the number of nanoseconds the cache spent creating the new value
   */
  public void recordCreate(long createTime);

  /**
   * Records the eviction of an entry from the cache. This should only been called when an entry
   * is evicted due to the cache's eviction strategy, and not as a result of manual {@linkplain
   * Cache#invalidate invalidations}.
   */
  public void recordEviction();

  /**
   * Returns a snapshot of this counter's values. Note that this may be an inconsistent view, as
   * it may be interleaved with update operations.
   */
  public CacheStats snapshot();
}
