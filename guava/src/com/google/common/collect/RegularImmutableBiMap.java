/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * Bimap with two or more mappings.
 *
 * @author Jared Levy
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
  final transient ImmutableMap<K, V> delegate;
  final transient ImmutableBiMap<V, K> inverse;

  RegularImmutableBiMap(ImmutableMap<K, V> delegate) {
    checkArgument(delegate.size() >= 2);
    this.delegate = delegate;

    ImmutableMap.Builder<V, K> builder = ImmutableMap.builder();
    for (Entry<K, V> entry : delegate.entrySet()) {
      builder.put(entry.getValue(), entry.getKey());
    }
    ImmutableMap<V, K> backwardMap = builder.build();
    this.inverse = new RegularImmutableBiMap<V, K>(backwardMap, this);
  }

  RegularImmutableBiMap(ImmutableMap<K, V> delegate,
      ImmutableBiMap<V, K> inverse) {
    this.delegate = delegate;
    this.inverse = inverse;
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
  public V get(@Nullable Object key) {
    return delegate.get(key);
  }

  @Override
  public ImmutableSet<Entry<K, V>> entrySet() {
    return delegate.entrySet();
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    throw new AssertionError("should never be called");
  }

  @Override
  public ImmutableSet<K> keySet() {
    return delegate.keySet();
  }

  @Override
  public boolean equals(@Nullable Object object) {
    return this == object || delegate.equals(object);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override public ImmutableBiMap<V, K> inverse() {
    return inverse;
  }

  @Override boolean isPartialView() {
    return delegate.isPartialView();
  }
}
