/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.collect.Multimaps.filterKeys;
import static com.google.common.collect.Multimaps.filterValues;
import static com.google.common.collect.Multimaps.forMap;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Predicate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit tests for {@link Multimaps} filtering methods.
 *
 * @author Jared Levy
 */
@GwtIncompatible // nottested
@NullUnmarked
public class FilteredMultimapTest extends TestCase {
  public void testFilterKeys() {
    Multimap<String, Integer> unfiltered = HashMultimap.create();
    unfiltered.put("foo", BAD_VALUE);
    unfiltered.put(BAD_KEY, 1);
    Multimap<String, Integer> filtered = filterKeys(unfiltered, KEY_PREDICATE);
    assertEquals(1, filtered.size());
    assertTrue(filtered.containsEntry("foo", BAD_VALUE));
  }

  public void testFilterValues() {
    Multimap<String, Integer> unfiltered = HashMultimap.create();
    unfiltered.put("foo", BAD_VALUE);
    unfiltered.put(BAD_KEY, 1);
    Multimap<String, Integer> filtered = filterValues(unfiltered, VALUE_PREDICATE);
    assertEquals(1, filtered.size());
    assertFalse(filtered.containsEntry("foo", BAD_VALUE));
    assertTrue(filtered.containsEntry(BAD_KEY, 1));
  }

  public void testFilterFiltered() {
    Multimap<String, Integer> unfiltered = HashMultimap.create();
    unfiltered.put("foo", BAD_VALUE);
    unfiltered.put(BAD_KEY, 1);
    unfiltered.put("foo", 1);
    Multimap<String, Integer> keyFiltered = filterKeys(unfiltered, KEY_PREDICATE);
    Multimap<String, Integer> filtered = filterValues(keyFiltered, VALUE_PREDICATE);
    assertEquals(1, filtered.size());
    assertTrue(filtered.containsEntry("foo", 1));
    assertTrue(filtered.keySet().retainAll(asList("cat", "dog")));
    assertEquals(0, filtered.size());
  }

  public void testFilterValuesIllegalSetValue() {
    Multimap<String, Integer> unfiltered = LinkedListMultimap.create();
    unfiltered.put("foo", 1);
    Multimap<String, Integer> filtered = filterValues(unfiltered, VALUE_PREDICATE);

    Entry<String, Integer> entry = filtered.entries().iterator().next();
    assertThrows(IllegalArgumentException.class, () -> entry.setValue(BAD_VALUE));
    assertEquals(ImmutableListMultimap.of("foo", 1), unfiltered);

    entry.setValue(2);
    assertEquals(ImmutableListMultimap.of("foo", 2), unfiltered);
  }

  @SuppressWarnings("unchecked") // casting the elements of toArray() back to entries
  public void testFilterValuesIllegalSetValueViaToArray() {
    Multimap<String, Integer> unfiltered = LinkedListMultimap.create();
    unfiltered.put("foo", 1);
    Multimap<String, Integer> filtered = filterValues(unfiltered, VALUE_PREDICATE);

    Entry<?, Integer> entry = (Entry<?, Integer>) filtered.entries().toArray()[0];
    assertThrows(IllegalArgumentException.class, () -> entry.setValue(BAD_VALUE));
    Entry<?, Integer> typedEntry =
        (Entry<?, Integer>) filtered.entries().toArray(new Entry<?, ?>[0])[0];
    assertThrows(IllegalArgumentException.class, () -> typedEntry.setValue(BAD_VALUE));

    assertEquals(ImmutableListMultimap.of("foo", 1), unfiltered);
  }

  public void testFilterValuesSetMultimapIllegalSetValue() {
    Map<String, Integer> backing = new HashMap<>();
    backing.put("foo", 1);
    SetMultimap<String, Integer> unfiltered = forMap(backing);
    SetMultimap<String, Integer> filtered = filterValues(unfiltered, VALUE_PREDICATE);

    Entry<String, Integer> entry = filtered.entries().iterator().next();
    assertThrows(IllegalArgumentException.class, () -> entry.setValue(BAD_VALUE));
    assertEquals(ImmutableSetMultimap.of("foo", 1), unfiltered);

    entry.setValue(2);
    assertEquals(ImmutableSetMultimap.of("foo", 2), unfiltered);
  }

  private static final String BAD_KEY = "badkey";
  private static final int BAD_VALUE = 55556;

  private static final Predicate<String> KEY_PREDICATE = key -> !Objects.equals(key, BAD_KEY);

  private static final Predicate<Integer> VALUE_PREDICATE =
      value -> !Objects.equals(value, BAD_VALUE);

  // TODO(jlevy): Many more tests needed.
}
