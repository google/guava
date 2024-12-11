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
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.arraycopy;
import static java.util.Collections.emptyMap;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.collect.Maps.IteratorBasedAbstractMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.WeakOuter;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Fixed-size {@link Table} implementation backed by a two-dimensional array.
 *
 * <p><b>Warning:</b> {@code ArrayTable} is rarely the {@link Table} implementation you want. First,
 * it requires that the complete universe of rows and columns be specified at construction time.
 * Second, it is always backed by an array large enough to hold a value for every possible
 * combination of row and column keys. (This is rarely optimal unless the table is extremely dense.)
 * Finally, every possible combination of row and column keys is always considered to have a value
 * associated with it: It is not possible to "remove" a value, only to replace it with {@code null},
 * which will still appear when iterating over the table's contents in a foreach loop or a call to a
 * null-hostile method like {@link ImmutableTable#copyOf}. For alternatives, please see <a
 * href="https://github.com/google/guava/wiki/NewCollectionTypesExplained#table">the wiki</a>.
 *
 * <p>The allowed row and column keys must be supplied when the table is created. The table always
 * contains a mapping for every row key / column pair. The value corresponding to a given row and
 * column is null unless another value is provided.
 *
 * <p>The table's size is constant: the product of the number of supplied row keys and the number of
 * supplied column keys. The {@code remove} and {@code clear} methods are not supported by the table
 * or its views. The {@link #erase} and {@link #eraseAll} methods may be used instead.
 *
 * <p>The ordering of the row and column keys provided when the table is constructed determines the
 * iteration ordering across rows and columns in the table's views. None of the view iterators
 * support {@link Iterator#remove}. If the table is modified after an iterator is created, the
 * iterator remains valid.
 *
 * <p>This class requires less memory than the {@link HashBasedTable} and {@link TreeBasedTable}
 * implementations, except when the table is sparse.
 *
 * <p>Null row keys or column keys are not permitted.
 *
 * <p>This class provides methods involving the underlying array structure, where the array indices
 * correspond to the position of a row or column in the lists of allowed keys and values. See the
 * {@link #at}, {@link #set}, {@link #toArray}, {@link #rowKeyList}, and {@link #columnKeyList}
 * methods for more details.
 *
 * <p>Note that this implementation is not synchronized. If multiple threads access the same cell of
 * an {@code ArrayTable} concurrently and one of the threads modifies its value, there is no
 * guarantee that the new value will be fully visible to the other threads. To guarantee that
 * modifications are visible, synchronize access to the table. Unlike other {@code Table}
 * implementations, synchronization is unnecessary between a thread that writes to one cell and a
 * thread that reads from another.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#table">{@code Table}</a>.
 *
 * @author Jared Levy
 * @since 10.0
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public final class ArrayTable<R, C, V> extends AbstractTable<R, C, @Nullable V>
    implements Serializable {

  /**
   * Creates an {@code ArrayTable} filled with {@code null}.
   *
   * @param rowKeys row keys that may be stored in the generated table
   * @param columnKeys column keys that may be stored in the generated table
   * @throws NullPointerException if any of the provided keys is null
   * @throws IllegalArgumentException if {@code rowKeys} or {@code columnKeys} contains duplicates
   *     or if exactly one of {@code rowKeys} or {@code columnKeys} is empty.
   */
  public static <R, C, V> ArrayTable<R, C, V> create(
      Iterable<? extends R> rowKeys, Iterable<? extends C> columnKeys) {
    return new ArrayTable<>(rowKeys, columnKeys);
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
   * <p>If {@code table} includes a mapping with row key {@code r} and a separate mapping with
   * column key {@code c}, the returned table contains a mapping with row key {@code r} and column
   * key {@code c}. If that row key / column key pair in not in {@code table}, the pair maps to
   * {@code null} in the generated table.
   *
   * <p>The returned table allows subsequent {@code put} calls with the row keys in {@code
   * table.rowKeySet()} and the column keys in {@code table.columnKeySet()}. Calling {@link #put}
   * with other keys leads to an {@code IllegalArgumentException}.
   *
   * <p>The ordering of {@code table.rowKeySet()} and {@code table.columnKeySet()} determines the
   * row and column iteration ordering of the returned table.
   *
   * @throws NullPointerException if {@code table} has a null key
   */
  @SuppressWarnings("unchecked") // TODO(cpovirk): Make constructor accept wildcard types?
  public static <R, C, V> ArrayTable<R, C, V> create(Table<R, C, ? extends @Nullable V> table) {
    return (table instanceof ArrayTable)
        ? new ArrayTable<R, C, V>((ArrayTable<R, C, V>) table)
        : new ArrayTable<R, C, V>(table);
  }

  private final ImmutableList<R> rowList;
  private final ImmutableList<C> columnList;

  // TODO(jlevy): Add getters returning rowKeyToIndex and columnKeyToIndex?
  private final ImmutableMap<R, Integer> rowKeyToIndex;
  private final ImmutableMap<C, Integer> columnKeyToIndex;
  private final @Nullable V[][] array;

  private ArrayTable(Iterable<? extends R> rowKeys, Iterable<? extends C> columnKeys) {
    this.rowList = ImmutableList.copyOf(rowKeys);
    this.columnList = ImmutableList.copyOf(columnKeys);
    checkArgument(rowList.isEmpty() == columnList.isEmpty());

    /*
     * TODO(jlevy): Support only one of rowKey / columnKey being empty? If we
     * do, when columnKeys is empty but rowKeys isn't, rowKeyList() can contain
     * elements but rowKeySet() will be empty and containsRow() won't
     * acknowledge them.
     */
    rowKeyToIndex = Maps.indexMap(rowList);
    columnKeyToIndex = Maps.indexMap(columnList);

    @SuppressWarnings("unchecked")
    @Nullable
    V[][] tmpArray = (@Nullable V[][]) new Object[rowList.size()][columnList.size()];
    array = tmpArray;
    // Necessary because in GWT the arrays are initialized with "undefined" instead of null.
    eraseAll();
  }

  private ArrayTable(Table<R, C, ? extends @Nullable V> table) {
    this(table.rowKeySet(), table.columnKeySet());
    putAll(table);
  }

  private ArrayTable(ArrayTable<R, C, V> table) {
    rowList = table.rowList;
    columnList = table.columnList;
    rowKeyToIndex = table.rowKeyToIndex;
    columnKeyToIndex = table.columnKeyToIndex;
    @SuppressWarnings("unchecked")
    @Nullable
    V[][] copy = (@Nullable V[][]) new Object[rowList.size()][columnList.size()];
    array = copy;
    for (int i = 0; i < rowList.size(); i++) {
      arraycopy(table.array[i], 0, copy[i], 0, table.array[i].length);
    }
  }

  private abstract static class ArrayMap<K, V extends @Nullable Object>
      extends IteratorBasedAbstractMap<K, V> {
    private final ImmutableMap<K, Integer> keyIndex;

    private ArrayMap(ImmutableMap<K, Integer> keyIndex) {
      this.keyIndex = keyIndex;
    }

    @Override
    public Set<K> keySet() {
      return keyIndex.keySet();
    }

    K getKey(int index) {
      return keyIndex.keySet().asList().get(index);
    }

    abstract String getKeyRole();

    @ParametricNullness
    abstract V getValue(int index);

    @ParametricNullness
    abstract V setValue(int index, @ParametricNullness V newValue);

    @Override
    public int size() {
      return keyIndex.size();
    }

    @Override
    public boolean isEmpty() {
      return keyIndex.isEmpty();
    }

    Entry<K, V> getEntry(final int index) {
      checkElementIndex(index, size());
      return new AbstractMapEntry<K, V>() {
        @Override
        public K getKey() {
          return ArrayMap.this.getKey(index);
        }

        @Override
        @ParametricNullness
        public V getValue() {
          return ArrayMap.this.getValue(index);
        }

        @Override
        @ParametricNullness
        public V setValue(@ParametricNullness V value) {
          return ArrayMap.this.setValue(index, value);
        }
      };
    }

    @Override
    Iterator<Entry<K, V>> entryIterator() {
      return new AbstractIndexedListIterator<Entry<K, V>>(size()) {
        @Override
        protected Entry<K, V> get(final int index) {
          return getEntry(index);
        }
      };
    }

    @Override
    Spliterator<Entry<K, V>> entrySpliterator() {
      return CollectSpliterators.indexed(size(), Spliterator.ORDERED, this::getEntry);
    }

    // TODO(lowasser): consider an optimized values() implementation

    @Override
    public boolean containsKey(@CheckForNull Object key) {
      return keyIndex.containsKey(key);
    }

    @CheckForNull
    @Override
    public V get(@CheckForNull Object key) {
      Integer index = keyIndex.get(key);
      if (index == null) {
        return null;
      } else {
        return getValue(index);
      }
    }

    @Override
    @CheckForNull
    public V put(K key, @ParametricNullness V value) {
      Integer index = keyIndex.get(key);
      if (index == null) {
        throw new IllegalArgumentException(
            getKeyRole() + " " + key + " not in " + keyIndex.keySet());
      }
      return setValue(index, value);
    }

    @Override
    @CheckForNull
    public V remove(@CheckForNull Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns, as an immutable list, the row keys provided when the table was constructed, including
   * those that are mapped to null values only.
   */
  public ImmutableList<R> rowKeyList() {
    return rowList;
  }

  /**
   * Returns, as an immutable list, the column keys provided when the table was constructed,
   * including those that are mapped to null values only.
   */
  public ImmutableList<C> columnKeyList() {
    return columnList;
  }

  /**
   * Returns the value corresponding to the specified row and column indices. The same value is
   * returned by {@code get(rowKeyList().get(rowIndex), columnKeyList().get(columnIndex))}, but this
   * method runs more quickly.
   *
   * @param rowIndex position of the row key in {@link #rowKeyList()}
   * @param columnIndex position of the row key in {@link #columnKeyList()}
   * @return the value with the specified row and column
   * @throws IndexOutOfBoundsException if either index is negative, {@code rowIndex} is greater than
   *     or equal to the number of allowed row keys, or {@code columnIndex} is greater than or equal
   *     to the number of allowed column keys
   */
  @CheckForNull
  public V at(int rowIndex, int columnIndex) {
    // In GWT array access never throws IndexOutOfBoundsException.
    checkElementIndex(rowIndex, rowList.size());
    checkElementIndex(columnIndex, columnList.size());
    return array[rowIndex][columnIndex];
  }

  /**
   * Associates {@code value} with the specified row and column indices. The logic {@code
   * put(rowKeyList().get(rowIndex), columnKeyList().get(columnIndex), value)} has the same
   * behavior, but this method runs more quickly.
   *
   * @param rowIndex position of the row key in {@link #rowKeyList()}
   * @param columnIndex position of the row key in {@link #columnKeyList()}
   * @param value value to store in the table
   * @return the previous value with the specified row and column
   * @throws IndexOutOfBoundsException if either index is negative, {@code rowIndex} is greater than
   *     or equal to the number of allowed row keys, or {@code columnIndex} is greater than or equal
   *     to the number of allowed column keys
   */
  @CanIgnoreReturnValue
  @CheckForNull
  public V set(int rowIndex, int columnIndex, @CheckForNull V value) {
    // In GWT array access never throws IndexOutOfBoundsException.
    checkElementIndex(rowIndex, rowList.size());
    checkElementIndex(columnIndex, columnList.size());
    V oldValue = array[rowIndex][columnIndex];
    array[rowIndex][columnIndex] = value;
    return oldValue;
  }

  /**
   * Returns a two-dimensional array with the table contents. The row and column indices correspond
   * to the positions of the row and column in the iterables provided during table construction. If
   * the table lacks a mapping for a given row and column, the corresponding array element is null.
   *
   * <p>Subsequent table changes will not modify the array, and vice versa.
   *
   * @param valueClass class of values stored in the returned array
   */
  @GwtIncompatible // reflection
  public @Nullable V[][] toArray(Class<V> valueClass) {
    @SuppressWarnings("unchecked") // TODO: safe?
    @Nullable
    V[][] copy = (@Nullable V[][]) Array.newInstance(valueClass, rowList.size(), columnList.size());
    for (int i = 0; i < rowList.size(); i++) {
      arraycopy(array[i], 0, copy[i], 0, array[i].length);
    }
    return copy;
  }

  /**
   * Not supported. Use {@link #eraseAll} instead.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Use {@link #eraseAll}
   */
  @DoNotCall("Always throws UnsupportedOperationException")
  @Override
  @Deprecated
  public void clear() {
    throw new UnsupportedOperationException();
  }

  /** Associates the value {@code null} with every pair of allowed row and column keys. */
  public void eraseAll() {
    for (@Nullable V[] row : array) {
      Arrays.fill(row, null);
    }
  }

  /**
   * Returns {@code true} if the provided keys are among the keys provided when the table was
   * constructed.
   */
  @Override
  public boolean contains(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
    return containsRow(rowKey) && containsColumn(columnKey);
  }

  /**
   * Returns {@code true} if the provided column key is among the column keys provided when the
   * table was constructed.
   */
  @Override
  public boolean containsColumn(@CheckForNull Object columnKey) {
    return columnKeyToIndex.containsKey(columnKey);
  }

  /**
   * Returns {@code true} if the provided row key is among the row keys provided when the table was
   * constructed.
   */
  @Override
  public boolean containsRow(@CheckForNull Object rowKey) {
    return rowKeyToIndex.containsKey(rowKey);
  }

  @Override
  public boolean containsValue(@CheckForNull Object value) {
    for (@Nullable V[] row : array) {
      for (V element : row) {
        if (Objects.equal(value, element)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  @CheckForNull
  public V get(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
    Integer rowIndex = rowKeyToIndex.get(rowKey);
    Integer columnIndex = columnKeyToIndex.get(columnKey);
    return (rowIndex == null || columnIndex == null) ? null : at(rowIndex, columnIndex);
  }

  /**
   * Returns {@code true} if {@code rowKeyList().size == 0} or {@code columnKeyList().size() == 0}.
   */
  @Override
  public boolean isEmpty() {
    return rowList.isEmpty() || columnList.isEmpty();
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if {@code rowKey} is not in {@link #rowKeySet()} or {@code
   *     columnKey} is not in {@link #columnKeySet()}.
   */
  @CanIgnoreReturnValue
  @Override
  @CheckForNull
  public V put(R rowKey, C columnKey, @CheckForNull V value) {
    checkNotNull(rowKey);
    checkNotNull(columnKey);
    Integer rowIndex = rowKeyToIndex.get(rowKey);
    checkArgument(rowIndex != null, "Row %s not in %s", rowKey, rowList);
    Integer columnIndex = columnKeyToIndex.get(columnKey);
    checkArgument(columnIndex != null, "Column %s not in %s", columnKey, columnList);
    return set(rowIndex, columnIndex, value);
  }

  /*
   * TODO(jlevy): Consider creating a merge() method, similar to putAll() but
   * copying non-null values only.
   */

  /**
   * {@inheritDoc}
   *
   * <p>If {@code table} is an {@code ArrayTable}, its null values will be stored in this table,
   * possibly replacing values that were previously non-null.
   *
   * @throws NullPointerException if {@code table} has a null key
   * @throws IllegalArgumentException if any of the provided table's row keys or column keys is not
   *     in {@link #rowKeySet()} or {@link #columnKeySet()}
   */
  @Override
  public void putAll(Table<? extends R, ? extends C, ? extends @Nullable V> table) {
    super.putAll(table);
  }

  /**
   * Not supported. Use {@link #erase} instead.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Use {@link #erase}
   */
  @DoNotCall("Always throws UnsupportedOperationException")
  @CanIgnoreReturnValue
  @Override
  @Deprecated
  @CheckForNull
  public V remove(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
    throw new UnsupportedOperationException();
  }

  /**
   * Associates the value {@code null} with the specified keys, assuming both keys are valid. If
   * either key is null or isn't among the keys provided during construction, this method has no
   * effect.
   *
   * <p>This method is equivalent to {@code put(rowKey, columnKey, null)} when both provided keys
   * are valid.
   *
   * @param rowKey row key of mapping to be erased
   * @param columnKey column key of mapping to be erased
   * @return the value previously associated with the keys, or {@code null} if no mapping existed
   *     for the keys
   */
  @CanIgnoreReturnValue
  @CheckForNull
  public V erase(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
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

  /**
   * Returns an unmodifiable set of all row key / column key / value triplets. Changes to the table
   * will update the returned set.
   *
   * <p>The returned set's iterator traverses the mappings with the first row key, the mappings with
   * the second row key, and so on.
   *
   * <p>The value in the returned cells may change if the table subsequently changes.
   *
   * @return set of table cells consisting of row key / column key / value triplets
   */
  @Override
  public Set<Cell<R, C, @Nullable V>> cellSet() {
    return super.cellSet();
  }

  @Override
  Iterator<Cell<R, C, @Nullable V>> cellIterator() {
    return new AbstractIndexedListIterator<Cell<R, C, @Nullable V>>(size()) {
      @Override
      protected Cell<R, C, @Nullable V> get(final int index) {
        return getCell(index);
      }
    };
  }

  @Override
  Spliterator<Cell<R, C, @Nullable V>> cellSpliterator() {
    return CollectSpliterators.<Cell<R, C, @Nullable V>>indexed(
        size(), Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.DISTINCT, this::getCell);
  }

  private Cell<R, C, @Nullable V> getCell(final int index) {
    return new Tables.AbstractCell<R, C, @Nullable V>() {
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
      @CheckForNull
      public V getValue() {
        return at(rowIndex, columnIndex);
      }
    };
  }

  @CheckForNull
  private V getValue(int index) {
    int rowIndex = index / columnList.size();
    int columnIndex = index % columnList.size();
    return at(rowIndex, columnIndex);
  }

  /**
   * Returns a view of all mappings that have the given column key. If the column key isn't in
   * {@link #columnKeySet()}, an empty immutable map is returned.
   *
   * <p>Otherwise, for each row key in {@link #rowKeySet()}, the returned map associates the row key
   * with the corresponding value in the table. Changes to the returned map will update the
   * underlying table, and vice versa.
   *
   * @param columnKey key of column to search for in the table
   * @return the corresponding map from row keys to values
   */
  @Override
  public Map<R, @Nullable V> column(C columnKey) {
    checkNotNull(columnKey);
    Integer columnIndex = columnKeyToIndex.get(columnKey);
    if (columnIndex == null) {
      return emptyMap();
    } else {
      return new Column(columnIndex);
    }
  }

  private class Column extends ArrayMap<R, @Nullable V> {
    final int columnIndex;

    Column(int columnIndex) {
      super(rowKeyToIndex);
      this.columnIndex = columnIndex;
    }

    @Override
    String getKeyRole() {
      return "Row";
    }

    @Override
    @CheckForNull
    V getValue(int index) {
      return at(index, columnIndex);
    }

    @Override
    @CheckForNull
    V setValue(int index, @CheckForNull V newValue) {
      return set(index, columnIndex, newValue);
    }
  }

  /**
   * Returns an immutable set of the valid column keys, including those that are associated with
   * null values only.
   *
   * @return immutable set of column keys
   */
  @Override
  public ImmutableSet<C> columnKeySet() {
    return columnKeyToIndex.keySet();
  }

  @LazyInit @CheckForNull private transient ColumnMap columnMap;

  @Override
  public Map<C, Map<R, @Nullable V>> columnMap() {
    ColumnMap map = columnMap;
    return (map == null) ? columnMap = new ColumnMap() : map;
  }

  @WeakOuter
  private class ColumnMap extends ArrayMap<C, Map<R, @Nullable V>> {
    private ColumnMap() {
      super(columnKeyToIndex);
    }

    @Override
    String getKeyRole() {
      return "Column";
    }

    @Override
    Map<R, @Nullable V> getValue(int index) {
      return new Column(index);
    }

    @Override
    Map<R, @Nullable V> setValue(int index, Map<R, @Nullable V> newValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    @CheckForNull
    public Map<R, @Nullable V> put(C key, Map<R, @Nullable V> value) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns a view of all mappings that have the given row key. If the row key isn't in {@link
   * #rowKeySet()}, an empty immutable map is returned.
   *
   * <p>Otherwise, for each column key in {@link #columnKeySet()}, the returned map associates the
   * column key with the corresponding value in the table. Changes to the returned map will update
   * the underlying table, and vice versa.
   *
   * @param rowKey key of row to search for in the table
   * @return the corresponding map from column keys to values
   */
  @Override
  public Map<C, @Nullable V> row(R rowKey) {
    checkNotNull(rowKey);
    Integer rowIndex = rowKeyToIndex.get(rowKey);
    if (rowIndex == null) {
      return emptyMap();
    } else {
      return new Row(rowIndex);
    }
  }

  private class Row extends ArrayMap<C, @Nullable V> {
    final int rowIndex;

    Row(int rowIndex) {
      super(columnKeyToIndex);
      this.rowIndex = rowIndex;
    }

    @Override
    String getKeyRole() {
      return "Column";
    }

    @Override
    @CheckForNull
    V getValue(int index) {
      return at(rowIndex, index);
    }

    @Override
    @CheckForNull
    V setValue(int index, @CheckForNull V newValue) {
      return set(rowIndex, index, newValue);
    }
  }

  /**
   * Returns an immutable set of the valid row keys, including those that are associated with null
   * values only.
   *
   * @return immutable set of row keys
   */
  @Override
  public ImmutableSet<R> rowKeySet() {
    return rowKeyToIndex.keySet();
  }

  @LazyInit @CheckForNull private transient RowMap rowMap;

  @Override
  public Map<R, Map<C, @Nullable V>> rowMap() {
    RowMap map = rowMap;
    return (map == null) ? rowMap = new RowMap() : map;
  }

  @WeakOuter
  private class RowMap extends ArrayMap<R, Map<C, @Nullable V>> {
    private RowMap() {
      super(rowKeyToIndex);
    }

    @Override
    String getKeyRole() {
      return "Row";
    }

    @Override
    Map<C, @Nullable V> getValue(int index) {
      return new Row(index);
    }

    @Override
    Map<C, @Nullable V> setValue(int index, Map<C, @Nullable V> newValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    @CheckForNull
    public Map<C, @Nullable V> put(R key, Map<C, @Nullable V> value) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns an unmodifiable collection of all values, which may contain duplicates. Changes to the
   * table will update the returned collection.
   *
   * <p>The returned collection's iterator traverses the values of the first row key, the values of
   * the second row key, and so on.
   *
   * @return collection of values
   */
  @Override
  public Collection<@Nullable V> values() {
    return super.values();
  }

  @Override
  Iterator<@Nullable V> valuesIterator() {
    return new AbstractIndexedListIterator<@Nullable V>(size()) {
      @Override
      @CheckForNull
      protected V get(int index) {
        return getValue(index);
      }
    };
  }

  @Override
  Spliterator<@Nullable V> valuesSpliterator() {
    return CollectSpliterators.<@Nullable V>indexed(size(), Spliterator.ORDERED, this::getValue);
  }

  private static final long serialVersionUID = 0;
}
