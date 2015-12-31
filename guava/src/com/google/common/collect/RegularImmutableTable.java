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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Table.Cell;
import com.google.j2objc.annotations.WeakOuter;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * An implementation of {@link ImmutableTable} holding an arbitrary number of
 * cells.
 *
 * @author Gregory Kick
 */
@GwtCompatible
abstract class RegularImmutableTable<R, C, V> extends ImmutableTable<R, C, V> {
  RegularImmutableTable() {}

  abstract Cell<R, C, V> getCell(int iterationIndex);

  @Override
  final ImmutableSet<Cell<R, C, V>> createCellSet() {
    return isEmpty() ? ImmutableSet.<Cell<R, C, V>>of() : new CellSet();
  }

  @WeakOuter
  private final class CellSet extends ImmutableSet.Indexed<Cell<R, C, V>> {
    @Override
    public int size() {
      return RegularImmutableTable.this.size();
    }

    @Override
    Cell<R, C, V> get(int index) {
      return getCell(index);
    }

    @Override
    public boolean contains(@Nullable Object object) {
      if (object instanceof Cell) {
        Cell<?, ?, ?> cell = (Cell<?, ?, ?>) object;
        Object value = RegularImmutableTable.this.get(cell.getRowKey(), cell.getColumnKey());
        return value != null && value.equals(cell.getValue());
      }
      return false;
    }

    @Override
    boolean isPartialView() {
      return false;
    }
  }

  abstract V getValue(int iterationIndex);

  @Override
  final ImmutableCollection<V> createValues() {
    return isEmpty() ? ImmutableList.<V>of() : new Values();
  }

  @WeakOuter
  private final class Values extends ImmutableList<V> {
    @Override
    public int size() {
      return RegularImmutableTable.this.size();
    }

    @Override
    public V get(int index) {
      return getValue(index);
    }

    @Override
    boolean isPartialView() {
      return true;
    }
  }

  static <R, C, V> RegularImmutableTable<R, C, V> forCells(
      List<Cell<R, C, V>> cells,
      @Nullable final Comparator<? super R> rowComparator,
      @Nullable final Comparator<? super C> columnComparator) {
    checkNotNull(cells);
    if (rowComparator != null || columnComparator != null) {
      /*
       * This sorting logic leads to a cellSet() ordering that may not be expected and that isn't
       * documented in the Javadoc. If a row Comparator is provided, cellSet() iterates across the
       * columns in the first row, the columns in the second row, etc. If a column Comparator is
       * provided but a row Comparator isn't, cellSet() iterates across the rows in the first
       * column, the rows in the second column, etc.
       */
      Comparator<Cell<R, C, V>> comparator =
          new Comparator<Cell<R, C, V>>() {
            @Override
            public int compare(Cell<R, C, V> cell1, Cell<R, C, V> cell2) {
              int rowCompare =
                  (rowComparator == null)
                      ? 0
                      : rowComparator.compare(cell1.getRowKey(), cell2.getRowKey());
              if (rowCompare != 0) {
                return rowCompare;
              }
              return (columnComparator == null)
                  ? 0
                  : columnComparator.compare(cell1.getColumnKey(), cell2.getColumnKey());
            }
          };
      Collections.sort(cells, comparator);
    }
    return forCellsInternal(cells, rowComparator, columnComparator);
  }

  static <R, C, V> RegularImmutableTable<R, C, V> forCells(Iterable<Cell<R, C, V>> cells) {
    return forCellsInternal(cells, null, null);
  }

  /**
   * A factory that chooses the most space-efficient representation of the
   * table.
   */
  private static final <R, C, V> RegularImmutableTable<R, C, V> forCellsInternal(
      Iterable<Cell<R, C, V>> cells,
      @Nullable Comparator<? super R> rowComparator,
      @Nullable Comparator<? super C> columnComparator) {
    Set<R> rowSpaceBuilder = new LinkedHashSet<R>();
    Set<C> columnSpaceBuilder = new LinkedHashSet<C>();
    ImmutableList<Cell<R, C, V>> cellList = ImmutableList.copyOf(cells);
    for (Cell<R, C, V> cell : cells) {
      rowSpaceBuilder.add(cell.getRowKey());
      columnSpaceBuilder.add(cell.getColumnKey());
    }

    ImmutableSet<R> rowSpace =
        (rowComparator == null)
            ? ImmutableSet.copyOf(rowSpaceBuilder)
            : ImmutableSet.copyOf(
                Ordering.from(rowComparator).immutableSortedCopy(rowSpaceBuilder));
    ImmutableSet<C> columnSpace =
        (columnComparator == null)
            ? ImmutableSet.copyOf(columnSpaceBuilder)
            : ImmutableSet.copyOf(
                Ordering.from(columnComparator).immutableSortedCopy(columnSpaceBuilder));

    // use a dense table if more than half of the cells have values
    // TODO(gak): tune this condition based on empirical evidence
    return (cellList.size() > (((long) rowSpace.size() * columnSpace.size()) / 2))
        ? new DenseImmutableTable<R, C, V>(cellList, rowSpace, columnSpace)
        : new SparseImmutableTable<R, C, V>(cellList, rowSpace, columnSpace);
  }
}
