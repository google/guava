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
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.TestingCacheLoaders.CountingLoader;
import com.google.common.cache.TestingCacheLoaders.IdentityLoader;
import com.google.common.cache.TestingRemovalListeners.CountingRemovalListener;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.testing.FakeTicker;
import com.google.common.testing.TestLogHandler;
import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.LogRecord;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests relating to cache loading: concurrent loading, exceptions during loading, etc.
 *
 * @author mike nonemacher
 */
@NullUnmarked
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
    Thread.interrupted();
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
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    Object key = new Object();
    assertThat(cache.get(key)).isSameInstanceAs(key);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    key = new Object();
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(key);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(2);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    key = new Object();
    cache.refresh(key);
    checkNothingLogged();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(3);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.get(key)).isSameInstanceAs(key);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(3);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);

    Object value = new Object();
    // callable is not called
    assertThat(cache.get(key, throwing(new Exception()))).isSameInstanceAs(key);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(3);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(2);

    key = new Object();
    assertThat(cache.get(key, Callables.returning(value))).isSameInstanceAs(value);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(3);
    assertThat(stats.loadSuccessCount()).isEqualTo(4);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(2);
  }

  public void testReload() throws ExecutionException {
    Object one = new Object();
    Object two = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return immediateFuture(two);
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(key);
    checkNothingLogged();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(2);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(two);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(2);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);
  }

  public void testRefresh() {
    Object one = new Object();
    Object two = new Object();
    FakeTicker ticker = new FakeTicker();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return immediateFuture(two);
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
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(two);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(2);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(2);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(two);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(2);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(3);
  }

  public void testRefresh_getIfPresent() {
    Object one = new Object();
    Object two = new Object();
    FakeTicker ticker = new FakeTicker();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return immediateFuture(two);
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
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getIfPresent(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getIfPresent(key)).isSameInstanceAs(two);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(2);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(2);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getIfPresent(key)).isSameInstanceAs(two);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(2);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(3);
  }

  public void testBulkLoad_default() throws ExecutionException {
    LoadingCache<Integer, Integer> cache =
        CacheBuilder.newBuilder()
            .recordStats()
            .build(TestingCacheLoaders.<Integer>identityLoader());
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getAll(ImmutableList.of())).isEmpty();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getAll(asList(1))).containsExactly(1, 1);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getAll(asList(1, 2, 3, 4))).containsExactly(1, 1, 2, 2, 3, 3, 4, 4);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(4);
    assertThat(stats.loadSuccessCount()).isEqualTo(4);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);

    assertThat(cache.getAll(asList(2, 3))).containsExactly(2, 2, 3, 3);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(4);
    assertThat(stats.loadSuccessCount()).isEqualTo(4);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(3);

    // duplicate keys are ignored, and don't impact stats
    assertThat(cache.getAll(asList(4, 5))).containsExactly(4, 4, 5, 5);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(5);
    assertThat(stats.loadSuccessCount()).isEqualTo(5);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(4);
  }

  public void testBulkLoad_loadAll() throws ExecutionException {
    IdentityLoader<Integer> backingLoader = identityLoader();
    CacheLoader<Integer, Integer> loader = bulkLoader(backingLoader);
    LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getAll(ImmutableList.<Integer>of())).containsExactly();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getAll(asList(1))).containsExactly(1, 1);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getAll(asList(1, 2, 3, 4))).containsExactly(1, 1, 2, 2, 3, 3, 4, 4);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(4);
    assertThat(stats.loadSuccessCount()).isEqualTo(2);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);

    assertThat(cache.getAll(asList(2, 3))).containsExactly(2, 2, 3, 3);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(4);
    assertThat(stats.loadSuccessCount()).isEqualTo(2);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(3);

    // duplicate keys are ignored, and don't impact stats
    assertThat(cache.getAll(asList(4, 5))).containsExactly(4, 4, 5, 5);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(5);
    assertThat(stats.loadSuccessCount()).isEqualTo(3);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(4);
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
            Map<Object, Object> result = new HashMap<>();
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
      assertThat(result.get(key)).isSameInstanceAs(value);
      assertThat(result.get(value)).isNull();
      assertThat(cache.asMap().get(key)).isSameInstanceAs(value);
      assertThat(cache.asMap().get(value)).isSameInstanceAs(key);
    }
  }

  public void testBulkLoad_clobber() throws ExecutionException {
    Object extraKey = new Object();
    Object extraValue = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) throws Exception {
            throw new AssertionError();
          }

          @Override
          public Map<Object, Object> loadAll(Iterable<?> keys) throws Exception {
            Map<Object, Object> result = new HashMap<>();
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
    assertThat(cache.asMap().get(extraKey)).isSameInstanceAs(extraKey);

    Object[] lookupKeys = new Object[] {new Object(), new Object(), new Object()};
    Map<Object, Object> result = cache.getAll(asList(lookupKeys));
    assertThat(result.keySet()).containsExactlyElementsIn(asList(lookupKeys));
    for (Entry<Object, Object> entry : result.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();
      assertThat(result.get(key)).isSameInstanceAs(value);
      assertThat(cache.asMap().get(key)).isSameInstanceAs(value);
    }
    assertThat(result.get(extraKey)).isNull();
    assertThat(cache.asMap().get(extraKey)).isSameInstanceAs(extraValue);
  }

  public void testBulkLoad_clobberNullValue() throws ExecutionException {
    Object extraKey = new Object();
    Object extraValue = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) throws Exception {
            throw new AssertionError();
          }

          @Override
          public Map<Object, Object> loadAll(Iterable<?> keys) throws Exception {
            Map<Object, Object> result = new HashMap<>();
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
    assertThat(cache.asMap().get(extraKey)).isSameInstanceAs(extraKey);

    Object[] lookupKeys = new Object[] {new Object(), new Object(), new Object()};
    assertThrows(InvalidCacheLoadException.class, () -> cache.getAll(asList(lookupKeys)));

    for (Object key : lookupKeys) {
      assertThat(cache.asMap().containsKey(key)).isTrue();
    }
    assertThat(cache.asMap().get(extraKey)).isSameInstanceAs(extraValue);
    assertThat(cache.asMap().containsKey(extraValue)).isFalse();
  }

  public void testBulkLoad_clobberNullKey() throws ExecutionException {
    Object extraKey = new Object();
    Object extraValue = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) throws Exception {
            throw new AssertionError();
          }

          @Override
          public Map<Object, Object> loadAll(Iterable<?> keys) throws Exception {
            Map<Object, Object> result = new HashMap<>();
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
    assertThat(cache.asMap().get(extraKey)).isSameInstanceAs(extraKey);

    Object[] lookupKeys = new Object[] {new Object(), new Object(), new Object()};
    assertThrows(InvalidCacheLoadException.class, () -> cache.getAll(asList(lookupKeys)));

    for (Object key : lookupKeys) {
      assertThat(cache.asMap().containsKey(key)).isTrue();
    }
    assertThat(cache.asMap().get(extraKey)).isSameInstanceAs(extraValue);
    assertThat(cache.asMap().containsValue(extraKey)).isFalse();
  }

  public void testBulkLoad_partial() throws ExecutionException {
    Object extraKey = new Object();
    Object extraValue = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) throws Exception {
            throw new AssertionError();
          }

          @Override
          public Map<Object, Object> loadAll(Iterable<?> keys) throws Exception {
            Map<Object, Object> result = new HashMap<>();
            // ignore request keys
            result.put(extraKey, extraValue);
            return result;
          }
        };
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().build(loader);

    Object[] lookupKeys = new Object[] {new Object(), new Object(), new Object()};
    assertThrows(InvalidCacheLoadException.class, () -> cache.getAll(asList(lookupKeys)));
    assertThat(cache.asMap().get(extraKey)).isSameInstanceAs(extraValue);
  }

  public void testLoadNull() throws ExecutionException {
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(constantLoader(null));
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThrows(InvalidCacheLoadException.class, () -> cache.get(new Object()));
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThrows(InvalidCacheLoadException.class, () -> cache.getUnchecked(new Object()));
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(2);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(new Object());
    checkLoggedInvalidLoad();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(3);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThrows(
        InvalidCacheLoadException.class, () -> cache.get(new Object(), Callables.returning(null)));
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(3);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(4);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThrows(InvalidCacheLoadException.class, () -> cache.getAll(asList(new Object())));
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(4);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(5);
    assertThat(stats.hitCount()).isEqualTo(0);
  }

  public void testReloadNull() throws ExecutionException {
    Object one = new Object();
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
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(key);
    checkLoggedInvalidLoad();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(1);
  }

  public void testReloadNullFuture() throws ExecutionException {
    Object one = new Object();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return immediateFuture(null);
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(key);
    checkLoggedInvalidLoad();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(1);
  }

  public void testRefreshNull() {
    Object one = new Object();
    FakeTicker ticker = new FakeTicker();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return immediateFuture(null);
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
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    // refreshed
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(2);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(2);
    assertThat(stats.hitCount()).isEqualTo(3);
  }

  public void testBulkLoadNull() throws ExecutionException {
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(bulkLoader(constantLoader(null)));
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThrows(InvalidCacheLoadException.class, () -> cache.getAll(asList(new Object())));
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);
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
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThrows(InvalidCacheLoadException.class, () -> cache.getAll(asList(new Object())));
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);
  }

  public void testLoadError() throws ExecutionException {
    Error e = new Error();
    CacheLoader<Object, Object> loader = errorLoader(e);
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    ExecutionError expected = assertThrows(ExecutionError.class, () -> cache.get(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    expected = assertThrows(ExecutionError.class, () -> cache.getUnchecked(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(2);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(new Object());
    checkLoggedCause(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(3);
    assertThat(stats.hitCount()).isEqualTo(0);

    Error callableError = new Error();
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
    assertThat(stats.missCount()).isEqualTo(3);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(4);
    assertThat(stats.hitCount()).isEqualTo(0);

    expected = assertThrows(ExecutionError.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(4);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(5);
    assertThat(stats.hitCount()).isEqualTo(0);
  }

  public void testReloadError() throws ExecutionException {
    Object one = new Object();
    Error e = new Error();
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
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(key);
    checkLoggedCause(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(1);
  }

  public void testReloadFutureError() throws ExecutionException {
    Object one = new Object();
    Error e = new Error();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return immediateFailedFuture(e);
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(key);
    checkLoggedCause(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(1);
  }

  public void testRefreshError() {
    Object one = new Object();
    Error e = new Error();
    FakeTicker ticker = new FakeTicker();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return immediateFailedFuture(e);
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
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    // refreshed
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(2);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(2);
    assertThat(stats.hitCount()).isEqualTo(3);
  }

  public void testBulkLoadError() throws ExecutionException {
    Error e = new Error();
    CacheLoader<Object, Object> loader = errorLoader(e);
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(bulkLoader(loader));
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    ExecutionError expected =
        assertThrows(ExecutionError.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);
  }

  public void testLoadCheckedException() {
    Exception e = new Exception();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    Exception expected = assertThrows(ExecutionException.class, () -> cache.get(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    expected =
        assertThrows(UncheckedExecutionException.class, () -> cache.getUnchecked(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(2);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(new Object());
    checkLoggedCause(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(3);
    assertThat(stats.hitCount()).isEqualTo(0);

    Exception callableException = new Exception();
    expected =
        assertThrows(
            ExecutionException.class, () -> cache.get(new Object(), throwing(callableException)));
    assertThat(expected).hasCauseThat().isSameInstanceAs(callableException);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(3);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(4);
    assertThat(stats.hitCount()).isEqualTo(0);

    expected = assertThrows(ExecutionException.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(4);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(5);
    assertThat(stats.hitCount()).isEqualTo(0);
  }

  public void testLoadInterruptedException() {
    Exception e = new InterruptedException();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    // Sanity check:
    assertThat(Thread.interrupted()).isFalse();

    Exception expected = assertThrows(ExecutionException.class, () -> cache.get(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    assertThat(Thread.interrupted()).isTrue();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    expected =
        assertThrows(UncheckedExecutionException.class, () -> cache.getUnchecked(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    assertThat(Thread.interrupted()).isTrue();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(2);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(new Object());
    assertThat(Thread.interrupted()).isTrue();
    checkLoggedCause(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(3);
    assertThat(stats.hitCount()).isEqualTo(0);

    Exception callableException = new InterruptedException();
    expected =
        assertThrows(
            ExecutionException.class, () -> cache.get(new Object(), throwing(callableException)));
    assertThat(expected).hasCauseThat().isSameInstanceAs(callableException);
    assertThat(Thread.interrupted()).isTrue();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(3);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(4);
    assertThat(stats.hitCount()).isEqualTo(0);

    expected = assertThrows(ExecutionException.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    assertThat(Thread.interrupted()).isTrue();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(4);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(5);
    assertThat(stats.hitCount()).isEqualTo(0);
  }

  public void testReloadCheckedException() {
    Object one = new Object();
    Exception e = new Exception();
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
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(key);
    checkLoggedCause(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(1);
  }

  public void testReloadFutureCheckedException() {
    Object one = new Object();
    Exception e = new Exception();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return immediateFailedFuture(e);
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(key);
    checkLoggedCause(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(1);
  }

  public void testRefreshCheckedException() {
    Object one = new Object();
    Exception e = new Exception();
    FakeTicker ticker = new FakeTicker();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return immediateFailedFuture(e);
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
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    // refreshed
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(2);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(2);
    assertThat(stats.hitCount()).isEqualTo(3);
  }

  public void testBulkLoadCheckedException() {
    Exception e = new Exception();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(bulkLoader(loader));
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);
  }

  public void testBulkLoadInterruptedException() {
    Exception e = new InterruptedException();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(bulkLoader(loader));
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    ExecutionException expected =
        assertThrows(ExecutionException.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    assertThat(Thread.interrupted()).isTrue();
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);
  }

  public void testLoadUncheckedException() throws ExecutionException {
    Exception e = new RuntimeException();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    UncheckedExecutionException expected =
        assertThrows(UncheckedExecutionException.class, () -> cache.get(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    expected =
        assertThrows(UncheckedExecutionException.class, () -> cache.getUnchecked(new Object()));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(2);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(new Object());
    checkLoggedCause(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(2);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(3);
    assertThat(stats.hitCount()).isEqualTo(0);

    Exception callableException = new RuntimeException();
    expected =
        assertThrows(
            UncheckedExecutionException.class,
            () -> cache.get(new Object(), throwing(callableException)));
    assertThat(expected).hasCauseThat().isSameInstanceAs(callableException);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(3);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(4);
    assertThat(stats.hitCount()).isEqualTo(0);

    expected =
        assertThrows(UncheckedExecutionException.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(4);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(5);
    assertThat(stats.hitCount()).isEqualTo(0);
  }

  public void testReloadUncheckedException() throws ExecutionException {
    Object one = new Object();
    Exception e = new RuntimeException();
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
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(key);
    checkLoggedCause(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(1);
  }

  public void testReloadFutureUncheckedException() throws ExecutionException {
    Object one = new Object();
    Exception e = new RuntimeException();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return immediateFailedFuture(e);
          }
        };

    LoadingCache<Object, Object> cache = CacheBuilder.newBuilder().recordStats().build(loader);
    Object key = new Object();
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    cache.refresh(key);
    checkLoggedCause(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(1);
  }

  public void testRefreshUncheckedException() {
    Object one = new Object();
    Exception e = new RuntimeException();
    FakeTicker ticker = new FakeTicker();
    CacheLoader<Object, Object> loader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            return one;
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            return immediateFailedFuture(e);
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
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(1);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    // refreshed
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(2);

    ticker.advance(1, MILLISECONDS);
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(one);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(1);
    assertThat(stats.loadExceptionCount()).isEqualTo(2);
    assertThat(stats.hitCount()).isEqualTo(3);
  }

  public void testBulkLoadUncheckedException() throws ExecutionException {
    Exception e = new RuntimeException();
    CacheLoader<Object, Object> loader = exceptionLoader(e);
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().recordStats().build(bulkLoader(loader));
    CacheStats stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(0);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(0);
    assertThat(stats.hitCount()).isEqualTo(0);

    UncheckedExecutionException expected =
        assertThrows(UncheckedExecutionException.class, () -> cache.getAll(asList(new Object())));
    assertThat(expected).hasCauseThat().isSameInstanceAs(e);
    stats = cache.stats();
    assertThat(stats.missCount()).isEqualTo(1);
    assertThat(stats.loadSuccessCount()).isEqualTo(0);
    assertThat(stats.loadExceptionCount()).isEqualTo(1);
    assertThat(stats.hitCount()).isEqualTo(0);
  }

  public void testReloadAfterFailure() throws ExecutionException {
    AtomicInteger count = new AtomicInteger();
    Exception e = new IllegalStateException("exception to trigger failure on first load()");
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

    assertThat(cache.getUnchecked(1)).isEqualTo("1");
    assertThat(removalListener.getCount()).isEqualTo(0);

    count.set(0);
    cache.refresh(2);
    checkLoggedCause(e);

    assertThat(cache.getUnchecked(2)).isEqualTo("2");
    assertThat(removalListener.getCount()).isEqualTo(0);
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
    assertThat(countingLoader.getCount()).isEqualTo(expectedComputations);

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
    assertThat(countingLoader.getCount()).isEqualTo(expectedComputations);
  }

  public void testReloadAfterSimulatedValueReclamation() throws ExecutionException {
    CountingLoader countingLoader = new CountingLoader();
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().concurrencyLevel(1).weakValues().build(countingLoader);

    Object key = new Object();
    assertThat(cache.getUnchecked(key)).isNotNull();

    CacheTesting.simulateValueReclamation(cache, key);

    // this blocks if computation can't deal with partially-collected values
    assertThat(cache.getUnchecked(key)).isNotNull();
    assertThat(cache.size()).isEqualTo(1);
    assertThat(countingLoader.getCount()).isEqualTo(2);

    CacheTesting.simulateValueReclamation(cache, key);
    cache.refresh(key);
    checkNothingLogged();
    assertThat(cache.size()).isEqualTo(1);
    assertThat(countingLoader.getCount()).isEqualTo(3);
  }

  public void testReloadAfterSimulatedKeyReclamation() throws ExecutionException {
    CountingLoader countingLoader = new CountingLoader();
    LoadingCache<Object, Object> cache =
        CacheBuilder.newBuilder().concurrencyLevel(1).weakKeys().build(countingLoader);

    Object key = new Object();
    assertThat(cache.getUnchecked(key)).isNotNull();
    assertThat(cache.size()).isEqualTo(1);

    CacheTesting.simulateKeyReclamation(cache, key);

    // this blocks if computation can't deal with partially-collected values
    assertThat(cache.getUnchecked(key)).isNotNull();
    assertThat(countingLoader.getCount()).isEqualTo(2);

    CacheTesting.simulateKeyReclamation(cache, key);
    cache.refresh(key);
    checkNothingLogged();
    assertThat(countingLoader.getCount()).isEqualTo(3);
  }

  /**
   * Make sure LoadingCache correctly wraps ExecutionExceptions and UncheckedExecutionExceptions.
   */
  public void testLoadingExceptionWithCause() {
    Exception cause = new Exception();
    UncheckedExecutionException uee = new UncheckedExecutionException(cause);
    ExecutionException ee = new ExecutionException(cause);

    LoadingCache<Object, Object> cacheUnchecked =
        CacheBuilder.newBuilder().build(exceptionLoader(uee));
    LoadingCache<Object, Object> cacheChecked =
        CacheBuilder.newBuilder().build(exceptionLoader(ee));

    UncheckedExecutionException caughtUee =
        assertThrows(UncheckedExecutionException.class, () -> cacheUnchecked.get(new Object()));
    assertThat(caughtUee).hasCauseThat().isSameInstanceAs(uee);

    caughtUee =
        assertThrows(
            UncheckedExecutionException.class, () -> cacheUnchecked.getUnchecked(new Object()));
    assertThat(caughtUee).hasCauseThat().isSameInstanceAs(uee);

    cacheUnchecked.refresh(new Object());
    checkLoggedCause(uee);

    caughtUee =
        assertThrows(
            UncheckedExecutionException.class, () -> cacheUnchecked.getAll(asList(new Object())));
    assertThat(caughtUee).hasCauseThat().isSameInstanceAs(uee);

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
    Exception cause = new Exception();
    UncheckedExecutionException uee = new UncheckedExecutionException(cause);
    ExecutionException ee = new ExecutionException(cause);

    LoadingCache<Object, Object> cacheUnchecked =
        CacheBuilder.newBuilder().build(bulkLoader(exceptionLoader(uee)));
    LoadingCache<Object, Object> cacheChecked =
        CacheBuilder.newBuilder().build(bulkLoader(exceptionLoader(ee)));

    UncheckedExecutionException caughtUee =
        assertThrows(
            UncheckedExecutionException.class, () -> cacheUnchecked.getAll(asList(new Object())));
    assertThat(caughtUee).hasCauseThat().isSameInstanceAs(uee);

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
    testConcurrentLoading(CacheBuilder.newBuilder().expireAfterWrite(10, SECONDS));
  }

  /**
   * On a successful concurrent computation, only one thread does the work, but all the threads get
   * the same result.
   */
  private static void testConcurrentLoadingDefault(CacheBuilder<Object, Object> builder)
      throws InterruptedException {

    int count = 10;
    AtomicInteger callCount = new AtomicInteger();
    CountDownLatch startSignal = new CountDownLatch(count + 1);
    Object result = new Object();

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

    assertThat(callCount.get()).isEqualTo(1);
    for (int i = 0; i < count; i++) {
      assertWithMessage("result(%s) didn't match expected", i)
          .that(resultArray.get(i))
          .isSameInstanceAs(result);
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
    AtomicInteger callCount = new AtomicInteger();
    CountDownLatch startSignal = new CountDownLatch(count + 1);

    LoadingCache<String, String> cache =
        builder.build(
            new CacheLoader<String, String>() {
              @Override
              @SuppressWarnings("CacheLoaderNull") // test of broken user implementation
              public String load(String key) throws InterruptedException {
                callCount.incrementAndGet();
                startSignal.await();
                return null;
              }
            });

    List<Object> result = doConcurrentGet(cache, "bar", count, startSignal);

    assertThat(callCount.get()).isEqualTo(1);
    for (int i = 0; i < count; i++) {
      assertThat(result.get(i)).isInstanceOf(InvalidCacheLoadException.class);
    }

    // subsequent calls should call the loader again, not get the old exception
    assertThrows(InvalidCacheLoadException.class, () -> cache.getUnchecked("bar"));
    assertThat(callCount.get()).isEqualTo(2);
  }

  /**
   * On a concurrent computation that throws an unchecked exception, all threads should get the
   * (wrapped) exception, with the loader called only once. The result should not be cached (a later
   * request should call the loader again).
   */
  private static void testConcurrentLoadingUncheckedException(CacheBuilder<Object, Object> builder)
      throws InterruptedException {

    int count = 10;
    AtomicInteger callCount = new AtomicInteger();
    CountDownLatch startSignal = new CountDownLatch(count + 1);
    RuntimeException e = new RuntimeException();

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

    assertThat(callCount.get()).isEqualTo(1);
    for (int i = 0; i < count; i++) {
      // doConcurrentGet alternates between calling getUnchecked and calling get, but an unchecked
      // exception thrown by the loader is always wrapped as an UncheckedExecutionException.
      assertThat(result.get(i)).isInstanceOf(UncheckedExecutionException.class);
      assertThat((UncheckedExecutionException) result.get(i)).hasCauseThat().isSameInstanceAs(e);
    }

    // subsequent calls should call the loader again, not get the old exception
    assertThrows(UncheckedExecutionException.class, () -> cache.getUnchecked("bar"));
    assertThat(callCount.get()).isEqualTo(2);
  }

  /**
   * On a concurrent computation that throws a checked exception, all threads should get the
   * (wrapped) exception, with the loader called only once. The result should not be cached (a later
   * request should call the loader again).
   */
  private static void testConcurrentLoadingCheckedException(CacheBuilder<Object, Object> builder)
      throws InterruptedException {

    int count = 10;
    AtomicInteger callCount = new AtomicInteger();
    CountDownLatch startSignal = new CountDownLatch(count + 1);
    IOException e = new IOException();

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

    assertThat(callCount.get()).isEqualTo(1);
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
    assertThrows(UncheckedExecutionException.class, () -> cache.getUnchecked("bar"));
    assertThat(callCount.get()).isEqualTo(2);
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
  @SuppressWarnings("ThreadPriorityCheck") // TODO: b/175898629 - Consider onSpinWait.
  private static <K> List<Object> doConcurrentGet(
      LoadingCache<K, ?> cache, K key, int nThreads, CountDownLatch gettersStartedSignal)
      throws InterruptedException {

    AtomicReferenceArray<Object> result = new AtomicReferenceArray<>(nThreads);
    CountDownLatch gettersComplete = new CountDownLatch(nThreads);
    for (int i = 0; i < nThreads; i++) {
      int index = i;
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
    CountDownLatch getStartedSignal = new CountDownLatch(2);
    CountDownLatch letGetFinishSignal = new CountDownLatch(1);
    CountDownLatch getFinishedSignal = new CountDownLatch(2);
    String getKey = "get";
    String refreshKey = "refresh";
    String suffix = "Suffix";

    CacheLoader<String, String> computeFunction =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws InterruptedException {
            getStartedSignal.countDown();
            letGetFinishSignal.await();
            return key + suffix;
          }
        };

    LoadingCache<String, String> cache = CacheBuilder.newBuilder().build(computeFunction);
    ConcurrentMap<String, String> map = cache.asMap();
    map.put(refreshKey, refreshKey);
    assertThat(map).hasSize(1);
    assertThat(map.containsKey(getKey)).isFalse();
    assertThat(map.get(refreshKey)).isSameInstanceAs(refreshKey);

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
    assertThat(map).hasSize(1);
    assertThat(map.containsKey(getKey)).isFalse();
    assertThat(map.get(refreshKey)).isSameInstanceAs(refreshKey);

    // let computation complete
    letGetFinishSignal.countDown();
    getFinishedSignal.await();
    checkNothingLogged();

    // asMap view should have been updated
    assertThat(cache.size()).isEqualTo(2);
    assertThat(map.get(getKey)).isEqualTo(getKey + suffix);
    assertThat(map.get(refreshKey)).isEqualTo(refreshKey + suffix);
  }

  public void testInvalidateDuringLoading() throws InterruptedException, ExecutionException {
    // computation starts; invalidate() is called on the key being computed, computation finishes
    CountDownLatch computationStarted = new CountDownLatch(2);
    CountDownLatch letGetFinishSignal = new CountDownLatch(1);
    CountDownLatch getFinishedSignal = new CountDownLatch(2);
    String getKey = "get";
    String refreshKey = "refresh";
    String suffix = "Suffix";

    CacheLoader<String, String> computeFunction =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws InterruptedException {
            computationStarted.countDown();
            letGetFinishSignal.await();
            return key + suffix;
          }
        };

    LoadingCache<String, String> cache = CacheBuilder.newBuilder().build(computeFunction);
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
    assertThat(map.containsKey(getKey)).isFalse();
    assertThat(map.containsKey(refreshKey)).isFalse();

    // let computation complete
    letGetFinishSignal.countDown();
    getFinishedSignal.await();
    checkNothingLogged();

    // results should be visible
    assertThat(cache.size()).isEqualTo(2);
    assertThat(map.get(getKey)).isEqualTo(getKey + suffix);
    assertThat(map.get(refreshKey)).isEqualTo(refreshKey + suffix);
    assertThat(cache.size()).isEqualTo(2);
  }

  public void testInvalidateAndReloadDuringLoading()
      throws InterruptedException, ExecutionException {
    // computation starts; clear() is called, computation finishes
    CountDownLatch computationStarted = new CountDownLatch(2);
    CountDownLatch letGetFinishSignal = new CountDownLatch(1);
    CountDownLatch getFinishedSignal = new CountDownLatch(4);
    String getKey = "get";
    String refreshKey = "refresh";
    String suffix = "Suffix";

    CacheLoader<String, String> computeFunction =
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws InterruptedException {
            computationStarted.countDown();
            letGetFinishSignal.await();
            return key + suffix;
          }
        };

    LoadingCache<String, String> cache = CacheBuilder.newBuilder().build(computeFunction);
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
    assertThat(map.containsKey(getKey)).isFalse();
    assertThat(map.containsKey(refreshKey)).isFalse();

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
    assertThat(cache.size()).isEqualTo(2);
    assertThat(map.get(getKey)).isEqualTo(getKey + suffix);
    assertThat(map.get(refreshKey)).isEqualTo(refreshKey + suffix);
  }

  @SuppressWarnings("ThreadPriorityCheck") // doing our best to test for races
  public void testExpandDuringLoading() throws InterruptedException {
    int count = 3;
    AtomicInteger callCount = new AtomicInteger();
    // tells the computing thread when to start computing
    CountDownLatch computeSignal = new CountDownLatch(1);
    // tells the main thread when computation is pending
    CountDownLatch secondSignal = new CountDownLatch(1);
    // tells the main thread when the second get has started
    CountDownLatch thirdSignal = new CountDownLatch(1);
    // tells the main thread when the third get has started
    CountDownLatch fourthSignal = new CountDownLatch(1);
    // tells the test when all gets have returned
    CountDownLatch doneSignal = new CountDownLatch(count);

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

    LoadingCache<String, String> cache =
        CacheBuilder.newBuilder().weakKeys().build(computeFunction);

    AtomicReferenceArray<String> result = new AtomicReferenceArray<>(count);

    String key = "bar";

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

    assertThat(callCount.get()).isEqualTo(1);
    assertThat(result.get(0)).isEqualTo("barfoo");
    assertThat(result.get(1)).isEqualTo("barfoo");
    assertThat(result.get(2)).isEqualTo("barfoo");
    assertThat(cache.getUnchecked(key)).isEqualTo("barfoo");
  }

  // Test ignored because it is extremely flaky in CI builds
  @SuppressWarnings("ThreadPriorityCheck") // doing our best to test for races
  public void
      ignoreTestExpandDuringRefresh()
      throws InterruptedException, ExecutionException {
    AtomicInteger callCount = new AtomicInteger();
    // tells the computing thread when to start computing
    CountDownLatch computeSignal = new CountDownLatch(1);
    // tells the main thread when computation is pending
    CountDownLatch secondSignal = new CountDownLatch(1);
    // tells the main thread when the second get has started
    CountDownLatch thirdSignal = new CountDownLatch(1);
    // tells the main thread when the third get has started
    CountDownLatch fourthSignal = new CountDownLatch(1);
    // tells the test when all gets have returned
    CountDownLatch doneSignal = new CountDownLatch(3);
    String suffix = "Suffix";

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

    AtomicReferenceArray<String> result = new AtomicReferenceArray<>(2);

    LoadingCache<String, String> cache = CacheBuilder.newBuilder().build(computeFunction);
    String key = "bar";
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

    assertThat(callCount.get()).isEqualTo(1);
    assertThat(result.get(0)).isEqualTo(key);
    assertThat(result.get(1)).isEqualTo(key);
    assertThat(cache.getUnchecked(key)).isEqualTo(key + suffix);
  }

  static <T> Callable<T> throwing(Exception exception) {
    return new Callable<T>() {
      @Override
      public T call() throws Exception {
        throw exception;
      }
    };
  }
}
