package com.google.common.collect;

import static com.google.common.collect.Tables.immutableCell;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Table.Cell;
import com.google.common.testing.CollectorTester;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import junit.framework.TestCase;

public final class TableCollectorsToImmutableTableTest extends TestCase {

  public void testToImmutableTable() {
    Collector<Cell<String, String, Integer>, ?, ImmutableTable<String, String, Integer>> collector =
        TableCollectors.toImmutableTable(Cell::getRowKey, Cell::getColumnKey, Cell::getValue);
    BiPredicate<ImmutableTable<String, String, Integer>, ImmutableTable<String, String, Integer>>
        equivalence = TableCollectorsTest.pairwiseOnResultOf(ImmutableTable::cellSet);
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

  public void testToImmutableTableConflict() {
    Collector<Cell<String, String, Integer>, ?, ImmutableTable<String, String, Integer>> collector =
        TableCollectors.toImmutableTable(Cell::getRowKey, Cell::getColumnKey, Cell::getValue);
    try {
      Stream.of(immutableCell("one", "uno", 1), immutableCell("one", "uno", 2)).collect(collector);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testToImmutableTableNullRowKey() {
    Collector<Cell<String, String, Integer>, ?, ImmutableTable<String, String, Integer>> collector =
        TableCollectors.toImmutableTable(t -> null, Cell::getColumnKey, Cell::getValue);
    try {
      Stream.of(immutableCell("one", "uno", 1)).collect(collector);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testToImmutableTableNullColumnKey() {
    Collector<Cell<String, String, Integer>, ?, ImmutableTable<String, String, Integer>> collector =
        TableCollectors.toImmutableTable(Cell::getRowKey, t -> null, Cell::getValue);
    try {
      Stream.of(immutableCell("one", "uno", 1)).collect(collector);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testToImmutableTableNullValue() {
    Collector<Cell<String, String, Integer>, ?, ImmutableTable<String, String, Integer>> collector =
        TableCollectors.toImmutableTable(Cell::getRowKey, Cell::getColumnKey, t -> null);
    try {
      Stream.of(immutableCell("one", "uno", 1)).collect(collector);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
    collector =
        TableCollectors.toImmutableTable(Cell::getRowKey, Cell::getColumnKey, Cell::getValue);
    try {
      Stream.of(immutableCell("one", "uno", 1), immutableCell("one", "uno", (Integer) null))
          .collect(collector);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testToImmutableTableMerging() {
    Collector<Cell<String, String, Integer>, ?, ImmutableTable<String, String, Integer>> collector =
        TableCollectors.toImmutableTable(
            Cell::getRowKey, Cell::getColumnKey, Cell::getValue, Integer::sum);
    BiPredicate<ImmutableTable<String, String, Integer>, ImmutableTable<String, String, Integer>>
        equivalence = TableCollectorsTest.pairwiseOnResultOf(ImmutableTable::cellSet);
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

  public void testToImmutableTableMergingNullRowKey() {
    Collector<Cell<String, String, Integer>, ?, ImmutableTable<String, String, Integer>> collector =
        TableCollectors.toImmutableTable(
            t -> null, Cell::getColumnKey, Cell::getValue, Integer::sum);
    try {
      Stream.of(immutableCell("one", "uno", 1)).collect(collector);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testToImmutableTableMergingNullColumnKey() {
    Collector<Cell<String, String, Integer>, ?, ImmutableTable<String, String, Integer>> collector =
        TableCollectors.toImmutableTable(Cell::getRowKey, t -> null, Cell::getValue, Integer::sum);
    try {
      Stream.of(immutableCell("one", "uno", 1)).collect(collector);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testToImmutableTableMergingNullValue() {
    Collector<Cell<String, String, Integer>, ?, ImmutableTable<String, String, Integer>> collector =
        TableCollectors.toImmutableTable(
            Cell::getRowKey, Cell::getColumnKey, t -> null, Integer::sum);
    try {
      Stream.of(immutableCell("one", "uno", 1)).collect(collector);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
    collector =
        TableCollectors.toImmutableTable(
            Cell::getRowKey,
            Cell::getColumnKey,
            Cell::getValue,
            (i, j) -> MoreObjects.firstNonNull(i, 0) + MoreObjects.firstNonNull(j, 0));
    try {
      Stream.of(immutableCell("one", "uno", 1), immutableCell("one", "uno", (Integer) null))
          .collect(collector);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testToImmutableTableMergingNullMerge() {
    Collector<Cell<String, String, Integer>, ?, ImmutableTable<String, String, Integer>> collector =
        TableCollectors.toImmutableTable(
            Cell::getRowKey, Cell::getColumnKey, Cell::getValue, (v1, v2) -> null);
    try {
      Stream.of(immutableCell("one", "uno", 1), immutableCell("one", "uno", 2)).collect(collector);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }
}
