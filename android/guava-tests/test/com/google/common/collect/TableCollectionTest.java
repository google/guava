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

import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.collect.testing.MapInterfaceTest;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Collection tests for {@link Table} implementations.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible
@NullMarked
public class TableCollectionTest {
  private TableCollectionTest() {}

  private abstract static class MapTests extends MapInterfaceTest<String, Integer> {
    MapTests(
        boolean allowsNullValues,
        boolean supportsPut,
        boolean supportsRemove,
        boolean supportsClear,
        boolean supportsIteratorRemove) {
      super(
          false,
          allowsNullValues,
          supportsPut,
          supportsRemove,
          supportsClear,
          supportsIteratorRemove);
    }

    @Override
    protected String getKeyNotInPopulatedMap() {
      return "four";
    }

    @Override
    protected Integer getValueNotInPopulatedMap() {
      return 4;
    }
  }

  abstract static class RowTests extends MapTests {
    RowTests(
        boolean allowsNullValues,
        boolean supportsPut,
        boolean supportsRemove,
        boolean supportsClear,
        boolean supportsIteratorRemove) {
      super(allowsNullValues, supportsPut, supportsRemove, supportsClear, supportsIteratorRemove);
    }

    abstract Table<Character, String, Integer> makeTable();

    @Override
    protected Map<String, Integer> makeEmptyMap() {
      return makeTable().row('a');
    }

    @Override
    protected Map<String, Integer> makePopulatedMap() {
      Table<Character, String, Integer> table = makeTable();
      table.put('a', "one", 1);
      table.put('a', "two", 2);
      table.put('a', "three", 3);
      table.put('b', "four", 4);
      return table.row('a');
    }
  }

  static final Function<@Nullable Integer, @Nullable Integer> DIVIDE_BY_2 =
      new Function<@Nullable Integer, @Nullable Integer>() {
        @Override
        public @Nullable Integer apply(@Nullable Integer input) {
          return (input == null) ? null : input / 2;
        }
      };

  abstract static class ColumnTests extends MapTests {
    ColumnTests(
        boolean allowsNullValues,
        boolean supportsPut,
        boolean supportsRemove,
        boolean supportsClear,
        boolean supportsIteratorRemove) {
      super(allowsNullValues, supportsPut, supportsRemove, supportsClear, supportsIteratorRemove);
    }

    abstract Table<String, Character, Integer> makeTable();

    @Override
    protected Map<String, Integer> makeEmptyMap() {
      return makeTable().column('a');
    }

    @Override
    protected Map<String, Integer> makePopulatedMap() {
      Table<String, Character, Integer> table = makeTable();
      table.put("one", 'a', 1);
      table.put("two", 'a', 2);
      table.put("three", 'a', 3);
      table.put("four", 'b', 4);
      return table.column('a');
    }
  }

  private abstract static class MapMapTests
      extends MapInterfaceTest<String, Map<Integer, Character>> {

    MapMapTests(
        boolean allowsNullValues,
        boolean supportsRemove,
        boolean supportsClear,
        boolean supportsIteratorRemove) {
      super(false, allowsNullValues, false, supportsRemove, supportsClear, supportsIteratorRemove);
    }

    @Override
    protected String getKeyNotInPopulatedMap() {
      return "cat";
    }

    @Override
    protected Map<Integer, Character> getValueNotInPopulatedMap() {
      return ImmutableMap.of();
    }

    /**
     * The version of this test supplied by {@link MapInterfaceTest} fails for this particular map
     * implementation, because {@code map.get()} returns a view collection that changes in the
     * course of a call to {@code remove()}. Thus, the expectation doesn't hold that {@code
     * map.remove(x)} returns the same value which {@code map.get(x)} did immediately beforehand.
     */
    @Override
    public void testRemove() {
      Map<String, Map<Integer, Character>> map;
      try {
        map = makePopulatedMap();
      } catch (UnsupportedOperationException e) {
        return;
      }
      String keyToRemove = map.keySet().iterator().next();
      if (supportsRemove) {
        int initialSize = map.size();
        // var oldValue = map.get(keyToRemove);
        map.remove(keyToRemove);
        // This line doesn't hold - see the Javadoc comments above.
        // assertEquals(expectedValue, oldValue);
        assertFalse(map.containsKey(keyToRemove));
        assertEquals(initialSize - 1, map.size());
      } else {
        assertThrows(UnsupportedOperationException.class, () -> map.remove(keyToRemove));
      }
      assertInvariants(map);
    }
  }

  abstract static class RowMapTests extends MapMapTests {
    RowMapTests(
        boolean allowsNullValues,
        boolean supportsRemove,
        boolean supportsClear,
        boolean supportsIteratorRemove) {
      super(allowsNullValues, supportsRemove, supportsClear, supportsIteratorRemove);
    }

    abstract Table<String, Integer, Character> makeTable();

    @Override
    protected Map<String, Map<Integer, Character>> makePopulatedMap() {
      Table<String, Integer, Character> table = makeTable();
      populateTable(table);
      return table.rowMap();
    }

    // `protected` to work around b/320650932 / KT-67447 runtime crash
    protected final void populateTable(Table<String, Integer, Character> table) {
      table.put("foo", 1, 'a');
      table.put("bar", 1, 'b');
      table.put("foo", 3, 'c');
    }

    @Override
    protected Map<String, Map<Integer, Character>> makeEmptyMap() {
      return makeTable().rowMap();
    }
  }

  static final Function<@Nullable String, @Nullable Character> FIRST_CHARACTER =
      new Function<@Nullable String, @Nullable Character>() {
        @Override
        public @Nullable Character apply(@Nullable String input) {
          return input == null ? null : input.charAt(0);
        }
      };

  abstract static class ColumnMapTests extends MapMapTests {
    ColumnMapTests(
        boolean allowsNullValues,
        boolean supportsRemove,
        boolean supportsClear,
        boolean supportsIteratorRemove) {
      super(allowsNullValues, supportsRemove, supportsClear, supportsIteratorRemove);
    }

    abstract Table<Integer, String, Character> makeTable();

    @Override
    protected Map<String, Map<Integer, Character>> makePopulatedMap() {
      Table<Integer, String, Character> table = makeTable();
      table.put(1, "foo", 'a');
      table.put(1, "bar", 'b');
      table.put(3, "foo", 'c');
      return table.columnMap();
    }

    @Override
    protected Map<String, Map<Integer, Character>> makeEmptyMap() {
      return makeTable().columnMap();
    }
  }
}
