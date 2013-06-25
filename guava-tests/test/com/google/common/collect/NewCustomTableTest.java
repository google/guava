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

import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Supplier;

import java.util.Map;
import java.util.TreeMap;

/**
 * Test cases for {@link Tables#newCustomTable}.
 *
 * @author Jared Levy
 */
@GwtCompatible
public class NewCustomTableTest extends AbstractTableTest {

  @Override protected Table<String, Integer, Character> create(
      Object... data) {
    Supplier<TreeMap<Integer, Character>> factory
        = new Supplier<TreeMap<Integer, Character>>() {
          @Override
          public TreeMap<Integer, Character> get() {
            return Maps.newTreeMap();
          }
        };
    Map<String, Map<Integer, Character>> backingMap
        = Maps.newLinkedHashMap();
    Table<String, Integer, Character> table
        = Tables.newCustomTable(backingMap, factory);
    populate(table, data);
    return table;
  }

  public void testRowKeySetOrdering() {
    table = create("foo", 3, 'a', "bar", 1, 'b', "foo", 2, 'c');
    ASSERT.that(table.rowKeySet()).has().exactly("foo", "bar").inOrder();
  }

  public void testRowOrdering() {
    table = create("foo", 3, 'a', "bar", 1, 'b', "foo", 2, 'c');
    ASSERT.that(table.row("foo").keySet()).has().exactly(2, 3).inOrder();
  }
}
