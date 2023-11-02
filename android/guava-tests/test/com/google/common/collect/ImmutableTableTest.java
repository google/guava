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
import com.google.common.testing.SerializableTester;

/**
 * Tests common methods in {@link ImmutableTable}
 *
 * @author Gregory Kick
 */
@GwtCompatible(emulated = true)
public class ImmutableTableTest extends AbstractTableReadTest {
  @Override
  protected Table<String, Integer, Character> create(Object... data) {
    ImmutableTable.Builder<String, Integer, Character> builder = ImmutableTable.builder();
    for (int i = 0; i < data.length; i = i + 3) {
      builder.put((String) data[i], (Integer) data[i + 1], (Character) data[i + 2]);
    }
    return builder.build();
  }

  // TODO(b/172823566): Use mainline testToImmutableMap once CollectorTester is usable to java7.
  public void testToImmutableTable_java7_combine() {
    ImmutableTable.Builder<String, String, Integer> zis =
        ImmutableTable.<String, String, Integer>builder().put("one", "uno", 1).put("two", "dos", 2);
    ImmutableTable.Builder<String, String, Integer> zat =
        ImmutableTable.<String, String, Integer>builder()
            .put("one", "eins", 1)
            .put("two", "twei", 2);
    ImmutableTable<String, String, Integer> table = zis.combine(zat).build();
    ImmutableTable<String, String, Integer> expected =
        ImmutableTable.<String, String, Integer>builder()
            .put("one", "uno", 1)
            .put("two", "dos", 2)
            .put("one", "eins", 1)
            .put("two", "twei", 2)
            .build();
    assertThat(table).isEqualTo(expected);
  }

  public void testBuilder() {
    ImmutableTable.Builder<Character, Integer, String> builder = new ImmutableTable.Builder<>();
    assertEquals(ImmutableTable.of(), builder.build());
    assertEquals(ImmutableTable.of('a', 1, "foo"), builder.put('a', 1, "foo").build());
    Table<Character, Integer, String> expectedTable = HashBasedTable.create();
    expectedTable.put('a', 1, "foo");
    expectedTable.put('b', 1, "bar");
    expectedTable.put('a', 2, "baz");
    Table<Character, Integer, String> otherTable = HashBasedTable.create();
    otherTable.put('b', 1, "bar");
    otherTable.put('a', 2, "baz");
    assertEquals(expectedTable, builder.putAll(otherTable).build());
  }

  public void testBuilder_withImmutableCell() {
    ImmutableTable.Builder<Character, Integer, String> builder = new ImmutableTable.Builder<>();
    assertEquals(
        ImmutableTable.of('a', 1, "foo"), builder.put(Tables.immutableCell('a', 1, "foo")).build());
  }

  public void testBuilder_withImmutableCellAndNullContents() {
    ImmutableTable.Builder<Character, Integer, String> builder = new ImmutableTable.Builder<>();
    try {
      builder.put(Tables.immutableCell((Character) null, 1, "foo"));
      fail();
    } catch (NullPointerException e) {
      // success
    }
    try {
      builder.put(Tables.immutableCell('a', (Integer) null, "foo"));
      fail();
    } catch (NullPointerException e) {
      // success
    }
    try {
      builder.put(Tables.immutableCell('a', 1, (String) null));
      fail();
    } catch (NullPointerException e) {
      // success
    }
  }

  private static class StringHolder {
    String string;
  }

  public void testBuilder_withMutableCell() {
    ImmutableTable.Builder<Character, Integer, String> builder = new ImmutableTable.Builder<>();

    final StringHolder holder = new StringHolder();
    holder.string = "foo";
    Table.Cell<Character, Integer, String> mutableCell =
        new Tables.AbstractCell<Character, Integer, String>() {
          @Override
          public Character getRowKey() {
            return 'K';
          }

          @Override
          public Integer getColumnKey() {
            return 42;
          }

          @Override
          public String getValue() {
            return holder.string;
          }
        };

    // Add the mutable cell to the builder
    builder.put(mutableCell);

    // Mutate the value
    holder.string = "bar";

    // Make sure it uses the original value.
    assertEquals(ImmutableTable.of('K', 42, "foo"), builder.build());
  }

  public void testBuilder_noDuplicates() {
    ImmutableTable.Builder<Character, Integer, String> builder =
        new ImmutableTable.Builder<Character, Integer, String>()
            .put('a', 1, "foo")
            .put('a', 1, "bar");
    try {
      builder.build();
      fail();
    } catch (IllegalArgumentException e) {
      // success
    }
  }

  public void testBuilder_noNulls() {
    ImmutableTable.Builder<Character, Integer, String> builder = new ImmutableTable.Builder<>();
    try {
      builder.put(null, 1, "foo");
      fail();
    } catch (NullPointerException e) {
      // success
    }
    try {
      builder.put('a', null, "foo");
      fail();
    } catch (NullPointerException e) {
      // success
    }
    try {
      builder.put('a', 1, null);
      fail();
    } catch (NullPointerException e) {
      // success
    }
  }

  private static <R, C, V> void validateTableCopies(Table<R, C, V> original) {
    Table<R, C, V> copy = ImmutableTable.copyOf(original);
    assertEquals(original, copy);
    validateViewOrdering(original, copy);

    Table<R, C, V> built = ImmutableTable.<R, C, V>builder().putAll(original).build();
    assertEquals(original, built);
    validateViewOrdering(original, built);
  }

  private static <R, C, V> void validateViewOrdering(Table<R, C, V> original, Table<R, C, V> copy) {
    assertThat(copy.cellSet()).containsExactlyElementsIn(original.cellSet()).inOrder();
    assertThat(copy.rowKeySet()).containsExactlyElementsIn(original.rowKeySet()).inOrder();
    assertThat(copy.values()).containsExactlyElementsIn(original.values()).inOrder();
  }

  public void testCopyOf() {
    Table<Character, Integer, String> table = TreeBasedTable.create();
    validateTableCopies(table);
    table.put('b', 2, "foo");
    validateTableCopies(table);
    table.put('b', 1, "bar");
    table.put('a', 2, "baz");
    validateTableCopies(table);
    // Even though rowKeySet, columnKeySet, and cellSet have the same
    // iteration ordering, row has an inconsistent ordering.
    assertThat(table.row('b').keySet()).containsExactly(1, 2).inOrder();
    assertThat(ImmutableTable.copyOf(table).row('b').keySet()).containsExactly(2, 1).inOrder();
  }

  public void testCopyOfSparse() {
    Table<Character, Integer, String> table = TreeBasedTable.create();
    table.put('x', 2, "foo");
    table.put('r', 1, "bar");
    table.put('c', 3, "baz");
    table.put('b', 7, "cat");
    table.put('e', 5, "dog");
    table.put('c', 0, "axe");
    table.put('e', 3, "tub");
    table.put('r', 4, "foo");
    table.put('x', 5, "bar");
    validateTableCopies(table);
  }

  public void testCopyOfDense() {
    Table<Character, Integer, String> table = TreeBasedTable.create();
    table.put('c', 3, "foo");
    table.put('c', 2, "bar");
    table.put('c', 1, "baz");
    table.put('b', 3, "cat");
    table.put('b', 1, "dog");
    table.put('a', 3, "foo");
    table.put('a', 2, "bar");
    table.put('a', 1, "baz");
    validateTableCopies(table);
  }

  public void testBuilder_orderRowsAndColumnsBy_putAll() {
    Table<Character, Integer, String> table = HashBasedTable.create();
    table.put('b', 2, "foo");
    table.put('b', 1, "bar");
    table.put('a', 2, "baz");
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    Table<Character, Integer, String> copy =
        builder
            .orderRowsBy(Ordering.natural())
            .orderColumnsBy(Ordering.natural())
            .putAll(table)
            .build();
    assertThat(copy.rowKeySet()).containsExactly('a', 'b').inOrder();
    assertThat(copy.columnKeySet()).containsExactly(1, 2).inOrder();
    assertThat(copy.values()).containsExactly("baz", "bar", "foo").inOrder();
    assertThat(copy.row('b').keySet()).containsExactly(1, 2).inOrder();
  }

  public void testBuilder_orderRowsAndColumnsBy_sparse() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.orderRowsBy(Ordering.natural());
    builder.orderColumnsBy(Ordering.natural());
    builder.put('x', 2, "foo");
    builder.put('r', 1, "bar");
    builder.put('c', 3, "baz");
    builder.put('b', 7, "cat");
    builder.put('e', 5, "dog");
    builder.put('c', 0, "axe");
    builder.put('e', 3, "tub");
    builder.put('r', 4, "foo");
    builder.put('x', 5, "bar");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table.rowKeySet()).containsExactly('b', 'c', 'e', 'r', 'x').inOrder();
    assertThat(table.columnKeySet()).containsExactly(0, 1, 2, 3, 4, 5, 7).inOrder();
    assertThat(table.values())
        .containsExactly("cat", "axe", "baz", "tub", "dog", "bar", "foo", "foo", "bar")
        .inOrder();
    assertThat(table.row('c').keySet()).containsExactly(0, 3).inOrder();
    assertThat(table.column(5).keySet()).containsExactly('e', 'x').inOrder();
  }

  public void testBuilder_orderRowsAndColumnsBy_dense() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.orderRowsBy(Ordering.natural());
    builder.orderColumnsBy(Ordering.natural());
    builder.put('c', 3, "foo");
    builder.put('c', 2, "bar");
    builder.put('c', 1, "baz");
    builder.put('b', 3, "cat");
    builder.put('b', 1, "dog");
    builder.put('a', 3, "foo");
    builder.put('a', 2, "bar");
    builder.put('a', 1, "baz");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table.rowKeySet()).containsExactly('a', 'b', 'c').inOrder();
    assertThat(table.columnKeySet()).containsExactly(1, 2, 3).inOrder();
    assertThat(table.values())
        .containsExactly("baz", "bar", "foo", "dog", "cat", "baz", "bar", "foo")
        .inOrder();
    assertThat(table.row('c').keySet()).containsExactly(1, 2, 3).inOrder();
    assertThat(table.column(1).keySet()).containsExactly('a', 'b', 'c').inOrder();
  }

  public void testBuilder_orderRowsBy_sparse() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.orderRowsBy(Ordering.natural());
    builder.put('x', 2, "foo");
    builder.put('r', 1, "bar");
    builder.put('c', 3, "baz");
    builder.put('b', 7, "cat");
    builder.put('e', 5, "dog");
    builder.put('c', 0, "axe");
    builder.put('e', 3, "tub");
    builder.put('r', 4, "foo");
    builder.put('x', 5, "bar");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table.rowKeySet()).containsExactly('b', 'c', 'e', 'r', 'x').inOrder();
    assertThat(table.column(5).keySet()).containsExactly('e', 'x').inOrder();
  }

  public void testBuilder_orderRowsBy_dense() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.orderRowsBy(Ordering.natural());
    builder.put('c', 3, "foo");
    builder.put('c', 2, "bar");
    builder.put('c', 1, "baz");
    builder.put('b', 3, "cat");
    builder.put('b', 1, "dog");
    builder.put('a', 3, "foo");
    builder.put('a', 2, "bar");
    builder.put('a', 1, "baz");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table.rowKeySet()).containsExactly('a', 'b', 'c').inOrder();
    assertThat(table.column(1).keySet()).containsExactly('a', 'b', 'c').inOrder();
  }

  public void testBuilder_orderColumnsBy_sparse() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.orderColumnsBy(Ordering.natural());
    builder.put('x', 2, "foo");
    builder.put('r', 1, "bar");
    builder.put('c', 3, "baz");
    builder.put('b', 7, "cat");
    builder.put('e', 5, "dog");
    builder.put('c', 0, "axe");
    builder.put('e', 3, "tub");
    builder.put('r', 4, "foo");
    builder.put('x', 5, "bar");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table.columnKeySet()).containsExactly(0, 1, 2, 3, 4, 5, 7).inOrder();
    assertThat(table.row('c').keySet()).containsExactly(0, 3).inOrder();
  }

  public void testBuilder_orderColumnsBy_dense() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.orderColumnsBy(Ordering.natural());
    builder.put('c', 3, "foo");
    builder.put('c', 2, "bar");
    builder.put('c', 1, "baz");
    builder.put('b', 3, "cat");
    builder.put('b', 1, "dog");
    builder.put('a', 3, "foo");
    builder.put('a', 2, "bar");
    builder.put('a', 1, "baz");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table.columnKeySet()).containsExactly(1, 2, 3).inOrder();
    assertThat(table.row('c').keySet()).containsExactly(1, 2, 3).inOrder();
  }

  public void testSerialization_empty() {
    validateReserialization(ImmutableTable.of());
  }

  public void testSerialization_singleElement() {
    validateReserialization(ImmutableTable.of('a', 2, "foo"));
  }

  public void testDenseSerialization_manualOrder() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.put('b', 2, "foo");
    builder.put('b', 1, "bar");
    builder.put('a', 2, "baz");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table).isInstanceOf(DenseImmutableTable.class);
    validateReserialization(table);
  }

  public void testDenseSerialization_rowOrder() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.orderRowsBy(Ordering.<Character>natural());
    builder.put('b', 2, "foo");
    builder.put('b', 1, "bar");
    builder.put('a', 2, "baz");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table).isInstanceOf(DenseImmutableTable.class);
    validateReserialization(table);
  }

  public void testDenseSerialization_columnOrder() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.orderColumnsBy(Ordering.<Integer>natural());
    builder.put('b', 2, "foo");
    builder.put('b', 1, "bar");
    builder.put('a', 2, "baz");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table).isInstanceOf(DenseImmutableTable.class);
    validateReserialization(table);
  }

  public void testDenseSerialization_bothOrders() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.orderRowsBy(Ordering.<Character>natural());
    builder.orderColumnsBy(Ordering.<Integer>natural());
    builder.put('b', 2, "foo");
    builder.put('b', 1, "bar");
    builder.put('a', 2, "baz");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table).isInstanceOf(DenseImmutableTable.class);
    validateReserialization(table);
  }

  public void testSparseSerialization_manualOrder() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.put('b', 2, "foo");
    builder.put('b', 1, "bar");
    builder.put('a', 2, "baz");
    builder.put('c', 3, "cat");
    builder.put('d', 4, "dog");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table).isInstanceOf(SparseImmutableTable.class);
    validateReserialization(table);
  }

  public void testSparseSerialization_rowOrder() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.orderRowsBy(Ordering.<Character>natural());
    builder.put('b', 2, "foo");
    builder.put('b', 1, "bar");
    builder.put('a', 2, "baz");
    builder.put('c', 3, "cat");
    builder.put('d', 4, "dog");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table).isInstanceOf(SparseImmutableTable.class);
    validateReserialization(table);
  }

  public void testSparseSerialization_columnOrder() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.orderColumnsBy(Ordering.<Integer>natural());
    builder.put('b', 2, "foo");
    builder.put('b', 1, "bar");
    builder.put('a', 2, "baz");
    builder.put('c', 3, "cat");
    builder.put('d', 4, "dog");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table).isInstanceOf(SparseImmutableTable.class);
    validateReserialization(table);
  }

  public void testSparseSerialization_bothOrders() {
    ImmutableTable.Builder<Character, Integer, String> builder = ImmutableTable.builder();
    builder.orderRowsBy(Ordering.<Character>natural());
    builder.orderColumnsBy(Ordering.<Integer>natural());
    builder.put('b', 2, "foo");
    builder.put('b', 1, "bar");
    builder.put('a', 2, "baz");
    builder.put('c', 3, "cat");
    builder.put('d', 4, "dog");
    Table<Character, Integer, String> table = builder.build();
    assertThat(table).isInstanceOf(SparseImmutableTable.class);
    validateReserialization(table);
  }

  private static <R, C, V> void validateReserialization(Table<R, C, V> original) {
    Table<R, C, V> copy = SerializableTester.reserializeAndAssert(original);
    assertThat(copy.cellSet()).containsExactlyElementsIn(original.cellSet()).inOrder();
    assertThat(copy.rowKeySet()).containsExactlyElementsIn(original.rowKeySet()).inOrder();
    assertThat(copy.columnKeySet()).containsExactlyElementsIn(original.columnKeySet()).inOrder();
  }

  @GwtIncompatible // Mind-bogglingly slow in GWT
  @AndroidIncompatible // slow
  public void testOverflowCondition() {
    // See https://code.google.com/p/guava-libraries/issues/detail?id=1322 for details.
    ImmutableTable.Builder<Integer, Integer, String> builder = ImmutableTable.builder();
    for (int i = 1; i < 0x10000; i++) {
      builder.put(i, 0, "foo");
      builder.put(0, i, "bar");
    }
    assertTrue(builder.build() instanceof SparseImmutableTable);
  }

  @GwtIncompatible // NullPointerTester
  @Override
  public void testNullPointerInstance() {
    if (isAndroid()) {
      /*
       * NPT fails under the old versions of Android we test under because it performs reflection on
       * ImmutableTable, which declares static methods that refer to Collector, which is unavailable
       * under such versions.
       *
       * We use a runtime check here instead of @AndroidIncompatible: @AndroidIncompatible operates
       * by stripping annotated methods entirely, and if we strip this method, then JUnit would just
       * run the supermethod as usual.
       *
       * TODO: b/292578973: Use @AndroidIncompatible if we change our system to keep the methods in
       * place but to have the test runner skip them. However, note that if we choose to *both*
       * strip the methods *and* have the test runner not run them (for some unusual cases in which
       * we don't run the stripping test for technical reasons), then we'd be back to the problem
       * described above, since the supermethod is *not* annotated @AndroidIncompatible (since it
       * works fine with the other Table implementations).
       */
      return;
    }
    super.testNullPointerInstance();
  }

  private static boolean isAndroid() {
    return System.getProperty("java.runtime.name", "").contains("Android");
  }
}
