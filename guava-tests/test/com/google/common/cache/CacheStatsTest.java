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

import junit.framework.TestCase;

/**
 * Unit test for {@link CacheStats}.
 *
 * @author Charles Fry
 */
public class CacheStatsTest extends TestCase {

  public void testEmpty() {
    CacheStats stats = new CacheStats(0, 0, 0, 0, 0, 0);
    assertEquals(0, stats.requestCount());
    assertEquals(0, stats.hitCount());
    assertEquals(1.0, stats.hitRate());
    assertEquals(0, stats.missCount());
    assertEquals(0.0, stats.missRate());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0.0, stats.loadExceptionRate());
    assertEquals(0, stats.loadCount());
    assertEquals(0, stats.totalLoadTime());
    assertEquals(0.0, stats.averageLoadPenalty());
    assertEquals(0, stats.evictionCount());
  }

  public void testSingle() {
    CacheStats stats = new CacheStats(11, 13, 17, 19, 23, 27);
    assertEquals(24, stats.requestCount());
    assertEquals(11, stats.hitCount());
    assertEquals(11.0 / 24, stats.hitRate());
    assertEquals(13, stats.missCount());
    assertEquals(13.0 / 24, stats.missRate());
    assertEquals(17, stats.loadSuccessCount());
    assertEquals(19, stats.loadExceptionCount());
    assertEquals(19.0 / 36, stats.loadExceptionRate());
    assertEquals(17 + 19, stats.loadCount());
    assertEquals(23, stats.totalLoadTime());
    assertEquals(23.0 / (17 + 19), stats.averageLoadPenalty());
    assertEquals(27, stats.evictionCount());
  }

  public void testMinus() {
    CacheStats one = new CacheStats(11, 13, 17, 19, 23, 27);
    CacheStats two = new CacheStats(53, 47, 43, 41, 37, 31);

    CacheStats diff = two.minus(one);
    assertEquals(76, diff.requestCount());
    assertEquals(42, diff.hitCount());
    assertEquals(42.0 / 76, diff.hitRate());
    assertEquals(34, diff.missCount());
    assertEquals(34.0 / 76, diff.missRate());
    assertEquals(26, diff.loadSuccessCount());
    assertEquals(22, diff.loadExceptionCount());
    assertEquals(22.0 / 48, diff.loadExceptionRate());
    assertEquals(26 + 22, diff.loadCount());
    assertEquals(14, diff.totalLoadTime());
    assertEquals(14.0 / (26 + 22), diff.averageLoadPenalty());
    assertEquals(4, diff.evictionCount());

    assertEquals(new CacheStats(0, 0, 0, 0, 0, 0), one.minus(two));
  }

  public void testPlus() {
    CacheStats one = new CacheStats(11, 13, 15, 13, 11, 9);
    CacheStats two = new CacheStats(53, 47, 41, 39, 37, 35);

    CacheStats sum = two.plus(one);
    assertEquals(124, sum.requestCount());
    assertEquals(64, sum.hitCount());
    assertEquals(64.0 / 124, sum.hitRate());
    assertEquals(60, sum.missCount());
    assertEquals(60.0 / 124, sum.missRate());
    assertEquals(56, sum.loadSuccessCount());
    assertEquals(52, sum.loadExceptionCount());
    assertEquals(52.0 / 108, sum.loadExceptionRate());
    assertEquals(56 + 52, sum.loadCount());
    assertEquals(48, sum.totalLoadTime());
    assertEquals(48.0 / (56 + 52), sum.averageLoadPenalty());
    assertEquals(44, sum.evictionCount());

    assertEquals(sum, one.plus(two));
  }

  public void testPlusLarge() {
    CacheStats maxCacheStats =
        new CacheStats(
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE);
    CacheStats smallCacheStats = new CacheStats(1, 1, 1, 1, 1, 1);

    CacheStats sum = smallCacheStats.plus(maxCacheStats);
    assertEquals(Long.MAX_VALUE, sum.requestCount());
    assertEquals(Long.MAX_VALUE, sum.hitCount());
    assertEquals(1.0, sum.hitRate());
    assertEquals(Long.MAX_VALUE, sum.missCount());
    assertEquals(1.0, sum.missRate());
    assertEquals(Long.MAX_VALUE, sum.loadSuccessCount());
    assertEquals(Long.MAX_VALUE, sum.loadExceptionCount());
    assertEquals(1.0, sum.loadExceptionRate());
    assertEquals(Long.MAX_VALUE, sum.loadCount());
    assertEquals(Long.MAX_VALUE, sum.totalLoadTime());
    assertEquals(1.0, sum.averageLoadPenalty());
    assertEquals(Long.MAX_VALUE, sum.evictionCount());

    assertEquals(sum, maxCacheStats.plus(smallCacheStats));
  }
}
