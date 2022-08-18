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

import static com.google.common.cache.TestingCacheLoaders.identityLoader;
import static com.google.common.cache.TestingRemovalListeners.countingRemovalListener;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.cache.TestingCacheLoaders.IdentityLoader;
import com.google.common.cache.TestingRemovalListeners.CountingRemovalListener;
import com.google.common.cache.TestingRemovalListeners.QueuingRemovalListener;
import com.google.common.collect.Iterators;
import com.google.common.testing.FakeTicker;
import com.google.common.util.concurrent.Callables;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;

/**
 * Tests relating to cache expiration: make sure entries expire at the right times, make sure
 * expired entries don't show up, etc.
 *
 * @author mike nonemacher
 */
@SuppressWarnings("deprecation") // tests of deprecated method
public class CacheExpirationTest extends TestCase {

  private static final long EXPIRING_TIME = 1000;
  private static final int VALUE_PREFIX = 12345;
  private static final String KEY_PREFIX = "key prefix:";

  public void testExpiration_expireAfterWrite() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    checkExpiration(cache, loader, ticker, removalListener);
  }

  public void testExpiration_expireAfterAccess() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    checkExpiration(cache, loader, ticker, removalListener);
  }

  private void checkExpiration(
      LoadingCache<String, Integer> cache,
      WatchedCreatorLoader loader,
      FakeTicker ticker,
      CountingRemovalListener<String, Integer> removalListener) {

    for (int i = 0; i < 10; i++) {
      assertEquals(Integer.valueOf(VALUE_PREFIX + i), cache.getUnchecked(KEY_PREFIX + i));
    }

    for (int i = 0; i < 10; i++) {
      loader.reset();
      assertEquals(Integer.valueOf(VALUE_PREFIX + i), cache.getUnchecked(KEY_PREFIX + i));
      assertFalse("Creator should not have been called @#" + i, loader.wasCalled());
    }

    CacheTesting.expireEntries((LoadingCache<?, ?>) cache, EXPIRING_TIME, ticker);

    assertEquals("Map must be empty by now", 0, cache.size());
    assertEquals("Eviction notifications must be received", 10, removalListener.getCount());

    CacheTesting.expireEntries((LoadingCache<?, ?>) cache, EXPIRING_TIME, ticker);
    // ensure that no new notifications are sent
    assertEquals("Eviction notifications must be received", 10, removalListener.getCount());
  }

  public void testExpiringGet_expireAfterWrite() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    runExpirationTest(cache, loader, ticker, removalListener);
  }

  public void testExpiringGet_expireAfterAccess() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    runExpirationTest(cache, loader, ticker, removalListener);
  }

  private void runExpirationTest(
      LoadingCache<String, Integer> cache,
      WatchedCreatorLoader loader,
      FakeTicker ticker,
      CountingRemovalListener<String, Integer> removalListener) {

    for (int i = 0; i < 10; i++) {
      assertEquals(Integer.valueOf(VALUE_PREFIX + i), cache.getUnchecked(KEY_PREFIX + i));
    }

    for (int i = 0; i < 10; i++) {
      loader.reset();
      assertEquals(Integer.valueOf(VALUE_PREFIX + i), cache.getUnchecked(KEY_PREFIX + i));
      assertFalse("Loader should NOT have been called @#" + i, loader.wasCalled());
    }

    // wait for entries to expire, but don't call expireEntries
    ticker.advance(EXPIRING_TIME * 10, MILLISECONDS);

    // add a single unexpired entry
    cache.getUnchecked(KEY_PREFIX + 11);

    // collections views shouldn't expose expired entries
    assertEquals(1, Iterators.size(cache.asMap().entrySet().iterator()));
    assertEquals(1, Iterators.size(cache.asMap().keySet().iterator()));
    assertEquals(1, Iterators.size(cache.asMap().values().iterator()));

    CacheTesting.expireEntries((LoadingCache<?, ?>) cache, EXPIRING_TIME, ticker);

    for (int i = 0; i < 11; i++) {
      assertFalse(cache.asMap().containsKey(KEY_PREFIX + i));
    }
    assertEquals(11, removalListener.getCount());

    for (int i = 0; i < 10; i++) {
      assertFalse(cache.asMap().containsKey(KEY_PREFIX + i));
      loader.reset();
      assertEquals(Integer.valueOf(VALUE_PREFIX + i), cache.getUnchecked(KEY_PREFIX + i));
      assertTrue("Creator should have been called @#" + i, loader.wasCalled());
    }

    // expire new values we just created
    CacheTesting.expireEntries((LoadingCache<?, ?>) cache, EXPIRING_TIME, ticker);
    assertEquals("Eviction notifications must be received", 21, removalListener.getCount());

    CacheTesting.expireEntries((LoadingCache<?, ?>) cache, EXPIRING_TIME, ticker);
    // ensure that no new notifications are sent
    assertEquals("Eviction notifications must be received", 21, removalListener.getCount());
  }

  public void testRemovalListener_expireAfterWrite() {
    FakeTicker ticker = new FakeTicker();
    final AtomicInteger evictionCount = new AtomicInteger();
    final AtomicInteger applyCount = new AtomicInteger();
    final AtomicInteger totalSum = new AtomicInteger();

    RemovalListener<Integer, AtomicInteger> removalListener =
        new RemovalListener<Integer, AtomicInteger>() {
          @Override
          public void onRemoval(RemovalNotification<Integer, AtomicInteger> notification) {
            if (notification.wasEvicted()) {
              evictionCount.incrementAndGet();
              totalSum.addAndGet(notification.getValue().get());
            }
          }
        };

    CacheLoader<Integer, AtomicInteger> loader =
        new CacheLoader<Integer, AtomicInteger>() {
          @Override
          public AtomicInteger load(Integer key) {
            applyCount.incrementAndGet();
            return new AtomicInteger();
          }
        };

    LoadingCache<Integer, AtomicInteger> cache =
        CacheBuilder.newBuilder()
            .removalListener(removalListener)
            .expireAfterWrite(10, MILLISECONDS)
            .ticker(ticker)
            .build(loader);

    // Increment 100 times
    for (int i = 0; i < 100; ++i) {
      cache.getUnchecked(10).incrementAndGet();
      ticker.advance(1, MILLISECONDS);
    }

    assertEquals(evictionCount.get() + 1, applyCount.get());
    int remaining = cache.getUnchecked(10).get();
    assertEquals(100, totalSum.get() + remaining);
  }

  public void testRemovalScheduler_expireAfterWrite() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    runRemovalScheduler(cache, removalListener, loader, ticker, KEY_PREFIX, EXPIRING_TIME);
  }

  public void testRemovalScheduler_expireAfterAccess() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    runRemovalScheduler(cache, removalListener, loader, ticker, KEY_PREFIX, EXPIRING_TIME);
  }

  public void testRemovalScheduler_expireAfterBoth() {
    FakeTicker ticker = new FakeTicker();
    CountingRemovalListener<String, Integer> removalListener = countingRemovalListener();
    WatchedCreatorLoader loader = new WatchedCreatorLoader();
    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(EXPIRING_TIME, MILLISECONDS)
            .expireAfterWrite(EXPIRING_TIME, MILLISECONDS)
            .removalListener(removalListener)
            .ticker(ticker)
            .build(loader);
    runRemovalScheduler(cache, removalListener, loader, ticker, KEY_PREFIX, EXPIRING_TIME);
  }

  public void testExpirationOrder_access() {
    // test lru within a single segment
    FakeTicker ticker = new FakeTicker();
    IdentityLoader<Integer> loader = identityLoader();
    LoadingCache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(11, MILLISECONDS)
            .ticker(ticker)
            .build(loader);
    for (int i = 0; i < 10; i++) {
      cache.getUnchecked(i);
      ticker.advance(1, MILLISECONDS);
    }
    Set<Integer> keySet = cache.asMap().keySet();
    assertThat(keySet).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    // 0 expires
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);

    // reorder
    getAll(cache, asList(0, 1, 2));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(2, MILLISECONDS);
    assertThat(keySet).containsExactly(3, 4, 5, 6, 7, 8, 9, 0, 1, 2);

    // 3 expires
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(4, 5, 6, 7, 8, 9, 0, 1, 2);

    // reorder
    getAll(cache, asList(5, 7, 9));
    CacheTesting.drainRecencyQueues(cache);
    assertThat(keySet).containsExactly(4, 6, 8, 0, 1, 2, 5, 7, 9);

    // 4 expires
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(6, 8, 0, 1, 2, 5, 7, 9);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(6, 8, 0, 1, 2, 5, 7, 9);

    // 6 expires
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(8, 0, 1, 2, 5, 7, 9);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(8, 0, 1, 2, 5, 7, 9);

    // 8 expires
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(0, 1, 2, 5, 7, 9);
  }

  public void testExpirationOrder_write() throws ExecutionException {
    // test lru within a single segment
    FakeTicker ticker = new FakeTicker();
    IdentityLoader<Integer> loader = identityLoader();
    LoadingCache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterWrite(11, MILLISECONDS)
            .ticker(ticker)
            .build(loader);
    for (int i = 0; i < 10; i++) {
      cache.getUnchecked(i);
      ticker.advance(1, MILLISECONDS);
    }
    Set<Integer> keySet = cache.asMap().keySet();
    assertThat(keySet).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    // 0 expires
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);

    // get doesn't stop 1 from expiring
    getAll(cache, asList(0, 1, 2));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(2, 3, 4, 5, 6, 7, 8, 9, 0);

    // get(K, Callable) doesn't stop 2 from expiring
    Integer unused = cache.get(2, Callables.returning(-2));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(3, 4, 5, 6, 7, 8, 9, 0);

    // asMap.put saves 3
    cache.asMap().put(3, -3);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(4, 5, 6, 7, 8, 9, 0, 3);

    // asMap.replace saves 4
    cache.asMap().replace(4, -4);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(5, 6, 7, 8, 9, 0, 3, 4);

    // 5 expires
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(6, 7, 8, 9, 0, 3, 4);
  }

  public void testExpirationOrder_writeAccess() throws ExecutionException {
    // test lru within a single segment
    FakeTicker ticker = new FakeTicker();
    IdentityLoader<Integer> loader = identityLoader();
    LoadingCache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterWrite(5, MILLISECONDS)
            .expireAfterAccess(3, MILLISECONDS)
            .ticker(ticker)
            .build(loader);
    for (int i = 0; i < 5; i++) {
      cache.getUnchecked(i);
    }
    ticker.advance(1, MILLISECONDS);
    for (int i = 5; i < 10; i++) {
      cache.getUnchecked(i);
    }
    ticker.advance(1, MILLISECONDS);

    Set<Integer> keySet = cache.asMap().keySet();
    assertThat(keySet).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    // get saves 1, 3; 0, 2, 4 expire
    getAll(cache, asList(1, 3));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(5, 6, 7, 8, 9, 1, 3);

    // get saves 6, 8; 5, 7, 9 expire
    getAll(cache, asList(6, 8));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(1, 3, 6, 8);

    // get fails to save 1, put saves 3
    cache.asMap().put(3, -3);
    getAll(cache, asList(1));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(6, 8, 3);

    // get(K, Callable) fails to save 8, replace saves 6
    cache.asMap().replace(6, -6);
    Integer unused = cache.get(8, Callables.returning(-8));
    CacheTesting.drainRecencyQueues(cache);
    ticker.advance(1, MILLISECONDS);
    assertThat(keySet).containsExactly(3, 6);
  }

  public void testExpiration_invalidateAll() {
    FakeTicker ticker = new FakeTicker();
    QueuingRemovalListener<Integer, Integer> listener =
        TestingRemovalListeners.queuingRemovalListener();
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .removalListener(listener)
            .ticker(ticker)
            .build();
    cache.put(1, 1);
    ticker.advance(10, TimeUnit.MINUTES);
    cache.invalidateAll();

    assertThat(listener.poll().getCause()).isEqualTo(RemovalCause.EXPIRED);
  }

  private void runRemovalScheduler(
      LoadingCache<String, Integer> cache,
      CountingRemovalListener<String, Integer> removalListener,
      WatchedCreatorLoader loader,
      FakeTicker ticker,
      String keyPrefix,
      long ttl) {

    int shift1 = 10 + VALUE_PREFIX;
    loader.setValuePrefix(shift1);
    // fill with initial data
    for (int i = 0; i < 10; i++) {
      assertEquals(Integer.valueOf(i + shift1), cache.getUnchecked(keyPrefix + i));
    }
    assertEquals(10, CacheTesting.expirationQueueSize(cache));
    assertEquals(0, removalListener.getCount());

    // wait, so that entries have just 10 ms to live
    ticker.advance(ttl * 2 / 3, MILLISECONDS);

    assertEquals(10, CacheTesting.expirationQueueSize(cache));
    assertEquals(0, removalListener.getCount());

    int shift2 = shift1 + 10;
    loader.setValuePrefix(shift2);
    // fill with new data - has to live for 20 ms more
    for (int i = 0; i < 10; i++) {
      cache.invalidate(keyPrefix + i);
      assertEquals(
          "key: " + keyPrefix + i, Integer.valueOf(i + shift2), cache.getUnchecked(keyPrefix + i));
    }
    assertEquals(10, CacheTesting.expirationQueueSize(cache));
    assertEquals(10, removalListener.getCount()); // these are the invalidated ones

    // old timeouts must expire after this wait
    ticker.advance(ttl * 2 / 3, MILLISECONDS);

    assertEquals(10, CacheTesting.expirationQueueSize(cache));
    assertEquals(10, removalListener.getCount());

    // check that new values are still there - they still have 10 ms to live
    for (int i = 0; i < 10; i++) {
      loader.reset();
      assertEquals(Integer.valueOf(i + shift2), cache.getUnchecked(keyPrefix + i));
      assertFalse("Creator should NOT have been called @#" + i, loader.wasCalled());
    }
    assertEquals(10, removalListener.getCount());
  }

  private static void getAll(LoadingCache<Integer, Integer> cache, List<Integer> keys) {
    for (int i : keys) {
      cache.getUnchecked(i);
    }
  }

  private static class WatchedCreatorLoader extends CacheLoader<String, Integer> {
    boolean wasCalled = false; // must be set in load()
    String keyPrefix = KEY_PREFIX;
    int valuePrefix = VALUE_PREFIX;

    public WatchedCreatorLoader() {}

    public void reset() {
      wasCalled = false;
    }

    public boolean wasCalled() {
      return wasCalled;
    }

    public void setKeyPrefix(String keyPrefix) {
      this.keyPrefix = keyPrefix;
    }

    public void setValuePrefix(int valuePrefix) {
      this.valuePrefix = valuePrefix;
    }

    @Override
    public Integer load(String key) {
      wasCalled = true;
      return valuePrefix + Integer.parseInt(key.substring(keyPrefix.length()));
    }
  }
}
