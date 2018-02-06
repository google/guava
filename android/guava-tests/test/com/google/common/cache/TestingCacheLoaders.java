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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Utility {@link CacheLoader} implementations intended for use in testing.
 *
 * @author mike nonemacher
 */
@GwtCompatible(emulated = true)
class TestingCacheLoaders {

  /**
   * Returns a {@link CacheLoader} that implements a naive {@link CacheLoader#loadAll}, delegating
   * {@link CacheLoader#load} calls to {@code loader}.
   */
  static <K, V> CacheLoader<K, V> bulkLoader(final CacheLoader<K, V> loader) {
    checkNotNull(loader);
    return new CacheLoader<K, V>() {
      @Override
      public V load(K key) throws Exception {
        return loader.load(key);
      }

      @Override
      public Map<K, V> loadAll(Iterable<? extends K> keys) throws Exception {
        Map<K, V> result = Maps.newHashMap(); // allow nulls
        for (K key : keys) {
          result.put(key, load(key));
        }
        return result;
      }
    };
  }

  /** Returns a {@link CacheLoader} that returns the given {@code constant} for every request. */
  static <K, V> ConstantLoader<K, V> constantLoader(@NullableDecl V constant) {
    return new ConstantLoader<>(constant);
  }

  /** Returns a {@link CacheLoader} that returns the given {@code constant} for every request. */
  static IncrementingLoader incrementingLoader() {
    return new IncrementingLoader();
  }

  /** Returns a {@link CacheLoader} that throws the given error for every request. */
  static <K, V> CacheLoader<K, V> errorLoader(final Error e) {
    checkNotNull(e);
    return new CacheLoader<K, V>() {
      @Override
      public V load(K key) {
        throw e;
      }
    };
  }

  /** Returns a {@link CacheLoader} that throws the given exception for every request. */
  static <K, V> CacheLoader<K, V> exceptionLoader(final Exception e) {
    checkNotNull(e);
    return new CacheLoader<K, V>() {
      @Override
      public V load(K key) throws Exception {
        throw e;
      }
    };
  }

  /** Returns a {@link CacheLoader} that returns the key for every request. */
  static <T> IdentityLoader<T> identityLoader() {
    return new IdentityLoader<T>();
  }

  /**
   * Returns a {@code new Object()} for every request, and increments a counter for every request.
   * The count is accessible via {@link #getCount}.
   */
  static class CountingLoader extends CacheLoader<Object, Object> {
    private final AtomicInteger count = new AtomicInteger();

    @Override
    public Object load(Object from) {
      count.incrementAndGet();
      return new Object();
    }

    public int getCount() {
      return count.get();
    }
  }

  static final class ConstantLoader<K, V> extends CacheLoader<K, V> {
    private final V constant;

    ConstantLoader(V constant) {
      this.constant = constant;
    }

    @Override
    public V load(K key) {
      return constant;
    }
  }

  /**
   * Returns a {@code new Object()} for every request, and increments a counter for every request.
   * An {@code Integer} loader that returns the key for {@code load} requests, and increments the
   * old value on {@code reload} requests. The load counts are accessible via {@link #getLoadCount}
   * and {@link #getReloadCount}.
   */
  static class IncrementingLoader extends CacheLoader<Integer, Integer> {
    private final AtomicInteger countLoad = new AtomicInteger();
    private final AtomicInteger countReload = new AtomicInteger();

    @Override
    public Integer load(Integer key) {
      countLoad.incrementAndGet();
      return key;
    }

    @GwtIncompatible // reload
    @Override
    public ListenableFuture<Integer> reload(Integer key, Integer oldValue) {
      countReload.incrementAndGet();
      return Futures.immediateFuture(oldValue + 1);
    }

    public int getLoadCount() {
      return countLoad.get();
    }

    public int getReloadCount() {
      return countReload.get();
    }
  }

  static final class IdentityLoader<T> extends CacheLoader<T, T> {
    @Override
    public T load(T key) {
      return key;
    }
  }
}
