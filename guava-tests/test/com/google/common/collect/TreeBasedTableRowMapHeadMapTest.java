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

@GwtCompatible
public class TreeBasedTableRowMapHeadMapTest extends RowMapTests {
  public TreeBasedTableRowMapHeadMapTest() {
    super(false, true, true, true);
  }

  @Override
  TreeBasedTable<String, Integer, Character> makeTable() {
    TreeBasedTable<String, Integer, Character> table = TreeBasedTable.create();
    table.put("z", 1, 'a');
    return table;
  }

  @Override
  protected Map<String, Map<Integer, Character>> makePopulatedMap() {
    TreeBasedTable<String, Integer, Character> table = makeTable();
    populateTable(table);
    return table.rowMap().headMap("x");
  }

  @Override
  protected Map<String, Map<Integer, Character>> makeEmptyMap() {
    return makeTable().rowMap().headMap("x");
  }

  @Override
  protected String getKeyNotInPopulatedMap() {
    return "z";
  }
}
