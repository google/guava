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

import static com.google.common.collect.Tables.immutableCell;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Table.Cell;
import com.google.common.testing.CollectorTester;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import junit.framework.TestCase;

/** Unit tests for {@link TableCollectors}. */
@GwtCompatible(emulated = true)
public class TableCollectorsTest extends TestCase {
  public void testToImmutableTable() {
    Collector<Cell<String, String, Integer>, ?, ImmutableTable<String, String, Integer>> collector =
        TableCollectors.toImmutableTable(Cell::getRowKey, Cell::getColumnKey, Cell::getValue);
    BiPredicate<ImmutableTable<String, String, Integer>, ImmutableTable<String, String, Integer>>
        equivalence = pairwiseOnResultOf(ImmutableTable::cellSet);
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
        equivalence = pairwiseOnResultOf(ImmutableTable::cellSet);
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

  public void testToTable() {
    Collector<Cell<String, String, Integer>, ?, Table<String, String, Integer>> collector =
        TableCollectors.toTable(
            Cell::getRowKey, Cell::getColumnKey, Cell::getValue, HashBasedTable::create);
    BiPredicate<Table<String, String, Integer>, Table<String, String, Integer>> equivalence =
        pairwiseOnResultOf(Table::cellSet);
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
        pairwiseOnResultOf(Table::cellSet);
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
        pairwiseOnResultOf(Table::cellSet);
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

  // This function specifically returns a BiPredicate, because Guava7’s Equivalence class does not
  // actually implement BiPredicate, and CollectorTests expects a BiPredicate.
  static <C, E, R extends Iterable<E>> BiPredicate<C, C> pairwiseOnResultOf(Function<C, R> arg) {
    Equivalence<C> equivalence = Equivalence.equals().<E>pairwise().onResultOf(arg);
    return equivalence::equivalent;
  }
}
