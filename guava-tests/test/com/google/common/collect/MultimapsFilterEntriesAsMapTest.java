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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Predicate;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Tests for Multimaps.filterEntries().asMap().
 *
 * @author Jared Levy
 */
@GwtIncompatible(value = "untested")
public class MultimapsFilterEntriesAsMapTest extends AbstractMultimapAsMapImplementsMapTest {
  private static final Predicate<Entry<String, Integer>> PREDICATE =
      new Predicate<Entry<String, Integer>>() {
        @Override
        public boolean apply(Entry<String, Integer> entry) {
          return !"badkey".equals(entry.getKey()) && 55556 != entry.getValue();
        }
      };

  public MultimapsFilterEntriesAsMapTest() {
    super(true, true, false);
  }

  private Multimap<String, Integer> createMultimap() {
    Multimap<String, Integer> unfiltered = HashMultimap.create();
    unfiltered.put("zero", 55556);
    unfiltered.put("one", 55556);
    unfiltered.put("badkey", 1);
    return Multimaps.filterEntries(unfiltered, PREDICATE);
  }

  @Override
  protected Map<String, Collection<Integer>> makeEmptyMap() {
    Multimap<String, Integer> multimap = createMultimap();
    return multimap.asMap();
  }

  @Override
  protected Map<String, Collection<Integer>> makePopulatedMap() {
    Multimap<String, Integer> multimap = createMultimap();
    populate(multimap);
    return multimap.asMap();
  }
}
