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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility {@link CacheLoader} implementations intended for use in testing.
 *
 * @author mike nonemacher
 */
class TestingCacheLoaders {

  /**
   * Returns a {@link CacheLoader} that returns the given {@code constant} for every request.
   */
  static <K, V> ConstantLoader<K, V> constantLoader(V constant) {
    return new ConstantLoader<K, V>(constant);
  }

  /**
   * Returns a {@link CacheLoader} that throws the given error for every request.
   */
  static <K, V> CacheLoader<K, V> errorLoader(final Error e) {
    return new CacheLoader<K, V>() {

      @Override
      public V load(K key) {
        throw e;
      }
    };
  }

  /**
   * Returns a {@link CacheLoader} that throws the given exception for every request.
   */
  static <K, V> CacheLoader<K, V> exceptionLoader(final Exception e) {
    return new CacheLoader<K, V>() {

      @Override
      public V load(K key) throws Exception {
        throw e;
      }
    };
  }

  /**
   * Returns a {@link CacheLoader} that returns the key for every request.
   */
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

  static final class IdentityLoader<T> extends CacheLoader<T, T> {
    @Override
    public T load(T key) {
      return key;
    }
  }
}
