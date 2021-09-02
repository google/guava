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

import static com.google.common.cache.CacheBuilder.EMPTY_STATS;
import static com.google.common.cache.LocalCacheTest.SMALL_MAX_SIZE;
import static com.google.common.cache.TestingCacheLoaders.identityLoader;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.cache.LocalCache.LocalLoadingCache;
import com.google.common.cache.LocalCache.Segment;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;

/** @author Charles Fry */
public class LocalLoadingCacheTest extends TestCase {

  private static <K, V> LocalLoadingCache<K, V> makeCache(
      CacheBuilder<K, V> builder, CacheLoader<? super K, V> loader) {
    return new LocalLoadingCache<>(builder, loader);
  }

  private CacheBuilder<Object, Object> createCacheBuilder() {
    return CacheBuilder.newBuilder().recordStats();
  }

  // constructor tests

  public void testComputingFunction() {
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object from) {
            return new Object();
          }
        };
    LocalLoadingCache<Object, Object> cache = makeCache(createCacheBuilder(), loader);
    assertSame(loader, cache.localCache.defaultLoader);
  }

  // null parameters test

  public void testNullParameters() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    CacheLoader<Object, Object> loader = identityLoader();
    tester.testAllPublicInstanceMethods(makeCache(createCacheBuilder(), loader));
  }

  // stats tests

  public void testStats() {
    CacheBuilder<Object, Object> builder = createCacheBuilder().concurrencyLevel(1).maximumSize(2);
    LocalLoadingCache<Object, Object> cache = makeCache(builder, identityLoader());
    assertEquals(EMPTY_STATS, cache.stats());

    Object one = new Object();
    cache.getUnchecked(one);
    CacheStats stats = cache.stats();
    assertEquals(1, stats.requestCount());
    assertEquals(0, stats.hitCount());
    assertEquals(0.0, stats.hitRate());
    assertEquals(1, stats.missCount());
    assertEquals(1.0, stats.missRate());
    assertEquals(1, stats.loadCount());
    long totalLoadTime = stats.totalLoadTime();
    assertTrue(totalLoadTime >= 0);
    assertTrue(stats.averageLoadPenalty() >= 0.0);
    assertEquals(0, stats.evictionCount());

    cache.getUnchecked(one);
    stats = cache.stats();
    assertEquals(2, stats.requestCount());
    assertEquals(1, stats.hitCount());
    assertEquals(1.0 / 2, stats.hitRate());
    assertEquals(1, stats.missCount());
    assertEquals(1.0 / 2, stats.missRate());
    assertEquals(1, stats.loadCount());
    assertEquals(0, stats.evictionCount());

    Object two = new Object();
    cache.getUnchecked(two);
    stats = cache.stats();
    assertEquals(3, stats.requestCount());
    assertEquals(1, stats.hitCount());
    assertEquals(1.0 / 3, stats.hitRate());
    assertEquals(2, stats.missCount());
    assertEquals(2.0 / 3, stats.missRate());
    assertEquals(2, stats.loadCount());
    assertTrue(stats.totalLoadTime() >= totalLoadTime);
    totalLoadTime = stats.totalLoadTime();
    assertTrue(stats.averageLoadPenalty() >= 0.0);
    assertEquals(0, stats.evictionCount());

    Object three = new Object();
    cache.getUnchecked(three);
    stats = cache.stats();
    assertEquals(4, stats.requestCount());
    assertEquals(1, stats.hitCount());
    assertEquals(1.0 / 4, stats.hitRate());
    assertEquals(3, stats.missCount());
    assertEquals(3.0 / 4, stats.missRate());
    assertEquals(3, stats.loadCount());
    assertTrue(stats.totalLoadTime() >= totalLoadTime);
    totalLoadTime = stats.totalLoadTime();
    assertTrue(stats.averageLoadPenalty() >= 0.0);
    assertEquals(1, stats.evictionCount());
  }

  public void testStatsNoops() {
    CacheBuilder<Object, Object> builder = createCacheBuilder().concurrencyLevel(1);
    LocalLoadingCache<Object, Object> cache = makeCache(builder, identityLoader());
    ConcurrentMap<Object, Object> map = cache.localCache; // modifiable map view
    assertEquals(EMPTY_STATS, cache.stats());

    Object one = new Object();
    assertNull(map.put(one, one));
    assertSame(one, map.get(one));
    assertTrue(map.containsKey(one));
    assertTrue(map.containsValue(one));
    Object two = new Object();
    assertSame(one, map.replace(one, two));
    assertTrue(map.containsKey(one));
    assertFalse(map.containsValue(one));
    Object three = new Object();
    assertTrue(map.replace(one, two, three));
    assertTrue(map.remove(one, three));
    assertFalse(map.containsKey(one));
    assertFalse(map.containsValue(one));
    assertNull(map.putIfAbsent(two, three));
    assertSame(three, map.remove(two));
    assertNull(map.put(three, one));
    assertNull(map.put(one, two));

    assertThat(map).containsEntry(three, one);
    assertThat(map).containsEntry(one, two);

    // TODO(user): Confirm with fry@ that this is a reasonable substitute.
    // Set<Entry<Object, Object>> entries = map.entrySet();
    // assertThat(entries).containsExactly(
    //    Maps.immutableEntry(three, one), Maps.immutableEntry(one, two));
    // Set<Object> keys = map.keySet();
    // assertThat(keys).containsExactly(one, three);
    // Collection<Object> values = map.values();
    // assertThat(values).containsExactly(one, two);

    map.clear();

    assertEquals(EMPTY_STATS, cache.stats());
  }

  public void testNoStats() {
    CacheBuilder<Object, Object> builder =
        CacheBuilder.newBuilder().concurrencyLevel(1).maximumSize(2);
    LocalLoadingCache<Object, Object> cache = makeCache(builder, identityLoader());
    assertEquals(EMPTY_STATS, cache.stats());

    Object one = new Object();
    cache.getUnchecked(one);
    assertEquals(EMPTY_STATS, cache.stats());

    cache.getUnchecked(one);
    assertEquals(EMPTY_STATS, cache.stats());

    Object two = new Object();
    cache.getUnchecked(two);
    assertEquals(EMPTY_STATS, cache.stats());

    Object three = new Object();
    cache.getUnchecked(three);
    assertEquals(EMPTY_STATS, cache.stats());
  }

  public void testRecordStats() {
    CacheBuilder<Object, Object> builder =
        createCacheBuilder().recordStats().concurrencyLevel(1).maximumSize(2);
    LocalLoadingCache<Object, Object> cache = makeCache(builder, identityLoader());
    assertEquals(0, cache.stats().hitCount());
    assertEquals(0, cache.stats().missCount());

    Object one = new Object();
    cache.getUnchecked(one);
    assertEquals(0, cache.stats().hitCount());
    assertEquals(1, cache.stats().missCount());

    cache.getUnchecked(one);
    assertEquals(1, cache.stats().hitCount());
    assertEquals(1, cache.stats().missCount());

    Object two = new Object();
    cache.getUnchecked(two);
    assertEquals(1, cache.stats().hitCount());
    assertEquals(2, cache.stats().missCount());

    Object three = new Object();
    cache.getUnchecked(three);
    assertEquals(1, cache.stats().hitCount());
    assertEquals(3, cache.stats().missCount());
  }

  // asMap tests

  public void testAsMap() {
    CacheBuilder<Object, Object> builder = createCacheBuilder();
    LocalLoadingCache<Object, Object> cache = makeCache(builder, identityLoader());
    assertEquals(EMPTY_STATS, cache.stats());

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();

    ConcurrentMap<Object, Object> map = cache.asMap();
    assertNull(map.put(one, two));
    assertSame(two, map.get(one));
    map.putAll(ImmutableMap.of(two, three));
    assertSame(three, map.get(two));
    assertSame(two, map.putIfAbsent(one, three));
    assertSame(two, map.get(one));
    assertNull(map.putIfAbsent(three, one));
    assertSame(one, map.get(three));
    assertSame(two, map.replace(one, three));
    assertSame(three, map.get(one));
    assertFalse(map.replace(one, two, three));
    assertSame(three, map.get(one));
    assertTrue(map.replace(one, three, two));
    assertSame(two, map.get(one));
    assertEquals(3, map.size());

    map.clear();
    assertTrue(map.isEmpty());
    assertEquals(0, map.size());

    cache.getUnchecked(one);
    assertEquals(1, map.size());
    assertSame(one, map.get(one));
    assertTrue(map.containsKey(one));
    assertTrue(map.containsValue(one));
    assertSame(one, map.remove(one));
    assertEquals(0, map.size());

    cache.getUnchecked(one);
    assertEquals(1, map.size());
    assertFalse(map.remove(one, two));
    assertTrue(map.remove(one, one));
    assertEquals(0, map.size());

    cache.getUnchecked(one);
    Map<Object, Object> newMap = ImmutableMap.of(one, one);
    assertEquals(newMap, map);
    assertEquals(newMap.entrySet(), map.entrySet());
    assertEquals(newMap.keySet(), map.keySet());
    Set<Object> expectedValues = ImmutableSet.of(one);
    Set<Object> actualValues = ImmutableSet.copyOf(map.values());
    assertEquals(expectedValues, actualValues);
  }

  /** Lookups on the map view shouldn't impact the recency queue. */
  public void testAsMapRecency() {
    CacheBuilder<Object, Object> builder =
        createCacheBuilder().concurrencyLevel(1).maximumSize(SMALL_MAX_SIZE);
    LocalLoadingCache<Object, Object> cache = makeCache(builder, identityLoader());
    Segment<Object, Object> segment = cache.localCache.segments[0];
    ConcurrentMap<Object, Object> map = cache.asMap();

    Object one = new Object();
    assertSame(one, cache.getUnchecked(one));
    assertTrue(segment.recencyQueue.isEmpty());
    assertSame(one, map.get(one));
    assertSame(one, segment.recencyQueue.peek().getKey());
    assertSame(one, cache.getUnchecked(one));
    assertFalse(segment.recencyQueue.isEmpty());
  }


  public void testRecursiveComputation() throws InterruptedException {
    final AtomicReference<LoadingCache<Integer, String>> cacheRef = new AtomicReference<>();
    CacheLoader<Integer, String> recursiveLoader =
        new CacheLoader<Integer, String>() {
          @Override
          public String load(Integer key) {
            if (key > 0) {
              return key + ", " + cacheRef.get().getUnchecked(key - 1);
            } else {
              return "0";
            }
          }
        };

    LoadingCache<Integer, String> recursiveCache =
        CacheBuilder.newBuilder().weakKeys().weakValues().build(recursiveLoader);
    cacheRef.set(recursiveCache);
    assertEquals("3, 2, 1, 0", recursiveCache.getUnchecked(3));

    recursiveLoader =
        new CacheLoader<Integer, String>() {
          @Override
          public String load(Integer key) {
            return cacheRef.get().getUnchecked(key);
          }
        };

    recursiveCache = CacheBuilder.newBuilder().weakKeys().weakValues().build(recursiveLoader);
    cacheRef.set(recursiveCache);

    // tells the test when the compution has completed
    final CountDownLatch doneSignal = new CountDownLatch(1);

    Thread thread =
        new Thread() {
          @Override
          public void run() {
            try {
              cacheRef.get().getUnchecked(3);
            } finally {
              doneSignal.countDown();
            }
          }
        };
    thread.setUncaughtExceptionHandler(
        new UncaughtExceptionHandler() {
          @Override
          public void uncaughtException(Thread t, Throwable e) {}
        });
    thread.start();

    boolean done = doneSignal.await(1, TimeUnit.SECONDS);
    if (!done) {
      StringBuilder builder = new StringBuilder();
      for (StackTraceElement trace : thread.getStackTrace()) {
        builder.append("\tat ").append(trace).append('\n');
      }
      fail(builder.toString());
    }
  }
}
