/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;

import com.google.common.annotations.Beta;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * GWT emulation of {@link com.google.common.collect.ImmutableBiMap}.
 *
 * @author Hayward Chan
 */
public abstract class ImmutableBiMap<K, V> extends ForwardingImmutableMap<K, V>
    implements BiMap<K, V> {
  @Beta
  public static <T, K, V> Collector<T, ?, ImmutableBiMap<K, V>> toImmutableBiMap(
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction) {
    return CollectCollectors.toImmutableBiMap(keyFunction, valueFunction);
  }

  // Casting to any type is safe because the set will never hold any elements.
  @SuppressWarnings("unchecked")
  public static <K, V> ImmutableBiMap<K, V> of() {
    return (ImmutableBiMap<K, V>) RegularImmutableBiMap.EMPTY;
  }

  public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1) {
    checkEntryNotNull(k1, v1);
    return new SingletonImmutableBiMap<K, V>(k1, v1);
  }

  public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2) {
    return new RegularImmutableBiMap<K, V>(ImmutableMap.of(k1, v1, k2, v2));
  }

  public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
    return new RegularImmutableBiMap<K, V>(ImmutableMap.of(k1, v1, k2, v2, k3, v3));
  }

  public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    return new RegularImmutableBiMap<K, V>(ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4));
  }

  public static <K, V> ImmutableBiMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    return new RegularImmutableBiMap<K, V>(ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
  }

  public static <K, V> Builder<K, V> builder() {
    return new Builder<K, V>();
  }

  public static final class Builder<K, V> extends ImmutableMap.Builder<K, V> {

    public Builder() {}

    Builder(int initCapacity) {
      super(initCapacity);
    }

    @Override
    public Builder<K, V> put(K key, V value) {
      super.put(key, value);
      return this;
    }

    @Override
    public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      super.put(entry);
      return this;
    }

    @Override
    public Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
      super.putAll(map);
      return this;
    }

    @Override
    public Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
      super.putAll(entries);
      return this;
    }

    public Builder<K, V> orderEntriesByValue(Comparator<? super V> valueComparator) {
      super.orderEntriesByValue(valueComparator);
      return this;
    }

    Builder<K, V> combine(Builder<K, V> other) {
      super.combine(other);
      return this;
    }

    @Override
    public ImmutableBiMap<K, V> build() {
      ImmutableMap<K, V> map = super.build();
      if (map.isEmpty()) {
        return of();
      }
      return new RegularImmutableBiMap<K, V>(super.build());
    }

    @Override
    ImmutableBiMap<K, V> buildJdkBacked() {
      return build();
    }
  }

  public static <K, V> ImmutableBiMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
    if (map instanceof ImmutableBiMap) {
      @SuppressWarnings("unchecked") // safe since map is not writable
      ImmutableBiMap<K, V> bimap = (ImmutableBiMap<K, V>) map;
      return bimap;
    }

    if (map.isEmpty()) {
      return of();
    }

    ImmutableMap<K, V> immutableMap = ImmutableMap.copyOf(map);
    return new RegularImmutableBiMap<K, V>(immutableMap);
  }

  public static <K, V> ImmutableBiMap<K, V> copyOf(
      Iterable<? extends Entry<? extends K, ? extends V>> entries) {
    return new Builder<K, V>().putAll(entries).build();
  }

  ImmutableBiMap(Map<K, V> delegate) {
    super(delegate);
  }

  public abstract ImmutableBiMap<V, K> inverse();

  @Override
  public ImmutableSet<V> values() {
    return inverse().keySet();
  }

  public final V forcePut(K key, V value) {
    throw new UnsupportedOperationException();
  }
}
