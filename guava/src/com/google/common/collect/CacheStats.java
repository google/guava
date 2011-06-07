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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;

import javax.annotation.Nullable;

/**
 * Statistics about the performance of a {@link Cache}. Instances of this class are immutable.
 *
 * <p>Cache statistics are incremented according to the following rules:
 *
 * <ul>
 * <li>When a cache lookup encounters an existing cache entry {@code hitCount} is incremented.
 * <li>When a cache lookup first encounters a missing cache entry, a new entry is created.
 * <ul>
 * <li>After successful creation {@code missCount} and {@code createSuccessCount} are
 *     incremented, and the total creation time, in nanoseconds, is added to
 *     {@code totalCreateTime}.
 * <li>When an exception is thrown during creation {@code missCount} and {@code
 *     createExceptionCount} are incremented, and the total creation time, in nanoseconds, is
 *     added to {@code totalCreateTime}.
 * <li>Cache lookups that encounter a missing cache entry that is pending creation will await
 *     creation (whether successful or not) and then increment {@code missCount}.
 * </ul>
 * <li>When an entry is evicted from the cache, {@code evictionCount} is incremented.
 * <li>No stats are modified when a cache entry is invalidated or manually removed.
 * <li>No stats are modified by operations invoked on the {@linkplain Cache#asMap asMap} view of
 *     the cache.
 * </ul>
 *
 * @author Charles Fry
 * @since Guava release 10
 */
@Beta
public final class CacheStats {
  private final long hitCount;
  private final long missCount;
  private final long createSuccessCount;
  private final long createExceptionCount;
  private final long totalCreateTime;
  private final long evictionCount;

  /**
   * Constructs a new {@code CacheStats} instance.
   *
   * <p>Five parameters of the same type in a row is a bad thing, but this class is not constructed
   * by end users and is too fine-grained for a builder.
   */
  public CacheStats(long hitCount, long missCount, long createSuccessCount,
      long createExceptionCount, long totalCreateTime, long evictionCount) {
    checkArgument(hitCount >= 0);
    checkArgument(missCount >= 0);
    checkArgument(createSuccessCount >= 0);
    checkArgument(createExceptionCount >= 0);
    checkArgument(totalCreateTime >= 0);
    checkArgument(evictionCount >= 0);

    this.hitCount = hitCount;
    this.missCount = missCount;
    this.createSuccessCount = createSuccessCount;
    this.createExceptionCount = createExceptionCount;
    this.totalCreateTime = totalCreateTime;
    this.evictionCount = evictionCount;
  }

  /**
   * Returns the number of times {@link Cache} lookup methods have returned either a cached or
   * uncached value. This is defined as {@code hitCount + missCount}.
   */
  public long requestCount() {
    return hitCount + missCount;
  }

  /**
   * Returns the number of times {@link Cache} lookup methods have returned a cached value.
   */
  public long hitCount() {
    return hitCount;
  }

  /**
   * Returns the ratio of cache requests which were hits. This is defined as
   * {@code hitCount / requestCount}, or {@code 1.0} when {@code requestCount == 0}.
   * Note that {@code hitRate + missRate =~ 1.0}.
   */
  public double hitRate() {
    long requestCount = requestCount();
    return (requestCount == 0) ? 1.0 : (double) hitCount / requestCount;
  }

  /**
   * Returns the number of times {@link Cache} lookup methods have returned an uncached (newly
   * created) value, or null. Multiple concurrent calls to {@link Cache} lookup methods on an absent
   * value can result in multiple misses, all returning the results of a single creation.
   */
  public long missCount() {
    return missCount;
  }

  /**
   * Returns the ratio of cache requests which were misses. This is defined as
   * {@code missCount / requestCount}, or {@code 0.0} when {@code requestCount == 0}.
   * Note that {@code hitRate + missRate =~ 1.0}. Cache misses include all requests which
   * weren't cache hits, including requests which resulted in either successful or failed creation
   * attempts, and requests which waited for other threads to finish creation. It is thus the case
   * that {@code missCount &gt;= createSuccessCount + createExceptionCount}. Multiple
   * concurrent misses for the same key will result in a single creation.
   */
  public double missRate() {
    long requestCount = requestCount();
    return (requestCount == 0) ? 0.0 : (double) missCount / requestCount;
  }

  /**
   * Returns the total number of times that {@link Cache} lookup methods attempted to create new
   * values. This includes both successful creations, as well as those that threw exceptions. This
   * is defined as {@code createSuccessCount + createExceptionCount}.
   */
  public long createCount() {
    return createSuccessCount + createExceptionCount;
  }

  /**
   * Returns the number of times {@link Cache} lookup methods have successfully created a new value.
   * This is always incremented in conjunction with {@link #missCount}, though {@code missCount}
   * is also incremented when an exception is encountered during creation (see
   * {@link #createExceptionCount}). Multiple concurrent misses for the same key will result in a
   * single creation.
   */
  public long createSuccessCount() {
    return createSuccessCount;
  }

  /**
   * Returns the number of times {@link Cache} lookup methods threw an exception while creating a
   * new value. This is always incremented in conjunction with {@code missCount}, though
   * {@code missCount} is also incremented when creation completes successfully (see
   * {@link #createSuccessCount}). Multiple concurrent misses for the same key will result in a
   * single creation.
   */
  public long createExceptionCount() {
    return createExceptionCount;
  }

  /**
   * Returns the ratio of cache creates which threw exceptions. This is defined as
   * {@code createExceptionCount / (createSuccessCount + createExceptionCount)}, or
   * {@code 0.0} when {@code createSuccessCount + createExceptionCount == 0}.
   */
  public double createExceptionRate() {
    long totalCreateCount = createSuccessCount + createExceptionCount;
    return (totalCreateCount == 0)
        ? 0.0
        : (double) createExceptionCount / totalCreateCount;
  }

  /**
   * Returns the total number of nanoseconds the cache has spent creating new values. This can be
   * used to calculate the miss penalty. This value is increased every time
   * {@code createSuccessCount} or {@code createExceptionCount} is incremented.
   */
  public long totalCreateTime() {
    return totalCreateTime;
  }

  /**
   * Returns the average time spent creating new values. This is defined as
   * {@code totalCreateTime / (createSuccessCount + createExceptionCount)}.
   */
  public double averageCreatePenalty() {
    long totalCreateCount = createSuccessCount + createExceptionCount;
    return (totalCreateCount == 0)
        ? 0.0
        : (double) totalCreateTime / totalCreateCount;
  }

  /**
   * Returns the number of times an entry has been evicted. This count does not include manual
   * {@linkplain Cache#invalidate invalidations}.
   */
  public long evictionCount() {
    return evictionCount;
  }

  /**
   * Returns a new {@code CacheStats} representing the difference between this {@code CacheStats}
   * and {@code other}. Negative values, which aren't supported by {@code CacheStats} will be
   * rounded up to zero.
   */
  public CacheStats minus(CacheStats other) {
    return new CacheStats(
        Math.max(0, hitCount - other.hitCount),
        Math.max(0, missCount - other.missCount),
        Math.max(0, createSuccessCount - other.createSuccessCount),
        Math.max(0, createExceptionCount - other.createExceptionCount),
        Math.max(0, totalCreateTime - other.totalCreateTime),
        Math.max(0, evictionCount - other.evictionCount));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(hitCount, missCount, createSuccessCount, createExceptionCount,
        totalCreateTime, evictionCount);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof CacheStats) {
      CacheStats other = (CacheStats) object;
      return hitCount == other.hitCount
          && missCount == other.missCount
          && createSuccessCount == other.createSuccessCount
          && createExceptionCount == other.createExceptionCount
          && totalCreateTime == other.totalCreateTime
          && evictionCount == other.evictionCount;
    }
    return false;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("hitCount", hitCount)
        .add("missCount", missCount)
        .add("createSuccessCount", createSuccessCount)
        .add("createExceptionCount", createExceptionCount)
        .add("totalCreateTime", totalCreateTime)
        .add("evictionCount", evictionCount)
        .toString();
  }
}
