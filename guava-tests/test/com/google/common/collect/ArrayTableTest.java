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

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.collect.Table.Cell;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.util.Arrays;
import java.util.Map;

/**
 * Test cases for {@link ArrayTable}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class ArrayTableTest extends AbstractTableTest {

  @Override
  protected ArrayTable<String, Integer, Character> create(Object... data) {
    // TODO: Specify different numbers of rows and columns, to detect problems
    // that arise when the wrong size is used.
    ArrayTable<String, Integer, Character> table =
        ArrayTable.create(asList("foo", "bar", "cat"), asList(1, 2, 3));
    populate(table, data);
    return table;
  }

  @Override
  protected void assertSize(int expectedSize) {
    assertEquals(9, table.size());
  }

  @Override
  protected boolean supportsRemove() {
    return false;
  }

  @Override
  protected boolean supportsNullValues() {
    return true;
  }

  // Overriding tests of behavior that differs for ArrayTable.

  @Override
  public void testContains() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertTrue(table.contains("foo", 1));
    assertTrue(table.contains("bar", 1));
    assertTrue(table.contains("foo", 3));
    assertTrue(table.contains("foo", 2));
    assertTrue(table.contains("bar", 3));
    assertTrue(table.contains("cat", 1));
    assertFalse(table.contains("foo", -1));
    assertFalse(table.contains("bad", 1));
    assertFalse(table.contains("bad", -1));
    assertFalse(table.contains("foo", null));
    assertFalse(table.contains(null, 1));
    assertFalse(table.contains(null, null));
  }

  @Override
  public void testContainsRow() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertTrue(table.containsRow("foo"));
    assertTrue(table.containsRow("bar"));
    assertTrue(table.containsRow("cat"));
    assertFalse(table.containsRow("bad"));
    assertFalse(table.containsRow(null));
  }

  @Override
  public void testContainsColumn() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertTrue(table.containsColumn(1));
    assertTrue(table.containsColumn(3));
    assertTrue(table.containsColumn(2));
    assertFalse(table.containsColumn(-1));
    assertFalse(table.containsColumn(null));
  }

  @Override
  public void testContainsValue() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertTrue(table.containsValue('a'));
    assertTrue(table.containsValue('b'));
    assertTrue(table.containsValue('c'));
    assertFalse(table.containsValue('x'));
    assertTrue(table.containsValue(null));
  }

  @Override
  public void testIsEmpty() {
    assertFalse(table.isEmpty());
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertFalse(table.isEmpty());
  }

  @Override
  public void testEquals() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Table<String, Integer, Character> hashCopy = HashBasedTable.create();
    hashCopy.put("foo", 1, 'a');
    hashCopy.put("bar", 1, 'b');
    hashCopy.put("foo", 3, 'c');
    Table<String, Integer, Character> reordered =
        create("foo", 3, 'c', "foo", 1, 'a', "bar", 1, 'b');
    Table<String, Integer, Character> smaller = create("foo", 1, 'a', "bar", 1, 'b');
    Table<String, Integer, Character> swapOuter =
        create("bar", 1, 'a', "foo", 1, 'b', "bar", 3, 'c');
    Table<String, Integer, Character> swapValues =
        create("foo", 1, 'c', "bar", 1, 'b', "foo", 3, 'a');

    new EqualsTester()
        .addEqualityGroup(table, reordered)
        .addEqualityGroup(hashCopy)
        .addEqualityGroup(smaller)
        .addEqualityGroup(swapOuter)
        .addEqualityGroup(swapValues)
        .testEquals();
  }

  @Override
  public void testHashCode() {
    table = ArrayTable.create(asList("foo", "bar"), asList(1, 3));
    table.put("foo", 1, 'a');
    table.put("bar", 1, 'b');
    table.put("foo", 3, 'c');
    int expected =
        Objects.hashCode("foo", 1, 'a')
            + Objects.hashCode("bar", 1, 'b')
            + Objects.hashCode("foo", 3, 'c')
            + Objects.hashCode("bar", 3, 0);
    assertEquals(expected, table.hashCode());
  }

  @Override
  public void testRow() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Map<Integer, Character> expected = Maps.newHashMap();
    expected.put(1, 'a');
    expected.put(3, 'c');
    expected.put(2, null);
    assertEquals(expected, table.row("foo"));
  }

  @Override
  public void testColumn() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Map<String, Character> expected = Maps.newHashMap();
    expected.put("foo", 'a');
    expected.put("bar", 'b');
    expected.put("cat", null);
    assertEquals(expected, table.column(1));
  }

  @Override
  public void testToStringSize1() {
    table = ArrayTable.create(ImmutableList.of("foo"), ImmutableList.of(1));
    table.put("foo", 1, 'a');
    assertEquals("{foo={1=a}}", table.toString());
  }

  public void testCreateDuplicateRows() {
    try {
      ArrayTable.create(asList("foo", "bar", "foo"), asList(1, 2, 3));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreateDuplicateColumns() {
    try {
      ArrayTable.create(asList("foo", "bar"), asList(1, 2, 3, 2));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreateEmptyRows() {
    try {
      ArrayTable.create(Arrays.<String>asList(), asList(1, 2, 3));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreateEmptyColumns() {
    try {
      ArrayTable.create(asList("foo", "bar"), Arrays.<Integer>asList());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreateEmptyRowsXColumns() {
    ArrayTable<String, String, Character> table =
        ArrayTable.create(Arrays.<String>asList(), Arrays.<String>asList());
    assertThat(table).isEmpty();
    assertThat(table).hasSize(0);
    assertThat(table.columnKeyList()).isEmpty();
    assertThat(table.rowKeyList()).isEmpty();
    assertThat(table.columnKeySet()).isEmpty();
    assertThat(table.rowKeySet()).isEmpty();
    try {
      table.at(0, 0);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  @GwtIncompatible // toArray
  public void testEmptyToArry() {
    ArrayTable<String, String, Character> table =
        ArrayTable.create(Arrays.<String>asList(), Arrays.<String>asList());
    assertThat(table.toArray(Character.class)).asList().isEmpty();
  }

  public void testCreateCopyArrayTable() {
    Table<String, Integer, Character> original =
        create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Table<String, Integer, Character> copy = ArrayTable.create(original);
    assertEquals(original, copy);
    original.put("foo", 1, 'd');
    assertEquals((Character) 'd', original.get("foo", 1));
    assertEquals((Character) 'a', copy.get("foo", 1));
    assertEquals(copy.rowKeySet(), original.rowKeySet());
    assertEquals(copy.columnKeySet(), original.columnKeySet());
  }

  public void testCreateCopyHashBasedTable() {
    Table<String, Integer, Character> original = HashBasedTable.create();
    original.put("foo", 1, 'a');
    original.put("bar", 1, 'b');
    original.put("foo", 3, 'c');
    Table<String, Integer, Character> copy = ArrayTable.create(original);
    assertEquals(4, copy.size());
    assertEquals((Character) 'a', copy.get("foo", 1));
    assertEquals((Character) 'b', copy.get("bar", 1));
    assertEquals((Character) 'c', copy.get("foo", 3));
    assertNull(copy.get("bar", 3));
    original.put("foo", 1, 'd');
    assertEquals((Character) 'd', original.get("foo", 1));
    assertEquals((Character) 'a', copy.get("foo", 1));
    assertEquals(copy.rowKeySet(), ImmutableSet.of("foo", "bar"));
    assertEquals(copy.columnKeySet(), ImmutableSet.of(1, 3));
  }

  public void testCreateCopyEmptyTable() {
    Table<String, Integer, Character> original = HashBasedTable.create();
    ArrayTable<String, Integer, Character> copy = ArrayTable.create(original);
    assertThat(copy).isEqualTo(original);
    assertThat(copy)
        .isEqualTo(ArrayTable.create(Arrays.<String>asList(), Arrays.<Integer>asList()));
    assertThat(copy).isEmpty();
  }

  public void testCreateCopyEmptyArrayTable() {
    Table<String, Integer, Character> original =
        ArrayTable.create(Arrays.<String>asList(), Arrays.<Integer>asList());
    ArrayTable<String, Integer, Character> copy = ArrayTable.create(original);
    assertThat(copy).isEqualTo(original);
    assertThat(copy).isEmpty();
  }

  public void testSerialization() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    SerializableTester.reserializeAndAssert(table);
  }

  @GwtIncompatible // reflection
  public void testNullPointerStatic() {
    new NullPointerTester().testAllPublicStaticMethods(ArrayTable.class);
  }

  public void testToString_ordered() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertEquals(
        "{foo={1=a, 2=null, 3=c}, "
            + "bar={1=b, 2=null, 3=null}, "
            + "cat={1=null, 2=null, 3=null}}",
        table.toString());
    assertEquals(
        "{foo={1=a, 2=null, 3=c}, "
            + "bar={1=b, 2=null, 3=null}, "
            + "cat={1=null, 2=null, 3=null}}",
        table.rowMap().toString());
  }

  public void testCellSetToString_ordered() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertEquals(
        "[(foo,1)=a, (foo,2)=null, (foo,3)=c, "
            + "(bar,1)=b, (bar,2)=null, (bar,3)=null, "
            + "(cat,1)=null, (cat,2)=null, (cat,3)=null]",
        table.cellSet().toString());
  }

  public void testRowKeySetToString_ordered() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertEquals("[foo, bar, cat]", table.rowKeySet().toString());
  }

  public void testColumnKeySetToString_ordered() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertEquals("[1, 2, 3]", table.columnKeySet().toString());
  }

  public void testValuesToString_ordered() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertEquals("[a, null, c, b, null, null, null, null, null]", table.values().toString());
  }

  public void testRowKeyList() {
    ArrayTable<String, Integer, Character> table =
        create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertThat(table.rowKeyList()).containsExactly("foo", "bar", "cat").inOrder();
  }

  public void testColumnKeyList() {
    ArrayTable<String, Integer, Character> table =
        create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertThat(table.columnKeyList()).containsExactly(1, 2, 3).inOrder();
  }

  public void testGetMissingKeys() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertNull(table.get("dog", 1));
    assertNull(table.get("foo", 4));
  }

  public void testAt() {
    ArrayTable<String, Integer, Character> table =
        create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertEquals((Character) 'b', table.at(1, 0));
    assertEquals((Character) 'c', table.at(0, 2));
    assertNull(table.at(1, 2));
    try {
      table.at(1, 3);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      table.at(1, -1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      table.at(3, 2);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      table.at(-1, 2);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testSet() {
    ArrayTable<String, Integer, Character> table =
        create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertEquals((Character) 'b', table.set(1, 0, 'd'));
    assertEquals((Character) 'd', table.get("bar", 1));
    assertNull(table.set(2, 0, 'e'));
    assertEquals((Character) 'e', table.get("cat", 1));
    assertEquals((Character) 'a', table.set(0, 0, null));
    assertNull(table.get("foo", 1));
    try {
      table.set(1, 3, 'z');
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      table.set(1, -1, 'z');
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      table.set(3, 2, 'z');
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      table.set(-1, 2, 'z');
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    assertFalse(table.containsValue('z'));
  }

  public void testEraseAll() {
    ArrayTable<String, Integer, Character> table =
        create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    table.eraseAll();
    assertEquals(9, table.size());
    assertNull(table.get("bar", 1));
    assertTrue(table.containsRow("foo"));
    assertFalse(table.containsValue('a'));
  }

  public void testPutIllegal() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    try {
      table.put("dog", 1, 'd');
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("Row dog not in [foo, bar, cat]");
    }
    try {
      table.put("foo", 4, 'd');
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("Column 4 not in [1, 2, 3]");
    }
    assertFalse(table.containsValue('d'));
  }

  public void testErase() {
    ArrayTable<String, Integer, Character> table =
        create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertEquals((Character) 'b', table.erase("bar", 1));
    assertNull(table.get("bar", 1));
    assertEquals(9, table.size());
    assertNull(table.erase("bar", 1));
    assertNull(table.erase("foo", 2));
    assertNull(table.erase("dog", 1));
    assertNull(table.erase("bar", 5));
    assertNull(table.erase(null, 1));
    assertNull(table.erase("bar", null));
  }

  @GwtIncompatible // ArrayTable.toArray(Class)
  public void testToArray() {
    ArrayTable<String, Integer, Character> table =
        create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Character[][] array = table.toArray(Character.class);
    assertThat(array).hasLength(3);
    assertThat(array[0]).asList().containsExactly('a', null, 'c').inOrder();
    assertThat(array[1]).asList().containsExactly('b', null, null).inOrder();
    assertThat(array[2]).asList().containsExactly(null, null, null).inOrder();
    table.set(0, 2, 'd');
    assertEquals((Character) 'c', array[0][2]);
    array[0][2] = 'e';
    assertEquals((Character) 'd', table.at(0, 2));
  }

  public void testCellReflectsChanges() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Cell<String, Integer, Character> cell = table.cellSet().iterator().next();
    assertEquals(Tables.immutableCell("foo", 1, 'a'), cell);
    assertEquals((Character) 'a', table.put("foo", 1, 'd'));
    assertEquals(Tables.immutableCell("foo", 1, 'd'), cell);
  }

  public void testRowMissing() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Map<Integer, Character> row = table.row("dog");
    assertTrue(row.isEmpty());
    try {
      row.put(1, 'd');
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testColumnMissing() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Map<String, Character> column = table.column(4);
    assertTrue(column.isEmpty());
    try {
      column.put("foo", 'd');
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testRowPutIllegal() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Map<Integer, Character> map = table.row("foo");
    try {
      map.put(4, 'd');
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("Column 4 not in [1, 2, 3]");
    }
  }

  public void testColumnPutIllegal() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Map<String, Character> map = table.column(3);
    try {
      map.put("dog", 'd');
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("Row dog not in [foo, bar, cat]");
    }
  }

  @GwtIncompatible // reflection
  public void testNulls() {
    new NullPointerTester().testAllPublicInstanceMethods(create());
  }

  @GwtIncompatible // serialize
  public void testSerializable() {
    SerializableTester.reserializeAndAssert(create());
  }
}
