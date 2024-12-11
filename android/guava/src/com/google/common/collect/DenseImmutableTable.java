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

import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableMap.IteratorBasedImmutableMap;
import com.google.errorprone.annotations.Immutable;
import com.google.j2objc.annotations.WeakOuter;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A {@code RegularImmutableTable} optimized for dense data. */
@GwtCompatible
@Immutable(containerOf = {"R", "C", "V"})
@ElementTypesAreNonnullByDefault
final class DenseImmutableTable<R, C, V> extends RegularImmutableTable<R, C, V> {
  private final ImmutableMap<R, Integer> rowKeyToIndex;
  private final ImmutableMap<C, Integer> columnKeyToIndex;
  private final ImmutableMap<R, ImmutableMap<C, V>> rowMap;
  private final ImmutableMap<C, ImmutableMap<R, V>> columnMap;

  @SuppressWarnings("Immutable") // We don't modify this after construction.
  private final int[] rowCounts;

  @SuppressWarnings("Immutable") // We don't modify this after construction.
  private final int[] columnCounts;

  @SuppressWarnings("Immutable") // We don't modify this after construction.
  private final @Nullable V[][] values;

  // For each cell in iteration order, the index of that cell's row key in the row key list.
  @SuppressWarnings("Immutable") // We don't modify this after construction.
  private final int[] cellRowIndices;

  // For each cell in iteration order, the index of that cell's column key in the column key list.
  @SuppressWarnings("Immutable") // We don't modify this after construction.
  private final int[] cellColumnIndices;

  DenseImmutableTable(
      ImmutableList<Cell<R, C, V>> cellList,
      ImmutableSet<R> rowSpace,
      ImmutableSet<C> columnSpace) {
    @SuppressWarnings("unchecked")
    @Nullable
    V[][] array = (@Nullable V[][]) new Object[rowSpace.size()][columnSpace.size()];
    this.values = array;
    this.rowKeyToIndex = Maps.indexMap(rowSpace);
    this.columnKeyToIndex = Maps.indexMap(columnSpace);
    rowCounts = new int[rowKeyToIndex.size()];
    columnCounts = new int[columnKeyToIndex.size()];
    int[] cellRowIndices = new int[cellList.size()];
    int[] cellColumnIndices = new int[cellList.size()];
    for (int i = 0; i < cellList.size(); i++) {
      Cell<R, C, V> cell = cellList.get(i);
      R rowKey = cell.getRowKey();
      C columnKey = cell.getColumnKey();
      // The requireNonNull calls are safe because we construct the indexes with indexMap.
      int rowIndex = requireNonNull(rowKeyToIndex.get(rowKey));
      int columnIndex = requireNonNull(columnKeyToIndex.get(columnKey));
      V existingValue = values[rowIndex][columnIndex];
      checkNoDuplicate(rowKey, columnKey, existingValue, cell.getValue());
      values[rowIndex][columnIndex] = cell.getValue();
      rowCounts[rowIndex]++;
      columnCounts[columnIndex]++;
      cellRowIndices[i] = rowIndex;
      cellColumnIndices[i] = columnIndex;
    }
    this.cellRowIndices = cellRowIndices;
    this.cellColumnIndices = cellColumnIndices;
    this.rowMap = new RowMap();
    this.columnMap = new ColumnMap();
  }

  /** An immutable map implementation backed by an indexed nullable array. */
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

    @CheckForNull
    abstract V getValue(int keyIndex);

    @Override
    ImmutableSet<K> createKeySet() {
      return isFull() ? keyToIndex().keySet() : super.createKeySet();
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    @CheckForNull
    public V get(@CheckForNull Object key) {
      Integer keyIndex = keyToIndex().get(key);
      return (keyIndex == null) ? null : getValue(keyIndex);
    }

    @Override
    UnmodifiableIterator<Entry<K, V>> entryIterator() {
      return new AbstractIterator<Entry<K, V>>() {
        private int index = -1;
        private final int maxIndex = keyToIndex().size();

        @Override
        @CheckForNull
        protected Entry<K, V> computeNext() {
          for (index++; index < maxIndex; index++) {
            V value = getValue(index);
            if (value != null) {
              return immutableEntry(getKey(index), value);
            }
          }
          return endOfData();
        }
      };
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @J2ktIncompatible // serialization
    @Override
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
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
    @CheckForNull
    V getValue(int keyIndex) {
      return values[rowIndex][keyIndex];
    }

    @Override
    boolean isPartialView() {
      return true;
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible // serialization
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
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
    @CheckForNull
    V getValue(int keyIndex) {
      return values[keyIndex][columnIndex];
    }

    @Override
    boolean isPartialView() {
      return true;
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible // serialization
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
    }
  }

  @WeakOuter
  private final class RowMap extends ImmutableArrayMap<R, ImmutableMap<C, V>> {
    private RowMap() {
      super(rowCounts.length);
    }

    @Override
    ImmutableMap<R, Integer> keyToIndex() {
      return rowKeyToIndex;
    }

    @Override
    ImmutableMap<C, V> getValue(int keyIndex) {
      return new Row(keyIndex);
    }

    @Override
    boolean isPartialView() {
      return false;
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible // serialization
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
    }
  }

  @WeakOuter
  private final class ColumnMap extends ImmutableArrayMap<C, ImmutableMap<R, V>> {
    private ColumnMap() {
      super(columnCounts.length);
    }

    @Override
    ImmutableMap<C, Integer> keyToIndex() {
      return columnKeyToIndex;
    }

    @Override
    ImmutableMap<R, V> getValue(int keyIndex) {
      return new Column(keyIndex);
    }

    @Override
    boolean isPartialView() {
      return false;
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible // serialization
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
    }
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
  @CheckForNull
  public V get(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
    Integer rowIndex = rowKeyToIndex.get(rowKey);
    Integer columnIndex = columnKeyToIndex.get(columnKey);
    return ((rowIndex == null) || (columnIndex == null)) ? null : values[rowIndex][columnIndex];
  }

  @Override
  public int size() {
    return cellRowIndices.length;
  }

  @Override
  Cell<R, C, V> getCell(int index) {
    int rowIndex = cellRowIndices[index];
    int columnIndex = cellColumnIndices[index];
    R rowKey = rowKeySet().asList().get(rowIndex);
    C columnKey = columnKeySet().asList().get(columnIndex);
    // requireNonNull is safe because we use indexes that were populated by the constructor.
    V value = requireNonNull(values[rowIndex][columnIndex]);
    return cellOf(rowKey, columnKey, value);
  }

  @Override
  V getValue(int index) {
    // requireNonNull is safe because we use indexes that were populated by the constructor.
    return requireNonNull(values[cellRowIndices[index]][cellColumnIndices[index]]);
  }

  @Override
  @J2ktIncompatible // serialization
  @GwtIncompatible // serialization
  Object writeReplace() {
    return SerializedForm.create(this, cellRowIndices, cellColumnIndices);
  }
}
