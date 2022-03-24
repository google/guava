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
import com.google.common.collect.TableCollectionTest.RowTests;
import java.util.Map;

@GwtCompatible
public class TablesTransformValuesRowTest extends RowTests {
  public TablesTransformValuesRowTest() {
    super(false, false, true, true, true);
  }

  @Override
  Table<Character, String, Integer> makeTable() {
    Table<Character, String, Integer> table = HashBasedTable.create();
    return Tables.transformValues(table, TableCollectionTest.DIVIDE_BY_2);
  }

  @Override
  protected Map<String, Integer> makePopulatedMap() {
    Table<Character, String, Integer> table = HashBasedTable.create();
    table.put('a', "one", 2);
    table.put('a', "two", 4);
    table.put('a', "three", 6);
    table.put('b', "four", 8);
    return Tables.transformValues(table, TableCollectionTest.DIVIDE_BY_2).row('a');
  }
}
