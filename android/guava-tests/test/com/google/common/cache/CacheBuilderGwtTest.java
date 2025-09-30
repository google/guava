/*
 * Copyright (C) 2012 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.FakeTicker;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.NullUnmarked;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test suite for {@link CacheBuilder}. TODO(cpovirk): merge into CacheBuilderTest?
 *
 * @author Jon Donovan
 */
@GwtCompatible
@NullUnmarked
@RunWith(JUnit4.class)
public class CacheBuilderGwtTest {

  private FakeTicker fakeTicker;

  @Before
  public void setUp() {
    fakeTicker = new FakeTicker();
  }

  @Test
  public void loader() throws ExecutionException {
    Cache<Integer, Integer> cache = CacheBuilder.newBuilder().build();

    Callable<Integer> loader =
        new Callable<Integer>() {
          private int i = 0;

          @Override
          public Integer call() throws Exception {
            return ++i;
          }
        };

    cache.put(0, 10);

    assertThat(cache.get(0, loader)).isEqualTo(10);
    assertThat(cache.get(20, loader)).isEqualTo(1);
    assertThat(cache.get(34, loader)).isEqualTo(2);

    cache.invalidate(0);
    assertThat(cache.get(0, loader)).isEqualTo(3);

    cache.put(0, 10);
    cache.invalidateAll();
    assertThat(cache.get(0, loader)).isEqualTo(4);
  }

  @Test
  public void sizeConstraint() {
    Cache<Integer, Integer> cache = CacheBuilder.newBuilder().maximumSize(4).build();

    cache.put(1, 10);
    cache.put(2, 20);
    cache.put(3, 30);
    cache.put(4, 40);
    cache.put(5, 50);

    assertThat(cache.getIfPresent(10)).isNull();
    // Order required to remove dependence on access order / write order constraint.
    assertThat(cache.getIfPresent(2)).isEqualTo(20);
    assertThat(cache.getIfPresent(3)).isEqualTo(30);
    assertThat(cache.getIfPresent(4)).isEqualTo(40);
    assertThat(cache.getIfPresent(5)).isEqualTo(50);

    cache.put(1, 10);
    assertThat(cache.getIfPresent(1)).isEqualTo(10);
    assertThat(cache.getIfPresent(3)).isEqualTo(30);
    assertThat(cache.getIfPresent(4)).isEqualTo(40);
    assertThat(cache.getIfPresent(5)).isEqualTo(50);
    assertThat(cache.getIfPresent(2)).isNull();
  }

  @SuppressWarnings({"deprecation", "LoadingCacheApply"})
  @Test
  public void loadingCache() throws ExecutionException {
    CacheLoader<Integer, Integer> loader =
        new CacheLoader<Integer, Integer>() {
          int i = 0;

          @Override
          public Integer load(Integer key) throws Exception {
            return i++;
          }
        };

    LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder().build(loader);

    cache.put(10, 20);

    Map<Integer, Integer> map = cache.getAll(ImmutableList.of(10, 20, 30, 54, 443, 1));

    assertThat(map).containsEntry(10, 20);
    assertThat(map).containsEntry(20, 0);
    assertThat(map).containsEntry(30, 1);
    assertThat(map).containsEntry(54, 2);
    assertThat(map).containsEntry(443, 3);
    assertThat(map).containsEntry(1, 4);
    assertThat(cache.get(6)).isEqualTo(5);
    assertThat(cache.apply(7)).isEqualTo(6);
  }

  @Test
  public void expireAfterAccess() {
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder().expireAfterAccess(1000, MILLISECONDS).ticker(fakeTicker).build();

    cache.put(0, 10);
    cache.put(2, 30);

    fakeTicker.advance(999, MILLISECONDS);
    assertThat(cache.getIfPresent(2)).isEqualTo(30);
    fakeTicker.advance(1, MILLISECONDS);
    assertThat(cache.getIfPresent(2)).isEqualTo(30);
    fakeTicker.advance(1000, MILLISECONDS);
    assertThat(cache.getIfPresent(0)).isNull();
  }

  @Test
  public void expireAfterWrite() {
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder().expireAfterWrite(1000, MILLISECONDS).ticker(fakeTicker).build();

    cache.put(10, 100);
    cache.put(20, 200);
    cache.put(4, 2);

    fakeTicker.advance(999, MILLISECONDS);
    assertThat(cache.getIfPresent(10)).isEqualTo(100);
    assertThat(cache.getIfPresent(20)).isEqualTo(200);
    assertThat(cache.getIfPresent(4)).isEqualTo(2);

    fakeTicker.advance(2, MILLISECONDS);
    assertThat(cache.getIfPresent(10)).isNull();
    assertThat(cache.getIfPresent(20)).isNull();
    assertThat(cache.getIfPresent(4)).isNull();

    cache.put(10, 20);
    assertThat(cache.getIfPresent(10)).isEqualTo(20);

    fakeTicker.advance(1000, MILLISECONDS);
    assertThat(cache.getIfPresent(10)).isNull();
  }

  @Test
  public void expireAfterWriteAndAccess() {
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(1000, MILLISECONDS)
            .expireAfterAccess(500, MILLISECONDS)
            .ticker(fakeTicker)
            .build();

    cache.put(10, 100);
    cache.put(20, 200);
    cache.put(4, 2);

    fakeTicker.advance(499, MILLISECONDS);
    assertThat(cache.getIfPresent(10)).isEqualTo(100);
    assertThat(cache.getIfPresent(20)).isEqualTo(200);

    fakeTicker.advance(2, MILLISECONDS);
    assertThat(cache.getIfPresent(10)).isEqualTo(100);
    assertThat(cache.getIfPresent(20)).isEqualTo(200);
    assertThat(cache.getIfPresent(4)).isNull();

    fakeTicker.advance(499, MILLISECONDS);
    assertThat(cache.getIfPresent(10)).isNull();
    assertThat(cache.getIfPresent(20)).isNull();

    cache.put(10, 20);
    assertThat(cache.getIfPresent(10)).isEqualTo(20);

    fakeTicker.advance(500, MILLISECONDS);
    assertThat(cache.getIfPresent(10)).isNull();
  }

  @SuppressWarnings("ContainsEntryAfterGetInteger") // we are testing our implementation of Map.get
  @Test
  public void mapMethods() {
    Cache<Integer, Integer> cache = CacheBuilder.newBuilder().build();

    ConcurrentMap<Integer, Integer> asMap = cache.asMap();

    cache.put(10, 100);
    cache.put(2, 52);

    asMap.replace(2, 79);
    asMap.replace(3, 60);

    assertThat(cache.getIfPresent(3)).isNull();
    assertThat(asMap.get(3)).isNull();

    assertThat(cache.getIfPresent(2)).isEqualTo(79);
    assertThat(asMap.get(2)).isEqualTo(79);

    asMap.replace(10, 100, 50);
    asMap.replace(2, 52, 99);

    assertThat(cache.getIfPresent(10)).isEqualTo(50);
    assertThat(asMap.get(10)).isEqualTo(50);
    assertThat(cache.getIfPresent(2)).isEqualTo(79);
    assertThat(asMap.get(2)).isEqualTo(79);

    asMap.remove(10, 100);
    asMap.remove(2, 79);

    assertThat(cache.getIfPresent(10)).isEqualTo(50);
    assertThat(asMap.get(10)).isEqualTo(50);
    assertThat(cache.getIfPresent(2)).isNull();
    assertThat(asMap.get(2)).isNull();

    asMap.putIfAbsent(2, 20);
    asMap.putIfAbsent(10, 20);

    assertThat(cache.getIfPresent(2)).isEqualTo(20);
    assertThat(asMap.get(2)).isEqualTo(20);
    assertThat(cache.getIfPresent(10)).isEqualTo(50);
    assertThat(asMap.get(10)).isEqualTo(50);
  }

  @Test
  public void removalListener() {
    int[] stats = new int[4];

    RemovalListener<Integer, Integer> countingListener =
        new RemovalListener<Integer, Integer>() {
          @Override
          public void onRemoval(RemovalNotification<Integer, Integer> notification) {
            switch (notification.getCause()) {
              case EXPIRED:
                stats[0]++;
                break;
              case EXPLICIT:
                stats[1]++;
                break;
              case REPLACED:
                stats[2]++;
                break;
              case SIZE:
                stats[3]++;
                break;
              default:
                throw new IllegalStateException("No collected exceptions in GWT CacheBuilder.");
            }
          }
        };

    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(1000, MILLISECONDS)
            .removalListener(countingListener)
            .ticker(fakeTicker)
            .maximumSize(2)
            .build();

    // Add more than two elements to increment size removals.
    cache.put(3, 20);
    cache.put(6, 2);
    cache.put(98, 45);
    cache.put(56, 76);
    cache.put(23, 84);

    // Replace the two present elements.
    cache.put(23, 20);
    cache.put(56, 49);
    cache.put(23, 2);
    cache.put(56, 4);

    // Expire the two present elements.
    fakeTicker.advance(1001, MILLISECONDS);

    cache.getIfPresent(23);
    cache.getIfPresent(56);

    // Add two elements and invalidate them.
    cache.put(1, 4);
    cache.put(2, 8);

    cache.invalidateAll();

    assertThat(stats[0]).isEqualTo(2);
    assertThat(stats[1]).isEqualTo(2);
    assertThat(stats[2]).isEqualTo(4);
    assertThat(stats[3]).isEqualTo(3);
  }

  @Test
  public void putAll() {
    Cache<Integer, Integer> cache = CacheBuilder.newBuilder().build();

    cache.putAll(ImmutableMap.of(10, 20, 30, 50, 60, 90));

    assertThat(cache.getIfPresent(10)).isEqualTo(20);
    assertThat(cache.getIfPresent(30)).isEqualTo(50);
    assertThat(cache.getIfPresent(60)).isEqualTo(90);

    cache.asMap().putAll(ImmutableMap.of(10, 50, 30, 20, 60, 70, 5, 5));

    assertThat(cache.getIfPresent(10)).isEqualTo(50);
    assertThat(cache.getIfPresent(30)).isEqualTo(20);
    assertThat(cache.getIfPresent(60)).isEqualTo(70);
    assertThat(cache.getIfPresent(5)).isEqualTo(5);
  }

  @Test
  public void invalidate() {
    Cache<Integer, Integer> cache = CacheBuilder.newBuilder().build();

    cache.put(654, 2675);
    cache.put(2456, 56);
    cache.put(2, 15);

    cache.invalidate(654);

    assertThat(cache.asMap().containsKey(654)).isFalse();
    assertThat(cache.asMap().containsKey(2456)).isTrue();
    assertThat(cache.asMap().containsKey(2)).isTrue();
  }

  @Test
  public void invalidateAll() {
    Cache<Integer, Integer> cache = CacheBuilder.newBuilder().build();

    cache.put(654, 2675);
    cache.put(2456, 56);
    cache.put(2, 15);

    cache.invalidateAll();
    assertThat(cache.asMap().containsKey(654)).isFalse();
    assertThat(cache.asMap().containsKey(2456)).isFalse();
    assertThat(cache.asMap().containsKey(2)).isFalse();

    cache.put(654, 2675);
    cache.put(2456, 56);
    cache.put(2, 15);
    cache.put(1, 3);

    cache.invalidateAll(ImmutableSet.of(1, 2));

    assertThat(cache.asMap().containsKey(1)).isFalse();
    assertThat(cache.asMap().containsKey(2)).isFalse();
    assertThat(cache.asMap().containsKey(654)).isTrue();
    assertThat(cache.asMap().containsKey(2456)).isTrue();
  }

  @Test
  public void asMap_containsValue() {
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder().expireAfterWrite(20000, MILLISECONDS).ticker(fakeTicker).build();

    cache.put(654, 2675);
    fakeTicker.advance(10000, MILLISECONDS);
    cache.put(2456, 56);
    cache.put(2, 15);

    fakeTicker.advance(10001, MILLISECONDS);

    assertThat(cache.asMap().containsValue(15)).isTrue();
    assertThat(cache.asMap().containsValue(56)).isTrue();
    assertThat(cache.asMap().containsValue(2675)).isFalse();
  }

  // we are testing our implementation of Map.containsKey
  @SuppressWarnings("ContainsEntryAfterGetInteger")
  @Test
  public void asMap_containsKey() {
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder().expireAfterWrite(20000, MILLISECONDS).ticker(fakeTicker).build();

    cache.put(654, 2675);
    fakeTicker.advance(10000, MILLISECONDS);
    cache.put(2456, 56);
    cache.put(2, 15);

    fakeTicker.advance(10001, MILLISECONDS);

    assertThat(cache.asMap().containsKey(2)).isTrue();
    assertThat(cache.asMap().containsKey(2456)).isTrue();
    assertThat(cache.asMap().containsKey(654)).isFalse();
  }

  // we are testing our implementation of Map.values().contains
  @SuppressWarnings("ValuesContainsValue")
  @Test
  public void asMapValues_contains() {
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder().expireAfterWrite(1000, MILLISECONDS).ticker(fakeTicker).build();

    cache.put(10, 20);
    fakeTicker.advance(500, MILLISECONDS);
    cache.put(20, 22);
    cache.put(5, 10);

    fakeTicker.advance(501, MILLISECONDS);

    assertThat(cache.asMap().values().contains(22)).isTrue();
    assertThat(cache.asMap().values().contains(10)).isTrue();
    assertThat(cache.asMap().values().contains(20)).isFalse();
  }

  @Test
  public void asMapKeySet() {
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder().expireAfterWrite(1000, MILLISECONDS).ticker(fakeTicker).build();

    cache.put(10, 20);
    fakeTicker.advance(500, MILLISECONDS);
    cache.put(20, 22);
    cache.put(5, 10);

    fakeTicker.advance(501, MILLISECONDS);

    Set<Integer> foundKeys = new HashSet<>(cache.asMap().keySet());

    assertThat(foundKeys).containsExactly(20, 5);
  }

  @Test
  public void asMapKeySet_contains() {
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder().expireAfterWrite(1000, MILLISECONDS).ticker(fakeTicker).build();

    cache.put(10, 20);
    fakeTicker.advance(500, MILLISECONDS);
    cache.put(20, 22);
    cache.put(5, 10);

    fakeTicker.advance(501, MILLISECONDS);

    assertThat(cache.asMap().keySet().contains(20)).isTrue();
    assertThat(cache.asMap().keySet().contains(5)).isTrue();
    assertThat(cache.asMap().keySet().contains(10)).isFalse();
  }

  @Test
  public void asMapEntrySet() {
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder().expireAfterWrite(1000, MILLISECONDS).ticker(fakeTicker).build();

    cache.put(10, 20);
    fakeTicker.advance(500, MILLISECONDS);
    cache.put(20, 22);
    cache.put(5, 10);

    fakeTicker.advance(501, MILLISECONDS);

    int sum = 0;
    for (Entry<Integer, Integer> current : cache.asMap().entrySet()) {
      sum += current.getKey() + current.getValue();
    }
    assertThat(sum).isEqualTo(57);
  }

  @Test
  public void asMapValues_iteratorRemove() {
    Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder().expireAfterWrite(1000, MILLISECONDS).ticker(fakeTicker).build();

    cache.put(10, 20);
    Iterator<Integer> iterator = cache.asMap().values().iterator();
    iterator.next();
    iterator.remove();

    assertThat(cache.size()).isEqualTo(0);
  }
}
