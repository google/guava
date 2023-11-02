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
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of ImmutableMap backed by a JDK HashMap, which has smartness protecting against
 * hash flooding.
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
final class JdkBackedImmutableMap<K, V> extends ImmutableMap<K, V> {
  /**
   * Creates an {@code ImmutableMap} backed by a JDK HashMap. Used when probable hash flooding is
   * detected. This implementation may replace the entries in entryArray with its own entry objects
   * (though they will have the same key/value contents), and will take ownership of entryArray.
   */
  static <K, V> ImmutableMap<K, V> create(
      int n, @Nullable Entry<K, V>[] entryArray, boolean throwIfDuplicateKeys) {
    Map<K, V> delegateMap = Maps.newHashMapWithExpectedSize(n);
    // If duplicates are allowed, this map will track the last value for each duplicated key.
    // A second pass will retain only the first entry for that key, but with this last value. The
    // value will then be replaced by null, signaling that later entries with the same key should
    // be deleted.
    Map<K, @Nullable V> duplicates = null;
    int dupCount = 0;
    for (int i = 0; i < n; i++) {
      // requireNonNull is safe because the first `n` elements have been filled in.
      entryArray[i] = makeImmutable(requireNonNull(entryArray[i]));
      K key = entryArray[i].getKey();
      V value = entryArray[i].getValue();
      V oldValue = delegateMap.put(key, value);
      if (oldValue != null) {
        if (throwIfDuplicateKeys) {
          throw conflictException("key", entryArray[i], entryArray[i].getKey() + "=" + oldValue);
        }
        if (duplicates == null) {
          duplicates = new HashMap<>();
        }
        duplicates.put(key, value);
        dupCount++;
      }
    }
    if (duplicates != null) {
      @SuppressWarnings({"rawtypes", "unchecked"})
      Entry<K, V>[] newEntryArray = new Entry[n - dupCount];
      for (int inI = 0, outI = 0; inI < n; inI++) {
        Entry<K, V> entry = requireNonNull(entryArray[inI]);
        K key = entry.getKey();
        if (duplicates.containsKey(key)) {
          V value = duplicates.get(key);
          if (value == null) {
            continue; // delete this duplicate
          }
          entry = new ImmutableMapEntry<>(key, value);
          duplicates.put(key, null);
        }
        newEntryArray[outI++] = entry;
      }
      entryArray = newEntryArray;
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
  @CheckForNull
  public V get(@CheckForNull Object key) {
    return delegateMap.get(key);
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return new ImmutableMapEntrySet.RegularEntrySet<>(this, entries);
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    checkNotNull(action);
    entries.forEach(e -> action.accept(e.getKey(), e.getValue()));
  }

  @Override
  ImmutableSet<K> createKeySet() {
    return new ImmutableMapKeySet<>(this);
  }

  @Override
  ImmutableCollection<V> createValues() {
    return new ImmutableMapValues<>(this);
  }

  @Override
  boolean isPartialView() {
    return false;
  }
}
