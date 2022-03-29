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
import com.google.common.collect.testing.SortedMapInterfaceTest;
import java.util.SortedMap;

@GwtCompatible
public class TreeBasedTableRowMapInterfaceTest extends SortedMapInterfaceTest<String, String> {
  public TreeBasedTableRowMapInterfaceTest() {
    super(false, false, true, true, true);
  }

  @Override
  protected SortedMap<String, String> makeEmptyMap() {
    TreeBasedTable<String, String, String> table = TreeBasedTable.create();
    table.put("a", "b", "c");
    table.put("c", "b", "a");
    table.put("a", "a", "d");
    return table.row("b");
  }

  @Override
  protected SortedMap<String, String> makePopulatedMap() {
    TreeBasedTable<String, String, String> table = TreeBasedTable.create();
    table.put("a", "b", "c");
    table.put("c", "b", "a");
    table.put("b", "b", "x");
    table.put("b", "c", "y");
    table.put("b", "x", "n");
    table.put("a", "a", "d");
    return table.row("b");
  }

  @Override
  protected String getKeyNotInPopulatedMap() {
    return "q";
  }

  @Override
  protected String getValueNotInPopulatedMap() {
    return "p";
  }

  public void testClearSubMapOfRowMap() {
    TreeBasedTable<String, String, String> table = TreeBasedTable.create();
    table.put("a", "b", "c");
    table.put("c", "b", "a");
    table.put("b", "b", "x");
    table.put("b", "c", "y");
    table.put("b", "x", "n");
    table.put("a", "a", "d");
    table.row("b").subMap("c", "x").clear();
    assertEquals(table.row("b"), ImmutableMap.of("b", "x", "x", "n"));
    table.row("b").subMap("b", "y").clear();
    assertEquals(table.row("b"), ImmutableMap.of());
    assertFalse(table.backingMap.containsKey("b"));
  }
}
