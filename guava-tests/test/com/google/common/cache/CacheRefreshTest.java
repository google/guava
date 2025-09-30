/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.cache;

import static com.google.common.cache.TestingCacheLoaders.incrementingLoader;
import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.cache.TestingCacheLoaders.IncrementingLoader;
import com.google.common.testing.FakeTicker;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests relating to automatic cache refreshing.
 *
 * @author Charles Fry
 */
@NullUnmarked
public class CacheRefreshTest extends TestCase {
  public void testAutoRefresh() {
    FakeTicker ticker = new FakeTicker();
    IncrementingLoader loader = incrementingLoader();
    LoadingCache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .refreshAfterWrite(3, MILLISECONDS)
            .expireAfterWrite(6, MILLISECONDS)
            .lenientParsing()
            .ticker(ticker)
            .build(loader);
    int expectedLoads = 0;
    int expectedReloads = 0;
    for (int i = 0; i < 3; i++) {
      assertThat(cache.getUnchecked(i)).isEqualTo(i);
      expectedLoads++;
      assertThat(loader.getLoadCount()).isEqualTo(expectedLoads);
      assertThat(loader.getReloadCount()).isEqualTo(expectedReloads);
      ticker.advance(1, MILLISECONDS);
    }

    assertThat(cache.getUnchecked(0)).isEqualTo(0);
    assertThat(cache.getUnchecked(1)).isEqualTo(1);
    assertThat(cache.getUnchecked(2)).isEqualTo(2);
    assertThat(loader.getLoadCount()).isEqualTo(expectedLoads);
    assertThat(loader.getReloadCount()).isEqualTo(expectedReloads);

    // refresh 0
    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(0)).isEqualTo(1);
    expectedReloads++;
    assertThat(cache.getUnchecked(1)).isEqualTo(1);
    assertThat(cache.getUnchecked(2)).isEqualTo(2);
    assertThat(loader.getLoadCount()).isEqualTo(expectedLoads);
    assertThat(loader.getReloadCount()).isEqualTo(expectedReloads);

    // write to 1 to delay its refresh
    cache.asMap().put(1, -1);
    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(0)).isEqualTo(1);
    assertThat(cache.getUnchecked(1)).isEqualTo(-1);
    assertThat(cache.getUnchecked(2)).isEqualTo(2);
    assertThat(loader.getLoadCount()).isEqualTo(expectedLoads);
    assertThat(loader.getReloadCount()).isEqualTo(expectedReloads);

    // refresh 2
    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(0)).isEqualTo(1);
    assertThat(cache.getUnchecked(1)).isEqualTo(-1);
    assertThat(cache.getUnchecked(2)).isEqualTo(3);
    expectedReloads++;
    assertThat(loader.getLoadCount()).isEqualTo(expectedLoads);
    assertThat(loader.getReloadCount()).isEqualTo(expectedReloads);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(0)).isEqualTo(1);
    assertThat(cache.getUnchecked(1)).isEqualTo(-1);
    assertThat(cache.getUnchecked(2)).isEqualTo(3);
    assertThat(loader.getLoadCount()).isEqualTo(expectedLoads);
    assertThat(loader.getReloadCount()).isEqualTo(expectedReloads);

    // refresh 0 and 1
    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(0)).isEqualTo(2);
    expectedReloads++;
    assertThat(cache.getUnchecked(1)).isEqualTo(0);
    expectedReloads++;
    assertThat(cache.getUnchecked(2)).isEqualTo(3);
    assertThat(loader.getLoadCount()).isEqualTo(expectedLoads);
    assertThat(loader.getReloadCount()).isEqualTo(expectedReloads);
  }
}
