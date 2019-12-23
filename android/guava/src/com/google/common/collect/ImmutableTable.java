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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotMock;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.compatqual.MonotonicNonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * A {@link Table} whose contents will never change, with many other important properties detailed
 * at {@link ImmutableCollection}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained"> immutable collections</a>.
 *
 * @author Gregory Kick
 * @since 11.0
 */
@GwtCompatible
public abstract class ImmutableTable<R, C, V> extends AbstractTable<R, C, V>
    implements Serializable {
  /** Returns an empty immutable table. */
  @SuppressWarnings("unchecked")
  public static <R, C, V> ImmutableTable<R, C, V> of() {
    return (ImmutableTable<R, C, V>) SparseImmutableTable.EMPTY;
  }

  /** Returns an immutable table containing a single cell. */
  public static <R, C, V> ImmutableTable<R, C, V> of(R rowKey, C columnKey, V value) {
    return new SingletonImmutableTable<>(rowKey, columnKey, value);
  }

  /**
   * Returns an immutable copy of the provided table.
   *
   * <p>The {@link Table#cellSet()} iteration order of the provided table determines the iteration
   * ordering of all views in the returned table. Note that some views of the original table and the
   * copied table may have different iteration orders. For more control over the ordering, create a
   * {@link Builder} and call {@link Builder#orderRowsBy}, {@link Builder#orderColumnsBy}, and
   * {@link Builder#putAll}
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   */
  public static <R, C, V> ImmutableTable<R, C, V> copyOf(
      Table<? extends R, ? extends C, ? extends V> table) {
    if (table instanceof ImmutableTable) {
      @SuppressWarnings("unchecked")
      ImmutableTable<R, C, V> parameterizedTable = (ImmutableTable<R, C, V>) table;
      return parameterizedTable;
    } else {
      return copyOf(table.cellSet());
    }
  }

  private static <R, C, V> ImmutableTable<R, C, V> copyOf(
      Iterable<? extends Cell<? extends R, ? extends C, ? extends V>> cells) {
    ImmutableTable.Builder<R, C, V> builder = ImmutableTable.builder();
    for (Cell<? extends R, ? extends C, ? extends V> cell : cells) {
      builder.put(cell);
    }
    return builder.build();
  }

  /**
   * Returns a new builder. The generated builder is equivalent to the builder created by the {@link
   * Builder#Builder() ImmutableTable.Builder()} constructor.
   */
  public static <R, C, V> Builder<R, C, V> builder() {
    return new Builder<>();
  }

  /**
   * Verifies that {@code rowKey}, {@code columnKey} and {@code value} are non-null, and returns a
   * new entry with those values.
   */
  static <R, C, V> Cell<R, C, V> cellOf(R rowKey, C columnKey, V value) {
    return Tables.immutableCell(
        checkNotNull(rowKey, "rowKey"),
        checkNotNull(columnKey, "columnKey"),
        checkNotNull(value, "value"));
  }

  /**
   * A builder for creating immutable table instances, especially {@code public static final} tables
   * ("constant tables"). Example:
   *
   * <pre>{@code
   * static final ImmutableTable<Integer, Character, String> SPREADSHEET =
   *     new ImmutableTable.Builder<Integer, Character, String>()
   *         .put(1, 'A', "foo")
   *         .put(1, 'B', "bar")
   *         .put(2, 'A', "baz")
   *         .build();
   * }</pre>
   *
   * <p>By default, the order in which cells are added to the builder determines the iteration
   * ordering of all views in the returned table, with {@link #putAll} following the {@link
   * Table#cellSet()} iteration order. However, if {@link #orderRowsBy} or {@link #orderColumnsBy}
   * is called, the views are sorted by the supplied comparators.
   *
   * <p>For empty or single-cell immutable tables, {@link #of()} and {@link #of(Object, Object,
   * Object)} are even more convenient.
   *
   * <p>Builder instances can be reused - it is safe to call {@link #build} multiple times to build
   * multiple tables in series. Each table is a superset of the tables created before it.
   *
   * @since 11.0
   */
  @DoNotMock
  public static final class Builder<R, C, V> {
    private final List<Cell<R, C, V>> cells = Lists.newArrayList();
    @MonotonicNonNullDecl private Comparator<? super R> rowComparator;
    @MonotonicNonNullDecl private Comparator<? super C> columnComparator;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder generated by {@link
     * ImmutableTable#builder}.
     */
    public Builder() {}

    /** Specifies the ordering of the generated table's rows. */
    @CanIgnoreReturnValue
    public Builder<R, C, V> orderRowsBy(Comparator<? super R> rowComparator) {
      this.rowComparator = checkNotNull(rowComparator, "rowComparator");
      return this;
    }

    /** Specifies the ordering of the generated table's columns. */
    @CanIgnoreReturnValue
    public Builder<R, C, V> orderColumnsBy(Comparator<? super C> columnComparator) {
      this.columnComparator = checkNotNull(columnComparator, "columnComparator");
      return this;
    }

    /**
     * Associates the ({@code rowKey}, {@code columnKey}) pair with {@code value} in the built
     * table. Duplicate key pairs are not allowed and will cause {@link #build} to fail.
     */
    @CanIgnoreReturnValue
    public Builder<R, C, V> put(R rowKey, C columnKey, V value) {
      cells.add(cellOf(rowKey, columnKey, value));
      return this;
    }

    /**
     * Adds the given {@code cell} to the table, making it immutable if necessary. Duplicate key
     * pairs are not allowed and will cause {@link #build} to fail.
     */
    @CanIgnoreReturnValue
    public Builder<R, C, V> put(Cell<? extends R, ? extends C, ? extends V> cell) {
      if (cell instanceof Tables.ImmutableCell) {
        checkNotNull(cell.getRowKey(), "row");
        checkNotNull(cell.getColumnKey(), "column");
        checkNotNull(cell.getValue(), "value");
        @SuppressWarnings("unchecked") // all supported methods are covariant
        Cell<R, C, V> immutableCell = (Cell<R, C, V>) cell;
        cells.add(immutableCell);
      } else {
        put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
      }
      return this;
    }

    /**
     * Associates all of the given table's keys and values in the built table. Duplicate row key
     * column key pairs are not allowed, and will cause {@link #build} to fail.
     *
     * @throws NullPointerException if any key or value in {@code table} is null
     */
    @CanIgnoreReturnValue
    public Builder<R, C, V> putAll(Table<? extends R, ? extends C, ? extends V> table) {
      for (Cell<? extends R, ? extends C, ? extends V> cell : table.cellSet()) {
        put(cell);
      }
      return this;
    }

    /**
     * Returns a newly-created immutable table.
     *
     * @throws IllegalArgumentException if duplicate key pairs were added
     */
    public ImmutableTable<R, C, V> build() {
      int size = cells.size();
      switch (size) {
        case 0:
          return of();
        case 1:
          return new SingletonImmutableTable<>(Iterables.getOnlyElement(cells));
        default:
          return RegularImmutableTable.forCells(cells, rowComparator, columnComparator);
      }
    }
  }

  ImmutableTable() {}

  @Override
  public ImmutableSet<Cell<R, C, V>> cellSet() {
    return (ImmutableSet<Cell<R, C, V>>) super.cellSet();
  }

  @Override
  abstract ImmutableSet<Cell<R, C, V>> createCellSet();

  @Override
  final UnmodifiableIterator<Cell<R, C, V>> cellIterator() {
    throw new AssertionError("should never be called");
  }

  @Override
  public ImmutableCollection<V> values() {
    return (ImmutableCollection<V>) super.values();
  }

  @Override
  abstract ImmutableCollection<V> createValues();

  @Override
  final Iterator<V> valuesIterator() {
    throw new AssertionError("should never be called");
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException if {@code columnKey} is {@code null}
   */
  @Override
  public ImmutableMap<R, V> column(C columnKey) {
    checkNotNull(columnKey, "columnKey");
    return MoreObjects.firstNonNull(
        (ImmutableMap<R, V>) columnMap().get(columnKey), ImmutableMap.<R, V>of());
  }

  @Override
  public ImmutableSet<C> columnKeySet() {
    return columnMap().keySet();
  }

  /**
   * {@inheritDoc}
   *
   * <p>The value {@code Map<R, V>} instances in the returned map are {@link ImmutableMap} instances
   * as well.
   */
  @Override
  public abstract ImmutableMap<C, Map<R, V>> columnMap();

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException if {@code rowKey} is {@code null}
   */
  @Override
  public ImmutableMap<C, V> row(R rowKey) {
    checkNotNull(rowKey, "rowKey");
    return MoreObjects.firstNonNull(
        (ImmutableMap<C, V>) rowMap().get(rowKey), ImmutableMap.<C, V>of());
  }

  @Override
  public ImmutableSet<R> rowKeySet() {
    return rowMap().keySet();
  }

  /**
   * {@inheritDoc}
   *
   * <p>The value {@code Map<C, V>} instances in the returned map are {@link ImmutableMap} instances
   * as well.
   */
  @Override
  public abstract ImmutableMap<R, Map<C, V>> rowMap();

  @Override
  public boolean contains(@NullableDecl Object rowKey, @NullableDecl Object columnKey) {
    return get(rowKey, columnKey) != null;
  }

  @Override
  public boolean containsValue(@NullableDecl Object value) {
    return values().contains(value);
  }

  /**
   * Guaranteed to throw an exception and leave the table unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public final void clear() {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the table unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  public final V put(R rowKey, C columnKey, V value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the table unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public final void putAll(Table<? extends R, ? extends C, ? extends V> table) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the table unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  public final V remove(Object rowKey, Object columnKey) {
    throw new UnsupportedOperationException();
  }

  /** Creates the common serialized form for this table. */
  abstract SerializedForm createSerializedForm();

  /**
   * Serialized type for all ImmutableTable instances. It captures the logical contents and
   * preserves iteration order of all views.
   */
  static final class SerializedForm implements Serializable {
    private final Object[] rowKeys;
    private final Object[] columnKeys;

    private final Object[] cellValues;
    private final int[] cellRowIndices;
    private final int[] cellColumnIndices;

    private SerializedForm(
        Object[] rowKeys,
        Object[] columnKeys,
        Object[] cellValues,
        int[] cellRowIndices,
        int[] cellColumnIndices) {
      this.rowKeys = rowKeys;
      this.columnKeys = columnKeys;
      this.cellValues = cellValues;
      this.cellRowIndices = cellRowIndices;
      this.cellColumnIndices = cellColumnIndices;
    }

    static SerializedForm create(
        ImmutableTable<?, ?, ?> table, int[] cellRowIndices, int[] cellColumnIndices) {
      return new SerializedForm(
          table.rowKeySet().toArray(),
          table.columnKeySet().toArray(),
          table.values().toArray(),
          cellRowIndices,
          cellColumnIndices);
    }

    Object readResolve() {
      if (cellValues.length == 0) {
        return of();
      }
      if (cellValues.length == 1) {
        return of(rowKeys[0], columnKeys[0], cellValues[0]);
      }
      ImmutableList.Builder<Cell<Object, Object, Object>> cellListBuilder =
          new ImmutableList.Builder<>(cellValues.length);
      for (int i = 0; i < cellValues.length; i++) {
        cellListBuilder.add(
            cellOf(rowKeys[cellRowIndices[i]], columnKeys[cellColumnIndices[i]], cellValues[i]));
      }
      return RegularImmutableTable.forOrderedComponents(
          cellListBuilder.build(), ImmutableSet.copyOf(rowKeys), ImmutableSet.copyOf(columnKeys));
    }

    private static final long serialVersionUID = 0;
  }

  final Object writeReplace() {
    return createSerializedForm();
  }
}
