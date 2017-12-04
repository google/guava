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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.EqualsTester;

/**
 * Tests {@link EmptyImmutableTable}
 *
 * @author Gregory Kick
 */
@GwtCompatible(emulated = true)
public class EmptyImmutableTableTest extends AbstractImmutableTableTest {
  private static final ImmutableTable<Character, Integer, String> INSTANCE = ImmutableTable.of();

  @Override
  Iterable<ImmutableTable<Character, Integer, String>> getTestInstances() {
    return ImmutableSet.of(INSTANCE);
  }

  public void testHashCode() {
    assertEquals(0, INSTANCE.hashCode());
  }

  public void testEqualsObject() {
    Table<Character, Integer, String> nonEmptyTable = HashBasedTable.create();
    nonEmptyTable.put('A', 1, "blah");

    new EqualsTester()
        .addEqualityGroup(INSTANCE, HashBasedTable.create(), TreeBasedTable.create())
        .addEqualityGroup(nonEmptyTable)
        .testEquals();
  }

  @GwtIncompatible // ArrayTable
  public void testEqualsObjectNullValues() {
    new EqualsTester()
        .addEqualityGroup(INSTANCE)
        .addEqualityGroup(ArrayTable.create(ImmutableSet.of('A'), ImmutableSet.of(1)))
        .testEquals();
  }

  public void testToString() {
    assertEquals("{}", INSTANCE.toString());
  }

  public void testSize() {
    assertEquals(0, INSTANCE.size());
  }

  public void testGet() {
    assertNull(INSTANCE.get('a', 1));
  }

  public void testIsEmpty() {
    assertTrue(INSTANCE.isEmpty());
  }

  public void testCellSet() {
    assertEquals(ImmutableSet.of(), INSTANCE.cellSet());
  }

  public void testColumn() {
    assertEquals(ImmutableMap.of(), INSTANCE.column(1));
  }

  public void testColumnKeySet() {
    assertEquals(ImmutableSet.of(), INSTANCE.columnKeySet());
  }

  public void testColumnMap() {
    assertEquals(ImmutableMap.of(), INSTANCE.columnMap());
  }

  public void testContains() {
    assertFalse(INSTANCE.contains('a', 1));
  }

  public void testContainsColumn() {
    assertFalse(INSTANCE.containsColumn(1));
  }

  public void testContainsRow() {
    assertFalse(INSTANCE.containsRow('a'));
  }

  public void testContainsValue() {
    assertFalse(INSTANCE.containsValue("blah"));
  }

  public void testRow() {
    assertEquals(ImmutableMap.of(), INSTANCE.row('a'));
  }

  public void testRowKeySet() {
    assertEquals(ImmutableSet.of(), INSTANCE.rowKeySet());
  }

  public void testRowMap() {
    assertEquals(ImmutableMap.of(), INSTANCE.rowMap());
  }

  public void testValues() {
    assertTrue(INSTANCE.values().isEmpty());
  }
}
