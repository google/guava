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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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
    suite.addTestSuite(ArrayRowTests.class);
    suite.addTestSuite(HashRowTests.class);
    suite.addTestSuite(TreeRowTests.class);
    suite.addTestSuite(TransposeRowTests.class);
    suite.addTestSuite(TransformValueRowTests.class);
    suite.addTestSuite(UnmodifiableHashRowTests.class);
    suite.addTestSuite(UnmodifiableTreeRowTests.class);
    suite.addTestSuite(ArrayColumnTests.class);
    suite.addTestSuite(HashColumnTests.class);
    suite.addTestSuite(TreeColumnTests.class);
    suite.addTestSuite(TransposeColumnTests.class);
    suite.addTestSuite(TransformValueColumnTests.class);
    suite.addTestSuite(UnmodifiableHashColumnTests.class);
    suite.addTestSuite(UnmodifiableTreeColumnTests.class);
    suite.addTestSuite(ArrayRowMapTests.class);
    suite.addTestSuite(HashRowMapTests.class);
    suite.addTestSuite(TreeRowMapTests.class);
    suite.addTestSuite(TreeRowMapHeadMapTests.class);
    suite.addTestSuite(TreeRowMapTailMapTests.class);
    suite.addTestSuite(TreeRowMapSubMapTests.class);
    suite.addTestSuite(TransformValueRowMapTests.class);
    suite.addTestSuite(UnmodifiableHashRowMapTests.class);
    suite.addTestSuite(UnmodifiableTreeRowMapTests.class);
    suite.addTestSuite(ArrayColumnMapTests.class);
    suite.addTestSuite(HashColumnMapTests.class);
    suite.addTestSuite(TreeColumnMapTests.class);
    suite.addTestSuite(TransformValueColumnMapTests.class);
    suite.addTestSuite(UnmodifiableHashColumnMapTests.class);
    suite.addTestSuite(UnmodifiableTreeColumnMapTests.class);

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

  private abstract static class RowTests extends MapTests {
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

  @GwtIncompatible // TODO(hhchan): ArrayTable
  public static class ArrayRowTests extends RowTests {
    public ArrayRowTests() {
      super(true, true, false, false, false);
    }

    @Override
    protected String getKeyNotInPopulatedMap() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected Map<String, Integer> makeEmptyMap() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected Table<Character, String, Integer> makeTable() {
      return ArrayTable.create(
          Arrays.asList('a', 'b', 'c'), Arrays.asList("one", "two", "three", "four"));
    }
  }

  public static class HashRowTests extends RowTests {
    public HashRowTests() {
      super(false, true, true, true, true);
    }

    @Override
    Table<Character, String, Integer> makeTable() {
      return HashBasedTable.create();
    }
  }

  public static class TreeRowTests extends RowTests {
    public TreeRowTests() {
      super(false, true, true, true, true);
    }

    @Override
    Table<Character, String, Integer> makeTable() {
      return TreeBasedTable.create();
    }
  }

  public static class TransposeRowTests extends RowTests {
    public TransposeRowTests() {
      super(false, true, true, true, false);
    }

    @Override
    Table<Character, String, Integer> makeTable() {
      Table<String, Character, Integer> original = TreeBasedTable.create();
      return Tables.transpose(original);
    }
  }

  private static final Function<Integer, Integer> DIVIDE_BY_2 =
      new Function<Integer, Integer>() {
        @Override
        public Integer apply(Integer input) {
          return (input == null) ? null : input / 2;
        }
      };

  public static class TransformValueRowTests extends RowTests {
    public TransformValueRowTests() {
      super(false, false, true, true, true);
    }

    @Override
    Table<Character, String, Integer> makeTable() {
      Table<Character, String, Integer> table = HashBasedTable.create();
      return Tables.transformValues(table, DIVIDE_BY_2);
    }

    @Override
    protected Map<String, Integer> makePopulatedMap() {
      Table<Character, String, Integer> table = HashBasedTable.create();
      table.put('a', "one", 2);
      table.put('a', "two", 4);
      table.put('a', "three", 6);
      table.put('b', "four", 8);
      return Tables.transformValues(table, DIVIDE_BY_2).row('a');
    }
  }

  public static class UnmodifiableHashRowTests extends RowTests {
    public UnmodifiableHashRowTests() {
      super(false, false, false, false, false);
    }

    @Override
    Table<Character, String, Integer> makeTable() {
      Table<Character, String, Integer> table = HashBasedTable.create();
      return Tables.unmodifiableTable(table);
    }

    @Override
    protected Map<String, Integer> makePopulatedMap() {
      Table<Character, String, Integer> table = HashBasedTable.create();
      table.put('a', "one", 1);
      table.put('a', "two", 2);
      table.put('a', "three", 3);
      table.put('b', "four", 4);
      return Tables.unmodifiableTable(table).row('a');
    }
  }

  public static class UnmodifiableTreeRowTests extends RowTests {
    public UnmodifiableTreeRowTests() {
      super(false, false, false, false, false);
    }

    @Override
    Table<Character, String, Integer> makeTable() {
      RowSortedTable<Character, String, Integer> table = TreeBasedTable.create();
      return Tables.unmodifiableRowSortedTable(table);
    }

    @Override
    protected Map<String, Integer> makePopulatedMap() {
      RowSortedTable<Character, String, Integer> table = TreeBasedTable.create();
      table.put('a', "one", 1);
      table.put('a', "two", 2);
      table.put('a', "three", 3);
      table.put('b', "four", 4);
      return Tables.unmodifiableRowSortedTable(table).row('a');
    }
  }

  private abstract static class ColumnTests extends MapTests {
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

  @GwtIncompatible // TODO(hhchan): ArrayTable
  public static class ArrayColumnTests extends ColumnTests {
    public ArrayColumnTests() {
      super(true, true, false, false, false);
    }

    @Override
    protected String getKeyNotInPopulatedMap() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected Map<String, Integer> makeEmptyMap() {
      throw new UnsupportedOperationException();
    }

    @Override
    Table<String, Character, Integer> makeTable() {
      return ArrayTable.create(
          Arrays.asList("one", "two", "three", "four"), Arrays.asList('a', 'b', 'c'));
    }
  }

  public static class HashColumnTests extends ColumnTests {
    public HashColumnTests() {
      super(false, true, true, true, false);
    }

    @Override
    Table<String, Character, Integer> makeTable() {
      return HashBasedTable.create();
    }
  }

  public static class TreeColumnTests extends ColumnTests {
    public TreeColumnTests() {
      super(false, true, true, true, false);
    }

    @Override
    Table<String, Character, Integer> makeTable() {
      return TreeBasedTable.create();
    }
  }

  public static class TransposeColumnTests extends ColumnTests {
    public TransposeColumnTests() {
      super(false, true, true, true, true);
    }

    @Override
    Table<String, Character, Integer> makeTable() {
      Table<Character, String, Integer> original = TreeBasedTable.create();
      return Tables.transpose(original);
    }
  }

  public static class TransformValueColumnTests extends ColumnTests {
    public TransformValueColumnTests() {
      super(false, false, true, true, false);
    }

    @Override
    Table<String, Character, Integer> makeTable() {
      Table<String, Character, Integer> table = HashBasedTable.create();
      return Tables.transformValues(table, DIVIDE_BY_2);
    }

    @Override
    protected Map<String, Integer> makePopulatedMap() {
      Table<String, Character, Integer> table = HashBasedTable.create();
      table.put("one", 'a', 1);
      table.put("two", 'a', 2);
      table.put("three", 'a', 3);
      table.put("four", 'b', 4);
      return Tables.transformValues(table, DIVIDE_BY_2).column('a');
    }
  }

  public static class UnmodifiableHashColumnTests extends ColumnTests {
    public UnmodifiableHashColumnTests() {
      super(false, false, false, false, false);
    }

    @Override
    Table<String, Character, Integer> makeTable() {
      Table<String, Character, Integer> table = HashBasedTable.create();
      return Tables.unmodifiableTable(table);
    }

    @Override
    protected Map<String, Integer> makePopulatedMap() {
      Table<String, Character, Integer> table = HashBasedTable.create();
      table.put("one", 'a', 1);
      table.put("two", 'a', 2);
      table.put("three", 'a', 3);
      table.put("four", 'b', 4);
      return Tables.unmodifiableTable(table).column('a');
    }
  }

  public static class UnmodifiableTreeColumnTests extends ColumnTests {
    public UnmodifiableTreeColumnTests() {
      super(false, false, false, false, false);
    }

    @Override
    Table<String, Character, Integer> makeTable() {
      RowSortedTable<String, Character, Integer> table = TreeBasedTable.create();
      return Tables.unmodifiableRowSortedTable(table);
    }

    @Override
    protected Map<String, Integer> makePopulatedMap() {
      RowSortedTable<String, Character, Integer> table = TreeBasedTable.create();
      table.put("one", 'a', 1);
      table.put("two", 'a', 2);
      table.put("three", 'a', 3);
      table.put("four", 'b', 4);
      return Tables.unmodifiableRowSortedTable(table).column('a');
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

  private abstract static class RowMapTests extends MapMapTests {
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

  @GwtIncompatible // TODO(hhchan): ArrayTable
  public static class ArrayRowMapTests extends RowMapTests {
    public ArrayRowMapTests() {
      super(true, false, false, false);
    }

    @Override
    Table<String, Integer, Character> makeTable() {
      return ArrayTable.create(Arrays.asList("foo", "bar", "dog"), Arrays.asList(1, 2, 3));
    }

    @Override
    protected Map<String, Map<Integer, Character>> makeEmptyMap() {
      throw new UnsupportedOperationException();
    }
  }

  public static class HashRowMapTests extends RowMapTests {
    public HashRowMapTests() {
      super(false, true, true, true);
    }

    @Override
    Table<String, Integer, Character> makeTable() {
      return HashBasedTable.create();
    }
  }

  public static class TreeRowMapTests extends RowMapTests {
    public TreeRowMapTests() {
      super(false, true, true, true);
    }

    @Override
    Table<String, Integer, Character> makeTable() {
      return TreeBasedTable.create();
    }
  }

  public static class TreeRowMapHeadMapTests extends RowMapTests {
    public TreeRowMapHeadMapTests() {
      super(false, true, true, true);
    }

    @Override
    TreeBasedTable<String, Integer, Character> makeTable() {
      TreeBasedTable<String, Integer, Character> table = TreeBasedTable.create();
      table.put("z", 1, 'a');
      return table;
    }

    @Override
    protected Map<String, Map<Integer, Character>> makePopulatedMap() {
      TreeBasedTable<String, Integer, Character> table = makeTable();
      populateTable(table);
      return table.rowMap().headMap("x");
    }

    @Override
    protected Map<String, Map<Integer, Character>> makeEmptyMap() {
      return makeTable().rowMap().headMap("x");
    }

    @Override
    protected String getKeyNotInPopulatedMap() {
      return "z";
    }
  }

  public static class TreeRowMapTailMapTests extends RowMapTests {
    public TreeRowMapTailMapTests() {
      super(false, true, true, true);
    }

    @Override
    TreeBasedTable<String, Integer, Character> makeTable() {
      TreeBasedTable<String, Integer, Character> table = TreeBasedTable.create();
      table.put("a", 1, 'a');
      return table;
    }

    @Override
    protected Map<String, Map<Integer, Character>> makePopulatedMap() {
      TreeBasedTable<String, Integer, Character> table = makeTable();
      populateTable(table);
      return table.rowMap().tailMap("b");
    }

    @Override
    protected Map<String, Map<Integer, Character>> makeEmptyMap() {
      return makeTable().rowMap().tailMap("b");
    }

    @Override
    protected String getKeyNotInPopulatedMap() {
      return "a";
    }
  }

  public static class TreeRowMapSubMapTests extends RowMapTests {
    public TreeRowMapSubMapTests() {
      super(false, true, true, true);
    }

    @Override
    TreeBasedTable<String, Integer, Character> makeTable() {
      TreeBasedTable<String, Integer, Character> table = TreeBasedTable.create();
      table.put("a", 1, 'a');
      table.put("z", 1, 'a');
      return table;
    }

    @Override
    protected Map<String, Map<Integer, Character>> makePopulatedMap() {
      TreeBasedTable<String, Integer, Character> table = makeTable();
      populateTable(table);
      return table.rowMap().subMap("b", "x");
    }

    @Override
    protected Map<String, Map<Integer, Character>> makeEmptyMap() {
      return makeTable().rowMap().subMap("b", "x");
    }

    @Override
    protected String getKeyNotInPopulatedMap() {
      return "z";
    }
  }

  private static final Function<String, Character> FIRST_CHARACTER =
      new Function<String, Character>() {
        @Override
        public Character apply(String input) {
          return input == null ? null : input.charAt(0);
        }
      };

  public static class TransformValueRowMapTests extends RowMapTests {
    public TransformValueRowMapTests() {
      super(false, true, true, true);
    }

    @Override
    Table<String, Integer, Character> makeTable() {
      Table<String, Integer, String> original = HashBasedTable.create();
      return Tables.transformValues(original, FIRST_CHARACTER);
    }

    @Override
    protected Map<String, Map<Integer, Character>> makePopulatedMap() {
      Table<String, Integer, String> table = HashBasedTable.create();
      table.put("foo", 1, "apple");
      table.put("bar", 1, "banana");
      table.put("foo", 3, "cat");
      return Tables.transformValues(table, FIRST_CHARACTER).rowMap();
    }
  }

  public static class UnmodifiableHashRowMapTests extends RowMapTests {
    public UnmodifiableHashRowMapTests() {
      super(false, false, false, false);
    }

    @Override
    Table<String, Integer, Character> makeTable() {
      Table<String, Integer, Character> original = HashBasedTable.create();
      return Tables.unmodifiableTable(original);
    }

    @Override
    protected Map<String, Map<Integer, Character>> makePopulatedMap() {
      Table<String, Integer, Character> table = HashBasedTable.create();
      table.put("foo", 1, 'a');
      table.put("bar", 1, 'b');
      table.put("foo", 3, 'c');
      return Tables.unmodifiableTable(table).rowMap();
    }
  }

  public static class UnmodifiableTreeRowMapTests extends RowMapTests {
    public UnmodifiableTreeRowMapTests() {
      super(false, false, false, false);
    }

    @Override
    RowSortedTable<String, Integer, Character> makeTable() {
      RowSortedTable<String, Integer, Character> original = TreeBasedTable.create();
      return Tables.unmodifiableRowSortedTable(original);
    }

    @Override
    protected SortedMap<String, Map<Integer, Character>> makePopulatedMap() {
      RowSortedTable<String, Integer, Character> table = TreeBasedTable.create();
      table.put("foo", 1, 'a');
      table.put("bar", 1, 'b');
      table.put("foo", 3, 'c');
      return Tables.unmodifiableRowSortedTable(table).rowMap();
    }
  }

  private abstract static class ColumnMapTests extends MapMapTests {
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

  @GwtIncompatible // TODO(hhchan): ArrayTable
  public static class ArrayColumnMapTests extends ColumnMapTests {
    public ArrayColumnMapTests() {
      super(true, false, false, false);
    }

    @Override
    Table<Integer, String, Character> makeTable() {
      return ArrayTable.create(Arrays.asList(1, 2, 3), Arrays.asList("foo", "bar", "dog"));
    }

    @Override
    protected Map<String, Map<Integer, Character>> makeEmptyMap() {
      throw new UnsupportedOperationException();
    }
  }

  public static class HashColumnMapTests extends ColumnMapTests {
    public HashColumnMapTests() {
      super(false, true, true, false);
    }

    @Override
    Table<Integer, String, Character> makeTable() {
      return HashBasedTable.create();
    }
  }

  public static class TreeColumnMapTests extends ColumnMapTests {
    public TreeColumnMapTests() {
      super(false, true, true, false);
    }

    @Override
    Table<Integer, String, Character> makeTable() {
      return TreeBasedTable.create();
    }
  }

  public static class TransformValueColumnMapTests extends ColumnMapTests {
    public TransformValueColumnMapTests() {
      super(false, true, true, false);
    }

    @Override
    Table<Integer, String, Character> makeTable() {
      Table<Integer, String, String> original = HashBasedTable.create();
      return Tables.transformValues(original, FIRST_CHARACTER);
    }

    @Override
    protected Map<String, Map<Integer, Character>> makePopulatedMap() {
      Table<Integer, String, String> table = HashBasedTable.create();
      table.put(1, "foo", "apple");
      table.put(1, "bar", "banana");
      table.put(3, "foo", "cat");
      return Tables.transformValues(table, FIRST_CHARACTER).columnMap();
    }
  }

  public static class UnmodifiableHashColumnMapTests extends ColumnMapTests {
    public UnmodifiableHashColumnMapTests() {
      super(false, false, false, false);
    }

    @Override
    Table<Integer, String, Character> makeTable() {
      Table<Integer, String, Character> original = HashBasedTable.create();
      return Tables.unmodifiableTable(original);
    }

    @Override
    protected Map<String, Map<Integer, Character>> makePopulatedMap() {
      Table<Integer, String, Character> table = HashBasedTable.create();
      table.put(1, "foo", 'a');
      table.put(1, "bar", 'b');
      table.put(3, "foo", 'c');
      return Tables.unmodifiableTable(table).columnMap();
    }
  }

  public static class UnmodifiableTreeColumnMapTests extends ColumnMapTests {
    public UnmodifiableTreeColumnMapTests() {
      super(false, false, false, false);
    }

    @Override
    Table<Integer, String, Character> makeTable() {
      RowSortedTable<Integer, String, Character> original = TreeBasedTable.create();
      return Tables.unmodifiableRowSortedTable(original);
    }

    @Override
    protected Map<String, Map<Integer, Character>> makePopulatedMap() {
      RowSortedTable<Integer, String, Character> table = TreeBasedTable.create();
      table.put(1, "foo", 'a');
      table.put(1, "bar", 'b');
      table.put(3, "foo", 'c');
      return Tables.unmodifiableRowSortedTable(table).columnMap();
    }
  }
}
