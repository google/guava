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

import static com.google.common.collect.Tables.unmodifiableRowSortedTable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.TableCollectionTest.ColumnTests;
import java.util.Map;

@GwtCompatible
@ElementTypesAreNonnullByDefault
public class UnmodifiableRowSortedTableColumnTest extends ColumnTests {
  public UnmodifiableRowSortedTableColumnTest() {
    super(false, false, false, false, false);
  }

  @Override
  Table<String, Character, Integer> makeTable() {
    RowSortedTable<String, Character, Integer> table = TreeBasedTable.create();
    return unmodifiableRowSortedTable(table);
  }

  @Override
  protected Map<String, Integer> makePopulatedMap() {
    RowSortedTable<String, Character, Integer> table = TreeBasedTable.create();
    table.put("one", 'a', 1);
    table.put("two", 'a', 2);
    table.put("three", 'a', 3);
    table.put("four", 'b', 4);
    return unmodifiableRowSortedTable(table).column('a');
  }
}
