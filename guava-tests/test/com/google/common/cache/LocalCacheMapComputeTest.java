/*
 * Copyright (C) 2017 The Guava Authors
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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import junit.framework.TestCase;

/** Test Java8 map.compute in concurrent cache context. */
public class LocalCacheMapComputeTest extends TestCase {
  final int count = 10000;
  final String delimiter = "-";
  final String key = "key";
  Cache<String, String> cache;

  // helper
  private static void doParallelCacheOp(int count, IntConsumer consumer) {
    IntStream.range(0, count).parallel().forEach(consumer);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.cache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(500000, MILLISECONDS)
            .maximumSize(count)
            .build();
  }

  public void testComputeIfAbsent() {
    // simultaneous insertion for same key, expect 1 winner
    doParallelCacheOp(
        count,
        n -> {
          cache.asMap().computeIfAbsent(key, k -> "value" + n);
        });
    assertEquals(1, cache.size());
  }

  public void testComputeIfAbsentEviction() {
    // b/80241237

    Cache<String, String> c = CacheBuilder.newBuilder().maximumSize(1).build();

    assertThat(c.asMap().computeIfAbsent("hash-1", k -> "")).isEqualTo("");
    assertThat(c.asMap().computeIfAbsent("hash-1", k -> "")).isEqualTo("");
    assertThat(c.asMap().computeIfAbsent("hash-1", k -> "")).isEqualTo("");
    assertThat(c.size()).isEqualTo(1);
    assertThat(c.asMap().computeIfAbsent("hash-2", k -> "")).isEqualTo("");
  }

  public void testComputeEviction() {
    // b/80241237

    Cache<String, String> c = CacheBuilder.newBuilder().maximumSize(1).build();

    assertThat(c.asMap().compute("hash-1", (k, v) -> "a")).isEqualTo("a");
    assertThat(c.asMap().compute("hash-1", (k, v) -> "b")).isEqualTo("b");
    assertThat(c.asMap().compute("hash-1", (k, v) -> "c")).isEqualTo("c");
    assertThat(c.size()).isEqualTo(1);
    assertThat(c.asMap().computeIfAbsent("hash-2", k -> "")).isEqualTo("");
  }

  public void testComputeIfPresent() {
    cache.put(key, "1");
    // simultaneous update for same key, expect count successful updates
    doParallelCacheOp(
        count,
        n -> {
          cache.asMap().computeIfPresent(key, (k, v) -> v + delimiter + n);
        });
    assertEquals(1, cache.size());
    assertThat(cache.getIfPresent(key).split(delimiter)).hasLength(count + 1);
  }

  public void testComputeIfPresentRemove() {
    List<RemovalNotification<Integer, Integer>> notifications = new ArrayList<>();
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .removalListener(
                new RemovalListener<Integer, Integer>() {
                  @Override
                  public void onRemoval(RemovalNotification<Integer, Integer> notification) {
                    notifications.add(notification);
                  }
                })
            .build();
    cache.put(1, 2);

    // explicitly remove the existing value
    cache.asMap().computeIfPresent(1, (key, value) -> null);
    assertThat(notifications).hasSize(1);
    CacheTesting.checkEmpty(cache);

    // ensure no zombie entry remains
    cache.asMap().computeIfPresent(1, (key, value) -> null);
    assertThat(notifications).hasSize(1);
    CacheTesting.checkEmpty(cache);
  }

  public void testUpdates() {
    cache.put(key, "1");
    // simultaneous update for same key, some null, some non-null
    doParallelCacheOp(
        count,
        n -> {
          cache.asMap().compute(key, (k, v) -> n % 2 == 0 ? v + delimiter + n : null);
        });
    assertTrue(1 >= cache.size());
  }

  public void testCompute() {
    cache.put(key, "1");
    // simultaneous deletion
    doParallelCacheOp(
        count,
        n -> {
          cache.asMap().compute(key, (k, v) -> null);
        });
    assertEquals(0, cache.size());
  }

  public void testComputeWithLoad() {
    Queue<RemovalNotification<String, String>> notifications = new ConcurrentLinkedQueue<>();
    cache =
        CacheBuilder.newBuilder()
            .removalListener(
                new RemovalListener<String, String>() {
                  @Override
                  public void onRemoval(RemovalNotification<String, String> notification) {
                    notifications.add(notification);
                  }
                })
            .expireAfterAccess(500000, MILLISECONDS)
            .maximumSize(count)
            .build();

    cache.put(key, "1");
    // simultaneous load and deletion
    doParallelCacheOp(
        count,
        n -> {
          try {
            String unused = cache.get(key, () -> key);
            cache.asMap().compute(key, (k, v) -> null);
          } catch (ExecutionException e) {
            throw new UncheckedExecutionException(e);
          }
        });

    CacheTesting.checkEmpty(cache);
    for (RemovalNotification<String, String> entry : notifications) {
      assertThat(entry.getKey()).isNotNull();
      assertThat(entry.getValue()).isNotNull();
    }
  }

  public void testComputeExceptionally() {
    assertThrows(
        RuntimeException.class,
        () ->
            doParallelCacheOp(
                count,
                n -> {
                  cache
                      .asMap()
                      .compute(
                          key,
                          (k, v) -> {
                            throw new RuntimeException();
                          });
                }));
  }
}
