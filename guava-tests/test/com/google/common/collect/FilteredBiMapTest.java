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

import static com.google.common.collect.Maps.filterValues;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import org.jspecify.annotations.NullMarked;

@GwtCompatible
@NullMarked
public class FilteredBiMapTest extends AbstractFilteredMapTest {
  @Override
  BiMap<String, Integer> createUnfiltered() {
    return HashBiMap.create();
  }

  public void testFilteredValuesReplaceAll() {
    BiMap<String, Integer> unfiltered = createUnfiltered();
    unfiltered.put("a", 2);
    unfiltered.put("b", 3);
    unfiltered.put("c", 4);
    BiMap<String, Integer> filtered = filterValues(unfiltered, value -> value % 2 == 0);

    filtered.replaceAll((key, value) -> value * 10);
    assertEquals(ImmutableMap.of("a", 20, "b", 3, "c", 40), unfiltered);
  }

  public void testFilteredValuesIllegalReplaceAll() {
    BiMap<String, Integer> unfiltered = createUnfiltered();
    unfiltered.put("a", 2);
    unfiltered.put("b", 3);
    unfiltered.put("c", 4);
    BiMap<String, Integer> filtered = filterValues(unfiltered, value -> value % 2 == 0);

    assertThrows(
        IllegalArgumentException.class, () -> filtered.replaceAll((key, value) -> value + 11));
    assertEquals(ImmutableMap.of("a", 2, "b", 3, "c", 4), unfiltered);

    assertThrows(
        IllegalArgumentException.class,
        () -> filtered.replaceAll((key, value) -> key.equals("c") ? 5 : value * 10));
    assertEquals(ImmutableMap.of("a", 2, "b", 3, "c", 4), unfiltered);
  }
}
