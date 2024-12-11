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

import static com.google.common.collect.Tables.transpose;

import com.google.common.annotations.GwtCompatible;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Test cases for {@link Tables#transpose}.
 *
 * @author Jared Levy
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public class TransposedTableTest extends AbstractTableTest<Character> {

  @Override
  protected Table<String, Integer, Character> create(@Nullable Object... data) {
    Table<Integer, String, Character> original = HashBasedTable.create();
    Table<String, Integer, Character> table = transpose(original);
    table.clear();
    populate(table, data);
    return table;
  }

  public void testTransposeTransposed() {
    Table<Integer, String, Character> original = HashBasedTable.create();
    assertSame(original, transpose(transpose(original)));
  }

  public void testPutOriginalModifiesTranspose() {
    Table<Integer, String, Character> original = HashBasedTable.create();
    Table<String, Integer, Character> transpose = transpose(original);
    original.put(1, "foo", 'a');
    assertEquals((Character) 'a', transpose.get("foo", 1));
  }

  public void testPutTransposeModifiesOriginal() {
    Table<Integer, String, Character> original = HashBasedTable.create();
    Table<String, Integer, Character> transpose = transpose(original);
    transpose.put("foo", 1, 'a');
    assertEquals((Character) 'a', original.get(1, "foo"));
  }

  public void testTransposedViews() {
    Table<Integer, String, Character> original = HashBasedTable.create();
    Table<String, Integer, Character> transpose = transpose(original);
    original.put(1, "foo", 'a');
    assertSame(original.columnKeySet(), transpose.rowKeySet());
    assertSame(original.rowKeySet(), transpose.columnKeySet());
    assertSame(original.columnMap(), transpose.rowMap());
    assertSame(original.rowMap(), transpose.columnMap());
    assertSame(original.values(), transpose.values());
    assertEquals(original.row(1), transpose.column(1));
    assertEquals(original.row(2), transpose.column(2));
    assertEquals(original.column("foo"), transpose.row("foo"));
    assertEquals(original.column("bar"), transpose.row("bar"));
  }
}
