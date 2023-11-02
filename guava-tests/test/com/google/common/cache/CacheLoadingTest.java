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

import static com.google.common.cache.TestingCacheLoaders.bulkLoader;
import static com.google.common.cache.TestingCacheLoaders.constantLoader;
import static com.google.common.cache.TestingCacheLoaders.errorLoader;
import static com.google.common.cache.TestingCacheLoaders.exceptionLoader;
import static com.google.common.cache.TestingCacheLoaders.identityLoader;
import static com.google.common.cache.TestingRemovalListeners.countingRemovalListener;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.TestingCacheLoaders.CountingLoader;
import com.google.common.cache.TestingCacheLoaders.IdentityLoader;
import com.google.common.cache.TestingRemovalListeners.CountingRemovalListener;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.testing.FakeTicker;
import com.google.common.testing.TestLogHandler;
import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.LogRecord;
import junit.framework.TestCase;

/**
 * Tests relating to cache loading: concurrent loading, exceptions during loading, etc.
 *
 * @author mike nonemacher
 */
public class CacheLoadingTest extends TestCase {
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
    // TODO(cpovirk): run tests in other thread instead of messing with main thread interrupt status
    currentThread().interrupted();
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
    assertThat(logHandler.getStoredLogRecords()).isEmpty();
  }

  private void checkLoggedCause(Throwable t) {
    assertThat(popLoggedThrowable()).hasCauseThat().isSameInstanceAs(t);
  }

  private void checkLoggedInvalidLoad() {
    assertThat(popLoggedThrowable()).isInstanceOf(InvalidCacheLoadException.class);
  }

  public void testLoad() throws ExecutionException {
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(identityLoader());
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    Object key = new Object();
    assertSame(key, cache.get(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    key = new Object();
    assertSame(key, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(2, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    key = new Object();
    cache.refresh(key);
    checkNothingLogged();
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(3, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(key, cache.get(key));
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(3, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());

    Object value = new Object();
    // callable is not called
    assertSame(key, cache.get(key, throwing(new Exception())));
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(3, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(2, stats.hitCount());

    key = new Object();
    assertSame(value, cache.get(key, Callables.returning(value)));
    stats = cache.stats();
    assertEquals(3, stats.missCount());
    assertEquals(4, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(2, stats.hitCount());
  }

  public void testReload() throws ExecutionException {
    final Object one = new Object();
    final Object two = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return Futures.immediateFuture(two);
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(key);
    checkNothingLogged();
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(2, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(two, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(2, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());
  }

  public void testRefresh() {
    final Object one = new Object();
    final Object two = new Object();
    FakeTicker ticker = new FakeTicker();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return Futures.immediateFuture(two);
          }
        };

    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder()
            .recordStats()
            .ticker(ticker)
            .refreshAfterWrite(1, MILLISECONDS)
            .build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(two, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(2, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(2, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(two, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(2, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(3, stats.hitCount());
  }

  public void testRefresh_getIfPresent() {
    final Object one = new Object();
    final Object two = new Object();
    FakeTicker ticker = new FakeTicker();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return Futures.immediateFuture(two);
          }
        };

    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder()
            .recordStats()
            .ticker(ticker)
            .refreshAfterWrite(1, MILLISECONDS)
            .build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getIfPresent(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(two, cache.getIfPresent(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(2, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(2, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(two, cache.getIfPresent(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(2, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(3, stats.hitCount());
  }

  public void testBulkLoad_default() throws ExecutionException {
    LoadingCache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .recordStats()
            .build(TestingCacheLoaders.<Integer>identityLoader());
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertEquals(ImmutableMap.of(), cache.getAll(ImmutableList.<Integer>of()));
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertEquals(ImmutableMap.of(1, 1), cache.getAll(asList(1)));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertEquals(ImmutableMap.of(1, 1, 2, 2, 3, 3, 4, 4), cache.getAll(asList(1, 2, 3, 4)));
    stats = cache.stats();
    assertEquals(4, stats.missCount());
    assertEquals(4, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());

    assertEquals(ImmutableMap.of(2, 2, 3, 3), cache.getAll(asList(2, 3)));
    stats = cache.stats();
    assertEquals(4, stats.missCount());
    assertEquals(4, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(3, stats.hitCount());

    // duplicate keys are ignored, and don't impact stats
    assertEquals(ImmutableMap.of(4, 4, 5, 5), cache.getAll(asList(4, 5)));
    stats = cache.stats();
    assertEquals(5, stats.missCount());
    assertEquals(5, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(4, stats.hitCount());
  }

  public void testBulkLoad_loadAll() throws ExecutionException {
    IdentityLoader<Integer> backingLoader = identityLoader();
    CacheLoader<Integer, Integer> loader = bulkLoader(backingLoader);
    LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertEquals(ImmutableMap.of(), cache.getAll(ImmutableList.<Integer>of()));
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertEquals(ImmutableMap.of(1, 1), cache.getAll(asList(1)));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertEquals(ImmutableMap.of(1, 1, 2, 2, 3, 3, 4, 4), cache.getAll(asList(1, 2, 3, 4)));
    stats = cache.stats();
    assertEquals(4, stats.missCount());
    assertEquals(2, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());

    assertEquals(ImmutableMap.of(2, 2, 3, 3), cache.getAll(asList(2, 3)));
    stats = cache.stats();
    assertEquals(4, stats.missCount());
    assertEquals(2, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(3, stats.hitCount());

    // duplicate keys are ignored, and don't impact stats
    assertEquals(ImmutableMap.of(4, 4, 5, 5), cache.getAll(asList(4, 5)));
    stats = cache.stats();
    assertEquals(5, stats.missCount());
    assertEquals(3, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(4, stats.hitCount());
  }

  public void testBulkLoad_extra() throws ExecutionException {
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) throws Exception {
            return new Object();
          }

          @Override
          public Map<Object, Object> loadAll(Iterable<?> keys) throws Exception {
            Map<Object, Object> result = Maps.newHashMap();
            for (Object key : keys) {
              Object value = new Object();
              result.put(key, value);
              // add extra entries
              result.put(value, key);
            }
            return result;
          }
        };
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().build(loader);

    Object[] lookupKeys = new Object[] {new Object(), new Object(), new Object()};
    Map<Object, Object> result = cache.getAll(asList(lookupKeys));
    assertThat(result.keySet()).containsExactlyElementsIn(asList(lookupKeys));
    for (Entry<Object, Object> entry : result.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();
      assertSame(value, result.get(key));
      assertNull(result.get(value));
      assertSame(value, cache.asMap().get(key));
      assertSame(key, cache.asMap().get(value));
    }
  }

  public void testBulkLoad_clobber() throws ExecutionException {
    final Object extraKey = new Object();
    final Object extraValue = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) throws Exception {
            throw new AssertionError();
          }

          @Override
          public Map<Object, Object> loadAll(Iterable<?> keys) throws Exception {
            Map<Object, Object> result = Maps.newHashMap();
            for (Object key : keys) {
              Object value = new Object();
              result.put(key, value);
            }
            result.put(extraKey, extraValue);
            return result;
          }
        };
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().build(loader);
    cache.asMap().put(extraKey, extraKey);
    assertSame(extraKey, cache.asMap().get(extraKey));

    Object[] lookupKeys = new Object[] {new Object(), new Object(), new Object()};
    Map<Object, Object> result = cache.getAll(asList(lookupKeys));
    assertThat(result.keySet()).containsExactlyElementsIn(asList(lookupKeys));
    for (Entry<Object, Object> entry : result.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();
      assertSame(value, result.get(key));
      assertSame(value, cache.asMap().get(key));
    }
    assertNull(result.get(extraKey));
    assertSame(extraValue, cache.asMap().get(extraKey));
  }

  public void testBulkLoad_clobberNullValue() throws ExecutionException {
    final Object extraKey = new Object();
    final Object extraValue = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) throws Exception {
            throw new AssertionError();
          }

          @Override
          public Map<Object, Object> loadAll(Iterable<?> keys) throws Exception {
            Map<Object, Object> result = Maps.newHashMap();
            for (Object key : keys) {
              Object value = new Object();
              result.put(key, value);
            }
            result.put(extraKey, extraValue);
            result.put(extraValue, null);
            return result;
          }
        };
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().build(loader);
    cache.asMap().put(extraKey, extraKey);
    assertSame(extraKey, cache.asMap().get(extraKey));

    Object[] lookupKeys = new Object[] {new Object(), new Object(), new Object()};
    assertThrows(InvalidCacheLoadException.class, () -> cache.getAll(asList(lookupKeys)));

    for (Object key : lookupKeys) {
      assertTrue(cache.asMap().containsKey(key));
    }
    assertSame(extraValue, cache.asMap().get(extraKey));
    assertFalse(cache.asMap().containsKey(extraValue));
  }

  public void testBulkLoad_clobberNullKey() throws ExecutionException {
    final Object extraKey = new Object();
    final Object extraValue = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) throws Exception {
            throw new AssertionError();
          }

          @Override
          public Map<Object, Object> loadAll(Iterable<?> keys) throws Exception {
            Map<Object, Object> result = Maps.newHashMap();
            for (Object key : keys) {
              Object value = new Object();
              result.put(key, value);
            }
            result.put(extraKey, extraValue);
            result.put(null, extraKey);
            return result;
          }
        };
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().build(loader);
    cache.asMap().put(extraKey, extraKey);
    assertSame(extraKey, cache.asMap().get(extraKey));

    Object[] lookupKeys = new Object[] {new Object(), new Object(), new Object()};
    assertThrows(InvalidCacheLoadException.class, () -> cache.getAll(asList(lookupKeys)));

    for (Object key : lookupKeys) {
      assertTrue(cache.asMap().containsKey(key));
    }
    assertSame(extraValue, cache.asMap().get(extraKey));
    assertFalse(cache.asMap().containsValue(extraKey));
  }

  public void testBulkLoad_partial() throws ExecutionException {
    final Object extraKey = new Object();
    final Object extraValue = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) throws Exception {
            throw new AssertionError();
          }

          @Override
          public Map<Object, Object> loadAll(Iterable<?> keys) throws Exception {
            Map<Object, Object> result = Maps.newHashMap();
            // ignore request keys
            result.put(extraKey, extraValue);
            return result;
          }
        };
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().build(loader);

    Object[] lookupKeys = new Object[] {new Object(), new Object(), new Object()};
    assertThrows(InvalidCacheLoadException.class, () -> cache.getAll(asList(lookupKeys)));
    assertSame(extraValue, cache.asMap().get(extraKey));
  }

  public void testLoadNull() throws ExecutionException {
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(constantLoader(null));
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertThrows(InvalidCacheLoadException.class, () -> cache.get(new Object()));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertThrows(InvalidCacheLoadException.class, () -> cache.getUnchecked(new Object()));
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(2, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(new Object());
    checkLoggedInvalidLoad();
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(3, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertThrows(
        InvalidCacheLoadException.class, () -> cache.get(new Object(), Callables.returning(null)));
    stats = cache.stats();
    assertEquals(3, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(4, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertThrows(InvalidCacheLoadException.class, () -> cache.getAll(asList(new Object())));
    stats = cache.stats();
    assertEquals(4, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(5, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
  }

  public void testReloadNull() throws ExecutionException {
    final Object one = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return null;
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(key);
    checkLoggedInvalidLoad();
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());
  }

  public void testReloadNullFuture() throws ExecutionException {
    final Object one = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return Futures.immediateFuture(null);
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(key);
    checkLoggedInvalidLoad();
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());
  }

  public void testRefreshNull() {
    final Object one = new Object();
    FakeTicker ticker = new FakeTicker();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return Futures.immediateFuture(null);
          }
        };

    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder()
            .recordStats()
            .ticker(ticker)
            .refreshAfterWrite(1, MILLISECONDS)
            .build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    // refreshed
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(2, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(2, stats.loadExceptionCount());
    assertEquals(3, stats.hitCount());
  }

  public void testBulkLoadNull() throws ExecutionException {
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(bulkLoader(constantLoader(null)));
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertThrows(InvalidCacheLoadException.class, () -> cache.getAll(asList(new Object())));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
  }

  public void testBulkLoadNullMap() throws ExecutionException {
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder()
            .recordStats()
            .build(
                new CacheLoader<Object, Object>() {
                  @Override
                  public Object load(Object key) {
                    throw new AssertionError();
                  }

                  @Override
                  public Map<Object, Object> loadAll(Iterable<?> keys) {
                    return null;
                  }
                });

    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertThrows(InvalidCacheLoadException.class, () -> cache.getAll(asList(new Object())));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
  }

  public void testLoadError() throws ExecutionException {
    Error e = new Error();
    CacheLoader<Object, Object> loader = errorLoader(e);
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    ExecutionError expected = assertThrows(ExecutionError.class, () -> cache.get(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    expected = assertThrows(ExecutionError.class, () -> cache.getUnchecked(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(2, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(new Object());
    checkLoggedCause(e);
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(3, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    final Error callableError = new Error();
    expected =
        assertThrows(
            ExecutionError.class,
            () ->
                cache.get(
                    new Object(),
                    new Callable<Object>() {
                      @Override
                      public Object call() {
                        throw callableError;
                      }
                    }));
    assertThat(expected).hasCauseThat().isSameInstanceAs(callableError);
    stats = cache.stats();
    assertEquals(3, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(4, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    expected = assertThrows(ExecutionError.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertEquals(4, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(5, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
  }

  public void testReloadError() throws ExecutionException {
    final Object one = new Object();
    final Error e = new Error();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            throw e;
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(key);
    checkLoggedCause(e);
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());
  }

  public void testReloadFutureError() throws ExecutionException {
    final Object one = new Object();
    final Error e = new Error();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return Futures.immediateFailedFuture(e);
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(key);
    checkLoggedCause(e);
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());
  }

  public void testRefreshError() {
    final Object one = new Object();
    final Error e = new Error();
    FakeTicker ticker = new FakeTicker();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return Futures.immediateFailedFuture(e);
          }
        };

    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder()
            .recordStats()
            .ticker(ticker)
            .refreshAfterWrite(1, MILLISECONDS)
            .build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    // refreshed
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(2, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(2, stats.loadExceptionCount());
    assertEquals(3, stats.hitCount());
  }

  public void testBulkLoadError() throws ExecutionException {
    Error e = new Error();
    CacheLoader<Object, Object> loader = errorLoader(e);
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(bulkLoader(loader));
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    ExecutionError expected =
        assertThrows(ExecutionError.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
  }

  public void testLoadCheckedException() {
    Exception e = new Exception();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    Exception expected = assertThrows(ExecutionException.class, () -> cache.get(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    expected =
        assertThrows(UncheckedExecutionException.class, () -> cache.getUnchecked(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(2, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(new Object());
    checkLoggedCause(e);
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(3, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    Exception callableException = new Exception();
    expected =
        assertThrows(
            ExecutionException.class, () -> cache.get(new Object(), throwing(callableException)));
    assertThat(expected).hasCauseThat().isSameInstanceAs(callableException);
    stats = cache.stats();
    assertEquals(3, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(4, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    expected = assertThrows(ExecutionException.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertEquals(4, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(5, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
  }

  public void testLoadInterruptedException() {
    Exception e = new InterruptedException();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    // Sanity check:
    assertFalse(currentThread().interrupted());

    Exception expected = assertThrows(ExecutionException.class, () -> cache.get(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    assertTrue(currentThread().interrupted());
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    expected =
        assertThrows(UncheckedExecutionException.class, () -> cache.getUnchecked(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    assertTrue(currentThread().interrupted());
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(2, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(new Object());
    assertTrue(currentThread().interrupted());
    checkLoggedCause(e);
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(3, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    Exception callableException = new InterruptedException();
    expected =
        assertThrows(
            ExecutionException.class, () -> cache.get(new Object(), throwing(callableException)));
    assertThat(expected).hasCauseThat().isSameInstanceAs(callableException);
    assertTrue(currentThread().interrupted());
    stats = cache.stats();
    assertEquals(3, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(4, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    expected = assertThrows(ExecutionException.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    assertTrue(currentThread().interrupted());
    stats = cache.stats();
    assertEquals(4, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(5, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
  }

  public void testReloadCheckedException() {
    final Object one = new Object();
    final Exception e = new Exception();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) throws Exception {
            throw e;
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(key);
    checkLoggedCause(e);
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());
  }

  public void testReloadFutureCheckedException() {
    final Object one = new Object();
    final Exception e = new Exception();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return Futures.immediateFailedFuture(e);
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(key);
    checkLoggedCause(e);
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());
  }

  public void testRefreshCheckedException() {
    final Object one = new Object();
    final Exception e = new Exception();
    FakeTicker ticker = new FakeTicker();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return Futures.immediateFailedFuture(e);
          }
        };

    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder()
            .recordStats()
            .ticker(ticker)
            .refreshAfterWrite(1, MILLISECONDS)
            .build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    // refreshed
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(2, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(2, stats.loadExceptionCount());
    assertEquals(3, stats.hitCount());
  }

  public void testBulkLoadCheckedException() {
    Exception e = new Exception();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(bulkLoader(loader));
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
  }

  public void testBulkLoadInterruptedException() {
    Exception e = new InterruptedException();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(bulkLoader(loader));
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    assertTrue(currentThread().interrupted());
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
  }

  public void testLoadUncheckedException() throws ExecutionException {
    Exception e = new RuntimeException();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    UncheckedExecutionException expected =
        assertThrows(UncheckedExecutionException.class, () -> cache.get(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    expected =
        assertThrows(UncheckedExecutionException.class, () -> cache.getUnchecked(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(2, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(new Object());
    checkLoggedCause(e);
    stats = cache.stats();
    assertEquals(2, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(3, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    Exception callableException = new RuntimeException();
    expected =
        assertThrows(
            UncheckedExecutionException.class,
            () -> cache.get(new Object(), throwing(callableException)));
    assertThat(expected).hasCauseThat().isSameInstanceAs(callableException);
    stats = cache.stats();
    assertEquals(3, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(4, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    expected =
        assertThrows(UncheckedExecutionException.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertEquals(4, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(5, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
  }

  public void testReloadUncheckedException() throws ExecutionException {
    final Object one = new Object();
    final Exception e = new RuntimeException();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) throws Exception {
            throw e;
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(key);
    checkLoggedCause(e);
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());
  }

  public void testReloadFutureUncheckedException() throws ExecutionException {
    final Object one = new Object();
    final Exception e = new RuntimeException();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return Futures.immediateFailedFuture(e);
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    cache.refresh(key);
    checkLoggedCause(e);
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());
  }

  public void testRefreshUncheckedException() {
    final Object one = new Object();
    final Exception e = new RuntimeException();
    FakeTicker ticker = new FakeTicker();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return Futures.immediateFailedFuture(e);
          }
        };

    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder()
            .recordStats()
            .ticker(ticker)
            .refreshAfterWrite(1, MILLISECONDS)
            .build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(1, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    // refreshed
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(2, stats.hitCount());

    ticker.advance(1, MILLISECONDS);
    assertSame(one, cache.getUnchecked(key));
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(2, stats.loadExceptionCount());
    assertEquals(3, stats.hitCount());
  }

  public void testBulkLoadUncheckedException() throws ExecutionException {
    Exception e = new RuntimeException();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(bulkLoader(loader));
    CacheStats stats = cache.stats();
    assertEquals(0, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(0, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());

    UncheckedExecutionException expected =
        assertThrows(UncheckedExecutionException.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertEquals(1, stats.missCount());
    assertEquals(0, stats.loadSuccessCount());
    assertEquals(1, stats.loadExceptionCount());
    assertEquals(0, stats.hitCount());
  }

  public void testReloadAfterFailure() throws ExecutionException {
    final AtomicInteger count = new AtomicInteger();
    final Exception e = new IllegalStateException("exception to trigger failure on first load()");
    CacheLoader<Integer, String> failOnceFunction =
        new CacheLoader<Integer, String>() {

          @Override
          public String load(Integer key) throws Exception {
            if (count.getAndIncrement() == 0) {
              throw e;
            }
            return key.toString();
          }
        };
    CountingRemovalListener<Integer, String> removalListener = countingRemovalListener();
    LoadingCache<Integer, String> cache =
        CacheBuilder.newBuilder().removalListener(removalListener).build(failOnceFunction);

    UncheckedExecutionException ue =
        assertThrows(UncheckedExecutionException.class, () -> cache.getUnchecked(1));
    assertThat(ue).hasCauseThat().isSameInstanceAs(e);

    assertEquals("1", cache.getUnchecked(1));
    assertEquals(0, removalListener.getCount());

    count.set(0);
    cache.refresh(2);
    checkLoggedCause(e);

    assertEquals("2", cache.getUnchecked(2));
    assertEquals(0, removalListener.getCount());
  }


  @AndroidIncompatible // Depends on GC behavior
  public void testReloadAfterValueReclamation() throws InterruptedException, ExecutionException {
    CountingLoader countingLoader = new CountingLoader();
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().weakValues().build(countingLoader);
    ConcurrentMap<Object, Object> map = cache.asMap();

    int iterations = 10;
    WeakReference<Object> ref = new WeakReference<>(null);
    int expectedComputations = 0;
    for (int i = 0; i < iterations; i++) {
      // The entry should get garbage collected and recomputed.
      Object oldValue = ref.get();
      if (oldValue == null) {
        expectedComputations++;
      }
      ref = new WeakReference<>(cache.getUnchecked(1));
      oldValue = null;
      Thread.sleep(i);
      System.gc();
    }
    assertEquals(expectedComputations, countingLoader.getCount());

    for (int i = 0; i < iterations; i++) {
      // The entry should get garbage collected and recomputed.
      Object oldValue = ref.get();
      if (oldValue == null) {
        expectedComputations++;
      }
      cache.refresh(1);
      checkNothingLogged();
      ref = new WeakReference<>(map.get(1));
      oldValue = null;
      Thread.sleep(i);
      System.gc();
    }
    assertEquals(expectedComputations, countingLoader.getCount());
  }

  public void testReloadAfterSimulatedValueReclamation() throws ExecutionException {
    CountingLoader countingLoader = new CountingLoader();
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().concurrencyLevel(1).weakValues().build(countingLoader);

    Object key = new Object();
    assertNotNull(cache.getUnchecked(key));

    CacheTesting.simulateValueReclamation(cache, key);

    // this blocks if computation can't deal with partially-collected values
    assertNotNull(cache.getUnchecked(key));
    assertEquals(1, cache.size());
    assertEquals(2, countingLoader.getCount());

    CacheTesting.simulateValueReclamation(cache, key);
    cache.refresh(key);
    checkNothingLogged();
    assertEquals(1, cache.size());
    assertEquals(3, countingLoader.getCount());
  }

  public void testReloadAfterSimulatedKeyReclamation() throws ExecutionException {
    CountingLoader countingLoader = new CountingLoader();
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().concurrencyLevel(1).weakKeys().build(countingLoader);

    Object key = new Object();
    assertNotNull(cache.getUnchecked(key));
    assertEquals(1, cache.size());

    CacheTesting.simulateKeyReclamation(cache, key);

    // this blocks if computation can't deal with partially-collected values
    assertNotNull(cache.getUnchecked(key));
    assertEquals(2, countingLoader.getCount());

    CacheTesting.simulateKeyReclamation(cache, key);
    cache.refresh(key);
    checkNothingLogged();
    assertEquals(3, countingLoader.getCount());
  }

  /**
   * Make sure LoadingCache correctly wraps ExecutionExceptions and UncheckedExecutionExceptions.
   */
  public void testLoadingExceptionWithCause() {
    final Exception cause = new Exception();
    final UncheckedExecutionException uee = new UncheckedExecutionException(cause);
    final ExecutionException ee = new ExecutionException(cause);

    LoadingCache<Object, Object> cacheUnchecked =
        CacheBuilder.newBuilder().build(exceptionLoader(uee));
    LoadingCache<Object, Object> cacheChecked =
        CacheBuilder.newBuilder().build(exceptionLoader(ee));

    try {
      cacheUnchecked.get(new Object());
      fail();
    } catch (ExecutionException e) {
      fail();
    } catch (UncheckedExecutionException caughtEe) {
      assertThat(caughtEe).hasCauseThat().isSameInstanceAs(uee);
    }

    UncheckedExecutionException caughtUee =
        assertThrows(
            UncheckedExecutionException.class, () -> cacheUnchecked.getUnchecked(new Object()));
    assertThat(caughtUee).hasCauseThat().isSameInstanceAs(uee);

    cacheUnchecked.refresh(new Object());
    checkLoggedCause(uee);

    try {
      cacheUnchecked.getAll(asList(new Object()));
      fail();
    } catch (ExecutionException e) {
      fail();
    } catch (UncheckedExecutionException caughtEe) {
      assertThat(caughtEe).hasCauseThat().isSameInstanceAs(uee);
    }

    ExecutionException caughtEe =
        assertThrows(ExecutionException.class, () -> cacheChecked.get(new Object()));
    assertThat(caughtEe).hasCauseThat().isSameInstanceAs(ee);

    caughtUee =
        assertThrows(
            UncheckedExecutionException.class, () -> cacheChecked.getUnchecked(new Object()));
    assertThat(caughtUee).hasCauseThat().isSameInstanceAs(ee);

    cacheChecked.refresh(new Object());
    checkLoggedCause(ee);

    caughtEe =
        assertThrows(ExecutionException.class, () -> cacheChecked.getAll(asList(new Object())));
    assertThat(caughtEe).hasCauseThat().isSameInstanceAs(ee);
  }

  public void testBulkLoadingExceptionWithCause() {
    final Exception cause = new Exception();
    final UncheckedExecutionException uee = new UncheckedExecutionException(cause);
    final ExecutionException ee = new ExecutionException(cause);

    LoadingCache<Object, Object> cacheUnchecked =
        CacheBuilder.newBuilder().build(bulkLoader(exceptionLoader(uee)));
    LoadingCache<Object, Object> cacheChecked =
        CacheBuilder.newBuilder().build(bulkLoader(exceptionLoader(ee)));

    try {
      cacheUnchecked.getAll(asList(new Object()));
      fail();
    } catch (ExecutionException e) {
      fail();
    } catch (UncheckedExecutionException caughtEe) {
      assertThat(caughtEe).hasCauseThat().isSameInstanceAs(uee);
    }

    ExecutionException caughtEe =
        assertThrows(ExecutionException.class, () -> cacheChecked.getAll(asList(new Object())));
    assertThat(caughtEe).hasCauseThat().isSameInstanceAs(ee);
  }

  @AndroidIncompatible // Bug? expected:<1> but was:<2>
  public void testConcurrentLoading() throws InterruptedException {
    testConcurrentLoading(CacheBuilder.newBuilder());
  }

  private static void testConcurrentLoading(CacheBuilder<Object, Object> builder)
      throws InterruptedException {
    testConcurrentLoadingDefault(builder);
    testConcurrentLoadingNull(builder);
    testConcurrentLoadingUncheckedException(builder);
    testConcurrentLoadingCheckedException(builder);
  }

  @AndroidIncompatible // Bug? expected:<1> but was:<2>
  public void testConcurrentExpirationLoading() throws InterruptedException {
    testConcurrentLoading(CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS));
  }

  /**
   * On a successful concurrent computation, only one thread does the work, but all the threads get
   * the same result.
   */
  private static void testConcurrentLoadingDefault(CacheBuilder<Object, Object> builder)
      throws InterruptedException {

    int count = 10;
    final AtomicInteger callCount = new AtomicInteger();
    final CountDownLatch startSignal = new CountDownLatch(count + 1);
    final Object result = new Object();

    LoadingCache<String, Object> cache =
        builder.build(
            new CacheLoader<String, Object>() {
              @Override
              public Object load(String key) throws InterruptedException {
                callCount.incrementAndGet();
                startSignal.await();
                return result;
              }
            });

    List<Object> resultArray = doConcurrentGet(cache, "bar", count, startSignal);

    assertEquals(1, callCount.get());
    for (int i = 0; i < count; i++) {
      assertSame("result(" + i + ") didn't match expected", result, resultArray.get(i));
    }
  }

  /**
   * On a concurrent computation that returns null, all threads should get an
   * InvalidCacheLoadException, with the loader only called once. The result should not be cached (a
   * later request should call the loader again).
   */
  private static void testConcurrentLoadingNull(CacheBuilder<Object, Object> builder)
      throws InterruptedException {

    int count = 10;
    final AtomicInteger callCount = new AtomicInteger();
    final CountDownLatch startSignal = new CountDownLatch(count + 1);

    LoadingCache<String, String> cache =
        builder.build(
            new CacheLoader<String, String>() {
              @Override
              public String load(String key) throws InterruptedException {
                callCount.incrementAndGet();
                startSignal.await();
                return null;
              }
            });

    List<Object> result = doConcurrentGet(cache, "bar", count, startSignal);

    assertEquals(1, callCount.get());
    for (int i = 0; i < count; i++) {
      assertThat(result.get(i)).isInstanceOf(InvalidCacheLoadException.class);
    }

    // subsequent calls should call the loader again, not get the old exception
    try {
      cache.getUnchecked("bar");
      fail();
    } catch (InvalidCacheLoadException expected) {
    }
    assertEquals(2, callCount.get());
  }

  /**
   * On a concurrent computation that throws an unchecked exception, all threads should get the
   * (wrapped) exception, with the loader called only once. The result should not be cached (a later
   * request should call the loader again).
   */
  private static void testConcurrentLoadingUncheckedException(CacheBuilder<Object, Object> builder)
      throws InterruptedException {

    int count = 10;
    final AtomicInteger callCount = new AtomicInteger();
    final CountDownLatch startSignal = new CountDownLatch(count + 1);
    final RuntimeException e = new RuntimeException();

    LoadingCache<String, String> cache =
        builder.build(
            new CacheLoader<String, String>() {
              @Override
              public String load(String key) throws InterruptedException {
                callCount.incrementAndGet();
                startSignal.await();
                throw e;
              }
            });

    List<Object> result = doConcurrentGet(cache, "bar", count, startSignal);

    assertEquals(1, callCount.get());
    for (int i = 0; i < count; i++) {
      // doConcurrentGet alternates between calling getUnchecked and calling get, but an unchecked
      // exception thrown by the loader is always wrapped as an UncheckedExecutionException.
      assertThat(result.get(i)).isInstanceOf(UncheckedExecutionException.class);
      assertThat(((UncheckedExecutionException) result.get(i))).hasCauseThat().isSameInstanceAs(e);
    }

    // subsequent calls should call the loader again, not get the old exception
    try {
      cache.getUnchecked("bar");
      fail();
    } catch (UncheckedExecutionException expected) {
    }
    assertEquals(2, callCount.get());
  }

  /**
   * On a concurrent computation that throws a checked exception, all threads should get the
   * (wrapped) exception, with the loader called only once. The result should not be cached (a later
   * request should call the loader again).
   */
  private static void testConcurrentLoadingCheckedException(CacheBuilder<Object, Object> builder)
      throws InterruptedException {

    int count = 10;
    final AtomicInteger callCount = new AtomicInteger();
    final CountDownLatch startSignal = new CountDownLatch(count + 1);
    final IOException e = new IOException();

    LoadingCache<String, String> cache =
        builder.build(
            new CacheLoader<String, String>() {
              @Override
              public String load(String key) throws IOException, InterruptedException {
                callCount.incrementAndGet();
                startSignal.await();
                throw e;
              }
            });

    List<Object> result = doConcurrentGet(cache, "bar", count, startSignal);

    assertEquals(1, callCount.get());
    for (int i = 0; i < count; i++) {
      // doConcurrentGet alternates between calling getUnchecked and calling get. If we call get(),
      // we should get an ExecutionException; if we call getUnchecked(), we should get an
      // UncheckedExecutionException.
      int mod = i % 3;
      if (mod == 0 || mod == 2) {
        assertThat(result.get(i)).isInstanceOf(ExecutionException.class);
        assertThat((ExecutionException) result.get(i)).hasCauseThat().isSameInstanceAs(e);
      } else {
        assertThat(result.get(i)).isInstanceOf(UncheckedExecutionException.class);
        assertThat((UncheckedExecutionException) result.get(i)).hasCauseThat().isSameInstanceAs(e);
      }
    }

    // subsequent calls should call the loader again, not get the old exception
    try {
      cache.getUnchecked("bar");
      fail();
    } catch (UncheckedExecutionException expected) {
    }
    assertEquals(2, callCount.get());
  }

  /**
   * Test-helper method that performs {@code nThreads} concurrent calls to {@code cache.get(key)} or
   * {@code cache.getUnchecked(key)}, and returns a List containing each of the results. The result
   * for any given call to {@code cache.get} or {@code cache.getUnchecked} is the value returned, or
   * the exception thrown.
   *
   * <p>As we iterate from {@code 0} to {@code nThreads}, threads with an even index will call
   * {@code getUnchecked}, and threads with an odd index will call {@code get}. If the cache throws
   * exceptions, this difference may be visible in the returned List.
   */
  private static <K> List<Object> doConcurrentGet(
      final LoadingCache<K, ?> cache,
      final K key,
      int nThreads,
      final CountDownLatch gettersStartedSignal)
      throws InterruptedException {

    final AtomicReferenceArray<Object> result = new AtomicReferenceArray<>(nThreads);
    final CountDownLatch gettersComplete = new CountDownLatch(nThreads);
    for (int i = 0; i < nThreads; i++) {
      final int index = i;
      Thread thread =
          new Thread(
              new Runnable() {
                @Override
                public void run() {
                  gettersStartedSignal.countDown();
                  Object value = null;
                  try {
                    int mod = index % 3;
                    if (mod == 0) {
                      value = cache.get(key);
                    } else if (mod == 1) {
                      value = cache.getUnchecked(key);
                    } else {
                      cache.refresh(key);
                      value = cache.get(key);
                    }
                    result.set(index, value);
                  } catch (Throwable t) {
                    result.set(index, t);
                  }
                  gettersComplete.countDown();
                }
              });
      thread.start();
      // we want to wait until each thread is WAITING - one thread waiting inside CacheLoader.load
      // (in startSignal.await()), and the others waiting for that thread's result.
      while (thread.isAlive() && thread.getState() != Thread.State.WAITING) {
        Thread.yield();
      }
    }
    gettersStartedSignal.countDown();
    gettersComplete.await();

    List<Object> resultList = Lists.newArrayListWithExpectedSize(nThreads);
    for (int i = 0; i < nThreads; i++) {
      resultList.add(result.get(i));
    }
    return resultList;
  }

  public void testAsMapDuringLoading() throws InterruptedException, ExecutionException {
    final CountDownLatch getStartedSignal = new CountDownLatch(2);
    final CountDownLatch letGetFinishSignal = new CountDownLatch(1);
    final CountDownLatch getFinishedSignal = new CountDownLatch(2);
    final String getKey = "get";
    final String refreshKey = "refresh";
    final String suffix = "Suffix";

    CacheLoader<String, String> computeFunction =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws InterruptedException {
            getStartedSignal.countDown();
            letGetFinishSignal.await();
            return key + suffix;
          }
        };

    final LoadingCache<String, String> cache = CacheBuilder.newBuilder().build(computeFunction);
    ConcurrentMap<String, String> map = cache.asMap();
    map.put(refreshKey, refreshKey);
    assertEquals(1, map.size());
    assertFalse(map.containsKey(getKey));
    assertSame(refreshKey, map.get(refreshKey));

    new Thread() {
      @Override
      public void run() {
        cache.getUnchecked(getKey);
        getFinishedSignal.countDown();
      }
    }.start();
    new Thread() {
      @Override
      public void run() {
        cache.refresh(refreshKey);
        getFinishedSignal.countDown();
      }
    }.start();

    getStartedSignal.await();

    // computation is in progress; asMap shouldn't have changed
    assertEquals(1, map.size());
    assertFalse(map.containsKey(getKey));
    assertSame(refreshKey, map.get(refreshKey));

    // let computation complete
    letGetFinishSignal.countDown();
    getFinishedSignal.await();
    checkNothingLogged();

    // asMap view should have been updated
    assertEquals(2, cache.size());
    assertEquals(getKey + suffix, map.get(getKey));
    assertEquals(refreshKey + suffix, map.get(refreshKey));
  }

  public void testInvalidateDuringLoading() throws InterruptedException, ExecutionException {
    // computation starts; invalidate() is called on the key being computed, computation finishes
    final CountDownLatch computationStarted = new CountDownLatch(2);
    final CountDownLatch letGetFinishSignal = new CountDownLatch(1);
    final CountDownLatch getFinishedSignal = new CountDownLatch(2);
    final String getKey = "get";
    final String refreshKey = "refresh";
    final String suffix = "Suffix";

    CacheLoader<String, String> computeFunction =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws InterruptedException {
            computationStarted.countDown();
            letGetFinishSignal.await();
            return key + suffix;
          }
        };

    final LoadingCache<String, String> cache = CacheBuilder.newBuilder().build(computeFunction);
    ConcurrentMap<String, String> map = cache.asMap();
    map.put(refreshKey, refreshKey);

    new Thread() {
      @Override
      public void run() {
        cache.getUnchecked(getKey);
        getFinishedSignal.countDown();
      }
    }.start();
    new Thread() {
      @Override
      public void run() {
        cache.refresh(refreshKey);
        getFinishedSignal.countDown();
      }
    }.start();

    computationStarted.await();
    cache.invalidate(getKey);
    cache.invalidate(refreshKey);
    assertFalse(map.containsKey(getKey));
    assertFalse(map.containsKey(refreshKey));

    // let computation complete
    letGetFinishSignal.countDown();
    getFinishedSignal.await();
    checkNothingLogged();

    // results should be visible
    assertEquals(2, cache.size());
    assertEquals(getKey + suffix, map.get(getKey));
    assertEquals(refreshKey + suffix, map.get(refreshKey));
    assertEquals(2, cache.size());
  }

  public void testInvalidateAndReloadDuringLoading()
      throws InterruptedException, ExecutionException {
    // computation starts; clear() is called, computation finishes
    final CountDownLatch computationStarted = new CountDownLatch(2);
    final CountDownLatch letGetFinishSignal = new CountDownLatch(1);
    final CountDownLatch getFinishedSignal = new CountDownLatch(4);
    final String getKey = "get";
    final String refreshKey = "refresh";
    final String suffix = "Suffix";

    CacheLoader<String, String> computeFunction =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws InterruptedException {
            computationStarted.countDown();
            letGetFinishSignal.await();
            return key + suffix;
          }
        };

    final LoadingCache<String, String> cache = CacheBuilder.newBuilder().build(computeFunction);
    ConcurrentMap<String, String> map = cache.asMap();
    map.put(refreshKey, refreshKey);

    new Thread() {
      @Override
      public void run() {
        cache.getUnchecked(getKey);
        getFinishedSignal.countDown();
      }
    }.start();
    new Thread() {
      @Override
      public void run() {
        cache.refresh(refreshKey);
        getFinishedSignal.countDown();
      }
    }.start();

    computationStarted.await();
    cache.invalidate(getKey);
    cache.invalidate(refreshKey);
    assertFalse(map.containsKey(getKey));
    assertFalse(map.containsKey(refreshKey));

    // start new computations
    new Thread() {
      @Override
      public void run() {
        cache.getUnchecked(getKey);
        getFinishedSignal.countDown();
      }
    }.start();
    new Thread() {
      @Override
      public void run() {
        cache.refresh(refreshKey);
        getFinishedSignal.countDown();
      }
    }.start();

    // let computation complete
    letGetFinishSignal.countDown();
    getFinishedSignal.await();
    checkNothingLogged();

    // results should be visible
    assertEquals(2, cache.size());
    assertEquals(getKey + suffix, map.get(getKey));
    assertEquals(refreshKey + suffix, map.get(refreshKey));
  }

  public void testExpandDuringLoading() throws InterruptedException {
    final int count = 3;
    final AtomicInteger callCount = new AtomicInteger();
    // tells the computing thread when to start computing
    final CountDownLatch computeSignal = new CountDownLatch(1);
    // tells the main thread when computation is pending
    final CountDownLatch secondSignal = new CountDownLatch(1);
    // tells the main thread when the second get has started
    final CountDownLatch thirdSignal = new CountDownLatch(1);
    // tells the main thread when the third get has started
    final CountDownLatch fourthSignal = new CountDownLatch(1);
    // tells the test when all gets have returned
    final CountDownLatch doneSignal = new CountDownLatch(count);

    CacheLoader<String, String> computeFunction =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws InterruptedException {
            callCount.incrementAndGet();
            secondSignal.countDown();
            computeSignal.await();
            return key + "foo";
          }
        };

    final LoadingCache<String, String> cache =
        CacheBuilder.newBuilder().weakKeys().build(computeFunction);

    final AtomicReferenceArray<String> result = new AtomicReferenceArray<>(count);

    final String key = "bar";

    // start computing thread
    new Thread() {
      @Override
      public void run() {
        result.set(0, cache.getUnchecked(key));
        doneSignal.countDown();
      }
    }.start();

    // wait for computation to start
    secondSignal.await();

    // start waiting thread
    new Thread() {
      @Override
      public void run() {
        thirdSignal.countDown();
        result.set(1, cache.getUnchecked(key));
        doneSignal.countDown();
      }
    }.start();

    // give the second get a chance to run; it is okay for this to be racy
    // as the end result should be the same either way
    thirdSignal.await();
    Thread.yield();

    // Expand!
    CacheTesting.forceExpandSegment(cache, key);

    // start another waiting thread
    new Thread() {
      @Override
      public void run() {
        fourthSignal.countDown();
        result.set(2, cache.getUnchecked(key));
        doneSignal.countDown();
      }
    }.start();

    // give the third get a chance to run; it is okay for this to be racy
    // as the end result should be the same either way
    fourthSignal.await();
    Thread.yield();

    // let computation finish
    computeSignal.countDown();
    doneSignal.await();

    assertTrue(callCount.get() == 1);
    assertEquals("barfoo", result.get(0));
    assertEquals("barfoo", result.get(1));
    assertEquals("barfoo", result.get(2));
    assertEquals("barfoo", cache.getUnchecked(key));
  }

  // Test ignored because it is extremely flaky in CI builds
  public void
      ignoreTestExpandDuringRefresh()
      throws InterruptedException, ExecutionException {
    final AtomicInteger callCount = new AtomicInteger();
    // tells the computing thread when to start computing
    final CountDownLatch computeSignal = new CountDownLatch(1);
    // tells the main thread when computation is pending
    final CountDownLatch secondSignal = new CountDownLatch(1);
    // tells the main thread when the second get has started
    final CountDownLatch thirdSignal = new CountDownLatch(1);
    // tells the main thread when the third get has started
    final CountDownLatch fourthSignal = new CountDownLatch(1);
    // tells the test when all gets have returned
    final CountDownLatch doneSignal = new CountDownLatch(3);
    final String suffix = "Suffix";

    CacheLoader<String, String> computeFunction =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws InterruptedException {
            callCount.incrementAndGet();
            secondSignal.countDown();
            computeSignal.await();
            return key + suffix;
          }
        };

    final AtomicReferenceArray<String> result = new AtomicReferenceArray<>(2);

    final LoadingCache<String, String> cache = CacheBuilder.newBuilder().build(computeFunction);
    final String key = "bar";
    cache.asMap().put(key, key);

    // start computing thread
    new Thread() {
      @Override
      public void run() {
        cache.refresh(key);
        doneSignal.countDown();
      }
    }.start();

    // wait for computation to start
    secondSignal.await();
    checkNothingLogged();

    // start waiting thread
    new Thread() {
      @Override
      public void run() {
        thirdSignal.countDown();
        result.set(0, cache.getUnchecked(key));
        doneSignal.countDown();
      }
    }.start();

    // give the second get a chance to run; it is okay for this to be racy
    // as the end result should be the same either way
    thirdSignal.await();
    Thread.yield();

    // Expand!
    CacheTesting.forceExpandSegment(cache, key);

    // start another waiting thread
    new Thread() {
      @Override
      public void run() {
        fourthSignal.countDown();
        result.set(1, cache.getUnchecked(key));
        doneSignal.countDown();
      }
    }.start();

    // give the third get a chance to run; it is okay for this to be racy
    // as the end result should be the same either way
    fourthSignal.await();
    Thread.yield();

    // let computation finish
    computeSignal.countDown();
    doneSignal.await();

    assertTrue(callCount.get() == 1);
    assertEquals(key, result.get(0));
    assertEquals(key, result.get(1));
    assertEquals(key + suffix, cache.getUnchecked(key));
  }

  static <T> Callable<T> throwing(final Exception exception) {
    return new Callable<T>() {
      @Override
      public T call() throws Exception {
        throw exception;
      }
    };
  }
}
