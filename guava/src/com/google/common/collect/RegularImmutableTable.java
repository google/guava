/*
 * Copyright (C) 2009 Google Inc.
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
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * An implementation of {@link ImmutableTable} holding an arbitrary number of
 * cells.
 *
 * @author gak@google.com (Gregory Kick)
 */
@GwtCompatible
abstract class RegularImmutableTable<R, C, V> extends ImmutableTable<R, C, V> {
  private final ImmutableSet<Cell<R, C, V>> cellSet;

  private RegularImmutableTable(ImmutableSet<Cell<R, C, V>> cellSet) {
    this.cellSet = cellSet;
  }

  private static final Function<Cell<Object, Object, Object>, Object>
      GET_VALUE_FUNCTION =
          new Function<Cell<Object, Object, Object>, Object>() {
            @Override public Object apply(Cell<Object, Object, Object> from) {
              return from.getValue();
            }
          };

  @SuppressWarnings("unchecked")
  private Function<Cell<R, C, V>, V> getValueFunction() {
    return (Function) GET_VALUE_FUNCTION;
  }

  @Nullable private transient volatile ImmutableList<V> valueList;

  @Override public final ImmutableCollection<V> values() {
    ImmutableList<V> result = valueList;
    if (result == null) {
      valueList = result = ImmutableList.copyOf(
          Iterables.transform(cellSet(), getValueFunction()));
    }
    return result;
  }

  @Override public final int size() {
    return cellSet().size();
  }

  @Override public final boolean containsValue(@Nullable Object value) {
    return values().contains(value);
  }

  @Override public final boolean isEmpty() {
    return false;
  }

  @Override public final ImmutableSet<Cell<R, C, V>> cellSet() {
    return cellSet;
  }

  static final <R, C, V> RegularImmutableTable<R, C, V> forCells(
      List<Cell<R, C, V>> cells,
      @Nullable final Comparator<? super R> rowComparator,
      @Nullable final Comparator<? super C> columnComparator) {
    checkNotNull(cells);
    if (rowComparator != null || columnComparator != null) {
      /*
       * This sorting logic leads to a cellSet() ordering that may not be
       * expected and that isn't documented in the Javadoc. If a row Comparator
       * is provided, cellSet() iterates across the columns in the first row,
       * the columns in the second row, etc. If a column Comparator is provided
       * but a row Comparator isn't, cellSet() iterates across the rows in the
       * first column, the rows in the second column, etc.
       */
      Comparator<Cell<R, C, V>> comparator = new Comparator<Cell<R, C, V>>() {
        @Override public int compare(Cell<R, C, V> cell1, Cell<R, C, V> cell2) {
          int rowCompare = (rowComparator == null) ? 0
            : rowComparator.compare(cell1.getRowKey(), cell2.getRowKey());
          if (rowCompare != 0) {
            return rowCompare;
          }
          return (columnComparator == null) ? 0
              : columnComparator.compare(
                  cell1.getColumnKey(), cell2.getColumnKey());
        }
      };
      Collections.sort(cells, comparator);
    }
    return forCellsInternal(cells, rowComparator, columnComparator);
  }

  static final <R, C, V> RegularImmutableTable<R, C, V> forCells(
      Iterable<Cell<R, C, V>> cells) {
    return forCellsInternal(cells, null, null);
  }

  /**
   * A factory that chooses the most space-efficient representation of the
   * table.
   */
  private static final <R, C, V> RegularImmutableTable<R, C, V>
      forCellsInternal(Iterable<Cell<R, C, V>> cells,
          @Nullable Comparator<? super R> rowComparator,
          @Nullable Comparator<? super C> columnComparator) {
    ImmutableSet.Builder<Cell<R, C, V>> cellSetBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<R> rowSpaceBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<C> columnSpaceBuilder = ImmutableSet.builder();
    for (Cell<R, C, V> cell : cells) {
      cellSetBuilder.add(cell);
      rowSpaceBuilder.add(cell.getRowKey());
      columnSpaceBuilder.add(cell.getColumnKey());
    }
    ImmutableSet<Cell<R, C, V>> cellSet = cellSetBuilder.build();

    ImmutableSet<R> rowSpace = rowSpaceBuilder.build();
    if (rowComparator != null) {
      List<R> rowList = Lists.newArrayList(rowSpace);
      Collections.sort(rowList, rowComparator);
      rowSpace = ImmutableSet.copyOf(rowList);
    }
    ImmutableSet<C> columnSpace = columnSpaceBuilder.build();
    if (columnComparator != null) {
      List<C> columnList = Lists.newArrayList(columnSpace);
      Collections.sort(columnList, columnComparator);
      columnSpace = ImmutableSet.copyOf(columnList);
    }

    // use a dense table if more than half of the cells have values
    // TODO(gak): tune this condition based on empirical evidence
    return (cellSet.size() > ((rowSpace.size() * columnSpace.size()) / 2 )) ?
        new DenseImmutableTable<R, C, V>(cellSet, rowSpace, columnSpace) :
        new SparseImmutableTable<R, C, V>(cellSet, rowSpace, columnSpace);
  }

  /**
   * A {@code RegularImmutableTable} optimized for sparse data.
   */
  @Immutable
  @VisibleForTesting
  static final class SparseImmutableTable<R, C, V>
      extends RegularImmutableTable<R, C, V> {

    private final ImmutableMap<R, Map<C, V>> rowMap;
    private final ImmutableMap<C, Map<R, V>> columnMap;

    /**
     * Creates a {@link Map} over the key space with
     * {@link ImmutableMap.Builder}s ready for values.
     */
    private static final <A, B, V> Map<A, ImmutableMap.Builder<B, V>>
        makeIndexBuilder(ImmutableSet<A> keySpace) {
      Map<A, ImmutableMap.Builder<B, V>> indexBuilder = Maps.newLinkedHashMap();
      for (A key : keySpace) {
        indexBuilder.put(key, ImmutableMap.<B, V>builder());
      }
      return indexBuilder;
    }

    /**
     * Builds the value maps of the index and creates an immutable copy of the
     * map.
     */
    private static final <A, B, V> ImmutableMap<A, Map<B, V>> buildIndex(
        Map<A, ImmutableMap.Builder<B, V>> indexBuilder) {
      return ImmutableMap.copyOf(Maps.transformValues(indexBuilder,
          new Function<ImmutableMap.Builder<B, V>, Map<B, V>>() {
            @Override public Map<B, V> apply(ImmutableMap.Builder<B, V> from) {
              return from.build();
            }
          }));
    }

    SparseImmutableTable(ImmutableSet<Cell<R, C, V>> cellSet,
        ImmutableSet<R> rowSpace, ImmutableSet<C> columnSpace) {
      super(cellSet);
      Map<R, ImmutableMap.Builder<C, V>> rowIndexBuilder
          = makeIndexBuilder(rowSpace);
      Map<C, ImmutableMap.Builder<R, V>> columnIndexBuilder
          = makeIndexBuilder(columnSpace);
      for (Cell<R, C, V> cell : cellSet) {
        R rowKey = cell.getRowKey();
        C columnKey = cell.getColumnKey();
        V value = cell.getValue();
        rowIndexBuilder.get(rowKey).put(columnKey, value);
        columnIndexBuilder.get(columnKey).put(rowKey, value);
      }
      this.rowMap = buildIndex(rowIndexBuilder);
      this.columnMap = buildIndex(columnIndexBuilder);
    }

    @Override public ImmutableMap<R, V> column(C columnKey) {
      checkNotNull(columnKey);
      // value maps are guaranteed to be immutable maps
      return Objects.firstNonNull((ImmutableMap<R, V>) columnMap.get(columnKey),
          ImmutableMap.<R, V>of());
    }

    @Override public ImmutableSet<C> columnKeySet() {
      return columnMap.keySet();
    }

    @Override public ImmutableMap<C, Map<R, V>> columnMap() {
      return columnMap;
    }

    @Override public ImmutableMap<C, V> row(R rowKey) {
      checkNotNull(rowKey);
      // value maps are guaranteed to be immutable maps
      return Objects.firstNonNull((ImmutableMap<C, V>) rowMap.get(rowKey),
          ImmutableMap.<C, V>of());
    }

    @Override public ImmutableSet<R> rowKeySet() {
      return rowMap.keySet();
    }

    @Override public ImmutableMap<R, Map<C, V>> rowMap() {
      return rowMap;
    }

    @Override public boolean contains(@Nullable Object rowKey,
        @Nullable Object columnKey) {
      Map<C, V> row = rowMap.get(rowKey);
      return (row != null) && row.containsKey(columnKey);
    }

    @Override public boolean containsColumn(@Nullable Object columnKey) {
      return columnMap.containsKey(columnKey);
    }

    @Override public boolean containsRow(@Nullable Object rowKey) {
      return rowMap.containsKey(rowKey);
    }

    @Override public V get(@Nullable Object rowKey,
        @Nullable Object columnKey) {
      Map<C, V> row = rowMap.get(rowKey);
      return (row == null) ? null : row.get(columnKey);
    }
  }

  /**
   * A {@code RegularImmutableTable} optimized for dense data.
   */
  @Immutable @VisibleForTesting
  static final class DenseImmutableTable<R, C, V>
      extends RegularImmutableTable<R, C, V> {

    private final ImmutableBiMap<R, Integer> rowKeyToIndex;
    private final ImmutableBiMap<C, Integer> columnKeyToIndex;
    private final V[][] values;

    private static <E> ImmutableBiMap<E, Integer> makeIndex(
        ImmutableSet<E> set) {
      ImmutableBiMap.Builder<E, Integer> indexBuilder =
          ImmutableBiMap.builder();
      int i = 0;
      for (E key : set) {
        indexBuilder.put(key, i);
        i++;
      }
      return indexBuilder.build();
    }

    DenseImmutableTable(ImmutableSet<Cell<R, C, V>> cellSet,
        ImmutableSet<R> rowSpace, ImmutableSet<C> columnSpace) {
      super(cellSet);
      @SuppressWarnings("unchecked")
      V[][] array = (V[][]) new Object[rowSpace.size()][columnSpace.size()];
      this.values = array;
      this.rowKeyToIndex = makeIndex(rowSpace);
      this.columnKeyToIndex = makeIndex(columnSpace);
      for (Cell<R, C, V> cell : cellSet) {
        R rowKey = cell.getRowKey();
        C columnKey = cell.getColumnKey();
        int rowIndex = rowKeyToIndex.get(rowKey);
        int columnIndex = columnKeyToIndex.get(columnKey);
        V existingValue = values[rowIndex][columnIndex];
        checkArgument(existingValue == null, "duplicate key: (%s, %s)", rowKey,
            columnKey);
        values[rowIndex][columnIndex] = cell.getValue();
      }
    }

    @Override public ImmutableMap<R, V> column(C columnKey) {
      checkNotNull(columnKey);
      Integer columnIndexInteger = columnKeyToIndex.get(columnKey);
      if (columnIndexInteger == null) {
        return ImmutableMap.of();
      } else {
        // unbox only once
        int columnIndex = columnIndexInteger;
        ImmutableMap.Builder<R, V> columnBuilder = ImmutableMap.builder();
        for (int i = 0; i < values.length; i++) {
          V value = values[i][columnIndex];
          if (value != null) {
            columnBuilder.put(rowKeyToIndex.inverse().get(i), value);
          }
        }
        return columnBuilder.build();
      }
    }

    @Override public ImmutableSet<C> columnKeySet() {
      return columnKeyToIndex.keySet();
    }

    private transient volatile ImmutableMap<C, Map<R, V>> columnMap;

    private ImmutableMap<C, Map<R, V>> makeColumnMap() {
      ImmutableMap.Builder<C, Map<R, V>> columnMapBuilder =
          ImmutableMap.builder();
      for (int c = 0; c < columnKeyToIndex.size(); c++) {
        ImmutableMap.Builder<R, V> rowMapBuilder = ImmutableMap.builder();
        for (int r = 0; r < rowKeyToIndex.size(); r++) {
          V value = values[r][c];
          if (value != null) {
            rowMapBuilder.put(rowKeyToIndex.inverse().get(r), value);
          }
        }
        columnMapBuilder.put(columnKeyToIndex.inverse().get(c),
            rowMapBuilder.build());
      }
      return columnMapBuilder.build();
    }

    @Override public ImmutableMap<C, Map<R, V>> columnMap() {
      ImmutableMap<C, Map<R, V>> result = columnMap;
      if (result == null) {
        columnMap = result = makeColumnMap();
      }
      return result;
    }

    @Override public boolean contains(@Nullable Object rowKey,
        @Nullable Object columnKey) {
      return (get(rowKey, columnKey) != null);
    }

    @Override public boolean containsColumn(@Nullable Object columnKey) {
      return columnKeyToIndex.containsKey(columnKey);
    }

    @Override public boolean containsRow(@Nullable Object rowKey) {
      return rowKeyToIndex.containsKey(rowKey);
    }

    @Override public V get(@Nullable Object rowKey,
        @Nullable Object columnKey) {
      Integer rowIndex = rowKeyToIndex.get(rowKey);
      Integer columnIndex = columnKeyToIndex.get(columnKey);
      return ((rowIndex == null) || (columnIndex == null)) ? null
          : values[rowIndex][columnIndex];
    }

    @Override public ImmutableMap<C, V> row(R rowKey) {
      checkNotNull(rowKey);
      Integer rowIndex = rowKeyToIndex.get(rowKey);
      if (rowIndex == null) {
        return ImmutableMap.of();
      } else {
        ImmutableMap.Builder<C, V> rowBuilder = ImmutableMap.builder();
        V[] row = values[rowIndex];
        for (int r = 0; r < row.length; r++) {
          V value = row[r];
          if (value != null) {
            rowBuilder.put(columnKeyToIndex.inverse().get(r), value);
          }
        }
        return rowBuilder.build();
      }
    }

    @Override public ImmutableSet<R> rowKeySet() {
      return rowKeyToIndex.keySet();
    }

    private transient volatile ImmutableMap<R, Map<C, V>> rowMap;

    private ImmutableMap<R, Map<C, V>> makeRowMap() {
      ImmutableMap.Builder<R, Map<C, V>> rowMapBuilder = ImmutableMap.builder();
      for (int r = 0; r < values.length; r++) {
        V[] row = values[r];
        ImmutableMap.Builder<C, V> columnMapBuilder = ImmutableMap.builder();
        for (int c = 0; c < row.length; c++) {
          V value = row[c];
          if (value != null) {
            columnMapBuilder.put(columnKeyToIndex.inverse().get(c), value);
          }
        }
        rowMapBuilder.put(rowKeyToIndex.inverse().get(r),
            columnMapBuilder.build());
      }
      return rowMapBuilder.build();
    }

    @Override public ImmutableMap<R, Map<C, V>> rowMap() {
      ImmutableMap<R, Map<C, V>> result = rowMap;
      if (result == null) {
        rowMap = result = makeRowMap();
      }
      return result;
    }
  }
}
