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

import static com.google.common.cache.CacheBuilder.NULL_TICKER;
import static com.google.common.cache.LocalCache.DISCARDING_QUEUE;
import static com.google.common.cache.LocalCache.DRAIN_THRESHOLD;
import static com.google.common.cache.LocalCache.nullEntry;
import static com.google.common.cache.LocalCache.unset;
import static com.google.common.cache.TestingCacheLoaders.identityLoader;
import static com.google.common.cache.TestingRemovalListeners.countingRemovalListener;
import static com.google.common.cache.TestingRemovalListeners.queuingRemovalListener;
import static com.google.common.cache.TestingWeighers.constantWeigher;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Equivalence;
import com.google.common.base.Ticker;
import com.google.common.cache.LocalCache.EntryFactory;
import com.google.common.cache.LocalCache.LoadingValueReference;
import com.google.common.cache.LocalCache.LocalLoadingCache;
import com.google.common.cache.LocalCache.LocalManualCache;
import com.google.common.cache.LocalCache.Segment;
import com.google.common.cache.LocalCache.Strength;
import com.google.common.cache.LocalCache.ValueReference;
import com.google.common.cache.TestingCacheLoaders.CountingLoader;
import com.google.common.cache.TestingRemovalListeners.CountingRemovalListener;
import com.google.common.cache.TestingRemovalListeners.QueuingRemovalListener;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.testing.ConcurrentMapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.FakeTicker;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import com.google.common.testing.TestLogHandler;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.LogRecord;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/** @author Charles Fry */
@SuppressWarnings("GuardedBy") // TODO(b/35466881): Fix or suppress.
public class LocalCacheTest extends TestCase {
  private static class TestStringCacheGenerator extends TestStringMapGenerator {
    private final CacheBuilder<? super String, ? super String> builder;

    TestStringCacheGenerator(CacheBuilder<? super String, ? super String> builder) {
      this.builder = builder;
    }

    @Override
    protected Map<String, String> create(Entry<String, String>[] entries) {
      LocalCache<String, String> map = makeLocalCache(builder);
      for (Entry<String, String> entry : entries) {
        map.put(entry.getKey(), entry.getValue());
      }
      return map;
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(LocalCacheTest.class);
    suite.addTest(
        ConcurrentMapTestSuiteBuilder.using(new TestStringCacheGenerator(createCacheBuilder()))
            .named("LocalCache with defaults")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        ConcurrentMapTestSuiteBuilder.using(
                new TestStringCacheGenerator(createCacheBuilder().concurrencyLevel(1)))
            .named("LocalCache with concurrencyLevel[1]")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        ConcurrentMapTestSuiteBuilder.using(
                new TestStringCacheGenerator(createCacheBuilder().maximumSize(Integer.MAX_VALUE)))
            .named("LocalCache with maximumSize")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        ConcurrentMapTestSuiteBuilder.using(
                new TestStringCacheGenerator(
                    createCacheBuilder()
                        .maximumWeight(Integer.MAX_VALUE)
                        .weigher(new SerializableWeigher<String, String>())))
            .named("LocalCache with maximumWeight")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        ConcurrentMapTestSuiteBuilder.using(
                new TestStringCacheGenerator(createCacheBuilder().weakKeys()))
            .named("LocalCache with weakKeys") // keys are string literals and won't be GC'd
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        ConcurrentMapTestSuiteBuilder.using(
                new TestStringCacheGenerator(createCacheBuilder().weakValues()))
            .named("LocalCache with weakValues") // values are string literals and won't be GC'd
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        ConcurrentMapTestSuiteBuilder.using(
                new TestStringCacheGenerator(createCacheBuilder().softValues()))
            .named("LocalCache with softValues") // values are string literals and won't be GC'd
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        ConcurrentMapTestSuiteBuilder.using(
                new TestStringCacheGenerator(
                    createCacheBuilder()
                        .expireAfterAccess(1, SECONDS)
                        .ticker(new SerializableTicker())))
            .named("LocalCache with expireAfterAccess") // SerializableTicker never advances
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        ConcurrentMapTestSuiteBuilder.using(
                new TestStringCacheGenerator(
                    createCacheBuilder()
                        .expireAfterWrite(1, SECONDS)
                        .ticker(new SerializableTicker())))
            .named("LocalCache with expireAfterWrite") // SerializableTicker never advances
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        ConcurrentMapTestSuiteBuilder.using(
                new TestStringCacheGenerator(
                    createCacheBuilder()
                        .removalListener(new SerializableRemovalListener<String, String>())))
            .named("LocalCache with removalListener")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    suite.addTest(
        ConcurrentMapTestSuiteBuilder.using(
                new TestStringCacheGenerator(createCacheBuilder().recordStats()))
            .named("LocalCache with recordStats")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());
    return suite;
  }

  static final int SMALL_MAX_SIZE = DRAIN_THRESHOLD * 5;

  TestLogHandler logHandler;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    logHandler = new TestLogHandler();
    LocalCache.logger.addHandler(logHandler);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    LocalCache.logger.removeHandler(logHandler);
  }

  private Throwable popLoggedThrowable() {
    List<LogRecord> logRecords = logHandler.getStoredLogRecords();
    assertEquals(1, logRecords.size());
    LogRecord logRecord = logRecords.get(0);
    logHandler.clear();
    return logRecord.getThrown();
  }

  private void checkNothingLogged() {
    assertTrue(logHandler.getStoredLogRecords().isEmpty());
  }

  private void checkLogged(Throwable t) {
    assertSame(t, popLoggedThrowable());
  }

  private static <K, V> LocalCache<K, V> makeLocalCache(
      CacheBuilder<? super K, ? super V> builder) {
    return new LocalCache<>(builder, null);
  }

  private static CacheBuilder<Object, Object> createCacheBuilder() {
    return CacheBuilder.newBuilder();
  }

  // constructor tests

  public void testDefaults() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder());

    assertSame(Strength.STRONG, map.keyStrength);
    assertSame(Strength.STRONG, map.valueStrength);
    assertSame(map.keyStrength.defaultEquivalence(), map.keyEquivalence);
    assertSame(map.valueStrength.defaultEquivalence(), map.valueEquivalence);

    assertEquals(0, map.expireAfterAccessNanos);
    assertEquals(0, map.expireAfterWriteNanos);
    assertEquals(0, map.refreshNanos);
    assertEquals(CacheBuilder.UNSET_INT, map.maxWeight);

    assertSame(EntryFactory.STRONG, map.entryFactory);
    assertSame(CacheBuilder.NullListener.INSTANCE, map.removalListener);
    assertSame(DISCARDING_QUEUE, map.removalNotificationQueue);
    assertSame(NULL_TICKER, map.ticker);

    assertEquals(4, map.concurrencyLevel);

    // concurrency level
    assertThat(map.segments).hasLength(4);
    // initial capacity / concurrency level
    assertEquals(16 / map.segments.length, map.segments[0].table.length());

    assertFalse(map.evictsBySize());
    assertFalse(map.expires());
    assertFalse(map.expiresAfterWrite());
    assertFalse(map.expiresAfterAccess());
    assertFalse(map.refreshes());
  }

  public void testSetKeyEquivalence() {
    Equivalence<Object> testEquivalence =
        new Equivalence<Object>() {
          @Override
          protected boolean doEquivalent(Object a, Object b) {
            return false;
          }

          @Override
          protected int doHash(Object t) {
            return 0;
          }
        };

    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().keyEquivalence(testEquivalence));
    assertSame(testEquivalence, map.keyEquivalence);
    assertSame(map.valueStrength.defaultEquivalence(), map.valueEquivalence);
  }

  public void testSetValueEquivalence() {
    Equivalence<Object> testEquivalence =
        new Equivalence<Object>() {
          @Override
          protected boolean doEquivalent(Object a, Object b) {
            return false;
          }

          @Override
          protected int doHash(Object t) {
            return 0;
          }
        };

    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().valueEquivalence(testEquivalence));
    assertSame(testEquivalence, map.valueEquivalence);
    assertSame(map.keyStrength.defaultEquivalence(), map.keyEquivalence);
  }

  public void testSetConcurrencyLevel() {
    // round up to the nearest power of two

    checkConcurrencyLevel(1, 1);
    checkConcurrencyLevel(2, 2);
    checkConcurrencyLevel(3, 4);
    checkConcurrencyLevel(4, 4);
    checkConcurrencyLevel(5, 8);
    checkConcurrencyLevel(6, 8);
    checkConcurrencyLevel(7, 8);
    checkConcurrencyLevel(8, 8);
  }

  private static void checkConcurrencyLevel(int concurrencyLevel, int segmentCount) {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(concurrencyLevel));
    assertThat(map.segments).hasLength(segmentCount);
  }

  public void testSetInitialCapacity() {
    // share capacity over each segment, then round up to the nearest power of two

    checkInitialCapacity(1, 0, 1);
    checkInitialCapacity(1, 1, 1);
    checkInitialCapacity(1, 2, 2);
    checkInitialCapacity(1, 3, 4);
    checkInitialCapacity(1, 4, 4);
    checkInitialCapacity(1, 5, 8);
    checkInitialCapacity(1, 6, 8);
    checkInitialCapacity(1, 7, 8);
    checkInitialCapacity(1, 8, 8);

    checkInitialCapacity(2, 0, 1);
    checkInitialCapacity(2, 1, 1);
    checkInitialCapacity(2, 2, 1);
    checkInitialCapacity(2, 3, 2);
    checkInitialCapacity(2, 4, 2);
    checkInitialCapacity(2, 5, 4);
    checkInitialCapacity(2, 6, 4);
    checkInitialCapacity(2, 7, 4);
    checkInitialCapacity(2, 8, 4);

    checkInitialCapacity(4, 0, 1);
    checkInitialCapacity(4, 1, 1);
    checkInitialCapacity(4, 2, 1);
    checkInitialCapacity(4, 3, 1);
    checkInitialCapacity(4, 4, 1);
    checkInitialCapacity(4, 5, 2);
    checkInitialCapacity(4, 6, 2);
    checkInitialCapacity(4, 7, 2);
    checkInitialCapacity(4, 8, 2);
  }

  private static void checkInitialCapacity(
      int concurrencyLevel, int initialCapacity, int segmentSize) {
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(concurrencyLevel)
                .initialCapacity(initialCapacity));
    for (int i = 0; i < map.segments.length; i++) {
      assertEquals(segmentSize, map.segments[i].table.length());
    }
  }

  public void testSetMaximumSize() {
    // vary maximumSize wrt concurrencyLevel

    for (int maxSize = 1; maxSize < 100; maxSize++) {
      checkMaximumSize(1, 8, maxSize);
      checkMaximumSize(2, 8, maxSize);
      checkMaximumSize(4, 8, maxSize);
      checkMaximumSize(8, 8, maxSize);
    }

    checkMaximumSize(1, 8, Long.MAX_VALUE);
    checkMaximumSize(2, 8, Long.MAX_VALUE);
    checkMaximumSize(4, 8, Long.MAX_VALUE);
    checkMaximumSize(8, 8, Long.MAX_VALUE);

    // vary initial capacity wrt maximumSize

    for (int capacity = 0; capacity < 8; capacity++) {
      checkMaximumSize(1, capacity, 4);
      checkMaximumSize(2, capacity, 4);
      checkMaximumSize(4, capacity, 4);
      checkMaximumSize(8, capacity, 4);
    }
  }

  private static void checkMaximumSize(int concurrencyLevel, int initialCapacity, long maxSize) {
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(concurrencyLevel)
                .initialCapacity(initialCapacity)
                .maximumSize(maxSize));
    long totalCapacity = 0;
    assertTrue(
        "segments=" + map.segments.length + ", maxSize=" + maxSize,
        map.segments.length <= Math.max(1, maxSize / 10));
    for (int i = 0; i < map.segments.length; i++) {
      totalCapacity += map.segments[i].maxSegmentWeight;
    }
    assertTrue("totalCapacity=" + totalCapacity + ", maxSize=" + maxSize, totalCapacity == maxSize);

    map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(concurrencyLevel)
                .initialCapacity(initialCapacity)
                .maximumWeight(maxSize)
                .weigher(constantWeigher(1)));
    assertTrue(
        "segments=" + map.segments.length + ", maxSize=" + maxSize,
        map.segments.length <= Math.max(1, maxSize / 10));
    totalCapacity = 0;
    for (int i = 0; i < map.segments.length; i++) {
      totalCapacity += map.segments[i].maxSegmentWeight;
    }
    assertTrue("totalCapacity=" + totalCapacity + ", maxSize=" + maxSize, totalCapacity == maxSize);
  }

  public void testSetWeigher() {
    Weigher<Object, Object> testWeigher =
        new Weigher<Object, Object>() {
          @Override
          public int weigh(Object key, Object value) {
            return 42;
          }
        };
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().maximumWeight(1).weigher(testWeigher));
    assertSame(testWeigher, map.weigher);
  }

  public void testSetWeakKeys() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().weakKeys());
    checkStrength(map, Strength.WEAK, Strength.STRONG);
    assertSame(EntryFactory.WEAK, map.entryFactory);
  }

  public void testSetWeakValues() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().weakValues());
    checkStrength(map, Strength.STRONG, Strength.WEAK);
    assertSame(EntryFactory.STRONG, map.entryFactory);
  }

  public void testSetSoftValues() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().softValues());
    checkStrength(map, Strength.STRONG, Strength.SOFT);
    assertSame(EntryFactory.STRONG, map.entryFactory);
  }

  private static void checkStrength(
      LocalCache<Object, Object> map, Strength keyStrength, Strength valueStrength) {
    assertSame(keyStrength, map.keyStrength);
    assertSame(valueStrength, map.valueStrength);
    assertSame(keyStrength.defaultEquivalence(), map.keyEquivalence);
    assertSame(valueStrength.defaultEquivalence(), map.valueEquivalence);
  }

  public void testSetExpireAfterWrite() {
    long duration = 42;
    TimeUnit unit = TimeUnit.SECONDS;
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().expireAfterWrite(duration, unit));
    assertEquals(unit.toNanos(duration), map.expireAfterWriteNanos);
  }

  public void testSetExpireAfterAccess() {
    long duration = 42;
    TimeUnit unit = TimeUnit.SECONDS;
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().expireAfterAccess(duration, unit));
    assertEquals(unit.toNanos(duration), map.expireAfterAccessNanos);
  }

  public void testSetRefresh() {
    long duration = 42;
    TimeUnit unit = TimeUnit.SECONDS;
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().refreshAfterWrite(duration, unit));
    assertEquals(unit.toNanos(duration), map.refreshNanos);
  }

  public void testSetRemovalListener() {
    RemovalListener<Object, Object> testListener = TestingRemovalListeners.nullRemovalListener();
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().removalListener(testListener));
    assertSame(testListener, map.removalListener);
  }

  public void testSetTicker() {
    Ticker testTicker =
        new Ticker() {
          @Override
          public long read() {
            return 0;
          }
        };
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().ticker(testTicker));
    assertSame(testTicker, map.ticker);
  }

  public void testEntryFactory() {
    assertSame(EntryFactory.STRONG, EntryFactory.getFactory(Strength.STRONG, false, false));
    assertSame(EntryFactory.STRONG_ACCESS, EntryFactory.getFactory(Strength.STRONG, true, false));
    assertSame(EntryFactory.STRONG_WRITE, EntryFactory.getFactory(Strength.STRONG, false, true));
    assertSame(
        EntryFactory.STRONG_ACCESS_WRITE, EntryFactory.getFactory(Strength.STRONG, true, true));
    assertSame(EntryFactory.WEAK, EntryFactory.getFactory(Strength.WEAK, false, false));
    assertSame(EntryFactory.WEAK_ACCESS, EntryFactory.getFactory(Strength.WEAK, true, false));
    assertSame(EntryFactory.WEAK_WRITE, EntryFactory.getFactory(Strength.WEAK, false, true));
    assertSame(EntryFactory.WEAK_ACCESS_WRITE, EntryFactory.getFactory(Strength.WEAK, true, true));
  }

  // computation tests

  public void testCompute() throws ExecutionException {
    CountingLoader loader = new CountingLoader();
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder());
    assertEquals(0, loader.getCount());

    Object key = new Object();
    Object value = map.get(key, loader);
    assertEquals(1, loader.getCount());
    assertEquals(value, map.get(key, loader));
    assertEquals(1, loader.getCount());
  }

  public void testRecordReadOnCompute() throws ExecutionException {
    CountingLoader loader = new CountingLoader();
    for (CacheBuilder<Object, Object> builder : allEvictingMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];
      List<ReferenceEntry<Object, Object>> writeOrder = Lists.newLinkedList();
      List<ReferenceEntry<Object, Object>> readOrder = Lists.newLinkedList();
      for (int i = 0; i < SMALL_MAX_SIZE; i++) {
        Object key = new Object();
        int hash = map.hash(key);

        map.get(key, loader);
        ReferenceEntry<Object, Object> entry = segment.getEntry(key, hash);
        writeOrder.add(entry);
        readOrder.add(entry);
      }

      checkEvictionQueues(map, segment, readOrder, writeOrder);
      checkExpirationTimes(map);
      assertTrue(segment.recencyQueue.isEmpty());

      // access some of the elements
      Random random = new Random();
      List<ReferenceEntry<Object, Object>> reads = Lists.newArrayList();
      Iterator<ReferenceEntry<Object, Object>> i = readOrder.iterator();
      while (i.hasNext()) {
        ReferenceEntry<Object, Object> entry = i.next();
        if (random.nextBoolean()) {
          map.get(entry.getKey(), loader);
          reads.add(entry);
          i.remove();
          assertTrue(segment.recencyQueue.size() <= DRAIN_THRESHOLD);
        }
      }
      int undrainedIndex = reads.size() - segment.recencyQueue.size();
      checkAndDrainRecencyQueue(map, segment, reads.subList(undrainedIndex, reads.size()));
      readOrder.addAll(reads);

      checkEvictionQueues(map, segment, readOrder, writeOrder);
      checkExpirationTimes(map);
    }
  }

  public void testComputeExistingEntry() throws ExecutionException {
    CountingLoader loader = new CountingLoader();
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder());
    assertEquals(0, loader.getCount());

    Object key = new Object();
    Object value = new Object();
    map.put(key, value);

    assertEquals(value, map.get(key, loader));
    assertEquals(0, loader.getCount());
  }

  public void testComputePartiallyCollectedKey() throws ExecutionException {
    CacheBuilder<Object, Object> builder = createCacheBuilder().concurrencyLevel(1);
    CountingLoader loader = new CountingLoader();
    LocalCache<Object, Object> map = makeLocalCache(builder);
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(0, loader.getCount());

    Object key = new Object();
    int hash = map.hash(key);
    Object value = new Object();
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> valueRef = DummyValueReference.create(value);
    entry.setValueReference(valueRef);
    table.set(index, entry);
    segment.count++;

    assertSame(value, map.get(key, loader));
    assertEquals(0, loader.getCount());
    assertEquals(1, segment.count);

    entry.clearKey();
    assertNotSame(value, map.get(key, loader));
    assertEquals(1, loader.getCount());
    assertEquals(2, segment.count);
  }

  public void testComputePartiallyCollectedValue() throws ExecutionException {
    CacheBuilder<Object, Object> builder = createCacheBuilder().concurrencyLevel(1);
    CountingLoader loader = new CountingLoader();
    LocalCache<Object, Object> map = makeLocalCache(builder);
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(0, loader.getCount());

    Object key = new Object();
    int hash = map.hash(key);
    Object value = new Object();
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> valueRef = DummyValueReference.create(value);
    entry.setValueReference(valueRef);
    table.set(index, entry);
    segment.count++;

    assertSame(value, map.get(key, loader));
    assertEquals(0, loader.getCount());
    assertEquals(1, segment.count);

    valueRef.clear();
    assertNotSame(value, map.get(key, loader));
    assertEquals(1, loader.getCount());
    assertEquals(1, segment.count);
  }

  @AndroidIncompatible // Perhaps emulator clock does not update between the two get() calls?
  public void testComputeExpiredEntry() throws ExecutionException {
    CacheBuilder<Object, Object> builder =
        createCacheBuilder().expireAfterWrite(1, TimeUnit.NANOSECONDS);
    CountingLoader loader = new CountingLoader();
    LocalCache<Object, Object> map = makeLocalCache(builder);
    assertEquals(0, loader.getCount());

    Object key = new Object();
    Object one = map.get(key, loader);
    assertEquals(1, loader.getCount());

    Object two = map.get(key, loader);
    assertNotSame(one, two);
    assertEquals(2, loader.getCount());
  }

  public void testValues() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder());
    map.put("foo", "bar");
    map.put("baz", "bar");
    map.put("quux", "quux");
    assertFalse(map.values() instanceof Set);
    assertTrue(map.values().removeAll(ImmutableSet.of("bar")));
    assertEquals(1, map.size());
  }

  public void testCopyEntry_computing() {
    final CountDownLatch startSignal = new CountDownLatch(1);
    final CountDownLatch computingSignal = new CountDownLatch(1);
    final CountDownLatch doneSignal = new CountDownLatch(2);
    final Object computedObject = new Object();

    final CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) throws Exception {
            computingSignal.countDown();
            startSignal.await();
            return computedObject;
          }
        };

    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    CacheBuilder<Object, Object> builder =
        createCacheBuilder().concurrencyLevel(1).removalListener(listener);
    final LocalCache<Object, Object> map = makeLocalCache(builder);
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertTrue(listener.isEmpty());

    final Object one = new Object();
    int hash = map.hash(one);
    int index = hash & (table.length() - 1);

    new Thread() {
      @Override
      public void run() {
        try {
          map.get(one, loader);
        } catch (ExecutionException e) {
          throw new RuntimeException(e);
        }
        doneSignal.countDown();
      }
    }.start();

    try {
      computingSignal.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    new Thread() {
      @Override
      public void run() {
        try {
          map.get(one, loader);
        } catch (ExecutionException e) {
          throw new RuntimeException(e);
        }
        doneSignal.countDown();
      }
    }.start();

    ReferenceEntry<Object, Object> entry = segment.getEntry(one, hash);
    ReferenceEntry<Object, Object> newEntry = segment.copyEntry(entry, null);
    table.set(index, newEntry);

    @SuppressWarnings("unchecked")
    LoadingValueReference<Object, Object> valueReference =
        (LoadingValueReference) newEntry.getValueReference();
    assertFalse(valueReference.futureValue.isDone());
    startSignal.countDown();

    try {
      doneSignal.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    map.cleanUp(); // force notifications
    assertTrue(listener.isEmpty());
    assertTrue(map.containsKey(one));
    assertEquals(1, map.size());
    assertSame(computedObject, map.get(one));
  }

  public void testRemovalListenerCheckedException() {
    final RuntimeException e = new RuntimeException();
    RemovalListener<Object, Object> listener =
        new RemovalListener<Object, Object>() {
          @Override
          public void onRemoval(RemovalNotification<Object, Object> notification) {
            throw e;
          }
        };

    CacheBuilder<Object, Object> builder = createCacheBuilder().removalListener(listener);
    final LocalCache<Object, Object> cache = makeLocalCache(builder);
    Object key = new Object();
    cache.put(key, new Object());
    checkNothingLogged();

    cache.remove(key);
    checkLogged(e);
  }

  public void testRemovalListener_replaced_computing() {
    final CountDownLatch startSignal = new CountDownLatch(1);
    final CountDownLatch computingSignal = new CountDownLatch(1);
    final CountDownLatch doneSignal = new CountDownLatch(1);
    final Object computedObject = new Object();

    final CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) throws Exception {
            computingSignal.countDown();
            startSignal.await();
            return computedObject;
          }
        };

    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    CacheBuilder<Object, Object> builder = createCacheBuilder().removalListener(listener);
    final LocalCache<Object, Object> map = makeLocalCache(builder);
    assertTrue(listener.isEmpty());

    final Object one = new Object();
    final Object two = new Object();

    new Thread() {
      @Override
      public void run() {
        try {
          map.get(one, loader);
        } catch (ExecutionException e) {
          throw new RuntimeException(e);
        }
        doneSignal.countDown();
      }
    }.start();

    try {
      computingSignal.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    map.put(one, two);
    assertSame(two, map.get(one));
    startSignal.countDown();

    try {
      doneSignal.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    map.cleanUp(); // force notifications
    assertNotified(listener, one, computedObject, RemovalCause.REPLACED);
    assertTrue(listener.isEmpty());
  }

  public void testSegmentRefresh_duplicate() throws ExecutionException {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().concurrencyLevel(1));
    Segment<Object, Object> segment = map.segments[0];

    Object key = new Object();
    int hash = map.hash(key);
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    int index = hash & (table.length() - 1);

    // already loading
    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> valueRef = DummyValueReference.create(null);
    valueRef.setLoading(true);
    entry.setValueReference(valueRef);
    table.set(index, entry);
    assertNull(segment.refresh(key, hash, identityLoader(), false));
  }

  // Removal listener tests

  public void testRemovalListener_explicit() {
    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().removalListener(listener));
    assertTrue(listener.isEmpty());

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();
    Object four = new Object();
    Object five = new Object();
    Object six = new Object();

    map.put(one, two);
    map.remove(one);
    assertNotified(listener, one, two, RemovalCause.EXPLICIT);

    map.put(two, three);
    map.remove(two, three);
    assertNotified(listener, two, three, RemovalCause.EXPLICIT);

    map.put(three, four);
    Iterator<?> i = map.entrySet().iterator();
    i.next();
    i.remove();
    assertNotified(listener, three, four, RemovalCause.EXPLICIT);

    map.put(four, five);
    i = map.keySet().iterator();
    i.next();
    i.remove();
    assertNotified(listener, four, five, RemovalCause.EXPLICIT);

    map.put(five, six);
    i = map.values().iterator();
    i.next();
    i.remove();
    assertNotified(listener, five, six, RemovalCause.EXPLICIT);

    assertTrue(listener.isEmpty());
  }

  public void testRemovalListener_replaced() {
    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().removalListener(listener));
    assertTrue(listener.isEmpty());

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();
    Object four = new Object();
    Object five = new Object();
    Object six = new Object();

    map.put(one, two);
    map.put(one, three);
    assertNotified(listener, one, two, RemovalCause.REPLACED);

    Map<Object, Object> newMap = ImmutableMap.of(one, four);
    map.putAll(newMap);
    assertNotified(listener, one, three, RemovalCause.REPLACED);

    map.replace(one, five);
    assertNotified(listener, one, four, RemovalCause.REPLACED);

    map.replace(one, five, six);
    assertNotified(listener, one, five, RemovalCause.REPLACED);
  }

  public void testRemovalListener_collected() {
    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder().concurrencyLevel(1).softValues().removalListener(listener));
    Segment<Object, Object> segment = map.segments[0];
    assertTrue(listener.isEmpty());

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();

    map.put(one, two);
    map.put(two, three);
    assertTrue(listener.isEmpty());

    int hash = map.hash(one);
    ReferenceEntry<Object, Object> entry = segment.getEntry(one, hash);
    map.reclaimValue(entry.getValueReference());
    assertNotified(listener, one, two, RemovalCause.COLLECTED);

    assertTrue(listener.isEmpty());
  }

  public void testRemovalListener_expired() {
    FakeTicker ticker = new FakeTicker();
    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .expireAfterWrite(3, TimeUnit.NANOSECONDS)
                .ticker(ticker)
                .removalListener(listener));
    assertTrue(listener.isEmpty());

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();
    Object four = new Object();
    Object five = new Object();

    map.put(one, two);
    ticker.advance(1);
    map.put(two, three);
    ticker.advance(1);
    map.put(three, four);
    assertTrue(listener.isEmpty());
    ticker.advance(1);
    map.put(four, five);
    assertNotified(listener, one, two, RemovalCause.EXPIRED);

    assertTrue(listener.isEmpty());
  }

  public void testRemovalListener_size() {
    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder().concurrencyLevel(1).maximumSize(2).removalListener(listener));
    assertTrue(listener.isEmpty());

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();
    Object four = new Object();

    map.put(one, two);
    map.put(two, three);
    assertTrue(listener.isEmpty());
    map.put(three, four);
    assertNotified(listener, one, two, RemovalCause.SIZE);

    assertTrue(listener.isEmpty());
  }

  static <K, V> void assertNotified(
      QueuingRemovalListener<K, V> listener, K key, V value, RemovalCause cause) {
    RemovalNotification<K, V> notification = listener.remove();
    assertSame(key, notification.getKey());
    assertSame(value, notification.getValue());
    assertSame(cause, notification.getCause());
  }

  // Segment core tests

  public void testNewEntry() {
    for (CacheBuilder<Object, Object> builder : allEntryTypeMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder);

      Object keyOne = new Object();
      Object valueOne = new Object();
      int hashOne = map.hash(keyOne);
      ReferenceEntry<Object, Object> entryOne = map.newEntry(keyOne, hashOne, null);
      ValueReference<Object, Object> valueRefOne = map.newValueReference(entryOne, valueOne, 1);
      assertSame(valueOne, valueRefOne.get());
      entryOne.setValueReference(valueRefOne);

      assertSame(keyOne, entryOne.getKey());
      assertEquals(hashOne, entryOne.getHash());
      assertNull(entryOne.getNext());
      assertSame(valueRefOne, entryOne.getValueReference());

      Object keyTwo = new Object();
      Object valueTwo = new Object();
      int hashTwo = map.hash(keyTwo);
      ReferenceEntry<Object, Object> entryTwo = map.newEntry(keyTwo, hashTwo, entryOne);
      ValueReference<Object, Object> valueRefTwo = map.newValueReference(entryTwo, valueTwo, 1);
      assertSame(valueTwo, valueRefTwo.get());
      entryTwo.setValueReference(valueRefTwo);

      assertSame(keyTwo, entryTwo.getKey());
      assertEquals(hashTwo, entryTwo.getHash());
      assertSame(entryOne, entryTwo.getNext());
      assertSame(valueRefTwo, entryTwo.getValueReference());
    }
  }

  public void testCopyEntry() {
    for (CacheBuilder<Object, Object> builder : allEntryTypeMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder);

      Object keyOne = new Object();
      Object valueOne = new Object();
      int hashOne = map.hash(keyOne);
      ReferenceEntry<Object, Object> entryOne = map.newEntry(keyOne, hashOne, null);
      entryOne.setValueReference(map.newValueReference(entryOne, valueOne, 1));

      Object keyTwo = new Object();
      Object valueTwo = new Object();
      int hashTwo = map.hash(keyTwo);
      ReferenceEntry<Object, Object> entryTwo = map.newEntry(keyTwo, hashTwo, entryOne);
      entryTwo.setValueReference(map.newValueReference(entryTwo, valueTwo, 1));
      if (map.usesAccessQueue()) {
        LocalCache.connectAccessOrder(entryOne, entryTwo);
      }
      if (map.usesWriteQueue()) {
        LocalCache.connectWriteOrder(entryOne, entryTwo);
      }
      assertConnected(map, entryOne, entryTwo);

      ReferenceEntry<Object, Object> copyOne = map.copyEntry(entryOne, null);
      assertSame(keyOne, entryOne.getKey());
      assertEquals(hashOne, entryOne.getHash());
      assertNull(entryOne.getNext());
      assertSame(valueOne, copyOne.getValueReference().get());
      assertConnected(map, copyOne, entryTwo);

      ReferenceEntry<Object, Object> copyTwo = map.copyEntry(entryTwo, copyOne);
      assertSame(keyTwo, copyTwo.getKey());
      assertEquals(hashTwo, copyTwo.getHash());
      assertSame(copyOne, copyTwo.getNext());
      assertSame(valueTwo, copyTwo.getValueReference().get());
      assertConnected(map, copyOne, copyTwo);
    }
  }

  private static <K, V> void assertConnected(
      LocalCache<K, V> map, ReferenceEntry<K, V> one, ReferenceEntry<K, V> two) {
    if (map.usesWriteQueue()) {
      assertSame(two, one.getNextInWriteQueue());
    }
    if (map.usesAccessQueue()) {
      assertSame(two, one.getNextInAccessQueue());
    }
  }

  public void testSegmentGetAndContains() {
    FakeTicker ticker = new FakeTicker();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .ticker(ticker)
                .expireAfterAccess(1, TimeUnit.NANOSECONDS));
    Segment<Object, Object> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object value = new Object();
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    int index = hash & (table.length() - 1);

    ReferenceEntry<Object, Object> entry = map.newEntry(key, hash, null);
    ValueReference<Object, Object> valueRef = map.newValueReference(entry, value, 1);
    entry.setValueReference(valueRef);

    assertNull(segment.get(key, hash));

    // count == 0
    table.set(index, entry);
    assertNull(segment.get(key, hash));
    assertFalse(segment.containsKey(key, hash));
    assertFalse(segment.containsValue(value));

    // count == 1
    segment.count++;
    assertSame(value, segment.get(key, hash));
    assertTrue(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    // don't see absent values now that count > 0
    assertNull(segment.get(new Object(), hash));

    // null key
    DummyEntry<Object, Object> nullEntry = DummyEntry.create(null, hash, entry);
    Object nullValue = new Object();
    ValueReference<Object, Object> nullValueRef = map.newValueReference(nullEntry, nullValue, 1);
    nullEntry.setValueReference(nullValueRef);
    table.set(index, nullEntry);
    // skip the null key
    assertSame(value, segment.get(key, hash));
    assertTrue(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    assertFalse(segment.containsValue(nullValue));

    // hash collision
    DummyEntry<Object, Object> dummy = DummyEntry.create(new Object(), hash, entry);
    Object dummyValue = new Object();
    ValueReference<Object, Object> dummyValueRef = map.newValueReference(dummy, dummyValue, 1);
    dummy.setValueReference(dummyValueRef);
    table.set(index, dummy);
    assertSame(value, segment.get(key, hash));
    assertTrue(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    assertTrue(segment.containsValue(dummyValue));

    // key collision
    dummy = DummyEntry.create(key, hash, entry);
    dummyValue = new Object();
    dummyValueRef = map.newValueReference(dummy, dummyValue, 1);
    dummy.setValueReference(dummyValueRef);
    table.set(index, dummy);
    // returns the most recent entry
    assertSame(dummyValue, segment.get(key, hash));
    assertTrue(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    assertTrue(segment.containsValue(dummyValue));

    // expired
    dummy.setAccessTime(ticker.read() - 2);
    assertNull(segment.get(key, hash));
    assertFalse(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    assertFalse(segment.containsValue(dummyValue));
  }

  public void testSegmentReplaceValue() {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).expireAfterAccess(99999, SECONDS));
    Segment<Object, Object> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue);
    entry.setValueReference(oldValueRef);

    // no entry
    assertFalse(segment.replace(key, hash, oldValue, newValue));
    assertEquals(0, segment.count);

    // same value
    table.set(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertTrue(segment.replace(key, hash, oldValue, newValue));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));

    // different value
    assertFalse(segment.replace(key, hash, oldValue, newValue));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));

    // cleared
    entry.setValueReference(oldValueRef);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear();
    assertFalse(segment.replace(key, hash, oldValue, newValue));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
  }

  public void testSegmentReplace() {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).expireAfterAccess(99999, SECONDS));
    Segment<Object, Object> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue);
    entry.setValueReference(oldValueRef);

    // no entry
    assertNull(segment.replace(key, hash, newValue));
    assertEquals(0, segment.count);

    // same key
    table.set(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertSame(oldValue, segment.replace(key, hash, newValue));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));

    // cleared
    entry.setValueReference(oldValueRef);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear();
    assertNull(segment.replace(key, hash, newValue));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
  }

  public void testSegmentPut() {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).expireAfterAccess(99999, SECONDS));
    Segment<Object, Object> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();

    // no entry
    assertEquals(0, segment.count);
    assertNull(segment.put(key, hash, oldValue, false));
    assertEquals(1, segment.count);

    // same key
    assertSame(oldValue, segment.put(key, hash, newValue, false));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));

    // cleared
    ReferenceEntry<Object, Object> entry = segment.getEntry(key, hash);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue);
    entry.setValueReference(oldValueRef);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear();
    assertNull(segment.put(key, hash, newValue, false));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));
  }

  public void testSegmentPutIfAbsent() {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).expireAfterAccess(99999, SECONDS));
    Segment<Object, Object> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();

    // no entry
    assertEquals(0, segment.count);
    assertNull(segment.put(key, hash, oldValue, true));
    assertEquals(1, segment.count);

    // same key
    assertSame(oldValue, segment.put(key, hash, newValue, true));
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));

    // cleared
    ReferenceEntry<Object, Object> entry = segment.getEntry(key, hash);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue);
    entry.setValueReference(oldValueRef);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear();
    assertNull(segment.put(key, hash, newValue, true));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));
  }

  public void testSegmentPut_expand() {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object> segment = map.segments[0];
    assertEquals(1, segment.table.length());

    int count = 1024;
    for (int i = 0; i < count; i++) {
      Object key = new Object();
      Object value = new Object();
      int hash = map.hash(key);
      assertNull(segment.put(key, hash, value, false));
      assertTrue(segment.table.length() > i);
    }
  }

  public void testSegmentPut_evict() {
    int maxSize = 10;
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).maximumSize(maxSize));

    // manually add elements to avoid eviction
    int originalCount = 1024;
    LinkedHashMap<Object, Object> originalMap = Maps.newLinkedHashMap();
    for (int i = 0; i < originalCount; i++) {
      Object key = new Object();
      Object value = new Object();
      map.put(key, value);
      originalMap.put(key, value);
      if (i >= maxSize) {
        Iterator<Object> it = originalMap.keySet().iterator();
        it.next();
        it.remove();
      }
      assertEquals(originalMap, map);
    }
  }

  public void testSegmentStoreComputedValue() {
    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).removalListener(listener));
    Segment<Object, Object> segment = map.segments[0];

    Object key = new Object();
    int hash = map.hash(key);
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    LoadingValueReference<Object, Object> valueRef = new LoadingValueReference<>();
    entry.setValueReference(valueRef);

    // absent
    Object value = new Object();
    assertTrue(listener.isEmpty());
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
    assertTrue(segment.storeLoadedValue(key, hash, valueRef, value));
    assertSame(value, segment.get(key, hash));
    assertEquals(1, segment.count);
    assertTrue(listener.isEmpty());

    // clobbered
    Object value2 = new Object();
    assertFalse(segment.storeLoadedValue(key, hash, valueRef, value2));
    assertEquals(1, segment.count);
    assertSame(value, segment.get(key, hash));
    RemovalNotification<Object, Object> notification = listener.remove();
    assertEquals(immutableEntry(key, value2), notification);
    assertEquals(RemovalCause.REPLACED, notification.getCause());
    assertTrue(listener.isEmpty());

    // inactive
    Object value3 = new Object();
    map.clear();
    listener.clear();
    assertEquals(0, segment.count);
    table.set(index, entry);
    assertTrue(segment.storeLoadedValue(key, hash, valueRef, value3));
    assertSame(value3, segment.get(key, hash));
    assertEquals(1, segment.count);
    assertTrue(listener.isEmpty());

    // replaced
    Object value4 = new Object();
    DummyValueReference<Object, Object> value3Ref = DummyValueReference.create(value3);
    valueRef = new LoadingValueReference<>(value3Ref);
    entry.setValueReference(valueRef);
    table.set(index, entry);
    assertSame(value3, segment.get(key, hash));
    assertEquals(1, segment.count);
    assertTrue(segment.storeLoadedValue(key, hash, valueRef, value4));
    assertSame(value4, segment.get(key, hash));
    assertEquals(1, segment.count);
    notification = listener.remove();
    assertEquals(immutableEntry(key, value3), notification);
    assertEquals(RemovalCause.REPLACED, notification.getCause());
    assertTrue(listener.isEmpty());

    // collected
    entry.setValueReference(valueRef);
    table.set(index, entry);
    assertSame(value3, segment.get(key, hash));
    assertEquals(1, segment.count);
    value3Ref.clear();
    assertTrue(segment.storeLoadedValue(key, hash, valueRef, value4));
    assertSame(value4, segment.get(key, hash));
    assertEquals(1, segment.count);
    notification = listener.remove();
    assertEquals(immutableEntry(key, null), notification);
    assertEquals(RemovalCause.COLLECTED, notification.getCause());
    assertTrue(listener.isEmpty());
  }

  public void testSegmentRemove() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().concurrencyLevel(1));
    Segment<Object, Object> segment = map.segments[0];

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue);
    entry.setValueReference(oldValueRef);

    // no entry
    assertEquals(0, segment.count);
    assertNull(segment.remove(key, hash));
    assertEquals(0, segment.count);

    // same key
    table.set(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertSame(oldValue, segment.remove(key, hash));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));

    // cleared
    table.set(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear();
    assertNull(segment.remove(key, hash));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
  }

  public void testSegmentRemoveValue() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().concurrencyLevel(1));
    Segment<Object, Object> segment = map.segments[0];

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue);
    entry.setValueReference(oldValueRef);

    // no entry
    assertEquals(0, segment.count);
    assertNull(segment.remove(key, hash));
    assertEquals(0, segment.count);

    // same value
    table.set(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertTrue(segment.remove(key, hash, oldValue));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));

    // different value
    table.set(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertFalse(segment.remove(key, hash, newValue));
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));

    // cleared
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear();
    assertFalse(segment.remove(key, hash, oldValue));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
  }

  public void testExpand() {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object> segment = map.segments[0];
    assertEquals(1, segment.table.length());

    // manually add elements to avoid expansion
    int originalCount = 1024;
    ReferenceEntry<Object, Object> entry = null;
    for (int i = 0; i < originalCount; i++) {
      Object key = new Object();
      Object value = new Object();
      int hash = map.hash(key);
      // chain all entries together as we only have a single bucket
      entry = map.newEntry(key, hash, entry);
      ValueReference<Object, Object> valueRef = map.newValueReference(entry, value, 1);
      entry.setValueReference(valueRef);
    }
    segment.table.set(0, entry);
    segment.count = originalCount;
    ImmutableMap<Object, Object> originalMap = ImmutableMap.copyOf(map);
    assertEquals(originalCount, originalMap.size());
    assertEquals(originalMap, map);

    for (int i = 1; i <= originalCount * 2; i *= 2) {
      if (i > 1) {
        segment.expand();
      }
      assertEquals(i, segment.table.length());
      assertEquals(originalCount, countLiveEntries(map, 0));
      assertEquals(originalCount, segment.count);
      assertEquals(originalMap, map);
    }
  }

  public void testGetCausesExpansion() throws ExecutionException {
    for (int count = 1; count <= 100; count++) {
      LocalCache<Object, Object> map =
          makeLocalCache(createCacheBuilder().concurrencyLevel(1).initialCapacity(1));
      Segment<Object, Object> segment = map.segments[0];
      assertEquals(1, segment.table.length());

      for (int i = 0; i < count; i++) {
        Object key = new Object();
        final Object value = new Object();
        segment.get(
            key,
            key.hashCode(),
            new CacheLoader<Object, Object>() {
              @Override
              public Object load(Object key) {
                return value;
              }
            });
      }
      assertEquals(count, segment.count);
      assertTrue(count <= segment.threshold);
      assertTrue(count <= (segment.table.length() * 3 / 4));
      assertTrue(count > (segment.table.length() * 3 / 8));
    }
  }

  public void testGetOrDefault() {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).initialCapacity(1));
    map.put(1, 1);
    assertEquals(1, map.getOrDefault(1, 2));
    assertEquals(2, map.getOrDefault(2, 2));
  }

  public void testPutCausesExpansion() {
    for (int count = 1; count <= 100; count++) {
      LocalCache<Object, Object> map =
          makeLocalCache(createCacheBuilder().concurrencyLevel(1).initialCapacity(1));
      Segment<Object, Object> segment = map.segments[0];
      assertEquals(1, segment.table.length());

      for (int i = 0; i < count; i++) {
        Object key = new Object();
        Object value = new Object();
        segment.put(key, key.hashCode(), value, true);
      }
      assertEquals(count, segment.count);
      assertTrue(count <= segment.threshold);
      assertTrue(count <= (segment.table.length() * 3 / 4));
      assertTrue(count > (segment.table.length() * 3 / 8));
    }
  }

  public void testReclaimKey() {
    CountingRemovalListener<Object, Object> listener = countingRemovalListener();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .initialCapacity(1)
                .maximumSize(SMALL_MAX_SIZE)
                .expireAfterWrite(99999, SECONDS)
                .removalListener(listener));
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(1, table.length());

    // create 3 objects and chain them together
    Object keyOne = new Object();
    Object valueOne = new Object();
    int hashOne = map.hash(keyOne);
    DummyEntry<Object, Object> entryOne = createDummyEntry(keyOne, hashOne, valueOne, null);
    Object keyTwo = new Object();
    Object valueTwo = new Object();
    int hashTwo = map.hash(keyTwo);
    DummyEntry<Object, Object> entryTwo = createDummyEntry(keyTwo, hashTwo, valueTwo, entryOne);
    Object keyThree = new Object();
    Object valueThree = new Object();
    int hashThree = map.hash(keyThree);
    DummyEntry<Object, Object> entryThree =
        createDummyEntry(keyThree, hashThree, valueThree, entryTwo);

    // absent
    assertEquals(0, listener.getCount());
    assertFalse(segment.reclaimKey(entryOne, hashOne));
    assertEquals(0, listener.getCount());
    table.set(0, entryOne);
    assertFalse(segment.reclaimKey(entryTwo, hashTwo));
    assertEquals(0, listener.getCount());
    table.set(0, entryTwo);
    assertFalse(segment.reclaimKey(entryThree, hashThree));
    assertEquals(0, listener.getCount());

    // present
    table.set(0, entryOne);
    segment.count = 1;
    assertTrue(segment.reclaimKey(entryOne, hashOne));
    assertEquals(1, listener.getCount());
    assertSame(keyOne, listener.getLastEvictedKey());
    assertSame(valueOne, listener.getLastEvictedValue());
    assertTrue(map.removalNotificationQueue.isEmpty());
    assertFalse(segment.accessQueue.contains(entryOne));
    assertFalse(segment.writeQueue.contains(entryOne));
    assertEquals(0, segment.count);
    assertNull(table.get(0));
  }

  public void testRemoveEntryFromChain() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().concurrencyLevel(1));
    Segment<Object, Object> segment = map.segments[0];

    // create 3 objects and chain them together
    Object keyOne = new Object();
    Object valueOne = new Object();
    int hashOne = map.hash(keyOne);
    DummyEntry<Object, Object> entryOne = createDummyEntry(keyOne, hashOne, valueOne, null);
    Object keyTwo = new Object();
    Object valueTwo = new Object();
    int hashTwo = map.hash(keyTwo);
    DummyEntry<Object, Object> entryTwo = createDummyEntry(keyTwo, hashTwo, valueTwo, entryOne);
    Object keyThree = new Object();
    Object valueThree = new Object();
    int hashThree = map.hash(keyThree);
    DummyEntry<Object, Object> entryThree =
        createDummyEntry(keyThree, hashThree, valueThree, entryTwo);

    // alone
    assertNull(segment.removeEntryFromChain(entryOne, entryOne));

    // head
    assertSame(entryOne, segment.removeEntryFromChain(entryTwo, entryTwo));

    // middle
    ReferenceEntry<Object, Object> newFirst = segment.removeEntryFromChain(entryThree, entryTwo);
    assertSame(keyThree, newFirst.getKey());
    assertSame(valueThree, newFirst.getValueReference().get());
    assertEquals(hashThree, newFirst.getHash());
    assertSame(entryOne, newFirst.getNext());

    // tail (remaining entries are copied in reverse order)
    newFirst = segment.removeEntryFromChain(entryThree, entryOne);
    assertSame(keyTwo, newFirst.getKey());
    assertSame(valueTwo, newFirst.getValueReference().get());
    assertEquals(hashTwo, newFirst.getHash());
    newFirst = newFirst.getNext();
    assertSame(keyThree, newFirst.getKey());
    assertSame(valueThree, newFirst.getValueReference().get());
    assertEquals(hashThree, newFirst.getHash());
    assertNull(newFirst.getNext());
  }

  public void testExpand_cleanup() {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object> segment = map.segments[0];
    assertEquals(1, segment.table.length());

    // manually add elements to avoid expansion
    // 1/3 null keys, 1/3 null values
    int originalCount = 1024;
    ReferenceEntry<Object, Object> entry = null;
    for (int i = 0; i < originalCount; i++) {
      Object key = new Object();
      Object value = (i % 3 == 0) ? null : new Object();
      int hash = map.hash(key);
      if (i % 3 == 1) {
        key = null;
      }
      // chain all entries together as we only have a single bucket
      entry = DummyEntry.create(key, hash, entry);
      ValueReference<Object, Object> valueRef = DummyValueReference.create(value);
      entry.setValueReference(valueRef);
    }
    segment.table.set(0, entry);
    segment.count = originalCount;
    int liveCount = originalCount / 3;
    assertEquals(1, segment.table.length());
    assertEquals(liveCount, countLiveEntries(map, 0));
    ImmutableMap<Object, Object> originalMap = ImmutableMap.copyOf(map);
    assertEquals(liveCount, originalMap.size());
    // can't compare map contents until cleanup occurs

    for (int i = 1; i <= originalCount * 2; i *= 2) {
      if (i > 1) {
        segment.expand();
      }
      assertEquals(i, segment.table.length());
      assertEquals(liveCount, countLiveEntries(map, 0));
      // expansion cleanup is sloppy, with a goal of avoiding unnecessary copies
      assertTrue(segment.count >= liveCount);
      assertTrue(segment.count <= originalCount);
      assertEquals(originalMap, ImmutableMap.copyOf(map));
    }
  }

  private static <K, V> int countLiveEntries(LocalCache<K, V> map, long now) {
    int result = 0;
    for (Segment<K, V> segment : map.segments) {
      AtomicReferenceArray<ReferenceEntry<K, V>> table = segment.table;
      for (int i = 0; i < table.length(); i++) {
        for (ReferenceEntry<K, V> e = table.get(i); e != null; e = e.getNext()) {
          if (map.isLive(e, now)) {
            result++;
          }
        }
      }
    }
    return result;
  }

  public void testClear() {
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .initialCapacity(1)
                .maximumSize(SMALL_MAX_SIZE)
                .expireAfterWrite(99999, SECONDS));
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(1, table.length());

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = createDummyEntry(key, hash, value, null);
    segment.recordWrite(entry, 1, map.ticker.read());
    segment.table.set(0, entry);
    segment.readCount.incrementAndGet();
    segment.count = 1;
    segment.totalWeight = 1;

    assertSame(entry, table.get(0));
    assertSame(entry, segment.accessQueue.peek());
    assertSame(entry, segment.writeQueue.peek());

    segment.clear();
    assertNull(table.get(0));
    assertTrue(segment.accessQueue.isEmpty());
    assertTrue(segment.writeQueue.isEmpty());
    assertEquals(0, segment.readCount.get());
    assertEquals(0, segment.count);
    assertEquals(0, segment.totalWeight);
  }

  public void testClear_notification() {
    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .initialCapacity(1)
                .maximumSize(SMALL_MAX_SIZE)
                .expireAfterWrite(99999, SECONDS)
                .removalListener(listener));
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(1, table.length());

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = createDummyEntry(key, hash, value, null);
    segment.recordWrite(entry, 1, map.ticker.read());
    segment.table.set(0, entry);
    segment.readCount.incrementAndGet();
    segment.count = 1;
    segment.totalWeight = 1;

    assertSame(entry, table.get(0));
    assertSame(entry, segment.accessQueue.peek());
    assertSame(entry, segment.writeQueue.peek());

    segment.clear();
    assertNull(table.get(0));
    assertTrue(segment.accessQueue.isEmpty());
    assertTrue(segment.writeQueue.isEmpty());
    assertEquals(0, segment.readCount.get());
    assertEquals(0, segment.count);
    assertEquals(0, segment.totalWeight);
    assertNotified(listener, key, value, RemovalCause.EXPLICIT);
  }

  public void testRemoveEntry() {
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .initialCapacity(1)
                .maximumSize(SMALL_MAX_SIZE)
                .expireAfterWrite(99999, SECONDS)
                .removalListener(countingRemovalListener()));
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(1, table.length());

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = createDummyEntry(key, hash, value, null);

    // remove absent
    assertFalse(segment.removeEntry(entry, hash, RemovalCause.COLLECTED));

    // remove live
    segment.recordWrite(entry, 1, map.ticker.read());
    table.set(0, entry);
    segment.count = 1;
    assertTrue(segment.removeEntry(entry, hash, RemovalCause.COLLECTED));
    assertNotificationEnqueued(map, key, value, hash);
    assertTrue(map.removalNotificationQueue.isEmpty());
    assertFalse(segment.accessQueue.contains(entry));
    assertFalse(segment.writeQueue.contains(entry));
    assertEquals(0, segment.count);
    assertNull(table.get(0));
  }

  public void testReclaimValue() {
    CountingRemovalListener<Object, Object> listener = countingRemovalListener();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .initialCapacity(1)
                .maximumSize(SMALL_MAX_SIZE)
                .expireAfterWrite(99999, SECONDS)
                .removalListener(listener));
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(1, table.length());

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> valueRef = DummyValueReference.create(value);
    entry.setValueReference(valueRef);

    // reclaim absent
    assertFalse(segment.reclaimValue(key, hash, valueRef));

    // reclaim live
    segment.recordWrite(entry, 1, map.ticker.read());
    table.set(0, entry);
    segment.count = 1;
    assertTrue(segment.reclaimValue(key, hash, valueRef));
    assertEquals(1, listener.getCount());
    assertSame(key, listener.getLastEvictedKey());
    assertSame(value, listener.getLastEvictedValue());
    assertTrue(map.removalNotificationQueue.isEmpty());
    assertFalse(segment.accessQueue.contains(entry));
    assertFalse(segment.writeQueue.contains(entry));
    assertEquals(0, segment.count);
    assertNull(table.get(0));

    // reclaim wrong value reference
    table.set(0, entry);
    DummyValueReference<Object, Object> otherValueRef = DummyValueReference.create(value);
    entry.setValueReference(otherValueRef);
    assertFalse(segment.reclaimValue(key, hash, valueRef));
    assertEquals(1, listener.getCount());
    assertTrue(segment.reclaimValue(key, hash, otherValueRef));
    assertEquals(2, listener.getCount());
    assertSame(key, listener.getLastEvictedKey());
    assertSame(value, listener.getLastEvictedValue());
  }

  public void testRemoveComputingValue() {
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .initialCapacity(1)
                .maximumSize(SMALL_MAX_SIZE)
                .expireAfterWrite(99999, SECONDS)
                .removalListener(countingRemovalListener()));
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(1, table.length());

    Object key = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    LoadingValueReference<Object, Object> valueRef = new LoadingValueReference<>();
    entry.setValueReference(valueRef);

    // absent
    assertFalse(segment.removeLoadingValue(key, hash, valueRef));

    // live
    table.set(0, entry);
    // don't increment count; this is used during computation
    assertTrue(segment.removeLoadingValue(key, hash, valueRef));
    // no notification sent with removeLoadingValue
    assertTrue(map.removalNotificationQueue.isEmpty());
    assertEquals(0, segment.count);
    assertNull(table.get(0));

    // active
    Object value = new Object();
    DummyValueReference<Object, Object> previousRef = DummyValueReference.create(value);
    valueRef = new LoadingValueReference<>(previousRef);
    entry.setValueReference(valueRef);
    table.set(0, entry);
    segment.count = 1;
    assertTrue(segment.removeLoadingValue(key, hash, valueRef));
    assertSame(entry, table.get(0));
    assertSame(value, segment.get(key, hash));

    // wrong value reference
    table.set(0, entry);
    DummyValueReference<Object, Object> otherValueRef = DummyValueReference.create(value);
    entry.setValueReference(otherValueRef);
    assertFalse(segment.removeLoadingValue(key, hash, valueRef));
    entry.setValueReference(valueRef);
    assertTrue(segment.removeLoadingValue(key, hash, valueRef));
  }

  private static <K, V> void assertNotificationEnqueued(
      LocalCache<K, V> map, K key, V value, int hash) {
    RemovalNotification<K, V> notification = map.removalNotificationQueue.poll();
    assertSame(key, notification.getKey());
    assertSame(value, notification.getValue());
  }

  // Segment eviction tests

  public void testDrainRecencyQueueOnWrite() {
    for (CacheBuilder<Object, Object> builder : allEvictingMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];

      if (segment.recencyQueue != DISCARDING_QUEUE) {
        Object keyOne = new Object();
        Object valueOne = new Object();
        Object keyTwo = new Object();
        Object valueTwo = new Object();

        map.put(keyOne, valueOne);
        assertTrue(segment.recencyQueue.isEmpty());

        for (int i = 0; i < DRAIN_THRESHOLD / 2; i++) {
          map.get(keyOne);
        }
        assertFalse(segment.recencyQueue.isEmpty());

        map.put(keyTwo, valueTwo);
        assertTrue(segment.recencyQueue.isEmpty());
      }
    }
  }

  public void testDrainRecencyQueueOnRead() {
    for (CacheBuilder<Object, Object> builder : allEvictingMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];

      if (segment.recencyQueue != DISCARDING_QUEUE) {
        Object keyOne = new Object();
        Object valueOne = new Object();

        // repeated get of the same key

        map.put(keyOne, valueOne);
        assertTrue(segment.recencyQueue.isEmpty());

        for (int i = 0; i < DRAIN_THRESHOLD / 2; i++) {
          map.get(keyOne);
        }
        assertFalse(segment.recencyQueue.isEmpty());

        for (int i = 0; i < DRAIN_THRESHOLD * 2; i++) {
          map.get(keyOne);
          assertTrue(segment.recencyQueue.size() <= DRAIN_THRESHOLD);
        }

        // get over many different keys

        for (int i = 0; i < DRAIN_THRESHOLD * 2; i++) {
          map.put(new Object(), new Object());
        }
        assertTrue(segment.recencyQueue.isEmpty());

        for (int i = 0; i < DRAIN_THRESHOLD / 2; i++) {
          map.get(keyOne);
        }
        assertFalse(segment.recencyQueue.isEmpty());

        for (Object key : map.keySet()) {
          map.get(key);
          assertTrue(segment.recencyQueue.size() <= DRAIN_THRESHOLD);
        }
      }
    }
  }

  public void testRecordRead() {
    for (CacheBuilder<Object, Object> builder : allEvictingMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];
      List<ReferenceEntry<Object, Object>> writeOrder = Lists.newLinkedList();
      List<ReferenceEntry<Object, Object>> readOrder = Lists.newLinkedList();
      for (int i = 0; i < DRAIN_THRESHOLD * 2; i++) {
        Object key = new Object();
        int hash = map.hash(key);
        Object value = new Object();

        ReferenceEntry<Object, Object> entry = createDummyEntry(key, hash, value, null);
        // must recordRead for drainRecencyQueue to believe this entry is live
        segment.recordWrite(entry, 1, map.ticker.read());
        writeOrder.add(entry);
        readOrder.add(entry);
      }

      checkEvictionQueues(map, segment, readOrder, writeOrder);
      checkExpirationTimes(map);

      // access some of the elements
      Random random = new Random();
      List<ReferenceEntry<Object, Object>> reads = Lists.newArrayList();
      Iterator<ReferenceEntry<Object, Object>> i = readOrder.iterator();
      while (i.hasNext()) {
        ReferenceEntry<Object, Object> entry = i.next();
        if (random.nextBoolean()) {
          segment.recordRead(entry, map.ticker.read());
          reads.add(entry);
          i.remove();
        }
      }
      checkAndDrainRecencyQueue(map, segment, reads);
      readOrder.addAll(reads);

      checkEvictionQueues(map, segment, readOrder, writeOrder);
      checkExpirationTimes(map);
    }
  }

  public void testRecordReadOnGet() {
    for (CacheBuilder<Object, Object> builder : allEvictingMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];
      List<ReferenceEntry<Object, Object>> writeOrder = Lists.newLinkedList();
      List<ReferenceEntry<Object, Object>> readOrder = Lists.newLinkedList();
      for (int i = 0; i < DRAIN_THRESHOLD * 2; i++) {
        Object key = new Object();
        int hash = map.hash(key);
        Object value = new Object();

        map.put(key, value);
        ReferenceEntry<Object, Object> entry = segment.getEntry(key, hash);
        writeOrder.add(entry);
        readOrder.add(entry);
      }

      checkEvictionQueues(map, segment, readOrder, writeOrder);
      checkExpirationTimes(map);
      assertTrue(segment.recencyQueue.isEmpty());

      // access some of the elements
      Random random = new Random();
      List<ReferenceEntry<Object, Object>> reads = Lists.newArrayList();
      Iterator<ReferenceEntry<Object, Object>> i = readOrder.iterator();
      while (i.hasNext()) {
        ReferenceEntry<Object, Object> entry = i.next();
        if (random.nextBoolean()) {
          map.get(entry.getKey());
          reads.add(entry);
          i.remove();
          assertTrue(segment.recencyQueue.size() <= DRAIN_THRESHOLD);
        }
      }
      int undrainedIndex = reads.size() - segment.recencyQueue.size();
      checkAndDrainRecencyQueue(map, segment, reads.subList(undrainedIndex, reads.size()));
      readOrder.addAll(reads);

      checkEvictionQueues(map, segment, readOrder, writeOrder);
      checkExpirationTimes(map);
    }
  }

  public void testRecordWrite() {
    for (CacheBuilder<Object, Object> builder : allEvictingMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];
      List<ReferenceEntry<Object, Object>> writeOrder = Lists.newLinkedList();
      for (int i = 0; i < DRAIN_THRESHOLD * 2; i++) {
        Object key = new Object();
        int hash = map.hash(key);
        Object value = new Object();

        ReferenceEntry<Object, Object> entry = createDummyEntry(key, hash, value, null);
        // must recordRead for drainRecencyQueue to believe this entry is live
        segment.recordWrite(entry, 1, map.ticker.read());
        writeOrder.add(entry);
      }

      checkEvictionQueues(map, segment, writeOrder, writeOrder);
      checkExpirationTimes(map);

      // access some of the elements
      Random random = new Random();
      List<ReferenceEntry<Object, Object>> writes = Lists.newArrayList();
      Iterator<ReferenceEntry<Object, Object>> i = writeOrder.iterator();
      while (i.hasNext()) {
        ReferenceEntry<Object, Object> entry = i.next();
        if (random.nextBoolean()) {
          segment.recordWrite(entry, 1, map.ticker.read());
          writes.add(entry);
          i.remove();
        }
      }
      writeOrder.addAll(writes);

      checkEvictionQueues(map, segment, writeOrder, writeOrder);
      checkExpirationTimes(map);
    }
  }

  static <K, V> void checkAndDrainRecencyQueue(
      LocalCache<K, V> map, Segment<K, V> segment, List<ReferenceEntry<K, V>> reads) {
    if (map.evictsBySize() || map.expiresAfterAccess()) {
      assertSameEntries(reads, ImmutableList.copyOf(segment.recencyQueue));
    }
    segment.drainRecencyQueue();
  }

  static <K, V> void checkEvictionQueues(
      LocalCache<K, V> map,
      Segment<K, V> segment,
      List<ReferenceEntry<K, V>> readOrder,
      List<ReferenceEntry<K, V>> writeOrder) {
    if (map.evictsBySize() || map.expiresAfterAccess()) {
      assertSameEntries(readOrder, ImmutableList.copyOf(segment.accessQueue));
    }
    if (map.expiresAfterWrite()) {
      assertSameEntries(writeOrder, ImmutableList.copyOf(segment.writeQueue));
    }
  }

  private static <K, V> void assertSameEntries(
      List<ReferenceEntry<K, V>> expectedEntries, List<ReferenceEntry<K, V>> actualEntries) {
    int size = expectedEntries.size();
    assertEquals(size, actualEntries.size());
    for (int i = 0; i < size; i++) {
      ReferenceEntry<K, V> expectedEntry = expectedEntries.get(i);
      ReferenceEntry<K, V> actualEntry = actualEntries.get(i);
      assertSame(expectedEntry.getKey(), actualEntry.getKey());
      assertSame(expectedEntry.getValueReference().get(), actualEntry.getValueReference().get());
    }
  }

  static <K, V> void checkExpirationTimes(LocalCache<K, V> map) {
    if (!map.expires()) {
      return;
    }

    for (Segment<K, V> segment : map.segments) {
      long lastAccessTime = 0;
      long lastWriteTime = 0;
      for (ReferenceEntry<K, V> e : segment.recencyQueue) {
        long accessTime = e.getAccessTime();
        assertTrue(accessTime >= lastAccessTime);
        lastAccessTime = accessTime;
        long writeTime = e.getWriteTime();
        assertTrue(writeTime >= lastWriteTime);
        lastWriteTime = writeTime;
      }

      lastAccessTime = 0;
      lastWriteTime = 0;
      for (ReferenceEntry<K, V> e : segment.accessQueue) {
        long accessTime = e.getAccessTime();
        assertTrue(accessTime >= lastAccessTime);
        lastAccessTime = accessTime;
      }
      for (ReferenceEntry<K, V> e : segment.writeQueue) {
        long writeTime = e.getWriteTime();
        assertTrue(writeTime >= lastWriteTime);
        lastWriteTime = writeTime;
      }
    }
  }

  public void testExpireAfterWrite() {
    FakeTicker ticker = new FakeTicker();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .ticker(ticker)
                .expireAfterWrite(2, TimeUnit.NANOSECONDS));
    Segment<Object, Object> segment = map.segments[0];

    Object key = new Object();
    Object value = new Object();
    map.put(key, value);
    ReferenceEntry<Object, Object> entry = map.getEntry(key);
    assertTrue(map.isLive(entry, ticker.read()));

    segment.writeQueue.add(entry);
    assertSame(value, map.get(key));
    assertSame(entry, segment.writeQueue.peek());
    assertEquals(1, segment.writeQueue.size());

    segment.recordRead(entry, ticker.read());
    segment.expireEntries(ticker.read());
    assertSame(value, map.get(key));
    assertSame(entry, segment.writeQueue.peek());
    assertEquals(1, segment.writeQueue.size());

    ticker.advance(1);
    segment.recordRead(entry, ticker.read());
    segment.expireEntries(ticker.read());
    assertSame(value, map.get(key));
    assertSame(entry, segment.writeQueue.peek());
    assertEquals(1, segment.writeQueue.size());

    ticker.advance(1);
    assertNull(map.get(key));
    segment.expireEntries(ticker.read());
    assertNull(map.get(key));
    assertTrue(segment.writeQueue.isEmpty());
  }

  public void testExpireAfterAccess() {
    FakeTicker ticker = new FakeTicker();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .ticker(ticker)
                .expireAfterAccess(2, TimeUnit.NANOSECONDS));
    Segment<Object, Object> segment = map.segments[0];

    Object key = new Object();
    Object value = new Object();
    map.put(key, value);
    ReferenceEntry<Object, Object> entry = map.getEntry(key);
    assertTrue(map.isLive(entry, ticker.read()));

    segment.accessQueue.add(entry);
    assertSame(value, map.get(key));
    assertSame(entry, segment.accessQueue.peek());
    assertEquals(1, segment.accessQueue.size());

    segment.recordRead(entry, ticker.read());
    segment.expireEntries(ticker.read());
    assertTrue(map.containsKey(key));
    assertSame(entry, segment.accessQueue.peek());
    assertEquals(1, segment.accessQueue.size());

    ticker.advance(1);
    segment.recordRead(entry, ticker.read());
    segment.expireEntries(ticker.read());
    assertTrue(map.containsKey(key));
    assertSame(entry, segment.accessQueue.peek());
    assertEquals(1, segment.accessQueue.size());

    ticker.advance(1);
    segment.recordRead(entry, ticker.read());
    segment.expireEntries(ticker.read());
    assertTrue(map.containsKey(key));
    assertSame(entry, segment.accessQueue.peek());
    assertEquals(1, segment.accessQueue.size());

    ticker.advance(1);
    segment.expireEntries(ticker.read());
    assertTrue(map.containsKey(key));
    assertSame(entry, segment.accessQueue.peek());
    assertEquals(1, segment.accessQueue.size());

    ticker.advance(1);
    assertFalse(map.containsKey(key));
    assertNull(map.get(key));
    segment.expireEntries(ticker.read());
    assertFalse(map.containsKey(key));
    assertNull(map.get(key));
    assertTrue(segment.accessQueue.isEmpty());
  }

  public void testEvictEntries() {
    int maxSize = 10;
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).maximumSize(maxSize));
    Segment<Object, Object> segment = map.segments[0];

    // manually add elements to avoid eviction
    int originalCount = 1024;
    ReferenceEntry<Object, Object> entry = null;
    LinkedHashMap<Object, Object> originalMap = Maps.newLinkedHashMap();
    for (int i = 0; i < originalCount; i++) {
      Object key = new Object();
      Object value = new Object();
      AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
      int hash = map.hash(key);
      int index = hash & (table.length() - 1);
      ReferenceEntry<Object, Object> first = table.get(index);
      entry = map.newEntry(key, hash, first);
      ValueReference<Object, Object> valueRef = map.newValueReference(entry, value, 1);
      entry.setValueReference(valueRef);
      segment.recordWrite(entry, 1, map.ticker.read());
      table.set(index, entry);
      originalMap.put(key, value);
    }
    segment.count = originalCount;
    segment.totalWeight = originalCount;
    assertEquals(originalCount, map.size());
    assertEquals(originalMap, map);

    Iterator<Object> it = originalMap.keySet().iterator();
    for (int i = 0; i < originalCount - maxSize; i++) {
      it.next();
      it.remove();
    }
    segment.evictEntries(entry);
    assertEquals(maxSize, map.size());
    assertEquals(originalMap, map);
  }

  // reference queues

  public void testDrainKeyReferenceQueueOnWrite() {
    for (CacheBuilder<Object, Object> builder : allKeyValueStrengthMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder.concurrencyLevel(1));
      if (map.usesKeyReferences()) {
        Segment<Object, Object> segment = map.segments[0];

        Object keyOne = new Object();
        int hashOne = map.hash(keyOne);
        Object valueOne = new Object();
        Object keyTwo = new Object();
        Object valueTwo = new Object();

        map.put(keyOne, valueOne);
        ReferenceEntry<Object, Object> entry = segment.getEntry(keyOne, hashOne);

        @SuppressWarnings("unchecked")
        Reference<Object> reference = (Reference<Object>) entry;
        reference.enqueue();

        map.put(keyTwo, valueTwo);
        assertFalse(map.containsKey(keyOne));
        assertFalse(map.containsValue(valueOne));
        assertNull(map.get(keyOne));
        assertEquals(1, map.size());
        assertNull(segment.keyReferenceQueue.poll());
      }
    }
  }

  public void testDrainValueReferenceQueueOnWrite() {
    for (CacheBuilder<Object, Object> builder : allKeyValueStrengthMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder.concurrencyLevel(1));
      if (map.usesValueReferences()) {
        Segment<Object, Object> segment = map.segments[0];

        Object keyOne = new Object();
        int hashOne = map.hash(keyOne);
        Object valueOne = new Object();
        Object keyTwo = new Object();
        Object valueTwo = new Object();

        map.put(keyOne, valueOne);
        ReferenceEntry<Object, Object> entry = segment.getEntry(keyOne, hashOne);
        ValueReference<Object, Object> valueReference = entry.getValueReference();

        @SuppressWarnings("unchecked")
        Reference<Object> reference = (Reference<Object>) valueReference;
        reference.enqueue();

        map.put(keyTwo, valueTwo);
        assertFalse(map.containsKey(keyOne));
        assertFalse(map.containsValue(valueOne));
        assertNull(map.get(keyOne));
        assertEquals(1, map.size());
        assertNull(segment.valueReferenceQueue.poll());
      }
    }
  }

  public void testDrainKeyReferenceQueueOnRead() {
    for (CacheBuilder<Object, Object> builder : allKeyValueStrengthMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder.concurrencyLevel(1));
      if (map.usesKeyReferences()) {
        Segment<Object, Object> segment = map.segments[0];

        Object keyOne = new Object();
        int hashOne = map.hash(keyOne);
        Object valueOne = new Object();
        Object keyTwo = new Object();

        map.put(keyOne, valueOne);
        ReferenceEntry<Object, Object> entry = segment.getEntry(keyOne, hashOne);

        @SuppressWarnings("unchecked")
        Reference<Object> reference = (Reference<Object>) entry;
        reference.enqueue();

        for (int i = 0; i < SMALL_MAX_SIZE; i++) {
          map.get(keyTwo);
        }
        assertFalse(map.containsKey(keyOne));
        assertFalse(map.containsValue(valueOne));
        assertNull(map.get(keyOne));
        assertEquals(0, map.size());
        assertNull(segment.keyReferenceQueue.poll());
      }
    }
  }

  public void testDrainValueReferenceQueueOnRead() {
    for (CacheBuilder<Object, Object> builder : allKeyValueStrengthMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder.concurrencyLevel(1));
      if (map.usesValueReferences()) {
        Segment<Object, Object> segment = map.segments[0];

        Object keyOne = new Object();
        int hashOne = map.hash(keyOne);
        Object valueOne = new Object();
        Object keyTwo = new Object();

        map.put(keyOne, valueOne);
        ReferenceEntry<Object, Object> entry = segment.getEntry(keyOne, hashOne);
        ValueReference<Object, Object> valueReference = entry.getValueReference();

        @SuppressWarnings("unchecked")
        Reference<Object> reference = (Reference<Object>) valueReference;
        reference.enqueue();

        for (int i = 0; i < SMALL_MAX_SIZE; i++) {
          map.get(keyTwo);
        }
        assertFalse(map.containsKey(keyOne));
        assertFalse(map.containsValue(valueOne));
        assertNull(map.get(keyOne));
        assertEquals(0, map.size());
        assertNull(segment.valueReferenceQueue.poll());
      }
    }
  }

  public void testNullParameters() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(makeLocalCache(createCacheBuilder()));
    CacheLoader<Object, Object> loader = identityLoader();
    tester.testAllPublicInstanceMethods(makeLocalCache(createCacheBuilder()));
  }

  public void testSerializationProxyLoading() {
    CacheLoader<Object, Object> loader = new SerializableCacheLoader();
    RemovalListener<Object, Object> listener = new SerializableRemovalListener<>();
    SerializableWeigher<Object, Object> weigher = new SerializableWeigher<>();
    Ticker ticker = new SerializableTicker();
    @SuppressWarnings("unchecked") // createMock
    LocalLoadingCache<Object, Object> one =
        (LocalLoadingCache)
            CacheBuilder.newBuilder()
                .weakKeys()
                .softValues()
                .expireAfterAccess(123, SECONDS)
                .expireAfterWrite(456, MINUTES)
                .maximumWeight(789)
                .weigher(weigher)
                .concurrencyLevel(12)
                .removalListener(listener)
                .ticker(ticker)
                .build(loader);
    // add a non-serializable entry
    one.getUnchecked(new Object());
    assertEquals(1, one.size());
    assertFalse(one.asMap().isEmpty());
    LocalLoadingCache<Object, Object> two = SerializableTester.reserialize(one);
    assertEquals(0, two.size());
    assertTrue(two.asMap().isEmpty());

    LocalCache<Object, Object> localCacheOne = one.localCache;
    LocalCache<Object, Object> localCacheTwo = two.localCache;

    assertEquals(localCacheOne.keyStrength, localCacheTwo.keyStrength);
    assertEquals(localCacheOne.keyStrength, localCacheTwo.keyStrength);
    assertEquals(localCacheOne.valueEquivalence, localCacheTwo.valueEquivalence);
    assertEquals(localCacheOne.valueEquivalence, localCacheTwo.valueEquivalence);
    assertEquals(localCacheOne.maxWeight, localCacheTwo.maxWeight);
    assertEquals(localCacheOne.weigher, localCacheTwo.weigher);
    assertEquals(localCacheOne.expireAfterAccessNanos, localCacheTwo.expireAfterAccessNanos);
    assertEquals(localCacheOne.expireAfterWriteNanos, localCacheTwo.expireAfterWriteNanos);
    assertEquals(localCacheOne.refreshNanos, localCacheTwo.refreshNanos);
    assertEquals(localCacheOne.removalListener, localCacheTwo.removalListener);
    assertEquals(localCacheOne.ticker, localCacheTwo.ticker);

    // serialize the reconstituted version to be sure we haven't lost the ability to reserialize
    LocalLoadingCache<Object, Object> three = SerializableTester.reserialize(two);
    LocalCache<Object, Object> localCacheThree = three.localCache;

    assertEquals(localCacheTwo.defaultLoader, localCacheThree.defaultLoader);
    assertEquals(localCacheTwo.keyStrength, localCacheThree.keyStrength);
    assertEquals(localCacheTwo.keyStrength, localCacheThree.keyStrength);
    assertEquals(localCacheTwo.valueEquivalence, localCacheThree.valueEquivalence);
    assertEquals(localCacheTwo.valueEquivalence, localCacheThree.valueEquivalence);
    assertEquals(localCacheTwo.maxWeight, localCacheThree.maxWeight);
    assertEquals(localCacheTwo.weigher, localCacheThree.weigher);
    assertEquals(localCacheTwo.expireAfterAccessNanos, localCacheThree.expireAfterAccessNanos);
    assertEquals(localCacheTwo.expireAfterWriteNanos, localCacheThree.expireAfterWriteNanos);
    assertEquals(localCacheTwo.removalListener, localCacheThree.removalListener);
    assertEquals(localCacheTwo.ticker, localCacheThree.ticker);
  }

  public void testSerializationProxyManual() {
    RemovalListener<Object, Object> listener = new SerializableRemovalListener<>();
    SerializableWeigher<Object, Object> weigher = new SerializableWeigher<>();
    Ticker ticker = new SerializableTicker();
    @SuppressWarnings("unchecked") // createMock
    LocalManualCache<Object, Object> one =
        (LocalManualCache)
            CacheBuilder.newBuilder()
                .weakKeys()
                .softValues()
                .expireAfterAccess(123, NANOSECONDS)
                .maximumWeight(789)
                .weigher(weigher)
                .concurrencyLevel(12)
                .removalListener(listener)
                .ticker(ticker)
                .build();
    // add a non-serializable entry
    one.put(new Object(), new Object());
    assertEquals(1, one.size());
    assertFalse(one.asMap().isEmpty());
    LocalManualCache<Object, Object> two = SerializableTester.reserialize(one);
    assertEquals(0, two.size());
    assertTrue(two.asMap().isEmpty());

    LocalCache<Object, Object> localCacheOne = one.localCache;
    LocalCache<Object, Object> localCacheTwo = two.localCache;

    assertEquals(localCacheOne.keyStrength, localCacheTwo.keyStrength);
    assertEquals(localCacheOne.keyStrength, localCacheTwo.keyStrength);
    assertEquals(localCacheOne.valueEquivalence, localCacheTwo.valueEquivalence);
    assertEquals(localCacheOne.valueEquivalence, localCacheTwo.valueEquivalence);
    assertEquals(localCacheOne.maxWeight, localCacheTwo.maxWeight);
    assertEquals(localCacheOne.weigher, localCacheTwo.weigher);
    assertEquals(localCacheOne.expireAfterAccessNanos, localCacheTwo.expireAfterAccessNanos);
    assertEquals(localCacheOne.expireAfterWriteNanos, localCacheTwo.expireAfterWriteNanos);
    assertEquals(localCacheOne.removalListener, localCacheTwo.removalListener);
    assertEquals(localCacheOne.ticker, localCacheTwo.ticker);

    // serialize the reconstituted version to be sure we haven't lost the ability to reserialize
    LocalManualCache<Object, Object> three = SerializableTester.reserialize(two);
    LocalCache<Object, Object> localCacheThree = three.localCache;

    assertEquals(localCacheTwo.keyStrength, localCacheThree.keyStrength);
    assertEquals(localCacheTwo.keyStrength, localCacheThree.keyStrength);
    assertEquals(localCacheTwo.valueEquivalence, localCacheThree.valueEquivalence);
    assertEquals(localCacheTwo.valueEquivalence, localCacheThree.valueEquivalence);
    assertEquals(localCacheTwo.maxWeight, localCacheThree.maxWeight);
    assertEquals(localCacheTwo.weigher, localCacheThree.weigher);
    assertEquals(localCacheTwo.expireAfterAccessNanos, localCacheThree.expireAfterAccessNanos);
    assertEquals(localCacheTwo.expireAfterWriteNanos, localCacheThree.expireAfterWriteNanos);
    assertEquals(localCacheTwo.removalListener, localCacheThree.removalListener);
    assertEquals(localCacheTwo.ticker, localCacheThree.ticker);
  }

  public void testLoadDifferentKeyInLoader() throws ExecutionException, InterruptedException {
    LocalCache<String, String> cache = makeLocalCache(createCacheBuilder());
    String key1 = "key1";
    String key2 = "key2";

    assertEquals(
        key2,
        cache.get(
            key1,
            new CacheLoader<String, String>() {
              @Override
              public String load(String key) throws Exception {
                return cache.get(key2, identityLoader()); // loads a different key, should work
              }
            }));
  }

  public void testRecursiveLoad() throws InterruptedException {
    LocalCache<String, String> cache = makeLocalCache(createCacheBuilder());
    String key = "key";
    CacheLoader<String, String> loader =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws Exception {
            return cache.get(key, identityLoader()); // recursive load, this should fail
          }
        };
    testLoadThrows(key, cache, loader);
  }

  public void testRecursiveLoadWithProxy() throws InterruptedException {
    String key = "key";
    String otherKey = "otherKey";
    LocalCache<String, String> cache = makeLocalCache(createCacheBuilder());
    CacheLoader<String, String> loader =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws Exception {
            return cache.get(
                key,
                identityLoader()); // recursive load (same as the initial one), this should fail
          }
        };
    CacheLoader<String, String> proxyLoader =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws Exception {
            return cache.get(otherKey, loader); // loads another key, is ok
          }
        };
    testLoadThrows(key, cache, proxyLoader);
  }

  // utility methods

  private void testLoadThrows(
      String key, LocalCache<String, String> cache, CacheLoader<String, String> loader)
      throws InterruptedException {
    CountDownLatch doneSignal = new CountDownLatch(1);
    Thread thread =
        new Thread(
            () -> {
              try {
                cache.get(key, loader);
              } catch (UncheckedExecutionException | ExecutionException e) {
                doneSignal.countDown();
              }
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

  /**
   * Returns an iterable containing all combinations of maximumSize, expireAfterAccess/Write,
   * weakKeys and weak/softValues.
   */
  @SuppressWarnings("unchecked") // varargs
  private static Iterable<CacheBuilder<Object, Object>> allEntryTypeMakers() {
    List<CacheBuilder<Object, Object>> result = newArrayList(allKeyValueStrengthMakers());
    for (CacheBuilder<Object, Object> builder : allKeyValueStrengthMakers()) {
      result.add(builder.maximumSize(SMALL_MAX_SIZE));
    }
    for (CacheBuilder<Object, Object> builder : allKeyValueStrengthMakers()) {
      result.add(builder.expireAfterAccess(99999, SECONDS));
    }
    for (CacheBuilder<Object, Object> builder : allKeyValueStrengthMakers()) {
      result.add(builder.expireAfterWrite(99999, SECONDS));
    }
    for (CacheBuilder<Object, Object> builder : allKeyValueStrengthMakers()) {
      result.add(builder.maximumSize(SMALL_MAX_SIZE).expireAfterAccess(99999, SECONDS));
    }
    for (CacheBuilder<Object, Object> builder : allKeyValueStrengthMakers()) {
      result.add(builder.maximumSize(SMALL_MAX_SIZE).expireAfterWrite(99999, SECONDS));
    }
    return result;
  }

  /** Returns an iterable containing all combinations of maximumSize and expireAfterAccess/Write. */
  @SuppressWarnings("unchecked") // varargs
  static Iterable<CacheBuilder<Object, Object>> allEvictingMakers() {
    return ImmutableList.of(
        createCacheBuilder().maximumSize(SMALL_MAX_SIZE),
        createCacheBuilder().expireAfterAccess(99999, SECONDS),
        createCacheBuilder().expireAfterWrite(99999, SECONDS),
        createCacheBuilder()
            .maximumSize(SMALL_MAX_SIZE)
            .expireAfterAccess(SMALL_MAX_SIZE, TimeUnit.SECONDS),
        createCacheBuilder()
            .maximumSize(SMALL_MAX_SIZE)
            .expireAfterWrite(SMALL_MAX_SIZE, TimeUnit.SECONDS));
  }

  /** Returns an iterable containing all combinations weakKeys and weak/softValues. */
  @SuppressWarnings("unchecked") // varargs
  private static Iterable<CacheBuilder<Object, Object>> allKeyValueStrengthMakers() {
    return ImmutableList.of(
        createCacheBuilder(),
        createCacheBuilder().weakValues(),
        createCacheBuilder().softValues(),
        createCacheBuilder().weakKeys(),
        createCacheBuilder().weakKeys().weakValues(),
        createCacheBuilder().weakKeys().softValues());
  }

  // entries and values

  private static <K, V> DummyEntry<K, V> createDummyEntry(
      K key, int hash, V value, @Nullable ReferenceEntry<K, V> next) {
    DummyEntry<K, V> entry = DummyEntry.create(key, hash, next);
    DummyValueReference<K, V> valueRef = DummyValueReference.create(value);
    entry.setValueReference(valueRef);
    return entry;
  }

  static class DummyEntry<K, V> implements ReferenceEntry<K, V> {
    private @Nullable K key;
    private final int hash;
    private final ReferenceEntry<K, V> next;

    public DummyEntry(K key, int hash, ReferenceEntry<K, V> next) {
      this.key = key;
      this.hash = hash;
      this.next = next;
    }

    public static <K, V> DummyEntry<K, V> create(
        K key, int hash, @Nullable ReferenceEntry<K, V> next) {
      return new DummyEntry<>(key, hash, next);
    }

    public void clearKey() {
      this.key = null;
    }

    private ValueReference<K, V> valueReference = unset();

    @Override
    public ValueReference<K, V> getValueReference() {
      return valueReference;
    }

    @Override
    public void setValueReference(ValueReference<K, V> valueReference) {
      this.valueReference = valueReference;
    }

    @Override
    public ReferenceEntry<K, V> getNext() {
      return next;
    }

    @Override
    public int getHash() {
      return hash;
    }

    @Override
    public K getKey() {
      return key;
    }

    private long accessTime = Long.MAX_VALUE;

    @Override
    public long getAccessTime() {
      return accessTime;
    }

    @Override
    public void setAccessTime(long time) {
      this.accessTime = time;
    }

    private ReferenceEntry<K, V> nextAccess = nullEntry();

    @Override
    public ReferenceEntry<K, V> getNextInAccessQueue() {
      return nextAccess;
    }

    @Override
    public void setNextInAccessQueue(ReferenceEntry<K, V> next) {
      this.nextAccess = next;
    }

    private ReferenceEntry<K, V> previousAccess = nullEntry();

    @Override
    public ReferenceEntry<K, V> getPreviousInAccessQueue() {
      return previousAccess;
    }

    @Override
    public void setPreviousInAccessQueue(ReferenceEntry<K, V> previous) {
      this.previousAccess = previous;
    }

    private long writeTime = Long.MAX_VALUE;

    @Override
    public long getWriteTime() {
      return writeTime;
    }

    @Override
    public void setWriteTime(long time) {
      this.writeTime = time;
    }

    private ReferenceEntry<K, V> nextWrite = nullEntry();

    @Override
    public ReferenceEntry<K, V> getNextInWriteQueue() {
      return nextWrite;
    }

    @Override
    public void setNextInWriteQueue(ReferenceEntry<K, V> next) {
      this.nextWrite = next;
    }

    private ReferenceEntry<K, V> previousWrite = nullEntry();

    @Override
    public ReferenceEntry<K, V> getPreviousInWriteQueue() {
      return previousWrite;
    }

    @Override
    public void setPreviousInWriteQueue(ReferenceEntry<K, V> previous) {
      this.previousWrite = previous;
    }
  }

  static class DummyValueReference<K, V> implements ValueReference<K, V> {
    private @Nullable V value;
    boolean loading = false;

    public DummyValueReference() {
      this.loading = true;
    }

    public DummyValueReference(V value) {
      this.value = value;
    }

    public static <K, V> DummyValueReference<K, V> create(V value) {
      return new DummyValueReference<>(value);
    }

    public static <K, V> DummyValueReference<K, V> createLoading() {
      return new DummyValueReference<>();
    }

    @Override
    public V get() {
      return value;
    }

    @Override
    public int getWeight() {
      return 1;
    }

    @Override
    public @Nullable ReferenceEntry<K, V> getEntry() {
      return null;
    }

    @Override
    public ValueReference<K, V> copyFor(
        ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
      return this;
    }

    public void setLoading(boolean loading) {
      this.loading = loading;
    }

    @Override
    public boolean isLoading() {
      return loading;
    }

    @Override
    public boolean isActive() {
      return !loading;
    }

    @Override
    public V waitForValue() {
      return get();
    }

    @Override
    public void notifyNewValue(V newValue) {}

    public void clear() {
      value = null;
    }
  }

  private static class SerializableCacheLoader extends CacheLoader<Object, Object>
      implements Serializable {
    @Override
    public Object load(Object key) {
      return new Object();
    }

    @Override
    public int hashCode() {
      return 42;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      return (o instanceof SerializableCacheLoader);
    }
  }

  private static class SerializableRemovalListener<K, V>
      implements RemovalListener<K, V>, Serializable {
    @Override
    public void onRemoval(RemovalNotification<K, V> notification) {}

    @Override
    public int hashCode() {
      return 42;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      return (o instanceof SerializableRemovalListener);
    }
  }

  private static class SerializableTicker extends Ticker implements Serializable {
    @Override
    public long read() {
      return 42;
    }

    @Override
    public int hashCode() {
      return 42;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      return (o instanceof SerializableTicker);
    }
  }

  private static class SerializableWeigher<K, V> implements Weigher<K, V>, Serializable {
    @Override
    public int weigh(K key, V value) {
      return 42;
    }

    @Override
    public int hashCode() {
      return 42;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      return (o instanceof SerializableWeigher);
    }
  }
}
