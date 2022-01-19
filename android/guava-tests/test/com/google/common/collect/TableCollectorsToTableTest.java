package com.google.common.collect;

import static com.google.common.collect.Tables.immutableCell;

import com.google.common.collect.Table.Cell;
import com.google.common.testing.CollectorTester;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import junit.framework.TestCase;

public final class TableCollectorsToTableTest extends TestCase {

  public void testToTable() {
    Collector<Cell<String, String, Integer>, ?, Table<String, String, Integer>> collector =
        TableCollectors.toTable(
            Cell::getRowKey, Cell::getColumnKey, Cell::getValue, HashBasedTable::create);
    BiPredicate<Table<String, String, Integer>, Table<String, String, Integer>> equivalence =
        TableCollectorsTest.pairwiseOnResultOf(Table::cellSet);
    CollectorTester.of(collector, equivalence)
        .expectCollects(
            new ImmutableTable.Builder<String, String, Integer>()
                .put("one", "uno", 1)
                .put("two", "dos", 2)
                .put("three", "tres", 3)
                .build(),
            immutableCell("one", "uno", 1),
            immutableCell("two", "dos", 2),
            immutableCell("three", "tres", 3));
  }

  public void testToTableNullMerge() {
    Collector<Cell<String, String, Integer>, ?, Table<String, String, Integer>> collector =
        TableCollectors.toTable(
            Cell::getRowKey,
            Cell::getColumnKey,
            Cell::getValue,
            (Integer v1, Integer v2) -> null,
            HashBasedTable::create);
    BiPredicate<Table<String, String, Integer>, Table<String, String, Integer>> equivalence =
        TableCollectorsTest.pairwiseOnResultOf(Table::cellSet);
    CollectorTester.of(collector, equivalence)
        .expectCollects(
            ImmutableTable.of(), immutableCell("one", "uno", 1), immutableCell("one", "uno", 2));
  }

  public void testToTableNullValues() {
    Collector<Cell<String, String, Integer>, ?, Table<String, String, Integer>> collector =
        TableCollectors.toTable(
            Cell::getRowKey,
            Cell::getColumnKey,
            Cell::getValue,
            () -> ArrayTable.create(ImmutableList.of("one"), ImmutableList.of("uno")));
    try {
      Stream.of(immutableCell("one", "uno", (Integer) null)).collect(collector);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testToTableConflict() {
    Collector<Cell<String, String, Integer>, ?, Table<String, String, Integer>> collector =
        TableCollectors.toTable(
            Cell::getRowKey, Cell::getColumnKey, Cell::getValue, HashBasedTable::create);
    try {
      Stream.of(immutableCell("one", "uno", 1), immutableCell("one", "uno", 2)).collect(collector);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
  }

  public void testToTableMerging() {
    Collector<Cell<String, String, Integer>, ?, Table<String, String, Integer>> collector =
        TableCollectors.toTable(
            Cell::getRowKey,
            Cell::getColumnKey,
            Cell::getValue,
            Integer::sum,
            HashBasedTable::create);
    BiPredicate<Table<String, String, Integer>, Table<String, String, Integer>> equivalence =
        TableCollectorsTest.pairwiseOnResultOf(Table::cellSet);
    CollectorTester.of(collector, equivalence)
        .expectCollects(
            new ImmutableTable.Builder<String, String, Integer>()
                .put("one", "uno", 1)
                .put("two", "dos", 6)
                .put("three", "tres", 3)
                .build(),
            immutableCell("one", "uno", 1),
            immutableCell("two", "dos", 2),
            immutableCell("three", "tres", 3),
            immutableCell("two", "dos", 4));
  }
}
