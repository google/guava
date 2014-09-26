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

import com.google.common.annotations.GwtCompatible;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

/**
 * A {@code RegularImmutableTable} optimized for sparse data.
 */
@GwtCompatible
@Immutable
final class SparseImmutableTable<R, C, V>
    extends RegularImmutableTable<R, C, V> {

  private final ImmutableMap<R, Map<C, V>> rowMap;
  private final ImmutableMap<C, Map<R, V>> columnMap;
  private final int[] iterationOrderRow;
  private final int[] iterationOrderColumn;

  SparseImmutableTable(ImmutableList<Cell<R, C, V>> cellList,
      ImmutableSet<R> rowSpace, ImmutableSet<C> columnSpace) {
    Map<R, Integer> rowIndex = Maps.indexMap(rowSpace);
    Map<R, Map<C, V>> rows = Maps.newLinkedHashMap();
    for (R row : rowSpace) {
      rows.put(row, new LinkedHashMap<C, V>());
    }
    Map<C, Map<R, V>> columns = Maps.newLinkedHashMap();
    for (C col : columnSpace) {
      columns.put(col, new LinkedHashMap<R, V>());
    }
    int[] iterationOrderRow = new int[cellList.size()];
    int[] iterationOrderColumn = new int[cellList.size()];
    for (int i = 0; i < cellList.size(); i++) {
      Cell<R, C, V> cell = cellList.get(i);
      R rowKey = cell.getRowKey();
      C columnKey = cell.getColumnKey();
      V value = cell.getValue();
      
      iterationOrderRow[i] = rowIndex.get(rowKey);
      Map<C, V> thisRow = rows.get(rowKey);
      iterationOrderColumn[i] = thisRow.size();
      V oldValue = thisRow.put(columnKey, value);
      if (oldValue != null) {
        throw new IllegalArgumentException("Duplicate value for row=" + rowKey + ", column="
            + columnKey + ": " + value + ", " + oldValue);
      }
      columns.get(columnKey).put(rowKey, value);
    }
    this.iterationOrderRow = iterationOrderRow;
    this.iterationOrderColumn = iterationOrderColumn;
    ImmutableMap.Builder<R, Map<C, V>> rowBuilder = 
        new ImmutableMap.Builder<R, Map<C, V>>(rows.size());
    for (Map.Entry<R, Map<C, V>> row : rows.entrySet()) {
      rowBuilder.put(row.getKey(), ImmutableMap.copyOf(row.getValue()));
    }
    this.rowMap = rowBuilder.build();
    
    ImmutableMap.Builder<C, Map<R, V>> columnBuilder = 
        new ImmutableMap.Builder<C, Map<R, V>>(columns.size());
    for (Map.Entry<C, Map<R, V>> col : columns.entrySet()) {
      columnBuilder.put(col.getKey(), ImmutableMap.copyOf(col.getValue()));
    }
    this.columnMap = columnBuilder.build();
  }

  @Override public ImmutableMap<C, Map<R, V>> columnMap() {
    return columnMap;
  }

  @Override public ImmutableMap<R, Map<C, V>> rowMap() {
    return rowMap;
  }

  @Override
  public int size() {
    return iterationOrderRow.length;
  }
  
  @Override
  Cell<R, C, V> getCell(int index) {
    int rowIndex = iterationOrderRow[index];
    Map.Entry<R, Map<C, V>> rowEntry = rowMap.entrySet().asList().get(rowIndex);
    ImmutableMap<C, V> row = (ImmutableMap<C, V>) rowEntry.getValue();
    int columnIndex = iterationOrderColumn[index];
    Map.Entry<C, V> colEntry = row.entrySet().asList().get(columnIndex);
    return cellOf(rowEntry.getKey(), colEntry.getKey(), colEntry.getValue());
  }

  @Override
  V getValue(int index) {
    int rowIndex = iterationOrderRow[index];
    ImmutableMap<C, V> row = (ImmutableMap<C, V>) rowMap.values().asList().get(rowIndex);
    int columnIndex = iterationOrderColumn[index];
    return row.values().asList().get(columnIndex);
  }
}
