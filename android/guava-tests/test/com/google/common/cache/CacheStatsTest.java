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

import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit test for {@link CacheStats}.
 *
 * @author Charles Fry
 */
@NullUnmarked
public class CacheStatsTest extends TestCase {

  public void testEmpty() {
    CacheStats stats = new CacheStats(0, 0, 0, 0, 0, 0);
    assertEquals(0, stats.requestCount());
    assertEquals(0, stats.hitCount());
    assertThat(stats.hitRate()).isEqualTo(1.0);
    assertEquals(0, stats.missCount());
    assertThat(stats.missRate()).isEqualTo(0.0);
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertThat(stats.loadExceptionRate()).isEqualTo(0.0);
    assertEquals(0, stats.loadCount());
    assertEquals(0, stats.totalLoadTime());
    assertThat(stats.averageLoadPenalty()).isEqualTo(0.0);
    assertEquals(0, stats.evictionCount());
  }

  public void testSingle() {
    CacheStats stats = new CacheStats(11, 13, 17, 19, 23, 27);
    assertEquals(24, stats.requestCount());
    assertEquals(11, stats.hitCount());
    assertThat(stats.hitRate()).isEqualTo(11.0 / 24);
    assertEquals(13, stats.missCount());
    assertThat(stats.missRate()).isEqualTo(13.0 / 24);
    assertEquals(17, stats.loadSuccessCount());
    assertEquals(19, stats.loadExceptionCount());
    assertThat(stats.loadExceptionRate()).isEqualTo(19.0 / 36);
    assertEquals(17 + 19, stats.loadCount());
    assertEquals(23, stats.totalLoadTime());
    assertThat(stats.averageLoadPenalty()).isEqualTo(23.0 / (17 + 19));
    assertEquals(27, stats.evictionCount());
  }

  public void testMinus() {
    CacheStats one = new CacheStats(11, 13, 17, 19, 23, 27);
    CacheStats two = new CacheStats(53, 47, 43, 41, 37, 31);

    CacheStats diff = two.minus(one);
    assertEquals(76, diff.requestCount());
    assertEquals(42, diff.hitCount());
    assertThat(diff.hitRate()).isEqualTo(42.0 / 76);
    assertEquals(34, diff.missCount());
    assertThat(diff.missRate()).isEqualTo(34.0 / 76);
    assertEquals(26, diff.loadSuccessCount());
    assertEquals(22, diff.loadExceptionCount());
    assertThat(diff.loadExceptionRate()).isEqualTo(22.0 / 48);
    assertEquals(26 + 22, diff.loadCount());
    assertEquals(14, diff.totalLoadTime());
    assertThat(diff.averageLoadPenalty()).isEqualTo(14.0 / (26 + 22));
    assertEquals(4, diff.evictionCount());

    assertEquals(new CacheStats(0, 0, 0, 0, 0, 0), one.minus(two));
  }

  public void testPlus() {
    CacheStats one = new CacheStats(11, 13, 15, 13, 11, 9);
    CacheStats two = new CacheStats(53, 47, 41, 39, 37, 35);

    CacheStats sum = two.plus(one);
    assertEquals(124, sum.requestCount());
    assertEquals(64, sum.hitCount());
    assertThat(sum.hitRate()).isEqualTo(64.0 / 124);
    assertEquals(60, sum.missCount());
    assertThat(sum.missRate()).isEqualTo(60.0 / 124);
    assertEquals(56, sum.loadSuccessCount());
    assertEquals(52, sum.loadExceptionCount());
    assertThat(sum.loadExceptionRate()).isEqualTo(52.0 / 108);
    assertEquals(56 + 52, sum.loadCount());
    assertEquals(48, sum.totalLoadTime());
    assertThat(sum.averageLoadPenalty()).isEqualTo(48.0 / (56 + 52));
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
    assertThat(sum.hitRate()).isEqualTo(1.0);
    assertEquals(Long.MAX_VALUE, sum.missCount());
    assertThat(sum.missRate()).isEqualTo(1.0);
    assertEquals(Long.MAX_VALUE, sum.loadSuccessCount());
    assertEquals(Long.MAX_VALUE, sum.loadExceptionCount());
    assertThat(sum.loadExceptionRate()).isEqualTo(1.0);
    assertEquals(Long.MAX_VALUE, sum.loadCount());
    assertEquals(Long.MAX_VALUE, sum.totalLoadTime());
    assertThat(sum.averageLoadPenalty()).isEqualTo(1.0);
    assertEquals(Long.MAX_VALUE, sum.evictionCount());

    assertEquals(sum, maxCacheStats.plus(smallCacheStats));
  }
}
