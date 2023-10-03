/*
 * Copyright (C) 2007 The Guava Authors
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
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

@GwtCompatible
abstract class AbstractFilteredMapTest extends TestCase {
  private static final Predicate<@Nullable String> NOT_LENGTH_3 =
      input -> input == null || input.length() != 3;
  private static final Predicate<@Nullable Integer> EVEN = input -> input == null || input % 2 == 0;
  static final Predicate<Entry<String, Integer>> CORRECT_LENGTH =
      input -> input.getKey().length() == input.getValue();

  abstract Map<String, Integer> createUnfiltered();

  public void testFilteredKeysIllegalPut() {
    Map<String, Integer> unfiltered = createUnfiltered();
    Map<String, Integer> filtered = Maps.filterKeys(unfiltered, NOT_LENGTH_3);
    filtered.put("a", 1);
    filtered.put("b", 2);
    assertEquals(ImmutableMap.of("a", 1, "b", 2), filtered);

    try {
      filtered.put("yyy", 3);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFilteredKeysIllegalPutAll() {
    Map<String, Integer> unfiltered = createUnfiltered();
    Map<String, Integer> filtered = Maps.filterKeys(unfiltered, NOT_LENGTH_3);
    filtered.put("a", 1);
    filtered.put("b", 2);
    assertEquals(ImmutableMap.of("a", 1, "b", 2), filtered);

    try {
      filtered.putAll(ImmutableMap.of("c", 3, "zzz", 4, "b", 5));
      fail();
    } catch (IllegalArgumentException expected) {
    }

    assertEquals(ImmutableMap.of("a", 1, "b", 2), filtered);
  }

  public void testFilteredKeysFilteredReflectsBackingChanges() {
    Map<String, Integer> unfiltered = createUnfiltered();
    Map<String, Integer> filtered = Maps.filterKeys(unfiltered, NOT_LENGTH_3);
    unfiltered.put("two", 2);
    unfiltered.put("three", 3);
    unfiltered.put("four", 4);
    assertEquals(ImmutableMap.of("two", 2, "three", 3, "four", 4), unfiltered);
    assertEquals(ImmutableMap.of("three", 3, "four", 4), filtered);

    unfiltered.remove("three");
    assertEquals(ImmutableMap.of("two", 2, "four", 4), unfiltered);
    assertEquals(ImmutableMap.of("four", 4), filtered);

    unfiltered.clear();
    assertEquals(ImmutableMap.of(), unfiltered);
    assertEquals(ImmutableMap.of(), filtered);
  }

  public void testFilteredValuesIllegalPut() {
    Map<String, Integer> unfiltered = createUnfiltered();
    Map<String, Integer> filtered = Maps.filterValues(unfiltered, EVEN);
    filtered.put("a", 2);
    unfiltered.put("b", 4);
    unfiltered.put("c", 5);
    assertEquals(ImmutableMap.of("a", 2, "b", 4), filtered);

    try {
      filtered.put("yyy", 3);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    assertEquals(ImmutableMap.of("a", 2, "b", 4), filtered);
  }

  public void testFilteredValuesIllegalPutAll() {
    Map<String, Integer> unfiltered = createUnfiltered();
    Map<String, Integer> filtered = Maps.filterValues(unfiltered, EVEN);
    filtered.put("a", 2);
    unfiltered.put("b", 4);
    unfiltered.put("c", 5);
    assertEquals(ImmutableMap.of("a", 2, "b", 4), filtered);

    try {
      filtered.putAll(ImmutableMap.of("c", 4, "zzz", 5, "b", 6));
      fail();
    } catch (IllegalArgumentException expected) {
    }
    assertEquals(ImmutableMap.of("a", 2, "b", 4), filtered);
  }

  public void testFilteredValuesIllegalSetValue() {
    Map<String, Integer> unfiltered = createUnfiltered();
    Map<String, Integer> filtered = Maps.filterValues(unfiltered, EVEN);
    filtered.put("a", 2);
    filtered.put("b", 4);
    assertEquals(ImmutableMap.of("a", 2, "b", 4), filtered);

    Entry<String, Integer> entry = filtered.entrySet().iterator().next();
    try {
      entry.setValue(5);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    assertEquals(ImmutableMap.of("a", 2, "b", 4), filtered);
  }

  public void testFilteredValuesClear() {
    Map<String, Integer> unfiltered = createUnfiltered();
    unfiltered.put("one", 1);
    unfiltered.put("two", 2);
    unfiltered.put("three", 3);
    unfiltered.put("four", 4);
    Map<String, Integer> filtered = Maps.filterValues(unfiltered, EVEN);
    assertEquals(ImmutableMap.of("one", 1, "two", 2, "three", 3, "four", 4), unfiltered);
    assertEquals(ImmutableMap.of("two", 2, "four", 4), filtered);

    filtered.clear();
    assertEquals(ImmutableMap.of("one", 1, "three", 3), unfiltered);
    assertTrue(filtered.isEmpty());
  }

  public void testFilteredEntriesIllegalPut() {
    Map<String, Integer> unfiltered = createUnfiltered();
    unfiltered.put("cat", 3);
    unfiltered.put("dog", 2);
    unfiltered.put("horse", 5);
    Map<String, Integer> filtered = Maps.filterEntries(unfiltered, CORRECT_LENGTH);
    assertEquals(ImmutableMap.of("cat", 3, "horse", 5), filtered);

    filtered.put("chicken", 7);
    assertEquals(ImmutableMap.of("cat", 3, "horse", 5, "chicken", 7), filtered);

    try {
      filtered.put("cow", 7);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    assertEquals(ImmutableMap.of("cat", 3, "horse", 5, "chicken", 7), filtered);
  }

  public void testFilteredEntriesIllegalPutAll() {
    Map<String, Integer> unfiltered = createUnfiltered();
    unfiltered.put("cat", 3);
    unfiltered.put("dog", 2);
    unfiltered.put("horse", 5);
    Map<String, Integer> filtered = Maps.filterEntries(unfiltered, CORRECT_LENGTH);
    assertEquals(ImmutableMap.of("cat", 3, "horse", 5), filtered);

    filtered.put("chicken", 7);
    assertEquals(ImmutableMap.of("cat", 3, "horse", 5, "chicken", 7), filtered);

    try {
      filtered.putAll(ImmutableMap.of("sheep", 5, "cow", 7));
      fail();
    } catch (IllegalArgumentException expected) {
    }
    assertEquals(ImmutableMap.of("cat", 3, "horse", 5, "chicken", 7), filtered);
  }

  public void testFilteredEntriesObjectPredicate() {
    Map<String, Integer> unfiltered = createUnfiltered();
    unfiltered.put("cat", 3);
    unfiltered.put("dog", 2);
    unfiltered.put("horse", 5);
    Predicate<Object> predicate = Predicates.alwaysFalse();
    Map<String, Integer> filtered = Maps.filterEntries(unfiltered, predicate);
    assertTrue(filtered.isEmpty());
  }

  public void testFilteredEntriesWildCardEntryPredicate() {
    Map<String, Integer> unfiltered = createUnfiltered();
    unfiltered.put("cat", 3);
    unfiltered.put("dog", 2);
    unfiltered.put("horse", 5);
    Predicate<Entry<?, ?>> predicate =
        new Predicate<Entry<?, ?>>() {
          @Override
          public boolean apply(Entry<?, ?> input) {
            return "cat".equals(input.getKey()) || Integer.valueOf(2) == input.getValue();
          }
        };
    Map<String, Integer> filtered = Maps.filterEntries(unfiltered, predicate);
    assertEquals(ImmutableMap.of("cat", 3, "dog", 2), filtered);
  }
}
