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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import junit.framework.TestCase;

/**
 * Test cases for {@link Table} read operations.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public abstract class AbstractTableReadTest extends TestCase {
  protected Table<String, Integer, Character> table;

  /**
   * Creates a table with the specified data.
   *
   * @param data the table data, repeating the sequence row key, column key, value once per mapping
   * @throws IllegalArgumentException if the size of {@code data} isn't a multiple of 3
   * @throws ClassCastException if a data element has the wrong type
   */
  protected abstract Table<String, Integer, Character> create(Object... data);

  protected void assertSize(int expectedSize) {
    assertEquals(expectedSize, table.size());
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    table = create();
  }

  public void testContains() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertTrue(table.contains("foo", 1));
    assertTrue(table.contains("bar", 1));
    assertTrue(table.contains("foo", 3));
    assertFalse(table.contains("foo", 2));
    assertFalse(table.contains("bar", 3));
    assertFalse(table.contains("cat", 1));
    assertFalse(table.contains("foo", null));
    assertFalse(table.contains(null, 1));
    assertFalse(table.contains(null, null));
  }

  public void testContainsRow() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertTrue(table.containsRow("foo"));
    assertTrue(table.containsRow("bar"));
    assertFalse(table.containsRow("cat"));
    assertFalse(table.containsRow(null));
  }

  public void testContainsColumn() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertTrue(table.containsColumn(1));
    assertTrue(table.containsColumn(3));
    assertFalse(table.containsColumn(2));
    assertFalse(table.containsColumn(null));
  }

  public void testContainsValue() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertTrue(table.containsValue('a'));
    assertTrue(table.containsValue('b'));
    assertTrue(table.containsValue('c'));
    assertFalse(table.containsValue('x'));
    assertFalse(table.containsValue(null));
  }

  public void testGet() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertEquals((Character) 'a', table.get("foo", 1));
    assertEquals((Character) 'b', table.get("bar", 1));
    assertEquals((Character) 'c', table.get("foo", 3));
    assertNull(table.get("foo", 2));
    assertNull(table.get("bar", 3));
    assertNull(table.get("cat", 1));
    assertNull(table.get("foo", null));
    assertNull(table.get(null, 1));
    assertNull(table.get(null, null));
  }

  public void testIsEmpty() {
    assertTrue(table.isEmpty());
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertFalse(table.isEmpty());
  }

  public void testSize() {
    assertSize(0);
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertSize(3);
  }

  public void testEquals() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Table<String, Integer, Character> hashCopy = HashBasedTable.create(table);
    Table<String, Integer, Character> reordered =
        create("foo", 3, 'c', "foo", 1, 'a', "bar", 1, 'b');
    Table<String, Integer, Character> smaller = create("foo", 1, 'a', "bar", 1, 'b');
    Table<String, Integer, Character> swapOuter =
        create("bar", 1, 'a', "foo", 1, 'b', "bar", 3, 'c');
    Table<String, Integer, Character> swapValues =
        create("foo", 1, 'c', "bar", 1, 'b', "foo", 3, 'a');

    new EqualsTester()
        .addEqualityGroup(table, hashCopy, reordered)
        .addEqualityGroup(smaller)
        .addEqualityGroup(swapOuter)
        .addEqualityGroup(swapValues)
        .testEquals();
  }

  public void testHashCode() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    int expected =
        Objects.hashCode("foo", 1, 'a')
            + Objects.hashCode("bar", 1, 'b')
            + Objects.hashCode("foo", 3, 'c');
    assertEquals(expected, table.hashCode());
  }

  public void testToStringSize1() {
    table = create("foo", 1, 'a');
    assertEquals("{foo={1=a}}", table.toString());
  }

  public void testRow() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertEquals(ImmutableMap.of(1, 'a', 3, 'c'), table.row("foo"));
  }

  // This test assumes that the implementation does not support null keys.
  public void testRowNull() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    try {
      table.row(null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testColumn() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertEquals(ImmutableMap.of("foo", 'a', "bar", 'b'), table.column(1));
  }

  // This test assumes that the implementation does not support null keys.
  public void testColumnNull() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    try {
      table.column(null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testColumnSetPartialOverlap() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 2, 'c', "bar", 3, 'd');
    assertThat(table.columnKeySet()).containsExactly(1, 2, 3);
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointerInstance() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 2, 'c', "bar", 3, 'd');
    new NullPointerTester().testAllPublicInstanceMethods(table);
  }
}
