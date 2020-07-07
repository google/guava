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

package com.google.common.collect.testing.google;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.testing.Helpers.mapEntry;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestListGenerator;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.TestStringSortedMapGenerator;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

/**
 * Generators of sorted maps and derived collections.
 *
 * @author Kevin Bourrillion
 * @author Jesse Wilson
 * @author Jared Levy
 * @author Hayward Chan
 * @author Chris Povirk
 * @author Louis Wasserman
 */
@GwtCompatible
public class SortedMapGenerators {
  public static class ImmutableSortedMapGenerator extends TestStringSortedMapGenerator {
    @Override
    public SortedMap<String, String> create(Entry<String, String>[] entries) {
      ImmutableSortedMap.Builder<String, String> builder = ImmutableSortedMap.naturalOrder();
      for (Entry<String, String> entry : entries) {
        checkNotNull(entry);
        builder.put(entry.getKey(), entry.getValue());
      }
      return builder.build();
    }
  }

  public static class ImmutableSortedMapCopyOfEntriesGenerator
      extends TestStringSortedMapGenerator {
    @Override
    public SortedMap<String, String> create(Entry<String, String>[] entries) {
      return ImmutableSortedMap.copyOf(Arrays.asList(entries));
    }
  }

  public static class ImmutableSortedMapEntryListGenerator
      implements TestListGenerator<Entry<String, Integer>> {

    @Override
    public SampleElements<Entry<String, Integer>> samples() {
      return new SampleElements<>(
          mapEntry("foo", 5),
          mapEntry("bar", 3),
          mapEntry("baz", 17),
          mapEntry("quux", 1),
          mapEntry("toaster", -2));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Entry<String, Integer>[] createArray(int length) {
      return new Entry[length];
    }

    @Override
    public Iterable<Entry<String, Integer>> order(List<Entry<String, Integer>> insertionOrder) {
      return new Ordering<Entry<String, Integer>>() {
        @Override
        public int compare(Entry<String, Integer> left, Entry<String, Integer> right) {
          return left.getKey().compareTo(right.getKey());
        }
      }.sortedCopy(insertionOrder);
    }

    @Override
    public List<Entry<String, Integer>> create(Object... elements) {
      ImmutableSortedMap.Builder<String, Integer> builder = ImmutableSortedMap.naturalOrder();
      for (Object o : elements) {
        @SuppressWarnings("unchecked")
        Entry<String, Integer> entry = (Entry<String, Integer>) o;
        builder.put(entry);
      }
      return builder.build().entrySet().asList();
    }
  }

  public static class ImmutableSortedMapKeyListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      ImmutableSortedMap.Builder<String, Integer> builder = ImmutableSortedMap.naturalOrder();
      for (int i = 0; i < elements.length; i++) {
        builder.put(elements[i], i);
      }
      return builder.build().keySet().asList();
    }

    @Override
    public List<String> order(List<String> insertionOrder) {
      return Ordering.natural().sortedCopy(insertionOrder);
    }
  }

  public static class ImmutableSortedMapValueListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      ImmutableSortedMap.Builder<Integer, String> builder = ImmutableSortedMap.naturalOrder();
      for (int i = 0; i < elements.length; i++) {
        builder.put(i, elements[i]);
      }
      return builder.build().values().asList();
    }
  }
}
