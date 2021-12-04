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
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Table.Cell;
import com.google.common.testing.CollectorTester;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import java.util.stream.Collector;
import junit.framework.TestCase;

/**
 * Tests for {@link Tables}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class TablesTest extends TestCase {

  // The bulk of the toTable tests can be found in TableCollectorsTest.
  // This gives minimal coverage to the forwarding functions
  public void testToTableSanityTest() {
    Collector<Cell<String, String, Integer>, ?, Table<String, String, Integer>> collector =
        Tables.toTable(Cell::getRowKey, Cell::getColumnKey, Cell::getValue, HashBasedTable::create);
    HashBasedTable<String, String, Integer> expected = HashBasedTable.create();
    expected.put("one", "uno", 1);
    CollectorTester.of(collector)
        .expectCollects(HashBasedTable.create())
        .expectCollects(expected, Tables.immutableCell("one", "uno", 1));
  }

  public void testToTableMergingSanityTest() {
    Collector<Cell<String, String, Integer>, ?, Table<String, String, Integer>> collector =
        Tables.toTable(
            Cell::getRowKey,
            Cell::getColumnKey,
            Cell::getValue,
            Integer::sum,
            HashBasedTable::create);
    HashBasedTable<String, String, Integer> expected = HashBasedTable.create();
    expected.put("one", "uno", 3);
    CollectorTester.of(collector)
        .expectCollects(HashBasedTable.create())
        .expectCollects(
            expected, Tables.immutableCell("one", "uno", 1), Tables.immutableCell("one", "uno", 2));
  }

  @GwtIncompatible // SerializableTester
  public void testImmutableEntrySerialization() {
    Cell<String, Integer, Character> entry = Tables.immutableCell("foo", 1, 'a');
    SerializableTester.reserializeAndAssert(entry);
  }

  public void testImmutableEntryToString() {
    Cell<String, Integer, Character> entry = Tables.immutableCell("foo", 1, 'a');
    assertEquals("(foo,1)=a", entry.toString());

    Cell<String, Integer, Character> nullEntry = Tables.immutableCell(null, null, null);
    assertEquals("(null,null)=null", nullEntry.toString());
  }

  public void testEntryEquals() {
    Cell<String, Integer, Character> entry = Tables.immutableCell("foo", 1, 'a');

    new EqualsTester()
        .addEqualityGroup(entry, Tables.immutableCell("foo", 1, 'a'))
        .addEqualityGroup(Tables.immutableCell("bar", 1, 'a'))
        .addEqualityGroup(Tables.immutableCell("foo", 2, 'a'))
        .addEqualityGroup(Tables.immutableCell("foo", 1, 'b'))
        .addEqualityGroup(Tables.immutableCell(null, null, null))
        .testEquals();
  }

  public void testEntryEqualsNull() {
    Cell<String, Integer, Character> entry = Tables.immutableCell(null, null, null);

    new EqualsTester()
        .addEqualityGroup(entry, Tables.immutableCell(null, null, null))
        .addEqualityGroup(Tables.immutableCell("bar", null, null))
        .addEqualityGroup(Tables.immutableCell(null, 2, null))
        .addEqualityGroup(Tables.immutableCell(null, null, 'b'))
        .addEqualityGroup(Tables.immutableCell("foo", 1, 'a'))
        .testEquals();
  }
}
