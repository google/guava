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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * An implementation of {@link ImmutableTable} that holds a single cell.
 *
 * @author Gregory Kick
 */
@GwtCompatible
class SingletonImmutableTable<R, C, V> extends ImmutableTable<R, C, V> {
  final R singleRowKey;
  final C singleColumnKey;
  final V singleValue;

  SingletonImmutableTable(R rowKey, C columnKey, V value) {
    this.singleRowKey = checkNotNull(rowKey);
    this.singleColumnKey = checkNotNull(columnKey);
    this.singleValue = checkNotNull(value);
  }

  SingletonImmutableTable(Cell<R, C, V> cell) {
    this(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
  }

  @Override public ImmutableSet<Cell<R, C, V>> cellSet() {
    return ImmutableSet.of(
        Tables.immutableCell(singleRowKey, singleColumnKey, singleValue));
  }

  @Override public ImmutableMap<R, V> column(C columnKey) {
    checkNotNull(columnKey);
    return containsColumn(columnKey)
        ? ImmutableMap.of(singleRowKey, singleValue)
        : ImmutableMap.<R, V>of();
  }

  @Override public ImmutableSet<C> columnKeySet() {
    return ImmutableSet.of(singleColumnKey);
  }

  @Override public ImmutableMap<C, Map<R, V>> columnMap() {
    return ImmutableMap.of(singleColumnKey,
        (Map<R, V>) ImmutableMap.of(singleRowKey, singleValue));
  }

  @Override public boolean contains(@Nullable Object rowKey,
      @Nullable Object columnKey) {
    return containsRow(rowKey) && containsColumn(columnKey);
  }

  @Override public boolean containsColumn(@Nullable Object columnKey) {
    return this.singleColumnKey.equals(columnKey);
  }

  @Override public boolean containsRow(@Nullable Object rowKey) {
    return this.singleRowKey.equals(rowKey);
  }

  @Override public boolean containsValue(@Nullable Object value) {
    return this.singleValue.equals(value);
  }

  @Override public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
    return contains(rowKey, columnKey) ? singleValue : null;
  }

  @Override public boolean isEmpty() {
    return false;
  }

  @Override public ImmutableMap<C, V> row(R rowKey) {
    checkNotNull(rowKey);
    return containsRow(rowKey)
        ? ImmutableMap.of(singleColumnKey, singleValue)
        : ImmutableMap.<C, V>of();
  }

  @Override public ImmutableSet<R> rowKeySet() {
    return ImmutableSet.of(singleRowKey);
  }

  @Override public ImmutableMap<R, Map<C, V>> rowMap() {
    return ImmutableMap.of(singleRowKey,
        (Map<C, V>) ImmutableMap.of(singleColumnKey, singleValue));
  }

  @Override public int size() {
    return 1;
  }

  @Override public ImmutableCollection<V> values() {
    return ImmutableSet.of(singleValue);
  }
}
