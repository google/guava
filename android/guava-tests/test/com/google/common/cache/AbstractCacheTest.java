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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.cache.AbstractCache.SimpleStatsCounter;
import com.google.common.cache.AbstractCache.StatsCounter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Unit test for {@link AbstractCache}.
 *
 * @author Charles Fry
 */
@NullUnmarked
public class AbstractCacheTest extends TestCase {

  public void testGetIfPresent() {
    AtomicReference<Object> valueRef = new AtomicReference<>();
    Cache<Object, Object> cache =
        new AbstractCache<Object, Object>() {
          @Override
          public @Nullable Object getIfPresent(Object key) {
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
          public @Nullable Object getIfPresent(Object key) {
            return null;
          }
        };

    assertEquals(ImmutableMap.of(), cache.getAllPresent(ImmutableList.of(new Object())));
  }

  public void testGetAllPresent_cached() {
    Object cachedKey = new Object();
    Object cachedValue = new Object();
    Cache<Object, Object> cache =
        new AbstractCache<Object, Object>() {
          @Override
          public @Nullable Object getIfPresent(Object key) {
            return cachedKey.equals(key) ? cachedValue : null;
          }
        };

    assertEquals(
        ImmutableMap.of(cachedKey, cachedValue),
        cache.getAllPresent(ImmutableList.of(cachedKey, new Object())));
  }

  public void testInvalidateAll() {
    List<Object> invalidated = new ArrayList<>();
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
    assertThat(stats.hitRate()).isEqualTo(1.0);
    assertEquals(0, stats.missCount());
    assertThat(stats.missRate()).isEqualTo(0.0);
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.loadCount());
    assertEquals(0, stats.totalLoadTime());
    assertThat(stats.averageLoadPenalty()).isEqualTo(0.0);
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
    assertThat(stats.hitRate()).isEqualTo(11.0 / requestCount);
    int missCount = 23;
    assertEquals(missCount, stats.missCount());
    assertThat(stats.missRate()).isEqualTo(((double) missCount) / requestCount);
    assertEquals(13, stats.loadSuccessCount());
    assertEquals(17, stats.loadExceptionCount());
    assertEquals(13 + 17, stats.loadCount());
    assertEquals(214, stats.totalLoadTime());
    assertThat(stats.averageLoadPenalty()).isEqualTo(214.0 / (13 + 17));
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
