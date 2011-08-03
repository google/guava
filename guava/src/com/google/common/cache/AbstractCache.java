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
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

/**
 * This class provides a skeletal implementation of the {@code Cache} interface to minimize the
 * effort required to implement this interface.
 *
 * <p>To implement a cache, the programmer needs only to extend this class and provide an
 * implementation for the {@code get} method.
 *
 * @author Charles Fry
 * @since Guava release 10
 */
@Beta
public abstract class AbstractCache<K, V> implements Cache<K, V> {

  /** Constructor for use by subclasses. */
  protected AbstractCache() {}

  @Override
  @Nullable
  public V getUnchecked(K key) {
    try {
      return get(key);
    } catch (ExecutionException e) {
      wrapAndThrowUnchecked(e.getCause());
      throw new AssertionError();
    }
  }

  @Override
  @Nullable
  public final V apply(K key) {
    return getUnchecked(key);
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invalidate(@Nullable Object key) {
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
     * Records the successful creation of a new value. This should be called when a cache request
     * triggers the creation of a new value, and that creation completes succesfully. In contrast to
     * {@link #recordConcurrentMiss}, this method should only be called by the creating thread.
     *
     * @param createTime the number of nanoseconds the cache spent creating the new value
     */
    public void recordCreateSuccess(long createTime);

    /**
     * Records the failed creation of a new value. This should be called when a cache request
     * triggers the creation of a new value, but that creation throws an exception. In contrast to
     * {@link #recordConcurrentMiss}, this method should only be called by the creating thread.
     *
     * @param createTime the number of nanoseconds the cache spent creating the new value prior to
     *     an exception being thrown
     */
    public void recordCreateException(long createTime);

    /**
     * Records a single concurrent miss. This should be called when a cache request returns a
     * value which was created by a different thread. In contrast to {@link #recordCreateSuccess}
     * and {@link #recordCreateException}, this method should never be called by the creating
     * thread. Multiple concurrent calls to {@link Cache} lookup methods with the same key on an
     * absent value should result in a single call to either {@code recordCreateSuccess} or
     * {@code recordCreateException} and multiple calls to this method, despite all being served by
     * the results of a single creation.
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
   * @since Guava release 10
   */
  @Beta
  public static class SimpleStatsCounter implements StatsCounter {
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong createSuccessCount = new AtomicLong();
    private final AtomicLong createExceptionCount = new AtomicLong();
    private final AtomicLong totalCreateTime = new AtomicLong();
    private final AtomicLong evictionCount = new AtomicLong();

    @Override
    public void recordHit() {
      hitCount.incrementAndGet();
    }

    @Override
    public void recordCreateSuccess(long createTime) {
      missCount.incrementAndGet();
      createSuccessCount.incrementAndGet();
      totalCreateTime.addAndGet(createTime);
    }

    @Override
    public void recordCreateException(long createTime) {
      missCount.incrementAndGet();
      createExceptionCount.incrementAndGet();
      totalCreateTime.addAndGet(createTime);
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
          createSuccessCount.get(),
          createExceptionCount.get(),
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
      createSuccessCount.addAndGet(otherStats.createSuccessCount());
      createExceptionCount.addAndGet(otherStats.createExceptionCount());
      totalCreateTime.addAndGet(otherStats.totalCreateTime());
      evictionCount.addAndGet(otherStats.evictionCount());
    }
  }

  // This code also appears in common.util.concurrent.
  static void wrapAndThrowUnchecked(Throwable cause) {
    if (cause instanceof Error) {
      throw new ExecutionError((Error) cause);
    }
    if (cause instanceof Exception) {
      throw new UncheckedExecutionException((Exception) cause);
    }
    /*
     * It's a non-Error, non-Exception Throwable. From my survey of such
     * classes, I believe that most users intended to extend Exception, so we'll
     * treat it like an Exception.
     */
    throw new UncheckedExecutionExceptionForThrowable(cause);
  }

  private static final class UncheckedExecutionExceptionForThrowable
      extends UncheckedExecutionException {
    UncheckedExecutionExceptionForThrowable(Throwable cause) {
      super(cause.toString());
      initCause(cause);
    }
  }
}
