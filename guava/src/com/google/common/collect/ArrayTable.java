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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Fixed-size {@link Table} implementation backed by a two-dimensional array.
 *
 * <p>The allowed row and column keys must be supplied when the table is
 * created. The table always contains a mapping for every row key / column pair.
 * The value corresponding to a given row and column is null unless another
 * value is provided.
 *
 * <p>The table's size is constant: the product of the number of supplied row
 * keys and the number of supplied column keys. The {@code remove} and {@code
 * clear} methods are not supported by the table or its views. The {@link
 * #erase} and {@link #eraseAll} methods may be used instead.
 *
 * <p>The ordering of the row and column keys provided when the table is
 * constructed determines the iteration ordering across rows and columns in the
 * table's views. None of the view iterators support {@link Iterator#remove}.
 * If the table is modified after an iterator is created, the iterator remains
 * valid.
 *
 * <p>This class requires less memory than the {@link HashBasedTable} and {@link
 * TreeBasedTable} implementations, except when the table is sparse.
 *
 * <p>Null row keys or column keys are not permitted.
 *
 * <p>This class provides methods involving the underlying array structure,
 * where the array indices correspond to the position of a row or column in the
 * lists of allowed keys and values. See the {@link #at}, {@link #set}, {@link
 * #toArray}, {@link #rowKeyList}, and {@link #columnKeyList} methods for more
 * details.
 *
 * <p>Note that this implementation is not synchronized. If multiple threads
 * access the same cell of an {@code ArrayTable} concurrently and one of the
 * threads modifies its value, there is no guarantee that the new value will be
 * fully visible to the other threads. To guarantee that modifications are
 * visible, synchronize access to the table. Unlike other {@code Table}
 * implementations, synchronization is unnecessary between a thread that writes
 * to one cell and a thread that reads from another.
 *
 * @author Jared Levy
 * @since 10.0
 */
@Beta
public final class ArrayTable<R, C, V> implements Table<R, C, V>, Serializable {

  /**
   * Creates an empty {@code ArrayTable}.
   *
   * @param rowKeys row keys that may be stored in the generated table
   * @param columnKeys column keys that may be stored in the generated table
   * @throws NullPointerException if any of the provided keys is null
   * @throws IllegalArgumentException if {@code rowKeys} or {@code columnKeys}
   *     contains duplicates or is empty
   */
  public static <R, C, V> ArrayTable<R, C, V> create(
      Iterable<? extends R> rowKeys, Iterable<? extends C> columnKeys) {
    return new ArrayTable<R, C, V>(rowKeys, columnKeys);
  }

  /*
   * TODO(jlevy): Add factory methods taking an Enum class, instead of an
   * iterable, to specify the allowed row keys and/or column keys. Note that
   * custom serialization logic is needed to support different enum sizes during
   * serialization and deserialization.
   */

  /**
   * Creates an {@code ArrayTable} with the mappings in the provided table.
   *
   * <p>If {@code table} includes a mapping with row key {@code r} and a
   * separate mapping with column key {@code c}, the returned table contains a
   * mapping with row key {@code r} and column key {@code c}. If that row key /
   * column key pair in not in {@code table}, the pair maps to {@code null} in
   * the generated table.
   *
   * <p>The returned table allows subsequent {@code put} calls with the row keys
   * in {@code table.rowKeySet()} and the column keys in {@code
   * table.columnKeySet()}. Calling {@link #put} with other keys leads to an
   * {@code IllegalArgumentException}.
   *
   * <p>The ordering of {@code table.rowKeySet()} and {@code
   * table.columnKeySet()} determines the row and column iteration ordering of
   * the returned table.
   *
   * @throws NullPointerException if {@code table} has a null key
   * @throws IllegalArgumentException if the provided table is empty
   */
  public static <R, C, V> ArrayTable<R, C, V> create(Table<R, C, V> table) {
    return new ArrayTable<R, C, V>(table);
  }

  /**
   * Creates an {@code ArrayTable} with the same mappings, allowed keys, and
   * iteration ordering as the provided {@code ArrayTable}.
   */
  public static <R, C, V> ArrayTable<R, C, V> create(
      ArrayTable<R, C, V> table) {
    return new ArrayTable<R, C, V>(table);
  }

  private final ImmutableList<R> rowList;
  private final ImmutableList<C> columnList;

  // TODO(jlevy): Add getters returning rowKeyToIndex and columnKeyToIndex?
  private final ImmutableMap<R, Integer> rowKeyToIndex;
  private final ImmutableMap<C, Integer> columnKeyToIndex;
  private final V[][] array;

  private ArrayTable(Iterable<? extends R> rowKeys,
      Iterable<? extends C> columnKeys) {
    this.rowList = ImmutableList.copyOf(rowKeys);
    this.columnList = ImmutableList.copyOf(columnKeys);
    checkArgument(!rowList.isEmpty());
    checkArgument(!columnList.isEmpty());

    /*
     * TODO(jlevy): Support empty rowKeys or columnKeys? If we do, when
     * columnKeys is empty but rowKeys isn't, the table is empty but
     * containsRow() can return true and rowKeySet() isn't empty.
     */
    ImmutableMap.Builder<R, Integer> rowBuilder = ImmutableMap.builder();
    for (int i = 0; i < rowList.size(); i++) {
      rowBuilder.put(rowList.get(i), i);
    }
    rowKeyToIndex = rowBuilder.build();

    ImmutableMap.Builder<C, Integer> columnBuilder = ImmutableMap.builder();
    for (int i = 0; i < columnList.size(); i++) {
      columnBuilder.put(columnList.get(i), i);
    }
    columnKeyToIndex = columnBuilder.build();

    @SuppressWarnings("unchecked")
    V[][] tmpArray
        = (V[][]) new Object[rowList.size()][columnList.size()];
    array = tmpArray;
  }

  private ArrayTable(Table<R, C, V> table) {
    this(table.rowKeySet(), table.columnKeySet());
    putAll(table);
  }

  private ArrayTable(ArrayTable<R, C, V> table) {
    rowList = table.rowList;
    columnList = table.columnList;
    rowKeyToIndex = table.rowKeyToIndex;
    columnKeyToIndex = table.columnKeyToIndex;
    @SuppressWarnings("unchecked")
    V[][] copy = (V[][]) new Object[rowList.size()][columnList.size()];
    array = copy;
    for (int i = 0; i < rowList.size(); i++) {
      System.arraycopy(table.array[i], 0, copy[i], 0, table.array[i].length);
    }
  }

  /**
   * Returns, as an immutable list, the row keys provided when the table was
   * constructed, including those that are mapped to null values only.
   */
  public ImmutableList<R> rowKeyList() {
    return rowList;
  }

  /**
   * Returns, as an immutable list, the column keys provided when the table was
   * constructed, including those that are mapped to null values only.
   */
  public ImmutableList<C> columnKeyList() {
    return columnList;
  }

  /**
   * Returns the value corresponding to the specified row and column indices.
   * The same value is returned by {@code
   * get(rowKeyList().get(rowIndex), columnKeyList().get(columnIndex))}, but
   * this method runs more quickly.
   *
   * @param rowIndex position of the row key in {@link #rowKeyList()}
   * @param columnIndex position of the row key in {@link #columnKeyList()}
   * @return the value with the specified row and column
   * @throws IndexOutOfBoundsException if either index is negative, {@code
   *     rowIndex} is greater then or equal to the number of allowed row keys,
   *     or {@code columnIndex} is greater then or equal to the number of
   *     allowed column keys
   */
  public V at(int rowIndex, int columnIndex) {
    return array[rowIndex][columnIndex];
  }

  /**
   * Associates {@code value} with the specified row and column indices. The
   * logic {@code
   * put(rowKeyList().get(rowIndex), columnKeyList().get(columnIndex), value)}
   * has the same behavior, but this method runs more quickly.
   *
   * @param rowIndex position of the row key in {@link #rowKeyList()}
   * @param columnIndex position of the row key in {@link #columnKeyList()}
   * @param value value to store in the table
   * @return the previous value with the specified row and column
   * @throws IndexOutOfBoundsException if either index is negative, {@code
   *     rowIndex} is greater then or equal to the number of allowed row keys,
   *     or {@code columnIndex} is greater then or equal to the number of
   *     allowed column keys
   */
  public V set(int rowIndex, int columnIndex, @Nullable V value) {
    V oldValue = array[rowIndex][columnIndex];
    array[rowIndex][columnIndex] = value;
    return oldValue;
  }

  /**
   * Returns a two-dimensional array with the table contents. The row and column
   * indices correspond to the positions of the row and column in the iterables
   * provided during table construction. If the table lacks a mapping for a
   * given row and column, the corresponding array element is null.
   *
   * <p>Subsequent table changes will not modify the array, and vice versa.
   *
   * @param valueClass class of values stored in the returned array
   */
  public V[][] toArray(Class<V> valueClass) {
    // Can change to use varargs in JDK 1.6 if we want
    @SuppressWarnings("unchecked") // TODO: safe?
    V[][] copy = (V[][]) Array.newInstance(
        valueClass, new int[] { rowList.size(), columnList.size() });
    for (int i = 0; i < rowList.size(); i++) {
      System.arraycopy(array[i], 0, copy[i], 0, array[i].length);
    }
    return copy;
  }

  /**
   * Not supported. Use {@link #eraseAll} instead.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Use {@link #eraseAll}
   */
  @Override
  @Deprecated public void clear() {
    throw new UnsupportedOperationException();
  }

  /**
   * Associates the value {@code null} with every pair of allowed row and column
   * keys.
   */
  public void eraseAll() {
    for (V[] row : array) {
      Arrays.fill(row, null);
    }
  }

  /**
   * Returns {@code true} if the provided keys are among the keys provided when
   * the table was constructed.
   */
  @Override
  public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
    return containsRow(rowKey) && containsColumn(columnKey);
  }

  /**
   * Returns {@code true} if the provided column key is among the column keys
   * provided when the table was constructed.
   */
  @Override
  public boolean containsColumn(@Nullable Object columnKey) {
    return columnKeyToIndex.containsKey(columnKey);
  }

  /**
   * Returns {@code true} if the provided row key is among the row keys
   * provided when the table was constructed.
   */
  @Override
  public boolean containsRow(@Nullable Object rowKey) {
    return rowKeyToIndex.containsKey(rowKey);
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    for (V[] row : array) {
      for (V element : row) {
        if (Objects.equal(value, element)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
    Integer rowIndex = rowKeyToIndex.get(rowKey);
    Integer columnIndex = columnKeyToIndex.get(columnKey);
    return getIndexed(rowIndex, columnIndex);
  }

  private V getIndexed(Integer rowIndex, Integer columnIndex) {
    return (rowIndex == null || columnIndex == null)
        ? null : array[rowIndex][columnIndex];
  }

  /**
   * Always returns {@code false}.
   */
  @Override
  public boolean isEmpty() {
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if {@code rowKey} is not in {@link
   *     #rowKeySet()} or {@code columnKey} is not in {@link #columnKeySet()}.
   */
  @Override
  public V put(R rowKey, C columnKey, @Nullable V value) {
    checkNotNull(rowKey);
    checkNotNull(columnKey);
    Integer rowIndex = rowKeyToIndex.get(rowKey);
    checkArgument(rowIndex != null, "Row %s not in %s", rowKey, rowList);
    Integer columnIndex = columnKeyToIndex.get(columnKey);
    checkArgument(columnIndex != null,
        "Column %s not in %s", columnKey, columnList);
    return set(rowIndex, columnIndex, value);
  }

  /*
   * TODO(jlevy): Consider creating a merge() method, similar to putAll() but
   * copying non-null values only.
   */

  /**
   * {@inheritDoc}
   *
   * <p>If {@code table} is an {@code ArrayTable}, its null values will be
   * stored in this table, possibly replacing values that were previously
   * non-null.
   *
   * @throws NullPointerException if {@code table} has a null key
   * @throws IllegalArgumentException if any of the provided table's row keys or
   *     column keys is not in {@link #rowKeySet()} or {@link #columnKeySet()}
   */
  @Override
  public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
    for (Cell<? extends R, ? extends C, ? extends V> cell : table.cellSet()) {
      put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
    }
  }

  /**
   * Not supported. Use {@link #erase} instead.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Use {@link #erase}
   */
  @Override
  @Deprecated public V remove(Object rowKey, Object columnKey) {
    throw new UnsupportedOperationException();
  }

  /**
   * Associates the value {@code null} with the specified keys, assuming both
   * keys are valid. If either key is null or isn't among the keys provided
   * during construction, this method has no effect.
   *
   * <p>This method is equivalent to {@code put(rowKey, columnKey, null)} when
   * both provided keys are valid.
   *
   * @param rowKey row key of mapping to be erased
   * @param columnKey column key of mapping to be erased
   * @return the value previously associated with the keys, or {@code null} if
   *     no mapping existed for the keys
   */
  public V erase(@Nullable Object rowKey, @Nullable Object columnKey) {
    Integer rowIndex = rowKeyToIndex.get(rowKey);
    Integer columnIndex = columnKeyToIndex.get(columnKey);
    if (rowIndex == null || columnIndex == null) {
      return null;
    }
    return set(rowIndex, columnIndex, null);
  }

  // TODO(jlevy): Add eraseRow and eraseColumn methods?

  @Override
  public int size() {
    return rowList.size() * columnList.size();
  }

  @Override public boolean equals(@Nullable Object obj) {
    if (obj instanceof Table) {
      Table<?, ?, ?> other = (Table<?, ?, ?>) obj;
      return cellSet().equals(other.cellSet());
    }
    return false;
  }

  @Override public int hashCode() {
    return cellSet().hashCode();
  }

  /**
   * Returns the string representation {@code rowMap().toString()}.
   */
  @Override public String toString() {
    return rowMap().toString();
  }

  private transient CellSet cellSet;

  /**
   * Returns an unmodifiable set of all row key / column key / value
   * triplets. Changes to the table will update the returned set.
   *
   * <p>The returned set's iterator traverses the mappings with the first row
   * key, the mappings with the second row key, and so on.
   *
   * <p>The value in the returned cells may change if the table subsequently
   * changes.
   *
   * @return set of table cells consisting of row key / column key / value
   *     triplets
   */
  @Override
  public Set<Cell<R, C, V>> cellSet() {
    CellSet set = cellSet;
    return (set == null) ? cellSet = new CellSet() : set;
  }

  private class CellSet extends AbstractSet<Cell<R, C, V>> {

    @Override public Iterator<Cell<R, C, V>> iterator() {
      return new AbstractIndexedListIterator<Cell<R, C, V>>(size()) {
        @Override protected Cell<R, C, V> get(final int index) {
          return new Tables.AbstractCell<R, C, V>() {
            final int rowIndex = index / columnList.size();
            final int columnIndex = index % columnList.size();
            @Override
            public R getRowKey() {
              return rowList.get(rowIndex);
            }
            @Override
            public C getColumnKey() {
              return columnList.get(columnIndex);
            }
            @Override
            public V getValue() {
              return array[rowIndex][columnIndex];
            }
          };
        }
      };
    }

    @Override public int size() {
      return ArrayTable.this.size();
    }

    @Override public boolean contains(Object obj) {
      if (obj instanceof Cell) {
        Cell<?, ?, ?> cell = (Cell<?, ?, ?>) obj;
        Integer rowIndex = rowKeyToIndex.get(cell.getRowKey());
        Integer columnIndex = columnKeyToIndex.get(cell.getColumnKey());
        return rowIndex != null
            && columnIndex != null
            && Objects.equal(array[rowIndex][columnIndex], cell.getValue());
      }
      return false;
    }
  }

  /**
   * Returns a view of all mappings that have the given column key. If the
   * column key isn't in {@link #columnKeySet()}, an empty immutable map is
   * returned.
   *
   * <p>Otherwise, for each row key in {@link #rowKeySet()}, the returned map
   * associates the row key with the corresponding value in the table. Changes
   * to the returned map will update the underlying table, and vice versa.
   *
   * @param columnKey key of column to search for in the table
   * @return the corresponding map from row keys to values
   */
  @Override
  public Map<R, V> column(C columnKey) {
    checkNotNull(columnKey);
    Integer columnIndex = columnKeyToIndex.get(columnKey);
    return (columnIndex == null)
        ? ImmutableMap.<R, V>of() : new Column(columnIndex);
  }

  private class Column extends AbstractMap<R, V> {
    final int columnIndex;

    Column(int columnIndex) {
      this.columnIndex = columnIndex;
    }

    ColumnEntrySet entrySet;

    @Override public Set<Entry<R, V>> entrySet() {
      ColumnEntrySet set = entrySet;
      return (set == null) ? entrySet = new ColumnEntrySet(columnIndex) : set;
    }

    @Override public V get(Object rowKey) {
      Integer rowIndex = rowKeyToIndex.get(rowKey);
      return getIndexed(rowIndex, columnIndex);
    }

    @Override public boolean containsKey(Object rowKey) {
      return rowKeyToIndex.containsKey(rowKey);
    }

    @Override public V put(R rowKey, V value) {
      checkNotNull(rowKey);
      Integer rowIndex = rowKeyToIndex.get(rowKey);
      checkArgument(rowIndex != null, "Row %s not in %s", rowKey, rowList);
      return set(rowIndex, columnIndex, value);
    }

    @Override public Set<R> keySet() {
      return rowKeySet();
    }
  }

  private class ColumnEntrySet extends AbstractSet<Entry<R, V>> {
    final int columnIndex;

    ColumnEntrySet(int columnIndex) {
      this.columnIndex = columnIndex;
    }

    @Override public Iterator<Entry<R, V>> iterator() {
      return new AbstractIndexedListIterator<Entry<R, V>>(size()) {
        @Override protected Entry<R, V> get(final int rowIndex) {
          return new AbstractMapEntry<R, V>() {
            @Override public R getKey() {
              return rowList.get(rowIndex);
            }
            @Override public V getValue() {
              return array[rowIndex][columnIndex];
            }
            @Override public V setValue(V value) {
              return ArrayTable.this.set(rowIndex, columnIndex, value);
            }
          };
        }
      };
    }

    @Override public int size() {
      return rowList.size();
    }
  }

  /**
   * Returns an immutable set of the valid column keys, including those that
   * are associated with null values only.
   *
   * @return immutable set of column keys
   */
  @Override
  public ImmutableSet<C> columnKeySet() {
    return columnKeyToIndex.keySet();
  }

  private transient ColumnMap columnMap;

  @Override
  public Map<C, Map<R, V>> columnMap() {
    ColumnMap map = columnMap;
    return (map == null) ? columnMap = new ColumnMap() : map;
  }

  private class ColumnMap extends AbstractMap<C, Map<R, V>> {
    transient ColumnMapEntrySet entrySet;

    @Override public Set<Entry<C, Map<R, V>>> entrySet() {
      ColumnMapEntrySet set = entrySet;
      return (set == null) ? entrySet = new ColumnMapEntrySet() : set;
    }

    @Override public Map<R, V> get(Object columnKey) {
      Integer columnIndex = columnKeyToIndex.get(columnKey);
      return (columnIndex == null) ? null : new Column(columnIndex);
    }

    @Override public boolean containsKey(Object columnKey) {
      return containsColumn(columnKey);
    }

    @Override public Set<C> keySet() {
      return columnKeySet();
    }

    @Override public Map<R, V> remove(Object columnKey) {
      throw new UnsupportedOperationException();
    }
  }

  private class ColumnMapEntrySet extends AbstractSet<Entry<C, Map<R, V>>> {
    @Override public Iterator<Entry<C, Map<R, V>>> iterator() {
      return new AbstractIndexedListIterator<Entry<C, Map<R, V>>>(size()) {
        @Override protected Entry<C, Map<R, V>> get(int index) {
          return Maps.<C, Map<R, V>>immutableEntry(columnList.get(index),
              new Column(index));
        }
      };
    }

    @Override public int size() {
      return columnList.size();
    }
  }

  /**
   * Returns a view of all mappings that have the given row key. If the
   * row key isn't in {@link #rowKeySet()}, an empty immutable map is
   * returned.
   *
   * <p>Otherwise, for each column key in {@link #columnKeySet()}, the returned
   * map associates the column key with the corresponding value in the
   * table. Changes to the returned map will update the underlying table, and
   * vice versa.
   *
   * @param rowKey key of row to search for in the table
   * @return the corresponding map from column keys to values
   */
  @Override
  public Map<C, V> row(R rowKey) {
    checkNotNull(rowKey);
    Integer rowIndex = rowKeyToIndex.get(rowKey);
    return (rowIndex == null) ? ImmutableMap.<C, V>of() : new Row(rowIndex);
  }

  private class Row extends AbstractMap<C, V> {
    final int rowIndex;

    Row(int rowIndex) {
      this.rowIndex = rowIndex;
    }

    RowEntrySet entrySet;

    @Override public Set<Entry<C, V>> entrySet() {
      RowEntrySet set = entrySet;
      return (set == null) ? entrySet = new RowEntrySet(rowIndex) : set;
    }

    @Override public V get(Object columnKey) {
      Integer columnIndex = columnKeyToIndex.get(columnKey);
      return getIndexed(rowIndex, columnIndex);
    }

    @Override public boolean containsKey(Object columnKey) {
      return containsColumn(columnKey);
    }

    @Override public V put(C columnKey, V value) {
      checkNotNull(columnKey);
      Integer columnIndex = columnKeyToIndex.get(columnKey);
      checkArgument(columnIndex != null,
          "Column %s not in %s", columnKey, columnList);
      return set(rowIndex, columnIndex, value);
    }

    @Override public Set<C> keySet() {
      return columnKeySet();
    }
  }

  private class RowEntrySet extends AbstractSet<Entry<C, V>> {
    final int rowIndex;

    RowEntrySet(int rowIndex) {
      this.rowIndex = rowIndex;
    }

    @Override public Iterator<Entry<C, V>> iterator() {
      return new AbstractIndexedListIterator<Entry<C, V>>(size()) {
        @Override protected Entry<C, V> get(final int columnIndex) {
          return new AbstractMapEntry<C, V>() {
            @Override public C getKey() {
              return columnList.get(columnIndex);
            }
            @Override public V getValue() {
              return array[rowIndex][columnIndex];
            }
            @Override public V setValue(V value) {
              return ArrayTable.this.set(rowIndex, columnIndex, value);
            }
          };
        }
      };
    }

    @Override public int size() {
      return columnList.size();
    }
  }

  /**
   * Returns an immutable set of the valid row keys, including those that are
   * associated with null values only.
   *
   * @return immutable set of row keys
   */
  @Override
  public ImmutableSet<R> rowKeySet() {
    return rowKeyToIndex.keySet();
  }

  private transient RowMap rowMap;

  @Override
  public Map<R, Map<C, V>> rowMap() {
    RowMap map = rowMap;
    return (map == null) ? rowMap = new RowMap() : map;
  }

  private class RowMap extends AbstractMap<R, Map<C, V>> {
    transient RowMapEntrySet entrySet;

    @Override public Set<Entry<R, Map<C, V>>> entrySet() {
      RowMapEntrySet set = entrySet;
      return (set == null) ? entrySet = new RowMapEntrySet() : set;
    }

    @Override public Map<C, V> get(Object rowKey) {
      Integer rowIndex = rowKeyToIndex.get(rowKey);
      return (rowIndex == null) ? null : new Row(rowIndex);
    }

    @Override public boolean containsKey(Object rowKey) {
      return containsRow(rowKey);
    }

    @Override public Set<R> keySet() {
      return rowKeySet();
    }

    @Override public Map<C, V> remove(Object rowKey) {
      throw new UnsupportedOperationException();
    }
  }

  private class RowMapEntrySet extends AbstractSet<Entry<R, Map<C, V>>> {
    @Override public Iterator<Entry<R, Map<C, V>>> iterator() {
      return new AbstractIndexedListIterator<Entry<R, Map<C, V>>>(size()) {
        @Override protected Entry<R, Map<C, V>> get(int index) {
          return Maps.<R, Map<C, V>>immutableEntry(rowList.get(index),
              new Row(index));
        }
      };
    }

    @Override public int size() {
      return rowList.size();
    }
  }

  private transient Collection<V> values;

  /**
   * Returns an unmodifiable collection of all values, which may contain
   * duplicates. Changes to the table will update the returned collection.
   *
   * <p>The returned collection's iterator traverses the values of the first row
   * key, the values of the second row key, and so on.
   *
   * @return collection of values
   */
  @Override
  public Collection<V> values() {
    Collection<V> v = values;
    return (v == null) ? values = new Values() : v;
  }

  private class Values extends AbstractCollection<V> {
    @Override public Iterator<V> iterator() {
      return new AbstractIndexedListIterator<V>(size()) {
        @Override protected V get(int index) {
          int rowIndex = index / columnList.size();
          int columnIndex = index % columnList.size();
          return array[rowIndex][columnIndex];
        }
      };
    }

    @Override public int size() {
      return ArrayTable.this.size();
    }

    @Override public boolean contains(Object value) {
      return containsValue(value);
    }
  }

  private static final long serialVersionUID = 0;
}
