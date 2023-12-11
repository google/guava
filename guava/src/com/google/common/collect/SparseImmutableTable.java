/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.Immutable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/** A {@code RegularImmutableTable} optimized for sparse data. */
@GwtCompatible
@Immutable(containerOf = {"R", "C", "V"})
@ElementTypesAreNonnullByDefault
final class SparseImmutableTable<R, C, V> extends RegularImmutableTable<R, C, V> {
  static final ImmutableTable<Object, Object, Object> EMPTY =
      new SparseImmutableTable<>(
          ImmutableList.<Cell<Object, Object, Object>>of(), ImmutableSet.of(), ImmutableSet.of());

  private final ImmutableMap<R, ImmutableMap<C, V>> rowMap;
  private final ImmutableMap<C, ImmutableMap<R, V>> columnMap;

  // For each cell in iteration order, the index of that cell's row key in the row key list.
  @SuppressWarnings("Immutable") // We don't modify this after construction.
  private final int[] cellRowIndices;

  // For each cell in iteration order, the index of that cell's column key in the list of column
  // keys present in that row.
  @SuppressWarnings("Immutable") // We don't modify this after construction.
  private final int[] cellColumnInRowIndices;

  SparseImmutableTable(
      ImmutableList<Cell<R, C, V>> cellList,
      ImmutableSet<R> rowSpace,
      ImmutableSet<C> columnSpace) {
    Map<R, Integer> rowIndex = Maps.indexMap(rowSpace);
    Map<R, Map<C, V>> rows = Maps.newLinkedHashMap();
    for (R row : rowSpace) {
      rows.put(row, new LinkedHashMap<C, V>());
    }
    Map<C, Map<R, V>> columns = Maps.newLinkedHashMap();
    for (C col : columnSpace) {
      columns.put(col, new LinkedHashMap<R, V>());
    }
    int[] cellRowIndices = new int[cellList.size()];
    int[] cellColumnInRowIndices = new int[cellList.size()];
    for (int i = 0; i < cellList.size(); i++) {
      Cell<R, C, V> cell = cellList.get(i);
      R rowKey = cell.getRowKey();
      C columnKey = cell.getColumnKey();
      V value = cell.getValue();

      /*
       * These requireNonNull calls are safe because we construct the maps to hold all the provided
       * cells.
       */
      cellRowIndices[i] = requireNonNull(rowIndex.get(rowKey));
      Map<C, V> thisRow = requireNonNull(rows.get(rowKey));
      cellColumnInRowIndices[i] = thisRow.size();
      V oldValue = thisRow.put(columnKey, value);
      checkNoDuplicate(rowKey, columnKey, oldValue, value);
      requireNonNull(columns.get(columnKey)).put(rowKey, value);
    }
    this.cellRowIndices = cellRowIndices;
    this.cellColumnInRowIndices = cellColumnInRowIndices;
    ImmutableMap.Builder<R, ImmutableMap<C, V>> rowBuilder =
        new ImmutableMap.Builder<>(rows.size());
    for (Entry<R, Map<C, V>> row : rows.entrySet()) {
      rowBuilder.put(row.getKey(), ImmutableMap.copyOf(row.getValue()));
    }
    this.rowMap = rowBuilder.buildOrThrow();

    ImmutableMap.Builder<C, ImmutableMap<R, V>> columnBuilder =
        new ImmutableMap.Builder<>(columns.size());
    for (Entry<C, Map<R, V>> col : columns.entrySet()) {
      columnBuilder.put(col.getKey(), ImmutableMap.copyOf(col.getValue()));
    }
    this.columnMap = columnBuilder.buildOrThrow();
  }

  @Override
  public ImmutableMap<C, Map<R, V>> columnMap() {
    // Casts without copying.
    ImmutableMap<C, ImmutableMap<R, V>> columnMap = this.columnMap;
    return ImmutableMap.<C, Map<R, V>>copyOf(columnMap);
  }

  @Override
  public ImmutableMap<R, Map<C, V>> rowMap() {
    // Casts without copying.
    ImmutableMap<R, ImmutableMap<C, V>> rowMap = this.rowMap;
    return ImmutableMap.<R, Map<C, V>>copyOf(rowMap);
  }

  @Override
  public int size() {
    return cellRowIndices.length;
  }

  @Override
  Cell<R, C, V> getCell(int index) {
    int rowIndex = cellRowIndices[index];
    Entry<R, ImmutableMap<C, V>> rowEntry = rowMap.entrySet().asList().get(rowIndex);
    ImmutableMap<C, V> row = rowEntry.getValue();
    int columnIndex = cellColumnInRowIndices[index];
    Entry<C, V> colEntry = row.entrySet().asList().get(columnIndex);
    return cellOf(rowEntry.getKey(), colEntry.getKey(), colEntry.getValue());
  }

  @Override
  V getValue(int index) {
    int rowIndex = cellRowIndices[index];
    ImmutableMap<C, V> row = rowMap.values().asList().get(rowIndex);
    int columnIndex = cellColumnInRowIndices[index];
    return row.values().asList().get(columnIndex);
  }

  @Override
  @J2ktIncompatible // serialization
  @GwtIncompatible // serialization
  Object writeReplace() {
    Map<C, Integer> columnKeyToIndex = Maps.indexMap(columnKeySet());
    int[] cellColumnIndices = new int[cellSet().size()];
    int i = 0;
    for (Cell<R, C, V> cell : cellSet()) {
      // requireNonNull is safe because the cell exists in the table.
      cellColumnIndices[i++] = requireNonNull(columnKeyToIndex.get(cell.getColumnKey()));
    }
    return SerializedForm.create(this, cellRowIndices, cellColumnIndices);
  }
}
