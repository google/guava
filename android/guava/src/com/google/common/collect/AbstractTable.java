/*
 * Copyright (C) 2013 The Guava Authors
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
import static com.google.common.collect.Maps.safeGet;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.WeakOuter;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Skeletal, implementation-agnostic implementation of the {@link Table} interface.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
abstract class AbstractTable<
        R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
    implements Table<R, C, V> {

  @Override
  public boolean containsRow(@CheckForNull Object rowKey) {
    return Maps.safeContainsKey(rowMap(), rowKey);
  }

  @Override
  public boolean containsColumn(@CheckForNull Object columnKey) {
    return Maps.safeContainsKey(columnMap(), columnKey);
  }

  @Override
  public Set<R> rowKeySet() {
    return rowMap().keySet();
  }

  @Override
  public Set<C> columnKeySet() {
    return columnMap().keySet();
  }

  @Override
  public boolean containsValue(@CheckForNull Object value) {
    for (Map<C, V> row : rowMap().values()) {
      if (row.containsValue(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean contains(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
    Map<C, V> row = safeGet(rowMap(), rowKey);
    return row != null && Maps.safeContainsKey(row, columnKey);
  }

  @Override
  @CheckForNull
  public V get(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
    Map<C, V> row = safeGet(rowMap(), rowKey);
    return (row == null) ? null : safeGet(row, columnKey);
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public void clear() {
    Iterators.clear(cellSet().iterator());
  }

  @CanIgnoreReturnValue
  @Override
  @CheckForNull
  public V remove(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
    Map<C, V> row = safeGet(rowMap(), rowKey);
    return (row == null) ? null : Maps.safeRemove(row, columnKey);
  }

  @CanIgnoreReturnValue
  @Override
  @CheckForNull
  public V put(
      @ParametricNullness R rowKey, @ParametricNullness C columnKey, @ParametricNullness V value) {
    return row(rowKey).put(columnKey, value);
  }

  @Override
  public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
    for (Table.Cell<? extends R, ? extends C, ? extends V> cell : table.cellSet()) {
      put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
    }
  }

  @LazyInit @CheckForNull private transient Set<Cell<R, C, V>> cellSet;

  @Override
  public Set<Cell<R, C, V>> cellSet() {
    Set<Cell<R, C, V>> result = cellSet;
    return (result == null) ? cellSet = createCellSet() : result;
  }

  Set<Cell<R, C, V>> createCellSet() {
    return new CellSet();
  }

  abstract Iterator<Table.Cell<R, C, V>> cellIterator();

  @WeakOuter
  class CellSet extends AbstractSet<Cell<R, C, V>> {
    @Override
    public boolean contains(@CheckForNull Object o) {
      if (o instanceof Cell) {
        Cell<?, ?, ?> cell = (Cell<?, ?, ?>) o;
        Map<C, V> row = safeGet(rowMap(), cell.getRowKey());
        return row != null
            && Collections2.safeContains(
                row.entrySet(), immutableEntry(cell.getColumnKey(), cell.getValue()));
      }
      return false;
    }

    @Override
    public boolean remove(@CheckForNull Object o) {
      if (o instanceof Cell) {
        Cell<?, ?, ?> cell = (Cell<?, ?, ?>) o;
        Map<C, V> row = safeGet(rowMap(), cell.getRowKey());
        return row != null
            && Collections2.safeRemove(
                row.entrySet(), immutableEntry(cell.getColumnKey(), cell.getValue()));
      }
      return false;
    }

    @Override
    public void clear() {
      AbstractTable.this.clear();
    }

    @Override
    public Iterator<Table.Cell<R, C, V>> iterator() {
      return cellIterator();
    }

    @Override
    public int size() {
      return AbstractTable.this.size();
    }
  }

  @LazyInit @CheckForNull private transient Collection<V> values;

  @Override
  public Collection<V> values() {
    Collection<V> result = values;
    return (result == null) ? values = createValues() : result;
  }

  Collection<V> createValues() {
    return new Values();
  }

  Iterator<V> valuesIterator() {
    return new TransformedIterator<Cell<R, C, V>, V>(cellSet().iterator()) {
      @Override
      @ParametricNullness
      V transform(Cell<R, C, V> cell) {
        return cell.getValue();
      }
    };
  }

  @WeakOuter
  class Values extends AbstractCollection<V> {
    @Override
    public Iterator<V> iterator() {
      return valuesIterator();
    }

    @Override
    public boolean contains(@CheckForNull Object o) {
      return containsValue(o);
    }

    @Override
    public void clear() {
      AbstractTable.this.clear();
    }

    @Override
    public int size() {
      return AbstractTable.this.size();
    }
  }

  @Override
  public boolean equals(@CheckForNull Object obj) {
    return Tables.equalsImpl(this, obj);
  }

  @Override
  public int hashCode() {
    return cellSet().hashCode();
  }

  /** Returns the string representation {@code rowMap().toString()}. */
  @Override
  public String toString() {
    return rowMap().toString();
  }
}
