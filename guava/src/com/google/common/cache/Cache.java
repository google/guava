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
import com.google.common.base.Function;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * A semi-persistent mapping from keys to values. Values are automatically loaded by the cache,
 * and are stored in the cache until either evicted or manually invalidated.
 *
 * <p>All methods other than {@link #get} and {@link #getUnchecked} are optional.
 *
 * <p>When evaluated as a {@link Function}, a cache yields the same result as invoking {@link
 * #getUnchecked}.
 *
 * @author Charles Fry
 * @since 10.0
 */
@Beta
public interface Cache<K, V> extends Function<K, V> {

  /**
   * Returns the value associated with the given key, creating or retrieving that value if
   * necessary, and throwing an execution exception on failure. No state associated with this cache
   * is modified until loading completes. Note that this method will never return {@code null}.
   *
   * @throws ExecutionException if a checked exception was thrown while loading the response
   * @throws UncheckedExecutionException if an unchecked exception was thrown while loading the
   *     response
   * @throws ExecutionError if an error was thrown while loading the response
   */
  V get(K key) throws ExecutionException;

  /**
   * Returns the value associated with the given key, loading that value if necessary. No state
   * associated with this cache is modified until computation completes. Unlike {@link #get}, this
   * method does not throw a checked exception, and thus should only be used in situations where
   * checked exceptions are not thrown by the cache loader. Note that this method will never return
   * {@code null}.
   *
   * <p><b>Warning:</b> this method silently converts checked exceptions to unchecked exceptions.
   * The {@link #get} method should be preferred for cache loaders which throw checked exceptions.
   *
   * @throws UncheckedExecutionException if an exception was thrown while loading the response,
   *     regardless of whether the exception was checked or unchecked
   * @throws ExecutionError if an error was thrown while loading the response
   */
  V getUnchecked(K key);

  /**
   * Discouraged. Provided to satisfy the {@code Function} interface; use {@link #get} or
   * {@link #getUnchecked} instead.
   *
   * @throws UncheckedExecutionException if an exception was thrown while loading the response,
   *     regardless of whether the exception was checked or unchecked
   * @throws ExecutionError if an error was thrown while loading the response
   */
  @Override
  V apply(K key);

  // TODO(fry): add bulk operations

  /**
   * Discards any cached value for key {@code key}, possibly asynchronously, so that a future
   * invocation of {@code get(key)} will result in a cache miss and reload.
   *
   * @throws UnsupportedOperationException if this operation is not supported by the cache
   *     implementation
   */
  void invalidate(Object key);

  /**
   * Discards all entries in the cache, possibly asynchronously.
   *
   * @throws UnsupportedOperationException if this operation is not supported by the cache
   *     implementation
   */
  void invalidateAll();

  /**
   * Returns the approximate number of entries in this cache.
   *
   * @throws UnsupportedOperationException if this operation is not supported by the cache
   *     implementation
   */
  long size();

  /**
   * Returns a current snapshot of this cache's cumulative statistics. All stats are initialized
   * to zero, and are monotonically increasing over the lifetime of the cache.
   *
   * @throws UnsupportedOperationException if this operation is not supported by the cache
   *     implementation
   */
  CacheStats stats();

  /**
   * Returns a view of the entries stored in this cache as a thread-safe map. Assume that none of
   * the returned map's optional operations will be implemented, unless otherwise specified.
   *
   * <p>Operations on the returned map will never cause new values to be loaded into the cache. So,
   * unlike {@link #get} and {@link #getUnchecked}, this map's {@link Map#get get} method will
   * always return {@code null} for a key that is not already cached.
   *
   * @throws UnsupportedOperationException if this operation is not supported by the cache
   *     implementation
   */
  ConcurrentMap<K, V> asMap();

  /**
   * Performs any pending maintenance operations needed by the cache. Exactly which activities are
   * performed -- if any -- is implementation-dependent.
   */
  void cleanUp();
}
