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
import java.util.SortedMap;

@GwtCompatible
public class FilteredSortedMapTest extends AbstractFilteredMapTest {
  @Override
  SortedMap<String, Integer> createUnfiltered() {
    return Maps.newTreeMap();
  }

  public void testFirstAndLastKeyFilteredMap() {
    SortedMap<String, Integer> unfiltered = createUnfiltered();
    unfiltered.put("apple", 2);
    unfiltered.put("banana", 6);
    unfiltered.put("cat", 3);
    unfiltered.put("dog", 5);

    SortedMap<String, Integer> filtered = Maps.filterEntries(unfiltered, CORRECT_LENGTH);
    assertEquals("banana", filtered.firstKey());
    assertEquals("cat", filtered.lastKey());
  }

  public void testHeadSubTailMap_FilteredMap() {
    SortedMap<String, Integer> unfiltered = createUnfiltered();
    unfiltered.put("apple", 2);
    unfiltered.put("banana", 6);
    unfiltered.put("cat", 4);
    unfiltered.put("dog", 3);
    SortedMap<String, Integer> filtered = Maps.filterEntries(unfiltered, CORRECT_LENGTH);

    assertEquals(ImmutableMap.of("banana", 6), filtered.headMap("dog"));
    assertEquals(ImmutableMap.of(), filtered.headMap("banana"));
    assertEquals(ImmutableMap.of("banana", 6, "dog", 3), filtered.headMap("emu"));

    assertEquals(ImmutableMap.of("banana", 6), filtered.subMap("banana", "dog"));
    assertEquals(ImmutableMap.of("dog", 3), filtered.subMap("cat", "emu"));

    assertEquals(ImmutableMap.of("dog", 3), filtered.tailMap("cat"));
    assertEquals(ImmutableMap.of("banana", 6, "dog", 3), filtered.tailMap("banana"));
  }
}
