/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap.IteratorBasedImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.Serializable;
import java.util.EnumMap;

import javax.annotation.Nullable;

/**
 * Implementation of {@link ImmutableMap} backed by a non-empty {@link
 * java.util.EnumMap}.
 *
 * @author Louis Wasserman
 * @author Lovro Pandzic
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public final class ImmutableEnumMap<K extends Enum<K>, V> extends IteratorBasedImmutableMap<K, V> {
  static <K extends Enum<K>, V> ImmutableMap<K, V> asImmutable(EnumMap<K, V> map) {
    switch (map.size()) {
      case 0:
        return ImmutableMap.of();
      case 1:
        Entry<K, V> entry = Iterables.getOnlyElement(map.entrySet());
        return ImmutableMap.of(entry.getKey(), entry.getValue());
      default:
        return new ImmutableEnumMap<K, V>(map);
    }
  }

  public static <K extends Enum<K>, V> ImmutableEnumMap<K, V> forAllKeys(K k1, V v1) {

    return builder(k1, v1).requireAllEnumKeys();
  }

  public static <K extends Enum<K>, V> ImmutableEnumMap<K, V> forAllKeys(K k1, V v1, K k2, V v2) {

    return builder(k1, v1).put(k2, v2).requireAllEnumKeys();
  }

  public static <K extends Enum<K>, V> ImmutableEnumMap<K, V> forAllKeys(K k1, V v1, K k2, V v2, K k3, V v3) {

    return builder(k1, v1).put(k2, v2).put(k3, v3).requireAllEnumKeys();
  }

  public static <K extends Enum<K>, V> ImmutableEnumMap<K, V> forAllKeys(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {

    return builder(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).requireAllEnumKeys();
  }

  public static <K extends Enum<K>, V> ImmutableEnumMap<K, V> forAllKeys(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {

    return builder(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).put(k5, v5).requireAllEnumKeys();
  }

  public static <K extends Enum<K>, V> ImmutableEnumMap<K, V> of(K k1, V v1) {

    return builder(k1, v1).build();
  }

  public static <K extends Enum<K>, V> ImmutableEnumMap<K, V> of(K k1, V v1, K k2, V v2) {

    return builder(k1, v1).put(k2, v2).build();
  }

  public static <K extends Enum<K>, V> ImmutableEnumMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {

    return builder(k1, v1).put(k2, v2).put(k3, v3).build();
  }

  public static <K extends Enum<K>, V> ImmutableEnumMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {

    return builder(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).build();
  }

  public static <K extends Enum<K>, V> ImmutableEnumMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {

    return builder(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).put(k5, v5).build();
  }

  public static <K extends Enum<K>, V> Builder<K, V> builder(K k1, V v1) {
    return new Builder<K, V>(k1, v1);
  }

  private final transient EnumMap<K, V> delegate;

  private ImmutableEnumMap(EnumMap<K, V> delegate) {
    this.delegate = delegate;
    checkArgument(!delegate.isEmpty());
  }

  @Override
  UnmodifiableIterator<K> keyIterator() {
    return Iterators.unmodifiableIterator(delegate.keySet().iterator());
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return delegate.containsKey(key);
  }

  @Override
  public V get(Object key) {
    return delegate.get(key);
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof ImmutableEnumMap) {
      object = ((ImmutableEnumMap<?, ?>) object).delegate;
    }
    return delegate.equals(object);
  }

  @Override
  UnmodifiableIterator<Entry<K, V>> entryIterator() {
    return Maps.unmodifiableEntryIterator(delegate.entrySet().iterator());
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  // All callers of the constructor are restricted to <K extends Enum<K>>.
  @Override
  Object writeReplace() {
    return new EnumSerializedForm<K, V>(delegate);
  }

  /*
   * This class is used to serialize ImmutableEnumMap instances.
   */
  private static class EnumSerializedForm<K extends Enum<K>, V> implements Serializable {
    final EnumMap<K, V> delegate;

    EnumSerializedForm(EnumMap<K, V> delegate) {
      this.delegate = delegate;
    }

    Object readResolve() {
      return new ImmutableEnumMap<K, V>(delegate);
    }

    private static final long serialVersionUID = 0;
  }

  public static final class Builder<K extends Enum<K>, V> {

    private final EnumMap<K, V> enumMap;
    private final Class<K> keyType;

    /**
     * Creates a new builder.
     *
     * @see ImmutableEnumMap#builder
     */
    Builder(K key, V value) {
      CollectPreconditions.checkEntryNotNull(key, value);
      keyType = key.getDeclaringClass();
      enumMap = new EnumMap<K, V>(key.getDeclaringClass());
      enumMap.put(key, value);
    }

    /**
     * Associates {@code key} with {@code value} in the built map.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @throws IllegalArgumentException if key or value is null or key is a duplicate
     */
    @CanIgnoreReturnValue
    public Builder<K, V> put(K key, V value) {
      CollectPreconditions.checkEntryNotNull(key, value);
      checkArgument(!enumMap.containsKey(key), key + " is already present in " + enumMap);
      enumMap.put(key, value);
      return this;
    }

    /**
     * Creates a new {@link ImmutableEnumMap}.
     *
     * @throws IllegalArgumentException if there are missing enum keys.
     */
    public ImmutableEnumMap<K, V> requireAllEnumKeys() {

      K[] requiredEnumKeys = keyType.getEnumConstants();

      for (K requiredEnumKey : requiredEnumKeys) {
        checkState(enumMap.containsKey(requiredEnumKey), enumMap + " does not contain entry for " + requiredEnumKey);
      }

      return build();
    }

    /**
     * Creates a new {@link ImmutableEnumMap}.
     */
    public ImmutableEnumMap<K, V> build() {
      return new ImmutableEnumMap<K, V>(enumMap);
    }
  }
}
