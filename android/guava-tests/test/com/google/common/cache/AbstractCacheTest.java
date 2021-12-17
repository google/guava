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

import com.google.common.cache.AbstractCache.SimpleStatsCounter;
import com.google.common.cache.AbstractCache.StatsCounter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;

/**
 * Unit test for {@link AbstractCache}.
 *
 * @author Charles Fry
 */
public class AbstractCacheTest extends TestCase {

  public void testGetIfPresent() {
    final AtomicReference<Object> valueRef = new AtomicReference<>();
    Cache<Object, Object> cache =
        new AbstractCache<Object, Object>() {
          @Override
          public Object getIfPresent(Object key) {
            return valueRef.get();
          }
        };

    assertNull(cache.getIfPresent(new Object()));

    Object newValue = new Object();
    valueRef.set(newValue);
    assertSame(newValue, cache.getIfPresent(new Object()));
  }

  public void testGetAllPresent_empty() {
    Cache<Object, Object> cache =
        new AbstractCache<Object, Object>() {
          @Override
          public Object getIfPresent(Object key) {
            return null;
          }
        };

    assertEquals(ImmutableMap.of(), cache.getAllPresent(ImmutableList.of(new Object())));
  }

  public void testGetAllPresent_cached() {
    final Object cachedKey = new Object();
    final Object cachedValue = new Object();
    Cache<Object, Object> cache =
        new AbstractCache<Object, Object>() {
          @Override
          public Object getIfPresent(Object key) {
            return cachedKey.equals(key) ? cachedValue : null;
          }
        };

    assertEquals(
        ImmutableMap.of(cachedKey, cachedValue),
        cache.getAllPresent(ImmutableList.of(cachedKey, new Object())));
  }

  public void testInvalidateAll() {
    final List<Object> invalidated = Lists.newArrayList();
    Cache<Integer, Integer> cache =
        new AbstractCache<Integer, Integer>() {
          @Override
          public Integer getIfPresent(Object key) {
            throw new UnsupportedOperationException();
          }

          @Override
          public void invalidate(Object key) {
            invalidated.add(key);
          }
        };

    List<Integer> toInvalidate = ImmutableList.of(1, 2, 3, 4);
    cache.invalidateAll(toInvalidate);
    assertEquals(toInvalidate, invalidated);
  }

  public void testEmptySimpleStats() {
    StatsCounter counter = new SimpleStatsCounter();
    CacheStats stats = counter.snapshot();
    assertEquals(0, stats.requestCount());
    assertEquals(0, stats.hitCount());
    assertEquals(1.0, stats.hitRate());
    assertEquals(0, stats.missCount());
    assertEquals(0.0, stats.missRate());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.loadCount());
    assertEquals(0, stats.totalLoadTime());
    assertEquals(0.0, stats.averageLoadPenalty());
    assertEquals(0, stats.evictionCount());
  }

  public void testSingleSimpleStats() {
    StatsCounter counter = new SimpleStatsCounter();
    for (int i = 0; i < 11; i++) {
      counter.recordHits(1);
    }
    for (int i = 0; i < 13; i++) {
      counter.recordLoadSuccess(i);
    }
    for (int i = 0; i < 17; i++) {
      counter.recordLoadException(i);
    }
    for (int i = 0; i < 23; i++) {
      counter.recordMisses(1);
    }
    for (int i = 0; i < 27; i++) {
      counter.recordEviction();
    }
    CacheStats stats = counter.snapshot();
    int requestCount = 11 + 23;
    assertEquals(requestCount, stats.requestCount());
    assertEquals(11, stats.hitCount());
    assertEquals(11.0 / requestCount, stats.hitRate());
    int missCount = 23;
    assertEquals(missCount, stats.missCount());
    assertEquals(((double) missCount) / requestCount, stats.missRate());
    assertEquals(13, stats.loadSuccessCount());
    assertEquals(17, stats.loadExceptionCount());
    assertEquals(13 + 17, stats.loadCount());
    assertEquals(214, stats.totalLoadTime());
    assertEquals(214.0 / (13 + 17), stats.averageLoadPenalty());
    assertEquals(27, stats.evictionCount());
  }

  public void testSimpleStatsOverflow() {
    StatsCounter counter = new SimpleStatsCounter();
    counter.recordLoadSuccess(Long.MAX_VALUE);
    counter.recordLoadSuccess(1);
    CacheStats stats = counter.snapshot();
    assertEquals(Long.MAX_VALUE, stats.totalLoadTime());
  }

  public void testSimpleStatsIncrementBy() {
    long totalLoadTime = 0;

    SimpleStatsCounter counter1 = new SimpleStatsCounter();
    for (int i = 0; i < 11; i++) {
      counter1.recordHits(1);
    }
    for (int i = 0; i < 13; i++) {
      counter1.recordLoadSuccess(i);
      totalLoadTime += i;
    }
    for (int i = 0; i < 17; i++) {
      counter1.recordLoadException(i);
      totalLoadTime += i;
    }
    for (int i = 0; i < 19; i++) {
      counter1.recordMisses(1);
    }
    for (int i = 0; i < 23; i++) {
      counter1.recordEviction();
    }

    SimpleStatsCounter counter2 = new SimpleStatsCounter();
    for (int i = 0; i < 27; i++) {
      counter2.recordHits(1);
    }
    for (int i = 0; i < 31; i++) {
      counter2.recordLoadSuccess(i);
      totalLoadTime += i;
    }
    for (int i = 0; i < 37; i++) {
      counter2.recordLoadException(i);
      totalLoadTime += i;
    }
    for (int i = 0; i < 41; i++) {
      counter2.recordMisses(1);
    }
    for (int i = 0; i < 43; i++) {
      counter2.recordEviction();
    }

    counter1.incrementBy(counter2);
    assertEquals(new CacheStats(38, 60, 44, 54, totalLoadTime, 66), counter1.snapshot());
  }
}
