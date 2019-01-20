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
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;

/**
 * Test cases for {@link HashBasedTable}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class HashBasedTableTest extends AbstractTableTest {

  @Override
  protected Table<String, Integer, Character> create(Object... data) {
    Table<String, Integer, Character> table = HashBasedTable.create();
    table.put("foo", 4, 'a');
    table.put("cat", 1, 'b');
    table.clear();
    populate(table, data);
    return table;
  }

  public void testIterationOrder() {
    Table<String, String, String> table = HashBasedTable.create();
    for (int i = 0; i < 5; i++) {
      table.put("r" + i, "c" + i, "v" + i);
    }
    assertThat(table.rowKeySet()).containsExactly("r0", "r1", "r2", "r3", "r4").inOrder();
    assertThat(table.columnKeySet()).containsExactly("c0", "c1", "c2", "c3", "c4").inOrder();
    assertThat(table.values()).containsExactly("v0", "v1", "v2", "v3", "v4").inOrder();
  }

  public void testCreateWithValidSizes() {
    Table<String, Integer, Character> table1 = HashBasedTable.create(100, 20);
    table1.put("foo", 1, 'a');
    assertEquals((Character) 'a', table1.get("foo", 1));

    Table<String, Integer, Character> table2 = HashBasedTable.create(100, 0);
    table2.put("foo", 1, 'a');
    assertEquals((Character) 'a', table2.get("foo", 1));

    Table<String, Integer, Character> table3 = HashBasedTable.create(0, 20);
    table3.put("foo", 1, 'a');
    assertEquals((Character) 'a', table3.get("foo", 1));

    Table<String, Integer, Character> table4 = HashBasedTable.create(0, 0);
    table4.put("foo", 1, 'a');
    assertEquals((Character) 'a', table4.get("foo", 1));
  }

  public void testCreateWithInvalidSizes() {
    try {
      HashBasedTable.create(100, -5);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      HashBasedTable.create(-5, 20);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreateCopy() {
    Table<String, Integer, Character> original =
        create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Table<String, Integer, Character> copy = HashBasedTable.create(original);
    assertEquals(original, copy);
    assertEquals((Character) 'a', copy.get("foo", 1));
  }

  @GwtIncompatible // SerializableTester
  public void testSerialization() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    SerializableTester.reserializeAndAssert(table);
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointerStatic() {
    new NullPointerTester().testAllPublicStaticMethods(HashBasedTable.class);
  }
}
