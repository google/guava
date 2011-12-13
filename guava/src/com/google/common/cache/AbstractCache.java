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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class provides a skeletal implementation of the {@code Cache} interface to minimize the
 * effort required to implement this interface.
 *
 * <p>To implement a cache, the programmer needs only to extend this class and provide an
 * implementation for the {@link #getIfPresent} method. {@link #getAllPresent} is implemented in
 * terms of {@code getIfPresent}; {@link #invalidateAll(Iterable)} is implemented in terms of
 * {@link #invalidate}. The method {@link #cleanUp} is a no-op. All other methods throw an
 * {@link UnsupportedOperationException}.
 *
 * @author Charles Fry
 * @since 10.0
 */
@Beta
@GwtCompatible
public abstract class AbstractCache<K, V> implements Cache<K, V> {

  /** Constructor for use by subclasses. */
  protected AbstractCache() {}

  /**
   * @since 11.0
   */
  @Override
  public V get(K key, Callable<? extends V> valueLoader) throws ExecutionException {
    throw new UnsupportedOperationException();
  }

  /**
   * @since 11.0
   */
  @Override
  public ImmutableMap<K, V> getAllPresent(Iterable<? extends K> keys) {
    Map<K, V> result = Maps.newLinkedHashMap();
    for (K key : keys) {
      if (!result.containsKey(key)) {
        result.put(key, getIfPresent(key));
      }
    }
    return ImmutableMap.copyOf(result);
  }

  /**
   * @since 11.0
   */
  @Override
  public void put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void cleanUp() {}

  @Override
  public long size() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invalidate(Object key) {
    throw new UnsupportedOperationException();
  }

  /**
   * @since 11.0
   */
  @Override
  public void invalidateAll(Iterable<?> keys) {
    for (Object key : keys) {
      invalidate(key);
    }
  }

  @Override
  public void invalidateAll() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CacheStats stats() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ConcurrentMap<K, V> asMap() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  public V getUnchecked(K key) {
    try {
      return get(key);
    } catch (ExecutionException e) {
      throw new UncheckedExecutionException(e.getCause());
    }
  }

  @Deprecated
  @Override
  public V apply(K key) {
    return getUnchecked(key);
  }

  /**
   * Accumulates statistics during the operation of a {@link Cache} for presentation by {@link
   * Cache#stats}. This is solely intended for consumption by {@code Cache} implementors.
   *
   * @since 10.0
   */
  @Beta
  public interface StatsCounter {
    /**
     * Records cache hits. This should be called when a cache request returns a cached value.
     *
     * @param count the number of hits to record
     * @since 11.0
     */
    public void recordHits(int count);

    /**
     * Records cache misses. This should be called when a cache request returns a value that was
     * not found in the cache. This method should be called by the loading thread, as well as by
     * threads blocking on the load. Multiple concurrent calls to {@link Cache} lookup methods with
     * the same key on an absent value should result in a single call to either
     * {@code recordLoadSuccess} or {@code recordLoadException} and multiple calls to this method,
     * despite all being served by the results of a single load operation.
     *
     * @param count the number of misses to record
     * @since 11.0
     */
    public void recordMisses(int count);

    /**
     * Records the successful load of a new entry. This should be called when a cache request
     * causes an entry to be loaded, and the loading completes successfully. In contrast to
     * {@link #recordConcurrentMiss}, this method should only be called by the loading thread.
     *
     * @param loadTime the number of nanoseconds the cache spent computing or retrieving the new
     *     value
     */
    public void recordLoadSuccess(long loadTime);

    /**
     * Records the failed load of a new entry. This should be called when a cache request causes
     * an entry to be loaded, but an exception is thrown while loading the entry. In contrast to
     * {@link #recordConcurrentMiss}, this method should only be called by the loading thread.
     *
     * @param loadTime the number of nanoseconds the cache spent computing or retrieving the new
     *     value prior to an exception being thrown
     */
    public void recordLoadException(long loadTime);

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

  /**
   * A thread-safe {@link StatsCounter} implementation for use by {@link Cache} implementors.
   *
   * @since 10.0
   */
  @Beta
  public static class SimpleStatsCounter implements StatsCounter {
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong loadSuccessCount = new AtomicLong();
    private final AtomicLong loadExceptionCount = new AtomicLong();
    private final AtomicLong totalLoadTime = new AtomicLong();
    private final AtomicLong evictionCount = new AtomicLong();

    /**
     * @since 11.0
     */
    @Override
    public void recordHits(int count) {
      hitCount.addAndGet(count);
    }

    /**
     * @since 11.0
     */
    @Override
    public void recordMisses(int count) {
      missCount.addAndGet(count);
    }

    @Override
    public void recordLoadSuccess(long loadTime) {
      loadSuccessCount.incrementAndGet();
      totalLoadTime.addAndGet(loadTime);
    }

    @Override
    public void recordLoadException(long loadTime) {
      loadExceptionCount.incrementAndGet();
      totalLoadTime.addAndGet(loadTime);
    }

    @Override
    public void recordEviction() {
      evictionCount.incrementAndGet();
    }

    @Override
    public CacheStats snapshot() {
      return new CacheStats(
          hitCount.get(),
          missCount.get(),
          loadSuccessCount.get(),
          loadExceptionCount.get(),
          totalLoadTime.get(),
          evictionCount.get());
    }

    /**
     * Increments all counters by the values in {@code other}.
     */
    public void incrementBy(StatsCounter other) {
      CacheStats otherStats = other.snapshot();
      hitCount.addAndGet(otherStats.hitCount());
      missCount.addAndGet(otherStats.missCount());
      loadSuccessCount.addAndGet(otherStats.loadSuccessCount());
      loadExceptionCount.addAndGet(otherStats.loadExceptionCount());
      totalLoadTime.addAndGet(otherStats.totalLoadTime());
      evictionCount.addAndGet(otherStats.evictionCount());
    }
  }
}
