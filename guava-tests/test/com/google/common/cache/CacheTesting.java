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
import static com.google.common.truth.Truth.assertWithMessage;
import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.base.Preconditions;
import com.google.common.cache.LocalCache.LocalLoadingCache;
import com.google.common.cache.LocalCache.Segment;
import com.google.common.cache.LocalCache.ValueReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.FakeTicker;
import java.lang.ref.Reference;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * A collection of utilities for {@link Cache} testing.
 *
 * @author mike nonemacher
 */
@SuppressWarnings("GuardedBy") // TODO(b/35466881): Fix or suppress.
@NullUnmarked
final class CacheTesting {

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
   * Forces the segment containing the given {@code key} to expand (see {@link Segment#expand()}).
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
    return checkNotNull(cache) instanceof LocalLoadingCache;
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

  static long getTotalSegmentSize(Cache<?, ?> cache) {
    LocalCache<?, ?> map = toLocalCache(cache);
    long totalSize = 0;
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
      assertThat(segment.isLocked()).isFalse();
      Map<?, ?> table = segmentTable(segment);
      // cleanup and then check count after we have a strong reference to all entries
      segment.cleanUp();
      // under high memory pressure keys/values may be nulled out but not yet enqueued
      assertThat(table.size()).isAtMost(segment.count);
      for (Entry<?, ?> entry : table.entrySet()) {
        assertThat(entry.getKey()).isNotNull();
        assertThat(entry.getValue()).isNotNull();
        assertThat(cchm.get(entry.getKey())).isSameInstanceAs(entry.getValue());
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
          assertThat(entries.add(current)).isTrue();
          if (prev != null) {
            assertThat(current.getPreviousInWriteQueue()).isSameInstanceAs(prev);
            assertThat(current).isSameInstanceAs(prev.getNextInWriteQueue());
            assertThat(prev.getWriteTime()).isAtMost(current.getWriteTime());
          }
          Object key = current.getKey();
          if (key != null) {
            assertThat(segment.getEntry(key, current.getHash())).isSameInstanceAs(current);
          }
          prev = current;
        }
        assertThat(entries).hasSize(segment.count);
      } else {
        assertThat(segment.writeQueue.isEmpty()).isTrue();
      }

      if (cchm.usesAccessQueue()) {
        Set<ReferenceEntry<?, ?>> entries = Sets.newIdentityHashSet();

        ReferenceEntry<?, ?> prev = null;
        for (ReferenceEntry<?, ?> current : segment.accessQueue) {
          assertThat(entries.add(current)).isTrue();
          if (prev != null) {
            assertThat(current.getPreviousInAccessQueue()).isSameInstanceAs(prev);
            assertThat(current).isSameInstanceAs(prev.getNextInAccessQueue());
            // read accesses may be slightly misordered
            assertThat(
                    prev.getAccessTime() <= current.getAccessTime()
                        || prev.getAccessTime() - current.getAccessTime() < 1000)
                .isTrue();
          }
          Object key = current.getKey();
          if (key != null) {
            assertThat(segment.getEntry(key, current.getHash())).isSameInstanceAs(current);
          }
          prev = current;
        }
        assertThat(entries).hasSize(segment.count);
      } else {
        assertThat(segment.accessQueue).isEmpty();
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
        assertThat(segment.recencyQueue).isEmpty();
        assertThat(segment.readCount.get()).isEqualTo(0);

        ReferenceEntry<?, ?> prev = null;
        for (ReferenceEntry<?, ?> current : segment.accessQueue) {
          if (prev != null) {
            assertThat(current.getPreviousInAccessQueue()).isSameInstanceAs(prev);
            assertThat(current).isSameInstanceAs(prev.getNextInAccessQueue());
          }
          Object key = current.getKey();
          if (key != null) {
            assertThat(segment.getEntry(key, current.getHash())).isSameInstanceAs(current);
          }
          prev = current;
        }
      }
    } else {
      for (Segment<?, ?> segment : map.segments) {
        assertThat(segment.recencyQueue).isEmpty();
      }
    }
  }

  static int segmentSize(Segment<?, ?> segment) {
    Map<?, ?> map = segmentTable(segment);
    return map.size();
  }

  static <K, V> Map<K, V> segmentTable(Segment<K, V> segment) {
    AtomicReferenceArray<? extends ReferenceEntry<K, V>> table = segment.table;
    Map<K, V> map = new LinkedHashMap<>();
    for (int i = 0; i < table.length(); i++) {
      for (ReferenceEntry<K, V> entry = table.get(i); entry != null; entry = entry.getNext()) {
        K key = entry.getKey();
        V value = entry.getValueReference().get();
        if (key != null && value != null) {
          assertThat(map.put(key, value)).isNull();
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
    return max(accessQueueSize(cache), writeQueueSize(cache));
  }

  static void processPendingNotifications(Cache<?, ?> cache) {
    if (hasLocalCache(cache)) {
      LocalCache<?, ?> cchm = toLocalCache(cache);
      cchm.processPendingNotifications();
    }
  }

  interface Receiver<T> {
    void accept(@Nullable T object);
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
      assertThat(accessQueueSize(cache)).isEqualTo(maxSize);
      assertThat(cache.size()).isEqualTo(maxSize);

      ReferenceEntry<?, ?> originalHead = segment.accessQueue.peek();
      @SuppressWarnings("unchecked")
      ReferenceEntry<Integer, Integer> entry = (ReferenceEntry<Integer, Integer>) originalHead;
      operation.accept(entry);
      drainRecencyQueue(segment);

      assertThat(segment.accessQueue.peek()).isNotSameInstanceAs(originalHead);
      assertThat(accessQueueSize(cache)).isEqualTo(cache.size());
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

    ticker.advance(2 * expiringTime, MILLISECONDS);

    long now = ticker.read();
    for (Segment<?, ?> segment : cchm.segments) {
      expireEntries(segment, now);
      assertWithMessage("Expiration queue must be empty by now")
          .that(writeQueueSize(segment))
          .isEqualTo(0);
      assertWithMessage("Expiration queue must be empty by now")
          .that(accessQueueSize(segment))
          .isEqualTo(0);
      assertWithMessage("Segments must be empty by now").that(segmentSize(segment)).isEqualTo(0);
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
    assertThat(cache.size()).isEqualTo(0);
    assertThat(cache.asMap().containsKey(null)).isFalse();
    assertThat(cache.asMap().containsKey(6)).isFalse();
    assertThat(cache.asMap().containsValue(null)).isFalse();
    assertThat(cache.asMap().containsValue(6)).isFalse();
    checkEmpty(cache.asMap());
  }

  static void checkEmpty(ConcurrentMap<?, ?> map) {
    checkEmpty(map.keySet());
    checkEmpty(map.values());
    checkEmpty(map.entrySet());
    assertThat(map).isEqualTo(ImmutableMap.of());
    assertThat(map.hashCode()).isEqualTo(ImmutableMap.of().hashCode());
    assertThat(map.toString()).isEqualTo(ImmutableMap.of().toString());

    if (map instanceof LocalCache) {
      LocalCache<?, ?> cchm = (LocalCache<?, ?>) map;

      checkValidState(cchm);
      assertThat(cchm.isEmpty()).isTrue();
      assertThat(cchm).isEmpty();
      for (LocalCache.Segment<?, ?> segment : cchm.segments) {
        assertThat(segment.count).isEqualTo(0);
        assertThat(segmentSize(segment)).isEqualTo(0);
        assertThat(segment.writeQueue.isEmpty()).isTrue();
        assertThat(segment.accessQueue.isEmpty()).isTrue();
      }
    }
  }

  static void checkEmpty(Collection<?> collection) {
    assertThat(collection.isEmpty()).isTrue();
    assertThat(collection).isEmpty();
    assertThat(collection.iterator().hasNext()).isFalse();
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

  private CacheTesting() {}
}
