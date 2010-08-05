/*
 * Copyright (C) 2008 Google Inc.
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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Collections2.TransformedCollection;
import com.google.common.collect.Table.Cell;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Provides static methods that involve a {@code Table}.
 *
 * @author Jared Levy
 * @since 7
 */
@GwtCompatible
@Beta
public final class Tables {
  private Tables() {}

  /**
   * Returns an immutable cell with the specified row key, column key, and
   * value.
   *
   * <p>The returned cell is serializable.
   *
   * @param rowKey the row key to be associated with the returned cell
   * @param columnKey the column key to be associated with the returned cell
   * @param value the value to be associated with the returned cell
   */
  public static <R, C, V> Cell<R, C, V> immutableCell(
      @Nullable R rowKey, @Nullable C columnKey, @Nullable V value) {
    return new ImmutableCell<R, C, V>(rowKey, columnKey, value);
  }

  private static class ImmutableCell<R, C, V>
      extends AbstractCell<R, C, V> implements Serializable {
    final R rowKey;
    final C columnKey;
    final V value;

    ImmutableCell(
        @Nullable R rowKey, @Nullable C columnKey, @Nullable V value) {
      this.rowKey = rowKey;
      this.columnKey = columnKey;
      this.value = value;
    }

    public R getRowKey() {
      return rowKey;
    }
    public C getColumnKey() {
      return columnKey;
    }
    public V getValue() {
      return value;
    }

    private static final long serialVersionUID = 0;
  }

  abstract static class AbstractCell<R, C, V> implements Cell<R, C, V> {
    // needed for serialization
    AbstractCell() {}

    @Override public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Cell) {
        Cell<?, ?, ?> other = (Cell<?, ?, ?>) obj;
        return Objects.equal(getRowKey(), other.getRowKey())
            && Objects.equal(getColumnKey(), other.getColumnKey())
            && Objects.equal(getValue(), other.getValue());
      }
      return false;
    }

    @Override public int hashCode() {
      return Objects.hashCode(getRowKey(), getColumnKey(), getValue());
    }

    @Override public String toString() {
      return "(" + getRowKey() + "," + getColumnKey() + ")=" + getValue();
    }
  }

  /**
   * Creates a transposed view of a given table that flips its row and column
   * keys. In other words, calling {@code get(columnKey, rowKey)} on the
   * generated table always returns the same value as calling {@code
   * get(rowKey, columnKey)} on the original table. Updating the original table
   * changes the contents of the transposed table and vice versa.
   *
   * <p>The returned table supports update operations as long as the input table
   * supports the analogous operation with swapped rows and columns. For
   * example, in a {@link HashBasedTable} instance, {@code
   * rowKeySet().iterator()} supports {@code remove()} but {@code
   * columnKeySet().iterator()} doesn't. With a transposed {@link
   * HashBasedTable}, it's the other way around.
   */
  public static <R, C, V> Table<C, R, V> transpose(Table<R, C, V> table) {
    return (table instanceof TransposeTable)
        ? ((TransposeTable<R, C, V>) table).original
        : new TransposeTable<C, R, V>(table);
  }

  private static class TransposeTable<C, R, V> implements Table<C, R, V> {
    final Table<R, C, V> original;

    TransposeTable(Table<R, C, V> original) {
      this.original = checkNotNull(original);
    }

    public void clear() {
      original.clear();
    }

    public Map<C, V> column(R columnKey) {
      return original.row(columnKey);
    }

    public Set<R> columnKeySet() {
      return original.rowKeySet();
    }

    public Map<R, Map<C, V>> columnMap() {
      return original.rowMap();
    }

    public boolean contains(
        @Nullable Object rowKey, @Nullable Object columnKey) {
      return original.contains(columnKey, rowKey);
    }

    public boolean containsColumn(@Nullable Object columnKey) {
      return original.containsRow(columnKey);
    }

    public boolean containsRow(@Nullable Object rowKey) {
      return original.containsColumn(rowKey);
    }

    public boolean containsValue(@Nullable Object value) {
      return original.containsValue(value);
    }

    public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
      return original.get(columnKey, rowKey);
    }

    public boolean isEmpty() {
      return original.isEmpty();
    }

    public V put(C rowKey, R columnKey, V value) {
      return original.put(columnKey, rowKey, value);
    }

    public void putAll(Table<? extends C, ? extends R, ? extends V> table) {
      original.putAll(transpose(table));
    }

    public V remove(@Nullable Object rowKey, @Nullable Object columnKey) {
      return original.remove(columnKey, rowKey);
    }

    public Map<R, V> row(C rowKey) {
      return original.column(rowKey);
    }

    public Set<C> rowKeySet() {
      return original.columnKeySet();
    }

    public Map<C, Map<R, V>> rowMap() {
      return original.columnMap();
    }

    public int size() {
      return original.size();
    }

    public Collection<V> values() {
      return original.values();
    }

    @Override public boolean equals(@Nullable Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Table) {
        Table<?, ?, ?> other = (Table<?, ?, ?>) obj;
        return cellSet().equals(other.cellSet());
      }
      return false;
    }

    @Override public int hashCode() {
      return cellSet().hashCode();
    }

    @Override public String toString() {
      return rowMap().toString();
    }

    // Will cast TRANSPOSE_CELL to a type that always succeeds
    @SuppressWarnings("unchecked") // eclipse doesn't like the raw type
    private static final Function TRANSPOSE_CELL = new Function() {
      public Object apply(Object from) {
        Cell<?, ?, ?> cell = (Cell<?, ?, ?>) from;
        return immutableCell(
            cell.getColumnKey(), cell.getRowKey(), cell.getValue());
      }
    };

    CellSet cellSet;

    public Set<Cell<C, R, V>> cellSet() {
      CellSet result = cellSet;
      return (result == null) ? cellSet = new CellSet() : result;
    }

    class CellSet extends TransformedCollection<Cell<R, C, V>, Cell<C, R, V>>
        implements Set<Cell<C, R, V>> {
      // Casting TRANSPOSE_CELL to a type that always succeeds
      @SuppressWarnings("unchecked")
      CellSet() {
        super(original.cellSet(), TRANSPOSE_CELL);
      }

      @Override public boolean equals(Object obj) {
        if (obj == this) {
          return true;
        }
        if (!(obj instanceof Set)) {
          return false;
        }
        Set<?> os = (Set<?>) obj;
        if (os.size() != size()) {
          return false;
        }
        return containsAll(os);
      }

      @Override public int hashCode() {
        return Sets.hashCodeImpl(this);
      }

      @Override public boolean contains(Object obj) {
        if (obj instanceof Cell) {
          Cell<?, ?, ?> cell = (Cell<?, ?, ?>) obj;
          return original.cellSet().contains(immutableCell(
              cell.getColumnKey(), cell.getRowKey(), cell.getValue()));
        }
        return false;
      }

      @Override public boolean remove(Object obj) {
        if (obj instanceof Cell) {
          Cell<?, ?, ?> cell = (Cell<?, ?, ?>) obj;
          return original.cellSet().remove(immutableCell(
              cell.getColumnKey(), cell.getRowKey(), cell.getValue()));
        }
        return false;
      }
    }
  }
}
