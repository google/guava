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
import static java.util.Arrays.asList;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.cache.CacheTesting.Receiver;
import com.google.common.cache.CustomConcurrentHashMap.ReferenceEntry;
import com.google.common.cache.TestingCacheLoaders.IdentityLoader;
import com.google.common.cache.TestingRemovalListeners.CountingRemovalListener;

import junit.framework.TestCase;

import java.util.List;
import java.util.Set;

/**
 * Tests relating to cache eviction: what does and doesn't count toward maximumSize, what happens
 * when maximumSize is reached, etc.
 *
 * @author mike nonemacher
 */
public class CacheEvictionTest extends TestCase {
  static final int MAX_SIZE = 100;

  public void testEviction_setMaxSegmentSize() {
    IdentityLoader<Object> loader = identityLoader();
    for (int i = 1; i < 1000; i++) {
      Cache<Object, Object> cache = CacheBuilder.newBuilder().maximumSize(i).build(loader);
      assertEquals(i, CacheTesting.getTotalSegmentSize(cache));
    }
  }

  public void testEviction_maxSizeOneSegment() {
    IdentityLoader<Integer> loader = identityLoader();
    Cache<Integer, Integer> cache = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .maximumSize(MAX_SIZE)
        .build(loader);
    for (int i = 0; i < 2 * MAX_SIZE; i++) {
      cache.getUnchecked(i);
      assertEquals(Math.min(i + 1, MAX_SIZE), cache.size());
    }

    assertEquals(MAX_SIZE, cache.size());
    CacheTesting.checkValidState(cache);
  }

  public void testEviction_maxSize() {
    CountingRemovalListener<Integer, Integer> removalListener = countingRemovalListener();
    IdentityLoader<Integer> loader = identityLoader();
    Cache<Integer, Integer> cache = CacheBuilder.newBuilder()
        .maximumSize(MAX_SIZE)
        .removalListener(removalListener)
        .build(loader);
    for (int i = 0; i < 2 * MAX_SIZE; i++) {
      cache.getUnchecked(i);
      assertTrue(cache.size() <= MAX_SIZE);
    }

    assertEquals(MAX_SIZE, CacheTesting.evictionQueueSize(cache));
    assertEquals(MAX_SIZE, cache.size());
    CacheTesting.processPendingNotifications(cache);
    assertEquals(MAX_SIZE, removalListener.getCount());
    CacheTesting.checkValidState(cache);
  }

  public void testUpdateRecency_onGet() {
    IdentityLoader<Integer> loader = identityLoader();
    final Cache<Integer, Integer> cache =
        CacheBuilder.newBuilder().maximumSize(MAX_SIZE).build(loader);
    CacheTesting.checkRecency(cache, MAX_SIZE,
        new Receiver<ReferenceEntry<Integer, Integer>>() {
          @Override
          public void accept(ReferenceEntry<Integer, Integer> entry) {
            cache.getUnchecked(entry.getKey());
          }
        });
  }

  public void testUpdateRecency_onInvalidate() {
    IdentityLoader<Integer> loader = identityLoader();
    final Cache<Integer, Integer> cache = CacheBuilder.newBuilder()
        .maximumSize(MAX_SIZE)
        .concurrencyLevel(1)
        .build(loader);
    CacheTesting.checkRecency(cache, MAX_SIZE,
        new Receiver<ReferenceEntry<Integer, Integer>>() {
          @Override
          public void accept(ReferenceEntry<Integer, Integer> entry) {
            Integer key = entry.getKey();
            cache.invalidate(key);
          }
        });
  }

  public void testEviction_lru() {
    // test lru within a single segment
    IdentityLoader<Integer> loader = identityLoader();
    Cache<Integer, Integer> cache = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .maximumSize(10)
        .build(loader);
    CacheTesting.warmUp(cache, 0, 10);
    Set<Integer> keySet = cache.asMap().keySet();
    ASSERT.that(keySet).hasContentsAnyOrder(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    // re-order
    getAll(cache, asList(0, 1, 2));
    ASSERT.that(keySet).hasContentsAnyOrder(3, 4, 5, 6, 7, 8, 9, 0, 1, 2);

    // evict 3, 4, 5
    getAll(cache, asList(10, 11, 12));
    ASSERT.that(keySet).hasContentsAnyOrder(6, 7, 8, 9, 0, 1, 2, 10, 11, 12);

    // re-order
    getAll(cache, asList(6, 7, 8));
    ASSERT.that(keySet).hasContentsAnyOrder(9, 0, 1, 2, 10, 11, 12, 6, 7, 8);

    // evict 9, 0, 1
    getAll(cache, asList(13, 14, 15));
    ASSERT.that(keySet).hasContentsAnyOrder(2, 10, 11, 12, 6, 7, 8, 13, 14, 15);
  }

  private void getAll(Cache<Integer, Integer> cache, List<Integer> keys) {
    for (int i : keys) {
      cache.getUnchecked(i);
    }
    CacheTesting.drainRecencyQueues(cache);
  }
}
