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

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class provides a skeletal implementation of the {@code Cache} interface to minimize the
 * effort required to implement this interface.
 *
 * <p>To implement a cache, the programmer needs only to extend this class and provide an
 * implementation for the {@code getChecked} method.
 *
 * @author Charles Fry
 * @since Guava release 10
 */
@Beta
public abstract class AbstractCache<K, V> implements Cache<K, V> {

  /** Constructor for use by subclasses. */
  protected AbstractCache() {}

  @Override
  public V getUnchecked(K key) {
    try {
      return getChecked(key);
    } catch (ExecutionException e) {
      throw new ComputationException(e.getCause());
    }
  }

  @Deprecated
  @Override
  public final V apply(K key) {
    return getUnchecked(key);
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invalidate(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CacheStats stats() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ImmutableList<Map.Entry<K, V>> activeEntries(int limit) {
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
   * @since Guava release 10
   */
  @Beta
  public interface StatsCounter {
    /**
     * Records a single hit. This should be called when a cache request returns a cached value.
     */
    public void recordHit();

    /**
     * Records a single miss. This should be called when a cache request returns an uncached (newly
     * created) value or null. Multiple concurrent calls to {@link Cache} lookup methods on an
     * absent value should result in multiple calls to this method, despite all being served by the
     * results of a single creation.
     */
    public void recordMiss();

    /**
     * Records the creation of a new value. This should be called when a cache request triggers the
     * creation of a new value. This differs from {@link #recordMiss} in the case of concurrent
     * calls to {@link Cache} lookup methods on an absent value, in which case only a single call to
     * this method should occur. Note that the creating thread should call both {@link
     * #recordCreate} and {@link #recordMiss}.
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

  /**
   * A thread-safe {@link StatsCounter} implementation for use by {@link Cache} implementors.
   *
   * @since Guava release 10
   */
  @Beta
  public static class SimpleStatsCounter implements StatsCounter {
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong createCount = new AtomicLong();
    private final AtomicLong totalCreateTime = new AtomicLong();
    private final AtomicLong evictionCount = new AtomicLong();

    @Override
    public void recordHit() {
      hitCount.incrementAndGet();
    }

    @Override
    public void recordMiss() {
      missCount.incrementAndGet();
    }

    @Override
    public void recordCreate(long createTime) {
      createCount.incrementAndGet();
      totalCreateTime.addAndGet(createTime);
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
          createCount.get(),
          totalCreateTime.get(),
          evictionCount.get());
    }

    /**
     * Increments all counters by the values in {@code other}.
     */
    public void incrementBy(StatsCounter other) {
      CacheStats otherStats = other.snapshot();
      hitCount.addAndGet(otherStats.hitCount());
      missCount.addAndGet(otherStats.missCount());
      createCount.addAndGet(otherStats.createCount());
      totalCreateTime.addAndGet(otherStats.totalCreateTime());
      evictionCount.addAndGet(otherStats.evictionCount());
    }
  }
}
