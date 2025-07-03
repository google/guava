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

import static com.google.common.collect.Tables.immutableCell;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Table.Cell;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link Tables}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
@NullMarked
public class TablesTest extends TestCase {

  @GwtIncompatible // SerializableTester
  public void testImmutableEntrySerialization() {
    Cell<String, Integer, Character> entry = immutableCell("foo", 1, 'a');
    SerializableTester.reserializeAndAssert(entry);
  }

  public void testImmutableEntryToString() {
    Cell<String, Integer, Character> entry = immutableCell("foo", 1, 'a');
    assertEquals("(foo,1)=a", entry.toString());

    Cell<@Nullable String, @Nullable Integer, @Nullable Character> nullEntry =
        immutableCell(null, null, null);
    assertEquals("(null,null)=null", nullEntry.toString());
  }

  public void testEntryEquals() {
    Cell<String, Integer, Character> entry = immutableCell("foo", 1, 'a');

    new EqualsTester()
        .addEqualityGroup(entry, immutableCell("foo", 1, 'a'))
        .addEqualityGroup(immutableCell("bar", 1, 'a'))
        .addEqualityGroup(immutableCell("foo", 2, 'a'))
        .addEqualityGroup(immutableCell("foo", 1, 'b'))
        .addEqualityGroup(
            Tables.<@Nullable Object, @Nullable Object, @Nullable Object>immutableCell(
                null, null, null))
        .testEquals();
  }

  public void testEntryEqualsNull() {
    Cell<@Nullable String, @Nullable Integer, @Nullable Character> entry =
        immutableCell(null, null, null);

    new EqualsTester()
        .addEqualityGroup(
            entry,
            Tables.<@Nullable Object, @Nullable Object, @Nullable Object>immutableCell(
                null, null, null))
        .addEqualityGroup(
            Tables.<String, @Nullable Object, @Nullable Object>immutableCell("bar", null, null))
        .addEqualityGroup(
            Tables.<@Nullable Object, Integer, @Nullable Object>immutableCell(null, 2, null))
        .addEqualityGroup(
            Tables.<@Nullable Object, @Nullable Object, Character>immutableCell(null, null, 'b'))
        .addEqualityGroup(immutableCell("foo", 1, 'a'))
        .testEquals();
  }
}
