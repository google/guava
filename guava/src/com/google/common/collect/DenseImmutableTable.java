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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap.IteratorBasedImmutableMap;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A {@code RegularImmutableTable} optimized for dense data.
 */
@GwtCompatible
@Immutable
final class DenseImmutableTable<R, C, V>
    extends RegularImmutableTable<R, C, V> {
  private final ImmutableMap<R, Integer> rowKeyToIndex;
  private final ImmutableMap<C, Integer> columnKeyToIndex;
  private final ImmutableMap<R, Map<C, V>> rowMap;
  private final ImmutableMap<C, Map<R, V>> columnMap;
  private final int[] rowCounts;
  private final int[] columnCounts;
  private final V[][] values;
  private final int[] iterationOrderRow;
  private final int[] iterationOrderColumn;

  DenseImmutableTable(ImmutableList<Cell<R, C, V>> cellList,
      ImmutableSet<R> rowSpace, ImmutableSet<C> columnSpace) {
    @SuppressWarnings("unchecked")
    V[][] array = (V[][]) new Object[rowSpace.size()][columnSpace.size()];
    this.values = array;
    this.rowKeyToIndex = Maps.indexMap(rowSpace);
    this.columnKeyToIndex = Maps.indexMap(columnSpace);
    rowCounts = new int[rowKeyToIndex.size()];
    columnCounts = new int[columnKeyToIndex.size()];
    int[] iterationOrderRow = new int[cellList.size()];
    int[] iterationOrderColumn = new int[cellList.size()];
    for (int i = 0; i < cellList.size(); i++) {
      Cell<R, C, V> cell = cellList.get(i);
      R rowKey = cell.getRowKey();
      C columnKey = cell.getColumnKey();
      int rowIndex = rowKeyToIndex.get(rowKey);
      int columnIndex = columnKeyToIndex.get(columnKey);
      V existingValue = values[rowIndex][columnIndex];
      checkArgument(existingValue == null, "duplicate key: (%s, %s)", rowKey, columnKey);
      values[rowIndex][columnIndex] = cell.getValue();
      rowCounts[rowIndex]++;
      columnCounts[columnIndex]++;
      iterationOrderRow[i] = rowIndex;
      iterationOrderColumn[i] = columnIndex;
    }
    this.iterationOrderRow = iterationOrderRow;
    this.iterationOrderColumn = iterationOrderColumn;
    this.rowMap = new RowMap();
    this.columnMap = new ColumnMap();
  }

  /**
   * An immutable map implementation backed by an indexed nullable array.
   */
  private abstract static class ImmutableArrayMap<K, V> extends IteratorBasedImmutableMap<K, V> {
    private final int size;
  
    ImmutableArrayMap(int size) {
      this.size = size;
    }
  
    abstract ImmutableMap<K, Integer> keyToIndex();
  
    // True if getValue never returns null.
    private boolean isFull() {
      return size == keyToIndex().size();
    }
  
    K getKey(int index) {
      return keyToIndex().keySet().asList().get(index);
    }
  
    @Nullable abstract V getValue(int keyIndex);
  
    @Override
    ImmutableSet<K> createKeySet() {
      return isFull() ? keyToIndex().keySet() : super.createKeySet();
    }
  
    @Override
    public int size() {
      return size;
    }
  
    @Override
    public V get(@Nullable Object key) {
      Integer keyIndex = keyToIndex().get(key);
      return (keyIndex == null) ? null : getValue(keyIndex);
    }

    @Override
    UnmodifiableIterator<Entry<K, V>> entryIterator() {
      return new AbstractIterator<Entry<K, V>>() {
        private int index = -1;
        private final int maxIndex = keyToIndex().size();

        @Override
        protected Entry<K, V> computeNext() {
          for (index++; index < maxIndex; index++) {
            V value = getValue(index);
            if (value != null) {
              return Maps.immutableEntry(getKey(index), value);
            }
          }
          return endOfData();
        }
      };
        }
  }

  private final class Row extends ImmutableArrayMap<C, V> {
    private final int rowIndex;

    Row(int rowIndex) {
      super(rowCounts[rowIndex]);
      this.rowIndex = rowIndex;
    }

    @Override
    ImmutableMap<C, Integer> keyToIndex() {
      return columnKeyToIndex;
    }

    @Override
    V getValue(int keyIndex) {
      return values[rowIndex][keyIndex];
    }

    @Override
    boolean isPartialView() {
      return true;
    }
  }

  private final class Column extends ImmutableArrayMap<R, V> {
    private final int columnIndex;

    Column(int columnIndex) {
      super(columnCounts[columnIndex]);
      this.columnIndex = columnIndex;
    }

    @Override
    ImmutableMap<R, Integer> keyToIndex() {
      return rowKeyToIndex;
    }

    @Override
    V getValue(int keyIndex) {
      return values[keyIndex][columnIndex];
    }

    @Override
    boolean isPartialView() {
      return true;
    }
  }

  private final class RowMap extends ImmutableArrayMap<R, Map<C, V>> {
    private RowMap() {
      super(rowCounts.length);
    }

    @Override
    ImmutableMap<R, Integer> keyToIndex() {
      return rowKeyToIndex;
    }

    @Override
    Map<C, V> getValue(int keyIndex) {
      return new Row(keyIndex);
    }

    @Override
    boolean isPartialView() {
      return false;
    }
  }

  private final class ColumnMap extends ImmutableArrayMap<C, Map<R, V>> {
    private ColumnMap() {
      super(columnCounts.length);
    }

    @Override
    ImmutableMap<C, Integer> keyToIndex() {
      return columnKeyToIndex;
    }

    @Override
    Map<R, V> getValue(int keyIndex) {
      return new Column(keyIndex);
    }

    @Override
    boolean isPartialView() {
      return false;
    }
  }

  @Override public ImmutableMap<C, Map<R, V>> columnMap() {
    return columnMap;
  }

  @Override
  public ImmutableMap<R, Map<C, V>> rowMap() {
    return rowMap;
  }

  @Override public V get(@Nullable Object rowKey,
      @Nullable Object columnKey) {
    Integer rowIndex = rowKeyToIndex.get(rowKey);
    Integer columnIndex = columnKeyToIndex.get(columnKey);
    return ((rowIndex == null) || (columnIndex == null)) ? null
        : values[rowIndex][columnIndex];
  }

  @Override
  public int size() {
    return iterationOrderRow.length;
  }

  @Override
  Cell<R, C, V> getCell(int index) {
    int rowIndex = iterationOrderRow[index];
    int columnIndex = iterationOrderColumn[index];
    R rowKey = rowKeySet().asList().get(rowIndex);
    C columnKey = columnKeySet().asList().get(columnIndex);
    V value = values[rowIndex][columnIndex];
    return cellOf(rowKey, columnKey, value);
  }

  @Override
  V getValue(int index) {
    return values[iterationOrderRow[index]][iterationOrderColumn[index]];
  }
}
