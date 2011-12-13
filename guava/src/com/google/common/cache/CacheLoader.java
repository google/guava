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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.Serializable;
import java.util.Map;

/**
 * Computes or retrieves values, based on a key, for use in populating a {@code Cache}.
 *
 * <p>Most implementations will only need to implement {@link #load}. Other methods may be
 * overridden as desired.
 *
 * @author Charles Fry
 * @since 10.0
 */
@Beta
@GwtCompatible(emulated = true)
public abstract class CacheLoader<K, V> {
  /**
   * Constructor for use by subclasses.
   */
  protected CacheLoader() {}

  /**
   * Computes or retrieves the value corresponding to {@code key}.
   *
   * @param key the non-null key whose value should be loaded
   * @return the value associated with {@code key}; <b>must not be null</b>
   */
  public abstract V load(K key) throws Exception;

  /**
   * Computes or retrieves a replacement value corresponding to an already-cached {@code key}. This
   * method is called when an existing cache entry is refreshed by
   * {@link CacheBuilder#refreshAfterWrite}, or through a call to {@link Cache#refresh}.
   *
   * <p>This implementation synchronously delegates to {@link #load}. It is recommended that it be
   * overridden with an asynchronous implementation when using
   * {@link CacheBuilder#refreshAfterWrite}.
   *
   * <p><b>Note:</b> <i>all exceptions thrown by this method will be logged and then swallowed</i>.
   *
   * @param key the non-null key whose value should be loaded
   * @param oldValue the non-null old value corresponding to {@code key}
   * @return the future new value associated with {@code key};
   *     <b>must not be null, must not return null</b>
   * @since 11.0
   */
  @GwtIncompatible("Futures")
  public ListenableFuture<V> reload(K key, V oldValue) throws Exception {
    return Futures.immediateFuture(load(key));
  }

  /**
   * Computes or retrieves the values corresponding to {@code keys}. This method is called by
   * {@link Cache#getAll}.
   *
   * <p>If the returned map doesn't contain all requested {@code keys} then the entries it does
   * contain will be cached, but {@code getAll} will throw an exception. If the returned map
   * contains extra keys not present in {@code keys} then all returned entries will be cached,
   * but only the entries for {@code keys} will be returned from {@code getAll}.
   *
   * <p>This method should be overriden when bulk retrieval is significantly more efficient than
   * many individual lookups. Note that {@link Cache#getAll} will defer to individual calls to
   * {@link Cache#get} if this method is not overriden.
   *
   * @param keys the unique, non-null keys whose values should be loaded
   * @return a map from each key in {@code keys} to the value associated with that key;
   *     <b>may not contain null values</b>
   * @since 11.0
   */
  public Map<K, V> loadAll(Iterable<? extends K> keys) throws Exception {
    // This will be caught by getAll(), causing it to fall back to multiple calls to Cache.get
    throw new UnsupportedLoadingOperationException();
  }

  /**
   * Returns a {@code CacheLoader} which creates values by applying a {@code Function} to the key.
   */
  public static <K, V> CacheLoader<K, V> from(Function<K, V> function) {
    return new FunctionToCacheLoader<K, V>(function);
  }

  private static final class FunctionToCacheLoader<K, V>
      extends CacheLoader<K, V> implements Serializable {
    private final Function<K, V> computingFunction;

    public FunctionToCacheLoader(Function<K, V> computingFunction) {
      this.computingFunction = checkNotNull(computingFunction);
    }

    @Override
    public V load(K key) {
      return computingFunction.apply(key);
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a {@code CacheLoader} which obtains values from a {@code Supplier} (independent of the
   * key).
   */
  public static <V> CacheLoader<Object, V> from(Supplier<V> supplier) {
    return new SupplierToCacheLoader<V>(supplier);
  }

  private static final class SupplierToCacheLoader<V>
      extends CacheLoader<Object, V> implements Serializable {
    private final Supplier<V> computingSupplier;

    public SupplierToCacheLoader(Supplier<V> computingSupplier) {
      this.computingSupplier = checkNotNull(computingSupplier);
    }

    @Override
    public V load(Object key) {
      return computingSupplier.get();
    }

    private static final long serialVersionUID = 0;
  }

  static final class UnsupportedLoadingOperationException extends UnsupportedOperationException {}

  /**
   * Thrown to indicate that an invalid response was returned from a call to {@link CacheLoader}.
   *
   * @since 11.0
   */
  public static final class InvalidCacheLoadException extends RuntimeException {
    public InvalidCacheLoadException(String message) {
      super(message);
    }
  }
}
