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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.base.TriConsumer;
import com.google.common.base.TriFunction;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CompatibleWith;
import com.google.errorprone.annotations.DoNotMock;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A collection that associates an ordered pair of keys, called a row key and a column key, with a
 * single value. A table may be sparse, with only a small fraction of row key / column key pairs
 * possessing a corresponding value.
 *
 * <p>The mappings corresponding to a given row key may be viewed as a {@link Map} whose keys are
 * the columns. The reverse is also available, associating a column with a row key / value map. Note
 * that, in some implementations, data access by column key may have fewer supported operations or
 * worse performance than data access by row key.
 *
 * <p>The methods returning collections or maps always return views of the underlying table.
 * Updating the table can change the contents of those collections, and updating the collections
 * will change the table.
 *
 * <p>All methods that modify the table are optional, and the views returned by the table may or may
 * not be modifiable. When modification isn't supported, those methods will throw an {@link
 * UnsupportedOperationException}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#table"> {@code Table}</a>.
 *
 * @author Jared Levy
 * @param <R> the type of the table row keys
 * @param <C> the type of the table column keys
 * @param <V> the type of the mapped values
 * @since 7.0
 */
@DoNotMock("Use ImmutableTable, HashBasedTable, or another implementation")
@GwtCompatible
public interface Table<R, C, V> {

  // Accessors

  /**
   * Returns {@code true} if the table contains a mapping with the specified row and column keys.
   *
   * @param rowKey key of row to search for
   * @param columnKey key of column to search for
   */
  boolean contains(
      @Nullable @CompatibleWith("R") Object rowKey,
      @Nullable @CompatibleWith("C") Object columnKey);

  /**
   * Returns {@code true} if the table contains a mapping with the specified row key.
   *
   * @param rowKey key of row to search for
   */
  boolean containsRow(@Nullable @CompatibleWith("R") Object rowKey);

  /**
   * Returns {@code true} if the table contains a mapping with the specified column.
   *
   * @param columnKey key of column to search for
   */
  boolean containsColumn(@Nullable @CompatibleWith("C") Object columnKey);

  /**
   * Returns {@code true} if the table contains a mapping with the specified value.
   *
   * @param value value to search for
   */
  boolean containsValue(@Nullable @CompatibleWith("V") Object value);

  /**
   * Returns the value corresponding to the given row and column keys, or {@code null} if no such
   * mapping exists.
   *
   * @param rowKey key of row to search for
   * @param columnKey key of column to search for
   */
  @Nullable
  V get(
      @Nullable @CompatibleWith("R") Object rowKey,
      @Nullable @CompatibleWith("C") Object columnKey);

  /** Returns {@code true} if the table contains no mappings. */
  boolean isEmpty();

  /** Returns the number of row key / column key / value mappings in the table. */
  int size();

  /**
   * Compares the specified object with this table for equality. Two tables are equal when their
   * cell views, as returned by {@link #cellSet}, are equal.
   */
  @Override
  boolean equals(@Nullable Object obj);

  /**
   * Returns the hash code for this table. The hash code of a table is defined as the hash code of
   * its cell view, as returned by {@link #cellSet}.
   */
  @Override
  int hashCode();

  // Mutators

  /** Removes all mappings from the table. */
  void clear();

  /**
   * Associates the specified value with the specified keys. If the table already contained a
   * mapping for those keys, the old value is replaced with the specified value.
   *
   * @param rowKey row key that the value should be associated with
   * @param columnKey column key that the value should be associated with
   * @param value value to be associated with the specified keys
   * @return the value previously associated with the keys, or {@code null} if no mapping existed
   *     for the keys
   */
  @CanIgnoreReturnValue
  @Nullable
  V put(R rowKey, C columnKey, V value);

  /**
   * Copies all mappings from the specified table to this table. The effect is equivalent to calling
   * {@link #put} with each row key / column key / value mapping in {@code table}.
   *
   * @param table the table to add to this table
   */
  void putAll(Table<? extends R, ? extends C, ? extends V> table);

  /**
   * Removes the mapping, if any, associated with the given keys.
   *
   * @param rowKey row key of mapping to be removed
   * @param columnKey column key of mapping to be removed
   * @return the value previously associated with the keys, or {@code null} if no such value existed
   */
  @CanIgnoreReturnValue
  @Nullable
  V remove(
      @Nullable @CompatibleWith("R") Object rowKey,
      @Nullable @CompatibleWith("C") Object columnKey);

  // Views

  /**
   * Returns a view of all mappings that have the given row key. For each row key / column key /
   * value mapping in the table with that row key, the returned map associates the column key with
   * the value. If no mappings in the table have the provided row key, an empty map is returned.
   *
   * <p>Changes to the returned map will update the underlying table, and vice versa.
   *
   * @param rowKey key of row to search for in the table
   * @return the corresponding map from column keys to values
   */
  Map<C, V> row(R rowKey);

  /**
   * Returns a view of all mappings that have the given column key. For each row key / column key /
   * value mapping in the table with that column key, the returned map associates the row key with
   * the value. If no mappings in the table have the provided column key, an empty map is returned.
   *
   * <p>Changes to the returned map will update the underlying table, and vice versa.
   *
   * @param columnKey key of column to search for in the table
   * @return the corresponding map from row keys to values
   */
  Map<R, V> column(C columnKey);

  /**
   * Returns a set of all row key / column key / value triplets. Changes to the returned set will
   * update the underlying table, and vice versa. The cell set does not support the {@code add} or
   * {@code addAll} methods.
   *
   * @return set of table cells consisting of row key / column key / value triplets
   */
  Set<Cell<R, C, V>> cellSet();

  /**
   * Returns a set of row keys that have one or more values in the table. Changes to the set will
   * update the underlying table, and vice versa.
   *
   * @return set of row keys
   */
  Set<R> rowKeySet();

  /**
   * Returns a set of column keys that have one or more values in the table. Changes to the set will
   * update the underlying table, and vice versa.
   *
   * @return set of column keys
   */
  Set<C> columnKeySet();

  /**
   * Returns a collection of all values, which may contain duplicates. Changes to the returned
   * collection will update the underlying table, and vice versa.
   *
   * @return collection of values
   */
  Collection<V> values();

  /**
   * Returns a view that associates each row key with the corresponding map from column keys to
   * values. Changes to the returned map will update this table. The returned map does not support
   * {@code put()} or {@code putAll()}, or {@code setValue()} on its entries.
   *
   * <p>In contrast, the maps returned by {@code rowMap().get()} have the same behavior as those
   * returned by {@link #row}. Those maps may support {@code setValue()}, {@code put()}, and {@code
   * putAll()}.
   *
   * @return a map view from each row key to a secondary map from column keys to values
   */
  Map<R, Map<C, V>> rowMap();

  /**
   * Returns a view that associates each column key with the corresponding map from row keys to
   * values. Changes to the returned map will update this table. The returned map does not support
   * {@code put()} or {@code putAll()}, or {@code setValue()} on its entries.
   *
   * <p>In contrast, the maps returned by {@code columnMap().get()} have the same behavior as those
   * returned by {@link #column}. Those maps may support {@code setValue()}, {@code put()}, and
   * {@code putAll()}.
   *
   * @return a map view from each column key to a secondary map from row keys to values
   */
  Map<C, Map<R, V>> columnMap();

  /**
   * Row key / column key / value triplet corresponding to a mapping in a table.
   *
   * @since 7.0
   */
  interface Cell<R, C, V> {
    /** Returns the row key of this cell. */
    @Nullable
    R getRowKey();

    /** Returns the column key of this cell. */
    @Nullable
    C getColumnKey();

    /** Returns the value of this cell. */
    @Nullable
    V getValue();

    /**
     * Compares the specified object with this cell for equality. Two cells are equal when they have
     * equal row keys, column keys, and values.
     */
    @Override
    boolean equals(@Nullable Object obj);

    /**
     * Returns the hash code of this cell.
     *
     * <p>The hash code of a table cell is equal to {@link Objects#hashCode}{@code (e.getRowKey(),
     * e.getColumnKey(), e.getValue())}.
     */
    @Override
    int hashCode();
  }

  // Defaultable methods

  /**
   * Returns the value to which the specified keys is mapped, or
   * {@code defaultValue} if this table contains no mapping for the keys.
   *
   * @param rowKey       the row key whose associated value is to be returned
   * @param columnKey    the column key whose associated value is to be returned
   * @param defaultValue the default mapping of the keys
   *
   * @return the value to which the specified keys is mapped, or
   * {@code defaultValue} if this table contains no mapping for the keys
   * @implSpec The default implementation makes no guarantees about synchronization
   * or atomicity properties of this method. Any implementation providing
   * atomicity guarantees must override this method and document its
   * concurrency properties.
   */
  default V getOrDefault(
      @Nullable @CompatibleWith("R") Object rowKey,
      @Nullable @CompatibleWith("C") Object columnKey,
      V defaultValue) {
    V v;
    return (((v = get(rowKey, columnKey)) != null) || contains(rowKey, columnKey))
        ? v
        : defaultValue;
  }

  /**
   * Performs the given action for each cell in this table until all cells
   * have been processed or the action throws an exception.   Unless
   * otherwise specified by the implementing class, actions are performed in
   * the order of cell set iteration (if an iteration order is specified.)
   * Exceptions thrown by the action are relayed to the caller.
   *
   * @param action The action to be performed for each cell
   *
   * @implSpec The default implementation is equivalent to, for this {@code table}:
   *
   * <pre> {@code
   * for (Table.Cell<K, V> cell : table.cellSet())
   *     action.accept(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
   * }</pre>
   *
   * <p>The default implementation makes no guarantees about synchronization
   * or atomicity properties of this method. Any implementation providing
   * atomicity guarantees must override this method and document its
   * concurrency properties.
   */
  default void forEach(TriConsumer<? super R, ? super C, ? super V> action) {
    checkNotNull(action);
    for (Cell<R, C, V> cell : cellSet()) {
      R r;
      C c;
      V v;
      try {
        r = cell.getRowKey();
        c = cell.getColumnKey();
        v = cell.getValue();
      } catch (IllegalStateException ise) {
        // this usually means the cell is no longer in the table.
        throw new ConcurrentModificationException(ise);
      }
      action.accept(r, c, v);
    }
  }

  /**
   * Replaces each cell's value with the result of invoking the given
   * function on that cell until all cells have been processed or the
   * function throws an exception.  Exceptions thrown by the function are
   * relayed to the caller.
   *
   * @param function the function to apply to each cell
   *
   * @implSpec The default implementation is equivalent to, for this {@code table}:
   *
   * <pre> {@code
   * for (Table.Cell<K, V> cell : table.cellSet())
   *     cell.setValue(function.apply(cell.getRowKey(), cell.getColumnKey(), cell.getValue()));
   * }</pre>
   *
   * <p>The default implementation makes no guarantees about synchronization
   * or atomicity properties of this method. Any implementation providing
   * atomicity guarantees must override this method and document its
   * concurrency properties.
   */
  default void replaceAll(TriFunction<? super R, ? super C, ? super V, ? extends V> function) {
    checkNotNull(function);
    for (Cell<R, C, V> cell : cellSet()) {
      R r;
      C c;
      V v;
      try {
        r = cell.getRowKey();
        c = cell.getColumnKey();
        v = cell.getValue();
      } catch (IllegalStateException ise) {
        // this usually means the cell is no longer in the table.
        throw new ConcurrentModificationException(ise);
      }

      // ise thrown from function is not a cme.
      v = function.apply(r, c, v);

      try {
        put(r, c, v);
      } catch (IllegalStateException ise) {
        // this usually means the cell is no longer in the table.
        throw new ConcurrentModificationException(ise);
      }
    }
  }

  /**
   * If the specified keys is not already associated with a value (or is mapped
   * to {@code null}) associates it with the given value and returns
   * {@code null}, else returns the current value.
   *
   * @param rowKey    row key with which the specified value is to be associated
   * @param columnKey column key with which the specified value is to be associated
   * @param value     value to be associated with the specified keys
   * @return the previous value associated with the specified keys, or
   * {@code null} if there was no mapping for the keys.
   * (A {@code null} return can also indicate that the table
   * previously associated {@code null} with the keys,
   * if the implementation supports null values.)
   *
   * @implSpec The default implementation is equivalent to, for this {@code table}:
   *
   * <pre> {@code
   * V v = table.get(rowKey, columnKey);
   * if (v == null)
   *     v = table.put(rowKey, columnKey, value);
   * return v;
   * }</pre>
   *
   * <p>The default implementation makes no guarantees about synchronization
   * or atomicity properties of this method. Any implementation providing
   * atomicity guarantees must override this method and document its
   * concurrency properties.
   */
  @CanIgnoreReturnValue
  @Nullable
  default V putIfAbsent(R rowKey, C columnKey, V value) {
    V v = get(rowKey, columnKey);
    if (v == null) {
      v = put(rowKey, columnKey, value);
    }
    return v;
  }

  /**
   * Removes the cell for the specified keys only if it is currently
   * mapped to the specified value.
   *
   * @param rowKey    row key with which the specified value is associated
   * @param columnKey column key with which the specified value is associated
   * @param value     value expected to be associated with the specified keys
   * @return {@code true} if the value was removed
   *
   * @implSpec The default implementation is equivalent to, for this {@code table}:
   *
   * <pre> {@code
   * if (table.contains(rowKey, columnKey)
   *         && Objects.equals(table.get(rowKey, columnKey), value)) {
   *     table.remove(rowKey, columnKey);
   *     return true;
   * } else
   *     return false;
   * }</pre>
   *
   * <p>The default implementation makes no guarantees about synchronization
   * or atomicity properties of this method. Any implementation providing
   * atomicity guarantees must override this method and document its
   * concurrency properties.
   */
  @CanIgnoreReturnValue
  default boolean remove(
      @Nullable @CompatibleWith("R") Object rowKey,
      @Nullable @CompatibleWith("C") Object columnKey,
      @Nullable @CompatibleWith("V") Object value) {
    Object curValue = get(rowKey, columnKey);
    if (!Objects.equal(curValue, value) || (curValue == null && !contains(rowKey, columnKey))) {
      return false;
    }
    remove(rowKey, columnKey);
    return true;
  }

  /**
   * Replaces the cell for the specified keys only if currently
   * mapped to the specified value.
   *
   * @param rowKey    row key with which the specified value is associated
   * @param columnKey column key with which the specified value is associated
   * @param oldValue  value expected to be associated with the specified keys
   * @param newValue  value to be associated with the specified keys
   * @return {@code true} if the value was replaced
   * @implSpec The default implementation is equivalent to, for this {@code table}:
   *
   * <pre> {@code
   * if (table.contains(rowKey, columnKey)
   *         && Objects.equals(table.get(rowKey, columnKey), oldValue)) {
   *     table.put(rowKey, columnKey, newValue);
   *     return true;
   * } else
   *     return false;
   * }</pre>
   *
   * <p>The default implementation does not throw NullPointerException
   * for maps that do not support null values if oldValue is null unless
   * newValue is also null.
   *
   * <p>The default implementation makes no guarantees about synchronization
   * or atomicity properties of this method. Any implementation providing
   * atomicity guarantees must override this method and document its
   * concurrency properties.
   */
  @CanIgnoreReturnValue
  default boolean replace(R rowKey, C columnKey, V oldValue, V newValue) {
    Object curValue = get(rowKey, columnKey);
    if (!Objects.equal(curValue, oldValue) || (curValue == null && !contains(rowKey, columnKey))) {
      return false;
    }
    put(rowKey, columnKey, newValue);
    return true;
  }

  /**
   * Replaces the cell for the specified keys only if it is
   * currently mapped to some value.
   *
   * @param rowKey    row key with which the specified value is associated
   * @param columnKey column key with which the specified value is associated
   * @param value     value to be associated with the specified keys
   * @return the previous value associated with the specified keys, or
   * {@code null} if there was no mapping for the keys.
   * (A {@code null} return can also indicate that the table
   * previously associated {@code null} with the keys,
   * if the implementation supports null values.)
   * @implSpec The default implementation is equivalent to, for this {@code table}:
   *
   * <pre> {@code
   * if (table.contains(rowKey, columnKey)) {
   *     return table.put(rowKey, columnKey, value);
   * } else
   *     return null;
   * }</pre>
   *
   * <p>The default implementation makes no guarantees about synchronization
   * or atomicity properties of this method. Any implementation providing
   * atomicity guarantees must override this method and document its
   * concurrency properties.
   */
  default V replace(R rowKey, C columnKey, V value) {
    V curValue;
    if (((curValue = get(rowKey, columnKey)) != null)
        || contains(rowKey, columnKey)) {
      curValue = put(rowKey, columnKey, value);
    }
    return curValue;
  }


  /**
   * If the specified keys is not already associated with a value (or is mapped
   * to {@code null}), attempts to compute its value using the given mapping
   * function and enters it into this table unless {@code null}.
   *
   * <p>If the mapping function returns {@code null}, no mapping is recorded.
   * If the mapping function itself throws an (unchecked) exception, the
   * exception is rethrown, and no mapping is recorded.  The most
   * common usage is to construct a new object serving as an initial
   * mapped value or memoized result, as in:
   *
   * <pre> {@code
   * table.computeIfAbsent(rowKey, columnKey, (r, c) -> new Value(f(r, c)));
   * }</pre>
   *
   * <p>Or to implement a multi-value table, {@code Table<R,C,Collection<V>>},
   * supporting multiple values per keys:
   *
   * <pre> {@code
   * table.computeIfAbsent(rowKey, columnKey, (r, c) -> new HashSet<V>()).add(v);
   * }</pre>
   *
   * <p>The mapping function should not modify this table during computation.
   *
   * @param rowKey          row key with which the specified value is to be associated
   * @param columnKey       column key with which the specified value is to be associated
   * @param mappingFunction the mapping function to compute a value
   * @return the current (existing or computed) value associated with
   * the specified keys, or null if the computed value is null
   * @implSpec The default implementation is equivalent to the following steps for this
   * {@code table}, then returning the current value or {@code null} if now
   * absent:
   *
   * <pre> {@code
   * if (table.get(rowKey, columnKey) == null) {
   *     V newValue = mappingFunction.apply(rowKey, columnKey);
   *     if (newValue != null)
   *         table.put(rowKey, columnKey, newValue);
   * }
   * }</pre>
   *
   * <p>The default implementation makes no guarantees about detecting if the
   * mapping function modifies this table during computation and, if
   * appropriate, reporting an error. Non-concurrent implementations should
   * override this method and, on a best-effort basis, throw a
   * {@code ConcurrentModificationException} if it is detected that the
   * mapping function modifies this table during computation. Concurrent
   * implementations should override this method and, on a best-effort basis,
   * throw an {@code IllegalStateException} if it is detected that the
   * mapping function modifies this table during computation and as a result
   * computation would never complete.
   *
   * <p>The default implementation makes no guarantees about synchronization
   * or atomicity properties of this method. Any implementation providing
   * atomicity guarantees must override this method and document its
   * concurrency properties.
   */
  default V computeIfAbsent(R rowKey, C columnKey,
                            BiFunction<? super R, ? super C, ? extends V> mappingFunction) {
    checkNotNull(mappingFunction);
    V v;
    if ((v = get(rowKey, columnKey)) == null) {
      V newValue;
      if ((newValue = mappingFunction.apply(rowKey, columnKey)) != null) {
        put(rowKey, columnKey, newValue);
        return newValue;
      }
    }
    return v;
  }

  /**
   * If the value for the specified keys is present and non-null, attempts to
   * compute a new mapping given the keys and its current mapped value.
   *
   * <p>If the remapping function returns {@code null}, the mapping is removed.
   * If the remapping function itself throws an (unchecked) exception, the
   * exception is rethrown, and the current mapping is left unchanged.
   *
   * <p>The remapping function should not modify this table during computation.
   *
   * @param rowKey            row key with which the specified value is to be associated
   * @param columnKey         column key with which the specified value is to be associated
   * @param remappingFunction the remapping function to compute a value
   * @return the new value associated with the specified keys, or null if none
   * @implSpec The default implementation is equivalent to performing the following
   * steps for this {@code table}, then returning the current value or
   * {@code null} if now absent:
   *
   * <pre> {@code
   * if (table.get(rowKey, columnKey) != null) {
   *     V oldValue = table.get(rowKey, columnKey);
   *     V newValue = remappingFunction.apply(rowKey, columnKey, oldValue);
   *     if (newValue != null)
   *         table.put(rowKey, columnKey, newValue);
   *     else
   *         table.remove(rowKey, columnKey);
   * }
   * }</pre>
   *
   * <p>The default implementation makes no guarantees about detecting if the
   * remapping function modifies this table during computation and, if
   * appropriate, reporting an error. Non-concurrent implementations should
   * override this method and, on a best-effort basis, throw a
   * {@code ConcurrentModificationException} if it is detected that the
   * remapping function modifies this table during computation. Concurrent
   * implementations should override this method and, on a best-effort basis,
   * throw an {@code IllegalStateException} if it is detected that the
   * remapping function modifies this table during computation and as a result
   * computation would never complete.
   *
   * <p>The default implementation makes no guarantees about synchronization
   * or atomicity properties of this method. Any implementation providing
   * atomicity guarantees must override this method and document its
   * concurrency properties.
   */
  default V computeIfPresent(R rowKey, C columnKey,
                             TriFunction<? super R, ? super C, ? super V, ? extends V> remappingFunction) {
    checkNotNull(remappingFunction);
    V oldValue;
    if ((oldValue = get(rowKey, columnKey)) != null) {
      V newValue = remappingFunction.apply(rowKey, columnKey, oldValue);
      if (newValue != null) {
        put(rowKey, columnKey, newValue);
        return newValue;
      } else {
        remove(rowKey, columnKey);
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * Attempts to compute a mapping for the specified keys and its current
   * mapped value (or {@code null} if there is no current mapping). For
   * example, to either create or append a {@code String} msg to a value
   * mapping:
   *
   * <pre> {@code
   * table.compute(rowKey, columnKey, (r, c, v) -> (v == null) ? msg : v.concat(msg))}</pre>
   * (Method {@link #merge merge()} is often simpler to use for such purposes.)
   *
   * <p>If the remapping function returns {@code null}, the mapping is removed
   * (or remains absent if initially absent).  If the remapping function
   * itself throws an (unchecked) exception, the exception is rethrown, and
   * the current mapping is left unchanged.
   *
   * <p>The remapping function should not modify this table during computation.
   *
   * @param rowKey            row key with which the specified value is to be associated
   * @param columnKey         column key with which the specified value is to be associated
   * @param remappingFunction the remapping function to compute a value
   * @return the new value associated with the specified keys, or null if none
   * @implSpec The default implementation is equivalent to performing the following
   * steps for this {@code table}, then returning the current value or
   * {@code null} if absent:
   *
   * <pre> {@code
   * V oldValue = table.get(rowKey, columnKey);
   * V newValue = remappingFunction.apply(rowKey, columnKey, oldValue);
   * if (oldValue != null) {
   *    if (newValue != null)
   *       table.put(rowKey, columnKey, newValue);
   *    else
   *       table.remove(rowKey, columnKey);
   * } else {
   *    if (newValue != null)
   *       table.put(rowKey, columnKey, newValue);
   *    else
   *       return null;
   * }
   * }</pre>
   *
   * <p>The default implementation makes no guarantees about detecting if the
   * remapping function modifies this table during computation and, if
   * appropriate, reporting an error. Non-concurrent implementations should
   * override this method and, on a best-effort basis, throw a
   * {@code ConcurrentModificationException} if it is detected that the
   * remapping function modifies this table during computation. Concurrent
   * implementations should override this method and, on a best-effort basis,
   * throw an {@code IllegalStateException} if it is detected that the
   * remapping function modifies this table during computation and as a result
   * computation would never complete.
   *
   * <p>The default implementation makes no guarantees about synchronization
   * or atomicity properties of this method. Any implementation providing
   * atomicity guarantees must override this method and document its
   * concurrency properties.
   */
  default V compute(R rowKey, C columnKey,
                    TriFunction<? super R, ? super C, ? super V, ? extends V> remappingFunction) {
    checkNotNull(remappingFunction);
    V oldValue = get(rowKey, columnKey);

    V newValue = remappingFunction.apply(rowKey, columnKey, oldValue);
    if (newValue == null) {
      // delete mapping
      if (oldValue != null || contains(rowKey, columnKey)) {
        // something to remove
        remove(rowKey, columnKey);
        return null;
      } else {
        // nothing to do. Leave things as they were.
        return null;
      }
    } else {
      // add or replace old mapping
      put(rowKey, columnKey, newValue);
      return newValue;
    }
  }

  /**
   * If the specified keys is not already associated with a value or is
   * associated with null, associates it with the given non-null value.
   * Otherwise, replaces the associated value with the results of the given
   * remapping function, or removes if the result is {@code null}. This
   * method may be of use when combining multiple mapped values for keys.
   * For example, to either create or append a {@code String msg} to a
   * value mapping:
   *
   * <pre> {@code
   * table.merge(rowKey, columnKey, msg, String::concat)
   * }</pre>
   *
   * <p>If the remapping function returns {@code null}, the mapping is removed.
   * If the remapping function itself throws an (unchecked) exception, the
   * exception is rethrown, and the current mapping is left unchanged.
   *
   * <p>The remapping function should not modify this table during computation.
   *
   * @param rowKey            row key with which the resulting value is to be associated
   * @param columnKey         column key with which the resulting value is to be associated
   * @param value             the non-null value to be merged with the existing value
   *                          associated with the keys or, if no existing value or a null value
   *                          is associated with the keys, to be associated with the keys
   * @param remappingFunction the remapping function to recompute a value if
   *                          present
   * @return the new value associated with the specified keys, or null if no
   * value is associated with the keys
   * @implSpec The default implementation is equivalent to performing the following
   * steps for this {@code table}, then returning the current value or
   * {@code null} if absent:
   *
   * <pre> {@code
   * V oldValue = table.get(rowKey, columnKey);
   * V newValue = (oldValue == null) ? value :
   *              remappingFunction.apply(oldValue, value);
   * if (newValue == null)
   *     table.remove(rowKey, columnKey);
   * else
   *     table.put(rowKey, columnKey, newValue);
   * }</pre>
   *
   * <p>The default implementation makes no guarantees about detecting if the
   * remapping function modifies this table during computation and, if
   * appropriate, reporting an error. Non-concurrent implementations should
   * override this method and, on a best-effort basis, throw a
   * {@code ConcurrentModificationException} if it is detected that the
   * remapping function modifies this table during computation. Concurrent
   * implementations should override this method and, on a best-effort basis,
   * throw an {@code IllegalStateException} if it is detected that the
   * remapping function modifies this table during computation and as a result
   * computation would never complete.
   *
   * <p>The default implementation makes no guarantees about synchronization
   * or atomicity properties of this method. Any implementation providing
   * atomicity guarantees must override this method and document its
   * concurrency properties.
   */
  default V merge(R rowKey, C columnKey, V value,
                  BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    checkNotNull(remappingFunction);
    checkNotNull(value);
    V oldValue = get(rowKey, columnKey);
    V newValue = (oldValue == null) ? value :
        remappingFunction.apply(oldValue, value);
    if (newValue == null) {
      remove(rowKey, columnKey);
    } else {
      put(rowKey, columnKey, newValue);
    }
    return newValue;
  }
}
