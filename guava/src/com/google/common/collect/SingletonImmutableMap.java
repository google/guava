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

import com.google.common.annotations.GwtCompatible;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Implementation of {@link ImmutableMap} with exactly one entry.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
final class SingletonImmutableMap<K, V> extends ImmutableMap<K, V> {

  final transient K singleKey;
  final transient V singleValue;

  SingletonImmutableMap(K singleKey, V singleValue) {
    this.singleKey = singleKey;
    this.singleValue = singleValue;
  }

  SingletonImmutableMap(Entry<K, V> entry) {
    this(entry.getKey(), entry.getValue());
  }

  @Override public V get(@Nullable Object key) {
    return singleKey.equals(key) ? singleValue : null;
  }

  @Override
  public int size() {
    return 1;
  }

  @Override public boolean isEmpty() {
    return false;
  }

  @Override public boolean containsKey(@Nullable Object key) {
    return singleKey.equals(key);
  }

  @Override public boolean containsValue(@Nullable Object value) {
    return singleValue.equals(value);
  }

  @Override boolean isPartialView() {
    return false;
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return ImmutableSet.of(Maps.immutableEntry(singleKey, singleValue));
  }

  @Override
  ImmutableSet<K> createKeySet() {
    return ImmutableSet.of(singleKey);
  }

  @Override
  ImmutableCollection<V> createValues() {
    return ImmutableList.of(singleValue);
  }

  @Override public boolean equals(@Nullable Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof Map) {
      Map<?, ?> that = (Map<?, ?>) object;
      if (that.size() == 1) {
        Entry<?, ?> entry = that.entrySet().iterator().next();
        return singleKey.equals(entry.getKey())
            && singleValue.equals(entry.getValue());
      }
    }
    return false;
  }

  @Override public int hashCode() {
    return singleKey.hashCode() ^ singleValue.hashCode();
  }
}
