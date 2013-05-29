/*
 * Copyright (C) 2012 The Guava Authors
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

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.MapMaker;

import java.util.Map;

/**
 * Compare CacheBuilder and MapMaker performance, ensuring that they remain on par with each other.
 *
 * @author Nikita Sidorov
 */
public class MapMakerComparisonBenchmark {
  private static final String TEST_KEY = "test key";
  private static final String TEST_VALUE = "test value";

  private static final Function<Object, Object> IDENTITY = Functions.identity();

  // Loading/computing versions:
  private final Map<Object, Object> computingMap = new MapMaker().makeComputingMap(IDENTITY);
  private final LoadingCache<Object, Object> loadingCache =
      CacheBuilder.newBuilder().recordStats().build(CacheLoader.from(IDENTITY));
  private final LoadingCache<Object, Object> loadingCacheNoStats =
      CacheBuilder.newBuilder().build(CacheLoader.from(IDENTITY));

  // Non-loading versions:
  private final Map<Object, Object> map = new MapMaker().makeMap(); // Returns ConcurrentHashMap
  private final Cache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build();
  private final Cache<Object, Object> cacheNoStats = CacheBuilder.newBuilder().build();

  @BeforeExperiment
  void setUp() {
    map.put(TEST_KEY, TEST_VALUE);
    cache.put(TEST_KEY, TEST_VALUE);
    cacheNoStats.put(TEST_KEY, TEST_VALUE);
  }

  @Benchmark void computingMapMaker(int rep) {
    for (int i = 0; i < rep; i++) {
      computingMap.get(TEST_KEY);
    }
  }

  @Benchmark void loadingCacheBuilder_stats(int rep) {
    for (int i = 0; i < rep; i++) {
      loadingCache.getUnchecked(TEST_KEY);
    }
  }

  @Benchmark void loadingCacheBuilder(int rep) {
    for (int i = 0; i < rep; i++) {
      loadingCacheNoStats.getUnchecked(TEST_KEY);
    }
  }

  @Benchmark void concurrentHashMap(int rep) {
    for (int i = 0; i < rep; i++) {
      map.get(TEST_KEY);
    }
  }

  @Benchmark void cacheBuilder_stats(int rep) {
    for (int i = 0; i < rep; i++) {
      cache.getIfPresent(TEST_KEY);
    }
  }

  @Benchmark void cacheBuilder(int rep) {
    for (int i = 0; i < rep; i++) {
      cacheNoStats.getIfPresent(TEST_KEY);
    }
  }
}
