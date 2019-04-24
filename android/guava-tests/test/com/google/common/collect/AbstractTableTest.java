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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.GwtCompatible;
import java.util.Map;

/**
 * Test cases for a {@link Table} implementation supporting reads and writes.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible
public abstract class AbstractTableTest extends AbstractTableReadTest {

  protected void populate(Table<String, Integer, Character> table, Object... data) {
    checkArgument(data.length % 3 == 0);
    for (int i = 0; i < data.length; i += 3) {
      table.put((String) data[i], (Integer) data[i + 1], (Character) data[i + 2]);
    }
  }

  protected boolean supportsRemove() {
    return true;
  }

  protected boolean supportsNullValues() {
    return false;
  }

  public void testClear() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    if (supportsRemove()) {
      table.clear();
      assertEquals(0, table.size());
      assertFalse(table.containsRow("foo"));
    } else {
      try {
        table.clear();
        fail();
      } catch (UnsupportedOperationException expected) {
      }
    }
  }

  public void testPut() {
    assertNull(table.put("foo", 1, 'a'));
    assertNull(table.put("bar", 1, 'b'));
    assertNull(table.put("foo", 3, 'c'));
    assertEquals((Character) 'a', table.put("foo", 1, 'd'));
    assertEquals((Character) 'd', table.get("foo", 1));
    assertEquals((Character) 'b', table.get("bar", 1));
    assertSize(3);
    assertEquals((Character) 'd', table.put("foo", 1, 'd'));
    assertEquals((Character) 'd', table.get("foo", 1));
    assertSize(3);
  }

  // This test assumes that the implementation does not support nulls.
  public void testPutNull() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    assertSize(3);
    try {
      table.put(null, 2, 'd');
      fail();
    } catch (NullPointerException expected) {
    }
    try {
      table.put("cat", null, 'd');
      fail();
    } catch (NullPointerException expected) {
    }
    if (supportsNullValues()) {
      assertNull(table.put("cat", 2, null));
      assertTrue(table.contains("cat", 2));
    } else {
      try {
        table.put("cat", 2, null);
        fail();
      } catch (NullPointerException expected) {
      }
    }
    assertSize(3);
  }

  public void testPutNullReplace() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');

    if (supportsNullValues()) {
      assertEquals((Character) 'b', table.put("bar", 1, null));
      assertNull(table.get("bar", 1));
    } else {
      try {
        table.put("bar", 1, null);
        fail();
      } catch (NullPointerException expected) {
      }
    }
  }

  public void testPutAllTable() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Table<String, Integer, Character> other = HashBasedTable.create();
    other.put("foo", 1, 'd');
    other.put("bar", 2, 'e');
    other.put("cat", 2, 'f');
    table.putAll(other);
    assertEquals((Character) 'd', table.get("foo", 1));
    assertEquals((Character) 'b', table.get("bar", 1));
    assertEquals((Character) 'c', table.get("foo", 3));
    assertEquals((Character) 'e', table.get("bar", 2));
    assertEquals((Character) 'f', table.get("cat", 2));
    assertSize(5);
  }

  public void testRemove() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    if (supportsRemove()) {
      assertNull(table.remove("cat", 1));
      assertNull(table.remove("bar", 3));
      assertEquals(3, table.size());
      assertEquals((Character) 'c', table.remove("foo", 3));
      assertEquals(2, table.size());
      assertEquals((Character) 'a', table.get("foo", 1));
      assertEquals((Character) 'b', table.get("bar", 1));
      assertNull(table.get("foo", 3));
      assertNull(table.remove(null, 1));
      assertNull(table.remove("foo", null));
      assertNull(table.remove(null, null));
      assertSize(2);
    } else {
      try {
        table.remove("foo", 3);
        fail();
      } catch (UnsupportedOperationException expected) {
      }
      assertEquals((Character) 'c', table.get("foo", 3));
    }
  }

  public void testRowClearAndPut() {
    if (supportsRemove()) {
      table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
      Map<Integer, Character> row = table.row("foo");
      assertEquals(ImmutableMap.of(1, 'a', 3, 'c'), row);
      table.remove("foo", 3);
      assertEquals(ImmutableMap.of(1, 'a'), row);
      table.remove("foo", 1);
      assertEquals(ImmutableMap.of(), row);
      table.put("foo", 2, 'b');
      assertEquals(ImmutableMap.of(2, 'b'), row);
      row.clear();
      assertEquals(ImmutableMap.of(), row);
      table.put("foo", 5, 'x');
      assertEquals(ImmutableMap.of(5, 'x'), row);
    }
  }
}
