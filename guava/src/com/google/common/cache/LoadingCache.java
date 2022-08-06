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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * A semi-persistent mapping from keys to values. Values are automatically loaded by the cache, and
 * are stored in the cache until either evicted or manually invalidated. The common way to build
 * instances is using {@link CacheBuilder}.
 *
 * <p>Implementations of this interface are expected to be thread-safe, and can be safely accessed
 * by multiple concurrent threads.
 *
 * <p>When evaluated as a {@link Function}, a cache yields the same result as invoking {@link
 * #getUnchecked}.
 *
 * @param <K> the type of the cache's keys, which are not permitted to be null
 * @param <V> the type of the cache's values, which are not permitted to be null
 * @author Charles Fry
 * @since 11.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public interface LoadingCache<K, V> extends Cache<K, V>, Function<K, V> {

  /**
   * Returns the value associated with {@code key} in this cache, first loading that value if
   * necessary. No observable state associated with this cache is modified until loading completes.
   *
   * <p>If another call to {@link #get} or {@link #getUnchecked} is currently loading the value for
   * {@code key}, simply waits for that thread to finish and returns its loaded value. Note that
   * multiple threads can concurrently load values for distinct keys.
   *
   * <p>Caches loaded by a {@link CacheLoader} will call {@link CacheLoader#load} to load new values
   * into the cache. Newly loaded values are added to the cache using {@code
   * Cache.asMap().putIfAbsent} after loading has completed; if another value was associated with
   * {@code key} while the new value was loading then a removal notification will be sent for the
   * new value.
   *
   * <p>If the cache loader associated with this cache is known not to throw checked exceptions,
   * then prefer {@link #getUnchecked} over this method.
   *
   * @throws ExecutionException if a checked exception was thrown while loading the value. ({@code
   *     ExecutionException} is thrown <a
   *     href="https://github.com/google/guava/wiki/CachesExplained#interruption">even if
   *     computation was interrupted by an {@code InterruptedException}</a>.)
   * @throws UncheckedExecutionException if an unchecked exception was thrown while loading the
   *     value
   * @throws ExecutionError if an error was thrown while loading the value
   */
  @CanIgnoreReturnValue // TODO(b/27479612): consider removing this?
  V get(K key) throws ExecutionException;

  /**
   * Returns the value associated with {@code key} in this cache, first loading that value if
   * necessary. No observable state associated with this cache is modified until loading completes.
   * Unlike {@link #get}, this method does not throw a checked exception, and thus should only be
   * used in situations where checked exceptions are not thrown by the cache loader.
   *
   * <p>If another call to {@link #get} or {@link #getUnchecked} is currently loading the value for
   * {@code key}, simply waits for that thread to finish and returns its loaded value. Note that
   * multiple threads can concurrently load values for distinct keys.
   *
   * <p>Caches loaded by a {@link CacheLoader} will call {@link CacheLoader#load} to load new values
   * into the cache. Newly loaded values are added to the cache using {@code
   * Cache.asMap().putIfAbsent} after loading has completed; if another value was associated with
   * {@code key} while the new value was loading then a removal notification will be sent for the
   * new value.
   *
   * <p><b>Warning:</b> this method silently converts checked exceptions to unchecked exceptions,
   * and should not be used with cache loaders which throw checked exceptions. In such cases use
   * {@link #get} instead.
   *
   * @throws UncheckedExecutionException if an exception was thrown while loading the value. (As
   *     explained in the last paragraph above, this should be an unchecked exception only.)
   * @throws ExecutionError if an error was thrown while loading the value
   */
  @CanIgnoreReturnValue // TODO(b/27479612): consider removing this?
  V getUnchecked(K key);

  /**
   * Returns a map of the values associated with {@code keys}, creating or retrieving those values
   * if necessary. The returned map contains entries that were already cached, combined with newly
   * loaded entries; it will never contain null keys or values.
   *
   * <p>Caches loaded by a {@link CacheLoader} will issue a single request to {@link
   * CacheLoader#loadAll} for all keys which are not already present in the cache. All entries
   * returned by {@link CacheLoader#loadAll} will be stored in the cache, over-writing any
   * previously cached values. This method will throw an exception if {@link CacheLoader#loadAll}
   * returns {@code null}, returns a map containing null keys or values, or fails to return an entry
   * for each requested key.
   *
   * <p>Note that duplicate elements in {@code keys}, as determined by {@link Object#equals}, will
   * be ignored.
   *
   * @throws ExecutionException if a checked exception was thrown while loading the value. ({@code
   *     ExecutionException} is thrown <a
   *     href="https://github.com/google/guava/wiki/CachesExplained#interruption">even if
   *     computation was interrupted by an {@code InterruptedException}</a>.)
   * @throws UncheckedExecutionException if an unchecked exception was thrown while loading the
   *     values
   * @throws ExecutionError if an error was thrown while loading the values
   * @since 11.0
   */
  @CanIgnoreReturnValue // TODO(b/27479612): consider removing this
  ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException;

  /**
   * @deprecated Provided to satisfy the {@code Function} interface; use {@link #get} or {@link
   *     #getUnchecked} instead.
   * @throws UncheckedExecutionException if an exception was thrown while loading the value. (As
   *     described in the documentation for {@link #getUnchecked}, {@code LoadingCache} should be
   *     used as a {@code Function} only with cache loaders that throw only unchecked exceptions.)
   */
  @Deprecated
  @Override
  V apply(K key);

  /**
   * Loads a new value for {@code key}, possibly asynchronously. While the new value is loading the
   * previous value (if any) will continue to be returned by {@code get(key)} unless it is evicted.
   * If the new value is loaded successfully it will replace the previous value in the cache; if an
   * exception is thrown while refreshing the previous value will remain, <i>and the exception will
   * be logged (using {@link java.util.logging.Logger}) and swallowed</i>.
   *
   * <p>Caches loaded by a {@link CacheLoader} will call {@link CacheLoader#reload} if the cache
   * currently contains a value for {@code key}, and {@link CacheLoader#load} otherwise. Loading is
   * asynchronous only if {@link CacheLoader#reload} was overridden with an asynchronous
   * implementation.
   *
   * <p>Returns without doing anything if another thread is currently loading the value for {@code
   * key}. If the cache loader associated with this cache performs refresh asynchronously then this
   * method may return before refresh completes.
   *
   * @since 11.0
   */
  void refresh(K key);

  /**
   * {@inheritDoc}
   *
   * <p><b>Note that although the view <i>is</i> modifiable, no method on the returned map will ever
   * cause entries to be automatically loaded.</b>
   */
  @Override
  ConcurrentMap<K, V> asMap();
}
