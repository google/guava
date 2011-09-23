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
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class provides a skeletal implementation of the {@code Cache} interface to minimize the
 * effort required to implement this interface.
 *
 * <p>To implement a cache, the programmer needs only to extend this class and provide an
 * implementation for the {@code get} method. This implementation throws an
 * {@link UnsupportedOperationException} on calls to {@link #size}, {@link #invalidate},
 * {@link #invalidateAll}, {@link #stats}, and {@link #asMap}. The methods
 * {@link #getUnchecked} and {@link #apply} are implemented in terms of {@link #get}. The method
 * {@link #cleanUp} is a no-op.
 *
 * @author Charles Fry
 * @since 10.0
 */
@Beta
public abstract class AbstractCache<K, V> implements Cache<K, V> {

  /** Constructor for use by subclasses. */
  protected AbstractCache() {}

  @Override
  public V getUnchecked(K key) {
    try {
      return get(key);
    } catch (ExecutionException e) {
      throw new UncheckedExecutionException(e.getCause());
    }
  }

  @Override
  public final V apply(K key) {
    return getUnchecked(key);
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

  /**
   * Accumulates statistics during the operation of a {@link Cache} for presentation by {@link
   * Cache#stats}. This is solely intended for consumption by {@code Cache} implementors.
   *
   * @since 10.0
   */
  @Beta
  public interface StatsCounter {
    /**
     * Records a single hit. This should be called when a cache request returns a cached value.
     */
    public void recordHit();

    /**
     * Records the successful load of a new entry. This should be called when a cache request
     * causes an entry to be loaded, and the loading completes succesfully. In contrast to
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
     * Records a single concurrent miss. This should be called when a cache request returns a
     * value which was loaded by a different thread. In contrast to {@link #recordLoadSuccess}
     * and {@link #recordLoadException}, this method should never be called by the loading
     * thread. Multiple concurrent calls to {@link Cache} lookup methods with the same key on an
     * absent value should result in a single call to either {@code recordLoadSuccess} or
     * {@code recordLoadException} and multiple calls to this method, despite all being served by
     * the results of a single load operation.
     */
    public void recordConcurrentMiss();

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

    @Override
    public void recordHit() {
      hitCount.incrementAndGet();
    }

    @Override
    public void recordLoadSuccess(long loadTime) {
      missCount.incrementAndGet();
      loadSuccessCount.incrementAndGet();
      totalLoadTime.addAndGet(loadTime);
    }

    @Override
    public void recordLoadException(long loadTime) {
      missCount.incrementAndGet();
      loadExceptionCount.incrementAndGet();
      totalLoadTime.addAndGet(loadTime);
    }

    @Override
    public void recordConcurrentMiss() {
      missCount.incrementAndGet();
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
