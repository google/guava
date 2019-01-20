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
import com.google.common.annotations.VisibleForTesting;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Bimap with zero or more mappings.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
final class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
  static final RegularImmutableBiMap<Object, Object> EMPTY = new RegularImmutableBiMap<>();

  private final transient int[] keyHashTable;
  @VisibleForTesting final transient Object[] alternatingKeysAndValues;
  private final transient int keyOffset; // 0 for K-to-V, 1 for V-to-K
  private final transient int size;
  private final transient RegularImmutableBiMap<V, K> inverse;

  /** Constructor for empty bimap. */
  @SuppressWarnings("unchecked")
  private RegularImmutableBiMap() {
    this.keyHashTable = null;
    this.alternatingKeysAndValues = new Object[0];
    this.keyOffset = 0;
    this.size = 0;
    this.inverse = (RegularImmutableBiMap<V, K>) this;
  }

  /** K-to-V constructor. */
  RegularImmutableBiMap(Object[] alternatingKeysAndValues, int size) {
    this.alternatingKeysAndValues = alternatingKeysAndValues;
    this.size = size;
    this.keyOffset = 0;
    int tableSize = (size >= 2) ? ImmutableSet.chooseTableSize(size) : 0;
    this.keyHashTable =
        RegularImmutableMap.createHashTable(alternatingKeysAndValues, size, tableSize, 0);
    int[] valueHashTable =
        RegularImmutableMap.createHashTable(alternatingKeysAndValues, size, tableSize, 1);
    this.inverse =
        new RegularImmutableBiMap<V, K>(valueHashTable, alternatingKeysAndValues, size, this);
  }

  /** V-to-K constructor. */
  private RegularImmutableBiMap(
      int[] valueHashTable,
      Object[] alternatingKeysAndValues,
      int size,
      RegularImmutableBiMap<V, K> inverse) {
    this.keyHashTable = valueHashTable;
    this.alternatingKeysAndValues = alternatingKeysAndValues;
    this.keyOffset = 1;
    this.size = size;
    this.inverse = inverse;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public ImmutableBiMap<V, K> inverse() {
    return inverse;
  }

  @SuppressWarnings("unchecked")
  @Override
  public V get(@NullableDecl Object key) {
    return (V)
        RegularImmutableMap.get(keyHashTable, alternatingKeysAndValues, size, keyOffset, key);
  }

  @Override
  ImmutableSet<Entry<K, V>> createEntrySet() {
    return new RegularImmutableMap.EntrySet<K, V>(this, alternatingKeysAndValues, keyOffset, size);
  }

  @Override
  ImmutableSet<K> createKeySet() {
    @SuppressWarnings("unchecked")
    ImmutableList<K> keyList =
        (ImmutableList<K>)
            new RegularImmutableMap.KeysOrValuesAsList(alternatingKeysAndValues, keyOffset, size);
    return new RegularImmutableMap.KeySet<>(this, keyList);
  }

  @Override
  boolean isPartialView() {
    return false;
  }
}
