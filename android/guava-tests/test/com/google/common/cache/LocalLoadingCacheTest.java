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
import static java.util.concurrent.TimeUnit.SECONDS;

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
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * @author Charles Fry
 */
@NullUnmarked
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
    assertThat(cache.localCache.defaultLoader).isSameInstanceAs(loader);
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
    assertThat(cache.stats()).isEqualTo(EMPTY_STATS);

    Object one = new Object();
    cache.getUnchecked(one);
    CacheStats stats = cache.stats();
    assertThat(stats.requestCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);
    assertThat(stats.hitRate()).isEqualTo(0.0);
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.missRate()).isEqualTo(1.0);
    assertThat(stats.loadCount()).isEqualTo(1);
    long totalLoadTime = stats.totalLoadTime();
    assertThat(totalLoadTime).isAtLeast(0);
    assertThat(stats.averageLoadPenalty()).isAtLeast(0.0);
    assertThat(stats.evictionCount()).isEqualTo(0);

    cache.getUnchecked(one);
    stats = cache.stats();
    assertThat(stats.requestCount()).isEqualTo(2);
    assertThat(stats.hitCount()).isEqualTo(1);
    assertThat(stats.hitRate()).isEqualTo(1.0 / 2);
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.missRate()).isEqualTo(1.0 / 2);
    assertThat(stats.loadCount()).isEqualTo(1);
    assertThat(stats.evictionCount()).isEqualTo(0);

    Object two = new Object();
    cache.getUnchecked(two);
    stats = cache.stats();
    assertThat(stats.requestCount()).isEqualTo(3);
    assertThat(stats.hitCount()).isEqualTo(1);
    assertThat(stats.hitRate()).isEqualTo(1.0 / 3);
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.missRate()).isEqualTo(2.0 / 3);
    assertThat(stats.loadCount()).isEqualTo(2);
    assertThat(stats.totalLoadTime()).isAtLeast(totalLoadTime);
    totalLoadTime = stats.totalLoadTime();
    assertThat(stats.averageLoadPenalty()).isAtLeast(0.0);
    assertThat(stats.evictionCount()).isEqualTo(0);

    Object three = new Object();
    cache.getUnchecked(three);
    stats = cache.stats();
    assertThat(stats.requestCount()).isEqualTo(4);
    assertThat(stats.hitCount()).isEqualTo(1);
    assertThat(stats.hitRate()).isEqualTo(1.0 / 4);
    assertThat(stats.missCount()).isEqualTo(3);
    assertThat(stats.missRate()).isEqualTo(3.0 / 4);
    assertThat(stats.loadCount()).isEqualTo(3);
    assertThat(stats.totalLoadTime()).isAtLeast(totalLoadTime);
    assertThat(stats.averageLoadPenalty()).isAtLeast(0.0);
    assertThat(stats.evictionCount()).isEqualTo(1);
  }

  public void testStatsNoops() {
    CacheBuilder<Object, Object> builder = createCacheBuilder().concurrencyLevel(1);
    LocalLoadingCache<Object, Object> cache = makeCache(builder, identityLoader());
    ConcurrentMap<Object, Object> map = cache.localCache; // modifiable map view
    assertThat(cache.stats()).isEqualTo(EMPTY_STATS);

    Object one = new Object();
    assertThat(map.put(one, one)).isNull();
    assertThat(map.get(one)).isSameInstanceAs(one);
    assertThat(map.containsKey(one)).isTrue();
    assertThat(map.containsValue(one)).isTrue();
    Object two = new Object();
    assertThat(map.replace(one, two)).isSameInstanceAs(one);
    assertThat(map.containsKey(one)).isTrue();
    assertThat(map.containsValue(one)).isFalse();
    Object three = new Object();
    assertThat(map.replace(one, two, three)).isTrue();
    assertThat(map.remove(one, three)).isTrue();
    assertThat(map.containsKey(one)).isFalse();
    assertThat(map.containsValue(one)).isFalse();
    assertThat(map.putIfAbsent(two, three)).isNull();
    assertThat(map.remove(two)).isSameInstanceAs(three);
    assertThat(map.put(three, one)).isNull();
    assertThat(map.put(one, two)).isNull();

    assertThat(map).containsEntry(three, one);
    assertThat(map).containsEntry(one, two);

    // TODO(cgruber): Confirm with fry@ that this is a reasonable substitute.
    // Set<Entry<Object, Object>> entries = map.entrySet();
    // assertThat(entries).containsExactly(
    //    Maps.immutableEntry(three, one), Maps.immutableEntry(one, two));
    // Set<Object> keys = map.keySet();
    // assertThat(keys).containsExactly(one, three);
    // Collection<Object> values = map.values();
    // assertThat(values).containsExactly(one, two);

    map.clear();

    assertThat(cache.stats()).isEqualTo(EMPTY_STATS);
  }

  public void testNoStats() {
    CacheBuilder<Object, Object> builder =
        CacheBuilder.newBuilder().concurrencyLevel(1).maximumSize(2);
    LocalLoadingCache<Object, Object> cache = makeCache(builder, identityLoader());
    assertThat(cache.stats()).isEqualTo(EMPTY_STATS);

    Object one = new Object();
    cache.getUnchecked(one);
    assertThat(cache.stats()).isEqualTo(EMPTY_STATS);

    cache.getUnchecked(one);
    assertThat(cache.stats()).isEqualTo(EMPTY_STATS);

    Object two = new Object();
    cache.getUnchecked(two);
    assertThat(cache.stats()).isEqualTo(EMPTY_STATS);

    Object three = new Object();
    cache.getUnchecked(three);
    assertThat(cache.stats()).isEqualTo(EMPTY_STATS);
  }

  public void testRecordStats() {
    CacheBuilder<Object, Object> builder =
        createCacheBuilder().recordStats().concurrencyLevel(1).maximumSize(2);
    LocalLoadingCache<Object, Object> cache = makeCache(builder, identityLoader());
    assertThat(cache.stats().hitCount()).isEqualTo(0);
    assertThat(cache.stats().missCount()).isEqualTo(0);

    Object one = new Object();
    cache.getUnchecked(one);
    assertThat(cache.stats().hitCount()).isEqualTo(0);
    assertThat(cache.stats().missCount()).isEqualTo(1);

    cache.getUnchecked(one);
    assertThat(cache.stats().hitCount()).isEqualTo(1);
    assertThat(cache.stats().missCount()).isEqualTo(1);

    Object two = new Object();
    cache.getUnchecked(two);
    assertThat(cache.stats().hitCount()).isEqualTo(1);
    assertThat(cache.stats().missCount()).isEqualTo(2);

    Object three = new Object();
    cache.getUnchecked(three);
    assertThat(cache.stats().hitCount()).isEqualTo(1);
    assertThat(cache.stats().missCount()).isEqualTo(3);
  }

  // asMap tests

  public void testAsMap() {
    CacheBuilder<Object, Object> builder = createCacheBuilder();
    LocalLoadingCache<Object, Object> cache = makeCache(builder, identityLoader());
    assertThat(cache.stats()).isEqualTo(EMPTY_STATS);

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();

    ConcurrentMap<Object, Object> map = cache.asMap();
    assertThat(map.put(one, two)).isNull();
    assertThat(map.get(one)).isSameInstanceAs(two);
    map.putAll(ImmutableMap.of(two, three));
    assertThat(map.get(two)).isSameInstanceAs(three);
    assertThat(map.putIfAbsent(one, three)).isSameInstanceAs(two);
    assertThat(map.get(one)).isSameInstanceAs(two);
    assertThat(map.putIfAbsent(three, one)).isNull();
    assertThat(map.get(three)).isSameInstanceAs(one);
    assertThat(map.replace(one, three)).isSameInstanceAs(two);
    assertThat(map.get(one)).isSameInstanceAs(three);
    assertThat(map.replace(one, two, three)).isFalse();
    assertThat(map.get(one)).isSameInstanceAs(three);
    assertThat(map.replace(one, three, two)).isTrue();
    assertThat(map.get(one)).isSameInstanceAs(two);
    assertThat(map).hasSize(3);

    map.clear();
    assertThat(map.isEmpty()).isTrue();
    assertThat(map).isEmpty();

    cache.getUnchecked(one);
    assertThat(map).hasSize(1);
    assertThat(map.get(one)).isSameInstanceAs(one);
    assertThat(map.containsKey(one)).isTrue();
    assertThat(map.containsValue(one)).isTrue();
    assertThat(map.remove(one)).isSameInstanceAs(one);
    assertThat(map).isEmpty();

    cache.getUnchecked(one);
    assertThat(map).hasSize(1);
    assertThat(map.remove(one, two)).isFalse();
    assertThat(map.remove(one, one)).isTrue();
    assertThat(map).isEmpty();

    cache.getUnchecked(one);
    Map<Object, Object> newMap = ImmutableMap.of(one, one);
    assertThat(map).isEqualTo(newMap);
    assertThat(map.entrySet()).isEqualTo(newMap.entrySet());
    assertThat(map.keySet()).isEqualTo(newMap.keySet());
    Set<Object> expectedValues = ImmutableSet.of(one);
    Set<Object> actualValues = ImmutableSet.copyOf(map.values());
    assertThat(actualValues).isEqualTo(expectedValues);
  }

  /** Lookups on the map view shouldn't impact the recency queue. */
  public void testAsMapRecency() {
    CacheBuilder<Object, Object> builder =
        createCacheBuilder().concurrencyLevel(1).maximumSize(SMALL_MAX_SIZE);
    LocalLoadingCache<Object, Object> cache = makeCache(builder, identityLoader());
    Segment<Object, Object> segment = cache.localCache.segments[0];
    ConcurrentMap<Object, Object> map = cache.asMap();

    Object one = new Object();
    assertThat(cache.getUnchecked(one)).isSameInstanceAs(one);
    assertThat(segment.recencyQueue.isEmpty()).isTrue();
    assertThat(map.get(one)).isSameInstanceAs(one);
    assertThat(segment.recencyQueue.peek().getKey()).isSameInstanceAs(one);
    assertThat(cache.getUnchecked(one)).isSameInstanceAs(one);
    assertThat(segment.recencyQueue.isEmpty()).isFalse();
  }

  public void testRecursiveComputation() throws InterruptedException {
    AtomicReference<LoadingCache<Integer, String>> cacheRef = new AtomicReference<>();
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
    assertThat(recursiveCache.getUnchecked(3)).isEqualTo("3, 2, 1, 0");

    recursiveLoader =
        new CacheLoader<Integer, String>() {
          @Override
          public String load(Integer key) {
            return cacheRef.get().getUnchecked(key);
          }
        };

    recursiveCache = CacheBuilder.newBuilder().weakKeys().weakValues().build(recursiveLoader);
    cacheRef.set(recursiveCache);

    // tells the test when the computation has completed
    CountDownLatch doneSignal = new CountDownLatch(1);

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

    boolean done = doneSignal.await(1, SECONDS);
    if (!done) {
      StringBuilder builder = new StringBuilder();
      for (StackTraceElement trace : thread.getStackTrace()) {
        builder.append("\tat ").append(trace).append('\n');
      }
      fail(builder.toString());
    }
  }
}
