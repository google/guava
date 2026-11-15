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

import static org.junit.Assert.assertThrows;

import java.util.SortedMap;
import java.util.TreeMap;
import junit.framework.TestCase;

/** Tests the endpoint checks on nested sorted row views. */
public class TreeBasedTableRowBoundsTest extends TestCase {
  public void testHeadMapCanReuseExclusiveUpperBound() {
    SortedMap<Integer, String> row = table().row(1).headMap(3);
    assertEquals(row, row.headMap(3));
  }

  public void testSubMapCanReuseExclusiveUpperBound() {
    SortedMap<Integer, String> row = table().row(1).headMap(3);
    assertEquals(ImmutableMap.of(1, "one", 2, "two"), row.subMap(1, 3));
  }

  public void testBoundedHeadMapCanReuseExclusiveUpperBound() {
    SortedMap<Integer, String> row = table().row(1).subMap(1, 3);
    assertEquals(row, row.headMap(3));
  }

  public void testBoundedSubMapCanReuseExclusiveUpperBound() {
    SortedMap<Integer, String> row = table().row(1).subMap(1, 3);
    assertEquals(ImmutableMap.of(2, "two"), row.subMap(2, 3));
  }

  public void testEmptyViewsWithinBounds() {
    SortedMap<Integer, String> row = table().row(1).subMap(1, 3);
    assertTrue(row.headMap(1).isEmpty());
    assertTrue(row.subMap(2, 2).isEmpty());
    assertTrue(row.headMap(1).headMap(1).isEmpty());
  }

  public void testOutOfRangeEndpointsRemainRejected() {
    SortedMap<Integer, String> row = table().row(1).subMap(1, 3);
    assertThrows(IllegalArgumentException.class, () -> row.headMap(0));
    assertThrows(IllegalArgumentException.class, () -> row.headMap(4));
    assertThrows(IllegalArgumentException.class, () -> row.subMap(0, 2));
    assertThrows(IllegalArgumentException.class, () -> row.subMap(1, 4));
    assertThrows(IllegalArgumentException.class, () -> row.subMap(2, 1));
    assertThrows(IllegalArgumentException.class, () -> row.subMap(3, 3));
    assertThrows(IllegalArgumentException.class, () -> row.tailMap(0));
    assertThrows(IllegalArgumentException.class, () -> row.tailMap(3));
    assertThrows(IllegalArgumentException.class, () -> row.put(3, "outside"));
    assertFalse(row.containsKey(3));
  }

  public void testNullEndpointsRemainRejected() {
    SortedMap<Integer, String> row = table().row(1).headMap(3);
    assertThrows(NullPointerException.class, () -> row.headMap(null));
    assertThrows(NullPointerException.class, () -> row.subMap(null, 2));
    assertThrows(NullPointerException.class, () -> row.subMap(1, null));
    assertThrows(NullPointerException.class, () -> row.tailMap(null));
  }

  public void testNestedViewsWriteThrough() {
    TreeBasedTable<Integer, Integer, String> table = table();
    SortedMap<Integer, String> row = table.row(1).headMap(3).headMap(3).subMap(1, 3);
    assertEquals("two", row.put(2, "changed"));
    assertEquals("changed", table.get(1, 2));
    table.put(1, 1, "updated");
    assertEquals("updated", row.get(1));
    assertEquals("updated", row.remove(1));
    assertFalse(table.contains(1, 1));
    row.clear();
    assertEquals(ImmutableMap.of(0, "zero", 3, "three", 4, "four"), table.row(1));
  }

  public void testReverseComparator() {
    TreeBasedTable<Integer, Integer, String> table =
        TreeBasedTable.create(Ordering.natural(), Ordering.<Integer>natural().reverse());
    table.putAll(table());
    SortedMap<Integer, String> row = table.row(1).subMap(4, 1).headMap(1).subMap(3, 1);
    assertSame(table.columnComparator(), row.comparator());
    assertEquals(ImmutableMap.of(3, "three", 2, "two"), row);
    assertEquals(Integer.valueOf(3), row.firstKey());
    assertEquals(Integer.valueOf(2), row.lastKey());
    assertThrows(IllegalArgumentException.class, () -> row.headMap(0));
    assertThrows(IllegalArgumentException.class, () -> row.put(1, "outside"));
  }

  public void testComparatorEquivalentUpperBound() {
    TreeBasedTable<Integer, String, String> table =
        TreeBasedTable.create(Ordering.natural(), String.CASE_INSENSITIVE_ORDER);
    table.put(1, "a", "one");
    table.put(1, "b", "two");
    table.put(1, "c", "three");
    SortedMap<String, String> row = table.row(1).headMap("c").headMap("C").subMap("a", "c");
    assertEquals(ImmutableMap.of("a", "one", "b", "two"), row);
    assertThrows(IllegalArgumentException.class, () -> row.put("C", "outside"));
  }

  public void testJdkSortedMapControl() {
    SortedMap<Integer, String> row = new TreeMap<>(table().row(1)).subMap(1, 3);
    assertEquals(row, row.headMap(3));
    assertEquals(ImmutableMap.of(2, "two"), row.subMap(2, 3));
    assertTrue(row.headMap(1).headMap(1).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> row.tailMap(3));
    assertThrows(IllegalArgumentException.class, () -> row.subMap(3, 3));
  }

  private static TreeBasedTable<Integer, Integer, String> table() {
    TreeBasedTable<Integer, Integer, String> table = TreeBasedTable.create();
    table.put(1, 0, "zero");
    table.put(1, 1, "one");
    table.put(1, 2, "two");
    table.put(1, 3, "three");
    table.put(1, 4, "four");
    return table;
  }
}
