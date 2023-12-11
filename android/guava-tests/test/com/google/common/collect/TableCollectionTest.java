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
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.testing.CollectionTestSuiteBuilder;
import com.google.common.collect.testing.MapInterfaceTest;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.SortedSetTestSuiteBuilder;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.TestStringCollectionGenerator;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Collection tests for {@link Table} implementations.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class TableCollectionTest extends TestCase {

  private static final Feature<?>[] COLLECTION_FEATURES = {
    CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_QUERIES
  };

  private static final Feature<?>[] COLLECTION_FEATURES_ORDER = {
    CollectionSize.ANY, CollectionFeature.KNOWN_ORDER, CollectionFeature.ALLOWS_NULL_QUERIES
  };

  private static final Feature<?>[] COLLECTION_FEATURES_REMOVE = {
    CollectionSize.ANY, CollectionFeature.SUPPORTS_REMOVE, CollectionFeature.ALLOWS_NULL_QUERIES
  };

  private static final Feature<?>[] COLLECTION_FEATURES_REMOVE_ORDER = {
    CollectionSize.ANY,
    CollectionFeature.KNOWN_ORDER,
    CollectionFeature.SUPPORTS_REMOVE,
    CollectionFeature.ALLOWS_NULL_QUERIES
  };

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();

    // Not testing rowKeySet() or columnKeySet() of Table.transformValues()
    // since the transformation doesn't affect the row and column key sets.

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Table<String, Integer, Character> table =
                        ArrayTable.create(ImmutableList.copyOf(elements), ImmutableList.of(1, 2));
                    populateForRowKeySet(table, elements);
                    return table.rowKeySet();
                  }
                })
            .named("ArrayTable.rowKeySet")
            .withFeatures(
                CollectionSize.ONE,
                CollectionSize.SEVERAL,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Table<String, Integer, Character> table = HashBasedTable.create();
                    populateForRowKeySet(table, elements);
                    return table.rowKeySet();
                  }
                })
            .named("HashBasedTable.rowKeySet")
            .withFeatures(COLLECTION_FEATURES_REMOVE)
            .withFeatures(CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());

    suite.addTest(
        SortedSetTestSuiteBuilder.using(
                new TestStringSortedSetGenerator() {
                  @Override
                  protected SortedSet<String> create(String[] elements) {
                    TreeBasedTable<String, Integer, Character> table = TreeBasedTable.create();
                    populateForRowKeySet(table, elements);
                    return table.rowKeySet();
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    Collections.sort(insertionOrder);
                    return insertionOrder;
                  }
                })
            .named("TreeBasedTable.rowKeySet")
            .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
            .withFeatures(CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Table<String, Integer, Character> table = HashBasedTable.create();
                    populateForRowKeySet(table, elements);
                    return Tables.unmodifiableTable(table).rowKeySet();
                  }
                })
            .named("unmodifiableTable[HashBasedTable].rowKeySet")
            .withFeatures(COLLECTION_FEATURES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    RowSortedTable<String, Integer, Character> table = TreeBasedTable.create();
                    populateForRowKeySet(table, elements);
                    return Tables.unmodifiableRowSortedTable(table).rowKeySet();
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    Collections.sort(insertionOrder);
                    return insertionOrder;
                  }
                })
            .named("unmodifiableRowSortedTable[TreeBasedTable].rowKeySet")
            .withFeatures(COLLECTION_FEATURES_ORDER)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Table<Integer, String, Character> table =
                        ArrayTable.create(ImmutableList.of(1, 2), ImmutableList.copyOf(elements));
                    populateForColumnKeySet(table, elements);
                    return table.columnKeySet();
                  }
                })
            .named("ArrayTable.columnKeySet")
            .withFeatures(
                CollectionSize.ONE,
                CollectionSize.SEVERAL,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Table<Integer, String, Character> table = HashBasedTable.create();
                    populateForColumnKeySet(table, elements);
                    return table.columnKeySet();
                  }
                })
            .named("HashBasedTable.columnKeySet")
            .withFeatures(COLLECTION_FEATURES_REMOVE)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Table<Integer, String, Character> table = TreeBasedTable.create();
                    populateForColumnKeySet(table, elements);
                    return table.columnKeySet();
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    Collections.sort(insertionOrder);
                    return insertionOrder;
                  }
                })
            .named("TreeBasedTable.columnKeySet")
            .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Table<Integer, String, Character> table = HashBasedTable.create();
                    populateForColumnKeySet(table, elements);
                    return Tables.unmodifiableTable(table).columnKeySet();
                  }
                })
            .named("unmodifiableTable[HashBasedTable].columnKeySet")
            .withFeatures(COLLECTION_FEATURES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    RowSortedTable<Integer, String, Character> table = TreeBasedTable.create();
                    populateForColumnKeySet(table, elements);
                    return Tables.unmodifiableRowSortedTable(table).columnKeySet();
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    Collections.sort(insertionOrder);
                    return insertionOrder;
                  }
                })
            .named("unmodifiableRowSortedTable[TreeBasedTable].columnKeySet")
            .withFeatures(COLLECTION_FEATURES_ORDER)
            .createTestSuite());

    suite.addTest(
        CollectionTestSuiteBuilder.using(
                new TestStringCollectionGenerator() {
                  @Override
                  protected Collection<String> create(String[] elements) {
                    List<Integer> rowKeys = Lists.newArrayList();
                    for (int i = 0; i < elements.length; i++) {
                      rowKeys.add(i);
                    }
                    Table<Integer, Character, String> table =
                        ArrayTable.create(rowKeys, ImmutableList.of('a'));
                    populateForValues(table, elements);
                    return table.values();
                  }
                })
            .named("ArrayTable.values")
            .withFeatures(
                CollectionSize.ONE,
                CollectionSize.SEVERAL,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.KNOWN_ORDER)
            .createTestSuite());

    suite.addTest(
        CollectionTestSuiteBuilder.using(
                new TestStringCollectionGenerator() {
                  @Override
                  protected Collection<String> create(String[] elements) {
                    Table<Integer, Character, String> table = HashBasedTable.create();
                    table.put(1, 'a', "foo");
                    table.clear();
                    populateForValues(table, elements);
                    return table.values();
                  }
                })
            .named("HashBasedTable.values")
            .withFeatures(COLLECTION_FEATURES_REMOVE)
            .withFeatures(CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());

    suite.addTest(
        CollectionTestSuiteBuilder.using(
                new TestStringCollectionGenerator() {
                  @Override
                  protected Collection<String> create(String[] elements) {
                    Table<Integer, Character, String> table = TreeBasedTable.create();
                    table.put(1, 'a', "foo");
                    table.clear();
                    populateForValues(table, elements);
                    return table.values();
                  }
                })
            .named("TreeBasedTable.values")
            .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
            .withFeatures(CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());

    final Function<String, String> removeFirstCharacter =
        new Function<String, String>() {
          @Override
          public String apply(String input) {
            return input.substring(1);
          }
        };

    suite.addTest(
        CollectionTestSuiteBuilder.using(
                new TestStringCollectionGenerator() {
                  @Override
                  protected Collection<String> create(String[] elements) {
                    Table<Integer, Character, String> table = HashBasedTable.create();
                    for (int i = 0; i < elements.length; i++) {
                      table.put(i, 'a', "x" + checkNotNull(elements[i]));
                    }
                    return Tables.transformValues(table, removeFirstCharacter).values();
                  }
                })
            .named("TransformValues.values")
            .withFeatures(COLLECTION_FEATURES_REMOVE)
            .withFeatures(CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
            .createTestSuite());

    suite.addTest(
        CollectionTestSuiteBuilder.using(
                new TestStringCollectionGenerator() {
                  @Override
                  protected Collection<String> create(String[] elements) {
                    Table<Integer, Character, String> table = HashBasedTable.create();
                    table.put(1, 'a', "foo");
                    table.clear();
                    populateForValues(table, elements);
                    return Tables.unmodifiableTable(table).values();
                  }
                })
            .named("unmodifiableTable[HashBasedTable].values")
            .withFeatures(COLLECTION_FEATURES)
            .createTestSuite());

    suite.addTest(
        CollectionTestSuiteBuilder.using(
                new TestStringCollectionGenerator() {
                  @Override
                  protected Collection<String> create(String[] elements) {
                    RowSortedTable<Integer, Character, String> table = TreeBasedTable.create();
                    table.put(1, 'a', "foo");
                    table.clear();
                    populateForValues(table, elements);
                    return Tables.unmodifiableRowSortedTable(table).values();
                  }
                })
            .named("unmodifiableTable[TreeBasedTable].values")
            .withFeatures(COLLECTION_FEATURES_ORDER)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestCellSetGenerator() {
                  @Override
                  public SampleElements<Cell<String, Integer, Character>> samples() {
                    return new SampleElements<>(
                        Tables.immutableCell("bar", 1, 'a'),
                        Tables.immutableCell("bar", 2, 'b'),
                        Tables.immutableCell("bar", 3, (Character) null),
                        Tables.immutableCell("bar", 4, 'b'),
                        Tables.immutableCell("bar", 5, 'b'));
                  }

                  @Override
                  public Set<Cell<String, Integer, Character>> create(Object... elements) {
                    List<Integer> columnKeys = Lists.newArrayList();
                    for (Object element : elements) {
                      @SuppressWarnings("unchecked")
                      Cell<String, Integer, Character> cell =
                          (Cell<String, Integer, Character>) element;
                      columnKeys.add(cell.getColumnKey());
                    }
                    Table<String, Integer, Character> table =
                        ArrayTable.create(ImmutableList.of("bar"), columnKeys);
                    for (Object element : elements) {
                      @SuppressWarnings("unchecked")
                      Cell<String, Integer, Character> cell =
                          (Cell<String, Integer, Character>) element;
                      table.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
                    }
                    return table.cellSet();
                  }

                  @Override
                  Table<String, Integer, Character> createTable() {
                    throw new UnsupportedOperationException();
                  }
                })
            .named("ArrayTable.cellSet")
            .withFeatures(
                CollectionSize.ONE,
                CollectionSize.SEVERAL,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestCellSetGenerator() {
                  @Override
                  Table<String, Integer, Character> createTable() {
                    return HashBasedTable.create();
                  }
                })
            .named("HashBasedTable.cellSet")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.REMOVE_OPERATIONS,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestCellSetGenerator() {
                  @Override
                  Table<String, Integer, Character> createTable() {
                    return TreeBasedTable.create();
                  }
                })
            .named("TreeBasedTable.cellSet")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.REMOVE_OPERATIONS,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestCellSetGenerator() {
                  @Override
                  Table<String, Integer, Character> createTable() {
                    Table<Integer, String, Character> original = TreeBasedTable.create();
                    return Tables.transpose(original);
                  }
                })
            .named("TransposedTable.cellSet")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.REMOVE_OPERATIONS,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestCellSetGenerator() {
                  @Override
                  Table<String, Integer, Character> createTable() {
                    return HashBasedTable.create();
                  }

                  @Override
                  public Set<Cell<String, Integer, Character>> create(Object... elements) {
                    Table<String, Integer, Character> table = createTable();
                    for (Object element : elements) {
                      @SuppressWarnings("unchecked")
                      Cell<String, Integer, Character> cell =
                          (Cell<String, Integer, Character>) element;
                      table.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
                    }
                    return Tables.transformValues(table, Functions.<Character>identity()).cellSet();
                  }
                })
            .named("TransformValues.cellSet")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_QUERIES,
                CollectionFeature.REMOVE_OPERATIONS)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestCellSetGenerator() {
                  @Override
                  Table<String, Integer, Character> createTable() {
                    return Tables.unmodifiableTable(
                        HashBasedTable.<String, Integer, Character>create());
                  }

                  @Override
                  public Set<Cell<String, Integer, Character>> create(Object... elements) {
                    Table<String, Integer, Character> table = HashBasedTable.create();
                    for (Object element : elements) {
                      @SuppressWarnings("unchecked")
                      Cell<String, Integer, Character> cell =
                          (Cell<String, Integer, Character>) element;
                      table.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
                    }
                    return Tables.unmodifiableTable(table).cellSet();
                  }
                })
            .named("unmodifiableTable[HashBasedTable].cellSet")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestCellSetGenerator() {
                  @Override
                  RowSortedTable<String, Integer, Character> createTable() {
                    return Tables.unmodifiableRowSortedTable(
                        TreeBasedTable.<String, Integer, Character>create());
                  }

                  @Override
                  public Set<Cell<String, Integer, Character>> create(Object... elements) {
                    RowSortedTable<String, Integer, Character> table = TreeBasedTable.create();
                    for (Object element : elements) {
                      @SuppressWarnings("unchecked")
                      Cell<String, Integer, Character> cell =
                          (Cell<String, Integer, Character>) element;
                      table.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
                    }
                    return Tables.unmodifiableRowSortedTable(table).cellSet();
                  }
                })
            .named("unmodifiableRowSortedTable[TreeBasedTable].cellSet")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Iterable<String> rowKeys = ImmutableSet.copyOf(elements);
                    Iterable<Integer> columnKeys = ImmutableList.of(1, 2, 3);
                    Table<String, Integer, Character> table =
                        ArrayTable.create(rowKeys, columnKeys);
                    populateForRowKeySet(table, elements);
                    return table.column(1).keySet();
                  }
                })
            .named("ArrayTable.column.keySet")
            .withFeatures(
                CollectionSize.ONE,
                CollectionSize.SEVERAL,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Table<String, Integer, Character> table = HashBasedTable.create();
                    populateForRowKeySet(table, elements);
                    return table.column(1).keySet();
                  }
                })
            .named("HashBasedTable.column.keySet")
            .withFeatures(COLLECTION_FEATURES_REMOVE)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Table<String, Integer, Character> table = TreeBasedTable.create();
                    populateForRowKeySet(table, elements);
                    return table.column(1).keySet();
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    Collections.sort(insertionOrder);
                    return insertionOrder;
                  }
                })
            .named("TreeBasedTable.column.keySet")
            .withFeatures(COLLECTION_FEATURES_REMOVE_ORDER)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Table<String, Integer, Character> table = HashBasedTable.create();
                    populateForRowKeySet(table, elements);
                    return Tables.transformValues(table, Functions.toStringFunction())
                        .column(1)
                        .keySet();
                  }
                })
            .named("TransformValues.column.keySet")
            .withFeatures(COLLECTION_FEATURES_REMOVE)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Table<String, Integer, Character> table = HashBasedTable.create();
                    populateForRowKeySet(table, elements);
                    return Tables.unmodifiableTable(table).column(1).keySet();
                  }
                })
            .named("unmodifiableTable[HashBasedTable].column.keySet")
            .withFeatures(COLLECTION_FEATURES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    RowSortedTable<String, Integer, Character> table = TreeBasedTable.create();
                    populateForRowKeySet(table, elements);
                    return Tables.unmodifiableRowSortedTable(table).column(1).keySet();
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    Collections.sort(insertionOrder);
                    return insertionOrder;
                  }
                })
            .named("unmodifiableRowSortedTable[TreeBasedTable].column.keySet")
            .withFeatures(COLLECTION_FEATURES_ORDER)
            .createTestSuite());

    return suite;
  }

  private static void populateForRowKeySet(
      Table<String, Integer, Character> table, String[] elements) {
    for (String row : elements) {
      table.put(row, 1, 'a');
      table.put(row, 2, 'b');
    }
  }

  private static void populateForColumnKeySet(
      Table<Integer, String, Character> table, String[] elements) {
    for (String column : elements) {
      table.put(1, column, 'a');
      table.put(2, column, 'b');
    }
  }

  private static void populateForValues(
      Table<Integer, Character, String> table, String[] elements) {
    for (int i = 0; i < elements.length; i++) {
      table.put(i, 'a', elements[i]);
    }
  }

  private abstract static class TestCellSetGenerator
      implements TestSetGenerator<Cell<String, Integer, Character>> {
    @Override
    public SampleElements<Cell<String, Integer, Character>> samples() {
      return new SampleElements<>(
          Tables.immutableCell("bar", 1, 'a'),
          Tables.immutableCell("bar", 2, 'b'),
          Tables.immutableCell("foo", 3, 'c'),
          Tables.immutableCell("bar", 1, 'b'),
          Tables.immutableCell("cat", 2, 'b'));
    }

    @Override
    public Set<Cell<String, Integer, Character>> create(Object... elements) {
      Table<String, Integer, Character> table = createTable();
      for (Object element : elements) {
        @SuppressWarnings("unchecked")
        Cell<String, Integer, Character> cell = (Cell<String, Integer, Character>) element;
        table.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
      }
      return table.cellSet();
    }

    abstract Table<String, Integer, Character> createTable();

    @Override
    @SuppressWarnings("unchecked")
    public Cell<String, Integer, Character>[] createArray(int length) {
      return (Cell<String, Integer, Character>[]) new Cell<?, ?, ?>[length];
    }

    @Override
    public List<Cell<String, Integer, Character>> order(
        List<Cell<String, Integer, Character>> insertionOrder) {
      return insertionOrder;
    }
  }

  private abstract static class MapTests extends MapInterfaceTest<String, Integer> {

    MapTests(
        boolean allowsNullValues,
        boolean supportsPut,
        boolean supportsRemove,
        boolean supportsClear,
        boolean supportsIteratorRemove) {
      super(
          false,
          allowsNullValues,
          supportsPut,
          supportsRemove,
          supportsClear,
          supportsIteratorRemove);
    }

    @Override
    protected String getKeyNotInPopulatedMap() {
      return "four";
    }

    @Override
    protected Integer getValueNotInPopulatedMap() {
      return 4;
    }
  }

  abstract static class RowTests extends MapTests {
    RowTests(
        boolean allowsNullValues,
        boolean supportsPut,
        boolean supportsRemove,
        boolean supportsClear,
        boolean supportsIteratorRemove) {
      super(allowsNullValues, supportsPut, supportsRemove, supportsClear, supportsIteratorRemove);
    }

    abstract Table<Character, String, Integer> makeTable();

    @Override
    protected Map<String, Integer> makeEmptyMap() {
      return makeTable().row('a');
    }

    @Override
    protected Map<String, Integer> makePopulatedMap() {
      Table<Character, String, Integer> table = makeTable();
      table.put('a', "one", 1);
      table.put('a', "two", 2);
      table.put('a', "three", 3);
      table.put('b', "four", 4);
      return table.row('a');
    }
  }

  static final Function<@Nullable Integer, @Nullable Integer> DIVIDE_BY_2 =
      new Function<@Nullable Integer, @Nullable Integer>() {
        @Override
        public @Nullable Integer apply(@Nullable Integer input) {
          return (input == null) ? null : input / 2;
        }
      };

  abstract static class ColumnTests extends MapTests {
    ColumnTests(
        boolean allowsNullValues,
        boolean supportsPut,
        boolean supportsRemove,
        boolean supportsClear,
        boolean supportsIteratorRemove) {
      super(allowsNullValues, supportsPut, supportsRemove, supportsClear, supportsIteratorRemove);
    }

    abstract Table<String, Character, Integer> makeTable();

    @Override
    protected Map<String, Integer> makeEmptyMap() {
      return makeTable().column('a');
    }

    @Override
    protected Map<String, Integer> makePopulatedMap() {
      Table<String, Character, Integer> table = makeTable();
      table.put("one", 'a', 1);
      table.put("two", 'a', 2);
      table.put("three", 'a', 3);
      table.put("four", 'b', 4);
      return table.column('a');
    }
  }

  private abstract static class MapMapTests
      extends MapInterfaceTest<String, Map<Integer, Character>> {

    MapMapTests(
        boolean allowsNullValues,
        boolean supportsRemove,
        boolean supportsClear,
        boolean supportsIteratorRemove) {
      super(false, allowsNullValues, false, supportsRemove, supportsClear, supportsIteratorRemove);
    }

    @Override
    protected String getKeyNotInPopulatedMap() {
      return "cat";
    }

    @Override
    protected Map<Integer, Character> getValueNotInPopulatedMap() {
      return ImmutableMap.of();
    }

    /**
     * The version of this test supplied by {@link MapInterfaceTest} fails for this particular map
     * implementation, because {@code map.get()} returns a view collection that changes in the
     * course of a call to {@code remove()}. Thus, the expectation doesn't hold that {@code
     * map.remove(x)} returns the same value which {@code map.get(x)} did immediately beforehand.
     */
    @Override
    public void testRemove() {
      final Map<String, Map<Integer, Character>> map;
      final String keyToRemove;
      try {
        map = makePopulatedMap();
      } catch (UnsupportedOperationException e) {
        return;
      }
      keyToRemove = map.keySet().iterator().next();
      if (supportsRemove) {
        int initialSize = map.size();
        map.get(keyToRemove);
        map.remove(keyToRemove);
        // This line doesn't hold - see the Javadoc comments above.
        // assertEquals(expectedValue, oldValue);
        assertFalse(map.containsKey(keyToRemove));
        assertEquals(initialSize - 1, map.size());
      } else {
        try {
          map.remove(keyToRemove);
          fail("Expected UnsupportedOperationException.");
        } catch (UnsupportedOperationException expected) {
        }
      }
      assertInvariants(map);
    }
  }

  abstract static class RowMapTests extends MapMapTests {
    RowMapTests(
        boolean allowsNullValues,
        boolean supportsRemove,
        boolean supportsClear,
        boolean supportsIteratorRemove) {
      super(allowsNullValues, supportsRemove, supportsClear, supportsIteratorRemove);
    }

    abstract Table<String, Integer, Character> makeTable();

    @Override
    protected Map<String, Map<Integer, Character>> makePopulatedMap() {
      Table<String, Integer, Character> table = makeTable();
      populateTable(table);
      return table.rowMap();
    }

    void populateTable(Table<String, Integer, Character> table) {
      table.put("foo", 1, 'a');
      table.put("bar", 1, 'b');
      table.put("foo", 3, 'c');
    }

    @Override
    protected Map<String, Map<Integer, Character>> makeEmptyMap() {
      return makeTable().rowMap();
    }
  }

  static final Function<@Nullable String, @Nullable Character> FIRST_CHARACTER =
      new Function<@Nullable String, @Nullable Character>() {
        @Override
        public @Nullable Character apply(@Nullable String input) {
          return input == null ? null : input.charAt(0);
        }
      };

  abstract static class ColumnMapTests extends MapMapTests {
    ColumnMapTests(
        boolean allowsNullValues,
        boolean supportsRemove,
        boolean supportsClear,
        boolean supportsIteratorRemove) {
      super(allowsNullValues, supportsRemove, supportsClear, supportsIteratorRemove);
    }

    abstract Table<Integer, String, Character> makeTable();

    @Override
    protected Map<String, Map<Integer, Character>> makePopulatedMap() {
      Table<Integer, String, Character> table = makeTable();
      table.put(1, "foo", 'a');
      table.put(1, "bar", 'b');
      table.put(3, "foo", 'c');
      return table.columnMap();
    }

    @Override
    protected Map<String, Map<Integer, Character>> makeEmptyMap() {
      return makeTable().columnMap();
    }
  }
}
