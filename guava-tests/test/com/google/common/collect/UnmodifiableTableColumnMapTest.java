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
import com.google.common.collect.TableCollectionTest.ColumnMapTests;
import java.util.Map;

@GwtCompatible
public class UnmodifiableTableColumnMapTest extends ColumnMapTests {
  public UnmodifiableTableColumnMapTest() {
    super(false, false, false, false);
  }

  @Override
  Table<Integer, String, Character> makeTable() {
    Table<Integer, String, Character> original = HashBasedTable.create();
    return Tables.unmodifiableTable(original);
  }

  @Override
  protected Map<String, Map<Integer, Character>> makePopulatedMap() {
    Table<Integer, String, Character> table = HashBasedTable.create();
    table.put(1, "foo", 'a');
    table.put(1, "bar", 'b');
    table.put(3, "foo", 'c');
    return Tables.unmodifiableTable(table).columnMap();
  }
}
