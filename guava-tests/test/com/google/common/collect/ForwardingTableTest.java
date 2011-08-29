/*
 * Copyright (C) 2009 The Guava Authors
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

/**
 * Tests {@link ForwardingTable}.
 *
 * @author Gregory Kick
 */
public class ForwardingTableTest extends ForwardingTestCase {

  private Table<String, Integer, Boolean> forward;

  @Override public void setUp() throws Exception {
    super.setUp();
    /*
     * Class parameters must be raw, so we can't create a proxy with generic
     * type arguments. The created proxy only records calls and returns null, so
     * the type is irrelevant at runtime.
     */
    @SuppressWarnings("unchecked")
    final Table<String, Integer, Boolean> table =
        createProxyInstance(Table.class);
    forward = new ForwardingTable<String, Integer, Boolean>() {
      @Override protected Table<String, Integer, Boolean> delegate() {
        return table;
      }
    };
  }

  public void testHashCode() {
    forward.hashCode();
    assertEquals("[hashCode]", getCalls());
  }

  public void testCellSet() {
    forward.cellSet();
    assertEquals("[cellSet]", getCalls());
  }

  public void testClear() {
    forward.clear();
    assertEquals("[clear]", getCalls());
  }

  public void testColumn() {
    forward.column(1);
    assertEquals("[column(Object)]", getCalls());
  }

  public void testColumnKeySet() {
    forward.columnKeySet();
    assertEquals("[columnKeySet]", getCalls());
  }

  public void testColumnMap() {
    forward.columnMap();
    assertEquals("[columnMap]", getCalls());
  }

  public void testContains() {
    forward.contains("blah", 1);
    assertEquals("[contains(Object,Object)]", getCalls());
  }

  public void testContainsColumn() {
    forward.containsColumn(1);
    assertEquals("[containsColumn(Object)]", getCalls());
  }

  public void testContainsRow() {
    forward.containsRow("blah");
    assertEquals("[containsRow(Object)]", getCalls());
  }

  public void testContainsValue() {
    forward.containsValue(false);
    assertEquals("[containsValue(Object)]", getCalls());
  }

  public void testGet() {
    forward.get("blah", 1);
    assertEquals("[get(Object,Object)]", getCalls());
  }

  public void testIsEmpty() {
    forward.isEmpty();
    assertEquals("[isEmpty]", getCalls());
  }

  public void testPut() {
    forward.put("blah", 1, false);
    assertEquals("[put(Object,Object,Object)]", getCalls());
  }

  public void testPutAll() {
    forward.putAll(HashBasedTable.<String, Integer, Boolean>create());
    assertEquals("[putAll(Table)]", getCalls());
  }

  public void testRemove() {
    forward.remove("blah", 1);
    assertEquals("[remove(Object,Object)]", getCalls());
  }

  public void testRow() {
    forward.row("String");
    assertEquals("[row(Object)]", getCalls());
  }

  public void testRowKeySet() {
    forward.rowKeySet();
    assertEquals("[rowKeySet]", getCalls());
  }

  public void testRowMap() {
    forward.rowMap();
    assertEquals("[rowMap]", getCalls());
  }

  public void testSize() {
    forward.size();
    assertEquals("[size]", getCalls());
  }

  public void testValues() {
    forward.values();
    assertEquals("[values]", getCalls());
  }

  public void testEqualsObject() {
    forward.equals(null);
    assertEquals("[equals(Object)]", getCalls());
  }

}
