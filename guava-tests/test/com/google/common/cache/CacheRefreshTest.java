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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.cache.TestingCacheLoaders.IncrementingLoader;
import com.google.common.testing.FakeTicker;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests relating to automatic cache refreshing.
 *
 * @author Charles Fry
 */
@GwtIncompatible
@J2ktIncompatible
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

  /**
   * Tests that a refresh that was started before {@code invalidateAll()} does not clobber an
   * in-flight load that was started after it, which previously corrupted the segment's count and
   * left the cache unable to refresh or invalidate the entry ever again.
   */
  public void testInvalidateAllDuringRefresh_doesNotCorruptCount() throws Exception {
    CountDownLatch refreshStarted = new CountDownLatch(1);
    CountDownLatch releaseRefresh = new CountDownLatch(1);
    CountDownLatch loadStarted = new CountDownLatch(1);
    CountDownLatch releaseLoad = new CountDownLatch(1);
    AtomicInteger loads = new AtomicInteger();
    CacheLoader<String, String> loader =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws Exception {
            int n = loads.incrementAndGet();
            if (n == 2) {
              loadStarted.countDown();
              releaseLoad.await();
            }
            return "loaded-" + n;
          }

          @Override
          public ListenableFuture<String> reload(String key, String oldValue) throws Exception {
            refreshStarted.countDown();
            releaseRefresh.await();
            return Futures.immediateFuture("refreshed");
          }
        };
    LoadingCache<String, String> cache = CacheBuilder.newBuilder().build(loader);
    ExecutorService executor = Executors.newCachedThreadPool();
    try {
      assertThat(cache.getUnchecked("k")).isEqualTo("loaded-1");

      // Start a refresh and leave it in flight, blocked inside reload().
      Future<?> refresh = executor.submit(() -> cache.refresh("k"));
      refreshStarted.await();

      // Discard everything, then start a new load of the same key, also left in flight.
      cache.invalidateAll();
      Future<String> load = executor.submit(() -> cache.getUnchecked("k"));
      loadStarted.await();

      // The stale refresh completes while the newer load is still pending. It must not replace the
      // pending load's reference or alter the entry count.
      releaseRefresh.countDown();
      refresh.get();
      releaseLoad.countDown();
      assertThat(load.get()).isEqualTo("loaded-2");

      assertThat(cache.size()).isEqualTo(1);
      assertThat(cache.getIfPresent("k")).isEqualTo("loaded-2");

      // The entry must remain invalidatable, and must be reloaded afterwards.
      cache.invalidateAll();
      assertThat(cache.size()).isEqualTo(0);
      assertThat(cache.getIfPresent("k")).isNull();
      assertThat(cache.getUnchecked("k")).isEqualTo("loaded-3");
    } finally {
      executor.shutdownNow();
    }
  }
}
