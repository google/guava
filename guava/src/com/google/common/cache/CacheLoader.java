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
import com.google.common.base.Function;
import com.google.common.base.Supplier;

import java.io.Serializable;

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
public abstract class CacheLoader<K, V> {

  /**
   * Computes or retrieves the value corresponding to {@code key}.
   *
   * @param key the non-null key whose value should be loaded
   * @return the value associated with {@code key}; <b>may not be null</b>
   */
  public abstract V load(K key) throws Exception;

  /**
   * Computes or retrieves a replacement value corresponding to an already-cached {@code key}. This
   * method is called when an existing cache entry is refreshed, such as through a call to
   * {@link Cache#refresh}.
   *
   * <p>This implementation simply delegates to {@link #load}. This method should be overriden when
   * the new value can be computed more efficiently from the old value.
   *
   * @param key the non-null key whose value should be loaded
   * @param oldValue the non-null old value corresponding to {@code key}
   * @return the new value associated with {@code key}; <b>may not be null</b>
   * @since 11.0
   */
  public V reload(K key, V oldValue) throws Exception {
    return load(key);
  }

  // TODO(fry): loadAll

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

}
