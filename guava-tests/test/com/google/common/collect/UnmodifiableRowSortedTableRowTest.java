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
import com.google.common.collect.TableCollectionTest.RowTests;
import java.util.Map;

@GwtCompatible
@ElementTypesAreNonnullByDefault
public class UnmodifiableRowSortedTableRowTest extends RowTests {
  public UnmodifiableRowSortedTableRowTest() {
    super(false, false, false, false, false);
  }

  @Override
  Table<Character, String, Integer> makeTable() {
    RowSortedTable<Character, String, Integer> table = TreeBasedTable.create();
    return unmodifiableRowSortedTable(table);
  }

  @Override
  protected Map<String, Integer> makePopulatedMap() {
    RowSortedTable<Character, String, Integer> table = TreeBasedTable.create();
    table.put('a', "one", 1);
    table.put('a', "two", 2);
    table.put('a', "three", 3);
    table.put('b', "four", 4);
    return unmodifiableRowSortedTable(table).row('a');
  }
}
