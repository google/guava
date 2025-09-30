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
    assertThat(stats.requestCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);
    assertThat(stats.hitRate()).isEqualTo(1.0);
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.missRate()).isEqualTo(0.0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.loadExceptionRate()).isEqualTo(0.0);
    assertThat(stats.loadCount()).isEqualTo(0);
    assertThat(stats.totalLoadTime()).isEqualTo(0);
    assertThat(stats.averageLoadPenalty()).isEqualTo(0.0);
    assertThat(stats.evictionCount()).isEqualTo(0);
  }

  public void testSingle() {
    CacheStats stats = new CacheStats(11, 13, 17, 19, 23, 27);
    assertThat(stats.requestCount()).isEqualTo(24);
    assertThat(stats.hitCount()).isEqualTo(11);
    assertThat(stats.hitRate()).isEqualTo(11.0 / 24);
    assertThat(stats.missCount()).isEqualTo(13);
    assertThat(stats.missRate()).isEqualTo(13.0 / 24);
    assertThat(stats.loadSuccessCount()).isEqualTo(17);
    assertThat(stats.loadExceptionCount()).isEqualTo(19);
    assertThat(stats.loadExceptionRate()).isEqualTo(19.0 / 36);
    assertThat(stats.loadCount()).isEqualTo(17 + 19);
    assertThat(stats.totalLoadTime()).isEqualTo(23);
    assertThat(stats.averageLoadPenalty()).isEqualTo(23.0 / (17 + 19));
    assertThat(stats.evictionCount()).isEqualTo(27);
  }

  public void testMinus() {
    CacheStats one = new CacheStats(11, 13, 17, 19, 23, 27);
    CacheStats two = new CacheStats(53, 47, 43, 41, 37, 31);

    CacheStats diff = two.minus(one);
    assertThat(diff.requestCount()).isEqualTo(76);
    assertThat(diff.hitCount()).isEqualTo(42);
    assertThat(diff.hitRate()).isEqualTo(42.0 / 76);
    assertThat(diff.missCount()).isEqualTo(34);
    assertThat(diff.missRate()).isEqualTo(34.0 / 76);
    assertThat(diff.loadSuccessCount()).isEqualTo(26);
    assertThat(diff.loadExceptionCount()).isEqualTo(22);
    assertThat(diff.loadExceptionRate()).isEqualTo(22.0 / 48);
    assertThat(diff.loadCount()).isEqualTo(26 + 22);
    assertThat(diff.totalLoadTime()).isEqualTo(14);
    assertThat(diff.averageLoadPenalty()).isEqualTo(14.0 / (26 + 22));
    assertThat(diff.evictionCount()).isEqualTo(4);

    assertThat(one.minus(two)).isEqualTo(new CacheStats(0, 0, 0, 0, 0, 0));
  }

  public void testPlus() {
    CacheStats one = new CacheStats(11, 13, 15, 13, 11, 9);
    CacheStats two = new CacheStats(53, 47, 41, 39, 37, 35);

    CacheStats sum = two.plus(one);
    assertThat(sum.requestCount()).isEqualTo(124);
    assertThat(sum.hitCount()).isEqualTo(64);
    assertThat(sum.hitRate()).isEqualTo(64.0 / 124);
    assertThat(sum.missCount()).isEqualTo(60);
    assertThat(sum.missRate()).isEqualTo(60.0 / 124);
    assertThat(sum.loadSuccessCount()).isEqualTo(56);
    assertThat(sum.loadExceptionCount()).isEqualTo(52);
    assertThat(sum.loadExceptionRate()).isEqualTo(52.0 / 108);
    assertThat(sum.loadCount()).isEqualTo(56 + 52);
    assertThat(sum.totalLoadTime()).isEqualTo(48);
    assertThat(sum.averageLoadPenalty()).isEqualTo(48.0 / (56 + 52));
    assertThat(sum.evictionCount()).isEqualTo(44);

    assertThat(one.plus(two)).isEqualTo(sum);
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
    assertThat(sum.requestCount()).isEqualTo(Long.MAX_VALUE);
    assertThat(sum.hitCount()).isEqualTo(Long.MAX_VALUE);
    assertThat(sum.hitRate()).isEqualTo(1.0);
    assertThat(sum.missCount()).isEqualTo(Long.MAX_VALUE);
    assertThat(sum.missRate()).isEqualTo(1.0);
    assertThat(sum.loadSuccessCount()).isEqualTo(Long.MAX_VALUE);
    assertThat(sum.loadExceptionCount()).isEqualTo(Long.MAX_VALUE);
    assertThat(sum.loadExceptionRate()).isEqualTo(1.0);
    assertThat(sum.loadCount()).isEqualTo(Long.MAX_VALUE);
    assertThat(sum.totalLoadTime()).isEqualTo(Long.MAX_VALUE);
    assertThat(sum.averageLoadPenalty()).isEqualTo(1.0);
    assertThat(sum.evictionCount()).isEqualTo(Long.MAX_VALUE);

    assertThat(maxCacheStats.plus(smallCacheStats)).isEqualTo(sum);
  }
}
