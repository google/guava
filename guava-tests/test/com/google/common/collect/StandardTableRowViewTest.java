/*
 * Copyright (C) 2026 The Guava Authors
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;

/** Tests retained row views when the table removes or replaces their backing row. */
@GwtCompatible
@NullMarked
public final class StandardTableRowViewTest extends TestCase {
  public void testHashRowReflectsTableClear() {
    checkRowReflectsTableClear(HashBasedTable.create());
  }

  public void testTreeRowReflectsTableClear() {
    checkRowReflectsTableClear(TreeBasedTable.create());
  }

  private static void checkRowReflectsTableClear(Table<Integer, Integer, String> table) {
    table.put(1, 1, "old");
    Map<Integer, String> row = table.row(1);
    Set<Integer> keys = row.keySet();
    Collection<String> values = row.values();
    assertThat(row.get(1)).isEqualTo("old");

    table.clear();

    assertThat(row).isEmpty();
    assertThat(keys).isEmpty();
    assertThat(values).isEmpty();
    assertThat(row.get(1)).isNull();
    row.put(2, "new");
    assertThat(table.row(1)).containsExactly(2, "new");
    assertThat(keys).containsExactly(2);
    assertThat(values).containsExactly("new");
  }

  public void testHashRowWritesThroughAfterRowMapRemove() {
    checkRowWritesThroughAfterRowMapRemove(HashBasedTable.create());
  }

  public void testTreeRowWritesThroughAfterRowMapRemove() {
    checkRowWritesThroughAfterRowMapRemove(TreeBasedTable.create());
  }

  private static void checkRowWritesThroughAfterRowMapRemove(
      Table<Integer, Integer, String> table) {
    table.put(1, 1, "old");
    Map<Integer, String> row = table.row(1);
    assertThat(row.get(1)).isEqualTo("old");

    Map<Integer, String> removed = table.rowMap().remove(1);
    assertThat(row.put(2, "new")).isNull();

    assertThat(table.row(1)).containsExactly(2, "new");
    assertThat(row).containsExactly(2, "new");
    assertThat(removed).containsExactly(1, "old");
    row.clear();
    assertThat(table.containsRow(1)).isFalse();
    row.put(3, "reused");
    assertThat(table.row(1)).containsExactly(3, "reused");
    assertThat(removed).containsExactly(1, "old");
  }

  public void testHashRowReflectsReplacementRow() {
    checkRowReflectsReplacementRow(HashBasedTable.create());
  }

  public void testTreeRowReflectsReplacementRow() {
    checkRowReflectsReplacementRow(TreeBasedTable.create());
  }

  private static void checkRowReflectsReplacementRow(Table<Integer, Integer, String> table) {
    table.put(1, 1, "old");
    Map<Integer, String> row = table.row(1);
    assertThat(row.get(1)).isEqualTo("old");

    Map<Integer, String> removed = table.rowMap().remove(1);
    table.put(1, 2, "replacement");

    assertThat(row).containsExactly(2, "replacement");
    assertThat(row.put(2, "updated")).isEqualTo("replacement");
    assertThat(table.get(1, 2)).isEqualTo("updated");
    assertThat(row.remove(2)).isEqualTo("updated");
    assertThat(table.containsRow(1)).isFalse();
    assertThat(removed).containsExactly(1, "old");
  }

  public void testHashRowAfterLastCellRemoval() {
    checkRowAfterLastCellRemoval(HashBasedTable.create());
  }

  public void testTreeRowAfterLastCellRemoval() {
    checkRowAfterLastCellRemoval(TreeBasedTable.create());
  }

  private static void checkRowAfterLastCellRemoval(Table<Integer, Integer, String> table) {
    table.put(1, 1, "old");
    Map<Integer, String> row = table.row(1);
    assertThat(row.get(1)).isEqualTo("old");

    table.remove(1, 1);
    row.put(2, "new");

    assertThat(table.row(1)).containsExactly(2, "new");
    assertThat(row).containsExactly(2, "new");
  }

  public void testBoundedTreeRowReflectsTableClear() {
    TreeBasedTable<Integer, Integer, String> table = TreeBasedTable.create();
    table.put(1, 0, "outside");
    table.put(1, 1, "old");
    SortedMap<Integer, String> row = table.row(1).headMap(3);
    assertThat(row.get(1)).isEqualTo("old");

    table.clear();

    assertThat(row).isEmpty();
    row.put(2, "new");
    assertThat(row.firstKey()).isEqualTo(2);
    assertThat(row.lastKey()).isEqualTo(2);
    assertThat(table.row(1)).containsExactly(2, "new");
  }

  public void testBoundedTreeRowWritesThroughAfterRowMapRemove() {
    TreeBasedTable<Integer, Integer, String> table = TreeBasedTable.create();
    table.put(1, 0, "outside");
    table.put(1, 1, "old");
    SortedMap<Integer, String> row = table.row(1).tailMap(1).headMap(3);
    assertThat(row.get(1)).isEqualTo("old");

    Map<Integer, String> removed = table.rowMap().remove(1);
    row.put(2, "new");

    assertThat(table.row(1)).containsExactly(2, "new");
    assertThat(row).containsExactly(2, "new");
    assertThat(removed).containsExactly(0, "outside", 1, "old");
  }

  public void testBoundedTreeRowReflectsReplacementRow() {
    TreeBasedTable<Integer, Integer, String> table = TreeBasedTable.create();
    table.put(1, 1, "old");
    SortedMap<Integer, String> row = table.row(1).subMap(1, 3);
    assertThat(row.get(1)).isEqualTo("old");

    Map<Integer, String> removed = table.rowMap().remove(1);
    table.put(1, 0, "outside");
    table.put(1, 2, "replacement");

    assertThat(row).containsExactly(2, "replacement");
    assertThat(row.firstKey()).isEqualTo(2);
    assertThat(row.lastKey()).isEqualTo(2);
    row.clear();
    assertThat(table.row(1)).containsExactly(0, "outside");
    row.put(1, "reused");
    assertThat(table.row(1)).containsExactly(0, "outside", 1, "reused");
    assertThat(removed).containsExactly(1, "old");
  }

  public void testBoundedTreeRowClearPreservesOtherColumns() {
    TreeBasedTable<Integer, Integer, String> table = TreeBasedTable.create();
    table.put(1, 0, "before");
    table.put(1, 1, "inside");
    table.put(1, 3, "after");
    SortedMap<Integer, String> row = table.row(1).subMap(1, 3);
    assertThat(row.get(1)).isEqualTo("inside");

    row.clear();

    assertThat(table.row(1)).containsExactly(0, "before", 3, "after");
    row.put(2, "new");
    assertThat(row).containsExactly(2, "new");
    assertThat(table.row(1)).containsExactly(0, "before", 2, "new", 3, "after");
    row.remove(2);
    assertThat(table.row(1)).containsExactly(0, "before", 3, "after");
  }
}
