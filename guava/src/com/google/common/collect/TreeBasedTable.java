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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Supplier;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import javax.annotation.CheckForNull;

/**
 * Implementation of {@code Table} whose row keys and column keys are ordered by their natural
 * ordering or by supplied comparators. When constructing a {@code TreeBasedTable}, you may provide
 * comparators for the row keys and the column keys, or you may use natural ordering for both.
 *
 * <p>The {@link #rowKeySet} method returns a {@link SortedSet} and the {@link #rowMap} method
 * returns a {@link SortedMap}, instead of the {@link Set} and {@link Map} specified by the {@link
 * Table} interface.
 *
 * <p>The views returned by {@link #column}, {@link #columnKeySet()}, and {@link #columnMap()} have
 * iterators that don't support {@code remove()}. Otherwise, all optional operations are supported.
 * Null row keys, columns keys, and values are not supported.
 *
 * <p>Lookups by row key are often faster than lookups by column key, because the data is stored in
 * a {@code Map<R, Map<C, V>>}. A method call like {@code column(columnKey).get(rowKey)} still runs
 * quickly, since the row key is provided. However, {@code column(columnKey).size()} takes longer,
 * since an iteration across all row keys occurs.
 *
 * <p>Because a {@code TreeBasedTable} has unique sorted values for a given row, both {@code
 * row(rowKey)} and {@code rowMap().get(rowKey)} are {@link SortedMap} instances, instead of the
 * {@link Map} specified in the {@link Table} interface.
 *
 * <p>Note that this implementation is not synchronized. If multiple threads access this table
 * concurrently and one of the threads modifies the table, it must be synchronized externally.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#table">{@code Table}</a>.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 7.0
 */
@GwtCompatible(serializable = true)
@ElementTypesAreNonnullByDefault
public class TreeBasedTable<R, C, V> extends StandardRowSortedTable<R, C, V> {
  private final Comparator<? super C> columnComparator;

  private static class Factory<C, V> implements Supplier<TreeMap<C, V>>, Serializable {
    final Comparator<? super C> comparator;

    Factory(Comparator<? super C> comparator) {
      this.comparator = comparator;
    }

    @Override
    public TreeMap<C, V> get() {
      return new TreeMap<>(comparator);
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Creates an empty {@code TreeBasedTable} that uses the natural orderings of both row and column
   * keys.
   *
   * <p>The method signature specifies {@code R extends Comparable} with a raw {@link Comparable},
   * instead of {@code R extends Comparable<? super R>}, and the same for {@code C}. That's
   * necessary to support classes defined without generics.
   */
  public static <R extends Comparable, C extends Comparable, V> TreeBasedTable<R, C, V> create() {
    return new TreeBasedTable<>(Ordering.natural(), Ordering.natural());
  }

  /**
   * Creates an empty {@code TreeBasedTable} that is ordered by the specified comparators.
   *
   * @param rowComparator the comparator that orders the row keys
   * @param columnComparator the comparator that orders the column keys
   */
  public static <R, C, V> TreeBasedTable<R, C, V> create(
      Comparator<? super R> rowComparator, Comparator<? super C> columnComparator) {
    checkNotNull(rowComparator);
    checkNotNull(columnComparator);
    return new TreeBasedTable<>(rowComparator, columnComparator);
  }

  /**
   * Creates a {@code TreeBasedTable} with the same mappings and sort order as the specified {@code
   * TreeBasedTable}.
   */
  public static <R, C, V> TreeBasedTable<R, C, V> create(TreeBasedTable<R, C, ? extends V> table) {
    TreeBasedTable<R, C, V> result =
        new TreeBasedTable<>(table.rowComparator(), table.columnComparator());
    result.putAll(table);
    return result;
  }

  TreeBasedTable(Comparator<? super R> rowComparator, Comparator<? super C> columnComparator) {
    super(new TreeMap<R, Map<C, V>>(rowComparator), new Factory<C, V>(columnComparator));
    this.columnComparator = columnComparator;
  }

  // TODO(jlevy): Move to StandardRowSortedTable?

  /**
   * Returns the comparator that orders the rows. With natural ordering, {@link Ordering#natural()}
   * is returned.
   *
   * @deprecated Use {@code table.rowKeySet().comparator()} instead.
   */
  @Deprecated
  public Comparator<? super R> rowComparator() {
    /*
     * requireNonNull is safe because the factories require non-null Comparators, which they pass on
     * to the backing collections.
     */
    return requireNonNull(rowKeySet().comparator());
  }

  /**
   * Returns the comparator that orders the columns. With natural ordering, {@link
   * Ordering#natural()} is returned.
   *
   * @deprecated Store the {@link Comparator} alongside the {@link Table}. Or, if you know that the
   *     {@link Table} contains at least one value, you can retrieve the {@link Comparator} with:
   *     {@code ((SortedMap<C, V>) table.rowMap().values().iterator().next()).comparator();}.
   */
  @Deprecated
  public Comparator<? super C> columnComparator() {
    return columnComparator;
  }

  // TODO(lowasser): make column return a SortedMap

  /**
   * {@inheritDoc}
   *
   * <p>Because a {@code TreeBasedTable} has unique sorted values for a given row, this method
   * returns a {@link SortedMap}, instead of the {@link Map} specified in the {@link Table}
   * interface.
   *
   * @since 10.0 (<a href="https://github.com/google/guava/wiki/Compatibility" >mostly
   *     source-compatible</a> since 7.0)
   */
  @Override
  public SortedMap<C, V> row(R rowKey) {
    return new TreeRow(rowKey);
  }

  private class TreeRow extends Row implements SortedMap<C, V> {
    @CheckForNull final C lowerBound;
    @CheckForNull final C upperBound;

    TreeRow(R rowKey) {
      this(rowKey, null, null);
    }

    TreeRow(R rowKey, @CheckForNull C lowerBound, @CheckForNull C upperBound) {
      super(rowKey);
      this.lowerBound = lowerBound;
      this.upperBound = upperBound;
      checkArgument(
          lowerBound == null || upperBound == null || compare(lowerBound, upperBound) <= 0);
    }

    @Override
    public SortedSet<C> keySet() {
      return new Maps.SortedKeySet<>(this);
    }

    @Override
    public Comparator<? super C> comparator() {
      return columnComparator();
    }

    int compare(Object a, Object b) {
      // pretend we can compare anything
      @SuppressWarnings("unchecked")
      Comparator<Object> cmp = (Comparator<Object>) comparator();
      return cmp.compare(a, b);
    }

    boolean rangeContains(@CheckForNull Object o) {
      return o != null
          && (lowerBound == null || compare(lowerBound, o) <= 0)
          && (upperBound == null || compare(upperBound, o) > 0);
    }

    @Override
    public SortedMap<C, V> subMap(C fromKey, C toKey) {
      checkArgument(rangeContains(checkNotNull(fromKey)) && rangeContains(checkNotNull(toKey)));
      return new TreeRow(rowKey, fromKey, toKey);
    }

    @Override
    public SortedMap<C, V> headMap(C toKey) {
      checkArgument(rangeContains(checkNotNull(toKey)));
      return new TreeRow(rowKey, lowerBound, toKey);
    }

    @Override
    public SortedMap<C, V> tailMap(C fromKey) {
      checkArgument(rangeContains(checkNotNull(fromKey)));
      return new TreeRow(rowKey, fromKey, upperBound);
    }

    @Override
    public C firstKey() {
      updateBackingRowMapField();
      if (backingRowMap == null) {
        throw new NoSuchElementException();
      }
      return ((SortedMap<C, V>) backingRowMap).firstKey();
    }

    @Override
    public C lastKey() {
      updateBackingRowMapField();
      if (backingRowMap == null) {
        throw new NoSuchElementException();
      }
      return ((SortedMap<C, V>) backingRowMap).lastKey();
    }

    @CheckForNull transient SortedMap<C, V> wholeRow;

    // If the row was previously empty, we check if there's a new row here every time we're queried.
    void updateWholeRowField() {
      if (wholeRow == null || (wholeRow.isEmpty() && backingMap.containsKey(rowKey))) {
        wholeRow = (SortedMap<C, V>) backingMap.get(rowKey);
      }
    }

    @Override
    @CheckForNull
    SortedMap<C, V> computeBackingRowMap() {
      updateWholeRowField();
      SortedMap<C, V> map = wholeRow;
      if (map != null) {
        if (lowerBound != null) {
          map = map.tailMap(lowerBound);
        }
        if (upperBound != null) {
          map = map.headMap(upperBound);
        }
        return map;
      }
      return null;
    }

    @Override
    void maintainEmptyInvariant() {
      updateWholeRowField();
      if (wholeRow != null && wholeRow.isEmpty()) {
        backingMap.remove(rowKey);
        wholeRow = null;
        backingRowMap = null;
      }
    }

    @Override
    public boolean containsKey(@CheckForNull Object key) {
      return rangeContains(key) && super.containsKey(key);
    }

    @Override
    @CheckForNull
    public V put(C key, V value) {
      checkArgument(rangeContains(checkNotNull(key)));
      return super.put(key, value);
    }
  }

  // rowKeySet() and rowMap() are defined here so they appear in the Javadoc.

  @Override
  public SortedSet<R> rowKeySet() {
    return super.rowKeySet();
  }

  @Override
  public SortedMap<R, Map<C, V>> rowMap() {
    return super.rowMap();
  }

  /** Overridden column iterator to return columns values in globally sorted order. */
  @Override
  Iterator<C> createColumnKeyIterator() {
    Comparator<? super C> comparator = columnComparator();

    Iterator<C> merged =
        Iterators.mergeSorted(
            Iterables.transform(
                backingMap.values(), (Map<C, V> input) -> input.keySet().iterator()),
            comparator);

    return new AbstractIterator<C>() {
      @CheckForNull C lastValue;

      @Override
      @CheckForNull
      protected C computeNext() {
        while (merged.hasNext()) {
          C next = merged.next();
          boolean duplicate = lastValue != null && comparator.compare(next, lastValue) == 0;

          // Keep looping till we find a non-duplicate value.
          if (!duplicate) {
            lastValue = next;
            return lastValue;
          }
        }

        lastValue = null; // clear reference to unused data
        return endOfData();
      }
    };
  }

  private static final long serialVersionUID = 0;
}
