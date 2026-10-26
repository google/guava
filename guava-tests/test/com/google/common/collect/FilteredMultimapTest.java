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
import static java.util.Arrays.asList;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Predicate;
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

  private static final Predicate<Entry<String, Integer>> ENTRY_PREDICATE =
      entry ->
          !Objects.equals(entry.getKey(), "badkey") && !Objects.equals(entry.getValue(), 55556);

  protected Multimap<String, Integer> create() {
    Multimap<String, Integer> unfiltered = HashMultimap.create();
    unfiltered.put("foo", 55556);
    unfiltered.put("badkey", 1);
    return Multimaps.filterEntries(unfiltered, ENTRY_PREDICATE);
  }

  private static final Predicate<String> KEY_PREDICATE = key -> !Objects.equals(key, "badkey");

  public void testFilterKeys() {
    Multimap<String, Integer> unfiltered = HashMultimap.create();
    unfiltered.put("foo", 55556);
    unfiltered.put("badkey", 1);
    Multimap<String, Integer> filtered = filterKeys(unfiltered, KEY_PREDICATE);
    assertEquals(1, filtered.size());
    assertTrue(filtered.containsEntry("foo", 55556));
  }

  private static final Predicate<Integer> VALUE_PREDICATE = value -> !Objects.equals(value, 55556);

  public void testFilterValues() {
    Multimap<String, Integer> unfiltered = HashMultimap.create();
    unfiltered.put("foo", 55556);
    unfiltered.put("badkey", 1);
    Multimap<String, Integer> filtered = filterValues(unfiltered, VALUE_PREDICATE);
    assertEquals(1, filtered.size());
    assertFalse(filtered.containsEntry("foo", 55556));
    assertTrue(filtered.containsEntry("badkey", 1));
  }

  public void testFilterFiltered() {
    Multimap<String, Integer> unfiltered = HashMultimap.create();
    unfiltered.put("foo", 55556);
    unfiltered.put("badkey", 1);
    unfiltered.put("foo", 1);
    Multimap<String, Integer> keyFiltered = filterKeys(unfiltered, KEY_PREDICATE);
    Multimap<String, Integer> filtered = filterValues(keyFiltered, VALUE_PREDICATE);
    assertEquals(1, filtered.size());
    assertTrue(filtered.containsEntry("foo", 1));
    assertTrue(filtered.keySet().retainAll(asList("cat", "dog")));
    assertEquals(0, filtered.size());
  }

  public void testFilterValuesEntrySetValue() {
    Multimap<String, Integer> unfiltered = LinkedListMultimap.create();
    unfiltered.put("foo", 1);
    Multimap<String, Integer> filtered = filterValues(unfiltered, VALUE_PREDICATE);
    Entry<String, Integer> entry = filtered.entries().iterator().next();
    assertThrows(IllegalArgumentException.class, () -> entry.setValue(55556));
    assertEquals(1, (int) entry.setValue(2));
    assertEquals(ImmutableList.of(Maps.immutableEntry("foo", 2)), unfiltered.entries());
  }

  @SuppressWarnings("unchecked") // the array contains only entries
  public void testFilterEntriesEntriesToArraySetValue() {
    Multimap<String, Integer> unfiltered = LinkedListMultimap.create();
    unfiltered.put("foo", 1);
    Multimap<String, Integer> filtered = Multimaps.filterEntries(unfiltered, ENTRY_PREDICATE);
    Entry<String, Integer> entry = (Entry<String, Integer>) filtered.entries().toArray()[0];
    assertThrows(IllegalArgumentException.class, () -> entry.setValue(55556));
    Entry<String, Integer> typedEntry = filtered.entries().toArray(new Entry[0])[0];
    assertThrows(IllegalArgumentException.class, () -> typedEntry.setValue(55556));
    assertEquals(ImmutableList.of(Maps.immutableEntry("foo", 1)), unfiltered.entries());
  }

  // TODO(jlevy): Many more tests needed.
}
