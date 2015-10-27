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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * This class provides a skeletal implementation of the {@code Cache} interface to minimize the
 * effort required to implement this interface.
 *
 * <p>To implement a cache, the programmer needs only to extend this class and provide an
 * implementation for the {@link #put} and {@link #getIfPresent} methods. {@link #getAllPresent} is
 * implemented in terms of {@link #getIfPresent}; {@link #putAll} is implemented in terms of
 * {@link #put}, {@link #invalidateAll(Iterable)} is implemented in terms of {@link #invalidate}.
 * The method {@link #cleanUp} is a no-op. All other methods throw an
 * {@link UnsupportedOperationException}.
 *
 * @author Charles Fry
 * @since 10.0
 */
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
   * This implementation of {@code getAllPresent} lacks any insight into the internal cache data
   * structure, and is thus forced to return the query keys instead of the cached keys. This is only
   * possible with an unsafe cast which requires {@code keys} to actually be of type {@code K}.
   *
   * {@inheritDoc}
   *
   * @since 11.0
   */
  @Override
  public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
    Map<K, V> result = Maps.newLinkedHashMap();
    for (Object key : keys) {
      if (!result.containsKey(key)) {
        @SuppressWarnings("unchecked")
        K castKey = (K) key;
        V value = getIfPresent(key);
        if (value != null) {
          result.put(castKey, value);
        }
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

  /**
   * @since 12.0
   */
  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
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

  /**
   * A thread-safe {@link StatsCounter} implementation for use by {@link Cache} implementors.
   *
   * @since 10.0
   */
  public static final class SimpleStatsCounter implements StatsCounter {
    private final LongAddable hitCount = LongAddables.create();
    private final LongAddable missCount = LongAddables.create();
    private final LongAddable loadSuccessCount = LongAddables.create();
    private final LongAddable loadExceptionCount = LongAddables.create();
    private final LongAddable totalLoadTime = LongAddables.create();
    private final LongAddable evictionCount = LongAddables.create();

    /**
     * Constructs an instance with all counts initialized to zero.
     */
    public SimpleStatsCounter() {}

    /**
     * @since 11.0
     */
    @Override
    public void recordHits(int count) {
      hitCount.add(count);
    }

    /**
     * @since 11.0
     */
    @Override
    public void recordMisses(int count) {
      missCount.add(count);
    }

    @Override
    public void recordLoadSuccess(long loadTime) {
      loadSuccessCount.increment();
      totalLoadTime.add(loadTime);
    }

    @Override
    public void recordLoadException(long loadTime) {
      loadExceptionCount.increment();
      totalLoadTime.add(loadTime);
    }

    @Override
    public void recordEviction() {
      evictionCount.increment();
    }

    @Override
    public CacheStats snapshot() {
      return new CacheStats(
          hitCount.sum(),
          missCount.sum(),
          loadSuccessCount.sum(),
          loadExceptionCount.sum(),
          totalLoadTime.sum(),
          evictionCount.sum());
    }

    /**
     * Increments all counters by the values in {@code other}.
     */
    public void incrementBy(StatsCounter other) {
      CacheStats otherStats = other.snapshot();
      hitCount.add(otherStats.hitCount());
      missCount.add(otherStats.missCount());
      loadSuccessCount.add(otherStats.loadSuccessCount());
      loadExceptionCount.add(otherStats.loadExceptionCount());
      totalLoadTime.add(otherStats.totalLoadTime());
      evictionCount.add(otherStats.evictionCount());
    }
  }
}
