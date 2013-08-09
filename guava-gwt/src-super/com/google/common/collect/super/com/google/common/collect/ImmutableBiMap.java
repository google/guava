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

import java.util.Map;

/**
 * GWT emulation of {@link ImmutableBiMap}.
 *
 * @author Hayward Chan
 */
public abstract class ImmutableBiMap<K, V> extends ForwardingImmutableMap<K, V>
    implements BiMap<K, V> {

  // Casting to any type is safe because the set will never hold any elements.
  @SuppressWarnings("unchecked")
  public static <K, V> ImmutableBiMap<K, V> of() {
    return (ImmutableBiMap<K, V>) EmptyImmutableBiMap.INSTANCE;
  }

  public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1) {
    checkEntryNotNull(k1, v1);
    return new SingletonImmutableBiMap<K, V>(k1, v1);
  }

  public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2) {
    return new RegularImmutableBiMap<K, V>(ImmutableMap.of(k1, v1, k2, v2));
  }

  public static <K, V> ImmutableBiMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3) {
    return new RegularImmutableBiMap<K, V>(ImmutableMap.of(
        k1, v1, k2, v2, k3, v3));
  }

  public static <K, V> ImmutableBiMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    return new RegularImmutableBiMap<K, V>(ImmutableMap.of(
        k1, v1, k2, v2, k3, v3, k4, v4));
  }

  public static <K, V> ImmutableBiMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    return new RegularImmutableBiMap<K, V>(ImmutableMap.of(
        k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
  }

  public static <K, V> Builder<K, V> builder() {
    return new Builder<K, V>();
  }

  public static final class Builder<K, V> extends ImmutableMap.Builder<K, V> {

    public Builder() {}

    @Override public Builder<K, V> put(K key, V value) {
      super.put(key, value);
      return this;
    }

    @Override public Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
      super.putAll(map);
      return this;
    }

    @Override public ImmutableBiMap<K, V> build() {
      ImmutableMap<K, V> map = super.build();
      if (map.isEmpty()) {
        return of();
      }
      return new RegularImmutableBiMap<K, V>(super.build());
    }
  }

  public static <K, V> ImmutableBiMap<K, V> copyOf(
      Map<? extends K, ? extends V> map) {
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

  ImmutableBiMap(Map<K, V> delegate) {
    super(delegate);
  }

  public abstract ImmutableBiMap<V, K> inverse();

  @Override public ImmutableSet<V> values() {
    return inverse().keySet();
  }

  public final V forcePut(K key, V value) {
    throw new UnsupportedOperationException();
  }
}
