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
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.cache.TestingCacheLoaders.IncrementingLoader;
import com.google.common.testing.FakeTicker;
import junit.framework.TestCase;

/**
 * Tests relating to automatic cache refreshing.
 *
 * @author Charles Fry
 */
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
      assertEquals(Integer.valueOf(i), cache.getUnchecked(i));
      expectedLoads++;
      assertEquals(expectedLoads, loader.getLoadCount());
      assertEquals(expectedReloads, loader.getReloadCount());
      ticker.advance(1, MILLISECONDS);
    }

    assertEquals(Integer.valueOf(0), cache.getUnchecked(0));
    assertEquals(Integer.valueOf(1), cache.getUnchecked(1));
    assertEquals(Integer.valueOf(2), cache.getUnchecked(2));
    assertEquals(expectedLoads, loader.getLoadCount());
    assertEquals(expectedReloads, loader.getReloadCount());

    // refresh 0
    ticker.advance(1, MILLISECONDS);
    assertEquals(Integer.valueOf(1), cache.getUnchecked(0));
    expectedReloads++;
    assertEquals(Integer.valueOf(1), cache.getUnchecked(1));
    assertEquals(Integer.valueOf(2), cache.getUnchecked(2));
    assertEquals(expectedLoads, loader.getLoadCount());
    assertEquals(expectedReloads, loader.getReloadCount());

    // write to 1 to delay its refresh
    cache.asMap().put(1, -1);
    ticker.advance(1, MILLISECONDS);
    assertEquals(Integer.valueOf(1), cache.getUnchecked(0));
    assertEquals(Integer.valueOf(-1), cache.getUnchecked(1));
    assertEquals(Integer.valueOf(2), cache.getUnchecked(2));
    assertEquals(expectedLoads, loader.getLoadCount());
    assertEquals(expectedReloads, loader.getReloadCount());

    // refresh 2
    ticker.advance(1, MILLISECONDS);
    assertEquals(Integer.valueOf(1), cache.getUnchecked(0));
    assertEquals(Integer.valueOf(-1), cache.getUnchecked(1));
    assertEquals(Integer.valueOf(3), cache.getUnchecked(2));
    expectedReloads++;
    assertEquals(expectedLoads, loader.getLoadCount());
    assertEquals(expectedReloads, loader.getReloadCount());

    ticker.advance(1, MILLISECONDS);
    assertEquals(Integer.valueOf(1), cache.getUnchecked(0));
    assertEquals(Integer.valueOf(-1), cache.getUnchecked(1));
    assertEquals(Integer.valueOf(3), cache.getUnchecked(2));
    assertEquals(expectedLoads, loader.getLoadCount());
    assertEquals(expectedReloads, loader.getReloadCount());

    // refresh 0 and 1
    ticker.advance(1, MILLISECONDS);
    assertEquals(Integer.valueOf(2), cache.getUnchecked(0));
    expectedReloads++;
    assertEquals(Integer.valueOf(0), cache.getUnchecked(1));
    expectedReloads++;
    assertEquals(Integer.valueOf(3), cache.getUnchecked(2));
    assertEquals(expectedLoads, loader.getLoadCount());
    assertEquals(expectedReloads, loader.getReloadCount());
  }
}
