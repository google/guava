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
import com.google.common.base.Function;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

/**
 * A semi-persistent mapping from keys to values. Values are automatically created by the cache as
 * a function of the keys, and are stored in the cache until either evicted or manually invalidated.
 *
 * <p>All methods other than {@link #get} are optional.
 *
 * <p>When evaluated as a {@link Function}, a cache yields the same result as
 * invoking {@link #get}.
 *
 * @author Charles Fry
 * @since Guava release 10
 */
@Beta
public interface Cache<K, V> extends Function<K, V> {

  /**
   * Returns the value associated with the given key, creating or retrieving that value if
   * necessary. The implementation may support null as a valid cached value, or may return null
   * without caching it, or may not permit null results at all.
   *
   * <p>This method is identical to {@link #getChecked} except that exceptions which occur during
   * cache loading are unchecked.
   *
   * @throws NullPointerException if the specified key is null and this cache does not permit null
   *     keys (optional)
   * @throws ComputationException wraps errors which occur while loading the response
   */
  @Nullable V get(@Nullable K key);

  /**
   * Returns the value associated with the given key, creating or retrieving that value if
   * necessary. The implementation may support null as a valid cached value, or may return null
   * without caching it, or may not permit null results at all.
   *
   * <p>This method is identical to {@link #get} except that exceptions which occur during cache
   * loading are checked.
   *
   * @throws NullPointerException if the specified key is null and this cache does not permit null
   *     keys (optional)
   * @throws ExecutionException wraps errors which occur while loading the response
   */
  @Nullable V getChecked(@Nullable K key) throws ExecutionException;

  /**
   * Provided to satisfy the {@code Function} interface; use {@link #get} instead.
   *
   * @deprecated Use {@link #get} instead.
   */
  @Deprecated
  @Override
  @Nullable V apply(@Nullable K key);

  // TODO(user): add bulk operations

  /**
   * Discards the cached value for key {@code key}, if it exists, so that the next invocation of
   * {@code get(key)} will result in a cache miss and re-creation.
   *
   * @throws UnsupportedOperationException if this operation is not supported by the cache
   *     implementation
   * @throws NullPointerException if the specified key is null and this cache does not permit null
   *     keys (optional)
   */
  void invalidate(@Nullable Object key);

  /**
   * Returns the approximate number of entries in this cache. If the cache contains more than {@code
   * Integer.MAX_VALUE} elements, returns {@code Integer.MAX_VALUE}.
   *
   * @throws UnsupportedOperationException if this operation is not supported by the cache
   *     implementation
   */
  int size();

  /**
   * Returns a current snapshot of this cache's cumulative statistics. All stats are initialized
   * to zero, and are monotonically increasing over the lifetime of the cache.
   *
   * @throws UnsupportedOperationException if this operation is not supported by the cache
   *     implementation
   */
  CacheStats stats();

  /**
   * Returns a list of immutable copies of this cache's most active entries, approximately ordered
   * from least likely to be evicted to most likely to be evicted.
   *
   * @param limit the maximum number of entries to return
   * @throws UnsupportedOperationException if this operation is not supported by the cache
   *     implementation
   */
  ImmutableList<Map.Entry<K, V>> activeEntries(int limit);

  /**
   * Returns a view of the entries stored in this cache as a thread-safe map. Assume that none of
   * the returned map's optional operations will be implemented, unless specified otherwise.
   *
   * <p>Note that the view's {@code get} method will return {@code null} if an entry is not found
   * in the cache for a given key. Unlike {@link Cache#get} it will never attempt to create a
   * missing value.
   *
   * @throws UnsupportedOperationException if this operation is not supported by the cache
   *     implementation
   */
  ConcurrentMap<K, V> asMap();
}
