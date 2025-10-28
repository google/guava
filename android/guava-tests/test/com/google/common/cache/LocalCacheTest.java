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
import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.lang.Math.max;
import static java.lang.Thread.State.WAITING;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
import com.google.common.collect.Iterables;
import com.google.common.collect.testing.ConcurrentMapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.testing.FakeTicker;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import com.google.common.testing.TestLogHandler;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * @author Charles Fry
 */
@SuppressWarnings("GuardedBy") // TODO(b/35466881): Fix or suppress.
@NullUnmarked
public class LocalCacheTest extends TestCase {
  @AndroidIncompatible
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

  @AndroidIncompatible // test-suite builders
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
    assertThat(logRecords).hasSize(1);
    LogRecord logRecord = logRecords.get(0);
    logHandler.clear();
    return logRecord.getThrown();
  }

  private void checkNothingLogged() {
    assertThat(logHandler.getStoredLogRecords().isEmpty()).isTrue();
  }

  private void checkLogged(Throwable t) {
    assertThat(popLoggedThrowable()).isSameInstanceAs(t);
  }

  /*
   * TODO(cpovirk): Can we replace makeLocalCache with a call to builder.build()? Some tests may
   * need access to LocalCache APIs, but maybe we can at least make makeLocalCache use
   * builder.build() and then cast?
   */

  private static <K, V> LocalCache<K, V> makeLocalCache(
      CacheBuilder<? super K, ? super V> builder) {
    return new LocalCache<>(builder, null);
  }

  private static <K, V> LocalCache<K, V> makeLocalCache(
      CacheBuilder<? super K, ? super V> builder, CacheLoader<? super K, V> loader) {
    return new LocalCache<>(builder, loader);
  }

  // TODO(cpovirk): Inline createCacheBuilder()?

  private static CacheBuilder<Object, Object> createCacheBuilder() {
    return CacheBuilder.newBuilder();
  }

  // constructor tests

  public void testDefaults() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder());

    assertThat(map.keyStrength).isEqualTo(Strength.STRONG);
    assertThat(map.valueStrength).isEqualTo(Strength.STRONG);
    assertThat(map.keyEquivalence).isSameInstanceAs(map.keyStrength.defaultEquivalence());
    assertThat(map.valueEquivalence).isSameInstanceAs(map.valueStrength.defaultEquivalence());

    assertThat(map.expireAfterAccessNanos).isEqualTo(0);
    assertThat(map.expireAfterWriteNanos).isEqualTo(0);
    assertThat(map.refreshNanos).isEqualTo(0);
    assertThat(map.maxWeight).isEqualTo(CacheBuilder.UNSET_INT);

    assertThat(map.entryFactory).isSameInstanceAs(EntryFactory.STRONG);
    assertThat(map.removalListener).isSameInstanceAs(CacheBuilder.NullListener.INSTANCE);
    assertThat(map.removalNotificationQueue).isSameInstanceAs(DISCARDING_QUEUE);
    assertThat(map.ticker).isSameInstanceAs(NULL_TICKER);

    assertThat(map.concurrencyLevel).isEqualTo(4);

    // concurrency level
    assertThat(map.segments).hasLength(4);
    // initial capacity / concurrency level
    assertThat(map.segments[0].table.length()).isEqualTo(16 / map.segments.length);

    assertThat(map.evictsBySize()).isFalse();
    assertThat(map.expires()).isFalse();
    assertThat(map.expiresAfterWrite()).isFalse();
    assertThat(map.expiresAfterAccess()).isFalse();
    assertThat(map.refreshes()).isFalse();
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
    assertThat(map.keyEquivalence).isSameInstanceAs(testEquivalence);
    assertThat(map.valueEquivalence).isSameInstanceAs(map.valueStrength.defaultEquivalence());
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
    assertThat(map.valueEquivalence).isSameInstanceAs(testEquivalence);
    assertThat(map.keyEquivalence).isSameInstanceAs(map.keyStrength.defaultEquivalence());
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
      assertThat(map.segments[i].table.length()).isEqualTo(segmentSize);
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
    assertWithMessage("segments=%s, maxSize=%s", map.segments.length, maxSize)
        .that((long) map.segments.length)
        .isAtMost(max(1, maxSize / 10));
    for (int i = 0; i < map.segments.length; i++) {
      totalCapacity += map.segments[i].maxSegmentWeight;
    }
    assertWithMessage("totalCapacity=%s, maxSize=%s", totalCapacity, maxSize)
        .that(totalCapacity)
        .isEqualTo(maxSize);

    map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(concurrencyLevel)
                .initialCapacity(initialCapacity)
                .maximumWeight(maxSize)
                .weigher(constantWeigher(1)));
    assertWithMessage("segments=%s, maxSize=%s", map.segments.length, maxSize)
        .that((long) map.segments.length)
        .isAtMost(max(1, maxSize / 10));
    totalCapacity = 0;
    for (int i = 0; i < map.segments.length; i++) {
      totalCapacity += map.segments[i].maxSegmentWeight;
    }
    assertWithMessage("totalCapacity=%s, maxSize=%s", totalCapacity, maxSize)
        .that(totalCapacity)
        .isEqualTo(maxSize);
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
    assertThat(map.weigher).isSameInstanceAs(testWeigher);
  }

  public void testSetWeakKeys() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().weakKeys());
    checkStrength(map, Strength.WEAK, Strength.STRONG);
    assertThat(map.entryFactory).isSameInstanceAs(EntryFactory.WEAK);
  }

  public void testSetWeakValues() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().weakValues());
    checkStrength(map, Strength.STRONG, Strength.WEAK);
    assertThat(map.entryFactory).isSameInstanceAs(EntryFactory.STRONG);
  }

  public void testSetSoftValues() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().softValues());
    checkStrength(map, Strength.STRONG, Strength.SOFT);
    assertThat(map.entryFactory).isSameInstanceAs(EntryFactory.STRONG);
  }

  private static void checkStrength(
      LocalCache<Object, Object> map, Strength keyStrength, Strength valueStrength) {
    assertThat(map.keyStrength).isSameInstanceAs(keyStrength);
    assertThat(map.valueStrength).isSameInstanceAs(valueStrength);
    assertThat(map.keyEquivalence).isSameInstanceAs(keyStrength.defaultEquivalence());
    assertThat(map.valueEquivalence).isSameInstanceAs(valueStrength.defaultEquivalence());
  }

  public void testSetExpireAfterWrite() {
    long duration = 42;
    TimeUnit unit = SECONDS;
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().expireAfterWrite(duration, unit));
    assertThat(map.expireAfterWriteNanos).isEqualTo(unit.toNanos(duration));
  }

  public void testSetExpireAfterAccess() {
    long duration = 42;
    TimeUnit unit = SECONDS;
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().expireAfterAccess(duration, unit));
    assertThat(map.expireAfterAccessNanos).isEqualTo(unit.toNanos(duration));
  }

  public void testSetRefresh() {
    long duration = 42;
    TimeUnit unit = SECONDS;
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().refreshAfterWrite(duration, unit));
    assertThat(map.refreshNanos).isEqualTo(unit.toNanos(duration));
  }

  public void testLongAsyncRefresh() throws Exception {
    FakeTicker ticker = new FakeTicker();
    CountDownLatch reloadStarted = new CountDownLatch(1);
    SettableFuture<Thread> threadAboutToBlockForRefresh = SettableFuture.create();

    ListeningExecutorService refreshExecutor = listeningDecorator(newSingleThreadExecutor());
    try {
      CacheBuilder<Object, Object> builder =
          createCacheBuilder()
              .expireAfterWrite(100, MILLISECONDS)
              .refreshAfterWrite(5, MILLISECONDS)
              .ticker(ticker);

      CacheLoader<String, String> loader =
          new CacheLoader<String, String>() {
            @Override
            public String load(String key) {
              return key + "Load";
            }

            @Override
            @SuppressWarnings("ThreadPriorityCheck") // TODO: b/175898629 - Consider onSpinWait.
            public ListenableFuture<String> reload(String key, String oldValue) {
              return refreshExecutor.submit(
                  () -> {
                    reloadStarted.countDown();

                    Thread blockingForRefresh = threadAboutToBlockForRefresh.get();
                    while (blockingForRefresh.isAlive()
                        && blockingForRefresh.getState() != WAITING) {
                      Thread.yield();
                    }

                    return key + "Reload";
                  });
            }
          };
      LocalCache<String, String> cache = makeLocalCache(builder, loader);

      assertThat(cache.getOrLoad("test")).isEqualTo("testLoad");

      ticker.advance(10, MILLISECONDS); // so that the next call will trigger refresh
      assertThat(cache.getOrLoad("test")).isEqualTo("testLoad");
      reloadStarted.await();
      ticker.advance(500, MILLISECONDS); // so that the entry expires during the reload
      threadAboutToBlockForRefresh.set(Thread.currentThread());
      assertThat(cache.getOrLoad("test")).isEqualTo("testReload");
    } finally {
      refreshExecutor.shutdown();
    }
  }

  public void testSetRemovalListener() {
    RemovalListener<Object, Object> testListener = TestingRemovalListeners.nullRemovalListener();
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().removalListener(testListener));
    assertThat(map.removalListener).isSameInstanceAs(testListener);
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
    assertThat(map.ticker).isSameInstanceAs(testTicker);
  }

  public void testEntryFactory() {
    assertThat(EntryFactory.getFactory(Strength.STRONG, false, false))
        .isSameInstanceAs(EntryFactory.STRONG);
    assertThat(EntryFactory.getFactory(Strength.STRONG, true, false))
        .isSameInstanceAs(EntryFactory.STRONG_ACCESS);
    assertThat(EntryFactory.getFactory(Strength.STRONG, false, true))
        .isSameInstanceAs(EntryFactory.STRONG_WRITE);
    assertThat(EntryFactory.getFactory(Strength.STRONG, true, true))
        .isSameInstanceAs(EntryFactory.STRONG_ACCESS_WRITE);
    assertThat(EntryFactory.getFactory(Strength.WEAK, false, false))
        .isSameInstanceAs(EntryFactory.WEAK);
    assertThat(EntryFactory.getFactory(Strength.WEAK, true, false))
        .isSameInstanceAs(EntryFactory.WEAK_ACCESS);
    assertThat(EntryFactory.getFactory(Strength.WEAK, false, true))
        .isSameInstanceAs(EntryFactory.WEAK_WRITE);
    assertThat(EntryFactory.getFactory(Strength.WEAK, true, true))
        .isSameInstanceAs(EntryFactory.WEAK_ACCESS_WRITE);
  }

  // computation tests

  public void testCompute() throws ExecutionException {
    CountingLoader loader = new CountingLoader();
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder());
    assertThat(loader.getCount()).isEqualTo(0);

    Object key = new Object();
    Object value = map.get(key, loader);
    assertThat(loader.getCount()).isEqualTo(1);
    assertThat(map.get(key, loader)).isEqualTo(value);
    assertThat(loader.getCount()).isEqualTo(1);
  }

  public void testRecordReadOnCompute() throws ExecutionException {
    CountingLoader loader = new CountingLoader();
    for (CacheBuilder<Object, Object> builder : allEvictingMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];
      List<ReferenceEntry<Object, Object>> writeOrder = new LinkedList<>();
      List<ReferenceEntry<Object, Object>> readOrder = new LinkedList<>();
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
      assertThat(segment.recencyQueue.isEmpty()).isTrue();

      // access some of the elements
      Random random = new Random();
      List<ReferenceEntry<Object, Object>> reads = new ArrayList<>();
      Iterator<ReferenceEntry<Object, Object>> i = readOrder.iterator();
      while (i.hasNext()) {
        ReferenceEntry<Object, Object> entry = i.next();
        if (random.nextBoolean()) {
          map.get(entry.getKey(), loader);
          reads.add(entry);
          i.remove();
          assertThat(segment.recencyQueue.size()).isAtMost(DRAIN_THRESHOLD);
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
    assertThat(loader.getCount()).isEqualTo(0);

    Object key = new Object();
    Object value = new Object();
    map.put(key, value);

    assertThat(map.get(key, loader)).isEqualTo(value);
    assertThat(loader.getCount()).isEqualTo(0);
  }

  public void testComputePartiallyCollectedKey() throws ExecutionException {
    CacheBuilder<Object, Object> builder = createCacheBuilder().concurrencyLevel(1);
    CountingLoader loader = new CountingLoader();
    LocalCache<Object, Object> map = makeLocalCache(builder);
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertThat(loader.getCount()).isEqualTo(0);

    Object key = new Object();
    int hash = map.hash(key);
    Object value = new Object();
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> valueRef = DummyValueReference.create(value);
    entry.setValueReference(valueRef);
    table.set(index, entry);
    segment.count++;

    assertThat(map.get(key, loader)).isSameInstanceAs(value);
    assertThat(loader.getCount()).isEqualTo(0);
    assertThat(segment.count).isEqualTo(1);

    entry.clearKey();
    assertThat(map.get(key, loader)).isNotSameInstanceAs(value);
    assertThat(loader.getCount()).isEqualTo(1);
    assertThat(segment.count).isEqualTo(2);
  }

  public void testComputePartiallyCollectedValue() throws ExecutionException {
    CacheBuilder<Object, Object> builder = createCacheBuilder().concurrencyLevel(1);
    CountingLoader loader = new CountingLoader();
    LocalCache<Object, Object> map = makeLocalCache(builder);
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertThat(loader.getCount()).isEqualTo(0);

    Object key = new Object();
    int hash = map.hash(key);
    Object value = new Object();
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> valueRef = DummyValueReference.create(value);
    entry.setValueReference(valueRef);
    table.set(index, entry);
    segment.count++;

    assertThat(map.get(key, loader)).isSameInstanceAs(value);
    assertThat(loader.getCount()).isEqualTo(0);
    assertThat(segment.count).isEqualTo(1);

    valueRef.clear();
    assertThat(map.get(key, loader)).isNotSameInstanceAs(value);
    assertThat(loader.getCount()).isEqualTo(1);
    assertThat(segment.count).isEqualTo(1);
  }

  @AndroidIncompatible // Perhaps emulator clock does not update between the two get() calls?
  public void testComputeExpiredEntry() throws ExecutionException {
    CacheBuilder<Object, Object> builder = createCacheBuilder().expireAfterWrite(1, NANOSECONDS);
    CountingLoader loader = new CountingLoader();
    LocalCache<Object, Object> map = makeLocalCache(builder);
    assertThat(loader.getCount()).isEqualTo(0);

    Object key = new Object();
    Object one = map.get(key, loader);
    assertThat(loader.getCount()).isEqualTo(1);

    Object two = map.get(key, loader);
    assertThat(two).isNotSameInstanceAs(one);
    assertThat(loader.getCount()).isEqualTo(2);
  }

  public void testValues() {
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder());
    map.put("foo", "bar");
    map.put("baz", "bar");
    map.put("quux", "quux");
    assertThat(map.values() instanceof Set).isFalse();
    assertThat(map.values().removeAll(ImmutableSet.of("bar"))).isTrue();
    assertThat(map).hasSize(1);
  }

  public void testCopyEntry_computing() {
    CountDownLatch startSignal = new CountDownLatch(1);
    CountDownLatch computingSignal = new CountDownLatch(1);
    CountDownLatch doneSignal = new CountDownLatch(2);
    Object computedObject = new Object();

    CacheLoader<Object, Object> loader =
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
    LocalCache<Object, Object> map = makeLocalCache(builder);
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertThat(listener.isEmpty()).isTrue();

    Object one = new Object();
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
    assertThat(valueReference.futureValue.isDone()).isFalse();
    startSignal.countDown();

    try {
      doneSignal.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    map.cleanUp(); // force notifications
    assertThat(listener.isEmpty()).isTrue();
    assertThat(map.containsKey(one)).isTrue();
    assertThat(map).hasSize(1);
    assertThat(map.get(one)).isSameInstanceAs(computedObject);
  }

  public void testRemovalListenerCheckedException() {
    RuntimeException e = new RuntimeException();
    RemovalListener<Object, Object> listener =
        new RemovalListener<Object, Object>() {
          @Override
          public void onRemoval(RemovalNotification<Object, Object> notification) {
            throw e;
          }
        };

    CacheBuilder<Object, Object> builder = createCacheBuilder().removalListener(listener);
    LocalCache<Object, Object> cache = makeLocalCache(builder);
    Object key = new Object();
    cache.put(key, new Object());
    checkNothingLogged();

    cache.remove(key);
    checkLogged(e);
  }

  public void testRemovalListener_replaced_computing() {
    CountDownLatch startSignal = new CountDownLatch(1);
    CountDownLatch computingSignal = new CountDownLatch(1);
    CountDownLatch doneSignal = new CountDownLatch(1);
    Object computedObject = new Object();

    CacheLoader<Object, Object> loader =
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
    LocalCache<Object, Object> map = makeLocalCache(builder);
    assertThat(listener.isEmpty()).isTrue();

    Object one = new Object();
    Object two = new Object();

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
    assertThat(map.get(one)).isSameInstanceAs(two);
    startSignal.countDown();

    try {
      doneSignal.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    map.cleanUp(); // force notifications
    assertNotified(listener, one, computedObject, RemovalCause.REPLACED);
    assertThat(listener.isEmpty()).isTrue();
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
    assertThat(segment.refresh(key, hash, identityLoader(), false)).isNull();
  }

  // Removal listener tests

  public void testRemovalListener_explicit() {
    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().removalListener(listener));
    assertThat(listener.isEmpty()).isTrue();

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

    assertThat(listener.isEmpty()).isTrue();
  }

  public void testRemovalListener_replaced() {
    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    LocalCache<Object, Object> map = makeLocalCache(createCacheBuilder().removalListener(listener));
    assertThat(listener.isEmpty()).isTrue();

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
    assertThat(listener.isEmpty()).isTrue();

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();

    map.put(one, two);
    map.put(two, three);
    assertThat(listener.isEmpty()).isTrue();

    int hash = map.hash(one);
    ReferenceEntry<Object, Object> entry = segment.getEntry(one, hash);
    map.reclaimValue(entry.getValueReference());
    assertNotified(listener, one, two, RemovalCause.COLLECTED);

    assertThat(listener.isEmpty()).isTrue();
  }

  public void testRemovalListener_expired() {
    FakeTicker ticker = new FakeTicker();
    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .expireAfterWrite(3, NANOSECONDS)
                .ticker(ticker)
                .removalListener(listener));
    assertThat(listener.isEmpty()).isTrue();

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
    assertThat(listener.isEmpty()).isTrue();
    ticker.advance(1);
    map.put(four, five);
    assertNotified(listener, one, two, RemovalCause.EXPIRED);

    assertThat(listener.isEmpty()).isTrue();
  }

  public void testRemovalListener_size() {
    QueuingRemovalListener<Object, Object> listener = queuingRemovalListener();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder().concurrencyLevel(1).maximumSize(2).removalListener(listener));
    assertThat(listener.isEmpty()).isTrue();

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();
    Object four = new Object();

    map.put(one, two);
    map.put(two, three);
    assertThat(listener.isEmpty()).isTrue();
    map.put(three, four);
    assertNotified(listener, one, two, RemovalCause.SIZE);

    assertThat(listener.isEmpty()).isTrue();
  }

  static <K, V> void assertNotified(
      QueuingRemovalListener<K, V> listener, K key, V value, RemovalCause cause) {
    RemovalNotification<K, V> notification = listener.remove();
    assertThat(notification.getKey()).isSameInstanceAs(key);
    assertThat(notification.getValue()).isSameInstanceAs(value);
    assertThat(notification.getCause()).isSameInstanceAs(cause);
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
      assertThat(valueRefOne.get()).isSameInstanceAs(valueOne);
      entryOne.setValueReference(valueRefOne);

      assertThat(entryOne.getKey()).isSameInstanceAs(keyOne);
      assertThat(entryOne.getHash()).isEqualTo(hashOne);
      assertThat(entryOne.getNext()).isNull();
      assertThat(entryOne.getValueReference()).isSameInstanceAs(valueRefOne);

      Object keyTwo = new Object();
      Object valueTwo = new Object();
      int hashTwo = map.hash(keyTwo);
      ReferenceEntry<Object, Object> entryTwo = map.newEntry(keyTwo, hashTwo, entryOne);
      ValueReference<Object, Object> valueRefTwo = map.newValueReference(entryTwo, valueTwo, 1);
      assertThat(valueRefTwo.get()).isSameInstanceAs(valueTwo);
      entryTwo.setValueReference(valueRefTwo);

      assertThat(entryTwo.getKey()).isSameInstanceAs(keyTwo);
      assertThat(entryTwo.getHash()).isEqualTo(hashTwo);
      assertThat(entryTwo.getNext()).isSameInstanceAs(entryOne);
      assertThat(entryTwo.getValueReference()).isSameInstanceAs(valueRefTwo);
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
      assertThat(entryOne.getKey()).isSameInstanceAs(keyOne);
      assertThat(entryOne.getHash()).isEqualTo(hashOne);
      assertThat(entryOne.getNext()).isNull();
      assertThat(copyOne.getValueReference().get()).isSameInstanceAs(valueOne);
      assertConnected(map, copyOne, entryTwo);

      ReferenceEntry<Object, Object> copyTwo = map.copyEntry(entryTwo, copyOne);
      assertThat(copyTwo.getKey()).isSameInstanceAs(keyTwo);
      assertThat(copyTwo.getHash()).isEqualTo(hashTwo);
      assertThat(copyTwo.getNext()).isSameInstanceAs(copyOne);
      assertThat(copyTwo.getValueReference().get()).isSameInstanceAs(valueTwo);
      assertConnected(map, copyOne, copyTwo);
    }
  }

  private static <K, V> void assertConnected(
      LocalCache<K, V> map, ReferenceEntry<K, V> one, ReferenceEntry<K, V> two) {
    if (map.usesWriteQueue()) {
      assertThat(one.getNextInWriteQueue()).isSameInstanceAs(two);
    }
    if (map.usesAccessQueue()) {
      assertThat(one.getNextInAccessQueue()).isSameInstanceAs(two);
    }
  }

  public void testSegmentGetAndContains() {
    FakeTicker ticker = new FakeTicker();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .ticker(ticker)
                .expireAfterAccess(1, NANOSECONDS));
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

    assertThat(segment.get(key, hash)).isNull();

    // count == 0
    table.set(index, entry);
    assertThat(segment.get(key, hash)).isNull();
    assertThat(segment.containsKey(key, hash)).isFalse();
    assertThat(segment.containsValue(value)).isFalse();

    // count == 1
    segment.count++;
    assertThat(segment.get(key, hash)).isSameInstanceAs(value);
    assertThat(segment.containsKey(key, hash)).isTrue();
    assertThat(segment.containsValue(value)).isTrue();
    // don't see absent values now that count > 0
    assertThat(segment.get(new Object(), hash)).isNull();

    // null key
    DummyEntry<Object, Object> nullEntry = DummyEntry.create(null, hash, entry);
    Object nullValue = new Object();
    ValueReference<Object, Object> nullValueRef = map.newValueReference(nullEntry, nullValue, 1);
    nullEntry.setValueReference(nullValueRef);
    table.set(index, nullEntry);
    // skip the null key
    assertThat(segment.get(key, hash)).isSameInstanceAs(value);
    assertThat(segment.containsKey(key, hash)).isTrue();
    assertThat(segment.containsValue(value)).isTrue();
    assertThat(segment.containsValue(nullValue)).isFalse();

    // hash collision
    DummyEntry<Object, Object> dummy = DummyEntry.create(new Object(), hash, entry);
    Object dummyValue = new Object();
    ValueReference<Object, Object> dummyValueRef = map.newValueReference(dummy, dummyValue, 1);
    dummy.setValueReference(dummyValueRef);
    table.set(index, dummy);
    assertThat(segment.get(key, hash)).isSameInstanceAs(value);
    assertThat(segment.containsKey(key, hash)).isTrue();
    assertThat(segment.containsValue(value)).isTrue();
    assertThat(segment.containsValue(dummyValue)).isTrue();

    // key collision
    dummy = DummyEntry.create(key, hash, entry);
    dummyValue = new Object();
    dummyValueRef = map.newValueReference(dummy, dummyValue, 1);
    dummy.setValueReference(dummyValueRef);
    table.set(index, dummy);
    // returns the most recent entry
    assertThat(segment.get(key, hash)).isSameInstanceAs(dummyValue);
    assertThat(segment.containsKey(key, hash)).isTrue();
    assertThat(segment.containsValue(value)).isTrue();
    assertThat(segment.containsValue(dummyValue)).isTrue();

    // expired
    dummy.setAccessTime(ticker.read() - 2);
    assertThat(segment.get(key, hash)).isNull();
    assertThat(segment.containsKey(key, hash)).isFalse();
    assertThat(segment.containsValue(value)).isTrue();
    assertThat(segment.containsValue(dummyValue)).isFalse();
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
    assertThat(segment.replace(key, hash, oldValue, newValue)).isFalse();
    assertThat(segment.count).isEqualTo(0);

    // same value
    table.set(index, entry);
    segment.count++;
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);
    assertThat(segment.replace(key, hash, oldValue, newValue)).isTrue();
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(newValue);

    // different value
    assertThat(segment.replace(key, hash, oldValue, newValue)).isFalse();
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(newValue);

    // cleared
    entry.setValueReference(oldValueRef);
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);
    oldValueRef.clear();
    assertThat(segment.replace(key, hash, oldValue, newValue)).isFalse();
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.get(key, hash)).isNull();
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
    assertThat(segment.replace(key, hash, newValue)).isNull();
    assertThat(segment.count).isEqualTo(0);

    // same key
    table.set(index, entry);
    segment.count++;
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);
    assertThat(segment.replace(key, hash, newValue)).isSameInstanceAs(oldValue);
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(newValue);

    // cleared
    entry.setValueReference(oldValueRef);
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);
    oldValueRef.clear();
    assertThat(segment.replace(key, hash, newValue)).isNull();
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.get(key, hash)).isNull();
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
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.put(key, hash, oldValue, false)).isNull();
    assertThat(segment.count).isEqualTo(1);

    // same key
    assertThat(segment.put(key, hash, newValue, false)).isSameInstanceAs(oldValue);
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(newValue);

    // cleared
    ReferenceEntry<Object, Object> entry = segment.getEntry(key, hash);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue);
    entry.setValueReference(oldValueRef);
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);
    oldValueRef.clear();
    assertThat(segment.put(key, hash, newValue, false)).isNull();
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(newValue);
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
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.put(key, hash, oldValue, true)).isNull();
    assertThat(segment.count).isEqualTo(1);

    // same key
    assertThat(segment.put(key, hash, newValue, true)).isSameInstanceAs(oldValue);
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);

    // cleared
    ReferenceEntry<Object, Object> entry = segment.getEntry(key, hash);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue);
    entry.setValueReference(oldValueRef);
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);
    oldValueRef.clear();
    assertThat(segment.put(key, hash, newValue, true)).isNull();
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(newValue);
  }

  public void testSegmentPut_expand() {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object> segment = map.segments[0];
    assertThat(segment.table.length()).isEqualTo(1);

    int count = 1024;
    for (int i = 0; i < count; i++) {
      Object key = new Object();
      Object value = new Object();
      int hash = map.hash(key);
      assertThat(segment.put(key, hash, value, false)).isNull();
      assertThat(segment.table.length()).isGreaterThan(i);
    }
  }

  public void testSegmentPut_evict() {
    int maxSize = 10;
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).maximumSize(maxSize));

    // manually add elements to avoid eviction
    int originalCount = 1024;
    LinkedHashMap<Object, Object> originalMap = new LinkedHashMap<>();
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
      assertThat(map).isEqualTo(originalMap);
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
    assertThat(listener.isEmpty()).isTrue();
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.get(key, hash)).isNull();
    assertThat(segment.storeLoadedValue(key, hash, valueRef, value)).isTrue();
    assertThat(segment.get(key, hash)).isSameInstanceAs(value);
    assertThat(segment.count).isEqualTo(1);
    assertThat(listener.isEmpty()).isTrue();

    // clobbered
    Object value2 = new Object();
    assertThat(segment.storeLoadedValue(key, hash, valueRef, value2)).isFalse();
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(value);
    RemovalNotification<Object, Object> notification = listener.remove();
    assertThat(notification).isEqualTo(immutableEntry(key, value2));
    assertThat(notification.getCause()).isEqualTo(RemovalCause.REPLACED);
    assertThat(listener.isEmpty()).isTrue();

    // inactive
    Object value3 = new Object();
    map.clear();
    listener.clear();
    assertThat(segment.count).isEqualTo(0);
    table.set(index, entry);
    assertThat(segment.storeLoadedValue(key, hash, valueRef, value3)).isTrue();
    assertThat(segment.get(key, hash)).isSameInstanceAs(value3);
    assertThat(segment.count).isEqualTo(1);
    assertThat(listener.isEmpty()).isTrue();

    // replaced
    Object value4 = new Object();
    DummyValueReference<Object, Object> value3Ref = DummyValueReference.create(value3);
    valueRef = new LoadingValueReference<>(value3Ref);
    entry.setValueReference(valueRef);
    table.set(index, entry);
    assertThat(segment.get(key, hash)).isSameInstanceAs(value3);
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.storeLoadedValue(key, hash, valueRef, value4)).isTrue();
    assertThat(segment.get(key, hash)).isSameInstanceAs(value4);
    assertThat(segment.count).isEqualTo(1);
    notification = listener.remove();
    assertThat(notification).isEqualTo(immutableEntry(key, value3));
    assertThat(notification.getCause()).isEqualTo(RemovalCause.REPLACED);
    assertThat(listener.isEmpty()).isTrue();

    // collected
    entry.setValueReference(valueRef);
    table.set(index, entry);
    assertThat(segment.get(key, hash)).isSameInstanceAs(value3);
    assertThat(segment.count).isEqualTo(1);
    value3Ref.clear();
    assertThat(segment.storeLoadedValue(key, hash, valueRef, value4)).isTrue();
    assertThat(segment.get(key, hash)).isSameInstanceAs(value4);
    assertThat(segment.count).isEqualTo(1);
    notification = listener.remove();
    assertThat(notification).isEqualTo(immutableEntry(key, null));
    assertThat(notification.getCause()).isEqualTo(RemovalCause.COLLECTED);
    assertThat(listener.isEmpty()).isTrue();
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
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.remove(key, hash)).isNull();
    assertThat(segment.count).isEqualTo(0);

    // same key
    table.set(index, entry);
    segment.count++;
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);
    assertThat(segment.remove(key, hash)).isSameInstanceAs(oldValue);
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.get(key, hash)).isNull();

    // cleared
    table.set(index, entry);
    segment.count++;
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);
    oldValueRef.clear();
    assertThat(segment.remove(key, hash)).isNull();
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.get(key, hash)).isNull();
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
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.remove(key, hash)).isNull();
    assertThat(segment.count).isEqualTo(0);

    // same value
    table.set(index, entry);
    segment.count++;
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);
    assertThat(segment.remove(key, hash, oldValue)).isTrue();
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.get(key, hash)).isNull();

    // different value
    table.set(index, entry);
    segment.count++;
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);
    assertThat(segment.remove(key, hash, newValue)).isFalse();
    assertThat(segment.count).isEqualTo(1);
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);

    // cleared
    assertThat(segment.get(key, hash)).isSameInstanceAs(oldValue);
    oldValueRef.clear();
    assertThat(segment.remove(key, hash, oldValue)).isFalse();
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.get(key, hash)).isNull();
  }

  public void testExpand() {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object> segment = map.segments[0];
    assertThat(segment.table.length()).isEqualTo(1);

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
    assertThat(originalMap).hasSize(originalCount);
    assertThat(map).isEqualTo(originalMap);

    for (int i = 1; i <= originalCount * 2; i *= 2) {
      if (i > 1) {
        segment.expand();
      }
      assertThat(segment.table.length()).isEqualTo(i);
      assertThat(countLiveEntries(map, 0)).isEqualTo(originalCount);
      assertThat(segment.count).isEqualTo(originalCount);
      assertThat(map).isEqualTo(originalMap);
    }
  }

  public void testGetCausesExpansion() throws ExecutionException {
    for (int count = 1; count <= 100; count++) {
      LocalCache<Object, Object> map =
          makeLocalCache(createCacheBuilder().concurrencyLevel(1).initialCapacity(1));
      Segment<Object, Object> segment = map.segments[0];
      assertThat(segment.table.length()).isEqualTo(1);

      for (int i = 0; i < count; i++) {
        Object key = new Object();
        Object value = new Object();
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
      assertThat(segment.count).isEqualTo(count);
      assertThat(count).isAtMost(segment.threshold);
      assertThat(count).isAtMost((segment.table.length() * 3 / 4));
      assertThat(count).isGreaterThan(segment.table.length() * 3 / 8);
    }
  }

  public void testGetOrDefault() {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).initialCapacity(1));
    map.put(1, 1);
    assertThat(map.getOrDefault(1, 2)).isEqualTo(1);
    assertThat(map.getOrDefault(2, 2)).isEqualTo(2);
  }

  public void testPutCausesExpansion() {
    for (int count = 1; count <= 100; count++) {
      LocalCache<Object, Object> map =
          makeLocalCache(createCacheBuilder().concurrencyLevel(1).initialCapacity(1));
      Segment<Object, Object> segment = map.segments[0];
      assertThat(segment.table.length()).isEqualTo(1);

      for (int i = 0; i < count; i++) {
        Object key = new Object();
        Object value = new Object();
        segment.put(key, key.hashCode(), value, true);
      }
      assertThat(segment.count).isEqualTo(count);
      assertThat(count).isAtMost(segment.threshold);
      assertThat(count).isAtMost((segment.table.length() * 3 / 4));
      assertThat(count).isGreaterThan(segment.table.length() * 3 / 8);
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
    assertThat(table.length()).isEqualTo(1);

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
    assertThat(listener.getCount()).isEqualTo(0);
    assertThat(segment.reclaimKey(entryOne, hashOne)).isFalse();
    assertThat(listener.getCount()).isEqualTo(0);
    table.set(0, entryOne);
    assertThat(segment.reclaimKey(entryTwo, hashTwo)).isFalse();
    assertThat(listener.getCount()).isEqualTo(0);
    table.set(0, entryTwo);
    assertThat(segment.reclaimKey(entryThree, hashThree)).isFalse();
    assertThat(listener.getCount()).isEqualTo(0);

    // present
    table.set(0, entryOne);
    segment.count = 1;
    assertThat(segment.reclaimKey(entryOne, hashOne)).isTrue();
    assertThat(listener.getCount()).isEqualTo(1);
    assertThat(listener.getLastEvictedKey()).isSameInstanceAs(keyOne);
    assertThat(listener.getLastEvictedValue()).isSameInstanceAs(valueOne);
    assertThat(map.removalNotificationQueue.isEmpty()).isTrue();
    assertThat(segment.accessQueue.contains(entryOne)).isFalse();
    assertThat(segment.writeQueue.contains(entryOne)).isFalse();
    assertThat(segment.count).isEqualTo(0);
    assertThat(table.get(0)).isNull();
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
    assertThat(segment.removeEntryFromChain(entryOne, entryOne)).isNull();

    // head
    assertThat(segment.removeEntryFromChain(entryTwo, entryTwo)).isSameInstanceAs(entryOne);

    // middle
    ReferenceEntry<Object, Object> newFirst = segment.removeEntryFromChain(entryThree, entryTwo);
    assertThat(newFirst.getKey()).isSameInstanceAs(keyThree);
    assertThat(newFirst.getValueReference().get()).isSameInstanceAs(valueThree);
    assertThat(newFirst.getHash()).isEqualTo(hashThree);
    assertThat(newFirst.getNext()).isSameInstanceAs(entryOne);

    // tail (remaining entries are copied in reverse order)
    newFirst = segment.removeEntryFromChain(entryThree, entryOne);
    assertThat(newFirst.getKey()).isSameInstanceAs(keyTwo);
    assertThat(newFirst.getValueReference().get()).isSameInstanceAs(valueTwo);
    assertThat(newFirst.getHash()).isEqualTo(hashTwo);
    newFirst = newFirst.getNext();
    assertThat(newFirst.getKey()).isSameInstanceAs(keyThree);
    assertThat(newFirst.getValueReference().get()).isSameInstanceAs(valueThree);
    assertThat(newFirst.getHash()).isEqualTo(hashThree);
    assertThat(newFirst.getNext()).isNull();
  }

  public void testExpand_cleanup() {
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object> segment = map.segments[0];
    assertThat(segment.table.length()).isEqualTo(1);

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
    assertThat(segment.table.length()).isEqualTo(1);
    assertThat(countLiveEntries(map, 0)).isEqualTo(liveCount);
    ImmutableMap<Object, Object> originalMap = ImmutableMap.copyOf(map);
    assertThat(originalMap).hasSize(liveCount);
    // can't compare map contents until cleanup occurs

    for (int i = 1; i <= originalCount * 2; i *= 2) {
      if (i > 1) {
        segment.expand();
      }
      assertThat(segment.table.length()).isEqualTo(i);
      assertThat(countLiveEntries(map, 0)).isEqualTo(liveCount);
      // expansion cleanup is sloppy, with a goal of avoiding unnecessary copies
      assertThat(segment.count).isAtLeast(liveCount);
      assertThat(segment.count).isAtMost(originalCount);
      assertThat(ImmutableMap.copyOf(map)).isEqualTo(originalMap);
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
    assertThat(table.length()).isEqualTo(1);

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = createDummyEntry(key, hash, value, null);
    segment.recordWrite(entry, 1, map.ticker.read());
    segment.table.set(0, entry);
    segment.readCount.incrementAndGet();
    segment.count = 1;
    segment.totalWeight = 1;

    assertThat(table.get(0)).isSameInstanceAs(entry);
    assertThat(segment.accessQueue.peek()).isSameInstanceAs(entry);
    assertThat(segment.writeQueue.peek()).isSameInstanceAs(entry);

    segment.clear();
    assertThat(table.get(0)).isNull();
    assertThat(segment.accessQueue.isEmpty()).isTrue();
    assertThat(segment.writeQueue.isEmpty()).isTrue();
    assertThat(segment.readCount.get()).isEqualTo(0);
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.totalWeight).isEqualTo(0);
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
    assertThat(table.length()).isEqualTo(1);

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = createDummyEntry(key, hash, value, null);
    segment.recordWrite(entry, 1, map.ticker.read());
    segment.table.set(0, entry);
    segment.readCount.incrementAndGet();
    segment.count = 1;
    segment.totalWeight = 1;

    assertThat(table.get(0)).isSameInstanceAs(entry);
    assertThat(segment.accessQueue.peek()).isSameInstanceAs(entry);
    assertThat(segment.writeQueue.peek()).isSameInstanceAs(entry);

    segment.clear();
    assertThat(table.get(0)).isNull();
    assertThat(segment.accessQueue.isEmpty()).isTrue();
    assertThat(segment.writeQueue.isEmpty()).isTrue();
    assertThat(segment.readCount.get()).isEqualTo(0);
    assertThat(segment.count).isEqualTo(0);
    assertThat(segment.totalWeight).isEqualTo(0);
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
    assertThat(table.length()).isEqualTo(1);

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = createDummyEntry(key, hash, value, null);

    // remove absent
    assertThat(segment.removeEntry(entry, hash, RemovalCause.COLLECTED)).isFalse();

    // remove live
    segment.recordWrite(entry, 1, map.ticker.read());
    table.set(0, entry);
    segment.count = 1;
    assertThat(segment.removeEntry(entry, hash, RemovalCause.COLLECTED)).isTrue();
    assertNotificationEnqueued(map, key, value);
    assertThat(map.removalNotificationQueue.isEmpty()).isTrue();
    assertThat(segment.accessQueue.contains(entry)).isFalse();
    assertThat(segment.writeQueue.contains(entry)).isFalse();
    assertThat(segment.count).isEqualTo(0);
    assertThat(table.get(0)).isNull();
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
    assertThat(table.length()).isEqualTo(1);

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> valueRef = DummyValueReference.create(value);
    entry.setValueReference(valueRef);

    // reclaim absent
    assertThat(segment.reclaimValue(key, hash, valueRef)).isFalse();

    // reclaim live
    segment.recordWrite(entry, 1, map.ticker.read());
    table.set(0, entry);
    segment.count = 1;
    assertThat(segment.reclaimValue(key, hash, valueRef)).isTrue();
    assertThat(listener.getCount()).isEqualTo(1);
    assertThat(listener.getLastEvictedKey()).isSameInstanceAs(key);
    assertThat(listener.getLastEvictedValue()).isSameInstanceAs(value);
    assertThat(map.removalNotificationQueue.isEmpty()).isTrue();
    assertThat(segment.accessQueue.contains(entry)).isFalse();
    assertThat(segment.writeQueue.contains(entry)).isFalse();
    assertThat(segment.count).isEqualTo(0);
    assertThat(table.get(0)).isNull();

    // reclaim wrong value reference
    table.set(0, entry);
    DummyValueReference<Object, Object> otherValueRef = DummyValueReference.create(value);
    entry.setValueReference(otherValueRef);
    assertThat(segment.reclaimValue(key, hash, valueRef)).isFalse();
    assertThat(listener.getCount()).isEqualTo(1);
    assertThat(segment.reclaimValue(key, hash, otherValueRef)).isTrue();
    assertThat(listener.getCount()).isEqualTo(2);
    assertThat(listener.getLastEvictedKey()).isSameInstanceAs(key);
    assertThat(listener.getLastEvictedValue()).isSameInstanceAs(value);
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
    assertThat(table.length()).isEqualTo(1);

    Object key = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    LoadingValueReference<Object, Object> valueRef = new LoadingValueReference<>();
    entry.setValueReference(valueRef);

    // absent
    assertThat(segment.removeLoadingValue(key, hash, valueRef)).isFalse();

    // live
    table.set(0, entry);
    // don't increment count; this is used during computation
    assertThat(segment.removeLoadingValue(key, hash, valueRef)).isTrue();
    // no notification sent with removeLoadingValue
    assertThat(map.removalNotificationQueue.isEmpty()).isTrue();
    assertThat(segment.count).isEqualTo(0);
    assertThat(table.get(0)).isNull();

    // active
    Object value = new Object();
    DummyValueReference<Object, Object> previousRef = DummyValueReference.create(value);
    valueRef = new LoadingValueReference<>(previousRef);
    entry.setValueReference(valueRef);
    table.set(0, entry);
    segment.count = 1;
    assertThat(segment.removeLoadingValue(key, hash, valueRef)).isTrue();
    assertThat(table.get(0)).isSameInstanceAs(entry);
    assertThat(segment.get(key, hash)).isSameInstanceAs(value);

    // wrong value reference
    table.set(0, entry);
    DummyValueReference<Object, Object> otherValueRef = DummyValueReference.create(value);
    entry.setValueReference(otherValueRef);
    assertThat(segment.removeLoadingValue(key, hash, valueRef)).isFalse();
    entry.setValueReference(valueRef);
    assertThat(segment.removeLoadingValue(key, hash, valueRef)).isTrue();
  }

  private static <K, V> void assertNotificationEnqueued(LocalCache<K, V> map, K key, V value) {
    RemovalNotification<K, V> notification = map.removalNotificationQueue.poll();
    assertThat(notification.getKey()).isSameInstanceAs(key);
    assertThat(notification.getValue()).isSameInstanceAs(value);
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
        assertThat(segment.recencyQueue.isEmpty()).isTrue();

        for (int i = 0; i < DRAIN_THRESHOLD / 2; i++) {
          map.get(keyOne);
        }
        assertThat(segment.recencyQueue.isEmpty()).isFalse();

        map.put(keyTwo, valueTwo);
        assertThat(segment.recencyQueue.isEmpty()).isTrue();
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
        assertThat(segment.recencyQueue.isEmpty()).isTrue();

        for (int i = 0; i < DRAIN_THRESHOLD / 2; i++) {
          map.get(keyOne);
        }
        assertThat(segment.recencyQueue.isEmpty()).isFalse();

        for (int i = 0; i < DRAIN_THRESHOLD * 2; i++) {
          map.get(keyOne);
          assertThat(segment.recencyQueue.size()).isAtMost(DRAIN_THRESHOLD);
        }

        // get over many different keys

        for (int i = 0; i < DRAIN_THRESHOLD * 2; i++) {
          map.put(new Object(), new Object());
        }
        assertThat(segment.recencyQueue.isEmpty()).isTrue();

        for (int i = 0; i < DRAIN_THRESHOLD / 2; i++) {
          map.get(keyOne);
        }
        assertThat(segment.recencyQueue.isEmpty()).isFalse();

        for (Object key : map.keySet()) {
          map.get(key);
          assertThat(segment.recencyQueue.size()).isAtMost(DRAIN_THRESHOLD);
        }
      }
    }
  }

  public void testRecordRead() {
    for (CacheBuilder<Object, Object> builder : allEvictingMakers()) {
      LocalCache<Object, Object> map = makeLocalCache(builder.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];
      List<ReferenceEntry<Object, Object>> writeOrder = new LinkedList<>();
      List<ReferenceEntry<Object, Object>> readOrder = new LinkedList<>();
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
      List<ReferenceEntry<Object, Object>> reads = new ArrayList<>();
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
      List<ReferenceEntry<Object, Object>> writeOrder = new LinkedList<>();
      List<ReferenceEntry<Object, Object>> readOrder = new LinkedList<>();
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
      assertThat(segment.recencyQueue.isEmpty()).isTrue();

      // access some of the elements
      Random random = new Random();
      List<ReferenceEntry<Object, Object>> reads = new ArrayList<>();
      Iterator<ReferenceEntry<Object, Object>> i = readOrder.iterator();
      while (i.hasNext()) {
        ReferenceEntry<Object, Object> entry = i.next();
        if (random.nextBoolean()) {
          map.get(entry.getKey());
          reads.add(entry);
          i.remove();
          assertThat(segment.recencyQueue.size()).isAtMost(DRAIN_THRESHOLD);
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
      List<ReferenceEntry<Object, Object>> writeOrder = new LinkedList<>();
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
      List<ReferenceEntry<Object, Object>> writes = new ArrayList<>();
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
    assertThat(actualEntries).hasSize(size);
    for (int i = 0; i < size; i++) {
      ReferenceEntry<K, V> expectedEntry = expectedEntries.get(i);
      ReferenceEntry<K, V> actualEntry = actualEntries.get(i);
      assertThat(actualEntry.getKey()).isSameInstanceAs(expectedEntry.getKey());
      assertThat(actualEntry.getValueReference().get())
          .isSameInstanceAs(expectedEntry.getValueReference().get());
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
        assertThat(accessTime).isAtLeast(lastAccessTime);
        lastAccessTime = accessTime;
        long writeTime = e.getWriteTime();
        assertThat(writeTime).isAtLeast(lastWriteTime);
        lastWriteTime = writeTime;
      }

      lastAccessTime = 0;
      lastWriteTime = 0;
      for (ReferenceEntry<K, V> e : segment.accessQueue) {
        long accessTime = e.getAccessTime();
        assertThat(accessTime).isAtLeast(lastAccessTime);
        lastAccessTime = accessTime;
      }
      for (ReferenceEntry<K, V> e : segment.writeQueue) {
        long writeTime = e.getWriteTime();
        assertThat(writeTime).isAtLeast(lastWriteTime);
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
                .expireAfterWrite(2, NANOSECONDS));
    Segment<Object, Object> segment = map.segments[0];

    Object key = new Object();
    Object value = new Object();
    map.put(key, value);
    ReferenceEntry<Object, Object> entry = map.getEntry(key);
    assertThat(map.isLive(entry, ticker.read())).isTrue();

    segment.writeQueue.add(entry);
    assertThat(map.get(key)).isSameInstanceAs(value);
    assertThat(segment.writeQueue.peek()).isSameInstanceAs(entry);
    assertThat(segment.writeQueue).hasSize(1);

    segment.recordRead(entry, ticker.read());
    segment.expireEntries(ticker.read());
    assertThat(map.get(key)).isSameInstanceAs(value);
    assertThat(segment.writeQueue.peek()).isSameInstanceAs(entry);
    assertThat(segment.writeQueue).hasSize(1);

    ticker.advance(1);
    segment.recordRead(entry, ticker.read());
    segment.expireEntries(ticker.read());
    assertThat(map.get(key)).isSameInstanceAs(value);
    assertThat(segment.writeQueue.peek()).isSameInstanceAs(entry);
    assertThat(segment.writeQueue).hasSize(1);

    ticker.advance(1);
    assertThat(map.get(key)).isNull();
    segment.expireEntries(ticker.read());
    assertThat(map.get(key)).isNull();
    assertThat(segment.writeQueue.isEmpty()).isTrue();
  }

  public void testExpireAfterAccess() {
    FakeTicker ticker = new FakeTicker();
    LocalCache<Object, Object> map =
        makeLocalCache(
            createCacheBuilder()
                .concurrencyLevel(1)
                .ticker(ticker)
                .expireAfterAccess(2, NANOSECONDS));
    Segment<Object, Object> segment = map.segments[0];

    Object key = new Object();
    Object value = new Object();
    map.put(key, value);
    ReferenceEntry<Object, Object> entry = map.getEntry(key);
    assertThat(map.isLive(entry, ticker.read())).isTrue();

    segment.accessQueue.add(entry);
    assertThat(map.get(key)).isSameInstanceAs(value);
    assertThat(segment.accessQueue.peek()).isSameInstanceAs(entry);
    assertThat(segment.accessQueue).hasSize(1);

    segment.recordRead(entry, ticker.read());
    segment.expireEntries(ticker.read());
    assertThat(map.containsKey(key)).isTrue();
    assertThat(segment.accessQueue.peek()).isSameInstanceAs(entry);
    assertThat(segment.accessQueue).hasSize(1);

    ticker.advance(1);
    segment.recordRead(entry, ticker.read());
    segment.expireEntries(ticker.read());
    assertThat(map.containsKey(key)).isTrue();
    assertThat(segment.accessQueue.peek()).isSameInstanceAs(entry);
    assertThat(segment.accessQueue).hasSize(1);

    ticker.advance(1);
    segment.recordRead(entry, ticker.read());
    segment.expireEntries(ticker.read());
    assertThat(map.containsKey(key)).isTrue();
    assertThat(segment.accessQueue.peek()).isSameInstanceAs(entry);
    assertThat(segment.accessQueue).hasSize(1);

    ticker.advance(1);
    segment.expireEntries(ticker.read());
    assertThat(map.containsKey(key)).isTrue();
    assertThat(segment.accessQueue.peek()).isSameInstanceAs(entry);
    assertThat(segment.accessQueue).hasSize(1);

    ticker.advance(1);
    assertThat(map.containsKey(key)).isFalse();
    assertThat(map.get(key)).isNull();
    segment.expireEntries(ticker.read());
    assertThat(map.containsKey(key)).isFalse();
    assertThat(map.get(key)).isNull();
    assertThat(segment.accessQueue.isEmpty()).isTrue();
  }

  public void testEvictEntries() {
    int maxSize = 10;
    LocalCache<Object, Object> map =
        makeLocalCache(createCacheBuilder().concurrencyLevel(1).maximumSize(maxSize));
    Segment<Object, Object> segment = map.segments[0];

    // manually add elements to avoid eviction
    int originalCount = 1024;
    ReferenceEntry<Object, Object> entry = null;
    LinkedHashMap<Object, Object> originalMap = new LinkedHashMap<>();
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
    assertThat(map).hasSize(originalCount);
    assertThat(map).isEqualTo(originalMap);

    Iterator<Object> it = originalMap.keySet().iterator();
    for (int i = 0; i < originalCount - maxSize; i++) {
      it.next();
      it.remove();
    }
    segment.evictEntries(entry);
    assertThat(map).hasSize(maxSize);
    assertThat(map).isEqualTo(originalMap);
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
        assertThat(map.containsKey(keyOne)).isFalse();
        assertThat(map.containsValue(valueOne)).isFalse();
        assertThat(map.get(keyOne)).isNull();
        assertThat(map).hasSize(1);
        assertThat(segment.keyReferenceQueue.poll()).isNull();
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
        assertThat(map.containsKey(keyOne)).isFalse();
        assertThat(map.containsValue(valueOne)).isFalse();
        assertThat(map.get(keyOne)).isNull();
        assertThat(map).hasSize(1);
        assertThat(segment.valueReferenceQueue.poll()).isNull();
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
        assertThat(map.containsKey(keyOne)).isFalse();
        assertThat(map.containsValue(valueOne)).isFalse();
        assertThat(map.get(keyOne)).isNull();
        assertThat(map).isEmpty();
        assertThat(segment.keyReferenceQueue.poll()).isNull();
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
        assertThat(map.containsKey(keyOne)).isFalse();
        assertThat(map.containsValue(valueOne)).isFalse();
        assertThat(map.get(keyOne)).isNull();
        assertThat(map).isEmpty();
        assertThat(segment.valueReferenceQueue.poll()).isNull();
      }
    }
  }

  public void testNullParameters() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(makeLocalCache(createCacheBuilder()));
    CacheLoader<Object, Object> loader = identityLoader();
    tester.testAllPublicInstanceMethods(makeLocalCache(createCacheBuilder(), loader));
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
    assertThat(one.size()).isEqualTo(1);
    assertThat(one.asMap().isEmpty()).isFalse();
    LocalLoadingCache<Object, Object> two = SerializableTester.reserialize(one);
    assertThat(two.size()).isEqualTo(0);
    assertThat(two.asMap().isEmpty()).isTrue();

    LocalCache<Object, Object> localCacheOne = one.localCache;
    LocalCache<Object, Object> localCacheTwo = two.localCache;

    assertThat(localCacheTwo.keyStrength).isEqualTo(localCacheOne.keyStrength);
    assertThat(localCacheTwo.keyStrength).isEqualTo(localCacheOne.keyStrength);
    assertThat(localCacheTwo.valueEquivalence).isEqualTo(localCacheOne.valueEquivalence);
    assertThat(localCacheTwo.valueEquivalence).isEqualTo(localCacheOne.valueEquivalence);
    assertThat(localCacheTwo.maxWeight).isEqualTo(localCacheOne.maxWeight);
    assertThat(localCacheTwo.weigher).isEqualTo(localCacheOne.weigher);
    assertThat(localCacheTwo.expireAfterAccessNanos)
        .isEqualTo(localCacheOne.expireAfterAccessNanos);
    assertThat(localCacheTwo.expireAfterWriteNanos).isEqualTo(localCacheOne.expireAfterWriteNanos);
    assertThat(localCacheTwo.refreshNanos).isEqualTo(localCacheOne.refreshNanos);
    assertThat(localCacheTwo.removalListener).isEqualTo(localCacheOne.removalListener);
    assertThat(localCacheTwo.ticker).isEqualTo(localCacheOne.ticker);

    // serialize the reconstituted version to be sure we haven't lost the ability to reserialize
    LocalLoadingCache<Object, Object> three = SerializableTester.reserialize(two);
    LocalCache<Object, Object> localCacheThree = three.localCache;

    assertThat(localCacheThree.defaultLoader).isEqualTo(localCacheTwo.defaultLoader);
    assertThat(localCacheThree.keyStrength).isEqualTo(localCacheTwo.keyStrength);
    assertThat(localCacheThree.keyStrength).isEqualTo(localCacheTwo.keyStrength);
    assertThat(localCacheThree.valueEquivalence).isEqualTo(localCacheTwo.valueEquivalence);
    assertThat(localCacheThree.valueEquivalence).isEqualTo(localCacheTwo.valueEquivalence);
    assertThat(localCacheThree.maxWeight).isEqualTo(localCacheTwo.maxWeight);
    assertThat(localCacheThree.weigher).isEqualTo(localCacheTwo.weigher);
    assertThat(localCacheThree.expireAfterAccessNanos)
        .isEqualTo(localCacheTwo.expireAfterAccessNanos);
    assertThat(localCacheThree.expireAfterWriteNanos)
        .isEqualTo(localCacheTwo.expireAfterWriteNanos);
    assertThat(localCacheThree.removalListener).isEqualTo(localCacheTwo.removalListener);
    assertThat(localCacheThree.ticker).isEqualTo(localCacheTwo.ticker);
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
    assertThat(one.size()).isEqualTo(1);
    assertThat(one.asMap().isEmpty()).isFalse();
    LocalManualCache<Object, Object> two = SerializableTester.reserialize(one);
    assertThat(two.size()).isEqualTo(0);
    assertThat(two.asMap().isEmpty()).isTrue();

    LocalCache<Object, Object> localCacheOne = one.localCache;
    LocalCache<Object, Object> localCacheTwo = two.localCache;

    assertThat(localCacheTwo.keyStrength).isEqualTo(localCacheOne.keyStrength);
    assertThat(localCacheTwo.keyStrength).isEqualTo(localCacheOne.keyStrength);
    assertThat(localCacheTwo.valueEquivalence).isEqualTo(localCacheOne.valueEquivalence);
    assertThat(localCacheTwo.valueEquivalence).isEqualTo(localCacheOne.valueEquivalence);
    assertThat(localCacheTwo.maxWeight).isEqualTo(localCacheOne.maxWeight);
    assertThat(localCacheTwo.weigher).isEqualTo(localCacheOne.weigher);
    assertThat(localCacheTwo.expireAfterAccessNanos)
        .isEqualTo(localCacheOne.expireAfterAccessNanos);
    assertThat(localCacheTwo.expireAfterWriteNanos).isEqualTo(localCacheOne.expireAfterWriteNanos);
    assertThat(localCacheTwo.removalListener).isEqualTo(localCacheOne.removalListener);
    assertThat(localCacheTwo.ticker).isEqualTo(localCacheOne.ticker);

    // serialize the reconstituted version to be sure we haven't lost the ability to reserialize
    LocalManualCache<Object, Object> three = SerializableTester.reserialize(two);
    LocalCache<Object, Object> localCacheThree = three.localCache;

    assertThat(localCacheThree.keyStrength).isEqualTo(localCacheTwo.keyStrength);
    assertThat(localCacheThree.keyStrength).isEqualTo(localCacheTwo.keyStrength);
    assertThat(localCacheThree.valueEquivalence).isEqualTo(localCacheTwo.valueEquivalence);
    assertThat(localCacheThree.valueEquivalence).isEqualTo(localCacheTwo.valueEquivalence);
    assertThat(localCacheThree.maxWeight).isEqualTo(localCacheTwo.maxWeight);
    assertThat(localCacheThree.weigher).isEqualTo(localCacheTwo.weigher);
    assertThat(localCacheThree.expireAfterAccessNanos)
        .isEqualTo(localCacheTwo.expireAfterAccessNanos);
    assertThat(localCacheThree.expireAfterWriteNanos)
        .isEqualTo(localCacheTwo.expireAfterWriteNanos);
    assertThat(localCacheThree.removalListener).isEqualTo(localCacheTwo.removalListener);
    assertThat(localCacheThree.ticker).isEqualTo(localCacheTwo.ticker);
  }

  public void testLoadDifferentKeyInLoader() throws ExecutionException, InterruptedException {
    LocalCache<String, String> cache = makeLocalCache(createCacheBuilder());
    String key1 = "key1";
    String key2 = "key2";

    assertThat(
            cache.get(
                key1,
                new CacheLoader<String, String>() {
                  @Override
                  public String load(String key) throws Exception {
                    return cache.get(key2, identityLoader()); // loads a different key, should work
                  }
                }))
        .isEqualTo(key2);
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

    boolean done = doneSignal.await(1, SECONDS);
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
  private static Iterable<CacheBuilder<Object, Object>> allEntryTypeMakers() {
    List<CacheBuilder<Object, Object>> result = new ArrayList<>();
    Iterables.addAll(result, allKeyValueStrengthMakers());
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
  static Iterable<CacheBuilder<Object, Object>> allEvictingMakers() {
    return ImmutableList.of(
        createCacheBuilder().maximumSize(SMALL_MAX_SIZE),
        createCacheBuilder().expireAfterAccess(99999, SECONDS),
        createCacheBuilder().expireAfterWrite(99999, SECONDS),
        createCacheBuilder().maximumSize(SMALL_MAX_SIZE).expireAfterAccess(SMALL_MAX_SIZE, SECONDS),
        createCacheBuilder().maximumSize(SMALL_MAX_SIZE).expireAfterWrite(SMALL_MAX_SIZE, SECONDS));
  }

  /** Returns an iterable containing all combinations weakKeys and weak/softValues. */
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
      return o instanceof SerializableCacheLoader;
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
      return o instanceof SerializableRemovalListener;
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
      return o instanceof SerializableTicker;
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
      return o instanceof SerializableWeigher;
    }
  }
}
