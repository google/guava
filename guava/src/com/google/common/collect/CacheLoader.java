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

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Supplier;

import java.io.Serializable;
import javax.annotation.Nullable;

/**
 * Computes or retrieves values, based on a key, for use in populating a {@Cache}.
 *
 * @author Charles Fry
 * @since Guava release 10
 */
@Beta
public abstract class CacheLoader<K, V> {

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

    public V load(Object key) {
      return computingSupplier.get();
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Computes or retrieves the value corresponding to {@code key}.
   *
   * @return the value associated with {@code key}, or {code null} if the value can't be loaded
   * @throws NullPointerException if {@code key} is {@code null} and this loader does not accept
   *     {@code null} keys; note that some cache implementations, including those produced by
   *     {@link MapMaker}, will guarantee never to pass {@code null} into this method
   */
  @Nullable
  public abstract V load(@Nullable K key) throws Exception;

  // TODO(user): loadAll

}
