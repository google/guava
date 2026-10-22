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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
    Map<String, Integer> backing = new LinkedHashMap<>();
    backing.put("a", 2);
    backing.put("b", 3);
    backing.put("c", 4);
    // We don't make any effort to avoid duplicate values in replaceAll or entrySet setValue calls.
    class OneWayBiMap extends ForwardingMap<String, Integer> implements BiMap<String, Integer> {
      @Override
      public void replaceAll(
          BiFunction<? super String, ? super Integer, ? extends Integer> function) {
        backing.replaceAll(function);
      }

      @Override
      public Set<Entry<String, Integer>> entrySet() {
        return backing.entrySet();
      }

      @Override
      public BiMap<Integer, String> inverse() {
        // filterValues requires something with an entrySet(), but the tests don't care beyond that.
        return ImmutableBiMap.of();
      }

      @Override
      protected Map<String, Integer> delegate() {
        throw new AssertionError();
      }

      @Override
      public @Nullable Integer forcePut(String key, Integer value) {
        throw new AssertionError();
      }

      @Override
      public Set<Integer> values() {
        throw new AssertionError();
      }
    }
    BiMap<String, Integer> filtered = filterValues(new OneWayBiMap(), value -> value % 2 == 0);

    assertThrows(
        IllegalArgumentException.class,
        () -> filtered.replaceAll((key, value) -> key.equals("c") ? 5 : value * 10));
    assertEquals(ImmutableMap.of("a", 20, "b", 3, "c", 4), backing);
  }
}
