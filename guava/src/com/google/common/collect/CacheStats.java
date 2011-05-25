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
 * <li>A cache lookup that encounters an existing cache entry will increment {@code hitCount}.
 * <li>The first cache lookup that encounters a missing cache entry will create a new entry. After
 *     successful creation it will increment {@code missCount} and {@code createCount}, and add the
 *     total creation time, in nanoseconds, to {@code totalCreateTime}.
 * <li>Cache lookups that encounter a missing cache entry that is pending creation will await
 *     successful creation and then increment {@code missCount}.
 * <li>No stats are modified when entry creation throws an exception, neither for the creating
 *     thread nor for waiting threads.
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
  private final long createCount;
  private final long totalCreateTime;
  private final long evictionCount;
  // TODO(user): add createExceptionCount?

  /**
   * Constructs a new {@code CacheStats} instance.
   *
   * <p>Five parameters of the same type in a row is a bad thing, but this class is not constructed
   * by end users and is too fine-grained for a builder.
   */
  public CacheStats(long hitCount, long missCount, long createCount, long totalCreateTime,
      long evictionCount) {
    checkArgument(hitCount >= 0);
    checkArgument(missCount >= 0);
    checkArgument(createCount >= 0);
    checkArgument(totalCreateTime >= 0);
    checkArgument(evictionCount >= 0);

    this.hitCount = hitCount;
    this.missCount = missCount;
    this.createCount = createCount;
    this.totalCreateTime = totalCreateTime;
    this.evictionCount = evictionCount;
  }

  /**
   * Returns the number of times {@link Cache} lookup methods have returned either a cached or
   * uncached value. This is defined as {@code hitCount() + missCount()}.
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
   * {@code hitCount() / requestCount()}, or {@code 1.0} when {@code requestCount() == 0}.
   * Note that {@code hitRate() + missRate() =~ 1.0}.
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
   * {@code missCount() / requestCount()}, or {@code 0.0} when {@code requestCount() == 0}.
   * Note that {@code hitRate() + missRate() =~ 1.0}.
   */
  public double missRate() {
    long requestCount = requestCount();
    return (requestCount == 0) ? 0.0 : (double) missCount / requestCount;
  }

  /**
   * Returns the number of times {@link Cache} lookup methods have successfully created a new value.
   * This differs from {@link #missCount} only in the case of concurrent calls to {@link Cache}
   * lookup methods on an absent value, in which case multiple simultaneous misses will result in a
   * single creation. Thus, the returned value can never exceed the value of {@code missCount()}.
   */
  public long createCount() {
    return createCount;
  }

  /**
   * Returns the total number of nanoseconds the cache has spent creating new values. This can be
   * used to calculate the miss penalty. This value is increased every time {@code createCount()}
   * is incremented.
   */
  public long totalCreateTime() {
    return totalCreateTime;
  }

  /**
   * Returns the average time spent creating new values. This is defined as
   * {@code totalCreateTime() / createCount()}.
   */
  public double averageCreatePenalty() {
    return (createCount == 0) ? 0.0 : (double) totalCreateTime / createCount;
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
   * and {@code other}.
   *
   * @throws IllegalArgumentException if any value in {@code other} is greater than the
   *     corresponding value in this instance (this will not happen if {@code other} was retrieved
   *     from the same cache at an earlier time)
   */
  public CacheStats minus(CacheStats other) {
    return new CacheStats(
        hitCount - other.hitCount,
        missCount - other.missCount,
        createCount - other.createCount,
        totalCreateTime - other.totalCreateTime,
        evictionCount - other.evictionCount);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(hitCount, missCount, createCount, totalCreateTime, evictionCount);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof CacheStats) {
      CacheStats other = (CacheStats) object;
      return hitCount == other.hitCount
          && missCount == other.missCount
          && createCount == other.createCount
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
        .add("createCount", createCount)
        .add("totalCreateTime", totalCreateTime)
        .add("evictionCount", evictionCount)
        .toString();
  }
}
