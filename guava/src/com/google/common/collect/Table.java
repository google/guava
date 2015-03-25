/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A collection that associates an ordered pair of keys, called a row key and a
 * column key, with a single value. A table may be sparse, with only a small
 * fraction of row key / column key pairs possessing a corresponding value.
 *
 * <p>The mappings corresponding to a given row key may be viewed as a {@link
 * Map} whose keys are the columns. The reverse is also available, associating a
 * column with a row key / value map. Note that, in some implementations, data
 * access by column key may have fewer supported operations or worse performance
 * than data access by row key.
 *
 * <p>The methods returning collections or maps always return views of the
 * underlying table. Updating the table can change the contents of those
 * collections, and updating the collections will change the table.
 *
 * <p>All methods that modify the table are optional, and the views returned by
 * the table may or may not be modifiable. When modification isn't supported,
 * those methods will throw an {@link UnsupportedOperationException}.
 * 
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/NewCollectionTypesExplained#Table">
 * {@code Table}</a>.
 *
 * @author Jared Levy
 * @param <R> the type of the table row keys
 * @param <C> the type of the table column keys
 * @param <V> the type of the mapped values
 * @since 7.0
 */
@GwtCompatible
public interface Table<R, C, V> {
  // TODO(jlevy): Consider adding methods similar to ConcurrentMap methods.

  // Accessors

  /**
   * Returns {@code true} if the table contains a mapping with the specified
   * row and column keys.
   *
   * @param rowKey key of row to search for
   * @param columnKey key of column to search for
   */
  boolean contains(@Nullable Object rowKey, @Nullable Object columnKey);

  /**
   * Returns {@code true} if the table contains a mapping with the specified
   * row key.
   *
   * @param rowKey key of row to search for
   */
  boolean containsRow(@Nullable Object rowKey);

  /**
   * Returns {@code true} if the table contains a mapping with the specified
   * column.
   *
   * @param columnKey key of column to search for
   */
  boolean containsColumn(@Nullable Object columnKey);

  /**
   * Returns {@code true} if the table contains a mapping with the specified
   * value.
   *
   * @param value value to search for
   */
  boolean containsValue(@Nullable Object value);

  /**
   * Returns the value corresponding to the given row and column keys, or
   * {@code null} if no such mapping exists.
   *
   * @param rowKey key of row to search for
   * @param columnKey key of column to search for
   */
  V get(@Nullable Object rowKey, @Nullable Object columnKey);

  /** Returns {@code true} if the table contains no mappings. */
  boolean isEmpty();

  /**
   * Returns the number of row key / column key / value mappings in the table.
   */
  int size();

  /**
   * Compares the specified object with this table for equality. Two tables are
   * equal when their cell views, as returned by {@link #cellSet}, are equal.
   */
  @Override
  boolean equals(@Nullable Object obj);

  /**
   * Returns the hash code for this table. The hash code of a table is defined
   * as the hash code of its cell view, as returned by {@link #cellSet}.
   */
  @Override
  int hashCode();

  // Mutators

  /** Removes all mappings from the table. */
  void clear();

  /**
   * Associates the specified value with the specified keys. If the table
   * already contained a mapping for those keys, the old value is replaced with
   * the specified value.
   *
   * @param rowKey row key that the value should be associated with
   * @param columnKey column key that the value should be associated with
   * @param value value to be associated with the specified keys
   * @return the value previously associated with the keys, or {@code null} if
   *     no mapping existed for the keys
   */
  @Nullable V put(R rowKey, C columnKey, V value);

  /**
   * Copies all mappings from the specified table to this table. The effect is
   * equivalent to calling {@link #put} with each row key / column key / value
   * mapping in {@code table}.
   *
   * @param table the table to add to this table
   */
  void putAll(Table<? extends R, ? extends C, ? extends V> table);

  /**
   * Removes the mapping, if any, associated with the given keys.
   *
   * @param rowKey row key of mapping to be removed
   * @param columnKey column key of mapping to be removed
   * @return the value previously associated with the keys, or {@code null} if
   *     no such value existed
   */
  @Nullable V remove(@Nullable Object rowKey, @Nullable Object columnKey);

  // Views

  /**
   * Returns a view of all mappings that have the given row key. For each row
   * key / column key / value mapping in the table with that row key, the
   * returned map associates the column key with the value. If no mappings in
   * the table have the provided row key, an empty map is returned.
   *
   * <p>Changes to the returned map will update the underlying table, and vice
   * versa.
   *
   * @param rowKey key of row to search for in the table
   * @return the corresponding map from column keys to values
   */
  Map<C, V> row(R rowKey);

  /**
   * Returns a view of all mappings that have the given column key. For each row
   * key / column key / value mapping in the table with that column key, the
   * returned map associates the row key with the value. If no mappings in the
   * table have the provided column key, an empty map is returned.
   *
   * <p>Changes to the returned map will update the underlying table, and vice
   * versa.
   *
   * @param columnKey key of column to search for in the table
   * @return the corresponding map from row keys to values
   */
  Map<R, V> column(C columnKey);

  /**
   * Returns a set of all row key / column key / value triplets. Changes to the
   * returned set will update the underlying table, and vice versa. The cell set
   * does not support the {@code add} or {@code addAll} methods.
   *
   * @return set of table cells consisting of row key / column key / value
   *     triplets
   */
  Set<Cell<R, C, V>> cellSet();

  /**
   * Returns a set of row keys that have one or more values in the table.
   * Changes to the set will update the underlying table, and vice versa.
   *
   * @return set of row keys
   */
  Set<R> rowKeySet();

  /**
   * Returns a set of column keys that have one or more values in the table.
   * Changes to the set will update the underlying table, and vice versa.
   *
   * @return set of column keys
   */
  Set<C> columnKeySet();

  /**
   * Returns a collection of all values, which may contain duplicates. Changes
   * to the returned collection will update the underlying table, and vice
   * versa.
   *
   * @return collection of values
   */
  Collection<V> values();

  /**
   * Returns a view that associates each row key with the corresponding map from
   * column keys to values. Changes to the returned map will update this table.
   * The returned map does not support {@code put()} or {@code putAll()}, or
   * {@code setValue()} on its entries.
   *
   * <p>In contrast, the maps returned by {@code rowMap().get()} have the same
   * behavior as those returned by {@link #row}. Those maps may support {@code
   * setValue()}, {@code put()}, and {@code putAll()}.
   *
   * @return a map view from each row key to a secondary map from column keys to
   *     values
   */
  Map<R, Map<C, V>> rowMap();

  /**
   * Returns a view that associates each column key with the corresponding map
   * from row keys to values. Changes to the returned map will update this
   * table. The returned map does not support {@code put()} or {@code putAll()},
   * or {@code setValue()} on its entries.
   *
   * <p>In contrast, the maps returned by {@code columnMap().get()} have the
   * same behavior as those returned by {@link #column}. Those maps may support
   * {@code setValue()}, {@code put()}, and {@code putAll()}.
   *
   * @return a map view from each column key to a secondary map from row keys to
   *     values
   */
  Map<C, Map<R, V>> columnMap();

  /**
   * Row key / column key / value triplet corresponding to a mapping in a table.
   *
   * @since 7.0
   */
  interface Cell<R, C, V> {
    /**
     * Returns the row key of this cell.
     */
    @Nullable R getRowKey();

    /**
     * Returns the column key of this cell.
     */
    @Nullable C getColumnKey();

    /**
     * Returns the value of this cell.
     */
    @Nullable V getValue();

    /**
     * Compares the specified object with this cell for equality. Two cells are
     * equal when they have equal row keys, column keys, and values.
     */
    @Override
    boolean equals(@Nullable Object obj);

    /**
     * Returns the hash code of this cell.
     *
     * <p>The hash code of a table cell is equal to {@link
     * Objects#hashCode}{@code (e.getRowKey(), e.getColumnKey(), e.getValue())}.
     */
    @Override
    int hashCode();
  }
}
