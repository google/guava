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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

import com.google.common.base.Preconditions;
import com.google.common.cache.LocalCache.LocalLoadingCache;
import com.google.common.cache.LocalCache.Segment;
import com.google.common.cache.LocalCache.ValueReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.FakeTicker;
import java.lang.ref.Reference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * A collection of utilities for {@link Cache} testing.
 *
 * @author mike nonemacher
 */
@SuppressWarnings("GuardedBy") // TODO(b/35466881): Fix or suppress.
class CacheTesting {

  /**
   * Poke into the Cache internals to simulate garbage collection of the value associated with the
   * given key. This assumes that the associated entry is a WeakValueReference or a
   * SoftValueReference (and not a LoadingValueReference), and throws an IllegalStateException if
   * that assumption does not hold.
   */
  @SuppressWarnings("unchecked") // the instanceof check and the cast generate this warning
  static <K, V> void simulateValueReclamation(Cache<K, V> cache, K key) {
    ReferenceEntry<K, V> entry = getReferenceEntry(cache, key);
    if (entry != null) {
      ValueReference<K, V> valueRef = entry.getValueReference();
      // fail on strong/computing refs
      Preconditions.checkState(valueRef instanceof Reference);
      Reference<V> ref = (Reference<V>) valueRef;
      if (ref != null) {
        ref.clear();
      }
    }
  }

  /**
   * Poke into the Cache internals to simulate garbage collection of the given key. This assumes
   * that the given entry is a weak or soft reference, and throws an IllegalStateException if that
   * assumption does not hold.
   */
  @SuppressWarnings("unchecked") // the instanceof check and the cast generate this warning
  static <K, V> void simulateKeyReclamation(Cache<K, V> cache, K key) {
    ReferenceEntry<K, V> entry = getReferenceEntry(cache, key);

    Preconditions.checkState(entry instanceof Reference);
    Reference<?> ref = (Reference<?>) entry;
    if (ref != null) {
      ref.clear();
    }
  }

  static <K, V> ReferenceEntry<K, V> getReferenceEntry(Cache<K, V> cache, K key) {
    checkNotNull(cache);
    checkNotNull(key);
    LocalCache<K, V> map = toLocalCache(cache);
    return map.getEntry(key);
  }

  /**
   * Forces the segment containing the given {@code key} to expand (see {@link Segment#expand()}.
   */
  static <K, V> void forceExpandSegment(Cache<K, V> cache, K key) {
    checkNotNull(cache);
    checkNotNull(key);
    LocalCache<K, V> map = toLocalCache(cache);
    int hash = map.hash(key);
    Segment<K, V> segment = map.segmentFor(hash);
    segment.expand();
  }

  /**
   * Gets the {@link LocalCache} used by the given {@link Cache}, if any, or throws an
   * IllegalArgumentException if this is a Cache type that doesn't have a LocalCache.
   */
  static <K, V> LocalCache<K, V> toLocalCache(Cache<K, V> cache) {
    if (cache instanceof LocalLoadingCache) {
      return ((LocalLoadingCache<K, V>) cache).localCache;
    }
    throw new IllegalArgumentException(
        "Cache of type " + cache.getClass() + " doesn't have a LocalCache.");
  }

  /**
   * Determines whether the given cache can be converted to a LocalCache by {@link #toLocalCache}
   * without throwing an exception.
   */
  static boolean hasLocalCache(Cache<?, ?> cache) {
    return (checkNotNull(cache) instanceof LocalLoadingCache);
  }

  static void drainRecencyQueues(Cache<?, ?> cache) {
    if (hasLocalCache(cache)) {
      LocalCache<?, ?> map = toLocalCache(cache);
      for (Segment<?, ?> segment : map.segments) {
        drainRecencyQueue(segment);
      }
    }
  }

  static void drainRecencyQueue(Segment<?, ?> segment) {
    segment.lock();
    try {
      segment.cleanUp();
    } finally {
      segment.unlock();
    }
  }

  static void drainReferenceQueues(Cache<?, ?> cache) {
    if (hasLocalCache(cache)) {
      drainReferenceQueues(toLocalCache(cache));
    }
  }

  static void drainReferenceQueues(LocalCache<?, ?> cchm) {
    for (LocalCache.Segment<?, ?> segment : cchm.segments) {
      drainReferenceQueue(segment);
    }
  }

  static void drainReferenceQueue(LocalCache.Segment<?, ?> segment) {
    segment.lock();
    try {
      segment.drainReferenceQueues();
    } finally {
      segment.unlock();
    }
  }

  static int getTotalSegmentSize(Cache<?, ?> cache) {
    LocalCache<?, ?> map = toLocalCache(cache);
    int totalSize = 0;
    for (Segment<?, ?> segment : map.segments) {
      totalSize += segment.maxSegmentWeight;
    }
    return totalSize;
  }

  /**
   * Peeks into the cache's internals to check its internal consistency. Verifies that each
   * segment's count matches its #elements (after cleanup), each segment is unlocked, each entry
   * contains a non-null key and value, and the eviction and expiration queues are consistent (see
   * {@link #checkEviction}, {@link #checkExpiration}).
   */
  static void checkValidState(Cache<?, ?> cache) {
    if (hasLocalCache(cache)) {
      checkValidState(toLocalCache(cache));
    }
  }

  static void checkValidState(LocalCache<?, ?> cchm) {
    for (Segment<?, ?> segment : cchm.segments) {
      segment.cleanUp();
      assertFalse(segment.isLocked());
      Map<?, ?> table = segmentTable(segment);
      // cleanup and then check count after we have a strong reference to all entries
      segment.cleanUp();
      // under high memory pressure keys/values may be nulled out but not yet enqueued
      assertThat(table.size()).isAtMost(segment.count);
      for (Entry<?, ?> entry : table.entrySet()) {
        assertNotNull(entry.getKey());
        assertNotNull(entry.getValue());
        assertSame(entry.getValue(), cchm.get(entry.getKey()));
      }
    }
    checkEviction(cchm);
    checkExpiration(cchm);
  }

  /**
   * Peeks into the cache's internals to verify that its expiration queue is consistent. Verifies
   * that the next/prev links in the expiration queue are correct, and that the queue is ordered by
   * expiration time.
   */
  static void checkExpiration(Cache<?, ?> cache) {
    if (hasLocalCache(cache)) {
      checkExpiration(toLocalCache(cache));
    }
  }

  static void checkExpiration(LocalCache<?, ?> cchm) {
    for (Segment<?, ?> segment : cchm.segments) {
      if (cchm.usesWriteQueue()) {
        Set<ReferenceEntry<?, ?>> entries = Sets.newIdentityHashSet();

        ReferenceEntry<?, ?> prev = null;
        for (ReferenceEntry<?, ?> current : segment.writeQueue) {
          assertTrue(entries.add(current));
          if (prev != null) {
            assertSame(prev, current.getPreviousInWriteQueue());
            assertSame(prev.getNextInWriteQueue(), current);
            assertThat(prev.getWriteTime()).isAtMost(current.getWriteTime());
          }
          Object key = current.getKey();
          if (key != null) {
            assertSame(current, segment.getEntry(key, current.getHash()));
          }
          prev = current;
        }
        assertEquals(segment.count, entries.size());
      } else {
        assertTrue(segment.writeQueue.isEmpty());
      }

      if (cchm.usesAccessQueue()) {
        Set<ReferenceEntry<?, ?>> entries = Sets.newIdentityHashSet();

        ReferenceEntry<?, ?> prev = null;
        for (ReferenceEntry<?, ?> current : segment.accessQueue) {
          assertTrue(entries.add(current));
          if (prev != null) {
            assertSame(prev, current.getPreviousInAccessQueue());
            assertSame(prev.getNextInAccessQueue(), current);
            // read accesses may be slightly misordered
            assertTrue(
                prev.getAccessTime() <= current.getAccessTime()
                    || prev.getAccessTime() - current.getAccessTime() < 1000);
          }
          Object key = current.getKey();
          if (key != null) {
            assertSame(current, segment.getEntry(key, current.getHash()));
          }
          prev = current;
        }
        assertEquals(segment.count, entries.size());
      } else {
        assertTrue(segment.accessQueue.isEmpty());
      }
    }
  }

  /**
   * Peeks into the cache's internals to verify that its eviction queue is consistent. Verifies that
   * the prev/next links are correct, and that all items in each segment are also in that segment's
   * eviction (recency) queue.
   */
  static void checkEviction(Cache<?, ?> cache) {
    if (hasLocalCache(cache)) {
      checkEviction(toLocalCache(cache));
    }
  }

  static void checkEviction(LocalCache<?, ?> map) {
    if (map.evictsBySize()) {
      for (Segment<?, ?> segment : map.segments) {
        drainRecencyQueue(segment);
        assertEquals(0, segment.recencyQueue.size());
        assertEquals(0, segment.readCount.get());

        ReferenceEntry<?, ?> prev = null;
        for (ReferenceEntry<?, ?> current : segment.accessQueue) {
          if (prev != null) {
            assertSame(prev, current.getPreviousInAccessQueue());
            assertSame(prev.getNextInAccessQueue(), current);
          }
          Object key = current.getKey();
          if (key != null) {
            assertSame(current, segment.getEntry(key, current.getHash()));
          }
          prev = current;
        }
      }
    } else {
      for (Segment<?, ?> segment : map.segments) {
        assertEquals(0, segment.recencyQueue.size());
      }
    }
  }

  static int segmentSize(Segment<?, ?> segment) {
    Map<?, ?> map = segmentTable(segment);
    return map.size();
  }

  static <K, V> Map<K, V> segmentTable(Segment<K, V> segment) {
    AtomicReferenceArray<? extends ReferenceEntry<K, V>> table = segment.table;
    Map<K, V> map = Maps.newLinkedHashMap();
    for (int i = 0; i < table.length(); i++) {
      for (ReferenceEntry<K, V> entry = table.get(i); entry != null; entry = entry.getNext()) {
        K key = entry.getKey();
        V value = entry.getValueReference().get();
        if (key != null && value != null) {
          assertNull(map.put(key, value));
        }
      }
    }
    return map;
  }

  static int writeQueueSize(Cache<?, ?> cache) {
    LocalCache<?, ?> cchm = toLocalCache(cache);

    int size = 0;
    for (Segment<?, ?> segment : cchm.segments) {
      size += writeQueueSize(segment);
    }
    return size;
  }

  static int writeQueueSize(Segment<?, ?> segment) {
    return segment.writeQueue.size();
  }

  static int accessQueueSize(Cache<?, ?> cache) {
    LocalCache<?, ?> cchm = toLocalCache(cache);
    int size = 0;
    for (Segment<?, ?> segment : cchm.segments) {
      size += accessQueueSize(segment);
    }
    return size;
  }

  static int accessQueueSize(Segment<?, ?> segment) {
    return segment.accessQueue.size();
  }

  static int expirationQueueSize(Cache<?, ?> cache) {
    return Math.max(accessQueueSize(cache), writeQueueSize(cache));
  }

  static void processPendingNotifications(Cache<?, ?> cache) {
    if (hasLocalCache(cache)) {
      LocalCache<?, ?> cchm = toLocalCache(cache);
      cchm.processPendingNotifications();
    }
  }

  interface Receiver<T> {
    void accept(@NullableDecl T object);
  }

  /**
   * Assuming the given cache has maximum size {@code maxSize}, this method populates the cache (by
   * getting a bunch of different keys), then makes sure all the items in the cache are also in the
   * eviction queue. It will invoke the given {@code operation} on the first element in the eviction
   * queue, and then reverify that all items in the cache are in the eviction queue, and verify that
   * the head of the eviction queue has changed as a result of the operation.
   */
  static void checkRecency(
      LoadingCache<Integer, Integer> cache,
      int maxSize,
      Receiver<ReferenceEntry<Integer, Integer>> operation) {
    checkNotNull(operation);
    if (hasLocalCache(cache)) {
      warmUp(cache, 0, 2 * maxSize);

      LocalCache<Integer, Integer> cchm = toLocalCache(cache);
      Segment<?, ?> segment = cchm.segments[0];
      drainRecencyQueue(segment);
      assertEquals(maxSize, accessQueueSize(cache));
      assertEquals(maxSize, cache.size());

      ReferenceEntry<?, ?> originalHead = segment.accessQueue.peek();
      @SuppressWarnings("unchecked")
      ReferenceEntry<Integer, Integer> entry = (ReferenceEntry<Integer, Integer>) originalHead;
      operation.accept(entry);
      drainRecencyQueue(segment);

      assertNotSame(originalHead, segment.accessQueue.peek());
      assertEquals(cache.size(), accessQueueSize(cache));
    }
  }

  /** Warms the given cache by getting all values in {@code [start, end)}, in order. */
  static void warmUp(LoadingCache<Integer, Integer> map, int start, int end) {
    checkNotNull(map);
    for (int i = start; i < end; i++) {
      map.getUnchecked(i);
    }
  }

  static void expireEntries(Cache<?, ?> cache, long expiringTime, FakeTicker ticker) {
    checkNotNull(ticker);
    expireEntries(toLocalCache(cache), expiringTime, ticker);
  }

  static void expireEntries(LocalCache<?, ?> cchm, long expiringTime, FakeTicker ticker) {

    for (Segment<?, ?> segment : cchm.segments) {
      drainRecencyQueue(segment);
    }

    ticker.advance(2 * expiringTime, TimeUnit.MILLISECONDS);

    long now = ticker.read();
    for (Segment<?, ?> segment : cchm.segments) {
      expireEntries(segment, now);
      assertEquals("Expiration queue must be empty by now", 0, writeQueueSize(segment));
      assertEquals("Expiration queue must be empty by now", 0, accessQueueSize(segment));
      assertEquals("Segments must be empty by now", 0, segmentSize(segment));
    }
    cchm.processPendingNotifications();
  }

  static void expireEntries(Segment<?, ?> segment, long now) {
    segment.lock();
    try {
      segment.expireEntries(now);
      segment.cleanUp();
    } finally {
      segment.unlock();
    }
  }

  static void checkEmpty(Cache<?, ?> cache) {
    assertEquals(0, cache.size());
    assertFalse(cache.asMap().containsKey(null));
    assertFalse(cache.asMap().containsKey(6));
    assertFalse(cache.asMap().containsValue(null));
    assertFalse(cache.asMap().containsValue(6));
    checkEmpty(cache.asMap());
  }

  static void checkEmpty(ConcurrentMap<?, ?> map) {
    checkEmpty(map.keySet());
    checkEmpty(map.values());
    checkEmpty(map.entrySet());
    assertEquals(ImmutableMap.of(), map);
    assertEquals(ImmutableMap.of().hashCode(), map.hashCode());
    assertEquals(ImmutableMap.of().toString(), map.toString());

    if (map instanceof LocalCache) {
      LocalCache<?, ?> cchm = (LocalCache<?, ?>) map;

      checkValidState(cchm);
      assertTrue(cchm.isEmpty());
      assertEquals(0, cchm.size());
      for (LocalCache.Segment<?, ?> segment : cchm.segments) {
        assertEquals(0, segment.count);
        assertEquals(0, segmentSize(segment));
        assertTrue(segment.writeQueue.isEmpty());
        assertTrue(segment.accessQueue.isEmpty());
      }
    }
  }

  static void checkEmpty(Collection<?> collection) {
    assertTrue(collection.isEmpty());
    assertEquals(0, collection.size());
    assertFalse(collection.iterator().hasNext());
    assertThat(collection.toArray()).isEmpty();
    assertThat(collection.toArray(new Object[0])).isEmpty();
    if (collection instanceof Set) {
      new EqualsTester()
          .addEqualityGroup(ImmutableSet.of(), collection)
          .addEqualityGroup(ImmutableSet.of(""))
          .testEquals();
    } else if (collection instanceof List) {
      new EqualsTester()
          .addEqualityGroup(ImmutableList.of(), collection)
          .addEqualityGroup(ImmutableList.of(""))
          .testEquals();
    }
  }
}
