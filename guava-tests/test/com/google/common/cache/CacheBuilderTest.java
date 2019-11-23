/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.cache.TestingCacheLoaders.constantLoader;
import static com.google.common.cache.TestingCacheLoaders.identityLoader;
import static com.google.common.cache.TestingRemovalListeners.countingRemovalListener;
import static com.google.common.cache.TestingRemovalListeners.nullRemovalListener;
import static com.google.common.cache.TestingRemovalListeners.queuingRemovalListener;
import static com.google.common.cache.TestingWeighers.constantWeigher;
import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ticker;
import com.google.common.cache.TestingRemovalListeners.CountingRemovalListener;
import com.google.common.cache.TestingRemovalListeners.QueuingRemovalListener;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.testing.NullPointerTester;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;

/** Unit tests for CacheBuilder. */
@GwtCompatible(emulated = true)
public class CacheBuilderTest extends TestCase {

  public void testNewBuilder() {
    CacheLoader<Object, Integer> loader = constantLoader(1);

    LoadingCache<String, Integer> cache =
        CacheBuilder.newBuilder().removalListener(countingRemovalListener()).build(loader);

    assertEquals(Integer.valueOf(1), cache.getUnchecked("one"));
    assertEquals(1, cache.size());
  }

  public void testInitialCapacity_negative() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    try {
      builder.initialCapacity(-1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testInitialCapacity_setTwice() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().initialCapacity(16);
    try {
      // even to the same value is not allowed
      builder.initialCapacity(16);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // CacheTesting
  public void testInitialCapacity_small() {
    LoadingCache<?, ?> cache = CacheBuilder.newBuilder().initialCapacity(5).build(identityLoader());
    LocalCache<?, ?> map = CacheTesting.toLocalCache(cache);

    assertThat(map.segments).hasLength(4);
    assertEquals(2, map.segments[0].table.length());
    assertEquals(2, map.segments[1].table.length());
    assertEquals(2, map.segments[2].table.length());
    assertEquals(2, map.segments[3].table.length());
  }

  @GwtIncompatible // CacheTesting
  public void testInitialCapacity_smallest() {
    LoadingCache<?, ?> cache = CacheBuilder.newBuilder().initialCapacity(0).build(identityLoader());
    LocalCache<?, ?> map = CacheTesting.toLocalCache(cache);

    assertThat(map.segments).hasLength(4);
    // 1 is as low as it goes, not 0. it feels dirty to know this/test this.
    assertEquals(1, map.segments[0].table.length());
    assertEquals(1, map.segments[1].table.length());
    assertEquals(1, map.segments[2].table.length());
    assertEquals(1, map.segments[3].table.length());
  }

  public void testInitialCapacity_large() {
    CacheBuilder.newBuilder().initialCapacity(Integer.MAX_VALUE);
    // that the builder didn't blow up is enough;
    // don't actually create this monster!
  }

  public void testConcurrencyLevel_zero() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    try {
      builder.concurrencyLevel(0);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcurrencyLevel_setTwice() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().concurrencyLevel(16);
    try {
      // even to the same value is not allowed
      builder.concurrencyLevel(16);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // CacheTesting
  public void testConcurrencyLevel_small() {
    LoadingCache<?, ?> cache =
        CacheBuilder.newBuilder().concurrencyLevel(1).build(identityLoader());
    LocalCache<?, ?> map = CacheTesting.toLocalCache(cache);
    assertThat(map.segments).hasLength(1);
  }

  public void testConcurrencyLevel_large() {
    CacheBuilder.newBuilder().concurrencyLevel(Integer.MAX_VALUE);
    // don't actually build this beast
  }

  public void testMaximumSize_negative() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    try {
      builder.maximumSize(-1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMaximumSize_setTwice() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().maximumSize(16);
    try {
      // even to the same value is not allowed
      builder.maximumSize(16);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // maximumWeight
  public void testMaximumSize_andWeight() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().maximumSize(16);
    try {
      builder.maximumWeight(16);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // digs into internals of the non-GWT implementation
  public void testMaximumSize_largerThanInt() {
    CacheBuilder<Object, Object> builder =
        CacheBuilder.newBuilder().initialCapacity(512).maximumSize(Long.MAX_VALUE);
    LocalCache<?, ?> cache = ((LocalCache.LocalManualCache<?, ?>) builder.build()).localCache;
    assertThat(cache.segments.length * cache.segments[0].table.length()).isEqualTo(512);
  }

  @GwtIncompatible // maximumWeight
  public void testMaximumWeight_negative() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    try {
      builder.maximumWeight(-1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // maximumWeight
  public void testMaximumWeight_setTwice() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().maximumWeight(16);
    try {
      // even to the same value is not allowed
      builder.maximumWeight(16);
      fail();
    } catch (IllegalStateException expected) {
    }
    try {
      builder.maximumSize(16);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // maximumWeight
  public void testMaximumWeight_withoutWeigher() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().maximumWeight(1);
    try {
      builder.build(identityLoader());
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // weigher
  public void testWeigher_withoutMaximumWeight() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().weigher(constantWeigher(42));
    try {
      builder.build(identityLoader());
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // weigher
  public void testWeigher_withMaximumSize() {
    try {
      CacheBuilder.newBuilder().weigher(constantWeigher(42)).maximumSize(1);
      fail();
    } catch (IllegalStateException expected) {
    }
    try {
      CacheBuilder.newBuilder().maximumSize(1).weigher(constantWeigher(42));
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // weakKeys
  public void testKeyStrengthSetTwice() {
    CacheBuilder<Object, Object> builder1 = CacheBuilder.newBuilder().weakKeys();
    try {
      builder1.weakKeys();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // weakValues
  public void testValueStrengthSetTwice() {
    CacheBuilder<Object, Object> builder1 = CacheBuilder.newBuilder().weakValues();
    try {
      builder1.weakValues();
      fail();
    } catch (IllegalStateException expected) {
    }
    try {
      builder1.softValues();
      fail();
    } catch (IllegalStateException expected) {
    }

    CacheBuilder<Object, Object> builder2 = CacheBuilder.newBuilder().softValues();
    try {
      builder2.softValues();
      fail();
    } catch (IllegalStateException expected) {
    }
    try {
      builder2.weakValues();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // java.time.Duration
  public void testLargeDurationsAreOk() {
    java.time.Duration threeHundredYears = java.time.Duration.ofDays(365 * 300);
    CacheBuilder<Object, Object> builder =
        CacheBuilder.newBuilder()
            .expireAfterWrite(threeHundredYears)
            .expireAfterAccess(threeHundredYears)
            .refreshAfterWrite(threeHundredYears);
  }

  public void testTimeToLive_negative() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    try {
      builder.expireAfterWrite(-1, SECONDS);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // java.time.Duration
  public void testTimeToLive_negative_duration() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    try {
      builder.expireAfterWrite(java.time.Duration.ofSeconds(-1));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testTimeToLive_small() {
    CacheBuilder.newBuilder().expireAfterWrite(1, NANOSECONDS).build(identityLoader());
    // well, it didn't blow up.
  }

  public void testTimeToLive_setTwice() {
    CacheBuilder<Object, Object> builder =
        CacheBuilder.newBuilder().expireAfterWrite(3600, SECONDS);
    try {
      // even to the same value is not allowed
      builder.expireAfterWrite(3600, SECONDS);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // java.time.Duration
  public void testTimeToLive_setTwice_duration() {
    CacheBuilder<Object, Object> builder =
        CacheBuilder.newBuilder().expireAfterWrite(java.time.Duration.ofSeconds(3600));
    try {
      // even to the same value is not allowed
      builder.expireAfterWrite(java.time.Duration.ofSeconds(3600));
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testTimeToIdle_negative() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    try {
      builder.expireAfterAccess(-1, SECONDS);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // java.time.Duration
  public void testTimeToIdle_negative_duration() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    try {
      builder.expireAfterAccess(java.time.Duration.ofSeconds(-1));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testTimeToIdle_small() {
    CacheBuilder.newBuilder().expireAfterAccess(1, NANOSECONDS).build(identityLoader());
    // well, it didn't blow up.
  }

  public void testTimeToIdle_setTwice() {
    CacheBuilder<Object, Object> builder =
        CacheBuilder.newBuilder().expireAfterAccess(3600, SECONDS);
    try {
      // even to the same value is not allowed
      builder.expireAfterAccess(3600, SECONDS);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // java.time.Duration
  public void testTimeToIdle_setTwice_duration() {
    CacheBuilder<Object, Object> builder =
        CacheBuilder.newBuilder().expireAfterAccess(java.time.Duration.ofSeconds(3600));
    try {
      // even to the same value is not allowed
      builder.expireAfterAccess(java.time.Duration.ofSeconds(3600));
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testTimeToIdleAndToLive() {
    CacheBuilder.newBuilder()
        .expireAfterWrite(1, NANOSECONDS)
        .expireAfterAccess(1, NANOSECONDS)
        .build(identityLoader());
    // well, it didn't blow up.
  }

  @GwtIncompatible // refreshAfterWrite
  public void testRefresh_zero() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    try {
      builder.refreshAfterWrite(0, SECONDS);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // java.time.Duration
  public void testRefresh_zero_duration() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    try {
      builder.refreshAfterWrite(java.time.Duration.ZERO);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // refreshAfterWrite
  public void testRefresh_setTwice() {
    CacheBuilder<Object, Object> builder =
        CacheBuilder.newBuilder().refreshAfterWrite(3600, SECONDS);
    try {
      // even to the same value is not allowed
      builder.refreshAfterWrite(3600, SECONDS);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @GwtIncompatible // java.time.Duration
  public void testRefresh_setTwice_duration() {
    CacheBuilder<Object, Object> builder =
        CacheBuilder.newBuilder().refreshAfterWrite(java.time.Duration.ofSeconds(3600));
    try {
      // even to the same value is not allowed
      builder.refreshAfterWrite(java.time.Duration.ofSeconds(3600));
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testTicker_setTwice() {
    Ticker testTicker = Ticker.systemTicker();
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().ticker(testTicker);
    try {
      // even to the same instance is not allowed
      builder.ticker(testTicker);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testRemovalListener_setTwice() {
    RemovalListener<Object, Object> testListener = nullRemovalListener();
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().removalListener(testListener);
    try {
      // even to the same instance is not allowed
      builder = builder.removalListener(testListener);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testValuesIsNotASet() {
    assertFalse(CacheBuilder.newBuilder().build().asMap().values() instanceof Set);
  }

  @GwtIncompatible // CacheTesting
  public void testNullCache() {
    CountingRemovalListener<Object, Object> listener = countingRemovalListener();
    LoadingCache<Object, Object> nullCache =
        CacheBuilder.newBuilder().maximumSize(0).removalListener(listener).build(identityLoader());
    assertEquals(0, nullCache.size());
    Object key = new Object();
    assertSame(key, nullCache.getUnchecked(key));
    assertEquals(1, listener.getCount());
    assertEquals(0, nullCache.size());
    CacheTesting.checkEmpty(nullCache.asMap());
  }

  @GwtIncompatible // QueuingRemovalListener

  public void testRemovalNotification_clear() throws InterruptedException {
    // If a clear() happens while a computation is pending, we should not get a removal
    // notification.

    final AtomicBoolean shouldWait = new AtomicBoolean(false);
    final CountDownLatch computingLatch = new CountDownLatch(1);
    CacheLoader<String, String> computingFunction =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws InterruptedException {
            if (shouldWait.get()) {
              computingLatch.await();
            }
            return key;
          }
        };
    QueuingRemovalListener<String, String> listener = queuingRemovalListener();

    final LoadingCache<String, String> cache =
        CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .removalListener(listener)
            .build(computingFunction);

    // seed the map, so its segment's count > 0
    cache.getUnchecked("a");
    shouldWait.set(true);

    final CountDownLatch computationStarted = new CountDownLatch(1);
    final CountDownLatch computationComplete = new CountDownLatch(1);
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                computationStarted.countDown();
                cache.getUnchecked("b");
                computationComplete.countDown();
              }
            })
        .start();

    // wait for the computingEntry to be created
    computationStarted.await();
    cache.invalidateAll();
    // let the computation proceed
    computingLatch.countDown();
    // don't check cache.size() until we know the get("b") call is complete
    computationComplete.await();

    // At this point, the listener should be holding the seed value (a -> a), and the map should
    // contain the computed value (b -> b), since the clear() happened before the computation
    // completed.
    assertEquals(1, listener.size());
    RemovalNotification<String, String> notification = listener.remove();
    assertEquals("a", notification.getKey());
    assertEquals("a", notification.getValue());
    assertEquals(1, cache.size());
    assertEquals("b", cache.getUnchecked("b"));
  }

  // "Basher tests", where we throw a bunch of stuff at a LoadingCache and check basic invariants.

  /**
   * This is a less carefully-controlled version of {@link #testRemovalNotification_clear} - this is
   * a black-box test that tries to create lots of different thread-interleavings, and asserts that
   * each computation is affected by a call to {@code clear()} (and therefore gets passed to the
   * removal listener), or else is not affected by the {@code clear()} (and therefore exists in the
   * cache afterward).
   */
  @GwtIncompatible // QueuingRemovalListener

  public void testRemovalNotification_clear_basher() throws InterruptedException {
    // If a clear() happens close to the end of computation, one of two things should happen:
    // - computation ends first: the removal listener is called, and the cache does not contain the
    //   key/value pair
    // - clear() happens first: the removal listener is not called, and the cache contains the pair
    AtomicBoolean computationShouldWait = new AtomicBoolean();
    CountDownLatch computationLatch = new CountDownLatch(1);
    QueuingRemovalListener<String, String> listener = queuingRemovalListener();
    final LoadingCache<String, String> cache =
        CacheBuilder.newBuilder()
            .removalListener(listener)
            .concurrencyLevel(20)
            .build(new DelayingIdentityLoader<String>(computationShouldWait, computationLatch));

    int nThreads = 100;
    int nTasks = 1000;
    int nSeededEntries = 100;
    Set<String> expectedKeys = Sets.newHashSetWithExpectedSize(nTasks + nSeededEntries);
    // seed the map, so its segments have a count>0; otherwise, clear() won't visit the in-progress
    // entries
    for (int i = 0; i < nSeededEntries; i++) {
      String s = "b" + i;
      cache.getUnchecked(s);
      expectedKeys.add(s);
    }
    computationShouldWait.set(true);

    final AtomicInteger computedCount = new AtomicInteger();
    ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);
    final CountDownLatch tasksFinished = new CountDownLatch(nTasks);
    for (int i = 0; i < nTasks; i++) {
      final String s = "a" + i;
      @SuppressWarnings("unused") // go/futurereturn-lsc
      Future<?> possiblyIgnoredError =
          threadPool.submit(
              new Runnable() {
                @Override
                public void run() {
                  cache.getUnchecked(s);
                  computedCount.incrementAndGet();
                  tasksFinished.countDown();
                }
              });
      expectedKeys.add(s);
    }

    computationLatch.countDown();
    // let some computations complete
    while (computedCount.get() < nThreads) {
      Thread.yield();
    }
    cache.invalidateAll();
    tasksFinished.await();

    // Check all of the removal notifications we received: they should have had correctly-associated
    // keys and values. (An earlier bug saw removal notifications for in-progress computations,
    // which had real keys with null values.)
    Map<String, String> removalNotifications = Maps.newHashMap();
    for (RemovalNotification<String, String> notification : listener) {
      removalNotifications.put(notification.getKey(), notification.getValue());
      assertEquals(
          "Unexpected key/value pair passed to removalListener",
          notification.getKey(),
          notification.getValue());
    }

    // All of the seed values should have been visible, so we should have gotten removal
    // notifications for all of them.
    for (int i = 0; i < nSeededEntries; i++) {
      assertEquals("b" + i, removalNotifications.get("b" + i));
    }

    // Each of the values added to the map should either still be there, or have seen a removal
    // notification.
    assertEquals(expectedKeys, Sets.union(cache.asMap().keySet(), removalNotifications.keySet()));
    assertTrue(Sets.intersection(cache.asMap().keySet(), removalNotifications.keySet()).isEmpty());
  }

  /**
   * Calls get() repeatedly from many different threads, and tests that all of the removed entries
   * (removed because of size limits or expiration) trigger appropriate removal notifications.
   */
  @GwtIncompatible // QueuingRemovalListener

  public void testRemovalNotification_get_basher() throws InterruptedException {
    int nTasks = 1000;
    int nThreads = 100;
    final int getsPerTask = 1000;
    final int nUniqueKeys = 10000;
    final Random random = new Random(); // Randoms.insecureRandom();

    QueuingRemovalListener<String, String> removalListener = queuingRemovalListener();
    final AtomicInteger computeCount = new AtomicInteger();
    final AtomicInteger exceptionCount = new AtomicInteger();
    final AtomicInteger computeNullCount = new AtomicInteger();
    CacheLoader<String, String> countingIdentityLoader =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws InterruptedException {
            int behavior = random.nextInt(4);
            if (behavior == 0) { // throw an exception
              exceptionCount.incrementAndGet();
              throw new RuntimeException("fake exception for test");
            } else if (behavior == 1) { // return null
              computeNullCount.incrementAndGet();
              return null;
            } else if (behavior == 2) { // slight delay before returning
              Thread.sleep(5);
              computeCount.incrementAndGet();
              return key;
            } else {
              computeCount.incrementAndGet();
              return key;
            }
          }
        };
    final LoadingCache<String, String> cache =
        CacheBuilder.newBuilder()
            .recordStats()
            .concurrencyLevel(2)
            .expireAfterWrite(100, MILLISECONDS)
            .removalListener(removalListener)
            .maximumSize(5000)
            .build(countingIdentityLoader);

    ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);
    for (int i = 0; i < nTasks; i++) {
      @SuppressWarnings("unused") // go/futurereturn-lsc
      Future<?> possiblyIgnoredError =
          threadPool.submit(
              new Runnable() {
                @Override
                public void run() {
                  for (int j = 0; j < getsPerTask; j++) {
                    try {
                      cache.getUnchecked("key" + random.nextInt(nUniqueKeys));
                    } catch (RuntimeException e) {
                    }
                  }
                }
              });
    }

    threadPool.shutdown();
    threadPool.awaitTermination(300, SECONDS);

    // Since we're not doing any more cache operations, and the cache only expires/evicts when doing
    // other operations, the cache and the removal queue won't change from this point on.

    // Verify that each received removal notification was valid
    for (RemovalNotification<String, String> notification : removalListener) {
      assertEquals("Invalid removal notification", notification.getKey(), notification.getValue());
    }

    CacheStats stats = cache.stats();
    assertEquals(removalListener.size(), stats.evictionCount());
    assertEquals(computeCount.get(), stats.loadSuccessCount());
    assertEquals(exceptionCount.get() + computeNullCount.get(), stats.loadExceptionCount());
    // each computed value is still in the cache, or was passed to the removal listener
    assertEquals(computeCount.get(), cache.size() + removalListener.size());
  }

  @GwtIncompatible // NullPointerTester
  public void testNullParameters() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    tester.testAllPublicInstanceMethods(builder);
  }

  @GwtIncompatible // CacheTesting
  public void testSizingDefaults() {
    LoadingCache<?, ?> cache = CacheBuilder.newBuilder().build(identityLoader());
    LocalCache<?, ?> map = CacheTesting.toLocalCache(cache);
    assertThat(map.segments).hasLength(4); // concurrency level
    assertEquals(4, map.segments[0].table.length()); // capacity / conc level
  }

  @GwtIncompatible // CountDownLatch
  static final class DelayingIdentityLoader<T> extends CacheLoader<T, T> {
    private final AtomicBoolean shouldWait;
    private final CountDownLatch delayLatch;

    DelayingIdentityLoader(AtomicBoolean shouldWait, CountDownLatch delayLatch) {
      this.shouldWait = shouldWait;
      this.delayLatch = delayLatch;
    }

    @Override
    public T load(T key) throws InterruptedException {
      if (shouldWait.get()) {
        delayLatch.await();
      }
      return key;
    }
  }
}
