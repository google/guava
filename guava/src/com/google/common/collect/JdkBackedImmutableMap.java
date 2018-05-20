/*
 * Copyright (C) 2018 The Guava Authors
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
import static com.google.common.collect.RegularImmutableMap.makeImmutable;

import com.google.common.annotations.GwtCompatible;
import java.util.Map;
import java.util.function.BiConsumer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of ImmutableMap backed by a JDK HashMap, which has smartness protecting against
 * hash flooding.
 */
@GwtCompatible(emulated = true)
final class JdkBackedImmutableMap<K, V> extends ImmutableMap<K, V> {
  /**
   * Creates an {@code ImmutableMap} backed by a JDK HashMap. Used when probable hash flooding is
   * detected. This implementation may replace the entries in entryArray with its own entry objects
   * (though they will have the same key/value contents), and will take ownership of entryArray.
   */
  static <K, V> ImmutableMap<K, V> create(int n, Entry<K, V>[] entryArray) {
    Map<K, V> delegateMap = Maps.newHashMapWithExpectedSize(n);
    for (int i = 0; i < n; i++) {
      entryArray[i] = makeImmutable(entryArray[i]);
      V oldValue = delegateMap.putIfAbsent(entryArray[i].getKey(), entryArray[i].getValue());
      if (oldValue != null) {
        throw conflictException("key", entryArray[i], entryArray[i].getKey() + "=" + oldValue);
      }
    }
    return new JdkBackedImmutableMap<>(delegateMap, ImmutableList.asImmutableList(entryArray, n));
  }

  private final transient Map<K, V> delegateMap;
  private final transient ImmutableList<Entry<K, V>> entries;

  JdkBackedImmutableMap(Map<K, V> delegateMap, ImmutableList<Entry<K, V>> entries) {
    this.delegateMap = delegateMap;
    this.entries = entries;
  }

  @Override
  public int size() {
    return entries.size();
  }

  @Override
  public V get(@Nullable Object key) {
    return delegateMap.get(key);
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return new ImmutableMapEntrySet.RegularEntrySet<K, V>(this, entries);
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    checkNotNull(action);
    entries.forEach(e -> action.accept(e.getKey(), e.getValue()));
  }

  @Override
  ImmutableSet<K> createKeySet() {
    return new ImmutableMapKeySet<K, V>(this);
  }

  @Override
  ImmutableCollection<V> createValues() {
    return new ImmutableMapValues<K, V>(this);
  }

  @Override
  boolean isPartialView() {
    return false;
  }
}
