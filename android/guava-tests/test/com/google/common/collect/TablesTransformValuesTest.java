/*
 * Copyright (C) 2011 The Guava Authors
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
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;

/**
 * Test cases for {@link Tables#transformValues}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class TablesTransformValuesTest extends AbstractTableTest {

  private static final Function<String, Character> FIRST_CHARACTER =
      new Function<String, Character>() {
        @Override
        public Character apply(String input) {
          return input == null ? null : input.charAt(0);
        }
      };

  @Override
  protected Table<String, Integer, Character> create(Object... data) {
    Table<String, Integer, String> table = HashBasedTable.create();
    checkArgument(data.length % 3 == 0);
    for (int i = 0; i < data.length; i += 3) {
      String value = (data[i + 2] == null) ? null : (data[i + 2] + "transformed");
      table.put((String) data[i], (Integer) data[i + 1], value);
    }
    return Tables.transformValues(table, FIRST_CHARACTER);
  }

  // Null support depends on the underlying table and function.
  @GwtIncompatible // NullPointerTester
  @Override
  public void testNullPointerInstance() {}

  // put() and putAll() aren't supported.
  @Override
  public void testPut() {
    try {
      table.put("foo", 1, 'a');
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    assertSize(0);
  }

  @Override
  public void testPutAllTable() {
    table = create("foo", 1, 'a', "bar", 1, 'b', "foo", 3, 'c');
    Table<String, Integer, Character> other = HashBasedTable.create();
    other.put("foo", 1, 'd');
    other.put("bar", 2, 'e');
    other.put("cat", 2, 'f');
    try {
      table.putAll(other);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    assertEquals((Character) 'a', table.get("foo", 1));
    assertEquals((Character) 'b', table.get("bar", 1));
    assertEquals((Character) 'c', table.get("foo", 3));
    assertSize(3);
  }

  @Override
  public void testPutNull() {}

  @Override
  public void testPutNullReplace() {}

  @Override
  public void testRowClearAndPut() {}
}
