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
import static com.google.common.cache.TestingWeighers.constantWeigher;
import static com.google.common.cache.TestingWeighers.intKeyWeigher;
import static java.util.Arrays.asList;
import static org.truth0.Truth.ASSERT;

import com.google.common.cache.CacheTesting.Receiver;
import com.google.common.cache.LocalCache.ReferenceEntry;
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
      LoadingCache<Object, Object> cache = CacheBuilder.newBuilder()
          .maximumSize(i)
          .build(loader);
      assertEquals(i, CacheTesting.getTotalSegmentSize(cache));
    }
  }

  public void testEviction_setMaxSegmentWeight() {
    IdentityLoader<Object> loader = identityLoader();
    for (int i = 1; i < 1000; i++) {
      LoadingCache<Object, Object> cache = CacheBuilder.newBuilder()
          .maximumWeight(i)
          .weigher(constantWeigher(1))
          .build(loader);
      assertEquals(i, CacheTesting.getTotalSegmentSize(cache));
    }
  }

  public void testEviction_maxSizeOneSegment() {
    IdentityLoader<Integer> loader = identityLoader();
    LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder()
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

  public void testEviction_maxWeightOneSegment() {
    IdentityLoader<Integer> loader = identityLoader();
    LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .maximumWeight(2 * MAX_SIZE)
        .weigher(constantWeigher(2))
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
    LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder()
        .maximumSize(MAX_SIZE)
        .removalListener(removalListener)
        .build(loader);
    for (int i = 0; i < 2 * MAX_SIZE; i++) {
      cache.getUnchecked(i);
      assertTrue(cache.size() <= MAX_SIZE);
    }

    assertEquals(MAX_SIZE, CacheTesting.accessQueueSize(cache));
    assertEquals(MAX_SIZE, cache.size());
    CacheTesting.processPendingNotifications(cache);
    assertEquals(MAX_SIZE, removalListener.getCount());
    CacheTesting.checkValidState(cache);
  }

  public void testEviction_maxWeight() {
    CountingRemovalListener<Integer, Integer> removalListener = countingRemovalListener();
    IdentityLoader<Integer> loader = identityLoader();
    LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder()
        .maximumWeight(2 * MAX_SIZE)
        .weigher(constantWeigher(2))
        .removalListener(removalListener)
        .build(loader);
    for (int i = 0; i < 2 * MAX_SIZE; i++) {
      cache.getUnchecked(i);
      assertTrue(cache.size() <= MAX_SIZE);
    }

    assertEquals(MAX_SIZE, CacheTesting.accessQueueSize(cache));
    assertEquals(MAX_SIZE, cache.size());
    CacheTesting.processPendingNotifications(cache);
    assertEquals(MAX_SIZE, removalListener.getCount());
    CacheTesting.checkValidState(cache);
  }

  public void testUpdateRecency_onGet() {
    IdentityLoader<Integer> loader = identityLoader();
    final LoadingCache<Integer, Integer> cache =
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
    final LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder()
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
    LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .maximumSize(10)
        .build(loader);
    CacheTesting.warmUp(cache, 0, 10);
    Set<Integer> keySet = cache.asMap().keySet();
    ASSERT.that(keySet).has().exactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    // re-order
    getAll(cache, asList(0, 1, 2));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(3, 4, 5, 6, 7, 8, 9, 0, 1, 2);

    // evict 3, 4, 5
    getAll(cache, asList(10, 11, 12));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(6, 7, 8, 9, 0, 1, 2, 10, 11, 12);

    // re-order
    getAll(cache, asList(6, 7, 8));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(9, 0, 1, 2, 10, 11, 12, 6, 7, 8);

    // evict 9, 0, 1
    getAll(cache, asList(13, 14, 15));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(2, 10, 11, 12, 6, 7, 8, 13, 14, 15);
  }

  public void testEviction_weightedLru() {
    // test weighted lru within a single segment
    IdentityLoader<Integer> loader = identityLoader();
    LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .maximumWeight(45)
        .weigher(intKeyWeigher())
        .build(loader);
    CacheTesting.warmUp(cache, 0, 10);
    Set<Integer> keySet = cache.asMap().keySet();
    ASSERT.that(keySet).has().exactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    // re-order
    getAll(cache, asList(0, 1, 2));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(3, 4, 5, 6, 7, 8, 9, 0, 1, 2);

    // evict 3, 4, 5
    getAll(cache, asList(10));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(6, 7, 8, 9, 0, 1, 2, 10);

    // re-order
    getAll(cache, asList(6, 7, 8));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(9, 0, 1, 2, 10, 6, 7, 8);

    // evict 9, 1, 2, 10
    getAll(cache, asList(15));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(0, 6, 7, 8, 15);

    // fill empty space
    getAll(cache, asList(9));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(0, 6, 7, 8, 15, 9);

    // evict 6
    getAll(cache, asList(1));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(0, 7, 8, 15, 9, 1);
  }

  public void testEviction_overweight() {
    // test weighted lru within a single segment
    IdentityLoader<Integer> loader = identityLoader();
    LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .maximumWeight(45)
        .weigher(intKeyWeigher())
        .build(loader);
    CacheTesting.warmUp(cache, 0, 10);
    Set<Integer> keySet = cache.asMap().keySet();
    ASSERT.that(keySet).has().exactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    // add an at-the-maximum-weight entry
    getAll(cache, asList(45));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(0, 45);

    // add an over-the-maximum-weight entry
    getAll(cache, asList(46));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().item(0);
  }

  public void testEviction_invalidateAll() {
    // test that .invalidateAll() resets total weight state correctly
    IdentityLoader<Integer> loader = identityLoader();
    LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .maximumSize(10)
        .build(loader);

    Set<Integer> keySet = cache.asMap().keySet();
    ASSERT.that(keySet).isEmpty();

    // add 0, 1, 2, 3, 4
    getAll(cache, asList(0, 1, 2, 3, 4));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(0, 1, 2, 3, 4);

    // invalidate all
    cache.invalidateAll();
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).isEmpty();

    // add 5, 6, 7, 8, 9, 10, 11, 12
    getAll(cache, asList(5, 6, 7, 8, 9, 10, 11, 12));
    CacheTesting.drainRecencyQueues(cache);
    ASSERT.that(keySet).has().exactly(5, 6, 7, 8, 9, 10, 11, 12);
  }

  private void getAll(LoadingCache<Integer, Integer> cache, List<Integer> keys) {
    for (int i : keys) {
      cache.getUnchecked(i);
    }
  }
}
