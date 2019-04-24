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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.testing.EqualsTester;

/**
 * Tests {@link SingletonImmutableTable}.
 *
 * @author Gregory Kick
 */
@GwtCompatible(emulated = true)
public class SingletonImmutableTableTest extends AbstractImmutableTableTest {
  private final ImmutableTable<Character, Integer, String> testTable =
      new SingletonImmutableTable<>('a', 1, "blah");

  public void testHashCode() {
    assertEquals(Objects.hashCode('a', 1, "blah"), testTable.hashCode());
  }

  public void testCellSet() {
    assertEquals(ImmutableSet.of(Tables.immutableCell('a', 1, "blah")), testTable.cellSet());
  }

  public void testColumn() {
    assertEquals(ImmutableMap.of(), testTable.column(0));
    assertEquals(ImmutableMap.of('a', "blah"), testTable.column(1));
  }

  public void testColumnKeySet() {
    assertEquals(ImmutableSet.of(1), testTable.columnKeySet());
  }

  public void testColumnMap() {
    assertEquals(ImmutableMap.of(1, ImmutableMap.of('a', "blah")), testTable.columnMap());
  }

  public void testRow() {
    assertEquals(ImmutableMap.of(), testTable.row('A'));
    assertEquals(ImmutableMap.of(1, "blah"), testTable.row('a'));
  }

  public void testRowKeySet() {
    assertEquals(ImmutableSet.of('a'), testTable.rowKeySet());
  }

  public void testRowMap() {
    assertEquals(ImmutableMap.of('a', ImmutableMap.of(1, "blah")), testTable.rowMap());
  }

  public void testEqualsObject() {
    new EqualsTester()
        .addEqualityGroup(testTable, HashBasedTable.create(testTable))
        .addEqualityGroup(ImmutableTable.of(), HashBasedTable.create())
        .addEqualityGroup(HashBasedTable.create(ImmutableTable.of('A', 2, "")))
        .testEquals();
  }

  @GwtIncompatible // ArrayTable
  public void testEqualsObjectNullValues() {
    new EqualsTester()
        .addEqualityGroup(testTable)
        .addEqualityGroup(ArrayTable.create(ImmutableSet.of('A'), ImmutableSet.of(1)))
        .testEquals();
  }

  public void testToString() {
    assertEquals("{a={1=blah}}", testTable.toString());
  }

  public void testContains() {
    assertTrue(testTable.contains('a', 1));
    assertFalse(testTable.contains('a', 2));
    assertFalse(testTable.contains('A', 1));
    assertFalse(testTable.contains('A', 2));
  }

  public void testContainsColumn() {
    assertTrue(testTable.containsColumn(1));
    assertFalse(testTable.containsColumn(2));
  }

  public void testContainsRow() {
    assertTrue(testTable.containsRow('a'));
    assertFalse(testTable.containsRow('A'));
  }

  public void testContainsValue() {
    assertTrue(testTable.containsValue("blah"));
    assertFalse(testTable.containsValue(""));
  }

  public void testGet() {
    assertEquals("blah", testTable.get('a', 1));
    assertNull(testTable.get('a', 2));
    assertNull(testTable.get('A', 1));
    assertNull(testTable.get('A', 2));
  }

  public void testIsEmpty() {
    assertFalse(testTable.isEmpty());
  }

  public void testSize() {
    assertEquals(1, testTable.size());
  }

  public void testValues() {
    assertThat(testTable.values()).contains("blah");
  }

  @Override
  Iterable<ImmutableTable<Character, Integer, String>> getTestInstances() {
    return ImmutableSet.of(testTable);
  }
}
