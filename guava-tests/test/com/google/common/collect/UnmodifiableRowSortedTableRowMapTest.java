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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.TableCollectionTest.RowMapTests;
import java.util.Map;
import java.util.SortedMap;

@GwtCompatible
public class UnmodifiableRowSortedTableRowMapTest extends RowMapTests {
  public UnmodifiableRowSortedTableRowMapTest() {
    super(false, false, false, false);
  }

  @Override
  RowSortedTable<String, Integer, Character> makeTable() {
    RowSortedTable<String, Integer, Character> original = TreeBasedTable.create();
    return Tables.unmodifiableRowSortedTable(original);
  }

  @Override
  protected SortedMap<String, Map<Integer, Character>> makePopulatedMap() {
    RowSortedTable<String, Integer, Character> table = TreeBasedTable.create();
    table.put("foo", 1, 'a');
    table.put("bar", 1, 'b');
    table.put("foo", 3, 'c');
    return Tables.unmodifiableRowSortedTable(table).rowMap();
  }
}
