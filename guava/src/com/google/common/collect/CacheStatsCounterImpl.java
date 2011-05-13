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

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * A thread-safe {@link Counter} implementation for use by {@link Cache} implementors.
 *
 * @author Charles Fry
 * @since Guava release 10
 */
@Beta
public class CacheStatsCounterImpl implements CacheStatsCounter {
  private static int HIT_INDEX = 0;
  private static int MISS_INDEX = 1;
  private static int CREATE_INDEX = 2;
  private static int CREATE_TIME_INDEX = 3;
  private static int EVICTION_INDEX = 4;
  private static int SIZE = 5;

  private final AtomicLongArray counts = new AtomicLongArray(SIZE);

  @Override
  public void recordHit() {
    counts.incrementAndGet(HIT_INDEX);
  }

  @Override
  public void recordMiss() {
    counts.incrementAndGet(MISS_INDEX);
  }

  @Override
  public void recordCreate(long createTime) {
    counts.incrementAndGet(CREATE_INDEX);
    counts.addAndGet(CREATE_TIME_INDEX, createTime);
  }

  @Override
  public void recordEviction() {
    counts.incrementAndGet(EVICTION_INDEX);
  }

  @Override
  public CacheStats snapshot() {
    return new CacheStats(
        counts.get(HIT_INDEX),
        counts.get(MISS_INDEX),
        counts.get(CREATE_INDEX),
        counts.get(CREATE_TIME_INDEX),
        counts.get(EVICTION_INDEX));
  }

  /**
   * Increments all counters by the values in {@code other}.
   */
  public void incrementBy(CacheStatsCounter other) {
    if (other instanceof CacheStatsCounterImpl) {
      CacheStatsCounterImpl otherCounter = (CacheStatsCounterImpl) other;
      for (int i = 0; i < SIZE; i++) {
        counts.addAndGet(i, otherCounter.counts.get(i));
      }
    } else {
      CacheStats otherStats = other.snapshot();
      counts.addAndGet(HIT_INDEX, otherStats.hitCount());
      counts.addAndGet(MISS_INDEX, otherStats.missCount());
      counts.addAndGet(CREATE_INDEX, otherStats.createCount());
      counts.addAndGet(CREATE_TIME_INDEX, otherStats.totalCreateTime());
      counts.addAndGet(EVICTION_INDEX, otherStats.evictionCount());
    }
  }
}
